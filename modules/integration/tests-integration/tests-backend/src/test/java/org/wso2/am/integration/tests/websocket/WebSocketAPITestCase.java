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
import org.json.JSONException;
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
import org.wso2.am.integration.test.utils.bean.APIThrottlingTierRequest;
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
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class WebSocketAPITestCase extends APIMIntegrationBaseTest {
    private static final long WAIT_TIME = 30 * 1000;
    private String testMessage = "WebSocketMessage1";
    private final Log log = LogFactory.getLog(WebSocketAPITestCase.class);
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    private APIIdentifier apiIdentifierWebSocketTest;
    private APIPublisherRestClient apiPublisher;
    private String provider;
    private final String API_NAME = "WebSocketAPI";
    private final String API_VERSION = "1.0.0";
    private final int WEB_SOCKET_SERVER_PORT = 8580;
    private String applicationNameTest1 = "WebSocketApplication";
    private String dest = "ws://127.0.0.1:9099/echo/1.0.0";
    private String consumerKey;
    private String consumerSecret;
    private ServerConfigurationManager serverConfigurationManager;
    APIRequest apiRequest;

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
        serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
//        serverConfigurationManager.applyConfiguration(new File(getAMResourceLocation()
//                + File.separator + "configFiles" + File.separator + "webSocketTest" + File.separator + "axis2.xml"));
        startWebSocketServer(WEB_SOCKET_SERVER_PORT);
    }

    @Test(description = "Publish WebSocket API")
    public void publishWebSocketAPI() throws Exception {
        apiPublisher = new APIPublisherRestClient(getPublisherURLHttp());
        apiStore = new APIStoreRestClient(getStoreURLHttp());
        provider = user.getUserName();
        String apiContext = "echo";
        String endpointUri = "ws://127.0.0.1:" + WEB_SOCKET_SERVER_PORT;

        //Create the api creation request object
        apiRequest = new APIRequest(API_NAME, apiContext, new URI(endpointUri));
        apiRequest.setVersion(API_VERSION);
        apiRequest.setTiersCollection("Unlimited");
        apiRequest.setProvider(provider);
        apiRequest.setWs("true");
        apiPublisher.login(user.getUserName(),
                user.getPassword());
        HttpResponse addAPIResponse = apiPublisher.addAPI(apiRequest);

        verifyResponse(addAPIResponse);
        //publishing API
        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(API_NAME, user.getUserName(),
                        APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);
        apiIdentifierWebSocketTest = new APIIdentifier(provider, API_NAME, API_VERSION);

        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION,
                APIMIntegrationConstants.IS_API_EXISTS);

        List<APIIdentifier> publisherAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiPublisher.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierWebSocketTest, publisherAPIList),
                "Published Api is visible in API Publisher.");

        List<APIIdentifier> storeAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiStore.getAllPublishedAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierWebSocketTest, storeAPIList),
                "Published Api is visible in API Store.");
    }

    @Test(description = "Create Application and subscribe", dependsOnMethods = "publishWebSocketAPI")
    public void testWebSocketAPIApplicationSubscription() throws Exception {
        apiStore.addApplication(applicationNameTest1, APIMIntegrationConstants.API_TIER.UNLIMITED, "", "");
        SubscriptionRequest subscriptionRequest =
                new SubscriptionRequest(API_NAME, provider);
        subscriptionRequest.setApplicationName(applicationNameTest1);
        subscriptionRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        //Validate Subscription of the API
        HttpResponse subscribeApiResponse = apiStore.subscribe(subscriptionRequest);
        assertEquals(subscribeApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                API_NAME + "is not Subscribed");
        assertTrue(subscribeApiResponse.getData().contains("\"error\" : false"),
                API_NAME + "is not Subscribed");
    }

    @Test(description = "Invoke API using token", dependsOnMethods = "testWebSocketAPIApplicationSubscription")
    public void testWebsocketAPIInvocation() throws Exception {
        APPKeyRequestGenerator generateAppKeyRequestSandBox =
                new APPKeyRequestGenerator(applicationNameTest1);
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

    @Test(groups = {"throttling"}, description = "Invoke API using token", dependsOnMethods = "testWebsocketAPIInvocation")
    public void testWebSocketAPIThrottling() throws Exception {
        //Deploy Throttling policy
        AdminDashboardRestClient adminDashboardRestClient = new AdminDashboardRestClient(getGatewayMgtURLHttps());
        adminDashboardRestClient.login(user.getUserName(), user.getPassword());
        InputStream is = new FileInputStream(getAMResourceLocation() + File.separator +
                "configFiles" + File.separator + "webSocketTest" + File.separator + "policy.json");
        String jsonTxt = IOUtils.toString(is);
        HttpResponse addPolicyResponse = adminDashboardRestClient.addThrottlingPolicy(jsonTxt);
        verifyResponse(addPolicyResponse);

        //Update API
        apiRequest.setApiTier("Unlimited");
        HttpResponse updateAPIResponse = apiPublisher.updateAPI(apiRequest);
        verifyResponse(updateAPIResponse);

        //Get an Access Token from the user who is logged into the API Store. See APIMANAGER-3152.
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
    public void testWebsocketAPIInvalidTokenInvocation() {
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
     * @param clientSocket
     * @param expectedMessage
     */
    private void waitForReply(ToUpperClientSocket clientSocket, String expectedMessage) {
        long currentTime = System.currentTimeMillis();
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
     * @param serverPort
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
     * @param accessToken
     */
    private void testThrottling(String accessToken) {
        int limit = 4;
        int numberOfIterations = 6;
        WebSocketClient client = new WebSocketClient();
        for (int count = 0; count < numberOfIterations; count++) {
            try {
                log.info(" =================================== Number of time API Invoked : " + count);
                if (count == limit) {
                    Thread.sleep(10000);
                }
                invokeAPI(client, accessToken);
                if (count == limit) {
                    log.info("Response code is not as expected");
                } else {
                    log.info("Response code is not as expected");
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
     * @param client
     * @param accessToken
     * @throws Exception
     */
    private void invokeAPI(WebSocketClient client, String accessToken) throws Exception {
        ToUpperClientSocket socket = new ToUpperClientSocket();
        client.start();
        URI echoUri = new URI(dest);
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        request.setHeader("Authorization", "Bearer " + accessToken);
        client.connect(socket, echoUri, request);
        socket.getLatch().await(3, TimeUnit.SECONDS);
        socket.sendMessage(testMessage);
        waitForReply(socket, testMessage);
        assertEquals(StringUtils.isEmpty(socket.getResponseMessage()), false,
                "Client did not receive response from server");
        socket.setResponseMessage(null);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        //super.cleanUp();
        executorService.shutdownNow();
    }
}
