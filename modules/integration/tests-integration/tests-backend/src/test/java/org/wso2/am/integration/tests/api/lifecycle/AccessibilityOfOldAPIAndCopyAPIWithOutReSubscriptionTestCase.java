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
 * Publish a API. Copy and create a new version, publish the new API version with out re-subscription required and
 * test invocation of New API without re-subscription."
 */
public class AccessibilityOfOldAPIAndCopyAPIWithOutReSubscriptionTestCase extends APIManagerLifecycleBaseTest {

    private final String API_NAME = "CopyAPIWithOutReSubscriptionTest";
    private final String API_CONTEXT = "CopyAPIWithOutReSubscription";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_END_POINT_METHOD = "/customers/123";
    private final String API_RESPONSE_DATA = "<id>123</id><name>John</name></Customer>";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String API_VERSION_2_0_0 = "2.0.0";
    private final String APPLICATION_NAME = "AccessibilityOfOldAPIAndCopyAPIWithOutReSubscriptionTestCase";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
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
        apiEndPointUrl = getGatewayURLHttp() +  API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName
                        , new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiIdentifierAPI1Version1 =
                new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiIdentifierAPI1Version2 =
                new APIIdentifier(providerName, API_NAME, API_VERSION_2_0_0);
        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);
        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(user.getUserName(), user.getPassword());
        //Login to API Store with  admin

        apiStoreClientUser1.login(user.getUserName(), user.getPassword());
        apiStoreClientUser1.addApplication(APPLICATION_NAME, TIER_UNLIMITED, "", "");
    }


    @Test(groups = {"wso2.am"}, description = "Test subscribe of old api version.")
    public void testSubscriptionOfOldAPI()
            throws APIManagerIntegrationTestException, IOException, XPathExpressionException {
        //Create and publish API version 1.0.0
        createAndPublishAPI(apiIdentifierAPI1Version1, apiCreationRequestBean, apiPublisherClientUser1, false);
        // Subscribe old api version (1.0.0)

        waitForAPIDeploymentSync(apiCreationRequestBean.getProvider(),
                                 apiCreationRequestBean.getName(),
                                 apiCreationRequestBean.getVersion(),
                                 APIMIntegrationConstants.IS_API_EXISTS);

        HttpResponse oldVersionSubscribeResponse =
                subscribeToAPI(apiIdentifierAPI1Version1, APPLICATION_NAME, apiStoreClientUser1);
        assertEquals(oldVersionSubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of old API version request not successful " +
                        getAPIIdentifierString(apiIdentifierAPI1Version1));
        assertEquals(getValueFromJSON(oldVersionSubscribeResponse, "error"), "false",
                "Error in subscribe of old API version" + getAPIIdentifierString(apiIdentifierAPI1Version1) +
                        "Response Data:" + oldVersionSubscribeResponse.getData());
    }


    @Test(groups = {"wso2.am"}, description = "Test publishing of copied API with out re-subscription required",
            dependsOnMethods = "testSubscriptionOfOldAPI")
    public void testPublishCopiedAPIWithOutReSubscriptionRequired() throws APIManagerIntegrationTestException, XPathExpressionException {
        // Copy  API
        copyAPI(apiIdentifierAPI1Version1, API_VERSION_2_0_0, apiPublisherClientUser1);
        //Publish  version 2.0.0 with re-subscription required
        APILifeCycleStateRequest publishUpdateRequest =
                new APILifeCycleStateRequest(API_NAME, providerName, APILifeCycleState.PUBLISHED);
        publishUpdateRequest.setVersion(API_VERSION_2_0_0);
        waitForAPIDeploymentSync(apiCreationRequestBean.getProvider(),
                apiCreationRequestBean.getName(),
                API_VERSION_2_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
        HttpResponse publishAPIResponse =
                apiPublisherClientUser1.changeAPILifeCycleStatusToPublish(apiIdentifierAPI1Version2, false);
        assertEquals(publishAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API publish Response code is invalid " + getAPIIdentifierString(apiIdentifierAPI1Version2));
        assertTrue(verifyAPIStatusChange(publishAPIResponse, APILifeCycleState.CREATED, APILifeCycleState.PUBLISHED),
                "API status Change is invalid in" + getAPIIdentifierString(apiIdentifierAPI1Version2) +
                        "Response Data:" + publishAPIResponse.getData());
    }


    @Test(groups = {"wso2.am"}, description = "Test invocation of new API version before the new version" +
            " is subscribed." ,dependsOnMethods = "testPublishCopiedAPIWithOutReSubscriptionRequired")
    public void testInvokeNewAPIWithoutSubscribeTheNewVersion() throws Exception {
        //Invoke  old version
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_2_0_0, APIMIntegrationConstants.IS_API_EXISTS);

        //get access token
        String accessToken = generateApplicationKeys(apiStoreClientUser1, APPLICATION_NAME).getAccessToken();
        // Create requestHeaders
        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "text/xml");
         requestHeaders.put("Authorization", "Bearer " + accessToken);
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp( API_CONTEXT ,API_VERSION_2_0_0) +
                                      API_END_POINT_METHOD, requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke new api before subscribe the new version when re-subscription" +
                        " is not required.");
        assertTrue(oldVersionInvokeResponse.getData().contains(API_RESPONSE_DATA),
                "Response data mismatched when invoke new API version before subscribe the new version when" +
                        "re-subscription is not required." + " Response Data:" + oldVersionInvokeResponse.getData());
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException {
        apiStoreClientUser1.removeApplication(APPLICATION_NAME);
        deleteAPI(apiIdentifierAPI1Version1, apiPublisherClientUser1);
        deleteAPI(apiIdentifierAPI1Version2, apiPublisherClientUser1);
    }
}
