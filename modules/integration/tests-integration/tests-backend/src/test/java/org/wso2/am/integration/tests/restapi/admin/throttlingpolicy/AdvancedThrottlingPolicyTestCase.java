/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.restapi.admin.throttlingpolicy;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.AdvancedThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.BandwidthLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.ConditionalGroupDTO;
import org.wso2.am.integration.clients.admin.api.dto.HeaderConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.IPConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.JWTClaimsConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.QueryParameterConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleConditionDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.helpers.AdminApiTestHelper;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.net.URL;
import java.util.*;

public class AdvancedThrottlingPolicyTestCase extends APIMIntegrationBaseTest {

    private String displayName = "Test Policy";
    private String description = "This is a test advanced throttle policy";
    private String timeUnit = "min";
    private String timeUnitHour = "hour";
    private Integer unitTime = 1;
    private AdvancedThrottlePolicyDTO requestCountPolicyDTO;
    private AdvancedThrottlePolicyDTO requestCountPolicyDTO1;
    private AdvancedThrottlePolicyDTO bandwidthPolicyDTO;
    private AdvancedThrottlePolicyDTO conditionalGroupsPolicyDTO;
    private AdminApiTestHelper adminApiTestHelper;
    private final String ADMIN_ROLE = "admin";
    private final String ADMIN1_USERNAME = "admin1";
    private final String ADMIN2_USERNAME = "admin2";
    private final String PASSWORD = "admin1";
    private String apiId1;
    private String apiId2;
    private String applicationId1;
    private String applicationId2;
    private ApplicationKeyDTO applicationKeyDTO;
    private final String API_END_POINT_METHOD = "/customers/123";
    private String apiEndPointUrl;
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";

    @Factory(dataProvider = "userModeDataProvider")
    public AdvancedThrottlingPolicyTestCase(TestUserMode userMode) {
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
        adminApiTestHelper = new AdminApiTestHelper();
        userManagementClient = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                keyManagerContext.getContextTenant().getTenantAdmin().getUserName(),
                keyManagerContext.getContextTenant().getTenantAdmin().getPassword());
        userManagementClient
                .addUser(ADMIN1_USERNAME, PASSWORD, new String[] { ADMIN_ROLE }, ADMIN1_USERNAME);
        userManagementClient
                .addUser(ADMIN2_USERNAME, PASSWORD, new String[] { ADMIN_ROLE }, ADMIN2_USERNAME);

        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
    }

    @Test(groups = {"wso2.am"}, description = "Test add advanced throttling policy with request count limit")
    public void testAddPolicyWithRequestCountLimit() throws Exception {

        //Create the advanced throttling policy DTO with request count limit
        String policyName = "TestPolicyOne";
        Long requestCount = 50L;
        List<ConditionalGroupDTO> conditionalGroups = new ArrayList<>();
        RequestCountLimitDTO requestCountLimit =
                DtoFactory.createRequestCountLimitDTO(timeUnit, unitTime, requestCount);
        ThrottleLimitDTO defaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, requestCountLimit, null);
        requestCountPolicyDTO = DtoFactory
                .createAdvancedThrottlePolicyDTO(policyName, displayName, description, false, defaultLimit,
                        conditionalGroups);

        //Add the advanced throttling policy
        ApiResponse<AdvancedThrottlePolicyDTO> addedPolicy =
                restAPIAdmin.addAdvancedThrottlingPolicy(requestCountPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        AdvancedThrottlePolicyDTO addedPolicyDTO = addedPolicy.getData();
        String policyId = addedPolicyDTO.getPolicyId();
        Assert.assertNotNull(policyId, "The policy ID cannot be null or empty");

        requestCountPolicyDTO.setPolicyId(policyId);
        requestCountPolicyDTO.setIsDeployed(true);
        //Verify the created advanced throttling policy DTO
        adminApiTestHelper.verifyAdvancedThrottlePolicyDTO(requestCountPolicyDTO, addedPolicyDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test add advanced throttling policy with bandwidth limit",
            dependsOnMethods = "testAddPolicyWithRequestCountLimit")
    public void testAddPolicyWithBandwidthLimit() throws Exception {

        //Create the advanced throttling policy DTO with bandwidth limit
        String policyName = "TestPolicyTwo_WithUnderscore";
        Long dataAmount = 2L;
        String dataUnit = "KB";
        List<ConditionalGroupDTO> conditionalGroups = new ArrayList<>();
        BandwidthLimitDTO bandwidthLimit = DtoFactory.createBandwidthLimitDTO(timeUnit, unitTime, dataAmount, dataUnit);
        ThrottleLimitDTO defaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.BANDWIDTHLIMIT, null, bandwidthLimit);
        bandwidthPolicyDTO = DtoFactory
                .createAdvancedThrottlePolicyDTO(policyName, displayName, description, false, defaultLimit,
                        conditionalGroups);

        //Add the advanced throttling policy
        ApiResponse<AdvancedThrottlePolicyDTO> addedPolicy =
                restAPIAdmin.addAdvancedThrottlingPolicy(bandwidthPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        AdvancedThrottlePolicyDTO addedPolicyDTO = addedPolicy.getData();
        String policyId = addedPolicyDTO.getPolicyId();
        Assert.assertNotNull(policyId, "The policy ID cannot be null or empty");

        bandwidthPolicyDTO.setPolicyId(policyId);
        bandwidthPolicyDTO.setIsDeployed(true);
        //Verify the created advanced throttling policy DTO
        adminApiTestHelper.verifyAdvancedThrottlePolicyDTO(bandwidthPolicyDTO, addedPolicyDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test add advanced throttling policy with conditional groups",
            dependsOnMethods = "testAddPolicyWithBandwidthLimit")
    public void testAddPolicyWithConditionalGroups() throws Exception {

        //Create the advanced throttling policy DTO with conditional groups
        String policyName = "TestPolicyThree";
        Long requestCount = 50L;
        List<ConditionalGroupDTO> conditionalGroups = new ArrayList<>();
        RequestCountLimitDTO requestCountLimit =
                DtoFactory.createRequestCountLimitDTO(timeUnit, unitTime, requestCount);
        ThrottleLimitDTO defaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, requestCountLimit, null);

        conditionalGroups.add(createConditionalGroup(defaultLimit));
        conditionalGroupsPolicyDTO = DtoFactory
                .createAdvancedThrottlePolicyDTO(policyName, displayName, description, false, defaultLimit,
                        conditionalGroups);

        //Add the advanced throttling policy
        ApiResponse<AdvancedThrottlePolicyDTO> addedPolicy =
                restAPIAdmin.addAdvancedThrottlingPolicy(conditionalGroupsPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        AdvancedThrottlePolicyDTO addedPolicyDTO = addedPolicy.getData();
        String policyId = addedPolicyDTO.getPolicyId();
        Assert.assertNotNull(policyId, "The policy ID cannot be null or empty");

        conditionalGroupsPolicyDTO.setPolicyId(policyId);
        conditionalGroupsPolicyDTO.setIsDeployed(true);
        //Verify the created advanced throttling policy DTO
        adminApiTestHelper.verifyAdvancedThrottlePolicyDTO(conditionalGroupsPolicyDTO, addedPolicyDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test get and update advanced throttling policy",
            dependsOnMethods = "testAddPolicyWithConditionalGroups")
    public void testGetAndUpdatePolicy() throws Exception {

        //Get the added advanced throttling policy with request count limit
        String policyId = requestCountPolicyDTO.getPolicyId();
        ApiResponse<AdvancedThrottlePolicyDTO> retrievedPolicy =
                restAPIAdmin.getAdvancedThrottlingPolicy(policyId);
        AdvancedThrottlePolicyDTO retrievedPolicyDTO = retrievedPolicy.getData();
        Assert.assertEquals(retrievedPolicy.getStatusCode(), HttpStatus.SC_OK);

        //Verify the retrieved advanced throttling policy DTO
        adminApiTestHelper.verifyAdvancedThrottlePolicyDTO(requestCountPolicyDTO, retrievedPolicyDTO);

        //Update the advanced throttling policy
        String updatedDescription = "This is a updated test advanced throttle policy";
        requestCountPolicyDTO.setDescription(updatedDescription);
        ApiResponse<AdvancedThrottlePolicyDTO> updatedPolicy =
                restAPIAdmin.updateAdvancedThrottlingPolicy(policyId, requestCountPolicyDTO);
        AdvancedThrottlePolicyDTO updatedPolicyDTO = updatedPolicy.getData();
        Assert.assertEquals(updatedPolicy.getStatusCode(), HttpStatus.SC_OK);

        //Verify the updated advanced throttling policy DTO
        adminApiTestHelper.verifyAdvancedThrottlePolicyDTO(requestCountPolicyDTO, updatedPolicyDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test delete already assigned advanced throttling policy",
            dependsOnMethods = "testGetAndUpdatePolicy")
    public void testDeletePolicyAlreadyExisting() throws Exception {
        APIRequest apiRequest = new APIRequest("AdvancedThrottlingPolicyTest", "AdvancedThrottlingPolicy",
                new URL(backEndServerUrl.getWebAppURLHttp() + "jaxrs_basic/services/customers/customerservice/"));
        apiRequest.setProvider(user.getUserName());
        apiRequest.setVersion("1.0.0");
        HttpResponse addResponse = restAPIPublisher.addAPI(apiRequest);
        String apiID = addResponse.getData();

        apiRequest.setApiTier(requestCountPolicyDTO.getPolicyName());
        restAPIPublisher.updateAPI(apiRequest, apiID);
        try {
            restAPIAdmin.deleteAdvancedThrottlingPolicy(requestCountPolicyDTO.getPolicyId());
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_FORBIDDEN,
                    "Advanced throttling policy " + requestCountPolicyDTO.getPolicyName() + ": " + requestCountPolicyDTO
                            .getPolicyId() + " deleted even it is already assigned to an API.");
            Assert.assertTrue(e.getResponseBody().contains(
                    "Cannot delete the advanced policy with the name " + requestCountPolicyDTO.getPolicyName()
                            + " because it is already assigned to an API/Resource"));
        } finally {
            if (apiID != null) {
                restAPIPublisher.deleteAPI(apiID);
            }
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test delete advanced throttling policy",
            dependsOnMethods = "testDeletePolicyAlreadyExisting")
    public void testDeletePolicy() throws Exception {

        ApiResponse<Void> apiResponse =
                restAPIAdmin.deleteAdvancedThrottlingPolicy(requestCountPolicyDTO.getPolicyId());
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK);
    }

    @Test(groups = {"wso2.am"}, description = "Test add advanced throttling policy with existing policy name",
            dependsOnMethods = "testDeletePolicy")
    public void testAddPolicyWithExistingPolicyName() {

        //Exception occurs when adding an advanced throttling policy with an existing policy name. The status code
        //in the Exception object is used to assert this scenario
        try {
            restAPIAdmin.addAdvancedThrottlingPolicy(bandwidthPolicyDTO);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_CONFLICT);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test delete advanced throttling policy with non existing policy ID",
            dependsOnMethods = "testAddPolicyWithExistingPolicyName")
    public void testDeletePolicyWithNonExistingPolicyId() {

        //Exception occurs when deleting an advanced throttling policy with a non existing policy ID. The
        //status code in the Exception object is used to assert this scenario
        try {
            //The policy ID is created by combining two generated UUIDs
            restAPIAdmin
                    .deleteAdvancedThrottlingPolicy(UUID.randomUUID().toString() + UUID.randomUUID().toString());
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_NOT_FOUND);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test change throttling policy from Operation level to API level ",
            dependsOnMethods = "testDeletePolicyWithNonExistingPolicyId")
    public void testChangePolicyOperationLevelToAPILevel() throws Exception {

        requestCountPolicyDTO1 = createThrottlingPolicy("NewThrottlingPolicy");

        //Add the advanced throttling policy
        ApiResponse<AdvancedThrottlePolicyDTO> addedPolicy =
                restAPIAdmin.addAdvancedThrottlingPolicy(requestCountPolicyDTO1);

        APIRequest apiRequest = new APIRequest("AdvancedThrottlingPolicyTestAPI5",
                "AdvancedThrottlingPolicyTestAPI5", new URL(apiEndPointUrl));
        apiRequest.setProvider(user.getUserName());
        apiRequest.setVersion("1.0.0");

        HttpResponse addResponse = restAPIPublisher.addAPI(apiRequest);
        apiId2 = addResponse.getData();

        apiRequest.setApiTier(requestCountPolicyDTO1.getPolicyName());
        restAPIPublisher.updateAPI(apiRequest, apiId2);

        createAPIRevisionAndDeployUsingRest(apiId2, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatusToPublish(apiId2, false);
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);

        //Verifying the change in swagger
        String retrievedSwagger;
        retrievedSwagger = restAPIPublisher.getSwaggerByID(apiId2);
        OpenAPIParser parser = new OpenAPIParser();
        SwaggerParseResult swaggerParseResult = parser.readContents(retrievedSwagger, null, null);
        OpenAPI openAPI = swaggerParseResult.getOpenAPI();
        Assert.assertEquals(openAPI.getExtensions().get("x-throttling-tier"), requestCountPolicyDTO1.getPolicyName());

        HttpResponse applicationResponse = restAPIStore.createApplication("TestApplication2",
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        Assert.assertEquals(applicationResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");

        applicationId2 = applicationResponse.getData();

        restAPIStore.createSubscription(apiId2, applicationId2, APIMIntegrationConstants.API_TIER.GOLD);

        ArrayList grantTypes = new ArrayList();
        grantTypes.add("client_credentials");
        applicationKeyDTO = restAPIStore.generateKeys(applicationId2, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        Assert.assertNotNull("Access Token not found. return token: ", accessToken);

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");

        //verify throttling
        verifyThrottling("AdvancedThrottlingPolicyTestAPI5", requestHeaders);
    }

    @Test(groups = {"wso2.am"}, description = "Test change throttling policy from API level to Operation level ",
            dependsOnMethods = "testChangePolicyOperationLevelToAPILevel")
    public void testChangePolicyAPILevelToOperationLevel() throws Exception {

        //Create API and assign policy
        APIRequest apiRequest = new APIRequest("AdvancedThrottlingPolicyTestAPI4",
                "AdvancedThrottlingPolicyTestAPI4", new URL(apiEndPointUrl));
        apiRequest.setProvider(user.getUserName());
        apiRequest.setVersion("1.0.0");
        apiRequest.setApiTier(requestCountPolicyDTO1.getPolicyName());

        HttpResponse addResponse = restAPIPublisher.addAPI(apiRequest);
        apiId1 = addResponse.getData();
        APIDTO apidto = restAPIPublisher.getAPIByID(apiId1);
        Assert.assertEquals(apidto.getApiThrottlingPolicy(), requestCountPolicyDTO1.getPolicyName());

        List<APIOperationsDTO> operationsDTOS = apidto.getOperations();
        for (APIOperationsDTO operationsDTO : operationsDTOS) {
            operationsDTO.setThrottlingPolicy(requestCountPolicyDTO1.getPolicyName());
        }
        apiRequest.setOperationsDTOS(operationsDTOS);
        restAPIPublisher.updateAPI(apiRequest, apiId1);

        //publish the api
        createAPIRevisionAndDeployUsingRest(apiId1, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatusToPublish(apiId1, false);
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);

        //Verifying the change in swagger
        String retrievedSwagger;
        List<Object> resourceThrottlingTiers;
        retrievedSwagger = restAPIPublisher.getSwaggerByID(apiId1);
        resourceThrottlingTiers = getResourceThrottlingPolicies(retrievedSwagger);
        Assert.assertEquals(resourceThrottlingTiers.get(0), requestCountPolicyDTO1.getPolicyName());

        //Create application
        HttpResponse applicationResponse = restAPIStore.createApplication("TestApplication1",
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        Assert.assertEquals(applicationResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");

        applicationId1 = applicationResponse.getData();
        restAPIStore.createSubscription(apiId1, applicationId1, APIMIntegrationConstants.API_TIER.GOLD);

        ArrayList grantTypes = new ArrayList();
        grantTypes.add("client_credentials");
        applicationKeyDTO = restAPIStore.generateKeys(applicationId1, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        Assert.assertNotNull("Access Token not found. return token: ", accessToken);

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");

        //verify throttling
        verifyThrottling("AdvancedThrottlingPolicyTestAPI4", requestHeaders);
    }

    @Test(groups = {"wso2.am"}, description = "Test delete advanced throttling policy created with a different " +
            "admin user ", dependsOnMethods = "testChangePolicyAPILevelToOperationLevel")
    public void testDeleteAdvancedPolicyWithDifferentAdminUser() throws Exception {
        restAPIAdmin = new RestAPIAdminImpl(ADMIN1_USERNAME, PASSWORD, user.getUserDomain(),
                adminURLHttps);

        requestCountPolicyDTO = createThrottlingPolicy("TestPolicyAdmin1");

        //Add the advanced throttling policy
        ApiResponse<AdvancedThrottlePolicyDTO> addedPolicy =
                restAPIAdmin.addAdvancedThrottlingPolicy(requestCountPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        AdvancedThrottlePolicyDTO addedPolicyDTO = addedPolicy.getData();
        String policyId = addedPolicyDTO.getPolicyId();
        Assert.assertNotNull(policyId, "The policy ID cannot be null or empty");

        requestCountPolicyDTO.setPolicyId(policyId);
        requestCountPolicyDTO.setIsDeployed(true);
        //Verify the created advanced throttling policy DTO
        adminApiTestHelper.verifyAdvancedThrottlePolicyDTO(requestCountPolicyDTO, addedPolicyDTO);

        restAPIAdmin = new RestAPIAdminImpl(ADMIN2_USERNAME, PASSWORD, user.getUserDomain(),
                adminURLHttps);
        //Delete the policy from a different admin user
        ApiResponse<Void> apiResponse =
                restAPIAdmin.deleteAdvancedThrottlingPolicy(policyId);
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK);
    }

    @Test(groups = {"wso2.am"}, description = "Test delete advanced throttling policy assigned to API created with a" +
            " different admin user ", dependsOnMethods = "testDeleteAdvancedPolicyWithDifferentAdminUser")
    public void testDeleteAssignedAPILevelAdvancedPolicyWithDifferentAdminUser() throws Exception {

        restAPIAdmin = new RestAPIAdminImpl(ADMIN1_USERNAME, PASSWORD, user.getUserDomain(),
                adminURLHttps);

        requestCountPolicyDTO = createThrottlingPolicy("TestPolicyAdmin2");
        //Add the advanced throttling policy
        ApiResponse<AdvancedThrottlePolicyDTO> addedPolicy =
                restAPIAdmin.addAdvancedThrottlingPolicy(requestCountPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        AdvancedThrottlePolicyDTO addedPolicyDTO = addedPolicy.getData();
        String policyId = addedPolicyDTO.getPolicyId();
        Assert.assertNotNull(policyId, "The policy ID cannot be null or empty");

        requestCountPolicyDTO.setPolicyId(policyId);
        requestCountPolicyDTO.setIsDeployed(true);
        //Verify the created advanced throttling policy DTO
        adminApiTestHelper.verifyAdvancedThrottlePolicyDTO(requestCountPolicyDTO, addedPolicyDTO);

        restAPIPublisher = new RestAPIPublisherImpl(ADMIN1_USERNAME, PASSWORD,
                user.getUserDomain(), publisherURLHttps);

        //Create API and assign policy to API
        APIRequest apiRequest = new APIRequest("AdvancedThrottlingPolicyTestAPI2",
                "AdvancedThrottlingPolicyTestAPI2", new URL(backEndServerUrl.getWebAppURLHttp() +
                "jaxrs_basic/services/customers/customerservice/"));
        apiRequest.setProvider(user.getUserName());
        apiRequest.setVersion("1.0.0");
        HttpResponse addResponse = restAPIPublisher.addAPI(apiRequest);
        String apiID = addResponse.getData();
        apiRequest.setApiTier(requestCountPolicyDTO.getPolicyName());
        restAPIPublisher.updateAPI(apiRequest, apiID);

        restAPIAdmin = new RestAPIAdminImpl(ADMIN2_USERNAME, PASSWORD, user.getUserDomain(),
                adminURLHttps);
        try {
            restAPIAdmin.deleteAdvancedThrottlingPolicy(policyId);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_FORBIDDEN);
        } finally {
            if (apiID != null) {
                restAPIPublisher.deleteAPI(apiID);
            }
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test delete advanced throttling policy assigned to a resource created " +
            "with a different admin user ", dependsOnMethods = "testDeleteAssignedAPILevelAdvancedPolicyWithDifferentAdminUser")
    public void testDeleteAssignedResourceLevelAdvancedPolicyWithDifferentAdminUser() throws Exception {

        restAPIAdmin = new RestAPIAdminImpl(ADMIN1_USERNAME, PASSWORD, user.getUserDomain(),
                adminURLHttps);
        requestCountPolicyDTO = createThrottlingPolicy("TestPolicyAdmin3");
        //Add the advanced throttling policy
        ApiResponse<AdvancedThrottlePolicyDTO> addedPolicy =
                restAPIAdmin.addAdvancedThrottlingPolicy(requestCountPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        AdvancedThrottlePolicyDTO addedPolicyDTO = addedPolicy.getData();
        String policyId = addedPolicyDTO.getPolicyId();
        Assert.assertNotNull(policyId, "The policy ID cannot be null or empty");

        requestCountPolicyDTO.setPolicyId(policyId);
        requestCountPolicyDTO.setIsDeployed(true);
        //Verify the created advanced throttling policy DTO
        adminApiTestHelper.verifyAdvancedThrottlePolicyDTO(requestCountPolicyDTO, addedPolicyDTO);

        //Create API and assign policy to resource
        APIRequest apiRequest = new APIRequest("AdvancedThrottlingPolicyTestAPI3",
                "AdvancedThrottlingPolicyTestAPI3", new URL(backEndServerUrl.getWebAppURLHttp() +
                "jaxrs_basic/services/customers/customerservice/"));
        apiRequest.setProvider(user.getUserName());
        apiRequest.setVersion("1.0.0");
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget("/customers/{id}");
        apiOperationsDTO.setAuthType("None");
        apiOperationsDTO.setThrottlingPolicy(requestCountPolicyDTO.getPolicyName());
        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO);
        apiRequest.setOperationsDTOS(operationsDTOS);

        HttpResponse addResponse = restAPIPublisher.addAPI(apiRequest);
        String apiID = addResponse.getData();
        Assert.assertNotNull(apiID);

        restAPIAdmin = new RestAPIAdminImpl(ADMIN2_USERNAME, PASSWORD, user.getUserDomain(),
                adminURLHttps);
        try {
            restAPIAdmin.deleteAdvancedThrottlingPolicy(policyId);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_FORBIDDEN);
        } finally {
            if (apiID != null) {
                restAPIPublisher.deleteAPI(apiID);
            }
        }
    }

    /**
     * Creates a conditional group with a list of conditions
     *
     * @param limit Throttle limit of the conditional group.
     * @return Created conditional group DTO
     */
    public ConditionalGroupDTO createConditionalGroup(ThrottleLimitDTO limit) {

        String conditionalGroupDescription = "This is a test conditional group";
        List<ThrottleConditionDTO> conditions = createThrottlingConditions();
        return DtoFactory.createConditionalGroupDTO(conditionalGroupDescription, conditions, limit);
    }

    /**
     * Creates a list of throttling conditions
     *
     * @return Created list of throttling condition DTOs
     */
    public List<ThrottleConditionDTO> createThrottlingConditions() {

        List<ThrottleConditionDTO> throttleConditions = new ArrayList<>();

        //Create the IP condition and add it to the throttle conditions list
        String specificIP = "10.100.1.22";
        IPConditionDTO ipConditionDTO =
                DtoFactory.createIPConditionDTO(IPConditionDTO.IpConditionTypeEnum.IPSPECIFIC, specificIP, null, null);
        ThrottleConditionDTO ipCondition = DtoFactory
                .createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum.IPCONDITION, false, null, ipConditionDTO,
                        null, null);
        throttleConditions.add(ipCondition);

        //Create the header condition and add it to the throttle conditions list
        String headerName = "Host";
        String headerValue = "10.100.7.77";
        HeaderConditionDTO headerConditionDTO = DtoFactory.createHeaderConditionDTO(headerName, headerValue);
        ThrottleConditionDTO headerCondition = DtoFactory
                .createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum.HEADERCONDITION, false, headerConditionDTO,
                        null, null, null);
        throttleConditions.add(headerCondition);

        //Create the query parameter condition and add it to the throttle conditions list
        String claimUrl = "claimUrl";
        String attribute = "claimAttribute";
        QueryParameterConditionDTO queryParameterConditionDTO =
                DtoFactory.createQueryParameterConditionDTO(claimUrl, attribute);
        ThrottleConditionDTO queryParameterCondition = DtoFactory
                .createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum.QUERYPARAMETERCONDITION, false, null, null,
                        null, queryParameterConditionDTO);
        throttleConditions.add(queryParameterCondition);

        //Create the JWT claims condition and add it to the throttle conditions list
        String parameterName = "name";
        String parameterValue = "admin";
        JWTClaimsConditionDTO jwtClaimsConditionDTO =
                DtoFactory.createJWTClaimsConditionDTO(parameterName, parameterValue);
        ThrottleConditionDTO jwtClaimsCondition = DtoFactory
                .createThrottleConditionDTO(ThrottleConditionDTO.TypeEnum.JWTCLAIMSCONDITION, false, null, null,
                        jwtClaimsConditionDTO, null);
        throttleConditions.add(jwtClaimsCondition);

        return throttleConditions;
    }

    /**
     * Creates an Advanced Throttling Policy
     *
     * @return Created Advanced Throttling Policy
     */
    public AdvancedThrottlePolicyDTO createThrottlingPolicy(String policyName) {
        AdvancedThrottlePolicyDTO advancedThrottlePolicyDTO;
        Long requestCount = 10L;
        List<ConditionalGroupDTO> conditionalGroups = new ArrayList<>();
        RequestCountLimitDTO requestCountLimit =
                DtoFactory.createRequestCountLimitDTO(timeUnitHour, unitTime, requestCount);
        ThrottleLimitDTO defaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, requestCountLimit, null);
        advancedThrottlePolicyDTO = DtoFactory
                .createAdvancedThrottlePolicyDTO(policyName, displayName, description, false, defaultLimit,
                        conditionalGroups);
        return advancedThrottlePolicyDTO;
    }



    /**
     * @param apiContext
     * @param requestHeaders
     */
    private void verifyThrottling(String apiContext, Map<String, String> requestHeaders) throws Exception {
        waitUntilClockMinute();
        boolean isThrottled = false;
        for (int invocationCount = 0; invocationCount < 20; invocationCount++) {
            //Invoke  API
            HttpResponse invokeResponse = HttpRequestUtil
                    .doGet(getAPIInvocationURLHttps(apiContext, "1.0.0") + API_END_POINT_METHOD,
                            requestHeaders);
            if (invokeResponse.getResponseCode() == 429) {
                Assert.assertTrue(invocationCount >= 10);
                isThrottled = true;
                break;
            }
            Thread.sleep(1000);
        }
        Assert.assertTrue(isThrottled, "Request not throttled by Throttling Policy");
    }

    /**
     * Gets Throttling policies of resources
     *
     * @return List of Throttling policies
     */
    private List<Object> getResourceThrottlingPolicies(String swaggerContent) throws APIManagementException {
        OpenAPIParser parser = new OpenAPIParser();
        SwaggerParseResult swaggerParseResult = parser.readContents(swaggerContent, null, null);
        OpenAPI openAPI = swaggerParseResult.getOpenAPI();
        Paths paths = openAPI.getPaths();
        List<Object> throttlingPolicies = new ArrayList<>();
        for (String pathKey : paths.keySet()) {
            Map<PathItem.HttpMethod, Operation> operationsMap = paths.get(pathKey).readOperationsMap();
            for (Map.Entry<PathItem.HttpMethod, Operation> entry : operationsMap.entrySet()) {
                Operation operation = entry.getValue();
                Map<String, Object> extensions = operation.getExtensions();
                Assert.assertNotNull(extensions.get("x-throttling-tier"));
                throttlingPolicies.add(extensions.get("x-throttling-tier"));
            }
        }
        return throttlingPolicies;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIAdmin.deleteAdvancedThrottlingPolicy(bandwidthPolicyDTO.getPolicyId());
        restAPIAdmin.deleteAdvancedThrottlingPolicy(conditionalGroupsPolicyDTO.getPolicyId());
        restAPIStore.deleteApplication(applicationId1);
        restAPIStore.deleteApplication(applicationId2);
        restAPIPublisher.deleteAPI(apiId1);
        restAPIPublisher.deleteAPI(apiId2);
    }
}
