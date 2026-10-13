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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

/**
 * The OTLP collector APIM exports spans to, as ONE real Jaeger all-in-one container per block.
 *
 * <p><b>Why a real collector and not a double.</b> The collector is the only component in this arc that we did
 * not author, so it is the only one that can CONTRADICT us: it parses the OTLP payload, applies its own
 * attribute typing and span-kind classification, and answers queries about what it actually stored. Every
 * span-semantics assertion (span kind, parent/child linkage, attribute names/values/types, the
 * {@code process.pid} resource attribute) is made against {@link #getQueryBaseUrl()} — never against
 * anything we built. A stand-in would be asserting against itself.
 *
 * <p><b>Which transport reaches it.</b> Only the OTLP/gRPC path. {@code JaegerTelemetry} hardcodes
 * {@code OtlpGrpcSpanExporter}, so the block's overlay selects the {@code jaeger} tracer (which ignores the
 * {@code Protocol} key) and addresses this container on {@link #OTLP_GRPC_PORT}. OTLP/HTTP — the transport
 * added for Moesif — is configured separately and never points at this collector.
 *
 * <p><b>Why the container is PER-BLOCK</b> rather than a shared singleton multi-homed across block networks
 * like {@link NodeAppServer}: the traces stored here are scenario-visible state. A shared collector would let
 * a sibling class's spans satisfy this block's assertions, and its trace store would grow without bound
 * across the whole suite. One collector per block keeps every query scoped to the block that produced it.
 *
 * <p><b>Scope (CLAUDE.md 14).</b> Infrastructure only. This class boots a container and publishes URLs. It
 * performs NO product operation — enabling tracing, invoking an API and reading spans back are all feature
 * steps.
 */
public class DynamicOtlpCollector {

    private static final Log logger = LogFactory.getLog(DynamicOtlpCollector.class);

    /** Alias the APIM overlay uses to reach this collector on the block network. */
    public static final String COLLECTOR_ALIAS = "jaeger";
    /** OTLP gRPC receiver — the port the {@code jaeger} tracer's overlay points at. */
    public static final int OTLP_GRPC_PORT = 4317;
    /** Jaeger query API + UI — the host-mapped port every span assertion reads through. */
    public static final int QUERY_PORT = 16686;

    private static final String DEFAULT_IMAGE = "jaegertracing/all-in-one:1.76.0";

    private final GenericContainer<?> collector;

    /**
     * @param blockLabel owning block's label — log prefix and diagnostics only
     * @param network    the block's private docker network this container joins (the caller owns its lifecycle;
     *                   {@link #stop()} must run before it is closed)
     */
    public DynamicOtlpCollector(String blockLabel, Network network) {

        logger.info("Initializing DynamicOtlpCollector for block '" + blockLabel + "'...");

        collector = new GenericContainer<>(
                System.getProperty("jaeger.collector.docker.image.name", DEFAULT_IMAGE))
                .withExposedPorts(OTLP_GRPC_PORT, QUERY_PORT)
                .withNetwork(network)
                .withNetworkAliases(COLLECTOR_ALIAS)
                // OTLP receivers are OFF by default in all-in-one; without this the tracer's endpoint is
                // reachable-but-dead and every export is dropped with no visible error.
                .withEnv("COLLECTOR_OTLP_ENABLED", "true")
                // Wait on the query API answering, not on the UI root and not on a log line: /api/services is
                // exactly what the span steps call, so the wait is on the real dependency. A container that is
                // up but not yet serving queries would otherwise hand scenarios a connection error that reads
                // like a product defect.
                .waitingFor(Wait.forHttp("/api/services")
                        .forPort(QUERY_PORT)
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofMinutes(3)));
        collector.withLogConsumer(new JclLogConsumer(logger));
    }

    public void start() {
        logger.info("Starting OTLP collector...");
        collector.start();
        logger.info("OTLP collector up. Query API at " + getQueryBaseUrl());
    }

    public void stop() {
        if (collector.isRunning()) {
            collector.stop();
        }
    }

    public boolean isRunning() {
        return collector.isRunning();
    }

    /**
     * Host alias the block's tracing overlay must set as
     * {@code apim.open_telemetry.remote_tracer.hostname} — not {@code localhost}, which inside the APIM
     * container is APIM itself.
     */
    public String getHostAlias() {
        return COLLECTOR_ALIAS;
    }

    /** OTLP gRPC port the overlay must set as {@code apim.open_telemetry.remote_tracer.port}. */
    public int getOtlpGrpcPort() {
        return OTLP_GRPC_PORT;
    }

    /**
     * Jaeger query API base URL as seen FROM THE HOST — this is what every span assertion reads. The test JVM
     * runs on the host, not inside the block network, so the collector's mapped port is required here; the
     * in-network alias would not resolve. The testcontainers-resolved host is used rather than a
     * {@code localhost} literal, so this keeps working under a remote or rootless daemon.
     */
    public String getQueryBaseUrl() {
        return "http://" + collector.getHost() + ":" + collector.getMappedPort(QUERY_PORT);
    }
}
