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
effective CP, TM, and Gateway TOMLs by merging each ZIP's product defaults with
small topology overlays and any component-qualified test overlay.

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

Docker must be available to the shell running the script. On Colima, use the
same Docker environment variables used by the focused integration suites:

```bash
export DOCKER_HOST=unix:///Users/nimsara/.colima/default/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
export TESTCONTAINERS_HOST_OVERRIDE=192.168.64.2
```

## Build the images

From this directory, run:

```bash
./build-images.sh \
  --cp-zip /path/to/wso2am-acp-4.7.0-SNAPSHOT.zip \
  --tm-zip /path/to/wso2am-tm-4.7.0-SNAPSHOT.zip \
  --gateway-zip /path/to/wso2am-universal-gw-4.7.0-SNAPSHOT.zip \
  --connector /path/to/mysql-connector-j-8.4.0.jar
```

The default output tags are:

```text
distributed-apim-cp:4.7.0-SNAPSHOT
distributed-apim-tm:4.7.0-SNAPSHOT
distributed-apim-gateway:4.7.0-SNAPSHOT
```

The script also accepts `--tag VERSION`. The current
`DistributedDynamicApimContainer` uses the default `4.7.0-SNAPSHOT` image tags
as constants, so a non-default tag is useful only when the runtime image names
are updated or made configurable as part of a future change. Do not assume
that passing `--tag` alone changes the image used by the test suite.

The build context is temporary and is removed after each image build. The
input ZIPs and connector are not modified. The Dockerfile installs each
component under `/opt/wso2`, copies the connector to
`repository/components/lib/mysql-connector-j.jar`, and starts the selected
component through `start-component.sh`.

## Runtime wiring

The images are consumed by `DistributedDynamicApimContainer`, normally
selected by `DistributedLifecycleListener`. Supply the three component ZIPs
to the test JVM when the container is constructed:

```text
-Ddistributed.apim.cp.zip=/path/to/wso2am-acp-<version>.zip
-Ddistributed.apim.tm.zip=/path/to/wso2am-tm-<version>.zip
-Ddistributed.apim.gateway.zip=/path/to/wso2am-universal-gw-<version>.zip
```

The composite owns and starts resources in this order:

```text
MySQL → CP → TM → Universal Gateway
```

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

## Component-specific test configuration

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

## Scope and current limitations

This is a manual developer image workflow, not a CI image publication process.
The current distributed focused suite is
`tests-integration/cucumber-tests/src/test/resources/testng-v2_distributed.xml`.
Run focused suites only while this topology is being developed.

The following boundaries are intentional and should not be mistaken for image
failures:

- distributed JaCoCo dump wiring is not implemented yet;
- `withCoverage()` is currently a no-op for the distributed composite;
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
