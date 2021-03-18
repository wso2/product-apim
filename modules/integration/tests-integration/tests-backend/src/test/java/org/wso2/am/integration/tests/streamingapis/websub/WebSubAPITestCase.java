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
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.WebsubSubscriptionConfigurationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
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
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class WebSubAPITestCase extends APIMIntegrationBaseTest {
    private final String apiName = "WebSubAPI";
    private final String applicationName = "WebSubApplication";
    private final String apiContext = "websub";
    private final String apiVersion = "1.0.0";

    private final Log log = LogFactory.getLog(WebSubAPITestCase.class);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private String webSubEventPublisherSource = TestConfigurationProvider.getResourceLocation() + File.separator +
            "artifacts"
            + File.separator + "AM" + File.separator + "configFiles" + File.separator + "streamingAPIs" + File.separator +"webSubTest"
            + File.separator;
    private String webSubRequestEventPublisherSource = "WebSub_Req_Logger.xml";
    private String webSubThrottleOutEventPublisherSource = "WebSub_Throttle_Out_Logger.xml";
    private String webSubEventPublisherTarget = FrameworkPathUtil.getCarbonHome() + File.separator + "repository"
            + File.separator + "deployment" + File.separator + "server" + File.separator + "eventpublishers"
            + File.separator;

    private ServerConfigurationManager serverConfigurationManager;
    private String provider;
    private APIRequest apiRequest;
    private int callbackReceiverPort;
    private String serverHost;
    private String apiId;
    private String appId;
    private int topicPort = 9021;
    private String topicName = "_default";
    private String topicSecret;
    private String apiEndpoint;
    private WebhookSender webhookSender;
    private CallbackServerServlet callbackServerServlet;
    private Server callbackServer;

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
    }

    @Test(description = "Publish WebSub API")
    public void testPublishWebSubApi() throws Exception {
        provider = user.getUserName();

        URI endpointUri = new URI("http://" + serverHost + ":" + callbackReceiverPort + "/receiver");

        //Create the api creation request object
        apiRequest = new APIRequest(apiName, apiContext, endpointUri, endpointUri);
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.ASYNC_UNLIMITED);
        apiRequest.setProvider(provider);
        apiRequest.setType("WEBSUB");
        HttpResponse addAPIResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = addAPIResponse.getData();
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);

        APIDTO apiDto = restAPIPublisher.getAPIByID(apiId);
        topicSecret = "websub-secret";
        WebsubSubscriptionConfigurationDTO websubSubscriptionConfig = new WebsubSubscriptionConfigurationDTO();
        websubSubscriptionConfig.setSecret(topicSecret);
        websubSubscriptionConfig.setSigningAlgorithm("SHA1");
        websubSubscriptionConfig.setSignatureHeader("x-hub-signature");
        apiDto.setWebsubSubscriptionConfiguration(websubSubscriptionConfig);
        restAPIPublisher.updateAPI(apiDto, apiId);
        Thread.sleep(6000);

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
                APIMIntegrationConstants.API_TIER.ASYNC_UNLIMITED);
        // Validate Subscription of the API
        Assert.assertEquals(subscriptionDTO.getStatus(), SubscriptionDTO.StatusEnum.UNBLOCKED);
    }

    private void initializeCallbackReceiver(int port) throws Exception {
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

    private void initializeWebhookSender(String apiContext, String apiVersion, String secret) {
        String payloadUrl = "http://" + serverHost + ":" + topicPort + "/" + apiContext + "/" + apiVersion +
                "/webhooks_events_receiver_resource?topic=" + topicName;
        webhookSender = new WebhookSender(payloadUrl, secret);
    }

    private static void subscribeToWebhookSender(String webSubApiUrl, String callbackUrl, String hubTopic,
                                                 String hubSecret, String hubLeaseSeconds, String bearerToken)
            throws MalformedURLException, AutomationFrameworkException, UnsupportedEncodingException {
        String encodedUrl = URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8.toString());

        String url = webSubApiUrl + "?hub.callback=" + encodedUrl + "&hub.mode=subscribe&hub.secret=" + hubSecret +
                "&hub.lease_seconds=" + hubLeaseSeconds + "&hub.topic=" + hubTopic;
        HttpRequestUtil.doPost(new URL(url), "",
                Collections.singletonMap("Authorization", "Bearer " + bearerToken));
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        serverConfigurationManager.restoreToLastConfiguration(false);
        executorService.shutdownNow();
        super.cleanUp();
    }
}
