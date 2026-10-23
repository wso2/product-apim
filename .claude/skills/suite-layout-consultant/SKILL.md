---
name: suite-layout-consultant
description: >-
  Decide where new or changed integration-v2 runners, features and overlays belong in the 4-lane CI suite layout,
  from measured runtime. Measures a runner's isolated runtime, models block length (boot + longest lane) and the
  4-queue packing, and recommends the cheapest placement: extend an existing feature/runner, join a compatible
  block, merge an orthogonal overlay, split an oversized runner/feature, and only as a last resort a new block.
  Mandatory for test-coverage work that adds or grows runners, features, blocks or overlays; also use to audit or
  rebalance the suite layout.
---

# Integration-V2 Suite Layout Consultant

The all-in-one suite runs every `<test>` block of `testng-v2.xml` on a 4-CPU / 16 GB GitHub runner, 4 blocks at a time
(`<suite parallel="tests" thread-count="4">`). Each block boots its own APIM container (and any IS/Squid/Solace it
needs), runs its runners on `thread-count` lanes, and tears down. Wall time is therefore set by how the blocks pack into
4 queues. Consolidating overlays, merging the WsProxy and NetworkAccessControl blocks into compatible blocks, splitting
blocks/runners/features by measured time and setting `thread-count` per workload took the suite from about 2 h 30 min
to about 1 h 10 min (the last CI run: 31 blocks, 15,203 s of block time, 4,068 s wall, ~93% packing efficiency).

Every test change can erode that: a new block costs a whole container boot (~115–170 s, more with an IS) before it runs
one scenario; a long runner appended to a `thread-count="1"` block stretches that block for everyone; a feature that
keeps growing turns its runner into the suite's critical path. This skill makes placement a measured decision, not a
default.

Rules you must obey when changing test layout: `all-in-one-apim/modules/integration-v2/tests-integration/cucumber-tests/CLAUDE.md`
(§10 runner grouping and lexicographic feature order, §13 overlays, `thread-count`, CPU caps).

## Operating principles

- **Measure, don't guess.** A placement decision cites the runner's measured isolated runtime and the current block
  lengths, from runs (`scripts/suite_timings.py`). Spans measured under concurrency include contention; use them for
  the current layout, use isolated runs for the new work.
- **Preference order (cheapest first).** (1) extend an existing feature in an existing runner; (2) a new runner in an
  existing block whose overlay and behaviour already suit it; (3) merge a small, orthogonal overlay into a compatible
  block (how WsProxy and NetworkAccessControl were absorbed); (4) split an oversized runner or feature across lanes or
  blocks; (5) a new block — only when no compatible block exists, with the measured case and lead approval.
- **Consolidation must not change what a test proves.** Before co-locating, read each block's suite comment and runner
  javadoc, verify the claim behind any "keep separate" note (comments can be stale), list every block that runs a shared
  feature, and check that neither overlay changes the other's behaviour. A test that goes green because its config
  silently changed is worse than a slow one.
- **The budget is the suite's, not the block's.** A change is acceptable when the modelled suite time and packing stay
  within budget (no queue over ~3,600 s, efficiency not degraded) — a block that is "only" 10 minutes longer can push the
  whole suite past the target.
- **Verify the placement at runtime.** Run the affected block(s) after the change; for a new or reshaped block, run it
  concurrently with the other heavy blocks (a 4-block focused suite) to confirm stability under contention.

## Procedure

### 0. Scope the change under review
Given a pull request (URL or number), a branch or a diff: list every runner, feature, block, overlay and
`serverFilesToCopy` it adds or changes, and every block whose runner list or `thread-count` it touches (diff the suite
file against the base). These are what you evaluate. If it changes `tests-common/testcontainers/src/main/resources/
nodeapps`, every run you make needs a backend image built from that change (`mvn test` does not rebuild images): build
a private tag from the worktree's `nodeapps` and pass `-Dnode.docker.image.name=<tag>`; remove it afterwards. Work
on a local checkout only: **never push to, comment on, review or otherwise act on the pull request.**

When asked to optimise the whole suite rather than place one change, the scope is every block: work from the hotspots
the metrics show (critical-path and boot-dominated blocks, tail runners, imbalanced lanes, multi-block registrations,
the file-order versus longest-first gap) and propose changes in the preference order below, each modelled on its own.

### 1. Baseline the current layout
Take the baseline from an existing full run, never from a new full local run. In order of preference:
1. **A pull request URL/number or a workflow run id** (preferred): `scripts/fetch_ci_timings.sh <PR | run id> <out dir>`
   downloads the run's `testng-results.xml` (block and runner timings under real CI conditions), the suite file at the
   run's commit, and the job log.
2. **A raw CI job log** (when the reports artefact has expired, ~90 days): block-level timings only, from the
   `BlockLifecycleListener` lines; runner times then come from your isolated runs (step 2).
3. **A local `testng-results.xml` from a full local run** (last resort, only when no CI data exists): state in the
   report that it reflects local conditions (different CPU, concurrency and platform from CI).
Run `scripts/suite_timings.py <testng-results.xml | job log> <suite xml> --features <out dir>/features`: it prints
the deterministic metrics the report pastes (run summary, blocks, top runners, top features, slowest scenarios,
multi-block runners, layout model). Read each candidate block's suite comment, `thread-count`, overlays,
parameters (`apimCpuLimit`, `initBackend`, `bootExternalIdentityServer`, `lifecycleLock`, ordering notes such as
"keep X last").

### 2. Measure the new/changed work in isolation
Run a focused suite holding the candidate block (or a temporary block with only the new runner, `thread-count="1"`,
same overlay and parameters) and read the runner's span from its `testng-results.xml`. Report the isolated runtime per
runner and per feature file (the Cucumber timestamps of the first and last scenario give per-feature time). If a feature
or runner is growing, compare with its previous span.

### 3. Model the candidates
For each placement option, write a what-if file (`<block> <runner> <seconds>`, `<block> thread-count <n>`,
`<block> remove <runner>`, `<block> drop`, `<block> position <n>`; a new block name pays the boot cost) and run
`suite_timings.py ... --what-if <file>`. The modelled suite time is file-order dispatch, which is what TestNG runs (it
matched CI within 0.3%); the longest-first figure beside it is the best order for the same blocks, so a gap between
them is a reorder to propose, and any block a change adds or lengthens must be placed in the file where the file-order
model says it costs least. Compare modelled suite time, the fullest queue and packing efficiency. See `references/layout-model.md` for the model and its limits, and
`references/consolidation-playbook.md` / `references/splitting-playbook.md` for what makes an option valid.

### 4. Check compatibility of the chosen option
Overlay orthogonality (TOML tables and keys, j2 templates, arrays that replace defaults), server-global state and
restarts (`thread-count` must be 1 if anything restarts or flips global config), workload type (CPU-bound runners at low
`thread-count`, wait-bound runners can share lanes), lifecycle locks and fixed aliases (browser blocks, external-IS
notification alias), ordering constraints, shared feature files run by other blocks, tenant/actor provisioning the
block needs (`initTenantUsers`, `initSecondaryUserStore`, `emailUserMode`).

### 5. Apply and verify
Make the change in the suite (and overlays/runners/feature splits), regenerate the coverage tree if features or runners
changed (`render_coverage_tree.py`, require `invalid: 0` and `not-executed: 0`), run the affected block(s), then a
4-block focused run with the heaviest neighbours. Re-run `suite_timings.py` on the result.

### Running blocks locally
Runs are focused suites of complete blocks, from `all-in-one-apim/modules/integration-v2`:
`mvn -o -B test -pl tests-integration/cucumber-tests -am -Dsurefire.suite.xml=<suite>` (`-am` is required: without it
stale upstream modules are used; `test`, never `install`, so the product image is not rebuilt). On macOS with Colima
export first: `DOCKER_HOST=unix://$HOME/.colima/default/docker.sock`,
`TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`, `TESTCONTAINERS_HOST_OVERRIDE=localhost`,
`TESTCONTAINERS_RYUK_DISABLED=true`. A fresh worktree lacks the generated IS trust material: copy
`tests-common/testcontainers/target/is7` from a built checkout before running external-IS blocks.

**Contention.** Before and during every timed run, check what else runs on the Docker host (`docker ps`, other Maven
processes). A span measured while another block or agent ran is contaminated: label it so in the report, never use it
as an isolated runtime, and re-measure after the host is quiet if the decision depends on it. Never stop containers or
processes you did not start. Local spans (macOS, Colima) are not CI spans: compare local with local, and CI with CI.

### 6. Report
Use `references/report-template.md` exactly: the verdict table, the tool's metrics pasted verbatim (baseline and the
recommended layout's what-if comparison), measured runtimes, hotspots, one proposals table (each with its own
modelled before/after, validity and verification status), compatibility evidence, runtime verification and residual
risk. For long runs keep a status note with the phase and an ETA as a clock time.

## Gate criteria (when used by test-coverage)

The layout change is acceptable when:
- the new/changed runners' isolated runtimes are measured and reported;
- the chosen placement is the cheapest valid option in the preference order, or the report shows why cheaper options
  are invalid (with evidence, not assertion);
- no new block is added without the measured case and lead approval;
- the modelled suite time does not exceed the budget and packing efficiency does not drop materially (state both
  before/after); no single block exceeds ~3,600 s;
- the affected blocks ran green after the change.

## Scope boundaries

One layout question per invocation. Never stage or commit. Delete every temporary focused suite (`zz-*.xml`) and scratch file
the invocation created before finishing, and confirm with `git status --porcelain`. Do not run the full suite without the developer's consent;
focused suites are the default. Follow the repository's lock or concurrency convention for Docker and Maven runs.

## References
- `references/layout-model.md` — lanes, block length, queue packing, budget, boot and resource costs, model limits
- `references/consolidation-playbook.md` — how to merge blocks/overlays safely (WsProxy, NetworkAccessControl examples)
- `references/splitting-playbook.md` — splitting runners, features and blocks by time; ordering and fixtures
- `references/report-template.md` — the consultant report
- `scripts/fetch_ci_timings.sh` — fetch a PR's or run's testng-results.xml, suite file and job log from CI
- `scripts/suite_timings.py` — measured block/runner timings and the 4-queue model with what-if layouts (also reads
  a job log for block-level timings)
