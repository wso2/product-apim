/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.cucumbertests.runners.publisher;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.*;
import org.wso2.am.integration.cucumbertests.utils.TestUser;
import org.wso2.am.integration.cucumbertests.utils.TestContext;

@CucumberOptions(
        features = "src/test/resources/features/publisher/create_an_api_through_the_publisher_rest_api_test.feature",
        glue = "org.wso2.am.integration.cucumbertests.stepdefinitions",
        plugin = {"pretty", "html:target/cucumber-report/createAPI.html"}
)

public class CreateAPITestRunner extends AbstractTestNGCucumberTests {

    private TestUser testUser;

    private void setTestUser(TestUser testUser) {
        this.testUser = testUser;
    }

    @BeforeClass(alwaysRun = true)
    public void beforeClass() {
        TestContext.set("currentUser", testUser);
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() {
        TestContext.remove("currentUser");
    }

    @Factory(dataProvider = "userModeDataProvider")
    public static Object[] factory(String username, String password, String tenantDomain) {
        CreateAPITestRunner runner = new CreateAPITestRunner();
        runner.setTestUser(new TestUser(username, password, tenantDomain));
        return new Object[]{ runner };
    }

    @DataProvider
    public Object[][] userModeDataProvider() {
        return new Object[][]{
                {"admin", "admin", "carbon.super"}, // Super tenant admin
//                {"user1", "userPassword1", "carbon.super"}, // Super tenant user
//                {"admin1", "adminPassword1", "tenant1.com"},  // Tenant admin
//                {"user2", "userPassword2", "tenant1.com"} // Tenant user
        };
    }
}
