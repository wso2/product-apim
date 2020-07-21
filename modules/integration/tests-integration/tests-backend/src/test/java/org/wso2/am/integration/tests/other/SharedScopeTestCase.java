/*
 * Copyright (c) 2020, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ScopeDTO;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.util.ArrayList;
import java.util.List;

public class SharedScopeTestCase extends APIManagerLifecycleBaseTest {
    private final Log log = LogFactory.getLog(SharedScopeTestCase.class);

    private String sharedScopeName = "TestSharedScope";
    private String sharedScopeDisplayName = "Test Shared Scope";
    private String description = "This is a test shared scope";
    private String updatedDescription = "This is a updated test shared scope";
    private List<String> roles = new ArrayList<>();
    private String sharedScopeId;

    @Factory(dataProvider = "userModeDataProvider")
    public SharedScopeTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }, };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
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
    }

    @Test(groups = { "wso2.am" }, description = "Test get and update shared scope",
            dependsOnMethods = "testGetAndUpdateSharedScope")
    public void testDeleteSharedScope() throws Exception {
        restAPIPublisher.deleteSharedScope(sharedScopeId);
    }

}
