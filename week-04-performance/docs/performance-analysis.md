# Week 4 Performance Analysis

## What was measured

The public post list was the target endpoint. The baseline was the Week 2 `GET /api/v1/posts` route, which returns every post. The Week 4 change adds `GET /api/v1/posts/page?page=0&size=20`, an additive route that leaves the original response contract in place. The new route uses a database slice, limits the returned records, and fetches authors with the posts. It allows page sizes from 1 through 100 and returns a `hasNext` flag without a separate total-count query.

The comparison used the same local file-backed H2 database and the same Java 21.0.11 runtime. The dataset contained 250 posts by one registered author, with a 3,999-character body per post. Each run sent 20 warm-up requests followed by 1,000 measured requests at concurrency 8. The baseline application was stopped before the optimized application was started. Both were measured on port 8094 with the same load-test script. The script saved per-request durations and response lengths as CSV and JSON.

## Results

| Measure | Baseline all posts | Week 4 page of 20 |
| --- | ---: | ---: |
| Successful measured responses | 1,000 / 1,000 | 1,000 / 1,000 |
| Mean response body size | 1,046,331 bytes | 83,761 bytes |
| Mean latency | 50.747 ms | 18.648 ms |
| Median latency | 46.449 ms | 13.616 ms |
| 95th percentile latency | 87.437 ms | 50.551 ms |
| Throughput | 156.07 requests/s | 424.50 requests/s |
| Average Java working set during sample | 284.04 MB | 271.75 MB |
| Peak Java working set during sample | 289.70 MB | 275.43 MB |
| Average CPU as a share of the host | 22.46% | 22.99% |

The page response body was 91.99% smaller for this dataset. Mean latency fell by 63.25%, the measured p95 fell by 42.19%, and the short-run throughput was 2.72 times the baseline. Average working set was 4.33% lower and peak working set was 4.93% lower. CPU use was effectively similar in this sample: 22.46% versus 22.99% of the host.

## Interpretation and limits

The strongest result is the smaller response: the new endpoint returns up to 20 posts instead of serializing all 250. That reduces the amount of JSON sent and the work needed to assemble each response. The p95 is included because an average alone can hide slower requests. The measured sample improved, but this is a local single-process check on H2, one author, one machine, and one concurrency level. The paged route also returns fewer records, so this is a capacity and bounded-response comparison, not a like-for-like test that returns identical content. The legacy endpoint remains available for compatibility and is still unbounded; callers should choose the paged route for collections that can grow.

To reproduce the dataset and run the requests, start the app as described in the README, then use `scripts/load_test.py`. The checked-in `results/baseline.json`, `results/baseline.csv`, `results/optimized.json`, and `results/optimized.csv` contain the measurements used above.
