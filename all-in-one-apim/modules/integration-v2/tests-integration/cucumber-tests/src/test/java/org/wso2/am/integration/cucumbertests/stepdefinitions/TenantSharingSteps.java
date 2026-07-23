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
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
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
        JSONObject tenant = new JSONObject()
                .put("id", "1234")
                .put("domain", tenantDomain)
                .put("ref", "https://wso2is:9443/api/server/v1/tenants/1234")
                .put("owners", new JSONArray().put(owner));
        JSONObject detail = new JSONObject()
                .put("initiatorType", "SYSTEM")
                .put("action", "CREATE")
                .put("tenant", tenant);
        JSONObject event = new JSONObject()
                .put("iss", "https://wso2is:9443")
                .put("jti", UUID.randomUUID().toString())
                .put("iat", System.currentTimeMillis() / 1000L)
                .put("events", new JSONObject().put(TENANT_CREATED_EVENT, detail));

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
}
