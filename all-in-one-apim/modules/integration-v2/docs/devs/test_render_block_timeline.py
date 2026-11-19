"""Unit tests for the best-effort CI timeline renderer (stdlib only)."""
import contextlib
import copy
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import render_block_timeline as timeline


def fixture():
    return {
        "schemaVersion": 1,
        "generatedAtMs": 5000,
        "totals": {"passed": 3, "failed": 0, "skipped": 1, "configurationFailed": 0},
        "blocks": [
            {"name": "IntegrationV2-One", "suite": "v2", "thread": "TestNG-tests-1",
             "startTimeMs": 1000, "endTimeMs": 3000, "durationMs": 2000, "bootFailed": False,
             "runners": [{"className": "example.OneRunner", "passed": 2, "failed": 0,
                          "skipped": 0, "configurationFailed": 0}]},
            {"name": "IntegrationV2-Distributed-Two", "suite": "v2", "thread": "TestNG-tests-2",
             "startTimeMs": 1100, "endTimeMs": 4000, "durationMs": 2900, "bootFailed": False,
             "runners": [{"className": "example.TwoRunner", "passed": 1, "failed": 0,
                          "skipped": 1, "configurationFailed": 0}]},
        ],
    }


class RenderTimelineTest(unittest.TestCase):
    def test_fixture_mermaid(self):
        markdown = timeline.render(fixture())
        self.assertIn('subgraph lane0["TestNG-tests-1"]', markdown)
        self.assertIn('subgraph lane1["TestNG-tests-2"]', markdown)
        self.assertIn('b0("✅ One<br/>2s"):::pass', markdown)
        self.assertIn('b1("✅ Two<br/>2s"):::pass', markdown)
        self.assertIn('start --> b0\n  b0 --> totals', markdown)
        self.assertIn('4 tests run<br/>0 failed · 1 skipped', markdown)
        self.assertIn('<details>\n<summary>All blocks: runner results</summary>', markdown)
        self.assertIn('✅ OneRunner<br/>2 pass · 0 fail · 0 skip', markdown)
        self.assertNotIn('Failed blocks:', markdown)
        self.assertIn('flowchart LR', markdown)

    def test_failed_runner_colours_block_and_expands_details(self):
        report = fixture()
        report['blocks'][0]['runners'][0]['failed'] = 1
        markdown = timeline.render(report)
        self.assertIn('b0("❌ One<br/>2s"):::fail', markdown)
        self.assertIn('❌ OneRunner', markdown)
        self.assertLess(markdown.index('Failed blocks:'), markdown.index('<details>'))
        self.assertEqual(markdown.count('❌ OneRunner'), 2)

    def test_configuration_and_boot_failure_are_red(self):
        for boot in (False, True):
            report = fixture()
            report['blocks'][0]['bootFailed'] = boot
            report['blocks'][0]['runners'][0]['configurationFailed'] = int(not boot)
            self.assertIn('b0("❌ One', timeline.render(report))

    def test_skipped_runner_and_lane_order(self):
        report = fixture()
        later = copy.deepcopy(report['blocks'][0])
        later['name'] = 'IntegrationV2-Later'
        later['startTimeMs'] = 4000
        later['runners'][0].update(passed=0, skipped=2)
        report['blocks'].insert(0, later)
        markdown = timeline.render(report)
        self.assertIn('b1 --> b0', markdown)
        self.assertIn('⏭ OneRunner', markdown)

    def test_missing_json_and_cli_exit_zero(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / 'summary.md'
            output.write_text('existing\n')
            self.assertEqual(timeline.main(['--input', str(Path(directory) / 'missing.json'),
                                           '--output', str(output)]), 0)
            self.assertTrue(output.read_text().startswith('existing\nv2 suite execution: block-timeline.json'))

    def test_json_cli_uses_github_summary_environment(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / 'timeline.json'
            source.write_text(json.dumps(fixture()))
            output = Path(directory) / 'summary.md'
            with patch.dict('os.environ', {'GITHUB_STEP_SUMMARY': str(output)}):
                self.assertEqual(timeline.main(['--input', str(source)]), 0)
            self.assertEqual(output.read_text(), timeline.render(fixture()))

    def test_malformed_json_and_unwritable_summary_exit_zero(self):
        with tempfile.TemporaryDirectory() as directory, contextlib.redirect_stderr(io.StringIO()):
            source = Path(directory) / 'bad.json'
            source.write_text('{bad')
            output = Path(directory) / 'summary.md'
            self.assertEqual(timeline.main(['--input', str(source), '--output', str(output)]), 0)
            self.assertIn('could not be rendered', output.read_text())
            self.assertEqual(timeline.main(['--input', str(source), '--output', directory]), 0)

    def test_empty_group_filtered_suite(self):
        report = fixture()
        report['blocks'] = []
        self.assertIn('No executed blocks', timeline.render(report))

    def test_label_escaping(self):
        report = fixture()
        report['blocks'][0]['name'] = 'IntegrationV2-"<script>&[x]'
        markdown = timeline.render(report)
        self.assertIn('&quot;&lt;script&gt;&amp;[x]', markdown)
        self.assertNotIn('<script>', markdown)

    def test_large_map_splits_diagrams_below_limits(self):
        report = fixture()
        template = report['blocks'][0]
        report['blocks'] = []
        for number in range(100):
            block = copy.deepcopy(template)
            block['name'] = f'IntegrationV2-{number}'
            block['runners'] *= 10
            report['blocks'].append(block)
        markdown = timeline.render(report)
        graphs = [part.split('```', 1)[0] for part in markdown.split('```mermaid\n')[1:]]
        self.assertGreater(len(graphs), 4)
        for graph in graphs:
            self.assertLessEqual(len(graph), timeline.MAX_DIAGRAM_CHARS)
            self.assertLessEqual(graph.count(' --> '), timeline.MAX_DIAGRAM_EDGES)
        for graph in graphs[3:]:
            self.assertLessEqual(graph.count(' --> '), 24)
        self.assertLess(len(markdown.encode()), timeline.MAX_SUMMARY_BYTES)


if __name__ == '__main__':
    unittest.main()
