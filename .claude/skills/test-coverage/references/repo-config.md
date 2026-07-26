# Repo-location config

The skill needs local checkout paths for the up-to-three repos it spans. Store them at working-directory scope so
reruns don't re-ask.

**File:** `.claude/test-coverage-repos.json` (repo-root-local; machine-specific — treat as untracked/gitignored).

**Format:**
```json
{
  "carbon-apimgt": "/abs/path/to/carbon-apimgt",
  "product-apim": "/abs/path/to/product-apim",
  "docs-apim": "/abs/path/to/docs-apim"
}
```

**Rules:**
- On Phase 0, read this file. For each key: if the path exists → that repo is *local* (you can run tests there).
- `product-apim` defaults to the current working repo root — do not ask for it.
- For any OTHER repo missing/stale in the config (`carbon-apimgt`; `docs-apim` for the feature persona): **ASK
  the developer and wait — do NOT auto-detect and persist a guess.** A wrong repo → wrong analysis. Write the
  confirmed path to the file so later runs don't re-ask.
- A repo the dev doesn't have locally → operate in *plan-only* mode for it (you can still fetch its PR diff via
  `gh pr diff` to analyze, but you cannot compile/run its tests).
- **Override:** if the dev gives a path inline for a run, use it for that run and offer to persist it.
