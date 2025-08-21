package org.wso2.am.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.containers.wait.strategy.Wait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class NodeAppServer {
    private static final Logger logger = LoggerFactory.getLogger(NodeAppServer.class);
    private final Integer[] exposedPorts = {3000, 3001, 3002, 3003, 3004, 3005, 3006, 3007, 3008, 3009, 3010, 3011, 3012, 3013, 3014, 3015, 3016, 3017};
    private final GenericContainer<?> container;

    public NodeAppServer() {
        logger.info("Initializing NodeAppServer...");
        container = new GenericContainer<>("node-customer-service")
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
}
