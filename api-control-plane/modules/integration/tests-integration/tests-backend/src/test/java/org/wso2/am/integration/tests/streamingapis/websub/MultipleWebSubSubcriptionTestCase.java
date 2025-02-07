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
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.WebsubSubscriptionConfigurationDTO;
import org.wso2.am.integration.clients.store.api.ApiException;
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
import org.wso2.am.integration.tests.streamingapis.websub.server.CallbackServerServlet2;
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

import javax.servlet.Servlet;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class MultipleWebSubSubcriptionTestCase extends APIMIntegrationBaseTest {

    private final Log log = LogFactory.getLog(WebSubAPITestCase.class);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final int TOPIC_PORT = 9521;
    private final String DEFAULT_TOPIC = "_default";
    private final String SUBSCRIBE = "subscribe";
    private final String UNSUBSCRIBE = "unsubscribe";

    private String apiName = "WebSubMultipleSubAPI";
    private String applicationName1 = "WebSubultipleSubApplication1";
    private String applicationName2 = "WebSubultipleSubApplication2";
    private String apiContext = "websubMultiSub";
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
    private int callbackReceiverPort1;
    private int callbackReceiverPort2;
    private String serverHost;
    private String apiId;
    private String appId;
    private String topicSecret;
    private String apiEndpoint;
    private WebhookSender webhookSender;
    private CallbackServerServlet callbackServerServlet;
    private CallbackServerServlet2 callbackServerServlet2;
    private Server callbackServer;
    private String accessToken1;
    private String accessToken2;

    @Factory(dataProvider = "userModeDataProvider")
    public MultipleWebSubSubcriptionTestCase(TestUserMode userMode) {
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
        callbackReceiverPort1 = getCallBackServletPort(8080, 8090);
        callbackReceiverPort2 = getCallBackServletPort(8060, 8070);
        log.info("Selected port " + callbackReceiverPort1 + " to start callback receiver");
        callbackServerServlet = new CallbackServerServlet();
        initializeCallbackReceiver(callbackReceiverPort1, callbackServerServlet);
        callbackServerServlet2 = new CallbackServerServlet2();
        initializeCallbackReceiver(callbackReceiverPort2, callbackServerServlet2);
        Thread.sleep(5000);
    }

    private int getCallBackServletPort(int lowerPortLimit, int upperPortLimit) throws
            APIManagerIntegrationTestException {
        int port = StreamingApiTestUtils.getAvailablePort(lowerPortLimit, upperPortLimit, serverHost);
        if (port == -1) {
            throw new APIManagerIntegrationTestException("No available port in the range " +
                    lowerPortLimit + "-" + upperPortLimit + " was found");
        }
        return port;
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
        accessToken1 = subscribeToAPI(applicationName1);
        accessToken2 = subscribeToAPI(applicationName2);
    }

    private String subscribeToAPI(String applicationName) throws ApiException, APIManagerIntegrationTestException {
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName,
                "", APIMIntegrationConstants.API_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH);
        appId = applicationResponse.getData();
        SubscriptionDTO subscriptionDTO = null;
        subscriptionDTO = restAPIStore.subscribeToAPI(apiId, appId,
                APIMIntegrationConstants.API_TIER.ASYNC_WH_UNLIMITED);
        // Validate Subscription of the API
        Assert.assertEquals(subscriptionDTO.getStatus(), SubscriptionDTO.StatusEnum.UNBLOCKED);
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.REFRESH_CODE);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(appId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        return applicationKeyDTO.getToken().getAccessToken();
    }

    @Test(description = "Invoke the WebSub API", dependsOnMethods = "testWebSubApiApplicationSubscription")
    public void testInvokeWebSubApi() throws Exception {
        String callbackUrl1 = "http://" + serverHost + ":" + callbackReceiverPort1 + "/receiver";
        String callbackUrl2 = "http://" + serverHost + ":" + callbackReceiverPort2 + "/receiver";
        handleCallbackSubscription(SUBSCRIBE, apiEndpoint, callbackUrl1, DEFAULT_TOPIC, topicSecret, "50000000",
                accessToken1);
        handleCallbackSubscription(SUBSCRIBE, apiEndpoint, callbackUrl2, DEFAULT_TOPIC, topicSecret, "50000000",
                accessToken2);
        initializeWebhookSender(topicSecret);
        Thread.sleep(5000);
        int noOfEventsToSend = 10;
        for (int i = 0; i < noOfEventsToSend; i++) {
            webhookSender.send();
            Thread.sleep(3000);
        }
        handleCallbackSubscription(UNSUBSCRIBE, apiEndpoint, callbackUrl1, DEFAULT_TOPIC, topicSecret, "50000000",
                accessToken1);
        handleCallbackSubscription(UNSUBSCRIBE, apiEndpoint, callbackUrl2, DEFAULT_TOPIC, topicSecret, "50000000",
                accessToken2);
        Thread.sleep(5000);
        int sent = webhookSender.getWebhooksSent();
        int receivedBy1 = callbackServerServlet.getCallbacksReceived();
        int receivedBy2 = callbackServerServlet2.getCallbacksReceived();
        Assert.assertEquals(sent, noOfEventsToSend);
        Assert.assertEquals(sent + 1, receivedBy1); // no. of events received = no. of events sent + 1 subscribe event
        Assert.assertEquals(sent + 1, receivedBy2);

        callbackServerServlet.setCallbacksReceived(0);
        callbackServerServlet2.setCallbacksReceived(0);
        webhookSender.setWebhooksSent(0);
    }

    private void initializeCallbackReceiver(int port, Servlet servlet) {
        Server server = new Server(port);
        ServletHandler servletHandler = new ServletHandler();
        server.setHandler(servletHandler);

        callbackServer = server;
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ServletHolder servletHolder = new ServletHolder(servlet);
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

    private static void handleCallbackSubscription(String hubMode, String webSubApiUrl, String callbackUrl,
                                                   String hubTopic, String hubSecret, String hubLeaseSeconds,
                                                   String bearerToken)
            throws UnsupportedEncodingException, MalformedURLException, AutomationFrameworkException {
        String encodedUrl = URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8.toString());
        String url = webSubApiUrl + "?hub.callback=" + encodedUrl + "&hub.mode=" + hubMode + "&hub.secret=" +
                hubSecret + "&hub.lease_seconds=" + hubLeaseSeconds + "&hub.topic=" + hubTopic;
        HttpRequestUtil.doPost(new URL(url), "", Collections.singletonMap("Authorization", "Bearer " + bearerToken));
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        serverConfigurationManager.restoreToLastConfiguration(false);
        callbackServer.stop();
        executorService.shutdownNow();
        super.cleanUp();
    }
}
