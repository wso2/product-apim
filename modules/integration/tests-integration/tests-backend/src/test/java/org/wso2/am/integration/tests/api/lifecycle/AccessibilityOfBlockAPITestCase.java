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
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * "Block an API and check its accessibility in the API Store."
 */
public class AccessibilityOfBlockAPITestCase extends APIManagerLifecycleBaseTest {


    private static final String API_NAME = "APILifeCycleTestAPI1";
    private static final String API_CONTEXT = "testAPI1";
    private static final String API_TAGS = "youtube, video, media";
    private static final String API_END_POINT_URL = "http://gdata.youtube.com/feeds/api/standardfeeds";
    private static final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private static final String API_END_POINT_METHOD = "/most_popular";
    private static final String API_RESPONSE_DATA = "<feed";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private static final String APPLICATION_NAME = "AccessibilityOfBlockAPITestCase";
    private APIIdentifier apiIdentifier;
    private String providerName;
    private APICreationRequestBean apiCreationRequestBean;
    private Map<String, String> requestHeaders;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        providerName = apimContext.getContextTenant().getContextUser().getUserName();

        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, new URL(API_END_POINT_URL));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);

        apiPublisherClientUser1 = new APIPublisherRestClient(getPublisherServerURLHttp());
        apiStoreClientUser1 = new APIStoreRestClient(getStoreServerURLHttp());

        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
        //Login to API Store with  admin
        apiStoreClientUser1.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiStoreClientUser1.addApplication(APPLICATION_NAME, "", "", "");
    }


    @Test(groups = {"wso2.am"}, description = "Test invocation of the APi before block")
    public void testInvokeAPIBeforeChangeAPILifecycleToBlock() throws Exception {

        //Create and publish  and subscribe API version 1.0.0
        createPublishAndSubscribeToAPI(apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1, apiStoreClientUser1, APPLICATION_NAME);
        //get access token
        String accessToken = getAccessToken(apiStoreClientUser1, APPLICATION_NAME);
        // Create requestHeaders
        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        //Invoke  old version
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(API_BASE_URL + API_CONTEXT + "/" + API_VERSION_1_0_0 + API_END_POINT_METHOD,
                        requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke api before block");
        assertTrue(oldVersionInvokeResponse.getData().contains(API_RESPONSE_DATA),
                "Response data mismatched when invoke  API  before block" +
                        " Response Data:" + oldVersionInvokeResponse.getData());

    }


    @Test(groups = {"wso2.am"}, description = "Change API lifecycle to block", dependsOnMethods = "testInvokeAPIBeforeChangeAPILifecycleToBlock")
    public void testChangeAPILifecycleToBlock() throws Exception {
        //Block the API version 1.0.0
        APILifeCycleStateRequest blockUpdateRequest =
                new APILifeCycleStateRequest(API_NAME, providerName, APILifeCycleState.BLOCKED);
        blockUpdateRequest.setVersion(API_VERSION_1_0_0);
        //Change API lifecycle  to Block
        HttpResponse blockAPIActionResponse =
                apiPublisherClientUser1.changeAPILifeCycleStatus(blockUpdateRequest);
        assertEquals(blockAPIActionResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertTrue(verifyAPIStatusChange(blockAPIActionResponse, APILifeCycleState.PUBLISHED,
                APILifeCycleState.BLOCKED), "API status Change is invalid when block an API :" +
                getAPIIdentifierString(apiIdentifier) + " Response Code:" + blockAPIActionResponse.getData());

    }


    @Test(groups = {"wso2.am"}, description = "Invocation og the APi after block", dependsOnMethods = "testChangeAPILifecycleToBlock")
    public void testInvokeAPIAfterChangeAPILifecycleToBlock() throws Exception {

        //Invoke  old version
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(API_BASE_URL + API_CONTEXT + "/" + API_VERSION_1_0_0 + API_END_POINT_METHOD,
                        requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_SERVICE_UNAVAILABLE,
                "Response code mismatched when invoke api after block");
        assertTrue(oldVersionInvokeResponse.getData().contains(HTTP_RESPONSE_DATA_API_BLOCK),
                "Response data mismatched when invoke  API  after block" +
                        " Response Data:" + oldVersionInvokeResponse.getData());

    }

    @AfterClass(alwaysRun = true)
    public void cleanup() throws Exception {
        apiStoreClientUser1.removeApplication(APPLICATION_NAME);
        deleteAPI(apiIdentifier, apiPublisherClientUser1);
    }


}

