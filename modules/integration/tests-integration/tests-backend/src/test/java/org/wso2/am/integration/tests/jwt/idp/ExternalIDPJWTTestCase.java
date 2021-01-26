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
import org.apache.commons.io.IOUtils;
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
import org.wso2.am.integration.test.impl.RestAPIAdminImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
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
    private static final String KEY_MANAGER_1 = "KeyManager-1";
    private static final String KEY_MANAGER_2 = "KeyManager-2";
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
    private String endpointURL;
    private String jwtApplicationId;
    private String apiId;
    URL tokenEndpointURL;
    private final String JWT_ASSERTION_HEADER = "X-JWT-Assertion";
    private String keyManager1Id;
    private String keyManager2Id;
    private String consumerKey1 = UUID.randomUUID().toString();
    private String consumerKey2 = UUID.randomUUID().toString();
    private String apiIdOnlyKm1;

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
        tokenEndpointURL = new URL(gatewayUrlsWrk.getWebAppURLNhttp() + "token");
        apiId = createAPI(apiName, apiContext, Arrays.asList(ALL_KEY_MANAGER));
        apiIdOnlyKm1 = createAPI(apiNameOnlyKM1, apiContextOnlyKM1, Arrays.asList(KEY_MANAGER_1));

        restAPIStore.subscribeToAPI(apiId, jwtApplicationId, TIER_GOLD);
        restAPIStore.subscribeToAPI(apiIdOnlyKm1, jwtApplicationId, TIER_GOLD);
        waitForKeyManagerDeployment(user.getUserDomain(), KEY_MANAGER_1);
        waitForKeyManagerDeployment(user.getUserDomain(), KEY_MANAGER_2);
        restAPIStore.mapConsumerKeyWithApplication(consumerKey1, jwtApplicationId, KEY_MANAGER_1);
        restAPIStore.mapConsumerKeyWithApplication(consumerKey2, jwtApplicationId, KEY_MANAGER_2);
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
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Response code mismatched when api invocation");
        String payload = IOUtils.toString(response.getEntity().getContent());
        Assert.assertTrue(payload.contains("900900"));
        Assert.assertTrue(payload.contains("Unclassified Authentication Failure"));
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

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        restAPIStore.deleteApplication(jwtApplicationId);
        restAPIAdmin.deleteKeyManager(keyManager1Id);
        restAPIAdmin.deleteKeyManager(keyManager2Id);
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

    @Factory(dataProvider = "userModeDataProvider")
    public ExternalIDPJWTTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    public String createKeyManager1(RestAPIAdminImpl restAPIAdmin) throws Exception {

        KeyManagerDTO keyManagerDTO = new KeyManagerDTO();
        keyManagerDTO.setType("custom");
        keyManagerDTO.setName(KEY_MANAGER_1);
        keyManagerDTO.setDescription("This is Key Manager");
        keyManagerDTO.setEnabled(true);
        keyManagerDTO.setIssuer("https://test.apim.integration");
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
        keyManagerDTO.setIssuer("https://test2.apim.integration");
        KeyManagerCertificatesDTO keyManagerCertificatesDTO = new KeyManagerCertificatesDTO();
        keyManagerCertificatesDTO.setType(KeyManagerCertificatesDTO.TypeEnum.JWKS);
        keyManagerCertificatesDTO.setValue("https://localhost:8743/jwks/1.0");
        keyManagerDTO.setCertificates(keyManagerCertificatesDTO);
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

    @Test(description = "test Generate consumer Keys when oauth app creation disable")
    public void generateKeysNegative() {

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

}
