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

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.am.testcontainers.DynamicISContainer;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provisions the WSO2 IS 7.x side and the APIM side of the external-IdP console-SSO flow: an IS OIDC
 * application the APIM consoles federate to, a federated (JIT-provisioned) user on IS, the APIM-side OIDC
 * identity provider registered against that IS app, and the multi-option (local + federated) authentication
 * step wired onto the publisher/admin/devportal console service providers.
 *
 * <p>This is the SOAP-admin-service exception of CLAUDE.md §14: APIM exposes no REST API for IdP registration
 * or for editing a console service provider's authentication steps, so a helper util is acceptable — it runs
 * super-tenant admin/admin and is called from a {@code _setup_*} step, never a listener hook.
 *
 * <p>Container-URL split this class encodes (the manual recipe used fixed {@code localhost:9443/9444}; the
 * framework maps ports and uses a network alias):
 * <ul>
 *   <li>The IdP's <b>authorize</b> and <b>logout</b> endpoints go to the IS browser-facing (mapped-host) base
 *       — APIM redirects the <i>browser</i> there.</li>
 *   <li>The IdP's <b>token</b> and <b>userinfo</b> endpoints go to the IS server-facing ({@code wso2is:9443})
 *       base — APIM calls these server-to-server, and the id_token {@code iss} matches that host.</li>
 *   <li>IS REST/SCIM2 management calls go to the browser-facing base (the test reaches IS on the mapped port),
 *       exactly like {@code TokenExchangeProvisioner}'s {@code isBase()}.</li>
 * </ul>
 */
public final class SsoProvisioner {

    /** IS "Standard-Based Application" template used for a plain OIDC app (same template the TX provisioner uses). */
    private static final String IS_APP_TEMPLATE_ID = "b9c5e11e-fc78-484b-9bec-015d247561b8";

    /** SOAP namespaces: IdP-mgt operations, common-model DTOs, and app-mgt operations. */
    private static final String NS_IDP_OPS = "http://mgt.idp.carbon.wso2.org";
    private static final String NS_MODEL = "http://model.common.application.identity.carbon.wso2.org/xsd";
    private static final String NS_APP_OPS = "http://org.apache.axis2/xsd";

    private static final String SCIM_CONTENT_TYPE = "application/scim+json";

    /**
     * APIM's INTERNAL browser-facing base — the hostname APIM emits in its own redirects and {@code redirect_uri}
     * (from {@code APIUtil.getServerURL()}). The Playwright client reaches this through its CONNECT proxy, so the
     * federated callback and the console code-exchange {@code redirect_uri} stay consistent. IS server-to-server
     * endpoints (token/userinfo) and the federated authorize/logout use the internal IS base {@link #isServerBase}
     * ({@code wso2is:9443}) so the id_token issuer is consistent too.
     */
    private static final String APIM_INTERNAL = "https://localhost:9443/";

    private SsoProvisioner() {
    }

    // ---------------------------------------------------------------------------------------------------------
    // URL split
    // ---------------------------------------------------------------------------------------------------------

    /** APIM browser-facing base (trailing slash), e.g. {@code https://localhost:32771/}. */
    private static String apimBase() {
        Object v = TestContext.get("baseUrl");
        if (v == null) {
            throw new IllegalStateException("baseUrl not in context; the block must be booted first");
        }
        return v.toString();
    }

    /** IS browser-facing base (mapped host, trailing slash) — IdP authorize/logout + IS REST/SCIM2 management. */
    private static String isBrowserBase() {
        return IntegrationActors.baseUrl(IntegrationActors.IS);
    }

    /** IS server-facing base ({@code https://wso2is:9443/}) — IdP token/userinfo (server-to-server, id_token iss). */
    private static String isServerBase() {
        return "https://" + DynamicISContainer.NETWORK_ALIAS + ":9443/";
    }

    /** The IS integration actor's auth headers (CLAUDE.md §14 — IS's own principal, not an APIM actor). */
    private static Map<String, String> isAuthHeaders() {
        return IntegrationActors.authHeaders(IntegrationActors.IS);
    }

    // ---------------------------------------------------------------------------------------------------------
    // 1) IS OIDC application
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Creates the IS OIDC application the APIM consoles federate to (authorization_code, callback = APIM's
     * {@code commonauth}, groups claim requested, JIT subject = username), skips login/logout consent, and
     * returns {@code [clientId, clientSecret]}.
     */
    public static String[] createIsSsoApp(String appName) throws IOException {
        String base = isBrowserBase();

        // The IS app's callback and allowed origin are APIM's INTERNAL host: APIM (via its OIDC authenticator)
        // sends redirect_uri=https://localhost:9443/commonauth to IS, and the Playwright browser reaches that
        // through the CONNECT proxy. Using the internal host keeps redirect_uri consistent end to end.
        JSONObject oidcInbound = new JSONObject()
                .put("grantTypes", new JSONArray().put("authorization_code"))
                .put("callbackURLs", new JSONArray().put(APIM_INTERNAL + "commonauth"))
                .put("allowedOrigins", new JSONArray().put("https://localhost:9443"))
                .put("publicClient", false);
        JSONObject claimConfig = new JSONObject()
                .put("dialect", "LOCAL")
                .put("requestedClaims", new JSONArray().put(new JSONObject()
                        .put("claim", new JSONObject().put("uri", "http://wso2.org/claims/groups"))
                        .put("mandatory", true)))
                .put("subject", new JSONObject()
                        .put("claim", new JSONObject().put("uri", "http://wso2.org/claims/username"))
                        .put("includeTenantDomain", false));
        String createPayload = new JSONObject()
                .put("name", appName)
                .put("templateId", IS_APP_TEMPLATE_ID)
                .put("inboundProtocolConfiguration", new JSONObject().put("oidc", oidcInbound))
                .put("claimConfiguration", claimConfig)
                .toString();

        // A freshly-booted, block-private IS can trip a transient thread-safety race on its FIRST admin
        // authentications under load (java.util.ConcurrentModificationException in IdentityUserNameResolverListener
        // during admin auth), failing the first authenticated call. Warm the auth path with an IDEMPOTENT probe
        // (a GET, safe to retry) so the create runs exactly once against a settled auth path. Retrying the
        // non-idempotent create POST itself would be unsafe: retryUntil also re-invokes on IOException / other
        // non-201s, where the app may have been created server-side but the response lost — leaving a
        // duplicate-name 409 retry-loop and an un-swept app (the §15 warning against re-POSTing a non-idempotent
        // create; use an idempotent probe or adopt-the-survivor instead).
        try {
            Utils.retryUntil(0L,
                    () -> SimpleHTTPClient.getInstance().doGet(base + "api/server/v1/applications?limit=1",
                            isAuthHeaders()),
                    r -> r != null && r.getResponseCode() == 200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted warming the IS admin auth path", e);
        }
        // Auth path is warm — create the app exactly ONCE (this create is not idempotent).
        HttpResponse create = SimpleHTTPClient.getInstance().doPost(base + "api/server/v1/applications",
                isAuthHeaders(), createPayload, Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertTrue(create != null && create.getResponseCode() == 201,
                "IS SSO app create failed: got=" + (create == null ? "null"
                        : create.getResponseCode() + "/" + create.getData()));
        String appId = locationId(create);
        Assert.assertNotNull(appId, "Could not read created IS SSO app id from Location header");
        // Register for the IS-side teardown sweep BEFORE the follow-up config calls, so an aborted config
        // still leaves the app swept (as the IS integration actor — see ISResourceCleanup).
        ISResourceCleanup.registerApplication(appId);

        HttpResponse oidcResp = SimpleHTTPClient.getInstance().doGet(
                base + "api/server/v1/applications/" + appId + "/inbound-protocols/oidc", isAuthHeaders());
        Assert.assertTrue(oidcResp != null && oidcResp.getResponseCode() == 200 && oidcResp.getData() != null
                        && !oidcResp.getData().isBlank(),
                "IS OIDC inbound fetch failed: got=" + (oidcResp == null ? "null"
                        : oidcResp.getResponseCode() + "/" + oidcResp.getData()));
        JSONObject oidc = new JSONObject(oidcResp.getData());

        String patchPayload = new JSONObject().put("advancedConfigurations", new JSONObject()
                .put("skipLoginConsent", true).put("skipLogoutConsent", true)).toString();
        HttpResponse patch = SimpleHTTPClient.getInstance().doPatch(
                base + "api/server/v1/applications/" + appId, isAuthHeaders(), patchPayload,
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertTrue(patch != null && patch.getResponseCode() >= 200 && patch.getResponseCode() < 300,
                "IS SSO app skip-consent PATCH failed: got=" + (patch == null ? "null"
                        : patch.getResponseCode() + "/" + patch.getData()));

        return new String[]{oidc.getString("clientId"), oidc.getString("clientSecret")};
    }

    // ---------------------------------------------------------------------------------------------------------
    // 2) IS federated user
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Creates a federated user on IS via SCIM2 and places them in the given groups (the IdP's remote roles).
     * A group create that collides with an existing group (409) is downgraded to a SCIM PATCH add-member.
     */
    public static void createIsFederatedUser(String userName, String password, String email, List<String> groups)
            throws IOException {
        String base = isBrowserBase();

        String userPayload = new JSONObject()
                .put("schemas", new JSONArray().put("urn:ietf:params:scim:schemas:core:2.0:User"))
                .put("userName", userName)
                .put("password", password)
                .put("name", new JSONObject().put("givenName", userName).put("familyName", "SSO"))
                .put("emails", new JSONArray().put(new JSONObject().put("primary", true).put("value", email)))
                .toString();
        HttpResponse userResp = SimpleHTTPClient.getInstance().doPost(base + "scim2/Users",
                isAuthHeaders(), userPayload, SCIM_CONTENT_TYPE);
        Assert.assertTrue(userResp != null && userResp.getResponseCode() == 201 && userResp.getData() != null
                        && !userResp.getData().isBlank(),
                "IS federated user create failed: got=" + (userResp == null ? "null"
                        : userResp.getResponseCode() + "/" + userResp.getData()));
        String userId = new JSONObject(userResp.getData()).getString("id");
        ISResourceCleanup.registerUser(userId);

        for (String group : groups) {
            addUserToGroup(base, group, userId, userName);
        }
    }

    /** Creates the group with the user as a member; on 409 (group exists) PATCH-adds the member instead. */
    private static void addUserToGroup(String base, String group, String userId, String userName)
            throws IOException {
        String groupPayload = new JSONObject()
                .put("schemas", new JSONArray().put("urn:ietf:params:scim:schemas:core:2.0:Group"))
                .put("displayName", group)
                .put("members", new JSONArray().put(new JSONObject()
                        .put("value", userId).put("display", userName)))
                .toString();
        HttpResponse groupResp = SimpleHTTPClient.getInstance().doPost(base + "scim2/Groups",
                isAuthHeaders(), groupPayload, SCIM_CONTENT_TYPE);
        if (groupResp != null && groupResp.getResponseCode() == 201) {
            return;
        }
        Assert.assertTrue(groupResp != null && groupResp.getResponseCode() == 409,
                "IS group create for '" + group + "' failed: got=" + (groupResp == null ? "null"
                        : groupResp.getResponseCode() + "/" + groupResp.getData()));

        // Group already exists — resolve its id, then PATCH-add the member.
        String filter = Utils.urlEncode("displayName eq " + group);
        HttpResponse find = SimpleHTTPClient.getInstance().doGet(
                base + "scim2/Groups?filter=" + filter, isAuthHeaders());
        Assert.assertTrue(find != null && find.getResponseCode() == 200 && find.getData() != null
                        && !find.getData().isBlank(),
                "IS group lookup for '" + group + "' failed: got=" + (find == null ? "null"
                        : find.getResponseCode() + "/" + find.getData()));
        JSONObject found = new JSONObject(find.getData());
        JSONArray resources = found.optJSONArray("Resources");
        Assert.assertTrue(resources != null && resources.length() > 0,
                "IS group '" + group + "' reported existing (409) but was not found by displayName filter: "
                        + find.getData());
        String groupId = resources.getJSONObject(0).getString("id");

        String patchPayload = new JSONObject()
                .put("schemas", new JSONArray().put("urn:ietf:params:scim:schemas:api:messages:2.0:PatchOp"))
                .put("Operations", new JSONArray().put(new JSONObject()
                        .put("op", "add").put("path", "members").put("value", new JSONArray().put(new JSONObject()
                                .put("value", userId).put("display", userName)))))
                .toString();
        HttpResponse patch = SimpleHTTPClient.getInstance().doPatch(base + "scim2/Groups/" + groupId,
                isAuthHeaders(), patchPayload, SCIM_CONTENT_TYPE);
        Assert.assertTrue(patch != null && patch.getResponseCode() >= 200 && patch.getResponseCode() < 300,
                "IS group '" + group + "' PATCH add-member failed: got=" + (patch == null ? "null"
                        : patch.getResponseCode() + "/" + patch.getData()));
    }

    // ---------------------------------------------------------------------------------------------------------
    // 3) APIM-side OIDC identity provider
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Registers (idempotently) the APIM-side OIDC identity provider against the IS app: authorize/logout on the
     * IS browser base, token/userinfo on the IS server base, callback to APIM's {@code commonauth}, groups as the
     * role claim, JIT provisioning on, and role mappings (admin/publisher/subscriber remote roles → local roles).
     *
     * <p>{@code addIdP} returns HTTP 500 even on success, so this does NOT assert on the SOAP status — it
     * verifies via {@code getIdPByName} afterwards. Deletes any existing IdP of the same name first.
     */
    public static void registerOidcIdp(String idpName, String isClientId, String isClientSecret)
            throws IOException {
        deleteIdp(idpName);

        String isServer = isServerBase();

        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ns=\"" + NS_IDP_OPS + "\" xmlns:m=\"" + NS_MODEL + "\">"
                + "<soapenv:Body><ns:addIdP><ns:identityProvider>"
                + "<m:claimConfig>"
                + "<m:claimMappings><m:localClaim><m:claimUri>http://wso2.org/claims/role</m:claimUri></m:localClaim>"
                + "<m:remoteClaim><m:claimUri>groups</m:claimUri></m:remoteClaim></m:claimMappings>"
                + "<m:roleClaimURI>groups</m:roleClaimURI>"
                + "</m:claimConfig>"
                + "<m:defaultAuthenticatorConfig><m:name>OpenIDConnectAuthenticator</m:name></m:defaultAuthenticatorConfig>"
                + "<m:displayName>WSO2 IS7 OIDC</m:displayName><m:enable>true</m:enable>"
                + "<m:federatedAuthenticatorConfigs>"
                + "<m:displayName>openidconnect</m:displayName><m:enabled>true</m:enabled>"
                + "<m:name>OpenIDConnectAuthenticator</m:name>"
                + idpAuthProperty("ClientId", isClientId)
                + idpAuthProperty("ClientSecret", isClientSecret)
                // All IS endpoints use the INTERNAL wso2is:9443 host (browser reaches authorize/logout via the
                // CONNECT proxy; APIM reaches token/userinfo directly in-network) so the id_token issuer is
                // consistent. The federated callback is APIM's internal host (localhost:9443/commonauth).
                + idpAuthProperty("OAuth2AuthzEPUrl", isServer + "oauth2/authorize")
                + idpAuthProperty("OAuth2TokenEPUrl", isServer + "oauth2/token")
                + idpAuthProperty("UserInfoUrl", isServer + "oauth2/userinfo")
                + idpAuthProperty("OIDCLogoutEPUrl", isServer + "oidc/logout")
                + idpAuthProperty("callbackUrl", APIM_INTERNAL + "commonauth")
                + idpAuthProperty("Scopes", "openid groups")
                // Authenticate to the external IS token endpoint with the HTTP Basic header ONLY (not client
                // creds in the body). IS 7.x runs EVERY registered client authenticator's canAuthenticate() and
                // rejects with "invalid_request, The client MUST NOT use more than one authentication method" if
                // more than one engages (OAuthClientAuthnService.failOnMultipleAuthenticators). client_secret_post
                // puts client_id in the body, which engages a second authenticator alongside BasicAuth → the
                // token exchange fails and the federated login bounces back to login.do. Basic-header-only keeps
                // the body free of client_id so exactly one authenticator engages.
                + idpAuthProperty("IsBasicAuthEnabled", "true")
                + "</m:federatedAuthenticatorConfigs>"
                + "<m:federationHub>false</m:federationHub>"
                + "<m:identityProviderName>" + Utils.escapeXml(idpName) + "</m:identityProviderName>"
                // provisioningUserStore is REQUIRED: JIT federated-association (DefaultProvisioningHandler
                // .associateUser) builds a User from (tenantDomain, userStoreDomain, username) and IS rejects it
                // with "10005 - The provided user identifier is invalid" if userStoreDomain is empty. With no
                // provisioning user store configured, ExternalIdPConfig.getProvisioningUserStoreId() returns null
                // → the user is created in PRIMARY but the association User carries a null store domain → the
                // whole federated login fails post-authentication. Pin it to PRIMARY so the store domain is set.
                + "<m:justInTimeProvisioningConfig><m:provisioningEnabled>true</m:provisioningEnabled>"
                + "<m:passwordProvisioningEnabled>true</m:passwordProvisioningEnabled>"
                + "<m:provisioningUserStore>PRIMARY</m:provisioningUserStore></m:justInTimeProvisioningConfig>"
                + "<m:permissionAndRoleConfig>"
                + roleMapping("admin", "admin")
                + roleMapping("Internal/publisher", "publisher")
                + roleMapping("Internal/subscriber", "subscriber")
                + "</m:permissionAndRoleConfig>"
                + "</ns:identityProvider></ns:addIdP></soapenv:Body></soapenv:Envelope>";

        // addIdP faults with HTTP 500 even on success — do NOT assert on status; verify with getIdPByName.
        idpSoap("urn:addIdP", payload);
        Assert.assertTrue(getIdpExists(idpName),
                "OIDC identity provider '" + idpName + "' was not created on APIM (addIdP verify failed)");
    }

    /** Deletes the named IdP if present (idempotency helper and teardown). */
    public static void deleteIdp(String idpName) throws IOException {
        if (!getIdpExists(idpName)) {
            return;
        }
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ns=\"" + NS_IDP_OPS + "\"><soapenv:Body><ns:deleteIdP>"
                + "<ns:idPName>" + Utils.escapeXml(idpName) + "</ns:idPName>"
                + "</ns:deleteIdP></soapenv:Body></soapenv:Envelope>";
        idpSoap("urn:deleteIdP", payload);
    }

    /** Whether the named IdP exists on APIM (verifies via {@code getIdPByName}; mirrors TokenExchangeProvisioner). */
    private static boolean getIdpExists(String idpName) throws IOException {
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ns=\"" + NS_IDP_OPS + "\"><soapenv:Body><ns:getIdPByName>"
                + "<ns:idPName>" + Utils.escapeXml(idpName) + "</ns:idPName>"
                + "</ns:getIdPByName></soapenv:Body></soapenv:Envelope>";
        HttpResponse r = idpSoap("urn:getIdPByName", payload);
        // A transport-level failure must FAIL the check, never read as "absent" (would false-pass a negative).
        if (r == null || r.getData() == null || r.getData().isBlank()) {
            throw new IllegalStateException("IdP existence check failed for '" + idpName + "': got "
                    + (r == null ? "no response" : r.getResponseCode() + " / blank body"));
        }
        return r.getData().contains("identityProviderName>" + idpName);
    }

    private static String idpAuthProperty(String name, String value) {
        return "<m:properties><m:name>" + Utils.escapeXml(name) + "</m:name>"
                + "<m:value>" + Utils.escapeXml(value) + "</m:value></m:properties>";
    }

    private static String roleMapping(String localRole, String remoteRole) {
        return "<m:roleMappings><m:localRole><m:localRoleName>" + Utils.escapeXml(localRole)
                + "</m:localRoleName></m:localRole><m:remoteRole>" + Utils.escapeXml(remoteRole)
                + "</m:remoteRole></m:roleMappings>";
    }

    private static HttpResponse idpSoap(String action, String payload) throws IOException {
        return SimpleHTTPClient.getInstance().sendSoapRequest(
                apimBase() + "services/IdentityProviderMgtService", payload, action, "admin", "admin");
    }

    // ---------------------------------------------------------------------------------------------------------
    // 4) Wire the multi-option (local + federated) auth step onto a console service provider
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Wires the {@code idpName} OIDC IdP as a second login option (alongside the local basic authenticator) onto
     * the named console service provider. The SP is created lazily by APIM the first time the console's login
     * endpoint is hit, so this triggers that, resolves the SP's application id and inbound OAuth key via
     * {@code getApplication}, and pushes an xsi-free minimal {@code updateApplication} — round-tripping the full
     * {@code getApplication} output back into {@code updateApplication} triggers an Axis2 StackOverflow, so the
     * payload is hand-built, not echoed.
     */
    public static void wireConsoleMultiOption(String consoleContext, String spName, String idpName)
            throws IOException {
        String apim = apimBase();

        // A console service provider is registered lazily (via DCR) the first time its login servlet is hit, and
        // the console webapp may not be deployed yet when the block's Publisher-readiness gate opens (Publisher is
        // ready before Admin/DevPortal). Poll within the propagation window: hit the login servlet to trigger
        // registration, then getApplication, until the SP's applicationID appears.
        String getPayload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ns1=\"" + NS_APP_OPS + "\"><soapenv:Body><ns1:getApplication>"
                + "<ns1:applicationName>" + Utils.escapeXml(spName) + "</ns1:applicationName>"
                + "</ns1:getApplication></soapenv:Body></soapenv:Envelope>";
        HttpResponse getResp;
        try {
            getResp = Utils.retryUntil(0L,
                    () -> {
                        // The DCR-triggering login JSP (idp.jsp) is mapped differently per console: publisher/admin
                        // at /services/auth/login, devportal (the store app) at /services/configs. Hit both so the
                        // DCR that registers the console's OAuth service provider fires.
                        for (String loginPath : new String[] {"/services/auth/login", "/services/configs"}) {
                            try {
                                SimpleHTTPClient.getInstance().doGet(
                                        apim + consoleContext + loginPath, new HashMap<>());
                            } catch (IOException ignored) {
                                // best-effort trigger; the getApplication below is the readiness signal
                            }
                        }
                        return appSoap("urn:getApplication", getPayload);
                    },
                    r -> r != null && r.getData() != null
                            && firstMatch(r.getData(), "applicationID>([^<]+)<") != null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted waiting for console SP '" + spName + "' registration", e);
        }
        Assert.assertNotNull(getResp, "getApplication for console SP '" + spName + "' never returned a response");
        String body = getResp.getData();
        String appId = firstMatch(body, "applicationID>([^<]+)<");
        String inboundAuthKey = firstMatch(body, "inboundAuthKey>([^<]+)<");
        Assert.assertNotNull(appId, "Could not resolve applicationID for console SP '" + spName
                + "' from getApplication response: " + body);
        Assert.assertNotNull(inboundAuthKey, "Could not resolve inboundAuthKey for console SP '" + spName
                + "' from getApplication response: " + body);

        String updatePayload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:axis2=\"" + NS_APP_OPS + "\"><soapenv:Body><axis2:updateApplication>"
                + "<axis2:serviceProvider xmlns:m=\"" + NS_MODEL + "\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<m:applicationID>" + Utils.escapeXml(appId) + "</m:applicationID>"
                + "<m:applicationName>" + Utils.escapeXml(spName) + "</m:applicationName>"
                + "<m:inboundAuthenticationConfig><m:inboundAuthenticationRequestConfigs>"
                + "<m:inboundAuthKey>" + Utils.escapeXml(inboundAuthKey) + "</m:inboundAuthKey>"
                + "<m:inboundAuthType>oauth2</m:inboundAuthType>"
                + "</m:inboundAuthenticationRequestConfigs></m:inboundAuthenticationConfig>"
                + "<m:localAndOutBoundAuthenticationConfig>"
                + "<m:authenticationSteps>"
                + "<m:attributeStep>true</m:attributeStep>"
                + "<m:federatedIdentityProviders>"
                + "<m:defaultAuthenticatorConfig><m:name>OpenIDConnectAuthenticator</m:name>"
                + "<m:enabled>true</m:enabled></m:defaultAuthenticatorConfig>"
                + "<m:federatedAuthenticatorConfigs><m:name>OpenIDConnectAuthenticator</m:name>"
                + "<m:enabled>true</m:enabled></m:federatedAuthenticatorConfigs>"
                + "<m:identityProviderName>" + Utils.escapeXml(idpName) + "</m:identityProviderName>"
                + "</m:federatedIdentityProviders>"
                + "<m:localAuthenticatorConfigs>"
                + "<m:displayName>basic</m:displayName><m:enabled>true</m:enabled><m:name>BasicAuthenticator</m:name>"
                + "</m:localAuthenticatorConfigs>"
                + "<m:stepOrder>1</m:stepOrder>"
                + "<m:subjectStep>true</m:subjectStep>"
                + "</m:authenticationSteps>"
                + "<m:authenticationType>flow</m:authenticationType>"
                // Skip the OAuth2 consent prompt for the console SP. Without this the federated login, after JIT
                // provisioning, halts at authenticationendpoint/oauth2_consent.do (a 200 page the headless walk
                // cannot approve) instead of redirecting to the console callback with a code. Consoles are
                // first-party apps, so consent is not meaningful here.
                + "<m:skipConsent>true</m:skipConsent>"
                + "<m:skipLogoutConsent>true</m:skipLogoutConsent>"
                + "</m:localAndOutBoundAuthenticationConfig>"
                + "<m:saasApp>true</m:saasApp></axis2:serviceProvider></axis2:updateApplication>"
                + "</soapenv:Body></soapenv:Envelope>";
        appSoap("urn:updateApplication", updatePayload);

        // The console's OAuth client was DCR-registered at startup against APIM's INTERNAL host, but the
        // containerised test drives the console on the mapped container port — so its redirect_uri never matches
        // the registered callback (invalid_callback). Relax the callback to a boot-port-agnostic regex.
        updateConsoleCallbackToRegex(inboundAuthKey);
    }

    /**
     * Rewrites a console OAuth client's callback (identified by its consumer key) to a regex that matches the
     * console callback path on ANY localhost port — the mapped container port varies per boot — via
     * {@code OAuthAdminService}. The client's other fields are read first and preserved, since
     * {@code updateConsumerApplication} replaces the whole DTO.
     */
    private static void updateConsoleCallbackToRegex(String consumerKey) throws IOException {
        String getPayload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:xsd=\"http://org.apache.axis2/xsd\"><soapenv:Body><xsd:getOAuthApplicationData>"
                + "<xsd:consumerKey>" + Utils.escapeXml(consumerKey) + "</xsd:consumerKey>"
                + "</xsd:getOAuthApplicationData></soapenv:Body></soapenv:Envelope>";
        HttpResponse getResp = oauthSoap("urn:getOAuthApplicationData", getPayload);
        String body = getResp == null ? "" : getResp.getData();
        String appName = firstMatch(body, "applicationName>([^<]+)<");
        String secret = firstMatch(body, "oauthConsumerSecret>([^<]+)<");
        String version = firstMatch(body, "[Oo][Aa]uthVersion>([^<]+)<");
        String grants = firstMatch(body, "grantTypes>([^<]*)<");
        Assert.assertNotNull(appName, "Could not read OAuth app (applicationName) for consumerKey '" + consumerKey
                + "' from getOAuthApplicationData: " + body);
        Assert.assertNotNull(secret, "Could not read consumerSecret for '" + consumerKey + "': " + body);
        // Match the console's original two-alternative shape (login|logout); both alternatives are
        // boot-port-agnostic so the mapped container port validates against whichever the console redirects to.
        String regexCallback = "regexp=(https://localhost:\\d+/.*/services/auth/callback/login"
                + "|https://localhost:\\d+/.*/services/auth/callback/logout)";
        // The DTO-carrying element MUST be named for the operation's real Java parameter — Axis2's RPC
        // deserializer (BeanUtil.deserialize) matches each SOAP child to a method parameter by name; a name that
        // is neither "arg*"/"item*" nor the actual parameter overruns the single-element param array and faults
        // with "Index 1 out of bounds for length 1". updateConsumerApplication(OAuthConsumerAppDTO consumerAppDTO)
        // so the element is <consumerAppDTO> (getOAuthApplicationData worked earlier because its element was
        // <consumerKey>, that method's real parameter name).
        String updatePayload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:xsd=\"http://org.apache.axis2/xsd\" "
                + "xmlns:xsd1=\"http://dto.oauth.identity.carbon.wso2.org/xsd\"><soapenv:Body>"
                + "<xsd:updateConsumerApplication><xsd:consumerAppDTO>"
                + "<xsd1:OAuthVersion>" + Utils.escapeXml(version == null ? "OAuth-2.0" : version)
                + "</xsd1:OAuthVersion>"
                + "<xsd1:applicationName>" + Utils.escapeXml(appName) + "</xsd1:applicationName>"
                + "<xsd1:callbackUrl>" + Utils.escapeXml(regexCallback) + "</xsd1:callbackUrl>"
                + (grants == null ? "" : "<xsd1:grantTypes>" + Utils.escapeXml(grants) + "</xsd1:grantTypes>")
                + "<xsd1:oauthConsumerKey>" + Utils.escapeXml(consumerKey) + "</xsd1:oauthConsumerKey>"
                + "<xsd1:oauthConsumerSecret>" + Utils.escapeXml(secret) + "</xsd1:oauthConsumerSecret>"
                // Disable PKCE on the console OAuth client. The console login initiator (idp.jsp) uses PKCE when
                // the client has PKCE_MANDATORY set (SystemApplicationDAO.isPKCEEnabled), storing the
                // code_verifier in the server-side HTTP session; login_callback.jsp then reads it back to complete
                // the code exchange. A headless client accumulates multiple JSESSIONIDs across the federated
                // round-trip and cannot scope them per path like a browser, so the callback's session (and the
                // code_verifier) is lost and the token exchange fails — no AM_ACC_TOKEN cookie. With PKCE off the
                // exchange authenticates with the client secret alone (Basic), independent of the browser session.
                + "<xsd1:pkceMandatory>false</xsd1:pkceMandatory>"
                + "<xsd1:pkceSupportPlain>false</xsd1:pkceSupportPlain>"
                + "</xsd:consumerAppDTO></xsd:updateConsumerApplication></soapenv:Body></soapenv:Envelope>";
        HttpResponse updResp = oauthSoap("urn:updateConsumerApplication", updatePayload);
        String updBody = updResp == null ? "null" : updResp.getResponseCode() + " " + updResp.getData();
        Assert.assertTrue(updResp != null && updResp.getData() != null
                        && !updResp.getData().toLowerCase().contains("faultstring")
                        && !updResp.getData().toLowerCase().contains("exception"),
                "updateConsumerApplication failed for consumerKey '" + consumerKey + "': " + updBody
                        + "\n---- getOAuthApplicationData response was ----\n" + body);
    }

    private static HttpResponse oauthSoap(String action, String payload) throws IOException {
        return SimpleHTTPClient.getInstance().sendSoapRequest(
                apimBase() + "services/OAuthAdminService", payload, action, "admin", "admin");
    }

    private static HttpResponse appSoap(String action, String payload) throws IOException {
        return SimpleHTTPClient.getInstance().sendSoapRequest(
                apimBase() + "services/IdentityApplicationManagementService", payload, action, "admin", "admin");
    }

    /** First regex-group-1 match in the text, or null. */
    private static String firstMatch(String text, String regex) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private static String locationId(HttpResponse resp) {
        if (resp == null || resp.getHeaders() == null) {
            return null;
        }
        String location = resp.getHeaders().get("Location");
        if (location == null) {
            location = resp.getHeaders().get("location");
        }
        if (location == null) {
            return null;
        }
        int slash = location.lastIndexOf('/');
        return slash >= 0 ? location.substring(slash + 1) : location;
    }
}
