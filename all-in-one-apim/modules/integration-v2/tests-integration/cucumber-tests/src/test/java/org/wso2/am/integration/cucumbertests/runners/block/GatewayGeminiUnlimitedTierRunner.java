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
 * Runner for the AI API arc under a disabled Unlimited tier (ports GeminiAPIUnlimitedTierDisabledTestCase).
 *
 * <p>Needs the {@code [apim.throttling] enable_unlimited_tier = false} overlay and the node backend, which
 * carries the mock Gemini {@code generateContent} route the AI API's endpoint points at.</p>
 *
 * <p>The overlay is server-GLOBAL and NOT inert: it removes {@code Unlimited} from every tier map, so a fixture
 * that names it — including the shared {@code create_apim_test_app.json}, which creates its application on the
 * Unlimited tier — is refused with 400 / 900305. This runner therefore cannot be folded into a default-lane
 * block, and nothing that subscribes on Unlimited may be added to its block.</p>
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/gateway/gemini_unlimited_tier_disabled.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/gateway-gemini-unlimited-tier.html"}
)
public class GatewayGeminiUnlimitedTierRunner extends BaseBlockRunner {
}
