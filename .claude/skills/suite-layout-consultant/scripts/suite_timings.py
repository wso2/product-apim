#!/usr/bin/env python3
"""Deterministic timing metrics of an integration-v2 run and the 4-lane layout model.

usage: suite_timings.py <testng-results.xml | CI job log> <suite xml> [--features <dir>] [--lanes 4] [--budget 3600]
                        [--boot <seconds>] [--top 15] [--what-if <file>]

Input: TestNG's testng-results.xml (target/surefire-reports/, archived by CI; fetch_ci_timings.sh downloads it with the
suite file and the features at the run's commit) and the suite file that ran. --features points at the features
directory of the same commit (to map feature titles to files). A CI job log instead of testng-results.xml gives the
block-level sections only (the log has no runner/feature/scenario timings).

Output (Markdown, always the same sections in the same order, so reports are comparable):
  1. Run summary          — blocks, tests, block time, boot overhead, measured wall span, ideal suite time
  2. Blocks               — highest total first: boot, run, lanes, imbalance, scenarios, share of suite, flags
  3. Top runners          — longest runner spans: block, share of block, scenarios, mean per scenario
  4. Top features         — longest features (summed scenario time per block): scenarios, mean, runner, block
  5. Slowest scenarios    — single scenario rows
  6. Multi-block runners  — runners registered in more than one block, and what each registration costs
  7. Layout model         — file-order dispatch (what TestNG runs; the modelled suite time) and longest-first
                            packing (the best order for the same blocks); the gap is the saving a reorder buys
  8. What-if comparison   — with --what-if: per-block and suite before/after and the change

Model: block length = boot + longest lane, runners dispatched in suite order onto thread-count lanes (each runner to
the lane that frees first). TestNG starts blocks in suite-file order, each on the first of --lanes threads to free up,
so file-order dispatch is the modelled suite time; longest-first packing of the same blocks is the target a reorder
can reach. Spans measured under concurrency include contention; measure moved runners in isolation and feed them to
--what-if.

--what-if <file>, one change per line (applied in order):
  <block> <runner> <seconds>       add a runner to a block with this duration (or override it)
  <block> remove <runner>          remove a runner from a block
  <block> thread-count <n>         change a block's lane count
  <block> boot <seconds>           change a block's boot estimate
  <block> drop                     remove a block (e.g. after moving all of its runners)
  <block> position <n>             move a block to position n (1-based) in the suite file order
A block name that does not exist yet creates a new block (it pays the median boot unless given one).
"""
import argparse
import os
import re
import statistics
import xml.etree.ElementTree as ET
from datetime import datetime


def ts(value):
    return datetime.strptime(value.rsplit(' ', 1)[0], '%Y-%m-%dT%H:%M:%S')


def parse_suite(path):
    suite = open(path, encoding='utf-8').read()
    layout = []
    for m in re.finditer(r'<test name="([^"]+)"([^>]*)>(.*?)</test>', suite, re.S):
        tc = re.search(r'thread-count="(\d+)"', m.group(2))
        runners = [c.rsplit('.', 1)[-1] for c in re.findall(r'<class name="([^"]+)"', m.group(3))]
        label = re.search(r'name="blockLabel" value="([^"]+)"', m.group(3))
        layout.append({'name': m.group(1), 'tc': int(tc.group(1)) if tc else 1, 'runners': runners,
                       'label': label.group(1) if label else None})
    return layout


def feature_files(features_dir):
    """feature title -> path relative to the features dir."""
    titles = {}
    if not features_dir:
        return titles
    for root, _, files in os.walk(features_dir):
        for f in files:
            if f.endswith('.feature'):
                path = os.path.join(root, f)
                m = re.search(r'^\s*Feature:\s*(.+?)\s*$', open(path, encoding='utf-8', errors='replace').read(), re.M)
                if m:
                    titles[m.group(1)] = os.path.relpath(path, features_dir)
    return titles


def parse_results(path):
    root = ET.parse(path).getroot()
    blocks, runners, scenarios, tests = {}, {}, [], 0
    for test in root.iter('test'):
        block = test.get('name')
        if test.get('started-at') and test.get('finished-at'):
            blocks[block] = (ts(test.get('started-at')), ts(test.get('finished-at')))
        for cls in test.iter('class'):
            runner = cls.get('name').rsplit('.', 1)[-1]
            for method in cls.iter('test-method'):
                if not method.get('started-at'):
                    continue
                start, finish = ts(method.get('started-at')), ts(method.get('finished-at'))
                ms = int(method.get('duration-ms', '0'))
                key = (block, runner)
                first, last, total, count = runners.get(key, (start, finish, 0, 0))
                is_scenario = method.get('is-config') != 'true'
                runners[key] = (min(first, start), max(last, finish), total + ms, count + (1 if is_scenario else 0))
                if is_scenario:
                    tests += 1
                    params = [((p.find('value').text or '').strip().strip('"') if p.find('value') is not None else '')
                              for p in method.iter('param')]
                    name = params[0] if params else method.get('name')
                    feature = re.sub(r'^Optional\[(.*)\]$', r'\1', params[1]) if len(params) > 1 else '?'
                    scenarios.append({'block': block, 'runner': runner, 'scenario': name, 'feature': feature,
                                      'seconds': ms / 1000.0})
    return blocks, runners, scenarios, tests


def parse_log(path, layout):
    by_label = {b['label']: b['name'] for b in layout if b['label']}
    line_re = re.compile(r"(\d\d:\d\d:\d\d)\.\d+ \[[^\]]+\] INFO\s+BlockLifecycleListener \[\] - Block '([^']+)' (.*)")
    times = {}
    with open(path, encoding='utf-8', errors='replace') as log:
        for raw in log:
            m = line_re.search(re.sub(r'\x1b\[[0-9;]*m', '', raw))
            if not m or m.group(2) not in by_label:
                continue
            t = datetime.strptime(m.group(1), '%H:%M:%S')
            entry = times.setdefault(by_label[m.group(2)], [t, None, None])
            if m.group(3).startswith('booted and ready'):
                entry[1] = t
            elif m.group(3).startswith('private network closed'):
                entry[2] = t
    return {k: v for k, v in times.items() if v[1] and v[2]}


def lanes_of(durations, lanes):
    free = [0.0] * max(1, lanes)
    for d in durations:
        free[free.index(min(free))] += d
    return free


def pack(rows, lanes):
    queues = [[0.0, []] for _ in range(lanes)]
    for name, length in sorted(rows, key=lambda r: (-r[1], r[0])):
        q = min(queues, key=lambda x: x[0])
        q[0] += length
        q[1].append(name)
    return sorted(queues, key=lambda q: -q[0])


def dispatch(rows, lanes):
    """Blocks in suite-file order, each started on the lane that frees first (TestNG's thread pool)."""
    queues = [[0.0, []] for _ in range(lanes)]
    for name, length in rows:
        q = min(queues, key=lambda x: x[0])
        q[0] += length
        q[1].append(name)
    return sorted(queues, key=lambda q: -q[0])


def block_lengths(layout, durations, boots, default_boot):
    rows = {}
    for b in layout:
        lanes = lanes_of([durations.get((b['name'], r), 0.0) for r in b['runners']], b['tc'])
        rows[b['name']] = (boots.get(b['name'], default_boot), max(lanes), lanes)
    return rows


def fmt(seconds):
    return f'{seconds:,.0f} s ({seconds / 60:.0f} min)' if abs(seconds) >= 60 else f'{seconds:.0f} s'


def model_table(title, layout, rows, lanes, budget):
    ordered = [(b['name'], sum(rows[b['name']][:2])) for b in layout]
    total = sum(length for _, length in ordered)
    results = {}
    for kind, queues in (('file order', dispatch(ordered, lanes)), ('longest first', pack(ordered, lanes))):
        makespan = queues[0][0] if queues else 0.0
        results[kind] = makespan
        print(f'\n### {title} — {kind}' + (' (what TestNG runs)' if kind == 'file order' else ' (best order)'))
        print('| Queue | Blocks (in start order) | Filled | Of budget |')
        print('|---|---|---:|---:|')
        for i, (filled, names) in enumerate(queues, 1):
            print(f'| {i} | {", ".join(names)} | {fmt(filled)} | {100 * filled / budget:.0f}% |')
        eff = 100 * total / (lanes * makespan) if makespan else 0
        print(f'\n{kind.capitalize()}: suite time **{fmt(makespan)}**; packing efficiency {eff:.0f}%.')
    gap = results['file order'] - results['longest first']
    print(f'\nModelled suite time (file order) **{fmt(results["file order"])}**; block time {fmt(total)}; '
          f'reordering the blocks longest-first would save {fmt(gap) if gap >= 1 else "0 s"}.')
    return results['file order'], results['longest first'], total


def apply_what_if(path, layout, durations, boots):
    layout = [dict(b, runners=list(b['runners'])) for b in layout]
    durations, boots = dict(durations), dict(boots)
    for line in open(path, encoding='utf-8'):
        parts = line.split()
        if not parts or line.lstrip().startswith('#'):
            continue
        name = parts[0]
        if len(parts) == 2 and parts[1] == 'drop':
            layout = [b for b in layout if b['name'] != name]
            continue
        block = next((b for b in layout if b['name'] == name), None)
        if block is None:
            block = {'name': name, 'tc': 1, 'runners': [], 'label': None}
            layout.append(block)
        if parts[1] == 'position':
            layout.remove(block)
            layout.insert(max(0, int(parts[2]) - 1), block)
        elif parts[1] == 'thread-count':
            block['tc'] = int(parts[2])
        elif parts[1] == 'boot':
            boots[name] = float(parts[2])
        elif parts[1] == 'remove':
            block['runners'] = [r for r in block['runners'] if r != parts[2]]
        else:
            durations[(name, parts[1])] = float(parts[2])
            if parts[1] not in block['runners']:
                block['runners'].append(parts[1])
    return layout, durations, boots


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('results')
    ap.add_argument('suite')
    ap.add_argument('--features')
    ap.add_argument('--lanes', type=int, default=4)
    ap.add_argument('--budget', type=float, default=3600)
    ap.add_argument('--boot', type=float)
    ap.add_argument('--top', type=int, default=15)
    ap.add_argument('--what-if')
    args = ap.parse_args()

    layout = parse_suite(args.suite)
    head = open(args.results, encoding='utf-8', errors='replace').read(200).lstrip()
    log_only = not head.startswith('<')
    if log_only:
        times = parse_log(args.results, layout)
        blocks = {n: (s, e) for n, (s, _, e) in times.items()}
        boots = {n: (r - s).total_seconds() for n, (s, r, _) in times.items()}
        durations = {(n, '__block__'): (e - r).total_seconds() for n, (_, r, e) in times.items()}
        layout = [dict(b, runners=['__block__'], tc=1) for b in layout if b['name'] in times]
        runners, scenarios, tests = {}, [], 0
    else:
        blocks, runners, scenarios, tests = parse_results(args.results)
        durations = {k: (v[1] - v[0]).total_seconds() for k, v in runners.items()}
        boots = {}
        for b in layout:
            if b['name'] in blocks:
                span = (blocks[b['name']][1] - blocks[b['name']][0]).total_seconds()
                lane = max(lanes_of([durations.get((b['name'], r), 0.0) for r in b['runners']], b['tc']))
                boots[b['name']] = max(0.0, span - lane)
    measured_boots = sorted(boots.values())
    default_boot = args.boot if args.boot is not None else (
        measured_boots[len(measured_boots) // 2] if measured_boots else 150.0)
    rows = block_lengths(layout, durations, boots, default_boot)
    titles = feature_files(args.features)

    # 1. Run summary
    block_time = sum(boot + lane for boot, lane, _ in rows.values())
    boot_time = sum(boot for boot, _, _ in rows.values())
    starts = [s for s, _ in blocks.values()]
    ends = [e for _, e in blocks.values()]
    wall = (max(ends) - min(starts)).total_seconds() if starts else 0.0
    print('## 1. Run summary')
    print('| Metric | Value |')
    print('|---|---|')
    print(f'| Source | {os.path.basename(args.results)}{" (job log: block level only)" if log_only else ""} |')
    regs = sum(len(b['runners']) for b in layout) if not log_only else 'n/a'
    print(f'| Blocks / runner registrations / scenario rows | {len(rows)} / {regs} / {tests if not log_only else "n/a"} |')
    print(f'| Measured wall span (first block start → last block end) | {fmt(wall)} |')
    print(f'| Block time (sum of boot + longest lane) | {fmt(block_time)} |')
    if block_time:
        print(f'| Boot overhead | {fmt(boot_time)} ({100 * boot_time / block_time:.0f}% of block time; '
              f'median boot {default_boot:.0f} s) |')
    print(f'| Ideal on {args.lanes} lanes (block time / lanes) | {fmt(block_time / args.lanes)} |')

    # 2. Blocks
    print('\n## 2. Blocks (highest total first)')
    print('| Block | Total | Boot | Run (longest lane) | thread-count | Runners | Scenarios | Lane imbalance | Share | Flags |')
    print('|---|---:|---:|---:|---:|---:|---:|---:|---:|---|')
    scen_count = {}
    for s in scenarios:
        scen_count[s['block']] = scen_count.get(s['block'], 0) + 1
    for b in sorted(layout, key=lambda x: (-(rows[x['name']][0] + rows[x['name']][1]), x['name'])):
        boot, lane, lanes = rows[b['name']]
        total = boot + lane
        imbalance = (lane - statistics.mean(lanes)) if len(lanes) > 1 else 0.0
        flags = []
        if total > args.budget:
            flags.append('over budget')
        if boot > lane:
            flags.append('boot-dominated')
        if b['tc'] > 1 and lane and imbalance > 0.25 * lane:
            flags.append('lanes imbalanced')
        if not log_only and b['runners']:
            last = durations.get((b['name'], b['runners'][-1]), 0.0)
            if b['tc'] == 1 and len(b['runners']) > 1 and lane and last > 0.4 * lane:
                flags.append(f'tail runner {b["runners"][-1]}')
        print(f'| {b["name"]} | {fmt(total)} | {boot:.0f} s | {lane:.0f} s | {b["tc"]} | '
              f'{len(b["runners"]) if not log_only else "?"} | {scen_count.get(b["name"], "n/a")} | '
              f'{imbalance:.0f} s | {100 * total / block_time:.1f}% | {", ".join(flags)} |')

    if not log_only:
        # 3. Top runners
        print(f'\n## 3. Top {args.top} runners (longest span)')
        print('| Runner | Block | Span | Share of block run | Scenarios | Mean per scenario |')
        print('|---|---|---:|---:|---:|---:|')
        for (block, runner), (first, last, _, count) in sorted(
                runners.items(), key=lambda kv: (-(kv[1][1] - kv[1][0]).total_seconds(), kv[0]))[:args.top]:
            span = (last - first).total_seconds()
            lane = rows.get(block, (0, 0, []))[1] or 1
            mean = f'{span / count:.0f} s' if count else ''
            print(f'| {runner} | {block} | {fmt(span)} | {100 * span / lane:.0f}% | {count} | {mean} |')

        # 4. Top features
        feats = {}
        for s in scenarios:
            key = (s['block'], s['feature'])
            secs, count, rs = feats.get(key, (0.0, 0, set()))
            rs.add(s['runner'])
            feats[key] = (secs + s['seconds'], count + 1, rs)
        print(f'\n## 4. Top {args.top} features (summed scenario time, per block)')
        print('| Feature | File | Block | Time | Scenarios | Mean per scenario | Runner |')
        print('|---|---|---|---:|---:|---:|---|')
        for (block, feature), (secs, count, rs) in sorted(feats.items(), key=lambda kv: (-kv[1][0], kv[0]))[:args.top]:
            print(f'| {feature} | {titles.get(feature, "")} | {block} | {fmt(secs)} | {count} | {secs / count:.0f} s | '
                  f'{", ".join(sorted(rs))} |')

        # 5. Slowest scenarios
        print(f'\n## 5. Top {args.top} slowest scenario rows')
        print('| Scenario | Feature | Block | Duration |')
        print('|---|---|---|---:|')
        for s in sorted(scenarios, key=lambda x: (-x['seconds'], x['scenario']))[:args.top]:
            print(f'| {s["scenario"][:110]} | {s["feature"]} | {s["block"]} | {fmt(s["seconds"])} |')

        # 6. Multi-block runners
        owners = {}
        for b in layout:
            for r in b['runners']:
                owners.setdefault(r, []).append(b['name'])
        multi = {r: bs for r, bs in owners.items() if len(bs) > 1}
        print('\n## 6. Runners registered in more than one block')
        if not multi:
            print('None.')
        else:
            print('| Runner | Blocks (span in each) | Total cost |')
            print('|---|---|---:|')
            for r, bs in sorted(multi.items(), key=lambda kv: (-sum(durations.get((b, kv[0]), 0) for b in kv[1]), kv[0])):
                spans = [f'{b} ({durations.get((b, r), 0):.0f} s)' for b in bs]
                print(f'| {r} | {"; ".join(spans)} | {fmt(sum(durations.get((b, r), 0) for b in bs))} |')

    # 7. Layout model
    print('\n## 7. Layout model')
    before_makespan, before_best, before_total = model_table('As measured', layout, rows, args.lanes, args.budget)
    if wall:
        print(f'\nMeasured wall span {fmt(wall)}; file-order model error {wall - before_makespan:+,.0f} s '
              f'({100 * (wall - before_makespan) / wall:+.1f}% of measured).')

    # 8. What-if comparison
    if args.what_if:
        print('\n## 8. What-if comparison')
        if log_only:
            print('Not available from a job log (no runner timings).')
            return
        layout2, durations2, boots2 = apply_what_if(args.what_if, layout, durations, boots)
        rows2 = block_lengths(layout2, durations2, boots2, default_boot)
        after_makespan, after_best, after_total = model_table('What-if layout', layout2, rows2, args.lanes,
                                                              args.budget)
        print('\n| Block | Before | After | Change |')
        print('|---|---:|---:|---:|')
        for name in sorted(set(rows) | set(rows2)):
            before = sum(rows[name][:2]) if name in rows else 0.0
            after = sum(rows2[name][:2]) if name in rows2 else 0.0
            if round(before) != round(after):
                print(f'| {name} | {fmt(before) if before else "—"} | {fmt(after) if after else "—"} | '
                      f'{after - before:+,.0f} s |')
        print('\n| Suite | Before | After | Change |')
        print('|---|---:|---:|---:|')
        print(f'| Modelled suite time (file order) | {fmt(before_makespan)} | {fmt(after_makespan)} | '
              f'{after_makespan - before_makespan:+,.0f} s '
              f'({100 * (after_makespan - before_makespan) / before_makespan:+.0f}%) |')
        print(f'| Best order (longest first) | {fmt(before_best)} | {fmt(after_best)} | '
              f'{after_best - before_best:+,.0f} s |')
        print(f'| Block time | {fmt(before_total)} | {fmt(after_total)} | {after_total - before_total:+,.0f} s |')
        print(f'| Blocks (boots) | {len(rows)} | {len(rows2)} | {len(rows2) - len(rows):+d} |')


if __name__ == '__main__':
    main()
