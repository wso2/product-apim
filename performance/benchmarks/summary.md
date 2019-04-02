# WSO2 API Manager Performance Test Results

During each release, we execute various automated performance test scenarios and publish the results.

| Test Scenarios | Description |
| --- | --- |
| Passthrough | A secured API, which directly invokes the back-end service. |
| Mediation | A secured API, which has a mediation extension to modify the message. |

Our test client is [Apache JMeter](https://jmeter.apache.org/index.html). We test each scenario for a fixed duration of
time. We split the test results into warmup and measurement parts and use the measurement part to compute the
performance metrics.

Test scenarios use a [Netty](https://netty.io/) based back-end service which echoes back any request
posted to it after a specified period of time.

We run the performance tests under different numbers of concurrent users, message sizes (payloads) and back-end service
delays.

The main performance metrics:

1. **Throughput**: The number of requests that the WSO2 API Manager processes during a specific time interval (e.g. per second).
2. **Response Time**: The end-to-end latency for an operation of invoking an API. The complete distribution of response times was recorded.

In addition to the above metrics, we measure the load average and several memory-related metrics.

The following are the test parameters.

| Test Parameter | Description | Values |
| --- | --- | --- |
| Scenario Name | The name of the test scenario. | Refer to the above table. |
| Heap Size | The amount of memory allocated to the application | 4G |
| Concurrent Users | The number of users accessing the application at the same time. | 50, 100, 200, 300, 500, 1000 |
| Message Size (Bytes) | The request payload size in Bytes. | 50, 1024, 10240 |
| Back-end Delay (ms) | The delay added by the back-end service. | 0, 30, 500, 1000 |

The duration of each test is **900 seconds**. The warm-up period is **300 seconds**.
The measurement results are collected after the warm-up period.

A [**m5.large** Amazon EC2 instance](https://aws.amazon.com/ec2/instance-types/) was used to install WSO2 API Manager.

The following are the measurements collected from each performance test conducted for a given combination of
test parameters.

| Measurement | Description |
| --- | --- |
| Error % | Percentage of requests with errors |
| Average Response Time (ms) | The average response time of a set of results |
| Standard Deviation of Response Time (ms) | The “Standard Deviation” of the response time. |
| 99th Percentile of Response Time (ms) | 99% of the requests took no more than this time. The remaining samples took at least as long as this |
| Throughput (Requests/sec) | The throughput measured in requests per second. |
| Average Memory Footprint After Full GC (M) | The average memory consumed by the application after a full garbage collection event. |

The following is the summary of performance test results collected for the measurement period.

|  Scenario Name | Heap Size | Concurrent Users | Message Size (Bytes) | Back-end Service Delay (ms) | Error % | Throughput (Requests/sec) | Average Response Time (ms) | Standard Deviation of Response Time (ms) | 99th Percentile of Response Time (ms) | WSO2 API Manager GC Throughput (%) | Average WSO2 API Manager Memory Footprint After Full GC (M) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
|  Mediation | 4G | 50 | 50 | 0 | 0 | 3127.05 | 15.93 | 24.17 | 93 | 94.26 | 302.331 |
|  Mediation | 4G | 50 | 50 | 30 | 0 | 1457.83 | 34.26 | 17.77 | 89 | 96.37 | 204.662 |
|  Mediation | 4G | 50 | 50 | 500 | 0 | 95.54 | 522.93 | 99.27 | 999 | 99.51 | 88.69 |
|  Mediation | 4G | 50 | 50 | 1000 | 0 | 48.05 | 1037.66 | 184.39 | 1999 | 99.57 | 90.813 |
|  Mediation | 4G | 50 | 1024 | 0 | 0 | 2161.76 | 23.07 | 22.18 | 95 | 95.54 | 236.363 |
|  Mediation | 4G | 50 | 1024 | 30 | 0 | 1325.62 | 37.67 | 17.25 | 96 | 96.21 | 164.231 |
|  Mediation | 4G | 50 | 1024 | 500 | 0 | 95.5 | 523.33 | 99.17 | 999 | 99.44 | 105.556 |
|  Mediation | 4G | 50 | 1024 | 1000 | 0 | 48.75 | 1022.87 | 140.73 | 1999 | 99.54 | 83.991 |
|  Mediation | 4G | 50 | 10240 | 0 | 0 | 587.37 | 85.02 | 42.7 | 213 | 96.05 | 106.448 |
|  Mediation | 4G | 50 | 10240 | 30 | 0 | 538.75 | 92.7 | 29.22 | 182 | 95.76 | 106.904 |
|  Mediation | 4G | 50 | 10240 | 500 | 0 | 97.28 | 514.03 | 10.44 | 551 | 99.24 | 103.364 |
|  Mediation | 4G | 50 | 10240 | 1000 | 0 | 48.53 | 1027.29 | 126.84 | 1999 | 99.44 | 103.986 |
|  Mediation | 4G | 100 | 50 | 0 | 0 | 3269.96 | 30.52 | 35.79 | 128 | 93.89 | 315.229 |
|  Mediation | 4G | 100 | 50 | 30 | 0 | 2439.9 | 40.93 | 27.47 | 120 | 95.21 | 272.934 |
|  Mediation | 4G | 100 | 50 | 500 | 0 | 194.94 | 512.62 | 70.32 | 999 | 99.38 | 91.516 |
|  Mediation | 4G | 100 | 50 | 1000 | 0 | 97.6 | 1022.68 | 140.68 | 1999 | 99.51 | 91.118 |
|  Mediation | 4G | 100 | 1024 | 0 | 0 | 2291.55 | 43.53 | 31.16 | 142 | 95.25 | 230.925 |
|  Mediation | 4G | 100 | 1024 | 30 | 0 | 1883.57 | 53.03 | 24.88 | 121 | 95.96 | 238.952 |
|  Mediation | 4G | 100 | 1024 | 500 | 0 | 194.86 | 512.92 | 70.17 | 999 | 99.3 | 82.931 |
|  Mediation | 4G | 100 | 1024 | 1000 | 0 | 97.51 | 1022.82 | 140.62 | 1999 | 99.46 | 92.082 |
|  Mediation | 4G | 100 | 10240 | 0 | 0 | 578.9 | 172.68 | 73.19 | 377 | 96.05 | 92.333 |
|  Mediation | 4G | 100 | 10240 | 30 | 0 | 556.64 | 179.56 | 60.08 | 345 | 95.94 | 86.535 |
|  Mediation | 4G | 100 | 10240 | 500 | 0 | 189.51 | 527.7 | 24.82 | 619 | 98.74 | 89.496 |
|  Mediation | 4G | 100 | 10240 | 1000 | 0 | 97.44 | 1024.32 | 99.53 | 1983 | 99.23 | 89.248 |
|  Mediation | 4G | 200 | 50 | 0 | 0 | 3269.14 | 61.12 | 47.11 | 196 | 93.96 | 302.775 |
|  Mediation | 4G | 200 | 50 | 30 | 0 | 3020.16 | 66.16 | 43.35 | 169 | 94.14 | 307.458 |
|  Mediation | 4G | 200 | 50 | 500 | 0 | 393.68 | 507.63 | 50 | 951 | 99.09 | 91.62 |
|  Mediation | 4G | 200 | 50 | 1000 | 0 | 197.11 | 1012.68 | 99.62 | 1999 | 99.38 | 90.55 |
|  Mediation | 4G | 200 | 1024 | 0 | 0 | 2187.45 | 91.36 | 56.13 | 255 | 95.12 | 255.429 |
|  Mediation | 4G | 200 | 1024 | 30 | 0 | 2189.03 | 91.29 | 42.09 | 196 | 95.16 | 239.083 |
|  Mediation | 4G | 200 | 1024 | 500 | 0 | 392.41 | 509.56 | 50.01 | 947 | 98.74 | 90.856 |
|  Mediation | 4G | 200 | 1024 | 1000 | 0 | 197.31 | 1012.19 | 94.49 | 1071 | 99.26 | 89.346 |
|  Mediation | 4G | 200 | 10240 | 0 | 0 | 591.82 | 338.1 | 122.91 | 659 | 95.76 | 139.332 |
|  Mediation | 4G | 200 | 10240 | 30 | 0 | 589.47 | 339.51 | 113.39 | 635 | 96.03 | 137.391 |
|  Mediation | 4G | 200 | 10240 | 500 | 0 | 300.13 | 665.94 | 102.88 | 911 | 97.71 | 91.391 |
|  Mediation | 4G | 200 | 10240 | 1000 | 0 | 192.87 | 1035.56 | 76.18 | 1183 | 98.77 | 90.412 |
|  Mediation | 4G | 300 | 50 | 0 | 0 | 3204.54 | 93.54 | 63.58 | 267 | 93.89 | 304.741 |
|  Mediation | 4G | 300 | 50 | 30 | 0 | 2906.23 | 103.14 | 66.04 | 257 | 94.86 | 317.442 |
|  Mediation | 4G | 300 | 50 | 500 | 0 | 590.46 | 507.64 | 41.59 | 599 | 98.49 | 88.229 |
|  Mediation | 4G | 300 | 50 | 1000 | 0 | 296.37 | 1009.7 | 81.33 | 1063 | 99.2 | 106.304 |
|  Mediation | 4G | 300 | 1024 | 0 | 0 | 2248.35 | 133.35 | 72.1 | 333 | 95.03 | 270.496 |
|  Mediation | 4G | 300 | 1024 | 30 | 0 | 2202.95 | 136.09 | 58.94 | 289 | 95.23 | 234.971 |
|  Mediation | 4G | 300 | 1024 | 500 | 0 | 585.86 | 511.76 | 42.35 | 615 | 97.94 | 105.782 |
|  Mediation | 4G | 300 | 1024 | 1000 | 0 | 296.13 | 1010.31 | 81.41 | 1063 | 99.05 | 90.242 |
|  Mediation | 4G | 300 | 10240 | 0 | 0 | 566.08 | 530.11 | 165.8 | 959 | 95.8 | 153.576 |
|  Mediation | 4G | 300 | 10240 | 30 | 0 | 553.14 | 542.39 | 162.72 | 955 | 97.24 | 86.577 |
|  Mediation | 4G | 300 | 10240 | 500 | 0 | 358.24 | 836.82 | 142.93 | 1079 | 97.01 | 89.135 |
|  Mediation | 4G | 300 | 10240 | 1000 | 0 | 244.51 | 1224.16 | 157.19 | 1631 | 98.32 | 85.231 |
|  Mediation | 4G | 500 | 50 | 0 | 0 | 3307.8 | 151.11 | 93.19 | 375 | 93.32 | 352.056 |
|  Mediation | 4G | 500 | 50 | 30 | 0 | 3184.83 | 156.92 | 92.1 | 361 | 94.16 | 346.492 |
|  Mediation | 4G | 500 | 50 | 500 | 0 | 965.57 | 517.44 | 40.55 | 667 | 96.98 | 107.561 |
|  Mediation | 4G | 500 | 50 | 1000 | 0 | 495.15 | 1008.07 | 63.29 | 1079 | 98.68 | 88.899 |
|  Mediation | 4G | 500 | 1024 | 0 | 0 | 2215.11 | 225.75 | 102.46 | 499 | 94.74 | 269.453 |
|  Mediation | 4G | 500 | 1024 | 30 | 0 | 2213.93 | 225.87 | 93.92 | 471 | 94.71 | 331.524 |
|  Mediation | 4G | 500 | 1024 | 500 | 0 | 941.98 | 530.49 | 47.51 | 711 | 96.28 | 88.448 |
|  Mediation | 4G | 500 | 1024 | 1000 | 0 | 493.25 | 1010.9 | 58.82 | 1095 | 98.44 | 91.194 |
|  Mediation | 4G | 500 | 10240 | 0 | 0 | 589.67 | 847.68 | 223.48 | 1423 | 96.05 | 142.149 |
|  Mediation | 4G | 500 | 10240 | 30 | 0 | 576.41 | 867.49 | 232.6 | 1471 | 96.08 | 146.856 |
|  Mediation | 4G | 500 | 10240 | 500 | 0 | 437.06 | 1143.02 | 219.04 | 1519 | 96.78 | 87.787 |
|  Mediation | 4G | 500 | 10240 | 1000 | 0 | 330.23 | 1513.69 | 249.46 | 1927 | 96.94 | 90.256 |
|  Mediation | 4G | 1000 | 50 | 0 | 0 | 3150.89 | 317.41 | 159.77 | 711 | 92.98 | 467.092 |
|  Mediation | 4G | 1000 | 50 | 30 | 0 | 2991.49 | 334.37 | 151.87 | 707 | 93.58 | 386.444 |
|  Mediation | 4G | 1000 | 50 | 500 | 0 | 1868.27 | 534.93 | 96.08 | 995 | 96.42 | 300.109 |
|  Mediation | 4G | 1000 | 50 | 1000 | 0 | 975.92 | 1022.59 | 56.26 | 1199 | 96.33 | 104.862 |
|  Mediation | 4G | 1000 | 1024 | 0 | 0 | 2181.31 | 458.56 | 164.95 | 903 | 94.2 | 336.746 |
|  Mediation | 4G | 1000 | 1024 | 30 | 0 | 2048.68 | 488.19 | 175.49 | 955 | 94.63 | 309.611 |
|  Mediation | 4G | 1000 | 1024 | 500 | 0 | 1614.65 | 618.9 | 112.72 | 939 | 96.27 | 301.295 |
|  Mediation | 4G | 1000 | 1024 | 1000 | 0 | 958.59 | 1041.31 | 74.05 | 1247 | 96.52 | 196.619 |
|  Mediation | 4G | 1000 | 10240 | 0 | 0 | 561.77 | 1776.74 | 365.56 | 2735 | 95.18 | 296.153 |
|  Mediation | 4G | 1000 | 10240 | 30 | 0 | 547.31 | 1824.45 | 344.97 | 2767 | 95.76 | 256.094 |
|  Mediation | 4G | 1000 | 10240 | 500 | 0 | 513.62 | 1943.83 | 433.81 | 3071 | 94.46 | 378.072 |
|  Mediation | 4G | 1000 | 10240 | 1000 | 0 | 360.71 | 2421.95 | 1502.24 | 3631 | 96.55 | 369.535 |
|  Passthrough | 4G | 50 | 50 | 0 | 0 | 4162.68 | 11.96 | 36.33 | 57 | 93.14 | 385.657 |
|  Passthrough | 4G | 50 | 50 | 30 | 0 | 1489.12 | 33.54 | 15.08 | 60 | 96.81 | 105.987 |
|  Passthrough | 4G | 50 | 50 | 500 | 0 | 97.56 | 512.27 | 70.46 | 999 | 99.56 | 87.95 |
|  Passthrough | 4G | 50 | 50 | 1000 | 0 | 48.8 | 1022.5 | 140.91 | 1999 | 99.57 | 90.641 |
|  Passthrough | 4G | 50 | 1024 | 0 | 0 | 3824.09 | 13.02 | 25.41 | 55 | 93.76 | 328.683 |
|  Passthrough | 4G | 50 | 1024 | 30 | 0 | 1487.81 | 33.56 | 13.66 | 60 | 96.86 | 88.852 |
|  Passthrough | 4G | 50 | 1024 | 500 | 0 | 97.62 | 512.11 | 70.42 | 999 | 99.55 | 86.418 |
|  Passthrough | 4G | 50 | 1024 | 1000 | 0 | 48.81 | 1022.56 | 140.82 | 1999 | 99.58 | 88.724 |
|  Passthrough | 4G | 50 | 10240 | 0 | 0 | 2242.12 | 22.24 | 19.77 | 56 | 96.21 | 206.748 |
|  Passthrough | 4G | 50 | 10240 | 30 | 0 | 1479.11 | 33.75 | 13.61 | 60 | 96.57 | 104.9 |
|  Passthrough | 4G | 50 | 10240 | 500 | 0 | 99.55 | 502.44 | 4.13 | 509 | 99.55 | 105.196 |
|  Passthrough | 4G | 50 | 10240 | 1000 | 0 | 48.86 | 1022.75 | 140.57 | 1999 | 99.57 | 89.04 |
|  Passthrough | 4G | 100 | 50 | 0 | 0 | 4331.93 | 23.02 | 44.74 | 119 | 92.78 | 409.829 |
|  Passthrough | 4G | 100 | 50 | 30 | 0 | 2867.54 | 34.83 | 28.82 | 71 | 95.31 | 273.454 |
|  Passthrough | 4G | 100 | 50 | 500 | 0 | 195.06 | 512.09 | 70.29 | 999 | 99.48 | 86.712 |
|  Passthrough | 4G | 100 | 50 | 1000 | 0 | 97.85 | 1019.4 | 129.36 | 1999 | 99.54 | 92.214 |
|  Passthrough | 4G | 100 | 1024 | 0 | 0 | 3849.65 | 25.92 | 36.99 | 116 | 93.68 | 346.922 |
|  Passthrough | 4G | 100 | 1024 | 30 | 0 | 2818.03 | 35.43 | 28.54 | 89 | 95.26 | 300.488 |
|  Passthrough | 4G | 100 | 1024 | 500 | 0 | 195.42 | 511.47 | 68.42 | 999 | 99.53 | 83.073 |
|  Passthrough | 4G | 100 | 1024 | 1000 | 0 | 98.58 | 1012.32 | 99.71 | 1999 | 99.53 | 93.332 |
|  Passthrough | 4G | 100 | 10240 | 0 | 0 | 2151.62 | 46.4 | 34.27 | 148 | 95.8 | 232.775 |
|  Passthrough | 4G | 100 | 10240 | 30 | 0 | 2127.76 | 46.92 | 30.76 | 135 | 96.13 | 240.796 |
|  Passthrough | 4G | 100 | 10240 | 500 | 0 | 196.97 | 507.41 | 49.9 | 999 | 99.48 | 86.843 |
|  Passthrough | 4G | 100 | 10240 | 1000 | 0 | 99.71 | 1002.34 | 4 | 1007 | 99.53 | 90.152 |
|  Passthrough | 4G | 200 | 50 | 0 | 0 | 4405.99 | 45.32 | 59.86 | 202 | 92.79 | 393.617 |
|  Passthrough | 4G | 200 | 50 | 30 | 0 | 3861.93 | 51.72 | 51.3 | 197 | 93.47 | 366.266 |
|  Passthrough | 4G | 200 | 50 | 500 | 0 | 394.77 | 506.36 | 45.34 | 571 | 99.28 | 84.824 |
|  Passthrough | 4G | 200 | 50 | 1000 | 0 | 197.08 | 1012.35 | 99.3 | 1967 | 99.47 | 87.813 |
|  Passthrough | 4G | 200 | 1024 | 0 | 0 | 3888.65 | 51.37 | 53.54 | 204 | 93.63 | 349.864 |
|  Passthrough | 4G | 200 | 1024 | 30 | 0 | 3755.83 | 53.19 | 50.17 | 201 | 93.53 | 356.516 |
|  Passthrough | 4G | 200 | 1024 | 500 | 0 | 396.03 | 504.76 | 35.52 | 523 | 99.29 | 81.436 |
|  Passthrough | 4G | 200 | 1024 | 1000 | 0 | 197.36 | 1011.19 | 93.43 | 1079 | 99.47 | 106.967 |
|  Passthrough | 4G | 200 | 10240 | 0 | 0 | 2083.55 | 95.87 | 48.67 | 236 | 96.03 | 229.002 |
|  Passthrough | 4G | 200 | 10240 | 30 | 0 | 2157.65 | 92.59 | 45.62 | 229 | 96.1 | 234.316 |
|  Passthrough | 4G | 200 | 10240 | 500 | 0 | 397.81 | 502.75 | 5.65 | 523 | 99.23 | 82.967 |
|  Passthrough | 4G | 200 | 10240 | 1000 | 0 | 199.28 | 1002.42 | 4.3 | 1011 | 99.45 | 85.009 |
|  Passthrough | 4G | 300 | 50 | 0 | 0 | 4066.95 | 71.75 | 74.84 | 259 | 95.05 | 333.44 |
|  Passthrough | 4G | 300 | 50 | 30 | 0 | 4002.07 | 74.88 | 67.96 | 242 | 94.47 | 400.592 |
|  Passthrough | 4G | 300 | 50 | 500 | 0 | 593.13 | 505.29 | 35.68 | 567 | 99.13 | 81.688 |
|  Passthrough | 4G | 300 | 50 | 1000 | 0.49 | 6.86 | 38893.63 | 56979.95 | 128511 | 99.51 | 97.313 |
|  Passthrough | 4G | 300 | 1024 | 0 | 0 | 2987.1 | 90.84 | 518.22 | 287 | 96.39 | 213.544 |
|  Passthrough | 4G | 300 | 1024 | 30 | 0 | 3572.91 | 83.88 | 60.38 | 257 | 95.25 | 304.184 |
|  Passthrough | 4G | 300 | 1024 | 500 | 0.85 | 62.67 | 4523.56 | 23063.38 | 139263 | 99.46 | 91.252 |
|  Passthrough | 4G | 300 | 1024 | 1000 | 0 | 294.48 | 1009.03 | 82.15 | 1063 | 99.39 | 85.648 |
|  Passthrough | 4G | 300 | 10240 | 0 | 0 | 1269.4 | 225.44 | 2802.56 | 391 | 97.99 | 143.084 |
|  Passthrough | 4G | 300 | 10240 | 30 | 0 | 1565.16 | 178.91 | 1732.39 | 387 | 97.19 | 158.558 |
|  Passthrough | 4G | 300 | 10240 | 500 | 0.64 | 67.24 | 3714.3 | 20815.52 | 129023 | 99.44 | 86.969 |
|  Passthrough | 4G | 300 | 10240 | 1000 | 0 | 200.97 | 1342.01 | 6368.85 | 1247 | 99.41 | 91.53 |
|  Passthrough | 4G | 500 | 50 | 0 | 0 | 3812.31 | 131.09 | 97.23 | 371 | 94.86 | 350.378 |
|  Passthrough | 4G | 500 | 50 | 30 | 0 | 3918.19 | 126.2 | 97.8 | 353 | 95.13 | 367.962 |
|  Passthrough | 4G | 500 | 50 | 500 | 0 | 982.5 | 508.52 | 35.82 | 643 | 98.52 | 87.154 |
|  Passthrough | 4G | 500 | 50 | 1000 | 0 | 495.54 | 1006.39 | 58.57 | 1047 | 99.2 | 88.684 |
|  Passthrough | 4G | 500 | 1024 | 0 | 0 | 3587.06 | 134.84 | 108.86 | 385 | 95.61 | 303.237 |
|  Passthrough | 4G | 500 | 1024 | 30 | 0 | 3470.16 | 144.01 | 80.03 | 357 | 95.56 | 287.971 |
|  Passthrough | 4G | 500 | 1024 | 500 | 0 | 959.21 | 514.28 | 155.6 | 659 | 98.61 | 88.046 |
|  Passthrough | 4G | 500 | 1024 | 1000 | 0 | 448.6 | 1005.47 | 149.39 | 1039 | 99.2 | 90.102 |
|  Passthrough | 4G | 500 | 10240 | 0 | 0 | 2067.36 | 241.85 | 88.07 | 475 | 96.38 | 271.092 |
|  Passthrough | 4G | 500 | 10240 | 30 | 0 | 2069.24 | 241.64 | 88.27 | 453 | 96.22 | 249.384 |
|  Passthrough | 4G | 500 | 10240 | 500 | 0 | 973.37 | 513.48 | 32.99 | 639 | 98.15 | 90.581 |
|  Passthrough | 4G | 500 | 10240 | 1000 | 0 | 496.03 | 1005.24 | 41.73 | 1047 | 99.04 | 85.56 |
|  Passthrough | 4G | 1000 | 50 | 0 | 0 | 4348.64 | 229.97 | 142.72 | 535 | 93.36 | 435.014 |
|  Passthrough | 4G | 1000 | 50 | 30 | 0 | 4070.39 | 245.6 | 143.11 | 567 | 94.01 | 421.673 |
|  Passthrough | 4G | 1000 | 50 | 500 | 0 | 1806.73 | 553.03 | 141.03 | 987 | 96.51 | 247.496 |
|  Passthrough | 4G | 1000 | 50 | 1000 | 0 | 815.45 | 1224.03 | 177.97 | 1559 | 98.6 | 106.128 |
|  Passthrough | 4G | 1000 | 1024 | 0 | 0 | 3519.55 | 284.16 | 159.6 | 631 | 94.68 | 409.271 |
|  Passthrough | 4G | 1000 | 1024 | 30 | 0 | 3817.13 | 262.03 | 137.95 | 555 | 93.37 | 452.123 |
|  Passthrough | 4G | 1000 | 1024 | 500 | 0 | 1886.79 | 529.72 | 106.18 | 1003 | 96.95 | 296.432 |
|  Passthrough | 4G | 1000 | 1024 | 1000 | 0 | 990.18 | 1007.69 | 47.06 | 1111 | 98.16 | 91.71 |
|  Passthrough | 4G | 1000 | 10240 | 0 | 0 | 2060.72 | 485.34 | 157.18 | 867 | 96.74 | 292.754 |
|  Passthrough | 4G | 1000 | 10240 | 30 | 0 | 2086.14 | 479.49 | 111.52 | 795 | 96.79 | 238.769 |
|  Passthrough | 4G | 1000 | 10240 | 500 | 0 | 1721.52 | 580.39 | 102.72 | 863 | 96.6 | 248.856 |
|  Passthrough | 4G | 1000 | 10240 | 1000 | 0 | 978.84 | 1018.98 | 53.97 | 1175 | 97.63 | 110.959 |
