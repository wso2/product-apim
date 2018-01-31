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

package org.wso2.am.integration.tests.other;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class APIApplicationLifeCycleTestCase extends APIMIntegrationBaseTest {

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws APIManagerIntegrationTestException, IOException,
            XPathExpressionException, URISyntaxException, SAXException, XMLStreamException,
            LoginAuthenticationExceptionException {
        super.init();
    }

    @Test(groups = {"wso2.am"}, description = "API Life cycle test case")
    public void testAPIApplicationLifeCycleITestCase() throws Exception {
        String apiData = "";
        String APIName = "APILifeCycleTestAPI";
        String APIContext = "testAPI";
        String tags = "testTag1, testTag2, testTag3";
        String APIEndpointMethod = "/customers/123";
        String url = "jaxrs_basic/services/customers/customerservice/";
        String description = "This is test API create by API manager integration test";
        String providerName = "admin";
        String APIVersion = "1.0.0";
        String applicationName = "APILifeCycleTestAPI-application";
        //This is because with the new context version strategy, if the context does not have the {version} param ,
        //then we add the {version} param to the end of the context.
        String apiContextAddedValue = APIContext + "/" + APIVersion;

        //Add all option methods
        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(gatewayUrlsWrk.getWebAppURLHttp() + url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setVisibility("restricted");
        apiRequest.setRoles("admin");
        apiPublisher.addAPI(apiRequest);
        apiPublisher.deleteAPI(APIName, APIVersion, providerName);
        //Add assertion
        apiPublisher.addAPI(apiRequest);
        APIBean apiBean = APIMTestCaseUtils.getAPIBeanFromHttpResponse(apiPublisher.getAPI(
                APIName, providerName));
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(APIName, providerName,
                APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);
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
        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());
        HttpResponse addApplicationResponse = apiStore.addApplication(applicationName, "10PerMin", "", "this-is-test");
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName,
                storeContext.getContextTenant().getContextUser().getUserName());
        subscriptionRequest.setApplicationName(applicationName);
        HttpResponse subscribeResponse = apiStore.subscribe(subscriptionRequest);

        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(applicationName);
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "application/json");
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                                 APIMIntegrationConstants.IS_API_EXISTS);
        HttpResponse apiResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp( APIContext, APIVersion) + APIEndpointMethod, requestHeaders);
        assertEquals(apiResponse.getResponseCode(), 200, "Response code mismatched");
        requestHeaders.clear();
        requestHeaders.put("Authorization", "Bearer " + "-wrong-tokent-text-");
        HttpResponse apiResponseError = HttpRequestUtil.doGet(getAPIInvocationURLHttp( APIContext, APIVersion) + APIEndpointMethod, requestHeaders);
        assertEquals(apiResponseError.getResponseCode(), 401, "Response code mismatched");

        apiStore.getAllPublishedAPIs();
        apiStore.getAllApplications();
        apiStore.getPublishedAPIsByApplication(applicationName);
        apiStore.isRatingActivated();
        apiStore.addRatingToAPI(APIName, APIVersion, providerName, "4");
        apiStore.addRatingToAPI(APIName, APIVersion, providerName, "2");
        apiStore.addRatingToAPI(APIName, APIVersion, providerName, "1");
        apiStore.removeRatingFromAPI(APIName, APIVersion, providerName);
        apiStore.getAllDocumentationOfAPI(APIName, APIVersion, providerName);
        //Negative cases
        apiStore.getPublishedAPIsByApplication("APILifeCycleTestAPI-application-wrong");
        apiStore.isRatingActivated();
        apiStore.addRatingToAPI("NoAPI", APIVersion, providerName, "4");
        apiStore.removeRatingFromAPI("NoAPI", APIVersion, providerName);
        apiStore.getAllDocumentationOfAPI("NoAPI", APIVersion, providerName);
        apiRequest.setTags("updated");
        apiRequest.setProvider("admin");
        Thread.sleep(1000);
        apiPublisher.updateAPI(apiRequest);
        waitForAPIDeployment();
        //TODO need to reformat this code after we finalize new APIs
        apiPublisher.addDocument(APIName, APIVersion, providerName, "Doc Name", "How To", "In-line",
                "url-no-need", "summary", "","","");
        apiPublisher.addDocument(APIName, APIVersion, providerName, "Doc Name1", "How To", "URL",
                "http://www.businesstoday.lk/article.php?article=3549", "summary", "","",null);
        apiPublisher.addDocument(APIName, APIVersion, providerName, "Doc Name2", "How To", " File",
                "url-no-need", "summary",
                getAMResourceLocation() + File.separator + "configFiles/tokenTest/" + "api-manager.xml","","");
        apiPublisher.removeDocumentation(APIName, APIVersion, providerName, "Doc Name", "How To");
        //Create application
        apiStore.addApplication("test-application", "20PerMin", "", "this-is-test");
        apiStore.addApplication("test-application2", "20PerMin", "", "this-is-test");
        apiStore.getAllApplications();

        //Test case to create new application and make subscriptions to that application
        SubscriptionRequest subscriptionRequest1 = new SubscriptionRequest(APIName,
                storeContext.getContextTenant().getContextUser().getUserName());
        subscriptionRequest1.setApplicationName("test-application");
        apiStore.subscribe(subscriptionRequest1);
        APPKeyRequestGenerator generateAppKeyRequest1 = new APPKeyRequestGenerator("test-application");
        String responseString1 = apiStore.generateApplicationKey(generateAppKeyRequest1).getData();
        JSONObject response1 = new JSONObject(responseString1);
        String accessToken1 = response1.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders1 = new HashMap<String, String>();
        requestHeaders1.put("Authorization", "Bearer " + accessToken1);
        requestHeaders1.put("accept", "application/json");
        HttpResponse apiResponseTestApp = HttpRequestUtil.doGet(getAPIInvocationURLHttp( APIContext, APIVersion) + APIEndpointMethod, requestHeaders1);
        for (int i = 0; i < 15; i++) {
            apiResponseTestApp = HttpRequestUtil.doGet(getAPIInvocationURLHttp( APIContext, APIVersion) + APIEndpointMethod, requestHeaders1);
        }
        assertEquals(apiResponseTestApp.getResponseCode(), 200, "Response code mismatched");

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

        HttpResponse removeSubscriptionnResponse = apiStore.removeAPISubscription(APIName, APIVersion, providerName, "1");
        apiData = removeSubscriptionnResponse.getData();
        assertTrue(apiData.contains("error"), "Error while unsubscribe from API");

        apiPublisher.logout();
        apiStore.removeApplication(applicationName);
    }

    @Test(groups = {"wso2.am"}, description = "API Life cycle test invalid scenario", dependsOnMethods = "testAPIApplicationLifeCycleITestCase")
    public void testInvalidLoginAsPublisherTestCase() {
        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        boolean loginFailed = false;
        String loginResponseString = "";
        //Try invalid login to publisher
        try {
            HttpResponse invalidLoginResponse = apiPublisherRestClient.login(publisherContext.getContextTenant().getContextUser().getUserName()
                    + "invalid",
                    publisherContext.getContextTenant().getContextUser().getPassword());
            loginResponseString = invalidLoginResponse.getData();
            JSONObject response = new JSONObject(loginResponseString);
            String isLoginError = response.get("error").toString();
            if (isLoginError.equals("true")) {
                loginFailed = true;
            }
        } catch (Exception e) {
            loginFailed = true;
        }
        Assert.assertTrue(loginFailed && loginResponseString.contains("Login failed. Please recheck the username and password and try again.."),
                "Invalid user can login to the API publisher");
    }

    @Test(groups = {"wso2.am"}, description = "API Life cycle test subscriber login")
    public void testInvalidLoginAsSubscriberTestCase()
            throws Exception {
        //Try login to publisher with subscriber user
        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        boolean loginFailed = false;
        String loginResponseString = "";

        if ((userManagementClient != null) &&
                !userManagementClient.userNameExists("Internal/subscriber", "subscriberUser")) {
            userManagementClient.addUser("subscriberUser", "password@123",
                    new String[]{"Internal/subscriber"}, null);
        }

        try {
            HttpResponse loginResponse = apiPublisherRestClient.login("subscriberUser",
                    "password@123");
            loginResponseString = loginResponse.getData();
            JSONObject response = new JSONObject(loginResponseString);
            String isLoginError = response.get("error").toString();
            if (isLoginError.equals("true")) {
                loginFailed = true;
            }
        } catch (Exception e) {
            loginFailed = true;
        }
        Assert.assertTrue(loginFailed && loginResponseString.contains("Login failed. Insufficient privileges."),
                "Invalid subscriber can login to the API publisher");
    }

    @Test(groups = {"wso2.am"}, description = "API Life cycle test subscriber login")
    public void testInvalidLoginAsTenantSubscriberTestCase()
            throws Exception {
        //Try login to publisher with tenant subscriber user
        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        boolean loginFailed = false;
        String loginResponseString = "";

        tenantManagementServiceClient.addTenant("wso2.com", "wso2@123", "wso2", "demo");
        if ((userManagementClient != null) &&
                !userManagementClient.userNameExists("Internal/subscriber", "subscriberUser@wso2.com")) {
            userManagementClient.addUser("subscriberUser@wso2.com", "password@123",
                    new String[]{"Internal/subscriber"}, null);
        }

        try {
            HttpResponse loginResponse = apiPublisherRestClient.login("subscriberUser@wso2.com",
                    "password@123");
            loginResponseString = loginResponse.getData();
            JSONObject response = new JSONObject(loginResponseString);
            String isLoginError = response.get("error").toString();
            if (isLoginError.equals("true")) {
                loginFailed = true;
            }
        } catch (Exception e) {
            loginFailed = true;
        }
        Assert.assertTrue(loginFailed && loginResponseString.contains("Login failed. Insufficient privileges."),
                "Invalid tenant subscriber can login to the API publisher");
    }

    @Test(groups = {"wso2.am"}, description = "API visibility")
    public void testAPIVisibilityTestCase()
            throws Exception {
        userManagementClient.addUser("subscriberUser1", "password@123",
                new String[]{"Internal/everyone"}, null);

        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        apiPublisherRestClient.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        addPublicAPI(apiPublisherRestClient);
        addVisibleToDomainOnlyAPI(apiPublisherRestClient);
        addVisibleToRolesAPI(apiPublisherRestClient);
        boolean bPublishedAPI = false;
        String publishedAPIs;
        APIStoreRestClient apiStore1 = new APIStoreRestClient(storeURLHttp);
        apiStore1.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());
        publishedAPIs = apiStore1.getAllPublishedAPIs().getData();
        bPublishedAPI = false;

        if (publishedAPIs.contains("APILifeCycleTestAPIPublic") &&
                publishedAPIs.contains("APILifeCycleTestAPIDomainOnly") &&
                publishedAPIs.contains("APILifeCycleTestAPIRoles")) {
            bPublishedAPI = true;
        }

        Assert.assertTrue(bPublishedAPI, "Admin user cannot view all API's");

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

    @Test(groups = {"wso2.am"}, description = "API visibility")
    public void copyAPILifeCycleTestCase() throws Exception {
        String APIName = "APILifeCycleTestAPI";
        String APIContext = "testAPI";
        String tags = "testTag1, testTag2, testTag3";
        String APIEndpointMethod = "/customers/123";
        String url = "jaxrs_basic/services/customers/customerservice/";
        String description = "This is test API create by API manager integration test";
        String providerName = "admin";
        String APIVersionOld = "1.0.0";
        String APIVersionNew = "2.0.0";
        String defaultVersion = "default_version";

        //add all option methods
        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        apiPublisherRestClient.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(gatewayUrlsWrk.getWebAppURLHttp() + url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersionOld);
        apiRequest.setVisibility("restricted");
        apiRequest.setRoles("admin");
        apiPublisherRestClient.addAPI(apiRequest);

        APIBean apiBean = APIMTestCaseUtils.getAPIBeanFromHttpResponse(apiPublisherRestClient.getAPI(
                APIName, providerName));
        APILifeCycleStateRequest updateRequest1 = new APILifeCycleStateRequest(APIName, providerName,
                APILifeCycleState.PUBLISHED);
        apiPublisherRestClient.changeAPILifeCycleStatus(updateRequest1);

        apiPublisherRestClient.copyAPI(providerName, APIName, APIVersionOld, APIVersionNew, "");
        //add assertion
        apiBean = APIMTestCaseUtils.getAPIBeanFromHttpResponse(apiPublisherRestClient.getAPI(APIName,
                providerName));
        APILifeCycleStateRequest updateRequest2 = new APILifeCycleStateRequest(APIName,
                providerName, APILifeCycleState.PUBLISHED);
        updateRequest2.setVersion(APIVersionNew);
        apiPublisherRestClient.changeAPILifeCycleStatus(updateRequest2);

        APIStoreRestClient apiStore = new APIStoreRestClient(storeURLHttp);
        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

        String apiData = apiStore.getAllPublishedAPIs().getData();

        Assert.assertTrue(!apiData.contains(APIVersionOld),
                "Old version available in the store");

        Assert.assertTrue(apiData.contains(APIVersionNew),
                "New version not available in the store");

        //subscribe to the old API version
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName,
                storeContext.getContextTenant().getContextUser().getUserName());
        apiData = apiStore.subscribe(subscriptionRequest).getData();

        Assert.assertTrue(apiData.contains("{\"error\" : false, \"status\" : {\"subscriptionStatus\" : \"UNBLOCKED\","),
                "Can subscribe to the old API version");

        //subscribe to the new API version
        subscriptionRequest.setVersion(APIVersionNew);
        apiData = apiStore.subscribe(subscriptionRequest).getData();

        Assert.assertTrue(apiData.contains("{\"error\" : false, \"status\" : {\"subscriptionStatus\" : \"UNBLOCKED\","),
                "Cannot subscribe to the new API version");

        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator("DefaultApplication");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "application/json");
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                                 APIMIntegrationConstants.IS_API_EXISTS);

        HttpResponse apiResponse1 = HttpRequestUtil.doGet(getAPIInvocationURLHttp( APIContext, "1.0.0") + APIEndpointMethod, requestHeaders);
        assertEquals(apiResponse1.getResponseCode(), 200, "Response code mismatched");

        HttpResponse apiResponse2 = HttpRequestUtil.doGet(getAPIInvocationURLHttp( APIContext, "2.0.0") + APIEndpointMethod, requestHeaders);
        assertEquals(apiResponse2.getResponseCode(), 200, "Response code mismatched");
    }

    @Test(groups = {"wso2.am"}, description = "API visibility")
    public void otherAPILifeCycleStatesTestCase() throws Exception {
        String APIName = "APILifeCycleAPI";
        String APIContext = "testVisibility";
        String tags = "testTag1, testTag2, testTag3";
        String url = "jaxrs_basic/services/customers/customerservice/";
        String description = "This is test API created by API manager Integration tests";
        String providerName = "admin";
        String APIVersion = "1.0.0";

        //add all option methods
        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        apiPublisherRestClient.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(gatewayUrlsWrk.getWebAppURLHttp() + url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setVisibility("restricted");
        apiRequest.setRoles("admin");
        apiPublisherRestClient.addAPI(apiRequest);
        APILifeCycleStateRequest updateRequest1 = new APILifeCycleStateRequest(APIName, providerName,
                APILifeCycleState.PUBLISHED);
        apiPublisherRestClient.changeAPILifeCycleStatus(updateRequest1);

        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

        String apiData = apiStore.getAllPublishedAPIs().getData();

        Assert.assertTrue(apiData.contains(APIName),
                "Added API not available in store");

        APILifeCycleStateRequest updateRequest2 = new APILifeCycleStateRequest(APIName, providerName,
                APILifeCycleState.DEPRECATED);
        apiPublisherRestClient.changeAPILifeCycleStatus(updateRequest2);

        APILifeCycleStateRequest updateRequest3 = new APILifeCycleStateRequest(APIName, providerName,
                APILifeCycleState.RETIRED);
        apiPublisherRestClient.changeAPILifeCycleStatus(updateRequest3);

        apiData = apiStore.getAllPublishedAPIs().getData();

        Assert.assertTrue(!apiData.contains(APIName),
                "Retired API available in store");

        APILifeCycleStateRequest updateRequest4 = new APILifeCycleStateRequest(APIName, providerName,
                APILifeCycleState.BLOCKED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest4);

        apiData = apiStore.getAllPublishedAPIs().getData();

        Assert.assertTrue(!apiData.contains(APIName),
                "Blocked API available in store");
    }

    @Test(groups = {"wso2.am"}, description = "API visibility")
    public void copyAndDepricateAPILifeCycleTestCase() throws Exception {
        String APIName = "APILifeCycleTestAPI";
        String APIContext = "testAPI";
        String tags = "testTag1, testTag2, testTag3";
        String APIEndpointMethod = "/customers/123";
        String url = "jaxrs_basic/services/customers/customerservice/";
        String description = "This is test API create by API manager integration test";
        String providerName = "admin";
        String APIVersionOld = "1.0.0";
        String APIVersionNew = "3.0.0";
        String applicationName = "TestApplication";

        //add all option methods
        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        apiPublisherRestClient.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(gatewayUrlsWrk.getWebAppURLHttp() + url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersionOld);
        apiRequest.setVisibility("restricted");
        apiRequest.setRoles("admin");
        apiPublisherRestClient.addAPI(apiRequest);

        //publish initial version
        APIBean apiBean = APIMTestCaseUtils.getAPIBeanFromHttpResponse(apiPublisherRestClient.getAPI(
                APIName, providerName));
        APILifeCycleStateRequest updateRequest1 = new APILifeCycleStateRequest(APIName, providerName,
                APILifeCycleState.PUBLISHED);
        apiPublisherRestClient.changeAPILifeCycleStatus(updateRequest1);

        //publish new version
        apiPublisherRestClient.copyAPI(providerName, APIName, APIVersionOld, APIVersionNew, "");
        //add assertion
        apiBean = APIMTestCaseUtils.getAPIBeanFromHttpResponse(apiPublisherRestClient.getAPI(APIName,
                providerName));
        APILifeCycleStateRequest updateRequest2 = new APILifeCycleStateRequest(APIName,
                providerName, APILifeCycleState.PUBLISHED);
        updateRequest2.setVersion(APIVersionNew);
        apiPublisherRestClient.changeAPILifeCycleStatus(updateRequest2);

        //deprecate old version
        apiBean = APIMTestCaseUtils.getAPIBeanFromHttpResponse(apiPublisherRestClient.getAPI(APIName,
                providerName));
        APILifeCycleStateRequest updateRequest3 = new APILifeCycleStateRequest(APIName,
                providerName, APILifeCycleState.DEPRECATED);
        updateRequest3.setVersion(APIVersionOld);
        apiPublisherRestClient.changeAPILifeCycleStatus(updateRequest3);

        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

        String apiData = apiStore.getAllPublishedAPIs().getData();

        Assert.assertTrue(!apiData.contains(APIVersionOld),
                "Old version available in the store");

        Assert.assertTrue(apiData.contains(APIVersionNew),
                "New version not available in the store");

        //subscribe to the old API version
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName,
                storeContext.getContextTenant().getContextUser().getUserName());
        apiData = apiStore.subscribe(subscriptionRequest).getData();

        Assert.assertTrue(apiData.contains("Error while adding subscription for user: admin. Reason: Subscriptions not allowed on APIs in the state: DEPRECATED"),
                "Can subscribe to the old API version");

        //subscribe to the new API version
        apiStore.addApplication(applicationName, "10PerMin", "", "this-is-test");
        SubscriptionRequest subscriptionAPIRequest = new SubscriptionRequest(APIName,
                storeContext.getContextTenant().getContextUser().getUserName());
        subscriptionAPIRequest.setApplicationName(applicationName);
        subscriptionAPIRequest.setVersion(APIVersionNew);
        apiStore.removeAPISubscriptionByName(APIName, APIVersionNew, providerName, "DefaultApplication");
        HttpResponse subscribeResponse = apiStore.subscribe(subscriptionAPIRequest);
        apiData = subscribeResponse.getData();
        Assert.assertTrue(apiData.contains("\"error\" : false"), "Cannot subscribe to the new API version");

        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(applicationName);
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "application/json");
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                                 APIMIntegrationConstants.IS_API_EXISTS);
        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), APIVersionNew,
                                 APIMIntegrationConstants.IS_API_EXISTS);

        HttpResponse apiResponse1 = HttpRequestUtil.doGet(getAPIInvocationURLHttp( APIContext, APIVersionOld) + APIEndpointMethod, requestHeaders);
        assertEquals(apiResponse1.getResponseCode(), 403, "Response code mismatched");

        HttpResponse apiResponse2 = HttpRequestUtil.doGet(getAPIInvocationURLHttp( APIContext, APIVersionNew) + APIEndpointMethod, requestHeaders);
        assertEquals(apiResponse2.getResponseCode(), 200, "Response code mismatched");
    }

    public void addPublicAPI(APIPublisherRestClient apiPublisherRestClient) throws Exception {
        String APIName = "APILifeCycleTestAPIPublic";
        String APIContext = "testAPIPublic";
        String tags = "testTag1, testTag2, testTag3";
        String APIEndpointMethod = "/customers/123";
        String url = "jaxrs_basic/services/customers/customerservice/";
        String description = "This is test API create by API manager integration test";
        String providerName = "admin";
        String APIVersion = "1.0.0";
        //add all option methods
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(gatewayUrlsWrk.getWebAppURLHttp() + url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setVisibility("public");
        apiPublisherRestClient.addAPI(apiRequest);
        //add assertion
        APIBean apiBean = APIMTestCaseUtils.getAPIBeanFromHttpResponse(apiPublisherRestClient.getAPI(APIName, providerName));
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(APIName, providerName, APILifeCycleState.PUBLISHED);
        apiPublisherRestClient.changeAPILifeCycleStatus(updateRequest);
    }

    public void addVisibleToDomainOnlyAPI(APIPublisherRestClient apiPublisherRestClient) throws Exception {
        String APIName = "APILifeCycleTestAPIDomainOnly";
        String APIContext = "testAPIDomainOnly";
        String tags = "testTag1, testTag2, testTag3";
        String APIEndpointMethod = "/customers/123";
        String url = "jaxrs_basic/services/customers/customerservice/";
        String description = "This is test API create by API manager integration test";
        String providerName = "admin";
        String APIVersion = "1.0.0";
        //add all option methods
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(gatewayUrlsWrk.getWebAppURLHttp() + url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setVisibility("private");
        apiPublisherRestClient.addAPI(apiRequest);
        APIBean apiBean = APIMTestCaseUtils.getAPIBeanFromHttpResponse(apiPublisherRestClient.getAPI(
                APIName, providerName));
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(APIName, providerName,
                APILifeCycleState.PUBLISHED);
        apiPublisherRestClient.changeAPILifeCycleStatus(updateRequest);
    }

    public void addVisibleToRolesAPI(APIPublisherRestClient apiPublisherRestClient) throws Exception {
        String APIName = "APILifeCycleTestAPIRoles";
        String APIContext = "testAPIRoles";
        String tags = "testTag1, testTag2, testTag3";
        String APIEndpointMethod = "/customers/123";
        String url = "jaxrs_basic/services/customers/customerservice/";
        String description = "This is test API create by API manager integration test";
        String providerName = "admin";
        String APIVersion = "1.0.0";
        //add all option methods
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(gatewayUrlsWrk.getWebAppURLHttp() + url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setVisibility("restricted");
        apiRequest.setRoles("admin");
        apiPublisherRestClient.addAPI(apiRequest);
        //add assertion
        APIBean apiBean = APIMTestCaseUtils.getAPIBeanFromHttpResponse(apiPublisherRestClient.getAPI(
                APIName, providerName));
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(
                APIName, providerName, APILifeCycleState.PUBLISHED);
        apiPublisherRestClient.changeAPILifeCycleStatus(updateRequest);
    }


    @Test(groups = {"wso2.am"}, description = "API Life cycle application related tests in store")
    public void testApplicationsInStoreTestCase()
            throws Exception {
        //Try login to publisher with tenant subscriber user

        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

        tenantManagementServiceClient.addTenant("wso2.com", "wso2@123", "wso2", "demo");
        if ((userManagementClient != null) &&
                !userManagementClient.userNameExists("Internal/subscriber", "user1@wso2.com")) {
            userManagementClient.addUser("user1@wso2.com", "password@123",
                    new String[]{"Internal/subscriber"}, null);
        }

        if ((userManagementClient != null) &&
                !userManagementClient.userNameExists("admin", "adminUser@wso2.com")) {
            userManagementClient.addUser("adminUser@wso2.com", "wso2@123",
                    new String[]{"admin"}, null);
        }

        apiStore.addApplication("carbonSuperApp", "10PerMin", "", "super-tenant-app");
        HttpResponse addApplicationResponse = apiStore.addApplication("carbonSuperApp", "10PerMin", "", "super-tenant-app");
        String apiData = addApplicationResponse.getData();
        Assert.assertTrue(apiData.contains("A duplicate application already exists"),
                "Application with duplicate name addition allowed");

        APIStoreRestClient apiStoreRestClient2 = new APIStoreRestClient(storeURLHttp);

        apiStoreRestClient2.login("adminUser@wso2.com",
                "wso2@123");
        HttpResponse addApplicationResponse1 = apiStoreRestClient2.addApplication(
                "carbonSuperApp", "10PerMin", "", "super-tenant-app");
        apiData = addApplicationResponse1.getData();
        Assert.assertTrue(!apiData.contains("{\"error\" : true}"),
                "Application with same name addition not allowed in other tenant");

        apiData = apiStore.removeApplication("carbonSuperApp").getData();
        Assert.assertTrue(apiData.contains("{\"error\" : false}"),
                "Application deletion failed");
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

        HttpResponse loginResponse = apiStoreRestClient.login("invaliduser", "invaliduser@123");
        String loginResponseString = loginResponse.getData();
        JSONObject response = new JSONObject(loginResponseString);
        String isLoginError = response.get("error").toString();
        if (isLoginError.equals("true")) {
            loginFailed = true;
        }
        Assert.assertTrue(loginFailed && loginResponseString.contains("Login failed. Please recheck the username and password and try again."),
                "Invalid user can login to the API store");
        loginFailed = false;

        try {
            apiStoreRestClient.login(APICreatorUser, password);
        } catch (Exception e) {
            loginFailed = true;
            errorString = e.getMessage().toString();
        }
        Assert.assertTrue(loginFailed && errorString.contains("No session cookie found with response"),
                "API creator can login to the API store");
        loginFailed = false;

        try {
            apiStoreRestClient.login(APIPublisherUser, password);
        } catch (Exception e) {
            loginFailed = true;
            errorString = e.getMessage().toString();
        }
        Assert.assertTrue(loginFailed && errorString.contains("No session cookie found with response"),
                "API publisher can login to the API store");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        //Update role permissions
        apiPublisher.updatePermissions("Gold", "allow", "admin");
        super.cleanUp();
    }
}
