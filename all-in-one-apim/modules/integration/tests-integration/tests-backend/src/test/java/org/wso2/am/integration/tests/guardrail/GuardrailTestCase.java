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
 * KIND, either express or implied. See the License for the
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
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.AIServiceProviderSummaryResponseDTO;
import org.wso2.am.integration.clients.admin.api.dto.AIServiceProviderSummaryResponseListDTO;
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
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.ws.rs.core.Response;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Guardrail test case that validates the Gemini AI API path and mocked Mistral embedding backend.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class GuardrailTestCase extends APIMIntegrationBaseTest {

	private static final Log log = LogFactory.getLog(GuardrailTestCase.class);

	private static final String API_NAME = "geminiAPI";
	private static final String API_VERSION = "1.0.0";
	private static final String API_CONTEXT = "geminiAPI";
	private static final String GEMINI_API_RESOURCE = "/v1beta/models/gemini-pro:generateContent";

	private static final int MOCK_BACKEND_PORT = 9977;
	private static final String MOCK_BACKEND_HOST = "http://localhost";
	private static final String GEMINI_BACKEND_BASE_PATH = "/gemini";
	private static final String MISTRAL_EMBEDDINGS_ENDPOINT = "https://api.mistral.ai/v1/embeddings";
	private static final String MISTRAL_EMBEDDINGS_RESOURCE = "/v1/embeddings";
	private static final String MISTRAL_EMBEDDINGS_MOCK_URL = MOCK_BACKEND_HOST + ":" + MOCK_BACKEND_PORT
			+ MISTRAL_EMBEDDINGS_RESOURCE;
	private static final String MISTRAL_EMBEDDINGS_MODEL = "mistral-embed";
	private static final String MISTRAL_INPUT_1 = "I'm planning a corporate retreat in Denver for next weekend. Can you find the weather forecast, book a conference room for 15 people, find a highly-rated catering service that offers vegan options, and then email the itinerary to my assistant at sarah@company.com?";
	private static final String MISTRAL_INPUT_2 = "Get current weather and 7-day forecast for a location.";
	private static final String MISTRAL_API_KEY = "mock-mistral-api-key";

	private static final String APPLICATION_NAME = "Guardrail-Test-Application";
	private static final String GEMINI_MOCK_RESPONSE_TEXT = "Mock Gemini response from test backend";

	private static final String GEMINI_DEFINITION_FILE = "gemini-def.json";
	private static final String GEMINI_ADD_PROPS_FILE = "gemini-add-props.json";
	private static final String MOCK_REQUEST_BODY_FILE = "mockrequestbody.json";
	private static final String MOCK_EMBEDDINGS_FILE = "mockembeddings.json";

	private String resourcePath;
	private String geminiApiId;
	private String applicationId;
	private String apiKey;

	private String mockRequestBody;
	private String mistralEmbeddingsRequestBody;
	private String mockEmbeddingsPayload;
	private String mockGeminiResponse;

	private WireMockServer wireMockServer;

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

		super.init(userMode);
		initializeTestData();
		startMockBackends();
		applicationId = createTestApplication();
		geminiApiId = createAndPublishGeminiAiApi();
		apiKey = subscribeToApiAndGenerateKey(geminiApiId);
	}

	@Test(groups = {"wso2.am"}, description = "Invoke Gemini AI API through APIM and verify backend capture")
	public void testGeminiAiApiInvocationWithMockBackend() throws Exception {

		Map<String, String> requestHeaders = new HashMap<>();
		requestHeaders.put("ApiKey", apiKey);
		requestHeaders.put("Content-Type", "application/json");

		HttpResponse serviceResponse = invokeGeminiWithGatewayPathFallback(requestHeaders);

		assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK,
				"Gemini AI API invocation failed");

		JSONObject responseJson = new JSONObject(serviceResponse.getData());
		String responseText = responseJson.getJSONArray("candidates")
				.getJSONObject(0)
				.getJSONObject("content")
				.getJSONArray("parts")
				.getJSONObject(0)
				.getString("text");
		assertEquals(responseText, GEMINI_MOCK_RESPONSE_TEXT,
				"Unexpected Gemini mock backend response payload");

		List<LoggedRequest> geminiRequests = wireMockServer.findAll(
				postRequestedFor(urlEqualTo(GEMINI_BACKEND_BASE_PATH + GEMINI_API_RESOURCE)));
		assertFalse(geminiRequests.isEmpty(), "Gemini mock backend did not receive a request");

		boolean foundExpectedGeminiRequest = false;
		for (LoggedRequest request : geminiRequests) {
			if (request.getBodyAsString().contains("function_declarations")
					&& request.getBodyAsString().contains("corporate retreat in Denver")) {
				foundExpectedGeminiRequest = true;
				break;
			}
		}
		assertTrue(foundExpectedGeminiRequest,
				"Gemini mock backend request did not contain the expected guardrail request payload");
	}

	@Test(groups = {"wso2.am"}, description = "Call mocked Mistral embedding backend and validate embedding response",
			dependsOnMethods = "testGeminiAiApiInvocationWithMockBackend")
	public void testMistralEmbeddingsMockBackend() throws Exception {

		Map<String, String> requestHeaders = new HashMap<>();
		requestHeaders.put("Content-Type", "application/json");
		requestHeaders.put("Authorization", "Bearer " + MISTRAL_API_KEY);

		String mockedEmbeddingsUrl = getMockUrlForEndpoint(MISTRAL_EMBEDDINGS_ENDPOINT);
		HttpResponse embeddingsResponse = HttpRequestUtil.doPost(new URL(mockedEmbeddingsUrl),
				mistralEmbeddingsRequestBody, requestHeaders);

		assertEquals(embeddingsResponse.getResponseCode(), HttpStatus.SC_OK,
				"Mistral embeddings mock backend invocation failed");

		JSONArray expectedEmbeddings = new JSONArray(mockEmbeddingsPayload);
		JSONArray actualEmbeddings = new JSONArray(embeddingsResponse.getData());

		assertEquals(actualEmbeddings.length(), expectedEmbeddings.length(),
				"Embedding record count mismatch");

		JSONObject expectedFirstRecord = expectedEmbeddings.getJSONObject(0);
		JSONObject actualFirstRecord = actualEmbeddings.getJSONObject(0);

		assertEquals(actualFirstRecord.getString("text"), expectedFirstRecord.getString("text"),
				"Embedding text mismatch in first record");
		assertEquals(actualFirstRecord.getString("source"), expectedFirstRecord.getString("source"),
				"Embedding source mismatch in first record");
		assertEquals(actualFirstRecord.getBoolean("is_user_query"), expectedFirstRecord.getBoolean("is_user_query"),
				"Embedding is_user_query mismatch in first record");

		JSONArray expectedVector = expectedFirstRecord.getJSONArray("embedding");
		JSONArray actualVector = actualFirstRecord.getJSONArray("embedding");
		assertEquals(actualVector.length(), expectedVector.length(),
				"Embedding vector length mismatch in first record");
		assertEquals(actualVector.getDouble(0), expectedVector.getDouble(0), 0.0,
				"Embedding vector first value mismatch in first record");

		List<LoggedRequest> embeddingRequests = wireMockServer.findAll(
				postRequestedFor(urlEqualTo(MISTRAL_EMBEDDINGS_RESOURCE)));
		assertFalse(embeddingRequests.isEmpty(), "Mistral embeddings endpoint did not receive a request");

		boolean foundExpectedEmbeddingRequest = false;
		for (LoggedRequest request : embeddingRequests) {
			JSONObject requestJson = new JSONObject(request.getBodyAsString());
			JSONArray inputArray = requestJson.optJSONArray("input");
			boolean hasExpectedInputArray = inputArray != null && inputArray.length() == 2
					&& MISTRAL_INPUT_1.equals(inputArray.optString(0))
					&& MISTRAL_INPUT_2.equals(inputArray.optString(1));

			if (MISTRAL_EMBEDDINGS_MODEL.equals(requestJson.optString("model"))
					&& hasExpectedInputArray
					&& ("Bearer " + MISTRAL_API_KEY).equals(request.getHeader("Authorization"))) {
				foundExpectedEmbeddingRequest = true;
				break;
			}
		}
		assertTrue(foundExpectedEmbeddingRequest,
				"Mistral embeddings backend request did not match expected model, input array, and auth header");
		assertEquals(mockedEmbeddingsUrl, MISTRAL_EMBEDDINGS_MOCK_URL,
				"Mocked embeddings URL should match the configured Mistral endpoint path");
	}

	@AfterClass(alwaysRun = true)
	public void cleanUpArtifacts() throws Exception {
		if (applicationId != null) {
			try {
				restAPIStore.deleteApplication(applicationId);
			} catch (Exception e) {
				log.warn("Error while deleting application " + applicationId + ": " + e.getMessage());
			}
		}

		if (geminiApiId != null) {
			try {
				undeployAndDeleteAPIRevisionsUsingRest(geminiApiId, restAPIPublisher);
			} catch (Exception e) {
				log.warn("Error while undeploying revisions for API " + geminiApiId + ": " + e.getMessage());
			}

			try {
				restAPIPublisher.deleteAPI(geminiApiId);
			} catch (Exception e) {
				log.warn("Error while deleting API " + geminiApiId + ": " + e.getMessage());
			}
		}

		if (wireMockServer != null) {
			try {
				wireMockServer.stop();
			} catch (Exception e) {
				log.warn("Error while stopping WireMock backend: " + e.getMessage());
			}
		}
	}

	private void initializeTestData() throws Exception {

		resourcePath = TestConfigurationProvider.getResourceLocation() + "guardrail" + File.separator;
		mockRequestBody = readFile(resourcePath + MOCK_REQUEST_BODY_FILE);
		mistralEmbeddingsRequestBody = buildMistralEmbeddingsRequestBody();
		mockEmbeddingsPayload = readFile(resourcePath + MOCK_EMBEDDINGS_FILE);
		mockGeminiResponse = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\""
				+ GEMINI_MOCK_RESPONSE_TEXT + "\"}]}}]}";
	}

	private void startMockBackends() {

		wireMockServer = new WireMockServer(options().port(MOCK_BACKEND_PORT));

		wireMockServer.stubFor(WireMock.post(urlEqualTo(GEMINI_BACKEND_BASE_PATH + GEMINI_API_RESOURCE))
				.withRequestBody(matchingJsonPath("$.contents[0].parts[0].text"))
				.withRequestBody(matchingJsonPath("$.tools[0].function_declarations[0].name",
						equalTo("get_weather")))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(mockGeminiResponse)));

		wireMockServer.stubFor(WireMock.post(urlEqualTo(MISTRAL_EMBEDDINGS_RESOURCE))
				.withHeader("Authorization", equalTo("Bearer " + MISTRAL_API_KEY))
				.withRequestBody(matchingJsonPath("$.model", equalTo(MISTRAL_EMBEDDINGS_MODEL)))
				.withRequestBody(matchingJsonPath("$.input[0]", equalTo(MISTRAL_INPUT_1)))
				.withRequestBody(matchingJsonPath("$.input[1]", equalTo(MISTRAL_INPUT_2)))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody(mockEmbeddingsPayload)));

		wireMockServer.start();
		log.info("Guardrail mock backend started on port " + MOCK_BACKEND_PORT);
	}

	private String createTestApplication() throws Exception {

		ApplicationDTO applicationDTO = restAPIStore.addApplication(APPLICATION_NAME,
				APIThrottlingTier.UNLIMITED.getState(), "", "guardrail-test");
		String appId = applicationDTO.getApplicationId();
		assertNotNull(appId, "Application ID should not be null");
		return appId;
	}

	private String createAndPublishGeminiAiApi() throws Exception {

		String apiDefinition = readFile(resourcePath + GEMINI_DEFINITION_FILE);
		String additionalProperties = readFile(resourcePath + GEMINI_ADD_PROPS_FILE);
		JSONObject additionalPropertiesObj = new JSONObject(additionalProperties);
		resolveAndSetGeminiProviderConfiguration(additionalPropertiesObj);

		JSONObject endpointConfig = additionalPropertiesObj.getJSONObject("endpointConfig");
		JSONObject productionEndpoints = new JSONObject();
		productionEndpoints.put("url", MOCK_BACKEND_HOST + ":" + MOCK_BACKEND_PORT + GEMINI_BACKEND_BASE_PATH);
		endpointConfig.put("production_endpoints", productionEndpoints);
		additionalPropertiesObj.put("endpointConfig", endpointConfig);

		File definitionFile = getTempFileWithContent(apiDefinition);
		APIDTO apidto = restAPIPublisher.importOASDefinition(definitionFile, additionalPropertiesObj.toString());
		String createdApiId = apidto.getId();

		HttpResponse createdApiResponse = restAPIPublisher.getAPI(createdApiId);
		assertEquals(createdApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
				"Failed to create Gemini AI API");

		String revisionUUID = createAPIRevisionAndDeployUsingRest(createdApiId, restAPIPublisher);
		assertNotNull(revisionUUID, "Revision UUID should not be null");

		HttpResponse lifecycleResponse = restAPIPublisher.changeAPILifeCycleStatusToPublish(createdApiId, false);
		assertEquals(lifecycleResponse.getResponseCode(), HttpStatus.SC_OK,
				"Failed to publish Gemini AI API");

		waitForAPIDeploymentSync(apidto.getProvider(), apidto.getName(), apidto.getVersion(),
				APIMIntegrationConstants.IS_API_EXISTS);
		return createdApiId;
	}

	private void resolveAndSetGeminiProviderConfiguration(JSONObject additionalPropertiesObj) throws Exception {

		ApiResponse<AIServiceProviderSummaryResponseListDTO> providersResponse = restAPIAdmin.getAIServiceProviders();
		assertEquals(providersResponse.getStatusCode(), Response.Status.OK.getStatusCode(),
				"Failed to retrieve AI service providers for Gemini configuration");

		assertNotNull(providersResponse.getData(), "AI service provider list response should not be null");
		List<AIServiceProviderSummaryResponseDTO> providerList = providersResponse.getData().getList();
		assertNotNull(providerList, "AI service provider list should not be null");

		AIServiceProviderSummaryResponseDTO geminiProvider = null;
		for (AIServiceProviderSummaryResponseDTO provider : providerList) {
			if (provider != null && provider.getName() != null &&
					(provider.getName().equalsIgnoreCase("Gemini") ||
							provider.getName().toLowerCase().contains("gemini"))) {
				geminiProvider = provider;
				break;
			}
		}

		assertNotNull(geminiProvider, "Gemini AI service provider is not available in predefined providers");
		assertNotNull(geminiProvider.getApiVersion(), "Gemini AI service provider API version should not be null");

		JSONObject subtypeConfiguration = additionalPropertiesObj.optJSONObject("subtypeConfiguration");
		if (subtypeConfiguration == null) {
			subtypeConfiguration = new JSONObject();
			additionalPropertiesObj.put("subtypeConfiguration", subtypeConfiguration);
		}
		subtypeConfiguration.put("subtype", "AIAPI");

		JSONObject configuration = subtypeConfiguration.optJSONObject("configuration");
		if (configuration == null) {
			configuration = new JSONObject();
			subtypeConfiguration.put("configuration", configuration);
		}
		configuration.put("llmProviderName", geminiProvider.getName());
		configuration.put("llmProviderApiVersion", geminiProvider.getApiVersion());

		log.info("Resolved Gemini provider config - Name: " + geminiProvider.getName() + ", Version: "
				+ geminiProvider.getApiVersion());
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
						MultitenantUtils.getTenantDomain(user.getUserName()), applicationDTO.getApplicationId()))
				.build();

		String generatedApiKey = new JWTGenerator().generateToken(tokenInfo);
		assertNotNull(generatedApiKey, "API Key should not be null");
		return generatedApiKey;
	}

	private File getTempFileWithContent(String content) throws IOException {

		File tempDirectory = new File(System.getProperty("user.dir"), "target/guardrail-temp");
		if (!tempDirectory.exists() && !tempDirectory.mkdirs()) {
			throw new IOException("Failed to create temporary directory: " + tempDirectory.getAbsolutePath());
		}

		File tempFile = File.createTempFile("guardrail-api-", ".json", tempDirectory);
		tempFile.deleteOnExit();

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
			writer.write(content);
		}
		return tempFile;
	}

	private String getMockUrlForEndpoint(String endpointUrl) throws Exception {

		URL endpoint = new URL(endpointUrl);
		return MOCK_BACKEND_HOST + ":" + MOCK_BACKEND_PORT + endpoint.getPath();
	}

	private String buildMistralEmbeddingsRequestBody() throws JSONException {

		JSONObject requestBody = new JSONObject();
		requestBody.put("model", MISTRAL_EMBEDDINGS_MODEL);
		requestBody.put("input", new JSONArray().put(MISTRAL_INPUT_1).put(MISTRAL_INPUT_2));
		return requestBody.toString();
	}

	private HttpResponse invokeGeminiWithGatewayPathFallback(Map<String, String> requestHeaders) throws Exception {

		String versionedInvokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION) + GEMINI_API_RESOURCE;
		HttpResponse response = HTTPSClientUtils.doPost(versionedInvokeURL, requestHeaders, mockRequestBody);

		if (response.getResponseCode() == HttpStatus.SC_NOT_FOUND) {
			String unversionedInvokeURL = getAPIInvocationURLHttp(API_CONTEXT) + GEMINI_API_RESOURCE;
			HttpResponse fallbackResponse = HTTPSClientUtils.doPost(unversionedInvokeURL, requestHeaders, mockRequestBody);

			if (fallbackResponse.getResponseCode() == HttpStatus.SC_OK) {
				log.info("Gemini invocation succeeded with unversioned gateway path: " + unversionedInvokeURL);
				return fallbackResponse;
			}

			log.warn("Gemini invocation failed for both gateway paths. Versioned URL status: "
					+ response.getResponseCode() + ", Unversioned URL status: " + fallbackResponse.getResponseCode());
			return fallbackResponse;
		}

		return response;
	}
}
