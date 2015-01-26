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

package org.wso2.carbon.am.tests.rest;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.am.tests.APIManagerBaseTest;
import org.wso2.carbon.am.tests.util.APIMgtTestUtil;
import org.wso2.carbon.am.tests.util.APIPublisherRestClient;
import org.wso2.carbon.am.tests.util.APIStoreRestClient;
import org.wso2.carbon.am.tests.util.bean.*;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.api.clients.webapp.mgt.WebAppAdminClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.webapp.WebApplicationDeploymentUtil;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertTrue;

/**
 * This class will host jaxrs_sample_02.war in apimgr- publisher instance and invoke its service.
 */
public class JAXRSSampleTestCase extends APIManagerBaseTest {

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String userName;
    private String password;
    private String APIName = "JaxRsTestAPI";
    private String providerName = "admin";
    private String apiInvocationURL;
    private WebAppAdminClient webAppAdminClient;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {

        super.initPublisher("AM", "api publisher",
                TestUserMode.SUPER_TENANT_ADMIN);
        super.initStore("AM", "api store",
                TestUserMode.SUPER_TENANT_ADMIN);

        userName = automationContextPublisher.getUser().getUserName();
        password = automationContextPublisher.getUser().getPassword();
        apiStore = new APIStoreRestClient(automationContextStore.getContextUrls().getWebAppURL());
        apiPublisher = new APIPublisherRestClient(automationContextPublisher.getContextUrls().getWebAppURL());
        apiInvocationURL = automationContextPublisher.getInstance().getProperty("endpoint").trim()
                + File.separator + "jaxRsTestAPI/1.0.0";
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        apiStore.removeSubscription("JaxRsTestAPI", "1.0.0", "admin", 1);
        apiPublisher.deleteApi("JaxRsTestAPI", "1.0.0", "admin");
        webAppAdminClient.deleteWebAppFile("jaxrs_sample_02.war");

        log.info("jaxrs_sample_02.war deleted successfully");
    }


    @Test(groups = "wso2.am", description = "upload war file and verify deployment")
    public void webApplicationUpload() throws Exception {

        webAppAdminClient = new WebAppAdminClient
                (automationContextPublisher.getContextUrls().getBackEndUrl(), automationContextPublisher.login());

        webAppAdminClient.warFileUplaoder(FrameworkPathUtil.getSystemResourceLocation() + "artifacts" +
                File.separator + "AM" + File.separator + "jaxrs" + File.separator + "jaxrs_sample_02.war");


        boolean isDeployed =
                WebApplicationDeploymentUtil.isWebApplicationDeployed
                        (automationContextPublisher.getContextUrls().getBackEndUrl(),
                                automationContextPublisher.login(),
                                "jaxrs_sample_02");

        Assert.assertTrue(isDeployed, "jaxrs_sample_02.war deployment failure");

    }

    @Test(groups = {"wso2.am"}, description = "Add JaxRsTestAPI", dependsOnMethods = "webApplicationUpload")
    public void addAPITestCase() throws Exception {

        // adding api
        String APIContext = "jaxRsTestAPI";
        String tags = "rest,jaxrs";
        String restBackendUrl = automationContextPublisher.getContextUrls().getWebAppURL() +
                "/jaxrs_sample_02/services/Starbucks_Outlet_Service";
        String description = "This JaxRsTestAPI was created by API manager integration test";
        String APIVersion = "1.0.0";

        apiPublisher.login(userName, password);

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(restBackendUrl));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setSandbox(restBackendUrl);
        apiPublisher.addAPI(apiRequest);

        APIBean apiBean = APIMgtTestUtil.getAPIBeanFromHttpResponse(apiPublisher.getApi(APIName, providerName));

        Assert.assertEquals(apiBean.getId().getApiName(), APIName, "API Name mismatch");
        Assert.assertEquals(apiBean.getContext().trim().substring(apiBean.getContext().indexOf("/") + 1), APIContext, "API context mismatch");
        Assert.assertEquals(apiBean.getId().getVersion(), APIVersion, "API version mismatch");
        Assert.assertEquals(apiBean.getId().getProviderName(), providerName, "Provider Name mismatch");
        for (String tag : apiBean.getTags()) {
            Assert.assertTrue(tags.contains(tag), "API tag data mismatched");
        }
        Assert.assertEquals(apiBean.getDescription(), description, "API description mismatch");
    }

    @Test(groups = {"wso2.am"}, description = "Send request to jaxrs service",
            dependsOnMethods = "addAPITestCase")
    public void invokeAPI() throws Exception {

        // publishing
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(APIName,
                providerName, APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatusTo(updateRequest);

        // subscribing
        apiStore.login(userName, password);
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName, providerName);
        apiStore.subscribe(subscriptionRequest);
        GenerateAppKeyRequest generateAppKeyRequest = new GenerateAppKeyRequest("DefaultApplication");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        Thread.sleep(2000);

        // invoke backend-service through api mgr
        assertTrue(HttpRequestUtil.doGet(apiInvocationURL + File.separator + "orders/123", requestHeaders).getResponseCode() == 200);
        assertTrue(HttpRequestUtil.doGet(apiInvocationURL + File.separator + "orders/123", requestHeaders).getData().contains("\"orderId\":123}}"));
    }
}


