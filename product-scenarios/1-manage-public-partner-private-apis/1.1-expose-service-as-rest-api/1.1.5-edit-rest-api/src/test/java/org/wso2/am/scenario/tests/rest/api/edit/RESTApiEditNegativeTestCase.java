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
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.UUID;
import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RESTApiEditNegativeTestCase extends ScenarioTestBase {

    private APIPublisherRestClient apiPublisher;
    private String apiName = UUID.randomUUID().toString();
    private String apiContext = "/" + UUID.randomUUID();
    private String apiVersion = "1.0.0";
    private String APICreator = "APICreatorEdit";
    private String pw = "wso2123$";
    private String admin = "admin";
    private String adminPw = "admin";
    private String description = "This is a API creation description";
    private String tag = "APICreationTag";
    private String invalidTag = "^invalid^";
    private String tierCollection = "Gold,Bronze";
    private String bizOwner = "wso2Test";
    private String bizOwnerMail = "wso2test@gmail.com";
    private String techOwner = "wso2";
    private String techOwnerMail = "wso2@gmail.com";
    private String default_version_checked = "default_version";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";
    private String updateErrorResponse = "Error while updating the API- " + apiName + "-1.0.0";
    private APICreationRequestBean apiCreationRequestBean;
    private static final Log log = LogFactory.getLog(ScenarioTestBase.class);

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {

        createUsers();
        apiPublisher = new APIPublisherRestClient(publisherURL);
        apiPublisher.login(APICreator, pw);
        //Create an API
        createAnAPI();

    }

    @Test(description = "1.1.5.2")
    public void testRESTAPIEditWithInvalidValue() throws Exception {

        //Check availability of the API in publisher
        HttpResponse apiResponsePublisher = apiPublisher.getAPI
                (apiName, APICreator, apiVersion);
        verifyResponse(apiResponsePublisher);
        assertTrue(apiResponsePublisher.getData().contains(apiName), apiName + " is not visible in publisher");

        //Update API with an invalid tag & validate the result
        apiCreationRequestBean.setTags(invalidTag);
        HttpResponse apiUpdateResponse = apiPublisher.updateAPI(apiCreationRequestBean);
        Assert.assertTrue(apiUpdateResponse.getData().contains(updateErrorResponse));

        //Check whether the previously created api is not altered
        HttpResponse apiUpdateResponsePublisher = apiPublisher.getAPI
                (apiName, APICreator, apiVersion);
        assertTrue(apiUpdateResponsePublisher.getData().contains(tag));
    }

    @Test(description = "1.1.5.16", dataProvider = "InvalidAPITags", dataProviderClass = ScenarioDataProvider.class)
    public void testRESTAPIEditTags(String tags) throws Exception {
        //Check availability of the API and the tag in publisher
        HttpResponse apiResponsePublisher = apiPublisher.getAPI
                (apiName, APICreator, apiVersion);
        verifyResponse(apiResponsePublisher);
        assertTrue(apiResponsePublisher.getData().contains(tag), apiName + " does not have the tag " + tag);

        //remove tags from the API and update with new tags
        apiCreationRequestBean.setTags(tags);

        HttpResponse apiUpdateResponse = null;
        try {
            apiUpdateResponse = apiPublisher.updateAPI(apiCreationRequestBean);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Exception when updating API URLDecoder: Incomplete trailing"));
            return;
        }

        verifyNegativeResponse(apiUpdateResponse);

    }

    /*
     *  Create Users that can be used in each test case in this class
     *  @throws APIManagerIntegrationTestException
     * */
    private void createUsers() throws Exception {

        createUserWithCreatorRole(APICreator, pw, admin, adminPw);
    }

    @AfterTest(alwaysRun = true)
    public void destroy() throws Exception {

        HttpResponse serviceResponse = apiPublisher.deleteAPI(apiName, apiVersion, APICreator);
        verifyResponse(serviceResponse);
        deleteUser(APICreator, admin, admin);

    }

    private void createAnAPI() throws Exception {

        apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion,
                APICreator, new URL(backendEndPoint));
        apiCreationRequestBean.setTags(tag);
        apiCreationRequestBean.setDescription(description);
        apiCreationRequestBean.setTiersCollection(tierCollection);
        apiCreationRequestBean.setDefaultVersion(default_version_checked);
        apiCreationRequestBean.setDefaultVersionChecked(default_version_checked);
        apiCreationRequestBean.setBizOwner(bizOwner);
        apiCreationRequestBean.setBizOwnerMail(bizOwnerMail);
        apiCreationRequestBean.setTechOwner(techOwner);
        apiCreationRequestBean.setTechOwnerMail(techOwnerMail);

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when creating the API");
        verifyResponse(apiCreationResponse);
    }
}
