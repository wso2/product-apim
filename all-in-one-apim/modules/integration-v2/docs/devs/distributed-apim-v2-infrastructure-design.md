# Distributed APIM v2 infrastructure design

**Status:** Implemented through the composite runtime and focused Publisher/Gateway lane; broader topology coverage remains incremental
**Date:** 2026-08-30  
**Scope:** A drop-in distributed replacement for the v2 all-in-one block lifecycle

## 1. Executive decision

The proposed distributed topology is feasible, but it must be implemented as a composite runtime abstraction rather than as a subclass that pretends three independent APIM processes are one Docker container.

The intended suite-level switch is:

    <listener class-name="org.wso2.am.integration.cucumbertests.utils.listeners.DistributedLifecycleListener"/>

The listener must preserve the existing block-facing contract: the same TestNG parameters, TestContext keys, URL meanings, readiness/failure semantics, and feature steps. The new runtime object should own one Control Plane, one Traffic Manager, one Universal Gateway, and one MySQL container.

The first implementation target was a focused distributed lane containing the current Publisher and Gateway blocks. That lane is now present as `testng-v2_distributed.xml`; it is the controlled verification vehicle while the remaining blocks are added incrementally.

## 2. Current all-in-one contract

BlockLifecycleListener runs once per TestNG <test> block. When blockLabel is present it creates a private network, optionally attaches the shared Node backend, creates one DynamicApimContainer, applies the resolved TOML, starts APIM, waits for readiness, provisions optional users/stores, optionally starts external IS and Platform Gateway, and tears everything down at block finish.

The shared TestContext contract is:

| Key | Meaning |
|---|---|
| blockApimContainer | APIM runtime object |
| baseUrl | APIM management and portal HTTPS base URL |
| baseGatewayUrl | Gateway HTTPS invocation base URL |
| baseGatewayWsUrl | Gateway WebSocket base URL |
| baseGatewayWssUrl | Secure Gateway WebSocket base URL |
| baseWebSubEventReceiverUrl | WebSub receiver base URL |
| gatewayClientIp | Effective gateway client IP |

The distributed replacement must publish the same keys with the same meanings. Physical ownership may change: baseUrl will point to CP while gateway accessors will point to the Universal Gateway.

## 3. Findings from the developer distributed setup

The feasibility study also used a developer distributed setup that is outside this repository. Its original
machine-local source is intentionally not recorded here; the durable in-repository implementation references are
the distributed container resources and README listed in Section 16.

The setup contains:

| Component | Pack | Developer host offset | Responsibility |
|---|---|---:|---|
| Control Plane | wso2am-acp | 0 | Portals, management APIs, event publisher |
| Traffic Manager | wso2am-tm | 1 | Throttling decision and event subscriber |
| Universal Gateway | wso2am-universal-gw | 2 | Runtime API traffic |
| MySQL | mysql:8.4.0-oraclelinux8 | host 3316 | WSO2AM_DB and WSO2AM_SHARED_DB |

The developer docker-compose file starts only MySQL. The three WSO2 components are unpacked onto the host and started with api-cp.sh, traffic-manager.sh, and gateway.sh. This is a configuration reference, not a Testcontainers lifecycle implementation.

The developer setup has several assumptions that must be removed:

* localhost means the same host process, but would mean the current container inside Docker;
* fixed offsets are used because all processes share one host;
* process IDs and logs are host-owned;
* the full TOMLs are environment-specific replacements, not base-plus-overlay configuration;
* MySQL is not isolated and owned by a TestNG block;
* component readiness is checked through host ports rather than dependency-aware probes.

## 4. Proposed composite runtime

DistributedDynamicApimContainer should be a composition object:

    DistributedDynamicApimContainer
      - MySQL
      - APIM Control Plane
      - APIM Traffic Manager
      - APIM Universal Gateway
      - private Docker network

It need not extend GenericContainer. Java inheritance cannot make three independent containers semantically equivalent to one GenericContainer, and inherited methods such as execInContainer would be misleading.

Instead, introduce a small ApimRuntime contract implemented by DynamicApimContainer and DistributedDynamicApimContainer. The contract should include:

* start and idempotent stop;
* servlet HTTP/HTTPS URL accessors;
* Gateway HTTP/HTTPS/WebSocket/Secure-WebSocket URL accessors;
* WebSub receiver URL and gateway client IP accessors;
* component-aware server-file copy and command execution;
* external IS trust and notification-alias configuration;
* Solace JWKS alias configuration;
* an explicit coverage capability or an explicit unsupported result.

Shared infrastructure consumers currently casting blockApimContainer to DynamicApimContainer must use this contract or an adapter. Known consumers include LoggingSteps, RemoteLoggingSteps, SecondaryUserStoreProvisioner, framework probes, and BlockLifecycleListener.

## 5. Lifecycle and network

The first distributed implementation should remain per TestNG <test> block, matching DynamicApimContainer. A later suite-wide optimization is possible, but it changes data isolation and ownership and should not be mixed into the first checkpoint.

Startup order:

    private network
      -> MySQL container
      -> database creation and schema seed
      -> Control Plane
      -> Traffic Manager
      -> Universal Gateway
      -> dependency-aware readiness probes

Teardown order:

    Universal Gateway
      -> Traffic Manager
      -> Control Plane
      -> MySQL
      -> private network

Every child must be registered immediately after creation so a failure at any point can stop already-started resources. This is important when Ryuk is disabled during focused local runs.

Suggested network aliases are apim-cp, apim-tm, apim-gw, and mysql. Internal TOML URLs must use these aliases and canonical container ports. The existing wso2am external-IS reverse-channel alias must resolve to CP, because CP owns the management endpoint and notification receiver.

## 6. Public port and URL compatibility

The public meanings remain:

| Accessor | Distributed target |
|---|---|
| getServletHttpsUrl | CP management/portal HTTPS |
| getServletHttpUrl | CP HTTP |
| getGatewayHttpsUrl | Universal Gateway HTTPS |
| getGatewayHttpUrl | Universal Gateway HTTP |
| getGatewayWsUrl | Universal Gateway WebSocket |
| getGatewayWssUrl | Universal Gateway secure WebSocket |
| getWebSubEventReceiverUrl | Component owning the WebSub receiver; verify empirically |
| getGatewayClientIp | IP observed by the Universal Gateway |

The developer offsets must not be used internally. Each component should run with portOffset=0 in its own container and expose canonical ports through Docker mapping. The test JVM receives the mapped host URLs through the same TestContext keys as today.

The exact exposed port set must be derived from the built packs and existing v2 consumers. Do not expose every product port by default.

## 7. MySQL design

The distributed JVMs cannot share an embedded H2 database. The composite must own a MySQL container and use the product-compatible schemas for WSO2AM_DB and WSO2AM_SHARED_DB.

Responsibilities:

1. Start MySQL on the private network.
2. Wait for authenticated MySQL health.
3. Create databases and the test user.
4. Seed schemas exactly once.
5. Copy mysql-connector-j into CP, TM, and Gateway images.
6. Generate component URLs using mysql:3306, never a host-mapped port.
7. Stop MySQL and remove its volume during teardown.

The developer SQL files are a reference. The authoritative schema must be compared with the scripts shipped by the 4.7.0 component ZIPs. CP and TM need the APIM/shared schemas; Gateway needs the shared schema and any profile-specific runtime tables. Schema ownership and seed order must be proven from boot logs and focused probes.

The distributed database must be seeded before CP starts, because CP is the first component to access APIM metadata. TM and Gateway must start only after CP is ready.

## 8. Images and ZIPs

The product modules produce:

* wso2am-acp-version.zip;
* wso2am-tm-version.zip;
* wso2am-universal-gw-version.zip.

The initial implementation may build three images by hand from these ZIPs:

    distributed-apim-cp:version
    distributed-apim-tm:version
    distributed-apim-gw:version

Each image must contain one unpacked product ZIP, JDK 21, the component startup script, a predictable server home, the MySQL connector, writable logs, and canonical ports with offset zero.

The component server-home names and startup commands must be represented in a descriptor. They must not be assumed identical across the three distributions.

After the first checkpoint, image creation should be automated from Maven-generated ZIPs in the same way the current all-in-one image is built from the generated APIM ZIP.

## 9. Overlay model

The developer full TOMLs must not be copied verbatim. Each component should resolve:

    ZIP default deployment.toml
      + distributed component base overlay
      + optional component-specific block overlay
      + generated runtime values

The ZIP default is authoritative for keys not intentionally changed. Existing Utils TOML merge behavior should be reused.

The preferred TestNG parameter form is:

    <parameter name="tomlExtraOverlayPath.cp" value="..."/>
    <parameter name="tomlExtraOverlayPath.tm" value="..."/>
    <parameter name="tomlExtraOverlayPath.gateway" value="..."/>

The unqualified tomlExtraOverlayPath should remain an all-in-one compatibility parameter. In distributed mode it should either be rejected as ambiguous or be explicitly documented as a shared overlay valid for all components. It must not silently apply an APIM-only overlay to TM and Gateway.

Generated values must be applied after static overlays so stale localhost addresses, fixed host offsets, and developer ports cannot override the actual network.

The same component qualification is needed for serverFilesToCopy. A file intended for CP must not be copied to all three server homes.

The distributed base overlays must cover database settings, CP event publishing, TM event subscription, Gateway key manager and event hub settings, Gateway synchronization, throttling decision/auth endpoints, TLS/truststore, server roles, and external-IS notification routing.

## 10. Parameter compatibility

| Existing parameter | Distributed behavior |
|---|---|
| blockLabel | Same block label and scope |
| initTenantUsers | Provision through CP management URL |
| initSecondaryUserStore | Same behavior through ApimRuntime |
| initBackend | Attach or route the shared backend so Universal Gateway can reach it |
| bootExternalIdentityServer | Start external IS on the block network and configure CP trust |
| receiveExternalIsNotifications | Bind wso2am to CP and verify reverse delivery |
| emailUserMode | Same user provisioning behavior |
| tomlOverlayPath | Component-specific or explicitly rejected as unsafe |
| tomlExtraOverlayPath | Keep only as safe compatibility alias; prefer dotted parameters |
| serverFilesToCopy | Add component-qualified form |
| bootPlatformGateway | Define explicitly; it would add another gateway beside Universal Gateway |
| apim.coverage | Defer or collect all three JVMs; never report CP-only coverage as full APIM coverage |

## 11. Readiness and acceptance probes

Listening ports are insufficient. The distributed lifecycle must prove:

1. MySQL accepts authenticated connections.
2. CP management and tenant-management services are ready.
3. TM is reachable and subscribed to CP event traffic.
4. Universal Gateway is reachable.
5. Gateway can reach CP for artifact synchronization.
6. Gateway can reach TM decision and authenticated endpoints.
7. A minimal API can be deployed/synchronized when required by the checkpoint.
8. External IS trust and reverse notification routing work when enabled.
9. The Node backend route works when initBackend is enabled.

Every probe failure should identify the component, URL, response, and a short log tail.

## 12. Compatibility risks and decisions

### Concrete type casts

A composite object will fail current DynamicApimContainer instanceof checks. The common runtime contract or a deliberately maintained adapter is mandatory.

### Localhost and offsets

Host-process configuration cannot be used inside containers. Internal routes must be generated from aliases and canonical ports.

### Gateway URL semantics

Existing tests expect baseGatewayUrl to be the invocation endpoint, not necessarily the same process as baseUrl. The distributed implementation can preserve semantics while changing physical ownership.

### Backend routing

The separately containerized Gateway cannot automatically resolve a backend attached only to another network. initBackend must multi-home the shared backend or provide a deliberate host-routable route, followed by a focused invocation probe.

### External IS notifications

The wso2am alias must resolve to CP. This ownership must be encoded, not inferred from whichever component starts first.

### Coverage

JaCoCo currently models one APIM JVM per block. Distributed coverage requires three agent ports and a merge. It is out of scope for the first checkpoint and must be explicitly rejected or deferred.

## 13. Feasibility matrix

| Objective | Result | Condition |
|---|---|---|
| Listener-only topology switch | Feasible | New listener preserves block lifecycle contract |
| One model over CP/TM/Gateway/MySQL | Feasible | Composition plus ApimRuntime |
| Same URL/accessor contract | Feasible | CP owns baseUrl; Gateway owns gateway accessors |
| Existing tenant/user parameters | Feasible | Provision through CP and remove concrete casts |
| Existing external IS parameters | Feasible | CP owns trust and reverse channel |
| Existing email mode | Feasible | Independent of topology |
| Existing backend parameter | Feasible | Network/routing work required |
| Component-specific overlays | Feasible | Dotted parameters and generated-value precedence |
| Transparent MySQL ownership | Feasible | Composite owns connector, schemas, health, cleanup |
| Complete Publisher/Gateway checkpoint | Feasible | First focused distributed suite |
| Immediate distributed coverage | Not in first phase | Multi-JVM collector required |

## 14. First checkpoint

Create a focused distributed TestNG suite containing the complete current Publisher and Gateway blocks. Do not run the entire testng-v2.xml suite until this checkpoint passes.

The checkpoint must prove:

* CP, TM, Gateway, and MySQL boot;
* Publisher blocks use CP for management operations;
* Gateway blocks invoke through the separate Universal Gateway;
* API artifacts synchronize from CP to Gateway;
* throttling paths reach TM;
* tenant/user and backend initialization still work;
* teardown removes all component containers, the database, and the network;
* the all-in-one listener and focused suites remain unaffected.

Only then should Admin, DevPortal, SSO, and the remaining blocks be added.

## 15. Non-goals for phase one

* No full distributed testng-v2.xml run before the Publisher/Gateway checkpoint.
* No fixed developer host offsets inside containers.
* No full developer TOML copied as an immutable fixture.
* No suite-wide distributed optimization in the first implementation.
* No CP-only distributed coverage claim.
* No change to all-in-one listener behavior.

## 16. References

* Current lifecycle: all-in-one-apim/modules/integration-v2/tests-integration/cucumber-tests/src/test/java/org/wso2/am/integration/cucumbertests/utils/listeners/BlockLifecycleListener.java
* Current container: all-in-one-apim/modules/integration-v2/tests-common/testcontainers/src/main/java/org/wso2/am/testcontainers/DynamicApimContainer.java
* Composite gateway example: all-in-one-apim/modules/integration-v2/tests-common/testcontainers/src/main/java/org/wso2/am/testcontainers/DynamicPlatformGatewayContainer.java
* Original developer distributed setup: unavailable in this repository; its machine-local paths are intentionally not
  documented. The durable implementation references are the distributed Dockerfile, component overlays, startup
  script, and README under `all-in-one-apim/modules/integration-v2/tests-common/testcontainers/src/main/resources/distributed-apim/`.

## 17. Phase 0 evidence record

The Phase 0 audit was completed against the current sources. The important
contract facts are:

* `BlockLifecycleListener` is a per-TestNG-`<test>` lifecycle owner. It is
  opt-in through `blockLabel`, creates one private network and one
  `DynamicApimContainer`, publishes the shared-scope runtime values, performs
  optional backend/tenant/secondary-store/IS/Platform-Gateway setup, and tears
  down in the reverse ownership order.
* `baseUrl` is the management and portal URL. `baseGatewayUrl` and the WS/WSS
  accessors are data-plane URLs. The distributed implementation must preserve
  these meanings even though the physical owners become CP and Universal
  Gateway.
* The current product Publisher block uses `initTenantUsers=true` and
  `initSecondaryUserStore=true`; the current product Gateway block uses
  `initTenantUsers=true` and `initBackend=true`. This establishes the minimum
  first-checkpoint compatibility surface.
* Framework consumers currently depend on the concrete container type in
  logging, remote logging, secondary-user-store provisioning, lifecycle code,
  and verification probes. A composite cannot be substituted under the same
  TestContext key until those consumers use `ApimRuntime` or an equivalent
  adapter.
* The developer distributed setup starts only MySQL in Compose; CP, TM, and
  Universal Gateway are host processes started by `run.sh`. Its `localhost`
  routes, host port offsets, and full TOMLs therefore cannot be copied into
  the Testcontainers implementation.

Baseline command, run after setting the Docker/Testcontainers environment appropriate for the local host:

    cd all-in-one-apim/modules/integration-v2/tests-integration/cucumber-tests
    ./src/test/scripts/verification/verify-7.2.sh

Result: `VERIFY 7.2: PASS`. The focused suite verified tenant/actor setup,
Publisher API creation and publication, Gateway invocation through the Node
backend, runner setup-handoff, extra-overlay merging, and no leaked block
containers. The log also confirmed the expected current all-in-one H2 URLs.

This is a framework-contract baseline, not the complete `testng-v2.xml`
Publisher and Gateway product blocks. Those blocks remain intentionally
reserved for Phase 6, where they will be placed in a dedicated focused
distributed suite. No full suite was run.

## 18. Phase 1 implementation evidence

Phase 1 introduced `ApimRuntime` in the testcontainers module. It contains the
block-facing lifecycle, URL, file, execution, provisioning, logging, and
coverage operations required by the current v2 framework. `DynamicApimContainer`
implements it while retaining its existing `GenericContainer` behavior and
fluent concrete return types.

The following framework consumers now depend on `ApimRuntime` rather than
`DynamicApimContainer`: `LoggingSteps`, `RemoteLoggingSteps`,
`SecondaryUserStoreProvisioner`, block lifecycle teardown, and framework probe
steps. Direct container verification tests that construct
`DynamicApimContainer` themselves remain concrete because they test that class
directly.

Verification evidence:

* `mvn -q -pl tests-integration/cucumber-tests -am -DskipTests test-compile`
  passed.
* With the required Docker variables, `verify-7.2.sh` passed after the change.
  This exercised provisioning, Publisher operations, Gateway invocation,
  overlay handling, handoff, and migrated container file access.
* With the required Docker variables, `verify-4.10.sh` passed after the change.
  This exercised the migrated stop contract and double-stop cleanup behavior.

No full suite was run and no commit was created.

## 19. Phase 2 implementation evidence

The manual image recipes are under
`tests-common/testcontainers/src/main/resources/distributed-apim`:

* `Dockerfile` unpacks one supplied component ZIP, validates its component
  startup script, installs it under `/opt/wso2`, and adds the MySQL connector.
* `start-component.sh` provides one stable entrypoint while selecting the
  component startup script through an image environment variable.
* `build-images.sh` builds the three separately tagged images:
  `distributed-apim-cp`, `distributed-apim-tm`, and
  `distributed-apim-gateway`.

The recipes use JDK 21 and deliberately leave port offset and deployment
configuration to the future composite runtime. They do not copy the developer
setup TOMLs into the image.

The scripts pass `bash -n` validation and their missing-input path was tested:
the builder fails before invoking Docker when any component ZIP or connector
is absent. The real component ZIPs were subsequently found in the sibling
CP/Gateway/TM build outputs and all three images were built successfully:

* `distributed-apim-cp:<cp-version>-jdk21`
* `distributed-apim-tm:<tm-version>-jdk21`
* `distributed-apim-gateway:<gateway-version>-jdk21`

Image-level smoke checks passed for each image: JDK 21 is available, the
expected component startup script is executable, the server home is
`/opt/wso2`, and the MySQL connector is present. The images are arm64 Linux
images in the current Colima environment. Full APIM process startup is
deferred to the distributed MySQL/configuration checkpoint, where the
generated component TOMLs and network dependencies will be available.

Build evidence from the selected sibling artifacts:

* CP ZIP SHA-256: `7e4d13b1d8b95d07aa0e858c63b6dc617939700e790dbce9cce7bc18af349e7d`;
  image ID: `sha256:92ef36a1fd9509c2dfd448630b029e62abf88721fb0a3f9197d791c7b8ceab41`.
* TM ZIP SHA-256: `8d47fab9627999236d73158d06213ed5d4a0c2d83c66cff2ee1ac0e836653859`;
  image ID: `sha256:1808e905d6856f659aeafce73685d290bf8768f77dc254152290b57817f104ec`.
* Gateway ZIP SHA-256: `76a89165bc8afac8e2c6d1364abfb14954bcde526be7f1f1c45743384019a561`;
  image ID: `sha256:518e166b8e3b0883c34fc5304ca8d826da921ad9a2d7b273b7494ea8d0bc9c0b`.

## 20. Phase 3 implementation evidence

`DistributedMySqlContainer` now owns an isolated MySQL 8.4 resource on the
block network under the stable `mysql` alias. It creates both APIM databases,
the `wso2carbon` user and its local/remote grants, and exposes separate host
and in-network JDBC URL accessors. Product schema files are registered by the
future composite at runtime; the resource does not assume a developer-machine
path.

Readiness uses both the Testcontainers log strategy and an authenticated
`mysqladmin ping` retry loop. Schema seeding uses a per-database marker table,
so a failed seed can be retried while a successful seed is not replayed. The
database is block-scoped on tmpfs, and stopping the container removes its data.

The focused `DistributedMySqlContainerTest` passed with the required Colima
Docker variables. It verified database initialization, schema seeding, the
network JDBC alias, and fresh-block isolation. The CP-against-MySQL boot probe
also passed with the CP product ZIP: it applied generated MySQL deployment
configuration using the developer setup's XML-escaped `&amp;` URL convention,
allowed WSO2's configuration mapper to generate the JNDI datasource entries,
  started CP against the product APIM/shared schemas, observed `WSO2 Carbon
  started`, and verified that the authenticated test user could read APIM tables.
  No static `master-datasources.xml` resource is maintained. The probe
  intentionally covers CP only; TM/Gateway configuration is covered by the
  Phase 4 configuration layer and the composite boot path.

## 21. Phase 4 configuration-layer evidence

`DistributedApimTomlBuilder` implements the distributed configuration contract.
Its merge order is product ZIP defaults, component distributed base overlay,
component-specific extra overlay, and generated runtime values. The last layer
always wins, preventing a static overlay from redirecting a service back to a
developer host or overriding a block-specific endpoint.

The three small base overlays are stored under
`tests-common/testcontainers/src/main/resources/distributed-apim`:

* `cp-base-overlay.toml` configures CP's MySQL databases, event hub, and TM
  event publication endpoints.
* `tm-base-overlay.toml` configures TM's MySQL databases and CP event hub.
* `gateway-base-overlay.toml` configures Gateway's shared database, CP key
  manager/event hub, CP artifact synchronization, and TM throttling endpoints.

The overlays preserve ZIP-provided TLS and keystore defaults and contain only
the distributed topology delta identified by comparing the developer CP/TM/
Universal-Gateway TOMLs with their corresponding ZIP defaults. Database URLs
use the XML-escaped `&amp;` convention required by WSO2's configuration mapper.

Distributed extra overlays and boot files must be qualified with `.cp`, `.tm`,
or `.gateway`. The builder rejects legacy unqualified parameters instead of
silently applying one component's configuration to all three. The focused
`DistributedApimTomlBuilderTest` passed with the required Colima Docker
variables and covered precedence, alias-only endpoints, component isolation,
ambiguous-parameter rejection, and qualified server-file resolution. The layer
is connected to `DistributedDynamicApimContainer`, which builds and applies the
CP, TM, and Gateway TOMLs during component creation.

## 22. Phase 5 composite-runtime evidence

`DistributedDynamicApimContainer` now implements `ApimRuntime` as a composite
over one CP, one TM, one Universal Gateway, and one block-scoped MySQL
container. It starts and stops the children in dependency order, publishes the
existing management and gateway URL contract, and keeps component-specific
overlay and server-file resolution behind the same block-facing abstraction.

`DistributedLifecycleListener` selects this composite at the listener level.
Existing block parameters continue to enter through the lifecycle contract;
distributed overlays and boot files are resolved per component. The distributed
focused suite is `tests-integration/cucumber-tests/src/test/resources/testng-v2_distributed.xml`.
It currently contains the Publisher and Gateway blocks, with the two MCP
product-bug runners intentionally commented out and tracked separately.

The composite and configuration focused tests were run with the required
Colima/Testcontainers Docker variables. The distributed Gateway REST invocation
baseline recorded 63 passed and 5 failed scenarios. The broader Publisher/Gateway lane remains a
diagnostic checkpoint rather than a claim that every runner is green; known
product/topology-specific failures must be resolved or explicitly parked before
the lane is treated as complete.

No full suite was run.
