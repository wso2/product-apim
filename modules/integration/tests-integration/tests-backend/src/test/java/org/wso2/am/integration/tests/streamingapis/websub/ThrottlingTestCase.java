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

package org.wso2.am.integration.tests.streamingapis.websub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.HttpStatus;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.EventCountLimitDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyDTO;
import org.wso2.am.integration.clients.admin.api.dto.SubscriptionThrottlePolicyPermissionDTO;
import org.wso2.am.integration.clients.admin.api.dto.ThrottleLimitDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.WebsubSubscriptionConfigurationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.tests.streamingapis.StreamingApiTestUtils;
import org.wso2.am.integration.tests.streamingapis.websub.client.WebhookSender;
import org.wso2.am.integration.tests.streamingapis.websub.server.CallbackServerServlet;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class ThrottlingTestCase extends APIMIntegrationBaseTest {

    private final Log log = LogFactory.getLog(WebSubAPITestCase.class);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final int TOPIC_PORT = 9521;
    private final String DEFAULT_TOPIC = "_default";
    private final String SUBSCRIBE = "subscribe";
    private final String UNSUBSCRIBE = "unsubscribe";

    private String apiName = "WebSubThrottlingAPI";
    private String applicationName = "WebSubThrottlingApplication";
    private String apiContext = "websubThrottling";
    private String apiVersion = "1.0.0";

    private String webSubEventPublisherSource = TestConfigurationProvider.getResourceLocation() + File.separator +
            "artifacts" + File.separator + "AM" + File.separator + "configFiles" + File.separator + "streamingAPIs" +
            File.separator +"webSubTest" + File.separator;
    private String webSubRequestEventPublisherSource = "WebSub_Req_Logger.xml";
    private String webSubThrottleOutEventPublisherSource = "WebSub_Throttle_Out_Logger.xml";
    private String webSubEventPublisherTarget = FrameworkPathUtil.getCarbonHome() + File.separator + "repository" +
            File.separator + "deployment" + File.separator + "server" + File.separator + "eventpublishers" +
            File.separator;

    private ServerConfigurationManager serverConfigurationManager;
    private String provider;
    private APIRequest apiRequest;
    private int callbackReceiverPort;
    private String serverHost;
    private String apiId;
    private String appId;
    private String topicSecret;
    private String apiEndpoint;
    private WebhookSender webhookSender;
    private CallbackServerServlet callbackServerServlet;
    private Server callbackServer;
    private String subPolicyId;
    private String subPolicyName;
    private String accessToken;

    @Factory(dataProvider = "userModeDataProvider")
    public ThrottlingTestCase(TestUserMode userMode) {
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
                (new File(webSubEventPublisherSource + webSubRequestEventPublisherSource),
                        new File(webSubEventPublisherTarget + webSubRequestEventPublisherSource), false);
        serverConfigurationManager.applyConfigurationWithoutRestart
                (new File(webSubEventPublisherSource + webSubThrottleOutEventPublisherSource),
                        new File(webSubEventPublisherTarget + webSubThrottleOutEventPublisherSource), false);
        serverHost = InetAddress.getLocalHost().getHostName();
        String INTERNAL_EVERYONE= "Internal/everyone";
        List<String> roleList = new ArrayList<>();
        SubscriptionThrottlePolicyPermissionDTO permissions;

        InputStream inputStream = new FileInputStream(getAMResourceLocation() + File.separator +
                "configFiles" + File.separator + "streamingAPIs" + File.separator + "webSubTest" +
                File.separator + "policy.json");

        // Extract the field values from the input stream
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonMap = mapper.readTree(inputStream);
        subPolicyName = jsonMap.get("policyName").textValue();
        JsonNode defaultLimitJson = jsonMap.get("defaultLimit");
        JsonNode requestCountJson = defaultLimitJson.get("requestCount");
        Long requestCountLimit = Long.valueOf(String.valueOf(requestCountJson.get("requestCount")));
        String timeUnit = requestCountJson.get("timeUnit").textValue();
        Integer unitTime = Integer.valueOf(String.valueOf(requestCountJson.get("unitTime")));
        // Create the advanced throttling policy with request count quota type
        EventCountLimitDTO eventCountLimitDTO = DtoFactory.createEventCountLimitDTO(timeUnit, unitTime,
                requestCountLimit);
        ThrottleLimitDTO defaultLimit = DtoFactory.createEventCountThrottleLimitDTO(eventCountLimitDTO);
        roleList.add(INTERNAL_EVERYONE);
        permissions = DtoFactory.
                createSubscriptionThrottlePolicyPermissionDTO(SubscriptionThrottlePolicyPermissionDTO.
                        PermissionTypeEnum.ALLOW, roleList);
        SubscriptionThrottlePolicyDTO bandwidthSubscriptionPolicyDTO = DtoFactory
                .createSubscriptionThrottlePolicyDTO(subPolicyName, "", "", false, defaultLimit,
                        -1, -1, 5, "min", new ArrayList<>(),
                        true, "", 2, permissions);
        ApiResponse<SubscriptionThrottlePolicyDTO> addedSubscriptionPolicy =
                restAPIAdmin.addSubscriptionThrottlingPolicy(bandwidthSubscriptionPolicyDTO);

        //Assert the status code and policy ID
        Assert.assertEquals(addedSubscriptionPolicy.getStatusCode(), HttpStatus.SC_CREATED);
        SubscriptionThrottlePolicyDTO addedSubscriptionPolicyDTO = addedSubscriptionPolicy.getData();
        subPolicyId = addedSubscriptionPolicyDTO.getPolicyId();
        Assert.assertNotNull(subPolicyId, "The policy ID cannot be null or empty");

        int lowerPortLimit = 8080;
        int upperPortLimit = 8090;
        callbackReceiverPort = StreamingApiTestUtils.getAvailablePort(lowerPortLimit, upperPortLimit, serverHost);
        if (callbackReceiverPort == -1) {
            throw new APIManagerIntegrationTestException("No available port in the range " +
                    lowerPortLimit + "-" + upperPortLimit + " was found");
        }
        log.info("Selected port " + callbackReceiverPort + " to start callback receiver");
        initializeCallbackReceiver(callbackReceiverPort);
        Thread.sleep(5000);
    }

    @Test(description = "Publish WebSub API")
    public void testPublishWebSubApi() throws Exception {
        provider = user.getUserName();

        URI endpointUri = new URI("http://" + serverHost + ":" + callbackReceiverPort + "/receiver");


        //Create the api creation request object
        apiRequest = new APIRequest(apiName, apiContext, endpointUri, endpointUri);
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(subPolicyName);
        apiRequest.setProvider(provider);
        apiRequest.setType("WEBSUB");
        HttpResponse addAPIResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = addAPIResponse.getData();
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        APIDTO apiDto = restAPIPublisher.getAPIByID(apiId);
        topicSecret = UUID.randomUUID().toString();
        WebsubSubscriptionConfigurationDTO websubSubscriptionConfig = new WebsubSubscriptionConfigurationDTO();
        websubSubscriptionConfig.setSecret(topicSecret);
        websubSubscriptionConfig.setSigningAlgorithm("SHA1");
        websubSubscriptionConfig.setSignatureHeader("x-hub-signature");
        apiDto.setWebsubSubscriptionConfiguration(websubSubscriptionConfig);
        restAPIPublisher.updateAPI(apiDto, apiId);
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

    @Test(description = "Create Application and subscribe", dependsOnMethods = "testPublishWebSubApi")
    public void testWebSubApiApplicationSubscription() throws Exception {
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
                "", APIMIntegrationConstants.API_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH);
        appId = applicationResponse.getData();
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, appId,
                subPolicyName);
        // Validate Subscription of the API
        Assert.assertEquals(subscriptionDTO.getStatus(), SubscriptionDTO.StatusEnum.UNBLOCKED);
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.REFRESH_CODE);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(appId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        accessToken = applicationKeyDTO.getToken().getAccessToken();
    }

    @Test(description = "Test events throttling", dependsOnMethods = "testWebSubApiApplicationSubscription")
    public void testEventsThrottling() throws Exception {
        String callbackUrl = "http://" + serverHost + ":" + callbackReceiverPort + "/receiver";
        handleCallbackSubscription(SUBSCRIBE, apiEndpoint, callbackUrl, DEFAULT_TOPIC, topicSecret, "50000000",
                accessToken);
        initializeWebhookSender(topicSecret);
        Thread.sleep(5000);
        int noOfEventsToSend = 10;
        for (int i = 0; i < noOfEventsToSend; i++) {
            webhookSender.send();
            Thread.sleep(5000);
        }
        handleCallbackSubscription(UNSUBSCRIBE, apiEndpoint, callbackUrl, DEFAULT_TOPIC, topicSecret, "50000000",
                accessToken);

        int sent = webhookSender.getWebhooksSent();
        int received = callbackServerServlet.getCallbacksReceived();
        Assert.assertEquals(sent, noOfEventsToSend);
        Assert.assertTrue(received < noOfEventsToSend);
        // received no of events are less than the sent events. The number cannot be guaranteed due to async nature.

        callbackServerServlet.setCallbacksReceived(0);
        webhookSender.setWebhooksSent(0);
    }

    @Test(description = "Test subscription count throttling", dependsOnMethods = "testWebSubApiApplicationSubscription")
    public void testSubscriptionCountThrottling() throws Exception {
        String callbackUrl1 = "http://" + serverHost + ":" + callbackReceiverPort + "/receiver";
        String callbackUrl2 = "http://" + serverHost + ":" + callbackReceiverPort + "/receiver2";
        String callbackUrl3 = "http://" + serverHost + ":" + callbackReceiverPort + "/receiver3";
        org.apache.http.HttpResponse response = handleCallbackSubscription(SUBSCRIBE, apiEndpoint, callbackUrl1, DEFAULT_TOPIC, topicSecret, "50000000",
                accessToken);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
        response = handleCallbackSubscription(SUBSCRIBE, apiEndpoint, callbackUrl2 , DEFAULT_TOPIC, topicSecret, "50000000",
                accessToken);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
        response = handleCallbackSubscription(SUBSCRIBE, apiEndpoint, callbackUrl3 , DEFAULT_TOPIC, topicSecret, "50000000",
                accessToken);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), 429);
    }

    private void initializeCallbackReceiver(int port) {
        Server server = new Server(port);
        ServletHandler servletHandler = new ServletHandler();
        server.setHandler(servletHandler);

        callbackServerServlet = new CallbackServerServlet();
        callbackServer = server;
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ServletHolder servletHolder = new ServletHolder(callbackServerServlet);
                    servletHandler.addServletWithMapping(servletHolder, "/receiver");
                    callbackServer.start();
                } catch (Exception e) {
                    log.error("Failed to start the callback server");
                }
            }
        });
    }

    private void initializeWebhookSender(String secret) {
        String payloadUrl = apiEndpoint.replaceAll(":([0-9]+)/", ":" + TOPIC_PORT + "/") +
                "/webhooks_events_receiver_resource?topic=" + DEFAULT_TOPIC;
        webhookSender = new WebhookSender(payloadUrl, secret);
    }

    private static org.apache.http.HttpResponse handleCallbackSubscription(String hubMode, String webSubApiUrl,
                                                                           String callbackUrl, String hubTopic,
                                                                           String hubSecret, String hubLeaseSeconds,
                                                                           String bearerToken)
            throws IOException, URISyntaxException {
        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(webSubApiUrl);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        if (!StringUtils.isEmpty(callbackUrl)) {
            String encodedUrl = URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8.toString());
            params.add(new BasicNameValuePair("hub.callback", encodedUrl));
        }
        if (!StringUtils.isEmpty(hubMode)) {
            params.add(new BasicNameValuePair("hub.mode", hubMode));
        }
        if (!StringUtils.isEmpty(hubTopic)) {
            params.add(new BasicNameValuePair("hub.topic", hubTopic));
        }
        if (!StringUtils.isEmpty(hubSecret)) {
            params.add(new BasicNameValuePair("hub.secret", hubSecret));
        }
        if (!StringUtils.isEmpty(hubLeaseSeconds)) {
            params.add(new BasicNameValuePair("hub.lease_seconds", hubLeaseSeconds));
        }
        URI uri = new URIBuilder(httppost.getURI()).addParameters(params).build();
        httppost.setURI(uri);
        httppost.setHeader("Authorization", "Bearer " + bearerToken);
        return httpclient.execute(httppost);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        serverConfigurationManager.restoreToLastConfiguration(false);
        callbackServer.stop();
        executorService.shutdownNow();
        super.cleanUp();
    }
}
