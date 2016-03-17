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
*
*/
package org.wso2.am.integration.tests.api.lifecycle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;

/**
 * This test class is use to test the coping and updating api test cases
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE })
public class NewVersionUpdateTestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(NewVersionUpdateTestCase.class);
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String publisherURLHttp;
    private String storeURLHttp;
    private APIRequest apiRequest;

    private String apiName = "NewVersionUpdateTestCaseAPIName";
    private String APIContext = "NewVersionUpdateTestCaseContext";
    private String tags = "test";
    private String endpointUrl, endpointUrlNew;
    private String description = "This is test API create by API manager integration test";
    private String APIVersion = "1.0.0";
    private String APIVersionNew = "2.0.0";

    @Factory(dataProvider = "userModeDataProvider")
    public NewVersionUpdateTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        publisherURLHttp = getPublisherURLHttp();
        storeURLHttp = getStoreURLHttp();
        endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api/add";
        endpointUrlNew =
                backEndServerUrl.getWebAppURLHttp() + "https://localhost:9443/am/sample/calculator/v1/api/multiply";
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(user.getUserName(), user.getPassword());
        apiStore = new APIStoreRestClient(storeURLHttp);
        apiStore.login(user.getUserName(), user.getPassword());

    }

    @Test(groups = { "wso2.am" }, description = "Create sample API and set the state as Prototype")
    public void testAPICreation() throws Exception {
        String providerName = user.getUserName();

        apiRequest = new APIRequest(apiName, APIContext, new URL(endpointUrl));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setProvider(providerName);

        //add test api
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(serviceResponse);

        //publish the api
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, user.getUserName(),
                APILifeCycleState.PROTOTYPED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);

    }

    @Test(groups = { "wso2.am" }, description = "Create new version and publish",
            dependsOnMethods = "testAPICreation")
    public void testAPINewVersionCreation() throws Exception {
        //add test api
        HttpResponse serviceResponse = apiPublisher
                .copyAPI(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(), APIVersionNew, null);
        verifyResponse(serviceResponse);

        //test the copied api
        serviceResponse = apiPublisher.getAPI(apiRequest.getName(), apiRequest.getProvider(), APIVersionNew);
        verifyResponse(serviceResponse);
        JSONObject response = new JSONObject(serviceResponse.getData());
        String version = response.getJSONObject("api").get("version").toString();
        Assert.assertEquals(version, APIVersionNew);

        //publish the api
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, user.getUserName(),
                APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);
        Assert.assertTrue(serviceResponse.getData().contains(APILifeCycleState.PUBLISHED.getState()));
    }

    @Test(groups = { "wso2.am" }, description = "Update new version api with endpoint",
            dependsOnMethods = "testAPINewVersionCreation")
    public void testNewVersionAPIUpdate() throws Exception {
        //create update request for restrict HTTP
        APIRequest apiUpdateRequest = new APIRequest(apiRequest.getName(), apiRequest.getContext(),
                new URL(endpointUrlNew));
        apiUpdateRequest.setProvider(apiRequest.getProvider());
        apiUpdateRequest.setVersion(APIVersionNew);
        HttpResponse serviceResponse = apiPublisher.updateAPI(apiUpdateRequest);
        verifyResponse(serviceResponse);

        //test the updated api endpoint
        serviceResponse = apiPublisher.getAPI(apiRequest.getName(), apiRequest.getProvider(), APIVersionNew);
        verifyResponse(serviceResponse);
        JSONObject response = new JSONObject(serviceResponse.getData());
        String endpointConfig = response.getJSONObject("api").get("endpointConfig").toString();
        Assert.assertTrue(endpointConfig.contains(endpointUrlNew));
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiPublisher.deleteAPI(apiName, APIVersion, user.getUserName());
        apiPublisher.deleteAPI(apiName, APIVersionNew, user.getUserName());
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }, };
    }
}
