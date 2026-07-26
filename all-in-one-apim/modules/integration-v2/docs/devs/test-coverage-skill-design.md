# Test-Coverage Authoring Skill — Design (draft v2)

A Claude Code skill that turns *source changes* into *verified tests* (unit + integration), maximizing
coverage gain and regression protection while minimizing test duplication. Complements — does not replace —
`tests-integration/cucumber-tests/CLAUDE.md` (the authoring rulebook); the skill **operationalizes** those
rules and adds change-driven test selection.

> v2 folds in review decisions (2026-07-06). Rollout plan (§7) is the build order.

---

## 1. Goal & guiding principle

Input: source changes (PRs / branches / diffs) across up to three repos — **carbon-apimgt** (OSGi components
+ unit tests), **product-apim** (distribution + integration tests, incl. this integration-v2 Cucumber suite),
**docs-apim** (behavioral spec, no tests).

Output: merged, verified tests that **improve coverage and harden product flows against regressions**.

Selection rule (cost-aware, coverage-first):
- **Prefer unit tests** where they *fully* capture the behavior — cheap, fast, precise.
- **Escalate to integration** for anything that only manifests through wiring (see the rubric, §4).
- **Complementary, not either/or:** a unit test pins the functional core; an integration test proves the
  product enforces it end-to-end. The plan shows the pairing.
- Cost is a tie-breaker, **never** a reason to skip a regression-critical flow.

## 1a. Two artifacts, two distinct roles (corrected)

- **Capability tree (`capability-map.yml` + `coverage-tree.md` / `v2-public-feature-coverage-map.md`) — the
  ORGANIZATION & COMPREHENSION map.** It exists so any human *or AI* can understand what product capability
  coverage exists today and **decide where a new flow belongs** in the hierarchy. It is **not** a gap oracle.
  **Every skill run keeps it current**: newly-implemented extended flows are added and marked covered, and the
  tree is re-rendered (`render_coverage_tree.py`, `invalid: 0`).
- **JaCoCo `-Dapim.coverage=true` — the QUANTITATIVE re-evaluation marker.** Used *after* the skill implements
  tests, to measure the real coverage delta the new tests produced and re-evaluate them. A number to confirm
  the work landed — not the thing that decides what to write.

So the analysis leans on the **tree for placement/organization** and reads existing feature files/step glue to
judge what's already covered; coverage is the **post-hoc scorecard**.

---

## 2. Two personas (a prior, not two skills)

Flow is ~95% shared; persona tunes emphasis and defaults:

| | **Feature developer** | **Patch developer** |
|---|---|---|
| Nature of change | Additive — new capability/feature/endpoint | Corrective — bug fix / minor improvement on a ticket |
| Spec source of truth | **docs-apim** additions → assertions derive from docs | The **ticket** + the fix's behavioral delta |
| New `@cap`/`@feat`? | Likely → lead-approval gate fires often | Rare → usually maps to an existing `@cap` |
| Plan emphasis | Breadth: new product flows end-to-end + unit-test new logic | Depth: a regression that *would have caught the bug* + adjacent gaps |
| Regression proof | n/a (new behavior) | **Parent-commit proof** (§4) — test fails on the fix's parent, passes on the fix |
| Extra input | Linked docs PR | Ticket (dev pastes text; auto-fetch is a later increment) |

One skill; persona is inferred from change shape (additive vs corrective) and **confirmed in one line**.

---

## 3. Flow (phases)

**Phase 0 — Intake.** Infer+confirm persona. **Ask for local repo locations** (carbon-apimgt / product-apim /
docs-apim) and **store them** so reruns don't re-ask — at **working-directory / session scope** (preferred over
global; if ever global, provide an override). Ingest PRs/branches/diffs (hybrid: use a local checkout where
present so tests can *run*; fall back to `gh pr diff` for repos not cloned → plan-only there). Correlate the
diffs as **one logical change** across repos (config key in carbon-apimgt → default config in product-apim →
doc in docs-apim), not three independent diffs. → normalized change-set manifest.

**Phase 1 — Change analysis.** Parse diff into semantic units; classify each per the rubric (§4). Feature: mine
docs-apim additions → candidate assertions. Patch: extract root cause + behavioral delta → the exact regression
to pin. → test-worthiness ledger.

**Phase 2 — Existing-coverage gap analysis.**
- *Unit track:* find existing `*Test` classes for touched classes; list changed methods/branches with no
  assertions.
- *Integration track:* place each candidate flow in the **capability tree** (organization) and read the owning
  feature file(s)/scenarios to judge covered / partial / absent, with pointers. This is the duplication
  firewall and the "where does it go" resolver.
- → gap report.

**Phase 3 — Plan synthesis.** Prioritize by `coverage-gain × regression-value ÷ cost`, where **cost is
block-level** (§4). Two presentations: unit → coverage-description summary (batch approval); integration →
**flow-by-flow**, each with `@cap`/`@feat` placement · file it *extends* (or new file/block + justification) ·
**steps it reuses** · exact-value assertions · ×2 tenant · new-infra flag. **Opportunistic** (not-from-diff)
gaps are allowed but **quarantined**: clearly labeled, default-off, capped, adjacency-limited. → proposed
`TEST-PLAN.md`, every item individually selectable.

**Phase 4 — Approval loop.** Dev selects + gives feedback; iterate until confident. **Lead-approval gates**
(halt the affected item until approved): (a) new `@cap`, (b) new `@feat` (both edit the closed vocabulary),
**(c) a new `<test>` block / container** (real wall-clock + host-budget cost). Warn against dodging (a)/(b) by
mis-classifying a genuinely-new capability under an existing `@cap`.

**Phase 5 — Implementation (per approved item, honoring CLAUDE.md).**
- *Unit:* extend the existing test class without duplication, in the module's idiom. **Patch persona: prove
  the regression** — check out the fix's parent commit, run the candidate test, confirm it **fails**; check out
  the fix, confirm it **passes**. A test that can't fail on the old code doesn't guard anything.
- *Integration:* full CLAUDE.md discipline — search-before-write, reuse/extend step definitions (never
  near-duplicate), extend existing feature files where one fits, correct folder + `@cap`, isolation / cleanup /
  actor rules, `Copyright (c) 2026`, ×2 tenant, strict exact-value assertions. A senior-QA
  **duplication-minimization pass** precedes every new artifact.
- *Docs (feature):* where a docs-derived assertion diverges from actual behavior, **surface it** (doc bug vs
  impl bug) — never silently encode one (see `feedback_suspicious_scenario_flag_dont_tweak`).

**Phase 6 — Verify.** Minimal first (unit: affected class(es); integration: scratch/minimal testng with just
the new block). **Then ask** before the full local suite. Run the full/affected suite with
`-Dapim.coverage=true` to capture the **coverage delta** as the quantitative re-eval. Blockers (infra gaps like
"needs a real WSO2 IS container") are surfaced and discussed, parked with a documented reason if unresolved.

**Phase 7 — Wrap.** Update `capability-map.yml` + regenerate the coverage tree (mark the new flows covered;
`render_coverage_tree.py` → `invalid: 0`). Report the coverage delta (unit + integration, before/after).

---

## 4. Rubrics & rules (the judgment the skill must encode)

**Unit vs integration — decide by observable properties of the change, not vibe:**
- **Integration** if the change touches: a gateway/Synapse handler or data-plane path · a REST resource /
  endpoint contract · a config/TOML key whose effect is runtime · a DTO crossing the wire · DB or a
  cross-component boundary · multi-actor / multi-tenant behavior · lifecycle/deploy semantics.
- **Unit** if the change is: a pure function, validator, calculator, parser, or branch logic with no I/O.
- **Both** when a computed value (unit) is enforced at a boundary (integration) — e.g. throttle-count calc +
  gateway rejection at the limit. State *why* both.
- **Ambiguous → escalate** (integration) only when a regression would otherwise be unguarded; else unit.

**Cost is block-level.** The unit of integration cost is the **container boot** (minutes + host budget). A
scenario appended to an existing feature/block is near-free; a new block with its own overlay/restart is a
whole container → **lead-approval gate (§4c)**. This reorders priority: *extend an existing block* dominates
*new block* even when a new block is cleaner.

**Regression proof (patch).** Parent-commit fail / fix pass, as in Phase 5. Gold standard; cheap for units.

---

## 5. Failure modes to guard against
- Hallucinated placement in the tree → mitigate by reading the actual owning feature files, not just tag names.
- Duplicate tests despite the rule → mandatory grep of glue + confirm the flow isn't already exercised.
- Over-mocked unit tests that pass but don't protect → the parent-commit fail check kills these.
- Plan bloat / scope creep → opportunistic quarantine (§3, Phase 3).
- `@cap`-gate gaming → explicit warning (§3, Phase 4).
- Docs ahead of impl → surface divergence (§3, Phase 5).

## 6. Skill structure
```
skills/test-coverage/
  SKILL.md                     # personas + 8 phases + gates + rubrics
  references/
    change-analysis.md         # diff → semantic-unit classification
    gap-analysis.md            # unit-gap + capability-tree placement procedure
    duplication-check.md       # pre-write grep/search discipline
    plan-template.md           # TEST-PLAN.md shape
  (points at) CLAUDE.md, capability-map.yml, coverage docs
```
Repo locations persisted at working-dir/session scope. Heavy read phases may fan out to sub-agents
(change-analyzer per repo, coverage-gap agent, implementer per item).

---

## 7. Rollout plan (incremental, dogfood-driven)

Risk is concentrated in analysis/plan quality, not orchestration. So build **read-only analysis first**,
validate it on our *own* recent diffs, and only then add the write path. Each increment: **build → dogfood on a
real diff → observe → tweak references/rubric/assumptions → lock → next.**

- **Inc 0 — Scaffold + repo-location memory.** SKILL.md skeleton; persona infer+confirm; ask+store repo
  locations (working-dir/session, override). *Test:* run twice — second run doesn't re-ask; override works.
- **Inc 1 — Change + gap analysis (READ-ONLY, integration-only, no writing).** *Dogfood:* feed it this
  session's GraphQL work as the "PR." *Output:* ledger + gap report + draft TEST-PLAN.md. *Observe:* does it
  place flows correctly in the tree, find existing coverage, avoid hallucination/duplication? **This is the
  make-or-break gate — do not proceed until the analysis is trustworthy.**
- **Inc 2 — Plan synthesis quality.** Unit-vs-integration rubric, block-level cost/priority, opportunistic
  quarantine, the two presentations. Still no writing. *Dogfood:* one feature-shaped and one patch-shaped diff;
  refine until the plan reads like a senior QA wrote it.
- **Inc 3 — Approval loop + gates.** Selection/feedback loop; lead-approval gates for new `@cap`/`@feat`/block.
  *Test:* a diff that needs a new cap correctly halts at the gate.
- **Inc 4 — Integration write path.** Implement ONE approved item end-to-end per CLAUDE.md with the dedup
  pass; verify minimal. *Observe:* does it reuse steps / extend files rather than duplicate?
- **Inc 5 — Verify + coverage re-eval + tree update.** Wire minimal→(consent)→full with `-Dapim.coverage=true`;
  regenerate the tree + covered markers; report the delta. *Dogfood:* implement an item, show before/after.
- **Inc 6 — Unit write path + patch parent-commit proof.** Only where carbon-apimgt is checked out+buildable
  (hybrid). Add the fail-on-parent / pass-on-fix regression check.
- **Inc 7 — Feature-persona docs-apim spec mining.** Docs-derived assertions + doc/impl divergence surfacing.
  Optional: patch ticket auto-fetch.

Stop points are real: if Inc 1 analysis isn't trustworthy, we iterate there before spending on the write path.
