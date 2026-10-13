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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** A filesystem Unix socket forwarding to the host's loopback-only CONNECT proxy. */
final class BrowserProxyRelay implements AutoCloseable {
    private final Path directory;
    private final Process process;

    BrowserProxyRelay(int proxyPort) {
        Path createdDirectory = null;
        Process createdProcess = null;
        try {
            createdDirectory = Files.createTempDirectory(Path.of("/tmp"), "pw-proxy-");
            createdProcess = new ProcessBuilder("socat", "-d", "-d",
                    "UNIX-LISTEN:" + createdDirectory.resolve("connect.sock") + ",fork",
                    "TCP4:127.0.0.1:" + proxyPort)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            awaitListening(createdProcess).get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            stop(createdProcess, createdDirectory);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Could not start the browser's Unix proxy relay (socat is required)", e);
        }
        directory = createdDirectory;
        process = createdProcess;
    }

    Path socket() {
        return directory.resolve("connect.sock");
    }

    private static CompletableFuture<Void> awaitListening(Process process) {
        CompletableFuture<Void> ready = new CompletableFuture<>();
        Thread reader = new Thread(() -> {
            try (BufferedReader lines = new BufferedReader(new InputStreamReader(
                    process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = lines.readLine()) != null) {
                    if (line.contains("listening on")) {
                        ready.complete(null);
                    }
                }
                ready.completeExceptionally(new IOException("Proxy relay exited before listening"));
            } catch (IOException e) {
                ready.completeExceptionally(e);
            }
        }, "browser-proxy-relay");
        reader.setDaemon(true);
        reader.start();
        return ready;
    }

    private static void stop(Process process, Path directory) {
        if (process != null) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            try {
                process.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (directory != null) {
            try {
                Files.deleteIfExists(directory.resolve("connect.sock"));
                Files.deleteIfExists(directory);
            } catch (IOException e) {
                throw new IllegalStateException("Could not remove the browser's Unix proxy relay socket", e);
            }
        }
    }

    @Override
    public void close() {
        stop(process, directory);
    }
}
