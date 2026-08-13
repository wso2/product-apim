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
 * Runner for gateway/rest_artifacts — retrieval of a deployed API's synapse artifacts via the gateway internal
 * REST API (the port of GatewayRestAPITestCase). Runs in the Gateway block (a deployed API + the gateway REST API).
 * Self-contained {@code @cleanup} scenarios.
 */
@CucumberOptions(
        features = "src/test/resources/features/gateway/rest_artifacts.feature",
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/gateway-rest-artifacts.html"}
)
public class GatewayRestArtifactsRunner extends BaseBlockRunner {
}
