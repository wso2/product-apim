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
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
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
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationPoliciesDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.OperationPolicyDTO;
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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * AI API Test Case
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class AIAPITestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(AIAPITestCase.class);

    // API Configuration Constants
    private static final String MISTRAL_API_NAME = "mistralAPI";
    private static final String UNSECURED_API_NAME = "mistralNoAuthAPI";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private static final String API_VERSION_2_0_0 = "2.0.0";
    private static final String MISTRAL_API_CONTEXT = "mistralAPI";
    private static final String UNSECURED_API_CONTEXT = "mistralNoAuthAPI";
    private static final String MISTRAL_API_ENDPOINT = "/mistral";
    private static final String MISTRAL_API_ENDPOINT_UPDATED = "/mistral-updated";
    private static final String NO_AUTH_API_ENDPOINT = "/no-auth";
    private static final String MISTRAL_API_RESOURCE = "/v1/chat/completions";
    // API Instance Variables
    private String mistralApiId;
    private String unsecuredApiId;
    private String mistralPayload;
    private String model1Response;
    private String model2Response;
    private String model3Response;

    // Application Configuration Constants
    private static final String API_PROVIDER = "admin";
    private static final String APPLICATION_NAME = "AI-API-Application";

    // Application Instance Variables
    private String applicationId;
    private String apiKey;

    // WireMock Configuration
    private static final String ENDPOINT_HOST = "http://localhost";

    // WireMock Instance Variables
    private String resourcePath;
    private WireMockServer wireMockServer;
    private int endpointPort;

    // AI Service Provider Configuration Constants
    private static final String AI_SERVICE_PROVIDER_NAME = "TestAIService";
    private static final String AI_SERVICE_PROVIDER_API_VERSION = "1.0.0";
    private static final String AI_SERVICE_PROVIDER_DESCRIPTION = "This is a copy of MistralAI service";
    private static final String MODEL_SMALL = "mistral-small-latest";
    private static final String MODEL_MEDIUM = "mistral-medium-latest";
    private static final String MODEL_LARGE = "mistral-large-latest";

    // AI Service Provider Instance Variables
    private String aiServiceProviderId;
    private String modelProviders;
    // Endpoint Configuration Constants
    private static final String PRODUCTION_ENDPOINT_NAME = "Prod Endpoint";
    private static final String SANDBOX_ENDPOINT_NAME = "Sandbox Endpoint";
    private static final String PRODUCTION_DEPLOYMENT_STAGE = "PRODUCTION";
    private static final String SANDBOX_DEPLOYMENT_STAGE = "SANDBOX";
    private static final String DEFAULT_PRODUCTION_ENDPOINT_ID = "default_production_endpoint";
    private static final String DEFAULT_PRODUCTION_ENDPOINT_NAME = "Default Production Endpoint";
    private static final String FAILOVER_ENDPOINT_URL = "/failover-endpoint";
    private static final String FAILOVER_ENDPOINT_NAME = "Failover Endpoint";
    // Endpoint Instance Variables
    private String productionEndpointId;
    private String sandboxEndpointId;
    private String newVersionApiId;
    private final List<JSONObject> endpointsList = new ArrayList<>();
    private Map<String, String> policyMap;

    // AI Service Provider Configuration Variables (loaded from JSON files)
    private String customAiServiceProviderConfigurations;
    private String updatedCustomAiServiceProviderConfigurations;

    // File Configuration Constants
    private static final String API_DEFINITION_FILE_NAME = "mistral-def.json";
    private static final String MISTRAL_RESPONSE_FILE_NAME = "mistral-response.json";
    private static final String MISTRAL_PAYLOAD_FILE_NAME = "mistral-payload.json";
    private static final String AI_SERVICE_PROVIDER_CONFIG_NO_AUTH_FILE = "ai-service-provider-config-no-auth.json";
    private static final String AI_SERVICE_PROVIDER_CONFIG_WITH_AUTH_FILE = "ai-service-provider-config-with-auth.json";
    private static final String ENDPOINT_CONFIG_TEMPLATE_FILE = "endpoint-config-template.json";

    // Default AI Service Providers
    private final List<String> defaultAiServiceProviders = new ArrayList<>();
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
        initializeDefaultAiServiceProviders();
        initializeModelConfigurations();
        policyMap = restAPIPublisher.getAllCommonOperationPolicies();
        applicationId = createTestApplication();
        initializeTestData();
        startWiremockServer();
    }

    /**
     * Tests the retrieval of predefined AI service providers and validates against expected providers.
     * Verifies that all expected built-in AI service providers are present and have correct properties.
     */
    @Test(groups = {"wso2.am"}, description = "Test retrieve AI Service Providers")
    public void testPredefinedAiServiceProviders() throws Exception {
        ApiResponse<AIServiceProviderSummaryResponseListDTO> aiServiceProviders = restAPIAdmin.getAIServiceProviders();
        assertEquals(Response.Status.OK.getStatusCode(), aiServiceProviders.getStatusCode(),
                "Failed to retrieve AI service providers");

        List<AIServiceProviderSummaryResponseDTO> providerList = aiServiceProviders.getData().getList();
        assertNotNull(providerList, "AI service providers list should not be null");
        assertTrue(providerList.size() >= defaultAiServiceProviders.size(),
                "Expected at least " + defaultAiServiceProviders.size() + " providers, but found: " + providerList.size());

        // Create a map of retrieved providers for easier lookup
        Map<String, AIServiceProviderSummaryResponseDTO> retrievedProviders = new HashMap<>();
        for (AIServiceProviderSummaryResponseDTO provider : providerList) {
            retrievedProviders.put(provider.getName(), provider);
        }

        // Validate each expected provider is present and has correct properties
        List<String> missingProviders = new ArrayList<>();
        for (String expectedProvider : defaultAiServiceProviders) {
            AIServiceProviderSummaryResponseDTO provider = retrievedProviders.get(expectedProvider);
            if (provider == null) {
                missingProviders.add(expectedProvider);
            } else {
                // Validate provider properties
                assertNotNull(provider.getId(), "Provider ID should not be null for: " + expectedProvider);
                assertEquals(expectedProvider, provider.getName(), "Provider name mismatch");
                assertNotNull(provider.getApiVersion(), "API version should not be null for: " + expectedProvider);
                assertTrue(provider.isBuiltInSupport(), "Provider should have built-in support: " + expectedProvider);
                assertNotNull(provider.getDescription(), "Description should not be null for: " + expectedProvider);

                log.info(
                        "Validated AI service provider: " + expectedProvider + " (ID: " + provider.getId() + ", Version: " + provider.getApiVersion() + ")");
            }
        }

        assertTrue(missingProviders.isEmpty(), "Missing expected AI service providers: " + missingProviders);

        log.info("Successfully validated " + defaultAiServiceProviders.size() + " predefined AI service providers");
    }

    /**
     * Adds a custom AI service provider with no auth and verifies successful creation.
     * Ensures the provider is created with the given details and retrieves its ID.
     */
    @Test(groups = {"wso2.am"}, description = "Add AI Service Provider")
    public void addCustomAiServiceProviderWithNoAuth() throws Exception {
        String originalDefinition = readFile(resourcePath + API_DEFINITION_FILE_NAME);
        File file = getTempFileWithContent(originalDefinition);

        ApiResponse<AIServiceProviderResponseDTO> createProviderResponse = restAPIAdmin.addAIServiceProvider(
                AI_SERVICE_PROVIDER_NAME, AI_SERVICE_PROVIDER_API_VERSION, AI_SERVICE_PROVIDER_DESCRIPTION, false,
                customAiServiceProviderConfigurations, file, modelProviders);

        assertEquals(createProviderResponse.getStatusCode(), Response.Status.CREATED.getStatusCode(),
                "Failed to add an AI service provider");
        aiServiceProviderId = createProviderResponse.getData().getId();
        assertNotNull(aiServiceProviderId, "AI Service Provider ID should not be null");
    }

    /**
     * Retrieves a specified AI service provider and verifies the provider's details.
     * Ensures the provider is retrieved successfully and the name and API version match.
     */
    @Test(groups = {"wso2.am"}, description = "Get AI Service Provider",
            dependsOnMethods = "addCustomAiServiceProviderWithNoAuth")
    public void retrieveCustomAiServiceProvider() throws Exception {
        ApiResponse<AIServiceProviderResponseDTO> getProviderResponse = restAPIAdmin.getAIServiceProvider(
                aiServiceProviderId);
        assertEquals(getProviderResponse.getStatusCode(), Response.Status.OK.getStatusCode(),
                "Failed to retrieve AI service provider");
        assertEquals(getProviderResponse.getData().getName(), AI_SERVICE_PROVIDER_NAME,
                "AI service provider name does not match");
        assertEquals(getProviderResponse.getData().getApiVersion(), AI_SERVICE_PROVIDER_API_VERSION,
                "AI service provider API version does not match");
    }

    /**
     * Test AI API with unsecured AI service provider. Verify creation, deployment, and publishing
     */
    @Test(groups = {"wso2.am"}, description = "Test Unsecured AI API creation, deployment and publishing",
            dependsOnMethods = "addCustomAiServiceProviderWithNoAuth")
    public void testUnsecuredAiApiCreationAndPublish() throws Exception {
        unsecuredApiId = createAndPublishAiApi(NO_AUTH_API_ENDPOINT, "mistral-no-auth-add-props.json");
    }

    /**
     * Test Unsecured AI API invocation
     */
    @Test(groups = {"wso2.am"}, description = "Test AI API invocation",
            dependsOnMethods = "testUnsecuredAiApiCreationAndPublish")
    public void testUnsecuredAiApiInvocation() throws Exception {
        apiKey = subscribeToApiAndGenerateKey(unsecuredApiId);

        // Invoke API
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("ApiKey", apiKey);
        requestHeaders.put("Content-Type", "application/json");
        String invokeURL = getAPIInvocationURLHttp(UNSECURED_API_CONTEXT, API_VERSION_1_0_0) + MISTRAL_API_RESOURCE;
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, mistralPayload);

        assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK, "Unsecured AI API invocation failed");
        assertEquals(serviceResponse.getData(), model1Response, "Unsecured AI API response mismatch");
    }

    /**
     * Updates the created AI service provider with apikey auth configurations and verifies the update.
     * Ensures the updated provider is retrieved successfully and the configurations are correct.
     */
    @Test(groups = {"wso2.am"}, description = "Update AI Service Provider",
            dependsOnMethods = "testUnsecuredAiApiInvocation")
    public void updateCustomAiServiceProvider() throws Exception {
        String originalDefinition = readFile(resourcePath + API_DEFINITION_FILE_NAME);
        File file = getTempFileWithContent(originalDefinition);

        ApiResponse<AIServiceProviderResponseDTO> updateProviderResponse = restAPIAdmin.updateAIServiceProvider(
                aiServiceProviderId, AI_SERVICE_PROVIDER_NAME, AI_SERVICE_PROVIDER_API_VERSION,
                AI_SERVICE_PROVIDER_DESCRIPTION, false, updatedCustomAiServiceProviderConfigurations, file,
                modelProviders);

        assertEquals(updateProviderResponse.getStatusCode(), Response.Status.OK.getStatusCode(),
                "Failed to update AI service provider");

        ApiResponse<AIServiceProviderResponseDTO> getProviderResponse = restAPIAdmin.getAIServiceProvider(
                aiServiceProviderId);
        assertEquals(getProviderResponse.getStatusCode(), Response.Status.OK.getStatusCode(),
                "Failed to retrieve AI service provider");
        assertEquals(getProviderResponse.getData().getConfigurations(), updatedCustomAiServiceProviderConfigurations,
                "Failed to update AI service provider configurations");
    }

    /**
     * Test AI API creation, deployment, and publishing
     */
    @Test(groups = {"wso2.am"}, description = "Test Mistral AI API creation, deployment and publishing",
            dependsOnMethods = "updateCustomAiServiceProvider")
    public void testSecuredAiApiCreationAndPublish() throws Exception {
        mistralApiId = createAndPublishAiApi(MISTRAL_API_ENDPOINT, "mistral-add-props.json");
    }

    /**
     * Test Mistral AI API invocation
     */
    @Test(groups = {"wso2.am"}, description = "Test AI API invocation",
            dependsOnMethods = "testSecuredAiApiCreationAndPublish")
    public void testSecuredAiApiInvocation() throws Exception {
        // Note: Use existing API key from previous test to avoid regeneration
        if (apiKey == null) {
            apiKey = subscribeToApiAndGenerateKey(mistralApiId);
        } else {
            // Just subscribe the existing application
            SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(mistralApiId, applicationId,
                    APIMIntegrationConstants.API_TIER.UNLIMITED);
            assertNotNull(subscriptionDTO, "AI API subscription failed");
        }

        // Invoke API
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("ApiKey", apiKey);
        requestHeaders.put("Content-Type", "application/json");
        String invokeURL = getAPIInvocationURLHttp(MISTRAL_API_CONTEXT, API_VERSION_1_0_0) + MISTRAL_API_RESOURCE;
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, mistralPayload);

        assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK, "AI API invocation failed");
        assertEquals(serviceResponse.getData(), model1Response, "AI API response mismatch");
    }

    /**
     * Test endpoint addition to Mistral AI API - handles multiple endpoints (production and sandbox)
     */
    @Test(groups = {"wso2.am"}, description = "Test multiple endpoint addition to AI API",
            dependsOnMethods = "testSecuredAiApiInvocation")
    public void testAddAiApiEndpoint() throws Exception {
        // Add production endpoint
        String endpointConfig = readFile(resourcePath + ENDPOINT_CONFIG_TEMPLATE_FILE);
        JSONObject prodEndpointConfigObj = new JSONObject(endpointConfig);
        JSONObject productionEndpoints = new JSONObject();
        productionEndpoints.put("url", ENDPOINT_HOST + ":" + endpointPort + MISTRAL_API_ENDPOINT);
        prodEndpointConfigObj.put("production_endpoints", productionEndpoints);

        // Remove sandbox section from endpoint_security for production endpoint
        JSONObject endpointSecurity = prodEndpointConfigObj.getJSONObject("endpoint_security");
        endpointSecurity.remove("sandbox");
        prodEndpointConfigObj.put("endpoint_security", endpointSecurity);

        Map prodEndpointConfigMap = new Gson().fromJson(prodEndpointConfigObj.toString(), Map.class);
        HttpResponse addProdEndpointResponse = restAPIPublisher.addApiEndpoint(mistralApiId, PRODUCTION_ENDPOINT_NAME,
                PRODUCTION_DEPLOYMENT_STAGE, prodEndpointConfigMap);
        assertEquals(addProdEndpointResponse.getResponseCode(), HttpStatus.SC_CREATED,
                "Failed to add production endpoint to API: " + mistralApiId);

        // Extract endpoint ID from response for later use
        JSONObject prodEndpointResponse = new JSONObject(addProdEndpointResponse.getData());
        productionEndpointId = prodEndpointResponse.getString("id");
        assertNotNull(productionEndpointId, "Production endpoint ID should not be null");

        // Add sandbox endpoint using the same template
        JSONObject sandboxEndpointConfigObj = new JSONObject(endpointConfig);
        JSONObject sandboxEndpoints = new JSONObject();
        sandboxEndpoints.put("url", ENDPOINT_HOST + ":" + endpointPort + MISTRAL_API_ENDPOINT + "/sandbox");
        sandboxEndpointConfigObj.put("sandbox_endpoints", sandboxEndpoints);

        // Remove production section from endpoint_security for sandbox endpoint
        JSONObject sandboxEndpointSecurity = sandboxEndpointConfigObj.getJSONObject("endpoint_security");
        sandboxEndpointSecurity.remove("production");
        sandboxEndpointConfigObj.put("endpoint_security", sandboxEndpointSecurity);

        Map<String, Object> sandboxEndpointConfigMap = new Gson().fromJson(sandboxEndpointConfigObj.toString(),
                Map.class);
        HttpResponse addSandboxEndpointResponse = restAPIPublisher.addApiEndpoint(mistralApiId, SANDBOX_ENDPOINT_NAME,
                SANDBOX_DEPLOYMENT_STAGE, sandboxEndpointConfigMap);
        assertEquals(addSandboxEndpointResponse.getResponseCode(), HttpStatus.SC_CREATED,
                "Failed to add sandbox endpoint to API: " + mistralApiId);

        // Extract endpoint ID from response for later use
        JSONObject sandboxEndpointResponse = new JSONObject(addSandboxEndpointResponse.getData());
        sandboxEndpointId = sandboxEndpointResponse.getString("id");
        assertNotNull(sandboxEndpointId, "Sandbox endpoint ID should not be null");
    }

    /**
     * Test retrieving all endpoints for an API
     */
    @Test(groups = {"wso2.am"}, description = "Test retrieving all API endpoints",
            dependsOnMethods = "testAddAiApiEndpoint")
    public void testGetApiEndpoints() throws Exception {
        HttpResponse getEndpointsResponse = restAPIPublisher.getApiEndpoints(mistralApiId);
        assertEquals(getEndpointsResponse.getResponseCode(), HttpStatus.SC_OK,
                "Failed to retrieve endpoints for API: " + mistralApiId);
        
        // Parse response to verify endpoints exist
        JSONObject endpointsResponse = new JSONObject(getEndpointsResponse.getData());
        assertTrue(endpointsResponse.has("list"), "Response should contain 'list' field");
        
        // Update the endpoints list with the current API response
        ArrayList<JSONObject> retrievedEndpointsList = new ArrayList<>();
        for (int i = 0; i < endpointsResponse.getJSONArray("list").length(); i++) {
            retrievedEndpointsList.add(endpointsResponse.getJSONArray("list").getJSONObject(i));
        }
        
        // Verify that we have at least 2 endpoints (production and sandbox)
        int endpointCount = retrievedEndpointsList.size();
        assertTrue(endpointCount >= 2, "Should have 2 endpoints, but found: " + endpointCount);
        
        // Verify that our created endpoints are in the list
        boolean foundProductionEndpoint = false;
        boolean foundSandboxEndpoint = false;
        
        for (JSONObject endpoint : retrievedEndpointsList) {
            String endpointId = endpoint.getString("id");
            String endpointName = endpoint.getString("name");
            
            if (productionEndpointId.equals(endpointId) && PRODUCTION_ENDPOINT_NAME.equals(endpointName)) {
                foundProductionEndpoint = true;
            }
            if (sandboxEndpointId.equals(endpointId) && SANDBOX_ENDPOINT_NAME.equals(endpointName)) {
                foundSandboxEndpoint = true;
            }
        }
        
        assertTrue(foundProductionEndpoint, "Production endpoint not found in the list");
        assertTrue(foundSandboxEndpoint, "Sandbox endpoint not found in the list");
    }

    /**
     * Test retrieving a specific endpoint by ID
     */
    @Test(groups = {"wso2.am"}, description = "Test retrieving a specific API endpoint",
            dependsOnMethods = "testAddAiApiEndpoint")
    public void testGetApiEndpoint() throws Exception {
        // Test retrieving production endpoint
        HttpResponse getProdEndpointResponse = restAPIPublisher.getApiEndpoint(mistralApiId, productionEndpointId);
        assertEquals(getProdEndpointResponse.getResponseCode(), HttpStatus.SC_OK,
                "Failed to retrieve production endpoint for API: " + mistralApiId);
        
        // Parse and verify production endpoint details
        JSONObject prodEndpoint = new JSONObject(getProdEndpointResponse.getData());
        assertEquals(prodEndpoint.getString("id"), productionEndpointId,
                "Retrieved endpoint ID does not match expected production endpoint ID");
        assertEquals(prodEndpoint.getString("name"), PRODUCTION_ENDPOINT_NAME,
                "Retrieved endpoint name does not match expected production endpoint name");
        assertEquals(prodEndpoint.getString("deploymentStage"), PRODUCTION_DEPLOYMENT_STAGE,
                "Retrieved endpoint deployment stage does not match expected production stage");
        
        // Test retrieving sandbox endpoint
        HttpResponse getSandboxEndpointResponse = restAPIPublisher.getApiEndpoint(mistralApiId, sandboxEndpointId);
        assertEquals(getSandboxEndpointResponse.getResponseCode(), HttpStatus.SC_OK,
                "Failed to retrieve sandbox endpoint for API: " + mistralApiId);
        
        // Parse and verify sandbox endpoint details
        JSONObject sandboxEndpoint = new JSONObject(getSandboxEndpointResponse.getData());
        assertEquals(sandboxEndpoint.getString("id"), sandboxEndpointId,
                "Retrieved endpoint ID does not match expected sandbox endpoint ID");
        assertEquals(sandboxEndpoint.getString("name"), SANDBOX_ENDPOINT_NAME,
                "Retrieved endpoint name does not match expected sandbox endpoint name");
        assertEquals(sandboxEndpoint.getString("deploymentStage"), SANDBOX_DEPLOYMENT_STAGE,
                "Retrieved endpoint deployment stage does not match expected sandbox stage");
    }

    /**
     * Test updating an existing endpoint
     */
    @Test(groups = {"wso2.am"}, description = "Test updating an API endpoint",
            dependsOnMethods = "testGetApiEndpoint")
    public void testUpdateApiEndpoint() throws Exception {
        // Create updated endpoint configuration for production endpoint
        String baseEndpointConfig = readFile(resourcePath + ENDPOINT_CONFIG_TEMPLATE_FILE);
        JSONObject updatedProdEndpointConfigObj = updateBaseEndpointConfig(baseEndpointConfig);

        // Convert JSONObject to Map to avoid "map" wrapper in serialization
        Map updatedProdEndpointConfigMap = new Gson().fromJson(updatedProdEndpointConfigObj.toString(), Map.class);
        // Update the production endpoint
        HttpResponse updateProdEndpointResponse = restAPIPublisher.updateApiEndpoint(mistralApiId,
                productionEndpointId, PRODUCTION_ENDPOINT_NAME, PRODUCTION_DEPLOYMENT_STAGE, updatedProdEndpointConfigMap);
        assertEquals(updateProdEndpointResponse.getResponseCode(), HttpStatus.SC_OK,
                "Failed to update production endpoint for API: " + mistralApiId);
        
        // Verify the update by retrieving the endpoint
        HttpResponse getUpdatedProdEndpointResponse = restAPIPublisher.getApiEndpoint(mistralApiId, productionEndpointId);
        assertEquals(getUpdatedProdEndpointResponse.getResponseCode(), HttpStatus.SC_OK,
                "Failed to retrieve updated production endpoint");
        
        JSONObject updatedProdEndpoint = new JSONObject(getUpdatedProdEndpointResponse.getData());
        JSONObject endpointConfig = new JSONObject(updatedProdEndpoint.getString("endpointConfig"));
        JSONObject prodEndpoints = endpointConfig.getJSONObject("production_endpoints");
        String updatedUrl = prodEndpoints.getString("url");
        assertTrue(updatedUrl.contains(MISTRAL_API_ENDPOINT_UPDATED),
                "Updated endpoint URL should contain '/mistral-updated' but was: " + updatedUrl);
    }

    @NotNull
    private JSONObject updateBaseEndpointConfig(String updatedProdEndpointConfig) throws JSONException {
        JSONObject updatedProdEndpointConfigObj = new JSONObject(updatedProdEndpointConfig);
        JSONObject updatedProductionEndpoints = new JSONObject();
        // Update the URL to a different endpoint
        updatedProductionEndpoints.put("url", ENDPOINT_HOST + ":" + endpointPort + MISTRAL_API_ENDPOINT_UPDATED);
        updatedProdEndpointConfigObj.put("production_endpoints", updatedProductionEndpoints);

        // Update the endpoint security configuration
        JSONObject endpointSecurity = updatedProdEndpointConfigObj.getJSONObject("endpoint_security");
        JSONObject productionSecurity = endpointSecurity.getJSONObject("production");
        productionSecurity.put("apiKeyValue", "Bearer 456"); // Change the API key value
        endpointSecurity.put("production", productionSecurity);

        // Remove sandbox section from endpoint_security for production endpoint update
        endpointSecurity.remove("sandbox");
        updatedProdEndpointConfigObj.put("endpoint_security", endpointSecurity);
        return updatedProdEndpointConfigObj;
    }

    /**
     * Test deleting an API endpoint
     */
    @Test(groups = {"wso2.am"}, description = "Test deleting an API endpoint",
            dependsOnMethods = "testUpdateApiEndpoint")
    public void testDeleteApiEndpoint() throws Exception {
        // First verify that the sandbox endpoint exists
        HttpResponse getSandboxEndpointResponse = restAPIPublisher.getApiEndpoint(mistralApiId, sandboxEndpointId);
        assertEquals(getSandboxEndpointResponse.getResponseCode(), HttpStatus.SC_OK,
                "Sandbox endpoint should exist before deletion");

        // Delete the sandbox endpoint
        HttpResponse deleteSandboxEndpointResponse = restAPIPublisher.deleteApiEndpoint(mistralApiId,
                sandboxEndpointId);
        assertEquals(deleteSandboxEndpointResponse.getResponseCode(), HttpStatus.SC_OK,
                "Failed to delete sandbox endpoint for API: " + mistralApiId);

        // Verify that the production endpoint still exists
        HttpResponse getProdEndpointResponse = restAPIPublisher.getApiEndpoint(mistralApiId, productionEndpointId);
        assertEquals(getProdEndpointResponse.getResponseCode(), HttpStatus.SC_OK,
                "Production endpoint should still exist after deleting sandbox endpoint");

        // Verify the endpoint count in the list has decreased
        HttpResponse getEndpointsResponse = restAPIPublisher.getApiEndpoints(mistralApiId);
        assertEquals(getEndpointsResponse.getResponseCode(), HttpStatus.SC_OK,
                "Failed to retrieve endpoints list after deletion");

        // Parse the response and verify endpoint count
        JSONObject endpointsResponse = new JSONObject(getEndpointsResponse.getData());
        int endpointCount = endpointsResponse.getJSONArray("list").length();
        assertTrue(endpointCount >= 1, "Should have at least 1 endpoint remaining, but found: " + endpointCount);

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
        assertFalse(foundSandboxEndpoint, "Deleted sandbox endpoint should not be in the endpoints list");
    }

    /**
     * Test final retrieval of all endpoints to verify the complete endpoint lifecycle
     */
    @Test(groups = {"wso2.am"}, description = "Test final retrieval of all API endpoints",
            dependsOnMethods = "testDeleteApiEndpoint")
    public void testGetAllEndpoints() throws Exception {
        // Get all endpoints one final time
        HttpResponse getEndpointsResponse = restAPIPublisher.getApiEndpoints(mistralApiId);
        assertEquals(getEndpointsResponse.getResponseCode(), HttpStatus.SC_OK,
                "Failed to retrieve final endpoints list for API: " + mistralApiId);
        
        // Update the endpoints list with the final API response
        JSONObject endpointsResponse = new JSONObject(getEndpointsResponse.getData());
        for (int i = 0; i < endpointsResponse.getJSONArray("list").length(); i++) {
            endpointsList.add(endpointsResponse.getJSONArray("list").getJSONObject(i));
        }
        
        // Verify that we have at least 1 endpoint remaining (production endpoint)
        assertFalse(endpointsList.isEmpty(),
                "Should have at least 1 endpoint remaining, but found: " + endpointsList.size());
        
        // Verify that the production endpoint still exists
        boolean foundProductionEndpoint = false;
        for (JSONObject endpoint : endpointsList) {
            if (productionEndpointId.equals(endpoint.getString("id"))) {
                foundProductionEndpoint = true;
                break;
            }
        }
        assertTrue(foundProductionEndpoint, "Production endpoint should still exist in the final list");
        
        // Verify that the sandbox endpoint is completely removed
        boolean foundSandboxEndpoint = false;
        for (JSONObject endpoint : endpointsList) {
            if (sandboxEndpointId.equals(endpoint.getString("id"))) {
                foundSandboxEndpoint = true;
                break;
            }
        }
        assertFalse(foundSandboxEndpoint, "Sandbox endpoint should be completely removed from the final list");
    }

    /**
     * Test retrieving AI service provider models from the publisher portal
     */
    @Test(groups = {"wso2.am"}, description = "Test retrieving AI service provider models",
            dependsOnMethods = "testGetAllEndpoints")
    public void testGetServiceProviderModels() {
        // Retrieve model list
        HttpResponse getModelListResponse = restAPIPublisher.getAIServiceProviderModels(aiServiceProviderId);
        assertEquals(getModelListResponse.getResponseCode(), HttpStatus.SC_OK,
                "Failed to retrieve model list of AI Service Provider: " + aiServiceProviderId);

        // Parse the model provider response to extract the models list
        List<Map<String, Object>> modelProviderList = new Gson().fromJson(getModelListResponse.getData(), List.class);
        List<String> modelList = new ArrayList<>();

        if (!modelProviderList.isEmpty()) {
            Map<String, Object> firstProvider = modelProviderList.get(0);
            if (firstProvider.containsKey("models")) {
                List<String> models = (List<String>) firstProvider.get("models");
                modelList.addAll(models);
            }
        }

        // Verify that the retrieved model list contains the expected 3 models
        assertNotNull(modelList, "Model list should not be null");
        assertEquals(modelList.size(), 3, "Expected 3 models but found: " + modelList.size());

        // Verify presence of specific models
        assertTrue(modelList.contains(MODEL_SMALL), "Model list should contain " + MODEL_SMALL);
        assertTrue(modelList.contains(MODEL_MEDIUM), "Model list should contain " + MODEL_MEDIUM);
        assertTrue(modelList.contains(MODEL_LARGE), "Model list should contain " + MODEL_LARGE);
    }

    /**
     * Test Mistral AI API invocation after adding model round-robin policy
     */
    @Test(groups = {"wso2.am"}, description = "Test AI API invocation after adding model round-robin policy",
            dependsOnMethods = "testGetServiceProviderModels")
    public void testApiInvocationWithRoundRobinPolicy() throws Exception {

        HttpResponse getAPIResponse = restAPIPublisher.getAPI(mistralApiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        // Add weighted round-robin policy
        String policyName = "modelWeightedRoundRobin";
        Assert.assertNotNull(policyMap.get(policyName), "Unable to find a common policy with name " + policyName);

        Map<String, Object> attributeMap = new HashMap<>();
        JSONObject weightedRoundRobinConfigs = new JSONObject();
        List<JSONObject> productionModelList = new ArrayList<>();

        // Use specific models for the round-robin configuration
        String firstModel = MODEL_MEDIUM;
        String secondModel = MODEL_LARGE;

        JSONObject model1Obj = new JSONObject();
        model1Obj.put("vendor", "");
        model1Obj.put("model", firstModel);
        model1Obj.put("endpointId", productionEndpointId);
        model1Obj.put("endpointName", PRODUCTION_ENDPOINT_NAME);
        model1Obj.put("weight", 80);

         JSONObject model2Obj = new JSONObject();
         model2Obj.put("vendor", "");
         model2Obj.put("model", secondModel);
         model2Obj.put("endpointId", DEFAULT_PRODUCTION_ENDPOINT_ID);
         model2Obj.put("endpointName", DEFAULT_PRODUCTION_ENDPOINT_NAME);
         model2Obj.put("weight", 20);

        productionModelList.add(model1Obj);
        productionModelList.add(model2Obj);
        weightedRoundRobinConfigs.put("production", productionModelList);
        weightedRoundRobinConfigs.put("sandbox", new ArrayList<>()); // No sandbox models configured
        weightedRoundRobinConfigs.put("suspendDuration", "5");

        String configString = weightedRoundRobinConfigs.toString().replace("\"", "'");
        attributeMap.put("weightedRoundRobinConfigs", configString);

        List<OperationPolicyDTO> requestPolicyList = new ArrayList<>();
        OperationPolicyDTO roundRobinPolicyDTO = new OperationPolicyDTO();
        roundRobinPolicyDTO.setPolicyName(policyName);
        roundRobinPolicyDTO.setPolicyType("common");
        roundRobinPolicyDTO.setPolicyId(policyMap.get(policyName));
        roundRobinPolicyDTO.setParameters(attributeMap);
        requestPolicyList.add(roundRobinPolicyDTO);

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setRequest(requestPolicyList);
        apiOperationPoliciesDTO.setResponse(new ArrayList<>());
        apiOperationPoliciesDTO.setFault(new ArrayList<>());
        apidto.setApiPolicies(apiOperationPoliciesDTO);
        restAPIPublisher.updateAPI(apidto);

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(mistralApiId, restAPIPublisher);
        waitForAPIDeployment();

        // Invoke API
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("ApiKey", apiKey);
        requestHeaders.put("Content-Type", "application/json");
        String invokeURL = getAPIInvocationURLHttp(MISTRAL_API_CONTEXT, API_VERSION_1_0_0) + MISTRAL_API_RESOURCE;
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, mistralPayload);

        assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK, "Failed to invoke Mistral AI API");

        // Verify that the response matches one of the configured models (model2 or model3)
        String actualResponse = serviceResponse.getData();
        boolean isFirstModelResponse = model2Response.equals(actualResponse);
        boolean isSecondModelResponse = model3Response.equals(actualResponse);

        assertTrue(isFirstModelResponse || isSecondModelResponse,
                "Response should match either " + firstModel + " or " + secondModel + " response. " +
                "Actual response: " + actualResponse +
                ", Expected " + firstModel + " response: " + model2Response +
                ", Expected " + secondModel + " response: " + model3Response);
    }

    /**
     * Test creating a new API version with failover policy
     */
    @Test(groups = { "wso2.am" }, description = "Test creating new API version with failover policy",
            dependsOnMethods = "testApiInvocationWithRoundRobinPolicy")
    public void testCreateApiVersionWithFailover() throws Exception {
        // Create new version of the API
        HttpResponse copyApiResponse = restAPIPublisher.copyAPI(API_VERSION_2_0_0, mistralApiId, false);
        assertEquals(copyApiResponse.getResponseCode(), HttpStatus.SC_OK, "Failed to create new version of API");

        newVersionApiId = copyApiResponse.getData();
        assertNotNull(newVersionApiId, "New version API ID should not be null");

        // Get the new version API
        HttpResponse getNewVersionApiResponse = restAPIPublisher.getAPI(newVersionApiId);
        assertEquals(getNewVersionApiResponse.getResponseCode(), HttpStatus.SC_OK,
                "Failed to retrieve new version API");

        APIDTO newVersionApiDto = new Gson().fromJson(getNewVersionApiResponse.getData(), APIDTO.class);
        assertEquals(newVersionApiDto.getVersion(), API_VERSION_2_0_0, "API version should match");

        // Add a new endpoint with failover URL for the new version
        String baseEndpointConfig = readFile(resourcePath + ENDPOINT_CONFIG_TEMPLATE_FILE);
        JSONObject failoverEndpointConfigObj = new JSONObject(baseEndpointConfig);
        JSONObject productionEndpoints = new JSONObject();
        productionEndpoints.put("url", ENDPOINT_HOST + ":" + endpointPort + FAILOVER_ENDPOINT_URL);
        failoverEndpointConfigObj.put("production_endpoints", productionEndpoints);

        // Remove sandbox section from endpoint_security for failover production endpoint
        JSONObject failoverEndpointSecurity = failoverEndpointConfigObj.getJSONObject("endpoint_security");
        failoverEndpointSecurity.remove("sandbox");
        failoverEndpointConfigObj.put("endpoint_security", failoverEndpointSecurity);

        Map<String, Object> failoverEndpointConfigMap = new Gson().fromJson(failoverEndpointConfigObj.toString(),
                Map.class);
        HttpResponse addFailoverEndpointResponse = restAPIPublisher.addApiEndpoint(newVersionApiId,
                FAILOVER_ENDPOINT_NAME, PRODUCTION_DEPLOYMENT_STAGE, failoverEndpointConfigMap);
        assertEquals(addFailoverEndpointResponse.getResponseCode(), HttpStatus.SC_CREATED,
                "Failed to add failover endpoint to new version API");

        // Extract failover endpoint ID from response
        JSONObject failoverEndpointResponse = new JSONObject(addFailoverEndpointResponse.getData());
        String failoverEndpointId = failoverEndpointResponse.getString("id");
        assertNotNull(failoverEndpointId, "Failover endpoint ID should not be null");

        // Update the API to set the new endpoint as primary production endpoint
        newVersionApiDto.setPrimaryProductionEndpointId(failoverEndpointId);

        // Remove the round-robin policy and add failover policy
        String failoverPolicyName = "modelFailover";
        Assert.assertNotNull(policyMap.get(failoverPolicyName),
                "Unable to find a common policy with name " + failoverPolicyName);

        Map<String, Object> failoverAttributeMap = new HashMap<>();
        JSONObject failoverConfigs = new JSONObject();

        // Configure production failover
        JSONObject productionConfig = getProductionFailoverProdConfig(failoverEndpointId);

        // Configure sandbox failover (empty in this case)
        JSONObject sandboxConfig = new JSONObject();
        sandboxConfig.put("targetModel", new JSONObject());
        sandboxConfig.put("fallbackModels", new ArrayList<JSONObject>());

        failoverConfigs.put("production", productionConfig);
        failoverConfigs.put("sandbox", sandboxConfig);
        failoverConfigs.put("requestTimeout", "120");
        failoverConfigs.put("suspendDuration", "0");

        String configString = failoverConfigs.toString().replace("\"", "'");
        failoverAttributeMap.put("failoverConfigs", configString);

        // Create failover policy
        List<OperationPolicyDTO> requestPolicyList = new ArrayList<>();
        OperationPolicyDTO failoverPolicyDTO = new OperationPolicyDTO();
        failoverPolicyDTO.setPolicyName(failoverPolicyName);
        failoverPolicyDTO.setPolicyType("common");
        failoverPolicyDTO.setPolicyId(policyMap.get(failoverPolicyName));
        failoverPolicyDTO.setParameters(failoverAttributeMap);
        requestPolicyList.add(failoverPolicyDTO);

        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setRequest(requestPolicyList);
        newVersionApiDto.setApiPolicies(apiOperationPoliciesDTO);

        // Update the new version API with failover policy
        HttpResponse updateResponse = restAPIPublisher.updateAPIWithHttpInfo(newVersionApiDto);
        assertEquals(updateResponse.getResponseCode(), HttpStatus.SC_OK,
                "Failed to update new version API with failover policy and updated primary production endpoint");

        // Create revision and deploy the new version
        String revisionUUID = createAPIRevisionAndDeployUsingRest(newVersionApiId, restAPIPublisher);
        assertNotNull(revisionUUID, "Failed to create and deploy revision for new version API");

        // Publish the new version API
        HttpResponse lifecycleResponse = restAPIPublisher.changeAPILifeCycleStatusToPublish(newVersionApiId, false);
        assertEquals(lifecycleResponse.getResponseCode(), HttpStatus.SC_OK, "Failed to publish new version API");

        // Wait for API deployment
        waitForAPIDeploymentSync(newVersionApiDto.getProvider(), newVersionApiDto.getName(),
                newVersionApiDto.getVersion(), APIMIntegrationConstants.IS_API_EXISTS);

        // Invoke the new version API to test failover
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("ApiKey", apiKey);
        requestHeaders.put("Content-Type", "application/json");
        String invokeURL = getAPIInvocationURLHttp(MISTRAL_API_CONTEXT, API_VERSION_2_0_0) + MISTRAL_API_RESOURCE;
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, mistralPayload);

        // Verify that the API call succeeded (failover should have worked)
        assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK,
                "API invocation should succeed due to failover");

        // Verify that the response matches the fallback model (model3)
        String actualResponse = serviceResponse.getData();
        String expectedFallbackResponse = model3Response;
        assertEquals(actualResponse, expectedFallbackResponse,
                "Response should match fallback model (model3) after failover");
    }

    /**
     * Helper method to create production failover configuration
     */
    @NotNull
    private JSONObject getProductionFailoverProdConfig(String failoverEndpointId) throws JSONException {
        JSONObject productionConfig = new JSONObject();
        JSONObject targetModel = new JSONObject();
        targetModel.put("model", MODEL_SMALL);
        targetModel.put("endpointId", failoverEndpointId);
        targetModel.put("endpointName", FAILOVER_ENDPOINT_NAME);
        productionConfig.put("targetModel", targetModel);

        List<JSONObject> fallbackModels = new ArrayList<>();
        JSONObject fallbackModel = new JSONObject();
        fallbackModel.put("model", MODEL_LARGE);
        fallbackModel.put("endpointId", DEFAULT_PRODUCTION_ENDPOINT_ID);
        fallbackModel.put("endpointName", DEFAULT_PRODUCTION_ENDPOINT_NAME);
        fallbackModels.add(fallbackModel);
        productionConfig.put("fallbackModels", fallbackModels);
        return productionConfig;
    }

    /**
     * Deletes a specified AI service provider after removing API subscriptions and applications. Verifies that the
     * provider is successfully deleted and no longer listed.
     */
    @Test(groups = {
            "wso2.am" }, description = "Delete AI Service Provider", dependsOnMethods = "testCreateApiVersionWithFailover")
    public void deleteAIServiceProvider() throws Exception {

        // Clean up APIs and subscriptions before deleting the AI service provider
        restAPIStore.removeAPISubscriptionByName(UNSECURED_API_NAME, API_VERSION_1_0_0, API_PROVIDER, APPLICATION_NAME);
        restAPIStore.removeAPISubscriptionByName(MISTRAL_API_NAME, API_VERSION_1_0_0, API_PROVIDER, APPLICATION_NAME);
        restAPIStore.removeAPISubscriptionByName(MISTRAL_API_NAME, API_VERSION_2_0_0, API_PROVIDER, APPLICATION_NAME);
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(unsecuredApiId);
        restAPIPublisher.deleteAPI(mistralApiId);
        restAPIPublisher.deleteAPI(newVersionApiId);

        ApiResponse<Void> deleteProviderResponse = restAPIAdmin.deleteAIServiceProvider(aiServiceProviderId);
        assertEquals(deleteProviderResponse.getStatusCode(), Response.Status.OK.getStatusCode(),
                "Failed to delete AI service provider");

        // Verify the AI service provider is no longer listed
        ApiResponse<AIServiceProviderSummaryResponseListDTO> aiServiceProviders = restAPIAdmin.getAIServiceProviders();
        assertEquals(aiServiceProviders.getStatusCode(), Response.Status.OK.getStatusCode(),
                "Failed to retrieve AI service providers after deletion");
        assertNotNull(aiServiceProviders.getData().getList(), "AI service providers list should not be null");

        // Ensure the deleted provider is not in the list
        for (AIServiceProviderSummaryResponseDTO provider : aiServiceProviders.getData().getList()) {
            if (provider.getName().equals(AI_SERVICE_PROVIDER_NAME)) {
                Assert.fail("AI Service Provider " + AI_SERVICE_PROVIDER_NAME + " has not been deleted correctly");
            }
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
        super.cleanUp();
    }

    /**
     * Initialize default AI service providers list
     */
    private void initializeDefaultAiServiceProviders() {
        defaultAiServiceProviders.add("MistralAI");
        defaultAiServiceProviders.add("OpenAI");
        defaultAiServiceProviders.add("AzureOpenAI");
        defaultAiServiceProviders.add("AWSBedrock");
        defaultAiServiceProviders.add("Anthropic");
        defaultAiServiceProviders.add("Gemini");
        defaultAiServiceProviders.add("AzureAIFoundry");
    }

    /**
     * Initialize model configurations
     */
    private void initializeModelConfigurations() {
        String modelList = String.format("[\"%s\", \"%s\", \"%s\"]", MODEL_SMALL, MODEL_MEDIUM, MODEL_LARGE);
        modelProviders = String.format("[{\"models\": %s, \"name\": \"%s\"}]", modelList, AI_SERVICE_PROVIDER_NAME);
    }

    /**
     * Create test application for API subscriptions
     *
     * @return application ID
     * @throws Exception if application creation fails
     */
    private String createTestApplication() throws Exception {
        ApplicationDTO applicationDTO = restAPIStore.addApplication(APPLICATION_NAME,
                APIThrottlingTier.UNLIMITED.getState(), "", "this-is-test");
        String appId = applicationDTO.getApplicationId();
        Assert.assertNotNull(appId, "Application ID should not be null");
        return appId;
    }

    /**
     * Initialize test data including response templates and payloads
     *
     * @throws Exception if file reading fails
     */
    private void initializeTestData() throws Exception {
        resourcePath = TestConfigurationProvider.getResourceLocation() + "ai-api" + File.separator;
        String mistralResponse = readFile(resourcePath + MISTRAL_RESPONSE_FILE_NAME);
        model1Response = mistralResponse.replace("{{jsonPath request.body '$.model'}}", MODEL_SMALL);
        model2Response = mistralResponse.replace("{{jsonPath request.body '$.model'}}", MODEL_MEDIUM);
        model3Response = mistralResponse.replace("{{jsonPath request.body '$.model'}}", MODEL_LARGE);
        mistralPayload = readFile(resourcePath + MISTRAL_PAYLOAD_FILE_NAME);

        // Load AI service provider configurations from JSON files
        customAiServiceProviderConfigurations = readFile(resourcePath + AI_SERVICE_PROVIDER_CONFIG_NO_AUTH_FILE);
        updatedCustomAiServiceProviderConfigurations = readFile(
                resourcePath + AI_SERVICE_PROVIDER_CONFIG_WITH_AUTH_FILE);
    }

    /**
     * Create and publish an AI API with given configurations
     *
     * @param endpoint                endpoint URL
     * @param additionalPropsFileName additional properties file name
     * @return API ID of the created API
     * @throws Exception if API creation fails
     */
    private String createAndPublishAiApi(String endpoint, String additionalPropsFileName) throws Exception {
        String apiDefinition = readFile(resourcePath + API_DEFINITION_FILE_NAME);
        String additionalProperties = readFile(resourcePath + additionalPropsFileName);
        JSONObject additionalPropertiesObj = new JSONObject(additionalProperties);

        // Update endpoint configuration
        JSONObject endpointConfig = additionalPropertiesObj.getJSONObject("endpointConfig");
        JSONObject productionEndpoints = new JSONObject();
        productionEndpoints.put("url", ENDPOINT_HOST + ":" + endpointPort + endpoint);
        endpointConfig.put("production_endpoints", productionEndpoints);
        additionalPropertiesObj.put("endpointConfig", endpointConfig);

        // Create API
        File file = getTempFileWithContent(apiDefinition);
        APIDTO apidto = restAPIPublisher.importOASDefinition(file, additionalPropertiesObj.toString());
        String apiId = apidto.getId();

        // Verify API creation
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(apiId);
        assertEquals(createdApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Failed to create AI API using the custom AI Service Provider");

        // Deploy and publish API
        String revisionUUID = createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        Assert.assertNotNull(revisionUUID, "Revision UUID should not be null");

        HttpResponse lifecycleResponse = restAPIPublisher.changeAPILifeCycleStatusToPublish(apiId, false);
        assertEquals(lifecycleResponse.getResponseCode(), HttpStatus.SC_OK, "Failed to publish API");

        waitForAPIDeploymentSync(apidto.getProvider(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        return apiId;
    }

    /**
     * Subscribe to API and generate API key
     *
     * @param apiId API ID to subscribe to
     * @return API key
     * @throws Exception if subscription or key generation fails
     */
    private String subscribeToApiAndGenerateKey(String apiId) throws Exception {
        // Subscribe to API
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);
        assertNotNull(subscriptionDTO, "API subscription should not be null");

        // Generate API Key
        APIKeyDTO apiKeyDTO = restAPIStore.generateAPIKeys(applicationId,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.toString(), -1, null, null);
        assertNotNull(apiKeyDTO, "API Key should not be null");
        return apiKeyDTO.getApikey();
    }

    /**
     * Create a temporary file with the given content
     *
     * @param content Content to write to the file
     * @return Temporary file
     * @throws IOException if file creation fails
     */
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
        wireMockServer.stubFor(WireMock.post(urlEqualTo(MISTRAL_API_ENDPOINT + MISTRAL_API_RESOURCE))
                .withHeader("Authorization", WireMock.matching("Bearer 123"))
                .withRequestBody(matchingJsonPath("$.model", equalTo(MODEL_SMALL)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(model1Response)));

        // Stub for round-robin policy: model1 request routed to model2 (80% weight)
        // Using more flexible matching with containsString to handle potential JSON formatting issues
        wireMockServer.stubFor(WireMock.post(urlEqualTo(MISTRAL_API_ENDPOINT_UPDATED + MISTRAL_API_RESOURCE))
                .withHeader("Authorization", WireMock.matching("Bearer 456"))
                .withRequestBody(matchingJsonPath("$.model", equalTo(MODEL_MEDIUM)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(model2Response)));

        // Stub for round-robin policy: model1 request routed to model3 (20% weight)  
        wireMockServer.stubFor(WireMock.post(urlEqualTo(MISTRAL_API_ENDPOINT_UPDATED + MISTRAL_API_RESOURCE))
                .withHeader("Authorization", WireMock.matching("Bearer 456"))
                .withRequestBody(matchingJsonPath("$.model", equalTo(MODEL_LARGE)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(model3Response)));

        // Stub for /mistral endpoint with Bearer 123 and model3
        // Using flexible matching to handle JSON formatting issues
        wireMockServer.stubFor(WireMock.post(urlEqualTo(MISTRAL_API_ENDPOINT + MISTRAL_API_RESOURCE))
                .withHeader("Authorization", WireMock.matching("Bearer 123"))
                .withRequestBody(matchingJsonPath("$.model", equalTo(MODEL_LARGE)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(model3Response)));

        // Stub for failover endpoint that returns 500 (to trigger failover)
        wireMockServer.stubFor(WireMock.post(urlEqualTo(FAILOVER_ENDPOINT_URL + MISTRAL_API_RESOURCE))
                .withHeader("Authorization", WireMock.matching("Bearer 123"))
                .withRequestBody(matchingJsonPath("$.model", equalTo(MODEL_SMALL)))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Rate limit exceeded\"}")));

        // Stub for unsecured AI API (no Authorization header)
        wireMockServer.stubFor(WireMock.post(urlEqualTo(NO_AUTH_API_ENDPOINT + MISTRAL_API_RESOURCE))
                .withRequestBody(matchingJsonPath("$.model", equalTo(MODEL_SMALL)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(model1Response)));

        wireMockServer.start();
        log.info("Wiremock server started on port " + endpointPort);
    }

    /**
     * Find a free port to start WireMock server in the specified port range
     * @return Available port number
     */
    private int getAvailablePort() {
        final int lowerPortLimit = 9950;
        final int upperPortLimit = 9999;

        for (int currentPort = lowerPortLimit; currentPort < upperPortLimit; currentPort++) {
            if (isPortFree(currentPort)) {
                return currentPort;
            }
        }
        return -1;
    }

    /**
     * Check whether given port is available
     * @param port Port number to check
     * @return true if port is available, false otherwise
     */
    private boolean isPortFree(int port) {
        try (Socket socket = new Socket("localhost", port)) {
            // Something is using the port and has responded
            return false;
        } catch (IOException e) {
            // Port is available
            return true;
        }
    }
}
