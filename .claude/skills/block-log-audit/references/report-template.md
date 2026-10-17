# Block log-audit report template

One report per block. Markdown. IDs are prefixed with the block label so several blocks' reports can be merged
(`publisher-A1`, `email-username-gateway-E2`). Sections I–K are present only when the invocation included fixing.

```
# Log audit — block <label> (IntegrationV2-<TestName>), <run identity: commit/branch + date + CI or local>

Runners / features: <runner → feature list>. Scenarios run: <n>. Failures in this block: <n or none>.
Unique ERROR/WARN after startup: <n> (<n> test-side). Startup noise: <one line, see benign catalogue>.

## 0. Failures in this block            (omit if none)
## A. Assertions that pass for the wrong reason
## B. Coverage gaps
## C. Resource leaks
## D. Test hygiene (cleanup re-deletes, literal placeholders, duplicate registration, drift)
## E. Test-side robustness (races, missing readiness gates, retries, external dependencies)
## F. Product bugs
## G. Expected / benign
## H. Open questions
## I. Fixes implemented and runtime verification      (when fixing)
## J. Diff                                             (when fixing)
## K. Cleanup performed                                (when the audit created Docker objects, files or processes)
## L. Skill feedback                                   (only when asked to evaluate the skill)
```

## Section rules

**0. Failures.** One subsection per failing scenario row: the failing step, the assertion message verbatim, the full
causal chain with log lines on both sides (test step → request → gateway/REST → product → response), and the
classification: test bug, product bug, environment, or timing. Prove environment/timing claims with a re-run.

**A–F.** One table per section:

| # | Where (feature:line / class:line) | What happens (with log evidence) | Root cause | Proposed fix / status |
|---|---|---|---|---|

- *Where*: the feature line and Examples row, plus the step/helper `Class.java:line` that matters.
- *What happens*: the exact log lines with timestamps (server clock or test clock — say which) and the asserted
  outcome. Quote, do not paraphrase, the decisive line.
- *Root cause*: the mechanism, with code or bytecode evidence. "Probably" is not allowed here; move it to H.
- *Proposed fix / status*: a fix that relaxes nothing and adds no static sleep, and how a run would verify it (which
  line disappears/appears). Mark items already catalogued (`same as known-product-issues PI-3`) or already fixed.

**F** additionally states whether a sound test-side mitigation exists. A product bug is never "fixed" by tolerating
it in an assertion.

**G** is one table:

| Message (normalised) | Count | Cause / why harmless or which assertion covers it |
|---|---|---|

Every row must say why it is harmless; "expected" alone is not a reason. For the server side of an asserted
negative, name the assertion that pins it.

**H** is one table: what could not be proven, and the evidence that would settle it (which probe, log, or bytecode).

**Empty sections**: write `None found` and one line on what was checked (e.g. "no `may leak` WARN in the test log;
cleanup sweep logged 14 deletes, all 2xx/404").

**I. Fixes.** Per fix: files changed (summary), the run that verifies it (Tests run / Failures / Skipped, log path),
the log evidence before and after, and for strengthened assertions the mutation that proved they can fail. For races,
the number of runs and their results.

**J. Diff.** `git diff --stat` and the full diff of the change.

**K. Cleanup.** Docker objects, temp files and processes the audit created, and their removal; lock released.

## Quality bar

A reviewer must be able to act on every row without repeating the audit: concrete file:line, verbatim log lines with
timestamps, the mechanism, and a fix with its verification. Reconcile the header's counts with the sections: every
unique message is in exactly one section.

**L. Skill feedback** (only when asked to evaluate the skill) is one table:

| # | Kind | Skill file / section | What happened | Proposed edit (exact text or diff) |
|---|---|---|---|---|

Kinds: `unclear`, `missing`, `wrong`, `impractical`, `tool defect`, `catalogue helped`, `catalogue misled`,
`worked well`. Every row except `worked well` and `catalogue helped` has a concrete proposed edit.
