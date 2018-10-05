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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.lifecycle.LifeCycleAdminClient;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.common.FileManager;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.AuthenticatorClient;

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
    private String apiEndPointUrl = "http://foo.com";
    private AuthenticatorClient loginClient;
    private String backendUrl;
    private APIPublisherRestClient apiPublisherClient;
    private String apiLifeCycleName = "APILifeCycle";

    @BeforeClass
    public void initialize() throws Exception {
        super.init();
        backendUrl = gatewayContextMgt.getContextUrls().getBackEndUrl();
        loginClient = new AuthenticatorClient(backendUrl);
        //admin login to use the GovernanceAdminClient
        String session = loginClient.login("admin", "admin", "localhost");
        lifeCycleAdminClient = new LifeCycleAdminClient(backendUrl, session);
        originalLifeCycleContent = lifeCycleAdminClient.getLifecycleConfiguration(apiLifeCycleName);
        String customizedAPILifecycleContent = FileManager.readFile(customizedAPILifecyclePath);
        lifeCycleAdminClient.editLifeCycle(apiLifeCycleName, customizedAPILifecycleContent);

        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0,
                user.getUserName(), new URL(apiEndPointUrl));
        apiIdentifier = new APIIdentifier(user.getUserName(), API_NAME, API_VERSION_1_0_0);
        publisherURLHttp = getPublisherURLHttp();
        apiPublisherClient = new APIPublisherRestClient(publisherURLHttp);
        apiPublisherClient.login(user.getUserName(), user.getPassword());
    }

    @Test(groups = {"wso2.am"}, description = "Check custom life cycle state.")
    public void testCustomLifeCycle() throws Exception {
        //Create and publish api
        createAndPublishAPI(apiIdentifier, apiCreationRequestBean, apiPublisherClient, false);
        APILifeCycleStateRequest apiLifeCycleStatusChangeRequest = new APILifeCycleStateRequest(API_NAME,
                user.getUserName(), APILifeCycleState.PROMOTE);
        apiLifeCycleStatusChangeRequest.setVersion(apiIdentifier.getVersion());

        //Change api status to custom state
        HttpResponse publishAPIResponse = apiPublisherClient.changeAPILifeCycleStatus(apiLifeCycleStatusChangeRequest);
        Boolean statusChangeSuccess = verifyAPIStatusChange(publishAPIResponse, APILifeCycleState.PUBLISHED,
                APILifeCycleState.PROMOTED);
        Assert.assertTrue(statusChangeSuccess, "API status Change is not successful");
    }

    @AfterClass
    public void cleanupArtifacts() throws Exception {
        //Remove test api and revert to original lifecycle config
        deleteAPI(apiIdentifier, apiPublisherClient);
        lifeCycleAdminClient.editLifeCycle(apiLifeCycleName, originalLifeCycleContent);
        loginClient.logOut();
        apiPublisherClient.logout();
    }
}
