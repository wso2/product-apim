/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.api.lifecycle;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Edit the API context and check its accessibility.
 */
public class EditAPIContextAndCheckAccessibilityTestCase extends APIManagerLifecycleBaseTest {
    APIIdentifier apiIdentifierAPI1Version1;

    private String applicationName;
    private String newContext;
    private Map<String, String> requestHeaders;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        apiIdentifierAPI1Version1 = new APIIdentifier(API1_PROVIDER_NAME, API1_NAME, API_VERSION1);
        applicationName =
                (this.getClass().getName().replace(this.getClass().getPackage().getName(), "")).replace(".", "");
        apiStoreClientUser1.addApplication(applicationName, "", "", "");
    }


    @Test(groups = {"wso2.am"}, description = "Test invoke the API before the context change")
    public void testInvokeAPIBeforeChangeAPIContext() throws Exception {

        //Create and publish  and subscribe API version 1.0.0
        createPublishAndSubscribeAPI(apiIdentifierAPI1Version1, API1_CONTEXT, apiPublisherClientUser1,
                apiStoreClientUser1, applicationName);

        //get access token
        String accessToken = getAccessToken(apiStoreClientUser1, applicationName);

        // Create requestHeaders
        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        //Invoke  old version
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(API_BASE_URL + API1_CONTEXT + "/" + API_VERSION1 + API1_END_POINT_METHOD,
                        requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke api before change the context");
        assertTrue(oldVersionInvokeResponse.getData().contains(API1_RESPONSE_DATA),
                "Response data mismatched when invoke  API  before change the context" +
                        " Response Data:" + oldVersionInvokeResponse.getData());

    }


    @Test(groups = {"wso2.am"}, description = "Test changing of the API context",
            dependsOnMethods = "testInvokeAPIBeforeChangeAPIContext")
    public void testEditAPIContext() throws Exception {


        //Create the API Request with new context
        newContext = "new" + API1_CONTEXT;
        APIRequest apiRequestBean = new APIRequest(API1_NAME, newContext, new URL(API1_END_POINT_URL));
        apiRequestBean.setTags(API1_TAGS);
        apiRequestBean.setDescription(API1_DESCRIPTION);
        apiRequestBean.setVersion(API_VERSION1);
        apiRequestBean.setVisibility("public");
        //Update API with Edited information
        HttpResponse updateAPIHTTPResponse = apiPublisherClientUser1.updateAPI(apiRequestBean);

        assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Update API Response Code is invalid." + getAPIIdentifierString(apiIdentifierAPI1Version1));
        assertEquals(getValueFromJSON(updateAPIHTTPResponse, "error"), "false",
                "Error in API Update in " + getAPIIdentifierString(apiIdentifierAPI1Version1) +
                        "Response Data:" + updateAPIHTTPResponse.getData());

    }


    @Test(groups = {"wso2.am"}, description = "Test the invocation of API using old context after Context change" +
            " after the API context change", dependsOnMethods = "testEditAPIContext")
    public void testInvokeAPIAfterChangeAPIContextWithOldContext() throws Exception {

        //Invoke  old context
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(API_BASE_URL + API1_CONTEXT + "/" + API_VERSION1 + API1_END_POINT_METHOD,
                        requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Response code mismatched when invoke api before changing the context");
        assertTrue(oldVersionInvokeResponse.getData().contains(HTTP_RESPONSE_DATA_NOT_FOUND),
                "Response data mismatched when invoke  API  before changing the context" +
                        " Response Data:" + oldVersionInvokeResponse.getData());

    }


    @Test(groups = {"wso2.am"}, description = "Test the invocation of API using new context after Context change",
            dependsOnMethods = "testInvokeAPIAfterChangeAPIContextWithOldContext")
    public void testInvokeAPIAfterChangeAPIContextWithNewContext() throws Exception {
        //Invoke  new context
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(API_BASE_URL + newContext + "/" + API_VERSION1 + API1_END_POINT_METHOD,
                        requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke api after changing the context");
        assertTrue(oldVersionInvokeResponse.getData().contains(API1_RESPONSE_DATA),
                "Response data mismatched when invoke  API  after changing the context" +
                        " Response Data:" + oldVersionInvokeResponse.getData());


    }

    @AfterClass(alwaysRun = true)
    public void cleanup() throws Exception {
        apiStoreClientUser1.removeApplication(applicationName);
        deleteAPI(apiIdentifierAPI1Version1, apiPublisherClientUser1);

    }


}




