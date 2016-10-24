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
import org.wso2.am.integration.tests.websocket.client.ToUpperClientSocket;
import org.wso2.am.integration.tests.websocket.server.ToUpperWebSocket;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.xml.StringUtils;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
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
    private final String apiVersion = "1.0.0";
    private final int webSocketServerPort = 8580;
    private final String applicationName = "WebSocketApplication";
    private final String testMessage = "Web Socket Test Message";
    private final String apiEndPoint = "ws://127.0.0.1:9099/echo/" + apiVersion;
    private APIPublisherRestClient apiPublisher;
    private String provider;
    private String consumerKey;
    private String consumerSecret;
    private APIRequest apiRequest;

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
        ServerConfigurationManager serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
        serverConfigurationManager.applyConfiguration(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "webSocketTest" + File.separator + "axis2.xml"));
        startWebSocketServer(webSocketServerPort);
    }

    @Test(description = "Publish WebSocket API")
    public void publishWebSocketAPI() throws Exception {
        apiPublisher = new APIPublisherRestClient(getPublisherURLHttp());
        apiStore = new APIStoreRestClient(getStoreURLHttp());
        provider = user.getUserName();
        String apiContext = "echo";
        URI endpointUri = new URI("ws://127.0.0.1:" + webSocketServerPort);

        //Create the api creation request object
        apiRequest = new APIRequest(apiName, apiContext, endpointUri, endpointUri);
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection("Unlimited");
        apiRequest.setProvider(provider);
        apiRequest.setWs("true");
        apiPublisher.login(user.getUserName(),
                user.getPassword());
        HttpResponse addAPIResponse = apiPublisher.addAPI(apiRequest);

        verifyResponse(addAPIResponse);
        //publishing API
        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiName, user.getUserName(),
                        APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        APIIdentifier apiIdentifierWebSocket = new APIIdentifier(provider, apiName, apiVersion);

        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

        waitForAPIDeploymentSync(user.getUserName(), apiName, apiVersion,
                APIMIntegrationConstants.IS_API_EXISTS);

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
        } catch (InterruptedException e) {
            log.error("Exception in connecting to server", e);
            assertTrue(false, "Client cannot connect to server");
        } finally {
            try {
                client.stop();
            } catch (Exception e) {
                log.error("Exception in disconnecting from server", e);
            }
        }
    }

    @Test(description = "Test Throttling for web socket API", dependsOnMethods = "testWebSocketAPIInvocation")
    public void testWebSocketAPIThrottling() throws Exception {
        //Deploy Throttling policy
        AdminDashboardRestClient adminDashboardRestClient = new AdminDashboardRestClient(getGatewayMgtURLHttps());
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
    public void testWebSocketAPIInvalidTokenInvocation() {
        WebSocketClient client = new WebSocketClient();
        try {
            invokeAPI(client, "00000000-0000-0000-0000-000000000000");
        } catch (APIManagerIntegrationTestException e) {
            log.error("Exception in connecting to server", e);
            assertTrue(true, "Client cannot connect to server");
        } catch (Exception e) {
            log.error("Exception in connecting to server", e);
        } finally {
            try {
                client.stop();
            } catch (Exception e) {
                log.error("Exception in disconnecting from server", e);
            }
        }
    }

    /**
     * Wait for client to receive reply from the server
     *
     * @param clientSocket WebSocket Client Object
     */
    private void waitForReply(ToUpperClientSocket clientSocket) {
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
                        factory.register(ToUpperWebSocket.class);
                    }
                };
                Server server = new Server(serverPort);
                server.setHandler(wsHandler);
                try {
                    server.start();
                } catch (InterruptedException ignore) {
                } catch (Exception e) {
                    log.error("Error while starting server ", e);
                }
            }

        });
    }

    /**
     * Test throttling by invoking API
     *
     * @param accessToken API accessToken
     */
    private void testThrottling(String accessToken) {
        int limit = 4;
        int numberOfIterations = 5;
        WebSocketClient client = new WebSocketClient();
        for (int count = 0; count < numberOfIterations; count++) {
            try {
                log.info("Number of time API Invoked : " + count);
                if (count == limit) {
                    Thread.sleep(10000);
                }
                if (count >= limit) {
                    ToUpperClientSocket socket = new ToUpperClientSocket();
                    client.start();
                    URI echoUri = new URI(apiEndPoint);
                    ClientUpgradeRequest request = new ClientUpgradeRequest();
                    request.setHeader("Authorization", "Bearer " + accessToken);
                    client.connect(socket, echoUri, request);
                    socket.getLatch().await(3, TimeUnit.SECONDS);
                    socket.sendMessage(testMessage);
                    waitForReply(socket);
                    assertEquals(socket.getResponseMessage(), "Websocket frame throttled out",
                            "Received response in not matching");
                } else {
                    invokeAPI(client, accessToken);
                }

            } catch (Exception ex) {
                log.error("Error occurred while calling API : " + ex);
                break;
            }
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

        ToUpperClientSocket socket = new ToUpperClientSocket();
        client.start();
        URI echoUri = new URI(apiEndPoint);
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        request.setHeader("Authorization", "Bearer " + accessToken);
        client.connect(socket, echoUri, request);
        socket.getLatch().await(3, TimeUnit.SECONDS);
        socket.sendMessage(testMessage);
        waitForReply(socket);
        assertEquals(StringUtils.isEmpty(socket.getResponseMessage()), false,
                "Client did not receive response from server");
        assertEquals(socket.getResponseMessage(), testMessage.toUpperCase(),
                "Received response in not matching");
        socket.setResponseMessage(null);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
        executorService.shutdownNow();
    }
}
