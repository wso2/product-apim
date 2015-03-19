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
 * "Retire an API and check its accessibility  and visibility in the API Store."
 */
public class AccessibilityOfRetireAPITestCase extends APIManagerLifecycleBaseTest {
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


    @Test(groups = {"wso2.am"}, description = "Test invocation of the APi before retire")
    public void testInvokeAPIBeforeChangeAPILifecycleToRetired() throws Exception {

        //Create and publish  and subscribe API version 1.0.0
        createPublishAndSubscribeAPI(apiIdentifierAPI1Version1, API1_CONTEXT,
                apiPublisherClientUser1, apiStoreClientUser1, applicationName);

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
                "Response code mismatched when invoke api before Retire");
        assertTrue(oldVersionInvokeResponse.getData().contains(API1_RESPONSE_DATA),
                "Response data mismatched when invoke  API  before Retire" +
                        " Response Data:" + oldVersionInvokeResponse.getData());

    }


    @Test(groups = {"wso2.am"}, description = "Change API lifecycle to Retired",
            dependsOnMethods = "testInvokeAPIBeforeChangeAPILifecycleToRetired")
    public void testChangeAPILifecycleToRetired() throws Exception {
        //Block the API version 1.0.0
        APILifeCycleStateRequest blockUpdateRequest =
                new APILifeCycleStateRequest(API1_NAME, API1_PROVIDER_NAME, APILifeCycleState.RETIRED);
        blockUpdateRequest.setVersion(API_VERSION1);
        //Change API lifecycle  to Block
        HttpResponse blockAPIActionResponse =
                apiPublisherClientUser1.changeAPILifeCycleStatus(blockUpdateRequest);
        assertEquals(blockAPIActionResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertTrue(verifyAPIStatusChange(blockAPIActionResponse, APILifeCycleState.PUBLISHED,
                APILifeCycleState.RETIRED), "API status Change is invalid when retire an API :" +
                getAPIIdentifierString(apiIdentifierAPI1Version1) +
                " Response Code:" + blockAPIActionResponse.getData());

    }


    @Test(groups = {"wso2.am"}, description = "Test the availability of retired API in the store",
            dependsOnMethods = "testChangeAPILifecycleToRetired")
    public void testAvailabilityOfRetiredAPIInStore() throws Exception {
        //  Verify the API in API Store : API should not be available in the store.
        List<APIIdentifier> apiStoreAPIIdentifierList = APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(
                apiStoreClientUser1.getAPI(API1_NAME));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierAPI1Version1, apiStoreAPIIdentifierList), false,
                "Api is  visible in API Store after retire." + getAPIIdentifierString(apiIdentifierAPI1Version1));

    }


    @Test(groups = {"wso2.am"}, description = "Test the invocation of the API after retire",
            dependsOnMethods = "testAvailabilityOfRetiredAPIInStore")
    public void testInvokeAPIAfterChangeAPILifecycleToRetired() throws Exception {

        //Invoke  old version
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(API_BASE_URL + API1_CONTEXT + "/" + API_VERSION1 + API1_END_POINT_METHOD,
                        requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Response code mismatched when invoke api after retire");
        assertTrue(oldVersionInvokeResponse.getData().contains(HTTP_RESPONSE_DATA_NOT_FOUND),
                "Response data mismatched when invoke  API  after retire" +
                        " Response Data:" + oldVersionInvokeResponse.getData());

    }

    @AfterClass(alwaysRun = true)
    public void cleanup() throws Exception {
        apiStoreClientUser1.removeApplication(applicationName);
        deleteAPI(apiIdentifierAPI1Version1, apiPublisherClientUser1);
    }


}
