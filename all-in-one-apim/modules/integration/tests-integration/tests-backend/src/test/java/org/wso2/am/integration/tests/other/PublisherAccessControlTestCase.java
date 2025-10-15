/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;

import java.net.URL;
import java.rmi.RemoteException;
import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;

/**
 * This test case verifies the functionality of publisher access control restriction.
 */
public class PublisherAccessControlTestCase extends APIManagerLifecycleBaseTest {

    private static final String VERSION = "1.0.0";
    private static final String RESTRICTED_ACCESS_CONTROL = "restricted";
    private static final String PUBLIC_VISIBILITY = "public";
    private static final String NO_ACCESS_CONTROL = "none";
    private final String[] OLD_ROLE_LIST = { "Internal/publisher", "Internal/creator",
            "Internal/subscriber", "Internal/everyone", "role1" };
    private String contextUsername = "admin";
    private String FIRST_USER = "pubu1";
    private String SECOND_USER = "pubu2";
    private String PUB_SUB_USER = "pubsu1";
    private String FIRST_ROLE = "publisher_role1";
    private String SECOND_ROLE = "publisher_role2";
    private String SUBSCRIBER_ROLE = "subscriber_role1";
    private String PUB_SUB_ROLE = "pub_sub_role1";
    private final String USER_PASSWORD = "123123";
    private String SUBSCRIBER_USER = "subu1";
    private final String INTERNAL_CREATOR = "Internal/creator";
    private final String INTERNAL_PUBLISHER = "Internal/publisher";
    private final String INTERNAL_SUBSCRIBER = "Internal/subscriber";
    private final String EP_URL = "http://gdata.youtube.com/feeds/api/standardfeeds";
    private final String EMAIL_DOMAIN = "@gmail.com";
    private final String AT = "@";
    private String publisherAccessControlAPI = "PublisherAccessControl";
    private String publisherAccessControlAPI2 = "PublisherAccessControl2";
    private String publicAccessRestrictedVisibilityAPI = "PublicAccessRestrictedVisibility";
    private String accessControlledPublicVisibilityAPI = "AccessControlledPublicVisibility";
    private final String restrictedAccessRestrictedVisibilityAPI = "RestrictedAccessRestrictedVisibility";
    private RestAPIPublisherImpl apiPublisherFirstUser;
    private RestAPIPublisherImpl apiPublisherSecondUser;
    private RestAPIStoreImpl apiStoreSubUser;
    private RestAPIPublisherImpl apiPublisherPubSubUser;
    private RestAPIStoreImpl apiStorePubSubUser;
    private String publisherAccessControlAPIId;
    private String publisherAccessControlAPI2Id;
    private String accessControlledPublicVisibilityAPIId;
    private String publicAccessRestrictedVisibilityAPIId;
    private String restrictedAccessRestrictedVisibilityAPIId;

    UserManagementClient userManagementClient1;

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
                new Object[]{TestUserMode.SUPER_TENANT_USER_STORE_USER},
                new Object[]{TestUserMode.SUPER_TENANT_EMAIL_USER},
                new Object[]{TestUserMode.TENANT_EMAIL_USER},
        };
    }

    @Factory(dataProvider = "userModeDataProvider")
    public PublisherAccessControlTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass
    public void initTestCase() throws APIManagerIntegrationTestException, XPathExpressionException, RemoteException,
            UserAdminUserAdminException {
        super.init(userMode);
        publisherURLHttp = publisherUrls.getWebAppURLHttp();
        storeURLHttp = storeUrls.getWebAppURLHttp();
        contextUsername = keyManagerContext.getContextTenant().getContextUser().getUserName();
        userManagementClient1 = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                keyManagerContext.getContextTenant().getTenantAdmin().getUserName(),
                keyManagerContext.getContextTenant().getTenantAdmin().getPassword());

        if (TestUserMode.TENANT_EMAIL_USER.equals(userMode) || TestUserMode.SUPER_TENANT_EMAIL_USER.equals(userMode)) {
            FIRST_USER = FIRST_USER + EMAIL_DOMAIN + AT + keyManagerContext.getContextTenant().getContextUser()
                    .getUserDomain();
            SECOND_USER = SECOND_USER + EMAIL_DOMAIN + AT + keyManagerContext.getContextTenant().getContextUser()
                    .getUserDomain();
            SUBSCRIBER_USER =
                    SUBSCRIBER_USER + EMAIL_DOMAIN + AT + keyManagerContext.getContextTenant().getContextUser()
                            .getUserDomain();
            PUB_SUB_USER = PUB_SUB_USER + EMAIL_DOMAIN + AT + keyManagerContext.getContextTenant().getContextUser()
                    .getUserDomain();
        } else if (TestUserMode.SUPER_TENANT_USER_STORE_USER.equals(userMode)) {
            FIRST_USER = APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + FIRST_USER;
            SECOND_USER = APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + SECOND_USER;
            SUBSCRIBER_USER = APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + SUBSCRIBER_USER;
            PUB_SUB_USER = APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + PUB_SUB_USER;

            FIRST_ROLE = APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + FIRST_ROLE;
            SECOND_ROLE = APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + SECOND_ROLE;
            SUBSCRIBER_ROLE = APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + SUBSCRIBER_ROLE;
            PUB_SUB_ROLE = APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + PUB_SUB_ROLE;
        }
        userManagementClient1
                .addRole(FIRST_ROLE, new String[] {}, new String[] {});
        userManagementClient1
                .addRole(SECOND_ROLE, new String[] {}, new String[] {});
        userManagementClient1
                .addRole(SUBSCRIBER_ROLE, new String[] {}, new String[] {});
        userManagementClient1.addRole(PUB_SUB_ROLE, new String[] {},
                new String[] {});
        userManagementClient1
                .addUser(FIRST_USER, USER_PASSWORD, new String[] { INTERNAL_CREATOR, FIRST_ROLE }, FIRST_USER);
        userManagementClient1
                .addUser(SECOND_USER, USER_PASSWORD, new String[] { INTERNAL_PUBLISHER, SECOND_ROLE }, SECOND_USER);
        userManagementClient1
                .addUser(SUBSCRIBER_USER, USER_PASSWORD, new String[] { INTERNAL_SUBSCRIBER, SUBSCRIBER_ROLE },
                        SUBSCRIBER_USER);
        userManagementClient1.addUser(PUB_SUB_USER, USER_PASSWORD,
                new String[] { INTERNAL_PUBLISHER, INTERNAL_SUBSCRIBER, PUB_SUB_ROLE }, PUB_SUB_USER);

        if (TestUserMode.TENANT_EMAIL_USER.equals(userMode) || TestUserMode.SUPER_TENANT_EMAIL_USER.equals(userMode)
                || TestUserMode.SUPER_TENANT_USER_STORE_USER.equals(userMode)) {
            String[] newRoleList = { "Internal/publisher", "Internal/creator", "Internal/subscriber",
                    "Internal/everyone", FIRST_ROLE};
            userManagementClient1.updateRolesOfUser(user.getUserNameWithoutDomain(), newRoleList);
        }

        apiPublisherFirstUser = new RestAPIPublisherImpl(FIRST_USER, USER_PASSWORD,
                keyManagerContext.getContextTenant().getDomain(), publisherURLHttps);

        apiPublisherSecondUser = new RestAPIPublisherImpl(SECOND_USER, USER_PASSWORD,
                keyManagerContext.getContextTenant().getDomain(), publisherURLHttps);

        apiPublisherPubSubUser = new RestAPIPublisherImpl(PUB_SUB_USER, USER_PASSWORD,
                keyManagerContext.getContextTenant().getDomain(), publisherURLHttps);

        apiStorePubSubUser = new RestAPIStoreImpl(PUB_SUB_USER, USER_PASSWORD,
                keyManagerContext.getContextTenant().getDomain(), storeURLHttps);

        apiStoreSubUser = new RestAPIStoreImpl(SUBSCRIBER_USER, USER_PASSWORD,
                keyManagerContext.getContextTenant().getDomain(), storeURLHttps);


        restAPIPublisher = new RestAPIPublisherImpl(
                publisherContext.getContextTenant().getContextUser().getUserNameWithoutDomain(),
                publisherContext.getContextTenant().getContextUser().getPassword(),
                publisherContext.getContextTenant().getDomain(), publisherURLHttps);
        restAPIStore =
                new RestAPIStoreImpl(storeContext.getContextTenant().getContextUser().getUserNameWithoutDomain(),
                        storeContext.getContextTenant().getContextUser().getPassword(),
                        storeContext.getContextTenant().getDomain(), storeURLHttps);
    }

    @Test(groups = "wso2.am", description = "This test case tests the retrieval of API which was added with a access "
            + "control restriction.")
    public void testAPIAdditionWithAccessControlRestriction() throws Exception {
        APIRequest brokenApiRequest = new APIRequest(publisherAccessControlAPI, publisherAccessControlAPI,
                new URL(EP_URL));
        brokenApiRequest.setVersion(VERSION);
        brokenApiRequest.setProvider(contextUsername);
        brokenApiRequest.setAccessControl(RESTRICTED_ACCESS_CONTROL);
        brokenApiRequest.setAccessControlRoles(FIRST_ROLE);
        publisherAccessControlAPIId = createAndPublishAPIUsingRest(brokenApiRequest, restAPIPublisher, false);
        HttpResponse response = restAPIPublisher.getAPI(publisherAccessControlAPIId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);

        Assert.assertTrue(apidto.getAccessControlRoles().contains(FIRST_ROLE), "API was not visible to the APIM admin user");

        response = apiPublisherFirstUser.getAPI(publisherAccessControlAPIId);
        apidto = g.fromJson(response.getData(), APIDTO.class);
        Assert.assertTrue(apidto.getAccessControlRoles().contains(FIRST_ROLE),
                "API was not visible to the creators who have the relevant access control roles of the API");

        response = apiPublisherSecondUser.getAPI(publisherAccessControlAPIId);
        Assert.assertEquals(response.getResponseCode(), Response.Status.FORBIDDEN.getStatusCode(),
                "Response code mismatch");
        Assert.assertTrue(response.getData().contains("User is not authorized to access the API"),
                "API is visible to the creators who do not have the relevant access control roles of the API");
    }

    @Test(groups = "wso2.am", description = "This test case tests the retrieval of API which was added without "
            + "access control restriction.", dependsOnMethods = "testAPIAdditionWithAccessControlRestriction")
    public void testAPIAdditionWithoutAccessControlRestriction()
            throws Exception {
        APIRequest brokenApiRequest = new APIRequest(publisherAccessControlAPI2, publisherAccessControlAPI2,
                new URL(EP_URL));
        brokenApiRequest.setVersion(VERSION);
        brokenApiRequest.setProvider(contextUsername);

        publisherAccessControlAPI2Id = createAndPublishAPIUsingRest(brokenApiRequest, restAPIPublisher, false);
        HttpResponse response = restAPIPublisher.getAPI(publisherAccessControlAPI2Id);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);

        Assert.assertEquals(apidto.getName(), publisherAccessControlAPI2,
                "API is not visible to APIM admin" + " without access control restriction");

        response = apiPublisherFirstUser.getAPI(publisherAccessControlAPI2Id);
        apidto = g.fromJson(response.getData(), APIDTO.class);
        Assert.assertEquals(apidto.getName(), publisherAccessControlAPI2,
                "API is not visible to creator" + " without access control restriction");

        response = apiPublisherSecondUser.getAPI(publisherAccessControlAPI2Id);
        apidto = g.fromJson(response.getData(), APIDTO.class);
        Assert.assertEquals(apidto.getName(), publisherAccessControlAPI2,
                "API is not visible to creator" + " without access control restriction");
    }

    @Test (groups = "wso2.am", description = "This test case tests the retrieval of API which from store which was "
            + "added without access control restriction and public visibility.",
            dependsOnMethods = "testAPIAdditionWithoutAccessControlRestriction")
    public void testGetPublicAPIFromStoreWithRestrictedPublisherAccess()
            throws Exception {
        APIRequest createAPIRequest = new APIRequest(accessControlledPublicVisibilityAPI,
                accessControlledPublicVisibilityAPI, new URL(EP_URL));
        createAPIRequest.setVersion(VERSION);
        createAPIRequest.setProvider(contextUsername);
        createAPIRequest.setAccessControl(RESTRICTED_ACCESS_CONTROL);
        createAPIRequest.setAccessControlRoles(FIRST_ROLE);
        createAPIRequest.setVisibility(PUBLIC_VISIBILITY);

        accessControlledPublicVisibilityAPIId = createAndPublishAPIUsingRest(createAPIRequest, restAPIPublisher, false);

        waitForAPIDeploymentSync(contextUsername, accessControlledPublicVisibilityAPI, VERSION,
                APIMIntegrationConstants.IS_API_EXISTS);

        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDto = apiStoreSubUser
                .getAPI(accessControlledPublicVisibilityAPIId);
        Assert.assertTrue(StringUtils.isNotEmpty(apiDto.getId()),
                "Public API with name " + accessControlledPublicVisibilityAPI + "is not " + "returned");
    }

    @Test (groups = "wso2.am", description = "This test case add restricted visibility on store for role1, but user "
            + "who can log into publisher should be able to view the api even though he does not have the role role1",
            dependsOnMethods = "testGetPublicAPIFromStoreWithRestrictedPublisherAccess")
    public void testCheckPublisherRoleCanViewRestrictedVisibilityAPIs() throws Exception {
        APIRequest createAPIRequest = new APIRequest(publicAccessRestrictedVisibilityAPI,
                publicAccessRestrictedVisibilityAPI, new URL(EP_URL));
        createAPIRequest.setVersion(VERSION);
        createAPIRequest.setProvider(contextUsername);
        createAPIRequest.setAccessControl(NO_ACCESS_CONTROL);
        createAPIRequest.setVisibility(RESTRICTED_ACCESS_CONTROL);
        createAPIRequest.setRoles(FIRST_ROLE);

        publicAccessRestrictedVisibilityAPIId = createAndPublishAPIUsingRest(createAPIRequest, restAPIPublisher, false);

        waitForAPIDeploymentSync(contextUsername, publicAccessRestrictedVisibilityAPI, VERSION,
                APIMIntegrationConstants.IS_API_EXISTS);

        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDto = apiStorePubSubUser
                .getAPI(publicAccessRestrictedVisibilityAPIId);
        Assert.assertTrue(StringUtils.isNotEmpty(apiDto.getId()),
                "Restricted visible api " + publicAccessRestrictedVisibilityAPI + "is not" + " visible to user  "
                        + PUB_SUB_USER + ", who can view it in publisher");
    }

    @Test (groups = "wso2.am", description = "This test case add restricted access in publisher(role1) and restricted "
            + "visibility in store(subscriber_role1). So check correct behaviour in publisher and store ",
            dependsOnMethods = "testCheckPublisherRoleCanViewRestrictedVisibilityAPIs")
    public void testPublisherAndStoreRestricted() throws Exception {
        APIRequest createAPIRequest = new APIRequest(restrictedAccessRestrictedVisibilityAPI,
                restrictedAccessRestrictedVisibilityAPI, new URL(EP_URL));
        createAPIRequest.setVersion(VERSION);
        createAPIRequest.setProvider(contextUsername);
        createAPIRequest.setAccessControl(RESTRICTED_ACCESS_CONTROL);
        createAPIRequest.setAccessControlRoles(FIRST_ROLE);
        createAPIRequest.setVisibility(PUBLIC_VISIBILITY);

        restrictedAccessRestrictedVisibilityAPIId = createAndPublishAPIUsingRest(createAPIRequest, restAPIPublisher, false);

        waitForAPIDeploymentSync(contextUsername, restrictedAccessRestrictedVisibilityAPI, VERSION,
                APIMIntegrationConstants.IS_API_EXISTS);
        // Waiting to index
        Thread.sleep(10000);

        HttpResponse response = apiPublisherFirstUser.getAPI(restrictedAccessRestrictedVisibilityAPIId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);
        Assert.assertEquals(response.getResponseCode(), 200, "Response code does not match");
        Assert.assertEquals(apidto.getName(), restrictedAccessRestrictedVisibilityAPI,
                "Restricted visible api " + restrictedAccessRestrictedVisibilityAPI + "is not" + " visible to user  "
                        + FIRST_USER + ", who should be able to view it");

        response = apiPublisherPubSubUser.getAPI(restrictedAccessRestrictedVisibilityAPIId);
        Assert.assertNotEquals(response.getResponseCode(), 200,
                "Restricted access api " + restrictedAccessRestrictedVisibilityAPI + "is visible to user  " + FIRST_USER
                        + ", who should not be able to view it");

        createAPIRequest.setVisibility(RESTRICTED_ACCESS_CONTROL);
        createAPIRequest.setRoles(SUBSCRIBER_ROLE);
        restAPIPublisher.updateAPI(createAPIRequest, restrictedAccessRestrictedVisibilityAPIId);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(restrictedAccessRestrictedVisibilityAPIId, restAPIPublisher);
        // Waiting to index after api update operation
        Thread.sleep(10000);

        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDto = apiStoreSubUser
                .getAPI(restrictedAccessRestrictedVisibilityAPIId);

        Assert.assertTrue(StringUtils.isNotEmpty(apiDto.getId()),
                "Restricted visible api " + restrictedAccessRestrictedVisibilityAPI + " is" + " not visible to user  "
                        + SUBSCRIBER_USER + ", who can view it in store");
    }

    @AfterClass (alwaysRun = true)
    public void destroy() throws Exception {
        undeployAndDeleteAPIRevisionsUsingRest(publisherAccessControlAPIId, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(publicAccessRestrictedVisibilityAPIId, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(publisherAccessControlAPI2Id, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(restrictedAccessRestrictedVisibilityAPIId, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(accessControlledPublicVisibilityAPIId, restAPIPublisher);
        restAPIPublisher.deleteAPI(publisherAccessControlAPIId);
        restAPIPublisher.deleteAPI(publicAccessRestrictedVisibilityAPIId);
        restAPIPublisher.deleteAPI(publisherAccessControlAPI2Id);
        restAPIPublisher.deleteAPI(restrictedAccessRestrictedVisibilityAPIId);
        restAPIPublisher.deleteAPI(accessControlledPublicVisibilityAPIId);

        //Reverting back the roles of email users
        if (TestUserMode.SUPER_TENANT_EMAIL_USER.equals(userMode) || TestUserMode.TENANT_EMAIL_USER.equals(userMode)) {
            userManagementClient1.updateRolesOfUser(user.getUserNameWithoutDomain(), OLD_ROLE_LIST);
        }

        userManagementClient1.deleteUser(FIRST_USER);
        userManagementClient1.deleteUser(SECOND_USER);
        userManagementClient1.deleteUser(SUBSCRIBER_USER);
        userManagementClient1.deleteUser(PUB_SUB_USER);

        userManagementClient1.deleteRole(FIRST_ROLE);
        userManagementClient1.deleteRole(SECOND_ROLE);
        userManagementClient1.deleteRole(SUBSCRIBER_ROLE);
        userManagementClient1.deleteRole(PUB_SUB_ROLE);

    }
}
