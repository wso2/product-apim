/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.am.integration.cucumbertests.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testcontainers.containers.Container;
import org.wso2.am.testcontainers.ApimRuntime;
import org.wso2.am.testcontainers.DistributedDynamicApimContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Bounded, read-only snapshots on a registry transport timeout; never changes the initialization verdict. */
public final class RegistryBootstrapDiagnostics {
    private static final Log LOG = LogFactory.getLog(RegistryBootstrapDiagnostics.class);
    private static final String JVM_SNAPSHOT = "timeout 12 sh -c '"
            + "pid=$(jcmd -l | awk \"/org.wso2.carbon.bootstrap.Bootstrap/ {print \\$1; exit}\"); "
            + "test -n \"$pid\" || exit 1; jcmd \"$pid\" Thread.print -l; jcmd \"$pid\" GC.heap_info'";

    private RegistryBootstrapDiagnostics() {
    }

    public static Runnable forRuntime(ApimRuntime runtime, String label, String tenant) {
        return () -> {
            var executor = Executors.newSingleThreadExecutor(task -> {
                Thread thread = new Thread(task, "registry-bootstrap-diagnostics");
                thread.setDaemon(true);
                return thread;
            });
            var capture = executor.submit(() -> {
                Path directory = Path.of("target", "logs", "registry-bootstrap",
                        (label + "-" + tenant).replaceAll("[^a-zA-Z0-9._-]", "_") + "-" + System.nanoTime());
                try {
                    Files.createDirectories(directory);
                    snapshot(directory.resolve("cp-jvm.txt"), () -> runtime.execInContainer("sh", "-c", JVM_SNAPSHOT));
                    if (runtime instanceof DistributedDynamicApimContainer distributed) {
                        snapshot(directory.resolve("mysql-locks.txt"), distributed::inspectDatabaseLocks);
                    }
                    LOG.warn("Registry bootstrap diagnostics saved to " + directory.toAbsolutePath());
                } catch (Exception unavailable) {
                    LOG.warn("Could not save registry bootstrap diagnostics", unavailable);
                }
            });
            try {
                capture.get(25, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                LOG.warn("Registry diagnostic capture interrupted", interrupted);
            } catch (Exception unavailable) {
                LOG.warn("Registry diagnostic capture did not complete within its bound", unavailable);
            } finally {
                capture.cancel(true);
                executor.shutdownNow();
            }
        };
    }

    private static void snapshot(Path file, Snapshot call) throws Exception {
        try {
            Container.ExecResult result = call.capture();
            Files.writeString(file, "exit=" + result.getExitCode() + "\n" + result.getStdout() + result.getStderr());
        } catch (Exception unavailable) {
            Files.writeString(file, "Diagnostic unavailable: " + unavailable);
        }
    }

    @FunctionalInterface
    private interface Snapshot {
        Container.ExecResult capture() throws Exception;
    }
}
