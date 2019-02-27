/*
 *Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.am.integration.tests.header;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import static org.testng.Assert.assertEquals;
import static org.wso2.am.integration.tests.restapi.RESTAPITestConstants.APPLICATION_JSON_CONTENT;
import static org.wso2.am.integration.tests.restapi.RESTAPITestConstants.AUTHORIZATION_KEY;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CustomHeaderTestCase extends APIManagerLifecycleBaseTest {
    private ServerConfigurationManager serverConfigurationManager;
    private final String CUSTOM_AUTHORIZATION_HEADER = "Test-Custom-Header";
    private final String API1_NAME = "CustomAuthHeaderTestAPI1";
    private final String API1_CONTEXT = "customAuthHeaderTest1";
    private final String API1_VERSION = "1.0.0";
    String apiProviderName;
    private final String APPLICATION1_NAME = "CustomHeaderTest-Application";
    private final String API_END_POINT_METHOD = "customers/123";

    private final String API2_NAME = "CustomAuthHeaderTestAPI2";
    private final String API2_CONTEXT = "customAuthHeaderTest2";
    private final String API2_VERSION = "1.0.0";
    private String accessToken;

    @Factory(dataProvider = "userModeDataProvider")
    public CustomHeaderTestCase(TestUserMode userMode) {
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
        serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
        serverConfigurationManager.applyConfiguration(new File(
                getAMResourceLocation() + File.separator + "configFiles" + File.separator + "customHeaderTest"
                        + File.separator + "api-manager.xml"));
        apiPublisher = new APIPublisherRestClient(getPublisherURLHttp());
        apiStore = new APIStoreRestClient(getStoreURLHttp());
        apiPublisher.login(user.getUserName(), user.getPassword());
        apiStore.login(user.getUserName(), user.getPassword());
        apiProviderName = publisherContext.getContextTenant().getContextUser().getUserName();
        // Create application
        apiStore.addApplication(APPLICATION1_NAME, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "",
                "this-is-test");
        //get access token
        accessToken = generateApplicationKeys(apiStore, APPLICATION1_NAME).getAccessToken();
    }

    @Test(groups = {"wso2.am"}, description = "Set a customer Auth header for all APIs in the system. (Test ID: 3.1.1.5, 3.1.1.14)")
    public void testSystemWideCustomAuthHeader() throws Exception {
        APIIdentifier apiIdentifier1 = new APIIdentifier(user.getUserName(), API1_NAME, API1_VERSION);

        String url = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice";
        APIRequest apiRequest = new APIRequest(API1_NAME, API1_CONTEXT, new URL(url), new URL(url));
        apiRequest.setVersion(API1_VERSION);
        apiRequest.setProvider(user.getUserName());
        apiPublisher.addAPI(apiRequest);
        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(API1_NAME, apiProviderName, APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);
        String invocationUrl = getAPIInvocationURLHttp(API1_CONTEXT, API1_VERSION) + "/" + API_END_POINT_METHOD;

        waitForAPIDeploymentSync(user.getUserName(), API1_NAME, API1_VERSION, APIMIntegrationConstants.IS_API_EXISTS);
        subscribeToAPI(apiIdentifier1, APPLICATION1_NAME, apiStore);

        // Test whether a request made with a valid token using the relevant custom auth header should yield the proper
        // response from the back-end, assuming the application has a valid subscription to the API. (Test ID: 3.1.1.5)
        Map<String, String> requestHeaders1 = new HashMap<>();
        requestHeaders1.put("accept", "application/json");
        requestHeaders1.put(CUSTOM_AUTHORIZATION_HEADER, "Bearer " + accessToken);
        HttpResponse apiResponse1 = HttpRequestUtil.doGet(invocationUrl, requestHeaders1);
        assertEquals(apiResponse1.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched");

        //Test whether the 401 Unauthorized Response will be returned when the default Auth header "Authorization"
        //is used to invoke the API when the system wide custom Authorization header is configured (Test ID :3.1.1.14))
        Map<String, String> requestHeaders2 = new HashMap<>();
        requestHeaders2.put("accept", APPLICATION_JSON_CONTENT);
        requestHeaders2.put(AUTHORIZATION_KEY, "Bearer " + accessToken);
        HttpResponse apiResponse2 = HttpRequestUtil.doGet(invocationUrl, requestHeaders2);
        assertEquals(apiResponse2.getResponseCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                "Response code mismatched");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiPublisher.deleteAPI(API1_NAME, API1_VERSION, apiProviderName);
        apiStore.removeApplication(APPLICATION1_NAME);
        super.cleanUp();
    }
}
