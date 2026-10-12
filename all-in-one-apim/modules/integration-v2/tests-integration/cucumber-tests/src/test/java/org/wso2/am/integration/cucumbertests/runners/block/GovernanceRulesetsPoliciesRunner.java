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
 * Runner for API Governance ruleset and policy CRUD (the ports of the legacy RulesetMgtTestCase and
 * PolicyMgtTestCase, plus the ruleset-attached integrity rule). One runner runs both features sequentially, which
 * the policy parity scenario requires: it reads the complete ruleset list while the ruleset scenarios mutate it.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/governance/policies.feature",
                "src/test/resources/features/governance/rulesets.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/governance-rulesets-policies.html"}
)
public class GovernanceRulesetsPoliciesRunner extends BaseBlockRunner {
}
