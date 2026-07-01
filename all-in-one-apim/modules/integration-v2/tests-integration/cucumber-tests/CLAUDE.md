# Writing integration-v2 tests

Rules for authoring Cucumber product tests in this module. Read before writing any test.

## 1. Search before you write
Duplicate tests are the #1 problem here. First check the coverage tree
(`../../docs/devs/v2-public-feature-coverage-map.md`) and the existing `.feature` files for the
capability you're about to test. Extend an existing feature/scenario if one fits; only add a new
file when nothing does.

## 2. Where it goes
Folders are shallow and by capability: `features/{publisher,devportal,gateway,admin,key-manager,analytics}/`
plus non-product `common/`, `framework-verification/`, `migration/`. The folder is just the physical
home and shared-fixture context — **`@cap` is the source of truth**, so a file may live in `publisher/`
yet be `@cap:gateway`.

## 3. Tags
Every product scenario is tagged. Valid `@cap`/`@feat` values are the closed vocabulary defined in
`../../docs/devs/capability-map.yml` — a tag not in that file fails lint.

| Tag | Cardinality | Meaning |
|-----|-------------|---------|
| `@cap:<id>` | exactly one | the capability under test (the subject of the assertions) |
| `@feat:<id>` | exactly one | feature under that capability |
| `@rule:<slug>` | 0–1 | free-text sub-grouping within a feature |
| `@type:smoke\|negative\|regression` | 0+ | test nature (selection axis) |
| `@dep:<cap>` | 0+ | a cross-capability **prerequisite** (NOT coverage of it) |
| `@legacy:<Class>` | 0+ | the legacy class this replaces (parity tracking) |

- **One `@cap` = one thing under test.** If you can't pick a single `@cap`, the scenario is doing too
  much — split it.
- `@dep` is for non-obvious cross-capability needs (e.g. gateway throttling needs an admin policy →
  `@dep:admin`). Don't tag the universal baseline (everything needs an API + token).
- Non-product features use exactly one exclusion marker — `@infra`, `@framework`, `@migration`, or
  `@setup` (reusable prerequisite features, see §10) — and are skipped from the product tree.

## 4. Isolation (the core concurrency rule)
Tests run in parallel on shared containers. **Every test owns its resources and shares nothing
mutable.**
- **Never name a resource by hand.** Use the shared naming utilities (`utils/Utils`,
  `utils/TestContext`) so names are unique by construction. Hardcoded names = cross-test collisions.
- No reliance on global state or on artifacts created by another scenario/class.
- **Cross-tenancy:** if a test needs other tenants, create them within that test class (via
  `utils/TenantUserProvisioner`), isolated from other classes — never reuse another class's tenant.
- **Wait, never sleep.** Poll/await for readiness (`utils/ServerReadiness` and equivalents). No
  `Thread.sleep` — it is the main cause of flaky parallel tests.

## 5. Cleanup
- Every resource a test creates is removed.
- Cleanup is **idempotent** and **runs even on failure** — do it in hooks, not as inline teardown
  scenarios that get skipped when an earlier step fails. Register created ids via
  `ResourceCleanup.register(CREATED_API_IDS / CREATED_APPLICATION_IDS, …)` (NOT bare
  `TestContext.addToList` — `register` tags each id with the **actor that created it** so teardown can delete
  it as that principal); both teardown paths below sweep them through `utils/ResourceCleanup`.
- **Teardown deletes each resource as its creating actor.** `ResourceCleanup` resolves the owner recorded at
  registration and deletes with that actor's token (devportal token for applications, publisher token for
  APIs/policies/scopes) — so a resource created by, say, `admin@tenant1` is deleted as `admin@tenant1`, not
  as whoever happens to be acting when cleanup runs. You therefore do **not** need to switch actor before
  `@cleanup` for teardown's sake (see §12). A non-2xx/404 delete is logged as a WARN (a real leak signal);
  don't ignore those.
- **Two teardown granularities — pick by lifetime of the resource:**
  - **Per-scenario:** tag the feature `@cleanup`. The `@After("@cleanup")` hook deletes the scenario's
    resources right after it. Use for self-contained single-scenario features (e.g. a full lifecycle in one
    scenario).
  - **Per-runner:** for the `_setup_*` fixture pattern (§10), where a setup feature's resources must survive
    across the runner's later scenarios, **leave the feature untagged** and rely on `BaseBlockRunner`'s
    `@AfterClass` sweep, which deletes once after all the runner's scenarios. A per-scenario `@cleanup` here
    would delete the shared fixture out from under the scenarios that follow.
- Leave zero residue (APIs, apps, subscriptions, tenants, keys).

## 6. Gherkin style
- Unique, capability-named `Feature:` titles (no two files sharing a title).
- Shared setup goes in `Background`, not repeated per scenario.
- One behavior per scenario; descriptive scenario names.
- **Don't start a description/prose line with `@`** — Gherkin parses any line whose first non-whitespace
  char is `@` as a tag and fails with "A tag may not contain whitespace". Reword (e.g. "the AfterClass
  sweep", not "`@AfterClass` sweep") in `Feature:` descriptions.

## 7. Step definitions (glue)
- **Reuse** existing steps. If one doesn't quite fit, **extend it** to cover the new need — never add
  a near-duplicate step. Search the glue before writing.
- **The `httpResponse` contract.** A request-making step publishes its result to the shared context key
  `"httpResponse"`; the generic assertion steps (`Then The response status code should be N`,
  `The response should contain …`) read it back. This decoupling has a **stale-response trap**: if your
  step throws *after* a previous step already stored an `httpResponse`, the old response lingers and a
  following assertion can pass against the **wrong** call (a false pass). So a step that makes a request
  must **clear `httpResponse` before the call** (`TestContext.remove("httpResponse")`), so a throw leaves
  it *absent* rather than stale — then set it only on a real response.
- **Invocation steps funnel through `APIInvocationSteps.execute(...)`** — the single primitive that does
  exactly this (clear → call → set → **return** the response). Add new invocation variants by calling
  `execute`, not by hand-writing another `TestContext.set("httpResponse", client.doX(...))` (that
  re-introduces the stale-response trap and the ~15-way duplication it replaced).
- **Retry-until-status loops:** catch only `IOException` (transient gateway warm-up — retry), so a bad
  token/payload context key (`IllegalArgumentException` from `Utils.resolveFromContext`) fails **fast**
  instead of being masked as a timeout. Keep the last *returned* response in a **local** variable and
  **assert after the loop** (`assertNotNull` then `assertEquals` on the expected status) — the step must
  fail on its own, never rely on a following `Then` to notice a persistent failure. See
  `assertReachedExpectedStatus`.

## 8. Run & verify locally
Run the suite reusing prebuilt images (use `mvn test`, not `install`, so the testcontainers image-build
execs don't re-fire):
```
mvn test -pl tests-integration/cucumber-tests -am -Dsurefire.suite.xml=<suite>.xml
```
(from `all-in-one-apim/modules/integration-v2`). Confirm your new scenario passes before committing.

**After editing the shared `tests-common/integration-test-utils` module** (e.g. new `Constants`/`Utils`
symbols), re-install it first — the bare `mvn test` resolves that module from the local `.m2` jar, not from
source, so it compiles against the stale jar and fails with "cannot find symbol":
```
mvn install -pl tests-common/integration-test-utils -DskipTests
```
(A `test-compile … -am` masks this because `-am` rebuilds the upstream module in-reactor; the bare run does not.)

## 9. Copyright header
New `.java` files require the standard WSO2 license header. Use the **current year** — do not copy a
year from another file.

## 10. Prerequisites, setup features & runner grouping
The isolation/parallelism unit is the **runner** (one TestNG `<test>` block). Runners run **in
parallel**; scenarios **within** a runner run **sequentially**, in order. A runner may group several
`.feature` files via the `features = {…}` **array**. **Execution order is LEXICOGRAPHIC by feature
filename — NOT the array order** (cucumber-testng sorts; the array order is not honoured). This is why the
`_setup_*` prefix works: the leading underscore sorts before letters, so the fixture runs first. If you need a
specific order, control it via filenames (e.g. a `_setup_` prefix, or numeric `1_`/`2_` prefixes), not by
arranging the array — verified by Phase 7.4.
- **Prerequisites a feature needs but doesn't test go in a `_setup_*.feature`.** Example: an invocation
  test (`@cap:gateway`) needs a published API, but publishing isn't its subject — so a
  `_setup_published_apis.feature` (`@setup`) is listed **first** in the runner and builds it.
- A setup feature **asserts nothing** and creates **uniquely-named** resources (see §4) into the runner's
  **local** `TestContext` scope. **Hand off both the id and any payload later scenarios need** — e.g. store
  `configApiId` *and* the retrieved `configApiPayload`, since a config-update step reads the payload, mutates
  one field, and PUTs it back. Local scope is keyed by runner instance, and all of a runner's scenarios
  (including Scenario-Outline rows) run on one instance, so values a setup feature stores are visible to every
  later scenario in the same runner.
- **Cleanup for setup fixtures is per-runner, not per-scenario.** Leave the setup feature **untagged by
  `@cleanup`** (it keeps `@setup` for taxonomy) so the per-scenario hook doesn't delete the shared fixture
  mid-run; `BaseBlockRunner`'s `@AfterClass` sweep tears it down once at the end (see §5). `@setup` and
  `@cleanup` are independent: `@setup` = taxonomy/coverage-exclusion, `@cleanup` = per-scenario teardown
  trigger.
- The file is reused **by reference** across runners — each runner runs its own isolated copy and
  resources, so concurrent runners never collide (given unique names — see §4).
- A `_setup_*` filename **must** be tagged `@setup` and vice-versa (lint enforces both directions); it is
  excluded from the coverage tree so its publishing isn't miscounted as product coverage.
- Don't `@dep` the universal baseline ("needs a published API" is assumed, not annotated).

## 11. Gateway invocation (runtime tests)
Features that **invoke** a deployed API through the gateway (`@cap:gateway`) need extra wiring beyond
publish:
- **Start the backend.** The upstream is `NodeAppServer` (a singleton in `tests-common/testcontainers` that
  runs the node sample apps — customer-service on `:3001`, graphql, etc. — on the shared network with alias
  **`nodebackend`**). The parallel-block lane does **not** start it by default. Opt in by adding
  `<parameter name="initBackend" value="true"/>` to the runner's `<test>` block; `BlockLifecycleListener`
  then starts it before APIM boots. Without it the gateway suspends the endpoint and invocation returns
  `303001 … endpoint SUSPENDED` (HTTP 500), not a 200.
- **Address tenant APIs by their full context.** The Publisher API's `context` field for a **tenant** API
  already includes the `/t/<tenant>` gateway prefix. Capture it (`I extract response field "context" …`) and
  invoke with **`I invoke the API at gateway context "{{apiContext}}/…"`**, which uses the context verbatim.
  Do **not** pass a tenant path through the tenant-prefixing `at path` variant as well, or you get a doubled
  `/t/tenant1.com/t/tenant1.com/…` → 404.
- **Invocation is async.** Use the `… until response status code becomes <n> within <s> seconds` invoke
  variants — a freshly published API takes a moment to become routable at the gateway.
- Invocation is a `@cap:gateway` concern with `@dep:publisher`; the publisher features assert only the
  publisher plane and factor the invoke arc out to here.

## 12. Actors, tenants & negatives
There is no mutable "current user". Each scenario resolves an **actor by reference** (`utils/Identity`), and
auth composites set that actor for the rest of the scenario. The block provisions the same actors into BOTH
tenants (`carbon.super` and `tenant1.com`) on boot — pick the one with the least privilege the flow needs:

| Actor ref | Role(s) | Use for |
|-----------|---------|---------|
| `admin` / `admin@tenant1.com` | tenant admin (all) | flows spanning provider+consumer, or admin-only ops (shared scopes, subscription block) |
| `publisherUser` / `publisherUser@tenant1.com` | `Internal/creator,publisher` (NOT admin) | publisher-plane create/deploy/publish |
| `subscriberUser` / `subscriberUser@tenant1.com` | `Internal/subscriber` | devportal consumer ops; the rejected actor in publisher/key-manager negatives |

- **Auth composites** (the `Background`/first step). Bare-name forms default to the super-tenant admin; the
  `as "<actor>"` forms set the acting actor (so subsequent no-arg token lookups in the glue resolve to it):
  - `The system is ready and I have valid publisher access tokens as "<actor>"` — publisher + devportal tokens
    (NO admin token; a non-admin actor is denied `apim:admin`). Default for publisher-plane features.
  - `The system is ready and I have valid devportal access token as "<actor>"` — devportal token only;
    default for devportal consumer features.
  - `I have valid access tokens as "<actor>"` — incl. the admin token (actor must be an admin). Pair with a
    separate `Given The system is ready` (this composite has no readiness prefix).
  - `I act as "<actor>"` — switch the acting actor mid-scenario (no new tokens). Needed when a scenario acts
    as one actor then another. (You no longer need to switch back to the resource-owner before `@cleanup`:
    teardown deletes each resource as the actor that created it — see §5 — so cleanup is correct regardless
    of who is acting at the end.)
- **Tenant ×2.** Run a feature in both tenants with a `Scenario Outline` over an actor column
  (`| admin | admin@tenant1.com |`, etc.) — no per-step changes; the actor's `@domain` drives tenant routing.
- **The acting actor leaks across scenarios — always set it explicitly.** There is no per-scenario reset:
  the acting actor persists from whatever ran last. A `Scenario Outline` leaves it set to its **last Examples
  row** — so an outline with `super` then `tenant1.com` rows leaves the actor on `admin@tenant1.com`. Any
  following scenario that targets a specific tenant's resource must open with its own `Given I act as
  "<actor>"` (or an auth composite); never assume the actor "carried over as admin". Symptom of getting this
  wrong: a **404** looking up a `carbon.super` resource while the leaked actor resolves the lookup against the
  `tenant1.com` org (and `@cleanup` then tries to delete super resources on the wrong org).
- **Negatives.** A token lacking the required scope is rejected by the management API as **401**
  ("Unauthenticated request" — NOT 403). At the gateway, an *unsubscribed* but valid token is **403**, and an
  invalid/garbage credential is **401**. Positive create/update steps assert success internally, so they
  can't be reused for negatives — use the non-asserting **`I attempt to …`** variants (create API / new
  version / shared scope / application / subscribe) and assert the status in the feature. Add a negative only
  where enforcement is real; skip hollow ones (e.g. "search rejected", "key-gen rejected").

## 13. Container config & TOML overlays
Each block boots a container whose `deployment.toml` is resolved by `BlockLifecycleListener`. Most blocks need
nothing here — the **default lane** merges the small shared `basic` overlay
(`artifacts/configFiles/basic/deployment.toml`) onto the product distribution toml, so the config tracks
distribution defaults. Only reach for an overlay when a feature genuinely needs non-default server config.
- **A feature that needs a few extra keys** (e.g. a custom gateway auth header, application sharing, token
  persistence) sets **`tomlExtraOverlayPath`** on its `<test>` block, pointing at a small overlay file with
  *only* those keys. It is merged **on top of** `basic` (base + basic + extra via `Utils.mergeTomls`), so the
  block still inherits every distribution/basic default. This is almost always what you want.
- **Do NOT use `tomlExtraOverlayPath` for keys the default already provides**, and do **NOT** reach for the
  full-file **`tomlOverlayPath`** (verbatim replacement) for product tests — it drops the entire distribution
  config (DB, ports, gateway wiring) unless you restate it all. `tomlOverlayPath` exists for framework-
  verification suites that deliberately supply or omit a complete file.
- **One overlay block can host several features** when their extra config is *orthogonal* (different TOML
  tables, no behavioural collision) — e.g. `IntegrationV2-CustomAuthHeaderAndAppSharing` carries
  `[apim.oauth_config] auth_header` (gateway data-plane) **and** `[apim.devportal] enable_application_sharing`
  in one overlay, saving a whole container. Co-locate only if neither feature's config changes the other's
  behaviour (verify it: a "loud" change like `auth_header` must not break the other feature's flows).
- A config-overlay feature lives in its own `<test>` block; it's still subject to the parallel model, so set
  the block's `parallel="classes" thread-count` per its needs (a feature that **restarts** the container needs
  `thread-count=1` so no sibling class shares the container mid-restart — but it can still run as one of the
  parallel *blocks*).

## Anti-patterns (don't)
Fixed ports · hardcoded resource names · `Thread.sleep` · depending on another scenario's order or
artifacts · shared mutable static state · cleanup in inline scenarios instead of hooks · duplicate
steps or duplicate tests · full-file `tomlOverlayPath` for product tests (use `tomlExtraOverlayPath`) ·
leaving a **stale `httpResponse`** when a request step throws (clear it first / funnel through
`execute`) · retry loops that catch bare `Exception` or don't assert the expected status after the loop.
