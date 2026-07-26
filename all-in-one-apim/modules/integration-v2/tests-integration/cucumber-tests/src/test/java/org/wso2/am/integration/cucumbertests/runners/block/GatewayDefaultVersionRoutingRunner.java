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
 * Runner for gateway default-version routing: a versionless context routes to the default version, follows the
 * default from v1 to v2, and returns 404 once no version is default. Runs in the IntegrationV2-Gateway block
 * (backend started); parallel-safe (own uniquely-created API versions/app).
 */
@CucumberOptions(
        features = {"src/test/resources/features/gateway/default_version_routing.feature"},
        glue = {"org.wso2.am.integration.cucumbertests.stepdefinitions"},
        plugin = {"pretty", "html:target/cucumber-report/gateway-default-version-routing.html"}
)
public class GatewayDefaultVersionRoutingRunner extends BaseBlockRunner {
}
