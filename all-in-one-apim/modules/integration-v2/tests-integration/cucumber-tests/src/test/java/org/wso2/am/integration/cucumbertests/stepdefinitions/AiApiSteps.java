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
import org.apache.commons.io.IOUtils;
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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Admin-plane AI Service Provider glue (ports the provider half of AIAPITestCase): list the predefined
 * providers and register a custom one. The custom no-auth provider is the prerequisite for creating an AIAPI
 * subtype API whose backend is a mock LLM (built-in providers require real LLM credentials).
 */
public class AiApiSteps {

    private String getBaseUrl() {
        return TestContext.get("baseUrl").toString();
    }

    /** Admin — GET /ai-service-providers (lists predefined + custom providers). Stores the response. */
    @When("I retrieve the AI service providers")
    public void iRetrieveAiServiceProviders() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = Requests.get(Utils.getAIServiceProvidersURL(getBaseUrl()), headers);
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
        File defFile = classpathToTempFile(defPath, ".json");

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        Map<String, String> formFields = new HashMap<>();
        formFields.put("name", name);
        formFields.put("apiVersion", apiVersion);
        formFields.put("description", "AI service provider for integration tests (no-auth copy of MistralAI)");
        formFields.put("configurations", configurations);
        Map<String, File> files = new HashMap<>();
        files.put("apiDefinition", defFile);

        HttpResponse response = Requests.postMultipart(Utils.getAIServiceProvidersURL(getBaseUrl()), headers,
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
        File defFile = classpathToTempFile(defPath, ".json");

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

        HttpResponse response = Requests.postMultipart(Utils.getAIServiceProvidersURL(getBaseUrl()), headers,
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
        File defFile = classpathToTempFile(defPath, ".json");

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
                Utils.getAIServiceProviderByIdURL(getBaseUrl(), id.toString()), headers, files, formFields);
    }

    /** Admin — GET /ai-service-providers/{id}. Retrieves a single provider (e.g. to assert an update persisted). */
    @When("I retrieve the AI service provider {string}")
    public void iRetrieveAiServiceProvider(String idKey) throws Exception {
        Object id = TestContext.resolve(idKey);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = Requests.get(Utils.getAIServiceProviderByIdURL(getBaseUrl(), id.toString()), headers);
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
        HttpResponse response = Requests.get(Utils.getAIServiceProviderModelsURL(getBaseUrl(), id.toString()),
                headers);
    }

    /** Admin — DELETE /ai-service-providers/{id} — the explicit delete scenarios use to assert removal.
     *  Teardown is additionally covered by ResourceCleanup (the create side registers the provider). */
    @When("I delete the AI service provider {string}")
    public void iDeleteAiServiceProvider(String idKey) throws Exception {
        Object id = TestContext.resolve(idKey);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = Requests.delete(Utils.getAIServiceProviderByIdURL(getBaseUrl(), id.toString()),
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
        HttpResponse listResp = SimpleHTTPClient.getInstance().doGet(Utils.getCommonPolicy(getBaseUrl()), headers);
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
                .doGet(Utils.getResourceEndpointURL(getBaseUrl(), "apis", actualApiId), headers);
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

        HttpResponse putResp = Requests.put(Utils.getResourceEndpointURL(getBaseUrl(), "apis", actualApiId), headers,
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
                .doGet(Utils.getResourceEndpointURL(getBaseUrl(), "apis", actualApiId), headers);
        JSONObject api = new JSONObject(getApi.getData());
        api.put("primaryProductionEndpointId", actualEndpointId);
        HttpResponse putResp = Requests.put(Utils.getResourceEndpointURL(getBaseUrl(), "apis", actualApiId), headers,
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

    private File classpathToTempFile(String path, String suffix) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new FileNotFoundException("Resource not found: " + path);
            }
            File temp = File.createTempFile("aidef", suffix);
            temp.deleteOnExit();
            Files.copy(in, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return temp;
        }
    }
}
