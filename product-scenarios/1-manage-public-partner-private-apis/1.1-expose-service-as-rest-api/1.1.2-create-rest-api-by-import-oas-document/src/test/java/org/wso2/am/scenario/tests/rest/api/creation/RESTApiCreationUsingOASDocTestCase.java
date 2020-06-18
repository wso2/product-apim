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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.am.scenario.test.common.ScenarioTestUtils;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.yaml.snakeyaml.Yaml;

public class RESTApiCreationUsingOASDocTestCase extends ScenarioTestBase {
    private static final Log log = LogFactory.getLog(RESTApiCreationUsingOASDocTestCase.class);

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_USERNAME = "admin@wso2.com";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";

    private String import_definition_url = "swagger-url";
    private String import_definition_file = "swagger-file";
    private File swaggerFile;
    private String swaggerUrl;
    private String type = "rest";
    private String apiName;
    private String apiVersion;
    private String apiContext;
    private String description;
    private String APICreator = "APICreator";
    private String pw = "wso2123$";
    private String link;

    private String apiId;
    private String apiProviderName;
    private List<String> apiIdList = new ArrayList<>();
    private String apiProductionEndPointUrl;
    private  String apiProductionEndpointPostfixUrl = "jaxrs_basic/services/customers/" + "customerservice/customers/123";

    private final static String OAS_V2 = "v2";
    private final static String OAS_V3 = "v3";

    String resourceLocation = System.getProperty("test.resource.location");

    @Factory(dataProvider = "userModeDataProvider")
    public RESTApiCreationUsingOASDocTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                                                  ADMIN_USERNAME, ADMIN_PW);
            createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, ADMIN_USERNAME, ADMIN_PW);
        }

        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
//           create user in wso2.com tenant
            addTenantAndActivate(ScenarioTestConstants.TENANT_WSO2, ADMIN_USERNAME, ADMIN_PW);
            if (isActivated(ScenarioTestConstants.TENANT_WSO2)) {
//           Add and activate wso2.com tenant
                createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                                                      TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
                createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, TENANT_ADMIN_USERNAME,
                                             TENANT_ADMIN_PW);
            }
        }
        super.init(userMode);
    }

    @Test(description = "1.1.2.1")
    public void createApiWithValidOAS2DocumentAsJSONFile() throws Exception {

        swaggerFile = new File(resourceLocation + File.separator + "swaggerFiles/OAS2Document.json");

        APIDTO apiDTO = new APIDTO();
        List<APIOperationsDTO> apiOperationsDTOs = new ArrayList<>();
        List<String> tagList = new ArrayList<>();

        String payload = ScenarioTestUtils.readFromFile(swaggerFile.getAbsolutePath());
        JSONObject jsonPayload = new JSONObject(payload);
        apiName = jsonPayload.getJSONObject("info").get("title").toString();
        apiContext = jsonPayload.get("basePath").toString();
        apiVersion = jsonPayload.getJSONObject("info").get("version").toString();
        description = jsonPayload.getJSONObject("info").get("description").toString();
        JSONObject paths = jsonPayload.getJSONObject("paths");
        JSONArray tags = jsonPayload.getJSONArray("tags");

        apiDTO.setName(apiName);
        apiDTO.setContext(apiContext);
        apiDTO.setVersion(apiVersion);
        apiDTO.setDescription(description);

        APIDTO responseAPIDTO = restAPIPublisher.addAPI(apiDTO, OAS_V2);
        apiId = responseAPIDTO.getId();
        apiIdList.add(apiId);
        Assert.assertEquals(responseAPIDTO.getLifeCycleStatus(), "CREATED");

        String swaggerResponse = restAPIPublisher.updateSwagger(apiId, payload);

        HttpResponse getResponse = restAPIPublisher.getAPI(apiId);
        JSONObject updatedResponse = new JSONObject(getResponse.getData());
        JSONArray resources = updatedResponse.getJSONArray("operations");
        Assert.assertTrue(resources != null, "API resources not imported correctly");

        Assert.assertEquals(updatedResponse.get("name"), apiName, "API name was not imported correctly");
        if (userMode.toString().equals("TENANT_USER")) {
            Assert.assertEquals(updatedResponse.get("context"), "/t/wso2.com" + apiContext, "API context was not imported correctly");
        } else {
            Assert.assertEquals(updatedResponse.get("context"), apiContext, "API context was not imported correctly");
        }
        Assert.assertEquals(updatedResponse.get("version"), apiVersion, "API version was not imported correctly");

        //Assert resources
        JSONObject resource = (JSONObject) resources.get(0);
        assertGETResource(resource);

        HttpResponse response = restAPIPublisher.deleteAPI(apiId);
        verifyResponse(response);

    }

    @Test(description = "1.1.2.2", dependsOnMethods = "createApiWithValidOAS2DocumentAsJSONFile")
    public void createApiWithValidOAS3DocumentAsJSONFile() throws Exception {

        swaggerFile = new File(resourceLocation + File.separator + "swaggerFiles/OAS3Document.json");

        APIDTO apiDTO = new APIDTO();
        List<APIOperationsDTO> apiOperationsDTOs = new ArrayList<>();
        List<String> tagList = new ArrayList<>();

        String payload = ScenarioTestUtils.readFromFile(swaggerFile.getAbsolutePath());
        JSONObject jsonPayload = new JSONObject(payload);
        apiName = jsonPayload.getJSONObject("info").get("title").toString();
        apiContext = jsonPayload.get("x-wso2-basePath").toString(); //"basePath": "/contextJsonV3",
        apiVersion = jsonPayload.getJSONObject("info").get("version").toString();
        JSONObject paths = jsonPayload.getJSONObject("paths");

        apiDTO.setName(apiName);
        apiDTO.setContext(apiContext);
        apiDTO.setVersion(apiVersion);
        apiDTO.setProvider(apiProviderName);

        APIDTO responseAPIDTO = restAPIPublisher.addAPI(apiDTO, OAS_V3);
        apiId = responseAPIDTO.getId();
        apiIdList.add(apiId);
        Assert.assertEquals(responseAPIDTO.getLifeCycleStatus(), "CREATED");

        String swaggerResponse = restAPIPublisher.updateSwagger(apiId, payload);

        HttpResponse getResponse = restAPIPublisher.getAPI(apiId);
        JSONObject updatedResponse = new JSONObject(getResponse.getData());
        JSONArray resources = updatedResponse.getJSONArray("operations");
        Assert.assertTrue(resources != null, "API resources not imported correctly");

        Assert.assertEquals(updatedResponse.get("name"), apiName, "API name was not imported correctly");
        if (userMode.toString().equals("TENANT_USER")) {
            Assert.assertEquals(updatedResponse.get("context"), "/t/wso2.com" + apiContext, "API context was not imported correctly");
        } else {
            Assert.assertEquals(updatedResponse.get("context"), apiContext, "API context was not imported correctly");
        }
        Assert.assertEquals(updatedResponse.get("version"), apiVersion, "API version was not imported correctly");


        //Assert resources
        JSONObject resourceGET = (JSONObject) resources.get(0);
        JSONObject resourcePOST = (JSONObject) resources.get(1);
        assertPOSTResource(resourcePOST);
        assertGETResource(resourceGET);

        HttpResponse response = restAPIPublisher.deleteAPI(apiId);
        verifyResponse(response);

    }

    @Test(description = "1.1.2.3", dependsOnMethods = "createApiWithValidOAS3DocumentAsJSONFile")
    public void createApiWithValidOAS2DocumentAsYAMLFile() throws Exception {

        swaggerFile = new File(resourceLocation + File.separator + "swaggerFiles/OAS2Document.yaml");

        APIDTO apiDTO = new APIDTO();
        List<APIOperationsDTO> apiOperationsDTOs = new ArrayList<>();
        List<String> tagList = new ArrayList<>();

        String payload = ScenarioTestUtils.readFromFile(swaggerFile.getAbsolutePath());
        Yaml yaml = new Yaml();
        Map<String, Object> map = (Map<String, Object>) yaml.load(payload);
        JSONObject jsonPayload = new JSONObject(map);
        apiName = jsonPayload.getJSONObject("info").get("title").toString();
        apiContext = jsonPayload.get("basePath").toString();
        apiVersion = jsonPayload.getJSONObject("info").get("version").toString();
        description = jsonPayload.getJSONObject("info").get("description").toString();
        JSONObject paths = jsonPayload.getJSONObject("paths");

        apiDTO.setName(apiName);
        apiDTO.setContext(apiContext);
        apiDTO.setVersion(apiVersion);
        apiDTO.setDescription(description);

        APIDTO responseAPIDTO = restAPIPublisher.addAPI(apiDTO, OAS_V2);
        apiId = responseAPIDTO.getId();
        apiIdList.add(apiId);
        Assert.assertEquals(responseAPIDTO.getLifeCycleStatus(), "CREATED");

        String swaggerResponse = restAPIPublisher.updateSwagger(apiId, payload);

        HttpResponse getResponse = restAPIPublisher.getAPI(apiId);
        JSONObject updatedResponse = new JSONObject(getResponse.getData());
        JSONArray resources = updatedResponse.getJSONArray("operations");
        Assert.assertTrue(resources != null, "API resources not imported correctly");

        //Assert imported values
        Assert.assertEquals(updatedResponse.get("name"), apiName, "API name was not imported correctly");
        if (userMode.toString().equals("TENANT_USER")) {
            Assert.assertEquals(updatedResponse.get("context"), "/t/wso2.com" + apiContext, "API context was not imported correctly");
        } else {
            Assert.assertEquals(updatedResponse.get("context"), apiContext, "API context was not imported correctly");
        }
        Assert.assertEquals(updatedResponse.get("version"), apiVersion, "API version was not imported correctly");

        //Assert resources
        JSONObject resource = (JSONObject) resources.get(0);
        assertGETResource(resource);

        HttpResponse response = restAPIPublisher.deleteAPI(apiId);
        verifyResponse(response);

    }

    @Test(description = "1.1.2.4", dependsOnMethods = "createApiWithValidOAS2DocumentAsYAMLFile")
    public void createApiWithValidOAS3DocumentAsYAMLFile() throws Exception {

        swaggerFile = new File(resourceLocation + File.separator + "swaggerFiles/OAS3Document.yaml");

        APIDTO apiDTO = new APIDTO();
        List<APIOperationsDTO> apiOperationsDTOs = new ArrayList<>();
        List<String> tagList = new ArrayList<>();

        String payload = ScenarioTestUtils.readFromFile(swaggerFile.getAbsolutePath());
        Yaml yaml = new Yaml();
        Map<String, Object> map = (Map<String, Object>) yaml.load(payload);
        JSONObject jsonPayload = new JSONObject(map);
        apiName = jsonPayload.getJSONObject("info").get("title").toString();
        apiContext = jsonPayload.get("x-wso2-basePath").toString();
        apiVersion = jsonPayload.getJSONObject("info").get("version").toString();
        JSONObject paths = jsonPayload.getJSONObject("paths");


        apiDTO.setName(apiName);
        apiDTO.setContext(apiContext);
        apiDTO.setVersion(apiVersion);

        APIDTO responseAPIDTO = restAPIPublisher.addAPI(apiDTO, OAS_V3);
        apiId = responseAPIDTO.getId();
        apiIdList.add(apiId);
        Assert.assertEquals(responseAPIDTO.getLifeCycleStatus(), "CREATED");

        String swaggerResponse = restAPIPublisher.updateSwagger(apiId, payload);

        HttpResponse getResponse = restAPIPublisher.getAPI(apiId);
        JSONObject updatedResponse = new JSONObject(getResponse.getData());
        JSONArray resources = updatedResponse.getJSONArray("operations");
        Assert.assertTrue(resources != null, "API resources not imported correctly");

        Assert.assertEquals(updatedResponse.get("name"), apiName, "API name was not imported correctly");
        if (userMode.toString().equals("TENANT_USER")) {
            Assert.assertEquals(updatedResponse.get("context"), "/t/wso2.com" + apiContext, "API context was not imported correctly");
        } else {
            Assert.assertEquals(updatedResponse.get("context"), apiContext, "API context was not imported correctly");
        }
        Assert.assertEquals(updatedResponse.get("version"), apiVersion, "API version was not imported correctly");

        //Assert resources
        JSONObject resourceGET = (JSONObject) resources.get(0);
        JSONObject resourcePOST = (JSONObject) resources.get(1);
        assertPOSTResource(resourcePOST);
        assertGETResource(resourceGET);

        HttpResponse response = restAPIPublisher.deleteAPI(apiId);
        verifyResponse(response);

    }

    @Test(description = "1.1.2.5")
    public void testCreateApiUsingValidOASDocumentFromJsonURL() throws Exception {
        String swaggerFileName = "OAS2Document.json";

        uploadFile(swaggerFileName);
        APIDTO apiDTO = new APIDTO();
        List<APIOperationsDTO> apiOperationsDTOs = new ArrayList<>();
        List<String> tagList = new ArrayList<>();
        String swaggerVersion;

        String payload = ScenarioTestUtils.readFromURL(link);

        JSONObject json = new JSONObject(payload);
        if (json.get("swagger") != null) {
            swaggerVersion = OAS_V2;
            apiContext = json.get("basePath").toString();
        } else {
            swaggerVersion = OAS_V3;
            apiContext = json.get("x-wso2-basePath").toString();
        }
        apiName = json.getJSONObject("info").get("title").toString();
        apiVersion = json.getJSONObject("info").get("version").toString();

        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            apiName=apiName+TestUserMode.TENANT_USER;
            apiContext=apiContext+TestUserMode.TENANT_USER;}

        apiDTO.setName(apiName);
        apiDTO.setContext(apiContext);
        apiDTO.setVersion(apiVersion);

        APIDTO responseAPIDTO = restAPIPublisher.addAPI(apiDTO, swaggerVersion);
        apiId = responseAPIDTO.getId();
        apiIdList.add(apiId);
        Assert.assertEquals(responseAPIDTO.getLifeCycleStatus(), "CREATED");

        String swaggerResponse = restAPIPublisher.updateSwagger(apiId, payload);

        HttpResponse getResponse = restAPIPublisher.getAPI(apiId);
        JSONObject updatedResponse = new JSONObject(getResponse.getData());
        JSONArray resources = updatedResponse.getJSONArray("operations");
        Assert.assertTrue(resources != null, "API resources not imported correctly");

        Assert.assertEquals(updatedResponse.get("name"), apiName, "API name was not imported correctly");
        if (userMode.toString().equals("TENANT_USER")) {
            Assert.assertEquals(updatedResponse.get("context"), "/t/wso2.com" + apiContext, "API context was not imported correctly");
        } else {
            Assert.assertEquals(updatedResponse.get("context"), apiContext, "API context was not imported correctly");
        }
        Assert.assertEquals(updatedResponse.get("version"), apiVersion, "API version was not imported correctly");

        HttpResponse response = restAPIPublisher.deleteAPI(apiId);
        verifyResponse(response);

    }

    @Test(description = "1.1.2.6")
    public void testCreateApiUsingValidOASDocumentFromYamlURL() throws Exception {
        String swaggerFileName = "OAS2Document.yaml";

        uploadFile(swaggerFileName);
        APIDTO apiDTO = new APIDTO();
        List<APIOperationsDTO> apiOperationsDTOs = new ArrayList<>();
        List<String> tagList = new ArrayList<>();
        String swaggerVersion;

        String payload = ScenarioTestUtils.readFromURL(link);
        Yaml yaml = new Yaml();
            Map<String, Object> map = (Map<String, Object>) yaml.load(payload);
        JSONObject jsonPayload = new JSONObject(map);
        if (jsonPayload.get("swagger") != null) {
            swaggerVersion = OAS_V2;
            apiContext = jsonPayload.get("basePath").toString();
        } else {
            swaggerVersion = OAS_V3;
            apiContext = jsonPayload.get("x-wso2-basePath").toString();
        }
        apiName = jsonPayload.getJSONObject("info").get("title").toString();
        apiVersion = jsonPayload.getJSONObject("info").get("version").toString();

        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            apiName=apiName+"tenant_API";
            apiContext=apiContext+"tenant_API";}

        apiDTO.setName(apiName);
        apiDTO.setContext(apiContext);
        apiDTO.setVersion(apiVersion);

        APIDTO responseAPIDTO = restAPIPublisher.addAPI(apiDTO, swaggerVersion);
        apiId = responseAPIDTO.getId();
        apiIdList.add(apiId);
        Assert.assertEquals(responseAPIDTO.getLifeCycleStatus(), "CREATED");

        String swaggerResponse = restAPIPublisher.updateSwagger(apiId, payload);

        HttpResponse getResponse = restAPIPublisher.getAPI(apiId);
        JSONObject updatedResponse = new JSONObject(getResponse.getData());
        JSONArray resources = updatedResponse.getJSONArray("operations");
        Assert.assertTrue(resources != null, "API resources not imported correctly");

        Assert.assertEquals(updatedResponse.get("name"), apiName, "API name was not imported correctly");
        if (userMode.toString().equals("TENANT_USER")) {
            Assert.assertEquals(updatedResponse.get("context"), "/t/wso2.com" + apiContext, "API context was not imported correctly");
        } else {
            Assert.assertEquals(updatedResponse.get("context"), apiContext, "API context was not imported correctly");
        }
        Assert.assertEquals(updatedResponse.get("version"), apiVersion, "API version was not imported correctly");

        HttpResponse response = restAPIPublisher.deleteAPI(apiId);
        verifyResponse(response);

    }

    private void assertGETResource(JSONObject resource) throws JSONException {
        String path = resource.get("target").toString();
        Assert.assertEquals(path, "/pets/{petId}", "API resource path not imported correctly");

        String httpMethod = resource.get("verb").toString();
        Assert.assertEquals(httpMethod, "GET", "API resource's httpMethod not imported correctly");

        String authType = resource.get("authType").toString();
        Assert.assertEquals(authType, "Application & Application User", "API resource's authType not imported correctly");
    }

    private void assertPOSTResource(JSONObject resource) throws JSONException {
        String path = resource.get("target").toString();
        Assert.assertEquals(path, "/pets", "API resource path not imported correctly");

        String httpMethod = resource.get("verb").toString();
        Assert.assertEquals(httpMethod, "POST", "API resource's httpMethod not imported correctly");

        String authType = resource.get("authType").toString();
        Assert.assertEquals(authType, "Application & Application User", "API resource's authType not imported correctly");
    }

    private String uploadFile(String uploadfile){
        String file = resourceLocation+ File.separator +"swaggerFiles/"+uploadfile;
        String[] command = {"curl", "-k", "-v", "-F","file=@"+file ,"https://file.io"};

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        String curlResult = "";
        String line = "";

        try {
            Process process = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while (true) {
                line = r.readLine();
                if (line == null) {
                    break;
                }
                curlResult = curlResult + line;

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String  filename=StringUtils.substringBetween(curlResult, "link\":\"https://file.io/", "\",\"expiry\"");
        link = "https://file.io/"+filename;
        return link ;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
//        clean the data
        try {
            for (String apiId : apiIdList) {
                restAPIPublisher.deleteAPI(apiId);
            }
        } catch (ApiException e) {
            e.printStackTrace();
        }
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            deleteUser(API_CREATOR_PUBLISHER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
            deleteUser(API_SUBSCRIBER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteUser(API_CREATOR_PUBLISHER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deleteUser(API_SUBSCRIBER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deactivateAndDeleteTenant(ScenarioTestConstants.TENANT_WSO2);
        }

    }

    @DataProvider
    public static Object[][] userModeDataProvider() throws Exception {
        setup();
        // return the relevant parameters for each test run
        // 1) Super tenant API creator
        // 2) Tenant API creator
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_USER},
//                new Object[]{TestUserMode.TENANT_USER},
        };
    }

}

