/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.am.integration.tests.other;

import junit.framework.Assert;
import org.apache.commons.httpclient.HttpStatus;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;

import javax.xml.xpath.XPathExpressionException;
import java.net.URL;
import java.rmi.RemoteException;

import static org.testng.Assert.assertTrue;

/**
 * None of the apis are visible in API Store when permissions for one API has changed.
 * This is test class tests the fix for this issue
 *
 * @see <a href="https://wso2.org/jira/browse/APIMANAGER-4373">APIMANAGER-4373</a>
 */
public class APIMANAGER4373BrokenAPIInStoreTestCase extends APIMIntegrationBaseTest {
    private final String USER_PASSWORD = "123123";
    private final String PERMISSION_LOGIN = "/permission/admin/login";
    private final String PERMISSION_API_SUBSCRIBE = "/permission/admin/manage/api/subscribe";
    private final String FIRST_USER = "APIMANAGER4373_user";
    private final String FIRST_ROLE = "APIMANAGER4373_role1";
    private final String SECOND_ROLE = "APIMANAGER4373_role2";
    private final String APP_NAME = "APIMANAGER4373";
    private final String BROKEN_API = "brokenAPI";
    private final String HEALTHY_API = "healthyAPI";
    private final String EP_URL = "http://gdata.youtube.com/feeds/api/standardfeeds";
    private final String API_VERSION = "1.0.0";
    private final String RESTRICTED = "restricted";
    private final String TAG_UPDATED = "updated";
    private String contextUsername = "admin";
    private String contextUserPassword = "admin";
    private UserManagementClient userManagementClient;
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStoreRestClient;
    private String publisherURLHttp;
    private String storeURLHttp;

    @BeforeMethod
    public void init() {
        try {
            super.init();
            publisherURLHttp = publisherUrls.getWebAppURLHttp();
            storeURLHttp = storeUrls.getWebAppURLHttp();
            apiPublisher = new APIPublisherRestClient(publisherURLHttp);
            apiStoreRestClient = new APIStoreRestClient(storeURLHttp);
            contextUsername = publisherContext.getContextTenant().getContextUser().getUserName();
            contextUserPassword = publisherContext.getContextTenant().getContextUser().getPassword();
            userManagementClient = new UserManagementClient(publisherContext.getContextUrls().getBackEndUrl(),
                    contextUsername, contextUserPassword);

            userManagementClient
                    .addRole(FIRST_ROLE, new String[] {}, new String[] { PERMISSION_LOGIN, PERMISSION_API_SUBSCRIBE });
            userManagementClient
                    .addRole(SECOND_ROLE, new String[] {}, new String[] { PERMISSION_LOGIN, PERMISSION_API_SUBSCRIBE });
            userManagementClient.addUser(FIRST_USER, USER_PASSWORD, new String[] { FIRST_ROLE }, FIRST_USER);
        } catch (APIManagerIntegrationTestException e) {
            Assert.fail("Error occurred while initializing testcase.");
        } catch (RemoteException e) {
            Assert.fail("Error occurred while adding new users.");
        } catch (UserAdminUserAdminException e) {
            Assert.fail("Error occurred while adding new users.");
        } catch (XPathExpressionException e) {
            Assert.fail("Error occurred while retrieving context.");
        }
    }

    @Test(groups = "wso2.am", description = "Test effect of changing the role of subscribed API")
    public void testAPIRoleChangeEffectInStore() throws Exception {
        // create two apis
        apiPublisher.login(contextUsername, contextUserPassword);
        APIRequest brokenApiRequest = new APIRequest(BROKEN_API, BROKEN_API, new URL(EP_URL));
        brokenApiRequest.setVersion(API_VERSION);
        brokenApiRequest.setProvider(contextUsername);
        brokenApiRequest.setVisibility(RESTRICTED);
        brokenApiRequest.setRoles(FIRST_ROLE);
        apiPublisher.addAPI(brokenApiRequest);
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(BROKEN_API, contextUsername,
                APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        APIRequest healthyApiRequest = new APIRequest(HEALTHY_API, HEALTHY_API, new URL(EP_URL));
        healthyApiRequest.setVersion(API_VERSION);
        healthyApiRequest.setProvider(contextUsername);
        healthyApiRequest.setVisibility(RESTRICTED);
        healthyApiRequest.setRoles(FIRST_ROLE);
        apiPublisher.addAPI(healthyApiRequest);
        updateRequest = new APILifeCycleStateRequest(HEALTHY_API, contextUsername, APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        // subscribe both apis
        apiStoreRestClient.login(FIRST_USER, USER_PASSWORD);

        apiStoreRestClient.addApplication(APP_NAME, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(BROKEN_API, contextUsername);
        subscriptionRequest.setApplicationName(APP_NAME);
        apiStoreRestClient.subscribe(subscriptionRequest);
        subscriptionRequest = new SubscriptionRequest(HEALTHY_API, contextUsername);
        subscriptionRequest.setApplicationName(APP_NAME);
        apiStoreRestClient.subscribe(subscriptionRequest);

        brokenApiRequest.setRoles(SECOND_ROLE);
        brokenApiRequest.setTags(TAG_UPDATED);
        Thread.sleep(1000);
        apiPublisher.updateAPI(brokenApiRequest);

        HttpResponse response = new HttpResponse("", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        try {
            response = apiStoreRestClient.getAllSubscriptions();
        } catch (Exception e) {
            // Expectation of this test case is to test whether permitted apis are returned to the store.
            // therefore exception thrown from the registry for unauthorized api is not handled here.
        }
        assertTrue(response.getData().contains(HEALTHY_API), "Subscription retrieval failed when one API is broken");
    }

    @AfterMethod(alwaysRun = true)
    public void destroy() throws Exception {
        if (apiStoreRestClient != null) {
            apiStoreRestClient.removeAPISubscriptionByName(BROKEN_API, API_VERSION, contextUsername, APP_NAME);
            apiStoreRestClient.removeAPISubscriptionByName(HEALTHY_API, API_VERSION, contextUsername, APP_NAME);
        }
        if (apiPublisher != null) {
            apiPublisher.deleteAPI(BROKEN_API, API_VERSION, contextUsername);
            apiPublisher.deleteAPI(HEALTHY_API, API_VERSION, contextUsername);
        }
        if (userManagementClient != null) {
            userManagementClient.deleteUser(FIRST_USER);
            userManagementClient.deleteRole(FIRST_ROLE);
            userManagementClient.deleteRole(SECOND_ROLE);
        }
        super.cleanUp();
    }
}
