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
import org.apache.commons.io.IOUtils;
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
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Admin-plane AI Service Provider glue (ports the provider half of AIAPITestCase): list the predefined
 * providers and register a custom one. The custom no-auth provider is the prerequisite for creating an AIAPI
 * subtype API whose backend is a mock LLM (built-in providers require real LLM credentials).
 */
public class AiApiSteps {

    /** Admin — GET /ai-service-providers (lists predefined + custom providers). Stores the response. */
    @When("I retrieve the AI service providers")
    public void iRetrieveAiServiceProviders() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = Requests.get(Utils.getAIServiceProvidersURL(Utils.getBaseUrl()), headers);
    }

    /**
     * Asserts the captured {@code GET /ai-service-providers} listing carries EVERY named provider, each flagged
     * {@code builtInSupport=true} and each carrying a non-blank {@code id}, {@code apiVersion} and
     * {@code description}. Ports AIAPITestCase#testPredefinedAiServiceProviders, whose point is the whole shipped
     * set (seven providers) and their summary fields — a substring check for one provider name would pass with the
     * other six missing, and would say nothing about the fields.
     *
     * @param namesCsv comma-separated expected provider names
     */
    @Then("The AI service providers {string} should each be listed with built-in support, an id, an apiVersion and a description")
    public void aiServiceProvidersShouldBeBuiltIn(String namesCsv) {
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response != null && response.getResponseCode() == 200
                        && response.getData() != null && !response.getData().isBlank(),
                "Expected a 200 AI-service-provider listing with a body, got: "
                        + (response == null ? "no response" : response.getResponseCode() + " / " + response.getData()));

        JSONArray list = new JSONObject(response.getData()).getJSONArray("list");
        Map<String, JSONObject> byName = new HashMap<>();
        for (int i = 0; i < list.length(); i++) {
            JSONObject provider = list.getJSONObject(i);
            byName.put(provider.optString("name"), provider);
        }

        for (String rawName : namesCsv.split(",")) {
            String name = rawName.trim();
            JSONObject provider = byName.get(name);
            Assert.assertNotNull(provider, "Built-in AI service provider '" + name + "' is missing from the listing; "
                    + "listed names=" + byName.keySet());
            Assert.assertTrue(provider.has("builtInSupport"),
                    "Provider '" + name + "' carries no builtInSupport flag: " + provider);
            Assert.assertTrue(provider.getBoolean("builtInSupport"),
                    "Provider '" + name + "' is not flagged as built-in: " + provider);
            for (String field : new String[] { "id", "apiVersion", "description" }) {
                Assert.assertTrue(provider.has(field) && !provider.isNull(field)
                                && !provider.getString(field).isBlank(),
                        "Provider '" + name + "' has no " + field + ": " + provider);
            }
        }
    }

    /**
     * Asserts the captured provider response's {@code configurations} is EXACTLY the given connector-configuration
     * file, compared as JSON (order- and whitespace-insensitive) rather than as text. This is the assertion that
     * makes a provider UPDATE meaningful: the description alone changing proves nothing about whether the new
     * authentication configuration was applied, so the whole configurations body is pinned before and after.
     * Ports the {@code getConfigurations()} equality of AIAPITestCase#updateCustomAiServiceProvider.
     *
     * @param configPath classpath path to the expected connector configuration JSON
     */
    @Then("The AI service provider configurations should match the file {string}")
    public void aiServiceProviderConfigurationsShouldMatchFile(String configPath) throws Exception {
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response != null && response.getResponseCode() == 200
                        && response.getData() != null && !response.getData().isBlank(),
                "Expected a 200 AI-service-provider response with a body, got: "
                        + (response == null ? "no response" : response.getResponseCode() + " / " + response.getData()));

        JSONObject provider = new JSONObject(response.getData());
        Assert.assertTrue(provider.has("configurations") && !provider.isNull("configurations"),
                "The AI service provider response carries no configurations: " + response.getData());
        // The field is returned as a JSON *string* by the admin API; accept an inlined object too so the assertion
        // does not hinge on that representation choice.
        Object raw = provider.get("configurations");
        JSONObject actual = raw instanceof JSONObject actualObject
                ? actualObject : new JSONObject(raw.toString());
        JSONObject expected = new JSONObject(readClasspath(configPath));

        Assert.assertTrue(expected.similar(actual),
                "The persisted AI service provider configurations differ from " + configPath
                        + ".\nexpected=" + expected + "\nactual=" + actual);
    }

    /**
     * Admin — POST /ai-service-providers (multipart): registers a custom AI service provider. Form fields
     * name/apiVersion/description + the connector {@code configurations} JSON (inline) + the LLM
     * {@code apiDefinition} OpenAPI file. Non-asserting; stores the created id on 2xx. Ports
     * addCustomAiServiceProviderWithNoAuth.
     *
     * @param name       provider name (e.g. TestAIService)
     * @param apiVersion provider api version (e.g. 1.0.0)
     * @param configPath classpath path to the connector configuration JSON
     * @param defPath    classpath path to the LLM OpenAPI definition
     * @param idKey      context key to store the created provider id under
     */
    @When("I create an AI service provider {string} version {string} with config {string} and definition {string} as {string}")
    public void iCreateAiServiceProvider(String name, String apiVersion, String configPath, String defPath,
                                         String idKey) throws Exception {
        String configurations = readClasspath(configPath);
        File defFile = Utils.classpathToTempFile(defPath, "aidef", ".json");

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        Map<String, String> formFields = new HashMap<>();
        formFields.put("name", name);
        formFields.put("apiVersion", apiVersion);
        formFields.put("description", "AI service provider for integration tests (no-auth copy of MistralAI)");
        formFields.put("configurations", configurations);
        Map<String, File> files = new HashMap<>();
        files.put("apiDefinition", defFile);

        HttpResponse response = Requests.postMultipart(Utils.getAIServiceProvidersURL(Utils.getBaseUrl()), headers,
                files, formFields);
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object createdId = Utils.extractValueFromPayload(response.getData(), "id");
            TestContext.set(idKey, createdId);
            // Register for hook teardown — swept AFTER the AIAPI-subtype APIs that reference it (an AI provider
            // delete is blocked by a foreign-key while any API still binds it). This also makes provider cleanup
            // idempotent and failure-safe, instead of relying on an inline best-effort delete step.
            ResourceCleanup.register(ResourceCleanup.CREATED_AI_PROVIDER_IDS, createdId);
        }
    }

    /**
     * As {@link #iCreateAiServiceProvider} but ALSO registers a model list on the provider via the
     * {@code modelProviders} form field ({@code [{"models":[...],"name":"<provider>"}]}) — needed so the
     * provider's model list can later be retrieved and asserted. {@code models} is a comma-separated list.
     * Ports the model-registering half of AIAPITestCase's provider setup.
     */
    @When("I create an AI service provider {string} version {string} with config {string} definition {string} and models {string} as {string}")
    public void iCreateAiServiceProviderWithModels(String name, String apiVersion, String configPath, String defPath,
                                                   String models, String idKey) throws Exception {
        String configurations = readClasspath(configPath);
        File defFile = Utils.classpathToTempFile(defPath, "aidef", ".json");

        JSONArray modelArray = new JSONArray();
        for (String m : models.split(",")) {
            modelArray.put(m.trim());
        }
        String modelProviders = new JSONArray()
                .put(new JSONObject().put("models", modelArray).put("name", name)).toString();

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        Map<String, String> formFields = new HashMap<>();
        formFields.put("name", name);
        formFields.put("apiVersion", apiVersion);
        formFields.put("description", "AI service provider for integration tests (with a registered model list)");
        formFields.put("configurations", configurations);
        formFields.put("modelProviders", modelProviders);
        Map<String, File> files = new HashMap<>();
        files.put("apiDefinition", defFile);

        HttpResponse response = Requests.postMultipart(Utils.getAIServiceProvidersURL(Utils.getBaseUrl()), headers,
                files, formFields);
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object createdId = Utils.extractValueFromPayload(response.getData(), "id");
            TestContext.set(idKey, createdId);
            ResourceCleanup.register(ResourceCleanup.CREATED_AI_PROVIDER_IDS, createdId);
        }
    }

    /**
     * Admin — PUT /ai-service-providers/{id} (multipart): updates a provider's configurations/description. Ports
     * the update-provider case of AIAPITestCase. Non-asserting; stores the response.
     */
    @When("I update the AI service provider {string} named {string} version {string} to config {string} with definition {string} and description {string}")
    public void iUpdateAiServiceProvider(String idKey, String name, String apiVersion, String configPath,
                                         String defPath, String description) throws Exception {
        Object id = TestContext.resolve(idKey);
        String configurations = readClasspath(configPath);
        File defFile = Utils.classpathToTempFile(defPath, "aidef", ".json");

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        Map<String, String> formFields = new HashMap<>();
        formFields.put("name", name);
        formFields.put("apiVersion", apiVersion);
        formFields.put("description", description);
        formFields.put("configurations", configurations);
        Map<String, File> files = new HashMap<>();
        files.put("apiDefinition", defFile);

        HttpResponse response = Requests.putMultipart(
                Utils.getAIServiceProviderByIdURL(Utils.getBaseUrl(), id.toString()), headers, files, formFields);
    }

    /** Admin — GET /ai-service-providers/{id}. Retrieves a single provider (e.g. to assert an update persisted). */
    @When("I retrieve the AI service provider {string}")
    public void iRetrieveAiServiceProvider(String idKey) throws Exception {
        Object id = TestContext.resolve(idKey);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = Requests.get(Utils.getAIServiceProviderByIdURL(Utils.getBaseUrl(), id.toString()), headers);
    }

    /**
     * Selects ONE built-in provider out of the captured {@code GET /ai-service-providers} listing by
     * {@code name} AND {@code apiVersion}, asserts its {@code deprecated} flag is EXACTLY the expected boolean,
     * and stores its id for the definition fetch that follows.
     *
     * <p>Name alone cannot address a shipped provider: {@code Gemini} ships twice (1.0.0 and 1.1.0) and so does
     * {@code AzureOpenAI} (1.0.0 and 2.0.0). That is the whole point of this step —
     * {@link #aiServiceProvidersShouldBeBuiltIn(String)} keys its map on {@code name} only, so the later of a
     * duplicated pair silently overwrites the earlier one and a version-specific claim cannot be made through it.
     * And the versions differ in a way this suite depends on: Gemini 1.0.0 is the deprecated one and its shipped
     * OAS carries {@code x-throttling-tier: Unlimited} on every operation, while 1.1.0 carries no WSO2 extensions
     * at all — so an API imported from 1.1.0 takes its operation tiers from the server's default, which is what
     * the unlimited-tier-disabled scenarios assert.</p>
     *
     * <p>{@code deprecated} is compared as a boolean against the literal {@code "true"}/{@code "false"}, not
     * merely checked for presence: "1.1.0 is not deprecated" is the assertion legacy made
     * (GeminiAPIUnlimitedTierDisabledTestCase asserts {@code assertFalse(provider.get("deprecated"))}), and a
     * presence check would pass on a provider the product had since deprecated.</p>
     *
     * @param name              the provider's name (e.g. {@code Gemini})
     * @param apiVersion        the provider's apiVersion (e.g. {@code 1.1.0})
     * @param expectedDeprecated {@code "true"} or {@code "false"} — the exact expected flag
     * @param idKey             context key to store the matched provider's id under
     */
    @Then("The AI service provider {string} version {string} should be listed with deprecated {string} and stored as {string}")
    public void aiServiceProviderVersionShouldBeListedWithDeprecated(String name, String apiVersion,
            String expectedDeprecated, String idKey) {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response != null && response.getResponseCode() == 200
                        && response.getData() != null && !response.getData().isBlank(),
                "Expected a 200 AI-service-provider listing with a body, got: "
                        + (response == null ? "no response" : response.getResponseCode() + " / " + response.getData()));

        JSONArray list = new JSONObject(response.getData()).getJSONArray("list");
        JSONObject match = null;
        StringBuilder seen = new StringBuilder();
        for (int i = 0; i < list.length(); i++) {
            JSONObject provider = list.getJSONObject(i);
            seen.append(provider.optString("name")).append('/').append(provider.optString("apiVersion")).append(' ');
            if (name.equals(provider.optString("name")) && apiVersion.equals(provider.optString("apiVersion"))) {
                Assert.assertNull(match, "AI service provider '" + name + "' version '" + apiVersion
                        + "' is listed MORE THAN ONCE, so it cannot be addressed by name+version: " + list);
                match = provider;
            }
        }
        Assert.assertNotNull(match, "AI service provider '" + name + "' version '" + apiVersion
                + "' is missing from the listing; listed name/apiVersion pairs=" + seen.toString().trim());
        Assert.assertTrue(match.has("deprecated"),
                "Provider '" + name + "' version '" + apiVersion + "' carries no deprecated flag: " + match);
        Assert.assertEquals(String.valueOf(match.getBoolean("deprecated")), expectedDeprecated,
                "Provider '" + name + "' version '" + apiVersion + "' deprecated flag mismatch: " + match);

        String id = match.optString("id", null);
        Assert.assertTrue(id != null && !id.isBlank(),
                "Provider '" + name + "' version '" + apiVersion + "' has no id: " + match);
        TestContext.set(Utils.normalizeContextKey(idKey), id);
    }

    /**
     * Publisher — GET /ai-service-providers/{id}/api-definition. Retrieves the provider's OWN OpenAPI definition
     * and publishes it as the response under test, so the feature asserts the 200 and then imports the retrieved
     * body verbatim (see the {@code import openapi definition captured as} step). Ports the
     * {@code getAIServiceProviderApiDefinition} half of GeminiAPIUnlimitedTierDisabledTestCase — the API under
     * test must be built from the SHIPPED definition, not from a fixture copy, or the property being asserted
     * (that 1.1.0's definition declares no throttling tiers) would be a property of the fixture instead.
     */
    @When("I retrieve the api definition of AI service provider {string}")
    public void iRetrieveAiServiceProviderApiDefinition(String idKey) throws Exception {
        Object id = TestContext.resolve(idKey);
        Map<String, String> headers = new HashMap<>();
        // apim:llm_provider_read — carried by the admin token, as for the models endpoint above.
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        Requests.get(Utils.getAIServiceProviderApiDefinitionURL(Utils.getBaseUrl(), id.toString()), headers);
    }

    /**
     * Publisher — GET /ai-service-providers/{id}/models. Retrieves a provider's registered model list. Ports
     * AIAPITestCase#testGetServiceProviderModels.
     */
    @When("I retrieve the models of AI service provider {string}")
    public void iRetrieveAiServiceProviderModels(String idKey) throws Exception {
        Object id = TestContext.resolve(idKey);
        Map<String, String> headers = new HashMap<>();
        // The publisher models endpoint requires apim:llm_provider_read — carried by the admin token (llm provider
        // scopes are admin-oriented; the publisher token's role does not grant them).
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = Requests.get(Utils.getAIServiceProviderModelsURL(Utils.getBaseUrl(), id.toString()),
                headers);
    }

    /** Admin — DELETE /ai-service-providers/{id} — the explicit delete scenarios use to assert removal.
     *  Teardown is additionally covered by ResourceCleanup (the create side registers the provider). */
    @When("I delete the AI service provider {string}")
    public void iDeleteAiServiceProvider(String idKey) throws Exception {
        Object id = TestContext.resolve(idKey);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = Requests.delete(Utils.getAIServiceProviderByIdURL(Utils.getBaseUrl(), id.toString()),
                headers);
    }

    /**
     * Applies a built-in AI mediation operation policy (e.g. {@code modelWeightedRoundRobin}, {@code modelFailover})
     * at the API level. Looks up the shipped COMMON policy by name to get its id, then GETs the API, injects an
     * {@code apiPolicies.request} entry (policyType {@code common}) whose single {@code parameters.<paramName>}
     * carries the config, and PUTs the API back. The config value's double-quotes are converted to single-quotes —
     * the legacy contract for embedding a JSON config inside a String policy parameter. Ports the round-robin /
     * failover policy-application of AIAPITestCase. {@code {{contextKey}}} placeholders in the config are resolved
     * (e.g. a captured endpoint id).
     *
     * @param policyName the common policy name (must be shipped in the pack)
     * @param paramName  the policy attribute name (weightedRoundRobinConfigs / failoverConfigs)
     * @param configKey  context key (or inline value) holding the config JSON
     * @param apiId      context key holding the API id
     */
    @When("I apply the AI mediation policy {string} with parameter {string} value {string} to API {string}")
    public void iApplyAiMediationPolicy(String policyName, String paramName, String configKey, String apiId)
            throws Exception {
        String actualApiId = TestContext.resolve(apiId).toString();
        String config = Utils.resolveContextPlaceholders(TestContext.resolve(configKey).toString());
        // Legacy contract: the JSON config is embedded in a String parameter with single quotes.
        String singleQuotedConfig = config.replace("\"", "'");

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        // 1. Resolve the shipped common policy's id by name.
        HttpResponse listResp = SimpleHTTPClient.getInstance().doGet(Utils.getCommonPolicy(Utils.getBaseUrl()), headers);
        // Confirm the GET succeeded with a body BEFORE parsing — otherwise new JSONObject(null/"") throws an
        // opaque JSONException/NPE instead of a clear failure.
        Assert.assertTrue(listResp != null && listResp.getResponseCode() >= 200 && listResp.getResponseCode() < 300
                        && listResp.getData() != null && !listResp.getData().isBlank(),
                "Failed to list common policies while resolving '" + policyName + "': expected a 2xx response with a "
                        + "body, got " + (listResp == null ? "no response" : listResp.getResponseCode()
                        + " / body=" + listResp.getData()));
        String policyId = null;
        JSONArray policies = new JSONObject(listResp.getData()).optJSONArray("list");
        for (int i = 0; policies != null && i < policies.length(); i++) {
            JSONObject p = policies.getJSONObject(i);
            if (policyName.equals(p.optString("name"))) {
                policyId = p.optString("id");
                break;
            }
        }
        if (policyId == null) {
            throw new IllegalStateException("Common operation policy '" + policyName + "' not found in the pack");
        }

        // 2. GET the API, 3. inject apiPolicies.request, 4. PUT it back.
        HttpResponse getApi = SimpleHTTPClient.getInstance()
                .doGet(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "apis", actualApiId), headers);
        // Confirm the GET succeeded with a body BEFORE parsing — otherwise new JSONObject(null/"") throws an
        // opaque JSONException/NPE instead of a clear failure.
        Assert.assertTrue(getApi != null && getApi.getResponseCode() >= 200 && getApi.getResponseCode() < 300
                        && getApi.getData() != null && !getApi.getData().isBlank(),
                "Failed to fetch API '" + actualApiId + "' before applying the AI mediation policy: expected a 2xx "
                        + "response with a body, got " + (getApi == null ? "no response" : getApi.getResponseCode()
                        + " / body=" + getApi.getData()));
        JSONObject api = new JSONObject(getApi.getData());
        JSONObject policyEntry = new JSONObject()
                .put("policyName", policyName)
                .put("policyType", "common")
                .put("policyId", policyId)
                .put("parameters", new JSONObject().put(paramName, singleQuotedConfig));
        JSONObject apiPolicies = new JSONObject()
                .put("request", new JSONArray().put(policyEntry))
                .put("response", new JSONArray())
                .put("fault", new JSONArray());
        api.put("apiPolicies", apiPolicies);

        HttpResponse putResp = Requests.put(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "apis", actualApiId), headers,
                api.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Sets an AI API's {@code primaryProductionEndpointId} (GET the API, set the field, PUT it back). Needed for
     * the failover test: the primary production endpoint must be the FAILOVER-TARGET endpoint so the gateway hits
     * it first (429) and the modelFailover policy then retries the fallback model on the default endpoint.
     */
    @When("I set the primary production endpoint of API {string} to {string}")
    public void iSetPrimaryProductionEndpoint(String apiId, String endpointId) throws Exception {
        String actualApiId = TestContext.resolve(apiId).toString();
        String actualEndpointId = TestContext.resolve(endpointId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        HttpResponse getApi = SimpleHTTPClient.getInstance()
                .doGet(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "apis", actualApiId), headers);
        // Confirm the GET succeeded with a body BEFORE parsing — otherwise new JSONObject(null/"") throws an
        // opaque JSONException/NPE instead of a clear failure.
        Assert.assertTrue(getApi != null && getApi.getResponseCode() >= 200 && getApi.getResponseCode() < 300
                        && getApi.getData() != null && !getApi.getData().isBlank(),
                "Failed to fetch API '" + actualApiId + "' before setting its primary production endpoint: expected a "
                        + "2xx response with a body, got " + (getApi == null ? "no response" : getApi.getResponseCode()
                        + " / body=" + getApi.getData()));
        JSONObject api = new JSONObject(getApi.getData());
        api.put("primaryProductionEndpointId", actualEndpointId);
        HttpResponse putResp = Requests.put(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "apis", actualApiId), headers,
                api.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    private String readClasspath(String path) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new FileNotFoundException("Resource not found: " + path);
            }
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        }
    }
}
