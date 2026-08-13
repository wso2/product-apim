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
 * Runner for gateway/audience_validation — gateway-plane JWT audience (aud) claim enforcement (the port of the API
 * half of AudienceValidationTestCase). Runs in the Gateway block (backend up, so a deployed API is invocable).
 * Self-contained {@code @cleanup} scenarios.
 */
@CucumberOptions(
        features = "src/test/resources/features/gateway/audience_validation.feature",
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/gateway-audience-validation.html"}
)
public class GatewayAudienceValidationRunner extends BaseBlockRunner {
}
