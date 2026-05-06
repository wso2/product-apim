/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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
 */

package org.wso2.am.integration.tests.jwt.idp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.api.dto.ClaimMappingEntryDTO;
import org.wso2.am.integration.clients.admin.api.dto.KeyManagerCertificatesDTO;
import org.wso2.am.integration.clients.admin.api.dto.KeyManagerDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.token.TokenUtils;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.identity.application.common.model.xsd.Claim;
import org.wso2.carbon.identity.application.common.model.xsd.ClaimConfig;
import org.wso2.carbon.identity.application.common.model.xsd.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.xsd.ServiceProvider;
import org.wso2.carbon.identity.oauth.stub.dto.OAuthConsumerAppDTO;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests token exchange custom claim handling with multi-value groups claims.
 */
public class TokenExchangeMultiValueClaimTestCase extends APIManagerLifecycleBaseTest {

    private static final String TOKEN_EXCHANGE_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:token-exchange";
    private static final String JWT_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:jwt";
    private static final String EXTERNAL_IDP_ISSUER = "https://external-idp.apim.integration";
    private static final String EXTERNAL_IDP_ALIAS = "external-api";
    private static final String EXTERNAL_CLIENT_ID = "external-client";
    private static final String EXTERNAL_CLIENT_SECRET = "clientSecretValue";
    private static final String DISPLAY_NAME_CLAIM_URI = "http://wso2.org/claims/displayName";
    private static final String GROUPS_CLAIM_URI = "http://wso2.org/claims/groups";
    private static final List<String> SUBJECT_TOKEN_GROUPS = Arrays.asList("engineering", "support", "analytics");

    private String applicationId;
    private String keyManagerId;
    private String keyManagerName;
    private ApplicationKeyDTO applicationKeyDTO;
    private String tokenEndpointURLString;

    @Factory(dataProvider = "userModeDataProvider", dataProviderClass = ExternalIDPJWTTestCase.class)
    public TokenExchangeMultiValueClaimTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setupTokenExchangeMultiValueClaimTest() throws Exception {

        super.init(userMode);
        keyManagerName = "TokenExchangeMultiValueClaimKM-" + userMode.name();
        tokenEndpointURLString = getKeyManagerURLHttps() + "/oauth2/token";

        HttpResponse applicationResponse = restAPIStore.createApplication(
                "TokenExchangeMultiValueClaimApp-" + userMode.name(), "JWT Application",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();

        keyManagerId = createKeyManager();
        waitForKeyManagerDeployment(user.getUserDomain(), keyManagerName);
        applicationKeyDTO = restAPIStore.generateKeysWithAdditionalProperties(applicationId, "3600", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, Arrays.asList("default"),
                Arrays.asList("password", "client_credentials", TOKEN_EXCHANGE_GRANT_TYPE),
                getResidentKeyManagerAdditionalProperties());
        updateServiceProviderWithRequiredClaims(applicationKeyDTO.getConsumerKey());
    }

    @Test(groups = { "wso2.am" }, description = "Validate token exchange groups claim handling for multi-value claims")
    public void testTokenExchangeGroupsClaimHandlingForMultiValueClaims() throws Exception {

        HttpResponse tokenResponse = invokeTokenEndpoint(generateSubjectToken(),
                new String[] { "email", "groups", "openid", "profile" });
        Assert.assertEquals(tokenResponse.getResponseCode(), 200, tokenResponse.getData());

        JSONObject tokenResponseBody = new JSONObject(tokenResponse.getData());
        JWTClaimsSet exchangedAccessTokenClaims = SignedJWT.parse(tokenResponseBody.getString("access_token"))
                .getJWTClaimsSet();

        Assert.assertEquals(exchangedAccessTokenClaims.getStringListClaim("groups"), SUBJECT_TOKEN_GROUPS,
                "Groups claim values are not handled as expected in the exchanged access token. Claims: "
                        + exchangedAccessTokenClaims.getClaims());
        Assert.assertEquals(exchangedAccessTokenClaims.getStringClaim("preferred_username"), "user1",
                "Single-valued custom claims should continue to be preserved. Claims: "
                        + exchangedAccessTokenClaims.getClaims());
    }

    @AfterClass(alwaysRun = true)
    public void cleanupTokenExchangeMultiValueClaimTest() throws Exception {

        if (applicationId != null) {
            restAPIStore.deleteApplication(applicationId);
        }
        if (keyManagerId != null) {
            restAPIAdmin.deleteKeyManager(keyManagerId);
        }
        super.cleanUp();
    }

    private String createKeyManager() throws Exception {

        KeyManagerDTO keyManagerDTO = new KeyManagerDTO();
        keyManagerDTO.setName(keyManagerName);
        keyManagerDTO.setDisplayName(keyManagerName);
        keyManagerDTO.setType("custom");
        keyManagerDTO.setDescription("Token exchange multi-value claim handling external OIDC key manager");
        keyManagerDTO.setEnabled(true);
        keyManagerDTO.setAlias(EXTERNAL_IDP_ALIAS);
        keyManagerDTO.setIssuer(EXTERNAL_IDP_ISSUER);
        keyManagerDTO.setWellKnownEndpoint(EXTERNAL_IDP_ISSUER + "/.well-known/openid-configuration");
        keyManagerDTO.setIntrospectionEndpoint(EXTERNAL_IDP_ISSUER + "/oauth2/introspect");
        keyManagerDTO.setClientRegistrationEndpoint(EXTERNAL_IDP_ISSUER + "/oauth2/register");
        keyManagerDTO.setTokenEndpoint(EXTERNAL_IDP_ISSUER + "/oauth2/token");
        keyManagerDTO.setDisplayTokenEndpoint(EXTERNAL_IDP_ISSUER + "/oauth2/token");
        keyManagerDTO.setRevokeEndpoint(EXTERNAL_IDP_ISSUER + "/oauth2/revoke");
        keyManagerDTO.setDisplayRevokeEndpoint(EXTERNAL_IDP_ISSUER + "/oauth2/revoke");
        keyManagerDTO.setUserInfoEndpoint(EXTERNAL_IDP_ISSUER + "/oauth2/userinfo");
        keyManagerDTO.setAuthorizeEndpoint(EXTERNAL_IDP_ISSUER + "/oauth2/authorize");
        keyManagerDTO.setEnableSelfValidationJWT(true);
        keyManagerDTO.setEnableOAuthAppCreation(true);
        keyManagerDTO.setEnableMapOAuthConsumerApps(true);
        keyManagerDTO.setEnableTokenGeneration(true);
        keyManagerDTO.setEnableTokenEncryption(false);
        keyManagerDTO.setEnableTokenHashing(false);
        keyManagerDTO.setTokenType(KeyManagerDTO.TokenTypeEnum.BOTH);
        keyManagerDTO.setConsumerKeyClaim("");
        keyManagerDTO.setScopesClaim("");
        keyManagerDTO.setAvailableGrantTypes(
                Arrays.asList("authorization_code", "client_credentials", "implicit", "password", "refresh_token",
                        "urn:ietf:params:oauth:grant-type:device_code", TOKEN_EXCHANGE_GRANT_TYPE,
                        "urn:ietf:params:oauth:grant-type:uma-ticket", "urn:openid:params:grant-type:ciba"));

        KeyManagerCertificatesDTO certificatesDTO = new KeyManagerCertificatesDTO();
        certificatesDTO.setType(KeyManagerCertificatesDTO.TypeEnum.JWKS);
        certificatesDTO.setValue("https://localhost:8743/jwks/1.0");
        keyManagerDTO.setCertificates(certificatesDTO);

        List<ClaimMappingEntryDTO> claimMappings = new ArrayList<>();
        claimMappings.add(new ClaimMappingEntryDTO().remoteClaim("groups").localClaim(GROUPS_CLAIM_URI));
        claimMappings.add(
                new ClaimMappingEntryDTO().remoteClaim("preferred_username").localClaim(DISPLAY_NAME_CLAIM_URI));
        keyManagerDTO.setClaimMapping(claimMappings);
        keyManagerDTO.setAdditionalProperties(getExternalIdpAdditionalProperties());

        return restAPIAdmin.addKeyManager(keyManagerDTO).getData().getId();
    }

    private Map<String, Object> getResidentKeyManagerAdditionalProperties() {

        Map<String, Object> additionalProperties = new HashMap<>();
        additionalProperties.put("application_access_token_expiry_time", "N/A");
        additionalProperties.put("user_access_token_expiry_time", "N/A");
        additionalProperties.put("refresh_token_expiry_time", "N/A");
        additionalProperties.put("id_token_expiry_time", "N/A");
        additionalProperties.put("pkceMandatory", "false");
        additionalProperties.put("pkceSupportPlain", "false");
        additionalProperties.put("bypassClientCredentials", "false");
        return additionalProperties;
    }

    private Object getExternalIdpAdditionalProperties() {

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("client_id", EXTERNAL_CLIENT_ID);
        jsonObject.addProperty("client_secret", EXTERNAL_CLIENT_SECRET);
        jsonObject.addProperty("self_validate_jwt", true);
        return new Gson().fromJson(jsonObject, Map.class);
    }

    private void updateServiceProviderWithRequiredClaims(String consumerKey) throws Exception {

        OAuthConsumerAppDTO oAuthApplicationData = oAuthAdminServiceClient.getOAuthApplicationData(consumerKey);
        ServiceProvider serviceProvider = applicationManagementClient.getApplication(
                oAuthApplicationData.getApplicationName());
        ClaimConfig claimConfig = new ClaimConfig();
        claimConfig.setLocalClaimDialect(true);
        for (String claimUri : Arrays.asList(DISPLAY_NAME_CLAIM_URI, GROUPS_CLAIM_URI)) {
            Claim claim = new Claim();
            claim.setClaimUri(claimUri);

            ClaimMapping claimMapping = new ClaimMapping();
            claimMapping.setLocalClaim(claim);
            claimMapping.setRemoteClaim(claim);
            claimMapping.setRequested(true);
            claimMapping.setMandatory(true);
            claimConfig.addClaimMappings(claimMapping);
        }
        serviceProvider.setClaimConfig(claimConfig);
        applicationManagementClient.updateApplication(serviceProvider);
    }

    private String generateSubjectToken() throws Exception {

        File keyStoreFile = Paths.get(getAMResourceLocation(), "configFiles", "idpjwt", "idp1.jks").toFile();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("typ", "Bearer");
        attributes.put("azp", EXTERNAL_CLIENT_ID);
        attributes.put("aud", EXTERNAL_IDP_ALIAS);
        attributes.put("groups", SUBJECT_TOKEN_GROUPS);
        attributes.put("preferred_username", "user1");
        attributes.put("scope", "openid profile email");
        attributes.put("email_verified", false);
        attributes.put("name", "user 1");
        attributes.put("given_name", "user");
        attributes.put("family_name", "1");
        attributes.put("email", "user1@abc.com");

        return JWTGeneratorUtil.generatedJWT(keyStoreFile, "idp2certificate", "idp1", "wso2carbon", "wso2carbon",
                "userexternal", EXTERNAL_IDP_ISSUER, attributes);
    }

    private HttpResponse invokeTokenEndpoint(String subjectToken, String[] scopes) throws Exception {

        List<NameValuePair> urlParameters = new ArrayList<>();
        Map<String, String> headers = new HashMap<>();
        String base64EncodedAppCredentials = TokenUtils.getBase64EncodedAppCredentials(
                applicationKeyDTO.getConsumerKey(), applicationKeyDTO.getConsumerSecret());
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Authorization", "Basic " + base64EncodedAppCredentials);
        urlParameters.add(new BasicNameValuePair("grant_type", TOKEN_EXCHANGE_GRANT_TYPE));
        urlParameters.add(new BasicNameValuePair("subject_token", subjectToken));
        urlParameters.add(new BasicNameValuePair("subject_token_type", JWT_TOKEN_TYPE));
        urlParameters.add(new BasicNameValuePair("requested_token_type", JWT_TOKEN_TYPE));
        urlParameters.add(new BasicNameValuePair("scope", String.join(" ", scopes)));
        return HTTPSClientUtils.doPost(tokenEndpointURLString, headers, urlParameters);
    }
}
