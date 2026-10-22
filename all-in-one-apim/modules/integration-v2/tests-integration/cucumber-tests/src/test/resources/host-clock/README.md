# Docker host clock preparation and monitoring

Both v2 topologies register `ClockStabilityListener`. It starts one small, unprivileged observer container per
suite using the already-built Node backend image. The observer has no network and a 0.1 CPU cap. In the
distributed topology it has a 128 MiB memory limit with up to 32 MiB of swap
(`DistributedMemoryLimits.memoryAndSwapLimitBytes`); the all-in-one observer has no configured memory limit.
It samples the Docker host's real-time and monotonic clocks every 200 ms. This also observes the
correct clock when Maven runs on macOS and Docker runs in an Ubuntu VM.

Before any block starts, the listener requires 20 seconds of healthy samples. Throughout startup, scenarios,
and teardown, it records samples under `target/logs/host-clock/<run-id>/`. Each real-time read is bracketed by
monotonic reads, accounting for CPU scheduling/throttling within a sample. A backward real-time sample,
a real-time/monotonic discrepancy outside that bracket by over 50 ms, an interrupted interval over 10 seconds,
or loss of the observer makes the run fail. The 50 ms threshold allows millisecond sampling precision;
the observer cannot prove the absence of smaller adjustments or opposing adjustments within one sample interval.
An individual sampling bracket exceeding one second also fails because it cannot establish clock stability.
Gradual NTP correction is allowed. Clock failures remain sticky even if later samples are healthy.

The listener checks each test invocation and the end of the suite, so an invalid environment cannot produce
a successful Maven run. Existing product failures are retained, with the clock failure attached as an additional
cause. It does not change product code, feature assertions, selected scenarios, block/runner order, or concurrency.

## Ubuntu CI

`maven.yml` installs Chrony and invokes the preparation script before either topology's Maven command. The script:

1. Requires an Ubuntu Docker host with no running containers, and records its time services and configuration.
2. Stops competing NTP services and synchronizes Chrony before APIM exists. It records the available sources
   and requests a measurement burst only for network NTP servers/peers. PHC-only Azure runners use their existing
   reference clock. Both paths must pass the same bounded synchronization checks; preparation still fails when
   no usable source synchronizes. Failed clock commands print their phase, exit status and output to the job log.
3. Configures Chrony to slew corrections, disables automatic clock steps, and limits slew rate to 1000 ppm.
   Leap-second correction also uses slewing. The policy remains in effect if Chrony restarts during the run.
4. Writes a preparation marker identifying the Linux boot. CI passes its path as
   `-Dapim.clock.prepared.file=.../prepared.json`; the listener rejects a marker for a different Docker host.

Other sources of clock steps, including a hypervisor, are detected by the observer; configuring Chrony alone
cannot control them. On local Ubuntu VMs with an active Lima guest agent, the script additionally removes
`CAP_SYS_TIME` from that service using a runtime systemd override, so its `SyncTime` RPC cannot step the clock.
Its networking functions remain available. This branch is not used on native GitHub Ubuntu runners.

From the `cucumber-tests` module, run on the Ubuntu Docker host before starting containers:

```bash
sudo apt-get install -y chrony python3 util-linux
mkdir -p target/logs/host-clock/preparation
sudo bash src/test/resources/host-clock/prepare-test-clock.sh prepare \
  "$PWD/target/logs/host-clock/preparation"
```

On a persistent development VM, restore its saved service/configuration state after testing:

```bash
sudo bash src/test/resources/host-clock/prepare-test-clock.sh restore \
  "$PWD/target/logs/host-clock/preparation"
```

Preparation modifies the Ubuntu host's clock services and Chrony configuration. Use it on a dedicated test host;
restore uses the same diagnostic directory and does not install or remove packages. Start each preparation with
a fresh diagnostic directory. CI runners are disposable; their existing report upload archives `target/logs/**`.

## Focused framework verification

`ClockSampleDetectorTest` exercises backward and forward adjustments, sampling loss and failure retention without
changing any system clock. `ClockStabilityVerificationTest` runs an inner TestNG suite that terminates only its
own observer. Observer loss during a test and after the final test must both fail the execution; the enclosing verification asserts
these failures. TestNG reports are retained before a late infrastructure failure is propagated. Product scenario
verification still uses the existing throttling runners and feature files.

After building the integration-v2 reactor dependencies, run this focused framework suite from `cucumber-tests`:

```bash
mvn test -Dsurefire.suite.xml=testng-fv-host-clock.xml
```

It runs 11 checks. The two deliberate observer failures belong to inner verification suites: the outer suite
passes only when those failures are correctly detected. They do not stop APIM or change the system clock.
See the [Chrony configuration reference](https://chrony-project.org/doc/4.5/chrony.conf.html) for `makestep`,
`maxslewrate`, and `leapsecmode` semantics.
