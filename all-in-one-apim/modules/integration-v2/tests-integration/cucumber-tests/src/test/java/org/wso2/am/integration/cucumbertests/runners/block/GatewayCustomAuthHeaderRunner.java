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
 * Runner for gateway/custom-auth-header. Belongs to the IntegrationV2-CustomAuthHeaderAndAppSharing block,
 * whose container sets {@code [apim.oauth_config] auth_header = "Test-Custom-Header"} via a feature-specific
 * TOML overlay so the gateway reads the token from that header. Requires the block's backend
 * ({@code initBackend}) for runtime invocation. Each scenario creates and registers its own resources;
 * teardown is the per-scenario {@code @cleanup} hook.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/gateway/custom_auth_header.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/gateway-custom-auth-header.html"}
)
public class GatewayCustomAuthHeaderRunner extends BaseBlockRunner {
}
