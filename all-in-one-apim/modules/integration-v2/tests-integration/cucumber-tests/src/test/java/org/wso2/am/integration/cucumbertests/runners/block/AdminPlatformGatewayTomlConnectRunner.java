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
 * Runner for TOML-declared ("connect-with-token") platform gateways — needs its own container because the
 * [[apim.platform_gateway.connect]] entries must be present in deployment.toml at boot (see the
 * IntegrationV2-PlatformGatewayTomlConnect block's tomlExtraOverlayPath in testng-v2.xml).
 */
@CucumberOptions(
        features = {"src/test/resources/features/admin/platform_gateway_toml_connect.feature"},
        glue = {"org.wso2.am.integration.cucumbertests.stepdefinitions"},
        plugin = {"pretty", "html:target/cucumber-report/admin-platform-gateway-toml-connect.html"}
)
public class AdminPlatformGatewayTomlConnectRunner extends BaseBlockRunner {
}
