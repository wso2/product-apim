/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.jwt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.user.RemoteUserStoreManagerServiceClient;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE })
public class URLSafeJWTTestCase extends APIMIntegrationBaseTest {
    private ServerConfigurationManager serverConfigurationManager;
    private UserManagementClient userManagementClient1;
    private static final Log log = LogFactory.getLog(URLSafeJWTTestCase.class);

    private String publisherURLHttp;
    private String storeURLHttp;

    private final String INTERNAL_ROLE_SUBSCRIBER = "Internal/subscriber";
    private final String ROLE_SUBSCRIBER = "subscriber";

    private final String JWT_ASSERTION_HEADER = "X-JWT-Assertion";

    private String apiName = "URLSafeJWTTokenTestAPI";
    private String apiContext = "urlSafeTokenTest";
    private String tags = "token, jwt";
    private String description = "This is test API created by API manager integration test";
    private String providerName;
    private String apiVersion = "1.0.0";
    private String applicationName = "URLSafeJWTTest-application";
    private String backendURL;

    String subscriberUser = "subscriberUser1";
    String subscriberUserWithTenantDomain;
    String password = "password@123";

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        subscriberUserWithTenantDomain = subscriberUser + "@" + user.getUserDomain();
        publisherURLHttp = getPublisherURLHttp();
        storeURLHttp = getStoreURLHttp();

        //enable JWT token generation
        if(TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
            serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "tokenTest" + File.separator
                + "urlSafeTokenTest" + File.separator + "api-manager.xml"));
            serverConfigurationManager.applyConfiguration(new File(getAMResourceLocation() + File.separator
                + "configFiles" + File.separator + "tokenTest" + File.separator + "urlSafeTokenTest"
                + File.separator + "log4j.properties"));
            subscriberUserWithTenantDomain = subscriberUser;
            //Load the back-end API
            String gatewaySessionCookie = createSession(gatewayContextMgt);
            loadSynapseConfigurationFromClasspath("artifacts" + File.separator + "AM" + File.separator
                + "synapseconfigs" + File.separator + "rest" + File.separator + "jwt_backend.xml",
                gatewayContextMgt, gatewaySessionCookie);
        }

        backendURL = getSuperTenantAPIInvocationURLHttp("jwt_backend", "1.0");
        providerName = user.getUserName();
    }

    @Test(groups = { "wso2.am" }, description = "Enabling JWT Token generation, admin user claims", enabled = true)
    public void testEnableJWTAndClaims() throws Exception {

        RemoteUserStoreManagerServiceClient remoteUserStoreManagerServiceClient = new RemoteUserStoreManagerServiceClient(
                keyManagerContext.getContextUrls().getBackEndUrl(), user.getUserName(), user.getPassword());

        String profile = "default";

        remoteUserStoreManagerServiceClient.setUserClaimValue(user.getUserNameWithoutDomain(),
                                                              "http://wso2.org/claims/givenname", "first name", profile);

        remoteUserStoreManagerServiceClient.setUserClaimValue(user.getUserNameWithoutDomain(),
                                                              "http://wso2.org/claims/lastname", "last name", profile);

        // restart the server since updated claims not picked unless cache expired
        ServerConfigurationManager serverConfigManagerForTenant =
                new ServerConfigurationManager(superTenantKeyManagerContext);
        serverConfigManagerForTenant.restartGracefully();
        super.init(userMode);

        addAPI(apiName, apiVersion, apiContext, description, backendURL, tags, providerName);

        APIStoreRestClient apiStoreRestClient = new APIStoreRestClient(storeURLHttp);
        apiStoreRestClient.login(user.getUserName(), user.getPassword());

        apiStoreRestClient.addApplication(applicationName, APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN, "",
                                          "this-is-test");
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, user.getUserName());
        subscriptionRequest.setApplicationName(applicationName);
        apiStoreRestClient.subscribe(subscriptionRequest);

        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(applicationName);
        String responseString = apiStoreRestClient.generateApplicationKey(generateAppKeyRequest).getData();
        String accessToken = new JSONObject(responseString).getJSONObject("data").getJSONObject("key")
                .get("accessToken").toString();

        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttp(apiContext, apiVersion));
        get.addHeader("Authorization", "Bearer " + accessToken);

        HttpResponse response = httpclient.execute(get);

        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                            "Response code mismatched when api invocation");

        Header[] responseHeaders = response.getAllHeaders();
        Header jwtheader = pickHeader(responseHeaders, JWT_ASSERTION_HEADER);

        Assert.assertNotNull(jwtheader, JWT_ASSERTION_HEADER + " is not available in the backend request.");

        //check the jwt header
        String decodedJWTHeaderString = APIMTestCaseUtils.getDecodedURLSafeJWTHeader(jwtheader.getValue());

        if (decodedJWTHeaderString != null) {
            log.debug("Decoded JWT header String = " + decodedJWTHeaderString);
            JSONObject jsonHeaderObject = new JSONObject(decodedJWTHeaderString);
            Assert.assertEquals(jsonHeaderObject.getString("typ"), "JWT");
            Assert.assertEquals(jsonHeaderObject.getString("alg"), "RS256");
        }

        String decodedJWTString = APIMTestCaseUtils.getDecodedURLSafeJWT(jwtheader.getValue());

        log.debug("Decoded JWTString = " + decodedJWTString);

        JSONObject jsonObject = new JSONObject(decodedJWTString);

        // check default claims
        checkDefaultUserClaims(jsonObject);

        // check user profile info claims
        String claim = jsonObject.getString("http://wso2.org/claims/givenname");
        assertTrue("JWT claim givenname  not received" + claim, claim.contains("first name"));

        claim = jsonObject.getString("http://wso2.org/claims/lastname");
        assertTrue("JWT claim lastname  not received" + claim, claim.contains("last name"));

        boolean bExceptionOccured = false;
        try {
            jsonObject.getString("http://wso2.org/claims/wrongclaim");
        } catch (JSONException e) {
            bExceptionOccured = true;
        }

        assertTrue("JWT claim invalid  claim received", bExceptionOccured);
    }

    /**
     * This test case is a test for the fix fix for APIMANAGER-3912, where jwt claims are attempted to retrieve from
     * an invalidated cache and hence failed. In carbon 4.2 products cache invalidation timeout is not configurable
     * and is hardcoded to 15 mins. So the test case will take approximately 15mins to complete and it will delay the
     * product build unnecessarily, hence the test case is disabled.
     */
    @Test(groups = { "wso2.am" }, description = "JWT Token generation when JWT caching is enabled", enabled = false)
    public void testAPIAccessWhenJWTCachingEnabledTestCase()
            throws APIManagerIntegrationTestException, XPathExpressionException, IOException, JSONException,
                   InterruptedException {

        String applicationName = "JWTTokenCacheTestApp";
        String apiName = "JWTTokenCacheTestAPI";
        String apiContext = "JWTTokenCacheTestAPI";
        String apiVersion = "1.0.0";
        String description = "JWTTokenCacheTestAPI description";
        String endpointURL = gatewayUrlsWrk.getWebAppURLNhttp() + "response";
        String apiTier = APIMIntegrationConstants.API_TIER.GOLD;
        String tags = "token,jwt,cache";
        int waitingSecs = 900;

        addAPI(apiName, apiVersion, apiContext, description, endpointURL, tags, providerName);

        APIStoreRestClient apiStoreRestClient = new APIStoreRestClient(storeURLHttp);
        apiStoreRestClient.login(storeContext.getContextTenant().getContextUser().getUserName(),
                                 storeContext.getContextTenant().getContextUser().getPassword());

        apiStoreRestClient
                .addApplication(applicationName, APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN, "", "this-is-test");
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName,
                                                                          storeContext.getContextTenant().getContextUser().getUserName());
        subscriptionRequest.setApplicationName(applicationName);
        apiStoreRestClient.subscribe(subscriptionRequest);

        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(applicationName);
        String responseString = apiStoreRestClient.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();

        String url = gatewayUrlsWrk.getWebAppURLNhttp() + apiContext + "/" + apiVersion;

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + accessToken);
        //Invoke the API
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse httpResponse = HttpRequestUtil.doGet(url, headers);
        assertEquals("GET request failed for " + url, 200, httpResponse.getResponseCode());

        //Wait till cache is invalidated
        log.info("Waiting " + waitingSecs + " sec(s) till claims local cache is invalidated");
        Thread.sleep(waitingSecs * 1000);

        //Second attempt to invoke the API.
        httpResponse = HttpRequestUtil.doGet(url, headers);
        assertEquals("GET request failed for " + url +
                     ". Most probably due to a failed invalidated cache access to retrieve JWT claims.", 200,
                     httpResponse.getResponseCode());
    }

    private void checkDefaultUserClaims(JSONObject jsonObject) throws JSONException {
        String claim = jsonObject.getString("iss");
        assertTrue("JWT assertion is invalid", claim.contains("wso2.org/products/am"));

        claim = jsonObject.getString("http://wso2.org/claims/subscriber");
        assertTrue("JWT claim subscriber invalid. Received " + claim, claim.contains(user.getUserName()));

        claim = jsonObject.getString("http://wso2.org/claims/applicationname");
        assertTrue("JWT claim applicationname invalid. Received " + claim,
                   claim.contains(applicationName));

        claim = jsonObject.getString("http://wso2.org/claims/applicationtier");
        assertTrue("JWT claim applicationtier invalid. Received " + claim,
                   claim.contains(APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN));

        claim = jsonObject.getString("http://wso2.org/claims/apicontext");
        assertTrue("JWT claim apicontext invalid. Received " + claim,
                   claim.contains("/" + apiContext + "/" + jsonObject.getString("http://wso2.org/claims/version")));

        claim = jsonObject.getString("http://wso2.org/claims/version");
        assertTrue("JWT claim version invalid. Received " + claim, claim.contains(apiVersion));

        claim = jsonObject.getString("http://wso2.org/claims/tier");
        assertTrue("JWT claim tier invalid. Received " + claim, claim.contains("Gold"));

        claim = jsonObject.getString("http://wso2.org/claims/keytype");
        assertTrue("JWT claim keytype invalid. Received " + claim, claim.contains("PRODUCTION"));

        claim = jsonObject.getString("http://wso2.org/claims/usertype");
        assertTrue("JWT claim usertype invalid. Received " + claim, claim.contains("APPLICATION"));

        JSONArray roleClaim = jsonObject.getJSONArray("http://wso2.org/claims/role");
        String roles = roleClaim.toString();
        assertTrue("JWT claim role invalid. Received " + roles,
                   roles.contains("admin") && roles.contains("Internal/everyone"));
    }

    @Test(groups = { "wso2.am" }, description = "Enabling JWT Token generation, specific user claims", enabled = true,
            dependsOnMethods = "testEnableJWTAndClaims")
    public void testSpecificUserJWTClaims() throws Exception {

        String accessToken;

        userManagementClient1 = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                                                         user.getUserName(), user.getPassword());

        if (!userManagementClient1.roleNameExists(INTERNAL_ROLE_SUBSCRIBER)) {
            userManagementClient1.addInternalRole(ROLE_SUBSCRIBER, new String[]{},
                                                  new String[]{"/permission/admin/login",
                                                               "/permission/admin/manage/api/subscribe"});
        }

        if ((userManagementClient1 != null) &&
            !userManagementClient1.userNameExists(INTERNAL_ROLE_SUBSCRIBER, subscriberUser)) {
            userManagementClient1.addUser(subscriberUser, password, new String[] {INTERNAL_ROLE_SUBSCRIBER},
                                          null);
        }

        RemoteUserStoreManagerServiceClient remoteUserStoreManagerServiceClient = new RemoteUserStoreManagerServiceClient(
                keyManagerContext.getContextUrls().getBackEndUrl(), user.getUserName(), user.getPassword());

        String profile = "default";

        remoteUserStoreManagerServiceClient
                .setUserClaimValue(subscriberUser, "http://wso2.org/claims/givenname", "subscriber given name", profile);

        remoteUserStoreManagerServiceClient
                .setUserClaimValue(subscriberUser, "http://wso2.org/claims/lastname", "subscriber last name", profile);

        // restart the server since updated claims not picked unless cache expired
        ServerConfigurationManager serverConfigManagerForTenant =
                new ServerConfigurationManager(superTenantKeyManagerContext);
        serverConfigManagerForTenant.restartGracefully();
        super.init(userMode);

        waitForAPIDeploymentSync(providerName, apiName, apiVersion, APIMIntegrationConstants.IS_API_EXISTS);

        APIStoreRestClient apiStoreRestClient = new APIStoreRestClient(storeURLHttp);
        apiStoreRestClient.login(subscriberUserWithTenantDomain, password);

        apiStoreRestClient
                .addApplication(applicationName, APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN, "", "this-is-test");
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, providerName);
        subscriptionRequest.setApplicationName(applicationName);
        apiStoreRestClient.subscribe(subscriptionRequest);

        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(applicationName);
        String responseString = apiStoreRestClient.generateApplicationKey(generateAppKeyRequest).getData();
        accessToken = new JSONObject(responseString).getJSONObject("data").getJSONObject("key")
                .get("accessToken").toString();

        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttp(apiContext, apiVersion));
        get.addHeader("Authorization", "Bearer " + accessToken);

        HttpResponse response = httpclient.execute(get);

        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                            "Response code mismatched when api invocation");

        Header[] responseHeaders = response.getAllHeaders();
        Header jwtheader = pickHeader(responseHeaders, JWT_ASSERTION_HEADER);

        Assert.assertNotNull(jwtheader, JWT_ASSERTION_HEADER + " is not available in the backend request.");

        String decodedJWTString = APIMTestCaseUtils.getDecodedURLSafeJWT(jwtheader.getValue());

        log.debug("Decoded JWTString = " + decodedJWTString);

        JSONObject jsonObject = new JSONObject(decodedJWTString);

        // check claims
        String claim = jsonObject.getString("iss");
        assertTrue("JWT assertion is invalid", claim.contains("wso2.org/products/am"));

        claim = jsonObject.getString("http://wso2.org/claims/subscriber");
        assertTrue("JWT claim subscriber invalid. Received " + claim, claim.contains(subscriberUser));

        claim = jsonObject.getString("http://wso2.org/claims/applicationname");
        assertTrue("JWT claim applicationname invalid. Received " + claim, claim.contains(applicationName));

        apiStoreRestClient.removeAPISubscriptionByApplicationName(apiName, apiVersion, providerName, applicationName);
        apiStoreRestClient.removeApplication(applicationName);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
        if(userManagementClient1 != null) {
            userManagementClient1.deleteRole(INTERNAL_ROLE_SUBSCRIBER);
            userManagementClient1.deleteUser(subscriberUser);
        }
        if(TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            serverConfigurationManager.restoreToLastConfiguration();
        }
    }

    private void addAPI(String apiName, String apiVersion, String apiContext, String description, String endpointURL,
                        String tags, String providerName)
            throws APIManagerIntegrationTestException, MalformedURLException, XPathExpressionException {

        APIPublisherRestClient apiPublisher = new APIPublisherRestClient(publisherURLHttp);

        apiPublisher.login(user.getUserName(), user.getPassword());

        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(endpointURL));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(apiVersion);
        apiRequest.setVisibility("public");
        apiRequest.setProvider(providerName);
        apiPublisher.addAPI(apiRequest);

        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, providerName,
                                                                              APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        waitForAPIDeploymentSync(providerName, apiName, apiVersion, APIMIntegrationConstants.IS_API_EXISTS);
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] {TestUserMode.SUPER_TENANT_ADMIN },
                                new Object[] {TestUserMode.TENANT_ADMIN }};
    }

    @Factory(dataProvider = "userModeDataProvider")
    public URLSafeJWTTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }
}
