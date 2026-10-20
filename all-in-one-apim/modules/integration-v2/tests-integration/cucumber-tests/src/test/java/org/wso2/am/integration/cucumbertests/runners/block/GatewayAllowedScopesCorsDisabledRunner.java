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
 */

package org.wso2.am.integration.cucumbertests.runners.block;

import io.cucumber.testng.CucumberOptions;

/**
 * Reruns the allowed-scopes matrix with gateway-global CORS disabled. This runner has a dedicated container overlay
 * so it does not change the CORS configuration used by any existing runner.
 */
@CucumberOptions(
        features = "src/test/resources/features/gateway/allowed_scopes.feature",
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        tags = "@legacy:AllowedScopesTestWithCorsDisabled",
        plugin = {"pretty", "html:target/cucumber-report/gateway-allowed-scopes-cors-disabled.html"}
)
public class GatewayAllowedScopesCorsDisabledRunner extends BaseBlockRunner {
}
