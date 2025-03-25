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

package org.wso2.am.integration.tests.jwt.idp;

import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.api.dto.ClaimMappingEntryDTO;
import org.wso2.am.integration.clients.admin.api.dto.KeyManagerCertificatesDTO;
import org.wso2.am.integration.clients.admin.api.dto.KeyManagerDTO;
import org.wso2.am.integration.clients.admin.api.dto.TokenValidationDTO;
import org.wso2.am.integration.clients.store.api.ApiResponse;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ErrorDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.KeyManagerInfoDTO;
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.token.TokenUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.identity.application.common.model.idp.xsd.IdentityProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;

import static org.testng.AssertJUnit.assertTrue;

public class ExternalIDPJWTTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(ExternalIDPJWTTestCase.class);
    private final String jwtAudience ="https://default";
    private final String jwtIssuer = "https://test6.apim.integration";
    private final String keystoreFileValid = "extidpjwt.jks";
    private final String keystoreFileValidPass = "extidpjwt";
    private final String keystoreFileValidAlias = "extidpjwt";
    private static final String KEY_MANAGER_1 = "KeyManager-1";
    private static final String KEY_MANAGER_2 = "KeyManager-2";
    private static final String KEY_MANAGER_3 = "KeyManager-3";
    private static final String KEY_MANAGER_4 = "KeyManager-4";
    private static final String KEY_MANAGER_5 = "KeyManager-5";
    private static final String KEY_MANAGER_6 = "KeyManager-6";
    public static final String ALL_KEY_MANAGER = "all";
    private String apiName = "ExternalJWTTest";
    private String apiNameOnlyKM1 = "ExternalJWTTestOnlyKM1";
    private String apiContext = "externaljwtTest";
    private String apiContextOnlyKM1 = "externaljwtTestkm1";
    private String providerName;
    private String apiVersion = "1.0.0";
    private String jwtApplicationName = "JWTAppFOrJWTTest";
    private static final String ISSUER_1 = "https://test.apim.integration";
    private static final String ISSUER_2 = "https://test2.apim.integration";
    private static final String ISSUER_3 = "https://test3.apim.integration";
    private static final String ISSUER_4 = "https://test4.apim.integration";
    private static final String ISSUER_5 = "https://test5.apim.integration";
    private static final String ISSUER_6 = "https://test6.apim.integration";
    private String endpointURL;
    private String jwtApplicationId;
    private String apiId;
    URL tokenEndpointURL;
    String tokenEndpointURLString;
    private final String JWT_ASSERTION_HEADER = "X-JWT-Assertion";
    private String keyManager1Id;
    private String keyManager2Id;
    private String keyManager3Id;
    private String keyManager4Id;
    private String keyManager5Id;
    private String keyManager6Id;
    private String consumerKey1 = UUID.randomUUID().toString();
    private String consumerKey2 = UUID.randomUUID().toString();
    private String consumerKey3 = UUID.randomUUID().toString();
    private String apiIdOnlyKm1;
    ApplicationKeyDTO applicationKeyDTO;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        //create JWT Base App
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse applicationDTO =
                restAPIStore.createApplication(jwtApplicationName, "JWT Application",
                        APIMIntegrationConstants.APPLICATION_TIER.TEN_PER_MIN, ApplicationDTO.TokenTypeEnum.JWT);
        jwtApplicationId = applicationDTO.getData();
        keyManager1Id = createKeyManager1(restAPIAdmin);
        keyManager2Id = createKeyManager2(restAPIAdmin);
        addValidIdentityProvider();
        tokenEndpointURLString = getKeyManagerURLHttps() + "/oauth2/token";
        tokenEndpointURL = new URL(tokenEndpointURLString);
        apiId = createAPI(apiName, apiContext, Arrays.asList(ALL_KEY_MANAGER));
        apiIdOnlyKm1 = createAPI(apiNameOnlyKM1, apiContextOnlyKM1, Arrays.asList(KEY_MANAGER_1));

        restAPIStore.subscribeToAPI(apiId, jwtApplicationId, TIER_GOLD);
        restAPIStore.subscribeToAPI(apiIdOnlyKm1, jwtApplicationId, TIER_GOLD);
        waitForKeyManagerDeployment(user.getUserDomain(), KEY_MANAGER_1);
        waitForKeyManagerDeployment(user.getUserDomain(), KEY_MANAGER_2);
        restAPIStore.mapConsumerKeyWithApplication(consumerKey1, null, jwtApplicationId, KEY_MANAGER_1);
        restAPIStore.mapConsumerKeyWithApplication(consumerKey2, null, jwtApplicationId, KEY_MANAGER_2);
        waitForAPIDeploymentSync(providerName, apiName, apiVersion, APIMIntegrationConstants.IS_API_EXISTS);
        applicationKeyDTO = restAPIStore.generateKeys(jwtApplicationId,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                new ArrayList<>(), Arrays.asList("client_credentials", "urn:ietf:params:oauth:grant-type:token" +
                        "-exchange"));
    }

    private String createAPI(String apiName, String apiContext, List<String> keyManagers)
            throws XPathExpressionException, APIManagerIntegrationTestException, MalformedURLException,
            org.wso2.am.integration.clients.publisher.api.ApiException {

        providerName = user.getUserName();
        endpointURL = getSuperTenantAPIInvocationURLHttp("jwt_backend", "1.0");
        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(endpointURL));
        apiRequest.setVersion(apiVersion);
        apiRequest.setVisibility("public");
        apiRequest.setProvider(providerName);
        if (!keyManagers.contains(ALL_KEY_MANAGER)) {
            apiRequest.setKeyManagers(keyManagers);
        }
        return createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);
    }

    @Test(groups = {"wso2.am"}, description = "invoking From ExternalIDP Generated JWT")
    public void testInvokeExternalIDPGeneratedJWT() throws Exception {

        File keyStoreFile = Paths.get(getAMResourceLocation(), "configFiles", "idpjwt", "keystore.jks").toFile();
        Map attributes = new HashMap();
        attributes.put("azp", consumerKey1);
        attributes.put("http://idp.org/claims/givenname", "first");
        attributes.put("http://idp.org/claims/firstname", "last");
        attributes.put("http://idp.org/claims/email", "first@gmail.com");
        attributes.put("http://idp.org/claims/mobileno", "424479772294778");

        String generatedJWT =
                JWTGeneratorUtil
                        .generatedJWT(keyStoreFile, "idptest", "idptest", "wso2carbon", "wso2carbon", "userexternal",
                                ISSUER_1, attributes);
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttp(apiContext, apiVersion));
        log.info("External IDP JWT Generated: " + generatedJWT);
        get.addHeader("Authorization", "Bearer " + generatedJWT);
        HttpResponse response = httpclient.execute(get);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");
        Header[] responseHeaders = response.getAllHeaders();
        Header jwtheader = pickHeader(responseHeaders, JWT_ASSERTION_HEADER);
        Assert.assertNotNull(jwtheader, JWT_ASSERTION_HEADER + " is not available in the backend request.");
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
        Object givenName = jsonObject.get("http://wso2.org/claims/givenname");
        Assert.assertNotNull(givenName);
        Assert.assertEquals(givenName, "first");
        Object firstName = jsonObject.get("http://wso2.org/claims/firstname");
        Assert.assertNotNull(firstName);
        Assert.assertEquals(firstName, "last");
        Object email = jsonObject.get("http://wso2.org/claims/email");
        Assert.assertNotNull(email);
        Assert.assertEquals(email, "first@gmail.com");
        try {
            Object mobileno = jsonObject.get("http://idp.org/claims/mobileno");
            Assert.assertNull(mobileno);
        } catch (JSONException e) {
            Assert.assertTrue(true, "Claim not in jwt");
        }
    }

    @Test(groups = {"wso2.am"}, description = "invoking From ExternalIDP Generated JWT Consumer key is invalid")
    public void testInvokeExternalIDPGeneratedJWTNegative1() throws Exception {

        File keyStoreFile = Paths.get(getAMResourceLocation(), "configFiles", "idpjwt", "keystore.jks").toFile();
        Map attributes = new HashMap();
        attributes.put("azp", UUID.randomUUID().toString());
        String generatedJWT =
                JWTGeneratorUtil
                        .generatedJWT(keyStoreFile, "idptest", "idptest", "wso2carbon", "wso2carbon", "userexternal",
                                ISSUER_1, attributes);
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttp(apiContext, apiVersion));
        get.addHeader("Authorization", "Bearer " + generatedJWT);
        HttpResponse response = httpclient.execute(get);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.FORBIDDEN.getStatusCode(),
                "Response code mismatched when api invocation");
        String payload = IOUtils.toString(response.getEntity().getContent());
        Assert.assertTrue(payload.contains("900908"));
        Assert.assertTrue(
                payload.contains("User is NOT authorized to access the Resource. API Subscription validation failed."));
    }

    @Test(groups = {"wso2.am"}, description = "invoking From ExternalIDP Generated JWT Certificate is unknown")
    public void testInvokeExternalIDPGeneratedJWTNegative2() throws Exception {

        File keyStoreFile = Paths.get(getAMResourceLocation(), "configFiles", "idpjwt", "keystore2.jks").toFile();
        Map attributes = new HashMap();
        attributes.put("azp", UUID.randomUUID().toString());
        String generatedJWT =
                JWTGeneratorUtil
                        .generatedJWT(keyStoreFile, "idptest", "idptest", "wso2carbon", "wso2carbon",
                                "userexternal", ISSUER_2, attributes);
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttp(apiContext, apiVersion));
        get.addHeader("Authorization", "Bearer " + generatedJWT);
        HttpResponse response = httpclient.execute(get);
        Assert.assertEquals(response.getStatusLine().getStatusCode(),
                Response.Status.UNAUTHORIZED.getStatusCode(),
                "Response code mismatched when api invocation");
        String payload = IOUtils.toString(response.getEntity().getContent());
        Assert.assertTrue(payload.contains("900901"));
        Assert.assertTrue(payload.contains("Invalid Credentials"));
    }

    @Test(groups = {"wso2.am"}, description = "invoking From ExternalIDP Generated JWT")
    public void testInvokeExternalIDPGeneratedJWT1() throws Exception {

        File keyStoreFile = Paths.get(getAMResourceLocation(), "configFiles", "idpjwt", "idp1.jks").toFile();
        Map attributes = new HashMap();
        attributes.put("azp", consumerKey2);
        attributes.put("http://idp2.org/claims/givenname", "first");
        attributes.put("http://idp2.org/claims/firstname", "last");
        attributes.put("http://idp2.org/claims/email", "first@gmail.com");
        attributes.put("http://idp2.org/claims/mobileno", "424479772294778");

        String generatedJWT =
                JWTGeneratorUtil
                        .generatedJWT(keyStoreFile, "idp2certificate", "idp1", "wso2carbon", "wso2carbon",
                                "userexternal", ISSUER_2, attributes);
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttp(apiContext, apiVersion));
        log.info("External IDP JWT Generated: " + generatedJWT);
        get.addHeader("Authorization", "Bearer " + generatedJWT);
        HttpResponse response = httpclient.execute(get);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");
        Header[] responseHeaders = response.getAllHeaders();
        Header jwtheader = pickHeader(responseHeaders, JWT_ASSERTION_HEADER);
        Assert.assertNotNull(jwtheader, JWT_ASSERTION_HEADER + " is not available in the backend request.");
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
        Object givenName = jsonObject.get("http://wso2.org/claims/givenname");
        Assert.assertNotNull(givenName);
        Assert.assertEquals(givenName, "first");
        Object firstName = jsonObject.get("http://wso2.org/claims/firstname");
        Assert.assertNotNull(firstName);
        Assert.assertEquals(firstName, "last");
        Object email = jsonObject.get("http://wso2.org/claims/email");
        Assert.assertNotNull(email);
        Assert.assertEquals(email, "first@gmail.com");
        try {
            Object mobileno = jsonObject.get("http://idp.org/claims/mobileno");
            Assert.assertNull(mobileno);
        } catch (JSONException e) {
            Assert.assertTrue(true, "Claim not in jwt");
        }
    }

    @Test(groups = {"wso2.am"}, description = "validating display token endpoint behavior")
    public void testIDPDisplaytokenEndpoints() throws Exception {

        String token_ep = "http://localhost:9443/oauth/token";
        String revoke_ep = "http://localhost:9443/oauth/revoke";
        String display_token_ep = "http://localhost:9443/display/oauth/token";
        String display_revoke_ep = "http://localhost:9443/display/oauth/revoke";

        // when display token endpoints available
        KeyManagerDTO keyManagerDTO = new KeyManagerDTO();
        keyManagerDTO.setType("custom");
        keyManagerDTO.setName(KEY_MANAGER_3);
        keyManagerDTO.setDescription("This is Key Manager");
        keyManagerDTO.setEnabled(true);
        keyManagerDTO.setTokenEndpoint(token_ep);
        keyManagerDTO.setRevokeEndpoint(revoke_ep);
        keyManagerDTO.setDisplayTokenEndpoint(display_token_ep);
        keyManagerDTO.setRevokeEndpoint(revoke_ep);
        keyManagerDTO.setDisplayRevokeEndpoint(display_revoke_ep);
        keyManagerDTO.setIssuer(ISSUER_3);

        org.wso2.am.integration.clients.admin.ApiResponse<KeyManagerDTO>
                keyManagerDTOApiResponse = restAPIAdmin.addKeyManager(keyManagerDTO);
        keyManager3Id = keyManagerDTOApiResponse.getData().getId();

        // when no display token endpoints available
        KeyManagerDTO keyManagerDTO1 = new KeyManagerDTO();
        keyManagerDTO1.setType("custom");
        keyManagerDTO1.setName(KEY_MANAGER_4);
        keyManagerDTO1.setDescription("This is Key Manager");
        keyManagerDTO1.setEnabled(true);
        keyManagerDTO1.setTokenEndpoint(token_ep);
        keyManagerDTO1.setRevokeEndpoint(revoke_ep);
        keyManagerDTO1.setDisplayTokenEndpoint("");
        keyManagerDTO1.setRevokeEndpoint(revoke_ep);
        keyManagerDTO1.setDisplayRevokeEndpoint("");
        keyManagerDTO.setIssuer(ISSUER_4);

        org.wso2.am.integration.clients.admin.ApiResponse<KeyManagerDTO>
                keyManagerDTOApiResponse1 = restAPIAdmin.addKeyManager(keyManagerDTO1);
        keyManager4Id = keyManagerDTOApiResponse1.getData().getId();

        for (KeyManagerInfoDTO keyManager : restAPIStore.getKeyManagers().getList()) {
            if (keyManager.getName().equals(KEY_MANAGER_3)) {
                Assert.assertEquals(keyManager.getTokenEndpoint(), display_token_ep);
                Assert.assertEquals(keyManager.getRevokeEndpoint(), display_revoke_ep);
            }
            if (keyManager.getName().equals(KEY_MANAGER_4)) {
                Assert.assertEquals(keyManager.getTokenEndpoint(), token_ep);
                Assert.assertEquals(keyManager.getRevokeEndpoint(), revoke_ep);
            }
        }
    }

    @Test(groups = {"wso2.am"}, description = "validating display token endpoint for exchange token grant")
    public void testCreateKeyManagerForExchangeType() throws Exception {

        String display_token_ep = "http://test.apim.integration/oauth/token";
        String display_alias = "https://default";
        String tokenType = KeyManagerDTO.TokenTypeEnum.EXCHANGED.toString();

        KeyManagerDTO keyManagerDTO = new KeyManagerDTO();

        keyManagerDTO.name(KEY_MANAGER_5);
        keyManagerDTO.displayName("Exchange Grant Type");
        keyManagerDTO.type("custom");
        keyManagerDTO.description("This is Exchange Grant Key Manager");
        keyManagerDTO.enabled(true);
        keyManagerDTO.alias("https://default");
        keyManagerDTO.tokenEndpoint("http://test.apim.integration/oauth/token");
        keyManagerDTO.setIssuer(ISSUER_5);
        keyManagerDTO.enableSelfValidationJWT(false);
        keyManagerDTO.enableOAuthAppCreation(false);
        keyManagerDTO.tokenType(KeyManagerDTO.TokenTypeEnum.EXCHANGED);
        KeyManagerCertificatesDTO certificatesDTO = new KeyManagerCertificatesDTO();
        certificatesDTO.type(KeyManagerCertificatesDTO.TypeEnum.JWKS);
        certificatesDTO.value("https://test.apim.integration/oauth2/default/v1/keys");
        keyManagerDTO.certificates(certificatesDTO);
        org.wso2.am.integration.clients.admin.ApiResponse<KeyManagerDTO>
                keyManagerDTOApiResponse = restAPIAdmin.addKeyManager(keyManagerDTO);
        keyManager5Id = keyManagerDTOApiResponse.getData().getId();

        for (KeyManagerInfoDTO keyManager : restAPIStore.getKeyManagers().getList()) {
            if (keyManager.getName().equals(keyManager5Id)) {
                Assert.assertEquals(keyManager.getTokenEndpoint(), display_token_ep);
                Assert.assertEquals(keyManager.getAlias(), display_alias);
                Assert.assertEquals(keyManager.getType(), tokenType);
            }
        }
    }

    @Test(groups = {"wso2.am"}, description = "validating display token endpoint for exchange token grant",
            dependsOnMethods = {"testCreateKeyManagerForExchangeType"})
    public void verifyTokenGenerationEnabledForTokenExchangeType() {

        try {
            restAPIStore.generateKeysWithHttpInfo(jwtApplicationId,
                    APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, "",
                    ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null,
                    Collections.singletonList("client_credentials"), KEY_MANAGER_5);
            Assert.fail();
        } catch (org.wso2.am.integration.clients.store.api.ApiException e) {
            Assert.assertEquals(e.getCode(), 400);
            ErrorDTO errorDTO = new Gson().fromJson(e.getResponseBody(), ErrorDTO.class);
            Assert.assertEquals(errorDTO.getCode().longValue(), 901405);
            Assert.assertEquals(errorDTO.getMessage(), "Key Manager doesn't support generating OAuth applications");
            Assert.assertEquals(errorDTO.getDescription(), "Key Manager doesn't support generating OAuth applications");
        }
    }

    @Test(groups = {"wso2.am"}, description = "validating display token endpoint for exchange token grant")
    public void testExchangeAndDirectGrantType() throws Exception {

        String display_token_ep = "http://test.apim.integration/oauth/token";
        String display_alias = "https://default";
        String tokenType = KeyManagerDTO.TokenTypeEnum.BOTH.toString();

        KeyManagerDTO keyManagerDTO = new KeyManagerDTO();

        keyManagerDTO.name(KEY_MANAGER_6);
        keyManagerDTO.displayName("Hybrid Type");
        keyManagerDTO.type("custom");
        keyManagerDTO.description("This is Exchange Grant Key Manager");
        keyManagerDTO.enabled(true);
        keyManagerDTO.alias("https://default");
        keyManagerDTO.tokenEndpoint("http://test.apim.integration/oauth/token");
        keyManagerDTO.setIssuer(ISSUER_6);
        keyManagerDTO.enableSelfValidationJWT(true);
        keyManagerDTO.enableOAuthAppCreation(false);
        keyManagerDTO.setEnableMapOAuthConsumerApps(true);
        keyManagerDTO.tokenType(KeyManagerDTO.TokenTypeEnum.BOTH);
        KeyManagerCertificatesDTO certificatesDTO = new KeyManagerCertificatesDTO();
        certificatesDTO.type(KeyManagerCertificatesDTO.TypeEnum.JWKS);
        certificatesDTO.setValue("https://localhost:8743/jwks/1.0");
        keyManagerDTO.certificates(certificatesDTO);
        org.wso2.am.integration.clients.admin.ApiResponse<KeyManagerDTO>
                keyManagerDTOApiResponse = restAPIAdmin.addKeyManager(keyManagerDTO);
        keyManager6Id = keyManagerDTOApiResponse.getData().getId();

        for (KeyManagerInfoDTO keyManager : restAPIStore.getKeyManagers().getList()) {
            if (keyManager.getName().equals(keyManager6Id)) {
                Assert.assertEquals(keyManager.getTokenEndpoint(), display_token_ep);
                Assert.assertEquals(keyManager.getAlias(), display_alias);
                Assert.assertEquals(keyManager.getType(), tokenType);
            }
        }
        waitForKeyManagerDeployment(user.getUserDomain(), KEY_MANAGER_6);
    }

    @Test(groups = {"wso2.am"}, description = "validating display token endpoint for exchange token grant",
            dependsOnMethods = {"testExchangeAndDirectGrantType"})
    public void testInvokeFromExchangeToken() throws Exception {

        restAPIStore.mapConsumerKeyWithApplication(consumerKey3, null, jwtApplicationId, KEY_MANAGER_6);
        File keyStoreFile = Paths.get(getAMResourceLocation(), "configFiles", "idpjwt", "idp1.jks").toFile();
        Map attributes = new HashMap();
        attributes.put("azp", consumerKey3);
        attributes.put("http://idp.org/claims/givenname", "first");
        attributes.put("http://idp.org/claims/firstname", "last");
        attributes.put("http://idp.org/claims/email", "first@gmail.com");
        attributes.put("http://idp.org/claims/mobileno", "424479772294778");
        attributes.put("aud", "https://default");
        String generatedJWT =
                JWTGeneratorUtil
                        .generatedJWT(keyStoreFile, "idp2certificate", "idp1", "wso2carbon", "wso2carbon",
                                "userexternal", ISSUER_6, attributes);
        //Invoke from Direct Token
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttp(apiContext, apiVersion));
        log.info("External IDP JWT Generated: " + generatedJWT);
        get.addHeader("Authorization", "Bearer " + generatedJWT);
        HttpResponse response = httpclient.execute(get);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");
        // Generate Exchange Token
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse httpResponse =
                invokeTokenEndpoint(applicationKeyDTO.getConsumerKey(), applicationKeyDTO.getConsumerSecret(),
                        generatedJWT, new String[0]);
        Assert.assertEquals(httpResponse.getResponseCode(), 200);
        JSONObject accessTokenGenerationResponse = new JSONObject(httpResponse.getData());
        log.info("Exchanged Token: " + accessTokenGenerationResponse.getString("access_token"));
        get.setHeader("Authorization", "Bearer " + accessTokenGenerationResponse.getString("access_token"));
        response = httpclient.execute(get);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");
    }

    @Test(groups = {"wso2.am"}, description = "validating display token endpoint for exchange token grant",
            dependsOnMethods = {"testInvokeFromExchangeToken"})
    public void testUpdateKMToExchangeOnly() throws Exception {

        org.wso2.am.integration.clients.admin.ApiResponse<KeyManagerDTO> keyManager =
                restAPIAdmin.getKeyManager(keyManager6Id);
        KeyManagerDTO keyManagerDTO = keyManager.getData();
        Assert.assertNotNull(keyManagerDTO);
        Assert.assertEquals(keyManagerDTO.getAlias(), "https://default");
        Assert.assertEquals(keyManagerDTO.getTokenType(), KeyManagerDTO.TokenTypeEnum.BOTH);
        keyManagerDTO.setTokenType(KeyManagerDTO.TokenTypeEnum.EXCHANGED);
        org.wso2.am.integration.clients.admin.ApiResponse<KeyManagerDTO> keyManagerDTOApiResponse =
                restAPIAdmin.updateKeyManager(keyManager6Id, keyManagerDTO);
        Assert.assertEquals(keyManagerDTOApiResponse.getStatusCode(), 200);
        Assert.assertEquals(keyManagerDTO.getTokenType(), KeyManagerDTO.TokenTypeEnum.EXCHANGED);
        waitForKeyManagerUnDeployment(user.getUserDomain(), KEY_MANAGER_6);
        File keyStoreFile = Paths.get(getAMResourceLocation(), "configFiles", "idpjwt", "idp1.jks").toFile();
        Map attributes = new HashMap();
        attributes.put("azp", consumerKey3);
        attributes.put("given_name", "first");
        attributes.put("family_name ", "last");
        attributes.put("email", "first@gmail.com");
        attributes.put("phone_number", "424479772294778");
        attributes.put("organization", "abc.com");
        attributes.put("aud", "https://default");
        String generatedJWT =
                JWTGeneratorUtil
                        .generatedJWT(keyStoreFile, "idp2certificate", "idp1", "wso2carbon", "wso2carbon",
                                "userexternal", ISSUER_6, attributes);
        //Invoke from Direct Token
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttp(apiContext, apiVersion));
        log.info("External IDP JWT Generated: " + generatedJWT);
        get.addHeader("Authorization", "Bearer " + generatedJWT);
        HttpResponse response = httpclient.execute(get);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                "Response code mismatched when api invocation");
        // Generate Exchange Token
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse httpResponse =
                invokeTokenEndpoint(applicationKeyDTO.getConsumerKey(), applicationKeyDTO.getConsumerSecret(),
                        generatedJWT, new String[0]);
        Assert.assertEquals(httpResponse.getResponseCode(), 200);
        JSONObject accessTokenGenerationResponse = new JSONObject(httpResponse.getData());
        log.info("Exchanged Token: " + accessTokenGenerationResponse.getString("access_token"));
        get.setHeader("Authorization", "Bearer " + accessTokenGenerationResponse.getString("access_token"));
        response = httpclient.execute(get);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");
    }

    @Test(groups = {"wso2.am"}, description = "validating display token endpoint for exchange token grant",
            dependsOnMethods = {"testUpdateKMToExchangeOnly"})
    public void testRemoveExchangeGrantAndCheckInvocation() throws Exception {

        ApplicationKeyDTO applicationKeyByKeyMappingId =
                restAPIStore.getApplicationKeyByKeyMappingId(jwtApplicationId, applicationKeyDTO.getKeyMappingId());
        applicationKeyByKeyMappingId.setSupportedGrantTypes(Arrays.asList("client_credentials"));
        applicationKeyByKeyMappingId.setAdditionalProperties(null);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.updateApplicationKeyByKeyMappingId(jwtApplicationId,
                this.applicationKeyDTO.getKeyMappingId(), applicationKeyByKeyMappingId);
        Assert.assertTrue(applicationKeyDTO.getSupportedGrantTypes().contains("client_credentials"));
        Assert.assertFalse(applicationKeyDTO.getSupportedGrantTypes().contains("urn:ietf:params:oauth:grant-type" +
                ":token-exchange"));
        File keyStoreFile = Paths.get(getAMResourceLocation(), "configFiles", "idpjwt", "idp1.jks").toFile();
        Map attributes = new HashMap();
        attributes.put("azp", consumerKey3);
        attributes.put("given_name", "first");
        attributes.put("family_name ", "last");
        attributes.put("email", "first@gmail.com");
        attributes.put("phone_number", "424479772294778");
        attributes.put("organization", "abc.com");
        attributes.put("aud", "https://default");
        String generatedJWT =
                JWTGeneratorUtil
                        .generatedJWT(keyStoreFile, "idp2certificate", "idp1", "wso2carbon", "wso2carbon",
                                "userexternal", "http://exchange.apim.integration/default", attributes);
        // Generate Exchange Token
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse httpResponse =
                invokeTokenEndpoint(applicationKeyDTO.getConsumerKey(), applicationKeyDTO.getConsumerSecret(),
                        generatedJWT, new String[0]);
        Assert.assertEquals(httpResponse.getResponseCode(), 400);
        applicationKeyByKeyMappingId =
                restAPIStore.getApplicationKeyByKeyMappingId(jwtApplicationId, applicationKeyDTO.getKeyMappingId());
        applicationKeyByKeyMappingId.setAdditionalProperties(null);
        applicationKeyByKeyMappingId.setSupportedGrantTypes(Arrays.asList("client_credentials", "urn:ietf:params:oauth:grant-type:token-exchange"));
        applicationKeyDTO = restAPIStore.updateApplicationKeyByKeyMappingId(jwtApplicationId,
                this.applicationKeyDTO.getKeyMappingId(), applicationKeyByKeyMappingId);
        Assert.assertTrue(applicationKeyDTO.getSupportedGrantTypes().contains("client_credentials"));
        Assert.assertTrue(applicationKeyDTO.getSupportedGrantTypes().contains("urn:ietf:params:oauth:grant-type:token-exchange"));
    }

    @Test(groups = {"wso2.am"}, description = "validating display token endpoint for exchange token grant",
            dependsOnMethods = {"testRemoveExchangeGrantAndCheckInvocation"})
    public void testUpdateKMToDirect() throws Exception {

        org.wso2.am.integration.clients.admin.ApiResponse<KeyManagerDTO> keyManager =
                restAPIAdmin.getKeyManager(keyManager6Id);
        KeyManagerDTO keyManagerDTO = keyManager.getData();
        Assert.assertNotNull(keyManagerDTO);
        Assert.assertEquals(keyManagerDTO.getAlias(), "https://default");
        Assert.assertEquals(keyManagerDTO.getTokenType(), KeyManagerDTO.TokenTypeEnum.EXCHANGED);
        keyManagerDTO.setTokenType(KeyManagerDTO.TokenTypeEnum.DIRECT);
        org.wso2.am.integration.clients.admin.ApiResponse<KeyManagerDTO> keyManagerDTOApiResponse =
                restAPIAdmin.updateKeyManager(keyManager6Id, keyManagerDTO);
        Assert.assertEquals(keyManagerDTOApiResponse.getStatusCode(), 200);
        Assert.assertEquals(keyManagerDTO.getTokenType(), KeyManagerDTO.TokenTypeEnum.DIRECT);
        waitForKeyManagerUnDeployment(user.getUserDomain(), KEY_MANAGER_6);
        waitForKeyManagerDeployment(user.getUserDomain(), KEY_MANAGER_6);
        File keyStoreFile = Paths.get(getAMResourceLocation(), "configFiles", "idpjwt", "idp1.jks").toFile();
        Map attributes = new HashMap();
        attributes.put("azp", consumerKey3);
        attributes.put("given_name", "first");
        attributes.put("family_name ", "last");
        attributes.put("email", "first@gmail.com");
        attributes.put("phone_number", "424479772294778");
        attributes.put("organization", "abc.com");
        attributes.put("aud", "https://default");
        String generatedJWT =
                JWTGeneratorUtil
                        .generatedJWT(keyStoreFile, "idp2certificate", "idp1", "wso2carbon", "wso2carbon",
                                "userexternal", ISSUER_6, attributes);
        //Invoke from Direct Token
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(getAPIInvocationURLHttp(apiContext, apiVersion));
        log.info("External IDP JWT Generated: " + generatedJWT);
        get.addHeader("Authorization", "Bearer " + generatedJWT);
        HttpResponse response = httpclient.execute(get);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");
        // Generate Exchange Token
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse httpResponse =
                invokeTokenEndpoint(applicationKeyDTO.getConsumerKey(), applicationKeyDTO.getConsumerSecret(),
                        generatedJWT, new String[0]);
        Assert.assertEquals(httpResponse.getResponseCode(), 400);
    }

    @Factory(dataProvider = "userModeDataProvider")
    public ExternalIDPJWTTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    private String createKeyManager1(RestAPIAdminImpl restAPIAdmin) throws Exception {

        KeyManagerDTO keyManagerDTO = new KeyManagerDTO();
        keyManagerDTO.setType("custom");
        keyManagerDTO.setName(KEY_MANAGER_1);
        keyManagerDTO.setDescription("This is Key Manager");
        keyManagerDTO.setEnabled(true);
        keyManagerDTO.setIssuer(ISSUER_1);
        keyManagerDTO.setEnableMapOAuthConsumerApps(true);
        keyManagerDTO.setEnableTokenGeneration(false);
        keyManagerDTO.setEnableOAuthAppCreation(false);
        keyManagerDTO
                .setAvailableGrantTypes(Arrays.asList("client_credentials", "password", "implicit", "refresh_token"));
        TokenValidationDTO tokenValidationDTO = new TokenValidationDTO();
        tokenValidationDTO.setEnable(false);
        List<TokenValidationDTO> tokenValidationDTOList = new ArrayList<>();
        tokenValidationDTOList.add(tokenValidationDTO);
        keyManagerDTO.setTokenValidation(tokenValidationDTOList);
        keyManagerDTO.setEnableSelfValidationJWT(true);
        List<ClaimMappingEntryDTO> claimMappingEntryDTOS = new ArrayList<>();
        claimMappingEntryDTOS.add(new ClaimMappingEntryDTO().remoteClaim("http://idp.org/claims/givenname")
                .localClaim("http://wso2.org/claims/givenname"));
        claimMappingEntryDTOS.add(new ClaimMappingEntryDTO().remoteClaim("http://idp.org/claims/firstname")
                .localClaim("http://wso2.org/claims/firstname"));
        claimMappingEntryDTOS.add(new ClaimMappingEntryDTO().remoteClaim("http://idp.org/claims/email")
                .localClaim("http://wso2.org/claims/email"));
        keyManagerDTO.setClaimMapping(claimMappingEntryDTOS);
        org.wso2.am.integration.clients.admin.ApiResponse<KeyManagerDTO>
                keyManagerDTOApiResponse = restAPIAdmin.addKeyManager(keyManagerDTO);
        KeyManagerDTO retrievedData = keyManagerDTOApiResponse.getData();
        return retrievedData.getId();
    }

    private String createKeyManager2(RestAPIAdminImpl restAPIAdmin) throws ApiException {

        KeyManagerDTO keyManagerDTO = new KeyManagerDTO();
        keyManagerDTO.setType("custom");
        keyManagerDTO.setName(KEY_MANAGER_2);
        keyManagerDTO.setDescription("This is Key Manager");
        keyManagerDTO.setEnabled(true);
        keyManagerDTO.setIssuer(ISSUER_2);
        KeyManagerCertificatesDTO keyManagerCertificatesDTO = new KeyManagerCertificatesDTO();
        keyManagerCertificatesDTO.setType(KeyManagerCertificatesDTO.TypeEnum.JWKS);
        keyManagerCertificatesDTO.setValue("https://localhost:8743/jwks/1.0");
        keyManagerDTO.setCertificates(keyManagerCertificatesDTO);
        keyManagerDTO.setEnableMapOAuthConsumerApps(true);
        keyManagerDTO.setEnableTokenGeneration(false);
        keyManagerDTO.setEnableOAuthAppCreation(false);
        keyManagerDTO.setTokenType(KeyManagerDTO.TokenTypeEnum.DIRECT);
        keyManagerDTO
                .setAvailableGrantTypes(Arrays.asList("client_credentials", "password", "implicit", "refresh_token"));
        TokenValidationDTO tokenValidationDTO = new TokenValidationDTO();
        tokenValidationDTO.setEnable(false);
        List<TokenValidationDTO> tokenValidationDTOList = new ArrayList<>();
        tokenValidationDTOList.add(tokenValidationDTO);
        keyManagerDTO.setTokenValidation(tokenValidationDTOList);
        keyManagerDTO.setEnableSelfValidationJWT(true);
        List<ClaimMappingEntryDTO> claimMappingEntryDTOS = new ArrayList<>();
        claimMappingEntryDTOS.add(new ClaimMappingEntryDTO().remoteClaim("http://idp2.org/claims/givenname")
                .localClaim("http://wso2.org/claims/givenname"));
        claimMappingEntryDTOS.add(new ClaimMappingEntryDTO().remoteClaim("http://idp2.org/claims/firstname")
                .localClaim("http://wso2.org/claims/firstname"));
        claimMappingEntryDTOS.add(new ClaimMappingEntryDTO().remoteClaim("http://idp2.org/claims/email")
                .localClaim("http://wso2.org/claims/email"));
        keyManagerDTO.setClaimMapping(claimMappingEntryDTOS);
        org.wso2.am.integration.clients.admin.ApiResponse<KeyManagerDTO>
                keyManagerDTOApiResponse = restAPIAdmin.addKeyManager(keyManagerDTO);
        KeyManagerDTO retrievedData = keyManagerDTOApiResponse.getData();
        return retrievedData.getId();
    }

    private void addValidIdentityProvider() throws Exception {
        addIdentityProvider(jwtAudience, jwtIssuer, keystoreFileValid, keystoreFileValidPass, keystoreFileValidAlias);
    }

    /**
     * To add the identity provider.
     *
     * @throws Exception Exception.
     */
    private void addIdentityProvider(String alias, String issuer, String keystoreFile, String pwd, String certAlias)
            throws Exception {
        File keyStoreFile = Paths.get(getAMResourceLocation(), "configFiles", "jwtgrant", keystoreFile).toFile();

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        FileInputStream fileInputStream = new FileInputStream(keyStoreFile);
        keyStore.load(fileInputStream, pwd.toCharArray());
        Certificate publicCert = keyStore.getCertificate("extidpjwt");
        byte[] encoded = Base64.encodeBase64(publicCert.getEncoded());

        IdentityProvider identityProvider = new IdentityProvider();
        identityProvider.setEnable(true);
        identityProvider.setAlias(alias);
        identityProvider.setDisplayName("disp_name");
        identityProvider.setCertificate(new String(encoded));
        identityProvider.setIdentityProviderName(issuer);
        identityProviderMgtServiceClient.addIdP(identityProvider);

        final String idpAddFailureError = "Identity provider was not added correctly";
        IdentityProvider addedIdp = identityProviderMgtServiceClient.getIdPByName(jwtIssuer);
        Assert.assertNotNull(addedIdp, idpAddFailureError);
        Assert.assertEquals(addedIdp.getIdentityProviderName(), jwtIssuer, idpAddFailureError);
    }

    @Test(description = "test Generate consumer Keys when oauth app creation disable")
    public void generateKeysNegative() throws APIManagerIntegrationTestException {

        org.wso2.carbon.automation.test.utils.http.client.HttpResponse applicationDTO =
                restAPIStore.createApplication("JWT_NEGATIVE_APP", "JWT Application3",
                        APIMIntegrationConstants.APPLICATION_TIER.TEN_PER_MIN, ApplicationDTO.TokenTypeEnum.JWT);
        String appId = applicationDTO.getData();
        try {
            ApiResponse<ApplicationKeyDTO> applicationKeyDTOApiResponse =
                    restAPIStore.generateKeysWithApiResponse(appId, "300", "https://localhost",
                            ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, Arrays.asList("default"),
                            Arrays.asList(
                                    "client_credentials"), Collections.emptyMap(), KEY_MANAGER_1);
            Assert.fail("Consumer Key Generated For App for key manager not supported");
        } catch (org.wso2.am.integration.clients.store.api.ApiException e) {
            Assert.assertEquals(e.getCode(), 400);
            ErrorDTO errorDTO = new Gson().fromJson(e.getResponseBody(), ErrorDTO.class);
            Assert.assertEquals(errorDTO.getCode().longValue(), 901405);
            Assert.assertEquals(errorDTO.getMessage(), "Key Manager doesn't support generating OAuth applications");
            Assert.assertEquals(errorDTO.getDescription(), "Key Manager doesn't support generating OAuth applications");
        }
        restAPIStore.deleteApplication(appId);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        restAPIStore.deleteApplication(jwtApplicationId);
        restAPIAdmin.deleteKeyManager(keyManager1Id);
        restAPIAdmin.deleteKeyManager(keyManager2Id);
        restAPIAdmin.deleteKeyManager(keyManager3Id);
        restAPIAdmin.deleteKeyManager(keyManager4Id);
        restAPIAdmin.deleteKeyManager(keyManager5Id);
        restAPIAdmin.deleteKeyManager(keyManager6Id);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        undeployAndDeleteAPIRevisionsUsingRest(apiIdOnlyKm1, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiIdOnlyKm1);
        super.cleanUp();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN}};
    }

    private org.wso2.carbon.automation.test.utils.http.client.HttpResponse invokeTokenEndpoint(String consumerKey,
                                                                                               String consumerSecret,
                                                                                               String assertion,
                                                                                               String[] scopes)
            throws IOException {

        List<NameValuePair> urlParameters = new ArrayList<>();
        Map<String, String> headers = new HashMap<>();
        String base64EncodedAppCredentials = TokenUtils.getBase64EncodedAppCredentials(consumerKey, consumerSecret);
        headers.put("Authorization", "Basic " + base64EncodedAppCredentials);
        urlParameters.add(new BasicNameValuePair("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange"));
        urlParameters.add(new BasicNameValuePair("subject_token_type", "urn:ietf:params:oauth:token-type:jwt"));
        urlParameters.add(new BasicNameValuePair("requested_token_type", "urn:ietf:params:oauth:token-type:jwt"));
        urlParameters.add(new BasicNameValuePair("subject_token", assertion));
        if (scopes != null && scopes.length > 0) {
            urlParameters.add(new BasicNameValuePair("scope", String.join(" ", scopes)));
        }
        return HTTPSClientUtils.doPost(tokenEndpointURLString, headers, urlParameters);
    }
}
