/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.throttling.unlimitedDisable;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.registry.ResourceAdminServiceClient;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.AdvancedThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.ApplicationThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLValidationResponseDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLValidationResponseGraphQLInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.SettingsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationInfoDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationListDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;

/**
 * Publish an API under Unlimited tier and test the invocation , then disable unlimited tier and change the api
 * tier to silver and do a new silver subscription and test invocation under Silver tier.
 */
public class ConfigurableDefaultPolicyTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(ConfigurableDefaultPolicyTestCase.class);
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String API_CONTEXT = "info";
    private final String GRAPHQL_API_NAME = "ConfigurableDefaultPolicyTestCaseGraphql";
    private final String END_POINT_URL = "https://localhost:9943/am-graphQL-sample/api/graphql/";
    private AdvancedThrottlePolicyDTO advancedThrottlePolicyDTO;
    private String providerName;
    private String schemaDefinition;
    private String graphqlAPIId;
    private String restAPIId;
    private SubscriptionThrottlePolicyDTO defaultSubscriptionPolicy;
    private ApplicationThrottlePolicyDTO defaultApplicationThrottlePolicyDTO;
    private ResourceAdminServiceClient resourceAdminServiceClient;
    private String tenantConfigBeforeTestCase;
    private String[] subscriberRole = {APIMIntegrationConstants.APIM_INTERNAL_ROLE.SUBSCRIBER};

    @Factory(dataProvider = "userModeDataProvider")
    public ConfigurableDefaultPolicyTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        providerName = user.getUserName();
        ThrottleLimitDTO defaultLimit = new ThrottleLimitDTO()
                .type(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT)
                .requestCount(new RequestCountLimitDTO().requestCount(10l).unitTime(1).timeUnit("min")).bandwidth(null);
        AdvancedThrottlePolicyDTO advancedThrottlePolicyDTO = new AdvancedThrottlePolicyDTO();
        advancedThrottlePolicyDTO.setPolicyName("DefaultAPIPolicy");
        advancedThrottlePolicyDTO.setDefaultLimit(defaultLimit);
        ApiResponse<AdvancedThrottlePolicyDTO> advancedThrottlePolicyDTOApiResponse =
                restAPIAdmin.addAdvancedThrottlingPolicy(advancedThrottlePolicyDTO);
        Assert.assertEquals(advancedThrottlePolicyDTOApiResponse.getStatusCode(), 201);
        this.advancedThrottlePolicyDTO = advancedThrottlePolicyDTOApiResponse.getData();

        SubscriptionThrottlePolicyDTO subscriptionThrottlePolicyDTO = new SubscriptionThrottlePolicyDTO();
        subscriptionThrottlePolicyDTO.setPolicyName("DefaultSubscriptionLevelTier");
        subscriptionThrottlePolicyDTO.setDefaultLimit(defaultLimit);
        subscriptionThrottlePolicyDTO.setRateLimitCount(10);
        subscriptionThrottlePolicyDTO.setRateLimitTimeUnit("sec");
        subscriptionThrottlePolicyDTO.setSubscriberCount(10);
        subscriptionThrottlePolicyDTO.setBillingPlan("FREE");
        ApiResponse<SubscriptionThrottlePolicyDTO> subscriptionThrottlePolicyDTOApiResponse =
                restAPIAdmin.addSubscriptionThrottlingPolicy(subscriptionThrottlePolicyDTO);
        Assert.assertEquals(subscriptionThrottlePolicyDTOApiResponse.getStatusCode(), 201);
        this.defaultSubscriptionPolicy = subscriptionThrottlePolicyDTOApiResponse.getData();

        ApplicationThrottlePolicyDTO applicationThrottlePolicyDTO = new ApplicationThrottlePolicyDTO();
        applicationThrottlePolicyDTO.setPolicyName("DefaultApplicationLevelTier");
        applicationThrottlePolicyDTO.setDefaultLimit(defaultLimit);
        ApiResponse<ApplicationThrottlePolicyDTO> applicationThrottlePolicyDTOApiResponse =
                restAPIAdmin.addApplicationThrottlingPolicy(applicationThrottlePolicyDTO);
        Assert.assertEquals(applicationThrottlePolicyDTOApiResponse.getStatusCode(), 201);
        this.defaultApplicationThrottlePolicyDTO = applicationThrottlePolicyDTOApiResponse.getData();

        //Add custom header by editing tenant-conf.json in super tenant registry
        resourceAdminServiceClient =
                new ResourceAdminServiceClient(gatewayContextMgt.getContextUrls().getBackEndUrl(),
                        createSession(gatewayContextMgt));
        tenantConfigBeforeTestCase = restAPIAdmin.getTenantConfig();
        org.json.simple.JSONObject tenantConfigJson = (org.json.simple.JSONObject) new JSONParser().parse(tenantConfigBeforeTestCase);
        tenantConfigJson.put("DefaultAPILevelTier", "DefaultAPIPolicy");
        tenantConfigJson.put("DefaultApplicationLevelTier", "DefaultApplicationLevelTier");
        tenantConfigJson.put("DefaultSubscriptionLevelTier", "DefaultSubscriptionLevelTier");
        restAPIAdmin.updateTenantConfig(tenantConfigJson);
        userManagementClient.addUser("tenantConfigured", "tenantConfigured", subscriberRole, "default");
        try {
            restAPIAdmin.deleteAdvancedThrottlingPolicy(this.advancedThrottlePolicyDTO.getPolicyId());
            Assert.fail("Default API Policy get deleted.");
        } catch (org.wso2.am.integration.clients.admin.ApiException e) {
            Assert.assertEquals(e.getCode(), 500);
        }
        try {
            restAPIAdmin.deleteSubscriptionThrottlingPolicy(this.defaultSubscriptionPolicy.getPolicyId());
            Assert.fail("Default Subscription Policy get deleted.");
        } catch (org.wso2.am.integration.clients.admin.ApiException e) {
            Assert.assertEquals(e.getCode(), 500);
        }
        try {
            restAPIAdmin.deleteApplicationThrottlingPolicy(this.defaultApplicationThrottlePolicyDTO.getPolicyId());
            Assert.fail("Default Application Policy get deleted.");
        } catch (org.wso2.am.integration.clients.admin.ApiException e) {
            Assert.assertEquals(e.getCode(), 500);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Create graphQL API and see if the rate limiting is set to a policy " +
            "other than Unlimited")
    public void testCreateGraphQLAPI() throws Exception {

        schemaDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("graphql" + File.separator + "schema.graphql"),
                "UTF-8");

        File file = getTempFileWithContent(schemaDefinition);
        GraphQLValidationResponseDTO responseApiDto = restAPIPublisher.validateGraphqlSchemaDefinition(file);
        GraphQLValidationResponseGraphQLInfoDTO graphQLInfo = responseApiDto.getGraphQLInfo();
        String arrayToJson = new ObjectMapper().writeValueAsString(graphQLInfo.getOperations());
        JSONArray operations = new JSONArray(arrayToJson);

        ArrayList<String> environment = new ArrayList<String>();
        environment.add(Constants.GATEWAY_ENVIRONMENT);

        ArrayList<String> policies = new ArrayList<String>();

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("name", GRAPHQL_API_NAME);
        additionalPropertiesObj.put("context", API_CONTEXT);
        additionalPropertiesObj.put("version", API_VERSION_1_0_0);

        JSONObject url = new JSONObject();
        url.put("url", END_POINT_URL);
        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("sandbox_endpoints", url);
        endpointConfig.put("production_endpoints", url);
        additionalPropertiesObj.put("endpointConfig", endpointConfig);
        additionalPropertiesObj.put("operations", operations);

        // create Graphql API
        String additionalProperties = additionalPropertiesObj.toString();
        APIDTO apidto = restAPIPublisher.importGraphqlSchemaDefinition(file, additionalProperties);
        graphqlAPIId = apidto.getId();
        List<APIOperationsDTO> operationsList = apidto.getOperations();
        log.info("operationsList" + operationsList);
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(graphqlAPIId);
        //get the operation of the API and check whether throttling tier is not unlimited
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode());
        Assert.assertTrue(operationsList.get(0).getThrottlingPolicy().equalsIgnoreCase("DefaultAPIPolicy"),
                "Throttling policy " + operationsList.get(0).getThrottlingPolicy() + " is applied");
    }

    @Test(groups = {"wso2.am"}, description = "Create a REST API without x-throttling tier and check if " +
            "API gets published successfully")
    public void createAPIwithThrottlingTierNull() throws Exception {
        //create a REST API
        String swaggerPath = getAMResourceLocation() + File.separator + "configFiles" + File.separator + "unlimitedTier"
                + File.separator + "TestAPI.json";
        File definition = new File(swaggerPath);

        JSONObject endpoints = new JSONObject();
        endpoints.put("url", "http://testapi.com");

        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("production_endpoints", endpoints);
        endpointConfig.put("sandbox_endpoints", endpoints);

        List<String> tierList = new ArrayList<>();
        tierList.add(APIMIntegrationConstants.API_TIER.SILVER);
        tierList.add(APIMIntegrationConstants.API_TIER.GOLD);

        JSONObject apiProperties = new JSONObject();
        apiProperties.put("name", "TestAPI");
        apiProperties.put("context", "/TestAPI");
        apiProperties.put("version", "1.0.0");
        apiProperties.put("provider", providerName);
        apiProperties.put("policies", tierList);
        apiProperties.put("endpointConfig", endpointConfig);
        APIDTO restAPIDTO = restAPIPublisher.importOASDefinition(definition, apiProperties.toString());
        restAPIId = restAPIDTO.getId();

        restAPIPublisher.changeAPILifeCycleStatusToPublish(restAPIId, false);
        APIDTO retrievedDto = restAPIPublisher.getAPIByID(restAPIId);
        Assert.assertNotNull(retrievedDto);
        Assert.assertNotNull(retrievedDto.getOperations());
        for (APIOperationsDTO operation : retrievedDto.getOperations()) {
            Assert.assertNotEquals(operation.getThrottlingPolicy(), "Unlimited");
        }
        String retrievedSwagger = restAPIPublisher.getSwaggerByID(restAPIId);
        retrievedSwagger = restAPIPublisher.updateSwagger(restAPIId, retrievedSwagger);
        Assert.assertNotNull(retrievedSwagger);
        validateThrottlingPolicy(retrievedSwagger);
        retrievedDto.getPolicies().add("Unlimited");
        try {
            restAPIPublisher.updateAPI(retrievedDto, restAPIId);
            Assert.fail("API Update Successful with Unlimited Subscription Policy.");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 400);
            Assert.assertFalse(e.getResponseBody().contains("Unlimited"));
        }
    }

    @Test(groups = {"wso2.am"}, description = "Check Setting API not returning Unlimited Tier")
    public void verifySettingsRestAPIInPublisher() throws ApiException {

        SettingsDTO settings = restAPIPublisher.getSettings();
        Assert.assertEquals(settings.getDefaultAdvancePolicy(), "DefaultAPIPolicy");
        Assert.assertEquals(settings.getDefaultSubscriptionPolicy(), "DefaultSubscriptionLevelTier");
    }

    @Test(groups = {"wso2.am"}, description = "Check Setting API not returning Unlimited Tier")
    public void verifyDefaultApplicationCreatedThrottlePolicy() throws
            org.wso2.am.integration.clients.store.api.ApiException {
        RestAPIStoreImpl restAPIStore1;
        if (userMode != TestUserMode.SUPER_TENANT_ADMIN) {
            restAPIStore1 = new RestAPIStoreImpl("tenantConfigured", "tenantConfigured", "wso2.com",
                    "https://localhost:9943/");
        } else {
            restAPIStore1 = new RestAPIStoreImpl("tenantConfigured", "tenantConfigured", "carbon" +
                    ".super",
                    "https://localhost:9943/");
        }
        ApplicationListDTO defaultApplication = restAPIStore1.getApplications("DefaultApplication");
        Assert.assertNotNull(defaultApplication);
        Assert.assertEquals(defaultApplication.getCount().intValue(), 1);
        ApplicationInfoDTO applicationInfoDTO = defaultApplication.getList().get(0);
        Assert.assertEquals(applicationInfoDTO.getThrottlingPolicy(),"DefaultApplicationLevelTier");
        restAPIStore1.deleteApplication(applicationInfoDTO.getApplicationId());
    }

    private File getTempFileWithContent(String schema) throws Exception {

        File temp = File.createTempFile("schema", ".graphql");
        temp.deleteOnExit();
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.write(schema);
        out.close();
        return temp;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        org.json.simple.JSONObject tenantConfigBeforeTestCaseJson = (org.json.simple.JSONObject) new JSONParser().parse(tenantConfigBeforeTestCase);
        restAPIAdmin.updateTenantConfig(tenantConfigBeforeTestCaseJson);
        restAPIPublisher.deleteAPI(restAPIId);
        restAPIPublisher.deleteAPI(graphqlAPIId);
        restAPIAdmin.deleteAdvancedThrottlingPolicy(advancedThrottlePolicyDTO.getPolicyId());
        restAPIAdmin.deleteSubscriptionThrottlingPolicy(defaultSubscriptionPolicy.getPolicyId());
        restAPIAdmin.deleteApplicationThrottlingPolicy(defaultApplicationThrottlePolicyDTO.getPolicyId());
        super.cleanUp();
    }

    private void validateThrottlingPolicy(String swaggerContent) throws APIManagementException {

        OpenAPIParser parser = new OpenAPIParser();
        SwaggerParseResult swaggerParseResult = parser.readContents(swaggerContent, null, null);
        OpenAPI openAPI = swaggerParseResult.getOpenAPI();
        Paths paths = openAPI.getPaths();
        for (String pathKey : paths.keySet()) {
            Map<PathItem.HttpMethod, Operation> operationsMap = paths.get(pathKey).readOperationsMap();
            for (Map.Entry<PathItem.HttpMethod, Operation> entry : operationsMap.entrySet()) {
                Operation operation = entry.getValue();
                Map<String, Object> extensions = operation.getExtensions();
                Assert.assertNotNull(extensions.get("x-throttling-tier"));
                Assert.assertEquals(extensions.get("x-throttling-tier"), "DefaultAPIPolicy");
            }
        }
    }
}
