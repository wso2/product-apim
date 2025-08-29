package org.wso2.am.testcontainers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.MountableFile;
import org.wso2.am.integration.test.Constants;

public class CustomAPIMContainer extends BaseAPIMContainer {
    private static final Logger logger = LoggerFactory.getLogger(CustomAPIMContainer.class);

    public CustomAPIMContainer(String containerLabel, String deploymentTomlPath) {
        super();
        withCopyFileToContainer(
                MountableFile.forHostPath(deploymentTomlPath),
                Constants.TOML_PATH
        );
        this.withLogConsumer(new Slf4jLogConsumer(logger).withPrefix(containerLabel));
    }
}