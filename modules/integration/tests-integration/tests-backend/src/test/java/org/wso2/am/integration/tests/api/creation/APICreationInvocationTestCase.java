/*
*  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.wso2.am.integration.tests.api.creation;


import junit.framework.Assert;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


public class APICreationInvocationTestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(APICreationInvocationTestCase.class);
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String apiName = "TestSampleApi1";
    private String apiContext = "testSampleApi1";
    private String appName = "sample-application1";
    private Map<String, String> requestHeaders = new HashMap<String, String>();

    @Factory(dataProvider = "userModeDataProvider")
    public APICreationInvocationTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();

        apiStore = new APIStoreRestClient(storeURLHttp);
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);

        apiPublisher.login(user.getUserName(), user.getPassword());
        apiStore.login(user.getUserName(), user.getPassword());

    }

    @Test(groups = {"wso2.am"}, description = "Sample API creation")
    public void testAPICreation() throws Exception {
        String backendEndPoint = getBackendEndServiceEndPointHttp("jaxrs_basic/services/customers/customerservice");
        APIRequest apiRequest = new APIRequest(apiName, apiContext,
                                               new URL(backendEndPoint));
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(serviceResponse);
    }

    @Test(groups = {"wso2.am"}, description = "Sample API Publishing", dependsOnMethods = "testAPICreation")
    public void testAPIPublishing() throws Exception {

        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiName, user.getUserName(),
                                             APILifeCycleState.PUBLISHED);
        HttpResponse serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);

    }

    @Test(groups = {"wso2.am"}, description = "Sample Application Creation", dependsOnMethods = "testAPIPublishing")
    public void testApplicationCreation() throws Exception {
        HttpResponse serviceResponse = apiStore.addApplication(appName, "Gold", "", "this-is-test");
        verifyResponse(serviceResponse);

    }

    @Test(groups = {"wso2.am"}, description = "API Subscription", dependsOnMethods = "testApplicationCreation")
    public void testAPISubscription() throws Exception {

        String provider = user.getUserName();

        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, provider);
        subscriptionRequest.setApplicationName(appName);
        subscriptionRequest.setTier("Gold");
        HttpResponse serviceResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(serviceResponse);

    }

    @Test(groups = {"wso2.am"}, description = "Application Key Generation", dependsOnMethods = "testAPISubscription")
    public void testApplicationKeyGeneration() throws Exception {

        APPKeyRequestGenerator generateAppKeyRequest =
                new APPKeyRequestGenerator(appName);
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        log.info(responseString);
        JSONObject response = new JSONObject(responseString);
        String accessToken =
                response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Assert.assertNotNull("Access Token not found " + responseString, accessToken);

        requestHeaders.put("Authorization", "Bearer " + accessToken);

    }

    @Test(groups = {"wso2.am"}, description = "Sample API creation", dependsOnMethods = "testApplicationKeyGeneration")
    public void testAPIInvocation() throws Exception {
        HttpResponse serviceResponse;
        requestHeaders.put("Accept", "application/xml");

        HttpResponse sampleResponse = HttpRequestUtil.doGet(
                getAPIInvocationURLHttp(apiContext + "/1.0.0/customers/123"), requestHeaders);
        log.info(sampleResponse.getData());
        assertEquals(sampleResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                     "Response code mismatched when api invocation");
        assertTrue(sampleResponse.getData().contains("<Customer>"),
                   "Response data mismatched when api invocation");

    }


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeApplication(appName);
        super.cleanUp();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                //new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }
}

