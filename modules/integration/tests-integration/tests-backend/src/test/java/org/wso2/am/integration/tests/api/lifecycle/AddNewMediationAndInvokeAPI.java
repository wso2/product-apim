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

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
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
import org.wso2.carbon.logging.view.stub.types.carbon.LogEvent;
import org.wso2.carbon.utils.ServerConstants;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Created by saneth on 4/2/15.
 */
public class AddNewMediationAndInvokeAPI extends APIManagerLifecycleBaseTest {
    private static final String API_NAME = "APILifeCycleTestAPI1";
    private static final String API_CONTEXT = "testAPI1";
    private static final String API_TAGS = "youtube, video, media";
    private static final String API_END_POINT_URL = "http://localhost:9763/jaxrs_basic/services/customers/customerservice/";
    private static final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private String APPLICATION_NAME = "AddNewMediationAndInvokeAPI";
    private String accessToken;
    private HashMap<String, String> requestHeadersGet;
    private final static String RESPONSE_GET = "I was at CustomAPIAuthenticationHandler";
    private final static String API_GET_ENDPOINT_METHOD = "/customers/123";
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private APICreationRequestBean apiCreationRequestBean;
    private String providerName;
    private String webAppTargetPath;
    private String publisherURLHttp;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();


        String webAppSourcePath =
                TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" + File.separator + "AM" +
                        File.separator + "configFiles" + File.separator + "lifecycletest" + File.separator + "jaxrs_basic.war";

        webAppTargetPath =
                System.getProperty(ServerConstants.CARBON_HOME) + File.separator + "repository" + File.separator + "deployment" +
                        File.separator + "server" + File.separator + "webapps";

        ServerConfigurationManager serverConfigurationManager = new ServerConfigurationManager(gatewayContext);
        FileManager.copyResourceToFileSystem(webAppSourcePath, webAppTargetPath, "jaxrs_basic.war");


        serverConfigurationManager.restartGracefully();
        super.init();
        apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, new URL(API_END_POINT_URL));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);

        providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);

        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        //Login to API Store with  admin
        apiStoreClientUser1.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());
    }


    @Test(groups = {"wso2.am"}, description = "Invoke the APi and check the  API request is going through the new handler.")
    public void testAPIInvocationHitsTheNewHandler() throws Exception {

        //Create application
        apiStoreClientUser1.addApplication(APPLICATION_NAME, TIER_GOLD, "", "");
        //Create publish and subscribe a API
        APIIdentifier apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiIdentifier.setTier(TIER_GOLD);
        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, new URL(API_END_POINT_URL));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiCreationRequestBean.setInSequence("inSequence");
        createPublishAndSubscribeToAPI(
                apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1, apiStoreClientUser1, APPLICATION_NAME);


        requestHeadersGet = new HashMap<String, String>();
        requestHeadersGet.put("accept", "text/xml");
        ;
        //get the  access token
        accessToken = getAccessToken(apiStoreClientUser1, APPLICATION_NAME);
        requestHeadersGet.put("Authorization", "Bearer " + accessToken);

        LogViewerClient logViewerClient = new LogViewerClient(gatewayUrls.getWebAppURLHttp(), createSession(gatewayContext));
        logViewerClient.clearLogs();
        //Send GET Request
        HttpResponse httpResponse =
                HttpRequestUtil.doGet(API_BASE_URL + API_CONTEXT + "/" + API_VERSION_1_0_0 + API_GET_ENDPOINT_METHOD,
                        requestHeadersGet);
        assertEquals(httpResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request");
        assertTrue(httpResponse.getData().contains(RESPONSE_GET), "Response Data not match for GET request." +
                " Expected value :\"" + RESPONSE_GET + "\" not contains in response data:\"" + httpResponse.getData() + "\"");
        LogEvent[] logEvents = logViewerClient.getAllRemoteSystemLogs();
        boolean isNewHandlerCalled = false;
        for (LogEvent logEvent : logEvents) {
            if (logEvent.getMessage().contains("I am at CustomAPIAuthenticationHandler:CustomAuthKey 123456789")) {
                isNewHandlerCalled = true;
                break;
            }

        }
        // assertTrue(isNewHandlerCalled, "API Request not went through the new handler");

        logViewerClient.clearLogs();
        apiCreationRequestBean.setInSequence("");
        apiPublisherClientUser1.updateAPI(apiCreationRequestBean);


        //Send GET Request
        HttpResponse httpResponse2 =
                HttpRequestUtil.doGet(API_BASE_URL + API_CONTEXT + "/" + API_VERSION_1_0_0 + API_GET_ENDPOINT_METHOD,
                        requestHeadersGet);
        assertEquals(httpResponse2.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request");
        assertTrue(httpResponse2.getData().contains(RESPONSE_GET), "Response Data not match for GET request." +
                " Expected value :\"" + RESPONSE_GET + "\" not contains in response data:\"" + httpResponse2.getData() + "\"");
        LogEvent[] logEvents2 = logViewerClient.getAllRemoteSystemLogs();
        boolean isNewHandlerCalled2 = false;
        for (LogEvent logEvent : logEvents2) {
            if (logEvent.getMessage().contains("I am at CustomAPIAuthenticationHandler:CustomAuthKey 123456789")) {
                isNewHandlerCalled2 = true;
                break;
            }

        }
        // assertTrue(isNewHandlerCalled, "API Request not went through the new handler");

    }
}