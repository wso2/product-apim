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
import org.testcontainers.containers.wait.strategy.Wait;
import org.wso2.am.integration.test.Constants;

import java.time.Duration;


public abstract class BaseAPIMContainer extends GenericContainer<BaseAPIMContainer> {

    private static final int HTTPS_PORT = 9443 ;
    private static final int HTTP_PORT = 9763 ;
    private static final int GATEWAY_HTTPS_PORT = 8243 ;
    private static final int GATEWAY_HTTP_PORT = 8280 ;

    public BaseAPIMContainer() {
        super(System.getProperty("apim.docker.image.name"));
        withExposedPorts(HTTPS_PORT, HTTP_PORT,GATEWAY_HTTP_PORT,GATEWAY_HTTPS_PORT);
        withNetwork(ContainerNetwork.SHARED_NETWORK);

        // Env vars for APIMGT_DB
        withEnv(Constants.API_MANAGER_DATABASE_TYPE, System.getenv(Constants.API_MANAGER_DATABASE_TYPE));
        withEnv(Constants.API_MANAGER_DATABASE_DRIVER, System.getenv(Constants.API_MANAGER_DATABASE_DRIVER));
        withEnv(Constants.API_MANAGER_DATABASE_URL, System.getenv(Constants.API_MANAGER_DATABASE_URL));
        withEnv(Constants.API_MANAGER_DATABASE_USERNAME, System.getenv(Constants.API_MANAGER_DATABASE_USERNAME));
        withEnv(Constants.API_MANAGER_DATABASE_PASSWORD, System.getenv(Constants.API_MANAGER_DATABASE_PASSWORD));
        withEnv(Constants.API_MANAGER_DATABASE_VALIDATION_QUERY, System.getenv(Constants.
                API_MANAGER_DATABASE_VALIDATION_QUERY));

        // Env vars for SHARED_DB
        withEnv(Constants.SHARED_DATABASE_TYPE, System.getenv(Constants.SHARED_DATABASE_TYPE));
        withEnv(Constants.SHARED_DATABASE_DRIVER, System.getenv(Constants.SHARED_DATABASE_DRIVER));
        withEnv(Constants.SHARED_DATABASE_URL, System.getenv(Constants.SHARED_DATABASE_URL));
        withEnv(Constants.SHARED_DATABASE_USERNAME, System.getenv(Constants.SHARED_DATABASE_USERNAME));
        withEnv(Constants.SHARED_DATABASE_PASSWORD, System.getenv(Constants.SHARED_DATABASE_PASSWORD));
        withEnv(Constants.SHARED_DATABASE_VALIDATION_QUERY, System.getenv(Constants.
                SHARED_DATABASE_VALIDATION_QUERY));

        waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(20)));
    }

    public String getAPIManagerUrl() {
        return String.format("https://%s:%d/", getHost(), getMappedPort(HTTPS_PORT));
    }

    public String getGatewayUrl() {
        return String.format("https://%s:%d/", getHost(), getMappedPort(GATEWAY_HTTPS_PORT ));
    }
}
