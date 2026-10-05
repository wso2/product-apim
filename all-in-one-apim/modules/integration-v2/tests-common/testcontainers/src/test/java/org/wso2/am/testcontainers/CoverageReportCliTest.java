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

import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.IRuntime;
import org.jacoco.core.runtime.LoggerRuntime;
import org.jacoco.core.runtime.RuntimeData;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Exercises {@link CoverageReportCli} end to end on real JaCoCo execution data: a small APIM-package class is
 * compiled, instrumented with the JaCoCo runtime, executed, and its probes written to an {@code .exec} file, which
 * the CLI then reports against a plugin-jar class-file directory.
 */
public class CoverageReportCliTest {

    private static final String PACKAGE = "org.wso2.carbon.apimgt.covtest";
    private static final String SAMPLE = PACKAGE + ".Sample";
    private static final String PAYLOAD = PACKAGE + ".dto.Payload";

    /** Sample: the implicit constructor, add() and unused() each occupy one source line. */
    private static final String SAMPLE_SOURCE = "package " + PACKAGE + ";\n"
            + "public class Sample {\n"
            + "    public int add(int a, int b) { return a + b; }\n"
            + "    public int unused() { return 1; }\n"
            + "}\n";

    /** The same class name with different bytecode, standing in for a different build of the product. */
    private static final String SAMPLE_OTHER_BUILD_SOURCE = "package " + PACKAGE + ";\n"
            + "public class Sample {\n"
            + "    public int add(int a, int b) { return a + b + 0 * a; }\n"
            + "    public int unused() { return 2; }\n"
            + "}\n";

    /** Matches the {@code *.dto.*} default exclude, so it must stay out of the denominator. */
    private static final String PAYLOAD_SOURCE = "package " + PACKAGE + ".dto;\n"
            + "public class Payload {\n"
            + "    public int value() { return 3; }\n"
            + "}\n";

    private Path work;

    @BeforeMethod
    public void createWorkDir() throws IOException {
        work = Files.createTempDirectory("coverage-cli-test");
    }

    @AfterMethod(alwaysRun = true)
    public void deleteWorkDir() throws IOException {
        JacocoCoverage.deleteRecursively(work.toFile());
    }

    @Test
    public void matchingExecutionDataReportsExactCountersAndExitsZero() throws Exception {
        byte[] sample = compile(SAMPLE, SAMPLE_SOURCE);
        File classfiles = pluginDir(sample, compile(PAYLOAD, PAYLOAD_SOURCE));
        File exec = exec(sample, "add", "legacy-coverage-exec-1/jacoco-data-merge.exec");

        Result r = cli("--exec", exec.getPath(), "--classfiles-dir", classfiles.getPath(), "--out", out());

        Assert.assertEquals(r.exit, CoverageReportCli.EXIT_OK, r.text());
        Properties s = r.summary();
        Assert.assertEquals(s.getProperty("nomatch.count"), "0", r.text());
        Assert.assertEquals(s.getProperty("exec.files"), "1");
        Assert.assertEquals(s.getProperty("sessions"), "1");
        // Only Sample is in the denominator: Payload sits in *.dto.* and is excluded exactly as the agent does.
        Assert.assertEquals(s.getProperty("class.total"), "1");
        Assert.assertEquals(s.getProperty("class.covered"), "1");
        Assert.assertEquals(s.getProperty("method.total"), "3");
        Assert.assertEquals(s.getProperty("method.covered"), "2");
        Assert.assertEquals(s.getProperty("line.total"), "3");
        Assert.assertEquals(s.getProperty("line.covered"), "2");
        Assert.assertEquals(s.getProperty("line.pct"), "66.67");
        Assert.assertTrue(new File(out(), "xml/jacoco.xml").length() > 0, "XML report missing");
        Assert.assertTrue(new File(out(), "html/index.html").isFile(), "HTML report missing");
    }

    @Test
    public void executionDataFromAnotherBuildFailsAsNoMatch() throws Exception {
        File classfiles = pluginDir(compile(SAMPLE, SAMPLE_SOURCE));
        File exec = exec(compile(SAMPLE, SAMPLE_OTHER_BUILD_SOURCE), "add", "other.exec");

        Result r = cli("--exec", exec.getPath(), "--classfiles-dir", classfiles.getPath(), "--out", out());

        Assert.assertEquals(r.exit, CoverageReportCli.EXIT_INTEGRITY, r.text());
        Assert.assertTrue(r.err.contains("does not match the class files"), r.text());
        Properties s = r.summary();
        Assert.assertEquals(s.getProperty("nomatch.count"), "1");
        Assert.assertEquals(s.getProperty("nomatch.classes"), SAMPLE);
        Assert.assertEquals(s.getProperty("line.covered"), "0");
    }

    @Test
    public void allowNoMatchReportsTheMismatchWithoutFailing() throws Exception {
        File classfiles = pluginDir(compile(SAMPLE, SAMPLE_SOURCE));
        File exec = exec(compile(SAMPLE, SAMPLE_OTHER_BUILD_SOURCE), "add", "other.exec");

        Result r = cli("--exec", exec.getPath(), "--classfiles-dir", classfiles.getPath(), "--out", out(),
                "--allow-no-match");

        Assert.assertEquals(r.exit, CoverageReportCli.EXIT_OK, r.text());
        Assert.assertTrue(r.err.contains("warning:"), r.text());
        Assert.assertEquals(r.summary().getProperty("nomatch.count"), "1");
    }

    @Test
    public void executionDataFilesAreMergedAcrossGroups() throws Exception {
        byte[] sample = compile(SAMPLE, SAMPLE_SOURCE);
        File classfiles = pluginDir(sample);
        exec(sample, "add", "execs/legacy-coverage-exec-1/jacoco-data-merge.exec");
        exec(sample, "unused", "execs/legacy-coverage-exec-2/jacoco-data-merge.exec");

        Result r = cli("--exec", work.resolve("execs").toString(), "--classfiles-dir", classfiles.getPath(),
                "--out", out(), "--expect-exec-files", "2");

        Assert.assertEquals(r.exit, CoverageReportCli.EXIT_OK, r.text());
        Properties s = r.summary();
        Assert.assertEquals(s.getProperty("exec.files"), "2");
        Assert.assertEquals(s.getProperty("sessions"), "2");
        Assert.assertEquals(s.getProperty("line.covered"), "3");
        Assert.assertEquals(s.getProperty("method.covered"), "3");
    }

    @Test
    public void wrongExecFileCountFails() throws Exception {
        byte[] sample = compile(SAMPLE, SAMPLE_SOURCE);
        File classfiles = pluginDir(sample);
        exec(sample, "add", "execs/legacy-coverage-exec-1/jacoco-data-merge.exec");
        exec(sample, "add", "execs/legacy-coverage-exec-2/jacoco-data-merge.exec");

        Result r = cli("--exec", work.resolve("execs").toString(), "--classfiles-dir", classfiles.getPath(),
                "--out", out(), "--expect-exec-files", "4");

        Assert.assertEquals(r.exit, CoverageReportCli.EXIT_INTEGRITY, r.text());
        Assert.assertTrue(r.err.contains("expected 4 .exec file(s) but found 2"), r.text());
    }

    @Test
    public void directoryWithoutExecutionDataFails() throws Exception {
        File classfiles = pluginDir(compile(SAMPLE, SAMPLE_SOURCE));
        Files.createDirectories(work.resolve("empty"));

        Result r = cli("--exec", work.resolve("empty").toString(), "--classfiles-dir", classfiles.getPath(),
                "--out", out());

        Assert.assertEquals(r.exit, CoverageReportCli.EXIT_INTEGRITY, r.text());
        Assert.assertTrue(r.err.contains("no .exec files found"), r.text());
    }

    @Test
    public void classFilesWithoutApimClassesFail() throws Exception {
        File exec = exec(compile(SAMPLE, SAMPLE_SOURCE), "add", "a.exec");
        Path unrelated = Files.createDirectories(work.resolve("unrelated"));
        Files.write(unrelated.resolve("readme.txt"), "no jars".getBytes(StandardCharsets.UTF_8));

        Result r = cli("--exec", exec.getPath(), "--classfiles-dir", unrelated.toString(), "--out", out());

        Assert.assertEquals(r.exit, CoverageReportCli.EXIT_INTEGRITY, r.text());
        Assert.assertTrue(r.err.contains("No org.wso2.carbon.apimgt.* class files found"), r.text());
    }

    @Test
    public void invalidArgumentsPrintUsage() throws Exception {
        Assert.assertEquals(cli().exit, CoverageReportCli.EXIT_USAGE);
        Assert.assertEquals(cli("--exec", "a.exec", "--out", out()).exit, CoverageReportCli.EXIT_USAGE,
                "a class-file source is required");
        Assert.assertEquals(cli("--exec", "a.exec", "--classfiles-dir", "d", "--dist-zip", "z.zip",
                "--out", out()).exit, CoverageReportCli.EXIT_USAGE, "class-file sources are exclusive");
        Assert.assertEquals(cli("--exec", "a.exec", "--classfiles-dir", "d", "--out", out(),
                "--expect-exec-files", "0").exit, CoverageReportCli.EXIT_USAGE);
        Assert.assertEquals(cli("--exec", "a.exec", "--classfiles-dir", "d", "--out").exit,
                CoverageReportCli.EXIT_USAGE, "a flag without its value");
        Result inside = cli("--exec", "a.exec", "--classfiles-dir", out() + "/classfiles/plugins", "--out", out());
        Assert.assertEquals(inside.exit, CoverageReportCli.EXIT_USAGE, inside.text());
        Assert.assertTrue(inside.err.contains("must not be inside"), inside.text());
        Result unknown = cli("--bogus");
        Assert.assertEquals(unknown.exit, CoverageReportCli.EXIT_USAGE);
        Assert.assertTrue(unknown.err.contains("unknown argument: --bogus"), unknown.text());
        Assert.assertTrue(unknown.err.contains("Usage: CoverageReportCli"), unknown.text());
    }

    private String out() {
        return work.resolve("out").toString();
    }

    private Result cli(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exit = CoverageReportCli.run(args, new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Result(exit, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8),
                new File(out(), CoverageReportCli.SUMMARY_FILE));
    }

    /** Compiles one source file and returns the bytecode of {@code className}. */
    private byte[] compile(String className, String source) throws IOException {
        Path src = Files.createTempDirectory(work, "src");
        Path srcFile = src.resolve(className.replace('.', '/') + ".java");
        Files.createDirectories(srcFile.getParent());
        Files.write(srcFile, source.getBytes(StandardCharsets.UTF_8));
        Path classes = Files.createTempDirectory(work, "classes");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Assert.assertNotNull(compiler, "a JDK (not a JRE) is required to compile the test classes");
        int status = compiler.run(null, null, null, "-d", classes.toString(), srcFile.toString());
        Assert.assertEquals(status, 0, "compiling " + className + " failed");
        return Files.readAllBytes(classes.resolve(className.replace('.', '/') + ".class"));
    }

    /** Lays the classes out as an APIM plugin bundle, the shape a docker cp of components/plugins yields. */
    private File pluginDir(byte[] sample, byte[]... others) throws IOException {
        Path dir = Files.createDirectories(work.resolve("server/plugins"));
        File jar = dir.resolve("org.wso2.carbon.apimgt.covtest_1.0.0.jar").toFile();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar))) {
            putClass(jos, SAMPLE, sample);
            for (byte[] other : others) {
                putClass(jos, PAYLOAD, other);
            }
        }
        return work.resolve("server").toFile();
    }

    private static void putClass(JarOutputStream jos, String className, byte[] bytes) throws IOException {
        jos.putNextEntry(new JarEntry(className.replace('.', '/') + ".class"));
        jos.write(bytes);
        jos.closeEntry();
    }

    /** Runs {@code method} of an instrumented copy of {@code bytecode} and writes the probes to {@code relPath}. */
    private File exec(byte[] bytecode, String method, String relPath) throws Exception {
        IRuntime runtime = new LoggerRuntime();
        byte[] instrumented = new Instrumenter(runtime).instrument(bytecode, SAMPLE);
        RuntimeData data = new RuntimeData();
        runtime.startup(data);
        try {
            Class<?> type = new SingleClassLoader(SAMPLE, instrumented).loadClass(SAMPLE);
            Object instance = type.getDeclaredConstructor().newInstance();
            if ("add".equals(method)) {
                type.getMethod("add", int.class, int.class).invoke(instance, 1, 2);
            } else {
                type.getMethod(method).invoke(instance);
            }
            ExecutionDataStore store = new ExecutionDataStore();
            SessionInfoStore sessions = new SessionInfoStore();
            data.collect(store, sessions, false);
            File file = work.resolve(relPath).toFile();
            Files.createDirectories(file.getParentFile().toPath());
            try (OutputStream os = new FileOutputStream(file)) {
                ExecutionDataWriter writer = new ExecutionDataWriter(os);
                sessions.accept(writer);
                store.accept(writer);
            }
            return file;
        } finally {
            runtime.shutdown();
        }
    }

    /** Defines one class from the given bytes; everything else is delegated to the parent loader. */
    private static final class SingleClassLoader extends ClassLoader {
        private final String name;
        private final byte[] bytes;

        SingleClassLoader(String name, byte[] bytes) {
            super(CoverageReportCliTest.class.getClassLoader());
            this.name = name;
            this.bytes = bytes;
        }

        @Override
        protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
            if (name.equals(className)) {
                synchronized (getClassLoadingLock(className)) {
                    Class<?> loaded = findLoadedClass(className);
                    return loaded != null ? loaded : defineClass(className, bytes, 0, bytes.length);
                }
            }
            return super.loadClass(className, resolve);
        }
    }

    private static final class Result {
        final int exit;
        final String out;
        final String err;
        final File summaryFile;

        Result(int exit, String out, String err, File summaryFile) {
            this.exit = exit;
            this.out = out;
            this.err = err;
            this.summaryFile = summaryFile;
        }

        Properties summary() throws IOException {
            Assert.assertTrue(summaryFile.isFile(), "summary not written: " + text());
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(summaryFile.toPath())) {
                p.load(in);
            }
            return p;
        }

        String text() {
            return "exit=" + exit + "\nstdout:\n" + out + "\nstderr:\n" + err;
        }
    }
}
