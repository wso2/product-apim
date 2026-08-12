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
 * Runner for the unlimited-tier-disabled capability (the port of the legacy UnlimitedTierDisabledTestCase). Runs
 * in the IntegrationV2-UnlimitedTierDisabled block, whose overlay sets
 * {@code [apim.throttling] enable_unlimited_tier = false}; the scenarios assert the exact tier the product
 * substitutes in place of Unlimited and the 400/900305 refusals of Unlimited at resource, subscription and
 * application level.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/admin/unlimited_tier_disabled.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/admin-unlimited-tier-disabled.html"}
)
public class UnlimitedTierDisabledRunner extends BaseBlockRunner {
}
