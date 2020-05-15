/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.am.scenario.tests.rest.api.creation;

//import com.sun.deploy.net.HttpResponse;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.am.scenario.test.common.ScenarioTestUtils;
import org.wso2.am.scenario.test.common.httpserver.SimpleHTTPServer;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.net.URL;
import java.util.Map;

public class RESTApiCreationUsingOASDocTestCase extends ScenarioTestBase {
    private static final Log log = LogFactory.getLog(RESTApiCreationUsingOASDocTestCase.class);

    private APIPublisherRestClient apiPublisher;
    private APIRequest apiRequest;
    private APICreationRequestBean designBean;

    private String import_definition_url = "swagger-url";
    private String import_definition_file = "swagger-file";
    private File swaggerFile;
    private String swaggerUrl;
    private String type = "rest";
    private String apiName;
    private String apiVersion;
    private String apiContext;
    private String APICreator = "APICreator";
    private String pw = "wso2123$";
    private final String admin = "admin";
    private final String adminPw = "admin";

    private String apiId;
    private String providerName;

    private String OAS2JSONAPIName = "PetApiSample_OAS2_JSON";
    private String OAS2YAMLAPIName = "PetApiSample_OAS2_YAML";
    private String OAS3JSONAPIName = "PetApiSample_OAS3_JSON";
    private String OAS3YAMLAPIName = "PetApiSample_OAS3_YAML";

    private final static String OAS_V2 = "v2";
    private final static String OAS_V3 = "v3";

    String resourceLocation = System.getProperty("test.resource.location");

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        providerName = user.getUserName();

    }

    @Test(description = "1.1.2.1")
    public void createApiWithValidOAS2DocumentAsJSONFile() throws Exception {

        swaggerFile = new File(resourceLocation + File.separator + "swaggerFiles/OAS2Document.json");

        String swaggerFileData = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(resourceLocation + File.separator + "swaggerFiles/OAS2Document.json"));
        ObjectMapper objectMapper = new ObjectMapper();
        APIDTO apiDTO = objectMapper.readValue(swaggerFile, APIDTO.class);
        apiDTO.setProvider(providerName);

        APIDTO responseAPIDTO = restAPIPublisher.addAPI(apiDTO, OAS_V2);
        apiId = responseAPIDTO.getId();
        Assert.assertEquals(responseAPIDTO.getLifeCycleStatus(), "CREATED");

        //Assert the API with imported values
        String payload = ScenarioTestUtils.readFromFile(swaggerFile.getAbsolutePath());
        JSONObject json = new JSONObject(payload);
        apiName = json.getJSONObject("info").get("title").toString();
        apiContext = json.get("basePath").toString();
        apiVersion = json.getJSONObject("info").get("version").toString();

        Assert.assertEquals(responseAPIDTO.getName(), apiName, "API name was not imported correctly");
        Assert.assertEquals(responseAPIDTO.getContext(), apiContext, "API context was not imported correctly");
        Assert.assertEquals(responseAPIDTO.getVersion(), apiVersion, "API version was not imported correctly");

        //Assert resources
        HttpResponse getResponse = restAPIPublisher.getAPI(apiId);
        String resources = (new JSONObject(getResponse)).getJSONObject("api").get("resources").toString();
        Assert.assertTrue(resources != null, "API resources not imported correctly");

        assertGETResource(((JSONArray) (new JSONObject(getResponse)).getJSONObject("api").get("templates")).get(0));

        HttpResponse response = restAPIPublisher.deleteAPI(apiId);
        verifyResponse(response);

        //Import api definition from swagger file
//        apiRequest = new APIRequest(import_definition_file, swaggerFile.getAbsolutePath(), "", type);
//        HttpResponse designResponse1 = apiPublisher.designAPIWithOAS(apiRequest);
//        Assert.assertTrue(designResponse1.getData().contains("imported"));

//        apiRequest = new APIRequest(apiName, apiContext, apiVersion);
//        apiRequest.setSwagger(payload);
//        HttpResponse designResponse2 = apiPublisher.designAPI(apiRequest);
//        verifyResponse(designResponse2);

        //Verify the API created with correct name, version and resources
//        String name = (new JSONObject(designResponse2.getData())).getJSONObject("data").get("apiName").toString();
//        String version = (new JSONObject(designResponse2.getData())).getJSONObject("data").get("version").toString();
//        Assert.assertEquals(name, apiName, "Api name was not imported correctly");
//        Assert.assertEquals(version, apiVersion, "Api version was not imported correctly");
//
//        HttpResponse getResponse = apiPublisher.getAPI(apiName, APICreator, apiVersion);
//        String resource = (new JSONObject(getResponse.getData())).getJSONObject("api").get("resources").toString();
//        Assert.assertTrue(resource != "null", "API resource was not imported correctly");

        //Assert resources
//        assertGETResource(((JSONArray) (new JSONObject(getResponse.getData())).getJSONObject("api").
//                get("templates")).get(0));


//        HttpResponse response = apiPublisher.deleteAPI(OAS2JSONAPIName, apiVersion, providerName);
//        verifyResponse(response);

    }

    @Test(description = "1.1.2.2", dependsOnMethods = "createApiWithValidOAS2DocumentAsJSONFile")
    public void createApiWithValidOAS3DocumentAsJSONFile() throws Exception {

        swaggerFile = new File(resourceLocation + File.separator + "swaggerFiles/OAS3Document.json");

        String swaggerFileData = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(resourceLocation + File.separator + "swaggerFiles/OAS3Document.json"));
        ObjectMapper objectMapper = new ObjectMapper();
        APIDTO apiDTO = objectMapper.readValue(swaggerFile, APIDTO.class);
        apiDTO.setProvider(providerName);

        APIDTO responseAPIDTO = restAPIPublisher.addAPI(apiDTO, OAS_V3);
        apiId = responseAPIDTO.getId();
        Assert.assertEquals(responseAPIDTO.getLifeCycleStatus(), "CREATED");

        //Assert the API with imported values
        String payload = ScenarioTestUtils.readFromFile(swaggerFile.getAbsolutePath());
        JSONObject json = new JSONObject(payload);
        apiName = json.getJSONObject("info").get("title").toString();
        apiContext = json.get("basePath").toString();
        apiVersion = json.getJSONObject("info").get("version").toString();

        Assert.assertEquals(responseAPIDTO.getName(), apiName, "API name was not imported correctly");
        Assert.assertEquals(responseAPIDTO.getContext(), apiContext, "API context was not imported correctly");
        Assert.assertEquals(responseAPIDTO.getVersion(), apiVersion, "API version was not imported correctly");

        //Assert resources
        HttpResponse getResponse = restAPIPublisher.getAPI(apiId);
        String resources = (new JSONObject(getResponse)).getJSONObject("api").get("resources").toString();
        Assert.assertTrue(resources != null, "API resources not imported correctly");

        assertGETResource(((JSONArray) (new JSONObject(getResponse)).getJSONObject("api").get("templates")).get(0));
        assertGETResource(((JSONArray) (new JSONObject(getResponse)).getJSONObject("api").get("templates")).get(1));

        HttpResponse response = restAPIPublisher.deleteAPI(apiId);
        verifyResponse(response);

        //Import api definition from swagger file
//        apiRequest = new APIRequest(import_definition_file, swaggerFile.getAbsolutePath(), "", type);
//
//        HttpResponse designResponse1 = apiPublisher.designAPIWithOAS(apiRequest);
//        Assert.assertTrue(designResponse1.getData().contains("imported"));
//
//        //Save the API with imported values
//        String payload = ScenarioTestUtils.readFromFile(swaggerFile.getAbsolutePath());
//        JSONObject json = new JSONObject(payload);
//        String apiName = json.getJSONObject("info").get("title").toString();
//        String apiContext = json.get("basePath").toString();
//        String apiVersion = json.getJSONObject("info").get("version").toString();
//
//        apiRequest = new APIRequest(apiName, apiContext, apiVersion);
//        apiRequest.setSwagger(payload);
//        HttpResponse designResponse2 = apiPublisher.designAPI(apiRequest);
//        verifyResponse(designResponse2);
//
//        //Verify the API created with correct name, version and resources
//        String name = (new JSONObject(designResponse2.getData())).getJSONObject("data").get("apiName").toString();
//        String version = (new JSONObject(designResponse2.getData())).getJSONObject("data").get("version").toString();
//        Assert.assertEquals(name, apiName, "Api name was not imported correctly");
//        Assert.assertEquals(version, apiVersion, "Api version was not imported correctly");
//
//        HttpResponse getResponse = apiPublisher.getAPI(apiName, APICreator, apiVersion);
//        String resource = (new JSONObject(getResponse.getData())).getJSONObject("api").get("resources").toString();
//        Assert.assertTrue(resource != "null", "API resource was not imported correctly");
//
//        //Assert resources
//        assertPOSTResource(((JSONArray) (new JSONObject(getResponse.getData())).getJSONObject("api").
//                get("templates")).get(0));
//        assertGETResource(((JSONArray) (new JSONObject(getResponse.getData())).getJSONObject("api").
//                get("templates")).get(1));
//
//        HttpResponse response = apiPublisher.deleteAPI(OAS3JSONAPIName, apiVersion, APICreator);
//        verifyResponse(response);

    }

    @Test(description = "1.1.2.3", dependsOnMethods = "createApiWithValidOAS3DocumentAsJSONFile")
    public void createApiWithValidOAS2DocumentAsYAMLFile() throws Exception {

        swaggerFile = new File(resourceLocation + File.separator + "swaggerFiles/OAS2Document.yaml");

        String swaggerFileData = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(resourceLocation + File.separator + "swaggerFiles/OAS2Document.yaml"));
        ObjectMapper objectMapper = new ObjectMapper();
        APIDTO apiDTO = objectMapper.readValue(swaggerFile, APIDTO.class);
        apiDTO.setProvider(providerName);

//        APIDTO responseAPIDTO = restAPIPublisher.addAPI(apiDTO, OAS_V3);
//        apiId = responseAPIDTO.getId();
//        Assert.assertEquals(responseAPIDTO.getLifeCycleStatus(), "CREATED");

        JSONObject additionalPropertiesObject = new JSONObject();
        additionalPropertiesObject.put("provider", providerName);

        APIDTO responseAPIDTO = restAPIPublisher.importOASDefinition(swaggerFile, providerName);
        apiId = responseAPIDTO.getId();
        Assert.assertEquals(responseAPIDTO.getLifeCycleStatus(), "CREATED");

        //Assert the API with imported values
        String payload = ScenarioTestUtils.readFromFile(swaggerFile.getAbsolutePath());
        Yaml yaml = new Yaml();
        Map<String, Object> map = (Map<String, Object>) yaml.load(payload);
        JSONObject json = new JSONObject(map);
        apiName = json.getJSONObject("info").get("title").toString();
        apiContext = json.get("basePath").toString();
        apiVersion = json.getJSONObject("info").get("version").toString();

        Assert.assertEquals(responseAPIDTO.getName(), apiName, "API name was not imported correctly");
        Assert.assertEquals(responseAPIDTO.getContext(), apiContext, "API context was not imported correctly");
        Assert.assertEquals(responseAPIDTO.getVersion(), apiVersion, "API version was not imported correctly");

        //Assert resources
        HttpResponse getResponse = restAPIPublisher.getAPI(apiId);
        String resources = (new JSONObject(getResponse)).getJSONObject("api").get("resources").toString();
        Assert.assertTrue(resources != null, "API resources not imported correctly");

        assertGETResource(((JSONArray) (new JSONObject(getResponse)).getJSONObject("api").get("templates")).get(0));

        HttpResponse response = restAPIPublisher.deleteAPI(apiId);
        verifyResponse(response);

//        //Import api definition from swagger file
//        apiRequest = new APIRequest(import_definition_file, swaggerFile.getAbsolutePath(), "", type);
//
//        HttpResponse designResponse1 = apiPublisher.designAPIWithOAS(apiRequest);
//        Assert.assertTrue(designResponse1.getData().contains("imported"));
//
//        //Save the API with imported values
//        String payload = ScenarioTestUtils.readFromFile(swaggerFile.getAbsolutePath());
//
//        Yaml yaml = new Yaml();
//        Map<String, Object> map = (Map<String, Object>) yaml.load(payload);
//        JSONObject json = new JSONObject(map);
//        String apiName = json.getJSONObject("info").get("title").toString();
//        String apiContext = json.get("basePath").toString();
//        String apiVersion = json.getJSONObject("info").get("version").toString();
//        String payloadJson = json.toString();
//
//        apiRequest = new APIRequest(apiName, apiContext, apiVersion);
//        apiRequest.setSwagger(payloadJson);
//        HttpResponse designResponse2 = apiPublisher.designAPI(apiRequest);
//        verifyResponse(designResponse2);
//
//        //Verify the API created with correct name, version and resources
//        String name = (new JSONObject(designResponse2.getData())).getJSONObject("data").get("apiName").toString();
//        String version = (new JSONObject(designResponse2.getData())).getJSONObject("data").get("version").toString();
//        Assert.assertEquals(name, apiName, "Api name was not imported correctly");
//        Assert.assertEquals(version, apiVersion, "Api version was not imported correctly");
//
//        HttpResponse getResponse = apiPublisher.getAPI(apiName, APICreator, apiVersion);
//        String resource = (new JSONObject(getResponse.getData())).getJSONObject("api").get("resources").toString();
//        Assert.assertTrue(resource != "null", "API resource was not imported correctly");
//
//        //Assert resources
//        assertGETResource(((JSONArray) (new JSONObject(getResponse.getData())).getJSONObject("api").
//                get("templates")).get(0));
//
//        HttpResponse response = apiPublisher.deleteAPI(OAS2YAMLAPIName, apiVersion, APICreator);
//        verifyResponse(response);

    }

    @Test(description = "1.1.2.4", dependsOnMethods = "createApiWithValidOAS2DocumentAsYAMLFile")
    public void createApiWithValidOAS3DocumentAsYAMLFile() throws Exception {

        swaggerFile = new File(resourceLocation + File.separator + "swaggerFiles/OAS3Document.yaml");

        // this can also be used, not sure about yaml object mapping
        String swaggerFileData = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(resourceLocation + File.separator + "swaggerFiles/OAS3Document.yaml"));
        ObjectMapper objectMapper = new ObjectMapper();
        APIDTO apiDTO = objectMapper.readValue(swaggerFile, APIDTO.class);
        apiDTO.setProvider(providerName);

//        APIDTO responseAPIDTO = restAPIPublisher.addAPI(apiDTO, OAS_V3);
//        apiId = responseAPIDTO.getId();
//        Assert.assertEquals(responseAPIDTO.getLifeCycleStatus(), "CREATED");

        JSONObject additionalPropertiesObject = new JSONObject();
        additionalPropertiesObject.put("provider", providerName);

        APIDTO responseAPIDTO = restAPIPublisher.importOASDefinition(swaggerFile, providerName);
        apiId = responseAPIDTO.getId();
        Assert.assertEquals(responseAPIDTO.getLifeCycleStatus(), "CREATED");

        //Assert the API with imported values
        String payload = ScenarioTestUtils.readFromFile(swaggerFile.getAbsolutePath());
        Yaml yaml = new Yaml();
        Map<String, Object> map = (Map<String, Object>) yaml.load(payload);
        JSONObject json = new JSONObject(map);
        apiName = json.getJSONObject("info").get("title").toString();
        apiContext = json.get("basePath").toString();
        apiVersion = json.getJSONObject("info").get("version").toString();

        Assert.assertEquals(responseAPIDTO.getName(), apiName, "API name was not imported correctly");
        Assert.assertEquals(responseAPIDTO.getContext(), apiContext, "API context was not imported correctly");
        Assert.assertEquals(responseAPIDTO.getVersion(), apiVersion, "API version was not imported correctly");

        //Assert resources
        HttpResponse getResponse = restAPIPublisher.getAPI(apiId);
        String resources = (new JSONObject(getResponse)).getJSONObject("api").get("resources").toString();
        Assert.assertTrue(resources != null, "API resources not imported correctly");

        assertGETResource(((JSONArray) (new JSONObject(getResponse)).getJSONObject("api").get("templates")).get(0));
        assertGETResource(((JSONArray) (new JSONObject(getResponse)).getJSONObject("api").get("templates")).get(1));

        HttpResponse response = restAPIPublisher.deleteAPI(apiId);
        verifyResponse(response);

        //Import api definition from swagger file
//        apiRequest = new APIRequest(import_definition_file, swaggerFile.getAbsolutePath(), "", type);
//
//        HttpResponse designResponse1 = apiPublisher.designAPIWithOAS(apiRequest);
//        Assert.assertTrue(designResponse1.getData().contains("imported"));
//
//        //Save the API with imported values
//        String payload = ScenarioTestUtils.readFromFile(swaggerFile.getAbsolutePath());
//
//        Yaml yaml = new Yaml();
//        Map<String, Object> map = (Map<String, Object>) yaml.load(payload);
//        JSONObject json = new JSONObject(map);
//        String apiName = json.getJSONObject("info").get("title").toString();
//        String apiContext = json.get("basePath").toString();
//        String apiVersion = json.getJSONObject("info").get("version").toString();
//        String payloadJson = json.toString();
//
//        apiRequest = new APIRequest(apiName, apiContext, apiVersion);
//        apiRequest.setSwagger(payloadJson);
//        HttpResponse designResponse2 = apiPublisher.designAPI(apiRequest);
//        verifyResponse(designResponse2);
//
//        //Verify the API created with correct name, version and resources
//        String name = (new JSONObject(designResponse2.getData())).getJSONObject("data").get("apiName").toString();
//        String version = (new JSONObject(designResponse2.getData())).getJSONObject("data").get("version").toString();
//        Assert.assertEquals(name, apiName, "Api name was not imported correctly");
//        Assert.assertEquals(version, apiVersion, "Api version was not imported correctly");
//
//        HttpResponse getResponse = apiPublisher.getAPI(apiName, APICreator, apiVersion);
//        String resource = (new JSONObject(getResponse.getData())).getJSONObject("api").get("resources").toString();
//        Assert.assertTrue(resource != "null", "API resource was not imported correctly");
//
//        //Assert resources
//        assertPOSTResource(((JSONArray) (new JSONObject(getResponse.getData())).getJSONObject("api").
//                get("templates")).get(0));
//        assertGETResource(((JSONArray) (new JSONObject(getResponse.getData())).getJSONObject("api").
//                get("templates")).get(1));
//
//        HttpResponse response = apiPublisher.deleteAPI(OAS3YAMLAPIName, apiVersion, APICreator);
//        verifyResponse(response);

    }

    private void assertGETResource(Object resource) throws JSONException {

        if (resource instanceof JSONArray) {
            JSONArray resourceAttributes = (JSONArray) resource;
            String path = resourceAttributes.get(0).toString();
            Assert.assertEquals(path, "/pets/{petId}", "API resource path not imported correctly");

            String httpMethod = resourceAttributes.get(1).toString();
            Assert.assertEquals(httpMethod, "GET", "API resource's httpMethod not imported correctly");

            String authType = resourceAttributes.get(2).toString();
            Assert.assertEquals(authType, "Any", "API resource's authType not imported correctly");
        }
    }

    private void assertPOSTResource(Object resource) throws JSONException {

        if (resource instanceof JSONArray) {
            JSONArray resourceAttributes = (JSONArray) resource;
            String path = resourceAttributes.get(0).toString();
            Assert.assertEquals(path, "/pets", "API resource path not imported correctly");

            String httpMethod = resourceAttributes.get(1).toString();
            Assert.assertEquals(httpMethod, "POST", "API resource's httpMethod not imported correctly");

            String authType = resourceAttributes.get(2).toString();
            Assert.assertEquals(authType, "Any", "API resource's authType not imported correctly");
        }
    }

    @Test(description = "1.1.2.5", dataProvider = "OASDocsWithJsonURL", dataProviderClass = ScenarioDataProvider.class)
    public void testCreateApiUsingValidOASDocumentFromJsonURL(String url) throws Exception {

        swaggerUrl = url;

        ObjectMapper objectMapper = new ObjectMapper();
        APIDTO apiDTO = objectMapper.readValue(new URL(swaggerUrl), APIDTO.class);
        apiDTO.setProvider(providerName);

        APIDTO responseAPIDTO = restAPIPublisher.addAPI(apiDTO, OAS_V2);
        apiId = responseAPIDTO.getId();
        Assert.assertEquals(responseAPIDTO.getLifeCycleStatus(), "CREATED");

        String payload = ScenarioTestUtils.readFromURL(swaggerUrl);
        JSONObject json = new JSONObject(payload);
        apiName = json.getJSONObject("info").get("title").toString();
        apiContext = json.get("basePath").toString();
        apiVersion = json.getJSONObject("info").get("version").toString();

        Assert.assertEquals(responseAPIDTO.getName(), apiName, "API name was not imported correctly");
        Assert.assertEquals(responseAPIDTO.getContext(), apiContext, "API context was not imported correctly");
        Assert.assertEquals(responseAPIDTO.getVersion(), apiVersion, "API version was not imported correctly");

        HttpResponse response = restAPIPublisher.deleteAPI(apiId);
        verifyResponse(response);


//        apiRequest = new APIRequest(import_definition_url, "", swaggerUrl, type);
//        SimpleHTTPServer server = new SimpleHTTPServer();
//        new Thread(server).start();
//        HttpResponse serviceResponse = apiPublisher.designAPIWithOASURL(apiRequest);
//        server.stop();
//        Assert.assertTrue(serviceResponse.getData().contains("imported"), "Error importing swagger from : " + url);
//
//        new Thread(new SimpleHTTPServer()).start();
//        String payload = ScenarioTestUtils.readFromURL(swaggerUrl);
//
//        JSONObject json = new JSONObject(payload);
//        String apiName = json.getJSONObject("info").get("title").toString();
//        String context = json.get("basePath").toString();
//        String version = json.getJSONObject("info").get("version").toString();
//
//        apiRequest = new APIRequest(apiName, context, version);
//        apiRequest.setSwagger(payload);
//
//        serviceResponse = apiPublisher.designAPI(apiRequest);
//        String name = (new JSONObject(serviceResponse.getData())).getJSONObject("data").get("apiName").toString();
//        Assert.assertEquals(name, apiName);
//
//        serviceResponse = apiPublisher.deleteAPI(apiName, version, APICreator);
//        verifyResponse(serviceResponse);
//
//        Thread.sleep(1000); // To avoid connection failure in the next iteration.
//        server = null;
    }

    @Test(description = "1.1.2.6", dataProvider = "OASDocsWithYamlURL", dataProviderClass = ScenarioDataProvider.class)
    public void testCreateApiUsingValidOASDocumentFromYamlURL(String url) throws Exception {

        swaggerUrl = url;

//        apiRequest = new APIRequest(import_definition_url, "", swaggerUrl, type);
//
//        SimpleHTTPServer server = new SimpleHTTPServer();
//        new Thread(server).start();
//
//        HttpResponse serviceResponse = apiPublisher.designAPIWithOAS(apiRequest);
//        server.stop();
//        Assert.assertTrue(serviceResponse.getData().contains("imported"));
//
//        new Thread(new SimpleHTTPServer()).start();
//        String payload = ScenarioTestUtils.readFromURL(swaggerUrl);
//
//        Yaml yaml = new Yaml();
//        Map<String, Object> map = (Map<String, Object>) yaml.load(payload);
//        JSONObject json = new JSONObject(map);
//        String apiName = json.getJSONObject("info").get("title").toString();
//        String context = json.get("basePath").toString();
//        String version = json.getJSONObject("info").get("version").toString();
//        String payloadJson = json.toString();
//
//        apiRequest = new APIRequest(apiName, context, version);
//        apiRequest.setSwagger(payloadJson);
//
//        serviceResponse = apiPublisher.designAPI(apiRequest);
//        String name = (new JSONObject(serviceResponse.getData())).getJSONObject("data").get("apiName").toString();
//        Assert.assertEquals(name, apiName);
//
//        serviceResponse = apiPublisher.deleteAPI(apiName, version, APICreator);
//        verifyResponse(serviceResponse);
//
//        Thread.sleep(1000); // To avoid connection failure in the next iteration.

    }

    @AfterClass(alwaysRun = true)
    public void RemoveAPI() throws Exception {
        //clean the data
        restAPIPublisher.deleteAPI(apiId);

    }

    /*
     *  Create Users that can be used in each test case in this class
     *  @throws APIManagerIntegrationTestException
     * */
    private void createUsers() throws Exception {

        createUser(APICreator, pw, new String[]{ScenarioTestConstants.CREATOR_ROLE}, admin, adminPw);
    }
}

