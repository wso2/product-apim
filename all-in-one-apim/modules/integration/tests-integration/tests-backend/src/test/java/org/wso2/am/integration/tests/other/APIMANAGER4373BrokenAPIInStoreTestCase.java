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

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;

import javax.xml.xpath.XPathExpressionException;
import java.net.URL;
import java.rmi.RemoteException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * None of the apis are visible in API Store when permissions for one API has changed.
 * This is test class tests the fix for this issue
 *
 * @see <a href="https://wso2.org/jira/browse/APIMANAGER-4373">APIMANAGER-4373</a>
 */
public class APIMANAGER4373BrokenAPIInStoreTestCase extends APIManagerLifecycleBaseTest {
    private final String USER_PASSWORD = "123123";
    private final String FIRST_USER = "APIMANAGER4373_user";
    private final String FIRST_ROLE = "APIMANAGER4373_role1";
    private final String SECOND_ROLE = "APIMANAGER4373_role2";
    private final String INTERNAL_SUBSCRIBER = "Internal/subscriber";
    private final String APP_NAME = "APIMANAGER4373";
    private final String BROKEN_API = "brokenAPI";
    private final String HEALTHY_API = "healthyAPI";
    private final String EP_URL = "http://gdata.youtube.com/feeds/api/standardfeeds";
    private final String API_VERSION = "1.0.0";
    private final String RESTRICTED = "restricted";
    private final String TAG_UPDATED = "updated";
    private String contextUsername = "admin";
    private String contextUserPassword = "admin";
    private UserManagementClient userManagementClient1;
    private RestAPIStoreImpl apiStoreSubUser;
    private String publisherURLHttp;
    private String storeURLHttp;
    private String brokenApiId;
    private String healthyApiId;
    private String appId;

    @BeforeClass
    public void init() {
        try {
            super.init();
            publisherURLHttp = publisherUrls.getWebAppURLHttp();
            storeURLHttp = storeUrls.getWebAppURLHttp();
            contextUsername = keyManagerContext.getContextTenant().getContextUser().getUserName();
            contextUserPassword = keyManagerContext.getContextTenant().getContextUser().getPassword();
            userManagementClient1 = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                                                             contextUsername, contextUserPassword);

            userManagementClient1
                    .addRole(FIRST_ROLE, new String[] {}, new String[] {});
            userManagementClient1
                    .addRole(SECOND_ROLE, new String[] {}, new String[] {});
            userManagementClient1
                    .addUser(FIRST_USER, USER_PASSWORD, new String[] { INTERNAL_SUBSCRIBER, FIRST_ROLE }, FIRST_USER);

            apiStoreSubUser = new RestAPIStoreImpl(FIRST_USER, USER_PASSWORD,
                    keyManagerContext.getContextTenant().getDomain(), storeURLHttps);

        } catch (APIManagerIntegrationTestException e) {
            assertTrue(false, "Error occurred while initializing testcase: " + e.getCause());
        } catch (RemoteException e) {
            assertTrue(false, "Error occurred while adding new users: " + e.getCause());
        } catch (UserAdminUserAdminException e) {
            assertTrue(false, "Error occurred while adding new users: " + e.getCause());
        } catch (XPathExpressionException e) {
            assertTrue(false, "Error occurred while retrieving context: " + e.getCause());
        }
    }

    @Test(groups = "wso2.am", description = "Test effect of changing the role of subscribed API")
    public void testAPIRoleChangeEffectInStore() throws Exception {
        // create two apis
        APIRequest brokenApiRequest = new APIRequest(BROKEN_API, BROKEN_API, new URL(EP_URL));
        brokenApiRequest.setVersion(API_VERSION);
        brokenApiRequest.setProvider(contextUsername);
        brokenApiRequest.setVisibility(RESTRICTED);
        brokenApiRequest.setRoles(FIRST_ROLE);
        brokenApiId = createAndPublishAPIUsingRest(brokenApiRequest, restAPIPublisher, false);

        APIRequest healthyApiRequest = new APIRequest(HEALTHY_API, HEALTHY_API, new URL(EP_URL));
        healthyApiRequest.setVersion(API_VERSION);
        healthyApiRequest.setProvider(contextUsername);
        healthyApiRequest.setVisibility(RESTRICTED);
        healthyApiRequest.setRoles(FIRST_ROLE);
        healthyApiId = createAndPublishAPIUsingRest(healthyApiRequest, restAPIPublisher, false);

        waitForAPIDeploymentSync(contextUsername, HEALTHY_API, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);

        // subscribe both apis

        //add an application
        HttpResponse applicationResponse = apiStoreSubUser.createApplication(APP_NAME,
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        appId = applicationResponse.getData();

        //subscribe to healthy api
        HttpResponse subscribeResponse = subscribeToAPIUsingRest(brokenApiId, appId,
                APIMIntegrationConstants.API_TIER.GOLD, apiStoreSubUser);
        assertEquals(subscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of API request not successful " +
                        " API Name:" + BROKEN_API + " API Version:" + API_VERSION +
                        " API Provider Name :" + contextUsername);

        //subscribe to healthy api
        subscribeResponse = subscribeToAPIUsingRest(healthyApiId, appId,
                APIMIntegrationConstants.API_TIER.GOLD, apiStoreSubUser);
        assertEquals(subscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of API request not successful " +
                        " API Name:" + HEALTHY_API + " API Version:" + API_VERSION +
                        " API Provider Name :" + contextUsername);

        brokenApiRequest.setRoles(SECOND_ROLE);
        brokenApiRequest.setTags(TAG_UPDATED);
        restAPIPublisher.updateAPI(brokenApiRequest, brokenApiId);
        waitForAPIDeployment();
        SubscriptionListDTO subscriptionListDTO = null;
        try {
            subscriptionListDTO = apiStoreSubUser.getAllSubscriptionsOfApplication(appId);
            assertTrue(subscriptionListDTO.getList().toString().contains(HEALTHY_API), "Subscription retrieval failed when one API is broken");
        } catch (Exception e) {
            // Expectation of this test case is to test whether permitted apis are returned to the store.
            // therefore exception thrown from the registry for unauthorized api is not handled here.
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        userManagementClient1
                .updateRolesOfUser(FIRST_USER, new String[] { INTERNAL_SUBSCRIBER, FIRST_ROLE, SECOND_ROLE });

        SubscriptionListDTO subsDTO = apiStoreSubUser.getAllSubscriptionsOfApplication(appId);
        for (SubscriptionDTO subscriptionDTO : subsDTO.getList()) {
            apiStoreSubUser.removeSubscription(subscriptionDTO);
        }

        apiStoreSubUser.deleteApplication(appId);
        undeployAndDeleteAPIRevisionsUsingRest(brokenApiId, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(healthyApiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(brokenApiId);
        restAPIPublisher.deleteAPI(healthyApiId);

        if (userManagementClient1 != null) {
            userManagementClient1.deleteUser(FIRST_USER);
            userManagementClient1.deleteRole(FIRST_ROLE);
            userManagementClient1.deleteRole(SECOND_ROLE);
        }
    }
}
