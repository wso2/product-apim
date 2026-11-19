/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.integration.cucumbertests.utils.listeners;

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.SkipException;
import org.testng.TestNG;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BlockTimelineListenerTest {

    @Test
    public void capturesTestNgResultsAndBootFailures() throws Exception {
        Path directory = Files.createTempDirectory("block-timeline-test");
        Path output = directory.resolve("timeline.json");
        try {
            Files.writeString(output, "stale");
            XmlSuite suite = new XmlSuite();
            suite.setName("timeline fixture");
            suite.setParallel(XmlSuite.ParallelMode.TESTS);
            suite.setThreadCount(2);
            block(suite, "pass", PassingRunner.class);
            block(suite, "fail", FailingRunner.class);
            block(suite, "skip", SkippingRunner.class);
            block(suite, "boot", BootRunner.class);
            TestNG testng = new TestNG();
            testng.setUseDefaultListeners(false);
            testng.setXmlSuites(List.of(suite));
            testng.addListener(new EnableFixtures());
            testng.addListener(new BootFailure());
            testng.addListener(new BlockTimelineListener(output));
            testng.run();

            JSONObject report = new JSONObject(Files.readString(output));
            Assert.assertEquals(report.getInt("schemaVersion"), 1);
            JSONArray blocks = report.getJSONArray("blocks");
            Assert.assertEquals(blocks.length(), 4);
            Map<String, JSONObject> byName = IntStream.range(0, blocks.length()).mapToObj(blocks::getJSONObject)
                    .collect(Collectors.toMap(b -> b.getString("name"), b -> b));
            Assert.assertEquals(runner(byName.get("pass")).getInt("passed"), 1);
            Assert.assertEquals(runner(byName.get("fail")).getInt("failed"), 1);
            Assert.assertEquals(runner(byName.get("skip")).getInt("skipped"), 1);
            Assert.assertTrue(byName.get("boot").getBoolean("bootFailed"));
            Assert.assertEquals(runner(byName.get("boot")).getInt("configurationFailed"), 1);
            Assert.assertEquals(runner(byName.get("boot")).getInt("skipped"), 1);
            Assert.assertEquals(report.getJSONObject("totals").getInt("failed"), 1);
            Assert.assertEquals(report.getJSONObject("totals").getInt("skipped"), 2);
            Assert.assertEquals(report.getJSONObject("totals").getInt("configurationFailed"), 1);
            for (JSONObject b : byName.values()) {
                Assert.assertTrue(b.getString("thread").startsWith("TestNG-tests-"), b.toString());
                Assert.assertTrue(b.getLong("startTimeMs") <= b.getLong("endTimeMs"));
                Assert.assertEquals(b.getLong("durationMs"), b.getLong("endTimeMs") - b.getLong("startTimeMs"));
                Assert.assertTrue(b.getLong("endTimeMs") <= report.getLong("generatedAtMs"));
                Assert.assertEquals(b.getString("suite"), suite.getName());
            }
        } finally {
            Files.deleteIfExists(output);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void selectedBlocksKeepTheirOwnRunnerCounts() throws Exception {
        Path directory = Files.createTempDirectory("block-timeline-selection");
        Path output = directory.resolve("timeline.json");
        try {
            XmlSuite suite = new XmlSuite();
            suite.setName("selected timeline");
            suite.setParallel(XmlSuite.ParallelMode.TESTS);
            suite.setThreadCount(2);
            block(suite, "first", PassingRunner.class);
            block(suite, "second", PassingRunner.class);
            block(suite, "omitted", PassingRunner.class);
            suite.getTests().get(0).addParameter("group", "selected");
            suite.getTests().get(1).addParameter("group", "selected");
            suite.getTests().get(2).addParameter("group", "other");
            SuiteGroupSelectionListener.select(List.of(suite), "selected");
            TestNG testng = new TestNG();
            testng.setUseDefaultListeners(false);
            testng.setXmlSuites(List.of(suite));
            testng.addListener(new EnableFixtures());
            testng.addListener(new BlockTimelineListener(output));
            testng.run();
            JSONObject report = new JSONObject(Files.readString(output));
            Assert.assertEquals(report.getJSONArray("blocks").length(), 2);
            Assert.assertEquals(report.getJSONObject("totals").getInt("passed"), 2);
            for (Object value : report.getJSONArray("blocks")) {
                JSONObject b = (JSONObject) value;
                Assert.assertNotEquals(b.getString("name"), "omitted");
                Assert.assertEquals(runner(b).getInt("passed"), 1);
                Assert.assertEquals(runner(b).getString("className"), PassingRunner.class.getName());
            }
        } finally {
            Files.deleteIfExists(output);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void unavailableOutputDoesNotFailTests() throws Exception {
        Path directory = Files.createTempDirectory("block-timeline-unavailable");
        Path child = directory.resolve("keep");
        try {
            Files.writeString(child, "non-empty directory");
            BlockTimelineListener listener = new BlockTimelineListener(directory);
            listener.onExecutionStart();
            listener.onExecutionFinish();
            Assert.assertTrue(Files.exists(child));
        } finally {
            Files.deleteIfExists(child);
            Files.deleteIfExists(directory);
        }
    }

    private static JSONObject runner(JSONObject block) {
        return block.getJSONArray("runners").getJSONObject(0);
    }

    private static void block(XmlSuite suite, String name, Class<?> runner) {
        XmlTest block = new XmlTest(suite);
        block.setName(name);
        block.setXmlClasses(List.of(new XmlClass(runner)));
    }

    // Fixture methods are enabled only inside the nested TestNG runs.
    public static class EnableFixtures implements org.testng.IAnnotationTransformer {
        @Override
        public void transform(org.testng.annotations.ITestAnnotation annotation, Class testClass,
                              java.lang.reflect.Constructor constructor, java.lang.reflect.Method method) {
            annotation.setEnabled(true);
        }
    }

    public static class PassingRunner {
        @Test(enabled = false) public void pass() { }
    }

    public static class FailingRunner {
        @Test(enabled = false) public void fail() { Assert.fail("fixture failure"); }
    }

    public static class SkippingRunner {
        @Test(enabled = false) public void skip() { throw new SkipException("fixture skip"); }
    }

    public static class BootRunner {
        @BeforeClass public void abort(ITestContext context) {
            if (context.getAttribute("bootError") != null) {
                throw new IllegalStateException("fixture block bootstrap failed");
            }
        }
        @Test(enabled = false) public void scenario() { Assert.fail("must not run after bootstrap failure"); }
    }

    public static class BootFailure implements ITestListener {
        @Override public void onStart(ITestContext context) {
            if (context.getName().equals("boot")) {
                context.setAttribute("bootError", new IllegalStateException("fixture startup failure"));
            }
        }
    }
}
