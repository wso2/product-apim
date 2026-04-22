/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.restapi.admin;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.json.JSONObject;
import org.wso2.am.integration.tests.websocket.client.WebSocketClientImpl;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.CreatePlatformGatewayRequestDTO;
import org.wso2.am.integration.clients.admin.api.dto.EnvironmentDTO;
import org.wso2.am.integration.clients.admin.api.dto.EnvironmentListDTO;
import org.wso2.am.integration.clients.admin.api.dto.GatewayListDTO;
import org.wso2.am.integration.clients.admin.api.dto.GatewayResponseWithTokenDTO;
import org.wso2.am.integration.clients.admin.api.dto.PlatformGatewayResponseDTO;
import org.wso2.am.integration.clients.admin.api.dto.UpdatePlatformGatewayRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URI;
import java.util.UUID;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for platform (Universal) gateway admin APIs, environment surfacing,
 * WebSocket connect to internal data plane, and gateway-scoped internal REST (/deployments, /fetch-batch, since).
 * Also covers: unknown environment 404, platform gateways omitted from GET /environments list.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class PlatformGatewayIntegrationTestCase extends APIMIntegrationBaseTest {

    private static final String GATEWAY_TYPE_PLATFORM = "APIPlatform";
    private static final String INTERNAL_DATA_V1 = "https://localhost:9943/internal/data/v1";
    private static final String INTERNAL_GATEWAY_WELL_KNOWN = "https://localhost:9943/internal/gateway/.well-known";
    private static final String WS_GATEWAY_CONNECT_PATH = "/ws/gateways/connect";

    private String lastCreatedGatewayId;
    private String lastRegistrationToken;

    @Factory(dataProvider = "userModeDataProvider")
    public PlatformGatewayIntegrationTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException {
        super.init(userMode);
    }

    @AfterMethod(alwaysRun = true)
    public void cleanupLastGateway() {
        if (StringUtils.isBlank(lastCreatedGatewayId)) {
            return;
        }
        try {
            restAPIAdmin.deletePlatformGateway(lastCreatedGatewayId);
        } catch (ApiException ignored) {
            // Gateway may already be removed by the test.
        } finally {
            lastCreatedGatewayId = null;
            lastRegistrationToken = null;
        }
    }

    private String uniqueGatewayName() {
        return "igw-" + System.currentTimeMillis();
    }

    private CreatePlatformGatewayRequestDTO newCreateRequest(String name) {
        CreatePlatformGatewayRequestDTO dto = new CreatePlatformGatewayRequestDTO();
        dto.setName(name);
        dto.setDisplayName("Integration test gateway");
        dto.setDescription("Created by PlatformGatewayIntegrationTestCase");
        dto.setVhost(java.net.URI.create("https://localhost:9999"));
        return dto;
    }

    private void registerForCleanup(String gatewayId, String token) {
        this.lastCreatedGatewayId = gatewayId;
        this.lastRegistrationToken = token;
    }

    /**
     * Resolves the deploy-target environment view for a platform gateway. {@code GET /environments}
     * intentionally omits platform gateways; {@code GET /environments/{id}} includes them by gateway UUID.
     */
    private EnvironmentDTO findUniversalEnv(String gatewayId) throws ApiException {
        try {
            ApiResponse<EnvironmentDTO> res = restAPIAdmin.getEnvironment(gatewayId);
            if (res.getStatusCode() != HttpStatus.SC_OK || res.getData() == null) {
                return null;
            }
            EnvironmentDTO e = res.getData();
            if (GATEWAY_TYPE_PLATFORM.equals(e.getGatewayType()) && gatewayId.equals(e.getId())) {
                return e;
            }
            return null;
        } catch (ApiException e) {
            if (e.getCode() == HttpStatus.SC_NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    /**
     * Connection state from {@code GET /gateways} ({@link PlatformGatewayResponseDTO#isIsActive()}).
     * Use this instead of parsing {@link EnvironmentDTO#getAdditionalProperties()} / {@code status}: the environment
     * deploy-target DTO often omits or mis-deserializes those fields in the integration client, while the platform
     * gateway API mirrors persisted DB {@code isActive} reliably.
     */
    private Boolean readPlatformGatewayIsActive(String gatewayId) throws ApiException {
        ApiResponse<GatewayListDTO> res = restAPIAdmin.getPlatformGateways();
        if (res.getStatusCode() != HttpStatus.SC_OK || res.getData() == null || res.getData().getList() == null) {
            return null;
        }
        for (PlatformGatewayResponseDTO g : res.getData().getList()) {
            if (gatewayId.equals(g.getId())) {
                return g.isIsActive();
            }
        }
        return null;
    }

    /** Polls {@link #readPlatformGatewayIsActive} until it matches {@code expectConnected} (true = CP marks gateway active). */
    private void awaitUniversalEnvConnected(String gatewayId, boolean expectConnected, long timeoutMs)
            throws Exception {

        long deadline = System.currentTimeMillis() + timeoutMs;
        Boolean last = null;
        while (System.currentTimeMillis() < deadline) {
            last = readPlatformGatewayIsActive(gatewayId);
            if (last != null && last.booleanValue() == expectConnected) {
                return;
            }
            Thread.sleep(500L);
        }
        Assert.fail("Timed out waiting for platform gateway " + gatewayId + " connected=" + expectConnected
                + " (last GET /gateways isActive=" + last + ")");
    }

    /**
     * WebSocket client that trusts all TLS certs (integration only), required for {@code wss://localhost:9943}.
     */
    private WebSocketClient newInternalDataWebSocketClient() {
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setTrustAll(true);
        return new WebSocketClient(sslContextFactory);
    }

    @Test
    public void testPlatformGatewayCrudListUpdateRegenerateAndDelete() throws Exception {
        String name = uniqueGatewayName();
        ApiResponse<GatewayResponseWithTokenDTO> created =
                restAPIAdmin.createPlatformGateway(newCreateRequest(name));
        Assert.assertEquals(created.getStatusCode(), HttpStatus.SC_CREATED);
        GatewayResponseWithTokenDTO body = created.getData();
        Assert.assertNotNull(body.getId());
        Assert.assertTrue(StringUtils.isNotBlank(body.getRegistrationToken()));
        Assert.assertEquals(body.getName(), name);

        registerForCleanup(body.getId(), body.getRegistrationToken());

        ApiResponse<GatewayListDTO> listed = restAPIAdmin.getPlatformGateways();
        Assert.assertEquals(listed.getStatusCode(), HttpStatus.SC_OK);
        boolean seen = false;
        if (listed.getData().getList() != null) {
            for (PlatformGatewayResponseDTO g : listed.getData().getList()) {
                if (name.equals(g.getName())) {
                    seen = true;
                    break;
                }
            }
        }
        Assert.assertTrue(seen, "Created gateway should appear in GET /gateways list");

        UpdatePlatformGatewayRequestDTO update = new UpdatePlatformGatewayRequestDTO();
        update.setName(name);
        update.setVhost(java.net.URI.create("https://localhost:9999"));
        update.setDisplayName("Updated display");
        update.setDescription("updated");
        ApiResponse<PlatformGatewayResponseDTO> put =
                restAPIAdmin.updatePlatformGateway(body.getId(), update);
        Assert.assertEquals(put.getStatusCode(), HttpStatus.SC_OK);
        Assert.assertEquals(put.getData().getDisplayName(), "Updated display");

        String oldToken = body.getRegistrationToken();
        ApiResponse<GatewayResponseWithTokenDTO> regen =
                restAPIAdmin.regeneratePlatformGatewayToken(body.getId());
        Assert.assertEquals(regen.getStatusCode(), HttpStatus.SC_OK);
        Assert.assertTrue(StringUtils.isNotBlank(regen.getData().getRegistrationToken()));
        Assert.assertNotEquals(regen.getData().getRegistrationToken(), oldToken);

        Map<String, String> oldHeaders = new HashMap<>();
        oldHeaders.put("api-key", oldToken);
        HttpResponse oldDep = HTTPSClientUtils.doGet(INTERNAL_DATA_V1 + "/deployments", oldHeaders);
        Assert.assertEquals(oldDep.getResponseCode(), HttpStatus.SC_UNAUTHORIZED,
                "Revoked registration token should not authenticate internal /deployments");

        lastRegistrationToken = regen.getData().getRegistrationToken();

        restAPIAdmin.deletePlatformGateway(body.getId());
        lastCreatedGatewayId = null;
        lastRegistrationToken = null;

        try {
            restAPIAdmin.deletePlatformGateway(body.getId());
            Assert.fail("Expected 404 when deleting removed gateway");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_NOT_FOUND);
        }
    }

    @Test
    public void testDuplicatePlatformGatewayNameReturnsConflict() throws Exception {
        String name = uniqueGatewayName();
        ApiResponse<GatewayResponseWithTokenDTO> first = restAPIAdmin.createPlatformGateway(newCreateRequest(name));
        Assert.assertEquals(first.getStatusCode(), HttpStatus.SC_CREATED);
        registerForCleanup(first.getData().getId(), first.getData().getRegistrationToken());
        try {
            restAPIAdmin.createPlatformGateway(newCreateRequest(name));
            Assert.fail("Expected conflict for duplicate gateway name");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_CONFLICT);
        }
    }

    @Test
    public void testInvalidPlatformGatewayNameReturnsBadRequest() throws Exception {
        CreatePlatformGatewayRequestDTO dto = newCreateRequest("Invalid_Name");
        try {
            restAPIAdmin.createPlatformGateway(dto);
            Assert.fail("Expected validation error for invalid name pattern");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test
    public void testUniversalEnvironmentListsGatewayInactiveBeforeConnect() throws Exception {
        String name = uniqueGatewayName();
        ApiResponse<GatewayResponseWithTokenDTO> created =
                restAPIAdmin.createPlatformGateway(newCreateRequest(name));
        String gatewayId = created.getData().getId();
        registerForCleanup(gatewayId, created.getData().getRegistrationToken());

        EnvironmentDTO env = findUniversalEnv(gatewayId);
        Assert.assertNotNull(env, "Platform gateway should be retrievable as GET /environments/{gatewayId}");
        Assert.assertEquals(env.getGatewayType(), GATEWAY_TYPE_PLATFORM);
        Boolean active = readPlatformGatewayIsActive(gatewayId);
        Assert.assertNotNull(active, "Created gateway should appear in GET /gateways");
        Assert.assertFalse(active, "Gateway should not be active before WebSocket connect (GET /gateways isActive)");
        Assert.assertTrue((env.getVhost() != null)
                        || (env.getVhosts() != null && !env.getVhosts().isEmpty()
                        && StringUtils.isNotBlank(env.getVhosts().get(0).getHost())),
                "Environment should expose gateway host via vhost or vhosts");
    }

    @Test
    public void testWebSocketConnectSetsUniversalEnvironmentActive() throws Exception {
        String name = uniqueGatewayName();
        ApiResponse<GatewayResponseWithTokenDTO> created =
                restAPIAdmin.createPlatformGateway(newCreateRequest(name));
        String gatewayId = created.getData().getId();
        String token = created.getData().getRegistrationToken();
        registerForCleanup(gatewayId, token);

        WebSocketClient client = newInternalDataWebSocketClient();
        WebSocketClientImpl socket = new WebSocketClientImpl();
        client.start();
        try {
            URI wsUri = new URI("wss://localhost:9943/internal/data/v1" + WS_GATEWAY_CONNECT_PATH);
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            request.setHeader("api-key", token);
            Future<Session> future = client.connect(socket, wsUri, request);
            Session session = future.get(15, TimeUnit.SECONDS);
            Assert.assertTrue(session.isOpen(), "WebSocket session should be open after successful connect");
            socket.getLatch().await(5L, TimeUnit.SECONDS);

            awaitUniversalEnvConnected(gatewayId, true, 15000L);

            session.close();
            awaitUniversalEnvConnected(gatewayId, false, 15000L);
        } finally {
            client.stop();
        }
    }

    @Test
    public void testWebSocketRejectedForInvalidRegistrationToken() throws Exception {
        WebSocketClient client = newInternalDataWebSocketClient();
        WebSocketClientImpl socket = new WebSocketClientImpl();
        client.start();
        try {
            URI wsUri = new URI("wss://localhost:9943/internal/data/v1" + WS_GATEWAY_CONNECT_PATH);
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            request.setHeader("api-key", "definitely-not-a-valid-platform-gateway-token");
            Future<Session> future = client.connect(socket, wsUri, request);
            Session session;
            try {
                session = future.get(15, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                // Handshake failed before a session was established.
                return;
            }
            /*
             * JSR-356/Jetty may complete the connect future after the HTTP upgrade even when the server
             * closes in @OnOpen with 4401 (Unauthorized). Reject invalid tokens by asserting the session
             * does not stay open.
             */
            long deadline = System.currentTimeMillis() + 5000L;
            while (session.isOpen() && System.currentTimeMillis() < deadline) {
                Thread.sleep(100L);
            }
            Assert.assertFalse(session.isOpen(),
                    "Invalid api-key must close the WebSocket (server uses close code 4401)");
        } finally {
            client.stop();
        }
    }

    @Test
    public void testInternalDeploymentsGetWithValidApiKey() throws Exception {
        String name = uniqueGatewayName();
        ApiResponse<GatewayResponseWithTokenDTO> created =
                restAPIAdmin.createPlatformGateway(newCreateRequest(name));
        registerForCleanup(created.getData().getId(), created.getData().getRegistrationToken());
        String token = created.getData().getRegistrationToken();

        Map<String, String> headers = new HashMap<>();
        headers.put("api-key", token);
        HttpResponse res = HTTPSClientUtils.doGet(INTERNAL_DATA_V1 + "/deployments", headers);
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_OK);
        JSONObject json = new JSONObject(res.getData());
        Assert.assertTrue(json.has("deployments"), "Response should contain deployments array");
    }

    @Test
    public void testInternalDeploymentsGetWithoutApiKeyReturnsUnauthorized() throws Exception {
        HttpResponse res = HTTPSClientUtils.doGet(INTERNAL_DATA_V1 + "/deployments", Collections.emptyMap());
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    public void testInternalGatewayWellKnownReturnsDiscoveryPayload() throws Exception {
        HttpResponse res = HTTPSClientUtils.doGet(INTERNAL_GATEWAY_WELL_KNOWN, Collections.emptyMap());
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_OK);

        JSONObject json = new JSONObject(res.getData());
        Assert.assertEquals(json.optString("gatewayPath"), "internal/data/v1",
                "Well-known should expose internal REST base path without /ws suffix");
        JSONObject controlPlane = json.optJSONObject("controlPlane");
        Assert.assertNotNull(controlPlane, "Well-known payload should include controlPlane metadata");
        Assert.assertEquals(controlPlane.optString("type"), "APIM");
        Assert.assertTrue(StringUtils.isNotBlank(controlPlane.optString("version")),
                "Well-known control plane version should be present");
    }

    @Test
    public void testGetEnvironmentByUnknownIdReturnsNotFound() throws Exception {
        try {
            restAPIAdmin.getEnvironment(UUID.randomUUID().toString());
            Assert.fail("Expected ApiException for non-existent environment id");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_NOT_FOUND);
        }
    }

    @Test
    public void testEnvironmentsListExcludesRegisteredPlatformGatewayByName() throws Exception {
        String name = uniqueGatewayName();
        ApiResponse<GatewayResponseWithTokenDTO> created =
                restAPIAdmin.createPlatformGateway(newCreateRequest(name));
        Assert.assertEquals(created.getStatusCode(), HttpStatus.SC_CREATED);
        registerForCleanup(created.getData().getId(), created.getData().getRegistrationToken());

        ApiResponse<EnvironmentListDTO> listRes = restAPIAdmin.getEnvironments();
        Assert.assertEquals(listRes.getStatusCode(), HttpStatus.SC_OK);
        if (listRes.getData() != null && listRes.getData().getList() != null) {
            for (EnvironmentDTO e : listRes.getData().getList()) {
                Assert.assertNotEquals(name, e.getName(),
                        "GET /environments must not expose synthetic platform gateway environments by name");
            }
        }
    }

    @Test
    public void testInternalDeploymentsGetWithSinceQueryAccepted() throws Exception {
        ApiResponse<GatewayResponseWithTokenDTO> created =
                restAPIAdmin.createPlatformGateway(newCreateRequest(uniqueGatewayName()));
        registerForCleanup(created.getData().getId(), created.getData().getRegistrationToken());
        String token = created.getData().getRegistrationToken();

        Map<String, String> headers = new HashMap<>();
        headers.put("api-key", token);
        HttpResponse res = HTTPSClientUtils.doGet(
                INTERNAL_DATA_V1 + "/deployments?since=1970-01-01T00:00:00Z", headers);
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_OK,
                "GET /deployments should accept optional ISO8601 since filter");
        JSONObject json = new JSONObject(res.getData());
        Assert.assertTrue(json.has("deployments"));
    }

    @Test
    public void testInternalFetchBatchWithUnknownDeploymentIdsReturnsOk() throws Exception {
        ApiResponse<GatewayResponseWithTokenDTO> created =
                restAPIAdmin.createPlatformGateway(newCreateRequest(uniqueGatewayName()));
        registerForCleanup(created.getData().getId(), created.getData().getRegistrationToken());
        String token = created.getData().getRegistrationToken();

        Map<String, String> headers = new HashMap<>();
        headers.put("api-key", token);
        headers.put("Content-Type", "application/json");
        String payload =
                "{\"deploymentIds\":[\"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee\",\"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb\"]}";
        HttpResponse res = HTTPSClientUtils.doPost(INTERNAL_DATA_V1 + "/deployments/fetch-batch", headers, payload);
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_OK,
                "fetch-batch skips unknown ids and still returns an archive envelope");
        Assert.assertNotNull(res.getData());
    }

    @Test
    public void testInternalDeploymentsFetchBatchWithEmptyList() throws Exception {
        String name = uniqueGatewayName();
        ApiResponse<GatewayResponseWithTokenDTO> created =
                restAPIAdmin.createPlatformGateway(newCreateRequest(name));
        registerForCleanup(created.getData().getId(), created.getData().getRegistrationToken());
        String token = created.getData().getRegistrationToken();

        Map<String, String> headers = new HashMap<>();
        headers.put("api-key", token);
        headers.put("Content-Type", "application/json");
        String payload = "{\"deploymentIds\":[]}";
        HttpResponse res = HTTPSClientUtils.doPost(INTERNAL_DATA_V1 + "/deployments/fetch-batch", headers, payload);
        Assert.assertEquals(res.getResponseCode(), HttpStatus.SC_BAD_REQUEST,
                "Empty deploymentIds should be rejected for fetch-batch");
    }
}
