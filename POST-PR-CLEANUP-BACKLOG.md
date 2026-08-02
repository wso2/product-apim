# integration-v2 glue/feature cleanup backlog (follow-up PR)

Tracking doc for mechanical cleanups **deliberately deferred** out of the Group-3 port PR
(`is7-key-manager-integration` / the G3-port branch). Each item below is a valid finding whose
*flagged occurrence was fixed in the G3 PR*, but whose identical siblings live in files the G3 PR
does **not** touch. We scoped the G3 PR to its own changed files to keep the diff reviewable; these
siblings are collected here to fix in a **separate, self-contained cleanup PR**.

- **Scope rule used in the G3 PR:** only files already in the PR's changed set were swept. Baseline
  for "changed set" = `git diff --name-only 4484d0d81..HEAD` plus the working tree.
- **These are zero-behaviour-change, mechanical, compile-verifiable edits.** They lose nothing by
  waiting and gain reviewability by landing on their own.
- Line numbers are a snapshot (2026-07-21) — **re-run the detector** before editing, don't trust the
  offsets blindly.

---

## 1. Dead local variable: `HttpResponse x = Requests.…` never read

**Defect class.** `Requests.*` already owns the `httpResponse` publish (clear-before / set-after),
so a local assigned from a `Requests.*` call that is never subsequently read is a pure dead
assignment (PMD/Checkstyle `UnusedLocalVariable`). Fix = drop the `HttpResponse x =` prefix; if
`HttpResponse` then has no other use in the file, drop its import too.

**Guard against false positives.** Keep the assignment when the local **is** read later in the same
method — e.g. an intermediate GET whose body is parsed locally (`new JSONObject(response.getData())`),
or a GET-then-mutate-then-PUT round trip. Those are legitimate uses, not dead code. (Example already
verified in-PR: `ApplicationBaseSteps.iShouldBeAbleToRetrieveSubscription` reads `response.getData()`,
so its local was correctly retained.)

**Done in the G3 PR (for reference):** 114 sites removed across ApplicationBaseSteps (55),
PublisherBaseSteps (41), GovernanceBaseSteps (9), OrganizationSteps (8), ApiProviderChangeSteps (1).

**Deferred — out-of-PR siblings (21 sites, 5 files):**

| File (stepdefinitions/) | Count | Lines (snapshot) |
|---|---|---|
| `AiApiSteps.java` | 7 | 59, 162, 172, 186, 197, 270, 296 |
| `ApiEndpointSteps.java` | 4 | 72, 81, 91, 100 |
| `MCPServerSteps.java` | 8 | 117, 158, 186, 341, 364, 378, 414, 424 |
| `MutualSslSteps.java` | 1 | 80 |
| `RemoteLoggingSteps.java` | 1 | 171 |

**Detector (re-run before editing):**
```python
# from tests-integration/cucumber-tests/
import re, pathlib
root = pathlib.Path("src/test/java/org/wso2/am/integration/cucumbertests/stepdefinitions")
assign = re.compile(r'HttpResponse\s+(\w+)\s*=\s*Requests\.')
for f in sorted(root.rglob("*.java")):
    lines = f.read_text().splitlines()
    for i, line in enumerate(lines):
        m = assign.search(line)
        if not m: continue
        var, j = m.group(1), i
        while ';' not in lines[j]: j += 1
        used = any(re.search(r'\b'+re.escape(var)+r'\b', lines[k])
                   for k in range(j+1, next((k for k in range(j+1, len(lines))
                                             if re.match(r'^    \}\s*$', lines[k])), len(lines))))
        if not used: print(f"{f.name}:{i+1}: {var}")
```

---

## 2. Unasserted deploy step (missing `Then The response status code should be 201`)

**Defect class.** A positive-path deploy step (`I deploy the API…`, `I deploy revision…`,
`I make a request to deploy revision…`, `I deploy the "<type>" resource…`) with **no immediately
following status assertion**. If the deploy silently fails, the scenario doesn't fail there — it dies
much later (gateway 404 / invocation timeout / a downstream assert), masking the true cause. Fix =
add `Then The response status code should be 201` right after the deploy step.

**Judgement required — not a blind insert.** An unasserted deploy is only a *real* defect when a
downstream assertion could be satisfied even if the deploy failed. Where the **very next** step would
itself fail loudly on a failed deploy, the assert is lower value:
- `… I deploy the "apis" resource … / When I create an MCP server from api …` — the MCP-create fails
  if the backing API wasn't deployed (backing-API deploys in `mcp_invocation`, `publisher/mcp_servers`,
  `governance/compliance`).
- `prototype_api` deploys are followed by a `retrieve … until it contains "PROTOTYPED"` poll.
Still worth adding the explicit 201 for consistency and a precise failure locus, but triage these vs.
the higher-value invocation-gated ones first.

**Done in the G3 PR (for reference):** 5 in-scope sites — `devportal/search.feature:247`,
`gateway/rest_invocation.feature:198,408`, `gateway/security_enforcement.feature:286`,
`key-manager/token_issuance.feature:162`. Validated 100/100 across the four affected runners.

**Deferred — out-of-PR siblings (38 sites, 16 files):**

| Feature file (features/) | Count | Lines (snapshot) |
|---|---|---|
| `gateway/ai_api_invocation.feature` | 6 | 39, 122, 159, 196, 231, 278 |
| `gateway/graphql_endpoint_security.feature` | 1 | 31 |
| `gateway/graphql_query_limits.feature` | 2 | 29, 59 |
| `gateway/graphql_scope_enforcement.feature` | 2 | 48, 114 |
| `gateway/graphql_subscription_invocation.feature` | 1 | 18 |
| `gateway/graphql_subscription_query_limits.feature` | 2 | 29, 53 |
| `gateway/graphql_subscription_throttling.feature` | 2 | 28, 53 |
| `gateway/mcp_invocation.feature` | 12 | 15, 53, 105, 144, 176, 220, 257, 260, 288, 293, 335, 340 |
| `gateway/multi_protocol_restart.feature` | 1 | 33 |
| `gateway/mutual_ssl_invocation.feature` | 1 | 23 |
| `gateway/schema_validation.feature` | 1 | 19 |
| `gateway/websocket_invocation.feature` | 1 | 216 |
| `governance/compliance.feature` | 1 | 105 |
| `publisher/mcp_servers.feature` | 1 | 155 |
| `publisher/prototype_api.feature` | 3 | 21, 43, 89 |
| `publisher/shared_scope_restart.feature` | 1 | 50 |

**Detector (re-run before editing):**
```python
# from tests-integration/cucumber-tests/src/test/resources/features/
import re, pathlib
deploy = [re.compile(r'^\s*(When|And|Given)\s+I deploy revision '),
          re.compile(r'^\s*(When|And|Given)\s+I deploy the API with id '),
          re.compile(r'^\s*(When|And|Given)\s+I deploy the "\S+" resource with id '),
          re.compile(r'^\s*(When|And|Given)\s+I make a request to deploy revision ')]
ok = re.compile(r'^\s*(Then|And)\s+The response status code should be')
for f in sorted(pathlib.Path('.').rglob('*.feature')):
    lines = f.read_text().splitlines()
    for i, line in enumerate(lines):
        if any(p.match(line) for p in deploy):
            nxt = next((lines[k] for k in range(i+1, len(lines))
                        if lines[k].strip() and not lines[k].strip().startswith('#')), '')
            if not ok.match(nxt): print(f"{f}:{i+1}")
```

---

## 3. Unescaped values interpolated into hand-built SOAP/XML envelopes

**Defect class.** A `String`-concatenated SOAP envelope interpolates a value (username, password, role,
URL, claim value, consumer key) straight into element text without XML-escaping. A value containing
`&`, `<`, or `>` corrupts the envelope (the request either faults or, worse, is silently mis-parsed).
Inputs are test-controlled today so live risk is low, but these are shared framework utilities — a
future feature using a special char (e.g. a password with `&`, or a remote-logging **URL with a `&`
query separator**) would fail confusingly. Fix = wrap the interpolated **data** value in
`Utils.escapeXml(...)` (added in the G3 PR, in `cucumbertests/utils/Utils.java`). Do **not** wrap
element/tag names, namespace URIs, or already-base64 values (base64 has no XML-special chars).

**Done in the G3 PR (for reference):** `Utils.escapeXml` helper + applied across `TenantUserProvisioner`
(addUser, addRole, getRoleListOfUser, isExistingUser, addUserInStore, addSecondaryUserStore/storeProp,
waitUntilStoreActive probe, deleteRole, deleteUser), `JwtGrantSteps` (addIdP/getIdPByName/updateIdP
envelopes), `KeyManagerAdminSteps` (updateConsumerAppState), `OrganizationSteps` (setUserClaimValue),
`ResourceCleanup` (removeOAuthApplicationData). Validated via the secondary-user-store, JwtGrant,
application-revocation, and org-visibility blocks (no-op for current special-char-free values → proves
no regression).

**Deferred — out-of-PR sibling (1 file):**

| File (stepdefinitions/) | Method | Values to escape (snapshot) |
|---|---|---|
| `RemoteLoggingSteps.java` | `sendConfigOp` (~107–108) | `logType`, `url` — **`url` can legitimately contain `&`** (query params). Escape the two `<ax2:…>` data values; leave the `op` tag-name interpolation alone. |

**Detector:** grep hand-built envelopes for raw interpolation —
`grep -rn 'soapenv:Envelope\|sendSoapRequest\|Requests.soap' src/test/java` then inspect each file's
`" + <var> + "</…>` and `.append(<var>)` sites; escape the ones carrying external/data values.

---

## 4. Fully-qualified type names where the codebase convention is to import

**Defect class.** A type used fully-qualified where the codebase convention (and every other step class)
imports the simple name — `org.json.{JSONObject,JSONArray,JSONException}`, `org.testng.Assert`, and the
`io.cucumber.java.en.*` annotations. A style inconsistency Checkstyle can flag. Fix = add the import and
use the short name.

**Done in the G3 PR (for reference):** de-qualified `org.json.*` across `BaseSteps` (6),
`ApplicationBaseSteps` (`JSONArray` ×2 + `JSONException` import), `ServiceCatalogSteps` (2),
`PublisherBaseSteps` (1), `JwtGrantSteps` (+`JSONArray` import), `KeyManagerAdminSteps` (+`JSONObject`
import); and `org.testng.Assert` + `@io.cucumber.java.en.Then` across `KeyManagerAdminSteps` (import
added), `GovernanceBaseSteps` (10, import added), `OrganizationSteps` (9, import added).

**Deferred — out-of-PR sibling (1 file, 2 sites):**

| File (stepdefinitions/) | Lines (snapshot) | Note |
|---|---|---|
| `MCPServerSteps.java` | 109 (`org.testng.Assert.assertTrue`), 115 (`new org.json.JSONArray(...)`) | `JSONArray` already imported (line 21); add an `org.testng.Assert` import; de-qualify both. |

**Detector:** flag any non-import line matching `(?<!import )\borg\.testng\.Assert\b`,
`@io\.cucumber\.java\.en\.`, or `\borg\.json\.(JSONObject|JSONArray|JSONException|JSONTokener)\b`; add the
missing import and de-qualify (every other step class already imports these).

---

## 5. Broad `catch (Exception)` in HTTP+parse retry/warm-up loops

**Defect class.** A retry-until-ready loop that does an HTTP call + parses the body and catches
`Exception` broadly. It rides out the transient set (network `IOException`, warm-up parse
`JSONException`/SSE) correctly, but also swallows **programming errors** (bad context key →
`IllegalArgumentException`, NPE) until the loop's deadline, surfacing them as a misleading "not ready"
timeout instead of failing fast (violates CLAUDE.md §7). Fix = narrow to the actual transient set
(`catch (IOException | JSONException e)` for JSON probes; add the SSE/parse type for MCP). Guard any
in-`try` `Thread.sleep` with its own `InterruptedException` handler so narrowing doesn't leak it.

**Done in the G3 PR (for reference):** `BaseSteps.waitFor…deployment` (the two-probe deploy-readiness
loop) and `PublisherBaseSteps.waitUntilRevisionIsDeployed` narrowed to `IOException | JSONException`.

**Deferred — out-of-PR sibling (1 file, 5 sites):**

| File (stepdefinitions/) | Lines (snapshot) | Note |
|---|---|---|
| `MCPInvocationSteps.java` | 115, 168, 209, 250, 291 | `catch (Exception transientDuringWarmup)` in the MCP invoke warm-up loops (HTTP + `sseOrJson` parse). Narrow to the real transient set (`IOException` + the JSON/SSE-parse exception `sseOrJson` throws — confirm its type first). |

**Explicitly NOT this defect (left broad, by design):** `WebSocketInvocationSteps` connect/send/close
warm-up catches — the WS client's transient-connect surface against a warming gateway is genuinely
broad and not an HTTP+JSON probe; narrowing risks masking legitimate transient states. One-shot
best-effort parses (`captureTokens`, operation-policy id extraction, `parseConfigValue`) are not retry
loops and correctly swallow-and-continue.

---

## Validation plan for the cleanup PR
Both cleanups are mechanical + compile-verifiable. Suggested gate:
1. `mvn test-compile -pl tests-integration/cucumber-tests -am` (catches any missed import removal).
2. Run the runners covering the touched feature files (build focused throwaway suites off
   `testng-v2.xml`'s block definitions — copy the exact `<listeners>` header:
   `utils.listeners.BlockLifecycleListener` + `BlockScopeListener`).
3. Re-run both detectors → expect zero remaining sites repo-wide.

## Related deferred items (not file-scoped sweeps — separate call each)
- `Constants.HTTP_RESPONSE` — defined but unused; "use-the-constant" refactor at the raw
  `"httpResponse"` string literals, or delete if we keep the literals. Design call, not mechanical.
- `@dep` baseline-tagging convention — needs a ruling recorded in TAGGING-CONVENTIONS.md before a sweep.

## Stale thread-count in overlay header (G3, comments-only)
`artifacts/configFiles/throttlingAndAllowedScopes/deployment.toml` line 2 says "co-hosted at
thread-count=1" but the IntegrationV2-ThrottlingAndAllowedScopes block runs thread-count=2 (bumped with 4x
validation in the G3 round; the testng-v2.xml comment was updated, the overlay header missed). One-line
comment fix; same drift class as the sandboxTxDisabled header fixed in the IS7 PR feedback batch.
Detector: grep overlay headers for "thread-count" and diff against the owning <test> block's attribute.

## DONE (2026-07-23, propagation-timeout batch): dead glue waitForPreviousAPIRevisionUndeployment removed (with the orphaned UNDEPLOYMENT_WAIT_TIME + MAX_RETRIES constants)
`BaseSteps.waitForPreviousAPIRevisionUndeployment` ("I wait for undeployment of the previous API
revision in {string}") has no consumer in any feature file (tracked or SSO lane) — dead since its
introduction in 5a68e1c99 ("Add concurrent support"), predating the IS7 PR. It was still hardened in
the IS7 review rounds (interrupt-aware sleep, then fail-on-interrupt) because reviewers read it as
live; removing it ends that recurring cost. Delete the step (and any then-unused imports), or wire it
into the revision-deployment features if the undeploy settle-wait is actually wanted somewhere.
Detector: for each `@Given/@When/@Then` pattern in the glue, grep features/ for a match; report
zero-hit patterns (catches any other dead glue in the same sweep).

## Unused `HttpResponse response` locals on non-asserting `Requests.*` calls (module-wide idiom)
Non-asserting steps whose only purpose is the funnel side effect (`Requests.*` publishes `httpResponse`
for a following `Then`) still assign `HttpResponse response = Requests.x(...)` and never read it — a
genuinely dead local. Flagged on the 3 `Requests.put` sites in ApplicationBaseSteps (~2880/2907/2954),
but the SAME shape recurs ~16× in ApplicationBaseSteps alone (post + put) and more across the glue, so
a 3-site patch would create inconsistency, not remove it. NOT urgent: no lint gate runs (no
maven-pmd-plugin / maven-checkstyle-plugin in the cucumber-tests → integration-v2 → all-in-one → root
pom chain), so nothing fails review — this is IDE-squiggle territory only. Do it as ONE consistent
module-wide sweep, and first pick the convention: either (a) drop the local on every non-asserting
`Requests.*` call (`Requests.put(...);`), or (b) keep it deliberately and suppress — I lean (a). Keep
the local ONLY where the response is actually read (asserts, `captureTokens`, id extraction).
Detector: `HttpResponse response = Requests\.(post|put|delete|patch|postMultipart)` immediately
followed by the method's closing `}` with no intervening use of `response`.

## (Infra proposal, NOT a mechanical sweep) Add a scoped PMD/Checkstyle gate for integration-v2
Several review nits this round (unused locals, swallowed interrupts, dead glue) are exactly what a static
analyser catches automatically. There is currently NO such gate: no maven-pmd-plugin / maven-checkstyle-plugin
anywhere in the pom chain (cucumber-tests → integration-v2 → all-in-one → root). Adding one would let the
build flag these instead of relying on reviewer eyes.
CAVEATS (why this is a discuss-first proposal, not a backlog one-liner):
  - Scope it to the **integration-v2 module only** with a **curated, minimal ruleset** (UnusedLocalVariable,
    EmptyCatchBlock/swallowed-InterruptedException, UnusedImports, and a few more). A default full ruleset would
    surface hundreds of findings across the legacy all-in-one-apim tree and be unworkable.
  - Decide `failOnViolation` posture: start **warn-only** (report, don't fail the build) to avoid blocking on a
    legacy backlog, then tighten to fail-on-new once the module is clean.
  - The parent WSO2 monorepo deliberately does not gate on PMD/Checkstyle, so this must not leak upward — bind the
    plugin in the integration-v2 pom, not a parent.
  - Land as its OWN PR (build-infra review), independent of the mechanical cleanups above.

## Guarding against fuzzy JSON-substring matching (op-selection class)
Context: `op.toString().contains(tool)` selected an MCP operation by substring-matching the whole serialized
op — fixable to `tool.equals(op.optString("target"))` (done at the 2 flagged sites; the file's own line ~151
already used the correct pattern). Two ways to keep this from recurring, in order of strength:
  1. (STRONGER — structural) Encapsulate the correct match in a shared helper so the wrong pattern is
     unwritable: `Utils.findJsonArrayElementByField(array, field, value)` (or an MCP-specific
     `findMcpOperationByTool(ops, tool)`). Every op-selection call site routes through it; no call site can
     regress to a whole-object substring match. Zero-dependency, ~10 lines, add to the §15 catalogue.
  2. (WEAKER — lint heuristic) A CUSTOM PMD XPath rule flagging `.toString().contains(<var>)` as a smell.
     CAVEAT: it is a heuristic, NOT a correctness check — legitimate stringify-and-contain assertions
     (`x.toString().contains(y)`) would false-positive, so it must be WARN-ONLY and reviewed, never a hard
     gate. No off-the-shelf PMD/Checkstyle rule catches this (it's semantic, not syntactic). Only worth
     doing IF the scoped-PMD infra proposal above is adopted; folds in as one extra custom rule there.
Recommendation: do (1) regardless (it's cheap and definitive); treat (2) as an optional rider on the PMD
proposal, not a standalone effort.

## UPSTREAM REPORT (carbon-apimgt product race): concurrent same-API export temp-dir collision
The v2 suite surfaced an intermittent import failure (900909 "cannot find the project definition") on
export/delete/import scenarios (e.g. definitions.feature "restricted-visibility ... export/import round-trip").
Root cause is server-side, NOT the test: concurrent exports of the SAME api by the SAME user collide in a
shared, non-unique temp dir. ExportUtils/CommonUtil.archiveDirectory keys the working dir only on
<user>-<apiName>-<version> (not per-export) and deletes it after zipping — so two simultaneous exports share
tmp/<user>-<apiName>-<version>/, one empties it mid-zip. API Governance's ComplianceEvaluationScheduler
materialises every NEWLY CREATED api as an "APIM project" through that same code path, so it races with the
test's GET /apis/{id}/export routinely, right after creation. Evidence in one failing run: a single
990601/APIImportExportException/FileNotFoundException on .../Definitions/swagger.yaml naming the scenario's
exact artifact UUID; the tenant row + sibling export scenarios passed. Same family as the
RemoteServerLoggerData.getUsername() NPE. FIX (product): make the export working dir unique per export
(e.g. include a request/UUID suffix) so concurrent exports of the same api don't share/clean each other's dir.
Test-side mitigation already landed (iExportApiToArchive re-exports until the zip contains api.yaml/api.json;
iDeleteTheResource deregisters a self-deleted id so cleanup doesn't log a spurious leak) — mitigation only,
does not fix the product race.

## Temp-file permissions: File.createTempFile -> Files.createTempFile (module-wide, 17 remaining sites)
`java.io.File.createTempFile` creates with a umask-dependent mode (typically world-readable 0644); NIO's
`java.nio.file.Files.createTempFile` creates with owner-only access already applied (0600 POSIX / restricted
ACLs elsewhere) and needs no manual PosixFilePermissions-plus-fallback branching. The SHARED helper
`Utils.classpathToTempFile` was switched (it is the §15 documented reuse path, so every future caller inherits
the safer default). 17 ad-hoc sites remain: SimpleHTTPClient:224 ("download"), ApplicationBaseSteps:2683,
ServiceCatalogSteps:105, MCPServerSteps:220, EndpointCertificateSteps:74, PublisherBaseSteps (×11: doc-content,
graphql-schema, graphql-derived, policy-spec, synapse-policy, openapi, and several "data"/additionalProperties).
PRIORITISE `SimpleHTTPClient:224` — it holds SERVER-returned payloads (exported API archives, which can carry
endpoint configuration), i.e. more sensitive than the committed repo fixtures the flagged helper copies.
Mechanical, behaviour-identical (same tmpdir, same prefix rules — all current prefixes are >=3 chars).
Detector: `grep -rn "File.createTempFile" ` under the module; expect zero hits after the sweep.

## Response-body guards: converge getData().isEmpty() -> isBlank() before JSON parsing (53 remaining)
`isBlank()` is strictly stricter than `isEmpty()` (it additionally rejects whitespace-only bodies), and every one
of these guards exists specifically to stop a bad body reaching `new JSONObject(...)`. With `isEmpty` a
whitespace-only body slips through into exactly the opaque `JSONException` the guard was written to prevent — and
in a poll loop that catches only `IOException`, that JSONException ESCAPES the loop and kills the poll instead of
retrying. The 3 guards in VisibilityAccessSteps were converted (the 2 flagged plus the identical third in the same
file, to keep it internally consistent). Module-wide the convention is currently mixed: ~53 `isEmpty` vs ~21
`isBlank`. Sweep the rest for one convention (isBlank) — mechanical and cannot break a passing test, since
anything `isEmpty` rejected is still rejected. DONE in the same wave: the §7 guard idiom in CLAUDE.md now shows
`isBlank()`, so new code stops reintroducing the old form.
Detector: `grep -rn "getData().isEmpty()"` under the module; expect zero hits after the sweep.

## (Conditional hardening) Reconcile revision-create after an uncertain IOException
`PublisherBaseSteps.iCreateResourceRevision` re-POSTs the non-idempotent revision-create after an
`IOException`, so a lost response could leave a phantom revision and the retry creates a second. NOT fixed
now, deliberately: the API is scenario-owned so the revision (a §5 child resource) cannot leak, and no
scenario creates more than 2 revisions against APIM's cap of 5 — a phantom cannot exhaust the quota. The
rationale is recorded in a comment at the catch site so this does not get re-litigated.
REVISIT IF a scenario ever creates 4+ revisions on one API, or a revision-limit test is added — then reconcile
by GETting the same `.../revisions` URL (a GET on it is the LIST, so no new helper is needed) and adopting the
NEWEST revision by createdTime (there is no unique key: the payload description is a fixed string). Do NOT
delete a discovered revision as part of reconciliation — a deployed revision must be undeployed first, so
rollback on an uncertain path risks destroying a legitimately deployed one.
