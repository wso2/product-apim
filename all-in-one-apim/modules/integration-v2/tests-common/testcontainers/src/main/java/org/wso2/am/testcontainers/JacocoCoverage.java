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

package org.wso2.am.testcontainers;

import org.jacoco.agent.AgentJar;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.tools.ExecDumpClient;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.ISourceFileLocator;
import org.jacoco.report.MultiSourceFileLocator;
import org.jacoco.report.html.HTMLFormatter;
import org.jacoco.report.xml.XMLFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Integration-test coverage helper for the all-in-one v2 lane (JaCoCo binary/integration coverage).
 *
 * <p>This is the Java analogue of api-platform's {@code common/testutils/coverage} (which uses Go's
 * {@code covdata}). The APIM server runs inside a container with the JaCoCo agent attached in
 * {@code output=tcpserver} mode (see {@link DynamicApimContainer#withCoverage()}); at teardown this class
 * (1) pulls the in-JVM counters over the mapped port with {@link ExecDumpClient} into a {@code .exec} file,
 * (2) optionally merges several {@code .exec} files, and (3) renders a scoped HTML/XML report against the
 * product class files (extracted from the built distribution zip) plus best-effort sources.
 *
 * <p>Scoping is two-layered (see docs/devs/v2-coverage-architecture.md): the agent instruments only
 * {@code org.wso2.carbon.apimgt.*} (perf/noise), and {@link #extractApimgtClassfiles} restricts the report
 * denominator to the APIM OSGi bundles. All methods are static; there is no per-instance state.
 */
public final class JacocoCoverage {

    private static final Logger logger = LoggerFactory.getLogger(JacocoCoverage.class);

    /** TCP port the in-container agent listens on (canonical; Docker maps it to an ephemeral host port). */
    public static final int TCP_PORT = 6300;
    /** Where the agent jar is copied inside the container. */
    public static final String CONTAINER_AGENT_PATH = "/opt/cov/jacocoagent.jar";

    /** Default scoping — instrument APIM code only, skip generated/stub/test classes. */
    public static final String DEFAULT_INCLUDES = "org.wso2.carbon.apimgt.*";
    public static final String DEFAULT_EXCLUDES = "*.stub.*:*.dto.*:*.gen.*:*Test";

    private JacocoCoverage() {
    }

    /**
     * The {@code -javaagent} VM argument to attach the agent inside the container, in tcpserver mode.
     * {@code address=*} is required so the dump client can reach it over the mapped port (the classic
     * "connection refused" cause is binding to loopback only).
     */
    public static String containerAgentVmArg() {
        return "-javaagent:" + CONTAINER_AGENT_PATH + "=output=tcpserver,address=*,port=" + TCP_PORT
                + ",includes=" + DEFAULT_INCLUDES
                + ",excludes=" + DEFAULT_EXCLUDES;
    }

    /** Extracts the bundled JaCoCo runtime agent jar to a temp file (to be copied into the container). */
    public static File extractAgentJar() throws IOException {
        File agent = AgentJar.extractToTempLocation();
        agent.deleteOnExit();
        return agent;
    }

    /**
     * Connects to the agent's tcpserver over {@code host:port}, dumps the current execution data, and writes
     * it to {@code destExec}. Must be called while the container is still running (before {@code stop()}).
     */
    public static void dump(String host, int port, File destExec) throws IOException {
        logger.info("Dumping JaCoCo coverage from {}:{} -> {}", host, port, destExec);
        ExecDumpClient client = new ExecDumpClient();
        client.setDump(true);     // request a dump
        client.setReset(false);   // keep counters (do not reset the server)
        client.setRetryCount(20); // the agent may still be binding right after boot
        ExecFileLoader loader = client.dump(host, port);
        destExec.getParentFile().mkdirs();
        loader.save(destExec, false);
        logger.info("Wrote coverage exec ({} bytes)", destExec.length());
    }

    /**
     * Extracts the APIM class files from the built distribution zip into {@code destDir}, covering BOTH:
     * <ul>
     *   <li>OSGi bundles: {@code repository/components/plugins/org.wso2.carbon.apimgt.*.jar} (core/impl/gateway/keymgt), and</li>
     *   <li>webapp code: inside each {@code repository/deployment/server/webapps/*.war} (a nested zip), the
     *       {@code WEB-INF/classes/org/wso2/carbon/apimgt/**.class} (the publisher/devportal/admin REST-API impl
     *       classes live here) and any {@code WEB-INF/lib/*apimgt*.jar}.</li>
     * </ul>
     * These are byte-identical to what the running image loaded (same zip), so JaCoCo class-id matching lines up.
     * Webapp {@code .class} files are deduped first-wins and webapp jars deduped by basename (the same shared
     * bundle can appear in several wars).
     *
     * @return the classfile roots to analyze (only non-empty ones), for {@link #report}.
     */
    public static List<File> extractApimgtClassfiles(File distributionZip, File destDir) throws IOException {
        // Start clean so a stale extraction from a previous (possibly different) distribution can't leak in.
        deleteRecursively(destDir);
        File pluginsDir = new File(destDir, "plugins");
        File webappClasses = new File(destDir, "webapp-classes");
        File webappLib = new File(destDir, "webapp-lib");
        for (File d : new File[]{pluginsDir, webappClasses, webappLib}) {
            Files.createDirectories(d.toPath());
        }
        int plugins = 0, warClasses = 0, warJars = 0;
        java.util.Set<String> seenJar = new java.util.HashSet<>();

        try (ZipFile zip = new ZipFile(distributionZip)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String name = e.getName();
                if (e.isDirectory()) {
                    continue;
                }
                // 1) OSGi plugin bundles.
                if (name.contains("/repository/components/plugins/org.wso2.carbon.apimgt.") && name.endsWith(".jar")) {
                    try (InputStream in = zip.getInputStream(e)) {
                        Files.copy(in, new File(pluginsDir, baseName(name)).toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                    plugins++;
                    continue;
                }
                // 2) Webapp WARs (nested zip): pull apimgt WEB-INF/classes and WEB-INF/lib apimgt jars.
                if (name.contains("/repository/deployment/server/webapps/") && name.endsWith(".war")) {
                    try (java.util.zip.ZipInputStream war =
                                 new java.util.zip.ZipInputStream(zip.getInputStream(e))) {
                        ZipEntry we;
                        while ((we = war.getNextEntry()) != null) {
                            String wn = we.getName();
                            if (we.isDirectory()) {
                                continue;
                            }
                            if (wn.startsWith("WEB-INF/classes/org/wso2/carbon/apimgt/") && wn.endsWith(".class")) {
                                File out = new File(webappClasses, wn.substring("WEB-INF/classes/".length()));
                                if (!out.exists()) { // first-wins dedupe across wars
                                    out.getParentFile().mkdirs();
                                    Files.copy(war, out.toPath());
                                    warClasses++;
                                }
                            } else if (wn.startsWith("WEB-INF/lib/") && wn.contains("apimgt") && wn.endsWith(".jar")) {
                                String b = baseName(wn);
                                if (seenJar.add(b)) {
                                    Files.copy(war, new File(webappLib, b).toPath());
                                    warJars++;
                                }
                            }
                        }
                    }
                }
            }
        }
        logger.info("Extracted APIM classfiles from {}: plugins={} jars, webapp .class={}, webapp libs={} jars",
                distributionZip.getName(), plugins, warClasses, warJars);
        if (plugins == 0 && warClasses == 0 && warJars == 0) {
            throw new IOException("No org.wso2.carbon.apimgt.* class files found in " + distributionZip);
        }
        List<File> roots = new ArrayList<>();
        for (File d : new File[]{pluginsDir, webappClasses, webappLib}) {
            String[] kids = d.list();
            if (kids != null && kids.length > 0) {
                roots.add(d);
            }
        }
        return roots;
    }

    private static String baseName(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static void deleteRecursively(File dir) throws IOException {
        if (dir == null || !dir.exists()) {
            return;
        }
        try (java.util.stream.Stream<Path> walk = Files.walk(dir.toPath())) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best-effort
                }
            });
        }
    }

    /**
     * Best-effort source discovery: all {@code src/main/java} directories under {@code sourceTreeRoot}
     * (e.g. a carbon-apimgt checkout). Sources only affect line highlighting in the HTML report; the
     * coverage numbers come from the class files.
     */
    public static List<File> discoverSourceRoots(File sourceTreeRoot) {
        List<File> roots = new ArrayList<>();
        if (sourceTreeRoot == null || !sourceTreeRoot.isDirectory()) {
            return roots;
        }
        try (java.util.stream.Stream<Path> stream = Files.walk(sourceTreeRoot.toPath())) {
            stream.filter(Files::isDirectory)
                    .filter(p -> p.endsWith(Path.of("src", "main", "java")))
                    .forEach(p -> roots.add(p.toFile()));
        } catch (IOException e) {
            logger.warn("Source discovery failed under {}: {}", sourceTreeRoot, e.getMessage());
        }
        logger.info("Discovered {} src/main/java source roots under {}", roots.size(), sourceTreeRoot);
        return roots;
    }

    /**
     * Renders a JaCoCo report from one or more {@code .exec} files against {@code classfiles} (jar/dir) and
     * best-effort {@code sourceRoots}. Writes {@code xmlOut} (for Codecov) and an HTML tree at {@code htmlDir}.
     *
     * @return total <b>line</b> coverage percentage over the analyzed (APIM) classes.
     */
    public static double report(List<File> execFiles, List<File> classfiles, List<File> sourceRoots,
                                File xmlOut, File htmlDir, String title) throws IOException {
        ExecFileLoader loader = new ExecFileLoader();
        for (File exec : execFiles) {
            loader.load(exec);
        }

        CoverageBuilder builder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(loader.getExecutionDataStore(), builder);
        for (File cf : classfiles) {
            // Per-root so a hiccup in one root (e.g. a duplicate class) can't sink the whole report.
            try {
                analyzer.analyzeAll(cf); // handles a jar, a .class, or a directory (recursively)
            } catch (Exception ex) {
                logger.warn("Skipping classfile root {} during analysis: {}", cf, ex.getMessage());
            }
        }
        IBundleCoverage bundle = builder.getBundle(title);

        ISourceFileLocator locator = buildSourceLocator(sourceRoots);

        // XML (single file) — this is what Codecov ingests.
        xmlOut.getParentFile().mkdirs();
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(xmlOut))) {
            XMLFormatter xml = new XMLFormatter();
            IReportVisitor v = xml.createVisitor(os);
            v.visitInfo(loader.getSessionInfoStore().getInfos(), loader.getExecutionDataStore().getContents());
            v.visitBundle(bundle, locator);
            v.visitEnd();
        }

        // HTML (directory tree) — for local eyeballing.
        Files.createDirectories(htmlDir.toPath());
        HTMLFormatter html = new HTMLFormatter();
        IReportVisitor hv = html.createVisitor(new FileMultiReportOutput(htmlDir));
        hv.visitInfo(loader.getSessionInfoStore().getInfos(), loader.getExecutionDataStore().getContents());
        hv.visitBundle(bundle, locator);
        hv.visitEnd();

        ICounter line = bundle.getLineCounter();
        ICounter instr = bundle.getInstructionCounter();
        double linePct = line.getTotalCount() == 0 ? 0.0 : line.getCoveredRatio() * 100.0;
        logger.info("Coverage bundle '{}': classes={}, methods={}, lines {}/{} ({}%), instructions {}/{}",
                title, bundle.getClassCounter().getTotalCount(), bundle.getMethodCounter().getTotalCount(),
                line.getCoveredCount(), line.getTotalCount(), String.format("%.1f", linePct),
                instr.getCoveredCount(), instr.getTotalCount());
        logger.info("Report: xml={} html={}/index.html", xmlOut, htmlDir);
        return linePct;
    }

    private static ISourceFileLocator buildSourceLocator(List<File> sourceRoots) {
        MultiSourceFileLocator multi = new MultiSourceFileLocator(4);
        if (sourceRoots != null) {
            for (File root : sourceRoots) {
                multi.add(new DirectorySourceFileLocator(root, "utf-8", 4));
            }
        }
        return multi;
    }
}
