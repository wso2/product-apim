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
import org.testcontainers.utility.MountableFile;
import org.wso2.am.integration.test.Constants;

public class CustomAPIMContainer extends BaseAPIMContainer {
    private static final Logger logger = LoggerFactory.getLogger(CustomAPIMContainer.class);

    public CustomAPIMContainer(String containerLabel, String deploymentTomlPath) {
        super();
        withCopyFileToContainer(MountableFile.forHostPath(deploymentTomlPath), getContainerTomlPath());
        this.withLogConsumer(new Slf4jLogConsumer(logger).withPrefix(containerLabel));
    }

    public String getContainerTomlPath() {
        return Constants.APIM_CONTAINER_USER_HOME + "/" + System.getProperty("apim.server.name") +
                Constants.DEPLOYMENT_TOML_PATH;
    }
}
