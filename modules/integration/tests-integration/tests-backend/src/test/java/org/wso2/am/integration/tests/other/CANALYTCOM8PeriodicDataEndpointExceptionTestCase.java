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

package org.wso2.am.integration.tests.other;

import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.LogViewerClient;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.logging.view.stub.types.carbon.LogEvent;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertFalse;

public class CANALYTCOM8PeriodicDataEndpointExceptionTestCase extends APIMIntegrationBaseTest {

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private LogViewerClient logViewerClient;
    private ServerConfigurationManager serverConfigurationManager;
    private String modifiedDataBridgeConfigPath =
            getAMResourceLocation() + File.separator + "configFiles" + File.separator + "databridgeconfig"
                    + File.separator + "data-bridge-config-modified.xml";
    private String dataBridgeConfigPath =
            FrameworkPathUtil.getCarbonHome() + File.separator + "repository" + File.separator + "conf" + File.separator
                    + "data-bridge" + File.separator + "data-bridge-config.xml";

    @BeforeClass (alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();
        serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
        serverConfigurationManager
                .applyConfiguration(new File(modifiedDataBridgeConfigPath), new File(dataBridgeConfigPath), true, true);
        String gatewaySession= createSession(new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_GATEWAY_MGT_INSTANCE, userMode));
        logViewerClient = new LogViewerClient(gatewayUrlsWrk.getWebAppURLHttps() + "services/", gatewaySession);
        logViewerClient.clearLogs();

        apiStore = new APIStoreRestClient(storeURLHttp);
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);

        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

    }

    @Test (groups = {"wso2.am"}, description = "Client timeout issue")

    public void testClientTimeoutIssue() throws Exception {
        List<APIResourceBean> resourceBeanList = new ArrayList<APIResourceBean>();

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean("PizzaAPI", "pizzashack", "1.0.0",
                "admin", new URL("http://localhost:9766/pizzashack-api-1.0.0/api/"));

        apiCreationRequestBean
                .setDescription("Pizza API:Allows to manage pizza orders (create, update, retrieve orders)");
        apiCreationRequestBean.setTags("pizza, order, pizza-menu");
        apiCreationRequestBean.setResourceCount("4");

        resourceBeanList.add(new APIResourceBean("GET", "Application & Application User", "Unlimited", "/menu"));
        resourceBeanList.add(new APIResourceBean("POST", "Application & Application User", "Unlimited", "/order"));
        resourceBeanList
                .add(new APIResourceBean("GET", "Application & Application User", "Unlimited", "/order/{orderid}"));
        resourceBeanList.add(new APIResourceBean("GET", "Application & Application User", "Unlimited", "/delivery"));
        apiCreationRequestBean.setResourceBeanList(resourceBeanList);

        apiCreationRequestBean.setTier("Unlimited");
        apiCreationRequestBean.setTiersCollection("Unlimited");

        apiPublisher.addAPI(apiCreationRequestBean);
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest("PizzaAPI",
                publisherContext.getContextTenant().getContextUser().getUserName(), APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);
        apiStore.addApplication("PizzaShack", "Unlimited", "", "");
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest("PizzaAPI",
                storeContext.getContextTenant().getContextUser().getUserName());
        subscriptionRequest.setApplicationName("PizzaShack");
        subscriptionRequest.setTier("Unlimited");
        apiStore.subscribe(subscriptionRequest);

        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator("PizzaShack");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        Thread.sleep(1000*75);

        HttpResponse pizzaShackResponse = HttpRequestUtil
                .doGet(gatewayUrlsWrk.getWebAppURLNhttp() + "pizzashack/1.0.0/menu", requestHeaders);

        String ERROR_LOG = "Unable to send events to the endpoint.";
        LogEvent[] logEvents = logViewerClient.getAllRemoteSystemLogs();
        boolean isErrorLogged = false;
        for (LogEvent logEvent : logEvents) {
            if ("ERROR".equals(logEvent.getPriority()) && logEvent.getMessage().contains(ERROR_LOG)) {
                isErrorLogged = true;
            }
        }
        assertFalse(isErrorLogged, "Session timeout exception is not properly handled");
    }


    @AfterClass (alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeApplication("PizzaShack");
        super.cleanUp();
        serverConfigurationManager.restoreToLastConfiguration();
    }
}
