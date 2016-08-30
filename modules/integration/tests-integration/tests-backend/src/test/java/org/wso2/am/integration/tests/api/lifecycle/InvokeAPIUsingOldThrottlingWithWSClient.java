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
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * Publish a API under gold tier and test the invocation with throttling , then change the api tier to silver
 * and do a new silver subscription and test invocation under Silver tier.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE })
public class InvokeAPIUsingOldThrottlingWithWSClient extends APIManagerLifecycleBaseTest {
    private final String API_NAME = "ChangeAPITierAndTestInvokingTest";
    private final String API_CONTEXT = "ChangeAPITierAndTestInvoking";
    private final String API_TAGS = "testTag1, testTag2, testTag3";

    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_END_POINT_METHOD = "customers/123";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "ChangeAPITierAndTestInvokingTestCase";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String applicationNameGold;
    private Map<String, String> requestHeadersGoldTier;
    private APIIdentifier apiIdentifier;
    private String providerName;
    private APICreationRequestBean apiCreationRequestBean;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private ServerConfigurationManager serverConfigurationManager;
    protected AutomationContext superTenantKeyManagerContext;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        superTenantKeyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE,
                TestUserMode.SUPER_TENANT_ADMIN);
        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);

        serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                + File.separator + "configFiles" + File.separator + "apiManagerXmlWithoutAdvancedThrottlingUsingWSClient" +
                File.separator + "api-manager.xml"));

        serverConfigurationManager.restartGracefully();

        apiEndPointUrl = getGatewayURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);
        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(user.getUserName(), user.getPassword());
        //Login to API Store with  admin
        apiStoreClientUser1.login(user.getUserName(), user.getPassword());
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
    }


    @Test(groups = {"wso2.am"}, description = "test  invocation of  api using old throttle and ws client")
    public void testInvokingAPIWithOldThrottleUsingWSClient() throws Exception {

        applicationNameGold = APPLICATION_NAME + TIER_GOLD;
        apiStoreClientUser1
                .addApplication(applicationNameGold, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");
        apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                                           new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiCreationRequestBean.setTier(TIER_GOLD);
        createPublishAndSubscribeToAPI(
                apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1, apiStoreClientUser1, applicationNameGold);

        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0, APIMIntegrationConstants.IS_API_EXISTS);

        //get access token
        String accessToken = generateApplicationKeys(apiStoreClientUser1, applicationNameGold).getAccessToken();

        // Create requestHeaders
        requestHeadersGoldTier = new HashMap<String, String>();
        requestHeadersGoldTier.put("Authorization", "Bearer " + accessToken);
        requestHeadersGoldTier.put("accept", "text/xml");

        HttpResponse invokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + "/" +
                                      API_END_POINT_METHOD, requestHeadersGoldTier);
        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                     "API Invocation failed");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        apiStoreClientUser1.removeApplication(applicationNameGold);
        deleteAPI(apiIdentifier, apiPublisherClientUser1);
        serverConfigurationManager.restoreToLastConfiguration();
    }
}
