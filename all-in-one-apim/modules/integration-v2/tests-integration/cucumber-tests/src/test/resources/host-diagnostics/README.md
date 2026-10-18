# Ubuntu host diagnostics

Both integration-v2 CI lanes wrap the existing Maven command with `run-with-host-diagnostics.py`. The wrapper
starts a transient systemd service on the Ubuntu Docker host, then runs Maven with its original arguments,
environment and exit status. The collector runs independently of Maven and TestNG. It does not change product
configuration, assertions, suite ordering, concurrency, or any existing container/browser limits.

## Sampling and evidence

- Every **5 seconds**: host memory (bytes), swap, CPU ticks and utilization, reclaim/swap/OOM counters,
  CPU/memory/I/O pressure, disk counters and free space; the largest 20 processes by RSS plus Java, Chromium,
  Docker and runner processes; current container and browser cgroup metrics. Process RSS can include containers:
  do not add process RSS to container memory totals. Disk counters are per device, not an aggregate across layers.
  Container `memory.current` includes charged cache and kernel memory; it is not JVM heap usage and can differ
  from Docker CLI's cache-adjusted memory display.
  Filesystem space/inodes are recorded separately for `/`, Docker's data directory and the output directory;
  this matters when the workspace is a separate mount. Filesystems with the same device ID share capacity.
- Every **60 seconds**: a summary relayed to the GitHub job log, including available memory, swap usage,
  host pressure, per-container CPU throttling and OOM counters, and observed memory/sampling extrema. The first
  sample and final shutdown also emit summaries. CPU counters are cumulative; compare consecutive samples
  for interval usage/throttling. Host CPU percentages exclude I/O wait, which is reported separately.
- Continuously: Docker lifecycle/OOM events and kernel journal messages. On `die`/`oom`, container inspection
  attempts to retain exit status and `OOMKilled`. Removal may race this inspection; earlier samples/events remain.
- Container discovery records image, name, network aliases/IPs, configured limits and cgroup path. These allow
  correlation with block/component log messages. Full Docker inspection, environment variables, command lines,
  arbitrary Docker labels and product application logs are not collected.

All records have UTC and host monotonic timestamps. Each sample includes the collection duration and the
gap since the previous sample. Missing Docker/cgroup/journal access is recorded explicitly rather than
interpreted as zero usage. Metadata includes boot ID, CPU count, OS/kernel/Docker versions and sampling settings.

Evidence is written under `cucumber-tests/target/logs/host-diagnostics/`:

| File | Contents |
|---|---|
| `metadata.json` | Initial host and collector identity |
| `samples.jsonl` | Detailed samples, line-flushed after each write |
| `events.jsonl` | Docker lifecycle events, container metadata and collector errors |
| `kernel.jsonl` | Kernel journal, including OOM messages |
| `summaries.jsonl`, `summary.json` | Summary history and latest/final summary |
| `service-unit.txt`, `service-start.log` | Transient service identity and startup result |

JSONL files rotate with numbered suffixes (highest number is oldest). Samples retain up to 256 MiB,
events 32 MiB, kernel records 16 MiB and summaries 8 MiB. Oldest records are discarded only when those bounds
are reached. The final summary retains memory extrema and the largest sampling gap for the whole run.
Line flushing makes completed records visible immediately; it does not guarantee survival of a host crash.

## Lifecycle and dependencies

Requires Linux cgroup v2, Python 3, Docker CLI/local socket, systemd/journalctl, and noninteractive sudo
(available on the GitHub Ubuntu runner). No additional packages or Python dependencies are installed.
The collector reads `/proc`, `/sys/fs/cgroup`, Docker and the kernel journal as root. It only writes its own
diagnostics directory. Its service is limited to 0.1 CPU, 192 MiB RAM and 32 tasks, with a seven-hour safety
timeout. Collector failures emit warnings and do not replace Maven's result. Missing metrics are diagnostic
limitations, not product test failures. The collector does not receive a reduced OOM score or change host settings.

The wrapper forwards cancellation signals to Maven's process group and stops the collector on exit.
A separate `always()` workflow step provides idempotent cleanup before the existing `target/logs/**`
artifact upload. A fresh output directory is required for each execution.

On the Ubuntu **Docker host**, from `integration-v2`:

```bash
python3 tests-integration/cucumber-tests/src/test/resources/host-diagnostics/run-with-host-diagnostics.py \
  --output tests-integration/cucumber-tests/target/logs/host-diagnostics -- \
  mvn test -pl tests-integration/cucumber-tests -am -Dsurefire.suite.xml=testng-v2_distributed.xml
```

For Maven on macOS with a Linux Docker VM, run the collector inside that VM and store/relay its evidence through
the shared workspace. `--start-only` starts the service without a child command; use `--stop` with the same output
directory after Maven exits. Relay updates to the shared `summary.json` locally during Maven execution.
Running the collector on macOS would measure the wrong host; CI needs no such split.

Unprivileged regression checks:

```bash
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -v \
  -s tests-integration/cucumber-tests/src/test/resources/host-diagnostics
```

## Runner loss

The existing artifact upload archives all these files when the runner can execute post-test steps. If the VM
dies or the runner cannot execute steps, `always()` cannot upload local files. The periodic GitHub log summaries
preserve metrics already transmitted, with gaps during a stall. Persisting raw samples across VM destruction
would require periodic off-host checkpointing; this collector does not send data to external storage.

See Linux [PSI](https://docs.kernel.org/accounting/psi.html) and
[cgroup v2](https://docs.kernel.org/admin-guide/cgroup-v2.html) for pressure, CPU throttling and memory event semantics.
