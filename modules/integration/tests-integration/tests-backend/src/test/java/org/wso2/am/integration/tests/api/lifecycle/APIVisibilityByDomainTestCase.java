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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.net.URL;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Create a API with domain visibility and check the visibility in Publisher Store.
 */
public class APIVisibilityByDomainTestCase extends APIManagerLifecycleBaseTest {

    private final String API_NAME = "APIVisibilityByDomainTest";
    private final String API_CONTEXT = "APIVisibilityByDomain";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String EMAIL_USER_KEY = "emailUser";
    private final String EMAIL_USER2_KEY = "emailUser2";
    private final String EMAIL_USER3_KEY = "emailUser3";
    private final String TENANT_DOMAIN_KEY = "wso2.com";
    private final String TENANT_DOMAIN_ADMIN_KEY = "admin";
    private final String USER_KEY_USER2 = "userKey1";
    private final String CARBON_SUPER_TENANT2_KEY = "userKey2";
    private final String OTHER_DOMAIN_TENANT_USER_KEY = "user1";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private final String AT = "@";
    private String apiEndPointUrl;
    private APIIdentifier apiIdentifier;
    private String providerName;
    private String otherDomain;
    private RestAPIPublisherImpl restAPIPublisherCarbonSuperAdmin;
    private RestAPIStoreImpl restAPIStoreCarbonSuperAdmin;
    private RestAPIPublisherImpl restAPIPublisherCarbonSuperUser1;
    private RestAPIStoreImpl restAPIStoreCarbonSuperUser1;
    private RestAPIPublisherImpl restAPIPublisherCarbonSuperUser2;
    private RestAPIStoreImpl restAPIStoreCarbonSuperUser2;
    private RestAPIPublisherImpl restAPIPublisherOtherDomainUser;
    private RestAPIStoreImpl restAPIStoreOtherDomainUser;
    private RestAPIPublisherImpl restAPIPublisherOtherDomainAdmin;
    private RestAPIStoreImpl restAPIStoreOtherDomainAdmin;
    private String apiID;
    private String apiCreatorStoreDomain;
    private User user2;
    private User user3;
    private User otherDomainUser2;

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.SUPER_TENANT_EMAIL_USER },
        };
    }

    @Factory(dataProvider = "userModeDataProvider")
    public APIVisibilityByDomainTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        String username2;
        String username3;
        //Creating CarbonSuper context
        super.init(userMode);
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        storeURLHttp = getStoreURLHttp();
        //Login to API Publisher and Store with CarbonSuper admin
        restAPIPublisherCarbonSuperAdmin = new RestAPIPublisherImpl(user.getUserNameWithoutDomain(), user.getPassword(),
                user.getUserDomain(), publisherURLHttps);

        restAPIStoreCarbonSuperAdmin = new RestAPIStoreImpl(user.getUserNameWithoutDomain(), user.getPassword(),
                user.getUserDomain(), storeURLHttps);

        apiCreatorStoreDomain = storeContext.getContextTenant().getDomain();

        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            user2 = publisherContext.getContextTenant().getTenantUser(USER_KEY_USER2);
            user3 = publisherContext.getContextTenant().getTenantUser(CARBON_SUPER_TENANT2_KEY);
            username2 = user2.getUserNameWithoutDomain();
            username3 = user3.getUserNameWithoutDomain();
            providerName = publisherContext.getContextTenant().getTenantUser(USER_KEY_USER2).getUserName();

        } else {
            user2 = publisherContext.getContextTenant().getTenantUser(EMAIL_USER2_KEY);
            user3 = publisherContext.getContextTenant().getTenantUser(EMAIL_USER3_KEY);
            username2 = user2.getUserName();
            username3 = user3.getUserName();

            providerName = publisherContext.getContextTenant().getTenantUser(EMAIL_USER2_KEY).getUserName() + AT
                    + apiCreatorStoreDomain;
        }

        //Login to API Publisher adn Store with CarbonSuper normal user1
        restAPIPublisherCarbonSuperUser1 = new RestAPIPublisherImpl(username2, user2.getPassword(),
                MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, publisherURLHttps);

        restAPIStoreCarbonSuperUser1 = new RestAPIStoreImpl(username2, user2.getPassword(),
                MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, storeURLHttps);

        //Login to API Publisher and Store with CarbonSuper normal user2
        restAPIPublisherCarbonSuperUser2 = new RestAPIPublisherImpl(username3, user3.getPassword(),
                MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, publisherURLHttps);

        restAPIStoreCarbonSuperUser2 = new RestAPIStoreImpl(username3, user3.getPassword(),
                MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, storeURLHttps);

        //Creating Tenant contexts
        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            init(TENANT_DOMAIN_KEY, TENANT_DOMAIN_ADMIN_KEY);
            otherDomainUser2 = storeContext.getContextTenant().getTenantUser(OTHER_DOMAIN_TENANT_USER_KEY);
        } else {
            init(TENANT_DOMAIN_KEY, EMAIL_USER_KEY);
            otherDomainUser2 = storeContext.getContextTenant().getTenantUser(EMAIL_USER2_KEY);
        }

        otherDomain = storeContext.getContextTenant().getDomain();

        //Login to the API Publisher and Store as Tenant user
        restAPIPublisherOtherDomainUser = new RestAPIPublisherImpl(otherDomainUser2.getUserNameWithoutDomain(),
                otherDomainUser2.getPassword(), otherDomain, publisherURLHttps);

        restAPIStoreOtherDomainUser = new RestAPIStoreImpl(otherDomainUser2.getUserNameWithoutDomain(),
                otherDomainUser2.getPassword(), otherDomain, storeURLHttps);

        //Login to the API Publisher adn Store as Tenant admin
        restAPIPublisherOtherDomainAdmin = new RestAPIPublisherImpl(
                storeContext.getContextTenant().getContextUser().getUserNameWithoutDomain(),
                storeContext.getContextTenant().getContextUser().getPassword(), otherDomain, publisherURLHttps);

        restAPIStoreOtherDomainAdmin = new RestAPIStoreImpl(
                storeContext.getContextTenant().getContextUser().getUserNameWithoutDomain(),
                storeContext.getContextTenant().getContextUser().getPassword(), otherDomain, storeURLHttps);
    }

    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Publisher for API creator ")
    public void testVisibilityForCreatorInPublisher() throws Exception {
        //Create API  with private visibility and publish.
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);

        APIRequest apiCreationRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));
        apiCreationRequest.setProvider(providerName);
        apiCreationRequest.setVersion(API_VERSION_1_0_0);
        apiCreationRequest.setTags(API_TAGS);
        apiCreationRequest.setDescription(API_DESCRIPTION);
        apiCreationRequest.setVisibility("PRIVATE");

        APIDTO apiAddResponse = restAPIPublisherCarbonSuperUser1.addAPI(apiCreationRequest, "v3");
        apiID = apiAddResponse.getId();

        APIIdentifier apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        HttpResponse apiPublishResponse = restAPIPublisherCarbonSuperUser1.changeAPILifeCycleStatus(apiID,
                APILifeCycleAction.PUBLISH.getAction(), null);

        waitForAPIDeployment();

        APIListDTO getAllApisResponse = restAPIPublisherCarbonSuperUser1
                .getAllAPIs();
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, getAllApisResponse),
                "API is not visible to creator in APi Publisher. When Visibility is private. ");
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Store for API creator",
            dependsOnMethods = "testVisibilityForCreatorInPublisher")
    public void testVisibilityForCreatorInStore() throws APIManagerIntegrationTestException {
        org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO apiListDTO =
                restAPIStoreCarbonSuperUser1.getAllPublishedAPIs();
        assertTrue(APIMTestCaseUtils.isAPIAvailableInStore(apiIdentifier, apiListDTO),
                "API is not visible to creator in API Store. When Visibility is private. " +
                        getAPIIdentifierString(apiIdentifier));
    }

    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Publisher for admin in same domain ",
            dependsOnMethods = "testVisibilityForCreatorInStore")
    public void testVisibilityForAdminInSameDomainInPublisher() throws APIManagerIntegrationTestException,
            ApiException {
        APIListDTO getAllApisResponse = restAPIPublisherCarbonSuperAdmin
                .getAllAPIs();
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, getAllApisResponse),
                "API is not visible to admin in same domain in API Publisher. When Visibility is private. " +
                        getAPIIdentifierString(apiIdentifier));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Store for admin in same domain ",
            dependsOnMethods = "testVisibilityForAdminInSameDomainInPublisher")
    public void testVisibilityForAdminInSameDomainInStore() throws APIManagerIntegrationTestException {
        org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO apiListDTO =
                restAPIStoreCarbonSuperAdmin.getAllPublishedAPIs();
        assertTrue(APIMTestCaseUtils.isAPIAvailableInStore(apiIdentifier, apiListDTO),
                "API is not visible to admin in same domain in API Store. When Visibility is private. " +
                        getAPIIdentifierString(apiIdentifier));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Publisher for another user in same domain",
            dependsOnMethods = "testVisibilityForAdminInSameDomainInStore")
    public void testVisibilityForAnotherUserInSameDomainInPublisher() throws APIManagerIntegrationTestException,
            ApiException {
        APIListDTO getAllApisResponse = restAPIPublisherCarbonSuperUser2
                .getAllAPIs();
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, getAllApisResponse),
                "API is not visible to another user in same domain in API Publisher. When Visibility is private." +
                        getAPIIdentifierString(apiIdentifier));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Store for another user in same domain",
            dependsOnMethods = "testVisibilityForAnotherUserInSameDomainInPublisher")
    public void testVisibilityForAnotherUserInSameDomainInStore() throws APIManagerIntegrationTestException {
        org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO apiListDTO =
                restAPIStoreCarbonSuperUser2.getAllPublishedAPIs();
        assertTrue(APIMTestCaseUtils.isAPIAvailableInStore(apiIdentifier, apiListDTO),
                "API is not visible to another user in same domain in API Store. When Visibility is private." +
                        getAPIIdentifierString(apiIdentifier));
    }

    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Publisher for another user in other domain",
            dependsOnMethods = "testVisibilityForAnotherUserInSameDomainInStore")
    public void testVisibilityForAnotherUserInOtherDomainInPublisher() throws APIManagerIntegrationTestException,
            ApiException {
        APIListDTO getAllApisResponse = restAPIPublisherOtherDomainUser
                .getAllAPIs();
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, getAllApisResponse),
                "API is  visible to another user in other domain in API Publisher. When Visibility is private."
                        + getAPIIdentifierString(apiIdentifier));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Store for another user in other domain",
            dependsOnMethods = "testVisibilityForAnotherUserInOtherDomainInPublisher")
    public void testVisibilityForAnotherUserInOtherDomainInStore() throws APIManagerIntegrationTestException {
        org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO apiListDTO =
                restAPIStoreOtherDomainUser.getAllPublishedAPIs();
        assertFalse(APIMTestCaseUtils.isAPIAvailableInStore(apiIdentifier, apiListDTO),
                "API is  visible to another user in other domain in API Store. When Visibility is private. " +
                        getAPIIdentifierString(apiIdentifier));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Publisher for admin in other domain",
            dependsOnMethods = "testVisibilityForAnotherUserInOtherDomainInStore")
    public void testVisibilityForAdminInOtherDomainInPublisher() throws APIManagerIntegrationTestException,
            ApiException {
        APIListDTO getAllApisResponse = restAPIPublisherOtherDomainAdmin
                .getAllAPIs();
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, getAllApisResponse),
                "API is  visible to admin in other domain in API Publisher. When Visibility is private. " +
                        getAPIIdentifierString(apiIdentifier));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in Store for admin in other domain",
            dependsOnMethods = "testVisibilityForAdminInOtherDomainInPublisher")
    public void testVisibilityForAdminInOtherDomainInStore() throws APIManagerIntegrationTestException {
        org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO apiListDTO =
                restAPIStoreOtherDomainAdmin.getAllPublishedAPIs();
        assertFalse(APIMTestCaseUtils.isAPIAvailableInStore(apiIdentifier, apiListDTO),
                "API is  visible to admin in other domain in API Store. When Visibility is private. " +
                        getAPIIdentifierString(apiIdentifier));
    }

    //https://wso2.org/jira/browse/APIMANAGER-4080
    @Test(groups = {"wso2.am"}, description = "Test the visibility for API in Same domainStore for anonymous user",
            dependsOnMethods = "testVisibilityForAdminInOtherDomainInStore")
    public void testVisibilityForAnonymousUserInSameDomainInStore() throws Exception {
        org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO apiListDTO =
                restAPIStoreCarbonSuperAdmin
                        .getAPIListFromStoreAsAnonymousUser(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
        assertFalse(APIMTestCaseUtils.isAPIAvailableInStore(apiIdentifier, apiListDTO),
                "API is  visible to anonymous in same domain in API Store. When Visibility is private. " +
                        getAPIIdentifierString(apiIdentifier));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility for API in other domainStore for anonymous user",
            dependsOnMethods = "testVisibilityForAnonymousUserInSameDomainInStore")
    public void testVisibilityForAnonymousUserInOtherDomainInStore() throws Exception {
        org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO apiListDTO =
                restAPIStoreOtherDomainAdmin.getAPIListFromStoreAsAnonymousUser(otherDomain);
        assertFalse(APIMTestCaseUtils.isAPIAvailableInStore(apiIdentifier, apiListDTO),
                "API is visible to anonymous user in other " +
                        "domain API Store. When Visibility is private. " + getAPIIdentifierString(apiIdentifier));
    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException {
        deleteAPI(apiID, apiIdentifier, restAPIPublisherCarbonSuperUser1);
    }
}
