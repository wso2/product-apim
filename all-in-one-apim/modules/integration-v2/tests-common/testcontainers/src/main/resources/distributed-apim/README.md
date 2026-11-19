# Manual distributed APIM component images

This directory contains the manual image recipe used by the integration-v2
distributed APIM runtime. It produces three independent WSO2 APIM component
images:

- Control Plane (CP), started with `api-cp.sh`.
- Traffic Manager (TM), started with `traffic-manager.sh`.
- Universal Gateway, started with `gateway.sh`.

The images are deliberately component-only. They do not contain the developer
setup's full deployment TOML, a database, or a second backend service. The
distributed composite creates a block-scoped MySQL container and generates the
effective CP, TM, and Gateway TOMLs by merging defaults extracted from the
component images with small topology overlays and any component-qualified test
overlay.

## Inputs and prerequisites

The builder requires:

- a CP distribution ZIP;
- a TM distribution ZIP;
- a Universal Gateway distribution ZIP; and
- the MySQL Connector/J JAR compatible with the product ZIPs.

Each ZIP must contain its component startup script under `bin/`. The CP ZIP
must also contain the product database scripts used by the runtime. The
builder checks that the input files exist and that the expected startup scripts
are executable before the Docker builds begin. It does not verify
product-version compatibility between the ZIPs and the connector, so those
versions must be selected deliberately.

Docker must be available to the shell running Maven. On Colima, use the
same Docker environment variables used by the focused integration suites:

```bash
export DOCKER_HOST=unix://$HOME/.colima/default/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
export TESTCONTAINERS_HOST_OVERRIDE=192.168.64.2
```

## Build the images

The three component images are built by the `build-distributed-apim-*-docker-image`
Maven executions in `tests-common/testcontainers/pom.xml`. Build the product
distributions first, then invoke the integration-v2 lifecycle from the
`modules/integration-v2` directory:

```bash
mvn pre-integration-test -Pinclude-distributed-images -pl tests-common/testcontainers -am
```

The executions read these Maven properties, whose defaults point to the ZIPs
produced by the repository reactor:

```text
distributed.apim.cp.zip
distributed.apim.tm.zip
distributed.apim.gateway.zip
distributed.apim.cp.image.name
distributed.apim.tm.image.name
distributed.apim.gateway.image.name
```

Override the ZIP and image-name properties with `-D` when using distributions
from another location. For example:

```bash
mvn pre-integration-test -Pinclude-distributed-images -pl tests-common/testcontainers -am \
  -Ddistributed.apim.cp.zip=/path/to/wso2am-acp-<version>.zip \
  -Ddistributed.apim.tm.zip=/path/to/wso2am-tm-<version>.zip \
  -Ddistributed.apim.gateway.zip=/path/to/wso2am-universal-gw-<version>.zip \
  -Ddistributed.apim.cp.image.name=distributed-apim-cp:<version>-jdk21 \
  -Ddistributed.apim.tm.image.name=distributed-apim-tm:<version>-jdk21 \
  -Ddistributed.apim.gateway.image.name=distributed-apim-gateway:<version>-jdk21
```

The default image names are:

```text
distributed-apim-cp:4.7.0-SNAPSHOT-jdk21
distributed-apim-tm:4.7.0-SNAPSHOT-jdk21
distributed-apim-gateway:4.7.0-SNAPSHOT-jdk21
```

The same image-name properties are passed to the test JVM, so the image names
used for building and testing stay aligned. Changing an image tag requires
changing the corresponding image-name properties as well; changing only an
unrelated Docker tag does not retag an existing image or update the test JVM.

The build context is temporary and is removed after each image build. The
input ZIPs and connector are not modified. Maven resolves the MySQL
Connector/J version and stages it for each build. The Dockerfile installs each
component under `/opt/wso2`, copies the connector to
`repository/components/lib/mysql-connector-j.jar`, and starts the selected
component through `start-component.sh`.

## Runtime wiring

The images are consumed by `DistributedDynamicApimContainer`, normally
selected by `DistributedLifecycleListener`. During Maven's
`pre-integration-test` phase, the framework extracts each image's default
`deployment.toml` and the CP database schemas into
`target/distributed-apim/defaults`. Test execution reads those staged files;
component ZIP paths are build-time inputs for image creation only and are not
required by the test JVM.

The composite owns and starts resources in this order:

```text
MySQL → CP → shared registry initialization → TM → Universal Gateway
```

Each APIM component container is capped at 1 CPU. The distributed CP container has a 1,536 MiB memory cap
and its server JVM uses `-Xms512m -Xmx768m`. The TM container has an 896 MiB memory cap and the Gateway
container has a 1,536 MiB memory cap; both server JVMs use `-Xms256m -Xmx384m`. The shared Node backend
container has a 1,024 MiB memory cap in distributed topology.
External Identity Server containers started by distributed blocks have a 1,024 MiB memory cap and use
`-Xms512m -Xmx768m`; all-in-one blocks retain `-Xms1g -Xmx1536m` without a Docker memory cap.
The shared Solace helper caps PubSub+ at 0.25 CPU and 512 MiB of RAM, and its control-plane shim
at 0.25 CPU and 100 MiB. The shared Squid proxy is capped at 0.5 CPU and 50 MiB.
These helper limits apply to both topologies, with swap disabled for each helper container.
The CP, TM and Gateway base overlays set `diagnostic_tool.server_configuration.diagnostic_tool_enabled = false`
to reduce memory use during concurrent test blocks. The Diagnostics JVM starts briefly, then exits after reading
the setting; automatic Diagnostics captures are disabled.
The existing `apimCpuLimit` block parameter, when present, overrides the CPU cap for all
three components; the default remains 1 CPU.

Each block's MySQL container is capped at 0.5 CPU and 384 MiB of RAM, with a swap allowance of up to 96 MiB
(`DistributedMemoryLimits.memoryAndSwapLimitBytes`: 25% of the memory cap). It uses a 64 MiB InnoDB
buffer pool, a 16 MiB log buffer, a 16 MiB TempTable RAM threshold, and table caches of 512 entries. Performance
Schema is disabled. Database files use the image's private anonymous disk volume instead of tmpfs;
Testcontainers removes the volume when it removes the container, keeping database state isolated between blocks.

It stops them in reverse order and closes the private block network. MySQL is
available inside that network as `mysql:3306`; CP, TM, and Gateway use the
canonical component aliases `apim-cp`, `apim-tm`, and `apim-gw`. Host URLs are
published dynamically through Testcontainers, so callers continue to use the
same `ApimRuntime` URL methods as with the all-in-one container.

The runtime applies these classpath base overlays:

- `cp-base-overlay.toml`: CP databases, event hub, and TM endpoints;
- `tm-base-overlay.toml`: TM databases and CP event hub endpoints;
- `gateway-base-overlay.toml`: Gateway shared database, CP artifact/key
  manager/event hub endpoints, and TM throttling endpoints.

The merge order is product defaults, distributed base overlay, component extra
overlay, and generated runtime values. This keeps the developer setup's
full-file TOMLs out of the image and prevents runtime-generated addresses from
being overwritten by a test overlay.

Each component also gets the template overlays under `templates/` (listed in
`DistributedDynamicApimContainer.TEMPLATE_OVERLAYS`). An overlay is an XML
skeleton of the literal values to set; `J2TemplateOverlay` writes them into the
component's own template, which the image-defaults extraction stages beside its
`deployment.toml` as `<component>/templates/`, and the result is copied into
`repository/resources/conf/templates/` before boot.
`templates/repository/conf/registry.xml.j2` turns the registry resource cache
off at the registry root, as the all-in-one lane's basic overlay does. This is
not a switch for the separate local `REG_PATH_CACHE`. Each distributed base
overlay also sets `governance_data.enable_cache = false` and
`config_data.enable_cache = false`, disabling both mounted resource caches.

## Throttle-data startup prerequisites

CP manages policies and publishes their changes; TM owns local Siddhi policy execution. Runtime-derived values
set `apim.throttling.enable_policy_deployment=false` on CP and `true` on TM after block overlays. The separate
`enable_policy_deploy` setting is retained. This avoids a second policy engine and duplicate tenant warm-up on CP
while preserving TM enforcement and Gateway's counter/decision routing.

The distributed lifecycle registers a CP initializer on the composite. Startup
is MySQL, CP, registry-service readiness, shared throttle-data hierarchy
initialization, TM, then Gateway. The callback uses the normal supertenant
integration actor and registry SOAP service while CP is the only component
writing this hierarchy. It runs for every distributed block, before TM has a
path-cache entry and before Gateway starts its asynchronous broker subscriptions.
Callback failures use the composite's normal reverse teardown.

Registry hierarchy initialization uses one bounded window (180 seconds normally, the CP startup budget during
its initializer). Reads can be retried during warm-up; each missing collection gets one creation request. A lost
creation response is reconciled through reads, without replaying the mutation. Every path must become readable
and identify itself as a collection; authentication, storage and explicit creation errors still fail. Bootstrap
requests do not replace a scenario's assertion response. Existing live broker readiness gates run afterwards.

On a transport failure or exhausted initialization budget, lifecycle callers save bounded diagnostic snapshots
under `target/logs/registry-bootstrap/`: CP threads/heap and, in distributed mode, MySQL process/lock state. The
capture is read-only and cannot turn an initialization failure into a pass.

The later `bootstrapThrottleData` block option retains tenant-specific setup
and checks readiness; it performs no distributed recovery restarts. Readiness
requires an active `throttledata` subscription from the current Gateway on
both CP's event-hub broker and TM's throttling broker. It uses the read-only
`AndesAdminService.getSubscriptions` operation and validates enabled primary
publishers. The HA-only `jmsEventPublisher2` and Gateway slow-start warnings do
not establish readiness. Direct low-level composite users can register their
own initializer with `withControlPlaneInitializer`.

## Component-specific test configuration

External-key-manager blocks stage the augmented client truststore in CP, TM and Gateway before startup. TM
needs it for CP policy and revoked-token requests when a block selects the fixed SSO TLS certificate. When
`receiveExternalIsNotifications` is enabled, CP receives the `wso2am` network alias and all CP HTTPS service
URLs use that certificate-covered authority. Other blocks retain the `apim-cp` authority. Trust and hostname
verification remain enabled; the SSO certificate covers `wso2am`, not `apim-cp`.

Distributed mode rejects ambiguous all-component parameters. Use qualified
names for extra TOMLs and boot files:

```text
tomlExtraOverlayPath.cp
tomlExtraOverlayPath.tm
tomlExtraOverlayPath.gateway

serverFilesToCopy.cp
serverFilesToCopy.tm
serverFilesToCopy.gateway
```

For example, a CP-only overlay is passed as:

```xml
<parameter name="tomlExtraOverlayPath.cp"
           value="src/test/resources/artifacts/configFiles/example/deployment.toml"/>
```

The unqualified legacy forms are rejected instead of being silently applied to
the wrong component. The ordinary block parameters such as tenant-user
initialization, backend initialization, external identity-server startup, and
email mode remain lifecycle concerns and are handled by the distributed
listener/composite.

`registryLogMillisecondPrecision` is a boolean block parameter, default `false`. The two DevPortal
blocks asserting registry indexing enable it to provision and verify `TIMESTAMP(3)` on the shared
MySQL `REG_LOG.REG_LOGGED_TIME` before CP starts. All other blocks keep the shipped schema. See the
[integration-v2 parameter reference](../../../../../../README.md#registry-activity-timestamp-precision)
for the strict timestamp-cursor rationale and hosted database behavior.

## Suite layout

`tests-integration/cucumber-tests/src/test/resources/testng-v2.xml` defines the block and runner layout for both
topologies. `testng-v2_distributed.xml` mirrors its block and runner order, with four concurrent blocks and matching
per-block class parallelism, lifecycle locks, user provisioning, and readiness parameters except declared omissions.
Distributed test names and block labels have a `Distributed-` / `distributed-` prefix so logs and reports identify
the topology. Runner classes and feature files are shared.

Run `python3 docs/devs/suite_alignment.py` from `integration-v2` to validate the suites without starting containers.
Adding a runner to either suite requires its registration in the matching block of the other suite, or a block-scoped
declaration with a reason in [`suite-alignment.ignore`](../../../../../../docs/devs/suite-alignment.ignore).
The standalone validator checks registrations, order, scheduling and shared local parameters; CI runs the validator
and rejects undeclared differences and stale declarations before building images. Solace lifecycle coverage and broker
initialization are omitted from distributed within the CI resource budget, as declared in that file.

The distributed suite routes the block's configuration to the component that consumes it:

- CP receives publisher/devportal settings, workflow executor JARs, API-generation templates, network access
  policies, and SMTP configuration.
- Gateway receives invocation settings, listener/sender TLS settings, custom authentication handlers, analytics,
  and outbound WebSocket proxy profiles.
- TM receives conditional throttling flags; settings consumed by multiple components use the same overlay file
  for each relevant component.

Blocks running `GatewayRestArtifactsRunner` also configure Gateway's APIM database and tenant loading, with the
same test encryption key on CP, TM, and Gateway. The custom-handler block retains that same topology wiring.
Gateway's base overlay sets `event_waiting_time = 0`, matching the all-in-one test baseline: the single CP and
Gateway use the same database and do not need an HA replication delay before fetching an artifact.

Keep block grouping, runner order, and parallelism aligned when updating either suite. Topology-specific fixes
belong in component configuration or infrastructure helpers. To compile the framework and suites without starting
containers, run from the `integration-v2` directory:

```bash
mvn -pl tests-integration/cucumber-tests -am -DskipTests test-compile
```

## Scope and current limitations

This is a manual developer image workflow, not a CI image publication process.
The distributed product suite is
`tests-integration/cucumber-tests/src/test/resources/testng-v2_distributed.xml`.
Runtime verification of the aligned suite is a separate step from layout and compilation checks.

The following boundaries are intentional and should not be mistaken for image
failures:

- distributed JaCoCo dump wiring is not implemented yet;
- `withCoverage()` throws `UnsupportedOperationException` for the distributed composite; distributed suites must
  run without integration coverage enabled;
- CP is the backing implementation for the legacy generic file and command
  access methods, while dedicated Gateway log access is provided separately;
- external key-manager trust and external-IS notification aliases are optional
  runtime wiring, not baked into the images;
- the image recipe itself does not start APIM or validate the complete
  CP/TM/Gateway lifecycle; focused composite and topology tests provide that
  verification.

No all-in-one ZIP is accepted as a substitute for a component ZIP. This keeps
the topology explicit and prevents a stripped or incorrectly packaged image
from appearing to be a valid distributed component.
