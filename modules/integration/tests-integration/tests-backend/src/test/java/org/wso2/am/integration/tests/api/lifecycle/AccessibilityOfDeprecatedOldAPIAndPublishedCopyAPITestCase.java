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
import org.wso2.am.integration.test.utils.APIMgtTestUtil;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Publish a API. Copy and create a new version, publish  the new version and deprecate the old version,
 * test invocation of both old and new API versions."
 */
public class AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase extends APIManagerLifecycleBaseTest {


    private APIIdentifier apiIdentifierAPI1Version1;
    private APIIdentifier apiIdentifierAPI1Version2;
    private String applicationName;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        apiIdentifierAPI1Version1 = new APIIdentifier(USER_NAME1, API1_NAME, API_VERSION_1_0_0);
        apiIdentifierAPI1Version2 = new APIIdentifier(USER_NAME1, API1_NAME, API_VERSION_2_0_0);
        applicationName =
                (this.getClass().getName().replace(this.getClass().getPackage().getName(), "")).replace(".", "");
        apiStoreClientUser1.addApplication(applicationName, "", "", "");
    }


    @Test(groups = {"wso2.am"}, description = "Test subscribe of old API version before deprecate the old version")
    public void testSubscribeOldVersionBeforeDeprecate() throws Exception {

        //Create and publish API version 1.0.0
        createAndPublishAPI(apiIdentifierAPI1Version1, API1_CONTEXT, apiPublisherClientUser1, false);
        // Copy to version 2.0.0 and Publish Copied API
        copyAndPublishCopiedAPI(apiIdentifierAPI1Version1, API_VERSION_2_0_0, apiPublisherClientUser1, false);

        HttpResponse oldVersionSubscribeResponse =
                subscribeToAPI(apiIdentifierAPI1Version1, applicationName, apiStoreClientUser1);
        assertEquals(oldVersionSubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of old API version request not successful " +
                        getAPIIdentifierString(apiIdentifierAPI1Version1));
        assertEquals(getValueFromJSON(oldVersionSubscribeResponse, "error"), "false",
                "Error in subscribe of old API version" + getAPIIdentifierString(apiIdentifierAPI1Version1) +
                        "Response Data:" + oldVersionSubscribeResponse.getData());
    }


    @Test(groups = {"wso2.am"}, description = "Test subscribe of new API version before deprecate the old version",
            dependsOnMethods = "testSubscribeOldVersionBeforeDeprecate")
    public void testSubscribeNewVersion() throws Exception {

        HttpResponse newVersionSubscribeResponse =
                subscribeToAPI(apiIdentifierAPI1Version2, applicationName, apiStoreClientUser1);
        assertEquals(newVersionSubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of old API version request not successful " +
                        getAPIIdentifierString(apiIdentifierAPI1Version2));
        assertEquals(getValueFromJSON(newVersionSubscribeResponse, "error"), "false",
                "Error in subscribe of old API version" + getAPIIdentifierString(apiIdentifierAPI1Version2) +
                        "Response Data:" + newVersionSubscribeResponse.getData());
    }


    @Test(groups = {"wso2.am"}, description = "Test deprecate old api version", dependsOnMethods = "testSubscribeNewVersion")
    public void testDeprecateOldVersion() throws Exception {

        APILifeCycleStateRequest deprecatedUpdateRequest =
                new APILifeCycleStateRequest(API1_NAME, USER_NAME1, APILifeCycleState.DEPRECATED);
        deprecatedUpdateRequest.setVersion(API_VERSION_1_0_0);
        HttpResponse deprecateAPIResponse =
                apiPublisherClientUser1.changeAPILifeCycleStatus(deprecatedUpdateRequest);

        assertEquals(deprecateAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API deprecate Response code is invalid " + getAPIIdentifierString(apiIdentifierAPI1Version1));
        assertTrue(verifyAPIStatusChange(deprecateAPIResponse,
                        APILifeCycleState.PUBLISHED, APILifeCycleState.DEPRECATED),
                "API deprecate status Change is invalid in" + getAPIIdentifierString(apiIdentifierAPI1Version1) +
                        "Response Data:" + deprecateAPIResponse.getData());

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in the store after API deprecate.",
            dependsOnMethods = "testDeprecateOldVersion")
    public void testVisibilityOfOldAPIInStoreAfterDeprecate() throws Exception {
        //Verify the API in API Store
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(apiStoreClientUser1.getAPI(API1_NAME));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierAPI1Version1, apiStoreAPIIdentifierList), true,
                "Old API version is not visible in API Store after deprecate." +
                        getAPIIdentifierString(apiIdentifierAPI1Version1));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in the store after API deprecate.",
            dependsOnMethods = "testVisibilityOfOldAPIInStoreAfterDeprecate")
    public void testVisibilityOfNewAPIInStore() throws Exception {
        //Verify the API in API Store
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(apiStoreClientUser1.getAPI(API1_NAME));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierAPI1Version2, apiStoreAPIIdentifierList), true,
                "New API version is not visible in API Store after deprecate the old version." +
                        getAPIIdentifierString(apiIdentifierAPI1Version2));

    }


    @Test(groups = {"wso2.am"}, description = "Test the subscription of deprecated API version.",
            dependsOnMethods = "testVisibilityOfNewAPIInStore")
    public void testSubscribeOldVersionAfterDeprecate() throws Exception {
        //subscribe deprecated old version

        HttpResponse oldVersionSubscribeResponse = subscribeToAPI
                (apiIdentifierAPI1Version1, applicationName, apiStoreClientUser2);
        assertEquals(oldVersionSubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of old API version  after deprecate response code is invalid." +
                        getAPIIdentifierString(apiIdentifierAPI1Version1));
        assertEquals(getValueFromJSON(oldVersionSubscribeResponse, "error"), "true",
                "Subscribe of old API version  after deprecate success, which should fail." +
                        getAPIIdentifierString(apiIdentifierAPI1Version1) +
                        "Response Data:" + oldVersionSubscribeResponse.getData());


    }


    @Test(groups = {"wso2.am"}, description = "Test the invocation of both deprecated old and  " +
            "publish new API versions", dependsOnMethods = "testSubscribeOldVersionAfterDeprecate")
    public void testAccessibilityOfDeprecateOldAPIAndPublishedCopyAPI() throws Exception {

        //get access token
        String accessToken = getAccessToken(apiStoreClientUser1, applicationName);

        // Create requestHeaders
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        //Invoke  old version
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(API_BASE_URL + API1_CONTEXT + "/" + API_VERSION_1_0_0 +
                        API1_END_POINT_METHOD, requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(),
                HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertTrue(oldVersionInvokeResponse.getData().contains(API1_RESPONSE_DATA), "Response data mismatched");

        //Invoke new version
        HttpResponse newVersionInvokeResponse = HttpRequestUtil.doGet(API_BASE_URL + API1_CONTEXT +
                "/" + API_VERSION_2_0_0 + API1_END_POINT_METHOD, requestHeaders);

        assertEquals(newVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched");
        assertTrue(newVersionInvokeResponse.getData().contains(API1_RESPONSE_DATA), "Response data mismatched");

    }


    @AfterClass(alwaysRun = true)
    public void cleanup() throws Exception {
        apiStoreClientUser1.removeApplication(applicationName);
        deleteAPI(apiIdentifierAPI1Version1, apiPublisherClientUser1);
        deleteAPI(apiIdentifierAPI1Version2, apiPublisherClientUser1);
    }


}
