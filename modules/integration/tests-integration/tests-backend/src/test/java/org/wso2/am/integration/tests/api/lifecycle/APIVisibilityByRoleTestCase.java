/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.api.lifecycle;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Create a API with Role visibility and check the visibility in Publisher Store.
 */
public class APIVisibilityByRoleTestCase extends APIManagerLifecycleBaseTest {

    private final String API_NAME_ADMIN_VISIBILITY = "APIVisibilityByRoleTest";
    private final String API_NAME_SUBSCRIBER_VISIBILITY = "APIVisibilityByRole";
    private final String API_CONTEXT1 = "testAPI1";
    private final String API_CONTEXT2 = "testAPI2";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String CARBON_SUPER_TENANT2_KEY = "userKey2";
    private final String TENANT_DOMAIN_KEY = "wso2.com";
    private final String TENANT_DOMAIN_ADMIN_KEY = "admin";
    private final String USER_KEY_USER2 = "userKey1";
    private final String OTHER_DOMAIN_TENANT_USER_KEY = "user1";
    private final String CARBON_SUPER_SUBSCRIBER_USERNAME = "subscriberUser1";
    private final char[] CARBON_SUPER_SUBSCRIBER_PASSWORD = "password@123".toCharArray();
    private final String TENANT_SUBSCRIBER_USERNAME = "subscriberUser2";
    private final char[] TENANT_SUBSCRIBER_PASSWORD = "password@123".toCharArray();
    private final String INTERNAL_ROLE_SUBSCRIBER = "Internal/subscriber";
    private final String ROLE_SUBSCRIBER = "subscriber";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String providerName;
    private APIPublisherRestClient apiPublisherClientCarbonSuperUser1;
    private APIStoreRestClient apiStoreClientCarbonSuperUser1;
    private APIPublisherRestClient apiPublisherClientCarbonSuperAdmin;
    private APIStoreRestClient apiStoreClientCarbonSuperAdmin;
    private APIIdentifier apiIdentifierAdminVisibility;
    private APIIdentifier apiIdentifierSubscriberVisibility;
    private APIStoreRestClient apiStoreClientCarbonSuperUser2;
    private APIPublisherRestClient apiPublisherClientCarbonSuperUser2;
    private APIStoreRestClient apiStoreClientAnotherUserOtherDomain;
    private APIPublisherRestClient apiPublisherClientAnotherUserOtherDomain;
    private APIStoreRestClient apiStoreClientAdminOtherDomain;
    private APIPublisherRestClient apiPublisherClientAdminOtherDomain;
    private UserManagementClient userManagementClient1;
    private UserManagementClient userManagementClient2;
    private APIStoreRestClient apiStoreClientSubscriberUserSameDomain;
    private APIStoreRestClient apiStoreClientSubscriberUserOtherDomain;
    private String apiCreatorStoreDomain;
    private String storeURLHttp;
    private String otherDomain;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        //Creating CarbonSuper context
        super.init();
        apiEndPointUrl = getGatewayURLHttp() + API_END_POINT_POSTFIX_URL;
        String publisherURLHttp = getPublisherURLHttp();
        storeURLHttp = getStoreURLHttp();
        //Login to API Publisher and Store with CarbonSuper admin
        apiPublisherClientCarbonSuperAdmin = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientCarbonSuperAdmin = new APIStoreRestClient(storeURLHttp);

        apiPublisherClientCarbonSuperAdmin.login(user.getUserName(), user.getPassword());

        apiStoreClientCarbonSuperAdmin.login(user.getUserName(), user.getPassword());

        //Login to API Publisher adn Store with CarbonSuper normal user1
        apiPublisherClientCarbonSuperUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientCarbonSuperUser1 = new APIStoreRestClient(storeURLHttp);
        providerName = publisherContext.getContextTenant().getTenantUser(USER_KEY_USER2).getUserName();

        apiPublisherClientCarbonSuperUser1.login(
                publisherContext.getContextTenant().getTenantUser(USER_KEY_USER2).getUserName(),
                publisherContext.getContextTenant().getTenantUser(USER_KEY_USER2).getPassword());

        apiStoreClientCarbonSuperUser1.login(
                storeContext.getContextTenant().getTenantUser(USER_KEY_USER2).getUserName(),
                storeContext.getContextTenant().getTenantUser(USER_KEY_USER2).getPassword());

        //Login to API Publisher adn Store with CarbonSuper normal user2
        apiCreatorStoreDomain = storeContext.getContextTenant().getDomain();
        apiStoreClientCarbonSuperUser2 = new APIStoreRestClient(storeURLHttp);

        apiPublisherClientCarbonSuperUser2 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientCarbonSuperUser2.login(
                storeContext.getContextTenant().getTenantUser(CARBON_SUPER_TENANT2_KEY).getUserName(),
                storeContext.getContextTenant().getTenantUser(CARBON_SUPER_TENANT2_KEY).getPassword());

        apiPublisherClientCarbonSuperUser2.login(
                publisherContext.getContextTenant().getTenantUser(CARBON_SUPER_TENANT2_KEY).getUserName(),
                publisherContext.getContextTenant().getTenantUser(CARBON_SUPER_TENANT2_KEY).getPassword());

        // create new user in CarbonSuper with only subscriber role and login to the Store
        userManagementClient1 =
                new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                                         createSession(keyManagerContext));

        if (userManagementClient1.userNameExists(INTERNAL_ROLE_SUBSCRIBER, CARBON_SUPER_SUBSCRIBER_USERNAME)) {
            userManagementClient1.deleteUser(CARBON_SUPER_SUBSCRIBER_USERNAME);
        }

        userManagementClient1.addUser(CARBON_SUPER_SUBSCRIBER_USERNAME, String.valueOf(CARBON_SUPER_SUBSCRIBER_PASSWORD),
                                      new String[]{INTERNAL_ROLE_SUBSCRIBER}, null);

        apiStoreClientSubscriberUserSameDomain = new APIStoreRestClient(storeURLHttp);
        apiStoreClientSubscriberUserSameDomain.login(
                CARBON_SUPER_SUBSCRIBER_USERNAME, String.valueOf(CARBON_SUPER_SUBSCRIBER_PASSWORD));
        //Creating Tenant contexts
        init(TENANT_DOMAIN_KEY, TENANT_DOMAIN_ADMIN_KEY);
        otherDomain = storeContext.getContextTenant().getDomain();
        //Login to the API Publisher adn Store as Tenant user
        apiStoreClientAnotherUserOtherDomain = new APIStoreRestClient(storeURLHttp);
        apiPublisherClientAnotherUserOtherDomain = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientAnotherUserOtherDomain.login(
                storeContext.getContextTenant().getTenantUser(OTHER_DOMAIN_TENANT_USER_KEY).getUserName(),
                storeContext.getContextTenant().getTenantUser(OTHER_DOMAIN_TENANT_USER_KEY).getUserName());
        apiPublisherClientAnotherUserOtherDomain.login(
                publisherContext.getContextTenant().getTenantUser(OTHER_DOMAIN_TENANT_USER_KEY).getUserName(),
                publisherContext.getContextTenant().getTenantUser(OTHER_DOMAIN_TENANT_USER_KEY).getUserName());
        //Login to the API Publisher adn Store as Tenant admin
        apiStoreClientAdminOtherDomain = new APIStoreRestClient(storeURLHttp);
        apiPublisherClientAdminOtherDomain = new APIPublisherRestClient(publisherURLHttp);

        apiStoreClientAdminOtherDomain.login(
                storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

        apiPublisherClientAdminOtherDomain.login(
                publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        // create new user in tenant with only subscriber role and login to the Store
        userManagementClient2 = new UserManagementClient(
                keyManagerContext.getContextUrls().getBackEndUrl(), createSession(keyManagerContext));
        if (userManagementClient2.roleNameExists(INTERNAL_ROLE_SUBSCRIBER)) {
            userManagementClient2.deleteRole(INTERNAL_ROLE_SUBSCRIBER);
        }

        userManagementClient2.addInternalRole(ROLE_SUBSCRIBER,
                                              new String[]{},
                                              new String[]{"/permission/admin/login",
                                                           "/permission/admin/manage/api/subscribe"});

        if (userManagementClient2.userNameExists(INTERNAL_ROLE_SUBSCRIBER, TENANT_SUBSCRIBER_USERNAME)) {
            userManagementClient2.deleteUser(TENANT_SUBSCRIBER_USERNAME);
        }

        userManagementClient2.addUser(TENANT_SUBSCRIBER_USERNAME, String.valueOf(TENANT_SUBSCRIBER_PASSWORD),
                                      new String[]{INTERNAL_ROLE_SUBSCRIBER}, null);

        apiStoreClientSubscriberUserOtherDomain = new APIStoreRestClient(storeURLHttp);
        apiStoreClientSubscriberUserOtherDomain.login(TENANT_SUBSCRIBER_USERNAME,
                                                      String.valueOf(TENANT_SUBSCRIBER_PASSWORD));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Publisher for API creator ")
    public void testVisibilityForCreatorInPublisher() throws APIManagerIntegrationTestException,
                                                             MalformedURLException,
                                                             XPathExpressionException {
        apiIdentifierAdminVisibility =
                new APIIdentifier(providerName, API_NAME_ADMIN_VISIBILITY, API_VERSION_1_0_0);
        apiIdentifierSubscriberVisibility =
                new APIIdentifier(providerName, API_NAME_SUBSCRIBER_VISIBILITY, API_VERSION_1_0_0);
        //Create API  with public visibility and publish.
        APICreationRequestBean apiCreationReqBeanVisibilityAdmin =
                new APICreationRequestBean(API_NAME_ADMIN_VISIBILITY, API_CONTEXT1, API_VERSION_1_0_0,
                                           providerName, new URL(apiEndPointUrl));
        apiCreationReqBeanVisibilityAdmin.setTags(API_TAGS);
        apiCreationReqBeanVisibilityAdmin.setDescription(API_DESCRIPTION);
        apiCreationReqBeanVisibilityAdmin.setVisibility("restricted");
        apiCreationReqBeanVisibilityAdmin.setRoles("admin");
        apiPublisherClientCarbonSuperUser1.addAPI(apiCreationReqBeanVisibilityAdmin);

        publishAPI(apiIdentifierAdminVisibility, apiPublisherClientCarbonSuperUser1, false);

        waitForAPIDeployment();

        APICreationRequestBean apiCreationReqBeanVisibilityInternalSubscriber =
                new APICreationRequestBean(API_NAME_SUBSCRIBER_VISIBILITY, API_CONTEXT2, API_VERSION_1_0_0,
                                           providerName, new URL(apiEndPointUrl));
        apiCreationReqBeanVisibilityInternalSubscriber.setTags(API_TAGS);
        apiCreationReqBeanVisibilityInternalSubscriber.setDescription(API_DESCRIPTION);
        apiCreationReqBeanVisibilityInternalSubscriber.setVisibility("restricted");
        apiCreationReqBeanVisibilityInternalSubscriber.setRoles("Internal/subscriber");
        apiPublisherClientCarbonSuperUser1.addAPI(apiCreationReqBeanVisibilityInternalSubscriber);

        publishAPI(apiIdentifierSubscriberVisibility, apiPublisherClientCarbonSuperUser1, false);

        waitForAPIDeployment();

        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(
                        apiPublisherClientCarbonSuperUser1.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierAdminVisibility, apiPublisherAPIIdentifierList),
                   "API with  Role admin  visibility is not visible to creator in API Publisher." +
                   getAPIIdentifierString(apiIdentifierAdminVisibility));
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierSubscriberVisibility, apiPublisherAPIIdentifierList),
                   "API with  Role Internal/subscriber  visibility is not visible to creator in API Publisher." +
                   getAPIIdentifierString(apiIdentifierSubscriberVisibility));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Store for API creator",
          dependsOnMethods = "testVisibilityForCreatorInPublisher")
    public void testVisibilityForCreatorInStore() throws APIManagerIntegrationTestException {
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientCarbonSuperUser1.
                        getAllPublishedAPIs(apiCreatorStoreDomain));
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierAdminVisibility, apiStoreAPIIdentifierList),
                   "API with  Role admin  visibility is not visible to creator in API Store." +
                   getAPIIdentifierString(apiIdentifierAdminVisibility));
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierSubscriberVisibility, apiStoreAPIIdentifierList),
                   "API  with  Role Internal/subscriber  is not visible to creator in API Store. " +
                   getAPIIdentifierString(apiIdentifierSubscriberVisibility));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Publisher for admin in same domain ",
          dependsOnMethods = "testVisibilityForCreatorInStore")
    public void testVisibilityForAdminUserWithAdminAndSubscriberRoleInSameDomainInPublisher()
            throws APIManagerIntegrationTestException {
        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiPublisherClientCarbonSuperAdmin.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierAdminVisibility, apiPublisherAPIIdentifierList),
                   "API with  Role admin  visibility is not visible to Admin user with Admin and subscriber role in same" +
                   " domain in API Publisher." +
                   getAPIIdentifierString(apiIdentifierAdminVisibility));
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierSubscriberVisibility, apiPublisherAPIIdentifierList),
                   "API with  Role Internal/subscriber  visibility is not visible to Admin user with Admin and subscriber" +
                   " role in same domain  in API Publisher." +
                   getAPIIdentifierString(apiIdentifierSubscriberVisibility));
    }

    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Store for admin in same domain ",
          dependsOnMethods = "testVisibilityForAdminUserWithAdminAndSubscriberRoleInSameDomainInPublisher")
    public void testVisibilityForAdminUserWithAdminAndSubscriberRoleInSameDomainInStore()
            throws APIManagerIntegrationTestException {
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientCarbonSuperAdmin.
                        getAllPublishedAPIs(apiCreatorStoreDomain));

        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierAdminVisibility, apiStoreAPIIdentifierList),
                   "API with  Role admin  visibility is not visible to Admin user with Admin and subscriber role in same " +
                   "domain  in API Store." + getAPIIdentifierString(apiIdentifierAdminVisibility));
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierSubscriberVisibility, apiStoreAPIIdentifierList),
                   "API  with  Role Internal/subscriber  is not visible to Admin user with Admin and subscriber role in same " +
                   "domain  in API Store. " + getAPIIdentifierString(apiIdentifierSubscriberVisibility));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Publisher for another user in same domain",
          dependsOnMethods = "testVisibilityForAdminUserWithAdminAndSubscriberRoleInSameDomainInStore")
    public void testVisibilityForAnotherUserWithAdminAndSubscriberRoleInSameDomainInPublisher()
            throws
            APIManagerIntegrationTestException {
        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(
                        apiPublisherClientCarbonSuperUser2.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierAdminVisibility, apiPublisherAPIIdentifierList),
                   "API with  Role admin  visibility is not visible to another user with Admin and subscriber role in same " +
                   "domain  in API Publisher." +
                   getAPIIdentifierString(apiIdentifierAdminVisibility));
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierSubscriberVisibility, apiPublisherAPIIdentifierList),
                   "API with  Role Internal/subscriber  visibility is not visible to another user with Admin and subscriber " +
                   "role in same domain in API Publisher." +
                   getAPIIdentifierString(apiIdentifierSubscriberVisibility));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Store for another user in same domain",
          dependsOnMethods = "testVisibilityForAnotherUserWithAdminAndSubscriberRoleInSameDomainInPublisher")
    public void testVisibilityForAnotherUserWithAdminAndSubscriberRoleInSameDomainInStore() throws
                                                                                            APIManagerIntegrationTestException {
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientCarbonSuperUser2.
                        getAllPublishedAPIs(apiCreatorStoreDomain));
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierAdminVisibility, apiStoreAPIIdentifierList),
                   "API with  Role admin  visibility is not visible to another user with Admin and subscriber role in same" +
                   " domain in API Store." +
                   getAPIIdentifierString(apiIdentifierAdminVisibility));
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierSubscriberVisibility, apiStoreAPIIdentifierList),
                   "API  with  Role Internal/subscriber  is not visible to another user with Admin and subscriber role in" +
                   " same domain in API Store. " +
                   getAPIIdentifierString(apiIdentifierSubscriberVisibility));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Publisher for another user in other domain",
          dependsOnMethods = "testVisibilityForAnotherUserWithAdminAndSubscriberRoleInSameDomainInStore")
    public void testVisibilityForAnotherUserWithAdminAndSubscriberRoleInOtherDomainInPublisher()
            throws
            APIManagerIntegrationTestException {
        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(
                        apiPublisherClientAnotherUserOtherDomain.getAllAPIs());
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifierAdminVisibility, apiPublisherAPIIdentifierList),
                    "API with  Role admin  visibility is  visible to another user with Admin and subscriber role in other " +
                    "domain in API Publisher." +
                    getAPIIdentifierString(apiIdentifierAdminVisibility));
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifierSubscriberVisibility, apiPublisherAPIIdentifierList),
                    "API with  Role Internal/subscriber  visibility is  visible to another user with Admin and subscriber" +
                    " role in other domain in API Publisher." +
                    getAPIIdentifierString(apiIdentifierSubscriberVisibility));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Store for another user in other domain",
          dependsOnMethods = "testVisibilityForAnotherUserWithAdminAndSubscriberRoleInOtherDomainInPublisher")
    public void testVisibilityForAnotherUserWithAdminAndSubscriberRoleInOtherDomainInStore()
            throws APIManagerIntegrationTestException {
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientAnotherUserOtherDomain.
                        getAllPublishedAPIs(apiCreatorStoreDomain));
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifierAdminVisibility, apiStoreAPIIdentifierList),
                    "API with  Role admin  visibility is  visible to another user with Admin and subscriber role in other " +
                    "domain in API Store." +
                    getAPIIdentifierString(apiIdentifierAdminVisibility));
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifierSubscriberVisibility, apiStoreAPIIdentifierList),
                    "API  with  Role Internal/subscriber  is  visible to another user with Admin and subscriber role in other " +
                    "domain in API Store. " +
                    getAPIIdentifierString(apiIdentifierSubscriberVisibility));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Publisher for admin in other domain",
          dependsOnMethods = "testVisibilityForAnotherUserWithAdminAndSubscriberRoleInOtherDomainInStore")
    public void testVisibilityForAdminWithAdminAndSubscriberRoleInOtherDomainInPublisher() throws
                                                                                           APIManagerIntegrationTestException {
        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(
                        apiPublisherClientAdminOtherDomain.getAllAPIs());

        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifierAdminVisibility, apiPublisherAPIIdentifierList),
                    "API with  Role admin  visibility is  visible to Admin user with Admin and subscriber role in other " +
                    "domain in API Publisher." +
                    getAPIIdentifierString(apiIdentifierAdminVisibility));

        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifierSubscriberVisibility, apiPublisherAPIIdentifierList),
                    "API with  Role Internal/subscriber  visibility is  visible to Admin user with Admin and subscriber role" +
                    " in other domain in API Publisher." +
                    getAPIIdentifierString(apiIdentifierSubscriberVisibility));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Store for admin in other domain",
          dependsOnMethods = "testVisibilityForAdminWithAdminAndSubscriberRoleInOtherDomainInPublisher")
    public void testVisibilityForAdminWithAdminAndSubscriberRoleInOtherDomainInStore()
            throws APIManagerIntegrationTestException {
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientAdminOtherDomain.
                        getAllPublishedAPIs(apiCreatorStoreDomain));
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifierAdminVisibility, apiStoreAPIIdentifierList),
                    "API with  Role admin  visibility is  visible to Admin user with Admin and subscriber role in other " +
                    "domain in API Store." +
                    getAPIIdentifierString(apiIdentifierAdminVisibility));
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifierSubscriberVisibility, apiStoreAPIIdentifierList),
                    "API  with  Role Internal/subscriber  is  visible to Admin user with Admin and subscriber role in other " +
                    "domain in API Store. " +
                    getAPIIdentifierString(apiIdentifierSubscriberVisibility));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Store for another user in same domain",
          dependsOnMethods = "testVisibilityForAdminWithAdminAndSubscriberRoleInOtherDomainInStore")
    public void testVisibilityForAnotherUserWithSubscriberRoleInSameDomainInStore()
            throws APIManagerIntegrationTestException {
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientSubscriberUserSameDomain.
                        getAllPublishedAPIs(apiCreatorStoreDomain));
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifierAdminVisibility, apiStoreAPIIdentifierList),
                    "API with  Role admin  visibility is  visible to another user with subscriber role in same domain " +
                    "in API Store." + getAPIIdentifierString(apiIdentifierAdminVisibility));
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierSubscriberVisibility, apiStoreAPIIdentifierList),
                   "API  with  Role Internal/subscriber  is not visible to another user with subscriber role in same " +
                   "domain in API Store. " +
                   getAPIIdentifierString(apiIdentifierSubscriberVisibility));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Store for another user in same domain",
          dependsOnMethods = "testVisibilityForAnotherUserWithSubscriberRoleInSameDomainInStore")
    public void testVisibilityForAnotherUserWithSubscriberRoleInOtherDomainInStore()
            throws APIManagerIntegrationTestException {
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientSubscriberUserOtherDomain.
                        getAllPublishedAPIs(apiCreatorStoreDomain));

        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifierAdminVisibility, apiStoreAPIIdentifierList),
                    "API with  Role admin  visibility is  visible to another user with subscriber role in same domain " +
                    "in API Store." + getAPIIdentifierString(apiIdentifierAdminVisibility));

        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifierSubscriberVisibility, apiStoreAPIIdentifierList),
                    "API  with  Role Internal/subscriber  is  visible to another user with subscriber role in same domain " +
                    "in API Store. " +
                    getAPIIdentifierString(apiIdentifierSubscriberVisibility));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility for API in other domainStore for anonymous user",
          dependsOnMethods = "testVisibilityForAnotherUserWithSubscriberRoleInOtherDomainInStore")
    public void testVisibilityForAnonymousUserInOtherDomainInStore()
            throws APIManagerIntegrationTestException {

        HttpResponse httpResponse = new APIStoreRestClient(storeURLHttp).getAPIListFromStoreAsAnonymousUser
                (apiCreatorStoreDomain);

        assertFalse(httpResponse.getData().contains(API_NAME_ADMIN_VISIBILITY),
                    "API with  Role admin  visibility " +
                    " is  visible to anonymous user in other domain API Store." +
                    getAPIIdentifierString(apiIdentifierAdminVisibility));

        assertFalse(httpResponse.getData().contains(API_NAME_SUBSCRIBER_VISIBILITY),
                    "API with  Role " +
                    "Internal/subscriber is  visible to anonymous user in other domain API Store." +
                    getAPIIdentifierString(apiIdentifierSubscriberVisibility));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility for API in Same domainStore for anonymous user",
          dependsOnMethods = "testVisibilityForAnonymousUserInOtherDomainInStore")
    public void testVisibilityForAnonymousUserInSameDomainInStore()
            throws APIManagerIntegrationTestException {

        HttpResponse httpResponse = new APIStoreRestClient(storeURLHttp).getAPIListFromStoreAsAnonymousUser(
                otherDomain);

        assertFalse(httpResponse.getData().contains(API_NAME_ADMIN_VISIBILITY),
                    "API with  Role admin  " +
                    "visibility  is not visible to anonymous user in same domain API Store." +
                    getAPIIdentifierString(apiIdentifierAdminVisibility));

        assertFalse(httpResponse.getData().contains(API_NAME_SUBSCRIBER_VISIBILITY),
                    "API with  Role " +
                    "Internal/subscriber is not visible to anonymous user in same domain API Store. " +
                    getAPIIdentifierString(apiIdentifierSubscriberVisibility));
    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        deleteAPI(apiIdentifierAdminVisibility, apiPublisherClientCarbonSuperAdmin);
        deleteAPI(apiIdentifierSubscriberVisibility, apiPublisherClientCarbonSuperAdmin);
        userManagementClient1.deleteUser(CARBON_SUPER_SUBSCRIBER_USERNAME);
        userManagementClient2.deleteUser(TENANT_SUBSCRIBER_USERNAME);
    }

}