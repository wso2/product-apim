---
name: parity-checker
description: >-
  Audit one legacy product-apim integration-test class against Integration V2. Trace TestNG/factory fan-out,
  scenario behavior, and assertion-level contracts; use when checking or closing legacy-to-V2 test parity.
---

# Legacy-to-V2 Parity Checker

Audit one legacy `tests-backend` Java test class per invocation. Keep the comparison focused on that class and
its direct fixtures, helpers, and execution wiring. The goal is complete, evidence-backed parity for every
currently executing legacy behavior **and** every meaningful regression-protecting assertion. Similar feature
names or a passing happy path are not proof. Assume enough time to trace the implementation fully; if evidence
is missing, mark the result unresolved rather than covered.

## Required accounting: two ledgers, no inferred parity

Maintain separate ledgers for:

1. **Behavior/fan-out cases:** each distinct invocation dimension that can change expected behavior (actor,
   tenant, input partition, auth/protocol, configuration, resource state, branch, or outcome).
2. **Assertion obligations:** each distinct product contract the legacy test verifies for those cases (status,
   exact error/code/body, persisted state, lifecycle transition, response fields, ordering, count, distribution,
   metadata/schema, or negative behavior).

Do not let a zero in one ledger erase gaps in the other. A case may have the right setup and scenario in V2 but
still have assertion gaps; conversely, a matching assertion in one actor/mode does not cover another fan-out row.
Avoid inflating counts with duplicate Java assertions for the same contract: count meaningful observable
obligations, but split obligations whenever the expected product behavior or independently observable result
differs. In the report and tracker, show behavior gaps and assertion gaps separately; if an existing tracker has
only one count, extend its schema rather than folding one category into the other.

### Value-add actor coverage: tenant context × privilege

Evaluate actor coverage on two independent axes: **tenant context** and **effective privilege**. Actor names or
tenant-domain strings are not, by themselves, distinct coverage modes.

- **Tenant-context comparison:** V2 commonly uses `admin@tenant1.com` as its representative tenant actor. Legacy may
  use a different tenant domain; that string mismatch alone is neither a parity gap nor additional coverage. Compare
  the tenant's actual provisioning, configuration, behavior, and assertions. The meaningful baseline, when tenant
  behavior is in scope, is super-tenant plus one non-super tenant (the repository's “x2 tenant” convention). A
  second non-super tenant with equivalent setup/role that repeats the same isolated flow is redundant. It counts as
  distinct only when the scenario exercises a real domain-specific difference: different tenant configuration or
  permissions, domain-dependent routing/product behavior, cross-tenant visibility/isolation, or another evidenced
  product path. Merely creating equivalent resources under another domain does not prove cross-tenant isolation.
- **Privilege comparison:** establish effective roles and permissions from fixture provisioning/configuration, then
  compare the direction of privilege between the legacy actor and the V2 actor. A different username is not a
  privilege mode unless its effective permissions or tested authorization outcome differ.
  - If legacy's scenario succeeds as a least-privileged actor, V2 must exercise that same least-privilege contract;
    running only as an equal- or higher-privileged actor does not cover it.
  - If legacy uses an equal- or higher-privileged actor while V2 uses a stricter/less-privileged actor, do not
    automatically claim parity. Verify that the V2 actor is actually authorized for the exact operation and resource
    under test, and that the same assertions are satisfied. If that actor is not supposed to have access but the
    operation succeeds, report a potential product-side authorization/security defect; do not treat the unexpected
    access as valid parity or weaken the legacy contract. If access is valid for that role, the V2 row may provide
    equal or stronger privilege coverage, but document the permission evidence and matching observable contract.
  - When legacy covers multiple genuinely distinct privilege levels (for example, publisher and subscriber), map
    each allowed/denied outcome. A role label alone is insufficient; inspect the assigned roles and the operation's
    authorization expectation.
- Preserve real legacy modes, but do not mechanically reproduce equivalent tenant-domain rows. Collapse repeated
  domain rows only after verifying equivalent roles, setup/configuration, inputs, behavior, and assertions, and that
  no domain-sensitive behavior is under test. Record the original legacy fan-out and why one V2 tenant representative
  suffices in the duplicate audit.

When assessing a proposed extra actor/example row, answer explicitly: (1) what tenant-context or privilege axis
does it add, (2) what distinct product behavior/assertion exercises that axis, (3) what role/configuration evidence
proves the difference, and (4) why an existing actor row does not already exercise it. If these cannot be answered
with concrete evidence, do not count it as parity value; classify it as a redundant candidate rather than expanding
the suite. A successful call by an unexpectedly underprivileged actor is a security finding to investigate, not
automatic evidence of good parity.

Counts are a summary, never the audit deliverable. For every `partial`, `gap`, or `unresolved` item, record the
specific legacy execution row and assertion, the closest V2 candidate (or explicitly `none`), and the precise missing
dimension or observable. Give source locations on both sides (file plus method/scenario/step; line numbers where
practical) and state the minimal V2 coverage that would close the item. This must let the next person implement the
gap without repeating the source audit. Reconcile each reported total against these itemized rows; do not infer counts
from a headline or count ambiguous items as resolved.

## 1. Establish exactly what legacy executes

Inspect the assigned class, superclass, directly used fixtures/helpers, and the exact `testng*.xml` registration.
Establish suite filters, ordering/dependencies, setup and teardown, active status, and the runtime configuration.
In this repository, backend Surefire uses `testng.xml` and `testng-server-mgt.xml`; `APIMAlterSuiteListener` can
filter main-suite sections/classes via `PRODUCT_APIM_TESTS`, `PRODUCT_APIM_TEST_CLASSES`, and
`PRODUCT_APIM_TEST_GROUPS`. XML `group1`–`group4` is a CI segment label, not a TestNG `@Test` group.

Build an explicit legacy invocation matrix. Expand every applicable source of fan-out:

- Every active `@Factory` provider row × every applicable test method; read the provider implementation and
  values, not just its annotation. A factory row is a distinct fixture instance.
- Every active method-level `@DataProvider` row and each meaningful input/expected-outcome partition.
- Loops, arrays, branches, actor switching, resource/API variants, auth grants, protocols, and configuration
  overlays that cause different product behavior.
- All identities and planes: super-tenant/tenant, admin/user, secondary-store/email identities, anonymous and
  cross-tenant actors. Do not assume the factory mode is the only actor used.
- `dependsOnMethods`, TestNG ordering, suite setup/configuration classes, and `@Before*`/`@After*` hooks. Setup
  classes are not scenario coverage by themselves.

For every active invocation row, enumerate the assertions that guard its behavior. Read helper implementations
and fixtures to learn what an assertion truly establishes; a mocked/stubbed response may verify fixture wiring
without proving product persistence or behavior. Record exact inputs, expected outcomes, state/readback, timing or
ordering constraints, and cleanup effects where they affect the tested contract. Inspect broad cleanup and shared
mutable state for hidden dependencies between methods or factory instances.

Keep dormant intent separate: commented methods, provider rows, and XML entries are not active. List them as
`dormant-low-priority` if relevant, but do not count them as active gaps unless re-enabled. For source test classes
absent from the configured suite, check alternate descriptors and class-selection paths before deciding whether
they execute.

## 2. Trace V2 execution end to end

Search by behavior, request, assertion, and actor—not only by legacy class name. For each candidate, trace:

`TestNG suite -> runner -> feature/example row -> step definition -> helper/client -> actual request/result assertion`.

Verify the runner is registered in each applicable topology suite and inspect topology-specific parameters,
overlays, dependencies, and actor data. A feature file without a registered runner is not active coverage; an
all-in-one registration does not prove distributed registration. Read shared steps/helpers to confirm they retain
the exact legacy expectation rather than swallowing errors, accepting a broader status, checking only non-null,
or asserting solely on a mocked fixture.

Map every legacy behavior row and every assertion obligation to concrete V2 evidence: suite XML, runner, feature
row, step definition/helper, and exact predicate or expected value. Record both sides' file/line locations where
possible. One V2 scenario can map to multiple legacy rows only when its inputs actually exercise each dimension
and its assertions distinguish the outcomes.

For each mapped V2 scenario, record its unique coverage contribution: legacy row(s), actor/input/configuration
dimensions exercised, and assertion obligations it protects. A matching scenario title or shared feature is not
evidence that every mapped row is actually exercised.

### Assertion-parity traps to check explicitly

Do not mark a case fully covered until checking these whenever applicable:

- **Exactness:** exact status/error code/body, field values, collection size/order, schema, metadata, and final
  persisted state—not just request acceptance or object existence.
- **Negative and boundary outcomes:** rejected credentials, invalid inputs, absent resources, limits, empty
  collections, and alternate branches that legacy explicitly asserted.
- **Mutation and propagation:** after update/delete/revoke/lifecycle changes, verify the resulting representation
  and, when legacy verifies a consuming plane, that plane too. A successful mutation response alone does not
  prove read-model, gateway, or event convergence.
- **Fan-out-sensitive behavior:** preserve actor × mode × input combinations. A scenario that only uses the
  default value does not cover other provider rows or branches.
- **Distribution/selection behavior:** when legacy configures weighted routing, failover, round robin, or multiple
  endpoints, verify that V2 really configures distinct targets and observes the intended split/selection across
  them. Seeing each possible response once is not proof of a weighted distribution. Compare sample size and
  acceptance bounds with the legacy contract; do not invent loose tolerances that weaken it.
- **Consumer-visible metadata:** where legacy checks updated tools/operations/API metadata, compare the exact
  post-update representation at the same plane. A Publisher-side update assertion does not prove Gateway-visible
  metadata was refreshed.
- **Fixture versus product evidence:** identify whether the test itself seeds a mock or stub before reading it.
  Such a read proves the fixture's behavior, not that APIM wrote or persisted that data.

These are audit prompts, not assumptions that every class has every contract. Apply only where the legacy source
demonstrates the behavior or assertion.

## 2a. Mandatory actionable gap register and duplicate audit

Before giving totals, produce an item for every non-covered behavior row and every non-covered assertion obligation.
Keep behavior and assertion entries separately identifiable, even when one V2 change could close both. A compact
register can use this schema:

| ID / ledger | Legacy method + fan-out row | Required behavior/assertion | V2 location and current evidence | Exact missing part | Minimal V2 coverage needed | Duplicate/reuse assessment |
|---|---|---|---|---|---|---|

Use concrete identifiers and locations (legacy method/provider row; V2 suite XML, runner, feature/example, step/helper,
and relevant predicate). For `partial`, `gap`, and `unresolved` items, state what is present and what is absent or
weaker. If one V2 change closes several items, cross-reference their IDs rather than silently merging or
double-counting them. Explain the denominator and derive the behavior-gap and assertion-gap totals from the register.

Also audit candidate V2 scenarios for duplication and unnecessary suite growth. Identify scenarios that appear to
exercise the same behavior and compare actual actor, tenant, input, configuration, lifecycle state, product plane,
setup, expected outcome, and assertion set. Classify each pair/group as:

- **distinct/keep:** a real fan-out or independent regression contract differs (for example, tenant versus
  super-tenant, negative versus positive outcome, or Publisher persistence versus Gateway-visible behavior);
- **overlapping/reuse:** one scenario can represent multiple legacy rows because it explicitly exercises each input
  dimension and retains each distinct assertion; or
- **redundant candidate:** execution and regression-protective assertions are equivalent, with no unique legacy row,
  product path, fixture, or diagnostic value.

For each redundant candidate, name the V2 scenarios and locations, list shared and unique coverage, and recommend the
smallest safe consolidation—or explain why none is safe. Do not recommend deletion merely because names, setup, or
expected status match. Never merge away required actor/fan-out coverage or independent assertions to reduce scenario
count. Report duplicate findings separately from legacy parity-gap counts; redundant V2 tests do not erase missing
legacy behavior.

## 3. Classify conservatively

For each behavior row and assertion obligation, use `covered`, `partial`, `gap`, `dormant-low-priority`, or
`unresolved`:

- `covered`: active V2 execution exercises the same relevant inputs/actors and retains the same or stronger
  observable contract, with evidence in the registered suite(s).
- `partial`: some but not all fan-out dimensions or assertion details are represented.
- `gap`: absent, disabled, non-equivalent, or demonstrably weaker V2 coverage.
- `unresolved`: source/runtime evidence is insufficient to decide. Never convert uncertainty to covered.
- `dormant-low-priority`: legacy behavior is commented out/not run; report separately from active gaps.

Do not credit a proposed fix, skipped/disabled scenario, prior unrelated pass, broader accepted outcome, or
matching name as parity. Keep source-level parity and runtime verification distinct: report which topology was
actually run, with runner/filter/command and result. If a runtime run cannot start or is blocked, say so and leave
runtime verification pending; do not describe compile, coverage-tree validation, or an earlier version's pass as
verification of current changes.

## 4. Report and tracker

Present the expanded legacy invocation matrix, then V2 mappings and the assertion-obligation ledger, then the
mandatory itemized gap register and duplicate audit. Only after those, give reconciled totals for behavior rows and
assertion obligations: covered, partial, missing, unresolved, and dormant where applicable. Explain fan-out
multiplication and gap-count derivation so another reviewer can reproduce the counts. Include exact evidence
paths/lines, topology registration, and runtime verification status. Never return only aggregate parity counts.

Only report **zero active source parity gaps** if both ledgers have zero partial, gap, or unresolved entries and
every applicable topology registers the coverage. Report **runtime verified** separately, and only for topologies
actually run successfully against the audited revision. If the investigation reveals that a previous audit
under-counted a class, redo that class's ledgers and correct the current tracker result while preserving its audit
history; note that the new result supersedes the earlier count.

For this repository, update `all-in-one-apim/modules/integration-v2/docs/devs/legacy-v2-parity-tracker.md` only
for the assigned class. Increment its audit attempt once per completed source audit. Keep separate behavior-gap
and assertion-gap counts, append a date column on a later audit date, and preserve prior dates/results. Do not
change other classes' rows. Do not implement test changes unless the user separately asks for gap closure.

Do not begin a broad suite-wide parity comparison, modify legacy tests, or run the full integration suite from
this skill invocation. If activation or runtime behavior cannot be established statically, recommend a focused
run and report the evidence as pending until that run is actually completed.
