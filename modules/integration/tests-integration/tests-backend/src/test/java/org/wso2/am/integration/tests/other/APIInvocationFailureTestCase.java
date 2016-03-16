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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.am.integration.tests.other;

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

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Test api invocation failure messages
 */
public class APIInvocationFailureTestCase extends APIMIntegrationBaseTest {

    private String publisherURLHttp;
    private APIPublisherRestClient apiPublisher;

    private String APIName = "TokenInvocationTestAPI";
    private String APIContext = "tokenInvocationTestAPI";
    private String tags = "youtube, token, media";
    private String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
    private String description = "This is test API create by API manager integration test";
    private String APIVersion = "1.0.0";

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        publisherURLHttp = getPublisherURLHttp();
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(user.getUserName(), user.getPassword());
    }

    @Test(groups = {"wso2.am"}, description = "Calling API with invalid token")
    public void APIInvocationFailure() throws Exception {
        String providerName = user.getUserName();
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setProvider(providerName);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setSandbox(url);
        apiRequest.setResourceMethod("GET");

        //add test api
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(serviceResponse);

        //publish the api
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(APIName, user.getUserName(),
                                                                              APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer xxxxxxxxxxxx");

        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                                 APIMIntegrationConstants.IS_API_EXISTS);

        HttpResponse youTubeResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp(APIContext, APIVersion)
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
    public APIInvocationFailureTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }
}
