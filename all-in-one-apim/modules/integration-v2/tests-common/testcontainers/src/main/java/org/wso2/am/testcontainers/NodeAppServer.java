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

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class NodeAppServer {

    private static final Logger logger = LoggerFactory.getLogger(NodeAppServer.class);
    private final Integer[] exposedPorts = {3000, 3001, 3002, 3003, 3004, 3005, 3006, 3007, 3008, 3009, 3010, 3011,
            3012, 3013, 3014, 3015, 3016, 3017};
    private final GenericContainer<?> container;

    public NodeAppServer() {
        logger.info("Initializing NodeAppServer...");
        container = new GenericContainer<>(System.getProperty("node.docker.image.name"))
                //expose the app to the host machine
                .withExposedPorts(exposedPorts)
                .withNetwork(ContainerNetwork.SHARED_NETWORK)
                .withNetworkAliases("nodebackend")
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)));

        Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(logger);
        container.withLogConsumer(logConsumer);
        container.start();
        logger.info("NodeAppServer successfully initialized");
    }

    private static class InstanceHolder {
        private static final NodeAppServer instance = new NodeAppServer();
    }

    public static NodeAppServer getInstance() {
        return NodeAppServer.InstanceHolder.instance;
    }

    public void restart() {
        logger.info("Restarting NodeAppServer...");
        try {
            if (container.isRunning()) {
                container.stop();
            }
        } catch (Exception e) {
            logger.warn("Error stopping container, continuing with start: {}", e.getMessage());
        }
        container.start();
        logger.info("NodeAppServer successfully restarted");
    }
}
