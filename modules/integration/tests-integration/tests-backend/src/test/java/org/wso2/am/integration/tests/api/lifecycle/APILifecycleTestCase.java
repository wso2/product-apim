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

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIMgtTestUtil;
import org.wso2.am.integration.test.utils.base.AMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.ServerConstants;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Test cases for API Manager lifecycle
 */
public class APILifecycleTestCase extends AMIntegrationBaseTest {

    private static final String API1_TAGS = "youtube, video, media";
    private static final String API1_END_POINT_URL = "http://gdata.youtube.com/feeds/api/standardfeeds";
    private static final String API1_DESCRIPTION = "This is test API create by API manager integration test";
    private static final String END_POINT_METHOD = "/most_popular";
    private static final String API1_PROVIDER_NAME = "admin";

    private static final String API_RESPONSE_DATA = "<feed";
    private static final String USER_KEY_USER2 = "userKey1";
    private static final String APIM_CONFIG_XML = "api-manager.xml";
    private static final String API_VERSION1 = "1.0.0";
    private static final String API_VERSION2 = "2.0.0";
    private static final String CARBON_HOME = System.getProperty(ServerConstants.CARBON_HOME);
    private static final String ARTIFACTS_LOCATION = TestConfigurationProvider.getResourceLocation() +
            File.separator +
            "artifacts" + File.separator + "AM" + File.separator + "configFiles" + File.separator + "lifecycletest" +
            File.separator;
    private static final String APIM_CONFIG_ARTIFACT_LOCATION = ARTIFACTS_LOCATION + APIM_CONFIG_XML;
    private static final String APIM_REPOSITORY_CONFIG_LOCATION = CARBON_HOME + File.separator + "repository" +
            File.separator + "conf" + File.separator + APIM_CONFIG_XML;
    private static final int HTTP_RESPONSE_CODE_OK = 200;
    private static final int HTTP_RESPONSE_CODE_UNAUTHORIZED = 401;
    private static final int HTTP_RESPONSE_CODE_NOT_FOUND = 404;
    private static final int HTTP_RESPONSE_CODE_SERVICE_UNAVAILABLE = 503;

    private String accessToken;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private APIPublisherRestClient apiPublisherClientUser2;
    private APIStoreRestClient apiStoreClientUser2;
    private ServerConfigurationManager serverManager;
    private static String API_BASE_URL;
    private final String string = "Publish a API. Copy and create a new version, publish  the new version" +
            " and deprecate the old version, test invocation of both old and new API versions.";

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();

        String publisherURLHttp = getPublisherServerURLHttp();
        String storeURLHttp = getStoreServerURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);
        apiPublisherClientUser2 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser2 = new APIStoreRestClient(storeURLHttp);

        File sourceFile = new File(APIM_CONFIG_ARTIFACT_LOCATION);
        File targetFile = new File(APIM_REPOSITORY_CONFIG_LOCATION);

        serverManager = new ServerConfigurationManager(apimContext);

        // apply configuration to  api-manager.xml
        serverManager.applyConfigurationWithoutRestart(sourceFile, targetFile, true);
        serverManager.restartGracefully();

        //Login to API Publisher with  User1
        apiPublisherClientUser1.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
        //Login to API Store with  User1
        apiStoreClientUser1.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());

        API_BASE_URL = getGatewayServerURLHttp();
        //Login to API Publisher with  User1
        String userNameUser1 = apimContext.getContextTenant().getTenantUser(USER_KEY_USER2).getUserName();
        String passwordUser1 = apimContext.getContextTenant().getTenantUser(USER_KEY_USER2).getPassword();

        apiPublisherClientUser2.login(userNameUser1, passwordUser1);
        //Login to API Store with  User1
        apiStoreClientUser2.login(userNameUser1, passwordUser1);

    }


    @Test(groups = {"wso2.am"}, description = "Publish a API and check its visibility in the API Store.", enabled = true)
    public void testAPIPublishingAndVisibilityInStore() throws Exception {

        String apiName = "APILifeCycleTestAPI1";
        String apiContext = "testAPI1";

        APIIdentifier apiIdentifierAPI1Version1 = new APIIdentifier(API1_PROVIDER_NAME, apiName, API_VERSION1);

        //Create APi
        HttpResponse createAPIResponse = createAPI(apiName, apiContext, API_VERSION1, apiPublisherClientUser1);
        assertEquals(createAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Create API request not successful");
        assertTrue(createAPIResponse.getData().contains("{\"error\" : false}"), "Create API request not successful");


        //Verify the API in API Publisher
        List<APIIdentifier> apiPublisherAPIIdentifierList = APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(
                apiPublisherClientUser1.getAPI(apiName, API1_PROVIDER_NAME, API_VERSION1));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierAPI1Version1, apiPublisherAPIIdentifierList), true,
                "Added Api is not available in APi Publisher. API Name:" + apiName + " API Version :" + API_VERSION1 +
                        " API Provider Name:" + API1_PROVIDER_NAME);

        //Verify the API in API Store : API should not be available in the store.
        List<APIIdentifier> apiStoreAPIIdentifierList = APIMgtTestUtil.getAPIIdentifierListFromHttpResponse
                (apiStoreClientUser1.getAPI(apiName));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierAPI1Version1, apiStoreAPIIdentifierList), false,
                "Api is visible in API Store before publish." + getAPIIdentifierString(apiIdentifierAPI1Version1));

        //Publish the API
        APILifeCycleStateRequest publishUpdateRequest = new APILifeCycleStateRequest(apiName, API1_PROVIDER_NAME,
                APILifeCycleState.PUBLISHED);
        publishUpdateRequest.setVersion(API_VERSION1);
        HttpResponse httpResponsePublishAPI = apiPublisherClientUser1.changeAPILifeCycleStatusToPublish
                (apiIdentifierAPI1Version1, false);
        assertEquals(httpResponsePublishAPI.getResponseCode(), HTTP_RESPONSE_CODE_OK, "API publish request not success");
        assertTrue(httpResponsePublishAPI.getData().contains("\"newStatus\" : \"CREATED\", \"oldStatus\""));

        //Verify the API in API Store : API should not be available in the store.
        apiStoreAPIIdentifierList = APIMgtTestUtil.getAPIIdentifierListFromHttpResponse
                (apiStoreClientUser1.getAPI(apiName));

        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierAPI1Version1, apiStoreAPIIdentifierList), true,
                "Api is not visible in API Store after publish. " + getAPIIdentifierString(apiIdentifierAPI1Version1));

    }

    @Test(groups = {"wso2.am"}, description = "Publish a API and check its visibility in the API Store. Copy and create a new version, " +
            "publish  the new version, test invocation of both old and new API versions.", dependsOnMethods = "testAPIPublishingAndVisibilityInStore")
    public void testAccessibilityOfPublishedOldAPIAndPublishedCopyAPI() throws Exception {


        String apiName = "APILifeCycleTestAPI2";
        String apiContext = "testAPI2";

        APIIdentifier apiIdentifierAPI1Version1 = new APIIdentifier(API1_PROVIDER_NAME, apiName, API_VERSION1);
        APIIdentifier apiIdentifierAPI1Version2 = new APIIdentifier(API1_PROVIDER_NAME, apiName, API_VERSION2);

        createAPI(apiName, apiContext, API_VERSION1, apiPublisherClientUser1);

        //Publish the API version 1.0.0
        APILifeCycleStateRequest publishUpdateRequest = new APILifeCycleStateRequest(apiName, API1_PROVIDER_NAME,
                APILifeCycleState.PUBLISHED);
        publishUpdateRequest.setVersion(API_VERSION1);
        apiPublisherClientUser1.changeAPILifeCycleStatus(publishUpdateRequest);

        //Copy API version 1.0.0  to 2.0.0
        apiPublisherClientUser1.copyAPI(API1_PROVIDER_NAME, apiName, API_VERSION1, API_VERSION2, "");

        //Publish  version 2.0.0
        publishUpdateRequest = new APILifeCycleStateRequest(apiName, API1_PROVIDER_NAME,
                APILifeCycleState.PUBLISHED);
        publishUpdateRequest.setVersion(API_VERSION2);
        apiPublisherClientUser1.changeAPILifeCycleStatus(publishUpdateRequest);

        // Check availability of old API version in API Store
        List<APIIdentifier> apiStoreAPIIdentifierList = APIMgtTestUtil.getAPIIdentifierListFromHttpResponse
                (apiStoreClientUser1.getAPI(apiName));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierAPI1Version1, apiStoreAPIIdentifierList), true,
                "Old version Api is not visible in API Store after publish new version." + getAPIIdentifierString(
                        apiIdentifierAPI1Version1));

        // Check availability of new API version in API Store
        apiStoreAPIIdentifierList = APIMgtTestUtil.getAPIIdentifierListFromHttpResponse
                (apiStoreClientUser1.getAPI(apiName));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierAPI1Version2, apiStoreAPIIdentifierList), true,
                "New version Api is not visible in API Store after publish new version." + getAPIIdentifierString(
                        apiIdentifierAPI1Version2));

        //subscribe Old version
        assertEquals(SubscribeAPI(apiIdentifierAPI1Version1, apiStoreClientUser1).getData(),
                "{\"error\" : false}", "Cannot subscribe to the old API version, " + getAPIIdentifierString(
                        apiIdentifierAPI1Version1));


        //subscribe new version
        assertEquals(SubscribeAPI(apiIdentifierAPI1Version2, apiStoreClientUser1).getData(), "{\"error\" : false}",
                "Cannot subscribe to the new API version, " + getAPIIdentifierString(apiIdentifierAPI1Version2));

        //get access token
        accessToken = getAccessToken(apiStoreClientUser1);

        // Create requestHeaders
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        //Invoke  old version
        HttpResponse oldVersionInvokeResponse = HttpRequestUtil.doGet(API_BASE_URL + apiContext + "/" + API_VERSION1 +
                END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        Assert.assertTrue(oldVersionInvokeResponse.getData().contains(API_RESPONSE_DATA), "Response data mismatched");

        //Invoke new version
        HttpResponse newVersionInvokeResponse = HttpRequestUtil.doGet(API_BASE_URL + apiContext + "/" + API_VERSION2 +
                END_POINT_METHOD, requestHeaders);

        Assert.assertEquals(newVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        Assert.assertTrue(newVersionInvokeResponse.getData().contains(API_RESPONSE_DATA), "Response data mismatched");

    }


    @Test(groups = {"wso2.am"}, description = string,
            dependsOnMethods = "testAccessibilityOfPublishedOldAPIAndPublishedCopyAPI")
    public void testAccessibilityOfDeprecatedOldAPIAndPublishedCopyAPI() throws Exception {


        String apiName = "APILifeCycleTestAPI3";
        String apiContext = "testAPI3";

        APIIdentifier apiIdentifierAPI1Version1 = new APIIdentifier(API1_PROVIDER_NAME, apiName, API_VERSION1);
        APIIdentifier apiIdentifierAPI1Version2 = new APIIdentifier(API1_PROVIDER_NAME, apiName, API_VERSION2);

        createAPI(apiName, apiContext, API_VERSION1, apiPublisherClientUser1);

        //Publish the API version 1.0.0
        APILifeCycleStateRequest publishUpdateRequest = new APILifeCycleStateRequest(apiName, API1_PROVIDER_NAME,
                APILifeCycleState.PUBLISHED);
        publishUpdateRequest.setVersion(API_VERSION1);
        apiPublisherClientUser1.changeAPILifeCycleStatus(publishUpdateRequest);

        //Copy API version 1.0.0  to 2.0.0
        apiPublisherClientUser1.copyAPI(API1_PROVIDER_NAME, apiName, API_VERSION1, API_VERSION2, "");

        //Publish  version 2.0.0
        publishUpdateRequest = new APILifeCycleStateRequest(apiName, API1_PROVIDER_NAME,
                APILifeCycleState.PUBLISHED);
        publishUpdateRequest.setVersion(API_VERSION2);
        apiPublisherClientUser1.changeAPILifeCycleStatus(publishUpdateRequest);


        //subscribe Old version
        assertEquals(SubscribeAPI(apiIdentifierAPI1Version1, apiStoreClientUser1).getData(), "{\"error\" : false}",
                "Cannot subscribe to the old API version, " + getAPIIdentifierString(apiIdentifierAPI1Version1));


        //subscribe new version
        assertEquals(SubscribeAPI(apiIdentifierAPI1Version2, apiStoreClientUser1).getData(), "{\"error\" : false}",
                "Cannot subscribe to the new API version, " + getAPIIdentifierString(apiIdentifierAPI1Version2));


        APILifeCycleStateRequest deprecatedUpdateRequest = new APILifeCycleStateRequest(apiName, API1_PROVIDER_NAME,
                APILifeCycleState.DEPRECATED);
        deprecatedUpdateRequest.setVersion(API_VERSION1);
        apiPublisherClientUser1.changeAPILifeCycleStatus(deprecatedUpdateRequest);
        //subscribe Old version
        String subscribeErrorMessage = "";
        try {
            SubscribeAPI(apiIdentifierAPI1Version1, apiStoreClientUser2).getData();
        } catch (Exception ex) {
            subscribeErrorMessage = ex.getMessage();
        } finally {
            assertTrue(subscribeErrorMessage.contains("Error while adding the subscription"),
                    "Users can subscribe to Deprecated API" + getAPIIdentifierString(apiIdentifierAPI1Version1));
        }


        // Check availability of old API version in API Store
        List<APIIdentifier> apiStoreAPIIdentifierList = APIMgtTestUtil.getAPIIdentifierListFromHttpResponse
                (apiStoreClientUser1.getAPI(apiName));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierAPI1Version1, apiStoreAPIIdentifierList), true,
                "Old version Api is not visible in API Store after publish new version." + getAPIIdentifierString
                        (apiIdentifierAPI1Version1));


        // Check availability of new API version in API Store
        apiStoreAPIIdentifierList = APIMgtTestUtil.getAPIIdentifierListFromHttpResponse
                (apiStoreClientUser1.getAPI(apiName));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierAPI1Version2, apiStoreAPIIdentifierList), true,
                "New version Api is not visible in API Store after publish new version. " + getAPIIdentifierString
                        (apiIdentifierAPI1Version2));

        // Create requestHeaders
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        //Invoke  old version
        HttpResponse oldVersionInvokeResponse = HttpRequestUtil.doGet(API_BASE_URL + apiContext + "/" + API_VERSION1
                + END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched");
        Assert.assertTrue(oldVersionInvokeResponse.getData().contains(API_RESPONSE_DATA), "Response data mismatched");

        //Invoke new version
        HttpResponse newVersionInvokeResponse = HttpRequestUtil.doGet(API_BASE_URL + apiContext + "/" + API_VERSION2 +
                END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(newVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched");
        Assert.assertTrue(newVersionInvokeResponse.getData().contains(API_RESPONSE_DATA), "Response data mismatched");

    }


    @Test(groups = {"wso2.am"}, description = "Publish a API. Copy and create a new version, publish  the new " +
            " API version with re-subscription required and test invocation of New API  before and after the " +
            "re-subscription.",
            dependsOnMethods = "testAccessibilityOfDeprecatedOldAPIAndPublishedCopyAPI", enabled = false)
    public void testAccessibilityOfOldAPIAndCopyAPIWithReSubscription() throws Exception {


        String apiName = "APILifeCycleTestAPI4";
        String apiContext = "testAPI4";

        APIIdentifier apiIdentifierAPI1Version1 = new APIIdentifier(API1_PROVIDER_NAME, apiName, API_VERSION1);
        APIIdentifier apiIdentifierAPI1Version2 = new APIIdentifier(API1_PROVIDER_NAME, apiName, API_VERSION2);

        //create API
        createAPI(apiName, apiContext, API_VERSION1, apiPublisherClientUser1);

        //Publish the API version 1.0.0
        HttpResponse httpResponsePublishAPIOldVersion = apiPublisherClientUser1.
                changeAPILifeCycleStatusToPublish(apiIdentifierAPI1Version1, false);
        assertTrue(httpResponsePublishAPIOldVersion.getData().contains("\"newStatus\" : \"PUBLISHED\""),
                "API is not publish correctly." + getAPIIdentifierString(apiIdentifierAPI1Version1));

        //subscribe Old version
        HttpResponse httpResponseSubscribeOldVersion = SubscribeAPI(apiIdentifierAPI1Version1, apiStoreClientUser1);
        assertEquals(httpResponseSubscribeOldVersion.getData(), "{\"error\" : false}", "Cannot subscribe to the " +
                "old API version, " + getAPIIdentifierString(apiIdentifierAPI1Version1));

        // Create requestHeaders
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        //   Invoke  old version
        HttpResponse oldVersionInvokeResponse = HttpRequestUtil.doGet(API_BASE_URL + apiContext + "/" + API_VERSION1
                + END_POINT_METHOD, requestHeaders);

        Assert.assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched");
        Assert.assertTrue(oldVersionInvokeResponse.getData().contains(API_RESPONSE_DATA), "Response data mismatched");

        //Copy API version 1.0.0  to 2.0.0
        apiPublisherClientUser1.copyAPI(API1_PROVIDER_NAME, apiName, API_VERSION1, API_VERSION2, "");

        //Publish  version 2.0.0 with Re-Subscription required
        HttpResponse httpResponsePublishAPIVersion2 = apiPublisherClientUser1.changeAPILifeCycleStatusToPublish(
                apiIdentifierAPI1Version2,
                true);
        assertTrue(httpResponsePublishAPIVersion2.getData().contains("\"newStatus\" : \"PUBLISHED\""),
                "API is not publish correctly." + getAPIIdentifierString(apiIdentifierAPI1Version2));

        //Invoke new version before subscribe
        HttpResponse newVersionInvokeResponseBeforeSubscribe = HttpRequestUtil.doGet(API_BASE_URL + apiContext + "/" +
                API_VERSION2 + END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(newVersionInvokeResponseBeforeSubscribe.getResponseCode(), HTTP_RESPONSE_CODE_UNAUTHORIZED,
                "Invoke was success before subscribe the new version of api when Re-Subscription required. " +
                        "Incorrect Response code");
        Assert.assertTrue(newVersionInvokeResponseBeforeSubscribe.getData().contains(
                        "<ams:code>900901</ams:code><ams:message>Invalid Credentials</ams:message>"),
                "Invoke was success before subscribe the new version of api when Re-Subscription required. " +
                        "Incorrect data in the response.");


        //subscribe new version
        HttpResponse httpResponseSubscribeNewVersion = SubscribeAPI(apiIdentifierAPI1Version2, apiStoreClientUser1);
        assertEquals(httpResponseSubscribeNewVersion.getData(), "{\"error\" : false}",
                "Cannot subscribe to the new API version, API Name:" + apiName + " API Version:" + API_VERSION2);

        //Invoke new version after subscribe
        HttpResponse newVersionInvokeResponse1 = HttpRequestUtil.doGet(API_BASE_URL + apiContext + "/" + API_VERSION2
                + END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(newVersionInvokeResponse1.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Invoke was not successful after subscribe the API.  Incorrect Response code\"");
        Assert.assertTrue(newVersionInvokeResponse1.getData().contains(API_RESPONSE_DATA),
                "Invoke was not successful after subscribe the API.Incorrect data in the response.");


    }

    @Test(groups = {"wso2.am"}, description = "Publish a API. Copy and create a new version, publish  the new API " +
            "version with out re-subscription required and test invocation of New API without re-subscription."
            , dependsOnMethods = "testAccessibilityOfDeprecatedOldAPIAndPublishedCopyAPI")
    public void testAccessibilityOfOldAPIAndCopyAPIWithOutReSubscription() throws Exception {


        String apiName = "APILifeCycleTestAPI5";
        String apiContext = "testAPI5";

        APIIdentifier apiIdentifierAPI1Version1 = new APIIdentifier(API1_PROVIDER_NAME, apiName, API_VERSION1);
        APIIdentifier apiIdentifierAPI1Version2 = new APIIdentifier(API1_PROVIDER_NAME, apiName, API_VERSION2);

        //create API
        createAPI(apiName, apiContext, API_VERSION1, apiPublisherClientUser1);

        //Publish the API version 1.0.0
        HttpResponse httpResponsePublishAPIOldVersion = apiPublisherClientUser1.changeAPILifeCycleStatusToPublish(
                apiIdentifierAPI1Version1, false);
        assertTrue(httpResponsePublishAPIOldVersion.getData().contains("\"newStatus\" : \"PUBLISHED\""),
                "API is not publish correctly." + getAPIIdentifierString(apiIdentifierAPI1Version1));

        //subscribe Old version
        HttpResponse httpResponseSubscribeOldVersion = SubscribeAPI(apiIdentifierAPI1Version1, apiStoreClientUser1);
        assertEquals(httpResponseSubscribeOldVersion.getData(), "{\"error\" : false}",
                "Cannot subscribe to the old API version, " + getAPIIdentifierString(apiIdentifierAPI1Version1));

        // Create requestHeaders
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        //   Invoke  old version
        HttpResponse oldVersionInvokeResponse = HttpRequestUtil.doGet(API_BASE_URL + apiContext + "/" + API_VERSION1 +
                END_POINT_METHOD, requestHeaders);

        Assert.assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched");
        Assert.assertTrue(oldVersionInvokeResponse.getData().contains(API_RESPONSE_DATA),
                "Response data mismatched");

        //Copy API version 1.0.0  to 2.0.0
        apiPublisherClientUser1.copyAPI(API1_PROVIDER_NAME, apiName, API_VERSION1, API_VERSION2, "");

        //Publish  version 2.0.0 with Re-Subscription not required
        HttpResponse httpResponsePublishAPIVersion2 = apiPublisherClientUser1.changeAPILifeCycleStatusToPublish(
                apiIdentifierAPI1Version2, false);
        assertTrue(httpResponsePublishAPIVersion2.getData().contains("\"newStatus\" : \"PUBLISHED\""),
                "API is not publish correctly." + getAPIIdentifierString(apiIdentifierAPI1Version2));

        //Invoke new version before subscribe
        HttpResponse newVersionInvokeResponseBeforeSubscribe = HttpRequestUtil.doGet(API_BASE_URL + apiContext + "/" +
                API_VERSION2 + END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(newVersionInvokeResponseBeforeSubscribe.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Invoke was not success before subscribe the new version of api when Re-Subscription not required." +
                        " Incorrect Response code");
        Assert.assertTrue(newVersionInvokeResponseBeforeSubscribe.getData().contains(API_RESPONSE_DATA),
                "Invoke was  not success before subscribe the new version of api when Re-Subscription not required." +
                        " Incorrect data in the response.");
    }


    @Test(groups = {"wso2.am"}, description = "Block an API and check its accessibility in the API Store.",
            dependsOnMethods = "testAccessibilityOfOldAPIAndCopyAPIWithOutReSubscription")
    public void testAccessibilityOfBlockAPI() throws Exception {


        String apiName = "APILifeCycleTestAPI6";
        String apiContext = "testAPI6";

        APIIdentifier apiIdentifierAPI1Version1 = new APIIdentifier(API1_PROVIDER_NAME, apiName, API_VERSION1);

        //create API
        createAPI(apiName, apiContext, API_VERSION1, apiPublisherClientUser1);

        //Publish the API version 1.0.0
        HttpResponse httpResponsePublishAPIOldVersion = apiPublisherClientUser1.changeAPILifeCycleStatusToPublish(
                apiIdentifierAPI1Version1, false);
        assertTrue(httpResponsePublishAPIOldVersion.getData().contains("\"newStatus\" : \"PUBLISHED\""),
                "API is not publish correctly." + getAPIIdentifierString(apiIdentifierAPI1Version1));

        //subscribe Old version
        HttpResponse httpResponseSubscribeOldVersion = SubscribeAPI(apiIdentifierAPI1Version1, apiStoreClientUser1);
        assertEquals(httpResponseSubscribeOldVersion.getData(), "{\"error\" : false}",
                "Cannot subscribe to the old API version, " + getAPIIdentifierString(apiIdentifierAPI1Version1));


        // Create requestHeaders
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        //   Invoke  old version
        HttpResponse oldVersionInvokeResponse = HttpRequestUtil.doGet(API_BASE_URL + apiContext + "/" +
                API_VERSION1 + END_POINT_METHOD, requestHeaders);

        Assert.assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched");
        Assert.assertTrue(oldVersionInvokeResponse.getData().contains(API_RESPONSE_DATA),
                "Response data mismatched");

        //Block the API version 1.0.0
        APILifeCycleStateRequest blockUpdateRequest = new APILifeCycleStateRequest(apiName, API1_PROVIDER_NAME,
                APILifeCycleState.BLOCKED);
        blockUpdateRequest.setVersion(API_VERSION1);
        //Change API lifecycle  to Block
        HttpResponse blockAPIActionResponse = apiPublisherClientUser1.changeAPILifeCycleStatus(blockUpdateRequest);
        Assert.assertEquals(blockAPIActionResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        Assert.assertTrue(blockAPIActionResponse.getData().contains(
                "\"newStatus\" : \"BLOCKED\", \"oldStatus\" : \"PUBLISHED\""), "Response data ");

        //Access API after Block
        HttpResponse blockAPIInvokeResponse = HttpRequestUtil.doGet(API_BASE_URL + apiContext +
                "/" + API_VERSION1 + END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(blockAPIInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_SERVICE_UNAVAILABLE,
                "Response code mismatched");
        Assert.assertTrue(blockAPIInvokeResponse.getData().contains(
                "<am:code>700700</am:code><am:message>API blocked</am:message>"), "Response data mismatched");

    }


    @Test(groups = {"wso2.am"}, description = "Retire an API and check its visibility and accessibility in the API Store.",
            dependsOnMethods = "testAccessibilityOfBlockAPI")
    public void testAccessibilityOfRetireAPI() throws Exception {


        String apiName = "APILifeCycleTestAPI7";
        String apiContext = "testAPI7";

        APIIdentifier apiIdentifierAPI1Version1 = new APIIdentifier(API1_PROVIDER_NAME, apiName, API_VERSION1);


        //create API
        createAPI(apiName, apiContext, API_VERSION1, apiPublisherClientUser1);

        //Publish the API version 1.0.0
        HttpResponse httpResponsePublishAPIOldVersion = apiPublisherClientUser1.changeAPILifeCycleStatusToPublish(
                apiIdentifierAPI1Version1, false);
        assertTrue(httpResponsePublishAPIOldVersion.getData().contains("\"newStatus\" : \"PUBLISHED\""),
                "API is not publish correctly." + getAPIIdentifierString(apiIdentifierAPI1Version1));

        //subscribe Old version
        HttpResponse httpResponseSubscribeOldVersion = SubscribeAPI(apiIdentifierAPI1Version1, apiStoreClientUser1);
        assertEquals(httpResponseSubscribeOldVersion.getData(), "{\"error\" : false}",
                "Cannot subscribe to the old API version, " + getAPIIdentifierString(apiIdentifierAPI1Version1));


        // Create requestHeaders
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        //   Invoke  old version
        HttpResponse oldVersionInvokeResponse = HttpRequestUtil.doGet(API_BASE_URL + apiContext + "/" + API_VERSION1 +
                END_POINT_METHOD, requestHeaders);

        Assert.assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched");
        Assert.assertTrue(oldVersionInvokeResponse.getData().contains(API_RESPONSE_DATA),
                "Response data mismatched");

        //Block the API version 1.0.0
        APILifeCycleStateRequest retireUpdateRequest = new APILifeCycleStateRequest(apiName, API1_PROVIDER_NAME,
                APILifeCycleState.RETIRED);
        retireUpdateRequest.setVersion(API_VERSION1);
        HttpResponse retireAPIActionResponse = apiPublisherClientUser1.changeAPILifeCycleStatus(retireUpdateRequest);
        Assert.assertEquals(retireAPIActionResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");

        //Verify the API in API Store : API should not be available in the store.
        List<APIIdentifier> apiStoreAPIIdentifierList = APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(
                apiStoreClientUser1.getAPI(apiName));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierAPI1Version1, apiStoreAPIIdentifierList), false,
                "Api is not visible in API Store after publish. API Name:" + apiName + " API Version :" + API_VERSION1 +
                        " API Provider Name:" + API1_PROVIDER_NAME);

        // Invoke the API
        HttpResponse retireAPIInvokeResponse = HttpRequestUtil.doGet(API_BASE_URL + apiContext + "/" + API_VERSION1 +
                END_POINT_METHOD, requestHeaders);
        Assert.assertEquals(retireAPIInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Response code mismatched");
        Assert.assertTrue(retireAPIInvokeResponse.getData().contains
                        ("<am:code>404</am:code><am:type>Status report</am:type><am:message>Not Found</am:message>"),
                "Response data mismatched");
    }


    @AfterClass(alwaysRun = true)
    /**
     * Restore the backup api-manager.xml file.
     */
    public void cleanup() throws Exception {
        serverManager.restoreToLastConfiguration();
    }



    /**
     * Create a API in API Publisher
     *
     * @param apiName             Name of the API
     * @param apiContext          API Context
     * @param apiVersion          API Version
     * @param publisherRestClient Instance of APIPublisherRestClient
     * @return Response of the API creation server call.
     * @throws Exception
     */
    private HttpResponse createAPI(String apiName, String apiContext, String apiVersion, APIPublisherRestClient
            publisherRestClient) throws Exception {

        //Create the API Request
        APIRequest apiRequestBean = new APIRequest(apiName, apiContext, new URL(API1_END_POINT_URL));
        apiRequestBean.setTags(API1_TAGS);
        apiRequestBean.setDescription(API1_DESCRIPTION);
        apiRequestBean.setVersion(apiVersion);
        apiRequestBean.setVisibility("public");

        //Add the API to API Publisher
        return publisherRestClient.addAPI(apiRequestBean);

    }


    /**
     * Return a String with combining the value of API Name,API Version and API Provider Name as key:value format
     *
     * @param apiIdentifier Instance of APIIdentifier object  that include the  API Name,API Version and API Provider
     *                      Name to create the String
     * @return String with API Name,API Version and API Provider Name as key:value format
     */
    private String getAPIIdentifierString(APIIdentifier apiIdentifier) {
        return "API Name:" + apiIdentifier.getApiName() + " API Version:" + apiIdentifier.getVersion() +
                " API Provider Name :" + apiIdentifier.getProviderName();

    }

    /**
     * Subscribe  a API
     *
     * @param apiIdentifier   Instance of APIIdentifier object  that include the  API Name,API Version and API Provider
     * @param storeRestClient Instance of APIPublisherRestClient
     * @return Response of the API subscribe action
     * @throws Exception
     */
    private HttpResponse SubscribeAPI(APIIdentifier apiIdentifier, APIStoreRestClient storeRestClient) throws Exception {
        SubscriptionRequest oldVersionSubscriptionRequest = new SubscriptionRequest(apiIdentifier.getApiName(),
                apiIdentifier.getProviderName());
        oldVersionSubscriptionRequest.setVersion(apiIdentifier.getVersion());
        return storeRestClient.subscribe(oldVersionSubscriptionRequest);

    }

    /**
     * Generate the access token
     *
     * @param storeRestClient Instance of storeRestClient
     * @return Access Token as a String.
     * @throws Exception
     */
    private String getAccessToken(APIStoreRestClient storeRestClient) throws Exception {

        GenerateAppKeyRequest generateAppKeyRequest = new GenerateAppKeyRequest("DefaultApplication");
        String responseString = storeRestClient.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        return response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();

    }

}