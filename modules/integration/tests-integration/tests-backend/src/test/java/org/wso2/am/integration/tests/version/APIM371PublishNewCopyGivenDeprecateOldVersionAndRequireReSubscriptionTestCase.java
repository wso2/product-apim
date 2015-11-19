/*
 *
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.version;


import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.webapp.WebAppAdminClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.am.integration.test.utils.webapp.WebAppDeploymentUtil;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * publish new copy using "Deprecate old versions" and "Require Re-Subscription" options from the
 * below options and Invoke both APIs
 * Propagate Changes to API Gateway
 * Deprecate Old Versions
 * Require Re-Subscription
 */
public class APIM371PublishNewCopyGivenDeprecateOldVersionAndRequireReSubscriptionTestCase extends
        APIMIntegrationBaseTest {

    private final String apiName = "APIM371DeprecateReSubscriptionTestAPI";
    private final String apiVersionOld = "1.0.0";
    private final String apiVersionNew = "1.0.1";
    private final String applicationName = "APIM371DeprecateReSubscriptionAPI";
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String providerNameApi;
    private String apiEndPointUrl;
    private APIIdentifier apiIdentifierStoreOldVersion;
    private APIIdentifier apiIdentifierStoreNewVersion;

    @Factory(dataProvider = "userModeDataProvider")
    public APIM371PublishNewCopyGivenDeprecateOldVersionAndRequireReSubscriptionTestCase
            (TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
//                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        String apiEndpointPostfixUrl = "jaxrs_basic/services/customers/" +
                "customerservice/customers/123";
        String webAppName = "jaxrs_basic";
        String sourcePath = TestConfigurationProvider.getResourceLocation() + File.separator +
                "artifacts" + File.separator + "AM" + File.separator + "lifecycletest" +
                File.separator + webAppName + ".war";

        String sessionId = createSession(gatewayContextMgt);
        WebAppAdminClient webAppAdminClient =
                new WebAppAdminClient(gatewayContextMgt.getContextUrls().getBackEndUrl(), sessionId);
        webAppAdminClient.uploadWarFile(sourcePath);
        WebAppDeploymentUtil.isWebApplicationDeployed(gatewayContextMgt.getContextUrls().getBackEndUrl(),
                sessionId, webAppName);

        boolean isWebAppDeploy = WebAppDeploymentUtil.isWebApplicationDeployed
                (gatewayContextMgt.getContextUrls().getBackEndUrl(), sessionId, webAppName);

        assertEquals(isWebAppDeploy, true, webAppName + "is not deployed");

        apiPublisher = new APIPublisherRestClient(publisherUrls.getWebAppURLHttp());
        apiStore = new APIStoreRestClient(storeUrls.getWebAppURLHttp());

        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

        apiEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiEndpointPostfixUrl;

        providerNameApi = publisherContext.getContextTenant().getContextUser().getUserName();

        apiIdentifierStoreOldVersion = new APIIdentifier(providerNameApi, apiName, apiVersionOld);
        apiIdentifierStoreNewVersion = new APIIdentifier(providerNameApi, apiName, apiVersionNew);

    }

    @Test(groups = {"wso2.am"}, description = "Check whether user can publish new copy for the " +
            " given Deprecate Old Version And Require Re-Subscription option")

    public void testPublishNewCopyGivenDeprecateAndRequireReSubscription() throws Exception {

        String apiContext = "apim371DeprecateReSubscriptionTestAPI";
        String apiDescription = "This is Test API Created by API Manager " +
                "Integration Test";
        String apiTag = "tag371-1, tag371-2, tag371-3";
        String defaultVersion = "default_version";
        String apiLimit = "5";
        String superUser = "carbon.super";

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiName, apiContext, apiVersionOld, providerNameApi,
                        new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setDefault_version(defaultVersion);
        apiCreationRequestBean.setDefaultVersionChecked(defaultVersion);

        //Check availability of the API after creation in Publisher
        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        JSONObject jsonObjectCreation = new JSONObject(apiCreationResponse.getData());
        assertFalse(jsonObjectCreation.getBoolean("error"), apiName + "is not created");

        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiName, providerNameApi, APILifeCycleState.PUBLISHED);

        HttpResponse creationResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        assertTrue(creationResponse.getData().contains("PUBLISHED"),
                apiName + " has not been created in publisher");

        //Check Availability of the Application in Store
        HttpResponse appResponse = apiStore.addApplication(applicationName, "Gold", "", "");
        JSONObject appJsonObject = new JSONObject(appResponse.getData());
        assertFalse(appJsonObject.getBoolean("error"), applicationName + " is not created in Store");
        assertEquals(appJsonObject.getString("status"), "APPROVED",
                applicationName + " is not created in Store");

        //Check whether API has subscribed in Store
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, providerNameApi);
        subscriptionRequest.setApplicationName(applicationName);
        HttpResponse subscriptionResponse = apiStore.subscribe(subscriptionRequest);
        assertEquals(subscriptionResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                apiName + " has not been subscribed");

        //Copy Existing API and set it as the default version and validate the result
        HttpResponse copyApiResponse = apiPublisher.copyAPI
                (providerNameApi, apiName, apiVersionOld, apiVersionNew, defaultVersion);
        JSONObject jsonObjectCopy = new JSONObject(copyApiResponse.getData());
        assertFalse(jsonObjectCopy.getBoolean("error"), apiName + " is not copied");

        //Again Publish the API after copying
        APILifeCycleStateRequest lifeCycleStateRequest =
                new APILifeCycleStateRequest(apiName, providerNameApi, APILifeCycleState.PUBLISHED);
        lifeCycleStateRequest.setVersion(apiVersionNew);
        lifeCycleStateRequest.setDeprecateOldVersions("true");
        lifeCycleStateRequest.setRequireResubscription("true");
        apiPublisher.changeAPILifeCycleStatus(lifeCycleStateRequest);

        //Validate Status and Version of the Original API and New Copy of the API in Publisher
        HttpResponse originalApiResponse = apiPublisher.
                getAPI(apiName, providerNameApi, apiVersionOld);
        assertTrue(originalApiResponse.getData().contains(apiVersionOld),
                "Version of the original " + apiName + " has been updated with new version");
        assertTrue(originalApiResponse.getData().contains("DEPRECATED"),
                "Status of the original  " + apiName + " has not been changed as DEPRECATED");

        HttpResponse newApiResponse = apiPublisher.getAPI(apiName, providerNameApi, apiVersionNew);
        assertTrue(newApiResponse.getData().contains(apiVersionNew),
                "Version of the Copied " + apiName + " has not been updated with the new version");
        assertTrue(newApiResponse.getData().contains("PUBLISHED"),
                "Status of the copied " + apiName + " has not been changed as PUBLISHED");

        //To Update the store with Newly published API
        Thread.sleep(14000);

        //Check Availability of the API with New Copy under Recently added API List in Store
        List<APIIdentifier> recentlyAddedAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiStore.getRecentlyAddedAPIs
                        (superUser, apiLimit));
        assertTrue(APIMTestCaseUtils.isAPIAvailable
                        (apiIdentifierStoreNewVersion, recentlyAddedAPIList),
                "New Copy of the " + apiName + " is not available as a Recently added API in store");
        assertFalse(APIMTestCaseUtils.isAPIAvailable
                        (apiIdentifierStoreOldVersion, recentlyAddedAPIList),
                "Original " + apiName + " is available as a Recently Added Api in store");


        //Check Availability of the API with the New Copy in General Store
        List<APIIdentifier> publishedAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiStore.getAllPublishedAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierStoreNewVersion, publishedAPIList),
                "New Copy of the " + apiName + " is not available under API in store");
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifierStoreOldVersion, publishedAPIList),
                "Original " + apiName + " is available under API in store");


        //Check Availability of the Original API and New Copy of the API under Subscribed API
        JSONObject subscribedApi = new JSONObject(apiStore.getSubscribedAPIs(applicationName).
                getData()).getJSONObject("subscriptions");
        JSONArray jsonArray = subscribedApi.getJSONArray("applications");
        JSONObject jsonObjectDepApi = (JSONObject) jsonArray.get(0);
        JSONArray jsonArrayApiList = jsonObjectDepApi.getJSONArray("subscriptions");
        JSONObject deprecateApi = (JSONObject) jsonArrayApiList.get(0);
        assertEquals(deprecateApi.getString("status"), "DEPRECATED",
                "Invalid Status of the Deprecated " +apiName);
        assertEquals(deprecateApi.getString("version"), apiVersionOld,
                "Invalid Version of the Deprecated" + apiName);
        assertEquals(deprecateApi.getString("name"), apiName,
                "Invalid name of the Deprecated " + apiName);
        assertFalse(deprecateApi.getString("version").contains(apiVersionNew),
                "New Copy of the" +apiName + " is visible under Subscribed API");


        //Subscribe New Copy of the Re-Subscription API
        SubscriptionRequest ReSubscriptionRequest = new SubscriptionRequest(apiName, providerNameApi);
        ReSubscriptionRequest.setApplicationName(applicationName);
        ReSubscriptionRequest.setVersion(apiVersionNew);

        HttpResponse ReSubscriptionResponse = apiStore.subscribe(ReSubscriptionRequest);
        assertEquals(ReSubscriptionResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "ReSubscription API has not been subscribed");

        //Generate Key for the Original API
        APPKeyRequestGenerator appKeyRequestGenerator = new APPKeyRequestGenerator(applicationName);
        String response = apiStore.generateApplicationKey(appKeyRequestGenerator).getData();

        JSONObject jsonObject = new JSONObject(response);

        String accessToken =
                jsonObject.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "text/xml");

        Thread.sleep(3000);

        //Invoke Original API
        HttpResponse oldApiResponse = HttpRequestUtil.doGet(gatewayUrlsWrk.getWebAppURLNhttp() +
                apiContext + "/" + apiVersionOld, requestHeaders);
//        Thread.sleep(8000);
        assertEquals(oldApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");
        assertTrue(oldApiResponse.getData().contains
                        ("<Customer><id>123</id><name>John</name></Customer>"),
                "Response data mismatched when api invocation for GET Request");

        Thread.sleep(3000);

        //Invoke new version Copy of the API
        HttpResponse newVersionApiResponse = HttpRequestUtil.doGet(gatewayUrlsWrk.getWebAppURLNhttp() +
                apiContext + "/" + apiVersionNew, requestHeaders);
        assertEquals(newVersionApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");
        assertTrue(oldApiResponse.getData().contains
                        ("<Customer><id>123</id><name>John</name></Customer>"),
                "Response data mismatched when api invocation for GET Request");
    }


    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        apiStore.removeAPISubscriptionByName(apiName,apiVersionOld,providerNameApi,applicationName);
        apiStore.removeAPISubscriptionByName(apiName,apiVersionNew,providerNameApi,applicationName);
        apiStore.removeApplication(applicationName);
        apiPublisher.deleteAPI(apiName, apiVersionOld, providerNameApi);
        apiPublisher.deleteAPI(apiName, apiVersionNew, providerNameApi);
    }
}
