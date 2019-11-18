/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.apiproduct;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIProductDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ProductAPIDTO;
import org.wso2.am.integration.test.impl.DtoUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class APIProductCreationTestCase extends APIManagerLifecycleBaseTest {

    private static String SWAGGER_FOLDER = "swagger";
    private static String API_1_DEFINITION_NAME = "customer-info-api.yaml";
    private static String API_2_DEFINITION_NAME  = "leasing-api.yaml";

    private List<APIDTO> apiDtos = new ArrayList<>();
    private List<String> apiProductIds = new ArrayList<>();

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        String swaggerPath = getAMResourceLocation() + File.separator + SWAGGER_FOLDER +
                File.separator + API_1_DEFINITION_NAME;

        File firstApiDefinition = new File(swaggerPath);

        swaggerPath = getAMResourceLocation() + File.separator + SWAGGER_FOLDER +
                File.separator + API_2_DEFINITION_NAME;

        File secondApiDefinition = new File(swaggerPath);

        apiDtos.add(restAPIPublisher.importOASDefinition(firstApiDefinition, null));
        apiDtos.add(restAPIPublisher.importOASDefinition(secondApiDefinition, null));
    }


    @Test(groups = {"wso2.am"}, description = "Test creation and invocation of API Product")
    public void testCreateAndInvokeApiProduct() throws Exception {
        final String provider = "publisher";
        final String name = "test-product";
        final String context = "/test-product";

        List<ProductAPIDTO> resourcesForProduct = getResourcesForProduct(apiDtos);

        APIProductDTO apiProductDTO = DtoUtils.createApiProductDTO(provider, name, context, resourcesForProduct);

        ApiResponse<APIProductDTO> response = restAPIPublisher.addApiProduct(apiProductDTO);

        Assert.assertEquals(response.getStatusCode(), 201);
        APIProductDTO responseData = response.getData();

        List<ProductAPIDTO> responseResources = responseData.getApis();

        Assert.assertEquals(new HashSet<>(responseResources), new HashSet<>(resourcesForProduct));
    }

    private List<ProductAPIDTO> getResourcesForProduct(List<APIDTO> apiDtos) {
        Map<APIDTO, Set<APIOperationsDTO>> selectedApiResourceMapping = new HashMap<>();

        // Pick two operations from each API to be used to create the APIProduct.
        for (APIDTO apiDto : apiDtos) {
            selectOperationsFromAPI(apiDto, 2, selectedApiResourceMapping);
        }

        return DtoUtils.getProductApiResources(selectedApiResourceMapping);
    }

    private void selectOperationsFromAPI(APIDTO apiDto, final int numberOfOperations,
                                            Map<APIDTO, Set<APIOperationsDTO>> selectedApiResourceMapping) {
        List<APIOperationsDTO> operations = apiDto.getOperations();

        Set<APIOperationsDTO> selectedOperations = new HashSet<>();
        selectedApiResourceMapping.put(apiDto, selectedOperations);

        for (APIOperationsDTO operation : operations) {
            selectedOperations.add(operation);

            // Only select upto the specified number of operations
            if (selectedOperations.size() == numberOfOperations) {
                break;
            }
        }
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        for (APIDTO apiDto : apiDtos) {
            restAPIPublisher.deleteAPI(apiDto.getId());
        }

        for (String apiProductId : apiProductIds) {
            restAPIPublisher.deleteApiProduct(apiProductId);
        }

        super.cleanUp();
    }
}
