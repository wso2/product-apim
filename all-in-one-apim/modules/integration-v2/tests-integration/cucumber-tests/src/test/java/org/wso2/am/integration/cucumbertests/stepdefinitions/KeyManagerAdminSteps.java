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

import io.cucumber.java.en.When;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
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

    private String getBaseUrl() {
        return baseSteps.getBaseUrl();
    }

    /** The tenant domain of the acting actor (parsed from its {@code user@domain} username; super if unqualified). */
    private static String actingTenantDomain() {
        String username = Identity.actingActor().getUserName();
        int at = username.indexOf(Constants.CHAR_AT);
        return at >= 0 ? username.substring(at + 1) : Constants.SUPER_TENANT_DOMAIN;
    }

    /** Basic-auth header for the acting actor's own carbon credentials (introspect/SOAP admin services). */
    private static String actingBasicAuth() {
        User actor = Identity.actingActor();
        String creds = actor.getUserName() + ":" + actor.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
    }

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
     */
    @When("I introspect the access token {string}")
    public void iIntrospectTheAccessToken(String tokenKey) throws IOException {
        String token = TestContext.resolve(tokenKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, actingBasicAuth());
        // A tenant token must be introspected at the tenant-qualified path; the super path 401s a tenant caller.
        Requests.post(Utils.getIntrospectEndpointURL(getBaseUrl(), actingTenantDomain()), headers,
                "token=" + token, "application/x-www-form-urlencoded");
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
                + "<xsd:updateConsumerAppState><xsd:consumerKey>" + consumerKey + "</xsd:consumerKey>"
                + "<xsd:newState>" + newState + "</xsd:newState>"
                + "</xsd:updateConsumerAppState></soapenv:Body></soapenv:Envelope>";
        Requests.soap(Utils.getOAuthAdminServiceURL(getBaseUrl()), envelope, "urn:updateConsumerAppState",
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
        Requests.post(Utils.getChangeApplicationOwnerURL(getBaseUrl(), appId, newOwner), headers, "", null);
    }

    /**
     * Admin change-owner to a RAW username string (not an actor reference) — for the negative "non-existent owner"
     * case where the target user does not exist as a provisioned actor. Non-asserting.
     */
    @When("I change the owner of application {string} to the raw user {string}")
    public void iChangeTheOwnerOfApplicationRaw(String appIdRef, String rawOwner) throws IOException {
        String appId = TestContext.resolve(appIdRef).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        Requests.post(Utils.getChangeApplicationOwnerURL(getBaseUrl(), appId, rawOwner), headers, "", null);
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
        Requests.get(Utils.getAdminApplicationsByOwnerURL(getBaseUrl(), owner), headers);
    }

    /**
     * Requests a {@code client_credentials} token directly at {@code /oauth2/token}, authenticating with a given
     * consumer key and secret VALUE (both resolved from context keys). Used to prove an ADDITIONAL client secret
     * yields a valid token (and, across two secrets, that both work and carry the same application identity).
     * Publishes the token response for assertion.
     */
    @When("I request a client-credentials token using consumer key {string} and secret {string}")
    public void iRequestClientCredentialsToken(String consumerKeyRef, String secretRef) throws IOException {
        String consumerKey = TestContext.resolve(consumerKeyRef).toString();
        String secret = TestContext.resolve(secretRef).toString();
        String creds = Base64.getEncoder().encodeToString(
                (consumerKey + ":" + secret).getBytes(StandardCharsets.UTF_8));
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic " + creds);
        Requests.post(Utils.getAPIMTokenEndpointURL(getBaseUrl()), headers, "grant_type=client_credentials",
                "application/x-www-form-urlencoded");
    }

    /**
     * Decodes the JWT access token held under {@code tokenKey} and stores the value of the given claim under
     * {@code targetKey}. Used to compare the {@code sub} claim of two tokens (same application identity).
     */
    @When("I extract JWT claim {string} from access token {string} and store it as {string}")
    public void iExtractJwtClaim(String claim, String tokenKey, String targetKey) {
        String token = TestContext.resolve(tokenKey).toString();
        String[] parts = token.split("\\.");
        org.testng.Assert.assertTrue(parts.length >= 2, "Access token is not a JWT (cannot extract claim '"
                + claim + "'): " + token);
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Object value = new org.json.JSONObject(payloadJson).opt(claim);
        org.testng.Assert.assertNotNull(value, "JWT claim '" + claim + "' not present in token payload: " + payloadJson);
        TestContext.set(targetKey, value.toString());
    }

    /** Asserts two previously-stored context values are equal (e.g. the sub claims of two tokens). */
    @io.cucumber.java.en.Then("The stored value {string} should equal {string}")
    public void theStoredValueShouldEqual(String keyA, String keyB) {
        String a = TestContext.resolve(keyA).toString();
        String b = TestContext.resolve(keyB).toString();
        org.testng.Assert.assertEquals(a, b, "Stored value '" + keyA + "' (" + a + ") != '" + keyB + "' (" + b + ")");
    }

    /** Asserts two previously-stored context values differ (e.g. two independent application ids). */
    @io.cucumber.java.en.Then("The stored value {string} should not equal {string}")
    public void theStoredValueShouldNotEqual(String keyA, String keyB) {
        String a = TestContext.resolve(keyA).toString();
        String b = TestContext.resolve(keyB).toString();
        org.testng.Assert.assertNotEquals(a, b, "Stored value '" + keyA + "' should differ from '" + keyB
                + "' but both are (" + a + ")");
    }

    /**
     * Asserts the VALUE of a (possibly nested, dot-path) field in the last response equals the expected value —
     * parsed structurally, not by substring, so it is immune to JSON formatting/whitespace (e.g. the keygen
     * response serialises {@code "pkceMandatory":true} compact, so a spaced string-contains would falsely fail).
     * The expected value is compared as a string against the field's stringified value ({@code true}/{@code false}
     * for booleans, the literal for strings/numbers).
     */
    @io.cucumber.java.en.Then("The value of response field {string} should be {string}")
    public void theValueOfResponseFieldShouldBe(String field, String expected) throws IOException {
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        org.testng.Assert.assertTrue(response != null && response.getResponseCode() >= 200
                        && response.getResponseCode() < 300 && response.getData() != null
                        && !response.getData().isEmpty(),
                "No successful response to read field '" + field + "' from, got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));
        Object value = Utils.extractValueFromPayload(response.getData(), field);
        org.testng.Assert.assertNotNull(value, "Field '" + field + "' not present in response: " + response.getData());
        org.testng.Assert.assertEquals(String.valueOf(value), expected,
                "Field '" + field + "' value mismatch in response: " + response.getData());
    }
}
