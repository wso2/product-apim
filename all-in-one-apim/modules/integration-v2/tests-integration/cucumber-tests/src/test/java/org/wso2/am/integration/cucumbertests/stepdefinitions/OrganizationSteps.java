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
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.ResourceCleanup;
import org.wso2.am.integration.cucumbertests.utils.TenantUserProvisioner;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Step definitions for B2B organizations (org visibility): registering the {@code organizationId} local claim
 * (SOAP {@code ClaimMetadataManagementService.addLocalClaim}) and organization CRUD over the admin
 * {@code /organizations} REST API. Phase-1 harness for the ConsumerOrganizationVisibility port.
 */
public class OrganizationSteps {

    BaseSteps baseSteps = new BaseSteps();

    /**
     * Registers the {@code http://wso2.org/claims/organizationId} local claim (mapped to the {@code
     * organizationId} attribute on the PRIMARY user store) via the ClaimMetadataManagementService SOAP admin
     * service. Idempotent-tolerant: if the claim already exists the service returns a fault, which the feature
     * can ignore. Non-asserting — the raw response is published as {@code httpResponse}.
     */
    @When("I register the organization local claim")
    public void iRegisterOrganizationLocalClaim() throws IOException {
        registerOrganizationLocalClaim(Constants.SUPER_TENANT_ADMIN_USERNAME, Constants.SUPER_TENANT_ADMIN_PASSWORD);
    }

    /** Tenant variant — registers the org claim using the given tenant's admin credentials. */
    @When("I register the organization local claim in tenant {string}")
    public void iRegisterOrganizationLocalClaimInTenant(String tenantDomain) throws IOException {
        org.wso2.carbon.automation.engine.context.beans.User admin =
                Utils.getTenantFromContext(tenantDomain).getTenantAdmin();
        registerOrganizationLocalClaim(admin.getUserName(), admin.getPassword());
    }

    private void registerOrganizationLocalClaim(String adminUser, String adminPass) throws IOException {

        String payload =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                        + "xmlns:ax=\"http://org.apache.axis2/xsd\" "
                        + "xmlns:dto=\"http://dto.mgt.metadata.claim.identity.carbon.wso2.org/xsd\">"
                        + "<soapenv:Header/><soapenv:Body>"
                        + "<ax:addLocalClaim><ax:localClaim>"
                        + "<dto:localClaimURI>http://wso2.org/claims/organizationId</dto:localClaimURI>"
                        + "<dto:attributeMappings>"
                        + "<dto:attributeName>organizationId</dto:attributeName>"
                        + "<dto:userStoreDomain>PRIMARY</dto:userStoreDomain>"
                        + "</dto:attributeMappings>"
                        + "<dto:claimProperties><dto:propertyName>DisplayName</dto:propertyName>"
                        + "<dto:propertyValue>Organization Id</dto:propertyValue></dto:claimProperties>"
                        + "<dto:claimProperties><dto:propertyName>Description</dto:propertyName>"
                        + "<dto:propertyValue>Organization Id</dto:propertyValue></dto:claimProperties>"
                        + "<dto:claimProperties><dto:propertyName>SupportedByDefault</dto:propertyName>"
                        + "<dto:propertyValue>true</dto:propertyValue></dto:claimProperties>"
                        + "</ax:localClaim></ax:addLocalClaim>"
                        + "</soapenv:Body></soapenv:Envelope>";

        Requests.soap(
                Utils.getClaimMetadataMgtServiceURL(Utils.getBaseUrl()), payload, "urn:addLocalClaim",
                adminUser, adminPass);
    }

    /**
     * Sets a user's {@code organizationId} claim (SOAP {@code RemoteUserStoreManagerService.setUserClaimValue})
     * — i.e. makes the user "belong to" the given organization. Non-asserting.
     *
     * @param username the (bare, tenant-less) username
     * @param orgId    the external organization id to tag the user with
     */
    @When("I set the organization claim of user {string} to {string}")
    public void iSetOrganizationClaim(String username, String orgId) throws IOException {
        setOrganizationClaim(username, orgId, Constants.SUPER_TENANT_ADMIN_USERNAME,
                Constants.SUPER_TENANT_ADMIN_PASSWORD);
    }

    /** Tenant variant — sets the org claim on a user using the given tenant's admin credentials. */
    @When("I set the organization claim of user {string} in tenant {string} to {string}")
    public void iSetOrganizationClaimInTenant(String username, String tenantDomain, String orgId)
            throws IOException {
        org.wso2.carbon.automation.engine.context.beans.User admin =
                Utils.getTenantFromContext(tenantDomain).getTenantAdmin();
        setOrganizationClaim(username, orgId, admin.getUserName(), admin.getPassword());
    }

    private void setOrganizationClaim(String username, String orgId, String adminUser, String adminPass)
            throws IOException {

        String payload =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                        + "xmlns:ser=\"http://service.ws.um.carbon.wso2.org\">"
                        + "<soapenv:Header/><soapenv:Body>"
                        + "<ser:setUserClaimValue>"
                        + "<ser:userName>" + Utils.escapeXml(username) + "</ser:userName>"
                        + "<ser:claimURI>http://wso2.org/claims/organizationId</ser:claimURI>"
                        + "<ser:claimValue>" + Utils.escapeXml(orgId) + "</ser:claimValue>"
                        + "<ser:profileName>default</ser:profileName>"
                        + "</ser:setUserClaimValue>"
                        + "</soapenv:Body></soapenv:Envelope>";

        Requests.soap(
                Utils.getRemoteUserStoreManagerServiceURL(Utils.getBaseUrl()), payload, "urn:setUserClaimValue",
                adminUser, adminPass);
    }

    /**
     * Creates an organization via the admin REST API, asserts 201, and stores the returned {@code
     * organizationId} under {@code idKey}.
     */
    @When("I create an organization {string} with display name {string} as {string}")
    public void iCreateOrganization(String externalOrgId, String displayName, String idKey) throws IOException {

        String resolvedExternalId = Utils.resolvePayloadPlaceholders(externalOrgId);
        JSONObject orgPayload = new JSONObject();
        orgPayload.put("externalOrganizationId", resolvedExternalId);
        orgPayload.put("displayName", displayName);

        HttpResponse response = Requests.post(Utils.getOrganizationsURL(Utils.getBaseUrl()),
                Identity.adminHeaders(), orgPayload.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        // Store the internal UUID (used in an API's visibleOrganizations) under idKey, and the external id
        // (used to tag org users' organizationId claim) under <idKey>External.
        Object organizationId = Utils.extractValueFromPayload(response.getData(), "organizationId");
        // Register for failure-safe teardown: the feature deletes orgs inline on the happy path, but an
        // earlier-step failure would skip that and leak the org. ResourceCleanup sweeps them as admin.
        ResourceCleanup.register(Constants.CREATED_ORGANIZATION_IDS, String.valueOf(organizationId));
        TestContext.set(idKey, organizationId);
        TestContext.set(idKey + "External", resolvedExternalId);
    }

    /** Lists all organizations. */
    @When("I retrieve all organizations")
    public void iRetrieveAllOrganizations() throws IOException {

        Requests.get(Utils.getOrganizationsURL(Utils.getBaseUrl()), Identity.adminHeaders());
    }

    /** Deletes the organization held under {@code idKey}. Non-asserting — the feature asserts the status. */
    @When("I delete the organization {string}")
    public void iDeleteOrganization(String idKey) throws IOException {

        String orgId = TestContext.resolve(idKey).toString();
        Requests.delete(Utils.getOrganizationByIdURL(Utils.getBaseUrl(), orgId), Identity.adminHeaders());
    }

    /**
     * Provisions a super-tenant user (SOAP addUser via {@link TenantUserProvisioner}) registered under
     * {@code userKey} as a resolvable actor, then tags it with the {@code organizationId} claim so it "belongs
     * to" the given organization. The username/password both equal {@code userKey}. Mint the user's token AFTER
     * this (the org membership must be present before token issue).
     *
     * @param userKey the actor key/username (also the password)
     * @param roles   comma-separated roles (e.g. {@code Internal/subscriber})
     * @param orgId   the external organization id (resolves {@code {{...}}}) to tag the user with
     */
    @When("I provision organization user {string} with roles {string} in organization {string}")
    public void iProvisionOrganizationUser(String userKey, String roles, String orgId) throws Exception {

        TenantUserProvisioner.addUser(Constants.SUPER_TENANT_DOMAIN, userKey, userKey, userKey, roles);
        iSetOrganizationClaim(userKey, Utils.resolveContextPlaceholders(orgId));
    }

    /** Tenant variant — provisions the org user in the given tenant, tagging its org claim with that tenant's admin. */
    @When("I provision organization user {string} with roles {string} in organization {string} in tenant {string}")
    public void iProvisionOrganizationUserInTenant(String userKey, String roles, String orgId, String tenantDomain)
            throws Exception {

        TenantUserProvisioner.addUser(tenantDomain, userKey, userKey, userKey, roles);
        iSetOrganizationClaimInTenant(userKey, tenantDomain, Utils.resolveContextPlaceholders(orgId));
    }

    /**
     * Provisions a plain super-tenant user (SOAP addUser) registered as a resolvable actor under {@code userKey}
     * (username = password = userKey) with the given comma-separated roles — no organization claim. Enabler for
     * role/permission tests (e.g. a second admin, or a role-scoped user). Mint the user's token afterwards with
     * {@code I have valid access tokens as "<userKey>"}.
     *
     * @param userKey the actor key/username (also the password)
     * @param roles   comma-separated roles (e.g. {@code admin} or {@code Internal/subscriber})
     */
    @When("I provision user {string} with roles {string}")
    public void iProvisionUser(String userKey, String roles) throws Exception {

        TenantUserProvisioner.addUser(Constants.SUPER_TENANT_DOMAIN, userKey, userKey, userKey,
                Utils.resolveContextPlaceholders(roles));
    }

    /** Tenant variant of {@link #iProvisionUser}. */
    @When("I provision user {string} with roles {string} in tenant {string}")
    public void iProvisionUserInTenant(String userKey, String roles, String tenantDomain) throws Exception {

        TenantUserProvisioner.addUser(tenantDomain, userKey, userKey, userKey,
                Utils.resolveContextPlaceholders(roles));
    }

    /**
     * Provisions an (empty) internal role in the acting actor's tenant (SOAP addRole). Enabler for access-control
     * tests: an API restricted to a role can only be authored/exported by users carrying it. Idempotent.
     *
     * @param roleName the (unqualified) role name to create
     */
    @When("I provision role {string}")
    public void iProvisionRole(String roleName) throws Exception {

        String tenantDomain = Identity.actingActor().getUserDomain();
        TenantUserProvisioner.addRole(tenantDomain, Utils.resolveContextPlaceholders(roleName));
    }

    /** Tenant variant of {@link #iProvisionRole}. */
    @When("I provision role {string} in tenant {string}")
    public void iProvisionRoleInTenant(String roleName, String tenantDomain) throws Exception {

        TenantUserProvisioner.addRole(tenantDomain, Utils.resolveContextPlaceholders(roleName));
    }

    /**
     * Provisions a role carrying the store-login + subscribe permissions (SOAP addRole with permissions). A role
     * used as an API's {@code visibleRoles} (DevPortal store visibility RESTRICTED) MUST carry
     * {@code /permission/admin/login}, or the publisher API-create rejects it with 900610 "Invalid user roles
     * found" — an empty role suffices for {@code accessControlRoles} but NOT for {@code visibleRoles}. Enabler for
     * the store-visibility tests.
     */
    @When("I provision store-visibility role {string} in tenant {string}")
    public void iProvisionStoreVisibilityRoleInTenant(String roleName, String tenantDomain) throws Exception {

        TenantUserProvisioner.addRole(tenantDomain, Utils.resolveContextPlaceholders(roleName), true);
    }

    /**
     * Registers the OIDC user-profile claim mappings (mobile/organization external claims) and binds the
     * profile claims to the {@code openid} scope, in the given tenant — so a token requesting {@code openid}
     * surfaces those claims. Ports the claim-mapping setup of JWTTestCase's user-profile-claims case.
     */
    @When("I register the OIDC user-profile claim mappings and scope in tenant {string}")
    public void iRegisterOidcClaims(String tenantDomain) throws Exception {
        TenantUserProvisioner.addOidcExternalClaim(tenantDomain, "mobile", "http://wso2.org/claims/mobile");
        TenantUserProvisioner.addOidcExternalClaim(tenantDomain, "organization", "http://wso2.org/claims/organization");
        TenantUserProvisioner.updateOidcScopeClaims(tenantDomain, "openid",
                new String[] {"given_name", "family_name", "mobile", "organization"});
    }

    /** Sets a user-profile claim value on a user (RemoteUserStoreManagerService setUserClaimValue) in a tenant. */
    @When("I set the user claim {string} to {string} for user {string} in tenant {string}")
    public void iSetUserClaim(String claimUri, String claimValue, String username, String tenantDomain)
            throws Exception {
        TenantUserProvisioner.setUserClaimValue(tenantDomain, Utils.resolveContextPlaceholders(username), claimUri,
                Utils.resolveContextPlaceholders(claimValue));
    }

    /**
     * Configures the OAuth service provider backing {@code consumerKey} to REQUEST the user-profile claims (the
     * backend JWT only surfaces claims the SP requests), in the given tenant.
     */
    @When("I configure the service provider for consumer key {string} to request the user-profile claims in tenant {string}")
    public void iRequestUserProfileClaims(String consumerKeyRef, String tenantDomain) throws Exception {
        String ck = TestContext.resolve(consumerKeyRef).toString();
        TenantUserProvisioner.addRequestedClaimsToServiceProvider(tenantDomain, ck,
                new String[] {"http://wso2.org/claims/givenname", "http://wso2.org/claims/lastname",
                        "http://wso2.org/claims/mobile", "http://wso2.org/claims/organization"});
    }

    /**
     * Sets an API's {@code visibleOrganizations} (GET the publisher API → replace the field → PUT). The value is
     * {@code none}, {@code all}, or an organization UUID (resolves {@code {{...}}}). Uses the acting actor's
     * publisher token. Non-asserting.
     */
    @When("I set the visible organizations of API {string} to {string}")
    public void iSetVisibleOrganizations(String apiIdKey, String value) throws IOException {

        String apiId = TestContext.resolve(apiIdKey).toString();
        String resolved = Utils.resolveContextPlaceholders(value);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        HttpResponse current = SimpleHTTPClient.getInstance()
                .doGet(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "apis", apiId), headers);
        // Intermediate GET of a GET→mutate→PUT: confirm a 2xx response WITH a body before parsing, so a
        // failed/empty fetch fails clearly instead of throwing an opaque JSONException/NPE.
        Assert.assertTrue(current != null && current.getResponseCode() >= 200
                        && current.getResponseCode() < 300 && current.getData() != null
                        && !current.getData().isEmpty(),
                "Failed to fetch API '" + apiId + "' before setting its visible organizations: expected a 2xx "
                        + "response with a body, got " + (current == null ? "no response"
                        : current.getResponseCode() + " / body=" + current.getData()));
        JSONObject api = new JSONObject(current.getData());
        api.put("visibleOrganizations", new JSONArray().put(resolved));

        Requests.put(
                Utils.getResourceEndpointURL(Utils.getBaseUrl(), "apis", apiId), headers, api.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Sets an API's per-organization subscription policies (and scopes visibility to that org): GET the
     * publisher API → set {@code visibleOrganizations=[org]} and {@code organizationPolicies=[{org, [tier]}]} →
     * PUT. Uses the acting actor's publisher token. Non-asserting.
     */
    @When("I set organization policies of API {string} for organization {string} to tier {string}")
    public void iSetOrganizationPolicies(String apiIdKey, String orgUUID, String tier) throws IOException {

        String apiId = TestContext.resolve(apiIdKey).toString();
        String orgId = Utils.resolveContextPlaceholders(orgUUID);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        HttpResponse current = SimpleHTTPClient.getInstance()
                .doGet(Utils.getResourceEndpointURL(Utils.getBaseUrl(), "apis", apiId), headers);
        // Intermediate GET of a GET→mutate→PUT: confirm a 2xx response WITH a body before parsing, so a
        // failed/empty fetch fails clearly instead of throwing an opaque JSONException/NPE.
        Assert.assertTrue(current != null && current.getResponseCode() >= 200
                        && current.getResponseCode() < 300 && current.getData() != null
                        && !current.getData().isEmpty(),
                "Failed to fetch API '" + apiId + "' before setting its organization policies: expected a 2xx "
                        + "response with a body, got " + (current == null ? "no response"
                        : current.getResponseCode() + " / body=" + current.getData()));
        JSONObject api = new JSONObject(current.getData());
        api.put("visibleOrganizations", new JSONArray().put(orgId));
        JSONObject policy = new JSONObject();
        policy.put("organizationID", orgId);
        policy.put("policies", new JSONArray().put(tier));
        api.put("organizationPolicies", new JSONArray().put(policy));

        Requests.put(
                Utils.getResourceEndpointURL(Utils.getBaseUrl(), "apis", apiId), headers, api.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Polls the DevPortal API-get (acting actor) until the response body contains {@code marker} (eventually
     * consistent after an org-policy change). Used to assert an org's available subscription tier.
     */
    @When("I retrieve the devportal API {string} until it contains {string} within {int} seconds")
    public void iRetrieveDevportalApiUntilContains(String apiIdKey, String marker, int timeoutSeconds)
            throws InterruptedException {

        String apiId = TestContext.resolve(apiIdKey).toString();
        String expected = Utils.resolveContextPlaceholders(marker);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        long deadlineStart = System.currentTimeMillis();
        long deadline = deadlineStart + Math.max(timeoutSeconds * 1000L, Constants.RUNTIME_PROPAGATION_TIMEOUT);
        HttpResponse last = null;
        boolean found = false;
        while (System.currentTimeMillis() < deadline) {
            try {
                last = SimpleHTTPClient.getInstance()
                        .doGet(Utils.getDevportalApiDetailURL(Utils.getBaseUrl(), apiId), headers);
                // Body null-guarded: a 200 with no body counts as still-pending rather than NPE-ing out of the
                // IOException-only catch and killing the poll.
                if (last.getResponseCode() == 200 && last.getData() != null && last.getData().contains(expected)) {
                    found = true;
                    break;
                }
            } catch (IOException transientDuringWarmup) {
                // retry transient connectivity only
            }
            Utils.pollPause(deadlineStart, 2000);
        }
        TestContext.set("httpResponse", last);
        Assert.assertNotNull(last, "No devportal response received for API " + apiId);
        Assert.assertTrue(found, "DevPortal API did not contain '" + expected + "' within "
                + timeoutSeconds + "s; last: " + (last == null ? "null" : last.getData()));
    }

    /** Retrieves an API from the DevPortal as the acting actor (its devportal token) — 200 visible / 403 not. */
    @When("I retrieve the devportal API {string}")
    public void iRetrieveDevportalApi(String apiIdKey) throws IOException {

        String apiId = TestContext.resolve(apiIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        Requests.get(Utils.getDevportalApiDetailURL(Utils.getBaseUrl(), apiId), headers);
    }

    /** Retrieves an API from the DevPortal with NO authentication (anonymous user). */
    @When("I retrieve the devportal API {string} anonymously")
    public void iRetrieveDevportalApiAnonymously(String apiIdKey) throws IOException {

        String apiId = TestContext.resolve(apiIdKey).toString();
        Requests.get(Utils.getDevportalApiDetailURL(Utils.getBaseUrl(), apiId), new HashMap<>());
    }

    /**
     * Polls the DevPortal API-get (acting actor's devportal token) until it returns {@code expectedStatus} or
     * the deadline passes. DevPortal visibility after a {@code visibleOrganizations} change is eventually
     * consistent, so a single GET can catch stale state — this retries. Asserts the status after the loop.
     */
    @When("I retrieve the devportal API {string} until the response status code becomes {int} within {int} seconds")
    public void iRetrieveDevportalApiUntil(String apiIdKey, int expectedStatus, int timeoutSeconds)
            throws InterruptedException {

        String apiId = TestContext.resolve(apiIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        pollDevportalApiUntil(apiId, headers, expectedStatus, timeoutSeconds);
    }

    /**
     * As above, anonymously (no auth) but with the DevPortal tenant context ({@code X-WSO2-Tenant}) so a tenant
     * API resolves for the anonymous caller (without it, a tenant API returns 404 rather than 403/200).
     */
    @When("I retrieve the devportal API {string} anonymously in tenant {string} until the response status code becomes {int} within {int} seconds")
    public void iRetrieveDevportalApiAnonInTenantUntil(String apiIdKey, String tenantDomain, int expectedStatus,
                                                       int timeoutSeconds) throws InterruptedException {

        String apiId = TestContext.resolve(apiIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put("X-WSO2-Tenant", tenantDomain);
        pollDevportalApiUntil(apiId, headers, expectedStatus, timeoutSeconds);
    }

    /**
     * Polls the DevPortal key-manager list (acting actor's devportal token) until it does / does not contain the
     * given id, within the deadline. KM org visibility is eventually consistent, so this retries.
     */
    @When("I retrieve the devportal key managers until it contains {string} within {int} seconds")
    public void iDevportalKeyManagersUntilContains(String idKey, int timeoutSeconds) throws InterruptedException {
        pollDevportalKeyManagers(TestContext.resolve(idKey).toString(), true, timeoutSeconds);
    }

    @When("I retrieve the devportal key managers until it does not contain {string} within {int} seconds")
    public void iDevportalKeyManagersUntilAbsent(String idKey, int timeoutSeconds) throws InterruptedException {
        pollDevportalKeyManagers(TestContext.resolve(idKey).toString(), false, timeoutSeconds);
    }

    private void pollDevportalKeyManagers(String kmId, boolean shouldContain, int timeoutSeconds)
            throws InterruptedException {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        long deadlineStart = System.currentTimeMillis();
        long deadline = deadlineStart + Math.max(timeoutSeconds * 1000L, Constants.RUNTIME_PROPAGATION_TIMEOUT);
        HttpResponse last = null;
        boolean present = !shouldContain;
        while (System.currentTimeMillis() < deadline) {
            try {
                last = SimpleHTTPClient.getInstance().doGet(Utils.getDevportalKeyManagersURL(Utils.getBaseUrl()), headers);
                // Body null-guarded: a 200 with no body counts as still-pending rather than NPE-ing out of the
                // IOException-only catch and killing the poll.
                if (last.getResponseCode() == 200 && last.getData() != null) {
                    present = last.getData().contains(kmId);
                    if (present == shouldContain) {
                        break;
                    }
                }
            } catch (IOException transientDuringWarmup) {
                // retry transient connectivity only
            }
            Utils.pollPause(deadlineStart, 2000);
        }
        TestContext.set("httpResponse", last);
        Assert.assertNotNull(last, "No devportal key-manager response received");
        Assert.assertEquals(present, shouldContain,
                "DevPortal key-manager " + kmId + (shouldContain ? " not visible" : " still visible")
                        + " within " + timeoutSeconds + "s; last: " + last.getData());
    }

    private void pollDevportalApiUntil(String apiId, Map<String, String> headers, int expectedStatus,
                                       int timeoutSeconds) throws InterruptedException {

        long deadlineStart = System.currentTimeMillis();
        long deadline = deadlineStart + Math.max(timeoutSeconds * 1000L, Constants.RUNTIME_PROPAGATION_TIMEOUT);
        HttpResponse last = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                last = SimpleHTTPClient.getInstance().doGet(Utils.getDevportalApiDetailURL(Utils.getBaseUrl(), apiId),
                        headers);
                if (last.getResponseCode() == expectedStatus) {
                    break;
                }
            } catch (IOException transientDuringWarmup) {
                // retry transient connectivity only
            }
            Utils.pollPause(deadlineStart, 2000);
        }
        TestContext.set("httpResponse", last);
        Assert.assertNotNull(last, "No devportal response received for API " + apiId);
        Assert.assertEquals(last.getResponseCode(), expectedStatus,
                "DevPortal API visibility did not reach " + expectedStatus + " within " + timeoutSeconds
                        + "s; last: " + last.getData());
    }
}
