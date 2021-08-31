/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.store.api.v1.dto.*;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class CrossTenantSubscriptionTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(CrossTenantSubscriptionTestCase.class);
    private UserManagementClient userManagementClient1 = null;
    private UserManagementClient userManagementClient2 = null;
    private static final String API_NAME = "APIScopeTestAPI";
    private static final String API_VERSION = "1.0.0";
    private static final String APP_NAME = "NewApplication";
    private String USER_SMITH = "smith";
    private String USER_MITCHEL = "mitchel";
    private String ADMIN_ROLE = "admin";
    private final String INTERNAL_ROLE_SUBSCRIBER = "Internal/subscriber";
    private final String INTERNAL_ROLE_PUBLISHER = "Internal/publisher";
    private final String INTERNAL_ROLE_CREATOR = "Internal/creator";
    private String apiId;
    private String applicationId;
    private ArrayList<String> grantTypes;

    @Factory(dataProvider = "userModeDataProvider")
    public CrossTenantSubscriptionTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        grantTypes = new ArrayList<>();
    }

    @Test(groups = {"wso2.am"}, description = "Testing the scopes with admin, subscriber roles")
    public void testSetScopeToResourceTestCase() throws Exception {

        tenantManagementServiceClient.addTenant("tenanta.com", "abc123", "usera", "demo");
        tenantManagementServiceClient.addTenant("tenantb.com", "abc123", "userb", "demo");

        userManagementClient1 = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                "usera@tenanta.com", "abc123");
        userManagementClient2 = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                "userb@tenantb.com", "abc123");
        // crating user smith
        String gatewayUrlTenantA =
                    gatewayUrlsWrk.getWebAppURLNhttp() + "t/tenanta.com/";
        String userSmith = USER_SMITH + "@tenanta.com";

        // crating user smith
        String gatewayUrlTenantB =
                gatewayUrlsWrk.getWebAppURLNhttp() + "t/tenantb.com/";
        String userMitchel = USER_MITCHEL + "@tenantb.com";

        userManagementClient1.addUser(USER_SMITH, "john123", new String[]{INTERNAL_ROLE_PUBLISHER, INTERNAL_ROLE_CREATOR}, USER_SMITH);
        userManagementClient2.addUser(USER_MITCHEL, "mitch123", new String[]{INTERNAL_ROLE_SUBSCRIBER}, USER_MITCHEL);
        restAPIPublisher = new RestAPIPublisherImpl(
                USER_SMITH,
                "john123",
                "tenanta.com", publisherURLHttps);
        restAPIStore =
                new RestAPIStoreImpl(
                        USER_MITCHEL,
                        "mitch123",
                        "tenantb.com", storeURLHttps, restAPIGateway);

        // Adding API
        String apiContext = "crossSubAPI";
        String url = getGatewayURLNhttp() + "response";
        String description = "This is a test API created by API manager integration test";

        APIRequest apiRequest = new APIRequest(API_NAME, apiContext, new URL(url));
        apiRequest.setDescription(description);
        apiRequest.setVersion(API_VERSION);
        apiRequest.setSubscriptionAvailability("all_tenants");
        apiRequest.setProvider(USER_SMITH + "@tenanta.com");

        apiId = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);

        waitForAPIDeploymentSync(USER_SMITH + "@tenanta.com", API_NAME, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);

        waitForAPIDeploymentSync(USER_SMITH + "@tenanta.com", API_NAME, API_VERSION, APIMIntegrationConstants.IS_API_NOT_EXISTS);
        waitForAPIDeploymentSync(USER_SMITH + "@tenanta.com", API_NAME, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);

        // For Admin user
        // create new application and subscribing
        //add an application
        HttpResponse applicationResponse = restAPIStore.createApplication(APP_NAME,
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();

        String provider = USER_SMITH + "@tenanta.com";
        //subscribe to the api
        HttpResponse subscribeResponse = subscribeToAPIUsingRest(apiId, applicationId,
                APIMIntegrationConstants.API_TIER.GOLD, restAPIStore);
        assertEquals(subscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of old API version request not successful " +
                        " API Name:" + API_NAME + " API Version:" + API_VERSION +
                        " API Provider Name :" + provider);

        //Generate production token and invoke with that
        //get access token
        ArrayList<String> scopes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, scopes, grantTypes);

        // get Consumer Key and Consumer Secret
        String consumerKey = applicationKeyDTO.getConsumerKey();
        String consumerSecret = applicationKeyDTO.getConsumerSecret();

        URL tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
        String accessToken;
        Map<String, String> requestHeaders;
        HttpResponse response;
        URL endPointURL;
        String requestBody;
        JSONObject accessTokenGenerationResponse;

        requestBody = "grant_type=password&username=" + userMitchel +
                "&password=" + "mitch123";

        response = restAPIStore.generateUserAccessKey(consumerKey, consumerSecret,
                                                  requestBody, tokenEndpointURL);
        accessTokenGenerationResponse = new JSONObject(response.getData());
        accessToken = accessTokenGenerationResponse.getString("access_token");

        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        response = HttpRequestUtil.doGet(gatewayUrlTenantA + "testScopeAPI/1.0.0/test", requestHeaders);

        assertEquals(response.getResponseCode(), Response.Status.ACCEPTED.getStatusCode(),
                     "Cross tenant user cannot access the GET Method");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(apiId);
        userManagementClient1.deleteUser(USER_SMITH);
        userManagementClient2.deleteUser(USER_MITCHEL);
        
        super.cleanUp();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
        };
    }
}

