"""Unit fixtures for the standalone suite registration validator (stdlib only)."""

from contextlib import redirect_stdout
from io import StringIO
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import suite_alignment as alignment


def block(name="Example", distributed=False, runners=("pkg.FooRunner",), parameters=None, threads="1"):
    prefix = "IntegrationV2-Distributed-" if distributed else "IntegrationV2-"
    label = ("distributed-" if distributed else "") + name.lower()
    params = {"blockLabel": label, **(parameters or {})}
    return (f'  <test name="{prefix}{name}" parallel="classes" thread-count="{threads}">\n'
            + "".join(f'    <parameter name="{key}" value="{value}"/>\n' for key, value in params.items())
            + '    <classes>\n'
            + "".join(f'      <class name="{runner}"/>\n' for runner in runners)
            + '    </classes>\n  </test>\n')


def suite(blocks=None, distributed=False, extra="", threads="4"):
    return (f'<suite name="Product" parallel="tests" thread-count="{threads}">\n'
            + extra + (block(distributed=distributed) if blocks is None else blocks) + '</suite>\n')


class SuiteAlignmentTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name).resolve()
        self.a = self.root / "testng-v2.xml"
        self.d = self.root / "testng-v2_distributed.xml"
        self.ignore = self.root / "suite-alignment.ignore"

    def validate(self, a=None, d=None, ignore=""):
        self.a.write_text(suite() if a is None else a, encoding="utf-8")
        self.d.write_text(suite(distributed=True) if d is None else d, encoding="utf-8")
        self.ignore.write_text(ignore, encoding="utf-8")
        return alignment.validate(self.a, self.d, self.ignore)

    def assert_problem(self, result, text):
        self.assertTrue(any(text in item.message for item in result.problems), result.problems)

    def test_identical_layouts(self):
        result = self.validate()
        self.assertEqual([], result.problems)
        self.assertEqual("suite-alignment: 1 paired blocks · 1 matched runner entries · "
                         "0 declared differences · 0 undeclared · 0 stale", result.summary())

    def test_comments_are_not_registrations(self):
        a = suite().replace('<classes>', '<classes><!-- <class name="pkg.ParkedRunner"/> -->')
        self.assertEqual([], self.validate(a=a).problems)

    def test_external_doctype_is_not_fetched(self):
        header = '<!DOCTYPE suite SYSTEM "file:///nonexistent/suite.dtd">\n'
        self.assertEqual([], self.validate(a=header + suite()).problems)

    def test_block_and_label_prefixes(self):
        self.assert_problem(self.validate(d=suite(distributed=True).replace(
            "IntegrationV2-Distributed-", "Wrong-")), "must start with")
        self.assert_problem(self.validate(d=suite(distributed=True).replace(
            'value="distributed-example"', 'value="example"')), "blockLabel")

    def test_prefix_replacement_is_anchored(self):
        self.assert_problem(self.validate(d=suite(distributed=True).replace(
            "IntegrationV2-Distributed-Example", "Other-IntegrationV2-Distributed-Example")), "must start with")

    def test_missing_label(self):
        self.assert_problem(self.validate(d=suite(distributed=True).replace(
            '    <parameter name="blockLabel" value="distributed-example"/>\n', '')), "blockLabel")

    def test_matching_duplicates_across_and_within_blocks(self):
        a = block(runners=("pkg.FooRunner", "pkg.FooRunner")) + block("Other")
        d = block(distributed=True, runners=("pkg.FooRunner", "pkg.FooRunner")) + block("Other", True)
        result = self.validate(suite(a), suite(d))
        self.assertEqual([], result.problems)
        self.assertEqual(3, result.matched)

    def test_multiplicity_difference(self):
        result = self.validate(d=suite(block(distributed=True, runners=("pkg.FooRunner", "pkg.FooRunner"))))
        self.assert_problem(result, "all-in-one=1, distributed=2")
        self.assertEqual(1, result.matched)

    def test_missing_occurrence_when_class_exists_in_another_block(self):
        a = block() + block("Other")
        d = block(distributed=True, runners=()) + block("Other", True)
        result = self.validate(suite(a), suite(d))
        self.assertEqual(1, result.undeclared)
        self.assert_problem(result, "Example runner 'pkg.FooRunner'")

    def test_fqcn_collisions_are_not_collapsed(self):
        result = self.validate(d=suite(block(distributed=True, runners=("other.FooRunner",))))
        self.assertEqual(2, result.undeclared)
        self.assertEqual(0, result.matched)

    def test_missing_block(self):
        result = self.validate(a=suite(block() + block("Other")))
        self.assert_problem(result, "Block 'Other' is missing in distributed")

    def test_block_order(self):
        result = self.validate(suite(block() + block("Other")),
                               suite(block("Other", True) + block(distributed=True)))
        self.assert_problem(result, "Block order differs")

    def test_runner_order(self):
        result = self.validate(suite(block(runners=("pkg.FooRunner", "pkg.BarRunner"))),
                               suite(block(distributed=True, runners=("pkg.BarRunner", "pkg.FooRunner"))))
        self.assert_problem(result, "runner order differs")

    def test_declared_missing_runner_does_not_change_shared_order(self):
        result = self.validate(suite(block(runners=("pkg.BarRunner", "pkg.FooRunner"))),
                               ignore="Example runner:BarRunner missing-in=distributed | Resource budget\n")
        self.assertEqual([], result.problems)
        self.assertEqual(1, result.declared)

    def test_local_parameter_difference_and_line(self):
        result = self.validate(suite(block(parameters={"initSecondaryUserStore": "true"})),
                               suite(block(distributed=True, parameters={"initSecondaryUserStore": "false"})))
        self.assert_problem(result, "parameter 'initSecondaryUserStore' differs")
        self.assertEqual(self.d, result.problems[0].path)
        self.assertEqual(4, result.problems[0].line)

    def test_suite_parameter_does_not_replace_local_parameter(self):
        result = self.validate(suite(block(parameters={"initSecondaryUserStore": "true"})),
                               suite(distributed=True, extra='<parameter name="initSecondaryUserStore" value="true"/>\n'))
        self.assert_problem(result, "Example parameter 'initSecondaryUserStore' is missing")

    def test_declared_missing_parameter(self):
        result = self.validate(a=suite(block(parameters={"initSolaceBroker": "true"})),
                               ignore="Example parameter:initSolaceBroker missing-in=distributed | No broker consumer\n")
        self.assertEqual([], result.problems)
        self.assertEqual(1, result.declared)

    def test_ignore_does_not_allow_changed_value(self):
        result = self.validate(suite(block(parameters={"flag": "true"})),
                               suite(block(distributed=True, parameters={"flag": "false"})),
                               "Example parameter:flag missing-in=distributed | Fixture reason\n")
        self.assert_problem(result, "parameter 'flag' differs")
        self.assertEqual(1, result.stale)

    def test_suite_and_block_scheduling(self):
        for d in [suite(distributed=True, threads="2"), suite(block(distributed=True, threads="2")),
                  suite(distributed=True).replace('parallel="classes"', 'parallel="methods"')]:
            with self.subTest(d=d):
                self.assert_problem(self.validate(d=d), "attribute")

    def test_expected_topology_fields(self):
        a = suite(block(parameters={"tomlExtraOverlayPath": "aio.toml", "serverFilesToCopy": "a::b"}),
                  extra=f'<listeners><listener class-name="{alignment.LIFECYCLE}BlockLifecycleListener"/></listeners>\n')
        d = suite(block(distributed=True, parameters={"tomlExtraOverlayPath.cp": "cp.toml",
                   "tomlExtraOverlayPath.tm": "tm.toml", "serverFilesToCopy.gateway": "c::d"}),
                  extra='<parameter name="apim.topology" value="distributed"/>\n'
                        f'<listeners><listener class-name="{alignment.LIFECYCLE}DistributedLifecycleListener"/></listeners>\n')
        self.assertEqual([], self.validate(a, d).problems)

    def test_unknown_component_and_is_overlay_are_compared(self):
        for name in ["tomlExtraOverlayPath.typo", "isTomlExtraOverlayPath"]:
            with self.subTest(name=name):
                self.assert_problem(self.validate(a=suite(block(parameters={name: "x"}))), "missing in distributed")

    def test_wrong_topology_file_parameter(self):
        self.assert_problem(self.validate(d=suite(block(distributed=True, parameters={"tomlExtraOverlayPath": "x"}))),
                            "Unexpected topology configuration")

    def test_other_listener_difference(self):
        result = self.validate(d=suite(distributed=True, extra='<listeners><listener class-name="pkg.Other"/></listeners>'))
        self.assert_problem(result, "Suite listeners differ")

    def test_unsupported_selectors(self):
        for selector in ['<packages><package name="pkg.*"/></packages>', '<groups/>', '<suite-files/>',
                         '<method-selectors/>', '<methods><include name="run"/></methods>']:
            with self.subTest(selector=selector):
                self.assert_problem(self.validate(d=suite(distributed=True, extra=selector)), "Unsupported suite selector")
        self.assert_problem(self.validate(d=suite(block(distributed=True, runners=("pkg.*Runner",)))), "class pattern")

    def test_disabled_registration(self):
        self.assert_problem(self.validate(d=suite(distributed=True).replace('<class name=', '<class enabled="false" name=')),
                            "Unsupported disabled")

    def test_selector_attribute_and_class_parameters(self):
        self.assert_problem(self.validate(d=suite(distributed=True).replace('<suite ', '<suite groups="smoke" ')),
                            "Unsupported group or method selector attribute")
        self.assert_problem(self.validate(d=suite(distributed=True).replace(
            '<class name="pkg.FooRunner"/>',
            '<class name="pkg.FooRunner"><parameter name="flag" value="true"/></class>')),
                            "Unsupported class-local parameters")

    def test_bad_ignore_entries(self):
        cases = [("Example runner:FooRunner missing-in=distributed |  ", "nonblank reason"),
                 ("Example runner:FooRunner missing-in=distributed", "Invalid suite alignment ignore"),
                 ("Example mystery:FooRunner missing-in=distributed | why", "Unknown ignore kind"),
                 ("* runner:FooRunner missing-in=distributed | why", "without prefixes or wildcards"),
                 ("Example runner:*Runner missing-in=distributed | why", "without prefixes or wildcards"),
                 ("IntegrationV2-Example runner:FooRunner missing-in=distributed | why", "without prefixes"),
                 ("Example runner:FooRunner missing-in=other | why", "Invalid suite alignment ignore")]
        for text, message in cases:
            with self.subTest(text=text):
                self.assert_problem(self.validate(ignore=text), message)

    def test_duplicate_ignore_including_simple_fqcn_alias(self):
        for target in ["FooRunner", "pkg.FooRunner"]:
            with self.subTest(target=target):
                result = self.validate(ignore="Example runner:FooRunner missing-in=distributed | why\n"
                                       f"Example runner:{target} missing-in=distributed | why\n")
                self.assert_problem(result, "Duplicate ignore entry")
                self.assertEqual(2, next(p.line for p in result.problems if "Duplicate" in p.message))

    def test_ambiguous_simple_ignore(self):
        result = self.validate(a=suite(block(runners=("pkg.FooRunner", "other.FooRunner"))),
                               ignore="Example runner:FooRunner missing-in=distributed | why\n")
        self.assert_problem(result, "Ambiguous runner name")

    def test_stale_entry_and_comments(self):
        result = self.validate(ignore="# explanation\n\nExample runner:FooRunner missing-in=distributed | why\n")
        self.assertEqual(1, result.stale)
        self.assertEqual(self.ignore, result.problems[0].path)
        self.assertEqual(3, result.problems[0].line)

    def test_ignore_is_block_scoped_and_directional(self):
        for text in ["Other runner:FooRunner missing-in=distributed | why",
                     "Example runner:FooRunner missing-in=all-in-one | why"]:
            with self.subTest(text=text):
                result = self.validate(d=suite(block(distributed=True, runners=())), ignore=text)
                self.assertEqual(1, result.undeclared)
                self.assertEqual(1, result.stale)

    def test_duplicate_blocks_and_parameters(self):
        self.assert_problem(self.validate(a=suite(block() + block())), "duplicate mapped block")
        self.assert_problem(self.validate(a=suite().replace('<classes>',
                            '<parameter name="blockLabel" value="duplicate"/><classes>')), "Duplicate local parameter")

    def test_malformed_and_missing_files(self):
        self.assert_problem(self.validate(a="<suite>"), "Cannot read suite XML")
        self.a.unlink()
        result = alignment.validate(self.a, self.d, self.ignore)
        self.assert_problem(result, "Cannot read suite XML")

    def test_annotation_path_line_and_fix(self):
        result = self.validate(d=suite(block(distributed=True, runners=())))
        output = StringIO()
        with patch.object(alignment, "REPO_ROOT", self.root), redirect_stdout(output):
            alignment.emit(result)
        self.assertIn("::error file=testng-v2.xml,line=5::Example runner 'pkg.FooRunner'", output.getvalue())
        self.assertIn("Fix: Add it to the distributed block, or declare it in docs/devs/suite-alignment.ignore with a reason.",
                      output.getvalue())

    def test_ignore_annotation_and_annotation_escaping(self):
        result = self.validate(ignore="Example runner:FooRunner missing-in=distributed |\n")
        output = StringIO()
        with patch.object(alignment, "REPO_ROOT", self.root), redirect_stdout(output):
            alignment.emit(result)
        self.assertIn("::error file=suite-alignment.ignore,line=1::Ignore entry requires a nonblank reason.",
                      output.getvalue())
        result = alignment.Result(problems=[alignment.Problem(self.root / "a,b.xml", 7, "bad%\nvalue", "Fix it.")])
        with patch.object(alignment, "REPO_ROOT", self.root), redirect_stdout(output):
            alignment.emit(result)
        self.assertIn("::error file=a%2Cb.xml,line=7::bad%25%0Avalue", output.getvalue())

    def test_main_exit_status_and_read_only(self):
        self.validate()
        before = {p.name: p.read_bytes() for p in self.root.iterdir()}
        args = ["--all-in-one", str(self.a), "--distributed", str(self.d), "--ignore", str(self.ignore)]
        with redirect_stdout(StringIO()) as output:
            self.assertEqual(0, alignment.main(args))
        self.assertEqual(1, len(output.getvalue().splitlines()))
        self.assertEqual(before, {p.name: p.read_bytes() for p in self.root.iterdir()})
        self.ignore.write_text("Example runner:FooRunner missing-in=distributed | why\n")
        with redirect_stdout(StringIO()):
            self.assertEqual(1, alignment.main(args))


if __name__ == "__main__":
    unittest.main()
