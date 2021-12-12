/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.apiproduct.lifecycle;

import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIKeyDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIProductDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.LifecycleStateDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.WorkflowResponseDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.impl.ApiProductTestHelper;
import org.wso2.am.integration.test.impl.ApiTestHelper;
import org.wso2.am.integration.test.impl.InvocationStatusCodes;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.http.HttpRequestUtil;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class APIProductLifecycleTest extends APIManagerLifecycleBaseTest {

    private ApiProductTestHelper apiProductTestHelper;
    private ApiTestHelper apiTestHelper;
    private List<APIDTO> apisToBeUsed;
    private String apiProductId;
    private String applicationId;
    private ApplicationKeyDTO productionAppKey;
    private InvocationStatusCodes invocationStatusCodes = new InvocationStatusCodes();

    @Factory(dataProvider = "userModeDataProvider")
    public APIProductLifecycleTest(TestUserMode userMode) {

        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {

        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
                new Object[]{TestUserMode.SUPER_TENANT_EMAIL_USER},
                new Object[]{TestUserMode.TENANT_EMAIL_USER},
                new Object[]{TestUserMode.SUPER_TENANT_USER_STORE_USER},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init(userMode);
        apiProductTestHelper = new ApiProductTestHelper(restAPIPublisher, restAPIStore);
        apiTestHelper = new ApiTestHelper(restAPIPublisher, restAPIStore, getAMResourceLocation(),
                keyManagerContext.getContextTenant().getDomain(), keyManagerHTTPSURL, user);

        apisToBeUsed = new ArrayList<>();
        APIDTO apiOne = apiTestHelper.createApiOne(getBackendEndServiceEndPointHttp("wildcard/resources"));
        APIDTO apiTwo = apiTestHelper.createApiTwo(getBackendEndServiceEndPointHttp("wildcard/resources"));
        apisToBeUsed.add(apiOne);
        apisToBeUsed.add(apiTwo);
    }

    @Test(groups = {"wso2.am"}, description = "Test invocation of the APi before retire")
    public void testCreateAPIProduct() throws Exception {

        String provider;
        if (userMode == TestUserMode.SUPER_TENANT_USER_STORE_USER) {
            provider = user.getUserNameWithoutDomain();
        } else {
            provider = user.getUserName();
        }
        final String name = UUID.randomUUID().toString();
        final String context = "/" + UUID.randomUUID();

        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        APIProductDTO apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(provider, name, context,
                apisToBeUsed, policies);
        apiProductId = apiProductDTO.getId();
        assert apiProductDTO.getState() != null;
        Assert.assertTrue(APILifeCycleState.CREATED.getState().equalsIgnoreCase(apiProductDTO.getState().getValue()));
        waitForAPIDeployment();

        createAPIProductRevisionAndDeployUsingRest(apiProductId, restAPIPublisher);
        apiProductTestHelper.verfiyApiProductInPublisher(apiProductDTO);

        ApiResponse<APIKeyDTO> apiKeyDTOResponse = restAPIPublisher.generateInternalApiKey(apiProductId);
        assertEquals(apiKeyDTOResponse.getStatusCode(), 200);
        invokeApiWithInternalKey(apiProductDTO.getContext(), "/*", apiKeyDTOResponse.getData().getApikey());
    }

    @Test(groups = {"wso2.am"}, description = "Test Publishing API Product", dependsOnMethods =
            {"testCreateAPIProduct"})
    public void testPublishAPIProduct() throws Exception {

        WorkflowResponseDTO workflowResponseDTO = apiProductTestHelper.changeLifecycleStateOfApiProduct(apiProductId,
                "Publish", null);
        assertNotNull(workflowResponseDTO);
        LifecycleStateDTO lifecycleStateDTO = workflowResponseDTO.getLifecycleState();
        assertNotNull(lifecycleStateDTO);
        assertEquals("APPROVED", workflowResponseDTO.getWorkflowStatus().getValue());
        assert APILifeCycleState.PUBLISHED.getState().equals(lifecycleStateDTO.getState());
        assert lifecycleStateDTO.getAvailableTransitions() != null;
        assertEquals(4, lifecycleStateDTO.getAvailableTransitions().size());

        APIProductDTO returnedProduct = restAPIPublisher.getApiProduct(apiProductId);
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDTO =
                apiProductTestHelper.verifyApiProductInPortal(returnedProduct);

        ApplicationDTO applicationDTO = apiTestHelper.verifySubscription(apiDTO, UUID.randomUUID().toString(),
                TIER_UNLIMITED);
        applicationId = applicationDTO.getApplicationId();

        List<String> grantTypes = Arrays.asList("client_credentials", "password");
        productionAppKey = apiTestHelper.verifyKeyGeneration(applicationDTO,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, new ArrayList<>(), grantTypes);

        apiTestHelper.verifyInvocation(apiDTO, productionAppKey.getToken().getAccessToken(),
                productionAppKey.getToken().getAccessToken(), invocationStatusCodes);
    }

    @Test(groups = {"wso2.am"}, description = "Test invocation of the APi before retire", dependsOnMethods =
            {"testPublishAPIProduct"})
    public void testChangeAPIProductLifecycleStateToBlockedState() throws Exception {

        WorkflowResponseDTO workflowResponseDTO = apiProductTestHelper.changeLifecycleStateOfApiProduct(apiProductId,
                "Block", null);
        assertNotNull(workflowResponseDTO);
        LifecycleStateDTO lifecycleStateDTO = workflowResponseDTO.getLifecycleState();
        assertNotNull(lifecycleStateDTO);
        assertEquals("APPROVED", workflowResponseDTO.getWorkflowStatus().getValue());
        assert APILifeCycleState.BLOCKED.getState().equals(lifecycleStateDTO.getState());
        assert lifecycleStateDTO.getAvailableTransitions() != null;
        // invoke blocked API Product

        // re-publish API Product
        workflowResponseDTO = apiProductTestHelper.changeLifecycleStateOfApiProduct(apiProductId,
                "Re-Publish", null);
        assertEquals("APPROVED", workflowResponseDTO.getWorkflowStatus().getValue());
        assertEquals(workflowResponseDTO.getLifecycleState().getState(),
                APILifeCycleState.PUBLISHED.getState());
        APIProductDTO returnedProduct = restAPIPublisher.getApiProduct(apiProductId);
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDTO =
                apiProductTestHelper.verifyApiProductInPortal(returnedProduct);

        // invoke published API Product
        apiTestHelper.verifyInvocation(apiDTO, productionAppKey.getToken().getAccessToken(),
                productionAppKey.getToken().getAccessToken(), invocationStatusCodes);
    }

    @Test(groups = {"wso2.am"}, description = "Test invocation of the APi before retire", dependsOnMethods =
            {"testChangeAPIProductLifecycleStateToBlockedState"})
    public void testDeleteDeprecatedAPIProductsWithSubscription() throws Exception {

        try {
            restAPIPublisher.deleteApiProduct(apiProductId);
        } catch (ApiException e) {
            assertEquals(Response.Status.CONFLICT.getStatusCode(), e.getCode());
            assertTrue(e.getResponseBody().contains("active subscriptions exist"));
        }

        // Deprecate API Product
        WorkflowResponseDTO workflowResponseDTO = apiProductTestHelper.changeLifecycleStateOfApiProduct(apiProductId,
                "Deprecate", null);
        assertEquals("APPROVED", workflowResponseDTO.getWorkflowStatus().getValue());

        // Test the visibility in of Deprecated API Product in Dev portal
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO responseData =
                restAPIStore.getAPI(apiProductId);

        // Test the invocation of Deprecated API Product
        APIProductDTO returnedProduct = restAPIPublisher.getApiProduct(apiProductId);
        apiTestHelper.verifyInvocation(responseData, productionAppKey.getToken().getAccessToken(),
                productionAppKey.getToken().getAccessToken(), invocationStatusCodes);
    }

    @Test(groups = {"wso2.am"}, description = "Test invocation of the APi before retire", dependsOnMethods =
            {"testDeleteDeprecatedAPIProductsWithSubscription"})
    public void testDeleteRetiredAPIProducts() throws Exception {

        // Retire API Product
        WorkflowResponseDTO workflowResponseDTO = apiProductTestHelper.changeLifecycleStateOfApiProduct(apiProductId,
                "Retire", null);
        assertEquals("APPROVED", workflowResponseDTO.getWorkflowStatus().getValue());
        assertNotNull(workflowResponseDTO.getLifecycleState());
        assertEquals(workflowResponseDTO.getLifecycleState().getState(), APILifeCycleState.RETIRED.getState());

        // Verify the subscriptions
        SubscriptionListDTO subscriptionListDTO = restAPIPublisher.getSubscriptionByAPIID(apiProductId);
        assertEquals(subscriptionListDTO.getList().size(), 0);

        // Delete API Product
        restAPIPublisher.deleteApiProduct(apiProductId);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        for (APIDTO apiDto: apisToBeUsed) {
            restAPIPublisher.deleteAPI(apiDto.getId());
        }
        restAPIStore.deleteApplication(applicationId);
    }

    private HttpResponse invokeApiWithInternalKey(String context, String resource, String internalKey)
            throws XPathExpressionException, IOException {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("accept", "application/json");
        requestHeaders.put("Internal-Key", internalKey);
        return HttpRequestUtil.doGet(getAPIProductInvocationURLHttps(context) + resource, requestHeaders);
    }
}
