#!/usr/bin/env python3
"""
V2 Gherkin feature-linter — enforces the CLAUDE.md .feature rules that Checkstyle/PMD structurally cannot see
(they parse Java, not Gherkin). See docs/devs/static-analysis-plan.md §4 (G-1..G-10).

Reads the closed @cap/@feat vocabulary from capability-map.yml. Prints one line per violation
(`<file>:<line>: [G-x] <message>`) and exits non-zero if any are found (so CI can gate on it).

Ratchet: pass --warn to always exit 0 (report-only) during the warning phase.

Implemented this pass: G-1 (tag vocabulary + cardinality), G-2 (@cap vs folder, @dep exception),
G-3 (unique Feature titles), G-4 (_setup_ <-> @setup), G-9 (trailing whitespace / EOF newline),
G-10 (no prose line starting with @). G-5/G-6/G-7/G-8 (Background dedup, one-behaviour, assert-the-effect,
deploy-needs-status) are heuristic/semantic and tracked as a follow-up.
"""
import os
import re
import sys
import glob
import yaml

HERE = os.path.dirname(os.path.abspath(__file__))
MODULE = os.path.abspath(os.path.join(HERE, "..", ".."))
FEATURES_DIR = os.path.join(
    MODULE, "tests-integration", "cucumber-tests", "src", "test", "resources", "features")
CAP_MAP = os.path.join(HERE, "capability-map.yml")

# Non-product exclusion markers: a feature/scenario carrying one of these is excluded from the product tree
# and from the @cap/@feat product rules (CLAUDE.md §3/§10).
NONPRODUCT = {"infra", "framework", "migration", "setup"}
# Folders that are non-product homes (no @cap expected).
NONPRODUCT_FOLDERS = {"common", "framework-verification", "migration"}

TAG_RE = re.compile(r"@[\w:.\-]+")


def load_vocab():
    data = yaml.safe_load(open(CAP_MAP))
    caps = {}
    for cap, body in (data.get("capabilities") or {}).items():
        caps[cap] = set(((body or {}).get("features") or {}).keys())
    return caps


class Scenario:
    def __init__(self, line, name):
        self.line = line
        self.name = name
        self.tags = []          # (line, tag) effective = feature + own
        self.own_tag_lines = []  # (line, raw) own tag lines only
        self.steps = []          # (line, keyword, text)


def parse_feature(path):
    """Line-based Gherkin parse: feature-level tags, title, scenarios (effective tags + steps)."""
    lines = open(path, encoding="utf-8").read().split("\n")
    feature_tags = []           # (line, tag)
    feature_title = None
    feature_title_line = None
    scenarios = []
    desc_lines = []             # (line, text) between Feature: and first Scenario
    bad_tag_lines = []          # (line, raw) @-lines that are NOT pure tag lines (prose-as-tag) — G-10
    has_background = False       # whether the feature declares a Background — G-5
    pending = []                # (line, raw_line, tags) accumulated tag lines
    in_desc = False
    cur = None
    for i, raw in enumerate(lines, 1):
        s = raw.strip()
        if s.startswith("@"):
            # A legit tag line is only @tags; a line starting with @ that has a non-@ token is prose Gherkin
            # will mis-parse as a tag ("a tag may not contain whitespace").
            if all(tok.startswith("@") for tok in s.split()):
                pending.append((i, raw, TAG_RE.findall(s)))
            else:
                bad_tag_lines.append((i, raw))
            continue
        if s.startswith("Feature:"):
            feature_tags = [(ln, t) for (ln, _, ts) in pending for t in ts]
            feature_title = s[len("Feature:"):].strip()
            feature_title_line = i
            pending = []
            in_desc = True
            continue
        if s.startswith("Scenario:") or s.startswith("Scenario Outline:"):
            name = s.split(":", 1)[1].strip()
            cur = Scenario(i, name)
            own = [(ln, t) for (ln, _, ts) in pending for t in ts]
            cur.own_tag_lines = [(ln, raw2) for (ln, raw2, _) in pending]
            cur.tags = feature_tags + own
            scenarios.append(cur)
            pending = []
            in_desc = False
            continue
        if re.match(r"(Given|When|Then|And|But)\b", s) and cur is not None:
            kw = s.split(None, 1)[0]
            cur.steps.append((i, kw, s))
            in_desc = False
            continue
        if s.startswith("Background:"):
            has_background = True
            in_desc = False
            pending = []
            continue
        if s.startswith("Rule:") or s.startswith("Examples:"):
            in_desc = False
            pending = []
            continue
        if in_desc and s:
            desc_lines.append((i, raw))
    return {
        "path": path, "feature_tags": feature_tags, "title": feature_title,
        "title_line": feature_title_line, "scenarios": scenarios, "desc": desc_lines,
        "bad_tag_lines": bad_tag_lines, "has_background": has_background, "lines": lines,
    }


def tag_val(tags, key):
    vals = []
    for (_, t) in tags:
        if t.startswith("@" + key + ":"):
            vals.append(t.split(":", 1)[1])
    return vals


def has_tag_prefix(tags, key):
    return any(t == "@" + key or t.startswith("@" + key + ":") for (_, t) in tags)


_KW_RE = re.compile(r"^(Given|When|Then|And|But)\b")


def norm_step(text):
    """Normalize a step for duplicate detection: drop the leading keyword (And/But inherit context), lowercase,
    collapse whitespace. Keeps data literals so only genuine copy-paste (not distinct data) matches — G-11."""
    t = _KW_RE.sub("", text).strip().lower()
    return re.sub(r"\s+", " ", t)


# G-12: a raw literal that should be parameterized in a reusable _setup_ fixture — hardcoded tenant domain or an
# inline credential value. Placeholders ({{...}} / <...>) and the framework's super-tenant are exempt.
_TENANT_LITERAL_RE = re.compile(r'"([^"{}<>]*\.(?:com|org|net))"')
_CRED_STEP_RE = re.compile(r"\b(password|secret|api[- ]?key)\b", re.I)
_QUOTED_RE = re.compile(r'"([^"]*)"')


def lint():
    caps = load_vocab()
    files = sorted(glob.glob(os.path.join(FEATURES_DIR, "**", "*.feature"), recursive=True))
    violations = []  # (path, line, code, message)

    def v(path, line, code, msg):
        violations.append((path, line, code, msg))

    titles = {}  # title -> [(path, line)]
    scen_keys = {}  # normalized-step-sequence -> [(path, line, name)]  — G-11 duplicate scenarios

    for path in files:
        rel = os.path.relpath(path, FEATURES_DIR)
        folder = rel.split(os.sep)[0]
        fname = os.path.basename(path)
        f = parse_feature(path)

        # G-3: collect titles for cross-file uniqueness.
        if f["title"]:
            titles.setdefault(f["title"], []).append((path, f["title_line"]))

        # G-4: _setup_ filename <-> @setup tag (both directions); a @setup feature must not be @cleanup.
        is_setup_file = fname.startswith("_setup_")
        has_setup = has_tag_prefix(f["feature_tags"], "setup")
        if is_setup_file and not has_setup:
            v(path, f["title_line"] or 1, "G-4", "_setup_* file must carry the @setup tag")
        if has_setup and not is_setup_file:
            v(path, 1, "G-4", "@setup feature must be named _setup_* (filename/tag mismatch)")
        if has_setup and has_tag_prefix(f["feature_tags"], "cleanup"):
            v(path, 1, "G-4", "@setup feature must not be @cleanup (per-runner sweep, not per-scenario)")

        # G-10: a line starting with '@' that is not a pure tag line (has a non-@ token) is prose masquerading
        # as a tag — Gherkin parses it as a tag and fails ("a tag may not contain whitespace").
        for (ln, raw) in f["bad_tag_lines"]:
            v(path, ln, "G-10", "line starts with '@' but is not a pure tag line (prose parsed as a tag)")

        # G-9: trailing whitespace (any line) + single trailing newline at EOF.
        for ln, raw in enumerate(f["lines"], 1):
            if raw != raw.rstrip() and raw.strip() != "":
                v(path, ln, "G-9", "trailing whitespace")
        text = "\n".join(f["lines"])
        if not text.endswith("\n") and text.strip():
            v(path, len(f["lines"]), "G-9", "file must end with exactly one newline")
        elif text.endswith("\n\n"):
            v(path, len(f["lines"]), "G-9", "file has multiple trailing newlines")

        # G-5: 2+ scenarios sharing an identical leading step-sequence (>=3 steps) with no Background -> hoist it.
        if not f["has_background"] and not is_setup_file and len(f["scenarios"]) >= 2:
            from collections import defaultdict
            groups = defaultdict(list)
            for sc in f["scenarios"]:
                if len(sc.steps) >= 3:
                    key = tuple(text for (_, _, text) in sc.steps[:3])
                    groups[key].append(sc)
            for _key, scs in groups.items():
                if len(scs) >= 2:
                    v(path, scs[0].line, "G-5",
                      f"{len(scs)} scenarios share the same first 3 steps; hoist the shared setup into a Background")

        # G-8: a When/And deploy/state-change step should be followed (before the next When) by a status assertion.
        # (G-6 "one behaviour per scenario" was removed — the step-count heuristic can't tell a cohesive arc from
        # bundled behaviours (~90% FP); it stays a CLAUDE.md/review rule. Setup fixtures are excluded here.)
        deploy_re = re.compile(r"\b(un|re-?)?deploy\b", re.I)
        for sc in f["scenarios"]:
            markers = {t.lstrip("@").split(":")[0] for (_, t) in sc.tags}
            if bool(markers & NONPRODUCT) or folder in NONPRODUCT_FOLDERS or is_setup_file:
                continue
            for idx, (ln, kw, tx) in enumerate(sc.steps):
                if kw in ("When", "And") and deploy_re.search(tx):
                    followed = False
                    for (_, kw2, tx2) in sc.steps[idx + 1:]:
                        if kw2 == "When":
                            break
                        if kw2 in ("Then", "And") and "status code" in tx2.lower():
                            followed = True
                            break
                    if not followed:
                        v(path, ln, "G-8",
                          "deploy/state-change step has no following 'Then ... status code' assertion")

        # G-11: collect normalized full step-sequences to flag copy-paste duplicate scenarios across the module.
        # Skip Scenario Outlines (parameterized templates: any step carries a <param>) and _setup_ fixtures.
        for sc in f["scenarios"]:
            if len(sc.steps) < 2:
                continue
            if any("<" in tx and ">" in tx for (_, _, tx) in sc.steps):
                continue
            key = tuple(norm_step(tx) for (_, _, tx) in sc.steps)
            scen_keys.setdefault(key, []).append((path, sc.line, sc.name))

        # G-12: a reusable _setup_ fixture should parameterize actors/tenants, not bake in literals — flag a
        # hardcoded tenant domain, or an inline credential value, that isn't a {{placeholder}} or <param>.
        if is_setup_file:
            for sc in f["scenarios"]:
                for (ln, kw, tx) in sc.steps:
                    for m in _TENANT_LITERAL_RE.finditer(tx):
                        v(path, ln, "G-12",
                          f"hardcoded tenant literal \"{m.group(1)}\" in a _setup_ fixture; parameterize it")
                    if _CRED_STEP_RE.search(tx):
                        for lit in _QUOTED_RE.findall(tx):
                            if lit and "{{" not in lit and "<" not in lit:
                                v(path, ln, "G-12",
                                  f"hardcoded credential literal \"{lit}\" in a _setup_ fixture; parameterize it")

        # Per-scenario product rules (G-1, G-2).
        for sc in f["scenarios"]:
            markers = {t.lstrip("@").split(":")[0] for (_, t) in sc.tags}
            is_nonproduct = bool(markers & NONPRODUCT) or folder in NONPRODUCT_FOLDERS
            if is_nonproduct:
                continue
            capvals = tag_val(sc.tags, "cap")
            featvals = tag_val(sc.tags, "feat")
            # G-1: exactly one @cap and one @feat.
            if len(capvals) != 1:
                v(path, sc.line, "G-1", f"scenario must have exactly one @cap (found {len(capvals)}: {capvals})")
            if len(featvals) != 1:
                v(path, sc.line, "G-1", f"scenario must have exactly one @feat (found {len(featvals)}: {featvals})")
            # G-1: @cap/@feat must be in the closed vocabulary.
            for cv in capvals:
                if cv not in caps:
                    v(path, sc.line, "G-1", f"@cap:{cv} not in capability-map.yml")
            for cv in capvals:
                for fv in featvals:
                    if cv in caps and fv not in caps[cv]:
                        v(path, sc.line, "G-1", f"@feat:{fv} is not a feature of @cap:{cv} in capability-map.yml")
            # G-2: @cap should match the parent folder unless a @dep declares the cross-capability tie.
            if folder in caps and capvals and capvals[0] != folder:
                if not has_tag_prefix(sc.tags, "dep"):
                    v(path, sc.line, "G-2",
                      f"@cap:{capvals[0]} lives in folder '{folder}'; move it or add a @dep:{folder}")

    # G-3: duplicate Feature titles across the module.
    for title, occ in titles.items():
        if len(occ) > 1:
            for (path, line) in occ:
                others = ", ".join(os.path.relpath(p, FEATURES_DIR) for p, _ in occ if p != path)
                v(path, line or 1, "G-3", f"duplicate Feature title (also in: {others})")

    # G-11: two+ scenarios with an identical normalized step-sequence are copy-paste duplicates (fold into one, or
    # make a Scenario Outline if only the data differs).
    for _key, occ in scen_keys.items():
        if len(occ) > 1:
            for (path, line, name) in occ:
                others = ", ".join(f"{os.path.relpath(p, FEATURES_DIR)}:{ln}" for (p, ln, _) in occ
                                   if not (p == path and ln == line))
                v(path, line, "G-11", f"duplicate scenario '{name}' (same steps as: {others})")

    return violations, len(files)


# HARD rules gate the build (fail on violation) — the tree is clean for these. ADVISORY rules are heuristic with
# real false positives (measured), so they REPORT but never fail the build (per the "larger diff -> warning"
# decision): G-5 Background-dedup (mostly true positives) and G-8 deploy-needs-status (FP on self-asserting/browser
# steps). (G-6 one-behaviour was removed as ~90% FP — a CLAUDE.md/review rule now.) See static-analysis-plan.md.
HARD = {"G-1", "G-2", "G-3", "G-4", "G-9", "G-10"}


def main():
    warn = "--warn" in sys.argv          # never exit non-zero
    show_all = warn or "--all" in sys.argv  # print advisory details too (default hides them for a clean build log)
    violations, nfiles = lint()
    violations.sort(key=lambda x: (x[0], x[1]))
    from collections import Counter
    hard = [x for x in violations if x[2] in HARD]
    advisory = [x for x in violations if x[2] not in HARD]
    for (path, line, code, msg) in hard:
        print(f"{os.path.relpath(path, MODULE)}:{line}: [{code}] {msg}")
    if show_all:
        for (path, line, code, msg) in advisory:
            print(f"{os.path.relpath(path, MODULE)}:{line}: [{code}][advisory] {msg}")
    hby = Counter(c for (_, _, c, _) in hard)
    aby = Counter(c for (_, _, c, _) in advisory)
    print(f"\n[feature-lint] {len(hard)} hard violation(s) across {nfiles} feature files"
          + (": " + ", ".join(f"{k}={v}" for k, v in sorted(hby.items())) if hard else " (clean)")
          + (f"  |  advisory (non-gating): " + ", ".join(f"{k}={v}" for k, v in sorted(aby.items()))
             if advisory else ""), file=sys.stderr)
    if hard and not warn:
        sys.exit(1)


if __name__ == "__main__":
    main()
