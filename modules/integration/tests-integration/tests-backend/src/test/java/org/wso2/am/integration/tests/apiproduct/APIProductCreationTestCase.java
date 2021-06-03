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
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIProductDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.MediationPolicyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.impl.ApiProductTestHelper;
import org.wso2.am.integration.test.impl.ApiTestHelper;
import org.wso2.am.integration.test.impl.InvocationStatusCodes;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class APIProductCreationTestCase extends APIManagerLifecycleBaseTest {

    private static final String INTERNAL_ROLE_SUBSCRIBER = "Internal/subscriber";
    private static final String RESTRICTED_SUBSCRIBER = "restricted_user";
    private static final String STANDARD_SUBSCRIBER = "standard_user";
    private static final String PASSWORD = "$3213#@sd";
    private static final String RESTRICTED_ROLE = "restricted_role";
    private static final String SCOPE = "restricted_scope";
    private ApiTestHelper apiTestHelper;
    private ApiProductTestHelper apiProductTestHelper;

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

        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        APIProductDTO apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(provider, name, context,
                apisToBeUsed, policies);
        createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        waitForAPIDeployment();

        // Step 2 : Verify created APIProduct in publisher
        apiProductTestHelper.verfiyApiProductInPublisher(apiProductDTO);

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

        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        APIProductDTO apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(provider, name, context,
                apisToBeUsed, policies);
        createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        waitForAPIDeployment();

        // Step 2 : Verify created APIProduct in publisher
        apiProductTestHelper.verfiyApiProductInPublisher(apiProductDTO);

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

        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        APIProductDTO apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(provider, name, context,
                apisToBeUsed, policies);
        createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        waitForAPIDeployment();

        // Step 2 : Verify created APIProduct in publisher
        apiProductTestHelper.verfiyApiProductInPublisher(apiProductDTO);

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
            "on an API with an in mediation sequence")
    public void testCreateAndInvokeApiProductWithInMediationApi() throws Exception {
        // Pre-Conditions : Create APIs
        List<APIDTO> apisToBeUsed = new ArrayList<>();
        APIDTO api = apiTestHelper.
                createInMediationSequenceApi(getBackendEndServiceEndPointHttp("wildcard/resources"));
        apisToBeUsed.add(api);

        // Step 1 : Create APIProduct
        final String provider = user.getUserName();
        final String name = UUID.randomUUID().toString();
        final String context = "/" + UUID.randomUUID().toString();

        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        APIProductDTO apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(provider, name, context,
                apisToBeUsed, policies);
        createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        waitForAPIDeployment();

        // Step 2 : Verify created APIProduct in publisher
        apiProductTestHelper.verfiyApiProductInPublisher(apiProductDTO);

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
        String requestBody = "{ \"foo\" : \"bar\" }";
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
        for (MediationPolicyDTO mediationPolicy : api.getMediationPolicies()) {
            if ("in".equals(mediationPolicy.getType())) {
                mediationPolicy.setId(null);
                mediationPolicy.setName("xml_to_json_in_message");
            }
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
            "on an API with an out mediation sequence")
    public void testCreateAndInvokeApiProductWithOutMediationApi() throws Exception {
        // Pre-Conditions : Create APIs
        List<APIDTO> apisToBeUsed = new ArrayList<>();
        APIDTO api = apiTestHelper
                .createOutMediationSequenceApi(getBackendEndServiceEndPointHttp("wildcard/resources"));
        apisToBeUsed.add(api);

        // Step 1 : Create APIProduct
        final String provider = user.getUserName();
        final String name = UUID.randomUUID().toString();
        final String context = "/" + UUID.randomUUID().toString();

        List<String> policies = Arrays.asList(TIER_UNLIMITED, TIER_GOLD);

        APIProductDTO apiProductDTO = apiProductTestHelper.createAPIProductInPublisher(provider, name, context,
                apisToBeUsed, policies);
        createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        waitForAPIDeployment();

        // Step 2 : Verify created APIProduct in publisher
        apiProductTestHelper.verfiyApiProductInPublisher(apiProductDTO);

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
        String requestBody = "<foo>bar</foo>";
        String expectedResponse = "{\"text\":\"<foo>bar</foo>\"}";
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/xml");
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
        for (MediationPolicyDTO mediationPolicy : api.getMediationPolicies()) {
            if ("out".equals(mediationPolicy.getType())) {
                mediationPolicy.setId(null);
                mediationPolicy.setName("json_to_xml_out_message");
            }
        }
        restAPIPublisher.updateAPI(api);
        apiTestHelper.verifyInvocation(apiDTO, productionToken, sandboxToken, invocationStatusCodes, requestBody,
                expectedResponse, headers);
        // Step10 : Update and deploy APIProduct with base API changes.
        ApiResponse<APIProductDTO> apiProductDTOApiResponse = restAPIPublisher.updateAPIProduct(apiProductDTO);
        Assert.assertEquals(apiProductDTOApiResponse.getStatusCode(), 200);
        createAPIProductRevisionAndDeployUsingRest(apiProductDTO.getId(), restAPIPublisher);
        waitForAPIDeployment();
        headers.put("Content-Type", "application/json");
        apiTestHelper.verifyInvocation(apiDTO, productionToken, sandboxToken, invocationStatusCodes, expectedResponse,
                requestBody, headers);
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {

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

}

