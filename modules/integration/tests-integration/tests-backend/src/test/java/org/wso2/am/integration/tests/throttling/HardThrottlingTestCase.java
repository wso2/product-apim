/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.throttling;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.Assert;
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
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
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

/**
 * This will test Hard Throttling for APIs.
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class HardThrottlingTestCase extends APIMIntegrationBaseTest {
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private ServerConfigurationManager serverConfigurationManager;
    private String publisherURLHttp;
    private String storeURLHttp;
    private static final Log log = LogFactory.getLog(HardThrottlingTestCase.class);

    @Factory(dataProvider = "userModeDataProvider")
    public HardThrottlingTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        publisherURLHttp = publisherUrls.getWebAppURLHttp();
        storeURLHttp = storeUrls.getWebAppURLHttp();

        serverConfigurationManager = new ServerConfigurationManager(
                new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                                      APIMIntegrationConstants.AM_GATEWAY_WRK_INSTANCE, TestUserMode.SUPER_TENANT_ADMIN));

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);
        String gatewaySessionCookie = createSession(gatewayContextMgt);
        loadSynapseConfigurationFromClasspath("artifacts" + File.separator + "AM" + File.separator +
                                              "synapseconfigs" + File.separator + "throttling" + File.separator +
                                              "dummy-stockquote.xml", gatewayContextMgt, gatewaySessionCookie);

    }

    @Test(groups = {"wso2.am"}, description = "Token API Test sample")
    public void testProductionLimit() throws Exception {
        String apiName = "HardThrottleTestAPI";
        String apiContext = "throttle";
        String tags = "throttle";
        String url = gatewayUrlsWrk.getWebAppURLNhttp()+"stockquote";
        String description = "This is test API create by API manager integration test";
        String providerName = "admin";
        String apiVersion = "1.0.0";
        String applicationName = "HardThrottleTestApplication";
        String keyType = "PRODUCTION";
        int requestLimit = 5;
        apiPublisher.login(publisherContext.getSuperTenant().getContextUser().getUserName(),
                           publisherContext.getSuperTenant().getContextUser().getPassword());
        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(url));
        apiRequest.addParameter("productionTps",Integer.toString(requestLimit));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(apiVersion);
        apiRequest.setSandbox(url);
        apiPublisher.addAPI(apiRequest);

        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiName, providerName, APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        apiStore.login(publisherContext.getSuperTenant().getContextUser().getUserName(),
                       publisherContext.getSuperTenant().getContextUser().getPassword());

        // Create application
        apiStore.addApplication(applicationName, "Gold", "", "this-is-test");

        SubscriptionRequest subscriptionRequest =
                new SubscriptionRequest(apiName, publisherContext.getSuperTenant().getContextUser().getUserName());
        subscriptionRequest.setApplicationName(applicationName);
        subscriptionRequest.setTier("Gold");
        apiStore.subscribe(subscriptionRequest);

        //Generate sandbox Token and invoke with that
        APPKeyRequestGenerator generateAppKeyRequestProduction = new APPKeyRequestGenerator(applicationName);
        generateAppKeyRequestProduction.setKeyType(keyType);
        String responseStringProduction = apiStore.generateApplicationKey(generateAppKeyRequestProduction).getData();
        JSONObject responseSandBOX = new JSONObject(responseStringProduction);
        String productionAccessToken = responseSandBOX.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeadersProduction = new HashMap<String, String>();
        requestHeadersProduction.put("Authorization", "Bearer " + productionAccessToken);

        int accessCount = 0;
        while(accessCount < requestLimit+1){
            HttpRequestUtil.doGet(gatewayUrlsWrk.getWebAppURLNhttp()
                                  +apiContext+apiVersion, requestHeadersProduction);
            accessCount++;
        }
        HttpResponse youTubeResponseSandBox = HttpRequestUtil.doGet(gatewayUrlsWrk.getWebAppURLNhttp()
                                                                    +apiContext+apiVersion, requestHeadersProduction);

        log.info("Response : "+ youTubeResponseSandBox.getData());
        log.info("Response Status: "+ youTubeResponseSandBox.getResponseCode());

        Assert.assertEquals(youTubeResponseSandBox.getResponseCode(), 503, "Response code mismatched");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
        serverConfigurationManager.restoreToLastConfiguration();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }
}
