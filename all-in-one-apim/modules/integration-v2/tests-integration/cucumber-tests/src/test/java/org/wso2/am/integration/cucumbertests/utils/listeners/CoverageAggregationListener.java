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
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.xml.XmlTest;
import org.wso2.am.integration.cucumbertests.utils.CoverageSupport;
import org.wso2.am.integration.cucumbertests.utils.ModulePathResolver;
import org.wso2.am.testcontainers.JacocoCoverage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * Suite-level coverage aggregation (opt-in; {@code -Dapim.coverage=true}). Runs once when the whole suite
 * finishes: merges every per-block {@code .exec} that {@code BlockLifecycleListener} dumped, extracts the APIM
 * class files from the distribution zip, and renders {@code jacoco-it.xml} (for Codecov) + an HTML tree.
 *
 * <p>No-op when coverage is off, so it can stay registered in any suite without affecting normal runs.
 * All failures are logged and swallowed — coverage reporting must never turn a green suite red.
 *
 * <p>It also tallies the dumps against the suite's blocks and writes {@code coverage-summary.properties}: a block
 * whose final {@code <label>.exec} is missing contributed nothing, which a valid-looking report would otherwise
 * hide. CI fails the run on a mismatch; locally it is a WARN.
 */
public class CoverageAggregationListener implements ISuiteListener {

    private static final Log logger = LogFactory.getLog(CoverageAggregationListener.class);

    @Override
    public void onStart(ISuite suite) {
        if (!CoverageSupport.enabled()) {
            return;
        }
        // Clean slate: purge any .exec files left by a previous run, so a rerun without `mvn clean` does not
        // merge stale dumps into this run's report (which would inflate the numbers / block count). Mirrors the
        // clean-slate reset extractApimgtClassfiles does for the classfiles dir. Best-effort — never fail a run.
        try {
            File execDir = CoverageSupport.execDir(ModulePathResolver.getModuleDir(CoverageAggregationListener.class));
            JacocoCoverage.deleteRecursively(execDir);
            CoverageSupport.resetFailedRestartDumps();
            logger.info("Coverage enabled: cleared stale exec dir before suite: " + execDir);
        } catch (Exception e) {
            logger.warn("Could not clear coverage exec dir at suite start: " + e.getMessage());
        }
    }

    @Override
    public void onFinish(ISuite suite) {
        if (!CoverageSupport.enabled()) {
            return;
        }
        try {
            String moduleDir = ModulePathResolver.getModuleDir(CoverageAggregationListener.class);

            File execDir = CoverageSupport.execDir(moduleDir);
            File[] execs = execDir.listFiles((d, name) -> name.endsWith(".exec"));
            List<File> execFiles = execs == null ? new ArrayList<>() : new ArrayList<>(List.of(execs));
            // Written for every coverage run, including one with no dumps at all, so the tally lists each missing
            // block instead of the run leaving no summary (or a previous run's) behind.
            try {
                writeDumpSummary(suite, execFiles, CoverageSupport.outputSummary(moduleDir));
            } catch (Exception e) {
                logger.warn("Could not write the coverage dump summary: " + e.getMessage());
            }
            if (execFiles.isEmpty()) {
                logger.warn("Coverage enabled but no .exec files found in " + execDir + " — nothing to report");
                return;
            }
            logger.info("Aggregating coverage from " + execFiles.size() + " exec file(s): " + execDir);

            File classfiles = CoverageSupport.classfilesDir(moduleDir);
            List<File> classfileRoots;
            File classfilesSrc = CoverageSupport.classfilesSourceDir();
            if (classfilesSrc != null) {
                // CI path: class files were docker-cp'd out of the already-shared image (no 538 MB dist-zip
                // artifact — see docs/devs/v2-coverage-architecture.md §8). The image is what ran, so its classes
                // are byte-identical to the executed ones.
                if (!classfilesSrc.isDirectory()) {
                    logger.warn("Coverage classfiles source (" + classfilesSrc + ") is not a directory; "
                            + "cannot render coverage report");
                    return;
                }
                classfileRoots = JacocoCoverage.extractApimgtClassfilesFromDir(classfilesSrc, classfiles);
            } else {
                // Local path: mine the built distribution zip.
                File distZip = CoverageSupport.distributionZip(moduleDir);
                if (!distZip.exists()) {
                    logger.warn("Distribution zip not found (" + distZip + "); cannot render coverage report");
                    return;
                }
                classfileRoots = JacocoCoverage.extractApimgtClassfiles(distZip, classfiles);
            }

            List<File> sourceRoots = new ArrayList<>();
            String src = CoverageSupport.sourcesRoot();
            if (src != null) {
                sourceRoots = JacocoCoverage.discoverSourceRoots(new File(src));
            }

            double linePct = JacocoCoverage.report(execFiles, classfileRoots, sourceRoots,
                    CoverageSupport.outputXml(moduleDir), CoverageSupport.outputHtml(moduleDir),
                    "apim-integration");
            logger.info("Integration coverage report generated: " + String.format("%.2f", linePct)
                    + "% line coverage across " + execFiles.size() + " exec file(s) -> "
                    + CoverageSupport.outputXml(moduleDir));
        } catch (Exception e) {
            logger.warn("Coverage aggregation failed (suite result unaffected): " + e.getMessage(), e);
        }
    }

    /**
     * Compares the block labels the suite declares with the block-end dumps present, logs the tally, and writes it
     * to {@code summaryFile}. Pre-restart dumps are counted separately; they never stand in for a block-end dump.
     */
    private static void writeDumpSummary(ISuite suite, List<File> execFiles, File summaryFile) throws IOException {
        Set<String> expected = new TreeSet<>();
        for (XmlTest test : suite.getXmlSuite().getTests()) {
            String label = test.getParameter("blockLabel");
            if (label != null && !label.isBlank()) {
                expected.add(label);
            }
        }
        Set<String> dumped = new TreeSet<>();
        int restartDumps = 0;
        for (File exec : execFiles) {
            String name = exec.getName();
            if (name.contains(CoverageSupport.RESTART_EXEC_INFIX)) {
                restartDumps++;
            } else {
                dumped.add(name.substring(0, name.length() - ".exec".length()));
            }
        }
        Set<String> missing = new TreeSet<>(expected);
        missing.removeAll(dumped);

        logger.info("Coverage dumps: blocks expected=" + expected.size() + ", block-end dumps=" + dumped.size()
                + ", pre-restart dumps=" + restartDumps);
        if (!missing.isEmpty()) {
            logger.warn("Coverage is missing the block-end dump of " + missing.size() + " block(s) — their "
                    + "counters are absent from the report: " + missing);
        }
        Set<String> failedRestartDumps = CoverageSupport.failedRestartDumps();
        if (!failedRestartDumps.isEmpty()) {
            logger.warn("Coverage is missing " + failedRestartDumps.size() + " pre-restart dump(s) — the counters of "
                    + "those restarted JVMs are absent from the report: " + failedRestartDumps);
        }

        Properties summary = new Properties();
        summary.setProperty("blocks.expected", String.valueOf(expected.size()));
        summary.setProperty("blocks.dumped", String.valueOf(dumped.size()));
        summary.setProperty("blocks.missing", String.join(",", missing));
        summary.setProperty("restart.dumps", String.valueOf(restartDumps));
        summary.setProperty("restart.dumps.failed", String.valueOf(failedRestartDumps.size()));
        summary.setProperty("restart.dumps.failedList", String.join(",", failedRestartDumps));
        summaryFile.getParentFile().mkdirs();
        try (OutputStream os = new FileOutputStream(summaryFile)) {
            summary.store(os, "Integration coverage dump tally");
        }
    }
}
