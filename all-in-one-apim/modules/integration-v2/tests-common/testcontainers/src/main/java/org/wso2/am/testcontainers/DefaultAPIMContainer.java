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
