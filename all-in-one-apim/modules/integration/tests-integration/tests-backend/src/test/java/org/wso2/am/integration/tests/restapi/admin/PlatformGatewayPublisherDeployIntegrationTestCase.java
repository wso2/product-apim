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

import org.apache.http.HttpStatus;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.CreatePlatformGatewayRequestDTO;
import org.wso2.am.integration.clients.admin.api.dto.GatewayResponseWithTokenDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIKeyInfoDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIKeyListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionDeployUndeployRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * End-to-end checks for Publisher revision deploy to a Universal (platform) gateway by environment
 * name, and visibility of that deployment on internal gateway sync APIs ({@code GET /deployments},
 * {@code POST /deployments/fetch-batch}).
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class PlatformGatewayPublisherDeployIntegrationTestCase extends APIMIntegrationBaseTest {

    private static final String INTERNAL_DATA_V1 = "https://localhost:9943/internal/data/v1";
    /** Expected in generated platform gateway API YAML (see PlatformGatewayAPIYamlConverter in carbon-apimgt). */
    private static final String PLATFORM_GATEWAY_YAML_API_VERSION = "gateway.api-platform.wso2.com";
    private static final long DEPLOYMENT_SYNC_TIMEOUT_MS = 120_000L;
    private static final long POLL_INTERVAL_MS = 500L;

    private String apiEndPointUrl;

    @WebSocket
    public static class InternalEventCollectorWebSocket {
        private final List<String> messages = new CopyOnWriteArrayList<>();
        private volatile Session session;

        @OnWebSocketConnect
        public void onConnect(Session connectedSession) {
            this.session = connectedSession;
        }

        @OnWebSocketMessage
        public void onText(Session ignored, String message) {
            messages.add(message);
        }

        public boolean isOpen() {
            return session != null && session.isOpen();
        }

        public List<String> getMessages() {
            return messages;
        }
    }

    @Factory(dataProvider = "userModeDataProvider")
    public PlatformGatewayPublisherDeployIntegrationTestCase(TestUserMode userMode) {
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
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + "jaxrs_basic/services/customers/customerservice/";
    }

    private String uniqueGatewayName() {
        return "igw-" + System.currentTimeMillis();
    }

    private static String awaitWebSocketEventType(InternalEventCollectorWebSocket collector, String eventType,
                                                  long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            for (String message : collector.getMessages()) {
                JSONObject json = new JSONObject(message);
                if (eventType.equals(json.optString("type"))) {
                    return message;
                }
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        Assert.fail("Timed out waiting for websocket event type: " + eventType);
        return null;
    }

    private static String awaitWebSocketEventTypeAfterIndex(InternalEventCollectorWebSocket collector, String eventType,
                                                            int startIndex, long timeoutMs)
            throws Exception {

        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            List<String> messages = collector.getMessages();
            for (int i = Math.max(0, startIndex); i < messages.size(); i++) {
                String message = messages.get(i);
                JSONObject json = new JSONObject(message);
                if (eventType.equals(json.optString("type"))) {
                    return message;
                }
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        Assert.fail("Timed out waiting for websocket event type: " + eventType + " after index " + startIndex);
        return null;
    }

    private static String awaitWebSocketAnyEventTypeAfterIndex(InternalEventCollectorWebSocket collector,
                                                               List<String> eventTypes, int startIndex,
                                                               long timeoutMs) throws Exception {

        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            List<String> messages = collector.getMessages();
            for (int i = Math.max(0, startIndex); i < messages.size(); i++) {
                String message = messages.get(i);
                JSONObject json = new JSONObject(message);
                if (eventTypes.contains(json.optString("type"))) {
                    return message;
                }
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        Assert.fail("Timed out waiting for websocket event type in " + eventTypes + " after index " + startIndex);
        return null;
    }

    private String findKeyUuidByName(String applicationId, String keyType, String keyName) throws Exception {
        APIKeyListDTO listDTO = restAPIStore.getAPIKeys(applicationId, keyType);
        if (listDTO == null || listDTO.getList() == null) {
            return null;
        }
        for (APIKeyInfoDTO info : listDTO.getList()) {
            if (info != null && keyName.equals(info.getKeyName())) {
                return info.getKeyUUID();
            }
        }
        return null;
    }

    private CreatePlatformGatewayRequestDTO newCreateRequest(String name) {
        CreatePlatformGatewayRequestDTO dto = new CreatePlatformGatewayRequestDTO();
        dto.setName(name);
        dto.setDisplayName("Publisher deploy integration gateway");
        dto.setDescription("PlatformGatewayPublisherDeployIntegrationTestCase");
        dto.setVhost(URI.create("https://localhost:9999"));
        return dto;
    }

    private String findDeploymentIdForApi(String registrationToken, String apiUuid) throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("api-key", registrationToken);
        HttpResponse res = HTTPSClientUtils.doGet(INTERNAL_DATA_V1 + "/deployments", headers);
        if (res.getResponseCode() != HttpStatus.SC_OK) {
            return null;
        }
        JSONObject json = new JSONObject(res.getData());
        JSONArray arr = json.optJSONArray("deployments");
        if (arr == null) {
            return null;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject d = arr.getJSONObject(i);
            if (apiUuid.equals(d.optString("artifactId"))) {
                String id = d.optString("deploymentId");
                return id.isEmpty() ? null : id;
            }
        }
        return null;
    }

    private String awaitDeploymentIdForApi(String registrationToken, String apiUuid, long timeoutMs)
            throws Exception {

        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            String id = findDeploymentIdForApi(registrationToken, apiUuid);
            if (id != null) {
                return id;
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        Assert.fail("Timed out waiting for internal /deployments to list API " + apiUuid);
        return null;
    }

    private void awaitDeploymentAbsentForApi(String registrationToken, String apiUuid, long timeoutMs)
            throws Exception {

        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (findDeploymentIdForApi(registrationToken, apiUuid) == null) {
                return;
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        Assert.fail("Timed out waiting for internal /deployments to drop API " + apiUuid);
    }

    private void undeployRevisionFromUniversal(String apiId, String revisionUUID, String gatewayName)
            throws Exception {

        List<APIRevisionDeployUndeployRequest> list = new ArrayList<>();
        APIRevisionDeployUndeployRequest u = new APIRevisionDeployUndeployRequest();
        u.setName(gatewayName);
        u.setVhost(null);
        u.setDisplayOnDevportal(true);
        list.add(u);
        HttpResponse r = restAPIPublisher.undeployAPIRevision(apiId, revisionUUID, list);
        Assert.assertEquals(r.getResponseCode(), HttpStatus.SC_CREATED,
                "Undeploy failed: " + r.getData());
    }

    private void cleanupApiAndGateway(String apiId, String revisionUUID, String gatewayName, String gatewayId) {
        if (apiId != null && revisionUUID != null && gatewayName != null) {
            try {
                undeployRevisionFromUniversal(apiId, revisionUUID, gatewayName);
            } catch (Exception ignored) {
            }
            try {
                restAPIPublisher.deleteAPIRevision(apiId, revisionUUID);
            } catch (Exception ignored) {
            }
        }
        if (apiId != null) {
            try {
                restAPIPublisher.deleteAPI(apiId);
            } catch (Exception ignored) {
            }
        }
        if (gatewayId != null) {
            try {
                restAPIAdmin.deletePlatformGateway(gatewayId);
            } catch (ApiException ignored) {
            }
        }
    }

    @Test
    public void testPublisherDeployToUniversalSyncsInternalDeploymentsAndFetchBatch() throws Exception {
        String gatewayName = uniqueGatewayName();
        ApiResponse<GatewayResponseWithTokenDTO> created =
                restAPIAdmin.createPlatformGateway(newCreateRequest(gatewayName));
        Assert.assertEquals(created.getStatusCode(), HttpStatus.SC_CREATED);
        String gatewayId = created.getData().getId();
        String token = created.getData().getRegistrationToken();

        String apiId = null;
        String revisionUUID = null;
        try {
            String suffix = String.valueOf(System.currentTimeMillis());
            APIRequest apiRequest = new APIRequest("PgPublisherDeployAPI_" + suffix, "pgpubrev" + suffix,
                    new URL(apiEndPointUrl));
            apiRequest.setVersion("1.0.0");
            apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
            apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

            HttpResponse addApiRes = restAPIPublisher.addAPI(apiRequest);
            Assert.assertEquals(addApiRes.getResponseCode(), HttpStatus.SC_CREATED, addApiRes.getData());
            apiId = addApiRes.getData();

            APIRevisionRequest revReq = new APIRevisionRequest();
            revReq.setApiUUID(apiId);
            revReq.setDescription("platform universal deploy");
            HttpResponse revRes = restAPIPublisher.addAPIRevision(revReq);
            Assert.assertEquals(revRes.getResponseCode(), HttpStatus.SC_CREATED, revRes.getData());
            revisionUUID = new JSONObject(revRes.getData()).getString("id");

            List<APIRevisionDeployUndeployRequest> deployList = new ArrayList<>();
            APIRevisionDeployUndeployRequest d = new APIRevisionDeployUndeployRequest();
            d.setName(gatewayName);
            d.setVhost("localhost");
            d.setDisplayOnDevportal(true);
            deployList.add(d);
            HttpResponse depRes = restAPIPublisher.deployAPIRevision(apiId, revisionUUID, deployList, "API");
            Assert.assertEquals(depRes.getResponseCode(), HttpStatus.SC_CREATED, depRes.getData());

            String deploymentId = awaitDeploymentIdForApi(token, apiId, DEPLOYMENT_SYNC_TIMEOUT_MS);
            Assert.assertNotNull(deploymentId);

            Map<String, String> headers = new HashMap<>();
            headers.put("api-key", token);
            headers.put("Content-Type", "application/json");
            String payload = "{\"deploymentIds\":[\"" + deploymentId + "\"]}";
            HttpResponse batch =
                    HTTPSClientUtils.doPost(INTERNAL_DATA_V1 + "/deployments/fetch-batch", headers, payload);
            Assert.assertEquals(batch.getResponseCode(), HttpStatus.SC_OK,
                    "fetch-batch should succeed for a listed deployment id");
            String body = batch.getData();
            Assert.assertNotNull(body);
            // Body is read as UTF-8 text by HTTPSClientUtils; tar entries still contain ASCII ustar magic.
            Assert.assertTrue(body.contains("ustar"), "fetch-batch should return a tar archive payload");
        } finally {
            cleanupApiAndGateway(apiId, revisionUUID, gatewayName, gatewayId);
        }
    }

    @Test
    public void testDeleteUniversalGatewayWithRevisionDeployedReturnsConflict() throws Exception {
        String gatewayName = uniqueGatewayName();
        ApiResponse<GatewayResponseWithTokenDTO> created =
                restAPIAdmin.createPlatformGateway(newCreateRequest(gatewayName));
        Assert.assertEquals(created.getStatusCode(), HttpStatus.SC_CREATED);
        String gatewayId = created.getData().getId();

        String apiId = null;
        String revisionUUID = null;
        try {
            String suffix = String.valueOf(System.currentTimeMillis());
            APIRequest apiRequest = new APIRequest("PgGatewayDeleteAPI_" + suffix, "pgdelgw" + suffix,
                    new URL(apiEndPointUrl));
            apiRequest.setVersion("1.0.0");
            apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
            apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

            HttpResponse addApiRes = restAPIPublisher.addAPI(apiRequest);
            Assert.assertEquals(addApiRes.getResponseCode(), HttpStatus.SC_CREATED, addApiRes.getData());
            apiId = addApiRes.getData();

            APIRevisionRequest revReq = new APIRevisionRequest();
            revReq.setApiUUID(apiId);
            revReq.setDescription("block gateway delete");
            HttpResponse revRes = restAPIPublisher.addAPIRevision(revReq);
            Assert.assertEquals(revRes.getResponseCode(), HttpStatus.SC_CREATED, revRes.getData());
            revisionUUID = new JSONObject(revRes.getData()).getString("id");

            List<APIRevisionDeployUndeployRequest> deployList = new ArrayList<>();
            APIRevisionDeployUndeployRequest d = new APIRevisionDeployUndeployRequest();
            d.setName(gatewayName);
            d.setVhost("localhost");
            d.setDisplayOnDevportal(true);
            deployList.add(d);
            HttpResponse depRes = restAPIPublisher.deployAPIRevision(apiId, revisionUUID, deployList, "API");
            Assert.assertEquals(depRes.getResponseCode(), HttpStatus.SC_CREATED, depRes.getData());

            try {
                restAPIAdmin.deletePlatformGateway(gatewayId);
                Assert.fail("Expected conflict when deleting gateway with deployed revisions");
            } catch (ApiException e) {
                Assert.assertEquals(e.getCode(), HttpStatus.SC_CONFLICT,
                        "Deleting gateway with active revision deployments should return 409");
            }
        } finally {
            cleanupApiAndGateway(apiId, revisionUUID, gatewayName, gatewayId);
        }
    }

    @Test
    public void testUndeployRemovesApiFromInternalDeploymentsList() throws Exception {
        String gatewayName = uniqueGatewayName();
        ApiResponse<GatewayResponseWithTokenDTO> created =
                restAPIAdmin.createPlatformGateway(newCreateRequest(gatewayName));
        Assert.assertEquals(created.getStatusCode(), HttpStatus.SC_CREATED);
        String gatewayId = created.getData().getId();
        String token = created.getData().getRegistrationToken();

        String apiId = null;
        String revisionUUID = null;
        try {
            String suffix = String.valueOf(System.currentTimeMillis());
            APIRequest apiRequest = new APIRequest("PgUndeployAPI_" + suffix, "pgundeploy" + suffix,
                    new URL(apiEndPointUrl));
            apiRequest.setVersion("1.0.0");
            apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
            apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

            HttpResponse addApiRes = restAPIPublisher.addAPI(apiRequest);
            Assert.assertEquals(addApiRes.getResponseCode(), HttpStatus.SC_CREATED, addApiRes.getData());
            apiId = addApiRes.getData();

            APIRevisionRequest revReq = new APIRevisionRequest();
            revReq.setApiUUID(apiId);
            revReq.setDescription("undeploy removes internal deployment row");
            HttpResponse revRes = restAPIPublisher.addAPIRevision(revReq);
            Assert.assertEquals(revRes.getResponseCode(), HttpStatus.SC_CREATED, revRes.getData());
            revisionUUID = new JSONObject(revRes.getData()).getString("id");

            List<APIRevisionDeployUndeployRequest> deployList = new ArrayList<>();
            APIRevisionDeployUndeployRequest d = new APIRevisionDeployUndeployRequest();
            d.setName(gatewayName);
            d.setVhost("localhost");
            d.setDisplayOnDevportal(true);
            deployList.add(d);
            HttpResponse depRes = restAPIPublisher.deployAPIRevision(apiId, revisionUUID, deployList, "API");
            Assert.assertEquals(depRes.getResponseCode(), HttpStatus.SC_CREATED, depRes.getData());

            awaitDeploymentIdForApi(token, apiId, DEPLOYMENT_SYNC_TIMEOUT_MS);

            undeployRevisionFromUniversal(apiId, revisionUUID, gatewayName);

            awaitDeploymentAbsentForApi(token, apiId, DEPLOYMENT_SYNC_TIMEOUT_MS);
        } finally {
            cleanupApiAndGateway(apiId, revisionUUID, gatewayName, gatewayId);
        }
    }

    @Test
    public void testFetchBatchTarContainsPlatformGatewayYamlMarker() throws Exception {
        String gatewayName = uniqueGatewayName();
        ApiResponse<GatewayResponseWithTokenDTO> created =
                restAPIAdmin.createPlatformGateway(newCreateRequest(gatewayName));
        Assert.assertEquals(created.getStatusCode(), HttpStatus.SC_CREATED);
        String gatewayId = created.getData().getId();
        String token = created.getData().getRegistrationToken();

        String apiId = null;
        String revisionUUID = null;
        try {
            String suffix = String.valueOf(System.currentTimeMillis());
            APIRequest apiRequest = new APIRequest("PgYamlMarkerAPI_" + suffix, "pgyamlmk" + suffix,
                    new URL(apiEndPointUrl));
            apiRequest.setVersion("1.0.0");
            apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
            apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

            HttpResponse addApiRes = restAPIPublisher.addAPI(apiRequest);
            Assert.assertEquals(addApiRes.getResponseCode(), HttpStatus.SC_CREATED, addApiRes.getData());
            apiId = addApiRes.getData();

            APIRevisionRequest revReq = new APIRevisionRequest();
            revReq.setApiUUID(apiId);
            revReq.setDescription("artifact yaml marker");
            HttpResponse revRes = restAPIPublisher.addAPIRevision(revReq);
            Assert.assertEquals(revRes.getResponseCode(), HttpStatus.SC_CREATED, revRes.getData());
            revisionUUID = new JSONObject(revRes.getData()).getString("id");

            List<APIRevisionDeployUndeployRequest> deployList = new ArrayList<>();
            APIRevisionDeployUndeployRequest d = new APIRevisionDeployUndeployRequest();
            d.setName(gatewayName);
            d.setVhost("localhost");
            d.setDisplayOnDevportal(true);
            deployList.add(d);
            HttpResponse depRes = restAPIPublisher.deployAPIRevision(apiId, revisionUUID, deployList, "API");
            Assert.assertEquals(depRes.getResponseCode(), HttpStatus.SC_CREATED, depRes.getData());

            String deploymentId = awaitDeploymentIdForApi(token, apiId, DEPLOYMENT_SYNC_TIMEOUT_MS);
            Assert.assertNotNull(deploymentId);

            Map<String, String> headers = new HashMap<>();
            headers.put("api-key", token);
            headers.put("Content-Type", "application/json");
            String payload = "{\"deploymentIds\":[\"" + deploymentId + "\"]}";
            HttpResponse batch =
                    HTTPSClientUtils.doPost(INTERNAL_DATA_V1 + "/deployments/fetch-batch", headers, payload);
            Assert.assertEquals(batch.getResponseCode(), HttpStatus.SC_OK, batch.getData());
            String body = batch.getData();
            Assert.assertNotNull(body);
            Assert.assertTrue(body.contains("ustar"), "fetch-batch should return a tar archive payload");
            Assert.assertTrue(body.contains(PLATFORM_GATEWAY_YAML_API_VERSION),
                    "platform gateway artifact YAML should declare the api-platform RestApi apiVersion");
        } finally {
            cleanupApiAndGateway(apiId, revisionUUID, gatewayName, gatewayId);
        }
    }

    @Test
    public void testApiKeyLifecycleEventsAreBroadcastOverInternalWebSocket() throws Exception {
        String gatewayName = uniqueGatewayName();
        ApiResponse<GatewayResponseWithTokenDTO> created =
                restAPIAdmin.createPlatformGateway(newCreateRequest(gatewayName));
        Assert.assertEquals(created.getStatusCode(), HttpStatus.SC_CREATED);
        String gatewayId = created.getData().getId();
        String registrationToken = created.getData().getRegistrationToken();

        String apiId = null;
        String revisionUUID = null;
        ApplicationDTO app = null;
        SubscriptionDTO subscription = null;

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setTrustAll(true);
        WebSocketClient client = new WebSocketClient(sslContextFactory);
        InternalEventCollectorWebSocket collector = new InternalEventCollectorWebSocket();

        try {
            client.start();
            URI wsUri = new URI("wss://localhost:9943/internal/data/v1/ws/gateways/connect");
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            request.setHeader("api-key", registrationToken);
            Future<Session> future = client.connect(collector, wsUri, request);
            Session session = future.get(15, TimeUnit.SECONDS);
            Assert.assertTrue(session.isOpen(), "Gateway websocket should connect using registration token");
            Assert.assertTrue(collector.isOpen(), "Collector socket should be open");

            String suffix = String.valueOf(System.currentTimeMillis());
            APIRequest apiRequest = new APIRequest("PgApiKeyEventAPI_" + suffix, "pgapikeyevt" + suffix,
                    new URL(apiEndPointUrl));
            apiRequest.setVersion("1.0.0");
            apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
            apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

            HttpResponse addApiRes = restAPIPublisher.addAPI(apiRequest);
            Assert.assertEquals(addApiRes.getResponseCode(), HttpStatus.SC_CREATED, addApiRes.getData());
            apiId = addApiRes.getData();
            HttpResponse publishRes = restAPIPublisher.changeAPILifeCycleStatusToPublish(apiId, false);
            Assert.assertNotNull(publishRes, "Publishing API should succeed before creating subscriptions");
            Assert.assertEquals(publishRes.getResponseCode(), HttpStatus.SC_OK);

            APIRevisionRequest revReq = new APIRevisionRequest();
            revReq.setApiUUID(apiId);
            revReq.setDescription("apikey websocket event validation");
            HttpResponse revRes = restAPIPublisher.addAPIRevision(revReq);
            Assert.assertEquals(revRes.getResponseCode(), HttpStatus.SC_CREATED, revRes.getData());
            revisionUUID = new JSONObject(revRes.getData()).getString("id");

            List<APIRevisionDeployUndeployRequest> deployList = new ArrayList<>();
            APIRevisionDeployUndeployRequest d = new APIRevisionDeployUndeployRequest();
            d.setName(gatewayName);
            d.setVhost("localhost");
            d.setDisplayOnDevportal(true);
            deployList.add(d);
            HttpResponse depRes = restAPIPublisher.deployAPIRevision(apiId, revisionUUID, deployList, "API");
            Assert.assertEquals(depRes.getResponseCode(), HttpStatus.SC_CREATED, depRes.getData());
            awaitDeploymentIdForApi(registrationToken, apiId, DEPLOYMENT_SYNC_TIMEOUT_MS);

            app = restAPIStore.addApplication("PgApiKeyEventApp_" + suffix, "Unlimited", null,
                    "App for api-key websocket event tests");
            Assert.assertNotNull(app);
            subscription = restAPIStore.subscribeToAPI(apiId, app.getApplicationId(), "Unlimited");
            Assert.assertNotNull(subscription);

            String keyName = "pg-key-" + suffix;
            APIKeyDTO createdKey = restAPIStore.generateAPIKeys(app.getApplicationId(), "PRODUCTION", 3600,
                    null, null, keyName);
            Assert.assertNotNull(createdKey);
            Assert.assertNotNull(createdKey.getApikey());

            String createdMessage = awaitWebSocketEventType(collector, "apikey.created", 30_000L);
            JSONObject createdEvent = new JSONObject(createdMessage);
            Assert.assertEquals(createdEvent.optString("type"), "apikey.created");
            JSONObject createdPayload = createdEvent.optJSONObject("payload");
            Assert.assertNotNull(createdPayload);
            Assert.assertEquals(createdPayload.optString("apiId"), apiId);
            Assert.assertEquals(createdPayload.optString("name"), keyName);
            Assert.assertTrue(createdPayload.optString("maskedApiKey").startsWith("****"));

            int beforeUpdateMessageCount = collector.getMessages().size();
            APIKeyDTO regeneratedKey = restAPIStore.generateAPIKeys(app.getApplicationId(), "PRODUCTION", 3600,
                    null, null, keyName);
            Assert.assertNotNull(regeneratedKey);
            Assert.assertNotNull(regeneratedKey.getApikey());
            Assert.assertNotEquals(regeneratedKey.getApikey(), createdKey.getApikey(),
                    "Regenerate should issue a new opaque API key");

            // Current runtime can emit apikey.updated or apikey.created for same key-name re-generate path.
            String updatedMessage = awaitWebSocketAnyEventTypeAfterIndex(collector,
                    java.util.Arrays.asList("apikey.updated", "apikey.created"), beforeUpdateMessageCount, 30_000L);
            JSONObject updatedEvent = new JSONObject(updatedMessage);
            String updateType = updatedEvent.optString("type");
            Assert.assertTrue("apikey.updated".equals(updateType) || "apikey.created".equals(updateType),
                    "Expected update-like key event type but received: " + updateType);
            JSONObject updatedPayload = updatedEvent.optJSONObject("payload");
            Assert.assertNotNull(updatedPayload);
            Assert.assertEquals(updatedPayload.optString("apiId"), apiId);
            String keyField = "apikey.updated".equals(updateType) ? "keyName" : "name";
            Assert.assertEquals(updatedPayload.optString(keyField), keyName);
            Assert.assertTrue(updatedPayload.optString("maskedApiKey").startsWith("****"));

            String keyUuid = findKeyUuidByName(app.getApplicationId(), "PRODUCTION", keyName);
            if (keyUuid != null) {
                restAPIStore.revokeAPIKeyByKeyUUID(app.getApplicationId(), "PRODUCTION", keyUuid);
            } else {
                // Fallback for runtimes that do not return key UUIDs in list response.
                restAPIStore.revokeAPIKey(app.getApplicationId(), regeneratedKey.getApikey());
            }

            String revokedMessage = awaitWebSocketEventType(collector, "apikey.revoked", 30_000L);
            JSONObject revokedEvent = new JSONObject(revokedMessage);
            Assert.assertEquals(revokedEvent.optString("type"), "apikey.revoked");
            JSONObject revokedPayload = revokedEvent.optJSONObject("payload");
            Assert.assertNotNull(revokedPayload);
            Assert.assertEquals(revokedPayload.optString("apiId"), apiId);
            Assert.assertEquals(revokedPayload.optString("keyName"), keyName);
        } finally {
            if (collector.isOpen()) {
                try {
                    collector.session.close();
                } catch (Exception ignored) {
                }
            }
            try {
                client.stop();
            } catch (Exception ignored) {
            }
            if (subscription != null) {
                try {
                    restAPIStore.removeSubscription(subscription);
                } catch (Exception ignored) {
                }
            }
            if (app != null) {
                try {
                    restAPIStore.deleteApplication(app.getApplicationId());
                } catch (Exception ignored) {
                }
            }
            cleanupApiAndGateway(apiId, revisionUUID, gatewayName, gatewayId);
        }
    }
}
