/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.am.integration.tests.other;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.ServerConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class LightweightObservabilityTestCase extends APIMIntegrationBaseTest {
    private static final Log log = LogFactory.getLog(LightweightObservabilityTestCase.class);
    private ServerConfigurationManager serverConfigurationManager;
    private WireMockServer wireMockServer;
    private final String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
    private final String correlationId = "32ac33f7-dcb2-455c-8615-62725690eb50";
    private final String endpointHost = "http://localhost";
    private int endpointPort;
    private int lowerPortLimit = 9950;
    private final int upperPortLimit = 9999;
    private final String apiName = "SampleAPI";
    private final String apiContext = "sampleapi";
    private final String version = "1.0.0";
    private String apiEndPointURL;
    private final String apiResourceName = "/hello";
    private final String appName = "SampleApp";
    private final String tier = "Unlimited";
    private String providerName;

    @Factory(dataProvider = "userModeDataProvider")
    public LightweightObservabilityTestCase(TestUserMode userMode) {
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
        superTenantKeyManagerContext = new AutomationContext(
                APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE,
                TestUserMode.SUPER_TENANT_ADMIN);
        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);

        // Add settings to log4j2.properties
        serverConfigurationManager.applyConfigurationWithoutRestart(
                new File(getAMResourceLocation() + File.separator + "configFiles" + File.separator
                        + "lightweightObservabilityTest" + File.separator + "log4j2.properties"));

        // Restart the server
        serverConfigurationManager.restartGracefully();

        // Initialize publisher and store
        providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(providerName, publisherContext.getContextTenant().getContextUser().getPassword());

        String storeURLHttp = storeUrls.getWebAppURLHttp();
        apiStore = new APIStoreRestClient(storeURLHttp);
        apiStore.login( storeContext.getContextTenant().getContextUser().getUserName(), storeContext.getContextTenant().getContextUser().getPassword());
    }

    @Test(groups = {"wso2.am"}, description = "Test whether message tracking logs are logged correctly")
    public void testMessageTrackingLogs() throws Exception {
        // Create and publish sample API
        startWiremockServer();
        apiEndPointURL = endpointHost + ":" + endpointPort + "/";

        //Create API request
        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(apiEndPointURL));
        apiRequest.setTags("hello");
        apiRequest.setDescription("Sample API for testing lightweight observability");
        apiRequest.setVersion(version);
        apiRequest.setProvider(providerName);
        apiRequest.setEndpointType("http");
        apiRequest.setTier(tier);
        apiRequest.setVisibility("public");

        // Add API resource to the request
        List<APIOperationsDTO> operationsDTOS2 = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO2 = new APIOperationsDTO();
        apiOperationsDTO2.setVerb("GET");
        apiOperationsDTO2.setTarget("/hello");
        apiOperationsDTO2.setAuthType("None");
        apiOperationsDTO2.setThrottlingPolicy(tier);
        operationsDTOS2.add(apiOperationsDTO2);
        apiRequest.setResourceCount("1");
        apiRequest.setOperationsDTOS(operationsDTOS2);

        // Create API
        HttpResponse addApiResponse = restAPIPublisher.addAPI(apiRequest);
        Assert.assertEquals(addApiResponse.getResponseCode(), HttpStatus.SC_CREATED, "Response code is not as expected");

        // Publish API
        restAPIPublisher.changeAPILifeCycleStatus(addApiResponse.getData(), APILifeCycleAction.PUBLISH.getAction());
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(),
                apiRequest.getVersion(), APIMIntegrationConstants.IS_API_EXISTS);
        Assert.assertEquals(addApiResponse.getResponseCode(), HttpStatus.SC_CREATED, "Response code is not as expected");

        // Invoke resource
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("activityid", correlationId);
        String colonSeparatedHeader = user.getUserName() + ":" + user.getPassword();
        String authorizationHeader = new String(Base64.encodeBase64(colonSeparatedHeader.getBytes()));
        requestHeaders.put("Authorization", "Basic " + authorizationHeader);

        HttpResponse invokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttps(apiContext, version) +
                        apiResourceName, requestHeaders);
        assertEquals(invokeResponse.getResponseCode(), HttpStatus.SC_OK);

        // Check whether correlationId is in logs
        try {
            String correlationLogFilePath = carbonHome + File.separator + "repository" + File.separator + "logs" +
                    File.separator + "correlation.log";
            BufferedReader bufferedReader = new BufferedReader(new FileReader(correlationLogFilePath));
            String logLine;
            while ((logLine = bufferedReader.readLine()) != null) {
                if (logLine.contains("/" + apiContext + "/" + version + apiResourceName)) {
                    Assert.assertTrue(logLine.contains(correlationId));
                }
            }
        } catch (IOException e) {
            log.info("Error while reading correlation.log file: " + e);
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeApplication(appName);
        apiPublisher.deleteAPI(apiName, version, user.getUserName());
        wireMockServer.stop();
        super.cleanUp();
        serverConfigurationManager.restoreToLastConfiguration();
    }

    /**
     * Start wiremock server with resource
     *
     */
    private void startWiremockServer() {
        endpointPort = getAvailablePort();
        assertNotEquals(endpointPort, -1, "No available port in the range " + lowerPortLimit + "-" +
                upperPortLimit + " was found");
        wireMockServer = new WireMockServer(options().port(endpointPort));
        wireMockServer.stubFor(WireMock.get(urlEqualTo(apiResourceName)).willReturn(aResponse()
                .withStatus(200).withHeader("Content-Type", "text/plain").withBody("Hello")));
        wireMockServer.start();
    }

    /**
     * Find a free port to start backend WebSocket server in given port range
     *
     * @return Available Port Number
     */
    private int getAvailablePort() {
        while (lowerPortLimit < upperPortLimit) {
            if (isPortFree(lowerPortLimit)) {
                return lowerPortLimit;
            }
            lowerPortLimit++;
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
            s = new Socket(endpointHost, port);
            // something is using the port and has responded.
            return false;
        } catch (IOException e) {
            // port available
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
}
