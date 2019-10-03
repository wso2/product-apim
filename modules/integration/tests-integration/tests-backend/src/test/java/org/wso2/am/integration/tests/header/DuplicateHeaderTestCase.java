/*
*Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.webapp.WebAppAdminClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APIThrottlingTier;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.am.integration.test.utils.webapp.WebAppDeploymentUtil;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.net.URL;

import static org.testng.Assert.assertEquals;

/**
 * Test a backend returning duplicate headers on tenant
 */
public class DuplicateHeaderTestCase extends APIMIntegrationBaseTest {

    private final String apiName = "APIMDuplicateHeaderTestAPI";
    private final String apiVersion = "1.0.0";
    private final String applicationName = "APIMDuplicateHeaderTestApp";
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String apiContext = "duplicateHeaderAPI";
    private String accessToken;
    private String providerNameApi;
    private String tenantAdmin;

    private final String tenantKey = "user1";

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(TestUserMode.TENANT_ADMIN);
        providerNameApi = publisherContext.getContextTenant().getTenantUser(tenantKey).getUserName();
        User tenantUser = publisherContext.getContextTenant().getTenantUser(tenantKey);
        tenantAdmin = tenantUser.getUserName();

        String apiEndpointPostfixUrl = "t/wso2.com/webapps/duplicate-header-backend/duplicateHeaderBackend";
        String webAppName = "duplicate-header-backend";
        String sourcePath = TestConfigurationProvider.getResourceLocation() + File.separator +
                "artifacts" + File.separator + "AM" + File.separator + "war" +
                File.separator + webAppName + ".war";
        String sessionId = createSession(gatewayContextWrk);

        WebAppAdminClient webAppAdminClient =
                new WebAppAdminClient(gatewayContextWrk.getContextUrls().getBackEndUrl(), sessionId);
        webAppAdminClient.uploadWarFile(sourcePath);
        WebAppDeploymentUtil.isWebApplicationDeployed(gatewayContextWrk.getContextUrls().
                getBackEndUrl(), sessionId, webAppName);

        apiStore = new APIStoreRestClient(storeURLHttp);
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);

        apiPublisher.login(tenantUser.getUserName(), tenantUser.getPassword());
        apiStore.login(tenantUser.getUserName(), tenantUser.getPassword());

        String backendEndPoint = getBackendEndServiceEndPointHttp(apiEndpointPostfixUrl);
        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(backendEndPoint));
        apiRequest.setProvider(tenantAdmin);
        apiRequest.setVersion(apiVersion);
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(serviceResponse);

        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiName, tenantAdmin, APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);

        serviceResponse = apiStore.addApplication(applicationName,
                APIThrottlingTier.UNLIMITED.getState(), "", "this-is-test");
        verifyResponse(serviceResponse);

        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, tenantAdmin);
        subscriptionRequest.setApplicationName(applicationName);
        subscriptionRequest.setTier("Gold");
        serviceResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(serviceResponse);

        APPKeyRequestGenerator generateAppKeyRequest =
                new APPKeyRequestGenerator(applicationName);
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();

        JSONObject response = new JSONObject(responseString);

        accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Assert.assertNotNull("Access Token not found " + responseString, accessToken);
    }

    @Test(groups = {"wso2.am"}, description = "Check whether user can publish new copy with the " +
            " given Require Re-Subscription option")
    public void testPublishNewCopyGivenRequireReSubscription() throws Exception {

        String userDomain = storeContext.getContextTenant().getTenantUser(tenantKey).getUserDomain();

        //Invoke Original API
        String gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "t/" + userDomain + "/" + apiContext + "/" + apiVersion;

        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet httpGet = new HttpGet(gatewayUrl);
        httpGet.setHeader("Authorization", "Bearer " + accessToken);
        org.apache.http.HttpResponse backendResponse = httpclient.execute(httpGet);
        assertEquals(backendResponse.getHeaders("cookie").length, 2,
                "Expected two headers with the same name");
    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        apiStore.removeAPISubscriptionByName(apiName, apiVersion, providerNameApi, applicationName);
        apiStore.removeApplication(applicationName);
        apiPublisher.deleteAPI(apiName, apiVersion, providerNameApi);
    }

}

