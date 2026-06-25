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

package org.wso2.am.integration.cucumbertests.verification;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.Tenant;
import org.wso2.carbon.automation.engine.context.beans.User;

/**
 * Phase 1.3 verification runner: drives a single end-to-end API lifecycle (DCR -> token -> create ->
 * deploy -> publish -> subscribe -> token -> invoke) through a {@link
 * org.wso2.am.testcontainers.DynamicApimContainer} on dynamic/ephemeral host ports. Unlike the
 * legacy runners this is not a {@code @Factory} over user modes — it runs only as super-tenant admin,
 * which is sufficient to prove the dynamic-port flows work. Run via {@code testng-fv-1.3.xml}.
 */
@CucumberOptions(
        features = "src/test/resources/features/framework-verification/dynamic_lifecycle.feature",
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions",
                "org.wso2.am.integration.cucumbertests.verification.steps"
        },
        plugin = {"pretty", "html:target/cucumber-report/dynamic-lifecycle.html"}
)
public class DynamicLifecycleVerificationRunner extends AbstractTestNGCucumberTests {

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        // Publish the super tenant under its domain key — the same shape TenantUserProvisioner uses — so
        // Identity.resolveActor(null) finds the default actor (super-tenant admin). No CURRENT_TENANT slot.
        Tenant superTenant = new Tenant();
        superTenant.setDomain(Constants.SUPER_TENANT_DOMAIN);
        User admin = new User();
        admin.setKey(Constants.ADMIN_USER_KEY);
        admin.setUserName(Constants.SUPER_TENANT_ADMIN_USERNAME);
        admin.setPassword(Constants.SUPER_TENANT_ADMIN_PASSWORD);
        superTenant.setTenantAdmin(admin);
        TestContext.set(Constants.SUPER_TENANT_DOMAIN, superTenant);
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() {
        TestContext.remove(Constants.SUPER_TENANT_DOMAIN);
    }
}
