/*
 *
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */



package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIMURLBean;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/*APIM2-641- Login to the API store
APIM-2-642 - Logout API store
APIM2-647 - sign up user to store*/

public class APIM641StoreApiTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(APIM641StoreApiTestCase.class);
    private APIStoreRestClient apiStore;

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException {
        super.init();
    }

    @DataProvider(name = "validLogin")
    public static Object[][] apiLoginCredentialsDataProvider() throws Exception {
        AutomationContext superTenantAdminAutomationContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_STORE_INSTANCE, TestUserMode.SUPER_TENANT_ADMIN);

        AutomationContext superTenantUserAutomationContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_STORE_INSTANCE, TestUserMode.SUPER_TENANT_USER);

        AutomationContext tenantAdminAutomationContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_STORE_INSTANCE, TestUserMode.TENANT_ADMIN);

        AutomationContext tenantUserAutomationContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_STORE_INSTANCE, TestUserMode.TENANT_USER);


        return new Object[][]{
                {superTenantAdminAutomationContext.getContextTenant().getContextUser().getUserName(),
                        superTenantAdminAutomationContext.getContextTenant().getContextUser().getPassword()},
                {superTenantUserAutomationContext.getContextTenant().getContextUser().getUserName(),
                        superTenantUserAutomationContext.getContextTenant().getContextUser().getPassword()},
                {tenantAdminAutomationContext.getContextTenant().getContextUser().getUserName(),
                        tenantAdminAutomationContext.getContextTenant().getContextUser().getPassword()},
                {tenantUserAutomationContext.getContextTenant().getContextUser().getUserName(),
                        tenantUserAutomationContext.getContextTenant().getContextUser().getPassword()}
        };
    }

    @DataProvider(name = "invalidLogin")
    public static Object[][] apiInvalidLoginCredentialsDataProvider() throws Exception {

        AutomationContext superTenantAdmin = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_STORE_INSTANCE, TestUserMode.SUPER_TENANT_ADMIN);

        return new Object[][]{
                {superTenantAdmin.getContextTenant().getContextUser().getUserName(), "12345"},
                {superTenantAdmin.getContextTenant().getContextUser().getUserName(), ""},
                {"", superTenantAdmin.getContextTenant().getContextUser().getPassword()},
                {"", ""},
                {"abc", "12345"}
        };
    }

    @DataProvider(name = "userSignUp")
    public static Object[][] storeUserSignUpCredentialsDataProvider() throws Exception {

        return new Object[][]{
                {"user1", "user1pass", "firstName", "lastNmae", "user1@wso2.com"},
                {"user3", "user3pass", "User3", "Test", ""},
                {"user4", "user4pass", "User2", "", "user2@wso2.com"},
                {"user5", "user5pass", "", "Test", "user2@wso2.com"},
        };
    }

    @DataProvider(name = "invalidUserSignUp")
    public static Object[][] storeInvalidUserSignUpCredentialsDataProvider() throws Exception {

        return new Object[][]{
                {"user6", "", "User6", "Test", "user2@wso2.com"},
                {"", "user7pass", "User7", "Test", "user2@wso2.com"}
        };
    }

    @Test(dataProvider = "apiLoginCredentialsDataProvider", description = "Verify Valid login for different scenarios")
    public void testValidStoreLoginAndLogout(String userName, String password) throws Exception {

        AutomationContext storeContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_STORE_INSTANCE, TestUserMode.SUPER_TENANT_ADMIN);
        APIMURLBean storeUrls = new APIMURLBean(storeContext.getContextUrls());
        String storeURLHttp = storeUrls.getWebAppURLHttp();
        apiStore = new APIStoreRestClient(storeURLHttp);
        HttpResponse loginResponse = apiStore.login(userName, password);
        log.info("Login User: " + userName + " Password: " + password);
        JSONObject loginJsonObject = new JSONObject(loginResponse.getData());
        assertFalse(loginJsonObject.getBoolean("error"), "Error in Login Request: User Name : " + userName);

        //Verify Logout
        HttpResponse logoutResponse = apiStore.logout();
        JSONObject logoutJsonObject = new JSONObject(logoutResponse.getData());
        assertFalse(logoutJsonObject.getBoolean("error"), "Error in Logout Request : User Name : " + userName);
    }

    @Test(dataProvider = "apiInvalidLoginCredentialsDataProvider", description = "Verify Invalid Login for different scenarios")
    public void testInvalidStoreLogin(String userName, String password) throws Exception {

        AutomationContext storeContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_STORE_INSTANCE, TestUserMode.SUPER_TENANT_ADMIN);
        APIMURLBean storeUrls = new APIMURLBean(storeContext.getContextUrls());
        String storeURLHttp = storeUrls.getWebAppURLHttp();
        apiStore = new APIStoreRestClient(storeURLHttp);
        HttpResponse loginResponse = apiStore.login(userName, password);
        log.info("Login User: " + userName + " Password: " + password);
        JSONObject loginJsonObject = new JSONObject(loginResponse.getData());
        assertTrue(loginJsonObject.getBoolean("error"), "Login success for invalid credentials : User Name: " + userName);
    }

    //user sign up
    @Test(dataProvider = "storeUserSignUpCredentialsDataProvider", description = "Verify user sign up")
    public void testUserSignUp(String userName, String password, String firstName, String lastName, String email) throws
            Exception {

        AutomationContext storeContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_STORE_INSTANCE, TestUserMode.SUPER_TENANT_ADMIN);
        APIMURLBean storeUrls = new APIMURLBean(storeContext.getContextUrls());
        String storeURLHttp = storeUrls.getWebAppURLHttp();
        apiStore = new APIStoreRestClient(storeURLHttp);
        HttpResponse storeSignUpResponse = apiStore.signUp(userName, password, firstName, lastName, email);
        log.info("Sign Up User: " + userName);
        JSONObject signUpJsonObject = new JSONObject(storeSignUpResponse.getData());
        assertFalse(signUpJsonObject.getBoolean("error"), "Error in user sign up Response");
        assertFalse(signUpJsonObject.getBoolean("showWorkflowTip"), "Error in sign up Response");

        //login with new user
        HttpResponse loginResponse = apiStore.login(userName, password);
        JSONObject loginJsonObject = new JSONObject(loginResponse.getData());
        assertFalse(loginJsonObject.getBoolean("error"), "Error in Login Request: User Name : " + userName);
    }

    //invalid user sign up
    @Test(dataProvider = "storeInvalidUserSignUpCredentialsDataProvider", description = "invalid user sign up")
    public void testInvalidUserSignUp(String userName, String password, String firstName, String lastName, String email)
            throws Exception {
        AutomationContext storeContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_STORE_INSTANCE, TestUserMode.SUPER_TENANT_ADMIN);
        APIMURLBean storeUrls = new APIMURLBean(storeContext.getContextUrls());
        String storeURLHttp = storeUrls.getWebAppURLHttp();
        apiStore = new APIStoreRestClient(storeURLHttp);
        HttpResponse storeSignUpResponse = apiStore.signUp(userName, password, firstName, lastName, email);
        JSONObject signUpJsonObject = new JSONObject(storeSignUpResponse.getData());
        assertTrue(signUpJsonObject.getBoolean("error"), "Error in Invalid User Sign up Response");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        int deletedUserCount = 0;
        int beforeDeleteUserCount = storeUserSignUpCredentialsDataProvider().length;
        for (int i = 0; i < storeUserSignUpCredentialsDataProvider().length; i++) {
            userManagementClient.deleteUser(storeUserSignUpCredentialsDataProvider()[i][0].toString());
            deletedUserCount++;
        }
        assertEquals(deletedUserCount,beforeDeleteUserCount,"Error in user Deletion");
    }
}
