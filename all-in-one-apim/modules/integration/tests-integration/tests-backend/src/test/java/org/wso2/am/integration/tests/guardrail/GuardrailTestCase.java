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

package org.wso2.am.integration.tests.guardrail;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.apache.commons.httpclient.HttpStatus;
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
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.AIServiceProviderResponseDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIThrottlingTier;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.jwt.JWTGenerator;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.nio.file.Files;
import java.nio.file.Paths;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.ws.rs.core.Response;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Verifies that an AI API created in the middleware (APIM Gateway) correctly
 * forwards the real AI request payload to the mock AI backend.
 *
 * <p>End-to-end flow:
 * <pre>
 *   Test client  ──►  APIM Gateway URL  ──►  WireMock (mock AI backend)
 * </pre>
 *
 * <p>What the test checks:
 * <ol>
 *   <li>The middleware returns HTTP 200.</li>
 *   <li>The response body matches exactly what the mock backend is configured to return.</li>
 *   <li>WireMock received exactly one POST to the AI chat-completions resource,
 *       with the correct {@code model} field and {@code messages} array — proving
 *       the gateway forwarded the original AI request unchanged.</li>
 * </ol>
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class GuardrailTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(GuardrailTestCase.class);

    // -----------------------------------------------------------------------
    // API constants
    // -----------------------------------------------------------------------
    private static final String API_NAME     = "MockAIBackendAPI";
    private static final String API_CONTEXT  = "mockAIBackend";
    private static final String API_VERSION  = "1.0.0";
    /** Base path served by the WireMock backend for all AI routes. */
    private static final String BACKEND_PATH  = "/mock-ai";
    /** Chat-completions resource path (matches the OAS definition). */
    private static final String CHAT_RESOURCE = "/v1/chat/completions";
    private static final String EMBEDDINGS_RESOURCE = "/v1/embeddings";
    private static final String ENDPOINT_HOST = "http://localhost";
    private static final int MOCK_BACKEND_PORT = 18080;
    private static final String EMBEDDING_MODEL_NAME = "mistral-embed";
    private static final String REQUESTED_TOOL_TEXT = "Get current weather and 7-day forecast for a location.";
    private static final String REQUESTED_QUERY_TEXT_SNIPPET = "corporate retreat in Denver";

    // -----------------------------------------------------------------------
    // AI service provider constants
    // -----------------------------------------------------------------------
    private static final String PROVIDER_NAME         = "MockAIProvider";
    private static final String PROVIDER_API_VERSION  = "1.0.0";
    private static final String PROVIDER_DESCRIPTION  = "Mock AI provider for backend request validation tests";
    private static final String MODEL_NAME            = "mistral-small-latest";

    // -----------------------------------------------------------------------
    // Application constants
    // -----------------------------------------------------------------------
    private static final String APP_NAME = "MockAI-Test-Application";

    // -----------------------------------------------------------------------
    // Resource file names (reuse existing ai-api test resources)
    // -----------------------------------------------------------------------
    private static final String API_DEFINITION_FILE  = "mistral-def.json";
    private static final String PAYLOAD_FILE         = "mistral-payload.json";
    private static final String RESPONSE_FILE        = "mistral-response.json";
    private static final String EMBEDDINGS_RESPONSE_FILE = "mockembeddings.json";
    private static final String PROVIDER_CONFIG_FILE = "ai-service-provider-config-no-auth.json";

    // -----------------------------------------------------------------------
    // Instance variables
    // -----------------------------------------------------------------------
    private String aiApiId;
    private String applicationId;
    private String apiKey;
    private String aiServiceProviderId;
    private String resourcePath;
    /** Pre-rendered mock response (model placeholder already substituted). */
    private String mockResponse;
    private String mockEmbeddingsResponse;
    /** JSON payload that the test client sends to the gateway. */
    private String requestPayload;
    private WireMockServer wireMockServer;
    private int mockPort;
	private ServerConfigurationManager serverConfigurationManager;
    // -----------------------------------------------------------------------
    // TestNG factory / data-provider
    // -----------------------------------------------------------------------

    @Factory(dataProvider = "userModeDataProvider")
    public GuardrailTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                {TestUserMode.SUPER_TENANT_ADMIN},
        };
    }

    // -----------------------------------------------------------------------
    // Setup / Teardown
    // -----------------------------------------------------------------------

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        String sourceTomlPath = resolveGuardrailDeploymentTomlPath();
        log.info("###===### Applying source deployment.toml: " + sourceTomlPath);

        superTenantKeyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE,
                TestUserMode.SUPER_TENANT_ADMIN);
        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);
        serverConfigurationManager.applyConfigurationWithoutRestart(new File(sourceTomlPath));
        serverConfigurationManager.restartGracefully();

        resourcePath = TestConfigurationProvider.getResourceLocation() + "guardrail" + File.separator;

        // Load request payload and pre-render the expected response
        requestPayload = readFile(resourcePath + PAYLOAD_FILE);
        String responseTemplate = readFile(resourcePath + RESPONSE_FILE);
        // The response template uses a Handlebars expression; substitute so the
        // expected string can be compared directly to the actual HTTP body.
        mockResponse = responseTemplate.replace("{{jsonPath request.body '$.model'}}", MODEL_NAME);
        mockEmbeddingsResponse = readFile(resourcePath + EMBEDDINGS_RESPONSE_FILE);

        // 1. Start the mock AI backend (WireMock)
        startMockBackend();

        // 2. Register a custom AI service provider pointing to the mock backend
        registerAiServiceProvider();

        // 3. Create, deploy, and publish the AI API in the middleware
        createAndPublishAiApi();

        // 4. Create a consumer application, subscribe, and obtain an API key
        createAppAndSubscribe();
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() {
        try {
            if (applicationId != null) {
                restAPIStore.deleteApplication(applicationId);
            }
        } catch (Exception e) {
            log.warn("###===### Could not delete application: " + e.getMessage());
        }
        try {
            if (aiApiId != null) {
                undeployAndDeleteAPIRevisionsUsingRest(aiApiId, restAPIPublisher);
                restAPIPublisher.deleteAPI(aiApiId);
            }
        } catch (Exception e) {
            log.warn("###===### Could not delete AI API: " + e.getMessage());
        }
        try {
            if (aiServiceProviderId != null) {
                restAPIAdmin.deleteAIServiceProvider(aiServiceProviderId);
            }
        } catch (Exception e) {
            log.warn("###===### Could not delete AI service provider: " + e.getMessage());
        }
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    // -----------------------------------------------------------------------
    // Test
    // -----------------------------------------------------------------------

    /**
     * Calls the middleware gateway URL and verifies that the mock AI backend
     * receives the real AI request payload.
     *
     * <p>Steps:
     * <ol>
     *   <li>POST to the APIM gateway URL (the URL given by the middleware).</li>
     *   <li>Assert HTTP 200 response from the gateway.</li>
     *   <li>Assert the response body matches the mock backend's reply.</li>
     *   <li>Use {@link WireMockServer#verify} to confirm the mock backend received
     *       exactly one request containing the expected AI JSON fields.</li>
     * </ol>
     */
    @Test(groups = {"wso2.am"},
            enabled = true,
            description = "Verify the middleware gateway URL forwards the AI request to the mock AI backend")
    public void testAIRequestForwardedToMockBackend() throws Exception {

        // Build request headers
        Map<String, String> headers = new HashMap<>();
        headers.put("ApiKey", apiKey);
        headers.put("Content-Type", "application/json");

        // Middleware URL: resolved by the test framework (handles tenant prefix automatically)
        String gatewayUrl = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION) + CHAT_RESOURCE;
        log.info("###===### Invoking middleware gateway URL: " + gatewayUrl);

        // --- Invoke ---
        HttpResponse response = HTTPSClientUtils.doPost(gatewayUrl, headers, requestPayload);

        // --- Assert gateway response ---
        assertEquals(response.getResponseCode(), HttpStatus.SC_OK,
                "Expected HTTP 200 from the middleware gateway but received: " + response.getResponseCode());
        assertEquals(response.getData(), mockResponse,
                "Gateway response body does not match the mock backend's configured reply");

        // --- Verify mock backend received the real AI request ---
        // WireMock must have seen exactly one POST to the chat-completions path and
        // the forwarded body must preserve the original AI fields unchanged.
        wireMockServer.verify(1, postRequestedFor(urlEqualTo(BACKEND_PATH + CHAT_RESOURCE))
            .withRequestBody(matchingJsonPath("$.contents[0].parts[0].text"))
            .withRequestBody(containing(REQUESTED_QUERY_TEXT_SNIPPET))
            .withRequestBody(containing(REQUESTED_TOOL_TEXT)));

        List<LoggedRequest> chatRequests = wireMockServer.findAll(
            postRequestedFor(urlEqualTo(BACKEND_PATH + CHAT_RESOURCE)));
        if (!chatRequests.isEmpty()) {
            log.info("###===### Received payload at mock AI backend (chat): " +
                chatRequests.get(0).getBodyAsString());
        }

        log.info("###===### Verification passed: mock AI backend received the real AI request forwarded by the middleware");
    }

    /**
     * Verifies that the deployment.toml applied by {@link #setEnvironment()} is
     * present in the running server's conf directory.
     * The server is unpacked under a dynamically-named carbontmp directory;
     * {@link FrameworkPathUtil#getCarbonHome()} resolves it at runtime.
     */
    @Test(groups = {"wso2.am"}, description = "Verify deployed deployment.toml matches the applied source file")
    public void testEnvironmentConfigurationOnly() throws Exception {
        assertNotNull(serverConfigurationManager,
                "ServerConfigurationManager should be initialized after environment configuration");

        // Source file to compare against: src/test/resources/guardrail/deployment.toml
        String sourceTomlPath = resolveGuardrailDeploymentTomlPath();

        // Deployed file: resolved dynamically via carbon.home (handles any carbontmp<N> directory)
        String deployedTomlPath = FrameworkPathUtil.getCarbonHome()
                + File.separator + "repository"
                + File.separator + "conf"
                + File.separator + "deployment.toml";

        log.info("###===### Source deployment.toml  : " + sourceTomlPath);
        log.info("###===### Deployed deployment.toml: " + deployedTomlPath);

        assertTrue(Files.exists(Paths.get(deployedTomlPath)),
                "Deployed deployment.toml not found at: " + deployedTomlPath);
        assertTrue(Files.exists(Paths.get(sourceTomlPath)),
                "Source deployment.toml not found at: " + sourceTomlPath);

        String sourceContent   = new String(Files.readAllBytes(Paths.get(sourceTomlPath)));
        String deployedContent = new String(Files.readAllBytes(Paths.get(deployedTomlPath)));

        assertEquals(deployedContent, sourceContent,
                "Deployed deployment.toml does not match the applied source file.\n"
                        + "Source : " + sourceTomlPath + "\n"
                        + "Deployed: " + deployedTomlPath);

        log.info("###===### deployment.toml content matches — environment configuration verified");
    }

    private String resolveGuardrailDeploymentTomlPath() throws IOException {
        String amResourceCandidate = getAMResourceLocation() + File.separator + "guardrail"
                + File.separator + "deployment.toml";
        String basedir = System.getProperty("basedir");
        String basedirCandidate = basedir == null ? null
                : basedir + File.separator + "src" + File.separator + "test" + File.separator
                + "resources" + File.separator + "guardrail" + File.separator + "deployment.toml";
        String relativeCandidate = "src" + File.separator + "test" + File.separator + "resources"
                + File.separator + "guardrail" + File.separator + "deployment.toml";

        String[] candidates = basedirCandidate == null
                ? new String[]{amResourceCandidate, relativeCandidate}
                : new String[]{amResourceCandidate, basedirCandidate, relativeCandidate};

        StringBuilder attempted = new StringBuilder();
        for (String candidate : candidates) {
            File candidateFile = new File(candidate);
            String absolutePath = candidateFile.getAbsolutePath();
            if (attempted.length() > 0) {
                attempted.append(", ");
            }
            attempted.append(absolutePath);
            if (candidateFile.exists() && candidateFile.isFile()) {
                return candidateFile.getCanonicalPath();
            }
        }

        throw new IOException("Guardrail deployment.toml not found. Tried: " + attempted);
    }

    @Test(groups = {"wso2.am"}, enabled = true, description = "Verify mock backend server started")
    public void testMockBackendServerStarted() {
        assertNotNull(wireMockServer, "WireMock server should be initialized");
        assertTrue(wireMockServer.isRunning(), "WireMock server should be running");
    }

    @Test(groups = {"wso2.am"}, enabled = true,
            description = "Verify mock embeddings provider returns data for known text")
    public void testMockEmbeddingsProviderResponse() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer mock-api-key");

        JSONObject payload = new JSONObject();
        payload.put("model", EMBEDDING_MODEL_NAME);
        payload.put("input", new JSONArray().put(REQUESTED_TOOL_TEXT));

        String mockEmbeddingUrl = ENDPOINT_HOST + ":" + mockPort + BACKEND_PATH + EMBEDDINGS_RESOURCE;
        HttpResponse response = HTTPSClientUtils.doPost(mockEmbeddingUrl, headers, payload.toString());

        assertEquals(response.getResponseCode(), HttpStatus.SC_OK,
                "Expected HTTP 200 from mock embeddings backend");
        assertEquals(response.getData(), mockEmbeddingsResponse,
                "Mock embeddings response body mismatch");

        wireMockServer.verify(1, postRequestedFor(urlEqualTo(BACKEND_PATH + EMBEDDINGS_RESOURCE))
                .withRequestBody(matchingJsonPath("$.model", equalTo(EMBEDDING_MODEL_NAME)))
                .withRequestBody(containing(REQUESTED_TOOL_TEXT)));

        List<LoggedRequest> embeddingRequests = wireMockServer.findAll(
            postRequestedFor(urlEqualTo(BACKEND_PATH + EMBEDDINGS_RESOURCE)));
        if (!embeddingRequests.isEmpty()) {
            log.info("###===### Received payload at mock AI backend (embeddings): " +
                embeddingRequests.get(0).getBodyAsString());
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Starts a WireMock HTTP server that acts as the mock AI backend.
     * Registers a stub for POST requests to the chat-completions resource.
     */
    private void startMockBackend() {
        mockPort = MOCK_BACKEND_PORT;

        wireMockServer = new WireMockServer(options().port(mockPort));

        // Stub: POST /mock-ai/v1/chat/completions → 200 OK with mock AI response
        wireMockServer.stubFor(WireMock.post(urlEqualTo(BACKEND_PATH + CHAT_RESOURCE))
            .withRequestBody(matchingJsonPath("$.contents[0].parts[0].text"))
            .withRequestBody(containing(REQUESTED_QUERY_TEXT_SNIPPET))
            .withRequestBody(containing(REQUESTED_TOOL_TEXT))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));

        // Stub for embeddings endpoint. Return mock embedding data only when the request includes
        // the expected text field content from mockembeddings.json.
        wireMockServer.stubFor(WireMock.post(urlEqualTo(BACKEND_PATH + EMBEDDINGS_RESOURCE))
            .atPriority(1)
            .withHeader("Authorization", containing("Bearer"))
            .withRequestBody(matchingJsonPath("$.model", equalTo(EMBEDDING_MODEL_NAME)))
            .withRequestBody(containing(REQUESTED_TOOL_TEXT))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(mockEmbeddingsResponse)));

        // Fallback for embeddings requests without the expected content.
        wireMockServer.stubFor(WireMock.post(urlEqualTo(BACKEND_PATH + EMBEDDINGS_RESOURCE))
            .atPriority(10)
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"Requested content not found in embeddings mock\"}")));

        wireMockServer.start();
        log.info("###===### Mock AI backend started on port " + mockPort);
        String mockBaseUrl = ENDPOINT_HOST + ":" + mockPort + BACKEND_PATH;
        log.info("###===### Mock backend base URL: " + mockBaseUrl);
        log.info("###===### Mock chat endpoint: " + mockBaseUrl + CHAT_RESOURCE);
        log.info("###===### Mock embeddings endpoint: " + mockBaseUrl + EMBEDDINGS_RESOURCE);
    }

    /**
     * Registers a custom AI service provider that uses no-auth configuration.
     * This provider is referenced by the AI API so the middleware knows how to
     * route requests to the backend.
     */
    private void registerAiServiceProvider() throws Exception {
        String providerConfig = readFile(resourcePath + PROVIDER_CONFIG_FILE);
        String apiDefinition  = readFile(resourcePath + API_DEFINITION_FILE);
        File defFile = writeTempFile(apiDefinition);

        String modelProviders = String.format(
                "[{\"models\": [\"%s\"], \"name\": \"%s\"}]", MODEL_NAME, PROVIDER_NAME);

        ApiResponse<AIServiceProviderResponseDTO> createResponse = restAPIAdmin.addAIServiceProvider(
                PROVIDER_NAME, PROVIDER_API_VERSION, PROVIDER_DESCRIPTION,
                false, providerConfig, defFile, modelProviders);

        assertEquals(createResponse.getStatusCode(), Response.Status.CREATED.getStatusCode(),
                "Failed to register mock AI service provider");
        aiServiceProviderId = createResponse.getData().getId();
        assertNotNull(aiServiceProviderId, "AI service provider ID must not be null");
        log.info("###===### Registered AI service provider: " + PROVIDER_NAME + " (id=" + aiServiceProviderId + ")");
    }

    /**
     * Creates an AI API in the middleware (APIM) whose backend endpoint is the
     * running WireMock server, then deploys a revision and publishes the API.
     */
    private void createAndPublishAiApi() throws Exception {
        // --- Build additionalProperties for the AI API ---
        JSONObject additionalProps = new JSONObject();
        additionalProps.put("name",        API_NAME);
        additionalProps.put("version",     API_VERSION);
        additionalProps.put("context",     API_CONTEXT);
        additionalProps.put("gatewayType", "wso2/synapse");
        additionalProps.put("policies",    new JSONArray("[\"Unlimited\"]"));

        // Endpoint configuration — point to the mock AI backend (WireMock)
        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        JSONObject productionEndpoints = new JSONObject();
        productionEndpoints.put("url", ENDPOINT_HOST + ":" + mockPort + BACKEND_PATH);
        endpointConfig.put("production_endpoints", productionEndpoints);
        additionalProps.put("endpointConfig", endpointConfig);

        // AI subtype — links this API to the registered AI service provider
        JSONObject subtypeConfig = new JSONObject();
        subtypeConfig.put("subtype", "AIAPI");
        JSONObject subtypeConfigInner = new JSONObject();
        subtypeConfigInner.put("llmProviderName",       PROVIDER_NAME);
        subtypeConfigInner.put("llmProviderApiVersion", PROVIDER_API_VERSION);
        subtypeConfig.put("configuration", subtypeConfigInner);
        additionalProps.put("subtypeConfiguration", subtypeConfig);

        // Token-based throttling (mirrors the existing ai-api resources)
        JSONObject maxTps = new JSONObject();
        maxTps.put("production", 1000);
        maxTps.put("sandbox",    1000);
        additionalProps.put("maxTps", maxTps);

        additionalProps.put("securityScheme", new JSONArray("[\"api_key\"]"));
        additionalProps.put("egress", true);

        // Import the API using the Mistral OAS definition
        String apiDefinition = readFile(resourcePath + API_DEFINITION_FILE);
        File defFile = writeTempFile(apiDefinition);
        APIDTO apidto = restAPIPublisher.importOASDefinition(defFile, additionalProps.toString());
        aiApiId = apidto.getId();
        assertNotNull(aiApiId, "AI API ID must not be null after creation");
        log.info("###===### Created AI API in middleware: " + API_NAME + " (id=" + aiApiId + ")");

        // Deploy revision to the gateway
        String revisionUUID = createAPIRevisionAndDeployUsingRest(aiApiId, restAPIPublisher);
        Assert.assertNotNull(revisionUUID, "Revision UUID must not be null");

        // Publish the API — after this the middleware hands out the gateway URL
        HttpResponse lifecycleResponse = restAPIPublisher.changeAPILifeCycleStatusToPublish(aiApiId, false);
        assertEquals(lifecycleResponse.getResponseCode(), HttpStatus.SC_OK,
                "Failed to publish AI API");

        waitForAPIDeploymentSync(apidto.getProvider(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        log.info("###===### AI API is published and available at the middleware gateway URL");
    }

    /**
     * Creates a consumer application, subscribes it to the AI API, and generates
     * a JWT-based API key used to authenticate requests through the gateway.
     */
    private void createAppAndSubscribe() throws Exception {
        // Create application
        ApplicationDTO appDTO = restAPIStore.addApplication(
                APP_NAME, APIThrottlingTier.UNLIMITED.getState(), "", "Mock AI backend test app");
        applicationId = appDTO.getApplicationId();
        assertNotNull(applicationId, "Application ID must not be null");

        // Subscribe to the AI API
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(
                aiApiId, applicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);
        assertNotNull(subscriptionDTO, "Subscription must not be null");

        // Generate JWT API key
        ApplicationDTO fullAppDTO = restAPIStore.getApplicationById(applicationId);
        JWTGenerator.JwtTokenInfo tokenInfo = new JWTGenerator.JwtTokenInfo.Builder()
                .endUsername(user.getUserName())
                .issuer(keyManagerHTTPSURL + "oauth2/token")
                .validityPeriod(3600)
                .keyType("PRODUCTION")
                .permittedIP(null)
                .permittedReferer(null)
                .applicationUUID(fullAppDTO.getApplicationId())
                .applicationName(fullAppDTO.getName())
                .applicationOwner(fullAppDTO.getOwner())
                .applicationTier(fullAppDTO.getThrottlingPolicy())
                .applicationId(restAPIInternal.getApplicationIdByUUID(
                        MultitenantUtils.getTenantDomain(user.getUserName()),
                        fullAppDTO.getApplicationId()))
                .build();
        apiKey = new JWTGenerator().generateToken(tokenInfo);
        assertNotNull(apiKey, "API key must not be null");
        log.info("###===### Consumer application created and API key generated for: " + APP_NAME);
    }

    /**
     * Writes {@code content} to a temporary file and schedules it for deletion on JVM exit.
     */
    private File writeTempFile(String content) throws IOException {
        File temp = File.createTempFile("ai-api-test", ".json");
        temp.deleteOnExit();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(temp))) {
            writer.write(content);
        }
        return temp;
    }
}
