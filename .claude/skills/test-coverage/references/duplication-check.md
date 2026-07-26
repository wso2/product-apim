# Duplication check (run BEFORE writing any integration artifact)

The #1 problem in this suite is duplicate tests and duplicate steps. Do this pass first — as a senior QA would.

## Before a new STEP definition
1. Grep the glue for the action and for likely phrasings:
   `grep -rniE "<verb>.*<noun>" tests-integration/cucumber-tests/src/test/java/.../stepdefinitions/`
   (e.g. "subscribe", "throttl", "create.*api", "invoke.*gateway").
2. If a step does *almost* what you need → **extend it** (add a parameter / a variant), never add a near-copy.
3. If a request-making step is missing → funnel through `APIInvocationSteps.execute(...)` (clears→calls→sets→
   returns `httpResponse`); do NOT hand-write `TestContext.set("httpResponse", ...)` (re-introduces the
   stale-response trap).
4. Only write a brand-new step when nothing reusable exists. Match the surrounding naming/idiom.

## Before a new FEATURE file
1. Check the coverage tree + grep `features/**` for the capability. If a file's `@cap` fits and it's a coherent
   home → **add your scenario there** (shared `Background`, sibling scenario).
2. New file only when nothing fits — and justify it in the plan.

## Before a new BLOCK / `<test>` in testng-v2.xml
- A new block = a new container = real cost → lead-approval gate (SKILL.md Phase 4). Strongly prefer adding the
  scenario to an existing block whose fixtures/overlay already fit.

## Reuse checklist (quick)
- [ ] searched glue for a reusable/extendable step
- [ ] searched features for an existing home (right `@cap`)
- [ ] using shared naming/`TestContext`/`Identity` utils (no hardcoded names)
- [ ] `ResourceCleanup.register(...)` for every created resource
- [ ] exact-value assertion (no `a || b`)
- [ ] ×2 tenant via Scenario Outline where the flow is tenant-sensitive
