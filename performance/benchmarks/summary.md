# WSO2 API Manager 3.0.0 Performance Test Results

During each release, we execute various automated performance test scenarios and publish the results.

| Test Scenarios | Description |
| --- | --- |
| Passthrough | A secured API, which directly invokes the back-end service. |
| Transformation | A secured API, which has a mediation extension to modify the message. |

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
|  Passthrough | 4G | 50 | 50 | 0 | 0 | 3241.21 | 15.33 | 38.75 | 62 | 93.08 | 447.021 |
|  Passthrough | 4G | 50 | 50 | 30 | 0 | 1496.7 | 33.32 | 25.77 | 87 | 95.34 | 134.468 |
|  Passthrough | 4G | 50 | 50 | 500 | 0 | 99.53 | 502.53 | 5.05 | 505 | 99.53 | 72.232 |
|  Passthrough | 4G | 50 | 50 | 1000 | 0 | 49.82 | 1002.27 | 3.75 | 1007 | 99.61 | 72.582 |
|  Passthrough | 4G | 50 | 1024 | 0 | 0 | 3213.41 | 15.47 | 28.71 | 60 | 93.42 | 414.436 |
|  Passthrough | 4G | 50 | 1024 | 30 | 0 | 1500.81 | 33.23 | 23.01 | 59 | 95.61 | 187.681 |
|  Passthrough | 4G | 50 | 1024 | 500 | 0 | 99.52 | 502.72 | 5.13 | 507 | 99.52 | 73.878 |
|  Passthrough | 4G | 50 | 1024 | 1000 | 0 | 49.77 | 1002.26 | 3.52 | 1007 | 99.59 | 73.081 |
|  Passthrough | 4G | 50 | 10240 | 0 | 0 | 1929.91 | 25.81 | 30.64 | 69 | 95.38 | 345.319 |
|  Passthrough | 4G | 50 | 10240 | 30 | 0 | 1429.46 | 34.88 | 19.92 | 76 | 96.04 | 157.103 |
|  Passthrough | 4G | 50 | 10240 | 500 | 0 | 99.39 | 503.17 | 5.51 | 509 | 99.49 | 71.868 |
|  Passthrough | 4G | 50 | 10240 | 1000 | 0 | 49.76 | 1002.69 | 5.74 | 1007 | 99.59 | 72.981 |
|  Passthrough | 4G | 100 | 50 | 0 | 0 | 3357.95 | 29.67 | 44.23 | 146 | 93.22 | 418.809 |
|  Passthrough | 4G | 100 | 50 | 30 | 0 | 2716.5 | 36.71 | 34.11 | 138 | 94.27 | 342.904 |
|  Passthrough | 4G | 100 | 50 | 500 | 0 | 198.96 | 502.6 | 4.74 | 507 | 99.46 | 73.223 |
|  Passthrough | 4G | 100 | 50 | 1000 | 0 | 99.66 | 1002.34 | 4.94 | 1007 | 99.56 | 72.158 |
|  Passthrough | 4G | 100 | 1024 | 0 | 0 | 3231.57 | 30.84 | 47.64 | 149 | 93.04 | 449.685 |
|  Passthrough | 4G | 100 | 1024 | 30 | 0 | 2633.22 | 37.88 | 39.42 | 142 | 94.4 | 367.826 |
|  Passthrough | 4G | 100 | 1024 | 500 | 0 | 198.9 | 502.8 | 5.86 | 509 | 99.41 | 71.916 |
|  Passthrough | 4G | 100 | 1024 | 1000 | 0 | 99.56 | 1002.66 | 7.12 | 1011 | 99.49 | 72.544 |
|  Passthrough | 4G | 100 | 10240 | 0 | 0 | 1902.08 | 52.46 | 44.06 | 163 | 95.5 | 347.6 |
|  Passthrough | 4G | 100 | 10240 | 30 | 0 | 1903.65 | 52.41 | 40.51 | 158 | 95.39 | 362.208 |
|  Passthrough | 4G | 100 | 10240 | 500 | 0 | 198.77 | 503.29 | 5.96 | 515 | 99.42 | 73.456 |
|  Passthrough | 4G | 100 | 10240 | 1000 | 0 | 99.58 | 1002.63 | 5.81 | 1011 | 99.5 | 72.545 |
|  Passthrough | 4G | 200 | 50 | 0 | 0 | 3458.22 | 57.7 | 65.06 | 213 | 92.79 | 437.743 |
|  Passthrough | 4G | 200 | 50 | 30 | 0 | 3346.5 | 59.64 | 69.51 | 203 | 92.12 | 546.679 |
|  Passthrough | 4G | 200 | 50 | 500 | 0 | 397.75 | 502.8 | 5.67 | 515 | 99.15 | 72.694 |
|  Passthrough | 4G | 200 | 50 | 1000 | 0 | 199.22 | 1002.5 | 5.15 | 1007 | 99.42 | 72.484 |
|  Passthrough | 4G | 200 | 1024 | 0 | 0 | 3247.17 | 61.47 | 61.06 | 216 | 93.2 | 429.146 |
|  Passthrough | 4G | 200 | 1024 | 30 | 0 | 3179.06 | 62.8 | 60.88 | 205 | 93.02 | 459.499 |
|  Passthrough | 4G | 200 | 1024 | 500 | 0 | 397.69 | 503.03 | 6.66 | 523 | 99.13 | 73.198 |
|  Passthrough | 4G | 200 | 1024 | 1000 | 0 | 199.24 | 1002.65 | 6.21 | 1011 | 99.45 | 71.904 |
|  Passthrough | 4G | 200 | 10240 | 0 | 0 | 1889.45 | 105.72 | 68.8 | 252 | 95.31 | 377.528 |
|  Passthrough | 4G | 200 | 10240 | 30 | 0 | 1914.61 | 104.32 | 62.69 | 243 | 95.71 | 327.165 |
|  Passthrough | 4G | 200 | 10240 | 500 | 0 | 397.16 | 503.6 | 6.56 | 523 | 99.11 | 73.634 |
|  Passthrough | 4G | 200 | 10240 | 1000 | 0 | 198.96 | 1003.45 | 8.41 | 1031 | 99.39 | 72.57 |
|  Passthrough | 4G | 300 | 50 | 0 | 0 | 3428.49 | 87.34 | 89.97 | 265 | 92.26 | 521.286 |
|  Passthrough | 4G | 300 | 50 | 30 | 0 | 3433.29 | 87.24 | 94 | 247 | 91.87 | 542.874 |
|  Passthrough | 4G | 300 | 50 | 500 | 0 | 595.16 | 503.77 | 9.73 | 547 | 98.78 | 72.68 |
|  Passthrough | 4G | 300 | 50 | 1000 | 0 | 298.93 | 1002.77 | 6.91 | 1015 | 99.34 | 72.988 |
|  Passthrough | 4G | 300 | 1024 | 0 | 0 | 3296.58 | 90.88 | 88.12 | 269 | 92.98 | 479.055 |
|  Passthrough | 4G | 300 | 1024 | 30 | 0 | 3346.97 | 89.51 | 88.85 | 248 | 92.55 | 535.546 |
|  Passthrough | 4G | 300 | 1024 | 500 | 0 | 595.35 | 503.76 | 10.26 | 539 | 98.75 | 71.932 |
|  Passthrough | 4G | 300 | 1024 | 1000 | 0 | 298.79 | 1002.95 | 7.53 | 1019 | 99.34 | 72.026 |
|  Passthrough | 4G | 300 | 10240 | 0 | 0 | 1912.29 | 156.75 | 81.07 | 327 | 95.34 | 364.902 |
|  Passthrough | 4G | 300 | 10240 | 30 | 0 | 1983.69 | 151.09 | 71.14 | 321 | 95.59 | 296.736 |
|  Passthrough | 4G | 300 | 10240 | 500 | 0 | 593.17 | 505.72 | 12.89 | 571 | 98.68 | 72.024 |
|  Passthrough | 4G | 300 | 10240 | 1000 | 0 | 298.54 | 1002.95 | 6.15 | 1023 | 99.27 | 74.043 |
|  Passthrough | 4G | 500 | 50 | 0 | 0 | 3520.7 | 141.88 | 121.02 | 365 | 92.16 | 534.513 |
|  Passthrough | 4G | 500 | 50 | 30 | 0 | 3541.48 | 141.05 | 111.25 | 339 | 92.13 | 527.158 |
|  Passthrough | 4G | 500 | 50 | 500 | 0 | 982.22 | 509 | 22.39 | 639 | 97.35 | 71.886 |
|  Passthrough | 4G | 500 | 50 | 1000 | 0 | 497.53 | 1003.64 | 9.79 | 1047 | 98.94 | 71.646 |
|  Passthrough | 4G | 500 | 1024 | 0 | 0 | 3338.97 | 149.63 | 118.96 | 367 | 92.55 | 538.788 |
|  Passthrough | 4G | 500 | 1024 | 30 | 0 | 3328.31 | 150.03 | 129.93 | 357 | 91.97 | 581.823 |
|  Passthrough | 4G | 500 | 1024 | 500 | 0 | 979.47 | 510.33 | 23.51 | 643 | 97.34 | 72.104 |
|  Passthrough | 4G | 500 | 1024 | 1000 | 0 | 497.57 | 1003.56 | 9.24 | 1039 | 98.87 | 73.807 |
|  Passthrough | 4G | 500 | 10240 | 0 | 0 | 1919.47 | 260.5 | 103.43 | 493 | 95.46 | 353.475 |
|  Passthrough | 4G | 500 | 10240 | 30 | 0 | 1953.08 | 256 | 99.96 | 463 | 95.6 | 354.284 |
|  Passthrough | 4G | 500 | 10240 | 500 | 0 | 964.79 | 518.09 | 32.25 | 671 | 97.09 | 73.143 |
|  Passthrough | 4G | 500 | 10240 | 1000 | 0 | 497.26 | 1004.02 | 9.69 | 1047 | 98.82 | 73.212 |
|  Passthrough | 4G | 1000 | 50 | 0 | 0 | 3562.1 | 280.62 | 147.59 | 619 | 92.31 | 504.08 |
|  Passthrough | 4G | 1000 | 50 | 30 | 0 | 3530.55 | 283.25 | 154.61 | 611 | 92.14 | 525.014 |
|  Passthrough | 4G | 1000 | 50 | 500 | 0 | 1927.65 | 518.32 | 84.27 | 667 | 95.38 | 397.591 |
|  Passthrough | 4G | 1000 | 50 | 1000 | 0 | 988.74 | 1009.88 | 24.22 | 1159 | 97.14 | 72.061 |
|  Passthrough | 4G | 1000 | 1024 | 0 | 0 | 3268.45 | 305.89 | 180.54 | 647 | 92.21 | 592.275 |
|  Passthrough | 4G | 1000 | 1024 | 30 | 0 | 3267.52 | 305.96 | 177.64 | 651 | 92.36 | 560.137 |
|  Passthrough | 4G | 1000 | 1024 | 500 | 0 | 1909.05 | 523.26 | 87.44 | 683 | 95.5 | 397.976 |
|  Passthrough | 4G | 1000 | 1024 | 1000 | 0 | 987.9 | 1010.1 | 24.3 | 1159 | 97.13 | 72.783 |
|  Passthrough | 4G | 1000 | 10240 | 0 | 0 | 1919.65 | 520.92 | 157.65 | 927 | 95.62 | 344.273 |
|  Passthrough | 4G | 1000 | 10240 | 30 | 0 | 1898.89 | 526.64 | 143.83 | 863 | 95.72 | 369.918 |
|  Passthrough | 4G | 1000 | 10240 | 500 | 0 | 1695.38 | 589.37 | 151.26 | 911 | 95.37 | 451.451 |
|  Passthrough | 4G | 1000 | 10240 | 1000 | 0 | 968.67 | 1030.26 | 46.58 | 1223 | 96.74 | 71.79 |
|  Transformation | 4G | 50 | 50 | 0 | 0 | 2713.04 | 18.33 | 28.98 | 97 | 93.96 | 359.761 |
|  Transformation | 4G | 50 | 50 | 30 | 0 | 1484.54 | 33.6 | 24.36 | 92 | 95.97 | 309.743 |
|  Transformation | 4G | 50 | 50 | 500 | 0 | 99.45 | 503.07 | 6.04 | 511 | 99.47 | 73.227 |
|  Transformation | 4G | 50 | 50 | 1000 | 0 | 49.81 | 1002.7 | 6.53 | 1007 | 99.54 | 72.265 |
|  Transformation | 4G | 50 | 1024 | 0 | 0 | 2080.82 | 23.93 | 27.3 | 98 | 95.11 | 329.479 |
|  Transformation | 4G | 50 | 1024 | 30 | 0 | 1403.24 | 35.55 | 16.23 | 92 | 96.22 | 203.707 |
|  Transformation | 4G | 50 | 1024 | 500 | 0 | 99.45 | 503.02 | 4.37 | 509 | 99.44 | 73.575 |
|  Transformation | 4G | 50 | 1024 | 1000 | 0 | 49.77 | 1002.96 | 6.43 | 1011 | 99.55 | 72.045 |
|  Transformation | 4G | 50 | 10240 | 0 | 0 | 745.44 | 66.93 | 35.9 | 170 | 96.2 | 149.303 |
|  Transformation | 4G | 50 | 10240 | 30 | 0 | 668.51 | 74.64 | 27.6 | 155 | 95.84 | 158.818 |
|  Transformation | 4G | 50 | 10240 | 500 | 0 | 98.6 | 507.02 | 5.32 | 527 | 99.27 | 73.947 |
|  Transformation | 4G | 50 | 10240 | 1000 | 0 | 49.65 | 1005.44 | 3.91 | 1019 | 99.45 | 72.782 |
|  Transformation | 4G | 100 | 50 | 0 | 0 | 2771.52 | 35.96 | 40.38 | 134 | 93.76 | 354.721 |
|  Transformation | 4G | 100 | 50 | 30 | 0 | 2360.64 | 42.26 | 37.12 | 124 | 94.33 | 377.771 |
|  Transformation | 4G | 100 | 50 | 500 | 0 | 198.87 | 502.83 | 4.51 | 515 | 99.3 | 73.028 |
|  Transformation | 4G | 100 | 50 | 1000 | 0 | 99.62 | 1002.54 | 4.86 | 1007 | 99.48 | 72.732 |
|  Transformation | 4G | 100 | 1024 | 0 | 0 | 2215.68 | 45.01 | 42.53 | 143 | 94.51 | 402.598 |
|  Transformation | 4G | 100 | 1024 | 30 | 0 | 1985.8 | 50.25 | 32.71 | 125 | 95.09 | 336.494 |
|  Transformation | 4G | 100 | 1024 | 500 | 0 | 198.77 | 503.18 | 4.73 | 519 | 99.24 | 73.104 |
|  Transformation | 4G | 100 | 1024 | 1000 | 0 | 99.59 | 1002.7 | 4.87 | 1011 | 99.43 | 73.173 |
|  Transformation | 4G | 100 | 10240 | 0 | 0 | 754.77 | 132.33 | 61.59 | 297 | 95.84 | 160.539 |
|  Transformation | 4G | 100 | 10240 | 30 | 0 | 748.67 | 133.38 | 47.46 | 259 | 95.76 | 150.684 |
|  Transformation | 4G | 100 | 10240 | 500 | 0 | 194.13 | 515.05 | 15.22 | 579 | 98.84 | 72.825 |
|  Transformation | 4G | 100 | 10240 | 1000 | 0 | 99.1 | 1007.62 | 7.16 | 1039 | 99.25 | 73.174 |
|  Transformation | 4G | 200 | 50 | 0 | 0 | 2740.25 | 72.84 | 60.83 | 209 | 93.57 | 401.283 |
|  Transformation | 4G | 200 | 50 | 30 | 0 | 2669.61 | 74.79 | 55.87 | 184 | 93.65 | 391.688 |
|  Transformation | 4G | 200 | 50 | 500 | 0 | 396.89 | 503.68 | 7.7 | 539 | 98.88 | 73.047 |
|  Transformation | 4G | 200 | 50 | 1000 | 0 | 199.19 | 1003.08 | 5.58 | 1031 | 99.29 | 73.145 |
|  Transformation | 4G | 200 | 1024 | 0 | 0 | 2216.93 | 90.08 | 58.07 | 233 | 94.5 | 390.357 |
|  Transformation | 4G | 200 | 1024 | 30 | 0 | 2222.42 | 89.86 | 53.14 | 198 | 94.34 | 369.49 |
|  Transformation | 4G | 200 | 1024 | 500 | 0 | 395.98 | 504.9 | 8.99 | 551 | 98.66 | 73.091 |
|  Transformation | 4G | 200 | 1024 | 1000 | 0 | 199.02 | 1003.44 | 6.04 | 1031 | 99.2 | 73.686 |
|  Transformation | 4G | 200 | 10240 | 0 | 0 | 744.51 | 268.67 | 105.86 | 547 | 96.15 | 173.043 |
|  Transformation | 4G | 200 | 10240 | 30 | 0 | 722.82 | 276.75 | 97.63 | 523 | 95.54 | 185.715 |
|  Transformation | 4G | 200 | 10240 | 500 | 0 | 346.53 | 576.8 | 66.67 | 759 | 97.64 | 73.694 |
|  Transformation | 4G | 200 | 10240 | 1000 | 0 | 196.15 | 1018.21 | 22.95 | 1111 | 98.77 | 73.119 |
|  Transformation | 4G | 300 | 50 | 0 | 0 | 2721.32 | 110.09 | 75.45 | 285 | 93.41 | 403.685 |
|  Transformation | 4G | 300 | 50 | 30 | 0 | 2694.71 | 111.18 | 76.26 | 253 | 93.36 | 417.383 |
|  Transformation | 4G | 300 | 50 | 500 | 0 | 592.45 | 506.17 | 13.43 | 583 | 98.15 | 72.839 |
|  Transformation | 4G | 300 | 50 | 1000 | 0 | 298.67 | 1003.11 | 6.46 | 1031 | 99.09 | 72.811 |
|  Transformation | 4G | 300 | 1024 | 0 | 0 | 2278.51 | 131.55 | 81.86 | 317 | 94.21 | 393.967 |
|  Transformation | 4G | 300 | 1024 | 30 | 0 | 2253.48 | 132.98 | 69.87 | 285 | 94.19 | 357.212 |
|  Transformation | 4G | 300 | 1024 | 500 | 0 | 590.66 | 507.98 | 14.75 | 591 | 97.81 | 73.206 |
|  Transformation | 4G | 300 | 1024 | 1000 | 0 | 298.38 | 1003.42 | 6.57 | 1031 | 98.94 | 73.105 |
|  Transformation | 4G | 300 | 10240 | 0 | 0 | 734.01 | 408.81 | 139.47 | 775 | 95.87 | 175.849 |
|  Transformation | 4G | 300 | 10240 | 30 | 0 | 735.98 | 407.69 | 131.67 | 743 | 95.77 | 172.766 |
|  Transformation | 4G | 300 | 10240 | 500 | 0 | 411.54 | 728.17 | 121.61 | 963 | 96.83 | 73.155 |
|  Transformation | 4G | 300 | 10240 | 1000 | 0 | 279.21 | 1072.73 | 78.45 | 1327 | 98.08 | 71.945 |
|  Transformation | 4G | 500 | 50 | 0 | 0 | 2700.39 | 185.06 | 108.25 | 423 | 93.03 | 429.801 |
|  Transformation | 4G | 500 | 50 | 30 | 0 | 2709.56 | 184.38 | 104.66 | 397 | 92.91 | 435.886 |
|  Transformation | 4G | 500 | 50 | 500 | 0 | 961.23 | 520.23 | 34.99 | 687 | 95.79 | 72.204 |
|  Transformation | 4G | 500 | 50 | 1000 | 0 | 497 | 1004.2 | 10.24 | 1063 | 98.42 | 72.903 |
|  Transformation | 4G | 500 | 1024 | 0 | 0 | 2205.1 | 226.69 | 109.18 | 489 | 94.06 | 375.639 |
|  Transformation | 4G | 500 | 1024 | 30 | 0 | 2162.94 | 231.1 | 103.69 | 471 | 94.18 | 365.016 |
|  Transformation | 4G | 500 | 1024 | 500 | 0 | 947.73 | 527.35 | 55.66 | 675 | 95.97 | 201.558 |
|  Transformation | 4G | 500 | 1024 | 1000 | 0 | 494.93 | 1008.28 | 16.43 | 1103 | 98.13 | 72.512 |
|  Transformation | 4G | 500 | 10240 | 0 | 0 | 734.41 | 680.59 | 189.07 | 1175 | 95.72 | 226.136 |
|  Transformation | 4G | 500 | 10240 | 30 | 0 | 735.25 | 679.36 | 181.36 | 1143 | 95.73 | 212.913 |
|  Transformation | 4G | 500 | 10240 | 500 | 0 | 534.82 | 933.64 | 189.3 | 1279 | 96.22 | 137.478 |
|  Transformation | 4G | 500 | 10240 | 1000 | 0 | 375.94 | 1328.26 | 189.98 | 1671 | 96.85 | 72.731 |
|  Transformation | 4G | 1000 | 50 | 0 | 0 | 2693.7 | 371.22 | 185.01 | 775 | 91.81 | 540.386 |
|  Transformation | 4G | 1000 | 50 | 30 | 0 | 2710.2 | 368.95 | 180.8 | 759 | 91.83 | 554.152 |
|  Transformation | 4G | 1000 | 50 | 500 | 0 | 1803.39 | 554.05 | 121.26 | 723 | 94.42 | 447.128 |
|  Transformation | 4G | 1000 | 50 | 1000 | 0 | 972.04 | 1027.23 | 69.08 | 1215 | 95.74 | 243.98 |
|  Transformation | 4G | 1000 | 1024 | 0 | 0 | 2207.49 | 452.95 | 182.46 | 895 | 92.94 | 436.386 |
|  Transformation | 4G | 1000 | 1024 | 30 | 0 | 2191.72 | 455.2 | 176.16 | 895 | 93.08 | 473.533 |
|  Transformation | 4G | 1000 | 1024 | 500 | 0 | 1684.48 | 593.16 | 131.7 | 843 | 94.48 | 431.158 |
|  Transformation | 4G | 1000 | 1024 | 1000 | 0 | 957.83 | 1041.97 | 90.83 | 1295 | 95.32 | 250.769 |
|  Transformation | 4G | 1000 | 10240 | 0 | 0 | 712.22 | 1402.18 | 292.9 | 2175 | 95.35 | 312.108 |
|  Transformation | 4G | 1000 | 10240 | 30 | 0 | 722.16 | 1382.48 | 259.43 | 2095 | 95.09 | 283.574 |
|  Transformation | 4G | 1000 | 10240 | 500 | 0 | 670.72 | 1487.79 | 346.19 | 2543 | 93.99 | 386.781 |
|  Transformation | 4G | 1000 | 10240 | 1000 | 0 | 552.15 | 1805.38 | 373.39 | 2799 | 95.29 | 336.271 |
