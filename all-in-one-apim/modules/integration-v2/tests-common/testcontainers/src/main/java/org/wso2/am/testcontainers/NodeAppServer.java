package org.wso2.am.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.containers.wait.strategy.Wait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;

public class NodeAppServer {
    private static final Logger logger = LoggerFactory.getLogger(NodeAppServer.class);
    private final GenericContainer<?> container;

    public NodeAppServer() {
        logger.info("Initializing NodeAppServer...");
        container = new GenericContainer<>("node-customer-service")
                .withExposedPorts(8080)
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
}
