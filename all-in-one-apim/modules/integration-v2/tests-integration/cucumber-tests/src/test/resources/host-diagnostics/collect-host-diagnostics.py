#!/usr/bin/env python3
# Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com). All Rights Reserved.
# Licensed under the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0).
"""Read-only Linux host/cgroup sampler. No third party Python packages required."""

import argparse
from collections import deque
import datetime
import http.client
import json
import os
from pathlib import Path
import signal
import socket
import subprocess
import threading
import time


STOP = threading.Event()
DISTRIBUTED_CGROUP = Path("/sys/fs/cgroup/apimdistributed.slice")
BROWSER_CGROUP = Path("/sys/fs/cgroup/pw-browser")


def stamp():
    return {"utc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
            "monotonic_seconds": time.monotonic()}


def read(path):
    try:
        return Path(path).read_text().strip()
    except (OSError, ValueError):
        return None


def counters(path):
    result = {}
    for line in (read(path) or "").splitlines():
        parts = line.split()
        if len(parts) == 2:
            try:
                result[parts[0].rstrip(":")] = int(parts[1])
            except ValueError:
                pass
    return result


def pressure(root):
    result = {}
    for resource in ("cpu", "memory", "io"):
        content = read(root / resource)
        if content is not None:
            result[resource] = {}
            for line in content.splitlines():
                fields = line.split()
                result[resource][fields[0]] = {
                    k: float(v) if k != "total" else int(v)
                    for k, v in (part.split("=", 1) for part in fields[1:])}
    return result


def filesystems(paths):
    result = {}
    for path in paths:
        try:
            stat = os.statvfs(path)
            result[str(path)] = {"device": os.stat(path).st_dev,
                                 "total_bytes": stat.f_blocks * stat.f_frsize,
                                 "available_bytes": stat.f_bavail * stat.f_frsize,
                                 "free_inodes": stat.f_favail}
        except OSError as error:
            result[str(path)] = {"error": str(error)[:200]}
    return result


class JsonLog:
    """Line-flushed files, bounded on disk; serialization also protects event threads."""

    def __init__(self, path, limit=32 * 1024 * 1024, backups=7):
        self.path, self.limit, self.backups = path, limit, backups
        self.lock = threading.Lock()

    def write(self, value):
        line = json.dumps(value, separators=(",", ":")) + "\n"
        with self.lock:
            if self.path.exists() and self.path.stat().st_size + len(line.encode()) > self.limit:
                for index in range(self.backups, 0, -1):
                    source = self.path if index == 1 else Path(str(self.path) + f".{index - 1}")
                    if source.exists():
                        source.replace(Path(str(self.path) + f".{index}"))
            with self.path.open("a", buffering=1) as stream:
                stream.write(line)


def atomic_json(path, value):
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(value, indent=2) + "\n")
    temporary.replace(path)


class DockerConnection(http.client.HTTPConnection):
    def __init__(self):
        super().__init__("localhost", timeout=2)

    def connect(self):
        self.sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        self.sock.settimeout(self.timeout)
        self.sock.connect("/var/run/docker.sock")


def docker_get(path):
    connection = DockerConnection()
    try:
        connection.request("GET", path)
        response = connection.getresponse()
        if response.status != 200:
            raise OSError(f"Docker GET {path}: HTTP {response.status}")
        return json.load(response)
    finally:
        connection.close()


def cgroup_for_pid(pid):
    for line in (read(f"/proc/{pid}/cgroup") or "").splitlines():
        if line.startswith("0::"):
            path = Path("/sys/fs/cgroup") / line[3:].lstrip("/")
            if path.is_dir():
                return path
    return None


def cgroup_sample(path):
    if path is None or not path.is_dir():
        return None
    result = {"path": str(path)}
    for name in ("memory.current", "memory.peak", "memory.high", "memory.max",
                 "memory.swap.current", "memory.swap.peak", "memory.swap.max",
                 "cpu.max", "pids.current", "pids.max"):
        value = read(path / name)
        if value is not None:
            result[name] = int(value) if value.isdigit() else value
    for name in ("cpu.stat", "memory.events", "memory.events.local", "memory.swap.events", "memory.stat", "pids.events"):
        result[name] = counters(path / name)
    result["io.stat"] = read(path / "io.stat")
    result["pressure"] = {}
    for resource in ("cpu", "memory", "io"):
        result["pressure"][resource] = read(path / f"{resource}.pressure")
    return result


def compact_cgroup(sample):
    if sample is None:
        return None
    return {**{key: sample.get(key) for key in ("memory.current", "memory.peak", "memory.high", "memory.max",
                                               "memory.swap.current", "memory.swap.peak", "memory.swap.max", "cpu.max",
                                               "cpu.stat", "memory.events", "memory.swap.events", "pids.current", "pids.max")},
            **memory_usage(sample)}


def memory_usage(sample):
    """RAM and swap are separate cgroup v2 charges; combine only known byte counts."""
    sample = sample or {}
    ram, swap = sample.get("memory.current"), sample.get("memory.swap.current")
    ram_limit, swap_limit = sample.get("memory.max"), sample.get("memory.swap.max")
    combined = ram + swap if isinstance(ram, int) and isinstance(swap, int) else None
    combined_limit = None
    if ram_limit is not None and swap_limit is not None:
        combined_limit = "max" if "max" in (ram_limit, swap_limit) else ram_limit + swap_limit
    return {"memory_bytes": ram, "memory_limit_bytes": ram_limit,
            "swap_bytes": swap, "swap_limit_bytes": swap_limit,
            "memory_and_swap_bytes": combined, "memory_and_swap_limit_bytes": combined_limit}


def swap_activity(host, previous=None, elapsed=None):
    memory = host.get("memory_bytes", {})
    total, free = memory.get("SwapTotal"), memory.get("SwapFree")
    result = {"total_bytes": total, "free_bytes": free,
              "used_bytes": total - free if total is not None and free is not None else None,
              "in_bytes_per_second": None, "out_bytes_per_second": None}
    if previous is not None and elapsed is not None and elapsed > 0:
        for direction, counter in (("in", "pswpin"), ("out", "pswpout")):
            current, old = host.get("vmstat", {}).get(counter), previous.get("vmstat", {}).get(counter)
            if current is not None and old is not None and current >= old:
                result[f"{direction}_bytes_per_second"] = (current - old) * os.sysconf("SC_PAGE_SIZE") / elapsed
    return result


def resource_group_samples():
    return {"workload": cgroup_sample(DISTRIBUTED_CGROUP),
            "browser": cgroup_sample(DISTRIBUTED_CGROUP / "apimdistributed-browser.service") or cgroup_sample(BROWSER_CGROUP)}


def container_details(identifier):
    data = docker_get(f"/containers/{identifier}/json")
    config, host, state = data.get("Config", {}), data.get("HostConfig", {}), data.get("State", {})
    # Deliberately exclude Env, command arguments, mounts and arbitrary labels.
    return {"id": identifier, "name": data.get("Name", "").lstrip("/"), "image": config.get("Image"),
            "state": {k: state.get(k) for k in ("Status", "Pid", "ExitCode", "OOMKilled", "StartedAt", "FinishedAt")},
            "limits": {k: host.get(k) for k in ("Memory", "MemorySwap", "NanoCpus", "CpuQuota", "CpuPeriod", "PidsLimit")},
            "networks": {k: {"ip": v.get("IPAddress"), "aliases": v.get("Aliases")}
                         for k, v in data.get("NetworkSettings", {}).get("Networks", {}).items()}}


def host_sample():
    memory = {}
    for line in (read("/proc/meminfo") or "").splitlines():
        key, value = line.split(":", 1)
        if key in ("MemTotal", "MemFree", "MemAvailable", "Buffers", "Cached", "SwapTotal", "SwapFree", "Dirty", "Writeback"):
            memory[key] = int(value.split()[0]) * 1024
    cpu = [int(value) for value in (read("/proc/stat") or "cpu 0 0 0 0 0 0 0 0").splitlines()[0].split()[1:9]]
    vm = counters("/proc/vmstat")
    vm = {k: v for k, v in vm.items() if k.startswith(("pswp", "pgmajfault", "pgscan", "pgsteal", "allocstall", "oom_kill"))}
    disks = {}
    for line in (read("/proc/diskstats") or "").splitlines():
        fields = line.split()
        if len(fields) >= 14 and (Path("/sys/block") / fields[2]).exists() and not fields[2].startswith(("loop", "ram")):
            disks[fields[2]] = {"reads": int(fields[3]), "read_sectors": int(fields[5]),
                                "writes": int(fields[7]), "write_sectors": int(fields[9]),
                                "io_in_progress": int(fields[11]), "io_ms": int(fields[12]),
                                "weighted_io_ms": int(fields[13])}
    return {"memory_bytes": memory, "cpu_ticks": cpu, "vmstat": vm,
            "loadavg": read("/proc/loadavg"), "pressure": pressure(Path("/proc/pressure")), "disks": disks}


def processes():
    result = []
    page_size = os.sysconf("SC_PAGE_SIZE")
    for path in Path("/proc").iterdir():
        if not path.name.isdigit():
            continue
        stat = read(path / "stat")
        if not stat:
            continue
        try:
            end = stat.rindex(")")
            fields = stat[end + 2:].split()
            result.append({"pid": int(path.name), "name": stat[stat.index("(") + 1:end],
                           "rss_bytes": int(fields[21]) * page_size,
                           "cpu_ticks": int(fields[11]) + int(fields[12]), "start_ticks": int(fields[19])})
        except (ValueError, IndexError):
            continue  # A process may exit while /proc is being read.
    result.sort(key=lambda process: process["rss_bytes"], reverse=True)
    selected = result[:20]
    for process in result[20:]:
        if process["name"].lower().startswith(("java", "chrome", "chromium", "dockerd", "containerd", "runner.")):
            selected.append(process)
    for process in selected:
        process["cgroup"] = read(f"/proc/{process['pid']}/cgroup")
    return selected


class Collector:
    def __init__(self, output, interval, summary_interval):
        self.output, self.interval, self.summary_interval = output, interval, summary_interval
        self.samples = JsonLog(output / "samples.jsonl")
        self.events = JsonLog(output / "events.jsonl", 8 * 1024 * 1024, 3)
        self.kernel = JsonLog(output / "kernel.jsonl", 8 * 1024 * 1024, 1)
        self.summaries = JsonLog(output / "summaries.jsonl", 4 * 1024 * 1024, 1)
        self.children, self.threads, self.containers = [], [], {}
        self.docker_root = Path("/var/lib/docker")
        self.started = time.monotonic()
        self.memory_peaks = {}
        self.aggregate = {"samples": 0, "max_sample_gap_seconds": 0, "min_available_bytes": None,
                          "max_swap_used_bytes": None, "max_running_containers": 0, "sampling_errors": 0,
                          "docker_oom_events": 0, "kernel_oom_messages": 0, "collector_errors": 0,
                          "max_memory_used_bytes": None,
                          "max_cpu_busy_percent": None, "max_cpu_iowait_percent": None,
                          "max_swap_in_bytes_per_second": None, "max_swap_out_bytes_per_second": None}

    def observe_usage(self, sample):
        """Retain extrema from every sample, including containers absent at summary time."""
        host = sample.get("host", {})
        memory = host.get("memory_bytes", {})
        total, available = memory.get("MemTotal"), memory.get("MemAvailable")
        used = total - available if total is not None and available is not None else None
        for key, value in (("max_cpu_busy_percent", host.get("cpu_percent", {}).get("busy")),
                           ("max_memory_used_bytes", used),
                           ("max_cpu_iowait_percent", host.get("cpu_percent", {}).get("iowait")),
                           ("max_swap_in_bytes_per_second", host.get("swap", {}).get("in_bytes_per_second")),
                           ("max_swap_out_bytes_per_second", host.get("swap", {}).get("out_bytes_per_second"))):
            if value is not None:
                old = self.aggregate[key]
                if old is None or value > old:
                    self.aggregate[key] = value
                    self.aggregate[f"{key}_utc"] = sample.get("utc")
        groups = [("container", container.get("id"), container.get("name"), container.get("image"),
                   container.get("cgroup")) for container in sample.get("containers", [])]
        groups.extend((kind, kind, kind, None, sample.get(kind)) for kind in ("workload", "browser"))
        for kind, identifier, name, image, cg in groups:
            if identifier is None or not cg:
                continue
            key = f"{kind}:{identifier}"
            record = self.memory_peaks.setdefault(key, {"kind": kind, "id": identifier, "name": name, "image": image,
                                                        "max_memory_bytes": None, "max_swap_bytes": None,
                                                        "max_memory_and_swap_bytes": None,
                                                        "memory_peak_bytes": None, "swap_peak_bytes": None})
            record["last_seen_utc"] = sample.get("utc")
            for metric, value in memory_usage(cg).items():
                if metric.endswith("limit_bytes"):
                    record[metric] = value
                peak = f"max_{metric}"
                if peak in record and isinstance(value, int):
                    old = record[peak]
                    record[peak] = value if old is None else max(old, value)
            # Kernel high-water marks can catch a spike between samples. Combined usage has no equivalent
            # kernel counter: its peak must come from simultaneous RAM/swap samples, never separate maxima.
            for metric, field in (("memory.peak", "memory_peak_bytes"), ("memory.swap.peak", "swap_peak_bytes")):
                value, old = cg.get(metric), record[field]
                if isinstance(value, int):
                    record[field] = value if old is None else max(old, value)

    def error(self, source, error):
        self.aggregate["collector_errors"] += 1
        self.events.write({**stamp(), "kind": "collector_error", "source": source, "error": str(error)[:500]})

    def stream(self, command, kind):
        def follow():
            recent_keys = deque(maxlen=256)
            last_event_nano = None
            last_cursor = None
            while not STOP.is_set():
                process = None
                try:
                    # Never interpolate command text or record full process environments.
                    arguments = list(command)
                    if kind == "docker" and last_event_nano is not None:
                        arguments[arguments.index("--since") + 1] = f"{last_event_nano // 10**9}.{last_event_nano % 10**9:09d}"
                    elif kind == "kernel" and last_cursor is not None:
                        arguments = [part for part in arguments if not part.startswith("--since=")]
                        arguments.append(f"--after-cursor={last_cursor}")
                    process = subprocess.Popen(arguments, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                               text=True, bufsize=1)
                    self.children.append(process)
                    for line in process.stdout:
                        if STOP.is_set():
                            break
                        if kind == "docker":
                            try:
                                event = json.loads(line)
                                action = event.get("Action", event.get("status"))
                                if action not in ("create", "start", "restart", "stop", "kill", "pause", "unpause", "die", "oom", "destroy"):
                                    continue  # exec events can contain command arguments and credentials.
                                actor = event.get("Actor", {})
                                attributes = actor.get("Attributes", {})
                                key = (event.get("timeNano"), actor.get("ID", event.get("id")), action)
                                if key in recent_keys:
                                    continue
                                recent_keys.append(key)
                                if event.get("timeNano"):
                                    last_event_nano = int(event["timeNano"])
                                record = {**stamp(), "kind": "docker_event", "action": action,
                                          "id": actor.get("ID", event.get("id")), "time_nano": event.get("timeNano"),
                                          "attributes": {k: attributes.get(k) for k in ("name", "image", "exitCode", "signal")}}
                                if action in ("die", "oom") and record["id"]:
                                    if action == "oom":
                                        self.aggregate["docker_oom_events"] += 1
                                    try:
                                        details = container_details(record["id"])
                                        details["cgroup"] = cgroup_sample(cgroup_for_pid(details["state"].get("Pid", 0)))
                                        record["container"] = details
                                    except (OSError, ValueError, http.client.HTTPException) as error:
                                        record["inspection_error"] = str(error)[:200]
                                self.events.write(record)
                            except ValueError:
                                self.error("docker_events", line.strip())
                        else:
                            try:
                                entry = json.loads(line)
                                key = entry.get("__CURSOR")
                                if key is not None:
                                    if key in recent_keys:
                                        continue
                                    recent_keys.append(key)
                                    last_cursor = key
                                message = str(entry.get("MESSAGE", ""))[:16384]
                                if any(text in message.lower() for text in ("out of memory", "oom-kill", "killed process")):
                                    self.aggregate["kernel_oom_messages"] += 1
                                self.kernel.write({**stamp(), "kernel_realtime_us": entry.get("__REALTIME_TIMESTAMP"),
                                                   "kernel_monotonic_us": entry.get("__MONOTONIC_TIMESTAMP"),
                                                   "message": message})
                            except ValueError:
                                self.error("kernel_journal", line.strip())
                    if not STOP.is_set():
                        self.error(kind, f"event stream exited: {process.wait()}")
                except OSError as error:
                    self.error(kind, error)
                finally:
                    if process is not None:
                        if process.poll() is None:
                            process.terminate()
                            try:
                                process.wait(timeout=2)
                            except subprocess.TimeoutExpired:
                                process.kill()
                        if process.stdout:
                            process.stdout.close()
                        self.children.remove(process)
                STOP.wait(5)

        thread = threading.Thread(target=follow, daemon=True)
        self.threads.append(thread)
        thread.start()

    def sample_containers(self):
        result, live = [], set()
        deadline = time.monotonic() + 3
        # Cgroup reads avoid one Docker stats request per container on every sample.
        for item in docker_get("/containers/json"):
            identifier = item["Id"]
            live.add(identifier)
            cached = self.containers.get(identifier)
            if cached is None or cached[1] is None or not cached[1].is_dir():
                if time.monotonic() > deadline:
                    result.append({"id": identifier, "name": (item.get("Names") or [identifier])[0],
                                   "image": item.get("Image"), "cgroup": None, "discovery_deferred": True})
                    continue
                try:
                    details = container_details(identifier)
                    path = cgroup_for_pid(details["state"].get("Pid", 0))
                    cached = details, path
                    self.containers[identifier] = cached
                    self.events.write({**stamp(), "kind": "container_discovered", "container": details,
                                       "cgroup_path": str(path) if path else None})
                except (OSError, ValueError, http.client.HTTPException) as error:
                    self.error("container_discovery", error)
                    continue
            details, path = cached
            result.append({"id": identifier, "name": details["name"], "image": details["image"],
                           "cgroup": cgroup_sample(path)})
        self.containers = {identifier: data for identifier, data in self.containers.items() if identifier in live}
        return result

    def summary(self, sample, final=False):
        memory = sample.get("host", {}).get("memory_bytes", {})
        swap = {**swap_activity(sample.get("host", {})), **sample.get("host", {}).get("swap", {})}
        total, available = memory.get("MemTotal"), memory.get("MemAvailable")
        top = []
        for container in sample.get("containers", []):
            cg = container.get("cgroup") or {}
            top.append({"id": container.get("id"), "name": container["name"], **memory_usage(cg),
                        "memory_peak_bytes": cg.get("memory.peak"), "swap_peak_bytes": cg.get("memory.swap.peak"),
                        "cpu_max": cg.get("cpu.max"), "cpu": cg.get("cpu.stat"),
                        "memory_events": cg.get("memory.events"), "swap_events": cg.get("memory.swap.events")})
        record = {**stamp(), "final": final, "elapsed_seconds": time.monotonic() - self.started,
                  **self.aggregate, "available_bytes": memory.get("MemAvailable"),
                  "memory_total_bytes": total,
                  "memory_used_bytes": total - available if total is not None and available is not None else None,
                  "cpu_count": os.cpu_count(),
                  "swap_used_bytes": swap["used_bytes"], "swap": swap,
                  "memory_peaks": list(self.memory_peaks.values()),
                  "cpu_percent": sample.get("host", {}).get("cpu_percent"),
                  "pressure": sample.get("host", {}).get("pressure"), "containers": top,
                  "browser": compact_cgroup(sample.get("browser")), "collector_pid": os.getpid(),
                  "workload": compact_cgroup(sample.get("workload")),
                  "collector": compact_cgroup(sample.get("collector")), "filesystems": sample.get("filesystems"),
                  "collection_seconds": sample.get("collection_seconds"), "docker_error": sample.get("docker_error")}
        atomic_json(self.output / "summary.json", record)
        self.summaries.write(record)

    def run(self):
        self.output.mkdir(parents=True, exist_ok=True)
        disk = os.statvfs(self.output)
        metadata = {**stamp(), "pid": os.getpid(), "kernel": os.uname().release,
                    "os_release": read("/etc/os-release"), "cpu_count": os.cpu_count(),
                    "clock_ticks_per_second": os.sysconf("SC_CLK_TCK"), "boot_id": read("/proc/sys/kernel/random/boot_id"),
                    "page_size_bytes": os.sysconf("SC_PAGE_SIZE"),
                    "sample_interval_seconds": self.interval, "summary_interval_seconds": self.summary_interval,
                    "disk_free_bytes": disk.f_bavail * disk.f_frsize,
                    "cgroup_controllers": read("/sys/fs/cgroup/cgroup.controllers"),
                    "collector_cgroup": str(cgroup_for_pid(os.getpid()))}
        try:
            version = docker_get("/version")
            metadata["docker"] = {k: version.get(k) for k in ("Version", "ApiVersion", "KernelVersion", "Os", "Arch")}
            info = docker_get("/info")
            metadata["docker"].update({k: info.get(k) for k in ("DockerRootDir", "Driver", "CgroupDriver", "CgroupVersion")})
            self.docker_root = Path(info.get("DockerRootDir") or "/var/lib/docker")
        except (OSError, ValueError, http.client.HTTPException) as error:
            self.error("docker_version", error)
        metadata["filesystems"] = filesystems((Path("/"), self.docker_root, self.output))
        atomic_json(self.output / "metadata.json", metadata)
        docker_events = ["docker", "events", "--since", str(int(time.time())), "--filter", "type=container"]
        for event in ("create", "start", "restart", "stop", "kill", "pause", "unpause", "die", "oom", "destroy"):
            docker_events.extend(["--filter", f"event={event}"])
        self.stream(docker_events + ["--format", "{{json .}}"], "docker")
        self.stream(["journalctl", "-k", "--follow", f"--since=@{int(time.time())}",
                     "--output=json", "--no-pager"], "kernel")
        previous, next_summary, sample = None, 0, {}
        try:
            while not STOP.is_set():
                started = time.monotonic()
                sample = stamp()
                try:
                    sample["host"] = host_sample()
                    sample["processes"] = processes()
                    sample["filesystems"] = filesystems((Path("/"), self.docker_root, self.output))
                    sample.update(resource_group_samples())
                    sample["collector"] = cgroup_sample(cgroup_for_pid(os.getpid()))
                    gap = None
                    if previous:
                        gap = sample["monotonic_seconds"] - previous["monotonic_seconds"]
                        sample["sample_gap_seconds"] = gap
                        self.aggregate["max_sample_gap_seconds"] = max(self.aggregate["max_sample_gap_seconds"], gap)
                        current, old = sample["host"]["cpu_ticks"], previous["host"]["cpu_ticks"]
                        delta = [a - b for a, b in zip(current, old)]
                        total = sum(delta)
                        if total > 0:
                            sample["host"]["cpu_percent"] = {"busy": 100 * (total - delta[3] - delta[4]) / total,
                                                             "iowait": 100 * delta[4] / total}
                    sample["host"]["swap"] = swap_activity(sample["host"], previous.get("host") if previous else None, gap)
                    try:
                        sample["containers"] = self.sample_containers()
                    except (OSError, ValueError, http.client.HTTPException) as error:
                        sample["docker_error"] = str(error)[:500]
                        self.error("docker_sample", error)
                    self.observe_usage(sample)
                    self.aggregate["samples"] += 1
                    memory = sample["host"]["memory_bytes"]
                    available = memory.get("MemAvailable")
                    if available is not None:
                        minimum = self.aggregate["min_available_bytes"]
                        self.aggregate["min_available_bytes"] = available if minimum is None else min(minimum, available)
                    swap_used = sample["host"]["swap"]["used_bytes"]
                    if swap_used is not None:
                        maximum = self.aggregate["max_swap_used_bytes"]
                        self.aggregate["max_swap_used_bytes"] = swap_used if maximum is None else max(maximum, swap_used)
                    self.aggregate["max_running_containers"] = max(self.aggregate["max_running_containers"],
                                                                   len(sample.get("containers", [])))
                    previous = sample
                except (OSError, ValueError, IndexError) as error:
                    self.aggregate["sampling_errors"] += 1
                    self.error("host_sample", error)
                sample["collection_seconds"] = time.monotonic() - started
                self.samples.write(sample)
                if started >= next_summary:
                    self.summary(sample)
                    next_summary = started + self.summary_interval
                STOP.wait(max(0, self.interval - (time.monotonic() - started)))
        finally:
            STOP.set()
            for child in list(self.children):
                if child.poll() is None:
                    child.terminate()
            for thread in self.threads:
                thread.join(timeout=3)
            self.summary(sample, final=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--sample-interval", type=float, default=5)
    parser.add_argument("--summary-interval", type=float, default=60)
    args = parser.parse_args()
    if args.sample_interval <= 0 or args.summary_interval <= 0:
        parser.error("intervals must be positive")
    for signum in (signal.SIGTERM, signal.SIGINT):
        signal.signal(signum, lambda *_: STOP.set())
    Collector(args.output.resolve(), args.sample_interval, args.summary_interval).run()
