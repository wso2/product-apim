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

import org.jacoco.core.analysis.ICounter;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Command-line entry point for the integration coverage report, for execution data produced outside the v2 suite
 * run (the legacy integration lane's {@code jacoco-data-merge.exec} files).
 *
 * <p>It renders through the same {@link JacocoCoverage} extraction and {@link JacocoCoverage#reportDetailed} the
 * v2 lane's {@code CoverageAggregationListener} uses, so both lanes are measured against one class-file set and
 * one denominator (the APIM bundles and webapp classes, minus {@link JacocoCoverage#DEFAULT_EXCLUDES}).
 *
 * <pre>
 * CoverageReportCli --exec &lt;file|dir&gt; [--exec ...] (--classfiles-dir &lt;dir&gt; | --dist-zip &lt;zip&gt;)
 *                   --out &lt;dir&gt; [--title &lt;name&gt;] [--sources &lt;dir&gt;] [--expect-exec-files &lt;n&gt;]
 *                   [--allow-no-match]
 * </pre>
 *
 * <p>Outputs under {@code --out}: {@code xml/jacoco.xml}, {@code html/index.html}, {@code classfiles/} (the
 * extracted class files) and {@code coverage-summary.properties}. Exit status: {@value #EXIT_OK} on success,
 * {@value #EXIT_INTEGRITY} when the inputs cannot produce a trustworthy number (no execution data, a wrong
 * {@code .exec} count, or execution data that does not match the class files), {@value #EXIT_USAGE} on bad
 * arguments.
 */
public final class CoverageReportCli {

    public static final int EXIT_OK = 0;
    public static final int EXIT_INTEGRITY = 1;
    public static final int EXIT_USAGE = 2;

    static final String SUMMARY_FILE = "coverage-summary.properties";

    private static final String USAGE = "Usage: CoverageReportCli --exec <file|dir> [--exec <file|dir> ...]\n"
            + "         (--classfiles-dir <dir> | --dist-zip <zip>) --out <dir>\n"
            + "         [--title <name>] [--sources <dir>] [--expect-exec-files <n>] [--allow-no-match]\n"
            + "  --exec               an .exec file, or a directory searched recursively for *.exec\n"
            + "  --classfiles-dir     directory holding the APIM plugin jars and webapp WARs (docker cp of\n"
            + "                       repository/components/plugins and repository/deployment/server/webapps)\n"
            + "  --dist-zip           the APIM distribution zip (alternative to --classfiles-dir)\n"
            + "  --out                output directory for xml/, html/, classfiles/ and " + SUMMARY_FILE + "\n"
            + "  --title              report bundle title (default: apim-integration)\n"
            + "  --sources            source tree searched for src/main/java roots (HTML highlighting only)\n"
            + "  --expect-exec-files  fail unless exactly this many .exec files are found\n"
            + "  --allow-no-match     report execution data that does not match the class files instead of failing\n";

    private CoverageReportCli() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /** Runs the report; returns the process exit status instead of exiting. */
    static int run(String[] args, PrintStream out, PrintStream err) {
        Options options;
        try {
            options = Options.parse(args);
        } catch (IllegalArgumentException e) {
            err.println("error: " + e.getMessage());
            err.print(USAGE);
            return EXIT_USAGE;
        }

        try {
            List<File> execFiles = collectExecFiles(options.execInputs);
            if (execFiles.isEmpty()) {
                err.println("error: no .exec files found in " + options.execInputs);
                return EXIT_INTEGRITY;
            }
            out.println("Execution data files (" + execFiles.size() + "):");
            for (File f : execFiles) {
                out.println("  " + f + " (" + f.length() + " bytes)");
            }
            if (options.expectExecFiles != null && execFiles.size() != options.expectExecFiles) {
                err.println("error: expected " + options.expectExecFiles + " .exec file(s) but found "
                        + execFiles.size());
                return EXIT_INTEGRITY;
            }

            File classfilesDir = new File(options.out, "classfiles");
            List<File> classfileRoots = options.classfilesDir != null
                    ? JacocoCoverage.extractApimgtClassfilesFromDir(options.classfilesDir, classfilesDir)
                    : JacocoCoverage.extractApimgtClassfiles(options.distZip, classfilesDir);
            List<File> sourceRoots = options.sources != null
                    ? JacocoCoverage.discoverSourceRoots(options.sources) : Collections.emptyList();

            File xmlOut = new File(options.out, "xml" + File.separator + "jacoco.xml");
            File htmlDir = new File(options.out, "html");
            JacocoCoverage.CoverageResult result = JacocoCoverage.reportDetailed(execFiles, classfileRoots,
                    sourceRoots, xmlOut, htmlDir, options.title);

            File summary = new File(options.out, SUMMARY_FILE);
            writeSummary(summary, options.title, execFiles, result);
            printSummary(out, options.title, result);
            out.println("Report: " + xmlOut + ", " + new File(htmlDir, "index.html") + ", " + summary);

            if (result.classes().getTotalCount() == 0) {
                err.println("error: no classes were analyzed; the class files do not hold APIM classes");
                return EXIT_INTEGRITY;
            }
            if (!result.noMatchClasses().isEmpty()) {
                String message = result.noMatchClasses().size() + " class(es) have execution data that does not "
                        + "match the class files, so they report as uncovered: " + result.noMatchClasses();
                if (!options.allowNoMatch) {
                    err.println("error: " + message);
                    return EXIT_INTEGRITY;
                }
                err.println("warning: " + message);
            }
            return EXIT_OK;
        } catch (IOException | RuntimeException e) {
            err.println("error: coverage report failed: " + e);
            return EXIT_INTEGRITY;
        }
    }

    /** Expands each input into {@code .exec} files: a file as-is, a directory recursively; sorted, no duplicates. */
    static List<File> collectExecFiles(List<File> inputs) throws IOException {
        List<Path> found = new ArrayList<>();
        for (File input : inputs) {
            if (input.isDirectory()) {
                try (Stream<Path> walk = Files.walk(input.toPath())) {
                    walk.filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().endsWith(".exec"))
                            .forEach(found::add);
                }
            } else if (input.isFile()) {
                found.add(input.toPath());
            } else {
                throw new IOException("Execution data input does not exist: " + input);
            }
        }
        List<File> files = new ArrayList<>();
        found.stream().map(p -> p.toAbsolutePath().normalize()).distinct().sorted()
                .forEach(p -> files.add(p.toFile()));
        return files;
    }

    private static void writeSummary(File summary, String title, List<File> execFiles,
                                     JacocoCoverage.CoverageResult result) throws IOException {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("title", title);
        values.put("exec.files", String.valueOf(result.execFiles()));
        List<String> execNames = new ArrayList<>();
        for (File f : execFiles) {
            execNames.add(f.getPath());
        }
        values.put("exec.list", String.join(",", execNames));
        values.put("sessions", String.valueOf(result.sessions()));
        values.put("execution.data.entries", String.valueOf(result.executionDataEntries()));
        values.put("analysis.failures", String.valueOf(result.analysisFailures()));
        values.put("nomatch.count", String.valueOf(result.noMatchClasses().size()));
        values.put("nomatch.classes", String.join(",", result.noMatchClasses()));
        putCounter(values, "line", result.lines());
        values.put("line.pct", String.format(Locale.ROOT, "%.2f", result.linePercent()));
        putCounter(values, "instruction", result.instructions());
        putCounter(values, "branch", result.branches());
        putCounter(values, "method", result.methods());
        putCounter(values, "class", result.classes());

        Files.createDirectories(summary.getAbsoluteFile().getParentFile().toPath());
        try (Writer w = Files.newBufferedWriter(summary.toPath(), StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> e : values.entrySet()) {
                w.write(e.getKey() + "=" + e.getValue() + "\n");
            }
        }
    }

    private static void putCounter(Map<String, String> values, String name, ICounter counter) {
        values.put(name + ".covered", String.valueOf(counter.getCoveredCount()));
        values.put(name + ".total", String.valueOf(counter.getTotalCount()));
    }

    private static void printSummary(PrintStream out, String title, JacocoCoverage.CoverageResult result) {
        out.println("Coverage '" + title + "': sessions=" + result.sessions()
                + ", execution data entries=" + result.executionDataEntries()
                + ", analysis failures=" + result.analysisFailures()
                + ", no-match classes=" + result.noMatchClasses().size());
        out.println(String.format(Locale.ROOT, "  lines        %d/%d (%.2f%%)", result.lines().getCoveredCount(),
                result.lines().getTotalCount(), result.linePercent()));
        printCounter(out, "instructions", result.instructions());
        printCounter(out, "branches", result.branches());
        printCounter(out, "methods", result.methods());
        printCounter(out, "classes", result.classes());
    }

    private static void printCounter(PrintStream out, String name, ICounter counter) {
        out.println(String.format(Locale.ROOT, "  %-12s %d/%d", name, counter.getCoveredCount(),
                counter.getTotalCount()));
    }

    /** Parsed command-line arguments. */
    static final class Options {
        final List<File> execInputs = new ArrayList<>();
        File classfilesDir;
        File distZip;
        File out;
        File sources;
        String title = "apim-integration";
        Integer expectExecFiles;
        boolean allowNoMatch;

        static Options parse(String[] args) {
            Options o = new Options();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--exec":
                        o.execInputs.add(new File(value(args, ++i, arg)));
                        break;
                    case "--classfiles-dir":
                        o.classfilesDir = new File(value(args, ++i, arg));
                        break;
                    case "--dist-zip":
                        o.distZip = new File(value(args, ++i, arg));
                        break;
                    case "--out":
                        o.out = new File(value(args, ++i, arg));
                        break;
                    case "--sources":
                        o.sources = new File(value(args, ++i, arg));
                        break;
                    case "--title":
                        o.title = value(args, ++i, arg);
                        break;
                    case "--expect-exec-files":
                        String n = value(args, ++i, arg);
                        try {
                            o.expectExecFiles = Integer.parseInt(n);
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("--expect-exec-files needs an integer, got: " + n);
                        }
                        if (o.expectExecFiles < 1) {
                            throw new IllegalArgumentException("--expect-exec-files must be at least 1, got: " + n);
                        }
                        break;
                    case "--allow-no-match":
                        o.allowNoMatch = true;
                        break;
                    default:
                        throw new IllegalArgumentException("unknown argument: " + arg);
                }
            }
            if (o.execInputs.isEmpty()) {
                throw new IllegalArgumentException("--exec is required");
            }
            if ((o.classfilesDir == null) == (o.distZip == null)) {
                throw new IllegalArgumentException("exactly one of --classfiles-dir and --dist-zip is required");
            }
            if (o.out == null) {
                throw new IllegalArgumentException("--out is required");
            }
            if (o.title.isBlank()) {
                throw new IllegalArgumentException("--title must not be blank");
            }
            Path extraction = new File(o.out, "classfiles").getAbsoluteFile().toPath().normalize();
            File source = o.classfilesDir != null ? o.classfilesDir : o.distZip;
            if (source.getAbsoluteFile().toPath().normalize().startsWith(extraction)) {
                // The extraction directory is wiped before it is filled, which would delete the source.
                throw new IllegalArgumentException("the class-file source must not be inside " + extraction);
            }
            return o;
        }

        private static String value(String[] args, int index, String flag) {
            if (index >= args.length || args[index].startsWith("--")) {
                throw new IllegalArgumentException(flag + " needs a value");
            }
            return args[index];
        }
    }
}
