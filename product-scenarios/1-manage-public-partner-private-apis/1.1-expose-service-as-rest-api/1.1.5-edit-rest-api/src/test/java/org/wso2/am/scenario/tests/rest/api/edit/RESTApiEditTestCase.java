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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RESTApiEditTestCase extends ScenarioTestBase {

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;

    private String apiName = UUID.randomUUID().toString();
    private String apiContext = "/" + UUID.randomUUID();
    private String apiVersion = "1.0.0";
    private final String admin = "admin";
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
    private String defaultVersionChecked = "default_version";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";
    private APICreationRequestBean apiCreationRequestBean;
    private File swaggerFile;
    String resourceLocation = System.getProperty("test.resource.location");
    private static final Log log = LogFactory.getLog(RESTApiEditTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException {

        apiPublisher = new APIPublisherRestClient(publisherURL);
        createUsers();
        apiPublisher.login(APICreator, pw);

        apiStore = new APIStoreRestClient(storeURL);
        apiStore.login(APISubscriber, subscriberPw);

        //Create an API
        try {
            apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion, APICreator,
                    new URL(backendEndPoint));
        } catch (MalformedURLException e) {
            throw new APIManagerIntegrationTestException("MalformedURLException for URL : " + backendEndPoint);
        }
        apiCreationRequestBean.setTags(tag);
        apiCreationRequestBean.setDescription(description);
        apiCreationRequestBean.setTiersCollection(tierCollection);
        apiCreationRequestBean.setDefaultVersion(defaultVersionChecked);
        apiCreationRequestBean.setDefaultVersionChecked(defaultVersionChecked);
        apiCreationRequestBean.setBizOwner(bizOwner);
        apiCreationRequestBean.setBizOwnerMail(bizOwnerMail);
        apiCreationRequestBean.setTechOwner(techOwner);
        apiCreationRequestBean.setTechOwnerMail(techOwnerMail);
    }

    @Test(description = "1.1.5.1")
    public void testRESTAPIEditAlreadyCreatedApi() throws Exception {

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when creating the API");
        verifyResponse(apiCreationResponse);

        //Check availability of the API in publisher
        HttpResponse apiResponsePublisher = apiPublisher.getAPI
                (apiName, APICreator, apiVersion);
        verifyResponse(apiResponsePublisher);
        assertTrue(apiResponsePublisher.getData().contains(apiName), apiName + " is not visible in publisher");

        //Update API with the description and tiersCollection & validate the result
        apiCreationRequestBean.setDescription("Description Changed");
        apiCreationRequestBean.setTiersCollection("Unlimited,Gold,Bronze");

        HttpResponse apiUpdateResponse = apiPublisher.updateAPI(apiCreationRequestBean);
        verifyResponse(apiUpdateResponse);

        //Check whether API is updated from the above request
        HttpResponse apiUpdateResponsePublisher = apiPublisher.getAPI
                (apiName, APICreator, apiVersion);
        assertTrue(apiUpdateResponsePublisher.getData().contains(apiName),
                apiName + " is not updated");
        assertTrue(apiUpdateResponsePublisher.getData().contains("Description Changed"),
                "Description of the " + apiName + " is not updated");
        assertTrue(apiUpdateResponsePublisher.getData().contains("Unlimited"),
                "Tier Collection of the " + apiName + " is not updated");
        assertTrue(apiUpdateResponsePublisher.getData().contains("Bronze"),
                "Tier Collection of the " + apiName + " is not updated");
        assertTrue(apiUpdateResponsePublisher.getData().contains("Gold"),
                "Tier Collection of the " + apiName + " is not updated");

    }

    /*
     *  Tests for Edit api using OAS JSON
     *
     * */
    @Test(description = "1.1.5.3", dataProvider = "OASDocsWithJSONFiles", dataProviderClass = ScenarioDataProvider.class)
    public void testRESTAPIEditUsingOASJSON(String fileName) throws Exception {

        //Check availability of the API in publisher
        HttpResponse apiResponsePublisher = apiPublisher.getAPI(apiName, APICreator, apiVersion);
        verifyResponse(apiResponsePublisher);
        assertTrue(apiResponsePublisher.getData().contains(apiName), apiName + " is not visible in publisher");

        swaggerFile = new File(resourceLocation + File.separator + fileName);

        //Create resource bean array from the OAS doc
        String payload = readFromFile(swaggerFile.getAbsolutePath());
        JSONObject json = new JSONObject(payload);
        String description = json.getJSONObject("info").get("description").toString();
        JSONObject paths = json.getJSONObject("paths");
        APIResourceBean resourceBean = null;
        ArrayList<APIResourceBean> resourceBeanArrayList = new ArrayList<>();
        Iterator<String> keys = paths.keys();
        String resourcePath = "";
        while (keys.hasNext()) {
            resourcePath = keys.next();
            String method = paths.getJSONObject(resourcePath).keys().next().toString();
            resourceBean = new APIResourceBean(method, "Any", "Unlimitted", resourcePath);
            resourceBeanArrayList.add(resourceBean);
        }

        //Update API with the description and resources & validate the result
        apiCreationRequestBean.setDescription(description);
        apiCreationRequestBean.setResourceBeanList(resourceBeanArrayList);
        HttpResponse apiUpdateResponse = apiPublisher.updateAPI(apiCreationRequestBean);
        verifyResponse(apiUpdateResponse);

        //Check whether API is updated from the above request
        HttpResponse apiUpdateResponsePublisher = apiPublisher.getAPI
                (apiName, APICreator, apiVersion);
        assertTrue(apiUpdateResponsePublisher.getData().contains(apiName),
                apiName + " is not updated"); // Name should not get changed.
        assertTrue(apiUpdateResponsePublisher.getData().contains("This is a sample for OAS JSON document"),
                "Description of the " + apiName + " is not updated");
        assertTrue(apiUpdateResponsePublisher.getData().contains(resourcePath),
                "Resources of the " + apiName + " is not updated");
    }

    /*
     *  Tests for Edit api using OAS YAML
     *
     * */
    @Test(description = "1.1.5.7", dataProvider = "OASDocsWithYAMLFiles", dataProviderClass = ScenarioDataProvider.class)
    public void testRESTAPIEditUsingOASYAML(String fileName) throws Exception {

        //Check availability of the API in publisher
        HttpResponse apiResponsePublisher = apiPublisher.getAPI(apiName, APICreator, apiVersion);
        verifyResponse(apiResponsePublisher);
        assertTrue(apiResponsePublisher.getData().contains(apiName), apiName + " is not visible in publisher");

        swaggerFile = new File(resourceLocation + File.separator + fileName);

        //Create resource bean array from the OAS doc
        String payload = readFromFile(swaggerFile.getAbsolutePath());
        Yaml yaml = new Yaml();
        Map<String, Object> map = (Map<String, Object>) yaml.load(payload);
        JSONObject json = new JSONObject(map);
        String description = json.getJSONObject("info").get("description").toString();
        JSONObject paths = json.getJSONObject("paths");
        APIResourceBean resourceBean = null;
        ArrayList<APIResourceBean> resourceBeanArrayList = new ArrayList<>();
        Iterator<String> keys = paths.keys();
        String resourcePath = "";
        while (keys.hasNext()) {
            resourcePath = keys.next();
            String method = paths.getJSONObject(resourcePath).keys().next().toString();
            resourceBean = new APIResourceBean(method, "Any", "Unlimitted", resourcePath);
            resourceBeanArrayList.add(resourceBean);
        }

        //Update API with the description and resources & validate the result
        apiCreationRequestBean.setDescription(description);
        apiCreationRequestBean.setResourceBeanList(resourceBeanArrayList);
        HttpResponse apiUpdateResponse = apiPublisher.updateAPI(apiCreationRequestBean);
        verifyResponse(apiUpdateResponse);

        //Check whether API is updated from the above request
        HttpResponse apiUpdateResponsePublisher = apiPublisher.getAPI
                (apiName, APICreator, apiVersion);
        assertTrue(apiUpdateResponsePublisher.getData().contains(apiName),
                apiName + " is not updated"); // Name should not get changed.
        assertTrue(apiUpdateResponsePublisher.getData().contains("This is a sample for OAS YAML document"),
                "Description of the " + apiName + " is not updated");
        assertTrue(apiUpdateResponsePublisher.getData().contains(resourcePath),
                "Resources of the " + apiName + " is not updated");
    }

    @Test(description = "1.1.5.8", dataProvider = "APITags", dataProviderClass = ScenarioDataProvider.class)
    public void testRESTAPIEditTags(String tags) throws Exception {

        String apiName = "TestTagsUpdateAPI";
        String apiContext = "/ctx";
        String apiVersion = "1.0.0";
        APICreationRequestBean apiCreationRequestBeanObj = new APICreationRequestBean(apiName, apiContext,
                apiVersion, APICreator, new URL(backendEndPoint));
        apiCreationRequestBeanObj.setTags(tag);
        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBeanObj);
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when creating the API");
        verifyResponse(apiCreationResponse);

        //Check availability of the API and the tag in publisher
        HttpResponse apiResponsePublisher = apiPublisher.getAPI
                (apiName, APICreator, apiVersion);
        verifyResponse(apiResponsePublisher);
        assertTrue(apiResponsePublisher.getData().contains(tag), apiName + " does not have the tag " + tag);

        //remove tags from the API and update with new tags
        apiCreationRequestBeanObj.setTags(tags);

        HttpResponse apiUpdateResponse = apiPublisher.updateAPI(apiCreationRequestBeanObj);
        verifyResponse(apiUpdateResponse);

        //Check whether API is updated from the above request
        HttpResponse apiUpdateResponsePublisher = apiPublisher.getAPI
                (apiName, APICreator, apiVersion);
        verifyTagsUpdatedInPublisherAPI(apiUpdateResponsePublisher, apiName, tags);

        // Verify new tags are updated in the store
        isTagsVisibleInStore(APICreator, apiName, apiVersion, tags, apiStore);

        apiCreationRequestBeanObj.setTags(tag); //reset to default tag value
        HttpResponse serviceResponse = apiPublisher.deleteAPI(apiName, apiVersion, APICreator);
        verifyResponse(serviceResponse);
    }

    @Test(description = "1.1.5.9")
    public void testRESTAPIEditAddMoreTags() throws Exception {

        String apiName = "TestTagsUpdateAPI";
        String apiContext = "/ctx";
        String apiVersion = "1.0.0";
        APICreationRequestBean apiCreationRequestBeanObj = new APICreationRequestBean(apiName, apiContext,
                apiVersion, APICreator, new URL(backendEndPoint));
        apiCreationRequestBeanObj.setTags(tag);
        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBeanObj);
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when creating the API");
        verifyResponse(apiCreationResponse);

        //Check availability of the API and the tag in publisher
        HttpResponse apiResponsePublisher = apiPublisher.getAPI(apiName, APICreator, apiVersion);
        verifyResponse(apiResponsePublisher);
        assertTrue(apiResponsePublisher.getData().contains(tag), apiName + " does not have the tag " + tag);

        // add more tags
        String newTagAdded = apiCreationRequestBeanObj.getTags() + ",additionalTag";
        apiCreationRequestBeanObj.setTags(newTagAdded);
        //Check whether API is updated from the above request
        HttpResponse apiUpdateResponse = apiPublisher.updateAPI(apiCreationRequestBeanObj);
        verifyResponse(apiUpdateResponse);
        HttpResponse apiUpdateResponsePublisher2 = apiPublisher.getAPI(apiName, APICreator, apiVersion);
        verifyTagsUpdatedInPublisherAPI(apiUpdateResponsePublisher2, apiName, newTagAdded);

        apiCreationRequestBeanObj.setTags(tag); //reset to default tag value
        HttpResponse serviceResponse = apiPublisher.deleteAPI(apiName, apiVersion, APICreator);
        verifyResponse(serviceResponse);
    }

    /*
     *  Create Users that can be used in each test case in this class
     *  @throws APIManagerIntegrationTestException
     * */
    private void createUsers() throws APIManagerIntegrationTestException {

        try {
            createUser(APICreator, pw,
                    new String[]{ScenarioTestConstants.CREATOR_ROLE}, admin, admin);
            createUser(APISubscriber, subscriberPw,
                    new String[]{ScenarioTestConstants.SUBSCRIBER_ROLE}, admin, admin);

        } catch (APIManagementException e) {
            throw new APIManagerIntegrationTestException("Error occurred while creating users", e);
        }
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

        apiPublisher.login(APICreator, pw);
        HttpResponse serviceResponse = apiPublisher.deleteAPI(apiName, apiVersion, APICreator);
        verifyResponse(serviceResponse);
        deleteUser(APICreator, admin, admin);
        deleteUser(APISubscriber, admin, admin);
    }
}
