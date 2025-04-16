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

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.internal.api.dto.WebhooksSubscriptionsListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.WebsubSubscriptionConfigurationDTO;
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
import org.wso2.am.integration.tests.streamingapis.websub.client.WebhookSender;
import org.wso2.am.integration.tests.streamingapis.websub.server.CallbackServerServlet;
import org.wso2.am.integration.tests.streamingapis.websub.server.CallbackServerServletWithSubVerification;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.exceptions.AutomationFrameworkException;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class WebSubAPITestCase extends APIMIntegrationBaseTest {

    private final Log log = LogFactory.getLog(WebSubAPITestCase.class);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final int TOPIC_PORT = 9521;
    private final String DEFAULT_TOPIC = "_default";
    private final String SUBSCRIBE = "subscribe";
    private final String UNSUBSCRIBE = "unsubscribe";

    private String apiName = "WebSubAPI";
    private String applicationName = "WebSubApplication";
    private String apiContext = "websub";
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
    private String accessToken;
    private CallbackServerServletWithSubVerification callbackServerServletWithSubVerification;
    private int callbackReceiverWithSubVerificationPort;
    private Server callbackServerWithSubVerification;

    @Factory(dataProvider = "userModeDataProvider")
    public WebSubAPITestCase(TestUserMode userMode) {
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
        callbackReceiverWithSubVerificationPort = StreamingApiTestUtils.getAvailablePort(lowerPortLimit, upperPortLimit,
                                                                                         serverHost);
        if (callbackReceiverWithSubVerificationPort == -1) {
            throw new APIManagerIntegrationTestException(
                    "No available port in the range " + lowerPortLimit + "-" + upperPortLimit + " was found");
        }
        log.info("Selected port " + callbackReceiverWithSubVerificationPort + " to start callback receiver");
        initializeCallbackReceiverWithSubVerification(callbackReceiverWithSubVerificationPort);
        Thread.sleep(5000);
    }

    @Test(description = "Publish WebSub API")
    public void testPublishWebSubApi() throws Exception {
        provider = user.getUserName();

        //Create the api creation request object
        apiRequest = new APIRequest(apiName, apiContext);
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.ASYNC_WH_UNLIMITED);
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
                APIMIntegrationConstants.API_TIER.ASYNC_WH_UNLIMITED);
        // Validate Subscription of the API
        Assert.assertEquals(subscriptionDTO.getStatus(), SubscriptionDTO.StatusEnum.UNBLOCKED);
    }

    @Test(description = "Invoke the WebSub API", dependsOnMethods = "testWebSubApiApplicationSubscription")
    public void testInvokeWebSubApi() throws Exception {
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.REFRESH_CODE);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(appId, "3600", null,
                                                                        ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                                                                        null, grantTypes);
        accessToken = applicationKeyDTO.getToken().getAccessToken();
        Assert.assertNotNull(accessToken, "Error occurred while generating the access token");
    }

    @Test(description = "Test invoke WebSub API when parameters are passed as query parameters",
            dependsOnMethods = "testInvokeWebSubApi")
    public void testInvokeWebSubApiWithQueryParameters() throws Exception {

        callbackServerServlet.setCallbacksReceived(0);
        String callbackUrl = "http://" + serverHost + ":" + callbackReceiverPort + "/receiver";
        handleCallbackSubscriptionWithQueryParameters(SUBSCRIBE, apiEndpoint, callbackUrl, DEFAULT_TOPIC, topicSecret,
                                                      "50000000", accessToken);
        initializeWebhookSender(topicSecret);
        Thread.sleep(5000);
        int noOfEventsToSend = 10;
        for (int i = 0; i < noOfEventsToSend; i++) {
            webhookSender.send();
            Thread.sleep(5000);
        }
        handleCallbackSubscriptionWithQueryParameters(UNSUBSCRIBE, apiEndpoint, callbackUrl, DEFAULT_TOPIC, topicSecret,
                                                      "50000000", accessToken);
        Thread.sleep(5000);
        int sent = webhookSender.getWebhooksSent();
        int received = callbackServerServlet.getCallbacksReceived();
        Assert.assertEquals(sent, noOfEventsToSend);
        Assert.assertEquals(sent + 1, received); // no. of events received = no. of events sent + 1 subscribe event
    }

    @Test(description = "Test invoke WebSub API when parameters are passed as form url encoded data",
            dependsOnMethods = "testInvokeWebSubApi")
    public void testInvokeWebSubAPIWithFormUrlEncodedData() throws Exception {

        callbackServerServlet.setCallbacksReceived(0);
        String callbackUrl = "http://" + serverHost + ":" + callbackReceiverPort + "/receiver";
        HttpResponse subResponse = handleCallbackSubscriptionWithFormUrlEncoded(SUBSCRIBE, apiEndpoint, callbackUrl,
                                                                                DEFAULT_TOPIC, topicSecret, "50000000",
                                                                                accessToken);
        Assert.assertEquals(HttpServletResponse.SC_ACCEPTED, subResponse.getResponseCode(),
                            "Subscribe request failed with a " + subResponse.getResponseCode() + " response");
        initializeWebhookSender(topicSecret);
        Thread.sleep(5000);
        int noOfEventsToSend = 5;
        for (int i = 0; i < noOfEventsToSend; i++) {
            webhookSender.send();
            Thread.sleep(5000);
        }
        HttpResponse unSubResponse = handleCallbackSubscriptionWithFormUrlEncoded(UNSUBSCRIBE, apiEndpoint, callbackUrl,
                                                                                  DEFAULT_TOPIC, topicSecret,
                                                                                  "50000000", accessToken);
        Thread.sleep(5000);
        Assert.assertEquals(HttpServletResponse.SC_ACCEPTED, unSubResponse.getResponseCode(),
                            "Unsubscribe request failed with a " + unSubResponse.getResponseCode() + " response");
        int sent = webhookSender.getWebhooksSent();
        int received = callbackServerServlet.getCallbacksReceived();
        Assert.assertEquals(sent, noOfEventsToSend, "Webhook sender failed to send all the requests");
        Assert.assertEquals(sent, received, "Callback server did not receive all the content distribution requests");
    }

    @Test(description = "Test invoke WebSub API with multiple subscriptions and check subscription count",
            dependsOnMethods = "testInvokeWebSubApi")
    public void testMultipleSubscriptions() throws Exception {
        String callbackUrl = "http://" + serverHost + ":" + callbackReceiverPort + "/receiver";
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.REFRESH_CODE);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        int initialSubscriptionCount = restAPIInternal.retrieveWebhooksSubscriptions().getList().size();

        // Create 3 applications and subscribe to the API and subscribe to the topic
        String application1Name = applicationName + "1";
        HttpResponse application1Response = restAPIStore.createApplication(application1Name,
                "", APIMIntegrationConstants.API_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH);
        String app1Id = application1Response.getData();
        restAPIStore.subscribeToAPI(apiId, app1Id,
                APIMIntegrationConstants.API_TIER.ASYNC_WH_UNLIMITED);
        ApplicationKeyDTO application1KeyDTO = restAPIStore.generateKeys(app1Id, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                null, grantTypes);
        String accessToken1 = application1KeyDTO.getToken().getAccessToken();
        handleCallbackSubscriptionWithFormUrlEncoded(SUBSCRIBE, apiEndpoint, callbackUrl,
                DEFAULT_TOPIC, topicSecret, "50000000",
                accessToken1);
        Thread.sleep(5000);

        String application2Name = applicationName + "2";
        HttpResponse application2Response = restAPIStore.createApplication(application2Name,
                "", APIMIntegrationConstants.API_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH);
        String app2Id = application2Response.getData();
        restAPIStore.subscribeToAPI(apiId, app2Id,
                APIMIntegrationConstants.API_TIER.ASYNC_WH_UNLIMITED);
        ApplicationKeyDTO application2KeyDTO = restAPIStore.generateKeys(app2Id, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                null, grantTypes);
        String accessToken2 = application2KeyDTO.getToken().getAccessToken();
        handleCallbackSubscriptionWithFormUrlEncoded(SUBSCRIBE, apiEndpoint, callbackUrl,
                DEFAULT_TOPIC, topicSecret, "50000000",
                accessToken2);
        Thread.sleep(5000);

        String application3Name = applicationName + "3";
        HttpResponse application3Response = restAPIStore.createApplication(application3Name,
                "", APIMIntegrationConstants.API_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH);
        String app3Id = application3Response.getData();
        restAPIStore.subscribeToAPI(apiId, app3Id,
                APIMIntegrationConstants.API_TIER.ASYNC_WH_UNLIMITED);
        ApplicationKeyDTO application3KeyDTO = restAPIStore.generateKeys(app3Id, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                null, grantTypes);
        String accessToken3 = application3KeyDTO.getToken().getAccessToken();
        // Subscribe to topic with infinite expiry time
        handleCallbackSubscriptionWithFormUrlEncoded(SUBSCRIBE, apiEndpoint, callbackUrl,
                DEFAULT_TOPIC, topicSecret, accessToken3);
        Thread.sleep(5000);

        WebhooksSubscriptionsListDTO webhooksSubscriptionsListDTO = restAPIInternal.retrieveWebhooksSubscriptions();
        Assert.assertEquals(webhooksSubscriptionsListDTO.getList().size(), initialSubscriptionCount + 3,
                "Expected number of subscriptions not found in the database");

        // Unsubscribe from the topic
        HttpResponse unSubResponse1 = handleCallbackSubscriptionWithFormUrlEncoded(UNSUBSCRIBE, apiEndpoint, callbackUrl,
                DEFAULT_TOPIC, topicSecret,
                "50000000", accessToken1);
        HttpResponse unSubResponse2 = handleCallbackSubscriptionWithFormUrlEncoded(UNSUBSCRIBE, apiEndpoint, callbackUrl,
                DEFAULT_TOPIC, topicSecret,
                "50000000", accessToken2);
        HttpResponse unSubResponse3 = handleCallbackSubscriptionWithFormUrlEncoded(UNSUBSCRIBE, apiEndpoint, callbackUrl,
                DEFAULT_TOPIC, topicSecret, accessToken3);
        Thread.sleep(5000);
        WebhooksSubscriptionsListDTO webhooksSubscriptionsListDTOAfterUnsubscribing = restAPIInternal.retrieveWebhooksSubscriptions();
        Assert.assertEquals(webhooksSubscriptionsListDTOAfterUnsubscribing.getList().size(), initialSubscriptionCount,
                "Expected number of subscriptions not found in the database");
    }

    @Test(description = "Check availability of mandatory parameters",
            dependsOnMethods = "testInvokeWebSubApi")
    public void testMandatoryParameters() throws Exception {

        callbackServerServlet.setCallbacksReceived(0);
        String callbackUrl = "http://" + serverHost + ":" + callbackReceiverPort + "/receiver";
        handleCallbackSubscriptionWithFormUrlEncoded(SUBSCRIBE, apiEndpoint, callbackUrl, DEFAULT_TOPIC, topicSecret,
                                                     "50000000", accessToken);
        initializeWebhookSender(topicSecret);
        Thread.sleep(5000);
        Assert.assertEquals(SUBSCRIBE, callbackServerServlet.getHubMode(),
                            "Callback server did not receive the expected hub.mode parameter");
        Assert.assertEquals(DEFAULT_TOPIC, callbackServerServlet.getHubTopic(),
                            "Callback server did not receive the expected hub.topic parameter");
        Assert.assertTrue(StringUtils.isNotEmpty(callbackServerServlet.getHubChallenge()),
                          "Callback server did not receive the hub.challenge parameter");

        String hubUrl = "http://localhost:" + TOPIC_PORT;
        webhookSender.send();
        Thread.sleep(5000);
        Assert.assertTrue(callbackServerServlet.getLinkHeader().contains(hubUrl),
                          "Missing link header in content distribution request");
        handleCallbackSubscriptionWithFormUrlEncoded(UNSUBSCRIBE, apiEndpoint, callbackUrl, DEFAULT_TOPIC, topicSecret,
                                                     "50000000", accessToken);
        Thread.sleep(5000);
    }

    @Test(description = "Check subscription when mandatory parameters are missing",
            dependsOnMethods = "testInvokeWebSubApi")
    public void testMissingMandatoryParameters() throws Exception {

        String callbackUrl = "http://" + serverHost + ":" + callbackReceiverPort + "/receiver";
        try {
            handleCallbackSubscriptionWithFormUrlEncoded("", apiEndpoint, callbackUrl, DEFAULT_TOPIC, topicSecret,
                                                         "50000000", accessToken);
            Thread.sleep(5000);
            Assert.fail("WebSub subscription invoked without mandatory parameters.");
        } catch (AutomationFrameworkException e) {
            assertTrue(e.getMessage().contains("Server returned HTTP response code: 500"));
        }
    }

    @Test(description = "Check subscriber verification",
            dependsOnMethods = "testInvokeWebSubApi")
    public void testSubscriberVerification() throws Exception {

        APIDTO apiDto = restAPIPublisher.getAPIByID(apiId);
        apiDto.setEnableSubscriberVerification(true);
        restAPIPublisher.updateAPI(apiDto, apiId);
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), apiName, apiVersion, APIMIntegrationConstants.IS_API_EXISTS);

        callbackServerServlet.setCallbacksReceived(0);
        String callbackUrl = "http://" + serverHost + ":" + callbackReceiverWithSubVerificationPort + "/receiver";
        handleCallbackSubscriptionWithFormUrlEncoded(SUBSCRIBE, apiEndpoint, callbackUrl, DEFAULT_TOPIC, topicSecret,
                                                     "50000000", accessToken);
        initializeWebhookSender(topicSecret);
        Thread.sleep(5000);
        int noOfEventsToSend = 5;
        for (int i = 0; i < noOfEventsToSend; i++) {
            webhookSender.send();
            Thread.sleep(5000);
        }
        handleCallbackSubscriptionWithFormUrlEncoded(UNSUBSCRIBE, apiEndpoint, callbackUrl, DEFAULT_TOPIC, topicSecret,
                                                     "50000000", accessToken);
        Thread.sleep(5000);
        int sent = webhookSender.getWebhooksSent();
        int received = callbackServerServletWithSubVerification.getCallbacksReceived();
        Assert.assertEquals(sent, noOfEventsToSend, "Webhook sender failed to send all the requests");
        Assert.assertEquals(sent, received, "Callback server did not receive all the content distribution requests");
    }

    private void initializeCallbackReceiverWithSubVerification(int port) {
        Server server = new Server(port);
        ServletHandler servletHandler = new ServletHandler();
        server.setHandler(servletHandler);

        callbackServerServletWithSubVerification = new CallbackServerServletWithSubVerification();
        callbackServerWithSubVerification = server;
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ServletHolder servletHolder = new ServletHolder(callbackServerServletWithSubVerification);
                    servletHandler.addServletWithMapping(servletHolder, "/receiver");
                    callbackServerWithSubVerification.start();
                } catch (Exception e) {
                    log.error("Failed to start the callback server");
                }
            }
        });
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
        webhookSender.setWebhooksSent(0);
    }

    private static void handleCallbackSubscriptionWithQueryParameters(String hubMode, String webSubApiUrl,
                                                                      String callbackUrl, String hubTopic,
                                                                      String hubSecret, String hubLeaseSeconds,
                                                                      String bearerToken)
            throws UnsupportedEncodingException, MalformedURLException, AutomationFrameworkException {
        String encodedUrl = URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8.toString());
        String url = webSubApiUrl + "?hub.callback=" + encodedUrl + "&hub.mode=" + hubMode + "&hub.secret=" + hubSecret
                + "&hub.lease_seconds=" + hubLeaseSeconds + "&hub.topic=" + hubTopic;
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        HttpRequestUtil.doPost(new URL(url), "", headers);
    }

    private static HttpResponse handleCallbackSubscriptionWithFormUrlEncoded(String hubMode, String url,
                                                                             String callbackUrl, String hubTopic,
                                                                             String hubSecret, String hubLeaseSeconds,
                                                                             String bearerToken)
            throws UnsupportedEncodingException, MalformedURLException, AutomationFrameworkException {
        String encodedUrl = URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8.toString());
        String body = "hub.callback=" + encodedUrl + "&hub.mode=" + hubMode + "&hub.secret=" + hubSecret
                + "&hub.lease_seconds=" + hubLeaseSeconds + "&hub.topic=" + hubTopic;
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
        return HttpRequestUtil.doPost(new URL(url), body, headers);
    }

    // Subscribe to topic without an expiry time (infinite expiry time)
    private static HttpResponse handleCallbackSubscriptionWithFormUrlEncoded(String hubMode, String url,
                                                                             String callbackUrl, String hubTopic,
                                                                             String hubSecret,
                                                                             String bearerToken)
            throws UnsupportedEncodingException, MalformedURLException, AutomationFrameworkException {
        String encodedUrl = URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8.toString());
        String body = "hub.callback=" + encodedUrl + "&hub.mode=" + hubMode + "&hub.secret=" + hubSecret
                + "&hub.topic=" + hubTopic;
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
        return HttpRequestUtil.doPost(new URL(url), body, headers);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        serverConfigurationManager.restoreToLastConfiguration(false);
        callbackServer.stop();
        callbackServerWithSubVerification.stop();
        executorService.shutdownNow();
        super.cleanUp();
    }
}
