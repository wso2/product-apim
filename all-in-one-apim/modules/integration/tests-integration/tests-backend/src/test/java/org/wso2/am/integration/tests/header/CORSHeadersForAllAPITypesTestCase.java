/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

package org.wso2.am.integration.tests.header;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.dto.*;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Verifies CORS handling for the supported API types after enabling CORS with the
 * {@code X-BrowserSessionID} access-control-allow header.
 * <p>
 * The covered API types are: REST, SOAP service exposed as REST, GraphQL, Server Sent Events
 * (SSE) and an Async API imported from a file.
 * <p>
 * For each API type the test asserts:
 * <ul>
 *     <li>The pre-flight (OPTIONS) response DOES contain {@code Access-Control-Allow-Origin},
 *     {@code Access-Control-Allow-Methods} and {@code Access-Control-Allow-Headers}, and that the
 *     allow-headers value contains {@code X-BrowserSessionID}.</li>
 *     <li>A normal (non pre-flight) request does NOT contain the pre-flight only headers
 *     {@code Access-Control-Allow-Methods} and {@code Access-Control-Allow-Headers}.</li>
 * </ul>
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class CORSHeadersForAllAPITypesTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(CORSHeadersForAllAPITypesTestCase.class);

    private static final String API_VERSION = "1.0.0";
    private static final String ORIGIN = "http://localhost";
    private static final String BROWSER_SESSION_ID_HEADER = "X-BrowserSessionID";

    private static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_ORIGIN_VALUE = "*";
    private static final String ACCESS_CONTROL_ALLOW_METHODS_HEADER = "Access-Control-Allow-Methods";
    private static final String ACCESS_CONTROL_ALLOW_HEADERS_HEADER = "Access-Control-Allow-Headers";

    private static final int HTTP_TIMEOUT_MILLIS = 30000;

    private final List<String> createdApiIds = new ArrayList<>();

    @Factory(dataProvider = "userModeDataProvider")
    public CORSHeadersForAllAPITypesTestCase(TestUserMode userMode) {
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
    }

    @Test(groups = {"wso2.am"}, description = "Verify CORS pre-flight and normal request headers for a REST API")
    public void testCorsHeadersForRestApi() throws Exception {
        String apiName = "CORSRestAPI";
        String apiContext = "corsRestApi";

        APIRequest apiRequest = new APIRequest(apiName, apiContext,
                new URL("http://localhost:8080/customerservice/"));
        apiRequest.setVersion(API_VERSION);
        apiRequest.setProvider(user.getUserName());
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);

        APIOperationsDTO getOperation = new APIOperationsDTO();
        getOperation.setVerb(APIMIntegrationConstants.HTTP_VERB_GET);
        getOperation.setTarget("/customers/{id}");
        getOperation.setAuthType(APIMIntegrationConstants.RESOURCE_AUTH_TYPE_APPLICATION_AND_APPLICATION_USER);
        getOperation.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED);
        apiRequest.setOperationsDTOS(Collections.singletonList(getOperation));

        String apiId = restAPIPublisher.addAPI(apiRequest).getData();
        createdApiIds.add(apiId);

        enableCorsWithBrowserSessionHeader(apiId);
        deployAndPublish(apiId, apiName, API_VERSION);

        verifyCorsPreflightAndNormalRequest(
                getAPIInvocationURLHttps(apiContext, API_VERSION) + "/customers/123", "REST API", "GET");
    }

    @Test(groups = {"wso2.am"},
            description = "Verify CORS pre-flight and normal request headers for a SOAP service exposed as a REST API")
    public void testCorsHeadersForSoapToRestApi() throws Exception {
        String apiName = "CORSSoapToRestAPI";
        String apiContext = "corsSoapToRestApi";

        String wsdlDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(
                        "artifacts" + File.separator + "AM" + File.separator + "soap" + File.separator
                                + "phoneverify.wsdl"), StandardCharsets.UTF_8);
        File wsdlFile = getTempFileWithContent(wsdlDefinition, ".wsdl");
        restAPIPublisher.validateWsdlDefinition(null, wsdlFile);

        JSONObject endpointObject = new JSONObject();
        endpointObject.put("type", "address");
        endpointObject.put("url", "http://localhost:8080/phoneverify");

        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "address");
        endpointConfig.put("sandbox_endpoints", endpointObject);
        endpointConfig.put("production_endpoints", endpointObject);

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("name", apiName);
        additionalPropertiesObj.put("context", apiContext);
        additionalPropertiesObj.put("version", API_VERSION);
        additionalPropertiesObj.put("endpointConfig", endpointConfig);
        additionalPropertiesObj.put("policies", Collections.singletonList(APIMIntegrationConstants.API_TIER.UNLIMITED));

        APIDTO apidto = restAPIPublisher.importWSDLSchemaDefinition(wsdlFile, null,
                additionalPropertiesObj.toString(), "SOAPTOREST");
        String apiId = apidto.getId();
        createdApiIds.add(apiId);

        enableCorsWithBrowserSessionHeader(apiId);
        deployAndPublish(apiId, apiName, API_VERSION);

        verifyCorsPreflightAndNormalRequest(
                getAPIInvocationURLHttps(apiContext, API_VERSION) + "/checkPhoneNumber", "SOAP to REST API", "POST");
    }

    @Test(groups = {"wso2.am"}, description = "Verify CORS pre-flight and normal request headers for a GraphQL API")
    public void testCorsHeadersForGraphQLApi() throws Exception {
        String apiName = "CORSGraphQLAPI";
        String apiContext = "corsGraphQLApi";

        String schemaDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("graphql" + File.separator + "schema.graphql"),
                StandardCharsets.UTF_8);
        File schemaFile = getTempFileWithContent(schemaDefinition, ".graphql");

        GraphQLValidationResponseDTO validationResponse =
                restAPIPublisher.validateGraphqlSchemaDefinition(schemaFile);
        GraphQLValidationResponseGraphQLInfoDTO graphQLInfo = validationResponse.getGraphQLInfo();
        JSONArray operations = new JSONArray(new ObjectMapper().writeValueAsString(graphQLInfo.getOperations()));

        JSONObject endpointUrl = new JSONObject();
        endpointUrl.put("url", "http://localhost:8080/graphql");
        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("sandbox_endpoints", endpointUrl);
        endpointConfig.put("production_endpoints", endpointUrl);

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("name", apiName);
        additionalPropertiesObj.put("context", apiContext);
        additionalPropertiesObj.put("version", API_VERSION);
        additionalPropertiesObj.put("endpointConfig", endpointConfig);
        additionalPropertiesObj.put("policies", Collections.singletonList(APIMIntegrationConstants.API_TIER.UNLIMITED));
        additionalPropertiesObj.put("operations", operations);

        APIDTO apidto = restAPIPublisher.importGraphqlSchemaDefinition(schemaFile, additionalPropertiesObj.toString());
        String apiId = apidto.getId();
        createdApiIds.add(apiId);

        enableCorsWithBrowserSessionHeader(apiId);
        deployAndPublish(apiId, apiName, API_VERSION);

        verifyCorsPreflightAndNormalRequest(
                getAPIInvocationURLHttps(apiContext, API_VERSION), "GraphQL API", "POST");
    }

    @Test(groups = {"wso2.am"},
            description = "Verify CORS pre-flight and normal request headers for a Server Sent Events (SSE) API")
    public void testCorsHeadersForServerSentEventsApi() throws Exception {
        String apiName = "CORSSseAPI";
        String apiContext = "corsSseApi";

        URI endpointUri = new URI("http://localhost:8080");
        APIRequest apiRequest = new APIRequest(apiName, apiContext, endpointUri, endpointUri);
        apiRequest.setVersion(API_VERSION);
        apiRequest.setProvider(user.getUserName());
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.ASYNC_UNLIMITED);
        apiRequest.setType("SSE");

        String apiId = restAPIPublisher.addAPI(apiRequest).getData();
        createdApiIds.add(apiId);

        enableCorsWithBrowserSessionHeader(apiId);
        deployAndPublish(apiId, apiName, API_VERSION);

        verifyCorsPreflightAndNormalRequest(
                getAPIInvocationURLHttps(apiContext, API_VERSION), "Server Sent Events API", "GET");
    }

    @Test(groups = {"wso2.am"},
            description = "Verify CORS pre-flight and normal request headers for an Async API imported from a file")
    public void testCorsHeadersForAsyncApiFromFile() throws Exception {
        String apiName = "CORSAsyncFileAPI";
        String apiContext = "corsAsyncFileApi";

        String asyncApiDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("async" + File.separator + "cors-asyncapi.yaml"),
                StandardCharsets.UTF_8);
        File asyncApiFile = getTempFileWithContent(asyncApiDefinition, ".yaml");

        JSONObject endpointUrl = new JSONObject();
        endpointUrl.put("url", "http://localhost:8080");
        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("sandbox_endpoints", endpointUrl);
        endpointConfig.put("production_endpoints", endpointUrl);

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("name", apiName);
        additionalPropertiesObj.put("context", apiContext);
        additionalPropertiesObj.put("version", API_VERSION);
        additionalPropertiesObj.put("type", "SSE");
        additionalPropertiesObj.put("endpointConfig", endpointConfig);
        additionalPropertiesObj.put("policies",
                Collections.singletonList(APIMIntegrationConstants.API_TIER.ASYNC_UNLIMITED));

        ApiResponse<APIDTO> response =
                restAPIPublisher.importAsyncAPIDefinition(asyncApiFile, additionalPropertiesObj.toString());
        assertEquals(response.getStatusCode(), HttpStatus.SC_CREATED,
                "Async API creation from file failed.");
        String apiId = response.getData().getId();
        createdApiIds.add(apiId);

        enableCorsWithBrowserSessionHeader(apiId);
        deployAndPublish(apiId, apiName, API_VERSION);

        verifyCorsPreflightAndNormalRequest(
                getAPIInvocationURLHttps(apiContext, API_VERSION), "Async API from file", "GET");
    }

    /**
     * Enables the CORS configuration for the given API with {@code X-BrowserSessionID} added as an
     * access-control-allow header.
     *
     * @param apiId ID of the API to update.
     * @throws ApiException if the update request fails.
     */
    private void enableCorsWithBrowserSessionHeader(String apiId) throws ApiException {
        APIDTO apidto = restAPIPublisher.getAPIByID(apiId);

        APICorsConfigurationDTO corsConfiguration = new APICorsConfigurationDTO();
        corsConfiguration.setCorsConfigurationEnabled(true);
        corsConfiguration.setAccessControlAllowCredentials(false);
        corsConfiguration.setAccessControlAllowOrigins(
                Collections.singletonList(ACCESS_CONTROL_ALLOW_ORIGIN_VALUE));
        corsConfiguration.setAccessControlAllowHeaders(Arrays.asList(
                "authorization", "Access-Control-Allow-Origin", "Content-Type", "SOAPAction",
                BROWSER_SESSION_ID_HEADER, "Authorization", "ApiKey"));
        corsConfiguration.setAccessControlAllowMethods(Arrays.asList("GET", "PUT", "POST", "DELETE", "PATCH"));

        apidto.setCorsConfiguration(corsConfiguration);
        restAPIPublisher.updateAPI(apidto, apiId);
    }

    /**
     * Creates a revision, deploys it to the gateway, publishes the API and waits for the deployment
     * to be synced.
     */
    private void deployAndPublish(String apiId, String apiName, String apiVersion) throws Exception {
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatus(apiId, Constants.PUBLISHED);
        waitForAPIDeploymentSync(user.getUserName(), apiName, apiVersion, APIMIntegrationConstants.IS_API_EXISTS);
    }

    /**
     * Sends a pre-flight (OPTIONS) request and a normal request to the given invocation URL and
     * asserts the CORS header expectations described in the class level Javadoc.
     *
     * @param invocationUrl gateway invocation URL of the API.
     * @param apiLabel      human readable API type label used in assertion messages.
     * @param requestMethod the HTTP method the browser intends to use (sent as
     *                      {@code Access-Control-Request-Method}).
     */
    private void verifyCorsPreflightAndNormalRequest(String invocationUrl, String apiLabel, String requestMethod)
            throws Exception {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(HTTP_TIMEOUT_MILLIS)
                .setConnectionRequestTimeout(HTTP_TIMEOUT_MILLIS)
                .setSocketTimeout(HTTP_TIMEOUT_MILLIS)
                .build();

        // --- Pre-flight (OPTIONS) request: MUST contain the CORS headers ---
        HttpOptions preflightRequest = new HttpOptions(invocationUrl);
        preflightRequest.addHeader("Origin", ORIGIN);
        preflightRequest.addHeader("Access-Control-Request-Method", requestMethod);
        preflightRequest.addHeader("Access-Control-Request-Headers", "authorization, " + BROWSER_SESSION_ID_HEADER);

        Header[] preflightHeaders;
        int preflightStatusCode;
        try (CloseableHttpClient preflightClient =
                     HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
             CloseableHttpResponse preflightResponse = preflightClient.execute(preflightRequest)) {
            preflightHeaders = preflightResponse.getAllHeaders();
            preflightStatusCode = preflightResponse.getStatusLine().getStatusCode();
            EntityUtils.consume(preflightResponse.getEntity());
        }
        logHeaders(apiLabel + " [pre-flight OPTIONS]", preflightHeaders);

        assertEquals(preflightStatusCode, HttpStatus.SC_OK,
                apiLabel + ": Pre-flight response code mismatch.");

        Header allowOrigin = pickHeader(preflightHeaders, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER);
        assertNotNull(allowOrigin, apiLabel + ": " + ACCESS_CONTROL_ALLOW_ORIGIN_HEADER
                + " header is missing in the pre-flight response.");
        assertEquals(allowOrigin.getValue(), ACCESS_CONTROL_ALLOW_ORIGIN_VALUE,
                apiLabel + ": " + ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header value mismatch.");

        assertNotNull(pickHeader(preflightHeaders, ACCESS_CONTROL_ALLOW_METHODS_HEADER),
                apiLabel + ": " + ACCESS_CONTROL_ALLOW_METHODS_HEADER
                        + " header is missing in the pre-flight response.");

        Header allowHeaders = pickHeader(preflightHeaders, ACCESS_CONTROL_ALLOW_HEADERS_HEADER);
        assertNotNull(allowHeaders, apiLabel + ": " + ACCESS_CONTROL_ALLOW_HEADERS_HEADER
                + " header is missing in the pre-flight response.");
        assertTrue(allowHeaders.getValue().toLowerCase().contains(BROWSER_SESSION_ID_HEADER.toLowerCase()),
                apiLabel + ": " + ACCESS_CONTROL_ALLOW_HEADERS_HEADER + " header does not contain "
                        + BROWSER_SESSION_ID_HEADER + ". Actual value: " + allowHeaders.getValue());

        // --- Normal (non pre-flight) request: MUST NOT contain the pre-flight only CORS headers ---
        // The normal request uses the same HTTP method the browser intends to use so the negative
        // assertion exercises the actual resource rather than always falling back to GET.
        HttpUriRequest normalRequest = buildRequest(requestMethod, invocationUrl);
        normalRequest.addHeader("Origin", ORIGIN);

        Header[] normalHeaders;
        try (CloseableHttpClient normalClient =
                     HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
             CloseableHttpResponse normalResponse = normalClient.execute(normalRequest)) {
            normalHeaders = normalResponse.getAllHeaders();
            EntityUtils.consume(normalResponse.getEntity());
        }
        logHeaders(apiLabel + " [normal " + requestMethod + "]", normalHeaders);

        assertNull(pickHeader(normalHeaders, ACCESS_CONTROL_ALLOW_METHODS_HEADER),
                apiLabel + ": " + ACCESS_CONTROL_ALLOW_METHODS_HEADER
                        + " header should NOT be present in a normal (non pre-flight) response.");
        assertNull(pickHeader(normalHeaders, ACCESS_CONTROL_ALLOW_HEADERS_HEADER),
                apiLabel + ": " + ACCESS_CONTROL_ALLOW_HEADERS_HEADER
                        + " header should NOT be present in a normal (non pre-flight) response.");
    }

    /**
     * Builds an {@link HttpUriRequest} for the given HTTP method against the given URL.
     */
    private HttpUriRequest buildRequest(String method, String url) {
        switch (method.toUpperCase()) {
            case "POST":
                return new HttpPost(url);
            case "PUT":
                return new HttpPut(url);
            case "DELETE":
                return new HttpDelete(url);
            case "PATCH":
                return new HttpPatch(url);
            case "GET":
            default:
                return new HttpGet(url);
        }
    }

    private void logHeaders(String label, Header[] headers) {
        log.info("---- Response headers for " + label + " ----");
        for (Header header : headers) {
            log.info(header.getName() + " : " + header.getValue());
        }
    }

    private File getTempFileWithContent(String content, String suffix) throws Exception {
        File temp = File.createTempFile("cors", suffix);
        temp.deleteOnExit();
        try (BufferedWriter out = new BufferedWriter(new FileWriter(temp))) {
            out.write(content);
        }
        return temp;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        for (String apiId : createdApiIds) {
            try {
                undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
                restAPIPublisher.deleteAPI(apiId);
            } catch (Exception e) {
                log.warn("Error while cleaning up API with ID: " + apiId, e);
            }
        }
        super.cleanUp();
    }
}
