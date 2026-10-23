# Week 4 Recorded Comparison

Date: 2026-10-04 (local machine)

Dataset: 250 posts, 3,999 characters per body, one author, file-backed H2 database.

Workload for each run: 20 warm-ups, 1,000 measured GET requests, concurrency 8, Java 21.0.11, Spring Boot 4.1.1, local Windows host.

| Metric | Baseline | Paged route |
| --- | ---: | ---: |
| Endpoint | `GET /api/v1/posts` | `GET /api/v1/posts/page?page=0&size=20` |
| Responses | 1,000 / 1,000 successful | 1,000 / 1,000 successful |
| Mean response bytes | 1,046,331 | 83,761 |
| Mean latency | 50.747 ms | 18.648 ms |
| Median latency | 46.449 ms | 13.616 ms |
| p95 latency | 87.437 ms | 50.551 ms |
| Throughput | 156.07 req/s | 424.50 req/s |
| Mean working set | 284.04 MB | 271.75 MB |
| Peak working set | 289.70 MB | 275.43 MB |
| Average CPU share of host | 22.46% | 22.99% |

The payload reduction is 91.99%. The page endpoint returns at most 20 records, so response-size and latency gains are tied to bounded output. These local measurements are a directional comparison, not a production capacity guarantee. See `docs/performance-analysis.md` for method and caveats.
