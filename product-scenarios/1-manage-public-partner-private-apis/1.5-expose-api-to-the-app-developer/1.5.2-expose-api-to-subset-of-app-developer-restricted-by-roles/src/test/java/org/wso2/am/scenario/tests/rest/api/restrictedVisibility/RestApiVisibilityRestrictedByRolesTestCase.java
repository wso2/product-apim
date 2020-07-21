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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.annotations.*;

import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.ApiResponse;
import org.wso2.am.integration.clients.store.api.v1.dto.TagListDTO;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.testng.Assert.*;

public class RestApiVisibilityRestrictedByRolesTestCase extends ScenarioTestBase {
    private static final Log log = LogFactory.getLog(RestApiVisibilityRestrictedByRolesTestCase.class);

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

    private String apiProviderName;
    private String apiProductionEndpointPostfixUrl = "jaxrs_basic/services/customers/" + "customerservice/customers/123";

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_USERNAME = "admin@wso2.com";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";

    private final String VISIBILITY_TYPE = "RESTRICTED";

    @Factory(dataProvider = "userModeDataProvider")
    public RestApiVisibilityRestrictedByRolesTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                    ADMIN_USERNAME, ADMIN_PW);
            createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, ADMIN_USERNAME, ADMIN_PW);
        }

        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            // create user in wso2.com tenant
            addTenantAndActivate(ScenarioTestConstants.TENANT_WSO2, ADMIN_USERNAME, ADMIN_PW);
            if (isActivated(ScenarioTestConstants.TENANT_WSO2)) {
                //Add and activate wso2.com tenant
                createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                        TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
                createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, TENANT_ADMIN_USERNAME,
                        TENANT_ADMIN_PW);
            }
        }

        setup();
        super.init(userMode);

        apiProviderName = publisherContext.getContextTenant().getContextUser().getUserName();

    }

    @Test(description = "1.5.2.1")
    public void testVisibilityOfAPISRestrictedByRoles() throws Exception {

        userName = "SubscriberUser";
        password = "password123$";
        apiName = "PhoneVerificationAdd";
        apiContext = "/phoneVerifyAdd";
        subscribeRole = "Health-Subscriber";
        List<String> apiIdList = new ArrayList<>();

        String[] permissionArray = new String[]{"/permission/admin/login", "/permission/admin/manage/api/subscribe"};

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createRole(ADMIN_USERNAME, ADMIN_PW, subscribeRole, permissionArray);
            createUser(userName, password, new String[]{subscribeRole}, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            createRole(TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW, subscribeRole, permissionArray);
            createUser(userName, password, new String[]{subscribeRole}, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }

        restAPIPublisher.validateRoles(Base64.getUrlEncoder().encodeToString(subscribeRole.getBytes()));

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion, userName, new URL(backendEndPoint));
        apiCreationRequestBean.setRoles(subscribeRole);
        apiCreationRequestBean.setVisibility(VISIBILITY_TYPE);

        String apiId = createAPI(apiCreationRequestBean);
        apiIdList.add(apiId);

        getAPI(apiId);
        publishAPI(apiId);

        RestAPIStoreImpl restAPIStoreNew = new RestAPIStoreImpl(
                userName, password, storeContext.getContextTenant().getDomain(), storeURLHttps);
        try {
            if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
                org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponseStore = restAPIStoreNew.getAPI(apiId);
                assertEquals(apiResponseStore.getId(), apiId, "Response object API ID mismatch");
            }
            if (this.userMode.equals(TestUserMode.TENANT_USER)) {
                org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponseStore = restAPIStoreNew.apIsApi.apisApiIdGet(apiId, "wso2.com", null);
                assertEquals(apiResponseStore.getId(), apiId, "Response object API ID mismatch");
            }
        } catch (ApiException e) {
            assertTrue(false, "Cannot get the API from the store for role " + subscribeRole);
        } finally {
            if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
                deleteRole(subscribeRole, ADMIN_USERNAME, ADMIN_PW);
                deleteUser(userName, ADMIN_USERNAME, ADMIN_PW);
            }
            if (this.userMode.equals(TestUserMode.TENANT_USER)) {
                deleteRole(subscribeRole, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
                deleteUser(userName, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            }
        }

        for (String id : apiIdList) {
            restAPIPublisher.deleteAPI(id);
        }
    }

    @Test(description = "1.5.2.2")
    public void testVisibilityOfAPISRestrictedByMultipleRoles() throws Exception {

        subscribeRole = "NewRole1";
        creatorRole = "NewRole2";
        userName = "MultipleRoleUser";
        password = "password123$";
        apiName = "APIWildCardApi";
        apiContext = "/AddApiWildCardApi";
        List<String> apiIdList = new ArrayList<>();

        String[] permissionArray = new String[]{"/permission/admin/login", "/permission/admin/manage/api/subscribe"};
        String multipleRoles = subscribeRole + "," + creatorRole;

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createRole(ADMIN_USERNAME, ADMIN_PW, subscribeRole, permissionArray);
            createRole(ADMIN_USERNAME, ADMIN_PW, creatorRole, permissionArray);
            createUser(userName, password, new String[]{subscribeRole}, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            createRole(TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW, subscribeRole, permissionArray);
            createRole(TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW, creatorRole, permissionArray);
            createUser(userName, password, new String[]{subscribeRole}, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }

        restAPIPublisher.validateRoles(Base64.getUrlEncoder().encodeToString(subscribeRole.getBytes()));
        restAPIPublisher.validateRoles(Base64.getUrlEncoder().encodeToString(creatorRole.getBytes()));

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, apiContext, apiVersion, userName, new URL(backendEndPoint));
        apiCreationRequestBean.setRoles(multipleRoles);
        apiCreationRequestBean.setVisibility(VISIBILITY_TYPE);

        String apiId = createAPI(apiCreationRequestBean);
        apiIdList.add(apiId);

        getAPI(apiId);
        publishAPI(apiId);

        RestAPIStoreImpl restAPIStoreNew = new RestAPIStoreImpl(
                userName, password, storeContext.getContextTenant().getDomain(), storeURLHttps);
        try {
            if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
                org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponseStore = restAPIStoreNew.getAPI(apiId);
                assertEquals(apiResponseStore.getId(), apiId, "Response object API ID mismatch");
            }
            if (this.userMode.equals(TestUserMode.TENANT_USER)) {
                org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponseStore = restAPIStoreNew.apIsApi.apisApiIdGet(apiId, "wso2.com", null);
                assertEquals(apiResponseStore.getId(), apiId, "Response object API ID mismatch");
            }
        } catch (ApiException e) {
            assertTrue(false, "Cannot get the API from the store for role " + subscribeRole);
        } finally {
            if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
                deleteRole(subscribeRole, ADMIN_USERNAME, ADMIN_PW);
                deleteRole(creatorRole, ADMIN_USERNAME, ADMIN_PW);
                deleteUser(userName, ADMIN_USERNAME, ADMIN_PW);
            }
            if (this.userMode.equals(TestUserMode.TENANT_USER)) {
                deleteRole(subscribeRole, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
                deleteRole(creatorRole, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
                deleteUser(userName, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            }
        }

        for (String id : apiIdList) {
            restAPIPublisher.deleteAPI(id);
        }
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
//        //isAPIVisibleInStore(apiName, apiStoreClient);
//    }

    @Test(description = "1.5.2.7")
    public void testVisibilityOfAPITags() throws Exception {
        apiName = "APIVisibility_tags";
        String tag = "tagA";
        List<String> apiIdList = new ArrayList<>();

        restAPIPublisher.validateRoles(Base64.getUrlEncoder().encodeToString(ScenarioTestConstants.SUBSCRIBER_ROLE.getBytes()));

        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, "/" + apiName, apiVersion, userName, new URL(backendEndPoint));
        apiCreationRequestBean.setRoles(ScenarioTestConstants.SUBSCRIBER_ROLE);
        apiCreationRequestBean.setVisibility(VISIBILITY_TYPE);
        apiCreationRequestBean.setTags(tag);

        String apiId = createAPI(apiCreationRequestBean);
        apiIdList.add(apiId);

        getAPI(apiId);
        publishAPI(apiId);
        // Wait until the tags are indexed in store
        Thread.sleep(15000);

        try {
            if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
                org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponseStore = restAPIStore.getAPI(apiId);
                assertEquals(apiResponseStore.getId(), apiId, "Response object API ID mismatch");

                ApiResponse<TagListDTO> tagResponse = restAPIStore.tagsApi.tagsGetWithHttpInfo(25, 0, "", null);
                assertTrue(tagResponse.getData().getList().get(0).getValue().equals(tag));
            }
            if (this.userMode.equals(TestUserMode.TENANT_USER)) {
                org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponseStore = restAPIStore.apIsApi.apisApiIdGet(apiId, "wso2.com", null);
                assertEquals(apiResponseStore.getId(), apiId, "Response object API ID mismatch");

                ApiResponse<TagListDTO> tagResponse = restAPIStore.tagsApi.tagsGetWithHttpInfo(25, 0, "wso2.com", null);
                assertTrue(tagResponse.getData().getList().get(0).getValue().equals(tag));
            }
        } catch (ApiException e) {
            assertTrue(false, "Cannot get the API from the store for role " + ScenarioTestConstants.SUBSCRIBER_ROLE);
        }

        for (String id : apiIdList) {
            restAPIPublisher.deleteAPI(id);
        }
    }

    @Test(description = "1.5.2.8")
    public void testVisibilityOfAPITagsWithRestrictedAndPublicAPIs() throws Exception {
        apiName = "APIVisibility_tagsPublicAndRestricted1";
        String tag = "tagsPublicAndRestricted";
        List<String> apiIdList = new ArrayList<>();

        restAPIPublisher.validateRoles(Base64.getUrlEncoder().encodeToString(ScenarioTestConstants.CREATOR_ROLE.getBytes()));

        APICreationRequestBean apiCreationRequestBeanRestricted = new APICreationRequestBean(apiName, "/" + apiName, apiVersion, userName, new URL(backendEndPoint));
        apiCreationRequestBeanRestricted.setRoles(ScenarioTestConstants.CREATOR_ROLE);
        apiCreationRequestBeanRestricted.setVisibility(VISIBILITY_TYPE);
        apiCreationRequestBeanRestricted.setTags(tag);

        String apiIdRestricted = createAPI(apiCreationRequestBeanRestricted);
        apiIdList.add(apiIdRestricted);

        getAPI(apiIdRestricted);
        publishAPI(apiIdRestricted);

        apiName = "APIVisibility_tagsPublicAndRestricted2";
        APICreationRequestBean apiCreationRequestBeanPublic = new APICreationRequestBean(apiName, "/" + apiName, apiVersion, userName, new URL(backendEndPoint));
        apiCreationRequestBeanPublic.setRoles(ScenarioTestConstants.CREATOR_ROLE);
        apiCreationRequestBeanPublic.setVisibility("PUBLIC");
        apiCreationRequestBeanPublic.setTags(tag);

        String apiIdPublic = createAPI(apiCreationRequestBeanPublic);
        apiIdList.add(apiIdPublic);

        getAPI(apiIdPublic);
        publishAPI(apiIdPublic);
        // Wait until the tags are indexed in store
        Thread.sleep(15000);


        try {
            if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
                org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponseStore = restAPIStore.getAPI(apiIdPublic);
                assertEquals(apiResponseStore.getId(), apiIdPublic, "Response object API ID mismatch");

                ApiResponse<TagListDTO> tagResponse = restAPIStore.tagsApi.tagsGetWithHttpInfo(25, 0, "", null);
                assertTrue(tagResponse.getData().getList().get(0).getValue().equals(tag), tag + " not visible in the store");
            }
            if (this.userMode.equals(TestUserMode.TENANT_USER)) {
                org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponseStore = restAPIStore.apIsApi.apisApiIdGet(apiIdPublic, "wso2.com", null);
                assertEquals(apiResponseStore.getId(), apiIdPublic, "Response object API ID mismatch");

                ApiResponse<TagListDTO> tagResponse = restAPIStore.tagsApi.tagsGetWithHttpInfo(25, 0, "wso2.com", null);
                assertTrue(tagResponse.getData().getList().get(0).getValue().equals(tag), tag + " not visible in the store");
            }
        } catch (ApiException e) {
            assertTrue(false, "Cannot get the API from the store for role " + ScenarioTestConstants.CREATOR_ROLE);
        }

        ApiClient existingAPIClient = restAPIStore.apIsApi.getApiClient();
        // Setting an unAuthenticatedClient.
        ApiClient unAuthenticatedApiClient = new ApiClient();
        unAuthenticatedApiClient.setBasePath(existingAPIClient.getBasePath());
        restAPIStore.apIsApi.setApiClient(unAuthenticatedApiClient);

        // Check visibility in store for unAuthenticated user.
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            ApiResponse<TagListDTO> tagResponse = restAPIStore.tagsApi.tagsGetWithHttpInfo(25, 0, "", null);
            assertTrue(tagResponse.getData().getList().get(0).getValue().equals(tag), tag + "not visible in the store");
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            ApiResponse<TagListDTO> tagResponse = restAPIStore.tagsApi.tagsGetWithHttpInfo(25, 0, "wso2.com", null);
            assertTrue(tagResponse.getData().getList().get(0).getValue().equals(tag), tag + "not visible in the store");
        }

        for (String id : apiIdList) {
            restAPIPublisher.deleteAPI(id);
        }
    }

    @Test(description = "1.5.2.9")
    public void testVisibilityOfTagsUsedByMultipleAPIsWithDistinctRoles() throws Exception {
        apiName = "APIVisibility_tagsDistinctRoles1";
        String tag = "tagsDistinctRoles";
        List<String> apiIdList = new ArrayList<>();

        restAPIPublisher.validateRoles(Base64.getUrlEncoder().encodeToString(ScenarioTestConstants.SUBSCRIBER_ROLE.getBytes()));
        restAPIPublisher.validateRoles(Base64.getUrlEncoder().encodeToString(ScenarioTestConstants.CREATOR_ROLE.getBytes()));
        APICreationRequestBean apiCreationRequestBeanCreator = new APICreationRequestBean(apiName, "/" + apiName, apiVersion, userName, new URL(backendEndPoint));
        apiCreationRequestBeanCreator.setRoles(ScenarioTestConstants.CREATOR_ROLE);
        apiCreationRequestBeanCreator.setVisibility(VISIBILITY_TYPE);
        apiCreationRequestBeanCreator.setTags(tag);

        String apiIdCreator = createAPI(apiCreationRequestBeanCreator);
        apiIdList.add(apiIdCreator);

        getAPI(apiIdCreator);
        publishAPI(apiIdCreator);

        apiName = "APIVisibility_tagsDistinctRoles2";

        APICreationRequestBean apiCreationRequestBeanSub = new APICreationRequestBean(apiName, "/" + apiName, apiVersion, userName, new URL(backendEndPoint));
        apiCreationRequestBeanSub.setRoles(ScenarioTestConstants.SUBSCRIBER_ROLE);
        apiCreationRequestBeanSub.setVisibility(VISIBILITY_TYPE);
        apiCreationRequestBeanSub.setTags(tag);

        String apiIdSub = createAPI(apiCreationRequestBeanSub);
        apiIdList.add(apiIdSub);

        getAPI(apiIdSub);
        publishAPI(apiIdSub);
        // Wait until the tags are indexed in store
        Thread.sleep(100000);

        try {
            if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
                org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponseStore = restAPIStore.getAPI(apiIdSub);
                assertEquals(apiResponseStore.getId(), apiIdSub, "Response object API ID mismatch");

                ApiResponse<TagListDTO> tagResponse = restAPIStore.tagsApi.tagsGetWithHttpInfo(25, 0, "", null);
                assertTrue(tagResponse.getData().getList().get(0).getValue().equals(tag), tag + " not visible in the store");
            }
            if (this.userMode.equals(TestUserMode.TENANT_USER)) {
                org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponseStore = restAPIStore.apIsApi.apisApiIdGet(apiIdSub, "wso2.com", null);
                assertEquals(apiResponseStore.getId(), apiIdSub, "Response object API ID mismatch");

                ApiResponse<TagListDTO> tagResponse = restAPIStore.tagsApi.tagsGetWithHttpInfo(25, 0, "wso2.com", null);
                assertTrue(tagResponse.getData().getList().get(0).getValue().equals(tag), tag + " not visible in the store");
            }
        } catch (ApiException e) {
            assertTrue(false, "Cannot get the API from the store for role " + ScenarioTestConstants.SUBSCRIBER_ROLE);
        }

        for (String id : apiIdList) {
            restAPIPublisher.deleteAPI(id);
        }

    }

//    // TODO: 2/27/19 Enable the test after identifying the cause of build failure
//    @Test(description = "1.5.2.10", dependsOnMethods = {"testVisibilityOfTagsUsedByMultipleAPIsWithDistinctRoles"}, enabled = false)
//    public void testListingOfRestrictedAPIsByTags() throws Exception {
//        String tag = "tagsDistinctRoles";
//
//        HttpResponse apisWithTagResponse = apiStoreClient.getAPIPageFilteredWithTags(tag);
//        Assert.assertNotNull(apisWithTagResponse, "Response object is null");
//        log.info("Response Code for get API by tag \'" + tag + "\': " + apisWithTagResponse.getResponseCode());
//        log.info("Response Message for get API by tag \'" + tag + "\': " + apisWithTagResponse.getData());
//        Assert.assertEquals(apisWithTagResponse.getResponseCode(), HttpStatus.SC_OK,
//                "Response code is not as expected for retrieving APIs by tag");
//        assertTrue(apisWithTagResponse.getData().contains("APIVisibility_tagsDistinctRoles2"),
//                "API \'APIVisibility_tagsDistinctRoles2\' is not visible to the user with restricted role");
//        assertTrue(!apisWithTagResponse.getData().contains("APIVisibility_tagsDistinctRoles1"),
//                "API \'APIVisibility_tagsDistinctRoles1\' is visible to the user without the restricted role");
//    }

    private String createAPI(APICreationRequestBean apiCreationRequest) throws Exception {
        APIDTO apidto = restAPIPublisher.addAPI(apiCreationRequest);
        String apiId = apidto.getId();
        assertNotNull(apiId, "API creation fails");
        return apiId;
    }

    private void publishAPI(String id) throws Exception {
        HttpResponse apiLifecycleResponse = restAPIPublisher.changeAPILifeCycleStatus(id, APILifeCycleAction.PUBLISH.getAction(), null);
    }

    public void getAPI(String id) throws Exception {
        HttpResponse apiResponseGetAPI = restAPIPublisher.getAPI(id);
        verifyResponse(apiResponseGetAPI);
        JSONObject responseJson = new JSONObject(apiResponseGetAPI.getData());
        assertEquals(responseJson.get("name").toString(), apiName, apiName + " is not updated");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            deleteUser(API_CREATOR_PUBLISHER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
            deleteUser(API_SUBSCRIBER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteUser(API_CREATOR_PUBLISHER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deleteUser(API_SUBSCRIBER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deactivateAndDeleteTenant(ScenarioTestConstants.TENANT_WSO2);
        }
    }

    @DataProvider
    public static Object[][] userModeDataProvider() throws Exception {
        setup();
        // return the relevant parameters for each test run
        // 1) Super tenant API creator
        // 2) Tenant API creator
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_USER},
                // new Object[]{TestUserMode.TENANT_USER},
        };
    }
}

