# WSO2 API Platform Gateway — integration-v2 Design & Coverage

Integration-v2 coverage for the **WSO2 API Platform Gateway** — a standalone gateway runtime (data plane)
run outside the APIM pack and driven by the APIM **control plane** (design / deploy / visibility). Before
this, the suite covered only the embedded/universal gateway; the platform-gateway registration API, the
control-plane↔gateway WebSocket, and REST API deploy/invoke/policy through a platform gateway were untested.

> Status: Phase-1 validated. Product facts below are verified against this repo's build (product
> 4.7.0-SNAPSHOT / carbon-apimgt 9.33.162), and the harness core is proven: a Phase-1 smoke registered a gateway
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
  host-mapped data-plane URL to the block's shared scope. Connection uses the hybrid token flow (pre-seed for the
  bulk; a feature step's `connect(...)` for the register→active coverage). **The listener registers nothing** and
  performs no product operation.
- **Registering a platform gateway is admin product behaviour, done by the feature** — the single journey
  calls `POST /api/am/admin/v4/gateways` as the admin actor (§14), captures the one-time `registrationToken`,
  and asserts the gateway reaches `status == Active`. Every subsequent operation (create/deploy an API,
  invoke through the gateway, attach policies) is likewise a cucumber step acting as an actor.
- **Registration token — hybrid (pre-seed + mint):** the listener pre-seeds a FIXED token via
  `[[apim.platform_gateway.connect]]` in APIM's toml and `GATEWAY_REGISTRATION_TOKEN` in the compose env, so
  the boot-time gateway is live for the deploy/invoke/policy scenarios; the journey ALSO does a real
  `POST /gateways` mint to cover the register→token→active flow as product behaviour.

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
  invocation — revisit if Group-D backend routing needs it.)
- The registration token is per-gateway, so parallel blocks registering distinct gateways do not contend.

### Suite layout (`testng-v2.xml`)
- **`IntegrationV2-PlatformGateway`** — one `<test>` block, `bootPlatformGateway=true`, hosting a SINGLE
  feature journey (single runner). The journey walks: control-plane config + registration (A) → gateway
  bring-up/active (B) → REST API create+deploy to the platform gateway (C) → invoke + auth variants (D) →
  policies request/response (E) → Dev Portal try-out (F). `thread-count` per the isolation proof.

### Tagging
Product tags `@cap:gateway @feat:platform-gateway` (new `feat` id to add to `capability-map.yml`), per-area
`@rule`s, `@type` per scenario; the feature lives in `features/gateway/`.

## Gateway distribution & container (verified)
- **Images (anonymously pullable from ghcr.io):** `gateway-controller:1.2.0` (gateway control plane) +
  `gateway-runtime:1.2.0` (Envoy data plane). The GA release is `gateway/v1.2.0-rc`, shipped as image tag
  `1.2.0`. Version property to add: `pg.gateway.version` / `pg.docker.image.*`.
- **Ports:** runtime data-plane **8443 (HTTPS ingress)**; health `/_gateway-health/healthy` and
  `/_gateway-health/ready` (HTTPS on 8443 — NOT a plain-HTTP `/health`, which is the legacy gateway);
  controller 9090 (REST) / 9092 (admin) / 18000 (xDS gRPC to Envoy).
- A trimmed compose (controller + runtime only) is baked as a test resource; the observability stack
  (jaeger/otel/opensearch/prometheus/grafana/fluent-bit) is removed.

## Registration & connection (verified in the 9.33.162 admin WAR + internal.service jar)
- **Admin API** (`/api/am/admin/v4`, tag "Platform Gateways", scope `apim:admin`):
  `POST /gateways` (`CreatePlatformGatewayRequest`: `name ^[a-z0-9-]+$` 3–64, `displayName` 1–128, `vhost`
  uri; optional `description`, `properties`, `permissions{permissionType: PUBLIC|ALLOW|DENY, roles[]}`) →
  `GatewayResponseWithToken` (201) with **`registrationToken`** (returned ONCE, stored hashed).
  Also `GET /gateways`, `PUT/DELETE /gateways/{id}`, `POST /gateways/{id}/regenerate-token`.
- **`gatewayType = APIPlatform`** (`APIConstants.WSO2_API_PLATFORM_GATEWAY`); classic gateways are `Regular`.
  `Environment.status` = `Active`/`Inactive` (control-plane connection state) for `APIPlatform`.
- **deployment.toml:** `[apim.platform_gateway] versions = ["1.0.0"]` (default `default.json:298`) gates the
  selectable versions at registration; optional `[[apim.platform_gateway.connect]]`
  (`registration_token`, `name`, `display_name`, `url`, `organization`) pre-authorizes a fixed token before
  the DB row exists — this is the pre-seed hook the listener uses.
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

## Coverage (validated) — `features/gateway/platform_gateway_lifecycle.feature`
A single journey scenario + one negative, run in `IntegrationV2-PlatformGateway`, all green:
- **register → one-time token → connect → `isActive:true`** (A2/A3/B1).
- **create a REST API** (`gatewayType:APIPlatform`, `gatewayVendor:wso2`) → **deploy** to the auto-created env
  (`deploy-revision [{name:<gateway>, vhost:"localhost", displayOnDevportal:true}]`) → **publish** (C1).
- **invoke through the gateway data plane (8443) with ENFORCED api-key auth** — the API attaches an `api-key-auth`
  policy, so: valid `ApiKey` → **200** (routed to the echo backend), **no auth → 401**, **wrong key → 401** (D — a
  genuine authentication test with both negatives).
- **a `set-headers` policy applied at the gateway** — the API also attaches `set-headers` (via `apiHubPolicies`),
  injecting a request header the echo backend reflects, asserted in the response body (E — policy attach + gateway
  enforcement proven; the same mechanism covers CORS / rate-limit / Basic).
- **negative:** a registered-but-never-connected gateway stays **`isActive:false`** (B3).

## Product findings & not-yet-covered (verified in carbon-apimgt 9.33.162)
- **Open-by-default (important):** the platform gateway does NOT enforce OAuth2 for an API with NO attached auth
  policy — an unauthenticated invoke returns 200. Auth (`api_key`/`basic_auth`/`oauth2`) is *derived* from an
  attached Policy-Hub policy, unlike the classic gateway (secured by default). The journey therefore attaches
  `api-key-auth` for a genuine enforced-auth test (covered above).
- **A4 version-gating — product gap / non-scenario:** `CreatePlatformGatewayRequest` has no version field;
  `[apim.platform_gateway] versions` is read-only UI metadata, never a registration gate.
- **C3 unsupported-type — product gap:** the non-REST rejection is not implemented for `APIPlatform` (only an APK
  gate exists) — a WS/GraphQL API with `gatewayType:APIPlatform` returns 201. **Flag to the feature owner.**
- **Policy attach (cracked) — D2 + E done; D4/CORS/rate-limit follow-up:** a platform-gateway hub policy attaches
  via the API's `apiHubPolicies`, each entry needing a non-blank **`policyId` in `name::version` form**
  (e.g. `api-key-auth::v1`, `set-headers::v1`). A by-name attach fails ("External policy identifier cannot be
  empty"); the operation-policy registry can't mint an id (its `supportedGateways` enum =
  `Synapse/ChoreoConnect/AWS/Azure/Kong`, no `APIPlatform`) — but a platform-gateway API takes the placeholder path
  (`ApiMgtDAO.createPlaceholderPolicyDataForExternalPolicy`), so a bare `name::version` id suffices with **no
  registration**. `api-key-auth` (enforced auth) and `set-headers` (header manipulation) are validated; the same
  mechanism covers `cors` / `token-based-ratelimit` / `basic-auth` (breadth follow-up). `jwt-auth` is NOT usable —
  no JWKS/key-manager is pushed to the gateway, so APIM Bearer tokens can't validate; hence `api-key-auth`.
- **F try-out — redundant:** no server-side try-out endpoint; the API Console is browser Swagger-UI hitting the
  gateway URL directly = the same authenticated invoke as D1.

## Open items / risks (resolved / remaining)
- ✅ Per-block isolation + reachability, ✅ version pairing 9.33.162 ⇄ gateway-1.2.0, ✅ create/deploy/invoke —
  all validated (see Coverage).
- **Stale client stub** — `modules/integration/tests-common/clients/admin/src/main/resources/admin-api.yaml`
  lacks the Platform Gateways API; the steps hand-call `/gateways` via the raw `Requests` client (no regen needed).
- **First-GA APIM version** for platform gateway is unconfirmed (present in 9.33.162 here).

## References
- docs-apim platform-gateway: getting-started, setting-up, adding-and-managing-policies.
- WSO2 API Platform: `github.com/wso2/api-platform` (gateway compose, Helm chart, releases).
- Verified in-repo: admin WAR `admin-api.yaml` (Platform Gateways), internal.service `GatewayConnectEndpoint`
  (WS), `default.json:298` + `api-manager.xml.j2` (`[apim.platform_gateway]` config).
- Reuses the per-block network-isolation + external-system pattern from `is7-key-manager-integration-plan.md`.
