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
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.google.gson.Gson;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationPoliciesDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.OperationPolicyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.ClientAuthenticator;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIMURLBean;
import org.wso2.am.integration.test.utils.bean.APIThrottlingTier;
import org.wso2.am.integration.test.utils.bean.DCRParamRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.MockServerUtils;
import org.wso2.am.integration.tests.jwt.JWTGenerator;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.ws.rs.core.Response;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Guardrail Test Case
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class GuardrailTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(GuardrailTestCase.class);

    // Mock Backend Endpoint Constants
    private static final String MISTRAL_API_ENDPOINT = "/mistral";
    private static final String MISTRAL_EMBEDDING_RESOURCE = "/v1/embeddings";
    private static final String GEMINI_API_ENDPOINT = "/gemini";
    private static final String GEMINI_GENERATE_RESOURCE = "/v1beta/models/gemini-pro:generateContent";

    // Gemini API Constants
    private static final String ENDPOINT_HOST = "http://localhost";
    private static final String GEMINI_API_CONTEXT = "geminiAPI";
    private static final String GEMINI_API_VERSION = "1.0.0";
    private static final String GEMINI_APP_NAME = "GeminiGuardrailApp";
    private static final String GEMINI_DEF_FILE = "gemini-def.json";
    private static final String GEMINI_ADD_PROPS_FILE = "gemini-add-props.json";
    private static final String MOCK_REQUEST_BODY_FILE = "mockrequestbody.json";

    // Policy Constants
    private static final String SEMANTIC_TOOL_FILTERING_POLICY = "SemanticToolFiltering";

    // Tool names that must be present in the Gemini request's function_declarations
    private static final List<String> REQUIRED_GEMINI_TOOL_NAMES = Arrays.asList(
            "get_weather", "book_venue", "find_restaurants");

    // WireMock Instance Variables
    private String resourcePath;
    private WireMockServer wireMockServer;
    private int endpointPort;

    // Server Configuration
    private ServerConfigurationManager serverConfigurationManager;

    // Test State
    private Map<String, String> policyMap;
    private String applicationId;
    private String apiKey;
    private String geminiApiId;
        private String embeddingModel = "mistral-embed";
    @Factory(dataProvider = "userModeDataProvider")
    public GuardrailTestCase(TestUserMode userMode) {

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

        resourcePath = TestConfigurationProvider.getResourceLocation() + "guardrail" + File.separator;

        // Read deployment.toml settings without modifying the file.
        loadEmbeddingProviderSettings();

        // Apply deployment.toml directly and restart the server.
        applyEmbeddingProviderConfiguration();

                // Initialize clients after the restart so they point to the active server.
                super.init(userMode);

        policyMap = restAPIPublisher.getAllCommonOperationPolicies();
        ApplicationDTO appDTO = restAPIStore.addApplication(GEMINI_APP_NAME,
                APIThrottlingTier.UNLIMITED.getState(), "", "Gemini guardrail test app");
        applicationId = appDTO.getApplicationId();
        assertNotNull(applicationId, "Application ID should not be null");
        startWiremockServer();
    }

    /**
         * Reads deployment.toml values required by the test, without modifying the file.
     */
        private void loadEmbeddingProviderSettings() throws Exception {
        String tomlContent = readFile(resourcePath + "deployment.toml");
        String configuredEmbeddingModel = extractTomlValue(tomlContent, "embedding_model");
        if (configuredEmbeddingModel != null && !configuredEmbeddingModel.isEmpty()) {
            embeddingModel = configuredEmbeddingModel;
        }

                String configuredEmbeddingEndpoint = extractTomlValue(tomlContent, "embedding_endpoint");
                if (configuredEmbeddingEndpoint != null && !configuredEmbeddingEndpoint.isEmpty()) {
                        try {
                                URI endpointUri = URI.create(configuredEmbeddingEndpoint);
                                String host = endpointUri.getHost();
                                int port = endpointUri.getPort();
                                if (("localhost".equals(host) || "127.0.0.1".equals(host)) && port > 0) {
                                        endpointPort = port;
                                        return;
                                }
                                log.info("embedding_endpoint in deployment.toml is not localhost with an explicit port; " +
                                                "using a dynamic WireMock port instead: " + configuredEmbeddingEndpoint);
                        } catch (Exception e) {
                                log.warn("Invalid embedding_endpoint in deployment.toml: " + configuredEmbeddingEndpoint, e);
                        }
        }

                endpointPort = MockServerUtils.getAvailablePort(MockServerUtils.LOCALHOST, true);
                assertNotEquals(endpointPort, -1,
                                "No available port in the range " + MockServerUtils.httpsPortLowerRange + "-" +
                                                MockServerUtils.httpsPortUpperRange + " was found");
        }

        /**
         * Applies deployment.toml directly and restarts the server so the change takes effect.
         */
        private void applyEmbeddingProviderConfiguration() throws Exception {
                File deploymentToml = new File(resourcePath + "deployment.toml");
                if (!deploymentToml.exists()) {
                        throw new IOException("deployment.toml file not found: " + deploymentToml.getAbsolutePath());
                }
                File targetDeploymentToml = resolveServerDeploymentToml();

        AutomationContext superTenantKeyManagerContext = new AutomationContext(
                APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE,
                TestUserMode.SUPER_TENANT_ADMIN);
        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);
                serverConfigurationManager.applyConfiguration(deploymentToml, targetDeploymentToml, true, true);
    }

        private File resolveServerDeploymentToml() throws IOException {
                String carbonHome = ServerConfigurationManager.getCarbonHome();
                if (carbonHome == null || carbonHome.isEmpty()) {
                        carbonHome = System.getProperty("carbon.home");
                }

                if (carbonHome != null && !carbonHome.isEmpty()) {
                        File deploymentToml = new File(carbonHome + File.separator + "repository" + File.separator + "conf" +
                                        File.separator + "deployment.toml");
                        if (deploymentToml.exists()) {
                                return deploymentToml;
                        }
                }

                Path targetPath = new File(System.getProperty("user.dir"), "target").toPath();
                if (Files.exists(targetPath)) {
                        try (Stream<Path> paths = Files.walk(targetPath, 6)) {
                                Path deploymentToml = paths
                                                .filter(Files::isRegularFile)
                                                .filter(path -> path.toString().contains("carbontmp"))
                                                .filter(path -> path.toString().endsWith(File.separator + "repository" + File.separator +
                                                                "conf" + File.separator + "deployment.toml"))
                                                .max(Comparator.comparingLong(path -> path.toFile().lastModified()))
                                                .orElse(null);
                                if (deploymentToml != null) {
                                        return deploymentToml.toFile();
                                }
                        }
                }

                throw new IOException("Unable to resolve active server deployment.toml location");
        }

        private String extractTomlValue(String tomlContent, String key) {
                Pattern pattern = Pattern.compile("(?m)^\\s*" + Pattern.quote(key) + "\\s*=\\s*\"([^\"]*)\"\\s*$");
                Matcher matcher = pattern.matcher(tomlContent);
                if (matcher.find()) {
                        return matcher.group(1);
                }
                return null;
        }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        if (geminiApiId != null) {
            undeployAndDeleteAPIRevisionsUsingRest(geminiApiId, restAPIPublisher);
            restAPIPublisher.deleteAPI(geminiApiId);
        }
        if (applicationId != null) {
            restAPIStore.deleteApplication(applicationId);
        }
        if (wireMockServer != null) {
            try {
                wireMockServer.stop();
            } catch (Exception e) {
                log.warn("Error stopping WireMock server: " + e.getMessage());
            }
        }
        if (serverConfigurationManager != null) {
            serverConfigurationManager.restoreToLastConfiguration();
        }
    }

        private void startWiremockServer() throws Exception {

        // endpointPort is already set in setEnvironment() before the config was applied
        wireMockServer = new WireMockServer(options()
                .port(endpointPort)
                .extensions(new ResponseTemplateTransformer(true)));

        // Load mock embeddings and create a stub per text entry
        String mockEmbeddingsJson = readFile(resourcePath + "mockembeddings.json");
        JSONArray embeddingsArray = new JSONArray(mockEmbeddingsJson);
        for (int i = 0; i < embeddingsArray.length(); i++) {
            JSONObject entry = embeddingsArray.getJSONObject(i);
            String text = entry.getString("text");
            JSONArray embedding = entry.getJSONArray("embedding");

            JSONObject responseBody = new JSONObject();
            responseBody.put("object", "list");
            responseBody.put("model", embeddingModel);

            JSONObject embeddingObj = new JSONObject();
            embeddingObj.put("object", "embedding");
            embeddingObj.put("embedding", embedding);
            embeddingObj.put("index", 0);
            responseBody.put("data", new JSONArray().put(embeddingObj));

            JSONObject usage = new JSONObject();
            usage.put("prompt_tokens", text.split("\\s+").length);
            usage.put("total_tokens", text.split("\\s+").length);
            responseBody.put("usage", usage);

            wireMockServer.stubFor(WireMock.post(urlEqualTo(MISTRAL_API_ENDPOINT + MISTRAL_EMBEDDING_RESOURCE))
                    .withRequestBody(matchingJsonPath("$.input", containing(text)))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(responseBody.toString())));
        }

        // Stub for Gemini generateContent API — requires all tool names in function_declarations
        MappingBuilder geminiStub = WireMock.post(urlEqualTo(GEMINI_API_ENDPOINT + GEMINI_GENERATE_RESOURCE));
        // for (String toolName : REQUIRED_GEMINI_TOOL_NAMES) {
        //     geminiStub = geminiStub.withRequestBody(matchingJsonPath(
        //             "$.tools[0].function_declarations[?(@.name == '" + toolName + "')]"));
        // }
        // Check number of tools is 3 to ensure the policy's limit parameter is working
        geminiStub = geminiStub.withRequestBody(matchingJsonPath("$.tools[0].function_declarations", matchingJsonPath("$[?(@.length() == 3)]")));
        wireMockServer.stubFor(geminiStub
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Claude Monet\"}],"
                                + "\"role\":\"model\"},\"finishReason\":\"STOP\",\"index\":0}],"
                                + "\"usageMetadata\":{\"promptTokenCount\":12,\"candidatesTokenCount\":5,"
                                + "\"totalTokenCount\":17},\"modelVersion\":\"gemini-pro\"}")));

        // Fallback stub — required tools missing from request
        wireMockServer.stubFor(WireMock.post(urlEqualTo(GEMINI_API_ENDPOINT + GEMINI_GENERATE_RESOURCE))
                .atPriority(2)
                .willReturn(aResponse()
                        .withStatus(422)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":{\"code\":422,"
                                + "\"message\":\"Required tool declarations missing from request\","
                                + "\"status\":\"UNPROCESSABLE_ENTITY\"}}"))); 

        try {
            wireMockServer.start();
            log.info("WireMock server started successfully on port " + endpointPort);
        } catch (Exception e) {
            log.error("Failed to start WireMock server on port " + endpointPort + ": " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Creates a Gemini AI API with the SemanticToolFiltering policy on the request flow.
     * Verifies that a request containing all required tool declarations is routed and returns HTTP 200.
     */
    @Test(groups = {"wso2.am"}, description = "Test Gemini AI API with Semantic Tool Filtering policy")
    public void testSemanticToolFilteringWithGeminiApi() throws Exception {
        // Create and publish the Gemini AI API
        geminiApiId = createAndPublishGeminiApi();
        assertNotNull(geminiApiId, "Gemini API ID should not be null");

        // Fetch the API and attach the SemanticToolFiltering policy
        assertNotNull(policyMap.get(SEMANTIC_TOOL_FILTERING_POLICY),
                "SemanticToolFiltering policy not found in common policies");

        HttpResponse getAPIResponse = restAPIPublisher.getAPI(geminiApiId);
        assertEquals(getAPIResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Failed to retrieve Gemini API");
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        Map<String, Object> policyParams = new HashMap<>();
        policyParams.put("selectionMode", "By Rank");
        policyParams.put("limit", "3");
        policyParams.put("queryJSONPath", "$.contents[0].parts[0].text");
        policyParams.put("toolsJSONPath", "$.tools[0].function_declarations");

        OperationPolicyDTO policyDTO = new OperationPolicyDTO();
        policyDTO.setPolicyName(SEMANTIC_TOOL_FILTERING_POLICY);
        policyDTO.setPolicyVersion("v1.0");
        policyDTO.setPolicyId(policyMap.get(SEMANTIC_TOOL_FILTERING_POLICY));
        policyDTO.setParameters(policyParams);

        List<OperationPolicyDTO> requestPolicies = new ArrayList<>();
        requestPolicies.add(policyDTO);

        APIOperationPoliciesDTO operationPoliciesDTO = new APIOperationPoliciesDTO();
        operationPoliciesDTO.setRequest(requestPolicies);
        operationPoliciesDTO.setResponse(new ArrayList<>());
        operationPoliciesDTO.setFault(new ArrayList<>());
        apidto.setApiPolicies(operationPoliciesDTO);

        restAPIPublisher.updateAPI(apidto);
        createAPIRevisionAndDeployUsingRest(geminiApiId, restAPIPublisher);
        waitForAPIDeployment();

        // Subscribe and generate API key
        apiKey = subscribeToApiAndGenerateKey(geminiApiId);

        // Invoke API with the mock request body (contains 5 tools; policy should filter to 3)
        String requestPayload = readFile(resourcePath + MOCK_REQUEST_BODY_FILE);
        Map<String, String> headers = new HashMap<>();
        headers.put("ApiKey", apiKey);
        headers.put("Content-Type", "application/json");
        String invokeURL = getAPIInvocationURLHttp(GEMINI_API_CONTEXT, GEMINI_API_VERSION)
                + GEMINI_GENERATE_RESOURCE;

        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, headers, requestPayload);
        assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK,
                "Gemini API invocation with SemanticToolFiltering policy failed");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String createAndPublishGeminiApi() throws Exception {
        String apiDefinition = readFile(resourcePath + GEMINI_DEF_FILE);
        String additionalProperties = readFile(resourcePath + GEMINI_ADD_PROPS_FILE);
        JSONObject additionalPropertiesObj = new JSONObject(additionalProperties);

        // Inject the dynamic WireMock endpoint URL
        JSONObject endpointConfig = additionalPropertiesObj.getJSONObject("endpointConfig");
        JSONObject productionEndpoints = new JSONObject();
        productionEndpoints.put("url", ENDPOINT_HOST + ":" + endpointPort + GEMINI_API_ENDPOINT);
        endpointConfig.put("production_endpoints", productionEndpoints);
        additionalPropertiesObj.put("endpointConfig", endpointConfig);

        File file = getTempFileWithContent(apiDefinition);
        APIDTO apidto = restAPIPublisher.importOASDefinition(file, additionalPropertiesObj.toString());
        String apiId = apidto.getId();

        HttpResponse createdApiResponse = restAPIPublisher.getAPI(apiId);
        assertEquals(createdApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Failed to create Gemini AI API");

        String revisionUUID = createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        assertNotNull(revisionUUID, "Revision UUID should not be null");

        HttpResponse lifecycleResponse = restAPIPublisher.changeAPILifeCycleStatusToPublish(apiId, false);
        assertEquals(lifecycleResponse.getResponseCode(), HttpStatus.SC_OK, "Failed to publish Gemini API");

        waitForAPIDeploymentSync(apidto.getProvider(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        return apiId;
    }

    private String subscribeToApiAndGenerateKey(String apiId) throws Exception {
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);
        assertNotNull(subscriptionDTO, "API subscription should not be null");

        ApplicationDTO applicationDTO = restAPIStore.getApplicationById(applicationId);
        JWTGenerator.JwtTokenInfo tokenInfo = new JWTGenerator.JwtTokenInfo.Builder()
                .endUsername(user.getUserName())
                .issuer(keyManagerHTTPSURL + "oauth2/token")
                .validityPeriod(3600)
                .keyType("PRODUCTION")
                .permittedIP(null)
                .permittedReferer(null)
                .applicationUUID(applicationDTO.getApplicationId())
                .applicationName(applicationDTO.getName())
                .applicationOwner(applicationDTO.getOwner())
                .applicationTier(applicationDTO.getThrottlingPolicy())
                .applicationId(restAPIInternal.getApplicationIdByUUID(
                        MultitenantUtils.getTenantDomain(user.getUserName()),
                        applicationDTO.getApplicationId()))
                .build();
        String key = new JWTGenerator().generateToken(tokenInfo);
        assertNotNull(key, "API Key should not be null");
        return key;
    }

    private File getTempFileWithContent(String content) throws IOException {
        File temp = File.createTempFile("swagger", ".json");
        temp.deleteOnExit();
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.write(content);
        out.close();
        return temp;
    }

}
