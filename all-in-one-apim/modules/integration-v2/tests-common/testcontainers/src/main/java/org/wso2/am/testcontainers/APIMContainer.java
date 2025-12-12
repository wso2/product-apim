/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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


import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;


public class APIMContainer extends GenericContainer<APIMContainer> {

    private static final Logger logger = LoggerFactory.getLogger(APIMContainer.class);

    private static final AtomicInteger offsetCounter = new AtomicInteger();
    private final int httpPort;
    private final int httpsPort;
    private final int gatewayHttpPort;
    private final int gatewayHttpsPort;

    public APIMContainer(String containerLabel, String deploymentTomlContent) {

        super(System.getProperty("apim.docker.image.name"));

        String apim_db_url = System.getenv(Constants.API_MANAGER_DATABASE_URL);
        String shared_db_url = System.getenv(Constants.SHARED_DATABASE_URL);

        apim_db_url = apim_db_url.replace("&", "&amp;");
        shared_db_url = shared_db_url.replace("&", "&amp;");

        int offset = Constants.DEFAULT_OFFSET;

        // Check if parallel execution is enabled
        if (Boolean.parseBoolean(System.getenv(Constants.APIM_TEST_CONTAINERS_PARALLEL_ENABLED))) {
            // Set unique offset for each container
            offset +=  offsetCounter.getAndIncrement();

            // Change db urls to use different databases for each container
            apim_db_url = apim_db_url.replace(Constants.APIM_DB_NAME,
                    Constants.APIM_DB_NAME + "_" + offsetCounter.get());
            shared_db_url = shared_db_url.replace(Constants.SHARED_DB_NAME,
                    Constants.SHARED_DB_NAME + "_" + offsetCounter.get());
        }

        logger.info("APIM Container Offset: {}", offset);
        logger.info("APIM DB URL: {}", apim_db_url);
        logger.info("SHARED DB URL: {}", shared_db_url);

        httpPort = Constants.HTTP_PORT + offset;
        httpsPort = Constants.HTTPS_PORT + offset;
        gatewayHttpPort = Constants.GATEWAY_HTTP_PORT + offset;
        gatewayHttpsPort = Constants.GATEWAY_HTTPS_PORT + offset;

        // Expose the container ports to the host machine
        addFixedExposedPort(httpPort, httpPort);
        addFixedExposedPort(httpsPort, httpsPort);
        addFixedExposedPort(gatewayHttpPort, gatewayHttpPort);
        addFixedExposedPort(gatewayHttpsPort, gatewayHttpsPort);

        // Env vars for APIMGT_DB
        withEnv(Constants.API_MANAGER_DATABASE_TYPE, System.getenv(Constants.API_MANAGER_DATABASE_TYPE));
        withEnv(Constants.API_MANAGER_DATABASE_DRIVER, System.getenv(Constants.API_MANAGER_DATABASE_DRIVER));
        withEnv(Constants.API_MANAGER_DATABASE_URL, apim_db_url);
        withEnv(Constants.API_MANAGER_DATABASE_USERNAME, System.getenv(Constants.API_MANAGER_DATABASE_USERNAME));
        withEnv(Constants.API_MANAGER_DATABASE_PASSWORD, System.getenv(Constants.API_MANAGER_DATABASE_PASSWORD));
        withEnv(Constants.API_MANAGER_DATABASE_VALIDATION_QUERY, System.getenv(Constants.
                API_MANAGER_DATABASE_VALIDATION_QUERY));

        // Env vars for SHARED_DB
        withEnv(Constants.SHARED_DATABASE_TYPE, System.getenv(Constants.SHARED_DATABASE_TYPE));
        withEnv(Constants.SHARED_DATABASE_DRIVER, System.getenv(Constants.SHARED_DATABASE_DRIVER));
        withEnv(Constants.SHARED_DATABASE_URL, shared_db_url);
        withEnv(Constants.SHARED_DATABASE_USERNAME, System.getenv(Constants.SHARED_DATABASE_USERNAME));
        withEnv(Constants.SHARED_DATABASE_PASSWORD, System.getenv(Constants.SHARED_DATABASE_PASSWORD));
        withEnv(Constants.SHARED_DATABASE_VALIDATION_QUERY, System.getenv(Constants.
                SHARED_DATABASE_VALIDATION_QUERY));

        withNetwork(ContainerNetwork.SHARED_NETWORK);
        // Copy the modified deployment.toml to the container
        withCopyToContainer(Transferable.of(deploymentTomlContent), getContainerTomlPath());

        // === Inject custom truststore and keystore into APIM container ===

        // Local file paths (coming from surefire system properties)
        String localTruststorePath = System.getProperty("apim.truststore.path");
        String truststorePassword = System.getProperty("apim.truststore.password");

        String localKeystorePath = System.getProperty("apim.keystore.path");
        String keystorePassword = System.getProperty("apim.keystore.password");

        logger.info("truststore path from system property: {}", localTruststorePath);
        logger.info("truststore password from system property: {}", truststorePassword != null ? "***" : "null");
        logger.info("Keystore path from system property: {}", localKeystorePath);
        logger.info("Keystore password from system property: {}", keystorePassword != null ? "***" : "null");
        // Container destination paths
        String containerBase =
                "/home/wso2carbon/" + System.getProperty("apim.server.name") + "/repository/resources/security/";

        String containerTruststorePath = containerBase + "client-truststore.jks";
        String containerKeystorePath = containerBase + "wso2carbon.jks";

        // Build JVM arguments for SSL configuration
        StringBuilder javaOptsBuilder = new StringBuilder();

        // Copy truststore (if provided)
        if (localTruststorePath != null && !localTruststorePath.isEmpty() && Files.exists(Paths.get(localTruststorePath))) {
            withCopyFileToContainer(
                    MountableFile.forHostPath(localTruststorePath),
                    containerTruststorePath
            );
            logger.info("Copied custom truststore to container: {} -> {}", localTruststorePath, containerTruststorePath);

            if (truststorePassword != null && !truststorePassword.isEmpty()) {
                if (javaOptsBuilder.length() > 0) {
                    javaOptsBuilder.append(" ");
                }
                javaOptsBuilder.append("-Djavax.net.ssl.trustStore=").append(containerTruststorePath)
                        .append(" -Djavax.net.ssl.trustStorePassword=").append(truststorePassword);
            }
        }

        // Copy keystore (if provided)
        if (localKeystorePath != null && !localKeystorePath.isEmpty() && Files.exists(Paths.get(localKeystorePath))) {
            withCopyFileToContainer(
                    MountableFile.forHostPath(localKeystorePath),
                    containerKeystorePath
            );
            logger.info("Copied custom keystore to container: {} -> {}", localKeystorePath, containerKeystorePath);

            if (keystorePassword != null && !keystorePassword.isEmpty()) {
                if (javaOptsBuilder.length() > 0) {
                    javaOptsBuilder.append(" ");
                }
                javaOptsBuilder.append("-Djavax.net.ssl.keyStore=").append(containerKeystorePath)
                        .append(" -Djavax.net.ssl.keyStorePassword=").append(keystorePassword);
            }
        }

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
            // Expose debug port
            addFixedExposedPort(debugPort, debugPort);
            withCommand("-DportOffset=" + offset, "-debug", debugPortStr);
        } else {
            withCommand("-DportOffset=" + offset);
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

    public String getAPIManagerUrl() {
        return String.format("https://%s:%d/", getHost(), httpsPort);
    }

    public String getGatewayUrl() {
        return String.format("https://%s:%d/", getHost(), gatewayHttpsPort);
    }

    public String getContainerTomlPath() {
        return Constants.APIM_CONTAINER_USER_HOME + "/" + System.getProperty("apim.server.name") +
                Constants.DEPLOYMENT_TOML_PATH;
    }
}
