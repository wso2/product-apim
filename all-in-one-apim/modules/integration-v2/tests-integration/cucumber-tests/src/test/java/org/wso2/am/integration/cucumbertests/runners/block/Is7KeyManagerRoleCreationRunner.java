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
 * Runner for the runtime role-creation feature. Runs in the core external-KM block (IntegrationV2-Is7KeyManager
 * in testng-v2.xml); the {@code _setup_is7_key_manager} fixture registers the WSO2-IS-7 KM with
 * enable_roles_creation=true, so registering a shared scope bound to a new role creates the role in IS. Proves
 * the created role exists via IS's SCIM2 Roles API. Extends
 * {@link BaseBlockRunner} for the block boot-failure guard and the runner-scoped cleanup safety net.
 */
@CucumberOptions(
        features = {"src/test/resources/features/admin/_setup_is7_key_manager.feature",
            "src/test/resources/features/admin/is7_role_creation.feature"},
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/is7-role-creation.html"}
)
public class Is7KeyManagerRoleCreationRunner extends BaseBlockRunner {
}
