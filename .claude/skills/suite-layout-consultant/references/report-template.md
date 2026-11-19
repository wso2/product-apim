# Suite layout consultant report template

Every report has these sections, in this order, with these tables. Sections 2–4 are **pasted verbatim** from
`scripts/suite_timings.py` output (the tool's section numbers are kept inside them) — never retyped or summarised — so
reports from different runs and developers are directly comparable. Judgement lives in sections 5–9. A section with
nothing to report says `None.` and one line on what was checked. IDs are stable within the report (`P1`, `P2`, … for
proposals; `H1`, … for hotspots).

```
# Suite layout — <PR or change under review>, <date>

## 1. Verdict
## 2. Baseline metrics (current layout)
## 3. Measured runtimes of the work under review
## 4. Recommended layout — model and comparison
## 5. Hotspots
## 6. Proposals
## 7. Compatibility evidence
## 8. Runtime verification
## 9. Residual risk
## 10. Skill feedback            (only when asked to evaluate the skill)
```

## 1. Verdict

| Field | Value |
|---|---|
| Change under review | PR URL / commit, and what it adds or grows (runners, features, blocks, overlays) |
| Baseline source | PR run id + commit (CI), or job log (block level), or local results (state "local conditions") |
| Current suite time | measured wall span and modelled suite time (from section 2) |
| Recommended layout suite time | modelled suite time (from section 4) |
| Change | seconds and % (negative = faster) |
| Boots | before → after |
| Within budget | yes/no: fullest queue ≤ budget, no block > ~3,600 s, efficiency not degraded |
| Decision needed | none / lead approval for a new block / developer choice between proposals |
| Confidence | high / medium / low, with the reason (measured vs modelled, single run vs repeated) |

Then at most five lines on why.

## 2. Baseline metrics (current layout)

Paste `suite_timings.py <baseline> <suite> --features <dir>` output sections 1–7 (run summary, blocks, top runners, top
features, slowest scenarios, multi-block runners, layout model).

## 3. Measured runtimes of the work under review

| Runner / feature | Source (CI run, isolated local run + log path) | Runs | Span (each run) | Scenarios | Mean per scenario | Note |
|---|---|---:|---|---:|---:|---|

Every runner whose placement a proposal changes appears here with an isolated measurement or the reason none is
needed (e.g. its CI span in an uncontended position).

## 4. Recommended layout — model and comparison

The what-if file used (verbatim, in a code block), then the tool's section 8 output pasted verbatim (what-if queues,
per-block before/after, suite before/after).

## 5. Hotspots

What the metrics show, each tied to its number in section 2:

| # | Kind | Where | Metric (from section 2) | Why it matters |
|---|---|---|---|---|

Kinds: `over-budget block`, `critical-path block` (in the fullest queue), `boot-dominated block`, `tail runner`,
`imbalanced lanes`, `oversized runner`, `oversized feature`, `slow scenario` (mean per scenario far above its feature's),
`multi-block registration cost`, `queue imbalance`.

## 6. Proposals

One table, one row per proposal, cheapest first in the skill's preference order:

| ID | Type | Change | Blocks affected | Block time before → after | Suite time before → after | Δ suite | Δ boots | Fixes hotspots | Validity | Status |
|---|---|---|---|---|---|---:|---:|---|---|---|

- *Type*: `extend feature/runner`, `move runner` (to an existing block), `consolidate blocks`, `merge overlay`,
  `split runner`, `split feature`, `thread-count`, `reorder runners`, `reorder blocks`, `new block`.
- *Block/suite times*: from a what-if run of **that proposal alone**; the combined effect appears in section 4.
- *Validity*: `valid` (all compatibility checks pass, section 7), `invalid` (which check fails), `unverified`.
- *Status*: `modelled` → `measured` (isolated runtimes) → `verified-block` (affected blocks ran green) →
  `verified-concurrent` (also ran green beside the heaviest blocks). Only `verified-*` proposals go in the recommended
  layout unless the verdict says otherwise.

Rejected cheaper options stay in the table as `invalid`, with the failing check named — never silently dropped.

## 7. Compatibility evidence

One sub-table per proposal that moves, merges or co-locates anything (the consolidation playbook's checklist):

| Check | Result | Evidence |
|---|---|---|
| Overlay orthogonality (keys, arrays that replace defaults, loud keys, j2, serverFilesToCopy) | pass/fail | file:line |
| Block subject not weakened (suite comment and runner javadoc read; claim verified) | pass/fail | quote + verification |
| Shared features (every block running them) | pass/fail | list |
| Server-global state, restarts, ordering constraints | pass/fail | |
| Infrastructure parameters (backend, proxy, tenants, secondary store, IS, emailUserMode, locks) | pass/fail | |
| Isolation inside the block (names, tenant-global config, actors) with its thread-count | pass/fail | |
| Node backend image (rebuilt when nodeapps changed) | pass/fail/n.a. | |

## 8. Runtime verification

| Run | Suite file (layout) | Blocks | Concurrency | Tests run / Failures / Skipped | Wall time | Log |
|---|---|---|---|---|---|---|

Include the affected blocks' complete runs and the concurrent run with the heaviest neighbours; compare measured spans
with the model (section 4) and state the model error.

## 9. Residual risk

The model's limits that matter here (contention, single-run variance, local vs CI, CI's file-order dispatch), what the
next CI run must show (which blocks' spans, which queue), and any proposal deferred and why.

## 10. Skill feedback (only when asked to evaluate the skill)

| # | Kind | Skill file / section | What happened | Proposed edit (exact text or diff) |
|---|---|---|---|---|

Kinds: `unclear`, `missing`, `wrong`, `impractical`, `tool defect`, `worked well`. Every non-`worked well` row has a
concrete proposed edit.
