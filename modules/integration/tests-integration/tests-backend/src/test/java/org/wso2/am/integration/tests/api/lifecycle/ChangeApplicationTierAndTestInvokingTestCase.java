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
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Change the API Application tier and test the throttling.
 */
public class ChangeApplicationTierAndTestInvokingTestCase extends APIManagerLifecycleBaseTest {

    private final String API_NAME = "ChangeApplicationTierAndTestInvokingTest";
    private final String API_CONTEXT = "ChangeApplicationTierAndTestInvoking";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_END_POINT_METHOD = "customers/123";
    private final String API_RESPONSE_DATA = "<id>123</id><name>John</name></Customer>";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "ChangeApplicationTierAndTestInvokingTestCase";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String providerName;
    private APIIdentifier apiIdentifier;
    private String applicationNameGold;
    private String applicationNameSilver;
    private Map<String, String> requestHeaders;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private APICreationRequestBean apiCreationRequestBean;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException, XPathExpressionException, MalformedURLException {
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

    }


    @Test(groups = {"wso2.am"}, description = "test  invocation of  api under  API tier Gold  and Application Tire Silver.")
    public void testInvokingWithAPIGoldTierApplicationSilver() throws Exception {
        applicationNameSilver = APPLICATION_NAME + TIER_SILVER;
        apiStoreClientUser1.addApplication(applicationNameSilver, TIER_SILVER, "", "");
        //Create publish and subscribe a API
        APIIdentifier apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiIdentifier.setTier(TIER_GOLD);
        createPublishAndSubscribeToAPI(
                apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1, apiStoreClientUser1,
                applicationNameSilver);
        //get access token
        String accessToken = generateApplicationKeys(apiStoreClientUser1, applicationNameSilver).getAccessToken();

        // Create requestHeaders
        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        long startTime = System.currentTimeMillis();
        long currentTime;
        for (int invocationCount = 1; invocationCount <= SILVER_INVOCATION_LIMIT_PER_MIN; invocationCount++) {
            currentTime = System.currentTimeMillis();
            //Invoke  API
            HttpResponse invokeResponse =
                    HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0)  + "/" +
                                          API_END_POINT_METHOD, requestHeaders);
            assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                    "Response code mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                            (currentTime - startTime) + " milliseconds under Gold API  and Silver Application level tier");
            assertTrue(invokeResponse.getData().contains(API_RESPONSE_DATA),
                    "Response data mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                            (currentTime - startTime) + " milliseconds under Gold API and Silver Application level tier");
        }
        currentTime = System.currentTimeMillis();
        HttpResponse invokeResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0)  + "/" +
                API_END_POINT_METHOD, requestHeaders);
        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_SERVICE_UNAVAILABLE,
                "Response code mismatched. Invocation attempt:" + (SILVER_INVOCATION_LIMIT_PER_MIN + 1) +
                        " passed  during :" + (currentTime - startTime) + " milliseconds under Gold API level tier");
        assertTrue(invokeResponse.getData().contains(MESSAGE_THROTTLED_OUT),
                "Response data mismatched. Invocation attempt:" + (SILVER_INVOCATION_LIMIT_PER_MIN + 1) +
                        " passed  during :" + (currentTime - startTime) + " milliseconds under Gold API level tier");
    }


    @Test(groups = {"wso2.am"}, description = "test  invocation of  api under API tier Gold  and Application Tire Gold..",
            dependsOnMethods = "testInvokingWithAPIGoldTierApplicationSilver")
    public void testInvokingWithAPIGoldTierApplicationGold() throws Exception,
            InterruptedException, IOException {
        applicationNameGold = APPLICATION_NAME + TIER_GOLD;
        apiStoreClientUser1.updateApplication(applicationNameSilver, applicationNameGold, "", "", TIER_GOLD);
        //Sleep until throttling unit time is passed.
        Thread.sleep(THROTTLING_UNIT_TIME + THROTTLING_ADDITIONAL_WAIT_TIME);
        long startTime = System.currentTimeMillis();
        long currentTime;
        for (int invocationCount = 1; invocationCount <= GOLD_INVOCATION_LIMIT_PER_MIN; invocationCount++) {
            currentTime = System.currentTimeMillis();
            //Invoke  API
            HttpResponse invokeResponse =
                    HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) +  "/" +
                                          API_END_POINT_METHOD, requestHeaders);
            assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                    "Response code mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                            (currentTime - startTime) + " milliseconds under Gold API  and Gold Application level tier");
            assertTrue(invokeResponse.getData().contains(API_RESPONSE_DATA),
                    "Response data mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                            (currentTime - startTime) + " milliseconds under Gold API and Gold Application level tier");
        }
        currentTime = System.currentTimeMillis();
        HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0)  + "/" +
                API_END_POINT_METHOD, requestHeaders);
        HttpResponse invokeResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0)  + "/" +
                API_END_POINT_METHOD, requestHeaders);
        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_SERVICE_UNAVAILABLE, "Response code mismatched." +
                " Invocation attempt:" + (GOLD_INVOCATION_LIMIT_PER_MIN + 1) + " passed  during :" +
                (currentTime - startTime) + " milliseconds under Gold API level tier and Gold Application level tier");
        assertTrue(invokeResponse.getData().contains(MESSAGE_THROTTLED_OUT), "Response data mismatched. Invocation attempt:"
                + (GOLD_INVOCATION_LIMIT_PER_MIN + 1) + " passed  during :" + (currentTime - startTime) +
                " milliseconds under Gold API level tier " + "and Gold Application level tier");
    }


    @Test(groups = {"wso2.am"}, description = "test  invocation of  api under API tier Gold  and Application Tire Silver." +
            "Change the Application tire to silver and test ", dependsOnMethods = "testInvokingWithAPIGoldTierApplicationGold")
    public void testInvokingWithAPIGoldTierApplicationSilverFor2ndTime() throws Exception,
            InterruptedException, IOException {
        applicationNameSilver = APPLICATION_NAME + TIER_SILVER;
        apiStoreClientUser1.updateApplication(applicationNameGold, applicationNameSilver, "", "", TIER_SILVER);
        //Sleep until throttling unit time is passed.
        Thread.sleep(THROTTLING_UNIT_TIME + THROTTLING_ADDITIONAL_WAIT_TIME);
        long startTime = System.currentTimeMillis();
        long currentTime;
        for (int invocationCount = 1; invocationCount <= SILVER_INVOCATION_LIMIT_PER_MIN; invocationCount++) {
            currentTime = System.currentTimeMillis();
            //Invoke  API
            HttpResponse invokeResponse =
                    HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0)  + "/" +
                                          API_END_POINT_METHOD, requestHeaders);
            assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                    "Response code mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                            (currentTime - startTime) + " milliseconds under Gold API  and Gold Application level tier");
            assertTrue(invokeResponse.getData().contains(API_RESPONSE_DATA),
                    "Response data mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                            (currentTime - startTime) + " milliseconds under Gold API and Gold Application level tier");
        }
        currentTime = System.currentTimeMillis();
        HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0)  + "/" +
                              API_END_POINT_METHOD, requestHeaders);
        HttpResponse invokeResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0)  + "/" +
                                                            API_END_POINT_METHOD, requestHeaders);
        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_SERVICE_UNAVAILABLE, "Response code mismatched." +
                " Invocation attempt:" + (SILVER_INVOCATION_LIMIT_PER_MIN + 1) + " passed  during :" +
                (currentTime - startTime) + " milliseconds under Gold API level tier and Gold Application level tier");
        assertTrue(invokeResponse.getData().contains(MESSAGE_THROTTLED_OUT), "Response data mismatched. Invocation attempt:"
                + (SILVER_INVOCATION_LIMIT_PER_MIN + 1) + " passed  during :" + (currentTime - startTime) +
                " milliseconds under Gold API level tier " + "and Gold Application level tier");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException {
        apiStoreClientUser1.removeApplication(applicationNameSilver);
        deleteAPI(apiIdentifier, apiPublisherClientUser1);
    }


}
