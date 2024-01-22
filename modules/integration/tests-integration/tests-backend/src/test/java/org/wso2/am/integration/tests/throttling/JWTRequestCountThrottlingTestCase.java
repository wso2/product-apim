/*
 *Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import com.google.gson.Gson;
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
import org.wso2.am.integration.clients.admin.api.dto.AdvancedThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.ApplicationThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.ConditionalGroupDTO;
import org.wso2.am.integration.clients.admin.api.dto.HeaderConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.IPConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.JWTClaimsConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.QueryParameterConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyPermissionDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.throttling.ThrottlingUtils;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JWTRequestCountThrottlingTestCase extends APIMIntegrationBaseTest {

    private final String appPolicyName = "AppPolicyWithRequestCount";
    private final String subPolicyName = "SubPolicyWithRequestCount";
    private final String apiPolicyName1 = "APIPolicyWithRequestCount1";
    private final String apiPolicyName2 = "APIPolicyWithRequestCount2";
    private String appPolicyId;
    private String subPolicyId;
    private String apiPolicyId1;
    private String apiPolicyId2;
    private String apiId;
    private String gatewayUrl;
    private String appId1;
    private String appId2;
    private String appId3;
    private String appId4;

    ApplicationKeyDTO applicationKeyDTOOfAPILevelTest;

    private static final Log log = LogFactory.getLog(JWTRequestCountThrottlingTestCase.class);

    @Factory(dataProvider = "userModeDataProvider")
    public JWTRequestCountThrottlingTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN}};
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        String INTERNAL_EVERYONE= "Internal/everyone";
        List<String> roleList = new ArrayList<>();
        SubscriptionThrottlePolicyPermissionDTO permissions;

        RequestCountLimitDTO requestCountLimit3PerMin =
                DtoFactory.createRequestCountLimitDTO("min", 1, 3L);
        RequestCountLimitDTO appLimit =
                DtoFactory.createRequestCountLimitDTO("min", 1, 5L);

        ThrottleLimitDTO defaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, requestCountLimit3PerMin, null);
        ThrottleLimitDTO appLimitDTO =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, appLimit, null);

        //Add the application throttling policy
        ApplicationThrottlePolicyDTO requestCountPolicyDTO = DtoFactory
                .createApplicationThrottlePolicyDTO(appPolicyName, "", "", false, appLimitDTO);
        ApiResponse<ApplicationThrottlePolicyDTO> addedApplicationPolicy =
                restAPIAdmin.addApplicationThrottlingPolicy(requestCountPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(addedApplicationPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        ApplicationThrottlePolicyDTO addedApplicationPolicyDTO = addedApplicationPolicy.getData();
        appPolicyId = addedApplicationPolicyDTO.getPolicyId();
        Assert.assertNotNull(appPolicyId, "The policy ID cannot be null or empty");
        roleList.add(INTERNAL_EVERYONE);
        permissions = DtoFactory.
                createSubscriptionThrottlePolicyPermissionDTO(SubscriptionThrottlePolicyPermissionDTO.
                        PermissionTypeEnum.ALLOW, roleList);
        //Create the subscription level policy
        SubscriptionThrottlePolicyDTO requestCountSubscriptionPolicyDTO = DtoFactory
                .createSubscriptionThrottlePolicyDTO(subPolicyName, "", "", false, defaultLimit,
                        -1, -1, 100, "min", new ArrayList<>(),
                        true, "", 0, permissions);
        ApiResponse<SubscriptionThrottlePolicyDTO> addedSubscriptionPolicy =
                restAPIAdmin.addSubscriptionThrottlingPolicy(requestCountSubscriptionPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(addedSubscriptionPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        SubscriptionThrottlePolicyDTO addedSubscriptionPolicyDTO = addedSubscriptionPolicy.getData();
        subPolicyId = addedSubscriptionPolicyDTO.getPolicyId();
        Assert.assertNotNull(subPolicyId, "The policy ID cannot be null or empty");

        //Create the advanced throttling policy with no conditions
        AdvancedThrottlePolicyDTO requestCountAdvancedPolicyDTO = DtoFactory
                .createAdvancedThrottlePolicyDTO(apiPolicyName1, "", "", false, defaultLimit,
                        new ArrayList<>());
        ApiResponse<AdvancedThrottlePolicyDTO> addedApiPolicy1 =
                restAPIAdmin.addAdvancedThrottlingPolicy(requestCountAdvancedPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(addedApiPolicy1.getStatusCode(), HttpStatus.SC_CREATED);
        AdvancedThrottlePolicyDTO addedAdvancedPolicyDTO = addedApiPolicy1.getData();
        apiPolicyId1 = addedAdvancedPolicyDTO.getPolicyId();
        Assert.assertNotNull(apiPolicyId1, "The policy ID cannot be null or empty");

        //Create the advanced throttling policy with conditions
        RequestCountLimitDTO requestCountLimit10PerMin =
                DtoFactory.createRequestCountLimitDTO("min", 1, 10L);
        ThrottleLimitDTO defaultLimit2 =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, requestCountLimit10PerMin, null);
        AdvancedThrottlePolicyDTO requestCountAdvancedPolicyDTO2 = DtoFactory
                .createAdvancedThrottlePolicyDTO(apiPolicyName2, "", "", false, defaultLimit2,
                        createConditionalGroups(defaultLimit));
        ApiResponse<AdvancedThrottlePolicyDTO> addedApiPolicy2 =
                restAPIAdmin.addAdvancedThrottlingPolicy(requestCountAdvancedPolicyDTO2);

        //Assert the status code and policy ID
        Assert.assertEquals(addedApiPolicy2.getStatusCode(), HttpStatus.SC_CREATED);
        AdvancedThrottlePolicyDTO addedAdvancedPolicyDTO2 = addedApiPolicy2.getData();
        apiPolicyId2 = addedAdvancedPolicyDTO2.getPolicyId();
        Assert.assertNotNull(apiPolicyId2, "The policy ID cannot be null or empty");


        String backendEP = gatewayUrlsWrk.getWebAppURLNhttp() + "response/";
        // create api
        String APIName = "RequestCountTestAPI";
        String APIContext = "requestcounttestapi";
        String description = "This is test API create by API manager integration test";
        String APIVersion = "1.0.0";
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(backendEP), new URL(backendEP));
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setSandbox(backendEP);
        apiRequest.setProvider(user.getUserName());
        apiRequest.setTiersCollection(Constants.TIERS_UNLIMITED + "," + subPolicyName);
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
        gatewayUrl = getAPIInvocationURLHttps(APIContext + "/" + APIVersion + "/");

        // check backend
        Map<String, String> requestHeaders = new HashMap<>();
        HttpResponse response = HttpRequestUtil.doGet(backendEP, requestHeaders);
        Assert.assertEquals(response.getResponseCode(), 200, "Backend (dummy_api.xml) is not up and running");

        // Create App,subscription for API level test cases
        ApplicationDTO applicationDTO = restAPIStore.addApplication("NormalAPP",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "this-is-test");
        appId3 = applicationDTO.getApplicationId();
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, applicationDTO.getApplicationId(),
                Constants.TIERS_UNLIMITED);
        Assert.assertEquals(subscriptionDTO.getThrottlingPolicy(), Constants.TIERS_UNLIMITED);

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        applicationKeyDTOOfAPILevelTest = restAPIStore.generateKeys(applicationDTO.getApplicationId(), "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
    }

    @Test(groups = {"wso2.am"}, description = "")
    public void testApplicationLevelThrottling() throws Exception {
        ApplicationDTO applicationDTO = restAPIStore.addApplication("ApplicationRequestCountTestApp",
                appPolicyName, "", "this-is-test");
        appId1 = applicationDTO.getApplicationId();
        Assert.assertEquals(applicationDTO.getThrottlingPolicy(), appPolicyName);
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, applicationDTO.getApplicationId(),
                Constants.TIERS_UNLIMITED);
        Assert.assertEquals(subscriptionDTO.getThrottlingPolicy(), Constants.TIERS_UNLIMITED);
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

        Assert.assertTrue(isThrottled(requestHeaders, null,12),
                "Request not throttled by request count condition in application tier");
    }

    @Test(groups = {"wso2.am"}, description = "")
    public void testSubscriptionLevelThrottling() throws Exception {

        ApplicationDTO applicationDTO = restAPIStore.addApplication("SubscriptionRequestCountTestApp",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "this-is-test");
        appId2 = applicationDTO.getApplicationId();

        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, applicationDTO.getApplicationId(),
                subPolicyName);
        Assert.assertEquals(subscriptionDTO.getThrottlingPolicy(), subPolicyName);
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(),
                "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        String jwtString = APIMTestCaseUtils.getDecodedJWT(accessToken);
        log.info("Decoded JWT token: " + jwtString);
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("content-type", "application/json");

        Assert.assertTrue(isThrottled(requestHeaders, null,6),
                "Request not throttled by request count condition in subscription tier");
    }

    @Test(groups = { "wso2.am" }, description = "", dependsOnMethods = { "testSubscriptionLevelThrottling" })
    public void testNonaunthenticatedResourceThrottlingWithJWTClaimCondition() throws Exception {
        HttpResponse api = restAPIPublisher.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        List<APIOperationsDTO> operations = apidto.getOperations();
        //Setting Off Security for api resource
        operations.get(0).setAuthType("None");
        operations.get(0).setThrottlingPolicy(apiPolicyName2);
        apidto.setOperations(operations);
        restAPIPublisher.updateAPI(apidto, apiId);

        // Create Revision and Deploy to Gateway
        String apiRevisionId1 = createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();
        waitForAPIDeploymentSync(user.getUserName(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("content-type", "application/json");
        Assert.assertTrue(isThrottled(requestHeaders, null, 25),
                "Request not throttled by default request count in app tier");

        //Setting On Security for api resource
        apidto = gson.fromJson(api.getData(), APIDTO.class);
        operations = apidto.getOperations();
        operations.get(0).setAuthType("Application & Application User");
        apidto.setOperations(operations);
        restAPIPublisher.updateAPI(apidto, apiId);

        // Create Revision and Deploy to Gateway
        String apiRevisionId2 = createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();
        waitForAPIDeploymentSync(user.getUserName(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);

        //Delete created API Revisions
        restAPIPublisher.deleteAPIRevision(apiId, apiRevisionId1);
        restAPIPublisher.deleteAPIRevision(apiId, apiRevisionId2);
    }

    @Test(groups = {"wso2.am"}, description = "", dependsOnMethods = {"testSubscriptionLevelThrottling",
            "testApplicationLevelThrottling"})
    public void testAPILevelThrottling() throws Exception {
        HttpResponse api = restAPIPublisher.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        apidto.setApiThrottlingPolicy(apiPolicyName1);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, apiId);
        Assert.assertEquals(updatedAPI.getApiThrottlingPolicy(), apiPolicyName1, "API tier not updated.");
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();
        waitForAPIDeploymentSync(user.getUserName(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        Assert.assertNotNull(applicationKeyDTOOfAPILevelTest.getToken());
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + applicationKeyDTOOfAPILevelTest.getToken().getAccessToken());
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("content-type", "application/json");

        Assert.assertTrue(isThrottled(requestHeaders, null,7),
                "Request not throttled by request count condition in api tier");
    }


    @Test(groups = {"wso2.am"}, description = "", dependsOnMethods = {"testAPILevelThrottling"})
    public void testAPILevelThrottlingWithIpCondition() throws Exception {
        HttpResponse api = restAPIPublisher.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        apidto.setApiThrottlingPolicy(APIMIntegrationConstants.API_TIER.UNLIMITED);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, apiId);
        Assert.assertEquals(updatedAPI.getApiThrottlingPolicy(), APIMIntegrationConstants.API_TIER.UNLIMITED,
                "API tier not updated.");
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_NOT_EXISTS);
        waitForAPIDeploymentSync(user.getUserName(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        Assert.assertNotNull(applicationKeyDTOOfAPILevelTest.getToken());
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + applicationKeyDTOOfAPILevelTest.getToken().getAccessToken());
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("content-type", "application/json");
        requestHeaders.put("X-Forwarded-For", "10.100.1.22");

        Assert.assertFalse(isThrottled(requestHeaders, null, -1),
                "Request was throttled unexpectedly in Unlimited API tier");

        apidto.setApiThrottlingPolicy(apiPolicyName2);
        updatedAPI = restAPIPublisher.updateAPI(apidto, apiId);
        Assert.assertEquals(updatedAPI.getApiThrottlingPolicy(), apiPolicyName2,
                "API tier not updated.");
        Assert.assertFalse(isThrottled(requestHeaders, null, -1), "Request not need to throttle since policy was " +
                "Unlimited");
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_NOT_EXISTS);
        waitForAPIDeploymentSync(user.getUserName(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        Assert.assertTrue(isThrottled(requestHeaders, null,7), "Request not need to throttle since policy was updated");
    }

    @Test(groups = {"wso2.am"}, description = "", dependsOnMethods = {"testAPILevelThrottlingWithIpCondition"})
    public void testAPILevelThrottlingWithHeaderCondition() throws Exception {
        HttpResponse api = restAPIPublisher.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        Assert.assertEquals(apidto.getApiThrottlingPolicy(), apiPolicyName2,
                "API tier not updated.");
        Assert.assertNotNull(applicationKeyDTOOfAPILevelTest.getToken());
        String accessToken = applicationKeyDTOOfAPILevelTest.getToken().getAccessToken();

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("content-type", "application/json");
        requestHeaders.put("Host", "10.100.7.77");
        Assert.assertTrue(isThrottled(requestHeaders, null,7),
                "Request not throttled by request count header condition in API tier");
    }

    @Test(groups = {"wso2.am"}, description = "", dependsOnMethods = {"testAPILevelThrottlingWithHeaderCondition"})
    public void testAPILevelThrottlingWithQueryCondition() throws Exception {
        HttpResponse api = restAPIPublisher.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        Assert.assertEquals(apidto.getApiThrottlingPolicy(), apiPolicyName2,
                "API tier not updated.");
        Assert.assertNotNull(applicationKeyDTOOfAPILevelTest.getToken());
        String accessToken = applicationKeyDTOOfAPILevelTest.getToken().getAccessToken();

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("content-type", "application/json");
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("name", "admin");
        Assert.assertTrue(isThrottled(requestHeaders, queryParams,7),
                "Request not throttled by request count query parameter condition in API tier");
        // Create Revision and Deploy to Gateway
    }

    @Test(groups = {"wso2.am"}, description = "", dependsOnMethods = {"testAPILevelThrottlingWithQueryCondition"})
    public void testAPILevelThrottlingWithJWTClaimCondition() throws Exception {
        HttpResponse api = restAPIPublisher.getAPI(apiId);
        Gson gson = new Gson();
        APIDTO apidto = gson.fromJson(api.getData(), APIDTO.class);
        Assert.assertEquals(apidto.getApiThrottlingPolicy(), apiPolicyName2,
                "API tier not updated.");
        ApplicationDTO applicationDTO = restAPIStore.addApplication("NormalAPP5",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "this-is-test");
        appId4 = applicationDTO.getApplicationId();
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, applicationDTO.getApplicationId(),
                Constants.TIERS_UNLIMITED);
        Assert.assertEquals(subscriptionDTO.getThrottlingPolicy(), Constants.TIERS_UNLIMITED);

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(), "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("content-type", "application/json");
        Assert.assertTrue(isThrottled(requestHeaders, null,10),
                "Request not throttled by request count jwt claim condition in API tier");
    }

    private boolean isThrottled(Map<String, String> requestHeaders, Map<String, String> queryParams,
                                int expectedCount) throws InterruptedException, IOException {
        waitUntilClockMinute();
        StringBuilder url = new StringBuilder(gatewayUrl);
        if (queryParams != null) {
            int i = 0;
            if (expectedCount==-1){
                expectedCount = 21;
            }
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                System.out.println(entry.getKey() + "/" + entry.getValue());
                if (i == 0) {
                    url.append(url).append("?").append(entry.getKey()).append("=").append(entry.getValue());
                } else {
                    url.append(url).append("&").append(entry.getKey()).append("=").append(entry.getValue());
                }
                i++;
            }
        }
        HttpResponse response;
        boolean isThrottled = false;
        for (int j = 0; j < expectedCount; j++) {
            if (j == expectedCount - 1) {
                log.info("Waiting for JMS messages to arrive in gateway before sending " + expectedCount + " request");
                Thread.sleep(ThrottlingUtils.WAIT_FOR_JMS_THROTTLE_EVENT_IN_MILLISECONDS);
            }
            String body = "{\"payload\" : \"00000000000000000\"}";
            response = HTTPSClientUtils.doPost(url.toString(), requestHeaders, body);
            log.info("============== Response " + response.getResponseCode());
            if (response.getResponseCode() == 429) {
                isThrottled = true;
                break;
            }
            Thread.sleep(500);
        }
        return isThrottled;
    }

    /**
     * Creates a set of conditional groups with a list of conditions
     *
     * @param limit Throttle limit of the conditional group.
     * @return Created list of conditional group DTO
     */
    public List<ConditionalGroupDTO> createConditionalGroups(ThrottleLimitDTO limit) {

        List<ConditionalGroupDTO> conditionalGroups = new ArrayList<>();

        //Create the IP condition and add it to the throttle conditions list
        List<ThrottleConditionDTO> throttleConditions1 = new ArrayList<>();
        String specificIP = "10.100.1.22";
        IPConditionDTO ipConditionDTO =
                DtoFactory.createIPConditionDTO(IPConditionDTO.IpConditionTypeEnum.IPSPECIFIC, specificIP, null, null);
        ThrottleConditionDTO ipCondition = DtoFactory
                .createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum.IPCONDITION, false, null, ipConditionDTO,
                        null, null);
        throttleConditions1.add(ipCondition);
        conditionalGroups.add(DtoFactory.createConditionalGroupDTO(
                "Test conditional group 1", throttleConditions1, limit));

        //Create the header condition and add it to the throttle conditions list
        List<ThrottleConditionDTO> throttleConditions2 = new ArrayList<>();
        String headerName = "Host";
        String headerValue = "10.100.7.77";
        HeaderConditionDTO headerConditionDTO = DtoFactory.createHeaderConditionDTO(headerName, headerValue);
        ThrottleConditionDTO headerCondition = DtoFactory
                .createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum.HEADERCONDITION, false, headerConditionDTO,
                        null, null, null);
        throttleConditions2.add(headerCondition);
        conditionalGroups.add(DtoFactory.createConditionalGroupDTO(
                "Test conditional group 2", throttleConditions2, limit));

        //Create the query parameter condition and add it to the throttle conditions list
        List<ThrottleConditionDTO> throttleConditions3 = new ArrayList<>();
        String parameterName = "name";
        String parameterValue = "admin";
        QueryParameterConditionDTO queryParameterConditionDTO =
                DtoFactory.createQueryParameterConditionDTO(parameterName, parameterValue);
        ThrottleConditionDTO queryParameterCondition = DtoFactory
                .createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum.QUERYPARAMETERCONDITION, false, null, null,
                        null, queryParameterConditionDTO);
        throttleConditions3.add(queryParameterCondition);
        conditionalGroups.add(DtoFactory.createConditionalGroupDTO(
                "Test conditional group 3", throttleConditions3, limit));

        //Create the JWT claims condition and add it to the throttle conditions list
        List<ThrottleConditionDTO> throttleConditions4 = new ArrayList<>();
        String claimUrl = "http://wso2.org/claims/applicationname";
        String attribute = "NormalAPP5";
        JWTClaimsConditionDTO jwtClaimsConditionDTO =
                DtoFactory.createJWTClaimsConditionDTO(claimUrl, attribute);
        ThrottleConditionDTO jwtClaimsCondition = DtoFactory
                .createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum.JWTCLAIMSCONDITION, false, null, null,
                        jwtClaimsConditionDTO, null);
        throttleConditions4.add(jwtClaimsCondition);
        conditionalGroups.add(DtoFactory.createConditionalGroupDTO(
                "Test conditional group 4", throttleConditions4, limit));

        return conditionalGroups;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(appId1);
        restAPIStore.deleteApplication(appId2);
        restAPIStore.deleteApplication(appId3);
        restAPIStore.deleteApplication(appId4);
        restAPIPublisher.deleteAPI(apiId);
        restAPIAdmin.deleteAdvancedThrottlingPolicy(apiPolicyId1);
        restAPIAdmin.deleteAdvancedThrottlingPolicy(apiPolicyId2);
        restAPIAdmin.deleteApplicationThrottlingPolicy(appPolicyId);
        restAPIAdmin.deleteSubscriptionThrottlingPolicy(subPolicyId);
    }
}
