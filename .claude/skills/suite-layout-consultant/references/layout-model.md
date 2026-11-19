# The grouped 4-lane layout model

## Shape of a run

- Two topologies share one block and runner layout: `testng-v2.xml` (all-in-one APIM per block) and
  `testng-v2_distributed.xml` (control plane, traffic manager and gateway per block); `docs/devs/suite_alignment.py`
  keeps them aligned, including each block's `group`.
- Every block declares `<parameter name="group" value="groupN"/>`. CI runs each topology as one job per group
  (`-Dapim.v2.test.groups=groupN`, matrix ids `v2-<topology>-<group>`), all jobs side by side on separate runners.
- Within a job: `<suite parallel="tests" thread-count="4">` — four of that group's `<test>` blocks run at a time; when
  one ends, the group's next block in file order starts. A block = one APIM container (plus IS/Squid/Solace/platform gateway if its
  parameters ask) booted by `BlockLifecycleListener`, its runners, then teardown.
- Inside a block, `<test parallel="classes" thread-count="N">` runs N runners at a time (lanes). Runners are
  dispatched in suite order; scenarios inside a runner run sequentially; feature files inside a runner run in
  **lexicographic filename order** (not array order).
- CI runner: 4 vCPU / 16 GB. APIM heap `-Xms1g -Xmx1536m`; a block peaks ~2.5 GiB; CPU, not memory, limits
  concurrency. Most blocks cap APIM at 1 CPU (`apimCpuLimit`), IS at 1 (`isCpuLimit`); the shared node backend is
  capped at 1 CPU for all blocks; Squid 0.5.

## Block length

`block length ≈ boot + longest lane`, where the longest lane comes from dispatching the block's runners (in suite
order) onto `thread-count` lanes, each runner to the lane that frees first.

- **Boot**: container start to "booted and ready" — ~115–170 s for a capped APIM block in CI; more with an external
  IS, throttle-data bootstrap (`bootstrapThrottleData`, readiness up to 180 s), listener-reload readiness, tenant and
  secondary-store provisioning. This is the fixed price of every block.
- **Lanes**: `thread-count` > 1 only shortens a block if its runners are wait-bound (gateway invocation, propagation
  polls) and isolated from each other. CPU-bound runners (Publisher/DevPortal CRUD) on a 1-CPU APIM gain little and
  add contention; restarts or server-global config flips require `thread-count="1"`.
- **Order matters**: with lanes, a long runner listed last starts late and becomes the tail; list long runners first
  unless an ordering constraint (documented in the suite comment) says otherwise.

## Suite length

Within each group's job, TestNG starts that group's `<test>` blocks in suite-file order, each on the first of the 4
threads to free up. That file-order dispatch is the group's modelled time, and the suite takes as long as its longest
group. On PR #14323's ungrouped run it gave 5,289 s against a measured 5,303 s (+0.3%); on PR #14336's two-group
all-in-one run, 5,834 s (group2) against a measured 5,841 s (+0.1%). Longest-first packing (greedy: each block,
longest first, onto the queue that frees first) is a heuristic candidate order within a group, reachable by listing
the blocks longest-first. It is usually close to the shortest schedule but not guaranteed: on PR #14336 group1 it
was 98 s longer than the file order. Treat it as one candidate to model, not a bound. Longest-first regrouping (all
blocks packed onto groups x 4 queues) is likewise a heuristic candidate for moving blocks between groups. Budget: per group, 4 queues × ~3,600 s; the groups should end
close together. The levers, cheapest first: reorder blocks (no compatibility risk), rebalance blocks between groups
(changes only the `group` value, in both suites), shorten the longest blocks (they set the floor), and remove boots
(fewer, fuller blocks).

`scripts/suite_timings.py` implements the models and a what-if mode; its `position` directive models a reorder and its
`group` directive a move to another group. `--group` limits the report to one group.

## Model limits — say these in the report

- Spans from a concurrent run include contention: runners in a block that shares the host with 3 others run slower
  than alone, and the slowdown is uneven (CPU-bound more than wait-bound). Isolated measurements are lower bounds.
- File-order dispatch is only as good as the block lengths fed to it: a block that ran slower on that run (runner
  variance, a retried scenario) shifts every block after it on its thread. A reorder that wins by less than a block's
  run-to-run variance is not a reliable gain.
- Single runs vary ~±15% per runner; for close decisions measure twice.
- Local (macOS + Colima) timings differ from CI (Linux, 4 vCPU). Compare like with like; prefer CI artefacts for the
  baseline.
