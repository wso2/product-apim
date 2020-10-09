/*
 *Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.am.scenario.tests.api.secure.userRoles;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIScopeDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ScopeDTO;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class SecureUsingUserRolesTestCase extends ScenarioTestBase {

    APIDTO apiDto;
    private APIDTO response;


    private String devPortalUser;
    private String apiID;
    private String apiVersion = "1.0.0";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";
    private String apiContext = "testContext";

    List<APIOperationsDTO> operationsDTOS;

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_USERNAME = "admin@wso2.com";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";

    @Factory(dataProvider = "userModeDataProvider")
    public SecureUsingUserRolesTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                    ADMIN_USERNAME, ADMIN_PW);
            createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, ADMIN_USERNAME, ADMIN_PW);
            devPortalUser = "adminUser1";
        }

        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
//           create user in wso2.com tenant
            addTenantAndActivate(ScenarioTestConstants.TENANT_WSO2, ADMIN_USERNAME, ADMIN_PW);
            if (isActivated(ScenarioTestConstants.TENANT_WSO2)) {
//           Add and activate wso2.com tenant
                createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                        TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
                createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, TENANT_ADMIN_USERNAME,
                        TENANT_ADMIN_PW);
            }
        }
        super.init(userMode);
    }

    @Test(description = "3.2.1.1")
    public void testScopeCreationWithValidValues() throws Exception {
        String userRole = "role";
        String scopeName = "valid_scope";
        String apiName = "testAPI";

        String[] permissionArray = new String[]{"/permission/admin/login", "/permission/admin/manage/api/publish"};

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createRole(ADMIN_USERNAME, ADMIN_PW, userRole, permissionArray);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            createRole(TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW, userRole, permissionArray);
        }

        createApiWithScope(apiName, userRole, scopeName);
        response = restAPIPublisher.addAPI(apiDto, "3.0");
        assertNotNull(response.getId());
        apiID = response.getId();

        try {
            restAPIPublisher.changeAPILifeCycleStatus(response.getId(), APILifeCycleAction.PUBLISH.getAction(), null);
        } catch (ApiException e) {
            assertTrue(e.getResponseBody().contains("Error"));
        }
        restAPIPublisher.deleteAPI(apiID);

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            deleteRole(userRole, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteRole(userRole, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }

    }

    @Test(description = "3.2.1.2")
    public void testScopeAssigningToMultipleResources() throws Exception {
        String userRole = "role";
        String scopeName = "valid_scope";
        String apiName = "testAPIwithMultipleResources";

        String[] permissionArray = new String[]{"/permission/admin/login", "/permission/admin/manage/api/publish"};

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createRole(ADMIN_USERNAME, ADMIN_PW, userRole, permissionArray);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            createRole(TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW, userRole, permissionArray);
        }

        createApiWithScope(apiName, userRole, scopeName);

        APIOperationsDTO anotherOperationsDTO = new APIOperationsDTO();

        anotherOperationsDTO.setVerb("PUT");
        anotherOperationsDTO.setTarget("http://dummy.restapiexample.com/api/v1/employees");
        List<String> scopes = new ArrayList<>();
        scopes.add(scopeName);
        anotherOperationsDTO.setScopes(scopes);

        operationsDTOS.add(anotherOperationsDTO);
        apiDto.setOperations(operationsDTOS);

        response = restAPIPublisher.addAPI(apiDto, "3.0");
        assertNotNull(response.getId());

        try {
            restAPIPublisher.changeAPILifeCycleStatus(response.getId(), APILifeCycleAction.PUBLISH.getAction(), null);
        } catch (ApiException e) {
            assertTrue(e.getResponseBody().contains("Error"));
        }

        restAPIPublisher.deleteAPI(response.getId());

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            deleteRole(userRole, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteRole(userRole, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }

    }

    @Test(description = "3.2.1.3")
    public void testScopeWithMultipleRoles() throws Exception {

        String userRole = "role";
        String secondRole = "secondRole";
        String scopeName = "valid_scope";
        String secscopeName = "Second_valid_scope";
        String apiName = "testAPIMultipleRoles";

        String[] permissionArray = new String[]{"/permission/admin/login", "/permission/admin/manage/api/publish"};

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createRole(ADMIN_USERNAME, ADMIN_PW, secondRole, permissionArray);
            createRole(ADMIN_USERNAME, ADMIN_PW, userRole, permissionArray);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            createRole(TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW, userRole, permissionArray);
            createRole(TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW, secondRole, permissionArray);
        }

        createApiWithScope(apiName, userRole, scopeName);

        ScopeDTO firstscopeDTO = new ScopeDTO();
        firstscopeDTO.setName(scopeName);
        firstscopeDTO.setDescription("First Scope test Description");
        List<String> bindingList = new ArrayList<>();
        bindingList.add(userRole);

        ScopeDTO secondscopeDTO = new ScopeDTO();
        secondscopeDTO.setName(secscopeName);
        secondscopeDTO.setDescription("Second Scope test Description");
        List<String> secbindingList = new ArrayList<>();
        secbindingList.add(userRole);

        APIScopeDTO firstAPIScopeDTO = new APIScopeDTO();
        firstAPIScopeDTO.setScope(firstscopeDTO);

        APIScopeDTO secondAPIScopeDTO = new APIScopeDTO();
        secondAPIScopeDTO.setScope(secondscopeDTO);

        List<APIScopeDTO> apiScopeDTOS = new ArrayList<>();
        apiScopeDTOS.add(firstAPIScopeDTO);
        apiScopeDTOS.add(secondAPIScopeDTO);

        apiDto.setScopes(apiScopeDTOS);

        response = restAPIPublisher.addAPI(apiDto, "3.0");
        assertNotNull(response.getId());

        try {
            restAPIPublisher.changeAPILifeCycleStatus(response.getId(), APILifeCycleAction.PUBLISH.getAction(), null);
        } catch (ApiException e) {
            assertTrue(e.getResponseBody().contains("Error"));
        }
        restAPIPublisher.deleteAPI(response.getId());

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            deleteRole(userRole, ADMIN_USERNAME, ADMIN_PW);
            deleteRole(secondRole, ADMIN_USERNAME, ADMIN_PW);

        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteRole(userRole, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deleteRole(secondRole, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
        }
    }

    private APIDTO createApiWithScope(String apiName, String role, String scopeName) throws ApiException {
        apiDto = new APIDTO();
        String verb = "GET";
        String tier = "Gold";
        apiDto.setName(apiName);
        apiDto.setContext(apiContext);
        apiDto.setVersion(apiVersion);

        org.json.simple.JSONObject jsonObject = new org.json.simple.JSONObject();
        jsonObject.put("endpoint_type", "http");
        org.json.simple.JSONObject sandUrl = new org.json.simple.JSONObject();
        sandUrl.put("url", backendEndPoint);
        jsonObject.put("sandbox_endpoints", sandUrl);
        jsonObject.put("production_endpoints", sandUrl);
        apiDto.setEndpointConfig(jsonObject);
        ArrayList<String> gatewayEnvironments = new ArrayList<>();
        gatewayEnvironments.add("Production and Sandbox");
        apiDto.setGatewayEnvironments(gatewayEnvironments);
        ArrayList<String> policies = new ArrayList<>();
        policies.add(tier);
        apiDto.setPolicies(policies);
        ScopeDTO scopeDTO = new ScopeDTO();
        scopeDTO.setName(scopeName);
        scopeDTO.setDescription("Scope test Description");
        List<String> bindingList = new ArrayList<>();
        bindingList.add(role);
        List<ScopeDTO> scopeDTOList = new ArrayList<>();
        scopeDTOList.add(scopeDTO);

        APIScopeDTO firstAPIScopeDTO = new APIScopeDTO();
        firstAPIScopeDTO.setScope(scopeDTO);

        List<APIScopeDTO> apiScopeDTOS = new ArrayList<>();
        apiScopeDTOS.add(firstAPIScopeDTO);

        apiDto.setScopes(apiScopeDTOS);
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb(verb);
        apiOperationsDTO.setTarget(backendEndPoint);
        List<String> scopes = new ArrayList<>();
        scopes.add(scopeName);
        apiOperationsDTO.setScopes(scopes);
        operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO);
        apiDto.setOperations(operationsDTOS);

        return apiDto;
    }


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            // deleteUser(API_CREATOR_PUBLISHER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
            // deleteUser(API_SUBSCRIBER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            // deleteUser(API_CREATOR_PUBLISHER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            // deleteUser(API_SUBSCRIBER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            // deactivateAndDeleteTenant(ScenarioTestConstants.TENANT_WSO2);
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
