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
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test class is used to test the API tags visibility for anonymous and authorised users when API role based visibility
 * is used.
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE })
public class APITagVisibilityByRoleTestCase extends APIMIntegrationBaseTest {

    private final Log log = LogFactory.getLog(APITagVisibilityByRoleTestCase.class);
    private APIStoreRestClient apiStore;
    private String storeURLHttp;
    private String publisherURLHttp;
    private APIPublisherRestClient apiPublisher;
    private URL tagListUrl;
    private UserManagementClient userManagementClient1;
    private APIRequest apiRequestPublic, apiRequestRestricted;
    private String endpointUrl;

    private String apiNamePublic = "APITagVisibilityByRoleTestCaseAPIName1";
    private String APIContextPublic = "APITagVisibilityByRoleTestCaseContext1";
    private String apiNameRestricted = "APITagVisibilityByRoleTestCaseAPIName2";
    private String APIContextRestricted = "APITagVisibilityByRoleTestCaseContext2";
    private String tagsPublic = "APITagVisibilityPublicTag";
    private String tagsRestricted = "APITagVisibilityRestrictedTag";
    private String description = "This is test API create by APIM APITagVisibilityByRoleTestCase";
    private String APIVersion = "1.0.0";
    private String allowedUser = "APITagVisibilityByRoleUser";
    private char[] allowedUserPass = "password@123".toCharArray();
    private String role = "APITagVisibilityRole1";
    private String[] permissions = { "/permission/admin/login", "/permission/admin/manage/api/subscribe" };
    private Map<String, String> requestHeaders = new HashMap<String, String>();
    private static final long WAIT_TIME = 45 * 1000;

    @Factory(dataProvider = "userModeDataProvider")
    public APITagVisibilityByRoleTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        publisherURLHttp = getPublisherURLHttp();
        storeURLHttp = getStoreURLHttp();
        endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api/add";

        apiStore = new APIStoreRestClient(storeURLHttp);
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(user.getUserName(), user.getPassword());

        //adding new role and user
        userManagementClient1 = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                                                         createSession(keyManagerContext));
        userManagementClient1.addRole(role, null, permissions);
        userManagementClient1.addUser(allowedUser, String.valueOf(allowedUserPass), new String[] {role }, null);
        tagListUrl = new URL(getStoreURLHttp() + "store/site/blocks/tag/tag-cloud/ajax/list.jag");
    }

    @Test(groups = { "wso2.am" }, description = "Create and publish two apis with public and role based visibility")
    public void testAPICreationWithVisibility() throws Exception {
        String providerName = user.getUserName();

        //API request for public visible API
        apiRequestPublic = new APIRequest(apiNamePublic, APIContextPublic, new URL(endpointUrl));
        apiRequestPublic.setTags(tagsPublic);
        apiRequestPublic.setDescription(description);
        apiRequestPublic.setVersion(APIVersion);
        apiRequestPublic.setProvider(providerName);

        //API request for role base visible API
        apiRequestRestricted = new APIRequest(apiNameRestricted, APIContextRestricted, new URL(endpointUrl));
        apiRequestRestricted.setTags(tagsRestricted);
        apiRequestRestricted.setDescription(description);
        apiRequestRestricted.setVersion(APIVersion);
        apiRequestRestricted.setProvider(providerName);
        apiRequestRestricted.setVisibility("restricted");
        apiRequestRestricted.setRoles(role);

        //add test api
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequestPublic);
        verifyResponse(serviceResponse);
        //publish the api
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiNamePublic, user.getUserName(),
                APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);

        //add test api
        serviceResponse = apiPublisher.addAPI(apiRequestRestricted);
        verifyResponse(serviceResponse);
        //publish the api
        updateRequest = new APILifeCycleStateRequest(apiNameRestricted, user.getUserName(),
                APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);
    }

    @Test(groups = { "wso2.am" }, description = "Test the API tag visibility as a anonymous user",
            dependsOnMethods = "testAPICreationWithVisibility")
    public void testAPITagVisibilityAnonymousUser() throws Exception {
        requestHeaders.clear();

        String tenant = user.getUserDomain();
        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add(new BasicNameValuePair("action", "getAllTags"));
        urlParameters.add(new BasicNameValuePair("tenant", tenant));
        HttpEntity content = new UrlEncodedFormEntity(urlParameters);
        String contentString = EntityUtils.toString(content);

        watForTagsAvailableOnSearchApi(tagsRestricted);
        HttpResponse serviceResponse = HttpRequestUtil.doPost(tagListUrl, contentString, requestHeaders);
        Assert.assertTrue(serviceResponse.getData().contains(tagsPublic),
                "Public visibility tag is not available for anonymous user");
        Assert.assertFalse(serviceResponse.getData().contains(tagsRestricted),
                "Restricted visibility tag is available for anonymous user");
    }

    @Test(groups = { "wso2.am" }, description = "Test the API tag visibility as a authorised user",
            dependsOnMethods = "testAPITagVisibilityAnonymousUser")
    public void testAPITagVisibilityAuthorisedUser() throws Exception {

        String tenant = user.getUserDomain();
        String currentUser = allowedUser;
        if ("wso2.com".equals(tenant)) {
            currentUser = currentUser + '@' + tenant;
        }
        HttpResponse response = apiStore.login(currentUser, String.valueOf(allowedUserPass));
        String cookie = response.getHeaders().get("Set-Cookie");
        requestHeaders.put("Cookie", cookie);

        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add(new BasicNameValuePair("action", "getAllTags"));
        urlParameters.add(new BasicNameValuePair("tenant", tenant));
        HttpEntity content = new UrlEncodedFormEntity(urlParameters);
        String contentString = EntityUtils.toString(content);

        HttpResponse serviceResponse = HttpRequestUtil.doPost(tagListUrl, contentString, requestHeaders);
        Assert.assertTrue(serviceResponse.getData().contains(tagsPublic),
                "Public visibility tag is not available for authorised user");
        Assert.assertTrue(serviceResponse.getData().contains(tagsRestricted),
                "Restricted visibility tag is not available for authorised user");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiPublisher.deleteAPI(apiNamePublic, APIVersion, user.getUserName());
        apiPublisher.deleteAPI(apiNameRestricted, APIVersion, user.getUserName());
        userManagementClient1.deleteRole(role);
        userManagementClient1.deleteUser(allowedUser);
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }, };
    }

    /**
     * Used to wait until published apis tags are appear in the Store tag cloud API
     *
     * @throws Exception if tag cloud api throws any exceptions
     */
    public void watForTagsAvailableOnSearchApi(String tag) throws Exception {
        long waitTime = System.currentTimeMillis() + WAIT_TIME;
        HttpResponse response;
        while (waitTime > System.currentTimeMillis()) {
            List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
            urlParameters.add(new BasicNameValuePair("action", "getAllTags"));
            urlParameters.add(new BasicNameValuePair("tenant", user.getUserDomain()));
            HttpEntity content = new UrlEncodedFormEntity(urlParameters);
            String contentString = EntityUtils.toString(content);
            Map<String, String> requestHeaders = new HashMap<String, String>();
            response = HttpRequestUtil.doPost(tagListUrl, contentString, requestHeaders);
            verifyResponse(response);
            log.info("WAIT for availability of tags : " + tag + " found on Store tag cloud");
            if (response != null) {
                log.info("Data: " + response.getData());
                if (response.getData().contains(tag)) {
                    log.info("Tag :" + tag + " found");
                    break;
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }
}
