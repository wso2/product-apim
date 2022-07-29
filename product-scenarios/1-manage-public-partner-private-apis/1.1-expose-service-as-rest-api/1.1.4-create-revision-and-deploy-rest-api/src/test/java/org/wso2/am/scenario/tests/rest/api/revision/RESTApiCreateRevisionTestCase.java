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
package org.wso2.am.scenario.tests.rest.api.revision;

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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.*;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionDeployUndeployRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionRequest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RESTApiCreateRevisionTestCase extends ScenarioTestBase {
    private static final Log log = LogFactory.getLog(RESTApiCreateRevisionTestCase.class);

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
    protected static final int HTTP_RESPONSE_CODE_OK = Response.Status.OK.getStatusCode();
    protected static final int HTTP_RESPONSE_CODE_CREATED = Response.Status.CREATED.getStatusCode();
    private String apiName = "RESTApiRevisionTestCaseApi";
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
    private String revisionUUID;
    private String apiProductionEndpointPostfixUrl = "jaxrs_basic/services/customers/" + "customerservice/customers/123";
    private List<String> apiIdList = new ArrayList<>();

    // All tests in this class will run with a super tenant API creator and a tenant API creator.
    @Factory(dataProvider = "userModeDataProvider")
    public RESTApiCreateRevisionTestCase(TestUserMode userMode) {
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
        waitForAPIDeployment();
    }

    @Test(description = "1.1.4.1")
    public void testRESTAPIRevisionCreate() throws Exception {

        //Check availability of the API in publisher
        HttpResponse apiResponsePublisher = restAPIPublisher.getAPI(apiId);
        verifyResponse(apiResponsePublisher);
        JSONObject resJson = new JSONObject(apiResponsePublisher.getData());
        assertEquals(resJson.get("name"), apiName, apiName + " is not visible in publisher");

        APIRevisionRequest apiRevisionRequest = new APIRevisionRequest();
        apiRevisionRequest.setApiUUID(apiId);
        apiRevisionRequest.setDescription("Test Revision 1");
        //Add the API Revision using the API publisher.
        HttpResponse apiRevisionResponse = restAPIPublisher.addAPIRevision(apiRevisionRequest);

        assertEquals(apiRevisionResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Create API Response Code is invalid." + apiRevisionResponse.getData());
        waitForAPIDeployment();
    }

    @Test(description = "1.1.4.2", dependsOnMethods = "testRESTAPIRevisionCreate")
    public void testGETRevisions() throws Exception {

        HttpResponse apiRevisionsGetResponse = restAPIPublisher.getAPIRevisions(apiId,null);
        assertEquals(apiRevisionsGetResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to retrieve revisions" + apiRevisionsGetResponse.getData());
        List<JSONObject> revisionList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(apiRevisionsGetResponse.getData());

        JSONArray arrayList = jsonObject.getJSONArray("list");
        for (int i = 0, l = arrayList.length(); i < l; i++) {
            revisionList.add(arrayList.getJSONObject(i));
        }
        for (JSONObject revision :revisionList) {
            revisionUUID = revision.getString("id");
        }
        assertNotNull(revisionUUID, "Unable to retrieve revision UUID");
        waitForAPIDeployment();
    }

    @Test(description = "1.1.4.3", dependsOnMethods = "testGETRevisions")
    public void testDeployAPIRevision() throws Exception {

        List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName("Default");
        apiRevisionDeployRequest.setVhost("localhost");
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        HttpResponse apiRevisionsDeployResponse = restAPIPublisher.deployAPIRevision(apiId, revisionUUID,
                apiRevisionDeployRequestList, "API");
        assertEquals(apiRevisionsDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to deploy API Revisions:" +apiRevisionsDeployResponse.getData());
        waitForAPIDeployment();
    }

    @Test(description = "1.1.4.4", dependsOnMethods = "testDeployAPIRevision")
    public void testUnDeployAPIRevisions() throws Exception {
        List<APIRevisionDeployUndeployRequest> apiRevisionUndeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionUnDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionUnDeployRequest.setName("Default");
        apiRevisionUnDeployRequest.setVhost("localhost");
        apiRevisionUnDeployRequest.setDisplayOnDevportal(true);
        apiRevisionUndeployRequestList.add(apiRevisionUnDeployRequest);
        HttpResponse apiRevisionsUnDeployResponse = restAPIPublisher.undeployAPIRevision(apiId, revisionUUID,
                apiRevisionUndeployRequestList);
        assertEquals(apiRevisionsUnDeployResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to Undeploy API Revisions:" + apiRevisionsUnDeployResponse.getData());
        waitForAPIDeployment();

    }

    @Test(description = "1.1.4.5", dependsOnMethods = "testUnDeployAPIRevisions")
    public void testRestoreAPIRevision() throws Exception {
        HttpResponse apiRevisionsRestoreResponse = restAPIPublisher.restoreAPIRevision(apiId, revisionUUID);
        assertEquals(apiRevisionsRestoreResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Unable to restore API Revisions:" + apiRevisionsRestoreResponse.getData());
        waitForAPIDeployment();
    }

    @Test(description = "1.1.4.6", dependsOnMethods = "testRestoreAPIRevision")
    public void testDeleteAPIRevision() throws Exception {
        HttpResponse apiRevisionsDeleteResponse = restAPIPublisher.deleteAPIRevision(apiId, revisionUUID);
        assertEquals(apiRevisionsDeleteResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to delete API Revisions:" + apiRevisionsDeleteResponse.getData());
        waitForAPIDeployment();
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
    }

    // This method runs prior to the @BeforeClass method.
    // Create users and tenants needed or the tests in here. Try to reuse the TENANT_WSO2 as much as possible to avoid the number of tenants growing.
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