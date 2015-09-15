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
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.xpath.XPathExpressionException;
import java.net.MalformedURLException;
import java.net.URL;
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
    private final String API_NAME = "APIAccessibilityOfOldAndCopyAPITest";
    private final String API_CONTEXT = "APIAccessibilityOfOldAndCopyAPI";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_END_POINT_METHOD = "customers/123";
    private final String API_RESPONSE_DATA = "<id>123</id><name>John</name></Customer>";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String API_VERSION_2_0_0 = "2.0.0";
    private final String APPLICATION_NAME = "APIAccessibilityOfPublishedOldAPIAndPublishedCopyAPITestCase";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private APIIdentifier apiIdentifierAPI1Version1;
    private APIIdentifier apiIdentifierAPI1Version2;
    private String providerName;
    private APICreationRequestBean apiCreationRequestBean;
    private APIPublisherRestClient apiPublisherRestClient;
    private APIStoreRestClient apiStoreRestClient;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException, XPathExpressionException, MalformedURLException {
        super.init();
        apiEndPointUrl = getGatewayURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                        new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiIdentifierAPI1Version1 = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiIdentifierAPI1Version2 = new APIIdentifier(providerName, API_NAME, API_VERSION_2_0_0);
        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();
        apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        apiStoreRestClient = new APIStoreRestClient(storeURLHttp);
        //Login to API Publisher with  admin
        apiPublisherRestClient.login(user.getUserName(), user.getPassword());
        //Login to API Store with  admin
        apiStoreRestClient.login(user.getUserName(), user.getPassword());
        apiStoreRestClient.addApplication(APPLICATION_NAME, "Unlimited", "", "");

    }


    @Test(groups = {"wso2.am"}, description = " Test Copy API.Copy API version 1.0.0  to 2.0.0 ")
    public void testCopyAPI() throws APIManagerIntegrationTestException {
        //Create and publish API version 1.0.0
        createAndPublishAPI(apiIdentifierAPI1Version1, apiCreationRequestBean, apiPublisherRestClient, false);
        //Copy API version 1.0.0  to 2.0.0
        HttpResponse httpResponseCopyAPI =
                apiPublisherRestClient.copyAPI(providerName, API_NAME, API_VERSION_1_0_0, API_VERSION_2_0_0, "");
        assertEquals(httpResponseCopyAPI.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Copy API request code is invalid." + getAPIIdentifierString(apiIdentifierAPI1Version1));
        assertEquals(getValueFromJSON(httpResponseCopyAPI, "error"), "false",
                "Copy  API response data is invalid" + getAPIIdentifierString(apiIdentifierAPI1Version1) +
                        "Response Data:" + httpResponseCopyAPI.getData());
    }


    @Test(groups = {"wso2.am"}, description = "Test publish activity of a copied API.", dependsOnMethods = "testCopyAPI")
    public void testPublishCopiedAPI() throws APIManagerIntegrationTestException {
        //Publish  version 2.0.0
        APILifeCycleStateRequest publishUpdateRequest =
                new APILifeCycleStateRequest(API_NAME, providerName, APILifeCycleState.PUBLISHED);
        publishUpdateRequest.setVersion(API_VERSION_2_0_0);
        HttpResponse publishAPIResponse =
                apiPublisherRestClient.changeAPILifeCycleStatusToPublish(apiIdentifierAPI1Version2, false);
        assertEquals(publishAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API publish Response code is invalid " + getAPIIdentifierString(apiIdentifierAPI1Version2));
        assertTrue(verifyAPIStatusChange(publishAPIResponse, APILifeCycleState.CREATED, APILifeCycleState.PUBLISHED),
                "API status Change is invalid in" + getAPIIdentifierString(apiIdentifierAPI1Version2) +
                        "Response Data:" + publishAPIResponse.getData());

    }


    @Test(groups = {"wso2.am"}, description = " Test availability of old and new API versions in the store.",
            dependsOnMethods = "testPublishCopiedAPI")
    public void testAvailabilityOfOldAndNewAPIVersionsInStore() throws APIManagerIntegrationTestException {
        // Check availability of old API version in API Store
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreRestClient.getAPI());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierAPI1Version1, apiStoreAPIIdentifierList),
                "Old version Api is not visible in API Store after publish new version." +
                        getAPIIdentifierString(apiIdentifierAPI1Version1));
        // Check availability of new API version in API Store
        apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreRestClient.getAPI());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierAPI1Version2, apiStoreAPIIdentifierList),
                "New version Api is not visible in API Store after publish new version." +
                        getAPIIdentifierString(apiIdentifierAPI1Version2));
    }


    @Test(groups = {"wso2.am"}, description = "Test subscribe of old API version.",
            dependsOnMethods = "testAvailabilityOfOldAndNewAPIVersionsInStore")
    public void testSubscribeOldVersion() throws APIManagerIntegrationTestException {
        HttpResponse oldVersionSubscribeResponse =
                subscribeToAPI(apiIdentifierAPI1Version1, APPLICATION_NAME, apiStoreRestClient);
        assertEquals(oldVersionSubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Subscribe of old API" +
                " version request not successful " + getAPIIdentifierString(apiIdentifierAPI1Version1));
        assertEquals(getValueFromJSON(oldVersionSubscribeResponse, "error"), "false",
                "Error in subscribe of old API version" + getAPIIdentifierString(apiIdentifierAPI1Version1) +
                        "Response Data:" + oldVersionSubscribeResponse.getData());
    }


    @Test(groups = {"wso2.am"}, description = " Test availability of old and new API versions i the store.",
            dependsOnMethods = "testSubscribeOldVersion")
    public void testSubscribeNewVersion() throws APIManagerIntegrationTestException {
        HttpResponse newVersionSubscribeResponse =
                subscribeToAPI(apiIdentifierAPI1Version2, APPLICATION_NAME, apiStoreRestClient);
        assertEquals(newVersionSubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Subscribe of old API" +
                " version request not successful " + getAPIIdentifierString(apiIdentifierAPI1Version2));
        assertEquals(getValueFromJSON(newVersionSubscribeResponse, "error"), "false",
                "Error in subscribe of old API version" + getAPIIdentifierString(apiIdentifierAPI1Version2) +
                        "Response Data:" + newVersionSubscribeResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Publish a API and check its visibility in the API Store. " +
            "Copy and create a new version, publish  the new version, test invocation of both old and" +
            " new API versions.", dependsOnMethods = "testSubscribeNewVersion")
    public void testAccessibilityOfPublishedOldAPIAndPublishedCopyAPI() throws Exception {
        //get access token
        String accessToken = generateApplicationKeys(apiStoreRestClient, APPLICATION_NAME).getAccessToken();
        // Create requestHeaders
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        //Invoke  old version
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0)  + "/" +
                        API_END_POINT_METHOD, requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertTrue(oldVersionInvokeResponse.getData().contains(API_RESPONSE_DATA), "Response data mismatched");
        //Invoke new version
        HttpResponse newVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_2_0_0)  + "/" +
                        API_END_POINT_METHOD, requestHeaders);
        assertEquals(newVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertTrue(newVersionInvokeResponse.getData().contains(API_RESPONSE_DATA), "Response data mismatched");

    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException {
        apiStoreRestClient.removeApplication(APPLICATION_NAME);
        deleteAPI(apiIdentifierAPI1Version1, apiPublisherRestClient);
        deleteAPI(apiIdentifierAPI1Version2, apiPublisherRestClient);
    }

}
