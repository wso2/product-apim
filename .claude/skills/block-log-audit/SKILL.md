---
name: block-log-audit
description: >-
  Audit the server (carbon) and test-side logs of one integration-v2 test block after a run: account for every
  ERROR/WARN, attribute it to the scenario that caused it, and root-cause it from code, logs and product bytecode.
  Finds assertions that pass for the wrong reason, coverage gaps, resource leaks, cleanup/hygiene defects,
  readiness races and product bugs that a green run hides. Mandatory gate for new or changed integration tests
  (test-coverage Phase 6); also use to audit a CI run block by block.
---

# Integration-V2 Block Log Audit

A green block is not proof that its tests test what they claim. A scenario can pass because the product rejected the
request for an unrelated reason, because a helper accepts any error, or because a poll eventually succeeded after a
race; cleanup can leak or re-delete; a fixture can reach a real external host. These defects are visible in the
block's logs long before they cause a flaky failure. This skill accounts for every ERROR and WARN a block produced
and turns each into one of: expected and correctly asserted, benign with a stated reason, or a finding with a root
cause and a fix.

Audit **one block per invocation** (one `<test name="IntegrationV2-...">` in the suite file that ran:
`testng-v2.xml` for all-in-one, `testng-v2_distributed.xml` for distributed). Rules you must obey
when proposing or making test changes: `all-in-one-apim/modules/integration-v2/tests-integration/cucumber-tests/CLAUDE.md`.

## Operating principles

- **Dig until the root cause is proven.** Time and tokens are unlimited; a quick pass is not an audit. A failure or
  finding is "open" (section H) only after every available source has been read for it: the failing resource's
  identity traced through the test log and every server/container log of the block, the step and helper code, the
  product source at the shipped version and the bytecode on any stack trace. "Unresolved" with unread evidence left
  is an incomplete audit, not a result.
- **Evidence or it did not happen.** Every claim cites the exact log line (with timestamp) and file:line, or product
  bytecode (`javap -c -p -l`). A plausible story is not a root cause. If evidence is missing, the item is an open
  question (section H) with the evidence that would settle it, never a finding.
- **Count everything, explain everything.** Every distinct normalised ERROR at any time and WARN after startup ends
  up in exactly one section. Startup WARN noise is catalogued separately. Silence is not a verdict: "no ERROR in the
  block" is checked, not assumed.
- **Asserted is not the same as expected.** A server ERROR is "expected" only if a step deliberately provokes it AND
  the scenario asserts the outcome that proves it, and the asserted outcome could not have come from another cause.
- **Fix the cause, never the symptom.** Never relax an assertion, never reduce coverage, no static sleeps (poll a real
  signal), no retry that re-sends a deterministically rejected or non-idempotent request, no recovery-as-fix.
- **Audit the complete block, as the suite runs it.** Every run the audit relies on (the run audited and every
  verification run) executes the block's whole `<test>` element verbatim: all of its runners, its `thread-count`, its
  runner order and its parameters/overlays. Never a subset, never only the new or changed runner, never a temporary
  single-runner block (that is for timing in `suite-layout-consultant`, not for this audit). Many defects exist only
  in the full block: runners on parallel lanes sharing one server (leaks one runner leaves that another trips over,
  name collisions, tenant-global config changed under a sibling), cleanup that deletes a sibling's shared fixture,
  ordering dependencies ("keep X last"), restarts or global flips that a sibling observes, and load-dependent races.
  When the block's own behaviour depends on its neighbours (4-lane CPU contention, the shared node backend), add a
  concurrent run with the heaviest neighbouring blocks.
- **A fix is not done until a run proves it.** Re-run the complete block; show the log line that disappeared or appeared and
  the block result. For a race, run it more than once. For an assertion strengthened against a wrong-reason pass,
  prove it can fail (mutation, see `references/evidence-and-pitfalls.md`).

## Inputs

1. The run's full Maven/CI log. Split it per block with `scripts/split_block_logs.py <raw log> <suite xml> <out dir>`
   (`<label>.carbon.log`: APIM container lines; `<label>.test.log`: the block's TestNG-thread lines;
   `<label>.is.log`: the block's external IS, attributed best-effort — verify by content). A distributed block
   (`testng-v2_distributed.xml`) has no `<label>.carbon.log`: each of its containers gets `<label>.<container>.log`
   (`apim-cp` control plane, `apim-gw` gateway, `apim-tm` traffic manager, `mysql`); a block's own extra containers
   (e.g. `gw-ctrl`/`gw-rt`) are split the same way. Use the raw log; the script strips ANSI with an exact pattern.
2. The code at exactly the revision that ran (a worktree at that commit plus any uncommitted content that ran).
3. The product binaries of the image that ran (OSGi bundles, REST webapps, default `conf/`), for bytecode evidence.
4. Known causes: `references/known-product-issues.md` and `references/benign-catalogue.md` — cite their IDs instead
   of re-deriving, but re-verify that the log matches the catalogued signature before citing.

## Procedure

### 1. Inventory
Run `scripts/digest_block_log.py <label>.carbon.log <label>.test.log` to normalise (UUIDs, `_<hex8>_<n>` unique
suffixes, numbers, timestamps) and count every ERROR at any time and WARN after server startup, with first/last
timestamps. For a distributed block run it once per
container log (the test log with any one of them); a request crosses containers (deploy on `apim-cp`, route and
handshake on `apim-gw`, throttling on `apim-tm`), so follow each failing resource through all of them. A block that
boots an external IS also has `<label>.is.log`: digest it too (same `WSO2 Carbon started in` startup boundary) and
count its post-startup ERROR/WARN with the others; a federated or key-manager failure often shows only there. Startup
WARN noise (catalogued in `references/benign-catalogue.md`) is listed once and set aside; an ERROR is a finding even
before startup completes. Read the block's definition in the suite file that ran (parameters, overlays, runners, any
XML comment explaining why it is separate) and each runner's
`@CucumberOptions(features = ...)`.

### 2. Attribute
Tie every distinct message to the scenario, Examples row/actor and step that caused it. Other blocks run
concurrently and their Cucumber output is interleaved: attribute by resource identity (`${UNIQUE:...}` suffixes, API
contexts, app names, tenant), not by time alone. Test-thread and container timestamps are both available; state
which clock you aligned. See `references/evidence-and-pitfalls.md` §Attribution.

### 3. Classify
Put each message (or group with one root cause) in exactly one class — `references/defect-patterns.md` has the
recurring patterns of each:
- **A. Assertion passing for the wrong reason** (highest priority): the asserted status/body was produced by a
  different cause than the scenario claims.
- **B. Coverage gap**: the scenario does not assert what its title/description claims, or a documented behaviour is
  left untested (status-only negative, substring where an exact code is needed, scope limits).
- **C. Resource leak**: something created is never removed, or cleanup cannot remove it.
- **D. Test hygiene**: cleanup re-deletes, duplicate registration, literal unresolved placeholders, comment/config
  drift.
- **E. Test-side robustness**: polls that start before a deploy/route/key is live, missing readiness gates, retries
  of deterministic or non-idempotent failures, fixtures contacting real external hosts, order dependence.
- **F. Product bug**: the product misbehaves (NPE, swallowed exception, cache race, wrong status). Prove it from
  bytecode plus the log sequence; state whether a sound test-side mitigation exists without relaxing anything.
- **G. Expected / benign**: one table with message, count and the reason it is harmless or asserted.
- **H. Open questions**: what could not be proven and what evidence would prove it.

### 4. Root-cause
For every non-benign item: read the step definition and helper, the feature lines, the cleanup path
(`utils/ResourceCleanup.java`), the readiness/heal gates, the block's overlays, and the product bytecode on any stack
trace. State the causal chain end to end (step → client → gateway/REST → product class → log line).

### 5. Fix and verify (when the invocation includes fixing — always under test-coverage)
Implement each test-side fix you are confident about, minimally and in the style of the surrounding code; comments
state what the code IS, never history. Product bugs: propose, never patch the product. Re-run the block; record Tests
run/Failures and the log evidence per fix. If a feature file changed, regenerate the coverage tree
(`python3 all-in-one-apim/modules/integration-v2/docs/devs/render_coverage_tree.py`, require `invalid: 0` and
`not-executed: 0`).

### 6. Report
Use `references/report-template.md` exactly: per-block header with counts, sections 0/A–H (and I–K when fixing),
tables with block-prefixed IDs (`<label>-A1`), file:line and timestamped log evidence in every row, "None found" plus
what was checked for empty sections.

## Gate criteria (when used as the test-coverage Phase 6 gate)

The block passes the gate only when, for the scenarios added or changed by the work under review:
- no item in A, B, C or D, and no E item without a readiness/robustness fix;
- every ERROR/WARN they produce is in G with a verified reason, or is a catalogued product issue (F) the change does
  not make worse;
- every new negative assertion is exact (status AND error code/message field where the product provides one) and
  has been shown able to fail;
- the cleanup sweep logs no WARN indicating a failed or unverified delete, deregistration, or unsubscribe, including
  `may leak`, `may have leaked`, `Cleanup failed to delete`, `Failed to delete ... on the external IS`, `cannot delete`,
  `deleteUser ... returned HTTP`, `deregister ... returned HTTP`, `Cleanup failed to deregister`, `STILL EXISTS`,
  `NOT deleted`, an IS sweep skipped despite registered resources, a WebSub unsubscribe or removal that failed, or
  persisted WebSub rows that remain, differ, or could not be verified after unsubscribe, and the server logs no
  not-found ERROR from cleanup re-deletes;
- the complete block (every runner, verbatim `thread-count`, order and parameters) ran green after the fixes, with
  the run cited in the report; a run of a subset of the block does not count.
Pre-existing findings in untouched scenarios are reported but do not block; list them separately.

## Scope boundaries

Do not run the full suite from this skill; one block (or a focused suite of the blocks under review) at a time.
Never stage or commit; leave changes for the developer. Delete every temporary focused suite (`zz-*.xml`) and scratch file
the invocation created before finishing, and confirm with `git status --porcelain`. On a shared Docker host, never prune, restart the engine, or
stop containers/processes you did not start. Follow the repository's lock or concurrency convention for Docker and
Maven runs.

## References
- `references/report-template.md` — the report format and table schema
- `references/defect-patterns.md` — recurring A–F patterns, how to detect each, and the proper fix
- `references/benign-catalogue.md` — startup noise and messages that are harmless, with the conditions that make them so
- `references/known-product-issues.md` — established product bugs with signatures (re-verify before citing)
- `references/evidence-and-pitfalls.md` — attribution, bytecode reading, running a block, and traps that silently
  produce wrong conclusions
- `scripts/split_block_logs.py` — split a run log into per-block carbon/test/IS logs
- `scripts/digest_block_log.py` — normalised ERROR/WARN inventory with counts and first/last timestamps
