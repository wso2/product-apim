/*
 *Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */
package org.wso2.am.integration.tests.throttling;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationTokenDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.AdminDashboardRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubscriptionThrottlePolicyUpdateTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(SubscriptionThrottlePolicyUpdateTestCase.class.getName());
    private AdminDashboardRestClient adminDashboardRestClient;
    private String apiId;
    private String gatewayUrl;
    private String app2Id;
    private String backendEP;
    private String body = "{\"payload\" : \"00000000000000000000000000000000000000000000000000000000000000000000"
            + "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"
            + "00000000000000000000000000\"}";
    private String appId2;

    @Factory(dataProvider = "userModeDataProvider")
    public SubscriptionThrottlePolicyUpdateTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN}};
    }

    private static void waitUntilClockHour() throws InterruptedException {

        while (getWaitTime() > 0) {
            Thread.sleep(60000);
        }
    }

    private static long getWaitTime() {

        Calendar calendar = Calendar.getInstance();
        int minutesInTime = calendar.get(Calendar.MINUTE);
        if (60 - minutesInTime >= 5) {
            return 0;
        } else {
            return 1;
        }
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        // create application level policy with bandwidth quota type
        adminDashboardRestClient = new AdminDashboardRestClient(getPublisherURLHttps());
        adminDashboardRestClient.login(user.getUserName(), user.getPassword());
        HttpResponse subs10PerHour = adminDashboardRestClient.addSubscriptionPolicy(
                "SubscriptionThrottlePolicyUpdateTestCase10PerHour",
                1, "hour", true, 10);
        verifyResponse(subs10PerHour);
        HttpResponse subs20PerHour = adminDashboardRestClient.addSubscriptionPolicy(
                "SubscriptionThrottlePolicyUpdateTestCase20PerHour",
                1, "hour", true, 20);
        verifyResponse(subs20PerHour);

        backendEP = gatewayUrlsWrk.getWebAppURLNhttp() + "response/";
        // create api
        String APIName = "BandwidthTestAPI";
        String APIContext = "bandwithtestapi";
        String tags = "youtube, token, media";
        String url = backendEP;
        String description = "This is test API create by API manager integration test";
        String providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        String APIVersion = "1.0.0";
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url), new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setSandbox(url);
        apiRequest.setProvider(user.getUserName());
        apiRequest.setTiersCollection(Constants.TIERS_UNLIMITED + "," +
                "SubscriptionThrottlePolicyUpdateTestCase10PerHour" + "," +
                "SubscriptionThrottlePolicyUpdateTestCase20PerHour");
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
        restAPIPublisher.changeAPILifeCycleStatus(apiId, Constants.PUBLISHED);
        waitForAPIDeploymentSync(user.getUserName(), APIName, APIVersion, APIMIntegrationConstants.IS_API_EXISTS);
        gatewayUrl = getAPIInvocationURLHttps(APIContext + "/" + APIVersion + "/");

        // check backend
        Map<String, String> requestHeaders = new HashMap<String, String>();
        HttpResponse response = HttpRequestUtil.doGet(backendEP, requestHeaders);
        Assert.assertEquals(response.getResponseCode(), 200, "Backend (dummy_api.xml) is not up and running");
    }

    @Test(groups = {"wso2.am"}, description = "")
    public void testSubscriptionLevelThrottling() throws Exception {

        ApplicationDTO applicationDTO = restAPIStore.addApplication("SubscriptionThrottlePolicyUpdateTestCase",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "this-is-test");
        app2Id = applicationDTO.getApplicationId();

        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, applicationDTO.getApplicationId(),
                "SubscriptionThrottlePolicyUpdateTestCase10PerHour");
        Assert.assertEquals(true, subscriptionDTO.getThrottlingPolicy().equals(
                "SubscriptionThrottlePolicyUpdateTestCase10PerHour"));
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(), "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        String jwtString = APIMTestCaseUtils.getDecodedJWT(accessToken);
        log.info("Decoded JWT token: " + jwtString);
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("content-type", "application/json");

        HttpResponse response;
        waitUntilClockHour();
        boolean isThrottled1 = false;
        for (int i = 0; i < 15; i++) {
            response = HTTPSClientUtils.doPost(gatewayUrl, requestHeaders, body);
            log.info("==============Response " + response.getResponseCode());
            if (response.getResponseCode() == 429) {
                Assert.assertTrue(i >= 10);
                isThrottled1 = true;
                break;
            }
            Thread.sleep(1000);
        }
        Assert.assertTrue(isThrottled1, "Request not throttled by SubscriptionThrottlePolicyUpdateTestCase10PerHour");

        restAPIStore.removeSubscription(subscriptionDTO.getSubscriptionId());
        subscriptionDTO = restAPIStore.subscribeToAPI(apiId, applicationDTO.getApplicationId(),
                "SubscriptionThrottlePolicyUpdateTestCase20PerHour");
        Assert.assertEquals(true, subscriptionDTO.getThrottlingPolicy().equals(
                "SubscriptionThrottlePolicyUpdateTestCase20PerHour"));
        // Verify older tokens still throttled.
        response = HTTPSClientUtils.doPost(gatewayUrl, requestHeaders, body);
        log.info("==============Response " + response.getResponseCode());
        Assert.assertEquals(429, response.getResponseCode());
        ApplicationTokenDTO applicationTokenDTO = restAPIStore.generateToken(app2Id,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.getValue(),
                applicationKeyDTO.getConsumerSecret(), null);
        requestHeaders.put("Authorization", "Bearer " + applicationTokenDTO.getAccessToken());
        jwtString = APIMTestCaseUtils.getDecodedJWT(applicationTokenDTO.getAccessToken());
        log.info("Decoded JWT token: " + jwtString);
        boolean isThrottled2 = false;
        for (int i = 0; i < 25; i++) {
            response = HTTPSClientUtils.doPost(gatewayUrl, requestHeaders, body);
            log.info("==============Response " + response.getResponseCode());
            if (response.getResponseCode() == 429) {
                isThrottled2 = true;
                Assert.assertTrue(i >= 20);
                break;
            }
            Thread.sleep(1000);
        }
        Assert.assertTrue(isThrottled2, "Request not throttled by SubscriptionThrottlePolicyUpdateTestCase20PerHour");
    }

    @Test(groups = {"wso2.am"}, description = "")
    public void testSubscriptionLevelThrottlingOpaque() throws Exception {

        ApplicationDTO applicationDTO = restAPIStore.addApplicationWithTokenType(
                "SubscriptionThrottlePolicyUpdateTestCaseOpaque",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "this-is-test",
                "OAUTH");
        appId2 = applicationDTO.getApplicationId();

        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, appId2,
                "SubscriptionThrottlePolicyUpdateTestCase10PerHour");
        Assert.assertEquals(true, subscriptionDTO.getThrottlingPolicy().equals(
                "SubscriptionThrottlePolicyUpdateTestCase10PerHour"));
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(), "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("content-type", "application/json");

        HttpResponse response;
        waitUntilClockHour();
        boolean isThrottled1 = false;
        for (int i = 0; i < 15; i++) {
            response = HTTPSClientUtils.doPost(gatewayUrl, requestHeaders, body);
            log.info("==============Response " + response.getResponseCode());
            if (response.getResponseCode() == 429) {
                Assert.assertTrue(i >= 10);
                isThrottled1 = true;
                break;
            }
            Thread.sleep(1000);
        }
        Assert.assertTrue(isThrottled1, "Request not throttled by SubscriptionThrottlePolicyUpdateTestCase10PerHour");

        restAPIStore.removeSubscription(subscriptionDTO.getSubscriptionId());
        subscriptionDTO = restAPIStore.subscribeToAPI(apiId, applicationDTO.getApplicationId(),
                "SubscriptionThrottlePolicyUpdateTestCase20PerHour");
        Assert.assertEquals(true, subscriptionDTO.getThrottlingPolicy().equals(
                "SubscriptionThrottlePolicyUpdateTestCase20PerHour"));
        ApplicationTokenDTO applicationTokenDTO = restAPIStore.generateToken(appId2,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.getValue(),
                applicationKeyDTO.getConsumerSecret(), accessToken);
        requestHeaders.put("Authorization", "Bearer " + applicationTokenDTO.getAccessToken());
        boolean isThrottled2 = false;
        for (int i = 0; i < 25; i++) {
            response = HTTPSClientUtils.doPost(gatewayUrl, requestHeaders, body);
            log.info("==============Response " + response.getResponseCode());
            if (response.getResponseCode() == 429) {
                Assert.assertTrue(i >= 20);
                isThrottled2 = true;
                break;
            }
            Thread.sleep(1000);
        }
        Assert.assertTrue(isThrottled2, "Request not throttled by SubscriptionThrottlePolicyUpdateTestCase20PerHour");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        restAPIStore.deleteApplication(appId2);
        restAPIStore.deleteApplication(app2Id);
        restAPIPublisher.deleteAPI(apiId);
        adminDashboardRestClient.deleteSubscriptionPolicy("SubscriptionThrottlePolicyUpdateTestCase10PerHour");
        adminDashboardRestClient.deleteSubscriptionPolicy("SubscriptionThrottlePolicyUpdateTestCase20PerHour");
    }
}