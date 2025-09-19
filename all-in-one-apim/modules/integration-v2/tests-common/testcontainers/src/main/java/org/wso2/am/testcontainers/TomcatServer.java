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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;
import org.wso2.am.integration.test.utils.ModulePathResolver;

import java.time.Duration;

public class TomcatServer {

    private static final Logger logger = LoggerFactory.getLogger(TomcatServer.class);
    private static final String ARTIFACT_DIR = "/src/main/resources/artifacts";
    String callerModuleDir = ModulePathResolver.getModuleDir(TomcatServer .class);

    private final GenericContainer<?> container;

    private TomcatServer() {

        logger.info("Initializing TomcatServer...");
        logger.info("Copying artifacts from: " + callerModuleDir + ARTIFACT_DIR);
        container = new GenericContainer<>("tomcat:9.0-jdk8")
                .withExposedPorts(8080)
                .withCopyFileToContainer(
                        MountableFile.forHostPath(callerModuleDir + ARTIFACT_DIR),
                        "/usr/local/tomcat/webapps/"
                )
                .withNetwork(ContainerNetwork.SHARED_NETWORK)
                .withNetworkAliases("tomcatbackend")
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(10)));

        Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(logger);
        container.withLogConsumer(logConsumer);
        container.start();
        logger.info("TomcatServer successfully initialized");
    }

    private static class InstanceHolder {
        private static final TomcatServer instance = new TomcatServer();
    }

    public static TomcatServer getInstance() {
        return InstanceHolder.instance;
    }

    public String getBaseUrl() {
        return String.format(
                "http://%s:%d/",
                container.getHost(),
                container.getMappedPort(8080)
        );
    }
}
