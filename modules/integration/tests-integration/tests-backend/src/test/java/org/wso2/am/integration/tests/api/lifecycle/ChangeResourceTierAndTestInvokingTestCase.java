/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.api.lifecycle;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.tests.throttling.AdvancedThrottlingConfig;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Change the API resource tier and test the throttling.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE })
public class ChangeResourceTierAndTestInvokingTestCase extends APIManagerLifecycleBaseTest {
    //Random number to avoid test failure on clustered setup. Swagger document of the API not getting
    //removed properly hence re-executing the tests trend to yield false negatives.
    private final String API_NAME = "ChangeResourceTierAndTestInvokingTest" + (int) (Math.random() * 100 + 1);
    private final String API_CONTEXT = "ChangeResourceTierAndTestInvoking" + (int) (Math.random() * 50 + 1);
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_END_POINT_METHOD = "customers/123";
    private final String API_RESPONSE_DATA = "<id>123</id><name>John</name></Customer>";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "ChangeResourceTierAndTestInvokingTestCase";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String providerName;
    private APIIdentifier apiIdentifier;
    private Map<String, String> requestHeaders;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private APICreationRequestBean apiCreationRequestBean;
    private boolean isInitialised = false;

    public void initialize() throws Exception {
        if (!isInitialised) {
            super.init();
            apiEndPointUrl = getGatewayURLHttp() + API_END_POINT_POSTFIX_URL;
            providerName = user.getUserName();
            apiCreationRequestBean =
                    new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                            new URL(apiEndPointUrl));
            apiCreationRequestBean.setTags(API_TAGS);
            apiCreationRequestBean.setDescription(API_DESCRIPTION);
            String publisherURLHttp = publisherUrls.getWebAppURLHttp();
            String storeURLHttp = storeUrls.getWebAppURLHttp();
            apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
            apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);

            //Login to API Publisher with  admin
            apiPublisherClientUser1.login(user.getUserName(), user.getPassword());

            //Login to API Store with  admin
            apiStoreClientUser1.login(user.getUserName(), user.getPassword());
            apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);

            isInitialised = true;
        }
    }


    @Test(groups = {"wso2.am"}, description = "test  the  throttling of a API. API Tier :Gold, Application Tier: GOLD, " +
                                              "Resource Tier: Unlimited.")
    public void testInvokingWithAPIGoldTierApplicationGoldResourceUnlimited() throws Exception {
        initialize();
        //Create application
        apiStoreClientUser1.addApplication(APPLICATION_NAME, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");
        //Create publish and subscribe a API
        APIIdentifier apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiIdentifier.setTier(TIER_GOLD);
        createPublishAndSubscribeToAPI(
                apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1, apiStoreClientUser1, APPLICATION_NAME);
        //get access token
        waitForAPIDeploymentSync(apiIdentifier.getProviderName(), API_NAME,
                                 apiIdentifier.getVersion(), APIMIntegrationConstants.IS_API_EXISTS);

        String accessToken = generateApplicationKeys(apiStoreClientUser1, APPLICATION_NAME).getAccessToken();
        // Create requestHeaders
        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        long startTime = System.currentTimeMillis();
        long currentTime;
        for (int invocationCount = 1; invocationCount <= GOLD_INVOCATION_LIMIT_PER_MIN; invocationCount++) {
            currentTime = System.currentTimeMillis();
            //Invoke  API
            HttpResponse invokeResponse =
                    HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/" +
                                          API_END_POINT_METHOD, requestHeaders);
            assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                         "Response code mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                         (currentTime - startTime) + " milliseconds under Gold API , Gold Application level tier" +
                         " and Unlimited Resource tier");
            assertTrue(invokeResponse.getData().contains(API_RESPONSE_DATA),
                       "Response data mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                       (currentTime - startTime) + " milliseconds under Gold API , Gold Application level tier" +
                       "  and Unlimited Resource tier");
        }

        HttpResponse invokeResponse;
        currentTime = System.currentTimeMillis();
        invokeResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/" +
                                               API_END_POINT_METHOD, requestHeaders);
        //send the request again if the test runs on a cluster. It will be 1 attempt more to get the api blocked.
        if (executionMode.equalsIgnoreCase(String.valueOf(ExecutionEnvironment.PLATFORM))) {
            while(true) {
                invokeResponse =
                        HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/" +
                                              API_END_POINT_METHOD, requestHeaders);
                System.out.println("response code XXXXX " + invokeResponse.getResponseCode());
                if (invokeResponse.getResponseCode() == HTTP_RESPONSE_CODE_TOO_MANY_REQUESTS){
                    break;
                }
            }
        }

        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_TOO_MANY_REQUESTS,
                     "Response code mismatched. Invocation attempt:" + (GOLD_INVOCATION_LIMIT_PER_MIN + 1) +
                     " passed  during :" + (currentTime - startTime) + " milliseconds under Gold API , " +
                     "Gold Application level tier and Unlimited Resource tier.");
        assertTrue(invokeResponse.getData().contains(MESSAGE_THROTTLED_OUT),
                   "Response data mismatched. Invocation attempt:" + (GOLD_INVOCATION_LIMIT_PER_MIN + 1) +
                   " passed  during :" + (currentTime - startTime) + " milliseconds under Gold API , " +
                   "Gold Application level tier and Unlimited Resource tier.");

    }


    @Test(groups = {"wso2.am"}, description = "test  the  throttling of a API. API Tier :Gold, Application Tier: GOLD, " +
   "Resource Tier: Plus.", dependsOnMethods = "testInvokingWithAPIGoldTierApplicationGoldResourceUnlimited")
    public void testInvokingWithAPIGoldTierApplicationGoldResourceSilver() throws Exception {
        initialize();
        Thread.sleep(THROTTLING_UNIT_TIME + THROTTLING_ADDITIONAL_WAIT_TIME);

        String swagger = " {\"paths\":{\"/*\":{\"get\":{\"x-auth-type\":\"Application \",\"x-throttling-tier\":" +
                "\"" + APIMIntegrationConstants.RESOURCE_TIER.PLUS
                + "\",\"responses\":{\"200\":\"{}\"}}}},\"swagger\":\"2.0\",\"x-wso2-security\":{\"apim\"" +
                ":{\"x-wso2-scopes\":[]}},\"info\":{\"licence\":{},\"title\":\"" + API_NAME + "\",\"description\":" +
                "\"This is test API create by API manager integration test\",\"contact\":{\"email\":null,\"name\":null}," +
                "\"version\":\"" + API_VERSION_1_0_0 + "\"}}";


        apiPublisherClientUser1.updateResourceOfAPI(providerName, API_NAME, API_VERSION_1_0_0, swagger);

        waitForAPIDeploymentSync(providerName, API_NAME, API_VERSION_1_0_0, APIMIntegrationConstants.IS_API_EXISTS);

        apiStoreClientUser1.waitForSwaggerDocument(user.getUserName(), API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.RESOURCE_TIER.PLUS, executionMode);

        long startTime = System.currentTimeMillis();
        long currentTime;
        for (int invocationCount = 1; invocationCount <= SILVER_INVOCATION_LIMIT_PER_MIN; invocationCount++) {
            currentTime = System.currentTimeMillis();
            //Invoke  API
            HttpResponse invokeResponse =
                    HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/" +
                                          API_END_POINT_METHOD, requestHeaders);
            assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,

                    "Response code mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                            (currentTime - startTime) + " milliseconds under Gold API  and Gold Application level tier" +
                            "  and Plus Resource tier");
            assertTrue(invokeResponse.getData().contains(API_RESPONSE_DATA),
                    "Response data mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                            (currentTime - startTime) + " milliseconds under Gold API and Gold Application level tier" +
                            " and Plus Resource tier");

        }

        currentTime = System.currentTimeMillis();

        HttpResponse invokeResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/" +
                                                            API_END_POINT_METHOD, requestHeaders);

        if (executionMode.equalsIgnoreCase(String.valueOf(ExecutionEnvironment.PLATFORM))) {
            while(true) {
                invokeResponse =
                        HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/" +
                                              API_END_POINT_METHOD, requestHeaders);
                System.out.println("response code XXXXX " + invokeResponse.getResponseCode());
                if (invokeResponse.getResponseCode() == HTTP_RESPONSE_CODE_TOO_MANY_REQUESTS){
                    break;
                }
            }
        }

        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_TOO_MANY_REQUESTS,
                "Response code mismatched. Invocation attempt:" + (SILVER_INVOCATION_LIMIT_PER_MIN + 1) +
                        " passed  during :" + (currentTime - startTime) + " milliseconds under Gold API  and Gold" +
                        " Application level tier and Plus Resource tier");
        assertTrue(invokeResponse.getData().contains(MESSAGE_THROTTLED_OUT_RESOURCE),
                "Response data mismatched. Invocation attempt:" + (SILVER_INVOCATION_LIMIT_PER_MIN + 1) +
                        " passed  during :" + (currentTime - startTime) + " milliseconds under Gold API  and Gold " +
                        "Application level tier and Plus Resource tier");

    }


    @Test(groups = {"wso2.am"}, description = "test  the  throttling of a API. API Tier :Gold, Application Tier: GOLD, " +
                                              "Resource Tier: Gold.", dependsOnMethods = "testInvokingWithAPIGoldTierApplicationGoldResourceSilver")
    public void testInvokingWithAPIGoldTierApplicationGoldResourceGold() throws Exception {
        initialize();
        Thread.sleep(THROTTLING_UNIT_TIME + THROTTLING_ADDITIONAL_WAIT_TIME);

        String swagger = " {\"paths\":{\"/*\":{\"get\":{\"x-auth-type\":\"Application \",\"x-throttling-tier\":" +
                "\"" + APIMIntegrationConstants.RESOURCE_TIER.ULTIMATE
                + "\",\"responses\":{\"200\":\"{}\"}}}},\"swagger\":\"2.0\",\"x-wso2-security\":{\"apim\"" +
                ":{\"x-wso2-scopes\":[]}},\"info\":{\"licence\":{},\"title\":\"" + API_NAME + "\",\"description\":" +
                "\"This is test API create by API manager integration test\",\"contact\":{\"email\":null,\"name\":null}," +
                "\"version\":\"" + API_VERSION_1_0_0 + "\"}}";

        apiPublisherClientUser1.updateResourceOfAPI(providerName, API_NAME, API_VERSION_1_0_0, swagger);

        apiStoreClientUser1.waitForSwaggerDocument(user.getUserName(), API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.RESOURCE_TIER.ULTIMATE, executionMode);

        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0, APIMIntegrationConstants.IS_API_EXISTS);

        long startTime = System.currentTimeMillis();
        long currentTime;
        for (int invocationCount = 1; invocationCount <= GOLD_INVOCATION_LIMIT_PER_MIN; invocationCount++) {
            currentTime = System.currentTimeMillis();
            //Invoke  API
            HttpResponse invokeResponse =
                    HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/" +
                                          API_END_POINT_METHOD, requestHeaders);
            assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                    "Response code mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                            (currentTime - startTime) + " milliseconds under Gold API  and Gold Application level tier" +
                            " and Ultimate Resource tier");
            assertTrue(invokeResponse.getData().contains(API_RESPONSE_DATA),
                    "Response data mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                            (currentTime - startTime) + " milliseconds under Gold API and Gold  Application level tier" +
                            " and Ultimate Resource tier");
        }
        currentTime = System.currentTimeMillis();
        HttpResponse invokeResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/" +
                                                            API_END_POINT_METHOD, requestHeaders);

        if (executionMode.equalsIgnoreCase(String.valueOf(ExecutionEnvironment.PLATFORM))) {
            while(true) {
                invokeResponse =
                        HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/" +
                                              API_END_POINT_METHOD, requestHeaders);
                System.out.println("response code XXXXX " + invokeResponse.getResponseCode());
                if (invokeResponse.getResponseCode() == HTTP_RESPONSE_CODE_TOO_MANY_REQUESTS){
                    break;
                }
            }
        }


        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_TOO_MANY_REQUESTS,
                "Response code mismatched. Invocation attempt:" + (GOLD_INVOCATION_LIMIT_PER_MIN + 1) +
                        " passed  during :" + (currentTime - startTime) + " milliseconds under Gold API  and Gold " +
                        "Application level tier and Ultimate Resource tier");
        assertTrue(invokeResponse.getData().contains(MESSAGE_THROTTLED_OUT_RESOURCE),
                "Response data mismatched. Invocation attempt:" + (GOLD_INVOCATION_LIMIT_PER_MIN + 1) +
                        " passed  during :" + (currentTime - startTime) + " milliseconds under Gold API  and Gold " +
                        "Application level tier and Ultimate Resource tier");
    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        apiStoreClientUser1.removeApplication(APPLICATION_NAME);
        deleteAPI(apiIdentifier, apiPublisherClientUser1);
    }


}
