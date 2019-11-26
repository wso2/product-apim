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
import org.testng.annotations.*;
import org.wso2.am.admin.clients.user.RemoteUserStoreManagerServiceClient;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class JWTTestCase extends APIManagerLifecycleBaseTest {
    private ServerConfigurationManager serverConfigurationManager;
    private UserManagementClient userManagementClient;
    private static final Log log = LogFactory.getLog(JWTTestCase.class);

    private final String INTERNAL_ROLE_SUBSCRIBER = "Internal/subscriber";
    private final String ROLE_SUBSCRIBER = "subscriber";
    private final String JWT_ASSERTION_HEADER = "X-JWT-Assertion";
    private final String PROTOTYPE_API_NAME = "JWTPrototypeAPIName";
    private final String PROTOTYPE_API_VERSION = "1.0.0";
    private final String PROTOTYPE_API_CONTEXT = "JWTPrototypeContext";
    private final String DEFAULT_PROFILE = "default";
    private String apiName;
    private String apiContext;
    private String tags = "token, jwt";
    private String description = "This is test API created by API manager integration test";
    private String providerName;
    private String apiVersion = "1.0.0";
    private String applicationName;
    private String endpointURL;
    String subscriberUsername = "subscriberUser2";
    String subscriberUserPassword = "password@123";

    @BeforeTest(alwaysRun = true)
    public void loadConfiguration() throws Exception {
        superTenantKeyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE,
                TestUserMode.SUPER_TENANT_ADMIN);
        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);
        serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "tokenTest" + File.separator + "deployment.toml"));
        serverConfigurationManager.applyConfiguration(new File(getAMResourceLocation() + File.separator
                + "configFiles" + File.separator + "tokenTest" + File.separator + "log4j2.properties"));
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        providerName = user.getUserName();
        endpointURL = getSuperTenantAPIInvocationURLHttp("jwt_backend", "1.0");
    }

    @Test(groups = {"wso2.am"}, description = "Enabling JWT Token generation, admin user claims", enabled = true)
    public void testEnableJWTAndClaims() throws Exception {

        apiName = "JWTTokenClaimTestAPI";
        apiContext = "JWTTokenClaimTestAPIContext";
        applicationName = "JWTClaimsTestApp";

        RemoteUserStoreManagerServiceClient remoteUserStoreManagerServiceClient = new
                RemoteUserStoreManagerServiceClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                user.getUserName(), user.getPassword());
        remoteUserStoreManagerServiceClient.setUserClaimValue(user.getUserNameWithoutDomain(),
                "http://wso2.org/claims/givenname", "first name", DEFAULT_PROFILE);
        remoteUserStoreManagerServiceClient.setUserClaimValue(user.getUserNameWithoutDomain(),
                "http://wso2.org/claims/lastname", "last name", DEFAULT_PROFILE);
        //create new Application
        ApplicationDTO applicationDTO = restAPIStore.addApplication(applicationName,
                APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN, "",
                "Test Application");
        String applicationId = applicationDTO.getApplicationId();
        //add an API, create an application , subscribe and obtain an access token
        String accessToken = addAndSubscribeToAPI(apiName, apiContext, description, applicationId, restAPIStore);

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
        String decodedJWTHeaderString = APIMTestCaseUtils.getDecodedJWTHeader(jwtheader.getValue());

        if (decodedJWTHeaderString != null) {
            log.debug("Decoded JWT header String = " + decodedJWTHeaderString);
            JSONObject jsonHeaderObject = new JSONObject(decodedJWTHeaderString);
            Assert.assertEquals(jsonHeaderObject.getString("typ"), "JWT");
            Assert.assertEquals(jsonHeaderObject.getString("alg"), "RS256");
        }
        String decodedJWTString = APIMTestCaseUtils.getDecodedJWT(jwtheader.getValue());
        log.debug("Decoded JWTString = " + decodedJWTString);
        JSONObject jsonObject = new JSONObject(decodedJWTString);

        // check default claims
        checkDefaultUserClaims(jsonObject, applicationName);
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
        assertTrue("JWT claim received is invalid", bExceptionOccured);
    }

    @Test(groups = {"wso2.am"}, description = "JWT Token generation when JWT caching is enabled", enabled = true,
            dependsOnMethods = "testEnableJWTAndClaims")
    public void testAPIAccessWhenJWTCachingEnabledTestCase() throws Exception {

        apiName = "JWTTokenCacheTestAPI";
        apiContext = "JWTTokenCacheTestAPI";
        description = "JWTTokenCacheTestAPI description";
        applicationName = "JWTTokenCacheTestApp";
        int waitingSecs = 20;

        ApplicationDTO applicationDTO = restAPIStore.addApplication(applicationName,
                APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN, "",
                "Test Application");
        String applicationId = applicationDTO.getApplicationId();

        String accessToken = addAndSubscribeToAPI(apiName, apiContext, description, applicationId,
                restAPIStore);
        String url = getAPIInvocationURLHttp(apiContext, apiVersion);
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + accessToken);
        //Invoke the API
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse httpResponse = HttpRequestUtil.doGet(url,
                headers);
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

    @Test(groups = {"wso2.am"}, description = "Enabling JWT Token generation, specific user claims", enabled = true,
            dependsOnMethods = "testAPIAccessWhenJWTCachingEnabledTestCase")
    public void testSpecificUserJWTClaims() throws Exception {

        apiName = "JWTUserClaimAPI";
        apiContext = "JWTUserClaimAPIContext";
        applicationName = "JWTUserClaimTestAPP";
        userManagementClient = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                user.getUserName(), user.getPassword());
        if (!userManagementClient.roleNameExists(INTERNAL_ROLE_SUBSCRIBER)) {
            userManagementClient.addInternalRole(ROLE_SUBSCRIBER, new String[]{},
                    new String[]{"/permission/admin/login", "/permission/admin/manage/api/subscribe"});
        }
        if (!userManagementClient.userNameExists(INTERNAL_ROLE_SUBSCRIBER, subscriberUsername)) {
            userManagementClient.addUser(subscriberUsername, subscriberUserPassword,
                    new String[]{INTERNAL_ROLE_SUBSCRIBER}, null);
        }
        RemoteUserStoreManagerServiceClient remoteUserStoreManagerServiceClient = new RemoteUserStoreManagerServiceClient(
                keyManagerContext.getContextUrls().getBackEndUrl(), user.getUserName(), user.getPassword());

        remoteUserStoreManagerServiceClient
                .setUserClaimValue(subscriberUsername, "http://wso2.org/claims/givenname", "subscriber given name", DEFAULT_PROFILE);
        remoteUserStoreManagerServiceClient
                .setUserClaimValue(subscriberUsername, "http://wso2.org/claims/lastname", "subscriber last name",
                        DEFAULT_PROFILE);

        RestAPIStoreImpl restAPIStoreSubsriberUser = new RestAPIStoreImpl(subscriberUsername,
                subscriberUserPassword, keyManagerContext.getContextTenant().getDomain(), storeURLHttps);
        ApplicationDTO applicationDTO = restAPIStoreSubsriberUser.addApplication(applicationName,
                APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN, "",
                "Test Application");
        String applicationId = applicationDTO.getApplicationId();
        String accessToken = addAndSubscribeToAPI(apiName, apiContext, description, applicationId,
                restAPIStoreSubsriberUser);
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttp(apiContext, apiVersion));
        get.addHeader("Authorization", "Bearer " + accessToken);
        HttpResponse response = httpclient.execute(get);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");
        Header[] responseHeaders = response.getAllHeaders();
        Header jwtheader = pickHeader(responseHeaders, JWT_ASSERTION_HEADER);
        Assert.assertNotNull(jwtheader, JWT_ASSERTION_HEADER + " is not available in the backend request.");
        String decodedJWTString = APIMTestCaseUtils.getDecodedJWT(jwtheader.getValue());
        log.debug("Decoded JWTString = " + decodedJWTString);
        JSONObject jsonObject = new JSONObject(decodedJWTString);
        // check claims
        String claim = jsonObject.getString("iss");
        assertTrue("JWT assertion is invalid", claim.contains("wso2.org/products/am"));
        if ("carbon.super".equalsIgnoreCase(user.getUserDomain())) {
            //Do the signature verification for super tenant as tenant key store not there accessible
            String jwtHeader = APIMTestCaseUtils.getDecodedJWTHeader(jwtheader.getValue());
            byte[] jwtSignature = APIMTestCaseUtils.getDecodedJWTSignature(jwtheader.getValue());
            String jwtAssertion = APIMTestCaseUtils.getJWTAssertion(jwtheader.getValue());
            boolean isSignatureValid = APIMTestCaseUtils.isJwtSignatureValid(jwtAssertion, jwtSignature, jwtHeader);
            assertTrue("JWT signature verification failed", isSignatureValid);
        }
        claim = jsonObject.getString("http://wso2.org/claims/subscriber");
        assertTrue("JWT claim subscriber invalid. Received " + claim, claim.contains(subscriberUsername));
        claim = jsonObject.getString("http://wso2.org/claims/applicationname");
        assertTrue("JWT claim applicationname invalid. Received " + claim, claim.contains(applicationName));
        restAPIStoreSubsriberUser.deleteApplication(applicationId);
    }

    @Test(groups = {"wso2.am"}, description = "Invoking Prototype api with JWT enabled",
            dependsOnMethods = "testSpecificUserJWTClaims")
    public void testPrototypeInvocationWithJWTEnabled() throws Exception {

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(PROTOTYPE_API_NAME,
                PROTOTYPE_API_CONTEXT, PROTOTYPE_API_VERSION, providerName, new URL(endpointURL));
        apiCreationRequestBean.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        //define resources
        ArrayList<APIResourceBean> resList = new ArrayList<APIResourceBean>();
        APIResourceBean res = new APIResourceBean(APIMIntegrationConstants.HTTP_VERB_GET,
                APIMIntegrationConstants.ResourceAuthTypes.APPLICATION.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED, "/*");
        resList.add(res);
        apiCreationRequestBean.setResourceBeanList(resList);
        //add test api
        APIDTO apidto = restAPIPublisher.addAPI(apiCreationRequestBean);
        String apiId = apidto.getId();
        restAPIPublisher.deployPrototypeAPI(apiId);
        String invokeURL = getAPIInvocationURLHttp(PROTOTYPE_API_CONTEXT, PROTOTYPE_API_VERSION);
        Map<String, String> requestHeaders = new HashMap<String, String>();
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse serviceResponse = HTTPSClientUtils.doGet(
                invokeURL, requestHeaders);
        Assert.assertEquals(serviceResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code is not as expected");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
        if (userManagementClient != null) {
            userManagementClient.deleteRole(INTERNAL_ROLE_SUBSCRIBER);
            userManagementClient.deleteUser(subscriberUsername);
        }
    }

    @AfterTest(alwaysRun = true)
    public void restoreConfiguration() throws Exception {
        superTenantKeyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE,
                TestUserMode.SUPER_TENANT_ADMIN);
        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);
        serverConfigurationManager.restoreToLastConfiguration(false);
    }

    private String addAndSubscribeToAPI(String apiName, String apiContext, String description,
                                        String applicationId, RestAPIStoreImpl restApiClient)
            throws Exception {

        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(endpointURL));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(apiVersion);
        apiRequest.setVisibility("public");
        apiRequest.setProvider(providerName);
        //create publish and subscribe to the API
        createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restApiClient, applicationId,
                APIMIntegrationConstants.API_TIER.GOLD);
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        //generate keys
        ApplicationKeyDTO applicationKeyDTO = restApiClient.generateKeys(applicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        return applicationKeyDTO.getToken().getAccessToken();
    }

    private void checkDefaultUserClaims(JSONObject jsonObject, String applicationName) throws JSONException {
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

        claim = jsonObject.getString("http://wso2.org/claims/keytype");
        assertTrue("JWT claim keytype invalid. Received " + claim, claim.contains("PRODUCTION"));

        JSONArray roleClaim = jsonObject.getJSONArray("http://wso2.org/claims/role");
        String roles = roleClaim.toString();
        assertTrue("JWT claim role invalid. Received " + roles,
                roles.contains("admin") && roles.contains("Internal/everyone"));
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN}};
    }

    @Factory(dataProvider = "userModeDataProvider")
    public JWTTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }
}
