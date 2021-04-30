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

package org.wso2.am.integration.tests.streamingapis.serversentevents;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
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
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.tests.streamingapis.StreamingApiTestUtils;
import org.wso2.am.integration.tests.streamingapis.serversentevents.client.SimpleSseReceiver;
import org.wso2.am.integration.tests.streamingapis.serversentevents.server.SseServlet;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.ALL})
public class ServerSentEventsAPITopicsTestCase extends APIMIntegrationBaseTest {

    private final Log log = LogFactory.getLog(ServerSentEventsAPITopicsTestCase.class);
    private static final String TEST_USER = "sseScopeTestUser";
    private static final String TEST_USER_PASSWORD = "sseScopeTestUserPassword";
    private static final String SSE_ROLE = "sseRole";
    private static final String GLOBAL_SCOPE = "sseglobalscope";
    private static final String LOCAL_SCOPE = "sselocalscope";
    private final String apiName = "SSETopicsAPI";
    private final String applicationName = "SSETopicsApplication";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private String apiContext;
    private String apiVersion;
    private URI endpointUri;
    private String apiEndpoint;
    private String provider;
    private APIRequest apiRequest;
    private int sseServerPort;
    private String sseServerHost;
    private SseServlet topic1Servlet;
    private SseServlet topic2Servlet;
    private SseServlet globalLocalScopedTopicServlet;
    private SseServlet globalScopedTopicServlet;
    private SseServlet localScopedTopicServlet;
    private SseServlet nonScopedTopicServlet;
    private Server sseServer;
    private SimpleSseReceiver sseReceiver;
    private ServerConfigurationManager serverConfigurationManager;
    private String apiId;
    private String appId;
    private String sharedScopeId;
    private String accessToken;
    private String consumerKey;
    private String consumerSecret;

    @Factory(dataProvider = "userModeDataProvider")
    public ServerSentEventsAPITopicsTestCase(TestUserMode userMode) {
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
        sseServerHost = InetAddress.getLocalHost().getHostName();
        int lowerPortLimit = 8080;
        int upperPortLimit = 9030;
        sseServerPort = StreamingApiTestUtils.getAvailablePort(lowerPortLimit, upperPortLimit, sseServerHost);
        if (sseServerPort == -1) {
            throw new APIManagerIntegrationTestException("No available port in the range " + lowerPortLimit + "-" +
                    upperPortLimit + " was found");
        }
        log.info("Selected port " + sseServerPort + " to start backend server");
        initializeSseServer(sseServerPort);
    }

    @Test(description = "Publish SSE API")
    public void testPublishSseApi() throws Exception {
        provider = user.getUserName();
        apiContext = "sse-topics";
        apiVersion = "1.0.0";

        endpointUri = new URI("http://" + sseServerHost + ":" + sseServerPort);

        apiRequest = new APIRequest(apiName, apiContext, endpointUri, endpointUri);
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.ASYNC_UNLIMITED);
        apiRequest.setProvider(provider);
        apiRequest.setType("SSE");
        HttpResponse addAPIResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = addAPIResponse.getData();
        String asyncApiDefinition = "{" +
                "   \"asyncapi\":\"2.0.0\"," +
                "   \"info\":{" +
                "      \"title\":\"" + apiRequest.getName() + "\"," +
                "      \"version\":\"" + apiRequest.getVersion() +
                "   \"}," +
                "   \"servers\":{" +
                "      \"production\":{" +
                "          \"url\":\"" + endpointUri.toString() + "\"," +
                "          \"protocol\":\"sse\"" +
                "      }" +
                "   }," +
                "   \"channels\":{" +
                "      \"/topic1\":{" +
                "          \"parameters\":{}," +
                "          \"subscribe\":{}" +
                "      }," +
                "      \"/topic2\":{" +
                "          \"parameters\":{}," +
                "          \"subscribe\":{}" +
                "      }" +
                "   }" +
                "}";
        restAPIPublisher.updateAsyncAPI(apiId, asyncApiDefinition);
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), apiName, apiVersion, APIMIntegrationConstants.IS_API_EXISTS);

        APIIdentifier apiIdentifier = new APIIdentifier(provider, apiName, apiVersion);

        // Replace port with inbound endpoint port
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode) || TestUserMode.SUPER_TENANT_USER.equals(userMode)) {
            apiEndpoint = getSuperTenantAPIInvocationURLHttp(apiContext, apiVersion);
        } else {
            apiEndpoint = getAPIInvocationURLHttp(apiContext, apiVersion);
        }
        log.info("API Endpoint URL" + apiEndpoint);
        APIListDTO apiPublisherAllAPIs = restAPIPublisher.getAllAPIs();
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiPublisherAllAPIs),
                "Published API is visible in API Publisher.");
        org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO restAPIStoreAllAPIs;
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            restAPIStoreAllAPIs = restAPIStore.getAllAPIs();
        } else {
            restAPIStoreAllAPIs = restAPIStore.getAllAPIs(user.getUserDomain());
        }
        assertTrue(APIMTestCaseUtils.isAPIAvailableInStore(apiIdentifier, restAPIStoreAllAPIs),
                "Published API is visible in API Store.");
    }

    @Test(description = "Create Application and subscribe", dependsOnMethods = "testPublishSseApi")
    public void testSseApiApplicationSubscription() throws Exception {
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
                "", APIMIntegrationConstants.API_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH);
        appId = applicationResponse.getData();
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, appId,
                APIMIntegrationConstants.API_TIER.ASYNC_UNLIMITED);
        // Validate Subscription of the API
        Assert.assertEquals(subscriptionDTO.getStatus(), SubscriptionDTO.StatusEnum.UNBLOCKED);
    }

    @Test(description = "Invoke API topics", dependsOnMethods = "testSseApiApplicationSubscription")
    public void testSseApiTopicsInvocation() throws Exception {
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.REFRESH_CODE);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(appId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        accessToken = applicationKeyDTO.getToken().getAccessToken();
        consumerKey = applicationKeyDTO.getConsumerKey();
        consumerSecret = applicationKeyDTO.getConsumerSecret();

        int sent;
        int received;

        topic1Servlet.setEventsSent(0);
        invokeSseApi("/topic1", accessToken, 30000);
        sent = topic1Servlet.getEventsSent();
        received = sseReceiver.getReceivedDataEventsCount();
        Assert.assertNotEquals(sent, 0);
        Assert.assertEquals(sent, received);

        topic2Servlet.setEventsSent(0);
        invokeSseApi("/topic2", accessToken, 30000);
        sent = topic2Servlet.getEventsSent();
        received = sseReceiver.getReceivedDataEventsCount();
        Assert.assertNotEquals(sent, 0);
        Assert.assertEquals(sent, received);
    }

    @Test(description = "Invoke API topics with scopes", dependsOnMethods = "testSseApiTopicsInvocation")
    public void testSseApiTopicsInvocationWithScopes() throws Exception {
        userManagementClient.addUser(TEST_USER, TEST_USER_PASSWORD, new String[]{}, null);
        userManagementClient.addRole(SSE_ROLE, new String[]{TEST_USER}, new String[]{});

        List<String> roles = new ArrayList<>();
        roles.add(SSE_ROLE);
        roles.add("admin");

        // Add the Global Scope
        ScopeDTO globalScopeDTO = new ScopeDTO();
        globalScopeDTO.setName(GLOBAL_SCOPE);
        globalScopeDTO.setDisplayName(GLOBAL_SCOPE);
        globalScopeDTO.setDescription(GLOBAL_SCOPE);
        globalScopeDTO.setBindings(roles);
        ScopeDTO addedScopeDTO = restAPIPublisher.addSharedScope(globalScopeDTO);
        sharedScopeId = addedScopeDTO.getId();
        Assert.assertNotNull(sharedScopeId, "The scope ID cannot be null or empty");

        // Set the Local scope
        ScopeDTO localScopeDTO = new ScopeDTO();
        localScopeDTO.setName(LOCAL_SCOPE);
        localScopeDTO.setDisplayName(LOCAL_SCOPE);
        localScopeDTO.setDescription(LOCAL_SCOPE);
        localScopeDTO.setBindings(roles);
        APIScopeDTO apiScopeDTO = new APIScopeDTO();
        apiScopeDTO.setScope(localScopeDTO);
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(createdApiResponse.getData(), APIDTO.class);
        apidto.setScopes(Collections.singletonList(apiScopeDTO));
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, apiId);
        Assert.assertNotNull(updatedAPI.getScopes(), "Updated API doesn't have any scopes");
        Assert.assertTrue(updatedAPI.getScopes().stream().anyMatch(s -> s != null && s.getScope() != null &&
                        LOCAL_SCOPE.equals(s.getScope().getName())),
                "Inserted API Scope is not present in the updated API DTO");
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), apiName, apiVersion, APIMIntegrationConstants.IS_API_EXISTS);

        // Update the Websocket API with topics
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

                // Topic with both global and local scopes
                "      \"/globallocal\":{" +
                "          \"parameters\":{}," +
                "          \"subscribe\":{" +
                "              \"x-scopes\":[\"" + GLOBAL_SCOPE + "\", \"" + LOCAL_SCOPE + "\"]" +
                "          }," +
                "          \"x-auth-type\":\"Any\"" +
                "      }," +
                // Topic with global scope
                "      \"/global\":{" +
                "          \"parameters\":{}," +
                "          \"subscribe\":{" +
                "              \"x-scopes\":[\"" + GLOBAL_SCOPE + "\"]" +
                "          }," +
                "          \"x-auth-type\":\"Any\"" +
                "      }," +
                // Topic with local scope
                "      \"/local\":{" +
                "          \"parameters\":{}," +
                "          \"subscribe\":{" +
                "              \"x-scopes\":[\"" + LOCAL_SCOPE + "\"]" +
                "          }," +
                "          \"x-auth-type\":\"Any\"" +
                "      }," +
                // Topic without scopes
                "      \"/noscopes\":{" +
                "          \"parameters\":{}," +
                "          \"subscribe\":{}" +
                "      }" +

                "   }," +

                "   \"components\":{\n" +
                "      \"securitySchemes\":{\n" +
                "         \"oauth2\":{\n" +
                "            \"type\":\"oauth2\",\n" +
                "            \"flows\":{\n" +
                "               \"implicit\":{\n" +
                "                  \"authorizationUrl\":\"http://localhost:9999\",\n" +
                "                  \"scopes\":{\n" +
                "                     \"" + GLOBAL_SCOPE + "\":\"\",\n" +
                "                     \"" + LOCAL_SCOPE + "\":\"\"\n" +
                "                  },\n" +
                "                  \"x-scopes-bindings\":{\n" +
                "                     \"" + GLOBAL_SCOPE + "\":\"" + SSE_ROLE + "\",\n" +
                "                     \"" + LOCAL_SCOPE + "\":\"" + SSE_ROLE + "\"\n" +
                "                  }\n" +
                "               }\n" +
                "            }\n" +
                "         }\n" +
                "      }\n" +
                "   }" +

                "}";

        restAPIPublisher.updateAsyncAPI(apiId, asyncApiDefinition);
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), apiName, apiVersion, APIMIntegrationConstants.IS_API_EXISTS);

        // Token without authorized scope
        String tokenWithoutScopes = accessToken;
        log.info("Access Token without scope: " + tokenWithoutScopes);

        // Token with global scope
        URL tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
        HttpResponse response;
        String requestBody;
        JSONObject accessTokenGenerationResponse;
        String username = TEST_USER;
        if (userMode != TestUserMode.SUPER_TENANT_ADMIN) {
            username = username.concat("@").concat(user.getUserDomain());
        }
        requestBody = "grant_type=password&username=" + username + "&password=" + TEST_USER_PASSWORD +
                "&scope=" + GLOBAL_SCOPE;
        response = restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL);
        accessTokenGenerationResponse = new JSONObject(response.getData());
        String tokenWithGlobalScope = accessTokenGenerationResponse.getString("access_token");
        log.info("Access Token with global scope: " + tokenWithGlobalScope);

        // Token with local scope
        requestBody = "grant_type=password&username=" + username + "&password=" + TEST_USER_PASSWORD +
                "&scope=" + LOCAL_SCOPE;
        response = restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL);
        accessTokenGenerationResponse = new JSONObject(response.getData());
        String tokenWithLocalScope = accessTokenGenerationResponse.getString("access_token");
        log.info("Access Token with local scope: " + tokenWithLocalScope);

        // Token with both local and global scopes
        requestBody = "grant_type=password&username=" + username + "&password=" + TEST_USER_PASSWORD +
                "&scope=" + LOCAL_SCOPE + " " + GLOBAL_SCOPE;
        response = restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL);
        accessTokenGenerationResponse = new JSONObject(response.getData());
        String tokenWithGlobalAndLocalScopes = accessTokenGenerationResponse.getString("access_token");
        log.info("Access Token with global & local scopes: " + tokenWithGlobalAndLocalScopes);

        int sent;
        int received;

        // Invoke the topic that has a global scope

        String topic = "/global";

        globalScopedTopicServlet.setEventsSent(0);
        invokeSseApi(topic, tokenWithoutScopes, 30000);
        received = sseReceiver.getReceivedDataEventsCount();
        Assert.assertEquals(received, 0);

        globalScopedTopicServlet.setEventsSent(0);
        invokeSseApi(topic, tokenWithLocalScope, 30000);
        received = sseReceiver.getReceivedDataEventsCount();
        Assert.assertEquals(received, 0);

        globalScopedTopicServlet.setEventsSent(0);
        invokeSseApi(topic, tokenWithGlobalScope, 30000);
        sent = globalScopedTopicServlet.getEventsSent();
        received = sseReceiver.getReceivedDataEventsCount();
        Assert.assertNotEquals(sent, 0);
        Assert.assertEquals(sent, received);

        globalScopedTopicServlet.setEventsSent(0);
        invokeSseApi(topic, tokenWithGlobalAndLocalScopes, 30000);
        sent = globalScopedTopicServlet.getEventsSent();
        received = sseReceiver.getReceivedDataEventsCount();
        Assert.assertNotEquals(sent, 0);
        Assert.assertEquals(sent, received);

        // Invoke the topic that has a local scope

        topic = "/local";

        localScopedTopicServlet.setEventsSent(0);
        invokeSseApi(topic, tokenWithoutScopes, 30000);
        received = sseReceiver.getReceivedDataEventsCount();
        Assert.assertEquals(received, 0);

        localScopedTopicServlet.setEventsSent(0);
        invokeSseApi(topic, tokenWithGlobalScope, 30000);
        received = sseReceiver.getReceivedDataEventsCount();
        Assert.assertEquals(received, 0);

        localScopedTopicServlet.setEventsSent(0);
        invokeSseApi(topic, tokenWithLocalScope, 30000);
        sent = localScopedTopicServlet.getEventsSent();
        received = sseReceiver.getReceivedDataEventsCount();
        Assert.assertNotEquals(sent, 0);
        Assert.assertEquals(sent, received);

        localScopedTopicServlet.setEventsSent(0);
        invokeSseApi(topic, tokenWithGlobalAndLocalScopes, 30000);
        sent = localScopedTopicServlet.getEventsSent();
        received = sseReceiver.getReceivedDataEventsCount();
        Assert.assertNotEquals(sent, 0);
        Assert.assertEquals(sent, received);

        // Invoke the topic that has both global and local scopes

        topic = "/globallocal";

        globalLocalScopedTopicServlet.setEventsSent(0);
        invokeSseApi(topic, tokenWithoutScopes, 30000);
        received = sseReceiver.getReceivedDataEventsCount();
        Assert.assertEquals(received, 0);

        globalLocalScopedTopicServlet.setEventsSent(0);
        invokeSseApi(topic, tokenWithGlobalScope, 30000);
        sent = globalLocalScopedTopicServlet.getEventsSent();
        received = sseReceiver.getReceivedDataEventsCount();
        Assert.assertNotEquals(sent, 0);
        Assert.assertEquals(sent, received);

        globalLocalScopedTopicServlet.setEventsSent(0);
        invokeSseApi(topic, tokenWithLocalScope, 30000);
        sent = globalLocalScopedTopicServlet.getEventsSent();
        received = sseReceiver.getReceivedDataEventsCount();
        Assert.assertNotEquals(sent, 0);
        Assert.assertEquals(sent, received);

        globalLocalScopedTopicServlet.setEventsSent(0);
        invokeSseApi(topic, tokenWithGlobalAndLocalScopes, 30000);
        sent = globalLocalScopedTopicServlet.getEventsSent();
        received = sseReceiver.getReceivedDataEventsCount();
        Assert.assertNotEquals(sent, 0);
        Assert.assertEquals(sent, received);

        // Invoke the topic that doesn't have a scope

        topic = "/noscopes";

        nonScopedTopicServlet.setEventsSent(0);
        invokeSseApi(topic, tokenWithoutScopes, 30000);
        sent = nonScopedTopicServlet.getEventsSent();
        received = sseReceiver.getReceivedDataEventsCount();
        Assert.assertNotEquals(sent, 0);
        Assert.assertEquals(sent, received);

        nonScopedTopicServlet.setEventsSent(0);
        invokeSseApi(topic, tokenWithGlobalScope, 30000);
        sent = nonScopedTopicServlet.getEventsSent();
        received = sseReceiver.getReceivedDataEventsCount();
        Assert.assertNotEquals(sent, 0);
        Assert.assertEquals(sent, received);

        nonScopedTopicServlet.setEventsSent(0);
        invokeSseApi(topic, tokenWithLocalScope, 30000);
        sent = nonScopedTopicServlet.getEventsSent();
        received = sseReceiver.getReceivedDataEventsCount();
        Assert.assertNotEquals(sent, 0);
        Assert.assertEquals(sent, received);

        nonScopedTopicServlet.setEventsSent(0);
        invokeSseApi(topic, tokenWithGlobalAndLocalScopes, 30000);
        sent = nonScopedTopicServlet.getEventsSent();
        received = sseReceiver.getReceivedDataEventsCount();
        Assert.assertNotEquals(sent, 0);
        Assert.assertEquals(sent, received);
    }

    private void invokeSseApi(String topic, String bearerToken, long runForMillis) throws Exception {
        startAndStopSseServer(runForMillis);
        Thread.sleep(5000);
        startSseReceiver(bearerToken, topic);
    }

    private void startAndStopSseServer(long stopAfterMillis) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    sseServer.start();
                    log.info("SSE Server Started and will be stopped after: " + stopAfterMillis + "ms.");
                    Thread.sleep(stopAfterMillis);
                    sseServer.stop();
                    log.info("SSE Server Stopped.");
                } catch (Exception e) {
                    log.error("Failed to start/stop the SSE server.", e);
                }
            }
        });
    }

    private void startSseReceiver(String bearerToken, String topic) {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(apiEndpoint + topic);
        sseReceiver = new SimpleSseReceiver(target, bearerToken);
        try {
            sseReceiver.open();
        } finally {
            sseReceiver.close(); // This will be called when sseServer.stop() is called
        }
    }

    private void initializeSseServer(int port) {
        Server server = new Server(port);
        ServletHandler servletHandler = new ServletHandler();
        server.setHandler(servletHandler);

        topic1Servlet = new SseServlet();
        ServletHolder topic1ServletHolder = new ServletHolder(topic1Servlet);
        servletHandler.addServletWithMapping(topic1ServletHolder, "/topic1");
        topic2Servlet = new SseServlet();
        ServletHolder topic2ServletHolder = new ServletHolder(topic2Servlet);
        servletHandler.addServletWithMapping(topic2ServletHolder, "/topic2");

        globalLocalScopedTopicServlet = new SseServlet();
        ServletHolder globalLocalScopedTopicServletHolder = new ServletHolder(globalLocalScopedTopicServlet);
        servletHandler.addServletWithMapping(globalLocalScopedTopicServletHolder, "/globallocal");
        globalScopedTopicServlet = new SseServlet();
        ServletHolder globalScopedTopicServletHolder = new ServletHolder(globalScopedTopicServlet);
        servletHandler.addServletWithMapping(globalScopedTopicServletHolder, "/global");
        localScopedTopicServlet = new SseServlet();
        ServletHolder localScopedTopicServletHolder = new ServletHolder(localScopedTopicServlet);
        servletHandler.addServletWithMapping(localScopedTopicServletHolder, "/local");
        nonScopedTopicServlet = new SseServlet();
        ServletHolder nonScopedTopicServletHolder = new ServletHolder(nonScopedTopicServlet);
        servletHandler.addServletWithMapping(nonScopedTopicServletHolder, "/noscopes");

        sseServer = server;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(appId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        serverConfigurationManager.restoreToLastConfiguration(false);
        executorService.shutdownNow();
        super.cleanUp();
    }
}
