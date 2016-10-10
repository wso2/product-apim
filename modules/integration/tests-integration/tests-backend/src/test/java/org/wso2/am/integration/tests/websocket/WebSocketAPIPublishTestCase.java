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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.tests.websocket.client.ToUpperClientSocket;
import org.wso2.am.integration.tests.websocket.server.ToUpperWebSocket;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.xml.StringUtils;

import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class WebSocketAPIPublishTestCase extends APIMIntegrationBaseTest {
    private static final long WAIT_TIME = 30 * 1000;
    private String message1 = "WebSocketMessage1";
    private String message2 = "WebSocketMessage2";
    private final Log log = LogFactory.getLog(WebSocketAPIPublishTestCase.class);
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    private APIIdentifier apiIdentifierWebSocketTest;
    private APIPublisherRestClient apiPublisher;
    private String provider;
    private static final String API_NAME = "WebSocketAPI";
    private static final String API_VERSION = "1.0.0";
    private String applicationNameTest1 = "WebSocketApplication";

    @Factory(dataProvider = "userModeDataProvider")
    public WebSocketAPIPublishTestCase(TestUserMode userMode) {
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
        apiPublisher = new APIPublisherRestClient(getPublisherURLHttp());
        apiStore = new APIStoreRestClient(getStoreURLHttp());
        provider = user.getUserName();
        String apiContext = "echo";
        String endpointUri = "ws://localhost:8580";

        //Create the api creation request object
        APIRequest apiRequest = new APIRequest(API_NAME, apiContext, new URI(endpointUri));
        apiRequest.setVersion(API_VERSION);
        apiRequest.setTiersCollection("Unlimited");
        apiRequest.setTier("Unlimited");
        apiRequest.setProvider(provider);
        apiRequest.setWs("true");
        apiPublisher.login(user.getUserName(),
                user.getPassword());
        apiPublisher.addAPI(apiRequest);

        //publishing API
        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(API_NAME, user.getUserName(),
                        APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);
        apiIdentifierWebSocketTest = new APIIdentifier(provider, API_NAME, API_VERSION);

        String apiInvocationUrl = getAPIInvocationURLHttp(apiContext, API_VERSION);
        log.info("API Invocation URL " + apiInvocationUrl);

    }

    @Test(groups = {"wso2.am"}, description = "Web Socket test case")
    public void testWebSocketEchoMessageTestCase() throws Exception {

        executorService.execute(new Runnable() {
            public void run() {
                WebSocketHandler wsHandler = new WebSocketHandler() {
                    @Override
                    public void configure(WebSocketServletFactory factory) {
                        factory.register(ToUpperWebSocket.class);
                    }
                };
                Server server = new Server(8580);
                server.setHandler(wsHandler);
                try {
                    server.start();
                } catch (Exception e) {
                    log.error("Error in starting web socket server: ", e);
                    throw new RuntimeException(e);
                }
            }

        });

        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

        List<APIIdentifier> publisherAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiPublisher.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierWebSocketTest, publisherAPIList),
                "Published Api is visible in API Publisher.");

        List<APIIdentifier> storeAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiStore.getAllPublishedAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierWebSocketTest, storeAPIList),
                "Published Api is visible in API Store.");
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


        APPKeyRequestGenerator generateAppKeyRequestSandBox =
                new APPKeyRequestGenerator(applicationNameTest1);
        generateAppKeyRequestSandBox.setKeyType("SANDBOX");
        String responseSandBox = apiStore.generateApplicationKey
                (generateAppKeyRequestSandBox).getData();
        JSONObject jsonObject = new JSONObject(responseSandBox);

        String sandboxAccessToken =
                jsonObject.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        log.info("Sandbox token " + sandboxAccessToken);
        String dest = "ws://127.0.0.1:9099/echo/1.0.0";
        WebSocketClient client = new WebSocketClient();
        try {
            ToUpperClientSocket socket = new ToUpperClientSocket();
            client.start();
            URI echoUri = new URI(dest);
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            request.setHeader("Authorization", "Bearer " + sandboxAccessToken);
            client.connect(socket, echoUri, request);
            socket.getLatch().await(3, TimeUnit.SECONDS);
            socket.sendMessage(message1);
            waitForReply(socket, message1);
        } catch (Exception e) {
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

    public void waitForReply(ToUpperClientSocket clientSocket, String expectedMessage) {
        long currentTime = System.currentTimeMillis();
        long waitTime = currentTime + WAIT_TIME;
        while (StringUtils.isEmpty(clientSocket.getResponseMessage()) && waitTime > System.currentTimeMillis()) {
            try {
                log.info("Waiting for reply from server:");
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
        log.info("Client received response : *****" + clientSocket.getResponseMessage());
        assertEquals(StringUtils.isEmpty(clientSocket.getResponseMessage()), false, "Client did not receive server response");
        assertEquals(clientSocket.getResponseMessage(), expectedMessage.toUpperCase(), "Message format invalid");
        clientSocket.setResponseMessage(null);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        //super.cleanUp();
        executorService.shutdownNow();
    }
}
