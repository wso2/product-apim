/*
 *
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.wso2.am.integration.tests.publisher;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIKeyDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;

/**
 * Create an API through the Publisher Rest API and validate the API
 * APIM2-18 / APIM2-538
 */
public class APIM18CreateAnAPIThroughThePublisherRestAPITestCase extends APIMIntegrationBaseTest {
    private final String apiNameTest = "APIM18PublisherTest";
    private final String apiVersion = "1.0.0";
    private String apiProviderName;
    private String apiProductionEndPointUrl;
    private String apiId;
    private String apiId2;

    @Factory(dataProvider = "userModeDataProvider")
    public APIM18CreateAnAPIThroughThePublisherRestAPITestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
//                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        String apiProductionEndpointPostfixUrl = "jaxrs_basic/services/customers/" +
                "customerservice/customers/123";

        String publisherURLHttp = publisherUrls.getWebAppURLHttp();


        apiProductionEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() +
                apiProductionEndpointPostfixUrl;
        apiProviderName = publisherContext.getContextTenant().getContextUser().getUserName();

    }

    @Test(groups = {"wso2.am"}, description = "Create an API Through the Publisher Rest API")
    public void testCreateAnAPIThroughThePublisherRest() throws Exception {

        String apiContextTest = "apim18PublisherTestAPI";
        String apiDescription = "This is Test API Created by API Manager Integration Test";
        String apiTag = "tag18-1, tag18-2, tag18-3";
        
        APIRequest apiCreationRequestBean;
        apiCreationRequestBean = new APIRequest(apiNameTest, apiContextTest, new URL(apiProductionEndPointUrl));

        apiCreationRequestBean.setVersion(apiVersion);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setTiersCollection("Gold,Bronze");
        apiCreationRequestBean.setTier("Gold");

        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget("/customers/{id}");

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO);
        apiCreationRequestBean.setOperationsDTOS(operationsDTOS);
        apiCreationRequestBean.setOperationsDTOS(operationsDTOS);
        apiCreationRequestBean.setDefault_version_checked("true");;

        apiCreationRequestBean.setBusinessOwner("api18b");
        apiCreationRequestBean.setBusinessOwnerEmail("api18b@ee.com");
        apiCreationRequestBean.setTechnicalOwner("api18t");
        apiCreationRequestBean.setTechnicalOwnerEmail("api18t@ww.com");

        apiCreationRequestBean.setOperationsDTOS(operationsDTOS);
        HttpResponse apiCreationResponse = restAPIPublisher.addAPI(apiCreationRequestBean);
        apiId = apiCreationResponse.getData();

        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Response Code miss matched when creating the API");


        //Check the availability of an API in Publisher
        HttpResponse response = restAPIPublisher.getAPI(apiId);
        assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when creating the API");
        assertTrue(response.getData().contains(apiNameTest), "Invalid API Name");
        assertTrue(response.getData().contains(apiVersion), "Invalid API Version");
        assertTrue(response.getData().contains(apiContextTest), "Invalid API Context");
        assertTrue(response.getData().contains("lastUpdatedTimestamp"), "Last Updated Timestamp is not available");
    }

    @Test(groups = {
            "wso2.am" }, description = "Create an API Through the Publisher Rest API with malformed context")
    public void testCreateAnAPIWithMalformedContextThroughThePublisherRest()
            throws Exception {

        // Now APIs with malformed context should not be allowed to create
        String apiContextTest = "apim18PublisherTestAPIMalformed`";
        String apiDescription = "This is Test API Created by API Manager Integration Test";
        String apiTag = "tag18-4, tag18-5, tag18-6";
        String apiName = "APIM18PublisherTestMalformed";

        APIRequest apiCreationRequestBean;
        apiCreationRequestBean = new APIRequest(apiName, apiContextTest, new URL(apiProductionEndPointUrl));

        apiCreationRequestBean.setVersion(apiVersion);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setTier(APIMIntegrationConstants.API_TIER.GOLD);

        HttpResponse response = restAPIPublisher.addAPIWithMalformedContext(apiCreationRequestBean);
        Assert.assertNotNull(response, "Response cannot be null");
        Assert.assertEquals(response.getResponseCode(), Response.Status.BAD_REQUEST.getStatusCode(), "Response Code miss matched when creating the API");
    }

    @Test(groups = {"wso2.am"}, description = "Remove an API Through the Publisher Rest API",
            dependsOnMethods = "testCreateAnAPIThroughThePublisherRest")
    public void testRemoveAnAPIThroughThePublisherRest() throws Exception {

        //Remove an API and validate the Response
        HttpResponse removeApiResponse = restAPIPublisher.deleteAPI(apiId);
        assertEquals(removeApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when removing an API");

        //Check the availability of an API  after removing
        
        HttpResponse response = restAPIPublisher.getAPI(apiId);
        assertEquals(response.getResponseCode(), Response.Status.NOT_FOUND.getStatusCode(),
                "Status code mismatch");

    }

    @Test(groups = {"wso2.am"}, description = "Import swagger definitions and Create two APIs with same context")
    public void testImportSwaggerAndCreateAPIWithSameContext() throws Exception {
        JSONObject apiProperties;
        String swaggerPath1 = getAMResourceLocation() + File.separator + "swagger" +
                File.separator + "customer-info-api.yaml";
        String swaggerPath2 = getAMResourceLocation() + File.separator + "swagger" +
                File.separator + "leasing-api.yaml";

        try {
            File definition = new File(swaggerPath1);
            apiProperties = getAPIDetails("CustomerInfoAPI");
            APIDTO apidto1Response = restAPIPublisher.
                    importOASDefinition(definition, apiProperties.toString());
            Assert.assertNotNull(apidto1Response);

            //Create another API with same context
            File definition2 = new File(swaggerPath2);
            apiProperties = getAPIDetails("LeasingAPI");
            restAPIPublisher.importOASDefinition(definition2, apiProperties.toString());
            Assert.fail("API created with same context");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 409);
            Assert.assertTrue(e.getResponseBody().contains("The API context already exists"));
        }
    }

    @Test(groups = {"wso2.am"}, description = "Create APIs with archives with master swagger containing remote references")
    public void testCreateApiWithArchivesWithRemoteReferences() throws Exception {
        String swaggerPath = getAMResourceLocation() + File.separator + "swagger" + File.separator +
                "swagger-archive.zip";
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
        apiProperties.put("name", "TestAPIWithRemoteReferences");
        apiProperties.put("context", "/TestAPIWithRemoteReferences");
        apiProperties.put("version", "1.0.0");
        apiProperties.put("provider", user.getUserName());
        apiProperties.put("endpointConfig", endpointConfig);
        apiProperties.put("policies", tierList);

        APIDTO apidtoResponse = restAPIPublisher.
                importOASDefinition(definition, apiProperties.toString());
        Assert.assertNotNull(apidtoResponse);
        apiId2 = apidtoResponse.getId();

        String retrievedSwagger = restAPIPublisher.getSwaggerByID(apiId2);

        validateRemoteReference(retrievedSwagger);
    }

    @Test(groups = {"wso2.am"}, description = "Create APIs with only Sandbox Endpoints")
    public void testCreateApiWithOnlySandboxEndpoints() throws Exception {
        String apiContextTest = "publisherTestAPIWithNoProductEndpoint";
        String apiNameNoProdEndpointTest = "APIM18PublisherTestNoProduction";
        String apiDescription = "This is Test API Created by API Manager Integration Test. This has no production endpoints";
        String apiTag = "tag18-1, tag18-2, tag18-3";

        APIRequest apiCreationRequestBean;
        apiCreationRequestBean = new APIRequest(apiNameNoProdEndpointTest, apiContextTest, false, new URL(apiProductionEndPointUrl));

        apiCreationRequestBean.setVersion(apiVersion);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setTiersCollection("Gold,Bronze");
        apiCreationRequestBean.setTier("Gold");

        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget("/customers/{id}");

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO);

        apiCreationRequestBean.setOperationsDTOS(operationsDTOS);
        apiCreationRequestBean.setOperationsDTOS(operationsDTOS);
        apiCreationRequestBean.setDefault_version_checked("true");;
        apiCreationRequestBean.setBusinessOwner("api18b");
        apiCreationRequestBean.setBusinessOwnerEmail("api18b@ee.com");
        apiCreationRequestBean.setTechnicalOwner("api18t");
        apiCreationRequestBean.setTechnicalOwnerEmail("api18t@ww.com");
        apiCreationRequestBean.setOperationsDTOS(operationsDTOS);

        HttpResponse apiCreationResponse = restAPIPublisher.addAPI(apiCreationRequestBean);
        apiId = apiCreationResponse.getData();

        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Response Code miss matched when creating the API");

        //Check the availability of an API in Publisher
        HttpResponse response = restAPIPublisher.getAPI(apiId);
        assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when creating the API");
        assertTrue(response.getData().contains(apiNameNoProdEndpointTest), "Invalid API Name");
        assertTrue(response.getData().contains(apiVersion), "Invalid API Version");
        assertTrue(response.getData().contains(apiContextTest), "Invalid API Context");

        ApiResponse<APIKeyDTO> apiKeyDTO = restAPIPublisher.generateInternalApiKey(apiId);
        assertEquals(200, apiKeyDTO.getStatusCode(),
                "Key generation is failed for APIs which don't have product endpoints");
        String apiKey = apiKeyDTO.getData().getApikey();
        assertNotNull(apiKey, "API Key is null");
        String[] split_string = apiKey.split("\\.");
        String base64EncodedBody = split_string[1];
        Base64 base64Url = new Base64(true);
        String body = new String(base64Url.decode(base64EncodedBody));
        JSONObject keyBody = new JSONObject(body);
        String keyType = keyBody.getString("keytype");
        assertEquals("SANDBOX", keyType, "API Key is not type SANDBOX");
    }

    @Test(groups = {"wso2.am"}, description = "Create APIs with archives with a random master swagger file name")
    public void testCreateApiWithArchivesWithRemoteReferencesWithIncorrectSwagger() throws Exception {
        String swaggerPath = getAMResourceLocation() + File.separator + "swagger" + File.separator +
                "incorrect-swagger-archive.zip";
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
        apiProperties.put("name", "TestAPIWithRemoteReferencesWithIncorrectSwaggerName");
        apiProperties.put("context", "/TestAPIWithRemoteReferencesWithIncorrectSwaggerName");
        apiProperties.put("version", "1.0.0");
        apiProperties.put("provider", user.getUserName());
        apiProperties.put("endpointConfig", endpointConfig);
        apiProperties.put("policies", tierList);

        try {
            restAPIPublisher.importOASDefinition(definition, apiProperties.toString());
            Assert.fail("API imported successfully with invalid swagger name");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 500);
            Assert.assertTrue(e.getResponseBody().contains("validating API Definition"));
        }
    }

    private JSONObject getAPIDetails(String apiName) throws JSONException {

        JSONObject endpoints = new JSONObject();
        endpoints.put("url", getBackendEndServiceEndPointHttp("wildcard/resources"));

        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("production_endpoints", endpoints);
        endpointConfig.put("sandbox_endpoints", endpoints);

        JSONObject apiProperties = new JSONObject();
        apiProperties.put("name", apiName);
        apiProperties.put("context", "/" + "SwaggerAPI1");
        apiProperties.put("version", "1.0.0");
        apiProperties.put("provider", apiProviderName);
        apiProperties.put("endpointConfig", endpointConfig);

        return apiProperties;

    }

    private void validateRemoteReference(String swaggerContent) {
        OpenAPIParser parser = new OpenAPIParser();
        SwaggerParseResult swaggerParseResult = parser.readContents(swaggerContent, null, null);
        OpenAPI openAPI = swaggerParseResult.getOpenAPI();
        boolean isRemoteReferenceAvailable = false;
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
        for (Map.Entry<String, Schema> schema : schemas.entrySet()) {
            if (schema.getKey().equalsIgnoreCase("dataSetList")) {
                isRemoteReferenceAvailable = true;
            }
        }
        Assert.assertTrue(isRemoteReferenceAvailable, "Remote reference is not available in the schema list");
    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        restAPIPublisher.deleteAPI(apiId2);
        super.cleanUp();
    }

}


