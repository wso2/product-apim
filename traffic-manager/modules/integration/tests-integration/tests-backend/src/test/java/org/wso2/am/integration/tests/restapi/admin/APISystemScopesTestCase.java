/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.wso2.am.integration.tests.restapi.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.testng.annotations.AfterClass;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.RoleAliasDTO;
import org.wso2.am.integration.clients.admin.api.dto.RoleAliasListDTO;
import org.wso2.am.integration.test.helpers.AdminApiTestHelper;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.tests.other.APIDenyPolicyTestCase;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class APISystemScopesTestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(APIDenyPolicyTestCase.class);
    private AdminApiTestHelper adminApiTestHelper;
    private RoleAliasListDTO roleList;

    @Factory(dataProvider = "userModeDataProvider")
    public APISystemScopesTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN }};
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        adminApiTestHelper = new AdminApiTestHelper();
    }

    @Test(groups = { "wso2.am" }, description = "Test add scope mapping")
    public void testAddScopeMapping() throws ApiException {

        roleList = new RoleAliasListDTO();
        RoleAliasDTO roleAliasDTO = new RoleAliasDTO();
        List<String> roleAliasList = new ArrayList<>();

        String role = "admin";
        String alias = "testRole";
        int count = 1;


        roleAliasDTO.setRole(role);
        roleAliasList.add(alias);
        roleAliasDTO.setAliases(roleAliasList);
        roleList.setCount(count);
        roleList.setList(Arrays.asList(roleAliasDTO));

        ApiResponse<RoleAliasListDTO> response = restAPIAdmin.putRoleAliases(roleList);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.SC_OK);

        adminApiTestHelper.verifyRoleAliasListDTO(response.getData(),roleList);

    }

    @Test(groups = { "wso2.am" }, description = "Test get scope mapping", dependsOnMethods = "testAddScopeMapping")
    public void testGetScopeMapping() throws ApiException {
        ApiResponse<RoleAliasListDTO> response = restAPIAdmin.getRoleAliases();
        Assert.assertEquals(response.getStatusCode(), HttpStatus.SC_OK);

        adminApiTestHelper.verifyRoleAliasListDTO(response.getData(), roleList);
    }

    @Test(groups = { "wso2.am" }, description = "Test delete scope mapping", dependsOnMethods = "testGetScopeMapping")
    public void testDeleteRoleAliasMapping() throws ApiException {

        //Delete role alias mapping
        List<RoleAliasDTO> deletedRoleAliasMapping = roleList.getList().
                stream().
                filter(item -> !item.getAliases().contains("testRole")).
                collect(Collectors.toList());

        RoleAliasListDTO updatedRoleAliasListDTO = new RoleAliasListDTO();
        updatedRoleAliasListDTO.setList(deletedRoleAliasMapping);
        updatedRoleAliasListDTO.setCount(0);

        ApiResponse<RoleAliasListDTO> response = restAPIAdmin.putRoleAliases(updatedRoleAliasListDTO);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.SC_OK);

        adminApiTestHelper.verifyRoleAliasListDTO(response.getData(), updatedRoleAliasListDTO);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }

}
