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
import org.wso2.am.integration.cucumbertests.utils.CoverageSupport;
import org.wso2.am.integration.cucumbertests.utils.ModulePathResolver;
import org.wso2.am.testcontainers.JacocoCoverage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Suite-level coverage aggregation (opt-in; {@code -Dapim.coverage=true}). Runs once when the whole suite
 * finishes: merges every per-block {@code .exec} that {@code BlockLifecycleListener} dumped, extracts the APIM
 * class files from the distribution zip, and renders {@code jacoco-it.xml} (for Codecov) + an HTML tree.
 *
 * <p>No-op when coverage is off, so it can stay registered in any suite without affecting normal runs.
 * All failures are logged and swallowed — coverage reporting must never turn a green suite red.
 */
public class CoverageAggregationListener implements ISuiteListener {

    private static final Log logger = LogFactory.getLog(CoverageAggregationListener.class);

    @Override
    public void onStart(ISuite suite) {
        // no-op
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
            if (execs == null || execs.length == 0) {
                logger.warn("Coverage enabled but no .exec files found in " + execDir + " — nothing to report");
                return;
            }
            List<File> execFiles = new ArrayList<>(List.of(execs));
            logger.info("Aggregating coverage from " + execFiles.size() + " block exec file(s): " + execDir);

            File distZip = CoverageSupport.distributionZip(moduleDir);
            if (!distZip.exists()) {
                logger.warn("Distribution zip not found (" + distZip + "); cannot render coverage report");
                return;
            }
            File classfiles = CoverageSupport.classfilesDir(moduleDir);
            List<File> classfileRoots = JacocoCoverage.extractApimgtClassfiles(distZip, classfiles);

            List<File> sourceRoots = new ArrayList<>();
            String src = CoverageSupport.sourcesRoot();
            if (src != null) {
                sourceRoots = JacocoCoverage.discoverSourceRoots(new File(src));
            }

            double linePct = JacocoCoverage.report(execFiles, classfileRoots, sourceRoots,
                    CoverageSupport.outputXml(moduleDir), CoverageSupport.outputHtml(moduleDir),
                    "apim-integration");
            logger.info("Integration coverage report generated: " + String.format("%.2f", linePct)
                    + "% line coverage across " + execFiles.size() + " block(s) -> "
                    + CoverageSupport.outputXml(moduleDir));
        } catch (Exception e) {
            logger.warn("Coverage aggregation failed (suite result unaffected): " + e.getMessage(), e);
        }
    }
}
