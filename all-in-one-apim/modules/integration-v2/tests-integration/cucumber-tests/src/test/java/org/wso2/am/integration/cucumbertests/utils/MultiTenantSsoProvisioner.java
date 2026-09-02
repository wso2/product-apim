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

package org.wso2.am.integration.cucumbertests.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provisions the multi-tenant console-SSO topology: a nested-OIDC broker chain that lets a TENANT's user
 * authenticate at an external Identity Server and land in an API Manager console.
 *
 * <p>The chain, outermost first:
 * <ol>
 *   <li>the Publisher's service provider (super tenant) federates to</li>
 *   <li>a super-tenant broker identity provider running {@code multiTenantAuthenticator}, which renders the
 *       tenant-selection page and then delegates to</li>
 *   <li>the chosen tenant's own {@code commonsp} service provider, which federates over OIDC to</li>
 *   <li>the Identity Server application in that tenant, where the user actually authenticates.</li>
 * </ol>
 *
 * <p>This is what makes a federated console login resolve to a TENANT user. A plain identity provider cannot:
 * the console's service provider is a super-tenant SaaS application, so an identity provider attached to it
 * provisions into the super tenant. The broker delegating to the tenant's own service provider is the
 * supported path.
 *
 * <p>Separate from {@link SsoProvisioner} rather than folded into it because every call here is made as a
 * DIFFERENT principal — the tenant's admin on both servers — against tenant-qualified endpoints, so these are
 * not the same primitives with an extra argument.
 *
 * <p>CLAUDE.md §14: these are SOAP admin services with no REST equivalent (identity-provider registration,
 * service-provider authentication steps, tenant creation), so a helper is acceptable — it is called from
 * {@code _setup_} steps as an actor, never from a listener hook. Artifacts live in the block's own API Manager
 * and Identity Server containers, which are destroyed at block teardown, so they need no per-resource sweep.
 */
public final class MultiTenantSsoProvisioner {

    private static final Log log = LogFactory.getLog(MultiTenantSsoProvisioner.class);

    private static final String NS_APP_OPS = "http://org.apache.axis2/xsd";
    private static final String NS_MODEL = "http://model.common.application.identity.carbon.wso2.org/xsd";
    private static final String NS_IDP_OPS = "http://mgt.idp.carbon.wso2.org";

    private static final String SCIM_CONTENT_TYPE = "application/scim+json";
    private static final String JSON_CONTENT_TYPE = "application/json";

    /** The application template the multi-tenant guide specifies, resolved by NAME at run time. */
    private static final String IS_APP_TEMPLATE_NAME = "Traditional Web Application";

    /** The grant types the multi-tenant guide enables on the common service provider's OAuth client. */
    private static final String COMMON_SP_GRANT_TYPES = "authorization_code implicit password client_credentials "
            + "refresh_token urn:ietf:params:oauth:grant-type:saml2-bearer iwa:ntlm "
            + "urn:ietf:params:oauth:grant-type:device_code urn:ietf:params:oauth:grant-type:token-exchange "
            + "urn:ietf:params:oauth:grant-type:jwt-bearer";

    /** The claim the identity provider releases group memberships under, as it arrives over OIDC. */
    private static final String GROUPS_CLAIM_URI = "groups";

    /** API Manager's own browser-facing host, as it appears in its redirects and {@code redirect_uri}s. */
    private static final String APIM_INTERNAL = "https://localhost:9443/";

    private static final Pattern APP_ID = Pattern.compile("applicationID>([^<]+)<");
    private static final Pattern CONSUMER_KEY = Pattern.compile("oauthConsumerKey>([^<]+)<");

    private MultiTenantSsoProvisioner() {
    }

    // ---------------------------------------------------------------------------------------------------------
    // Endpoints
    // ---------------------------------------------------------------------------------------------------------

    /** API Manager browser-facing base (trailing slash) from the block's shared scope. */
    private static String apimBase() {
        return Utils.getBaseUrl();
    }

    /** Identity Server browser-facing base (mapped host) — its management APIs are reached here. */
    private static String isBase() {
        return IntegrationActors.baseUrl(IntegrationActors.IS);
    }

    /**
     * Identity Server SERVER-facing base ({@code https://wso2is:9443/}). The identity providers point their
     * authorize/token/userinfo endpoints here so the {@code id_token} issuer is the same host API Manager calls
     * server-to-server, and so the browser (through its CONNECT proxy) sees the same authority.
     */
    private static String isServerBase() {
        return "https://" + org.wso2.am.testcontainers.DynamicISContainer.NETWORK_ALIAS + ":9443/";
    }

    /** {@code ""} for the super tenant, else {@code t/<domain>/} — the tenant-qualified URL prefix. */
    private static String tenantPrefix(String tenantDomain) {
        return Constants.SUPER_TENANT_DOMAIN.equals(tenantDomain) ? "" : "t/" + tenantDomain + "/";
    }

    private static String appService(String tenantDomain) {
        return apimBase() + tenantPrefix(tenantDomain) + "services/IdentityApplicationManagementService";
    }

    private static String idpService(String tenantDomain) {
        return apimBase() + tenantPrefix(tenantDomain) + "services/IdentityProviderMgtService";
    }

    private static String oauthService(String tenantDomain) {
        return apimBase() + tenantPrefix(tenantDomain) + "services/OAuthAdminService";
    }

    // ---------------------------------------------------------------------------------------------------------
    // 1) The tenant: created in the Identity Server, synchronized to API Manager
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Creates {@code domain} in the IDENTITY SERVER. The Identity Server's tenant-synchronization listener then
     * pushes the tenant to API Manager, which is the path the multi-tenant SSO guide prescribes — rather than
     * creating the tenant separately on each side and hoping they agree.
     */
    public static void createIsTenant(String domain, String tenantAdmin, String tenantAdminPassword, String email)
            throws IOException {
        String payload = new JSONObject()
                .put("domain", domain)
                .put("owners", new JSONArray().put(new JSONObject()
                        .put("username", tenantAdmin)
                        .put("password", tenantAdminPassword)
                        .put("email", email)
                        .put("firstname", "SSO")
                        .put("lastname", "Admin")
                        .put("provisioningMethod", "inline-password")))
                .toString();
        HttpResponse resp = SimpleHTTPClient.getInstance().doPost(isBase() + "api/server/v1/tenants",
                IntegrationActors.authHeaders(IntegrationActors.IS), payload, JSON_CONTENT_TYPE);
        // 409 is accepted only because the organization may already exist; it is NOT taken as evidence that the
        // organization is usable — the check below decides that for both outcomes.
        Assert.assertTrue(resp != null && (resp.getResponseCode() == 201 || resp.getResponseCode() == 409),
                "Creating organization '" + domain + "' on the identity server failed: got="
                        + (resp == null ? "null" : resp.getResponseCode() + "/" + resp.getData()));
        assertOrganizationUsable(domain, tenantAdmin, tenantAdminPassword);
    }

    /**
     * Asserts the organization is not merely created but USABLE, by making an authenticated read inside it as its
     * own admin — the equivalent of the guide's "log into the new organization".
     *
     * <p>A status code from the create call proves only that a request was accepted. This single call proves the
     * three things everything downstream depends on: the organization exists, it is addressable at
     * {@code /t/<domain>/}, and the admin credentials just set actually work there. Without it a broken
     * organization stays invisible until the next step, which then reports "creating the identity server
     * application failed" and points at the wrong thing.
     */
    private static void assertOrganizationUsable(String domain, String admin, String password) throws IOException {
        String url = isBase() + tenantPrefix(domain) + "api/server/v1/applications";
        HttpResponse resp = SimpleHTTPClient.getInstance().doGet(url,
                tenantAdminHeaders(domain, admin, password));
        Assert.assertTrue(resp != null && resp.getResponseCode() == 200,
                "Organization '" + domain + "' is not usable on the identity server: reading " + url + " as its "
                        + "own admin '" + admin + "@" + domain + "' returned "
                        + (resp == null ? "null" : resp.getResponseCode() + "/" + resp.getData())
                        + ". The organization was reported as created, so either it does not exist at /t/" + domain
                        + "/ or the admin credentials set for it do not work.");
    }

    /**
     * Waits until the tenant has been synchronized to API Manager AND activated.
     *
     * <p>Synchronization is two-phase — the listener's create event is followed by an activate event — so a
     * tenant that merely exists is not yet usable; the login would fail later and further away. The wait is
     * therefore for {@code active}, not for presence.
     */
    public static void awaitApimTenantSynced(String domain) throws InterruptedException {
        String url = apimBase() + "services/TenantMgtAdminService";
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ser=\"http://services.mgt.tenant.carbon.wso2.org\"><soapenv:Body>"
                + "<ser:getTenant><ser:tenantDomain>" + Utils.escapeXml(domain) + "</ser:tenantDomain>"
                + "</ser:getTenant></soapenv:Body></soapenv:Envelope>";
        HttpResponse last = Utils.retryUntil(0L,
                () -> SimpleHTTPClient.getInstance().sendSoapRequest(url, payload, "urn:getTenant",
                        Constants.SUPER_TENANT_ADMIN_USERNAME, Constants.SUPER_TENANT_ADMIN_PASSWORD),
                MultiTenantSsoProvisioner::reportsActiveTenant);
        Assert.assertNotNull(last, "The identity server's tenant synchronization never produced a response from "
                + "API Manager for tenant '" + domain + "'.");
        Assert.assertTrue(reportsActiveTenant(last),
                "Tenant '" + domain + "' did not become ACTIVE in API Manager within the propagation window. "
                        + "Synchronization is two-phase, so this usually means the create event arrived but the "
                        + "activate did not, leaving the tenant unusable. Last response: " + last.getData());
    }

    /** Whether a {@code getTenant} response describes a tenant that exists AND is active. */
    private static boolean reportsActiveTenant(HttpResponse resp) {
        return resp != null && resp.getData() != null
                && Pattern.compile("active>true<").matcher(resp.getData()).find();
    }

    // ---------------------------------------------------------------------------------------------------------
    // 2) Identity Server side, inside the tenant
    // ---------------------------------------------------------------------------------------------------------

    /** Basic-auth headers for a tenant admin (qualified username), used for that tenant's management APIs. */
    private static Map<String, String> tenantAdminHeaders(String tenantDomain, String admin, String password) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                (admin + "@" + tenantDomain + ":" + password).getBytes(StandardCharsets.UTF_8)));
        return headers;
    }

    /**
     * Creates the OIDC application in the Identity Server TENANT that the tenant's {@code commonsp} federates to,
     * returning {@code [clientId, clientSecret]}.
     *
     * <p>Requests the {@code groups} claim and skips login/logout consent: without the consent skip the inner leg
     * stops at a consent page that no automated flow can approve, and the login never returns.
     */
    public static String[] createTenantOidcApp(String tenantDomain, String appName, String admin, String password)
            throws IOException {
        // The tenant's own commonauth endpoint. The identity server rejects any redirect_uri not registered
        // here, so this must be the exact single value the tenant's service provider will redirect to.
        String callback = APIM_INTERNAL + tenantPrefix(tenantDomain) + "commonauth";
        String appsUrl = isBase() + tenantPrefix(tenantDomain) + "api/server/v1/applications";
        Map<String, String> headers = tenantAdminHeaders(tenantDomain, admin, password);
        JSONObject oidc = new JSONObject()
                .put("grantTypes", new JSONArray().put("authorization_code"))
                .put("callbackURLs", new JSONArray().put(callback))
                .put("allowedOrigins", new JSONArray().put("https://localhost:9443"))
                .put("publicClient", false);
        // Groups drives role mapping; the profile attributes carry the identity itself. Groups alone leaves the
        // chain with nothing but an opaque subject identifier to represent the user by.
        JSONArray requested = new JSONArray();
        for (String claimUri : new String[] {"http://wso2.org/claims/groups", "http://wso2.org/claims/username",
                "http://wso2.org/claims/givenname", "http://wso2.org/claims/lastname",
                "http://wso2.org/claims/emailaddress"}) {
            requested.put(new JSONObject()
                    .put("claim", new JSONObject().put("uri", claimUri))
                    .put("mandatory", "http://wso2.org/claims/groups".equals(claimUri)));
        }
        JSONObject claims = new JSONObject()
                .put("dialect", "LOCAL")
                .put("requestedClaims", requested)
                // Username as the alternate subject identifier, so the subject the chain propagates is the
                // tenant user's name rather than an opaque generated id.
                .put("subject", new JSONObject()
                        .put("claim", new JSONObject().put("uri", "http://wso2.org/claims/username")));
        String payload = new JSONObject()
                .put("name", appName)
                .put("templateId", resolveTemplateId(tenantDomain, headers))
                .put("inboundProtocolConfiguration", new JSONObject().put("oidc", oidc))
                .put("claimConfiguration", claims)
                .toString();

        HttpResponse created = SimpleHTTPClient.getInstance().doPost(appsUrl, headers, payload, JSON_CONTENT_TYPE);
        Assert.assertTrue(created != null && created.getResponseCode() == 201,
                "Creating the identity server application in tenant '" + tenantDomain + "' failed: got="
                        + (created == null ? "null" : created.getResponseCode() + "/" + created.getData()));

        String appId = locationId(created);
        Assert.assertNotNull(appId, "No application id returned when creating '" + appName + "' in tenant '"
                + tenantDomain + "'.");

        // Consent is skipped by PATCHING the created application: the create call rejects
        // advancedConfigurations. Without this the inner leg stops on a consent page that the journey cannot
        // approve, and the login never returns.
        String consentPatch = new JSONObject().put("advancedConfigurations", new JSONObject()
                .put("skipLoginConsent", true).put("skipLogoutConsent", true)).toString();
        HttpResponse patched = SimpleHTTPClient.getInstance().doPatch(appsUrl + "/" + appId, headers,
                consentPatch, JSON_CONTENT_TYPE);
        Assert.assertTrue(patched != null && patched.getResponseCode() == 200,
                "Skipping consent on '" + appName + "' in tenant '" + tenantDomain + "' failed: got="
                        + (patched == null ? "null" : patched.getResponseCode() + "/" + patched.getData()));
        HttpResponse creds = SimpleHTTPClient.getInstance().doGet(
                appsUrl + "/" + appId + "/inbound-protocols/oidc", headers);
        Assert.assertTrue(creds != null && creds.getResponseCode() == 200 && creds.getData() != null
                        && !creds.getData().isBlank(),
                "Could not read the OIDC credentials of '" + appName + "' in tenant '" + tenantDomain + "'.");
        assertApplicationConfigured(appsUrl, appId, headers, callback);

        JSONObject body = new JSONObject(creds.getData());
        return new String[] {body.getString("clientId"), body.getString("clientSecret")};
    }

    /**
     * Creates the federated user in the Identity Server TENANT and places it in {@code groups} — the groups the
     * chain maps onto API Manager roles, so this is what decides whether the user can do role-gated work after
     * logging in.
     */
    public static void createTenantUser(String tenantDomain, String userName, String password, String email,
            List<String> groups, String admin, String adminPassword) throws IOException {
        String base = isBase() + tenantPrefix(tenantDomain);
        Map<String, String> headers = tenantAdminHeaders(tenantDomain, admin, adminPassword);

        String userPayload = new JSONObject()
                .put("schemas", new JSONArray().put("urn:ietf:params:scim:schemas:core:2.0:User"))
                .put("userName", userName)
                .put("password", password)
                .put("name", new JSONObject().put("givenName", userName).put("familyName", "SSO"))
                .put("emails", new JSONArray().put(new JSONObject().put("primary", true).put("value", email)))
                .toString();
        HttpResponse userResp = SimpleHTTPClient.getInstance().doPost(base + "scim2/Users", headers, userPayload,
                SCIM_CONTENT_TYPE);
        Assert.assertTrue(userResp != null && userResp.getResponseCode() == 201 && userResp.getData() != null
                        && !userResp.getData().isBlank(),
                "Creating user '" + userName + "' in identity server tenant '" + tenantDomain + "' failed: got="
                        + (userResp == null ? "null" : userResp.getResponseCode() + "/" + userResp.getData()));
        String userId = new JSONObject(userResp.getData()).getString("id");

        for (String group : groups) {
            addUserToGroup(base, headers, group, userId, userName);
        }
    }

    /**
     * Resolves the identity server's id for {@link #IS_APP_TEMPLATE_NAME} from the extension management API,
     * which serves the console's application templates.
     *
     * <p>Looked up by name rather than hardcoded: a template id is opaque, so a wrong constant creates a
     * different KIND of application — different default grant types and client behaviour — with nothing
     * downstream reporting it. Resolving by name also asserts the template exists on this server.
     */
    private static String resolveTemplateId(String tenantDomain, Map<String, String> headers) throws IOException {
        String url = isBase() + tenantPrefix(tenantDomain) + "api/server/v1/extensions/applications";
        HttpResponse resp = SimpleHTTPClient.getInstance().doGet(url, headers);
        Assert.assertTrue(resp != null && resp.getResponseCode() == 200 && resp.getData() != null,
                "Could not list application templates at " + url + ": got="
                        + (resp == null ? "null" : resp.getResponseCode() + "/" + resp.getData()));
        JSONArray templates = templatesOf(resp.getData());
        StringBuilder seen = new StringBuilder();
        for (int i = 0; i < templates.length(); i++) {
            JSONObject t = templates.getJSONObject(i);
            String name = t.optString("name", "");
            seen.append(name).append(" | ");
            if (IS_APP_TEMPLATE_NAME.equalsIgnoreCase(name)) {
                String id = t.optString("id", null);
                Assert.assertNotNull(id, "Template '" + name + "' has no id: " + t);
                log.info("[MT-IS] template '" + IS_APP_TEMPLATE_NAME + "' resolved to id=" + id);
                return id;
            }
        }
        throw new AssertionError("The identity server offers no '" + IS_APP_TEMPLATE_NAME + "' application "
                + "template, which the multi-tenant guide requires. Available: [" + seen + "]");
    }

    /** The template list, tolerating either a bare array or an object wrapping one. */
    private static JSONArray templatesOf(String body) {
        String trimmed = body.trim();
        if (trimmed.startsWith("[")) {
            return new JSONArray(trimmed);
        }
        JSONObject obj = new JSONObject(trimmed);
        for (String key : new String[] {"templates", "applicationTemplates", "list"}) {
            if (obj.has(key)) {
                return obj.getJSONArray(key);
            }
        }
        throw new AssertionError("Unrecognised application-templates response shape: " + body);
    }

    /**
     * Asserts the created application actually carries the callback and claims that were asked for.
     *
     * <p>A 201 means the request was accepted, not that every field was retained — unrecognised configuration is
     * dropped without an error. A dropped callback or claim surfaces only much later, as a login that fails or
     * resolves the wrong identity, so the stored form is read back and checked here.
     */
    private static void assertApplicationConfigured(String appsUrl, String appId, Map<String, String> headers,
            String expectedCallback) throws IOException {
        HttpResponse oidcResp = SimpleHTTPClient.getInstance().doGet(
                appsUrl + "/" + appId + "/inbound-protocols/oidc", headers);
        Assert.assertTrue(oidcResp != null && oidcResp.getResponseCode() == 200 && oidcResp.getData() != null
                        && !oidcResp.getData().isBlank(),
                "Could not read back the application's OIDC configuration.");
        String callbacks = new JSONObject(oidcResp.getData()).optJSONArray("callbackURLs") == null ? ""
                : new JSONObject(oidcResp.getData()).getJSONArray("callbackURLs").toString();
        Assert.assertTrue(callbacks.contains(expectedCallback),
                "The application's registered callback is " + callbacks + ", which does not contain the "
                        + "tenant-qualified " + expectedCallback + ". A mismatched redirect_uri would then be "
                        + "rejected at login, or worse, silently accepted against the wrong tenant.");

        HttpResponse appResp = SimpleHTTPClient.getInstance().doGet(appsUrl + "/" + appId, headers);
        String stored = appResp == null ? "" : String.valueOf(appResp.getData());
        log.info("[MT-IS] application claims stored: groups=" + stored.contains("claims/groups")
                + " username=" + stored.contains("claims/username")
                + " subjectClaim=" + (stored.contains("claims/username") ? "username" : "?"));
        Assert.assertTrue(stored.contains("claims/groups") && stored.contains("claims/username"),
                "The application does not release both the groups and username claims. The chain requests both, "
                        + "so a missing one leaves the federated user without roles or without an identity. "
                        + "Stored configuration: " + stored);
    }

    /**
     * Creates the group with the user as its member, or PATCH-adds the member when the group already exists.
     *
     * <p>The member entry carries {@code display} alongside {@code value}; the Identity Server rejects the PATCH
     * with 400 without it.
     */
    private static void addUserToGroup(String base, Map<String, String> headers, String group, String userId,
            String userName) throws IOException {
        JSONObject member = new JSONObject().put("value", userId).put("display", userName);
        String createPayload = new JSONObject()
                .put("schemas", new JSONArray().put("urn:ietf:params:scim:schemas:core:2.0:Group"))
                .put("displayName", group)
                .put("members", new JSONArray().put(member))
                .toString();
        HttpResponse created = SimpleHTTPClient.getInstance().doPost(base + "scim2/Groups", headers, createPayload,
                SCIM_CONTENT_TYPE);
        if (created != null && created.getResponseCode() == 201) {
            return;
        }
        Assert.assertTrue(created != null && created.getResponseCode() == 409,
                "Creating group '" + group + "' failed for a reason other than it already existing: got="
                        + (created == null ? "null" : created.getResponseCode() + "/" + created.getData()));

        String groupId = findGroupId(base, headers, group);
        Assert.assertNotNull(groupId, "Group '" + group + "' reported as existing but could not be found.");
        String patch = new JSONObject()
                .put("schemas", new JSONArray().put("urn:ietf:params:scim:api:messages:2.0:PatchOp"))
                .put("Operations", new JSONArray().put(new JSONObject()
                        .put("op", "add")
                        .put("value", new JSONObject().put("members", new JSONArray().put(member)))))
                .toString();
        HttpResponse patched = SimpleHTTPClient.getInstance().doPatch(base + "scim2/Groups/" + groupId, headers,
                patch, SCIM_CONTENT_TYPE);
        Assert.assertTrue(patched != null && patched.getResponseCode() == 200,
                "Adding '" + userName + "' to existing group '" + group + "' failed: got="
                        + (patched == null ? "null" : patched.getResponseCode() + "/" + patched.getData()));
    }

    private static String findGroupId(String base, Map<String, String> headers, String group) throws IOException {
        HttpResponse resp = SimpleHTTPClient.getInstance().doGet(
                base + "scim2/Groups?filter=displayName+eq+" + Utils.urlEncode(group), headers);
        if (resp == null || resp.getData() == null || resp.getData().isBlank()) {
            return null;
        }
        JSONObject body = new JSONObject(resp.getData());
        JSONArray resources = body.optJSONArray("Resources");
        return resources == null || resources.isEmpty() ? null : resources.getJSONObject(0).optString("id", null);
    }

    private static String locationId(HttpResponse resp) {
        if (resp == null || resp.getHeaders() == null) {
            return null;
        }
        String location = resp.getHeaders().get("Location");
        if (location == null) {
            location = resp.getHeaders().get("location");
        }
        return location == null ? null : location.substring(location.lastIndexOf('/') + 1);
    }

    // ---------------------------------------------------------------------------------------------------------
    // 3) API Manager side: the tenant's identity provider and common service provider
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Registers, in the API Manager TENANT, the OIDC identity provider that federates to the Identity Server
     * application in the same tenant. Its endpoints are the tenant-qualified ones on the Identity Server.
     */
    public static void registerTenantOidcIdp(String tenantDomain, String idpName, String clientId,
            String clientSecret, String admin, String password) throws IOException {
        String isTenant = isServerBase() + tenantPrefix(tenantDomain);
        String federated = "<m:federatedAuthenticatorConfigs><m:displayName>openidconnect</m:displayName>"
                + "<m:enabled>true</m:enabled><m:name>OpenIDConnectAuthenticator</m:name>"
                + property("ClientId", clientId)
                + property("ClientSecret", clientSecret)
                + property("OAuth2AuthzEPUrl", isTenant + "oauth2/authorize")
                + property("OAuth2TokenEPUrl", isTenant + "oauth2/token")
                + property("UserInfoUrl", isTenant + "oauth2/userinfo")
                + property("OIDCLogoutEPUrl", isTenant + "oidc/logout")
                // This is the redirect_uri sent to the identity server, so it must be byte-identical to the
                // callback registered on the tenant application there — the tenant's own commonauth, not the
                // super tenant's, because this leg of the chain runs in the tenant's context.
                + property("callbackUrl", APIM_INTERNAL + tenantPrefix(tenantDomain) + "commonauth")
                // 'groups' is required alongside 'openid': the identity server releases group memberships
                // only when that scope is requested, and without them no role mapping can happen.
                + property("Scopes", "openid groups")
                + property("IsUserIdInClaims", "false")
                + property("IsBasicAuthEnabled", "false")
                + "</m:federatedAuthenticatorConfigs>";
        addIdp(tenantDomain, idpName, "OpenIDConnectAuthenticator", federated + tenantIdpClaimsAndRoles(),
                admin + "@" + tenantDomain, password, true);
    }

    /**
     * The provisioning and role half of an identity provider, shared by the tenant and broker providers.
     *
     * <p>Three things have to line up before a federated user can do anything. Just-in-time provisioning creates
     * the local account at all. The claim mapping carries the identity server's {@code groups} claim into the
     * local role claim, and {@code roleClaimURI} names which incoming claim holds the roles — without both, the
     * groups arrive and are discarded. Only then do the role mappings translate those groups into API Manager
     * roles; without them the user logs in holding nothing and every role-gated action is refused.
     */
    /**
     * The super tenant identity provider's claim, provisioning and role configuration (guide step 3).
     *
     * <p>Written separately from the tenant provider's: the guide configures each one in its own portal, so they
     * are free to differ.
     */
    private static String superTenantIdpClaimsAndRoles() {
        // A mapping may only reference a claim that is declared as an identity-provider claim; one naming an
        // undeclared claim is discarded.
        return "<m:claimConfig>"
                + "<m:localClaimDialect>false</m:localClaimDialect>"
                + "<m:roleClaimURI>" + GROUPS_CLAIM_URI + "</m:roleClaimURI>"
                + "<m:idpClaims><m:claimId>0</m:claimId><m:claimUri>" + GROUPS_CLAIM_URI
                + "</m:claimUri></m:idpClaims>"
                + "<m:claimMappings>"
                + "<m:localClaim><m:claimUri>http://wso2.org/claims/role</m:claimUri></m:localClaim>"
                + "<m:remoteClaim><m:claimId>0</m:claimId><m:claimUri>" + GROUPS_CLAIM_URI
                + "</m:claimUri></m:remoteClaim>"
                + "<m:requested>true</m:requested>"
                + "</m:claimMappings>"
                + "</m:claimConfig>"
                // Always provision to the primary user store, silently: no prompt for username, password or
                // consent.
                + "<m:justInTimeProvisioningConfig>"
                + "<m:provisioningEnabled>true</m:provisioningEnabled>"
                + "<m:provisioningUserStore>PRIMARY</m:provisioningUserStore>"
                + "<m:modifyUserNameAllowed>false</m:modifyUserNameAllowed>"
                + "<m:passwordProvisioningEnabled>false</m:passwordProvisioningEnabled>"
                + "<m:promptConsent>false</m:promptConsent>"
                + "</m:justInTimeProvisioningConfig>"
                + "<m:permissionAndRoleConfig>"
                + roleMapping("creator", "Internal/creator")
                + roleMapping("publisher", "Internal/publisher")
                + roleMapping("subscriber", "Internal/subscriber")
                + "</m:permissionAndRoleConfig>";
    }

    /**
     * The tenant identity provider's claim, provisioning and role configuration (guide step 5, which repeats
     * step 3 inside the tenant).
     */
    private static String tenantIdpClaimsAndRoles() {
        // A mapping may only reference a claim that is declared as an identity-provider claim; one naming an
        // undeclared claim is discarded.
        return "<m:claimConfig>"
                + "<m:localClaimDialect>false</m:localClaimDialect>"
                + "<m:roleClaimURI>" + GROUPS_CLAIM_URI + "</m:roleClaimURI>"
                + "<m:idpClaims><m:claimId>0</m:claimId><m:claimUri>" + GROUPS_CLAIM_URI
                + "</m:claimUri></m:idpClaims>"
                + "<m:claimMappings>"
                + "<m:localClaim><m:claimUri>http://wso2.org/claims/role</m:claimUri></m:localClaim>"
                + "<m:remoteClaim><m:claimId>0</m:claimId><m:claimUri>" + GROUPS_CLAIM_URI
                + "</m:claimUri></m:remoteClaim>"
                + "<m:requested>true</m:requested>"
                + "</m:claimMappings>"
                + "</m:claimConfig>"
                // Always provision to the primary user store, silently: no prompt for username, password or
                // consent.
                + "<m:justInTimeProvisioningConfig>"
                + "<m:provisioningEnabled>true</m:provisioningEnabled>"
                + "<m:provisioningUserStore>PRIMARY</m:provisioningUserStore>"
                + "<m:modifyUserNameAllowed>false</m:modifyUserNameAllowed>"
                + "<m:passwordProvisioningEnabled>false</m:passwordProvisioningEnabled>"
                + "<m:promptConsent>false</m:promptConsent>"
                + "</m:justInTimeProvisioningConfig>"
                + "<m:permissionAndRoleConfig>"
                + roleMapping("creator", "Internal/creator")
                + roleMapping("publisher", "Internal/publisher")
                + roleMapping("subscriber", "Internal/subscriber")
                // The tenant user must receive the API Manager tenant-admin role as well as console roles. The
                // external IS releases this group and the tenant IdP maps it to the bare tenant role "admin".
                + roleMapping("admin", "admin")
                + "</m:permissionAndRoleConfig>";
    }

    /**
     * Whether a user exists in a tenant.
     *
     * <p>This is the only sound existence check: {@code getRoleListOfUser} answers {@code Internal/everyone} for
     * ANY username, present or not, so a non-empty role list proves nothing.
     */
    public static boolean userExists(String tenantDomain, String userName) throws IOException {
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ser=\"http://service.ws.um.carbon.wso2.org\"><soapenv:Body>"
                + "<ser:isExistingUser><ser:userName>" + Utils.escapeXml(qualify(tenantDomain, userName))
                + "</ser:userName></ser:isExistingUser></soapenv:Body></soapenv:Envelope>";
        HttpResponse resp = SimpleHTTPClient.getInstance().sendSoapRequest(
                apimBase() + "services/RemoteUserStoreManagerService", payload, "urn:isExistingUser",
                tenantAdminName(tenantDomain), tenantAdminPassword(tenantDomain));
        return resp != null && resp.getData() != null && resp.getData().contains(">true<");
    }

    private static String qualify(String tenantDomain, String userName) {
        return Constants.SUPER_TENANT_DOMAIN.equals(tenantDomain) ? userName : userName + "@" + tenantDomain;
    }

    private static String tenantAdminName(String tenantDomain) {
        return Constants.SUPER_TENANT_DOMAIN.equals(tenantDomain)
                ? Constants.SUPER_TENANT_ADMIN_USERNAME : "admin@" + tenantDomain;
    }

    private static String tenantAdminPassword(String tenantDomain) {
        return Constants.SUPER_TENANT_DOMAIN.equals(tenantDomain)
                ? Constants.SUPER_TENANT_ADMIN_PASSWORD : "Admin@12345";
    }

    /** The roles a user holds in a tenant, read from API Manager's user-store admin service. */
    public static String rolesOfUser(String tenantDomain, String userName) throws IOException {
        String qualified = Constants.SUPER_TENANT_DOMAIN.equals(tenantDomain)
                ? userName : userName + "@" + tenantDomain;
        String admin = Constants.SUPER_TENANT_DOMAIN.equals(tenantDomain)
                ? Constants.SUPER_TENANT_ADMIN_USERNAME : "admin@" + tenantDomain;
        String adminPass = Constants.SUPER_TENANT_DOMAIN.equals(tenantDomain)
                ? Constants.SUPER_TENANT_ADMIN_PASSWORD : "Admin@12345";
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ser=\"http://service.ws.um.carbon.wso2.org\"><soapenv:Body>"
                + "<ser:getRoleListOfUser><ser:userName>" + Utils.escapeXml(qualified) + "</ser:userName>"
                + "</ser:getRoleListOfUser></soapenv:Body></soapenv:Envelope>";
        HttpResponse resp = SimpleHTTPClient.getInstance().sendSoapRequest(
                apimBase() + "services/RemoteUserStoreManagerService", payload, "urn:getRoleListOfUser",
                admin, adminPass);
        return resp == null ? "null" : String.valueOf(resp.getData()).replaceAll("\\s+", " ");
    }

    /** Maps an identity server group onto an API Manager role. */
    private static String roleMapping(String remoteGroup, String localRole) {
        return "<m:roleMappings><m:localRole><m:localRoleName>" + localRole + "</m:localRoleName></m:localRole>"
                + "<m:remoteRole>" + remoteGroup + "</m:remoteRole></m:roleMappings>";
    }

    /**
     * Registers the SUPER TENANT's broker identity provider. Its {@code multiTenantAuthenticator} is what renders
     * the tenant-selection page and then hands the flow to the chosen tenant's common service provider.
     */
    public static void registerBrokerIdp(String idpName, String commonSpName, String clientId,
            String clientSecret) throws IOException {
        String federated = "<m:federatedAuthenticatorConfigs>"
                + "<m:displayName>Multi Tenant Authenticator</m:displayName>"
                + "<m:enabled>true</m:enabled><m:name>multiTenantAuthenticator</m:name>"
                + property("ClientId", clientId)
                + property("ClientSecret", clientSecret)
                + property("OAuth2AuthzEPUrl", isServerBase() + "oauth2/authorize")
                + property("OAuth2TokenEPUrl", isServerBase() + "oauth2/token")
                + property("UserInfoUrl", isServerBase() + "oauth2/userinfo")
                + property("OIDCLogoutEPUrl", isServerBase() + "oidc/logout")
                + property("callbackUrl", APIM_INTERNAL + "commonauth")
                // 'groups' is required alongside 'openid': the identity server releases group memberships
                // only when that scope is requested, and without them no role mapping can happen.
                + property("Scopes", "openid groups")
                + property("IsUserIdInClaims", "false")
                + property("CommonSPName", commonSpName)
                + property("TenantSelectionPageUrl", APIM_INTERNAL + "select-tenant/")
                + property("IsBasicAuthEnabled", "false")
                + "</m:federatedAuthenticatorConfigs>";
        addIdp(Constants.SUPER_TENANT_DOMAIN, idpName, "multiTenantAuthenticator",
                federated + superTenantIdpClaimsAndRoles(),
                Constants.SUPER_TENANT_ADMIN_USERNAME, Constants.SUPER_TENANT_ADMIN_PASSWORD, false);
    }

    /**
     * Creates the tenant's {@code commonsp}: its own OAuth2 client, plus an authentication step federating to the
     * tenant's identity provider. The broker hands the flow here, so this service provider is the tenant's entry
     * point into the chain.
     */
    public static void setupCommonServiceProvider(String tenantDomain, String spName, String idpName,
            String authenticatorName, String admin, String password) throws IOException {
        String qualifiedAdmin = admin + "@" + tenantDomain;

        String registerOauth = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:xsd=\"" + NS_APP_OPS + "\" xmlns:dto=\"http://dto.oauth.identity.carbon.wso2.org/xsd\">"
                + "<soapenv:Body><xsd:registerOAuthApplicationData><xsd:application>"
                + "<dto:applicationName>" + Utils.escapeXml(spName) + "</dto:applicationName>"
                + "<dto:callbackUrl>" + Utils.escapeXml(APIM_INTERNAL + "commonauth") + "</dto:callbackUrl>"
                + "<dto:grantTypes>" + COMMON_SP_GRANT_TYPES + "</dto:grantTypes>"
                + "<dto:OAuthVersion>OAuth-2.0</dto:OAuthVersion>"
                + "<dto:pkceMandatory>false</dto:pkceMandatory>"
                + "<dto:pkceSupportPlain>true</dto:pkceSupportPlain>"
                + "<dto:renewRefreshTokenEnabled>true</dto:renewRefreshTokenEnabled>"
                + "</xsd:application></xsd:registerOAuthApplicationData></soapenv:Body></soapenv:Envelope>";
        soap(oauthService(tenantDomain), registerOauth, "urn:registerOAuthApplicationData", qualifiedAdmin,
                password, "registering the OAuth client for '" + spName + "'");

        String readOauth = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:xsd=\"" + NS_APP_OPS + "\"><soapenv:Body><xsd:getOAuthApplicationDataByAppName>"
                + "<xsd:appName>" + Utils.escapeXml(spName) + "</xsd:appName>"
                + "</xsd:getOAuthApplicationDataByAppName></soapenv:Body></soapenv:Envelope>";
        HttpResponse oauthResp = soap(oauthService(tenantDomain), readOauth,
                "urn:getOAuthApplicationDataByAppName", qualifiedAdmin, password,
                "reading the OAuth client of '" + spName + "'");
        String consumerKey = firstMatch(CONSUMER_KEY, oauthResp.getData());
        Assert.assertNotNull(consumerKey, "No consumer key for '" + spName + "': " + oauthResp.getData());

        String create = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:axis2=\"" + NS_APP_OPS + "\"><soapenv:Body><axis2:createApplication>"
                + "<axis2:serviceProvider xmlns:m=\"" + NS_MODEL + "\">"
                + "<m:applicationName>" + Utils.escapeXml(spName) + "</m:applicationName>"
                + "</axis2:serviceProvider></axis2:createApplication></soapenv:Body></soapenv:Envelope>";
        soap(appService(tenantDomain), create, "urn:createApplication", qualifiedAdmin, password,
                "creating service provider '" + spName + "'");

        String appId = applicationId(tenantDomain, spName, qualifiedAdmin, password);
        Assert.assertNotNull(appId, "Service provider '" + spName + "' was not found after creation.");

        String update = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:axis2=\"" + NS_APP_OPS + "\"><soapenv:Body><axis2:updateApplication>"
                + "<axis2:serviceProvider xmlns:m=\"" + NS_MODEL + "\">"
                + "<m:applicationID>" + Utils.escapeXml(appId) + "</m:applicationID>"
                + "<m:applicationName>" + Utils.escapeXml(spName) + "</m:applicationName>"
                + "<m:inboundAuthenticationConfig><m:inboundAuthenticationRequestConfigs>"
                + "<m:inboundAuthKey>" + Utils.escapeXml(consumerKey) + "</m:inboundAuthKey>"
                + "<m:inboundAuthType>oauth2</m:inboundAuthType>"
                + "</m:inboundAuthenticationRequestConfigs></m:inboundAuthenticationConfig>"
                + federatedOutbound(idpName, authenticatorName)
                + "<m:subjectClaimUri>http://wso2.org/claims/username</m:subjectClaimUri>"
                + "<m:skipConsent>true</m:skipConsent><m:skipLogoutConsent>true</m:skipLogoutConsent>"
                + "<m:useTenantDomainInLocalSubjectIdentifier>true"
                + "</m:useTenantDomainInLocalSubjectIdentifier>"
                + "<m:useUserstoreDomainInLocalSubjectIdentifier>false"
                + "</m:useUserstoreDomainInLocalSubjectIdentifier>"
                + "<m:useUserstoreDomainInRoles>true</m:useUserstoreDomainInRoles>"
                + "<m:alwaysSendBackAuthenticatedListOfIdPs>false"
                + "</m:alwaysSendBackAuthenticatedListOfIdPs>"
                + "<m:enableAuthorization>false</m:enableAuthorization>"
                + "</m:localAndOutBoundAuthenticationConfig>"
                + commonSpClaimConfig()
                + "</axis2:serviceProvider></axis2:updateApplication></soapenv:Body></soapenv:Envelope>";
        soap(appService(tenantDomain), update, "urn:updateApplication", qualifiedAdmin, password,
                "wiring service provider '" + spName + "' to identity provider '" + idpName + "'");
    }

    /**
     * Points a console's service provider at the broker identity provider, so opening that console starts the
     * multi-tenant chain instead of showing the local login.
     */
    public static void wireConsoleToBroker(String consoleContext, String spName, String brokerIdpName)
            throws IOException {
        // The console registers its service provider lazily, the first time its login endpoint is hit. Hitting
        // that endpoint is the re-trigger; the provider appearing is the readiness signal.
        try {
            HealGate.awaitOrHeal("console service provider '" + spName + "'",
                    () -> applicationId(Constants.SUPER_TENANT_DOMAIN, spName,
                            Constants.SUPER_TENANT_ADMIN_USERNAME, Constants.SUPER_TENANT_ADMIN_PASSWORD) != null
                            ? new HealGate.Ready()
                            : new HealGate.NotReady("not registered yet"),
                    attempt -> {
                        triggerConsoleRegistration(consoleContext);
                        return new HealGate.NotReady("login endpoint hit, awaiting registration");
                    },
                    3);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted awaiting the '" + consoleContext
                    + "' console service provider.", e);
        }
        String appId = applicationId(Constants.SUPER_TENANT_DOMAIN, spName,
                Constants.SUPER_TENANT_ADMIN_USERNAME, Constants.SUPER_TENANT_ADMIN_PASSWORD);
        Assert.assertNotNull(appId, "The '" + consoleContext + "' console service provider '" + spName
                + "' does not exist, so it cannot be wired to the broker identity provider.");
        String inboundKey = inboundAuthKey(Constants.SUPER_TENANT_DOMAIN, spName);

        String update = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:axis2=\"" + NS_APP_OPS + "\"><soapenv:Body><axis2:updateApplication>"
                + "<axis2:serviceProvider xmlns:m=\"" + NS_MODEL + "\">"
                + "<m:applicationID>" + Utils.escapeXml(appId) + "</m:applicationID>"
                + "<m:applicationName>" + Utils.escapeXml(spName) + "</m:applicationName>"
                + (inboundKey == null ? "" : "<m:inboundAuthenticationConfig>"
                        + "<m:inboundAuthenticationRequestConfigs>"
                        + "<m:inboundAuthKey>" + Utils.escapeXml(inboundKey) + "</m:inboundAuthKey>"
                        + "<m:inboundAuthType>oauth2</m:inboundAuthType>"
                        + "</m:inboundAuthenticationRequestConfigs></m:inboundAuthenticationConfig>")
                + multiOptionOutbound(brokerIdpName, "multiTenantAuthenticator")
                + "<m:useTenantDomainInLocalSubjectIdentifier>false"
                + "</m:useTenantDomainInLocalSubjectIdentifier>"
                + "<m:useUserstoreDomainInLocalSubjectIdentifier>false"
                + "</m:useUserstoreDomainInLocalSubjectIdentifier>"
                + "<m:useUserstoreDomainInRoles>true</m:useUserstoreDomainInRoles>"
                + "<m:skipConsent>true</m:skipConsent><m:skipLogoutConsent>true</m:skipLogoutConsent>"
                + "</m:localAndOutBoundAuthenticationConfig>"
                // Assert identity using the mapped local subject identifier: a claimConfig field, which is what
                // carries the federated user onto the locally provisioned account the role mapping applies to.
                + "<m:claimConfig>"
                + "<m:alwaysSendMappedLocalSubjectId>true</m:alwaysSendMappedLocalSubjectId>"
                + "</m:claimConfig>"
                + "<m:saasApp>true</m:saasApp>"
                + "</axis2:serviceProvider></axis2:updateApplication></soapenv:Body></soapenv:Envelope>";
        soap(appService(Constants.SUPER_TENANT_DOMAIN), update, "urn:updateApplication",
                Constants.SUPER_TENANT_ADMIN_USERNAME, Constants.SUPER_TENANT_ADMIN_PASSWORD,
                "wiring the '" + consoleContext + "' console to broker identity provider '" + brokerIdpName + "'");
        assertConsoleWired(consoleContext, spName, brokerIdpName);
    }

    /**
     * Asserts that a console's lazily-created service provider is present before any authentication-step mutation.
     * This is especially important for DevPortal, whose SPA login flow does not use the generic server-side login
     * trigger used by Publisher and Admin.
     */
    public static void assertConsoleServiceProviderExists(String consoleContext, String spName) throws IOException {
        String appId = applicationId(Constants.SUPER_TENANT_DOMAIN, spName,
                Constants.SUPER_TENANT_ADMIN_USERNAME, Constants.SUPER_TENANT_ADMIN_PASSWORD);
        Assert.assertNotNull(appId, "The '" + consoleContext + "' console service provider '" + spName
                + "' was not lazily created after its explicit login. It cannot be wired to the broker.");
    }

    /** Requests the console's login endpoint, which is what makes it register its service provider. */
    private static void triggerConsoleRegistration(String consoleContext) {
        try {
            SimpleHTTPClient.getInstance().doGet(apimBase() + consoleContext + "/services/auth/login",
                    new HashMap<>());
        } catch (IOException stillWarmingUp) {
            // The gate's probe decides readiness; a refused request here is just an attempt that achieved
            // nothing.
        }
    }

    /**
     * Touches every console's unauthenticated login endpoint so APIM performs its lazy resident service-provider
     * registration before the setup starts wiring those providers. This deliberately does not authenticate a user.
     */
    public static void initializeConsoleServiceProviders() {
        for (String console : new String[]{"publisher", "admin", "devportal"}) {
            triggerConsoleRegistration(console);
        }
    }

    /**
     * Asserts the console's service provider really came back federated to the broker.
     *
     * <p>{@code updateApplication} reports success for a service provider it stored only in part: elements it does
     * not recognise are dropped silently. If the outbound configuration is among them the console keeps showing
     * its local login, and the multi-tenant journey then fails far from its cause.
     *
     * <p>The subject flags are logged rather than asserted because which of them a service provider retains is
     * what this read-back exists to establish.
     */
    private static void assertConsoleWired(String consoleContext, String spName, String brokerIdpName)
            throws IOException {
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ns1=\"" + NS_APP_OPS + "\"><soapenv:Body><ns1:getApplication>"
                + "<ns1:applicationName>" + Utils.escapeXml(spName) + "</ns1:applicationName>"
                + "</ns1:getApplication></soapenv:Body></soapenv:Envelope>";
        HttpResponse resp = SimpleHTTPClient.getInstance().sendSoapRequest(
                appService(Constants.SUPER_TENANT_DOMAIN), payload, "urn:getApplication",
                Constants.SUPER_TENANT_ADMIN_USERNAME, Constants.SUPER_TENANT_ADMIN_PASSWORD);
        String stored = resp == null ? "" : String.valueOf(resp.getData());

        log.info("[MT-SP] " + consoleContext + " subject flags stored:"
                + " alwaysSendMappedLocalSubjectId="
                + firstMatch(Pattern.compile("alwaysSendMappedLocalSubjectId>([^<]+)<"), stored)
                + " useUserstoreDomainInRoles="
                + firstMatch(Pattern.compile("useUserstoreDomainInRoles>([^<]+)<"), stored)
                + " useTenantDomainInLocalSubjectIdentifier="
                + firstMatch(Pattern.compile("useTenantDomainInLocalSubjectIdentifier>([^<]+)<"), stored)
                + " skipConsent=" + firstMatch(Pattern.compile("skipConsent>([^<]+)<"), stored));
        Assert.assertEquals(firstMatch(Pattern.compile("alwaysSendMappedLocalSubjectId>([^<]+)<"), stored), "true",
                "The '" + consoleContext + "' console service provider does not assert identity using the mapped "
                        + "local subject identifier, which the multi-tenant guide requires for role mapping to "
                        + "work. Stored configuration: " + stored);

        Assert.assertTrue(stored.contains(brokerIdpName),
                "The '" + consoleContext + "' console service provider is not federated to broker identity "
                        + "provider '" + brokerIdpName + "', so opening the console would show the local login "
                        + "instead of the tenant-selection page. Stored configuration: " + stored);
        Assert.assertTrue(stored.contains("multiTenantAuthenticator"),
                "The '" + consoleContext + "' console service provider names no multiTenantAuthenticator step. "
                        + "Without it the broker cannot render tenant selection. Stored configuration: " + stored);
        // Matched on the local name because the response's namespace prefix is server-assigned.
        Assert.assertTrue(stored.matches("(?s).*saasApp>true<.*"),
                "The '" + consoleContext + "' console service provider is no longer a SaaS application. A "
                        + "non-SaaS super-tenant service provider refuses logins from other tenants outright. "
                        + "Stored configuration: " + stored);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Builders and small readers
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Registers an identity provider.
     *
     * <p>{@code addIdP} answers 500 ({@code No Identity Provider claim URIs defined for tenant}) while still
     * creating the provider, so the outcome is confirmed with {@code getIdPByName} rather than read from the
     * status code.
     */
    private static void addIdp(String tenantDomain, String idpName, String authenticatorName, String federated,
            String admin, String password, boolean requireAdminRoleMapping) throws IOException {
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ns=\"" + NS_IDP_OPS + "\" xmlns:m=\"" + NS_MODEL + "\"><soapenv:Body>"
                + "<ns:addIdP><ns:identityProvider>"
                + "<m:identityProviderName>" + Utils.escapeXml(idpName) + "</m:identityProviderName>"
                + "<m:enable>true</m:enable>"
                + "<m:defaultAuthenticatorConfig><m:name>" + authenticatorName + "</m:name>"
                + "<m:enabled>true</m:enabled></m:defaultAuthenticatorConfig>"
                + federated
                + "</ns:identityProvider></ns:addIdP></soapenv:Body></soapenv:Envelope>";
        try {
            SimpleHTTPClient.getInstance().sendSoapRequest(idpService(tenantDomain), payload, "urn:addIdP",
                    admin, password);
        } catch (IOException e) {
            // The creation may still have succeeded; the existence check below is what decides.
        }
        String verify = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ns=\"" + NS_IDP_OPS + "\"><soapenv:Body><ns:getIdPByName>"
                + "<ns:idPName>" + Utils.escapeXml(idpName) + "</ns:idPName>"
                + "</ns:getIdPByName></soapenv:Body></soapenv:Envelope>";
        HttpResponse resp = SimpleHTTPClient.getInstance().sendSoapRequest(idpService(tenantDomain), verify,
                "urn:getIdPByName", admin, password);
        Assert.assertTrue(resp != null && resp.getData() != null
                        && resp.getData().contains("identityProviderName>" + idpName + "<"),
                "Identity provider '" + idpName + "' was not registered in tenant '" + tenantDomain
                        + "'. getIdPByName returned: " + (resp == null ? "null" : resp.getData()));

        // addIdP creates the provider but silently discards the CLAIM MAPPINGS, while keeping the role claim
        // URI, role mappings and provisioning config. Measured by reading the provider back. Losing them is
        // quiet and expensive: the identity server's groups then never reach the local role claim, so the user
        // authenticates, is provisioned with no roles, and every role-gated action is refused far from here.
        //
        // Re-applying them with updateIdP works because nothing references the provider yet — a service provider
        // wired to it makes updateIdP fail while it disables the federated authenticator.
        String update = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ns=\"" + NS_IDP_OPS + "\" xmlns:m=\"" + NS_MODEL + "\"><soapenv:Body>"
                + "<ns:updateIdP><ns:oldIdPName>" + Utils.escapeXml(idpName) + "</ns:oldIdPName>"
                + "<ns:identityProvider>"
                + "<m:identityProviderName>" + Utils.escapeXml(idpName) + "</m:identityProviderName>"
                + "<m:enable>true</m:enable>"
                + "<m:defaultAuthenticatorConfig><m:name>" + authenticatorName + "</m:name>"
                + "<m:enabled>true</m:enabled></m:defaultAuthenticatorConfig>"
                + federated
                + "</ns:identityProvider></ns:updateIdP></soapenv:Body></soapenv:Envelope>";
        try {
            SimpleHTTPClient.getInstance().sendSoapRequest(idpService(tenantDomain), update, "urn:updateIdP",
                    admin, password);
        } catch (IOException e) {
            // Same shape as addIdP: the call can report a failure it did not have. The read-back below decides.
        }

        HttpResponse after = SimpleHTTPClient.getInstance().sendSoapRequest(idpService(tenantDomain), verify,
                "urn:getIdPByName", admin, password);
        String stored = after == null ? "" : String.valueOf(after.getData());
        log.info("[MT-IDP] '" + idpName + "' in " + tenantDomain
                + " provisioningEnabled=" + firstMatch(Pattern.compile("provisioningEnabled>([^<]+)<"), stored)
                + " roleClaimURI=" + firstMatch(Pattern.compile("roleClaimURI>([^<]+)<"), stored)
                + " claimMappings=" + (stored.contains("claimMappings") ? "present" : "DROPPED")
                + " roleMappings=" + (stored.contains("roleMappings") ? "present" : "DROPPED")
                + " userStore=" + firstMatch(Pattern.compile("provisioningUserStore>([^<]+)<"), stored)
                + " passwordProvisioning="
                + firstMatch(Pattern.compile("passwordProvisioningEnabled>([^<]+)<"), stored)
                + " promptConsent=" + firstMatch(Pattern.compile("promptConsent>([^<]+)<"), stored));
        Assert.assertTrue(stored.contains("claimMappings"),
                "Identity provider '" + idpName + "' in tenant '" + tenantDomain + "' kept no claim mappings even "
                        + "after re-applying them, so the identity server's groups cannot reach the local role "
                        + "claim and the federated user would be provisioned with no roles.");
        if (requireAdminRoleMapping) {
            Assert.assertTrue(stored.matches("(?s).*localRoleName>admin<.*remoteRole>admin<.*"),
                    "Identity provider '" + idpName + "' in tenant '" + tenantDomain
                            + "' has no admin-group to admin-role mapping. The external tenant user could log in, "
                            + "but could not be authorized for tenant Admin operations.");
        }
    }

    private static String federatedOutbound(String idpName, String authenticatorName) {
        return "<m:localAndOutBoundAuthenticationConfig><m:authenticationSteps>"
                + "<m:federatedIdentityProviders>"
                + "<m:defaultAuthenticatorConfig><m:name>" + authenticatorName + "</m:name>"
                + "<m:enabled>true</m:enabled></m:defaultAuthenticatorConfig>"
                + "<m:federatedAuthenticatorConfigs><m:name>" + authenticatorName + "</m:name>"
                + "<m:enabled>true</m:enabled></m:federatedAuthenticatorConfigs>"
                + "<m:identityProviderName>" + Utils.escapeXml(idpName) + "</m:identityProviderName>"
                + "</m:federatedIdentityProviders><m:stepOrder>1</m:stepOrder></m:authenticationSteps>"
                + "<m:authenticationType>federated</m:authenticationType>";
    }

    /** Adds the local BasicAuthenticator beside the MT broker option on a console service provider. */
    private static String multiOptionOutbound(String idpName, String authenticatorName) {
        return "<m:localAndOutBoundAuthenticationConfig><m:authenticationSteps>"
                + "<m:federatedIdentityProviders>"
                + "<m:defaultAuthenticatorConfig><m:name>" + authenticatorName + "</m:name>"
                + "<m:enabled>true</m:enabled></m:defaultAuthenticatorConfig>"
                + "<m:federatedAuthenticatorConfigs><m:name>" + authenticatorName + "</m:name>"
                + "<m:enabled>true</m:enabled></m:federatedAuthenticatorConfigs>"
                + "<m:identityProviderName>" + Utils.escapeXml(idpName) + "</m:identityProviderName>"
                + "</m:federatedIdentityProviders>"
                + "<m:localAuthenticatorConfigs><m:displayName>basic</m:displayName>"
                + "<m:enabled>true</m:enabled><m:name>BasicAuthenticator</m:name>"
                + "</m:localAuthenticatorConfigs><m:stepOrder>1</m:stepOrder>"
                + "<m:subjectStep>true</m:subjectStep></m:authenticationSteps>"
                + "<m:authenticationType>flow</m:authenticationType>";
    }

    /**
     * The common service provider's claim configuration: the local dialect, the username and roles claims, and
     * the mapped local subject identifier the guide requires for role mapping.
     *
     * <p>A service provider carries ONE claimConfig, so both mappings sit inside it.
     */
    private static String commonSpClaimConfig() {
        return "<m:claimConfig>"
                + "<m:localClaimDialect>true</m:localClaimDialect>"
                + "<m:alwaysSendMappedLocalSubjectId>true</m:alwaysSendMappedLocalSubjectId>"
                + claimMapping("http://wso2.org/claims/username")
                + claimMapping("http://wso2.org/claims/roles")
                + "</m:claimConfig>";
    }

    /** One claim mapped onto itself, requested from the identity provider. */
    private static String claimMapping(String uri) {
        return "<m:claimMappings><m:localClaim><m:claimUri>" + uri + "</m:claimUri></m:localClaim>"
                + "<m:remoteClaim><m:claimUri>" + uri + "</m:claimUri></m:remoteClaim>"
                + "<m:requested>true</m:requested></m:claimMappings>";
    }

    private static String property(String name, String value) {
        return "<m:properties><m:name>" + name + "</m:name><m:value>" + Utils.escapeXml(value) + "</m:value>"
                + "</m:properties>";
    }

    private static String applicationId(String tenantDomain, String spName, String admin, String password)
            throws IOException {
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ns1=\"" + NS_APP_OPS + "\"><soapenv:Body><ns1:getApplication>"
                + "<ns1:applicationName>" + Utils.escapeXml(spName) + "</ns1:applicationName>"
                + "</ns1:getApplication></soapenv:Body></soapenv:Envelope>";
        HttpResponse resp = SimpleHTTPClient.getInstance().sendSoapRequest(appService(tenantDomain), payload,
                "urn:getApplication", admin, password);
        return firstMatch(APP_ID, resp == null ? null : resp.getData());
    }

    private static String inboundAuthKey(String tenantDomain, String spName) throws IOException {
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ns1=\"" + NS_APP_OPS + "\"><soapenv:Body><ns1:getApplication>"
                + "<ns1:applicationName>" + Utils.escapeXml(spName) + "</ns1:applicationName>"
                + "</ns1:getApplication></soapenv:Body></soapenv:Envelope>";
        HttpResponse resp = SimpleHTTPClient.getInstance().sendSoapRequest(appService(tenantDomain), payload,
                "urn:getApplication", Constants.SUPER_TENANT_ADMIN_USERNAME,
                Constants.SUPER_TENANT_ADMIN_PASSWORD);
        return firstMatch(Pattern.compile("inboundAuthKey>([^<]+)<"), resp == null ? null : resp.getData());
    }

    /** Sends a SOAP call and fails with what it was trying to do, including any fault string. */
    private static HttpResponse soap(String url, String payload, String action, String user, String password,
            String what) throws IOException {
        HttpResponse resp = SimpleHTTPClient.getInstance().sendSoapRequest(url, payload, action, user, password);
        String body = resp == null ? null : resp.getData();
        String fault = firstMatch(Pattern.compile("<faultstring>([^<]+)<"), body);
        Assert.assertTrue(resp != null && resp.getResponseCode() == 200 && fault == null,
                "Failed while " + what + ": got=" + (resp == null ? "null" : resp.getResponseCode())
                        + (fault == null ? "" : " fault=" + fault) + " body=" + body);
        return resp;
    }

    private static String firstMatch(Pattern pattern, String text) {
        if (text == null) {
            return null;
        }
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : null;
    }
}
