#!/usr/bin/env python3
"""Render the integration-v2 coverage tree from capability-map.yml + .feature tags, and lint tags.

Single pass: builds a Markdown capability->feature->scenario tree (empty branches shown as gaps) and
validates every product scenario's tags against the curated skeleton. Invalid/untagged product
scenarios go to an "Unmapped / invalid" bucket; the script exits non-zero if that bucket is non-empty
so it can be wired as a CI gate. Non-product scenarios (@infra/@framework/@migration/@setup) are
excluded from the tree. @setup marks reusable prerequisite features and is bidirectional with the
`_setup_` filename prefix (one without the other is a lint violation). See TAGGING-CONVENTIONS.md.

EXECUTION IS VERIFIED, NOT ASSUMED. A correctly-tagged scenario only counts as coverage if a runner
that actually RUNS its .feature is registered in testng-v2.xml. The chain is
testng-v2.xml <class> -> Runner.java @CucumberOptions(features=...) -> .feature, and scenarios whose
file is unreachable through it are reported separately as NOT EXECUTED and excluded from the covered
totals. Without this the tree credited a scenario that never runs, which is worse than an admitted
gap: an uncovered feature shows as an empty branch to fill, whereas a dead one looked covered. This is
a real state, not hypothetical -- a runner is deliberately parked pending a product fix, and its
scenarios were being counted. XML comments are stripped before matching <class>, so a commented-out
registration reads as absent (not stripping them would invert the whole check).

Dependency-free (stdlib only). capability-map.yml must keep its simple documented shape.

Usage:
  python3 render_coverage_tree.py [--map capability-map.yml] [--features <dir>] [--out coverage-tree.md]
                                  [--testng testng-v2.xml] [--runners <dir>]
"""
import argparse, os, re, sys
from collections import OrderedDict

HERE = os.path.dirname(os.path.abspath(__file__))
_CUKE = os.path.normpath(os.path.join(HERE, "..", "..", "tests-integration", "cucumber-tests"))
DEFAULT_MAP = os.path.join(HERE, "capability-map.yml")
DEFAULT_FEATURES = os.path.join(_CUKE, "src", "test", "resources", "features")
DEFAULT_OUT = os.path.join(HERE, "coverage-tree.md")
DEFAULT_TESTNG = os.path.join(_CUKE, "src", "test", "resources", "testng-v2.xml")
DEFAULT_RUNNERS = os.path.join(_CUKE, "src", "test", "java", "org", "wso2", "am", "integration",
                               "cucumbertests", "runners")

EXCLUSION = {"infra", "framework", "migration", "setup"}
SETUP_PREFIX = "_setup_"
VALID_TYPES = {"smoke", "negative", "regression"}
SCENARIO_RE = re.compile(r"^\s*(Scenario Outline|Scenario):\s*(.*)$")
FEATURE_RE = re.compile(r"^\s*Feature:\s*(.*)$")
CLASS_RE = re.compile(r'<class\s+name="([^"]+)"')
XML_COMMENT_RE = re.compile(r"<!--.*?-->", re.S)
# Comment stripping is a hand-rolled scan, NOT a regex, because a regex cannot tell a comment from a `//`
# inside a string literal. `features = "http://x/a.feature"` would lose everything from the `//` on, the
# features option would then not match, and the runner would silently map to [] -- reported as NOT EXECUTED,
# which is the exact false signal this tool exists to eliminate. Nothing trips it today; the point is that
# it would fail SILENTLY when something does.
def strip_java_comments(src):
    """Java source with comments removed, leaving string and char literals intact."""
    out, i, n = [], 0, len(src)
    while i < n:
        c = src[i]
        if c in '"\'':                                   # literal: copy verbatim, honouring escapes
            quote = c
            out.append(c)
            i += 1
            while i < n:
                out.append(src[i])
                if src[i] == "\\":                       # escaped char: consume the pair
                    if i + 1 < n:
                        out.append(src[i + 1])
                        i += 2
                        continue
                elif src[i] == quote:
                    i += 1
                    break
                i += 1
            continue
        if c == "/" and i + 1 < n:
            if src[i + 1] == "/":
                while i < n and src[i] != "\n":
                    i += 1
                continue
            if src[i + 1] == "*":
                end = src.find("*/", i + 2)
                i = n if end < 0 else end + 2
                continue
        out.append(c)
        i += 1
    return "".join(out)
# Both @CucumberOptions forms: the braced array `features = {"a", "b"}` AND the single-value
# `features = "a"` (42 runners use the latter -- requiring braces silently dropped every one of them).
FEATURES_OPT_RE = re.compile(r"features\s*=\s*(\{.*?\}|\"[^\"]*\")", re.S)
FEATURE_PATH_RE = re.compile(r'"([^"]*?\.feature)"')
ABSTRACT_CLASS_RE = re.compile(r"\babstract\s+class\b")


def parse_capability_map(path):
    caps = OrderedDict()
    cur, in_feats = None, False
    with open(path, encoding="utf-8") as fh:
        for raw in fh:
            if not raw.strip() or raw.lstrip().startswith("#"):
                continue
            indent = len(raw) - len(raw.lstrip())
            content = raw.strip()
            if indent == 0:
                continue
            if indent == 2 and content.endswith(":") and "{" not in content:
                cur = content[:-1].strip()
                caps[cur] = {"name": cur, "features": OrderedDict()}
                in_feats = False
            elif indent == 4 and content.startswith("name:"):
                if cur is None:
                    sys.exit("capability-map malformed: 'name:' at indent 4 before any capability header")
                caps[cur]["name"] = content[len("name:"):].strip()
            elif indent == 4 and content.startswith("features:"):
                if cur is None:
                    sys.exit("capability-map malformed: 'features:' at indent 4 before any capability header")
                in_feats = True
            elif indent == 6 and in_feats and cur is not None:
                m = re.match(r"([\w-]+):\s*(.*)$", content)
                if m:
                    fid, rest = m.group(1), m.group(2)
                    nm = re.search(r"name:\s*([^}]+)", rest)
                    caps[cur]["features"][fid] = nm.group(1).strip() if nm else fid
    return caps


def parse_tags(line):
    """Return (namespaced dict key->list[val], set of bare tags) for one tag line."""
    ns, bare = {}, set()
    for tok in line.split():
        if not tok.startswith("@"):
            continue
        body = tok[1:]
        if ":" in body:
            k, v = body.split(":", 1)
            ns.setdefault(k, []).append(v)
        else:
            bare.add(body)
    return ns, bare


def parse_registered_runners(path):
    """Simple class names of every runner registered in the suite, or None if the suite is missing.

    XML comments are stripped FIRST: a deliberately parked runner is commented out rather than deleted
    (so the line can be restored with its rationale), and matching inside a comment would report it as
    registered -- exactly the false credit this check exists to prevent.
    """
    if not os.path.isfile(path):
        return None
    with open(path, encoding="utf-8") as fh:
        text = XML_COMMENT_RE.sub("", fh.read())
    return {fqcn.rsplit(".", 1)[-1] for fqcn in CLASS_RE.findall(text)}


def parse_runner_features(root):
    """Map runner simple class name -> list of .feature paths from its @CucumberOptions(features=...).

    Java comments are stripped so a commented-out feature entry (or a javadoc example) is not read as
    a live registration, mirroring the XML treatment above.
    """
    mapping = {}
    if not os.path.isdir(root):
        return mapping
    for dirpath, _, files in os.walk(root):
        for fn in sorted(files):
            if not fn.endswith("Runner.java"):
                continue
            with open(os.path.join(dirpath, fn), encoding="utf-8") as fh:
                text = strip_java_comments(fh.read())
            m = FEATURES_OPT_RE.search(text)
            # A CONCRETE runner with no resolvable features option is a parse failure, not a runner that
            # declares none -- say so loudly rather than letting it read as a genuine coverage gap. The
            # abstract base carries no @CucumberOptions by design, so it is not a finding.
            if not m and not ABSTRACT_CLASS_RE.search(text):
                print("WARNING: %s declares no parseable `features = ...` option; "
                      "its scenarios will read as NOT EXECUTED" % fn, file=sys.stderr)
            mapping[fn[:-len(".java")]] = FEATURE_PATH_RE.findall(m.group(1)) if m else []
    return mapping


def feature_rel(path):
    """A runner's declared feature path -> the same form scan_features reports (relative to features/)."""
    norm = path.replace("\\", "/")
    marker = "features/"
    i = norm.find(marker)
    return norm[i + len(marker):] if i >= 0 else norm


def executed_feature_files(testng_path, runners_dir):
    """Feature files reachable from a REGISTERED runner.

    Returns (executed, registered, unresolved). `executed` is None when the chain can't be resolved at
    all -- meaning "unknown", which disables the check (so the tree still renders on a partial checkout)
    rather than silently declaring everything unexecuted.

    `unresolved` is the registered class names for which NO runner source was found. These must be
    reported SEPARATELY from parked scenarios: a registration whose class cannot be located contributes
    zero features, so its scenarios surface as `✗ NOT RUN` and read as "no runner runs this feature"
    when the truth is "the suite names a runner this scan could not resolve" -- a renamed/moved class, or
    a `--runners` path narrower than the suite. Those two diagnoses have OPPOSITE fixes (register the
    feature vs. fix the registration/path), so conflating them sends the reader the wrong way.
    """
    registered = parse_registered_runners(testng_path)
    if registered is None:
        return None, None, set()
    runner_features = parse_runner_features(runners_dir)
    if not runner_features:
        return None, None, set()
    executed = set()
    unresolved = set()
    for cls in registered:
        if cls not in runner_features:
            unresolved.add(cls)
            continue
        for f in runner_features[cls]:
            executed.add(feature_rel(f))
    return executed, registered, unresolved


def scan_features(root):
    """Yield dict per scenario: file, line, name, ns(tags), bare(tags)."""
    scenarios = []
    if not os.path.isdir(root):
        return scenarios
    # dirnames is sorted IN PLACE so os.walk descends in name order. Without it the traversal follows
    # filesystem order, which differs between a contributor's machine and the CI runner, so the rendered
    # tree's within-bucket ordering was not reproducible.
    for dirpath, dirnames, files in os.walk(root):
        dirnames.sort()
        for fn in sorted(files):
            if not fn.endswith(".feature"):
                continue
            path = os.path.join(dirpath, fn)
            rel = os.path.relpath(path, root)
            feat_ns, feat_bare = {}, set()
            pend_ns, pend_bare = {}, set()
            seen_feature = False
            with open(path, encoding="utf-8") as fh:
                for i, raw in enumerate(fh, 1):
                    s = raw.strip()
                    if s.startswith("@"):
                        ns, bare = parse_tags(s)
                        for k, v in ns.items():
                            pend_ns.setdefault(k, []).extend(v)
                        pend_bare |= bare
                        continue
                    if FEATURE_RE.match(s):
                        feat_ns, feat_bare = pend_ns, pend_bare
                        pend_ns, pend_bare = {}, set()
                        seen_feature = True
                        continue
                    m = SCENARIO_RE.match(s)
                    if m:
                        ns = {k: list(v) for k, v in feat_ns.items()}
                        for k, v in pend_ns.items():
                            ns.setdefault(k, []).extend(v)
                        bare = set(feat_bare) | pend_bare
                        scenarios.append({
                            "file": rel, "line": i, "name": m.group(2).strip() or "(unnamed)",
                            "ns": ns, "bare": bare,
                        })
                        pend_ns, pend_bare = {}, set()
    return scenarios


def classify(scenarios, caps, executed=None):
    """Return (placed, excluded, invalid, unrun).

    placed/unrun are both {cap:{feat:[entries]}}; a correctly-tagged scenario lands in `unrun` instead
    of `placed` when `executed` is a known set and its file is not in it, so it is never counted as
    coverage. Tag validity is judged FIRST -- a scenario that is both mis-tagged and unexecuted is a
    tagging bug, which is the actionable root cause.
    """
    placed = {c: {f: [] for f in caps[c]["features"]} for c in caps}
    unrun = {c: {f: [] for f in caps[c]["features"]} for c in caps}
    excluded, invalid = [], []
    for sc in scenarios:
        is_setup_file = os.path.basename(sc["file"]).startswith(SETUP_PREFIX)
        has_setup_tag = "setup" in sc["bare"]
        if is_setup_file != has_setup_tag:
            invalid.append((sc, "file is _setup_* but scenario is not tagged @setup" if is_setup_file
                            else "scenario tagged @setup but file is not named _setup_*"))
            continue
        excl = sc["bare"] & EXCLUSION
        if excl:
            excluded.append((sc, sorted(excl)))
            continue
        caps_v = sc["ns"].get("cap", [])
        feats_v = sc["ns"].get("feat", [])
        reasons = []
        if len(caps_v) != 1:
            reasons.append("missing @cap" if not caps_v else "multiple @cap")
        if len(feats_v) != 1:
            reasons.append("missing @feat" if not feats_v else "multiple @feat")
        if len(caps_v) == 1 and len(feats_v) == 1:
            c, f = caps_v[0], feats_v[0]
            if c not in caps:
                reasons.append(f"unknown @cap:{c}")
            elif f not in caps[c]["features"]:
                reasons.append(f"@feat:{f} not under @cap:{c}")
        for t in sc["ns"].get("type", []):
            if t not in VALID_TYPES:
                reasons.append(f"invalid @type:{t}")
        for d in sc["ns"].get("dep", []):
            if d not in caps:
                reasons.append(f"unknown @dep:{d}")
        if reasons:
            invalid.append((sc, "; ".join(reasons)))
        elif executed is not None and sc["file"].replace("\\", "/") not in executed:
            unrun[caps_v[0]][feats_v[0]].append(sc)
        else:
            placed[caps_v[0]][feats_v[0]].append(sc)
    return placed, excluded, invalid, unrun


def render(caps, placed, excluded, invalid, unrun):
    out = ["# integration-v2 coverage tree",
           "",
           "_Generated by `render_coverage_tree.py` from `capability-map.yml` + `.feature` tags, with "
           "execution verified against `testng-v2.xml`. Do not edit by hand._ Empty branches are "
           "uncovered features (gaps to fill by eye).",
           ""]
    total = sum(len(v) for c in placed.values() for v in c.values())
    unrun_n = sum(len(v) for c in unrun.values() for v in c.values())
    line = (f"**{total}** product scenarios covered · **{len(excluded)}** excluded "
            f"(infra/framework/migration/setup) · **{len(invalid)}** unmapped/invalid")
    line += f" · **{unrun_n}** NOT EXECUTED." if unrun_n else "."
    out.append(line)
    if unrun_n:
        out.append("")
        out.append(f"> {unrun_n} correctly-tagged scenario(s) are marked `✗ NOT RUN` below: no runner "
                   "registered in `testng-v2.xml` executes their feature, so they are **excluded from "
                   "the covered count**. See the section at the end for why each one is parked.")
    out.append("")
    out.append("```text")
    out.append("integration-v2 product tests")
    cap_ids = list(caps)
    for ci, c in enumerate(cap_ids):
        cap_last = ci == len(cap_ids) - 1
        cb = "└── " if cap_last else "├── "
        cont = "    " if cap_last else "│   "
        ccount = sum(len(placed[c][f]) for f in caps[c]["features"])
        out.append(f"{cb}{c}  — {caps[c]['name']}  ({ccount})")
        feats = list(caps[c]["features"])
        for fi, f in enumerate(feats):
            f_last = fi == len(feats) - 1
            fb = "└── " if f_last else "├── "
            fcont = "    " if f_last else "│   "
            scs = placed[c][f]
            dead = unrun[c][f]
            label = f"{f}  — {caps[c]['features'][f]}"
            if not scs and not dead:
                out.append(f"{cont}{fb}{label}  (—)")
                continue
            # The count is COVERED scenarios only; anything unexecuted is called out separately so a
            # branch can never look populated on the strength of tests that never run.
            suffix = f"  ({len(scs) if scs else '—'}" + (f", ✗{len(dead)} NOT RUN)" if dead else ")")
            out.append(f"{cont}{fb}{label}{suffix}")
            rows = [(sc, False) for sc in scs] + [(sc, True) for sc in dead]
            for si, (sc, is_dead) in enumerate(rows):
                s_last = si == len(rows) - 1
                sb = "└── " if s_last else "├── "
                rule = sc["ns"].get("rule", [])
                rtag = f"[{rule[0]}] " if rule else ""
                mark = "✗ NOT RUN — " if is_dead else ""
                out.append(f"{cont}{fcont}{sb}{mark}{rtag}{sc['name']}  ({sc['file']}:{sc['line']})")
    out.append("```")
    out.append("")
    if invalid:
        out.append("## Unmapped / invalid (fix these — lint fails)")
        out.append("")
        for sc, reason in invalid:
            out.append(f"- `{sc['file']}:{sc['line']}` — {sc['name']} — **{reason}**")
        out.append("")
    dead_all = [(sc, c, f) for c in unrun for f in unrun[c] for sc in unrun[c][f]]
    if dead_all:
        out.append(f"## Not executed ({len(dead_all)}) — tagged, but no registered runner runs them")
        out.append("")
        out.append("These are NOT counted as coverage. Either register a runner for the feature in "
                   "`testng-v2.xml`, or delete the scenarios if they are obsolete — a parked scenario "
                   "should carry a comment in the suite saying what unblocks it.")
        out.append("")
        for sc, c, f in sorted(dead_all, key=lambda t: (t[0]["file"], t[0]["line"])):
            out.append(f"- `{sc['file']}:{sc['line']}` — {sc['name']} — @cap:{c} @feat:{f}")
        out.append("")
    if excluded:
        out.append(f"## Excluded ({len(excluded)})")
        out.append("")
        for sc, marks in excluded:
            out.append(f"- `{sc['file']}:{sc['line']}` — {sc['name']} — @{', @'.join(marks)}")
        out.append("")
    return "\n".join(out) + "\n"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--map", default=DEFAULT_MAP)
    ap.add_argument("--features", default=DEFAULT_FEATURES)
    ap.add_argument("--out", default=DEFAULT_OUT)
    ap.add_argument("--testng", default=DEFAULT_TESTNG,
                    help="suite XML used to verify a scenario is actually executed")
    ap.add_argument("--runners", default=DEFAULT_RUNNERS,
                    help="runner source root, for the runner->feature mapping")
    args = ap.parse_args()

    caps = parse_capability_map(args.map)
    scenarios = scan_features(args.features)
    executed, registered, unresolved = executed_feature_files(args.testng, args.runners)
    placed, excluded, invalid, unrun = classify(scenarios, caps, executed)
    with open(args.out, "w", encoding="utf-8") as fh:
        fh.write(render(caps, placed, excluded, invalid, unrun))

    placed_n = sum(len(v) for c in placed.values() for v in c.values())
    unrun_n = sum(len(v) for c in unrun.values() for v in c.values())
    print(f"capabilities: {len(caps)} | scenarios: {len(scenarios)} | "
          f"covered: {placed_n} | excluded: {len(excluded)} | invalid: {len(invalid)} | "
          f"not-executed: {unrun_n}")
    if executed is None:
        print("WARNING: could not resolve the testng-v2.xml -> runner -> feature chain; execution was "
              "NOT verified and every tagged scenario is counted as covered", file=sys.stderr)
    else:
        print(f"execution verified against {len(registered) - len(unresolved)} registered runner(s) "
              f"covering {len(executed)} feature file(s)"
              + (f" | {len(unresolved)} registration(s) UNRESOLVED" if unresolved else ""))
    print(f"wrote {os.path.relpath(args.out)}")
    if unresolved:
        # Printed before the NOT-RUN note because it is a cause of it. Warning, not lint failure: a
        # --runners path narrower than the suite is a legitimate way to run this script.
        shown = sorted(unresolved)[:10]
        print(f"WARNING: {len(unresolved)} registered runner(s) could not be resolved to a source file "
              f"under {os.path.relpath(args.runners)} and contributed NO features: "
              + ", ".join(shown)
              + (f" (+{len(unresolved) - len(shown)} more)" if len(unresolved) > len(shown) else "")
              + ". Any 'Not executed' scenario below may be a consequence of this, not a parked test.",
              file=sys.stderr)
    if unrun_n:
        # Deliberately NOT a lint failure: parking a scenario pending a product fix is legitimate. It is
        # loud on stderr and excluded from the count, which is what stops it being mistaken for coverage.
        print(f"NOTE: {unrun_n} tagged scenario(s) are not executed by any registered runner "
              f"(excluded from the covered count; see the 'Not executed' section)", file=sys.stderr)
    if invalid:
        print(f"LINT FAILED: {len(invalid)} unmapped/invalid scenario(s)", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
