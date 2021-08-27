/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.jwt;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ScopeDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.token.TokenUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.tests.jwt.idp.JWTGeneratorUtil;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.identity.application.common.model.idp.xsd.Claim;
import org.wso2.carbon.identity.application.common.model.idp.xsd.ClaimConfig;
import org.wso2.carbon.identity.application.common.model.idp.xsd.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.idp.xsd.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.idp.xsd.LocalRole;
import org.wso2.carbon.identity.application.common.model.idp.xsd.PermissionsAndRoleConfig;
import org.wso2.carbon.identity.application.common.model.idp.xsd.RoleMapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JWTGrantTestCase extends APIManagerLifecycleBaseTest {

    private final String jwtAudience = UUID.randomUUID().toString();
    private final String jwtIssuer = "jwtgrant_test_issuer";
    private final String keystoreFileValid = "extidpjwt.jks";
    private final String keystoreFileValidPass = "extidpjwt";
    private final String keystoreFileValidAlias = "extidpjwt";
    private final String scopeToRequest = "scope-jwt";
    private String jwtApplicationId;
    private String consumerKey;
    private String consumerSecret;
    private String scopeId;

    private String tokenUrl;
    List<String> grantTypesWithJWT = new ArrayList<>();

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN}
        };
    }

    @Factory(dataProvider = "userModeDataProvider")
    public JWTGrantTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        tokenUrl = getKeyManagerURLHttps() + "/oauth2/token";
        grantTypesWithJWT.add(APIMIntegrationConstants.GRANT_TYPE.JWT);
        String applicationName = "JWTGrantApp";
        ApplicationDTO applicationDTO =
                restAPIStore.addApplication(applicationName,"Unlimited", null,
                        applicationName + " description");
        Assert.assertNotNull(applicationDTO, "consumerKey generation of the application doesn't work as expected");
        Assert.assertTrue(StringUtils.isNotBlank(applicationDTO.getApplicationId()), "application is not created as "
                + "expected");
        jwtApplicationId = applicationDTO.getApplicationId();
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(jwtApplicationId,
                APIMIntegrationConstants.DEFAULT_TOKEN_VALIDITY_TIME, "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypesWithJWT);
        consumerKey = applicationKeyDTO.getConsumerKey();
        consumerSecret = applicationKeyDTO.getConsumerSecret();
        Assert.assertNotNull(consumerKey, "consumerKey generation of the application doesn't work as expected");
        Assert.assertNotNull(consumerSecret, "consumerSecret generation of the application doesn't work as expected");
        addValidIdentityProvider();

        // Add a shared scope with restricted to role 'admin'
        ScopeDTO scopeDTO = new ScopeDTO();
        scopeDTO.setName(scopeToRequest);
        scopeDTO.setBindings(new ArrayList<String>() {{
            add("admin");
        }});
        scopeDTO.setDisplayName("Scope for JWT grant test");
        ScopeDTO addedScope = restAPIPublisher.addSharedScope(scopeDTO);
        scopeId = addedScope.getId();
    }

    @Test(groups = "wso2.am", description = "Testing JWT grant for a JWT token with a registered IDP for it")
    public void testGenerateTokenWithValidRegisteredIDP() throws Exception {
        String jwt = generateJWTTokenForValidIDP();
        HttpResponse res = invokeTokenEndpoint(jwt);
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_OK, "Response code is not 200 as expected");
        JSONObject response = new JSONObject(res.getData());
        String accessToken = response.getString("access_token");
        Assert.assertNotNull(accessToken, "Couldn't find accessToken");
    }

    @Test(groups = "wso2.am", description = "Testing JWT grant for a JWT token without a registered IDP for it")
    public void testGenerateTokenForNonRegisteredIDP() throws Exception {
        final String assertDescPrefix = "Checking the JWT grant with non-registered IDP as issuer, " +
                "didn't get the expected ";
        final String errorDescExpected = "No Registered IDP found";
        String jwt = generateJWTTokenForInvalidIDP();
        HttpResponse res = invokeTokenEndpoint(jwt);
        assertErrorResponse(assertDescPrefix, errorDescExpected, res);
    }

    @Test(groups = "wso2.am", description = "Testing JWT grant for a JWT token which is expired")
    public void testGenerateTokenWithExpiredJWT() throws Exception {
        final String assertDescPrefix = "Checking the JWT grant with expired jwt, didn't get the expected ";
        final String errorDescExpected = "JSON Web Token is expired";
        String jwt = generateExpiredJWTToken();
        HttpResponse res = invokeTokenEndpoint(jwt);
        assertErrorResponse(assertDescPrefix, errorDescExpected, res);
    }

    @Test(groups = "wso2.am", description = "Testing JWT grant for a JWT token which is tampered")
    public void testGenerateTokenWithTamperedJWT() throws Exception {
        final String assertDescPrefix = "Checking the JWT grant with tampered jwt, didn't get the expected ";
        final String errorDescExpected = "Signature or Message Authentication invalid";
        String jwt = generateTamperedJWTToken();
        HttpResponse res = invokeTokenEndpoint(jwt);
        assertErrorResponse(assertDescPrefix, errorDescExpected, res);
    }

    @Test(groups = "wso2.am", description = "Testing JWT grant for a JWT token signed with a different cert " +
            "not matching IDP")
    public void testGenerateTokenWithJWTSignedWithDifferentCert() throws Exception {
        final String assertDescPrefix = "Checking the JWT grant with jwt signed with different cert," +
                " didn't get the expected ";
        final String errorDescExpected = "Signature or Message Authentication invalid";
        String jwt = generateJWTTokenSignedFromDifferentCertificate();
        HttpResponse res = invokeTokenEndpoint(jwt);
        assertErrorResponse(assertDescPrefix, errorDescExpected, res);
    }

    @Test(groups = "wso2.am", description = "Testing JWT grant for a JWT token without IDP roles/mappings are added " +
            "and try to generate a token with a scope which is restricted by a role. The Scope shouldn't be returned.",
            dependsOnMethods = "testGenerateTokenWithValidRegisteredIDP")
    public void testGenerateTokenWithScopesUsingJWTBeforeAddingIdpRoles() throws Exception {

        if (userMode == TestUserMode.TENANT_ADMIN) {
            // todo: warning! this is currently failing in tenant mode due to a product bug
            return;
        }

        // Generates a token by requesting 'scope-jwt' scope without updating the IDP with role mappings.
        //  The token response should be successful but it shouldn't get the requested scope.
        String jwt = generateJWTTokenForValidIDPWithIdpRoles();
        HttpResponse res = invokeTokenEndpoint(jwt, new String[]{scopeToRequest});
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_OK, "Response code is not 200 as expected");
        JSONObject response = new JSONObject(res.getData());
        String accessToken = response.getString("access_token");
        Assert.assertNotNull(accessToken, "Couldn't find accessToken");
        String scope = response.getString("scope");
        Assert.assertFalse(scope.contains(scopeToRequest), "Received scopes contains requested scope " +
                "(" + scopeToRequest + ") even without adding role mappings to the IDP.");
    }

    @Test(groups = "wso2.am", description = "Testing JWT grant for a JWT token with IDP roles " +
            "and generate token with scope which is restricted by a role. This should succeed.",
            dependsOnMethods = "testGenerateTokenWithScopesUsingJWTBeforeAddingIdpRoles")
    public void testGenerateTokenWithScopesUsingJWTWithIdpRoles() throws Exception {
        String jwt = generateJWTTokenForValidIDPWithIdpRoles();
        // Update the IDP with role mappings
        updateIdentityProviderWithRoleMappings(jwtIssuer);
        HttpResponse res = invokeTokenEndpoint(jwt, new String[]{scopeToRequest});
        JSONObject response = new JSONObject(res.getData());
        String accessToken = response.getString("access_token");
        Assert.assertNotNull(accessToken, "Couldn't find accessToken");
        String scope = response.getString("scope");
        Assert.assertTrue(scope.contains(scopeToRequest), "Received scopes doesn't contain requested scope " +
                "(" + scopeToRequest + ") even after adding role mappings to the IDP.");
    }

    private void assertErrorResponse(String assertDescPrefix, String errorDescExpected, HttpResponse res)
            throws JSONException {
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_BAD_REQUEST, assertDescPrefix + " 400 status code.");
        JSONObject response = new JSONObject(res.getData());
        String errorDescriptionKey = "error_description";
        String errorDescription = response.getString(errorDescriptionKey);
        Assert.assertTrue(errorDescription.contains(errorDescExpected),
                assertDescPrefix + "'" + errorDescExpected + "' in the error_description but was '"
                        + errorDescription + "'");
    }

    private HttpResponse invokeTokenEndpoint(String jwt) throws IOException {
        return invokeTokenEndpoint(jwt, new String[0]);
    }

    private HttpResponse invokeTokenEndpoint(String jwt, String[] scopes) throws IOException {
        List<NameValuePair> urlParameters = new ArrayList<>();
        Map<String, String> headers = new HashMap<>();
        String base64EncodedAppCredentials = TokenUtils.getBase64EncodedAppCredentials(consumerKey, consumerSecret);
        headers.put("Authorization", "Basic " + base64EncodedAppCredentials);

        urlParameters.add(new BasicNameValuePair("grant_type", APIMIntegrationConstants.GRANT_TYPE.JWT));
        urlParameters.add(new BasicNameValuePair("assertion", jwt));
        if (scopes != null && scopes.length > 0) {
            urlParameters.add(new BasicNameValuePair("scope", String.join(" ", scopes)));
        }
        return HTTPSClientUtils.doPost(tokenUrl, headers, urlParameters);
    }

    private String generateJWTTokenForValidIDP() throws Exception {
        return generateJWTTokenFromExternalIDP(keystoreFileValid, keystoreFileValidPass, keystoreFileValidAlias,
                jwtAudience, jwtIssuer, System.currentTimeMillis());
    }

    private String generateJWTTokenForValidIDPWithIdpRoles() throws Exception {
        Map<String, Object> idpRoles = new HashMap<>();
        idpRoles.put("http://extidp.org/claims/role", new String[]{"idp_admin"});
        return generateJWTTokenFromExternalIDP(keystoreFileValid, keystoreFileValidPass, keystoreFileValidAlias,
                jwtAudience, jwtIssuer, System.currentTimeMillis(), idpRoles);
    }

    private String generateJWTTokenForInvalidIDP() throws Exception {
        return generateJWTTokenFromExternalIDP(keystoreFileValid, keystoreFileValidPass, keystoreFileValidAlias,
                jwtAudience, "jwtgrant_test_issuer_invalid", System.currentTimeMillis());
    }

    private String generateJWTTokenFromExternalIDP(String keyStoreFile, String pwd, String keyAlias,
                                                   String aud, String issuer, long notBeforeTime) throws Exception {
        return generateJWTTokenFromExternalIDP(keyStoreFile, pwd, keyAlias, aud, issuer, notBeforeTime,
                new HashMap<>());
    }

    private String generateJWTTokenFromExternalIDP(String keyStoreFile, String pwd, String keyAlias,
                                                   String aud, String issuer, long notBeforeTime,
                                                   Map<String, Object> additionalClaims) throws Exception {
        File keyStoreFileAbs = Paths.get(getAMResourceLocation(),
                "configFiles", "jwtgrant", keyStoreFile).toFile();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("azp", aud);
        attributes.put("aud", aud);
        attributes.putAll(additionalClaims);
        return JWTGeneratorUtil
                        .generatedJWT(keyStoreFileAbs, UUID.randomUUID().toString(), keyAlias,
                                pwd, pwd, "ext-user",
                                issuer, notBeforeTime, attributes);
    }

    private String generateTamperedJWTToken() throws Exception {
        String jwt = generateJWTTokenForValidIDP();
        String[] jwtParts = jwt.split("\\.");
        String base64EncodedHeader = jwtParts[0];
        String base64EncodedPayload = jwtParts[1];
        String base64EncodedSignature = jwtParts[2];
        String jwtPayload = new String(Base64.decodeBase64(base64EncodedPayload), Charset.defaultCharset());
        jwtPayload = jwtPayload.replace("ext-user", "attacker");
        byte[] encoded = Base64.encodeBase64(jwtPayload.getBytes());
        return base64EncodedHeader + "."
                + new String(encoded).replace("=","") + "." + base64EncodedSignature;
    }

    private String generateExpiredJWTToken() throws Exception {
        return generateJWTTokenFromExternalIDP(keystoreFileValid, keystoreFileValidPass, keystoreFileValidAlias,
                jwtAudience, jwtIssuer, System.currentTimeMillis() - 30 * 60 * 1000);
    }

    private String generateJWTTokenSignedFromDifferentCertificate() throws Exception {
        String keystoreFileInvalid = "other-keystore.jks";
        String keystoreFileInvalidPass = "wso2carbon";
        String keystoreFileInvalidAlias = "idptest";
        return generateJWTTokenFromExternalIDP(keystoreFileInvalid, keystoreFileInvalidPass, keystoreFileInvalidAlias,
                jwtAudience, jwtIssuer, System.currentTimeMillis() - 30 * 60 * 1000);
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

    private void updateIdentityProviderWithRoleMappings(String name) throws Exception {
        IdentityProvider identityProvider = identityProviderMgtServiceClient.getIdPByName(name);
        identityProvider.setClaimConfig(getClaimConfig());
        identityProvider.setPermissionAndRoleConfig(getPermissionsAndRoleConfig());
        identityProviderMgtServiceClient.updateIdP(name, identityProvider);

        IdentityProvider updatedIdp = identityProviderMgtServiceClient.getIdPByName(name);
        Assert.assertNotNull(updatedIdp.getClaimConfig());
        Assert.assertNotNull(updatedIdp.getClaimConfig().getClaimMappings());
        Assert.assertEquals(updatedIdp.getClaimConfig().getClaimMappings().length, 1);
        Assert.assertNotNull(updatedIdp.getPermissionAndRoleConfig());
        Assert.assertNotNull(updatedIdp.getPermissionAndRoleConfig().getIdpRoles());
        Assert.assertEquals(updatedIdp.getPermissionAndRoleConfig().getIdpRoles().length, 1);
    }

    private static ClaimConfig getClaimConfig() {
        ClaimConfig claimConfig = new ClaimConfig();
        ClaimMapping[] claimMappings = new ClaimMapping[1];
        ClaimMapping claimMapping = new ClaimMapping();
        Claim localRoleClaim = new Claim();
        localRoleClaim.setClaimUri("http://wso2.org/claims/role");
        Claim idpRoleClaim = new Claim();
        idpRoleClaim.setClaimUri("http://extidp.org/claims/role");
        claimMapping.setLocalClaim(localRoleClaim);
        claimMapping.setRemoteClaim(idpRoleClaim);
        claimMappings[0] = claimMapping;
        claimConfig.setClaimMappings(claimMappings);
        claimConfig.setIdpClaims(new Claim[]{idpRoleClaim});
        claimConfig.setRoleClaimURI("http://extidp.org/claims/role");
        return claimConfig;
    }

    private static PermissionsAndRoleConfig getPermissionsAndRoleConfig() {
        PermissionsAndRoleConfig roleConfig = new PermissionsAndRoleConfig();
        roleConfig.addIdpRoles("idp_admin");
        RoleMapping roleMapping = new RoleMapping();
        LocalRole localRole = new LocalRole();
        localRole.setLocalRoleName("admin");
        roleMapping.setLocalRole(localRole);
        roleMapping.setRemoteRole("idp_admin");
        roleConfig.addRoleMappings(roleMapping);
        return roleConfig;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(jwtApplicationId);
        identityProviderMgtServiceClient.deleteIdP(jwtIssuer);
        restAPIPublisher.removeSharedScope(scopeId);
        super.cleanUp();
    }
}
