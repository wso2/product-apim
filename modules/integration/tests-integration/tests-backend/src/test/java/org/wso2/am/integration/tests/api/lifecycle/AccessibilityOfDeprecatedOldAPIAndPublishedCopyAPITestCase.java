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
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Publish a API. Copy and create a new version, publish  the new version and deprecate the old version,
 * test invocGation of both old and new API versions."
 */
public class AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase
        extends APIManagerLifecycleBaseTest {
    private final String API_NAME = "DeprecatedAPITest";
    private final String API_CONTEXT = "DeprecatedAPI";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_END_POINT_METHOD = "/customers/123";
    private final String API_RESPONSE_DATA = "<id>123</id><name>John</name></Customer>";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String API_VERSION_2_0_0 = "2.0.0";
    private final String APPLICATION_NAME = "AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase";
    private String apiEndPointUrl;
    private APIIdentifier apiIdentifierAPI1Version1;
    private APIIdentifier apiIdentifierAPI1Version2;
    private String providerName;
    private APICreationRequestBean apiCreationRequestBean;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private APIStoreRestClient apiStoreClientUser2;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException, XPathExpressionException,
                                    MalformedURLException {
        super.init();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0,
                                           providerName, new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiIdentifierAPI1Version1 = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiIdentifierAPI1Version2 = new APIIdentifier(providerName, API_NAME, API_VERSION_2_0_0);
        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);
        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(user.getUserName(), user.getPassword());
        //Login to API Store with  admin
        apiStoreClientUser1.login(user.getUserName(), user.getPassword());

        apiStoreClientUser2 = new APIStoreRestClient(storeURLHttp);
        //Login to API Store with  User2
        apiStoreClientUser2.login(
                storeContext.getContextTenant().getTenantUserList().get(0).getUserName(),
                storeContext.getContextTenant().getTenantUserList().get(0).getPassword());
        apiStoreClientUser1
                .addApplication(APPLICATION_NAME, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");
    }


    @Test(groups = {"wso2.am"}, description = "Test subscribe of old API version before deprecate the old version")
    public void testSubscribeOldVersionBeforeDeprecate() throws APIManagerIntegrationTestException {
        //Create and publish API version 1.0.0
        createAndPublishAPI(apiIdentifierAPI1Version1, apiCreationRequestBean, apiPublisherClientUser1, false);
        // Copy to version 2.0.0 and Publish Copied API
        copyAndPublishCopiedAPI(apiIdentifierAPI1Version1, API_VERSION_2_0_0, apiPublisherClientUser1, false);
        HttpResponse oldVersionSubscribeResponse =
                subscribeToAPI(apiIdentifierAPI1Version1, APPLICATION_NAME, apiStoreClientUser1);
        assertEquals(oldVersionSubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                     "Subscribe of old API version request not successful " +
                     getAPIIdentifierString(apiIdentifierAPI1Version1));
        assertEquals(getValueFromJSON(oldVersionSubscribeResponse, "error"), "false",
                     "Error in subscribe of old API version" + getAPIIdentifierString(apiIdentifierAPI1Version1) +
                     "Response Data:" + oldVersionSubscribeResponse.getData());
    }


    @Test(groups = {"wso2.am"}, description = "Test subscribe of new API version before deprecate the old version",
          dependsOnMethods = "testSubscribeOldVersionBeforeDeprecate")
    public void testSubscribeNewVersion() throws APIManagerIntegrationTestException {
        HttpResponse newVersionSubscribeResponse =
                subscribeToAPI(apiIdentifierAPI1Version2, APPLICATION_NAME, apiStoreClientUser1);
        assertEquals(newVersionSubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                     "Subscribe of old API version request not successful " +
                     getAPIIdentifierString(apiIdentifierAPI1Version2));
        assertEquals(getValueFromJSON(newVersionSubscribeResponse, "error"), "false",
                     "Error in subscribe of old API version" + getAPIIdentifierString(apiIdentifierAPI1Version2) +
                     "Response Data:" + newVersionSubscribeResponse.getData());
    }


    @Test(groups = {"wso2.am"}, description = "Test deprecate old api version",
          dependsOnMethods = "testSubscribeNewVersion")
    public void testDeprecateOldVersion() throws APIManagerIntegrationTestException {

        APILifeCycleStateRequest deprecatedUpdateRequest =
                new APILifeCycleStateRequest(API_NAME, providerName, APILifeCycleState.DEPRECATED);
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
    public void testVisibilityOfOldAPIInStoreAfterDeprecate()
            throws APIManagerIntegrationTestException, IOException, XPathExpressionException {
        //Verify the API in API Store

        waitForAPIDeploymentSync(user.getUserName(), apiIdentifierAPI1Version1.getApiName(),
                                 apiIdentifierAPI1Version1.getVersion(),
                                 APIMIntegrationConstants.IS_API_EXISTS);

        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_2_0_0,
                                 APIMIntegrationConstants.IS_API_EXISTS);


        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientUser1.getAPI());
//        DisplayMultipleVersions property in api_manager.xml set to false in order to run the test on cluster
//        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierAPI1Version1, apiStoreAPIIdentifierList),
//                "Old API version is not visible in API Store after deprecate." +
//                        getAPIIdentifierString(apiIdentifierAPI1Version1));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in the store after API deprecate.",
          dependsOnMethods = "testVisibilityOfOldAPIInStoreAfterDeprecate")
    public void testVisibilityOfNewAPIInStore() throws APIManagerIntegrationTestException {
        //Verify the API in API Store
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientUser1.getAPI());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierAPI1Version2, apiStoreAPIIdentifierList),
                   "New API version is not visible in API Store after deprecate the old version." +
                   getAPIIdentifierString(apiIdentifierAPI1Version2));

    }


    @Test(groups = {"wso2.am"}, description = "Test the subscription of deprecated API version.",
          dependsOnMethods = "testVisibilityOfNewAPIInStore")
    public void testSubscribeOldVersionAfterDeprecate() throws APIManagerIntegrationTestException {
        //subscribe deprecated old version
        HttpResponse oldVersionSubscribeResponse = subscribeToAPI
                (apiIdentifierAPI1Version1, APPLICATION_NAME, apiStoreClientUser2);
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
        String accessToken = generateApplicationKeys(apiStoreClientUser1, APPLICATION_NAME).getAccessToken();
        // Create requestHeaders
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        //Invoke  old version
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) +
                                      API_END_POINT_METHOD, requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(),
                     HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertTrue(oldVersionInvokeResponse.getData().contains(API_RESPONSE_DATA), "Response data mismatched");
        //Invoke new version
        HttpResponse newVersionInvokeResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT,
                                                                                              API_VERSION_2_0_0) + API_END_POINT_METHOD, requestHeaders);
        assertEquals(newVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                     "Response code mismatched");
        assertTrue(newVersionInvokeResponse.getData().contains(API_RESPONSE_DATA), "Response data mismatched");

    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException {
        apiStoreClientUser1.removeApplication(APPLICATION_NAME);
        deleteAPI(apiIdentifierAPI1Version1, apiPublisherClientUser1);
        deleteAPI(apiIdentifierAPI1Version2, apiPublisherClientUser1);
    }


}
