#!/usr/bin/env python3
"""Render the integration-v2 coverage tree from capability-map.yml + .feature tags, and lint tags.

Single pass: builds a Markdown capability->feature->scenario tree (empty branches shown as gaps) and
validates every product scenario's tags against the curated skeleton. Invalid/untagged product
scenarios go to an "Unmapped / invalid" bucket; the script exits non-zero if that bucket is non-empty
so it can be wired as a CI gate. Non-product scenarios (@infra/@framework/@migration/@setup) are
excluded from the tree. @setup marks reusable prerequisite features and is bidirectional with the
`_setup_` filename prefix (one without the other is a lint violation). See TAGGING-CONVENTIONS.md.

Dependency-free (stdlib only). capability-map.yml must keep its simple documented shape.

Usage:
  python3 render_coverage_tree.py [--map capability-map.yml] [--features <dir>] [--out coverage-tree.md]
"""
import argparse, os, re, sys
from collections import OrderedDict

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_MAP = os.path.join(HERE, "capability-map.yml")
DEFAULT_FEATURES = os.path.normpath(
    os.path.join(HERE, "..", "..", "tests-integration", "cucumber-tests", "src", "test", "resources", "features"))
DEFAULT_OUT = os.path.join(HERE, "coverage-tree.md")

EXCLUSION = {"infra", "framework", "migration", "setup"}
SETUP_PREFIX = "_setup_"
VALID_TYPES = {"smoke", "negative", "regression"}
SCENARIO_RE = re.compile(r"^\s*(Scenario Outline|Scenario):\s*(.*)$")
FEATURE_RE = re.compile(r"^\s*Feature:\s*(.*)$")


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
                caps[cur]["name"] = content[len("name:"):].strip()
            elif indent == 4 and content.startswith("features:"):
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


def scan_features(root):
    """Yield dict per scenario: file, line, name, ns(tags), bare(tags)."""
    scenarios = []
    if not os.path.isdir(root):
        return scenarios
    for dirpath, _, files in os.walk(root):
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


def classify(scenarios, caps):
    """Return (placed: {cap:{feat:[entries]}}, excluded:[...], invalid:[(entry,reason)])."""
    placed = {c: {f: [] for f in caps[c]["features"]} for c in caps}
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
        else:
            placed[caps_v[0]][feats_v[0]].append(sc)
    return placed, excluded, invalid


def render(caps, placed, excluded, invalid):
    out = ["# integration-v2 coverage tree",
           "",
           "_Generated by `render_coverage_tree.py` from `capability-map.yml` + `.feature` tags. "
           "Do not edit by hand._ Empty branches are uncovered features (gaps to fill by eye).",
           ""]
    total = sum(len(v) for c in placed.values() for v in c.values())
    out.append(f"**{total}** product scenarios placed · **{len(excluded)}** excluded "
               f"(infra/framework/migration/setup) · **{len(invalid)}** unmapped/invalid.")
    out.append("")
    out.append("```")
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
            label = f"{f}  — {caps[c]['features'][f]}"
            if not scs:
                out.append(f"{cont}{fb}{label}  (—)")
                continue
            out.append(f"{cont}{fb}{label}  ({len(scs)})")
            for si, sc in enumerate(scs):
                s_last = si == len(scs) - 1
                sb = "└── " if s_last else "├── "
                rule = sc["ns"].get("rule", [])
                rtag = f"[{rule[0]}] " if rule else ""
                out.append(f"{cont}{fcont}{sb}{rtag}{sc['name']}  ({sc['file']}:{sc['line']})")
    out.append("```")
    out.append("")
    if invalid:
        out.append("## Unmapped / invalid (fix these — lint fails)")
        out.append("")
        for sc, reason in invalid:
            out.append(f"- `{sc['file']}:{sc['line']}` — {sc['name']} — **{reason}**")
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
    args = ap.parse_args()

    caps = parse_capability_map(args.map)
    scenarios = scan_features(args.features)
    placed, excluded, invalid = classify(scenarios, caps)
    with open(args.out, "w", encoding="utf-8") as fh:
        fh.write(render(caps, placed, excluded, invalid))

    placed_n = sum(len(v) for c in placed.values() for v in c.values())
    print(f"capabilities: {len(caps)} | scenarios: {len(scenarios)} | "
          f"placed: {placed_n} | excluded: {len(excluded)} | invalid: {len(invalid)}")
    print(f"wrote {os.path.relpath(args.out)}")
    if invalid:
        print(f"LINT FAILED: {len(invalid)} unmapped/invalid scenario(s)", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
