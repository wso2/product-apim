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


import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.*;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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
    private String providerName;
    private String applicationId;
    private String apiId;
    private String newApiId;
    private String consumerKey;
    private String consumerSecret;
    private APIRequest apiRequest;
    private Map<String, String> requestHeaders;
    private ArrayList<String> grantTypes;


    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        grantTypes = new ArrayList<>();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() +  API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();

        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(providerName);
        apiRequest.setTags(API_TAGS);
        apiRequest.setDescription(API_DESCRIPTION);

        //Create and publish API version 1.0.0
        apiId = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);

        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();
    }


    @Test(groups = {"wso2.am"}, description = "Test subscribe of old api version.")
    public void testSubscriptionOfOldAPI() throws APIManagerIntegrationTestException, XPathExpressionException {
        //Create and publish API version 1.0.0
        // Subscribe old api version (1.0.0)
        waitForAPIDeploymentSync(apiRequest.getProvider(),
                apiRequest.getName(),
                apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);

        HttpResponse oldVersionSubscribeResponse = subscribeToAPIUsingRest(apiId, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED, restAPIStore);
        assertEquals(oldVersionSubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of old API version request not successful " +
                        getAPIIdentifierStringFromAPIRequest(apiRequest));
        assertTrue(StringUtils.isNotEmpty(oldVersionSubscribeResponse.getData()),
                "Error in subscribe of old API version" + getAPIIdentifierStringFromAPIRequest(apiRequest));
    }


    @Test(groups = {"wso2.am"}, description = "Test publishing of copied API with re-subscription required",
            dependsOnMethods = "testSubscriptionOfOldAPI")
    public void testPublishCopiedAPIWithReSubscriptionRequired() throws Exception {

        // Copy  API
        //Create a new Version
        apiRequest.setVersion(API_VERSION_2_0_0);
        HttpResponse newVersionResponse = restAPIPublisher.copyAPI(API_VERSION_2_0_0, apiId, null);
        assertEquals(newVersionResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code Mismatch");
        newApiId = newVersionResponse.getData();
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(newApiId, restAPIPublisher);
        //Publish  version 2.0.0 without re-subscription required
        String lifecycleChecklist = "Requires re-subscription when publishing the API:true";
        HttpResponse publishAPIResponse = restAPIPublisher
                .changeAPILifeCycleStatus(newApiId, APILifeCycleAction.PUBLISH.getAction(), lifecycleChecklist);
        assertEquals(publishAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API publish Response code is invalid " + getAPIIdentifierStringFromAPIRequest(apiRequest));
        assertTrue(APILifeCycleState.PUBLISHED.getState().equals(publishAPIResponse.getData()),
                "API status Change is invalid when retire an API :" + API_NAME + " with API ID ("
                        + apiId + ")" + " Response Code:" + publishAPIResponse.getResponseCode());

        waitForAPIDeploymentSync(providerName, API_NAME, API_VERSION_2_0_0, APIMIntegrationConstants.IS_API_EXISTS);
    }


    @Test(groups = {"wso2.am"}, description = "Test invocation of old API version  before the new version is subscribed.",
            dependsOnMethods = "testPublishCopiedAPIWithReSubscriptionRequired")
    public void testInvokeOldAPIBeforeSubscribeTheNewVersion() throws Exception {
        //get access token
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore
                .generateKeys(applicationId, "36000", "", ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null,
                        grantTypes);
        consumerKey = applicationKeyDTO.getConsumerKey();
        consumerSecret = applicationKeyDTO.getConsumerSecret();
        // Create requestHeaders
        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + applicationKeyDTO.getToken().getAccessToken());
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttps(API_CONTEXT, API_VERSION_1_0_0) +
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
                HttpRequestUtil.doGet(getAPIInvocationURLHttps(API_CONTEXT, API_VERSION_2_0_0) +
                        API_END_POINT_METHOD, requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_FORBIDDEN,
                "Response code mismatched when invoke new api before subscribe the new version");
        assertTrue(oldVersionInvokeResponse.getData().contains(HTTP_RESPONSE_DATA_API_FORBIDDEN),
                "Response data mismatched when invoke new API version before subscribe the new version." +
                        " Response Data:" + oldVersionInvokeResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test subscribe the new API Version",
            dependsOnMethods = "testInvokeNewAPIBeforeSubscribeTheNewVersion")
    public void testSubscribeTheNewVersion() throws Exception {
        //subscribe new version
        HttpResponse newVersionSubscribeResponse = subscribeToAPIUsingRest(newApiId, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED, restAPIStore);
        assertEquals(newVersionSubscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of New API version  when re-subscription required not successful. Invalid Response Code " +
                        getAPIIdentifierStringFromAPIRequest(apiRequest));
        assertTrue(StringUtils.isNotEmpty(newVersionSubscribeResponse.getData()),
                "Error in subscribe of old API version" + getAPIIdentifierStringFromAPIRequest(apiRequest));

        URL tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
        String subsAccessTokenPayload = APIMTestCaseUtils.getPayloadForPasswordGrant(user.getUserName(),
                user.getPassword());
        JSONObject subsAccessTokenGenerationResponse = new JSONObject(
                restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, subsAccessTokenPayload,
                        tokenEndpointURL).getData());
        requestHeaders.put("Authorization", "Bearer " + subsAccessTokenGenerationResponse.getString("access_token"));
    }

    @Test(groups = {"wso2.am"}, description = "Test invocation of new API version  after the new version is subscribed.",
            dependsOnMethods = "testSubscribeTheNewVersion")
    public void testInvokeNewAPIAfterSubscribeTheNewVersion() throws Exception {
        //Invoke  new version after subscription
        HttpResponse newVersionInvokeResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttps(API_CONTEXT, API_VERSION_2_0_0) + API_END_POINT_METHOD, requestHeaders);
        assertEquals(newVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched when" +
                " invoke new api after subscribe the new version");
        assertTrue(newVersionInvokeResponse.getData().contains(API_RESPONSE_DATA), "Response data mismatched when invoke" +
                " new API version after subscribe the new version. Response Data:" + newVersionInvokeResponse.getData());
    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        SubscriptionListDTO subsDTO = restAPIStore.getAllSubscriptionsOfApplication(applicationId);
        for (SubscriptionDTO subscriptionDTO: subsDTO.getList()){
            restAPIStore.removeSubscription(subscriptionDTO);
        }
        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(newApiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        restAPIPublisher.deleteAPI(newApiId);
    }


}


