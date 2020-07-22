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
package org.wso2.am.scenario.tests.rest.api.edit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.yaml.snakeyaml.Yaml;

public class RESTApiEditTestCase extends ScenarioTestBase {
    private static final Log log = LogFactory.getLog(RESTApiEditTestCase.class);

    private APICreationRequestBean apiCreationRequestBean;
    private APIDTO apidto;

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_USERNAME = "admin@wso2.com";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";

    private String apiName = "RESTApiEditTestCaseApi";
    private String apiContext = "/test5";
    private String apiVersion = "1.0.0";
    private String APICreator = "APICreatorEdit";
    private String pw = "wso2123$";
    private String APISubscriber = "APISubscriber";
    private String subscriberPw = "wso2123$";
    private String description = "This is a API creation description";
    private String tag = "APICreationTag";
    private String tierCollection = "Gold,Bronze";
    private String bizOwner = "wso2Test";
    private String bizOwnerMail = "wso2test@gmail.com";
    private String techOwner = "wso2";
    private String techOwnerMail = "wso2@gmail.com";
    private String defaultVersionChecked = "true";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";

    private String apiProductionEndPointUrl;
    private String apiProviderName;
    private String apiId;
    private String apiProductionEndpointPostfixUrl = "jaxrs_basic/services/customers/" + "customerservice/customers/123";
    private List<String> apiIdList = new ArrayList<>();

    private File swaggerFile;
    String resourceLocation = System.getProperty("test.resource.location");

    @Factory(dataProvider = "userModeDataProvider")
    public RESTApiEditTestCase(TestUserMode userMode) {
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
            // create user in wso2.com tenant
            addTenantAndActivate(ScenarioTestConstants.TENANT_WSO2, ADMIN_USERNAME, ADMIN_PW);
            if (isActivated(ScenarioTestConstants.TENANT_WSO2)) {
                //Add and activate wso2.com tenant
                createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                        TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
                createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, TENANT_ADMIN_USERNAME,
                        TENANT_ADMIN_PW);
            }
        }
        super.init(userMode);
        apiProviderName = publisherContext.getContextTenant().getContextUser().getUserName();
        log.info("Provider in RESTApiEditTestCase " + apiProviderName);

        //Create an API
        apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion, apiProviderName, new URL(backendEndPoint));
        apiCreationRequestBean.setTags(tag);
        apiCreationRequestBean.setDescription(description);
        apiCreationRequestBean.setTiersCollection(tierCollection);
        apiCreationRequestBean.setDefaultVersion(defaultVersionChecked);
        apiCreationRequestBean.setDefaultVersionChecked(defaultVersionChecked);
        apiCreationRequestBean.setBizOwner(bizOwner);
        apiCreationRequestBean.setBizOwnerMail(bizOwnerMail);
        apiCreationRequestBean.setTechOwner(techOwner);
        apiCreationRequestBean.setTechOwnerMail(techOwnerMail);

        apidto = restAPIPublisher.addAPI(apiCreationRequestBean);
        apiId = apidto.getId();
        apiIdList.add(apiId);
        assertTrue(StringUtils.isNotEmpty(apiId), "Error occured when creating api");
    }

    @Test(description = "1.1.5.1")
    public void testRESTAPIEditAlreadyCreatedApi() throws Exception {

        //Check availability of the API in publisher
        HttpResponse apiResponsePublisher = restAPIPublisher.getAPI(apiId);
        verifyResponse(apiResponsePublisher);
        JSONObject resJson = new JSONObject(apiResponsePublisher.getData());
        assertEquals(resJson.get("name"), apiName, apiName + " is not visible in publisher");

        //Update API with the description and tiersCollection & validate the result
        apidto.setDescription("Description Changed");
        List<String> tiersCollectionList = new ArrayList<>();
        tiersCollectionList.add("Unlimited");
        tiersCollectionList.add("Gold");
        tiersCollectionList.add("Bronze");
        apidto.setPolicies(tiersCollectionList);

        APIDTO apidtoResponse = restAPIPublisher.updateAPI(apidto);
        assertNotNull(apidtoResponse, "Response object is null");

        //Check whether API is updated from the above request
        HttpResponse apiUpdateResponsePublisher = restAPIPublisher.getAPI(apiId);
        verifyResponse(apiUpdateResponsePublisher);
        JSONObject responseJson = new JSONObject(apiUpdateResponsePublisher.getData());
        assertEquals(responseJson.get("name").toString(), apiName, apiName + " is not updated");
        assertEquals(responseJson.get("description").toString(), "Description Changed", "Description of the " + apiName + " is not updated");
        assertEquals(responseJson.getJSONArray("policies").length(), 3, "Tier Collection of the " + apiName + " is not updated");
    }

    /*
     *  Tests for Edit api using OAS JSON
     *
     * */
//    TODO : Investgate test failures and fix
    @Test(description = "1.1.5.3", dataProvider = "OASDocsWithJSONFiles", dataProviderClass = ScenarioDataProvider.class
            , dependsOnMethods = "testRESTAPIEditAlreadyCreatedApi")
    public void testRESTAPIEditUsingOASJSON(String fileName) throws Exception {

        //Check availability of the API in publisher
        HttpResponse apiResponsePublisher = restAPIPublisher.getAPI(apiId);
        verifyResponse(apiResponsePublisher);
        JSONObject resJson = new JSONObject(apiResponsePublisher.getData());
        assertEquals(resJson.get("name"), apiName, apiName + " is not visible in publisher");

        swaggerFile = new File(resourceLocation + File.separator + fileName);

        //Create resource bean array from the OAS doc
        String payload = readFromFile(swaggerFile.getAbsolutePath());
        JSONObject json = new JSONObject(payload);
        String description = json.getJSONObject("info").get("description").toString();
        JSONObject paths = json.getJSONObject("paths");

        List<APIOperationsDTO> apiOperationsDTOs = new ArrayList<>();

        Iterator<String> keys = paths.keys();
        String resourcePath = "";
        while (keys.hasNext()) {
            APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
            resourcePath = keys.next();
            String method = paths.getJSONObject(resourcePath).keys().next().toString().toUpperCase();
            apiOperationsDTO.setVerb(method);
            apiOperationsDTO.setTarget(resourcePath);
            apiOperationsDTOs.add(apiOperationsDTO);
        }

        //Update API with the description and resources & validate the result
        apidto.setDescription(description);
        apidto.setOperations(apiOperationsDTOs);

        APIDTO apidtoResponse = restAPIPublisher.updateAPI(apidto);
        assertNotNull(apidtoResponse, "Response object is null");

        //Check whether API is updated from the above request
        HttpResponse apiUpdateResponsePublisher = restAPIPublisher.getAPI(apiId);
        verifyResponse(apiResponsePublisher);
        JSONObject responseJson = new JSONObject(apiUpdateResponsePublisher.getData());
        assertEquals(responseJson.get("name").toString(), apiName, apiName + " is not updated");
        assertEquals(responseJson.get("description").toString(), description, "Description of the " + apiName + " is not updated");
        if (responseJson.getJSONArray("operations").length() > 1) {
            assertEquals(responseJson.getJSONArray("operations").getJSONObject(1).get("target"), resourcePath, "Resources of the " + apiName + " is not updated");
        } else {
            assertEquals(responseJson.getJSONArray("operations").getJSONObject(0).get("target"), resourcePath, "Resources of the " + apiName + " is not updated");
        }
    }

    /*
     *  Tests for Edit api using OAS YAML
     *
     * */
    @Test(description = "1.1.5.7", dataProvider = "OASDocsWithYAMLFiles", dataProviderClass = ScenarioDataProvider.class
            , dependsOnMethods = "testRESTAPIEditUsingOASJSON")
    public void testRESTAPIEditUsingOASYAML(String fileName) throws Exception {

        //Check availability of the API in publisher
        HttpResponse apiResponsePublisher = restAPIPublisher.getAPI(apiId);
        verifyResponse(apiResponsePublisher);
        JSONObject resJson = new JSONObject(apiResponsePublisher.getData());
        assertEquals(resJson.get("name"), apiName, apiName + " is not visible in publisher");

        swaggerFile = new File(resourceLocation + File.separator + fileName);

        //Create resource bean array from the OAS doc
        String payload = readFromFile(swaggerFile.getAbsolutePath());
        Yaml yaml = new Yaml();
        Map<String, Object> map = (Map<String, Object>) yaml.load(payload);
        JSONObject json = new JSONObject(map);
        String description = json.getJSONObject("info").get("description").toString();
        JSONObject paths = json.getJSONObject("paths");

        List<APIOperationsDTO> apiOperationsDTOs = new ArrayList<>();

        Iterator<String> keys = paths.keys();
        String resourcePath = "";
        while (keys.hasNext()) {
            APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
            resourcePath = keys.next();
            String method = paths.getJSONObject(resourcePath).keys().next().toString().toUpperCase();
            apiOperationsDTO.setVerb(method);
            apiOperationsDTO.setTarget(resourcePath);
            apiOperationsDTOs.add(apiOperationsDTO);
        }

        //Update API with the description and resources & validate the result
        apidto.setDescription(description);
        apidto.setOperations(apiOperationsDTOs);

        APIDTO apidtoResponse = restAPIPublisher.updateAPI(apidto);
        assertNotNull(apidtoResponse, "Response object is null");

        //Check whether API is updated from the above request
        HttpResponse apiUpdateResponsePublisher = restAPIPublisher.getAPI(apiId);
        verifyResponse(apiUpdateResponsePublisher);
        JSONObject responseJson = new JSONObject(apiUpdateResponsePublisher.getData());
        assertEquals(responseJson.get("name").toString(), apiName, apiName + " is not updated");
        assertEquals(responseJson.get("description").toString(), description, "Description of the " + apiName + " is not updated");
        if (responseJson.getJSONArray("operations").length() > 1) {
            assertEquals(responseJson.getJSONArray("operations").getJSONObject(1).get("target"), resourcePath, "Resources of the " + apiName + " is not updated");
        } else {
            assertEquals(responseJson.getJSONArray("operations").getJSONObject(0).get("target"), resourcePath, "Resources of the " + apiName + " is not updated");
        }
    }

    @Test(description = "1.1.5.8", dataProvider = "APITags", dataProviderClass = ScenarioDataProvider.class
            , dependsOnMethods = "testRESTAPIEditUsingOASYAML")
    public void testRESTAPIEditTags(String tags) throws Exception {

        String apiName = "TestTagsUpdateAPI";
        String apiContext = "/ctx";
        String apiVersion = "1.0.0";

        APIDTO apiCreationDTOObj = new APIDTO();
        apiCreationDTOObj.setName(apiName);
        apiCreationDTOObj.setContext(apiContext);
        apiCreationDTOObj.setVersion(apiVersion);
        apiCreationDTOObj.setProvider(apiProviderName);

        List<String> tagList = new ArrayList<>();
        tagList.add(tag);

        apiCreationDTOObj.setTags(tagList);

        APIDTO apidtoCreate = restAPIPublisher.addAPI(apiCreationDTOObj, "v3");
        String apiIdNew = apidtoCreate.getId();
        assertTrue(StringUtils.isNotEmpty(apiIdNew), "Error occured when creating api");

        //Check availability of the API and the tag in publisher
        HttpResponse apiResponsePublisher = restAPIPublisher.getAPI(apiIdNew);
        verifyResponse(apiResponsePublisher);
        JSONObject responseJson = new JSONObject(apiResponsePublisher.getData());

        assertEquals(responseJson.getJSONArray("tags").get(0), tag, apiName + " does not have the tag " + tag);

        //remove tags from the API and update with new tags
        List<String> newTagList = Arrays.asList(tags.split(","));
        apidtoCreate.setTags(newTagList);

        APIDTO apidtoResponse = restAPIPublisher.updateAPI(apidtoCreate);
        assertNotNull(apidtoResponse, "Response object is null");

        //Check whether API is updated from the above request
        HttpResponse apiUpdateResponsePublisher = restAPIPublisher.getAPI(apiIdNew);
        verifyResponse(apiUpdateResponsePublisher);
        verifyTagsUpdatedInPublisherAPI(apiUpdateResponsePublisher, apiName, tags);

        // Verify new tags are updated in the store
//        isTagsVisibleInStore(apiProviderName, apiName, apiVersion, tags, apiStore);

        HttpResponse serviceResponse = restAPIPublisher.deleteAPI(apiIdNew);
        verifyResponse(serviceResponse);
    }

    @Test(description = "1.1.5.9"
            , dependsOnMethods = "testRESTAPIEditTags")
    public void testRESTAPIEditAddMoreTags() throws Exception {

        String apiName = "TestTagsUpdateAPI";
        String apiContext = "/ctx";
        String apiVersion = "1.0.0";

        APIDTO apiCreationDTOObj = new APIDTO();
        apiCreationDTOObj.setName(apiName);
        apiCreationDTOObj.setContext(apiContext);
        apiCreationDTOObj.setVersion(apiVersion);
        apiCreationDTOObj.setProvider(apiProviderName);

        List<String> tagList = new ArrayList<>();
        tagList.add(tag);

        apiCreationDTOObj.setTags(tagList);

        APIDTO apidto = restAPIPublisher.addAPI(apiCreationDTOObj, "v3");
        String apiIdNew = apidto.getId();
        assertTrue(StringUtils.isNotEmpty(apiIdNew), "Error occured when creating api");

        //Check availability of the API and the tag in publisher
        HttpResponse apiResponsePublisher = restAPIPublisher.getAPI(apiIdNew);
        verifyResponse(apiResponsePublisher);
        JSONObject responseJson = new JSONObject(apiResponsePublisher.getData());
        assertEquals(responseJson.getJSONArray("tags").get(0), tag, apiName + " does not have the tag " + tag);

        // add more tags
        Gson g = new Gson();
        apidto = g.fromJson(apiResponsePublisher.getData(), APIDTO.class);
        List<String> tagListNew = apidto.getTags();
        tagListNew.add("additionalTag");
        apidto.setTags(tagListNew);

        APIDTO apidtoResponse = restAPIPublisher.updateAPI(apidto);
        assertNotNull(apidtoResponse, "Response object is null");

        HttpResponse apiUpdateResponsePublisher2 = restAPIPublisher.getAPI(apiIdNew);
        verifyResponse(apiUpdateResponsePublisher2);
        verifyTagsUpdatedInPublisherAPI(apiUpdateResponsePublisher2, apiName, tag + ",additionalTag");

        HttpResponse serviceResponse = restAPIPublisher.deleteAPI(apiIdNew);
        verifyResponse(serviceResponse);
    }

    /*
     * This method is used to read a file (OAS doc)
     * @param fileName name of the file
     * @Return file content as a string
     * */
    public static String readFromFile(String fileName) throws IOException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
        StringBuilder stringBuilder = new StringBuilder();
        int charAt;
        while ((charAt = bufferedReader.read()) != -1) {
            stringBuilder.append((char) charAt);
        }
        String payloadText = stringBuilder.toString();
        return payloadText;
    }

    @AfterTest(alwaysRun = true)
    public void destroy() throws Exception {
        for (String apiId : apiIdList) {
            restAPIPublisher.deleteAPI(apiId);
        }

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            // deleteUser(API_CREATOR_PUBLISHER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
            // deleteUser(API_SUBSCRIBER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            // deleteUser(API_CREATOR_PUBLISHER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            // deleteUser(API_SUBSCRIBER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            // deactivateAndDeleteTenant(ScenarioTestConstants.TENANT_WSO2);
        }
//        verifyResponse(serviceResponse);
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
