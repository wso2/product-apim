#!/usr/bin/env bash
# Fetches the timing inputs of an integration-v2 CI run for suite_timings.py.
#
# usage: fetch_ci_timings.sh <pull request URL | PR number | workflow run id> <out dir> [repo]
#                            [--topology all-in-one|distributed]
#        repo defaults to wso2/product-apim; topology to all-in-one. Needs an authenticated `gh` CLI.
#
# CI runs each topology's suite (testng-v2.xml, testng-v2_distributed.xml) as one job per `group` block parameter
# (matrix ids v2-<topology>-<group>); a run from before the group split has one v2-<topology> job.
# Writes to <out dir>:
#   testng-results.xml  the topology's block AND runner timings: the reports artefacts of all of its group jobs, merged
#                       into one file (suite_timings.py separates them again by each block's group)
#   <suite file>        testng-v2.xml or testng-v2_distributed.xml at the run's commit (the layout that ran)
#   features/           the feature files at the run's commit (suite_timings.py --features maps titles to files)
#   job.log             the topology's integration-v2 job logs, every group concatenated (block-level fallback when
#                       the artefacts have expired)
#   run.txt             run id, commit, conclusion, URLs
# A PR resolves to its most recent run of the workflow that contains the integration-v2 jobs. Timings are complete or
# absent: a missing group's artefact (or log) discards that source rather than leaving its blocks out. Exits non-zero,
# saying what is missing, when neither source is complete.
set -euo pipefail

usage="usage: fetch_ci_timings.sh <PR URL | PR number | run id> <out dir> [repo] [--topology all-in-one|distributed]"
topology=all-in-one
args=()
while [ $# -gt 0 ]; do
    case "$1" in
        --topology) topology="${2:?$usage}"; shift 2 ;;
        *) args+=("$1"); shift ;;
    esac
done
target="${args[0]:?$usage}"
out="${args[1]:?$usage}"
repo="${args[2]:-wso2/product-apim}"
case "$topology" in
    all-in-one) suite_file=testng-v2.xml ;;
    distributed) suite_file=testng-v2_distributed.xml ;;
    *) echo "unknown topology '$topology'; $usage" >&2; exit 2 ;;
esac
suite_path=all-in-one-apim/modules/integration-v2/tests-integration/cucumber-tests/src/test/resources/$suite_file
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
git show "$sha:$suite_path" > "$out/$suite_file"
features_path="$(dirname "$suite_path")/features"
rm -rf "$out/features"
git archive "$sha" "$features_path" | tar -x -C "$out" --strip-components=$(( $(tr -cd / <<<"$features_path" | wc -c) ))

# The topology's matrix ids: one per group, or the single job of a run from before the group split. The run's jobs
# decide the groups, so expired artefacts cannot make a grouped run look ungrouped.
artefacts=$(gh api --paginate "repos/$repo/actions/runs/$run/artifacts" --jq '.artifacts[] | select(.expired == false)
    | select(.name | test("^integration-v2-reports-v2-'"$topology"'(-group[0-9]+)?$")) | [.name, .id, .created_at] | @tsv')
groups=()
while IFS= read -r group; do groups+=("$group"); done < <(jq -r --arg p "integration-v2 (v2-$topology-" \
    '.jobs[].name | select(startswith($p)) | ltrimstr($p) | split(",")[0]' <<<"$meta" | sort -u)
[ "${#groups[@]}" -gt 0 ] || groups=("")
if [ -z "${groups[0]}" ] && ! jq -e --arg n "integration-v2 (v2-$topology," \
        '[.jobs[] | select(.name | startswith($n))] | length > 0' <<<"$meta" > /dev/null; then
    echo "run $run has no integration-v2 job for topology $topology" >&2
    exit 1
fi
echo "topology $topology: ${groups[*]:-ungrouped}" >&2
artefact_name() { echo "integration-v2-reports-v2-$topology${1:+-$1}"; }
job_name() { echo "integration-v2 (v2-$topology${1:+-$1},"; }

# Full timings: every group's reports artefact (the newest upload of each name, i.e. the latest attempt).
have_results=false
tmp=$(mktemp -d)
results=()
missing=""
for group in "${groups[@]}"; do
    name=$(artefact_name "$group")
    id=$(awk -F'\t' -v n="$name" '$1 == n' <<<"$artefacts" | sort -t$'\t' -k3 | tail -1 | cut -f2)
    if [ -z "$id" ]; then missing="$missing $name"; continue; fi
    mkdir -p "$tmp/$name"
    gh api "repos/$repo/actions/artifacts/$id/zip" > "$tmp/$name.zip"
    (cd "$tmp/$name" && unzip -q "../$name.zip")
    found=$(find "$tmp/$name" -name testng-results.xml | head -1)
    if [ -n "$found" ]; then results+=("$found"); else missing="$missing $name(no testng-results.xml)"; fi
done
if [ -z "$missing" ] && [ "${#results[@]}" -gt 0 ]; then
    # One file for suite_timings.py: each further group's <test> blocks join the first group's <suite>.
    python3 - "$out/testng-results.xml" "${results[@]}" <<'PY'
import sys
import xml.etree.ElementTree as ET
target, first, *rest = sys.argv[1:]
tree = ET.parse(first)
suite = tree.getroot().find('suite')
for path in rest:
    for test in ET.parse(path).getroot().find('suite').findall('test'):
        suite.append(test)
tree.write(target, encoding='utf-8', xml_declaration=True)
PY
    have_results=true
else
    echo "reports artefact missing for the $topology suite:${missing:- none found}" >&2
fi
rm -rf "$tmp"

# Block-level fallback: every group's integration-v2 job log, concatenated.
# The raw log (no job/step prefixes), retried because the logs endpoint can answer empty transiently.
have_log=true
: > "$out/job.log"
for group in "${groups[@]}"; do
    name=$(job_name "$group")
    job=$(jq -r --arg n "$name" '[.jobs[] | select(.name | startswith($n))][0].databaseId' <<<"$meta")
    got=false
    if [ -n "$job" ] && [ "$job" != null ]; then
        for attempt in 1 2 3; do
            if gh api "repos/$repo/actions/jobs/$job/logs" > "$out/job.part" 2>/dev/null && [ -s "$out/job.part" ]; then
                cat "$out/job.part" >> "$out/job.log"
                got=true
                break
            fi
            echo "job log for job $job came back empty (attempt $attempt/3)" >&2
            sleep $((attempt * 10))
        done
    fi
    $got || { echo "job log missing for ${name%,}" >&2; have_log=false; }
done
rm -f "$out/job.part"
$have_log || rm -f "$out/job.log"

echo "run $run at $sha: testng-results.xml=$have_results job.log=$have_log -> $out"
if ! $have_results && ! $have_log; then
    echo "neither complete reports artefacts nor complete job logs are available for run $run" >&2
    exit 1
fi
