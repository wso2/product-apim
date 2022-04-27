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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.am.scenario.test.common.ScenarioTestUtils;
import org.wso2.carbon.automation.engine.context.TestUserMode;

public class RESTApiCreationUsingOASDocNegativeTestCase extends ScenarioTestBase {
    private static final Log log = LogFactory.getLog(RESTApiCreationUsingOASDocNegativeTestCase.class);

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_USERNAME = "admin@wso2.com";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";

    private File swaggerFile;
    private String swaggerUrl;
    private String type = "rest";
    private String apiName;
    private String apiVersion;
    private String apiContext;
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
    public RESTApiCreationUsingOASDocNegativeTestCase(TestUserMode userMode) {
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


    @Test(description = "1.1.2.7")
    public void createApiWithInvalidOAS3DocumentAsJSONFile() throws Exception {

        swaggerFile = new File(resourceLocation + File.separator + "swaggerFiles/InvalidOAS3Document.json");

        APIDTO apiDTO = new APIDTO();
        List<APIOperationsDTO> apiOperationsDTOs = new ArrayList<>();
        List<String> tagList = new ArrayList<>();

        String payload = ScenarioTestUtils.readFromFile(swaggerFile.getAbsolutePath());
        JSONObject jsonPayload = new JSONObject(payload);
        apiName = "Swagger3SampleAPI2";
        apiContext = "basePath2";
        apiVersion = "1.0.0";
        JSONObject paths = jsonPayload.getJSONObject("paths");
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            apiName=apiName+TestUserMode.TENANT_USER;
            apiContext=apiContext+TestUserMode.TENANT_USER;}
        apiDTO.setName(apiName);
        apiDTO.setContext(apiContext);
        apiDTO.setVersion(apiVersion);
        apiDTO.setProvider(apiProviderName);

        APIDTO responseAPIDTO = restAPIPublisher.addAPI(apiDTO, OAS_V3);
        apiId = responseAPIDTO.getId();
        apiIdList.add(apiId);
        Assert.assertEquals(responseAPIDTO.getLifeCycleStatus(), "CREATED");
        boolean updatedSwaggerSuccessfully = true;
    try {
        String swaggerResponse = restAPIPublisher.updateSwagger(apiId, payload);
    } catch (ApiException e) {
        Assert.assertTrue(e.getResponseBody().contains("Error while parsing OpenAPI definition"),"Invalid OAS Document was updated successfully!");
        }
    }

    @Test(description = "1.1.2.8", enabled = false)
    public void testCreateApiUsingInvalidOASDocumentFromJsonURL() throws Exception {
        String swaggerFileName = "InvalidOAS2Document.json";

        uploadFile(swaggerFileName);
        APIDTO apiDTO = new APIDTO();
        List<APIOperationsDTO> apiOperationsDTOs = new ArrayList<>();
        List<String> tagList = new ArrayList<>();
        String swaggerVersion;
        JSONObject json;
        String payload = ScenarioTestUtils.readFromURL(link);
        json = new JSONObject(payload);

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

        try {
          String swaggerResponse = restAPIPublisher.updateSwagger(apiId, payload);
             }
        catch (ApiException e) {
        Assert.assertTrue(e.getResponseBody().contains("No resources found"),"Invalid OAS Document was updated successfully!");
        }
    }

    private String uploadFile(String uploadfile){
        String file = resourceLocation+ File.separator +"swaggerFiles/"+uploadfile;
        String[] command = {"curl", "-k", "-v", "-F","file=@"+file ,"https://file.io"};

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        String curlResult = null;
        String line = "";
    while (curlResult!=null) {
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
}
    String filename = StringUtils.substringBetween(curlResult, "link\":\"https://file.io/", "\",\"expiry\"");
    Assert.assertNotNull(filename , "File not uploaded successfully!");
    link = "https://file.io/"+filename;
    return link ;
    }

    @AfterMethod(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        RestAPIPublisherImpl restAPIPublisherNew;
        restAPIPublisherNew = new RestAPIPublisherImpl(ADMIN_USERNAME, ADMIN_PW, publisherContext.getContextTenant().getDomain(), publisherURLHttps);
        try {
            for (String apiId : apiIdList) {
                restAPIPublisherNew.deleteAPI(apiId);
            }
        } catch (ApiException e) {
            e.printStackTrace();
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
//        clean the data
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            // deleteUser(API_CREATOR_PUBLISHER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
            // deleteUser(API_SUBSCRIBER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            // deleteUser(API_CREATOR_PUBLISHER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            // deleteUser(API_SUBSCRIBER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            // deactivateAndDeleteTenant(ScenarioTestConstants.TENANT_WSO2);
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
            new Object[]{TestUserMode.TENANT_USER},
            };
    }

}

