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
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Publish a API and check its visibility in the API Store.
 */
public class APIPublishingAndVisibilityInStoreTestCase extends APIManagerLifecycleBaseTest {


    private APIIdentifier apiIdentifierAPI1Version1;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        apiIdentifierAPI1Version1 = new APIIdentifier(USER_NAME1, API1_NAME, API_VERSION_1_0_0);
    }


    @Test(groups = {"wso2.am"}, description = "Create a API and  check its availability in Publisher.")
    public void testAPICreation() throws Exception {
        //Create APi
        HttpResponse createAPIResponse = createAPI(API1_NAME, API1_CONTEXT, API_VERSION_1_0_0, apiPublisherClientUser1);
        assertEquals(createAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Create API Response Code is invalid." + getAPIIdentifierString(apiIdentifierAPI1Version1));
        assertEquals(getValueFromJSON(createAPIResponse, "error"), "false",
                "Error in API Creation in " + getAPIIdentifierString(apiIdentifierAPI1Version1) +
                        "Response Data:" + createAPIResponse.getData());

        //Verify the API in API Publisher
        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(
                        apiPublisherClientUser1.getApi(API1_NAME, USER_NAME1, API_VERSION_1_0_0));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierAPI1Version1, apiPublisherAPIIdentifierList), true,
                "Added Api is not available in APi Publisher. " + getAPIIdentifierString(apiIdentifierAPI1Version1));
    }


    @Test(groups = {"wso2.am"}, description = "Check the visibility of API in Store before the API publish. " +
            "it should not be available in store.", dependsOnMethods = "testAPICreation")
    public void testVisibilityOfAPIInStoreBeforePublishing() throws Exception {
        //Verify the API in API Store : API should not be available in the store.
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(apiStoreClientUser1.getAPI(API1_NAME));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierAPI1Version1, apiStoreAPIIdentifierList), false,
                "Api is visible in API Store before publish." + getAPIIdentifierString(apiIdentifierAPI1Version1));
    }


    @Test(groups = {"wso2.am"}, description = "Test the API publishing action. " +
            "Response HTTP message should contains API status change from  CREATED to PUBLISHED",
            dependsOnMethods = "testVisibilityOfAPIInStoreBeforePublishing")
    public void testAPIPublishing() throws Exception {
        //Publish the API
        APILifeCycleStateRequest publishUpdateRequest =
                new APILifeCycleStateRequest(API1_NAME, USER_NAME1, APILifeCycleState.PUBLISHED);
        publishUpdateRequest.setVersion(API_VERSION_1_0_0);
        HttpResponse publishAPIResponse =
                apiPublisherClientUser1.changeAPILifeCycleStatusToPublish(apiIdentifierAPI1Version1, false);
        assertEquals(publishAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API publish Response code is invalid " + getAPIIdentifierString(apiIdentifierAPI1Version1));
        assertTrue(verifyAPIStatusChange(publishAPIResponse, APILifeCycleState.CREATED, APILifeCycleState.PUBLISHED),
                "API status Change is invalid in" + getAPIIdentifierString(apiIdentifierAPI1Version1) +
                        "Response Data:" + publishAPIResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in the store after API publish.",
            dependsOnMethods = "testAPIPublishing")
    public void testVisibilityOfAPIInStoreAfterPublishing() throws Exception {
        //Verify the API in API Store : API should not be available in the store.
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(apiStoreClientUser1.getAPI(API1_NAME));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierAPI1Version1, apiStoreAPIIdentifierList), true,
                "Api is not visible in API Store after publish. " + getAPIIdentifierString(apiIdentifierAPI1Version1));

    }


    @AfterClass(alwaysRun = true)
    public void cleanup() throws Exception {
        deleteAPI(apiIdentifierAPI1Version1, apiPublisherClientUser1);
    }


}
