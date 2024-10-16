/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.throttling;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.*;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.helpers.AdminApiTestHelper;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Test Application Throttle Policy Resetting for client credentials grant type
 */
public class ApplicationThrottlingResetTestCase extends APIMIntegrationBaseTest {

    private String displayName1 = "5PerHour Test Policy 1";
    private String policyName1 = "5PerHourTestPolicy1";
    private String description1 = "This is a request count test application throttle policy";
    private String displayName2 = "5PerHour Test Policy 1";
    private String policyName2 = "5PerHourTestPolicy2";
    private String description2 = "This is a bandwidth test application throttle policy";
    private String timeUnit = "min";
    private String dataUnit = "B";
    private Integer unitTime = 60;
    private ApplicationThrottlePolicyDTO applicationThrottlePolicyDTO1;
    private ApplicationThrottlePolicyDTO applicationThrottlePolicyDTO2;
    private String backendEP;
    private String apiId;
    private String APIName = "APIThrottleBurstAPI";
    private String APIVersion = "1.0.0";
    private String APIContext = "api_burst";
    private String applicationName1 = "request-count-limit-application";
    private String applicationName2 = "bandwidth-limit-application";
    private ApplicationDTO requestCountApplicationDTO;
    private ApplicationDTO bandwidthApplicationDTO;
    private String payload = "{\"payload\" : \"test\"}";
    private Map<String, String> requestHeadersForRequestCount = new HashMap<String, String>();
    private Map<String, String> requestHeadersForBandwidth = new HashMap<String, String>();
    private String apiInvocationUrlForRequestCount;
    private String apiInvocationUrlForBandwidth;

    @Factory(dataProvider = "userModeDataProvider")
    public ApplicationThrottlingResetTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_USER},
                new Object[]{TestUserMode.TENANT_USER},
                new Object[]{TestUserMode.TENANT_EMAIL_USER},
                };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        //Create the application throttling policy DTO with request count limit
        Long requestCount = 5L;
        RequestCountLimitDTO requestCountLimit = DtoFactory.createRequestCountLimitDTO(timeUnit, unitTime,
                requestCount);
        ThrottleLimitDTO defaultLimit1 = DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT,
                requestCountLimit, null);
        applicationThrottlePolicyDTO1 = DtoFactory.createApplicationThrottlePolicyDTO(policyName1, displayName1,
                description1, false, defaultLimit1);

        //Add the application throttling policy
        ApiResponse<ApplicationThrottlePolicyDTO> addedPolicy1 = restAPIAdmin.addApplicationThrottlingPolicy(
                applicationThrottlePolicyDTO1);

        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy1.getStatusCode(), HttpStatus.SC_CREATED);
        applicationThrottlePolicyDTO1 = addedPolicy1.getData();
        Assert.assertNotNull(applicationThrottlePolicyDTO1.getPolicyId(), "The policy ID cannot be null or empty");

        applicationThrottlePolicyDTO1.setIsDeployed(true);

        //Create the application throttling policy DTO with request count limit
        Long bandwidth = 150L;
        BandwidthLimitDTO bandWidthLimit = DtoFactory.createBandwidthLimitDTO(timeUnit, unitTime, bandwidth, dataUnit);
        ThrottleLimitDTO defaultLimit2 = DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.BANDWIDTHLIMIT,
                null, bandWidthLimit);
        applicationThrottlePolicyDTO2 = DtoFactory.createApplicationThrottlePolicyDTO(policyName2, displayName2,
                description2, false, defaultLimit2);

        //Add the application throttling policy
        ApiResponse<ApplicationThrottlePolicyDTO> addedPolicy2 = restAPIAdmin.addApplicationThrottlingPolicy(
                applicationThrottlePolicyDTO2);

        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy2.getStatusCode(), HttpStatus.SC_CREATED);
        applicationThrottlePolicyDTO2 = addedPolicy2.getData();
        Assert.assertNotNull(applicationThrottlePolicyDTO2.getPolicyId(), "The policy ID cannot be null or empty");
        applicationThrottlePolicyDTO2.setIsDeployed(true);

        // create api
        backendEP = gatewayUrlsWrk.getWebAppURLNhttp() + "response/";
        String url = backendEP;
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url), new URL(url));
        apiRequest.setVersion(APIVersion);
        apiRequest.setSandbox(url);
        apiRequest.setProvider(user.getUserName());
        apiRequest.setTiersCollection(Constants.TIERS_UNLIMITED);
        List<APIOperationsDTO> operations = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("POST");
        apiOperationsDTO.setTarget("/*");
        apiOperationsDTO.setAuthType("Application & Application User");
        apiOperationsDTO.setThrottlingPolicy("Unlimited");
        operations.add(apiOperationsDTO);
        apiRequest.setOperationsDTOS(operations);

        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = serviceResponse.getData();
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatus(apiId, Constants.PUBLISHED);
        waitForAPIDeploymentSync(user.getUserName(), APIName, APIVersion, APIMIntegrationConstants.IS_API_EXISTS);

        // check backend
        Map<String, String> requestHeaders = new HashMap<String, String>();
        HttpResponse response = HttpRequestUtil.doGet(backendEP, requestHeaders);
        Assert.assertEquals(response.getResponseCode(), 200, "Backend (dummy_api.xml) is not up and running");

        // Create an Application for request count limit
        requestCountApplicationDTO = restAPIStore.addApplication(applicationName1, policyName1, "",
                "this-is-test-application-for-request-count-limit");

        // Create an Application for bandwidth limit
        bandwidthApplicationDTO = restAPIStore.addApplication(applicationName2, policyName2, "",
                "this-is-test-application-for-bandwidth-limit");

        //subscribe to API with application with request count limit throttle policy
        SubscriptionDTO subscriptionDTO1 = restAPIStore.subscribeToAPI(apiId,
                requestCountApplicationDTO.getApplicationId(), Constants.TIERS_UNLIMITED);
        Assert.assertEquals(subscriptionDTO1.getThrottlingPolicy(), Constants.TIERS_UNLIMITED,
                "Error occurred " + "while subscribing to the api. Subscribed policy is not as expected as "
                        + Constants.TIERS_UNLIMITED);

        // generate keys and token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO1 = restAPIStore.generateKeys(requestCountApplicationDTO.getApplicationId(),
                "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                null, grantTypes);
        String accessToken1 = applicationKeyDTO1.getToken().getAccessToken();

        requestHeadersForRequestCount.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken1);
        requestHeadersForRequestCount.put("accept", "text/xml");
        requestHeadersForRequestCount.put("content-type", Constants.APPLICATION_JSON);
        apiInvocationUrlForRequestCount = getAPIInvocationURLHttps(APIContext + "/" + APIVersion + "/");

        //subscribe to API with application with bandwidth limit throttle policy
        SubscriptionDTO subscriptionDTO2 = restAPIStore.subscribeToAPI(apiId, bandwidthApplicationDTO.getApplicationId(),
                Constants.TIERS_UNLIMITED);
        Assert.assertEquals(subscriptionDTO2.getThrottlingPolicy(), Constants.TIERS_UNLIMITED,
                "Error occurred " + "while subscribing to the api. Subscribed policy is not as expected as "
                        + Constants.TIERS_UNLIMITED);

        // generate keys and token
        ApplicationKeyDTO applicationKeyDTO2 = restAPIStore.generateKeys(bandwidthApplicationDTO.getApplicationId(),
                "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                null, grantTypes);
        String accessToken2 = applicationKeyDTO2.getToken().getAccessToken();

        requestHeadersForBandwidth.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken2);
        requestHeadersForBandwidth.put("accept", "text/xml");
        requestHeadersForBandwidth.put("content-type", Constants.APPLICATION_JSON);
        apiInvocationUrlForBandwidth = getAPIInvocationURLHttps(APIContext + "/" + APIVersion + "/");
    }

    @Test(groups = { "wso2.am" }, description = "Test reset application throttling policy with request count limit")
    public void testResetPolicyWithRequestCountLimit() throws Exception {
        // invoke the api and check the throttling
        checkThrottling(apiInvocationUrlForRequestCount, requestHeadersForRequestCount, 5);

        String userId = requestCountApplicationDTO.getOwner();

        // reset the application policy
        org.wso2.am.integration.clients.store.api.ApiResponse<Void> resetResponse =
                restAPIStore.resetApplicationThrottlePolicy(
                requestCountApplicationDTO.getApplicationId(), userId);
        Assert.assertEquals(resetResponse.getStatusCode(), 200, "reset Application policy is not successful");
        Thread.sleep(5000);

        // invoke the api and check the throttling again to verify reset is happened
        checkThrottling(apiInvocationUrlForRequestCount, requestHeadersForRequestCount, 5);
    }

    @Test(groups = { "wso2.am" }, description = "Test reset application throttling policy with bandwidth limit")
    public void testResetPolicyWithBandwidthLimit() throws Exception {
        // invoke the api and check the throttling
        checkThrottling(apiInvocationUrlForBandwidth, requestHeadersForBandwidth, 5);

        String userId = bandwidthApplicationDTO.getOwner();

        // reset the application policy
        org.wso2.am.integration.clients.store.api.ApiResponse<Void> resetResponse =
                restAPIStore.resetApplicationThrottlePolicy(
                bandwidthApplicationDTO.getApplicationId(), userId);
        Assert.assertEquals(resetResponse.getStatusCode(), 200, "reset Application policy is not successful");
        Thread.sleep(5000);

        // invoke the api and check the throttling again to verify reset is happened
        checkThrottling(apiInvocationUrlForBandwidth, requestHeadersForBandwidth, 5);
    }

    private void checkThrottling(String invokeURL, Map<String, String> requestHeaders, int limit)
            throws IOException, InterruptedException {
        HttpResponse apiCallResponse;
        for (int count = 1; count < limit + 2; count++) {
            if (count > limit) {
                boolean isThrottled = false;
                for (; count < limit + 10; count++) { // invoke 10 more times to bear the throttle delay
                    apiCallResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, payload);
                    if (apiCallResponse.getResponseCode() == 429) {
                        isThrottled = true;
                        break;
                    }
                    Thread.sleep(500);
                }
                Assert.assertTrue(isThrottled, "Throttling hasn't happened at the expected count");
            } else {
                apiCallResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, payload);
                if (apiCallResponse.getResponseCode() == 429) {
                    Assert.fail(
                            "Throttling has happened at the count : " + count + ". But expected to throttle after the"
                                    + " request count " + limit);
                } else {
                    Assert.assertEquals(apiCallResponse.getResponseCode(),
                            org.apache.commons.httpclient.HttpStatus.SC_OK,
                            "API invocation Response code is not as expected");
                }
            }
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(requestCountApplicationDTO.getApplicationId());
        restAPIStore.deleteApplication(bandwidthApplicationDTO.getApplicationId());
        restAPIPublisher.deleteAPI(apiId);
        restAPIAdmin.deleteApplicationThrottlingPolicy(applicationThrottlePolicyDTO1.getPolicyId());
        restAPIAdmin.deleteApplicationThrottlingPolicy(applicationThrottlePolicyDTO2.getPolicyId());
        super.cleanUp();
    }
}
