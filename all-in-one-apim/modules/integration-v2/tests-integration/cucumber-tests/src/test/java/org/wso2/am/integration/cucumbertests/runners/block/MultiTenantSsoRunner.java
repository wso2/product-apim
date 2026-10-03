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
 * Multi-tenant console SSO: a tenant's user authenticates at an external Identity Server through the
 * multi-tenant broker and lands in the Publisher as that tenant's principal.
 *
 * <p>The setup feature runs first (its {@code _setup_} prefix sorts ahead) and provisions the broker topology
 * the journey needs; both share this runner's context, so the journey resolves the identifiers the setup stored.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/admin/_setup_multitenant_sso.feature",
                "src/test/resources/features/admin/multitenant_sso.feature"
        },
        glue = {"org.wso2.am.integration.cucumbertests.stepdefinitions"},
        plugin = {"pretty", "html:target/cucumber-report/multitenant-sso.html"}
)
public class MultiTenantSsoRunner extends BaseBlockRunner {
}
