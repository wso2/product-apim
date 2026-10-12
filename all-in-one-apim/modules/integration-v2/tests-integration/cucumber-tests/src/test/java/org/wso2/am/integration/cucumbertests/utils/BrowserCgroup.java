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

import com.microsoft.playwright.BrowserType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Caps the host Playwright browser with a cgroup-v2 group on Linux hosts (CI). The group is created once per job by
 * {@code src/test/resources/browser-cgroup/setup-browser-cgroup.sh} (1 CPU, 2.5 GiB, no swap); when it exists,
 * Chromium is launched through {@code chromium-cgroup-wrapper.sh}, which moves itself into the group and execs the
 * real headless shell, so the browser and every process it spawns share that one budget. Hosts without the group
 * (macOS has no cgroups) launch the browser uncapped.
 */
final class BrowserCgroup {

    /** System property overriding the cgroup directory. */
    static final String CGROUP_PROPERTY = "playwright.browser.cgroup";
    private static final String DEFAULT_CGROUP = "/sys/fs/cgroup/pw-browser";
    private static final Path WRAPPER = Paths.get("src/test/resources/browser-cgroup/chromium-cgroup-wrapper.sh");
    private static final String HEADLESS_SHELL_BINARY = "chrome-headless-shell";

    private BrowserCgroup() {
    }

    /** The browser cgroup directory, when this is a Linux host on which the group has been created. */
    static Optional<Path> active() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux")) {
            return Optional.empty();
        }
        Path cgroup = Paths.get(System.getProperty(CGROUP_PROPERTY, DEFAULT_CGROUP));
        return Files.isRegularFile(cgroup.resolve("cgroup.procs")) ? Optional.of(cgroup) : Optional.empty();
    }

    /** Routes the launch through the wrapper so the browser starts inside {@code cgroup}. */
    static BrowserType.LaunchOptions applyTo(BrowserType.LaunchOptions options, BrowserType chromium, Path cgroup) {
        Path wrapper = WRAPPER.toAbsolutePath();
        if (!Files.isExecutable(wrapper) && !wrapper.toFile().setExecutable(true)) {
            throw new IllegalStateException("Browser cgroup wrapper is not executable: " + wrapper);
        }
        // LaunchOptions.env replaces the browser's whole environment, so start from this process's environment.
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("PW_BROWSER_CGROUP", cgroup.toString());
        env.put("PW_REAL_BROWSER", headlessShell(chromium).toString());
        return options.setExecutablePath(wrapper).setEnv(env);
    }

    /** The group's limits and member pids, for the launch log. */
    static String describe(Path cgroup) {
        return "cgroup " + cgroup + " cpu.max=[" + read(cgroup.resolve("cpu.max")) + "] memory.max=["
                + read(cgroup.resolve("memory.max")) + "] pids=[" + read(cgroup.resolve("cgroup.procs"))
                .replace('\n', ' ') + "]";
    }

    /**
     * The Chromium headless shell Playwright installed alongside {@code chromium.executablePath()}: the full
     * browser lives under {@code <cache>/chromium-<rev>/}, the shell under {@code <cache>/chromium_headless_shell-<rev>/}.
     */
    private static Path headlessShell(BrowserType chromium) {
        Path fullBrowser = Paths.get(chromium.executablePath());
        Path revisionDir = fullBrowser;
        while (revisionDir != null && (revisionDir.getFileName() == null
                || !revisionDir.getFileName().toString().startsWith("chromium-"))) {
            revisionDir = revisionDir.getParent();
        }
        if (revisionDir == null) {
            throw new IllegalStateException("Cannot locate the Chromium revision directory of " + fullBrowser);
        }
        String revision = revisionDir.getFileName().toString().substring("chromium-".length());
        Path shellDir = revisionDir.resolveSibling("chromium_headless_shell-" + revision);
        try (Stream<Path> files = Files.walk(shellDir)) {
            return files.filter(p -> p.getFileName().toString().equals(HEADLESS_SHELL_BINARY))
                    .filter(Files::isExecutable)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No " + HEADLESS_SHELL_BINARY + " under " + shellDir));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot search " + shellDir + " for " + HEADLESS_SHELL_BINARY, e);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file).trim();
        } catch (IOException e) {
            return "unreadable: " + e.getMessage();
        }
    }
}
