/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ScopeDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SharedScopeTestWithRestart extends APIManagerLifecycleBaseTest {

    private String sharedScopeName = "TestSharedScopeWithRestart";
    private String sharedScopeDisplayName = "Test Shared Scope with Restart";
    private String description = "This is a test shared scope with Restart";
    private String updatedDescription = "This is a updated test shared scope with Restart";
    private String updatedDescription1 = "This is a updated test shared scope with Restart(2)";
    private String updatedDescription2 = "This is a updated test shared scope with Restart(3)";
    private List<String> roles = new ArrayList<>();
    private String sharedScopeId;
    private ServerConfigurationManager serverConfigurationManager;

    @Factory(dataProvider = "userModeDataProvider")
    public SharedScopeTestWithRestart(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN }};
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        superTenantKeyManagerContext = new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                APIMIntegrationConstants.AM_KEY_MANAGER_INSTANCE,
                TestUserMode.SUPER_TENANT_ADMIN);
        serverConfigurationManager = new ServerConfigurationManager(superTenantKeyManagerContext);
        serverConfigurationManager.applyConfiguration(new File(
                getAMResourceLocation() + File.separator + "scopes"
                        + File.separator + "deployment.toml"));
    }

    @Test(groups = { "wso2.am" }, description = "Test add shared scope")
    public void testAddSharedScope() throws Exception {
        ScopeDTO scopeDTO = new ScopeDTO();
        scopeDTO.setName(sharedScopeName);
        scopeDTO.setDisplayName(sharedScopeDisplayName);
        scopeDTO.setDescription(description);

        roles.add("Internal/publisher");
        roles.add("admin");
        scopeDTO.setBindings(roles);

        ScopeDTO addedScopeDTO = restAPIPublisher.addSharedScope(scopeDTO);
        sharedScopeId = addedScopeDTO.getId();
        Assert.assertNotNull(sharedScopeId, "The scope ID cannot be null or empty");
    }

    @Test(groups = { "wso2.am" }, description = "Test get and update shared scope",
            dependsOnMethods = "testAddSharedScope")
    public void testGetAndUpdateSharedScope() throws Exception {
        ScopeDTO sharedScopeDTO = restAPIPublisher.getSharedScopeById(sharedScopeId);
        Assert.assertEquals(sharedScopeDTO.getName(), sharedScopeName,
                "Shared scope name does not match with the expected name");
        Assert.assertEquals(sharedScopeDTO.getDisplayName(), sharedScopeDisplayName,
                "Shared scope display name does not match with the expected display name");
        Assert.assertTrue(sharedScopeDTO.getBindings().contains("admin"),
                "Shared scope does not include the expected role");

        sharedScopeDTO.setDescription(updatedDescription);
        ScopeDTO updateScopeDTO = restAPIPublisher.updateSharedScope(sharedScopeId, sharedScopeDTO);
        Assert.assertEquals(updateScopeDTO.getDescription(), updatedDescription,
                "Shared scope description does not match with the expected description");

        sharedScopeDTO.setDescription(updatedDescription1);
        updateScopeDTO = restAPIPublisher.updateSharedScope(sharedScopeId, sharedScopeDTO);
        Assert.assertEquals(updateScopeDTO.getDescription(), updatedDescription1,
                "Shared scope description does not match with the expected description");

        sharedScopeDTO.setDescription(updatedDescription2);
        updateScopeDTO = restAPIPublisher.updateSharedScope(sharedScopeId, sharedScopeDTO);
        Assert.assertEquals(updateScopeDTO.getDescription(), updatedDescription2,
                "Shared scope description does not match with the expected description");
    }

    @Test(groups = { "wso2.am" }, description = "Test delete shared scope",
            dependsOnMethods = "testGetAndUpdateSharedScope")
    public void testDeleteSharedScope() throws Exception {
        restAPIPublisher.deleteSharedScope(sharedScopeId);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
        serverConfigurationManager.restoreToLastConfiguration();
    }
}
