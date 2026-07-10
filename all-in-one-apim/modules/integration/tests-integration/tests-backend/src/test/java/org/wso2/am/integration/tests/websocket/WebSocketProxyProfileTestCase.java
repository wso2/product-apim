/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.wso2.am.integration.tests.websocket;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.websocket.client.WebSocketClientImpl;
import org.wso2.am.integration.tests.websocket.server.TestHttpConnectProxy;
import org.wso2.am.integration.tests.websocket.server.WebSocketServerImpl;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Integration tests for WS proxy profile support (ws.proxyProfiles in axis2.xml).
 *
 * Test design:
 *   ONE server restart in @BeforeClass. All scenarios run under a single config.
 *   Four proxy profiles are loaded at startup:
 *     1. Anonymous proxy — target_hosts: ["proxied\\.ws\\.local", "127\\.0\\.0\\.1"]
 *                          bypass_hosts: ["127\\.0\\.0\\.1"]
 *     2. Authenticated proxy — target_hosts: ["auth\\.proxied\\.ws\\.local"]
 *                              proxy_username/proxy_password: correct credentials
 *     3. Wrong-credentials — target_hosts: ["wrong-auth\\.ws\\.local"]
 *                            points to authProxy port but with wrong credentials
 *     4. Catch-all — target_hosts: ["*"]
 *                    bypass_hosts: ["localhost"]
 *
 *   Six APIs are created (one per routing scenario) and subscribed to a shared application.
 *
 *   Note: Tests that verify actual proxy routing (CONNECT count checks) require the gateway to
 *   connect to a backend hostname that is not DNS-resolvable from the gateway host (so the
 *   HttpProxyHandler intercepts before DNS can fail). Those scenarios are not covered here;
 *   the tests below cover bypass_hosts behaviour and failure paths only.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class WebSocketProxyProfileTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(WebSocketProxyProfileTestCase.class);

    // Hostnames used as API backend endpoints. Profile matching is on the hostname string,
    // not the resolved IP, so fake .local names work through proxy (relay goes to 127.0.0.1).
    private static final String PROXIED_HOST       = "proxied.ws.local";
    private static final String AUTH_PROXIED_HOST  = "auth.proxied.ws.local";
    // DIRECT uses 127.0.0.1 — in profile 1's target_hosts AND bypass_hosts → direct connection
    private static final String WRONG_AUTH_HOST    = "wrong-auth.ws.local";
    private static final String CATCHALL_HOST      = "catchall.ws.local";
    // BYPASS_CATCHALL uses "localhost" — matches catch-all profile but is in catch-all bypass_hosts → direct
    private static final String BYPASS_CATCHALL_HOST = "localhost";

    // Credentials for profile 2 (authenticated proxy). Must match what authProxy enforces.
    private static final String PROXY_USERNAME = "testproxyuser";
    private static final String PROXY_PASSWORD = "testproxypass";

    // Wrong credentials for profile 3 — authProxy rejects these with 407.
    private static final String WRONG_PROXY_USERNAME = "wrongproxyuser";
    private static final String WRONG_PROXY_PASSWORD = "wrongproxypass";

    private static final String TEST_MESSAGE = "ws-proxy-test-message";
    private static final String API_VERSION  = "1.0.0";

    private static final String PROXIED_API_NAME       = "WSProxiedAPI";
    private static final String PROXIED_API_CONTEXT    = "ws-proxied";
    private static final String AUTH_PROXIED_API_NAME  = "WSAuthProxiedAPI";
    private static final String AUTH_PROXIED_API_CONTEXT = "ws-auth-proxied";
    private static final String DIRECT_API_NAME        = "WSDirectAPI";
    private static final String DIRECT_API_CONTEXT     = "ws-direct";
    private static final String WRONG_AUTH_API_NAME    = "WSWrongAuthAPI";
    private static final String WRONG_AUTH_API_CONTEXT = "ws-wrong-auth";
    private static final String CATCHALL_API_NAME      = "WSCatchAllAPI";
    private static final String CATCHALL_API_CONTEXT   = "ws-catchall";
    private static final String BYPASS_CATCHALL_API_NAME    = "WSBypassCatchAllAPI";
    private static final String BYPASS_CATCHALL_API_CONTEXT = "ws-bypass-catchall";

    private static final String APP_NAME = "WSProxyTestApp";

    private Server wsBackendServer;
    private int wsBackendPort;

    // Three embedded CONNECT proxies — one per routing path under test.
    private TestHttpConnectProxy proxy;         // anonymous, profile 1 + 3
    private TestHttpConnectProxy authProxy;     // enforces Basic auth, profiles 2 & 3
    private TestHttpConnectProxy catchAllProxy; // anonymous, catch-all profile 4

    private ServerConfigurationManager serverConfigurationManager;

    private String proxiedApiId;
    private String authProxiedApiId;
    private String directApiId;
    private String wrongAuthApiId;
    private String catchAllApiId;
    private String bypassCatchAllApiId;
    private String appId;
    private String accessToken;

    private String proxiedApiEndpoint;
    private String authProxiedApiEndpoint;
    private String directApiEndpoint;
    private String wrongAuthApiEndpoint;
    private String catchAllApiEndpoint;
    private String bypassCatchAllApiEndpoint;

    @Factory(dataProvider = "userModeDataProvider")
    public WebSocketProxyProfileTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        // Proxy profiles are server-wide config; no additional coverage from running as tenant.
        return new Object[][]{ new Object[]{TestUserMode.SUPER_TENANT_ADMIN} };
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        startWsBackend();

        // Start all three proxies before building deployment.toml so we have their ports.
        proxy = new TestHttpConnectProxy();
        proxy.start();

        authProxy = new TestHttpConnectProxy(PROXY_USERNAME, PROXY_PASSWORD);
        authProxy.start();

        catchAllProxy = new TestHttpConnectProxy();
        catchAllProxy.start();

        File deploymentToml = buildDeploymentToml(proxy.getPort(), authProxy.getPort(),
                catchAllProxy.getPort());

        serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
        serverConfigurationManager.applyConfiguration(deploymentToml);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        try {
            for (String apiId : new String[]{
                    proxiedApiId, authProxiedApiId, directApiId,
                    wrongAuthApiId, catchAllApiId, bypassCatchAllApiId}) {
                if (apiId != null) {
                    undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
                }
            }
            super.cleanUp();
        } finally {
            if (wsBackendServer != null && wsBackendServer.isRunning()) {
                wsBackendServer.stop();
            }
            stopProxy(proxy);
            stopProxy(authProxy);
            stopProxy(catchAllProxy);
            serverConfigurationManager.restoreToLastConfiguration(true);
        }
    }

    // -----------------------------------------------------------------------
    // Setup tests — publish APIs and generate a shared access token
    // -----------------------------------------------------------------------

    @Test(description = "Publish proxied WS API (endpoint matches anonymous proxy profile)")
    public void testPublishProxiedWsApi() throws Exception {
        URI endpoint = new URI("ws://" + PROXIED_HOST + ":" + wsBackendPort);
        proxiedApiId = createAndPublishWsApi(PROXIED_API_NAME, PROXIED_API_CONTEXT, endpoint);
        proxiedApiEndpoint = getWebSocketAPIInvocationURL(PROXIED_API_CONTEXT, API_VERSION);
        log.info("Proxied API endpoint: " + proxiedApiEndpoint);
    }

    @Test(description = "Publish auth-proxied WS API (endpoint matches authenticated proxy profile)",
            dependsOnMethods = "testPublishProxiedWsApi")
    public void testPublishAuthProxiedWsApi() throws Exception {
        URI endpoint = new URI("ws://" + AUTH_PROXIED_HOST + ":" + wsBackendPort);
        authProxiedApiId = createAndPublishWsApi(AUTH_PROXIED_API_NAME, AUTH_PROXIED_API_CONTEXT, endpoint);
        authProxiedApiEndpoint = getWebSocketAPIInvocationURL(AUTH_PROXIED_API_CONTEXT, API_VERSION);
        log.info("Auth-proxied API endpoint: " + authProxiedApiEndpoint);
    }

    /**
     * 127.0.0.1 is in profile 1's target_hosts AND bypass_hosts, so the gateway connects
     * directly even though a matching profile exists.
     */
    @Test(description = "Publish direct WS API (127.0.0.1 endpoint — in profile 1 bypass_hosts)",
            dependsOnMethods = "testPublishAuthProxiedWsApi")
    public void testPublishDirectWsApi() throws Exception {
        URI endpoint = new URI("ws://127.0.0.1:" + wsBackendPort);
        directApiId = createAndPublishWsApi(DIRECT_API_NAME, DIRECT_API_CONTEXT, endpoint);
        directApiEndpoint = getWebSocketAPIInvocationURL(DIRECT_API_CONTEXT, API_VERSION);
        log.info("Direct API endpoint: " + directApiEndpoint);
    }

    /**
     * wrong-auth.ws.local matches profile 3, which points to the authProxy port but carries
     * wrong credentials. The authProxy returns 407, and the gateway fails the upgrade.
     */
    @Test(description = "Publish wrong-auth WS API (wrong proxy credentials — authProxy returns 407)",
            dependsOnMethods = "testPublishDirectWsApi")
    public void testPublishWrongAuthWsApi() throws Exception {
        URI endpoint = new URI("ws://" + WRONG_AUTH_HOST + ":" + wsBackendPort);
        wrongAuthApiId = createAndPublishWsApi(WRONG_AUTH_API_NAME, WRONG_AUTH_API_CONTEXT, endpoint);
        wrongAuthApiEndpoint = getWebSocketAPIInvocationURL(WRONG_AUTH_API_CONTEXT, API_VERSION);
        log.info("Wrong-auth API endpoint: " + wrongAuthApiEndpoint);
    }

    /**
     * catchall.ws.local does not match profiles 1, 2, or 3 (all specific), so it falls
     * through to the catch-all profile 4 and is routed through catchAllProxy.
     */
    @Test(description = "Publish catch-all WS API (no specific profile match — routed through catch-all proxy)",
            dependsOnMethods = "testPublishWrongAuthWsApi")
    public void testPublishCatchAllWsApi() throws Exception {
        URI endpoint = new URI("ws://" + CATCHALL_HOST + ":" + wsBackendPort);
        catchAllApiId = createAndPublishWsApi(CATCHALL_API_NAME, CATCHALL_API_CONTEXT, endpoint);
        catchAllApiEndpoint = getWebSocketAPIInvocationURL(CATCHALL_API_CONTEXT, API_VERSION);
        log.info("Catch-all API endpoint: " + catchAllApiEndpoint);
    }

    /**
     * localhost falls through to the catch-all profile but is listed in its bypass_hosts,
     * so the gateway connects directly to localhost:wsBackendPort (= 127.0.0.1:wsBackendPort).
     */
    @Test(description = "Publish bypass-catch-all WS API (localhost — in catch-all bypass_hosts → direct)",
            dependsOnMethods = "testPublishCatchAllWsApi")
    public void testPublishBypassCatchAllWsApi() throws Exception {
        URI endpoint = new URI("ws://" + BYPASS_CATCHALL_HOST + ":" + wsBackendPort);
        bypassCatchAllApiId = createAndPublishWsApi(
                BYPASS_CATCHALL_API_NAME, BYPASS_CATCHALL_API_CONTEXT, endpoint);
        bypassCatchAllApiEndpoint = getWebSocketAPIInvocationURL(
                BYPASS_CATCHALL_API_CONTEXT, API_VERSION);
        log.info("Bypass-catch-all API endpoint: " + bypassCatchAllApiEndpoint);
    }

    @Test(description = "Create application, subscribe to all six APIs, generate access token",
            dependsOnMethods = "testPublishBypassCatchAllWsApi")
    public void testCreateApplicationAndGenerateToken() throws Exception {
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse appResponse =
                restAPIStore.createApplication(APP_NAME, "",
                        APIMIntegrationConstants.API_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH);
        appId = appResponse.getData();

        for (String apiId : new String[]{
                proxiedApiId, authProxiedApiId, directApiId,
                wrongAuthApiId, catchAllApiId, bypassCatchAllApiId}) {
            SubscriptionDTO sub = restAPIStore.subscribeToAPI(
                    apiId, appId, APIMIntegrationConstants.API_TIER.ASYNC_UNLIMITED);
            assertEquals(sub.getStatus(), SubscriptionDTO.StatusEnum.UNBLOCKED,
                    "Subscription should be in UNBLOCKED state");
        }

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO keyDTO = restAPIStore.generateKeys(appId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        accessToken = keyDTO.getToken().getAccessToken();
        Assert.assertNotNull(accessToken, "Access token should not be null");
    }

    // -----------------------------------------------------------------------
    // Positive tests
    // -----------------------------------------------------------------------

    /**
     * 127.0.0.1 is in profile 1's target_hosts (so the profile matches) but also in its
     * bypass_hosts. bypass_hosts takes precedence — the gateway connects directly.
     *
     * This is the key bypass_hosts-in-specific-profile scenario: the host WOULD be proxied
     * by profile 1 based on target_hosts alone, but the bypass_hosts entry suppresses the proxy.
     *
     * Verified by: echo succeeds (direct to backend), no proxy receives a CONNECT.
     */
    @Test(description = "[Positive] bypass_hosts in specific profile — matching host bypasses proxy",
            dependsOnMethods = "testCreateApplicationAndGenerateToken")
    public void testBypassHostInSpecificProfileGosDirect() throws Exception {
        int proxyCountBefore     = proxy.getConnectCount();
        int authProxyCountBefore = authProxy.getConnectCount();
        int catchAllCountBefore  = catchAllProxy.getConnectCount();

        invokeAndAssertEcho(directApiEndpoint, accessToken);

        assertEquals(proxy.getConnectCount(), proxyCountBefore,
                "Anonymous proxy should not receive a CONNECT for a host in bypass_hosts");
        assertEquals(authProxy.getConnectCount(), authProxyCountBefore,
                "Auth proxy should not receive a CONNECT for a bypass host");
        assertEquals(catchAllProxy.getConnectCount(), catchAllCountBefore,
                "Catch-all proxy should not receive a CONNECT — profile 1 matched first and bypassed");
    }

    /**
     * localhost falls through to the catch-all profile 4 (no specific profile matches).
     * Profile 4 has bypass_hosts = ["localhost"], so the gateway connects directly.
     *
     * This is the key bypass_hosts-in-catch-all scenario: the host WOULD be proxied
     * by the catch-all, but bypass_hosts suppresses it.
     *
     * Verified by: echo succeeds (direct to 127.0.0.1:wsBackendPort), catchAllProxy not contacted.
     */
    @Test(description = "[Positive] bypass_hosts in catch-all profile — matched host bypasses catch-all proxy",
            dependsOnMethods = "testBypassHostInSpecificProfileGosDirect")
    public void testBypassHostInCatchAllProfileGosDirect() throws Exception {
        int catchAllCountBefore = catchAllProxy.getConnectCount();
        int proxyCountBefore    = proxy.getConnectCount();

        invokeAndAssertEcho(bypassCatchAllApiEndpoint, accessToken);

        assertEquals(catchAllProxy.getConnectCount(), catchAllCountBefore,
                "Catch-all proxy should not receive a CONNECT for a host in its bypass_hosts");
        assertEquals(proxy.getConnectCount(), proxyCountBefore,
                "Anonymous proxy should not be contacted — localhost has no specific profile match");
    }

    // -----------------------------------------------------------------------
    // Negative tests
    // -----------------------------------------------------------------------

    /**
     * A connection with an invalid Bearer token is rejected at the gateway before it contacts
     * any proxy or backend.
     * Verified by: connection times out (no onConnect), proxy CONNECT count unchanged.
     */
    @Test(description = "[Negative] Invalid token — gateway rejects before contacting proxy",
            dependsOnMethods = "testBypassHostInCatchAllProfileGosDirect")
    public void testInvalidTokenRejectedBeforeProxy() throws Exception {
        int proxyCountBefore      = proxy.getConnectCount();
        int catchAllCountBefore   = catchAllProxy.getConnectCount();

        WebSocketClient client = new WebSocketClient();
        WebSocketClientImpl socket = new WebSocketClientImpl();
        try {
            client.start();
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            request.setHeader("Authorization", "Bearer invalid_token_xyz");
            client.connect(socket, new URI(proxiedApiEndpoint), request);
            assertFalse(socket.getLatch().await(5, TimeUnit.SECONDS),
                    "Connection should be rejected with an invalid token");
        } finally {
            client.stop();
        }

        assertEquals(proxy.getConnectCount(), proxyCountBefore,
                "Proxy should not receive a CONNECT when the token is invalid");
        assertEquals(catchAllProxy.getConnectCount(), catchAllCountBefore,
                "Catch-all proxy should not receive a CONNECT when the token is invalid");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void startWsBackend() throws Exception {
        WebSocketHandler wsHandler = new WebSocketHandler() {
            @Override
            public void configure(WebSocketServletFactory factory) {
                factory.register(WebSocketServerImpl.class);
            }
        };
        wsBackendServer = new Server(0);
        wsBackendServer.setHandler(wsHandler);
        wsBackendServer.start();
        wsBackendPort = wsBackendServer.getURI().getPort();
        log.info("WS echo backend started on port " + wsBackendPort);
    }

    private String createAndPublishWsApi(String name, String context, URI endpoint) throws Exception {
        APIRequest req = new APIRequest(name, context, endpoint, endpoint);
        req.setVersion(API_VERSION);
        req.setTiersCollection(APIMIntegrationConstants.API_TIER.ASYNC_UNLIMITED);
        req.setProvider(user.getUserName());
        req.setType("WS");
        req.setApiTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

        String apiId = restAPIPublisher.addAPI(req).getData();
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);
        waitForAPIDeploymentSync(user.getUserName(), name, API_VERSION,
                APIMIntegrationConstants.IS_API_EXISTS);
        return apiId;
    }

    private void invokeAndAssertEcho(String apiEndpoint, String token) throws Exception {
        WebSocketClient client = new WebSocketClient();
        WebSocketClientImpl socket = new WebSocketClientImpl();
        try {
            client.start();
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            request.setHeader("Authorization", "Bearer " + token);
            client.connect(socket, new URI(apiEndpoint), request);

            assertTrue(socket.getLatch().await(30, TimeUnit.SECONDS),
                    "WS connection should be established within 30 seconds");
            socket.sendMessage(TEST_MESSAGE);
            waitForReply(socket);

            assertFalse(StringUtils.isEmpty(socket.getResponseMessage()),
                    "Client should receive a response from the backend");
            assertEquals(socket.getResponseMessage(), TEST_MESSAGE.toUpperCase(),
                    "Echo response should be the uppercased test message");
        } finally {
            client.stop();
        }
    }

    private void waitForReply(WebSocketClientImpl socket) {
        long deadline = System.currentTimeMillis() + 30_000;
        while (StringUtils.isEmpty(socket.getResponseMessage())
                && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void stopProxy(TestHttpConnectProxy p) {
        if (p != null) {
            try {
                p.stop();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Reads the base webSocketTest/deployment.toml and appends four proxy profile blocks:
     *
     *   Profile 1 — anonymous proxy for proxied.ws.local; 127.0.0.1 in target + bypass → direct.
     *   Profile 2 — authenticated proxy for auth.proxied.ws.local with correct credentials.
     *   Profile 3 — wrong-credentials profile for wrong-auth.ws.local pointing to authProxy port.
     *   Profile 4 — catch-all (*) with bypass_hosts = ["localhost"] → localhost goes direct.
     *
     * TOML escaping note:
     *   Java "\\\\." → actual string "\\." → TOML parses "\\" as "\" → regex "\." (escaped dot).
     */
    private File buildDeploymentToml(int proxyPort, int authProxyPort, int catchAllProxyPort)
            throws Exception {
        String basePath = TestConfigurationProvider.getResourceLocation()
                + File.separator + "artifacts"
                + File.separator + "AM"
                + File.separator + "configFiles"
                + File.separator + "webSocketTest"
                + File.separator + "deployment.toml";

        String base = new String(Files.readAllBytes(Paths.get(basePath)), StandardCharsets.UTF_8);

        String proxyProfileSection = "\n\n"
                // Profile 1: anonymous proxy; 127.0.0.1 is in target_hosts AND bypass_hosts
                + "[[transport.ws.proxy_profile]]\n"
                + "target_hosts = [\"proxied\\\\.ws\\\\.local\", \"127\\\\.0\\\\.0\\\\.1\"]\n"
                + "proxy_host = \"127.0.0.1\"\n"
                + "proxy_port = " + proxyPort + "\n"
                + "bypass_hosts = [\"127\\\\.0\\\\.0\\\\.1\"]\n"
                + "\n"
                // Profile 2: authenticated proxy with correct credentials
                + "[[transport.ws.proxy_profile]]\n"
                + "target_hosts = [\"auth\\\\.proxied\\\\.ws\\\\.local\"]\n"
                + "proxy_host = \"127.0.0.1\"\n"
                + "proxy_port = " + authProxyPort + "\n"
                + "proxy_username = \"" + PROXY_USERNAME + "\"\n"
                + "proxy_password = \"" + PROXY_PASSWORD + "\"\n"
                + "\n"
                // Profile 3: same authProxy port but wrong credentials → 407
                + "[[transport.ws.proxy_profile]]\n"
                + "target_hosts = [\"wrong-auth\\\\.ws\\\\.local\"]\n"
                + "proxy_host = \"127.0.0.1\"\n"
                + "proxy_port = " + authProxyPort + "\n"
                + "proxy_username = \"" + WRONG_PROXY_USERNAME + "\"\n"
                + "proxy_password = \"" + WRONG_PROXY_PASSWORD + "\"\n"
                + "\n"
                // Profile 4: catch-all; localhost is in bypass_hosts → direct
                + "[[transport.ws.proxy_profile]]\n"
                + "target_hosts = [\"*\"]\n"
                + "proxy_host = \"127.0.0.1\"\n"
                + "proxy_port = " + catchAllProxyPort + "\n"
                + "bypass_hosts = [\"localhost\"]\n";

        // ServerConfigurationManager.applyConfiguration(File) calls backupConfiguration
        // using file.getName() as the target name in <carbonHome>/repository/conf/.
        // The file MUST be named "deployment.toml" so the framework can find and back up
        // the existing conf/deployment.toml before replacing it.
        // Use a sub-directory (wsProxy/) so this generated file doesn't collide with
        // the base deployment.toml we read from above.
        File generatedFile = new File(
                TestConfigurationProvider.getResourceLocation()
                + File.separator + "artifacts"
                + File.separator + "AM"
                + File.separator + "configFiles"
                + File.separator + "webSocketTest"
                + File.separator + "wsProxy"
                + File.separator + "deployment.toml");
        Files.write(generatedFile.toPath(),
                (base + proxyProfileSection).getBytes(StandardCharsets.UTF_8));
        log.info("Generated proxy deployment.toml at " + generatedFile.getAbsolutePath()
                + " (proxy=" + proxyPort
                + ", authProxy=" + authProxyPort
                + ", catchAllProxy=" + catchAllProxyPort + ")");
        return generatedFile;
    }
}
