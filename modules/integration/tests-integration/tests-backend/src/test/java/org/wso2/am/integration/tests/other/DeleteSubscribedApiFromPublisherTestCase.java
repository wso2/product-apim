/*
 *
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.*;
import org.wso2.am.admin.clients.webapp.WebAppAdminClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.am.integration.test.utils.webapp.WebAppDeploymentUtil;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.common.FileManager;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

public class DeleteSubscribedApiFromPublisherTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(DeleteSubscribedApiFromPublisherTestCase.class);
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String apiName = "DeleteSubscribeAPITest1";
    private String version = "1.0.0";
    private String apiContext = "deletesubscribeapi";
    private String tags = "testtag1,teasttag2";
    private String providerName;
    private String applicationName = "DeleteSubscribeAPITestCase";
    private String visibility = "public";
    private String description = "Test_Delete_Subscribe_API_from_Publisher";
    private String endPointType = "http";
    private String tier = "Gold";
    private static final String webApp = "jaxrs_basic";

    @Factory(dataProvider = "userModeDataProvider")
    public DeleteSubscribedApiFromPublisherTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        String fileFormat = ".war";
        super.init(userMode);
        log.info("Test Starting user mode:" + userMode);

        //copy first .war file
        String path = TestConfigurationProvider.getResourceLocation() + File.separator +
                "artifacts" + File.separator + "AM" + File.separator + "lifecycletest" + File.separator;

        String sourcePath = path + webApp + fileFormat;

        String sessionId = createSession(gatewayContextWrk);
        WebAppAdminClient webAppAdminClient = new WebAppAdminClient(gatewayContextWrk.getContextUrls().
                getBackEndUrl(), sessionId);
        webAppAdminClient.uploadWarFile(sourcePath);
        boolean isWebAppDeployed = WebAppDeploymentUtil.isWebApplicationDeployed
                (gatewayContextWrk.getContextUrls().getBackEndUrl(), sessionId, webApp);
        assertTrue(isWebAppDeployed, "Web APP is not deployed");
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);

        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());


        providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        List<APIResourceBean> resourceBeanList = new ArrayList<APIResourceBean>();
        resourceBeanList.add(new APIResourceBean("GET", "Application & Application User", tier,
                "customers/{id}/"));
        String gatewayUrl;
        if (gatewayContextWrk.getContextTenant().getDomain().equals("carbon.super")) {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp();
        } else {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "t/" +
                    gatewayContextWrk.getContextTenant().getDomain() + "/";

        }

        String endpoint = "/services/customers/customerservice";

        String endPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + webApp + endpoint;
        String accessUrl = gatewayUrl + apiContext + "/" + version + "/" + "customers/123";

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiName, apiContext, version, providerName,
                        new URL(endPointUrl));
        apiCreationRequestBean.setResourceBeanList(resourceBeanList);
        apiCreationRequestBean.setVisibility(visibility);
        apiCreationRequestBean.setDescription(description);
        apiCreationRequestBean.setTags(tags);
        apiCreationRequestBean.setEndpointType(endPointType);

        HttpResponse apiCreateResponse = apiPublisher.addAPI(apiCreationRequestBean);
        assertEquals(apiCreateResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Invalid Response Code");
        assertTrue(apiCreateResponse.getData().contains("{\"error\" : false}"),
                "Response Data Mismatched Actual: " +
                        apiCreateResponse.getData());

        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, providerName,
                APILifeCycleState.PUBLISHED);
        HttpResponse statusUpdateResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);

        assertEquals(statusUpdateResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code is Mismatched");
        assertTrue(statusUpdateResponse.getData().contains("\"error\" : false"), "Response Data Mismatched");

        //Add application to Store
        apiStore.addApplication(applicationName, tier, "", "This-is-Test");

        providerName = storeContext.getContextTenant().getContextUser().getUserName();
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, version,
                providerName, applicationName, tier);
        apiStore.subscribe(subscriptionRequest);
        log.info(subscriptionRequest);

        //Generate Key
        APPKeyRequestGenerator appKeyRequestGenerator = new APPKeyRequestGenerator(applicationName);
        String responseString = apiStore.generateApplicationKey(appKeyRequestGenerator).getData();

        JSONObject jsonObject = new JSONObject(responseString);
        String accessToken = jsonObject.getJSONObject("data").getJSONObject("key").getString("accessToken");

        Map<String, String> requestHeader = new HashMap<String, String>();
        requestHeader.put("Authorization", " Bearer " + accessToken);
        requestHeader.put("accept", "text/xml");

        HttpResponse response = new HttpRequestUtil().doGet(accessUrl, requestHeader);

        assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(), "Response Code Mismatched");
        assertTrue(response.getData().contains("<Customer><id>123</id><name>John</name></Customer>"),
                "Response Data Mismatch");

    }

    @Test(groups = {"wso2.am"}, description = "Delete Subscribe API from Publisher")
    public void testDeleteSubscribedApi() throws Exception {

        providerName=publisherContext.getContextTenant().getContextUser().getUserName();
        HttpResponse deleteResponse = apiPublisher.deleteAPI(apiName, version, providerName);
        assertEquals(deleteResponse.getResponseCode(), Response.Status.OK.getStatusCode());
        JSONObject deleteResponseJson=new JSONObject(deleteResponse.getData());
        assertTrue(deleteResponseJson.getBoolean("error"), "Response Data Error");

        //verify the API not deleted from publisher
        HttpResponse apiNOTDeletePublisher = apiPublisher.getAllAPIs();
        JSONObject jsonObject = new JSONObject(apiNOTDeletePublisher);
        String responseString = jsonObject.getString("data");
        JSONObject jsonObject1 = new JSONObject(responseString);
        JSONArray jsonArray = jsonObject1.getJSONArray("apis");

        if (jsonArray.length() != 0) {
            for (int i = 0; i < jsonArray.length(); i++) {
                String apiName = jsonArray.getJSONObject(i).getString("name");
                assertEquals(apiName, this.apiName, "API is deleted from the publisher");
            }
        }
    }

    @Test(groups = {"wso2.am"}, description = "Delete API from Store and Publisher",
            dependsOnMethods = {"testDeleteSubscribedApi"})
    public void deleteApiFromSubscriberPublisher() throws Exception {

        providerName = storeContext.getContextTenant().getContextUser().getUserName();
        HttpResponse storeDeleteResponse = apiStore.removeAPISubscriptionByName(apiName, version,
                providerName, applicationName);
        assertEquals(storeDeleteResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Invalid Response Code");

        assertTrue(storeDeleteResponse.getData().contains("{\"error\" : false}"),
                "Response Data Mismatch");


        providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        HttpResponse publisherApiDelete = apiPublisher.deleteAPI(apiName, version, providerName);
        assertEquals(publisherApiDelete.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Invalid Response Code");
        assertTrue(publisherApiDelete.getData().contains("{\"error\" : false}"),
                "Response Data Mismatch");

        //verify the API delete from publisher
        HttpResponse apiDeletePublisher = apiPublisher.getAllAPIs();
        JSONObject jsonObject = new JSONObject(apiDeletePublisher);
        String responseString = jsonObject.getString("data");
        JSONObject jsonObject1 = new JSONObject(responseString);
        JSONArray jsonArray = jsonObject1.getJSONArray("apis");

        if (jsonArray.length() != 0) {
            for (int i = 0; i < jsonArray.length(); i++) {
                String apiName = jsonArray.getJSONObject(i).getString("name");
                assertNotEquals(apiName, this.apiName, "API is not deleted from the publisher");

            }
        }

        //Verify subscription not work when no api in Publisher
        providerName = storeContext.getContextTenant().getContextUser().getUserName();
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, version,
                providerName, applicationName, tier);
        HttpResponse subscribeResponseWithoutPublisher = apiStore.subscribe(subscriptionRequest);

        assertTrue(subscribeResponseWithoutPublisher.getData().contains("{\"error\" : true"),
                "Response Data Mismatched");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeApplication(applicationName);
        super.cleanUp();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
//                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }
}


