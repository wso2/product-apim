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

import org.json.JSONException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.APIMgtTestUtil;
import org.wso2.am.integration.test.utils.base.AMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.xpath.XPathExpressionException;
import java.net.URL;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * Create a API with domain visibility and check the visibility in Publisher Store.
 */
public class APIVisibilityByDomainTestCase extends APIManagerLifecycleBaseTest {

    private static final String API_NAME = "APILifeCycleTestAPI1";
    private static final String API_CONTEXT = "testAPI1";
    private static final String API_TAGS = "youtube, video, media";
    private static final String API_END_POINT_URL = "http://gdata.youtube.com/feeds/api/standardfeeds";
    private static final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private static final String API_END_POINT_METHOD = "/most_popular";
    private static final String API_RESPONSE_DATA = "<feed";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private static final String CARBON_SUPER_TENANT2_KEY = "userKey2";
    private static final String TENANT_DOMAIN_KEY = "wso2.com";
    private static final String TENANT_DOMAIN_ADMIN_KEY = "admin";
    private static final String USER_KEY_USER2 = "userKey1";
    private APIIdentifier apiIdentifier;
    private APIStoreRestClient apiStoreClientAnotherUserSameDomain;
    private APIPublisherRestClient apiPublisherClientUserAnotherUserSameDomain;
    private APIStoreRestClient apiStoreClientAnotherUserOtherDomain;
    private APIPublisherRestClient apiPublisherClientAnotherUserOtherDomain;
    private APIStoreRestClient apiStoreClientAdminOtherDomain;
    private APIPublisherRestClient apiPublisherClientAdminOtherDomain;
    private String providerName;
    private AutomationContext otherDomainContext;
    private APIPublisherRestClient apiPublisherClientUser2;
    private APIStoreRestClient apiStoreClientUser2;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private APICreationRequestBean apiCreationRequestBean;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, new URL(API_END_POINT_URL));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiPublisherClientUser1 = new APIPublisherRestClient(getPublisherServerURLHttp());
        apiStoreClientUser1 = new APIStoreRestClient(getStoreServerURLHttp());

        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
        //Login to API Store with  admin
        apiStoreClientUser1.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
        apiPublisherClientUser2 = new APIPublisherRestClient(getPublisherServerURLHttp());
        apiStoreClientUser2 = new APIStoreRestClient(getStoreServerURLHttp());
        //Login to API Publisher with  User1
        providerName = apimContext.getContextTenant().getTenantUser(USER_KEY_USER2).getUserName();
        String user2PassWord = apimContext.getContextTenant().getTenantUser(USER_KEY_USER2).getPassword();
        apiPublisherClientUser2.login(providerName, user2PassWord);
        //Login to API Store with  User1
        apiStoreClientUser2.login(providerName, user2PassWord);
        apiStoreClientAnotherUserSameDomain = new APIStoreRestClient(getStoreServerURLHttp());
        apiPublisherClientUserAnotherUserSameDomain = new APIPublisherRestClient(getPublisherServerURLHttp());
        String AnotherUserSameDomainUserName =
                apimContext.getContextTenant().getTenantUser(CARBON_SUPER_TENANT2_KEY).getUserName();
        String AnotherUserSameDomainPassword =
                apimContext.getContextTenant().getTenantUser(CARBON_SUPER_TENANT2_KEY).getPassword();
        apiStoreClientAnotherUserSameDomain.login(AnotherUserSameDomainUserName, AnotherUserSameDomainPassword);
        apiPublisherClientUserAnotherUserSameDomain.login(AnotherUserSameDomainUserName, AnotherUserSameDomainPassword);
        otherDomainContext =
                new AutomationContext(AMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                        AMIntegrationConstants.AM_1ST_INSTANCE,
                        TENANT_DOMAIN_KEY, TENANT_DOMAIN_ADMIN_KEY);
        apiStoreClientAnotherUserOtherDomain = new APIStoreRestClient(getStoreServerURLHttp());
        apiPublisherClientAnotherUserOtherDomain = new APIPublisherRestClient(getPublisherServerURLHttp());
        String userOtherDomainUserName = otherDomainContext.getContextTenant().getTenantUser("user1").getUserName();
        String userOtherDomainPassword = otherDomainContext.getContextTenant().getTenantUser("user1").getPassword();
        apiStoreClientAnotherUserOtherDomain.login(userOtherDomainUserName, userOtherDomainPassword);
        apiPublisherClientAnotherUserOtherDomain.login(userOtherDomainUserName, userOtherDomainPassword);

        apiStoreClientAdminOtherDomain = new APIStoreRestClient(getStoreServerURLHttp());
        apiPublisherClientAdminOtherDomain = new APIPublisherRestClient(getPublisherServerURLHttp());
        String adminOtherDomainUserName = otherDomainContext.getContextTenant().getContextUser().getUserName();
        String adminOtherDomainPassword = otherDomainContext.getContextTenant().getContextUser().getPassword();
        apiStoreClientAdminOtherDomain.login(adminOtherDomainUserName, adminOtherDomainPassword);
        apiPublisherClientAdminOtherDomain.login(adminOtherDomainUserName, adminOtherDomainPassword);
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Publisher for API creator ")
    public void tesVisibilityForCreatorInPublisher() throws APIManagerIntegrationTestException, JSONException {
        //Create API  with private visibility and publish.
        //  createAPI(API_NAME, API1_CONTEXT, API_VERSION_1_0_0, API1_TAGS, "private", "", apiPublisherClientUser2);
        apiPublisherClientUser2.addAPI(apiCreationRequestBean);
        APIIdentifier apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        publishAPI(apiIdentifier, apiPublisherClientUser2, false);

        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(
                        apiPublisherClientUser2.getAllAPIs());
        assertEquals(APIMgtTestUtil.isAPIAvailable(this.apiIdentifier, apiPublisherAPIIdentifierList), true,
                "API is not visible to creator in APi Publisher. When Visibility is private. " +
                        getAPIIdentifierString(this.apiIdentifier));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Store for API creator",
            dependsOnMethods = "tesVisibilityForCreatorInPublisher")
    public void tesVisibilityForCreatorInStore() throws Exception {

        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(apiStoreClientUser2.getAPI(API_NAME));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList), true,
                "API is not visible to creator in API Store. When Visibility is private. " +
                        getAPIIdentifierString(apiIdentifier));

    }

    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Publisher for admin in same domain ",
            dependsOnMethods = "tesVisibilityForCreatorInStore")
    public void tesVisibilityForAdminInSameDomainInPublisher() throws APIManagerIntegrationTestException, JSONException {

        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(
                        apiPublisherClientUser1.getAllAPIs());
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifier, apiPublisherAPIIdentifierList), true,
                "API is not visible to admin in same domain in API Publisher. When Visibility is private. " +
                        getAPIIdentifierString(apiIdentifier));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Store for admin in same domain ",
            dependsOnMethods = "tesVisibilityForAdminInSameDomainInPublisher")
    public void tesVisibilityForAdminInSameDomainInStore() throws Exception {

        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(apiStoreClientUser1.getAPI(API_NAME));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList), true,
                "API is not visible to admin in same domain in API Store. When Visibility is private. " +
                        getAPIIdentifierString(apiIdentifier));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Publisher for another user in same domain",
            dependsOnMethods = "tesVisibilityForAdminInSameDomainInStore")
    public void tesVisibilityForAnotherUserInSameDomainInPublisher() throws APIManagerIntegrationTestException, JSONException {

        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(
                        apiPublisherClientUserAnotherUserSameDomain.getAllAPIs());
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifier, apiPublisherAPIIdentifierList), true,
                "API is not visible to another user in same domain in API Publisher. When Visibility is private." +
                        getAPIIdentifierString(apiIdentifier));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Store for another user in same domain",
            dependsOnMethods = "tesVisibilityForAnotherUserInSameDomainInPublisher")
    public void tesVisibilityForAnotherUserInSameDomainInStore() throws Exception {

        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(apiStoreClientAnotherUserSameDomain.getAPI(API_NAME));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList), true,
                "API is not visible to another user in same domain in API Store. When Visibility is private." +
                        getAPIIdentifierString(apiIdentifier));

    }

    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Publisher for another user in other domain",
            dependsOnMethods = "tesVisibilityForAnotherUserInSameDomainInStore")
    public void tesVisibilityForAnotherUserInOtherDomainInPublisher() throws APIManagerIntegrationTestException, JSONException {

        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(
                        apiPublisherClientAnotherUserOtherDomain.getAllAPIs());
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifier, apiPublisherAPIIdentifierList), false,
                "API is  visible to another user in other domain in API Publisher. When Visibility is private." +
                        getAPIIdentifierString(apiIdentifier));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Store for another user in other domain",
            dependsOnMethods = "tesVisibilityForAnotherUserInOtherDomainInPublisher")
    public void tesVisibilityForAnotherUserInOtherDomainInStore() throws Exception {

        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(apiStoreClientAnotherUserOtherDomain.getAPI(API_NAME));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList), false,
                "API is  visible to another user in other domain in API Store. When Visibility is private. " +
                        getAPIIdentifierString(apiIdentifier));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Publisher for admin in other domain",
            dependsOnMethods = "tesVisibilityForAnotherUserInOtherDomainInStore")
    public void tesVisibilityForAdminInOtherDomainInPublisher() throws APIManagerIntegrationTestException, JSONException {

        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(
                        apiPublisherClientAdminOtherDomain.getAllAPIs());
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifier, apiPublisherAPIIdentifierList), false,
                "API is  visible to admin in other domain in API Publisher. When Visibility is private. " +
                        getAPIIdentifierString(apiIdentifier));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Store for admin in other domain",
            dependsOnMethods = "tesVisibilityForAdminInOtherDomainInPublisher")
    public void tesVisibilityForAdminInOtherDomainInStore() throws Exception {

        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(apiStoreClientAdminOtherDomain.getAPI(API_NAME));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList), false,
                "API is  visible to admin in other domain in API Store. When Visibility is private. " +
                        getAPIIdentifierString(apiIdentifier));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility for API in Same domainStore for anonymous user",
            dependsOnMethods = "tesVisibilityForAdminInOtherDomainInStore")
    public void tesVisibilityForAnonymousUserInSameDomainInStore() throws XPathExpressionException,
            APIManagerIntegrationTestException {

        HttpResponse httpResponse = new APIStoreRestClient(getStoreServerURLHttp()).getAPIStorePageAsAnonymousUser(
                apimContext.getContextTenant().getDomain());
        assertEquals(httpResponse.getData().contains(API_NAME), false, "API  visible to anonymous user in same " +
                "domain API Store. When Visibility is private.  " + getAPIIdentifierString(apiIdentifier));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility for API in other domainStore for anonymous user",
            dependsOnMethods = "tesVisibilityForAnonymousUserInSameDomainInStore")
    public void tesVisibilityForAnonymousUserInOtherDomainInStore() throws XPathExpressionException,
            APIManagerIntegrationTestException {

        HttpResponse httpResponse = new APIStoreRestClient(getStoreServerURLHttp()).getAPIStorePageAsAnonymousUser
                (otherDomainContext.getContextTenant().getDomain());

        assertEquals(httpResponse.getData().contains(API_NAME), false, "API is  visible to anonymous user in other " +
                "domain API Store. When Visibility is private. " + getAPIIdentifierString(apiIdentifier));
    }


    @AfterClass(alwaysRun = true)
    public void cleanup() throws Exception {
        deleteAPI(apiIdentifier, apiPublisherClientUser1);


    }


}
