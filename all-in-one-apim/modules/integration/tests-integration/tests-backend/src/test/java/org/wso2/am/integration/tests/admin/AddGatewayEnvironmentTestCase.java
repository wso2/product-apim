/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package org.wso2.am.integration.tests.restapi.admin;

import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.api.dto.EnvironmentDTO;
import org.wso2.am.integration.clients.admin.api.dto.VHostDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.DtoFactory;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.util.ArrayList;
import java.util.List;

public class AddGatewayEnvironmentWithoutDisplayNameTestCase extends APIMIntegrationBaseTest {

    private String environmentId;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(TestUserMode.SUPER_TENANT_ADMIN);
    }

    @Test(groups = {"wso2.am"},
            description = "Test adding a gateway environment without a display name should succeed")
    public void testAddGatewayEnvironmentWithoutDisplayName() throws Exception {

        String name = "test-env-no-displayname";
        String displayName = "";
        String description = "Environment created without a display name";
        String provider = "wso2";

        VHostDTO vHostDTO = DtoFactory.createVHostDTO("localhost", "", 8280, 8243, 9099, 8099);
        List<VHostDTO> vHostDTOList = new ArrayList<>();
        vHostDTOList.add(vHostDTO);

        EnvironmentDTO environmentDTO = DtoFactory.createEnvironmentDTO(
                name, displayName, description, provider, false, vHostDTOList);

        try {
            EnvironmentDTO addedEnvironment = restAPIAdmin.addEnvironment(environmentDTO);
            Assert.assertNotNull(addedEnvironment);
            Assert.assertEquals(addedEnvironment.getName(), name);
            environmentId = addedEnvironment.getId();
        } catch (ApiException e) {
            Assert.fail("Adding gateway environment without display name failed: " + e.getMessage());
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (environmentId != null) {
            restAPIAdmin.deleteEnvironment(environmentId);
        }
    }
}

/*
PR title:
Add missing test case for gateway environment creation without display name

PR description:
The existing EnvironmentTestCase.java covers environments without VHosts,
with special characters, and with duplicate VHosts.
However, there was no test covering what happens when a gateway environment
is created without a display name. This PR adds that missing test case.

*/