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
 * The Zipkin server APIM exports spans to, as ONE real {@code openzipkin/zipkin} container per block.
 *
 * <p><b>Why a real collector and not a double.</b> The same argument as {@link DynamicOtlpCollector}: Zipkin is
 * the only component in this arc we did not author, so it is the only one that can CONTRADICT us — it parses the
 * v2 JSON payload the exporter POSTs and answers queries about what it actually stored. Every span assertion in
 * the Zipkin leg is made against {@link #getQueryBaseUrl()} ({@code GET /api/v2/traces}), never against anything
 * we built.
 *
 * <p><b>Which transport reaches it.</b> Only the Zipkin v2 JSON path. {@code ZipkinTelemetry} builds the exporter's
 * endpoint as {@code http://<hostname>:<port>/api/v2/spans}, so the block's overlay selects the {@code zipkin}
 * tracer, which reads {@code OpenTelemetry.RemoteTracer.HostName} and {@code .Port} ONLY (it ignores the
 * {@code Protocol} key) and points at this container on {@link #HTTP_API_PORT}.
 *
 * <p><b>Why the container is PER-BLOCK</b> rather than a shared singleton like {@link NodeAppServer}: the traces
 * stored here are scenario-visible state. A shared collector would let a sibling class's spans satisfy this
 * block's queries, and its store would grow without bound across the whole suite.
 *
 * <p><b>Scope (CLAUDE.md 14).</b> Infrastructure only. This class boots a container and publishes URLs. It
 * performs NO product operation — enabling tracing, invoking an API and reading spans back are all feature
 * steps.
 */
public class DynamicZipkin {

    private static final Log logger = LogFactory.getLog(DynamicZipkin.class);

    /** Alias the APIM overlay uses to reach this collector on the block network (ZipkinTelemetry's default host). */
    public static final String ZIPKIN_ALIAS = "zipkin";
    /** The v2 HTTP API ports — the single port Zipkin listens on (spans ingest + query API are the same port). */
    public static final int HTTP_API_PORT = 9411;

    private static final String DEFAULT_IMAGE = "openzipkin/zipkin:latest";

    private final GenericContainer<?> collector;

    /**
     * @param blockLabel owning block's label — log prefix and diagnostics only
     * @param network    the block's private docker network this container joins (the caller owns its lifecycle;
     *                   {@link #stop()} must run before it is closed)
     */
    public DynamicZipkin(String blockLabel, Network network) {

        logger.info("Initializing DynamicZipkin for block '" + blockLabel + "'...");

        collector = new GenericContainer<>(
                System.getProperty("zipkin.docker.image.name", DEFAULT_IMAGE))
                .withExposedPorts(HTTP_API_PORT)
                .withNetwork(network)
                .withNetworkAliases(ZIPKIN_ALIAS)
                // Wait on the query API answering, not on the UI root and not on a log line: /api/v2/services is
                // exactly the kind of call the span steps make, so the wait is on the real dependency. A container
                // that is up but not yet serving queries would otherwise hand scenarios a connection error that
                // reads like a product defect. Zipkin answers 200 even when the service list is empty, so this also
                // gates on the storage layer being ready rather than merely on the HTTP port.
                .waitingFor(Wait.forHttp("/api/v2/services")
                        .forPort(HTTP_API_PORT)
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofMinutes(3)));
        collector.withLogConsumer(new JclLogConsumer(logger));
    }

    public void start() {
        logger.info("Starting Zipkin collector...");
        collector.start();
        logger.info("Zipkin collector up. Query API at " + getQueryBaseUrl());
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
        return ZIPKIN_ALIAS;
    }

    /** The v2 HTTP API port the overlay must set as {@code apim.open_telemetry.remote_tracer.port}. */
    public int getPort() {
        return HTTP_API_PORT;
    }

    /**
     * Zipkin's v2 query API base URL as seen FROM THE HOST — this is what every span assertion reads. The test
     * JVM runs on the host, not inside the block network, so the collector's mapped port is required here; the
     * in-network alias would not resolve. The testcontainers-resolved host is used rather than a
     * {@code localhost} literal, so this keeps working under a remote or rootless daemon.
     */
    public String getQueryBaseUrl() {
        return "http://" + collector.getHost() + ":" + collector.getMappedPort(HTTP_API_PORT);
    }
}