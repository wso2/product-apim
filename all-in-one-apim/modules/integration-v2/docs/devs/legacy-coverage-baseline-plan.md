# Legacy coverage baseline — plan

**Goal:** compute the legacy suite's code coverage with *exactly* the calculation v2 uses, then list the lines legacy
covers that v2 does not — coverage-level parity gaps.

## Background

- **v2** runs the JaCoCo agent in `tcpserver` mode, dumps one `.exec` per block (plus one per graceful restart, see
  `v2-coverage-architecture.md` §7), and `CoverageAggregationListener` merges them into `jacoco-it.xml`.
- **Legacy** (TAF) attaches the agent in file mode:
  `-javaagent:…/jacocoagent.jar=destfile=…/tests-backend/target/jacoco/jacoco<N>.exec,append=true,includes=org.wso2.carbon.apimgt*:api#am#*:internal#data*:client-registration*`.
  - Agent version is `0.8.12`, the same as v2, so the `.exec` formats are compatible.
  - With `append=true`, every JVM appends its counters on exit, so one file per group survives all restarts. Group 1 in
    run 36088050275 started Carbon 12 times (11 restarts) into a single 1.4 MB file.
  - TAF merges it to `jacoco-data-merge.exec` and renders its own `target/jacoco/coverage/jacoco.xml`, which the Codecov
    action uploads (flag `integration_tests`). The `.exec` itself is not kept.
- The 4 legacy groups (`group1`–`group4`) are four CI matrix jobs. Modelling each group as a v2 "block" gives four
  `.exec` files to aggregate, just as v2 aggregates its per-block files.

## Plan

### Step 1 — keep the legacy `.exec` (additive change to the legacy CI job)

- Add one `actions/upload-artifact` step to each legacy group job, uploading
  `all-in-one-apim/modules/integration/tests-integration/tests-backend/target/jacoco/jacoco-data-merge.exec` as
  artifact `legacy-exec-group<N>`.
- Nothing about how legacy tests run or report changes.

### Step 2 — one shared calculation

- Move the body of `CoverageAggregationListener.onFinish` into a static method with a small `main`. Its inputs are the
  `.exec` files, the class-file source directory and the output directory.
- The v2 listener calls the method; the baseline job calls the `main`. Both go through the same
  `extractApimgtClassfilesFromDir` → `JacocoCoverage.report` path, with the same excludes and the same denominator.
- **Proof of no change:** on v2's own `.exec` files it must reproduce today's v2 number exactly. The standalone call
  already reproduces the listener's figures (19.36% before and 21.73% after the restart fix, locally).

### Step 3 — new `coverage-baseline` CI job

- `needs:` the 4 legacy jobs and `v2-build-images`.
- Downloads the 4 legacy artifacts as `legacy-group1.exec` … `legacy-group4.exec` (one "block" per group).
- Extracts class files from the shared v2 image with the same `docker cp` the `integration-v2` job uses, and runs the
  shared calculation.
- Applies the same dump-count check as v2 (`coverage-summary.properties`): expected 4 of 4.
- **Correctness gate:** count classes whose recorded counters don't match the analysed class file
  (`IClassCoverage.isNoMatch()`) and fail if any exist. A mismatched class shows as 0% and would look like a fake gap.
  None are expected: `carbon.apimgt.version` is a pinned release (9.33.176), so both lanes run the same jars from Nexus.

### Step 4 — parity report

- Legacy and v2 side by side, per package and class.
- **Key output:** lines covered by legacy but not by v2 — the coverage-level parity gaps. Lines covered only by v2 are
  listed too.
- Published as a job artifact, with a short summary in the job log.

## Things to know up front

| Point | Detail |
|---|---|
| Needs the restart fix first | Without it v2 is under-counted and the comparison is biased against it. |
| Baseline ≠ legacy's Codecov number | TAF's own report also counts `lib/runtimes/cxf3` and applies `filters.txt`. The difference is expected; using one calculation for both lanes is the point. |
| Measures what legacy CI runs on a PR | CI passes `-DskipRestartTests=true` and skips the benchmark tests. That is the right parity target. |
| Agent scope | Legacy has no agent excludes and its include pattern covers all of `org.wso2.carbon.apimgt*` (the WAR-name patterns match no class names), so legacy records at least everything v2 records. The shared report applies v2's excludes to both lanes. |
| A signal, not a verdict | A legacy-only line is a candidate gap; each one still needs checking, as with the parity audits. |
| Cost | About 6 MB of artifacts and a few minutes for the new job. Legacy run time is unchanged. |

## Order and approvals

1. Commit the v2 restart fix.
2. Step 1 — edits the legacy CI job, so it needs approval. One CI run then yields real legacy `.exec` files.
3. Steps 2 and 3 — built and proven locally against that run's artifacts before touching CI again.
4. Step 4, then review the first baseline numbers together.
