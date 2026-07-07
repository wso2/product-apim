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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central gate + path layout for the (opt-in) integration-coverage collection.
 *
 * <p>Coverage is off unless {@code -Dapim.coverage=true}. When on, {@code BlockLifecycleListener} enables the
 * JaCoCo agent on each block's container and dumps that block's counters to {@link #execFile} before stopping
 * it; the suite-level {@code CoverageAggregationListener} then merges all per-block {@code .exec} files and
 * renders {@code jacoco-it.xml} + HTML (see docs/devs/v2-coverage-architecture.md). All-in-one lane only.
 */
public final class CoverageSupport {

    private static final Log logger = LogFactory.getLog(CoverageSupport.class);

    /** {@code -Dapim.coverage=true} turns collection on (default off). Forwarded to the fork via surefire. */
    public static final String ENABLED_PROPERTY = "apim.coverage";
    /** Optional {@code -Dapim.coverage.sources=<path>} — a source tree (e.g. carbon-apimgt) for HTML line detail. */
    public static final String SOURCES_PROPERTY = "apim.coverage.sources";
    /**
     * Optional {@code -Dapim.coverage.classfiles=<dir>} — a directory tree holding the APIM plugin jars + webapp
     * WARs (a {@code docker cp} of {@code repository/components/plugins} + {@code .../webapps} out of the shared
     * image). When set, the report mines class files from here instead of the distribution zip — CI avoids
     * shipping the ~538 MB zip as a second artifact (see docs/devs/v2-coverage-architecture.md §8). Unset locally
     * ⇒ the zip path ({@link #distributionZip}) is used.
     */
    public static final String CLASSFILES_PROPERTY = "apim.coverage.classfiles";

    /** Warn at most once if the property is present-but-not-truthy (enabled() is polled per block + at suite ends). */
    private static final AtomicBoolean WARNED_NOT_TRUTHY = new AtomicBoolean(false);

    private CoverageSupport() {
    }

    public static boolean enabled() {
        String v = System.getProperty(ENABLED_PROPERTY);
        boolean on = Boolean.parseBoolean(v);
        // A bare `-Dapim.coverage` (no `=true`) resolves to "" → false, so coverage silently stays OFF with no hint.
        // Surface that once: the property was clearly meant to enable coverage, but only `=true` is truthy here.
        if (!on && v != null && WARNED_NOT_TRUTHY.compareAndSet(false, true)) {
            logger.warn("-D" + ENABLED_PROPERTY + " is set to \"" + v + "\", which is not truthy — integration "
                    + "coverage stays OFF. Pass -D" + ENABLED_PROPERTY + "=true to enable it.");
        }
        return on;
    }

    /** Optional source tree for report line-highlighting; {@code null} if not configured. */
    public static String sourcesRoot() {
        String v = System.getProperty(SOURCES_PROPERTY);
        return (v == null || v.isBlank()) ? null : v;
    }

    /** The docker-cp'd classfiles source dir if {@code -Dapim.coverage.classfiles} is set; {@code null} otherwise
     *  (⇒ fall back to the distribution zip). See {@link #CLASSFILES_PROPERTY}. */
    public static File classfilesSourceDir() {
        String v = System.getProperty(CLASSFILES_PROPERTY);
        return (v == null || v.isBlank()) ? null : new File(v);
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
        if (serverName == null || serverName.isBlank()) {
            // Fail fast with the real cause instead of resolving `…/target/null.zip`, which later surfaces as a
            // bizarre "Distribution zip not found (…/null.zip)" that hides the unset property.
            throw new IllegalStateException("apim.server.name is not set — required to locate the distribution zip "
                    + "for coverage reporting (normally passed via the surefire systemPropertyVariables; pass "
                    + "-Dapim.server.name=<name> if running the suite outside the Maven config).");
        }
        return Paths.get(moduleDir, "..", "..", "..", "distribution", "product", "target",
                serverName + ".zip").normalize().toFile();
    }
}
