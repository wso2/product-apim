/*
 *
 *   Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.ITestContext;
import org.testng.Reporter;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.wso2.am.admin.clients.webapp.WebAppAdminClient;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.am.integration.test.utils.webapp.WebAppDeploymentUtil;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.net.URL;

public class AdvancedWebAppDeploymentConfig extends APIManagerLifecycleBaseTest {
    private static final Log log = LogFactory.getLog(AdvancedWebAppDeploymentConfig.class);
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String providerName;
    private WebAppAdminClient webAppAdminClient;
    private String apiId;
    private String applicationID;

    @BeforeTest(alwaysRun = true)
    public void deployWebApps(ITestContext ctx) throws Exception {
        super.init();
        String fileFormat = ".war";
        String webApp = "jaxrs_basic";
        String path = TestConfigurationProvider.getResourceLocation() + File.separator +
                "artifacts" + File.separator + "AM" + File.separator + "lifecycletest" + File.separator;

        String sourcePath = path + webApp + fileFormat;

        String sessionId = createSession(gatewayContextWrk);
        webAppAdminClient = new WebAppAdminClient(gatewayContextWrk.getContextUrls().
                getBackEndUrl(), sessionId);
        webAppAdminClient.uploadWarFile(sourcePath);
        webAppAdminClient.uploadWarFile(path + APIMIntegrationConstants.SANDBOXEP1_WEB_APP_NAME + ".war");

        WebAppDeploymentUtil
                .isWebApplicationDeployed(gatewayContextWrk.getContextUrls().getBackEndUrl(), sessionId, webApp);
        WebAppDeploymentUtil.isWebApplicationDeployed(gatewayContextWrk.getContextUrls().getBackEndUrl(), sessionId,
                APIMIntegrationConstants.SANDBOXEP1_WEB_APP_NAME);
        //Deploying the Mock ETCD Server
        String webAppName = "etcdmock";
        sourcePath = org.wso2.am.integration.test.utils.generic.TestConfigurationProvider.getResourceLocation()
                + File.separator + "artifacts" + File.separator + "AM" + File.separator + "war" + File.separator
                + webAppName + ".war";
        webAppAdminClient.uploadWarFile(sourcePath);
        WebAppDeploymentUtil
                .isWebApplicationDeployed(gatewayContextWrk.getContextUrls().getBackEndUrl(), sessionId, webAppName);
        log.info("Web App Deployed");

        initialize(ctx);
    }

    @AfterTest(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        restAPIStore.deleteApplication(applicationID);
        restAPIPublisher.deleteAPI(apiId);
    }

    private void initialize(ITestContext ctx) throws Exception {
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        createAPIs(ctx);
    }

    private void createAPIs(ITestContext ctx) throws Exception {

        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.DEFAULT_APP_POLICY_FIFTY_REQ_PER_MIN,
                ApplicationDTO.TokenTypeEnum.OAUTH);
        applicationID = applicationResponse.getData();
        //Create publish and subscribe a API
        APIRequest apiRequest;
        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));

        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTags(API_TAGS);

        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationID,
                APIMIntegrationConstants.API_TIER.UNLIMITED);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
        ctx.setAttribute("apiId", apiId);
        ctx.setAttribute("applicationID", applicationID);
    }

}
