/*
 *Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.am.scenario.tests.api.secure.oauth2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.HttpClient;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.beans.APIManageBean;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.wso2.am.scenario.test.common.ScenarioTestUtils.readFromFile;

public class SecureUsingAuth2NegativeTestCases extends ScenarioTestBase {

    private APIStoreRestClient apiStore;
    private static final Log log = LogFactory.getLog(SecureUsingAuth2NegativeTestCases.class);
    private APIPublisherRestClient apiPublisher;
    private final String CREATOR_USERNAME = "3.1.1-creator";
    private final String CREATOR_PASSWORD = "password@123";
    private final String SUBSCRIBER_USERNAME = "3.1.1-subscriber";
    private final String SUBSCRIBER_PASSWORD = "password@123";
    private final String ADMIN_LOGIN_USERNAME = "admin";
    private final String ADMIN_PASSWORD = "admin";
    private final String API_DEVELOPER_USERNAME = "3.1.1-developer";
    private final String API_DEVELOPER_PASSWORD = "password@123";
    private final String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";
    private final String TEST_API_1_NAME = "PhoneVerifyAPI-1";
    private final String TEST_API_1_CONTEXT = "/phone";
    private final String TEST_API_1_VERSION = "1.0.0";
    private final String TEST_APPLICATION_NAME_1 = "TestApp1";
    private final String TEST_APPLICATION_NAME_2 = "TestApp2";
    private final String INVALID_TOKEN = "Bear !23sqsAe%2@4&~";
    private final String REVOKE_TOKEN = "revoke";
    private final String CUSTOM_AUTH_HEADER = "foo";
    private String accessToken;
    private String consumerKey;
    private String consumerSecret;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        apiStore = new APIStoreRestClient(storeURL);
        apiPublisher = new APIPublisherRestClient(publisherURL);

        createUserWithCreatorRole(CREATOR_USERNAME, CREATOR_PASSWORD, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        createUserWithSubscriberRole(SUBSCRIBER_USERNAME, SUBSCRIBER_PASSWORD, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        createUserWithPublisherAndCreatorRole(API_DEVELOPER_USERNAME, API_DEVELOPER_PASSWORD, ADMIN_LOGIN_USERNAME,
                ADMIN_PASSWORD);

        apiStore.login(SUBSCRIBER_USERNAME, SUBSCRIBER_PASSWORD);
        apiPublisher.login(API_DEVELOPER_USERNAME, API_DEVELOPER_PASSWORD);

        // create and publish sample API
        String swaggerFilePath = System.getProperty("framework.resource.location") + "swaggerFiles" + File.separator +
                "phoneverify-swagger.json";
        File swaggerFile = new File(swaggerFilePath);
        String swaggerContent = readFromFile(swaggerFile.getAbsolutePath());
        JSONObject swaggerJson = new JSONObject(swaggerContent);

        apiPublisher.developSampleAPI(swaggerJson, API_DEVELOPER_USERNAME, backendEndPoint, true, "public");
        createApplication(TEST_APPLICATION_NAME_1);

        // Check the visibility of the API in API store
        isAPIVisibleInStore(TEST_API_1_NAME, apiStore);

        // Add subscription to API
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(TEST_API_1_NAME, TEST_API_1_VERSION,
                API_DEVELOPER_USERNAME,
                TEST_APPLICATION_NAME_1, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);
        HttpResponse addSubscriptionResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(addSubscriptionResponse);
        if (log.isDebugEnabled()) {
            log.debug(TEST_APPLICATION_NAME_1 + " is subscribed to " + TEST_API_1_NAME);
        }
        accessToken = generateAppKeys(TEST_APPLICATION_NAME_1,36000);
    }

    @Test(description = "3.1.1.8")
    public void testOAuth2AuthorizationWithoutToken() throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        String gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT, TEST_API_1_VERSION,
                "/CheckPhoneNumber");
        if (log.isDebugEnabled()) {
            log.debug("Gateway HTTPS URL : " + gatewayHttpsUrl);
        }
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("PhoneNumber", "18006785432"));
        urlParameters.add(new BasicNameValuePair("LicenseKey", "0"));
        HttpResponse apiResponse = HttpClient.doPost(gatewayHttpsUrl, requestHeaders, urlParameters);
        assertEquals(apiResponse.getResponseCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                "Response code mismatched when api invocation. \n API response : " + apiResponse.getData());
    }

    @Test(description = "3.1.1.9")
    public void testOAuth2AuthorizationWithInValidToken() throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, INVALID_TOKEN);
        String gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT, TEST_API_1_VERSION,
                "/CheckPhoneNumber");
        if (log.isDebugEnabled()) {
            log.debug("Gateway HTTPS URL : " + gatewayHttpsUrl);
        }
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("PhoneNumber", "18006785432"));
        urlParameters.add(new BasicNameValuePair("LicenseKey", "0"));
        HttpResponse apiResponse = HttpClient.doPost(gatewayHttpsUrl, requestHeaders, urlParameters);
        assertEquals(apiResponse.getResponseCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                "Response code mismatched when api invocation. \n API response : " + apiResponse.getData());
    }

    @Test(description = "3.1.1.10")
    public void testResourceInvokedByExpiredToken() throws Exception {

        createApplication(TEST_APPLICATION_NAME_2);

        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(TEST_API_1_NAME, TEST_API_1_VERSION,
                API_DEVELOPER_USERNAME,
                TEST_APPLICATION_NAME_2, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);
        HttpResponse addSubscriptionResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(addSubscriptionResponse);
        if (log.isDebugEnabled()) {
            log.debug(TEST_APPLICATION_NAME_1 + " is subscribed to " + TEST_API_1_NAME);
        }

        accessToken = generateAppKeys(TEST_APPLICATION_NAME_2, 0);
        Map<String, String> requestHeaders = new HashMap<>();

        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        String gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT, TEST_API_1_VERSION,
                "/CheckPhoneNumber");

        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("PhoneNumber", "18006785432"));
        urlParameters.add(new BasicNameValuePair("LicenseKey", "0"));
        HttpResponse apiResponse = HttpClient.doPost(gatewayHttpsUrl, requestHeaders, urlParameters);
        assertEquals(apiResponse.getResponseCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                "Response code mismatched when api invocation. \n API response : " + apiResponse.getData());
    }

    @Test(description = "3.1.1.11")
    public void testResourceInvokedByRevokedToken() throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        URL tokenEndpointURL = new URL(gatewayHttpsURL + "/" + REVOKE_TOKEN);

        String requestBody = "token=" + accessToken;
        HttpResponse response = apiStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL);
        assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when revoke toke. \n API response : " +
                        response.getData());

        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        String gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT, TEST_API_1_VERSION,
                "/CheckPhoneNumber");

        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("PhoneNumber", "18006785432"));
        urlParameters.add(new BasicNameValuePair("LicenseKey", "0"));
        HttpResponse apiResponse = HttpClient.doPost(gatewayHttpsUrl, requestHeaders, urlParameters);
        assertEquals(apiResponse.getResponseCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                "Response code mismatched when api invocation. \n API response : " + apiResponse.getData());
    }

    @Test(description = "3.1.1.12")
    public void testSecurityTypeAsApplicationResourceInvokedByTokenWithPasswordGrantType() throws Exception {
        String accessTokenWithPasswordGrantType = generateAccessTokenByPasswordGrantType();
        Map<String, String> requestHeaders = new HashMap();
        requestHeaders.put("Authorization", "Bearer " + accessTokenWithPasswordGrantType);
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        String gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT, TEST_API_1_VERSION,
                "/CheckPhoneNumber");
        HttpResponse apiResponse = HttpClient.doGet(gatewayHttpsUrl + "?PhoneNumber=18006785432&LicenseKey=0",
                requestHeaders);
        assertEquals(apiResponse.getResponseCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                "Response code mismatched when api invocation. \n API response : " + apiResponse.getData());
    }

    @Test(description = "3.1.1.13")
    public void testSecurityTypeAsApplicationResourceUserInvokedByTokenWithClientCredentials() throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        String gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT, TEST_API_1_VERSION,
                "/CheckPhoneNumbers");
        HttpResponse apiResponse = HttpClient.doGet(gatewayHttpsUrl + "?PhoneNumbers=180785432&LicenseKey=0",
                requestHeaders);
        assertEquals(apiResponse.getResponseCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                "Response code mismatched when api invocation. \n API response : " + apiResponse.getData());
    }

    @Test(description = "3.1.1.15")
    public void testResourceApplicationInvokeByCustomAuthorization() throws Exception {
        Map<String, String> requestHeaders = new HashMap();
        apiPublisher.login(API_DEVELOPER_USERNAME, API_DEVELOPER_PASSWORD);
        updateAPIWithCustomHeader(TEST_API_1_NAME, CUSTOM_AUTH_HEADER);
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        String gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT, TEST_API_1_VERSION,
                "/CheckPhoneNumber");
        HttpResponse apiResponse = HttpClient.doGet(gatewayHttpsUrl + "?PhoneNumber=18006785432&LicenseKey=0",
                requestHeaders);
        assertEquals(apiResponse.getResponseCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                "Response code mismatched when api invocation. \n API response : " + apiResponse.getData());
        changeCustomAuthorizationHeaderInAPI(TEST_API_1_NAME, "");
        updateAPIWithCustomHeader(TEST_API_1_NAME, "");
    }

    @Test(description = "3.1.1.17", dataProvider = "IncorrectFormattedAuthorizationHeadersDataProvider",
            dataProviderClass = ScenarioDataProvider.class)
    public void testOAuth2AuthorizationWithIncorrectFormattedHeader(String tokenPrefix, String tokenValue)
            throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();

        if (tokenValue.isEmpty()) {
            requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, tokenPrefix);
        }
        if (tokenValue.equals("tokenVal")) {
            requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, tokenPrefix + accessToken);
        }
        if (tokenValue.equals("tokenDuplicated")) {
            requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, tokenPrefix + accessToken + "; " +
                    tokenPrefix + accessToken);
        }

        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        String gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT, TEST_API_1_VERSION,
                "/CheckPhoneNumber");
        if (log.isDebugEnabled()) {
            log.debug("Gateway HTTPS URL : " + gatewayHttpsUrl);
        }
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("PhoneNumber", "18006785432"));
        urlParameters.add(new BasicNameValuePair("LicenseKey", "0"));
        HttpResponse apiResponse = HttpClient.doPost(gatewayHttpsUrl, requestHeaders, urlParameters);
        assertEquals(apiResponse.getResponseCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                "Response code mismatched when api invocation. \n API response : " + apiResponse.getData());
    }

    @Test(description = "3.1.1.18")
    public void testModifyCustomAuthHeaderByUserWithCreatorRole() throws Exception {
        apiPublisher.login(CREATOR_USERNAME, CREATOR_PASSWORD);
        HttpResponse updateCustomHeaderResponse = changeCustomAuthorizationHeaderInAPI(TEST_API_1_NAME, CUSTOM_AUTH_HEADER);
        JSONObject responseData = new JSONObject(updateCustomHeaderResponse.getData());
        Assert.assertTrue(responseData.getBoolean(APIMIntegrationConstants.API_RESPONSE_ELEMENT_NAME_ERROR),
                "Error message received " + updateCustomHeaderResponse.getData());
    }

    public void createApplication(String applicationName) throws Exception {
        HttpResponse addApplicationResponse = null;
        addApplicationResponse = apiStore.addApplication(applicationName,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "description");
        verifyResponse(addApplicationResponse);
        if (log.isDebugEnabled()) {
            log.debug("Application - " + applicationName + "is created successfully");
        }
    }

    public void updateAPIWithCustomHeader(String apiName, String customAuthHeader) throws Exception {
        HttpResponse response = changeCustomAuthorizationHeaderInAPI(TEST_API_1_NAME, CUSTOM_AUTH_HEADER);
        verifyResponse(response);
        publishAPI(TEST_API_1_NAME);
    }

    public void publishAPI(String apiName) throws Exception {
        org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest updateLifeCycle =
                new org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest(apiName,
                        API_DEVELOPER_USERNAME, APILifeCycleState.PUBLISHED);
        HttpResponse apiPublishResponse = apiPublisher.changeAPILifeCycleStatus(updateLifeCycle);
        verifyResponse(apiPublishResponse);
        waitForAPIDeploymentSync(API_DEVELOPER_USERNAME, TEST_API_1_NAME, TEST_API_1_VERSION,
                APIMIntegrationConstants.IS_API_EXISTS);
    }

    public String generateAppKeys(String applicationName,int validateTime) throws Exception {
        APPKeyRequestGenerator appKeyRequestGenerator = new APPKeyRequestGenerator(applicationName);
        appKeyRequestGenerator.setValidityTime(validateTime);
        HttpResponse keyGenerationResponse = null;
        keyGenerationResponse = apiStore.generateApplicationKey(appKeyRequestGenerator);
        log.info("Key generation response for application \'" + applicationName + "\' response data :"
                + keyGenerationResponse.getData());
        verifyResponse(keyGenerationResponse);
        JSONObject keyGenerationRespData = new JSONObject(keyGenerationResponse.getData());

        accessToken = (keyGenerationRespData.getJSONObject("data").getJSONObject("key"))
                .get("accessToken").toString();
        consumerKey = keyGenerationRespData.getJSONObject("data").getJSONObject("key").
                getString("consumerKey");
        consumerSecret = keyGenerationRespData.getJSONObject("data").getJSONObject("key").
                getString("consumerSecret");

        return accessToken;
    }

    public String generateAccessTokenByPasswordGrantType() throws Exception {
        URL tokenEndpointURL = new URL(gatewayHttpsURL + "/token");
        HttpResponse response;
        String requestBody;
        JSONObject accessTokenGenerationResponse;

        //Obtain user access token for user
        requestBody = "grant_type=password" + "&username=" + ADMIN_LOGIN_USERNAME + "&password=" + ADMIN_PASSWORD;
        response = apiStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL);
        accessTokenGenerationResponse = new JSONObject(response.getData());

        return accessTokenGenerationResponse.getString("access_token");
    }

    public HttpResponse changeCustomAuthorizationHeaderInAPI(String apiName, String customAuth) throws Exception {

        HttpResponse getSwaggerResponse = apiPublisher.getSwagger(apiName, TEST_API_1_VERSION, API_DEVELOPER_USERNAME);
        APIManageBean apiManageBean = new APIManageBean(apiName, TEST_API_1_VERSION, API_DEVELOPER_USERNAME,
                "https", "disabled", "resource_level",
                "Production and Sandbox", getSwaggerResponse.getData(), "Unlimited,Gold,Bronze");
        apiManageBean.setAuthorizationHeader(customAuth);
        HttpResponse apiManageResponse = apiPublisher.manageAPI(apiManageBean);

        return apiManageResponse;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        apiStore.removeApplication(TEST_APPLICATION_NAME_1);
        apiStore.removeApplication(TEST_APPLICATION_NAME_2);
        apiPublisher.deleteAPI(TEST_API_1_NAME, TEST_API_1_VERSION, API_DEVELOPER_USERNAME);
        deleteUser(SUBSCRIBER_USERNAME, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        deleteUser(API_DEVELOPER_USERNAME, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        deleteUser(CREATOR_USERNAME, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
    }
}
