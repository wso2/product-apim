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
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
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
 * Publish a API under gold tier and test the invocation with throttling , then change the api tier to silver
 * and do a new silver subscription and test invocation under Silver tier.
 */
public class ChangeAPITierAndTestInvokingTestCase extends APIManagerLifecycleBaseTest {
    private final String API_NAME = "ChangeAPITierAndTestInvokingTest";
    private final String API_CONTEXT = "ChangeAPITierAndTestInvoking";
    private final String API_TAGS = "testTag1, testTag2, testTag3";

    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_END_POINT_METHOD = "customers/123";
    private final String API_RESPONSE_DATA = "<id>123</id><name>John</name></Customer>";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "ChangeAPITierAndTestInvokingTestCase";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String applicationNameGold;
    private String applicationNameSilver;
    private Map<String, String> requestHeadersGoldTier;
    private APIIdentifier apiIdentifier;
    private String providerName;
    private APICreationRequestBean apiCreationRequestBean;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException, XPathExpressionException {
        super.init();
        apiEndPointUrl = getGatewayURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);
        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(user.getUserName(), user.getPassword());
        //Login to API Store with  admin
        apiStoreClientUser1.login(user.getUserName(), user.getPassword());
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
    }


    @Test(groups = {"wso2.am"}, description = "test  invocation of  api under tier Gold.")
    public void testInvokingWithGoldTier() throws Exception {

        applicationNameGold = APPLICATION_NAME + TIER_GOLD;
        apiStoreClientUser1
                .addApplication(applicationNameGold, APIMIntegrationConstants.APPLICATION_TIER.LARGE, "", "");
        apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                                           new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiCreationRequestBean.setTier(TIER_GOLD);
        createPublishAndSubscribeToAPI(
                apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1, apiStoreClientUser1, applicationNameGold);

        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0, APIMIntegrationConstants.IS_API_EXISTS);

        //get access token
        String accessToken = generateApplicationKeys(apiStoreClientUser1, applicationNameGold).getAccessToken();

        // Create requestHeaders
        requestHeadersGoldTier = new HashMap<String, String>();
        requestHeadersGoldTier.put("Authorization", "Bearer " + accessToken);
        requestHeadersGoldTier.put("accept", "text/xml");
        long startTime = System.currentTimeMillis();
        long currentTime;
        for (int invocationCount = 1; invocationCount <= GOLD_INVOCATION_LIMIT_PER_MIN; invocationCount++) {
            currentTime = System.currentTimeMillis();
            //Invoke  API
            HttpResponse invokeResponse =
                    HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/" +
                                          API_END_POINT_METHOD, requestHeadersGoldTier);
            assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                         "Response code mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                         (currentTime - startTime) + " milliseconds under Gold API level tier");
            assertTrue(invokeResponse.getData().contains(API_RESPONSE_DATA),
                       "Response data mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                       (currentTime - startTime) + " milliseconds under Gold API level tier");
        }


        currentTime = System.currentTimeMillis();
        HttpResponse invokeResponse;
        invokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/" +
                                      API_END_POINT_METHOD, requestHeadersGoldTier);

        //send the request again if the test runs on a cluster. It will be 1 attempt more to get the api blocked.
        if (executionMode.equalsIgnoreCase(String.valueOf(ExecutionEnvironment.PLATFORM))) {
            invokeResponse =
                    HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/" +
                                          API_END_POINT_METHOD, requestHeadersGoldTier);
        }

        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_TOO_MANY_REQUESTS,
                     "Response code mismatched. Invocation attempt:" + (GOLD_INVOCATION_LIMIT_PER_MIN + 1) +
                     " passed  during :" + (currentTime - startTime) + " milliseconds under Gold API level tier");
        assertTrue(invokeResponse.getData().contains(MESSAGE_THROTTLED_OUT),
                   "Response data mismatched. Invocation attempt:" + (GOLD_INVOCATION_LIMIT_PER_MIN + 1) +
                   " passed  during :" + (currentTime - startTime) + " milliseconds under Gold API level tier");
    }


    @Test(groups = {"wso2.am"}, description = "test  invocation of APi after expire the throttling block time.",
          dependsOnMethods = "testInvokingWithGoldTier")
    public void testInvokingAfterExpireThrottleExpireTime() throws Exception {
        //wait millisecond to expire the throttling block
        Thread.sleep(THROTTLING_UNIT_TIME + THROTTLING_ADDITIONAL_WAIT_TIME);
        HttpResponse invokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/" +
                                      API_END_POINT_METHOD, requestHeadersGoldTier);
        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                     "Response code mismatched, " +
                     "Invocation fails after wait " + (THROTTLING_UNIT_TIME + THROTTLING_ADDITIONAL_WAIT_TIME) +
                     "millisecond to expire the throttling block");

        assertTrue(invokeResponse.getData().contains(API_RESPONSE_DATA),
                   "Response data mismatched. " +
                   "Invocation fails after wait " + (THROTTLING_UNIT_TIME + THROTTLING_ADDITIONAL_WAIT_TIME) +
                   "millisecond to expire the throttling block");
    }

    @Test(groups = {"wso2.am"}, description = "Test changing of the API Tier from Gold to Silver",
          dependsOnMethods = "testInvokingAfterExpireThrottleExpireTime")
    public void testEditAPITierToSilver()
            throws APIManagerIntegrationTestException, MalformedURLException {
        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                                                            new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiCreationRequestBean.setTier(TIER_SILVER);
        apiCreationRequestBean.setTiersCollection(TIER_SILVER);
        //Update API with Edited information with Tier Silver
        HttpResponse updateAPIHTTPResponse = apiPublisherClientUser1.updateAPI(apiCreationRequestBean);
        assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                     "Update API Response Code is" +
                     " invalid. Updating of API information fail" + getAPIIdentifierString(apiIdentifier));

        assertEquals(getValueFromJSON(updateAPIHTTPResponse, "error"), "false",
                     "Error in API Update in " +
                     getAPIIdentifierString(apiIdentifier) + "Response Data:" + updateAPIHTTPResponse.getData());

    }


    @Test(groups = {"wso2.am"}, description = "test  invocation of  api under tier Silver.",
          dependsOnMethods = "testEditAPITierToSilver")
    public void testInvokingWithSilverTier() throws Exception {
        applicationNameSilver = APPLICATION_NAME + TIER_SILVER;
        // create new application
        apiStoreClientUser1
                .addApplication(applicationNameSilver, APIMIntegrationConstants.APPLICATION_TIER.LARGE, "", "");
        apiIdentifier.setTier(TIER_SILVER);
        // Do a API Silver subscription.
        subscribeToAPI(apiIdentifier, applicationNameSilver, apiStoreClientUser1);
        //get access token
        String accessToken = generateApplicationKeys(apiStoreClientUser1, applicationNameSilver).getAccessToken();
        // Create requestHeaders
        Map<String, String> requestHeadersSilverTier = new HashMap<String, String>();
        requestHeadersSilverTier.put("accept", "text/xml");
        requestHeadersSilverTier.put("Authorization", "Bearer " + accessToken);
        //millisecond to expire the throttling block
        Thread.sleep(THROTTLING_UNIT_TIME + THROTTLING_ADDITIONAL_WAIT_TIME);
        long startTime = System.currentTimeMillis();
        long currentTime;
        for (int invocationCount = 1; invocationCount <= SILVER_INVOCATION_LIMIT_PER_MIN; invocationCount++) {
            currentTime = System.currentTimeMillis();
            //Invoke  API
            HttpResponse invokeResponse =
                    HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/" +
                                          API_END_POINT_METHOD, requestHeadersSilverTier);
            assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                         "Response code mismatched. " +
                         "Invocation attempt:" + invocationCount + " failed  during :" + (currentTime - startTime) +
                         " milliseconds under Silver API level tier");
            assertTrue(invokeResponse.getData().contains(API_RESPONSE_DATA),
                       "Response data mismatched." +
                       " Invocation attempt:" + invocationCount + " failed  during :" + (currentTime - startTime) +
                       " milliseconds under Silver API level tier");
        }
        HttpResponse invokeResponse;
        currentTime = System.currentTimeMillis();
        invokeResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/" +
                                               API_END_POINT_METHOD, requestHeadersSilverTier);

        if (executionMode.equalsIgnoreCase(String.valueOf(ExecutionEnvironment.PLATFORM))) {
            invokeResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/" +
                                                   API_END_POINT_METHOD, requestHeadersSilverTier);
        }

        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_TOO_MANY_REQUESTS,
                     "Response code mismatched. Invocation attempt:" + (SILVER_INVOCATION_LIMIT_PER_MIN + 1) +
                     " passed  during :" + (currentTime - startTime) + " milliseconds under Silver API level tier");

        assertTrue(invokeResponse.getData().contains(MESSAGE_THROTTLED_OUT),
                   "Response data mismatched. Invocation attempt:" + (SILVER_INVOCATION_LIMIT_PER_MIN + 1) +
                   " passed  during :" + (currentTime - startTime) + " milliseconds under Silver API level tier");
    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException {
        apiStoreClientUser1.removeApplication(applicationNameGold);
        apiStoreClientUser1.removeApplication(applicationNameSilver);
        deleteAPI(apiIdentifier, apiPublisherClientUser1);
    }
}
