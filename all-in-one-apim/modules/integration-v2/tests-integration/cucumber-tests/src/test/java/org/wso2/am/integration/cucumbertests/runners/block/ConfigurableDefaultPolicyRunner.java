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
 * Runner for the configurable-default-throttling-policy capability (the port of the legacy
 * ConfigurableDefaultPolicyTestCase). Shares the IntegrationV2-UnlimitedTierDisabled block with
 * {@link UnlimitedTierDisabledRunner}: the capability is driven by TENANT CONFIGURATION, and every scenario
 * restores the tenant configuration it changed, so the two runners do not perturb each other as long as they
 * never run CONCURRENTLY — which is why the block is pinned to thread-count=1.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/admin/configurable_default_policy.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/admin-configurable-default-policy.html"}
)
public class ConfigurableDefaultPolicyRunner extends BaseBlockRunner {
}
