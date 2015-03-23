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
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Change the API resource tier and test the throttling.
 */
public class ChangeResourceTierAndTestInvokingTestCase extends APIManagerLifecycleBaseTest {

    private APIIdentifier apiIdentifier;
    private static final String APPLICATION_NAME = "ChangeResourceTierAndTestInvokingTestCase";
    private Map<String, String> requestHeaders;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        apiIdentifier = new APIIdentifier(USER_NAME1, API1_NAME, API_VERSION_1_0_0);

    }


    @Test(groups = {"wso2.am"}, description = "test  the  throttling of a API. API Tier :Gold, Application Tier: GOLD, " +
            "Resource Tier: Unlimited.")
    public void testInvokingWithAPIGoldTierApplicationGoldResourceUnlimited() throws Exception {
        //Create application
        apiStoreClientUser1.addApplication(APPLICATION_NAME, TIER_GOLD, "", "");
        //Create publish and subscribe a API
        APIIdentifier apiIdentifier = new APIIdentifier(USER_NAME1, API1_NAME, API_VERSION_1_0_0);
        apiIdentifier.setTier(TIER_GOLD);
        createPublishAndSubscribeToAPI(
                apiIdentifier, API1_CONTEXT, apiPublisherClientUser1, apiStoreClientUser1, APPLICATION_NAME);

        //get access token
        String accessToken = getAccessToken(apiStoreClientUser1, APPLICATION_NAME);

        // Create requestHeaders
        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        long startTime = System.currentTimeMillis();
        long currentTime;
        for (int invocationCount = 1; invocationCount <= GOLD_INVOCATION_LIMIT_PER_MIN; invocationCount++) {
            currentTime = System.currentTimeMillis();
            //Invoke  API
            HttpResponse invokeResponse =
                    HttpRequestUtil.doGet(API_BASE_URL + API1_CONTEXT + "/" + API_VERSION_1_0_0 +
                            API1_END_POINT_METHOD, requestHeaders);
            assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                    "Response code mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                            (currentTime - startTime) + " milliseconds under Gold API , Gold Application level tier" +
                            " and Unlimited Resource tier");
            assertTrue(invokeResponse.getData().contains(API1_RESPONSE_DATA),
                    "Response data mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                            (currentTime - startTime) + " milliseconds under Gold API , Gold Application level tier" +
                            "  and Unlimited Resource tier");

        }

        currentTime = System.currentTimeMillis();


        HttpResponse invokeResponse = HttpRequestUtil.doGet(API_BASE_URL + API1_CONTEXT + "/" + API_VERSION_1_0_0 +
                API1_END_POINT_METHOD, requestHeaders);
        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_SERVICE_UNAVAILABLE,
                "Response code mismatched. Invocation attempt:" + (GOLD_INVOCATION_LIMIT_PER_MIN + 1) +
                        " passed  during :" + (currentTime - startTime) + " milliseconds under Gold API , " +
                        "Gold Application level tier and Unlimited Resource tier.");
        assertTrue(invokeResponse.getData().contains(MESSAGE_THROTTLED_OUT),
                "Response data mismatched. Invocation attempt:" + (GOLD_INVOCATION_LIMIT_PER_MIN + 1) +
                        " passed  during :" + (currentTime - startTime) + " milliseconds under Gold API , " +
                        "Gold Application level tier and Unlimited Resource tier.");


    }


    @Test(groups = {"wso2.am"}, description = "test  the  throttling of a API. API Tier :Gold, Application Tier: GOLD, " +
            "Resource Tier: Silver.", dependsOnMethods = "testInvokingWithAPIGoldTierApplicationGoldResourceUnlimited")
    public void testInvokingWithAPIGoldTierApplicationGoldResourceSilver() throws Exception {

        Thread.sleep(THROTTLING_UNIT_TIME + THROTTLING_ADDITIONAL_WAIT_TIME);

        String swagger = "{\"apiVersion\":\"" + API_VERSION_1_0_0 + "\",\"swaggerVersion\":\"1.2\",\"authorizations\":{\"oauth2\":" +
                "{\"scopes\":[],\"type\":\"oauth2\"}},\"apis\":[{\"file\":{\"apiVersion\":\"1.0.0\",\"basePath\":" +
                "\"" + getGatewayServerURLHttp() + "/" + API1_CONTEXT + "/" + API_VERSION_1_0_0 + "\",\"resourcePath\":" +
                "\"/default\",\"swaggerVersion\":\"1.2\",\"authorizations\":{\"oauth2\":{\"scopes\":[],\"type\":\"" +
                "oauth2\"}},\"apis\":[{\"path\":\"/*\",\"operations\":[{\"auth_type\":\"Application \",\"throttling_tier\":" +
                "\"Silver\",\"method\":\"GET\",\"parameters\":[]}]}],\"info\":{\"termsOfServiceUrl\":\"\"," +
                "\"title\":\"\",\"description\":\"\",\"license\":\"\",\"contact\":\"\",\"licenseUrl\":\"\"}}," +
                "\"description\":\"\",\"path\":\"/default\"}],\"info\":{\"termsOfServiceUrl\":\"\",\"title\":\"\"," +
                "\"description\":\"This is test API create by API manager integration test\",\"license\":\"\",\"contact\":" +
                "\"\",\"licenseUrl\":\"\"}}";

        apiPublisherClientUser1.updateResourceOfAPI(USER_NAME1, API1_NAME, API_VERSION_1_0_0, swagger);


        long startTime = System.currentTimeMillis();
        long currentTime;
        for (int invocationCount = 1; invocationCount <= SILVER_INVOCATION_LIMIT_PER_MIN; invocationCount++) {
            currentTime = System.currentTimeMillis();
            //Invoke  API
            HttpResponse invokeResponse =
                    HttpRequestUtil.doGet(API_BASE_URL + API1_CONTEXT + "/" + API_VERSION_1_0_0 +
                            API1_END_POINT_METHOD, requestHeaders);
            assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                    "Response code mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                            (currentTime - startTime) + " milliseconds under Gold API  and Gold Application level tier" +
                            "  and Silver Resource tier");
            assertTrue(invokeResponse.getData().contains(API1_RESPONSE_DATA),
                    "Response data mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                            (currentTime - startTime) + " milliseconds under Gold API and Gold Application level tier" +
                            " and Silver Resource tier");

        }

        currentTime = System.currentTimeMillis();

        HttpResponse invokeResponse = HttpRequestUtil.doGet(API_BASE_URL + API1_CONTEXT + "/" + API_VERSION_1_0_0 +
                API1_END_POINT_METHOD, requestHeaders);
        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_SERVICE_UNAVAILABLE,
                "Response code mismatched. Invocation attempt:" + (SILVER_INVOCATION_LIMIT_PER_MIN + 1) +
                        " passed  during :" + (currentTime - startTime) + " milliseconds under Gold API  and Gold" +
                        " Application level tier and Silver Resource tier");
        assertTrue(invokeResponse.getData().contains(MESSAGE_THROTTLED_OUT),
                "Response data mismatched. Invocation attempt:" + (SILVER_INVOCATION_LIMIT_PER_MIN + 1) +
                        " passed  during :" + (currentTime - startTime) + " milliseconds under Gold API  and Gold " +
                        "Application level tier and Silver Resource tier");


    }


    @Test(groups = {"wso2.am"}, description = "test  the  throttling of a API. API Tier :Gold, Application Tier: GOLD, " +
            "Resource Tier: Gold.", dependsOnMethods = "testInvokingWithAPIGoldTierApplicationGoldResourceSilver")
    public void testInvokingWithAPIGoldTierApplicationGoldResourceGold() throws Exception {


        Thread.sleep(THROTTLING_UNIT_TIME + THROTTLING_ADDITIONAL_WAIT_TIME);


        String swagger = "{\"apiVersion\":\"" + API_VERSION_1_0_0 + "\",\"swaggerVersion\":\"1.2\",\"authorizations\":{\"oauth2\":" +
                "{\"scopes\":[],\"type\":\"oauth2\"}},\"apis\":[{\"file\":{\"apiVersion\":\"1.0.0\",\"basePath\":" +
                "\"" + getGatewayServerURLHttp() + "/" + API1_CONTEXT + "/" + API_VERSION_1_0_0 + "\",\"resourcePath\":" +
                "\"/default\",\"swaggerVersion\":\"1.2\",\"authorizations\":{\"oauth2\":{\"scopes\":[],\"type\":\"" +
                "oauth2\"}},\"apis\":[{\"path\":\"/*\",\"operations\":[{\"auth_type\":\"Application \",\"throttling_tier\":" +
                "\"Gold\",\"method\":\"GET\",\"parameters\":[]}]}],\"info\":{\"termsOfServiceUrl\":\"\"," +
                "\"title\":\"\",\"description\":\"\",\"license\":\"\",\"contact\":\"\",\"licenseUrl\":\"\"}}," +
                "\"description\":\"\",\"path\":\"/default\"}],\"info\":{\"termsOfServiceUrl\":\"\",\"title\":\"\"," +
                "\"description\":\"This is test API create by API manager integration test\",\"license\":\"\",\"contact\":" +
                "\"\",\"licenseUrl\":\"\"}}";


        apiPublisherClientUser1.updateResourceOfAPI(USER_NAME1, API1_NAME, API_VERSION_1_0_0, swagger);


        long startTime = System.currentTimeMillis();
        long currentTime;
        for (int invocationCount = 1; invocationCount <= GOLD_INVOCATION_LIMIT_PER_MIN; invocationCount++) {
            currentTime = System.currentTimeMillis();
            //Invoke  API
            HttpResponse invokeResponse =
                    HttpRequestUtil.doGet(API_BASE_URL + API1_CONTEXT + "/" + API_VERSION_1_0_0 +
                            API1_END_POINT_METHOD, requestHeaders);
            assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                    "Response code mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                            (currentTime - startTime) + " milliseconds under Gold API  and Gold Application level tier" +
                            " and Gold Resource tier");
            assertTrue(invokeResponse.getData().contains(API1_RESPONSE_DATA),
                    "Response data mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                            (currentTime - startTime) + " milliseconds under Gold API and Gold  Application level tier" +
                            " and Gold Resource tier");

        }
        currentTime = System.currentTimeMillis();


        HttpResponse invokeResponse = HttpRequestUtil.doGet(API_BASE_URL + API1_CONTEXT + "/" + API_VERSION_1_0_0 +
                API1_END_POINT_METHOD, requestHeaders);
        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_SERVICE_UNAVAILABLE,
                "Response code mismatched. Invocation attempt:" + (GOLD_INVOCATION_LIMIT_PER_MIN + 1) +
                        " passed  during :" + (currentTime - startTime) + " milliseconds under Gold API  and Gold " +
                        "Application level tier and Gold Resource tier");
        assertTrue(invokeResponse.getData().contains(MESSAGE_THROTTLED_OUT),
                "Response data mismatched. Invocation attempt:" + (GOLD_INVOCATION_LIMIT_PER_MIN + 1) +
                        " passed  during :" + (currentTime - startTime) + " milliseconds under Gold API  and Gold " +
                        "Application level tier and Gold Resource tier");


    }


    @AfterClass(alwaysRun = true)
    public void cleanup() throws Exception {
        apiStoreClientUser1.removeApplication(APPLICATION_NAME);
        deleteAPI(apiIdentifier, apiPublisherClientUser1);

    }


}
