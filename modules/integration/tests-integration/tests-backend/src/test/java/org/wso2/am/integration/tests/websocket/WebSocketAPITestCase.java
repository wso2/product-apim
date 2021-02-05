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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
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
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.token.TokenUtils;
import org.wso2.am.integration.tests.websocket.client.WebSocketClientImpl;
import org.wso2.am.integration.tests.websocket.server.WebSocketServerImpl;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.xml.StringUtils;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class WebSocketAPITestCase extends APIMIntegrationBaseTest {

    private final Log log = LogFactory.getLog(WebSocketAPITestCase.class);
    enum AUTH_IN {
        HEADER,
        QUERY
    }
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final String apiName = "WebSocketAPI";
    private final String applicationName = "WebSocketApplication";
    private final String applicationJWTName = "WebSocketJWTTypeApplication";
    private final String testMessage = "Web Socket Test Message";
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
    String appId;
    String appJWTId;

    @Factory(dataProvider = "userModeDataProvider")
    public WebSocketAPITestCase(TestUserMode userMode) {

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
        apiRequest.setTiersCollection("Unlimited");
        apiRequest.setProvider(provider);
        apiRequest.setType("WS");
        HttpResponse addAPIResponse = restAPIPublisher.addAPI(apiRequest);
         websocketAPIID = addAPIResponse.getData();
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

    @Test(description = "Create Application and subscribe", dependsOnMethods = "publishWebSocketAPI")
    public void testWebSocketAPIApplicationSubscription() throws Exception {
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
                "", APIMIntegrationConstants.API_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH);
        appId = applicationResponse.getData();
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(websocketAPIID, appId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);
        //Validate Subscription of the API
        Assert.assertEquals(subscriptionDTO.getStatus(), SubscriptionDTO.StatusEnum.UNBLOCKED);
    }

    @Test(description = "Invoke API using token", dependsOnMethods = "testWebSocketAPIApplicationSubscription")
    public void testWebSocketAPIInvocation() throws Exception {
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.REFRESH_CODE);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(appId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        String tokenJti = TokenUtils.getJtiOfJwtToken(accessToken);
        consumerKey = applicationKeyDTO.getConsumerKey();
        consumerSecret = applicationKeyDTO.getConsumerSecret();
        WebSocketClient client = new WebSocketClient();
        try {
            invokeAPI(client, tokenJti, AUTH_IN.HEADER);
            invokeAPI(client, tokenJti, AUTH_IN.QUERY);
        } catch (Exception e) {
            log.error("Exception in connecting to server", e);
            Assert.fail("Client cannot connect to server");
        } finally {
            client.stop();
        }
    }

    @Test(description = "Create JWT Type Application and subscribe", dependsOnMethods = "publishWebSocketAPI")
    public void testWebSocketAPIJWTApplicationSubscription() throws Exception {
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationJWTName,
                "", APIMIntegrationConstants.API_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.JWT);
        appJWTId = applicationResponse.getData();
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(websocketAPIID, appJWTId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);
        //Validate Subscription of the API
        Assert.assertEquals(subscriptionDTO.getStatus(), SubscriptionDTO.StatusEnum.UNBLOCKED);
    }

    @Test(description = "Invoke API using token", dependsOnMethods = "testWebSocketAPIJWTApplicationSubscription")
    public void testWebSocketAPIInvocationWithJWTToken() throws Exception {
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.REFRESH_CODE);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(appJWTId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        //consumerKey = applicationKeyDTO.getConsumerKey();
        //consumerSecret = applicationKeyDTO.getConsumerSecret();
        WebSocketClient client = new WebSocketClient();
        try {
            invokeAPI(client, accessToken, AUTH_IN.HEADER);
            invokeAPI(client, accessToken, AUTH_IN.QUERY);
        } catch (Exception e) {
            log.error("Exception in connecting to server", e);
            Assert.fail("Client cannot connect to server");
        } finally {
            client.stop();
        }
    }
    @Test(description = "Test Throttling for WebSocket API", dependsOnMethods = "testWebSocketAPIInvocation")
    public void testWebSocketAPIThrottling() throws Exception {
        // Deploy Throttling policy with throttle limit set as 8 frames. One message is two frames, therefore 4
        // messages can be sent.
        InputStream inputStream = new FileInputStream(getAMResourceLocation() + File.separator +
                "configFiles" + File.separator + "webSocketTest" + File.separator + "policy.json");

        //Extract the field values from the input stream
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonMap = mapper.readTree(inputStream);
        String policyName = jsonMap.get("policyName").textValue();
        String policyDescription = jsonMap.get("policyDescription").textValue();
        JsonNode defaultLimitJson = jsonMap.get("defaultLimit");
        JsonNode requestCountJson = defaultLimitJson.get("requestCount");
        Long requestCountLimit = Long.valueOf(String.valueOf(requestCountJson.get("requestCount")));
        String timeUnit = requestCountJson.get("timeUnit").textValue();
        Integer unitTime = Integer.valueOf(String.valueOf(requestCountJson.get("unitTime")));

        //Create the advanced throttling policy with request count quota type
        RequestCountLimitDTO requestCountLimitDTO = DtoFactory.createRequestCountLimitDTO(timeUnit, unitTime,
                requestCountLimit);
        ThrottleLimitDTO defaultLimit =
                DtoFactory.createThrottleLimitDTO(ThrottleLimitDTO.TypeEnum.REQUESTCOUNTLIMIT, requestCountLimitDTO,
                        null);
        AdvancedThrottlePolicyDTO bandwidthAdvancedPolicyDTO = DtoFactory
                .createAdvancedThrottlePolicyDTO(policyName, "", policyDescription, false, defaultLimit,
                        new ArrayList<>());

        //Add the advanced throttling policy
        ApiResponse<AdvancedThrottlePolicyDTO> addedPolicy =
                restAPIAdmin.addAdvancedThrottlingPolicy(bandwidthAdvancedPolicyDTO);
        //Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        AdvancedThrottlePolicyDTO addedAdvancedPolicyDTO = addedPolicy.getData();
        String apiPolicyId = addedAdvancedPolicyDTO.getPolicyId();
        Assert.assertNotNull(apiPolicyId, "The policy ID cannot be null or empty");

        //Update Throttling policy of the API
        HttpResponse response = restAPIPublisher.getAPI(websocketAPIID);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);
        apidto.setApiThrottlingPolicy("WebSocketTestThrottlingPolicy");
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto);
        Assert.assertEquals(updatedAPI.getApiThrottlingPolicy(), "WebSocketTestThrottlingPolicy");
        //Get an Access Token from the user who is logged into the API Store.
        URL tokenEndpointURL = new URL(getGatewayURLNhttp() + "token");
        String subsAccessTokenPayload = APIMTestCaseUtils.getPayloadForPasswordGrant(user.getUserName(),
                user.getPassword());
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
                "grant_type=refresh_token&refresh_token=" + refreshToken;
        accessTokenGenerationResponse = new JSONObject(
                apiStore.generateUserAccessKey(consumerKey, consumerSecret,
                        getAccessTokenFromRefreshTokenRequestBody,
                        tokenEndpointURL).getData());
        String userAccessToken = accessTokenGenerationResponse.getString("access_token");

        Assert.assertNotNull("Access Token not found " + accessTokenGenerationResponse, userAccessToken);
        String tokenJti = TokenUtils.getJtiOfJwtToken(userAccessToken);
        testThrottling(tokenJti);
    }

    @Test(description = "Invoke API using invalid token", dependsOnMethods = "testWebSocketAPIThrottling")
    public void testWebSocketAPIInvalidTokenInvocation() throws Exception {

        WebSocketClient client = new WebSocketClient();
        try {
            invokeAPI(client, "00000000-0000-0000-0000-000000000000", AUTH_IN.HEADER);
        } catch (APIManagerIntegrationTestException e) {
            log.error("Exception in connecting to server", e);
            assertTrue(true, "Client cannot connect to server");
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
     * Test throttling by invoking API with throttling policy
     *
     * @param accessToken API accessToken
     */
    private void testThrottling(String accessToken) throws Exception {

        /* Prevent API requests getting dispersed into two time units */
        while (LocalDateTime.now().getSecond() > 20) {
            Thread.sleep(5000L);
        }
        int startingDistinctUnitTime = LocalDateTime.now().getMinute();

        int limit = 2;
        WebSocketClient client = new WebSocketClient();
        WebSocketClientImpl socket = new WebSocketClientImpl();
        client.start();
        URI echoUri = new URI(apiEndPoint);
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        request.setHeader("Authorization", "Bearer " + accessToken);
        client.connect(socket, echoUri, request);
        socket.getLatch().await(3L, TimeUnit.SECONDS);
        // Send 3 WebSocket messages when throttle limit is 2.
        try {
            for (int count = 1; count <= limit + 1; count++) {
                if (count > limit) {
                    // Set time gap to allow throttle to take place
                    Thread.sleep(15000L);
                }
                socket.sendMessage(testMessage);
                waitForReply(socket);
                log.info("Count :" + count + " Message :" + socket.getResponseMessage());
                // At the 3rd message check frame is throttled out.
                if (count > limit) {
                    //If throttling testing time duration is dispersed into two separate unit times, repeat the test
                    if (LocalDateTime.now().getMinute() != startingDistinctUnitTime) {
                        //repeat the test
                        log.info("Repeating the test as throttling testing time duration is dispersed into two " +
                                "separate units of time");
                        testThrottling(accessToken);
                    }
                    assertEquals(socket.getResponseMessage(), "Websocket frame throttled out",
                            "Received response is not matching");
                }
                socket.setResponseMessage(null);
            }
        } catch (Exception ex) {
            log.error("Error occurred while calling API.", ex);
            Assert.fail("Client cannot connect to server");
        } finally {
            client.stop();
        }
    }

    /**
     * Invoke deployed API via WebSocketClient and wait for reply
     *
     * @param client      WebSocketClient object
     * @param accessToken API access Token
     * @param in location of the Auth header. {@code query} or {@code header}
     * @throws Exception If an error occurs while invoking WebSocket API
     */
    private void invokeAPI(WebSocketClient client, String accessToken, AUTH_IN in) throws Exception {

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

        serverConfigurationManager.restoreToLastConfiguration(false);
        executorService.shutdownNow();
        super.cleanUp();
    }
}
