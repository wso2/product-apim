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

package org.wso2.am.integration.cucumbertests.utils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Central gate + path layout for the (opt-in) integration-coverage collection.
 *
 * <p>Coverage is off unless {@code -Dapim.coverage=true}. When on, {@code BlockLifecycleListener} enables the
 * JaCoCo agent on each block's container and dumps that block's counters to {@link #execFile} before stopping
 * it; the suite-level {@code CoverageAggregationListener} then merges all per-block {@code .exec} files and
 * renders {@code jacoco-it.xml} + HTML (see docs/devs/v2-coverage-architecture.md). All-in-one lane only.
 */
public final class CoverageSupport {

    /** {@code -Dapim.coverage=true} turns collection on (default off). Forwarded to the fork via surefire. */
    public static final String ENABLED_PROPERTY = "apim.coverage";
    /** Optional {@code -Dapim.coverage.sources=<path>} — a source tree (e.g. carbon-apimgt) for HTML line detail. */
    public static final String SOURCES_PROPERTY = "apim.coverage.sources";

    private CoverageSupport() {
    }

    public static boolean enabled() {
        return Boolean.parseBoolean(System.getProperty(ENABLED_PROPERTY));
    }

    /** Optional source tree for report line-highlighting; {@code null} if not configured. */
    public static String sourcesRoot() {
        String v = System.getProperty(SOURCES_PROPERTY);
        return (v == null || v.isBlank()) ? null : v;
    }

    private static Path root(String moduleDir) {
        return Paths.get(moduleDir, "target", "coverage");
    }

    /** Per-block {@code .exec} dumps live here (one file per block label). */
    public static File execDir(String moduleDir) {
        return root(moduleDir).resolve("exec").toFile();
    }

    /** The {@code .exec} destination for a given block label. */
    public static File execFile(String moduleDir, String blockLabel) {
        return new File(execDir(moduleDir), blockLabel + ".exec");
    }

    /** Where APIM class files extracted from the distribution zip are staged for the report. */
    public static File classfilesDir(String moduleDir) {
        return root(moduleDir).resolve("classfiles").toFile();
    }

    public static File outputXml(String moduleDir) {
        return root(moduleDir).resolve(Paths.get("output", "txt", "jacoco-it.xml").toString()).toFile();
    }

    public static File outputHtml(String moduleDir) {
        return root(moduleDir).resolve(Paths.get("output", "html").toString()).toFile();
    }

    /** The built all-in-one distribution zip (source of the report's class files). */
    public static File distributionZip(String moduleDir) {
        String serverName = System.getProperty("apim.server.name"); // e.g. wso2am-4.7.0-SNAPSHOT
        return Paths.get(moduleDir, "..", "..", "..", "distribution", "product", "target",
                serverName + ".zip").normalize().toFile();
    }
}
