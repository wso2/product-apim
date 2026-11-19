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
import java.util.Set;
import java.util.stream.Stream;

/**
 * Configures the Linux Chromium wrapper: a private network namespace and, when present, the browser cgroup-v2
 * budget (1 CPU, 2.5 GiB and no swap for all-in-one; 512 MiB and up to 128 MiB swap for distributed CI). The
 * wrapper's processes inherit the cgroup and the browser keeps its caller's user identity. The wrapper is used on
 * Linux hosts prepared by {@code setup-browser-network.sh} (CI); other hosts launch Chromium directly.
 */
final class BrowserCgroup {

    /** System property overriding the cgroup directory. */
    static final String CGROUP_PROPERTY = "playwright.browser.cgroup";
    private static final String DEFAULT_CGROUP = "/sys/fs/cgroup/pw-browser";
    /** Written by {@code setup-browser-network.sh} once the wrapper's sudo unshare is permitted. */
    private static final Path NETWORK_SETUP_MARKER = Paths.get("/etc/pw-browser-network");
    private static final Path WRAPPER = Paths.get("src/test/resources/browser-cgroup/chromium-cgroup-wrapper.sh");
    private static final Set<String> HEADLESS_SHELL_BINARIES = Set.of("chrome-headless-shell", "headless_shell");

    private BrowserCgroup() {
    }

    /** The browser cgroup directory, when this is a Linux host on which the group has been created. */
    static Optional<Path> active() {
        if (!isLinux()) {
            return Optional.empty();
        }
        Path cgroup = Paths.get(System.getProperty(CGROUP_PROPERTY, DEFAULT_CGROUP));
        return Files.isRegularFile(cgroup.resolve("cgroup.procs")) ? Optional.of(cgroup) : Optional.empty();
    }

    /**
     * Whether the browser runs in its private network namespace: a Linux host prepared by
     * {@code setup-browser-network.sh}. A host with the browser cgroup but without that preparation is refused,
     * since the cgroup is entered only through the namespace wrapper.
     */
    static boolean networkIsolated(Optional<Path> cgroup) {
        if (!isLinux() || !Files.exists(NETWORK_SETUP_MARKER)) {
            if (cgroup.isPresent()) {
                throw new IllegalStateException("Browser cgroup " + cgroup.get() + " exists but the browser network "
                        + "namespace is not configured: run src/test/resources/browser-cgroup/setup-browser-network.sh");
            }
            return false;
        }
        return true;
    }

    private static boolean isLinux() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");
    }

    /** Routes the launch through the private network namespace, with the optional cgroup budget. */
    static BrowserType.LaunchOptions applyTo(BrowserType.LaunchOptions options, BrowserType chromium,
            Optional<Path> cgroup, BrowserProxyRelay relay, int proxyPort) {
        Path wrapper = WRAPPER.toAbsolutePath();
        if (!Files.isExecutable(wrapper) && !wrapper.toFile().setExecutable(true)) {
            throw new IllegalStateException("Browser cgroup wrapper is not executable: " + wrapper);
        }
        // LaunchOptions.env replaces the browser's whole environment, so start from this process's environment.
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("PW_BROWSER_CGROUP", cgroup.map(Path::toString).orElse(""));
        env.put("PW_BROWSER_PROXY_SOCKET", relay.socket().toString());
        env.put("PW_BROWSER_PROXY_PORT", Integer.toString(proxyPort));
        env.put("PW_BROWSER_HOME", System.getenv().getOrDefault("HOME", ""));
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
            return files.filter(p -> HEADLESS_SHELL_BINARIES.contains(p.getFileName().toString()))
                    .filter(Files::isExecutable)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No Chromium headless shell under " + shellDir));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot search " + shellDir + " for the Chromium headless shell", e);
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
