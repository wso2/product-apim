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

package org.wso2.am.testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

/** One unprivileged clock observer per suite, using the same Linux kernel clock as the product containers. */
public final class DockerHostClockMonitor implements AutoCloseable {
    private static final long DISTRIBUTED_MEMORY_LIMIT_BYTES = 128L * 1024 * 1024;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final long WARMUP_NANOS = Duration.ofSeconds(20).toNanos();
    private final ClockSampleDetector detector = new ClockSampleDetector();
    private final AtomicReference<String> failure = new AtomicReference<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Path directory;
    private final BufferedWriter samples;
    private final GenericContainer<?> probe;
    private final StringBuilder pending = new StringBuilder();
    private volatile long lastReceivedNanos;
    private volatile String bootId;
    private volatile boolean closing;

    public DockerHostClockMonitor(Path directory) throws IOException {
        this(directory, false);
    }

    public DockerHostClockMonitor(Path directory, boolean distributedTopology) throws IOException {
        this.directory = directory;
        Files.createDirectories(directory);
        samples = Files.newBufferedWriter(directory.resolve("samples.jsonl"));
        probe = new GenericContainer<>(System.getProperty("node.docker.image.name", "node-app-server:latest"))
                .withNetworkMode("none")
                .withLabel("org.wso2.apim.clock-probe", "true")
                .withCopyFileToContainer(MountableFile.forClasspathResource("host-clock/probe.js"),
                        "/tmp/apim-clock-probe.js")
                .withCreateContainerCmdModifier(cmd -> {
                    cmd.withEntrypoint("node");
                    cmd.getHostConfig().withNanoCPUs(100_000_000L);
                    if (distributedTopology) {
                        cmd.getHostConfig().withMemory(DISTRIBUTED_MEMORY_LIMIT_BYTES)
                                .withMemorySwap(DistributedMemoryLimits.memoryAndSwapLimitBytes(
                                        DISTRIBUTED_MEMORY_LIMIT_BYTES));
                    }
                })
                .withCommand("/tmp/apim-clock-probe.js")
                .withLogConsumer(this::consume)
                .waitingFor(Wait.forLogMessage(".*\"type\":\"clock-sample\".*\\n", 1)
                        .withStartupTimeout(Duration.ofSeconds(45)));
    }

    public void start(Path preparationFile) throws IOException, InterruptedException {
        try {
            probe.start();
            long start = System.nanoTime();
            while (System.nanoTime() - start < WARMUP_NANOS) {
                assertHealthy();
                Thread.sleep(200);
            }
            if (detector.samples() < 50) {
                throw new IllegalStateException("Insufficient Docker host clock samples during startup");
            }
            if (preparationFile != null) {
                JsonNode prepared = JSON.readTree(preparationFile.toFile());
                if (!prepared.path("prepared").asBoolean() || !"slew".equals(prepared.path("mode").asText())
                        || !bootId.equals(prepared.path("bootId").asText())) {
                    throw new IllegalStateException("Clock preparation marker does not describe this Docker host: "
                            + preparationFile);
                }
                Files.copy(preparationFile, directory.resolve("prepared.json"));
            }
            assertHealthy();
        } catch (IOException | InterruptedException | RuntimeException e) {
            failure.compareAndSet(null, "Clock preflight failed: " + e.getMessage());
            try {
                close();
            } catch (Exception cleanup) {
                e.addSuppressed(cleanup);
            }
            throw e;
        }
    }

    private synchronized void consume(OutputFrame frame) {
        if (closed.get()) {
            return;
        }
        if (frame.getType() == OutputFrame.OutputType.END) {
            if (!closing) {
                failure.compareAndSet(null, "Docker host clock observer terminated unexpectedly");
            }
            return;
        }
        pending.append(frame.getUtf8String());
        int newline;
        while ((newline = pending.indexOf("\n")) >= 0) {
            String line = pending.substring(0, newline);
            pending.delete(0, newline + 1);
            try {
                JsonNode sample = JSON.readTree(line);
                if (!"clock-sample".equals(sample.path("type").asText())) {
                    throw new IOException("Unexpected clock observer output");
                }
                String currentBootId = sample.path("bootId").asText();
                if (currentBootId.isBlank() || (bootId != null && !bootId.equals(currentBootId))) {
                    throw new IOException("Docker host boot identity missing or changed");
                }
                bootId = currentBootId;
                detector.accept(sample.required("wallTimeMillis").asLong(),
                        Long.parseLong(sample.required("monotonicNanos").asText()),
                        Long.parseLong(sample.required("monotonicAfterNanos").asText()));
                lastReceivedNanos = System.nanoTime();
                samples.write(line);
                samples.newLine();
                samples.flush();
            } catch (IOException | RuntimeException e) {
                failure.compareAndSet(null, "Could not record Docker host clock sample: " + e.getMessage());
            }
        }
    }

    public void assertHealthy() {
        String detected = detector.failure();
        if (detected != null) {
            failure.compareAndSet(null, detected);
        }
        if (!closing && lastReceivedNanos != 0
                && System.nanoTime() - lastReceivedNanos > Duration.ofSeconds(10).toNanos()) {
            failure.compareAndSet(null, "Docker host clock observer has supplied no samples for 10 seconds");
        }
        if (failure.get() != null) {
            throw new IllegalStateException("APIM test infrastructure clock failure: " + failure.get()
                    + "; evidence=" + directory);
        }
    }

    public String containerId() {
        return probe.getContainerId();
    }

    @Override
    public void close() throws IOException {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        try {
            assertHealthy();
        } catch (RuntimeException e) {
            // The owner propagates the clock failure after collecting diagnostics and stopping the observer.
        }
        closing = true;
        try {
            probe.stop();
        } finally {
            synchronized (this) {
                samples.close();
                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("bootId", bootId);
                summary.put("samples", detector.samples());
                summary.put("largestDiscontinuityMillis", detector.largestDiscontinuityMillis());
                summary.put("failure", failure.get());
                summary.put("healthy", failure.get() == null);
                JSON.writerWithDefaultPrettyPrinter().writeValue(directory.resolve("summary.json").toFile(), summary);
            }
        }
    }
}
