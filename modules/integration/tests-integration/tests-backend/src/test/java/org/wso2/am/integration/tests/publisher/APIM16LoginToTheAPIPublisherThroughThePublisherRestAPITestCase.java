/*
 *
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.publisher;

import org.json.JSONObject;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIMURLBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.util.Map;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
/*
Login and Logout API Publisher through Publisher Rest API using valid and invalid credentials
APIM2-16 & APIM2-17
 */

public class APIM16LoginToTheAPIPublisherThroughThePublisherRestAPITestCase
        extends APIMIntegrationBaseTest {

    private APIPublisherRestClient apiPublisher;

    @DataProvider(name = "validLoginDataPro")
    public static Object[][] apiPublisherValidLoginCredentials() throws Exception {
        AutomationContext superTenantAdminContext = new AutomationContext
                (APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                        APIMIntegrationConstants.AM_PUBLISHER_INSTANCE,
                        TestUserMode.SUPER_TENANT_ADMIN);
        AutomationContext superTenantUserContext = new AutomationContext
                (APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                        APIMIntegrationConstants.AM_PUBLISHER_INSTANCE,
                        TestUserMode.SUPER_TENANT_USER);
        AutomationContext tenantAdminContext = new AutomationContext
                (APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                        APIMIntegrationConstants.AM_PUBLISHER_INSTANCE,
                        TestUserMode.TENANT_ADMIN);
        AutomationContext tenantUserContext = new AutomationContext
                (APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                        APIMIntegrationConstants.AM_PUBLISHER_INSTANCE,
                        TestUserMode.TENANT_USER);
        return new Object[][]{
                {superTenantAdminContext.getContextTenant().getContextUser().getUserName(),
                        superTenantAdminContext.getContextTenant().getContextUser().getPassword()},
                {superTenantUserContext.getContextTenant().getContextUser().getUserName(),
                        superTenantUserContext.getContextTenant().getContextUser().getPassword()},
                {tenantAdminContext.getContextTenant().getContextUser().getUserName(),
                        tenantAdminContext.getContextTenant().getContextUser().getPassword()},
                {tenantUserContext.getContextTenant().getContextUser().getUserName(),
                        tenantUserContext.getContextTenant().getContextUser().getPassword()}
        };
    }

    @DataProvider(name = "invalidLoginDataPro")
    public static Object[][] apiPublisherInValidLoginCredentials() throws Exception {
        AutomationContext superTenantAdminContext = new AutomationContext
                (APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                        APIMIntegrationConstants.AM_PUBLISHER_INSTANCE,
                        TestUserMode.SUPER_TENANT_ADMIN);
        return new Object[][]{
                {superTenantAdminContext.getContextTenant().getContextUser().getUserName(),
                        "invalid"},
                {superTenantAdminContext.getContextTenant().getContextUser().getUserName(),
                        null},
                {"invalid", superTenantAdminContext.getContextTenant().getContextUser().
                        getPassword()},
                {null, superTenantAdminContext.getContextTenant().getContextUser().
                        getPassword()},
                {null, null},
                {"1234", "asde"}
        };
    }

    @Test(dataProvider = "validLoginDataPro", description = "Login to the API Publisher through " +
            "publisher Rest API using valid credentials")
    public void testPublisherLoginAndLogoutForValidCredentials(String userName, String password)
            throws Exception {

        AutomationContext publisherContext = new AutomationContext
                (APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                        APIMIntegrationConstants.AM_PUBLISHER_INSTANCE,
                        TestUserMode.SUPER_TENANT_ADMIN);
        APIMURLBean publisherUrls = new APIMURLBean(publisherContext.getContextUrls());
        apiPublisher = new APIPublisherRestClient(publisherUrls.getWebAppURLHttp());

        HttpResponse loginResponse = apiPublisher.login(userName, password);
        JSONObject loginJsonObject = new JSONObject(loginResponse.getData());
        assertFalse(loginJsonObject.getBoolean("error"),
                "Invalid login Request for user :" + userName);

        //Validate Session Cookies
        Map<String, String> loginSession = loginResponse.getHeaders();
//        assertTrue(loginResponse.getData().contains());

        //Get all the APIs after login to the Publisher Rest API
        JSONObject apiObject = new JSONObject(apiPublisher.getAllAPIs().getData());
        assertFalse(apiObject.getBoolean("error"),
                "Cannot Retrieve all the APIs after successful login");


        //TODO Check session cookies and called getAPI method

        //API Publisher Logout
        HttpResponse logoutResponse = apiPublisher.logout();
        JSONObject logoutJsonObject = new JSONObject(logoutResponse.getData());
        assertFalse(logoutJsonObject.getBoolean("error"),
                "Invalid logout request for user : " + userName);

        //Trying to Get all the APIs after logout from the Publisher Rest API
        JSONObject apiLogoutObject = new JSONObject(apiPublisher.getAllAPIs().getData());
        assertTrue(apiLogoutObject.getBoolean("error"),
                "Able to Retrieve all the APIs after successful logout");
    }

    @Test(dataProvider = "invalidLoginDataPro", description =
            "Login to the API Publisher through " +
                    "publisher Rest API using invalid credentials ")
    public void testInvalidStoreLogin(String userName, String password) throws Exception {

        AutomationContext publisherContext = new AutomationContext
                (APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                        APIMIntegrationConstants.AM_PUBLISHER_INSTANCE,
                        TestUserMode.SUPER_TENANT_ADMIN);
        APIMURLBean publisherUrls = new APIMURLBean(publisherContext.getContextUrls());
        apiPublisher = new APIPublisherRestClient(publisherUrls.getWebAppURLHttp());

        HttpResponse loginResponse = apiPublisher.login(userName, password);
        JSONObject loginJsonObject = new JSONObject(loginResponse.getData());
        assertTrue(loginJsonObject.getBoolean("error"),
                "Allow to login with invalid credential :" + userName);
        //TODO Assert error message and session cookies
    }

}

//TODO check for the multiple login