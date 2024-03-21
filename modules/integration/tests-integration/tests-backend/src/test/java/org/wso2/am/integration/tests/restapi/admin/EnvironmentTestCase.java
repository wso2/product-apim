/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.dto.EnvironmentDTO;
import org.wso2.am.integration.clients.admin.api.dto.EnvironmentListDTO;
import org.wso2.am.integration.clients.admin.api.dto.VHostDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIProductDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIEndpointURLsDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.helpers.AdminApiTestHelper;
import org.wso2.am.integration.test.impl.ApiProductTestHelper;
import org.wso2.am.integration.test.impl.ApiTestHelper;
import org.wso2.am.integration.test.impl.DtoFactory;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRevisionDeployUndeployRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionRequest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

public class EnvironmentTestCase extends APIMIntegrationBaseTest {

    private EnvironmentDTO environmentDTO;
    private AdminApiTestHelper adminApiTestHelper;
    private ApiTestHelper apiTestHelper;
    private ApiProductTestHelper apiProductTestHelper;

    private String apiOneId;
    private String apiTwoId;
    private String apiProductId;
    private String apiOneRevisionId;
    private String apiTwoRevisionId;
    private String apiProductRevisionId;
    private static final String TIER_UNLIMITED = "Unlimited";
    private static final String TIER_GOLD = "Gold";

    @Factory(dataProvider = "userModeDataProvider")
    public EnvironmentTestCase(TestUserMode userMode) {
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
        apiTestHelper = new ApiTestHelper(restAPIPublisher, restAPIStore, getAMResourceLocation(),
                keyManagerContext.getContextTenant().getDomain(), keyManagerHTTPSURL, user);
        apiProductTestHelper = new ApiProductTestHelper(restAPIPublisher, restAPIStore);
        adminApiTestHelper = new AdminApiTestHelper();
    }

    @Test(groups = {"wso2.am"}, description = "Test add gateway environment without VHost")
    public void testAddGatewayEnvironmentWithoutVHost() throws Exception {
        //Create the environment DTO
        String name = "asia-region";
        String displayName = "Asia Region";
        String description = "Gateway environment deployed in Asia region";
        String provider = Constants.WSO2_GATEWAY_ENVIRONMENT;
        List<VHostDTO> vHostDTOList = new ArrayList<>();
        environmentDTO = DtoFactory.createEnvironmentDTO(name, displayName, description, provider,
                false, vHostDTOList, null);

        //Add the environment
        try {
            restAPIAdmin.addEnvironment(environmentDTO);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }


    @Test(groups = {"wso2.am"}, description = "Test adding gateway environment name with special characters",
            dependsOnMethods = "testAddGatewayEnvironmentWithoutVHost")
    public void testAddingGatewayEnvironmentNameWithSpecialCharacters() throws Exception {
        //Create the environment DTO
        String name = "asia-region#$";
        String displayName = "Asia Region #$";
        String description = "Gateway environment deployed in Asia region";
        String provider = Constants.WSO2_GATEWAY_ENVIRONMENT;
        List<VHostDTO> vHostDTOList = new ArrayList<>();
        vHostDTOList.add(DtoFactory.createVhostDTO("foods.com", "zfoods",
                8280, 8243, 9099, 8099));
        environmentDTO = DtoFactory.createEnvironmentDTO(name, displayName, description, provider,
                false, vHostDTOList, null);
        //Add the environment
        try {
            restAPIAdmin.addEnvironment(environmentDTO);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test adding gateway environment without displayName",
            dependsOnMethods = "testAddingGatewayEnvironmentNameWithSpecialCharacters")
    public void testAddingGatewayEnvironmentWithoutDisplayName() throws Exception {
        //Create the environment DTO
        String name = "asia-region";
        String displayName = null;
        String description = "Gateway environment deployed in Asia region";
        String provider = Constants.WSO2_GATEWAY_ENVIRONMENT;
        List<VHostDTO> vHostDTOList = new ArrayList<>();
        vHostDTOList.add(DtoFactory.createVhostDTO("foods.com", "zfoods",
                8280, 8243, 9099, 8099));
        environmentDTO = DtoFactory.createEnvironmentDTO(name, displayName, description, provider,
                false, vHostDTOList, null);
        //Able to add the environment successfully
        ApiResponse<EnvironmentDTO> addedEnvironments = restAPIAdmin.addEnvironment(environmentDTO);

        //Assert the status code and environment ID
        Assert.assertEquals(addedEnvironments.getStatusCode(), HttpStatus.SC_CREATED);
        EnvironmentDTO addedEnvironmentDTO = addedEnvironments.getData();
        String environmentId = addedEnvironmentDTO.getId();
        Assert.assertNotNull(environmentId, "The environment ID cannot be null or empty");
    }

    @Test(groups = {"wso2.am"}, description = "Test adding gateway environment with Gateway Type configured",
            dependsOnMethods = "testAddingGatewayEnvironmentWithoutDisplayName")
    public void testAddingGatewayEnvironmentWithGatewayType() throws Exception {
        //Create the environment DTO
        String name = "asia-region-gateway-type";
        String displayName = "Asia Region";
        String description = "Gateway environment deployed in Asia region";
        String provider = Constants.WSO2_GATEWAY_ENVIRONMENT;
        String gatewayType = "APK";
        List<VHostDTO> vHostDTOList = new ArrayList<>();
        vHostDTOList.add(DtoFactory.createVhostDTO("foods.com", "zfoods",
                8280, 8243, null, null));
        environmentDTO = DtoFactory.createEnvironmentDTO(name, displayName, description, provider,
                false, vHostDTOList, gatewayType);
        //Able to add the environment successfully
        ApiResponse<EnvironmentDTO> addedEnvironments = restAPIAdmin.addEnvironment(environmentDTO);

        //Assert the status code and environment ID
        Assert.assertEquals(addedEnvironments.getStatusCode(), HttpStatus.SC_CREATED);
        EnvironmentDTO addedEnvironmentDTO = addedEnvironments.getData();
        String environmentId = addedEnvironmentDTO.getId();
        Assert.assertNotNull(environmentId, "The environment ID cannot be null or empty");
        String addedGatewayType = addedEnvironmentDTO.getGatewayType();
        Assert.assertEquals(addedGatewayType, gatewayType, "The added gateway type is not matching with the expected");
    }

    @Test(groups = {"wso2.am"}, description = "Test adding gateway environment with multiple Vhosts with same hostname",
            dependsOnMethods = "testAddingGatewayEnvironmentWithGatewayType")
    public void testAddingGatewayEnvironmentWithMultipleVhostsWithSameHostName() throws Exception {
        //Create the environment DTO
        String name = "asia-region";
        String displayName = "Asia Region";
        String description = "Gateway environment deployed in Asia region";
        String provider = Constants.WSO2_GATEWAY_ENVIRONMENT;
        List<VHostDTO> vHostDTOList = new ArrayList<>();
        vHostDTOList.add(DtoFactory.createVhostDTO("foods.com", "zfoods",
                8280, 8243, 9099, 8099));
        vHostDTOList.add(DtoFactory.createVhostDTO("foods.com", "zfoods",
                8280, 8243, 9099, 8099));
        environmentDTO = DtoFactory.createEnvironmentDTO(name, displayName, description, provider,
                false, vHostDTOList, null);
        //Add the environment
        try {
            restAPIAdmin.addEnvironment(environmentDTO);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test adding gateway environment with vhost hostname having special characters",
            dependsOnMethods = "testAddingGatewayEnvironmentWithMultipleVhostsWithSameHostName")
    public void testAddingGatewayEnvironmentWithVhostsHavingSpecialCharacters() throws Exception {
        //Create the environment DTO
        String name = "asia-region";
        String displayName = "Asia Region";
        String description = "Gateway environment deployed in Asia region";
        String provider = Constants.WSO2_GATEWAY_ENVIRONMENT;
        List<VHostDTO> vHostDTOList = new ArrayList<>();
        vHostDTOList.add(DtoFactory.createVhostDTO("foods.com#$%?", "zfoods",
                8280, 8243, 9099, 8099));
        environmentDTO = DtoFactory.createEnvironmentDTO(name, displayName, description, provider,
                false, vHostDTOList, null);
        //Add the environment
        try {
            restAPIAdmin.addEnvironment(environmentDTO);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test add gateway environment with single VHost",
            dependsOnMethods = "testAddingGatewayEnvironmentWithVhostsHavingSpecialCharacters")
    public void testAddGatewayEnvironmentSingleVHost() throws Exception {
        //Create the environment DTO
        String name = "europe-region";
        String displayName = "Europe Region";
        String description = "Gateway environment deployed in Europe region";
        String provider = Constants.WSO2_GATEWAY_ENVIRONMENT;
        List<VHostDTO> vHostDTOList = new ArrayList<>();
        vHostDTOList.add(DtoFactory.createVhostDTO("foods.com", "zfoods",
                8280, 8243, 9099, 8099));
        environmentDTO = DtoFactory.createEnvironmentDTO(name, displayName, description, provider,
                false, vHostDTOList, null);

        //Add the environment
        ApiResponse<EnvironmentDTO> addedEnvironments = restAPIAdmin.addEnvironment(environmentDTO);

        //Assert the status code and environment ID
        Assert.assertEquals(addedEnvironments.getStatusCode(), HttpStatus.SC_CREATED);
        EnvironmentDTO addedEnvironmentDTO = addedEnvironments.getData();
        String environmentId = addedEnvironmentDTO.getId();
        Assert.assertNotNull(environmentId, "The environment ID cannot be null or empty");

        environmentDTO.setId(environmentId);
        //Verify the created label DTO
        adminApiTestHelper.verifyEnvironmentDTO(environmentDTO, addedEnvironmentDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test add already existing gateway environment",
            dependsOnMethods = "testAddGatewayEnvironmentSingleVHost")
    public void testAddAlreadyExistingEnvironment() throws Exception {
        //Add already existing environment - bad request
        EnvironmentDTO configuredGatewayEnvironment = getConfiguredGatewayEnvironment();
        try {
            restAPIAdmin.addEnvironment(configuredGatewayEnvironment);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test add gateway environment with multiple VHosts",
            dependsOnMethods = "testAddAlreadyExistingEnvironment")
    public void testAddGatewayEnvironmentMultipleVHosts() throws Exception {
        //Create the environment DTO
        String name = "us-region";
        String displayName = "US Region";
        String description = "Gateway environment deployed in US region";
        String provider = Constants.WSO2_GATEWAY_ENVIRONMENT;
        List<VHostDTO> vHostDTOList = new ArrayList<>();
        vHostDTOList.add(DtoFactory.createVhostDTO("us.mg.wso2.com", "", 80, 443, 9099, 8099));
        vHostDTOList.add(DtoFactory.createVhostDTO("foods.com", "zfoods", 8280, 8243, 9099, 8099));
        environmentDTO = DtoFactory.createEnvironmentDTO(name, displayName, description, provider, false, vHostDTOList, null);

        //Add the environment
        ApiResponse<EnvironmentDTO> addedEnvironments = restAPIAdmin.addEnvironment(environmentDTO);

        //Assert the status code and environment ID
        Assert.assertEquals(addedEnvironments.getStatusCode(), HttpStatus.SC_CREATED);
        EnvironmentDTO addedEnvironmentDTO = addedEnvironments.getData();
        String environmentId = addedEnvironmentDTO.getId();
        Assert.assertNotNull(environmentId, "The environment ID cannot be null or empty");

        environmentDTO.setId(environmentId);
        //Verify the created label DTO
        adminApiTestHelper.verifyEnvironmentDTO(environmentDTO, addedEnvironmentDTO);

        //Add already existing environment - bad request
        EnvironmentDTO configuredGatewayEnvironment = getConfiguredGatewayEnvironment();
        try {
            restAPIAdmin.addEnvironment(configuredGatewayEnvironment);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test get all gateway environments",
            dependsOnMethods = "testAddGatewayEnvironmentMultipleVHosts")
    public void testGetGatewayEnvironments() throws Exception {
        //Retrieve all Environments
        ApiResponse<EnvironmentListDTO> retrievedEnvs = restAPIAdmin.getEnvironments();
        Assert.assertEquals(retrievedEnvs.getStatusCode(), HttpStatus.SC_OK);

        EnvironmentListDTO environmentListDTO = retrievedEnvs.getData();
        List<EnvironmentDTO> environmentDTOS = environmentListDTO.getList();
        //Verify the retrieved labels
        Assert.assertNotNull(environmentDTOS, "Environment list can not be null");
        EnvironmentDTO configuredGatewayEnvironment = getConfiguredGatewayEnvironment();
        for (EnvironmentDTO environment : environmentDTOS) {
            //There are two environments which is configured from deployment toml file and the created
            //dynamic environment with the testAddGatewayEnvironment test.
            // TODO: Add Provider property to database level and retrieve with this restAPIAdmin.getEnvironments() REST
            // call. Until then that value will be set manually.
            environment.setProvider(Constants.WSO2_GATEWAY_ENVIRONMENT);
            if (configuredGatewayEnvironment.getName().equals(environment.getName())) {
                adminApiTestHelper.verifyEnvironmentDTO(configuredGatewayEnvironment, environment);
            } else {
                if (StringUtils.equals(environmentDTO.getName(), environment.getName())) {
                    adminApiTestHelper.verifyEnvironmentDTO(environmentDTO, environment);
                }
            }
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test deploy API revision with a Vhost",
            dependsOnMethods = "testGetGatewayEnvironments")
    public void testDeployApiRevisionWithVhost() throws Exception {
        addApiAndProductRevision();

        // Deploy API one in "Default"
        List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList = new ArrayList<>();
        APIRevisionDeployUndeployRequest apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionDeployRequest.setVhost("localhost");
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        HttpResponse apiRevisionsDeployResponse = restAPIPublisher.deployAPIRevision(apiOneId, apiOneRevisionId,
                apiRevisionDeployRequestList,"API");
        assertEquals(apiRevisionsDeployResponse.getResponseCode(), HttpStatus.SC_CREATED,
                "Unable to deploy API Revisions:" + apiRevisionsDeployResponse.getData());

        // Deploy API two in "Default" and "us-region"
        apiRevisionDeployRequestList = new ArrayList<>();
        apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName(Constants.GATEWAY_ENVIRONMENT);
        apiRevisionDeployRequest.setVhost("localhost");
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName("us-region");
        apiRevisionDeployRequest.setVhost("foods.com");
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        apiRevisionsDeployResponse = restAPIPublisher.deployAPIRevision(apiTwoId, apiTwoRevisionId,
                apiRevisionDeployRequestList,"API");
        assertEquals(apiRevisionsDeployResponse.getResponseCode(), HttpStatus.SC_CREATED,
                "Unable to deploy API Revisions:" + apiRevisionsDeployResponse.getData());

        // Deploy API product in "us-region"
        apiRevisionDeployRequestList = new ArrayList<>();
        apiRevisionDeployRequest = new APIRevisionDeployUndeployRequest();
        apiRevisionDeployRequest.setName("us-region");
        apiRevisionDeployRequest.setVhost("us.mg.wso2.com");
        apiRevisionDeployRequest.setDisplayOnDevportal(true);
        apiRevisionDeployRequestList.add(apiRevisionDeployRequest);
        apiRevisionsDeployResponse = restAPIPublisher.deployAPIProductRevision(apiProductId, apiProductRevisionId,
                apiRevisionDeployRequestList, "APIProduct");
        assertEquals(apiRevisionsDeployResponse.getResponseCode(), HttpStatus.SC_CREATED,
                "Unable to deploy API Product Revisions:" + apiRevisionsDeployResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test validate Devportal API and Swagger Response",
            dependsOnMethods = "testDeployApiRevisionWithVhost")
    public void testValidateDevportalAPIAndSwaggerResponse() throws Exception {
        String tenantDomain = gatewayContextMgt.getContextTenant().getDomain();
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiResponse = restAPIStore.getAPI(apiTwoId);
        String swagger = restAPIStore.getSwaggerByID(apiTwoId, tenantDomain);
        Assert.assertNotNull(apiResponse);
        List<APIEndpointURLsDTO> apiEndpointURLsDTOS = apiResponse.getEndpointURLs();
        for (APIEndpointURLsDTO apiEndpointUrlDTP: apiEndpointURLsDTOS) {
            if (StringUtils.equalsIgnoreCase(apiEndpointUrlDTP.getEnvironmentName(),"us-region")) {
                Assert.assertEquals(apiEndpointUrlDTP.getEnvironmentName(),"us-region");
            } else {
                Assert.assertEquals(apiEndpointUrlDTP.getEnvironmentName(),"Default");
            }
        }
        Assert.assertNotNull(swagger);
        JSONObject jsonObject = new JSONObject(swagger);
        Assert.assertNotNull(jsonObject.getString("servers"));
    }

    @Test(groups = {"wso2.am"}, description = "Test update gateway environment",
            dependsOnMethods = "testValidateDevportalAPIAndSwaggerResponse")
    public void testUpdateEnvironment() throws Exception {
        //Update the dynamic environment
        environmentDTO.setDisplayName("US Gateway Environment");
        environmentDTO.setDescription("This is a updated test gateway environment");
        List<VHostDTO> vHostDTOList = new ArrayList<>();
        vHostDTOList.add(DtoFactory.createVhostDTO("updated.wso2.com", "", 7080, 7443, 7099, 8099));
        environmentDTO.setVhosts(vHostDTOList);

        ApiResponse<EnvironmentDTO> updatedEnvironment = restAPIAdmin.updateEnvironment(environmentDTO.getId(), environmentDTO);
        EnvironmentDTO updatedEnvironmentDTO = updatedEnvironment.getData();
        Assert.assertEquals(updatedEnvironment.getStatusCode(), HttpStatus.SC_OK);

        //Verify the updated label DTO
        adminApiTestHelper.verifyEnvironmentDTO(environmentDTO, updatedEnvironmentDTO);

        //Update configured environment - bad request
        EnvironmentDTO configuredGatewayEnvironment = getConfiguredGatewayEnvironment();
        try {
            restAPIAdmin.updateEnvironment(configuredGatewayEnvironment.getId(),
                    configuredGatewayEnvironment);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test update gateway environment by removing exisiting Vhost and adding new Vhost",
            dependsOnMethods = "testUpdateEnvironment")
    public void testUpdateEnvironmentByRemovingVHost() throws Exception {
        //Remove VHost from the gateway environment
        List<VHostDTO> vHostDTOList = environmentDTO.getVhosts();
        vHostDTOList.remove(0);
        vHostDTOList.add(DtoFactory.createVhostDTO("new.com", "zfoods",
                8280, 8243, 9099, 8099));

        ApiResponse<EnvironmentDTO> updatedEnvironment = restAPIAdmin.updateEnvironment(environmentDTO.getId(), environmentDTO);
        EnvironmentDTO updatedEnvironmentDTO = updatedEnvironment.getData();
        Assert.assertEquals(updatedEnvironment.getStatusCode(), HttpStatus.SC_OK);

        //Verify the updated label DTO
        adminApiTestHelper.verifyEnvironmentDTO(environmentDTO, updatedEnvironmentDTO);
    }

    @Test(groups = {"wso2.am"}, description = "Test delete environment",
            dependsOnMethods = "testUpdateEnvironmentByRemovingVHost")
    public void testDeleteEnvironment() throws Exception {
        //Delete dynamic environment
        ApiResponse<Void> apiResponse = restAPIAdmin.deleteEnvironment(environmentDTO.getId());
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK);

        //Delete configured environment - bad request
        EnvironmentDTO configuredGatewayEnvironment = getConfiguredGatewayEnvironment();
        try {
            restAPIAdmin.deleteEnvironment(configuredGatewayEnvironment.getId());
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_BAD_REQUEST);
        }

        //Delete non existing environment - not found
        try {
            apiResponse = restAPIAdmin.deleteEnvironment(UUID.randomUUID().toString());
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), HttpStatus.SC_NOT_FOUND);
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }

    /**
     * Get gateway environment configured in deployment toml
     *
     * @return EnvironmentDTO with default configs
     */
    private EnvironmentDTO getConfiguredGatewayEnvironment() {
        VHostDTO vhostDTO = DtoFactory.createVhostDTO("localhost", "", 8780, 8743, 9099, 8099);
        EnvironmentDTO configuredEnv = DtoFactory.createEnvironmentDTO(
                                        Constants.GATEWAY_ENVIRONMENT,
                                        Constants.GATEWAY_ENVIRONMENT,
                "This is a hybrid gateway that handles both production and sandbox token traffic.",
                Constants.WSO2_GATEWAY_ENVIRONMENT,
                true,
                Collections.singletonList(vhostDTO), "Regular"
        );
        configuredEnv.setId(Constants.GATEWAY_ENVIRONMENT);
        return configuredEnv;
    }

    private void addApiAndProductRevision() throws Exception {
        // Pre-Conditions : Create APIs
        List<APIDTO> apisToBeUsed = new ArrayList<>();

        // Step 1 : Create APIs
        APIDTO apiOne = apiTestHelper.
                createApiOne(getBackendEndServiceEndPointHttp("wildcard/resources"));
        APIDTO apiTwo = apiTestHelper.
                createApiTwo(getBackendEndServiceEndPointHttp("wildcard/resources"));
        apisToBeUsed.add(apiOne);
        apisToBeUsed.add(apiTwo);

        // Step 2 : Create APIProduct
        String provider = UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();
        String context = "/" + UUID.randomUUID().toString();

        String tenantDomain = gatewayContextMgt.getContextTenant().getDomain();
        if (this.userMode != TestUserMode.SUPER_TENANT_ADMIN) {
            provider = provider + "@" + tenantDomain;
        }

        final String version = "1.0.0";
        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        APIProductDTO apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(provider, name, context, version,
                apisToBeUsed, policies);

        waitForAPIDeployment();

        // Step 3 : Verify created APIProduct in publisher
        apiProductTestHelper.verfiyApiProductInPublisher(apiProductDTO);

        // Step 4: Create revisions
        APIRevisionRequest apiRevisionRequest = new APIRevisionRequest();
        apiRevisionRequest.setApiUUID(apiProductDTO.getId());

        //Add the API Revision using the API publisher.
        apiOneId = apiOne.getId();
        apiRevisionRequest.setApiUUID(apiOneId);
        HttpResponse apiRevisionResponse = restAPIPublisher.addAPIRevision(apiRevisionRequest);
        apiOneRevisionId = extractRevisionId(apiRevisionResponse);

        apiTwoId = apiTwo.getId();
        apiRevisionRequest.setApiUUID(apiTwoId);
        apiRevisionResponse = restAPIPublisher.addAPIRevision(apiRevisionRequest);
        apiTwoRevisionId = extractRevisionId(apiRevisionResponse);
        apiTwo.setPolicies(policies);
        restAPIPublisher.updateAPI(apiTwo);
        restAPIPublisher.changeAPILifeCycleStatus(apiTwoId, APILifeCycleAction.PUBLISH.getAction());

        //Add the API Revision using the API publisher.
        apiProductId = apiProductDTO.getId();
        apiRevisionRequest.setApiUUID(apiProductId);
        apiRevisionResponse = restAPIPublisher.addAPIProductRevision(apiRevisionRequest);
        apiProductRevisionId = extractRevisionId(apiRevisionResponse);
    }

    private String extractRevisionId(HttpResponse httpResponse) throws JSONException {
        assertEquals(httpResponse.getResponseCode(), HttpStatus.SC_CREATED,
                "Create API Response Code is invalid." + httpResponse.getData());
        JSONObject jsonObject = new JSONObject(httpResponse.getData());
        return jsonObject.getString("id");
    }

}
