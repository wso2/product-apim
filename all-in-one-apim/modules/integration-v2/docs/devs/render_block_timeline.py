#!/usr/bin/env python3
"""Append a passive TestNG block timeline to the GitHub job summary (stdlib only).

Input is target/block-timeline.json from BlockTimelineListener, schemaVersion 1.
Configuration failures colour runners/blocks red but stay outside scenario totals.
Each diagram is bounded independently; oversized detail maps are split. Reporting
errors, including missing JSON after a JVM failure, produce a note and exit zero.
"""

import argparse
import html
import json
import os
from pathlib import Path
import re
import sys

DEFAULT_INPUT = Path("tests-integration/cucumber-tests/target/block-timeline.json")
MAX_DIAGRAM_CHARS = 40_000
MAX_DIAGRAM_EDGES = 350
MAX_SUMMARY_BYTES = 900_000
STYLE = """  classDef pass fill:#ffffff,stroke:#1a7f37,stroke-width:1px,color:#1f2328,rx:8,ry:8;
  classDef fail fill:#ffebe9,stroke:#cf222e,stroke-width:1px,color:#82071e,rx:8,ry:8;
  classDef skip fill:#ffffff,stroke:#9a6700,stroke-width:1px,color:#1f2328,rx:8,ry:8;
  classDef neutral fill:#ffffff,stroke:#d1d9e0,stroke-width:1px,color:#1f2328,rx:8,ry:8;
  linkStyle default stroke:#8c959f,stroke-width:1px;
"""


def label(value, width=24):
    """Escape data labels and wrap long names without inserting Mermaid syntax."""
    parts = re.findall(r".{1,%d}(?:[- ]|$)|.{1,%d}" % (width, width), str(value))
    return "<br/>".join(html.escape(p.strip(), quote=True).replace('`', '&#96;') for p in parts)


def short_name(name):
    return re.sub(r"^IntegrationV2-(?:Distributed-)?", "", name)


def duration(milliseconds):
    seconds = max(0, int(milliseconds) // 1000)
    hours, seconds = divmod(seconds, 3600)
    minutes, seconds = divmod(seconds, 60)
    return (f"{hours}h " if hours else "") + (f"{minutes}m " if minutes or hours else "") + f"{seconds}s"


def failed(runner):
    return runner.get("failed", 0) > 0 or runner.get("configurationFailed", 0) > 0


def block_failed(block):
    return block.get("bootFailed", False) or any(failed(r) for r in block["runners"])


def card(node, text, status):
    return f'  {node}("{text}"):::{status}\n'


def fenced(body):
    graph = "flowchart LR\n" + body + STYLE
    if len(graph) > MAX_DIAGRAM_CHARS or graph.count(" --> ") > MAX_DIAGRAM_EDGES:
        raise ValueError("diagram exceeds the summary rendering budget")
    return "```mermaid\n" + graph + "```\n\n"


def main_graph(blocks, totals):
    lanes = {}
    for index, block in blocks:
        lanes.setdefault(block["thread"], []).append((index, block))
    body = card("start", "Suite start", "neutral")
    run = sum(totals[k] for k in ("passed", "failed", "skipped"))
    text = f'{run} tests run<br/>{totals["failed"]} failed · {totals["skipped"]} skipped'
    if totals.get("configurationFailed", 0):
        text += f'<br/>{totals["configurationFailed"]} configuration failures'
    body += card("totals", text, "fail" if any(block_failed(b) for _, b in blocks) else "neutral")
    for lane, (thread, entries) in enumerate(sorted(lanes.items())):
        body += f'  subgraph lane{lane}["{label(thread)}"]\n    direction LR\n'
        for index, block in entries:
            status = "fail" if block_failed(block) else "pass"
            icon = "❌" if status == "fail" else "✅"
            body += card(f"b{index}", f'{icon} {label(short_name(block["name"]))}<br/>{duration(block["durationMs"])}', status)
        body += "  end\n"
        body += f"  style lane{lane} fill:#f6f8fa,stroke:#d1d9e0,color:#1f2328,rx:8,ry:8\n"
        nodes = ["start"] + [f"b{i}" for i, _ in entries] + ["totals"]
        body += "".join(f"  {a} --> {b}\n" for a, b in zip(nodes, nodes[1:]))
    return fenced(body)


def detail_groups(blocks):
    for index, block in blocks:
        # Runner groups bound diagram height as well as character and edge counts.
        runners = list(enumerate(block["runners"]))
        for offset in range(0, max(1, len(runners)), 8):
            group = f"detail{index}_{offset}"
            heading = label(short_name(block["name"]), 36)
            if len(runners) > 8:
                heading += f"<br/>Runners {offset + 1}–{min(offset + 8, len(runners))} of {len(runners)}"
            body = f'  subgraph {group}["{heading}"]\n    direction LR\n'
            status = "fail" if block_failed(block) else "pass"
            title = "❌" if status == "fail" else "✅"
            if block.get("bootFailed", False):
                title += " Bootstrap failed"
            body += card(f"{group}b", title + " " + duration(block["durationMs"]), status)
            for number, runner in runners[offset:offset + 8]:
                status = "fail" if block.get("bootFailed", False) or failed(runner) else (
                    "pass" if runner["passed"] else "skip")
                icon = {"pass": "✅", "fail": "❌", "skip": "⏭"}[status]
                name = runner["className"].rsplit(".", 1)[-1]
                text = f'{icon} {label(name, 30)}<br/>{runner["passed"]} pass · {runner["failed"]} fail · {runner["skipped"]} skip'
                if runner.get("configurationFailed", 0):
                    text += f'<br/>{runner["configurationFailed"]} configuration failures'
                node = f"{group}r{number}"
                body += card(node, text, status)
                body += f"  {group}b --> {node}\n"
            body += f"  end\n  style {group} fill:#f6f8fa,stroke:#d1d9e0,color:#1f2328,rx:8,ry:8\n"
            yield body


def details(blocks):
    chunk = ""
    groups = 0
    for group in detail_groups(blocks):
        if chunk and (len(chunk + group + STYLE) > 35_000
                      or (chunk + group).count(" --> ") > 24 or groups >= 4):
            yield fenced(chunk)
            chunk = ""
            groups = 0
        chunk += group
        groups += 1
    if chunk:
        yield fenced(chunk)


def validate(report):
    if report["schemaVersion"] != 1:
        raise ValueError("unsupported block timeline schema")
    for key in ("passed", "failed", "skipped", "configurationFailed"):
        if not isinstance(report["totals"][key], int) or report["totals"][key] < 0:
            raise ValueError("invalid result totals")
    for block in report["blocks"]:
        for key in ("name", "thread"):
            if not isinstance(block[key], str) or len(block[key]) > 300:
                raise ValueError("invalid block name or worker thread")
        for key in ("startTimeMs", "endTimeMs", "durationMs"):
            if not isinstance(block[key], int) or block[key] < 0:
                raise ValueError("invalid block timing")
        for runner in block["runners"]:
            if not isinstance(runner["className"], str) or len(runner["className"]) > 500:
                raise ValueError("invalid runner name")
            for key in ("passed", "failed", "skipped", "configurationFailed"):
                if not isinstance(runner[key], int) or runner[key] < 0:
                    raise ValueError("invalid runner result")


def render(report):
    validate(report)
    blocks = sorted(enumerate(report["blocks"]), key=lambda item: (item[1]["startTimeMs"], item[0]))
    if not blocks:
        return "### v2 suite execution\n\nNo executed blocks were recorded.\n"
    parts = ["### v2 suite execution\n\n",
             "Lanes are TestNG suite workers; arrows show block order, not elapsed-time scale. "
             "Block durations are TestNG intervals, including bootstrap and test execution, before teardown callbacks. "
             "Runner counts are TestNG test invocations; "
             "configuration failures are separate. ⏭ indicates skipped or unexecuted runners.\n\n"]
    for offset in range(0, len(blocks), 40):
        parts.append(main_graph(blocks[offset:offset + 40], report["totals"]))
    main_parts = list(parts)
    red = [(i, b) for i, b in blocks if block_failed(b)]
    if red:
        parts.append("#### Failed blocks: runner results\n\n")
        parts.extend(details(red))
    parts.append("<details>\n<summary>All blocks: runner results</summary>\n\n")
    parts.extend(details(blocks))
    parts.append("</details>\n")
    result = "".join(parts)
    if len(result.encode("utf-8")) > MAX_SUMMARY_BYTES:
        # The lane diagram remains available even when the complete runner map is unusually large.
        result = "".join(main_parts) + "Runner detail exceeds the summary budget; see block-timeline.json.\n"
        if len(result.encode("utf-8")) > MAX_SUMMARY_BYTES:
            result = "v2 suite execution: timeline exceeds the summary budget; see block-timeline.json.\n"
    return result


def append_summary(source, destination=None):
    try:
        report = json.loads(source.read_text(encoding="utf-8"))
        markdown = render(report)
    except FileNotFoundError:
        markdown = "v2 suite execution: block-timeline.json is unavailable (the test JVM may not have finished).\n"
    except Exception as error:
        print(f"v2 suite execution: unable to render timeline: {error}", file=sys.stderr)
        markdown = "v2 suite execution: timeline could not be rendered; see the step log and test reports.\n"
    if destination:
        with destination.open("a", encoding="utf-8") as output:
            output.write(markdown)
    else:
        print(markdown, end="")


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, default=DEFAULT_INPUT)
    parser.add_argument("--output", type=Path, default=os.environ.get("GITHUB_STEP_SUMMARY"))
    args = parser.parse_args(argv)
    try:
        append_summary(args.input, args.output)
    except Exception as error:
        print(f"v2 suite execution: summary unavailable: {error}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
