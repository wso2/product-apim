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
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Integration tests for platform (Universal) gateway admin APIs, environment surfacing,
 * WebSocket connect to internal data plane, and gateway-scoped internal REST (/deployments).
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class PlatformGatewayIntegrationTestCase extends APIMIntegrationBaseTest {

    private static final String GATEWAY_TYPE_UNIVERSAL = "Universal";
    private static final String INTERNAL_DATA_V1 = "https://localhost:9943/internal/data/v1";
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
    public void init() throws Exception {
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

    private EnvironmentDTO findUniversalEnv(String gatewayId) throws ApiException {
        ApiResponse<EnvironmentListDTO> envs = restAPIAdmin.getEnvironments();
        List<EnvironmentDTO> list = envs.getData().getList();
        if (list == null) {
            return null;
        }
        for (EnvironmentDTO e : list) {
            if (GATEWAY_TYPE_UNIVERSAL.equals(e.getGatewayType()) && gatewayId.equals(e.getId())) {
                return e;
            }
        }
        return null;
    }

    private void awaitUniversalEnvStatus(String gatewayId, EnvironmentDTO.StatusEnum expected, long timeoutMs)
            throws Exception {

        long deadline = System.currentTimeMillis() + timeoutMs;
        EnvironmentDTO found = null;
        while (System.currentTimeMillis() < deadline) {
            found = findUniversalEnv(gatewayId);
            if (found != null && expected.equals(found.getStatus())) {
                return;
            }
            Thread.sleep(500L);
        }
        Assert.fail("Timed out waiting for Universal environment " + gatewayId + " status " + expected
                + (found == null ? " (not found)" : " (last status=" + found.getStatus() + ")"));
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
        Assert.assertNotNull(env, "Platform gateway should appear in GET /environments");
        Assert.assertEquals(env.getGatewayType(), GATEWAY_TYPE_UNIVERSAL);
        Assert.assertEquals(env.getStatus(), EnvironmentDTO.StatusEnum.INACTIVE);
        Assert.assertNotNull(env.getVhost());
    }

    @Test
    public void testWebSocketConnectSetsUniversalEnvironmentActive() throws Exception {
        String name = uniqueGatewayName();
        ApiResponse<GatewayResponseWithTokenDTO> created =
                restAPIAdmin.createPlatformGateway(newCreateRequest(name));
        String gatewayId = created.getData().getId();
        String token = created.getData().getRegistrationToken();
        registerForCleanup(gatewayId, token);

        WebSocketClient client = new WebSocketClient();
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

            awaitUniversalEnvStatus(gatewayId, EnvironmentDTO.StatusEnum.ACTIVE, 15000L);

            session.close();
            awaitUniversalEnvStatus(gatewayId, EnvironmentDTO.StatusEnum.INACTIVE, 15000L);
        } finally {
            client.stop();
        }
    }

    @Test
    public void testWebSocketRejectedForInvalidRegistrationToken() throws Exception {
        WebSocketClient client = new WebSocketClient();
        WebSocketClientImpl socket = new WebSocketClientImpl();
        client.start();
        try {
            URI wsUri = new URI("wss://localhost:9943/internal/data/v1" + WS_GATEWAY_CONNECT_PATH);
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            request.setHeader("api-key", "definitely-not-a-valid-platform-gateway-token");
            Future<Session> future = client.connect(socket, wsUri, request);
            try {
                future.get(15, TimeUnit.SECONDS);
                Assert.fail("Connect should fail for invalid api-key");
            } catch (ExecutionException | TimeoutException ok) {
                // Jetty fails the upgrade when the server closes with 4401.
            }
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
