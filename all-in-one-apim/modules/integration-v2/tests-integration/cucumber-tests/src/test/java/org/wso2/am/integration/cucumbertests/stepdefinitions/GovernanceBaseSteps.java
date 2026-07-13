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
import org.json.JSONArray;
import org.json.JSONObject;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.ResourceCleanup;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Step definitions for the API Governance capability (/api/am/governance/v1):
 * ruleset CRUD, policy CRUD, and artifact-compliance evaluation.
 *
 * <p>Governance is its own product API with its own token scopes ({@code apim:gov_*}); it is NOT reachable
 * with the standard admin token. Scenarios first mint a governance-scoped token via the step
 * "I have a valid Governance access token as ..."; the steps here send that token (via
 * {@link Identity#governanceToken()}).
 */
public class GovernanceBaseSteps {

    BaseSteps baseSteps = new BaseSteps();

    private String getBaseUrl() {

        return baseSteps.getBaseUrl();
    }

    private Map<String, String> governanceAuthHeaders() {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.governanceToken());
        return headers;
    }

    /**
     * Lists all governance rulesets. Doubles as the token-path probe: a 200 confirms the governance-scoped
     * token was accepted by the governance API.
     */
    @When("I retrieve all governance rulesets")
    public void iRetrieveAllGovernanceRulesets() throws IOException {

        HttpResponse response = Requests.get(Utils.getGovernanceRulesetsURL(getBaseUrl()), governanceAuthHeaders());
    }

    // ---- Ruleset CRUD ----------------------------------------------------------------------------------

    /** Loads a governance fixture (YAML/JSON) off the classpath into a temp file for the multipart upload. */
    private File loadResourceAsTempFile(String resourcePath) throws IOException {

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }
            String suffix = resourcePath.endsWith(".json") ? ".json" : ".yaml";
            File temp = File.createTempFile("ruleset", suffix);
            temp.deleteOnExit();
            Files.copy(inputStream, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return temp;
        }
    }

    /**
     * The multipart form fields for a ruleset create/update. Rule type / artifact type / category / provider
     * are fixed to the spectral REST-API-definition shape the fixtures target.
     */
    private Map<String, String> rulesetFormFields(String name, String description, String documentationLink) {

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("name", name);
        fields.put("description", description);
        fields.put("ruleCategory", "SPECTRAL");
        fields.put("ruleType", "API_DEFINITION");
        fields.put("artifactType", "REST_API");
        fields.put("documentationLink", documentationLink);
        fields.put("provider", "admin");
        return fields;
    }

    private HttpResponse postRuleset(String name, String contentResourcePath, String description,
                                     String documentationLink) throws IOException {

        TestContext.remove("httpResponse");
        Map<String, File> files = new HashMap<>();
        files.put("rulesetContent", loadResourceAsTempFile(contentResourcePath));
        HttpResponse response = Requests.postMultipart(
                Utils.getGovernanceRulesetsURL(getBaseUrl()), governanceAuthHeaders(), files,
                rulesetFormFields(name, description, documentationLink));
        return response;
    }

    /**
     * Creates a governance ruleset from a fixture file, asserts 201, stores the new id under {@code idKey} and
     * registers it for teardown. The name is resolved for {@code ${UNIQUE:...}} so parallel scenarios do not
     * collide.
     */
    @When("I create a governance ruleset {string} from content file {string} as {string}")
    public void iCreateGovernanceRuleset(String nameBase, String contentResourcePath, String idKey)
            throws IOException {

        String name = Utils.resolvePayloadPlaceholders(nameBase);
        HttpResponse response = postRuleset(name, contentResourcePath, "Ruleset created by integration test",
                "https://wso2.com");
        org.testng.Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        Object rulesetId = Utils.extractValueFromPayload(response.getData(), "id");
        TestContext.set(idKey, rulesetId);
        ResourceCleanup.register(Constants.CREATED_GOVERNANCE_RULESET_IDS, rulesetId);
    }

    /**
     * Attempts to create a governance ruleset without asserting success — for negatives (e.g. an invalid
     * ruleset expected to 400). The feature asserts the resulting status/error code.
     */
    @When("I attempt to create a governance ruleset {string} from content file {string}")
    public void iAttemptToCreateGovernanceRuleset(String nameBase, String contentResourcePath)
            throws IOException {

        postRuleset(Utils.resolvePayloadPlaceholders(nameBase), contentResourcePath,
                "Ruleset created by integration test", "https://wso2.com");
    }

    /**
     * Updates a governance ruleset (multipart PUT) with a new name, content file, description and documentation
     * link. Non-asserting — the feature asserts the status and (for a valid update) the reflected fields; the
     * invalid-content negative reuses this same step and asserts a 400.
     */
    @When("I update the governance ruleset {string} with name {string} content file {string} description {string} and documentation link {string}")
    public void iUpdateGovernanceRuleset(String idKey, String nameBase, String contentResourcePath,
                                         String description, String documentationLink) throws IOException {

        String rulesetId = TestContext.resolve(idKey).toString();
        String name = Utils.resolvePayloadPlaceholders(nameBase);
        TestContext.remove("httpResponse");
        Map<String, File> files = new HashMap<>();
        files.put("rulesetContent", loadResourceAsTempFile(contentResourcePath));
        HttpResponse response = Requests.putMultipart(
                Utils.getGovernanceRulesetByIdURL(getBaseUrl(), rulesetId), governanceAuthHeaders(), files,
                rulesetFormFields(name, description, documentationLink));
    }

    /** Retrieves a single governance ruleset by the id held under {@code idKey}. */
    @When("I retrieve the governance ruleset {string}")
    public void iRetrieveGovernanceRuleset(String idKey) throws IOException {

        String rulesetId = TestContext.resolve(idKey).toString();
        HttpResponse response = Requests.get(Utils.getGovernanceRulesetByIdURL(getBaseUrl(), rulesetId), governanceAuthHeaders());
    }

    /** Deletes the governance ruleset held under {@code idKey}. Non-asserting — the feature asserts the status. */
    @When("I delete the governance ruleset {string}")
    public void iDeleteGovernanceRuleset(String idKey) throws IOException {

        String rulesetId = TestContext.resolve(idKey).toString();
        HttpResponse response = Requests.delete(Utils.getGovernanceRulesetByIdURL(getBaseUrl(), rulesetId), governanceAuthHeaders());
    }

    /**
     * Looks up a governance ruleset by name in the org's ruleset list and stores its id under {@code idKey}.
     * Used to reference a built-in default ruleset (e.g. to assert a policy-attached ruleset cannot be deleted).
     */
    @When("I capture the governance ruleset id of {string} as {string}")
    public void iCaptureGovernanceRulesetId(String rulesetName, String idKey) throws IOException {

        HttpResponse response = Requests.get(Utils.getGovernanceRulesetsURL(getBaseUrl()), governanceAuthHeaders());
        org.testng.Assert.assertEquals(response.getResponseCode(), 200, response.getData());
        String id = Utils.extractIdByName(response.getData(), rulesetName);
        org.testng.Assert.assertNotNull(id, "Governance ruleset '" + rulesetName + "' not found in list: "
                + response.getData());
        TestContext.set(idKey, id);
    }

    // ---- Policy CRUD -----------------------------------------------------------------------------------

    /** Builds a governance policy JSON attaching a single ruleset, governing the API_UPDATE state, globally. */
    private String buildPolicyPayload(String name, String description, String rulesetId) {

        JSONObject policy = new JSONObject();
        policy.put("name", name);
        policy.put("description", description);
        policy.put("rulesets", new JSONArray().put(rulesetId));
        policy.put("governableStates", new JSONArray().put("API_UPDATE"));
        policy.put("labels", new JSONArray().put("global"));
        return policy.toString();
    }

    /**
     * Creates a governance policy attaching the ruleset held under {@code rulesetIdKey}, asserts 201, stores the
     * new id under {@code policyIdKey} and registers it for teardown (deleted before its rulesets).
     */
    @When("I create a governance policy {string} attaching ruleset {string} as {string}")
    public void iCreateGovernancePolicy(String nameBase, String rulesetIdKey, String policyIdKey)
            throws IOException {

        String rulesetId = TestContext.resolve(rulesetIdKey).toString();
        String payload = buildPolicyPayload(Utils.resolvePayloadPlaceholders(nameBase),
                "Policy created by integration test", rulesetId);
        HttpResponse response = Requests.post(
                Utils.getGovernancePoliciesURL(getBaseUrl()), governanceAuthHeaders(), payload,
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        org.testng.Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        Object policyId = Utils.extractValueFromPayload(response.getData(), "id");
        TestContext.set(policyIdKey, policyId);
        ResourceCleanup.register(Constants.CREATED_GOVERNANCE_POLICY_IDS, policyId);
    }

    /** Retrieves a single governance policy by the id held under {@code idKey}. */
    @When("I retrieve the governance policy {string}")
    public void iRetrieveGovernancePolicy(String idKey) throws IOException {

        String policyId = TestContext.resolve(idKey).toString();
        HttpResponse response = Requests.get(Utils.getGovernancePolicyByIdURL(getBaseUrl(), policyId), governanceAuthHeaders());
    }

    /** Lists all governance policies. */
    @When("I retrieve all governance policies")
    public void iRetrieveAllGovernancePolicies() throws IOException {

        HttpResponse response = Requests.get(Utils.getGovernancePoliciesURL(getBaseUrl()), governanceAuthHeaders());
    }

    /**
     * Updates a governance policy's description in place: retrieves the current policy, replaces its
     * description, and PUTs the whole payload back. Non-asserting — the feature asserts the status and the
     * reflected description.
     */
    @When("I update the governance policy {string} setting its description to {string}")
    public void iUpdateGovernancePolicyDescription(String idKey, String newDescription) throws IOException {

        String policyId = TestContext.resolve(idKey).toString();
        HttpResponse current = SimpleHTTPClient.getInstance()
                .doGet(Utils.getGovernancePolicyByIdURL(getBaseUrl(), policyId), governanceAuthHeaders());
        // Intermediate GET of a GET→mutate→PUT: confirm a 2xx response WITH a body before parsing, so a
        // failed/empty fetch fails clearly instead of throwing an opaque JSONException/NPE.
        org.testng.Assert.assertTrue(current != null && current.getResponseCode() >= 200
                        && current.getResponseCode() < 300 && current.getData() != null
                        && !current.getData().isEmpty(),
                "Failed to fetch governance policy '" + policyId + "' before updating its description: expected a 2xx "
                        + "response with a body, got " + (current == null ? "no response"
                        : current.getResponseCode() + " / body=" + current.getData()));

        JSONObject policy = new JSONObject(current.getData());
        policy.put("description", newDescription);
        // Drop server-managed read-only fields so the PUT carries only the editable policy shape.
        policy.remove("id");
        policy.remove("createdBy");
        policy.remove("createdTime");
        policy.remove("updatedBy");
        policy.remove("updatedTime");

        HttpResponse response = Requests.put(
                Utils.getGovernancePolicyByIdURL(getBaseUrl(), policyId), governanceAuthHeaders(),
                policy.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Deletes the governance policy held under {@code idKey}. Non-asserting — the feature asserts the status. */
    @When("I delete the governance policy {string}")
    public void iDeleteGovernancePolicy(String idKey) throws IOException {

        String policyId = TestContext.resolve(idKey).toString();
        HttpResponse response = Requests.delete(Utils.getGovernancePolicyByIdURL(getBaseUrl(), policyId), governanceAuthHeaders());
    }

    // ---- Compliance ------------------------------------------------------------------------------------

    /** The org's built-in default ruleset names — a bare API violates these, so they drive compliance failures. */
    private static final String[] DEFAULT_RULESET_NAMES = {
            "WSO2 API Management Guidelines", "WSO2 REST API Design Guidelines", "OWASP Top 10"
    };

    /** Collects the ids of the org's built-in default rulesets from the ruleset list. */
    private JSONArray defaultRulesetIds() throws IOException {

        HttpResponse response = Requests.get(Utils.getGovernanceRulesetsURL(getBaseUrl()), governanceAuthHeaders());
        org.testng.Assert.assertEquals(response.getResponseCode(), 200, response.getData());
        JSONArray ids = new JSONArray();
        for (String name : DEFAULT_RULESET_NAMES) {
            String id = Utils.extractIdByName(response.getData(), name);
            if (id != null) {
                ids.put(id);
            }
        }
        org.testng.Assert.assertTrue(ids.length() > 0, "No default rulesets found to attach: " + response.getData());
        return ids;
    }

    /**
     * Creates a governance policy attaching the built-in default rulesets with a BLOCK action on API_DEPLOY, so
     * a non-compliant API is blocked from deployment (revision creation). Asserts 201, stores the id and
     * registers it for teardown.
     */
    @When("I create a governance policy {string} attaching the default rulesets blocking API deployment as {string}")
    public void iCreateBlockingGovernancePolicy(String nameBase, String policyIdKey) throws IOException {

        JSONObject action = new JSONObject();
        action.put("state", "API_DEPLOY");
        action.put("ruleSeverity", "WARN");
        action.put("type", "BLOCK");

        JSONObject policy = new JSONObject();
        policy.put("name", Utils.resolvePayloadPlaceholders(nameBase));
        policy.put("description", "Blocking policy created by integration test");
        policy.put("rulesets", defaultRulesetIds());
        policy.put("governableStates", new JSONArray().put("API_DEPLOY"));
        policy.put("labels", new JSONArray().put("global"));
        policy.put("actions", new JSONArray().put(action));

        HttpResponse response = Requests.post(
                Utils.getGovernancePoliciesURL(getBaseUrl()), governanceAuthHeaders(), policy.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        org.testng.Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        Object policyId = Utils.extractValueFromPayload(response.getData(), "id");
        TestContext.set(policyIdKey, policyId);
        ResourceCleanup.register(Constants.CREATED_GOVERNANCE_POLICY_IDS, policyId);
    }

    /** Retrieves the artifact-compliance details of the API held under {@code apiIdKey}. */
    @When("I retrieve the compliance of API {string}")
    public void iRetrieveApiCompliance(String apiIdKey) throws IOException {

        String apiId = TestContext.resolve(apiIdKey).toString();
        HttpResponse response = Requests.get(Utils.getGovernanceApiComplianceURL(getBaseUrl(), apiId), governanceAuthHeaders());
    }

    /**
     * Polls the artifact-compliance of an API until its {@code status} field equals {@code expectedStatus}
     * (compliance evaluation is asynchronous — the status is PENDING until the background job completes). Only
     * transient {@link IOException}s are retried; a bad context key fails fast. The last response is left in
     * context and asserted after the loop, so the step fails on its own.
     */
    @When("I retrieve the compliance of API {string} until the status is {string} within {int} seconds")
    public void iRetrieveApiComplianceUntilStatus(String apiIdKey, String expectedStatus, int timeoutSeconds)
            throws InterruptedException {

        String apiId = TestContext.resolve(apiIdKey).toString();
        String url = Utils.getGovernanceApiComplianceURL(getBaseUrl(), apiId);
        long deadline = System.nanoTime() + timeoutSeconds * 1_000_000_000L;
        HttpResponse last = null;
        String actualStatus = null;
        while (System.nanoTime() < deadline) {
            try {
                last = SimpleHTTPClient.getInstance().doGet(url, governanceAuthHeaders());
                // Only parse a 200 that actually has a body; an empty 200 during warm-up falls through and we
                // keep polling rather than throwing an uncaught JSONException (the catch below is IOException-only).
                if (last.getResponseCode() == 200 && last.getData() != null && !last.getData().isEmpty()) {
                    actualStatus = new JSONObject(last.getData()).optString("status", null);
                    if (expectedStatus.equals(actualStatus)) {
                        break;
                    }
                }
            } catch (IOException transientError) {
                // gateway/management warm-up — retry
            }
            Thread.sleep(5000);
        }
        TestContext.set("httpResponse", last);
        org.testng.Assert.assertNotNull(last, "No compliance response received for API " + apiId);
        org.testng.Assert.assertEquals(actualStatus, expectedStatus,
                "Compliance status did not reach '" + expectedStatus + "' within " + timeoutSeconds
                        + "s; last: " + last.getData());
    }
}
