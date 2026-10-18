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
                            "cpu.stat": "usage_usec 900\nnr_throttled 7\n", "memory.events": "oom_kill 2\n"}.items():
            (self.output / name).write_text(value)
        sample = collect.cgroup_sample(self.output)
        self.assertEqual(123, sample["memory.current"])
        self.assertEqual("max", sample["memory.max"])
        self.assertEqual("50000 100000", sample["cpu.max"])
        self.assertEqual(7, sample["cpu.stat"]["nr_throttled"])
        self.assertEqual(2, sample["memory.events"]["oom_kill"])

    def test_pressure_parsing(self):
        (self.output / "memory").write_text("some avg10=1.50 avg60=0.25 avg300=0.10 total=12345\n")
        sample = collect.pressure(self.output)
        self.assertEqual(1.5, sample["memory"]["some"]["avg10"])
        self.assertEqual(12345, sample["memory"]["some"]["total"])
        self.assertNotIn("cpu", sample)

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
