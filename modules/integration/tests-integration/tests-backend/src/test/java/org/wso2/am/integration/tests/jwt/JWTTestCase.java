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

import javax.ws.rs.core.Response;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

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

    private String endpointURL;
    String users[] = {"subscriberUser2", "subscriberUser2@wso2.com", "subscriberUser2@abc.com"};
    String enduserPassword = "password@123";
    private String oauthApplicationId;
    private String jwtApplicationId;
    private String apiKeyApplicationId;
    private String apiId;
    URL tokenEndpointURL;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        tokenEndpointURL = new URL(gatewayUrlsWrk.getWebAppURLNhttp() + "token");
        providerName = user.getUserName();
        endpointURL = getSuperTenantAPIInvocationURLHttp("jwt_backend", "1.0");
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
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        //generate keys
        restAPIStore.generateKeys(oauthApplicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        restAPIStore.generateKeys(jwtApplicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        restAPIStore.generateAPIKeys(apiKeyApplicationId,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION.toString(), 36000, null, null);
        createUser();
        createClaimMapping();
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
            String jwtHeader = APIMTestCaseUtils.getDecodedJWTHeader(jwtheader.getValue());
            byte[] jwtSignature = APIMTestCaseUtils.getDecodedJWTSignature(jwtheader.getValue());
            String jwtAssertion = APIMTestCaseUtils.getJWTAssertion(jwtheader.getValue());
            boolean isSignatureValid = APIMTestCaseUtils.isJwtSignatureValid(jwtAssertion, jwtSignature, jwtHeader);
            assertTrue("JWT signature verification failed", isSignatureValid);
            log.debug("Decoded JWT header String = " + decodedJWTHeaderString);
            JSONObject jsonHeaderObject = new JSONObject(decodedJWTHeaderString);
            Assert.assertEquals(jsonHeaderObject.getString("typ"), "JWT");
            Assert.assertEquals(jsonHeaderObject.getString("alg"), "RS256");
            JSONObject jsonObject = new JSONObject(decodedJWTString);
            log.info("JWT Received ==" + jsonObject.toString());
            // check default claims
            checkDefaultUserClaims(jsonObject, oauthApplicationName);
            // check user profile info claims
            String claim = jsonObject.getString("http://wso2.org/claims/givenname");
            assertTrue("JWT claim givenname  not received" + claim, claim.contains("first name".concat(endUser)));
            claim = jsonObject.getString("http://wso2.org/claims/lastname");
            assertTrue("JWT claim lastname  not received" + claim, claim.contains("last name".concat(endUser)));
            claim = jsonObject.getString("http://wso2.org/claims/mobile");
            assertTrue("JWT claim mobile  not received" + claim, claim.contains("94123456987"));
            claim = jsonObject.getString("http://wso2.org/claims/organization");
            assertTrue("JWT claim mobile  not received" + claim, claim.contains("ABC".concat(endUser)));

            boolean bExceptionOccured = false;
            try {
                jsonObject.getString("http://wso2.org/claims/wrongclaim");
            } catch (JSONException e) {
                bExceptionOccured = true;
            }
            assertTrue("JWT claim received is invalid", bExceptionOccured);
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

            String jwtHeader = APIMTestCaseUtils.getDecodedJWTHeader(jwtheader.getValue());
            byte[] jwtSignature = APIMTestCaseUtils.getDecodedJWTSignature(jwtheader.getValue());
            String jwtAssertion = APIMTestCaseUtils.getJWTAssertion(jwtheader.getValue());
            boolean isSignatureValid = APIMTestCaseUtils.isJwtSignatureValid(jwtAssertion, jwtSignature, jwtHeader);
            assertTrue("JWT signature verification failed", isSignatureValid);
            log.debug("Decoded JWT header String = " + decodedJWTHeaderString);
            JSONObject jsonHeaderObject = new JSONObject(decodedJWTHeaderString);
            Assert.assertEquals(jsonHeaderObject.getString("typ"), "JWT");
            Assert.assertEquals(jsonHeaderObject.getString("alg"), "RS256");
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

            boolean bExceptionOccured = false;
            try {
                jsonObject.getString("http://wso2.org/claims/wrongclaim");
            } catch (JSONException e) {
                bExceptionOccured = true;
            }
            assertTrue("JWT claim received is invalid", bExceptionOccured);

        }
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

        String jwtHeader = APIMTestCaseUtils.getDecodedJWTHeader(jwtheader.getValue());
        byte[] jwtSignature = APIMTestCaseUtils.getDecodedJWTSignature(jwtheader.getValue());
        String jwtAssertion = APIMTestCaseUtils.getJWTAssertion(jwtheader.getValue());
        boolean isSignatureValid = APIMTestCaseUtils.isJwtSignatureValid(jwtAssertion, jwtSignature, jwtHeader);
        assertTrue("JWT signature verification failed", isSignatureValid);
        log.debug("Decoded JWT header String = " + decodedJWTHeaderString);
        JSONObject jsonHeaderObject = new JSONObject(decodedJWTHeaderString);
        Assert.assertEquals(jsonHeaderObject.getString("typ"), "JWT");
        Assert.assertEquals(jsonHeaderObject.getString("alg"), "RS256");
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

        boolean bExceptionOccured = false;
        try {
            jsonObject.getString("http://wso2.org/claims/wrongclaim");
        } catch (JSONException e) {
            bExceptionOccured = true;
        }
        assertTrue("JWT claim received is invalid", bExceptionOccured);
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
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        super.cleanUp();

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

        remoteClaimMetaDataMgtAdminClient
                .addExternalClaim("http://wso2.org/oidc/claim", "mobile", "http://wso2.org/claims/mobile");
        remoteClaimMetaDataMgtAdminClient
                .addExternalClaim("http://wso2.org/oidc/claim", "organization", "http://wso2.org/claims/organization");
        oAuthAdminServiceClient.updateScope("openid", new String[]{"mobile", "organization"}, new String[0]);
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

        oAuthAdminServiceClient.updateScope("openid", new String[0], new String[]{"mobile", "organization"});
        remoteClaimMetaDataMgtAdminClient
                .removeExternalClaim("http://wso2.org/oidc/claim", "http://wso2.org/oidc/claim/mobile");
        remoteClaimMetaDataMgtAdminClient
                .removeExternalClaim("http://wso2.org/oidc/claim", "http://wso2.org/oidc/claim/organization");
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
}
