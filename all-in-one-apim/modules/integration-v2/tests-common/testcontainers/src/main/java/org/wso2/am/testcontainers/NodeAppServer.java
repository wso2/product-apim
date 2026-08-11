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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.Collections;

public class NodeAppServer {

    private static final Log logger = LogFactory.getLog(NodeAppServer.class);

    /** Network alias APIM gateways use to reach the sample backend apps on whichever network it is attached to. */
    public static final String NETWORK_ALIAS = "nodebackend";

    private final Integer[] exposedPorts = {3000, 3001, 3002, 3003, 3004, 3005, 3006, 3007, 3008, 3009, 3010, 3011,
            3012, 3013, 3014, 3015, 3016, 3017, 3018, 3019};
    private final GenericContainer<?> container;

    public NodeAppServer() {
        logger.info("Initializing NodeAppServer...");
        container = new GenericContainer<>(System.getProperty("node.docker.image.name"))
                //expose the app to the host machine
                .withExposedPorts(exposedPorts)
                .withNetwork(ContainerNetwork.BACKEND_HOME_NETWORK)
                .withNetworkAliases(NETWORK_ALIAS)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)));

        JclLogConsumer logConsumer = new JclLogConsumer(logger);
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

    /**
     * Connects the shared backend to a block's private network under the {@link #NETWORK_ALIAS} alias, so that
     * block's APIM gateway resolves {@code nodebackend} to this singleton. The backend is a stateless upstream
     * (read-only sample apps), so multi-homing one instance onto every block's network is safe and avoids a
     * backend container per block. Idempotent per network: each block attaches its OWN unique network, and
     * {@link #detachFromNetwork(Network)} removes it at block teardown before the network is closed (a network
     * with a connected container cannot be removed).
     */
    public void attachToNetwork(Network network) {
        DockerClientFactory.instance().client().connectToNetworkCmd()
                .withContainerId(container.getContainerId())
                .withNetworkId(network.getId())
                .withContainerNetwork(new com.github.dockerjava.api.model.ContainerNetwork()
                        .withAliases(Collections.singletonList(NETWORK_ALIAS)))
                .exec();
        logger.info("NodeAppServer attached to network " + network.getId() + " as '" + NETWORK_ALIAS + "'");
    }

    /**
     * Disconnects the shared backend from a block's private network at teardown, so the block's
     * {@link Network#close()} (docker network removal) succeeds — Docker refuses to remove a network that still
     * has a container connected. Best-effort: a disconnect failure is logged, not thrown, so it never masks the
     * block's real teardown outcome.
     */
    public void detachFromNetwork(Network network) {
        try {
            DockerClientFactory.instance().client().disconnectFromNetworkCmd()
                    .withContainerId(container.getContainerId())
                    .withNetworkId(network.getId())
                    .exec();
            logger.info("NodeAppServer detached from network " + network.getId());
        } catch (RuntimeException e) {
            logger.warn("NodeAppServer detach from network " + network.getId() + " failed (continuing teardown): "
                    + e.getMessage());
        }
    }
}
