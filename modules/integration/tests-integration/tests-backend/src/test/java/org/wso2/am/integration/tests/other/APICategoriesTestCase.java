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

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.APICategoryDTO;
import org.wso2.am.integration.clients.admin.api.dto.APICategoryListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.test.helpers.AdminApiTestHelper;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class APICategoriesTestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(APICategoriesTestCase.class);
    private AdminApiTestHelper adminApiTestHelper;
    private APICategoryDTO apiCategoryDTO;
    private String apiId;

    @Factory(dataProvider = "userModeDataProvider")
    public APICategoriesTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN },};
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        adminApiTestHelper = new AdminApiTestHelper();
    }

    @Test(groups = { "wso2.am" }, description = "Test add API category")
    public void testAddAPICategory() throws Exception {

        //Create the api category DTO
        String name = "Marketing";
        String description = "Marketing category";
        apiCategoryDTO = DtoFactory.createApiCategoryDTO(name, description);

        //Add the api category
        ApiResponse<APICategoryDTO> addedApiCategory = restAPIAdmin.addApiCategory(apiCategoryDTO);

        //Assert the status code and api category ID
        Assert.assertEquals(addedApiCategory.getStatusCode(), HttpStatus.SC_CREATED);
        APICategoryDTO addedApiCategoryDTO = addedApiCategory.getData();
        String apiCategoryId = addedApiCategoryDTO.getId();
        Assert.assertNotNull(apiCategoryId, "The api category ID cannot be null or empty");

        apiCategoryDTO.setId(apiCategoryId);
        //Verify the created api category DTO
        adminApiTestHelper.verifyApiCategoryDTO(apiCategoryDTO, addedApiCategoryDTO);
    }

    @Test(groups = { "wso2.am" }, description = "Test add API category without Name", dependsOnMethods = "testAddAPICategory")
    public void testAddAPICategoryWithoutName() {

        //Create the API Category DTO
        String description = "Marketing Category";
        APICategoryDTO apiCategoryDTO = DtoFactory.createApiCategoryDTO(null, description);
        //Add the API Category
        try {
            ApiResponse<APICategoryDTO> addedAPICategory = restAPIAdmin.addApiCategory(apiCategoryDTO);
            Assert.assertNotEquals(addedAPICategory.getStatusCode(), HttpStatus.SC_CREATED,
                    "API category was added without a name");
        } catch (ApiException e) {
            //Assert the Status Code
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test(groups = { "wso2.am" }, description = "Test add API category Name With Special Characters", dependsOnMethods = "testAddAPICategoryWithoutName")
    public void testAddAPICategoryNameWithSpecialCharacters() {

        //Create the API Category DTO
        String name = "Marketing Category";
        String description = "This is Marketing Category";
        APICategoryDTO apiCategoryDTO = DtoFactory.createApiCategoryDTO(name, description);

        //Add the API Category
        try {
            ApiResponse<APICategoryDTO> addedAPICategory = restAPIAdmin.addApiCategory(apiCategoryDTO);
            Assert.assertNotEquals(addedAPICategory.getStatusCode(), HttpStatus.SC_CREATED,
                    "API category was added with special characters in the name");
        } catch (ApiException e) {
            //Assert the Status Code
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test(groups = { "wso2.am" }, description = "Test add API category with duplicate name", dependsOnMethods = {
            "testAddAPICategoryNameWithSpecialCharacters" })
    public void addAPICategoryWithDuplicateName() {

        try {
            //Add the duplicate api category
            ApiResponse<APICategoryDTO> addedApiCategory = restAPIAdmin.addApiCategory(apiCategoryDTO);
            Assert.assertNotEquals(addedApiCategory.getStatusCode(), HttpStatus.SC_CREATED,
                    "Duplicate API category was added");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
            Assert.assertTrue(e.getResponseBody().contains("Category with name 'Marketing' already exists"));
        }
    }

    @Test(groups = { "wso2.am" }, description = "Test update API category", dependsOnMethods = {
            "addAPICategoryWithDuplicateName" })
    public void testUpdateAPICategory() throws Exception {
        String newDescription = "This is marketing category";
        apiCategoryDTO.setDescription(newDescription);

        ApiResponse<APICategoryDTO> updatedApiCategory = restAPIAdmin
                .updateApiCategory(apiCategoryDTO.getId(), apiCategoryDTO);
        APICategoryDTO updatedApiCategoryDTO = updatedApiCategory.getData();
        Assert.assertEquals(updatedApiCategory.getStatusCode(), HttpStatus.SC_OK);

        //Verify the updated api category DTO
        adminApiTestHelper.verifyApiCategoryDTO(apiCategoryDTO, updatedApiCategoryDTO);
    }

    @Test(groups = { "wso2.am" }, description = "Test get API categories",
            dependsOnMethods = { "testUpdateAPICategory" })
    public void testGetAPICategoriesFromAdminAPI() throws Exception {
        //Retrieve all api categories
        ApiResponse<APICategoryListDTO> retrievedApiCategories = restAPIAdmin.getApiCategories();
        Assert.assertEquals(retrievedApiCategories.getStatusCode(), HttpStatus.SC_OK);

        APICategoryListDTO apiCategoryListDTO = retrievedApiCategories.getData();
        List<APICategoryDTO> apiCategoryDTOS = apiCategoryListDTO.getList();
        //Verify the retrieved api categories
        for (APICategoryDTO apiCategory : apiCategoryDTOS) {
            //Since there is only one api category available, the global apiCategoryDTO
            //object is used to verify the retrieved api category
            adminApiTestHelper.verifyApiCategoryDTO(apiCategoryDTO, apiCategory);
        }
    }

    @Test(groups = { "wso2.am" }, description = "Test attach API category to API", dependsOnMethods = {
            "testGetAPICategoriesFromAdminAPI" })
    public void testAttachAPICategoryToAPI() throws Exception {

        //Add API
        String apiName = "CategoryTestAPI";
        String apiContext = "category";
        String apiVersion = "1.0";
        String url = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice";
        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(url));
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(user.getUserName());

        //Add the API using the API publisher.
        HttpResponse postResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = postResponse.getData();

        //update API with category mapping
        List<String> apiCategories = new ArrayList<>();
        apiCategories.add("Marketing");
        apiRequest.setApiCategories(apiCategories);
        HttpResponse updateResponse = restAPIPublisher.updateAPI(apiRequest, apiId);

        waitForAPIDeployment();
        HttpResponse getResponse = restAPIPublisher.getAPI(updateResponse.getData());

        Gson g = new Gson();
        APIDTO apidto = g.fromJson(getResponse.getData(), APIDTO.class);
        List<String> categoriesInReceivedAPI = apidto.getCategories();
        Assert.assertNotNull(categoriesInReceivedAPI);
        Assert.assertTrue(categoriesInReceivedAPI.contains("Marketing"));

        removeAPICategoryFromAPI(apiRequest);
    }

    private void removeAPICategoryFromAPI(APIRequest apiRequest) throws Exception {
        List<String> apiCategories = new ArrayList<>();
        apiRequest.setApiCategories(apiCategories);
        restAPIPublisher.updateAPI(apiRequest, apiId);
        waitForAPIDeployment();
    }

    @Test(groups = { "wso2.am" }, description = "Test delete API category", dependsOnMethods = {
            "testGetAPICategoriesFromAdminAPI" })
    public void testDeleteAPICategory() throws Exception {
        ApiResponse<Void> apiResponse = restAPIAdmin.deleteApiCategory(apiCategoryDTO.getId());
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
        super.cleanUp();
    }
}
