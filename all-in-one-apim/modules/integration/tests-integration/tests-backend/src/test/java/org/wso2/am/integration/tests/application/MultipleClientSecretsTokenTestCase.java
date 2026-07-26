/*
 * Copyright (c) 2026, WSO2 LLC (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC licenses this file to you under the Apache License,
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
package org.wso2.am.integration.tests.application;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ConsumerSecretCreationRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ConsumerSecretDeletionRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ConsumerSecretDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
public class MultipleClientSecretsTokenTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(MultipleClientSecretsTokenTestCase.class);
    private static final String APP_NAME = "MultipleSecretsTokenTestApp";

    private String applicationId;
    private String consumerKey;
    private String keyMappingId;
    private String firstAdditionalSecretId;
    private String firstAdditionalSecretValue;

    @Factory(dataProvider = "userModeDataProvider")
    public MultipleClientSecretsTokenTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] {
                new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        HttpResponse appResponse = restAPIStore.createApplication(APP_NAME,
                "Token test application for multiple client secrets",
                APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        Assert.assertEquals(appResponse.getResponseCode(), HttpStatus.SC_OK,
                "Error creating test application in setup");
        applicationId = appResponse.getData();

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO keyDTO = restAPIStore.generateKeys(applicationId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        Assert.assertNotNull(keyDTO, "Key generation failed in setup");
        consumerKey = keyDTO.getConsumerKey();
        keyMappingId = keyDTO.getKeyMappingId();
    }

    @Test(groups = {"wso2.am"}, description = "A generated additional secret can be used to obtain an access token")
    public void testTokenGenerationWithGeneratedSecret() throws Exception {
        ConsumerSecretCreationRequestDTO request = new ConsumerSecretCreationRequestDTO();
        Map<String, Object> props = new HashMap<>();
        props.put("description", "token-test-secret-1");
        request.setAdditionalProperties(props);

        ConsumerSecretDTO secretDTO;
        try {
            secretDTO = restAPIStore.generateConsumerSecret(applicationId, keyMappingId, request);
        } catch (ApiException e) {
            if (e.getCode() == HttpStatus.SC_BAD_REQUEST && e.getResponseBody() != null
                    && e.getResponseBody().contains("900916")) {
                throw new SkipException("Multiple client secrets mode is disabled in the test runtime.");
            }
            throw e;
        }

        Assert.assertNotNull(secretDTO, "Generate secret response should not be null");
        firstAdditionalSecretId = secretDTO.getSecretId();
        firstAdditionalSecretValue = secretDTO.getSecretValue();
        Assert.assertTrue(StringUtils.isNotBlank(firstAdditionalSecretId),
                "Generated secretId should not be blank");
        Assert.assertTrue(StringUtils.isNotBlank(firstAdditionalSecretValue),
                "Generated secretValue should not be blank");

        HttpResponse tokenResponse = requestTokenWithSecret(consumerKey, firstAdditionalSecretValue);
        Assert.assertEquals(tokenResponse.getResponseCode(), HttpStatus.SC_OK,
                "Token request with generated additional secret failed");
        JsonObject tokenJson = JsonParser.parseString(tokenResponse.getData()).getAsJsonObject();
        Assert.assertTrue(tokenJson.has("access_token"), "access_token missing in token response");
        Assert.assertTrue(StringUtils.isNotBlank(tokenJson.get("access_token").getAsString()),
                "access_token should not be blank");

        log.info("Successfully obtained token using generated additional secret: " + firstAdditionalSecretId);
    }

    @Test(groups = {"wso2.am"}, description = "Token generation should fail after the additional secret is revoked",
            dependsOnMethods = "testTokenGenerationWithGeneratedSecret")
    public void testTokenGenerationFailsAfterRevoke() throws Exception {
        // The IS prevents deleting the most recently added additional secret. Generate a second
        // secret first so that firstAdditionalSecretId is no longer the latest and can be revoked.
        ConsumerSecretCreationRequestDTO helperRequest = new ConsumerSecretCreationRequestDTO();
        Map<String, Object> helperProps = new HashMap<>();
        helperProps.put("description", "revoke-test-helper-secret");
        helperRequest.setAdditionalProperties(helperProps);
        restAPIStore.generateConsumerSecret(applicationId, keyMappingId, helperRequest);

        ConsumerSecretDeletionRequestDTO revokeRequest = new ConsumerSecretDeletionRequestDTO();
        revokeRequest.setSecretId(firstAdditionalSecretId);
        restAPIStore.revokeConsumerSecret(applicationId, keyMappingId, revokeRequest);

        HttpResponse tokenResponse = requestTokenWithSecret(consumerKey, firstAdditionalSecretValue);
        Assert.assertEquals(tokenResponse.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,
                "Token request should return 401 after the secret is revoked");

        log.info("Verified that token generation fails after revoking secret: " + firstAdditionalSecretId);
    }

    @Test(groups = {"wso2.am"}, description = "Two additional secrets for the same key mapping should both yield valid tokens concurrently")
    public void testBothSecretsWorkConcurrently() throws Exception {
        ConsumerSecretCreationRequestDTO request1 = new ConsumerSecretCreationRequestDTO();
        Map<String, Object> props1 = new HashMap<>();
        props1.put("description", "concurrent-secret-1");
        request1.setAdditionalProperties(props1);

        ConsumerSecretDTO secret1;
        try {
            secret1 = restAPIStore.generateConsumerSecret(applicationId, keyMappingId, request1);
        } catch (ApiException e) {
            if (e.getCode() == HttpStatus.SC_BAD_REQUEST && e.getResponseBody() != null
                    && e.getResponseBody().contains("900916")) {
                throw new SkipException("Multiple client secrets mode is disabled in the test runtime.");
            }
            throw e;
        }
        Assert.assertNotNull(secret1, "First concurrent secret generation failed");
        String secretValue1 = secret1.getSecretValue();

        ConsumerSecretCreationRequestDTO request2 = new ConsumerSecretCreationRequestDTO();
        Map<String, Object> props2 = new HashMap<>();
        props2.put("description", "concurrent-secret-2");
        request2.setAdditionalProperties(props2);

        ConsumerSecretDTO secret2;
        try {
            secret2 = restAPIStore.generateConsumerSecret(applicationId, keyMappingId, request2);
        } catch (ApiException e) {
            if (e.getCode() == HttpStatus.SC_BAD_REQUEST && e.getResponseBody() != null
                    && e.getResponseBody().contains("900916")) {
                throw new SkipException("Multiple client secrets mode is disabled in the test runtime.");
            }
            throw e;
        }
        Assert.assertNotNull(secret2, "Second concurrent secret generation failed");
        String secretValue2 = secret2.getSecretValue();

        HttpResponse token1 = requestTokenWithSecret(consumerKey, secretValue1);
        Assert.assertEquals(token1.getResponseCode(), HttpStatus.SC_OK,
                "Token request with first concurrent secret failed");
        Assert.assertTrue(
                JsonParser.parseString(token1.getData()).getAsJsonObject().has("access_token"),
                "access_token missing for first concurrent secret");

        HttpResponse token2 = requestTokenWithSecret(consumerKey, secretValue2);
        Assert.assertEquals(token2.getResponseCode(), HttpStatus.SC_OK,
                "Token request with second concurrent secret failed");
        Assert.assertTrue(
                JsonParser.parseString(token2.getData()).getAsJsonObject().has("access_token"),
                "access_token missing for second concurrent secret");

        log.info("Both concurrent secrets yielded valid tokens");
    }

    @Test(groups = {"wso2.am"}, description = "Tokens obtained with different secrets for the same key mapping carry the same sub claim",
            dependsOnMethods = "testBothSecretsWorkConcurrently")
    public void testTokensFromDifferentSecretsHaveSameApplication() throws Exception {
        ConsumerSecretCreationRequestDTO request1 = new ConsumerSecretCreationRequestDTO();
        Map<String, Object> props1 = new HashMap<>();
        props1.put("description", "identity-check-secret-1");
        request1.setAdditionalProperties(props1);
        ConsumerSecretDTO secret1;
        try {
            secret1 = restAPIStore.generateConsumerSecret(applicationId, keyMappingId, request1);
        } catch (ApiException e) {
            if (e.getCode() == HttpStatus.SC_BAD_REQUEST && e.getResponseBody() != null
                    && e.getResponseBody().contains("900916")) {
                throw new SkipException("Multiple client secrets mode is disabled in the test runtime.");
            }
            throw e;
        }
        Assert.assertNotNull(secret1, "Secret 1 generation failed");

        ConsumerSecretCreationRequestDTO request2 = new ConsumerSecretCreationRequestDTO();
        Map<String, Object> props2 = new HashMap<>();
        props2.put("description", "identity-check-secret-2");
        request2.setAdditionalProperties(props2);
        ConsumerSecretDTO secret2 = restAPIStore.generateConsumerSecret(applicationId, keyMappingId, request2);
        Assert.assertNotNull(secret2, "Secret 2 generation failed");

        HttpResponse tokenResponse1 = requestTokenWithSecret(consumerKey, secret1.getSecretValue());
        HttpResponse tokenResponse2 = requestTokenWithSecret(consumerKey, secret2.getSecretValue());
        Assert.assertEquals(tokenResponse1.getResponseCode(), HttpStatus.SC_OK, "Token request 1 failed");
        Assert.assertEquals(tokenResponse2.getResponseCode(), HttpStatus.SC_OK, "Token request 2 failed");

        String accessToken1 = JsonParser.parseString(tokenResponse1.getData()).getAsJsonObject()
                .get("access_token").getAsString();
        String accessToken2 = JsonParser.parseString(tokenResponse2.getData()).getAsJsonObject()
                .get("access_token").getAsString();

        String sub1 = extractJwtClaim(accessToken1, "sub");
        String sub2 = extractJwtClaim(accessToken2, "sub");
        if (sub1 == null || sub2 == null) {
            log.warn("sub claim could not be extracted (tokens may be opaque); skipping sub claim equality check");
            return;
        }
        Assert.assertEquals(sub1, sub2,
                "Tokens from different secrets for the same key mapping should carry the same sub claim");

        log.info("Verified both tokens share the same sub claim: " + sub1);
    }

    private HttpResponse requestTokenWithSecret(String clientId, String clientSecret) throws Exception {
        String credentials = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + credentials);
        return HTTPSClientUtils.doPost(
                new URL(getKeyManagerURLHttps() + "/oauth2/token"),
                "grant_type=client_credentials",
                headers);
    }

    private String extractJwtClaim(String jwtToken, String claimName) {
        try {
            String[] parts = jwtToken.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonObject payload = JsonParser.parseString(payloadJson).getAsJsonObject();
            return payload.has(claimName) ? payload.get(claimName).getAsString() : null;
        } catch (Exception e) {
            log.warn("Failed to decode JWT claim '" + claimName + "' from token", e);
            return null;
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (applicationId != null) {
            restAPIStore.deleteApplication(applicationId);
        }
    }
}
