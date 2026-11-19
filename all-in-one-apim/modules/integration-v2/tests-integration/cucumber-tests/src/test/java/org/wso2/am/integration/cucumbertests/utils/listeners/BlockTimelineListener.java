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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.IExecutionListener;
import org.testng.IResultMap;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Captures executed blocks and TestNG results for the CI job summary. TestNG context intervals include bootstrap
 * and test execution; lifecycle teardown callbacks are outside that interval. Configuration failures
 * are separate from scenario totals and make the affected runner red, including failed block bootstrap.
 * All callbacks are best effort and cannot change the suite result.
 */
public class BlockTimelineListener implements ITestListener, IExecutionListener {

    private static final Log LOG = LogFactory.getLog(BlockTimelineListener.class);
    private final Path output;
    private final Map<ITestContext, String> threads = new ConcurrentHashMap<>();
    private final List<JSONObject> blocks = new ArrayList<>();

    public BlockTimelineListener() {
        this(Path.of("target", "block-timeline.json"));
    }

    BlockTimelineListener(Path output) {
        this.output = output;
    }

    @Override
    public void onExecutionStart() {
        safely(() -> {
            threads.clear();
            synchronized (blocks) {
                blocks.clear();
            }
            Files.deleteIfExists(output);
        });
    }

    @Override
    public void onStart(ITestContext context) {
        safely(() -> threads.put(context, Thread.currentThread().getName()));
    }

    @Override
    public void onFinish(ITestContext context) {
        safely(() -> {
            Map<String, JSONObject> runners = new LinkedHashMap<>();
            context.getCurrentXmlTest().getXmlClasses().forEach(c -> runners.putIfAbsent(c.getName(), runner(c.getName())));
            count(context.getPassedTests(), runners, "passed");
            count(context.getFailedTests(), runners, "failed");
            count(context.getFailedButWithinSuccessPercentageTests(), runners, "failed");
            count(context.getSkippedTests(), runners, "skipped");
            count(context.getFailedConfigurations(), runners, "configurationFailed");
            long start = context.getStartDate().getTime();
            long end = context.getEndDate().getTime();
            JSONObject block = new JSONObject()
                    .put("suite", context.getSuite().getName())
                    .put("name", context.getName())
                    .put("thread", threads.getOrDefault(context, Thread.currentThread().getName()))
                    .put("startTimeMs", start).put("endTimeMs", end)
                    .put("durationMs", Math.max(0, end - start))
                    .put("bootFailed", context.getAttribute("bootError") != null)
                    .put("runners", new JSONArray(runners.values()));
            synchronized (blocks) {
                blocks.add(block);
            }
            threads.remove(context);
        });
    }

    @Override
    public void onExecutionFinish() {
        safely(() -> {
            List<JSONObject> snapshot;
            synchronized (blocks) {
                snapshot = new ArrayList<>(blocks);
            }
            snapshot.sort(Comparator.comparingLong(b -> b.getLong("startTimeMs")));
            JSONObject totals = new JSONObject().put("passed", 0).put("failed", 0).put("skipped", 0)
                    .put("configurationFailed", 0);
            for (JSONObject block : snapshot) {
                for (Object value : block.getJSONArray("runners")) {
                    JSONObject runner = (JSONObject) value;
                    for (String key : totals.keySet()) {
                        totals.put(key, totals.getInt(key) + runner.getInt(key));
                    }
                }
            }
            JSONObject report = new JSONObject().put("schemaVersion", 1)
                    .put("generatedAtMs", System.currentTimeMillis())
                    .put("totals", totals).put("blocks", new JSONArray(snapshot));
            Files.createDirectories(output.toAbsolutePath().getParent());
            Files.writeString(output, report.toString(2) + System.lineSeparator(), StandardCharsets.UTF_8);
            LOG.info("Block timeline written to " + output.toAbsolutePath());
        });
    }

    private static JSONObject runner(String name) {
        return new JSONObject().put("className", name).put("passed", 0).put("failed", 0)
                .put("skipped", 0).put("configurationFailed", 0);
    }

    private static void count(IResultMap results, Map<String, JSONObject> runners, String key) {
        for (ITestResult result : results.getAllResults()) {
            String name = result.getTestClass().getRealClass().getName();
            JSONObject runner = runners.computeIfAbsent(name, BlockTimelineListener::runner);
            runner.put(key, runner.getInt(key) + 1);
        }
    }

    private static void safely(ReportAction action) {
        try {
            action.run();
        } catch (Throwable failure) {
            // Reporting is independent of the suite result, including unavailable output directories.
            try {
                LOG.warn("Block timeline unavailable", failure);
            } catch (Throwable ignored) {
                // Logging is best effort too.
            }
        }
    }

    @FunctionalInterface
    private interface ReportAction {
        void run() throws Exception;
    }
}
