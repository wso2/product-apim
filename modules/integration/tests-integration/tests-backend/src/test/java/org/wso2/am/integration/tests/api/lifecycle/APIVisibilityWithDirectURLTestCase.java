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
*
*/
package org.wso2.am.integration.tests.api.lifecycle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class APIVisibilityWithDirectURLTestCase extends APIManagerLifecycleBaseTest {
    private final Log log = LogFactory.getLog(APIManagerLifecycleBaseTest.class);
    private String apiName = "APIVisibilityWithDirectURLTestCaseAPIName";
    private String APIContext = "APIVisibilityWithDirectURLTestCaseAPIContext";
    private String apiNameTenant = "APIVisibilityWithDirectURLTestCaseAPIName";
    private String APIContextTenant = "APIVisibilityWithDirectURLTestCaseAPIContext";
    private String tags = "test, EndpointType";
    private String endpointUrl;
    private String description = "This is test API create by API manager integration test";
    private String APIVersion = "1.0.0";

    private final String CARBON_SUPER_SUBSCRIBER_USERNAME = "directUrlUser";
    private final char[] CARBON_SUPER_SUBSCRIBER_PASSWORD = "password@123".toCharArray();
    private final String CARBON_SUPER_SUBSCRIBER_1_USERNAME = "directUrlUser1";
    private final char[] CARBON_SUPER_SUBSCRIBER_1_PASSWORD = "password@123".toCharArray();
    private final String INTERNAL_ROLE_SUBSCRIBER = "directUrlRole";
    private final String INTERNAL_ROLE_SUBSCRIBER_1 = "directUrlRole1";
    private final String SUPER_ADMIN_USERNAME = "admin";
    private final String SUPER_ADMIN_DOMAIN = "carbon.super";
    private final String TENANT_SUBSCRIBER_USERNAME = "directUrlUser";
    private final char[] TENANT_SUBSCRIBER_PASSWORD = "password@123".toCharArray();
    private final String TENANT_SUBSCRIBER_1_USERNAME = "directUrlUser1";
    private final char[] TENANT_SUBSCRIBER_1_PASSWORD = "password@123".toCharArray();
    private final String TENANT_DOMAIN_KEY = "wso2.com";
    private final String TENANT_DOMAIN_ADMIN_KEY = "admin";

    private Map<String, String> requestHeaders = new HashMap<String, String>();
    private APIRequest apiRequest;
    private APIRequest apiRequestTenant;
    private APIPublisherRestClient apiPublisherClientAdminOtherDomain;
    private UserManagementClient userManagementClient2;
    private APIStoreRestClient apiStoreClientAnotherUserOtherDomain;
    private UserManagementClient userManagementClient1;
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String[] permissions = { "/permission/admin/login", "/permission/admin/manage/api/subscribe" };

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        //Creating CarbonSuper context
        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();
        endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api/add";
        apiStore = new APIStoreRestClient(storeURLHttp);
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(user.getUserName(), user.getPassword());

        userManagementClient1 = new UserManagementClient(publisherContext.getContextUrls().getBackEndUrl(),
                createSession(publisherContext));
        userManagementClient1.addRole(INTERNAL_ROLE_SUBSCRIBER, null, permissions);
        userManagementClient1.addRole(INTERNAL_ROLE_SUBSCRIBER_1, null, permissions);
        userManagementClient1
                .addUser(CARBON_SUPER_SUBSCRIBER_USERNAME, String.valueOf(CARBON_SUPER_SUBSCRIBER_PASSWORD),
                        new String[] { INTERNAL_ROLE_SUBSCRIBER }, null);
        userManagementClient1
                .addUser(CARBON_SUPER_SUBSCRIBER_1_USERNAME, String.valueOf(CARBON_SUPER_SUBSCRIBER_1_PASSWORD),
                        new String[] { INTERNAL_ROLE_SUBSCRIBER_1 }, null);
    }

    @Test(groups = { "wso2.am" }, description = "Sample API creation and publishing")
    public void testAPICreation() throws Exception {
        String providerName = user.getUserName();

        apiRequest = new APIRequest(apiName, APIContext, new URL(endpointUrl));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setProvider(providerName);
        apiRequest.setVisibility("restricted");
        apiRequest.setRoles(INTERNAL_ROLE_SUBSCRIBER);
        //add test api
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(serviceResponse);

        //publish the api
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, user.getUserName(),
                APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);

    }

    @Test(groups = {
            "wso2.am" }, description = "Test availability of the api without login", dependsOnMethods = "testAPICreation")
    public void testDirectLinkAnonymous() throws Exception {
        HttpResponse a = HttpRequestUtil
                .doGet(getStoreURLHttps() + "/store/apis/info?name=" + apiName + "&version=" + APIVersion + "&provider="
                        + SUPER_ADMIN_USERNAME + "&tenant=" + SUPER_ADMIN_DOMAIN, requestHeaders);
        Assert.assertTrue(a.getData().contains("user is not authorized to view the API"),
                "API " + apiName + "is available to the restricted user");
    }

    @Test(groups = {
            "wso2.am" }, description = "Test availability of the api with login", dependsOnMethods = "testDirectLinkAnonymous")
    public void testDirectLink() throws Exception {
        HttpResponse response = apiStore
                .login(CARBON_SUPER_SUBSCRIBER_USERNAME, String.valueOf(CARBON_SUPER_SUBSCRIBER_PASSWORD));
        String session = response.getHeaders().get("Set-Cookie");
        requestHeaders.put("Cookie", session);
        HttpResponse a = HttpRequestUtil
                .doGet(getStoreURLHttps() + "/store/apis/info?name=" + apiName + "&version=" + APIVersion + "&provider="
                        + SUPER_ADMIN_USERNAME + "&tenant=" + SUPER_ADMIN_DOMAIN, requestHeaders);
        Assert.assertFalse(a.getData().contains("user is not authorized to view the API"),
                "API " + apiName + "is not available to the authorised user");
    }

    @Test(groups = { "wso2.am" }, description = "Test availability of the api from user without restricted role",
            dependsOnMethods = "testDirectLinkAnonymous")
    public void testDirectLinkWithoutRestrictedRoleUser() throws Exception {
        HttpResponse response = apiStore
                .login(CARBON_SUPER_SUBSCRIBER_1_USERNAME, String.valueOf(CARBON_SUPER_SUBSCRIBER_1_PASSWORD));
        String session = response.getHeaders().get("Set-Cookie");
        requestHeaders.put("Cookie", session);
        HttpResponse a = HttpRequestUtil
                .doGet(getStoreURLHttps() + "/store/apis/info?name=" + apiName + "&version=" + APIVersion + "&provider="
                        + SUPER_ADMIN_USERNAME + "&tenant=" + SUPER_ADMIN_DOMAIN, requestHeaders);
        Assert.assertTrue(a.getData().contains("user is not authorized to view the API"),
                "API " + apiName + "is available to the unauthorised user");
    }

    @Test(groups = {
            "wso2.am" }, description = "Sample API creation and publishing in Tenant", dependsOnMethods = "testDirectLink")
    public void testAPICreationInTenant() throws Exception {

        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();

        init(TENANT_DOMAIN_KEY, TENANT_DOMAIN_ADMIN_KEY);
        //        otherDomain = storeContext.getContextTenant().getDomain();
        apiPublisherClientAdminOtherDomain = new APIPublisherRestClient(publisherURLHttp);
        apiPublisherClientAdminOtherDomain.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        userManagementClient2 = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                createSession(keyManagerContext));

        userManagementClient2.addRole(INTERNAL_ROLE_SUBSCRIBER, null, permissions);
        userManagementClient2.addRole(INTERNAL_ROLE_SUBSCRIBER_1, null, permissions);
        userManagementClient2.addUser(TENANT_SUBSCRIBER_USERNAME, String.valueOf(TENANT_SUBSCRIBER_PASSWORD),
                new String[] { INTERNAL_ROLE_SUBSCRIBER }, null);
        userManagementClient2.addUser(TENANT_SUBSCRIBER_1_USERNAME, String.valueOf(TENANT_SUBSCRIBER_1_PASSWORD),
                new String[] { INTERNAL_ROLE_SUBSCRIBER_1 }, null);

        apiStoreClientAnotherUserOtherDomain = new APIStoreRestClient(storeURLHttp);

        String publisher = storeContext.getContextTenant().getContextUser().getUserName();
        apiRequestTenant = new APIRequest(apiNameTenant, APIContextTenant, new URL(endpointUrl));
        apiRequestTenant.setTags(tags);
        apiRequestTenant.setDescription(description);
        apiRequestTenant.setVersion(APIVersion);
        apiRequestTenant.setProvider(publisher);
        apiRequestTenant.setVisibility("restricted");
        apiRequestTenant.setRoles(INTERNAL_ROLE_SUBSCRIBER);
        //add test api
        HttpResponse serviceResponse = apiPublisherClientAdminOtherDomain.addAPI(apiRequestTenant);
        verifyResponse(serviceResponse);

        //publish the api
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiNameTenant, publisher,
                APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisherClientAdminOtherDomain.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);

    }

    @Test(groups = {
            "wso2.am" }, description = "Test availability of the api without logi in tenant", dependsOnMethods = "testAPICreationInTenant")
    public void testDirectLinkInTenantAnonymous() throws Exception {
        requestHeaders.clear();
        String publisher = storeContext.getContextTenant().getContextUser().getUserName();
        HttpResponse a = HttpRequestUtil
                .doGet(getStoreURLHttps() + "/store/apis/info?name=" + apiNameTenant + "&version=" + APIVersion
                        + "&provider=" + publisher + "&tenant=" + TENANT_DOMAIN_KEY, requestHeaders);
        Assert.assertTrue(a.getData().contains("user is not authorized to view the API"),
                "API " + apiNameTenant + "is available to the restricted user in Tenant");
    }

    @Test(groups = {
            "wso2.am" }, description = "Test availability of the api with login in tenant", dependsOnMethods = "testDirectLinkInTenantAnonymous")
    public void testDirectLinkInTenant() throws Exception {

        HttpResponse response = apiStoreClientAnotherUserOtherDomain
                .login(TENANT_SUBSCRIBER_USERNAME + "@" + TENANT_DOMAIN_KEY,
                        String.valueOf(TENANT_SUBSCRIBER_PASSWORD));
        String publisher = storeContext.getContextTenant().getContextUser().getUserName();
        String session = response.getHeaders().get("Set-Cookie");
        requestHeaders.put("Cookie", session);
        HttpResponse a = HttpRequestUtil
                .doGet(getStoreURLHttps() + "/store/apis/info?name=" + apiNameTenant + "&version=" + APIVersion
                        + "&provider=" + publisher + "&tenant=" + TENANT_DOMAIN_KEY, requestHeaders);
        Assert.assertFalse(a.getData().contains("user is not authorized to view the API"),
                "API " + apiNameTenant + "is not available to the authorised user in Tenant");
    }

    @Test(groups = {
            "wso2.am" }, description = "Test availability of the api from user without restricted role in tenant",
            dependsOnMethods = "testDirectLinkInTenantAnonymous")
    public void testDirectLinkInTenantWithoutRestrictedRoleUser() throws Exception {

        HttpResponse response = apiStoreClientAnotherUserOtherDomain
                .login(TENANT_SUBSCRIBER_1_USERNAME + "@" + TENANT_DOMAIN_KEY,
                        String.valueOf(TENANT_SUBSCRIBER_1_PASSWORD));
        String publisher = storeContext.getContextTenant().getContextUser().getUserName();
        String session = response.getHeaders().get("Set-Cookie");
        requestHeaders.put("Cookie", session);
        HttpResponse a = HttpRequestUtil
                .doGet(getStoreURLHttps() + "/store/apis/info?name=" + apiNameTenant + "&version=" + APIVersion
                        + "&provider=" + publisher + "&tenant=" + TENANT_DOMAIN_KEY, requestHeaders);
        Assert.assertTrue(a.getData().contains("user is not authorized to view the API"),
                "API " + apiNameTenant + "is not available to the unauthorised user in Tenant");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiPublisher.deleteAPI(apiName, APIVersion, user.getUserName());
        String publisher = storeContext.getContextTenant().getContextUser().getUserName();
        apiPublisherClientAdminOtherDomain.deleteAPI(apiNameTenant, APIVersion, publisher);

        userManagementClient1.deleteUser(CARBON_SUPER_SUBSCRIBER_USERNAME);
        userManagementClient2.deleteUser(TENANT_SUBSCRIBER_USERNAME);

        super.cleanUp();
    }

}
