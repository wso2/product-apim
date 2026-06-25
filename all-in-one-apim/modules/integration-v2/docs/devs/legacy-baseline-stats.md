# Legacy Backend Integration Test Stats

> Collected 2026-06-17 from `modules/integration/tests-integration` (legacy TestNG suite),
> via filesystem counts (`find`/`grep`/`wc`). Intended as the baseline that drives grouping and
> scope decisions for the integration-v2 rewrite.

## Module-level counts

| Module | Java files | `*TestCase` classes | `@Test` methods | `@DataProvider` | `@Factory` | LOC |
| --- | --- | --- | --- | --- | --- | --- |
| tests-backend | 331 | 259 | 1,174 | 220 | 212 | 87,111 |
| tests-restart | 20 | 13 | 54 | 1 | 1 | 4,151 |
| tests-benchmark | 5 | 1 | 14 | 2 | 2 | 1,160 |
| tests-config | 1 | 1 | 2 | 2 | 0 | 367 |
| **Total** | **357** | **274** | **1,244** | **225** | **215** | **92,789** |

LOC = total lines in the Java source files (`wc -l`), including blanks, comments, imports, and
license headers. It is a relative size/effort proxy only, not a precise estimate.

## Multipliers (raw `@Test` count understates real coverage)

- **User-mode factory: 212 of 259 backend classes** use `@Factory(dataProvider="userModeDataProvider")`,
  instantiating the whole class once per `TestUserMode`. Mode usage across backend:

  | TestUserMode | References |
  | --- | --- |
  | SUPER_TENANT_ADMIN | 325 |
  | TENANT_ADMIN | 178 |
  | SUPER_TENANT_EMAIL_USER | 27 |
  | TENANT_EMAIL_USER | 26 |
  | SUPER_TENANT_USER_STORE_USER | 26 |
  | SUPER_TENANT_USER | 20 |
  | TENANT_USER | 9 |

  Most classes run 1–2 modes; a subset (email/userstore JWT tests) run 5–6. Effective executed cases are
  roughly **1.5–2× the raw `@Test` count** (~1,800–2,300 backend runs).
- **Data providers: 220** — many tests are further row-parametrized on top of the mode factory.

## Server restart footprint

- **65 backend classes use `ServerConfigurationManager`** (config overlay + apply, usually requiring a
  restart) — ~25% of backend classes, the expensive long-tail.
- `restartGracefully` called 10× in backend, 7× in the dedicated `tests-restart` module
  (13 classes / 54 tests).

## Suite wiring (what is actually run)

| Suite XML | `<class>` entries | `<test>` groups |
| --- | --- | --- |
| tests-backend `testng.xml` | 282 | 44 |
| tests-backend `testng-server-mgt.xml` | 6 | — |
| tests-restart `testng-server-restart.xml` | 12 | — |

(282 class entries vs 259 `*TestCase` files: some classes appear in multiple groups; some are non-TestCase.)

## Legacy domain grouping

### By package (top leaf packages, backend)

| Package | Test classes |
| --- | --- |
| tests/other | 59 |
| tests/api/lifecycle | 40 |
| tests/publisher | 22 |
| tests/restapi/testcases | 14 |
| tests/header | 12 |
| tests/application | 9 |
| tests/version | 8 |
| tests/token | 7 |
| tests/throttling | 7 |
| tests/restapi/admin/throttlingpolicy | 7 |
| tests/streamingapis/websub | 6 |
| tests/websocket | 5 |
| tests/restapi | 5 |
| tests/jwt | 5 |
| tests/apimGovernance | 4 |

The 59-class `other` bucket is a catch-all worth re-categorizing in the new structure.

### By suite `<test>` name (44 functional groups)

api-common, api-change-endpoint, api-product, api-governance, ui-tests, api-lifecycle (+lifecycle-2),
email-secondary-userstore, CORS, samples, shared-scope-with-restart, publisher, store,
grant-type-token, graphql, admin-rest-api, rest-api, websocket, streaming-api, async-api-import,
without-restarts, application-sharing, JWT, urlsafe-JWT, endpoint-security, external-idp,
emailusername-login, workflow, logging, cross-tenant, solace-broker, revoke-one-time-token-flow,
schema-validation, b2b, tenant-validation, apim-is-tenant-sync, application, websocket-invocation,
mandatory-properties-with-restart, unlimited-tier-disabled, token-persistence (+url-safe-jwt,
external-idp-jwt, jwt variants).

This list is a ready-made candidate taxonomy for the new grouping.

## Per-class `@Test` spread

Heaviest classes: KeyManagers 38, APISecurity 35, OperationPolicy 30, APIRevision 28,
CrossTenantSubscription 27, APIImportExport 22, WebSocketAPI 20, AIAPI 19. Six classes have 0 `@Test`
(base/abstract helpers).

## External dependencies / sidecars

What the tests actually depend on:

| Dependency | Footprint | Role |
| --- | --- | --- |
| JAX-RS sample war (`jaxrs_basic` @ `/services/`) | 116 refs | The primary API backend for invocation tests |
| WireMock (via `MockServerUtils`) | 10 classes | Programmable mock backend (mTLS, retries, endpoint behaviors) |
| `log.war` | 30 refs | Logging sample app for log/analytics tests |
| WebSocket backend | 5 classes | WS invocation tests |
| External IDP (WSO2 IS) | 6 classes | external-idp / federated-JWT |
| qpid (AMQP) | 1 class | async/JMS |
| Solace broker | 1 class | streaming long-tail |

**Recommendation:** treat sidecars as a tiered catalog — Tier 1 (JAX-RS sample + WireMock + WebSocket)
blocks ~90% of invocation work; Tier 2 (WSO2 IS) is scheduled as a later wave (heavy: a second full
product container); Tier 3 (Solace/qpid) is decided keep-or-drop before any porting. v2 already ships
`NodeAppServer` — first verify its route surface covers what `jaxrs_basic` exposes. Adopt the
Testcontainers WireMock module for the programmable-stub cases rather than extending `NodeAppServer`.

## Config-overlay inventory

Mechanism breakdown across the 65 `ServerConfigurationManager` classes:

| Pattern | Calls | Meaning in v2 terms |
| --- | --- | --- |
| `applyConfiguration(...)` (restart) | 30 | boot a fresh container with overlay X |
| `applyConfigurationWithoutRestart` | 11 | hot-apply (harder — needs exec into running container) |
| `restoreToLastConfiguration` | 46 | **free in v2** — just discard the container |
| `restartGracefully` | 10 | container restart-in-place |

Artifacts: 35 `.toml` files in the backend module — **all are `deployment.toml`** (there are no other
`.toml` configs in backend). Of these, ~3–4 are baseline/restore copies rather than feature overlays
(`configFiles/originalFile`, `configFiles/corsACACTest/original`, `configFiles/common`,
`toml_config/case1`), so the genuine feature-overlay count is ~31. v2 won't need the restore copies —
discarding the container replaces "restore to original."

Plus non-toml configs the v2 framework does **not** yet support copying: `tiers.xml` (10),
`tenant-conf.json` (7), `workflow-extensions.xml` (7), `additionalProperties.json` (8),
`log4j2.properties` (3), `api-manager.xml` (3), WS/WebSub logger XMLs.

### Restart is not always a deployment.toml change

Restart in the backend falls into three distinct patterns; only the first is "boot fresh container
with a toml overlay":

| Pattern | What it does | v2 mapping |
| --- | --- | --- |
| **A. Restart + deployment.toml overlay** | swap `deployment.toml`, restart | boot a fresh container with the overlay (easy) |
| **B. Restart + non-toml config swap** (~20 classes) | swap a different config file, restart | needs non-toml file overlay support + restart — **current framework gap** |
| **C. Restart-in-place to verify persistence** (token-persistence suites) | apply config, generate token, restart **same** server, assert state survived | needs a state-preserving restart, **opposite of the throwaway-container model** |

Pattern B config files and example classes:

| Config file swapped | Classes (examples) |
| --- | --- |
| `WS_*_Logger.xml` / `WebSub_*_Logger.xml` / `SSE_*_Logger.xml` | WebSocketAPI, WebSocketAPIScope, GraphqlSubscription, all WebSub/SSE cases (~10) |
| `tiers.xml` / `app-tiers.xml` / `res-tiers.xml` | EditTiersXMLAndVerifyInPublisher |
| `workflow-extensions.xml` | SubscriptionWFHTTPRedirect, APIStateChangeWorkflow, OnHoldSubscriptionWorkflowId |
| `tenant-conf.json` / `api-manager.xml` | APIMANAGER5417Prototyped, APIStateChangeWorkflow |
| `master-datasources.xml` | APIMANAGER5327KeyGenerationWithPGSQL |
| `secondary.xml` (userstore) | SecondaryUserStoreCaseInsensitive |
| `axis2.xml`, `json_to_xml.xml`, `dummy_api_*.xml` | ErrorMessageType, ESBJAVA3380, APIM4312NPE |
| `log4j2.properties` (likely) | CorrelationLoggingTest |

For Pattern C, verify the existing v2 `restart` feature actually restarts the same container (preserving
state) rather than recreating it — otherwise the persistence assertions are meaningless.

**Recommendations:**

- Build a **base-toml + named-patch-fragment merge primitive** so each overlay is a small diff, not a
  full hand-maintained copy (35 full tomls will drift). Add non-toml file overlay support
  (`tiers.xml`, `tenant-conf.json`, `workflow-extensions.xml`, `log4j2.properties`) — required before
  the throttling/workflow/logging domains can port.
- Map `applyConfiguration + restart` to "boot container with overlay"; `restoreToLastConfiguration`
  (46 calls of teardown pain) disappears because the container is discarded.
- Carve the **11 `applyConfigurationWithoutRestart`** cases out as a special-case backlog — they don't
  map to the boot-time model and need a hot-apply path or per-test redesign.
- **Cost control:** 35 overlays × ~30s cold boot is expensive if every test boots its own. Bucket the
  65 classes by overlay so tests sharing an overlay reuse one container boot. The overlay directory
  names are a ready-made grouping key (e.g. `tokenTest/persistence/*` → one persistence-config lane).

### Overlay deviation analysis (the real delta is tiny)

The overlays were added incrementally over time; each author copied a full `deployment.toml` from an
earlier test and tweaked a few lines. As a result every overlay carries the whole file (~350 lines) but
the change the test actually needs is small. A line-frequency diff across the 31 feature overlays
(base = lines present in ≥90% of overlays) bands the content as:

- **base boilerplate** (≥90% of overlays): 123 lines
- **inconsistently-copied drift** (25–90%): 31 lines — copied unevenly, mostly not test-relevant
- **rare** (≤25%): 165 lines — the candidate per-test delta

Even the "rare" band over-counts, because two things inflate it: (a) a recurring copied **CORS /
`[service_provider]` / `sp_name_regex` / `node_ip` / `wss_endpoint` block** that drifted into unrelated
overlays, and (b) the **synapse artifact `skip_list`** (lists of `*.xml` API files) — file-based
deployment fixtures that v2 does not use at all (v2 deploys APIs via REST). Stripping both, the genuine
config delta per overlay is typically **1–4 keys**:

| Overlay | Genuine config delta (after removing cruft + artifact lists) |
| --- | --- |
| cross-tenant | `enable_cross_tenant_subscriptions = true` |
| tokenTest/apiInvokeCombinationsTest | `type = "sandbox"` |
| customHeaderTest | `[apim.oauth_config] auth_header = "Test-Custom-Header"` |
| ElkAnalytics | `[apim.analytics] type = "elk"` |
| logAnalyticsEnabled | `[apim.analytics] type = "elk"` (**identical to ElkAnalytics**) |
| endpointCertificate | `[transport.passthru_https.sender.ssl_profile] interval = 30000` |
| webSocketTest | backend ws endpoint URLs (test fixture, not really an APIM knob) |
| streamingAPIs/async | `[apim.publisher] use_legacy_async_parser = false` |
| streamingAPIs/legacyAsync | `[apim.publisher] use_legacy_async_parser = true` |
| scopes | `[apim.http_client] max_total/default_max_per_route` |
| emailusernametest | `[tenant_mgt] disable_email_domain_validation`, `enable_email_domain`, `IsEmailUserName` |
| fileBaseAPIS | `synapse...file.storage.enabled=true`, `enable_apikey_subscription_validation=true` |
| allowedScopes | `[oauth] allowed_scopes = [...]` |
| allowedScopesWithCorsDisabled | `[oauth] allowed_scopes` + CORS `enable = false` |
| applicationConsentPage | `show_display_name_in_consent_page = true` |
| applicationSharing | `enable_application_sharing = true`, `application_sharing_type = "default"` |
| applicationAttributes | one `[[apim.devportal.application_attributes]]` block |
| idpjwt | `[apim.jwt_authenitcation]` block (claim_dialect, excluded_claims, km validation) |
| webSocketWithTracing | `[apim.open_telemetry]` jaeger tracer block |
| unlimitedTier | `[apim.throttling] enable_unlimited_tier = false` (rest is copied CORS cruft) |
| corsACACTest | CORS `allow_credentials/allow_origins/allow_methods` + sdk/jwt extras |
| solace | devportal JWT attribute block (rest is copied cruft) |

Genuinely large, real-subsystem overlays (not reducible to a few keys): `tokenTest/persistence/*`
family (token persistence + carbon DB url + app attributes), `tenantsync` (tenant-sharing + WSO2-IS-7
key manager), `mandatory-properties` (custom publisher properties + datasources), `approveWorkflow`
(workflow service URLs + notification + hybrid gateway env).

**Consolidation / elimination candidates (verify against test code before acting):**

- `ElkAnalytics` and `logAnalyticsEnabled` have an **identical** real delta → one is redundant.
- `streamingAPIs/async` vs `legacyAsync` differ by a single boolean → one Scenario Outline parameter.
- `allowedScopes` vs `allowedScopesWithCorsDisabled` differ only by the CORS `enable` flag.
- The `*`-cruft block (CORS/`service_provider`/`sp_name_regex`/`wss_endpoint`) in `unlimitedTier`,
  `solace`, `applicationAttributes`, `tenantsync`, `mandatory-properties` is almost certainly not
  required by those tests — copied, not authored.

**Implication for v2:** future test writers should supply only the **patch fragment** (the 1–4 keys
above), merged onto a single maintained base toml — never a full file. This both prevents the drift
seen here and lets us drop several overlays/restarts entirely once each delta is confirmed against the
owning test. **Caveat:** "rare line present" proves deviation, not necessity — confirming a key is
actually *required* (vs copied) needs reading the owning test; the table above is the candidate set for
that verification.

## REST API surface

Top call-sites by persona (grep-based call-site counts; relative magnitudes are the signal):

### Publisher (22 classes)

| Method | Calls |
| --- | --- |
| deleteAPI | 239 |
| getAPI | 130 |
| addAPI | 130 |
| updateAPI | 117 |
| changeAPILifeCycleStatus | 105 |
| getAPIByID | 43 |
| changeAPILifeCycleStatusToPublish | 28 |
| getAllAPIs | 27 |
| importOASDefinition | 20 |
| getSwaggerByID | 19 |
| copyAPI | 15 |
| updateSwagger | 14 |
| getAPIRevisions | 14 |
| addDocument | 14 |
| getLifecycleStatus | 12 |

### Store / DevPortal (38 classes)

| Method | Calls |
| --- | --- |
| deleteApplication | 131 |
| generateKeys | 119 |
| createApplication | 107 |
| subscribeToAPI | 81 |
| generateUserAccessKey | 41 |
| getApplicationById | 39 |
| getAllAPIs | 31 |
| getAPI | 28 |
| addApplication | 25 |
| createSubscription | 22 |
| searchPaginatedAPIs | 19 |
| generateAPIKeys | 19 |
| getAPIs | 16 |
| removeSubscription | 15 |
| getAllSubscriptionsOfApplication | 13 |

### Admin (12 classes + 11 `AdminApiTestHelper`)

| Method | Calls |
| --- | --- |
| addKeyManager | 26 |
| getWorkflows | 25 |
| deleteKeyManager | 25 |
| getWorkflowByExternalWorkflowReference | 24 |
| updateWorkflowStatus | 21 |
| addAdvancedThrottlingPolicy | 17 |
| addSubscriptionThrottlingPolicy | 16 |
| deleteAdvancedThrottlingPolicy | 13 |
| addDenyThrottlingPolicy | 13 |
| importThrottlePolicy | 12 |
| addEnvironment | 12 |
| deleteSubscriptionThrottlingPolicy | 11 |
| deleteApplicationThrottlingPolicy | 9 |
| addApplicationThrottlingPolicy | 9 |
| updateKeyManager | 8 |

**Analysis & implications:**

- The surface is **heavily Pareto-distributed**. The publisher CRUD+lifecycle quintet
  (add/get/update/delete/changeLifeCycle) is ~720 call-sites; the store quintet
  (createApplication/generateKeys/subscribeToAPI/deleteApplication/createSubscription) is ~500.
  **~15 publisher + ~15 store + ~15 admin reusable steps cover the vast majority of all legacy
  interactions** — strong evidence for a step-primitive-first build, not test-by-test porting.
- **Reuse the existing clients.** These call into `RestAPIPublisherImpl` / `RestAPIStoreImpl` /
  `RestAPIAdminImpl` in `test-utils`; the new Cucumber steps should wrap those, not reimplement HTTP.
- **Tests are inherently cross-persona journeys.** The store client appears inside 22 "publisher"
  classes → the typical scenario is *create in publisher → subscribe in store → invoke gateway*. Maps
  naturally to Cucumber `Background` + multi-persona step libraries; design step namespaces around
  personas but expect scenarios to span them.
- **Admin = three coherent sub-domains:** KeyManager, Workflow, Throttling-policy — clean grouping
  boundaries.

**Discrepancy to investigate:** the parity tracker marks **Workflows as `No`/planned**, yet legacy
exercises the workflow API heavily (`getWorkflows` 25, `updateWorkflowStatus` 21,
`getWorkflowByExternalWorkflowReference` 24). Either the tracker classification is stale or those calls
are incidental setup in other domains — re-check before scoping workflows out.

## Combinable test classes (shared container by config delta)

Derived from the overlay deviation table above: classes can share one container boot when their genuine
config deltas are *additive* (don't change baseline behaviour). Three tiers. Caveat: Tier-2 unions must
be confirmed against the owning test before merging — a "deviation" is not proof the test doesn't also
depend on a default the union changes.

### Tier 1 — Identical / same-overlay deltas (directly shareable)

| Shared config | Classes (one container serves all) |
| --- | --- |
| `[apim.analytics] type="elk"` (ElkAnalytics ≡ logAnalyticsEnabled) | DisableSecurityAndTryOutRESTResourceWithElkAnalytics, ELKAnalyticsWithRespondMediator, APIMAnalyticsTest (3 — currently split across 2 identical overlays) |
| WebSocket backend endpoints (+ additive tracing) | WebSocketAPITestCase, WebSocketAPIScope, WebSocketAPICorsValidation, APIMANAGER5869WSGatewayURL, GraphqlSubscription, WebSocketAPIInvocationWithTracing (6–7) |
| external-IDP JWT (`idpjwt`) | FederatedUserJWT, ExternalIDPJWTTestCase, ExternalIDPJWTTestSuite, TokenPersistenceExternalIDPJWTTestSuite (4, already shared) |
| `endpointCertificate` ssl_profile interval | APIEndpointCertificateTestCase, APIEndpointCertificateUsageTestCase (2) |
| cross-tenant flag | CrossTenantSubscriptionUpdateTestCase, CrossTenantSubscriptionTestSuite (2) |

### Tier 2 — Combinable via one small union overlay (additive, non-conflicting)

Different config sections, none changes baseline behaviour → a single "additive-flags" container can
host all:

- `cross-tenant` → `enable_cross_tenant_subscriptions=true`
- `applicationSharing` → `enable_application_sharing=true` + `application_sharing_type`
- `applicationConsentPage` → `show_display_name_in_consent_page=true`
- `endpointCertificate` → ssl_profile interval
- `scopes` → `[apim.http_client]` pool sizes
- `ElkAnalytics`/`logAnalyticsEnabled` → analytics type (no-op without an ELK server)

→ ~8–10 classes collapse onto one container instead of ~6 boots.

### Tier 3 — Cannot combine (delta changes global behaviour)

| Overlay / classes | Why isolated |
| --- | --- |
| `customHeaderTest` (CustomHeaderTestCase) | renames `auth_header` globally → breaks standard `Authorization` tests |
| `unlimitedTier` (UnlimitedTierDisabled, ConfigurableDefaultPolicy) | `enable_unlimited_tier=false` removes a tier other tests rely on |
| `corsACACTest`, `allowedScopesWithCorsDisabled` | change CORS behaviour → conflict with default-CORS tests and each other |
| `emailusernametest` | email-as-username changes the user model |
| `streamingAPIs/async` vs `legacyAsync` | same key, opposite boolean → mutually exclusive (async group of 3 shareable; legacyAsync separate) |
| `applicationAttributes`, `tokenTest*` | `required=true` devportal attributes force all app-creation to supply them; conflict with each other on attribute sets |
| `tenantsync`, token-persistence variants | swap key manager (WSO2-IS-7) / token persistence + encoding → conflict with default-KM and each other |
| `approveWorkflow`, `mandatory-properties`, `fileBaseAPIS`, `solace` | change workflow/property/deployment-mode/broker subsystems globally |

### Net effect

Tier 1 + Tier 2 fold ~15 classes' worth of restarts down to ~3 shared container configs (analytics,
websocket/graphql-stream, additive-flags union). The Tier-3 set (~10 overlays) genuinely needs its own
server and maps cleanly to the restart lane.

## Overlay reduction (before / after)

Backend module, `deployment.toml` overlay count as we collapse cruft and combinable configs:

| Stage | Count | Change |
| --- | --- | --- |
| Original `deployment.toml` files | 35 | — |
| Drop baseline/restore copies (free in v2) | 31 | −4 (`originalFile`, `corsACACTest/original`, `common`, `toml_config/case1`) |
| Consolidate combinable configs (Tier 1 + Tier 2) | 24 | −7 |

**−7 breakdown:** Tier-2 additive union 7→1 = −6 (absorbs cross-tenant + applicationSharing +
applicationConsentPage + endpointCertificate + scopes + ElkAnalytics + logAnalyticsEnabled);
WebSocket merge `webSocketTest` + `webSocketWithTracing` 2→1 = −1.

**Net effect:** 35 → 24 distinct container configs (~31% fewer); ~15 classes fold onto ~3 shared configs.

**Caveats:**
- Tier-3 overlays (~10) stay separate — each changes global behaviour and needs its own server.
- 24 is the *distinct-config* count. In v2 each config is expressed as a base + patch fragment, so the
  per-test authoring cost is just the delta, not a whole `deployment.toml`.

## Limitations

### Test classes cannot run in parallel against a shared APIM server

The current v2 wiring parallelises only at the `<test>`-block granularity, and each `<test>` block boots
its own container. Within a block, the `<class>` runners execute **sequentially on one thread**. So a
single APIM server's request concurrency is wasted — ~15 data-isolated classes run serially against it
instead of concurrently, and every isolated config pays a fresh ~20–30s container boot.

This is an implementation choice, not a fundamental constraint. What blocks parallel-classes-on-one-server:

| Blocker | Hard? | Notes |
| --- | --- | --- |
| Parallel mode is only ever `TESTS` (never `classes`/`methods`) | No | `ParallelToggleAlterSuiteListener` only emits `TESTS` or `NONE`; trivially changeable. |
| Lifecycle modeled as ordered sibling classes | **Yes** | `SystemInitializationRunner` / `SystemShutdown` are `<class>` entries relying on `preserve-order` + serial execution as an implicit barrier. With `parallel="classes"`, init runs concurrently with tests and shutdown can fire mid-test. This is the real structural blocker. |
| `TestContext` thread-safety | No | `ConcurrentHashMap`-backed; shared scope holds the container handle read-only after init; local scope already keyed per class-instance. Concurrent reads are safe. |

**Why the blunt default exists:** container-per-`<test>` buys isolation for free, and a large slice of the
suite needs it — the Tier-3 overlay/restart tests mutate server-global state (deployment.toml, custom auth
header, unlimited-tier removal, throttle policies) and tests with global assertions ("list all APIs",
lifecycle counts, throttle counters) assume they own the server. Those are unsafe to share concurrently
regardless of architecture.

**Better model (two-tier lane):**
- *Shared-server lane:* one long-lived default-config container; run data-isolated classes (Tier 1 + Tier 2)
  against it with `parallel="classes"` (or `methods` + `dataProviderThreadCount`). Requires unique resource
  naming per scenario and no global-state mutation.
- *Isolated lane:* Tier-3 overlay/restart classes each get their own container, parallelised at `<test>`
  granularity as today.
- *Move lifecycle out of sibling classes* into `@BeforeSuite`/`@AfterSuite` (or a ref-counted container
  singleton) so there is a real barrier and shutdown cannot race a running test.

The hard part is not TestNG config; it is the lifecycle barrier plus per-test data-isolation discipline —
which is exactly the Tier-1/2/3 grouping mapped above.

### Cross-cutting risks / prerequisites for parallel-on-shared

Status of each item needed before the two-tier lane is safe, with the suggested fix:

| Risk / prerequisite | Status now | Why | Suggested solution |
| --- | --- | --- | --- |
| Blocker B — independent thread budgets | **Resolved (verified)** | The DTD showed per-`<test>` `thread-count` overrides the suite value, so block-level + class-level budgets are expressible in one run. Smoke test on TestNG 7.4.0 confirmed: nested suite `parallel=tests` + per-`<test>` `parallel=classes` engages both pools (4-way concurrency, 4 per-block threads), and per-`<test>` `thread-count` is a real bound (cap=2 with 3 classes → max 2, 3rd queued). | Adopt `<suite parallel="tests" thread-count="K">` (max concurrent containers) + per-`<test> parallel="classes" thread-count="N"` (max concurrent classes on that block's container). Single surefire run; no split needed. |
| Blocker A — DB-name suffix no-op | **Non-issue for H2 (first cut) — remove dead code** | `APIMContainer.java:61-63` `.replace("WSO2AM_APIMGT_DB"/"WSO2AM_COMMON_DB", …)` never matches the pom URL tokens (`WSO2AM_DB`/`WSO2SHARED_DB`) → no-op. But H2 is embedded **file-based per container**, so each container is already isolated by its own filesystem — the suffixing is redundant dead code, not a correctness bug. Only matters if a shared external DB (MySQL/Postgres) is later adopted, where the suffix would be the only isolation. | First cut is **H2-only**, so **delete the DB-rename block** (`APIMContainer.java:61-63`) — it implies an isolation guarantee it doesn't provide. Leave a one-line comment: per-container H2 is filesystem-isolated; a shared external DB would need real per-container schema isolation. Defer shared-DB support (and the proper suffix/derivation fix + per-container distinct-URL assertion) to a separate effort. |
| Port robustness (monotonic offset + fixed host ports) | **Decided — dynamic host-port mapping** | Static `AtomicInteger` only increments and `addFixedExposedPort` binds exact host ports → drift + collision risk across many blocks (the `8743 already allocated` class), worsened by `testcontainers.reuse.enable` disabling Ryuk. | **Dynamic port assignment.** Run the server on canonical ports (`-DportOffset=0`, each container has its own network namespace) and `withExposedPorts(9443, 9763, 8243, 8280)`; after `start()`, resolve host ports via `getMappedPort(internalPort)`. Expose role-named accessors on `APIMContainer` — `getServletHttpsUrl` (9443: publisher/devportal/admin/token/DCR), `getServletHttpUrl` (9763), `getGatewayHttpsUrl` (8243), `getGatewayHttpUrl` (8280) — and store the resulting URLs in the test-block shared scope (`baseUrl`=servlet-https, `baseGatewayUrl`=gateway-https). Removes drift + cross-run collisions and drops the offset counter entirely. **Caveat (accepted for H2 first cut):** host port ≠ internal port, so APIM self-advertised absolute URLs (OAuth redirect, devportal try-it, discovery) won't be host-reachable; safe here because every test-side URL derives from `baseUrl`/`baseGatewayUrl` and the toml `${...port}` self-refs resolve on the container's own canonical ports. |
| Data-isolation discipline (parallel-classes lane) | **Mostly addressable — auto-prefix + scoped queries; only Tier-3 global-writers excluded** | Bundles four sub-risks: (1) name collisions, (2) global *read* assertions (`count all == N`, `list all`), (3) global-state *mutation* (throttle/tenant/KM/toml-level config), (4) runtime `setShared`. Naming and read-assertions are automatable; global *writes* are inherent to Tier-3. | **Auto-prefix every created resource** in the resource-creation steps (centralized, not per-author discipline) so it's a guarantee. Derive the prefix from the per-instance scope id we already compute — `testName::className#instanceHash` (TestNameMdcListener) — **plus a method/uuid discriminator**, because `@Factory` user-mode and data-provider parallelism run below the class level, so a class-only prefix still collides. Then: (2) replace global reads with **prefix-scoped queries** (`?query=name:<prefix>*`) so "count all" → "count mine"; (3) keep genuine global-*write* classes (Tier-3) out of the shared lane — a name prefix can't isolate server-wide state; (4) restrict writes to the per-instance local scope (no runtime `setShared`); bonus: **delete-by-prefix** for robust teardown. Net: prefix + scoped queries collapse risks 1–2, leaving only Tier-3 global-writers excluded. |
| `onStart` boot-failure guard | **Solvable — listener owns lifecycle; inherited config-method guard skips cleanly** | `ITestListener` (and `onStart`) cannot trigger TestNG SKIP — throwing there is swallowed and the block runs anyway. A `SkipException` thrown through a *Cucumber* `@Before` is wrapped by cucumber-testng (`CucumberTestNGException`) → surfaces as FAILURE, not SKIP. Only a **TestNG configuration method** (or the test method) can trigger a native skip-cascade. | **Own the container lifecycle in the `<test>`-scoped listener, not in any test class** (extend the existing `TestNameMdcListener` pattern; both hooks fire once per `<test>`): `onStart(ctx)` boots the **single** container for the block (config/overlay from a `<parameter>` on the `<test>`, so `SystemInitializationRunner` disappears) and, **on boot/readiness failure, stashes a `bootError` on the native `ITestContext` attribute bag — it does not throw**; `onFinish(ctx)` calls `container.stop()` once (Docker releases the dynamic host ports automatically — no pool to reclaim), null-guarded so it's a no-op if boot produced no container. Then **one inherited guard** on `BaseBlockRunner` converts the flag to a clean skip: `@BeforeClass(alwaysRun=true) void abort(ITestContext c){ Object e=c.getAttribute("bootError"); if(e!=null) throw new SkipException("APIM block boot failed",(Throwable)e); }` — `SkipException` from a config method **is** honored, so the whole block is SKIPPED with the boot error as cause (no NPE cascade). **Net added state: one `bootError` flag + one ~3-line inherited guard; test-class authors write zero lifecycle code** (a runner just `extends BaseBlockRunner`). Prefer the `ITestContext` attribute over the `TestContext` shared map — it's auto-injected into both the listener and the guard and already scoped per `<test>`. |
| Req 5 provisioning refactor | **Solvable — low-risk extraction (no real DI coupling)** | `TenantUserInitialisationSteps` is **not** actually tied to picocontainer DI — it has no injected fields; every step is stateless and works through *static* `TestContext` + `SimpleHTTPClient.getInstance()` + `Utils`. The only coupling is the Gherkin `@When` annotations + reading `baseUrl` and writing tenants to the shared scope keyed by domain. So `onStart` can drive the same logic once it's lifted out of the step layer. | 1) Extract a plain `TenantUserProvisioner` (`addSuperTenant()`, `addTenant(domain, admin, pass, …)`, `addUser(domain, key, user, pass, roles)`) by moving the SOAP-build + "skip if exists" bodies verbatim; it keeps writing to `TestContext` shared scope, so step-level readers are unaffected. 2) Cucumber steps become thin delegators (drop `TenantUserInitializationRunner` like `SystemInitializationRunner`, keeping the feature only where a lane still wants Gherkin-driven init). 3) `onStart` calls the provisioner directly after boot + `baseUrl` is set, gated on `<parameter name="initTenantUsers">` (a second param names the tenant set — `default` vs migration's `adpsample`). **Scope detail:** `onStart` must `TestContext.setScope(ctx.getName(), …)` *before* provisioning so the `setShared` writes land on the block's `<test name>` key that the test-method threads later read (cross-thread visibility is fine — the shared map is static; the scope id just selects the bucket). |
| `<test name>` uniqueness foot-gun | **Solved by design — namespaced scope key (`suiteName::testName`) + composite lint** | Today the shared-scope/container handle is keyed by bare `<test name>` (set in `TestNameMdcListener.beforeInvocation`), so two blocks with the same name silently merge state. Decision: **namespace the key** so collisions can't happen even if a name repeats across suites, backed by a fail-fast lint. | **Namespace the shared-scope key as `suiteName::testName`** — derive it from `ITestContext.getSuite().getName()` + `getName()`, available to both the invocation listener and the new `onStart`/`onFinish` lifecycle hook. Centralize the derivation in one helper (`TestContext.sharedScopeId(ITestContext)`) so the two setters can't drift; test classes are unaffected (they go through `setShared`/`get`). Pair with a suite-load lint in the existing `IAlterSuiteListener` (`ParallelToggleAlterSuiteListener.alter`) that fails fast on duplicate **composite** keys globally (across `getChildSuites()`) and on unnamed/default suite names (TestNG falls back to "Default Suite", which would defeat the namespacing). Optional: prefix `buildLocalScopeId` with the suite too for full consistency (low priority — it already carries `className#instanceHash`). |
| Boot-cost amplification | **Control confirmed (Blocker B); cost inherent — bounded, not removed** | The *control* is solved: suite `parallel=tests thread-count=K` caps concurrent **containers** and per-`<test> thread-count=N` caps concurrent **classes**, and Blocker B's smoke test proved the two-level budget works in one run — so the unbounded/runaway risk (host OOM, Docker exhaustion) is handled. The *cost* is not: each container is still a ~20–30s boot + a full JVM/RAM footprint, and `thread-count` only chooses **how many you pay for at once** (low K → fits host but longer wall-clock; high K → faster but higher peak pressure). You can't parallelize boot latency away — only pick the ceiling. | Size the K/N caps to what the Docker host can run (trade wall-clock vs resource ceiling deliberately). The only lever that reduces *boot count* (not just bounds it) is the **shared-lane** — more classes per container → fewer total boots. Use `testcontainers.reuse.enable` only for identical-config blocks. |
