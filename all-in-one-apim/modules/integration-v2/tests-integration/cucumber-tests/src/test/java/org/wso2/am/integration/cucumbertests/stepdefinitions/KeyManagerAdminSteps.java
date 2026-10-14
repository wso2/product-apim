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
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.ResourceCleanup;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Step definitions for application/key-manager admin flows not previously in integration-v2:
 * OAuth2 token introspection ({@code /oauth2/introspect}), application-level OAuth revocation via the Carbon
 * {@code OAuthAdminService} SOAP admin service ({@code updateConsumerAppState}), and the admin change-owner
 * operation ({@code POST api/am/admin/v4/applications/{id}/change-owner?owner=…}). Kept in this NEW class so the
 * shared step classes are untouched (a sibling agent holds hunks in ApplicationBaseSteps).
 */
public class KeyManagerAdminSteps {

    private final BaseSteps baseSteps = new BaseSteps();

    /**
     * The username as the APIM admin/consumer APIs expect it: a super-tenant user is passed UNQUALIFIED
     * ({@code subscriberUser1}, not {@code subscriberUser1@carbon.super}), while a tenant user keeps its full
     * {@code user@tenant} form. Sending the {@code @carbon.super}-qualified name makes the change-owner /
     * owner-search user-store lookup fail with "User … doesn't exist in user store" (HTTP 500). Same convention
     * as PublisherBaseSteps' provider-field comparison.
     */
    private static String apiUsername(User actor) {
        String username = actor.getUserName();
        String superSuffix = Constants.CHAR_AT + Constants.SUPER_TENANT_DOMAIN;
        if (username.endsWith(superSuffix)) {
            return username.substring(0, username.length() - superSuffix.length());
        }
        return username;
    }

    /**
     * Introspects an OAuth2 access token (resolved from a context key holding the token) at the
     * {@code /oauth2/introspect} endpoint, authenticating with the acting actor's carbon credentials. Publishes
     * the introspection response for assertion (the feature checks {@code active} and {@code client_id}).
     * <p>
     * The context value may be either a bare token or a whole {@code Authorization} header VALUE
     * ({@code Bearer <token>}) — the {@code Bearer } prefix is stripped. That lets the endpoint-security
     * scenarios introspect the backend token the gateway MINTED and injected, which reaches the test only as the
     * Authorization header echoed verbatim by the {@code /sec} backend route.
     */
    @When("I introspect the access token {string}")
    public void iIntrospectTheAccessToken(String tokenKey) throws IOException {
        String token = TestContext.resolve(tokenKey).toString().trim();
        if (token.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            token = token.substring("Bearer ".length()).trim();
        }
        // Acting actor's own carbon credentials: introspection is a Basic-auth (not bearer) endpoint.
        Map<String, String> headers = Identity.actingBasicAuthHeaders();
        // A tenant token must be introspected at the tenant-qualified path; the super path 401s a tenant caller.
        Requests.post(Utils.getIntrospectEndpointURL(Utils.getBaseUrl(), Identity.actingTenantDomain()), headers,
                "token=" + Utils.urlEncode(token), "application/x-www-form-urlencoded");
    }

    /**
     * Revokes an application's OAuth state by setting the consumer app's state to the given value (e.g.
     * {@code REVOKED}) via the {@code OAuthAdminService.updateConsumerAppState} SOAP operation, authenticated as
     * the acting actor. The consumer key is read from a context key (e.g. {@code consumerKey} set by the
     * key-generation step). Publishes the SOAP response; a 2xx envelope means the state change was accepted.
     */
    @When("I revoke the OAuth application with consumer key {string} by setting its state to {string}")
    public void iRevokeTheOAuthApplicationState(String consumerKeyRef, String newState) throws IOException {
        String consumerKey = TestContext.resolve(consumerKeyRef).toString();
        User actor = Identity.actingActor();
        String ns = "http://org.apache.axis2/xsd";
        String envelope = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:xsd=\"" + ns + "\"><soapenv:Header/><soapenv:Body>"
                + "<xsd:updateConsumerAppState><xsd:consumerKey>" + Utils.escapeXml(consumerKey) + "</xsd:consumerKey>"
                + "<xsd:newState>" + Utils.escapeXml(newState) + "</xsd:newState>"
                + "</xsd:updateConsumerAppState></soapenv:Body></soapenv:Envelope>";
        Requests.soap(Utils.getOAuthAdminServiceURL(Utils.getBaseUrl()), envelope, "urn:updateConsumerAppState",
                actor.getUserName(), actor.getPassword());
    }

    /**
     * Admin change-owner: transfers an application (context key holding the app id) to a new owner (an actor
     * reference, resolved to its full username) via {@code POST /applications/{id}/change-owner?owner=…} with the
     * acting admin's token. Non-asserting — the feature asserts the exact status (200 valid; 404/500 negatives).
     */
    @When("I change the owner of application {string} to {string}")
    public void iChangeTheOwnerOfApplication(String appIdRef, String newOwnerRef) throws IOException {
        String appId = TestContext.resolve(appIdRef).toString();
        String newOwner = apiUsername(Identity.resolveActor(newOwnerRef));
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = Requests.post(Utils.getChangeApplicationOwnerURL(Utils.getBaseUrl(), appId, newOwner),
                headers, "", null);
        // On a SUCCESSFUL transfer the application now belongs to the new owner, so teardown must delete it AS the
        // new owner (CLAUDE.md §5): the DevPortal delete is scoped to the requesting subscriber, so sweeping it with
        // the original creator's token would 404 and leak the application while looking like an already-gone id.
        // 200 exactly: change-owner declares only 200/400/404, so a 2xx range check would accept a code the
        // contract does not define. Null-guarded like every other deregister site.
        if (response != null && response.getResponseCode() == 200) {
            ResourceCleanup.deregister(Constants.CREATED_APPLICATION_IDS, appId);
            ResourceCleanup.registerFor(Constants.CREATED_APPLICATION_IDS, appId, newOwnerRef);
        }
    }

    /**
     * Admin change-owner to a RAW username string (not an actor reference) — for the negative "non-existent owner"
     * case where the target user does not exist as a provisioned actor. {@code ${UNIQUE:...}} and {@code {{...}}}
     * placeholders are resolved so a uniquified name can be passed. Non-asserting.
     */
    @When("I change the owner of application {string} to the raw user {string}")
    public void iChangeTheOwnerOfApplicationRaw(String appIdRef, String rawOwner) throws IOException {
        String appId = TestContext.resolve(appIdRef).toString();
        String owner = Utils.resolveContextPlaceholders(Utils.resolvePayloadPlaceholders(rawOwner));
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        Requests.post(Utils.getChangeApplicationOwnerURL(Utils.getBaseUrl(), appId, owner), headers, "", null);
    }

    /**
     * Lists applications owned by the given owner (actor reference) via the admin API, publishing the response so
     * the feature can assert the transferred application now appears under the new owner.
     */
    @When("I retrieve the admin applications owned by {string}")
    public void iRetrieveAdminApplicationsOwnedBy(String ownerRef) throws IOException {
        String owner = apiUsername(Identity.resolveActor(ownerRef));
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        Requests.get(Utils.getAdminApplicationsByOwnerURL(Utils.getBaseUrl(), owner), headers);
    }

    /**
     * Requests a {@code client_credentials} token directly at {@code /oauth2/token}, authenticating with a given
     * consumer key and secret VALUE (both resolved from context keys). Used to prove an ADDITIONAL client secret
     * yields a valid token (and, across two secrets, that both work and carry the same application identity).
     * Publishes the token response for assertion.
     */
    @When("I request a client-credentials token using consumer key {string} and secret {string}")
    public void iRequestClientCredentialsToken(String consumerKeyRef, String secretRef) throws IOException {
        requestClientCredentialsToken(consumerKeyRef, secretRef, "");
    }

    /**
     * As the client-credentials step, but requesting an explicit scope. The key manager caches an issued token per
     * (client, scope) pair and answers a repeat request with the SAME token, so asking for a distinct scope is the
     * only way to force a genuinely NEW token out of a client that already holds one — which is what proves a
     * gateway verdict was cached against the earlier token rather than against the client itself.
     */
    @When("I request a client-credentials token using consumer key {string} and secret {string} with scope {string}")
    public void iRequestClientCredentialsTokenWithScope(String consumerKeyRef, String secretRef, String scope)
            throws IOException {
        requestClientCredentialsToken(consumerKeyRef, secretRef, scope);
    }

    private void requestClientCredentialsToken(String consumerKeyRef, String secretRef, String scope)
            throws IOException {
        String consumerKey = TestContext.resolve(consumerKeyRef).toString();
        String secret = TestContext.resolve(secretRef).toString();
        String creds = Base64.getEncoder().encodeToString(
                (consumerKey + ":" + secret).getBytes(StandardCharsets.UTF_8));
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic " + creds);
        String body = "grant_type=client_credentials";
        if (scope != null && !scope.isEmpty()) {
            body += "&scope=" + Utils.urlEncode(Utils.resolveContextPlaceholders(scope));
        }
        Requests.post(Utils.getAPIMTokenEndpointURL(Utils.getBaseUrl()), headers, body,
                "application/x-www-form-urlencoded");
    }

    /**
     * Switches an application's OAuth client from JWT to opaque-token issuance through the Carbon
     * {@code OAuthAdminService}. The DevPortal application's {@code tokenType} is a separate representation and is
     * not changed by this operation. Read the current DTO first because {@code updateConsumerApplication} replaces
     * it; preserve its fields and change only {@code tokenType}.
     */
    @When("I switch the OAuth client with consumer key {string} to opaque token issuance")
    public void iSwitchOAuthClientToOpaqueTokenIssuance(String consumerKeyRef) throws IOException {
        String consumerKey = TestContext.resolve(consumerKeyRef).toString();
        User actor = Identity.actingActor();
        String serviceUrl = Utils.getOAuthAdminServiceURL(Utils.getBaseUrl());
        String axisNs = "http://org.apache.axis2/xsd";
        String dtoNs = "http://dto.oauth.identity.carbon.wso2.org/xsd";
        String getPayload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:xsd=\"" + axisNs + "\"><soapenv:Body><xsd:getOAuthApplicationData>"
                + "<xsd:consumerKey>" + Utils.escapeXml(consumerKey)
                + "</xsd:consumerKey></xsd:getOAuthApplicationData></soapenv:Body></soapenv:Envelope>";
        HttpResponse before = SimpleHTTPClient.getInstance().sendSoapRequest(serviceUrl, getPayload,
                "urn:getOAuthApplicationData", actor.getUserName(), actor.getPassword());
        assertOAuthAdminSoapSuccess(before, "getOAuthApplicationData before opaque-token switch");
        String beforeBody = before.getData();
        String appName = soapFieldValue(beforeBody, "applicationName");
        String oauthVersion = soapFieldValue(beforeBody, "OAuthVersion");
        String callbackUrl = soapFieldValue(beforeBody, "callbackUrl");
        String grantTypes = soapFieldValue(beforeBody, "grantTypes");
        String registeredKey = soapFieldValue(beforeBody, "oauthConsumerKey");
        String registeredSecret = soapFieldValue(beforeBody, "oauthConsumerSecret");
        Assert.assertNotNull(appName, "OAuthAdminService did not return applicationName for the consumer key");
        Assert.assertNotNull(registeredKey, "OAuthAdminService did not return oauthConsumerKey for the consumer key");
        Assert.assertNotNull(registeredSecret,
                "OAuthAdminService did not return oauthConsumerSecret for the consumer key");
        Assert.assertEquals(registeredKey, consumerKey,
                "OAuthAdminService returned a different client than the requested consumer key");
        Assert.assertEquals(soapFieldValue(beforeBody, "tokenType"), "JWT",
                "Expected a JWT OAuth client before switching it to opaque-token issuance");

        String updatePayload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:xsd=\"" + axisNs + "\" xmlns:xsd1=\"" + dtoNs + "\"><soapenv:Body>"
                + "<xsd:updateConsumerApplication><xsd:consumerAppDTO>"
                + soapDtoField("OAuthVersion", oauthVersion)
                + soapDtoField("applicationName", appName)
                + soapDtoField("callbackUrl", callbackUrl)
                + soapDtoField("grantTypes", grantTypes)
                + soapDtoField("oauthConsumerKey", registeredKey)
                + soapDtoField("oauthConsumerSecret", registeredSecret)
                + soapDtoField("tokenType", "OAUTH")
                + "</xsd:consumerAppDTO></xsd:updateConsumerApplication></soapenv:Body></soapenv:Envelope>";
        HttpResponse update = SimpleHTTPClient.getInstance().sendSoapRequest(serviceUrl, updatePayload,
                "urn:updateConsumerApplication", actor.getUserName(), actor.getPassword());
        assertOAuthAdminSoapSuccess(update, "updateConsumerApplication to opaque-token issuance");

        HttpResponse after = SimpleHTTPClient.getInstance().sendSoapRequest(serviceUrl, getPayload,
                "urn:getOAuthApplicationData", actor.getUserName(), actor.getPassword());
        assertOAuthAdminSoapSuccess(after, "getOAuthApplicationData after opaque-token switch");
        Assert.assertEquals(soapFieldValue(after.getData(), "tokenType"), "OAUTH",
                "OAuthAdminService did not retain OAUTH tokenType after updateConsumerApplication");
    }

    /** Validates token format without ever including the credential itself in assertion output or logs. */
    @Then("The stored access token {string} should be opaque, not a JWT")
    public void theStoredAccessTokenShouldBeOpaque(String tokenRef) {
        String token = TestContext.resolve(tokenRef).toString();
        Assert.assertFalse(token.isBlank(), "Expected a nonblank access token");
        Assert.assertEquals(token.split("\\.", -1).length, 1,
                "Expected an opaque access token (not a three-segment JWT); token value is intentionally omitted");
    }

    private static void assertOAuthAdminSoapSuccess(HttpResponse response, String operation) {
        String body = response == null ? null : response.getData();
        String lowerBody = body == null ? "" : body.toLowerCase();
        Assert.assertTrue(response != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && body != null && !body.isBlank() && !lowerBody.contains("<faultstring")
                        && !lowerBody.contains("exception"),
                operation + " failed: HTTP " + (response == null ? "no response" : response.getResponseCode())
                        + ", responseBodyPresent=" + (body != null && !body.isBlank()));
    }

    private static String soapFieldValue(String body, String localName) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("<[^>]*" + localName
                + ">(.*?)</[^>]*" + localName + ">", java.util.regex.Pattern.DOTALL).matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String soapDtoField(String localName, String value) {
        return value == null ? "" : "<xsd1:" + localName + ">" + Utils.escapeXml(value)
                + "</xsd1:" + localName + ">";
    }

    /**
     * Decodes the JWT access token held under {@code tokenKey} and stores the value of the given claim under
     * {@code targetKey}. Used to compare the {@code sub} claim of two tokens (same application identity).
     */
    @When("I extract JWT claim {string} from access token {string} and store it as {string}")
    public void iExtractJwtClaim(String claim, String tokenKey, String targetKey) {
        String token = TestContext.resolve(tokenKey).toString();
        String[] parts = token.split("\\.");
        Assert.assertTrue(parts.length >= 2, "Access token is not a JWT (cannot extract claim '"
                + claim + "'): " + token);
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Object value = new JSONObject(payloadJson).opt(claim);
        Assert.assertNotNull(value, "JWT claim '" + claim + "' not present in token payload: " + payloadJson);
        TestContext.set(targetKey, value.toString());
    }

    /** Asserts two previously-stored context values are equal (e.g. the sub claims of two tokens). */
    @Then("The stored value {string} should equal {string}")
    public void theStoredValueShouldEqual(String keyA, String keyB) {
        String a = TestContext.resolve(keyA).toString();
        String b = TestContext.resolve(keyB).toString();
        Assert.assertEquals(a, b, "Stored value '" + keyA + "' (" + a + ") != '" + keyB + "' (" + b + ")");
    }

    /**
     * Admin: the scope settings of {@code apim:subscribe} (or any scope) FOR A PARTICULAR USER
     * ({@code GET system-scopes/{base64(scope)}?username=…}) — the lookup change-owner performs to decide whether a
     * candidate owner is a valid subscriber. {@code username} is a RAW username (not an actor reference) so the
     * non-existent-user negative can address a name that was never provisioned; {@code ${UNIQUE:...}} and
     * {@code {{...}}} placeholders are resolved so a uniquified name can be passed. Non-asserting — the feature
     * asserts the exact status.
     */
    @When("I retrieve the system scope {string} for the raw user {string}")
    public void iRetrieveSystemScopeForUser(String scopeName, String rawUsername) throws IOException {
        String username = Utils.resolveContextPlaceholders(Utils.resolvePayloadPlaceholders(rawUsername));
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        Requests.get(Utils.getSystemScopeForUserURL(Utils.getBaseUrl(), scopeName, username), headers);
    }

    /** As {@link #iRetrieveSystemScopeForUser} but for a provisioned ACTOR (resolved to its API username form). */
    @When("I retrieve the system scope {string} for {string}")
    public void iRetrieveSystemScopeForActor(String scopeName, String actorRef) throws IOException {
        iRetrieveSystemScopeForUser(scopeName, apiUsername(Identity.resolveActor(actorRef)));
    }

    /**
     * Asserts the consumer-secrets list response is INTERNALLY consistent: its {@code count} field equals the number
     * of entries in {@code list}. No generic step can express this — it compares two fields of the SAME body against
     * each other rather than a field against a literal, and a wrong {@code count} (the thing this guards) would pass
     * every contains/field assertion.
     */
    @Then("The consumer secrets list count should equal the number of listed secrets")
    public void theSecretsCountShouldMatchListSize() {
        JSONObject body = successBody("consumer secrets list");
        Assert.assertTrue(body.has("count"), "count field missing in secrets list response: " + body);
        int listSize = body.getJSONArray("list").length();
        Assert.assertEquals(body.getInt("count"), listSize,
                "secrets list count field (" + body.getInt("count") + ") != number of entries (" + listSize
                        + "): " + body);
    }

    /**
     * Asserts the secret whose id is held under {@code secretIdKey} appears in the published consumer-secrets list
     * AND that its {@code additionalProperties.description} equals {@code expectedDescription} exactly — i.e. the
     * description supplied at generation round-trips. Needs its own step because it is a lookup of one list entry BY
     * ID followed by a nested-property comparison; the JSONPath field step cannot express it (it resolves
     * placeholders only in the expected value, not inside the path).
     */
    @Then("The listed consumer secret {string} should have description {string}")
    public void theListedSecretShouldHaveDescription(String secretIdKey, String expectedDescription) {
        String secretId = TestContext.resolve(secretIdKey).toString();
        JSONObject body = successBody("consumer secrets list");
        JSONArray list = body.getJSONArray("list");
        for (int i = 0; i < list.length(); i++) {
            JSONObject secret = list.getJSONObject(i);
            if (secretId.equals(secret.optString("secretId"))) {
                JSONObject props = secret.optJSONObject("additionalProperties");
                Assert.assertNotNull(props, "secret " + secretId + " has no additionalProperties: " + secret);
                Assert.assertEquals(props.optString("description", null), expectedDescription,
                        "description of secret " + secretId + " did not round-trip: " + secret);
                return;
            }
        }
        Assert.fail("Secret id " + secretId + " is not present in the secrets list: " + body);
    }

    /**
     * The published {@code httpResponse} parsed as a JSON object, after asserting it is a 2xx WITH a body (CLAUDE.md
     * §7): parsing a failed/empty response otherwise throws an opaque JSONException instead of a clear failure.
     */
    private static JSONObject successBody(String what) {
        HttpResponse resp = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(resp != null && resp.getResponseCode() >= 200 && resp.getResponseCode() < 300
                        && resp.getData() != null && !resp.getData().isBlank(),
                "Expected a 2xx response with a body for the " + what + ", but got: "
                        + (resp == null ? "null" : resp.getResponseCode() + " / " + resp.getData()));
        return new JSONObject(resp.getData());
    }

    /** Asserts two previously-stored context values differ (e.g. two independent application ids). */
    @Then("The stored value {string} should not equal {string}")
    public void theStoredValueShouldNotEqual(String keyA, String keyB) {
        String a = TestContext.resolve(keyA).toString();
        String b = TestContext.resolve(keyB).toString();
        Assert.assertNotEquals(a, b, "Stored value '" + keyA + "' should differ from '" + keyB
                + "' but both are (" + a + ")");
    }

}
