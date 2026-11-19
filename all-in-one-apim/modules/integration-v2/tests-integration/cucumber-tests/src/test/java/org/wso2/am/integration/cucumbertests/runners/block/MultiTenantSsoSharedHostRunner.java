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
 * Multi-tenant console SSO single logout with API Manager and the Identity Server on ONE host ({@code localhost},
 * on ports 9443 and 9444), as a single-machine deployment runs them.
 *
 * <p>Runs the same broker topology and the same single-logout scenario as {@link MultiTenantSsoRunner}; only the
 * Identity Server's host differs. Browser cookies are scoped by host and not by port, so here the two servers'
 * session cookies coincide, whereas in {@link MultiTenantSsoRunner} the Identity Server keeps its own under
 * {@code wso2is}.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/admin/_setup_multitenant_sso.feature",
                "src/test/resources/features/admin/multitenant_sso.feature"
        },
        tags = "@setup or @mt-sso-single-logout",
        glue = {"org.wso2.am.integration.cucumbertests.stepdefinitions"},
        plugin = {"pretty", "html:target/cucumber-report/multitenant-sso-shared-host.html"}
)
public class MultiTenantSsoSharedHostRunner extends BaseBlockRunner {
}
