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
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * "Block an API and check its accessibility in the API Store."
 */
public class AccessibilityOfBlockAPITestCase extends APIManagerLifecycleBaseTest {
    private APIIdentifier apiIdentifierAPI1Version1;

    private String applicationName;

    private Map<String, String> requestHeaders;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        apiIdentifierAPI1Version1 = new APIIdentifier(API1_PROVIDER_NAME, API1_NAME, API_VERSION1);
        applicationName =
                (this.getClass().getName().replace(this.getClass().getPackage().getName(), "")).replace(".", "");
        apiStoreClientUser1.addApplication(applicationName, "", "", "");
    }


    @Test(groups = {"wso2.am"}, description = "Test invocation of the APi before block")
    public void testInvokeAPIBeforeChangeAPILifecycleToBlock() throws Exception {

        //Create and publish  and subscribe API version 1.0.0
        createPublishAndSubscribeAPI(apiIdentifierAPI1Version1, API1_CONTEXT, apiPublisherClientUser1, apiStoreClientUser1, applicationName);


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
                "Response code mismatched when invoke api before block");
        assertTrue(oldVersionInvokeResponse.getData().contains(API1_RESPONSE_DATA),
                "Response data mismatched when invoke  API  before block" +
                        " Response Data:" + oldVersionInvokeResponse.getData());

    }


    @Test(groups = {"wso2.am"}, description = "Change API lifecycle to block", dependsOnMethods = "testInvokeAPIBeforeChangeAPILifecycleToBlock")
    public void testChangeAPILifecycleToBlock() throws Exception {
        //Block the API version 1.0.0
        APILifeCycleStateRequest blockUpdateRequest =
                new APILifeCycleStateRequest(API1_NAME, API1_PROVIDER_NAME, APILifeCycleState.BLOCKED);
        blockUpdateRequest.setVersion(API_VERSION1);
        //Change API lifecycle  to Block
        HttpResponse blockAPIActionResponse =
                apiPublisherClientUser1.changeAPILifeCycleStatus(blockUpdateRequest);
        assertEquals(blockAPIActionResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertTrue(verifyAPIStatusChange(blockAPIActionResponse, APILifeCycleState.PUBLISHED,
                APILifeCycleState.BLOCKED), "API status Change is invalid when block an API :" +
                getAPIIdentifierString(apiIdentifierAPI1Version1) + " Response Code:" + blockAPIActionResponse.getData());

    }


    @Test(groups = {"wso2.am"}, description = "Invocation og the APi after block", dependsOnMethods = "testChangeAPILifecycleToBlock")
    public void testInvokeAPIAfterChangeAPILifecycleToBlock() throws Exception {

        //Invoke  old version
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(API_BASE_URL + API1_CONTEXT + "/" + API_VERSION1 + API1_END_POINT_METHOD,
                        requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_SERVICE_UNAVAILABLE,
                "Response code mismatched when invoke api after block");
        assertTrue(oldVersionInvokeResponse.getData().contains(HTTP_RESPONSE_DATA_API_BLOCK),
                "Response data mismatched when invoke  API  after block" +
                        " Response Data:" + oldVersionInvokeResponse.getData());

    }

    @AfterClass(alwaysRun = true)
    public void cleanup() throws Exception {
        apiStoreClientUser1.removeApplication(applicationName);
        deleteAPI(apiIdentifierAPI1Version1, apiPublisherClientUser1);
    }


}

