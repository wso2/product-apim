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

package org.wso2.am.integration.cucumbertests.runners.migration;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.Tenant;
import org.wso2.carbon.automation.engine.context.beans.User;

@CucumberOptions(
        features = "src/test/resources/features/migration/migrated_api_update.feature",
        glue = "org.wso2.am.integration.cucumbertests.stepdefinitions",
        plugin = {"pretty", "html:target/cucumber-report/migrated-api-update.html"}
)

@Test(groups = {"migrationTest"})
public class MigratedAPIUpdateRunner extends AbstractTestNGCucumberTests {
    private String testUserDomain;
    private String testUserKey;

    private void setTestUserDomain(String testUserDomain) {
        this.testUserDomain = testUserDomain;
    }

    private void setTestUserKey(String testUserKey) {
        this.testUserKey = testUserKey;
    }

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        Tenant tenant = Utils.getTenantFromContext(testUserDomain);
        User user  = testUserKey.equals(Constants.ADMIN_USER_KEY)
                ? tenant.getTenantAdmin()
                : tenant.getTenantUser(testUserKey);
        tenant.setContextUser(user);
        TestContext.set(Constants.CURRENT_TENANT, tenant);
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() {
        TestContext.remove(Constants.CURRENT_TENANT);
    }

    @Factory(dataProvider = "userModeDataProvider")
    public static Object[] factory(String tenantDomain, String userKey) {
        MigratedAPIUpdateRunner runner = new MigratedAPIUpdateRunner();
        runner.setTestUserDomain(tenantDomain);
        runner.setTestUserKey(userKey);
        return new Object[]{ runner };
    }

    @DataProvider
    public Object[][] userModeDataProvider() {
        return new Object[][]{
                {Constants.SUPER_TENANT_DOMAIN, Constants.ADMIN_USER_KEY}, // Super tenant admin
                {Constants.SUPER_TENANT_DOMAIN, Constants.USER_KEY}, // Super tenant user
                {Constants.ADPSAMPLE_TENANT_DOMAIN, Constants.ADPSAMPLE_ADMIN_USER_KEY}, // Tenant admin
                {Constants.ADPSAMPLE_TENANT_DOMAIN, Constants.ADPSAMPLE_USER_KEY} // Tenant user
        };
    }
}
