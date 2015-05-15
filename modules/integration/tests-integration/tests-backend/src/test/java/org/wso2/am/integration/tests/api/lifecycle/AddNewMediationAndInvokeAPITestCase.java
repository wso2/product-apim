/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.api.lifecycle;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.common.FileManager;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.LogViewerClient;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.logging.view.stub.LogViewerLogViewerException;
import org.wso2.carbon.logging.view.stub.types.carbon.LogEvent;
import org.wso2.carbon.utils.ServerConstants;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import static org.testng.Assert.*;

/**
 * Add new Log mediation to the in-flow and check the logs to verify the  added mediation is working.
 */
public class AddNewMediationAndInvokeAPITestCase extends APIManagerLifecycleBaseTest {
    private static final String API_NAME = "APILifeCycleTestAPI";
    private static final String API_CONTEXT = "testAPI";
    private static final String API_TAGS = "youtube, video, media";
    private static final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private static final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private static final String APPLICATION_NAME = "AddNewMediationAndInvokeAPI";
    private final static String RESPONSE_GET = "<id>123</id><name>John</name></Customer>";
    private final static String API_GET_ENDPOINT_METHOD = "/customers/123";
    private final static String MEDIATION_LOG_OUTPUT1 = "To: /testAPI/1.0.0/customers/123";
    private final static String MEDIATION_LOG_OUTPUT2 = "Direction: request, IN_MESSAGE = IN_MESSAGE";
    private final static String MEDIATION_LOGGER = "org.apache.synapse.mediators.builtin.LogMediator";
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private APICreationRequestBean apiCreationRequestBean;
    private String apiEndPointUrl;
    private String providerName;
    private String webAppTargetPath;
    private String publisherURLHttp;
    private LogViewerClient logViewerClient;
    private APIIdentifier apiIdentifier;
    private String accessToken;
    private HashMap<String, String> requestHeadersGet;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        apiEndPointUrl = gatewayUrls.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        String webAppSourcePath =
                TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" + File.separator +
                        "AM" + File.separator + "lifecycletest" + File.separator +
                        "jaxrs_basic.war";
        webAppTargetPath =
                System.getProperty(ServerConstants.CARBON_HOME) + File.separator + "repository" + File.separator +
                        "deployment" + File.separator + "server" + File.separator + "webapps";
        ServerConfigurationManager serverConfigurationManager = new ServerConfigurationManager(gatewayContext);
        FileManager.copyResourceToFileSystem(webAppSourcePath, webAppTargetPath, "jaxrs_basic.war");
        serverConfigurationManager.restartGracefully();
        super.init();
        providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                        new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);
        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(
                publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        //Login to API Store with  admin
        apiStoreClientUser1.login(
                storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());
        logViewerClient = new LogViewerClient(
                gatewayContext.getContextUrls().getBackEndUrl(), createSession(gatewayContext));
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiIdentifier.setTier(TIER_GOLD);
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                        new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
    }


    @Test(groups = {"wso2.am"}, description = "Invoke the API before adding the log mediation")
    public void testAPIInvocationBeforeAddingNewMediation() throws APIManagerIntegrationTestException, IOException,
            LogViewerLogViewerException {
        //Create application
        apiStoreClientUser1.addApplication(APPLICATION_NAME, TIER_GOLD, "", "");
        //Create publish and subscribe a API
        createPublishAndSubscribeToAPI(
                apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1, apiStoreClientUser1, APPLICATION_NAME);
        requestHeadersGet = new HashMap<String, String>();
        requestHeadersGet.put("accept", "text/xml");
        //get the  access token
        accessToken = generateApplicationKeys(apiStoreClientUser1, APPLICATION_NAME).getAccessToken();
        requestHeadersGet.put("Authorization", "Bearer " + accessToken);
        logViewerClient.clearLogs();
        //Send GET Request
        HttpResponse httpResponse =
                HttpRequestUtil.doGet(GATEWAY_WEB_APP_URL + API_CONTEXT + "/" + API_VERSION_1_0_0 + API_GET_ENDPOINT_METHOD,
                        requestHeadersGet);
        assertEquals(httpResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request");
        assertTrue(httpResponse.getData().contains(RESPONSE_GET), "Response Data not match for GET request." +
                " Expected value :\"" + RESPONSE_GET + "\" not contains in response data:\"" +
                httpResponse.getData() + "\"");
        LogEvent[] logEvents = logViewerClient.getAllRemoteSystemLogs();
        assertFalse(isLogAvailable(logEvents, MEDIATION_LOGGER, MEDIATION_LOG_OUTPUT1),
                "API request  went through  the  log mediation before adding");
        assertFalse(isLogAvailable(logEvents, MEDIATION_LOGGER, MEDIATION_LOG_OUTPUT2),
                "API request  went through  the  log mediation before adding");
        logViewerClient.clearLogs();

    }


    @Test(groups = {"wso2.am"}, description = "Invoke the API after adding the log mediation",
            dependsOnMethods = "testAPIInvocationBeforeAddingNewMediation")
    public void testAPIInvocationAfterAddingNewMediation() throws APIManagerIntegrationTestException, IOException,
            LogViewerLogViewerException {
        logViewerClient.clearLogs();
        apiCreationRequestBean.setInSequence("log_in_message");
        apiPublisherClientUser1.updateAPI(apiCreationRequestBean);
        logViewerClient.clearLogs();
        //Send GET Request
        HttpResponse httpResponse =
                HttpRequestUtil.doGet(GATEWAY_WEB_APP_URL + API_CONTEXT + "/" + API_VERSION_1_0_0 + API_GET_ENDPOINT_METHOD,
                        requestHeadersGet);
        assertEquals(httpResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request");
        assertTrue(httpResponse.getData().contains(RESPONSE_GET), "Response Data not match for GET request." +
                " Expected value :\"" + RESPONSE_GET + "\" not contains in response data:\"" +
                httpResponse.getData() + "\"");
        LogEvent[] logEvents = logViewerClient.getAllRemoteSystemLogs();
        assertTrue(isLogAvailable(logEvents, MEDIATION_LOGGER, MEDIATION_LOG_OUTPUT1),
                "API request did not go through the log mediation after adding");
        assertTrue(isLogAvailable(logEvents, MEDIATION_LOGGER, MEDIATION_LOG_OUTPUT2),
                "API request did not go through the log mediation after adding");
        logViewerClient.clearLogs();
    }


    @Test(groups = {"wso2.am"}, description = "IInvoke the API after removing the log mediation",
            dependsOnMethods = "testAPIInvocationAfterAddingNewMediation")
    public void testAPIInvocationBeforeRemovingNewMediation() throws APIManagerIntegrationTestException, IOException,
            LogViewerLogViewerException {
        logViewerClient.clearLogs();
        apiCreationRequestBean.setInSequence("");
        apiPublisherClientUser1.updateAPI(apiCreationRequestBean);
        //Send GET Request
        HttpResponse httpResponse =
                HttpRequestUtil.doGet(GATEWAY_WEB_APP_URL + API_CONTEXT + "/" + API_VERSION_1_0_0 +
                        API_GET_ENDPOINT_METHOD, requestHeadersGet);
        assertEquals(httpResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request");
        assertTrue(httpResponse.getData().contains(RESPONSE_GET), "Response Data not match for GET request." +
                " Expected value :\"" + RESPONSE_GET + "\" not contains in response data:\"" +
                httpResponse.getData() + "\"");
        LogEvent[] logEvents = logViewerClient.getAllRemoteSystemLogs();
        assertFalse(isLogAvailable(logEvents, MEDIATION_LOGGER, MEDIATION_LOG_OUTPUT1),
                "API request went through the log mediation after removing it");
        assertFalse(isLogAvailable(logEvents, MEDIATION_LOGGER, MEDIATION_LOG_OUTPUT2),
                "API request went through the log mediation after removing it");
    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException {
        apiStoreClientUser1.removeApplication(APPLICATION_NAME);
        deleteAPI(apiIdentifier, apiPublisherClientUser1);
    }

    /**
     * Check the availability of logs
     *
     * @param logEventsArray - LogEvent Array that need to be searched for the logs.
     * @param expectedLogger - Name of the class that do the logging.
     * @param expectedLog    - Expected log message.
     * @return boolean - true of expected log is found under expected Logger class in the
     * logEventsArray, else false.
     */
    private boolean isLogAvailable(LogEvent[] logEventsArray, String expectedLogger, String expectedLog) {
        boolean isNewMediationCalled = false;
        for (LogEvent logEvent : logEventsArray) {
            if (logEvent.getLogger().equals(expectedLogger) && logEvent.getMessage().contains(expectedLog)) {
                isNewMediationCalled = true;
                System.out.print(logEvent);
                break;
            }
        }
        return isNewMediationCalled;
    }


}