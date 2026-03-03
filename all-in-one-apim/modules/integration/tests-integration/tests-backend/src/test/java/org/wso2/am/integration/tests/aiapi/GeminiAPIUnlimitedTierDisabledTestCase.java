/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.MockServerUtils;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Gemini AI API Test Case with Unlimited Tier Disabled
 * Tests Gemini 1.1.0 API to ensure it works correctly when enable_unlimited_tier = false
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class GeminiAPIUnlimitedTierDisabledTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(GeminiAPIUnlimitedTierDisabledTestCase.class);

    // API Configuration Constants
    private static final String API_VERSION_1_1_0 = "1.1.0";
    private static final String GEMINI_API_CONTEXT = "geminiapi";
    private static final String GEMINI_API_ENDPOINT = "/gemini";
    private static final String GEMINI_API_RESOURCE = "/v1beta/models/gemini-1.5-flash:generateContent";

    // API Instance Variables
    private String geminiApiId;
    private String geminiPayload;
    private String geminiResponse;

    // Application Configuration Constants
    private static final String APPLICATION_NAME = "Gemini-API-Application";

    // Application Instance Variables
    private String applicationId;
    private String apiKey;

    // WireMock Configuration
    private static final String ENDPOINT_HOST = "http://localhost";

    // WireMock Instance Variables
    private String resourcePath;
    private WireMockServer wireMockServer;
    private int endpointPort;

    // Server Configuration Manager
    private ServerConfigurationManager serverConfigurationManager;

    // File Configuration Constants
    private static final String GEMINI_RESPONSE_FILE_NAME = "gemini-response.json";
    private static final String GEMINI_PAYLOAD_FILE_NAME = "gemini-payload.json";
    private static final String GEMINI_ADD_PROPS_FILE_NAME = "gemini-add-props.json";

    @Factory(dataProvider = "userModeDataProvider")
    public GeminiAPIUnlimitedTierDisabledTestCase(TestUserMode userMode) {
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
        AutomationContext superTenantKeyManagerContext =
                new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                        APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE, TestUserMode.SUPER_TENANT_ADMIN);
        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);
        serverConfigurationManager.applyConfiguration(new File(
                getAMResourceLocation() + File.separator + "configFiles" + File.separator + "aiapis"
                        + File.separator + "gemini110" + File.separator + "deployment.toml"));

        super.init(userMode);
        userManagementClient = new UserManagementClient(
                keyManagerContext.getContextUrls().getBackEndUrl(),
                keyManagerContext.getContextTenant().getTenantAdmin().getUserName(),
                keyManagerContext.getContextTenant().getTenantAdmin().getPassword());

        applicationId = createTestApplication();
        initializeTestData();
        startWiremockServer();
    }

    /**
     * Test Gemini AI API creation, deployment, and publishing with unlimited tier disabled.
     * This test verifies that Gemini 1.1.0 API works correctly when enable_unlimited_tier = false
     * in deployment.toml configuration.
     */
    @Test(groups = {"wso2.am"}, description = "Test Gemini AI API creation with unlimited tier disabled")
    public void testGeminiAiApiCreationAndPublish() throws Exception {
        geminiApiId = createAndPublishAiApi(GEMINI_API_ENDPOINT, GEMINI_ADD_PROPS_FILE_NAME);
    }

    /**
     * Test Gemini AI API invocation with unlimited tier disabled.
     * Verifies that the API can be invoked successfully and returns the expected response.
     */
    @Test(groups = {"wso2.am"}, description = "Test Gemini AI API invocation with unlimited tier disabled",
            dependsOnMethods = "testGeminiAiApiCreationAndPublish")
    public void testGeminiAiApiInvocation() throws Exception {
        apiKey = subscribeToApiAndGenerateKey(geminiApiId);

        // Invoke API
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("ApiKey", apiKey);
        requestHeaders.put("Content-Type", APIMIntegrationConstants.APPLICATION_JSON_MEDIA_TYPE);
        String invokeURL = getAPIInvocationURLHttp(GEMINI_API_CONTEXT, API_VERSION_1_1_0) + GEMINI_API_RESOURCE;
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, geminiPayload);

         assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK, "Gemini AI API invocation failed");
        assertEquals(serviceResponse.getData(), geminiResponse, "Gemini AI API response mismatch");
    }

    /**
     * Test that the Gemini API operations have valid throttling tiers (not Unlimited)
     * when unlimited tier is disabled in configuration.
     */
    @Test(groups = {"wso2.am"}, description = "Test Gemini API throttling tier with unlimited tier disabled",
            dependsOnMethods = "testGeminiAiApiCreationAndPublish")
    public void testGeminiApiThrottlingTier() throws Exception {
        HttpResponse getAPIResponse = restAPIPublisher.getAPI(geminiApiId);
        assertEquals(getAPIResponse.getResponseCode(), HttpStatus.SC_OK, "Failed to retrieve Gemini API");

        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);
        assertNotNull(apidto.getOperations(), "API operations should not be null");

        // Verify that operations do not have Unlimited throttling tier
        for (APIOperationsDTO operation : apidto.getOperations()) {
            assertNotEquals(
                    operation.getThrottlingPolicy(),
                    APIMIntegrationConstants.API_TIER.UNLIMITED,
                    "Operation should not have Unlimited throttling tier when unlimited tier is disabled"
            );
            log.info("Operation: " + operation.getTarget()
                    + " has throttling policy: " + operation.getThrottlingPolicy());
        }
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        // Remove all subscriptions first
        if (applicationId != null) {
            try {
                SubscriptionListDTO subsDTO = restAPIStore.getAllSubscriptionsOfApplication(applicationId);
                for (SubscriptionDTO subscriptionDTO : subsDTO.getList()) {
                    restAPIStore.removeSubscription(subscriptionDTO);
                }
                restAPIStore.deleteApplication(applicationId);
            } catch (Exception e) {
                log.warn("Failed to delete application: " + e.getMessage());
            }
        }

        if (geminiApiId != null) {
            try {
                undeployAndDeleteAPIRevisionsUsingRest(geminiApiId, restAPIPublisher);
                restAPIPublisher.deleteAPI(geminiApiId);
            } catch (Exception e) {
                log.warn("Failed to delete API: " + e.getMessage());
            }
        }

        // Stop WireMock server
        if (wireMockServer != null) {
            try {
                wireMockServer.stop();
            } catch (Exception e) {
                log.warn("Error stopping WireMock server: " + e.getMessage());
            }
        }

        // Restore server configuration for every factory instance, since setEnvironment()
        // applies the deployment.toml for all instances regardless of user mode.
        if (serverConfigurationManager != null) {
            serverConfigurationManager.restoreToLastConfiguration();
        }
    }

    /**
     * Create test application for API subscriptions
     *
     * @return application ID
     * @throws Exception if application creation fails
     */
    private String createTestApplication() throws Exception {
        ApplicationDTO applicationDTO = restAPIStore.addApplication(APPLICATION_NAME,
                APIMIntegrationConstants.APPLICATION_TIER.TEN_PER_MIN, "", "this-is-test");
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
        geminiResponse = readFile(resourcePath + GEMINI_RESPONSE_FILE_NAME);
        geminiPayload = readFile(resourcePath + GEMINI_PAYLOAD_FILE_NAME);
    }

    /**
     * Create and publish an AI API with given configurations.
     * Fetches the OpenAPI definition dynamically from the AI Service Provider.
     *
     * @param endpoint                endpoint URL
     * @param additionalPropsFileName additional properties file name
     * @return API ID of the created API
     * @throws Exception if API creation fails
     */
    private String createAndPublishAiApi(String endpoint, String additionalPropsFileName) throws Exception {
        String additionalProperties = readFile(resourcePath + additionalPropsFileName);
        JSONObject additionalPropertiesObj = new JSONObject(additionalProperties);

        // Update endpoint configuration
        JSONObject endpointConfig = additionalPropertiesObj.getJSONObject(
                APIMIntegrationConstants.ENDPOINT_CONFIG);
        JSONObject productionEndpoints = new JSONObject();
        productionEndpoints.put(APIMIntegrationConstants.URL, ENDPOINT_HOST + ":" + endpointPort + endpoint);
        endpointConfig.put(APIMIntegrationConstants.PRODUCTION_ENDPOINTS, productionEndpoints);
        additionalPropertiesObj.put(APIMIntegrationConstants.ENDPOINT_CONFIG, endpointConfig);

        // Get AI Service Providers and find Gemini 1.1.0
        HttpResponse aiProvidersResponse = restAPIPublisher.getAIServiceProviders();
        assertEquals(aiProvidersResponse.getResponseCode(), HttpStatus.SC_OK,
                "Failed to retrieve AI service providers");

        Gson gson = new Gson();
        com.google.gson.JsonObject providersJson = gson.fromJson(aiProvidersResponse.getData(),
                com.google.gson.JsonObject.class);
        com.google.gson.JsonArray providerList = providersJson.getAsJsonArray("list");

        String geminiProviderId = null;
        for (int i = 0; i < providerList.size(); i++) {
            com.google.gson.JsonObject provider = providerList.get(i).getAsJsonObject();
            String name = provider.get(APIMIntegrationConstants.API_NAME).getAsString();
            String apiVersion = provider.get("apiVersion").getAsString();
            if ("Gemini".equals(name) && API_VERSION_1_1_0.equals(apiVersion)) {
                Assert.assertFalse(provider.get("deprecated").getAsBoolean(),
                        "Gemini AI Service Provider (version 1.1.0) should not be deprecated");
                geminiProviderId = provider.get(APIMIntegrationConstants.API_RESPONSE_ELEMENT_NAME_ID).getAsString();
                log.info("Found Gemini AI Service Provider with ID: " + geminiProviderId);
                break;
            }
        }

        Assert.assertNotNull(geminiProviderId,
                "Gemini AI Service Provider (version 1.1.0) not found in available providers");

        // Fetch the OpenAPI definition from the AI Service Provider
        HttpResponse apiDefinitionResponse = restAPIPublisher.getAIServiceProviderApiDefinition(geminiProviderId);
        assertEquals(apiDefinitionResponse.getResponseCode(), HttpStatus.SC_OK,
                "Failed to retrieve API definition from AI service provider");
        String apiDefinition = apiDefinitionResponse.getData();
        log.info("Successfully retrieved OpenAPI definition from AI Service Provider");

        // Update subtypeConfiguration.configuration with llmProviderId
        JSONObject subtypeConfiguration;
        if (additionalPropertiesObj.has("subtypeConfiguration")) {
            subtypeConfiguration = additionalPropertiesObj.getJSONObject("subtypeConfiguration");
        } else {
            subtypeConfiguration = new JSONObject();
            subtypeConfiguration.put("subtype", "AIAPI");
            additionalPropertiesObj.put("subtypeConfiguration", subtypeConfiguration);
        }

        JSONObject configuration;
        if (subtypeConfiguration.has("configuration")) {
            configuration = subtypeConfiguration.getJSONObject("configuration");
        } else {
            configuration = new JSONObject();
            subtypeConfiguration.put("configuration", configuration);
        }
        configuration.put("llmProviderId", geminiProviderId);

        // Create API using inline definition instead of file
        APIDTO apidto = restAPIPublisher.importOASDefinitionWithInlineContent(apiDefinition,
                additionalPropertiesObj.toString());
        String apiId = apidto.getId();

        // Verify API creation
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(apiId);
        assertEquals(createdApiResponse.getResponseCode(), HttpStatus.SC_OK,
                "Failed to create Gemini AI API");

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
        // Subscribe to API - use a specific tier instead of Unlimited since unlimited tier is disabled
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, applicationId, "AIBronze");
        assertNotNull(subscriptionDTO, "API subscription should not be null");

        // Generate API Key
        APIKeyDTO apiKeyDTO = restAPIStore.generateAPIKeys(applicationId,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.toString(), -1, null, null, null);
        assertNotNull(apiKeyDTO, "API Key should not be null");
        return apiKeyDTO.getApikey();
    }

    private void startWiremockServer() {
        endpointPort = MockServerUtils.getAvailablePort(MockServerUtils.LOCALHOST, true);
        assertNotEquals(endpointPort, -1,
                "No available port in the range " + MockServerUtils.httpsPortLowerRange + "-" +
                        MockServerUtils.httpsPortUpperRange + " was found");
        wireMockServer = new WireMockServer(options()
                .port(endpointPort)
                .extensions(new ResponseTemplateTransformer(true)));

        // Stub for Gemini AI API - matches the generateContent endpoint
        wireMockServer.stubFor(WireMock.post(urlEqualTo(GEMINI_API_ENDPOINT + GEMINI_API_RESOURCE))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", APIMIntegrationConstants.APPLICATION_JSON_MEDIA_TYPE)
                        .withHeader("Connection", "close")
                        .withHeader("Content-Length", String.valueOf(geminiResponse.length()))
                        .withBody(geminiResponse)));

        try {
            wireMockServer.start();
            log.info("WireMock server started successfully on port " + endpointPort);
        } catch (Exception e) {
            log.error("Failed to start WireMock server on port " + endpointPort + ": " + e.getMessage(), e);
            throw e;
        }
    }
}
