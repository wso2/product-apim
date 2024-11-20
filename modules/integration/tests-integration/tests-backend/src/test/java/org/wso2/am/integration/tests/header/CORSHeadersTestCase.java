/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.am.integration.tests.header;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APICorsConfigurationDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Test CORS functionality
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.ALL })
public class CORSHeadersTestCase extends APIManagerLifecycleBaseTest {

    private static final String API_NAME = "CorsHeadersTestAPI";
    private static final String APPLICATION_NAME = "CorsHeadersApp";
    private static final String API_CONTEXT = "corsHeadersTestAPI";
    private static final String API_VERSION = "1.0.0";
    private static final String TAGS = "cors, test";
    private static final String DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";

    private static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER_VALUE = "*";
    private static final String ACCESS_CONTROL_ALLOW_METHODS_HEADER = "Access-Control-Allow-Methods";
    private static final String ACCESS_CONTROL_ALLOW_METHODS_HEADER_VALUE = "DELETE,POST,PUT,PATCH,GET";
    private static final String ACCESS_CONTROL_ALLOW_HEADERS_HEADER = "Access-Control-Allow-Headers";
    private static final String ACCESS_CONTROL_ALLOW_HEADERS_HEADER_VALUE
            = "authorization,Access-Control-Allow-Origin,Content-Type,SOAPAction,Authorization,ApiKey";
    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER = "Access-Control-Allow-Credentials";

    private String accessToken;
    private String applicationId;
    private String apiId;
    private String apiId2;
    private String apiId3;
    private ArrayList<String> grantTypes;
    private Map<String, String> requestHeaders;
    private String apiEndPointUrl;

    Log log = LogFactory.getLog(CORSHeadersTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttps() + API_END_POINT_POSTFIX_URL;
        String providerName = user.getUserName();
        APIRequest apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl), true);
        apiRequest.setTags(TAGS);
        apiRequest.setDescription(DESCRIPTION);
        apiRequest.setTiersCollection(TIER_UNLIMITED);
        apiRequest.setProvider(providerName);
        //Add api resource
        APIOperationsDTO apiOperationsDTO1 = new APIOperationsDTO();
        apiOperationsDTO1.setVerb(APIMIntegrationConstants.HTTP_VERB_GET);
        apiOperationsDTO1.setTarget("/customers/{id}");
        apiOperationsDTO1.setAuthType(APIMIntegrationConstants.RESOURCE_AUTH_TYPE_APPLICATION_AND_APPLICATION_USER);
        apiOperationsDTO1.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED);

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO1);
        apiRequest.setOperationsDTOS(operationsDTOS);

        //Create application
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse applicationResponse =
                restAPIStore.createApplication(APPLICATION_NAME,
                        APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN,
                        APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                        ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();

        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);

        //get access token
        grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        accessToken = applicationKeyDTO.getToken().getAccessToken();

        // Create requestHeaders
        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + applicationKeyDTO.getToken().getAccessToken());

        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);
    }

    @Test(groups = {"wso2.am"}, description = "Checking CORS headers in pre-flight response")
    public void CheckCORSHeadersInPreFlightResponse() throws Exception {
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpUriRequest option = new HttpOptions(getAPIInvocationURLHttps(API_CONTEXT, API_VERSION) + "/customers/123");
        option.addHeader("Origin", "http://localhost");
        option.addHeader("Access-Control-Request-Method", "GET");
        HttpResponse response = httpclient.execute(option);

       assertEquals(response.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatch.");

        Header[] responseHeaders = response.getAllHeaders();

        log.info("Response Headers: CheckCORSHeadersInPreFlightResponse");
        for (Header header : responseHeaders) {
            log.info(header.getName() + " : " + header.getValue());
        }

        Header header = pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER);
        assertNotNull(header, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header is not available in the response.");
        assertEquals(header.getValue(), ACCESS_CONTROL_ALLOW_ORIGIN_HEADER_VALUE,
                     ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header value mismatch.");

        header = pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_METHODS_HEADER);
        assertNotNull(header, ACCESS_CONTROL_ALLOW_METHODS_HEADER + " header is not available in the response.");
        assertTrue(ACCESS_CONTROL_ALLOW_METHODS_HEADER_VALUE.contains(header.getValue()),
                header.getValue() + " header value mismatch.");

        header = pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_HEADERS_HEADER);
        assertNotNull(header, ACCESS_CONTROL_ALLOW_HEADERS_HEADER + " header is not available in the response.");
        assertEquals(header.getValue(), ACCESS_CONTROL_ALLOW_HEADERS_HEADER_VALUE,
                     ACCESS_CONTROL_ALLOW_HEADERS_HEADER + " header value mismatch.");

        assertNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER),
                    ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER + " header is available in the response, " +
                    "but it should not be.");
    }

    @Test(groups = {"wso2.am"}, description = "Checking CORS headers in response",
            dependsOnMethods = "CheckCORSHeadersInPreFlightResponse")
    public void CheckCORSHeadersInResponse() throws Exception {
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttps(API_CONTEXT, API_VERSION) + "/customers/123");
        get.addHeader("Origin", "http://localhost");
        get.addHeader("Authorization", "Bearer " + accessToken);

        HttpResponse response = httpclient.execute(get);
        assertEquals(response.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatch.");

        Header[] responseHeaders = response.getAllHeaders();

        log.info("Response Headers: CheckCORSHeadersInResponse");
        for (Header header : responseHeaders) {
            log.info(header.getName() + " : " + header.getValue());
        }

        Header header = pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER);
        assertNotNull(header, ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header is not available in the response.");
        assertEquals(header.getValue(), ACCESS_CONTROL_ALLOW_ORIGIN_HEADER_VALUE,
                     ACCESS_CONTROL_ALLOW_ORIGIN_HEADER + " header value mismatch.");

        header = pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_METHODS_HEADER);
        assertNotNull(header, ACCESS_CONTROL_ALLOW_METHODS_HEADER + " header is not available in the response.");
             assertTrue(ACCESS_CONTROL_ALLOW_METHODS_HEADER_VALUE.contains(header.getValue()),
                     ACCESS_CONTROL_ALLOW_METHODS_HEADER + " header value mismatch.");

        header = pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_HEADERS_HEADER);
        assertNotNull(header, ACCESS_CONTROL_ALLOW_HEADERS_HEADER + " header is not available in the response.");
        assertEquals(header.getValue(), ACCESS_CONTROL_ALLOW_HEADERS_HEADER_VALUE,
                     ACCESS_CONTROL_ALLOW_HEADERS_HEADER + " header value mismatch.");

        assertNull(pickHeader(responseHeaders, ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER),
                   ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER + " header is available in the response, " +
                   "but it should not be.");
    }

    @Test(groups = {"wso2.am"}, description = "Enable CORS and verify ", dependsOnMethods = "CheckCORSHeadersInResponse")
    public void AddCORSHeadersToAPIAndVerify() throws Exception {

        APIRequest apiRequest = new APIRequest("CORSAPI", "corsAPI", new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION);
        apiRequest.setProvider(user.getUserName());

        org.wso2.carbon.automation.test.utils.http.client.HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiId2 = serviceResponse.getData();

        APICorsConfigurationDTO apiCorsConfigurationDTO = new APICorsConfigurationDTO();
        List<String> accessControlAllowHeaders = new ArrayList<String>();
        accessControlAllowHeaders.add("Access-Control-Allow-Origin");
        accessControlAllowHeaders.add("Content-Type");

        List<String> accessControlAllowMethods = new ArrayList<String>();
        accessControlAllowMethods.add("GET");
        accessControlAllowMethods.add("PUT");
        accessControlAllowMethods.add("POST");

        List<String> accessControlAllowOrigins = new ArrayList<String>();
        accessControlAllowOrigins.add("*");

        apiCorsConfigurationDTO.setCorsConfigurationEnabled(true);
        apiCorsConfigurationDTO.setAccessControlAllowHeaders(accessControlAllowHeaders);
        apiCorsConfigurationDTO.setAccessControlAllowMethods(accessControlAllowMethods);
        apiCorsConfigurationDTO.setAccessControlAllowOrigins(accessControlAllowOrigins);

        APIDTO apidto = restAPIPublisher.getAPIByID(apiId2);
        apidto.setCorsConfiguration(apiCorsConfigurationDTO);
        APIDTO updatedApidto = restAPIPublisher.updateAPI(apidto, apiId2);

        APICorsConfigurationDTO apiCorsConfigurationDTO1 = updatedApidto.getCorsConfiguration();
        Assert.assertTrue(apiCorsConfigurationDTO1.isCorsConfigurationEnabled(), "CORS is not enabled");

        String retrievedSwagger = restAPIPublisher.getSwaggerByID(apiId2);
        retrievedSwagger = restAPIPublisher.updateSwagger(apiId2, retrievedSwagger);
        Assert.assertNotNull(retrievedSwagger);

        OpenAPIParser parser = new OpenAPIParser();
        SwaggerParseResult swaggerParseResult = parser.readContents(retrievedSwagger, null, null);
        OpenAPI openAPI = swaggerParseResult.getOpenAPI();
        Assert.assertNotNull(openAPI.getExtensions().get("x-wso2-cors"));

        HashMap<String, Object> corsDetails = (HashMap<String, Object>) openAPI.getExtensions().get("x-wso2-cors");
        for (Map.Entry<String, Object> entry : corsDetails.entrySet()) {
            if (entry.getKey().equalsIgnoreCase("corsConfigurationEnabled")) {
                Assert.assertTrue((Boolean) entry.getValue(), "CORS not enabled");
            }
        }
    }

    @Test(groups = {"wso2.am"}, description = "Enable CORS through swagger and verify", dependsOnMethods = "AddCORSHeadersToAPIAndVerify")
    public void CreateAPIWithSwaggerwithCORSHeadersAndVerify() throws Exception {

        //create a REST API
        String swaggerPath = getAMResourceLocation() + File.separator + "configFiles" + File.separator + "cors"
                + File.separator + "CORSApi.yaml";
        File definition = new File(swaggerPath);
        JSONObject endpoints = new JSONObject();
        endpoints.put("url", "https://test.com");

        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("production_endpoints", endpoints);
        endpointConfig.put("sandbox_endpoints", endpoints);

        List<String> tierList = new ArrayList<>();
        tierList.add(APIMIntegrationConstants.API_TIER.SILVER);
        tierList.add(APIMIntegrationConstants.API_TIER.GOLD);

        JSONObject apiProperties = new JSONObject();
        apiProperties.put("name", "ImportedCORSAPI");
        apiProperties.put("context", "/importedCORSAPI");
        apiProperties.put("version", "1.0.0");
        apiProperties.put("provider", user.getUserName());
        apiProperties.put("endpointConfig", endpointConfig);
        apiProperties.put("policies", tierList);

        APIDTO restAPIDTO = restAPIPublisher.importOASDefinition(definition, apiProperties.toString());
        apiId3 = restAPIDTO.getId();

        APIDTO apidto = restAPIPublisher.getAPIByID(apiId3);
        APICorsConfigurationDTO apiCorsConfigurationDTO = apidto.getCorsConfiguration();
        Assert.assertTrue(apiCorsConfigurationDTO.isCorsConfigurationEnabled(), "CORS has not been enabled");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        SubscriptionListDTO subsDTO = restAPIStore.getAllSubscriptionsOfApplication(applicationId);
        for (SubscriptionDTO subscriptionDTO: subsDTO.getList()){
            restAPIStore.removeSubscription(subscriptionDTO);
        }
        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);

        super.cleanUp();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN },
        };
    }

    @Factory(dataProvider = "userModeDataProvider")
    public CORSHeadersTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }
}
