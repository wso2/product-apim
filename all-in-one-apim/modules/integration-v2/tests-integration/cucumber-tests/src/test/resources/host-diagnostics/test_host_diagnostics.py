"""Focused, unprivileged regression checks: python3 -m unittest discover -s <this directory>."""

import contextlib
import importlib.util
import io
import json
from pathlib import Path
import subprocess
import tempfile
import threading
import unittest
from unittest.mock import patch


def module(filename):
    spec = importlib.util.spec_from_file_location(filename.replace("-", "_"), Path(__file__).with_name(filename + ".py"))
    loaded = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(loaded)
    return loaded


collect = module("collect-host-diagnostics")
wrapper = module("run-with-host-diagnostics")


class HostDiagnosticsTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.output = Path(self.temporary.name)

    def test_missing_file_is_missing_not_zero(self):
        self.assertIsNone(collect.read(self.output / "absent"))
        self.assertIsNone(collect.cgroup_sample(self.output / "absent"))

    def test_counter_parsing(self):
        path = self.output / "counters"
        path.write_text("oom 2\noom_kill 1\nnot-a-counter\nignored max\n")
        self.assertEqual({"oom": 2, "oom_kill": 1}, collect.counters(path))

    def test_cgroup_caps_and_events(self):
        for name, value in {"memory.current": "123", "memory.max": "max", "cpu.max": "50000 100000",
                            "cpu.stat": "usage_usec 900\nnr_throttled 7\n", "memory.events": "oom_kill 2\n",
                            "memory.swap.current": "12", "memory.swap.peak": "24",
                            "memory.swap.events": "max 1\nfail 0\n"}.items():
            (self.output / name).write_text(value)
        sample = collect.cgroup_sample(self.output)
        self.assertEqual(123, sample["memory.current"])
        self.assertEqual("max", sample["memory.max"])
        self.assertEqual("50000 100000", sample["cpu.max"])
        self.assertEqual(7, sample["cpu.stat"]["nr_throttled"])
        self.assertEqual(2, sample["memory.events"]["oom_kill"])
        self.assertEqual(24, sample["memory.swap.peak"])
        self.assertEqual(1, sample["memory.swap.events"]["max"])
        self.assertEqual(135, collect.compact_cgroup(sample)["memory_and_swap_bytes"])

    def test_pressure_parsing(self):
        (self.output / "memory").write_text("some avg10=1.50 avg60=0.25 avg300=0.10 total=12345\n")
        sample = collect.pressure(self.output)
        self.assertEqual(1.5, sample["memory"]["some"]["avg10"])
        self.assertEqual(12345, sample["memory"]["some"]["total"])
        self.assertNotIn("cpu", sample)

    def test_distributed_budget_and_browser_are_sampled_under_the_slice(self):
        workload = self.output / "apimdistributed.slice"
        browser = workload / "apimdistributed-browser.service"
        browser.mkdir(parents=True)
        (workload / "memory.high").write_text(str(14 * 1024**3))
        (workload / "memory.max").write_text(str(15 * 1024**3))
        (workload / "memory.swap.max").write_text(str(5 * 1024**3))
        (workload / "memory.events").write_text("high 9\noom_kill 1\n")
        (browser / "memory.max").write_text(str(512 * 1024**2))
        (browser / "memory.swap.max").write_text(str(128 * 1024**2))
        with patch.object(collect, "DISTRIBUTED_CGROUP", workload), \
                patch.object(collect, "BROWSER_CGROUP", self.output / "absent"):
            sample = collect.resource_group_samples()
        collector = collect.Collector(self.output, 5, 60)
        collector.summary(sample)
        summary = json.loads((self.output / "summary.json").read_text())
        self.assertEqual(14 * 1024**3, summary["workload"]["memory.high"])
        self.assertEqual(15 * 1024**3, summary["workload"]["memory.max"])
        self.assertEqual(5 * 1024**3, summary["workload"]["memory.swap.max"])
        self.assertEqual(9, summary["workload"]["memory.events"]["high"])
        self.assertEqual(1, summary["workload"]["memory.events"]["oom_kill"])
        self.assertEqual(512 * 1024**2, summary["browser"]["memory.max"])
        self.assertEqual(128 * 1024**2, summary["browser"]["memory.swap.max"])

    def test_all_in_one_browser_is_sampled_without_a_distributed_slice(self):
        browser = self.output / "pw-browser"
        browser.mkdir()
        (browser / "memory.max").write_text(str(2560 * 1024**2))
        (browser / "memory.swap.max").write_text("0")
        with patch.object(collect, "DISTRIBUTED_CGROUP", self.output / "absent"), \
                patch.object(collect, "BROWSER_CGROUP", browser):
            sample = collect.resource_group_samples()
        self.assertIsNone(sample["workload"])
        self.assertEqual(2560 * 1024**2, sample["browser"]["memory.max"])
        self.assertEqual(0, sample["browser"]["memory.swap.max"])

    def test_filesystem_usage_and_missing_paths(self):
        sample = collect.filesystems([self.output, self.output / "missing"])
        self.assertGreater(sample[str(self.output)]["available_bytes"], 0)
        self.assertIn("device", sample[str(self.output)])
        self.assertIn("error", sample[str(self.output / "missing")])

    def test_line_flushing_and_rotation(self):
        path = self.output / "records.jsonl"
        log = collect.JsonLog(path, limit=20, backups=2)
        for number in range(5):
            log.write({"number": number})
            self.assertEqual(number, json.loads(path.read_text())["number"])
        self.assertEqual(3, json.loads(Path(str(path) + ".1").read_text())["number"])
        self.assertEqual(2, json.loads(Path(str(path) + ".2").read_text())["number"])
        self.assertFalse(Path(str(path) + ".3").exists())

    def test_container_metadata_does_not_include_secrets(self):
        data = {"Name": "/gateway", "Config": {"Image": "gateway:test", "Env": ["PASSWORD=secret"],
                                                 "Cmd": ["secret"], "Labels": {"secret": "value"}},
                "HostConfig": {"Memory": 1024}, "State": {"Pid": 12, "OOMKilled": True},
                "NetworkSettings": {"Networks": {"lane": {"IPAddress": "10.0.0.2", "Aliases": ["gateway"]}}}}
        with patch.object(collect, "docker_get", return_value=data):
            metadata = collect.container_details("identifier")
        self.assertNotIn("secret", json.dumps(metadata))
        self.assertEqual("gateway", metadata["name"])
        self.assertTrue(metadata["state"]["OOMKilled"])

    def test_final_summary_retains_extrema(self):
        collector = collect.Collector(self.output, 5, 60)
        collector.aggregate["min_available_bytes"] = 123
        collector.aggregate["max_sample_gap_seconds"] = 72
        collector.summary({"host": {"memory_bytes": {"MemAvailable": 1024}}}, final=True)
        summary = json.loads((self.output / "summary.json").read_text())
        self.assertTrue(summary["final"])
        self.assertEqual(123, summary["min_available_bytes"])
        self.assertEqual(72, summary["max_sample_gap_seconds"])
        self.assertFalse((self.output / "summary.json.tmp").exists())

    def test_memory_and_swap_are_combined_without_treating_unknown_as_zero(self):
        usage = collect.memory_usage({"memory.current": 100, "memory.swap.current": 30,
                                      "memory.max": 200, "memory.swap.max": 50})
        self.assertEqual(130, usage["memory_and_swap_bytes"])
        self.assertEqual(250, usage["memory_and_swap_limit_bytes"])
        self.assertEqual(30, usage["swap_bytes"])
        self.assertIsNone(collect.memory_usage({"memory.current": 100})["memory_and_swap_bytes"])
        self.assertIsNone(collect.memory_usage(None)["swap_bytes"])
        self.assertEqual("max", collect.memory_usage({"memory.max": "max", "memory.swap.max": 0})[
            "memory_and_swap_limit_bytes"])

    def test_swap_activity_uses_page_size_and_monotonic_interval(self):
        old = {"vmstat": {"pswpin": 10, "pswpout": 20}}
        host = {"memory_bytes": {"SwapTotal": 1000, "SwapFree": 750},
                "vmstat": {"pswpin": 13, "pswpout": 25}}
        with patch.object(collect.os, "sysconf", return_value=65536):
            usage = collect.swap_activity(host, old, 5)
        self.assertEqual(250, usage["used_bytes"])
        self.assertEqual(3 * 65536 / 5, usage["in_bytes_per_second"])
        self.assertEqual(65536, usage["out_bytes_per_second"])
        self.assertIsNone(collect.swap_activity(host)["in_bytes_per_second"])
        self.assertIsNone(collect.swap_activity(host, old, 0)["out_bytes_per_second"])
        self.assertIsNone(collect.swap_activity({}, old, 5)["used_bytes"])
        self.assertIsNone(collect.swap_activity({"vmstat": {"pswpin": 1}}, old, 5)["in_bytes_per_second"])

    def test_summary_retains_sampled_cpu_and_memory_peaks_after_container_exit(self):
        collector = collect.Collector(self.output, 5, 60)
        container = {"id": "original", "name": "cp", "image": "cp:test",
                     "cgroup": {"memory.current": 100, "memory.swap.current": 40,
                                "memory.max": 200, "memory.swap.max": 50, "memory.peak": 120}}
        first = {"utc": "first", "host": {"memory_bytes": {"MemTotal": 1000, "MemAvailable": 250},
                                         "cpu_percent": {"busy": 90, "iowait": 3},
                                         "swap": {"in_bytes_per_second": 10, "out_bytes_per_second": 20}},
                 "containers": [container], "browser": {"memory.current": 50, "memory.swap.current": 10}}
        collector.observe_usage(first)
        collector.summary(first)
        summary = json.loads((self.output / "summary.json").read_text())
        self.assertEqual(40, summary["containers"][0]["swap_bytes"])
        self.assertEqual(50, summary["containers"][0]["swap_limit_bytes"])
        self.assertEqual(140, summary["containers"][0]["memory_and_swap_bytes"])
        self.assertEqual(120, summary["containers"][0]["memory_peak_bytes"])
        # RAM and swap reach their separate peaks in different samples: do not sum those peaks.
        container["cgroup"].update({"memory.current": 130, "memory.swap.current": 0})
        collector.observe_usage({"utc": "second", "containers": [container]})
        final = {"host": {"cpu_percent": {"busy": 10, "iowait": 1}}, "containers": []}
        collector.observe_usage(final)
        collector.summary(final, final=True)
        summary = json.loads((self.output / "summary.json").read_text())
        self.assertEqual(90, summary["max_cpu_busy_percent"])
        self.assertEqual("first", summary["max_cpu_busy_percent_utc"])
        self.assertEqual(750, summary["max_memory_used_bytes"])
        self.assertEqual(3, summary["max_cpu_iowait_percent"])
        self.assertEqual(20, summary["max_swap_out_bytes_per_second"])
        peaks = {entry["id"]: entry for entry in summary["memory_peaks"]}
        self.assertEqual(130, peaks["original"]["max_memory_bytes"])
        self.assertEqual(40, peaks["original"]["max_swap_bytes"])
        self.assertEqual(140, peaks["original"]["max_memory_and_swap_bytes"])
        self.assertEqual(120, peaks["original"]["memory_peak_bytes"])
        self.assertEqual(50, peaks["original"]["swap_limit_bytes"])
        self.assertEqual(60, peaks["browser"]["max_memory_and_swap_bytes"])
        self.assertEqual("second", peaks["original"]["last_seen_utc"])
        self.assertEqual([], summary["containers"])

    def test_peak_identity_uses_container_id_and_missing_metrics_remain_unknown(self):
        collector = collect.Collector(self.output, 5, 60)
        collector.observe_usage({"containers": [
            {"id": "one", "name": "cp", "cgroup": {"memory.current": 100}},
            {"id": "two", "name": "cp", "cgroup": {"memory.current": 20, "memory.swap.current": 0}}]})
        collector.summary({})
        summary = json.loads((self.output / "summary.json").read_text())
        self.assertIsNone(summary["max_cpu_busy_percent"])
        peaks = {entry["id"]: entry for entry in summary["memory_peaks"]}
        self.assertIsNone(peaks["one"]["max_swap_bytes"])
        self.assertIsNone(peaks["one"]["max_memory_and_swap_bytes"])
        self.assertEqual(0, peaks["two"]["max_swap_bytes"])
        self.assertEqual(20, peaks["two"]["max_memory_bytes"])

    def check_stream_reconnect(self, kind, records, command):
        stop = threading.Event()
        launches = []

        def launch(arguments, **_kwargs):
            index = len(launches)
            launches.append(arguments)
            process = unittest.mock.Mock()

            def lines():
                for record in records[index]:
                    yield json.dumps(record) + "\n"
                if index == 1:
                    stop.set()

            process.stdout = unittest.mock.Mock()
            process.stdout.__iter__ = unittest.mock.Mock(side_effect=lines)
            process.wait.return_value, process.poll.return_value = 1, 0
            return process

        collector = collect.Collector(self.output, 5, 60)
        with patch.object(collect, "STOP", stop), patch.object(stop, "wait", side_effect=lambda _: stop.is_set()), \
                patch.object(collect.subprocess, "Popen", side_effect=launch), \
                patch.object(collect, "container_details", side_effect=OSError("already removed")):
            collector.stream(command, kind)
            collector.threads[0].join(timeout=3)
            self.assertFalse(collector.threads[0].is_alive())
        self.assertEqual(2, len(launches))
        return collector, launches

    def test_docker_reconnect_resumes_without_recounting_oom(self):
        event = {"Action": "oom", "Actor": {"ID": "owned"}, "timeNano": 1000000001}
        collector, calls = self.check_stream_reconnect("docker", [[event], [event]],
                                                       ["docker", "events", "--since", "1"])
        self.assertEqual("1.000000001", calls[1][calls[1].index("--since") + 1])
        self.assertEqual(1, collector.aggregate["docker_oom_events"])

    def test_journal_reconnect_uses_cursor_and_deduplicates(self):
        first = {"__CURSOR": "first", "MESSAGE": "oom-kill: owned test"}
        second = {"__CURSOR": "second", "MESSAGE": "normal"}
        collector, calls = self.check_stream_reconnect("kernel", [[first], [first, second]],
                                                       ["journalctl", "-k", "--follow", "--since=@1"])
        self.assertIn("--after-cursor=first", calls[1])
        self.assertNotIn("--since=@1", calls[1])
        self.assertEqual(1, collector.aggregate["kernel_oom_messages"])

    def test_wrapper_uses_sixty_second_summaries_and_only_caps_itself(self):
        calls = []
        def fake_service(*arguments):
            calls.append(arguments)
            return subprocess.CompletedProcess(arguments, 0, "")
        with patch.object(wrapper, "service", side_effect=fake_service), contextlib.redirect_stdout(io.StringIO()):
            wrapper.start(self.output)
        arguments = calls[0]
        self.assertEqual("60", arguments[arguments.index("--summary-interval") + 1])
        self.assertEqual("5", arguments[arguments.index("--sample-interval") + 1])
        self.assertIn("--property=CPUQuota=10%", arguments)
        self.assertIn("--property=MemoryMax=192M", arguments)
        self.assertNotIn("--property=OOMScoreAdjust=-100", arguments)

    def test_ci_log_retains_all_peaks_in_small_batches_without_modifying_summary(self):
        peaks = [{"id": str(index), "max_swap_bytes": index} for index in range(21)]
        record = {"utc": "sample-time", "max_cpu_busy_percent": 99, "memory_peaks": peaks}
        captured = io.StringIO()
        with contextlib.redirect_stdout(captured):
            wrapper.emit_summary(record)
        records = [json.loads(line.removeprefix("[host-diagnostics] ")) for line in captured.getvalue().splitlines()]
        self.assertEqual(99, records[0]["max_cpu_busy_percent"])
        self.assertEqual(21, records[0]["memory_peak_records"])
        self.assertEqual([0, 8, 16], [item["memory_peaks_offset"] for item in records[1:]])
        self.assertEqual(peaks, [peak for item in records[1:] for peak in item["memory_peaks"]])
        self.assertTrue(all(len(item["memory_peaks"]) <= 8 for item in records[1:]))
        self.assertTrue(all(item["utc"] == "sample-time" for item in records))
        self.assertEqual(peaks, record["memory_peaks"])

    def test_maven_exit_status_and_arguments_are_preserved(self):
        for status in (0, 7, -9):
            with self.subTest(status=status):
                process = unittest.mock.Mock()
                process.poll.return_value = status
                process.wait.return_value = status
                with patch.object(wrapper, "start", return_value="unit"), patch.object(wrapper, "stop"), \
                        patch.object(wrapper.subprocess, "Popen", return_value=process) as launch:
                    result = wrapper.run(self.output, ["mvn", "test", "-Dvalue=unchanged"])
                launch.assert_called_once_with(["mvn", "test", "-Dvalue=unchanged"], start_new_session=True)
                self.assertEqual(status if status >= 0 else 128 - status, result)

    def test_cleanup_failure_does_not_replace_maven_failure(self):
        process = unittest.mock.Mock()
        process.poll.return_value, process.wait.return_value = 7, 7
        with patch.object(wrapper, "start", return_value="unit"), \
                patch.object(wrapper, "stop", side_effect=OSError("unavailable")), \
                patch.object(wrapper.subprocess, "Popen", return_value=process), contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(7, wrapper.run(self.output, ["mvn", "test"]))

    def test_stop_does_not_touch_other_services(self):
        (self.output / "service-unit.txt").write_text("docker.service\n")
        with patch.object(wrapper, "service") as call:
            with self.assertRaises(ValueError):
                wrapper.stop(self.output)
        call.assert_not_called()

    def test_stop_without_start_is_noop(self):
        with patch.object(wrapper, "service") as call:
            wrapper.stop(self.output)
        call.assert_not_called()

    def test_reusing_output_is_rejected(self):
        (self.output / "metadata.json").write_text("{}")
        with self.assertRaises(ValueError):
            wrapper.start(self.output)


if __name__ == "__main__":
    unittest.main()
