# Parallel framework — implementation plan

Step-by-step plan to build the parallel-on-shared execution model designed in
`legacy-baseline-stats.md` (see its **Cross-cutting risks / prerequisites** table).

**Scope of this effort:** core framework changes + provisioning specs only.
We are **not** writing or migrating server-flow tests here — only the machinery
(container lifecycle, dynamic ports, scope namespacing, skip-on-failure, provisioning
extraction) and the verification harnesses that prove each piece works.

## Guiding principles

- **Non-breaking / additive.** Do not modify existing core behaviour. When a change
  to existing logic is needed, **copy it into a new class and add the change there**.
  The legacy path must stay green at every step: provisioning via
  `<class .../SystemInitializationRunner/>` + `<class .../SystemShutdown/>` in the
  existing `testng.xml` keeps working untouched.
- **Reuse what exists.** Testcontainer `start()`/`stop()`/`execInContainer()`
  (`GenericContainer`), the readiness poll in `BaseSteps.waitForAPIMServerToBeReady`
  (`@Then("I wait for the APIM server to be ready")`), the SOAP provisioning bodies in
  `TenantUserInitialisationSteps`, and the existing listeners.
- **Verify before proceeding.** Every implementation step has a verification gate with
  explicit pass criteria. Do not start the next step until the current gate is green and
  confirmed.
- **Two verification harness types:**
  - **(A) Pure-TestNG smoke** — programmatic `XmlSuite`/`TestNG` run, no Docker (the
    approach already used to prove block- vs class-level concurrency, `/tmp/tng-smoke`).
    Fast; for scheduling/scope/skip semantics.
  - **(B) Docker integration probe** — a dedicated verification TestNG suite
    (`testng-framework-verification.xml`) + minimal Cucumber "probe" runners that only
    assert framework state (server ready, ports mapped, shared vars isolated). Needs the
    APIM image. Keep concurrency low (e.g. K=2) to limit resource use.

## Status legend

`[ ]` todo  · `[~]` in progress / stale-needs-reverify · `[x]` done & verified · `[!]` blocked · `[-]` n/a (superseded)

## Isolation contract (why there is no baseline-anchor phase)

The new lane lives **entirely in new files** and is wired only into a **new verification
suite** — it never edits a class or xml that the legacy suite loads. Concretely, *new*:

- a new container class (`DynamicApimContainer`),
- a new TestNG suite (`testng-framework-verification.xml`),
- new listeners (`BlockScopeListener`, `BlockUniquenessLintListener`, `BlockLifecycleListener`),
- a new base runner (`BaseBlockRunner`) + new probe runners + new probe feature files,
- a new provisioner helper (`TenantUserProvisioner`).

The only change to *existing* shared code is **additive**: a new
`TestContext.sharedScopeId(ITestContext)` helper and the fact that `TestContext.setScope`
already accepts any string. `TestNameMdcListener` and `ParallelToggleAlterSuiteListener`
(registered in the legacy `testng.xml`) are left **byte-for-byte unchanged**.

Because nothing the legacy suite loads changes, a green legacy run cannot regress from this
work, so an explicit baseline-anchor phase is unnecessary. The residual safety net is
verification **4.15** (mixed-lane co-existence) and **5.4** (legacy parity), which assert the
legacy lane still behaves when the two lanes share a JVM.

**Hard constraint:** the `onStart` lifecycle (Phase 4) and the per-step scope listener
(Phase 2) must compute the shared-scope key **identically**, via the single shared
`TestContext.sharedScopeId(...)` helper — never by re-deriving it inline.

> The two `pom.xml` edits and the `testng.xml` comment-outs currently in the working tree
> are temporary single-test harness changes on the legacy suite. Leave them as-is for now;
> they are **not** part of this plan and will be reverted during final cleanup (out of scope).

## Verification harness conventions

Every gate ships a **re-runnable** script so any step can be re-checked in isolation at any
time (regression-style). Rules all scripts follow:

- **Self-asserting.** The script decides PASS/FAIL itself, prints a single final
  `VERIFY <step>: PASS` / `VERIFY <step>: FAIL ...` line, and **exits non-zero on failure**
  (CI-friendly).
- **Idempotent.** It provisions its own state and cleans up after itself (stops containers,
  prunes by label), so back-to-back runs start clean. Never depends on a prior gate's leftovers.
- **Located together.** All scripts live in
  `tests-integration/cucumber-tests/src/test/scripts/verification/`, named `verify-<step>.sh`
  (e.g. `verify-1.1.sh`), with a `verify-all.sh` that runs them in order and stops at the first
  failure.

Two harness types (as in Guiding principles):

- **Type-A (no Docker).** A standalone Java `main` (the `/tmp/tng-smoke` style) compiled and run
  by the wrapper, which greps its `PASS/FAIL` line. Used for scheduling/scope/skip semantics.
- **Type-B (Docker).** The wrapper runs Maven against a **per-step** verification suite
  (`-Dsurefire.suite.xml=testng-fv-<step>.xml`) so the test JVM inherits the **exact** surefire
  env (DB env vars, truststore, image name) and runs **only** that step's probe. (Per-step files
  are required because the pom hard-wires `suiteXmlFiles`, so `-Dtest` filtering is ignored.)
  After the run the wrapper asserts on **observable** state the test can't fake — `docker ps -a`
  shows zero leftovers with our label, previously mapped host ports are socket-refused. Keep K
  low (≤2). A later capstone (Phase 6) gets its own aggregate suite.

Each Type-B script tags its containers with a unique label (e.g. `verify-1.1`) and, on both
success and failure paths, runs `docker ps -aq --filter "label=..." | xargs -r docker rm -f` so a
crashed run never poisons the next.

## Traceability — phase → risk-table row

| Phase | Risk-table row it implements |
| --- | --- |
| 1 | Port robustness (dynamic host-port mapping) |
| 2 | `<test name>` uniqueness (namespaced scope key) — part 1 |
| 3 | `<test name>` uniqueness (composite lint) — part 2 |
| 4 | `onStart` boot-failure guard; Blocker B (two-level concurrency); Data-isolation (scope) |
| 5 | Req 5 provisioning refactor; Blocker A (H2 dead-code removal) |
| 6 | Capstone — all of the above together |

---

## Phase 1 — Dynamic-port container (new class)

Implements the **Port robustness** decision: `portOffset=0` + `withExposedPorts` +
`getMappedPort`, with named URL accessors. (The legacy fixed-port `APIMContainer` was **removed
2026-07-01** as unused dead code — `DynamicApimContainer` is now the sole container.)

- `[x]` **1.1 Create `DynamicApimContainer`** in `tests-common/testcontainers`,
  copied from `APIMContainer` with these deltas:
  *(Verified 2026-06-23 via `verify-1.1.sh` → `testng-fv-1.1.xml` +
  `DynamicApimContainerVerificationTest`: real container booted, ports mapped to ephemeral
  host ports, health-check 200, ports released after stop, no container leaks.)*
  - `withExposedPorts(HTTPS_PORT, HTTP_PORT, GATEWAY_HTTPS_PORT, GATEWAY_HTTP_PORT)`
    instead of `addFixedExposedPort(...)`.
  - `-DportOffset=0` (each container has its own network namespace).
  - **Drop the offset counter and the DB-name `.replace(...)` block** (Blocker A dead code;
    H2 is file-isolated per container). Keep the DB env wiring.
  - Add accessors: `getServletHttpsUrl()` (9443), `getServletHttpUrl()` (9763),
    `getGatewayHttpsUrl()` (8243), `getGatewayHttpUrl()` (8280), each via
    `getMappedPort(internalPort)`.
  - Keep `waitingFor(Wait.forListeningPort()...)` and `execInContainer`/toml copy as-is.
  - **Verify (B, single container):** start one `DynamicApimContainer`; assert
    `getMappedPort(9443) != 9443` (ephemeral), the four URLs are well-formed, the servlet
    HTTPS port answers and the gateway health-check returns 200. Then `stop()` and assert
    **port release**: a socket connect to the previously mapped host port is refused within
    a short timeout, and `docker ps -a` shows the container gone.
  - **Confirm gate.**

- `[x]` **1.2 Multiple containers in parallel.** Start 2–3 `DynamicApimContainer`
  instances concurrently.
  - **Verify (B):** all get **distinct** host ports (no `address already in use`); all
    four URLs reachable per instance; each container's gateway health-check is 200;
    after stopping all, every mapped port is released and no containers leak.
  - **Confirm gate.**
  *(Verified 2026-06-23 via `verify-1.2.sh` → `testng-fv-1.2.xml` +
  `DynamicApimContainerParallelVerificationTest`: 2 containers booted concurrently, 8 distinct
  host ports, both healthy, all ports released, no leaks.)*

- `[-]` _(DEPRECATED 2026-06-30 — old direct-container e2e-invoke probe; its gateway-invocation property is superseded by **7.7** (and already green via `gateway/*` + the 159-test suite). **REMOVED 2026-07-01**: `testng-fv-1.3.xml`, `verify-1.3.sh`, `DynamicLifecycleVerificationRunner`, and `dynamic_lifecycle.feature` deleted as superseded dead code. `TestNameMdcListener` and the `...verification.steps` glue are retained — still used by 2.1 and the block probe runners respectively.)_ **1.3 Verify — accepted self-URL caveat is actually harmless end-to-end (B).**
  The risk we accepted: host port ≠ internal port. Prove the flows we *use* still work
  through dynamic ports: DCR → obtain a token (via mapped servlet-https port) → deploy a
  trivial API → **invoke it through the mapped gateway port** and get a valid gateway
  response. If this passes, the caveat is confirmed non-blocking for the H2 first cut.
  - **Confirm gate.**
  *(Verified 2026-06-23 via `verify-1.3.sh` → `testng-fv-1.3.xml` +
  `DynamicLifecycleVerificationRunner` + `dynamic_lifecycle.feature` +
  `DynamicContainerVerificationSteps`: real container on dynamic ports drove DCR → tokens →
  create+deploy+publish → subscribe+token → gateway invocation returned 200 through the mapped
  gateway port; servlet-https port released after stop, no container leaks. Reused the existing
  Cucumber step library; new glue isolated in `...verification.steps`.)*

- `[x]` **1.4 Verify — misuse + abnormal-termination behavior (B).**
  - `getMappedPort(port)` **before** `start()` fails fast with a clear error (not a silent
    0 / wrong port).
  - Reuse flag interaction: with `testcontainers.reuse.enable=true` (Ryuk disabled),
    confirm whether a **killed** (not gracefully stopped) container leaks host ports, and
    document the cleanup expectation (e.g. JVM shutdown hook or documented manual prune).
  - **Confirm gate.**
  *(Verified 2026-06-23 via `verify-1.4.sh` → `testng-fv-1.4.xml` +
  `DynamicApimContainerMisuseVerificationTest`. Findings: `getMappedPort()` before `start()`
  throws `IllegalStateException("Mapped port can only be obtained after the container is
  started")` — fails fast, no silent 0. A hard `docker kill` (abnormal exit, bypassing graceful
  `stop()`) **releases the host port** — no host-port leak — but leaves the container as an
  `exited` record that survives until explicitly pruned. **Cleanup expectation:** abnormally
  terminated containers need a manual/scripted prune (the verify scripts do this via the
  `verify-step` label on every exit path); a killed container does not strand its host port.)*

---

## Phase 2 — Namespaced shared-scope key

Implements **`<test name>` uniqueness, part 1**: key the shared scope by
`suiteName::testName` so same-named blocks can't merge state.

- `[-]` _(N/A 2026-06-30 — verify-2.1's legacy-`testng.xml` listener-wiring assertion is superseded by the v2 lane; the v2 scope derivation itself stays verified by 2.2/2.3.)_ **2.1 Add centralized derivation `TestContext.sharedScopeId(ITestContext)`**
  (additive — new method only) returning `ctx.getSuite().getName() + "::" + ctx.getName()`.
  Then add a **new** `BlockScopeListener` (a new `IInvokedMethodListener`, **not** an edit to
  `TestNameMdcListener`) that calls `TestContext.setScope(...)` using this helper, and
  register it **only** in `testng-framework-verification.xml`. The Phase 4 lifecycle listener
  uses the same helper (per the isolation contract's hard constraint). The legacy
  `TestNameMdcListener` stays byte-for-byte unchanged. Local-scope id may optionally be
  suite-prefixed (low priority).
  - **Verify (A, no Docker):** programmatic suite with **two `<test>` blocks of the same
    name** (and/or two suites), with `BlockScopeListener` registered. In each block write a
    sentinel via `setShared("sentinel", <blockId>)` and read it back from a method in that
    block. Assert each block reads **its own** sentinel — no cross-block override. (Optional
    negative: show the bare-`testName` key collides, to prove the namespacing is what fixes it.)
  - **Verify regression:** the legacy `testng.xml` run is **unaffected** — it never loads
    `BlockScopeListener` and `TestNameMdcListener` is unchanged.
  - **Confirm gate.**
  *(Verified 2026-06-23 via `verify-2.1.sh` → `testng-fv-2.1.xml` +
  `BlockScopeIsolationVerificationTest` + `SentinelProbe`. `TestContext.sharedScopeId` added
  (additive); new `BlockScopeListener` keys shared scope by `suiteName::testName` and namespaces
  the local scope too; registered only in the fv suite. Positive: two same-named `SharedBlock`
  `<test>` blocks across `FV-2.1-SuiteA`/`FV-2.1-SuiteB` (real nested TestNG runs through
  `BlockScopeListener`) each read back their own sentinel (`A`/`B`) — no merge. Negative: the
  legacy bare-`testName` key collided (last writer won, first value lost). Regression: script
  asserts legacy `testng.xml` never references `BlockScopeListener` and still wires
  `TestNameMdcListener`; the latter is byte-for-byte unchanged.)*

- `[x]` **2.2 Verify — scope visibility on the *executing* thread (A, critical).** The
  scope is an `InheritableThreadLocal` set in `beforeInvocation`. Cucumber/TestNG may run
  the scenario body on a different thread than the one that set the scope — especially with
  `data-provider-thread-count > 1`. Run a block with `data-provider-thread-count=2` (and
  with parallel classes) and assert every step reads the **correct** block's shared vars,
  never the default/global scope or a sibling's. If the ThreadLocal isn't visible on the
  worker thread, this surfaces here before it corrupts real runs.
  - **Confirm gate.**
  *(Verified 2026-06-23 via `verify-2.2.sh` → `testng-fv-2.2.xml` +
  `BlockScopeThreadVisibilityVerificationTest` + `ThreadScopeProbe`. Nested suite of 3
  `parallel="tests"` blocks, each with a `parallel=true` data provider and
  `data-provider-thread-count=2`, `BlockScopeListener` registered. Global scope pre-seeded with
  a `POISON` sentinel. Result: 6 invocations ran concurrently on multiple worker threads
  (`TestNG-PoolService-0/1`); every one read back its own block id — none saw the global POISON
  (scope was visible on the worker thread) and none saw a sibling's value (no cross-block leak).
  The test also asserts >1 distinct worker thread so the race is actually exercised.)*

- `[x]` **2.3 Verify — no scope leak across many blocks (A).** Run a suite with many
  short blocks (e.g. 20) and assert `sharedContexts`/`localContexts` do not grow unbounded
  — each block's entry is removed on `clear()`/`onFinish` (no map-entry-per-block buildup).
  - **Confirm gate.**
  *(Verified 2026-06-23 via `verify-2.3.sh` → `testng-fv-2.3.xml` + `BlockScopeLeakVerificationTest`
  + `ManyBlocksProbe`. Added additive `TestContext.sharedScopeCount()/localScopeCount()` for
  observability. Positive: 20 blocks each clearing on teardown → shared/local map growth = 0.
  Negative control: 20 blocks without teardown → growth = 20 each, confirming `clear()` is what
  reclaims the entry. Per-block `clear()` will be driven by Phase 4's `BlockLifecycleListener.onFinish`.)*

---

## Phase 3 — Suite/test-name uniqueness lint

Implements **`<test name>` uniqueness, part 2**: fail fast at suite load, before any boot.

- `[-]` _(N/A 2026-06-30 — verify-3.1's legacy-`testng.xml` `ParallelToggleAlterSuiteListener` assertion is superseded by the v2 lane; the v2 uniqueness lint is now `render_coverage_tree.py`, tracked at 7.9.)_ **3.1 Add a new `BlockUniquenessLintListener`** (a **new** `IAlterSuiteListener`,
  **not** an edit to `ParallelToggleAlterSuiteListener`) that validates the **composite**
  `suiteName::testName` for global uniqueness across `suites` + `getChildSuites()`, and
  rejects unnamed/default suite names (TestNG's "Default Suite" fallback would defeat
  namespacing). Throw on violation. Register it **only** in
  `testng-framework-verification.xml`; the legacy `ParallelToggleAlterSuiteListener` is left
  byte-for-byte unchanged.
  - **Verify (A):** (a) a suite (with this listener) with duplicate composite names → run
    **fails fast at load**, before any container boot; (b) an unnamed `<suite>` → fails with
    a clear message; (c) a valid uniquely-named suite → passes through unchanged.
  - **Verify regression:** legacy suite is **unaffected** — it never loads the lint listener.
  - **Confirm gate.**
  *(Verified 2026-06-23 via `verify-3.1.sh` → `testng-fv-3.1.xml` + `BlockUniquenessLintVerificationTest`
  + `UniquenessProbe`. New `BlockUniquenessLintListener` (IAlterSuiteListener) validates the composite
  `suiteName::testName` across `suites` + `getChildSuites()` and rejects unnamed/"Default Suite" names;
  registered only in the fv suite, `ParallelToggleAlterSuiteListener` untouched. 5 Type-A cases pass:
  duplicate across same-named suites, duplicate across a child suite (recursion), unnamed suite, and a
  duplicate in a real `SuiteXmlParser`-parsed XML are all rejected by `alter()` at load; distinct
  composites across suites + a child pass through unchanged. Regression guards assert legacy `testng.xml`
  never references the lint and still wires `ParallelToggleAlterSuiteListener`. Note: a nested programmatic
  TestNG run is not used to prove fail-fast — TestNG doesn't dispatch alter-suite listeners for file-based
  inner runs — so the real-run case parses the fixture and invokes the exact `alter()` load hook directly.)*

---

## Phase 4 — Block lifecycle listener + base runner + skip guard

Implements **`onStart` boot-failure guard**, **Blocker B** (two-level concurrency wiring),
and the scope half of **Data-isolation**. This is the core of the redesign. The legacy
sibling-class pattern (`SystemInitializationRunner`/`SystemShutdown`) is **kept** and is
not used by this new lane.

- `[x]` **4.1 `BaseBlockRunner extends AbstractTestNGCucumberTests`** with the inherited
  skip-guard only:
  ```java
  @BeforeClass(alwaysRun = true)
  void abortIfBlockBootFailed(ITestContext c) {
      Object e = c.getAttribute("bootError");
      if (e != null) throw new SkipException("APIM block boot failed", (Throwable) e);
  }
  ```
  Test-class authors get this by extending it; they write no lifecycle code.
  *(Done 2026-06-23. New `org.wso2.am.integration.cucumbertests.runners.block.BaseBlockRunner` with the
  package-private `@BeforeClass(alwaysRun=true)` guard reading the `bootError` attribute (constant
  `BOOT_ERROR_ATTRIBUTE`) and raising `SkipException(msg, cause)`. Verified Type-A (no Docker) via
  `verify-4.1.sh` → `testng-fv-4.1.xml` + `BaseBlockRunnerVerificationTest`: with a recorded bootError the
  guard throws SkipException whose cause is that error (per-class SKIP, no NPE cascade); with none it is a
  no-op. Note: the test uses the real `ITestContext` TestNG injects into each `@Test` and only touches its
  IAttributes methods — a hand-rolled `Proxy`/stub over `ITestContext` fails with `NoClassDefFoundError:
  com.google.inject.Injector` because proxy generation force-resolves ITestContext's Guice-typed methods,
  and Guice isn't on the runtime classpath.)*

- `[x]` **4.2 `BlockLifecycleListener implements ITestListener`** (fires once per `<test>`):
  <!-- Verified 2026-06-23: created BlockLifecycleListener (onStart boots one DynamicApimContainer,
       runs readiness gate via extracted ServerReadiness helper, stores container + baseUrl/baseGatewayUrl
       in shared scope, records `bootError` attribute on failure without throwing; onFinish stops the
       container null-guarded and clears scope). Readiness poll extracted to shared `ServerReadiness`
       and `BaseSteps.waitForAPIMServerToBeReady()` refactored to delegate to it. The BaseSteps
       refactor — the only edit to a pre-existing shared file — was smoke-tested by re-running
       verify-1.3.sh (boots a real container and exercises "I wait for the APIM server to be ready"
       now routed through ServerReadiness): VERIFY 1.3 PASS (full lifecycle through dynamic gateway
       port 200, port released, no leaks), which also confirms the whole source set test-compiles
       cleanly. No dedicated verify-4.2 per plan; functional gates are 4.4-4.6 (need 4.3 probe runners). -->

  - `onStart(ctx)`: set scope via `TestContext.sharedScopeId(ctx)`; read block config from
    `<parameter>` (toml-overlay path/label); boot **one** `DynamicApimContainer`; run the
    readiness gate (reuse the `BaseSteps` health-check poll logic, extracted to a shared
    helper); on success store the container + `baseUrl`/`baseGatewayUrl` in shared scope.
    **On boot/readiness failure: `ctx.setAttribute("bootError", t)` — do not throw.**
  - `onFinish(ctx)`: `container.stop()` (null-guarded so it's a no-op when boot produced no
    container); clear scope. Dynamic host ports are released by Docker on stop.
  - Register it in the new `testng-framework-verification.xml` (not the legacy suite).

- `[x]` **4.3 Minimal probe runners + feature.** Create 2–3 tiny Cucumber runners
  extending `BaseBlockRunner` under a new `features/framework-verification/` area whose
  scenarios only assert framework state (e.g. "the APIM server is ready",
  "the shared baseUrl is present"). No server-flow logic. Reuse the existing
  "I wait for the APIM server to be ready" step.
  <!-- Done 2026-06-23: feature `features/framework-verification/block_probe.feature` (one scenario:
       reuses BaseSteps "I wait for the APIM server to be ready", then asserts "the shared baseUrl is
       present" and "the shared gateway URL is present"). New step glue
       `verification/steps/BlockProbeSteps.java` (the two present-and-non-blank assertions reading
       TestContext "baseUrl"/"baseGatewayUrl"). Two runner classes `runners/block/BlockProbeRunnerOne`
       and `BlockProbeRunnerTwo` extending BaseBlockRunner (inherit boot-failure skip guard), both
       pointing at the probe feature with glue = stepdefinitions + verification.steps. Reactor
       test-compile clean; classes + feature present on the test classpath. No verify script at 4.3 per
       plan — functional gates are 4.4+ (need a testng-fv suite registering BlockLifecycleListener). -->


- `[x]` **4.4 Verify — boot once + readiness gate (B).** One block with 2 probe classes.
  - **Verify:** exactly **one** container booted for the block (not per class); both probe
    classes observe a **ready** server and the **same** `baseUrl`/gateway URL; this proves
    classes wait until the server is ready (config method completes before test methods).
  - **Confirm gate.**
  <!-- Verified 2026-06-23: testng-fv-4.4.xml = one <test> "Phase4.4-Block" (param blockLabel=fv-4.4)
       with BlockProbeRunnerOne + BlockProbeRunnerTwo, registering BOTH listeners: BlockLifecycleListener
       (onStart boots one container + readiness gate + publishes baseUrl/gateway into the block's shared
       scope) and BlockScopeListener (sets that shared scope on each worker invocation so both classes
       read it). Extended block_probe.feature with "I record the block observation" -> BlockProbeSteps
       appends millis|thread|containerId|baseUrl|gatewayUrl to target/fv-block-observations.txt.
       verify-4.4.sh asserts: exactly 2 observations; 1 distinct real container id (boot-once, not
       per class); 1 distinct baseUrl + 1 distinct gateway URL (same ready server); no leftover
       containers. Log confirms single onStart boot (baseUrl=...:33073) and onFinish stop. VERIFY 4.4
       PASS. NOTE: strict host-port socket-release check deliberately deferred to its dedicated gate 4.6
       (polled with grace period) - on colima the port-forward lingers a beat after stop, so a bare
       post-run socket probe is a timing artifact, not a leak; 4.4's scope is boot-once + readiness. -->


- `[x]` **4.5 Verify — skip-on-failure (B).** Force a boot failure (bad image tag or
  invalid toml overlay via `<parameter>`).
  - **Verify:** every class in the block is **SKIPPED** (not FAILED), with the boot
    exception as the single root cause and **no NPE cascade**; `onFinish` runs and no-ops
    cleanly (no leaked container/ports).
  - **Confirm gate.**
  <!-- Verified 2026-06-23: testng-fv-4.5.xml = block "Phase4.5-Block" (blockLabel=fv-4.5) with
       tomlOverlayPath=/nonexistent/fv-4.5/deployment.toml, so onStart's Files.readString throws
       NoSuchFileException -> recorded as bootError (no throw) -> BaseBlockRunner guard -> per-class
       SkipException. verify-4.5.sh asserts Maven build SUCCEEDS (skips != failures); testng-results
       skipped=2, failed=0, passed=0; results carry "APIM block boot failed" + NoSuchFileException
       (diagnosable root cause, not a blank skip); NO NullPointerException (no NPE cascade); observation
       file NOT produced (no probe step ran); no leaked containers (onFinish no-op, none created).
       VERIFY 4.5 PASS. -->


- `[x]` **4.6 Verify — teardown + release (B).** After a successful block finishes.
  - **Verify:** container stopped, mapped host ports released (socket refused), no leftover
    containers.
  - **Confirm gate.**
  <!-- Verified 2026-06-23: testng-fv-4.6.xml = one successful block "Phase4.6-Block"
       (blockLabel=fv-4.6, single BlockProbeRunnerOne). verify-4.6.sh asserts: >=1 observation with a
       real container id (block ran against a booted server); no container with the block label remains
       (onFinish stop tore it down); the mapped servlet-https host port (parsed from the recorded
       baseUrl) becomes refused. The host-port check is POLLED for up to 30s (grace period) because a
       stopped container's port-forward lingers a beat on colima - a genuine leak never releases and is
       still caught. VERIFY 4.6 PASS. -->


- `[x]` **4.7 Verify — two-level concurrency, real containers (B, scaled).** Suite
  `parallel=tests thread-count=K` with N>K blocks, each block `parallel=classes
  thread-count=M` with >M probe classes. Use K=2, M=2 to bound resources.
  - **Verify:** at most **K** containers run simultaneously (distinct ports; observe via
    timestamps/`docker ps`); within a block at most **M** classes run at once; excess
    queues. (Cross-check against the earlier pure-TestNG bound result.)
  - **Confirm gate.**
  <!-- Verified 2026-06-23: confirmed TestNG 7.4.0 DTD allows parallel+thread-count on <test>, enabling
       true two-level concurrency in static XML. Added BlockProbeRunnerThree. testng-fv-4.7.xml = suite
       parallel="tests" thread-count="2" (K=2) with N=3 blocks Phase4.7-BlockA/B/C (distinct <test> names
       => distinct shared scopes, but all share docker label block=fv-4.7 so one filter counts them),
       each parallel="classes" thread-count="2" (M=2) over 3 probe classes. verify-4.7.sh backgrounds
       Maven and polls `docker ps` for peak live block containers, then asserts: suite passed; 9
       observations across exactly 3 distinct real container ids; peak live containers <=K AND >=2 (cap
       is a real bound and blocks truly overlapped); within each block <=M distinct worker threads.
       Result: 9 obs / 3 containers / peak 2 live / exactly 2 threads per block (e.g. BlockA-1,BlockA-2)
       => 3rd class queued. VERIFY 4.7 PASS. -->


- `[x]` **4.8 Verify — `onFinish` always releases, including on test FAILURE (B).** A block
  whose probe test deliberately fails (and, separately, one that errors) must still trigger
  `onFinish` → container stopped + ports released. Teardown must not be conditional on
  success/skip.
  - **Confirm gate.**
  <!-- 2026-06-23 VERIFIED: testng-fv-4.8.xml (BlockFail asserts-fail, BlockError throws; each boots a
       real container, records its obs, then fails) + verify-4.8.sh. Result: Maven non-zero (failed=2,
       passed=0) yet onFinish stopped both containers and released their host ports (polled grace), no
       leaks. VERIFY 4.8 PASS. -->

- `[x]` **4.9 Verify — boot-failure isolation across parallel blocks (B).** With K parallel
  blocks where **one** block's boot fails: the failing block skips, and the **other blocks
  still pass** — a bad boot (or a hard, unexpected `onStart` RuntimeException) must not
  fail/skip sibling blocks or abort the suite.
  - **Confirm gate.**
  <!-- 2026-06-23 VERIFIED: testng-fv-4.9.xml (parallel=tests thread-count=2; BlockBad bad-toml boot
       fails, BlockGoodA/B boot real containers) + verify-4.9.sh. Result: build SUCCEEDED, failed=0,
       passed=2, skipped=1; skip carried boot cause; no NPE; 2 obs / 2 ids; no leaks. VERIFY 4.9 PASS. -->

- `[x]` **4.10 Verify — teardown idempotency / double-stop (A or B).** `onFinish` on a
  block where boot produced no container (null-guard), and `stop()` invoked when already
  stopped, must be no-ops — no exception, no spurious error in the report.
  - **Confirm gate.**
  <!-- 2026-06-23 VERIFIED: testng-fv-4.10.xml (DoubleStop probe stops its own container so onFinish
       double-stops; NoContainer bad-toml boot => onFinish null-guard) + verify-4.10.sh. Result: build
       SUCCEEDED, failed=0, passed=1, skipped=1; no NPE; 1 obs; no leaks. VERIFY 4.10 PASS. -->

- `[x]` **4.11 Verify — skip reason is diagnosable (B).** The SKIPPED results from 4.5/4.9
  carry the **boot exception as cause** in the TestNG/cucumber report (not a blank skip), so
  a failure is debuggable.
  - **Confirm gate.**
  <!-- 2026-06-23 VERIFIED: testng-fv-4.11.xml (bad toml overlay with a distinctive path marker
       fv-4.11-diagnostic-marker) + verify-4.11.sh. Fast/no-Docker. Result: build SUCCEEDED, skipped=1
       failed=0 passed=0; report records class="org.testng.SkipException" + "APIM block boot failed" +
       a "Caused by:" chain with NoSuchFileException naming the exact marker file; same chain surfaced in
       the Maven console; no NPE. Stronger than 4.5's text checks (asserts exception CLASS + causal chain
       + exact failing artifact). VERIFY 4.11 PASS. -->

- `[x]` **4.12 Verify — suite cap negative control (B, scaled).** Mirroring the pure-TestNG
  negative control (`SmokeTest2`): N=3 blocks with suite `thread-count=2` must **never** show
  3 live containers at once — the 3rd waits. Proves K is a real container bound, not just a
  thread bound.
  - **Confirm gate.**
  <!-- 2026-06-23 VERIFIED: testng-fv-4.12.xml (N=3 SINGLE-class blocks Phase4.12-BlockA/B/C, suite
       parallel="tests" thread-count="2"=K, shared label block=fv-4.12) + verify-4.12.sh. Single-class
       blocks isolate the suite-level cap from inner class parallelism (vs 4.7). Script backgrounds Maven,
       polls `docker ps` for the peak live count. Result: 3 obs / 3 distinct container ids; peak live = 2
       (NEVER reached 3 => cap held; reached 2 => cap actually exercised, not a vacuous serial run); all
       passed. VERIFY 4.12 PASS. -->

- `[x]` **4.13 Verify — readiness gate rejects partial boot (B).** Simulate a container that
  opens its TCP port but whose app never serves 200 (e.g. point the health poll at a port
  that listens but 404s, or use a deliberately broken overlay). The gate must **time out →
  set `bootError` → skip**, never report a false "ready". Confirms readiness gates on the
  HTTP health-check, not just `Wait.forListeningPort`.
  - **Confirm gate.**
  <!-- 2026-06-23 VERIFIED: added additive ServerReadiness.awaitReady(baseUrl, timeoutMillis) overload
       (existing no-timeout method delegates with SERVER_STARTUP_WAIT_TIME). PartialBootReadinessVerification
       Test (testng-fv-4.13.xml) + verify-4.13.sh. NEGATIVE control runs a REAL Testcontainers container
       (node-app-server, the framework's own backend image, already local) that LISTENS on its port but
       404s on the gateway health-check path: socket proves the port is open, yet awaitReady(...,10s)
       returns false => a Wait.forListeningPort gate would have falsely passed it. POSITIVE control: an
       in-JVM 200-serving HttpServer makes the same gate return true (non-vacuity). NOTE: nginx would be
       the textbook stub but this env cannot pull public images (Docker Hub unreachable from the colima
       VM); node-app-server gives the same listens-but-non-200 shape with no registry pull. Result: build
       SUCCEEDED, passed=1 failed=0 skipped=0, no leaks. VERIFY 4.13 PASS. -->

- `[x]` **4.14 Verify — `<parameter>` handling (B).** (a) Block with **no**
  `initTenantUsers` / no overlay param → defaults safely (provisioning skipped, base toml
  used), no NPE. (b) Block **with** an overlay param → the running container's
  `deployment.toml` reflects the merged delta (confirm by `cat`-ing it inside the container,
  as the legacy step already does).
  - **Confirm gate.**
  <!-- 2026-06-23 DONE: testng-fv-4.14.xml runs two REAL APIM blocks serially (label block=fv-4.14):
       Phase4.14-Defaults sets no tomlOverlayPath -> listener falls back to base deployment.toml, boots
       ready (no NPE), probe cats the in-container toml and asserts the overlay marker is ABSENT.
       Phase4.14-Overlay sets tomlOverlayPath=target/fv-4.14-deployment.toml (= base toml + a
       "# FV-4.14-OVERLAY-MARKER" comment, generated by verify-4.14.sh); probe cats the in-container toml
       and asserts the marker is PRESENT - proving the param reached the running container, not just disk.
       New marker steps added to BlockProbeSteps via apim.execInContainer("cat", getContainerTomlPath()).
       verify-4.14.sh asserts build SUCCESS, 2 obs across 2 distinct real container ids, no label leaks.
       Verified PASS. Files: BlockProbeDefaultsRunner/BlockProbeOverlayRunner, block_probe_defaults.feature,
       block_probe_overlay.feature, testng-fv-4.14.xml, verify-4.14.sh. -->


- `[-]` _(N/A 2026-06-30 — exercises legacy-lane + lifecycle-lane co-existence; the legacy lane it depends on was superseded, so this no longer applies. Suite + script removed 2026-07-01, along with the legacy `SystemInitializationRunner`/`APIMContainer` they drove.)_ **4.15 Verify — mixed-lane co-existence (B).** Because listeners are suite-global,
  run a suite containing **both** a legacy fixed-port `<test>` block (SystemInitializationRunner
  + SystemShutdown) **and** a new lifecycle block. The `BlockLifecycleListener` must **no-op**
  for the legacy block (it isn't opted in) and not disturb it; both lanes pass.
  - **Confirm gate.**
  <!-- 2026-06-23 DONE: Added an opt-in gate to BlockLifecycleListener - onStart/onFinish now read the
       blockLabel <parameter> first and return early (no-op) when it is absent/blank, so a block joins the
       parallel-on-shared lane only by declaring blockLabel. (Removed the now-unused paramOrDefault helper;
       the listener no longer falls back to the <test> name.) testng-fv-4.15.xml mixes BOTH lanes in one
       suite (parallel=false): Phase4.15-Legacy (no blockLabel) self-boots a legacy fixed-port APIMContainer
       via SystemInitializationRunner + SystemShutdown; Phase4.15-Lifecycle (blockLabel=fv-4.15) lets the
       listener boot its DynamicApimContainer and BlockProbeRunnerOne records one observation.
       verify-4.15.sh proves the no-op by asserting NO container carries label block=Phase4.15-Legacy (the
       fallback label the listener WOULD have stamped pre-opt-in), exactly 1 observation on 1 real container
       id, and no fv-4.15 leak. Both REAL containers; verified PASS. Files: BlockLifecycleListener (opt-in),
       testng-fv-4.15.xml, verify-4.15.sh (reuses BlockProbeRunnerOne + legacy init/shutdown runners). -->


---

## Phase 5 — Provisioning refactor (lifecycle-driven)

Implements **Req 5 provisioning refactor**. `TenantUserInitialisationSteps` and the
legacy `TenantUserInitializationRunner` are **left working**; we add a copy callable
outside a scenario.

- `[x]` **5.1 Extract `TenantUserProvisioner`** (plain Java helper) by copying the SOAP-build
  + "skip if exists" bodies from `TenantUserInitialisationSteps` verbatim:
  `addSuperTenant()`, `addTenant(domain, admin, pass, first, last, email)`,
  `addUser(domain, key, user, pass, roles)`. It keeps writing tenants/users to
  `TestContext` shared scope exactly as the steps do (so existing readers are unaffected).
  Legacy steps are not modified in this phase.
  <!-- 2026-06-23 DONE: Added cucumbertests/utils/TenantUserProvisioner (final class, private ctor, static
       methods - mirrors ServerReadiness). Copied the SOAP payloads + skip-if-exists + setShared bodies
       verbatim from TenantUserInitialisationSteps; only changes are dropping @When/@And + DataTable for
       plain params and folding each "retrieve existing" step (retrieveTenants / listUsers) into the create
       method that consumes it (private retrieveExistingTenantDomains() / retrieveExistingUsers(Tenant)).
       Reads baseUrl from and writes tenant/user beans to TestContext shared scope under the tenant-domain
       key exactly as the steps do, so legacy readers are unaffected. Legacy steps UNCHANGED. No verify gate
       for 5.1 (extract-only); test-compile of the module passes clean. 5.2 will call it from onStart. -->


- `[x]` **5.2 Drive provisioning from the lifecycle.** In `onStart`, **after** boot +
  `baseUrl`, gated on `<parameter name="initTenantUsers">` (a second parameter selects the
  tenant set — `default` vs migration's `adpsample`), call `TenantUserProvisioner`. Scope
  is already set to the composite key, so `setShared` writes land on the block key the
  probe classes read.
  <!-- 2026-06-23 DONE: BlockLifecycleListener.onStart now, after publishing baseUrl into the block scope
       and INSIDE the boot try (so failures become bootError -> clean skip, setting up 5.5), checks
       Boolean.parseBoolean(param initTenantUsers); when true it calls a private provisionTenantUsers(label,
       param tenantSet). tenantSet=adpsample provisions the migration set (addAdpsampleTenant + the adpsample
       user, matching migrated_tenant_user_initialization.feature); anything else = the default set
       (addSuperTenant + tenant1.com + the two users, matching tenant_users_initialisation.feature). New
       params: initTenantUsers (gate), tenantSet (selector). Added TenantUserProvisioner.addAdpsampleTenant()
       (copied verbatim from the "I add adpsample tenant to context" step - bean only, no SOAP). The
       provisioner reads baseUrl from / writes beans to the block's shared scope (set by onStart), so writes
       land on the composite block key the probe classes read. Legacy steps/runners UNCHANGED. No verify gate
       for 5.2 (5.3 is the gate); module test-compile passes clean.
       NOTE: the adpsample tenant set (the adpsample branch in provisionTenantUsers + TenantUserProvisioner.
       addAdpsampleTenant) only serves the migration profile. If the integration tests never provision the
       adpsample set, both can be removed as dead code - default would then be the only set. -->


- `[-]` _(DEPRECATED 2026-06-30 — probe asserts pre-actor-model provisioning shape (Tenant bean / `userKey1`); superseded by **7.2 + 7.3** against the actor model. Provisioning itself works: 159-test suite + 6.1 green. `verify-5.3.sh` is guarded (not run).)_ **5.3 Verify — provisioning in a fresh container (B).** A block with
  `initTenantUsers=true` and a probe class.
  - **Verify:** tenant(s) + user(s) exist in the freshly booted container (query back via
    the same SOAP/REST the steps use); the tenant beans are readable from shared scope
    under the composite key; the probe reads `CURRENT_TENANT`/users correctly.
  - **Confirm gate.**
  <!-- 2026-06-23: PASS. testng-fv-5.3.xml runs one REAL block (block=fv-5.3,
       initTenantUsers=true, no tenantSet => default set) with BlockLifecycleListener +
       BlockScopeListener; BlockLifecycleListener.onStart boots the DynamicApimContainer,
       gates readiness, then provisions super tenant + tenant1.com + users
       testUser1/testUser11 into THIS block's own container. BlockProvisioningProbeRunner
       (glue: stepdefinitions + verification.steps) drives block_probe_provisioning.feature:
       (1) shared-scope beans readable under tenant-domain key — carbon.super userKey1,
       tenant1.com admin + userKey1; (2) CURRENT_TENANT resolves off the shared bean
       exactly as publisher runners do; (3) tenants/users confirmed on the LIVE server by
       parsing the legacy SOAP retrieve steps' responses (retrieved tenants include
       tenant1.com; retrieved users include testUser1 / testUser11). New steps live in
       BlockProbeSteps.java. verify-5.3.sh: Maven SUCCESS, 1 observation / 1 distinct real
       container id, no fv-5.3 leak. Re-runnable. VERIFY 5.3: PASS. -->

- `[-]` _(N/A 2026-06-30 — verifies the legacy provisioning path, intentionally superseded by the lifecycle-driven actor-model provisioning; current provisioning is covered by 7.2/7.3. Suite + script removed 2026-07-01, along with the legacy `SystemInitializationRunner`/`APIMContainer` they drove.)_ **5.4 Verify — legacy parity (regression).** The legacy path
  (`SystemInitializationRunner` + `TenantUserInitializationRunner` + `SystemShutdown`)
  still provisions and runs green, unchanged.
  - **Confirm gate.**
  <!-- 2026-06-23: PASS. testng-fv-5.4.xml runs the UNCHANGED legacy trio in order
       (SystemInitializationRunner -> TenantUserInitializationRunner -> SystemShutdown) with
       NO blockLabel, so BlockLifecycleListener opts out and the legacy fixed-port lifecycle
       drives itself exactly as production testng.xml does. Proves the 5.1/5.2 extraction
       (TenantUserProvisioner + listener-driven provisioning) left the legacy provisioning
       path untouched. verify-5.4.sh: Maven SUCCESS; testng-results passed=5, failed=0,
       skipped=0; and the new-lane observation file is NOT produced (listener no-opped).
       Re-runnable. VERIFY 5.4: PASS. -->

- `[x]` **5.5 Verify — provisioning failure skips the block (B).** Force a provisioning
  failure (e.g. SOAP returns non-200 / unreachable). Because provisioning runs in `onStart`,
  it must set `bootError` and **skip the block cleanly** — not surface mid-scenario as an
  NPE on missing tenants.
  - **Confirm gate.**
  <!-- 2026-06-23: PASS. testng-fv-5.5.xml boots one REAL block (block=fv-5.5,
       initTenantUsers=true, tenantSet=adpsample). adpsample is the pre-migrated profile:
       addAdpsampleTenant only builds a context bean (no SOAP create), so on this FRESH
       (non-migrated) container adpsample.com does not exist; the follow-up addUser SOAP
       authenticates as admin@adpsample.com, gets non-200, and TenantUserProvisioner throws
       - a genuine "SOAP returns non-200" failure, no fault-injection code. The throw lands
       in onStart's catch as bootError (container left for onFinish), and BaseBlockRunner's
       @BeforeClass turns it into a SkipException. verify-5.5.sh: Maven SUCCESS (clean skip
       != failure); testng-results skipped=1, failed=0; Maven log carries the listener's
       "boot/readiness failed" marker (skip is provisioning-driven); NO observation recorded
       (probe never ran); no fv-5.5 leak (onFinish released the post-boot-failed container).
       This also gives the otherwise-dead adpsample branch a verification purpose.
       Re-runnable. VERIFY 5.5: PASS. -->

- `[-]` _(DEPRECATED 2026-06-30 — same pre-actor-model provisioning-shape probe as 5.3 under parallelism; superseded by **7.2 + 7.3**. Per-block isolation itself is green via 6.1. `verify-5.6.sh` is guarded (not run).)_ **5.6 Verify — provisioning targets the block's OWN container under parallelism (B).**
  Two parallel blocks each provisioning (incl. the same tenant domain). Assert each
  provisioner writes to **its own** container's `baseUrl` (mapped port) and that the tenants
  exist in the right container — a wrong-URL bug only manifests under parallel blocks.
  - **Confirm gate.**
  <!-- 2026-06-23: PASS. testng-fv-5.6.xml runs TWO REAL blocks in parallel (suite
       parallel="tests" thread-count="2"), both block=fv-5.6, both initTenantUsers=true with
       the DEFAULT set, so both provision the SAME domains (carbon.super + tenant1.com +
       testUser1/testUser11) concurrently. Each runs BlockProvisioningProbeRunner, which
       SOAP-retrieves tenants/users from ITS OWN baseUrl and asserts they exist there.
       verify-5.6.sh: Maven SUCCESS; 2 observations / 2 distinct real container ids; 2
       DISTINCT baseUrls (per-container mapped ports) - the heart of the gate: each
       provisioner targeted its own container, no shared-URL/scope crosstalk; no fv-5.6 leak.

       FIX during this gate (real race surfaced by parallelism, not a wrong-URL bug): the
       first run failed because ServerReadiness.awaitReady gates only on the GATEWAY health
       check, which can pass seconds before the SOAP admin services (TenantMgtAdminService)
       finish deploying; under two parallel boots sharing host CPU that window widened and one
       block's onStart provisioning hit a transient 404 (expected 200) on retrieveTenants ->
       bootError -> only 1 observation. Added TenantUserProvisioner.awaitTenantMgtServiceReady()
       (polls retrieveTenants until 200) and call it at the start of
       BlockLifecycleListener.provisionTenantUsers, so provisioning never fires before the
       endpoint it needs is live. Re-run: VERIFY 5.6: PASS. (Does not slow 5.5: the super-tenant
       retrieveTenants probe goes ready fast, then the adpsample addUser still fails fast.) -->

- `[-]` _(DEPRECATED 2026-06-30 — idempotency probe asserts pre-actor-model re-provisioning behaviour; superseded by **7.2 + 7.3**. `verify-5.7.sh` is guarded (not run).)_ **5.7 Verify — provisioning idempotency (B).** Exercise the "skip if exists" branch:
  provision, then run the same provisioning again against the same container; it must
  no-op without error / double-create. (Guards reuse and retry scenarios.)
  - **Confirm gate.**
  <!-- 2026-06-23: PASS. testng-fv-5.7.xml boots one REAL block (block=fv-5.7,
       initTenantUsers=true), so the lifecycle provisions the default set once in onStart.
       BlockIdempotencyProbeRunner (block_probe_idempotency.feature) then re-provisions the
       SAME set against the same container via the new step "I provision the default tenant
       set again" (BlockProbeSteps -> TenantUserProvisioner default methods). skip-if-exists
       must no-op: a broken branch would re-create and the server would answer non-200,
       throwing. The probe then asserts tenant1.com present and testUser11 exists EXACTLY
       ONCE (new step "the retrieved users include {string} exactly once"). verify-5.7.sh:
       Maven SUCCESS (re-run no-opped, exactly-once held); 1 observation / 1 distinct real
       container id; no fv-5.7 leak. Re-runnable. VERIFY 5.7: PASS.

       Phase 5 (provisioning consolidation) COMPLETE: 5.1-5.7 all green. Next is Phase 6
       capstone (multi-block parallel provisioning lane). -->

---

## Phase 6 — Capstone: multi-block parallel provisioning lane

**Goal:** prove the whole machine together on a provisioning-only lane (still no
server-flow tests).

- `[x]` **6.1 Assemble the verification suite.** `testng-framework-verification.xml` with
  <!-- Verified 2026-06-23: src/test/resources/testng-framework-verification.xml (3 blocks, all
       block=fv-6.1, suite parallel="tests" thread-count="2" K=2, each <test> parallel="classes"
       thread-count="2" M=2). Phase6.1-BlockA / Phase6.1-BlockB: initTenantUsers=true default set, two
       probe classes each (BlockProbeRunnerOne/Two). Phase6.1-BrokenBlock: tenantSet=adpsample on a fresh
       container -> provisioning fails in onStart -> bootError -> BaseBlockRunner SkipException.
       verify-6.1.sh polls live container count during the run and asserts the full checklist: build
       SUCCEEDS; 4 observations (2x2); 2 distinct container ids + 2 distinct baseUrls each appearing exactly
       twice (per-block isolation, no cross-block override); peak live <=K=2 and reached 2 (cap real +
       parallelism observed); <=M=2 worker threads per block; broken block SKIPPED cleanly (skipped=1,
       failed=0) with the listener's boot-failure marker in the maven log; zero fv-6.1 leaks. Ran PASS. -->

  multiple uniquely-named `<test>` blocks, each: its own `<parameter>` overlay +
  `initTenantUsers`, `BlockLifecycleListener`, probe classes extending `BaseBlockRunner`,
  `parallel=tests` (K=2) and per-block `parallel=classes` (M=2).
  - **Verify (B), the full checklist:**
    - **Multiple containers** boot, one per block, capped at K.
    - **Dynamic ports** distinct per block; no collisions.
    - **Readiness** — every probe sees a ready server before running.
    - **Isolation** — each block's shared vars (container, URLs, tenants) are its own; no
      cross-block override under the `suiteName::testName` namespace.
    - **Skip** — a deliberately-broken block skips cleanly while sibling blocks pass.
    - **Release** — all containers stop and all ports release at the end; zero leaks.
  - **Confirm gate.**

---

## Cross-cutting / robustness verifications

Run these once the capstone (Phase 6) is green — they target nondeterminism and
whole-system safety rather than a single phase.

- `[x]` **C.1 Repeatability / flake soak.** Run the capstone suite **N times** (e.g. 10)
  back-to-back; it must be green every time with **zero leftover containers/ports** between
  runs. Catches port races, scope races, and readiness-timing flakes that a single run hides.
  <!-- Verified 2026-06-24: src/test/scripts/verification/verify-C.1.sh [N] (default 10) loops
       verify-6.1.sh N times, asserting each capstone run exits 0 AND zero fv-6.1 containers remain
       between runs (cross-run leak guard) plus a clean-host pre-flight. Result: 10/10 consecutive
       capstone runs PASS, zero leftover containers between runs - no port/scope/readiness flakes.
       NOTE (feeds C.4): the soak is sensitive to Docker host capacity. Two early N=10 attempts failed
       on run 1 NOT due to any framework defect: (a) host overloaded by a co-located k3s cluster
       (load avg ~13-16) -> Testcontainers image-resolve timed out (2 min) though the image was local;
       (b) the colima VM came back at the default 2 CPU / 1.9 GB after a restart -> one of the two K=2
       APIM servers (each ~1.5-2 GB) could not become ready within 300s and its block skipped (only 2
       observations). Resizing colima to 6 CPU / 11.66 GB fixed it and gave the clean 10/10. So: APIM
       footprint ~=1.5-2 GB/container; K=2 needs a VM with >=~4 GB of server headroom. -->

- `[x]` **C.2 Concurrent shared-scope write stress (A).** Many parallel blocks writing
  shared vars at the same instant; assert no lost updates, no `ConcurrentModification`/
  `ClassCast`, and strict per-block isolation under contention.
  <!-- Verified 2026-06-24: Type-A (no Docker). TestContextConcurrencyStressTest
       (src/test/java/.../utils/) spins up SCOPES=8 x THREADS_PER_SCOPE=4 = 32 worker threads, holds them
       on a CyclicBarrier so they begin writing at the same instant, and hammers
       TestContext.setShared/get/addToList for 16000 writes. Asserts: no lost updates (each thread reads
       back its own writes + a final cross-thread sweep over every scope's full keyset); no CME/CCE
       (interleaved reads + getList cast never throw; any worker throwable fails the test); strict
       per-scope isolation (a per-scope scopeOwner sentinel never bleeds across the suiteName::testName
       namespace; per-thread LOCAL lists hold exactly their own appends). Wired via testng-fv-C.2.xml +
       verify-C.2.sh, which also reads the test's target/fv-c2-stress.txt marker to confirm the stress
       ran at scale (errors=0, totalWrites=16000, sharedScopeCount>=8). Ran PASS first try. -->

- `[x]` **C.3 Suite-abort cleanup.** Interrupt a running parallel suite (Ctrl-C / kill);
  document and verify what containers/ports remain (relevant with Ryuk disabled), and that
  a follow-up run still succeeds (no stuck ports). Add a JVM shutdown hook if leaks are
  unacceptable.
  <!-- Verified 2026-06-24: testng-fv-C.3.xml launches a genuinely parallel K=2 suite (2 blocks,
       label block=fv-c.3, no tenant provisioning), and verify-C.3.sh waits until both block containers
       are RUNNING, captures their published host ports, then SIGKILLs the Maven/surefire test JVM
       (untrappable => bypasses BlockLifecycleListener.onFinish AND any JVM shutdown hook), simulating an
       OOM-kill/crashed-agent abort. DOCUMENTED FINDING: with Testcontainers reuse enabled, Ryuk is
       disabled, so both containers LEAK (stay running) on abrupt kill - this is the expected behavior of
       the current config, so the verify reports it rather than failing. The verify then force-removes the
       leftovers and asserts every captured host port frees at the OS level (no stuck ports - the Phase 1.4
       primitive: a killed container releases its dynamic host port; only the exited record lingers), then
       runs the SAME suite synchronously and asserts it succeeds with fresh observations, proving an abort
       never wedges a follow-up. Ran PASS first try: leaked 2/2 on SIGKILL, all 8 captured ports released,
       follow-up green with 2 observations, clean final state.
       OPEN DECISION (deferred to user): a JVM shutdown hook in BlockLifecycleListener would reap leaks on
       trappable signals (Ctrl-C/SIGTERM) but NOT on SIGKILL/OOM; only re-enabling Ryuk covers SIGKILL.
       Since dynamic ports mean a leak never blocks a follow-up run (only consumes RAM until pruned), this
       was left as a config/policy choice rather than added unilaterally. -->
  - **Decision needed:** leaks on abrupt kill are real but harmless to follow-up runs (dynamic ports);
    a shutdown hook only helps trappable signals, Ryuk-on is the only SIGKILL-safe fix. Pending user call.
- `[x]` **C.4 Host-capacity sanity.** Confirm the chosen K (suite `thread-count`) fits the
  CI/dev Docker host (CPU/RAM/file-descriptors) without OOM or boot-timeout cascades; record
  the safe K and the observed per-container footprint.
  <!-- Verified 2026-06-24: verify-C.4.sh runs the real capstone (testng-framework-verification.xml) at the
       chosen K=2 and, while it runs, samples each live block container's memory via `docker stats`, tracking
       peak per-container and peak aggregate. Asserts: capstone succeeds with all 4 observations (no OOM /
       boot-timeout cascade at K=2 on this host); peak live containers reached K=2 (capacity actually
       exercised); a real per-container footprint was measured; peak aggregate stayed within an 80% memory
       budget; safeMaxK (= 80%-budget / per-container) >= chosen K; fd soft limit unlimited or >= 1024.
       Records target/fv-c4-capacity.txt. MEASURED on the 6 CPU / 11934 MiB colima VM: ~1525 MiB/container
       (confirms C.1's 1.5-2 GB estimate, lower end), peak aggregate 2946 MiB = 24% of host, safeMaxK=6,
       fd=unlimited. So K=2 fits with large margin; this host could run up to ~6 concurrent APIM containers
       within the 80% budget. Ran PASS first try. NOTE: safeMaxK is memory-bound only; CPU (6 cores) and
       APIM boot-time contention are the practical ceiling well before K=6 - K=2 remains the recommended
       default (see C.1 for the boot-timeout failure mode on an undersized VM). -->

## Phase 7 — Test-authoring framework features (added during feature porting)

Phases 1–6 verified the **concurrency/lifecycle core** (container, scope key, lint, block lifecycle,
provisioning, capstone). The mechanisms below were added **afterwards**, while porting the legacy suite, as new
requirements surfaced. They are now load-bearing framework features with **no dedicated regression test**, so a
refactor can silently break them — e.g. the actor-model and `basic`-as-overlay design changes already regressed
several Phase 1–4 probes (2026-06-30 fv sweep). Each item below gets a `verify-7.x.sh` → `testng-fv-7.x.xml`
(+ a `*VerificationTest` or probe feature), same convention as above. (A) = in-JVM, (B) = boots a container.

**Status (2026-06-30): all complete.** 7.1 and 7.9 are standalone Type-A (`verify-7.1.sh`, `verify-7.9.sh`);
7.5 is verified by the `key-manager/token-persistence` feature. To keep container boots low, **7.2/7.3/7.4/7.7/7.8
are jointly verified in one block boot** by `verify-7.2.sh` → `testng-fv-7.2.xml` (the `IntegrationV2-FrameworkFeatures`
block: `FrameworkFeaturesProbeRunner` covers tenancy/actor/gateway/overlay, `FrameworkFeaturesHandoffRunner`
covers setup-handoff). 7.6 is by-reference (see its note). All green.

- `[x]` **7.1 Context sharing & scoping (A).** `TestContext` local (runner) vs shared (block) scope: values a
  setup step stores are visible to later steps/scenarios on the SAME runner instance (incl. Scenario-Outline
  rows) and on worker threads; `${UNIQUE:...}` yields collision-free names; `{{key}}` placeholders resolve from
  context; one runner's local scope is NOT visible to another. (CLAUDE.md §4) _(Verified 2026-06-30 via
  `verify-7.1.sh` → `testng-fv-7.1.xml` + `TestContextScopingVerificationTest`.)_
- `[x]` **7.2 Tenancy — provisioning + routing (B).** Boot a block with `initTenantUsers=true`: assert
  `tenant1.com` and its users are provisioned, a Tenant bean is in context, and a resource created as a tenant
  actor is addressed at the `/t/<tenant>` context (actor `@domain` → tenant routing). (CLAUDE.md §12)
- `[x]` **7.3 Actor / Identity model + auth keys (B).** Default actors (admin / publisherUser / subscriberUser)
  provisioned in BOTH tenants; each auth composite mints the right token set (publisher / devportal / admin) for
  the named actor; `I act as` / `setActingActor` switches the no-arg token resolution; a least-privilege actor is
  denied an admin-scoped op (401). (CLAUDE.md §12)
- `[x]` **7.4 Multi-feature runner + setup-fixture handoff (B).** A runner with `features={_setup, consumer}`
  (ordered): the setup feature creates a resource into runner-local scope; the consumer reads its id+payload and
  succeeds; the setup resource survives across the consumer's scenarios (untagged `@cleanup`); the `@AfterClass`
  sweep tears it down once. (CLAUDE.md §10)
- `[x]` **7.5 Server restart — in-product graceful (B).** With the `enable_restart_from_api` + token-persistence
  overlay, the `I gracefully restart the API Manager server` step bounces the carbon JVM (ServerAdmin
  `restartGracefully`), `ServerReadiness.awaitRestart` blocks down→up, the SAME container/ports/DB survive, and a
  token issued before the restart still validates after. _(Verified 2026-06-30 — the
  `key-manager/token-persistence` feature IS the probe: a token still validates (200) after a graceful restart
  and a revoked token stays revoked (401) across a second restart; green in the `IntegrationV2-ServerRestart`
  block.)_
- `[x]` **7.6 Cleanup model (B).** Both teardown granularities: the `@After("@cleanup")` hook deletes a
  scenario's registered resources, and `BaseBlockRunner` `@AfterClass` sweeps the `CREATED_*` lists; cleanup is
  idempotent (already-deleted → ignored), runs on failure, and is a **no-op when nothing was registered**
  (actor-less block — regression fixed 2026-06-30). Leaves zero residue. (CLAUDE.md §5) _(Verified by-reference
  2026-06-30 — exercised by every `@cleanup` product feature (residue would surface as 409s on the shared
  container) plus the no-op-when-empty fix, which is itself exercised by the actor-less framework-verification
  probe blocks. No standalone probe.)_
- `[x]` **7.7 Gateway invocation wiring (B).** With `initBackend=true` the NodeAppServer backend is reachable at
  alias `nodebackend`; a deployed API is invocable at its full gateway context with no double `/t/` prefix; the
  `until response status code becomes N within S seconds` retry floors at `DEPLOYMENT_WAIT_TIME`. (CLAUDE.md §11)
- `[x]` **7.8 TOML config overlays (B).** `tomlExtraOverlayPath`: base + `basic` + extra are merged (extra keys
  present AND distribution/basic defaults retained); two orthogonal overlays co-exist in one block; the full-file
  `tomlOverlayPath` still replaces verbatim. Guards `Utils.mergeTomls` / `resolveDefaultToml` /
  `resolveTomlContent`. (CLAUDE.md §13)
- `[x]` **7.9 Capability taxonomy lint (A).** `render_coverage_tree.py` accepts the closed `@cap`/`@feat` vocab
  from `capability-map.yml`, rejects an unknown tag (non-zero `invalid`), enforces the `@setup`↔`_setup_*`
  filename rule bidirectionally, and excludes non-product markers — run against a fixture with a deliberately bad
  tag. (CLAUDE.md §3) _(Verified 2026-06-30 via `verify-7.9.sh` — temp-fixture lint: valid→`invalid: 0`/exit 0,
  unknown @cap and `_setup_`-without-@setup→non-zero exit.)_

## Out of scope / later

- Migrating real server-flow runners onto `BaseBlockRunner` and the Tier-1/2/3 lane split.
- Auto-prefix resource naming + prefix-scoped queries (Data-isolation, the *test-author*
  half) — needed when real tests move to the shared-classes lane.
- Shared external DB (MySQL/Postgres) support and real per-container schema isolation.
- Reverting the temporary `pom.xml`/`testng.xml` single-test harness edits and switching
  the production suites over to the new lane.
- **Container leak on abrupt suite abort (SIGKILL/OOM).** Edge case surfaced by C.3: when the
  test JVM dies on an untrappable signal (kill -9, OOM-killer, crashed CI agent), the running
  block containers are NOT reaped — Testcontainers `reuse` is enabled, which disables the Ryuk
  reaper, and `BlockLifecycleListener.onFinish` never runs. Verified harmless to correctness:
  dynamic host ports mean a leaked container never collides with a follow-up run (only consumes
  RAM until pruned), so C.3's follow-up run passes cleanly. Proposed solution, by coverage:
    - *Trappable signals only (Ctrl-C/SIGTERM):* add a JVM shutdown hook that stops any
      live block containers. Cheap and additive, but does nothing for SIGKILL/OOM.
    - *Full coverage incl. SIGKILL/OOM:* re-enable Ryuk (drop `testcontainers.reuse.enable`),
      accepting the loss of container reuse and a small startup cost per container.
    - *Operational stopgap (no code):* a pre-run `docker rm -f` sweep by `block=` label (the
      verify scripts already do this) clears any leftovers from a prior aborted run.
  Recommended: keep reuse + the label sweep for CI cleanliness; add the shutdown hook only if
  interactive Ctrl-C leaks become a developer nuisance. Re-enable Ryuk only if RAM exhaustion
  from leaked containers is observed on the shared host.
- **Shared `NodeAppServer` backend under parallelism.** The API backend (`NodeAppServer`) is the
  original (2025) model and was left untouched by the parallel-on-shared-container work. It is a
  per-JVM singleton (`InstanceHolder`) joined to the static `ContainerNetwork.SHARED_NETWORK` under
  alias `nodebackend`; every `DynamicApimContainer` joins the same network, so all APIM nodes —
  however many run concurrently — proxy to the *one* shared backend by Docker DNS alias (fixed
  container ports 3000–3017, no host-port contention). This sharing is fine for stateless
  request/response, but `NodeAppServer.restart()` stops+starts the shared container, is not
  synchronized, and is called by `MigratedSharedScopesRunner` and `APIPoliciesRunner`. Harmless in
  the sequential legacy lane, but if either runner is ever composed into a block that runs in
  parallel with another block (Tier 4 / capstone, K>1), the restart will break the other block's
  in-flight backend calls. Later, when real runners move onto the shared-classes lane: either give
  each block its own backend, isolate the two restart-callers to a serialized block, or make
  `restart()` a no-op/guarded under parallel execution.
- **`deepMerge` cannot delete keys.** The block toml is now expressed as a small *overlay*
  merged (`Utils.mergeToml` → `deepMerge`) onto the product distribution's
  `deployment.toml` (the base shipped inside the image). `deepMerge` recurses into nested
  tables and otherwise adds-or-overrides; it has no way to *remove* a key the base defines.
  So any distribution-only block the test config would have dropped is silently inherited.
  Two such blocks are intentionally tolerated rather than worked around:
    - `[[oauth.extensions.token_types]]` (the JWT issuer entry), and
    - `[apim.analytics] type = "moesif"` (the overlay re-asserts `enable = false` and adds
      `auth_token = ""`, but cannot strip the inherited `type`).
  Both are harmless to the current suites. If a future test genuinely needs a key removed,
  extend the merge with a delete directive (e.g. a sentinel/null convention) rather than
  reverting to a full-file toml copy.
