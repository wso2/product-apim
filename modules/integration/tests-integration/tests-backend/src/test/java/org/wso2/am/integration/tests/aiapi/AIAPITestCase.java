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
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * AI API Test Case
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE })
public class AIAPITestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(AIAPITestCase.class);

    // Variables related to mistral AI API
    private static final String mistralAPIName = "mistralAPI";
    private static final String mistralAPIVersion = "1.0.0";
    private static final String mistralAPIContext = "mistralAPI";
    private static final String mistralAPIEndpoint = "/mistral";
    private static final String mistralAPIResource = "/v1/chat/completions";
    private String mistralAPIId;
    private String mistralResponse;

    // Application and API related common details
    private static final String apiProvider = "admin";
    private static final String applicationName = "AI-API-Application";
    private String applicationId;

    // Other variables
    private String resourcePath;
    private WireMockServer wireMockServer;
    private int endpointPort;
    private String endpointHost = "http://localhost";
    private int lowerPortLimit = 9950;
    private int upperPortLimit = 9999;


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

        // Add application
        ApplicationDTO applicationDTO = restAPIStore.addApplication(applicationName,
                APIThrottlingTier.UNLIMITED.getState(), "", "this-is-test");
        applicationId = applicationDTO.getApplicationId();
        Assert.assertNotNull(applicationId);

        resourcePath = TestConfigurationProvider.getResourceLocation() + "ai-api" + File.separator;
        mistralResponse = readFile(resourcePath + "mistral-response.json");

        // Start WireMock server
        startWiremockServer();
    }

    /**
     * Test Mistral AI API creation, deployment, and publishing
     */
    @Test(groups = { "wso2.am" }, description = "Test Mistral AI API creation, deployment and publishing")
    public void testMistralAIAPICreationAndPublish() throws Exception {
        String originalDefinition = readFile(resourcePath + "mistral-def.json");
        String additionalProperties = readFile(resourcePath + "mistral-add-props.json");
        JSONObject additionalPropertiesObj = new JSONObject(additionalProperties);

        // Update production endpoint
        JSONObject endpointConfig = additionalPropertiesObj.getJSONObject("endpointConfig");
        JSONObject productionEndpoints = new JSONObject();
        productionEndpoints.put("url", endpointHost + ":" + endpointPort + mistralAPIEndpoint);
        endpointConfig.put("production_endpoints", productionEndpoints);
        additionalPropertiesObj.put("endpointConfig", endpointConfig);

        // Create API
        File file = getTempFileWithContent(originalDefinition);
        APIDTO apidto = restAPIPublisher.importOASDefinition(file, additionalPropertiesObj.toString());
        mistralAPIId = apidto.getId();

        HttpResponse createdApiResponse = restAPIPublisher.getAPI(mistralAPIId);
        assertEquals(Response.Status.OK.getStatusCode(),
                createdApiResponse.getResponseCode(), mistralAPIId + " AI API creation failed");

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
    @Test(groups = { "wso2.am" }, description = "Test AI API invocation",
            dependsOnMethods = "testMistralAIAPICreationAndPublish")
    public void testMistralAIApiInvocation() throws Exception {

        // Subscribe to API
        SubscriptionDTO subscriptionDTO = restAPIStore.
                subscribeToAPI(mistralAPIId, applicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);
        assertNotNull(subscriptionDTO, "Mistral AI API Subscription failed");

        // Get API Key
        APIKeyDTO apiKeyDTO = restAPIStore.
                generateAPIKeys(applicationId, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.toString(),
                        -1, null, null);
        assertNotNull(apiKeyDTO, "Mistral AI API Key generation failed");
        String apiKey = apiKeyDTO.getApikey();

        // Invoke API
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("ApiKey", apiKey);
        String invokeURL = getAPIInvocationURLHttp(mistralAPIContext, mistralAPIVersion) +
                mistralAPIResource;
        String mistralPayload = readFile(resourcePath + "mistral-payload.json");
        HttpResponse serviceResponse = HTTPSClientUtils.
                doPost(invokeURL, requestHeaders, mistralPayload);

        assertEquals(HttpStatus.SC_OK, serviceResponse.getResponseCode(), "Failed to invoke Mistral AI API");
        assertEquals(mistralResponse, serviceResponse.getData(), "Mistral AI API response mismatch");
    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
        restAPIStore.removeAPISubscriptionByName(mistralAPIName, mistralAPIVersion, apiProvider, applicationName);
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(mistralAPIId);
    }

    private File getTempFileWithContent(String content) throws IOException {
        File temp = File.createTempFile("swagger", ".json");
        try (BufferedWriter out = new BufferedWriter(new FileWriter(temp))) {
            out.write(content);
        }
        temp.deleteOnExit();
        return temp;
    }

    private void startWiremockServer() throws Exception {
        endpointPort = getAvailablePort();
        wireMockServer = new WireMockServer(options().port(endpointPort));

        wireMockServer.stubFor(WireMock.post(urlEqualTo(mistralAPIEndpoint + mistralAPIResource))
                .willReturn(aResponse().withStatus(200).
                        withHeader("Content-Type", "application/json").withBody(mistralResponse)));

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
     * Check whether give port is available
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
