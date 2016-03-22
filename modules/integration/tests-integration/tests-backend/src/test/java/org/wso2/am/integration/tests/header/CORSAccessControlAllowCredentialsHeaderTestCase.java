/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.am.integration.tests.header;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Test CORS functionality
 */
public class CORSAccessControlAllowCredentialsHeaderTestCase extends APIMIntegrationBaseTest {

    private String publisherURLHttp;
    private APIPublisherRestClient apiPublisher;

    private static final String API_NAME = "CorsHeadersTestAPI";
    private static final String API_CONTEXT = "corsHeadersTestAPI";
    private static final String API_VERSION = "1.0.0";
    private static final String TAGS = "cors, test";
    private static final String DESCRIPTION = "This is test API create by API manager integration test";

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        //Load the back-end dummy API
        if(TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            String gatewaySessionCookie = createSession(gatewayContextMgt);
            loadSynapseConfigurationFromClasspath("artifacts" + File.separator + "AM"
                                                  + File.separator + "synapseconfigs" + File.separator + "rest"
                                                  + File.separator + "dummy_api.xml", gatewayContextMgt,
                                                  gatewaySessionCookie);

            //Enable CORS
            ServerConfigurationManager serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
            serverConfigurationManager.applyConfigurationWithoutRestart(
                    new File(getAMResourceLocation() + File.separator + "configFiles/corsACACTest/api-manager.xml"));
        }
        publisherURLHttp = getPublisherURLHttp();
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(user.getUserName(), user.getPassword());
    }

    @Test(groups = {"wso2.am"}, description = "Calling API with invalid token")
    public void APIInvocationFailure() throws Exception {
        String providerName = user.getUserName();
        String endpointUrl = getGatewayURLNhttp() + "response";
        APIRequest apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(endpointUrl));
        apiRequest.setTags(TAGS);
        apiRequest.setProvider(providerName);
        apiRequest.setDescription(DESCRIPTION);
        apiRequest.setVersion(API_VERSION);
        apiRequest.setSandbox(endpointUrl);
        apiRequest.setResourceMethod("GET");

        //add test api
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(serviceResponse);

        //publish the api
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(API_NAME, user.getUserName(),
                                                                              APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer xxxxxxxxxxxx");

        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                                 APIMIntegrationConstants.IS_API_EXISTS);

        HttpResponse youTubeResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION)
                                                             + "/most_popular", requestHeaders);
        assertEquals(youTubeResponse.getResponseCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                     "Response code mismatched when api invocation");
        assertTrue(youTubeResponse.getData().contains("900901"), "Error code mismach");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] {TestUserMode.SUPER_TENANT_ADMIN },
                                new Object[] { TestUserMode.TENANT_ADMIN }, };
    }

    @Factory(dataProvider = "userModeDataProvider")
    public CORSAccessControlAllowCredentialsHeaderTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }
}
