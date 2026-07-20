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
 *       {@code description:}-scoped query matches exactly one API (pinned live: a bare content query does not match
 *       a {@code Names.unique}-style underscore-joined token, so the feature uses the {@code description:} field),
 *       and a role-restricted API is found (count 1) only by an authorised searcher (count 0 otherwise), plus
 *       tag-cloud presence/absence of a specific tag value.</li>
 * </ul>
 * The role-restricted status the DevPortal returns to an unauthorised caller is <b>404</b> (verified live and in
 * the legacy DevPortalVisibilityTestCase — the store hides a restricted API rather than 403-ing it), so the
 * feature asserts the exact 404 / 200, never a relaxed 4xx.
 */
public class VisibilityAccessSteps {

    private final BaseSteps baseSteps = new BaseSteps();
    private final PublisherBaseSteps publisherBaseSteps = new PublisherBaseSteps();

    private String getBaseUrl() {
        return baseSteps.getBaseUrl();
    }

    private Map<String, String> devportalAuthHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        return headers;
    }

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
        json.put("visibleRoles", rolesArray(rolesCsv));
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
        json.put("visibleRoles", rolesArray(rolesCsv));
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
        json.put("accessControlRoles", rolesArray(rolesCsv));
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
        json.put("accessControlRoles", rolesArray(accessRolesCsv));
        json.put("visibility", "PUBLIC");
        createAndDeployFromJson(json, apiIdKey);
    }

    private JSONObject loadPayload(String payloadPath) throws IOException {
        baseSteps.putJsonPayloadFromFile(payloadPath, "<visAccessApiPayload>");
        return new JSONObject(TestContext.resolve("<visAccessApiPayload>").toString());
    }

    private JSONArray rolesArray(String rolesCsv) {
        JSONArray roles = new JSONArray();
        for (String r : rolesCsv.split(",")) {
            roles.put(Utils.resolveContextPlaceholders(r.trim()));
        }
        return roles;
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
        String url = Utils.getAPICreateEndpointURL(getBaseUrl(), "apis");

        long deadline = System.currentTimeMillis() + Constants.DEPLOYMENT_WAIT_TIME;
        HttpResponse response;
        while (true) {
            response = Requests.post(url, headers, json.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
            boolean roleNotYetVisible = response != null && response.getResponseCode() == 400
                    && response.getData() != null && response.getData().contains("900610");
            if (!roleNotYetVisible || System.currentTimeMillis() >= deadline) {
                break;
            }
            Thread.sleep(2000);
        }
        Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        Object createdId = Utils.extractValueFromPayload(response.getData(), "id");
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
        api.put("tags", rolesArray(tagsCsv));
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
        api.put("visibleRoles", rolesArray(rolesCsv));
        putPublisherApi(apiIdKey, api);
    }

    private JSONObject fetchPublisherApi(String apiIdKey) throws IOException {
        String apiId = TestContext.resolve(apiIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        HttpResponse current = SimpleHTTPClient.getInstance()
                .doGet(Utils.getResourceEndpointURL(getBaseUrl(), "apis", apiId), headers);
        // Intermediate GET of a GET→mutate→PUT: confirm a 2xx response WITH a body before parsing, so a
        // failed/empty fetch fails clearly instead of throwing an opaque JSONException/NPE.
        Assert.assertTrue(current != null && current.getResponseCode() >= 200 && current.getResponseCode() < 300
                        && current.getData() != null && !current.getData().isEmpty(),
                "Failed to fetch API '" + apiId + "' before updating it: expected a 2xx response with a body, got "
                        + (current == null ? "no response" : current.getResponseCode() + " / body=" + current.getData()));
        return new JSONObject(current.getData());
    }

    private void putPublisherApi(String apiIdKey, JSONObject api) throws IOException {
        String apiId = TestContext.resolve(apiIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.put(Utils.getResourceEndpointURL(getBaseUrl(), "apis", apiId), headers, api.toString(),
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
        pollUntilStatus(subResourceUrl(kind, apiIdKey), devportalAuthHeaders(), expectedStatus, timeoutSeconds);
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
                return Utils.getDevportalApiDetailURL(getBaseUrl(), apiId);
            case "swagger":
                return Utils.getDevportalApiSwaggerURL(getBaseUrl(), apiId);
            case "document":
                return Utils.getDevportalApiDocumentURL(getBaseUrl(), apiId,
                        TestContext.resolve("documentID").toString());
            case "document content":
                return Utils.getDevportalApiDocumentContentURL(getBaseUrl(), apiId,
                        TestContext.resolve("documentID").toString());
            default:
                throw new IllegalArgumentException("Unknown devportal sub-resource kind: '" + kind
                        + "' (expected api | swagger | document | document content)");
        }
    }

    private void pollUntilStatus(String url, Map<String, String> headers, int expectedStatus, int timeoutSeconds)
            throws InterruptedException {
        long deadline = System.currentTimeMillis()
                + Math.max(timeoutSeconds * 1000L, Constants.DEPLOYMENT_WAIT_TIME);
        HttpResponse last = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                last = SimpleHTTPClient.getInstance().doGet(url, headers);
                if (last.getResponseCode() == expectedStatus) {
                    break;
                }
            } catch (IOException transientDuringWarmup) {
                // retry transient connectivity only
            }
            Thread.sleep(2000);
        }
        TestContext.set("httpResponse", last);
        Assert.assertNotNull(last, "No devportal response received for " + url);
        Assert.assertEquals(last.getResponseCode(), expectedStatus,
                "DevPortal visibility did not reach " + expectedStatus + " within " + timeoutSeconds
                        + "s for " + url + "; last: " + last.getData());
    }

    // ---- Content search (publisher + devportal), asserting an exact result count -------------------------

    /**
     * Publisher content search ({@code /apis?query=<content>}) as the acting actor, polling until the result count
     * equals {@code expectedCount} (the search index is asynchronous, hence the poll). Used with a
     * {@code description:<uniqueWord>} query — which matches exactly the one API whose description carries the word.
     * Ports the publisher half of ContentSearch (search by description).
     */
    @When("I search Publisher APIs with content query {string} until the result count is {int} within {int} seconds")
    public void iSearchPublisherContentUntilCount(String query, int expectedCount, int timeoutSeconds)
            throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        searchUntilCount(Utils.getPublisherApiSearchURL(getBaseUrl(), Utils.resolveContextPlaceholders(query)),
                headers, expectedCount, timeoutSeconds);
    }

    /**
     * DevPortal content search ({@code /apis?query=<content>}) as the acting actor's devportal token, polling until
     * the result count equals {@code expectedCount}. Ports the store half of ContentSearch (search by description
     * and visibility-filtered search): with a {@code description:<uniqueWord>} query, an authorised searcher gets
     * count 1 and an unauthorised one (lacking the visibility role) gets count 0.
     */
    @When("I search DevPortal APIs with content query {string} until the result count is {int} within {int} seconds")
    public void iSearchDevportalContentUntilCount(String query, int expectedCount, int timeoutSeconds)
            throws IOException, InterruptedException {
        searchUntilCount(Utils.getApiSearchURL(getBaseUrl(), Utils.resolveContextPlaceholders(query)),
                devportalAuthHeaders(), expectedCount, timeoutSeconds);
    }

    private void searchUntilCount(String url, Map<String, String> headers, int expectedCount, int timeoutSeconds)
            throws IOException, InterruptedException {
        long endTime = System.currentTimeMillis() + timeoutSeconds * 1000L;
        HttpResponse response = null;
        int actual = -1;
        while (true) {
            try {
                response = Requests.get(url, headers);
                if (response.getResponseCode() == 200
                        && response.getData() != null && !response.getData().isEmpty()) {
                    actual = new JSONObject(response.getData()).optInt("count", -1);
                }
            } catch (IOException transientFailure) {
                // transient network failure — keep polling; the previous response (if any) is retained
            }
            if (actual == expectedCount || System.currentTimeMillis() >= endTime) {
                break;
            }
            Thread.sleep(2000);
        }
        Assert.assertNotNull(response, "No content-search response for " + url);
        Assert.assertEquals(actual, expectedCount,
                "Content search result count did not reach " + expectedCount + " within " + timeoutSeconds
                        + "s for " + url + "; last count=" + actual);
    }

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
        Assert.assertTrue(response.getData() != null && !response.getData().isEmpty(),
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
