/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.integration.tests.restapi.admin;

import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.LabelDTO;
import org.wso2.am.integration.clients.admin.api.dto.LabelListDTO;
import org.wso2.am.integration.test.helpers.AdminApiTestHelper;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class LabelTestCase extends APIMIntegrationBaseTest {

    private LabelDTO labelDTO;
    private AdminApiTestHelper adminApiTestHelper;

    @Factory(dataProvider = "userModeDataProvider")
    public LabelTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN}};
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        adminApiTestHelper = new AdminApiTestHelper();
    }

    @Test(groups = {"wso2.am"}, description = "Test add label")
    public void testAddLabel() throws Exception {

        //Create the label DTO
        String name = "Test Label";
        String description = "This is a test label";
        List<String> accessUrls = Collections.singletonList("http://localhost:9443/");
        labelDTO = DtoFactory.createLabelDTO(name, description, accessUrls);

        //Add the label
        ApiResponse<LabelDTO> addedLabel = restAPIAdmin.addLabel(labelDTO);

        //Assert the status code and label ID
        Assert.assertEquals(addedLabel.getStatusCode(), HttpStatus.SC_CREATED);
        LabelDTO addedLabelDTO = addedLabel.getData();
        String labelId = addedLabelDTO.getId();
        Assert.assertNotNull(labelId, "The label ID cannot be null or empty");

        labelDTO.setId(labelId);
        //Verify the created label DTO
        adminApiTestHelper.verifyLabelDTO(labelDTO, addedLabelDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test get all labels", dependsOnMethods = "testAddLabel")
    public void testGetLabels() throws Exception {

        //Retrieve all Labels
        ApiResponse<LabelListDTO> retrievedLabels = restAPIAdmin.getLabels();
        Assert.assertEquals(retrievedLabels.getStatusCode(), HttpStatus.SC_OK);

        LabelListDTO labelListDTO = retrievedLabels.getData();
        List<LabelDTO> labelDTOS = labelListDTO.getList();
        //Verify the retrieved labels
        for (LabelDTO label : labelDTOS) {
            //Since there is only one label available which was added in the testAddLabel test, the global labelDTO
            //object is used to verify the retrieved label
            adminApiTestHelper.verifyLabelDTO(labelDTO, label);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test update label", dependsOnMethods = "testGetLabels")
    public void testUpdateLabel() throws Exception {

        //Update the label
        String updatedDescription = "This is a updated test label";
        labelDTO.setDescription(updatedDescription);
        ApiResponse<LabelDTO> updatedLabel = restAPIAdmin.updateLabel(labelDTO.getId(), labelDTO);
        LabelDTO updatedLabelDTO = updatedLabel.getData();
        Assert.assertEquals(updatedLabel.getStatusCode(), HttpStatus.SC_OK);

        //Verify the updated label DTO
        adminApiTestHelper.verifyLabelDTO(labelDTO, updatedLabelDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test delete label", dependsOnMethods = "testUpdateLabel")
    public void testDeleteLabel() throws Exception {

        ApiResponse<Void> apiResponse = restAPIAdmin.deleteLabel(labelDTO.getId());
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK);
    }

    @Test(groups = {"wso2.am"}, description = "Test delete label with non existing label ID",
            dependsOnMethods = "testDeleteLabel")
    public void testDeleteLabelWithNonExistingLabelId() {

        //Exception occurs when deleting a label with a non existing label ID. The status code in the Exception
        //object is used to assert this scenario
        try {
            //The label ID is created by combining two generated UUIDs
            restAPIAdmin.deleteLabel(UUID.randomUUID().toString() + UUID.randomUUID().toString());
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_NOT_FOUND);
        }
    }

}
