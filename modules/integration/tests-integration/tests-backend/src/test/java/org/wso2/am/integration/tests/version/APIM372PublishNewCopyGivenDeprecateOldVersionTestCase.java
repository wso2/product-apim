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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
/**
 * publish new copy using "Deprecate old versions" from the below options and Invoke Deprecated API
 * and Published API from the Store for "GET/POST/PUT/DELETE" methods
 * Propagate Changes to API Gateway
 * Deprecate Old Versions
 * Require Re-Subscription
 */
public class APIM372PublishNewCopyGivenDeprecateOldVersionTestCase extends APIMIntegrationBaseTest {

    private final String apiName = "APIM372DeprecateTestAPI";
    private final String apiVersionOld = "1.0.0";
    private final String apiVersionNew = "1.0.1";
    private final String applicationName = "APIM372DeprecateAPI";
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String providerNameApi;
    private String apiProductionEndPointUrl;

    private APIIdentifier apiIdentifierOldVersion;
    private APIIdentifier apiIdentifierNewVersion;
    private HashMap<String, String> requestHeadersGet;
    private HashMap<String, String> requestHeadersPost;
    private HashMap<String, String> requestHeadersPut;
    private HashMap<String, String> requestHeadersDelete;

    @Factory(dataProvider = "userModeDataProvider")
    public APIM372PublishNewCopyGivenDeprecateOldVersionTestCase(TestUserMode userMode) {
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

        String apiProductionEndpointPostfixUrl = "jaxrs_basic/services/" +
                "customers/customerservice/";
        String productionWebAppName = "jaxrs_basic";
        String sourcePathProd = TestConfigurationProvider.getResourceLocation() + File.separator +
                "artifacts" + File.separator + "AM" + File.separator + "lifecycletest" +
                File.separator + productionWebAppName + ".war";
        String sessionId = createSession(gatewayContextWrk);

        WebAppAdminClient webAppAdminClient =
                new WebAppAdminClient(gatewayContextWrk.getContextUrls().getBackEndUrl(), sessionId);
        webAppAdminClient.uploadWarFile(sourcePathProd);

        boolean isWebAppDeployProd = WebAppDeploymentUtil.isWebApplicationDeployed
                (gatewayContextWrk.getContextUrls().getBackEndUrl(), sessionId, productionWebAppName);
        assertTrue(isWebAppDeployProd, productionWebAppName + " is not deployed");

        apiPublisher = new APIPublisherRestClient(publisherUrls.getWebAppURLHttp());
        apiStore = new APIStoreRestClient(storeUrls.getWebAppURLHttp());

        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

        apiProductionEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiProductionEndpointPostfixUrl;
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
            " given deprecate old versions option")

    public void testPublishNewCopyGivenDeprecateOldVersion() throws Exception {

        String apiContext = "apim372deprecateTestAPI";
        String apiDescription = "This is Test API Created by API Manager " +
                "Integration Test";
        String apiTag = "tag372-1, tag372-2, tag372-3";
        List<APIResourceBean> resourceBeanList = new ArrayList<APIResourceBean>();

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiName, apiContext, apiVersionOld, providerNameApi,
                        new URL(apiProductionEndPointUrl));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);
        resourceBeanList.add
                (new APIResourceBean("GET", "Application & Application User", "Unlimited", "/*"));
        resourceBeanList.add
                (new APIResourceBean("POST", "Application & Application User", "Unlimited", "/*"));
        resourceBeanList.add
                (new APIResourceBean("PUT", "Application & Application User", "Unlimited", "/*"));
        resourceBeanList.add
                (new APIResourceBean("DELETE", "Application & Application User", "Unlimited", "/*"));
        apiCreationRequestBean.setResourceBeanList(resourceBeanList);

        //Check the availability of the API after creation in Publisher
        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        JSONObject jsonObjectCreation = new JSONObject(apiCreationResponse.getData());
        assertFalse(jsonObjectCreation.getBoolean("error"), apiName + "is not created");

        //Check the status update of the API after publishing
        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiName, providerNameApi, APILifeCycleState.PUBLISHED);
        HttpResponse creationResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        assertTrue(creationResponse.getData().contains("PUBLISHED"),
                apiName + " is not Published ");

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
                apiName + "  has not been subscribed");

        //Copy Existing API and set it as the default version and validate it
        HttpResponse copyApiResponse = apiPublisher.copyAPI
                (providerNameApi, apiName, apiVersionOld, apiVersionNew, null);
        JSONObject jsonObjectCopyApi = new JSONObject(copyApiResponse.getData());
        assertFalse(jsonObjectCopyApi.getBoolean("error"),apiName + " is not copied");

        //Again Publish the API after copying
        APILifeCycleStateRequest lifeCycleStateRequest =
                new APILifeCycleStateRequest(apiName, providerNameApi, APILifeCycleState.PUBLISHED);
              lifeCycleStateRequest.setVersion(apiVersionNew);
        lifeCycleStateRequest.setDeprecateOldVersions("true");

        apiPublisher.changeAPILifeCycleStatus(lifeCycleStateRequest);

        //Validate Status and Version of the Original API and New Copy of the API in Publisher
        HttpResponse originalApiResponse = apiPublisher.getAPI
                (apiName, providerNameApi, apiVersionOld);
        assertTrue(originalApiResponse.getData().contains(apiVersionOld),
                "Version of the original " +apiName + " has been updated with new version");
        assertTrue(originalApiResponse.getData().contains("DEPRECATED"),
                "Status of the original " +apiName + " has not been changed as DEPRECATED");

        HttpResponse newApiResponse = apiPublisher.getAPI(apiName, providerNameApi, apiVersionNew);
        assertTrue(newApiResponse.getData().contains(apiVersionNew),
                "Version of the Copied API has not been updated with the new version");
        assertTrue(newApiResponse.getData().contains("PUBLISHED"),
                "Status of the new " +apiName + " has not been changed as PUBLISHED");


        //Validate availability of the API with the new version in Store
        List<APIIdentifier> publishedAPIList = APIMTestCaseUtils.
                getAPIIdentifierListFromHttpResponse(apiStore.getAllPublishedAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierNewVersion, publishedAPIList),
                apiName + "  with the new Version is not available in store");
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifierOldVersion, publishedAPIList),
               apiName + " with the Old Version is available in store");

        //Validate the Version and Status of the Original API under Subscribed API
        JSONObject subscribedApi = new JSONObject(apiStore.getSubscribedAPIs(applicationName).
                getData()).getJSONObject("subscriptions");
        JSONArray jsonArray = subscribedApi.getJSONArray("applications");
        JSONObject jsonObjectDepApi = (JSONObject) jsonArray.get(0);
        JSONArray jsonArrayApiList = jsonObjectDepApi.getJSONArray("subscriptions");
        JSONObject deprecateApi = (JSONObject) jsonArrayApiList.get(0);
        assertEquals(deprecateApi.getString("status"), "DEPRECATED",
                "Invalid Status of the Deprecated API");
        assertEquals(deprecateApi.getString("version"), apiVersionOld,
                "Invalid Version of the Deprecated API");
        assertEquals(deprecateApi.getString("name"), apiName,
                "Invalid name of the Deprecated API");


        //Validate the Version and Status of the New API under Subscribed API
        JSONObject publishApi = (JSONObject) jsonArrayApiList.get(1);
        assertEquals(publishApi.getString("status"), "PUBLISHED",
                "Invalid Status of the Newly published API");
        assertEquals(publishApi.getString("version"), apiVersionNew,
                "Invalid Version of the Newly published API");
        assertEquals(publishApi.getString("name"), apiName,
                "Invalid name of the Newly published API");

        //Generate Key for the Published API
        APPKeyRequestGenerator appKeyRequestGenerator = new APPKeyRequestGenerator(applicationName);
        String response = apiStore.generateApplicationKey(appKeyRequestGenerator).getData();
        JSONObject jsonObject = new JSONObject(response);

        String accessToken =
                jsonObject.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        requestHeadersGet.put("Authorization", "Bearer " + accessToken);
        requestHeadersPost.put("Authorization", "Bearer " + accessToken);
        requestHeadersPut.put("Authorization", "Bearer " + accessToken);
        requestHeadersDelete.put("Authorization", "Bearer " + accessToken);

        //Invoke Production environment of the Deprecated API using GET method
        HttpResponse oldApiResponseGet = HttpRequestUtil.doGet(getAPIInvocationURLHttp
                ( apiContext + "/" + apiVersionOld ) + "/customers/123", requestHeadersGet);
        Thread.sleep(15000);
        assertEquals(oldApiResponseGet.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when Old version api invocation for GET Request");
        assertTrue(oldApiResponseGet.getData().contains
                        ("<Customer><id>123</id><name>John</name></Customer>"),
                "Response data mismatched when old version api invocation for GET Request");

        //Invoke Production environment of the Deprecated API using POST method
        HttpResponse oldApiResponsePost = HttpRequestUtil.doPost
                (new URL(getAPIInvocationURLHttp( apiContext + "/" + apiVersionOld )+
                "/customers/"), "<Customer><name>Jack</name></Customer>", requestHeadersPost);
        Thread.sleep(12000);

        assertEquals(oldApiResponsePost.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when Old version api invocation for POST Request");
        assertTrue(oldApiResponsePost.getData().contains
                        ("<Customer><id>124</id><name>Jack</name></Customer>"),
                "New Customer Record is not added as expected for the POST Request");

        //Invoke Production environment of the Deprecated API using Get Method to validate the
        // new customer creation by using post method
        HttpResponse oldApiResponseGetPost = HttpRequestUtil.doGet(getAPIInvocationURLHttp(
                apiContext + "/" + apiVersionOld) + "/customers/124", requestHeadersGet);
        Thread.sleep(12000);

        assertEquals(oldApiResponseGetPost.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");
        assertTrue(oldApiResponseGetPost.getData().contains
                        ("<Customer><id>124</id><name>Jack</name></Customer>"),
                "Response data mismatched when api invocation for GET Request");

        //Invoke Production environment of the Deprecated API using PUT method
        HttpResponse oldApiResponsePut = HttpRequestUtil.doPut
                (new URL(getAPIInvocationURLHttp(apiContext + "/" + apiVersionOld) +
                        "/customers/"), "<Customer><id>124</id><name>Tom</name></Customer>",
                        requestHeadersPut);
        assertEquals(oldApiResponsePut.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation for PUT Request");

        //Invoke Production environment of the Deprecated API using Get Method to validate the
        // updated customer by using put method
        HttpResponse oldApiResponseGetPut = HttpRequestUtil.doGet(getAPIInvocationURLHttp(
                apiContext + "/" + apiVersionOld) + "/customers/124", requestHeadersGet);
        assertEquals(oldApiResponseGetPut.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");
        assertTrue(oldApiResponseGetPut.getData().contains
                        ("<Customer><id>124</id><name>Tom</name></Customer>"),
                "Response data mismatched when api invocation for GET Request");

        //Invoke Production environment of the Deprecated API using DELETE method
        int oldApiResponseDelete = HttpRequestUtil.doDelete
                (new URL(getAPIInvocationURLHttp( apiContext + "/" + apiVersionOld) +
                        "/customers/124"), requestHeadersDelete);
        assertEquals(oldApiResponseDelete, Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation for DELETE Request");

        //Invoke Production environment of the Deprecated API using Get Method to validate the
        // customer deletion by using delete method
        HttpResponse oldApiResponseGetDelete = HttpRequestUtil.doGet
                (getAPIInvocationURLHttp( apiContext + "/" + apiVersionOld )+
                        "/customers/124", requestHeadersGet);
        assertEquals(oldApiResponseGetDelete.getResponseCode(),
                Response.Status.NO_CONTENT.getStatusCode(),
                "Response code mismatched when old version of the api invocation for GET request " +
                        "after delete the requested data");

        //Invoke Production environment of the New version API using GET method
        HttpResponse newApiResponseGet = HttpRequestUtil.doGet(getAPIInvocationURLHttp(
                apiContext + "/" + apiVersionNew )+ "/customers/123", requestHeadersGet);
        assertEquals(newApiResponseGet.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when New version api invocation for GET Request");
        assertTrue(newApiResponseGet.getData().contains
                        ("<Customer><id>123</id><name>John</name></Customer>"),
                "Response data mismatched when New version api invocation for GET Request");

        //Invoke Production environment of the New version API using POST method
        HttpResponse newApiResponsePost = HttpRequestUtil.doPost(new URL
                (getAPIInvocationURLHttp( apiContext + "/" + apiVersionNew) +
                "/customers/"), "<Customer><name>Evin</name></Customer>", requestHeadersPost);
        assertEquals(newApiResponsePost.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when New version api invocation for POST Request");
        assertTrue(newApiResponsePost.getData().contains
                        ("<Customer><id>125</id><name>Evin</name></Customer>"),
                "New Customer Record is not added as expected for the POST Request");

        //Invoke Production environment of the New version API using Get Method to validate the
        // new customer creation by using post method
        HttpResponse newApiResponseGetPost = HttpRequestUtil.doGet(getAPIInvocationURLHttp(
                apiContext + "/" + apiVersionNew) + "/customers/125", requestHeadersGet);
        assertEquals(newApiResponseGetPost.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when New version of the api invocation for GET Request");
        assertTrue(newApiResponseGetPost.getData().contains
                        ("<Customer><id>125</id><name>Evin</name></Customer>"),
                "Response data mismatched when New version of the api invocation for GET Request");

        //Invoke Production environment of the New version API using PUT method
        HttpResponse newApiResponsePut = HttpRequestUtil.doPut
                (new URL(getAPIInvocationURLHttp( apiContext + "/" + apiVersionNew) +
                                "/customers/"),
                "<Customer><id>125</id><name>James</name></Customer>", requestHeadersPut);
        assertEquals(newApiResponsePut.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when New version of the api invocation for PUT Request");

        //Invoke Production environment of the New version API using Get Method to validate the
        // updated customer by using put method
        HttpResponse newApiResponseGetPut = HttpRequestUtil.doGet(getAPIInvocationURLHttp(
                apiContext + "/" + apiVersionNew) + "/customers/125", requestHeadersGet);
        assertEquals(newApiResponseGetPut.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when New version of the api invocation for GET Request");
        assertTrue(newApiResponseGetPut.getData().contains
                        ("<Customer><id>125</id><name>James</name></Customer>"),
                "Response data mismatched when New version of the api invocation for GET Request");

        //Invoke Production environment of the New version API using DELETE method
        int newApiResponseDelete = HttpRequestUtil.doDelete
                (new URL(getAPIInvocationURLHttp(
                apiContext + "/" + apiVersionNew )+ "/customers/125"), requestHeadersDelete);
        assertEquals(newApiResponseDelete, Response.Status.OK.getStatusCode(),
                "Response code mismatched when new version of the api invocation for DELETE Request");

        //Invoke Production environment of the New version API using Get Method to validate the
        // customer deletion by using delete method
        HttpResponse newApiResponseGetDelete = HttpRequestUtil.doGet
                (getAPIInvocationURLHttp(apiContext + "/" + apiVersionNew )+
                        "/customers/125", requestHeadersGet);
        assertEquals(newApiResponseGetDelete.getResponseCode(),
                Response.Status.NO_CONTENT.getStatusCode(),
                "Response code mismatched when new version of the api invocation for" +
                        " GET request after delete the requested data");

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
        super.cleanUp();
    }




}
