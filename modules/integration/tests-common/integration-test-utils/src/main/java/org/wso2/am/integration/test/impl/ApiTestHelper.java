/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.am.integration.test.impl;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIEndpointURLsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIInfoDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APITiersDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIURLsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationInfoDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationTokenDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.wso2.am.integration.clients.store.api.v1.dto.WorkflowResponseDTO.WorkflowStatusEnum.APPROVED;

public class ApiTestHelper {
    private static String SWAGGER_FOLDER = "swagger";
    private static String ADDITIONAL_PROPERTIES_FOLDER = "additional-properties";

    private RestAPIPublisherImpl restAPIPublisher;
    private RestAPIStoreImpl restAPIStore;
    private final String resourceLocation;
    private final String tenantDomain;
    private final String keyManagerUrl;
    private final User user;
    public ApiTestHelper(RestAPIPublisherImpl restAPIPublisher, RestAPIStoreImpl restAPIStore, String resourceLocation,
                         String tenantDomain, String keyManagerUrl, User user) {
        this.restAPIPublisher = restAPIPublisher;
        this.restAPIStore = restAPIStore;
        this.resourceLocation = resourceLocation;
        this.tenantDomain = tenantDomain;
        this.keyManagerUrl = keyManagerUrl;
        this.user = user;
    }

    public APIDTO createApiOne(String backendUrl) throws ApiException {
        String swaggerPath = resourceLocation + File.separator + SWAGGER_FOLDER +
                File.separator + "customer-info-api.yaml";

        return getApiDTO(backendUrl, swaggerPath);
    }

    public APIDTO createApiTwo(String backendUrl) throws ApiException {
        String swaggerPath = resourceLocation + File.separator + SWAGGER_FOLDER +
                File.separator + "leasing-api.yaml";

        return getApiDTO(backendUrl, swaggerPath);
    }

    private APIDTO getApiDTO(String backendUrl, String swaggerPath) throws ApiException {
        File definition = new File(swaggerPath);

        JSONObject endpoints = new JSONObject();
        endpoints.put("url", backendUrl);

        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("production_endpoints", endpoints);
        endpointConfig.put("sandbox_endpoints", endpoints);

        String uniqueName = UUID.randomUUID().toString();


        JSONObject apiProperties = new JSONObject();
        apiProperties.put("name", uniqueName);
        apiProperties.put("context", "/" + uniqueName);
        apiProperties.put("version", "1.0.0");
        apiProperties.put("provider", user.getUserName());
        apiProperties.put("endpointConfig", endpointConfig);

        return restAPIPublisher.importOASDefinition(definition, apiProperties.toString());
    }

    public APIDTO createRestrictedAccessControlApi(String backendUrl, String role) throws ApiException {
        String swaggerPath = resourceLocation + File.separator + SWAGGER_FOLDER +
                File.separator + "customer-info-api.yaml";

        File definition = new File(swaggerPath);

        JSONObject endpoints = new JSONObject();
        endpoints.put("url", backendUrl);

        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("production_endpoints", endpoints);
        endpointConfig.put("sandbox_endpoints", endpoints);



        String uniqueName = UUID.randomUUID().toString();
        JSONObject apiProperties = new JSONObject();
        apiProperties.put("name", uniqueName);
        apiProperties.put("context", "/" + uniqueName);
        apiProperties.put("version", "1.0.0");
        apiProperties.put("provider", user.getUserName());
        apiProperties.put("endpointConfig", endpointConfig);
        apiProperties.put("visibility", "RESTRICTED");
        apiProperties.put("visibleRoles", Collections.singletonList(role));

        return restAPIPublisher.importOASDefinition(definition, apiProperties.toString());
    }

    public APIDTO createScopeProtectedApi(String backendUrl, String role, String scope) throws ApiException, IOException {
        String swaggerPath = resourceLocation + File.separator + SWAGGER_FOLDER +
                File.separator + "customer-info-api.yaml";

        String additionalPropertiesPath = resourceLocation + File.separator + ADDITIONAL_PROPERTIES_FOLDER +
                File.separator + "scoped-api-properties.json";

        File definition = new File(swaggerPath);

        String content = new String(Files.readAllBytes(Paths.get(additionalPropertiesPath)));
        JSONObject apiProperties = new JSONObject(content);

        String uniqueName = UUID.randomUUID().toString();
        apiProperties.put("name", uniqueName);
        apiProperties.put("context", "/" + uniqueName);
        apiProperties.put("provider", user.getUserName());
        ((JSONObject) ((JSONObject) apiProperties.get("endpointConfig")).get("production_endpoints")).put("url", backendUrl);
        ((JSONObject) ((JSONObject) apiProperties.get("endpointConfig")).get("sandbox_endpoints")).put("url", backendUrl);


        ((JSONObject) (((JSONObject) (apiProperties.getJSONArray("scopes")).get(0)).get("scope"))).put("bindings",
                Collections.singletonList(role));

        ((JSONObject) (((JSONObject) (apiProperties.getJSONArray("scopes")).get(0)).get("scope"))).put("name", scope);
        ((JSONObject) (((JSONObject) (apiProperties.getJSONArray("scopes")).get(0)).get("scope"))).put("description", scope);

        JSONArray operations = apiProperties.getJSONArray("operations");

        for (int i = 0; i < operations.length(); ++i) {
            ((JSONObject) operations.get(i)).put("scopes", Collections.singletonList(scope));
        }

        return restAPIPublisher.importOASDefinition(definition, apiProperties.toString());
    }

    public APIDTO createAnApi(String backendUrl) throws IOException, ApiException {
        String swaggerPath = resourceLocation + File.separator + SWAGGER_FOLDER +
                File.separator + "customer-info-api.yaml";

        String additionalPropertiesPath = resourceLocation + File.separator + ADDITIONAL_PROPERTIES_FOLDER +
                File.separator + "api-properties.json";

        File definition = new File(swaggerPath);

        String content = new String(Files.readAllBytes(Paths.get(additionalPropertiesPath)));
        JSONObject apiProperties = new JSONObject(content);

        String uniqueName = UUID.randomUUID().toString();
        apiProperties.put("name", uniqueName);
        apiProperties.put("context", "/" + uniqueName);
        apiProperties.put("provider", user.getUserName());
        ((JSONObject) ((JSONObject) apiProperties.get("endpointConfig")).get("production_endpoints")).put("url", backendUrl);
        ((JSONObject) ((JSONObject) apiProperties.get("endpointConfig")).get("sandbox_endpoints")).put("url", backendUrl);

        return restAPIPublisher.importOASDefinition(definition, apiProperties.toString());
    }

    public APIDTO createAdvertiseOnlyApi(String backendUrl) throws ApiException {
        String swaggerPath = resourceLocation + File.separator + SWAGGER_FOLDER +
                File.separator + "customer-info-api.yaml";

        File definition = new File(swaggerPath);

        JSONObject apiProperties = new JSONObject();

        String uniqueName = UUID.randomUUID().toString();
        apiProperties.put("name", uniqueName);
        apiProperties.put("context", "/" + uniqueName);
        apiProperties.put("version", "1.0.0");
        apiProperties.put("provider", user.getUserName());

        JSONObject advertiseInfo = new JSONObject();
        advertiseInfo.put("advertised", true);
        advertiseInfo.put("apiExternalProductionEndpoint", backendUrl);
        advertiseInfo.put("apiExternalSandboxEndpoint", backendUrl);
        advertiseInfo.put("originalDevPortalUrl", "https://test.com");
        advertiseInfo.put("vendor", "WSO2");
        advertiseInfo.put("apiOwner", user.getUserName());
        apiProperties.put("advertiseInfo", advertiseInfo);

        return restAPIPublisher.importOASDefinition(definition, apiProperties.toString());
    }

    public ApplicationDTO verifySubscription(org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDTO,
                                             String applicationName, String subscriptionPolicy)
            throws org.wso2.am.integration.clients.store.api.ApiException, APIManagerIntegrationTestException {
        ApplicationDTO applicationDTO = restAPIStore.addApplication(applicationName,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "http://localhost", "");

        SubscriptionDTO subscriptionDTO = null;
        subscriptionDTO = restAPIStore.subscribeToAPI(apiDTO.getId(),
                applicationDTO.getApplicationId(), subscriptionPolicy);

        Assert.assertEquals(subscriptionDTO.getApiId(), apiDTO.getId());
        Assert.assertEquals(subscriptionDTO.getApplicationId(), applicationDTO.getApplicationId());
        verifySubscriptionApiInfo(subscriptionDTO.getApiInfo(), apiDTO);
        verifySubscriptionAppInfo(subscriptionDTO.getApplicationInfo(), applicationDTO);
        Assert.assertEquals(subscriptionDTO.getStatus(), SubscriptionDTO.StatusEnum.UNBLOCKED);
        Assert.assertEquals(subscriptionDTO.getThrottlingPolicy(), subscriptionPolicy);

        return restAPIStore.getApplicationById(applicationDTO.getApplicationId());
    }

    public ApplicationKeyDTO verifyKeyGeneration(ApplicationDTO applicationDTO,
                                      ApplicationKeyGenerateRequestDTO.KeyTypeEnum keyType,
                                      ArrayList<String> scopes, List<String> grantTypes)
            throws org.wso2.am.integration.clients.store.api.ApiException, APIManagerIntegrationTestException {
        final String validityTime = "3600";
        final String callbackUrl = "http://localhost";

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationDTO.getApplicationId(),
                validityTime, callbackUrl, keyType, scopes, grantTypes);

        Assert.assertEquals(applicationKeyDTO.getCallbackUrl(), callbackUrl);
        Assert.assertEquals(applicationKeyDTO.getKeyType().getValue(), keyType.getValue());
        Assert.assertEquals(new HashSet<>(applicationKeyDTO.getSupportedGrantTypes()), new HashSet<>(grantTypes));
        Assert.assertEquals(applicationKeyDTO.getKeyState(), APPROVED.getValue());

        ApplicationTokenDTO token = applicationKeyDTO.getToken();

        List<String> expectedScopes = new ArrayList<>(Arrays.asList("default"));
        expectedScopes.addAll(scopes);
        Assert.assertEquals(new HashSet<>(token.getTokenScopes()), new HashSet<>(expectedScopes));

        Assert.assertEquals(token.getValidityTime().longValue(), Long.parseLong(validityTime));
        return applicationKeyDTO;
    }

    public String generateTokenPasswordGrant(String consumerKey, String consumerSecret, String userName, String password,
                                            List<String> scopes) throws APIManagerIntegrationTestException,
                                                                            MalformedURLException {
        String tokenEndpointURL = keyManagerUrl + "oauth2/token";
        if (!"carbon.super".equals(tenantDomain)) {
            userName = userName + "@" + tenantDomain;
        }

        String requestBody = "grant_type=password&username=" + userName +
                "&password=" + password;

        if (!scopes.isEmpty()) {
            requestBody += "&scope=" + String.join(" ", scopes);
        }

        HttpResponse response = restAPIStore.generateUserAccessKey(consumerKey, consumerSecret,
                requestBody, new URL(tokenEndpointURL));

        JSONObject tokenResponse = new JSONObject(response.getData());

        if (!scopes.isEmpty()) {
            String receivedScopes = tokenResponse.getString("scope");
            Assert.assertEquals(new HashSet<>(Arrays.asList(receivedScopes.split(" "))), scopes);
        }

        return tokenResponse.getString("access_token");
    }

    public void verifyInvocation(org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDTO,
                                 String productionAccessToken, String sandboxAccessToken,
                                 InvocationStatusCodes expectedStatus)
            throws IOException {
        List<APIEndpointURLsDTO> endpointURLs = apiDTO.getEndpointURLs();
        Assert.assertFalse(endpointURLs.isEmpty());

        for (APIEndpointURLsDTO endpointURL : endpointURLs) {
            String environmentType = endpointURL.getEnvironmentType();
            switch (environmentType) {
                case "hybrid":
                    sendRequest(endpointURL.getUrLs(), apiDTO, productionAccessToken, expectedStatus);
                    sendRequest(endpointURL.getUrLs(), apiDTO, sandboxAccessToken, expectedStatus);
                    break;
                case "production":
                    sendRequest(endpointURL.getUrLs(), apiDTO, productionAccessToken, expectedStatus);
                    break;
                case "sandbox":
                    sendRequest(endpointURL.getUrLs(), apiDTO, sandboxAccessToken, expectedStatus);
                    break;
                default:
                    Assert.assertTrue(Arrays.stream(new String[]{"hybrid", "production", "sandbox"}).
                            parallel().anyMatch(environmentType::contains));
                    break;
            }
        }
    }

    public void verifyInvocation(org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDTO,
                                 String productionAccessToken, String sandboxAccessToken,
                                 InvocationStatusCodes expectedStatus,
                                 String requestBody, String expectedResponse, Map<String, String> headers)
            throws IOException {
        List<APIEndpointURLsDTO> endpointURLs = apiDTO.getEndpointURLs();
        Assert.assertFalse(endpointURLs.isEmpty());

        for (APIEndpointURLsDTO endpointURL : endpointURLs) {
            String environmentType = endpointURL.getEnvironmentType();
            switch (environmentType) {
                case "hybrid":
                    sendRequest(endpointURL.getUrLs(), apiDTO, productionAccessToken, expectedStatus, requestBody, expectedResponse, headers);
                    sendRequest(endpointURL.getUrLs(), apiDTO, sandboxAccessToken, expectedStatus, requestBody, expectedResponse, headers);
                    break;
                case "production":
                    sendRequest(endpointURL.getUrLs(), apiDTO, productionAccessToken, expectedStatus, requestBody, expectedResponse, headers);
                    break;
                case "sandbox":
                    sendRequest(endpointURL.getUrLs(), apiDTO, sandboxAccessToken, expectedStatus, requestBody, expectedResponse, headers);
                    break;
                default:
                    Assert.assertTrue(Arrays.stream(new String[]{"hybrid", "production", "sandbox"}).
                            parallel().anyMatch(environmentType::contains));
                    break;
            }
        }
    }

    private HttpUriRequest constructRequest(String targetUrl, String httpMethod) throws IOException {
        // Detect and replace resource path parameters with fixed id
        targetUrl = targetUrl.replaceAll("\\{\\w+}", "123");

        if ("GET".equals(httpMethod)) {
            return new HttpGet(targetUrl);
        } else if ("POST".equals(httpMethod)) {
            HttpPost request = new HttpPost(targetUrl);
            request.setEntity(new StringEntity("<test/>"));
            return request;
        } else if ("PUT".equals(httpMethod)) {
            HttpPut request = new HttpPut(targetUrl);
            request.setEntity(new StringEntity("<test/>"));
            return request;
        } else if ("DELETE".equals(httpMethod)) {
            return new HttpDelete(targetUrl);
        } else if ("HEAD".equals(httpMethod)) {
            return new HttpHead(targetUrl);
        } else {
            HttpPatch request = new HttpPatch(targetUrl);
            request.setEntity(new StringEntity("<test/>"));
            return request;
        }
    }

    private HttpUriRequest constructRequest(String targetUrl, String httpMethod, String requestBody) throws IOException {
        // Detect and replace resource path parameters with fixed id
        targetUrl = targetUrl.replaceAll("\\{\\w+}", "123");
        HttpEntityEnclosingRequestBase request;

        if ("POST".equals(httpMethod)) {
             request = new HttpPost(targetUrl);
        } else if ("PUT".equals(httpMethod)) {
            request = new HttpPut(targetUrl);
        } else {
            request = new HttpPatch(targetUrl);
        }

        request.setEntity(new StringEntity(requestBody));
        return request;
    }

    private void sendRequest(APIURLsDTO apiurLsDTO, org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDTO,
                             String accessToken, InvocationStatusCodes expectedStatusCodes) throws IOException {
        List<org.wso2.am.integration.clients.store.api.v1.dto.APIOperationsDTO> operations = apiDTO.getOperations();

        Map<String, String> pathScopeMapping = getPathScopeMapping(apiDTO);

        for (org.wso2.am.integration.clients.store.api.v1.dto.APIOperationsDTO operation : operations) {
            HttpUriRequest request = constructRequest(apiurLsDTO.getHttp() + operation.getTarget(),
                    operation.getVerb());
            // add request headers
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

            try (CloseableHttpClient httpClient = HttpClients.custom().
                    setHostnameVerifier(new AllowAllHostnameVerifier()).build();
                 CloseableHttpResponse response = httpClient.execute(request)) {
                String key = operation.getTarget() + operation.getVerb().toLowerCase();;
                int expectedCode;
                if (pathScopeMapping.containsKey(key)) {
                    String scope = pathScopeMapping.get(key);
                    expectedCode = expectedStatusCodes.getStatusCodeForScope(scope);
                } else {
                    expectedCode = expectedStatusCodes.getDefaultStatusCode();
                }

                Assert.assertEquals(response.getStatusLine().getStatusCode(), expectedCode);
            }
        }
    }

    private void sendRequest(APIURLsDTO apiurLsDTO, org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDTO,
                             String accessToken, InvocationStatusCodes expectedStatusCodes, String requestBody,
                             String expectedResponse, Map<String, String> headers)
            throws IOException {
        List<org.wso2.am.integration.clients.store.api.v1.dto.APIOperationsDTO> operations = apiDTO.getOperations();

        Map<String, String> pathScopeMapping = getPathScopeMapping(apiDTO);

        for (org.wso2.am.integration.clients.store.api.v1.dto.APIOperationsDTO operation : operations) {
            if (operation.getVerb().equals("GET") || operation.getVerb().equals("DELETE")) {
                continue;
            }

            HttpUriRequest request = constructRequest(apiurLsDTO.getHttp() + operation.getTarget(),
                    operation.getVerb(), requestBody);
            // add request headers
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

            for (Map.Entry<String, String> entry : headers.entrySet()) {
                request.addHeader(entry.getKey(), entry.getValue());
            }

            try (CloseableHttpClient httpClient = HttpClients.custom().
                    setHostnameVerifier(new AllowAllHostnameVerifier()).build();
                 CloseableHttpResponse response = httpClient.execute(request)) {
                String key = operation.getTarget() + operation.getVerb().toLowerCase();
                int expectedCode;
                if (pathScopeMapping.containsKey(key)) {
                    String scope = pathScopeMapping.get(key);
                    expectedCode = expectedStatusCodes.getStatusCodeForScope(scope);
                } else {
                    expectedCode = expectedStatusCodes.getDefaultStatusCode();
                }

                Assert.assertEquals(response.getStatusLine().getStatusCode(), expectedCode);
                Assert.assertEquals(EntityUtils.toString(response.getEntity()), expectedResponse);
            }
        }
    }

    private Map<String, String> getPathScopeMapping(org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDTO) {
        Map<String, String> pathScopeMapping = new HashMap<>();

        JSONObject apiDefinition = new JSONObject(apiDTO.getApiDefinition());
        JSONObject paths = (JSONObject) apiDefinition.get("paths");

        paths.keys().forEachRemaining(pathKey -> {
            JSONObject resources = (JSONObject) paths.get((String) pathKey);
            resources.keys().forEachRemaining(verbKey -> {
                JSONObject resource = (JSONObject) resources.get((String) verbKey);
                JSONArray securities = (JSONArray) resource.get("security");
                if (securities.length() == 1) {
                    JSONObject security = (JSONObject) securities.get(0);
                    JSONArray scopes = (JSONArray) security.get("default");
                    if (scopes.length() == 1) {
                        pathScopeMapping.put((String) pathKey + verbKey, (String) scopes.get(0));
                    }
                }
            });
        });

        return pathScopeMapping;
    }


    private void verifySubscriptionApiInfo(APIInfoDTO apiInfo,
                                           org.wso2.am.integration.clients.store.api.v1.dto.APIDTO apiDTO) {
        Assert.assertEquals(apiInfo.getId(), apiDTO.getId());
        Assert.assertEquals(apiInfo.getProvider(), apiDTO.getProvider());
        Assert.assertEquals(apiInfo.getName(), apiDTO.getName());
        verifySubscriptionApiInfoPolicies(apiInfo.getThrottlingPolicies(), apiDTO.getTiers());
        Assert.assertEquals(apiInfo.getLifeCycleStatus(), apiDTO.getLifeCycleStatus());
        Assert.assertEquals(apiInfo.getDescription(), apiDTO.getDescription());
        Assert.assertEquals(apiInfo.getContext(), apiDTO.getContext());
    }

    private void verifySubscriptionApiInfoPolicies(List<String> policies, List<APITiersDTO> tiers) {
        Assert.assertEquals(policies.size(), tiers.size());

        tiers.sort(Comparator.comparing(APITiersDTO::getTierName));
        policies.sort(Comparator.naturalOrder());

        for (int i = 0; i < policies.size(); ++i) {
            APITiersDTO apiTiersDTO = tiers.get(i);
            Assert.assertEquals(policies.get(i), apiTiersDTO.getTierName());
        }
    }


    private void verifySubscriptionAppInfo(ApplicationInfoDTO applicationInfo, ApplicationDTO applicationDTO) {
        Assert.assertEquals(applicationInfo.getApplicationId(), applicationDTO.getApplicationId());
        Assert.assertEquals(applicationInfo.getDescription(), applicationDTO.getDescription());
        Assert.assertEquals(applicationInfo.getName(), applicationDTO.getName());
        Assert.assertEquals(new HashSet<>(applicationInfo.getGroups()), new HashSet<>(applicationDTO.getGroups()));
        Assert.assertEquals(applicationInfo.getOwner(), applicationDTO.getOwner());
        Assert.assertEquals(applicationInfo.getStatus(), applicationDTO.getStatus());
        Assert.assertEquals(applicationInfo.getThrottlingPolicy(), applicationDTO.getThrottlingPolicy());
        Assert.assertEquals(applicationInfo.getSubscriptionCount(),
                new Integer(applicationDTO.getSubscriptionCount() + 1));
    }
}
