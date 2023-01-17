/*
 *  Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.application;

import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.SubscriptionsApi;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.UserManagementUtils;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.wso2.am.integration.test.utils.base.APIMIntegrationConstants.SUPER_TENANT_DOMAIN;

public class ApplicationBlockSubscriptionTestCase extends APIManagerLifecycleBaseTest {
    private static final String APPLICATION_NAME = "test-app";
    private static final String USER_NAME = "test-user";
    private static final String PASSWORD = "password";
    private static final String ORGANIZATION = "test";
    private static final String FIRST_NAME = "John";
    private RestAPIStoreImpl restAPIStoreClientUser;
    private String applicationId;
    private String apiId;
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private SubscriptionsApi subscriptionsApi;
    private String subscriptionId;
    private final String API_END_POINT_METHOD = "/customers/123";
    private final String API_NAME = "BlockAPITest";
    private final String API_CONTEXT = "BlockAPI";
    private static final String PROD_ONLY_BLOCK_STATE = "BLOCKED";
    private String accessToken;

    @Factory(dataProvider = "userModeDataProvider")
    public ApplicationBlockSubscriptionTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        subscriptionsApi = new SubscriptionsApi(restAPIPublisher.apiPublisherClient);
        createUsersAndApplications();
    }

    @Test(description = "To test block functionality works for the application name and owner name which contains hyphen")
    public void testBlockUnblockSubscription() throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
        HttpResponse invokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT,
                        API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke api before block");

        int statusCodeBlock = subscriptionsApi.blockSubscriptionWithHttpInfo(subscriptionId,
                PROD_ONLY_BLOCK_STATE, null).getStatusCode();
        Assert.assertEquals(statusCodeBlock, HTTP_RESPONSE_CODE_OK);

        // Wait one second
        Thread.sleep(1000);
        invokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT,
                        API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        // Retry invocation
        if (invokeResponse.getResponseCode() != HTTP_RESPONSE_CODE_UNAUTHORIZED) {
            // Wait five seconds
            Thread.sleep(5000);
            invokeResponse =
                    HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT,
                            API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        }
        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_UNAUTHORIZED,
                "Response code mismatched when invoke api after block");

        int statusCodeUnblock = subscriptionsApi.unBlockSubscriptionWithHttpInfo(subscriptionId,
                null).getStatusCode();
        Assert.assertEquals(statusCodeUnblock, HTTP_RESPONSE_CODE_OK);

        // Wait one second
        Thread.sleep(1000);
        invokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT,
                        API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        if (invokeResponse.getResponseCode() != HTTP_RESPONSE_CODE_OK) {
            // Wait five seconds
            Thread.sleep(5000);
            invokeResponse =
                    HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT,
                            API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        }
        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke api after unblock");

    }

    private void createUsersAndApplications() throws Exception{
        // Signup user.
        UserManagementUtils.signupUser(USER_NAME, PASSWORD, FIRST_NAME, ORGANIZATION);
        restAPIStoreClientUser = new RestAPIStoreImpl(USER_NAME, PASSWORD, SUPER_TENANT_DOMAIN, storeURLHttps);
        // Create application
        HttpResponse appCreationResponse1 = restAPIStoreClientUser.createApplication(APPLICATION_NAME,
                "App created by user", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = appCreationResponse1.getData();

        APIRequest apiRequest;
        String apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));

        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(user.getUserName());
        //Create, Publish
        apiId = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);
        // Subscribe
        subscriptionId = restAPIStoreClientUser.subscribeToAPI(apiId, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED).getSubscriptionId();

        ArrayList grantTypes = new ArrayList();
        grantTypes.add("client_credentials");
        ApplicationKeyDTO applicationKeyDTO = restAPIStoreClientUser.generateKeys(applicationId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        accessToken = applicationKeyDTO.getToken().getAccessToken();
    }
}
