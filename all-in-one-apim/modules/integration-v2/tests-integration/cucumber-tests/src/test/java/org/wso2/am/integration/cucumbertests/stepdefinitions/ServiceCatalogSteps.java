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
 * Step definitions for the Service Catalog (port of ServiceCatalogRestAPITestCase). Exercises the admin-plane
 * {@code /api/am/service-catalog/v1/services} REST API: create (multipart {@code serviceMetadata} JSON field +
 * {@code definitionFile}), retrieve, retrieve-definition, search (by name/version/type/key + sort + limit/offset),
 * update, usage, and delete. Uses the acting actor's admin token.
 *
 * <p>Requests funnel through {@link Requests} so the response is published as {@code httpResponse} for the generic
 * assertion steps. Created services are registered for failure-safe teardown and swept by {@link ResourceCleanup}
 * (a NEW top-level resource type — see the wired {@code CREATED_SERVICE_CATALOG_IDS} sweep).
 */
public class ServiceCatalogSteps {

    private final BaseSteps baseSteps = new BaseSteps();

    private String getBaseUrl() {
        return baseSteps.getBaseUrl();
    }

    /**
     * Service-catalog auth headers. The Service Catalog REST API is gated by the {@code service_catalog:service_*}
     * scopes (pinned live: the admin/publisher tokens' scopes get 401), so this mints a dedicated token for the
     * acting actor from its DCR credentials (cached per actor), with the write+view service-catalog scopes.
     */
    private Map<String, String> serviceCatalogHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + serviceCatalogToken());
        return headers;
    }

    private String serviceCatalogToken() {
        User actor = Identity.actingActor();
        String cacheKey = "serviceCatalogToken::" + actor.getUserName();
        Object cached = TestContext.get(cacheKey);
        if (cached != null) {
            return cached.toString();
        }
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                    "Basic " + TestContext.get(Identity.dcrCredentialsKey(actor)).toString());
            JSONObject json = new JSONObject();
            json.put("grant_type", "password");
            json.put("username", actor.getUserName());
            json.put("password", actor.getPassword());
            json.put("scope", "service_catalog:service_write service_catalog:service_view");
            HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(getBaseUrl()),
                    headers, json.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
            Assert.assertEquals(response.getResponseCode(), 200, response.getData());
            String token = Utils.extractValueFromPayload(response.getData(), "access_token").toString();
            TestContext.set(cacheKey, token);
            return token;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to obtain a service-catalog token for " + actor.getUserName(), e);
        }
    }

    /** Copies a classpath definition resource to a temp file (the multipart upload needs a {@link File}). */
    private File definitionFile(String resourcePath) throws IOException {
        String suffix = resourcePath.substring(resourcePath.lastIndexOf('.'));
        File temp;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("Definition resource not found on classpath: " + resourcePath);
            }
            temp = File.createTempFile("svc-catalog-def", suffix);
            temp.deleteOnExit();
            Files.copy(in, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return temp;
    }

    /** Builds the serviceMetadata JSON for a create/update, resolving {@code {{...}}} in name/version/key. */
    private String buildMetadata(String name, String version, String key, String description) {
        JSONObject md = new JSONObject();
        md.put("name", Utils.resolveContextPlaceholders(name));
        md.put("version", Utils.resolveContextPlaceholders(version));
        md.put("serviceKey", Utils.resolveContextPlaceholders(key));
        md.put("serviceUrl", "https://localhost:9443/service");
        md.put("definitionType", "OAS3");
        md.put("securityType", "BASIC");
        md.put("mutualSSLEnabled", false);
        if (description != null) {
            md.put("description", description);
        }
        return md.toString();
    }

    private HttpResponse createService(String metadata, String definitionResource) throws IOException {
        Map<String, File> files = new HashMap<>();
        files.put("definitionFile", definitionFile(definitionResource));
        Map<String, String> jsonFields = new HashMap<>();
        // serviceMetadata MUST be an application/json part (a text/plain JSON part is rejected 500 — pinned live).
        jsonFields.put("serviceMetadata", metadata);
        return Requests.postMultipartWithJsonFields(Utils.getServiceCatalogURL(getBaseUrl()),
                serviceCatalogHeaders(), files, new HashMap<>(), jsonFields);
    }

    /**
     * Creates a service (multipart metadata + definition), asserts 200 (the create returns 200, not 201 — pinned
     * live), stores its id under {@code idKey}, and registers it for teardown. Name/version/key resolve
     * {@code {{...}}} so a scenario-unique service flows through.
     */
    @When("I create a service catalog entry named {string} version {string} key {string} from definition {string} as {string}")
    public void iCreateService(String name, String version, String key, String definitionResource, String idKey)
            throws IOException {
        HttpResponse response = createService(buildMetadata(name, version, key, "Catalog entry"), definitionResource);
        Assert.assertEquals(response.getResponseCode(), 200, response.getData());
        Object id = Utils.extractValueFromPayload(response.getData(), "id");
        TestContext.set(idKey, id);
        ResourceCleanup.register(ResourceCleanup.CREATED_SERVICE_CATALOG_IDS, id);
    }

    /**
     * Attempts to create a service WITHOUT asserting success — for the negatives (duplicate key → 409). Publishes
     * the response for the feature to assert; does not register (the create either failed or duplicates an
     * already-registered entry).
     */
    @When("I attempt to create a service catalog entry named {string} version {string} key {string} from definition {string}")
    public void iAttemptToCreateService(String name, String version, String key, String definitionResource)
            throws IOException {
        createService(buildMetadata(name, version, key, "Catalog entry"), definitionResource);
    }

    /** Attempts to create a service with NO definition file (multipart with only serviceMetadata) — expects 400. */
    @When("I attempt to create a service catalog entry named {string} version {string} key {string} without a definition")
    public void iAttemptToCreateServiceWithoutDefinition(String name, String version, String key) throws IOException {
        Map<String, String> formFields = new HashMap<>();
        formFields.put("serviceMetadata", buildMetadata(name, version, key, "Catalog entry"));
        Requests.postMultipart(Utils.getServiceCatalogURL(getBaseUrl()), serviceCatalogHeaders(), new HashMap<>(),
                formFields);
    }

    /**
     * Imports a services archive (a .zip carrying serviceMetadata + definition), asserts 200, and registers the
     * imported service ids for teardown. Publishes the response so the feature can assert the imported name. The
     * archive is a classpath resource (reuses {@link #definitionFile} — the multipart upload needs a File).
     */
    @When("I import a service catalog archive {string} with overwrite {string} as {string}")
    public void iImportServiceArchive(String archiveResource, String overwrite, String idKey) throws IOException {
        File archive = definitionFile(archiveResource);
        Map<String, File> files = new LinkedHashMap<>();
        files.put("file", archive);
        HttpResponse response = Requests.postMultipart(
                Utils.getServiceCatalogImportURL(getBaseUrl(), Boolean.parseBoolean(overwrite)),
                serviceCatalogHeaders(), files, new HashMap<>());
        // Guard status AND body before parsing — a 200 with an empty body would otherwise surface as an opaque
        // JSONException instead of a clear failure.
        Assert.assertTrue(response.getResponseCode() == 200
                        && response.getData() != null && !response.getData().isEmpty(),
                "Service catalog archive import did not return 200 with a body; got "
                        + response.getResponseCode() + " / body=" + response.getData());
        // The response is a ServiceInfoList; register every imported service id for teardown, store the first.
        JSONArray list = new JSONObject(response.getData()).getJSONArray("list");
        for (int i = 0; i < list.length(); i++) {
            Object id = list.getJSONObject(i).get("id");
            ResourceCleanup.register(ResourceCleanup.CREATED_SERVICE_CATALOG_IDS, id);
            if (i == 0) {
                TestContext.set(idKey, id);
            }
        }
    }

    /** Attempts to import a services archive with NO file part — for the missing-file 400 negative. */
    @When("I attempt to import a service catalog archive with no file")
    public void iAttemptToImportWithoutFile() throws IOException {
        Requests.postMultipart(Utils.getServiceCatalogImportURL(getBaseUrl(), true), serviceCatalogHeaders(),
                new HashMap<>(), new HashMap<>());
    }

    /**
     * Attempts to import a services archive at the given overwrite setting WITHOUT asserting success — the feature
     * asserts the status. Used for the overwrite-conflict dimension: an archive whose service name/version already
     * exists is rejected with 400 when {@code overwrite=false} and succeeds (200) when {@code overwrite=true}. On a
     * 2xx it registers every imported service id for teardown. Publishes the response as {@code httpResponse}.
     */
    @When("I attempt to import a service catalog archive {string} with overwrite {string}")
    public void iAttemptToImportServiceArchive(String archiveResource, String overwrite) throws IOException {
        File archive = definitionFile(archiveResource);
        Map<String, File> files = new LinkedHashMap<>();
        files.put("file", archive);
        HttpResponse response = Requests.postMultipart(
                Utils.getServiceCatalogImportURL(getBaseUrl(), Boolean.parseBoolean(overwrite)),
                serviceCatalogHeaders(), files, new HashMap<>());
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            // A 2xx import MUST carry the ServiceInfoList body — guard before parsing so a degenerate empty-body
            // success fails clearly instead of as a JSONException. (Non-2xx negatives skip this branch untouched.)
            Assert.assertTrue(response.getData() != null && !response.getData().isEmpty(),
                    "Service catalog archive import returned " + response.getResponseCode()
                            + " but carried no body to register imported service ids from");
            JSONArray list = new JSONObject(response.getData()).getJSONArray("list");
            for (int i = 0; i < list.length(); i++) {
                ResourceCleanup.register(ResourceCleanup.CREATED_SERVICE_CATALOG_IDS, list.getJSONObject(i).get("id"));
            }
        }
    }

    /** Retrieves a service by the id held under {@code idKey} (publishes the response). */
    @When("I retrieve the service catalog entry {string}")
    public void iRetrieveService(String idKey) throws IOException {
        Requests.get(Utils.getServiceCatalogByIdURL(getBaseUrl(), TestContext.resolve(idKey).toString()),
                serviceCatalogHeaders());
    }

    /** Retrieves a service by a raw (literal) id — for the invalid-id 404 negative. */
    @When("I retrieve the service catalog entry with raw id {string}")
    public void iRetrieveServiceRawId(String rawId) throws IOException {
        Requests.get(Utils.getServiceCatalogByIdURL(getBaseUrl(), rawId), serviceCatalogHeaders());
    }

    /** Retrieves a service's definition (publishes the response). */
    @When("I retrieve the definition of service catalog entry {string}")
    public void iRetrieveServiceDefinition(String idKey) throws IOException {
        Requests.get(Utils.getServiceCatalogDefinitionURL(getBaseUrl(), TestContext.resolve(idKey).toString()),
                serviceCatalogHeaders());
    }

    /** Retrieves a service's usage — the APIs referencing it (publishes the response). */
    @When("I retrieve the usage of service catalog entry {string}")
    public void iRetrieveServiceUsage(String idKey) throws IOException {
        Requests.get(Utils.getServiceCatalogUsageURL(getBaseUrl(), TestContext.resolve(idKey).toString()),
                serviceCatalogHeaders());
    }

    /** Deletes a service by id (publishes the response). */
    @When("I delete the service catalog entry {string}")
    public void iDeleteService(String idKey) throws IOException {
        Requests.delete(Utils.getServiceCatalogByIdURL(getBaseUrl(), TestContext.resolve(idKey).toString()),
                serviceCatalogHeaders());
    }

    /** Searches services by a single field ({@code name}/{@code version}/{@code definitionType}/{@code key}). */
    @When("I search service catalog entries by {string} {string}")
    public void iSearchServicesByField(String field, String value) throws IOException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put(field, Utils.resolveContextPlaceholders(value));
        Requests.get(Utils.getServiceCatalogSearchURL(getBaseUrl(), params), serviceCatalogHeaders());
    }

    /** Searches services by name with an explicit limit and offset (for pagination assertions). */
    @When("I search service catalog entries by name {string} with limit {int} and offset {int}")
    public void iSearchServicesWithPagination(String name, int limit, int offset) throws IOException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("name", Utils.resolveContextPlaceholders(name));
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        Requests.get(Utils.getServiceCatalogSearchURL(getBaseUrl(), params), serviceCatalogHeaders());
    }

    /**
     * Asserts the number of entries in the last service-catalog search/list response. The ServiceListDTO carries
     * NO top-level {@code count} field (pinned live) — it has a {@code list} array (respecting limit/offset) and a
     * {@code pagination.total}. The returned-page size is {@code list.length()}, which is what the limit/offset
     * pagination assertions need.
     */
    @Then("The service catalog search should return {int} entries")
    public void theSearchShouldReturnNEntries(int expected) {
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertNotNull(response, "No service-catalog search response captured");
        Assert.assertTrue(response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isEmpty(),
                "Service-catalog search did not return a 2xx body: got " + response.getResponseCode()
                        + " / " + response.getData());
        JSONArray list = new JSONObject(response.getData()).optJSONArray("list");
        int actual = list == null ? 0 : list.length();
        Assert.assertEquals(actual, expected, "Service-catalog search entry-count mismatch; body: " + response.getData());
    }

    /**
     * Asserts how many services the last IMPORT response reported as (re)imported. The import response is a
     * ServiceInfoList whose {@code list} holds one entry per service actually imported. Re-importing a
     * byte-identical archive whose service already exists is a 200 no-op that imports NOTHING — the existing
     * service is neither duplicated, clobbered, nor rejected, and the API returns an empty list
     * ({@code "count":0,"list":[]}), NOT a 400 (verified live; the legacy suite's 400 assertion sat in a
     * never-firing catch — see the scenario note). This holds regardless of the overwrite flag: with unchanged
     * content there is nothing to overwrite. The flag only produces a non-empty list when the incoming archive
     * DIFFERS from the stored service.
     */
    @Then("The service catalog import should report {int} imported services")
    public void theImportShouldReportNServices(int expected) {
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertNotNull(response, "No service-catalog import response captured");
        Assert.assertTrue(response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isEmpty(),
                "Service-catalog import did not return a 2xx body: got " + response.getResponseCode()
                        + " / " + response.getData());
        JSONArray list = new JSONObject(response.getData()).optJSONArray("list");
        int actual = list == null ? 0 : list.length();
        Assert.assertEquals(actual, expected,
                "Service-catalog import imported-count mismatch; body: " + response.getData());
    }
}
