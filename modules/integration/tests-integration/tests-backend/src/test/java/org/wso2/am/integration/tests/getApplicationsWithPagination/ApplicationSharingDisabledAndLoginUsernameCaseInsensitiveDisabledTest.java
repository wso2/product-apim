/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.getApplicationsWithPagination;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.testng.annotations.AfterClass;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationListDTO;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.UserManagementUtils;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.ServerConstants;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;

/**
 * This will test fetching applications in the developer portal with ApplicationSharing disabled and
 * LoginUsernameCaseInsensitive disabled
 */
public class ApplicationSharingDisabledAndLoginUsernameCaseInsensitiveDisabledTest extends APIMIntegrationBaseTest {

    private ServerConfigurationManager serverConfigurationManager;
    private static final String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
    private static final String testPassword = "test_password";
    private static final String firstName = "first_name";
    private static final String orgTestUser = "org_test_user_1";
    private static final String nonOrgTestUser = "non_org_test_user_1";
    private static final String testOrganization = "test_organization";
    private static final String testEmail = "test@wso2.com";

    @Factory(dataProvider = "userModeDataProvider")
    public ApplicationSharingDisabledAndLoginUsernameCaseInsensitiveDisabledTest(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN } };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        if (userMode == TestUserMode.SUPER_TENANT_ADMIN) {
            AutomationContext superTenantKeyManagerContext = new AutomationContext(
                    APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME, APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE,
                    TestUserMode.SUPER_TENANT_ADMIN);
            serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);
            serverConfigurationManager.applyConfiguration(new File(
                            getAMResourceLocation() + File.separator + "configFiles" + File.separator
                                    + "getApplicationsWithPagination" + File.separator
                                    + "disable_application_sharing_disable_login_username_case_insensitive_deployment.toml"),
                    new File(carbonHome + File.separator + "repository" + File.separator + "conf" + File.separator
                            + "deployment.toml"));
            super.init(userMode);
            UserManagementUtils.signupUser(orgTestUser, testPassword, firstName, testOrganization, testEmail,
                    keyManagerContext.getContextTenant().getDomain());
            UserManagementUtils.signupUser(nonOrgTestUser, testPassword, firstName, "", testEmail,
                    keyManagerContext.getContextTenant().getDomain());
        } else if (userMode == TestUserMode.TENANT_ADMIN) {
            UserManagementUtils.signupUser(orgTestUser + "@wso2.com", testPassword, firstName, testOrganization,
                    testEmail, keyManagerContext.getContextTenant().getDomain());
            UserManagementUtils.signupUser(nonOrgTestUser + "@wso2.com", testPassword, firstName, testOrganization,
                    testEmail, keyManagerContext.getContextTenant().getDomain());
            userManagementClient.updateRolesOfUser(orgTestUser + "@wso2.com",
                    new String[] { APIMIntegrationConstants.APIM_INTERNAL_ROLE.SUBSCRIBER });
            userManagementClient.updateRolesOfUser(nonOrgTestUser + "@wso2.com",
                    new String[] { APIMIntegrationConstants.APIM_INTERNAL_ROLE.SUBSCRIBER });
        }
    }

    @Test(groups = { "wso2.am" }, description = "Check getting applications with pagination for user having organization"
            + " with ApplicationSharing disabled and LoginUsernameCaseInsensitive disabled")
    public void verifyGetApplicationsWithPaginationForOrgUser() throws ApiException, XPathExpressionException {

        RestAPIStoreImpl restAPIStoreClient = null;
        if (userMode == TestUserMode.SUPER_TENANT_ADMIN) {
            restAPIStoreClient = new RestAPIStoreImpl(orgTestUser, testPassword,
                    keyManagerContext.getContextTenant().getDomain(), storeUrls.getWebAppURLHttps());
        } else if (userMode == TestUserMode.TENANT_ADMIN) {
            restAPIStoreClient = new RestAPIStoreImpl(orgTestUser + "@wso2.com", testPassword,
                    keyManagerContext.getContextTenant().getDomain(), storeUrls.getWebAppURLHttps());
        }

        // enable_application_sharing = false
        // login_username_case_insensitive = false
        ApplicationListDTO appList = restAPIStoreClient.getApplications(null);
        Assert.assertNotNull(appList);
        Assert.assertEquals(appList.getCount().intValue(), 1);
    }

    @Test(groups = { "wso2.am" }, description = "Check getting applications with pagination for user not having organization "
            + "with ApplicationSharing disabled and LoginUsernameCaseInsensitive disabled")
    public void verifyGetApplicationsWithPaginationForNonOrgUser() throws ApiException, XPathExpressionException {

        RestAPIStoreImpl restAPIStoreClient = null;
        if (userMode == TestUserMode.SUPER_TENANT_ADMIN) {
            restAPIStoreClient = new RestAPIStoreImpl(nonOrgTestUser, testPassword,
                    keyManagerContext.getContextTenant().getDomain(), storeUrls.getWebAppURLHttps());
        } else if (userMode == TestUserMode.TENANT_ADMIN) {
            restAPIStoreClient = new RestAPIStoreImpl(nonOrgTestUser + "@wso2.com", testPassword,
                    keyManagerContext.getContextTenant().getDomain(), storeUrls.getWebAppURLHttps());
        }

        // enable_application_sharing = false
        // login_username_case_insensitive = false
        ApplicationListDTO appList = restAPIStoreClient.getApplications(null);
        Assert.assertNotNull(appList);
        Assert.assertEquals(appList.getCount().intValue(), 1);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (userMode == TestUserMode.SUPER_TENANT_ADMIN) {
            userManagementClient.deleteUser(orgTestUser);
            userManagementClient.deleteUser(nonOrgTestUser);
        } else if (userMode == TestUserMode.TENANT_ADMIN) {
            userManagementClient.deleteUser(orgTestUser + "@wso2.com");
            userManagementClient.deleteUser(nonOrgTestUser + "@wso2.com");
        }
    }
}