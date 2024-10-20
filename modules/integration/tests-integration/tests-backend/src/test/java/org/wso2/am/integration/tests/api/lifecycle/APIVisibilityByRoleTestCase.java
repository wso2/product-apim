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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.base.MultitenantConstants;
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
    private final String EMAIL_USER_KEY = "emailUser";
    private final String EMAIL_USER2_KEY = "emailUser2";
    private final String EMAIL_USER3_KEY = "emailUser3";
    private final String TENANT_DOMAIN_KEY = "wso2.com";
    private final String TENANT_DOMAIN_ADMIN_KEY = "admin";
    private final String USER_KEY_USER2 = "userKey1";
    private final String OTHER_DOMAIN_TENANT_USER_KEY = "user1";
    private final String CARBON_SUPER_SUBSCRIBER_USERNAME = "subscriberUser1";
    private final String CARBON_SUPER_SUBSCRIBER_PASSWORD = "password@123";
    private final String TENANT_SUBSCRIBER_USERNAME = "subscriberUser2";
    private final String TENANT_SUBSCRIBER_PASSWORD = "password@123";
    private final String INTERNAL_ROLE_SUBSCRIBER = "Internal/subscriber";
    private final String ROLE_SUBSCRIBER = "subscriber";
    private final String AT = "@";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String providerName;
    private String apiVisibilityByRoleTestId;
    private String apiVisibilityByRoleId;
    private RestAPIPublisherImpl apiPublisherClientCarbonSuperUser1;
    private RestAPIStoreImpl apiStoreClientCarbonSuperUser1;
    private RestAPIPublisherImpl apiPublisherClientCarbonSuperAdmin;
    private RestAPIStoreImpl apiStoreClientCarbonSuperAdmin;
    private APIIdentifier apiIdentifierAdminVisibility;
    private APIIdentifier apiIdentifierSubscriberVisibility;
    private RestAPIStoreImpl apiStoreClientCarbonSuperUser2;
    private RestAPIPublisherImpl apiPublisherClientCarbonSuperUser2;
    private RestAPIStoreImpl apiStoreClientAnotherUserOtherDomain;
    private RestAPIPublisherImpl apiPublisherClientAnotherUserOtherDomain;
    private RestAPIStoreImpl apiStoreClientAdminOtherDomain;
    private RestAPIPublisherImpl apiPublisherClientAdminOtherDomain;
    private UserManagementClient userManagementClient1;
    private UserManagementClient userManagementClient2;
    private RestAPIStoreImpl apiStoreClientSubscriberUserSameDomain;
    private RestAPIStoreImpl apiStoreClientSubscriberUserOtherDomain;
    private String apiCreatorStoreDomain;
    private String otherDomain;

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.SUPER_TENANT_EMAIL_USER },
        };
    }

    @Factory(dataProvider = "userModeDataProvider")
    public APIVisibilityByRoleTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        User user2;
        User user3;
        User otherTenantUser;
        String username2;
        String username3;
        //Creating CarbonSuper context
        super.init(userMode);
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        storeURLHttp = getStoreURLHttp();
        //Login to API Publisher and Store with CarbonSuper admin
        apiPublisherClientCarbonSuperAdmin = new RestAPIPublisherImpl(user.getUserNameWithoutDomain(),
                user.getPassword(), user.getUserDomain(), publisherURLHttps);
        apiStoreClientCarbonSuperAdmin = new RestAPIStoreImpl(user.getUserNameWithoutDomain(), user.getPassword(),
                user.getUserDomain(), storeURLHttps);
        apiCreatorStoreDomain = storeContext.getContextTenant().getDomain();

        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            //Login to API Publisher adn Store with CarbonSuper normal user1
            providerName = publisherContext.getContextTenant().getTenantUser(USER_KEY_USER2).getUserName();

            user2 = publisherContext.getContextTenant().getTenantUser(USER_KEY_USER2);
            user3 = publisherContext.getContextTenant().getTenantUser(CARBON_SUPER_TENANT2_KEY);
            username2 = user2.getUserNameWithoutDomain();
            username3 = user3.getUserNameWithoutDomain();

        } else {
            //Login to API Publisher adn Store with CarbonSuper normal user1
            providerName = publisherContext.getContextTenant().getTenantUser(EMAIL_USER2_KEY).getUserName() + AT
                    + apiCreatorStoreDomain;

            user2 = publisherContext.getContextTenant().getTenantUser(EMAIL_USER2_KEY);
            user3 = publisherContext.getContextTenant().getTenantUser(EMAIL_USER3_KEY);
            username2 = user2.getUserName();
            username3 = user3.getUserName();

        }

        apiPublisherClientCarbonSuperUser1 = new RestAPIPublisherImpl(username2, user2.getPassword(),
                apiCreatorStoreDomain, publisherURLHttps);

        apiStoreClientCarbonSuperUser1 = new RestAPIStoreImpl(username2, user2.getPassword(), apiCreatorStoreDomain,
                storeURLHttps);

        apiStoreClientCarbonSuperUser2 = new RestAPIStoreImpl(username3, user3.getPassword(), apiCreatorStoreDomain,
                storeURLHttps);

        apiPublisherClientCarbonSuperUser2 = new RestAPIPublisherImpl(username3, user3.getPassword(),
                apiCreatorStoreDomain, publisherURLHttps);

        // create new user in CarbonSuper with only subscriber role and login to the Store
        userManagementClient1 =
                new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                        keyManagerContext.getContextTenant().getTenantAdmin().getUserName(),
                        keyManagerContext.getContextTenant().getTenantAdmin().getPassword());

        if (userManagementClient1.userNameExists(INTERNAL_ROLE_SUBSCRIBER, CARBON_SUPER_SUBSCRIBER_USERNAME)) {
            userManagementClient1.deleteUser(CARBON_SUPER_SUBSCRIBER_USERNAME);
        }

        userManagementClient1.addUser(CARBON_SUPER_SUBSCRIBER_USERNAME, CARBON_SUPER_SUBSCRIBER_PASSWORD,
                                      new String[]{INTERNAL_ROLE_SUBSCRIBER}, null);

        apiStoreClientSubscriberUserSameDomain = new RestAPIStoreImpl(CARBON_SUPER_SUBSCRIBER_USERNAME,
                CARBON_SUPER_SUBSCRIBER_PASSWORD, MultitenantConstants.SUPER_TENANT_DOMAIN_NAME,
                storeURLHttps);

        //Creating Tenant contexts
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            init(TENANT_DOMAIN_KEY, TENANT_DOMAIN_ADMIN_KEY);
            otherTenantUser = publisherContext.getContextTenant().getTenantUser(OTHER_DOMAIN_TENANT_USER_KEY);
        } else {
            init(TENANT_DOMAIN_KEY, EMAIL_USER_KEY);
            otherTenantUser = publisherContext.getContextTenant().getTenantUser(EMAIL_USER2_KEY);
        }

        otherDomain = storeContext.getContextTenant().getDomain();
        apiStoreClientAnotherUserOtherDomain = new RestAPIStoreImpl(otherTenantUser.getUserNameWithoutDomain(),
                otherTenantUser.getPassword(), otherDomain,
                storeURLHttps);
        apiPublisherClientAnotherUserOtherDomain = new RestAPIPublisherImpl(
                otherTenantUser.getUserNameWithoutDomain(), otherTenantUser.getPassword(),
                otherDomain, publisherURLHttps);

        //Login to the API Publisher adn Store as Tenant admin
        apiStoreClientAdminOtherDomain = new RestAPIStoreImpl(
                storeContext.getContextTenant().getContextUser().getUserNameWithoutDomain(),
                storeContext.getContextTenant().getContextUser().getPassword(),
                storeContext.getContextTenant().getContextUser().getUserDomain(), storeURLHttps);
        apiPublisherClientAdminOtherDomain = new RestAPIPublisherImpl(
                publisherContext.getContextTenant().getContextUser().getUserNameWithoutDomain(),
                publisherContext.getContextTenant().getContextUser().getPassword(),
                publisherContext.getContextTenant().getContextUser().getUserDomain(), storeURLHttps);

        // create new user in tenant with only subscriber role and login to the Store
        userManagementClient2 = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                keyManagerContext.getContextTenant().getTenantAdmin().getUserName(),
                keyManagerContext.getContextTenant().getTenantAdmin().getPassword());

        if (userManagementClient2.userNameExists(INTERNAL_ROLE_SUBSCRIBER, TENANT_SUBSCRIBER_USERNAME)) {
            userManagementClient2.deleteUser(TENANT_SUBSCRIBER_USERNAME);
        }

        userManagementClient2.addUser(TENANT_SUBSCRIBER_USERNAME, TENANT_SUBSCRIBER_PASSWORD,
                                      new String[]{INTERNAL_ROLE_SUBSCRIBER}, null);

        apiStoreClientSubscriberUserOtherDomain = new RestAPIStoreImpl(TENANT_SUBSCRIBER_USERNAME,
                TENANT_SUBSCRIBER_PASSWORD, TENANT_DOMAIN_KEY,
                storeURLHttps);

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Publisher for API creator ")
    public void testVisibilityForCreatorInPublisher()
            throws APIManagerIntegrationTestException, MalformedURLException, XPathExpressionException, ApiException {

        // This testCase will test the visibility of API in Publisher for admin in same domain"
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
        apiCreationReqBeanVisibilityAdmin.setVisibility(APIDTO.VisibilityEnum.RESTRICTED.getValue());
        apiCreationReqBeanVisibilityAdmin.setRoles("admin");
        APIDTO apiDto = apiPublisherClientCarbonSuperUser1.addAPI(apiCreationReqBeanVisibilityAdmin);
        apiVisibilityByRoleTestId = apiDto.getId();

        publishAPI(apiVisibilityByRoleTestId, apiPublisherClientCarbonSuperUser1, false);

        waitForAPIDeployment();

        APICreationRequestBean apiCreationReqBeanVisibilityInternalSubscriber =
                new APICreationRequestBean(API_NAME_SUBSCRIBER_VISIBILITY, API_CONTEXT2, API_VERSION_1_0_0,
                                           providerName, new URL(apiEndPointUrl));
        apiCreationReqBeanVisibilityInternalSubscriber.setTags(API_TAGS);
        apiCreationReqBeanVisibilityInternalSubscriber.setDescription(API_DESCRIPTION);
        apiCreationReqBeanVisibilityInternalSubscriber.setVisibility(APIDTO.VisibilityEnum.RESTRICTED.getValue());
        apiCreationReqBeanVisibilityInternalSubscriber.setRoles("Internal/subscriber");
        apiDto = apiPublisherClientCarbonSuperUser1.addAPI(apiCreationReqBeanVisibilityInternalSubscriber);
        apiVisibilityByRoleId = apiDto.getId();

        publishAPI(apiVisibilityByRoleId, apiPublisherClientCarbonSuperUser1, false);

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
    public void testVisibilityForCreatorInStore()
            throws APIManagerIntegrationTestException, org.wso2.am.integration.clients.store.api.ApiException {
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientCarbonSuperUser1.
                        getAllAPIs(apiCreatorStoreDomain));
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierAdminVisibility, apiStoreAPIIdentifierList),
                   "API with  Role admin  visibility is not visible to creator in API Store." +
                   getAPIIdentifierString(apiIdentifierAdminVisibility));
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifierSubscriberVisibility, apiStoreAPIIdentifierList),
                   "API  with  Role Internal/subscriber  is not visible to creator in API Store. " +
                   getAPIIdentifierString(apiIdentifierSubscriberVisibility));

    }

    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Store for admin in same domain ",
          dependsOnMethods = "testVisibilityForCreatorInPublisher")
    public void testVisibilityForAdminUserWithAdminAndSubscriberRoleInSameDomainInStore()
            throws APIManagerIntegrationTestException, org.wso2.am.integration.clients.store.api.ApiException {
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientCarbonSuperAdmin.
                        getAllAPIs(apiCreatorStoreDomain));

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
            throws APIManagerIntegrationTestException, ApiException {
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
    public void testVisibilityForAnotherUserWithAdminAndSubscriberRoleInSameDomainInStore()
            throws APIManagerIntegrationTestException, org.wso2.am.integration.clients.store.api.ApiException {
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientCarbonSuperUser2.
                        getAllAPIs(apiCreatorStoreDomain));
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
            throws APIManagerIntegrationTestException, ApiException {
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
            throws APIManagerIntegrationTestException, org.wso2.am.integration.clients.store.api.ApiException {
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientAnotherUserOtherDomain.
                        getAllAPIs());
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
    public void testVisibilityForAdminWithAdminAndSubscriberRoleInOtherDomainInPublisher()
            throws APIManagerIntegrationTestException, ApiException {
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
            throws APIManagerIntegrationTestException, org.wso2.am.integration.clients.store.api.ApiException {
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientAdminOtherDomain.
                        getAllAPIs());
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
            throws APIManagerIntegrationTestException, org.wso2.am.integration.clients.store.api.ApiException {
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientSubscriberUserSameDomain.
                        getAllAPIs());
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
            throws APIManagerIntegrationTestException, org.wso2.am.integration.clients.store.api.ApiException {
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientSubscriberUserOtherDomain.
                        getAllAPIs());

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
            throws APIManagerIntegrationTestException, org.wso2.am.integration.clients.store.api.ApiException {

        List<APIIdentifier> apiStoreAPIIdentifierList = APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(
                apiStoreClientAdminOtherDomain.getAPIListFromStoreAsAnonymousUser(apiCreatorStoreDomain));
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifierAdminVisibility, apiStoreAPIIdentifierList),
                "API is  visible to admin in other domain in API Store. When Visibility is public. "
                        + getAPIIdentifierString(apiIdentifierAdminVisibility));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility for API in Same domainStore for anonymous user",
          dependsOnMethods = "testVisibilityForAnonymousUserInOtherDomainInStore")
    public void testVisibilityForAnonymousUserInSameDomainInStore()
            throws APIManagerIntegrationTestException, org.wso2.am.integration.clients.store.api.ApiException {

        List<APIIdentifier> apiStoreAPIIdentifierList = APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(
                apiStoreClientAdminOtherDomain.getAPIListFromStoreAsAnonymousUser(otherDomain));


        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifierAdminVisibility, apiStoreAPIIdentifierList),
                "API is  visible to admin in other domain in API Store. When Visibility is public. "
                        + getAPIIdentifierString(apiIdentifierAdminVisibility));
    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        deleteAPI(apiVisibilityByRoleTestId, apiPublisherClientCarbonSuperAdmin);
        deleteAPI(apiVisibilityByRoleId, apiPublisherClientCarbonSuperAdmin);
        userManagementClient1.deleteUser(CARBON_SUPER_SUBSCRIBER_USERNAME);
        userManagementClient2.deleteUser(TENANT_SUBSCRIBER_USERNAME);
    }

}