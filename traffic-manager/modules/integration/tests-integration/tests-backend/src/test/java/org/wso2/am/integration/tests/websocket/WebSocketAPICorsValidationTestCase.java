/*
 *   Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.AdvancedThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.RequestCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.token.TokenUtils;
import org.wso2.am.integration.tests.websocket.client.WebSocketClientImpl;
import org.wso2.am.integration.tests.websocket.server.WebSocketServerImpl;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.xml.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.*;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class WebSocketAPICorsValidationTestCase extends APIMIntegrationBaseTest {

    private final Log log = LogFactory.getLog(WebSocketAPICorsValidationTestCase.class);
    enum AUTH_IN {
        HEADER,
        QUERY
    }
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final String apiName = "WebSocketAPICorsValidation";
    private final String applicationName = "WebSocketApplicationCorsValidation";
    private final String applicationJWTName = "WebSocketJWTTypeApplicationCorsValidation";
    private final String testMessage = "Web Socket Test Message";
    private String apiEndPoint;
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
    private final String originHeaderName = "http://global.config1.com";
    String appId;
    String appJWTId;
    ApplicationKeyDTO applicationKeyDTO;
    long throttleMarkTime = 0;

    @Factory(dataProvider = "userModeDataProvider")
    public WebSocketAPICorsValidationTestCase(TestUserMode userMode) {

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
        createAPIRevisionAndDeployUsingRest(websocketAPIID,restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatus(websocketAPIID, APILifeCycleAction.PUBLISH.getAction(), null);
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



    /**
      *  Test CORS origin header validation for Websocket API invocations.
      *  This test is added for the fix of https://github.com/wso2/product-apim/issues/12414
      *  Note: This test is better to be executed as the final test as the default deployment.toml configurations are
      *  changed for this test.
      *  Added configs:
      *      [apim.cors]
      *          allow_origins = ["http://global.config1.com", "http://global.config2.com", "http://global.config3.com"]
      *          enable_validation_for_ws = true
     */
    @Test(description = "Test CORS origin header validation for Websocket API invocations", dependsOnMethods = "testWebSocketAPIJWTApplicationSubscription")
    public void testWebsocketAPICORSValidation() throws Exception {
        // Generate access token
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.REFRESH_CODE);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(appJWTId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        WebSocketClient client = new WebSocketClient();

        // Invoke API with 'Origin' header which is configured in deployement.toml
        HttpHeaders headers1 = new DefaultHttpHeaders();
        headers1.add(HttpHeaderNames.ORIGIN, originHeaderName);
        while ( System.currentTimeMillis() < throttleMarkTime + 60000) {
            Thread.sleep(5000L);
        }

        try {
            invokeAPI(client, accessToken, AUTH_IN.HEADER, headers1);
        } catch (Exception e) {
            log.error("Exception in connecting to server", e);
            Assert.fail("Client cannot connect to server");
        } finally {
            client.stop();
        }

        // Invoke API without 'Origin' header. Invocation should be successful due to
        // the fix for the issue https://github.com/wso2/api-manager/issues/612
        HttpHeaders headers2 = new DefaultHttpHeaders();
        try {
            invokeAPI(client, accessToken, AUTH_IN.HEADER, headers2);
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
     * @param in location of the Auth header. {@code query} or {@code header}
     * @throws Exception If an error occurs while invoking WebSocket API
     */
    private void invokeAPI(WebSocketClient client, String accessToken, AUTH_IN in, HttpHeaders optionalRequestHeaders) throws Exception {

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
        if (socket.getLatch().await(30, TimeUnit.SECONDS)) {
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
        } else {
            throw new APIManagerIntegrationTestException("Unable to create client connection");
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        executorService.shutdownNow();
        super.cleanUp();
    }
}
