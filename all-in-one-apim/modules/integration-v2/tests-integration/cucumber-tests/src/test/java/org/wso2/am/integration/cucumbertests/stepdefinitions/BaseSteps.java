/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

import com.google.gson.JsonObject;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Names;
import org.wso2.am.integration.cucumbertests.utils.ServerReadiness;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BaseSteps {

    private static final Log log = LogFactory.getLog(BaseSteps.class);

    protected String getBaseUrl() {
        return Utils.getBaseUrl();
    }

    /**
     * Readiness assertion only. Server boot/readiness is the block lifecycle listener's job, so this step
     * no longer caches any "current" tenant/user — it simply confirms the block published a baseUrl. Kept
     * so existing feature prose ("Given The system is ready") still resolves; carries no ordering contract.
     */
    @Given("The system is ready")
    public void theSystemIsReady() {

        getBaseUrl();
    }

    /**
     * Sets the scenario's acting actor for subsequent no-arg token lookups. Used by access-control scenarios
     * that switch identity mid-scenario (e.g. create a resource as a publisher, then act as a subscriber to
     * prove rejection) and need to switch back — notably so the {@code @cleanup} teardown deletes the
     * publisher-owned resources using the publisher's token rather than the last-acting (powerless) actor.
     */
    @Given("I act as {string}")
    public void iActAs(String actorRef) {
        Identity.setActingActor(actorRef);
    }

    /**
     * Creates a Dynamic Client Registration (DCR) application for the default actor (super-tenant admin).
     */
    @When("I have a valid DCR application for the current user")
    public void iHaveADCRApplication() throws IOException {

        createDcrApplication(Identity.actingActor());
    }

    /**
     * Creates a DCR application for a named actor (e.g. {@code "userKey1"} or {@code "admin@tenant1.com"}).
     */
    @When("I have a valid DCR application as {string}")
    public void iHaveADCRApplicationAs(String actorRef) throws IOException {

        createDcrApplication(Identity.resolveActor(actorRef));
    }

    private void createDcrApplication(User actor) throws IOException {

        //Create json payload for DCR endpoint. The DCR client name must adhere to ^[\sa-zA-Z0-9._-]*$ — a
        // secondary-store actor's username carries the store-domain separator (e.g. SECONDARY.COM/secondaryAdmin1),
        // whose '/' is outside that set, so sanitize any disallowed char to '_' when DERIVING the name. This is
        // cosmetic only: the actual OAuth identity is the untouched `owner` (actor.getUserName()) below.
        String clientNameSafe = ("integration_test_app_" + actor.getUserNameWithoutDomain() + "_"
                + actor.getUserDomain()).replaceAll("[^\\sa-zA-Z0-9._-]", "_");
        JsonObject json = new JsonObject();
        json.addProperty("callbackUrl", "test.com");
        json.addProperty("clientName", clientNameSafe);
        json.addProperty("grantType", "client_credentials password refresh_token");
        json.addProperty("saasApp", true);
        json.addProperty("owner", actor.getUserName());

        String encodedCredentials = Base64.getEncoder().encodeToString(
                    (actor.getUserName() + ':' + actor.getPassword()).getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + encodedCredentials);

        // The gateway health-check can pass before the client-registration webapp finishes deploying, so a
        // DCR POST fired immediately after boot can hit a transient 500 "Dynamic Client Registration Service
        // not available" — a race that parallel runners sharing one freshly-booted container widen. Retrying
        // the POST blindly is safe for THIS endpoint: DCR is an idempotent upsert keyed on clientName (which
        // is deterministic per actor above — every token acquisition re-POSTs it and receives the existing
        // client back), so even a create that committed server-side with a lost response just returns the
        // same client on the retry. Retry until 200 or the startup window elapses, mirroring
        // TenantUserProvisioner.awaitTenantMgtServiceReady for the admin services.
        String dcrUrl = Utils.getDCREndpointURL(getBaseUrl());
        long deadlineStart = System.currentTimeMillis();
        long deadline = deadlineStart + Constants.SERVER_STARTUP_WAIT_TIME;
        HttpResponse dcrResponse = null;
        while (true) {
            try {
                dcrResponse = SimpleHTTPClient.getInstance().doPost(dcrUrl, headers, json.toString(),
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
                if (dcrResponse.getResponseCode() == 200) {
                    break;
                }
            } catch (IOException transientFailure) {
                // Connection-level failure during the same warm-up window the 500-retry exists for — keep
                // retrying within the startup deadline instead of letting one refused connection kill the step.
            }
            if (System.currentTimeMillis() >= deadline) {
                break;
            }
            log.info("DCR endpoint not ready yet"
                    + (dcrResponse == null ? "" : " (status " + dcrResponse.getResponseCode() + ")")
                    + "; retrying registration...");
            try {
                Utils.pollPause(deadlineStart, 500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        Assert.assertNotNull(dcrResponse,
                "DCR registration got no response within the startup window (requests failed)");
        Assert.assertEquals(dcrResponse.getResponseCode(), 200, dcrResponse.getData());

        String clientId = Utils.extractValueFromPayload(dcrResponse.getData(), "clientId").toString();
        String clientSecret = Utils.extractValueFromPayload(dcrResponse.getData(), "clientSecret").toString();
        // get base64 encoded "clientId:clientSecret"
        String dcrCredentials = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret)
                .getBytes(StandardCharsets.UTF_8));

        TestContext.set(Identity.dcrCredentialsKey(actor), dcrCredentials);
    }

    /**
     * Obtains a valid Publisher API access token for the default actor (super-tenant admin).
     */
    @Given("I have a valid Publisher access token for the current user")
    public void iHaveValidPublisherAccessToken() throws Exception {

        mintPublisherToken(Identity.actingActor());
    }

    /**
     * Obtains a valid Publisher API access token for a named actor.
     */
    @Given("I have a valid Publisher access token as {string}")
    public void iHaveValidPublisherAccessTokenAs(String actorRef) throws Exception {

        mintPublisherToken(Identity.resolveActor(actorRef));
    }

    private void mintPublisherToken(User actor) throws Exception {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Basic " + TestContext.get(Identity.dcrCredentialsKey(actor)).toString());

        // create json payload to obtain publisher access token
        JsonObject json = new JsonObject();
        json.addProperty("grant_type", "password");
        json.addProperty("username", actor.getUserName());
        json.addProperty("password", actor.getPassword());
        json.addProperty("scope", "apim:api_view apim:api_create apim:api_publish apim:api_delete apim:api_manage apim:api_import_export apim:subscription_manage apim:client_certificates_add apim:client_certificates_update apim:shared_scope_manage apim:common_operation_policy_manage apim:policies_import_export apim:api_generate_key apim:gateway_policy_manage apim:mcp_server_create apim:mcp_server_manage apim:mcp_server_publish apim:mcp_server_view apim:mcp_server_delete apim:mcp_server_list_view");

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(getBaseUrl()), headers,
            json.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertEquals(response.getResponseCode(), 200, response.getData());

        String accessToken = Utils.extractValueFromPayload(response.getData(), "access_token").toString();
        TestContext.set(Identity.publisherTokenKey(actor), accessToken);
        log.info("Obtained Publisher access token for user " + actor.getUserName()
                + " with expires_in (seconds): "
                + Utils.extractValueFromPayload(response.getData(), "expires_in"));
    }

    /**
     * Obtains a valid Developer Portal access token for the default actor (super-tenant admin).
     */
    @Given("I have a valid Devportal access token for the current user")
    public void iHaveValidDevportalAccessToken() throws Exception {

        mintDevportalToken(Identity.actingActor());
    }

    /**
     * Obtains a valid Developer Portal access token for a named actor.
     */
    @Given("I have a valid Devportal access token as {string}")
    public void iHaveValidDevportalAccessTokenAs(String actorRef) throws Exception {

        mintDevportalToken(Identity.resolveActor(actorRef));
    }

    private void mintDevportalToken(User actor) throws Exception {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Basic " + TestContext.get(Identity.dcrCredentialsKey(actor)).toString());

        // create json payload to obtain devportal access token
        JsonObject json = new JsonObject();
        json.addProperty("grant_type", "password");
        json.addProperty("username", actor.getUserName());
        json.addProperty("password", actor.getPassword());
        json.addProperty("scope", "apim:app_manage apim:sub_manage apim:subscribe");

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(getBaseUrl()), headers,
            json.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertEquals(response.getResponseCode(), 200, response.getData());

        String accessToken = Utils.extractValueFromPayload(response.getData(), "access_token").toString();
        TestContext.set(Identity.devportalTokenKey(actor), accessToken);
        log.info("Obtained Devportal access token for user " + actor.getUserName()
                + " with expires_in (seconds): "
                + Utils.extractValueFromPayload(response.getData(), "expires_in"));
    }

    /**
     * Obtains a valid Admin access token for the default actor (super-tenant admin).
     */
    public void iHaveValidAdminAccessToken() throws Exception {

        mintAdminToken(Identity.actingActor());
    }

    private void mintAdminToken(User actor) throws Exception {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Basic " + TestContext.get(Identity.dcrCredentialsKey(actor)).toString());

        // create json payload to obtain admin access token
        JsonObject json = new JsonObject();
        json.addProperty("grant_type", "password");
        json.addProperty("username", actor.getUserName());
        json.addProperty("password", actor.getPassword());
        json.addProperty("scope", "apim:admin apim:tier_view apim:api_provider_change apim:llm_provider_manage apim:llm_provider_read");

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(getBaseUrl()), headers,
                json.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertEquals(response.getResponseCode(), 200, response.getData());

        String accessToken = Utils.extractValueFromPayload(response.getData(), "access_token").toString();
        TestContext.set(Identity.adminTokenKey(actor), accessToken);
        log.info("Obtained Admin access token for user " + actor.getUserName()
                + " with expires_in (seconds): "
                + Utils.extractValueFromPayload(response.getData(), "expires_in"));
    }

    /**
     * Obtains a valid Governance API access token for a named actor (must have governance rights). Requires the
     * actor's DCR application to already exist (e.g. via "I have valid access tokens as ..."). Governance is its
     * own product API with its own {@code apim:gov_*} scopes — not reachable with the admin token.
     */
    @Given("I have a valid Governance access token as {string}")
    public void iHaveValidGovernanceAccessTokenAs(String actorRef) throws Exception {

        mintGovernanceToken(Identity.resolveActor(actorRef));
    }

    private void mintGovernanceToken(User actor) throws Exception {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Basic " + TestContext.get(Identity.dcrCredentialsKey(actor)).toString());

        // create json payload to obtain governance access token
        JsonObject json = new JsonObject();
        json.addProperty("grant_type", "password");
        json.addProperty("username", actor.getUserName());
        json.addProperty("password", actor.getPassword());
        json.addProperty("scope", "apim:gov_rule_read apim:gov_rule_manage apim:gov_policy_read apim:gov_policy_manage apim:gov_result_read openid");

        HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(getBaseUrl()), headers,
                json.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertEquals(response.getResponseCode(), 200, response.getData());

        String accessToken = Utils.extractValueFromPayload(response.getData(), "access_token").toString();
        TestContext.set(Identity.governanceTokenKey(actor), accessToken);
        log.info("Obtained Governance access token for user " + actor.getUserName()
                + " with expires_in (seconds): "
                + Utils.extractValueFromPayload(response.getData(), "expires_in"));
    }

    /**
     * Composite step that obtains DCR + all access tokens for the default actor (super-tenant admin).
     */
    @Given("The system is ready and I have valid access tokens for current user")
    public void iHaveSystemAndTokens() throws Exception {

        theSystemIsReady();
        User actor = Identity.actingActor();
        createDcrApplication(actor);
        mintPublisherToken(actor);
        mintDevportalToken(actor);
        mintAdminToken(actor);
    }

    /**
     * Composite step that obtains DCR + all access tokens for a named actor (incl. the admin token, so the
     * actor must have admin rights). Records the actor as the scenario's acting actor so subsequent no-arg
     * token lookups in the glue resolve to it.
     */
    @Given("I have valid access tokens as {string}")
    public void iHaveTokensAs(String actorRef) throws Exception {

        Identity.setActingActor(actorRef);
        User actor = Identity.resolveActor(actorRef);
        createDcrApplication(actor);
        mintPublisherToken(actor);
        mintDevportalToken(actor);
        mintAdminToken(actor);
    }

    /**
     * Composite step for a least-privilege publisher actor: DCR + publisher + devportal tokens only (NO admin
     * token, since a non-admin publisher user is denied the {@code apim:admin} scope). Records the actor as the
     * scenario's acting actor so the publisher glue's no-arg token lookups resolve to it. This is the default
     * Background for publisher-plane features, parameterizable over a {@code Scenario Outline} actor column
     * (e.g. {@code publisherUser} vs {@code publisherUser@tenant1.com}).
     */
    @Given("The system is ready and I have valid publisher access tokens as {string}")
    public void iHavePublisherTokensAs(String actorRef) throws Exception {

        theSystemIsReady();
        Identity.setActingActor(actorRef);
        User actor = Identity.resolveActor(actorRef);
        createDcrApplication(actor);
        mintPublisherToken(actor);
        mintDevportalToken(actor);
    }

    /**
     * Composite step that obtains DCR + devportal token only for the default actor (super-tenant admin).
     * Useful for subscriber-only users who do not have publisher or admin permissions.
     */
    @Given("The system is ready and I have valid devportal access token for current user")
    public void iHaveSystemAndDevportalToken() throws Exception {

        theSystemIsReady();
        User actor = Identity.actingActor();
        createDcrApplication(actor);
        mintDevportalToken(actor);
    }

    /**
     * Composite step for a DevPortal consumer actor: DCR + devportal token only, for a named actor. Records
     * the actor as the scenario's acting actor so the devportal glue's no-arg token lookups resolve to it.
     * This is the default Background for devportal-plane features (application CRUD, subscribe, etc.),
     * parameterizable over a {@code Scenario Outline} actor column (e.g. {@code subscriberUser} vs
     * {@code subscriberUser@tenant1.com}).
     */
    @Given("The system is ready and I have valid devportal access token as {string}")
    public void iHaveDevportalTokenAs(String actorRef) throws Exception {

        theSystemIsReady();
        Identity.setActingActor(actorRef);
        User actor = Identity.resolveActor(actorRef);
        createDcrApplication(actor);
        mintDevportalToken(actor);
    }

    /**
     * Replaces a literal substring in a stored context value and writes it back. Used to resolve a server-side
     * template placeholder that the publisher API returns verbatim — e.g. a version-first API's context is
     * stored as {@code /{version}/ctx}, and the gateway invocation needs {@code /1.0.0/ctx}.
     *
     * @param token    literal substring to replace (e.g. {@code {version}})
     * @param value    replacement value (e.g. {@code 1.0.0})
     * @param key      context key whose value is rewritten in place
     */
    @When("I replace {string} with {string} in context {string}")
    public void iReplaceInContext(String token, String value, String key) {
        String resolved = TestContext.resolve(key).toString().replace(token, value);
        TestContext.set(Utils.normalizeContextKey(key), resolved);
    }

    /**
     * Copies the value of an EXISTING context key under a second key (an alias, e.g. to keep a value stable under
     * a scenario-specific name after a later step overwrites the original). {@code value} must therefore already
     * be a context key: {@link TestContext#resolve} throws when it is not, so a typo'd key fails fast with a clear
     * message instead of silently aliasing the literal text (§7 — {@code resolve} is the default for step
     * arguments precisely for that reason). To seed a value the product never produced, use a step that MINTS it
     * (e.g. {@link #iGenerateRandomUuidAndStore}) rather than passing a literal here.
     *
     * @param value The name of an existing context key whose value should be copied
     * @param contextKey The key under which the value should be stored in TestContext
     */
    @When("I put value {string} in context as {string}")
    public void iPutValueInContextAs(String value, String contextKey) {
        Object resolvedValue = TestContext.resolve(value);

        log.info("Setting context key: " + contextKey + " with value: " + resolvedValue);
        TestContext.set(Utils.normalizeContextKey(contextKey), resolvedValue.toString());
    }

    /**
     * Decodes the JWT stored under a context key (a {@code header.payload.signature} token) and asserts its
     * payload segment contains the expected substring. Used to verify token claims such as the internal API
     * key's {@code keytype}. Base64url-decodes tolerantly (falls back to standard base64).
     *
     * @param contextKey context key holding the JWT
     * @param expected   substring expected in the decoded JWT payload (e.g. {@code "keytype":"SANDBOX"})
     */
    @Then("The JWT stored as {string} should contain {string}")
    public void theJwtStoredShouldContain(String contextKey, String expected) {
        String jwt = TestContext.resolve(contextKey).toString();
        String[] segments = jwt.split("\\.");
        Assert.assertTrue(segments.length >= 2, "Malformed JWT (expected >= 2 segments): " + jwt);
        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(segments[1]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            payload = new String(Base64.getDecoder().decode(segments[1]), StandardCharsets.UTF_8);
        }
        Assert.assertTrue(payload.contains(expected),
                "Decoded JWT payload does not contain '" + expected + "': " + payload);
    }

    /**
     * Loads a JSON payload from a file and stores it in the test context.
     *
     * @param jsonFilePath Path to the JSON file
     * @param key Context key to store the JSON payload
     */
    @When("I put JSON payload from file {string} in context as {string}")
    public void putJsonPayloadFromFile(String jsonFilePath, String key) throws IOException {

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(jsonFilePath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("File not found on classpath: " + jsonFilePath);
            }
            String jsonPayload = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            TestContext.set(Utils.normalizeContextKey(key), Utils.resolvePayloadPlaceholders(jsonPayload));
        }
    }

    /**
     * Stores a JSON payload in the test context.
     *
     * @param key Context key to store the JSON payload
     * @param docStringJson JSON payload provided as a doc string
     */
    @When("I put the following JSON payload in context as {string}")
    public void putJsonPayloadInContext(String key, String docStringJson)  {

        TestContext.set(Utils.normalizeContextKey(key), Utils.resolvePayloadPlaceholders(docStringJson));
    }

    /**
     * Stores a filler text payload of exactly {@code sizeKb} KB in the context, for a request whose BYTE COUNT is
     * the point rather than its content — a bandwidth (BANDWIDTHLIMIT) throttle quota is spent by bytes, so one
     * body larger than the whole quota trips it in a single call. Generated rather than held as a fixture so the
     * size is stated at the call site and no multi-KB literal lands in a feature file.
     *
     * @param sizeKb size of the payload in KB
     * @param key    context key to store the payload under
     */
    @When("I put a {int} KB text payload in context as {string}")
    public void putSizedTextPayloadInContext(int sizeKb, String key) {

        TestContext.set(Utils.normalizeContextKey(key), "A".repeat(sizeKb * 1024));
    }

    /**
     * Sets a top-level field to a JSON VALUE (array or object) rather than a string — e.g. giving an API two
     * subscription policies, {@code ["QuotaPlan","AsyncWHUnlimited"]}, which a scenario needs when it compares a
     * limited subscriber against an unlimited control on the SAME API.
     *
     * <p>Deliberately a separate step from {@link #iSetFieldInPayload}, not a smart-parsing upgrade of it: that one
     * sets a STRING and callers depend on it doing exactly that (a value that merely looks like JSON must stay a
     * string there). Picking the step IS the statement of intent, like TestContext.resolve/get/contains. Fails
     * loudly if the value is not parseable JSON, so a typo cannot silently land as a string.
     *
     * @param field      the top-level JSON field to set
     * @param json       a JSON array or object literal
     * @param contextKey the context key holding the JSON payload
     */
    @When("I set the field {string} to the JSON value {string} in the payload {string}")
    public void iSetFieldToJsonValueInPayload(String field, String json, String contextKey) {

        JSONObject payload = new JSONObject(TestContext.resolve(contextKey).toString());
        String resolved = Utils.resolveContextPlaceholders(json).trim();
        Object value;
        try {
            value = resolved.startsWith("[") ? new org.json.JSONArray(resolved) : new JSONObject(resolved);
        } catch (org.json.JSONException e) {
            throw new IllegalArgumentException("Value for field '" + field + "' is not valid JSON: " + resolved, e);
        }
        payload.put(field, value);
        TestContext.set(Utils.normalizeContextKey(contextKey), payload.toString());
    }

    /**
     * Sets a top-level field of a JSON payload already in context to the given value, writing it back under the
     * same key. Used to build create-validation negatives from a valid base payload (e.g. blank name/context/
     * version, or an invalid context) without a separate fixture per case. An empty {@code value} sets the
     * field to an empty string.
     *
     * @param field      the top-level JSON field to set
     * @param value      the value to set (may be empty)
     * @param contextKey the context key holding the JSON payload
     */
    @When("I set the field {string} to {string} in the payload {string}")
    public void iSetFieldInPayload(String field, String value, String contextKey) {

        JSONObject payload = new JSONObject(TestContext.resolve(contextKey).toString());
        // Resolve any {{contextKey}} placeholders in the value (e.g. reusing an existing API's context to build a
        // duplicate-context negative). Values with no placeholders pass through unchanged; an unknown key throws.
        payload.put(field, Utils.resolveContextPlaceholders(value));
        TestContext.set(Utils.normalizeContextKey(contextKey), payload.toString());
    }

    /**
     * Replaces every occurrence of a literal substring in a payload already in context, writing it back under
     * the same key. A text-level edit for cases where a structured setter can't reach — e.g. a field whose value
     * is itself a STRINGIFIED JSON blob (an MCP backend's {@code endpointConfig} is a JSON string, not a nested
     * object), where changing a URL inside it means editing the raw text. {@code {{contextKey}}} placeholders in
     * both arguments are resolved.
     *
     * @param target      the literal substring to replace
     * @param replacement the replacement text
     * @param contextKey  the context key holding the payload
     */
    @When("I replace {string} with {string} in the payload {string}")
    public void iReplaceInPayload(String target, String replacement, String contextKey) {
        String json = TestContext.resolve(contextKey).toString();
        String resolvedTarget = Utils.resolveContextPlaceholders(target);
        String resolvedReplacement = Utils.resolveContextPlaceholders(replacement);
        TestContext.set(Utils.normalizeContextKey(contextKey), json.replace(resolvedTarget, resolvedReplacement));
    }

    /**
     * Removes a top-level field from a JSON payload already in context, writing it back under the same key.
     * Used to build create-validation negatives where a field must be absent rather than blank.
     *
     * @param field      the top-level JSON field to remove
     * @param contextKey the context key holding the JSON payload
     */
    @When("I remove the field {string} from the payload {string}")
    public void iRemoveFieldFromPayload(String field, String contextKey) {

        JSONObject payload = new JSONObject(TestContext.resolve(contextKey).toString());
        payload.remove(field);
        TestContext.set(Utils.normalizeContextKey(contextKey), payload.toString());
    }

    /**
     * Generates one fresh unique token and stores it, so a value that must be REUSED across several requests in a
     * scenario (e.g. a common tag shared by two APIs, or a name reused as a case-variant) stays stable — unlike an
     * inline {@code ${UNIQUE:...}}, which mints a new value on every reference. Reference it later via
     * {@code {{contextKey}}}.
     *
     * @param contextKey the context key under which the generated value is stored
     */
    @When("I generate a unique value and store it as {string}")
    public void iGenerateUniqueValueAndStore(String contextKey) {
        String key = Utils.normalizeContextKey(contextKey);
        TestContext.set(key, Names.unique(key));
    }

    /**
     * Mints a random UUID and stores it, giving a scenario a WELL-FORMED resource id that names nothing — the input
     * a "not found" negative needs (e.g. deleting a key manager id that does not exist must be 404, not 400). Do NOT
     * merge this with {@link #iGenerateUniqueValueAndStore}: that mints a readable {@code Names.unique} token, which
     * is not a UUID, so an API that validates id FORMAT could reject it before ever looking the id up and the
     * scenario would pass for the wrong reason. Reference the value later via {@code {{contextKey}}}.
     *
     * @param contextKey the context key under which the generated UUID is stored
     */
    @When("I generate a random UUID and store it as {string}")
    public void iGenerateRandomUuidAndStore(String contextKey) {
        TestContext.set(Utils.normalizeContextKey(contextKey), UUID.randomUUID().toString());
    }

    /**
     * Stores the fully upper-cased form of a stored value. Used to build a case-variant that is case-insensitively
     * equal to the source (e.g. proving API-name uniqueness is case-insensitive) while both values remain unique
     * across parallel scenarios (the source is itself a uniquely generated token).
     *
     * @param sourceKey context key holding the source value
     * @param targetKey context key under which the upper-cased value is stored
     */
    @When("I store the uppercase of {string} as {string}")
    public void iStoreUppercaseOf(String sourceKey, String targetKey) {
        Object value = TestContext.resolve(sourceKey);
        TestContext.set(Utils.normalizeContextKey(targetKey), value.toString().toUpperCase(java.util.Locale.ROOT));
    }

    /**
     * Sets a top-level field of a JSON payload (in context) to a JSON OBJECT parsed from a classpath file — for
     * injecting a nested structure (e.g. embedding a custom "LifeCycle" definition into the tenant configuration)
     * that the string setter cannot express. Writes the merged payload back under the same key.
     *
     * @param field        the top-level field to set to the parsed JSON object
     * @param jsonFilePath classpath path of the JSON file whose content becomes the field value
     * @param contextKey   context key holding the JSON payload to mutate
     */
    @When("I set the JSON field {string} from file {string} in the payload {string}")
    public void iSetJsonFieldFromFileInPayload(String field, String jsonFilePath, String contextKey)
            throws IOException {
        JSONObject payload = new JSONObject(TestContext.resolve(contextKey).toString());
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(jsonFilePath)) {
            if (in == null) {
                throw new FileNotFoundException("JSON file not found: " + jsonFilePath);
            }
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            payload.put(field, new JSONObject(content));
        }
        TestContext.set(Utils.normalizeContextKey(contextKey), payload.toString());
    }

    /** Sets a top-level field of a JSON payload in context to an empty array, writing it back under the key. */
    @When("I set the field {string} to an empty array in the payload {string}")
    public void iSetFieldToEmptyArrayInPayload(String field, String contextKey) {

        JSONObject payload = new JSONObject(TestContext.resolve(contextKey).toString());
        payload.put(field, new JSONArray());
        TestContext.set(Utils.normalizeContextKey(contextKey), payload.toString());
    }

    /**
     * Sets a top-level field of a JSON payload in context to JSON {@code null}, writing it back under the key. Used
     * to prove the publisher accepts an API update that nulls an optional field (e.g. {@code securityScheme} or
     * {@code endpointConfig}) — a past NullPointerException regression (UpdateAPINullPointerTestCase). Uses
     * {@link JSONObject#NULL} so the field is serialized as {@code null}, not the string {@code "null"}.
     */
    @When("I set the field {string} to null in the payload {string}")
    public void iSetFieldToNullInPayload(String field, String contextKey) {

        JSONObject payload = new JSONObject(TestContext.resolve(contextKey).toString());
        payload.put(field, JSONObject.NULL);
        TestContext.set(Utils.normalizeContextKey(contextKey), payload.toString());
    }

    /** Sets a top-level boolean field of a JSON payload in context, writing it back under the key. */
    @When("I set the boolean field {string} to {string} in the payload {string}")
    public void iSetBooleanFieldInPayload(String field, String value, String contextKey) {

        JSONObject payload = new JSONObject(TestContext.resolve(contextKey).toString());
        payload.put(field, Boolean.parseBoolean(value));
        TestContext.set(Utils.normalizeContextKey(contextKey), payload.toString());
    }

    /**
     * Generates a runner-unique alphanumeric value (Names.unique with non-alphanumerics stripped) and stores it
     * under the given context key. Use when a scenario needs a SINGLE unique value reused across several later
     * steps (e.g. a category name referenced in create, update, list and attach) — an inline {@code ${UNIQUE:...}}
     * placeholder can't serve this because it regenerates a fresh value on every resolution.
     */
    @When("I generate a unique alphanumeric value and store it as {string}")
    public void iGenerateUniqueAlphanumericValueAndStore(String contextKey) {
        String key = Utils.normalizeContextKey(contextKey);
        TestContext.set(key, org.wso2.am.integration.cucumbertests.utils.Names.unique(key)
                .replaceAll("[^A-Za-z0-9]", ""));
    }

    /**
     * Stores the most recent HTTP response payload in the test context.
     *
     * @param key Context key to store the response payload
     */
    @When("I put the response payload in context as {string}")
    public void putResponsePayloadInContext(String key) throws InterruptedException {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        // Capturing a null/empty payload is never useful — later steps mutate it and PUT it back, so the failure
        // would surface far away with nothing pointing here. Fail now, naming the status.
        Assert.assertTrue(response != null && response.getData() != null && !response.getData().isBlank(),
                "No response payload to store under '" + key + "': got "
                        + (response == null ? "no response at all" : "status " + response.getResponseCode()
                        + " with an empty body"));
        TestContext.set(Utils.normalizeContextKey(key), response.getData());
        Thread.sleep(Constants.WAIT_TIME);
    }

    /**
     * Verifies that the HTTP response status code matches the expected value.
     *
     * @param expectedStatusCode The expected HTTP status code
     */
    @Then("The response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int expectedStatusCode) {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertEquals(response.getResponseCode(), expectedStatusCode, response.getData());
    }


    /**
     * Verifies that the HTTP response body contains the specified string value.
     *
     * @param expectedValue The string value that should be present in the response body
     */
    @Then("The response should contain {string}")
    public void responseShouldContainFieldValue(String expectedValue) {

        // Resolve any {{contextKey}} placeholders so assertions can reference uniquely-generated values
        // (e.g. a ${UNIQUE:...} API name captured into context). Literals without {{}} pass through unchanged.
        expectedValue = Utils.resolveContextPlaceholders(expectedValue);
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        // A body is a precondition of asserting on its content: without this a missing/empty response NPEs here
        // and reports nothing about what actually came back.
        Assert.assertTrue(response != null && response.getData() != null && !response.getData().isBlank(),
                "No response body to search for '" + expectedValue + "' — got "
                        + (response == null ? "no response at all" : "status " + response.getResponseCode()
                        + " with an empty body"));
        Assert.assertTrue(response.getData().contains(expectedValue),
                "Expected response to contain '" + expectedValue + "' but it did not: " + response.getData());
    }

    /**
     * Asserts that a field in the stored JSON response equals an exact expected value (not a substring — use this
     * where {@code The response should contain} would be ambiguous, e.g. a boolean flag whose literal could match
     * another field). {@code fieldName} is a field name / JSONPath resolved by {@link Utils#extractValueFromPayload}.
     * Comparison is on string form so {@code "true"}/{@code "false"}/numbers work without type ceremony.
     *
     * <p>{@code {{contextKey}}} placeholders are resolved in BOTH arguments. Resolving them in the PATH matters
     * as much as in the expected value: a JSONPath predicate routinely filters on a per-scenario value
     * ({@code list[?(@.conditionValue=='{{apiContext}}')]}), and an unresolved literal makes the predicate match
     * nothing — which surfaces as an empty result compared against a correct expectation, i.e. a failure in the
     * TEST while the response was in fact right. Do not "fix" such a failure by
     * restructuring the calling scenario to avoid a placeholder in the path; the resolution belongs here.</p>
     *
     * @param fieldName     field name or JSONPath to read from the response body
     * @param expectedValue the exact expected value (string form)
     */
    @Then("The value of response field {string} should be {string}")
    public void theValueOfResponseFieldShouldBe(String fieldName, String expectedValue) throws IOException {

        assertResponseFieldValue(fieldName, expectedValue, true);
    }

    /**
     * Asserts a field's exact value in an ERROR (non-2xx) response body — the counterpart of
     * {@link #theValueOfResponseFieldShouldBe} for a fault payload that names WHY the call was refused, where the
     * status alone is ambiguous. Example: a gateway 429 carries a {@code code} identifying which throttle limit
     * fired (application vs subscription vs burst vs API-level vs operation-level vs custom rule), so the status
     * by itself cannot tell those dimensions apart.
     *
     * <p>Its status guard is the MIRROR IMAGE of the 2xx twin's, not an absence of one: the response must be
     * non-2xx with a body, so a success payload carrying a same-named field can never satisfy this step either.
     * Assert the status itself in its own step; this one pins the diagnosis in the body.
     *
     * @param fieldName     field name or JSONPath to read from the error response body
     * @param expectedValue the exact expected value (string form)
     */
    @Then("The value of error response field {string} should be {string}")
    public void theValueOfErrorResponseFieldShouldBe(String fieldName, String expectedValue) throws IOException {

        assertResponseFieldValue(fieldName, expectedValue, false);
    }

    /**
     * Shared body of the two field-value assertions: requires a body on the expected side of the 2xx boundary,
     * then compares the field's string form to the expected value exactly.
     *
     * @param expectSuccess whether the response under assertion must be 2xx ({@code false} = must be non-2xx)
     */
    private void assertResponseFieldValue(String fieldName, String expectedValue, boolean expectSuccess)
            throws IOException {

        fieldName = Utils.resolveContextPlaceholders(fieldName);
        expectedValue = Utils.resolveContextPlaceholders(expectedValue);
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        // Pin the response to the expected side of the 2xx boundary: an error body (401/500 JSON) can carry a
        // same-named field and must never satisfy a field assertion written against the success payload, nor
        // vice versa.
        boolean successful = response != null && response.getResponseCode() >= 200
                && response.getResponseCode() < 300;
        Assert.assertTrue(response != null && successful == expectSuccess
                        && response.getData() != null && !response.getData().isBlank(),
                "Expected a " + (expectSuccess ? "2xx" : "non-2xx") + " response with a body to read field '"
                        + fieldName + "' from, but got: "
                        + (response == null ? "null" : response.getResponseCode() + " / " + response.getData()));
        Object actual = Utils.extractValueFromPayload(response.getData(), fieldName);
        // Distinguish a MISSING field from a field literally equal to "null": without this a missing field
        // would compare String.valueOf(null) -> "null" and could silently satisfy an expected "null".
        Assert.assertNotNull(actual, "Field '" + fieldName + "' not present in response: " + response.getData());
        Assert.assertEquals(String.valueOf(actual), expectedValue,
                String.format("Field '%s' was [%s] but expected [%s]. Data: %s",
                        fieldName, actual, expectedValue, response.getData()));
    }

    /**
     * Asserts a response field is PRESENT and holds JSON {@code null} — the one value
     * {@link #theValueOfResponseFieldShouldBe} deliberately cannot express, because it rejects null so a MISSING
     * field can never silently satisfy an expected {@code "null"} literal. That distinction is exactly why this
     * exists as its own step rather than as a magic expected-value: "the server returns the field as null" and
     * "the server omits the field" are different contracts, and a test that pins one must fail on the other.
     * A field that is absent fails here too (the path lookup throws), so this cannot degrade into "either way".
     *
     * <p>First consumer: a plain API's {@code apiThrottlingPolicy}, which is JSON {@code null} — not
     * {@code "Unlimited"} — on an API created without an API-level policy.</p>
     *
     * @param fieldName field name or JSONPath expected to resolve to JSON null
     */
    @Then("The response field {string} should be null")
    public void theResponseFieldShouldBeNull(String fieldName) throws IOException {

        fieldName = Utils.resolveContextPlaceholders(fieldName);
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isBlank(),
                "Expected a 2xx response with a body to read field '" + fieldName + "' from, but got: "
                        + (response == null ? "null" : response.getResponseCode() + " / " + response.getData()));
        // Throws when the path resolves to nothing — an ABSENT field must not pass a "should be null" assertion.
        Object actual = Utils.extractValueFromPayload(response.getData(), fieldName);
        Assert.assertNull(actual, String.format("Field '%s' was [%s] but expected JSON null. Data: %s",
                fieldName, actual, response.getData()));
    }

    /**
     * Asserts that SEVERAL response fields all hold the SAME expected value — the plural counterpart of
     * {@link #theValueOfResponseFieldShouldBe} (which pins one field to one value). Use it where the property
     * under test is shared by a whole SET of fields whose membership varies by case, so a fixed number of
     * singular steps cannot express it: the write-only connector secrets a key manager masks on GET are one
     * such set ({@code client_secret} alone for Auth0/KeyCloak/Forgerock, {@code apiKey} AND
     * {@code client_secret} for Okta, {@code password} AND {@code client_secret} for PingFederate), all of
     * which must come back as exactly the {@code *****} sentinel.
     *
     * @param fieldNamesCsv comma-separated field names / JSONPaths to read from the response body
     * @param expectedValue the exact expected value every one of them must equal (string form)
     */
    @Then("Each of the response fields {string} should be {string}")
    public void eachOfTheResponseFieldsShouldBe(String fieldNamesCsv, String expectedValue) throws IOException {

        for (String fieldName : fieldNamesCsv.split(",")) {
            theValueOfResponseFieldShouldBe(fieldName.trim(), expectedValue);
        }
    }

    /**
     * Asserts that a JSON ARRAY field of the response holds exactly the given elements — no more, no fewer,
     * order-independent. Set equality is the right contract for a collection the server may reorder (e.g. a key
     * manager's {@code availableGrantTypes}), where an index-by-index check would be brittle and a substring
     * {@code contains} would silently pass with extra elements present. An empty expected list asserts the array
     * is empty (or absent). {@code fieldName} resolves through {@link Utils#extractValueFromPayload}, i.e. the same
     * JSONPath resolution as its singular sibling {@link #theValueOfResponseFieldShouldBe}, so a nested path such
     * as {@code additionalProperties.someList} works and a path that resolves to a NON-array fails loudly instead
     * of being read as "empty".
     *
     * <p>{@code {{contextKey}}} placeholders are resolved in BOTH arguments, for the reason spelled out on
     * {@link #theValueOfResponseFieldShouldBe}: an unresolved placeholder inside a JSONPath predicate matches
     * nothing and reports an empty result against a correct response.</p>
     *
     * @param fieldName     field name or JSONPath of the array to read from the response body
     * @param expectedCsv   comma-separated expected elements; empty means "the array must be empty"
     */
    @Then("The response field {string} should be exactly the list {string}")
    public void theResponseArrayFieldShouldBeExactly(String fieldName, String expectedCsv) throws IOException {

        fieldName = Utils.resolveContextPlaceholders(fieldName);
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        // Require a SUCCESSFUL response with a body: an error payload must never satisfy an assertion written
        // against the success shape.
        Assert.assertTrue(response != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isBlank(),
                "Expected a 2xx response with a body to read array field '" + fieldName + "' from, but got: "
                        + (response == null ? "null" : response.getResponseCode() + " / " + response.getData()));

        // SET semantics, deliberately — not a multiset. Call sites assert universal claims ("every policy's limit
        // type is EVENTCOUNTLIMIT") where the array legitimately repeats one value N times; comparing as a multiset
        // would fail those. The count a set cannot see is pinned separately by "should have exactly N entries".
        Set<String> expected = new HashSet<>();
        for (String element : Utils.resolveContextPlaceholders(expectedCsv).split(",")) {
            if (!element.isBlank()) {
                expected.add(element.trim());
            }
        }

        Object actualValue;
        try {
            actualValue = Utils.extractValueFromPayload(response.getData(), fieldName);
        } catch (IOException e) {
            // The path resolves to nothing. Absent is only acceptable when nothing was expected.
            Assert.assertTrue(expected.isEmpty(),
                    String.format("Response array field '%s' is absent but expected exactly %s. Data: %s",
                            fieldName, expected, response.getData()));
            return;
        }
        Assert.assertNotNull(actualValue,
                String.format("Response array field '%s' is null but expected exactly %s. Data: %s",
                        fieldName, expected, response.getData()));

        Set<String> actual = new HashSet<>();
        if (actualValue instanceof Collection<?> elements) {
            for (Object element : elements) {
                actual.add(String.valueOf(element));
            }
        } else if (actualValue instanceof JSONArray array) {
            for (int i = 0; i < array.length(); i++) {
                actual.add(String.valueOf(array.get(i)));
            }
        } else {
            // A path that resolves to a scalar or an object is a real mismatch and must not be read as "empty".
            Assert.fail(String.format("Response field '%s' resolved to a non-array value [%s]; expected a JSON "
                    + "array holding exactly %s. Data: %s", fieldName, actualValue, expected, response.getData()));
        }

        Assert.assertEquals(actual, expected,
                String.format("Response array field '%s' was %s but expected exactly %s. Data: %s",
                        fieldName, actual, expected, response.getData()));
    }

    /**
     * Asserts both substrings are present in the response and that the first occurs BEFORE the second — an
     * order-preserving check (e.g. resource order in a returned swagger) that is robust to server reformatting,
     * unlike matching a whole pre-formatted block verbatim.
     *
     * @param first  the substring expected to appear first
     * @param second the substring expected to appear after the first
     */
    @Then("The response should contain {string} before {string}")
    public void responseShouldContainBefore(String first, String second) {
        first = Utils.resolveContextPlaceholders(first);
        second = Utils.resolveContextPlaceholders(second);
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        String data = response.getData();
        int firstIdx = data.indexOf(first);
        int secondIdx = data.indexOf(second);
        Assert.assertTrue(firstIdx >= 0, "Expected response to contain '" + first + "': " + data);
        Assert.assertTrue(secondIdx >= 0, "Expected response to contain '" + second + "': " + data);
        Assert.assertTrue(firstIdx < secondIdx,
                "Expected '" + first + "' to appear before '" + second + "' in the response: " + data);
    }

    /**
     * Extracts a value from the stored HTTP response payload and saves it in TestContext.
     *
     * @param responseField field name or JSONPath to extract from the response body
     * @param contextKey context key under which the extracted value should be stored
     * @throws IOException if the HTTP response is missing or the field is not found
     */
    @Then("I extract response field {string} and store it as {string}")
    public void iExtractResponseFieldAndStoreItAs(String responseField, String contextKey) throws IOException {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        if (response == null) {
            throw new IOException("No HTTP response found in TestContext.");
        }

        Object value = Utils.extractValueFromPayload(response.getData(), responseField);
        if (value == null) {
            throw new IOException("No value found in response for field: " + responseField);
        }

        if (value instanceof net.minidev.json.JSONArray) {
            value = new JSONArray(value.toString());
        } else if (value instanceof net.minidev.json.JSONObject) {
            value = new JSONObject(value.toString());
        }
        TestContext.set(Utils.normalizeContextKey(contextKey), value);
    }

    /**
     * Captures the {@code id} of the entry called {@code name} in the last response's {@code {"list":[...]}}
     * payload — the generic counterpart of {@link #iExtractResponseFieldAndStoreItAs} for the common "reference
     * a resource whose id cannot be known ahead of time, by its name" need. It reads the response the PRECEDING
     * list step already published, so no second HTTP call is made (and it works for any {@code {"list":[...]}}
     * endpoint). Fails clearly when the response is missing/unsuccessful or holds no entry with that name.
     *
     * @param name       the exact {@code name} of the list entry to find
     * @param contextKey context key under which the entry's id is stored
     */
    @Then("I capture the id of the list entry named {string} as {string}")
    public void iCaptureListEntryIdByName(String name, String contextKey) throws IOException {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response != null && response.getResponseCode() >= 200
                        && response.getResponseCode() < 300 && response.getData() != null
                        && !response.getData().isBlank(),
                "No successful list response with a body captured to look up entry '" + name + "'; got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));
        String entryName = Utils.resolveContextPlaceholders(name);
        String id = Utils.extractIdByName(response.getData(), entryName);
        Assert.assertNotNull(id, "No entry named '" + entryName + "' in the list response: " + response.getData());
        TestContext.set(Utils.normalizeContextKey(contextKey), id);
    }

    /**
     * Extracts a field or JSONPath value from a JSON payload stored in TestContext
     * and stores the extracted value back in TestContext.
     *
     * @param sourceContextKey context key containing the source JSON payload
     * @param fieldPath field name or JSONPath to extract from the stored payload
     * @param targetContextKey context key under which the extracted value should be stored
     * @throws IOException if the source payload is missing or the field is not found
     */
    @And("I extract field {string} from json payload {string} and store it as {string}")
    public void iExtractFieldFromJsonPayloadAndStoreItAs(String fieldPath, String sourceContextKey,
                                                         String targetContextKey) throws IOException {

        Object sourceValue = TestContext.resolve(sourceContextKey);

        Object value = Utils.extractValueFromPayload(String.valueOf(sourceValue), fieldPath);
        if (value == null) {
            throw new IOException("No value found in payload for field: " + fieldPath);
        }

        if (value instanceof net.minidev.json.JSONArray) {
            value = new JSONArray(value.toString());
        } else if (value instanceof net.minidev.json.JSONObject) {
            value = new JSONObject(value.toString());
        }

        TestContext.set(Utils.normalizeContextKey(targetContextKey), value);
    }

    /**
     * Copies a context value from one key to another. Useful when a later composite step overwrites a shared key
     * (e.g. {@code generatedAccessToken}) but an earlier value must be preserved — e.g. keeping a REST, GraphQL and
     * WebSocket token side-by-side to invoke all three across a single server restart.
     */
    @And("I copy context value {string} to {string}")
    public void iCopyContextValueTo(String fromKey, String toKey) {
        Object value = TestContext.resolve(fromKey);
        TestContext.set(Utils.normalizeContextKey(toKey), value);
    }

    /**
     * Extracts a field value from a JSONObject stored in TestContext and stores it under another key.
     *
     * @param fieldName JSON field name to extract
     * @param sourceKey TestContext key containing the JSONObject
     * @param targetKey TestContext key to store the extracted value
     */
    @And("I extract field {string} from {string} and store it as {string}")
    public void iExtractFieldFromAndStoreItAs(String fieldName, String sourceKey, String targetKey) {

        Object contextValue = TestContext.resolve(sourceKey);

        if (!(contextValue instanceof JSONObject jsonObject)) {
            throw new IllegalStateException("Expected JSONObject in TestContext for key '" + sourceKey
                            + "' but found: " + contextValue.getClass().getSimpleName());
        }

        if (!jsonObject.has(fieldName)) {
            throw new AssertionError(
                    "Field '" + fieldName + "' not found in object stored under key '" + sourceKey + "'");
        }

        Object extractedValue = jsonObject.get(fieldName);
        TestContext.set(Utils.normalizeContextKey(targetKey), extractedValue);
    }

    /**
     * Verifies that the HTTP response body does not contain the specified string value.
     *
     * @param unexpectedValue The string value that should not be present in the response body
     */
    @Then("The response should not contain {string}")
    public void responseShouldNotContainFieldValue(String unexpectedValue) {

        // Resolve {{contextKey}} placeholders (mirrors "The response should contain") so a captured value — e.g.
        // a deleted tier's name — is matched literally, not as the placeholder text (which would falsely pass).
        String resolved = Utils.resolveContextPlaceholders(unexpectedValue);
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        // A missing RESPONSE is framework misuse (no request was made) and must fail. An empty BODY is not: a
        // 200 with no payload genuinely does not contain the value, which is exactly what this step asserts —
        // requiring a body here failed 8 correct endpoint-security scenarios whose backend answers 200 empty.
        Assert.assertNotNull(response, "No response in the context to check for the absence of '" + resolved + "'");
        String body = response.getData();
        Assert.assertFalse(body != null && body.contains(resolved),
                "Response unexpectedly contains \"" + resolved + "\": " + body);
    }

    /**
     * Verifies that the HTTP response contains a specific header with the expected value.
     *
     * @param headerName The name of the HTTP header to check
     * @param expectedValue The expected value of the header
     */
    @Then("The response should contain the header {string} with value {string}")
    public void responseShouldContainHeaderWithValue(String headerName, String expectedValue) {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response.getHeaders().containsKey(headerName), "Header " + headerName + " not found in response");
        Assert.assertEquals(response.getHeaders().get(headerName), expectedValue, "Header value mismatch for " + headerName);
    }

    /**
     * Verifies that the HTTP response does not contain a specific header with the specified value.
     *
     * @param headerName The name of the HTTP header to check
     * @param expectedValue The value that should not be present in the header
     */
    @And("The response should not contain the header {string} with value {string}")
    public void theResponseShouldNotContainTheHeaderWithValue(String headerName, String expectedValue) {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertFalse(response.getHeaders().containsKey(headerName), "Header " + headerName + "found in response");
        Assert.assertNotEquals(response.getHeaders().get(headerName), expectedValue, "Header value match for " + headerName);

    }

    /**
     * Asserts a response header is present and its value CONTAINS the given substring (case-insensitive header
     * lookup). The substring variant our exact-value {@code should contain the header X with value Y} cannot
     * express — needed e.g. for a forwarded {@code Location} header whose host segment may be rewritten, so only
     * the path portion is asserted.
     */
    @Then("The response header {string} should contain {string}")
    public void responseHeaderShouldContain(String headerName, String expected) {
        String resolved = Utils.resolveContextPlaceholders(expected);
        String actual = responseHeaderValue(headerName);
        Assert.assertNotNull(actual, "Response header '" + headerName + "' is not present");
        Assert.assertTrue(actual.contains(resolved),
                "Response header '" + headerName + "' (" + actual + ") does not contain '" + resolved + "'");
    }

    /** Asserts a response header is present and its value does NOT contain the substring (e.g. no doubled slash). */
    @Then("The response header {string} should not contain {string}")
    public void responseHeaderShouldNotContain(String headerName, String unexpected) {
        String resolved = Utils.resolveContextPlaceholders(unexpected);
        String actual = responseHeaderValue(headerName);
        Assert.assertNotNull(actual, "Response header '" + headerName + "' is not present");
        Assert.assertFalse(actual.contains(resolved),
                "Response header '" + headerName + "' (" + actual + ") unexpectedly contains '" + resolved + "'");
    }

    /**
     * Asserts a response header is entirely ABSENT (case-insensitive lookup) — distinct from
     * {@code should not contain the header X with value Y} (which pins a specific value). Needed for the CORS
     * negative case: a pre-flight-only header (e.g. {@code Access-Control-Allow-Methods}) must not appear on a
     * normal, non-pre-flight response.
     */
    @Then("The response should not contain the header {string}")
    public void responseShouldNotContainHeader(String headerName) {
        String actual = responseHeaderValue(headerName);
        Assert.assertNull(actual,
                "Response header '" + headerName + "' unexpectedly present with value '" + actual + "'");
    }

    /** Case-insensitive lookup of a response header value from the stored httpResponse (null if absent). */
    private String responseHeaderValue(String headerName) {
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertNotNull(response, "No response captured");
        Map<String, String> headers = response.getHeaders();
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(headerName)) {
                    return e.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Verifies that a resource reflects an updated configuration value.
     *
     * @param resourceType The type of resource to check
     * @param config The configuration field name to verify
     * @param expectedConfigValue The expected configuration value
     */
    @And("The {string} resource should reflect the updated {string} as:")
    public void theResourceShouldReflectTheUpdatedAs(String resourceType, String config, String expectedConfigValue) throws IOException, InterruptedException {
        // Get the API ID from the update response — guard before parsing (a cleared/failed update leaves no
        // response, and an empty body would throw an opaque JSONException instead of a clear failure).
        HttpResponse updateResponse = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(updateResponse != null && updateResponse.getData() != null
                        && !updateResponse.getData().isBlank(),
                "No update response with a body captured to verify the '" + config + "' update against");
        JSONObject updateResponseJson = new JSONObject(updateResponse.getData());
        String resourceId = updateResponseJson.optString("id", null);
        User actor = Identity.actingActor();
        String tenantDomain = actor.getUserDomain();

        if ("endpointConfig".equals(config)){
            expectedConfigValue = TestContext.resolve(expectedConfigValue).toString();
        }

        Object parsedExpectedValue = Utils.parseConfigValue(expectedConfigValue);
        String normalizedConfigValue = String.valueOf(parsedExpectedValue);

        if ("provider".equals(config)) {
            if (tenantDomain != null && !Constants.SUPER_TENANT_DOMAIN.equals(tenantDomain)) {
                if (!normalizedConfigValue.contains("@")) {
                    normalizedConfigValue = normalizedConfigValue + "@" + tenantDomain;
                }
            }
        }

        if (resourceId == null || resourceId.isBlank()) {
            verifyConfigurationInResponse(updateResponse, config, normalizedConfigValue);
            return;
        }

        // Retry mechanism: retrieve the API and check until the configuration matches
        int maxRetries = 30;
        int delayMs = 4000;
        boolean configMatches = false;
        HttpResponse retrievedResponse = null;

        for (int i = 0; i < maxRetries; i++) {
            log.info("[Attempt " + i + "/" + maxRetries + "] Fetching resource " + resourceType + " with ID: "
                    + resourceId);
            Map<String, String> headers = new HashMap<>();
            headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                    "Bearer " + Identity.publisherToken(actor));

            retrievedResponse = SimpleHTTPClient.getInstance().doGet(
                    Utils.getResourceEndpointURL(getBaseUrl(), resourceType, resourceId), headers);

            if (retrievedResponse.getResponseCode() == 200) {
                try {
                    verifyConfigurationInResponse(retrievedResponse, config, normalizedConfigValue);
                    configMatches = true;
                    break;
                } catch (AssertionError e) {
                    // Configuration doesn't match yet, retry
                    if (i < maxRetries - 1) {
                        Thread.sleep(delayMs);
                    } else {
                        throw e;
                    }
                }
            } else {
                if (i == 0) {
                    verifyConfigurationInResponse(updateResponse, config, normalizedConfigValue);
                    return;
                }
                Thread.sleep(delayMs);
            }
        }

        // Final fall back
        if (!configMatches) {
            log.warn("Criteria not met. Falling back to initial update response verification.");
            verifyConfigurationInResponse(updateResponse, config, normalizedConfigValue);
        }
    }

    @And("The {string} resource should reflect the updated {string} as value from context {string}")
    public void theResourceShouldReflectTheUpdatedAsValueFromContext(String resourceType, String config,
               String configValueContextKey) throws IOException, InterruptedException {

        Object ctxValue = TestContext.resolve(configValueContextKey);
        theResourceShouldReflectTheUpdatedAs(resourceType, config, ctxValue.toString());
    }

    /**
     * Helper method that verifies a specific configuration field in the HTTP response matches the expected value.
     *
     * @param response The HTTP response containing the configuration to verify
     * @param config The configuration field name to check
     * @param configValue The expected configuration value
     */
    private void verifyConfigurationInResponse(HttpResponse response, String config, String configValue) {
        Assert.assertTrue(response != null && response.getData() != null && !response.getData().isBlank(),
                "No response with a body to verify configuration '" + config + "' in; got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));
        JSONObject json = new JSONObject(response.getData());
        Assert.assertTrue(json.has(config), "Configuration '" + config + "' not found in response");

        Object actualValue = json.get(config);

        // Handle JSON true/false, numbers, or strings
        if (actualValue instanceof Boolean) {
            Assert.assertEquals(actualValue.toString(), configValue,
                    "Expected boolean " + configValue + " but got " + actualValue);
        } else if (actualValue instanceof Number) {
            Assert.assertEquals(String.valueOf(actualValue), configValue,
                    "Expected numeric " + configValue + " but got " + actualValue);
        } else if (actualValue instanceof JSONArray) {
            JSONArray expectedArray = new JSONArray(configValue);
            JSONArray actualArray = (JSONArray) actualValue;

            Assert.assertTrue(actualArray.similar(expectedArray),
                    "JSON Arrays do not match. Expected (order-insensitive): " + expectedArray
                            + "But got: " + actualArray);

        } else if (actualValue instanceof JSONObject) {
            JSONObject expectedObject = new JSONObject(configValue);
            JSONObject actualObject = (JSONObject) actualValue;

            Assert.assertTrue(actualObject.similar(expectedObject), "Expected JSON object:\n" + expectedObject
                    + "\nbut got:\n" + actualObject);
        } else {
            Assert.assertEquals(actualValue.toString(), configValue,
                    "Expected string " + configValue + " but got " + actualValue);
        }
    }

    /**
     * Waits for the APIM server to be ready by polling the gateway health check endpoint.
     */
    @Then("I wait for the APIM server to be ready")
    public void waitForAPIMServerToBeReady() {

        boolean isServerReady = ServerReadiness.awaitReady(getBaseUrl());
        // Report the window awaitReady ACTUALLY used (its no-arg overload passes SERVER_STARTUP_WAIT_TIME);
        // quoting the propagation timeout here understated the real wait and misled triage.
        Assert.assertTrue(isServerReady, "APIM server is not ready even after waiting for "
                + Constants.SERVER_STARTUP_WAIT_TIME / 60000 + " minutes");
    }

    /**
     * Waits for an API to be deployed in the gateway.
     *
     * @param apiDetailsPayload Context key containing the API details JSON payload
     */
    @Then("I wait for deployment of the resource in {string}")
    public void waitForAPIDeployment(String apiDetailsPayload) throws IOException, InterruptedException {

        String actualApiDetailsPayload = TestContext.resolve(apiDetailsPayload).toString();

        String apiName  = Utils.extractValueFromPayload(actualApiDetailsPayload, "name").toString();
        String apiVersion = Utils.extractValueFromPayload(actualApiDetailsPayload, "version").toString();
        // The deployed-revisions list is the publisher-plane distinguishing state — it flips as soon as the
        // revision is deployed, so it is available to the same actor that owns the API.
        String apiId = Utils.extractValueFromPayload(actualApiDetailsPayload, "id").toString();
        // Use the tenant ADMIN (not the acting actor) — the gateway-artifact admin endpoint requires admin
        // credentials, which a least-privilege publisher actor does not have.
        User tenantAdmin = Identity.actingTenantAdmin();
        String tenantDomain = tenantAdmin.getUserDomain();

        String artifactUrl = Utils.getAPIArtifactDeployedInGatewayURL(getBaseUrl(), apiName, apiVersion, tenantDomain);

        String encodedCredentials = Base64.getEncoder().encodeToString(
                (tenantAdmin.getUserName() + ':' + tenantAdmin.getPassword()).getBytes(StandardCharsets.UTF_8));
        Map<String, String> artifactHeaders = new HashMap<>();
        artifactHeaders.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic " + encodedCredentials);

        String revisionsUrl = Utils.getRevisionDeployments(getBaseUrl(), "apis", apiId);
        Map<String, String> revisionsHeaders = new HashMap<>();
        revisionsHeaders.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        // De-flake (load-induced eventual-consistency, same class as the saml2-bearer clock-skew fix):
        // when many freshly-imported APIs pile up in one serially-loaded block, the synapse ARTIFACT
        // materialization behind the gateway api-artifact endpoint lags well past the standard
        // RUNTIME_PROPAGATION_TIMEOUT window,
        // even though the revision IS deployed. So we (1) poll the correct distinguishing signal — the
        // deployed-revisions list, which flips on publisher-plane deployment — as the primary readiness gate,
        // accepting the gateway-artifact 200 only as a secondary confirmation, (2) catch the transient set on
        // either probe — IOException (gateway warm-up) and JSONException (a not-yet-well-formed body) — so a
        // programming error (bad context key, NPE) still fails fast instead of being masked as a deploy
        // timeout, and (3) lengthen the window to a freshly-imported-under-load budget. Fast cases still
        // exit early on the first positive poll.
        long waitTimeStart = System.currentTimeMillis();
        long waitTime = waitTimeStart + (2 * Constants.RUNTIME_PROPAGATION_TIMEOUT);
        boolean isApiDeployed = false;

        while (System.currentTimeMillis() < waitTime) {
            try {
                HttpResponse revisionsResponse = SimpleHTTPClient.getInstance().doGet(revisionsUrl, revisionsHeaders);
                if (revisionsResponse != null && revisionsResponse.getResponseCode() == 200
                        && new JSONObject(revisionsResponse.getData()).getJSONArray("list").length() > 0) {
                    isApiDeployed = true;
                    break;
                }
                HttpResponse artifactResponse = SimpleHTTPClient.getInstance().doGet(artifactUrl, artifactHeaders);
                if (artifactResponse != null && artifactResponse.getResponseCode() == 200) {
                    isApiDeployed = true;
                    break;
                }
            } catch (IOException | JSONException e) {
                log.warn("API: " + apiName + " with version: " + apiVersion + " not yet deployed in tenant: " +
                        tenantDomain + " – retrying");
            }
            try {
                log.info("Wait for availability of API: " + apiName + " with version: " + apiVersion +
                        " in tenant " + tenantDomain);
                Utils.pollPause(waitTimeStart, 500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        Assert.assertTrue(isApiDeployed, "API " + apiName + " v" + apiVersion +
                " was not deployed within the timeout");
        Thread.sleep(10000);
    }


    /**
     * Extracts a value from a JSON payload using the given path and stores it in the test context.
     *
     * @param payloadContextKey the context key of the JSON payload
     * @param path the JSON path used to extract the value
     * @param outputContextKey the context key used to store the extracted value
     */
    @When("I get the value from json payload {string} at path {string} and store it as {string}")
    public void iGetTheValueFromPayloadAtPathAndStoreItAs(String payloadContextKey, String path,
                                                          String outputContextKey) throws IOException {

        Object ctxValue = TestContext.resolve(payloadContextKey);
        Object value = Utils.extractValueFromPayload(ctxValue.toString(), path);
        TestContext.set(Utils.normalizeContextKey(outputContextKey), String.valueOf(value));
    }

    /**
     * Appends a parsed JSON value to the JSON array stored in the test context.
     *
     * @param arrayContextKey the context key of the target JSON array
     * @param valueToAppend the JSON value to append to the array
     */
    @And("I append the following value to the json array {string}:")
    public void iAppendTheFollowingValueToTheJsonArray(String arrayContextKey, String valueToAppend) {

        Object ctxValue = TestContext.resolve(arrayContextKey);
        JSONArray jsonArray;
        // Convert the stored context value into a JSONArray
        if (ctxValue instanceof JSONArray) {
            jsonArray = (JSONArray) ctxValue;
        } else {
            jsonArray = new JSONArray(ctxValue.toString());
        }
        Object parsedValue = Utils.parseConfigValue(valueToAppend);
        jsonArray.put(parsedValue);
        TestContext.set(Utils.normalizeContextKey(arrayContextKey), jsonArray.toString());
    }

    /**
     * Finds a resource in the JSON array stored in the given context key using
     * the provided property-value pairs, and stores the matched object in TestContext.
     * Expected DataTable format:
     * | name    | addHeader |
     * | version | v1        |
     *
     * @param contextKey the TestContext key containing the JSONArray
     * @param outputKey the TestContext key to store the matched JSONObject
     * @param propertiesTable property-value pairs used for matching
     */
    @And("I find the resource with following properties in {string} as {string}")
    public void iFindTheResourceWithFollowingPropertiesInAs(String contextKey, String outputKey,
                                                            DataTable propertiesTable) {

        Object contextValue = TestContext.resolve(contextKey);
        if (!(contextValue instanceof JSONArray jsonArray)) {
            throw new IllegalStateException(
                    "Expected JSONArray in TestContext for key '" + contextKey + "' but found: "
                            + contextValue.getClass().getSimpleName());
        }

        // Get the raw map from the DataTable
        Map<String, String> rawProperties = propertiesTable.asMap(String.class, String.class);
        Map<String, String> resolvedProperties = new HashMap<>();

        // Resolve if values are context keys
        for (Map.Entry<String, String> entry : rawProperties.entrySet()) {
            Object resolvedValue = Utils.resolveIfContextKey(entry.getValue());
            resolvedProperties.put(entry.getKey(), String.valueOf(resolvedValue));
        }

        JSONObject matchedObject = Utils.findMatchingJsonObjectInArray(jsonArray, resolvedProperties);
        TestContext.set(Utils.normalizeContextKey(outputKey), matchedObject);
    }

    /**
     * Verifies that the actual value stored in context matches the expected value.
     * Expected value can be a literal string or a context key.
     *
     * @param actualKey TestContext key containing the actual value
     * @param expectedValue expected value as a string
     */
    @Then("the actual value of {string} should match the expected value:")
    public void theActualValueShouldMatchTheExpectedValue(String actualKey, String expectedValue) {

        Object actualValue = TestContext.resolve(actualKey);
        String finalExpectedValue = Utils.resolveIfContextKey(expectedValue).toString();
        Utils.assertConfigValueMatchesExpectedValue(actualValue, finalExpectedValue);
    }

    /**
     * Asserts a value previously stored in context CONTAINS the given substring. The exact-match step above
     * cannot express this for values that are legitimately larger than what is being asserted — a rendered HTML
     * page, or the joined {@code Set-Cookie} headers of one response — where the assertion is that a specific
     * token is present in it. {@code {{contextKey}}} placeholders in the expected substring are resolved, so a
     * scenario can assert on a runtime-generated identifier.
     *
     * @param actualKey        TestContext key holding the value to search
     * @param expectedFragment the substring that must be present
     */
    @Then("The stored value {string} should contain {string}")
    public void theStoredValueShouldContain(String actualKey, String expectedFragment) {

        String actual = TestContext.resolve(actualKey).toString();
        String expected = Utils.resolveContextPlaceholders(expectedFragment);
        Assert.assertTrue(actual.contains(expected),
                "Stored value '" + actualKey + "' does not contain '" + expected + "'. Value: " + actual);
    }

    /**
     * Asserts a value previously stored in context is EXACTLY the given literal, with {@code {{contextKey}}}
     * placeholders resolved in that literal first. Distinct from {@code The stored value X should equal Y},
     * which compares two context KEYS: this one compares a stored value against an expected literal that is
     * partly composed of runtime identifiers (e.g. an OAuth service-provider name built from an application
     * UUID). The exact-match step {@code the actual value of X should match the expected value:} cannot be used
     * for those — it resolves only whole-value {@code <key>} references, not embedded placeholders.
     *
     * @param actualKey     TestContext key holding the value to check
     * @param expectedValue the expected value, possibly containing {@code {{contextKey}}} placeholders
     */
    @Then("The stored value {string} should be {string}")
    public void theStoredValueShouldBe(String actualKey, String expectedValue) {

        String actual = TestContext.resolve(actualKey).toString();
        String expected = Utils.resolveContextPlaceholders(expectedValue);
        Assert.assertEquals(actual, expected, "Stored value '" + actualKey + "' is not the expected value");
    }

    /**
     * Asserts the WHOLE response body is JSON equal — STRICTLY, i.e. no extra and no missing members at any
     * depth — to a classpath JSON fixture. Use it where the endpoint's contract is "give back exactly this
     * object": the publisher linter-custom-rules read returns the tenant-config {@code LinterCustomRules} block
     * verbatim, so comparing against the very fixture that seeded it is the only assertion that proves the
     * round trip. A {@code The response should contain} check cannot: it passes on a body that dropped or
     * rewrote every rule but one.
     *
     * <p>Strict is deliberate. {@link Utils#assertConfigValueMatchesExpectedValue} compares LENIENTly (extra
     * members tolerated), which is right for "the field I set is now present" but would let this endpoint
     * silently return the seeded rules PLUS unrelated content and still pass.</p>
     *
     * @param jsonFilePath classpath path of the expected JSON document
     */
    @Then("The response body should equal the JSON file {string}")
    public void theResponseBodyShouldEqualJsonFile(String jsonFilePath) throws IOException {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isBlank(),
                "Expected a 2xx response with a body to compare against '" + jsonFilePath + "', but got: "
                        + (response == null ? "null" : response.getResponseCode() + " / " + response.getData()));
        String expected = Utils.readClasspathResource(jsonFilePath);
        try {
            JSONAssert.assertEquals(expected, response.getData(), JSONCompareMode.STRICT);
        } catch (JSONException e) {
            throw new AssertionError("Response body is not valid JSON or could not be compared against '"
                    + jsonFilePath + "': " + response.getData(), e);
        }
    }
}
