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

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.admin.api.dto.APICategoryDTO;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationPoliciesDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIProductDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.LifecycleStateDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.OpenAPIDefinitionValidationResponseDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.OperationPolicyDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.WorkflowResponseDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.impl.ApiProductTestHelper;
import org.wso2.am.integration.test.impl.ApiTestHelper;
import org.wso2.am.integration.test.impl.InvocationStatusCodes;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import javax.ws.rs.core.Response;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class APIProductCreationTestCase extends APIManagerLifecycleBaseTest {

    private static final String INTERNAL_ROLE_SUBSCRIBER = "Internal/subscriber";
    private static final String RESTRICTED_SUBSCRIBER = "restricted_user";
    private static final String STANDARD_SUBSCRIBER = "standard_user";
    private static final String PASSWORD = "$3213#@sd";
    private static final String POLICY_TYPE_COMMON = "common";
    private static final String RESTRICTED_ROLE = "restricted_role";
    private static final String SCOPE = "restricted_scope";
    private ApiTestHelper apiTestHelper;
    private ApiProductTestHelper apiProductTestHelper;
    private String apiProductId2;
    private String resourcePath;
    private String apiID1;
    private String apiID2;

    @Factory(dataProvider = "userModeDataProvider")
    public APIProductCreationTestCase(TestUserMode userMode) {

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
        resourcePath = TestConfigurationProvider.getResourceLocation() + File.separator + "oas" + File.separator + "v3"
                + File.separator + "api-product" + File.separator;
        userManagementClient = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                createSession(keyManagerContext));
        apiTestHelper = new ApiTestHelper(restAPIPublisher, restAPIStore, getAMResourceLocation(),
                keyManagerContext.getContextTenant().getDomain(), keyManagerHTTPSURL, user);
        apiProductTestHelper = new ApiProductTestHelper(restAPIPublisher, restAPIStore);

        userManagementClient.addRole(RESTRICTED_ROLE, null, null);

        if (userManagementClient.userNameExists(INTERNAL_ROLE_SUBSCRIBER, RESTRICTED_SUBSCRIBER)) {
            userManagementClient.deleteUser(RESTRICTED_SUBSCRIBER);
        }

        userManagementClient.addUser(RESTRICTED_SUBSCRIBER, PASSWORD,
                new String[]{INTERNAL_ROLE_SUBSCRIBER, RESTRICTED_ROLE}, null);

        if (userManagementClient.userNameExists(INTERNAL_ROLE_SUBSCRIBER, STANDARD_SUBSCRIBER)) {
            userManagementClient.deleteUser(STANDARD_SUBSCRIBER);
        }

        userManagementClient.addUser(STANDARD_SUBSCRIBER, PASSWORD,
                new String[]{INTERNAL_ROLE_SUBSCRIBER}, null);
    }

    @Test(groups = {"wso2.am"}, description = "Test creation and invocation of API Product")
    public void testCreateAndInvokeApiProduct() throws Exception {
        // Pre-Conditions : Create APIs
        List<APIDTO> apisToBeUsed = new ArrayList<>();
        APIDTO apiOne = apiTestHelper.createApiOne(getBackendEndServiceEndPointHttp("wildcard/resources"));
        APIDTO apiTwo = apiTestHelper.createApiTwo(getBackendEndServiceEndPointHttp("wildcard/resources"));
        apisToBeUsed.add(apiOne);
        apisToBeUsed.add(apiTwo);

        // Step 1 : Create APIProduct
        final String provider = user.getUserName();
        final String name = UUID.randomUUID().toString();
        final String context = "/" + UUID.randomUUID().toString();
        final String version = "1.0.0";

        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        APIProductDTO apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(provider, name, context, version,
                apisToBeUsed, policies);
        createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        waitForAPIDeployment();

        // Step 2 : Verify created APIProduct in publisher
        apiProductTestHelper.verfiyApiProductInPublisher(apiProductDTO);

        apiProductDTO = publishAPIProduct(apiProductDTO.getId());

        // Step 3 : Verify APIProduct in dev portal
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDTO =
                apiProductTestHelper.verifyApiProductInPortal(apiProductDTO);

        // Step 4 : Subscribe to APIProduct
        ApplicationDTO applicationDTO = apiTestHelper.verifySubscription(apiDTO, UUID.randomUUID().toString(),
                TIER_UNLIMITED);

        // Step 5 : Generate Production and Sandbox keys and Application Access tokens without scopes
        List<String> grantTypes = Arrays.asList("client_credentials", "password");
        ApplicationKeyDTO productionAppKey = apiTestHelper.verifyKeyGeneration(applicationDTO,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, new ArrayList<>(), grantTypes);

        ApplicationKeyDTO sandboxAppKey = apiTestHelper.verifyKeyGeneration(applicationDTO,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX, new ArrayList<>(), grantTypes);

        // Step 6 : Invoke API Product with Application Access tokens
        InvocationStatusCodes invocationStatusCodes = new InvocationStatusCodes();
        apiTestHelper.verifyInvocation(apiDTO, productionAppKey.getToken().getAccessToken(),
                sandboxAppKey.getToken().getAccessToken(), invocationStatusCodes);

        // Step 7 : Generate Production and Sandbox User tokens without scopes
        String productionToken = apiTestHelper.generateTokenPasswordGrant(productionAppKey.getConsumerKey(),
                productionAppKey.getConsumerSecret(), STANDARD_SUBSCRIBER, PASSWORD,
                Collections.emptyList());

        String sandboxToken = apiTestHelper.generateTokenPasswordGrant(sandboxAppKey.getConsumerKey(),
                sandboxAppKey.getConsumerSecret(), STANDARD_SUBSCRIBER, PASSWORD,
                Collections.emptyList());

        // Step 8 : Invoke API Product with User Access tokens
        apiTestHelper.verifyInvocation(apiDTO, productionToken, sandboxToken, invocationStatusCodes);

        // Step 9 : Change Endpoint in one of the API and deploy and verify endpoint changing
        JSONObject endpointConfigJson = new JSONObject();
        endpointConfigJson.putAll((Map) apiOne.getEndpointConfig());
        Map productionEndpoint = (Map) endpointConfigJson.get("production_endpoints");
        String endpointUrl = getGatewayURLNhttp() + "version2";
        productionEndpoint.replace("url", endpointUrl);
        apiOne.setEndpointConfig(endpointConfigJson);
        restAPIPublisher.updateAPI(apiOne);
        apiTestHelper.verifyInvocation(apiDTO, productionToken, sandboxToken, invocationStatusCodes);
        // Step 10 : Update with Endpoint Detail in base API.
        ApiResponse<APIProductDTO> apiProductDTOApiResponse = restAPIPublisher.updateAPIProduct(apiProductDTO);
        Assert.assertEquals(apiProductDTOApiResponse.getStatusCode(), 200);
        createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        waitForAPIDeployment();
        Map<String, String> requestHeadersGet = new HashMap<>();
        requestHeadersGet.put("Authorization", "Bearer " + productionToken);
        HttpResponse httpResponse = invokeWithGet(getAPIInvocationURLHttp(context.replaceFirst("/", "")) +
                "/customers", requestHeadersGet);
        Assert.assertEquals(httpResponse.getResponseCode(), 200);
        Assert.assertEquals(httpResponse.getHeaders().get("Version"), "v2");
    }

    @Test(groups = {"wso2.am"}, description = "Create new version and publish")
    public void testAPIProductNewVersionCreation() throws Exception {

        String APIVersionNew  = "2.0.0";
        // Pre-Conditions : Create APIs
        List<APIDTO> apisToBeUsed = new ArrayList<>();
        APIDTO apiOne = apiTestHelper.createApiOne(getBackendEndServiceEndPointHttp("wildcard/resources"));
        APIDTO apiTwo = apiTestHelper.createApiTwo(getBackendEndServiceEndPointHttp("wildcard/resources"));
        apisToBeUsed.add(apiOne);
        apisToBeUsed.add(apiTwo);

        // Step 1 : Create APIProduct
        final String provider = user.getUserName();
        final String name = UUID.randomUUID().toString();
        final String context = "/" + UUID.randomUUID().toString();
        final String version = "1.0.0";

        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        APIProductDTO apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(provider, name, context, version,
                apisToBeUsed, policies);
        createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        waitForAPIDeployment();

        // Step 2 : Verify created APIProduct in publisher
        apiProductTestHelper.verfiyApiProductInPublisher(apiProductDTO);

        apiProductDTO = publishAPIProduct(apiProductDTO.getId());
        HttpResponse apiProductCopyResponse = restAPIPublisher.copyAPIProduct(APIVersionNew,
                apiProductDTO.getId(), false);

        Assert.assertEquals(apiProductCopyResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code Mismatch");
        apiProductId2 = apiProductCopyResponse.getData();

        //test the copied api Product
        APIProductDTO newApiProductDTO = restAPIPublisher.getApiProduct(apiProductId2);
        Assert.assertEquals(newApiProductDTO.getVersion(), APIVersionNew);
    }


    @Test(groups = {"wso2.am"}, description = "Create new version by setting isDefaultVersion and publish")
    public void testAPIProductNewVersionCreationWithDefaultVersion() throws Exception {
        // Pre-Conditions : Create APIs
        List<APIDTO> apisToBeUsed = new ArrayList<>();
        APIDTO apiOne = apiTestHelper.createApiOne(getBackendEndServiceEndPointHttp("wildcard/resources"));
        APIDTO apiTwo = apiTestHelper.createApiTwo(getBackendEndServiceEndPointHttp("wildcard/resources"));
        apisToBeUsed.add(apiOne);
        apisToBeUsed.add(apiTwo);

        // Step 1 : Create APIProduct
        final String provider = user.getUserName();
        final String name = UUID.randomUUID().toString();
        final String context = "/" + UUID.randomUUID().toString();
        final String version = "1.0.0";

        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        APIProductDTO apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(provider, name, context, version,
                apisToBeUsed, policies);
        createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        waitForAPIDeployment();

        // Step 2 : Verify created APIProduct in publisher
        apiProductTestHelper.verfiyApiProductInPublisher(apiProductDTO);

        String APIVersionNew  = "2.0.0";
        apiProductDTO = publishAPIProduct(apiProductDTO.getId());
        HttpResponse apiProductCopyResponse = restAPIPublisher.copyAPIProduct(APIVersionNew, apiProductDTO.getId(), true);
        Assert.assertEquals(apiProductCopyResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code Mismatch");

        apiProductId2 = apiProductCopyResponse.getData();

        //test the copied api Product
        APIProductDTO newApiProductDTO = restAPIPublisher.getApiProduct(apiProductId2);
        Assert.assertEquals(newApiProductDTO.getVersion(), APIVersionNew);

        boolean isDefaultVersion = Boolean.TRUE.equals(newApiProductDTO.isIsDefaultVersion());
        Assert.assertEquals(isDefaultVersion, true, "Copied API Product is not the default version");
    }

    @Test(groups = {"wso2.am"}, description = "Test creation of API Product with malformed context")
    public void testCreateApiProductWithMalformedContext() throws Exception {
        // Pre-Conditions : Create APIs
        List<APIDTO> apisToBeUsed = new ArrayList<>();
        APIDTO apiOne = apiTestHelper.createApiOne(getBackendEndServiceEndPointHttp("wildcard/resources"));
        APIDTO apiTwo = apiTestHelper.createApiTwo(getBackendEndServiceEndPointHttp("wildcard/resources"));
        apisToBeUsed.add(apiOne);
        apisToBeUsed.add(apiTwo);
        apiID1 = apiOne.getId();
        apiID2 = apiTwo.getId();

        // Step 1 : Create APIProduct
        final String provider = user.getUserName();
        final String name = UUID.randomUUID().toString();
        final String context = "/" + UUID.randomUUID().toString() + "{version}" ;
        final String version = "1.0.0";

        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        try{
            apiProductTestHelper.createAPIProductInPublisher(provider, name, context, version,
                    apisToBeUsed, policies);
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), Response.Status.BAD_REQUEST.getStatusCode());
            Assert.assertTrue(e.getResponseBody().contains(
                    APIMIntegrationConstants.API_PRODUCT_CONTEXT_MALFORMED_ERROR));
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test creation and invocation of API Product which depends " +
            "on a visibility restricted API")
    public void testCreateAndInvokeApiProductWithVisibilityRestrictedApi() throws Exception {
        // Pre-Conditions : Create APIs
        List<APIDTO> apisToBeUsed = new ArrayList<>();

        apisToBeUsed.add(apiTestHelper.
                createApiTwo(getBackendEndServiceEndPointHttp("wildcard/resources")));
        apisToBeUsed.add(apiTestHelper.
                createRestrictedAccessControlApi(getBackendEndServiceEndPointHttp("wildcard/resources"),
                        RESTRICTED_ROLE));

        // Step 1 : Create APIProduct
        final String provider = user.getUserName();
        final String name = UUID.randomUUID().toString();
        final String context = "/" + UUID.randomUUID().toString();
        final String version = "1.0.0";

        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        APIProductDTO apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(provider, name, context, version,
                apisToBeUsed, policies);
        createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        waitForAPIDeployment();

        // Step 2 : Verify created APIProduct in publisher
        apiProductTestHelper.verfiyApiProductInPublisher(apiProductDTO);

        apiProductDTO = publishAPIProduct(apiProductDTO.getId());

        // Step 3 : Verify APIProduct in dev portal
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDTO =
                apiProductTestHelper.verifyApiProductInPortal(apiProductDTO);

        // Step 4 : Subscribe to APIProduct
        ApplicationDTO applicationDTO = apiTestHelper.verifySubscription(apiDTO, UUID.randomUUID().toString(),
                TIER_UNLIMITED);

        // Step 5 : Generate Production and Sandbox keys and Application Access tokens without scopes
        List<String> grantTypes = Arrays.asList("client_credentials", "password");
        ApplicationKeyDTO productionAppKey = apiTestHelper.verifyKeyGeneration(applicationDTO,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, new ArrayList<>(), grantTypes);

        ApplicationKeyDTO sandboxAppKey = apiTestHelper.verifyKeyGeneration(applicationDTO,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX, new ArrayList<>(), grantTypes);

        // Step 6 : Invoke API Product with Application Access tokens
        InvocationStatusCodes invocationStatusCodes = new InvocationStatusCodes();
        apiTestHelper.verifyInvocation(apiDTO, productionAppKey.getToken().getAccessToken(),
                sandboxAppKey.getToken().getAccessToken(), invocationStatusCodes);

        // Step 7 : Generate Production and Sandbox User tokens without scopes
        String productionToken = apiTestHelper.generateTokenPasswordGrant(productionAppKey.getConsumerKey(),
                productionAppKey.getConsumerSecret(), STANDARD_SUBSCRIBER, PASSWORD,
                Collections.emptyList());

        String sandboxToken = apiTestHelper.generateTokenPasswordGrant(sandboxAppKey.getConsumerKey(),
                sandboxAppKey.getConsumerSecret(), STANDARD_SUBSCRIBER, PASSWORD,
                Collections.emptyList());

        // Step 8 : Invoke API Product with User Access tokens
        apiTestHelper.verifyInvocation(apiDTO, productionToken, sandboxToken, invocationStatusCodes);
    }

//     @Test(groups = {"wso2.am"}, description = "Test creation and deployment of API Product with an API " +
//             "category")
    public void testCreateAndInvokeApiProductWithAPICategoryAdded() throws Exception {
        // Create the Marketing category first
        APICategoryDTO categoryDTO = new APICategoryDTO();
        String provider = user.getUserName();
        String name = UUID.randomUUID().toString();
        String context = "/" + UUID.randomUUID().toString();
        String version = "1.0.0";

        String categoryName = "Marketing-" + UUID.randomUUID().toString().substring(0, 8);
        categoryDTO.setName(categoryName);
        categoryDTO.setDescription("Marketing category for testing");
        try {
            restAPIAdmin.addApiCategory(categoryDTO);
        } catch (org.wso2.am.integration.clients.admin.ApiException e) {
            //Assert the Status Code
            Assert.assertTrue(
                e.getResponseBody() != null && e.getResponseBody().contains("already exists"),
                "Unexpected error creating category: " + e.getResponseBody());
        }
        
        List<APIDTO> apisToBeUsed = new ArrayList<>();

        apisToBeUsed.add(apiTestHelper.createApiOne(getBackendEndServiceEndPointHttp("wildcard/resources")));

        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        APIProductDTO apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(provider, name, context, version,
                apisToBeUsed, policies);
        List<String> apiProductCategories = new ArrayList<>();
        apiProductCategories.add(categoryName);
        apiProductDTO.setCategories(apiProductCategories);
        ApiResponse<APIProductDTO> updateResponse = restAPIPublisher.updateAPIProduct(apiProductDTO);
        Assert.assertEquals(updateResponse.getStatusCode(), HttpStatus.SC_OK);
        apiProductDTO = updateResponse.getData();
        createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        waitForAPIDeployment();

        apiProductTestHelper.verfiyApiProductInPublisher(apiProductDTO);
        
        // Verify categories before publish
        List<String> categoriesBeforePublish = apiProductDTO.getCategories();
        Assert.assertNotNull(categoriesBeforePublish);
        Assert.assertTrue(categoriesBeforePublish.contains(categoryName));
        
        apiProductDTO = publishAPIProduct(apiProductDTO.getId());

        List<String> categoriesInPublisherAPI = apiProductDTO.getCategories();
        Assert.assertNotNull(categoriesInPublisherAPI);
        Assert.assertTrue(categoriesInPublisherAPI.contains(categoryName));

        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDTO =
                apiProductTestHelper.verifyApiProductInPortal(apiProductDTO);
        List<String> categoriesInReceivedAPI = apiDTO.getCategories();
        Assert.assertNotNull(categoriesInReceivedAPI);
        Assert.assertTrue(categoriesInReceivedAPI.contains(categoryName));
    }

    @Test(groups = {"wso2.am"}, description = "Test creation and invocation of API Product which depends " +
            "on a scope protected API")
    public void testCreateAndInvokeApiProductWithScopes() throws Exception {
        // Pre-Conditions : Create APIs
        List<APIDTO> apisToBeUsed = new ArrayList<>();

        apisToBeUsed.add(apiTestHelper.
                createApiTwo(getBackendEndServiceEndPointHttp("wildcard/resources")));
        apisToBeUsed.add(apiTestHelper.
                createScopeProtectedApi(getBackendEndServiceEndPointHttp("wildcard/resources"),
                        RESTRICTED_ROLE, SCOPE));

        // Step 1 : Create APIProduct
        final String provider = user.getUserName();
        final String name = UUID.randomUUID().toString();
        final String context = "/" + UUID.randomUUID().toString();
        final String version = "1.0.0";

        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        APIProductDTO apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(provider, name, context, version,
                apisToBeUsed, policies);
        createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        waitForAPIDeployment();

        // Step 2 : Verify created APIProduct in publisher
        apiProductTestHelper.verfiyApiProductInPublisher(apiProductDTO);

        apiProductDTO = publishAPIProduct(apiProductDTO.getId());

        // Step 3 : Verify APIProduct in dev portal
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDTO =
                apiProductTestHelper.verifyApiProductInPortal(apiProductDTO);

        // Step 4 : Subscribe to APIProduct
        ApplicationDTO applicationDTO = apiTestHelper.verifySubscription(apiDTO, UUID.randomUUID().toString(),
                TIER_UNLIMITED);

        // Step 5 : Generate Production and Sandbox keys
        List<String> grantTypes = Arrays.asList("client_credentials", "password");
        ApplicationKeyDTO productionAppKey = apiTestHelper.verifyKeyGeneration(applicationDTO,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                new ArrayList<>(), grantTypes);

        ApplicationKeyDTO sandboxAppKey = apiTestHelper.verifyKeyGeneration(applicationDTO,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX,
                new ArrayList<>(), grantTypes);

        // Step 6 : Invoke API Product with Application Access tokens and receive Forbidden response
        InvocationStatusCodes invocationStatusCodes = new InvocationStatusCodes();
        invocationStatusCodes.addScopeSpecificStatusCode(SCOPE, HttpStatus.SC_FORBIDDEN);
        apiTestHelper.verifyInvocation(apiDTO, productionAppKey.getToken().getAccessToken(),
                sandboxAppKey.getToken().getAccessToken(), invocationStatusCodes);

        // Step 7 : Generate Production and Sandbox tokens with scopes
        String productionToken = apiTestHelper.generateTokenPasswordGrant(productionAppKey.getConsumerKey(),
                productionAppKey.getConsumerSecret(), RESTRICTED_SUBSCRIBER, PASSWORD,
                Collections.singletonList(SCOPE));

        String sandboxToken = apiTestHelper.generateTokenPasswordGrant(sandboxAppKey.getConsumerKey(),
                sandboxAppKey.getConsumerSecret(), RESTRICTED_SUBSCRIBER, PASSWORD,
                Collections.singletonList(SCOPE));

        // Step 8 : Invoke API Product
        invocationStatusCodes.addScopeSpecificStatusCode(SCOPE, HttpStatus.SC_OK);
        apiTestHelper.verifyInvocation(apiDTO, productionToken, sandboxToken, invocationStatusCodes);
    }

    @Test(groups = {"wso2.am"}, description = "Test creation and invocation of API Product which depends " +
            "on an API with operation policies in request flow")
    public void testCreateAndInvokeApiProductWithOperationPoliciesInRequestApi() throws Exception {
        // Pre-Conditions : Create APIs
        List<APIDTO> apisToBeUsed = new ArrayList<>();
        APIDTO api = apiTestHelper.createAnApi(getBackendEndServiceEndPointHttp("wildcard/resources"));
        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setRequest(getPolicyList("jsonToXML", POLICY_TYPE_COMMON, null));
        apiOperationPoliciesDTO.setFault(getPolicyList("jsonFault", POLICY_TYPE_COMMON, null));
        for (APIOperationsDTO operationsDTO : api.getOperations()) {
            operationsDTO.setOperationPolicies(apiOperationPoliciesDTO);
        }
        restAPIPublisher.updateAPI(api);
        apisToBeUsed.add(api);

        // Step 1 : Create APIProduct
        final String provider = user.getUserName();
        final String name = UUID.randomUUID().toString();
        final String context = "/" + UUID.randomUUID().toString();
        final String version = "1.0.0";

        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        APIProductDTO apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(provider, name, context, version,
                apisToBeUsed, policies);
        createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        waitForAPIDeployment();

        // Step 2 : Verify created APIProduct in publisher
        apiProductTestHelper.verfiyApiProductInPublisher(apiProductDTO);

        apiProductDTO = publishAPIProduct(apiProductDTO.getId());

        // Step 3 : Verify APIProduct in dev portal
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDTO =
                apiProductTestHelper.verifyApiProductInPortal(apiProductDTO);

        // Step 4 : Subscribe to APIProduct
        ApplicationDTO applicationDTO = apiTestHelper.verifySubscription(apiDTO, UUID.randomUUID().toString(),
                TIER_UNLIMITED);

        // Step 5 : Generate Production and Sandbox keys
        List<String> grantTypes = Arrays.asList("client_credentials", "password");
        ApplicationKeyDTO productionAppKey = apiTestHelper.verifyKeyGeneration(applicationDTO,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                new ArrayList<>(), grantTypes);

        ApplicationKeyDTO sandboxAppKey = apiTestHelper.verifyKeyGeneration(applicationDTO,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX,
                new ArrayList<>(), grantTypes);

        // Step 6 : Invoke API Product with Application Access tokens
        String requestBody = "{\"foo\":\"bar\"}";
        String expectedResponse = "<jsonObject><foo>bar</foo></jsonObject>";
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        InvocationStatusCodes invocationStatusCodes = new InvocationStatusCodes();

        apiTestHelper.verifyInvocation(apiDTO, productionAppKey.getToken().getAccessToken(),
                sandboxAppKey.getToken().getAccessToken(), invocationStatusCodes, requestBody, expectedResponse,
                headers);

        // Step 7 : Generate Production and Sandbox tokens with scopes
        String productionToken = apiTestHelper.generateTokenPasswordGrant(productionAppKey.getConsumerKey(),
                productionAppKey.getConsumerSecret(), RESTRICTED_SUBSCRIBER, PASSWORD,
                Collections.emptyList());

        String sandboxToken = apiTestHelper.generateTokenPasswordGrant(sandboxAppKey.getConsumerKey(),
                sandboxAppKey.getConsumerSecret(), RESTRICTED_SUBSCRIBER, PASSWORD,
                Collections.emptyList());

        // Step 8 : Invoke API Product
        apiTestHelper.verifyInvocation(apiDTO, productionToken, sandboxToken, invocationStatusCodes, requestBody,
                expectedResponse, headers);
        // Step 9 : Change In mediation Sequence in base API and verify change reflect in APIProduct.
        Assert.assertNotNull(api);
        apiOperationPoliciesDTO.setRequest(getPolicyList("xmlToJson", POLICY_TYPE_COMMON, null));
        for (APIOperationsDTO operationsDTO : api.getOperations()) {
            operationsDTO.setOperationPolicies(apiOperationPoliciesDTO);
        }
        restAPIPublisher.updateAPI(api);
        apiTestHelper.verifyInvocation(apiDTO, productionToken, sandboxToken, invocationStatusCodes, requestBody,
                expectedResponse, headers);
        // Step10 : Update and deploy APIProduct with base API changes.
        ApiResponse<APIProductDTO> apiProductDTOApiResponse = restAPIPublisher.updateAPIProduct(apiProductDTO);
        Assert.assertEquals(apiProductDTOApiResponse.getStatusCode(), 200);
        createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        waitForAPIDeployment();
        headers.put("Content-Type", "text/xml");
        apiTestHelper.verifyInvocation(apiDTO, productionToken, sandboxToken, invocationStatusCodes, expectedResponse,
                requestBody, headers);
    }

    @Test(groups = {"wso2.am"}, description = "Test creation and invocation of API Product which depends " +
            "on an API with operation policies in response flow")
    public void testCreateAndInvokeApiProductWithOperationPoliciesInResponseApi() throws Exception {
        // Pre-Conditions : Create APIs
        List<APIDTO> apisToBeUsed = new ArrayList<>();
        APIDTO api = apiTestHelper.createAnApi(getBackendEndServiceEndPointHttp("wildcard/resources"));
        APIOperationPoliciesDTO apiOperationPoliciesDTO = new APIOperationPoliciesDTO();
        apiOperationPoliciesDTO.setResponse(getPolicyList("xmlToJson", POLICY_TYPE_COMMON, null));
        apiOperationPoliciesDTO.setFault(getPolicyList("jsonFault", POLICY_TYPE_COMMON, null));
        for (APIOperationsDTO operationsDTO : api.getOperations()) {
            operationsDTO.setOperationPolicies(apiOperationPoliciesDTO);
        }
        restAPIPublisher.updateAPI(api);
        apisToBeUsed.add(api);

        // Step 1 : Create APIProduct
        final String provider = user.getUserName();
        final String name = UUID.randomUUID().toString();
        final String context = "/" + UUID.randomUUID().toString();
        final String version = "1.0.0";

        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        APIProductDTO apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(provider, name, context, version,
                apisToBeUsed, policies);
        createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        waitForAPIDeployment();

        // Step 2 : Verify created APIProduct in publisher
        apiProductTestHelper.verfiyApiProductInPublisher(apiProductDTO);

        apiProductDTO = publishAPIProduct(apiProductDTO.getId());

        // Step 3 : Verify APIProduct in dev portal
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDTO =
                apiProductTestHelper.verifyApiProductInPortal(apiProductDTO);

        // Step 4 : Subscribe to APIProduct
        ApplicationDTO applicationDTO = apiTestHelper.verifySubscription(apiDTO, UUID.randomUUID().toString(),
                TIER_UNLIMITED);

        // Step 5 : Generate Production and Sandbox keys
        List<String> grantTypes = Arrays.asList("client_credentials", "password");
        ApplicationKeyDTO productionAppKey = apiTestHelper.verifyKeyGeneration(applicationDTO,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                new ArrayList<>(), grantTypes);

        ApplicationKeyDTO sandboxAppKey = apiTestHelper.verifyKeyGeneration(applicationDTO,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX,
                new ArrayList<>(), grantTypes);

        // Step 6 : Invoke API Product with Application Access tokens
        String xmlRequestBody = "<foo>bar</foo>";
        String jsonResponseBody = "{\"text\":\"<foo>bar</foo>\"}";
        String jsonRequestBody = "{\"foo\":\"bar\"}";
        String xmlResponseBody = "{\"text\":\"<jsonObject><foo>bar</foo></jsonObject>\"}";
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/xml");
        InvocationStatusCodes invocationStatusCodes = new InvocationStatusCodes();

        apiTestHelper.verifyInvocation(apiDTO, productionAppKey.getToken().getAccessToken(),
                sandboxAppKey.getToken().getAccessToken(), invocationStatusCodes, xmlRequestBody, jsonResponseBody,
                headers);

        // Step 7 : Generate Production and Sandbox tokens with scopes
        String productionToken = apiTestHelper.generateTokenPasswordGrant(productionAppKey.getConsumerKey(),
                productionAppKey.getConsumerSecret(), RESTRICTED_SUBSCRIBER, PASSWORD,
                Collections.emptyList());

        String sandboxToken = apiTestHelper.generateTokenPasswordGrant(sandboxAppKey.getConsumerKey(),
                sandboxAppKey.getConsumerSecret(), RESTRICTED_SUBSCRIBER, PASSWORD,
                Collections.emptyList());

        // Step 8 : Invoke API Product
        apiTestHelper.verifyInvocation(apiDTO, productionToken, sandboxToken, invocationStatusCodes, xmlRequestBody,
                jsonResponseBody, headers);
        // Step 9 : Change In mediation Sequence in base API and verify change reflect in APIProduct.
        Assert.assertNotNull(api);
        apiOperationPoliciesDTO.setRequest(getPolicyList("jsonToXML", POLICY_TYPE_COMMON, null));
        for (APIOperationsDTO operationsDTO : api.getOperations()) {
            operationsDTO.setOperationPolicies(apiOperationPoliciesDTO);
        }
        restAPIPublisher.updateAPI(api);
        apiTestHelper.verifyInvocation(apiDTO, productionToken, sandboxToken, invocationStatusCodes, xmlRequestBody,
                jsonResponseBody, headers);
        // Step10 : Update and deploy APIProduct with base API changes.
        ApiResponse<APIProductDTO> apiProductDTOApiResponse = restAPIPublisher.updateAPIProduct(apiProductDTO);
        Assert.assertEquals(apiProductDTOApiResponse.getStatusCode(), 200);
        createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        waitForAPIDeployment();
        headers.put("Content-Type", "application/json");
        apiTestHelper.verifyInvocation(apiDTO, productionToken, sandboxToken, invocationStatusCodes, jsonRequestBody,
                xmlResponseBody, headers);
    }

    @Test(groups = {"wso2.am"}, description = "Test creation of API Product with an Advertise only API")
    public void testCreateApiProductWithAdvertiseOnlyApi() throws Exception {
        // Pre-Conditions : Create APIs
        List<APIDTO> apisToBeUsed = new ArrayList<>();
        APIDTO advertiseOnlyApi = apiTestHelper
                .createAdvertiseOnlyApi(getBackendEndServiceEndPointHttp("wildcard/resources"));
        apisToBeUsed.add(advertiseOnlyApi);

        // Step 1 : Create APIProduct
        final String provider = user.getUserName();
        final String name = UUID.randomUUID().toString();
        final String context = "/" + UUID.randomUUID();
        final String version = "1.0.0";

        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        // Step 1 : Create APIProduct
        APIProductDTO apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(provider, name, context, version,
                apisToBeUsed, policies);
        createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        waitForAPIDeployment();

        // Step 2 : Verify created APIProduct in publisher
        apiProductTestHelper.verfiyApiProductInPublisher(apiProductDTO);

        apiProductDTO = publishAPIProduct(apiProductDTO.getId());

        // Step 3 : Verify APIProduct in dev portal
        org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDTO =
                apiProductTestHelper.verifyApiProductInPortal(apiProductDTO);

        // Step 4 : Subscribe to APIProduct
        ApplicationDTO applicationDTO = apiTestHelper.verifySubscription(apiDTO, UUID.randomUUID().toString(),
                TIER_UNLIMITED);

        // Step 5 : Generate Production and Sandbox keys and Application Access tokens without scopes
        List<String> grantTypes = Arrays.asList("client_credentials", "password");
        ApplicationKeyDTO productionAppKey = apiTestHelper.verifyKeyGeneration(applicationDTO,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, new ArrayList<>(), grantTypes);

        ApplicationKeyDTO sandboxAppKey = apiTestHelper.verifyKeyGeneration(applicationDTO,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX, new ArrayList<>(), grantTypes);

        // Step 6 : Invoke API Product with Application Access tokens
        InvocationStatusCodes invocationStatusCodes = new InvocationStatusCodes();
        apiTestHelper.verifyInvocation(apiDTO, productionAppKey.getToken().getAccessToken(),
                sandboxAppKey.getToken().getAccessToken(), invocationStatusCodes);

        // Step 7 : Generate Production and Sandbox User tokens without scopes
        String productionToken = apiTestHelper.generateTokenPasswordGrant(productionAppKey.getConsumerKey(),
                productionAppKey.getConsumerSecret(), STANDARD_SUBSCRIBER, PASSWORD,
                Collections.emptyList());

        String sandboxToken = apiTestHelper.generateTokenPasswordGrant(sandboxAppKey.getConsumerKey(),
                sandboxAppKey.getConsumerSecret(), STANDARD_SUBSCRIBER, PASSWORD,
                Collections.emptyList());

        // Step 8 : Invoke API Product with User Access tokens
        apiTestHelper.verifyInvocation(apiDTO, productionToken, sandboxToken, invocationStatusCodes);
    }

    @Test(groups = {"wso2.am"}, description = "Test deployment of API Product with Mutual SSL enabled")
    public void testCreateAndDeployApiProductWithMutualSSLEnabled() throws Exception {
        // Step 1: Create APIs
        List<APIDTO> apisToBeUsed = new ArrayList<>();
        apisToBeUsed.add(apiTestHelper.createApiOne(getBackendEndServiceEndPointHttp("wildcard/resources")));
        apisToBeUsed.add(apiTestHelper.createApiTwo(getBackendEndServiceEndPointHttp("wildcard/resources")));

        // Step 2: Create APIProduct
        String provider = user.getUserName();
        String name = UUID.randomUUID().toString();
        String context = "/" + UUID.randomUUID().toString();
        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);
        final String version = "1.0.0";

        APIProductDTO apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(provider, name, context, version,
                apisToBeUsed, policies);

        // Step 3: Enable Mutual SSL with client certificate
        List<String> securityScheme = Arrays.asList("mutualssl", "mutualssl_mandatory");
        apiProductDTO.setSecurityScheme(securityScheme);
        restAPIPublisher.updateAPIProduct(apiProductDTO);
        String certificate = getAMResourceLocation() + File.separator + "lifecycletest" + File.separator + "mutualssl"
                + File.separator + "example.crt";
        restAPIPublisher.uploadCertificate(new File(certificate), "example", apiProductDTO.getId(),
                APIMIntegrationConstants.API_TIER.UNLIMITED, APIMIntegrationConstants.KEY_TYPE.SANDBOX);

        // Step 4: Verify deployment of APIProduct with Mutual SSL enabled
        String revisionUUID = createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        Assert.assertNotNull(revisionUUID);
    }

    @Test(groups = { "wso2.am" }, description = "API product swagger definition reference verification")
    public void testAPIProductSwaggerDefinition() throws Exception {

        // Create a REST API using swagger definition
        List<APIDTO> apisToBeUsed = new ArrayList<>();
        String swaggerPath = resourcePath + "test-api-1-oas.yaml";
        File definition = new File(swaggerPath);
        org.json.JSONObject endpoints = new org.json.JSONObject();
        endpoints.put("url", "https://test.com");

        org.json.JSONObject endpointConfig = new org.json.JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("production_endpoints", endpoints);
        endpointConfig.put("sandbox_endpoints", endpoints);

        List<String> tierList = new ArrayList<>();
        tierList.add(APIMIntegrationConstants.API_TIER.SILVER);
        tierList.add(APIMIntegrationConstants.API_TIER.GOLD);

        org.json.JSONObject apiProperties = new org.json.JSONObject();
        apiProperties.put("name", "testAPI1");
        apiProperties.put("context", "/testapi1");
        apiProperties.put("version", "1.0.0");
        apiProperties.put("provider", user.getUserName());
        apiProperties.put("endpointConfig", endpointConfig);
        apiProperties.put("policies", tierList);

        APIDTO apiOne = restAPIPublisher.importOASDefinition(definition, apiProperties.toString());
        APIDTO apiTwo = apiTestHelper.createApiTwo(getBackendEndServiceEndPointHttp("wildcard/resources"));
        apisToBeUsed.add(apiOne);
        apisToBeUsed.add(apiTwo);

        // Create API Product and verify in publisher
        final String provider = user.getUserName();
        final String name = UUID.randomUUID().toString();
        final String context = "/" + UUID.randomUUID().toString();
        final String version = "1.0.0";

        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        APIProductDTO apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(
                provider, name, context, version, apisToBeUsed, policies);
        createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        waitForAPIDeployment();
        apiProductTestHelper.verfiyApiProductInPublisher(apiProductDTO);
        apiProductDTO = publishAPIProduct(apiProductDTO.getId());
        String apiProductID = apiProductDTO.getId();

        // Get api product definition and validate
        String apiProductDefinition = restAPIPublisher.getAPIProductSwaggerByID(apiProductID);
        validateDefinition(apiProductDefinition);
    }

    private void validateDefinition(String oasDefinition) throws Exception {
        File file = geTempFileWithContent(oasDefinition);
        OpenAPIDefinitionValidationResponseDTO responseDTO = restAPIPublisher.validateOASDefinition(file);
        assertTrue(responseDTO.isIsValid());
    }

    private File geTempFileWithContent(String swagger) throws Exception {
        File temp = File.createTempFile("swagger", ".json");
        temp.deleteOnExit();
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.write(swagger);
        out.close();
        return temp;
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {

        restAPIPublisher.deleteAPI(apiID1);
        restAPIPublisher.deleteAPI(apiID2);
        super.cleanUp();
        userManagementClient.deleteUser(RESTRICTED_SUBSCRIBER);
        userManagementClient.deleteUser(STANDARD_SUBSCRIBER);
        userManagementClient.deleteRole(RESTRICTED_ROLE);
    }

    private HttpResponse invokeWithGet(String url, Map<String, String> headers) throws IOException {

        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(url);
        headers.forEach(get::addHeader);
        org.apache.http.HttpResponse response = httpclient.execute(get);
        InputStream stream = response.getEntity().getContent();
        String content = IOUtils.toString(stream);
        Map<String, String> outputHeaders = new HashMap();
        for (Header header : response.getAllHeaders()) {
            outputHeaders.put(header.getName(), header.getValue());
        }
        return new HttpResponse(content, response.getStatusLine().getStatusCode(), outputHeaders);
    }

    private APIProductDTO publishAPIProduct(String uuid) throws ApiException, APIManagerIntegrationTestException {

        WorkflowResponseDTO workflowResponseDTO = apiProductTestHelper.changeLifecycleStateOfApiProduct(uuid,
                "Publish", null);
        assertNotNull(workflowResponseDTO);
        LifecycleStateDTO lifecycleStateDTO = workflowResponseDTO.getLifecycleState();
        assertNotNull(lifecycleStateDTO);
        assertEquals("APPROVED", workflowResponseDTO.getWorkflowStatus().getValue());
        assert APILifeCycleState.PUBLISHED.getState().equals(lifecycleStateDTO.getState());

        return restAPIPublisher.getApiProduct(uuid);
    }

    public List<OperationPolicyDTO> getPolicyList(String policyName, String policyType, Map<String,
            Object> attributeMap) {

        List<OperationPolicyDTO> policyList = new ArrayList<>();
        OperationPolicyDTO policyDTO = new OperationPolicyDTO();
        policyDTO.setPolicyName(policyName);
        policyDTO.setPolicyType(policyType);
        policyDTO.setParameters(attributeMap);
        policyList.add(policyDTO);

        return policyList;
    }
}

