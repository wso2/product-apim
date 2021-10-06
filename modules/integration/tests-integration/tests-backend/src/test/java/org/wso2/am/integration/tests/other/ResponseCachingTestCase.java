/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.other;

import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.AuthenticatorClient;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class ResponseCachingTestCase extends APIMIntegrationBaseTest {

    private String apiId;
    private String tokenTestApiAppId;
    private String session;
    private String gatewayBackendURL;

    @Factory(dataProvider = "userModeDataProvider")
    public ResponseCachingTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[] { TestUserMode.TENANT_ADMIN },
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        AuthenticatorClient login = new AuthenticatorClient(gatewayContextMgt.getContextUrls().getBackEndUrl());
        session = login.login("admin", "admin", "localhost");
        gatewayBackendURL = gatewayContextMgt.getContextUrls().getBackEndUrl();
        // Upload the synapse api
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            String file = "artifacts" + File.separator + "AM" + File.separator + "synapseconfigs" +
                    File.separator + "rest" + File.separator + "response_caching_dummy_api_1.xml";
            APIMTestCaseUtils.updateSynapseConfiguration(APIMTestCaseUtils.loadResource(file), gatewayBackendURL, session);
        } else if (TestUserMode.TENANT_ADMIN == userMode) {
            String file = "artifacts" + File.separator + "AM" + File.separator + "synapseconfigs" +
                    File.separator + "rest" + File.separator + "response_caching_dummy_api_2.xml";
            APIMTestCaseUtils.updateSynapseConfiguration(APIMTestCaseUtils.loadResource(file), gatewayBackendURL, session);
        }
        Thread.sleep(5000);
    }

    @Test(groups = {"wso2.am"}, description = "Test response caching functionality")
    public void testResponseCaching() throws Exception {

        String APIName = "ResponseCachingTestAPI";
        String APIContext = "responseCachingTestAPI";
        String description = "This is to test Response caching by API manager";
        String APIVersion = "1.0.0";
        String url = "";
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            url = gatewayUrlsWrk.getWebAppURLNhttp() + "responseCache1";
        } else if (TestUserMode.TENANT_ADMIN == userMode) {
            url = gatewayUrlsWrk.getWebAppURLNhttp() + "responseCache2";
        }

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url), new URL(url));
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setSandbox(url);
        apiRequest.setResponseCachingEnabled(true);
        apiRequest.setProvider(user.getUserName());

        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = serviceResponse.getData();
        restAPIPublisher.changeAPILifeCycleStatus(apiId, Constants.PUBLISHED);

        // Create application
        ApplicationDTO applicationDTO = restAPIStore.addApplication("ResponseCachingTestAPI-Application",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "this-is-test");
        tokenTestApiAppId = applicationDTO.getApplicationId();

        // Create subscription
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, applicationDTO.getApplicationId(),
                APIMIntegrationConstants.API_TIER.GOLD);
        Assert.assertEquals(true, subscriptionDTO.getThrottlingPolicy().equals("Gold"));

        //Generate sandbox Token and invoke with that
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(), "3600",
                null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX, null, grantTypes);
        String sandboxAccessToken = applicationKeyDTO.getToken().getAccessToken();
        Map<String, String> requestHeadersSandBox = new HashMap<String, String>();
        requestHeadersSandBox.put("Authorization", "Bearer " + sandboxAccessToken);
        requestHeadersSandBox.put("accept", "text/xml");
        HttpResponse responseSandBox = HttpRequestUtil
                .doGet(getAPIInvocationURLHttp("responseCachingTestAPI/1.0.0/customers/123"), requestHeadersSandBox);
        assertEquals(responseSandBox.getResponseCode(), 200, "Response code mismatched");

        // Delete backend api
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            APIMTestCaseUtils.deleteApi(gatewayBackendURL, session, "Response_Cache_1");
        } else if (TestUserMode.TENANT_ADMIN == userMode) {
            APIMTestCaseUtils.deleteApi(gatewayBackendURL, session, "Response_Cache_2");
        }

        // Invoke the api to test response caching
        responseSandBox = HttpRequestUtil
                .doGet(getAPIInvocationURLHttp("responseCachingTestAPI/1.0.0/customers/123"), requestHeadersSandBox);
        assertEquals(responseSandBox.getResponseCode(), 200, "Response code mismatched");
        assertTrue(responseSandBox.getData().contains("<response><value>Received Request</value></response>"),
                "Response data mismatched when invoking API with response caching enabled." +
                        " Response Data:" + responseSandBox.getData());

        // Add a new request header and test whether the cache gets cleared
        requestHeadersSandBox.put("custom-header", "responseCacheTest");
        responseSandBox = HttpRequestUtil
                .doGet(getAPIInvocationURLHttp("responseCachingTestAPI/1.0.0/customers/123"), requestHeadersSandBox);
        assertEquals(responseSandBox.getResponseCode(), 404, "Response code mismatched. " +
                "Response cache is not getting cleared for a request with request header change.");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(tokenTestApiAppId);
        restAPIPublisher.deleteAPI(apiId);
    }
}
