/*
 *   Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.*;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.tests.streamingapis.StreamingApiTestUtils;
import org.wso2.am.integration.tests.websocket.client.WebSocketClientImpl;
import org.wso2.am.integration.tests.websocket.server.topic.WebSocketRootTopicServlet;
import org.wso2.am.integration.tests.websocket.server.topic.WebSocketTopicServlet;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.xml.StringUtils;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class WebSocketAPITopicsTestCase extends APIMIntegrationBaseTest {

    private final Log log = LogFactory.getLog(WebSocketAPITopicsTestCase.class);
    enum AUTH_IN {
        HEADER,
        QUERY
    }
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final String apiName = "WebSocketAPI";
    private final String applicationName = "WebSocketApplication";
    private String apiEndPoint;
    private String provider;
    private APIRequest apiRequest;
    private int webSocketServerPort;
    private String webSocketServerHost;
    private ServerConfigurationManager serverConfigurationManager;
    private String websocketAPIID;
    String appId;

    @Factory(dataProvider = "userModeDataProvider")
    public WebSocketAPITopicsTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init(userMode);
        serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
        webSocketServerHost = InetAddress.getLocalHost().getHostName();
        int lowerPortLimit = 8080;
        int upperPortLimit = 9030;
        webSocketServerPort = StreamingApiTestUtils
                .getAvailablePort(lowerPortLimit, upperPortLimit, webSocketServerHost);
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
        String apiContext = "echo-topic";
        String apiVersion = "1.0.0";

        URI endpointUri = new URI("ws://" + webSocketServerHost + ":" + webSocketServerPort);

        apiRequest = new APIRequest(apiName, apiContext, endpointUri, endpointUri);
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.ASYNC_UNLIMITED);
        apiRequest.setProvider(provider);
        apiRequest.setType("WS");
        HttpResponse addAPIResponse = restAPIPublisher.addAPI(apiRequest);
        websocketAPIID = addAPIResponse.getData();
        String asyncApiDefinition = "{" +
                "   \"asyncapi\":\"2.0.0\"," +
                "   \"info\":{" +
                "      \"title\":\"" + apiRequest.getName() + "\"," +
                "      \"version\":\"" + apiRequest.getVersion() +
                "   \"}," +
                "   \"servers\":{" +
                "      \"production\":{" +
                "          \"url\":\"" + endpointUri.toString() + "\"," +
                "          \"protocol\":\"ws\"" +
                "      }" +
                "   }," +
                "   \"channels\":{" +

                // Topics with mappings
                "      \"/test\":{" +
                "          \"parameters\":{}," +
                "          \"publish\":{" +
                "              \"x-uri-mapping\":\"/test\"" +
                "          }," +
                "          \"subscribe\":{" +
                "              \"x-uri-mapping\":\"/test\"" +
                "          }" +
                "      }," +
                "      \"/test2/{prop}\":{" +
                "          \"parameters\":{" +
                "              \"prop\":{\"description\":\"\", \"schema\":{\"type\":\"string\"}}" +
                "          }," +
                "          \"publish\":{" +
                "              \"x-uri-mapping\":\"/test2/{uri.var.prop}\"" +
                "          }," +
                "          \"subscribe\":{" +
                "              \"x-uri-mapping\":\"/test2/{uri.var.prop}\"" +
                "          }" +
                "      }," +

                // Topics without mappings
                "      \"/test3\":{" +
                "          \"parameters\":{}," +
                "          \"publish\":{}," +
                "          \"subscribe\":{}" +
                "      }," +
                "      \"/test3/{prop}\":{" +
                "          \"parameters\":{}," +
                "          \"publish\":{}," +
                "          \"subscribe\":{}" +
                "      }" +

                "   }" +
                "}";
        restAPIPublisher.updateAsyncAPI(websocketAPIID, asyncApiDefinition);
        restAPIPublisher.changeAPILifeCycleStatus(websocketAPIID, APILifeCycleAction.PUBLISH.getAction(), null);
        createAPIRevisionAndDeployUsingRest(websocketAPIID, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), apiName, apiVersion, APIMIntegrationConstants.IS_API_EXISTS);

        APIIdentifier apiIdentifierWebSocket = new APIIdentifier(provider, apiName, apiVersion);

        // Replace port with inbound endpoint port
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

    @Test(description = "Create Application and subscribe", dependsOnMethods = "publishWebSocketAPI")
    public void testWebSocketAPIApplicationSubscription() throws Exception {
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
                "", APIMIntegrationConstants.API_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH);
        appId = applicationResponse.getData();
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(websocketAPIID, appId,
                APIMIntegrationConstants.API_TIER.ASYNC_UNLIMITED);
        //Validate Subscription of the API
        Assert.assertEquals(subscriptionDTO.getStatus(), SubscriptionDTO.StatusEnum.UNBLOCKED);
    }

    @Test(description = "Invoke API topics", dependsOnMethods = "testWebSocketAPIApplicationSubscription")
    public void testWebSocketAPITopicsInvocation() throws Exception {
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.REFRESH_CODE);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(appId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        WebSocketClient client = new WebSocketClient();

        // Invoke topics that have mappings. Expected to connect to the endpoint as specified in the mapping.

        String endpointWithTopic = apiEndPoint + "/test";
        invokeAPI(client, accessToken, AUTH_IN.HEADER, endpointWithTopic, false);
        invokeAPI(client, accessToken, AUTH_IN.QUERY, endpointWithTopic, false);

        endpointWithTopic = apiEndPoint + "/test2/p1";
        invokeAPI(client, accessToken, AUTH_IN.HEADER, endpointWithTopic, false);
        invokeAPI(client, accessToken, AUTH_IN.QUERY, endpointWithTopic, false);

        endpointWithTopic = apiEndPoint + "/test2/p2";
        invokeAPI(client, accessToken, AUTH_IN.HEADER, endpointWithTopic, false);
        invokeAPI(client, accessToken, AUTH_IN.QUERY, endpointWithTopic, false);

        // Invoke topics that don't have mappings. Expected to connect to the root of the endpoint ("/").

        endpointWithTopic = apiEndPoint + "/test3";
        invokeAPI(client, accessToken, AUTH_IN.HEADER, endpointWithTopic, true);
        invokeAPI(client, accessToken, AUTH_IN.QUERY, endpointWithTopic, true);

        endpointWithTopic = apiEndPoint + "/test3/p1";
        invokeAPI(client, accessToken, AUTH_IN.HEADER, endpointWithTopic, true);
        invokeAPI(client, accessToken, AUTH_IN.QUERY, endpointWithTopic, true);

        // Invoke a topic which doesn't exist. Expected to fail to connect.
        endpointWithTopic = apiEndPoint + "/test2";
        try {
            invokeAPI(client, accessToken, AUTH_IN.HEADER, endpointWithTopic, false);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().toLowerCase().contains("failed to connect"));
        }
        try {
            invokeAPI(client, accessToken, AUTH_IN.QUERY, endpointWithTopic, false);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().toLowerCase().contains("failed to connect"));
        }
    }

    private static void invokeAPI(WebSocketClient client, String accessToken, AUTH_IN in,
                                  String apiEndPoint, boolean isRootTopic) throws Exception {
        String testMessage = "hello";
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

        client.connect(socket, echoUri, request);
        if (socket.getLatch().await(30, TimeUnit.SECONDS)) {
            socket.sendMessage(testMessage);
            waitForReply(socket);
            Assert.assertFalse(StringUtils.isEmpty(socket.getResponseMessage()),
                    "Received response is empty");
            String expectedResponseMessage;
            if (isRootTopic) {
                expectedResponseMessage = new StringBuilder(testMessage).reverse().toString();
            } else {
                expectedResponseMessage = testMessage.toUpperCase();
            }
            Assert.assertEquals(socket.getResponseMessage(), expectedResponseMessage,
                    "Received response in not matching");
            socket.setResponseMessage(null);
        } else {
            throw new Exception("Failed to connect to: " + apiEndPoint);
        }
    }

    private static void waitForReply(WebSocketClientImpl clientSocket) {

        long currentTime = System.currentTimeMillis();
        long WAIT_TIME = 30 * 1000;
        long waitTime = currentTime + WAIT_TIME;
        while (StringUtils.isEmpty(clientSocket.getResponseMessage()) && waitTime > System.currentTimeMillis()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void startWebSocketServer(final int serverPort) {

        executorService.execute(new Runnable() {
            public void run() {
                Server server = new Server(serverPort);
                ServletHandler servletHandler = new ServletHandler();
                server.setHandler(servletHandler);

                WebSocketTopicServlet webSocketTopicServlet = new WebSocketTopicServlet();
                ServletHolder topicServletHolder = new ServletHolder(webSocketTopicServlet);
                servletHandler.addServletWithMapping(topicServletHolder, "/test");
                servletHandler.addServletWithMapping(topicServletHolder, "/test2/p1");
                servletHandler.addServletWithMapping(topicServletHolder, "/test2/p2");

                WebSocketRootTopicServlet webSocketRootTopicServlet = new WebSocketRootTopicServlet();
                ServletHolder rootTopicServletHolder = new ServletHolder(webSocketRootTopicServlet);
                servletHandler.addServletWithMapping(rootTopicServletHolder, "/");

                try {
                    server.start();
                    log.info("WebSocket endpoint started at port: " + serverPort);
                } catch (Exception e) {
                    log.info("Error while starting WebSocket endpoint at port: " + serverPort);
                    Assert.fail("Cannot start WebSocket endpoint");
                }
            }

        });
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        serverConfigurationManager.restoreToLastConfiguration(false);
        executorService.shutdownNow();
        super.cleanUp();
    }
}
