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

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.ApiResponse;
import org.wso2.am.integration.clients.store.api.v1.dto.APIKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.token.TokenUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.andes.util.Strings;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.identity.application.common.model.xsd.Claim;
import org.wso2.carbon.identity.application.common.model.xsd.ClaimConfig;
import org.wso2.carbon.identity.application.common.model.xsd.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.xsd.ServiceProvider;
import org.wso2.carbon.identity.application.mgt.stub.IdentityApplicationManagementServiceIdentityApplicationManagementException;
import org.wso2.carbon.identity.claim.metadata.mgt.stub.ClaimMetadataManagementServiceClaimMetadataException;
import org.wso2.carbon.identity.oauth.stub.OAuthAdminServiceIdentityOAuthAdminException;
import org.wso2.carbon.identity.oauth.stub.dto.OAuthConsumerAppDTO;
import org.wso2.carbon.um.ws.api.stub.ClaimValue;
import org.wso2.carbon.um.ws.api.stub.RemoteUserStoreManagerServiceUserStoreExceptionException;
import org.wso2.carbon.user.core.UserStoreException;

import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

public class JWTTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(JWTTestCase.class);

    private final String JWT_ASSERTION_HEADER = "X-JWT-Assertion";
    private final String DEFAULT_PROFILE = "default";
    private String apiName = "JWTUserClaimAPI";
    private String apiContext = "jwtTest";
    private String providerName;
    private String apiVersion = "1.0.0";
    private String oauthApplicationName = "OauthAppForJWTTest";
    private String jwtApplicationName = "JWTAppForJWTTest";
    private String apiKeyApplicationName = "ApiKeyAppForJWTTest";
    private String authCodeApplicationName = "AuthCodeAppForJWTTest";
    private String api2Name = "ApiKeyOnlyAPI";
    private String api2Context = "apiKeyTest";
    private String endpointURL;
    String users[] = {"subscriberUser2", "subscriberUser2@wso2.com", "subscriberUser2@abc.com"};
    String enduserPassword = "password@123";
    private String oauthApplicationId;
    private String jwtApplicationId;
    private String apiKeyApplicationId;
    private String authCodeApplicationId;
    private String apiId;
    private String api2Id;
    URL tokenEndpointURL;
    private String tokenURL;
    private String identityLoginURL;
    private String jwksKidClaim;
    private final String CALLBACK_URL = "https://localhost:9443/store/";

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
        providerName = user.getUserName();
        endpointURL = getSuperTenantAPIInvocationURLHttp("jwt_backend", "1.0");
        tokenURL = getKeyManagerURLHttps() + "oauth2/token";
        identityLoginURL = getKeyManagerURLHttps() + "oauth2/authorize";

        //create Oauth Base App
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse applicationDTO =
                restAPIStore.createApplication(oauthApplicationName, "Test Application",
                        APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN,
                        ApplicationDTO.TokenTypeEnum.OAUTH);
        oauthApplicationId = applicationDTO.getData();
        //create JWT Base App
        applicationDTO =
                restAPIStore.createApplication(jwtApplicationName, "JWT Application",
                        APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN,
                        ApplicationDTO.TokenTypeEnum.JWT);
        jwtApplicationId = applicationDTO.getData();
        //create API Key Base App
        applicationDTO =
                restAPIStore.createApplication(apiKeyApplicationName, "API Key Application",
                        APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN,
                        ApplicationDTO.TokenTypeEnum.JWT);
        apiKeyApplicationId = applicationDTO.getData();
        //create App for Backend JWT test with Auth Code grant
        applicationDTO = restAPIStore.createApplication(authCodeApplicationName, "Auth Code Application",
                APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN,
                ApplicationDTO.TokenTypeEnum.JWT);
        authCodeApplicationId = applicationDTO.getData();

        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(endpointURL));
        apiRequest.setVersion(apiVersion);
        apiRequest.setVisibility("public");
        apiRequest.setProvider(providerName);

        List<String> securitySchemes = new ArrayList<>();
        securitySchemes.add("oauth2");
        securitySchemes.add("api_key");
        apiRequest.setSecurityScheme(securitySchemes);

        apiId = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);
        restAPIStore.subscribeToAPI(apiId, oauthApplicationId, TIER_GOLD);
        restAPIStore.subscribeToAPI(apiId, jwtApplicationId, TIER_GOLD);
        restAPIStore.subscribeToAPI(apiId, apiKeyApplicationId, TIER_GOLD);
        restAPIStore.subscribeToAPI(apiId, authCodeApplicationId, TIER_GOLD);

        // Create, publish and subscribe API with "api_key" security only
        APIRequest api2Request = new APIRequest(api2Name, api2Context, new URL(endpointURL));
        api2Request.setVersion(apiVersion);
        api2Request.setVisibility("public");
        api2Request.setProvider(providerName);

        List<String> api2SecuritySchemes = new ArrayList<>();
        api2SecuritySchemes.add("api_key");
        api2Request.setSecurityScheme(api2SecuritySchemes);

        api2Id = createAndPublishAPIUsingRest(api2Request, restAPIPublisher, false);
        restAPIStore.subscribeToAPI(api2Id, apiKeyApplicationId, TIER_GOLD);

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.AUTHORIZATION_CODE);
        //generate keys
        restAPIStore.generateKeys(oauthApplicationId, "36000", CALLBACK_URL,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        restAPIStore.generateKeys(jwtApplicationId, "36000", CALLBACK_URL,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        restAPIStore.generateAPIKeys(apiKeyApplicationId,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.toString(), 36000, null, null);
        restAPIStore.generateKeys(authCodeApplicationId, "36000", CALLBACK_URL,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        createUser();
        createClaimMapping();
        waitForAPIDeploymentSync(user.getUserName(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        waitForAPIDeploymentSync(user.getUserName(), api2Request.getName(), api2Request.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);

        // Invoke JWKS endpoint and retrieve kid claim to validate backend JWT
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet jwksGet = new HttpGet(getAPIInvocationURLHttp("jwks"));
        HttpResponse jwksResponse = httpclient.execute(jwksGet);
        assertEquals(jwksResponse.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation fails for JWKS GET request");
        String jwksResponseString = EntityUtils.toString(jwksResponse.getEntity(), "UTF-8");
        JSONObject jwksResponseObject = new JSONObject(jwksResponseString);
        jwksKidClaim = jwksResponseObject.getJSONArray("keys").getJSONObject(0).getString("kid");
    }

    @Test(groups = {"wso2.am"}, description = "Backend JWT Token Generation for Oauth Based App")
    public void testEnableJWTAndClaimsForOauthApp() throws Exception {

        ApiResponse<ApplicationKeyDTO> applicationKeysByKeyType =
                restAPIStore.getApplicationKeysByKeyType(oauthApplicationId,
                        ApplicationKeyDTO.KeyTypeEnum.PRODUCTION.getValue());
        ApplicationKeyDTO applicationKeyDTO = applicationKeysByKeyType.getData();
        for (String endUser : users) {
            String accessToken = generateUserToken(applicationKeyDTO.getConsumerKey(),
                    applicationKeyDTO.getConsumerSecret(), endUser, enduserPassword, user, new String[]{"default"});
            log.info("Access Token Generated in oauth ==" + accessToken);
            String tokenJti = TokenUtils.getJtiOfJwtToken(accessToken);

            HttpClient httpclient = HttpClientBuilder.create().build();
            HttpGet get = new HttpGet(getAPIInvocationURLHttp(apiContext, apiVersion));
            get.addHeader("Authorization", "Bearer " + tokenJti);
            HttpResponse response = httpclient.execute(get);
            Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched when api invocation");

            Header[] responseHeaders = response.getAllHeaders();
            Header jwtheader = pickHeader(responseHeaders, JWT_ASSERTION_HEADER);
            Assert.assertNotNull(jwtheader, JWT_ASSERTION_HEADER + " is not available in the backend request.");

            //check the jwt header
            String decodedJWTHeaderString = APIMTestCaseUtils.getDecodedJWTHeader(jwtheader.getValue());
            Assert.assertNotNull(jwtheader, JWT_ASSERTION_HEADER + " is not available in the backend request.");
            String decodedJWTString = APIMTestCaseUtils.getDecodedJWT(jwtheader.getValue());
            log.debug("Decoded JWTString = " + decodedJWTString);
            //Do the signature verification for super tenant as tenant key store not there accessible
            BackendJWTUtil.verifySignature(jwtheader);
            log.debug("Decoded JWT header String = " + decodedJWTHeaderString);
            BackendJWTUtil.verifyJWTHeader(decodedJWTHeaderString, jwksKidClaim);
            JSONObject jsonObject = new JSONObject(decodedJWTString);
            log.info("JWT Received ==" + jsonObject.toString());
            //Validate expiry time
            Long expiry = jsonObject.getLong("exp");
            Long currentTime = System.currentTimeMillis() / 1000;
            Assert.assertTrue(currentTime <= expiry, "Token expired");
            // check default claims
            checkDefaultUserClaims(jsonObject, oauthApplicationName);
            // check user profile info claims
            verifyUserProfileInfoClaims(jsonObject, endUser);
            // check wrong claims
            BackendJWTUtil.verifyWrongClaims(jsonObject);

            // http://wso2.org/claims/applicationAttributes should contain 'Optional attribute' as
            // enable_empty_values_in_application_attributes is true and therefore empty values are allowed for custom
            // application attributes
            assertTrue(jsonObject.getString("http://wso2.org/claims/applicationAttributes").
                    equals("{\"Required attribute 2\":\"Default value of Required attribute 2\",\"Required attribute 1\"" +
                            ":\"Default value of Required attribute 1\",\"Optional attribute\":\"\"}"));
        }
    }

    @Test(groups = {"wso2.am"}, description = "Backend JWT Token Generation for JWT Based App")
    public void testEnableJWTAndClaimsForJWTApp() throws Exception {

        ApiResponse<ApplicationKeyDTO> applicationKeysByKeyType =
                restAPIStore.getApplicationKeysByKeyType(jwtApplicationId,
                        ApplicationKeyDTO.KeyTypeEnum.PRODUCTION.getValue());
        ApplicationKeyDTO applicationKeyDTO = applicationKeysByKeyType.getData();
        updateServiceProviderWithRequiredClaims(applicationKeyDTO.getConsumerKey());
        for (String endUser : users) {
            String accessToken = generateUserToken(applicationKeyDTO.getConsumerKey(),
                    applicationKeyDTO.getConsumerSecret(), endUser, enduserPassword, user, new String[]{"openid"});
            log.info("Access Token Generated in JWT ==" + accessToken);
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
            Assert.assertNotNull(jwtheader, JWT_ASSERTION_HEADER + " is not available in the backend request.");
            String decodedJWTString = APIMTestCaseUtils.getDecodedJWT(jwtheader.getValue());
            log.debug("Decoded JWTString = " + decodedJWTString);

            //Do the signature verification
            BackendJWTUtil.verifySignature(jwtheader);
            log.debug("Decoded JWT header String = " + decodedJWTHeaderString);
            BackendJWTUtil.verifyJWTHeader(decodedJWTHeaderString, jwksKidClaim);
            JSONObject jsonObject = new JSONObject(decodedJWTString);

            // check default claims
            checkDefaultUserClaims(jsonObject, jwtApplicationName);
            // check user profile info claims
            log.info("JWT Received ==" + jsonObject.toString());
            String claim = jsonObject.getString("http://wso2.org/claims/givenname");
            assertTrue("JWT claim givenname  not received" + claim, claim.contains("first name".concat(endUser)));
            claim = jsonObject.getString("http://wso2.org/claims/lastname");
            assertTrue("JWT claim lastname  not received" + claim, claim.contains("last name".concat(endUser)));
            claim = jsonObject.getString("mobile");
            assertTrue("JWT claim mobile  not received" + claim, claim.contains("94123456987"));
            claim = jsonObject.getString("organization");
            assertTrue("JWT claim mobile  not received" + claim, claim.contains("ABC".concat(endUser)));
            // verify wrong claims
            BackendJWTUtil.verifyWrongClaims(jsonObject);

            // http://wso2.org/claims/applicationAttributes should contain 'Optional attribute' as
            // enable_empty_values_in_application_attributes is true and therefore empty values are allowed for custom
            // application attributes
            assertTrue(jsonObject.getString("http://wso2.org/claims/applicationAttributes").
                    equals("{\"Required attribute 2\":\"Default value of Required attribute 2\",\"Required attribute 1\"" +
                            ":\"Default value of Required attribute 1\",\"Optional attribute\":\"\"}"));
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test invoking API that is secured only with 'API key' when back end " +
            "JWT generation is enabled")
    public void testAPIKeyOnlySecuredAPIInvocation() throws Exception {
        APIKeyDTO apiKeyDTO = restAPIStore.generateAPIKeys(apiKeyApplicationId,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.toString(), 36000, null, null);

        assertNotNull(apiKeyDTO, "API Key generation failed");
        log.info("Access Token Generated in JWT ==" + apiKeyDTO.getApikey());
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttp(api2Context, apiVersion));
        get.addHeader("apikey", apiKeyDTO.getApikey());
        HttpResponse response = httpclient.execute(get);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");

        //check JWT headers
        Header[] responseHeaders = response.getAllHeaders();
        Header jwtheader = pickHeader(responseHeaders, JWT_ASSERTION_HEADER);
        Assert.assertNotNull(jwtheader, JWT_ASSERTION_HEADER + " is not available in the backend request.");
    }

    @Test(groups = {"wso2.am"}, description = "Backend JWT Token Generation for API Key Based App")
    public void testEnableJWTAndClaimsForAPIKeyApp() throws Exception {
        APIKeyDTO apiKeyDTO = restAPIStore.generateAPIKeys(apiKeyApplicationId,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.toString(), 36000, null, null);

        assertNotNull(apiKeyDTO, "API Key generation failed");

        log.info("Access Token Generated in JWT ==" + apiKeyDTO.getApikey());
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttp(apiContext, apiVersion));
        get.addHeader("apikey", apiKeyDTO.getApikey());
        HttpResponse response = httpclient.execute(get);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");

        //check JWT headers
        Header[] responseHeaders = response.getAllHeaders();
        Header jwtheader = pickHeader(responseHeaders, JWT_ASSERTION_HEADER);
        Assert.assertNotNull(jwtheader, JWT_ASSERTION_HEADER + " is not available in the backend request.");

        String decodedJWTHeaderString = APIMTestCaseUtils.getDecodedJWTHeader(jwtheader.getValue());
        String decodedJWTString = APIMTestCaseUtils.getDecodedJWT(jwtheader.getValue());
        log.debug("Decoded JWTString = " + decodedJWTString);

        //Do the signature verification
        BackendJWTUtil.verifySignature(jwtheader);
        log.debug("Decoded JWT header String = " + decodedJWTHeaderString);
        BackendJWTUtil.verifyJWTHeader(decodedJWTHeaderString, jwksKidClaim);
        JSONObject jsonObject = new JSONObject(decodedJWTString);

        // check default claims
        checkDefaultUserClaims(jsonObject, apiKeyApplicationName);
        // check API details
        log.info("JWT Received ==" + jsonObject.toString());
        String claim = jsonObject.getString("http://wso2.org/claims/apiname");
        assertTrue("JWT claim API name not received " + claim, claim.contains(apiName));
        claim = jsonObject.getString("http://wso2.org/claims/version");
        assertTrue("JWT claim API version not received " + claim, claim.contains(apiVersion));
        claim = jsonObject.getString("http://wso2.org/claims/apicontext");
        assertTrue("JWT claim API context not received " + claim, claim.contains(apiContext));
        // verify wrong claims
        BackendJWTUtil.verifyWrongClaims(jsonObject);
    }

    @Test(groups = {"wso2.am"}, description = "Backend JWT Token Generation with Client Credentials Grant Type")
    public void testBackendJWTWithClientCredentialsGrant() throws Exception {
        ApiResponse<ApplicationKeyDTO> applicationKeysByKeyType = restAPIStore
                .getApplicationKeysByKeyType(jwtApplicationId, ApplicationKeyDTO.KeyTypeEnum.PRODUCTION.getValue());
        ApplicationKeyDTO applicationKeyDTO = applicationKeysByKeyType.getData();
        String accessToken = generateTokenWithClientCredentialsGrant(applicationKeyDTO.getConsumerKey(),
                applicationKeyDTO.getConsumerSecret(), new String[] { "default" });
        log.info("Access Token Generated in JWT ==" + accessToken);
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
        Assert.assertNotNull(jwtheader, JWT_ASSERTION_HEADER + " is not available in the backend request.");
        String decodedJWTString = APIMTestCaseUtils.getDecodedJWT(jwtheader.getValue());
        log.debug("Decoded JWTString = " + decodedJWTString);

        //Do the signature verification
        BackendJWTUtil.verifySignature(jwtheader);
        log.debug("Decoded JWT header String = " + decodedJWTHeaderString);
        BackendJWTUtil.verifyJWTHeader(decodedJWTHeaderString, jwksKidClaim);
        JSONObject jsonObject = new JSONObject(decodedJWTString);

        // check default claims
        checkDefaultUserClaims(jsonObject, jwtApplicationName);

        //check activityid header
        Header activityId_request_path = pickHeader(responseHeaders, "in_activityid");
        Header activityId_response_path = pickHeader(responseHeaders, "activityid");

        Assert.assertTrue(activityId_request_path.getValue().equals(activityId_response_path.getValue()),
                "activityid in request path ( " + activityId_request_path +
                ") does not match with the response path ( " + activityId_response_path + " ).");

    }

    @Test(groups = { "wso2.am" }, description = "Backend JWT Token Generation with Auth Code Grant Type")
    public void testBackendJWTWithAuthCodeGrant() throws Exception {

        ApiResponse<ApplicationKeyDTO> applicationKeysByKeyType = restAPIStore
                .getApplicationKeysByKeyType(authCodeApplicationId,
                        ApplicationKeyDTO.KeyTypeEnum.PRODUCTION.getValue());
        ApplicationKeyDTO applicationKeyDTO = applicationKeysByKeyType.getData();
        for (String endUser : users) {
            String accessToken = generateTokenWithAuthCodeGrant(applicationKeyDTO.getConsumerKey(),
                    applicationKeyDTO.getConsumerSecret(), endUser, enduserPassword, user, new String[] { "default" });
            log.info("Access Token Generated in JWT ==" + accessToken);
            String tokenJti = TokenUtils.getJtiOfJwtToken(accessToken);
            HttpClient httpclient = HttpClientBuilder.create().build();
            HttpGet get = new HttpGet(getAPIInvocationURLHttp(apiContext, apiVersion));
            get.addHeader("Authorization", "Bearer " + tokenJti);
            HttpResponse response = httpclient.execute(get);
            Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched when api invocation");

            Header[] responseHeaders = response.getAllHeaders();
            Header jwtheader = pickHeader(responseHeaders, JWT_ASSERTION_HEADER);
            Assert.assertNotNull(jwtheader, JWT_ASSERTION_HEADER + " is not available in the backend request.");

            //check the jwt header
            String decodedJWTHeaderString = APIMTestCaseUtils.getDecodedJWTHeader(jwtheader.getValue());
            Assert.assertNotNull(jwtheader, JWT_ASSERTION_HEADER + " is not available in the backend request.");
            String decodedJWTString = APIMTestCaseUtils.getDecodedJWT(jwtheader.getValue());
            log.debug("Decoded JWTString = " + decodedJWTString);

            //Do the signature verification
            BackendJWTUtil.verifySignature(jwtheader);
            log.debug("Decoded JWT header String = " + decodedJWTHeaderString);
            BackendJWTUtil.verifyJWTHeader(decodedJWTHeaderString, jwksKidClaim);
            JSONObject jsonObject = new JSONObject(decodedJWTString);

            // check default claims
            checkDefaultUserClaims(jsonObject, authCodeApplicationName);
            // check user profile info claims
            log.info("JWT Received ==" + jsonObject.toString());
            verifyUserProfileInfoClaims(jsonObject, endUser);
            // verify wrong claims
            BackendJWTUtil.verifyWrongClaims(jsonObject);
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        for (String user : users) {
            userManagementClient.deleteUser(user);
        }
        removeClaimMapping();
        restAPIStore.deleteApplication(oauthApplicationId);
        restAPIStore.deleteApplication(jwtApplicationId);
        restAPIStore.deleteApplication(apiKeyApplicationId);
        restAPIStore.deleteApplication(authCodeApplicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(api2Id, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        restAPIPublisher.deleteAPI(api2Id);

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

    private void createUser() throws RemoteException,
            RemoteUserStoreManagerServiceUserStoreExceptionException, UserStoreException {

        for (String user : users) {
            remoteUserStoreManagerServiceClient.addUser(user, enduserPassword, new String[]{}, new ClaimValue[]{},
                    DEFAULT_PROFILE, false);
            remoteUserStoreManagerServiceClient.setUserClaimValue(user,
                    "http://wso2.org/claims/givenname", "first name".concat(user), DEFAULT_PROFILE);
            remoteUserStoreManagerServiceClient.setUserClaimValue(user,
                    "http://wso2.org/claims/lastname", "last name".concat(user), DEFAULT_PROFILE);
            remoteUserStoreManagerServiceClient.setUserClaimValue(user,
                    "http://wso2.org/claims/organization", "ABC".concat(user), DEFAULT_PROFILE);
            remoteUserStoreManagerServiceClient.setUserClaimValue(user,
                    "http://wso2.org/claims/mobile", "94123456987", DEFAULT_PROFILE);
        }

    }

    private void createClaimMapping() throws RemoteException, ClaimMetadataManagementServiceClaimMetadataException,
            OAuthAdminServiceIdentityOAuthAdminException {

        remoteClaimMetaDataMgtAdminClient.addExternalClaim("http://wso2.org/oidc/claim", "mobile",
                "http://wso2.org/claims/mobile");
        remoteClaimMetaDataMgtAdminClient.addExternalClaim("http://wso2.org/oidc/claim", "organization",
                "http://wso2.org/claims/organization");
        oAuthAdminServiceClient.updateScope("openid",
                new String[] { "given_name", "family_name", "mobile", "organization" }, new String[0]);
    }

    private void updateServiceProviderWithRequiredClaims(String consumerKey)
            throws OAuthAdminServiceIdentityOAuthAdminException, RemoteException,
            IdentityApplicationManagementServiceIdentityApplicationManagementException {
        String[] requestedClaims = {"http://wso2.org/claims/givenname","http://wso2.org/claims/lastname","http://wso2" +
                ".org/claims/organization","http://wso2.org/claims/mobile"};
        OAuthConsumerAppDTO oAuthApplicationData = oAuthAdminServiceClient.getOAuthApplicationData(consumerKey);
        String applicationName = oAuthApplicationData.getApplicationName();
        ServiceProvider application = applicationManagementClient.getApplication(applicationName);
        ClaimConfig claimConfig = new ClaimConfig();
        for (String claimUri : requestedClaims){
            ClaimMapping claimMapping = new ClaimMapping();
            Claim claim = new Claim();
            claim.setClaimUri(claimUri);
            claimMapping.setLocalClaim(claim);
            claimMapping.setRemoteClaim(claim);
            claimMapping.setRequested(true);
            claimMapping.setMandatory(true);
            claimConfig.addClaimMappings(claimMapping);
        }
        application.setClaimConfig(claimConfig);
        applicationManagementClient.updateApplication(application);
    }

    private void removeClaimMapping() throws RemoteException, ClaimMetadataManagementServiceClaimMetadataException,
            OAuthAdminServiceIdentityOAuthAdminException {

        oAuthAdminServiceClient.updateScope("openid", new String[0],
                new String[] { "given_name", "family_name", "mobile", "organization" });
        remoteClaimMetaDataMgtAdminClient.removeExternalClaim("http://wso2.org/oidc/claim",
                "http://wso2.org/oidc/claim/mobile");
        remoteClaimMetaDataMgtAdminClient.removeExternalClaim("http://wso2.org/oidc/claim",
                "http://wso2.org/oidc/claim/organization");
    }

    private String generateUserToken(String consumerKey, String consumerSecret, String enduserName,
                                     String enduserPassword, User user, String[] scopes)
            throws APIManagerIntegrationTestException,
            JSONException {

        String username = enduserName.concat("@").concat(user.getUserDomain());
        String requestBody = "grant_type=password&username=" + username + "&password=" + enduserPassword + "&scope=" +
                Strings.join(" ", scopes);

        org.wso2.carbon.automation.test.utils.http.client.HttpResponse httpResponse =
                restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL);
        JSONObject accessTokenGenerationResponse = new JSONObject(httpResponse.getData());
        return accessTokenGenerationResponse.getString("access_token");

    }

    private String generateTokenWithClientCredentialsGrant(String consumerKey, String consumerSecret, String[] scopes)
            throws APIManagerIntegrationTestException, JSONException {

        String requestBody = "grant_type=client_credentials" + "&scope=" + Strings.join(" ", scopes);
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse httpResponse = restAPIStore
                .generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL);
        JSONObject accessTokenGenerationResponse = new JSONObject(httpResponse.getData());
        return accessTokenGenerationResponse.getString("access_token");
    }

    private String generateTokenWithAuthCodeGrant(String consumerKey, String consumerSecret, String enduserName,
            String enduserPassword, User user, String[] scopes) throws JSONException, IOException {

        String username = enduserName.concat("@").concat(user.getUserDomain());
        Map<String, String> headers = new HashMap<>();
        List<NameValuePair> urlParameters = new ArrayList<>();
        //Sending first request to approve grant authorization to app
        String APPLICATION_CONTENT_TYPE = "application/x-www-form-urlencoded";
        headers.put("Content-Type", APPLICATION_CONTENT_TYPE);
        String url = identityLoginURL + "?response_type=code&" + "client_id=" + consumerKey + "&scope=" + Strings
                .join(" ", scopes) + "&redirect_uri=" + CALLBACK_URL;
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse res = HTTPSClientUtils.doGet(url, headers);
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_MOVED_TEMPORARILY, "Response code is not as expected");
        String LOCATION_HEADER = "Location";
        String locationHeader = res.getHeaders().get(LOCATION_HEADER);
        Assert.assertNotNull(locationHeader, "Couldn't found Location Header");
        String SET_COOKIE_HEADER = "Set-Cookie";
        String sessionNonceCookie = res.getHeaders().get(SET_COOKIE_HEADER);
        Assert.assertNotNull(sessionNonceCookie, "Couldn't find the sessionNonceCookie Header");
        String sessionDataKey = getURLParameter(locationHeader, "sessionDataKey");
        Assert.assertNotNull(sessionDataKey, "Couldn't found sessionDataKey from the Location Header");

        //Login to the Identity with user/pass
        headers.clear();
        headers.put("Content-Type", APPLICATION_CONTENT_TYPE);
        headers.put("Cookie", sessionNonceCookie);
        urlParameters.add(new BasicNameValuePair("username", username));
        urlParameters.add(new BasicNameValuePair("password", enduserPassword));
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
        String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";
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
        return accessToken;
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

    /**
     * verify user profile info claims from decoded JWT JSON Object
     *
     * @param decodedJWTJSONObject decoded JWT JSON Object
     * @param endUser username of end user
     * @throws JSONException if JSON payload is malformed
     */
    private void verifyUserProfileInfoClaims(JSONObject decodedJWTJSONObject, String endUser) throws JSONException {

        String claim = decodedJWTJSONObject.getString("http://wso2.org/claims/givenname");
        assertTrue("JWT claim givenname  not received" + claim, claim.contains("first name".concat(endUser)));
        claim = decodedJWTJSONObject.getString("http://wso2.org/claims/lastname");
        assertTrue("JWT claim lastname  not received" + claim, claim.contains("last name".concat(endUser)));
        claim = decodedJWTJSONObject.getString("http://wso2.org/claims/mobile");
        assertTrue("JWT claim mobile  not received" + claim, claim.contains("94123456987"));
        claim = decodedJWTJSONObject.getString("http://wso2.org/claims/organization");
        assertTrue("JWT claim mobile  not received" + claim, claim.contains("ABC".concat(endUser)));
    }
}
