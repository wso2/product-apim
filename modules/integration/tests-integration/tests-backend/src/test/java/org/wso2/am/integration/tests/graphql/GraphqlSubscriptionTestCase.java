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

package org.wso2.am.integration.tests.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLValidationResponseDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.GraphQLValidationResponseGraphQLInfoDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.tests.graphql.websocket.client.SubscriptionWSClientImpl;
import org.wso2.am.integration.tests.graphql.websocket.server.SubscriptionServerCreator;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.xml.StringUtils;

import javax.ws.rs.core.Response;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class GraphqlSubscriptionTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(GraphqlSubscriptionTestCase.class);
    private final String applicationName = "GraphQLSubApplication";
    private final String API_NAME = "SnowtoothGraphQLSubAPI";
    private final String API_CONTEXT = "snowtooth";
    private final String API_VERSION = "1.0.0";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private int webSocketServerPort;
    private String webSocketServerHost;
    private String graphqlApiId;
    private String apiEndPoint;
    String appJWTId;

    private enum AUTH_IN {
        HEADER,
        QUERY
    }

    @Factory(dataProvider = "userModeDataProvider")
    public GraphqlSubscriptionTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN}
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
        log.info("Selected port " + webSocketServerPort + " to start graphql subscription backend server");
        startGraphQLSubscriptionServer(webSocketServerPort);

    }

    @Test(description = "Publish GraphQL API with Subscriptions")
    public void publishGraphQLAPIWithSubscriptions() throws Exception {

        String schemaDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("graphql" + File.separator + "subscriptions"
                        + File.separator + "schema.graphql"), "UTF-8");
        File file = getTempFileWithContent(schemaDefinition);
        GraphQLValidationResponseDTO responseApiDto = restAPIPublisher.validateGraphqlSchemaDefinition(file);
        GraphQLValidationResponseGraphQLInfoDTO graphQLInfo = responseApiDto.getGraphQLInfo();
        String arrayToJson = new ObjectMapper().writeValueAsString(graphQLInfo.getOperations());
        JSONArray operations = new JSONArray(arrayToJson);

        ArrayList<String> environment = new ArrayList<>();
        environment.add(Constants.GATEWAY_ENVIRONMENT);

        ArrayList<String> policies = new ArrayList<String>();
        policies.add("Unlimited");

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("name", API_NAME);
        additionalPropertiesObj.put("context", API_CONTEXT);
        additionalPropertiesObj.put("version", API_VERSION);

        JSONObject url = new JSONObject();
        url.put("url", "http://" + webSocketServerHost + ":" + webSocketServerPort);
        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("sandbox_endpoints", url);
        endpointConfig.put("production_endpoints", url);
        additionalPropertiesObj.put("endpointConfig", endpointConfig);
        additionalPropertiesObj.put("policies", policies);
        additionalPropertiesObj.put("operations", operations);

        // create Graphql API
        APIDTO apidto = restAPIPublisher.importGraphqlSchemaDefinition(file, additionalPropertiesObj.toString());
        graphqlApiId = apidto.getId();
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(graphqlApiId);
        System.out.println(createdApiResponse.getData());
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode(),
                API_NAME + " API creation is failed");
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(graphqlApiId, restAPIPublisher);
        // publish api
        restAPIPublisher.changeAPILifeCycleStatus(graphqlApiId, Constants.PUBLISHED);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION,
                APIMIntegrationConstants.IS_API_EXISTS);

        // replace port with inbound endpoint port
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode) || TestUserMode.SUPER_TENANT_USER.equals(userMode)) {
            apiEndPoint = getWebSocketAPIInvocationURL(API_CONTEXT, API_VERSION);
        } else {
            apiEndPoint = getWebSocketTenantAPIInvocationURL(API_CONTEXT, API_VERSION, user.getUserDomain());
        }
        log.info("API Endpoint URL" + apiEndPoint);
        APIIdentifier apiIdentifierWebSocket = new APIIdentifier(user.getUserName(), API_NAME, API_VERSION);
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
                "Published API is visible in Dev Portal.");
    }

    @Test(description = "Create JWT Type Application and subscribe",
            dependsOnMethods = "publishGraphQLAPIWithSubscriptions")
    public void testGraphQLAPIJWTApplicationSubscription() throws Exception {

        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
                "", APIMIntegrationConstants.API_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.JWT);
        appJWTId = applicationResponse.getData();
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(graphqlApiId, appJWTId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);
        //Validate Subscription of the API
        Assert.assertEquals(subscriptionDTO.getStatus(), SubscriptionDTO.StatusEnum.UNBLOCKED);
    }

    @Test(description = "Invoke Subscriptions using token", dependsOnMethods =
            "testGraphQLAPIJWTApplicationSubscription")
    public void testWebSocketAPIInvocationWithJWTToken() throws Exception {

        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.REFRESH_CODE);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(appJWTId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        WebSocketClient client = new WebSocketClient();
        try {
            invokeGraphQLSubscriptionSuccess(client, accessToken, AUTH_IN.HEADER);
            invokeGraphQLSubscriptionSuccess(client, accessToken, AUTH_IN.QUERY);
        } catch (Exception e) {
            log.error("Exception in connecting to server", e);
            Assert.fail("Client cannot connect to server");
        } finally {
            client.stop();
        }
    }

    /**
     * Invoke deployed API via GraphQL Websocket client and wait for success reply (happy path)
     *
     * @param client      WebSocketClient object
     * @param accessToken API access Token
     * @param in          location of the Auth header. {@code query} or {@code header}
     * @throws Exception If an error occurs while invoking WebSocket API
     */
    private void invokeGraphQLSubscriptionSuccess(WebSocketClient client, String accessToken, AUTH_IN in)
            throws Exception {

        SubscriptionWSClientImpl socket = new SubscriptionWSClientImpl();
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
            String textMessage = "";
            //Send connection init message
            textMessage = "{\"type\":\"connection_init\",\"payload\":{}}";
            socket.sendMessage(textMessage);
            waitForReply(socket);
            assertFalse(StringUtils.isEmpty(socket.getResponseMessage()),
                    "Client did not receive response from server");
            assertEquals(socket.getResponseMessage(), "{\"type\":\"connection_ack\"}",
                    "Received response in not a Connection Ack response");
            socket.setResponseMessage(null);
            //Send graphQL subscription request message
            textMessage = "{\"id\":\"1\",\"type\":\"start\",\"payload\":{\"variables\":{},\"extensions\":{},"
                    + "\"operationName\":null,\"query\":"
                    + "\"subscription {\\n  liftStatusChange {\\n    name\\n  }\\n}\\n\"}}";
            socket.sendMessage(textMessage);
            waitForReply(socket);
            assertFalse(StringUtils.isEmpty(socket.getResponseMessage()),
                    "Client did not receive response from server");
            assertEquals(socket.getResponseMessage(), "{\"type\":\"data\",\"id\":\"1\",\"payload\":{\"data\":"
                            + "{\"liftStatusChange\":{\"name\":\"Astra Express\"}}}}",
                    "Received response in not a lift status change sub topic event response");
            socket.setResponseMessage(null);
        } else {
            throw new APIManagerIntegrationTestException("Unable to create client connection");
        }
    }

    /**
     * Starts backend graphQL web socket server in given port
     *
     * @param serverPort Port that WebSocket Server starts
     */
    private void startGraphQLSubscriptionServer(final int serverPort) {

        executorService.execute(() -> {

            WebSocketHandler wsHandler = new WebSocketHandler() {
                @Override
                public void configure(WebSocketServletFactory factory) {

                    factory.setCreator(new SubscriptionServerCreator());
                }
            };

            Server server = new Server(serverPort);
            server.setHandler(wsHandler);
            try {
                server.start();
                log.info("GraphQL WebSocket backend server started at port: " + serverPort);
            } catch (InterruptedException ignore) {
            } catch (Exception e) {
                log.error("Error while starting graphql backend server at port: " + serverPort, e);
                Assert.fail("Cannot start GraphQL WebSocket server");
            }
        });
    }

    /**
     * Find a free port to start backend WebSocket server in given port range
     *
     * @param lowerPortLimit from port number
     * @param upperPortLimit to port number
     * @return Available Port Number
     */
    protected int getAvailablePort(int lowerPortLimit, int upperPortLimit) {

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
            s = new Socket("localhost", port);
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

    private File getTempFileWithContent(String schema) throws Exception {
        File temp = File.createTempFile("schema", ".graphql");
        temp.deleteOnExit();
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.write(schema);
        out.close();
        return temp;
    }

    /**
     * Wait for client to receive reply from the server
     *
     * @param clientSocket WebSocket Client Object
     */
    private void waitForReply(SubscriptionWSClientImpl clientSocket) {

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

}
