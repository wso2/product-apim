/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*
*/
package org.wso2.am.integration.tests.application;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This test case is used to test authorization_code and implicitly token generation
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
public class GrantTypeTokenGenerateTestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(GrantTypeTokenGenerateTestCase.class);
    private final String API_NAME = "GrantTypeTokenGenerateAPIName";
    private final String API_CONTEXT = "GrantTypeTokenGenerateContext";
    private final String DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION = "1.0.0";
    private final String APP_NAME = "GrantTypeTokenGenerateApp";
    private final String CALLBACK_URL_UPDATE_APP_NAME = "GrantTypeTokenGenerateCallbackApp";
    private final String CALLBACK_URL = "https://localhost:9443/store/";
    private final String TAGS = "grantType,implicitly,code";
    private final String APPLICATION_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private final String LOCATION_HEADER = "Location";
    private final String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";
    private final String TIER_COLLECTION = APIMIntegrationConstants.API_TIER.UNLIMITED;
    private String publisherURLHttps;
    private String storeURLHttp;
    private APICreationRequestBean apiCreationRequestBean;
    private List<APIResourceBean> resList;
    private String endpointUrl;
    private Map<String, String> requestHeaders = new HashMap<String, String>();
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String consumerKey, consumerSecret;
    private String authorizeURL;
    private String tokenURL;
    private String identityLoginURL;
    private List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
    private Map<String, String> headers = new HashMap<String, String>();

    @Factory(dataProvider = "userModeDataProvider")
    public GrantTypeTokenGenerateTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        publisherURLHttps = publisherUrls.getWebAppURLHttp();
        storeURLHttp = getStoreURLHttp();
        endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api";
        apiPublisher = new APIPublisherRestClient(publisherURLHttps);
        apiPublisher.login(user.getUserName(), user.getPassword());
        apiStore = new APIStoreRestClient(storeURLHttp);
        apiStore.login(user.getUserName(), user.getPassword());
        authorizeURL = gatewayUrlsWrk.getWebAppURLNhttps() + "/authorize";
        tokenURL = gatewayUrlsWrk.getWebAppURLNhttps() + "/token";
        identityLoginURL = getKeyManagerURLHttps() + "/oauth2/authorize";
    }

    @Test(groups = { "wso2.am" }, description = "Sample API creation")
    public void testAPICreation() throws Exception {
        String providerName = user.getUserName();

        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION, providerName,
                new URL(endpointUrl));
        apiCreationRequestBean.setTags(TAGS);
        apiCreationRequestBean.setDescription(DESCRIPTION);
        apiCreationRequestBean.setTiersCollection(TIER_COLLECTION);

        //define resources
        resList = new ArrayList<APIResourceBean>();
        APIResourceBean res1 = new APIResourceBean(APIMIntegrationConstants.HTTP_VERB_GET,
                APIMIntegrationConstants.ResourceAuthTypes.APPLICATION_AND_APPLICATION_USER.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.PLUS, "/add");
        resList.add(res1);

        apiCreationRequestBean.setResourceBeanList(resList);

        //add test api
        HttpResponse serviceResponse = apiPublisher.addAPI(apiCreationRequestBean);
        verifyResponse(serviceResponse);

        //publish the api
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(API_NAME, user.getUserName(),
                APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);
    }

    @Test(groups = { "wso2.am" }, description = "Test Application Creation", dependsOnMethods = "testAPICreation")
    public void testApplicationCreation() throws Exception {

        //add a application
        HttpResponse serviceResponse = apiStore
                .addApplication(APP_NAME, APIThrottlingTier.UNLIMITED.getState(), CALLBACK_URL, "this-is-test");
        verifyResponse(serviceResponse);

        //subscribe to the api
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(API_NAME, user.getUserName());
        subscriptionRequest.setApplicationName(APP_NAME);
        subscriptionRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        serviceResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(serviceResponse);

        //generate the key for the subscription
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(APP_NAME);
        generateAppKeyRequest.setCallbackUrl(CALLBACK_URL);
        serviceResponse = apiStore.generateApplicationKey(generateAppKeyRequest);
        verifyResponse(serviceResponse);
        JSONObject response = new JSONObject(serviceResponse.getData());
        consumerKey = response.getJSONObject("data").getJSONObject("key").get("consumerKey").toString();
        consumerSecret = response.getJSONObject("data").getJSONObject("key").get("consumerSecret").toString();
        Assert.assertNotNull(consumerKey, "Consumer Key not found");
        Assert.assertNotNull(consumerSecret, "Consumer Secret not found ");
    }

    @Test(groups = { "wso2.am" }, description = "Test authorization_code token generation",
            dependsOnMethods = "testApplicationCreation")
    public void testAuthCode() throws Exception {

        //Sending first request to approve grant authorization to app
        headers.put("Content-Type", APPLICATION_CONTENT_TYPE);
        String url =
                authorizeURL + "?response_type=code&" + "client_id=" + consumerKey + "&scope=PRODUCTION&redirect_uri="
                        + CALLBACK_URL;
        HttpResponse res = HTTPSClientUtils.doGet(url, headers);
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_MOVED_TEMPORARILY, "Response code is not as expected");
        String locationHeader = res.getHeaders().get(LOCATION_HEADER);
        Assert.assertNotNull(locationHeader, "Couldn't found Location Header");
        String sessionDataKey = getURLParameter(locationHeader, "sessionDataKey");
        Assert.assertNotNull(sessionDataKey, "Couldn't found sessionDataKey from the Location Header");

        //Login to the Identity with user/pass
        headers.clear();
        headers.put("Content-Type", APPLICATION_CONTENT_TYPE);
        urlParameters.add(new BasicNameValuePair("username", user.getUserName()));
        urlParameters.add(new BasicNameValuePair("password", user.getPassword()));
        urlParameters.add(new BasicNameValuePair("tocommonauth", "true"));
        urlParameters.add(new BasicNameValuePair("sessionDataKey", sessionDataKey));

        res = HTTPSClientUtils.doPost(identityLoginURL, headers, urlParameters);
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_MOVED_TEMPORARILY, "Response code is not as expected");
        locationHeader = res.getHeaders().get(LOCATION_HEADER);
        Assert.assertNotNull(locationHeader, "Couldn't found Location Header");
        String sessionDataKeyConsent = getURLParameter(locationHeader, "sessionDataKeyConsent");
        Assert.assertNotNull(sessionDataKey, "Couldn't found sessionDataKeyConsent from the Location Header");

        //approve the application by logged user
        headers.clear();
        urlParameters.clear();
        headers.put("Content-Type", APPLICATION_CONTENT_TYPE);
        urlParameters.add(new BasicNameValuePair("consent", "approve"));
        urlParameters.add(new BasicNameValuePair("hasApprovedAlways", "false"));
        urlParameters.add(new BasicNameValuePair("sessionDataKeyConsent", sessionDataKeyConsent));

        res = HTTPSClientUtils.doPost(identityLoginURL, headers, urlParameters);
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_MOVED_TEMPORARILY, "Response code is not as expected");
        locationHeader = res.getHeaders().get(LOCATION_HEADER);
        Assert.assertNotNull(locationHeader, "Couldn't found Location Header");
        String tempCode = getURLParameter(locationHeader, "code");
        Assert.assertNotNull(tempCode, "Couldn't found auth code from the Location Header");

        //get accessToken
        headers.clear();
        urlParameters.clear();
        urlParameters.add(new BasicNameValuePair("grant_type", AUTHORIZATION_CODE_GRANT_TYPE));
        urlParameters.add(new BasicNameValuePair("code", tempCode));
        urlParameters.add(new BasicNameValuePair("redirect_uri", CALLBACK_URL));
        urlParameters.add(new BasicNameValuePair("client_secret", consumerSecret));
        urlParameters.add(new BasicNameValuePair("client_id", consumerKey));

        res = HTTPSClientUtils.doPost(tokenURL, headers, urlParameters);
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");
        JSONObject response = new JSONObject(res.getData());
        String accessToken = response.getString("access_token");
        Assert.assertNotNull(accessToken, "Couldn't found accessToken");

        //invoke the api with generate token
        requestHeaders.clear();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION);
        res = HTTPSClientUtils.doGet(invokeURL + "/add?x=1&y=1", requestHeaders);
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");
    }

    @Test(groups = { "wso2.am" }, description = "Test implicit token generation",
            dependsOnMethods = "testAuthCode")
    public void testImplicit() throws Exception {
        headers.clear();
        urlParameters.clear();
        //Sending first request to approve grant authorization to app
        headers.put("Content-Type", APPLICATION_CONTENT_TYPE);
        String url =
                authorizeURL + "?response_type=token&" + "client_id=" + consumerKey + "&scope=PRODUCTION&redirect_uri="
                        + CALLBACK_URL;
        HttpResponse res = HTTPSClientUtils.doGet(url, headers);
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_MOVED_TEMPORARILY, "Response code is not as expected");
        String locationHeader = res.getHeaders().get(LOCATION_HEADER);
        Assert.assertNotNull(locationHeader, "Couldn't found Location Header");
        String sessionDataKey = getURLParameter(locationHeader, "sessionDataKey");
        Assert.assertNotNull(sessionDataKey, "Couldn't found sessionDataKey from the Location Header");

        //Login to the Identity with user/pass
        headers.clear();
        headers.put("Content-Type", APPLICATION_CONTENT_TYPE);
        urlParameters.add(new BasicNameValuePair("username", user.getUserName()));
        urlParameters.add(new BasicNameValuePair("password", user.getPassword()));
        urlParameters.add(new BasicNameValuePair("tocommonauth", "true"));
        urlParameters.add(new BasicNameValuePair("sessionDataKey", sessionDataKey));

        res = HTTPSClientUtils.doPost(identityLoginURL, headers, urlParameters);
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_MOVED_TEMPORARILY, "Response code is not as expected");
        locationHeader = res.getHeaders().get(LOCATION_HEADER);
        Assert.assertNotNull(locationHeader, "Couldn't found Location Header");
        String sessionDataKeyConsent = getURLParameter(locationHeader, "sessionDataKeyConsent");
        Assert.assertNotNull(sessionDataKey, "Couldn't found sessionDataKeyConsent from the Location Header");

        //approve the application by logged user
        headers.clear();
        urlParameters.clear();
        headers.put("Content-Type", APPLICATION_CONTENT_TYPE);
        urlParameters.add(new BasicNameValuePair("consent", "approve"));
        urlParameters.add(new BasicNameValuePair("hasApprovedAlways", "false"));
        urlParameters.add(new BasicNameValuePair("sessionDataKeyConsent", sessionDataKeyConsent));

        res = HTTPSClientUtils.doPost(identityLoginURL, headers, urlParameters);
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_MOVED_TEMPORARILY, "Response code is not as expected");
        locationHeader = res.getHeaders().get(LOCATION_HEADER);
        Assert.assertNotNull(locationHeader, "Couldn't found Location Header");
        String accessToken = getURLParameter(locationHeader, "access_token");
        Assert.assertNotNull(accessToken, "Couldn't found auth code from the Location Header");

        //invoke the api with generate token
        requestHeaders.clear();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION);
        res = HTTPSClientUtils.doGet(invokeURL + "/add?x=1&y=1", requestHeaders);
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");
    }

    @Test(groups = { "wso2.am" }, description = "Test Application Creation without callback URL",
            dependsOnMethods = "testImplicit")
    public void testApplicationCreationWithoutCallBackURL() throws Exception {
        //add a application
        HttpResponse serviceResponse = apiStore
                .addApplication(CALLBACK_URL_UPDATE_APP_NAME, APIThrottlingTier.UNLIMITED.getState(), "",
                        "this-is-test");
        verifyResponse(serviceResponse);

        //subscribe to the api
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(API_NAME, user.getUserName());
        subscriptionRequest.setApplicationName(CALLBACK_URL_UPDATE_APP_NAME);
        subscriptionRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        serviceResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(serviceResponse);

        //generate the key for the subscription
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(CALLBACK_URL_UPDATE_APP_NAME);
        serviceResponse = apiStore.generateApplicationKey(generateAppKeyRequest);
        verifyResponse(serviceResponse);
        JSONObject response = new JSONObject(serviceResponse.getData());
        consumerKey = response.getJSONObject("data").getJSONObject("key").get("consumerKey").toString();
        consumerSecret = response.getJSONObject("data").getJSONObject("key").get("consumerSecret").toString();
        Assert.assertNotNull(consumerKey, "Consumer Key not found");
        Assert.assertNotNull(consumerSecret, "Consumer Secret not found ");
    }

    @Test(groups = { "wso2.am" }, description = "Test authorization_code token generation",
            dependsOnMethods = "testApplicationCreationWithoutCallBackURL")
    public void testAuthRequestWithoutCallbackURL() throws Exception {
        headers.clear();
        //Sending first request to approve grant authorization to app
        headers.put("Content-Type", APPLICATION_CONTENT_TYPE);
        String url =
                authorizeURL + "?response_type=code&" + "client_id=" + consumerKey + "&scope=PRODUCTION&redirect_uri=";
        HttpResponse res = HTTPSClientUtils.doGet(url, headers);
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_MOVED_TEMPORARILY, "Response code is not as expected");
        String locationHeader = res.getHeaders().get(LOCATION_HEADER);
        Assert.assertNotNull(locationHeader, "Couldn't found Location Header");
        Assert.assertTrue(locationHeader.contains("oauthErrorCode"), "Redirection page should be a error page");
    }

    @Test(groups = { "wso2.am" }, description = "Test authorization_code token generation",
            dependsOnMethods = "testAuthRequestWithoutCallbackURL")
    public void testApplicationUpdateAndTestKeyGeneration() throws Exception {
        String keyType = "PRODUCTION";
        String authorizedDomains = "ALL";
        String retryAfterFailure = String.valueOf(false);
        String jsonParams = "{\"grant_types\":\"urn:ietf:params:oauth:grant-type:saml2-bearer iwa:ntlm implicit "
                + "refresh_token client_credentials authorization_code password\"}";

        HttpResponse response = apiStore
                .updateClientApplication(CALLBACK_URL_UPDATE_APP_NAME, keyType, authorizedDomains, retryAfterFailure,
                        jsonParams, CALLBACK_URL);
        verifyResponse(response);

        //Test the Authorization Code key generation with updates values
        testAuthCode();
        //Test the Implicit key generation with updates values
        testImplicit();
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeApplication(APP_NAME);
        apiStore.removeApplication(CALLBACK_URL_UPDATE_APP_NAME);
        apiPublisher.deleteAPI(API_NAME, API_VERSION, user.getUserName());
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }, };
    }

    /**
     * return the required parameter value from the URL
     *
     * @param url       URL as String
     * @param attribute name of the attribute
     * @return attribute value as String
     */
    private String getURLParameter(String url, String attribute) {
        try {
            Pattern p = Pattern.compile(attribute + "=([^&]+)");
            Matcher m = p.matcher(url);
            if (m.find()) {
                return m.group(1);
            }
        } catch (PatternSyntaxException ignore) {
            // error ignored
        }
        return null;
    }
}
