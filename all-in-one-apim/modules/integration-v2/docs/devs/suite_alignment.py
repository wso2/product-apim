#!/usr/bin/env python3
"""Validate the two canonical product suites and their block-scoped exceptions, without writes."""

import argparse
from collections import Counter
from dataclasses import dataclass, field
from pathlib import Path
import re
import sys
from xml.parsers import expat


HERE = Path(__file__).resolve().parent
REPO_ROOT = HERE.parents[4]
RESOURCES = HERE.parents[1] / "tests-integration/cucumber-tests/src/test/resources"
IGNORE_REFERENCE = "docs/devs/suite-alignment.ignore"
CLASS_NAME = re.compile(r"[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*", re.ASCII)
BLOCK_NAME = re.compile(r"[A-Za-z0-9][A-Za-z0-9_.-]*")
PARAMETER_NAME = re.compile(r"[A-Za-z][A-Za-z0-9_.-]*")
UNSUPPORTED = {"packages", "package", "groups", "methods", "include", "exclude",
               "suite-files", "suite-file", "method-selectors", "method-selector", "script"}
FILE_PARAMETERS = {base + suffix for base in ("tomlExtraOverlayPath", "serverFilesToCopy")
                   for suffix in ("", ".cp", ".tm", ".gateway")}
LIFECYCLE = "org.wso2.am.integration.cucumbertests.utils.listeners."


@dataclass
class Node:
    tag: str
    attrs: dict
    line: int
    children: list = field(default_factory=list)

    def elements(self, tag):
        return [child for child in self.children if child.tag == tag]


@dataclass
class Problem:
    path: Path
    line: int
    message: str
    fix: str


@dataclass
class ExceptionEntry:
    block: str
    kind: str
    target: str
    missing: str
    line: int
    used: bool = False


@dataclass
class Block:
    node: Node
    key: str
    parameters: dict
    classes: list


@dataclass
class Suite:
    path: Path
    node: Node
    blocks: dict


@dataclass
class Result:
    problems: list = field(default_factory=list)
    paired: int = 0
    matched: int = 0
    declared: int = 0
    undeclared: int = 0
    stale: int = 0

    def summary(self):
        return (f"suite-alignment: {self.paired} paired blocks · {self.matched} matched runner entries"
                f" · {self.declared} declared differences · {self.undeclared} undeclared · {self.stale} stale")

    def problem(self, path, node, message, fix):
        self.problems.append(Problem(path, node.line if isinstance(node, Node) else node, message, fix))


def read_xml(path, result):
    """Expat preserves source lines and does not fetch the suite's external DTD."""
    parser = expat.ParserCreate()
    stack, roots = [], []

    def start(tag, attrs):
        node = Node(tag, attrs, parser.CurrentLineNumber)
        (stack[-1].children if stack else roots).append(node)
        stack.append(node)

    parser.StartElementHandler = start
    parser.EndElementHandler = lambda tag: stack.pop()
    try:
        with path.open("rb") as source:
            parser.ParseFile(source)
    except (OSError, expat.ExpatError) as error:
        result.problem(path, getattr(error, "lineno", 1), f"Cannot read suite XML: {error}",
                       "Provide a readable, well-formed product suite XML file.")
        return None
    if not roots or roots[0].tag != "suite":
        result.problem(path, 1, "Expected a <suite> root.", "Use a TestNG product suite XML file.")
        return None
    return roots[0]


def check_selectors(path, node, result):
    if node.tag in UNSUPPORTED:
        result.problem(path, node, f"Unsupported suite selector <{node.tag}>.",
                       "Use explicit <test><classes><class name=\"FQCN\"/> registrations; "
                       "extend the validator before introducing execution selectors.")
        return
    if any(name in node.attrs for name in ("groups", "included-groups", "excluded-groups",
                                           "depends-on-groups", "depends-on-methods")):
        result.problem(path, node, "Unsupported group or method selector attribute.",
                       "Use explicit unfiltered runner registrations.")
    if node.attrs.get("enabled", "true").lower() != "true":
        result.problem(path, node, "Unsupported disabled suite registration.",
                       "Remove the registration and declare any topology omission with a reason.")
    if node.tag == "class" and not CLASS_NAME.fullmatch(node.attrs.get("name", "")):
        result.problem(path, node, "Unsupported class pattern or invalid class name.",
                       "Register each runner using its literal fully qualified class name.")
    for child in node.children:
        check_selectors(path, child, result)


def parameters(path, node, result):
    values = {}
    for parameter in node.elements("parameter"):
        name = parameter.attrs.get("name", "")
        if not name or "value" not in parameter.attrs:
            result.problem(path, parameter, "Parameter requires a name and value.", "Correct the parameter declaration.")
        elif name in values:
            result.problem(path, parameter, f"Duplicate local parameter '{name}'.", "Keep one declaration per scope.")
        else:
            values[name] = parameter
    return values


def read_suite(path, distributed, result):
    root = read_xml(path, result)
    if root is None:
        return None
    check_selectors(path, root, result)
    blocks = {}
    prefix = "IntegrationV2-Distributed-" if distributed else "IntegrationV2-"
    for node in root.elements("test"):
        name = node.attrs.get("name", "")
        if not name.startswith(prefix) or (not distributed and name.startswith("IntegrationV2-Distributed-")):
            result.problem(path, node, f"Block name '{name}' must start with '{prefix}'.", "Use the topology's block prefix.")
            continue
        key = name[len(prefix):]
        if not key or key in blocks:
            result.problem(path, node, f"Empty or duplicate mapped block '{key}'.", "Use a unique nonempty block name.")
            continue
        local = parameters(path, node, result)
        classes = [item for group in node.elements("classes") for item in group.elements("class")]
        for item in classes:
            if item.elements("parameter"):
                result.problem(path, item, "Unsupported class-local parameters.", "Keep framework parameters local to the block.")
        for name, parameter in local.items():
            if name in FILE_PARAMETERS and (distributed == ("." not in name)):
                result.problem(path, parameter, f"Unexpected topology configuration parameter '{name}'.",
                               "Use unqualified APIM file parameters in all-in-one and .cp/.tm/.gateway in distributed.")
        blocks[key] = Block(node, key, local, classes)
    if not blocks:
        result.problem(path, root, "Suite contains no explicit product blocks.", "Register the product blocks explicitly.")
    return Suite(path, root, blocks)


def read_ignores(path, suites, result):
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except (OSError, UnicodeError) as error:
        result.problem(path, 1, f"Cannot read suite alignment ignore file: {error}", "Provide the block-scoped ignore file.")
        return []
    entries, seen = [], set()
    syntax = re.compile(r"(\S+)\s+([^\s:]+):(\S+)\s+missing-in=(all-in-one|distributed)\s*\|\s*(.*)")
    for number, raw in enumerate(lines, 1):
        text = raw.strip()
        if not text or text.startswith("#"):
            continue
        match = syntax.fullmatch(text)
        if not match:
            result.problem(path, number, "Invalid suite alignment ignore entry.",
                           "Use '<block> runner:<class>|parameter:<name> missing-in=<all-in-one|distributed> | <reason>' "
                           "with one kind and a nonblank reason.")
            continue
        block, kind, target, missing, reason = match.groups()
        if kind not in {"runner", "parameter"}:
            error = f"Unknown ignore kind '{kind}'."
        elif not reason.strip():
            error = "Ignore entry requires a nonblank reason."
        elif (not BLOCK_NAME.fullmatch(block) or block.startswith("IntegrationV2-")
              or not (CLASS_NAME if kind == "runner" else PARAMETER_NAME).fullmatch(target)):
            error = "Ignore entries require literal block/target names without prefixes or wildcards."
        else:
            error = None
        if error:
            result.problem(path, number, error, "Correct the block-scoped declaration and provide its reason.")
            continue
        if kind == "runner" and "." not in target:
            candidates = {item.attrs["name"] for suite in suites if suite and block in suite.blocks
                          for item in suite.blocks[block].classes if item.attrs.get("name", "").rsplit(".", 1)[-1] == target}
            if len(candidates) > 1:
                result.problem(path, number, f"Ambiguous runner name '{target}' in block '{block}'.",
                               "Use the fully qualified runner class name.")
                continue
            if candidates:
                target = candidates.pop()
        key = (block, kind, target)
        if key in seen:
            result.problem(path, number, f"Duplicate ignore entry for {block} {kind}:{target}.",
                           "Keep one declaration for this block and target.")
            continue
        seen.add(key)
        entries.append(ExceptionEntry(block, kind, target, missing, number))
    return entries


def validate(all_in_one, distributed, ignore):
    result = Result()
    a = read_suite(Path(all_in_one), False, result)
    d = read_suite(Path(distributed), True, result)
    entries = read_ignores(Path(ignore), (a, d), result)
    if a is None or d is None:
        return result

    def difference(path, node, message, fix, block=None, kind=None, target=None, missing=None):
        for entry in entries:
            if (entry.block, entry.kind, entry.target, entry.missing) == (block, kind, target, missing):
                entry.used = True
                result.declared += 1
                return True
        result.undeclared += 1
        result.problem(path, node, message, fix)
        return False

    def compare_attributes(left, right, path, subject):
        for name in sorted((left.attrs.keys() | right.attrs.keys()) - {"name"}):
            if left.attrs.get(name) != right.attrs.get(name):
                difference(path, right, f"{subject} attribute '{name}' differs: all-in-one={left.attrs.get(name)!r}, "
                           f"distributed={right.attrs.get(name)!r}.", "Align the scheduling attributes in both suites.")

    def compare_parameters(left, right, left_path, right_path, block=None):
        excluded = FILE_PARAMETERS | {"blockLabel", "apim.topology"}
        for name in sorted((left.keys() | right.keys()) - excluded):
            ln, rn = left.get(name), right.get(name)
            if ln is not None and rn is not None:
                if ln.attrs["value"] != rn.attrs["value"]:
                    difference(right_path, rn, f"{block or 'Suite'} parameter '{name}' differs: "
                               f"all-in-one={ln.attrs['value']!r}, distributed={rn.attrs['value']!r}.",
                               "Align the local parameter values in both suites.")
            else:
                missing = "distributed" if rn is None else "all-in-one"
                source, path = (ln, left_path) if rn is None else (rn, right_path)
                difference(path, source, f"{block or 'Suite'} parameter '{name}' is missing in {missing}.",
                           f"Add it to the {missing} block, or declare it in {IGNORE_REFERENCE} with a reason."
                           if block else "Align suite-level parameters in both suites.",
                           block, "parameter", name, missing)

    compare_attributes(a.node, d.node, d.path, "Suite")
    compare_parameters(parameters(a.path, a.node, result), parameters(d.path, d.node, result),
                       a.path, d.path)
    listeners = lambda suite: [item.attrs.get("class-name") for group in suite.node.elements("listeners")
                               for item in group.elements("listener")]
    expected = [LIFECYCLE + "DistributedLifecycleListener" if item == LIFECYCLE + "BlockLifecycleListener" else item
                for item in listeners(a)]
    if expected != listeners(d):
        difference(d.path, d.node.elements("listeners")[0] if d.node.elements("listeners") else d.node,
                   "Suite listeners differ beyond the topology lifecycle listener.",
                   "Align listener order and use DistributedLifecycleListener in distributed.")
    common = a.blocks.keys() & d.blocks.keys()
    result.paired = len(common)
    for key in sorted(a.blocks.keys() | d.blocks.keys()):
        if key not in common:
            source = a if key in a.blocks else d
            missing = "distributed" if source is a else "all-in-one"
            difference(source.path, source.blocks[key].node, f"Block '{key}' is missing in {missing}.",
                       f"Add the matching block to the {missing} suite.")
    ao, do = [key for key in a.blocks if key in common], [key for key in d.blocks if key in common]
    if ao != do:
        key = next(right for left, right in zip(ao, do) if left != right)
        difference(d.path, d.blocks[key].node, f"Block order differs: all-in-one={ao}, distributed={do}.",
                   "Align block declaration order in both suites.")
    for key in ao:
        left, right = a.blocks[key], d.blocks[key]
        compare_attributes(left.node, right.node, d.path, key)
        al, dl = left.parameters.get("blockLabel"), right.parameters.get("blockLabel")
        if al is None or dl is None or dl.attrs["value"] != "distributed-" + al.attrs["value"]:
            difference(d.path, dl or right.node, f"{key} blockLabel must be 'distributed-' plus its all-in-one label.",
                       "Declare both blockLabel parameters with the documented topology prefix.")
        compare_parameters(left.parameters, right.parameters, a.path, d.path, key)
        ac, dc = Counter(item.attrs.get("name", "") for item in left.classes), Counter(item.attrs.get("name", "") for item in right.classes)
        result.matched += sum((ac & dc).values())
        for name in sorted(ac.keys() | dc.keys()):
            if ac[name] == dc[name]:
                continue
            source, suite = (left, a) if ac[name] > dc[name] else (right, d)
            occurrence = min(ac[name], dc[name])
            node = [item for item in source.classes if item.attrs.get("name", "") == name][occurrence]
            missing = "distributed" if dc[name] == 0 else "all-in-one" if ac[name] == 0 else None
            difference(suite.path, node, f"{key} runner '{name}' registration count differs: "
                       f"all-in-one={ac[name]}, distributed={dc[name]}.",
                       f"Add it to the {missing} block, or declare it in {IGNORE_REFERENCE} with a reason."
                       if missing else "Align the runner registration multiplicities in both blocks.",
                       key, "runner", name, missing)
        shared = ac & dc

        def shared_order(block):
            remaining, order = shared.copy(), []
            for item in block.classes:
                name = item.attrs.get("name", "")
                if remaining[name]:
                    order.append(name)
                    remaining[name] -= 1
            return order

        lo, ro = shared_order(left), shared_order(right)
        if lo != ro:
            index = next(i for i, names in enumerate(zip(lo, ro)) if names[0] != names[1])
            node = next(item for item in right.classes if item.attrs.get("name") == ro[index])
            difference(d.path, node, f"{key} runner order differs: all-in-one={lo}, distributed={ro}.",
                       "Align runner declaration order in both blocks.")
    for entry in entries:
        if not entry.used:
            result.stale += 1
            result.problem(Path(ignore), entry.line, f"Stale ignore entry: {entry.block} {entry.kind}:{entry.target} "
                           f"missing-in={entry.missing} is not present as a difference.",
                           "Remove the stale declaration or correct its exact block, target and missing topology.")
    return result


def emit(result):
    import os

    def escape(text, property_value=False):
        text = text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
        return text.replace(",", "%2C").replace(":", "%3A") if property_value else text

    for problem in result.problems:
        path = Path(os.path.relpath(problem.path.resolve(), REPO_ROOT)).as_posix()
        print(f"::error file={escape(path, True)},line={problem.line}::{escape(problem.message)}")
        print(f"{path}:{problem.line}: {problem.message}\nFix: {problem.fix}")
    print(result.summary())


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--all-in-one", type=Path, default=RESOURCES / "testng-v2.xml")
    parser.add_argument("--distributed", type=Path, default=RESOURCES / "testng-v2_distributed.xml")
    parser.add_argument("--ignore", type=Path, default=HERE / "suite-alignment.ignore")
    args = parser.parse_args(argv)
    result = validate(args.all_in_one, args.distributed, args.ignore)
    emit(result)
    return 1 if result.problems else 0


if __name__ == "__main__":
    sys.exit(main())
