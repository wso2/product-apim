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

import org.json.JSONException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIProductDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.impl.ApiTestHelper;
import org.wso2.am.integration.test.impl.ApiProductTestHelper;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class APIProductCreationTestCase extends APIManagerLifecycleBaseTest {
    private ApiTestHelper apiTestHelper;
    private ApiProductTestHelper apiProductTestHelper;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException, ApiException, JSONException {
        super.init();
        apiTestHelper = new ApiTestHelper(restAPIPublisher, restAPIStore, getAMResourceLocation());
        apiProductTestHelper = new ApiProductTestHelper(restAPIPublisher, restAPIStore);
    }


    @Test(groups = {"wso2.am"}, description = "Test creation and invocation of API Product")
    public void testCreateAndInvokeApiProduct() throws Exception {
        // Pre-Conditions : Create APIs
        List<APIDTO> apisToBeUsed = new ArrayList<>();

        apisToBeUsed.add(apiTestHelper.
                createCustomerInfoApi(getBackendEndServiceEndPointHttp("wildcard/resources")));
        apisToBeUsed.add(apiTestHelper.
                createLeasingApi(getBackendEndServiceEndPointHttp("wildcard/resources")));

        // Step 1 : Create APIProduct
        final String provider = UUID.randomUUID().toString();
        final String name = UUID.randomUUID().toString();
        final String context = "/" + UUID.randomUUID().toString();

        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        APIProductDTO apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(provider, name, context,
                apisToBeUsed, policies);

        waitForAPIDeployment();

        // Step 2 : Verify created APIProduct in publisher
        apiProductTestHelper.verfiyApiProductInPublisher(apiProductDTO);

        // Step 3 : Verify APIProduct in dev portal
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDTO =
                apiProductTestHelper.verifyApiProductInPortal(apiProductDTO);

        // Step 4 : Subscribe to APIProduct
        ApplicationDTO applicationDTO = apiTestHelper.verifySubscription(apiDTO, UUID.randomUUID().toString(),
                TIER_UNLIMITED);

        // Step 5 : Generate Production and Sandbox tokens without scopes
        List<String> grantTypes = Arrays.asList("client_credentials", "password");
        String productionAccessToken = apiTestHelper.verifyKeyGeneration(applicationDTO,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, new ArrayList<>(), grantTypes);

        String sandboxAccessToken = apiTestHelper.verifyKeyGeneration(applicationDTO,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX, new ArrayList<>(), grantTypes);

        // Step 6 : Invoke API Product
        apiTestHelper.verifyInvocation(apiDTO, productionAccessToken, sandboxAccessToken);
    }



    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        super.cleanUp();
    }
}

