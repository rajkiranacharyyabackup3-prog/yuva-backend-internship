#!/usr/bin/env python3
"""Seed and measure the blog API using only Python's standard library."""

import argparse
import base64
import concurrent.futures
import csv
import ctypes
import json
import math
import os
import statistics
import threading
import time
import urllib.error
import urllib.request
import uuid
from pathlib import Path


def request_json(url, method="GET", payload=None, auth=None, timeout=30):
    headers = {"Accept": "application/json"}
    body = None
    if payload is not None:
        headers["Content-Type"] = "application/json"
        body = json.dumps(payload).encode("utf-8")
    if auth:
        token = base64.b64encode(f"{auth[0]}:{auth[1]}".encode()).decode()
        headers["Authorization"] = f"Basic {token}"
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=timeout) as response:
        return response.status, response.read()


def seed(args):
    username = "bench_" + uuid.uuid4().hex[:10]
    password = "BenchPass123!"
    base = args.base_url.rstrip("/")
    status, _ = request_json(base + "/api/v1/users", "POST", {
        "username": username, "email": username + "@example.com", "password": password
    })
    if status != 201:
        raise RuntimeError(f"Registration returned HTTP {status}.")
    content = "Load-test content. " + ("x" * max(0, args.content_bytes - 20))
    for index in range(args.posts):
        status, _ = request_json(base + "/api/v1/posts", "POST", {
            "title": f"Benchmark post {index + 1:04d}", "content": content
        }, auth=(username, password))
        if status != 201:
            raise RuntimeError(f"Creating post {index + 1} returned HTTP {status}.")
        if (index + 1) % 50 == 0:
            print(f"Seeded {index + 1}/{args.posts} posts", flush=True)
    print(json.dumps({"seededPosts": args.posts, "contentCharacters": len(content),
                      "account": username}, indent=2))


class WindowsProcessSampler:
    """Sample working set and process CPU through the Windows process APIs."""

    class FileTime(ctypes.Structure):
        _fields_ = [("low", ctypes.c_ulong), ("high", ctypes.c_ulong)]

    class MemoryCounters(ctypes.Structure):
        _fields_ = [("cb", ctypes.c_ulong), ("PageFaultCount", ctypes.c_ulong),
                    ("PeakWorkingSetSize", ctypes.c_size_t), ("WorkingSetSize", ctypes.c_size_t),
                    ("QuotaPeakPagedPoolUsage", ctypes.c_size_t), ("QuotaPagedPoolUsage", ctypes.c_size_t),
                    ("QuotaPeakNonPagedPoolUsage", ctypes.c_size_t), ("QuotaNonPagedPoolUsage", ctypes.c_size_t),
                    ("PagefileUsage", ctypes.c_size_t), ("PeakPagefileUsage", ctypes.c_size_t),
                    ("PrivateUsage", ctypes.c_size_t)]

    def __init__(self, pid):
        self.samples = []
        self.stop_event = threading.Event()
        self.pid = pid
        self.available = os.name == "nt" and pid is not None
        if not self.available:
            return
        self.kernel = ctypes.WinDLL("kernel32", use_last_error=True)
        self.psapi = ctypes.WinDLL("psapi", use_last_error=True)
        self.handle = self.kernel.OpenProcess(0x1000 | 0x0010, False, pid)
        self.available = bool(self.handle)
        self.thread = threading.Thread(target=self._sample_loop, daemon=True)

    @staticmethod
    def _ticks(ft):
        return (ft.high << 32) + ft.low

    def _sample(self):
        creation, exit_time, kernel_time, user_time = self.FileTime(), self.FileTime(), self.FileTime(), self.FileTime()
        mem = self.MemoryCounters()
        mem.cb = ctypes.sizeof(mem)
        if not self.kernel.GetProcessTimes(self.handle, ctypes.byref(creation), ctypes.byref(exit_time),
                                           ctypes.byref(kernel_time), ctypes.byref(user_time)):
            return
        if not self.psapi.GetProcessMemoryInfo(self.handle, ctypes.byref(mem), mem.cb):
            return
        self.samples.append({"time": time.monotonic(), "cpu": (self._ticks(kernel_time) + self._ticks(user_time)) / 10_000_000,
                             "workingSetMB": mem.WorkingSetSize / (1024 * 1024)})

    def _sample_loop(self):
        while not self.stop_event.is_set():
            self._sample()
            self.stop_event.wait(0.25)
        self._sample()

    def start(self):
        if self.available:
            self.thread.start()

    def finish(self, elapsed):
        if not self.available:
            return {"processId": self.pid, "available": False}
        self.stop_event.set()
        self.thread.join()
        self.kernel.CloseHandle(self.handle)
        if not self.samples:
            return {"processId": self.pid, "available": False}
        values = [sample["workingSetMB"] for sample in self.samples]
        cpu_delta = max(0, self.samples[-1]["cpu"] - self.samples[0]["cpu"])
        return {"processId": self.pid, "available": True, "samples": len(values),
                "averageWorkingSetMB": round(statistics.mean(values), 2),
                "peakWorkingSetMB": round(max(values), 2),
                "averageCpuPercentOfHost": round(cpu_delta / max(elapsed, 0.001)
                                                   / max(os.cpu_count() or 1, 1) * 100, 2)}


def percentile(values, p):
    ordered = sorted(values)
    return ordered[min(len(ordered) - 1, math.ceil(p * len(ordered)) - 1)]


def measure(args):
    base = args.base_url.rstrip("/")
    endpoint = "/api/v1/posts" if args.mode == "all" else f"/api/v1/posts/page?page=0&size={args.size}"
    url = base + endpoint
    for _ in range(args.warmup):
        status, _ = request_json(url)
        if status != 200:
            raise RuntimeError(f"Warm-up returned HTTP {status}.")

    sampler = WindowsProcessSampler(args.pid)
    sampler.start()
    start = time.perf_counter()
    def one(index):
        begin = time.perf_counter()
        try:
            status, data = request_json(url)
            return {"request": index + 1, "status": status,
                    "durationMs": round((time.perf_counter() - begin) * 1000, 3), "responseBytes": len(data)}
        except (urllib.error.URLError, TimeoutError) as exc:
            return {"request": index + 1, "status": 0,
                    "durationMs": round((time.perf_counter() - begin) * 1000, 3),
                    "responseBytes": 0, "error": str(exc)}

    with concurrent.futures.ThreadPoolExecutor(max_workers=args.concurrency) as pool:
        samples = list(pool.map(one, range(args.requests)))
    elapsed = time.perf_counter() - start
    resources = sampler.finish(elapsed)
    durations = [row["durationMs"] for row in samples]
    sizes = [row["responseBytes"] for row in samples]
    successful = sum(row["status"] == 200 for row in samples)
    summary = {"label": args.label, "mode": args.mode, "endpoint": endpoint,
               "requests": args.requests, "successfulResponses": successful,
               "concurrency": args.concurrency, "warmupRequests": args.warmup,
               "elapsedSeconds": round(elapsed, 3),
               "throughputRequestsPerSecond": round(successful / max(elapsed, 0.001), 2),
               "latencyMs": {"mean": round(statistics.mean(durations), 3),
                             "median": round(statistics.median(durations), 3),
                             "p95": round(percentile(durations, 0.95), 3),
                             "max": round(max(durations), 3)},
               "responseBytes": {"mean": round(statistics.mean(sizes), 1),
                                 "median": round(statistics.median(sizes), 1),
                                 "max": max(sizes)},
               "processResources": resources,
               "notes": "One seeded author; response sizes differ by design because the paged route returns at most the requested page."}
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps({"summary": summary, "samples": samples}, indent=2), encoding="utf-8")
    csv_path = output.with_suffix(".csv")
    with csv_path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=sorted({key for row in samples for key in row}))
        writer.writeheader()
        writer.writerows(samples)
    print(json.dumps(summary, indent=2))
    print(f"Saved raw results: {output} and {csv_path}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    seed_parser = sub.add_parser("seed", help="Create one account and a set of posts through the API.")
    seed_parser.add_argument("--base-url", default="http://localhost:8094")
    seed_parser.add_argument("--posts", type=int, default=250)
    seed_parser.add_argument("--content-bytes", type=int, default=4000)
    seed_parser.set_defaults(func=seed)
    measure_parser = sub.add_parser("measure", help="Run a concurrent read-only load sample.")
    measure_parser.add_argument("--base-url", default="http://localhost:8094")
    measure_parser.add_argument("--mode", choices=["all", "page"], required=True)
    measure_parser.add_argument("--label", required=True)
    measure_parser.add_argument("--requests", type=int, default=100)
    measure_parser.add_argument("--concurrency", type=int, default=8)
    measure_parser.add_argument("--warmup", type=int, default=10)
    measure_parser.add_argument("--size", type=int, default=20)
    measure_parser.add_argument("--pid", type=int, help="Java process ID for Windows CPU and working-set samples.")
    measure_parser.add_argument("--output", required=True)
    measure_parser.set_defaults(func=measure)
    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
