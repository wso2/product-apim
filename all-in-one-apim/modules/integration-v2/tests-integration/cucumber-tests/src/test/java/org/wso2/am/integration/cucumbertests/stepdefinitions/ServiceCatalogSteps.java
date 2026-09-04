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
 * update, import, export, usage, and delete — plus binding an API to a catalog entry via the API's
 * {@code serviceInfo}, which is what makes a service "in use" (its usage list non-empty and its delete a 409).
 * Uses the acting actor's admin token.
 *
 * <p>Requests funnel through {@link Requests} so the response is published as {@code httpResponse} for the generic
 * assertion steps. Created services are registered for failure-safe teardown and swept by {@link ResourceCleanup}
 * (a NEW top-level resource type — see the wired {@code CREATED_SERVICE_CATALOG_IDS} sweep).
 */
public class ServiceCatalogSteps {

    private final BaseSteps baseSteps = new BaseSteps();

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
        // Cached under the shared Identity key so ResourceCleanup's sweep can delete registered entries with THIS
        // token — the admin token it would otherwise use is rejected 401 by the service-catalog plane.
        String cacheKey = Identity.serviceCatalogTokenKey(actor);
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
            HttpResponse response = SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(Utils.getBaseUrl()),
                    headers, json.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
            // The OAuth2 token endpoint's own 200 (RFC 6749 §5.1) -- unrelated to the service-catalog contract.
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
        return Requests.postMultipartWithJsonFields(Utils.getServiceCatalogURL(Utils.getBaseUrl()),
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
        // Pinned to the product's actual 200; the contract declares 201 (wso2/api-manager#5195).
        // Change to 201 once that is fixed.
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
        Requests.postMultipart(Utils.getServiceCatalogURL(Utils.getBaseUrl()), serviceCatalogHeaders(), new HashMap<>(),
                formFields);
    }

    /**
     * Updates a service (PUT multipart, same shape as the create) WITHOUT asserting success — the feature asserts
     * the status, so a wrongly-successful update cannot pass silently (the legacy suite asserted the update
     * negatives inside {@code catch} blocks that never fire on a 200). Publishes the response so the feature can
     * pin the echoed id/name/version/serviceKey/description. Nothing is registered: the service already exists and
     * was registered by its create.
     *
     * <p>The {@code idKey} is a context key, so the invalid-UUID 404 is expressed by putting a bogus id in context
     * first ({@code I put value ... in context as ...}) rather than by a second raw-id step variant.
     */
    @When("I attempt to update the service catalog entry {string} named {string} version {string} key {string} with description {string} from definition {string}")
    public void iAttemptToUpdateService(String idKey, String name, String version, String key, String description,
                                       String definitionResource) throws IOException {
        Map<String, File> files = new HashMap<>();
        files.put("definitionFile", definitionFile(definitionResource));
        Map<String, String> jsonFields = new HashMap<>();
        jsonFields.put("serviceMetadata", buildMetadata(name, version, key, description));
        Requests.putMultipartWithJsonFields(
                Utils.getServiceCatalogByIdURL(Utils.getBaseUrl(), TestContext.resolve(idKey).toString()),
                serviceCatalogHeaders(), files, new HashMap<>(), jsonFields);
    }

    /**
     * Attempts to update a service with NO definition file (multipart carrying only serviceMetadata) — the
     * missing-definition 400 negative. Non-asserting; the feature asserts the status.
     */
    @When("I attempt to update the service catalog entry {string} named {string} version {string} key {string} with description {string} without a definition")
    public void iAttemptToUpdateServiceWithoutDefinition(String idKey, String name, String version, String key,
                                                         String description) throws IOException {
        Map<String, String> jsonFields = new HashMap<>();
        jsonFields.put("serviceMetadata", buildMetadata(name, version, key, description));
        Requests.putMultipartWithJsonFields(
                Utils.getServiceCatalogByIdURL(Utils.getBaseUrl(), TestContext.resolve(idKey).toString()),
                serviceCatalogHeaders(), new HashMap<>(), new HashMap<>(), jsonFields);
    }

    /**
     * Exports a service addressed by name+version ({@code GET /services/export}) and asserts the download returned
     * {@code expectedStatus}. On a 200 it additionally proves the payload is a REAL, complete archive — a non-empty
     * file that unzips and carries both the service metadata and its definition — so a 200 with an empty or corrupt
     * body fails here instead of looking green. A binary download (a String GET would corrupt the zip bytes) whose
     * status travels on the DownloadResult, so nothing is published to {@code httpResponse}. Name/version resolve
     * {@code {{...}}} so a scenario-unique service flows through; the wrong-name 404 uses the same step.
     */
    @When("I export the service catalog entry named {string} version {string} expecting status {int}")
    public void iExportService(String name, String version, int expectedStatus) throws IOException {
        String actualName = Utils.resolveContextPlaceholders(name);
        String actualVersion = Utils.resolveContextPlaceholders(version);
        SimpleHTTPClient.DownloadResult result = Requests.getToFile(
                Utils.getServiceCatalogExportURL(Utils.getBaseUrl(), actualName, actualVersion),
                serviceCatalogHeaders(), ".zip");
        Assert.assertEquals(result.getStatusCode(), expectedStatus,
                "Service catalog export status mismatch for name=" + actualName + " version=" + actualVersion);
        if (expectedStatus == 200) {
            Assert.assertTrue(result.getFile().length() > 0,
                    "Service catalog export of " + actualName + " returned 200 with an EMPTY archive");
            // Content, not just the entry name: zipContainsEntryNamed is true for a ZERO-BYTE entry, so an
            // export writing empty metadata/definition would read as a complete archive.
            String metadata = Utils.zipEntryText(result.getFile(), "metadata.yaml", "metadata.json");
            Assert.assertTrue(metadata != null && !metadata.isBlank(),
                    "Exported service archive for " + actualName + " carries no readable service metadata "
                            + "(entry " + (metadata == null ? "absent" : "present but empty") + ", archive size="
                            + result.getFile().length() + ")");
            String definition = Utils.zipEntryText(result.getFile(), "definition.yaml", "definition.json");
            Assert.assertTrue(definition != null && !definition.isBlank(),
                    "Exported service archive for " + actualName + " carries no readable service definition "
                            + "(entry " + (definition == null ? "absent" : "present but empty") + ", archive size="
                            + result.getFile().length() + ")");
        }
    }

    /**
     * Injects a {@code serviceInfo} block referencing an existing catalog entry into an API-create payload already
     * held in context, so the following generic API create binds the API to that service (the API's endpoint is
     * then the service's {@code serviceUrl}). {@code serviceInfo} is a NESTED object, which the generic
     * {@code I set the field ... in the payload ...} step cannot express — hence this step rather than a
     * per-scenario fixture with a hardcoded service key (the key must be scenario-unique, see isolation).
     *
     * <p>The key/name/version are read back from the STORED service (an intermediate GET whose body is consumed
     * locally and is NOT the asserted response), so the binding can only ever reference what the catalog actually
     * holds. {@code outdated:false} mirrors the legacy payload.
     */
    @When("I add service catalog entry {string} as serviceInfo to the payload {string}")
    public void iAddServiceInfoToPayload(String serviceIdKey, String payloadKey) throws IOException {
        String serviceId = TestContext.resolve(serviceIdKey).toString();
        HttpResponse serviceResp = SimpleHTTPClient.getInstance()
                .doGet(Utils.getServiceCatalogByIdURL(Utils.getBaseUrl(), serviceId), serviceCatalogHeaders());
        Assert.assertTrue(serviceResp != null && serviceResp.getResponseCode() >= 200
                        && serviceResp.getResponseCode() < 300 && serviceResp.getData() != null
                        && !serviceResp.getData().isBlank(),
                "Could not read service catalog entry " + serviceId + " to build the API's serviceInfo; got="
                        + (serviceResp == null ? "null" : serviceResp.getResponseCode() + "/" + serviceResp.getData()));
        JSONObject service = new JSONObject(serviceResp.getData());

        JSONObject serviceInfo = new JSONObject();
        serviceInfo.put("key", service.getString("serviceKey"));
        serviceInfo.put("name", service.getString("name"));
        serviceInfo.put("version", service.getString("version"));
        serviceInfo.put("outdated", false);

        JSONObject payload = new JSONObject(TestContext.resolve(payloadKey).toString());
        payload.put("serviceInfo", serviceInfo);
        TestContext.set(Utils.normalizeContextKey(payloadKey), payload.toString());
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
                Utils.getServiceCatalogImportURL(Utils.getBaseUrl(), Boolean.parseBoolean(overwrite)),
                serviceCatalogHeaders(), files, new HashMap<>());
        // Guard status AND body before parsing — a 200 with an empty body would otherwise surface as an opaque
        // JSONException instead of a clear failure.
        Assert.assertTrue(response.getResponseCode() == 200
                        && response.getData() != null && !response.getData().isBlank(),
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
        Requests.postMultipart(Utils.getServiceCatalogImportURL(Utils.getBaseUrl(), true), serviceCatalogHeaders(),
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
                Utils.getServiceCatalogImportURL(Utils.getBaseUrl(), Boolean.parseBoolean(overwrite)),
                serviceCatalogHeaders(), files, new HashMap<>());
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            // A 2xx import MUST carry the ServiceInfoList body — guard before parsing so a degenerate empty-body
            // success fails clearly instead of as a JSONException. (Non-2xx negatives skip this branch untouched.)
            Assert.assertTrue(response.getData() != null && !response.getData().isBlank(),
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
        Requests.get(Utils.getServiceCatalogByIdURL(Utils.getBaseUrl(), TestContext.resolve(idKey).toString()),
                serviceCatalogHeaders());
    }

    /** Retrieves a service by a raw (literal) id — for the invalid-id 404 negative. */
    @When("I retrieve the service catalog entry with raw id {string}")
    public void iRetrieveServiceRawId(String rawId) throws IOException {
        Requests.get(Utils.getServiceCatalogByIdURL(Utils.getBaseUrl(), rawId), serviceCatalogHeaders());
    }

    /** Retrieves a service's definition (publishes the response). */
    @When("I retrieve the definition of service catalog entry {string}")
    public void iRetrieveServiceDefinition(String idKey) throws IOException {
        Requests.get(Utils.getServiceCatalogDefinitionURL(Utils.getBaseUrl(), TestContext.resolve(idKey).toString()),
                serviceCatalogHeaders());
    }

    /** Retrieves a service's usage — the APIs referencing it (publishes the response). */
    @When("I retrieve the usage of service catalog entry {string}")
    public void iRetrieveServiceUsage(String idKey) throws IOException {
        Requests.get(Utils.getServiceCatalogUsageURL(Utils.getBaseUrl(), TestContext.resolve(idKey).toString()),
                serviceCatalogHeaders());
    }

    /** Deletes a service by id (publishes the response). */
    @When("I delete the service catalog entry {string}")
    public void iDeleteService(String idKey) throws IOException {
        Requests.delete(Utils.getServiceCatalogByIdURL(Utils.getBaseUrl(), TestContext.resolve(idKey).toString()),
                serviceCatalogHeaders());
    }

    /** Searches services by a single field ({@code name}/{@code version}/{@code definitionType}/{@code key}). */
    @When("I search service catalog entries by {string} {string}")
    public void iSearchServicesByField(String field, String value) throws IOException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put(field, Utils.resolveContextPlaceholders(value));
        Requests.get(Utils.getServiceCatalogSearchURL(Utils.getBaseUrl(), params), serviceCatalogHeaders());
    }

    /** Searches services by name with an explicit limit and offset (for pagination assertions). */
    @When("I search service catalog entries by name {string} with limit {int} and offset {int}")
    public void iSearchServicesWithPagination(String name, int limit, int offset) throws IOException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("name", Utils.resolveContextPlaceholders(name));
        params.put("limit", String.valueOf(limit));
        params.put("offset", String.valueOf(offset));
        Requests.get(Utils.getServiceCatalogSearchURL(Utils.getBaseUrl(), params), serviceCatalogHeaders());
    }

    /**
     * Searches services by name with an explicit {@code sortBy}/{@code sortOrder}, so a scenario can assert the
     * returned ORDER of its own entries (distinct from the limit/offset pagination step above). The name acts as
     * the isolation scope: the catalog is tenant-global, so a bare sorted search would interleave other scenarios'
     * services and no exact ordering could be asserted. An EMPTY name is deliberately allowed and drops the
     * parameter (the URL builder skips empty values) — that is how the invalid-sortBy / invalid-sortOrder 400
     * negatives are expressed, where the rejection happens before any matching and the scope is irrelevant.
     */
    @When("I search service catalog entries by name {string} sorted by {string} in {string} order")
    public void iSearchServicesSorted(String name, String sortBy, String sortOrder) throws IOException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("name", Utils.resolveContextPlaceholders(name));
        params.put("sortBy", sortBy);
        params.put("sortOrder", sortOrder);
        Requests.get(Utils.getServiceCatalogSearchURL(Utils.getBaseUrl(), params), serviceCatalogHeaders());
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
                        && response.getData() != null && !response.getData().isBlank(),
                "Service-catalog import did not return a 2xx body: got " + response.getResponseCode()
                        + " / " + response.getData());
        JSONArray list = new JSONObject(response.getData()).optJSONArray("list");
        int actual = list == null ? 0 : list.length();
        Assert.assertEquals(actual, expected,
                "Service-catalog import imported-count mismatch; body: " + response.getData());
    }
}
