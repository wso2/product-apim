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

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        publisherURLHttp = getPublisherURLHttp();
    }

    @Test(groups = {"wso2.am"}, description = "Calling API with invalid token")
    public void APIInvocationFailure() throws Exception {

        String APIName = "TokenInvocationTestAPI";
        String APIContext = "tokenInvocationTestAPI";
        String tags = "youtube, token, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";
        String providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        String APIVersion = "1.0.0";
        APIPublisherRestClient apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                           publisherContext.getContextTenant().getContextUser().getPassword());

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setSandbox(url);
        apiRequest.setResourceMethod("GET");
        apiPublisher.addAPI(apiRequest);
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(APIName,
                providerName,
                APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

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
