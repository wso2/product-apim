#!/usr/bin/env bash
# Fetches the timing inputs of an integration-v2 CI run for suite_timings.py.
#
# usage: fetch_ci_timings.sh <pull request URL | PR number | workflow run id> <out dir> [repo]
#        repo defaults to wso2/product-apim. Needs an authenticated `gh` CLI.
#
# Writes to <out dir>:
#   testng-results.xml  from the run's integration-v2 reports artefact (block AND runner timings)
#   testng-v2.xml       the suite file at the run's commit (the layout that produced those timings)
#   features/           the feature files at the run's commit (suite_timings.py --features maps titles to files)
#   job.log             the integration-v2 job log (block-level fallback when the artefact has expired)
#   run.txt             run id, commit, conclusion, URLs
# A PR resolves to its most recent run of the workflow that contains the integration-v2 job. Exits non-zero, saying
# what is missing, when neither the artefact nor the job log is available.
set -euo pipefail

target="${1:?usage: fetch_ci_timings.sh <PR URL | PR number | run id> <out dir> [repo]}"
out="${2:?usage: fetch_ci_timings.sh <PR URL | PR number | run id> <out dir> [repo]}"
repo="${3:-wso2/product-apim}"
suite_path=all-in-one-apim/modules/integration-v2/tests-integration/cucumber-tests/src/test/resources/testng-v2.xml
mkdir -p "$out"
out=$(cd "$out" && pwd)
# The product-apim checkout: the one this script lives in, else the current directory.
checkout=$(git -C "$(dirname "$0")" rev-parse --show-toplevel 2>/dev/null || git rev-parse --show-toplevel 2>/dev/null) \
    || { echo "run from a product-apim checkout (git is needed for the suite and features at the run's commit)" >&2; exit 1; }
cd "$checkout"

if [[ "$target" =~ /pull/([0-9]+) ]] || { [[ "$target" =~ ^[0-9]+$ ]] && [ "${#target}" -lt 8 ]; }; then
    pr="${BASH_REMATCH[1]:-$target}"
    sha=$(gh pr view "$pr" -R "$repo" --json headRefOid --jq .headRefOid)
    run=$(gh run list -R "$repo" --commit "$sha" --limit 20 --json databaseId,workflowName \
        --jq '[.[] | select(.workflowName == "APIM builder")][0].databaseId')
    [ -n "$run" ] && [ "$run" != null ] || { echo "no 'APIM builder' run found for PR #$pr ($sha)" >&2; exit 1; }
else
    run="${target##*/runs/}"; run="${run%%/*}"
fi

meta=$(gh run view "$run" -R "$repo" --json headSha,conclusion,url,createdAt,jobs)
sha=$(jq -r .headSha <<<"$meta")
printf 'run %s\ncommit %s\nconclusion %s\ncreated %s\nurl %s\n' "$run" "$sha" "$(jq -r .conclusion <<<"$meta")" \
    "$(jq -r .createdAt <<<"$meta")" "$(jq -r .url <<<"$meta")" > "$out/run.txt"

# The suite file at the run's commit (fetch the commit if it is not local yet).
git cat-file -e "$sha^{commit}" 2>/dev/null || git fetch -q "https://github.com/$repo.git" "$sha"
git show "$sha:$suite_path" > "$out/testng-v2.xml"
features_path="$(dirname "$suite_path")/features"
rm -rf "$out/features"
git archive "$sha" "$features_path" | tar -x -C "$out" --strip-components=$(( $(tr -cd / <<<"$features_path" | wc -c) ))

# Full timings: the integration-v2 reports artefact.
have_results=false
artefact=$(gh api "repos/$repo/actions/runs/$run/artifacts" \
    --jq '[.artifacts[] | select(.name | startswith("integration-v2-reports")) | select(.expired == false)][0].name')
if [ -n "$artefact" ] && [ "$artefact" != null ]; then
    tmp=$(mktemp -d)
    gh run download "$run" -R "$repo" -n "$artefact" -D "$tmp"
    found=$(find "$tmp" -name testng-results.xml | head -1)
    if [ -n "$found" ]; then cp "$found" "$out/testng-results.xml"; have_results=true; fi
    rm -rf "$tmp"
fi

# Block-level fallback: the integration-v2 job log.
# The raw log (no job/step prefixes), retried because the logs endpoint can answer empty transiently.
job=$(jq -r '[.jobs[] | select(.name | startswith("integration-v2 ("))][0].databaseId' <<<"$meta")
have_log=false
if [ -n "$job" ] && [ "$job" != null ]; then
    for attempt in 1 2 3; do
        if gh api "repos/$repo/actions/jobs/$job/logs" > "$out/job.log" 2>/dev/null && [ -s "$out/job.log" ]; then
            have_log=true
            break
        fi
        echo "job log for job $job came back empty (attempt $attempt/3)" >&2
        sleep $((attempt * 10))
    done
    $have_log || rm -f "$out/job.log"
fi

echo "run $run at $sha: testng-results.xml=$have_results job.log=$have_log -> $out"
if ! $have_results && ! $have_log; then
    echo "neither the reports artefact nor the job log is available for run $run" >&2
    exit 1
fi
