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
 * Runner for deploying an AI API to a gateway that serves ONE stage. Its block's overlay redefines Default as
 * production-only (see that overlay for why it cannot be co-hosted), and needs the node backend because the
 * subject is asserted by invoking through the gateway.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/gateway/ai_api_non_hybrid_gateway.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/gateway-ai-api-non-hybrid-gateway.html"}
)
public class GatewayAiApiNonHybridGatewayRunner extends BaseBlockRunner {
}
