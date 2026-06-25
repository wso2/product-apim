# integration-v2 feature rewrite — task tracker

Rewrite the **22 product feature files** (authoritative inventory on `master-new-test-framework`) into our
own capability-decomposed shape (**21 target features + 3 `_setup_` features**), parallel-safe on the v2
shared-container lane. Reorganize by decomposition — do **not** copy `.feature` files verbatim.

See the source→target decomposition and rationale in the project memory / earlier analysis. This file is the
live checklist; tick boxes as work lands.

## Conventions

- **Run command (verify between every phase).** Build images do **not** change, so we only run the cucumber
  tests — no upstream/image rebuild. From `all-in-one-apim/modules/integration-v2`:
  ```
  mvn test -pl tests-integration/cucumber-tests -Dsurefire.suite.xml=testng-v2.xml
  ```
  (`test`, not `install`; **no `-am`** — that would re-fire the testcontainers image-build execs.
  `testcontainers.reuse.enable=true` is already set in the pom so containers/images are reused.)
- **Wire new tests into `testng-v2.xml`.** Every rewritten feature's runner is added as a `<test>` block in
  `src/test/resources/testng-v2.xml` (the v2 parallel-on-shared-container lane; `parallel="tests"`,
  `thread-count` = K concurrent blocks). Runners are built onto `BaseBlockRunner` and registered with the
  `BlockLifecycleListener` + `BlockScopeListener` already declared in that suite. Setup features go **first**
  in a runner's `features = {…}` array.
- **Between-phase gate.** After each phase: add that phase's runner(s) to `testng-v2.xml`, run the command
  above, confirm **green + zero residue + lint-green** (`python3 docs/devs/render_coverage_tree.py`) before
  starting the next phase.
- **Every feature** must: use unique-by-construction names (0a), clean up in `@After` (0b), use only ported
  glue with no fixed-waits (0c), be self-contained (own prereqs via `_setup_`), and carry exactly one
  `@cap` + one `@feat` (+ `@legacy`).

---

## Phase 0 — Foundations (block everything)

> **Reordered after the glue audit.** `master-public` (this branch) has the framework infra + the generic/auth
> `BaseSteps` + framework-verification features, but **no product step glue and no product features/runners** —
> all of that lives on `master-new-test-framework`. So the foundational move is **porting the product glue** in
> the new identity shape (0c), and that port is **validated by the pilot, not a standalone placeholder block**:
> the current verify gate (placeholder block + lint) never exercises product auth, so 0a/0b/0c/0d/0e/0f are
> compile-only until the pilot runs them. Phase 0 and the pilot therefore land together. Apply 0d/0e **during**
> the port; adapt 0a/0b/0f from existing `master-new-test-framework` code rather than building blind.

- [ ] **Lock open design decisions.** (a) `@feat` for `governance_policy` — **PARKED**: `governance_policy`
  stays out of `capability-map.yml` and its rewrite (task #62) is deferred until unparked. (b) keep/drop the
  unwired `invalid_token_invocation` & `subscription_blocking` — **DECIDED: keep** (rewritten into
  `gateway/security-enforcement` and `devportal/subscription-management`). (c) whether `api_versioning` keeps
  its gateway-invoke step or factors it out — **DECIDED: factor out.** `api_versioning` legacy mixes a
  publisher concern (create/manage version) with a gateway concern (invoke v2 → assert 200, see legacy lines
  34-37). `publisher/versioning` (`@cap:publisher`) asserts only publisher-plane outcomes (v2 created,
  published, listed, default-version flag); "the new version is invocable" becomes a `gateway/rest-invocation`
  scenario `@dep:publisher`. As the pilot, this sets the invocation-lives-in-gateway precedent. **(d) Identity model — DECIDED (see 0d below):** the
  mutable `currentTenant`/`currentuser` pointer is removed entirely; actors are resolved by role from the
  block's provisioned set; default actor = super-tenant admin; tenant/role variation is a `Scenario Outline`
  over named actors; the legacy 4-mode `@Factory` fan-out is retired. Record outcomes in memory.
- [x] **Update `capability-map.yml`** for the 21 targets. **DONE** — all 20 target `(cap,feat)` pairs already
  present (governance parked); renderer parses. No edits needed.
- [x] **Foundational glue port (0c) — the first real move.** **DONE (pilot tier).** Ported `BaseSteps`,
  `PublisherBaseSteps`, `ApplicationBaseSteps`, `Hooks` reshaped into the identity model; added supporting
  `Utils.extractAPIUUID`/`getRevokeEndpointURL` + `Constants.CREATED_API_IDS`/`CREATED_APPLICATION_IDS`/
  `DEFAULT_APIM_REVOKE_EP`; dropped the `I wait for {int} seconds` + `system indexing to stabilize` fixed-wait
  steps (no remaining refs). `AdminBaseSteps`/`APIInvocationSteps` deferred as planned. Build green
  (`mvn test-compile -pl tests-integration/cucumber-tests -am` → BUILD SUCCESS). Runtime validation pends the
  pilot. *Original scope below:* Port from `master-new-test-framework` only the
  steps the 21 features use, **reshaped into the new identity model** (apply 0d/0e during the port). Pilot tier
  (port now): `BaseSteps` (573 ln, auth + the identity refactor), `PublisherBaseSteps` (1551 ln, ~45 steps read
  `publisherAccessToken`), `ApplicationBaseSteps` (747 ln — subscribe arc needed; key-gen steps deferrable),
  `Hooks` (91 ln → becomes 0b), `TenantUserInitialisationSteps` (251 ln — or **supersede** with the existing
  `TenantUserProvisioner` util, which it duplicates). Defer tier: `AdminBaseSteps` (72 ln), `APIInvocationSteps`
  (279 ln — gateway invoke, not in the publisher/versioning pilot). Also: drop the `I wait for N seconds`
  fixed-wait (use poll-until), fix literal-vs-bracket placeholder bugs, reuse/extend never duplicate.
  *Note paths: cross-branch reads need the FULL repo-relative path, e.g.*
  `git show master-new-test-framework:all-in-one-apim/modules/integration-v2/.../stepdefinitions/PublisherBaseSteps.java`.
- [x] **Identity model — remove the current-pointer model (0d) — applied during the 0c port.** **DONE.** New
  `utils/Identity.java` resolves actors by reference from the block-provisioned set (default = super-tenant
  admin); `tenant`/`currentuser` fields + `currentTenant` slot removed from `BaseSteps`; `theSystemIsReady`
  is now a readiness no-op; `theResourceShouldReflectTheUpdatedAs` derives domain from the actor;
  `DynamicLifecycleVerificationRunner` publishes the super tenant under its domain key. Zero
  `currentTenant`/`CURRENT_TENANT`/`getContextUser` residue in `stepdefinitions/`. *Original scope below:* Delete the
  `tenant`/`currentuser` instance fields and the `currentTenant` context slot entirely
  (`BaseSteps.java:55-56,73-74`; the slot is read at `:73` and `:510`, keyed `"currentTenant"` — separate from
  the domain-keyed tenants). Verified: `currentuser` feeds **only auth** (DCR owner/clientName +
  publisher/devportal/admin token username+password — all 13 uses); the pointer's only non-user use is
  `currentTenant.getDomain()` at `:510`, derivable from the actor (`User.getUserDomain()`, since usernames carry
  `@domain`). Replacement: **resolve actors by role from the block's provisioned set** — `getTenantAdmin()`
  (super admin), `getTenantUser("publisher")`, `getTenantUser("subscriber")`. Auth steps become actor-explicit
  (e.g. `...token as "publisher"`) with a **default actor = super-tenant admin** so simple features stay terse.
  Refactor `theResourceShouldReflectTheUpdatedAs` (`:510`) to take the domain from the actor. Also removes the
  "The system is ready"-must-run-first ordering trap (becomes a readiness assert / no-op — readiness is the
  listener's job) and the v2 NPE risk (nothing set `currentTenant` in the block lane). No mutable state →
  access-control variation is a `Scenario Outline` over named actors, fully parallel-safe. The
  `setContextUser`/`CURRENT_TENANT` mechanism also lives in ~30 runner files — out of pilot scope, handled as
  each runner is rewritten. (Extra/non-default tenants & users come from 0f.)
- [x] **Identity-qualified cache keys (0e) — applied during the 0c port, lands with 0d.** **DONE.** All token
  reads route through `Identity.publisherToken()/devportalToken()/adminToken()`, which key the cache by full
  username (`publisherAccessToken::<userName>` etc.); `Hooks` re-pointed to the qualified keys; zero remaining
  global `"publisherAccessToken"`/`"devportalAccessToken"`/`"adminAccessToken"`/`"dcrCredentials"` string
  literals in `stepdefinitions/`. *Original scope below:* Per-identity/
  per-resource values in `BaseSteps` are cached under global keys today — `publisherAccessToken`
  (`BaseSteps.java:132`, read by ~45 PublisherBaseSteps steps), `devportalAccessToken` (~20 ApplicationBaseSteps
  steps + Hooks), `adminAccessToken` (AdminBaseSteps), `dcrCredentials` (3 BaseSteps mint steps) — ~65 reader
  sites total. With actor-explicit auth (0d) these would silently reuse the wrong identity's token (false pass).
  **Qualify by the full username** (`User.getUserName()`, which carries `@domain`) — NOT the userKey, since keys
  `admin`/`userKey1` repeat across tenants — e.g. `publisherAccessToken::<userName>`. Sweep all ~65 reader sites
  (mostly mechanical) and any other global key in `BaseSteps` that is really per-identity/per-resource.
- [x] **`@After` cleanup hooks (0b) — port/reshape `Hooks.java`.** **DONE.** `Hooks.java` ported; its two
  hardwired token keys re-pointed to the 0e-qualified keys via `Identity.defaultActor()`; deletes apps-then-APIs
  idempotently on `@After("@cleanup")`. *Original scope below:* `Hooks.java` (91 ln) already has
  `@After("@cleanup") cleanUpCreatedResources()` reading `getList(CREATED_APPLICATION_IDS)` &
  `CREATED_API_IDS` (populated via `addToList` in PublisherBaseSteps `:88,:1135` & ApplicationBaseSteps `:72`),
  deleting apps-then-APIs, idempotent best-effort, finally-removing the lists — **reusable as-is** except it
  hardwires the 2 global token keys, which must be **re-pointed to the 0e-qualified keys**. Replace all trailing
  inline "Delete…" scenarios with this hook.
- [ ] **Cleanup-registration audit follow-ups (0b cont.).** Audit of every create-type step in the ported glue
  (done 2026-06-29). `ResourceCleanup` now sweeps `CREATED_API_IDS` / `CREATED_APPLICATION_IDS` /
  `CREATED_SHARED_SCOPE_IDS`. Registered & safe: create-API (`PublisherBaseSteps:70`), new-version (`:623`),
  GraphQL-API (`:1109`), import-OAS (`:1479`), API-product (`:1176`, delegates to create-API), create-app
  (`ApplicationBaseSteps:56`), shared-scope (`:987`). **No leak (child/cascade):** documents (die with parent
  API), API-specific policy, subscriptions + app keys (cascade when the app is deleted). **Two REAL unregistered
  leaks — register when the owning feature is implemented, not speculatively:**
  (1) **common operation policy** (`PublisherBaseSteps:1264`) and (2) **global policy** (`:1401`) — both
  tenant-wide, survive API deletion. Owned by `governance_policy` (#62, PARKED) and possibly throttling
  features; when unparked, add `CREATED_*_POLICY_IDS` + a `ResourceCleanup` sweep step (delete after APIs).
- [x] **Unique-naming utility (0a) — greenfield.** **DONE.** New `utils/Names.java` generates per-runner unique
  names (cached UUID suffix + monotonic counter in local scope); `Utils.resolvePayloadPlaceholders` substitutes
  `${UNIQUE:<base>}` tokens, wired into `BaseSteps.putJsonPayloadFromFile`/`putJsonPayloadInContext` so names
  are injected at payload-resolution time with no step-logic edits; `create_apim_test_api.json` +
  `create_apim_test_app*.json` updated to use the placeholders. *Original scope below:* No naming utility exists on either branch. Audit confirms
  hardcoded names (`APIMTest`, `apiTestContext/1.0.0`, …) appear **only in payload JSONs / `.feature` files, 0
  in step Java** — so uniqueness can be injected at **payload-resolution time** without editing step logic.
  Build the per-runner-instance unique-name helper and wire it at that resolution point; context isolation ≠
  container isolation.
- [ ] **Mid-test user/tenant provisioning steps (0f) — advanced/setup lane only.** Add Gherkin steps backed
  by the already-idempotent `TenantUserProvisioner.addUser/addTenant` (read `baseUrl` from context):
  `Given a user "<name>" with role "<role>" [in tenant "<dom>"]` and `Given a tenant "<dom>"`. **Used only by
  `_setup_*` features**, unique-named, `@After`-registered — never on the common path. Don't pre-seed a big
  per-role default user matrix in `provisionTenantUsers`; **extend the provisioned set on demand** as new tests
  need it. Note: default `testUser1` carries all three internal roles, so negative role tests ("a subscriber
  cannot publish") need a single-role user — add those incrementally via this step / the default set when a
  test actually requires one, not upfront.

## Phase 1 — Prereqs & pilot (validates Phase 0)

- [x] **Author the 3 `_setup_` features** — **DONE (authored; runtime-validated by the pilot).** All three in
  `features/common/`, each `@setup`, assert nothing, unique-named, `@After`-registered: `_setup_base_apis` now
  creates all **4 base APIs** (REST + SOAP via the standard create step; GraphQL via the multipart
  schema-upload step + `graphql_schema.graphql`; WebSocket via the standard step); `_setup_published_apis`
  (1 published REST API); `_setup_subscribed_app` (app + keys + sub). Payloads
  `create_apim_test_{soap,graphql,websocket}_api.json` + `graphql_schema.graphql` ported with `${UNIQUE:...}`
  name/context placeholders; all JSON validated; required glue (`Utils.getGraphQLSchema`,
  `SimpleHTTPClient.doPostMultipartWithFiles`) confirmed present; build green. Live execution still pends the
  pilot run.
- [x] **Pilot: `publisher/versioning`** end-to-end. **DONE & GREEN.** `features/publisher/versioning.feature`
  (`@cap:publisher @feat:versioning`, gateway-invoke + inline deletes factored out per 0c(c); teardown via
  `@cleanup` hook), `PublisherVersioningRunner` on `BaseBlockRunner`, wired as a `<test>` block in
  `testng-v2.xml`. Ran `mvn test -pl tests-integration/cucumber-tests -Dsurefire.suite.xml=testng-v2.xml`
  → **Tests run: 3, Failures: 0** (block guard + 2 scenario-outline rows). Run log confirms actor-explicit
  auth (`user admin` tokens), `${UNIQUE:...}` naming (`APIMTest_bf7d2870_*`), and `@After` cleanup (undeploy
  on teardown). NOTE: the shared `integration-test-utils` module must be `mvn install`-ed after edits or the
  bare `mvn test` compiles against the stale `.m2` jar.
- [x] **✅ Verify Phase 0 + Phase 1** — **DONE.** v2 suite green, zero residue (cleanup fired), renderer lint
  green (`placed: 1` under `publisher/versioning`, `invalid: 0`).

## Phase 2 — Publisher (7 features)

- [x] **`publisher/api-lifecycle`** — **DONE & GREEN.** `features/publisher/api_lifecycle.feature` merges the
  lifecycle arc of `create_an_api…` + `create_deploy_publish` into one scenario (create+deploy → retrieve →
  update desc/tiers → update-does-not-rename invariant → publish → list); subscribe/invoke factored out to
  devportal/gateway. `PublisherLifecycleRunner` + `<test>` block in `testng-v2.xml`. Added `update_apim_test_api.json`
  + `rename_apim_test_api.json` payloads (literal names — update-by-id ignores name/context). Ran v2 suite →
  **Tests run: 4, Failures: 0** (both publisher blocks parallel-green). Literal `APIMTest`/`apiTestContext`
  assertions dropped since names are now `${UNIQUE:}`-randomized; rename invariant still holds.
  **UPDATE (tenant/role model):** converted to a `Scenario Outline` running as the least-privilege
  `publisherUser` (creator+publisher, NOT admin) across **both tenants** (`publisherUser`,
  `publisherUser@tenant1.com`) — the representative ×2 core-feature. Background now uses the new
  `The system is ready and I have valid publisher access tokens as "<actor>"` composite (publisher+devportal,
  no admin token). Proves a non-admin publisher can drive the full lifecycle in each tenant. Re-ran → green.
  **NEGATIVE (role enforcement):** added a `@type:negative` outline — `subscriberUser` / `subscriberUser@tenant1.com`
  attempt to create an API and are **rejected with 401** (a token lacking the publisher scope is treated as
  *Unauthenticated request* for the resource — NOT 403; verified against the live server). New non-asserting
  step `I attempt to create an "<type>" resource with payload "<key>"` (the normal create asserts 201, so a
  separate attempt step is needed for negatives; registers no id). Full suite → **Tests run: 23, Failures: 0**.
- [x] **`publisher/api-config`** — **DONE & GREEN.** `features/publisher/api_config.feature` merges the config
  parts of `api_runtime_configurations` + `api_other_common_configurations`: a Scenario Outline over
  responseCachingEnabled / cacheTimeout / enableSchemaValidation / transport / corsConfiguration /
  additionalProperties / policies, plus an add-resource-operation scenario. Each PATCHes one field and
  re-fetches to assert persistence. Shared-scope arc deferred to `publisher/scopes` (#51). Established the
  **`_setup_*` fixture pattern**: `_setup_config_api.feature` (untagged, listed first in the runner's
  `features={}` array) creates the base API into the runner's local scope (`configApiId` + `configApiPayload`);
  `PublisherConfigRunner` added as a 3rd class in the shared `IntegrationV2-Publisher` block. **Cleanup-gap
  fix (reusable):** extracted `utils/ResourceCleanup`; `Hooks @After("@cleanup")` delegates to it (per-scenario
  path), and `BaseBlockRunner` gained an `@AfterClass` runner-scoped sweep so setup fixtures are torn down once
  after all the runner's scenarios (a per-scenario hook would delete them mid-run). Ran v2 suite →
  **Tests run: 12, Failures: 0**; log confirms the base API survived all config scenarios then was deleted at
  AfterClass. GOTCHA: a feature description line starting with `@` parses as a Gherkin tag ("A tag may not
  contain whitespace") — keep `@` out of description prose.
- [x] **`publisher/scopes`** — **DONE & GREEN.** `features/publisher/scopes.feature` splits the shared-scope
  arc out of `api_other_common_configurations`: scenario 1 creates + retrieves a shared scope; scenario 2
  creates a scope and assigns it to an API via the API-level `scopes` array (assigning only at the operation
  level does NOT persist — the API-level `scopes` array is the real assign). Both self-contained + `@cleanup`.
  `PublisherScopesRunner` added to the `IntegrationV2-Publisher` block. **Cleanup extension (reusable):** shared
  scopes are tenant-wide and were leaking (ResourceCleanup only swept APIs/apps) — added
  `Constants.CREATED_SHARED_SCOPE_IDS`, registered scopes in the create step, and `ResourceCleanup` now deletes
  scopes LAST (after the APIs that reference them). Required a `mvn install` of integration-test-utils. Ran v2
  suite → **Tests run: 14, Failures: 0**.
- [x] **`publisher/definitions`** — **DONE & GREEN.** `features/publisher/definitions.feature` rewrites
  `import_OAS_definition` as a Scenario Outline over OAS 2/3/3.1: import definition → create → revision →
  deploy → publish (publisher-plane only; invoke arc factored to gateway/rest-invocation). Brought 6 OAS
  payloads under `artifacts/payloads/OAS/` (literal names — distinct per version, collisions only matter
  within a runner's container). **Fixed cleanup gap:** the import-OAS step didn't register its created API
  (`addToList(CREATED_API_IDS)` added). `PublisherDefinitionsRunner` added to the `IntegrationV2-Publisher`
  block. Ran v2 suite → **Tests run: 17, Failures: 0**.
- [x] **`publisher/docs`** — **DONE & GREEN.** `features/publisher/docs.feature`: one self-contained
  `@cleanup` scenario doing the full doc CRUD arc (add → list → get → update → delete) against a REST API
  created in-scenario. Documents are API-children, so the `@cleanup` hook tearing down the API removes them
  (no separate registration — as the 0b audit predicted). Brought `add_new_document_api.json` (literal
  `<type>`/`<sourceType>`/`<inlineContent>` placeholders, replaced by the prepare step). Deviated from legacy:
  dropped the 4-API-type matrix (tests the doc API, not the API type — redundant) and used create-in-scenario
  instead of `_setup_base_apis` (docs CRUD wants its own API per scenario). `PublisherDocsRunner` added to the
  `IntegrationV2-Publisher` block. Ran v2 suite → **Tests run: 18, Failures: 0**.
- [x] **`publisher/graphql-design`** + **`publisher/streaming-design`** — **DONE & GREEN.** Two separate
  features (distinct `@feat`): `graphql_design.feature` (create-from-schema → revision → deploy → publish) and
  `streaming_design.feature` (WebSocket create+deploy → publish). Design/publish only; invocation factored to
  gateway. Both create steps already register `CREATED_API_IDS`, single `@cleanup` scenario each; payloads
  carry `${UNIQUE:}` from #47. `PublisherGraphQLDesignRunner` + `PublisherStreamingDesignRunner` added to the
  `IntegrationV2-Publisher` block. Inline deletes dropped in favor of `@cleanup`.
- [x] **`publisher/soap-design`** — **DONE & GREEN (gap-fill).** Surfaced by the api-lifecycle vs source
  comparison: the legacy `create_deploy_publish` published all 4 API types but our rewrite was REST-only, and
  GraphQL/WebSocket got dedicated design features while SOAP publish was covered nowhere. Added
  `publisher/soap_design.feature` (create+deploy → `Created` → publish → `Published`) + `@feat:soap-design` in
  `capability-map.yml` + `PublisherSoapDesignRunner` in the publisher block. Also closed the minor gap: added
  the post-deploy `Created` lifecycle assertion to `api_lifecycle` (the source asserted it; our composite only
  checked the deploy 201).
- [x] **✅ Verify Phase 2** — **DONE & GREEN.** 9 publisher runners share the `IntegrationV2-Publisher` block
  on one container. Full v2 suite → **Tests run: 24, Failures: 0**; renderer lint green (`placed: 12`,
  `invalid: 0`). Includes the tenant/role model (api-lifecycle ×2 + subscriber negative) and the SOAP gap-fill.

### Phase 2b — propagate tenant ×2 + per-feature negative to all publisher features (in progress)

Decision: every publisher feature runs its positive flow over **both tenants** (`publisherUser` super +
`publisherUser@tenant1.com`) as the least-privilege publisher, and each gets a **subscriber-rejected negative**
(both tenants). For fixture-based features the `_setup_*` fixture creates its base API **per tenant** (no
role needed — just the relevant tenant). Patterns: self-contained features convert `Background`→`Scenario
Outline` over an actor column; negatives use non-asserting `attempt` steps + `I act as "<publisher>"` to
restore the owning actor before `@cleanup`.

- [x] **`versioning` ×2 + negative — DONE & GREEN.** First conversion; surfaced & fixed **three latent
  shared-glue bugs** the super-admin-only suite hid: (1) `theLifecycleStatusShouldBe` did a single GET, no
  retry → flaky on async publish under load → now polls; (2) `waitForAPIDeployment` authed the gateway-artifact
  ADMIN endpoint with the acting actor → a non-admin publisher could never confirm deploy → now uses new
  `Identity.actingTenantAdmin()`; (3) `contain "true"` is a meaningless whole-body substring match (legacy
  parity was illusory) → dropped, and `isDefaultVersion` exposed undocumented product behavior so we don't
  over-assert. Added `I attempt to create a new version …` (non-asserting) and `I act as "<actor>"` steps.
  Suite → **Tests run: 26, Failures: 0**.
- [x] **`api-config` ×2 (fixture per tenant, full field matrix both tenants) + negative — DONE & GREEN.**
  `_setup_config_api` is now a `Scenario Outline` over tenants: creates the base API per tenant as that
  tenant's admin (`I have valid access tokens as "admin<suffix>"`), storing tenant-suffixed keys
  (`configApiId` / `configApiId@tenant1.com` + payloads). `api_config` scenarios `I act as "admin<suffix>"`
  then run all 7 fields ×2 tenants (14) + add-operation ×2 + subscriber-negative ×2. Suite → **Tests run: 37**.
  Gotcha fixed: the admin composite is `I have valid access tokens as "<actor>"` (no "The system is ready and"
  prefix) — pair with a separate `Given The system is ready`.
- [x] **`definitions` ×2 + negative — DONE.** Positive outline over [publisherUser, publisherUser@tenant1.com]
  × 3 OAS versions (6 rows); negative = subscriber can't create the API to import into.
- [x] **`docs` ×2 + negative — DONE.** Doc CRUD outline ×2; subscriber-create negative ×2.
- [x] **`scopes` ×2 + negative — DONE.** **Product finding:** shared-scope management needs admin
  (`apim:shared_scope_manage` not granted to creator+publisher), so positives run as **admin** ×2 tenant
  (not publisherUser); subscriber-rejected negative ×2. Added non-asserting `I attempt to create a shared
  scope as "<name>"` step.
- [x] **`graphql-design` ×2 + negative — DONE.**
- [x] **`streaming-design` ×2 + negative — DONE.**
- [x] **`soap-design` ×2 + negative — DONE.**
- [x] **✅ Re-verify Phase 2b — DONE & GREEN.** Full v2 suite → **Tests run: 56, Failures: 0**; lint green.
  Three latent shared-glue bugs fixed during versioning conversion (lifecycle-status poll, deploy-wait admin
  auth via `Identity.actingTenantAdmin()`, meaningless substring asserts) + the scopes admin-privilege finding.
  Negatives uniformly assert **401** (publisher-scope-less token = unauthenticated-for-resource). New steps:
  `I act as "<actor>"`, `I attempt to create a new version …`, `I attempt to create a shared scope as …`.

### Thread-count sweep — HOST-LOCAL, indicative only (not CI guidance)

Quick wall-clock-vs-`thread-count` sweep of the single `IntegrationV2-Publisher` block (9 runner classes,
`parallel="classes"`, 56 tests). **Measured on a developer host, NOT CI** — treat numbers as relative shape,
not absolutes. CI tuning is a separate task (different cores/memory/IO; re-measure there).

- **Host:** macOS, 8 logical CPUs. **Docker engine:** Colima VM = **6 CPU / ~12 GB**. One APIM container
  (`wso2am:4.7.0-SNAPSHOT-jdk21`) shared by all 9 runners; fresh boot (~35 s) per `mvn` invocation.
- **Command:** `mvn test -pl tests-integration/cucumber-tests -Dsurefire.suite.xml=testng-v2.xml` (maven Total time).

| thread-count | wall-clock | result |
|---|---|---|
| 1 | 6:05 | green (sequential baseline) |
| 2 | 4:10 | green (the committed setting) |
| 4 | 3:46 | green |
| 6 | 3:35 | green (knee ≈ VM core count) |
| 9 | 3:38 | **1 failure** — deploy-wait timed out under starvation |

**Durable, host-independent takeaways** (the reason this is recorded):
- The knee sits at ≈ the container VM's core count: one shared APIM container can't exploit more class-level
  parallelism than the VM has cores. Big gain 1→2 (−31%), then flattens hard (2→6 only another −14%).
- **Past the knee it gets slower AND flaky:** at `tc=9` the runners starve the single 6-CPU server, the async
  API deploy-wait (60 s window) times out → intermittent `expected [true] but found [false]`. More parallelism
  on one container is a reliability risk, not just diminishing returns.
- Implication for scaling later phases: prefer **splitting into more `<test>` blocks** over cranking one
  block's `thread-count` — BUT see the update below: more blocks means more concurrent containers, which is
  itself host-bounded.
- **UPDATE (Phase 3, 2-block reality):** once a 2nd block (DevPortal) was added, running both blocks in
  parallel (suite `parallel="tests" thread-count="2"` = **2 APIM containers at once**) overwhelmed the 6-CPU
  VM — ~11 scattered `900967 General Error` 500s on create/deploy across both blocks. Fix: **suite
  `thread-count=1`** (blocks run sequentially, one container at a time; each block still internally
  `parallel="classes" thread-count=2`). Net: this host reliably runs **one APIM container at a time**;
  K=2 *containers* is too much. Full suite then green at ~5 min. CI (more cores) may sustain K≥2 — re-measure
  there as its own task.
  - **Reproduced deliberately (Phase 3 close):** re-ran with suite `thread-count=2` (both blocks concurrent) —
    `900967 General Error` 500s resurfaced on `iCreateAnAPIWithPayloadAs` (API create), **1 failure / 3
    occurrences** this time vs ~11 before. The varying count confirms it's a **load-dependent race, not
    deterministic** — i.e. K=2 is intermittently flaky, never safely green (worse than a clean fail for CI).
    Suite stays at `thread-count=1`.

### DevPortal block thread-count sweep — HOST-LOCAL (publisher block commented out)

Same method, isolated to the 4-runner-class DevPortal block (12 tests):

| thread-count | wall-clock | result |
|---|---|---|
| 1 | 1:11 | green |
| 2 | 0:54 | green |
| 4 | 0:53 | green |

**Durable takeaway:** the knee is at **2 here, vs 6 for publisher** — because the useful thread-count is capped
by `min(independent-runner-count, VM-cores)`, NOT by cores alone. DevPortal has only 4 runner classes (and one,
`subscribe`, is a fixture-runner whose two features run sequentially), so past tc=2 there isn't enough
independent work to fill more threads → flat. Publisher had 9 independent runners, so it kept gaining to the
6-core ceiling. So per-block `thread-count` should be ≈ its runner count, capped at VM cores; the committed
DevPortal `thread-count=2` is already optimal. Also: the fixed ~35 s container boot is ~55-65% of a small
block's run, so over-splitting into many tiny blocks wastes wall-clock on boots.

## Phase 3 — DevPortal (4 features)

- [x] **`devportal/applications`** — **DONE & GREEN.** `features/devportal/applications.feature`: app
  create → retrieve → update → delete as a DevPortal consumer (`subscriberUser`) ×2 tenant. New
  `IntegrationV2-DevPortal` `<test>` block (separate container; validated the 2-block K=2 topology — publisher
  + devportal boot in parallel, both green). New composite `The system is ready and I have valid devportal
  access token as "<actor>"`. Key-gen factored out to key-manager/oauth-keys (#64). **Finding:** org/group
  sharing (`groups:["org1"]`) NOT asserted — needs `enable_cross_tenant_group_sharing` server config the
  default container lacks (server silently drops it); same class as the `isDefaultVersion` over-assert. Kept
  app-CRUD assertions; org-sharing deferred to a config-specific feature/overlay. Suite → **Tests run: 58**.
- [x] **`devportal/search`** — **DONE & GREEN.** `features/devportal/search.feature`: publish an API, then
  find it in the DevPortal store by name and by context, ×2 tenant. The search step already polls the async
  (Solr) index — no fixed-wait needed (Watch concern was a non-issue; the ported glue already retries).
  **Key addition for randomized names:** wired `{{contextKey}}` resolution into the search-query step AND
  the `The response should contain` step, so the feature captures the `${UNIQUE:}`-generated name/context
  (`name:{{createdApiName}}`) and asserts on it — reusable for any dynamic-value assertion. Created inline
  (self-contained `@cleanup`) rather than a `_setup_` fixture since one scenario owns the API.
  `DevPortalSearchRunner` added to the DevPortal block. Suite → **Tests run: 60**.
- [x] **`devportal/subscription-management`** — **DONE & GREEN.** Merges `subscription_blocking` +
  `subscription_throttling_policy` into `features/devportal/subscription_management.feature`: block/unblock
  (`@rule:blocking`) and throttling-plan update to Gold (`@rule:throttling-plan`), ×2 tenant. Runs as **admin**
  (the flow spans provider create/publish/block + consumer subscribe — admin holds both). Invoke-arc 401 check
  factored to gateway/security-enforcement. `DevPortalSubscriptionManagementRunner` in the DevPortal block.
  **Bug fixed:** the plan-update step needs the current subscription payload in context — re-added the legacy
  `get subscription + put response payload in context as "subscriptionPayload"` prep step I'd dropped.
  **SCALING FINDING:** with the suite running 2 blocks in parallel (suite `thread-count=2` = 2 containers),
  the larger suite threw ~11 scattered `900967 General Error` 500s across BOTH blocks (host exhaustion: 2 APIM
  servers + 4 concurrent ops on the 6-CPU VM). Dropped **suite `thread-count` to 1** (blocks sequential, each
  still internally `parallel=classes` tc=2) → fully green, 4:56. K=2 *containers* is too much for this host;
  this is the limit the thread-count note anticipated.
- [x] **`devportal/subscribe`** — **DONE & GREEN.** `features/devportal/subscribe.feature`: a subscriber
  consumer creates an app and subscribes it to a published API, then confirms the subscription — ×2 tenant.
  Uses the `_setup_published_apis` fixture, **rewritten per-tenant** (publishes an API per tenant as that
  tenant's admin, tenant-suffixed keys `publishedApiId` / `publishedApiId@tenant1.com`; untagged + runner
  `@AfterClass` sweep). The genuine role-distinct path: API published by **admin**, subscribe done as
  **subscriberUser**. `DevPortalSubscribeRunner` lists the fixture first. Suite → **Tests run: 68**.
- [x] **✅ Verify Phase 3 — DONE & GREEN.** DevPortal block (applications, search, subscription-management,
  subscribe) all green; full v2 suite → **Tests run: 68, Failures: 0** (~5 min, blocks sequential per the
  host K=1 limit); renderer lint green (`placed: 25`, `invalid: 0`).
- [x] **DevPortal access-control negatives (added later, ×2 tenant) — DONE & GREEN.** Added per-feature
  negatives where enforcement is real (skipped hollow ones on search): **applications** — a publisher-role
  user (no app-manage scope) cannot create an app; **subscribe** — a publisher-role user (no subscribe scope)
  cannot subscribe; **subscription-management** — a subscriber-role user (no block scope) cannot block a
  subscription. All assert **401** (publisher-negative contract: scope-less token = unauthenticated-for-
  resource, NOT 403). New non-asserting steps: `I attempt to create an application`, `I attempt to subscribe
  to API …`; `I act as` used to restore the owning actor before `@cleanup`. Full suite → **Tests run: 86,
  Failures: 0** (7:37). NOTE: an intervening full run hit a **transient** `APIM block 'devportal' did not
  become ready within 300s` (4 boot-guard failures, 18 skips, 2:49h of timeout stacking) under host pressure;
  a clean re-run was fully green — environmental, not a regression.

## Phase 4 — Gateway (4 features)

- [x] **`gateway/{rest,soap,graphql}-invocation`** — **DONE & GREEN (all ×2 tenant).**
  `features/gateway/rest_invocation.feature`: publish → subscribe+token → invoke through gateway → 200, ×2
  tenant as admin. New `IntegrationV2-Gateway` block. **Ported `APIInvocationSteps`** (deferred defer-tier
  glue) with the identity reshape — tenant domain from the acting actor, not `currentTenant`; dropped the
  migration-only token-file step + userinfo step. **Three findings fixed:** (1) gateway invocation needs the
  `nodebackend` upstream — there's a `NodeAppServer` singleton the parallel lane never started; added opt-in
  `initBackend` block param, `BlockLifecycleListener` starts it before APIM (fixed super-tenant
  `303001 SUSPENDED`); (2) tenant API `context` from the Publisher API ALREADY carries `/t/<tenant>`, and the
  URL builder re-added it → `/t/tenant1.com/t/tenant1.com/...` 404; added `I invoke the API at gateway context`
  step that uses the full context verbatim (no re-prefix); (3) path `{{}}` placeholder resolution added to
  invoke steps. **SOAP + GraphQL now DONE too:** GraphQL invokes the in-network `am-graphQL-sample:3003`
  backend; SOAP required a backend — **added a `soap-stub` node app** (`nodeapps/soap-stub`, port 3019,
  returns a fixed SOAP envelope) wired into the Dockerfile/ecosystem/NodeAppServer ports + rebuilt the
  `node-app-server` image, and repointed `create_apim_test_soap_api.json` from external `ws.cdyne.com` to
  `nodebackend:3019` — so SOAP invocation is in-network and deterministic. `gateway/{soap,graphql}_invocation`
  features + runners added to the gateway block. Full suite (3 blocks) → **Tests run: 72, Failures: 0** (6:05).
  **UPDATE: SOAP + GraphQL extended to ×2 tenant** (super + tenant1.com). Needed a SOAP-by-gateway-context
  invoke step (`I invoke the SOAP API at gateway context …`) so tenant SOAP isn't double-`/t/`-prefixed like
  the REST fix; GraphQL already used the gateway-context invoke. All three invocation features now ×2 tenant.
- [x] **`gateway/security-enforcement`** — **DONE & GREEN.** `features/gateway/security_enforcement.feature`:
  a published API invoked with a garbage bearer token is rejected with **401** + "correct security
  credentials" message (super tenant; the valid-token path is gateway/rest-invocation). Uses the ported
  `APIInvocationSteps` + the `invoke at gateway context … until status 401` retry (naturally waits for the
  route to come up, then asserts enforcement). GOTCHA: `I put value "<literal>"` resolves the literal as a
  context key (throws if absent) — staged the invalid token via the verbatim `I put the following JSON payload
  in context as` doc-string step instead. `GatewaySecurityEnforcementRunner` in the gateway block. Full suite
  → **Tests run: 73, Failures: 0** (6:09). **UPDATE: extended to ×2 tenant + a 2nd negative.** Now two
  `@rule`-tagged scenario outlines: `@rule:invalid-token` (garbage token → 401) and `@rule:no-subscription`
  (valid token from an app NOT subscribed to the API → **403**). The 403-vs-401 distinction confirms the
  gateway separates "unauthenticated" from "authenticated-but-not-subscribed" — a distinct enforcement path.
- [ ] **`gateway/custom-auth-header` — PARKED (2026-06-29).** `custom_authorization_header`; **special**: own
  custom-header TOML system-init block (cannot share default container). ⚠ **Watch:** FIRST feature needing a
  **non-default container config** — gets its OWN `<test>` block in `testng-v2.xml` with a `tomlOverlayPath`
  parameter (see `BlockLifecycleListener` PARAM_TOML_OVERLAY); do NOT add it as a class in the shared
  publisher/gateway block. Uses the ported `APIInvocationSteps`.
- [ ] **`governance_policy`** — operation-policy CRUD; `@cap/@feat` per Phase-0 decision. **When unparked,
  close the cleanup leak flagged in the 0b audit:** the common-operation-policy (`PublisherBaseSteps:1264`)
  and global-policy (`:1401`) create steps don't register for teardown and are tenant-wide — add
  `CREATED_*_POLICY_IDS` + a `ResourceCleanup` sweep (delete after APIs).
- [ ] **✅ Verify Phase 4** — add gateway runners (+ custom-header system-init block); run; all green.

## Phase 5 — Key-manager (4 features)

- [x] **`key-manager/token-issuance`** — **DONE & GREEN.** `features/keymanager/token_issuance.feature` merges
  all 4 variants as separate scenarios (`@rule:jwt-format|openid|refresh|sandbox`): JWT-format production token,
  OpenID-scoped token + userinfo, refresh-token re-issue + gateway invoke, sandbox token + gateway invoke. Runs
  as admin. **Re-added** the OpenID userinfo step (`APIInvocationSteps.invokeUserInfoEndpoint`) + `Utils.getUserInfoEndpointURL`
  + `Constants.DEFAULT_APIM_USERINFO_EP` (dropped during the Phase-4 port). New `IntegrationV2-KeyManager` block
  (initBackend for the refresh/sandbox invocations).
- [x] **`key-manager/token-revocation`** — **DONE & GREEN.** `features/keymanager/token_revocation.feature`:
  issue token → invoke 200 → revoke → invoke 401 (gateway-context retry), as admin.
- [x] **`key-manager/api-key`** — **DONE & GREEN.** `features/keymanager/api_key.feature`: enable api_key
  security scheme → deploy/publish → subscribe → generate API key → invoke with the key (api-key `at path`
  variant; super-tenant context, no `/t/` prefix).
- [x] **`key-manager/oauth-keys`** — **DONE & GREEN.** `features/keymanager/oauth_keys.feature`: generate
  production consumer credentials (consumerKey/Secret) → exchange for an application access token. Key-gen arc
  factored out of devportal/applications.
- [x] **✅ Verify Phase 5 — DONE & GREEN.** All 4 key-manager features in the `IntegrationV2-KeyManager` block
  (7 scenarios). Full v2 suite (4 blocks) → **Tests run: 80, Failures: 0** (7:26); renderer lint green.
- [x] **Key-manager ×2 tenant + negative (added later) — DONE & GREEN.** Extended all 4 features to run ×2
  (super + tenant1.com) as admin — token-issuance now 8 outline rows (4 variants ×2), revocation/api-key/
  oauth-keys ×2 each. Added a **`@type:negative` invalid-API-key** scenario to api-key (garbage key → 401).
  Needed api-key + SOAP **by-gateway-context** invoke steps (`I invoke the API at gateway context … using api
  key …`, `I invoke the SOAP API at gateway context …`) so tenant rows aren't double-`/t/`-prefixed. KeyManager
  block in isolation → **Tests run: 16, Failures: 0** (2:38).

## Phase 6 — Wire & final validation

- [ ] **Runners/blocks finalized in `testng-v2.xml`** — PublisherConfigRunner, PublisherLifecycleRunner,
  DevPortalRunner, GatewayInvocationRunner, KeyManagerTokenRunner, CustomAuthHeaderRunner,
  GovernancePolicyRunner; setup-first arrays; split for parallelism within host capacity (~6 CPU/12 GB, K=2).
- [ ] **Full-suite run** — `mvn test -pl tests-integration/cucumber-tests -Dsurefire.suite.xml=testng-v2.xml`;
  parallel-safe, zero residue.
- [ ] **Lint + coverage tree** — `render_coverage_tree.py` green; refresh `coverage-tree.md`.
- [ ] **Docs** — point `CLAUDE.md` §1 at `coverage-tree.md` (not the stale public map); add a
  superseded/staleness banner to `v2-public-feature-coverage-map.md`; record the 21-feature parity (~38%).

---

### Source → target quick reference (22 → 21 + 3 setup)

| Target | Source(s) |
|---|---|
| publisher/api-lifecycle | create_an_api… + create_deploy_publish (lifecycle) |
| publisher/api-config | api_runtime_configurations + api_other_common_configurations (config) |
| publisher/scopes | api_other_common_configurations (scopes) |
| publisher/versioning | api_versioning |
| publisher/definitions | import_OAS_definition |
| publisher/docs | api_documents |
| publisher/graphql-design | graphql_api_baseline |
| publisher/streaming-design | websocket_api_baseline |
| devportal/applications | create_new_application (app CRUD/share) |
| devportal/search | devportal_search_visibility |
| devportal/subscription-management | subscription_blocking + subscription_throttling_policy |
| devportal/subscribe | factored-out subscribe arcs |
| gateway/rest-invocation | invoke arcs (create_deploy_publish/create_an_api/import_OAS) |
| gateway/soap-invocation | create_deploy_publish (SOAP) |
| gateway/graphql-invocation | create_deploy_publish (GraphQL) |
| gateway/security-enforcement | invalid_token_invocation |
| gateway/custom-auth-header | custom_authorization_header |
| governance_policy | governance_policy_baseline |
| key-manager/token-issuance | jwt_token_format + openid_token + refresh_token + sandbox_token |
| key-manager/token-revocation | revoke_token |
| key-manager/api-key | api_key_invocation |
| key-manager/oauth-keys | create_new_application (key-gen) |
| _setup_base_apis / _setup_published_apis / _setup_subscribed_app | (new prereq features) |
