# Gap analysis (Phase 2)

For each ledger row, decide whether it's already covered and — if not — where a new test belongs.

## Unit track (straightforward)
1. Locate the existing test class for the touched class — convention `<Class>Test` in the same module
   (`src/test/java/...` mirroring `src/main/java/...`).
2. Read it: which changed methods/branches already have assertions? Which don't?
3. A changed method/branch with no assertion covering the new behavior = a unit gap.
4. Prefer EXTENDING that existing test class over a new one.

## Integration track (structured — tree for placement, feature files for truth)
The capability tree is the **organization/comprehension map**, not a coverage oracle. Use it to decide *where a
flow belongs*; use the actual feature files to decide *what's already covered*.

1. **Place the flow in the tree.** Open `docs/devs/capability-map.yml` (closed `@cap`/`@feat` vocabulary) and
   `docs/devs/v2-public-feature-coverage-map.md`. Find the `@cap`/`@feat` the flow belongs under. If nothing fits
   → a NEW `@cap`/`@feat` is needed (Phase 4 lead-approval gate).
2. **Read the owning feature file(s).** Grep `features/**` for the capability + related steps. Read the scenarios
   — do NOT judge coverage from tag names. Decide per flow:
   - **covered** — an existing scenario already asserts this exact behavior (incl. ×2 tenant). Nothing to add;
     note it.
   - **partial** — the area is tested but this case/assertion/tenant is missing. Extend the existing scenario or
     add a sibling scenario in the SAME file.
   - **absent** — no scenario exercises it. New scenario; extend an existing feature file if one fits its `@cap`,
     else a new file (justify).
3. Record the exact file + scenario pointers for each verdict (the plan needs them).

## Coverage as post-hoc measurement (NOT the gap oracle)
`-Dapim.coverage=true` quantifies what the tests you IMPLEMENT actually exercise. Use it in Phase 6/7 to report
the delta and re-evaluate — not to decide what to write in Phase 2.

## Output — the gap report
- Unit gaps: class → methods/branches missing assertions.
- Integration gaps: flow → {covered | partial | absent} + owning feature file/scenario + suggested placement.
