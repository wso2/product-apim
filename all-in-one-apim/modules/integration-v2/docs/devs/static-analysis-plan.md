# V2 Static-Analysis Rule Catalog — DRAFT for validation

Purpose: turn the coding rules we enforce *by review today* (CLAUDE.md) plus the bad practices reviewers
actually flagged on PRs **14235 / 14240 / 14241 / 14247 / 14248 / 14250 / 14254 / 14262 / 14264 / 14268**
into **automated gates** (Checkstyle + PMD for Java, a small feature-linter for Gherkin), so the same smells
fail the build instead of relying on a reviewer catching them each time.

This is the **merge** of two sources — CLAUDE.md is the distilled/validated backbone; the PR harvest surfaces
the gaps that never made it into CLAUDE.md. Nothing is silently dropped: every rule is Tier 1/2/3, and every
Tier-3 (can't-be-statically-checked) call is listed in §5 for you to red-pen.

Source tags: **[C§n]** = CLAUDE.md section n · **[P]** = PR review feedback (catalog in
`/tmp/pr-feedback-catalog.md`).

## How to read the tiers
- **Tier 1 — built-in rule.** A stock Checkstyle module or PMD rule. Cheap, high-confidence.
- **Tier 2 — custom rule.** A Checkstyle `Regexp*` or PMD XPath rule we author to encode a V2-specific
  convention. This is where most of CLAUDE.md's value lives.
- **Tier 3 — not statically checkable.** Stays a CLAUDE.md/review rule. Listed with *why* + any partial proxy.

## Escape hatch (deliberate, in-the-open violations)
- Checkstyle: `SuppressionCommentFilter` → `// CHECKSTYLE:OFF <Rule> — <reason>` … `// CHECKSTYLE:ON`.
- PMD: `@SuppressWarnings("PMD.<Rule>")` on the element, or `// NOPMD - <reason>` on the line.
Every suppression must carry a reason. This is the "violate only when necessary" model.

## Rollout (agreed)
Ratchet: install all gates at **`warning`** first, fix only *our recently-written* code, then a **separate PR**
does the full-tree sweep and flips the **entire ruleset** to `failOnViolation` **all-or-nothing** (not per-rule)
— once the whole tree is clean the gate goes hard for everything at once. Scope: **all of `integration-v2`**
(tests-common/testcontainers, integration-test-utils, tests-integration/cucumber-tests).

---

## 0. Implementation status (as built — warning mode)

Wired into the parent pom (`failOnViolation=false`), config under `build-config/`, applied to all `integration-v2`
modules (main + test sources). Escape hatches verified: `@SuppressWarnings("checkstyle:…")`/`("PMD.…")` and
`// CHECKSTYLE:OFF <id>` (Checker-level Regexp via `SuppressWithPlainTextCommentFilter` + `idFormat`).

**Checkstyle** — implemented: `NewlineAtEndOfFile`, trailing-whitespace, `System.out/err` ban (id `systemOut`),
`Thread.sleep` ban (id `threadSleep`), hand-rolled-deadline-loop ban (id `deadlineLoop`, RegexpMultiline),
`UnusedImports`, `RedundantImport`, `MissingSwitchDefault`, `IllegalCatch` (Exception/Throwable — RuntimeException
intentionally excluded pending calibration). Header check (T1-5) still TODO (needs the header regex calibrated to
the codebase's variants).

**PMD** — 4 built-in (`EmptyCatchBlock`, `UnusedLocalVariable`, `UnusedAssignment`, `CloseResource`) + **16 custom
XPath rules, each fixture-validated** (fires on a known-bad case, exempts the good variants). Tier-2 (8): T2-3
InterruptedException-restore/rethrow, T2-5 redundant-httpResponse-remove, T2-6 guard-before-JSON-parse, T2-7
escapeXml, T2-8 urlEncode, T2-9 multiline-XML-DOTALL, T2-13 deregister-only-after-2xx-delete, T2-14
async-needs-executor. **Group-B promotions (8)**: D-7 NonIdempotentPostInRetryFunnel, D-8 ResolveCallerKeyNotRawGet,
D-12 MutableSharedStaticState, D-13 CheckContainerExecResult, D-14 NoSecretsInLogs, D-19
MatchByFieldNotWholeObjectToString, D-2 AssertRetryResult, D-5 RegisterCreatedResourceForCleanup, D-9
PollLoopMustShortCircuitOn4xx (best-effort — all fire on fixtures, 0 real-tree FP).

**SpotBugs + FindSecBugs** — narrowed include filter (security + resource-leak patterns; T1-9 `OS_OPEN_STREAM*`
fixture-proven to fire on a leak). D-10 (concurrency) / D-12 (mutable static) were trialled here but the SpotBugs
`MS_*`/`MT_` detectors **fixture-proven not to fire** in this setup — D-12 moved to the PMD rule above, D-10 stays
review-only.

**Gherkin feature-lint** — hard: G-1…G-4, G-9, G-10. Advisory (report-only, non-gating): G-5, G-8, **G-11
(D-1 duplicate scenarios — fixture-validated, 0 real), G-12 (D-11 hardcoded `_setup_` tenant/credential literals —
8 real, mostly true positives)**.

**Verification pass — every rule fixture-tested to actually FIRE (guards against "0 = silently broken", not "0 =
clean").** Found and fixed two false-greens: (1) Gherkin **G-10** never fired (parser routed prose-`@` lines into
the tag bucket) — fixed to flag any `@`-line with a non-`@` token; (2) PMD `UnusedLocalVariable` under-covers the
*assigned-then-never-read* case (the actual T1-7 smell) — added `UnusedAssignment`. All others confirmed firing:
Checkstyle `NewlineAtEndOfFile`/`RedundantImport`/`MissingSwitchDefault`/`IllegalCatch`/`UnusedImports`/regex-bans;
PMD `EmptyCatchBlock`/`CloseResource` + the 8 custom rules; Gherkin G-1…G-4/G-9/G-10. All four suppression mechanisms
verified against a fixture: Checkstyle `@SuppressWarnings("checkstyle:…")` + `// CHECKSTYLE:OFF <id>` (idFormat),
and PMD `@SuppressWarnings("PMD.…")` + `// NOPMD` (both suppress; the un-suppressed control still fires).

**Current violation counts** (whole tree; ratchet debt for the sweep PR): Checkstyle **132** (61 IllegalCatch, 30
Thread.sleep, 18 System.out, 35 deadline-loop, 7 unused-import, 6 trailing-ws — minus the ~25 already fixed in our
newly-written code), PMD **2** (1 `CloseResource`, 1 `UrlEncodeDynamicFormFieldValue`). **Our newly-written code is
clean.**

**Known FP:** T2-8 (urlEncode) fires on `JacocoCoverage.java:97` (a JVM `-javaagent:…port=" + TCP_PORT` string, not a
URL field) — a `key=value`-heuristic false positive; suppress-with-reason in the sweep. T2-6 uses a lenient
method-scoped `getResponseCode` guard (low-FP, some-FN by design).

**Demoted Tier-2 → Tier-3 (review-only), with reason:** T2-11 (provisioners/listeners must not do product ops) —
the infra-vs-product distinction is semantic: `SsoProvisioner`/`TokenExchangeProvisioner` SOAP admin (sanctioned
§14 exception) and `BlockLifecycleListener` readiness `SimpleHTTPClient` calls would all false-positive; a static
"HTTP-client-in-a-Provisioner/Listener" rule can't separate the smell from the allowed pattern.

**Gherkin feature-linter** — `docs/devs/lint_features.py` (reads `capability-map.yml`). Implemented + **passing on
all 130 features**: G-1 (tag vocabulary + exactly-one @cap/@feat), G-2 (@cap vs folder, @dep exception), G-3 (unique
Feature titles), G-4 (`_setup_`⇄`@setup`, no @cleanup on setup), G-9 (trailing-ws / EOF newline), G-10 (no
prose-as-tag). **Wired as a HARD gate** (exec-maven-plugin in cucumber-tests at `validate`, fails on violation) —
the tree is clean so no ratchet needed. First run caught one real gap (this was the first time @cap/@feat were ever
validated against the map — the CLAUDE.md §3 "fails lint" never actually existed): `external_idp_console_sso.feature`
used `@feat:sso` which wasn't registered under `@cap:admin`; fixed by adding `admin/sso` to `capability-map.yml`.
**Heuristic Gherkin rules G-5/G-6/G-8 — built, fixture-validated to fire, real-FP measured, wired as ADVISORY**
(report-only via `--all`; NEVER gate the build — the hard gate stays on the clean G-1…G-4/G-9/G-10). Measured
counts + verdicts (setup/non-product scenarios excluded):
  - **G-5** Background-dedup — 25 hits, **mostly true positives** (real "N scenarios share the first 3 steps →
    hoist to a Background" candidates). Keep as advisory.
  - **G-6** one-behaviour-per-scenario — measured **326 hits, overwhelmingly false positives** (the
    When/status-count heuristic cannot distinguish a cohesive multi-step arc — a CRUD/lifecycle Scenario Outline,
    an SSO journey — from genuinely-bundled independent behaviours). **DEMOTED to review-only (Tier-3), removed
    from the linter** — same class as T2-11: the smell is semantic, not structural. Stays a CLAUDE.md §6 rule.
  - **G-8** deploy-needs-status — 49 hits, **mixed**: real REST-deploy-without-status smells among false positives
    on self-asserting/browser/composite deploy steps (e.g. the SSO SPA `I redeploy the API via the publisher UI`).
    Keep as advisory.
G-7 assert-the-effect remains review-only (semantic).

**SpotBugs + FindSecBugs** — wired into the parent pom (report-only `spotbugs` goal — ratchet; the sweep PR
switches to `check` to gate). Analyses BYTECODE, so runs after compile with `includeTests=true`. Scope narrowed
via `build-config/spotbugs-include.xml` to the plan's high-signal patterns (T1-13 trust-all/TLS/hostname, T1-12
Zip-Slip write-side, T1-9 resource-leak, + XXE and weak-crypto) — the full SECURITY category floods a test module
with noise (CRLF-in-logs on every logger call, HARD_CODE_PASSWORD on test creds, UNENCRYPTED_SOCKET on the local
CONNECT proxy). Verified it catches real issues (not a silently-empty filter): **13 pre-existing findings** —
`WEAK_TRUST_MANAGER`×9 (trust-all on self-signed test containers → confined suppressions in the sweep),
`XXE_*`×3, `WEAK_MESSAGE_DIGEST_SHA1`×1. **Our newly-written code is clean.** *Gap:* T1-14 temp-file-permissions
has no FindSecBugs detector (that was a semgrep finding on the PRs) and you run no semgrep — left as a
review-only item; add a small PMD custom rule or semgrep later if wanted.

**Not yet built:** the full-tree Java sweep + all-or-nothing flip to `failOnViolation` / `check` (separate PR,
§7.2 — DECIDED all-or-nothing).

## 1. Tooling

| Tool | Covers | Notes |
|------|--------|-------|
| **maven-checkstyle-plugin** | header, formatting, imports, custom `Regexp*` rules | greenfield — no config to inherit |
| **maven-pmd-plugin** (+ **CPD**) | best-practices/error-prone/design rules, custom XPath, copy-paste | CPD is the "no duplicate helper/step" signal |
| **feature-linter** (small custom script, e.g. Python under `docs/devs/`) | Gherkin `.feature` rules | Checkstyle/PMD can't parse Gherkin; the `@cap` "fails lint" in [C§3] isn't actually implemented yet |
| **SpotBugs + FindSecBugs** *(decision needed — §7)* | Zip-Slip, weak TLS, trust-all, temp-file perms, unclosed resources | the PR security findings are SpotBugs/SAST territory, not Checkstyle/PMD. If your CI already runs a SAST (temp-file-permissions was flagged by one), we route these there instead of adding a 3rd tool |

CI-config hygiene from [P] (actionlint `permissions:`/pinned refs, Gitleaks committed keys, Codecov
fail-flag, coverage denominator) is **out of scope for this effort** — different tools (actionlint / Gitleaks /
semgrep) on the workflow files, tracked separately. Flagged so it isn't lost.

---

## 2. Tier 1 — built-in rules (the easy wins)

| # | Rule | Tool / module | Source |
|---|------|---------------|--------|
| T1-1 | No `Thread.sleep` for sync | PMD `DoNotUseThreads` is too broad → use custom (see T2-1); Tier-1 fallback: PMD `AvoidThreadGroup`+ | [C§4][P] |
| T1-2 | Retry/poll loops must not catch bare `Exception`/`Throwable`/`RuntimeException` | Checkstyle `IllegalCatch` (illegalClassNames = Exception, Throwable, RuntimeException) · PMD `AvoidCatchingGenericException` | [C§7,§15][P] |
| T1-3 | No empty `catch` blocks | PMD `EmptyCatchBlock` (allowCommentedBlocks=false) · Checkstyle `EmptyCatchBlock` | [P] anti-pattern |
| T1-4 | `switch` must have `default` | Checkstyle `MissingSwitchDefault` · PMD `SwitchStmtsShouldHaveDefault` | [P] (gap — not in CLAUDE.md) |
| T1-5 | Full standard license header on every `.java` | Checkstyle `Header`/`RegexpHeader` | [C§9][P] |
| T1-6 | No unused imports / no redundant imports | Checkstyle `UnusedImports`, `RedundantImport` | [C§8][P] |
| T1-7 | No unused local variables / dead assignments | PMD `UnusedLocalVariable`, `UnusedAssignment` | [P] (`HttpResponse r = Requests.x()` never read) |
| T1-8 | No `System.out`/`System.err` in framework code | PMD `SystemPrintln` · Checkstyle `Regexp` | [P] (gap) — **my `PlaywrightSsoClient` violates this** |
| T1-9 | Unclosed resources on failure path | PMD `CloseResource` · prefer try-with-resources | [P] (gap) |
| T1-10 | Repeated string literals → constant | PMD `AvoidDuplicateLiterals` (e.g. `"httpResponse"`) | [C§7][P] |
| T1-11 | No FQN where the type is imported | Checkstyle `IllegalType`/custom `RegexpSinglelineJava` | [P] (gap) |
| T1-12 | Zip-Slip on archive extraction | SpotBugs-FindSecBugs `ZIP_SLIP` / PMD heuristic | [P] (gap, security) |
| T1-13 | SSL context ≥ TLSv1.2; trust-all/noop-verifier suppressed to test scope | FindSecBugs `SSL_CONTEXT`/`WEAK_TRUST_MANAGER`/`WEAK_HOSTNAME_VERIFIER` | [P] (gap, security) |
| T1-14 | Temp files owner-only | FindSecBugs `TEMPFILE_INSECURE` / PMD | [P] (gap, security) |
| T1-15 | Compiler `source/target` ↔ properties ↔ JDK agree; no stale dep versions | `maven-enforcer-plugin` (`requireJavaVersion`, `requireProperty`, dependency-convergence) | [P] (gap, build) |
| T1-16 | Newline at EOF | Checkstyle `NewlineAtEndOfFile` (+ editorconfig) | [P] |
| T1-17 | No trailing whitespace | Checkstyle `RegexpSingleline` `\s+$` | [P] |

---

## 3. Tier 2 — custom rules (V2/CLAUDE.md-specific — the real value)

| # | Rule | Mechanism (sketch) | Source |
|---|------|--------------------|--------|
| T2-1 | Ban `Thread.sleep(` in step/util classes | PMD XPath `//MethodCall[@MethodName='sleep'][TypeExpression//ClassOrInterfaceType[pmd-java:typeIs('java.lang.Thread')]]` · or Checkstyle `RegexpSinglelineJava` `Thread\.sleep\(` scoped to `stepdefinitions`/`utils` | [C§4][P] |
| T2-2 | No hand-rolled deadline/retry loop — funnel through `Utils.retryUntil`/`awaitWithRetry` | Checkstyle `RegexpMultiline` flag `while\s*\(\s*System\.currentTimeMillis\(\)` outside `Utils` | [C§7,§15][P] |
| T2-3 | `InterruptedException` catch must restore flag or rethrow | PMD XPath: `CatchClause` for `InterruptedException` whose body has no `MethodCall[@MethodName='interrupt']` and no `ThrowStatement` | [P] |
| T2-4 | Don't hand-set `httpResponse` outside the funnel | PMD XPath: `MethodCall[@MethodName='set'][ArgumentList/StringLiteral[@Image='"httpResponse"']]` in a class not named `Requests`/`APIInvocationSteps` | [C§7][P] |
| T2-5 | No redundant `TestContext.remove("httpResponse")` before a `Requests.*` call | PMD XPath: `remove("httpResponse")` immediately followed by a `Requests.` call | [P] |
| T2-6 | Guard response before parsing body | PMD XPath: `ConstructorCall` of `JSONObject`/`JSONArray` (or `.getData().contains(`) whose arg derives from `getData()` with no preceding non-null + `2xx` + `isBlank()` guard in-block. Secondary: prefer `isBlank()` over `isEmpty()` on bodies | [C§7][P] |
| T2-7 | XML-escape dynamic values in hand-built SOAP/XML bodies | PMD XPath: `+` concatenation producing a `<...>` string where an operand isn't wrapped in `escapeXml(` | [C§15][P] |
| T2-8 | URL-encode form/query field values | PMD XPath: `"...=" +` form/query concat where operand isn't `urlEncode(` | [C§15][P] |
| T2-9 | `Pattern`/`replaceAll` over multi-line XML needs `DOTALL`/`(?s)` | PMD XPath on regex literal containing `.*?` spanning `<...>` without `(?s)`/`Pattern.DOTALL` | [P] |
| T2-10 | No duplicated helper/URL/retry/header-map code — reuse shared glue | **PMD-CPD** (token threshold ~50) · secondary PMD `ExcessiveMethodLength` | [C§7,§15][P] |
| T2-11 | Provisioners/listeners must not do product ops | PMD XPath: reference to `SimpleHTTPClient`/`Requests.` inside a class whose name matches `*Provisioner`/package `*listeners*` (allowlist infra endpoints) | [C§14][P] |
| T2-12 | No `InheritableThreadLocal` for per-task isolation | PMD XPath `//FieldDeclaration//ClassOrInterfaceType[@SimpleName='InheritableThreadLocal']` | [C§4][P] |
| T2-13 | Deregister-from-cleanup only after a 2xx/404 delete | PMD XPath: `ResourceCleanup.deregister(` not guarded by a status check on the preceding `Requests.delete` | [C§5][P] |
| T2-14 | Blocking async work needs an explicit `Executor` (not common pool) | PMD XPath: `CompletableFuture.supplyAsync`/`runAsync` with a single argument | [P] |
| T2-15 | One Javadoc block per declaration (no stale/duplicate) | Checkstyle custom `RegexpMultiline` for two consecutive `*/` before one declaration | [C§6][P] |
| T2-16 | `_setup_*` filename ⇄ `@setup` tag both-directions | feature-linter (see §4) — but the file-naming half is a filename check | [C§10] |

---

## 4. Gherkin feature-linter rules

Small custom linter over `.feature` files (Checkstyle/PMD can't parse Gherkin). Reads the closed tag
vocabulary from `docs/devs/capability-map.yml` (the `@cap`/`@feat` source of truth).

| # | Rule | Source |
|---|------|--------|
| G-1 | `@cap`/`@feat` values ∈ closed vocabulary (capability-map.yml); exactly one `@cap` + one `@feat` | [C§3] |
| G-2 | Scenario's `@cap` + `Feature:` title prefix match the parent folder, unless a `@dep:` is present | [C§2][P] |
| G-3 | Unique `Feature:` titles across the module (no two files share a title) | [C§6] |
| G-4 | Setup features: `_setup_*` filename ⇄ `@setup` tag (both directions); `@setup` feature has no `@cleanup` | [C§10] |
| G-5 | Shared leading step-sequence across N+ scenarios → require a `Background` | [C§6][P] |
| G-6 | One behavior per scenario — flag multiple independent `When→Then` cycles / repeated `Then status` blocks | [C§6][P] |
| G-7 | A state-changing step (`create/update/delete/deploy/clear`) followed by a re-read must have a **content** assertion, not just `status code should be 200` | [C§12][P] |
| G-8 | Every `When ... deploy/state-change` step is followed by a `Then ... status code` | [P] |
| G-9 | No leading whitespace on tag lines; no trailing whitespace in string values; single EOF newline | [P] |
| G-10 | No description/prose line starting with `@` (parsed as a tag → lint break) | [C§6] |

---

## 5. Tier-3 review-only calls — RESOLVED

Original claim: these "cannot be reliably enforced by static analysis." After your **"all Group-B → enforce, warn
now / error in the sweep"** directive we re-attacked every dismissal that had a partial proxy (Group B). Outcome:
**11 of 12 promoted to enforced rules** (warn-mode now, flip to error in the sweep PR), each **fixture-validated to
fire** and **measured for real-tree false positives**. D-10 is the sole Group-B item that stayed review-only — its
tool (SpotBugs) was **fixture-proven not to fire** here. The pure-semantic dismissals (Group A) remain review-only.

### 5a. Promoted → enforced (warn now → error in sweep)

| # | Rule | Enforced as | Fixture | Real FP | Source |
|---|------|-------------|:---:|:---:|--------|
| D-1 | No duplicate scenario | feature-lint **G-11** (identical normalized step-sequence, cross-module) | ✅ | 0 | [C§1] |
| D-2 | Assert the retry result | PMD **AssertRetryResult** (bare `retryUntil(...)` statement — result discarded) | ✅ | 0 | [C§7][P] |
| D-3 | Route asserted requests through the funnel | Checkstyle **T2-4** (hand-set `httpResponse`) | ✅ | 0 | [C§7][P] |
| D-5 | Register every created resource | PMD **RegisterCreatedResourceForCleanup** (create call in a method with no `ResourceCleanup`) — *low recall: most creation goes through provisioners* | ✅ | 0 | [C§5][P] |
| D-7 | No blind retry of a non-idempotent POST | PMD **NonIdempotentPostInRetryFunnel** (`post`/`patch` inside `retryUntil`/`awaitWithRetry`) | ✅ | 0 | [C§15][P] |
| D-8 | Resolve caller-supplied keys (not raw `get`) | PMD **ResolveCallerKeyNotRawGet** (`TestContext.get(variable)`, exempts literals) | ✅ | 0 | [C§7][P] |
| D-9 | Break retry early on 4xx | PMD **PollLoopMustShortCircuitOn4xx** (`==200` loop, no 4xx) — *largely moot; deadline loops banned by T2-2* | ✅ | 0 | [P] |
| D-11 | No hardcoded user/tenant in `_setup_` | feature-lint **G-12** (hardcoded tenant/credential literal in `_setup_*`) | ✅ | 8 (mostly TP) | [C§4][P] |
| D-12 | No shared mutable static | PMD **MutableSharedStaticState** (non-final `static` field) | ✅ | 0 | [C§4] |
| D-13 | Don't discard a container exit code | PMD **CheckContainerExecResult** (bare `execInContainer(...)` — result discarded) | ✅ | 0 | [P] |
| D-14 | No secrets in logs | PMD **NoSecretsInLogs** (log call referencing a `password`/`secret`/`privateKey` identifier) | ✅ | 0 | [P] |
| D-19 | Match by field, not whole-object `.toString().contains` | PMD **MatchByFieldNotWholeObjectToString** | ✅ | 0 | [P] |

*D-13's dataflow half (env var → `.replace`/`new File` with no null-check) stays review-only; the discarded-exit-code
half is the enforced part. D-11's `Names.unique` half is review-only; the `_setup_` literal half is enforced.*

### 5b. Not statically enforceable — stays review-only

| # | Rule | Why it resisted enforcement | Source |
|---|------|------------------------------|--------|
| D-4 | Absence assertions must guard success first (no vacuous pass) | Assertion polarity/intent — no structural signal | [C§12][P] |
| D-6 | Teardown idempotent + reset all mutated server state | Pair each mutation with its undo across files | [C§5][P] |
| **D-10** | Concurrency: capture-once, sync singleton restart, race-safe lists | Data-race correctness — **SpotBugs MS_*/MT_ detectors fixture-proven not to fire in this setup** (a `public static` mutable array/collection produced 0 findings at effort=Max/threshold=Low) | [C§4][P] |
| D-15 | Docs/log/remediation text matches actual behavior | Pure semantic | [P] |
| D-16 | `tomlExtraOverlayPath` (not `tomlOverlayPath`); overlay only non-default keys | Config-intent semantic | [C§13] |
| D-17 | Gateway-invocation wiring (initBackend, tenant context, async variants) | Semantic setup correctness | [C§11] |
| D-18 | Least-privilege actor; set acting actor explicitly | Requires knowing the flow's privilege needs | [C§12] |

---

## 6. CLAUDE.md coverage cross-check (nothing dropped silently)

| CLAUDE.md § | Enforced by |
|-------------|-------------|
| §1 search before write | D-1 (review) |
| §2 folder/@cap placement | G-2 |
| §3 tags closed vocabulary | G-1 |
| §4 isolation (no sleep / unique names / no global state) | T2-1, T2-12, D-10, D-11, D-12 |
| §5 cleanup (register, idempotent, hooks) | T2-13, D-5, D-6 |
| §6 Gherkin style | G-3, G-5, G-6, G-10, T2-15 |
| §7 step defs (funnel, guard, no stale httpResponse, retryUntil) | T2-2, T2-4, T2-5, T2-6, T1-2, D-2, D-3, D-8 |
| §8 run & verify | process (N/A to lint) |
| §9 copyright header | T1-5 |
| §10 setup features naming | G-4, T2-16 |
| §11 gateway invocation | D-17 (review) |
| §12 actors / exact assertions | G-7, D-4, D-18 |
| §13 toml overlays | D-16 (review) |
| §14 provisioner boundary | T2-11 (heuristic) + D (review) |
| §15 shared glue reuse | T2-10 (CPD) |
| Anti-patterns list | T1-3, T1-2, T2-1, T2-10, T2-4, D-1, D-5 |

## 7. Decisions still open (for §7 of the rollout)
1. **SpotBugs/FindSecBugs**: add as a 3rd tool for the security rules (T1-12/13/14, T1-9), or route them to your
   existing CI SAST? (temp-file-permissions was already flagged by a SAST in the PRs — suggests one exists.)
2. ~~Per-rule vs all-or-nothing `error`~~ — **DECIDED: all-or-nothing.** The whole ruleset flips to
   `failOnViolation` together in the sweep PR once the tree is clean; until then everything stays `warning`.
3. **Gherkin `fail-on-violation`** now vs warn — depends on how many G-* violations the current tree has.

## 8. Notable gaps the PRs added beyond CLAUDE.md (worth landing regardless)
Security: Zip-Slip, weak TLS, trust-all confinement, temp-file perms · correctness: missing `switch` default,
unclosed resources on failure path, `InterruptedException` handling, blocking work on the common ForkJoinPool,
DOTALL on multi-line regex · hygiene: `System.out` in framework code, inline FQNs, repeated string literals,
unused `Requests.*` locals, stale/duplicate Javadoc, compiler-version drift.
