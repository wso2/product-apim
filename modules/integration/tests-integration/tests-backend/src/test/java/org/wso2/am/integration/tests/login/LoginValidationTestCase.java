/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.am.integration.tests.login;


import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.FrameworkConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import static org.testng.Assert.assertTrue;

/**
 * test fail test scenarios
 */

public class LoginValidationTestCase extends APIMIntegrationBaseTest {
    
    private final String INTERNAL_ROLE_SUBSCRIBER = "Internal/subscriber";
    private final String ROLE_SUBSCRIBER = "subscriber";
    
    private String publisherURLHttp;
    private String storeURLHttp;
    private UserManagementClient userManagementClient;
    private String invalidUserName;
    private String subscriberUser;
    //private String subscriberRole;

    @Factory(dataProvider = "userModeDataProvider")
    public LoginValidationTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        publisherURLHttp = getPublisherURLHttp();
        storeURLHttp = getStoreURLHttp();
     

        userManagementClient = new UserManagementClient(
                storeContext.getContextUrls().getBackEndUrl(), createSession(storeContext));

        if (storeContext.getContextTenant().getDomain().equals(FrameworkConstants.SUPER_TENANT_DOMAIN_NAME)) {
            invalidUserName = storeContext.getContextTenant().getContextUser().getUserName() + "invalid";
            subscriberUser ="subscriberUser";            
        }
        else{
            invalidUserName = storeContext.getContextTenant().getTenantAdmin().getUserName().replace("admin","admininvalid");
            subscriberUser = "subscriberUser@wso2.com";            
        }

    }

    @Test(groups = {"wso2.am"}, description = "Login as invalid user to publisher")
    public void testInvalidLoginAsPublisherTestCase() throws Exception {

        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        //Try invalid login to publisher

        HttpResponse httpResponse = apiPublisherRestClient.login(invalidUserName,
                                                                 publisherContext.getContextTenant().getContextUser().getPassword());

        JSONObject response = new JSONObject(httpResponse.getData());

        assertTrue(response.getString("error").toString().equals("true") &&
                   response.getString("message").toString().contains("Please recheck the username and password and try again"),
                   "Invalid user can login to the API publisher");

    }

    @Test(groups = {"wso2.am"}, description = "Login to publisher as subscriber user")
    public void testInvalidLoginAsSubscriberTestCase()
            throws Exception {

        //Try login to publisher with subscriber user
        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        
        if(!userManagementClient.roleNameExists(INTERNAL_ROLE_SUBSCRIBER)){
            String[] subscriberPermissions = {
                    "/permission/admin/login",
                    "/permission/admin/manage/api/subscribe"};
            userManagementClient.addInternalRole(ROLE_SUBSCRIBER, null, subscriberPermissions);
        }

        if ((userManagementClient != null) &&
            !userManagementClient.userNameExists(INTERNAL_ROLE_SUBSCRIBER, "subscriberUser")) {
            userManagementClient.addUser("subscriberUser", "password@123",
                                         new String[]{INTERNAL_ROLE_SUBSCRIBER}, null);
        }

        HttpResponse httpResponse = apiPublisherRestClient.login(subscriberUser,
                                                                 "password@123");

        JSONObject response = new JSONObject(httpResponse.getData());

        assertTrue(response.getString("error").toString().equals("true")
                   && response.getString("message").toString().contains("Login failed.Insufficient privileges"),
                   "Invalid subscriber can login to the API publisher");
    }



    @Test(groups = {"wso2.am"}, description = "Login to API store test scenario")
    public void testLoginToStoreTestCase() throws Exception {

        //Try login to publisher with tenant subscriber user
        String APICreatorRole = "APICreatorRole";
        String APIPublisherRole = "APIPublisherRole";
        String APIPublisherUser = "APIPublisherUser";
        String APICreatorUser = "APICreatorUser";
        String password = "password@123";

        APIStoreRestClient apiStoreRestClient = new APIStoreRestClient(storeURLHttp);

        String[] createPermissions = {
                "/permission/admin/login",
                "/permission/admin/manage/api/create"};

        if (!userManagementClient.roleNameExists(APICreatorRole)) {
            userManagementClient.addRole(APICreatorRole, null, createPermissions);
        }

        if ((userManagementClient != null) &&
            !userManagementClient.userNameExists(APICreatorRole, APICreatorUser)) {
            userManagementClient.addUser(APICreatorUser, password,
                                         new String[]{APICreatorRole}, null);
        }

        String[] publishPermissions = {
                "/permission/admin/login",
                "/permission/admin/manage/api/publish"};

        if (!userManagementClient.roleNameExists(APIPublisherRole)) {
            userManagementClient.addRole(APIPublisherRole, null, publishPermissions);
        }

        if ((userManagementClient != null) &&
            !userManagementClient.userNameExists(APIPublisherRole, APIPublisherUser)) {
            userManagementClient.addUser(APIPublisherUser, password,
                                         new String[]{APIPublisherRole}, null);
        }

        HttpResponse httpResponse = apiStoreRestClient.login("invaliduser", "invaliduser@123");

        JSONObject response = new JSONObject(httpResponse.getData());

        assertTrue(response.getString("error").toString().equals("true")
                   && response.getString("message").toString().contains(
                           "Login failed.Please recheck the username and password and try again"),
                   "Invalid user can login to the API store");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
    }

}
