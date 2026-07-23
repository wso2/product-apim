/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.IntegrationActors;
import org.wso2.am.integration.cucumbertests.utils.Names;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.JwtTestUtils;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.TokenExchangeProvisioner;
import org.wso2.am.integration.cucumbertests.utils.TokenExchangeProvisioner.IdpScope;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;

/**
 * Steps for the RFC 8693 token-exchange block: provisions the IS subject-token app and the APIM trusted IdP
 * (PEM / JWKS signature-validation shapes), mints an IS subject JWT, exchanges it at APIM's own token endpoint
 * for an APIM access token, and drives the id_token/{@code enforce_type_header_validation} negative. Runtime
 * token acquisition/exchange results are published to {@code generatedAccessToken}/{@code httpResponse} so the
 * shared gateway-invoke and status-assertion steps work unchanged.
 */
public class TokenExchangeSteps {

    private static final String TOKEN_EXCHANGE_GRANT = "urn:ietf:params:oauth:grant-type:token-exchange";
    private static final String JWT_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:jwt";

    /**
     * Fixture context keys that are per-tenant: the setup stashes them under a tenant-suffixed key for each
     * tenant, and the select-fixture step copies the acting tenant's values back into these plain keys so the
     * exchange / invoke / id_token steps read them unchanged.
     */
    private static final String[] FIXTURE_KEYS =
            {"apiContext", "consumerKey", "consumerSecret", "txNoGrantKey", "txNoGrantSecret"};

    /** IdP scope (tenant SOAP path + admin creds) for the currently-acting actor's tenant. */
    private static IdpScope actingIdpScope() {
        User actor = Identity.actingActor();
        return IdpScope.of(Identity.actingTenantDomain(), actor.getUserName(), actor.getPassword());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Provisioning (setup feature)
    // ---------------------------------------------------------------------------------------------------------

    /** Creates the IS OIDC app (client_credentials, JWT access tokens) whose token is exchanged; stores its creds. */
    @When("I provision the token-exchange subject application on the identity server")
    public void iProvisionSubjectApp() throws Exception {
        String[] creds = TokenExchangeProvisioner.createIsJwtClientCredentialsApp(Names.unique("TokenExchangeSubjectApp"));
        TestContext.set("txIsClientId", creds[0]);
        TestContext.set("txIsClientSecret", creds[1]);
    }

    /** Registers the APIM trusted IdP (in the acting tenant) validating the subject token via a static PEM cert. */
    @When("I register the token-exchange trusted identity provider {string} using PEM certificate validation")
    public void iRegisterTrustedIdpPem(String idpName) throws Exception {
        TokenExchangeProvisioner.registerTrustedIdpPem(actingIdpScope(), idpName,
                TestContext.resolve("txIsClientId").toString());
        TestContext.set("txIdpName", idpName);
    }

    /** Registers the APIM trusted IdP (in the acting tenant) validating the subject token via IS's JWKS endpoint. */
    @When("I register the token-exchange trusted identity provider {string} using JWKS validation")
    public void iRegisterTrustedIdpJwks(String idpName) throws Exception {
        TokenExchangeProvisioner.registerTrustedIdpJwks(actingIdpScope(), idpName,
                TestContext.resolve("txIsClientId").toString());
        TestContext.set("txIdpName", idpName);
    }

    /**
     * Registers the PEM trusted IdP pinned to a certificate of a different key pair than IS's current signer -
     * modelling the post-key-rotation state where the pinned cert has gone stale (the key-rotation canary).
     */
    @When("I register the token-exchange trusted identity provider {string} using a stale PEM certificate")
    public void iRegisterTrustedIdpStalePem(String idpName) throws Exception {
        TokenExchangeProvisioner.registerTrustedIdpStalePem(actingIdpScope(), idpName,
                TestContext.resolve("txIsClientId").toString());
        TestContext.set("txIdpName", idpName);
    }

    /**
     * Attempts to register the trusted IdP with a certificate field that is not an X.509 certificate at all
     * (e.g. a pasted keystore blob) - the wrong-format-certificate pitfall. Non-asserting; the following step
     * pins that APIM refuses the registration (the IdP is never created).
     */
    @When("I attempt to register the token-exchange trusted identity provider {string} using a malformed certificate")
    public void iAttemptRegisterTrustedIdpMalformedCert(String idpName) throws Exception {
        TokenExchangeProvisioner.attemptRegisterTrustedIdpMalformedCert(actingIdpScope(), idpName,
                TestContext.resolve("txIsClientId").toString());
    }

    /** Asserts the named trusted IdP does NOT exist (the malformed-certificate registration was refused). */
    @Then("the token-exchange trusted identity provider {string} should not exist")
    public void theTrustedIdpShouldNotExist(String idpName) throws Exception {
        Assert.assertFalse(TokenExchangeProvisioner.trustedIdpExists(actingIdpScope(), idpName),
                "Expected APIM to refuse the malformed-certificate IdP registration, but '" + idpName
                        + "' was created");
    }

    /**
     * Stashes the current per-tenant fixture (apiContext, application credentials, grantless credentials) under
     * a key suffixed by the acting tenant, so the setup can provision both tenants and each scenario later
     * selects its tenant's fixture. Run after each tenant's provisioning in the setup.
     */
    @When("I stash the token-exchange fixture for the acting tenant")
    public void iStashFixtureForActingTenant() {
        String tenant = Identity.actingTenantDomain();
        for (String key : FIXTURE_KEYS) {
            Object v = TestContext.get(key);
            if (v != null) {
                TestContext.set(key + "::" + tenant, v);
            }
        }
    }

    /**
     * Selects the acting tenant's previously-stashed fixture: copies its tenant-suffixed values back into the
     * plain fixture keys so the unchanged exchange / invoke / id_token steps operate on this tenant's
     * application and API.
     */
    @When("I use the token-exchange fixture for the acting tenant")
    public void iUseFixtureForActingTenant() {
        String tenant = Identity.actingTenantDomain();
        for (String key : FIXTURE_KEYS) {
            Object v = TestContext.get(key + "::" + tenant);
            if (v != null) {
                TestContext.set(key, v);
            }
        }
    }

    /** Removes the trusted IdP (in the acting tenant) so the subject token's issuer is untrusted. */
    @When("I remove the token-exchange trusted identity provider {string}")
    public void iRemoveTrustedIdp(String idpName) throws Exception {
        TokenExchangeProvisioner.deleteIdp(actingIdpScope(), idpName);
    }

    /**
     * Stashes the CURRENT application's Resident-KM credentials (just generated) under grantless-app keys, so a
     * later scenario can attempt the exchange as an application whose keys do NOT include the token-exchange
     * grant. Called in the setup between the grantless app's key generation and the main app's.
     */
    @When("I remember the current client credentials as the token-exchange grantless application")
    public void iRememberGrantlessCreds() {
        TestContext.set("txNoGrantKey", TestContext.resolve("consumerKey"));
        TestContext.set("txNoGrantSecret", TestContext.resolve("consumerSecret"));
    }

    /** Attempts the exchange authenticated as the application WITHOUT the token-exchange grant. */
    @When("I attempt a token exchange using the grantless application credentials")
    public void iExchangeAsGrantlessApp() throws Exception {
        exchange(TestContext.resolve("subjectToken").toString(), JWT_TOKEN_TYPE,
                TestContext.resolve("txNoGrantKey").toString(),
                TestContext.resolve("txNoGrantSecret").toString());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Runtime
    // ---------------------------------------------------------------------------------------------------------

    /** Mints a JWT subject token from IS via client_credentials (IS app creds); stores it under {@code subjectToken}. */
    @When("I obtain a subject JWT from the identity server")
    public void iObtainSubjectJwt() throws Exception {
        String cid = TestContext.resolve("txIsClientId").toString();
        String csec = TestContext.resolve("txIsClientSecret").toString();
        HttpResponse resp = SimpleHTTPClient.getInstance().doPost(IntegrationActors.tokenEndpoint(IntegrationActors.IS), Identity.basicAuthHeaders(cid, csec),
                "grant_type=client_credentials&scope=openid",
                Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);
        Assert.assertTrue(resp != null && resp.getResponseCode() == 200 && resp.getData() != null
                        && !resp.getData().isBlank(),
                "IS subject-token request failed: got=" + (resp == null ? "null"
                        : resp.getResponseCode() + "/" + resp.getData()));
        TestContext.set("subjectToken", new JSONObject(resp.getData()).getString("access_token"));
    }

    /**
     * Exchanges the stored subject token at APIM's own token endpoint (authenticated with the application's
     * Resident-KM client credentials) for an APIM access token. Publishes the response as {@code httpResponse}
     * and the issued token as {@code generatedAccessToken}. When {@code tamperSubject} is true the subject token
     * is corrupted first (signature no longer matches) to drive the invalid-signature negative.
     */
    @When("I exchange the subject token at the API Manager token endpoint")
    public void iExchangeSubjectToken() throws Exception {
        exchange(TestContext.resolve("subjectToken").toString(), JWT_TOKEN_TYPE);
    }

    /** As the exchange step but tampering the subject token first (flips a payload byte, keeping the signature). */
    @When("I exchange a tampered subject token at the API Manager token endpoint")
    public void iExchangeTamperedSubjectToken() throws Exception {
        exchange(JwtTestUtils.tamperClaim(TestContext.resolve("subjectToken").toString(), "sub"), JWT_TOKEN_TYPE);
    }

    /** Attempts the exchange with a missing/blank subject token (malformed-request negative). */
    @When("I attempt a token exchange with no subject token")
    public void iExchangeNoSubjectToken() throws Exception {
        exchangeForm("grant_type=" + Utils.urlEncode(TOKEN_EXCHANGE_GRANT)
                + "&requested_token_type=" + Utils.urlEncode(JWT_TOKEN_TYPE)
                + "&subject_token_type=" + Utils.urlEncode(JWT_TOKEN_TYPE));
    }

    /**
     * Attempts the token exchange with a placeholder subject token. Used by the grant-disabled block: the token
     * endpoint rejects the disabled grant at dispatch (before the subject token is processed), so the placeholder
     * is never parsed and the response is {@code unsupported_grant_type}.
     */
    @When("I attempt a token exchange with a placeholder subject token")
    public void iExchangePlaceholderSubjectToken() throws Exception {
        exchange("placeholder.subject.token", JWT_TOKEN_TYPE);
    }

    private void exchange(String subjectToken, String subjectTokenType) throws IOException {
        exchange(subjectToken, subjectTokenType, TestContext.resolve("consumerKey").toString(),
                TestContext.resolve("consumerSecret").toString());
    }

    /** As {@link #exchange(String, String)} but authenticated with explicit client credentials. */
    private void exchange(String subjectToken, String subjectTokenType, String clientKey, String clientSecret)
            throws IOException {
        exchangeForm("grant_type=" + Utils.urlEncode(TOKEN_EXCHANGE_GRANT)
                + "&requested_token_type=" + Utils.urlEncode(JWT_TOKEN_TYPE)
                + "&subject_token_type=" + Utils.urlEncode(subjectTokenType)
                + "&subject_token=" + Utils.urlEncode(subjectToken), clientKey, clientSecret);
    }

    private void exchangeForm(String body) throws IOException {
        exchangeForm(body, TestContext.resolve("consumerKey").toString(),
                TestContext.resolve("consumerSecret").toString());
    }

    private void exchangeForm(String body, String clientKey, String clientSecret) throws IOException {
        HttpResponse resp = Requests.post(Utils.getAPIMTokenEndpointURL(Utils.getBaseUrl()),
                Identity.basicAuthHeaders(clientKey, clientSecret), body,
                Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);
        if (resp != null && resp.getResponseCode() == 200 && resp.getData() != null
                && !resp.getData().isBlank()) {
            JSONObject json = new JSONObject(resp.getData());
            if (json.has("access_token")) {
                TestContext.set("generatedAccessToken", json.getString("access_token"));
            }
        }
    }

    /**
     * Obtains an OIDC id_token (JWT with no {@code at+jwt} type header) from the application's Resident-KM client
     * via the password grant, and stores it as {@code generatedAccessToken} so the gateway-invoke step can prove
     * that - with {@code enforce_type_header_validation} on - an id_token is refused (HTTP 401 / 900905), unlike
     * the {@code at+jwt} token the exchange issues.
     */
    @When("I obtain an id_token for the application as the acting user")
    public void iObtainIdToken() throws Exception {
        User actor = Identity.actingActor();
        String consumerKey = TestContext.resolve("consumerKey").toString();
        String consumerSecret = TestContext.resolve("consumerSecret").toString();
        String body = "grant_type=password&username=" + Utils.urlEncode(actor.getUserName())
                + "&password=" + Utils.urlEncode(actor.getPassword()) + "&scope=" + Utils.urlEncode("openid");
        HttpResponse resp = SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(Utils.getBaseUrl()),
                Identity.basicAuthHeaders(consumerKey, consumerSecret), body,
                Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);
        Assert.assertTrue(resp != null && resp.getResponseCode() == 200 && resp.getData() != null
                        && !resp.getData().isBlank(),
                "Password-grant id_token request failed: got=" + (resp == null ? "null"
                        : resp.getResponseCode() + "/" + resp.getData()));
        JSONObject json = new JSONObject(resp.getData());
        Assert.assertTrue(json.has("id_token"), "No id_token issued (needs the password grant + openid scope): "
                + resp.getData());
        TestContext.set("generatedAccessToken", json.getString("id_token"));
    }

    /**
     * Mints a subject JWT from a short-lived IS app and waits (bounded, using the token's own {@code exp}) until
     * it has expired, storing it under {@code subjectToken} so the exchange step can prove an expired subject
     * token is refused. The wait is on a known, imminent event (the token's expiry), not a fixed readiness sleep.
     */
    @When("I obtain an expired subject JWT from the identity server")
    public void iObtainExpiredSubjectJwt() throws Exception {
        // Name it per acting tenant: the external IS is shared, so the super and tenant1 rows must not collide.
        String[] creds = TokenExchangeProvisioner.createIsJwtClientCredentialsApp(
                Names.unique("TokenExchangeExpiredApp-" + Identity.actingTenantDomain()), 5);
        HttpResponse resp = SimpleHTTPClient.getInstance().doPost(IntegrationActors.tokenEndpoint(IntegrationActors.IS), Identity.basicAuthHeaders(creds[0], creds[1]),
                "grant_type=client_credentials&scope=openid",
                Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);
        Assert.assertTrue(resp != null && resp.getResponseCode() == 200 && resp.getData() != null
                        && !resp.getData().isBlank(),
                "IS short-lived subject-token request failed: got=" + (resp == null ? "null"
                        : resp.getResponseCode() + "/" + resp.getData()));
        String token = new JSONObject(resp.getData()).getString("access_token");
        long expEpochMs = jwtExpEpochMillis(token);
        long deadlineStart = System.currentTimeMillis();
        long deadline = deadlineStart + 30_000;
        while (System.currentTimeMillis() <= expEpochMs + 1000 && System.currentTimeMillis() < deadline) {
            Utils.pollPause(deadlineStart, 500);
        }
        TestContext.set("subjectToken", token);
    }

    /** Asserts the exchanged access token's subject is the IS subject application (federated identity carried). */
    @Then("the exchanged access token subject should match the identity server subject application")
    public void theExchangedSubjectMatchesIsApp() {
        String token = TestContext.resolve("generatedAccessToken").toString();
        String payloadJson = JwtTestUtils.decodePayload(token);
        String sub = new JSONObject(payloadJson).optString("sub");
        Assert.assertEquals(sub, TestContext.resolve("txIsClientId").toString(),
                "Exchanged token subject should carry the IS subject app's identity. Payload: " + payloadJson);
    }

    /** Asserts the stored {@code generatedAccessToken} JWT header type equals {@code at+jwt} (RFC 9068). */
    @Then("the generated access token should have the {string} type header")
    public void theTokenShouldHaveTypeHeader(String expectedType) {
        String token = TestContext.resolve("generatedAccessToken").toString();
        String headerJson = JwtTestUtils.decodeHeader(token);
        Assert.assertEquals(new JSONObject(headerJson).optString("typ"), expectedType,
                "Unexpected JWT type header. Header: " + headerJson);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------

    /** Reads the {@code exp} claim (epoch seconds) of a JWT and returns it in epoch millis. */
    private static long jwtExpEpochMillis(String jwt) {
        return new JSONObject(JwtTestUtils.decodePayload(jwt)).getLong("exp") * 1000L;
    }
}
