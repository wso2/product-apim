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

import static junit.framework.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ScopeBindingsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ScopeDTO;

import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;

public class SecureUsingUserRolesNegativeTestCase extends ScenarioTestBase {

    APIDTO apiDto;
    private APIDTO response;

    private String apiVersion = "1.0.0";
    private String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";
    private String apiContext = "testContext";

    List<APIOperationsDTO> operationsDTOS;

    private static final String ITEM_ADD = "item_add";
    private static final String ORDER_VIEW = "order_view";
    private static final String ORDER_ADD = "order_add";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_USERNAME = "admin@wso2.com";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";

    @Factory(dataProvider = "userModeDataProvider")
    public SecureUsingUserRolesNegativeTestCase(TestUserMode userMode) {
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

    @Test(description = "3.2.1.9", dataProvider = "ScopeAndInValidRoleDataProvider",
          dataProviderClass = SecureUsingUserRolesNegativeTestCase.class)
    public void testScopeCreationWithInValidRoles(String role, String scope) throws Exception {
        String apiName = "testAPI_1.9";

        createApiWithScope(apiName,role,scope);

        try {
            response = restAPIPublisher.addAPI(apiDto, "3.0");
        } catch (ApiException e) {
            assertTrue("Invalid Role was added successfully!", e.getResponseBody().contains("Role '"+ role +"' does not exist."));
        }
    }

    @Test(description = "3.2.1.13")
    public void testScopeWithDuplicateKey() throws Exception {

        String userRole = "admin" ;
        String scopeName = "duplicate_scope";
        String apiName = "testAPIWithDuplicateKey";
        String firstScopeDescription = "First Scope test Description";
        String secondScopeDescription = "Second Scope test Description";

        createApiWithScope(apiName,userRole,scopeName);

        ScopeDTO firstscopeDTO = new ScopeDTO();
        firstscopeDTO.setName(scopeName);
        firstscopeDTO.setDescription(firstScopeDescription);
        ScopeBindingsDTO scopeBindingsDTO = new ScopeBindingsDTO();
        scopeBindingsDTO.setType(null);
        List<String> bindingList = new ArrayList<>();
        bindingList.add(userRole);
        scopeBindingsDTO.setValues(bindingList);
        firstscopeDTO.setBindings(scopeBindingsDTO);

        ScopeDTO secondscopeDTO = new ScopeDTO();
        secondscopeDTO.setName(scopeName);
        secondscopeDTO.setDescription(secondScopeDescription);
        ScopeBindingsDTO secscopeBindingsDTO = new ScopeBindingsDTO();
        secscopeBindingsDTO.setType(null);
        List<String> secbindingList = new ArrayList<>();
        secbindingList.add(userRole);
        secscopeBindingsDTO.setValues(secbindingList);
        secondscopeDTO.setBindings(secscopeBindingsDTO);

        List<ScopeDTO> scopeDTOList = new ArrayList<>();
        scopeDTOList.add(firstscopeDTO);
        scopeDTOList.add(secondscopeDTO);

        apiDto.setScopes(scopeDTOList);

        try {
            response = restAPIPublisher.addAPI(apiDto, "3.0");
        } catch (ApiException e) {
            assertFalse( e.getResponseBody().contains("Error"), e.getResponseBody());
        }

        boolean duplicateScope = false;
        if(response.getScopes().toString().contains(firstScopeDescription) && response.getScopes().toString().contains(secondScopeDescription)){
            duplicateScope = true;   
        }
        assertFalse(duplicateScope,"Duplicate Scopes were added.");

        restAPIPublisher.deleteAPI(response.getId());

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
        ScopeBindingsDTO scopeBindingsDTO = new ScopeBindingsDTO();
        scopeBindingsDTO.setType(null);
        List<String> bindingList = new ArrayList<>();
        bindingList.add(role);
        scopeBindingsDTO.setValues(bindingList);
        scopeDTO.setBindings(scopeBindingsDTO);
        List<ScopeDTO> scopeDTOList = new ArrayList<>();
        scopeDTOList.add(scopeDTO);
        apiDto.setScopes(scopeDTOList);
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
            deleteUser(API_CREATOR_PUBLISHER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
            deleteUser(API_SUBSCRIBER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteUser(API_CREATOR_PUBLISHER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deleteUser(API_SUBSCRIBER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deactivateAndDeleteTenant(ScenarioTestConstants.TENANT_WSO2);
        }
    }

    @DataProvider(name = "ScopeAndInValidRoleDataProvider")
    public static Object[][] ValidRoleDataProvider() {
        return new Object[][]{
            {"everyone", ITEM_ADD},
            {"admn", ORDER_ADD},
            {"Internal/Craetor", ORDER_VIEW}
        };
    }

    @DataProvider
    public static Object[][] userModeDataProvider() throws Exception {
        setup();
        // return the relevant parameters for each test run
        // 1) Super tenant API creator
        // 2) Tenant API creator
        return new Object[][] {
            new Object[] {TestUserMode.SUPER_TENANT_USER},
            new Object[] {TestUserMode.TENANT_USER},
            };
    }
}
