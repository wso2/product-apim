
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

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.TagDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.TagListDTO;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class RESTApiVisibilityRestrictedByRolesNegativeTestCase extends ScenarioTestBase {

    private String apiName;
    private String apiContext;
    private String apiVersion = "1.0.0";

    private String apiName2 = "RestrictedAPITagTest";
    private String apiContext2 = "/tag-test";
    private String apiContext3 = "/tag-test3";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";

    private final String ADMIN_LOGIN_USERNAME = "admin";
    private final String ADMIN_PASSWORD = "admin";
    private final String SUPER_TENANT_USER_USERNAME = "APIVisiSuperTenantUserNeg";
    private final String SUPER_TENANT_USER_PASSWORD = "APIVisiSuperTenantUserNeg";
    private final String TENANT_SUBSCRIBER_USERNAME = "APIVisiTenantSubscriberNeg";
    private final String TENANT_SUBSCRIBER_PASSWORD = "APIVisiTenantSubscriberNeg";
    private final String VISIBILITY_TYPE = "store";
    private final String CREATOR_ROLE = "Creator";
    private final String SUBSCRIBER_ROLE = "Subscriber";

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_USERNAME = "admin@wso2.com";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";
    private static String apiID1;
    private static String apiID2;
    private static String apiID3;


    @Factory(dataProvider = "userModeDataProvider")
    public RESTApiVisibilityRestrictedByRolesNegativeTestCase(TestUserMode userMode) {
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
//                Add and activate wso2.com tenant
                createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                        TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
                createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, TENANT_ADMIN_USERNAME,
                        TENANT_ADMIN_PW);
            }
        }
        super.init(userMode);
    }


    @Test(description = "1.5.2.3")
    public void testVisibilityOfAPISLoginUserWithIncompatibleRole() throws Exception {
        apiName = "PhoneVerificationOptionalAdd";
        apiContext = "/phoneverify";

        APIDTO apiDto = new APIDTO();
        apiDto.setName(apiName);
        apiDto.setContext(apiContext);
        apiDto.setVersion(apiVersion);
        List<String> visibleRoles = new ArrayList<>();
        visibleRoles.add(SUBSCRIBER_ROLE);
        apiDto.setVisibility(APIDTO.VisibilityEnum.RESTRICTED);
        apiDto.setVisibleRoles(visibleRoles);

        org.json.simple.JSONObject jsonObject = new org.json.simple.JSONObject();
        jsonObject.put("endpoint_type", "http");
        org.json.simple.JSONObject sandUrl = new org.json.simple.JSONObject();
        sandUrl.put("url", backendEndPoint);
        jsonObject.put("sandbox_endpoints", sandUrl);
        jsonObject.put("production_endpoints", sandUrl);
        apiDto.setEndpointConfig(jsonObject);
        List<String> policies = new ArrayList<>();
        policies.add("Gold");
        policies.add("Unlimited");
        apiDto.setPolicies(policies);
        APIDTO apiDtoResponse = restAPIPublisher.addAPI(apiDto, "3.0");
        apiID1 = apiDtoResponse.getId();

        HttpResponse apiResponsePublisher = restAPIPublisher.getAPI(apiID1);
        verifyResponse(apiResponsePublisher);
        assertTrue(apiResponsePublisher.getData().contains(apiName), apiName + " is not visible in publisher");

        restAPIPublisher
                .changeAPILifeCycleStatus(apiID1, APILifeCycleAction.PUBLISH.getAction(), null);
        try {
            org.wso2.am.integration.clients.store.api.v1.dto.APIDTO api = restAPIStore.getAPI(apiID1);
            fail("API can be retrieved by restricted users. ");
        } catch (ApiException e) {
            assertEquals(e.getCode(), HttpURLConnection.HTTP_NOT_FOUND, "API can be retrieved by restricted users. ");
        }
    }

    @Test(description = "1.5.2.6", dependsOnMethods = "testVisibilityOfAPISLoginUserWithIncompatibleRole")
    public void testTagCloudContainsRestrictedTag() throws Exception {
        final String restrictedTag = "restricted-tag";

        APIDTO apiDto = new APIDTO();
        apiDto.setName(apiName2);
        apiDto.setContext(apiContext2);
        apiDto.setVersion(apiVersion);
        List<String> tags = new ArrayList<>();
        tags.add(restrictedTag);
        apiDto.setTags(tags);
        List<String> visibleRoles = new ArrayList<>();
        visibleRoles.add(SUBSCRIBER_ROLE);
        apiDto.setVisibility(APIDTO.VisibilityEnum.RESTRICTED);
        apiDto.setVisibleRoles(visibleRoles);

        org.json.simple.JSONObject jsonObject = new org.json.simple.JSONObject();
        jsonObject.put("endpoint_type", "http");
        org.json.simple.JSONObject sandUrl = new org.json.simple.JSONObject();
        sandUrl.put("url", backendEndPoint);
        jsonObject.put("sandbox_endpoints", sandUrl);
        jsonObject.put("production_endpoints", sandUrl);
        apiDto.setEndpointConfig(jsonObject);
        List<String> policies = new ArrayList<>();
        policies.add("Gold");
        policies.add("Unlimited");
        apiDto.setPolicies(policies);
        APIDTO apiDtoResponse = restAPIPublisher.addAPI(apiDto, "3.0");
        apiID2 = apiDtoResponse.getId();

        restAPIPublisher
                .changeAPILifeCycleStatus(apiID2, APILifeCycleAction.PUBLISH.getAction(), null);

        TagListDTO allTags = restAPIStore.getAllTags();

        if (allTags.getCount().equals(0)) {
            assertEquals((int) allTags.getCount(), 0, "Restricted tag was"
                    + " returned in the tag cloud but it is not expected to be returned.");
        } else {
            for (TagDTO tag : allTags.getList()) {
                if (tag.getValue().equals(restrictedTag)) {
                    fail("Restricted tag was  returned in the tag cloud but it is not expected to be returned.");
                }
            }
        }
    }

    @Test(description = "1.5.2.2", dependsOnMethods = "testAPIVisibilityRestrictedByRoleAndTenantType")
    public void testCreateAPIWithInvalidRoleInStoreVisibility() throws Exception {
        try {
            HttpResponse response = restAPIPublisher.validateRoles(Base64.getEncoder().encodeToString(CREATOR_ROLE.getBytes()));
            if (response.getResponseCode() == 200) {
                fail("Incorrect role validated successfully");
            }
        } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
            assertEquals(e.getCode(), HttpURLConnection.HTTP_NOT_FOUND);
        }

    }

    @Test(description = "1.5.2.11", dependsOnMethods = "testTagCloudContainsRestrictedTag")
    public void testAPIVisibilityRestrictedByRoleAndTenantType() throws Exception {
        String tenantDomain = "abc.com";

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            addTenantAndActivate(tenantDomain, ADMIN_USERNAME, TENANT_ADMIN_PW);
            //Add and activate wso2.com tenant
            createUserWithPublisherAndCreatorRole(SUPER_TENANT_USER_USERNAME, SUPER_TENANT_USER_PASSWORD,
                    ADMIN_LOGIN_USERNAME + "@" + tenantDomain, TENANT_ADMIN_PW);
            // create user in wso2.com tenant
            createUserWithSubscriberRole(TENANT_SUBSCRIBER_USERNAME, TENANT_SUBSCRIBER_PASSWORD,
                    ADMIN_LOGIN_USERNAME + "@" + tenantDomain, TENANT_ADMIN_PW);

            APIDTO apiDto = new APIDTO();
            apiDto.setName("APIVisibility_ByRoleAndTenant");
            apiDto.setContext(apiContext3);
            apiDto.setVersion(apiVersion);
            List<String> visibleRoles = new ArrayList<>();
            visibleRoles.add(SUBSCRIBER_ROLE);
            apiDto.setVisibility(APIDTO.VisibilityEnum.RESTRICTED);
            apiDto.setVisibleRoles(visibleRoles);
            List<String> visibleTenants = new ArrayList<>();
            visibleTenants.add("abc.com");
            visibleTenants.add("wso2.com");
            apiDto.setVisibleTenants(visibleTenants);

            org.json.simple.JSONObject jsonObject = new org.json.simple.JSONObject();
            jsonObject.put("endpoint_type", "http");
            org.json.simple.JSONObject sandUrl = new org.json.simple.JSONObject();
            sandUrl.put("url", backendEndPoint);
            jsonObject.put("sandbox_endpoints", sandUrl);
            jsonObject.put("production_endpoints", sandUrl);
            apiDto.setEndpointConfig(jsonObject);
            List<String> policies = new ArrayList<>();
            policies.add("Gold");
            policies.add("Unlimited");
            apiDto.setPolicies(policies);
            APIDTO apiDtoResponse = restAPIPublisher.addAPI(apiDto, "3.0");
            apiID3 = apiDtoResponse.getId();

            restAPIPublisher
                    .changeAPILifeCycleStatus(apiID3, APILifeCycleAction.PUBLISH.getAction(), null);

            try {
                org.wso2.am.integration.clients.store.api.v1.dto.APIDTO api = restAPIStore.getAPI(apiID3);
                fail("API can be retrieved by restricted users. ");
            } catch (ApiException e) {
                assertEquals(e.getCode(), HttpURLConnection.HTTP_NOT_FOUND, "API can be retrieved by restricted users. ");
            }

        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIPublisher.deleteAPI(apiID1);
        restAPIPublisher.deleteAPI(apiID2);
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            restAPIPublisher.deleteAPI(apiID3);
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
                new Object[]{TestUserMode.TENANT_USER},
        };
    }
}
