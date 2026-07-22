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
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Drives the WSO2-IS-7 tenant-sharing default-key-manager path (plan items #24/#31) via APIM's tenant-sharing
 * notify endpoint - self-contained over {@link SimpleHTTPClient}, exactly as the legacy backend test
 * {@code APIMTenantCreationNotificationTestCase} does: it POSTs a {@code tenantCreated} event to
 * {@code /internal/data/v1/notify} (no external IS is needed - the WSO2-IS-7 key manager is created from the
 * event, not fetched from IS), then reads the provisioned tenant's key managers with a freshly-minted tenant
 * admin token. The tenant becomes active a moment after the notify returns (an internal SOAP self-call), so the
 * admin-token acquisition retries until the tenant is ready.
 */
public final class DefaultKmProvisioner {

    private static final String NOTIFY_PATH = "internal/data/v1/notify";
    private static final String TENANT_CREATED_EVENT =
            "https://schemas.identity.wso2.org/events/tenant/event-type/tenantCreated";
    /** Header the TenantSyncListener sends and APIM's TenantManagementEventHandler routes on. */
    private static final String KM_HEADER = "X-WSO2-KEY-MANAGER";
    private static final String KM_HEADER_VALUE = "TENANT_MANAGEMENT";
    private static final long ADMIN_TOKEN_WAIT_MS = 120_000L;

    private DefaultKmProvisioner() {
    }

    private static String apimBase() {
        Object v = TestContext.get("baseUrl");
        if (v == null) {
            throw new IllegalStateException("baseUrl not in context; the block must be booted first");
        }
        return v.toString();
    }

    private static Map<String, String> basicAuth(String user, String pass) {
        Map<String, String> h = new HashMap<>();
        h.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic " + Base64.getEncoder()
                .encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8)));
        return h;
    }

    private static Map<String, String> bearer(String token) {
        Map<String, String> h = new HashMap<>();
        h.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + token);
        return h;
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    /**
     * POSTs a {@code tenantCreated} tenant-management event to APIM's notify endpoint as the super admin,
     * simulating the IS TenantSyncListener. The tenant owner is {@code admin} with the given password.
     */
    public static HttpResponse notifyTenantCreated(String tenantDomain, String tenantAdminPass) throws IOException {
        JSONObject owner = new JSONObject()
                .put("username", "admin")
                .put("password", tenantAdminPass)
                .put("email", "admin@" + tenantDomain)
                .put("firstname", "Dkm")
                .put("lastname", "Test");
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

        Map<String, String> headers = basicAuth(Constants.SUPER_TENANT_ADMIN_USERNAME,
                Constants.SUPER_TENANT_ADMIN_PASSWORD);
        headers.put(KM_HEADER, KM_HEADER_VALUE);
        return SimpleHTTPClient.getInstance().doPost(apimBase() + NOTIFY_PATH, headers,
                event.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * DCR + password grant for the given user, retried until the (possibly just-synced) tenant is active and
     * issues a token, or the startup window elapses.
     */
    public static String awaitAdminToken(String user, String pass, String scope) {
        long deadline = System.currentTimeMillis() + ADMIN_TOKEN_WAIT_MS;
        String last = "no attempt";
        while (System.currentTimeMillis() < deadline) {
            try {
                String creds = dcr(user, pass);
                if (creds != null) {
                    String token = passwordToken(creds, user, pass, scope);
                    if (token != null) {
                        return token;
                    }
                    last = "DCR ok, token not issued yet";
                } else {
                    last = "DCR not ready";
                }
            } catch (IOException e) {
                last = e.getMessage();
            }
            sleep();
        }
        throw new IllegalStateException("Could not obtain an admin token for '" + user + "' within "
                + (ADMIN_TOKEN_WAIT_MS / 1000) + "s (tenant may not have synced). Last: " + last);
    }

    private static String dcr(String user, String pass) throws IOException {
        JSONObject body = new JSONObject()
                .put("callbackUrl", "test.com")
                .put("clientName", "dkm_" + user.replace("@", "_"))
                .put("grantType", "password client_credentials refresh_token")
                .put("saasApp", true)
                .put("owner", user);
        HttpResponse r = SimpleHTTPClient.getInstance().doPost(Utils.getDCREndpointURL(apimBase()),
                basicAuth(user, pass), body.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
        if (r == null || r.getResponseCode() != 200 || r.getData() == null) {
            return null;
        }
        JSONObject j = new JSONObject(r.getData());
        return Base64.getEncoder().encodeToString((j.getString("clientId") + ":" + j.getString("clientSecret"))
                .getBytes(StandardCharsets.UTF_8));
    }

    private static String passwordToken(String creds, String user, String pass, String scope) throws IOException {
        String form = "grant_type=password&username=" + enc(user) + "&password=" + enc(pass)
                + "&scope=" + enc(scope);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic " + creds);
        HttpResponse r = SimpleHTTPClient.getInstance().doPost(Utils.getAPIMTokenEndpointURL(apimBase()),
                headers, form, "application/x-www-form-urlencoded");
        if (r == null || r.getResponseCode() != 200 || r.getData() == null) {
            return null;
        }
        return new JSONObject(r.getData()).getString("access_token");
    }

    /** Admin REST GET of the acting principal's tenant key managers. */
    public static HttpResponse listKeyManagers(String token) throws IOException {
        return SimpleHTTPClient.getInstance().doGet(Utils.getKeyManagersURL(apimBase()), bearer(token));
    }

    /** Creates a Developer Portal application (returns the raw response; caller reads {@code applicationId}). */
    public static HttpResponse createApplication(String token, String name) throws IOException {
        JSONObject body = new JSONObject()
                .put("name", name)
                .put("throttlingPolicy", "Unlimited")
                .put("tokenType", "JWT");
        return SimpleHTTPClient.getInstance().doPost(Utils.getApplicationCreateURL(apimBase()),
                bearer(token), body.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Attempts PRODUCTION key generation for an application against the (named) Resident Key Manager. */
    public static HttpResponse generateKeys(String token, String applicationId) throws IOException {
        JSONObject body = new JSONObject()
                .put("keyType", "PRODUCTION")
                .put("grantTypesToBeSupported", new JSONArray().put("client_credentials"))
                .put("keyManager", "Resident Key Manager")
                .put("validityTime", 3600);
        return SimpleHTTPClient.getInstance().doPost(
                Utils.getGenerateApplicationKeysURL(apimBase(), applicationId),
                bearer(token), body.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    private static void sleep() {
        try {
            Thread.sleep(3000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
