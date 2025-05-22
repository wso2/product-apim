package org.wso2.am.testcontainers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.MountableFile;

public class CustomAPIMContainer extends BaseAPIMContainer {
    private static final Logger logger = LoggerFactory.getLogger(CustomAPIMContainer.class);

    public CustomAPIMContainer(String containerLabel, String deploymentTomlPath) {
        super();
        withCopyFileToContainer(
                MountableFile.forHostPath(deploymentTomlPath),
                TOML_PATH
        );
        this.withLogConsumer(new Slf4jLogConsumer(logger).withPrefix(containerLabel));
    }
}