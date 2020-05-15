/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.am.scenario.test.common.ScenarioTestUtils;
import org.wso2.am.scenario.test.common.httpserver.SimpleHTTPServer;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import javax.validation.constraints.AssertFalse;

import static org.testng.Assert.assertFalse;

/*
 * This class provides test cases for creating NEW REST API from scratch Negative test scenarios.
 * 1.1.2.7
 *
 * */
public class RESTApiCreationUsingOASDocNegativeTestCase extends ScenarioTestBase {

    private APIPublisherRestClient apiPublisher;
    private APIRequest apiRequest;
    private File swaggerFile;
    private String importDefinitionUrl = "swagger-url";
    private String importDefinitionFile = "swagger-file";
    private String type = "rest";
    private String apiName;
    private String apiVersion;
    private String apiContext;
    private String swaggerUrl;
    private String APICreator = "APICreator";
    private String pw = "wso2123$";
    private final String admin = "admin";
    private final String adminPw = "admin";

    private String providerName;

    String resourceLocation = System.getProperty("test.resource.location");

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        providerName = user.getUserName();
    }

//    @Test(description = "1.1.2.7") //todo fix uploaded swagger file not reading issue
//    public void createApiWithInvalidOAS3DocumentAsJSONFile() throws Exception {
//
//        swaggerFile = new File(resourceLocation + File.separator + "swaggerFiles/InvalidOAS3Document.json");
//
//        //Import api definition from swagger file
//        apiRequest = new APIRequest(importDefinitionFile, swaggerFile.getAbsolutePath(), "", type);
//        String boundary = "===" + System.currentTimeMillis() + "===";
////        apiPublisher.sertContentType("multipart/form-data; boundary=----WebKitFormBoundary3KQfws99yVtxs0Ow");
//        HttpResponse designResponse1 = apiPublisher.designAPIWithOAS(apiRequest);
//        verifyNegativeResponse(designResponse1);
//
//    }

    @Test(description = "1.1.2.8", dataProvider = "InvaidOASDocs", dataProviderClass = ScenarioDataProvider.class)
    public void testCreateApiUsingInvalidOASDocumentFromJsonURL(String url) throws Exception {

        swaggerUrl = url;

        apiRequest = new APIRequest(importDefinitionUrl, "", swaggerUrl, type);
        SimpleHTTPServer server = new SimpleHTTPServer();
        new Thread(server).start();
        HttpResponse serviceResponse = apiPublisher.designAPIWithOASURL(apiRequest);
        server.stop();
        Assert.assertTrue(serviceResponse.getData().contains("imported"), "Error importing swagger from : " + url);

        //Save the API with imported values
        new Thread( new SimpleHTTPServer()).start();
        String payload = ScenarioTestUtils.readFromURL(swaggerUrl);
        JSONObject json = new JSONObject(payload);
        int paths = json.getJSONObject("paths").length();
        //UI prevents creating the API when there are no resources added from the swagger or manually.
        Assert.assertTrue(paths == 0, "Swagger doc contains resource paths.");
    }

    @Test(description = "1.1.2.10", dataProvider = "OASDocsWithInvalidURL", dataProviderClass = ScenarioDataProvider.class)
    public void createApiWithInvalidOAS3DocumentURL(String url) throws Exception {

        swaggerUrl = url;
        apiRequest = new APIRequest(importDefinitionUrl, "", swaggerUrl, type);

        HttpResponse serviceResponse = apiPublisher.designAPIWithOASURL(apiRequest);
        verifyNegativeResponse(serviceResponse);

    }

    @AfterClass(alwaysRun = true)
    public void RemoveAPI() throws Exception {

//        deleteUser(APICreator, admin, adminPw);
    }

    /*
     *  Create Users that can be used in each test case in this class
     *  @throws Exception
     * */
    private void createUsers() throws Exception {

        createUser(APICreator, pw, new String[]{ScenarioTestConstants.CREATOR_ROLE}, admin, adminPw);
    }
}