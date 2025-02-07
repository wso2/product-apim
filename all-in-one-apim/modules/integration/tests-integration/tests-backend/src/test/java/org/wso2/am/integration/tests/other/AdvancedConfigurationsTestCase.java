/*
 * Copyright (c) 2021, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.am.integration.tests.other;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.test.helpers.AdminApiTestHelper;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.io.File;

public class AdvancedConfigurationsTestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(AdvancedConfigurationsTestCase.class);
    private AdminApiTestHelper adminApiTestHelper;

    @Factory(dataProvider = "userModeDataProvider")
    public AdvancedConfigurationsTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN },};
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        adminApiTestHelper = new AdminApiTestHelper();
    }

    @Test(groups = { "wso2.am" }, description = "Test Get Tenant Configuration")
    public void testGetTenantConfiguration() throws Exception {
        String tenantConfigBeforeTestCase = restAPIAdmin.getTenantConfig().toString();
        Assert.assertNotNull(tenantConfigBeforeTestCase);
    }

    @Test(groups = { "wso2.am" }, description = "Test add Tenant Configuration", dependsOnMethods = "testGetTenantConfiguration")
    public void testUpdateTenantConfiguration() throws Exception {
        String tenantConfContent = FileUtils.readFileToString(new File(getAMResourceLocation() + File.separator
                + "configFiles" + File.separator + "tenantConf" + File.separator + "tenant-conf.json"), "UTF-8");
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(tenantConfContent);
        restAPIAdmin.updateTenantConfig(jsonObject);
        String updatedConfContent = restAPIAdmin.getTenantConfig().toString();
        Assert.assertNotNull(updatedConfContent);
    }

    @Test(groups = { "wso2.am" }, description = "Test Get Tenant Schema Configuration", dependsOnMethods = "testGetTenantConfiguration")
    public void testGetTenantConfigurationSchema() throws Exception {
        String tenantConfigSchema = restAPIAdmin.getTenantConfigSchema().toString();
        Assert.assertNotNull(tenantConfigSchema);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
    }
}
