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
    private String provider;
    private APIRequest apiRequest;
    private int sseServerPort;
    private String sseServerHost;
    private String apiId;
    private String appId;
    private SseServlet sseServlet;
    private Server sseServer;
    private SimpleSseReceiver sseReceiver;
    private String apiEndpoint;
    private String consumerKey;
    private String consumerSecret;

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
        String apiContext = "sse";
        String apiVersion = "1.0.0";

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
        consumerKey = applicationKeyDTO.getConsumerKey();
        consumerSecret = applicationKeyDTO.getConsumerSecret();
        invokeSseApi(accessToken, 30000);

        int sent = sseServlet.getEventsSent();
        int received = sseReceiver.getReceivedDataEventsCount();
        Assert.assertNotEquals(sent, 0);
        Assert.assertEquals(sent, received);

        sseServlet.setEventsSent(0);
        sseReceiver.setReceivedDataEventsCount(0);
    }

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
        AdvancedThrottlePolicyDTO advancedPolicyDTO = DtoFactory
                .createAdvancedThrottlePolicyDTO(policyName, "", policyDescription, false, defaultLimit,
                        new ArrayList<>());

        // Add the advanced throttling policy
        ApiResponse<AdvancedThrottlePolicyDTO> addedPolicy =
                restAPIAdmin.addAdvancedThrottlingPolicy(advancedPolicyDTO);
        // Assert the status code and policy ID
        Assert.assertEquals(addedPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        AdvancedThrottlePolicyDTO addedAdvancedPolicyDTO = addedPolicy.getData();
        String apiPolicyId = addedAdvancedPolicyDTO.getPolicyId();
        Assert.assertNotNull(apiPolicyId, "The policy ID cannot be null or empty");

        // Update Throttling policy of the API
        HttpResponse response = restAPIPublisher.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);
        apidto.setApiThrottlingPolicy("SSETestThrottlingPolicy");
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto);
        Assert.assertEquals(updatedAPI.getApiThrottlingPolicy(), "SSETestThrottlingPolicy");
        // Get an Access Token from the user who is logged into the API Store.
        URL tokenEndpointURL = new URL(getKeyManagerURLHttps() + "/oauth2/token");
        String subsAccessTokenPayload = APIMTestCaseUtils.getPayloadForPasswordGrant(user.getUserName(),
                user.getPassword());
        JSONObject subsAccessTokenGenerationResponse = new JSONObject(
                apiStore.generateUserAccessKey(consumerKey, consumerSecret, subsAccessTokenPayload,
                        tokenEndpointURL).getData());

        String subsRefreshToken = subsAccessTokenGenerationResponse.getString("refresh_token");
        assertFalse(org.apache.commons.lang.StringUtils.isEmpty(subsRefreshToken),
                "Refresh token of access token generated by subscriber is empty");

        // Obtain user access token
        String requestBody = APIMTestCaseUtils.getPayloadForPasswordGrant(user.getUserName(), user.getPassword());
        JSONObject accessTokenGenerationResponse = new JSONObject(
                apiStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody,
                        tokenEndpointURL).getData());

        // Get Access Token and Refresh Token
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

    private void testThrottling(String accessToken) throws Exception {
        // Attempt to run for 3 minutes. Connection is expected to be closed within 1 minute due to throttle out.
        startAndStopSseServer(3 * 60000);
        // Prevent API requests getting dispersed into two time units
        while (LocalDateTime.now().getSecond() > 40) {
            Thread.sleep(5000L);
        }
        long startTime = System.currentTimeMillis();
        startSseReceiver(accessToken, null);
        long endTime = System.currentTimeMillis();
        Assert.assertTrue((endTime - startTime) < 60000);

        // Re-attempt to connect after throttle out. A throttled out event is expected to be received.
        AtomicBoolean isThrottled = new AtomicBoolean(false);
        startSseReceiver(accessToken, new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) {
                isThrottled.set(aBoolean);
            }
        });
        Thread.sleep(3000L);
        Assert.assertTrue(isThrottled.get());
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
        serverConfigurationManager.restoreToLastConfiguration(false);
        executorService.shutdownNow();
        super.cleanUp();
    }
}
