# Splitting playbook — runners, features and blocks by measured time

Split when a runner or block sets a queue's length (it is the suite's critical path) or when one block would exceed
~3,600 s. Splitting is the opposite lever to consolidation: it trades boots for parallelism, so split only where the
time saved exceeds the boot added.

## Splitting a feature file

- Split by **cohesive sub-capability**, not arbitrarily: e.g. comments into `comments_1_thread_reads.feature` and
  `comments_2_thread_mutations.feature`; `throttling_restart.feature` and `throttling_restart_gateway_limits.feature`.
- Execution order inside a runner is **lexicographic by filename**: use numeric prefixes (`1_`, `2_`) or the `_setup_`
  prefix to keep dependent parts in order.
- Shared prerequisites go in a `_setup_*.feature` (tagged `@setup`, no assertions on product behaviour), listed by each
  runner that needs it; each runner runs its own copy with unique names, so split runners never collide.
- Every scenario must stay executed by a registered runner: regenerate the coverage tree and require
  `not-executed: 0`.

## Splitting a runner

- A runner is the unit of lane parallelism. Two runners over the two halves of a split feature can share one block on
  two lanes (`thread-count="2"`) when they are wait-bound and isolated, or go to different blocks.
- Keep runner javadoc stating what it runs and any ordering/isolation constraint.

## Splitting a block

- Gateway was split into Gateway-A/B/C by measured runner time so no single gateway block dominates a queue; Publisher
  and DevPortal stay single blocks at `thread-count="1"` (CPU-bound CRUD on a 1-CPU APIM).
- A new block pays a boot; it is justified only when the modelled suite time drops (run the what-if) or a block would
  otherwise exceed the budget.

## Rebalancing without splitting

- Move a long runner to the start of its block's runner list (it otherwise starts late and becomes the tail).
- Move a runner to a block with spare lane time and a compatible overlay (consolidation checklist).
- Reconsider `thread-count`: raise it for wait-bound blocks with isolated runners; lower it for CPU-bound blocks.
- Reorder `<test>` blocks so the heaviest start first.
