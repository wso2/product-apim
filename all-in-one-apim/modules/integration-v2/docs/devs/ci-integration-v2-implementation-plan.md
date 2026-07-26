# CI integration for the parallel-on-shared-container (v2) lane

Goal: add a **manually-triggered** GitHub Actions job that runs the new Cucumber/Testcontainers
integration suite (`testng-v2.xml`) on H2, using the parallel-on-shared-container model. The job lives
in the existing `.github/workflows/maven.yml` but is gated to `workflow_dispatch` only, so it never runs
on PRs until we promote it.

This is additive. The legacy `build` / `run-benchmark-test` / `show-report` jobs and the legacy
`testng.xml` are left untouched.

## Design decisions (settled)

- **Where:** new job in `maven.yml`, `if: github.event_name == 'workflow_dispatch'`.
- **DB:** none — H2 (packed with APIM) via the existing `default` profile.
- **Test selection:** `-Dsurefire.suite.xml=testng-v2.xml` (the override our pom fix enables). Legacy
  `testng.xml` is never referenced.
- **Profile:** reuse `default` (active-by-default: H2 + SSL + image-name props + parallel). We always pass
  the suite override, so no new profile is needed.
- **Concurrency:** suite-level `parallel="tests" thread-count="2"` (K=2) set inside `testng-v2.xml`
  (`ParallelToggleAlterSuiteListener` is not registered in this suite, so K lives in the XML, not the
  profile env). Matrix starts as a single entry; sharding comes later.
- **Job shape:** self-contained (build dist, then run tests, in the same job — no cross-job artifact
  handoff).
- **`testng-v2.xml` content:** starts minimal — one placeholder block. Real integration tests are added
  one `<test>` block at a time as they are rewritten onto `BaseBlockRunner`.

## Key facts that shape the steps

- The Maven product build produces the **distribution zip**, not a Docker image.
- The **Docker images** (APIM `wso2am:4.7.0-SNAPSHOT-jdk21` and `node-app-server:latest`) are built by
  `tests-common/testcontainers/pom.xml` `exec-maven-plugin` execs bound to `pre-integration-test`: they
  start a `python3 http.server :8000` over `distribution/product/target`, then `docker build` both images
  (the APIM image pulls the zip over `http://host.docker.internal:8000`).
- `pre-integration-test` only fires in the `install`/`verify` lifecycle — so the test step uses `install`,
  not `test`.
- **Ordering rule (the important one):** the dist zip must already exist at
  `modules/distribution/product/target` before the testcontainers exec runs. We guarantee this by building
  the dist in a separate prior step, then running the test step. This mirrors how `migration-tests.yaml`
  pre-stages the zip before its integration-v2 reactor runs.
- **Use the full all-in-one-apim reactor for the dist build — NOT `-pl :wso2am -am`.** The product dist
  assembly (`dist.xml`, `pre_dist` execution) bundles the apimgt component artifacts (`wso2carbon-core`,
  the `rest.api.*.feature` zips, etc.). A `-pl :wso2am -am` scoped build only builds the 3 distribution
  poms (aggregator → dist-parent → wso2am) and does not produce/resolve those components, so the assembly
  fails with *"archive cannot be empty."* The proven recipe (`migration-tests.yaml`) is a full
  `mvn clean install -Dmaven.test.skip=true`. Scoping is unnecessary anyway: `integration-v2` is a
  **separate reactor** (not a module of `all-in-one-apim/pom.xml`), so a full root build never reaches the
  testcontainers Docker-image-build execs — the "no premature Docker build" property holds without it.

## Local prerequisites

- JDK 21, Maven, `python3`, Docker (colima) with `/var/run/docker.sock` symlinked to the colima socket.
- Confirmed coordinates: product module = `:wso2am` at `modules/distribution/product`; zip =
  `modules/distribution/product/target/wso2am-4.7.0-SNAPSHOT.zip`.
- All commands below are run from the repo root unless noted.

---

## Tasks

### T1 — Confirm the distribution build target (no code change)

**Goal:** prove a full `all-in-one-apim` reactor build with `-Dmaven.test.skip=true` produces the zip and
triggers **no** Docker build (so step 1 of the job is safe and won't fire the image build prematurely).

> **Note (2026-06-25):** an earlier attempt scoped to `-pl :wso2am -am` FAILED with
> `maven-assembly-plugin (pre_dist): archive cannot be empty` — that narrow set doesn't build the apimgt
> components the dist assembly bundles. Use the full reactor (the proven `migration-tests.yaml` recipe).
> The Docker-build-not-triggered half held even in the failed attempt (images unchanged), and is preserved
> by the full build because `integration-v2` is a separate reactor.

**Local verification:**
```bash
cd all-in-one-apim
rm -f modules/distribution/product/target/wso2am-4.7.0-SNAPSHOT.zip
docker images --format '{{.Repository}}:{{.Tag}} {{.CreatedAt}}' | grep -E 'wso2am|node-app-server' || true   # note timestamps
mvn clean install -f pom.xml -Dmaven.test.skip=true -DskipBenchMarkTest=true -DskipRestartTests=true
ls -lh modules/distribution/product/target/wso2am-4.7.0-SNAPSHOT.zip
docker images --format '{{.Repository}}:{{.Tag}} {{.CreatedAt}}' | grep -E 'wso2am|node-app-server' || true   # timestamps unchanged => no rebuild
```

**Done when:** the zip exists and the APIM/node image timestamps are unchanged (the dist build did not
`docker build`).

> **Verified 2026-06-25:** full-reactor build `BUILD SUCCESS` (16.5 min, 42 modules); produced
> `modules/distribution/product/target/wso2am-4.7.0-SNAPSHOT.zip` (514 MB); wso2am/node-app-server image
> timestamps unchanged (2026-06-24) — no Docker build fired.

- [x] T1 complete

---

### T2 — Create `testng-v2.xml` seed

**Goal:** add the minimal v2 suite (one placeholder block) under the block listeners.

**Change:** new file `tests-integration/cucumber-tests/src/test/resources/testng-v2.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
<!--
  Integration test suite for the parallel-on-shared-container (v2) lane. Starts minimal: one placeholder
  block proving the lane boots a shared container on H2 and runs a class against it. Real integration
  tests are added one <test> block at a time as they are rewritten onto BaseBlockRunner. The legacy
  testng.xml is never referenced; this suite is selected via -Dsurefire.suite.xml=testng-v2.xml.
  K (concurrent blocks/containers) is set here via thread-count, not via profile env.
-->
<suite name="Integration Tests v2" parallel="tests" thread-count="2">
    <listeners>
        <listener class-name="org.wso2.am.integration.cucumbertests.utils.listeners.BlockLifecycleListener"/>
        <listener class-name="org.wso2.am.integration.cucumbertests.utils.listeners.BlockScopeListener"/>
    </listeners>
    <test name="IntegrationV2-PlaceholderBlock" parallel="classes" thread-count="2">
        <parameter name="blockLabel" value="integration-v2"/>
        <parameter name="initTenantUsers" value="true"/>
        <classes>
            <class name="org.wso2.am.integration.cucumbertests.runners.block.BlockProbeRunnerOne"/>
        </classes>
    </test>
</suite>
```

**Local verification:** (requires the zip from T1 present)
```bash
cd all-in-one-apim/modules/integration-v2
mvn install -pl tests-integration/cucumber-tests -am \
    -Dsurefire.suite.xml=testng-v2.xml \
    -Ddocker.extra.hosts="--add-host=host.docker.internal:host-gateway"
# inspect results
grep -o '<testng-results[^>]*>' tests-integration/cucumber-tests/target/surefire-reports/testng-results.xml
```

**Done when:** `BUILD SUCCESS`; testng-results shows the placeholder block ran (`passed="1" failed="0"
skipped="0"`); the APIM + node images built; the `DynamicApimContainer` booted on H2 and was stopped at
block finish (no leftover `block=integration-v2` container: `docker ps -a | grep integration-v2` empty).

- [x] T2 complete — verified 2026-06-25. `mvn install -pl tests-integration/cucumber-tests -am
  -Dsurefire.suite.xml=testng-v2.xml -Ddocker.extra.hosts=...` → `BUILD SUCCESS` (BUILD_RC=0);
  testng-results `passed="1" failed="0" skipped="0"`; both images rebuilt fresh (wso2am 20:22,
  node-app-server 20:10); no leftover `block=integration-v2` container. node-app-server base switched
  to `node:18-alpine`, shrinking that image 1.18 GB → 189 MB with no behavior change.

---

### T3 — Add the `integration-v2` job to `maven.yml`

**Goal:** append the manually-triggered job; leave legacy jobs byte-for-byte unchanged.

**Change:** add to `.github/workflows/maven.yml` (after the existing jobs):

```yaml
  integration-v2:
    if: github.event_name == 'workflow_dispatch'
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        include:
          - id: v2-1
            suite: testng-v2.xml
    steps:
      - name: Run hostname
        run: hostname
      - name: Fix host entry
        run: sudo echo "127.0.0.1 $(hostname)" | sudo tee -a /etc/hosts
      - name: Checkout
        uses: actions/checkout@v4
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      - name: Build APIM distribution (skip tests)
        run: >
          mvn clean install -f all-in-one-apim/pom.xml
          -Dmaven.test.skip=true -DskipBenchMarkTest=true -DskipRestartTests=true
      - name: Run integration-v2 suite (H2, parallel-on-shared-container)
        working-directory: all-in-one-apim/modules/integration-v2
        run: >
          mvn install -pl tests-integration/cucumber-tests -am
          -Dsurefire.suite.xml=${{ matrix.suite }}
          -Ddocker.extra.hosts="--add-host=host.docker.internal:host-gateway"
      - name: Archive test reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: integration-v2-reports-${{ matrix.id }}
          path: |
            all-in-one-apim/modules/integration-v2/tests-integration/cucumber-tests/target/surefire-reports/**
            all-in-one-apim/modules/integration-v2/tests-integration/cucumber-tests/target/cucumber-report/**
          if-no-files-found: warn
```

**Local verification:**
```bash
# diff shows ONLY an added job, no edits to legacy jobs
git diff .github/workflows/maven.yml
# YAML is valid
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/maven.yml')); print('yaml ok')"
# (optional, if installed) actionlint .github/workflows/maven.yml
```

**Done when:** the diff is purely additive (legacy `build`/`run-benchmark-test`/`show-report` untouched),
YAML parses, and the `if: github.event_name == 'workflow_dispatch'` gate is present.

- [x] T3 complete — verified 2026-06-25. Appended the `integration-v2` job after `show-report` in
  `.github/workflows/maven.yml`; `git diff --stat` shows 41 insertions / 0 deletions (legacy
  `build`/`run-benchmark-test`/`show-report` untouched); `yq '.jobs | keys'` lists all four jobs and
  parses cleanly; the `if: github.event_name == 'workflow_dispatch'` gate is present.

---

### T4 — End-to-end local dry run (mimic a fresh CI runner)

**Goal:** run the two job commands back-to-back against a cleared image cache, exactly as CI will.

**Local verification:**
```bash
# force the image-build path to run from scratch
docker rmi -f wso2am:4.7.0-SNAPSHOT-jdk21 node-app-server:latest 2>/dev/null || true
rm -f all-in-one-apim/modules/distribution/product/target/wso2am-4.7.0-SNAPSHOT.zip

# step 1: build dist (no docker build expected here — integration-v2 is a separate reactor)
mvn clean install -f all-in-one-apim/pom.xml \
    -Dmaven.test.skip=true -DskipBenchMarkTest=true -DskipRestartTests=true

# step 2: build images + run the v2 suite on H2
cd all-in-one-apim/modules/integration-v2
mvn install -pl tests-integration/cucumber-tests -am \
    -Dsurefire.suite.xml=testng-v2.xml \
    -Ddocker.extra.hosts="--add-host=host.docker.internal:host-gateway"
```

**Done when:** both commands succeed; images were rebuilt by step 2 (not step 1); suite is green; reports
exist under `tests-integration/cucumber-tests/target/surefire-reports/`.

- [ ] T4 complete

---

### T5 — CI verification (manual trigger)

**Goal:** confirm the job runs green on a real runner.

**Steps:**
```bash
git add .github/workflows/maven.yml \
        all-in-one-apim/modules/integration-v2/tests-integration/cucumber-tests/src/test/resources/testng-v2.xml \
        all-in-one-apim/modules/integration-v2/docs/devs/ci-integration-v2-implementation-plan.md
git commit -m "Add manually-triggered integration-v2 CI job and testng-v2 suite"
git push
# trigger (workflow name in maven.yml is "APIM builder")
gh workflow run "APIM builder" --ref master-new-test-framework
gh run watch
```

**Notes / expectations:**
- On `workflow_dispatch` the legacy `build` matrix also runs (it already has a `workflow_dispatch`
  trigger — unchanged behavior). Look specifically at the `integration-v2` job result.
- The `integration-v2` job should be green and upload the `integration-v2-reports-v2-1` artifact.
- If APIM readiness flakes under boot contention, drop `thread-count` in `testng-v2.xml` to `1`
  (public 4-vCPU runner is the K=2 ceiling; private 2-vCPU needs K=1).

**Done when:** the `integration-v2` job is green in Actions and the report artifact is present.

- [ ] T5 complete

---

### T6 — Build the dist + images once, share across matrix entries

**Goal:** when `integration-v2` grows beyond one matrix entry, stop per-entry rebuilding. Each matrix
entry is a separate job on a fresh runner (own filesystem + own Docker daemon), so as written every
entry re-runs both the ~15-min dist build and the image builds. Build once, fan out.

**Approach:** split into a prerequisite `v2-build-images` job + the `integration-v2` matrix job:
- `v2-build-images`: `mvn clean install -f all-in-one-apim/pom.xml -Dmaven.test.skip=true ...` (dist),
  then `mvn install -pl tests-integration/cucumber-tests -am -DskipTests` (builds images, runs no
  suite), then `docker save wso2am:4.7.0-SNAPSHOT-jdk21 node-app-server:latest | gzip` → upload-artifact.
- `integration-v2` (`needs: v2-build-images`): download-artifact → `docker load` → run the suite with
  **`mvn test`** (not `install`). Key enabler: the image-build execs bind to `pre-integration-test`,
  which runs in `install`/`verify` but NOT `test`, so `mvn test` reuses the loaded images and never
  rebuilds. The framework boots the container from the fixed tag, so a `docker load` is all each entry needs.

**Ship-images mechanism — decide empirically:** `docker save`→artifact→`docker load` (no registry, but
tarball is large: wso2am ~1.4 GB + node 189 MB, gzipped ~600 MB–1 GB, so per-entry upload/download adds
minutes) vs push to GHCR + `docker pull` (cleaner past ~4–5 entries, needs registry auth + GHCR image
tag). The ~15-min build is a generous budget to amortize against, so measure the tarball download cost
on a real runner before committing — only worth it if download < rebuild.

**Done when:** the dist + image build runs exactly once per workflow run regardless of matrix size, and
matrix entries consume the prebuilt images (verified by the absence of an image-build step in their logs).

- [ ] T6 complete

---

## Out of scope (tracked elsewhere)

- Rewriting real integration tests onto `BaseBlockRunner` (added to `testng-v2.xml` one block at a time).
- Matrix sharding across blocks + per-shard `threads` (introduce once there is >1 real block).
- Promoting the job to a PR gate (flip trigger to `pull_request` or fold into the legacy matrix once
  stable).
- Shared `NodeAppServer.restart()` hazard under K>1 (see `parallel-framework-implementation-plan.md`).
