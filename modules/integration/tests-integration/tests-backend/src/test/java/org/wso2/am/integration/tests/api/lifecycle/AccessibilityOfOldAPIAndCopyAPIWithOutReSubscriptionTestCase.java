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
 * Publish a API. Copy and create a new version, publish the new API version with out re-subscription required and
 * test invocation of New API without re-subscription."
 */
public class AccessibilityOfOldAPIAndCopyAPIWithOutReSubscriptionTestCase extends APIManagerLifecycleBaseTest {
    private APIIdentifier apiIdentifierAPI1Version1;
    private APIIdentifier apiIdentifierAPI1Version2;
    private String applicationName;

    private Map<String, String> requestHeaders;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        apiIdentifierAPI1Version1 = new APIIdentifier(USER_NAME1, API1_NAME, API_VERSION_1_0_0);
        apiIdentifierAPI1Version2 = new APIIdentifier(USER_NAME1, API1_NAME, API_VERSION_2_0_0);
        applicationName =
                (this.getClass().getName().replace(this.getClass().getPackage().getName(), "")).replace(".", "");
        apiStoreClientUser1.addApplication(applicationName, "", "", "");
    }


    @Test(groups = {"wso2.am"}, description = "Test subscribe of old api version.")
    public void testSubscriptionOfOldAPI() throws Exception {
        //Create and publish API version 1.0.0
        createAndPublishAPI(apiIdentifierAPI1Version1, API1_CONTEXT, apiPublisherClientUser1, false);

        // Subscribe old api version (1.0.0)
        HttpResponse oldVersionSubscribeResponse =
                subscribeToAPI(apiIdentifierAPI1Version1, applicationName, apiStoreClientUser1);
        assertEquals(oldVersionSubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of old API version request not successful " +
                        getAPIIdentifierString(apiIdentifierAPI1Version1));
        assertEquals(getValueFromJSON(oldVersionSubscribeResponse, "error"), "false",
                "Error in subscribe of old API version" + getAPIIdentifierString(apiIdentifierAPI1Version1) +
                        "Response Data:" + oldVersionSubscribeResponse.getData());


    }


    @Test(groups = {"wso2.am"}, description = "Test publishing of copied API with out re-subscription required",
            dependsOnMethods = "testSubscriptionOfOldAPI")
    public void testPublishCopiedAPIWithOutReSubscriptionRequired() throws Exception {
        // Copy  API
        copyAPI(apiIdentifierAPI1Version1, API_VERSION_2_0_0, apiPublisherClientUser1);


        //Publish  version 2.0.0 with re-subscription required
        APILifeCycleStateRequest publishUpdateRequest =
                new APILifeCycleStateRequest(API1_NAME, USER_NAME1, APILifeCycleState.PUBLISHED);
        publishUpdateRequest.setVersion(API_VERSION_2_0_0);
        HttpResponse publishAPIResponse =
                apiPublisherClientUser1.changeAPILifeCycleStatusToPublish(apiIdentifierAPI1Version2, false);
        assertEquals(publishAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API publish Response code is invalid " + getAPIIdentifierString(apiIdentifierAPI1Version2));
        assertTrue(verifyAPIStatusChange(publishAPIResponse, APILifeCycleState.CREATED, APILifeCycleState.PUBLISHED),
                "API status Change is invalid in" + getAPIIdentifierString(apiIdentifierAPI1Version2) +
                        "Response Data:" + publishAPIResponse.getData());

    }


    @Test(groups = {"wso2.am"}, description = "Test invocation of old API version  before the" +
            " new version is subscribed.", dependsOnMethods = "testPublishCopiedAPIWithOutReSubscriptionRequired")
    public void testInvokeOldAPIBeforeSubscribeTheNewVersion() throws Exception {

        //get access token
        String accessToken = getAccessToken(apiStoreClientUser1, applicationName);

        // Create requestHeaders
        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        //Invoke  old version
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(API_BASE_URL + API1_CONTEXT + "/" + API_VERSION_1_0_0 + API1_END_POINT_METHOD,
                        requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke old api before subscribe the new version");
        assertTrue(oldVersionInvokeResponse.getData().contains(API1_RESPONSE_DATA),
                "Response data mismatched when invoke old API version before subscribe the new version." +
                        " Response Data:" + oldVersionInvokeResponse.getData());


    }


    @Test(groups = {"wso2.am"}, description = "Test invocation of new API version" +
            " before the new version is subscribed.", dependsOnMethods = "testInvokeOldAPIBeforeSubscribeTheNewVersion")
    public void testInvokeNewAPIWithoutSubscribeTheNewVersion() throws Exception {

        //Invoke  old version
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(API_BASE_URL + API1_CONTEXT + "/" + API_VERSION_2_0_0 + API1_END_POINT_METHOD,
                        requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke new api before subscribe the new version when re-subscription" +
                        " is not required.");
        assertTrue(oldVersionInvokeResponse.getData().contains(API1_RESPONSE_DATA),
                "Response data mismatched when invoke new API version before subscribe the new version when" +
                        "re-subscription is not required." + " Response Data:" + oldVersionInvokeResponse.getData());


    }


    @AfterClass(alwaysRun = true)
    public void cleanup() throws Exception {
        apiStoreClientUser1.removeApplication(applicationName);
        deleteAPI(apiIdentifierAPI1Version1, apiPublisherClientUser1);
        deleteAPI(apiIdentifierAPI1Version2, apiPublisherClientUser1);
    }


}
