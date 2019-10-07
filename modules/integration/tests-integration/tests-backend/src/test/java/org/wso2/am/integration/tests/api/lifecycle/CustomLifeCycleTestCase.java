/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.am.integration.tests.api.lifecycle;

import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.admin.clients.lifecycle.LifeCycleAdminClient;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.common.FileManager;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.AuthenticatorClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.File;
import java.net.URL;

public class CustomLifeCycleTestCase extends APIManagerLifecycleBaseTest {

    private static final String API_NAME = "APICustomLifecycleTestApi";
    String publisherURLHttp;
    private LifeCycleAdminClient lifeCycleAdminClient;
    private String customizedAPILifecyclePath =
            FrameworkPathUtil.getSystemResourceLocation() + "artifacts" + File.separator + "AM" + File.separator
                    + "configFiles" + File.separator + "customLifecycleTest" + File.separator + "APILifeCycle.xml";
    private String originalLifeCycleContent;
    private APICreationRequestBean apiCreationRequestBean;
    private APIIdentifier apiIdentifier;
    private String apiEndPointUrl;
    private AuthenticatorClient loginClient;
    private String backendUrl;
    private APIPublisherRestClient apiPublisherClient;
    private String apiLifeCycleName = "APILifeCycle";
    private String PROMOTE = "Promote";
    private String RE_PUBLISH = "Re-Publish";
    private String apiId;

    @Factory(dataProvider = "userModeDataProvider")
    public CustomLifeCycleTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN}};
    }

    @BeforeClass
    public void initialize() throws Exception {
        super.init(userMode);
        backendUrl = gatewayContextMgt.getContextUrls().getBackEndUrl();
        loginClient = new AuthenticatorClient(backendUrl);
        //admin login to use the GovernanceAdminClient
        String session = loginClient.login(user.getUserName(), user.getPassword(), "localhost");
        lifeCycleAdminClient = new LifeCycleAdminClient(backendUrl, session);
        originalLifeCycleContent = lifeCycleAdminClient.getLifecycleConfiguration(apiLifeCycleName);
        String customizedAPILifecycleContent = FileManager.readFile(customizedAPILifecyclePath);
        lifeCycleAdminClient.editLifeCycle(apiLifeCycleName, customizedAPILifecycleContent);

        String gatewayUrl;
        if (gatewayContextWrk.getContextTenant().getDomain().equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp();
        } else {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "t/" +
                    gatewayContextWrk.getContextTenant().getDomain() + "/";
        }
        apiEndPointUrl = gatewayUrl + "jaxrs_basic/services/customers/customerservice";
        APIRequest apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setSandbox(apiEndPointUrl);
        apiRequest.setProvider(user.getUserName());
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = serviceResponse.getData();
    }

    @Test(groups = {"wso2.am"}, description = "Check custom life cycle state.")
    public void testCustomLifeCycle() throws Exception {
        //Create and publish api
        restAPIPublisher.changeAPILifeCycleStatus(apiId, Constants.PUBLISHED);
        Assert.assertEquals(APILifeCycleState.PUBLISHED.getState(), restAPIPublisher.getLifecycleStatus(apiId).
                getData(), "lifecycle not changed to published");
        restAPIPublisher.changeAPILifeCycleStatus(apiId, PROMOTE);
        Assert.assertEquals(PROMOTE + "d", restAPIPublisher.getLifecycleStatus(apiId).getData(),
                "lifecycle not changed to custom");
        restAPIPublisher.changeAPILifeCycleStatus(apiId, RE_PUBLISH);
        Assert.assertEquals(APILifeCycleState.PUBLISHED.getState(), restAPIPublisher.getLifecycleStatus(apiId).getData(),
                "lifecycle not changed to published");
    }

    @AfterClass
    public void cleanupArtifacts() throws Exception {
        //Remove test api and revert to original lifecycle config
        restAPIPublisher.deleteAPI(apiId);
    }
}
