/*
 *
 *   Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.prototype;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.WorkflowResponseDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

public class PrototypedAPITestcase extends APIMIntegrationBaseTest {

    private final String apiVersion = "1.0.0";
    private String apiProvider;
    private String apiName;
    private String apiEndPointUrl;
    private String resourcePath;
    private String apiID;
    private APIIdentifier apiIdentifier;
    private final String APPLICATION_NAME = "PrototypedAPITestcaseApllication";
    private String accessToken;
    String applicationId1;
    String applicationId2;
    String applicationId3;

    @Factory(dataProvider = "userModeDataProvider")
    public PrototypedAPITestcase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_USER}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws APIManagerIntegrationTestException, XPathExpressionException {

        super.init(userMode);
        apiProvider = user.getUserName();
        String apiPrototypeEndpointPostfixUrl = "am/sample/pizzashack/v1/api/menu";
        apiEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiPrototypeEndpointPostfixUrl;
    }

    @Test(groups = {"wso2.am"}, description = "Create an API with a prototype endpoint and invoke")
    public void testPrototypedAPIEndpoint() throws Exception {

        apiName = "APIMPrototypedEndpointAPI1";
        String apiContext = "pizzashack-prototype";
        String apiTags = "pizza, order, pizza-menu";
        String apiDescription = "Pizza API:Allows to manage pizza orders " +
                "(create, update, retrieve orders)";

        apiIdentifier = new APIIdentifier(apiProvider, apiName, apiVersion);
        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(apiEndPointUrl));
        apiRequest.setVersion(apiVersion);
        apiRequest.setDescription(apiDescription);
        apiRequest.setTags(apiTags);
        apiRequest.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        apiRequest.setProvider(apiProvider);

        HttpResponse addAPIResponse = restAPIPublisher.addAPI(apiRequest);
        assertEquals(addAPIResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Invalid Response Code");
        apiID = addAPIResponse.getData();

        //Deployed API as a Prototyped API & check the status
        WorkflowResponseDTO lcChangeResponse = restAPIPublisher.changeAPILifeCycleStatus(apiID,
                APILifeCycleAction.DEPLOY_AS_PROTOTYPE.getAction());

        HttpResponse response = restAPIPublisher.getAPI(apiID);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);
        String endPointString = "{\"implementation_status\":\"prototyped\",\"endpoint_type\":\"http\"," +
                "\"production_endpoints\":{\"config\":null," +
                "\"url\":\"" + apiEndPointUrl + "\"}," +
                "\"sandbox_endpoints\":{\"config\":null,\"url\":\"" + apiEndPointUrl + "\"}}";

        JSONParser parser = new JSONParser();
        JSONObject endpoint = (JSONObject) parser.parse(endPointString);
        apidto.setEndpointConfig(endpoint);

        restAPIPublisher.updateAPI(apidto);

        assertTrue(lcChangeResponse.getLifecycleState().getState().equals("Prototyped"),
                apiName + "  status not updated as Prototyped");
        waitForAPIDeployment();

        // Create a revision and Deploy the API
        createAPIRevisionAndDeployUsingRest(apiID, restAPIPublisher);

        //Check whether Prototype API is available in publisher
        APIListDTO getAllAPIsResponse = restAPIPublisher.getAllAPIs();
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, getAllAPIsResponse),
                "Implemented" + apiName + " Api is not visible in API Publisher.");
        waitForAPIDeployment();

        //Check whether Prototype API is available under the Prototyped API
        org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO prototypedAPIs = restAPIStore
                .getPrototypedAPIs(user.getUserDomain());
        assertTrue(APIMTestCaseUtils.isAPIAvailableInStore(apiIdentifier, prototypedAPIs),
                apiName + " is not visible as Prototyped API");

        HttpResponse applicationResponse = restAPIStore
                .createApplication(APPLICATION_NAME + "testPrototypedAPIEndpoint",
                        "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                        ApplicationDTO.TokenTypeEnum.JWT);
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        applicationId1 = applicationResponse.getData();
        restAPIStore.subscribeToAPI(apiID, applicationId1, "Gold");
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId1, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        //get access token
        accessToken = applicationKeyDTO.getToken().getAccessToken();

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "application/json");

        //Invoke the Prototype endpoint and validate
        HttpResponse response1 = HTTPSClientUtils
                .doGet(getAPIInvocationURLHttps(apiContext, apiVersion) +
                        "", requestHeaders);
        Assert.assertEquals(response1.getResponseCode(), 200);
        Assert.assertTrue(response1.getData().contains("BBQ Chicken Bacon"));
    }

    @Test(groups = {"wso2.am"}, description = "Create an API with a prototype endpoint, demote to created and invoke", dependsOnMethods = {"testPrototypedAPIEndpoint"})
    public void testDemotedPrototypedEndpointAPItoCreated() throws Exception {

        apiName = "APIMPrototypedEndpointAPI2";
        String apiContext = "pizzashack-prototype2";
        String apiTags = "pizza, order, pizza-menu";
        String apiDescription = "Pizza API:Allows to manage pizza orders " +
                "(create, update, retrieve orders)";

        apiIdentifier = new APIIdentifier(apiProvider, apiName, apiVersion);
        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(apiEndPointUrl));
        apiRequest.setVersion(apiVersion);
        apiRequest.setDescription(apiDescription);
        apiRequest.setTags(apiTags);
        apiRequest.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        apiRequest.setProvider(apiProvider);

        HttpResponse addAPIResponse = restAPIPublisher.addAPI(apiRequest);
        assertEquals(addAPIResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Invalid Response Code");
        apiID = addAPIResponse.getData();

        //Deployed API as a Prototyped API & check the status
        WorkflowResponseDTO lcChangeResponse = restAPIPublisher.changeAPILifeCycleStatus(apiID,
                APILifeCycleAction.DEPLOY_AS_PROTOTYPE.getAction());

        HttpResponse response = restAPIPublisher.getAPI(apiID);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);
        String endPointString = "{\"implementation_status\":\"prototyped\",\"endpoint_type\":\"http\"," +
                "\"production_endpoints\":{\"config\":null," +
                "\"url\":\"" + apiEndPointUrl + "\"}," +
                "\"sandbox_endpoints\":{\"config\":null,\"url\":\"" + apiEndPointUrl + "\"}}";

        JSONParser parser = new JSONParser();
        JSONObject endpoint = (JSONObject) parser.parse(endPointString);
        apidto.setEndpointConfig(endpoint);

        // Update the API with Prototype endpoint
        restAPIPublisher.updateAPI(apidto);

        assertTrue(lcChangeResponse.getLifecycleState().getState().equals("Prototyped"),
                apiName + "  status not updated as Prototyped");
        waitForAPIDeployment();

        // Create a revision and Deploy the API
        createAPIRevisionAndDeployUsingRest(apiID, restAPIPublisher);

        // Check whether Prototype API is available in publisher
        APIListDTO getAllAPIsResponse = restAPIPublisher.getAllAPIs();
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, getAllAPIsResponse),
                "Implemented" + apiName + " Api is not visible in API Publisher.");
        waitForAPIDeployment();

        // Check whether Prototype API is available under the Prototyped API
        org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO prototypedAPIs = restAPIStore
                .getPrototypedAPIs(user.getUserDomain());
        assertTrue(APIMTestCaseUtils.isAPIAvailableInStore(apiIdentifier, prototypedAPIs),
                apiName + " is not visible as Prototyped API");

        // Change the status PROTOTYPED to CREATED
        restAPIPublisher.changeAPILifeCycleStatus(apiID, APILifeCycleAction.DEMOTE_TO_CREATE.getAction());
        assertTrue(APILifeCycleState.CREATED.getState().equals(restAPIPublisher.getLifecycleStatus(apiID).getData()),
                apiName + "status not updated as CREATED");

        // Wait for the changes to be applied after demoting to Created.
        waitForAPIDeployment();
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "application/json");

        // Invoke the Prototype endpoint
        HttpResponse response2 = HTTPSClientUtils
                .doGet(getAPIInvocationURLHttps(apiContext, apiVersion) +
                        "", requestHeaders);
        Assert.assertEquals(response2.getResponseCode(), 401, "User was able to invoke the API demoted to CREATED from PROTOTYPE");
    }

    @Test(groups = {"wso2.am"}, description = "Create an inline protoype API with OAS3 and Generate mock")
    public void testOAS3InlinePrototypeWithMock() throws Exception {

        String context = "/SwaggerPetstorev3import";
        resourcePath = "oas" + File.separator + "v3" + File.separator;
        String originalDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "prototype" + File.separator + "oas_import.json"),
                "UTF-8");
        String additionalProperties = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "prototype" + File.separator + "additionalProperties.json"),
                "UTF-8");
        String updatedMock = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "prototype" + File.separator + "updatedMockOas.json"),
                "UTF-8");
        org.json.JSONObject additionalPropertiesObj = new org.json.JSONObject(additionalProperties);
        additionalPropertiesObj.put("provider", user.getUserName());
        if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(user.getUserDomain())) {
            context = "/t/" + user.getUserDomain() + context;
        }
        additionalPropertiesObj.put("context", context);
        org.json.JSONObject updatedMockObj = new org.json.JSONObject(updatedMock);
        updatedMockObj.put("provider", user.getUserName());
        File file = getTempFileWithContent(originalDefinition);
        // Create an api by importing OAS3 file
        APIDTO apidto = restAPIPublisher.importOASDefinition(file, additionalPropertiesObj.toString());
        String apiImportId = apidto.getId();

        // Change the lifecycle status to Prototype
        restAPIPublisher.changeAPILifeCycleStatus(apiImportId, Constants.DEPLOY_AS_PROTOTYPE);

        // Generate mock Script for Prototype Implementation
        HttpResponse mockgenResponse = restAPIPublisher.generateMockScript(apiImportId);
        Assert.assertEquals(mockgenResponse.getResponseCode(), 200);

        // Retrieve and validate the generated mock script
        HttpResponse mockedGetResponse = restAPIPublisher.getGenerateMockScript(apiImportId);
        Assert.assertTrue(mockedGetResponse.getData().contains("/pets"));
        Assert.assertTrue(mockedGetResponse.getData().contains("/pets/{petId}"));
        Assert.assertTrue(mockedGetResponse.getData().contains("/oldpets"));

        // Create a revision and Deploy the API
        createAPIRevisionAndDeployUsingRest(apiImportId, restAPIPublisher);
        HttpResponse applicationResponse = restAPIStore
                .createApplication(APPLICATION_NAME + "testOAS3InlinePrototypeWithMock",
                        "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                        ApplicationDTO.TokenTypeEnum.JWT);
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        applicationId2 = applicationResponse.getData();
        restAPIStore.subscribeToAPI(apiImportId, applicationId2, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId2, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        //get access token
        accessToken = applicationKeyDTO.getToken().getAccessToken();

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "application/json");
        waitForAPIDeployment();
        //Invoke the Prototype endpoint and validate
        HttpResponse response1 = HTTPSClientUtils
                .doGet(getAPIInvocationURLHttps("SwaggerPetstorev3import", "1.0.0") +
                        "/pets/1", requestHeaders);

        Assert.assertEquals(response1.getResponseCode(), 200);
    }

    @Test(groups = {"wso2.am"}, description = "Create an inline protoype API with OAS2 and Generate mock")
    public void testOAS2InlinePrototypeWithMock() throws Exception {

        String context = "/SwaggerPetstorev2import";
        resourcePath = "oas" + File.separator + "v2" + File.separator;
        String originalDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "prototype" + File.separator + "oas_import.json"),
                "UTF-8");
        String additionalProperties = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(resourcePath + "prototype" + File.separator + "additionalProperties.json"),
                "UTF-8");
        if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(user.getUserDomain())) {
            context = "/t/" + user.getUserDomain() + context;
        }
        org.json.JSONObject additionalPropertiesObj = new org.json.JSONObject(additionalProperties);
        additionalPropertiesObj.put("provider", user.getUserName());
        additionalPropertiesObj.put("context", context);

        File file = getTempFileWithContent(originalDefinition);
        // Create an api by importing OAS2 file
        APIDTO apidto = restAPIPublisher.importOASDefinition(file, additionalPropertiesObj.toString());
        String apiImportId = apidto.getId();

        // Change the lifecycle status to Prototype
        restAPIPublisher.changeAPILifeCycleStatus(apiImportId, Constants.DEPLOY_AS_PROTOTYPE);

        // Generate mock Script for Prototype Implementation
        HttpResponse mockgenResponse = restAPIPublisher.generateMockScript(apiImportId);
        Assert.assertEquals(mockgenResponse.getResponseCode(), 200);

        // Retrieve and validate the generated mock script
        HttpResponse mockedGetResponse = restAPIPublisher.getGenerateMockScript(apiImportId);
        Assert.assertTrue(mockedGetResponse.getData().contains("/pets"));
        Assert.assertTrue(mockedGetResponse.getData().contains("/pets/{petId}"));
        Assert.assertTrue(mockedGetResponse.getData().contains("/oldpets"));

        // Create a revision and Deploy the API
        createAPIRevisionAndDeployUsingRest(apiImportId, restAPIPublisher);

        HttpResponse applicationResponse = restAPIStore
                .createApplication(APPLICATION_NAME + "testOAS2InlinePrototypeWithMock",
                        "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                        ApplicationDTO.TokenTypeEnum.JWT);
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        applicationId3 = applicationResponse.getData();
        restAPIStore.subscribeToAPI(apiImportId, applicationId3, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId3, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        //get access token
        accessToken = applicationKeyDTO.getToken().getAccessToken();

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "application/json");
        waitForAPIDeployment();
        //Invoke the Prototype endpoint and validate
        HttpResponse response1 = HTTPSClientUtils
                .doGet(getAPIInvocationURLHttps("SwaggerPetstorev2import", "1.0.0") +
                        "/pets/1", requestHeaders);
        Assert.assertEquals(response1.getResponseCode(), 200);
    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        restAPIStore.deleteApplication(applicationId1);
        restAPIStore.deleteApplication(applicationId2);
        restAPIStore.deleteApplication(applicationId3);
        super.cleanUp();
    }

    private File getTempFileWithContent(String swagger) throws Exception {

        File temp = File.createTempFile("swagger", ".json");
        temp.deleteOnExit();
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.write(swagger);
        out.close();
        return temp;
    }
}
