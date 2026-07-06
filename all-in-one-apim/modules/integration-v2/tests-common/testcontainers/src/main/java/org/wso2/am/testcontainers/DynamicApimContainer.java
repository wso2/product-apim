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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;
import org.wso2.am.integration.test.utils.Constants;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

/**
 * APIM container variant for the parallel-on-shared lane. Unlike {@link APIMContainer}, this
 * runs with {@code portOffset=0} and exposes the canonical ports via {@code withExposedPorts},
 * letting Docker map each to an ephemeral host port. Callers resolve the host port through the
 * {@code get*Url()} accessors. Because every container has its own network namespace, no offset
 * counter or per-container DB renaming is needed.
 */
public class DynamicApimContainer extends GenericContainer<DynamicApimContainer> {

    private static final Logger logger = LoggerFactory.getLogger(DynamicApimContainer.class);
    private static final String DEFAULT_APIM_IMAGE = "wso2am:4.7.0-SNAPSHOT-jdk21";

    public DynamicApimContainer(String containerLabel, String deploymentTomlContent) {

        super(System.getProperty("apim.docker.image.name", DEFAULT_APIM_IMAGE));

        String apimDbUrl = System.getenv(Constants.API_MANAGER_DATABASE_URL).replace("&", "&amp;");
        String sharedDbUrl = System.getenv(Constants.SHARED_DATABASE_URL).replace("&", "&amp;");

        logger.info("APIM DB URL: {}", apimDbUrl);
        logger.info("SHARED DB URL: {}", sharedDbUrl);

        // Expose canonical ports; Docker assigns ephemeral host ports resolved via getMappedPort.
        withExposedPorts(Constants.HTTPS_PORT, Constants.HTTP_PORT,
                Constants.GATEWAY_HTTPS_PORT, Constants.GATEWAY_HTTP_PORT);

        // Env vars for APIMGT_DB
        withEnv(Constants.API_MANAGER_DATABASE_TYPE, System.getenv(Constants.API_MANAGER_DATABASE_TYPE));
        withEnv(Constants.API_MANAGER_DATABASE_DRIVER, System.getenv(Constants.API_MANAGER_DATABASE_DRIVER));
        withEnv(Constants.API_MANAGER_DATABASE_URL, apimDbUrl);
        withEnv(Constants.API_MANAGER_DATABASE_USERNAME, System.getenv(Constants.API_MANAGER_DATABASE_USERNAME));
        withEnv(Constants.API_MANAGER_DATABASE_PASSWORD, System.getenv(Constants.API_MANAGER_DATABASE_PASSWORD));
        withEnv(Constants.API_MANAGER_DATABASE_VALIDATION_QUERY, System.getenv(Constants.
                API_MANAGER_DATABASE_VALIDATION_QUERY));

        // Env vars for SHARED_DB
        withEnv(Constants.SHARED_DATABASE_TYPE, System.getenv(Constants.SHARED_DATABASE_TYPE));
        withEnv(Constants.SHARED_DATABASE_DRIVER, System.getenv(Constants.SHARED_DATABASE_DRIVER));
        withEnv(Constants.SHARED_DATABASE_URL, sharedDbUrl);
        withEnv(Constants.SHARED_DATABASE_USERNAME, System.getenv(Constants.SHARED_DATABASE_USERNAME));
        withEnv(Constants.SHARED_DATABASE_PASSWORD, System.getenv(Constants.SHARED_DATABASE_PASSWORD));
        withEnv(Constants.SHARED_DATABASE_VALIDATION_QUERY, System.getenv(Constants.
                SHARED_DATABASE_VALIDATION_QUERY));

        // Add host.docker.internal mapping for Linux compatibility (needed for accessing host services)
        withExtraHost("host.docker.internal", "host-gateway");

        withNetwork(ContainerNetwork.SHARED_NETWORK);
        // Copy the modified deployment.toml to the container
        withCopyToContainer(Transferable.of(deploymentTomlContent), getContainerTomlPath());

        if (Boolean.parseBoolean(System.getProperty(Constants.APIM_DEBUG_ENABLED))) {
            String debugPortStr = System.getProperty(Constants.APIM_DEBUG_PORT);
            int debugPort;
            try {
                debugPort = Integer.parseInt(debugPortStr);
            } catch (NumberFormatException ignored) {
                debugPort = Constants.DEFAULT_APIM_DEBUG_PORT;
                if (!debugPortStr.contains(String.valueOf(debugPort))) {
                    throw new RuntimeException("Invalid APIM_DEBUG_PORT. APIM debug port must be " + debugPort);
                }
            }
            logger.info("Debugging enabled for the API Manager Instance on port {}", debugPortStr);
            // Debug uses a fixed host port so the IDE can attach to a known address.
            addFixedExposedPort(debugPort, debugPort);
            withCommand("-DportOffset=0", "-debug", debugPortStr);
        } else {
            withCommand("-DportOffset=0");
        }

        String testName = MDC.get("testName") != null ? MDC.get("testName") : "default";
        // send container logs with MDC fields
        Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(logger)
                .withPrefix(containerLabel)
                .withSeparateOutputStreams()
                .withMdc("testName", testName);

        withLogConsumer(logConsumer);
        waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(20)));
    }

    /**
     * Enables JaCoCo integration coverage (opt-in; the caller decides when to attach it). Copies the JaCoCo
     * runtime agent into the container, attaches it to the server JVM via {@code JAVA_TOOL_OPTIONS} in
     * tcpserver mode, and exposes the agent port so counters can be dumped over a mapped host port at
     * teardown (see {@link JacocoCoverage}). Must be called before {@link #start()}.
     */
    public DynamicApimContainer withCoverage() {
        try {
            File agentJar = JacocoCoverage.extractAgentJar();
            withCopyToContainer(MountableFile.forHostPath(agentJar.getAbsolutePath()),
                    JacocoCoverage.CONTAINER_AGENT_PATH);
        } catch (IOException e) {
            throw new RuntimeException("Failed to stage JaCoCo agent jar", e);
        }
        // JAVA_TOOL_OPTIONS is honoured by the JVM itself, so no need to touch the WSO2 startup script.
        withEnv("JAVA_TOOL_OPTIONS", JacocoCoverage.containerAgentVmArg());
        // addExposedPort (not withExposedPorts) so the canonical APIM ports set in the constructor are kept.
        addExposedPort(JacocoCoverage.TCP_PORT);
        logger.info("JaCoCo coverage enabled: agent at {}, tcpserver port {}",
                JacocoCoverage.CONTAINER_AGENT_PATH, JacocoCoverage.TCP_PORT);
        return this;
    }

    /** Host for dumping coverage (valid after start). */
    public String getCoverageDumpHost() {
        return getHost();
    }

    /** Ephemeral host port mapped to the in-container JaCoCo agent tcpserver (valid after start). */
    public int getCoverageDumpPort() {
        return getMappedPort(JacocoCoverage.TCP_PORT);
    }

    public String getServletHttpsUrl() {
        return String.format("https://%s:%d/", getHost(), getMappedPort(Constants.HTTPS_PORT));
    }

    public String getServletHttpUrl() {
        return String.format("http://%s:%d/", getHost(), getMappedPort(Constants.HTTP_PORT));
    }

    public String getGatewayHttpsUrl() {
        return String.format("https://%s:%d/", getHost(), getMappedPort(Constants.GATEWAY_HTTPS_PORT));
    }

    public String getGatewayHttpUrl() {
        return String.format("http://%s:%d/", getHost(), getMappedPort(Constants.GATEWAY_HTTP_PORT));
    }

    public String getContainerTomlPath() {
        return Constants.APIM_CONTAINER_USER_HOME + "/" + System.getProperty("apim.server.name") +
                Constants.DEPLOYMENT_TOML_PATH;
    }
}
