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

import org.apache.commons.io.IOUtils;
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
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.clients.AdminDashboardRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.tests.websocket.client.WebSocketClientImpl;
import org.wso2.am.integration.tests.websocket.server.WebSocketServerImpl;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.xml.StringUtils;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class WebSocketAPITestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(WebSocketAPITestCase.class);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final String apiName = "WebSocketAPI";
    private final String applicationName = "WebSocketApplication";
    private final String testMessage = "Web Socket Test Message";
    private String apiEndPoint;
    private APIPublisherRestClient apiPublisher;
    private String provider;
    private String consumerKey;
    private String consumerSecret;
    private APIRequest apiRequest;
    private int webSocketServerPort;
    private String webSocketServerHost;

    @Factory(dataProvider = "userModeDataProvider")
    public WebSocketAPITestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
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
        apiPublisher = new APIPublisherRestClient(getPublisherURLHttp());
        apiStore = new APIStoreRestClient(getStoreURLHttp());
        provider = user.getUserName();
        String apiContext = "echo";
        String apiVersion = "1.0.0";

        URI endpointUri = new URI("ws://" + webSocketServerHost + ":" + webSocketServerPort);

        //Create the api creation request object
        apiRequest = new APIRequest(apiName, apiContext, endpointUri, endpointUri);
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection("Unlimited");
        apiRequest.setProvider(provider);
        apiRequest.setType("WS");
        apiPublisher.login(user.getUserName(),
                user.getPassword());
        HttpResponse addAPIResponse = apiPublisher.addAPI(apiRequest);

        verifyResponse(addAPIResponse);

        //publishing API
        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiName, user.getUserName(),
                        APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);
        waitForAPIDeploymentSync(user.getUserName(), apiName, apiVersion,
                APIMIntegrationConstants.IS_API_EXISTS);

        APIIdentifier apiIdentifierWebSocket = new APIIdentifier(provider, apiName, apiVersion);

        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

        // replace port with inbound endpoint port
        apiEndPoint = getWebSocketAPIInvocationURL(apiContext, apiVersion);
        log.info("API Endpoint URL" + apiEndPoint);

        List<APIIdentifier> publisherAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiPublisher.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierWebSocket, publisherAPIList),
                "Published API is visible in API Publisher.");

        List<APIIdentifier> storeAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiStore.getAllPublishedAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierWebSocket, storeAPIList),
                "Published API is visible in API Store.");
    }

    @Test(description = "Create Application and subscribe", dependsOnMethods = "publishWebSocketAPI")
    public void testWebSocketAPIApplicationSubscription() throws Exception {
        apiStore.addApplication(applicationName, APIMIntegrationConstants.API_TIER.UNLIMITED, "", "");
        SubscriptionRequest subscriptionRequest =
                new SubscriptionRequest(apiName, provider);
        subscriptionRequest.setApplicationName(applicationName);
        subscriptionRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

        //Validate Subscription of the API
        HttpResponse subscribeApiResponse = apiStore.subscribe(subscriptionRequest);
        assertEquals(subscribeApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                apiName + "is not Subscribed");
        assertTrue(subscribeApiResponse.getData().contains("\"error\" : false"),
                apiName + "is not Subscribed");
    }

    @Test(description = "Invoke API using token", dependsOnMethods = "testWebSocketAPIApplicationSubscription")
    public void testWebSocketAPIInvocation() throws Exception {
        APPKeyRequestGenerator generateAppKeyRequestSandBox =
                new APPKeyRequestGenerator(applicationName);
        generateAppKeyRequestSandBox.setKeyType("PRODUCTION");
        String responseSandBoxToken = apiStore.generateApplicationKey
                (generateAppKeyRequestSandBox).getData();
        JSONObject jsonObject = new JSONObject(responseSandBoxToken);
        String accessToken =
                jsonObject.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        consumerKey =
                jsonObject.getJSONObject("data").getJSONObject("key").getString("consumerKey");
        consumerSecret =
                jsonObject.getJSONObject("data").getJSONObject("key").getString("consumerSecret");
        Assert.assertNotNull("Access Token not found " + responseSandBoxToken, accessToken);

        WebSocketClient client = new WebSocketClient();
        try {
            invokeAPI(client, accessToken);
        } catch (Exception e) {
            log.error("Exception in connecting to server", e);
            assertTrue(false, "Client cannot connect to server");
        } finally {
            client.stop();
        }
    }

    @Test(description = "Test Throttling for web socket API", dependsOnMethods = "testWebSocketAPIInvocation")
    public void testWebSocketAPIThrottling() throws Exception {
        //Deploy Throttling policy
        AdminDashboardRestClient adminDashboardRestClient = new AdminDashboardRestClient(getPublisherURLHttps());
        adminDashboardRestClient.login(user.getUserName(), user.getPassword());
        InputStream inputStream = new FileInputStream(getAMResourceLocation() + File.separator +
                "configFiles" + File.separator + "webSocketTest" + File.separator + "policy.json");
        HttpResponse addPolicyResponse = adminDashboardRestClient.addThrottlingPolicy(IOUtils.toString(inputStream));
        verifyResponse(addPolicyResponse);

        //Update Throttling policy of the API
        apiRequest.setApiTier("WebSocketTestThrottlingPolicy");
        HttpResponse updateAPIResponse = apiPublisher.updateAPI(apiRequest);
        verifyResponse(updateAPIResponse);

        //Get an Access Token from the user who is logged into the API Store.
        URL tokenEndpointURL = new URL(getGatewayURLNhttp() + "token");
        String subsAccessTokenPayload = APIMTestCaseUtils.getPayloadForPasswordGrant(
                storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

        JSONObject subsAccessTokenGenerationResponse = new JSONObject(
                apiStore.generateUserAccessKey(consumerKey, consumerSecret, subsAccessTokenPayload,
                        tokenEndpointURL).getData());

        String subsRefreshToken = subsAccessTokenGenerationResponse.getString("refresh_token");

        assertFalse(org.apache.commons.lang.StringUtils.isEmpty(subsRefreshToken),
                "Refresh token of access token generated by subscriber is empty");

        //Obtain user access token
        String requestBody = APIMTestCaseUtils.getPayloadForPasswordGrant(user.getUserName(), user.getPassword());
        JSONObject accessTokenGenerationResponse = new JSONObject(
                apiStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody,
                        tokenEndpointURL).getData());

        // get Access Token and Refresh Token
        String refreshToken = accessTokenGenerationResponse.getString("refresh_token");
        String getAccessTokenFromRefreshTokenRequestBody =
                "grant_type=refresh_token&refresh_token=" + refreshToken + "&scope=PRODUCTION";
        accessTokenGenerationResponse = new JSONObject(
                apiStore.generateUserAccessKey(consumerKey, consumerSecret,
                        getAccessTokenFromRefreshTokenRequestBody,
                        tokenEndpointURL).getData());
        String userAccessToken = accessTokenGenerationResponse.getString("access_token");

        Assert.assertNotNull("Access Token not found " + accessTokenGenerationResponse, userAccessToken);
        testThrottling(userAccessToken);
    }

    @Test(description = "Invoke API using invalid token", dependsOnMethods = "testWebSocketAPIThrottling")
    public void testWebSocketAPIInvalidTokenInvocation() throws Exception {
        WebSocketClient client = new WebSocketClient();
        try {
            invokeAPI(client, "00000000-0000-0000-0000-000000000000");
        } catch (APIManagerIntegrationTestException e) {
            log.error("Exception in connecting to server", e);
            assertTrue(true, "Client cannot connect to server");
        } catch (Exception e) {
            log.error("Exception in connecting to server", e);
            assertTrue(false, "Client cannot connect to server");
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
                    assertTrue(false, "Cannot start WebSocket server");
                }
            }

        });
    }

    /**
     * Test throttling by invoking API
     *
     * @param accessToken API accessToken
     */
    private void testThrottling(String accessToken) throws Exception {
        int limit = 4;
        int numberOfIterations = 6;
        WebSocketClient client = new WebSocketClient();
        WebSocketClientImpl socket = new WebSocketClientImpl();
        client.start();
        URI echoUri = new URI(apiEndPoint);
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        request.setHeader("Authorization", "Bearer " + accessToken);
        client.connect(socket, echoUri, request);
        socket.getLatch().await(3, TimeUnit.SECONDS);
        try {
            for (int count = 1; count <= numberOfIterations; count++) {
                socket.sendMessage(testMessage);
                waitForReply(socket);
                log.info("Count :" + count + " Message :" + socket.getResponseMessage());
                if (count > limit) {
                    assertEquals(socket.getResponseMessage(), "Websocket frame throttled out",
                            "Received response is not matching");
                } else {
                    assertEquals(socket.getResponseMessage(), testMessage.toUpperCase(),
                            "Received response is not matching");
                }
                socket.setResponseMessage(null);
            }
        } catch (Exception ex) {
            log.error("Error occurred while calling API : " + ex);
            assertTrue(false, "Client cannot connect to server");
        } finally {
            client.stop();
        }
    }

    /**
     * Invoke deployed API via WebSocketClient and wait for reply
     *
     * @param client      WebSocketClient object
     * @param accessToken API access Token
     * @throws Exception
     */
    private void invokeAPI(WebSocketClient client, String accessToken) throws Exception {

        WebSocketClientImpl socket = new WebSocketClientImpl();
        client.start();
        URI echoUri = new URI(apiEndPoint);
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        request.setHeader("Authorization", "Bearer " + accessToken);
        client.connect(socket, echoUri, request);
        if (socket.getLatch().await(30, TimeUnit.SECONDS)) {
            socket.sendMessage(testMessage);
            waitForReply(socket);
            assertEquals(StringUtils.isEmpty(socket.getResponseMessage()), false,
                    "Client did not receive response from server");
            assertEquals(socket.getResponseMessage(), testMessage.toUpperCase(),
                    "Received response in not matching");
            socket.setResponseMessage(null);
        } else {
            throw new APIManagerIntegrationTestException("Unable to create client connection");
        }
    }

    /**
     * Find a free port to start backend WebSocket server in given port range
     *
     * @param lowerPortLimit from port number
     * @param upperPortLimit to port number
     * @return Available Port Number
     */
    private int getAvailablePort(int lowerPortLimit, int upperPortLimit) {

        while (lowerPortLimit < upperPortLimit) {
            if (isPortFree(lowerPortLimit)) {
                return lowerPortLimit;
            }
            lowerPortLimit += 1;
        }
        return -1;
    }

    /**
     * Check whether give port is available
     *
     * @param port Port Number
     * @return status
     */
    private boolean isPortFree(int port) {
        Socket s = null;
        try {
            s = new Socket(webSocketServerHost, port);
            // something is using the port and has responded.
            return false;
        } catch (IOException e) {
            //port available
            return true;
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to close connection ", e);
                }
            }
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        executorService.shutdownNow();
        super.cleanUp();
    }
}
