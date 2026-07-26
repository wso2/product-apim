/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.cucumbertests.runners.block;

import io.cucumber.testng.CucumberOptions;

/**
 * Runner for backend-JWT claim generation (the port of JWTTestCase). Runs in the IntegrationV2-ApplicationAttributes
 * block, which already enables backend JWT ([apim.jwt] enable) via its overlay and starts the node backend
 * (the /reflect-headers route echoes the gateway-injected X-JWT-Assertion). The per-scenario cleanup hook tears
 * down the API/app.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/key-manager/backend_jwt.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/keymanager-backend-jwt.html"}
)
public class KeyManagerBackendJwtRunner extends BaseBlockRunner {
}
