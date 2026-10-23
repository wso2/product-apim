# WSO2 API Platform Gateway — integration-v2 Design & Coverage

Integration-v2 coverage for the **WSO2 API Platform Gateway** — a standalone gateway runtime (data plane)
run outside the APIM pack and driven by the APIM **control plane** (design / deploy / visibility). Before
this, the suite covered only the embedded/universal gateway; the platform-gateway registration API, the
control-plane↔gateway WebSocket, and REST API deploy/invoke/policy through a platform gateway were untested.

> Status: The harness and two-scenario feature are implemented. The original product investigation and smoke used
> product 4.7.0-SNAPSHOT / carbon-apimgt 9.33.162. The current reactor pins carbon-apimgt 9.34.8; this docs-only
> audit confirmed the current runner, feature, payloads, image properties, and container wiring, but did not rebuild
> APIM or revalidate the product behavior findings below against the 9.34.8 distribution. Treat version-specific
> findings as historical until that product-level check is repeated. The Phase-1 smoke registered a gateway
> and connected **gateway-1.2.0 ↔ APIM 9.33.162** (WS handshake → `isActive:true`), and
> `DynamicPlatformGatewayContainer` boots the two-service compose healthy through testcontainers. Scope is the
> Docker-testable core; VM / Kubernetes-Helm / cert-manager / Moesif analytics are out of scope.

## Architecture

### Infrastructure vs product behaviour
- **`bootPlatformGateway`** (block param, `BlockLifecycleListener`) provisions gateway *infrastructure*
  only: it spins up a `DynamicPlatformGatewayContainer` — a single-container ABSTRACTION over a trimmed
  testcontainers `ComposeContainer` running the two WSO2 gateway images (`gateway-controller` + `gateway-runtime`,
  observability stripped) — points it at this block's APIM control plane via `host.docker.internal:<mapped-9443>`
  (skip-verify), awaits the gateway health endpoint (`/_gateway-health/ready`, HTTPS 8443), and publishes the
  host-mapped data-plane URL to the block's shared scope. The listener boots it unconnected; the feature step's
  `connect(...)` follows the API-minted registration token. **The listener registers nothing** and
  performs no product operation.
- **Registering a platform gateway is admin product behaviour, done by the feature** — the single journey
  calls `POST /api/am/admin/v4/gateways` as an admin actor in `features/gateway/platform_gateway_lifecycle.feature`, captures the one-time `registrationToken`,
  and asserts the gateway reports `isActive == true`. Every subsequent operation (create/deploy an API,
  invoke through the gateway, attach policies) is likewise a cucumber step acting as an actor.
- **Registration token — runtime mint:** the listener starts the gateway without control-plane connection
  values. The feature registers it through `POST /gateways`, captures the one-time token, then calls
  `DynamicPlatformGatewayContainer.connect(...)` to write the control-plane host/token/name into the generated
  `api-platform.env` and restart the Compose runtime. API deploy and invocation follow that connection in the
  same journey. The harness does not configure `[[apim.platform_gateway.connect]]` or a fixed registration token.

### Concurrency model
- Each platform-gateway block runs its OWN `DynamicPlatformGatewayContainer`; testcontainers `ComposeContainer`
  gives every instance a **unique compose project identifier** that namespaces its containers, network and
  volumes, so concurrent blocks' gateways are fully isolated with no shared-network coordination (proven in the
  Phase-1 smoke — no manual `COMPOSE_PROJECT_NAME` needed).
- The gateway dials the control plane at `wss://<cp>:9443/internal/data/v1/ws/gateways/connect` (outbound) and
  APIM pushes artifacts back over that socket. The harness points the gateway at APIM's **host-mapped 9443** via
  `host.docker.internal` (the compose grants the controller `extra_hosts: host.docker.internal:host-gateway`);
  with `insecure_skip_verify=true` no cert exchange is needed. (Placing the compose on the block's own `Network`
  under the `wso2am` alias is an alternative that additionally enables backend-by-alias reachability for
  invocation — revisit if backend-routing scenarios need it.)
- Each parallel block owns a separately named gateway runtime and Compose project; the feature-minted token is
  used only to connect that block's runtime.

### Suite layout (`testng-v2.xml`)
- **`IntegrationV2-PlatformGateway`** — one `<test>` block, `bootPlatformGateway=true`, hosting a SINGLE
  feature journey (single runner). The journey walks: control-plane config + registration (A) → gateway
  bring-up/active (B) → REST API create+deploy to the platform gateway (C) → invoke + auth variants (D) →
  policies request/response (E). The current TestNG block uses `parallel="classes" thread-count="1"`.

### Tagging
Product tags `@cap:gateway @feat:platform-gateway` (defined in `capability-map.yml`), per-area
`@rule`s, `@type` per scenario; the feature lives in `features/gateway/`.

## Gateway distribution & container (harness configuration)
- The compose uses `ghcr.io/wso2/api-platform/gateway-controller` and
  `ghcr.io/wso2/api-platform/gateway-runtime`. The Maven property `pg.gateway.version` defaults to `1.2.0`,
  and the image names are configured by `pg.gateway.controller.image.name` and
  `pg.gateway.runtime.image.name`. The original product investigation described the `1.2.0` image as the
  `gateway/v1.2.0-rc` release; that release-label detail was not revalidated here.
- **Ports:** runtime data-plane **8443 (HTTPS ingress)**; health `/_gateway-health/healthy` and
  `/_gateway-health/ready` (HTTPS on 8443 — NOT a plain-HTTP `/health`, which is the legacy gateway);
  controller 9090 (REST) / 9092 (admin) / 18000 (xDS gRPC to Envoy).
- A trimmed compose (controller + runtime only) is baked as a test resource; the observability stack
  (jaeger/otel/opensearch/prometheus/grafana/fluent-bit) is removed.

## Registration & connection (historical investigation in the 9.33.162 admin WAR + internal.service jar)
- **Admin API** (`/api/am/admin/v4`, tag "Platform Gateways", scope `apim:admin`):
  `POST /gateways` (`CreatePlatformGatewayRequest`: `name ^[a-z0-9-]+$` 3–64, `displayName` 1–128, `vhost`
  uri; optional `description`, `properties`, `permissions{permissionType: PUBLIC|ALLOW|DENY, roles[]}`) →
  `GatewayResponseWithToken` (201) with **`registrationToken`** (returned ONCE, stored hashed).
  Also `GET /gateways`, `PUT/DELETE /gateways/{id}`, `POST /gateways/{id}/regenerate-token`.
- **`gatewayType = APIPlatform`** (`APIConstants.WSO2_API_PLATFORM_GATEWAY`); classic gateways are `Regular`.
  `Environment.status` = `Active`/`Inactive` (control-plane connection state) for `APIPlatform`.
- **deployment.toml:** `[apim.platform_gateway] versions = ["1.0.0"]` (default `default.json:298`) supplies
  selectable-version metadata; the investigation below found it is not a registration gate. Optional
  `[[apim.platform_gateway.connect]]`
  (`registration_token`, `name`, `display_name`, `url`, `organization`) can pre-authorize a fixed token before
  the DB row exists; the current harness does not use this optional pre-seed mechanism.
- **Connection:** the gateway opens a persistent WebSocket to
  `wss://<cp-host>:9443/internal/data/v1/ws/gateways/connect` (server class `GatewayConnectEndpoint`),
  sending `registrationToken` in the **`api-key`** header; an invalid token closes the socket with **4401**.
  API artifacts are PUSHED server→gateway over the same socket (~2s scheduler).

## TLS trust (simpler than IS7 — no cert exchange)
- The gateway (WSS client) connects with **`insecure_skip_verify=true`** (the config default, kept for tests),
  so it does NOT validate the control-plane cert — no `wso2am.p12` / augmented-truststore recipe is needed. The
  control plane authenticates the gateway by the `registrationToken` in the `api-key` header, not by a client
  cert (no mutual TLS observed). The gateway's own data-plane 8443 presents a self-signed listener cert
  (`platform-gateway/listener-certs`, CN=localhost); tests invoke it with TLS verification off.

## Coverage implemented — `features/gateway/platform_gateway_lifecycle.feature`
The feature has one registration/deploy/invoke journey scenario and one negative, and is listed in
`IntegrationV2-PlatformGateway` (`testng-v2.xml`, `thread-count=1`). The original Phase-1 run reported both green;
this docs-only audit did not rerun them:
- **register → one-time token → connect → `isActive:true`.**
- **create a REST API** (`gatewayType:APIPlatform`, `gatewayVendor:wso2`) → **deploy** to the auto-created env
  (`deploy-revision [{name:<gateway>, vhost:"localhost", displayOnDevportal:true}]`) → **publish**.
- **invoke through the gateway data plane (8443) with ENFORCED api-key auth** — the API attaches an `api-key-auth`
  policy, so: valid `ApiKey` → **200** (routed through the singleton NodeAppServer customer service), **no auth → 401**, **wrong key → 401** (a
  genuine authentication test with both negatives).
- **a `set-headers` policy applied at the gateway** — the API also attaches `set-headers` (via `apiHubPolicies`),
  injecting a request header the NodeAppServer `/reflect-headers` route reflects, asserted in the response body (E — policy attach + gateway
  enforcement proven for these policies; CORS / rate-limit / Basic are not covered here).
- **negative:** a registered-but-never-connected gateway stays **`isActive:false`**.

## Product findings & not-yet-covered (historical investigation against carbon-apimgt 9.33.162)
- **Open-by-default (important):** the platform gateway does NOT enforce OAuth2 for an API with NO attached auth
  policy — an unauthenticated invoke returns 200. Auth (`api_key`/`basic_auth`/`oauth2`) is *derived* from an
  attached Policy-Hub policy, unlike the classic gateway (secured by default). The journey therefore attaches
  `api-key-auth` for a genuine enforced-auth test (covered above).
- **Version-gating product gap:** `CreatePlatformGatewayRequest` has no version field;
  `[apim.platform_gateway] versions` is read-only UI metadata, never a registration gate.
- **Unsupported-type product gap:** the non-REST rejection is not implemented for `APIPlatform` (only an APK
  gate exists) — a WS/GraphQL API with `gatewayType:APIPlatform` returns 201. **Flag to the feature owner.**
- **Policy attach (two cases validated; broader policy coverage remains):** a platform-gateway hub policy attaches
  via the API's `apiHubPolicies`, each entry needing a non-blank **`policyId` in `name::version` form**
  (e.g. `api-key-auth::v1`, `set-headers::v1`). A by-name attach fails ("External policy identifier cannot be
  empty"); the operation-policy registry can't mint an id (its `supportedGateways` enum =
  `Synapse/ChoreoConnect/AWS/Azure/Kong`, no `APIPlatform`) — but a platform-gateway API takes the placeholder path
  (`ApiMgtDAO.createPlaceholderPolicyDataForExternalPolicy`), so a bare `name::version` id suffices with **no
  registration**. `api-key-auth` (enforced auth) and `set-headers` (header manipulation) are validated; whether the
  same mechanism works for `cors` / `token-based-ratelimit` / `basic-auth` remains to be verified. `jwt-auth` is NOT usable —
  no JWKS/key-manager is pushed to the gateway, so APIM Bearer tokens can't validate; hence `api-key-auth`.
- **Try-out — redundant:** no server-side try-out endpoint; the API Console is browser Swagger-UI hitting the
  gateway URL directly, which exercises the same authenticated invoke covered above.

## Open items / risks (resolved / remaining)
- ✅ Per-block isolation + reachability, ✅ version pairing 9.33.162 ⇄ gateway-1.2.0, ✅ create/deploy/invoke —
  all validated (see Coverage).
- **Stale client stub** — `all-in-one-apim/modules/integration/tests-common/clients/admin/src/main/resources/admin-api.yaml`
  lacks the Platform Gateways API; the steps hand-call `/gateways` via the raw `Requests` client (no regen needed).
- **First-GA APIM version** for platform gateway is unconfirmed (present in 9.33.162 here).

## References
- docs-apim platform-gateway: getting-started, setting-up, adding-and-managing-policies.
- WSO2 API Platform: `github.com/wso2/api-platform` (gateway compose, Helm chart, releases).
- Verified in-repo: admin WAR `admin-api.yaml` (Platform Gateways), internal.service `GatewayConnectEndpoint`
  (WS), `default.json:298` + `api-manager.xml.j2` (`[apim.platform_gateway]` config).
- Reuses the per-block network-isolation + external-system pattern from `is7-key-manager-integration-plan.md`.
