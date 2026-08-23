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
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Step definitions for the visibility &amp; access-control cluster (ports of DevPortalVisibility,
 * APIVisibilityWithDirectURL, APITagVisibilityByRole, PublisherAccessControl, ContentSearch, DevPortalSearch,
 * ChangeAPITags and APIMANAGER4373). These add three capabilities not present in the base publisher/devportal
 * glue:
 * <ul>
 *   <li>authoring an API whose {@code visibility}/{@code accessControl} is role-restricted (a payload mutation
 *       over the shared create-and-deploy primitive), so a devportal / publisher read then enforces the role;</li>
 *   <li>DevPortal reads of an API's <em>sub-resources</em> (document, document content, swagger) as the acting
 *       actor or anonymously-in-tenant, polling until the visibility-driven status settles (the store index is
 *       eventually consistent after a publish / visibility change);</li>
 *   <li>content search on the publisher and devportal planes asserting an exact result <em>count</em> — a
 *       {@code description:}-scoped query matches exactly one API. The field MUST be named: pinned live, a
 *       FIELDLESS query on this build is a NAME search, not a content search (a bare query for a token appearing
 *       only in an API's description returns 0 while {@code description:<sameToken>} returns 1), which is also why
 *       document-content search is not portable here. Also: a role-restricted API is found (count 1) only by an
 *       authorised searcher (count 0 otherwise), plus tag-cloud presence/absence of a specific tag value.</li>
 * </ul>
 * The role-restricted status the DevPortal returns to an unauthorised caller is <b>404</b> (verified live and in
 * the legacy DevPortalVisibilityTestCase — the store hides a restricted API rather than 403-ing it), so the
 * feature asserts the exact 404 / 200, never a relaxed 4xx.
 */
public class VisibilityAccessSteps {

    private final BaseSteps baseSteps = new BaseSteps();
    private final PublisherBaseSteps publisherBaseSteps = new PublisherBaseSteps();

    // ---- Role-restricted API authoring ------------------------------------------------------------------

    /**
     * Creates an API from the payload with its DevPortal {@code visibility} set to RESTRICTED for the given
     * comma-separated {@code roles} (i.e. only a devportal user carrying one of those roles — or a publisher-role
     * user — can see it in the store), then creates a revision and deploys it. The roles resolve {@code {{...}}}
     * placeholders so a scenario-unique role name flows through. Ports the visibility-restricted create used by
     * DevPortalVisibility / APIVisibilityWithDirectURL / APITagVisibilityByRole.
     */
    @When("I have created an api from {string} with restricted visibility for roles {string} as {string} and deployed it")
    public void iCreateVisibilityRestrictedApi(String payloadPath, String rolesCsv, String apiIdKey)
            throws IOException, InterruptedException {
        JSONObject json = loadPayload(payloadPath);
        json.put("visibility", "RESTRICTED");
        json.put("visibleRoles", csvToJsonArray(rolesCsv));
        createAndDeployFromJson(json, apiIdKey);
    }

    /**
     * As above, but starting from a payload already prepared into a context key (e.g. one whose description was
     * set for a content-search assertion) rather than a file — so a scenario can both customise a field AND apply
     * the restricted visibility. Ports the visibility-restricted content-search API of ContentSearchTestCase.
     */
    @When("I have created an api from context payload {string} with restricted visibility for roles {string} as {string} and deployed it")
    public void iCreateVisibilityRestrictedApiFromContext(String payloadKey, String rolesCsv, String apiIdKey)
            throws IOException, InterruptedException {
        JSONObject json = new JSONObject(TestContext.resolve(payloadKey).toString());
        json.put("visibility", "RESTRICTED");
        json.put("visibleRoles", csvToJsonArray(rolesCsv));
        createAndDeployFromJson(json, apiIdKey);
    }

    /**
     * The mode-PARAMETERISED generalisation of the two {@code restricted visibility} steps above: creates an API
     * from a payload already in context with its DevPortal {@code visibility} set to the given mode, then revisions
     * and deploys it through the same shared primitive. It exists so ONE {@code Scenario Outline} can drive
     * {@code PUBLIC}, {@code PRIVATE} and {@code RESTRICTED} from an Examples column — which is the whole point of
     * the cross-domain visibility features (proving the publisher-plane listing is tenant-scoped *regardless* of
     * the DevPortal visibility mode requires all three modes side by side, and a per-mode step would make that
     * table impossible). The two steps above remain the RESTRICTED-only shorthands the role-visibility features
     * read better with.
     * <p>
     * {@code visibility} is one of:
     * <ul>
     *   <li>{@code PUBLIC} — visible to everyone, including anonymous store callers;</li>
     *   <li>{@code PRIVATE} — the DTO value behind the UI's "Visible to my domain": visible to every principal of
     *       the API's own tenant but to nobody outside it, and NOT to an anonymous store caller. This is a
     *       DevPortal-plane field only — it does not restrict the Publisher plane;</li>
     *   <li>{@code RESTRICTED} — visible only to a caller carrying one of {@code roles}.</li>
     * </ul>
     * {@code roles} MUST be empty for PUBLIC/PRIVATE (those modes carry no role list) and non-empty for RESTRICTED;
     * a mismatch fails fast rather than silently authoring a different API than the scenario describes.
     */
    @When("I have created an api from context payload {string} with devportal visibility {string} for roles {string} as {string} and deployed it")
    public void iCreateApiWithDevportalVisibility(String payloadKey, String visibility, String rolesCsv,
                                                  String apiIdKey) throws IOException, InterruptedException {
        boolean restricted = "RESTRICTED".equals(visibility);
        if (!restricted && !"PUBLIC".equals(visibility) && !"PRIVATE".equals(visibility)) {
            throw new IllegalArgumentException("Unknown DevPortal visibility '" + visibility
                    + "' (expected PUBLIC | PRIVATE | RESTRICTED)");
        }
        boolean rolesGiven = rolesCsv != null && !rolesCsv.isBlank();
        Assert.assertEquals(rolesGiven, restricted, "DevPortal visibility '" + visibility + "' was given roles='"
                + rolesCsv + "': roles are required for RESTRICTED and must be empty for PUBLIC/PRIVATE");

        JSONObject json = new JSONObject(TestContext.resolve(payloadKey).toString());
        json.put("visibility", visibility);
        if (restricted) {
            json.put("visibleRoles", csvToJsonArray(rolesCsv));
        }
        createAndDeployFromJson(json, apiIdKey);
    }

    /**
     * Sets the {@code description} field on a payload already stored in a context key (resolving {@code {{...}}}
     * placeholders), writing it back to the same key. Used to plant a scenario-unique word in an API's description
     * for a content-search assertion. Non-asserting (a pure payload mutation).
     */
    @When("I set the description of context payload {string} to {string}")
    public void iSetDescriptionOfContextPayload(String payloadKey, String description) {
        JSONObject json = new JSONObject(TestContext.resolve(payloadKey).toString());
        json.put("description", Utils.resolveContextPlaceholders(description));
        TestContext.set(Utils.normalizeContextKey(payloadKey), json.toString());
    }

    /**
     * Creates an API with its publisher-plane {@code accessControl} set to RESTRICTED for the given roles (only a
     * creator/publisher carrying one of those roles can view/edit it in the Publisher), then deploys it. Ports the
     * access-control-restricted create of PublisherAccessControl.
     */
    @When("I have created an api from {string} with restricted access control for roles {string} as {string} and deployed it")
    public void iCreateAccessControlRestrictedApi(String payloadPath, String rolesCsv, String apiIdKey)
            throws IOException, InterruptedException {
        JSONObject json = loadPayload(payloadPath);
        json.put("accessControl", "RESTRICTED");
        json.put("accessControlRoles", csvToJsonArray(rolesCsv));
        createAndDeployFromJson(json, apiIdKey);
    }

    /**
     * As above, but starting from a payload already prepared into a context key — so a scenario can set a
     * scenario-unique description AND restrict the publisher-plane access control on the same API. That
     * combination is what the access-control-filtered CONTENT SEARCH needs (ContentSearchTestCase
     * testContentSearchWithAccessControl): the description carries the searched token while the access role
     * decides which publisher principal the API is counted for.
     */
    @When("I have created an api from context payload {string} with restricted access control for roles {string} as {string} and deployed it")
    public void iCreateAccessControlRestrictedApiFromContext(String payloadKey, String rolesCsv, String apiIdKey)
            throws IOException, InterruptedException {
        JSONObject json = new JSONObject(TestContext.resolve(payloadKey).toString());
        json.put("accessControl", "RESTRICTED");
        json.put("accessControlRoles", csvToJsonArray(rolesCsv));
        createAndDeployFromJson(json, apiIdKey);
    }

    /**
     * Creates an API with BOTH a restricted publisher {@code accessControl} (for {@code accessRoles}) and a public
     * DevPortal {@code visibility}, then deploys it. The mixed case from PublisherAccessControl: access control is
     * publisher-plane only, so a store consumer still sees the (publicly visible) API even though a non-privileged
     * creator cannot view it in the Publisher.
     */
    @When("I have created an api from {string} with restricted access control for roles {string} and public visibility as {string} and deployed it")
    public void iCreateAccessRestrictedPublicVisibilityApi(String payloadPath, String accessRolesCsv, String apiIdKey)
            throws IOException, InterruptedException {
        JSONObject json = loadPayload(payloadPath);
        json.put("accessControl", "RESTRICTED");
        json.put("accessControlRoles", csvToJsonArray(accessRolesCsv));
        json.put("visibility", "PUBLIC");
        createAndDeployFromJson(json, apiIdKey);
    }

    private JSONObject loadPayload(String payloadPath) throws IOException {
        baseSteps.putJsonPayloadFromFile(payloadPath, "<visAccessApiPayload>");
        return new JSONObject(TestContext.resolve("<visAccessApiPayload>").toString());
    }

    private JSONArray csvToJsonArray(String csv) {
        JSONArray values = new JSONArray();
        for (String item : csv.split(",")) {
            values.put(Utils.resolveContextPlaceholders(item.trim()));
        }
        return values;
    }

    /**
     * Creates the (already-mutated) API as the acting actor's publisher, then revisions + deploys it. The create
     * is retried on a 900610 "Invalid user roles found" response: a role provisioned moments earlier (SOAP addRole)
     * is validated by the publisher against the user-store, whose role cache is eventually consistent — so a
     * freshly-created visibility/access role can transiently read as "not existing" on a loaded shared container
     * (verified: the identical create succeeds on a quiescent standalone server). This is a readiness wait on the
     * role becoming visible to validation, NOT a relaxed assertion — any other non-201 fails immediately, and a
     * persistent 900610 still fails after the window. Revision + deploy then reuse the shared publisher primitives.
     */
    private void createAndDeployFromJson(JSONObject json, String apiIdKey) throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        String url = Utils.getAPICreateEndpointURL(Utils.getBaseUrl(), "apis");

        long deadlineStart = System.currentTimeMillis();
        long deadline = deadlineStart + Constants.RUNTIME_PROPAGATION_TIMEOUT;
        HttpResponse response = null;
        Object createdId = null;
        boolean outcomeUncertain = false;
        while (true) {
            try {
                response = Requests.post(url, headers, json.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
            } catch (IOException transientFailure) {
                // The create POST is NOT idempotent and its outcome is now UNKNOWN — the server may have
                // committed before the connection died, and a blind re-POST would 409 against our own API and
                // leak it. Resolve by the unique name: found -> adopt that id; absent -> re-POST (the 900610
                // rejection branch below stays a plain retry: the server provably created nothing there).
                response = null;
                outcomeUncertain = true;
                createdId = Utils.findIdByNameInListResponse(
                        Utils.getAPISearchEndpointURL(Utils.getBaseUrl(), "name:" + json.getString("name"), null, null),
                        headers, json.getString("name"), "id");
                if (createdId != null) {
                    break;
                }
            }
            boolean retryable = response == null || (response.getResponseCode() == 400
                    && response.getData() != null && response.getData().contains("900610"));
            if (!retryable || System.currentTimeMillis() >= deadline) {
                break;
            }
            Utils.pollPause(deadlineStart, 2000);
        }
        if (createdId == null && outcomeUncertain && response != null && response.getResponseCode() == 409) {
            // The uncertainty lookup raced the eventually-consistent search index: the lost-response create
            // surfaced as a conflict on the re-POST. The 409 PROVES the API exists server-side (the name is
            // unique to this call, so the conflict is with our own committed create), so POLL the index until it
            // appears rather than giving up after one read — a single miss would fail the 201 assert below AND
            // leak the API, which is not registered for teardown yet. Always makes at least one attempt (the
            // deadline may already be spent by the time we get here).
            String searchUrl = Utils.getAPISearchEndpointURL(Utils.getBaseUrl(),
                    "name:" + json.getString("name"), null, null);
            while (true) {
                createdId = Utils.findIdByNameInListResponse(searchUrl, headers, json.getString("name"), "id");
                if (createdId != null || System.currentTimeMillis() >= deadline) {
                    break;
                }
                Utils.pollPause(deadlineStart, 2000);
            }
        }
        if (createdId == null) {
            Assert.assertNotNull(response, "API create got no response within the deadline (requests failed)");
            Assert.assertEquals(response.getResponseCode(), 201, response.getData());
            createdId = Utils.extractValueFromPayload(response.getData(), "id");
        }
        TestContext.set(apiIdKey, createdId);
        ResourceCleanup.register(Constants.CREATED_API_IDS, createdId);

        baseSteps.putJsonPayloadInContext("<visAccessRevisionPayload>", "{\"description\":\"Initial Revision\"}");
        publisherBaseSteps.iCreateResourceRevision("apis", apiIdKey, "<visAccessRevisionPayload>");
        baseSteps.putJsonPayloadInContext("<visAccessDeployPayload>",
                "[{\"name\":\"{{gatewayEnvironment}}\",\"vhost\":\"localhost\",\"displayOnDevportal\":true}]");
        publisherBaseSteps.iDeployApiRevisionGivenPayload("<revisionId>", "apis", apiIdKey, "<visAccessDeployPayload>");
        baseSteps.theResponseStatusCodeShouldBe(201);
    }

    /**
     * Updates a published API's tags in place (GET the publisher API → replace {@code tags} → PUT) as the acting
     * actor's publisher token. Used by ChangeAPITags to remove a tag and prove the store tag filter no longer
     * matches it. Non-asserting — the feature asserts the PUT status.
     */
    @When("I set the tags of API {string} to {string}")
    public void iSetApiTags(String apiIdKey, String tagsCsv) throws IOException {
        JSONObject api = fetchPublisherApi(apiIdKey);
        api.put("tags", csvToJsonArray(tagsCsv));
        putPublisherApi(apiIdKey, api);
    }

    /**
     * Updates a published API's DevPortal visibility roles in place (GET the publisher API → keep visibility
     * RESTRICTED, replace {@code visibleRoles} → PUT) as the acting actor's publisher token. Used by APIMANAGER4373
     * to change a subscribed API's visibility role away from the subscriber's role (making it inaccessible to that
     * subscriber). Non-asserting.
     */
    @When("I set the visibility roles of API {string} to {string}")
    public void iSetApiVisibilityRoles(String apiIdKey, String rolesCsv) throws IOException {
        JSONObject api = fetchPublisherApi(apiIdKey);
        api.put("visibility", "RESTRICTED");
        api.put("visibleRoles", csvToJsonArray(rolesCsv));
        putPublisherApi(apiIdKey, api);
    }

    private JSONObject fetchPublisherApi(String apiIdKey) throws IOException {
        String apiId = TestContext.resolve(apiIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        HttpResponse current = SimpleHTTPClient.getInstance()
                .doGet(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "apis", apiId), headers);
        // Intermediate GET of a GET→mutate→PUT: confirm a 2xx response WITH a body before parsing, so a
        // failed/empty fetch fails clearly instead of throwing an opaque JSONException/NPE.
        Assert.assertTrue(current != null && current.getResponseCode() >= 200 && current.getResponseCode() < 300
                        && current.getData() != null && !current.getData().isBlank(),
                "Failed to fetch API '" + apiId + "' before updating it: expected a 2xx response with a body, got "
                        + (current == null ? "no response" : current.getResponseCode() + " / body=" + current.getData()));
        return new JSONObject(current.getData());
    }

    private void putPublisherApi(String apiIdKey, JSONObject api) throws IOException {
        String apiId = TestContext.resolve(apiIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.put(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "apis", apiId), headers, api.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    // ---- DevPortal sub-resource reads (visibility-gated) -------------------------------------------------

    /**
     * Polls a DevPortal GET of an API sub-resource (the API itself, a document, a document's content, or the
     * swagger) as the acting actor's devportal token until it returns {@code expectedStatus}. The kind selects the
     * URL:
     * <ul>
     *   <li>{@code api} → {@code /apis/{id}}</li>
     *   <li>{@code document} → {@code /apis/{id}/documents/{docId}} (docId from context key {@code documentID})</li>
     *   <li>{@code document content} → {@code /apis/{id}/documents/{docId}/content}</li>
     *   <li>{@code swagger} → {@code /apis/{id}/swagger}</li>
     * </ul>
     * Visibility after a publish / visibility change is eventually consistent, so this retries and asserts the
     * exact status after the loop.
     */
    @Then("I retrieve the devportal {string} of API {string} until the response status code becomes {int} within {int} seconds")
    public void iRetrieveDevportalSubResourceUntil(String kind, String apiIdKey, int expectedStatus, int timeoutSeconds)
            throws InterruptedException {
        pollUntilStatus(subResourceUrl(kind, apiIdKey), Identity.devportalHeaders(), expectedStatus, timeoutSeconds);
    }

    /**
     * As above, but anonymously (no auth) with the DevPortal tenant context header so a tenant API resolves for the
     * anonymous caller — without {@code X-WSO2-Tenant} a tenant API returns 404 regardless of visibility, masking
     * the visibility check. Anonymous access to a restricted sub-resource is expected to be 404.
     */
    @Then("I retrieve the devportal {string} of API {string} anonymously in tenant {string} until the response status code becomes {int} within {int} seconds")
    public void iRetrieveDevportalSubResourceAnonUntil(String kind, String apiIdKey, String tenantDomain,
                                                       int expectedStatus, int timeoutSeconds)
            throws InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-WSO2-Tenant", tenantDomain);
        pollUntilStatus(subResourceUrl(kind, apiIdKey), headers, expectedStatus, timeoutSeconds);
    }

    private String subResourceUrl(String kind, String apiIdKey) {
        String apiId = TestContext.resolve(apiIdKey).toString();
        switch (kind) {
            case "api":
                return Utils.getDevportalApiDetailURL(Utils.getBaseUrl(), apiId);
            case "swagger":
                return Utils.getDevportalApiSwaggerURL(Utils.getBaseUrl(), apiId);
            case "document":
                return Utils.getDevportalApiDocumentURL(Utils.getBaseUrl(), apiId,
                        TestContext.resolve("documentID").toString());
            case "document content":
                return Utils.getDevportalApiDocumentContentURL(Utils.getBaseUrl(), apiId,
                        TestContext.resolve("documentID").toString());
            default:
                throw new IllegalArgumentException("Unknown devportal sub-resource kind: '" + kind
                        + "' (expected api | swagger | document | document content)");
        }
    }

    /**
     * Polls the devportal read path until it answers {@code expectedStatus}, publishes the LAST response as the
     * step's assertion target and asserts the exact status itself (§7/§12).
     *
     * <p>Funnelled through {@link Utils#retryUntil} rather than a hand-rolled deadline loop (§7/§15): the
     * envelope owns the {@code max(timeout, RUNTIME_PROPAGATION_TIMEOUT)} ceiling, so this call site cannot
     * drift below it, and it retries ONLY {@code IOException} — a bad context key still fails fast instead of
     * being masked as a timeout. Behaviourally identical to the loop it replaces, which already used the same
     * ceiling and the same {@code pollPause} cadence.
     */
    private void pollUntilStatus(String url, Map<String, String> headers, int expectedStatus, int timeoutSeconds)
            throws InterruptedException {
        // Cleared BEFORE the call so a throw leaves httpResponse ABSENT rather than stale — otherwise a
        // following assertion could pass against the previous step's response (§7's stale-response trap).
        TestContext.remove("httpResponse");
        HttpResponse last = Utils.retryUntil(timeoutSeconds * 1000L,
                () -> SimpleHTTPClient.getInstance().doGet(url, headers),
                response -> response.getResponseCode() == expectedStatus);
        TestContext.set("httpResponse", last);
        Assert.assertNotNull(last, "No devportal response received for " + url);
        Assert.assertEquals(last.getResponseCode(), expectedStatus,
                "DevPortal visibility did not reach " + expectedStatus + " within " + timeoutSeconds
                        + "s for " + url + "; last: " + last.getData());
    }

    // ---- Content search (publisher + devportal), asserting an exact result count -------------------------

    /**
     * Publisher search ({@code /apis?query=<query>}) as the acting actor, polling until the result count equals
     * {@code expectedCount} (the search index is asynchronous, hence the poll). Two query shapes use it:
     * <ul>
     *   <li>{@code description:<uniqueWord>} — matches exactly the one API whose description carries the word
     *       (the publisher half of ContentSearch, search by description);</li>
     *   <li>{@code name:<uniqueName>} — the publisher LIST membership probe of the cross-domain visibility
     *       features: count 1 means the API is in the list this principal is entitled to see, count 0 means it is
     *       absent. Scoping the list by the API's scenario-unique name (rather than reading the unfiltered
     *       {@code /apis} page) is what makes the assertion exact under parallel load — an unfiltered first page
     *       would otherwise hide the API behind the default page size and read as "absent".</li>
     * </ul>
     * The count is compared exactly, so a genuine absence is count 0 on a real 200 — never a 401/error read as
     * "not there" (a non-200 leaves the count at -1 and fails the step).
     */
    @When("I search Publisher APIs with content query {string} until the result count is {int} within {int} seconds")
    public void iSearchPublisherContentUntilCount(String query, int expectedCount, int timeoutSeconds)
            throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        searchUntilCount(Utils.getPublisherApiSearchURL(Utils.getBaseUrl(), Utils.resolveContextPlaceholders(query)),
                headers, expectedCount, timeoutSeconds);
    }

    /**
     * DevPortal search ({@code /apis?query=<query>}) as the acting actor's devportal token, polling until the result
     * count equals {@code expectedCount}. It is the EXACT-COUNT counterpart of the {@code until it contains} /
     * {@code until it does not contain} DevPortal search steps — same endpoint, but the assertion is the count, so
     * use this wherever a count is available (§12) and those only where the subject is genuinely membership.
     * <p>
     * The {@code query} is any DevPortal search expression, not only a content one:
     * <ul>
     *   <li>{@code description:<uniqueWord>} — the store half of ContentSearch (search by description, and the
     *       visibility-filtered variant where an authorised searcher gets 1 and an unauthorised one 0);</li>
     *   <li>a bare token, {@code name:}, {@code tags:}/{@code tag:} and any AND/OR combination of them — the
     *       exact-count query matrix of DevPortalSearchTest. Measured semantics: terms of the same field OR
     *       together, different fields AND together, a bare term matches an API's NAME but never a tag or a
     *       description word, and a tag match is case-SENSITIVE.</li>
     * </ul>
     */
    @When("I search DevPortal APIs with content query {string} until the result count is {int} within {int} seconds")
    public void iSearchDevportalContentUntilCount(String query, int expectedCount, int timeoutSeconds)
            throws IOException, InterruptedException {
        searchUntilCount(Utils.getApiSearchURL(Utils.getBaseUrl(), Utils.resolveContextPlaceholders(query)),
                Identity.devportalHeaders(), expectedCount, timeoutSeconds);
    }

    /**
     * ANONYMOUS DevPortal API LIST search ({@code /apis?query=<query>} with no credentials), polling until the
     * result count equals {@code expectedCount}. The tenant is addressed by the {@code X-WSO2-Tenant} header, as the
     * anonymous by-id reads above do — without it the store resolves against the super tenant and a tenant API is
     * missing for the wrong reason, masking the visibility check.
     * <p>
     * This is the one genuinely new probe the cross-domain visibility ports need: the existing anonymous steps are
     * all by-ID GETs of an API or a sub-resource, and the legacy classes assert LIST membership
     * ({@code getAPIListFromStoreAsAnonymousUser} + {@code isAPIAvailableInStore}) — a different observable, since a
     * by-id 404 does not prove the API is absent from the anonymous listing.
     * <p>
     * Because count 0 is also what a stale index returns, a scenario asserting an anonymous ABSENCE must first
     * establish the API IS listed for a principal entitled to see it (or, for PUBLIC, anonymously in its own
     * tenant); otherwise the absence could pass before the API would ever have appeared.
     */
    @When("I search DevPortal APIs anonymously in tenant {string} with query {string} until the result count is {int} within {int} seconds")
    public void iSearchDevportalAnonymouslyUntilCount(String tenantDomain, String query, int expectedCount,
                                                      int timeoutSeconds) throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-WSO2-Tenant", tenantDomain);
        searchUntilCount(Utils.getApiSearchURL(Utils.getBaseUrl(), Utils.resolveContextPlaceholders(query)),
                headers, expectedCount, timeoutSeconds);
    }

    /**
     * Polls a search URL until its result {@code count} equals {@code expectedCount}, through
     * {@link Utils#retryUntil} — the shared envelope for a step whose RESULT IS THE ASSERTION TARGET (§7/§15):
     * it owns the deadline (floored at the shared propagation ceiling), the tiered pacing and the
     * retry-only-{@code IOException} policy, and returns the LAST response so this method publishes it and
     * asserts the exact count itself. The count is compared exactly, so an absence is count 0 on a real 200 —
     * a non-200 leaves the count at -1 and fails.
     */
    private void searchUntilCount(String url, Map<String, String> headers, int expectedCount, int timeoutSeconds)
            throws InterruptedException {
        HttpResponse response = Utils.retryUntil(timeoutSeconds * 1000L,
                () -> Requests.get(url, headers),
                resp -> Utils.listCountOf(resp) == expectedCount);
        Requests.publishPollResult(response);
        Assert.assertNotNull(response, "No content-search response for " + url);
        Assert.assertEquals(Utils.listCountOf(response), expectedCount,
                "Content search result count did not reach " + expectedCount + " within the retryUntil window "
                        + "(max(" + timeoutSeconds + "s, the shared propagation ceiling)) for " + url
                        + "; last response: " + response.getResponseCode() + " / " + response.getData());
    }

    /** The {@code count} of a search response, or -1 when the response is not a 200 with a body. */

    // ---- DevPortal tag cloud presence / absence of a specific tag ----------------------------------------

    /**
     * Asserts the DevPortal tag cloud (already fetched into {@code httpResponse}) does NOT contain a tag whose
     * value equals {@code tagValue}. Used to prove a restricted-visibility API's tag is hidden from an
     * unauthorised (e.g. anonymous) tag cloud. Complements the existing {@code tag cloud should contain tag ...
     * with count} step.
     */
    @Then("the DevPortal tag cloud should not contain tag {string}")
    public void tagCloudShouldNotContainTag(String tagValue) {
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertNotNull(response, "No tag cloud response captured");
        // Guard status AND body BEFORE the absence check: an error response (whose JSON carries no "list") or an
        // empty body would otherwise "not contain" the tag VACUOUSLY — a false pass on a visibility assertion.
        Assert.assertTrue(response.getResponseCode() >= 200 && response.getResponseCode() < 300,
                "Tag cloud retrieval failed — cannot assert tag absence against an error response; got "
                        + response.getResponseCode() + " / body=" + response.getData());
        Assert.assertTrue(response.getData() != null && !response.getData().isBlank(),
                "Tag cloud response carried no body — cannot assert tag absence against an empty response (status "
                        + response.getResponseCode() + ")");
        String resolved = Utils.resolveContextPlaceholders(tagValue);
        JSONArray list = new JSONObject(response.getData()).optJSONArray("list");
        boolean present = false;
        if (list != null) {
            for (int i = 0; i < list.length(); i++) {
                if (resolved.equals(list.getJSONObject(i).optString("value"))) {
                    present = true;
                    break;
                }
            }
        }
        Assert.assertFalse(present,
                "Restricted-visibility tag '" + resolved + "' is present in the tag cloud but should be hidden; "
                        + "tag cloud: " + response.getData());
    }
}
