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
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * "Publish a API. Copy and create a new version, publish  the new API version with re-subscription required and
 * test invocation of New API  before and after the re-subscription."
 */
public class AccessibilityOfOldAPIAndCopyAPIWithReSubscriptionTestCase extends APIManagerLifecycleBaseTest {
    private static final String API_NAME = "CopyAPIWithReSubscriptionTest";
    private static final String API_CONTEXT = "CopyAPIWithReSubscription";
    private static final String API_TAGS = "testTag1, testTag2, testTag3";
    private static final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private static final String API_END_POINT_METHOD = "/customers/123";
    private static final String API_RESPONSE_DATA = "<id>123</id><name>John</name></Customer>";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private static final String API_VERSION_2_0_0 = "2.0.0";
    private static final String APPLICATION_NAME = "AccessibilityOfOldAPIAndCopyAPIWithReSubscriptionTestCase";
    private static final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private APIIdentifier apiIdentifierAPI1Version1;
    private APIIdentifier apiIdentifierAPI1Version2;
    private String providerName;
    private APICreationRequestBean apiCreationRequestBean;
    private Map<String, String> requestHeaders;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;


    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException, XPathExpressionException, MalformedURLException {
        super.init();
        apiEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0,
                providerName, new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiIdentifierAPI1Version1 = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiIdentifierAPI1Version2 = new APIIdentifier(providerName, API_NAME, API_VERSION_2_0_0);
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);
        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(
                publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        //Login to API Store with  admin
        apiStoreClientUser1.login(
                storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());
        apiStoreClientUser1.addApplication(APPLICATION_NAME, "", "", "");
    }


    @Test(groups = {"wso2.am"}, description = "Test subscribe of old api version.")
    public void testSubscriptionOfOldAPI() throws APIManagerIntegrationTestException {
        //Create and publish API version 1.0.0
        createAndPublishAPI(apiIdentifierAPI1Version1, apiCreationRequestBean, apiPublisherClientUser1, false);
        // Subscribe old api version (1.0.0)
        HttpResponse oldVersionSubscribeResponse =
                subscribeToAPI(apiIdentifierAPI1Version1, APPLICATION_NAME, apiStoreClientUser1);
        assertEquals(oldVersionSubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of old API version request not successful " +
                        getAPIIdentifierString(apiIdentifierAPI1Version1));
        assertEquals(getValueFromJSON(oldVersionSubscribeResponse, "error"), "false",
                "Error in subscribe of old API version" + getAPIIdentifierString(apiIdentifierAPI1Version1) +
                        "Response Data:" + oldVersionSubscribeResponse.getData());
    }


    @Test(groups = {"wso2.am"}, description = "Test publishing of copied API with re-subscription required",
            dependsOnMethods = "testSubscriptionOfOldAPI")
    public void testPublishCopiedAPIWithReSubscriptionRequired() throws APIManagerIntegrationTestException {

        // Copy  API
        copyAPI(apiIdentifierAPI1Version1, API_VERSION_2_0_0, apiPublisherClientUser1);
        //Publish  version 2.0.0 with re-subscription required
        APILifeCycleStateRequest publishUpdateRequest =
                new APILifeCycleStateRequest(API_NAME, providerName, APILifeCycleState.PUBLISHED);
        publishUpdateRequest.setVersion(API_VERSION_2_0_0);
        HttpResponse publishAPIResponse =
                apiPublisherClientUser1.changeAPILifeCycleStatusToPublish(apiIdentifierAPI1Version2, true);
        assertEquals(publishAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API publish Response code is invalid " + getAPIIdentifierString(apiIdentifierAPI1Version2));
        assertTrue(verifyAPIStatusChange(publishAPIResponse, APILifeCycleState.CREATED, APILifeCycleState.PUBLISHED),
                "API status Change is invalid in" + getAPIIdentifierString(apiIdentifierAPI1Version2) +
                        "Response Data:" + publishAPIResponse.getData());
    }


    @Test(groups = {"wso2.am"}, description = "Test invocation of old API version  before the new version is subscribed.",
            dependsOnMethods = "testPublishCopiedAPIWithReSubscriptionRequired")
    public void testInvokeOldAPIBeforeSubscribeTheNewVersion() throws Exception {
        //get access token
        String accessToken = generateApplicationKeys(apiStoreClientUser1, APPLICATION_NAME).getAccessToken();
        // Create requestHeaders
        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        //Invoke  old version
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0)  + "/" +
                                      API_END_POINT_METHOD, requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke old api before subscribe the new version");
        assertTrue(oldVersionInvokeResponse.getData().contains(API_RESPONSE_DATA),
                "Response data mismatched when invoke old API version before subscribe the new version." +
                        " Response Data:" + oldVersionInvokeResponse.getData());
    }


    @Test(groups = {"wso2.am"}, description = "Test invocation of new API version  before the new version is subscribed." +
            "This invocation should be failed", dependsOnMethods = "testInvokeOldAPIBeforeSubscribeTheNewVersion")
    public void testInvokeNewAPIBeforeSubscribeTheNewVersion() throws Exception {
        //Invoke  old version
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_2_0_0) +  "/" +
                        API_END_POINT_METHOD, requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_UNAUTHORIZED,
                "Response code mismatched when invoke new api before subscribe the new version");
        assertTrue(oldVersionInvokeResponse.getData().contains(UNCLASSIFIED_AUTHENTICATION_FAILURE),
                "Response data mismatched when invoke new API version before subscribe the new version." +
                        " Response Data:" + oldVersionInvokeResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test subscribe the new API Version",
            dependsOnMethods = "testInvokeNewAPIBeforeSubscribeTheNewVersion")
    public void testSubscribeTheNewVersion() throws Exception {
        //subscribe new version
        HttpResponse httpResponseSubscribeNewVersion =
                subscribeToAPI(apiIdentifierAPI1Version2, APPLICATION_NAME, apiStoreClientUser1);
        assertEquals(httpResponseSubscribeNewVersion.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of New API version  when re-subscription required not successful. Invalid Response Code " +
                        getAPIIdentifierString(apiIdentifierAPI1Version2));
        assertEquals(getValueFromJSON(httpResponseSubscribeNewVersion, "error"), "false",
                "Error in subscribe of New API version when re-subscription required not successful" +
                        getAPIIdentifierString(apiIdentifierAPI1Version2) + "Response Data:" +
                        httpResponseSubscribeNewVersion.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test invocation of new API version  after the new version is subscribed.",
            dependsOnMethods = "testSubscribeTheNewVersion")
    public void testInvokeNewAPIAfterSubscribeTheNewVersion() throws Exception {
        //Invoke  new version after subscription
        HttpResponse oldVersionInvokeResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT,API_VERSION_2_0_0)
                                                                      + API_END_POINT_METHOD, requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched when" +
                " invoke new api after subscribe the new version");
        assertTrue(oldVersionInvokeResponse.getData().contains(API_RESPONSE_DATA), "Response data mismatched when invoke" +
                " new API version after subscribe the new version. Response Data:" + oldVersionInvokeResponse.getData());
    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException {
        apiStoreClientUser1.removeApplication(APPLICATION_NAME);
        deleteAPI(apiIdentifierAPI1Version1, apiPublisherClientUser1);
        deleteAPI(apiIdentifierAPI1Version2, apiPublisherClientUser1);
    }


}


