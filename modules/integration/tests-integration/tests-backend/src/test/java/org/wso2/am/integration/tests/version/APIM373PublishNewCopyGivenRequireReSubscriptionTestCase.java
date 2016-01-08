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
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.am.integration.test.utils.webapp.WebAppDeploymentUtil;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.am.integration.test.utils.http.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * publish new copy by selecting "Require re-subscription" option from below three options and
 * Invoke Original API and New Version API from Store for "GET/POST/PUT/DELETE" methods
 * Propagate Changes to API Gateway
 * Deprecate Old Versions
 * Require Re-Subscription
 */
public class APIM373PublishNewCopyGivenRequireReSubscriptionTestCase extends
        APIMIntegrationBaseTest {

    private final String apiName = "APIM373ReSubscriptionTestAPI";
    private final String apiVersionOld = "1.0.0";
    private final String apiVersionNew = "1.0.1";
    private String applicationName = "APIM373ReSubscriptionAPI";
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String providerNameApi;
    private String apiProductionEndPointUrl;
    private String apiSandBoxEndPointUrl;
    private APIIdentifier apiIdentifierOldVersion;
    private APIIdentifier apiIdentifierNewVersion;
    private HashMap<String, String> requestHeadersGet;
    private HashMap<String, String> requestHeadersPost;
    private HashMap<String, String> requestHeadersPut;
    private HashMap<String, String> requestHeadersDelete;

    @Factory(dataProvider = "userModeDataProvider")
    public APIM373PublishNewCopyGivenRequireReSubscriptionTestCase(TestUserMode userMode) {
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

        String sessionId = createSession(gatewayContextWrk);
        String apiProductionEndpointPostfixUrl = "jaxrs_basic/services/" +
                "customers/customerservice/";
        String apiSandBoxEndpointPostfixUrl = "name-check1/";
        String productionWebAppName = "jaxrs_basic";
        String SanBoxWebAppName = "name-check1";

        String sourcePathProd = TestConfigurationProvider.getResourceLocation() + File.separator +
                "artifacts" + File.separator + "AM" + File.separator + "lifecycletest" +
                File.separator + productionWebAppName + ".war";

        String sourcePathSand = TestConfigurationProvider.getResourceLocation() + File.separator +
                "artifacts" + File.separator + "AM" + File.separator + "lifecycletest" +
                File.separator + SanBoxWebAppName + ".war";

        WebAppAdminClient webAppAdminClient =
                new WebAppAdminClient(gatewayContextWrk.getContextUrls().getBackEndUrl(), sessionId);
        webAppAdminClient.uploadWarFile(sourcePathProd);
        webAppAdminClient.uploadWarFile(sourcePathSand);

        boolean isWebAppDeployProd = WebAppDeploymentUtil.isWebApplicationDeployed
                (gatewayContextWrk.getContextUrls().getBackEndUrl(), sessionId, productionWebAppName);
        assertTrue(isWebAppDeployProd, productionWebAppName + " is not deployed");
        boolean isWebAppDeploySand = WebAppDeploymentUtil.isWebApplicationDeployed
                (gatewayContextWrk.getContextUrls().getBackEndUrl(), sessionId, SanBoxWebAppName);
        assertTrue(isWebAppDeploySand, SanBoxWebAppName + " is not deployed");

        apiPublisher = new APIPublisherRestClient(publisherUrls.getWebAppURLHttp());
        apiStore = new APIStoreRestClient(storeUrls.getWebAppURLHttp());

        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

        apiProductionEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiProductionEndpointPostfixUrl;
        apiSandBoxEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiSandBoxEndpointPostfixUrl;

        providerNameApi = publisherContext.getContextTenant().getContextUser().getUserName();

        apiIdentifierOldVersion = new APIIdentifier(providerNameApi, apiName, apiVersionOld);
        apiIdentifierNewVersion = new APIIdentifier(providerNameApi, apiName, apiVersionNew);

        requestHeadersGet = new HashMap<String, String>();
        requestHeadersGet.put("accept", "text/xml");
        requestHeadersPost = new HashMap<String, String>();
        requestHeadersPost.put("accept", "text/xml");
        requestHeadersPost.put("Content-Type", "text/xml");
        requestHeadersPut = new HashMap<String, String>();
        requestHeadersPut.put("accept", "text/xml");
        requestHeadersPut.put("Content-Type", "text/xml");
        requestHeadersDelete = new HashMap<String, String>();
        requestHeadersDelete.put("accept", "text/xml");

    }

    @Test(groups = {"wso2.am"}, description = "Check whether user can publish new copy for the " +
            " given Require Re-Subscription option")

    public void testPublishNewCopyGivenRequireReSubscription() throws Exception {

        List<APIResourceBean> resourceBeanList = new ArrayList<APIResourceBean>();
        String apiContext = "apim373ReSubscriptionTestAPI";
        String apiDescription = "This is Test API Created by API Manager " +
                "Integration Test";
        String apiTag = "tag373-1, tag373-2, tag373-3";
        String defaultVersion = "default_version";
        String apiLimit = "5";
        String superUser = "carbon.super";
        String apiResponse = "<Customer><id>123</id><name>John</name></Customer>";

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiName, apiContext, apiVersionOld, providerNameApi,
                        new URL(apiProductionEndPointUrl), new URL(apiSandBoxEndPointUrl));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setDefault_version(defaultVersion);
        apiCreationRequestBean.setDefaultVersionChecked(defaultVersion);
        resourceBeanList.add
                (new APIResourceBean("GET", "Application & Application User", "Unlimited", "/*"));
        resourceBeanList.add
                (new APIResourceBean("POST", "Application & Application User", "Unlimited", "/*"));
        resourceBeanList.add
                (new APIResourceBean("PUT", "Application & Application User", "Unlimited", "/*"));
        resourceBeanList.add
                (new APIResourceBean("DELETE", "Application & Application User", "Unlimited", "/*"));
        apiCreationRequestBean.setResourceBeanList(resourceBeanList);

        //Check availability of the API after creation in Publisher
        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        JSONObject jsonObjectCreation = new JSONObject(apiCreationResponse.getData());
        assertFalse(jsonObjectCreation.getBoolean("error"), apiName + "is not created");

        //Check status of the published API in Publisher
        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiName, providerNameApi, APILifeCycleState.PUBLISHED);

        HttpResponse creationResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        assertTrue(creationResponse.getData().contains("PUBLISHED"), "API has not been created in publisher");

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
        lifeCycleStateRequest.setRequireResubscription("true");
        HttpResponse statusResponse = apiPublisher.changeAPILifeCycleStatus(lifeCycleStateRequest);
        assertTrue(statusResponse.getData().contains("PUBLISHED"),
                "New Copy of the " + apiName + " is not published");

        //To Update the store with Newly published API
        Thread.sleep(15000);

        //Check Availability of the API with New Copy under Recently added API List in Store
        List<APIIdentifier> recentlyAddedAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiStore.getRecentlyAddedAPIs
                        (superUser, apiLimit));
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierNewVersion, recentlyAddedAPIList),
                "New Copy of the " + apiName + " is not available under Recently added API in store");
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifierOldVersion, recentlyAddedAPIList),
                "Original version of the " + apiName +
                        " is available under Recently Added Api in store");


        //Check Availability of the API with the New Copy in General Store
        List<APIIdentifier> publishedAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiStore.getAllPublishedAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierNewVersion, publishedAPIList),
                "New Copy of the" + apiName + "is not available in store");
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifierOldVersion, publishedAPIList),
                "Original" + apiName + " is available in store");


        //Check Availability of the Original API and New Copy of the API under Subscribed API
        JSONObject subscribedApi = new JSONObject(apiStore.getSubscribedAPIs(applicationName).
                getData()).getJSONObject("subscriptions");
        JSONArray jsonArray = subscribedApi.getJSONArray("applications");
        JSONObject jsonObjectDepApi = (JSONObject) jsonArray.get(0);
        JSONArray jsonArrayApiList = jsonObjectDepApi.getJSONArray("subscriptions");
        JSONObject jsonObjectNewCopyApi = (JSONObject) jsonArrayApiList.get(0);
        assertEquals(jsonObjectNewCopyApi.getString("status"), "PUBLISHED",
                "Invalid Status of the Original " + apiName);
        assertEquals(jsonObjectNewCopyApi.getString("version"), apiVersionOld,
                "Invalid Version of the Original " + apiName);
        assertEquals(jsonObjectNewCopyApi.getString("name"), apiName,
                "Invalid name of the Original " + apiName);
        assertFalse(jsonObjectNewCopyApi.getString("version").contains(apiVersionNew),
                "New Copy of the " + apiName + " is visible under Subscribed API");


        //Generate Key for the Original API
        APPKeyRequestGenerator appKeyRequestGenerator = new APPKeyRequestGenerator(applicationName);
        String response = apiStore.generateApplicationKey(appKeyRequestGenerator).getData();

        JSONObject jsonObject = new JSONObject(response);

        String accessToken =
                jsonObject.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeadersGet.put("Authorization", "Bearer " + accessToken);
        requestHeadersPost.put("Authorization", "Bearer " + accessToken);
        requestHeadersPut.put("Authorization", "Bearer " + accessToken);
        requestHeadersDelete.put("Authorization", "Bearer " + accessToken);

//        Thread.sleep(4000);

        //Invoke Original API
        HttpResponse oldApiResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp
                (apiContext + "/" + apiVersionOld )+ "/customers/123", requestHeadersGet);
        assertEquals(oldApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");
        assertTrue(oldApiResponse.getData().contains(apiResponse),
                "Response data mismatched when api invocation for GET Request");

        //Invoke New version of the API
        HttpResponse newApiResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp
                (apiContext + "/" + apiVersionNew), requestHeaders);
        assertEquals(newApiResponse.getResponseCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                "Response code mismatched when invalid api invocation");

        //Re-subscribed the API and Check the New Copy and Original API under Subscribed APIs
        SubscriptionRequest ReSubscriptionRequest = new SubscriptionRequest(apiName, providerNameApi);
        ReSubscriptionRequest.setApplicationName(applicationName);
        ReSubscriptionRequest.setVersion(apiVersionNew);
        apiStore.subscribe(ReSubscriptionRequest);

        JSONObject ReSubscribedApi = new JSONObject(apiStore.getSubscribedAPIs(applicationName).
                getData()).getJSONObject("subscriptions");
        JSONArray jsonArraySub = ReSubscribedApi.getJSONArray("applications");
        JSONObject jsonObjectReSub = (JSONObject) jsonArraySub.get(0);
        JSONArray jsonArrayReSub = jsonObjectReSub.getJSONArray("subscriptions");
        JSONObject jsonObjectReSubCopy = (JSONObject) jsonArrayReSub.get(1);
        assertEquals(jsonObjectReSubCopy.getString("status"), "PUBLISHED",
                "Invalid Status of the Original" + apiName);
        assertEquals(jsonObjectReSubCopy.getString("version"), apiVersionNew,
                "Invalid Version of the Original" + apiName);
        assertEquals(jsonObjectNewCopyApi.getString("name"), apiName,
                "Invalid name of the Original " + apiName);

        JSONObject jsonObjectReSubOldCopy = (JSONObject) jsonArrayReSub.get(0);
        assertEquals(jsonObjectReSubOldCopy.getString("status"), "PUBLISHED",
                "Invalid Status of the Original API API");
        assertEquals(jsonObjectReSubOldCopy.getString("version"), apiVersionOld,
                "Invalid Version of the Original API");
        assertEquals(jsonObjectReSubOldCopy.getString("name"), apiName,
                "Invalid name of the Original API");


        //Invoke Production environment of the New version API using GET method
        HttpResponse newApiResponseGet = HttpRequestUtil.doGet(getAPIInvocationURLHttp
                ( apiContext + "/" + apiVersionNew )+ "/customers/123", requestHeadersGet);
        assertEquals(newApiResponseGet.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when New version api invocation for GET Request");
        assertTrue(newApiResponseGet.getData().contains(apiResponse),
                "Response data mismatched when New version api invocation for GET Request");

        //Invoke Production environment of the New version API using POST method
        HttpResponse newApiResponsePost =HttpRequestUtil.doPost(new URL
                (getAPIInvocationURLHttp( apiContext + "/" + apiVersionNew) +
                "/customers/"), "<Customer><name>Evin</name></Customer>", requestHeadersPost);
        assertEquals(newApiResponsePost.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when New version api invocation for POST Request");
        assertTrue(newApiResponsePost.getData().contains
                        ("<Customer><id>124</id><name>Evin</name></Customer>"),
                "New Customer Record is not added as expected for the POST Request");

        //Invoke Production environment of the New version API using Get Method to validate the
        // new customer creation by using post method
        HttpResponse newApiResponseGetPost = HttpRequestUtil.doGet(getAPIInvocationURLHttp
                (apiContext + "/" + apiVersionNew )+ "/customers/124", requestHeadersGet);
        assertEquals(newApiResponseGetPost.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when New version of the api invocation for GET Request");
        assertTrue(newApiResponseGetPost.getData().contains
                        ("<Customer><id>124</id><name>Evin</name></Customer>"),
                "Response data mismatched when New version of the api invocation for GET Request");

        //Invoke Production environment of the New version API using PUT method
        HttpResponse newApiResponsePut = HttpRequestUtil.doPut(new URL
                        (getAPIInvocationURLHttp( apiContext + "/" + apiVersionNew )+
                                "/customers/"),
                        "<Customer><id>124</id><name>James</name></Customer>", requestHeadersPut);
        assertEquals(newApiResponsePut.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when New version of the api invocation for PUT Request");

        //Invoke Production environment of the New version API using Get Method to validate the
        // updated customer by using put method
        HttpResponse newApiResponseGetPut = HttpRequestUtil.doGet(getAPIInvocationURLHttp
                ( apiContext + "/" + apiVersionNew )+ "/customers/124", requestHeadersGet);
        assertEquals(newApiResponseGetPut.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when New version of the api invocation for GET Request");
        assertTrue(newApiResponseGetPut.getData().contains
                        ("<Customer><id>124</id><name>James</name></Customer>"),
                "Response data mismatched when New version of the api invocation for GET Request");

        //Invoke Production environment of the New version API using DELETE method
        int newApiResponseDelete = HttpRequestUtil.doDelete(new URL(getAPIInvocationURLHttp
                (apiContext + "/" + apiVersionNew) + "/customers/124"), requestHeadersDelete);
        assertEquals(newApiResponseDelete, Response.Status.OK.getStatusCode(),
                "Response code mismatched when new version of the api invocation for DELETE Request");

        //Invoke Production environment of the New version API using Get Method to validate the
        // customer deletion by using delete method
        HttpResponse newApiResponseGetDelete = HttpRequestUtil.
                doGet(getAPIInvocationURLHttp( apiContext + "/" +
                        apiVersionNew) + "/customers/124", requestHeadersGet);
        assertEquals(newApiResponseGetDelete.getResponseCode(), Response.Status.NO_CONTENT.
                        getStatusCode(), "Response code mismatched when new version of the api " +
                "invocation for GET request after delete the requested data");
    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        apiStore.removeAPISubscriptionByName
                (apiName, apiVersionOld, providerNameApi, applicationName);
        apiStore.removeAPISubscriptionByName
                (apiName, apiVersionNew, providerNameApi, applicationName);
        apiStore.removeApplication(applicationName);
        apiPublisher.deleteAPI(apiName, apiVersionOld, providerNameApi);
        apiPublisher.deleteAPI(apiName, apiVersionNew, providerNameApi);
    }


}


