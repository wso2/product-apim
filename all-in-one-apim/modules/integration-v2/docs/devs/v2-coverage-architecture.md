# v2 integration-test coverage — architecture & local verification (all-in-one)

**Status:** Implemented — opt-in `-Dapim.coverage=true` drives agent-at-boot → per-block dump → suite-end merge+report (verified locally end-to-end). The CI wiring is committed too: the `integration-v2` job runs the suite with coverage, extracts the product class files from the already-shared Docker image via `docker cp` (no separate dist-zip artifact — the image is byte-identical to what ran; passed via `-Dapim.coverage.classfiles`), and a Codecov upload step publishes it (flag `integration-v2_tests`). See §8.
**Scope:** Server-side JaCoCo coverage for the **integration-v2 all-in-one lane** (the `DynamicApimContainer` Testcontainers path), uploaded to the **product-apim** Codecov project **standalone** — the same model the legacy lane uses (flag `integration_tests`). The coverage requirement is **all-in-one only** — the distributed lane is **not** a coverage target (by requirement, not merely deferred).
**Prior art:** modelled on the api-platform (Go) coverage pipeline, adapted to Java/JVM + JaCoCo.

---

## 1. Goal & non-goals

**Goal.** When the v2 all-in-one integration suite runs against a containerised APIM server, capture which product code (`org.wso2.carbon.apimgt.*`) actually executed, render a JaCoCo report locally, and — **per PR, on the same trigger as the legacy builder** — upload it to the **product-apim** Codecov project under flag `integration-v2_tests` (standalone, alongside legacy's `integration_tests`). product-apim's scope is integration coverage only; a unit+integration merge, if wanted, is carbon-apimgt's concern (§8).

**Non-goals.**
- Distributed lane coverage (ACP/GW/TM multi-JVM merge). Out of scope by requirement — coverage is all-in-one only.
- Blocking PRs on coverage thresholds (start informational).
- Solving cross-repo attribution definitively (documented as a decision, §8).

---

## 2. Approved decisions (locked)

| # | Decision | Choice |
|---|----------|--------|
| **A** | How to attach the JaCoCo agent | **A1 — runtime injection via `JAVA_TOOL_OPTIONS`**, opt-in behind `-Dapim.coverage=true`. No image rebuild, no startup-script edit. |
| **B** | How to extract the `.exec` | **B1 — `output=tcpserver` + on-demand dump** over a mapped port at block/suite teardown (no reliance on graceful-stop flush or writable bind mounts). |
| **C** | How to render the report | **C1 — JaCoCo CLI `report`** with classfiles pulled from the built distribution + sources from the product source tree, **with scoping** (§4). |
| **D** | Distributed lane | **Not a coverage target** (by requirement) — all-in-one only. |
| **E** | Local verification | Fully local via JaCoCo (no Codecov needed to compute/view). Codecov is CI-only. See §6. |
| **F** | CI cadence | **per PR** (same trigger as the legacy builder) → product-apim project, flag `integration-v2_tests`, carryforward. Heavy — shard like legacy's group1-4 if needed. |

Toolchain note: JaCoCo `0.8.12` is already available in the build (`jacoco.agent.version` in `all-in-one-apim/pom.xml`); reuse it.

---

## 3. The pipeline (all-in-one)

```
[boot]      DynamicApimContainer starts APIM server JVM with the JaCoCo agent
            (JAVA_TOOL_OPTIONS=-javaagent:.../jacocoagent.jar=output=tcpserver,address=*,port=6300,includes=...)
            container exposes 6300 -> Testcontainers maps to an ephemeral host port
[run]       Cucumber/TestNG blocks drive HTTP/gateway traffic; instrumented server records counters in-JVM
[dump]      at block teardown (BEFORE stopping the container), pull counters via the mapped port
            -> coverage/<block>.exec        (JaCoCo ExecutionDataClient, or `jacococli dump`)
[merge]     at suite end: jacoco merge coverage/*.exec -> coverage/it.exec
[report]    jacococli report coverage/it.exec
              --classfiles <apimgt jars from distribution>   (SCOPED, §4)
              --sourcefiles <carbon-apimgt / module source>
              --xml coverage/output/txt/jacoco-it.xml --html coverage/output/html
[CI only]   codecov upload coverage/output/txt/jacoco-it.xml --flag integration-v2_tests  (-> product-apim project)
[codecov]   stands alone in the product-apim project (like legacy integration_tests); no unit merge here
```

Key ordering rule (same lesson as api-platform): **dump before the container stops.** With `tcpserver` we dump over the live mapped port, so we're not dependent on SIGTERM flushing — but the dump call must happen before `stop()` removes the container.

---

## 4. Scoping (C1) — two layers, both required

Scoping happens in **two** places that do different jobs:

### 4.1 Agent-level `includes`/`excludes` — perf & noise
Controls which classes get *instrumented*. Set in the agent string (JaCoCo patterns match the fully-qualified class name; `*`/`?` wildcards):
```
includes=org.wso2.carbon.apimgt.*
excludes=*.stub.*:*.dto.*:*.gen.*:*Test:*.thrift.*
```
This is the direct analogue of the legacy TAF `instrumentation.txt` (include list) + `filters.txt` (exclude list).

### 4.2 Report-level `--classfiles` — the actual denominator
JaCoCo only counts classes fed as class files. This is where the real scope is set. **Two sources** (both needed — the PoC confirmed the OSGi plugins alone miss the REST-API impl):
- **OSGi plugins:** `repository/components/plugins/org.wso2.carbon.apimgt.*.jar` (core/impl/gateway/keymgt + `rest.api.common`).
- **Webapp code (nested in the WARs):** inside each `repository/deployment/server/webapps/*.war`, the
  `WEB-INF/classes/org/wso2/carbon/apimgt/**.class` — the publisher/devportal/admin REST-API impl classes live
  here (e.g. `api#am#publisher.war` ships ~80 such `.class` files, **not** in `WEB-INF/lib`) — plus any
  `WEB-INF/lib/*apimgt*.jar`. This mirrors the legacy `<coverageClassesRelativeDirectories>` (webapps + plugins + cxf3).

PoC effect of adding webapps: analyzed set grew **2146 → 3005 classes, 68,962 → 111,985 lines** (983 webapp `.class` files). The % *drops* when you add untested REST classes to the denominator — expected and correct.

**Rule of thumb:** agent includes = perf hygiene; report classfiles = the number. If the classfiles set is right, the % is correct even if the agent instrumented slightly more.

### 4.3 Where classfiles & sources come from
- **Class files:** read out of the **built distribution zip** (the same artifact the image is built from — plugins directly, webapp classes via nested WAR extraction), exactly as `DistributedDbScripts` reads DDL from the zip. This guarantees byte-identical class-id matching.
- **Sources:** *version drift is real* — the pom pins `carbon.apimgt.version` (currently `9.33.141`), but a given built zip may carry an earlier build (observed `9.33.134`), and a random local checkout differs again. So do **not** rely on a local checkout matching. For the **Codecov (carbon-apimgt) target this is a non-issue**: JaCoCo XML carries package-relative paths and Codecov resolves sources from the carbon-apimgt repo itself at the analyzed ref. Local HTML line-highlighting is best-effort only (`-Dapim.coverage.sources`).

---

## 5. How it plugs into the existing v2 framework

Mirror api-platform's `common/testutils/coverage` package shape.

1. **`tests-common`: `JacocoCoverage` helper** (analogue of api-platform's coverage collector):
   - stages `org.jacoco.agent-<ver>-runtime.jar` and `org.jacoco.cli-<ver>-nodeps.jar` (via `maven-dependency-plugin copy`, as the legacy `copy-jacoco-dependencies` did);
   - builds the `JAVA_TOOL_OPTIONS` agent string (§4.1);
   - `dump(host, mappedPort, destExec)` (JaCoCo `ExecutionDataClient` in-process, or shell `jacococli dump`);
   - `merge(dir) -> it.exec`; `report(exec, classfiles, sources, xmlOut, htmlOut)`.
2. **`DynamicApimContainer.withCoverage()`** — when `-Dapim.coverage=true`: `withCopyToContainer(agentJar, "/opt/cov/jacocoagent.jar")`, `withEnv("JAVA_TOOL_OPTIONS", agentString)`, `withExposedPorts(6300)`; expose `getCoverageMappedPort()`.
3. **`BlockLifecycleListener`** — on block finish, **before** stopping the container, call `JacocoCoverage.dump(...)` into `coverage/<block>.exec`.
4. **Suite end / Maven `post-integration-test`** — `merge` all block execs → `report --xml --html` (scoped, §4) → `coverage/output/`.
5. **`pom.xml`** — property `apim.coverage` (default `false`); dependency-plugin copy of the agent + cli jars; keep the existing unit `jacoco-maven-plugin` untouched.
6. **`codecov.yml`** (new, repo-level) — flags + carryforward (§6.3).

Layout stays identical between local and CI (`coverage/output/…`), so the CI job's only *extra* line over local is `codecov upload`.

---

## 6. Local verification (E) — no Codecov required

**Principle: local == CI minus the final upload.** Report *generation* is identical; only `codecov upload` is CI-only (needs the token). If local `jacoco.xml`/HTML is correct, ~95% of the pipeline is verified before CI.

**Do we need to send to Codecov?** For the shared dashboard and PR comments — yes, and that's CI-only. For seeing the (integration) report locally — no; JaCoCo renders the HTML/XML on the box. product-apim's concern is **integration coverage only**; any unit+integration combination is carbon-apimgt's responsibility (§8), not something we reproduce locally here.

### 6.1 Level 1 — mechanics (no Codecov)
Prove instrument → dump → report and open the HTML:
```bash
mvn -pl tests-integration/cucumber-tests test \
    -Dapim.topology=allinone -Dapim.coverage=true -Dsurefire.suite.xml=testng-v2.xml
# framework dumps per-block execs and merges -> coverage/it.exec, then:
java -jar org.jacoco.cli-0.8.12-nodeps.jar report coverage/it.exec \
    --classfiles <unpacked-dist>/.../org.wso2.carbon.apimgt.*.jar \
    --sourcefiles <carbon-apimgt-src> \
    --xml coverage/output/txt/jacoco-it.xml --html coverage/output/html
open coverage/output/html/index.html      # total % also printed to stdout
```
Confirms: agent attached, tcpserver dump over mapped port worked, classfiles/sources map correctly.

### 6.2 Level 2 — validate the Codecov config offline (nothing sent)
```bash
curl -s --data-binary @codecov.yml https://codecov.io/validate      # validate yaml
codecovcli do-upload --dry-run --flag integration-v2_tests --file coverage/output/txt/jacoco-it.xml
```
`--dry-run` exercises the exact flag/path wiring without the token or sending data.

> Note: there is no local "combined unit+integration" preview to do — product-apim has no unit tests, and the
> unit+integration merge (if any) is carbon-apimgt's concern (§8). Locally you verify the **integration** report only.

Proposed `codecov.yml` (product-apim project, standalone):
```yaml
flags:
  integration-v2_tests: { carryforward: true }   # v2 all-in-one lane (coexists with legacy integration_tests)
coverage:
  status:
    project: { default: { informational: true } }   # non-blocking to start
```

### 6.3 Suggested developer entry point
Add a `make coverage-it` (or script) that runs Level 1 end-to-end and opens the HTML — the analogue of api-platform's `gateway/it` `make coverage-report`.

---

## 7. Risks & mitigations
- **Perf/flakiness** — agent instrumentation slows startup & adds memory, and this runs per PR (heavy). Keep coverage opt-in; raise container mem; treat coverage failures as non-fatal (don't fail the suite on a dump/report error); shard like legacy's group1-4 if per-PR wall-clock hurts.
- **Classfile/source skew** — the `.exec` must be reported against the exact jars/sources that ran. Pull classfiles from the same distribution zip the image was built from; pin the `carbon-apimgt` source SHA used for `--sourcefiles`.
- **Under-count** — black-box tests + many disabled scenarios ⇒ the % is a floor. Keep Codecov status `informational: true` initially.
- **`JAVA_TOOL_OPTIONS` leakage** — it applies to every JVM in the container and logs a "Picked up JAVA_TOOL_OPTIONS" line; verify no init/bootstrap JVM double-counts or breaks on it.
- **tcpserver reachability** — must use `address=*` and expose the port, or you hit the classic "connection refused." Dump strictly before `stop()`.

---

## 8. Where the report goes — DECIDED: product-apim (standalone), matching legacy
The v2 integration tests exercise `org.wso2.carbon.apimgt.*` code, so in principle a *combined* number would live in the carbon-apimgt project (where its unit tests are). We chose **not** to do that.

**Decision (locked): upload v2 integration coverage to the product-apim Codecov project, standalone, PER PR — exactly the legacy mechanism.** A per-PR workflow (same `on: pull_request` trigger as legacy's "APIM builder", in product-apim) builds + runs the v2 lane and uploads `jacoco-it.xml` to product-apim's own project under flag **`integration-v2_tests`** — landing on the same PR commit as legacy's `integration_tests`, as a separate flag (Codecov keeps both). No cross-repo upload, no commit-SHA alignment, no unit merge. The CI workflow is now **committed**: the `integration-v2` job runs with `-Dapim.coverage=true` and a `codecov/codecov-action` upload step (flag `integration-v2_tests`). No `codecov.yml` is required (none committed — the flag is passed via the action, exactly like legacy's `integration_tests`). **Class files come from the already-shared image (no dist-zip artifact):** `integration-v2` already `docker load`s the product image (`v2-images`); after loading it `docker cp`s the classfile sources — `repository/components/plugins` + `repository/deployment/server/webapps` — out of a throwaway `docker create` container into `$RUNNER_TEMP/cov-classfiles`, and passes `-Dapim.coverage.classfiles=<dir>` to the suite. `docker cp` is a **local** copy, so there is **zero extra artifact transfer**. The inline `codecov.yml` in §6.2 remains the intended shape if per-flag tuning is ever wanted.

**Image-classfile extraction — DONE (was the `v2-dist-zip` follow-up).** Originally the report mined the built distribution zip, shared to `integration-v2` as a ~538 MB `v2-dist-zip` artifact. That artifact is now **gone**: the job extracts class files from the image it already downloads. Mechanism + rationale:

```bash
# integration-v2 job, after `docker load`:
APIM_IMAGE=$(mvn -q -N help:evaluate -Dexpression=apim.docker.image.name -DforceStdout)
APIM_SERVER_NAME=$(mvn -q -N help:evaluate -Dexpression=apim.server.name -DforceStdout)
SRC="$RUNNER_TEMP/cov-classfiles"; mkdir -p "$SRC"
cid=$(docker create "$APIM_IMAGE")
docker cp "$cid:/home/wso2carbon/$APIM_SERVER_NAME/repository/components/plugins"        "$SRC/plugins"
docker cp "$cid:/home/wso2carbon/$APIM_SERVER_NAME/repository/deployment/server/webapps" "$SRC/webapps"
# suite runs with -Dapim.coverage.classfiles="$SRC"
```

- **More correct, not just cheaper.** The image *is* what runs, so its classes are byte-identical to the executed ones by construction — exact JaCoCo class-id match. It also sidesteps the §4.3 "which build did the zip carry" version-drift question (the image's plugins are `9.33.134`, matching the runtime). The original dist zip is *deleted* from the image by the Dockerfile after unpack, so we extract the **unpacked tree**, not a zip.
- **Code.** `JacocoCoverage.extractApimgtClassfilesFromDir(dir, dest)` mines a directory tree (`Files.walk` matching `org.wso2.carbon.apimgt.*.jar` plugins + `*.war` webapps by NAME — the cp'd tree is already scoped, so no path-substring rule needed); it shares the WAR-mining/dedupe/zip-slip/partial-drift logic with the zip path via the private `ClassfileExtraction` helper, so the apimgt-selection rules live in ONE place. `CoverageSupport.classfilesSourceDir()` reads `-Dapim.coverage.classfiles`; when unset (local dev) the report falls back to the distribution zip.
- **`v2-build-images` needs no coverage step.** The image it already ships suffices; coverage is purely an `integration-v2`-job concern. (Bonus: the build job's "Resolve APIM artifact names" step dropped to a single `help:evaluate`.)
- **Verified parity.** Extracting from the docker-cp'd image tree yields the *identical* denominator as the zip path — **plugins=31, webapp-classes=983** from both — so the coverage number is unchanged (proven-green 25.24% at TP=2). Trade-off: the `docker cp` pulls the whole `plugins/` dir + all WARs to the runner's local disk (a few hundred MB, no network); the extractor still filters to apimgt.

**Consequence (accepted):** there is **no combined unit+integration number** — product-apim has no unit tests, and carbon-apimgt's unit coverage stays in the carbon-apimgt project. v2 integration coverage stands alone (like legacy's ~9.4%).

**If a combined number is ever wanted (alternative, not chosen):** attribute the integration report to the carbon-apimgt Codecov project instead — upload with codecov-action `slug: wso2/carbon-apimgt` from a carbon-apimgt checkout (so JaCoCo's package-relative paths resolve and the upload is keyed to a real carbon-apimgt commit), flag `integration`; carbon-apimgt's per-PR `unit` then line-unions with it (carryforward bridges the cadence gap).

---

## 9. Phased rollout
1. **Spike (Level 1):** `-Dapim.coverage=true` on `DynamicApimContainer`; tcpserver-dump one block; hand-run `jacococli report`; eyeball HTML. Proves instrumentation + classfile mapping.
2. **Collector + listener:** generalise into `tests-common`; wire `BlockLifecycleListener` dump; suite-end merge + report; emit `coverage/output/txt/jacoco-it.xml`.
3. **CI + Codecov:** per-PR workflow (same trigger as legacy) uploads the integration report to the **product-apim** project under flag `integration-v2_tests` (standalone, like legacy); add `codecov.yml`. Unit+integration merge, if ever wanted, is carbon-apimgt's concern (§8).
4. **Later:** extend the same collector to the distributed lane (per-component dump + `jacoco merge`) — deferred.
