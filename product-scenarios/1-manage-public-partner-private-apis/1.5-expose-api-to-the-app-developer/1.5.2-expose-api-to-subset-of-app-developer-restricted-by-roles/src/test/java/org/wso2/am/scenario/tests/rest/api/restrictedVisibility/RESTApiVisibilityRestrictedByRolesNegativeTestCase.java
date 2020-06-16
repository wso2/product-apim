
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

import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.scenario.test.common.APIConstants;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

public class RESTApiVisibilityRestrictedByRolesNegativeTestCase extends ScenarioTestBase {

    private APIPublisherRestClient apiPublisher;
    private String apiName;
    private String apiContext;
    private String apiVersion = "1.0.0";

    private String apiName2 = "RestrictedAPITagTest";
    private String apiContext2 = "/tag-test";
    private String apiVersion2 = "1.0.0";

    private String apiVisibility = "restricted";
    private String apiResource = "/find";
    private String tierCollection = "Gold,Bronze";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";

    private final String ADMIN_LOGIN_USERNAME = "admin";
    private final String ADMIN_PASSWORD = "admin";
    private final String SUBSCRIBER_USERNAME = "subscriberUser2";
    private final String SUBSCRIBER_PASSWORD = "password@123";
    private final String SUPER_TENANT_USER_USERNAME = "APIVisiSuperTenantUserNeg";
    private final String SUPER_TENANT_USER_PASSWORD = "APIVisiSuperTenantUserNeg";
    private final String TENANT_SUBSCRIBER_USERNAME = "APIVisiTenantSubscriberNeg";
    private final String TENANT_SUBSCRIBER_PASSWORD = "APIVisiTenantSubscriberNeg";
    private final String VISIBILITY_TYPE = "store";
    private final String CREATOR_ROLE = "Creator";
    private final String SUBSCRIBER_ROLE = "Subscriber";

    private final String API_DEVELOPER_USER = "api-dev-user1";
    private final String API_DEVELOPER_USER_PWD = "api-dev-user1";

    private APIStoreRestClient apiStoreClient;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        apiPublisher = new APIPublisherRestClient(publisherURL);
        apiStoreClient = new APIStoreRestClient(storeURL);

        String[] permission = new String[]{"/permission/admin/login",
                "/permission/admin/manage/api/subscribe"};
        
        createRole(ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD, SUBSCRIBER_ROLE, permission);
        createUserWithSubscriberRole(SUBSCRIBER_USERNAME, SUBSCRIBER_PASSWORD, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        createUserWithPublisherAndCreatorRole(API_DEVELOPER_USER, API_DEVELOPER_USER_PWD, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
    }

    @Test(description = "1.5.2.3")
    public void testVisibilityOfAPISLoginUserWithIncompatibleRole() throws Exception {
        apiName = "PhoneVerificationOptionalAdd";
        apiContext = "/phoneverify";

        APIRequest apiRequest = new APIRequest(apiName, apiContext, apiVisibility, SUBSCRIBER_ROLE, VISIBILITY_TYPE, apiVersion, apiResource,
                tierCollection, new URL(backendEndPoint));

        apiPublisher.login(API_DEVELOPER_USER, API_DEVELOPER_USER_PWD);
        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(apiCreationResponse);

        HttpResponse apiResponsePublisher = apiPublisher.getAPI(apiName, API_DEVELOPER_USER, apiVersion);
        verifyResponse(apiResponsePublisher);
        assertTrue(apiResponsePublisher.getData().contains(apiName), apiName + " is not visible in publisher");

        APILifeCycleStateRequest updateLifeCycle =
                new APILifeCycleStateRequest(apiName, API_DEVELOPER_USER, APILifeCycleState.PUBLISHED);
        HttpResponse apiPublishStatusResponse = apiPublisher.changeAPILifeCycleStatus(updateLifeCycle);
        verifyResponse(apiPublishStatusResponse);
        assertTrue(apiPublishStatusResponse.getData().contains("PUBLISHED"));

        apiStoreClient.login(SUBSCRIBER_USERNAME, SUBSCRIBER_PASSWORD);
        HttpResponse apiResponseStore = apiStoreClient.getAllPublishedAPIs();
        verifyResponse(apiResponseStore);
        assertFalse(apiResponseStore.getData().contains(apiName));
    }

    @Test(description = "1.5.2.6")
    public void testTagCloudContainsRestrictedTag() throws Exception {
        final String restrictedTag = "restricted-tag";
        APIRequest apiRequest = new APIRequest(apiName2, apiContext2, apiVisibility, ADMIN_LOGIN_USERNAME, apiVersion,
                apiResource, "",
                restrictedTag, tierCollection, backendEndPoint, "", "", "", "", "",
                "", "", "", APIConstants.DefaultVersion.ENABLED, APIConstants.ResponseCaching.DISABLED, "0",
                APIConstants.SubscriptionAvailability.ALL_TENANTS, APIConstants.TRANSPORT.HTTP, "", "", "");

        apiPublisher.login(API_DEVELOPER_USER, API_DEVELOPER_USER_PWD);
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(serviceResponse);
        apiPublisher
                .changeAPILifeCycleStatusToPublish(new APIIdentifier(API_DEVELOPER_USER, apiName2, apiVersion2), false);

        apiStoreClient.login(SUBSCRIBER_USERNAME, SUBSCRIBER_PASSWORD);
        HttpResponse serviceResponseGetAllTags = apiStoreClient.getAllTags();
        verifyResponse(serviceResponseGetAllTags);

        JSONObject tagsResponseData = new JSONObject(serviceResponseGetAllTags.getData());
        assertFalse(isTagsResponseContainsTag(tagsResponseData, restrictedTag), "Restricted tag was"
                + " returned in the tag cloud but it is not expected to be returned.");
    }

    @Test(description = "1.5.2.2")
    public void testCreateAPIWithInvalidRoleInStoreVisibility() throws Exception {
        apiPublisher.login(API_DEVELOPER_USER, API_DEVELOPER_USER_PWD);
        HttpResponse checkValidationRole = apiPublisher.validateRoles(CREATOR_ROLE);
        assertFalse(checkValidationRole.getData().contains("true"));
        verifyResponse(checkValidationRole);
    }

    @Test(description = "1.5.2.11")
    public void testAPIVisibilityRestrictedByRoleAndTenantType() throws Exception {
        //Add and activate wso2.com tenant
        addTenantAndActivate(ScenarioTestConstants.TENANT_WSO2, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        createUser(SUPER_TENANT_USER_USERNAME, SUPER_TENANT_USER_PASSWORD, new String[]{
                ScenarioTestConstants.CREATOR_ROLE, ScenarioTestConstants.PUBLISHER_ROLE,
                ScenarioTestConstants.SUBSCRIBER_ROLE}, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        // create user in wso2.com tenant
        createUserWithSubscriberRole(TENANT_SUBSCRIBER_USERNAME, TENANT_SUBSCRIBER_PASSWORD,
                ADMIN_LOGIN_USERNAME + "@" + ScenarioTestConstants.TENANT_WSO2, ADMIN_PASSWORD);

        apiPublisher.login(SUPER_TENANT_USER_USERNAME, SUPER_TENANT_USER_PASSWORD);
        APIRequest apiRequest = new APIRequest("APIVisibility_ByRoleAndTenant", apiContext, apiVisibility,
                SUBSCRIBER_ROLE, VISIBILITY_TYPE, apiVersion, apiResource, tierCollection, new URL(backendEndPoint));
        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(apiCreationResponse);

        APILifeCycleStateRequest updateLifeCycle =
                new APILifeCycleStateRequest("APIVisibility_ByRoleAndTenant", SUPER_TENANT_USER_USERNAME,
                        APILifeCycleState.PUBLISHED);
        HttpResponse apiPublishStatusResponse = apiPublisher.changeAPILifeCycleStatus(updateLifeCycle);
        verifyResponse(apiPublishStatusResponse);
        assertTrue(apiPublishStatusResponse.getData().contains("PUBLISHED"));

        apiStoreClient.login(SUPER_TENANT_USER_USERNAME, SUPER_TENANT_USER_PASSWORD);
        isAPIVisibleInStore("APIVisibility_ByRoleAndTenant", apiStoreClient);
        apiStoreClient.login(TENANT_SUBSCRIBER_USERNAME+ "@" + ScenarioTestConstants.TENANT_WSO2,
                TENANT_SUBSCRIBER_PASSWORD);
        isAPINotVisibleInStore("APIVisibility_ByRoleAndTenant", apiStoreClient);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiPublisher.login(API_DEVELOPER_USER, API_DEVELOPER_USER_PWD);
        apiPublisher.deleteAPI(apiName, apiVersion, API_DEVELOPER_USER);
        apiPublisher.deleteAPI(apiName2, apiVersion2, API_DEVELOPER_USER);
        apiPublisher.login(SUPER_TENANT_USER_USERNAME, SUPER_TENANT_USER_PASSWORD);
        apiPublisher.deleteAPI("APIVisibility_ByRoleAndTenant", apiVersion, SUPER_TENANT_USER_USERNAME);
        deleteUser(SUBSCRIBER_USERNAME, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        deleteUser(API_DEVELOPER_USER, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        deleteUser(SUPER_TENANT_USER_USERNAME, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
        deleteUser(TENANT_SUBSCRIBER_USERNAME, ADMIN_LOGIN_USERNAME + "@" +
                ScenarioTestConstants.TENANT_WSO2, ADMIN_PASSWORD);
        deleteRole(SUBSCRIBER_ROLE, ADMIN_LOGIN_USERNAME, ADMIN_PASSWORD);
    }
}
