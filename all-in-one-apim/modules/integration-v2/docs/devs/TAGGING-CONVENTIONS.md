# Tagging conventions

The detailed reference behind the short rules in
[`cucumber-tests/CLAUDE.md`](../../tests-integration/cucumber-tests/CLAUDE.md). Tags are how every
scenario is placed in the coverage tree and validated. The closed vocabulary for `@cap`/`@feat` lives
in [`capability-map.yml`](capability-map.yml).

## The tags

| Tag | Syntax | Cardinality | Required | Purpose |
|-----|--------|-------------|----------|---------|
| `@cap` | `@cap:<id>` | exactly one | yes | The capability **under test** — the subject of the assertions |
| `@feat` | `@feat:<id>` | exactly one | yes | Feature under that capability |
| `@rule` | `@rule:<slug>` | 0–1 | no | Free-text sub-group within a feature |
| `@type` | `@type:<v>` | 0+ | no | Test nature: `smoke` \| `negative` \| `regression` |
| `@dep` | `@dep:<cap>` | 0+ | no | A cross-capability **prerequisite** (not coverage) |
| `@legacy` | `@legacy:<Class>` | 0+ | no | Legacy TestNG class this scenario replaces |

Exclusion markers (mutually exclusive with the above; mark non-product scenarios):
`@infra` · `@framework` · `@migration` · `@setup`.

IDs are lowercase kebab-case. `@cap`/`@feat` values must exist in `capability-map.yml`.

## `@cap` — exactly one

Pick the capability the scenario **asserts against**, not everything it touches. If you can't choose a
single `@cap`, the scenario is doing two things — split it. The folder a feature file sits in is just
its physical home; `@cap` is the source of truth, so a file under `publisher/` may legitimately be
`@cap:gateway` if that's what it tests.

## `@feat`

A feature id defined under the chosen `@cap` in `capability-map.yml`. The pair `(@cap, @feat)` must
resolve to a real node, or lint fails.

## Feature-level vs scenario-level tags

Gherkin **inherits** `Feature:`-level tags onto every scenario in the file. So:
- If all scenarios in a file share one `(@cap, @feat)`, set it once on the `Feature:`.
- If scenarios have **different** capabilities, do **not** set `@cap`/`@feat` on the `Feature:` — tag
  each scenario, or you'll get "multiple @cap" (feature value + scenario value) and lint will fail.

## `@rule` — free text

Ad-hoc grouping inside a feature, e.g. `@rule:revocation`, `@rule:negative-inputs`. Not validated
against the skeleton; use it to cluster related scenarios in the rendered tree.

## `@type`

A **selection** axis, orthogonal to what's being tested:
- `smoke` — fast happy-path "is it basically working" check (candidate for a quick CI gate).
- `negative` — error/rejection paths (400/401/403, malformed input).
- `regression` — pinned to a specific past bug (pair with `@legacy:` / a bug id).

## `@dep` vs coverage — the important distinction

`@dep` declares that a scenario **needs** another capability to function — it does **not** claim to
test it. A gateway throttling-enforcement scenario needs an admin-created throttling policy, but it
asserts nothing about admin's policy CRUD:

```gherkin
@cap:gateway @feat:throttling-enforcement @dep:admin @type:regression @legacy:JWTRequestCountThrottlingTestCase
Scenario: Requests beyond the subscription tier limit are throttled
```

This keeps coverage honest: admin's throttling-policy coverage must come from real `@cap:admin` tests,
never borrowed from a gateway test. It also documents setup ordering and enables an impact view
("change the admin throttling API → these gateway scenarios are at risk").

**Guardrail:** only tag *non-obvious, cross-capability* prerequisites. Don't tag the universal
baseline — almost everything needs publisher to create an API and key-manager to mint a token; that's
assumed, not annotated.

## `@legacy`

The legacy class this scenario replaces, e.g. `@legacy:APIKeyTestCase`. Makes the parity cross-check
against `legacy-feature-coverage-map.md` exact instead of estimated. Multiple allowed when one
scenario replaces several legacy methods/classes.

## Exclusion markers

Non-product scenarios carry exactly one of `@infra` (system init/shutdown, provisioning),
`@framework` (block probes, framework self-tests), `@migration` (migration suite), or `@setup`
(reusable prerequisite features — see below). The renderer skips these from the product tree but
still checks they're well-formed.

## `@setup` — reusable prerequisite features

A `@setup` feature builds prerequisites that a real product feature needs but does **not** itself
assert against — most commonly a created/deployed/published API. It exists so a consumer feature can
test its own capability without re-proving the setup. Rules:

- **It asserts nothing about coverage.** A setup feature performs real actions (e.g. create → deploy →
  publish) but is a *means*, not a subject. It is excluded from the coverage tree so that publishing
  done here is **never** miscounted as `publisher/api-lifecycle` coverage — that coverage must come
  from the real `api-lifecycle` feature.
- **Bidirectional `_setup_` filename rule.** A file named `_setup_*.feature` **must** be tagged
  `@setup`, and a `@setup` feature **must** be named `_setup_*.feature`. Either one without the other
  is a lint failure. This catches a prereq file that forgot the tag (would wrongly count as coverage)
  and a real test accidentally tagged `@setup` (would silently vanish from the tree).
- **How it's used.** List the setup feature **first** in a runner's `features = {…}` array so it runs
  before the consumers (scenarios within a runner run sequentially). It creates **uniquely-named**
  resources into the per-runner **local** context and registers them for `@After` cleanup. The file is
  reused **by reference** across many runners; each runner runs its own isolated copy and resources.
- **Don't `@dep` the baseline.** A consumer needing a published API is the universal baseline, so it
  does **not** carry `@dep:publisher`. Reserve `@dep` for non-obvious cross-capability prerequisites.

## Examples

Good:
```gherkin
@cap:key-manager @feat:api-key @rule:revocation @type:smoke @legacy:APIKeyTestCase
Scenario: Generate API key, associate to API, then revoke

@cap:publisher @feat:definitions @type:negative
Scenario: Importing an invalid OpenAPI definition is rejected
```

Bad:
```gherkin
# two capabilities under test — split it
@cap:publisher @cap:gateway @feat:api-lifecycle
Scenario: Create an API and invoke it

# @feat not defined under @cap:gateway in capability-map.yml — lint fails
@cap:gateway @feat:api-lifecycle
Scenario: ...

# untagged product scenario — lint fails (missing @cap/@feat)
Scenario: ...
```

## What lint enforces

The renderer (`render_coverage_tree.py`) validates, in one pass, that every **product** scenario:
1. has exactly one `@cap` and exactly one `@feat`;
2. its `(@cap, @feat)` pair exists in `capability-map.yml`;
3. any `@type` value is one of `smoke`/`negative`/`regression`;
4. any `@dep` value is a valid capability id.

It also enforces the bidirectional `_setup_`⟺`@setup` rule on **every** scenario (product or not):
a `_setup_*.feature` must be `@setup` and vice-versa, or the scenario goes to the invalid bucket.

Anything failing goes to an **Unmapped / invalid** bucket and the tool **exits non-zero** — wire it as
a CI gate. Excluded (`@infra`/`@framework`/`@migration`/`@setup`) scenarios are checked only for being
well-formed (and, for `@setup`, the filename rule). This is convention validation, **not** a coverage
gap-finder — empty branches in the tree are shown, never failed.

## Running

```
python3 docs/devs/render_coverage_tree.py
```
Writes `docs/devs/coverage-tree.md` and prints a lint summary; non-zero exit on violations.
