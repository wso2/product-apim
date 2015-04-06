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
 * Change the lifecycle of a copy API to published without deprecating old version.Check whether newest version is
 * listed in store. Check whether old version is still listed under more apis from same creator. Whether users can
 * still subscribe to old version. Test invocation of both old and new API versions.
 */

public class APIAccessibilityOfPublishedOldAPIAndPublishedCopyAPITestCase extends APIManagerLifecycleBaseTest {


    private APIIdentifier apiIdentifierAPI1Version1;
    private APIIdentifier apiIdentifierAPI1Version2;
    private String applicationName;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.initialize();
        apiIdentifierAPI1Version1 = new APIIdentifier(API1_PROVIDER_NAME, API1_NAME, API_VERSION1);
        apiIdentifierAPI1Version2 = new APIIdentifier(API1_PROVIDER_NAME, API1_NAME, API_VERSION2);
        applicationName = (this.getClass().getName().replace(this.getClass().getPackage().getName(), "")).replace(".", "");
        ;
        apiStoreClientUser1.addApplication(applicationName, "", "", "");
    }


    @Test(groups = {"wso2.am"}, description = " Test Copy API.Copy API version 1.0.0  to 2.0.0 ")
    public void testCopyAPI() throws Exception {

        //Create and publish API version 1.0.0
        createAndPublishAPIWithoutRequireReSubscription(
                apiIdentifierAPI1Version1, API1_CONTEXT, apiPublisherClientUser1);

        //Copy API version 1.0.0  to 2.0.0
        HttpResponse httpResponseCopyAPI =
                apiPublisherClientUser1.copyAPI(API1_PROVIDER_NAME, API1_NAME, API_VERSION1, API_VERSION2, "");

        assertEquals(httpResponseCopyAPI.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Copy API request code is invalid." + getAPIIdentifierString(apiIdentifierAPI1Version1));
        assertEquals(getValueFromJSON(httpResponseCopyAPI, "error"), "false",
                "Copy  API response data is invalid" + getAPIIdentifierString(apiIdentifierAPI1Version1) +
                        "Response Data:" + httpResponseCopyAPI.getData());

    }


    @Test(groups = {"wso2.am"}, description = " Test Copy API.", dependsOnMethods = "testCopyAPI")
    public void testPublishCopiedAPI() throws Exception {

        //Publish  version 2.0.0
        APILifeCycleStateRequest publishUpdateRequest =
                new APILifeCycleStateRequest(API1_NAME, API1_PROVIDER_NAME, APILifeCycleState.PUBLISHED);
        publishUpdateRequest.setVersion(API_VERSION2);
        HttpResponse publishAPIResponse =
                apiPublisherClientUser1.changeAPILifeCycleStatusToPublish(apiIdentifierAPI1Version2, false);
        assertEquals(publishAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API publish Response code is invalid " + getAPIIdentifierString(apiIdentifierAPI1Version2));
        assertTrue(verifyAPIStatusChange(publishAPIResponse, APILifeCycleState.CREATED, APILifeCycleState.PUBLISHED),
                "API status Change is invalid in" + getAPIIdentifierString(apiIdentifierAPI1Version2) +
                        "Response Data:" + publishAPIResponse.getData());


    }


    @Test(groups = {"wso2.am"}, description = " Test availability of old and new API versions in the store.",
            dependsOnMethods = "testPublishCopiedAPI")
    public void testAvailabilityOfOldAndNewAPIVersionsInStore() throws Exception {

        // Check availability of old API version in API Store
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(apiStoreClientUser1.getAPI(API1_NAME));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierAPI1Version1, apiStoreAPIIdentifierList), true,
                "Old version Api is not visible in API Store after publish new version." + getAPIIdentifierString(
                        apiIdentifierAPI1Version1));

        // Check availability of new API version in API Store
        apiStoreAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(apiStoreClientUser1.getAPI(API1_NAME));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierAPI1Version2, apiStoreAPIIdentifierList), true,
                "New version Api is not visible in API Store after publish new version." + getAPIIdentifierString(
                        apiIdentifierAPI1Version2));
    }


    @Test(groups = {"wso2.am"}, description = "Test subscribe of old API version.",
            dependsOnMethods = "testAvailabilityOfOldAndNewAPIVersionsInStore")
    public void testSubscribeOldVersion() throws Exception {
        HttpResponse oldVersionSubscribeResponse = subscribeAPI(apiIdentifierAPI1Version1, applicationName, apiStoreClientUser1);
        assertEquals(oldVersionSubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of old API version request not successful " + getAPIIdentifierString(apiIdentifierAPI1Version1));
        assertEquals(getValueFromJSON(oldVersionSubscribeResponse, "error"), "false",
                "Error in subscribe of old API version" + getAPIIdentifierString(apiIdentifierAPI1Version1) +
                        "Response Data:" + oldVersionSubscribeResponse.getData());
    }


    @Test(groups = {"wso2.am"}, description = " Test availability of old and new API versions i the store.", dependsOnMethods = "testSubscribeOldVersion")
    public void testSubscribeNewVersion() throws Exception {

        HttpResponse newVersionSubscribeResponse = subscribeAPI(apiIdentifierAPI1Version2, applicationName, apiStoreClientUser1);
        assertEquals(newVersionSubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of old API version request not successful " + getAPIIdentifierString(apiIdentifierAPI1Version2));
        assertEquals(getValueFromJSON(newVersionSubscribeResponse, "error"), "false",
                "Error in subscribe of old API version" + getAPIIdentifierString(apiIdentifierAPI1Version2) +
                        "Response Data:" + newVersionSubscribeResponse.getData());
    }


    @Test(groups = {"wso2.am"}, description = "Publish a API and check its visibility in the API Store. Copy and create a new version, " +
            "publish  the new version, test invocation of both old and new API versions.", dependsOnMethods = "testSubscribeNewVersion")
    public void testAccessibilityOfPublishedOldAPIAndPublishedCopyAPI() throws Exception {

        //get access token
        String accessToken = getAccessToken(apiStoreClientUser1, applicationName);

        // Create requestHeaders
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        //Invoke  old version
        HttpResponse oldVersionInvokeResponse = HttpRequestUtil.doGet(API_BASE_URL + API1_CONTEXT + "/" + API_VERSION1 +
                API1_END_POINT_METHOD, requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertTrue(oldVersionInvokeResponse.getData().contains(API1_RESPONSE_DATA), "Response data mismatched");

        //Invoke new version
        HttpResponse newVersionInvokeResponse = HttpRequestUtil.doGet(API_BASE_URL + API1_CONTEXT + "/" + API_VERSION2 +
                API1_END_POINT_METHOD, requestHeaders);

        assertEquals(newVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertTrue(newVersionInvokeResponse.getData().contains(API1_RESPONSE_DATA), "Response data mismatched");

    }


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStoreClientUser1.removeApplication(applicationName);
        deleteAPI(apiIdentifierAPI1Version1, apiPublisherClientUser1);
        deleteAPI(apiIdentifierAPI1Version2, apiPublisherClientUser1);
    }

}
