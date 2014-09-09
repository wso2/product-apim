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

package org.wso2.carbon.am.tests.sample;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTest;
import org.wso2.am.integration.test.utils.APIMgtTestUtil;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.TenantManagementServiceClient;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class APIApplicationLifeCycleTestCase extends APIManagerIntegrationTest {
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

        if (isBuilderEnabled()) {
            publisherURLHttp = getServerURLHttp();
            storeURLHttp = getServerURLHttp();

        } else {
            publisherURLHttp = getPublisherServerURLHttp();
            storeURLHttp = getStoreServerURLHttp();
        }
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);

        userManagementClient = new UserManagementClient(
                context.getContextUrls().getBackEndUrl(), getSessionCookie());

        tenantManagementServiceClient = new TenantManagementServiceClient(
                context.getContextUrls().getBackEndUrl(), getSessionCookie());

    }

    @Test(groups = {"wso2.am"}, description = "API Life cycle test case", enabled = false)
    public void testAPIApplicationLifeCycleITestCase() throws Exception {
        String APIName = "APILifeCycleTestAPI";
        String APIContext = "testAPI";
        String tags = "youtube, video, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";
        String providerName = "admin";
        String APIVersion = "1.0.0";
        //add all option methods
        apiPublisher.login(context.getContextTenant().getContextUser().getUserName(),
                context.getContextTenant().getContextUser().getPassword());
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
        Assert.assertEquals(apiBean.getId().getApiName(), APIName, "API Name mismatch");
        Assert.assertEquals(apiBean.getContext().trim().substring(apiBean.getContext().indexOf("/") + 1), APIContext, "API context mismatch");
        Assert.assertEquals(apiBean.getId().getVersion(), APIVersion, "API version mismatch");
        Assert.assertEquals(apiBean.getId().getProviderName(), providerName, "Provider Name mismatch");
        for (String tag : apiBean.getTags()) {
            Assert.assertTrue(tags.contains(tag), "API tag data mismatched");
        }
        Assert.assertEquals(apiBean.getDescription(), description, "API description mismatch");
        apiStore.login(context.getContextTenant().getContextUser().getUserName(),
                context.getContextTenant().getContextUser().getPassword());
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName,
                context.getContextTenant().getContextUser().getUserName());
        apiStore.subscribe(subscriptionRequest);

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
        //  for (int i = 0; i < 19; i++) {

        HttpResponse youTubeResponse = HttpRequestUtil.doGet(getApiInvocationURLHttp("testAPI/1.0.0/most_popular"), requestHeaders);
        Assert.assertEquals(youTubeResponse.getResponseCode(), 200, "Response code mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<feed"), "Response data mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<category"), "Response data mismatched");
        Assert.assertTrue(youTubeResponse.getData().contains("<entry>"), "Response data mismatched");

        // }
        //Do get,post,put,delete all here
        //HttpResponse youTubeResponse = HttpRequestUtil.doGet(getApiInvocationURLHttp("commentRating/1.0.0/most_popular"), requestHeaders);
        //Assert.assertEquals(youTubeResponse.getResponseCode(), 503, "Response code mismatched");
        //Thread.sleep(60000);
        HttpResponse youTubeResponse1 = HttpRequestUtil.doGet(getApiInvocationURLHttp("testAPI/1.0.0/most_popular"), null);
        Assert.assertEquals(youTubeResponse1.getResponseCode(), 401, "Response code mismatched");
        requestHeaders.clear();
        requestHeaders.put("Authorization", "Bearer " + "-wrong-tokent-text-");
        HttpResponse youTubeResponseError = HttpRequestUtil.doGet(getApiInvocationURLHttp("testAPI/1.0.0/most_popular"), null);
        Assert.assertEquals(youTubeResponseError.getResponseCode(), 401, "Response code mismatched");

        apiStore.getAllPublishedAPIs();
        apiStore.getAllApplications();
        apiStore.getPublishedAPIsByApplication("DefaultApplication");
        apiStore.isRatingActivated();
        apiStore.addRatingToAPI(APIName, APIVersion, providerName, "4");
        apiStore.addRatingToAPI(APIName, APIVersion, providerName, "2");
        apiStore.addRatingToAPI(APIName, APIVersion, providerName, "1");
        apiStore.removeRatingFromAPI(APIName, APIVersion, providerName);
        apiStore.getAllDocumentationOfApi(APIName, APIVersion, providerName);
        //apiStore.getAllPaginatedPublishedAPIs("carbon.super","0","10");
        //Negative cases
        apiStore.getPublishedAPIsByApplication("DefaultApplicationWrong");
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
        apiPublisher.addDocument(APIName, APIVersion, providerName, "Doc Name", "How To", "URL",
                "http://www.businesstoday.lk/article.php?article=3549", "summary", "");
        apiPublisher.addDocument(APIName, APIVersion, providerName, "Doc Name", "How To", " File",
                "url-no-need", "summary",
                getAMResourceLocation() + File.separator + "configFiles/tokenTest/" + "api-manager.xml");
        apiPublisher.removeDocumentation(APIName, APIVersion, providerName, "Doc Name", "How To");
        //create application
        apiStore.addApplication("test-application", "Gold", "", "this-is-test");
        apiStore.getAllApplications();

        //Test case to create new application and make subscriptions to that application
        SubscriptionRequest subscriptionRequest1 = new SubscriptionRequest(APIName,
                context.getContextTenant().getContextUser().getUserName());
        subscriptionRequest1.setApplicationName("test-application");
        apiStore.subscribe(subscriptionRequest1);
        GenerateAppKeyRequest generateAppKeyRequest1 = new GenerateAppKeyRequest("test-application");
        String responseString1 = apiStore.generateApplicationKey(generateAppKeyRequest1).getData();
        JSONObject response1 = new JSONObject(responseString1);
        String accessToken1 = response1.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders1 = new HashMap<String, String>();
        requestHeaders1.put("Authorization", "Bearer " + accessToken1);
        HttpResponse youTubeResponseTestApp = HttpRequestUtil.doGet(getApiInvocationURLHttp("testAPI/1.0.0/most_popular"), requestHeaders1);
        for (int i = 0; i < 40; i++) {
            youTubeResponseTestApp = HttpRequestUtil.doGet(getApiInvocationURLHttp("testAPI/1.0.0/most_popular"), requestHeaders1);
        }
        Assert.assertEquals(youTubeResponseTestApp.getResponseCode(), 503, "Response code mismatched");

        //Add comment
        apiStore.isCommentActivated();
        apiStore.addComment(APIName, APIVersion, providerName, "this-is-comment");
        apiStore.getRecentlyAddedAPIs("carbon.super", "5");


        apiStore.updateApplication("test-application", "test-updated-application", "test-url", "this-is-updated", "bronze");
        apiStore.getAllApplications();
        apiStore.removeApplication("test-updated-application");
        apiStore.getAllSubscriptions();
        apiStore.getAllTags();

        //Update role permissions
        HttpResponse updateTierPermissionResponse = apiPublisher.updatePermissions("Gold", "Deny", "admin");
        JSONObject jsonObject = new JSONObject(updateTierPermissionResponse.getData());
        Assert.assertTrue(!(Boolean) jsonObject.get("error"), "Error while updating tier permission");

        apiPublisher.logout();

    }

    @Test(groups = {"wso2.am"}, description = "API Life cycle test invalid scenario")
    public void testInvalidLoginAsPublisherTestCase()  {

        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        //Try invalid login to publisher
        try {
            apiPublisherRestClient.login(context.getContextTenant().getContextUser().getUserName()
                    + "invalid",
                    context.getContextTenant().getContextUser().getPassword());
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().toString().contains(
                    "Please recheck the username and password and try again"),
                    "Invalid user can login to the API publisher");
        }

    }

    @Test(groups = {"wso2.am"}, description = "API Life cycle test subscriber login")
    public void testInvalidLoginAsSubscriberTestCase()
            throws Exception {

        //Try login to publisher with subscriber user
        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);

        if ((userManagementClient != null) &&
                !userManagementClient.userNameExists("Internal/subscriber", "subscriberUser")) {
            userManagementClient.addUser("subscriberUser", "password@123",
                    new String[]{"Internal/subscriber"}, null);
        }

        try {
            apiPublisherRestClient.login("subscriberUser",
                    "password@123");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().toString().contains("Login failed.Insufficient privileges"),
                    "Invalid subscriber can login to the API publisher");
        }
    }


    @Test(groups = {"wso2.am"}, description = "API Life cycle test subscriber login")
    public void testInvalidLoginAsTenantSubscriberTestCase()
            throws Exception {

        //Try login to publisher with tenant subscriber user

        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);

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
            Assert.assertTrue(e.getMessage().toString().contains("Operation not successful: " +
                    "Login failed.Please recheck the username and password and try again"),
                    "Invalid tenant subscriber can login to the API publisher");
        }

    }

    @Test(groups = {"wso2.am"}, description = "API visibility")
    public void testAPIVisibilityTestCase()
            throws Exception {

        userManagementClient.addUser("subscriberUser1", "password@123",
                new String[]{"Internal/everyone"}, null);

        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        apiPublisherRestClient.login(context.getContextTenant().getContextUser().getUserName(),
                context.getContextTenant().getContextUser().getPassword());

        addPublicAPI(apiPublisherRestClient);
        addVisibleToDomainOnlyAPI(apiPublisherRestClient);
        addVisibleToRolesAPI(apiPublisherRestClient);

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
        apiStore1.login(context.getContextTenant().getContextUser().getUserName(),
                context.getContextTenant().getContextUser().getPassword());
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
        apiStore2.login("subscriberUser","password@123");
        publishedAPIs = apiStore2.getAllPublishedAPIs().getData();
        bPublishedAPI = false;

        if (publishedAPIs.contains("APILifeCycleTestAPIPublic") &&
                publishedAPIs.contains("APILifeCycleTestAPIDomainOnly") &&
                !publishedAPIs.contains("APILifeCycleTestAPIRoles")) {
            bPublishedAPI = true;
        }

        apiPublisherRestClient.logout();
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


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        apiPublisherRestClient.login(context.getContextTenant().getContextUser().getUserName(),
                context.getContextTenant().getContextUser().getPassword());

        apiPublisherRestClient.deleteApi("APILifeCycleTestAPIPublic", "1.0.0", "admin");
        apiPublisherRestClient.deleteApi("APILifeCycleTestAPIDomainOnly", "1.0.0", "admin");
        apiPublisherRestClient.deleteApi("APILifeCycleTestAPIRoles", "1.0.0", "admin");

        Thread.sleep(5000);
        super.cleanup();
    }
}
