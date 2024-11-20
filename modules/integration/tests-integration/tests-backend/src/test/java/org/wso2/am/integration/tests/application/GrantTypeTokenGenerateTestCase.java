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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.ApiResponse;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.tests.restapi.RESTAPITestConstants;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.testng.Assert.*;

/**
 * This test case is used to test authorization_code and implicitly token generation
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
public class GrantTypeTokenGenerateTestCase extends APIManagerLifecycleBaseTest {
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
    private final String SET_COOKIE_HEADER = "Set-Cookie";
    private final String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";
    private final String TIER_COLLECTION = APIMIntegrationConstants.API_TIER.UNLIMITED;
    private String endpointUrl;
    private Map<String, String> requestHeaders = new HashMap<String, String>();
    private String consumerKey, consumerSecret;
    private String tokenURL;
    private String identityLoginURL;
    private String apiId;
    private String applicationId;
    private String applicationIdWithoutCallback;
    private List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
    private Map<String, String> headers = new HashMap<String, String>();
    private ArrayList<String> grantTypes = new ArrayList<>();
    private APIRequest apiRequest;
    private ServerConfigurationManager serverConfigurationManager;
    private AutomationContext superTenantKeyManagerContext;

    @Factory(dataProvider = "userModeDataProvider")
    public GrantTypeTokenGenerateTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeTest(alwaysRun = true)
    public void loadConfiguration() throws Exception {

        superTenantKeyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE, TestUserMode.SUPER_TENANT_ADMIN);

        try {
            serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);

            //Apply application consent page related config
            serverConfigurationManager.applyConfiguration(new File(
                    getAMResourceLocation() + File.separator + "configFiles" + File.separator + "applicationConsentPage"
                            + File.separator + "deployment.toml"));
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Error while changing server configuration", e);
        }
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        storeURLHttp = getStoreURLHttp();
        endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api";
        tokenURL = getKeyManagerURLHttps() + "oauth2/token";
        identityLoginURL = getKeyManagerURLHttps() + "oauth2/authorize";

        //create Application
        HttpResponse applicationResponse = restAPIStore.createApplication(APP_NAME,
                "Test Application", APIThrottlingTier.UNLIMITED.getState(),
                ApplicationDTO.TokenTypeEnum.JWT);
        assertEquals(applicationResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");

        applicationId = applicationResponse.getData();

        String providerName = user.getUserName();

        List<APIOperationsDTO> apiOperationsDTOS = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb(RESTAPITestConstants.GET_METHOD);
        apiOperationsDTO
                .setAuthType(APIMIntegrationConstants.ResourceAuthTypes.APPLICATION_AND_APPLICATION_USER.getAuthType());
        apiOperationsDTO.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.TWENTYK_PER_MIN);
        apiOperationsDTO.setTarget("/add");
        apiOperationsDTOS.add(apiOperationsDTO);

        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(endpointUrl));

        apiRequest.setVersion(API_VERSION);
        apiRequest.setProvider(providerName);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setOperationsDTOS(apiOperationsDTOS);
        apiRequest.setTiersCollection(TIER_COLLECTION);
        apiRequest.setTags(TAGS);
        apiRequest.setDescription(DESCRIPTION);

        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);

        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.AUTHORIZATION_CODE);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.REFRESH_CODE);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.SAML2);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.NTLM);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.JWT);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.IMPLICIT);
    }

    @Test(groups = { "wso2.am" }, description = "Test Application Creation")
    public void testApplicationCreation() throws Exception {
        //generate keys for the subscription
        ApplicationKeyDTO applicationKeyDTO = restAPIStore
                .generateKeys(applicationId, "3600", CALLBACK_URL, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                        null, grantTypes);

        assertNotNull(applicationKeyDTO.getToken().getAccessToken());


        consumerKey = applicationKeyDTO.getConsumerKey();
        consumerSecret = applicationKeyDTO.getConsumerSecret();
        Assert.assertNotNull(consumerKey, "Consumer Key not found");
        Assert.assertNotNull(consumerSecret, "Consumer Secret not found ");
    }

    @Test(groups = {"wso2.am" }, description = "Test token generations with corrupted client credentials",
            dependsOnMethods = "testApplicationCreation")
    public void testTokenGenerationWithCorruptedClientCredentials() throws Exception {
        // Create corrupted client credentials
        byte[] encodedBytes = Base64.encodeBase64((consumerKey + ":" + consumerSecret).getBytes());
        String credentials = new String(encodedBytes, StandardCharsets.UTF_8);
        credentials = credentials.substring(0, credentials.length() - 2);

        // Get token response with corrupted client credentials
        headers.clear();
        headers.put("Authorization", "Basic " + credentials);
        urlParameters.clear();
        urlParameters.add(new BasicNameValuePair("grant_type",
                APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL));
        HttpResponse tokenResponse = HTTPSClientUtils.doPost(tokenURL, headers, urlParameters);

        // Validate token response
        Assert.assertEquals(tokenResponse.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,
                "Response code is not as expected");
        JSONObject responseData = new JSONObject(tokenResponse.getData());
        Assert.assertEquals(responseData.getString("error"), "invalid_client", "Error message is not as expected");
        Assert.assertNotNull(tokenResponse.getHeaders().get("WWW-Authenticate"), "WWW-Authenticate header is not found");
    }

    @Test(groups = { "wso2.am" }, description = "Test authorization_code token generation",
            dependsOnMethods = "testApplicationCreation")
    public void testAuthCode() throws Exception {

        //Sending first request to approve grant authorization to app
        headers.put("Content-Type", APPLICATION_CONTENT_TYPE);
        String url =
                identityLoginURL + "?response_type=code&" + "client_id=" + consumerKey + "&scope=PRODUCTION&redirect_uri="
                        + CALLBACK_URL;
        HttpResponse res = HTTPSClientUtils.doGet(url, headers);
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_MOVED_TEMPORARILY, "Response code is not as expected");
        String locationHeader = res.getHeaders().get(LOCATION_HEADER);
        Assert.assertNotNull(locationHeader, "Couldn't found Location Header");
        String sessionNonceCookie = res.getHeaders().get(SET_COOKIE_HEADER);
        Assert.assertNotNull(sessionNonceCookie, "Couldn't find the sessionNonceCookie Header");
        String sessionDataKey = getURLParameter(locationHeader, "sessionDataKey");
        Assert.assertNotNull(sessionDataKey, "Couldn't found sessionDataKey from the Location Header");

        //Login to the Identity with user/pass
        headers.clear();
        headers.put("Content-Type", APPLICATION_CONTENT_TYPE);
        headers.put("Cookie", sessionNonceCookie);
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
        headers.put("Cookie", sessionNonceCookie);
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
                identityLoginURL + "?response_type=token&" + "client_id=" + consumerKey + "&scope=PRODUCTION&redirect_uri="
                        + CALLBACK_URL;
        HttpResponse res = HTTPSClientUtils.doGet(url, headers);
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_MOVED_TEMPORARILY, "Response code is not as expected");
        String locationHeader = res.getHeaders().get(LOCATION_HEADER);
        Assert.assertNotNull(locationHeader, "Couldn't found Location Header");
        String sessionNonceCookie = res.getHeaders().get(SET_COOKIE_HEADER);
        Assert.assertNotNull(sessionNonceCookie, "Couldn't find the sessionNonceCookie Header");
        String sessionDataKey = getURLParameter(locationHeader, "sessionDataKey");
        Assert.assertNotNull(sessionDataKey, "Couldn't found sessionDataKey from the Location Header");

        //Login to the Identity with user/pass
        headers.clear();
        headers.put("Content-Type", APPLICATION_CONTENT_TYPE);
        headers.put("Cookie", sessionNonceCookie);
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
        headers.put("Cookie", sessionNonceCookie);
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
            dependsOnMethods = "testImplicit", expectedExceptions = ApiException.class)
    public void testApplicationCreationWithoutCallBackURL() throws Exception {
        //create Application
        HttpResponse applicationResponse = restAPIStore.createApplication(CALLBACK_URL_UPDATE_APP_NAME,
                "Test Application", APIThrottlingTier.UNLIMITED.getState(),
                ApplicationDTO.TokenTypeEnum.JWT);
        assertEquals(applicationResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");

        applicationIdWithoutCallback = applicationResponse.getData();

        //subscribing to the application
        HttpResponse subscribeResponse = subscribeToAPIUsingRest(apiId, applicationIdWithoutCallback,
                APIMIntegrationConstants.API_TIER.UNLIMITED, restAPIStore);

        assertEquals(subscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of old API version request not successful " +
                        getAPIIdentifierStringFromAPIRequest(apiRequest));
        assertTrue(StringUtils.isNotEmpty(subscribeResponse.getData()),
                "Error in subscribe of old API version" + getAPIIdentifierStringFromAPIRequest(apiRequest));


        //generate the key for the subscription

        ApiResponse<ApplicationKeyDTO> response = restAPIStore
                .generateKeysWithApiResponse(applicationIdWithoutCallback, "3600", "", ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                        null, grantTypes, Collections.emptyMap(),null);

        assertEquals(response.getStatusCode(), HTTP_RESPONSE_CODE_BAD_REQUEST,
                "Test Application Creation without callback URL not successful");
    }

    @Test(groups = { "wso2.am" }, description = "Test authorization_code token generation",
            dependsOnMethods = "testApplicationCreationWithoutCallBackURL")
    public void testAuthRequestWithoutCallbackURL() throws Exception {
        headers.clear();
        //Sending first request to approve grant authorization to app
        headers.put("Content-Type", APPLICATION_CONTENT_TYPE);
        String url =
                identityLoginURL + "?response_type=code&" + "client_id=" + consumerKey + "&scope=PRODUCTION&redirect_uri=";
        HttpResponse res = HTTPSClientUtils.doGet(url, headers);
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_MOVED_TEMPORARILY, "Response code is not as expected");
        String locationHeader = res.getHeaders().get(LOCATION_HEADER);
        Assert.assertNotNull(locationHeader, "Couldn't found Location Header");
        Assert.assertTrue(locationHeader.contains("oauthErrorCode"), "Redirection page should be a error page");
    }

    @Test(groups = { "wso2.am" }, description = "Test application display name in consent page",
            dependsOnMethods = "testApplicationCreation")
    public void testAuthCodeAppDisplayName() throws Exception {

        //Sending first request to approve grant authorization to app
        headers.clear();
        headers.put("Content-Type", APPLICATION_CONTENT_TYPE);
        String url = identityLoginURL + "?response_type=code&" + "client_id=" + consumerKey
                + "&scope=PRODUCTION&redirect_uri=" + CALLBACK_URL;
        HttpResponse res = HTTPSClientUtils.doGet(url, headers);
        String sessionNonceCookie = res.getHeaders().get(SET_COOKIE_HEADER);
        String sessionDataKey = getURLParameter(res.getHeaders().get(LOCATION_HEADER), "sessionDataKey");

        //Login to the Identity with user/pass
        headers.clear();
        headers.put("Content-Type", APPLICATION_CONTENT_TYPE);
        headers.put("Cookie", sessionNonceCookie);
        urlParameters.add(new BasicNameValuePair("username", user.getUserName()));
        urlParameters.add(new BasicNameValuePair("password", user.getPassword()));
        urlParameters.add(new BasicNameValuePair("tocommonauth", "true"));
        urlParameters.add(new BasicNameValuePair("sessionDataKey", sessionDataKey));

        res = HTTPSClientUtils.doPost(identityLoginURL, headers, urlParameters);
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_MOVED_TEMPORARILY, "Response code is not as expected");
        String locationHeader = res.getHeaders().get(LOCATION_HEADER);
        Assert.assertNotNull(locationHeader, "Couldn't found Location Header");

        //Test application display name in consent page
        res = HTTPSClientUtils.doGet(locationHeader, null);
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");
        Assert.assertEquals(res.getData().contains(APP_NAME), true,
                "App display name in consent page is not as expected");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        restAPIStore.deleteApplication(applicationIdWithoutCallback);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
    }

    @AfterTest(alwaysRun = true)
    public void restoreConfiguration() throws Exception {

        //Remove application consent page related config
        serverConfigurationManager.restoreToLastConfiguration();
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
