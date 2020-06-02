/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.scenario.tests.rest.api.restrictedVisibility;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;

import java.net.URL;

import static org.testng.Assert.assertTrue;

public class RestApiVisibilityRestrictedByRolesTestCase extends ScenarioTestBase {
    private static final Log log = LogFactory.getLog(RestApiVisibilityRestrictedByRolesTestCase.class);
    private APIPublisherRestClient apiPublisher;
    private String apiName;
    private String apiContext;
    private String apiVersion = "1.0.0";
    private String apiResource = "/find";
    private String apiVisibility = "restricted";
    private String tierCollection = "Gold,Bronze";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";

    private String userName;
    private String password;
    private String subscribeRole;
    private String creatorRole;

    private final String ADMIN_LOGIN_USERNAME = "admin";
    private final String ADMIN_PASSWORD = "admin";
    private final String VISIBILITY_TYPE = "store";
    private final String PUBLISHER_CREATOR_USERNAME = "APIVisiPublisherCreatorPos";
    private final String PUBLISHER_CREATOR_PW = "APIVisiPublisherCreatorPos";
    private final String SUBSCRIBER_USERNAME = "APIVisibilitySubscriberPos";
    private final String SUBSCRIBER_PW = "APIVisibilitySubscriberPos";

    private APIStoreRestClient apiStoreClient;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        apiPublisher = new APIPublisherRestClient(publisherURL);
        createUserWithPublisherAndCreatorRole(PUBLISHER_CREATOR_USERNAME, PUBLISHER_CREATOR_PW, ADMIN_LOGIN_USERNAME,
                ADMIN_PASSWORD);
        apiPublisher.login(PUBLISHER_CREATOR_USERNAME, PUBLISHER_CREATOR_PW);
        createUserWithSubscriberRole(SUBSCRIBER_USERNAME, SUBSCRIBER_PW, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
    }

    @Test(description = "1.5.2.1")
    public void testVisibilityOfAPISRestrictedByRoles() throws Exception {

        userName = "SubscriberUser";
        password = "password123$";
        apiName = "PhoneVerificationAdd";
        apiContext = "/phoneVerifyAdd";
        subscribeRole = "Health-Subscriber";

        String[] permissionArray = new String[]{"/permission/admin/login",
                "/permission/admin/manage/api/subscribe"};

        createRole(ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD, subscribeRole, permissionArray);
        createUser(userName, password, new String[]{subscribeRole} , ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        APIRequest apiRequest = new APIRequest(apiName, apiContext, apiVisibility, subscribeRole, VISIBILITY_TYPE, apiVersion, apiResource,
                tierCollection, new URL(backendEndPoint));
        apiPublisher.validateRoles(subscribeRole);

        createAPI(apiRequest);
        getAPI(PUBLISHER_CREATOR_USERNAME);
        publishAPI(apiName, PUBLISHER_CREATOR_USERNAME);
        loginToStore(userName, password);
        isAPIVisibleInStore(apiName, apiStoreClient);
    }

    @Test(description = "1.5.2.2")
    public void testVisibilityOfAPISRestrictedByMultipleRoles() throws Exception {

        subscribeRole = "NewRole1";
        creatorRole = "NewRole2";
        userName = "MultipleRoleUser";
        password = "password123$";
        apiName = "APIWildCardApi";
        apiContext = "/AddApiWildCardApi";

        String[] permissionArray = new String[]{"/permission/admin/login",
                "/permission/admin/manage/api/subscribe"};

        createRole(ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD, subscribeRole, permissionArray);
        createRole(ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD, creatorRole, permissionArray);
        createUser(userName, password, new String[]{subscribeRole} , ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);

        String multipleRoles = subscribeRole + "," + creatorRole;
        APIRequest apiRequest = new APIRequest(apiName, apiContext, apiVisibility, multipleRoles, VISIBILITY_TYPE,
                apiVersion, apiResource, tierCollection, new URL(backendEndPoint));

        validateRoles(multipleRoles);
        createAPI(apiRequest);
        getAPI(PUBLISHER_CREATOR_USERNAME);
        publishAPI(apiName, PUBLISHER_CREATOR_USERNAME);
        loginToStore(userName, password);
        isAPIVisibleInStore(apiName, apiStoreClient);
    }

//    todo need to config a secondary userstore to check roles with spaces
//    @Test(description = "1.5.2.5")
//    public void testVisibilityForRolesWithSpaces() throws Exception {
//        apiName = "APIVisibility_roleWithSpace";
//        String[] subscriberPermission = new String[]{"/permission/admin/login",
//                "/permission/admin/manage/api/subscribe"};
//
//        createRole(ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD, SUBSCRIBER_ROLE_WITH_SPACE, subscriberPermission);
//        createUser(SUBSCRIBER1_USERNAME, SUBSCRIBER1_PW, new String[]{SUBSCRIBER_ROLE_WITH_SPACE} ,
//                ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
//        APIRequest apiRequest = new APIRequest(apiName, "/" + apiName, apiVisibility, SUBSCRIBER_ROLE_WITH_SPACE,
//                apiVersion, apiResource, tierCollection, new URL(backendEndPoint));
//        validateRoles(SUBSCRIBER_ROLE_WITH_SPACE);
//        createAPI(apiRequest);
//        getAPI(PUBLISHER_CREATOR_USERNAME);
//        publishAPI(apiName, ADMIN_LOGIN_USERNAME);
//        loginToStore(SUBSCRIBER_USERNAME, SUBSCRIBER_PW);
//        isAPIVisibleInStore(apiName, apiStoreClient);
//    }

    @Test(description = "1.5.2.7")
    public void testVisibilityOfAPITags() throws Exception {
        apiName = "APIVisibility_tags";
        String tag = "tagA";

        APIRequest apiRequest = new APIRequest(apiName, "/" + apiName, apiVisibility,
                ScenarioTestConstants.SUBSCRIBER_ROLE, apiVersion, apiResource, tierCollection,
                new URL(backendEndPoint), tag);
        validateRoles(ScenarioTestConstants.SUBSCRIBER_ROLE);
        createAPI(apiRequest);
        getAPI(PUBLISHER_CREATOR_USERNAME);
        publishAPI(apiName, PUBLISHER_CREATOR_USERNAME);
        loginToStore(SUBSCRIBER_USERNAME, SUBSCRIBER_PW);
        isAPIVisibleInStore(apiName, apiStoreClient);
        isTagVisibleInStore(tag, apiStoreClient, false);
    }

    @Test(description = "1.5.2.8")
    public void testVisibilityOfAPITagsWithRestrictedAndPublicAPIs() throws Exception {
        apiName = "APIVisibility_tagsPublicAndRestricted1";
        String tag = "tagsPublicAndRestricted";

        APIRequest apiRequest = new APIRequest(apiName, "/" + apiName, apiVisibility,
                ScenarioTestConstants.CREATOR_ROLE, apiVersion, apiResource, tierCollection,
                new URL(backendEndPoint), tag);
        validateRoles(ScenarioTestConstants.CREATOR_ROLE);
        createAPI(apiRequest);
        getAPI(PUBLISHER_CREATOR_USERNAME);
        publishAPI(apiName, PUBLISHER_CREATOR_USERNAME);

        apiName = "APIVisibility_tagsPublicAndRestricted2";
        apiRequest = new APIRequest(apiName, "/" + apiName, "public", apiVersion, apiResource,
                tierCollection, new URL(backendEndPoint), tag);
        createAPI(apiRequest);
        getAPI(PUBLISHER_CREATOR_USERNAME);
        publishAPI(apiName, PUBLISHER_CREATOR_USERNAME);
        loginToStore(SUBSCRIBER_USERNAME, SUBSCRIBER_PW);
        isAPIVisibleInStore(apiName, apiStoreClient);
        isTagVisibleInStore(tag, apiStoreClient,false);
        isTagVisibleInStore(tag, new APIStoreRestClient(storeURL), true);
    }

    @Test(description = "1.5.2.9")
    public void testVisibilityOfTagsUsedByMultipleAPIsWithDistinctRoles() throws Exception {
        apiName = "APIVisibility_tagsDistinctRoles1";
        String tag = "tagsDistinctRoles";

        APIRequest apiRequest = new APIRequest(apiName, "/" + apiName, apiVisibility,
                ScenarioTestConstants.CREATOR_ROLE, apiVersion, apiResource, tierCollection,
                new URL(backendEndPoint), tag);
        validateRoles(ScenarioTestConstants.CREATOR_ROLE);
        createAPI(apiRequest);
        getAPI(PUBLISHER_CREATOR_USERNAME);
        publishAPI(apiName, PUBLISHER_CREATOR_USERNAME);
        waitForAPIDeploymentSync(PUBLISHER_CREATOR_USERNAME, apiName, apiVersion,
                APIMIntegrationConstants.IS_API_EXISTS);

        apiName = "APIVisibility_tagsDistinctRoles2";
        apiRequest = new APIRequest(apiName, "/" + apiName, apiVisibility,
                ScenarioTestConstants.SUBSCRIBER_ROLE, apiVersion, apiResource, tierCollection,
                new URL(backendEndPoint), tag);
        validateRoles(ScenarioTestConstants.SUBSCRIBER_ROLE);
        createAPI(apiRequest);
        getAPI(PUBLISHER_CREATOR_USERNAME);
        publishAPI(apiName, PUBLISHER_CREATOR_USERNAME);
        loginToStore(SUBSCRIBER_USERNAME, SUBSCRIBER_PW);
        isAPIVisibleInStore(apiName, apiStoreClient);
        isTagVisibleInStore(tag, apiStoreClient,false);
    }

    // TODO: 2/27/19 Enable the test after identifying the cause of build failure
    @Test(description = "1.5.2.10", dependsOnMethods = {"testVisibilityOfTagsUsedByMultipleAPIsWithDistinctRoles"}, enabled = false)
    public void testListingOfRestrictedAPIsByTags() throws Exception {
        String tag = "tagsDistinctRoles";

        HttpResponse apisWithTagResponse = apiStoreClient.getAPIPageFilteredWithTags(tag);
        Assert.assertNotNull(apisWithTagResponse, "Response object is null");
        log.info("Response Code for get API by tag \'" + tag + "\': " + apisWithTagResponse.getResponseCode());
        log.info("Response Message for get API by tag \'" + tag + "\': " + apisWithTagResponse.getData());
        Assert.assertEquals(apisWithTagResponse.getResponseCode(), HttpStatus.SC_OK,
                "Response code is not as expected for retrieving APIs by tag");
        assertTrue(apisWithTagResponse.getData().contains("APIVisibility_tagsDistinctRoles2"),
                "API \'APIVisibility_tagsDistinctRoles2\' is not visible to the user with restricted role");
        assertTrue(!apisWithTagResponse.getData().contains("APIVisibility_tagsDistinctRoles1"),
                "API \'APIVisibility_tagsDistinctRoles1\' is visible to the user without the restricted role");
    }

    private void loginToStore(String userName, String password) throws Exception {

        setKeyStoreProperties();
        apiStoreClient = new APIStoreRestClient(storeURL);
        apiStoreClient.login(userName, password);
    }

    private void validateRoles(String roles) throws Exception {

        HttpResponse checkValidationRole = apiPublisher.validateRoles(roles);
        assertTrue(checkValidationRole.getData().contains("true"));
        verifyResponse(checkValidationRole);
    }

    private void createAPI(APIRequest apiCreationRequest) throws Exception {

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequest);
        verifyResponse(apiCreationResponse);
    }

    private void publishAPI(String apiName, String username) throws Exception {

        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiName, username, APILifeCycleState.PUBLISHED);
        HttpResponse apiResponsePublisher = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(apiResponsePublisher);
        assertTrue(apiResponsePublisher.getData().contains("PUBLISHED"), "API has not been created in publisher");
    }

    public void getAPI(String provider) throws Exception {

        HttpResponse apiResponseGetAPI = apiPublisher.getAPI(apiName, provider, apiVersion);
        verifyResponse(apiResponseGetAPI);
        assertTrue(apiResponseGetAPI.getData().contains(apiName), apiName + " is not visible in publisher");
    }

   @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        apiPublisher.deleteAPI("PhoneVerificationAdd", apiVersion, PUBLISHER_CREATOR_USERNAME);
        apiPublisher.deleteAPI("APIWildCardApi", apiVersion, PUBLISHER_CREATOR_USERNAME);
        apiPublisher.deleteAPI("APIVisibility_tags", apiVersion, PUBLISHER_CREATOR_USERNAME);
        apiPublisher.deleteAPI("APIVisibility_tagsPublicAndRestricted1", apiVersion,
                PUBLISHER_CREATOR_USERNAME);
        apiPublisher.deleteAPI("APIVisibility_tagsPublicAndRestricted2", apiVersion,
                PUBLISHER_CREATOR_USERNAME);
        apiPublisher.deleteAPI("APIVisibility_tagsDistinctRoles1", apiVersion,
                PUBLISHER_CREATOR_USERNAME);
        apiPublisher.deleteAPI("APIVisibility_tagsDistinctRoles2", apiVersion,
                PUBLISHER_CREATOR_USERNAME);
        apiPublisher.deleteAPI("APIVisibility_apiByTag1", apiVersion,
                PUBLISHER_CREATOR_USERNAME);
        apiPublisher.deleteAPI("APIVisibility_apiByTag2", apiVersion,
                PUBLISHER_CREATOR_USERNAME);

        deleteUser(PUBLISHER_CREATOR_USERNAME, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        deleteUser(SUBSCRIBER_USERNAME, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        deleteUser("SubscriberUser", ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        deleteRole("Health-Subscriber", ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        deleteUser("MultipleRoleUser", ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        deleteRole("NewRole1", ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        deleteRole("NewRole2", ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
    }
}

