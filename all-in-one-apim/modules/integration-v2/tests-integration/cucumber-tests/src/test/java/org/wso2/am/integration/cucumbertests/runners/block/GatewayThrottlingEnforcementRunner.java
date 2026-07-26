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
 * Runner for gateway runtime throttling enforcement: an application on a low request-count policy is refused
 * with 429 once it exceeds the limit. Runs in the IntegrationV2-Gateway block (which starts the backend); the
 * throttle counter is per-application, so it is unaffected by sibling classes sharing the container.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/gateway/throttling_enforcement.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/gateway-throttling-enforcement.html"}
)
public class GatewayThrottlingEnforcementRunner extends BaseBlockRunner {
}
