/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.streamingapis.serversentevents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.AdvancedThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.EventCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.TopicListDTO;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.token.TokenUtils;
import org.wso2.am.integration.tests.streamingapis.StreamingApiTestUtils;
import org.wso2.am.integration.tests.streamingapis.serversentevents.client.SimpleSseReceiver;
import org.wso2.am.integration.tests.streamingapis.serversentevents.server.SseServlet;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class ServerSentEventsAPITestCase extends APIMIntegrationBaseTest {

    private final Log log = LogFactory.getLog(ServerSentEventsAPITestCase.class);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private String sseEventPublisherSource = TestConfigurationProvider.getResourceLocation() + File.separator +
            "artifacts" + File.separator + "AM" + File.separator + "configFiles" + File.separator + "streamingAPIs" +
            File.separator + "serverSentEventsTest" + File.separator;
    private String sseRequestEventPublisherSource = "SSE_Req_Logger.xml";
    private String sseThrottleOutEventPublisherSource = "SSE_Throttle_Out_Logger.xml";
    private String sseEventPublisherTarget = FrameworkPathUtil.getCarbonHome() + File.separator + "repository" +
            File.separator + "deployment" + File.separator + "server" + File.separator + "eventpublishers" +
            File.separator;

    private String apiName = "SSEAPI";
    private String applicationName = "SSEApplication";

    private ServerConfigurationManager serverConfigurationManager;
    private String apiContext;
    private String apiVersion;
    private String provider;
    private APIRequest apiRequest;
    private int sseServerPort;
    private String sseServerHost;
    private String apiId;
    private String appId;
    private String throttleAppId;
    private SseServlet sseServlet;
    private Server sseServer;
    private SimpleSseReceiver sseReceiver;
    private String apiEndpoint;

    @Factory(dataProvider = "userModeDataProvider")
    public ServerSentEventsAPITestCase(TestUserMode userMode) {
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
        serverConfigurationManager.applyConfigurationWithoutRestart
                (new File(sseEventPublisherSource + sseRequestEventPublisherSource),
                        new File(sseEventPublisherTarget + sseRequestEventPublisherSource), false);
        serverConfigurationManager.applyConfigurationWithoutRestart
                (new File(sseEventPublisherSource + sseThrottleOutEventPublisherSource),
                        new File(sseEventPublisherTarget + sseThrottleOutEventPublisherSource), false);
        sseServerHost = InetAddress.getLocalHost().getHostName();
        int lowerPortLimit = 8080;
        int upperPortLimit = 8090;
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
        apiContext = "sse";
        apiVersion = "1.0.0";

        URI endpointUri = new URI("http://" + sseServerHost + ":" + sseServerPort);

        //Create the api creation request object
        apiRequest = new APIRequest(apiName, apiContext, endpointUri, endpointUri);
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.ASYNC_UNLIMITED);
        apiRequest.setProvider(provider);
        apiRequest.setType("SSE");
        HttpResponse addAPIResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = addAPIResponse.getData();
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);
        waitForAPIDeploymentSync(user.getUserName(), apiName, apiVersion, APIMIntegrationConstants.IS_API_EXISTS);

        APIIdentifier apiIdentifier = new APIIdentifier(provider, apiName, apiVersion);

        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode) || TestUserMode.SUPER_TENANT_USER.equals(userMode)) {
            apiEndpoint = getSuperTenantAPIInvocationURLHttp(apiContext, apiVersion);
        } else {
            apiEndpoint = getAPIInvocationURLHttp(apiContext, apiVersion);
        }

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

    @Test(description = "check for topics of a SSE api", dependsOnMethods = "testSseApiApplicationSubscription")
    public void testTopicRetrievalofSSEApi() throws  Exception {
        HttpResponse topicResponse = restAPIStore.getTopics(apiId, user.getUserDomain());
        Gson g = new Gson();
        TopicListDTO topicListDTO = g.fromJson(topicResponse.getData(), TopicListDTO.class);
        Assert.assertEquals(topicListDTO.getCount().intValue(), 1);

    }

    @Test(description = "Invoke SSE API", dependsOnMethods = "testSseApiApplicationSubscription")
    public void testInvokeSseApi() throws Exception {
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.REFRESH_CODE);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(appId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        invokeSseApi(accessToken, 30000);

        int sent = sseServlet.getEventsSent();
        int received = sseReceiver.getReceivedDataEventsCount();
        Assert.assertNotEquals(sent, 0);
        Assert.assertEquals(sent, received);

        sseServlet.setEventsSent(0);
        sseReceiver.setReceivedDataEventsCount(0);
    }

    @Test(description = "Invoke SSE API with invalid token", dependsOnMethods = "testInvokeSseApi")
    public void testInvokeSseApiWithInvalidToken() throws Exception {
        invokeSseApi("00000000-0000-0000-0000-000000000000", 30000);
        int received = sseReceiver.getReceivedDataEventsCount();
        Assert.assertEquals(received, 0);

        sseServlet.setEventsSent(0);
        sseReceiver.setReceivedDataEventsCount(0);
    }

    @Test(description = "Invoke SSE API with custom throttling policy", dependsOnMethods = "testInvokeSseApiWithInvalidToken")
    public void testSseApiThrottling() throws Exception {
        InputStream inputStream = new FileInputStream(getAMResourceLocation() + File.separator +
                "configFiles" + File.separator + "streamingAPIs" + File.separator + "serverSentEventsTest" +
                File.separator + "policy.json");

        // Extract the field values from the input stream
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonMap = mapper.readTree(inputStream);
        String policyName = jsonMap.get("policyName").textValue();
        String policyDescription = jsonMap.get("policyDescription").textValue();
        JsonNode defaultLimitJson = jsonMap.get("defaultLimit");
        JsonNode requestCountJson = defaultLimitJson.get("requestCount");
        Long requestCountLimit = Long.valueOf(String.valueOf(requestCountJson.get("requestCount")));
        String timeUnit = requestCountJson.get("timeUnit").textValue();
        Integer unitTime = Integer.valueOf(String.valueOf(requestCountJson.get("unitTime")));

        // Create the advanced throttling policy with request count quota type
        EventCountLimitDTO eventCountLimitDTO = DtoFactory.createEventCountLimitDTO(timeUnit, unitTime,
                requestCountLimit);
        ThrottleLimitDTO defaultLimit = DtoFactory.createEventCountThrottleLimitDTO(eventCountLimitDTO);
        SubscriptionThrottlePolicyDTO subscriptionThrottlingPolicy = DtoFactory
                .createSubscriptionThrottlePolicyDTO(policyName, policyName, policyDescription, false, defaultLimit,
                        0, 0, 0, "min", Collections.emptyList(), true, "free", 2);

        // Add the subscription throttle policy
        ApiResponse<SubscriptionThrottlePolicyDTO> addedPolicy =
                restAPIAdmin.addSubscriptionThrottlingPolicy(subscriptionThrottlingPolicy);
        // Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        SubscriptionThrottlePolicyDTO addedSubscriptionPolicyDTO = addedPolicy.getData();
        String apiPolicyId = addedSubscriptionPolicyDTO.getPolicyId();
        Assert.assertNotNull(apiPolicyId, "The policy ID cannot be null or empty");

        // Update Throttling policy of the API
        HttpResponse response = restAPIPublisher.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);
        List<String> policies = new ArrayList<>();
        policies.add(policyName);
        policies.add("AsyncUnlimited");
        apidto.setPolicies(policies);
        restAPIPublisher.updateAPI(apidto, apiId);
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), apiName, apiVersion, APIMIntegrationConstants.IS_API_EXISTS);
        APIDTO api = restAPIPublisher.getAPIByID(apiId);
        Assert.assertNotNull(api.getPolicies());
        Assert.assertTrue(api.getPolicies().contains(policyName));

        // Subscribe to the API using a new application with custom policy as the tier
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName + "ThrottleTest",
                "", APIMIntegrationConstants.API_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH);
        throttleAppId = applicationResponse.getData();
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, throttleAppId, policyName);
        // Validate Subscription of the API
        Assert.assertEquals(subscriptionDTO.getStatus(), SubscriptionDTO.StatusEnum.UNBLOCKED);
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.REFRESH_CODE);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(throttleAppId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        testThrottling(accessToken);
        sseServlet.setEventsSent(0);
        sseReceiver.setReceivedDataEventsCount(0);
    }

    private void testThrottling(String accessToken) throws Exception {
        sseServer.start();
        // Prevent API requests getting dispersed into two time units
        while (LocalDateTime.now().getSecond() > 30) {
            Thread.sleep(5000L);
        }
        long startTime = System.currentTimeMillis();
        startSseReceiver(accessToken, null);
        long endTime = System.currentTimeMillis();
        long connectionAliveDuration = endTime - startTime;
        Assert.assertTrue(connectionAliveDuration < 60000);
        Assert.assertTrue(connectionAliveDuration > 0);
    }

    private void initializeSseServer(int port) {
        Server server = new Server(port);
        ServletHandler servletHandler = new ServletHandler();
        server.setHandler(servletHandler);

        sseServlet = new SseServlet();
        ServletHolder servletHolder = new ServletHolder(sseServlet);
        servletHandler.addServletWithMapping(servletHolder, "/memory");

        sseServer = server;
    }

    private void invokeSseApi(String bearerToken, long runForMillis) throws Exception {
        startAndStopSseServer(runForMillis);
        Thread.sleep(5000);
        startSseReceiver(bearerToken, null);
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

    private void startSseReceiver(String bearerToken, Consumer<Boolean> throttledResponseConsumer) {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(apiEndpoint + "/memory");
        sseReceiver = new SimpleSseReceiver(target, bearerToken);
        sseReceiver.registerThrottledResponseConsumer(throttledResponseConsumer);
        try {
            sseReceiver.open();
        } finally {
            sseReceiver.close(); // This will be called when sseServer.stop() is called
        }
    }

    @AfterTest(alwaysRun = true)
    public void destroy() throws Exception {
        sseServer.stop();
        serverConfigurationManager.restoreToLastConfiguration(false);
        executorService.shutdownNow();
        super.cleanUp();
    }
}
