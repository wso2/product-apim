#!/usr/bin/env python3
# Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com). All Rights Reserved.
# Licensed under the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0).
"""Run a command unchanged while relaying independently collected host diagnostics."""

import argparse
import json
import os
from pathlib import Path
import signal
import subprocess
import sys
import time
import uuid


def emit(message):
    print(f"[host-diagnostics] {message}", flush=True)


def emit_summary(record):
    # A full suite accumulates historical container peaks. Keep each CI log line small while retaining
    # every record at the same 60-second cadence; local summary files retain the complete JSON object.
    peaks = record.get("memory_peaks", [])
    summary = {key: value for key, value in record.items() if key != "memory_peaks"}
    summary["memory_peak_records"] = len(peaks)
    emit(json.dumps(summary, separators=(",", ":")))
    for offset in range(0, len(peaks), 8):
        emit(json.dumps({"utc": record.get("utc"), "memory_peaks_offset": offset,
                         "memory_peak_records": len(peaks), "memory_peaks": peaks[offset:offset + 8]},
                        separators=(",", ":")))


def service(*arguments):
    return subprocess.run(["sudo", "-n", *arguments], stdout=subprocess.PIPE,
                          stderr=subprocess.STDOUT, text=True, timeout=20)


def stop(output):
    unit_file = output / "service-unit.txt"
    if unit_file.exists():
        unit = unit_file.read_text().strip()
        if not unit.startswith("apim-host-diagnostics-") or not unit.endswith(".service") or "/" in unit:
            raise ValueError("Unexpected diagnostics service name")
        state = service("systemctl", "show", unit, "--property=LoadState", "--value")
        if state.stdout.strip() == "not-found":
            return
        result = service("systemctl", "stop", unit)
        if result.returncode:
            emit(f"Collector stop returned {result.returncode}: {result.stdout.strip()}")


def start(output):
    output.mkdir(parents=True, exist_ok=True)
    if (output / "metadata.json").exists() or (output / "service-unit.txt").exists():
        raise ValueError("Use a fresh diagnostics output directory for each run")
    unit = f"apim-host-diagnostics-{uuid.uuid4().hex}.service"
    (output / "service-unit.txt").write_text(unit + "\n")
    collector = Path(__file__).resolve().with_name("collect-host-diagnostics.py")
    try:
        result = service("systemd-run", "--quiet", "--collect", "--service-type=exec", f"--unit={unit}",
                         "--property=MemoryMax=192M", "--property=CPUQuota=10%", "--property=TasksMax=32",
                         "--property=TimeoutStopSec=10", "--property=RuntimeMaxSec=7h",
                         "/usr/bin/python3", "-u", str(collector), "--output", str(output),
                         "--sample-interval", "5", "--summary-interval", "60")
        (output / "service-start.log").write_text(result.stdout)
        if result.returncode:
            emit(f"WARNING: collector could not start: {result.stdout.strip()}")
        else:
            emit(f"Collector started as {unit}; samples=5s, CI summaries=60s; output={output}")
    except (OSError, subprocess.TimeoutExpired) as error:
        emit(f"WARNING: collector could not start: {error}")
        (output / "service-start.log").write_text(str(error) + "\n")
    return unit


def run(output, command):
    unit = start(output)

    process, last_summary, last_check = None, None, 0
    received_signal = None

    def relay_signal(signum, _frame):
        nonlocal received_signal
        received_signal = signum
        if process is not None and process.poll() is None:
            try:
                os.killpg(process.pid, signum)
            except ProcessLookupError:
                pass

    def relay_summary():
        nonlocal last_summary
        try:
            record = json.loads((output / "summary.json").read_text())
            if record.get("utc") != last_summary:
                # The collector emits only metrics and selected names, never commands or environment values.
                emit_summary(record)
                last_summary = record.get("utc")
        except (OSError, ValueError):
            pass

    previous_handlers = {number: signal.signal(number, relay_signal) for number in (signal.SIGINT, signal.SIGTERM)}
    try:
        process = subprocess.Popen(command, start_new_session=True)
        while process.poll() is None:
            relay_summary()
            if time.monotonic() - last_check >= 60:
                last_check = time.monotonic()
                try:
                    health = service("systemctl", "is-active", unit)
                    if health.returncode:
                        emit(f"WARNING: collector is {health.stdout.strip()}; Maven continues unchanged")
                    summary_path = output / "summary.json"
                    if summary_path.exists() and time.time() - summary_path.stat().st_mtime > 120:
                        emit("WARNING: collector summary is over 120s old")
                except (OSError, subprocess.TimeoutExpired) as error:
                    emit(f"WARNING: could not check collector: {error}")
            time.sleep(1)
        returncode = process.wait()
    finally:
        try:
            stop(output)
            relay_summary()
        except (OSError, ValueError, subprocess.TimeoutExpired) as error:
            emit(f"WARNING: collector cleanup failed: {error}")
        for number, handler in previous_handlers.items():
            signal.signal(number, handler)
    if received_signal is not None:
        return 128 + received_signal
    return returncode if returncode >= 0 else 128 - returncode


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--stop", action="store_true", help="Idempotent workflow cleanup after cancellation")
    parser.add_argument("--start-only", action="store_true", help="Start on a Linux Docker VM when Maven runs on its host")
    parser.add_argument("command", nargs=argparse.REMAINDER)
    args = parser.parse_args()
    output = args.output.resolve()
    if args.stop:
        try:
            stop(output)
        except (OSError, ValueError, subprocess.TimeoutExpired) as error:
            emit(f"WARNING: collector cleanup failed: {error}")
    elif args.start_only:
        start(output)
    else:
        command = args.command[1:] if args.command[:1] == ["--"] else args.command
        if not command:
            parser.error("Provide a command after --")
        sys.exit(run(output, command))
