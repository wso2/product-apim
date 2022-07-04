/*
 *   Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.am.integration.tests.endpoint;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIEndpointDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIEndpointListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class APIEndpointTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(APIEndpointTestCase.class);

    private final String API_NAME = "APIEndpointTestCase";
    private final String API_CONTEXT = "APIEndpointTestCase";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String API_END_POINT_POSTFIX_URL = "xmlapi";
    private String applicationId;
    private String apiId;
    private Map<String, String> apiEndpoints;

    @Factory(dataProvider = "userModeDataProvider")
    public APIEndpointTestCase(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init(userMode);

        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Test Application Endpoint APITestCase", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();

        String apiEndPointUrl = getAPIInvocationURLHttp(API_END_POINT_POSTFIX_URL, API_VERSION_1_0_0);
        APIRequest apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTags(API_TAGS);
        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);

//        ArrayList grantTypes = new ArrayList();
//        grantTypes.add("client_credentials");
//
//        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "3600", null,
//                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
//        accessToken = applicationKeyDTO.getToken().getAccessToken();
        apiEndpoints = new HashMap<>();
    }

    @Test(groups = {"wso2.am"}, description = "Add API Endpoint.")
    public void testAddNewAPIEndpoint() throws Exception {
        APIEndpointDTO apiEndpointDTO = new APIEndpointDTO();
        apiEndpointDTO.setName("TestingEndpoint");
        apiEndpointDTO.setEndpointType("REST");

        Map<String,Object> endpointConfig = new HashMap<>();
        endpointConfig.put("url", "https://google.lk");
        endpointConfig.put("timeout", 1000);

        Map<String,Object> endpointSecurity = new HashMap<>();
        endpointSecurity.put("enabled", true);
        endpointSecurity.put("username", "test_user");
        endpointSecurity.put("password", "password123");

        endpointConfig.put("endpoint_security", endpointSecurity);

        apiEndpointDTO.setEndpointConfig(endpointConfig);

        APIEndpointDTO createdApiEndpointDTO = addAPIEndpoint(apiId, apiEndpointDTO);

        assertNotNull(createdApiEndpointDTO, "Error adding API Endpoint.");

        String createdApiEndpointId = createdApiEndpointDTO.getId();
        assertNotNull(createdApiEndpointId, "APIEndpoint Id is null");

        apiEndpoints.put("createdApiEndpoint", createdApiEndpointId);
        log.info("API Endpoint created : " + createdApiEndpointId);
    }

    @Test(groups = {"wso2.am"}, description = "Get API Endpoint By UUID of Endpoint.",
            dependsOnMethods = {"testAddNewAPIEndpoint"})
    public void testGetAPIEndpointById() throws Exception {
        APIEndpointDTO apiEndpointDTO = restAPIPublisher.getAPIEndpointById(
                apiId, apiEndpoints.get("createdApiEndpoint"));

        assertNotNull(apiEndpointDTO, "Error getting API Endpoint By UUID.");
    }

    @Test(groups = {"wso2.am"}, description = "Update API Endpoint By UUID of Endpoint.",
            dependsOnMethods = {"testAddNewAPIEndpoint"})
    public void testUpdateAPIEndpointById() throws Exception {
        APIEndpointDTO apiEndpointDTO = new APIEndpointDTO();
        apiEndpointDTO.setName("test1");
        apiEndpointDTO.setEndpointType("REST");

        Map<String,Object> endpointConfig = new HashMap<>();
        endpointConfig.put("url", "https://youtube.lk");
        endpointConfig.put("timeout", 1000);

        Map<String,Object> endpointSecurity = new HashMap<>();
        endpointSecurity.put("enabled", true);
        endpointSecurity.put("username", "test_user2");
        endpointSecurity.put("password", "QWE@#!QWE");

        endpointConfig.put("endpoint_security", endpointSecurity);

        apiEndpointDTO.setEndpointConfig(endpointConfig);

        APIEndpointDTO updatedApiEndpointDTO = updateAPIEndpoint(apiId, apiEndpointDTO);

        assertNotNull(updatedApiEndpointDTO, "Error updating API Endpoint By UUID.");

        String updatedApiEndpointDTOId = updatedApiEndpointDTO.getId();
        assertNotNull(updatedApiEndpointDTOId, "APIEndpoint Id is null");
    }


    @Test(groups = {"wso2.am"}, description = "Get all API Endpoints.")
    public void testGetAllAPIEndpoints() throws Exception {

        APIEndpointListDTO getEndpointListResponse = restAPIPublisher.getAllAPIEndpoints(apiId);

        assertNotNull(getEndpointListResponse, "Error getting all API Endpoints.");

        for (APIEndpointDTO apiEndpointDTO : getEndpointListResponse.getList()) {
            apiEndpoints.put(apiEndpointDTO.getName(), apiEndpointDTO.getId());
        }
    }

    @Test(groups = {"wso2.am"}, description = "Testing for Primary APIEndpoint to API.",
            dependsOnMethods = {"testAddNewAPIEndpoint"})
    public void testPrimaryMappingWithAnEndpoint() throws Exception {
        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        //Set primary api endpoint
        apidto.setPrimaryProductionEndpointId(apiEndpoints.get("createdApiEndpoint"));

        APIDTO updatedApiDTO =  restAPIPublisher.updateAPI(apidto);
        assertEquals(updatedApiDTO.getPrimaryProductionEndpointId(), apiEndpoints.get("createdApiEndpoint"),
                "Fail to mapped primary APIEndpoint to API.");
    }

    @Test(groups = {"wso2.am"}, description = "Testing for APIEndpoint for operation mapping.",
            dependsOnMethods = {"testAddNewAPIEndpoint"})
    public void testOperationMappingWithAnEndpoint() throws Exception {
        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        //Set an APIEndpoint to an operation
        List<APIOperationsDTO> apiOperationsDTOS =  apidto.getOperations();
        APIOperationsDTO apiOperationsDTO = apiOperationsDTOS.get(0);
        apiOperationsDTO.setProductionEndpointId(apiEndpoints.get("createdApiEndpoint"));
        apiOperationsDTOS.set(0, apiOperationsDTO);
        apidto.setOperations(apiOperationsDTOS);

        APIDTO updatedApiDTO =  restAPIPublisher.updateAPI(apidto);
        assertNotNull(updatedApiDTO);
        assertEquals(updatedApiDTO.getOperations().get(0).getProductionEndpointId(),
                apiEndpoints.get("createdApiEndpoint"), "Fail to mapped the created endpoint to an operation.");
    }

    @Test(groups = {"wso2.am"}, description = "Testing for none case APIEndpoint for operation mapping.")
    public void testOperationMappingWithNoneEndpoint() throws Exception {
        HttpResponse getAPIResponse = restAPIPublisher.getAPI(apiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        //Set none case for an operation's endpoint
        List<APIOperationsDTO> apiOperationsDTOS =  apidto.getOperations();
        APIOperationsDTO apiOperationsDTO = apiOperationsDTOS.get(0);
        apiOperationsDTO.setProductionEndpointId("none");
        apiOperationsDTOS.set(0, apiOperationsDTO);
        apidto.setOperations(apiOperationsDTOS);

        APIDTO updatedApiDTO =  restAPIPublisher.updateAPI(apidto);
        assertNotNull(updatedApiDTO);
        assertEquals(updatedApiDTO.getOperations().get(0).getProductionEndpointId(), "none",
                "Fail to mapped the none endpoint to an operation.");
    }

    @Test(groups = {"wso2.am"}, description = "Delete API Endpoint By UUID of Endpoint.",
            dependsOnMethods = {"testAddNewAPIEndpoint"})
    public void testDeleteAPIEndpointById() throws Exception {
        HttpResponse getEndpointResponse = restAPIPublisher.deleteAPIEndpointById(
                apiId, apiEndpoints.get("createdApiEndpoint"));

        assertEquals(getEndpointResponse.getResponseCode(), 200, "Response code mismatched");
        apiEndpoints.remove("createdApiEndpoint");
    }


    private APIEndpointDTO addAPIEndpoint(String apiId, APIEndpointDTO apiEndpointDTO) throws ApiException {

        APIEndpointDTO addApiEndpointResponse = restAPIPublisher.addAPIEndpoint(apiId, apiEndpointDTO);
        return addApiEndpointResponse;
    }

    private APIEndpointDTO updateAPIEndpoint(String apiId, APIEndpointDTO apiEndpointDTO) throws ApiException {

        APIEndpointDTO updateApiEndpointResponse = restAPIPublisher.updateAPIEndpoint(
                apiId, apiEndpoints.get("createdApiEndpoint"), apiEndpointDTO);
        return updateApiEndpointResponse;
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
    }
}
