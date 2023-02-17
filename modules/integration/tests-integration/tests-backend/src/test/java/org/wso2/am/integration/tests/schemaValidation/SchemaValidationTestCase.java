/*
 *   Copyright (c) 2023, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.am.integration.tests.schemaValidation;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SchemaValidationTestCase extends APIManagerLifecycleBaseTest {

    private final Log log = LogFactory.getLog(SchemaValidationTestCase.class);
    private RestAPIPublisherImpl restAPIPublisherExport;
    private final String ROOT_RESOURCE_PATH =  "schemaValidation";
    private final String API_END_POINT_POSTFIX_URL =  "schemaValidationAPI";
    private String apiContext = "schemaValidationTestAPI";
    private String invokeURL, applicationID, apiID;
    Map<String, String> requestHeaders = new HashMap<>();

    @BeforeClass(alwaysRun = true) public void initialize() throws Exception {
        super.init();
        String apiDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(
                        ROOT_RESOURCE_PATH + File.separator + "schemaValidator.yml"), "UTF-8");
        String additionalProperties = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(
                        ROOT_RESOURCE_PATH + File.separator + "apiRequest.json"),"UTF-8");
        org.json.JSONObject apiCreateRequestObject = new org.json.JSONObject(additionalProperties);
        apiCreateRequestObject.put("provider", user.getUserName());

        //Populate Dummy endpoint configs
        String apiEndPointUrl = getGatewayURLNhttp() + API_END_POINT_POSTFIX_URL;
        JSONObject endpoint = new JSONObject();
        endpoint.put("url", apiEndPointUrl);
        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type","http");
        endpointConfig.put("production_endpoints", endpoint);
        endpointConfig.put("sandbox_endpoints", endpoint);
        apiCreateRequestObject.put("endpointConfig", endpointConfig);
        if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(user.getUserDomain())) {
            apiContext = "/t/" + user.getUserDomain() + apiContext;
        }
        apiCreateRequestObject.put("context", apiContext);
        apiCreateRequestObject.put("version", API_VERSION_1_0_0);
        File file = APIMTestCaseUtils.getTempSwaggerFileWithContent(apiDefinition);

        //Import API definition and create API with schema validation enabled
        APIDTO apidto = restAPIPublisher.importOASDefinition(file, apiCreateRequestObject.toString());
        apiID = apidto.getId();
        createAPIRevisionAndDeployUsingRest(apiID, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatusToPublish(apiID, false);
        waitForAPIDeploymentSync(apidto.getProvider(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);

        //Create Application, subscribe and generate tokens
        HttpResponse applicationResponse = restAPIStore
                .createApplication(APPLICATION_NAME + "testSchemaValidationApp", "Application to test Schema "
                        + "Validation", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.JWT);
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        applicationID = applicationResponse.getData();
        restAPIStore.subscribeToAPI(apiID, applicationID, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationID, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put("accept", "application/json");
        requestHeaders.put("Content-Type", "application/json");
        invokeURL = getAPIInvocationURLHttp(apiContext, API_VERSION_1_0_0);
    }

    @Test(groups = {"wso2.am"}, description = "Test SchemaValidation with invalid request body")
    public void testRequestSchemaValidationForInvalidRequestBody() throws Exception {
        JSONObject queryObject = new JSONObject();
        queryObject.put("category", "dog");
        queryObject.put("status", "available");
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL + "/pets", requestHeaders,
                queryObject.toString());
        //Schema validation fails as the request body does not match the defined schema
        Assert.assertEquals(serviceResponse.getResponseCode(), 400);
        Assert.assertEquals(serviceResponse.getData().contains("Schema validation failed in the Request"), true);
    }

    @Test(groups = {"wso2.am"}, description = "Test SchemaValidation failure for requests without required headers")
    public void testSchemaValidationForRequestsWithoutRequiredHeaders() throws Exception {
        HttpResponse serviceResponseWithoutHeader = HTTPSClientUtils.doGet(invokeURL + "/pets", requestHeaders);
        //Schema validation fails as the request does not have required headers
        Assert.assertEquals(serviceResponseWithoutHeader.getResponseCode(), 400);
        Assert.assertEquals(serviceResponseWithoutHeader.getData().contains("Schema validation failed in the Request"), true);
    }

    @Test(groups = {"wso2.am"}, description = "Test SchemaValidation success for requests with required headers")
    public void testSchemaValidationForRequestsWithRequiredHeaders() throws Exception {
        //Header in lower case
        requestHeaders.put("x-request-id", "787878");
        HttpResponse serviceResponseWithHeader = HTTPSClientUtils.doGet(invokeURL + "/pets", requestHeaders);
        //Schema validation passes as the request has the required header(case-insensitive)
        Assert.assertEquals(serviceResponseWithHeader.getResponseCode(), 200);
        //Clean the custom request body
        requestHeaders.remove("x-request-id");
    }

    @Test(groups = {"wso2.am"}, description = "Test SchemaValidation with invalid response body")
    public void testRequestSchemaValidationForInvalidResponse() throws Exception {
        HttpResponse serviceResponse = HTTPSClientUtils.doGet(invokeURL + "/pets/123",
                requestHeaders);
        //Schema validation fails as the response body does not match the defined schema
        Assert.assertEquals(serviceResponse.getResponseCode(), 400);
        Assert.assertEquals(serviceResponse.getData().contains("Schema validation failed in the Response:"), true);
        //Clean the custom request body
    }

    @Test(groups = {"wso2.am"}, description = "Test SchemaValidation with valid request body")
    public void testRequestSchemaValidationForValidRequestBody() throws Exception {
        JSONObject queryObject = new JSONObject();
        queryObject.put("id", "8999898");
        queryObject.put("name", "max");
        queryObject.put("tag", "terrier");
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL + "/pets", requestHeaders,
                queryObject.toString());
        //Schema validation fails as the request body does not match the defined schema
        Assert.assertEquals(serviceResponse.getResponseCode(), 200);
    }

    @Test(groups = {"wso2.am"}, description = "Test SchemaValidation with valid response body")
    public void testRequestSchemaValidationForValidResponseBody() throws Exception {
        HttpResponse serviceResponse = HTTPSClientUtils.doGet(invokeURL + "/pets/123?isAvailable=false", requestHeaders);
        //Schema validation fails as the request body does not match the defined schema
        Assert.assertEquals(serviceResponse.getResponseCode(), 200);
    }

    @Test(groups = {"wso2.am"}, description = "Test SchemaValidation with valid requests for unsecured API resources")
    public void testRequestSchemaValidationForUnSecuredAPIResource() throws Exception {
        Map<String, String> requestHeadersWithoutAuthToken = new HashMap<>();
        requestHeadersWithoutAuthToken.put("accept", "application/json");
        requestHeadersWithoutAuthToken.put("Content-Type", "application/json");
        HttpResponse serviceResponse = HTTPSClientUtils.doGet(invokeURL + "/pet/findByStatus",
                requestHeadersWithoutAuthToken);
        //Schema validation fails for unsecured API resources
        Assert.assertEquals(serviceResponse.getResponseCode(), 400);
        Assert.assertEquals(serviceResponse.getData().contains("Schema validation failed in the Request"), true);
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        restAPIStore.deleteApplication(applicationID);
        undeployAndDeleteAPIRevisionsUsingRest(apiID, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiID);
    }
}
