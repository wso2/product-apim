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
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.JwtTestUtils;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.ResourceCleanup;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.UUID;

/**
 * Step definitions for the external-IdP self-validated-JWT arc (legacy {@code ExternalIDPJWTTestCase}): minting
 * the consumer key an external IdP's {@code azp} claim carries, hand-signing that IdP's JWTs, asserting the
 * Developer Portal's display-endpoint contract, and PUTting an existing key mapping's supported grant types.
 *
 * <p>The two external IdPs modelled here do NOT exist as servers. Each is a key manager registered with
 * {@code self_validate_jwt} and a pinned PEM certificate whose key pair is committed under
 * {@code artifacts/certs/externalidp{1,2}/}, so the gateway verifies the signature locally and never contacts the
 * IdP - exactly what the legacy test did with its {@code configFiles/idpjwt/*.jks} keystores, and why the block
 * needs no Identity Server container.
 *
 * <p><b>The issuer / alias / claim-namespace literals below MUST stay in step with the key-manager payload
 * fixtures</b> {@code artifacts/payloads/keymanagers/external-idp-{1,2}.json}: the gateway resolves the key
 * manager by the token's {@code iss}, so a drift here means the token no longer matches any key manager and
 * every invocation fails 401 with no other clue.
 */
public class ExternalIdpJwtSteps {

    /** Profile of one modelled external IdP - kept in step with artifacts/payloads/keymanagers/external-idp-N.json. */
    private enum ExternalIdp {

        IDP1("https://external-idp-1.apim.integration", "external-idp-1-audience", "http://idp.org/claims/",
                "artifacts/certs/externalidp1/idp1-key.pem", "extIdpAzp1"),
        IDP2("https://external-idp-2.apim.integration", "external-idp-2-audience", "http://idp2.org/claims/",
                "artifacts/certs/externalidp2/idp2-key.pem", "extIdpAzp2");

        private final String issuer;
        private final String alias;
        private final String claimNamespace;
        private final String privateKeyResource;
        private final String azpContextKey;

        ExternalIdp(String issuer, String alias, String claimNamespace, String privateKeyResource,
                    String azpContextKey) {
            this.issuer = issuer;
            this.alias = alias;
            this.claimNamespace = claimNamespace;
            this.privateKeyResource = privateKeyResource;
            this.azpContextKey = azpContextKey;
        }

        static ExternalIdp of(String ref) {
            for (ExternalIdp idp : values()) {
                if (idp.name().equalsIgnoreCase(ref)) {
                    return idp;
                }
            }
            throw new IllegalArgumentException("Unknown external IdP reference '" + ref
                    + "'; expected one of idp1 / idp2");
        }

        ExternalIdp other() {
            return this == IDP1 ? IDP2 : IDP1;
        }
    }

    /**
     * Mints the consumer key/secret pair an external IdP's tokens will carry as {@code azp}. The external IdP is
     * not a live server, so there is no client to register - the pair is only an identifier the key manager's
     * key MAPPING binds to an application (the legacy test used a bare {@code UUID.randomUUID()} the same way).
     * <p>Stored under {@code <idKey>ClientId} / {@code <idKey>ClientSecret} - the SAME context contract the
     * DCR-registered BYO client uses - so the existing {@code I map OAuth client … via key manager …} step maps
     * it unchanged; and additionally under the plain {@code <idKey>}, which is what the JWT's {@code azp} claim
     * carries and what the per-tenant fixture stash keys on.
     */
    @When("I mint an external IdP consumer key as {string}")
    public void iMintExternalIdpConsumerKey(String idKey) {

        String consumerKey = UUID.randomUUID().toString();
        TestContext.set(idKey, consumerKey);
        TestContext.set(idKey + "ClientId", consumerKey);
        TestContext.set(idKey + "ClientSecret", UUID.randomUUID().toString());
    }

    /**
     * Hand-signs an RS256 JWT as the named external IdP for the consumer key MAPPED to the fixture application,
     * and publishes it under BOTH {@code generatedAccessToken} (so the shared gateway-invoke steps use it as the
     * bearer token, the DIRECT route) and {@code subjectToken} (so the shared token-exchange steps exchange it,
     * the EXCHANGED route) - one token, both routes, which is exactly the property a {@code tokenType=BOTH} key
     * manager is meant to have.
     *
     * <p>Claims mirror the legacy test's attribute map: the three claims the key manager's {@code claimMapping}
     * translates into the local dialect, plus an unmapped {@code mobileno} and an unmapped {@code department} in
     * the IdP's own namespace. Every token gets a fresh {@code jti} so the gateway's token cache can never serve
     * a verdict reached under a previous {@code tokenType}.
     */
    @When("I obtain a self-signed JWT from external IdP {string} for the mapped consumer key")
    public void iObtainJwtForMappedKey(String idpRef) throws Exception {

        ExternalIdp idp = ExternalIdp.of(idpRef);
        publishJwt(idp, idp, TestContext.resolve(idp.azpContextKey).toString());
    }

    /**
     * As above but with an {@code azp} that is NOT mapped to any application - a validly-signed token from a
     * trusted issuer whose authorized party the key manager cannot resolve. Drives the unknown-{@code azp}
     * subscription-validation negative (403 / 900908).
     */
    @When("I obtain a self-signed JWT from external IdP {string} for an unmapped consumer key")
    public void iObtainJwtForUnmappedKey(String idpRef) throws Exception {

        ExternalIdp idp = ExternalIdp.of(idpRef);
        publishJwt(idp, idp, UUID.randomUUID().toString());
    }

    /**
     * As above but SIGNED WITH THE OTHER IdP's key while still claiming this IdP's {@code iss} - i.e. an
     * untrusted signer for the key manager the issuer resolves to. Distinct from the tampered-payload negative
     * (which mutates a claim of an otherwise legitimately-signed token): here the whole signature was produced by
     * a key pair the key manager's pinned certificate does not correspond to.
     */
    @When("I obtain a self-signed JWT from external IdP {string} signed by the other IdP key")
    public void iObtainJwtSignedByOtherKey(String idpRef) throws Exception {

        ExternalIdp idp = ExternalIdp.of(idpRef);
        publishJwt(idp, idp.other(), TestContext.resolve(idp.azpContextKey).toString());
    }

    /**
     * Assembles and signs one external-IdP JWT and publishes it for both the invoke and the exchange families.
     *
     * <p><b>The {@code kid} header is MANDATORY, not decoration.</b> The gateway's
     * {@code JWTValidatorImpl#validateSignature} only consults the key manager's configured signer (its PEM
     * certificate or JWKS endpoint) when the JWT header carries a key id; with no {@code kid} it falls straight
     * through to verifying against the gateway's OWN certificate alias, so ANY externally-signed token is refused
     * 401/900901 no matter how the key manager is configured. That would silently invalidate every assertion here
     * - the positive ones would fail, and the untrusted-signer negative would pass for the wrong reason (never
     * reaching the signature check at all). The value itself is not matched against the certificate on the PEM
     * path, so the IdP's own name is used to keep it traceable.
     */
    private void publishJwt(ExternalIdp claimingIdp, ExternalIdp signingIdp, String azp) throws Exception {

        long now = System.currentTimeMillis() / 1000L;
        String namespace = claimingIdp.claimNamespace;
        JSONObject claims = new JSONObject()
                .put("iss", claimingIdp.issuer)
                .put("sub", "userexternal")
                .put("aud", claimingIdp.alias)
                .put("azp", azp)
                .put("jti", UUID.randomUUID().toString())
                .put("scope", "default")
                .put(namespace + "givenname", "first")
                .put(namespace + "firstname", "last")
                .put(namespace + "email", "first@gmail.com")
                .put(namespace + "mobileno", "424479772294778")
                .put(namespace + "department", "platform")
                .put("iat", now).put("nbf", now).put("exp", now + 900);
        String header = new JSONObject()
                .put("alg", "RS256")
                .put("typ", "JWT")
                .put("kid", claimingIdp.name().toLowerCase())
                .toString();
        String jwt = JwtTestUtils.buildRs256Jwt(header, claims.toString(),
                JwtTestUtils.rsaPrivateKeyFromPem(Utils.readClasspathResource(signingIdp.privateKeyResource)));
        TestContext.set("generatedAccessToken", jwt);
        TestContext.set("subjectToken", jwt);
    }

    /**
     * Asserts what the Developer Portal key-manager listing ADVERTISES as a key manager's token and revoke
     * endpoints. The devportal DTO does not echo the stored endpoints verbatim: it prefers
     * {@code display_token_endpoint} / {@code display_revoke_endpoint} when they are present and non-blank and
     * otherwise falls back to the real {@code token_endpoint} / {@code revoke_endpoint}
     * (store {@code KeyManagerMappingUtil}). Both branches of that contract are asserted by this one step, on the
     * entry matching the given name - never on the whole list, which also carries the Resident Key Manager and
     * any sibling scenario's key managers.
     *
     * @param kmName        key-manager name (resolves {@code {{...}}})
     * @param tokenEndpoint the exact endpoint the listing must advertise (resolves {@code {{...}}})
     * @param revokeEndpoint the exact revoke endpoint the listing must advertise
     */
    @Then("the devportal key manager {string} should advertise token endpoint {string} and revoke endpoint {string}")
    public void theDevportalKeyManagerShouldAdvertise(String kmName, String tokenEndpoint, String revokeEndpoint) {

        String name = Utils.resolveContextPlaceholders(kmName);
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response != null && response.getResponseCode() == 200 && response.getData() != null
                        && !response.getData().isBlank(),
                "Expected a 200 devportal key-manager listing with a body, got: "
                        + (response == null ? "null" : response.getResponseCode() + " / " + response.getData()));
        JSONArray list = new JSONObject(response.getData()).optJSONArray("list");
        Assert.assertNotNull(list, "Devportal key-manager listing has no 'list' array: " + response.getData());
        JSONObject match = null;
        for (int i = 0; i < list.length(); i++) {
            if (name.equals(list.getJSONObject(i).optString("name"))) {
                match = list.getJSONObject(i);
                break;
            }
        }
        Assert.assertNotNull(match, "Key manager '" + name + "' is not in the devportal key-manager listing: "
                + response.getData());
        Assert.assertEquals(match.optString("tokenEndpoint"), Utils.resolveContextPlaceholders(tokenEndpoint),
                "Devportal listing advertises the wrong token endpoint for '" + name + "': " + match);
        Assert.assertEquals(match.optString("revokeEndpoint"), Utils.resolveContextPlaceholders(revokeEndpoint),
                "Devportal listing advertises the wrong revoke endpoint for '" + name + "': " + match);
    }

    /**
     * Replaces the supported grant types of an EXISTING key mapping in place (GET
     * {@code applications/{id}/oauth-keys/{keyMappingId}} → mutate → PUT), the round trip the legacy test drove
     * through {@code updateApplicationKeyByKeyMappingId}. {@code additionalProperties} is cleared before the PUT
     * as the legacy did: it comes back from the GET as the key manager's stored connector properties, which the
     * update endpoint rejects when echoed verbatim.
     *
     * <p>Non-asserting - the feature asserts the status and the returned grant list, before and after.
     *
     * @param keyMappingIdKey context key holding the key-mapping id
     * @param appIdKey        context key holding the application id
     * @param grantTypes      comma-separated grant types to set
     */
    @When("I update the supported grant types of key mapping {string} on application {string} to {string}")
    public void iUpdateKeyMappingGrantTypes(String keyMappingIdKey, String appIdKey, String grantTypes)
            throws IOException {

        String keyMappingId = TestContext.resolve(keyMappingIdKey).toString();
        String appId = TestContext.resolve(appIdKey).toString();
        String url = Utils.getOAuthKeyURL(Utils.getBaseUrl(), appId, keyMappingId);
        HttpResponse current = SimpleHTTPClient.getInstance().doGet(url, Identity.devportalHeaders());
        // Intermediate GET of a GET→mutate→PUT: confirm a 2xx response WITH a body before parsing, so a
        // failed/empty fetch fails clearly instead of throwing an opaque JSONException/NPE.
        Assert.assertTrue(current != null && current.getResponseCode() >= 200 && current.getResponseCode() < 300
                        && current.getData() != null && !current.getData().isBlank(),
                "Failed to fetch key mapping '" + keyMappingId + "' of application '" + appId
                        + "' before updating its grant types: expected a 2xx response with a body, got "
                        + (current == null ? "no response" : current.getResponseCode() + " / body="
                        + current.getData()));

        JSONObject keyMapping = new JSONObject(current.getData());
        JSONArray grants = new JSONArray();
        for (String grant : grantTypes.split("\\s*,\\s*")) {
            grants.put(grant);
        }
        keyMapping.put("supportedGrantTypes", grants);
        keyMapping.remove("additionalProperties");

        Requests.put(url, Identity.devportalHeaders(), keyMapping.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    @When("I register key mapping {string} on application {string} for cleanup restoration")
    public void iRegisterKeyMappingForRestoration(String keyMappingIdKey, String appIdKey) throws IOException {
        String keyMappingId = TestContext.resolve(keyMappingIdKey).toString();
        String appId = TestContext.resolve(appIdKey).toString();
        HttpResponse current = SimpleHTTPClient.getInstance().doGet(
                Utils.getOAuthKeyURL(Utils.getBaseUrl(), appId, keyMappingId), Identity.devportalHeaders());
        Assert.assertTrue(current != null && current.getResponseCode() >= 200 && current.getResponseCode() < 300
                        && current.getData() != null && !current.getData().isBlank(),
                "Failed to capture key mapping '" + keyMappingId + "' on application '" + appId
                        + "' for cleanup restoration: got=" + (current == null ? "null"
                        : current.getResponseCode() + "/" + current.getData()));
        JSONObject payload = new JSONObject(current.getData());
        payload.remove("additionalProperties");
        ResourceCleanup.registerKeyMapping(appId, keyMappingId, payload.toString());
    }
}
