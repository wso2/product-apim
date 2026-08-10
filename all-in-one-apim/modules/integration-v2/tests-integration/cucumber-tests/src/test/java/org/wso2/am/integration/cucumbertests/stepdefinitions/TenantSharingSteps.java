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
import org.wso2.am.integration.cucumbertests.utils.TenantUserProvisioner;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Steps for the tenant-sharing (WSO2-IS-7 default key manager) features. Per CLAUDE.md §14 the notify POST is
 * a product operation performed as the ACTING actor through the {@code Requests} funnel (it IS the subject of
 * these features), and the notify-synced tenant's admin is registered as a RUNTIME actor
 * ({@link TenantUserProvisioner#registerRuntimeTenantAdmin}) so the standard auth composites, steps and
 * {@code ResourceCleanup} all apply to it — no side-channel tokens.
 */
public class TenantSharingSteps {

    private static final String KM_LIST_KEY = "tenantSharingKmList";
    private static final String TENANT_CREATED_EVENT =
            "https://schemas.identity.wso2.org/events/tenant/event-type/tenantCreated";
    private static final String TENANT_OWNER_UPDATED_EVENT =
            "https://schemas.identity.wso2.org/events/tenant/event-type/tenantOwnerUpdated";
    private static final String TENANT_ACTIVATED_EVENT =
            "https://schemas.identity.wso2.org/events/tenant/event-type/tenantActivated";
    /** Header the IS TenantSyncListener sends and APIM's TenantManagementEventHandler routes on. */
    private static final String KM_HEADER = "X-WSO2-KEY-MANAGER";
    private static final String KM_HEADER_VALUE = "TENANT_MANAGEMENT";

    /**
     * POSTs a {@code tenantCreated} tenant-management event to APIM's {@code /internal/data/v1/notify} as the
     * ACTING actor (basic auth — the endpoint authenticates the event sender's carbon credentials, exactly as
     * the IS TenantSyncListener does), simulating IS-side tenant creation. The synced tenant's owner is
     * {@code admin} with the given password. Publishes the response for the following status assertion.
     */
    @When("I synchronize a new tenant {string} with admin password {string} via the tenant-sharing notify endpoint")
    public void iSynchronizeTenant(String tenantDomain, String adminPassword) throws IOException {

        JSONObject owner = new JSONObject()
                .put("username", "admin")
                .put("password", adminPassword)
                .put("email", "admin@" + tenantDomain)
                .put("firstname", "Tenant")
                .put("lastname", "Sharing");
        JSONObject tenant = tenantBase(tenantDomain).put("owners", new JSONArray().put(owner));
        postTenantEvent(TENANT_CREATED_EVENT, "CREATE", tenant);
    }

    /**
     * POSTs a {@code tenantOwnerUpdated} event carrying the owner's NEW password (mirroring the legacy
     * buildPayload, which OMITS the username on an update — only CREATE carries it). After this the tenant
     * admin's OLD-password credentials are rejected and the NEW-password credentials accepted. Publishes the
     * response for the following status assertion.
     */
    @When("I notify tenant owner update for {string} with new admin password {string} "
            + "via the tenant-sharing notify endpoint")
    public void iNotifyTenantOwnerUpdate(String tenantDomain, String newAdminPassword) throws IOException {

        // Legacy OMITS username on the update event — only the new password + profile fields are sent.
        JSONObject owner = new JSONObject()
                .put("password", newAdminPassword)
                .put("email", "admin@" + tenantDomain)
                .put("firstname", "Tenant")
                .put("lastname", "Sharing");
        JSONObject tenant = tenantBase(tenantDomain).put("owners", new JSONArray().put(owner));
        postTenantEvent(TENANT_OWNER_UPDATED_EVENT, "UPDATE", tenant);
    }

    /**
     * POSTs a {@code tenantActivated} event with {@code lifecycleStatus.activated} = the given flag (legacy
     * uses the SAME event URI for both activation and deactivation, distinguished only by this flag and the
     * ACTIVATE/DEACTIVATE action string). After this the tenant admin's token issuance succeeds (activated) or
     * fails (deactivated). Publishes the response for the following status assertion.
     */
    @When("I notify tenant {string} activation status {string} via the tenant-sharing notify endpoint")
    public void iNotifyTenantActivation(String tenantDomain, String activated) throws IOException {

        boolean isActive = Boolean.parseBoolean(activated);
        JSONObject tenant = tenantBase(tenantDomain)
                .put("lifecycleStatus", new JSONObject().put("activated", isActive));
        postTenantEvent(TENANT_ACTIVATED_EVENT, isActive ? "ACTIVATE" : "DEACTIVATE", tenant);
    }

    /**
     * The same {@code tenantActivated} notify as {@link #iNotifyTenantActivation}, but RE-POSTED until the endpoint
     * answers {@code expectedStatus}. Needed only for the DEACTIVATE direction, where the handler's synchronous
     * TenantMgtAdminService self-call is intermittently unavailable on this lane and the notify then answers
     * {@code 500 {"Message":"Error while executing tenant management service"}} — observed on ONE of the two
     * deactivate calls in a single locked run while the other answered 200
     * (/tmp/w10w8-run4-locked-deliveryprobe.log), so it is genuinely nondeterministic rather than a fixed lane
     * behaviour.
     *
     * <p>This is {@code retryUntil} and not {@code awaitWithRetry} on purpose (§15): the STATUS is the assertion
     * target of the row being ported (the legacy {@code assertEquals(SC_OK)}), so the retry must fail loudly if a
     * 200 is never reached rather than heal a prerequisite. Re-posting is safe because the event is idempotent —
     * it carries the desired {@code lifecycleStatus.activated} flag rather than a toggle. The LAST response stays
     * published, so the following {@code The response status code should be} assertion is a real assertion on a
     * real response and not a widened "any status passes" form.
     */
    @When("I notify tenant {string} activation status {string} via the tenant-sharing notify endpoint until it "
            + "returns status {int} within {int} seconds")
    public void iNotifyTenantActivationUntilStatus(String tenantDomain, String activated, int expectedStatus,
                                                   int timeoutSeconds) throws Exception {

        Integer reached = Utils.retryUntil(timeoutSeconds * 1000L, () -> {
            iNotifyTenantActivation(tenantDomain, activated);
            return ((HttpResponse) TestContext.get("httpResponse")).getResponseCode();
        }, status -> status == expectedStatus);
        HttpResponse last = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertEquals(reached.intValue(), expectedStatus, "The tenantActivated notify for '" + tenantDomain
                + "' (activated=" + activated + ") never answered " + expectedStatus + " within " + timeoutSeconds
                + "s; last response: " + (last == null ? "<none>" : last.getResponseCode() + " " + last.getData()));
    }

    /** The tenant sub-object common to every tenant-management event (id/domain/ref), mirroring the legacy. */
    private static JSONObject tenantBase(String tenantDomain) {
        return new JSONObject()
                .put("id", "1234")
                .put("domain", tenantDomain)
                .put("ref", "https://wso2is:9443/api/server/v1/tenants/1234");
    }

    /**
     * Builds the tenant-management envelope for {@code eventUri}/{@code action} around the given tenant object
     * and POSTs it to APIM's {@code /internal/data/v1/notify} as the ACTING actor (basic auth — the endpoint
     * authenticates the event sender's carbon credentials, exactly as the IS TenantSyncListener does).
     * Publishes the response for the following status assertion.
     */
    private static void postTenantEvent(String eventUri, String action, JSONObject tenant) throws IOException {

        JSONObject detail = new JSONObject()
                .put("initiatorType", "SYSTEM")
                .put("action", action)
                .put("tenant", tenant);
        JSONObject event = new JSONObject()
                .put("iss", "https://wso2is:9443")
                .put("jti", UUID.randomUUID().toString())
                .put("iat", System.currentTimeMillis() / 1000L)
                .put("events", new JSONObject().put(eventUri, detail));

        User actor = Identity.actingActor();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(
                (actor.getUserName() + ":" + actor.getPassword()).getBytes(StandardCharsets.UTF_8)));
        headers.put(KM_HEADER, KM_HEADER_VALUE);
        Requests.post(Utils.getBaseUrl() + "internal/data/v1/notify", headers, event.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Registers a notify-synced tenant's admin as a runtime ACTOR ({@code "admin@<domain>"} reference form),
     * awaiting the tenant's async activation. After this the standard auth composites and steps work for the
     * actor, and resources it creates are cleanup-swept as their owner.
     */
    @When("I register the runtime tenant admin {string} with password {string} as an actor")
    public void iRegisterRuntimeTenantAdmin(String adminRef, String password) {

        Assert.assertTrue(adminRef.startsWith("admin@"),
                "Runtime tenant admin reference must be of the form admin@<domain>, got: " + adminRef);
        String tenantDomain = adminRef.substring("admin@".length());
        TenantUserProvisioner.registerRuntimeTenantAdmin(tenantDomain, password);
    }

    /**
     * Lists the ACTING actor's tenant key managers via the admin REST API and asserts the exact count. Stashes
     * the list for the type-membership assertion below.
     */
    @Then("the key manager list for the acting actor has {int} entries")
    public void theKeyManagerListHasEntries(int expectedCount) throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = Requests.get(Utils.getKeyManagersURL(Utils.getBaseUrl()), headers);
        Assert.assertTrue(response != null && response.getResponseCode() == 200 && response.getData() != null
                        && !response.getData().isBlank(),
                "Key manager list failed for " + Identity.actingActor().getUserName() + ": got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));
        JSONObject body = new JSONObject(response.getData());
        Assert.assertEquals(body.getInt("count"), expectedCount,
                "Unexpected key manager count for " + Identity.actingActor().getUserName()
                        + " - list=" + response.getData());
        TestContext.set(KM_LIST_KEY, response.getData());
    }

    /** Asserts the stashed key-manager list contains an entry of the given connector type. */
    @Then("the key manager list includes a {string} key manager")
    public void theKeyManagerListIncludesType(String type) {

        Object list = TestContext.get(KM_LIST_KEY);
        Assert.assertNotNull(list, "No key manager list captured; assert the count first");
        JSONArray managers = new JSONObject(list.toString()).getJSONArray("list");
        boolean found = false;
        for (int i = 0; i < managers.length(); i++) {
            if (type.equals(managers.getJSONObject(i).optString("type"))) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found, "No key manager of type '" + type + "' in " + list);
    }

    /** Asserts the stashed key-manager list contains an entry with the given display name. */
    @Then("the key manager list includes a key manager named {string}")
    public void theKeyManagerListIncludesName(String name) {

        Object list = TestContext.get(KM_LIST_KEY);
        Assert.assertNotNull(list, "No key manager list captured; assert the count first");
        JSONArray managers = new JSONObject(list.toString()).getJSONArray("list");
        boolean found = false;
        for (int i = 0; i < managers.length(); i++) {
            if (name.equals(managers.getJSONObject(i).optString("name"))) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found, "No key manager named '" + name + "' in " + list);
    }

    /**
     * Asserts a tenant admin's credentials are ACCEPTED — polling until a password-grant token is issued (200).
     * This is the observable the legacy update/activate assertions used ({@code new RestAPIAdminImpl(...)}
     * succeeding): a full DCR + password-grant round trip as {@code admin@<domain>} with the given password.
     * Polled because the notify event is processed asynchronously (an IS-side SOAP self-call), so the new
     * password / re-activation takes a user-store-propagation moment to take effect.
     */
    @Then("a token request for tenant admin {string} with password {string} eventually succeeds")
    public void aTokenRequestEventuallySucceeds(String adminRef, String password) throws Exception {

        HttpResponse response = Utils.retryUntil(Constants.RUNTIME_PROPAGATION_TIMEOUT,
                () -> attemptAdminToken(adminRef, password),
                resp -> resp != null && resp.getResponseCode() == 200);
        Assert.assertNotNull(response, "No response attempting a token for " + adminRef);
        Assert.assertEquals(response.getResponseCode(), 200,
                "Expected the credentials of " + adminRef + " to be accepted (token issued), but got="
                        + response.getResponseCode() + "/" + response.getData());
    }

    /**
     * Asserts a tenant admin's credentials are REJECTED with the given EXACT status — polling until the reject
     * is observed. The rejection can surface at either leg of the credential check (DCR basic-auth or the
     * password grant), whichever the product refuses first; {@link #attemptAdminToken} returns that failing
     * response. Polled because the notify event is processed asynchronously, so the old password / deactivation
     * takes a propagation moment to take effect (a stale 200 must not slip through as a false pass).
     */
    @Then("a token request for tenant admin {string} with password {string} is eventually rejected with status {int}")
    public void aTokenRequestEventuallyRejected(String adminRef, String password, int expectedStatus)
            throws Exception {

        HttpResponse response = Utils.retryUntil(Constants.RUNTIME_PROPAGATION_TIMEOUT,
                () -> attemptAdminToken(adminRef, password),
                resp -> resp != null && resp.getResponseCode() == expectedStatus);
        Assert.assertNotNull(response, "No response attempting a token for " + adminRef);
        Assert.assertEquals(response.getResponseCode(), expectedStatus,
                "Expected the credentials of " + adminRef + " to be rejected with " + expectedStatus
                        + ", but got=" + response.getResponseCode() + "/" + response.getData());
    }

    /**
     * Performs a fresh DCR + password-grant round trip for {@code admin@<domain>} with the given password,
     * returning the response of whichever leg is the outcome: the DCR response if DCR itself fails (the admin's
     * carbon credentials are refused — the first gate the legacy {@code RestAPIAdminImpl} constructor hits), or
     * the token response otherwise. Not a scenario-owned assertion target and not published to
     * {@code httpResponse}; the retry envelope in the caller asserts on it. Uses {@link SimpleHTTPClient}
     * directly (raw round trip consumed locally), never cached actor tokens — the point is to exercise these
     * exact credentials from scratch every attempt. A unique DCR clientName per attempt keeps parallel blocks
     * isolated (DCR is an idempotent upsert by clientName). Only {@code IOException} propagates (retried by the
     * envelope); a non-2xx HTTP response is returned as-is so the caller pins the exact status.
     */
    private static HttpResponse attemptAdminToken(String adminRef, String password) throws IOException {

        Assert.assertTrue(adminRef.startsWith("admin@"),
                "Tenant admin reference must be of the form admin@<domain>, got: " + adminRef);
        // adminRef ("admin@<domain>") IS the login username for a tenant admin.
        String username = adminRef;
        Map<String, String> dcrBasic = Identity.basicAuthHeaders(username, password);
        String dcrBody = new JSONObject()
                .put("callbackUrl", "www.google.lk")
                .put("clientName", "tenantSyncProbe_" + username.replaceAll("[^a-zA-Z0-9]", "_")
                        + "_" + UUID.randomUUID())
                .put("grantType", "password")
                .put("saasApp", true)
                .put("owner", username)
                .toString();
        HttpResponse dcr = SimpleHTTPClient.getInstance().doPost(Utils.getDCREndpointURL(Utils.getBaseUrl()),
                dcrBasic, dcrBody, Constants.CONTENT_TYPES.APPLICATION_JSON);
        if (dcr == null || dcr.getResponseCode() != 200 || dcr.getData() == null || dcr.getData().isBlank()) {
            // DCR (the admin's own basic-auth) is the first credential gate — a rejected password / deactivated
            // tenant is refused here. Return that response so the caller pins its exact status.
            return dcr;
        }
        JSONObject creds = new JSONObject(dcr.getData());
        String tokenBasic = Base64.getEncoder().encodeToString((creds.getString("clientId") + ":"
                + creds.getString("clientSecret")).getBytes(StandardCharsets.UTF_8));
        Map<String, String> tokenHeaders = new HashMap<>();
        tokenHeaders.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic " + tokenBasic);
        String form = "grant_type=password&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8) + "&scope=openid";
        return SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(Utils.getBaseUrl()),
                tokenHeaders, form, "application/x-www-form-urlencoded");
    }
}
