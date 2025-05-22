package org.wso2.am.testcontainers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;

public class DefaultAPIMContainer extends BaseAPIMContainer {
    private static final Logger logger = LoggerFactory.getLogger(DefaultAPIMContainer.class);

    private DefaultAPIMContainer() {
        logger.info("Initializing DefaultAPIMContainer...");
        this.withLogConsumer(new Slf4jLogConsumer(logger));
        this.start();
        logger.info("DefaultAPIMContainer successfully initialized");
    }

    // This class won't be loaded until getInstance() is called
    private static class InstanceHolder {
        // This initialization is guaranteed to be atomic and thread-safe
        private static final DefaultAPIMContainer instance = new DefaultAPIMContainer();
    }

    public static DefaultAPIMContainer getInstance() {
        return InstanceHolder.instance;
    }
}
