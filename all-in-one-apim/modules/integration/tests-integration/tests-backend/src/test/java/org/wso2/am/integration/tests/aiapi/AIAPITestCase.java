/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

package org.wso2.am.integration.tests.aiapi;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.google.gson.Gson;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.AIServiceProviderResponseDTO;
import org.wso2.am.integration.clients.admin.api.dto.AIServiceProviderSummaryResponseDTO;
import org.wso2.am.integration.clients.admin.api.dto.AIServiceProviderSummaryResponseListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
//import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationPoliciesDTO;
//import org.wso2.am.integration.clients.publisher.api.v1.dto.OperationPolicyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIThrottlingTier;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * AI API Test Case
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class AIAPITestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(AIAPITestCase.class);

    // Variables related to mistral AI API
    private static final String mistralAPIName = "mistralAPI";
    public static final String unsecuredAPIName = "mistralNoAuthAPI";
    private static final String mistralAPIVersion = "1.0.0";
    public static final String unsecuredAPIVersion = "1.0.0";
    private static final String mistralAPIContext = "mistralAPI";
    public static final String unsecuredAPIContext = "mistralNoAuthAPI";
    private static final String mistralAPIEndpoint = "/mistral";
    public static final String mistralAPIEndpoint2 = "/mistral-updated";
    public static final String noAuthAPIEndpoint = "/noauth";
    private static final String mistralAPIResource = "/v1/chat/completions";
    private String mistralAPIId;
    private String unsecuredAPIId;
    private String mistralPayload;
    private String mistralResponse;
    private String model1Response;
    private String model2Response;
    private String model3Response;

    // Application and API related common details
    private static final String apiProvider = "admin";
    private static final String applicationName = "AI-API-Application";
    private String applicationId;
    private String apiKey;

    // Other variables
    private String resourcePath;
    private WireMockServer wireMockServer;
    private int endpointPort;
    private String endpointHost = "http://localhost";
    private int lowerPortLimit = 9950;
    private int upperPortLimit = 9999;

    private String aiServiceProviderId;
    private final String aiServiceProviderName = "TestAIService";
    private final String aiServiceProviderApiVersion = "1.0.0";

    private final String aiServiceProviderDescription = "This is a copy of MistralAI service";
    private final String model1 = "mistral-small-latest";
    private final String model2 = "mistral-medium-latest";
    private final String model3 = "mistral-large-latest";
    private final String modelList = String.format("[\"%s\", \"%s\", \"%s\"]", model1, model2, model3);
    private final String modelProviders = String.format("[{\"models\": %s, \"name\": \"%s\"}]", modelList,
            aiServiceProviderName);
    private final String productionEndpointName = "Prod Endpoint";
    private final String sandboxEndpointName = "Sandbox Endpoint";
    private final String productionDeploymentStage = "PRODUCTION";
    private final String sandboxDeploymentStage = "SANDBOX";
    private String productionEndpointId;
    private String sandboxEndpointId;
    private List<JSONObject> endpointsList = new ArrayList<>();
//    private Map<String, String> policyMap;

    private final String customAIServiceProviderConfigurations = "{\"connectorType\":\"mistralAi_1.0.0\"," +
            "\"authenticationConfiguration\":{\"enabled\":false,\"type\":\"none\"," +
            "\"parameters\":{}}," +
            "\"metadata\":[{\"attributeName\":\"requestModel\",\"inputSource\":\"payload\",\"attributeIdentifier\"" +
            ":\"$.model\",\"required\":false},{\"attributeName\":\"responseModel\",\"inputSource\":\"payload\"," +
            "\"attributeIdentifier\":\"$.model\",\"required\":true},{\"attributeName\":\"promptTokenCount\"," +
            "\"inputSource\":\"payload\",\"attributeIdentifier\":\"$.usage.prompt_tokens\",\"required\":true}," +
            "{\"attributeName\":\"completionTokenCount\",\"inputSource\":\"payload\"," +
            "\"attributeIdentifier\":\"$.usage.completion_tokens\",\"required\":true}," +
            "{\"attributeName\":\"totalTokenCount\",\"inputSource\":\"payload\"," +
            "\"attributeIdentifier\":\"$.usage.total_tokens\",\"required\":true}," +
            "{\"attributeName\":\"remainingTokenCount\",\"inputSource\":\"header\"," +
            "\"attributeIdentifier\":\"x-ratelimit-remaining-tokens\",\"required\":false}]}";

    private final String updatedCustomAIServiceProviderConfigurations = "{\"connectorType\":\"mistralAi_1.0.0\"," +
            "\"authenticationConfiguration\":{\"enabled\":true,\"type\":\"apikey\"," +
            "\"parameters\":{\"headerEnabled\":true,\"queryParameterEnabled\":false,\"headerName\":\"Authorization\"" +
            "}},\"metadata\":[{\"attributeName\":\"requestModel\",\"inputSource\":\"payload\",\"attributeIdentifier\"" +
            ":\"$.model\",\"required\":false},{\"attributeName\":\"responseModel\",\"inputSource\":\"payload\"," +
            "\"attributeIdentifier\":\"$.model\",\"required\":true},{\"attributeName\":\"promptTokenCount\"," +
            "\"inputSource\":\"payload\",\"attributeIdentifier\":\"$.usage.prompt_tokens\",\"required\":true}," +
            "{\"attributeName\":\"completionTokenCount\",\"inputSource\":\"payload\"," +
            "\"attributeIdentifier\":\"$.usage.completion_tokens\",\"required\":true}," +
            "{\"attributeName\":\"totalTokenCount\",\"inputSource\":\"payload\"," +
            "\"attributeIdentifier\":\"$.usage.total_tokens\",\"required\":true}," +
            "{\"attributeName\":\"remainingTokenCount\",\"inputSource\":\"header\"," +
            "\"attributeIdentifier\":\"x-ratelimit-remaining-tokens\",\"required\":false}]}";


    private List<String> defaultAIServiceProviders = new ArrayList<>();

    private final String apiDefinitionFileName = "mistral-def.json";

    private final String mistralResponseFileName = "mistral-response.json";
    @Factory(dataProvider = "userModeDataProvider")
    public AIAPITestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                {TestUserMode.SUPER_TENANT_ADMIN},
                {TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        defaultAIServiceProviders.add("MistralAI");
        defaultAIServiceProviders.add("OpenAI");
        defaultAIServiceProviders.add("AzureOpenAI");
        defaultAIServiceProviders.add("AWSBedrock");

//        policyMap = restAPIPublisher.getAllCommonOperationPolicies();

        // Add application
        ApplicationDTO applicationDTO = restAPIStore.addApplication(applicationName,
                APIThrottlingTier.UNLIMITED.getState(), "", "this-is-test");
        applicationId = applicationDTO.getApplicationId();
        Assert.assertNotNull(applicationId);

        resourcePath = TestConfigurationProvider.getResourceLocation() + "ai-api" + File.separator;
        mistralResponse = readFile(resourcePath + mistralResponseFileName);
        model1Response = mistralResponse.replace("{{jsonPath request.body '$.model'}}", model1);
        model2Response = mistralResponse.replace("{{jsonPath request.body '$.model'}}", model2);
        model3Response = mistralResponse.replace("{{jsonPath request.body '$.model'}}", model3);

        mistralPayload = readFile(resourcePath + "mistral-payload.json");

        // Start WireMock server
        startWiremockServer();
    }

    /**
     * Tests the retrieval of predefined AI service providers and verifies all are present.
     * Ensures that all predefined AI service providers are retrieved successfully.
     */
    @Test(groups = {"wso2.am"}, description = "Test retrieve AI Service Providers")
    public void testPredefinedAIServiceProviders() throws Exception {
        List<String> copyDefaultAIServiceProviders = new ArrayList<>(defaultAIServiceProviders);
        ApiResponse<AIServiceProviderSummaryResponseListDTO> aiServiceProviders = restAPIAdmin.getAIServiceProviders();
        assertEquals(Response.Status.OK.getStatusCode(),
                aiServiceProviders.getStatusCode(), "Failed to retrieve AI service providers");
        for (AIServiceProviderSummaryResponseDTO provider : aiServiceProviders.getData().getList()) {
            if (defaultAIServiceProviders.contains(provider.getName())) {
                copyDefaultAIServiceProviders.remove(provider.getName());
            }
        }
        assertEquals(0,
                copyDefaultAIServiceProviders.size(), "Failed to retrieve all predefined AI service providers");
    }

    /**
     * Adds a custom AI service provider with no auth and verifies successful creation.
     * Ensures the provider is created with the given details and retrieves its ID.
     */
    @Test(groups = {"wso2.am"}, description = "Add AI Service Provider")
    public void addCustomAIServiceProviderWithNoAuth() throws Exception {
        String originalDefinition = readFile(resourcePath + apiDefinitionFileName);
        File file = getTempFileWithContent(originalDefinition);

        ApiResponse<AIServiceProviderResponseDTO> createProviderResponse = restAPIAdmin.addAIServiceProvider(
                aiServiceProviderName, aiServiceProviderApiVersion, aiServiceProviderDescription, false,
                customAIServiceProviderConfigurations, file, modelProviders);

        assertEquals(Response.Status.CREATED.getStatusCode(), createProviderResponse.getStatusCode(),
                "Failed to add an AI service provider");
        aiServiceProviderId = createProviderResponse.getData().getId();
        Assert.assertNotNull(aiServiceProviderId, "AI Service Provider ID is null");
    }

    /**
     * Retrieves a specified AI service provider and verifies the provider's details.
     * Ensures the provider is retrieved successfully and the name and API version match.
     */
    @Test(groups = {"wso2.am"}, description = "Get AI Service Provider",
            dependsOnMethods = "addCustomAIServiceProviderWithNoAuth")
    public void retrieveCustomAIServiceProvider() throws Exception {
        ApiResponse<AIServiceProviderResponseDTO> getProviderResponse = restAPIAdmin.getAIServiceProvider(
                aiServiceProviderId);
        assertEquals(Response.Status.OK.getStatusCode(), getProviderResponse.getStatusCode(),
                "Failed to retrieve AI service provider");
        assertEquals(getProviderResponse.getData().getName(), aiServiceProviderName,
                "AI service provider name does not match");
        assertEquals(getProviderResponse.getData().getApiVersion(), aiServiceProviderApiVersion,
                "AI service provider API version does not match");
    }

    /**
     * Test AI API with unsecured AI service provider. Verify creation, deployment, and publishing
     */
    @Test(groups = {"wso2.am"}, description = "Test Unsecured AI API creation, deployment and publishing",
            dependsOnMethods = "addCustomAIServiceProviderWithNoAuth")
    public void testUnsecuredAIAPICreationAndPublish() throws Exception {

        String apiDefinition = readFile(resourcePath + apiDefinitionFileName);
        String additionalProperties = readFile(resourcePath + "mistral-no-auth-add-props.json");
        JSONObject additionalPropertiesObj = new JSONObject(additionalProperties);

        // Update production endpoint
        JSONObject endpointConfig = additionalPropertiesObj.getJSONObject("endpointConfig");
        JSONObject productionEndpoints = new JSONObject();
        productionEndpoints.put("url", endpointHost + ":" + endpointPort + noAuthAPIEndpoint);
        endpointConfig.put("production_endpoints", productionEndpoints);
        additionalPropertiesObj.put("endpointConfig", endpointConfig);

        // Create API
        File file = getTempFileWithContent(apiDefinition);
        APIDTO apidto = restAPIPublisher.importOASDefinition(file, additionalPropertiesObj.toString());
        unsecuredAPIId = apidto.getId();

        HttpResponse createdApiResponse = restAPIPublisher.getAPI(unsecuredAPIId);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode(),
                "Failed to create AI API using the custom AI Service Provider");

        // Deploy API
        String revisionUUID = createAPIRevisionAndDeployUsingRest(unsecuredAPIId, restAPIPublisher);
        Assert.assertNotNull(revisionUUID);

        // Publish API
        HttpResponse lifecycleResponse = restAPIPublisher
                .changeAPILifeCycleStatusToPublish(unsecuredAPIId, false);
        assertEquals(lifecycleResponse.getResponseCode(), HttpStatus.SC_OK);

        waitForAPIDeploymentSync(apidto.getProvider(),
                apidto.getName(), apidto.getVersion(), APIMIntegrationConstants.IS_API_EXISTS);
    }

    /**
     * Test Unsecured AI API invocation
     */
    @Test(groups = {"wso2.am"}, description = "Test AI API invocation",
            dependsOnMethods = "testUnsecuredAIAPICreationAndPublish")
    public void testUnsecuredAIApiInvocation() throws Exception {

        // Subscribe to API
        SubscriptionDTO subscriptionDTO = restAPIStore.
                subscribeToAPI(unsecuredAPIId, applicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);
        assertNotNull(subscriptionDTO, "Unsecured AI API subscription failed");

        // Get API Key
        APIKeyDTO apiKeyDTO = restAPIStore.
                generateAPIKeys(applicationId, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.toString(),
                -1, null, null);
        assertNotNull(apiKeyDTO, "Unsecured AI API Key generation failed");
        apiKey = apiKeyDTO.getApikey();

        // Invoke API
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("ApiKey", apiKey);
        String invokeURL = getAPIInvocationURLHttp(unsecuredAPIContext, unsecuredAPIVersion) + mistralAPIResource;
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, mistralPayload);

        assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK, "Unsecured AI API invocation failed");
        assertEquals(model1Response, serviceResponse.getData(), "Unsecured AI API response mismatch");
    }

    /**
     * Updates the created AI service provider with apikey auth configurations and verifies the update.
     * Ensures the updated provider is retrieved successfully and the configurations are correct.
     */
    @Test(groups = {"wso2.am"}, description = "Update AI Service Provider",
            dependsOnMethods = "testUnsecuredAIApiInvocation")
    public void updateCustomAIServiceProvider() throws Exception {
        String originalDefinition = readFile(resourcePath + apiDefinitionFileName);
        File file = getTempFileWithContent(originalDefinition);

        ApiResponse<AIServiceProviderResponseDTO> updateProviderResponse = restAPIAdmin.updateAIServiceProvider(
                aiServiceProviderId, aiServiceProviderName, aiServiceProviderApiVersion, aiServiceProviderDescription,
                false, updatedCustomAIServiceProviderConfigurations, file, modelProviders);

        assertEquals(Response.Status.OK.getStatusCode(),
                updateProviderResponse.getStatusCode(), "Failed to update AI service provider");

        ApiResponse<AIServiceProviderResponseDTO> getProviderResponse = restAPIAdmin.getAIServiceProvider(
                aiServiceProviderId);
        assertEquals(Response.Status.OK.getStatusCode(), getProviderResponse.getStatusCode(),
                "Failed to retrieve AI service provider");
        assertEquals(getProviderResponse.getData().getConfigurations(), updatedCustomAIServiceProviderConfigurations,
                "Failed to update AI service provider configurations");
    }

    /**
     * Test AI API creation, deployment, and publishing
     */
    @Test(groups = {"wso2.am"}, description = "Test Mistral AI API creation, deployment and publishing",
            dependsOnMethods = "updateCustomAIServiceProvider")
    public void testSecuredAIAPICreationAndPublish() throws Exception {

        String apiDefinition = readFile(resourcePath + apiDefinitionFileName);
        String additionalProperties = readFile(resourcePath + "mistral-add-props.json");
        JSONObject additionalPropertiesObj = new JSONObject(additionalProperties);

        // Update production endpoint
        JSONObject endpointConfig = additionalPropertiesObj.getJSONObject("endpointConfig");
        JSONObject productionEndpoints = new JSONObject();
        productionEndpoints.put("url", endpointHost + ":" + endpointPort + mistralAPIEndpoint);
        endpointConfig.put("production_endpoints", productionEndpoints);
        additionalPropertiesObj.put("endpointConfig", endpointConfig);

        // Create API
        File file = getTempFileWithContent(apiDefinition);
        APIDTO apidto = restAPIPublisher.importOASDefinition(file, additionalPropertiesObj.toString());
        mistralAPIId = apidto.getId();

        HttpResponse createdApiResponse = restAPIPublisher.getAPI(mistralAPIId);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode(),
                "Failed to create AI API using the custom AI Service Provider");

        // Deploy API
        String revisionUUID = createAPIRevisionAndDeployUsingRest(mistralAPIId, restAPIPublisher);
        Assert.assertNotNull(revisionUUID);

        // Publish API
        HttpResponse lifecycleResponse = restAPIPublisher
                .changeAPILifeCycleStatusToPublish(mistralAPIId, false);
        assertEquals(lifecycleResponse.getResponseCode(), HttpStatus.SC_OK);

        waitForAPIDeploymentSync(apidto.getProvider(),
                apidto.getName(), apidto.getVersion(), APIMIntegrationConstants.IS_API_EXISTS);
    }

    /**
     * Test Mistral AI API invocation
     */
    @Test(groups = {"wso2.am"}, description = "Test AI API invocation",
            dependsOnMethods = "testSecuredAIAPICreationAndPublish")
    public void testSecuredAIApiInvocation() throws Exception {

        // Subscribe to API
        SubscriptionDTO subscriptionDTO = restAPIStore.
                subscribeToAPI(mistralAPIId, applicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);
        assertNotNull(subscriptionDTO, "AI API subscription failed");

        // Get API Key
        APIKeyDTO apiKeyDTO = restAPIStore.
                generateAPIKeys(applicationId, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.toString(),
                        -1, null, null);
        assertNotNull(apiKeyDTO, "AI API Key generation failed");
        apiKey = apiKeyDTO.getApikey();

        // Invoke API
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("ApiKey", apiKey);
        String invokeURL = getAPIInvocationURLHttp(mistralAPIContext, mistralAPIVersion) + mistralAPIResource;
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, mistralPayload);

        assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK, "AI API invocation failed");
        assertEquals(model1Response, serviceResponse.getData(), "AI API response mismatch");
    }

    /**
     * Test endpoint addition to Mistral AI API - handles multiple endpoints (production and sandbox)
     */
    @Test(groups = {"wso2.am"}, description = "Test multiple endpoint addition to AI API",
            dependsOnMethods = "testSecuredAIApiInvocation")
    public void testAddAIAPIEndpoint() throws Exception {
        // Add production endpoint
        String prodEndpointConfig = readFile(resourcePath + "prod-endpoint-add-endpoint-config.json");
        JSONObject prodEndpointConfigObj = new JSONObject(prodEndpointConfig);
        JSONObject productionEndpoints = new JSONObject();
        productionEndpoints.put("url", endpointHost + ":" + endpointPort + mistralAPIEndpoint);
        prodEndpointConfigObj.put("production_endpoints", productionEndpoints);

        Map<String, Object> prodEndpointConfigMap = new Gson().fromJson(prodEndpointConfigObj.toString(), Map.class);
        HttpResponse addProdEndpointResponse = restAPIPublisher.addApiEndpoint(mistralAPIId, productionEndpointName,
                productionDeploymentStage, prodEndpointConfigMap);
        Assert.assertEquals(HttpStatus.SC_CREATED, addProdEndpointResponse.getResponseCode(),
                "Failed to add production endpoint to API: " + mistralAPIId);
        
        // Extract endpoint ID from response for later use
        JSONObject prodEndpointResponse = new JSONObject(addProdEndpointResponse.getData());
        productionEndpointId = prodEndpointResponse.getString("id");
        Assert.assertNotNull(productionEndpointId, "Production endpoint ID should not be null");

        // Add sandbox endpoint
        String sandboxEndpointConfig = readFile(resourcePath + "sandbox-endpoint-config.json");
        JSONObject sandboxEndpointConfigObj = new JSONObject(sandboxEndpointConfig);
        JSONObject sandboxEndpoints = new JSONObject();
        sandboxEndpoints.put("url", endpointHost + ":" + endpointPort + mistralAPIEndpoint + "/sandbox");
        sandboxEndpointConfigObj.put("sandbox_endpoints", sandboxEndpoints);

        Map<String, Object> sandboxEndpointConfigMap = new Gson().fromJson(sandboxEndpointConfigObj.toString(), Map.class);
        HttpResponse addSandboxEndpointResponse = restAPIPublisher.addApiEndpoint(mistralAPIId, sandboxEndpointName,
                sandboxDeploymentStage, sandboxEndpointConfigMap);
        Assert.assertEquals(HttpStatus.SC_CREATED, addSandboxEndpointResponse.getResponseCode(),
                "Failed to add sandbox endpoint to API: " + mistralAPIId);
        
        // Extract endpoint ID from response for later use
        JSONObject sandboxEndpointResponse = new JSONObject(addSandboxEndpointResponse.getData());
        sandboxEndpointId = sandboxEndpointResponse.getString("id");
        Assert.assertNotNull(sandboxEndpointId, "Sandbox endpoint ID should not be null");
    }

    /**
     * Test retrieving all endpoints for an API
     */
    @Test(groups = {"wso2.am"}, description = "Test retrieving all API endpoints",
            dependsOnMethods = "testAddAIAPIEndpoint")
    public void testGetApiEndpoints() throws Exception {
        HttpResponse getEndpointsResponse = restAPIPublisher.getApiEndpoints(mistralAPIId);
        Assert.assertEquals(HttpStatus.SC_OK, getEndpointsResponse.getResponseCode(),
                "Failed to retrieve endpoints for API: " + mistralAPIId);
        
        // Parse response to verify endpoints exist
        JSONObject endpointsResponse = new JSONObject(getEndpointsResponse.getData());
        Assert.assertTrue(endpointsResponse.has("list"), "Response should contain 'list' field");
        
        // Update the endpoints list with the current API response
        ArrayList<JSONObject> retrievedEndpointsList = new ArrayList<>();
        for (int i = 0; i < endpointsResponse.getJSONArray("list").length(); i++) {
            retrievedEndpointsList.add(endpointsResponse.getJSONArray("list").getJSONObject(i));
        }
        
        // Verify that we have at least 2 endpoints (production and sandbox)
        int endpointCount = retrievedEndpointsList.size();
        Assert.assertTrue(endpointCount >= 2, "Should have 2 endpoints, but found: " + endpointCount);
        
        // Verify that our created endpoints are in the list
        boolean foundProductionEndpoint = false;
        boolean foundSandboxEndpoint = false;
        
        for (JSONObject endpoint : retrievedEndpointsList) {
            String endpointId = endpoint.getString("id");
            String endpointName = endpoint.getString("name");
            
            if (productionEndpointId.equals(endpointId) && productionEndpointName.equals(endpointName)) {
                foundProductionEndpoint = true;
            }
            if (sandboxEndpointId.equals(endpointId) && sandboxEndpointName.equals(endpointName)) {
                foundSandboxEndpoint = true;
            }
        }
        
        Assert.assertTrue(foundProductionEndpoint, "Production endpoint not found in the list");
        Assert.assertTrue(foundSandboxEndpoint, "Sandbox endpoint not found in the list");
    }

    /**
     * Test retrieving a specific endpoint by ID
     */
    @Test(groups = {"wso2.am"}, description = "Test retrieving a specific API endpoint",
            dependsOnMethods = "testAddAIAPIEndpoint")
    public void testGetApiEndpoint() throws Exception {
        // Test retrieving production endpoint
        HttpResponse getProdEndpointResponse = restAPIPublisher.getApiEndpoint(mistralAPIId, productionEndpointId);
        Assert.assertEquals(HttpStatus.SC_OK, getProdEndpointResponse.getResponseCode(),
                "Failed to retrieve production endpoint for API: " + mistralAPIId);
        
        // Parse and verify production endpoint details
        JSONObject prodEndpoint = new JSONObject(getProdEndpointResponse.getData());
        Assert.assertEquals(prodEndpoint.getString("id"), productionEndpointId,
                "Retrieved endpoint ID does not match expected production endpoint ID");
        Assert.assertEquals(prodEndpoint.getString("name"), productionEndpointName,
                "Retrieved endpoint name does not match expected production endpoint name");
        Assert.assertEquals(prodEndpoint.getString("deploymentStage"), productionDeploymentStage,
                "Retrieved endpoint deployment stage does not match expected production stage");
        
        // Test retrieving sandbox endpoint
        HttpResponse getSandboxEndpointResponse = restAPIPublisher.getApiEndpoint(mistralAPIId, sandboxEndpointId);
        Assert.assertEquals(HttpStatus.SC_OK, getSandboxEndpointResponse.getResponseCode(),
                "Failed to retrieve sandbox endpoint for API: " + mistralAPIId);
        
        // Parse and verify sandbox endpoint details
        JSONObject sandboxEndpoint = new JSONObject(getSandboxEndpointResponse.getData());
        Assert.assertEquals(sandboxEndpoint.getString("id"), sandboxEndpointId,
                "Retrieved endpoint ID does not match expected sandbox endpoint ID");
        Assert.assertEquals(sandboxEndpoint.getString("name"), sandboxEndpointName,
                "Retrieved endpoint name does not match expected sandbox endpoint name");
        Assert.assertEquals(sandboxEndpoint.getString("deploymentStage"), sandboxDeploymentStage,
                "Retrieved endpoint deployment stage does not match expected sandbox stage");
    }

    /**
     * Test updating an existing endpoint
     */
    @Test(groups = {"wso2.am"}, description = "Test updating an API endpoint",
            dependsOnMethods = "testGetApiEndpoint")
    public void testUpdateApiEndpoint() throws Exception {
        // Create updated endpoint configuration for production endpoint
        String updatedProdEndpointConfig = readFile(resourcePath + "prod-endpoint-add-endpoint-config.json");
        JSONObject updatedProdEndpointConfigObj = new JSONObject(updatedProdEndpointConfig);
        JSONObject updatedProductionEndpoints = new JSONObject();
        // Update the URL to a different endpoint
        updatedProductionEndpoints.put("url", endpointHost + ":" + endpointPort + mistralAPIEndpoint2);
        updatedProdEndpointConfigObj.put("production_endpoints", updatedProductionEndpoints);
        
        // Update the endpoint security configuration
        JSONObject endpointSecurity = updatedProdEndpointConfigObj.getJSONObject("endpoint_security");
        JSONObject productionSecurity = endpointSecurity.getJSONObject("production");
        productionSecurity.put("apiKeyValue", "Bearer 456"); // Change the API key value
        endpointSecurity.put("production", productionSecurity);
        updatedProdEndpointConfigObj.put("endpoint_security", endpointSecurity);

        // Convert JSONObject to Map to avoid "map" wrapper in serialization
        Map<String, Object> updatedProdEndpointConfigMap = new Gson().fromJson(updatedProdEndpointConfigObj.toString(), Map.class);
        // Update the production endpoint
        HttpResponse updateProdEndpointResponse = restAPIPublisher.updateApiEndpoint(mistralAPIId, 
                productionEndpointId, productionEndpointName, productionDeploymentStage, updatedProdEndpointConfigMap);
        Assert.assertEquals(HttpStatus.SC_OK, updateProdEndpointResponse.getResponseCode(),
                "Failed to update production endpoint for API: " + mistralAPIId);
        
        // Verify the update by retrieving the endpoint
        HttpResponse getUpdatedProdEndpointResponse = restAPIPublisher.getApiEndpoint(mistralAPIId, productionEndpointId);
        Assert.assertEquals(HttpStatus.SC_OK, getUpdatedProdEndpointResponse.getResponseCode(),
                "Failed to retrieve updated production endpoint");
        
        JSONObject updatedProdEndpoint = new JSONObject(getUpdatedProdEndpointResponse.getData());
        JSONObject endpointConfig = new JSONObject(updatedProdEndpoint.getString("endpointConfig"));
        JSONObject prodEndpoints = endpointConfig.getJSONObject("production_endpoints");
        String updatedUrl = prodEndpoints.getString("url");
        Assert.assertTrue(updatedUrl.contains(mistralAPIEndpoint2),
                "Updated endpoint URL should contain '/mistral-updated' but was: " + updatedUrl);
        
        // Verify the security configuration was updated
        JSONObject updatedEndpointSecurity = endpointConfig.getJSONObject("endpoint_security");
        JSONObject updatedProductionSecurity = updatedEndpointSecurity.getJSONObject("production");
        String updatedApiKeyValue = updatedProductionSecurity.getString("apiKeyValue");
        Assert.assertEquals(updatedApiKeyValue, "Bearer 456",
                "Updated API key value should be 'Bearer 456' but was: " + updatedApiKeyValue);
    }

    /**
     * Test deleting an API endpoint
     */
    @Test(groups = {"wso2.am"}, description = "Test deleting an API endpoint",
            dependsOnMethods = "testUpdateApiEndpoint")
    public void testDeleteApiEndpoint() throws Exception {
        // First verify that the sandbox endpoint exists
        HttpResponse getSandboxEndpointResponse = restAPIPublisher.getApiEndpoint(mistralAPIId, sandboxEndpointId);
        Assert.assertEquals(HttpStatus.SC_OK, getSandboxEndpointResponse.getResponseCode(),
                "Sandbox endpoint should exist before deletion");
        
        // Delete the sandbox endpoint
        HttpResponse deleteSandboxEndpointResponse = restAPIPublisher.deleteApiEndpoint(mistralAPIId, sandboxEndpointId);
        Assert.assertEquals(HttpStatus.SC_OK, deleteSandboxEndpointResponse.getResponseCode(),
                "Failed to delete sandbox endpoint for API: " + mistralAPIId);
        
        // Verify that the endpoint is deleted by trying to retrieve it
        HttpResponse getDeletedSandboxEndpointResponse = restAPIPublisher.getApiEndpoint(mistralAPIId, sandboxEndpointId);
        Assert.assertEquals(HttpStatus.SC_NOT_FOUND, getDeletedSandboxEndpointResponse.getResponseCode(),
                "Deleted sandbox endpoint should not be retrievable");
        
        // Verify that the production endpoint still exists
        HttpResponse getProdEndpointResponse = restAPIPublisher.getApiEndpoint(mistralAPIId, productionEndpointId);
        Assert.assertEquals(HttpStatus.SC_OK, getProdEndpointResponse.getResponseCode(),
                "Production endpoint should still exist after deleting sandbox endpoint");
        
        // Verify the endpoint count in the list has decreased
        HttpResponse getEndpointsResponse = restAPIPublisher.getApiEndpoints(mistralAPIId);
        Assert.assertEquals(HttpStatus.SC_OK, getEndpointsResponse.getResponseCode(),
                "Failed to retrieve endpoints list after deletion");
        
        // Parse the response and verify endpoint count
        JSONObject endpointsResponse = new JSONObject(getEndpointsResponse.getData());
        int endpointCount = endpointsResponse.getJSONArray("list").length();
        Assert.assertTrue(endpointCount >= 1, "Should have at least 1 endpoint remaining, but found: " + endpointCount);
        
        // Verify that the sandbox endpoint is not in the list anymore
        boolean foundSandboxEndpoint = false;
        for (int i = 0; i < endpointCount; i++) {
            JSONObject endpoint = endpointsResponse.getJSONArray("list").getJSONObject(i);
            String endpointId = endpoint.getString("id");
            if (sandboxEndpointId.equals(endpointId)) {
                foundSandboxEndpoint = true;
                break;
            }
        }
        Assert.assertFalse(foundSandboxEndpoint, "Deleted sandbox endpoint should not be in the endpoints list");
    }

    /**
     * Test final retrieval of all endpoints to verify the complete endpoint lifecycle
     */
    @Test(groups = {"wso2.am"}, description = "Test final retrieval of all API endpoints",
            dependsOnMethods = "testDeleteApiEndpoint")
    public void testGetAllEndpointsFinal() throws Exception {
        // Get all endpoints one final time
        HttpResponse getEndpointsResponse = restAPIPublisher.getApiEndpoints(mistralAPIId);
        Assert.assertEquals(HttpStatus.SC_OK, getEndpointsResponse.getResponseCode(),
                "Failed to retrieve final endpoints list for API: " + mistralAPIId);
        
        // Update the endpoints list with the final API response
        JSONObject endpointsResponse = new JSONObject(getEndpointsResponse.getData());
        for (int i = 0; i < endpointsResponse.getJSONArray("list").length(); i++) {
            endpointsList.add(endpointsResponse.getJSONArray("list").getJSONObject(i));
        }
        
        // Verify that we have at least 1 endpoint remaining (production endpoint)
        Assert.assertTrue(endpointsList.size() >= 1, 
                "Should have at least 1 endpoint remaining, but found: " + endpointsList.size());
        
        // Verify that the production endpoint still exists
        boolean foundProductionEndpoint = false;
        for (JSONObject endpoint : endpointsList) {
            if (productionEndpointId.equals(endpoint.getString("id"))) {
                foundProductionEndpoint = true;
                break;
            }
        }
        Assert.assertTrue(foundProductionEndpoint, "Production endpoint should still exist in the final list");
        
        // Verify that the sandbox endpoint is completely removed
        boolean foundSandboxEndpoint = false;
        for (JSONObject endpoint : endpointsList) {
            if (sandboxEndpointId.equals(endpoint.getString("id"))) {
                foundSandboxEndpoint = true;
                break;
            }
        }
        Assert.assertFalse(foundSandboxEndpoint, "Sandbox endpoint should be completely removed from the final list");
    }

//    /**
//     * Test Mistral AI API invocation after adding model round-robin policy
//     */
//    @Test(groups = {"wso2.am"}, description = "Test AI API invocation after adding model round-robin policy",
//            dependsOnMethods = "testMistralAIApiEndpointAddition")
//    public void testMistralAIApiInvocationAfterAddingModelRoundRobinPolicy() throws Exception {
//
//        // Retrieve model list
//        HttpResponse getModelListResponse = restAPIPublisher.getLLMProviderModelList(aiServiceProviderId);
//        Assert.assertEquals(HttpStatus.SC_OK, getModelListResponse.getResponseCode(),
//                "Failed to retrieve model list of LLM provider: " + aiServiceProviderId);
//
////        HttpResponse getAPIResponse = restAPIPublisher.getAPI(mistralAPIId);
////        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);
////
////        // Add weighted round-robin policy
////        String policyName = "modelWeightedRoundRobin";
////        Assert.assertNotNull(policyMap.get(policyName), "Unable to find a common policy with name " + policyName);
////
////        Map<String, Object> attributeMap = new HashMap<>();
////        JSONObject weightedRoundRobinConfigs = new JSONObject();
////        List<JSONObject> productionModelList = new ArrayList<>();
////        JSONObject model1Obj = new JSONObject();
////        model1Obj.put("model", model1);
////        model1Obj.put("endpointId", );
////        model1Obj.put("weight", 80);
////        productionModelList.add();
////        weightedRoundRobinConfigs.put("production", productionModelList);
////
////        attributeMap.put("weightedRoundRobinConfigs", "");
////
////        List<OperationPolicyDTO> requestPolicyList = new ArrayList<>();
////        OperationPolicyDTO roundRobinPolicyDTO = new OperationPolicyDTO();
////        roundRobinPolicyDTO.setPolicyName(policyName);
////        roundRobinPolicyDTO.setPolicyType("common");
////        roundRobinPolicyDTO.setPolicyId(policyMap.get(policyName));
////        roundRobinPolicyDTO.setParameters(attributeMap);
////        requestPolicyList.add(roundRobinPolicyDTO);
////
////        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
////        apiOperationPoliciesDTO.setRequest(requestPolicyList);
////        apidto.setApiPolicies(apiOperationPoliciesDTO);
////        restAPIPublisher.updateAPI(apidto);
////
////        // Create Revision and Deploy to Gateway
////        createAPIRevisionAndDeployUsingRest(mistralAPIId, restAPIPublisher);
////        waitForAPIDeployment();
////
////        // Invoke API
////        Map<String, String> requestHeaders = new HashMap<>();
////        requestHeaders.put("ApiKey", apiKey);
////        String invokeURL = getAPIInvocationURLHttp(mistralAPIContext, mistralAPIVersion) + mistralAPIResource;
////        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, mistralPayload);
////
////        assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK, "Failed to invoke Mistral AI API");
////        assertEquals(model1Response, serviceResponse.getData(), "Mistral AI API response mismatch");
//    }

//    testCreateNewVersionAfterAddingModelRoundRobinPolicy

    /**
     * Deletes a specified AI service provider after removing API subscriptions and applications.
     * Verifies that the provider is successfully deleted and no longer listed.
     */
    @Test(groups = {"wso2.am"}, description = "Delete AI Service Provider",
            dependsOnMethods = "testGetAllEndpointsFinal")
    public void deleteAIServiceProvider() throws Exception {

        restAPIStore.removeAPISubscriptionByName(unsecuredAPIName, unsecuredAPIVersion, apiProvider,
                applicationName);
        restAPIStore.removeAPISubscriptionByName(mistralAPIName, mistralAPIVersion, apiProvider, applicationName);
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(unsecuredAPIId);
        restAPIPublisher.deleteAPI(mistralAPIId);

        ApiResponse<Void> deleteProviderResponse = restAPIAdmin.deleteAIServiceProvider(aiServiceProviderId);
        assertEquals(Response.Status.OK.getStatusCode(),
                deleteProviderResponse.getStatusCode(), "Failed to delete AI service provider");

        ApiResponse<AIServiceProviderSummaryResponseListDTO> aiServiceProviders = restAPIAdmin.getAIServiceProviders();
        assertEquals(Response.Status.OK.getStatusCode(),
                aiServiceProviders.getStatusCode(), "Failed to retrieve AI service providers");
        for (AIServiceProviderSummaryResponseDTO provider : aiServiceProviders.getData().getList()) {
            if (provider.getName().equals(aiServiceProviderName)) {
                Assert.fail("AI Service Provider " + aiServiceProviderName + " has not deleted correctly");
            }
        }

    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {

        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    private File getTempFileWithContent(String content) throws IOException {

        File temp = File.createTempFile("swagger", ".json");
        try (BufferedWriter out = new BufferedWriter(new FileWriter(temp))) {
            out.write(content);
        }
        temp.deleteOnExit();
        return temp;
    }

    private void startWiremockServer() {

        endpointPort = getAvailablePort();
        wireMockServer = new WireMockServer(options()
                .port(endpointPort)
                .extensions(new ResponseTemplateTransformer(true)));

        // Stub for secured AI API (with Authorization header value of Bearer 123)
        wireMockServer.stubFor(WireMock.post(urlEqualTo(mistralAPIEndpoint + mistralAPIResource))
                .withHeader("Authorization", WireMock.matching("Bearer 123"))
                .withRequestBody(matchingJsonPath("$.model", equalTo(model1)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mistralResponse)
                        .withTransformers("response-template")));

        // Stub for secured AI API (with Authorization header value of Bearer 456)
        wireMockServer.stubFor(WireMock.post(urlEqualTo(mistralAPIEndpoint2 + mistralAPIResource))
                .withHeader("Authorization", WireMock.matching("Bearer 456"))
                .withRequestBody(matchingJsonPath("$.model", equalTo(model1)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mistralResponse)
                        .withTransformers("response-template")));

        // Stub for unsecured AI API (no Authorization header)
        wireMockServer.stubFor(WireMock.post(urlEqualTo(noAuthAPIEndpoint + mistralAPIResource))
                .withRequestBody(matchingJsonPath("$.model", equalTo(model1)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mistralResponse)
                        .withTransformers("response-template")));

        wireMockServer.start();
        log.info("Wiremock server started on port " + endpointPort);
    }

    /**
     * Find a free port to start backend WebSocket server in given port range
     *
     * @return Available Port Number
     */
    private int getAvailablePort() {

        while (lowerPortLimit < upperPortLimit) {
            if (isPortFree(lowerPortLimit)) {
                return lowerPortLimit;
            }
            lowerPortLimit++;
        }
        return -1;
    }

    /**
     * Check whether given port is available
     *
     * @param port Port Number
     * @return status
     */
    private boolean isPortFree(int port) {

        Socket s = null;
        try {
            s = new Socket(endpointHost, port);
            // something is using the port and has responded.
            return false;
        } catch (IOException e) {
            // port available
            return true;
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to close connection ", e);
                }
            }
        }
    }
}
