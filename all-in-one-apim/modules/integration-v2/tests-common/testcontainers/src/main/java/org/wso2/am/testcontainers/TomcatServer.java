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
        container = new GenericContainer<>("tomcat:9.0-jdk8")
                .withExposedPorts(8080)
                .withCopyFileToContainer(
                        MountableFile.forHostPath(callerModuleDir+ARTIFACT_DIR),
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
