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
import org.wso2.am.integration.cucumbertests.utils.ResourceCleanup;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

/**
 * Platform (Universal / self-hosted) gateway glue: admin-plane CRUD of {@code /gateways}, the internal
 * control-plane REST surface a connected gateway calls ({@code /internal/data/v1/deployments[...]},
 * {@code /internal/gateway/.well-known}), and the internal WebSocket registration handshake
 * ({@code /internal/data/v1/ws/gateways/connect}). Internal endpoints authenticate with an {@code api-key}
 * header (the gateway's registration token) rather than a Bearer token, so they funnel through
 * {@code Requests}/{@code SimpleHTTPClient} with an explicit header map rather than the {@code Identity}
 * token helpers used for the admin-plane calls.
 */
public class PlatformGatewaySteps {

    private final BaseSteps baseSteps = new BaseSteps();

    private static final String API_KEY_HEADER = "api-key";
    private static final String GATEWAY_ID_KEY = "platformGatewayId";
    private static final String GATEWAY_NAME_KEY = "platformGatewayName";
    private static final String GATEWAY_VHOST_KEY = "platformGatewayVhost";
    private static final String GATEWAY_TOKEN_KEY = "platformGatewayToken";
    private static final String GATEWAY_OLD_TOKEN_KEY = "platformGatewayOldToken";
    private static final String WS_HANDLE_KEY = "platformGatewayWsHandle";
    private static final String WS_LAST_EVENT_KEY = "platformGatewayLastWsEvent";
    private static final String WS_LAST_EVENT_TYPE_KEY = "platformGatewayLastWsEventType";
    private static final String FETCH_BATCH_TEXT_KEY = "platformGatewayFetchBatchText";

    private String getBaseUrl() {
        return baseSteps.getBaseUrl();
    }

    // ---- Platform Gateway CRUD (admin API: /api/am/admin/v4/gateways) ----

    private String resolveName(String raw) {
        return Utils.resolveContextPlaceholders(Utils.resolvePayloadPlaceholders(raw));
    }

    /** Resolves a value that may be a known context key OR a literal (e.g. an unknown id / bad token). */
    private String resolveIdOrLiteral(String key) {
        if (TestContext.contains(key)) {
            return TestContext.get(key).toString();
        }
        return Utils.resolveContextPlaceholders(key);
    }

    private void createPlatformGateway(String nameRaw, String permissionType, String role) throws IOException {
        // Names.unique() (behind ${UNIQUE:...}) joins base/suffix/counter with underscores, but a platform
        // gateway name is validated against ^[a-z0-9-]+$ (stricter than a gateway environment's name) — swap
        // to hyphens. Safe for the invalid-name negatives too: none of those literals contain an underscore
        // except "Invalid_Name", which still fails the regex on its uppercase letters after the swap.
        String name = resolveName(nameRaw).replace("_", "-");
        String vhost = "https://localhost:9999";
        JSONObject body = new JSONObject()
                .put("name", name)
                .put("displayName", name)
                .put("description", "Platform gateway created by PlatformGatewaySteps")
                .put("vhost", vhost);
        if (permissionType != null) {
            body.put("permissions", new JSONObject()
                    .put("permissionType", permissionType)
                    .put("roles", new JSONArray().put(role)));
        }
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = Requests.post(Utils.getPlatformGatewaysURL(getBaseUrl()), headers, body.toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object id = Utils.extractValueFromPayload(response.getData(), "id");
            Object token = Utils.extractValueFromPayload(response.getData(), "registrationToken");
            if (id != null) {
                TestContext.set(GATEWAY_ID_KEY, id);
                ResourceCleanup.register(Constants.CREATED_PLATFORM_GATEWAY_IDS, id);
            }
            TestContext.set(GATEWAY_NAME_KEY, name);
            TestContext.set(GATEWAY_VHOST_KEY, vhost);
            if (token != null) {
                TestContext.set(GATEWAY_TOKEN_KEY, token);
            }
        }
    }

    /** Registers a platform gateway. Non-asserting — the feature confirms 201 or a validation negative. */
    @When("I register a platform gateway named {string}")
    public void iRegisterPlatformGateway(String nameBase) throws IOException {
        createPlatformGateway(nameBase, null, null);
    }

    /** Registers a platform gateway restricted to the given role via an ALLOW permission. */
    @When("I register a platform gateway named {string} allowing role {string}")
    public void iRegisterPlatformGatewayAllowingRole(String nameBase, String role) throws IOException {
        createPlatformGateway(nameBase, "ALLOW", role);
    }

    /** Registers a platform gateway that hides itself from the given role via a DENY permission. */
    @When("I register a platform gateway named {string} denying role {string}")
    public void iRegisterPlatformGatewayDenyingRole(String nameBase, String role) throws IOException {
        createPlatformGateway(nameBase, "DENY", role);
    }

    @When("I retrieve all platform gateways")
    public void iRetrieveAllPlatformGateways() throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = Requests.get(Utils.getPlatformGatewaysURL(getBaseUrl()), headers);
    }

    private void updatePlatformGateway(String idKey, String name, String vhost, String displayName,
                                       String description) throws IOException {
        String id = resolveIdOrLiteral(idKey);
        JSONObject body = new JSONObject()
                .put("name", name)
                .put("vhost", vhost)
                .put("displayName", displayName)
                .put("description", description);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = Requests.put(Utils.getPlatformGatewayByIdURL(getBaseUrl(), id), headers,
                body.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Updates a platform gateway's display name/description (name/vhost carried over unchanged — PUT keeps
     *  the name immutable). Non-asserting. */
    @When("I update the platform gateway {string} setting its display name to {string} and description to {string}")
    public void iUpdatePlatformGateway(String idKey, String displayName, String description) throws IOException {
        String name = TestContext.resolve(GATEWAY_NAME_KEY).toString();
        String vhost = TestContext.resolve(GATEWAY_VHOST_KEY).toString();
        updatePlatformGateway(idKey, name, vhost, displayName, description);
    }

    /** PUT with a mismatched {@code name} (name is immutable) — negative. Non-asserting. */
    @When("I update the platform gateway {string} with name {string}")
    public void iUpdatePlatformGatewayWithName(String idKey, String newName) throws IOException {
        String resolvedNewName = Utils.resolveContextPlaceholders(newName);
        String vhost = TestContext.resolve(GATEWAY_VHOST_KEY).toString();
        updatePlatformGateway(idKey, resolvedNewName, vhost, "Updated display", "updated");
    }

    /** PUT with an invalid {@code vhost} — negative. Non-asserting. */
    @When("I update the platform gateway {string} with vhost {string}")
    public void iUpdatePlatformGatewayWithVhost(String idKey, String vhost) throws IOException {
        String name = TestContext.resolve(GATEWAY_NAME_KEY).toString();
        updatePlatformGateway(idKey, name, vhost, "Display", "desc");
    }

    /** Regenerates a platform gateway's registration token; the old token is preserved under a separate key
     *  so a following step can assert it no longer authenticates. Non-asserting. */
    @When("I regenerate the token for the platform gateway {string}")
    public void iRegenerateToken(String idKey) throws IOException {
        String id = resolveIdOrLiteral(idKey);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = Requests.post(Utils.getPlatformGatewayRegenerateTokenURL(getBaseUrl(), id), headers,
                null, Constants.CONTENT_TYPES.APPLICATION_JSON);
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Object oldToken = TestContext.get(GATEWAY_TOKEN_KEY);
            if (oldToken != null) {
                TestContext.set(GATEWAY_OLD_TOKEN_KEY, oldToken);
            }
            Object newToken = Utils.extractValueFromPayload(response.getData(), "registrationToken");
            if (newToken != null) {
                TestContext.set(GATEWAY_TOKEN_KEY, newToken);
            }
        }
    }

    /** Deletes a platform gateway by id (may be a context key such as {@code platformGatewayId} or a literal
     *  unknown id). Non-asserting (also used for the delete-then-404 / conflict negatives). */
    @When("I delete the platform gateway with id {string}")
    public void iDeletePlatformGateway(String idKey) throws IOException {
        String id = resolveIdOrLiteral(idKey);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = Requests.delete(Utils.getPlatformGatewayByIdURL(getBaseUrl(), id), headers);
    }

    /**
     * Looks up a platform gateway's id by name via {@code GET /gateways} and stores it. Used for a
     * TOML-declared ("connect-with-token") gateway, whose id is DB-assigned at lazy bootstrap time rather than
     * known up front — unlike a dynamically {@code POST}-registered one, whose id our own create response
     * already gives us.
     */
    @When("I look up the platform gateway id for name {string} and store it as {string}")
    public void iLookUpPlatformGatewayIdForName(String name, String ctxKey) throws IOException {
        String resolvedName = Utils.resolveContextPlaceholders(name);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = SimpleHTTPClient.getInstance().doGet(Utils.getPlatformGatewaysURL(getBaseUrl()),
                headers);
        Assert.assertTrue(response != null && response.getResponseCode() == 200 && response.getData() != null,
                "Failed to list platform gateways while looking up '" + resolvedName + "': "
                        + (response == null ? "no response" : response.getResponseCode() + " / " + response.getData()));
        JSONObject json = new JSONObject(response.getData());
        JSONArray list = json.optJSONArray("list");
        Assert.assertNotNull(list, "GET /gateways returned no 'list' while looking up '" + resolvedName + "'");
        for (int i = 0; i < list.length(); i++) {
            JSONObject g = list.getJSONObject(i);
            if (resolvedName.equals(g.optString("name"))) {
                TestContext.set(ctxKey, g.getString("id"));
                return;
            }
        }
        Assert.fail("No platform gateway named '" + resolvedName + "' found in GET /gateways");
    }

    /** Registers an id NOT created by {@link #createPlatformGateway} (e.g. a TOML-declared connect-with-token
     *  gateway, whose id is a fixed fixture constant rather than a response from our own POST) for the
     *  runner's {@code @AfterClass} cleanup sweep. */
    @When("I register the platform gateway {string} for cleanup")
    public void iRegisterPlatformGatewayForCleanup(String idKey) {
        ResourceCleanup.register(Constants.CREATED_PLATFORM_GATEWAY_IDS, resolveIdOrLiteral(idKey));
    }

    /** Polls {@code GET /gateways} until the given gateway's {@code isActive} flag matches {@code active} or
     *  {@code inactive}, or fails after the timeout. Reflects the internal WebSocket connect/disconnect state. */
    @Then("The platform gateway {string} should become {word} within {int} seconds")
    public void thePlatformGatewayShouldBecome(String idKey, String state, int timeoutSeconds) throws Exception {
        boolean expectActive;
        if ("active".equalsIgnoreCase(state)) {
            expectActive = true;
        } else if ("inactive".equalsIgnoreCase(state)) {
            expectActive = false;
        } else {
            throw new IllegalArgumentException("Expected 'active' or 'inactive', got: " + state);
        }
        String id = resolveIdOrLiteral(idKey);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        long deadline = System.currentTimeMillis() + Math.max(timeoutSeconds * 1000L, 5000L);
        Boolean last = null;
        while (System.currentTimeMillis() < deadline) {
            HttpResponse response = SimpleHTTPClient.getInstance()
                    .doGet(Utils.getPlatformGatewaysURL(getBaseUrl()), headers);
            last = findIsActive(response, id);
            if (last != null && last == expectActive) {
                return;
            }
            Thread.sleep(500);
        }
        Assert.fail("Timed out waiting for platform gateway " + id + " to become " + state
                + " (last isActive=" + last + ")");
    }

    private Boolean findIsActive(HttpResponse response, String gatewayId) {
        if (response == null || response.getResponseCode() != 200 || response.getData() == null) {
            return null;
        }
        JSONObject json = new JSONObject(response.getData());
        JSONArray list = json.optJSONArray("list");
        if (list == null) {
            return null;
        }
        for (int i = 0; i < list.length(); i++) {
            JSONObject g = list.getJSONObject(i);
            if (gatewayId.equals(g.optString("id"))) {
                return g.optBoolean("isActive");
            }
        }
        return null;
    }

    // ---- Internal control-plane REST (api-key header auth) ----

    @When("I retrieve internal deployments using api key {string}")
    public void iRetrieveInternalDeployments(String tokenKey) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(API_KEY_HEADER, resolveIdOrLiteral(tokenKey));
        HttpResponse response = Requests.get(Utils.getInternalDeploymentsURL(getBaseUrl()), headers);
    }

    @When("I retrieve internal deployments without an api key")
    public void iRetrieveInternalDeploymentsNoKey() throws IOException {
        HttpResponse response = Requests.get(Utils.getInternalDeploymentsURL(getBaseUrl()), new HashMap<>());
    }

    /** Polls internal {@code /deployments} until the given API's synced deployment appears, storing its
     *  {@code deploymentId} as {@code platformGatewayDeploymentId} for a following fetch-batch step — the
     *  publisher-plane deploy takes a moment to propagate to the internal sync surface. */
    @When("I wait until internal deployments list {string} using api key {string} within {int} seconds")
    public void iWaitUntilInternalDeploymentsList(String apiIdKey, String tokenKey, int timeoutSeconds)
            throws Exception {
        String apiId = resolveIdOrLiteral(apiIdKey);
        Map<String, String> headers = new HashMap<>();
        headers.put(API_KEY_HEADER, resolveIdOrLiteral(tokenKey));
        long deadline = System.currentTimeMillis() + Math.max(timeoutSeconds * 1000L, 5000L);
        while (System.currentTimeMillis() < deadline) {
            HttpResponse response = SimpleHTTPClient.getInstance()
                    .doGet(Utils.getInternalDeploymentsURL(getBaseUrl()), headers);
            String deploymentId = findDeploymentIdForApi(response, apiId);
            if (deploymentId != null) {
                TestContext.set("platformGatewayDeploymentId", deploymentId);
                return;
            }
            Thread.sleep(500);
        }
        Assert.fail("Timed out waiting for internal /deployments to list API " + apiId);
    }

    /** The converse of {@link #iWaitUntilInternalDeploymentsList} — polls until the API's deployment is GONE
     *  from internal {@code /deployments} (used to confirm an undeploy propagated). */
    @Then("Internal deployments should stop listing {string} using api key {string} within {int} seconds")
    public void internalDeploymentsShouldStopListing(String apiIdKey, String tokenKey, int timeoutSeconds)
            throws Exception {
        String apiId = resolveIdOrLiteral(apiIdKey);
        Map<String, String> headers = new HashMap<>();
        headers.put(API_KEY_HEADER, resolveIdOrLiteral(tokenKey));
        long deadline = System.currentTimeMillis() + Math.max(timeoutSeconds * 1000L, 5000L);
        while (System.currentTimeMillis() < deadline) {
            HttpResponse response = SimpleHTTPClient.getInstance()
                    .doGet(Utils.getInternalDeploymentsURL(getBaseUrl()), headers);
            if (findDeploymentIdForApi(response, apiId) == null) {
                return;
            }
            Thread.sleep(500);
        }
        Assert.fail("Timed out waiting for internal /deployments to drop API " + apiId);
    }

    private String findDeploymentIdForApi(HttpResponse response, String apiId) {
        if (response == null || response.getResponseCode() != 200 || response.getData() == null) {
            return null;
        }
        JSONObject json = new JSONObject(response.getData());
        JSONArray deployments = json.optJSONArray("deployments");
        if (deployments == null) {
            return null;
        }
        for (int i = 0; i < deployments.length(); i++) {
            JSONObject d = deployments.getJSONObject(i);
            if (apiId.equals(d.optString("artifactId"))) {
                String id = d.optString("deploymentId");
                return id.isEmpty() ? null : id;
            }
        }
        return null;
    }

    @When("I retrieve internal deployments with since {string} using api key {string}")
    public void iRetrieveInternalDeploymentsSince(String since, String tokenKey) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(API_KEY_HEADER, resolveIdOrLiteral(tokenKey));
        String url = Utils.getInternalDeploymentsURL(getBaseUrl()) + "?since="
                + java.net.URLEncoder.encode(since, StandardCharsets.UTF_8);
        HttpResponse response = Requests.get(url, headers);
    }

    /** Fetch-batch, status/shape assertions only (no binary tar inspection) — used for the unknown-ids /
     *  empty-list edge cases where only the HTTP status matters. */
    @When("I fetch internal deployment batch for deployment ids {string} using api key {string}")
    public void iFetchInternalDeploymentBatch(String idsCsv, String tokenKey) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(API_KEY_HEADER, resolveIdOrLiteral(tokenKey));
        headers.put(Constants.REQUEST_HEADERS.CONTENT_TYPE, Constants.CONTENT_TYPES.APPLICATION_JSON);
        JSONArray ids = new JSONArray();
        if (idsCsv != null && !idsCsv.isBlank()) {
            for (String rawId : idsCsv.split(",")) {
                ids.put(resolveIdOrLiteral(rawId.trim()));
            }
        }
        String payload = new JSONObject().put("deploymentIds", ids).toString();
        HttpResponse response = Requests.post(Utils.getInternalDeploymentsFetchBatchURL(getBaseUrl()), headers,
                payload, Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Fetch-batch for ONE deployment id, reading the response as raw bytes (a String read would corrupt the
     *  gzip-wrapped tar), unwrapping it, and asserting it is a genuine tar archive. Asserts 200 itself and
     *  stores the unwrapped bytes as ISO-8859-1 "searchable text" (byte-lossless) for a following content
     *  assertion. */
    @When("I fetch internal deployment batch as tar for deployment id {string} using api key {string}")
    public void iFetchInternalDeploymentBatchAsTar(String deploymentIdKey, String tokenKey) throws IOException {
        String deploymentId = resolveIdOrLiteral(deploymentIdKey);
        Map<String, String> headers = new HashMap<>();
        headers.put(API_KEY_HEADER, resolveIdOrLiteral(tokenKey));
        headers.put(Constants.REQUEST_HEADERS.CONTENT_TYPE, Constants.CONTENT_TYPES.APPLICATION_JSON);
        String payload = new JSONObject().put("deploymentIds", new JSONArray().put(deploymentId)).toString();
        SimpleHTTPClient.DownloadResult result = Requests.postToFile(
                Utils.getInternalDeploymentsFetchBatchURL(getBaseUrl()), headers, payload,
                Constants.CONTENT_TYPES.APPLICATION_JSON, ".tar.gz");
        Assert.assertEquals(result.getStatusCode(), 200,
                "fetch-batch should succeed for a listed deployment id " + deploymentId);
        byte[] raw = Files.readAllBytes(result.getFile().toPath());
        byte[] tarBytes = unwrapFetchBatchToTar(raw);
        Assert.assertTrue(containsAscii(tarBytes, "ustar"),
                "fetch-batch should return a tar archive payload after gzip unwrap");
        TestContext.set(FETCH_BATCH_TEXT_KEY, new String(tarBytes, StandardCharsets.ISO_8859_1));
    }

    @Then("The fetched deployment archive should contain {string}")
    public void theFetchedArchiveShouldContain(String expected) {
        String text = TestContext.resolve(FETCH_BATCH_TEXT_KEY).toString();
        String resolvedExpected = Utils.resolveContextPlaceholders(expected);
        Assert.assertTrue(text.contains(resolvedExpected),
                "Fetched deployment archive did not contain expected marker: " + resolvedExpected);
    }

    @When("I retrieve the internal gateway well-known discovery document")
    public void iRetrieveInternalGatewayWellKnown() throws IOException {
        HttpResponse response = Requests.get(Utils.getInternalGatewayWellKnownURL(getBaseUrl()), new HashMap<>());
    }

    /**
     * Unwraps a fetch-batch payload to TAR bytes. The server may return the archive already gzip-wrapped, and
     * the HTTP layer may ALSO gzip Content-Encoding it, so up to two unwrap passes are tried.
     */
    private static byte[] unwrapFetchBatchToTar(byte[] responseBytes) throws IOException {
        byte[] current = responseBytes;
        for (int i = 0; i < 2; i++) {
            if (containsAscii(current, "ustar")) {
                return current;
            }
            try {
                current = gunzip(current);
            } catch (IOException e) {
                break;
            }
        }
        return current;
    }

    private static byte[] gunzip(byte[] compressed) throws IOException {
        try (GZIPInputStream gin = new GZIPInputStream(new ByteArrayInputStream(compressed));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = gin.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static boolean containsAscii(byte[] data, String needle) {
        byte[] n = needle.getBytes(StandardCharsets.US_ASCII);
        outer:
        for (int i = 0; i <= data.length - n.length; i++) {
            for (int j = 0; j < n.length; j++) {
                if (data[i + j] != n[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    // ---- Internal control-plane WebSocket (registration handshake) ----

    /** Per-connection state captured by the WebSocket listener, keyed into {@link TestContext} so later
     *  steps in the same scenario can poll it. {@code nextScanIndex} is the event-scan checkpoint an
     *  either-of-two-types match advances, so a later distinct-type match never re-matches a stale message. */
    private static final class WsHandle {
        volatile WebSocket socket;
        final List<String> messages = new CopyOnWriteArrayList<>();
        volatile boolean open;
        volatile int closeCode = -1;
        volatile String connectFailure;
        int nextScanIndex;
    }

    private WsHandle currentWsHandle() {
        Object handle = TestContext.get(WS_HANDLE_KEY);
        if (!(handle instanceof WsHandle)) {
            throw new IllegalStateException("No internal gateway WebSocket connection attempt recorded yet");
        }
        return (WsHandle) handle;
    }

    @When("I connect to the internal gateway control-plane WebSocket using api key {string}")
    public void iConnectToInternalGatewayWebSocket(String tokenKey) {
        String apiKey = resolveIdOrLiteral(tokenKey);
        String wsUrl = Utils.getInternalGatewayConnectWsURL(getBaseUrl());
        WsHandle handle = new WsHandle();
        try {
            HttpClient client = HttpClient.newBuilder().sslContext(trustAllSslContext()).build();
            WebSocket socket = client.newWebSocketBuilder()
                    .header(API_KEY_HEADER, apiKey)
                    .connectTimeout(Duration.ofSeconds(15))
                    .buildAsync(URI.create(wsUrl), new WebSocket.Listener() {
                        private final StringBuilder parts = new StringBuilder();

                        @Override
                        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                            parts.append(data);
                            if (last) {
                                handle.messages.add(parts.toString());
                                parts.setLength(0);
                            }
                            webSocket.request(1);
                            return null;
                        }

                        @Override
                        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                            handle.open = false;
                            handle.closeCode = statusCode;
                            return null;
                        }

                        @Override
                        public void onError(WebSocket webSocket, Throwable error) {
                            handle.open = false;
                        }
                    }).get(20, TimeUnit.SECONDS);
            handle.socket = socket;
            handle.open = true;
        } catch (Exception connectFailed) {
            handle.open = false;
            handle.connectFailure = rootCauseMessage(connectFailed);
        }
        TestContext.set(WS_HANDLE_KEY, handle);
    }

    @Then("The internal gateway WebSocket connection should be open")
    public void theConnectionShouldBeOpen() {
        WsHandle handle = currentWsHandle();
        Assert.assertTrue(handle.open, "Expected the internal gateway WebSocket connection to be open; "
                + "connect failure=" + handle.connectFailure);
    }

    /**
     * Confirms the connection was rejected with the given close code. The server may either accept the HTTP
     * upgrade then close in {@code @OnOpen} with the code (observed via {@code onClose}), or refuse the
     * upgrade outright (surfaced as a connect failure with no close frame to observe) — both count as
     * rejection, matched by close code or by an "unauthorized/401" signal in the failure message.
     */
    @Then("The internal gateway WebSocket connection should be rejected with close code {int}")
    public void theConnectionShouldBeRejected(int expectedCloseCode) throws InterruptedException {
        WsHandle handle = currentWsHandle();
        long deadline = System.currentTimeMillis() + 15000L;
        while (handle.open && handle.closeCode < 0 && handle.connectFailure == null
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        if (handle.connectFailure != null) {
            String lower = handle.connectFailure.toLowerCase();
            Assert.assertTrue(lower.contains("unauthorized") || lower.contains("401")
                            || handle.connectFailure.contains(String.valueOf(expectedCloseCode)),
                    "Expected an unauthorized WebSocket connect failure, got: " + handle.connectFailure);
            return;
        }
        Assert.assertFalse(handle.open, "Invalid api-key must close the WebSocket session");
        Assert.assertEquals(handle.closeCode, expectedCloseCode,
                "Invalid api-key should close the WebSocket with the expected close code");
    }

    @When("I close the internal gateway WebSocket connection")
    public void iCloseInternalGatewayWebSocket() {
        WsHandle handle = currentWsHandle();
        if (handle.socket != null) {
            handle.socket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
        }
        handle.open = false;
    }

    @Then("I should receive an internal websocket event of type {string} within {int} seconds")
    public void iShouldReceiveWsEventOfType(String eventType, int timeoutSeconds) throws InterruptedException {
        matchWsEvent(List.of(eventType), timeoutSeconds);
    }

    @Then("I should receive an internal websocket event of type {string} or {string} within {int} seconds")
    public void iShouldReceiveWsEventOfEitherType(String eventTypeA, String eventTypeB, int timeoutSeconds)
            throws InterruptedException {
        matchWsEvent(List.of(eventTypeA, eventTypeB), timeoutSeconds);
    }

    /** Scans forward from the handle's checkpoint for the first message whose {@code type} matches, records
     *  its {@code payload} + {@code type} for later field assertions, and advances the checkpoint past it so
     *  a following distinct-type match never re-matches this same message (e.g. create then update). */
    private void matchWsEvent(List<String> candidateTypes, int timeoutSeconds) throws InterruptedException {
        WsHandle handle = currentWsHandle();
        long deadline = System.currentTimeMillis() + Math.max(timeoutSeconds * 1000L, 5000L);
        while (System.currentTimeMillis() < deadline) {
            List<String> messages = handle.messages;
            for (int i = handle.nextScanIndex; i < messages.size(); i++) {
                JSONObject json;
                try {
                    json = new JSONObject(messages.get(i));
                } catch (org.json.JSONException notJson) {
                    continue;   // an unexpected non-JSON frame must not abort the scan
                }
                String type = json.optString("type");
                if (candidateTypes.contains(type)) {
                    handle.nextScanIndex = i + 1;
                    TestContext.set(WS_LAST_EVENT_KEY, json);
                    TestContext.set(WS_LAST_EVENT_TYPE_KEY, type);
                    return;
                }
            }
            Thread.sleep(200);
        }
        Assert.fail("Timed out waiting for an internal websocket event of type " + candidateTypes
                + " after index " + handle.nextScanIndex);
    }

    private JSONObject lastWsEventPayload() {
        Object event = TestContext.resolve(WS_LAST_EVENT_KEY);
        JSONObject payload = ((JSONObject) event).optJSONObject("payload");
        Assert.assertNotNull(payload, "Last internal websocket event had no 'payload' object: " + event);
        return payload;
    }

    @Then("The last internal websocket event field {string} should equal {string}")
    public void theLastWsEventFieldShouldEqual(String field, String expected) {
        JSONObject payload = lastWsEventPayload();
        String resolvedExpected = Utils.resolveContextPlaceholders(expected);
        Assert.assertEquals(payload.optString(field), resolvedExpected,
                "Internal websocket event field '" + field + "' mismatch");
    }

    @Then("The last internal websocket event field {string} should start with {string}")
    public void theLastWsEventFieldShouldStartWith(String field, String prefix) {
        JSONObject payload = lastWsEventPayload();
        Assert.assertTrue(payload.optString(field).startsWith(prefix),
                "Internal websocket event field '" + field + "' expected to start with '" + prefix + "', got: "
                        + payload.optString(field));
    }

    /**
     * The key-name field differs by event type on the runtime: {@code apikey.created} and
     * {@code apikey.revoked} carry it as {@code name}/{@code keyName} respectively, and a same-key-name
     * regenerate can surface as EITHER {@code apikey.updated} (field {@code keyName}) or, on some runtimes,
     * a second {@code apikey.created} (field {@code name}) — this resolves which field to check from the
     * type that actually matched the preceding either-of-two-types step.
     */
    @Then("The last internal websocket event's key-name field should equal {string}")
    public void theLastWsEventKeyNameFieldShouldEqual(String expected) {
        String eventType = TestContext.resolve(WS_LAST_EVENT_TYPE_KEY).toString();
        String field = "apikey.updated".equals(eventType) ? "keyName" : "name";
        theLastWsEventFieldShouldEqual(field, expected);
    }

    private SSLContext trustAllSslContext() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) { }
            public void checkServerTrusted(X509Certificate[] chain, String authType) { }
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        }}, new SecureRandom());
        return sslContext;
    }

    private static String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }
}
