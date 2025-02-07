/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.CustomAttributeDTO;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyPermissionDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test Burst control/rate limiting
 */
public class BurstControlTestCase  extends APIManagerLifecycleBaseTest {
    private String subscriptionTier5RPMburst = "SubscriptionTier5RPMburst";
    private String subscriptionTier25RPMburst = "SubscriptionTier25RPMburst";

    private SubscriptionThrottlePolicyDTO subscriptionThrottlePolicyDTO1;
    private SubscriptionThrottlePolicyDTO subscriptionThrottlePolicyDTO2;

    private String rateLimitTimeUnit = "min";
    private boolean stopQuotaOnReach = true;
    private String billingPlan = "COMMERCIAL";
    private Integer subscriberCount = 0;
    private List<CustomAttributeDTO> customAttributes = null;
    private int burstLimit1 = 5;
    private int burstLimit2 = 25;
    private String backendEP;
    private String apiId;
    private String APIName = "APIThrottleBurstAPI";
    private String APIVersion = "1.0.0";
    private String APIContext = "api_burst";
    private String applicationName = "APIThrottleBurst-application";
    private ApplicationDTO applicationDTO;
    private final Log log = LogFactory.getLog(BurstControlTestCase.class);
    private String payload = "{\"payload\" : \"test\"}";

    @Factory(dataProvider = "userModeDataProvider")
    public BurstControlTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                                        new Object[] { TestUserMode.TENANT_ADMIN }, };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        String INTERNAL_EVERYONE= "Internal/everyone";
        List<String> roleList = new ArrayList<>();
        SubscriptionThrottlePolicyPermissionDTO permissions;
        // Add subscription policy - 1
        RequestCountLimitDTO requestCountLimit =
                                        DtoFactory.createRequestCountLimitDTO("min", 1, 1000L);
        ThrottleLimitDTO defaultLimit =
                                        DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, requestCountLimit, null);
        roleList.add(INTERNAL_EVERYONE);
        permissions = DtoFactory.
                createSubscriptionThrottlePolicyPermissionDTO(SubscriptionThrottlePolicyPermissionDTO.
                        PermissionTypeEnum.ALLOW, roleList);
        subscriptionThrottlePolicyDTO1 = DtoFactory.createSubscriptionThrottlePolicyDTO(subscriptionTier5RPMburst,
                                        "subscriptionTier5RPMburst",
                                        "1000 request per min with burst of 5 requests per minute",
                                        false, defaultLimit, 0, 0, burstLimit1,
                                        rateLimitTimeUnit, customAttributes, stopQuotaOnReach, billingPlan,
                                        subscriberCount, permissions);
        ApiResponse<SubscriptionThrottlePolicyDTO> apiResponse =
                                        restAPIAdmin.addSubscriptionThrottlingPolicy(subscriptionThrottlePolicyDTO1);
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_CREATED);
        subscriptionThrottlePolicyDTO1 = apiResponse.getData();

        // Add subscription policy - 2
        subscriptionThrottlePolicyDTO2 = DtoFactory.createSubscriptionThrottlePolicyDTO(subscriptionTier25RPMburst,
                                        "subscriptionTier25RPMburst",
                                        "1000 request per min with burst of 25 requests per minute",
                                        false, defaultLimit, 0, 0, burstLimit2,
                                        rateLimitTimeUnit, customAttributes, stopQuotaOnReach, billingPlan,
                                        subscriberCount, permissions);
        apiResponse = restAPIAdmin.addSubscriptionThrottlingPolicy(subscriptionThrottlePolicyDTO2);
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_CREATED);
        subscriptionThrottlePolicyDTO2 = apiResponse.getData();

        // create api
        backendEP = gatewayUrlsWrk.getWebAppURLNhttp() + "response/";
        String url = backendEP;
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url), new URL(url));
        apiRequest.setVersion(APIVersion);
        apiRequest.setSandbox(url);
        apiRequest.setProvider(user.getUserName());
        apiRequest.setTiersCollection(Constants.TIERS_UNLIMITED + "," + subscriptionTier5RPMburst + ","
                                        + subscriptionTier25RPMburst);
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

        // Create an Application
        applicationDTO = restAPIStore.addApplication(applicationName,
                                        APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "this-is-test");
    }

    @Test(groups = { "wso2.am" }, description = "Test changing the burst limit of an API subscription by subscribing "
                                    + "to a different subscription policy with different burst limit")
    public void testBurstLimitChange() throws Exception {
        //subscribe to API
        SubscriptionDTO subscriptionDTO1 = restAPIStore.subscribeToAPI(apiId, applicationDTO.getApplicationId(),
                                        subscriptionTier5RPMburst);
        Assert.assertTrue(subscriptionDTO1.getThrottlingPolicy().equals(subscriptionTier5RPMburst), "Error occurred "
                                        + "while subscribing to the api. Subscribed policy is not as expected as "
                                        + subscriptionTier5RPMburst);

        // generate keys and token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(), "3600", null,
                                        ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("content-type", "application/json");
        String apiInvocationUrl = getAPIInvocationURLHttps(APIContext + "/" + APIVersion + "/");

        // verify burst control on subscription tier "subscriptionTier5RPMburst"
        checkThrottling(apiInvocationUrl, requestHeaders, burstLimit1);

        // remove previous subscription
        log.info("Old subscription id:" + subscriptionDTO1.getSubscriptionId());
        HttpResponse httpResponse = restAPIStore.removeSubscription(subscriptionDTO1);
        log.info("httpResponse of removeSubscription ====== : " + httpResponse);
        log.info("AAA= " + subscriptionDTO1);
        Thread.sleep(5000);

        // add new subscription
        SubscriptionDTO subscriptionDTO2 = restAPIStore.subscribeToAPI(apiId, applicationDTO.getApplicationId(),
                                        subscriptionTier25RPMburst);
        Assert.assertTrue(subscriptionDTO2.getThrottlingPolicy().equals(subscriptionTier25RPMburst), "Error occurred "
                                        + "while subscribing to the api. Subscribed policy is not as expected as "
                                        + subscriptionTier25RPMburst);
        Thread.sleep(60000); // wait until throttled period ends
        // verify burst control on subscription tier "subscriptionTier25RPMburst"
        checkThrottling(apiInvocationUrl, requestHeaders, burstLimit2);
    }

    private void checkThrottling(String invokeURL, Map<String, String> requestHeaders, int limit)
                                    throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();
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
                Assert.assertTrue(isThrottled, "Throttling has't happened at the expected count");
            } else {
                apiCallResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, payload);
                if (apiCallResponse.getResponseCode() == 429) {
                    Assert.fail("Throttling has happened at the count : " + count
                                                    + ". But expected to throttle after the request count " + limit);
                } else {
                    Assert.assertEquals(apiCallResponse.getResponseCode(), org.apache.commons.httpclient.HttpStatus.SC_OK,
                                                    "API invocation Response code is not as expected");
                }
            }
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationDTO.getApplicationId());
        restAPIPublisher.deleteAPI(apiId);
        restAPIAdmin.deleteSubscriptionThrottlingPolicy(subscriptionThrottlePolicyDTO1.getPolicyId());
        restAPIAdmin.deleteSubscriptionThrottlingPolicy(subscriptionThrottlePolicyDTO2.getPolicyId());
        super.cleanUp();
    }
}
