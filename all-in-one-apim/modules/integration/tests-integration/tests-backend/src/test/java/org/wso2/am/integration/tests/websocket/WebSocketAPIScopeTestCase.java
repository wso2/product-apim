/*
 *   Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.am.integration.tests.websocket;

import com.google.gson.Gson;
import io.netty.handler.codec.http.HttpHeaders;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIScopeDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ScopeDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.tests.websocket.client.WebSocketClientImpl;
import org.wso2.am.integration.tests.websocket.server.WebSocketServerImpl;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.xml.StringUtils;

import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class WebSocketAPIScopeTestCase extends APIMIntegrationBaseTest {

    private final Log log = LogFactory.getLog(WebSocketAPIScopeTestCase.class);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final String apiName = "WebSocketAPI";
    private final String applicationJWTName = "WebSocketJWTTypeApplication";
    private final String testMessage = "Web Socket Test Message";
    private final String PRODUCTS_CATALOG_1_METHOD = "/products/catalog/1";
    private final String PRODUCTS_POPULAR_METHOD = "/products/popular";
    private final String ORDERS = "/orders";
    private final String WILDCARD = "/noexactmatch";
    private String appJWTId;
    private ApplicationKeyDTO applicationKeyDTO;
    private String apiEndPoint;
    private APIPublisherRestClient apiPublisher;
    private String provider;
    private String consumerKey;
    private String consumerSecret;
    private APIRequest apiRequest;
    private int webSocketServerPort;
    private String webSocketServerHost;
    private ServerConfigurationManager serverConfigurationManager;
    private String wsEventPublisherSource = TestConfigurationProvider.getResourceLocation() + File.separator +
            "artifacts"
            + File.separator + "AM" + File.separator + "configFiles" + File.separator + "webSocketTest"
            + File.separator;
    private String wsEventPublisherTarget = FrameworkPathUtil.getCarbonHome() + File.separator + "repository"
            + File.separator + "deployment" + File.separator + "server" + File.separator + "eventpublishers"
            + File.separator;
    private String wsRequestEventPublisherSource = "WS_Req_Logger.xml";
    private String wsThrottleOutEventPublisherSource = "WS_Throttle_Out_Logger.xml";
    private String websocketAPIID;
    private URL tokenEndpointURL;
    @Factory(dataProvider = "userModeDataProvider")
    public WebSocketAPIScopeTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        // Removing Tenant_ADMIN due to https://github.com/wso2/product-apim/issues/10183
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN}
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
        serverConfigurationManager.applyConfigurationWithoutRestart
                (new File(wsEventPublisherSource + wsRequestEventPublisherSource),
                        new File(wsEventPublisherTarget + wsRequestEventPublisherSource), false);
        serverConfigurationManager.applyConfigurationWithoutRestart
                (new File(wsEventPublisherSource + wsThrottleOutEventPublisherSource),
                        new File(wsEventPublisherTarget + wsThrottleOutEventPublisherSource), false);
        webSocketServerHost = InetAddress.getLocalHost().getHostName();
        int lowerPortLimit = 9950;
        int upperPortLimit = 9999;
        webSocketServerPort = getAvailablePort(lowerPortLimit, upperPortLimit);
        if (webSocketServerPort == -1) {
            throw new APIManagerIntegrationTestException("No available port in the range " +
                    lowerPortLimit + "-" + upperPortLimit + " was found");
        }
        log.info("Selected port " + webSocketServerPort + " to start backend server");
        startWebSocketServer(webSocketServerPort);
    }

    @Test(description = "Publish WebSocket API")
    public void publishWebSocketAPI() throws Exception {

        provider = user.getUserName();
        String apiContext = "echo";
        String apiVersion = "1.0.0";

        URI endpointUri = new URI("ws://" + webSocketServerHost + ":" + webSocketServerPort);

        //Create the api creation request object
        apiRequest = new APIRequest(apiName, apiContext, endpointUri, endpointUri);
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.ASYNC_UNLIMITED);
        apiRequest.setProvider(provider);
        apiRequest.setType("WS");
        apiRequest.setApiTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        HttpResponse addAPIResponse = restAPIPublisher.addAPI(apiRequest);
        websocketAPIID = addAPIResponse.getData();
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(websocketAPIID);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(createdApiResponse.getData(), APIDTO.class);

        // Adding admin role
        List role = new ArrayList();
        role.add("admin");
        //Adding Scope A
        ScopeDTO scopeObjectA = new ScopeDTO();
        scopeObjectA.setName("ScopeA");
        scopeObjectA.setBindings(role);
        APIScopeDTO apiScopeADTO = new APIScopeDTO();
        apiScopeADTO.setScope(scopeObjectA);
        //Adding Scope B
        ScopeDTO scopeObjectB = new ScopeDTO();
        scopeObjectB.setName("ScopeB");
        scopeObjectB.setBindings(role);
        APIScopeDTO apiScopeBDTO = new APIScopeDTO();
        apiScopeBDTO.setScope(scopeObjectB);
        //Adding Scope C
        ScopeDTO scopeObjectC = new ScopeDTO();
        scopeObjectC.setName("ScopeC");
        scopeObjectC.setBindings(role);
        APIScopeDTO apiScopeCDTO = new APIScopeDTO();
        apiScopeCDTO.setScope(scopeObjectC);
        //Adding Scope D
        ScopeDTO scopeObjectD = new ScopeDTO();
        scopeObjectD.setName("ScopeD");
        scopeObjectD.setBindings(role);
        APIScopeDTO apiScopeDDTO = new APIScopeDTO();
        apiScopeDDTO.setScope(scopeObjectD);
        // Adding scopes to API
        List apiScopeList = new ArrayList();
        apiScopeList.add(apiScopeADTO);
        apiScopeList.add(apiScopeBDTO);
        apiScopeList.add(apiScopeCDTO);
        apiScopeList.add(apiScopeDDTO);
        apidto.setScopes(apiScopeList);

        // Adding Operation with scopes
        List<APIOperationsDTO> operations = new ArrayList<>();

        // Add PRODUCTS_CATALOG_1_METHOD
        List<String> scopesA = new ArrayList<>();
        scopesA.add("ScopeA");
        APIOperationsDTO apiOperationsDTOA = new APIOperationsDTO();
        apiOperationsDTOA.setVerb("SUBSCRIBE");
        apiOperationsDTOA.setTarget("/products/catalog/{catalog-id}");
        apiOperationsDTOA.setAuthType("Application & Application User");
        apiOperationsDTOA.setThrottlingPolicy("Unlimited");
        apiOperationsDTOA.setScopes(scopesA);
        operations.add(apiOperationsDTOA);

        // Add PRODUCTS_POPULAR_METHOD
        List<String> scopesB = new ArrayList<>();
        scopesB.add("ScopeB");
        APIOperationsDTO apiOperationsDTOB = new APIOperationsDTO();
        apiOperationsDTOB.setVerb("SUBSCRIBE");
        apiOperationsDTOB.setTarget("/products/popular");
        apiOperationsDTOB.setAuthType("Application & Application User");
        apiOperationsDTOB.setThrottlingPolicy("Unlimited");
        apiOperationsDTOB.setScopes(scopesB);
        operations.add(apiOperationsDTOB);

        // Add ORDERS
        List<String> scopesC = new ArrayList<>();
        scopesC.add("ScopeC");
        APIOperationsDTO apiOperationsDTOC = new APIOperationsDTO();
        apiOperationsDTOC.setVerb("SUBSCRIBE");
        apiOperationsDTOC.setTarget("/orders");
        apiOperationsDTOC.setAuthType("Application & Application User");
        apiOperationsDTOC.setThrottlingPolicy("Unlimited");
        apiOperationsDTOC.setScopes(scopesC);
        operations.add(apiOperationsDTOC);

        // Add ORDERS
        List<String> scopesD = new ArrayList<>();
        scopesD.add("ScopeD");
        APIOperationsDTO apiOperationsDTOD = new APIOperationsDTO();
        apiOperationsDTOD.setVerb("SUBSCRIBE");
        apiOperationsDTOD.setTarget("/*");
        apiOperationsDTOD.setAuthType("Application & Application User");
        apiOperationsDTOD.setThrottlingPolicy("Unlimited");
        apiOperationsDTOD.setScopes(scopesD);
        operations.add(apiOperationsDTOD);

        apidto.operations(operations);

        // Update the API
        restAPIPublisher.updateAPI(apidto);

        createAPIRevisionAndDeployUsingRest(websocketAPIID, restAPIPublisher);
        restAPIPublisher
                .changeAPILifeCycleStatus(websocketAPIID, APILifeCycleAction.PUBLISH.getAction(), null);
        waitForAPIDeploymentSync(user.getUserName(), apiName, apiVersion,
                APIMIntegrationConstants.IS_API_EXISTS);

        APIIdentifier apiIdentifierWebSocket = new APIIdentifier(provider, apiName, apiVersion);

        // replace port with inbound endpoint port

        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode) || TestUserMode.SUPER_TENANT_USER.equals(userMode)) {
            apiEndPoint = getWebSocketAPIInvocationURL(apiContext, apiVersion);
        } else {
            apiEndPoint = getWebSocketTenantAPIInvocationURL(apiContext, apiVersion, user.getUserDomain());
        }
        log.info("API Endpoint URL" + apiEndPoint);
        APIListDTO apiPublisherAllAPIs = restAPIPublisher.getAllAPIs();
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierWebSocket, apiPublisherAllAPIs),
                "Published API is visible in API Publisher.");
        org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO restAPIStoreAllAPIs;
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            restAPIStoreAllAPIs = restAPIStore.getAllAPIs();
        } else {
            restAPIStoreAllAPIs = restAPIStore.getAllAPIs(user.getUserDomain());
        }
        assertTrue(APIMTestCaseUtils.isAPIAvailableInStore(apiIdentifierWebSocket, restAPIStoreAllAPIs),
                "Published API is visible in API Store.");
    }

    @Test(description = "Create JWT Type Application and subscribe", dependsOnMethods = "publishWebSocketAPI")
    public void testWebSocketAPIJWTApplicationSubscription() throws Exception {

        HttpResponse applicationResponse = restAPIStore.createApplication(applicationJWTName,
                "", APIMIntegrationConstants.API_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.JWT);
        appJWTId = applicationResponse.getData();
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(websocketAPIID, appJWTId,
                APIMIntegrationConstants.API_TIER.ASYNC_UNLIMITED);
        //Validate Subscription of the API
        Assert.assertEquals(subscriptionDTO.getStatus(), SubscriptionDTO.StatusEnum.UNBLOCKED);
    }

    @Test(description = "Invoke API using token", dependsOnMethods = "testWebSocketAPIJWTApplicationSubscription")
    public void testWebSocketAPIGenerateKeys() throws Exception {

        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.REFRESH_CODE);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        applicationKeyDTO = restAPIStore.generateKeys(appJWTId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        consumerKey = applicationKeyDTO.getConsumerKey();
        consumerSecret = applicationKeyDTO.getConsumerSecret();
        tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
    }

    @Test(description = "Invoke API PRODUCTS_CATALOG_1_METHOD endpoint using token", dependsOnMethods =
            "testWebSocketAPIGenerateKeys")
    public void testWebSocketAPIInvocation_PRODUCTS_CATALOG_1_METHOD() throws Exception {

        String requestBodyForScope =
                "grant_type=password&username=" + user.getUserName() + "&password=" + user.getPassword() + "&scope" +
                        "=ScopeA";
        JSONObject accessTokenGenerationResponseScope = new JSONObject(
                restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBodyForScope, tokenEndpointURL)
                        .getData());
        // Validate access token
        org.junit.Assert.assertNotNull(accessTokenGenerationResponseScope);
        org.junit.Assert.assertTrue(accessTokenGenerationResponseScope.getString("scope").contains("ScopeA"));
        org.junit.Assert.assertTrue(accessTokenGenerationResponseScope.getString("expires_in").equals("3600"));
        String accessTokenScope = accessTokenGenerationResponseScope.getString("access_token");
        WebSocketClient client = new WebSocketClient();
        try {
            invokeAPI(
                    client, accessTokenScope, AUTH_IN.HEADER, null,
                    apiEndPoint + PRODUCTS_CATALOG_1_METHOD, true);
            invokeAPI(
                    client, accessTokenScope, AUTH_IN.HEADER, null,
                    apiEndPoint + PRODUCTS_POPULAR_METHOD, false);
            invokeAPI(
                    client, accessTokenScope, AUTH_IN.HEADER, null,
                    apiEndPoint + ORDERS, false);
            invokeAPI(
                    client, accessTokenScope, AUTH_IN.HEADER, null,
                    apiEndPoint + WILDCARD, false);
        } catch (Exception e) {
            log.error("Exception in connecting to server", e);
            Assert.fail("Client cannot connect to server");
        } finally {
            client.stop();
        }
    }

    @Test(description = "Invoke API PRODUCTS_POPULAR_METHOD endpoint using token", dependsOnMethods =
            "testWebSocketAPIGenerateKeys")
    public void testWebSocketAPIInvocation_PRODUCTS_POPULAR_METHOD() throws Exception {

        String requestBodyForScope =
                "grant_type=password&username=" + user.getUserName() + "&password=" + user.getPassword() + "&scope" +
                        "=ScopeB";
        JSONObject accessTokenGenerationResponseScope = new JSONObject(
                restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBodyForScope, tokenEndpointURL)
                        .getData());
        // Validate access token
        org.junit.Assert.assertNotNull(accessTokenGenerationResponseScope);
        org.junit.Assert.assertTrue(accessTokenGenerationResponseScope.getString("scope").contains("ScopeB"));
        org.junit.Assert.assertTrue(accessTokenGenerationResponseScope.getString("expires_in").equals("3600"));
        String accessTokenScope = accessTokenGenerationResponseScope.getString("access_token");
        WebSocketClient client = new WebSocketClient();
        try {
            invokeAPI(
                    client, accessTokenScope, AUTH_IN.HEADER, null,
                    apiEndPoint + PRODUCTS_POPULAR_METHOD, true);
            invokeAPI(
                    client, accessTokenScope, AUTH_IN.HEADER, null,
                    apiEndPoint + PRODUCTS_CATALOG_1_METHOD, false);
            invokeAPI(
                    client, accessTokenScope, AUTH_IN.HEADER, null,
                    apiEndPoint + ORDERS, false);
            invokeAPI(
                    client, accessTokenScope, AUTH_IN.HEADER, null,
                    apiEndPoint + WILDCARD, false);
        } catch (Exception e) {
            log.error("Exception in connecting to server", e);
            Assert.fail("Client cannot connect to server");
        } finally {
            client.stop();
        }
    }

    @Test(description = "Invoke API ORDERS endpoint using token", dependsOnMethods = "testWebSocketAPIGenerateKeys")
    public void testWebSocketAPIInvocation_ORDERS() throws Exception {

        String requestBodyForScope =
                "grant_type=password&username=" + user.getUserName() + "&password=" + user.getPassword() + "&scope" +
                        "=ScopeC";
        JSONObject accessTokenGenerationResponseScope = new JSONObject(
                restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBodyForScope, tokenEndpointURL)
                        .getData());
        // Validate access token
        org.junit.Assert.assertNotNull(accessTokenGenerationResponseScope);
        org.junit.Assert.assertTrue(accessTokenGenerationResponseScope.getString("scope").contains("ScopeC"));
        org.junit.Assert.assertTrue(accessTokenGenerationResponseScope.getString("expires_in").equals("3600"));
        String accessTokenScope = accessTokenGenerationResponseScope.getString("access_token");
        WebSocketClient client = new WebSocketClient();
        try {
            invokeAPI(
                    client, accessTokenScope, AUTH_IN.HEADER, null,
                    apiEndPoint + ORDERS, true);
            invokeAPI(
                    client, accessTokenScope, AUTH_IN.HEADER, null,
                    apiEndPoint + PRODUCTS_CATALOG_1_METHOD, false);
            invokeAPI(
                    client, accessTokenScope, AUTH_IN.HEADER, null,
                    apiEndPoint + PRODUCTS_POPULAR_METHOD, false);
            invokeAPI(
                    client, accessTokenScope, AUTH_IN.HEADER, null,
                    apiEndPoint + WILDCARD, false);
        } catch (Exception e) {
            log.error("Exception in connecting to server", e);
            Assert.fail("Client cannot connect to server");
        } finally {
            client.stop();
        }
    }

    @Test(description = "Invoke API WILDCARD endpoint using token", dependsOnMethods = "testWebSocketAPIGenerateKeys")
    public void testWebSocketAPIInvocation_WILDCARD() throws Exception {

        String requestBodyForScope =
                "grant_type=password&username=" + user.getUserName() + "&password=" + user.getPassword() + "&scope" +
                        "=ScopeD";
        JSONObject accessTokenGenerationResponseScope = new JSONObject(
                restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBodyForScope, tokenEndpointURL)
                        .getData());
        // Validate access token
        org.junit.Assert.assertNotNull(accessTokenGenerationResponseScope);
        org.junit.Assert.assertTrue(accessTokenGenerationResponseScope.getString("scope").contains("ScopeD"));
        org.junit.Assert.assertTrue(accessTokenGenerationResponseScope.getString("expires_in").equals("3600"));
        String accessTokenScope = accessTokenGenerationResponseScope.getString("access_token");
        WebSocketClient client = new WebSocketClient();
        try {
            invokeAPI(
                    client, accessTokenScope, AUTH_IN.HEADER, null,
                    apiEndPoint + WILDCARD, true);
            invokeAPI(
                    client, accessTokenScope, AUTH_IN.HEADER, null,
                    apiEndPoint + PRODUCTS_CATALOG_1_METHOD, false);
            invokeAPI(
                    client, accessTokenScope, AUTH_IN.HEADER, null,
                    apiEndPoint + PRODUCTS_POPULAR_METHOD, false);
            invokeAPI(
                    client, accessTokenScope, AUTH_IN.HEADER, null,
                    apiEndPoint + ORDERS, false);
        } catch (Exception e) {
            log.error("Exception in connecting to server", e);
            Assert.fail("Client cannot connect to server");
        } finally {
            client.stop();
        }
    }

    /**
     * Wait for client to receive reply from the server
     *
     * @param clientSocket WebSocket Client Object
     */
    private void waitForReply(WebSocketClientImpl clientSocket) {

        long currentTime = System.currentTimeMillis();
        long WAIT_TIME = 30 * 1000;
        long waitTime = currentTime + WAIT_TIME;
        while (StringUtils.isEmpty(clientSocket.getResponseMessage()) && waitTime > System.currentTimeMillis()) {
            try {
                log.info("Waiting for reply from server:");
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
        log.info("Client received :" + clientSocket.getResponseMessage());
    }

    /**
     * Starts backend web socket server in given port
     *
     * @param serverPort Port that WebSocket Server starts
     */
    private void startWebSocketServer(final int serverPort) {

        executorService.execute(new Runnable() {
            public void run() {

                WebSocketHandler wsHandler = new WebSocketHandler() {
                    @Override
                    public void configure(WebSocketServletFactory factory) {

                        factory.register(WebSocketServerImpl.class);
                    }
                };
                Server server = new Server(serverPort);
                server.setHandler(wsHandler);
                try {
                    server.start();
                    log.info("WebSocket backend server started at port: " + serverPort);
                } catch (InterruptedException ignore) {
                } catch (Exception e) {
                    log.error("Error while starting backend server at port: " + serverPort, e);
                    Assert.fail("Cannot start WebSocket server");
                }
            }

        });
    }

    /**
     * Invoke deployed API via WebSocketClient and wait for reply
     *
     * @param client      WebSocketClient object
     * @param accessToken API access Token
     * @param in          location of the Auth header. {@code query} or {@code header}
     * @param apiEndPoint Endpoint URI
     * @throws Exception If an error occurs while invoking WebSocket API
     */
    private void invokeAPI(WebSocketClient client, String accessToken, AUTH_IN in, HttpHeaders optionalRequestHeaders,
                           String apiEndPoint, boolean isValidToken) throws Exception {

        WebSocketClientImpl socket = new WebSocketClientImpl();
        client.start();
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        URI echoUri = null;

        if (AUTH_IN.HEADER == in) {
            request.setHeader("Authorization", "Bearer " + accessToken);
            echoUri = new URI(apiEndPoint);
        } else if (AUTH_IN.QUERY == in) {
            echoUri = new URI(apiEndPoint + "?access_token=" + accessToken);
        }

        if (optionalRequestHeaders != null) {
            for (Map.Entry<String, String> headerEntry : optionalRequestHeaders.entries()) {
                request.setHeader(headerEntry.getKey(), headerEntry.getValue());
            }
        }

        client.connect(socket, echoUri, request);
        boolean isConnected = socket.getLatch().await(30, TimeUnit.SECONDS);
        if (isConnected && isValidToken) {
            socket.sendMessage(testMessage);
            waitForReply(socket);
            if (StringUtils.isEmpty(socket.getResponseMessage())) {
                throw new APIManagerIntegrationTestException("Unable to create client connection");
            }
            assertEquals(StringUtils.isEmpty(socket.getResponseMessage()), false,
                    "Client did not receive response from server");
            assertEquals(socket.getResponseMessage(), testMessage.toUpperCase(),
                    "Received response in not matching");
            socket.setResponseMessage(null);
        } else if (!isConnected && !isValidToken) {
            assertEquals(isConnected, isValidToken, "Client received response from server for invalid token");
        }
        else {
            throw new APIManagerIntegrationTestException("Unable to create client connection");
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(appJWTId);
        restAPIPublisher.deleteAPI(websocketAPIID);
        serverConfigurationManager.restoreToLastConfiguration(false);
        executorService.shutdownNow();
        super.cleanUp();
    }

    enum AUTH_IN {
        HEADER,
        QUERY
    }
}
