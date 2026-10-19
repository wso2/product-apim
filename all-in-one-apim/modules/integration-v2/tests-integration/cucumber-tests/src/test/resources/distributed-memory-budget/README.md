# Distributed CI memory budget

The distributed GitHub Actions lane places the test process, its Playwright browser cgroup, and Docker containers
under the `apimdistributed.slice` cgroup-v2 slice. The slice requests:

| Limit | Value | Effect |
|---|---:|---|
| `MemoryHigh` | 14 GiB | Reclaim and memory pressure begin above this level |
| `MemoryMax` | 15 GiB | Hard RAM ceiling, including page cache and kernel memory charged to the workload |
| `MemorySwapMax` | 5 GiB | Aggregate swap ceiling for the distributed workload |

The workflow temporarily sets Docker's default cgroup parent to this slice, then restores the runner's original
Docker configuration and systemd slice after the suite. If the runner has less than 5 GiB of swap, setup tops it up
with a temporary swap file after checking that sufficient disk space remains. Cleanup removes the file after
successfully disabling it.

Each distributed Docker container keeps its existing RAM cap and may use additional swap up to the
smaller of 25% of that cap or 512 MiB. The browser keeps its 512 MiB RAM cap and may use up to 128 MiB swap.
These per-container allowances are subordinate to the slice's 5 GiB aggregate swap limit. The all-in-one workflow
does not create this slice or alter Docker's configuration; its browser remains capped at 2.5 GiB with no swap.
The distributed browser uses the managed `apimdistributed-browser.service` child cgroup so systemd preserves
its limits as the test scopes and containers start. The suite launch preserves the runner user's environment,
pinned Maven/JDK paths, working directory and open-file limits when entering the shared slice.

The setup script requires Ubuntu with cgroup v2 and Docker's systemd cgroup driver. It is intended to run before
starting the distributed test containers:

```bash
sudo bash setup-distributed-memory-budget.sh setup "$RUNNER_TEMP/apim-distributed-memory-budget"
```

After the test containers have stopped, restore host configuration with:

```bash
sudo bash setup-distributed-memory-budget.sh cleanup "$RUNNER_TEMP/apim-distributed-memory-budget"
```

Setup rolls back host configuration if any step fails. Cleanup stops the test slice, including containers and
browser helpers left after cancellation, before restoring configuration and disabling the temporary swap file.
If restoration fails, cleanup reports an error and retains its backups so it can be retried.
