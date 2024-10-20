/*
 *Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.nimbusds.jose.JOSEException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
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
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.token.TokenUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.tests.jwt.idp.JWTGeneratorUtil;
import org.wso2.andes.util.Strings;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.identity.application.common.model.idp.xsd.Claim;
import org.wso2.carbon.identity.application.common.model.idp.xsd.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.idp.xsd.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.idp.xsd.Property;
import org.wso2.carbon.identity.application.common.model.xsd.AuthenticationStep;
import org.wso2.carbon.identity.application.common.model.xsd.ClaimConfig;
import org.wso2.carbon.identity.application.common.model.xsd.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.xsd.ServiceProvider;
import org.wso2.carbon.identity.application.common.util.IdentityApplicationConstants;
import org.wso2.carbon.identity.application.mgt.stub.IdentityApplicationManagementServiceIdentityApplicationManagementException;
import org.wso2.carbon.identity.claim.metadata.mgt.stub.ClaimMetadataManagementServiceClaimMetadataException;
import org.wso2.carbon.identity.oauth.stub.OAuthAdminServiceIdentityOAuthAdminException;
import org.wso2.carbon.identity.oauth.stub.dto.OAuthConsumerAppDTO;
import org.wso2.carbon.idp.mgt.stub.IdentityProviderMgtServiceIdentityProviderManagementExceptionException;
import org.wso2.carbon.um.ws.api.stub.ClaimValue;
import org.wso2.carbon.um.ws.api.stub.RemoteUserStoreManagerServiceUserStoreExceptionException;
import org.wso2.carbon.user.core.UserStoreException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.ws.rs.core.Response;

import static org.testng.AssertJUnit.assertTrue;

public class FederatedUserJWTTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(FederatedUserJWTTestCase.class);

    private final String JWT_ASSERTION_HEADER = "X-JWT-Assertion";
    private final String DEFAULT_PROFILE = "default";
    String users[] = {"subscriberUser2", "subscriberUser2@wso2.com", "subscriberUser2@abc.com"};
    String enduserPassword = "password@123";
    URL tokenEndpointURL;
    WireMockServer wireMockServer;
    private String apiName = "JWTUserClaimAPI";
    private String apiContext = "jwtTest";
    private String providerName;
    private String apiVersion = "1.0.0";
    private String oauthApplicationName = "FederatedOauthAppForJWTTest";
    private String jwtApplicationName = "FederatedJWTAppForJWTTest";
    private String endpointURL;
    private String oauthApplicationId;
    private String jwtApplicationId;
    private String apiId;
    private int idpPort;
    private String callbackUrl = "https://localhost:9943/services/Version";
    private String authorizeURL;

    @Factory(dataProvider = "userModeDataProvider")
    public FederatedUserJWTTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN}
        };
    }

    private static CloseableHttpResponse executeRequest(HttpUriRequest request, HttpClientContext context) throws IOException {

        try (CloseableHttpClient closeableHttpClient =
                     HttpClientBuilder.create().disableAutomaticRetries().disableRedirectHandling().build()) {
            return closeableHttpClient.execute(request, context);
        }
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        authorizeURL = getKeyManagerURLHttps() + "/oauth2/authorize";
        idpPort = getAvailablePort(9950, 9999);
        if (idpPort == -1) {
            throw new APIManagerIntegrationTestException("No available port in the range " +
                    9950 + "-" + 9999 + " was found");
        }
        log.info("Selected port " + idpPort + " to start backend server");
        startIdp(idpPort);
        tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
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
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.AUTHORIZATION_CODE);
        //generate keys
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(oauthApplicationId, "36000", callbackUrl,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        ApplicationKeyDTO applicationKeyDTO1 = restAPIStore.generateKeys(jwtApplicationId, "36000", callbackUrl,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        createUser();
        configureIDP();
        createClaimMapping();
        configureIDPtoFederationInServiceProvider(applicationKeyDTO.getConsumerKey());
        configureIDPtoFederationInServiceProvider(applicationKeyDTO1.getConsumerKey());
    }

    private void startIdp(int idpPort) throws JSONException, CertificateException, UnrecoverableKeyException,
            NoSuchAlgorithmException, KeyStoreException, JOSEException, IOException {

        wireMockServer =
                new WireMockServer(WireMockConfiguration.options().port(idpPort).extensions(new ResponseTemplateTransformer(false)));
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/authorize"))
                .willReturn(WireMock.aResponse().withHeader("Location", "https://localhost:9943/commonauth?code" +
                        "=" + UUID.randomUUID().toString() + "&state={{request.query.state}}")
                        .withStatus(302).withTransformers("response-template")));
        wireMockServer.stubFor(WireMock.post(WireMock.urlPathMatching("/token")).willReturn(WireMock.aResponse()
                .withStatus(200).withBody(getMockTokenResponse()).withHeader("Content-Type", "application/json")));
        wireMockServer.start();

    }

    private String getMockTokenResponse() throws JSONException, CertificateException, UnrecoverableKeyException,
            NoSuchAlgorithmException, KeyStoreException, JOSEException, IOException {

        File keyStoreFile = Paths.get(getAMResourceLocation(), "configFiles", "idpjwt", "keystore.jks").toFile();
        Map attributes = new HashMap();
        attributes.put("azp", "consumerKey1");
        attributes.put("given_name", "first");
        attributes.put("family_name ", "last");
        attributes.put("email", "first@gmail.com");
        attributes.put("phone_number", "424479772294778");
        attributes.put("organization", "abc.com");

        String generatedJWT =
                JWTGeneratorUtil
                        .generatedJWT(keyStoreFile, "idptest", "idptest", "wso2carbon", "wso2carbon", "userexternal",
                                "https://localhost.idp.com", attributes);
        JSONObject token = new JSONObject();
        token.put("access_token", UUID.randomUUID().toString());
        token.put("refresh_token", UUID.randomUUID().toString());
        token.put("scope", "openid");
        token.put("token_type", "Bearer");
        token.put("expires_in", 3600);
        token.put("id_token", generatedJWT);
        return token.toString();
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

    @Test(groups = {"wso2.am"}, description = "Backend JWT Token Generation for JWT Based App", dependsOnMethods = {
            "testEnableJWTAndClaimsForJWTApp"})
    public void testVerifyJWTClaimsInFederatedUserJWTAPP() throws Exception {

        ApiResponse<ApplicationKeyDTO> applicationKeysByKeyType =
                restAPIStore.getApplicationKeysByKeyType(jwtApplicationId,
                        ApplicationKeyDTO.KeyTypeEnum.PRODUCTION.getValue());
        ApplicationKeyDTO applicationKeyDTO = applicationKeysByKeyType.getData();
        String accessToken = generateTokenFromFederation(applicationKeyDTO);
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
        assertTrue("JWT claim givenname  not received" + claim, claim.contains("first"));
        claim = jsonObject.getString("http://wso2.org/claims/telephone");
        assertTrue("JWT claim mobile  not received" + claim, claim.contains("424479772294778"));
        claim = jsonObject.getString("organization");
        assertTrue("JWT claim mobile  not received" + claim, claim.contains("abc.com"));
        claim = jsonObject.getString("http://wso2.org/claims/emailaddress");
        assertTrue("JWT claim mobile  not received" + claim, claim.contains("first@gmail.com"));

        boolean bExceptionOccured = false;
        try {
            jsonObject.getString("http://wso2.org/claims/wrongclaim");
        } catch (JSONException e) {
            bExceptionOccured = true;
        }
        assertTrue("JWT claim received is invalid", bExceptionOccured);

    }

    @Test(groups = {"wso2.am"}, description = "Backend JWT Token Generation for JWT Based App", dependsOnMethods = {
            "testVerifyJWTClaimsInFederatedUserJWTAPP"})
    public void testVerifyJWTClaimsInFederatedUserOauthAPP() throws Exception {

        ApiResponse<ApplicationKeyDTO> applicationKeysByKeyType =
                restAPIStore.getApplicationKeysByKeyType(oauthApplicationId,
                        ApplicationKeyDTO.KeyTypeEnum.PRODUCTION.getValue());
        ApplicationKeyDTO applicationKeyDTO = applicationKeysByKeyType.getData();
        updateServiceProviderWithRequiredClaims(applicationKeyDTO.getConsumerKey());
        String accessToken = generateTokenFromFederation(applicationKeyDTO);
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
        checkDefaultUserClaims(jsonObject, oauthApplicationName);
        // check user profile info claims
        log.info("JWT Received ==" + jsonObject.toString());
        String claim = jsonObject.getString("given_name");
        assertTrue("JWT claim givenname  not received" + claim, claim.contains("first"));
        claim = jsonObject.getString("phone_number");
        assertTrue("JWT claim mobile  not received" + claim, claim.contains("424479772294778"));
        claim = jsonObject.getString("organization");
        assertTrue("JWT claim mobile  not received" + claim, claim.contains("abc.com"));
        claim = jsonObject.getString("email");
        assertTrue("JWT claim mobile  not received" + claim, claim.contains("first@gmail.com"));

        boolean bExceptionOccured = false;
        try {
            jsonObject.getString("http://wso2.org/claims/wrongclaim");
        } catch (JSONException e) {
            bExceptionOccured = true;
        }
        assertTrue("JWT claim received is invalid", bExceptionOccured);

    }

    private String generateTokenFromFederation(ApplicationKeyDTO applicationKeyDTO) throws IOException,
            APIManagerIntegrationTestException, JSONException {
        HttpClientContext context = HttpClientContext.create();
        HttpGet httpGet =
                new HttpGet(authorizeURL + "?response_type=code&state=&client_id" + "="
                        + applicationKeyDTO.getConsumerKey() + "&redirect_uri=" + callbackUrl + "&scope=openid");
        try (CloseableHttpResponse responseFromBrowserToAPIM = executeRequest(httpGet,context)) {
            Assert.assertEquals(responseFromBrowserToAPIM.getStatusLine().getStatusCode(), 302);
            Assert.assertNotNull(responseFromBrowserToAPIM.getFirstHeader("Location"));
            Header location = responseFromBrowserToAPIM.getFirstHeader("Location");
            Assert.assertTrue(location.getValue().contains("http://localhost:" + idpPort + "/authorize"));
            try (CloseableHttpResponse responseFromFedIDP = executeRequest(new HttpGet(location.getValue()), context)) {
                Assert.assertEquals(responseFromFedIDP.getStatusLine().getStatusCode(), 302);
                Assert.assertNotNull(responseFromFedIDP.getFirstHeader("Location"));
                Header locationFromFedIDP = responseFromFedIDP.getFirstHeader("Location");
                Assert.assertTrue(locationFromFedIDP.getValue().contains("https://localhost:9943/commonauth?code="));
                try (CloseableHttpResponse responseFromCommonAuth =
                             executeRequest(new HttpGet(locationFromFedIDP.getValue()), context)) {
                    Assert.assertEquals(responseFromCommonAuth.getStatusLine().getStatusCode(), 302);
                    Assert.assertNotNull(responseFromCommonAuth.getFirstHeader("Location"));
                    Header locationFromCommonAuth = responseFromCommonAuth.getFirstHeader("Location");
                    Assert.assertTrue(locationFromCommonAuth.getValue().contains("https://localhost:9943/oauth2" +
                            "/authorize?sessionDataKey="));
                    try (CloseableHttpResponse response =
                                 executeRequest(new HttpGet(locationFromCommonAuth.getValue()), context)) {
                        Assert.assertEquals(response.getStatusLine().getStatusCode(), 302);
                        Assert.assertNotNull(response.getFirstHeader("Location"));
                        Header locationHeader = response.getFirstHeader("Location");
                        Assert.assertTrue(locationHeader.getValue().contains(callbackUrl + "?code="));
                        String code = getURLParameter(locationHeader.getValue(), "code");
                        return generateAuthCodeToken(applicationKeyDTO.getConsumerKey(),
                                applicationKeyDTO.getConsumerSecret(), code);
                    }
                }
            }
        }
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

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        deleteClaimMapping();
        wireMockServer.stop();
        for (String user : users) {
            userManagementClient.deleteUser(user);
        }
        restAPIStore.deleteApplication(oauthApplicationId);
        restAPIStore.deleteApplication(jwtApplicationId);
        restAPIPublisher.deleteAPI(apiId);
        identityProviderMgtClient.deleteIdp("federated-idp");

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

    private String generateAuthCodeToken(String consumerKey, String consumerSecret, String authCode)
            throws APIManagerIntegrationTestException,
            JSONException {

        String requestBody = "grant_type=authorization_code&code=" + authCode + "&redirect_uri=" + callbackUrl;

        org.wso2.carbon.automation.test.utils.http.client.HttpResponse httpResponse =
                restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL);
        JSONObject accessTokenGenerationResponse = new JSONObject(httpResponse.getData());
        return accessTokenGenerationResponse.getString("access_token");

    }

    private void configureIDP() throws RemoteException,
            IdentityProviderMgtServiceIdentityProviderManagementExceptionException {

        IdentityProvider identityProvider = new IdentityProvider();
        identityProvider.setIdentityProviderName("federated-idp");
        identityProvider.setEnable(true);
        FederatedAuthenticatorConfig federatedAuthenticatorConfig = addFederatedAuthenticatorConfigs();
        identityProvider.setFederatedAuthenticatorConfigs(new FederatedAuthenticatorConfig[]{federatedAuthenticatorConfig});
        identityProvider.setDefaultAuthenticatorConfig(federatedAuthenticatorConfig);
        identityProviderMgtClient.addIDP(identityProvider);
    }

    private Claim getClaim(String claimUri) {

        Claim claim = new Claim();
        claim.setClaimUri(claimUri);
        return claim;
    }

    private FederatedAuthenticatorConfig addFederatedAuthenticatorConfigs() {

        FederatedAuthenticatorConfig federatedAuthenticatorConfig = new FederatedAuthenticatorConfig();
        federatedAuthenticatorConfig.setName("OpenIDConnectAuthenticator");
        federatedAuthenticatorConfig.setEnabled(true);
        federatedAuthenticatorConfig.setDisplayName("openidconnect");
        Property[] properties = new Property[9];
        Property property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.Facebook.CLIENT_ID);
        property.setValue(UUID.randomUUID().toString());
        properties[0] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.OIDC.OAUTH2_AUTHZ_URL);
        property.setValue("http://localhost:" + idpPort + "/authorize");
        properties[1] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.OIDC.OAUTH2_TOKEN_URL);
        property.setValue("http://localhost:" + idpPort + "/token");
        properties[2] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.OIDC.CLIENT_SECRET);
        property.setValue(UUID.randomUUID().toString());
        property.setConfidential(true);
        properties[3] = property;

        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.OIDC.IS_USER_ID_IN_CLAIMS);
        properties[4] = property;
        property.setValue("false");
        property = new Property();
        property.setName("commonAuthQueryParams");
        properties[5] = property;
        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.OIDC.CALLBACK_URL);
        property.setValue("https://localhost:9943:/commonauth");
        properties[6] = property;
        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.OIDC.OIDC_LOGOUT_URL);
        properties[7] = property;
        property = new Property();
        property.setName(IdentityApplicationConstants.Authenticator.OIDC.IS_BASIC_AUTH_ENABLED);
        property.setValue("true");
        properties[8] = property;

        federatedAuthenticatorConfig.setProperties(properties);
        return federatedAuthenticatorConfig;
    }

    private void configureIDPtoFederationInServiceProvider(String consumerKey) throws Exception {

        OAuthConsumerAppDTO oAuthApplicationData = oAuthAdminServiceClient.getOAuthApplicationData(consumerKey);
        String applicationName = oAuthApplicationData.getApplicationName();
        ServiceProvider application = applicationManagementClient.getApplication(applicationName);
        application.getLocalAndOutBoundAuthenticationConfig().setSkipLogoutConsent(true);
        application.getLocalAndOutBoundAuthenticationConfig().setSkipConsent(true);
        application.getLocalAndOutBoundAuthenticationConfig().setAuthenticationType("federated");
        AuthenticationStep authStep = new AuthenticationStep();
        org.wso2.carbon.identity.application.common.model.xsd.IdentityProvider idp =
                new org.wso2.carbon.identity.application.common.model.xsd.IdentityProvider();
        idp.setIdentityProviderName("federated-idp");
        authStep.setFederatedIdentityProviders(new org.wso2.carbon.identity.application.common.model.xsd.IdentityProvider[]{idp});
        application.getLocalAndOutBoundAuthenticationConfig().setAuthenticationSteps(new AuthenticationStep[]{authStep});
        application.getLocalAndOutBoundAuthenticationConfig().setAuthenticationScriptConfig(null);
        applicationManagementClient.updateApplication(application);
    }

    private void updateServiceProviderWithRequiredClaims(String consumerKey)
            throws OAuthAdminServiceIdentityOAuthAdminException, RemoteException,
            IdentityApplicationManagementServiceIdentityApplicationManagementException {
        String[] requestedClaims = { "http://wso2.org/claims/givenname", "http://wso2.org/claims/lastname",
                "http://wso2.org/claims/mobile", "http://wso2.org/claims/organization",
                "http://wso2.org/claims/telephone", "http://wso2.org/claims/emailaddress" };
        OAuthConsumerAppDTO oAuthApplicationData = oAuthAdminServiceClient.getOAuthApplicationData(consumerKey);
        String applicationName = oAuthApplicationData.getApplicationName();
        ServiceProvider application = applicationManagementClient.getApplication(applicationName);
        ClaimConfig claimConfig = new ClaimConfig();
        for (String claimUri : requestedClaims) {
            ClaimMapping claimMapping = new ClaimMapping();
            org.wso2.carbon.identity.application.common.model.xsd.Claim claim = new org.wso2.carbon.identity.application.common.model.xsd.Claim();
            claim.setClaimUri(claimUri);
            claimMapping.setLocalClaim(claim);
            claimMapping.setRemoteClaim(claim);
            claimMapping.setRequested(true);
            claimMapping.setMandatory(false);
            claimConfig.addClaimMappings(claimMapping);
        }
        application.setClaimConfig(claimConfig);
        applicationManagementClient.updateApplication(application);
    }

    private void createClaimMapping() throws ClaimMetadataManagementServiceClaimMetadataException, RemoteException,
            OAuthAdminServiceIdentityOAuthAdminException {

        remoteClaimMetaDataMgtAdminClient.addExternalClaim("http://wso2.org/oidc/claim", "mobile",
                "http://wso2.org/claims/mobile");
        remoteClaimMetaDataMgtAdminClient.addExternalClaim("http://wso2.org/oidc/claim", "organization",
                "http://wso2.org/claims/organization");
        oAuthAdminServiceClient.updateScope("openid",
                new String[] { "given_name", "family_name", "mobile", "organization", "phone_number", "email" },
                new String[0]);
    }

    private void deleteClaimMapping() throws Exception {

        oAuthAdminServiceClient.updateScope("openid", new String[0],
                new String[] { "given_name", "family_name", "mobile", "organization", "phone_number", "email" });
        remoteClaimMetaDataMgtAdminClient.removeExternalClaim("http://wso2.org/oidc/claim", "organization");
        remoteClaimMetaDataMgtAdminClient.removeExternalClaim("http://wso2.org/oidc/claim", "mobile");
    }
}
