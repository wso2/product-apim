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
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.xpath.XPathExpressionException;
import java.net.URL;
import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Create a API with public visibility and check the visibility in Publisher Store.
 */
public class APIVisibilityByPublicTestCase extends APIManagerLifecycleBaseTest {
    private static final Log log = LogFactory.getLog(APIVisibilityByPublicTestCase.class);
    private static final String API_NAME = "APILifeCycleTestAPI";
    private static final String API_CONTEXT = "testAPI";
    private static final String API_TAGS = "youtube, video, media";
    private static final String API_END_POINT_URL = "http://gdata.youtube.com/feeds/api/standardfeeds";
    private static final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private static final String CARBON_SUPER_TENANT2_KEY = "userKey2";
    private static final String TENANT_DOMAIN_KEY = "wso2.com";
    private static final String TENANT_DOMAIN_ADMIN_KEY = "admin";
    private static final String USER_KEY_USER2 = "userKey1";
    private static final String OTHER_DOMAIN_TENANT_USER_KEY = "user1";
    private APIIdentifier apiIdentifier;
    private APIStoreRestClient apiStoreClientAnotherUserSameDomain;
    private APIPublisherRestClient apiPublisherClientUserAnotherUserSameDomain;
    private APIStoreRestClient apiStoreClientAnotherUserOtherDomain;
    private APIPublisherRestClient apiPublisherClientAnotherUserOtherDomain;
    private APIStoreRestClient apiStoreClientAdminOtherDomain;
    private APIPublisherRestClient apiPublisherClientAdminOtherDomain;
    private String providerName;
    private APIPublisherRestClient apiPublisherClientUser2;
    private APIStoreRestClient apiStoreClientUser2;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private String otherDomain;
    private String apiCreatorStoreDomain;
    private String storeURLHttp;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException, XPathExpressionException {
        //Creating CarbonSuper context
        super.init();
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        storeURLHttp = storeUrls.getWebAppURLHttp();

        //Login to API Publisher and Store with CarbonSuper admin
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);
        apiPublisherClientUser1.login(
                publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        apiStoreClientUser1.login(
                storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

        //Login to API Publisher adn Store with CarbonSuper normal user1
        apiPublisherClientUser2 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser2 = new APIStoreRestClient(storeURLHttp);
        providerName = publisherContext.getContextTenant().getTenantUser(USER_KEY_USER2).getUserName();
        apiPublisherClientUser2.login(
                publisherContext.getContextTenant().getTenantUser(USER_KEY_USER2).getUserName(),
                publisherContext.getContextTenant().getTenantUser(USER_KEY_USER2).getPassword());
        apiStoreClientUser2.login(
                storeContext.getContextTenant().getTenantUser(USER_KEY_USER2).getUserName(),
                storeContext.getContextTenant().getTenantUser(USER_KEY_USER2).getPassword());
        apiCreatorStoreDomain = storeContext.getContextTenant().getDomain();
        //Login to API Publisher adn Store with CarbonSuper normal user2
        apiStoreClientAnotherUserSameDomain = new APIStoreRestClient(storeURLHttp);
        apiPublisherClientUserAnotherUserSameDomain = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientAnotherUserSameDomain.login(
                storeContext.getContextTenant().getTenantUser(CARBON_SUPER_TENANT2_KEY).getUserName(),
                storeContext.getContextTenant().getTenantUser(CARBON_SUPER_TENANT2_KEY).getPassword());
        apiPublisherClientUserAnotherUserSameDomain.login(
                publisherContext.getContextTenant().getTenantUser(CARBON_SUPER_TENANT2_KEY).getUserName(),
                publisherContext.getContextTenant().getTenantUser(CARBON_SUPER_TENANT2_KEY).getPassword());

        init(TENANT_DOMAIN_KEY, TENANT_DOMAIN_ADMIN_KEY, "publisher", "store", "gateway");

        otherDomain = storeContext.getContextTenant().getDomain();
        //Login to the API Publisher adn Store as Tenant user
        apiStoreClientAnotherUserOtherDomain = new APIStoreRestClient(storeURLHttp);
        apiPublisherClientAnotherUserOtherDomain = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientAnotherUserOtherDomain.login(
                storeContext.getContextTenant().getTenantUser(OTHER_DOMAIN_TENANT_USER_KEY).getUserName(),
                storeContext.getContextTenant().getTenantUser(OTHER_DOMAIN_TENANT_USER_KEY).getPassword());
        apiPublisherClientAnotherUserOtherDomain.login(
                publisherContext.getContextTenant().getTenantUser(OTHER_DOMAIN_TENANT_USER_KEY).getUserName(),
                publisherContext.getContextTenant().getTenantUser(OTHER_DOMAIN_TENANT_USER_KEY).getPassword());

        //Login to the API Publisher adn Store as Tenant admin
        apiStoreClientAdminOtherDomain = new APIStoreRestClient(storeURLHttp);
        apiPublisherClientAdminOtherDomain = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientAdminOtherDomain.login(
                storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());
        apiPublisherClientAdminOtherDomain.login(
                publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Publisher for API creator ")
    public void testVisibilityForCreatorInPublisher() throws Exception {
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                        new URL(API_END_POINT_URL));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        //Create API  with public visibility and publish.
        apiPublisherClientUser2.addAPI(apiCreationRequestBean);
        APIIdentifier apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        publishAPI(apiIdentifier, apiPublisherClientUser2, false);
        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(
                        apiPublisherClientUser2.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(this.apiIdentifier, apiPublisherAPIIdentifierList),
                "API is not visible to creator in APi Publisher. When Visibility is public. " +
                        getAPIIdentifierString(this.apiIdentifier));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Store for API creator",
            dependsOnMethods = "testVisibilityForCreatorInPublisher")
    public void testVisibilityForCreatorInStore() throws APIManagerIntegrationTestException {
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientUser2.getAPI());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList),
                "API is not visible to creator in API Store. When Visibility is public. " +
                        getAPIIdentifierString(apiIdentifier));
    }

    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Publisher for admin in same domain ",
            dependsOnMethods = "testVisibilityForCreatorInStore")
    public void testVisibilityForAdminInSameDomainInPublisher() throws APIManagerIntegrationTestException {
        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(
                        apiPublisherClientUser1.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiPublisherAPIIdentifierList),
                "API is not visible to admin in same domain in API Publisher. When Visibility is public. " +
                        getAPIIdentifierString(apiIdentifier));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Store for admin in same domain ",
            dependsOnMethods = "testVisibilityForAdminInSameDomainInPublisher")
    public void testVisibilityForAdminInSameDomainInStore() throws APIManagerIntegrationTestException {

        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientUser1.getAPI());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList),
                "API is not visible to admin in same domain in API Store. When Visibility is public. " +
                        getAPIIdentifierString(apiIdentifier));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Publisher for another user in same domain",
            dependsOnMethods = "testVisibilityForAdminInSameDomainInStore")
    public void testVisibilityForAnotherUserInSameDomainInPublisher() throws APIManagerIntegrationTestException {
        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(
                        apiPublisherClientUserAnotherUserSameDomain.getAllAPIs());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiPublisherAPIIdentifierList),
                "API is not visible to another user in same domain in API Publisher. When Visibility is public." +
                        getAPIIdentifierString(apiIdentifier));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Store for another user in same domain",
            dependsOnMethods = "testVisibilityForAnotherUserInSameDomainInPublisher")
    public void testVisibilityForAnotherUserInSameDomainInStore() throws APIManagerIntegrationTestException {
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientAnotherUserSameDomain.getAPI());
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList),
                "API is not visible to another user in same domain in API Store. When Visibility is public." +
                        getAPIIdentifierString(apiIdentifier));
    }

    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Publisher for another user in other domain",
            dependsOnMethods = "testVisibilityForAnotherUserInSameDomainInStore")
    public void testVisibilityForAnotherUserInOtherDomainInPublisher() throws APIManagerIntegrationTestException {
        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(
                        apiPublisherClientAnotherUserOtherDomain.getAllAPIs());
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiPublisherAPIIdentifierList),
                "API is  visible to another user in other domain in API Publisher. When Visibility is public." +
                        getAPIIdentifierString(apiIdentifier));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Store for another user in other domain",
            dependsOnMethods = "testVisibilityForAnotherUserInOtherDomainInPublisher")
    public void testVisibilityForAnotherUserInOtherDomainInStore() throws APIManagerIntegrationTestException {
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientAnotherUserOtherDomain.getAPI());
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList),
                "API is  visible to another user in other domain in API Store. When Visibility is public. " +
                        getAPIIdentifierString(apiIdentifier));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Publisher for admin in other domain",
            dependsOnMethods = "testVisibilityForAnotherUserInOtherDomainInStore")
    public void testVisibilityForAdminInOtherDomainInPublisher() throws APIManagerIntegrationTestException {
        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(
                        apiPublisherClientAdminOtherDomain.getAllAPIs());
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiPublisherAPIIdentifierList),
                "API is  visible to admin in other domain in API Publisher. When Visibility is public. " +
                        getAPIIdentifierString(apiIdentifier));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Store for admin in other domain",
            dependsOnMethods = "testVisibilityForAdminInOtherDomainInPublisher")
    public void testVisibilityForAdminInOtherDomainInStore() throws APIManagerIntegrationTestException {
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(apiStoreClientAdminOtherDomain.getAPI());
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList),
                "API is  visible to admin in other domain in API Store. When Visibility is public. " +
                        getAPIIdentifierString(apiIdentifier));
    }

    @Test(groups = {"wso2.am"}, description = "Test the visibility for API in other domainStore for anonymous user",
            dependsOnMethods = "testVisibilityForAdminInOtherDomainInStore")
    public void testVisibilityForAnonymousUserInOtherDomainInStore() throws APIManagerIntegrationTestException {
        HttpResponse httpResponse = new APIStoreRestClient(storeURLHttp).getAPIStorePageAsAnonymousUser(otherDomain);
        assertFalse(httpResponse.getData().contains(API_NAME), "API is  visible to anonymous user in other " +
                "domain API Store. When Visibility is public. " + getAPIIdentifierString(apiIdentifier));
    }

    @Test(groups = {"wso2.am"}, description = "Test the visibility for API in Same domainStore for anonymous user",
            dependsOnMethods = "testVisibilityForAnonymousUserInOtherDomainInStore")
    public void testVisibilityForAnonymousUserInSameDomainInStore() throws APIManagerIntegrationTestException {
        long maxLookupTime = 60 * 1000;
        long currentTime;
        boolean notFound = true;
        long startTime = System.currentTimeMillis();
        do {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                log.warn("InterruptedException occurs while sleeping 500 milliseconds", e);
            }
            currentTime = System.currentTimeMillis();
            if (new APIStoreRestClient(storeURLHttp).getAPIStorePageAsAnonymousUser(apiCreatorStoreDomain).getData().
                    contains(API_NAME)) {
                notFound = false;
            }
            log.info("API is visible for anonymous user in same domain in store after :" + (currentTime - startTime) +
                    " milliseconds :" + !notFound);
        } while (notFound || currentTime - startTime > maxLookupTime);
        HttpResponse httpResponse = new APIStoreRestClient(storeURLHttp).getAPIStorePageAsAnonymousUser(
                apiCreatorStoreDomain);
        assertTrue(!notFound, "API is not visible to anonymous user in same domain API Store After" +
                (currentTime - startTime) + "milliseconds. When Visibility is public.  " +
                getAPIIdentifierString(apiIdentifier));
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException {
        deleteAPI(apiIdentifier, apiPublisherClientUser1);
    }


}
