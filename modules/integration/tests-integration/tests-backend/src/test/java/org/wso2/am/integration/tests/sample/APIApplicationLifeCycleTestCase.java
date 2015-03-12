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

package org.wso2.am.integration.tests.sample;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.common.TenantManagementServiceClient;
import org.wso2.am.integration.test.utils.APIMgtTestUtil;
import org.wso2.am.integration.test.utils.base.AMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.publisher.utils.APIPublisherRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class APIApplicationLifeCycleTestCase extends AMIntegrationBaseTest {
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private UserManagementClient userManagementClient;
    private TenantManagementServiceClient tenantManagementServiceClient;
    //move to base class
    private String publisherURLHttp;
    private String storeURLHttp;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        publisherURLHttp = getServerURLHttp();
        storeURLHttp = getServerURLHttp();
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);

        userManagementClient = new UserManagementClient(
                apimContext.getContextUrls().getBackEndUrl(), getSessionCookie());

        tenantManagementServiceClient = new TenantManagementServiceClient(
                apimContext.getContextUrls().getBackEndUrl(), getSessionCookie());
    }

    @Test(groups = {"wso2.am"}, description = "API Life cycle test case")
    public void testAPIApplicationLifeCycleITestCase() throws Exception {

        String apiData = "";
        String APIName = "APILifeCycleTestAPI";
        String APIContext = "testAPI";
        String tags = "youtube, video, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";
        String providerName = "admin";
        String APIVersion = "1.0.0";
        // This is because with the new context version strategy, if the context does not have the {version} param ,
        // then we add the {version} param to the end of the context.
        String apiContextAddedValue = APIContext + "/" + APIVersion;

        //add all option methods
        apiPublisher.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setWsdl("https://svn.wso2.org/repos/wso2/carbon/platform/trunk/products" +
                "/bps/modules/samples/product/src/main/resources/bpel/2.0/MyRoleMexTestProcess/echo.wsdl");
        apiRequest.setVisibility("restricted");
        apiRequest.setRoles("admin");
        apiPublisher.addAPI(apiRequest);
        apiPublisher.deleteApi(APIName, APIVersion, providerName);
        //add assertion
        apiPublisher.addAPI(apiRequest);
        APIBean apiBean = APIMgtTestUtil.getAPIBeanFromHttpResponse(apiPublisher.getApi(
                APIName, providerName));
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(APIName, providerName,
                APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatusTo(updateRequest);
        //Test API properties
        assertEquals(apiBean.getId().getApiName(), APIName, "API Name mismatch");
        assertEquals(apiBean.getContext().trim().substring(apiBean.getContext().indexOf("/") + 1),
                     apiContextAddedValue, "API context mismatch");
        assertEquals(apiBean.getId().getVersion(), APIVersion, "API version mismatch");
        assertEquals(apiBean.getId().getProviderName(), providerName, "Provider Name mismatch");
        for (String tag : apiBean.getTags()) {
           assertTrue(tags.contains(tag), "API tag data mismatched");
        }
        assertEquals(apiBean.getDescription(), description, "API description mismatch");
        apiStore.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
        apiStore.addApplication("APILifeCycleTestAPI-application", "Gold", "", "this-is-test");
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName,
                apimContext.getContextTenant().getContextUser().getUserName());
        subscriptionRequest.setApplicationName("APILifeCycleTestAPI-application");
        apiStore.subscribe(subscriptionRequest);

        GenerateAppKeyRequest generateAppKeyRequest = new GenerateAppKeyRequest("APILifeCycleTestAPI-application");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        //Here add API tags and check same have in response.
        //Here check same tags are there
        //Add some comment to API
        //check comment is there
        //Add rating
        //check rating
        Thread.sleep(60000);
        //  for (int i = 0; i < 19; i++) {

        HttpResponse youTubeResponse = HttpRequestUtil.doGet(getGatewayServerURLHttp()+"/testAPI/1.0.0/most_popular", requestHeaders);
        assertEquals(youTubeResponse.getResponseCode(), 200, "Response code mismatched");
        assertTrue(youTubeResponse.getData().contains("<feed"), "Response data mismatched");
        assertTrue(youTubeResponse.getData().contains("<category"), "Response data mismatched");
        assertTrue(youTubeResponse.getData().contains("<entry>"), "Response data mismatched");

        // }
        //Do get,post,put,delete all here
        //HttpResponse youTubeResponse = HttpRequestUtil.doGet(getApiInvocationURLHttp("commentRating/1.0.0/most_popular"), requestHeaders);
        //Assert.assertEquals(youTubeResponse.getResponseCode(), 503, "Response code mismatched");
        //Thread.sleep(60000);
        HttpResponse youTubeResponse1 = HttpRequestUtil.doGet(getGatewayServerURLHttp()+"/testAPI/1.0.0/most_popular", null);
        assertEquals(youTubeResponse1.getResponseCode(), 401, "Response code mismatched");
        requestHeaders.clear();
        requestHeaders.put("Authorization", "Bearer " + "-wrong-tokent-text-");
        HttpResponse youTubeResponseError = HttpRequestUtil.doGet(getGatewayServerURLHttp()+"/testAPI/1.0.0/most_popular", null);
        assertEquals(youTubeResponseError.getResponseCode(), 401, "Response code mismatched");

        apiStore.getAllPublishedAPIs();
        apiStore.getAllApplications();
        apiStore.getPublishedAPIsByApplication("APILifeCycleTestAPI-application");
        apiStore.isRatingActivated();
        apiStore.addRatingToAPI(APIName, APIVersion, providerName, "4");
        apiStore.addRatingToAPI(APIName, APIVersion, providerName, "2");
        apiStore.addRatingToAPI(APIName, APIVersion, providerName, "1");
        apiStore.removeRatingFromAPI(APIName, APIVersion, providerName);
        apiStore.getAllDocumentationOfApi(APIName, APIVersion, providerName);
        //apiStore.getAllPaginatedPublishedAPIs("carbon.super","0","10");
        //Negative cases
        apiStore.getPublishedAPIsByApplication("APILifeCycleTestAPI-application-wrong");
        apiStore.isRatingActivated();
        apiStore.addRatingToAPI("NoAPI", APIVersion, providerName, "4");
        apiStore.removeRatingFromAPI("NoAPI", APIVersion, providerName);
        apiStore.getAllDocumentationOfApi("NoAPI", APIVersion, providerName);
        apiRequest.setTags("updated");
        apiRequest.setProvider("admin");
        Thread.sleep(1000);
        apiPublisher.updateAPI(apiRequest);
        //TODO need to reformat this code after we finalize new APIs
        apiPublisher.addDocument(APIName, APIVersion, providerName, "Doc Name", "How To", "In-line",
                "url-no-need", "summary", "");
        apiPublisher.addDocument(APIName, APIVersion, providerName, "Doc Name1", "How To", "URL",
                "http://www.businesstoday.lk/article.php?article=3549", "summary", "");
        apiPublisher.addDocument(APIName, APIVersion, providerName, "Doc Name2", "How To", " File",
                "url-no-need", "summary",
                getAMResourceLocation() + File.separator + "configFiles/tokenTest/" + "api-manager.xml");
        apiPublisher.removeDocumentation(APIName, APIVersion, providerName, "Doc Name", "How To");
        //create application
        apiStore.addApplication("test-application", "Gold", "", "this-is-test");
        apiStore.addApplication("test-application2", "Gold", "", "this-is-test");
        apiStore.getAllApplications();

        //Test case to create new application and make subscriptions to that application
        SubscriptionRequest subscriptionRequest1 = new SubscriptionRequest(APIName,
                apimContext.getContextTenant().getContextUser().getUserName());
        subscriptionRequest1.setApplicationName("test-application");
        apiStore.subscribe(subscriptionRequest1);
        GenerateAppKeyRequest generateAppKeyRequest1 = new GenerateAppKeyRequest("test-application");
        String responseString1 = apiStore.generateApplicationKey(generateAppKeyRequest1).getData();
        JSONObject response1 = new JSONObject(responseString1);
        String accessToken1 = response1.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders1 = new HashMap<String, String>();
        requestHeaders1.put("Authorization", "Bearer " + accessToken1);
        HttpResponse youTubeResponseTestApp = HttpRequestUtil.doGet(getGatewayServerURLHttp()+"/testAPI/1.0.0/most_popular", requestHeaders1);
        for (int i = 0; i < 40; i++) {
            youTubeResponseTestApp = HttpRequestUtil.doGet(getGatewayServerURLHttp()+"/testAPI/1.0.0/most_popular", requestHeaders1);
        }
        assertEquals(youTubeResponseTestApp.getResponseCode(), 503, "Response code mismatched");

        //Add comment
        apiStore.isCommentActivated();
        apiStore.addComment(APIName, APIVersion, providerName, "this-is-comment");
        apiStore.getRecentlyAddedAPIs("carbon.super", "5");

        apiStore.updateApplication("test-application", "test-updated-application", "test-url", "this-is-updated", "bronze");
        apiStore.getAllApplications();
        apiData = apiStore.removeApplication("test-updated-application").getData();
        assertTrue(apiData.contains("false"), "Error while removing applications");

        apiStore.getAllApplications();
        apiData = apiStore.getAllSubscriptions().getData();
        assertTrue(apiData.contains("test-application"), "Error while getting all the subscriptions");

        apiStore.getAllTags();

        //Update role permissions
        HttpResponse updateTierPermissionResponse = apiPublisher.updatePermissions("Gold", "Deny", "admin");
        JSONObject jsonObject = new JSONObject(updateTierPermissionResponse.getData());
        assertTrue(!(Boolean) jsonObject.get("error"), "Error while updating tier permission");

        apiData = apiStore.removeAPISubscription(APIName, APIVersion, providerName, "1").getData();
        assertTrue(apiData.contains("{\"error\" : false}"), "Error while unsubscribe from API");

        apiPublisher.logout();
        apiStore.removeApplication("APILifeCycleTestAPI-application");

    }

    @Test(groups = {"wso2.am"}, description = "API Life cycle test invalid scenario", dependsOnMethods = "testAPIApplicationLifeCycleITestCase")
    public void testInvalidLoginAsPublisherTestCase() {

        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        //Try invalid login to publisher
        try {
            apiPublisherRestClient.login(apimContext.getContextTenant().getContextUser().getUserName()
                    + "invalid",
                    apimContext.getContextTenant().getContextUser().getPassword());
        } catch (Exception e) {
            assertTrue(e.getMessage().toString().contains(
                    "Please recheck the username and password and try again"),
                    "Invalid user can login to the API publisher");
        }

    }

    @Test(groups = {"wso2.am"}, description = "API Life cycle test subscriber login", enabled = false)
    public void testInvalidLoginAsSubscriberTestCase()
            throws Exception {

        //Try login to publisher with subscriber user
        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        boolean loginFailed = false;
        String error = "";

        if ((userManagementClient != null) &&
                !userManagementClient.userNameExists("Internal/subscriber", "subscriberUser")) {
            userManagementClient.addUser("subscriberUser", "password@123",
                    new String[]{"Internal/subscriber"}, null);
        }

        try {
            apiPublisherRestClient.login("subscriberUser",
                    "password@123");
        } catch (Exception e) {
            loginFailed = true;
            error = e.getMessage().toString();
        }

        Assert.assertTrue(loginFailed && error.contains("Login failed.Insufficient privileges"),
                "Invalid subscriber can login to the API publisher");
    }


    @Test(groups = {"wso2.am"}, description = "API Life cycle test subscriber login", enabled = false)
    public void testInvalidLoginAsTenantSubscriberTestCase()
            throws Exception {

        //Try login to publisher with tenant subscriber user

        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        boolean loginFailed = false;
        String error = "";

        tenantManagementServiceClient.addTenant("wso2.com", "wso2@123", "wso2", "Gold");

        if ((userManagementClient != null) &&
                !userManagementClient.userNameExists("Internal/subscriber", "subscriberUser@wso2.com")) {
            userManagementClient.addUser("subscriberUser@wso2.com", "password@123",
                    new String[]{"Internal/subscriber"}, null);
        }

        try {
            apiPublisherRestClient.login("subscriberUser@wso2.com",
                    "password@123");
        } catch (Exception e) {
            loginFailed = true;
            error = e.getMessage().toString();
        }

        Assert.assertTrue(error.contains("Operation not successful: " +
                "Login failed.Please recheck the username and password and try again"),
                "Invalid tenant subscriber can login to the API publisher");

    }

    @Test(groups = {"wso2.am"}, description = "API visibility", enabled = false)
    public void testAPIVisibilityTestCase()
            throws Exception {

        userManagementClient.addUser("subscriberUser1", "password@123",
                new String[]{"Internal/everyone"}, null);

        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        apiPublisherRestClient.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());

        addPublicAPI(apiPublisherRestClient);
        addVisibleToDomainOnlyAPI(apiPublisherRestClient);
        addVisibleToRolesAPI(apiPublisherRestClient);
        //apiPublisherRestClient.logout();
        boolean bPublishedAPI = false;
        String publishedAPIs;
        /* publishedAPIs = apiStore.getAllPublishedAPIsAsAnonUser().getData();

  if (publishedAPIs.contains("APILifeCycleTestAPIPublic") &&
          !publishedAPIs.contains("APILifeCycleTestAPIDomainOnly") &&
          !publishedAPIs.contains("APILifeCycleTestAPIRoles")) {
      bPublishedAPI = true;
  }

  Assert.assertTrue(bPublishedAPI, "Anonymous user can view other API's");*/

        APIStoreRestClient apiStore1 = new APIStoreRestClient(storeURLHttp);
        apiStore1.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
        publishedAPIs = apiStore1.getAllPublishedAPIs().getData();
        bPublishedAPI = false;

        if (publishedAPIs.contains("APILifeCycleTestAPIPublic") &&
                publishedAPIs.contains("APILifeCycleTestAPIDomainOnly") &&
                publishedAPIs.contains("APILifeCycleTestAPIRoles")) {
            bPublishedAPI = true;
        }

        Assert.assertTrue(bPublishedAPI, "Admin user can not view all API's");

        if ((userManagementClient != null) &&
                !userManagementClient.userNameExists("Internal/subscriber", "subscriberUser")) {
            userManagementClient.addUser("subscriberUser", "password@123",
                    new String[]{"Internal/subscriber"}, null);
        }

        APIStoreRestClient apiStore2 = new APIStoreRestClient(storeURLHttp);
        apiStore2.login("subscriberUser", "password@123");
        publishedAPIs = apiStore2.getAllPublishedAPIs().getData();
        bPublishedAPI = false;

        if (publishedAPIs.contains("APILifeCycleTestAPIPublic") &&
                publishedAPIs.contains("APILifeCycleTestAPIDomainOnly") &&
                !publishedAPIs.contains("APILifeCycleTestAPIRoles")) {
            bPublishedAPI = true;
        }

    }

    @Test(groups = {"wso2.am"}, description = "API visibility", enabled = false)
    public void copyAPILifeCycleTestCase() throws Exception {

        String APIName = "APILifeCycleTestAPI";
        String APIContext = "testAPI";
        String tags = "youtube, video, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";
        String providerName = "admin";
        String APIVersionOld = "1.0.0";
        String APIVersionNew = "2.0.0";
        String defaultVersion = "default_version";

        //add all option methods
        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        apiPublisherRestClient.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());


        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersionOld);
        apiRequest.setWsdl("https://svn.wso2.org/repos/wso2/carbon/platform/trunk/products" +
                "/bps/modules/samples/product/src/main/resources/bpel/2.0/MyRoleMexTestProcess/echo.wsdl");
        apiRequest.setVisibility("restricted");
        apiRequest.setRoles("admin");
        apiPublisherRestClient.addAPI(apiRequest);

        APIBean apiBean = APIMgtTestUtil.getAPIBeanFromHttpResponse(apiPublisherRestClient.getApi(
                APIName, providerName));
        APILifeCycleStateRequest updateRequest1 = new APILifeCycleStateRequest(APIName, providerName,
                APILifeCycleState.PUBLISHED);
        apiPublisherRestClient.changeAPILifeCycleStatusTo(updateRequest1);

        apiPublisherRestClient.copyAPI(providerName, APIName, APIVersionOld, APIVersionNew, "");
        //add assertion
        apiBean = APIMgtTestUtil.getAPIBeanFromHttpResponse(apiPublisherRestClient.getApi(APIName,
                providerName));
        APILifeCycleStateRequest updateRequest2 = new APILifeCycleStateRequest(APIName,
                providerName, APILifeCycleState.PUBLISHED);
        updateRequest2.setVersion(APIVersionNew);
        apiPublisherRestClient.changeAPILifeCycleStatusTo(updateRequest2);

        APIStoreRestClient apiStore = new APIStoreRestClient(storeURLHttp);
        apiStore.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());

        String apiData = apiStore.getAllPublishedAPIs().getData();

        Assert.assertTrue(!apiData.contains(APIVersionOld),
                "Old version available in the store");

        Assert.assertTrue(apiData.contains(APIVersionNew),
                "New version not available in the store");

        //subscribe to the old API version
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName,
                apimContext.getContextTenant().getContextUser().getUserName());
        apiData = apiStore.subscribe(subscriptionRequest).getData();

        Assert.assertTrue(apiData.contains("{\"error\" : false, \"status\" : \"UNBLOCKED\"}"),
                "Can subscribe to the old API version");

        //subscribe to the new API version
        subscriptionRequest.setVersion(APIVersionNew);
        apiData = apiStore.subscribe(subscriptionRequest).getData();

        Assert.assertTrue(apiData.contains("{\"error\" : false, \"status\" : \"UNBLOCKED\"}"),
                "Can not subscribe to the old API version");

        GenerateAppKeyRequest generateAppKeyRequest = new GenerateAppKeyRequest("DefaultApplication");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        //Here add API tags and check same have in response.
        //Here check same tags are there
        //Add some comment to API
        //check comment is there
        //Add rating
        //check rating
        Thread.sleep(60000);

        HttpResponse youTubeResponse = HttpRequestUtil.doGet(getApiInvocationURLHttp("testAPI/1.0.0/most_popular"), requestHeaders);
        Assert.assertEquals(youTubeResponse.getResponseCode(), 200, "Response code mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<feed"), "Response data mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<category"), "Response data mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<entry>"), "Response data mismatched");

        youTubeResponse = HttpRequestUtil.doGet(getApiInvocationURLHttp("testAPI/2.0.0/most_popular"), requestHeaders);
        Assert.assertEquals(youTubeResponse.getResponseCode(), 200, "Response code mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<feed"), "Response data mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<category"), "Response data mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<entry>"), "Response data mismatched");

    }

    @Test(groups = {"wso2.am"}, description = "API visibility", enabled = false)
    public void otherAPILifeCycleStatesTestCase() throws Exception {

        String APIName = "APILifeCycleTestAPI";
        String APIContext = "testAPI";
        String tags = "youtube, video, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";
        String providerName = "admin";
        String APIVersionOld = "1.0.0";
        String APIVersionNew = "2.0.0";
        String defaultVersion = "default_version";

        //add all option methods
        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        apiPublisherRestClient.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersionOld);
        apiRequest.setWsdl("https://svn.wso2.org/repos/wso2/carbon/platform/trunk/products" +
                "/bps/modules/samples/product/src/main/resources/bpel/2.0/MyRoleMexTestProcess/echo.wsdl");
        apiRequest.setVisibility("restricted");
        apiRequest.setRoles("admin");
        apiPublisherRestClient.addAPI(apiRequest);

        APIBean apiBean = APIMgtTestUtil.getAPIBeanFromHttpResponse(apiPublisherRestClient.getApi(
                APIName, providerName));
        APILifeCycleStateRequest updateRequest1 = new APILifeCycleStateRequest(APIName, providerName,
                APILifeCycleState.PUBLISHED);
        apiPublisherRestClient.changeAPILifeCycleStatusTo(updateRequest1);

        APIStoreRestClient apiStore = new APIStoreRestClient(storeURLHttp);
        apiStore.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());

        String apiData = apiStore.getAllPublishedAPIs().getData();

        Assert.assertTrue(apiData.contains(APIName),
                "Added API not available in store");

        APILifeCycleStateRequest updateRequest2 = new APILifeCycleStateRequest(APIName, providerName,
                APILifeCycleState.RETIRED);
        apiPublisherRestClient.changeAPILifeCycleStatusTo(updateRequest2);

        apiData = apiStore.getAllPublishedAPIs().getData();

        Assert.assertTrue(!apiData.contains(APIName),
                "Retired API available in store");


        APILifeCycleStateRequest updateRequest3 = new APILifeCycleStateRequest(APIName, providerName,
                APILifeCycleState.BLOCKED);
        apiPublisherRestClient.changeAPILifeCycleStatusTo(updateRequest3);

        apiData = apiStore.getAllPublishedAPIs().getData();

        Assert.assertTrue(!apiData.contains(APIName),
                "Blocked API available in store");

    }

    @Test(groups = {"wso2.am"}, description = "API visibility", enabled = false)
    public void copyAndDepricateAPILifeCycleTestCase() throws Exception {

        String APIName = "APILifeCycleTestAPI";
        String APIContext = "testAPI";
        String tags = "youtube, video, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";
        String providerName = "admin";
        String APIVersionOld = "1.0.0";
        String APIVersionNew = "2.0.0";
        String defaultVersion = "default_version";

        //add all option methods
        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        apiPublisherRestClient.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());


        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersionOld);
        apiRequest.setWsdl("https://svn.wso2.org/repos/wso2/carbon/platform/trunk/products" +
                "/bps/modules/samples/product/src/main/resources/bpel/2.0/MyRoleMexTestProcess/echo.wsdl");
        apiRequest.setVisibility("restricted");
        apiRequest.setRoles("admin");
        apiPublisherRestClient.addAPI(apiRequest);

        //publish initial version
        APIBean apiBean = APIMgtTestUtil.getAPIBeanFromHttpResponse(apiPublisherRestClient.getApi(
                APIName, providerName));
        APILifeCycleStateRequest updateRequest1 = new APILifeCycleStateRequest(APIName, providerName,
                APILifeCycleState.PUBLISHED);
        apiPublisherRestClient.changeAPILifeCycleStatusTo(updateRequest1);

        //publish new version
        apiPublisherRestClient.copyAPI(providerName, APIName, APIVersionOld, APIVersionNew, "");
        //add assertion
        apiBean = APIMgtTestUtil.getAPIBeanFromHttpResponse(apiPublisherRestClient.getApi(APIName,
                providerName));
        APILifeCycleStateRequest updateRequest2 = new APILifeCycleStateRequest(APIName,
                providerName, APILifeCycleState.PUBLISHED);
        updateRequest2.setVersion(APIVersionNew);
        apiPublisherRestClient.changeAPILifeCycleStatusTo(updateRequest2);

        //deprecate old version
        apiBean = APIMgtTestUtil.getAPIBeanFromHttpResponse(apiPublisherRestClient.getApi(APIName,
                providerName));
        APILifeCycleStateRequest updateRequest3 = new APILifeCycleStateRequest(APIName,
                providerName, APILifeCycleState.DEPRECATED);
        updateRequest3.setVersion(APIVersionOld);
        apiPublisherRestClient.changeAPILifeCycleStatusTo(updateRequest3);

        APIStoreRestClient apiStore = new APIStoreRestClient(storeURLHttp);
        apiStore.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());

        String apiData = apiStore.getAllPublishedAPIs().getData();

        Assert.assertTrue(!apiData.contains(APIVersionOld),
                "Old version available in the store");

        Assert.assertTrue(apiData.contains(APIVersionNew),
                "New version not available in the store");

        //subscribe to the old API version
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName,
                apimContext.getContextTenant().getContextUser().getUserName());
        apiData = apiStore.subscribe(subscriptionRequest).getData();

        Assert.assertTrue(apiData.contains("{\"error\" : false, \"status\" : \"UNBLOCKED\"}"),
                "Can subscribe to the old API version");

        //subscribe to the new API version
        subscriptionRequest.setVersion(APIVersionNew);
        apiData = apiStore.subscribe(subscriptionRequest).getData();

        Assert.assertTrue(apiData.contains("{\"error\" : false, \"status\" : \"UNBLOCKED\"}"),
                "Can not subscribe to the old API version");

        GenerateAppKeyRequest generateAppKeyRequest = new GenerateAppKeyRequest("DefaultApplication");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        //Here add API tags and check same have in response.
        //Here check same tags are there
        //Add some comment to API
        //check comment is there
        //Add rating
        //check rating
        Thread.sleep(60000);

        HttpResponse youTubeResponse = HttpRequestUtil.doGet(getApiInvocationURLHttp("testAPI/1.0.0/most_popular"), requestHeaders);
        Assert.assertEquals(youTubeResponse.getResponseCode(), 200, "Response code mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<feed"), "Response data mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<category"), "Response data mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<entry>"), "Response data mismatched");

        youTubeResponse = HttpRequestUtil.doGet(getApiInvocationURLHttp("testAPI/2.0.0/most_popular"), requestHeaders);
        Assert.assertEquals(youTubeResponse.getResponseCode(), 200, "Response code mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<feed"), "Response data mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<category"), "Response data mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<entry>"), "Response data mismatched");

    }

    public void addPublicAPI(APIPublisherRestClient apiPublisherRestClient) throws Exception {

        String APIName = "APILifeCycleTestAPIPublic";
        String APIContext = "testAPIPublic";
        String tags = "youtube, video, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";
        String providerName = "admin";
        String APIVersion = "1.0.0";
        //add all option methods

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setWsdl("https://svn.wso2.org/repos/wso2/carbon/platform/trunk/products" +
                "/bps/modules/samples/product/src/main/resources/bpel/2.0/MyRoleMexTestProcess/echo.wsdl");
        apiRequest.setVisibility("public");
        apiPublisherRestClient.addAPI(apiRequest);
        //add assertion
        APIBean apiBean = APIMgtTestUtil.getAPIBeanFromHttpResponse(apiPublisherRestClient.getApi(APIName, providerName));
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(APIName, providerName, APILifeCycleState.PUBLISHED);
        apiPublisherRestClient.changeAPILifeCycleStatusTo(updateRequest);

    }

    public void addVisibleToDomainOnlyAPI(APIPublisherRestClient apiPublisherRestClient) throws Exception {

        String APIName = "APILifeCycleTestAPIDomainOnly";
        String APIContext = "testAPIDomainOnly";
        String tags = "youtube, video, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";
        String providerName = "admin";
        String APIVersion = "1.0.0";
        //add all option methods
        //apiPublisher.login(userInfo.getUserName(), userInfo.getPassword());
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setWsdl("https://svn.wso2.org/repos/wso2/carbon/platform/trunk/products/" +
                "bps/modules/samples/product/src/main/resources/bpel/2.0/MyRoleMex" +
                "TestProcess/echo.wsdl");
        apiRequest.setVisibility("private");
        apiPublisherRestClient.addAPI(apiRequest);
        //add assertion;
        APIBean apiBean = APIMgtTestUtil.getAPIBeanFromHttpResponse(apiPublisherRestClient.getApi(
                APIName, providerName));
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(APIName, providerName,
                APILifeCycleState.PUBLISHED);
        apiPublisherRestClient.changeAPILifeCycleStatusTo(updateRequest);
    }

    public void addVisibleToRolesAPI(APIPublisherRestClient apiPublisherRestClient) throws Exception {

        String APIName = "APILifeCycleTestAPIRoles";
        String APIContext = "testAPIRoles";
        String tags = "youtube, video, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";
        String providerName = "admin";
        String APIVersion = "1.0.0";
        //add all option methods
        //apiPublisher.login(userInfo.getUserName(), userInfo.getPassword());
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setWsdl("https://svn.wso2.org/repos/wso2/carbon/platform/trunk" +
                "/products/bps/modules/samples/product/src/main/resources/bpel" +
                "/2.0/MyRoleMexTestProcess/echo.wsdl");
        apiRequest.setVisibility("restricted");
        apiRequest.setRoles("admin");
        apiPublisherRestClient.addAPI(apiRequest);
        //add assertion
        APIBean apiBean = APIMgtTestUtil.getAPIBeanFromHttpResponse(apiPublisherRestClient.getApi(
                APIName, providerName));
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(
                APIName, providerName, APILifeCycleState.PUBLISHED);
        apiPublisherRestClient.changeAPILifeCycleStatusTo(updateRequest);
    }


    @Test(groups = {"wso2.am"}, description = "API Life cycle application related tests in store", enabled = false)
    public void testApplicationsInStoreTestCase()
            throws Exception {

        //Try login to publisher with tenant subscriber user

        APIStoreRestClient apiStoreRestClient1 = new APIStoreRestClient(storeURLHttp);

        apiStoreRestClient1.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());

        tenantManagementServiceClient.addTenant("wso2.com", "wso2@123", "wso2", "Gold");

        if ((userManagementClient != null) &&
                !userManagementClient.userNameExists("Internal/subscriber", "subscriberUser@wso2.com")) {
            userManagementClient.addUser("subscriberUser@wso2.com", "password@123",
                    new String[]{"Internal/subscriber"}, null);
        }

        apiStoreRestClient1.addApplication("carbonSuperApp", "Gold", "", "super tenant app");
        String apiData = apiStoreRestClient1.addApplication("carbonSuperApp", "Gold", "",
                "super tenant app").getData();
        Assert.assertTrue(apiData.contains("A duplicate application already exists"),
                "application with duplicate name addition allowed");

        APIStoreRestClient apiStoreRestClient2 = new APIStoreRestClient(storeURLHttp);

        apiStoreRestClient2.login("admin@wso2.com",
                "wso2@123");
        apiData = apiStoreRestClient2.addApplication(
                "carbonSuperApp", "Gold", "", "super tenant app").getData();
        Assert.assertTrue(apiData.contains("{\"error\" : false}"),
                "application with same name addition not allowed in other tenant");

        apiStoreRestClient1.addApplication("carbonSuperApp", "Gold", "", "super tenant app");
        apiData = apiStoreRestClient1.removeApplication("carbonSuperApp").getData();
        Assert.assertTrue(apiData.contains("{\"error\" : false}"),
                "application deletion failed");


    }

    @Test(groups = {"wso2.am"}, description = "API Life cycle login to store")
    public void testLoginToStoreTestCase()
            throws Exception {

        //Try login to publisher with tenant subscriber user
        String APICreatorRole = "APICreatorRole";
        String APIPublisherRole = "APIPublisherRole";
        String APIPublisherUser = "APIPublisherUser";
        String APICreatorUser = "APICreatorUser";
        String password = "password@123";
        boolean loginFailed = false;
        String errorString = "";

        APIStoreRestClient apiStoreRestClient = new APIStoreRestClient(storeURLHttp);

        String[] createPermissions = {
                "/permission/admin/login",
                "/permission/admin/manage/api/create"};

        if (!userManagementClient.roleNameExists(APICreatorRole)) {
            userManagementClient.addRole(APICreatorRole, null, createPermissions);
        }

        if ((userManagementClient != null) &&
                !userManagementClient.userNameExists(APICreatorRole, APICreatorUser)) {
            userManagementClient.addUser(APICreatorUser, password,
                    new String[]{APICreatorRole}, null);
        }

        String[] publishPermissions = {
                "/permission/admin/login",
                "/permission/admin/manage/api/publish"};

        if (!userManagementClient.roleNameExists(APIPublisherRole)) {
            userManagementClient.addRole(APIPublisherRole, null, publishPermissions);
        }

        if ((userManagementClient != null) &&
                !userManagementClient.userNameExists(APIPublisherRole, APIPublisherUser)) {
            userManagementClient.addUser(APIPublisherUser, password,
                    new String[]{APIPublisherRole}, null);
        }

        try {
            apiStoreRestClient.login("invaliduser", "invaliduser@123");
        } catch (Exception e) {
            loginFailed = true;
            errorString = e.getMessage().toString();
        }

        Assert.assertTrue(loginFailed && errorString.contains("Operation not successful: " +
                "Login failed.Please recheck the username and password and try again"),
                "Invalid user can login to the API store");
        loginFailed = false;

        try {
            apiStoreRestClient.login(APICreatorUser, password);
        } catch (Exception e) {
            loginFailed = true;
            errorString = e.getMessage().toString();
        }

        Assert.assertTrue(loginFailed && errorString.contains("Login failed.Insufficient Privileges"),
                "API creator can login to the API store");
        loginFailed = false;

        try {
            apiStoreRestClient.login(APIPublisherUser, password);
        } catch (Exception e) {
            loginFailed = true;
            errorString = e.getMessage().toString();
        }

        Assert.assertTrue(loginFailed && errorString.contains("Login failed.Insufficient Privileges"),
                "API publisher can login to the API store");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        apiPublisherRestClient.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());

        /*apiPublisherRestClient.deleteApi("APILifeCycleTestAPIPublic", "1.0.0", "admin");
        apiPublisherRestClient.deleteApi("APILifeCycleTestAPIDomainOnly", "1.0.0", "admin");
        apiPublisherRestClient.deleteApi("APILifeCycleTestAPIRoles", "1.0.0", "admin");*/

        Thread.sleep(5000);
        super.cleanup();
    }
}