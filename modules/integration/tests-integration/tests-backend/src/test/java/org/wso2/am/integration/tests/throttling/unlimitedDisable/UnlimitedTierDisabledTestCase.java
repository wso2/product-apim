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
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLValidationResponseDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLValidationResponseGraphQLInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.SettingsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;

/**
 * Publish a API under Unlimited tier and test the invocation , then disable unlimited tier and change the api
 * tier to silver and do a new silver subscription and test invocation under Silver tier.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class UnlimitedTierDisabledTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(UnlimitedTierDisabledTestCase.class);
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String GRAPHQL_API_NAME = "CountriesGraphqlAPI";
    private final String END_POINT_URL = "https://localhost:9943/am-graphQL-sample/api/graphql/";
    private String providerName;
    private String schemaDefinition;
    private String graphqlAPIId;
    private String restAPIId;

    @Factory(dataProvider = "userModeDataProvider")
    public UnlimitedTierDisabledTestCase(TestUserMode userMode) {

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
        environment.add("Production and Sandbox");

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("name", GRAPHQL_API_NAME);
        additionalPropertiesObj.put("context", "graphqlAPI");
        additionalPropertiesObj.put("version", API_VERSION_1_0_0);

        JSONObject url = new JSONObject();
        url.put("url", END_POINT_URL);
        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("sandbox_endpoints", url);
        endpointConfig.put("production_endpoints", url);
        additionalPropertiesObj.put("endpointConfig", endpointConfig);
        additionalPropertiesObj.put("gatewayEnvironments", environment);
        additionalPropertiesObj.put("operations", operations);

        // create Graphql API
        APIDTO apidto = restAPIPublisher.importGraphqlSchemaDefinition(file, additionalPropertiesObj.toString());
        graphqlAPIId = apidto.getId();
        List<APIOperationsDTO> operationsList = apidto.getOperations();
        log.info("operationsList" + operationsList);
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(graphqlAPIId);
        //get the operation of the API and check whether throttling tier is not unlimited
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode());
        Assert.assertFalse(operationsList.get(0).getThrottlingPolicy().equalsIgnoreCase("Unlimited"),
                "Throttling policy " + operationsList.get(0).getThrottlingPolicy() + " is applied");

    }

    @Test(groups = {"wso2.am"}, description = "Create a REST API without x-throttling tier and check if " +
            "API gets published successfully")
    public void createAPIwithThrottlingTierNull() throws Exception {
        //create a REST API
        String swaggerPath = getAMResourceLocation() + File.separator + "configFiles" + File.separator + "unlimitedTier"
                + File.separator + "TestAPI.yaml";
        File definition = new File(swaggerPath);
        JSONObject endpoints = new JSONObject();
        endpoints.put("url", "test");

        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("production_endpoints", endpoints);
        endpointConfig.put("sandbox_endpoints", endpoints);

        List<String> tierList = new ArrayList<>();
        tierList.add(APIMIntegrationConstants.API_TIER.SILVER);
        tierList.add(APIMIntegrationConstants.API_TIER.GOLD);

        JSONObject apiProperties = new JSONObject();
        apiProperties.put("name", "TestAPI");
        apiProperties.put("context", "/testAPIwithUnlimitedTierDisabled");
        apiProperties.put("version", "1.0.0");
        apiProperties.put("provider", providerName);
        apiProperties.put("endpointConfig", endpointConfig);
        apiProperties.put("policies", tierList);
        APIDTO restAPIDTO = restAPIPublisher.importOASDefinition(definition, apiProperties.toString());
        restAPIId = restAPIDTO.getId();
        restAPIPublisher.changeAPILifeCycleStatus(restAPIId, Constants.PUBLISHED);
        APIDTO retrievedDto = restAPIPublisher.getAPIByID(restAPIId);
        Assert.assertNotNull(retrievedDto);
        Assert.assertNotNull(retrievedDto.getOperations());
        for (APIOperationsDTO operation : retrievedDto.getOperations()) {
            Assert.assertNotEquals(operation.getThrottlingPolicy(), "Unlimited");
        }
        String retrievedSwagger = restAPIPublisher.getSwaggerByID(restAPIId);
        retrievedSwagger = restAPIPublisher.updateSwagger(restAPIId, retrievedSwagger);
        Assert.assertNotNull(retrievedSwagger);
        validateThrottlingPolicyNotUnlimited(retrievedSwagger);
        retrievedDto.getPolicies().add("Unlimited");
        try {
            restAPIPublisher.updateAPI(retrievedDto, restAPIId);
            Assert.fail("API Update Successful with Unlimited Subscription Policy.");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 400);
            Assert.assertTrue(e.getResponseBody().contains("Unlimited"));
        }
    }

    @Test(groups = {"wso2.am"}, description = "Create a REST API with x-throttling tier as Unlimited and check if " +
            "API gets published successfully")
    public void createAPIwithThrottlingTierUnlimitedNegative1() throws Exception {
        //create a REST API
        String swaggerPath = getAMResourceLocation() + File.separator + "configFiles" + File.separator + "unlimitedTier"
                + File.separator + "UnlimitedTierAvailableTestAPI.yaml";
        File definition = new File(swaggerPath);
        JSONObject endpoints = new JSONObject();
        endpoints.put("url", "test");

        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("production_endpoints", endpoints);
        endpointConfig.put("sandbox_endpoints", endpoints);

        List<String> tierList = new ArrayList<>();
        tierList.add(APIMIntegrationConstants.API_TIER.SILVER);
        tierList.add(APIMIntegrationConstants.API_TIER.GOLD);

        JSONObject apiProperties = new JSONObject();
        apiProperties.put("name", "TestAPINegative");
        apiProperties.put("context", "/testAPIwithUnlimitedTierDisabledNegative");
        apiProperties.put("version", "1.0.0");
        apiProperties.put("provider", providerName);
        apiProperties.put("endpointConfig", endpointConfig);
        apiProperties.put("policies", tierList);
        try {
            restAPIPublisher.importOASDefinitionResponse(definition, apiProperties.toString());
            Assert.fail("API Imported Successfully with Unlimited Tier");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 500);
            Assert.assertTrue(e.getResponseBody().contains("Unlimited"));
        }
    }

    @Test(groups = {"wso2.am"}, description = "Create Application with Application tier as Unlimited successfully.")
    public void createApplicationWithUnlimitedTierNegative2() {

        try {
            restAPIStore.createApplicationWithHttpInfo("UnlimitedNegativeApp", "", "Unlimited",
                    ApplicationDTO.TokenTypeEnum.JWT);
            Assert.fail("Application created successfully with Unlimited Tier");
        } catch (org.wso2.am.integration.clients.store.api.ApiException e) {
            Assert.assertEquals(e.getCode(), 400);
            Assert.assertTrue(e.getResponseBody().contains("Unlimited"));
        }
    }

    @Test(groups = {"wso2.am"}, description = "Check Setting API not returning Unlimited Tier")
    public void verifySettingsRestAPIInPublisher() throws ApiException {

        SettingsDTO settings = restAPIPublisher.getSettings();
        Assert.assertNotEquals(settings.getDefaultAdvancePolicy(), "Unlimited");
        Assert.assertNotEquals(settings.getDefaultSubscriptionPolicy(), "Unlimited");
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

        restAPIPublisher.deleteAPIByID(restAPIId);
        restAPIPublisher.deleteAPIByID(graphqlAPIId);
    }

    private void validateThrottlingPolicyNotUnlimited(String swaggerContent) throws APIManagementException {

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
                Assert.assertNotEquals(extensions.get("x-throttling-tier"), "Unlimited");
            }
        }
    }
}