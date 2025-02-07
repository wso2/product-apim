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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

import java.net.URL;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Create a API with public visibility and check the visibility in Publisher Store.
 */
public class APIVisibilityByPublicTestCase extends APIManagerLifecycleBaseTest {
    private final Log log = LogFactory.getLog(APIVisibilityByPublicTestCase.class);
    private final String API_NAME = "APIVisibilityByPublicTest";
    private final String API_CONTEXT = "APIVisibilityByPublic";
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
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private final String AT = "@";
    private String apiEndPointUrl;
    private APIIdentifier apiIdentifier;
    private String apiID;
    private RestAPIStoreImpl apiStoreClientAnotherUserSameDomain;
    private RestAPIPublisherImpl apiPublisherClientUserAnotherUserSameDomain;
    private RestAPIStoreImpl apiStoreClientAnotherUserOtherDomain;
    private RestAPIPublisherImpl apiPublisherClientAnotherUserOtherDomain;
    private RestAPIStoreImpl apiStoreClientAdminOtherDomain;
    private RestAPIPublisherImpl apiPublisherClientAdminOtherDomain;
    private String providerName;
    private RestAPIPublisherImpl apiPublisherClientUser2;
    private RestAPIStoreImpl apiStoreClientUser2;
    private RestAPIPublisherImpl apiPublisherClientUser1;
    private RestAPIStoreImpl apiStoreClientUser1;
    private String otherDomain;
    private String apiCreatorStoreDomain;
    private String storeURLHttp;
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
    public APIVisibilityByPublicTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException, XPathExpressionException {
        String username2;
        String username3;
        //Creating CarbonSuper context
        super.init(userMode);
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        storeURLHttp = getStoreURLHttp();

        //Login to API Publisher and Store with CarbonSuper admin
        apiPublisherClientUser1 =
                new RestAPIPublisherImpl(user.getUserNameWithoutDomain(), user.getPassword(), user.getUserDomain()
                        , publisherURLHttps);
        apiStoreClientUser1 =
                new RestAPIStoreImpl(user.getUserNameWithoutDomain(), user.getPassword(), user.getUserDomain(),
                        storeURLHttps);

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
        apiPublisherClientUser2 = new RestAPIPublisherImpl(username2, user2.getPassword(), apiCreatorStoreDomain,
                publisherURLHttps);
        apiStoreClientUser2 = new RestAPIStoreImpl(username2, user2.getPassword(), apiCreatorStoreDomain,
                storeURLHttps);

        //Login to API Publisher adn Store with CarbonSuper normal user2
        apiStoreClientAnotherUserSameDomain = new RestAPIStoreImpl(username3, user3.getPassword(),
                apiCreatorStoreDomain, storeURLHttps);
        apiPublisherClientUserAnotherUserSameDomain = new RestAPIPublisherImpl(username3, user3.getPassword(),
                apiCreatorStoreDomain, publisherURLHttps);

        if (TestUserMode.SUPER_TENANT_ADMIN.equals(userMode)) {
            init(TENANT_DOMAIN_KEY, TENANT_DOMAIN_ADMIN_KEY);
            otherDomainUser2 = storeContext.getContextTenant().getTenantUser(OTHER_DOMAIN_TENANT_USER_KEY);
        } else {
            init(TENANT_DOMAIN_KEY, EMAIL_USER_KEY);
            otherDomainUser2 = storeContext.getContextTenant().getTenantUser(EMAIL_USER2_KEY);
        }

        otherDomain = storeContext.getContextTenant().getDomain();
        //Login to the API Publisher adn Store as Tenant user
        apiStoreClientAnotherUserOtherDomain = new RestAPIStoreImpl(otherDomainUser2.getUserNameWithoutDomain(),
                otherDomainUser2.getPassword(), otherDomain, publisherURLHttps);
        apiPublisherClientAnotherUserOtherDomain = new RestAPIPublisherImpl(otherDomainUser2.getUserNameWithoutDomain(),
                otherDomainUser2.getPassword(), otherDomain, publisherURLHttps);

        //Login to the API Publisher and Store as Tenant admin
        apiStoreClientAdminOtherDomain =
                new RestAPIStoreImpl(storeContext.getContextTenant().getContextUser().getUserNameWithoutDomain(),
                        storeContext.getContextTenant().getContextUser().getPassword(),
                        storeContext.getContextTenant().getContextUser().getUserDomain(), publisherURLHttps
                );
        apiPublisherClientAdminOtherDomain = new RestAPIPublisherImpl(
                publisherContext.getContextTenant().getContextUser().getUserNameWithoutDomain(),
                publisherContext.getContextTenant().getContextUser().getPassword(),
                publisherContext.getContextTenant().getContextUser().getUserDomain(), publisherURLHttps);
    }

    @Test(groups = { "wso2.am" }, description = "Test the visibility of API in Publisher for API creator ")
    public void testVisibilityForCreatorInPublisher() throws Exception {
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                        new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiCreationRequestBean.setVisibility(APIDTO.VisibilityEnum.PUBLIC.getValue());
        //Create API  with public visibility and publish.
        APIDTO apidto = apiPublisherClientUser2.addAPI(apiCreationRequestBean);
        apiID = apidto.getId();

        publishAPI(apiID, apiPublisherClientUser2, false);

        int retry = 0;
        while (retry < 15) {
            List<APIIdentifier> apiStoreAPIIdentifierList =
                    APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientUser2.getAllAPIs());
            if (APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList)) {
                log.info("API '" + API_NAME + "' is available on store.");
                break;
            }
            log.info("Waiting for API '" + API_NAME + "' is available on store.");
            Thread.sleep(1000);
            retry++;
        }

        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiPublisherClientUser2.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(this.apiIdentifier, apiPublisherAPIIdentifierList),
                "API is not visible to creator in APi Publisher. When Visibility is public. " + getAPIIdentifierString(
                        this.apiIdentifier));
    }

    @Test(groups = { "wso2.am" }, description = "Test the visibility of API in Store for API creator",
            dependsOnMethods = "testVisibilityForCreatorInPublisher")
    public void testVisibilityForCreatorInStore() throws Exception {
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientUser2.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList),
                "API is not visible to creator in API Store. When Visibility is public. " + getAPIIdentifierString(
                        apiIdentifier));
    }

    @Test(groups = { "wso2.am" }, description = "Test the visibility of API in Publisher for admin in same domain ",
            dependsOnMethods = "testVisibilityForCreatorInStore")
    public void testVisibilityForAdminInSameDomainInPublisher()
            throws APIManagerIntegrationTestException, ApiException {
        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiPublisherClientUser1.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiPublisherAPIIdentifierList),
                "API is not visible to admin in same domain in API Publisher. When Visibility is public. "
                        + getAPIIdentifierString(apiIdentifier));
    }

    @Test(groups = { "wso2.am" }, description = "Test the visibility of API in Store for admin in same domain ",
            dependsOnMethods = "testVisibilityForAdminInSameDomainInPublisher")
    public void testVisibilityForAdminInSameDomainInStore() throws Exception {

        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientUser1.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList),
                "API is not visible to admin in same domain in API Store. When Visibility is public. "
                        + getAPIIdentifierString(apiIdentifier));

    }

    @Test(groups = { "wso2.am" },
            description = "Test the visibility of API in Publisher for another user in same domain",
            dependsOnMethods = "testVisibilityForAdminInSameDomainInStore")
    public void testVisibilityForAnotherUserInSameDomainInPublisher()
            throws APIManagerIntegrationTestException, ApiException {
        List<APIIdentifier> apiPublisherAPIIdentifierList = APIMTestCaseUtils
                .getAPIIdentifierListFromHttpResponse(apiPublisherClientUserAnotherUserSameDomain.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiPublisherAPIIdentifierList),
                "API is not visible to another user in same domain in API Publisher. When Visibility is public."
                        + getAPIIdentifierString(apiIdentifier));
    }

    @Test(groups = { "wso2.am" }, description = "Test the visibility of API in Store for another user in same domain",
            dependsOnMethods = "testVisibilityForAnotherUserInSameDomainInPublisher")
    public void testVisibilityForAnotherUserInSameDomainInStore() throws Exception {
        List<APIIdentifier> apiStoreAPIIdentifierList = APIMTestCaseUtils
                .getAPIIdentifierListFromHttpResponse(apiStoreClientAnotherUserSameDomain.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList),
                "API is not visible to another user in same domain in API Store. When Visibility is public."
                        + getAPIIdentifierString(apiIdentifier));
    }

    @Test(groups = { "wso2.am" },
            description = "Test the visibility of API in Publisher for another user in other domain",
            dependsOnMethods = "testVisibilityForAnotherUserInSameDomainInStore")
    public void testVisibilityForAnotherUserInOtherDomainInPublisher()
            throws APIManagerIntegrationTestException, ApiException {
        List<APIIdentifier> apiPublisherAPIIdentifierList = APIMTestCaseUtils
                .getAPIIdentifierListFromHttpResponse(apiPublisherClientAnotherUserOtherDomain.getAllAPIs());
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiPublisherAPIIdentifierList),
                "API is  visible to another user in other domain in API Publisher. When Visibility is public."
                        + getAPIIdentifierString(apiIdentifier));
    }

    @Test(groups = { "wso2.am" }, description = "Test the visibility of API in Store for another user in other domain",
            dependsOnMethods = "testVisibilityForAnotherUserInOtherDomainInPublisher")
    public void testVisibilityForAnotherUserInOtherDomainInStore() throws Exception {
        List<APIIdentifier> apiStoreAPIIdentifierList = APIMTestCaseUtils
                .getAPIIdentifierListFromHttpResponse(apiStoreClientAnotherUserOtherDomain.getAllAPIs());
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList),
                "API is  visible to another user in other domain in API Store. When Visibility is public. "
                        + getAPIIdentifierString(apiIdentifier));
    }

    @Test(groups = { "wso2.am" }, description = "Test the visibility of API in Publisher for admin in other domain",
            dependsOnMethods = "testVisibilityForAnotherUserInOtherDomainInStore")
    public void testVisibilityForAdminInOtherDomainInPublisher()
            throws APIManagerIntegrationTestException, ApiException {
        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiPublisherClientAdminOtherDomain.getAllAPIs());
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiPublisherAPIIdentifierList),
                "API is  visible to admin in other domain in API Publisher. When Visibility is public. "
                        + getAPIIdentifierString(apiIdentifier));
    }

    @Test(groups = { "wso2.am" }, description = "Test the visibility of API in Store for admin in other domain",
            dependsOnMethods = "testVisibilityForAdminInOtherDomainInPublisher")
    public void testVisibilityForAdminInOtherDomainInStore() throws Exception {
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientAdminOtherDomain.getAllAPIs());
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList),
                "API is  visible to admin in other domain in API Store. When Visibility is public. "
                        + getAPIIdentifierString(apiIdentifier));
    }

    @Test(groups = { "wso2.am" }, description = "Test the visibility for API in other domainStore for anonymous user",
            dependsOnMethods = "testVisibilityForAdminInOtherDomainInStore")
    public void testVisibilityForAnonymousUserInOtherDomainInStore() throws Exception {
        List<APIIdentifier> apiStoreAPIIdentifierList = APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(
                apiStoreClientAdminOtherDomain.getAPIListFromStoreAsAnonymousUser(otherDomain));
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList),
                "API is  visible to admin in other domain in API Store. When Visibility is public. "
                        + getAPIIdentifierString(apiIdentifier));
    }

    @Test(groups = { "wso2.am" }, description = "Test the visibility for API in Same domainStore for anonymous user",
            dependsOnMethods = "testVisibilityForAnonymousUserInOtherDomainInStore")
    public void testVisibilityForAnonymousUserInSameDomainInStore() throws Exception {
        long maxLookupTime = 60 * 1000;
        long currentTime;
        boolean apiFound = false;
        long startTime = System.currentTimeMillis();
        do {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                log.warn("InterruptedException occurs while sleeping 500 milliseconds", e);
            }
            currentTime = System.currentTimeMillis();
            List<APIIdentifier> apiStoreAPIIdentifierList = APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(
                    apiStoreClientAdminOtherDomain.getAPIListFromStoreAsAnonymousUser(apiCreatorStoreDomain));
            if (APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList)) {
                apiFound = true;
                break;
            }
            log.info(API_NAME + " API is not visible for anonymous user in same domain in store after :" + (currentTime
                    - startTime) + " milliseconds");
        } while ((currentTime - startTime) < maxLookupTime);
        assertTrue(apiFound,
                "API is not visible to anonymous user in same domain API Store After " + (currentTime - startTime)
                        + " milliseconds. When Visibility is public.  " + getAPIIdentifierString(apiIdentifier));
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException, ApiException {
        deleteAPI(apiID, apiPublisherClientUser1);
    }

}
