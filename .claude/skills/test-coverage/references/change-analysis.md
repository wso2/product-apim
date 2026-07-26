# Change analysis (Phase 1)

Turn the change-set into a **test-worthiness ledger**: one row per semantic unit of change, with the recommended
test type(s) and why.

## Get the diff
- Local branch: `git -C <repo> diff <base>...<head>` (or the PR's merge-base).
- Remote-only repo: `gh pr diff <url>` (plan-only for that repo).
- Prefer the merge-base diff (`...`) so you see only what the PR adds, not unrelated main drift.

## Decompose into semantic units
For each hunk, identify what actually changed (not just line-level):
- new/modified **public method / signature** (unit candidate)
- new/changed **REST resource, endpoint path, or request/response contract** (integration)
- new/changed **config or TOML key** whose effect is runtime (integration)
- new/changed **DTO** that crosses the wire (integration)
- **gateway/Synapse handler**, mediation, throttle, auth, or data-plane logic (integration)
- **DB / cross-component** interaction (integration)
- **validator / calculator / parser / branch logic** with no I/O (unit)
- **docs-apim** page describing intended behavior (spec → assertions)

## Classify each unit → {unit, integration, both, doc-spec}
Apply the SKILL.md rubric. When "both", note the pairing (unit pins the core value; integration proves the
boundary enforces it).

## Persona-specific extraction
- **Feature:** for each new capability, mine the linked docs-apim additions for the *intended* behavior and turn
  it into candidate assertions (exact status codes, exact response fields). Flag anything the docs assert that you
  can't yet confirm in code — that's a verify-later item, not a silent assumption.
- **Patch:** find the **root cause** and the **behavioral delta** — precisely "what input previously produced the
  wrong output, and what it should produce now". That delta IS the regression test. Note the fix's parent commit
  (for the fail-on-parent / pass-on-fix proof in Phase 5).

## Output — the ledger
| unit of change | repo | kind (unit/int/both/doc) | intended behavior / regression to pin | notes |
Keep it tight; this feeds Phase 2 (gap analysis) and Phase 3 (plan).
