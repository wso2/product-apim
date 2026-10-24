#!/usr/bin/env python3
"""Split an integration-v2 Maven/CI log into per-block logs.

usage: split_block_logs.py <raw log> <suite xml> <out dir>

For every <test> block in the suite (blockLabel parameter -> TestNG test name) it writes:
  <label>.carbon.log  the block's APIM container lines (everything up to and including "[<label>] : " removed)
  <label>.<container>.log  for a distributed block, one server log per container (apim-cp, apim-gw, apim-tm, mysql),
                      from the lines prefixed "[<label>/<container>] : " (prefix removed the same way)
  <label>.test.log    the test JVM lines from the block's TestNG threads ("TestNG-test=<test name>-<n>")
  <label>.is.log      the block's external Identity Server lines, when the block boots one (see below)
Files with no lines are not written. A summary line per block is printed.

External IS lines carry no block label. Each IS container logs through its own docker-java-stream thread, and the
block that starts it logs "Initializing DynamicISContainer (..., block='<label>')" first. A new IS stream thread is
attributed to the block whose initialisation is the oldest one not yet matched to a stream. With several IS blocks
booting at the same moment the attribution can be wrong: confirm it by content (tenant names, resource names).

ANSI colour sequences are removed with an exact ESC[...m pattern, so text such as "[mt-sso]" is never altered.
"""
import os
import re
import sys
from collections import deque

ANSI = re.compile(r'\x1b\[[0-9;]*m')
IS_INIT = re.compile(r"Initializing DynamicISContainer \(.*block='([^']+)'")
DISTRIBUTED = re.compile(r'\[([^\]/]+)/([A-Za-z0-9_-]+)\] : ')
TEST_THREAD = re.compile(r'TestNG-test=(.+)-\d+\]')
IS_LINE = re.compile(r'\[(docker-java-stream-[^\]]+)\] \w+\s+DynamicISContainer \[[^\]]*\] - ')


def blocks(suite_path):
    suite = open(suite_path, encoding='utf-8').read()
    for m in re.finditer(r'<test name="([^"]+)"(.*?)</test>', suite, re.S):
        label = re.search(r'name="blockLabel" value="([^"]+)"', m.group(2))
        if label:
            yield label.group(1), m.group(1)


def main(raw_log, suite_path, out_dir):
    os.makedirs(out_dir, exist_ok=True)
    pairs = list(blocks(suite_path))
    labels = {label for label, _ in pairs}
    label_of_test = {test: label for label, test in pairs}
    handles = {}
    counts = {label: {'carbon': 0, 'test': 0, 'is': 0} for label in labels}
    containers = {label: {} for label in labels}

    def write(label, kind, text):
        key = (label, kind)
        if key not in handles:
            handles[key] = open(os.path.join(out_dir, f'{label}.{kind}.log'), 'w', encoding='utf-8')
        handles[key].write(text)
        if kind in counts[label]:
            counts[label][kind] += 1
        else:
            containers[label][kind] = containers[label].get(kind, 0) + 1

    pending_is = deque()
    is_stream = {}
    with open(raw_log, encoding='utf-8', errors='replace') as log:
        for raw in log:
            line = ANSI.sub('', raw)
            init = IS_INIT.search(line)
            if init and init.group(1) in labels:
                pending_is.append(init.group(1))
            is_match = IS_LINE.search(line)
            if is_match:
                stream = is_match.group(1)
                if stream not in is_stream and pending_is:
                    is_stream[stream] = pending_is.popleft()
                if stream in is_stream:
                    write(is_stream[stream], 'is', line[is_match.end():])
                    continue
            distributed = DISTRIBUTED.search(line)
            if distributed and distributed.group(1) in labels:
                write(distributed.group(1), distributed.group(2), line[distributed.end():])
                continue
            for label, test in pairs:
                prefix = f'[{label}] : '
                at = line.find(prefix)
                if at >= 0:
                    write(label, 'carbon', line[at + len(prefix):])
                    break
            else:
                # The thread's full test name, matched exactly: a block name can be a prefix of another
                # (IntegrationV2-ServerRestart-EmailUser / IntegrationV2-ServerRestart-EmailUser-Throttling).
                thread = TEST_THREAD.search(line)
                if thread and thread.group(1) in label_of_test:
                    write(label_of_test[thread.group(1)], 'test', line)
    for handle in handles.values():
        handle.close()
    for label, _ in pairs:
        c = counts[label]
        if any(c.values()) or containers[label]:
            per_container = ''.join(f' {name}={n}' for name, n in sorted(containers[label].items()))
            print(f'{label}: carbon={c["carbon"]} test={c["test"]} is={c["is"]}{per_container}')


if __name__ == '__main__':
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    main(*sys.argv[1:4])
