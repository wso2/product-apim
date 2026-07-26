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
import java.util.regex.Pattern;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
    public static final String DEFAULT_EXCLUDES = "*.stub.*:*.dto.*:*.gen.*:*Test:*.thrift.*";

    /** Upper bound on a single dump, so an unresponsive agent can't stall test teardown forever. */
    private static final int DUMP_TIMEOUT_SECONDS = 60;

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

        // ExecDumpClient retries the CONNECT but has no read timeout, so a connected-but-silent agent would
        // block indefinitely and stall teardown (the caller can't help — dump() would never return to throw).
        // Run it on a daemon thread with a hard deadline; a leaked daemon thread can't hold up JVM exit.
        ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "jacoco-dump-" + host + ":" + port);
            t.setDaemon(true);
            return t;
        });
        try {
            Future<ExecFileLoader> future = exec.submit(() -> client.dump(host, port));
            ExecFileLoader loader = future.get(DUMP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            destExec.getParentFile().mkdirs();
            loader.save(destExec, false);
            logger.info("Wrote coverage exec ({} bytes)", destExec.length());
        } catch (TimeoutException e) {
            throw new IOException("JaCoCo dump from " + host + ":" + port + " timed out after "
                    + DUMP_TIMEOUT_SECONDS + "s (agent unresponsive); skipping this block's coverage", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;   // preserve the original dump/connection failure
            }
            throw new IOException("JaCoCo dump from " + host + ":" + port + " failed: " + cause, cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("JaCoCo dump from " + host + ":" + port + " was interrupted", e);
        } finally {
            exec.shutdownNow();
        }
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
        ClassfileExtraction ex = new ClassfileExtraction(destDir);
        try (ZipFile zip = new ZipFile(distributionZip)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String name = e.getName();
                if (e.isDirectory()) {
                    continue;
                }
                // 1) OSGi plugin bundles — matched by their full path in the distribution layout.
                if (name.contains("/repository/components/plugins/org.wso2.carbon.apimgt.") && name.endsWith(".jar")) {
                    try (InputStream in = zip.getInputStream(e)) {
                        ex.addPlugin(baseName(name), in);
                    }
                // 2) Webapp WARs (nested zip): pull apimgt WEB-INF/classes and WEB-INF/lib apimgt jars.
                } else if (name.contains("/repository/deployment/server/webapps/") && name.endsWith(".war")) {
                    try (InputStream in = zip.getInputStream(e)) {
                        ex.mineWar(in);
                    }
                }
            }
        }
        return ex.finish(distributionZip.getName());
    }

    /**
     * Directory-source analogue of {@link #extractApimgtClassfiles(File, File)}, for CI where the class files are
     * pulled out of the already-shared Docker image (a {@code docker cp} of {@code repository/components/plugins}
     * and {@code repository/deployment/server/webapps}) instead of shipping the whole ~538 MB distribution zip as
     * a second artifact. The image is what actually ran, so its classes are byte-identical to the executed ones —
     * exact JaCoCo class-id match (see docs/devs/v2-coverage-architecture.md §8).
     *
     * <p>{@code serverDir} is any directory tree holding the apimgt plugin bundle jars and the webapp WARs;
     * matching is by file NAME (the cp'd tree is already scoped to plugins + webapps, so the path-substring rule
     * the zip path uses isn't needed). Identical filtering, dedupe, and layout to the zip path — both funnel
     * through {@link ClassfileExtraction}, so the apimgt-selection rules live in ONE place.
     *
     * @return the classfile roots to analyze (only non-empty ones), for {@link #report}.
     */
    public static List<File> extractApimgtClassfilesFromDir(File serverDir, File destDir) throws IOException {
        if (serverDir == null || !serverDir.isDirectory()) {
            throw new IOException("Coverage classfiles source is not a directory: " + serverDir);
        }
        ClassfileExtraction ex = new ClassfileExtraction(destDir);
        List<Path> files;
        try (java.util.stream.Stream<Path> walk = Files.walk(serverDir.toPath())) {
            files = walk.filter(Files::isRegularFile).toList();
        }
        for (Path p : files) {
            String fileName = p.getFileName().toString();
            // Plugin bundles land directly in the cp'd plugins/ dir; WARs are copied whole (not exploded), so a
            // name match is sufficient and safe within this already-scoped tree.
            if (fileName.startsWith("org.wso2.carbon.apimgt.") && fileName.endsWith(".jar")) {
                try (InputStream in = Files.newInputStream(p)) {
                    ex.addPlugin(fileName, in);
                }
            } else if (fileName.endsWith(".war")) {
                try (InputStream in = Files.newInputStream(p)) {
                    ex.mineWar(in);
                }
            }
        }
        return ex.finish(serverDir.getPath());
    }

    /**
     * Accumulates extracted APIM class files into the standard {@code {plugins, webapp-classes, webapp-lib}}
     * layout. Shared by the distribution-zip and the (docker-cp'd) directory extractors so the apimgt-selection,
     * zip-slip, dedupe, and partial-drift rules live in exactly ONE place (no reimplementation drift).
     */
    private static final class ClassfileExtraction {
        private final File pluginsDir;
        private final File webappClasses;
        private final File webappLib;
        private final java.util.Set<String> seenJar = new java.util.HashSet<>();
        private int plugins = 0;
        private int warClasses = 0;
        private int warJars = 0;

        ClassfileExtraction(File destDir) throws IOException {
            // Start clean so a stale extraction from a previous (possibly different) source can't leak in.
            deleteRecursively(destDir);
            pluginsDir = new File(destDir, "plugins");
            webappClasses = new File(destDir, "webapp-classes");
            webappLib = new File(destDir, "webapp-lib");
            for (File d : new File[]{pluginsDir, webappClasses, webappLib}) {
                Files.createDirectories(d.toPath());
            }
        }

        /** Copy an OSGi apimgt plugin bundle jar (by base name) into the plugins dir. */
        void addPlugin(String jarBaseName, InputStream in) throws IOException {
            Files.copy(in, new File(pluginsDir, jarBaseName).toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            plugins++;
        }

        /** Mine one webapp WAR (a nested zip): apimgt {@code WEB-INF/classes} and {@code WEB-INF/lib} apimgt jars. */
        void mineWar(InputStream warStream) throws IOException {
            try (java.util.zip.ZipInputStream war = new java.util.zip.ZipInputStream(warStream)) {
                ZipEntry we;
                while ((we = war.getNextEntry()) != null) {
                    String wn = we.getName();
                    if (we.isDirectory()) {
                        continue;
                    }
                    if (wn.startsWith("WEB-INF/classes/org/wso2/carbon/apimgt/") && wn.endsWith(".class")) {
                        File out = new File(webappClasses, wn.substring("WEB-INF/classes/".length()));
                        // Zip-Slip guard: the entry name keeps its sub-path, so a `..` segment could escape
                        // webappClasses. Require the resolved path to stay inside it before writing.
                        if (!isWithin(out, webappClasses)) {
                            logger.warn("Skipping WAR entry that escapes the extraction dir (zip-slip): {}", wn);
                        } else if (!out.exists()) { // first-wins dedupe across wars
                            out.getParentFile().mkdirs();
                            Files.copy(war, out.toPath());
                            warClasses++;
                        } else if (we.getSize() >= 0 && out.length() != we.getSize()) {
                            // Same FQN in another WAR with a different size: the first-wins copy may not be the
                            // bytecode the container loaded, so JaCoCo would silently report this class 0%
                            // (class-id mismatch). Surface it instead of dropping it silently. (getSize() is -1
                            // when unknown from the WAR's ZipInputStream — skip the check then.)
                            logger.warn("Duplicate webapp class across WARs with differing size (kept first "
                                    + "copy; may show 0% if it is not the loaded one): {}", wn);
                        }
                    } else if (wn.startsWith("WEB-INF/lib/") && wn.contains("apimgt") && wn.endsWith(".jar")) {
                        String b = baseName(wn);
                        File libOut = new File(webappLib, b);
                        // baseName already strips any path, so this can't traverse — guard kept for symmetry
                        // with the classes branch (and to satisfy zip-slip scanners).
                        if (!isWithin(libOut, webappLib)) {
                            logger.warn("Skipping WAR lib entry that escapes the extraction dir: {}", wn);
                        } else if (seenJar.add(b)) {
                            Files.copy(war, libOut.toPath());
                            warJars++;
                        }
                    }
                }
            }
        }

        /** Log the tally, fail on a total miss, warn on partial drift, and return the non-empty roots. */
        List<File> finish(String sourceLabel) throws IOException {
            logger.info("Extracted APIM classfiles from {}: plugins={} jars, webapp .class={}, webapp libs={} jars",
                    sourceLabel, plugins, warClasses, warJars);
            if (plugins == 0 && warClasses == 0 && warJars == 0) {
                throw new IOException("No org.wso2.carbon.apimgt.* class files found in " + sourceLabel);
            }
            // Partial-layout drift: one category empty while another is populated signals the paths moved (only a
            // TOTAL miss throws above). Coverage would silently shrink, so make the skew visible.
            if ((plugins == 0) != (warClasses == 0 && warJars == 0)) {
                logger.warn("APIM classfile extraction looks partial (plugins={} jars, webapp .class={}, webapp "
                        + "libs={} jars) — layout may have drifted; coverage denominator is understated.",
                        plugins, warClasses, warJars);
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
    }

    private static String baseName(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /** Zip-Slip containment check: true iff {@code child}'s canonical path is inside {@code parent}. Used to
     *  reject archive entries whose name contains {@code ..} segments that would escape the extraction dir. */
    private static boolean isWithin(File child, File parent) {
        try {
            return child.getCanonicalPath().startsWith(parent.getCanonicalPath() + File.separator);
        } catch (IOException e) {
            return false; // can't resolve → don't trust it
        }
    }

    /** Compiles {@link #DEFAULT_EXCLUDES} (JaCoCo-style, colon-separated, {@code *}/{@code ?} wildcards over the
     *  dotted FQN) into regexes, so the report denominator can apply the same excludes the agent uses. */
    private static List<Pattern> excludePatterns() {
        List<Pattern> patterns = new ArrayList<>();
        for (String glob : DEFAULT_EXCLUDES.split(":")) {
            if (glob.isEmpty()) {
                continue;
            }
            StringBuilder re = new StringBuilder();
            for (int i = 0; i < glob.length(); i++) {
                char c = glob.charAt(i);
                if (c == '*') {
                    re.append(".*");
                } else if (c == '?') {
                    re.append('.');
                } else if ("\\.[]{}()^$+|".indexOf(c) >= 0) {
                    re.append('\\').append(c);
                } else {
                    re.append(c);
                }
            }
            patterns.add(Pattern.compile(re.toString()));
        }
        return patterns;
    }

    private static boolean isExcluded(String fqn, List<Pattern> excludes) {
        for (Pattern p : excludes) {
            if (p.matcher(fqn).matches()) {
                return true;
            }
        }
        return false;
    }

    /** Like {@code Analyzer.analyzeAll}, but skips classes whose FQN matches {@code excludes} — so the analyzed
     *  (denominator) set matches the agent's instrumented set. Handles a jar or a class directory (the two root
     *  shapes {@link #extractApimgtClassfiles} produces); an unexpected shape is skipped loudly (see below).
     *  @return the number of classes skipped due to a read/parse <em>failure</em> (NOT the intentional exclude
     *  skips), so {@link #report} can surface a systemic analysis loss as a single summary line. */
    private static int analyzeFiltered(Analyzer analyzer, File root, List<Pattern> excludes) throws IOException {
        int failed = 0;
        if (root.isDirectory()) {
            // A directory root may hold loose .class files (webapp-classes) AND .jar files (plugins, webapp-lib)
            // — recurse into BOTH, matching what analyzeAll(dir) covered (missing the jars drops ~all classes).
            Path base = root.toPath();
            List<Path> files;
            try (java.util.stream.Stream<Path> walk = Files.walk(base)) {
                files = walk.filter(Files::isRegularFile).toList();
            }
            for (Path p : files) {
                String fileName = p.getFileName().toString();
                if (fileName.endsWith(".jar")) {
                    failed += analyzeJarFiltered(analyzer, p.toFile(), excludes);
                } else if (fileName.endsWith(".class")) {
                    String rel = base.relativize(p).toString().replace(File.separatorChar, '.');
                    String fqn = rel.substring(0, rel.length() - ".class".length());
                    if (isExcluded(fqn, excludes)) {
                        continue;
                    }
                    try (InputStream in = Files.newInputStream(p)) {
                        analyzer.analyzeClass(in, p.toString());
                    } catch (Exception perClass) {
                        failed++;
                        logger.warn("Skipping class {} during analysis: {}", fqn, perClass.getMessage());
                    }
                }
            }
        } else if (root.getName().endsWith(".jar")) {
            failed += analyzeJarFiltered(analyzer, root, excludes);
        } else {
            // Contract: extractApimgtClassfiles returns only directory roots (each may contain .class and .jar).
            // Anything else means the contract drifted — skip it loudly rather than analyze it UNFILTERED (which
            // would pull excluded stub/dto/gen classes into the denominator at a forced 0% and understate coverage).
            logger.warn("Unexpected classfile root shape (neither dir nor .jar): {} — skipped (would bypass excludes)",
                    root);
        }
        return failed;
    }

    /** Analyzes the {@code .class} entries of a jar, skipping any whose FQN matches {@code excludes}.
     *  @return the number of classes skipped due to a read/parse failure (for the {@link #report} summary). */
    private static int analyzeJarFiltered(Analyzer analyzer, File jar, List<Pattern> excludes) throws IOException {
        int failed = 0;
        try (ZipFile zf = new ZipFile(jar)) {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (e.isDirectory() || !e.getName().endsWith(".class")) {
                    continue;
                }
                String name = e.getName();
                String fqn = name.substring(0, name.length() - ".class".length()).replace('/', '.');
                if (isExcluded(fqn, excludes)) {
                    continue;
                }
                try (InputStream in = zf.getInputStream(e)) {
                    analyzer.analyzeClass(in, name);
                } catch (Exception perClass) {
                    failed++;
                    logger.warn("Skipping class {} during analysis: {}", fqn, perClass.getMessage());
                }
            }
        }
        return failed;
    }

    /** Recursively deletes {@code dir} (best-effort) for a clean slate — used to reset the classfiles dir before
     *  extraction and the exec dir before a suite run. */
    public static void deleteRecursively(File dir) throws IOException {
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
        // Apply the SAME excludes as the agent (DEFAULT_EXCLUDES) to the report DENOMINATOR, so the analyzed set
        // == the instrumented set. Otherwise generated stub/dto/gen/thrift classes (present in the jars but never
        // instrumented) would sit in the denominator at a forced 0%, systematically understating coverage.
        List<Pattern> excludes = excludePatterns();
        int skipped = 0;
        for (File cf : classfiles) {
            // Per-root so a hiccup in one root (e.g. a duplicate class) can't sink the whole report.
            try {
                skipped += analyzeFiltered(analyzer, cf, excludes);
            } catch (Exception ex) {
                logger.warn("Skipping classfile root {} during analysis: {}", cf, ex.getMessage());
            }
        }
        if (skipped > 0) {
            // Individual per-class failures are logged above; this one line makes a SYSTEMIC loss (bytecode-version
            // mismatch, mass duplicates) visible instead of hiding it in log spam — the denominator is short by N.
            logger.warn("Coverage analysis skipped {} class(es) due to read/parse failures — the denominator is "
                    + "reduced by that many classes (check the per-class warnings above for a systemic cause).", skipped);
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
