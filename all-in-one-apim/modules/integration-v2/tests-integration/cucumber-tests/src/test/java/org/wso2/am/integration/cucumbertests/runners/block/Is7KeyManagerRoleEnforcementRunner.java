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
 * Runner for IS7 role-based authorization enforcement. Runs in its own external-KM block
 * (testng-is7km-roleenf.xml) so the scope-protected-API setup registers scopes against a clean tenant (scope
 * registration fans registerScope out to every KM in the tenant, so an unrelated broken KM would fail it). Uses
 * the {@code _setup_} fixture pattern - {@code _setup_is7_role_enforcement}
 * (listed first, provisions the scope-protected API / application / IS users once) then
 * {@code is7_role_enforcement} (the with-role -> 200 and without-role -> 403 scenarios, reusing that fixture). The
 * fixture is torn down once by {@link BaseBlockRunner}'s AfterClass sweep.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/admin/_setup_is7_role_enforcement.feature",
                "src/test/resources/features/admin/is7_role_enforcement.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/is7-role-enforcement.html"}
)
public class Is7KeyManagerRoleEnforcementRunner extends BaseBlockRunner {
}
