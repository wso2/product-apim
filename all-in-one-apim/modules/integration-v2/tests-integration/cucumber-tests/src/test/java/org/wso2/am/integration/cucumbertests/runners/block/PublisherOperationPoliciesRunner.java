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
 * Runner for publisher/operation-policies. Each scenario creates and tears down its own resources (base API,
 * common policy, API-specific policy), so no shared {@code _setup_*} fixture is needed; teardown is the
 * per-scenario {@code @cleanup} hook, complemented by the common-policy registration swept in
 * {@code BaseBlockRunner}'s {@code @AfterClass}.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/publisher/operation_policies.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/publisher-operation-policies.html"}
)
public class PublisherOperationPoliciesRunner extends BaseBlockRunner {
}
