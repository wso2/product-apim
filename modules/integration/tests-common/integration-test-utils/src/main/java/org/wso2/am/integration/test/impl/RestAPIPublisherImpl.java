/*
 * Copyright (c) 2019, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

package org.wso2.am.integration.test.impl;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.testng.Assert;
import org.wso2.am.integration.clients.gateway.api.v2.dto.APIInfoDTO;
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.ApIsApi;
import org.wso2.am.integration.clients.publisher.api.v1.ApiAuditApi;
import org.wso2.am.integration.clients.publisher.api.v1.ApiDocumentsApi;
import org.wso2.am.integration.clients.publisher.api.v1.ApiLifecycleApi;
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductRevisionsApi;
import org.wso2.am.integration.clients.publisher.api.v1.ApiProductsApi;
import org.wso2.am.integration.clients.publisher.api.v1.ApiResourcePoliciesApi;
import org.wso2.am.integration.clients.publisher.api.v1.ApiRevisionsApi;
import org.wso2.am.integration.clients.publisher.api.v1.ClientCertificatesApi;
import org.wso2.am.integration.clients.publisher.api.v1.CommentsApi;
import org.wso2.am.integration.clients.publisher.api.v1.EndpointCertificatesApi;
import org.wso2.am.integration.clients.publisher.api.v1.GlobalMediationPoliciesApi;
import org.wso2.am.integration.clients.publisher.api.v1.GraphQlPoliciesApi;
import org.wso2.am.integration.clients.publisher.api.v1.GraphQlSchemaApi;
import org.wso2.am.integration.clients.publisher.api.v1.GraphQlSchemaIndividualApi;
import org.wso2.am.integration.clients.publisher.api.v1.RolesApi;
import org.wso2.am.integration.clients.publisher.api.v1.ScopesApi;
import org.wso2.am.integration.clients.publisher.api.v1.SubscriptionsApi;
import org.wso2.am.integration.clients.publisher.api.v1.ThrottlingPoliciesApi;
import org.wso2.am.integration.clients.publisher.api.v1.UnifiedSearchApi;
import org.wso2.am.integration.clients.publisher.api.v1.ValidationApi;
import org.wso2.am.integration.clients.publisher.api.v1.dto.*;
import org.wso2.am.integration.test.ClientAuthenticator;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.integration.test.utils.bean.APIRevisionDeployUndeployRequest;
import org.wso2.am.integration.test.utils.bean.APIRevisionRequest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * This util class performs the actions related to APIDTOobjects.
 */
public class RestAPIPublisherImpl {

    public static final String appName = "Integration_Test_App_Publisher";
    public static final String callBackURL = "test.com";
    public static final String tokenScope = "Production";
    public static final String appOwner = "admin";
    public static final String grantType = "password";
    public static final String username = "admin";
    public static final String password = "admin";
    public static final String testNameProperty = "testName";
    public ApIsApi apIsApi = new ApIsApi();
    public ApiDocumentsApi apiDocumentsApi = new ApiDocumentsApi();
    public ApiRevisionsApi apiRevisionsApi = new ApiRevisionsApi();
    public ApiResourcePoliciesApi apiResourcePoliciesApi = new ApiResourcePoliciesApi();
    public ApiProductRevisionsApi apiProductRevisionsApi = new ApiProductRevisionsApi();
    public ThrottlingPoliciesApi throttlingPoliciesApi = new ThrottlingPoliciesApi();
    public ClientCertificatesApi clientCertificatesApi = new ClientCertificatesApi();
    public EndpointCertificatesApi endpointCertificatesApi = new EndpointCertificatesApi();
    public GraphQlSchemaApi graphQlSchemaApi = new GraphQlSchemaApi();
    public GraphQlSchemaIndividualApi graphQlSchemaIndividualApi = new GraphQlSchemaIndividualApi();
    public ApiLifecycleApi apiLifecycleApi = new ApiLifecycleApi();
    public CommentsApi commentsApi = new CommentsApi();
    public RolesApi rolesApi = new RolesApi();
    public ValidationApi validationApi = new ValidationApi();
    public SubscriptionsApi subscriptionsApi = new SubscriptionsApi();
    public ApiAuditApi apiAuditApi = new ApiAuditApi();
    public GraphQlPoliciesApi graphQlPoliciesApi = new GraphQlPoliciesApi();
    public UnifiedSearchApi unifiedSearchApi = new UnifiedSearchApi();
    public GlobalMediationPoliciesApi globalMediationPoliciesApi = new GlobalMediationPoliciesApi();
    public ScopesApi sharedScopesApi = new ScopesApi();
    public ApiClient apiPublisherClient = new ApiClient();
    public String tenantDomain;
    private ApiProductsApi apiProductsApi = new ApiProductsApi();
    private RestAPIGatewayImpl restAPIGateway;
    private String disableVerification = System.getProperty("disableVerification");

    @Deprecated
    public RestAPIPublisherImpl() {

        this(username, password, "", "https://localhost:9943");
    }

    public RestAPIPublisherImpl(String username, String password, String tenantDomain, String publisherURL) {
        // token/DCR of Publisher node itself will be used
        String tokenURL = publisherURL + "oauth2/token";
        String dcrURL = publisherURL + "client-registration/v0.17/register";
        String accessToken = ClientAuthenticator
                .getAccessToken("openid apim:api_view apim:api_create apim:api_delete apim:api_publish " +
                                "apim:subscription_view apim:subscription_block apim:external_services_discover " +
                                "apim:threat_protection_policy_create apim:threat_protection_policy_manage " +
                                "apim:document_create apim:document_manage apim:mediation_policy_view " +
                                "apim:mediation_policy_create apim:mediation_policy_manage " +
                                "apim:client_certificates_view apim:client_certificates_add " +
                                "apim:client_certificates_update apim:ep_certificates_view " +
                                "apim:ep_certificates_add apim:ep_certificates_update apim:publisher_settings " +
                                "apim:pub_alert_manage apim:shared_scope_manage apim:api_generate_key apim:comment_view " +
                                "apim:comment_write",
                        appName, callBackURL, tokenScope, appOwner, grantType, dcrURL, username, password, tenantDomain, tokenURL);

        apiPublisherClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
        apiPublisherClient.setBasePath(publisherURL + "api/am/publisher/v2");
        apiPublisherClient.setDebugging(true);
        apiPublisherClient.setReadTimeout(600000);
        apiPublisherClient.setConnectTimeout(600000);
        apiPublisherClient.setWriteTimeout(600000);
        apIsApi.setApiClient(apiPublisherClient);
        apiProductsApi.setApiClient(apiPublisherClient);
        apiRevisionsApi.setApiClient(apiPublisherClient);
        apiResourcePoliciesApi.setApiClient(apiPublisherClient);
        apiProductRevisionsApi.setApiClient(apiPublisherClient);
        graphQlSchemaApi.setApiClient(apiPublisherClient);
        commentsApi.setApiClient(apiPublisherClient);
        graphQlSchemaIndividualApi.setApiClient(apiPublisherClient);
        apiDocumentsApi.setApiClient(apiPublisherClient);
        throttlingPoliciesApi.setApiClient(apiPublisherClient);
        apiLifecycleApi.setApiClient(apiPublisherClient);
        rolesApi.setApiClient(apiPublisherClient);
        validationApi.setApiClient(apiPublisherClient);
        clientCertificatesApi.setApiClient(apiPublisherClient);
        subscriptionsApi.setApiClient(apiPublisherClient);
        graphQlPoliciesApi.setApiClient(apiPublisherClient);
        apiAuditApi.setApiClient(apiPublisherClient);
        unifiedSearchApi.setApiClient(apiPublisherClient);
        sharedScopesApi.setApiClient(apiPublisherClient);
        globalMediationPoliciesApi.setApiClient(apiPublisherClient);
        endpointCertificatesApi.setApiClient(apiPublisherClient);
        this.tenantDomain = tenantDomain;
        this.restAPIGateway = new RestAPIGatewayImpl(this.username, this.password, tenantDomain);
    }

    /**
     * \
     * This method is used to create an API.
     *
     * @param apiRequest
     * @return HttpResponse
     * @throws ApiException throws of an error occurred when creating the API.
     */
    public HttpResponse addAPI(APIRequest apiRequest) throws ApiException {

        String osVersion = "v3";
        setActivityID();
        APIDTO apidto = this.addAPI(apiRequest, osVersion);

        HttpResponse response = null;
        if (apidto != null && StringUtils.isNotEmpty(apidto.getId())) {
            response = new HttpResponse(apidto.getId(), 201);
        }
        return response;
    }

    /**
     * \
     * This method is used to create an API.
     *
     * @param apiRequest
     * @return HttpResponse
     * @throws ApiException throws of an error occurred when creating the API.
     */
    public APIDTO addAPI(APIRequest apiRequest, String osVersion) throws ApiException {

        APIDTO body = new APIDTO();

        body.setName(apiRequest.getName());
        body.setContext(apiRequest.getContext());
        body.setVersion(apiRequest.getVersion());
        if (apiRequest.getVisibility() != null) {
            body.setVisibility(APIDTO.VisibilityEnum.valueOf(apiRequest.getVisibility().toUpperCase()));
            if (APIDTO.VisibilityEnum.RESTRICTED.getValue().equalsIgnoreCase(apiRequest.getVisibility())
                    && StringUtils.isNotEmpty(apiRequest.getRoles())) {
                List<String> roleList = new ArrayList<>(
                        Arrays.asList(apiRequest.getRoles().split(" , ")));
                body.setVisibleRoles(roleList);
            }
        } else {
            body.setVisibility(APIDTO.VisibilityEnum.PUBLIC);
        }

        if (apiRequest.getAccessControl() != null) {
            body.setAccessControl(APIDTO.AccessControlEnum.valueOf(apiRequest.getAccessControl().toUpperCase()));
            if (APIDTO.AccessControlEnum.RESTRICTED.getValue().equalsIgnoreCase(apiRequest.getAccessControl())
                    && StringUtils.isNotEmpty(apiRequest.getAccessControlRoles())) {
                List<String> roleList = new ArrayList<>(
                        Arrays.asList(apiRequest.getAccessControlRoles().split(" , ")));
                body.setAccessControlRoles(roleList);
            }
        } else {
            body.setAccessControl(APIDTO.AccessControlEnum.NONE);
        }

        body.setDescription(apiRequest.getDescription());
        body.setProvider(apiRequest.getProvider());
        ArrayList<String> transports = new ArrayList<>();
        if (Constants.PROTOCOL_HTTP.equals(apiRequest.getHttp_checked())) {
            transports.add(Constants.PROTOCOL_HTTP);
        }
        if (Constants.PROTOCOL_HTTPS.equals(apiRequest.getHttps_checked())) {
            transports.add(Constants.PROTOCOL_HTTPS);
        }
        body.setTransport(transports);
        body.isDefaultVersion(false);
        body.setCacheTimeout(100);
        if (apiRequest.getOperationsDTOS() != null) {
            body.setOperations(apiRequest.getOperationsDTOS());
        } else {
            List<APIOperationsDTO> operations = new ArrayList<>();
            APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();

            if (isAsyncApi(apiRequest)) {
                apiOperationsDTO.setVerb("SUBSCRIBE");
            } else {
                apiOperationsDTO.setVerb("GET");
            }

            if ("WEBSUB".equalsIgnoreCase(apiRequest.getType())) {
                apiOperationsDTO.setTarget("_default");
            } else {
                apiOperationsDTO.setTarget("/*");
            }
            apiOperationsDTO.setAuthType("Application & Application User");
            apiOperationsDTO.setThrottlingPolicy("Unlimited");
            operations.add(apiOperationsDTO);
            body.setOperations(operations);
        }
        body.setMediationPolicies(apiRequest.getMediationPolicies());
        body.setBusinessInformation(new APIBusinessInformationDTO());
        body.setCorsConfiguration(new APICorsConfigurationDTO());
        body.setTags(Arrays.asList(apiRequest.getTags().split(",")));
        body.setEndpointConfig(apiRequest.getEndpointConfig());
        body.setSecurityScheme(apiRequest.getSecurityScheme());
        body.setType(APIDTO.TypeEnum.fromValue(apiRequest.getType()));
        if (StringUtils.isNotBlank(apiRequest.getApiTier())) {
            body.setApiThrottlingPolicy(apiRequest.getApiTier());
        }
        List<String> tierList = new ArrayList<>();
        tierList.add(Constants.TIERS_UNLIMITED);
        body.setPolicies(Arrays.asList(apiRequest.getTiersCollection().split(",")));
        body.isDefaultVersion(Boolean.valueOf(apiRequest.getDefault_version_checked()));
        APIDTO apidto;
        try {
            ApiResponse<APIDTO> httpInfo = apIsApi.createAPIWithHttpInfo(body, osVersion);
            Assert.assertEquals(201, httpInfo.getStatusCode());
            apidto = httpInfo.getData();
        } catch (ApiException e) {
            if (e.getResponseBody().contains("already exists")) {
                return null;
            }
            throw new ApiException(e);
        }
        return apidto;
    }

    public APIDTO addAPI(APIDTO apidto, String osVersion) throws ApiException {

        ApiResponse<APIDTO> httpInfo = apIsApi.createAPIWithHttpInfo(apidto, osVersion);
        Assert.assertEquals(201, httpInfo.getStatusCode());
        return httpInfo.getData();
    }

    private boolean isAsyncApi(APIRequest apiRequest) {

        String type = apiRequest.getType();
        return "SSE".equalsIgnoreCase(type) || "WS".equalsIgnoreCase(type) || "WEBSUB".equalsIgnoreCase(type);
    }

    /**
     * This method is used to create a new API of the existing API.
     *
     * @param newVersion     new API version
     * @param apiId          old API ID
     * @param defaultVersion is this the
     * @return apiID of the newly created api version.
     * @throws ApiException Throws if an error occurs when creating the new API version.
     */
    public String createNewAPIVersion(String newVersion, String apiId, boolean defaultVersion) throws ApiException {

        String apiLocation =
                apIsApi.createNewAPIVersionWithHttpInfo(newVersion, apiId, defaultVersion, null).getHeaders()
                        .get("Location").get(0);
        String[] splitValues = apiLocation.split("/");
        return splitValues[splitValues.length - 1];
    }

    /**
     * This method is used to get the JSON content.
     *
     * @return API definition.
     * @throws IOException throws if an error occurred when creating the API.
     */
    private String getJsonContent(String fileName) throws IOException {

        if (StringUtils.isNotEmpty(fileName)) {
            return IOUtils.toString(RestAPIPublisherImpl.class.getClassLoader().getResourceAsStream(fileName),
                    StandardCharsets.UTF_8.name());
        }
        return null;
    }

    /**
     * This method is used to publish the created API.
     *
     * @param action API id that need to published.
     * @param apiId  API id that need to published.
     *               return ApiResponse<WorkflowResponseDTO> change response.
     * @throws ApiException throws if an error occurred when publishing the API.
     */
    public HttpResponse changeAPILifeCycleStatus(String apiId, String action, String lifecycleChecklist) throws ApiException {

        setActivityID();
        WorkflowResponseDTO workflowResponseDTO = this.apiLifecycleApi
                .changeAPILifecycle(action, apiId, lifecycleChecklist, null);
        HttpResponse response = null;
        if (StringUtils.isNotEmpty(workflowResponseDTO.getLifecycleState().getState())) {
            response = new HttpResponse(workflowResponseDTO.getLifecycleState().getState(), 200);
        }
        return response;
    }

    public WorkflowResponseDTO changeAPILifeCycleStatus(String apiId, String action) throws ApiException {

        ApiResponse<WorkflowResponseDTO> workflowResponseDTOApiResponse =
                this.apiLifecycleApi.changeAPILifecycleWithHttpInfo(action, apiId, null, null);
        Assert.assertEquals(HttpStatus.SC_OK, workflowResponseDTOApiResponse.getStatusCode());
        return workflowResponseDTOApiResponse.getData();
    }

    /**
     * This method is used to deprecate the created API.
     *
     * @param apiId API id that need to published.
     * @throws ApiException throws if an error occurred when publishing the API.
     */
    public void deprecateAPI(String apiId) throws ApiException {

        apiLifecycleApi.changeAPILifecycle(Constants.DEPRECATE, apiId, null, null);
    }

    /**
     * This method is used to  deploy the api as a prototype.
     *
     * @param apiId API id that need to be prototyped.
     * @throws ApiException throws if an error occurred when publishing the API.
     */
    public void deployPrototypeAPI(String apiId) throws ApiException {

        apiLifecycleApi.changeAPILifecycle(Constants.DEPLOY_AS_PROTOTYPE, apiId, null, null);
    }

    /**
     * This method is used to block the created API.
     *
     * @param apiId API id that need to published.
     * @throws ApiException throws if an error occurred when publishing the API.
     */
    public void blockAPI(String apiId) throws ApiException {

        apiLifecycleApi.changeAPILifecycle(Constants.BLOCK, apiId, null, null);
    }

    public HttpResponse getLifecycleStatus(String apiId) throws ApiException {

        LifecycleStateDTO lifecycleStateDTO = this.apiLifecycleApi.getAPILifecycleState(apiId, null);
        HttpResponse response = null;
        if (StringUtils.isNotEmpty(lifecycleStateDTO.getState())) {
            response = new HttpResponse(lifecycleStateDTO.getState(), 200);
        }
        return response;

    }

    public LifecycleStateDTO getLifecycleStatusDTO(String apiId) throws ApiException {

        ApiResponse<LifecycleStateDTO> apiResponse =
                this.apiLifecycleApi.getAPILifecycleStateWithHttpInfo(apiId, null);
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK);
        return apiResponse.getData();
    }

    public LifecycleHistoryDTO getLifecycleHistory(String apiId) throws ApiException {

        ApiResponse<LifecycleHistoryDTO> apiResponse =
                this.apiLifecycleApi.getAPILifecycleHistoryWithHttpInfo(apiId, null);
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK);
        return apiResponse.getData();
    }

    /**
     * copy API from existing API
     *
     * @param newVersion - new version of the API
     * @param apiId      - existing API Id
     * @param isDefault  - make the default version
     * @return - http response object
     * @throws APIManagerIntegrationTestException - Throws if error occurred at API copy operation
     */
    public HttpResponse copyAPI(String newVersion, String apiId, Boolean isDefault) throws ApiException {

        APIDTO apiDto = apIsApi.createNewAPIVersion(newVersion, apiId, isDefault, null);
        HttpResponse response = null;
        if (StringUtils.isNotEmpty(apiDto.getId())) {
            response = new HttpResponse(apiDto.getId(), 200);
        }
        return response;
    }

    /**
     * @param newVersion
     * @param apiId
     * @param isDefault
     * @return
     * @throws ApiException
     */
    public APIDTO copyAPIWithReturnDTO(String newVersion, String apiId, Boolean isDefault) throws ApiException {

        ApiResponse<APIDTO> response = apIsApi.createNewAPIVersionWithHttpInfo(newVersion, apiId, isDefault, null);
        Assert.assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        return response.getData();
    }

    /**
     * Facilitate update API
     *
     * @param apiRequest - constructed API request object
     * @return http response object
     * @throws APIManagerIntegrationTestException - throws if update API fails
     */
    public HttpResponse updateAPI(APIRequest apiRequest, String apiId) throws ApiException {

        APIDTO body = new APIDTO();

        body.setName(apiRequest.getName());
        body.setContext(apiRequest.getContext());
        body.setVersion(apiRequest.getVersion());
        if (apiRequest.getVisibility() != null) {
            body.setVisibility(APIDTO.VisibilityEnum.valueOf(apiRequest.getVisibility().toUpperCase()));
            if (APIDTO.VisibilityEnum.RESTRICTED.getValue().equalsIgnoreCase(apiRequest.getVisibility())
                    && StringUtils.isNotEmpty(apiRequest.getRoles())) {
                List<String> roleList = new ArrayList<>(
                        Arrays.asList(apiRequest.getRoles().split(" , ")));
                body.setVisibleRoles(roleList);
            }
        } else {
            body.setVisibility(APIDTO.VisibilityEnum.PUBLIC);
        }

        if (apiRequest.getAccessControl() != null) {
            body.setAccessControl(APIDTO.AccessControlEnum.valueOf(apiRequest.getAccessControl().toUpperCase()));
            if (APIDTO.AccessControlEnum.RESTRICTED.getValue().equalsIgnoreCase(apiRequest.getAccessControl())
                    && StringUtils.isNotEmpty(apiRequest.getAccessControlRoles())) {
                List<String> roleList = new ArrayList<>(
                        Arrays.asList(apiRequest.getAccessControlRoles().split(" , ")));
                body.setAccessControlRoles(roleList);
            }
        } else {
            body.setAccessControl(APIDTO.AccessControlEnum.NONE);
        }

        body.setDescription(apiRequest.getDescription());
        body.setProvider(apiRequest.getProvider());
        ArrayList<String> transports = new ArrayList<>();
        if (Constants.PROTOCOL_HTTP.equals(apiRequest.getHttp_checked())) {
            transports.add(Constants.PROTOCOL_HTTP);
        }
        if (Constants.PROTOCOL_HTTPS.equals(apiRequest.getHttps_checked())) {
            transports.add(Constants.PROTOCOL_HTTPS);
        }
        body.setTransport(transports);
        body.isDefaultVersion(false);
        body.setCacheTimeout(100);
        body.setMediationPolicies(apiRequest.getMediationPolicies());
        if (apiRequest.getOperationsDTOS() != null) {
            body.setOperations(apiRequest.getOperationsDTOS());
        } else {
            List<APIOperationsDTO> operations = new ArrayList<>();
            APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
            apiOperationsDTO.setVerb("GET");
            apiOperationsDTO.setTarget("/*");
            apiOperationsDTO.setAuthType("Application & Application User");
            apiOperationsDTO.setThrottlingPolicy("Unlimited");
            operations.add(apiOperationsDTO);
            body.setOperations(operations);
        }

        body.setBusinessInformation(new APIBusinessInformationDTO());
        body.setCorsConfiguration(new APICorsConfigurationDTO());
        body.setTags(Arrays.asList(apiRequest.getTags().split(",")));
        body.setEndpointConfig(apiRequest.getEndpointConfig());
        if (StringUtils.isNotBlank(apiRequest.getApiTier())) {
            body.setApiThrottlingPolicy(apiRequest.getApiTier());
        }
        List<String> tierList = new ArrayList<>();
        tierList.add(Constants.TIERS_UNLIMITED);
        body.setPolicies(Arrays.asList(apiRequest.getTiersCollection().split(",")));
        body.setCategories(apiRequest.getApiCategories());
        APIDTO apidto;
        try {
            apidto = apIsApi.updateAPI(apiId, body, null);
        } catch (ApiException e) {
            if (e.getResponseBody().contains("already exists")) {
                return null;
            }
            throw new ApiException(e);
        }
        HttpResponse response = null;
        if (StringUtils.isNotEmpty(apidto.getId())) {
            response = new HttpResponse(apidto.getId(), 200);
        }

        return response;
    }

    public APIDTO updateAPI(APIDTO apidto, String apiId) throws ApiException {

        ApiResponse<APIDTO> apiDtoApiResponse = apIsApi.updateAPIWithHttpInfo(apiId, apidto, null);
        Assert.assertEquals(HttpStatus.SC_OK, apiDtoApiResponse.getStatusCode());
        return apiDtoApiResponse.getData();
    }

    /**
     * Method to get API information
     *
     * @param apiId - API id
     * @return http response object
     * @throws ApiException - Throws if api information cannot be retrieved.
     */
    public HttpResponse getAPI(String apiId) throws ApiException {

        setActivityID();
        APIDTO apidto = null;
        HttpResponse response = null;
        Gson gson = new Gson();
        try {
            apidto = apIsApi.getAPI(apiId, null, null);
        } catch (ApiException e) {
            return new HttpResponse(gson.toJson(e.getResponseBody()), e.getCode());
        }
        if (StringUtils.isNotEmpty(apidto.getId())) {
            response = new HttpResponse(gson.toJson(apidto), 200);
        }
        return response;
    }

    /**
     * delete API
     *
     * @param apiId - API id
     * @return http response object
     * @throws ApiException - Throws if API delete fails
     */
    public HttpResponse deleteAPI(String apiId) throws ApiException {

        ApiResponse<Void> deleteResponse = apIsApi.deleteAPIWithHttpInfo(apiId, null);
        HttpResponse response = null;
        if (deleteResponse.getStatusCode() == 200) {
            response = new HttpResponse("Successfully deleted the API", 200);
        }
        return response;
    }

    public HttpResponse generateMockScript(String apiId) throws ApiException {
        ApiResponse<String> mockResponse = apIsApi.generateMockScriptsWithHttpInfo(apiId, null);
        HttpResponse response = null;
        if (mockResponse.getStatusCode() == 200) {
            response = new HttpResponse("Successfully generated MockScript", 200);
        }
        return response;
    }

    public HttpResponse getGenerateMockScript(String apiId) throws ApiException {
        ApiResponse<MockResponsePayloadListDTO> mockResponse = apIsApi.getGeneratedMockScriptsOfAPIWithHttpInfo(apiId, null);
        HttpResponse response = null;
        if (mockResponse.getStatusCode() == 200) {
            response = new HttpResponse(mockResponse.getData().toString(), 200);
        }
        return response;
    }

    /**
     * Remove document
     *
     * @param apiId - API id
     * @param docId - document id
     * @return http response object
     * @throws ApiException - Throws if remove API document fails
     */
    public HttpResponse removeDocumentation(String apiId, String docId) throws ApiException {

        ApiResponse<Void> deleteResponse = apiDocumentsApi.deleteAPIDocumentWithHttpInfo(apiId, docId, null);
        HttpResponse response = null;
        if (deleteResponse.getStatusCode() == 200) {
            response = new HttpResponse("Successfully removed the documentation", 200);
        }
        return response;
    }

    /**
     * revoke access token
     *
     * @param accessToken - access token already  received
     * @param consumerKey -  consumer key returned
     * @param authUser    - user name
     * @return http response object
     * @throws APIManagerIntegrationTestException - throws if access token revoke fails
     */
    public HttpResponse revokeAccessToken(String accessToken, String consumerKey, String authUser)
            throws APIManagerIntegrationTestException {

        return null;
    }

    /**
     * update permissions to API access
     *
     * @param tierName       - name of api throttling tier
     * @param permissionType - permission type
     * @param roles          - roles of permission
     * @return http response object
     * @throws APIManagerIntegrationTestException - throws if permission update fails
     */
    public HttpResponse updatePermissions(String tierName, String permissionType, String roles)
            throws APIManagerIntegrationTestException {

        return null;
    }

    /**
     * Update resources of API
     *
     * @param apiId - API Id
     * @return http response object
     * @throws APIManagerIntegrationTestException - throws if API resource update fails
     */
    public HttpResponse updateResourceOfAPI(String apiId, String api)
            throws APIManagerIntegrationTestException {

        return null;
    }

    /**
     * Check whether the Endpoint is valid
     *
     * @param endpointUrl url of the endpoint
     * @param apiId       id of the api which the endpoint to be validated
     * @return HttpResponse -  Response of the getAPI request
     * @throws ApiException - Check for valid endpoint fails.
     */
    public HttpResponse checkValidEndpoint(String endpointUrl, String apiId) throws ApiException {

        ApiEndpointValidationResponseDTO validationResponseDTO = validationApi.validateEndpoint(endpointUrl, apiId);
        HttpResponse response = null;
        if (validationResponseDTO.getStatusCode() == 200) {
            response = new HttpResponse(validationResponseDTO.getStatusMessage(), 200);
        }
        return response;
    }

    /**
     * Change the API Lifecycle status to Publish with the option of Re-subscription is required or not
     *
     * @param apiId                   - API ID
     * @param isRequireReSubscription - true if Re-subscription is required else false.
     * @return HttpResponse - Response of the API publish event
     * @throws APIManagerIntegrationTestException - Exception Throws in checkAuthentication() and when do the REST
     *                                            service calls to do the lifecycle change.
     */
    public HttpResponse changeAPILifeCycleStatusToPublish(String apiId, boolean isRequireReSubscription) throws ApiException {

        ApiResponse<WorkflowResponseDTO> responseDTOApiResponse = this.apiLifecycleApi.changeAPILifecycleWithHttpInfo(
                Constants.PUBLISHED, apiId, "Re-Subscription:" + isRequireReSubscription, null);
        HttpResponse response = null;
        if (responseDTOApiResponse.getStatusCode() == 200) {
            response = new HttpResponse("Successfully changed the lifecycle of the API", 200);
        }
        return response;
    }

    /**
     * Retrieve the Tier Permission Page
     *
     * @return HttpResponse - Response that contains the Tier Permission Page
     * @throws APIManagerIntegrationTestException - Exception throws from checkAuthentication() method and
     *                                            HTTPSClientUtils.doGet() method call
     */
    public HttpResponse getTierPermissionsPage() throws APIManagerIntegrationTestException {

        return null;
    }

    /**
     * Adding a documentation
     *
     * @param apiId - Id of the API.
     * @param body  - document Body.
     * @return HttpResponse - Response  with Document adding result.
     * @throws ApiException - Exception throws if error occurred when adding document.
     */
    public HttpResponse addDocument(String apiId, DocumentDTO body) throws ApiException {

        DocumentDTO doc = apiDocumentsApi.addAPIDocument(apiId, body, null);
        HttpResponse response = null;
        if (StringUtils.isNotEmpty(doc.getDocumentId())) {
            response = new HttpResponse(doc.getDocumentId(), 200);
        }
        return response;
    }

    /**
     * Adding a content to the document
     *
     * @param apiId      - Id of the API.
     * @param docId      - document Id.
     * @param docContent - document content
     * @return HttpResponse - Response  with Document adding result.
     * @throws ApiException - Exception throws if error occurred when adding document.
     */
    public HttpResponse addContentDocument(String apiId, String docId, String docContent) throws ApiException {

        DocumentDTO doc = apiDocumentsApi.addAPIDocumentContent(apiId, docId, null, null,
                docContent);
        HttpResponse response = null;
        if (StringUtils.isNotEmpty(doc.getDocumentId())) {
            response = new HttpResponse("Successfully created the documentation", 200);
        }
        return response;
    }

    /**
     * Updating the document content using file
     *
     * @param apiId      - Id of the API.
     * @param docId      - document Id.
     * @param docContent - file content
     * @return HttpResponse - Response  with Document adding result.
     * @throws ApiException - Exception throws if error occurred when adding document.
     */
    public HttpResponse updateContentDocument(String apiId, String docId, File docContent) throws ApiException {

        DocumentDTO doc = apiDocumentsApi.addAPIDocumentContent(apiId, docId, null, docContent,
                null);
        HttpResponse response = null;
        if (StringUtils.isNotEmpty(doc.getDocumentId())) {
            response = new HttpResponse("Successfully updated the documentation", 200);
        }
        return response;
    }

    /**
     * Update API document.
     *
     * @param apiId       api Id
     * @param docId       document Id
     * @param documentDTO documentation object.
     * @return
     * @throws ApiException Exception throws if error occurred when updating document.
     */
    public HttpResponse updateDocument(String apiId, String docId, DocumentDTO documentDTO) throws ApiException {

        DocumentDTO doc = apiDocumentsApi.updateAPIDocument(apiId, docId, documentDTO, null);
        HttpResponse response = null;
        if (StringUtils.isNotEmpty(doc.getDocumentId())) {
            response = new HttpResponse("Successfully created the documentation", 200);
        }
        return response;
    }

    /**
     * This method is used to get documents
     * Get Documents for the given limit and offset values
     *
     * @param apiId apiId
     * @return Documents for the given limit and offset values
     */
    public DocumentListDTO getDocuments(String apiId) throws ApiException {

        ApiResponse<DocumentListDTO> apiResponse = apiDocumentsApi.getAPIDocumentsWithHttpInfo(apiId,
                null, null, null);
        Assert.assertEquals(HttpStatus.SC_OK, apiResponse.getStatusCode());
        return apiResponse.getData();
    }

    /**
     * This method is used to get the content of API documents
     *
     * @param apiId      UUID of the API
     * @param documentId UUID of the API document
     * @return
     * @throws ApiException
     */
    public HttpResponse getDocumentContent(String apiId, String documentId) throws ApiException {

        ApiResponse<String> apiResponse = apiDocumentsApi.getAPIDocumentContentByDocumentIdWithHttpInfo(apiId,
                documentId, null);
        HttpResponse response = null;
        if (apiResponse.getStatusCode() == 200) {
            response = new HttpResponse("Successfully retrieved the Document content", 200);
        }
        return response;
    }

    /**
     * delete Document
     *
     * @param apiId      - API id
     * @param documentId - API id
     * @return http response object
     * @throws ApiException - Throws if API delete fails
     */
    public HttpResponse deleteDocument(String apiId, String documentId) throws ApiException {

        ApiResponse<Void> deleteResponse = apiDocumentsApi.deleteAPIDocumentWithHttpInfo(apiId, documentId, null);
        HttpResponse response = null;
        if (deleteResponse.getStatusCode() == 200) {
            response = new HttpResponse("Successfully deleted the Document", 200);
        }
        return response;
    }

    /***
     * Add a shared scope
     *
     * @param scopeDTO
     * @return ScopeDTO - Returns the added shared scope
     * @throws ApiException
     */
    public ScopeDTO addSharedScope(ScopeDTO scopeDTO) throws ApiException {

        ApiResponse<ScopeDTO> httpInfo = sharedScopesApi.addSharedScopeWithHttpInfo(scopeDTO);
        Assert.assertEquals(httpInfo.getStatusCode(), HttpStatus.SC_CREATED);
        return httpInfo.getData();
    }

    /***
     * Remove a shared scope
     *
     * @param id id of the scope to delete
     * @throws ApiException
     */
    public void removeSharedScope(String id) throws ApiException {
        ApiResponse<Void> httpInfo = sharedScopesApi.deleteSharedScopeWithHttpInfo(id);
        Assert.assertEquals(httpInfo.getStatusCode(), HttpStatus.SC_OK);
    }

    /***
     * Update a shared scopes
     *
     * @param uuid
     * @param scopeDTO
     * @return ScopeDTO - Returns the updated shared scope
     * @throws ApiException
     */
    public ScopeDTO updateSharedScope(String uuid, ScopeDTO scopeDTO) throws ApiException {

        ApiResponse<ScopeDTO> httpInfo = sharedScopesApi.updateSharedScopeWithHttpInfo(uuid, scopeDTO);
        Assert.assertEquals(httpInfo.getStatusCode(), HttpStatus.SC_OK);
        return httpInfo.getData();
    }

    /***
     * Get a shared scope
     *
     * @param uuid
     * @return ScopeDTO - Returns the updated shared scope
     * @throws ApiException
     */
    public ScopeDTO getSharedScopeById(String uuid) throws ApiException {

        ApiResponse<ScopeDTO> httpInfo = sharedScopesApi.getSharedScopeWithHttpInfo(uuid);
        Assert.assertEquals(httpInfo.getStatusCode(), HttpStatus.SC_OK);
        return httpInfo.getData();
    }

    /***
     * Delete a shared scope
     *
     * @param uuid
     * @throws ApiException
     */
    public void deleteSharedScope(String uuid) throws ApiException {

        ApiResponse<Void> httpInfo = sharedScopesApi.deleteSharedScopeWithHttpInfo(uuid);
        Assert.assertEquals(httpInfo.getStatusCode(), HttpStatus.SC_OK);
    }

    /***
     * Get all shared scopes
     *
     * @return ScopeListDTO - Returns all the shared scopes
     * @throws ApiException
     */
    public ScopeListDTO getAllSharedScopes() throws ApiException {

        ApiResponse<ScopeListDTO> httpInfo = sharedScopesApi.getSharedScopesWithHttpInfo(null, null);
        Assert.assertEquals(httpInfo.getStatusCode(), HttpStatus.SC_OK);
        return httpInfo.getData();
    }

    /**
     * Retrieve the All APIs available for the user in Publisher.
     *
     * @return HttpResponse - Response that contains all available APIs for the user
     * @throws APIManagerIntegrationTestException - Exception throws from checkAuthentication() method and
     *                                            HTTPSClientUtils.doGet() method call
     */
    public APIListDTO getAllAPIs() throws APIManagerIntegrationTestException, ApiException {

        APIListDTO apis = apIsApi.getAllAPIs(null, null, null, null, null, null, null);
        if (apis.getCount() > 0) {
            return apis;
        }
        return null;
    }

    /**
     * Retrieve the APIs according to the search query in Publisher.
     *
     * @param query - The query on which the APIs needs to be filtered
     * @return SearchResultListDTO - The search results of the query
     * @throws ApiException
     */
    public SearchResultListDTO searchAPIs(String query) throws ApiException {

        ApiResponse<SearchResultListDTO> searchResponse = unifiedSearchApi.searchWithHttpInfo(null, null, query, null);
        Assert.assertEquals(HttpStatus.SC_OK, searchResponse.getStatusCode());
        return searchResponse.getData();
    }

    /**
     * This method is used to upload endpoint certificates
     * Get APIs for the given limit and offset values
     *
     * @param offset starting position
     * @param limit  maximum number of APIs to return
     * @return APIs for the given limit and offset values
     */
    public APIListDTO getAPIs(int offset, int limit) throws ApiException {

        setActivityID();
        ApiResponse<APIListDTO> apiResponse = apIsApi.getAllAPIsWithHttpInfo(limit, offset, this.tenantDomain, null,
                null, false, null);
        Assert.assertEquals(HttpStatus.SC_OK, apiResponse.getStatusCode());
        return apiResponse.getData();
    }

    /**
     * This method is used to upload certificates
     *
     * @param certificate certificate
     * @param alias       alis
     * @param endpoint    endpoint.
     * @return
     * @throws ApiException if an error occurred while uploading the certificate.
     */
    public ApiResponse<CertMetadataDTO> uploadEndpointCertificate(File certificate, String alias, String endpoint) throws ApiException {

        ApiResponse<CertMetadataDTO> certificateDTO =
                endpointCertificatesApi.addEndpointCertificateWithHttpInfo(certificate, alias, endpoint);
        return certificateDTO;

    }

    /**
     * This method is used to get all throttling tiers.
     *
     * @param - API or resource
     * @return - Response that contains all the available tiers
     * @throws ApiException
     */
    public ThrottlingPolicyListDTO getTiers(String policyLevel) throws ApiException {

        ThrottlingPolicyListDTO policies
                = throttlingPoliciesApi.getAllThrottlingPolicies(policyLevel, null, null, null);
        if (policies.getCount() > 0) {
            return policies;
        }
        return null;

    }

    public SubscriptionPolicyListDTO getSubscriptionPolicies(String tierQuotaTypes) throws ApiException {

        SubscriptionPolicyListDTO subscriptionPolicyList
                = throttlingPoliciesApi.getSubscriptionThrottlingPolicies(null, null, null);
        if (subscriptionPolicyList.getCount() > 0) {
            return subscriptionPolicyList;
        }
        return null;
    }

    /**
     * This method is used to validate roles.
     *
     * @param roleId role Id
     * @return HttpResponse
     * @throws APIManagerIntegrationTestException
     */
    public ApiResponse<Void> validateRoles(String roleId) throws ApiException {

        String encodedRoleName = Base64.getUrlEncoder().encodeToString(roleId.getBytes());
        return rolesApi.validateSystemRoleWithHttpInfo(encodedRoleName);
    }

    public String getSwaggerByID(String apiId) throws ApiException {

        ApiResponse<String> response = apIsApi.getAPISwaggerWithHttpInfo(apiId, null);
        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        return response.getData();
    }

    public String updateSwagger(String apiId, String definition) throws ApiException {

        ApiResponse<String> apiResponse = apIsApi.updateAPISwaggerWithHttpInfo(apiId, null, definition, null, null);
        Assert.assertEquals(HttpStatus.SC_OK, apiResponse.getStatusCode());
        return apiResponse.getData();
    }

    public OpenAPIDefinitionValidationResponseDTO validateOASDefinition(File oasDefinition) throws ApiException {

        ApiResponse<OpenAPIDefinitionValidationResponseDTO> response =
                validationApi.validateOpenAPIDefinitionWithHttpInfo(null, null, oasDefinition, null);
        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        return response.getData();
    }

    public APIDTO getAPIByID(String apiId, String tenantDomain) throws ApiException {

        ApiResponse<APIDTO> apiDtoApiResponse = apIsApi.getAPIWithHttpInfo(apiId, tenantDomain, null);
        Assert.assertEquals(HttpStatus.SC_OK, apiDtoApiResponse.getStatusCode());
        return apiDtoApiResponse.getData();
    }

    public APIDTO getAPIByID(String apiId) throws ApiException {

        return apIsApi.getAPI(apiId, tenantDomain, null);
    }

    public APIDTO importOASDefinition(File file, String properties) throws ApiException {

        ApiResponse<APIDTO> apiDtoApiResponse = apIsApi.importOpenAPIDefinitionWithHttpInfo(file, null, properties,
                null);
        Assert.assertEquals(HttpStatus.SC_CREATED, apiDtoApiResponse.getStatusCode());
        return apiDtoApiResponse.getData();
    }

    public GraphQLValidationResponseDTO validateGraphqlSchemaDefinition(File schemaDefinition) throws ApiException {

        ApiResponse<GraphQLValidationResponseDTO> response =
                validationApi.validateGraphQLSchemaWithHttpInfo(schemaDefinition);
        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        return response.getData();
    }

    public APIDTO importGraphqlSchemaDefinition(File file, String properties) throws ApiException {

        ApiResponse<APIDTO> apiDtoApiResponse = apIsApi.importGraphQLSchemaWithHttpInfo(null, "GRAPHQL",
                file, properties);
        Assert.assertEquals(HttpStatus.SC_CREATED, apiDtoApiResponse.getStatusCode());
        return apiDtoApiResponse.getData();
    }

    public GraphQLSchemaDTO getGraphqlSchemaDefinition(String apiId) throws ApiException {

        ApiResponse<GraphQLSchemaDTO> schemaDefinitionDTO = graphQlSchemaIndividualApi.
                getAPIGraphQLSchemaWithHttpInfo(apiId, "application/json", null);
        Assert.assertEquals(HttpStatus.SC_OK, schemaDefinitionDTO.getStatusCode());
        return schemaDefinitionDTO.getData();
    }

    public void updateGraphqlSchemaDefinition(String apiId, String schemaDefinition) throws ApiException {

        ApiResponse<Void> schemaDefinitionDTO = graphQlSchemaApi.updateAPIGraphQLSchemaWithHttpInfo
                (apiId, schemaDefinition, null);
        Assert.assertEquals(HttpStatus.SC_OK, schemaDefinitionDTO.getStatusCode());
    }

    public WSDLValidationResponseDTO validateWsdlDefinition(String url, File wsdlDefinition) throws ApiException {
        ApiResponse<WSDLValidationResponseDTO> response = validationApi
                .validateWSDLDefinitionWithHttpInfo(url, wsdlDefinition);
        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        return response.getData();
    }

    public APIDTO importWSDLSchemaDefinition(File file, String url, String properties, String type)
            throws ApiException {
        ApiResponse<APIDTO> apiDtoApiResponse = apIsApi.importWSDLDefinitionWithHttpInfo(file, url, properties, type);
        Assert.assertEquals(HttpStatus.SC_CREATED, apiDtoApiResponse.getStatusCode());
        return apiDtoApiResponse.getData();
    }

    public ApiResponse<Void> getWSDLSchemaDefinitionOfAPI(String apiId) throws ApiException {
        ApiResponse<Void> apiDtoApiResponse = apIsApi.getWSDLOfAPIWithHttpInfo(apiId,null);
        Assert.assertEquals(HttpStatus.SC_OK, apiDtoApiResponse.getStatusCode());
        return apiDtoApiResponse;
    }

    public ResourcePolicyListDTO getApiResourcePolicies(String apiId, String sequenceType, String resourcePath,
            String verb) throws ApiException {
        ApiResponse<ResourcePolicyListDTO> policyListDTOApiResponse = apiResourcePoliciesApi
                .getAPIResourcePoliciesWithHttpInfo(apiId, sequenceType, resourcePath, verb, null);
        Assert.assertEquals(policyListDTOApiResponse.getStatusCode(), HttpStatus.SC_OK);
        return policyListDTOApiResponse.getData();
    }

    public ResourcePolicyInfoDTO updateApiResourcePolicies(String apiId, String resourcePolicyId, String resourcePath,
            ResourcePolicyInfoDTO resourcePolicyInfoDTO, String verb) throws ApiException {

        ApiResponse<ResourcePolicyInfoDTO> response = apiResourcePoliciesApi
                .updateAPIResourcePoliciesByPolicyIdWithHttpInfo(apiId, resourcePolicyId, resourcePolicyInfoDTO, null);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
        return response.getData();
    }


    public AsyncAPISpecificationValidationResponseDTO validateAsyncAPISchemaDefinition(String url, File file)
            throws ApiException {
        ApiResponse<AsyncAPISpecificationValidationResponseDTO> response = validationApi
                .validateAsyncAPISpecificationWithHttpInfo(false, url, file);
        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        return response.getData();
    }

    public APIDTO importAsyncAPISchemaDefinition(File file, String url, String properties)
            throws ApiException {
        ApiResponse<APIDTO> apiDtoApiResponse = apIsApi.importAsyncAPISpecificationWithHttpInfo(file, url, properties);
        Assert.assertEquals(HttpStatus.SC_CREATED, apiDtoApiResponse.getStatusCode());
        return apiDtoApiResponse.getData();
    }

    public ApiResponse<Void> getAsyncAPIDefinitionOfAPI(String apiId) throws ApiException {
        ApiResponse<Void> apiDtoApiResponse = apIsApi.getWSDLOfAPIWithHttpInfo(apiId,null);
        Assert.assertEquals(HttpStatus.SC_OK, apiDtoApiResponse.getStatusCode());
        return apiDtoApiResponse;
    }

    public APIDTO addAPI(APICreationRequestBean apiCreationRequestBean) throws ApiException {

        APIDTO body = new APIDTO();

        body.setName(apiCreationRequestBean.getName());
        body.setContext(apiCreationRequestBean.getContext());
        body.setVersion(apiCreationRequestBean.getVersion());
        if (apiCreationRequestBean.getVisibility() != null) {
            body.setVisibility(APIDTO.VisibilityEnum.valueOf(apiCreationRequestBean.getVisibility().toUpperCase()));
            if (APIDTO.VisibilityEnum.RESTRICTED.getValue().equalsIgnoreCase(apiCreationRequestBean.getVisibility())
                    && StringUtils.isNotEmpty(apiCreationRequestBean.getRoles())) {
                List<String> roleList = new ArrayList<>(
                        Arrays.asList(apiCreationRequestBean.getRoles().split(" , ")));
                body.setVisibleRoles(roleList);
            }
        } else {
            body.setVisibility(APIDTO.VisibilityEnum.PUBLIC);
        }
        body.setDescription(apiCreationRequestBean.getDescription());
        body.setProvider(apiCreationRequestBean.getProvider());
        body.setTransport(new ArrayList<String>() {{
            add(Constants.PROTOCOL_HTTP);
            add(Constants.PROTOCOL_HTTPS);
        }});
        body.isDefaultVersion(false);
        body.setCacheTimeout(100);
        List<APIOperationsDTO> operations = new ArrayList<>();
        for (APIResourceBean resourceBean : apiCreationRequestBean.getResourceBeanList()) {
            APIOperationsDTO dto = new APIOperationsDTO();
            dto.setTarget(resourceBean.getUriTemplate());
            dto.setAuthType(resourceBean.getResourceMethodAuthType());
            dto.setVerb(resourceBean.getResourceMethod());
            dto.setThrottlingPolicy(resourceBean.getResourceMethodThrottlingTier());
            operations.add(dto);
        }
        body.setOperations(operations);
        body.setBusinessInformation(new APIBusinessInformationDTO());
        body.setCorsConfiguration(new APICorsConfigurationDTO());
        body.setTags(Arrays.asList(apiCreationRequestBean.getTags().split(",")));

        if (apiCreationRequestBean.getSetEndpointSecurityDirectlyToEndpoint()) {
            try {
                body.setEndpointConfig(new JSONParser().parse(apiCreationRequestBean.getEndpoint().toString()));
            } catch (ParseException e) {
                throw new ApiException(e);
            }
        } else {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("endpoint_type", "http");
            JSONObject sandUrl = new JSONObject();
            sandUrl.put("url", apiCreationRequestBean.getEndpointUrl().toString());
            jsonObject.put("sandbox_endpoints", sandUrl);
            jsonObject.put("production_endpoints", sandUrl);
            if ("basic".equalsIgnoreCase(apiCreationRequestBean.getEndpointType())) {
                JSONObject endpointSecurityGlobal = new JSONObject();
                endpointSecurityGlobal.put("enabled", true);
                endpointSecurityGlobal.put("type", "basic");
                endpointSecurityGlobal.put("username", apiCreationRequestBean.getEpUsername());
                endpointSecurityGlobal.put("password", apiCreationRequestBean.getEpPassword());
                JSONObject endpointSecurity = new JSONObject();
                endpointSecurity.put("production", endpointSecurityGlobal);
                endpointSecurity.put("sandbox", endpointSecurityGlobal);
                jsonObject.put("endpoint_security", endpointSecurity);
            }
            body.setEndpointConfig(jsonObject);
        }

        List<String> tierList = new ArrayList<>();
        tierList.add(Constants.TIERS_UNLIMITED);
        if (apiCreationRequestBean.getSubPolicyCollection() != null) {
            String[] tiers = apiCreationRequestBean.getSubPolicyCollection().split(",");
            for (String tier : tiers) {
                tierList.add(tier);
            }
        }
        body.setPolicies(tierList);
        ApiResponse<APIDTO> httpInfo = apIsApi.createAPIWithHttpInfo(body, "v3");
        Assert.assertEquals(201, httpInfo.getStatusCode());
        return httpInfo.getData();
    }

    /**
     * This method is used to upload certificates
     *
     * @param certificate certificate
     * @param alias       alis
     * @return
     * @throws ApiException if an error occurred while uploading the certificate.
     */
    public HttpResponse uploadCertificate(File certificate, String alias, String apiId, String tier)
            throws ApiException {

        ClientCertMetadataDTO certificateDTO = clientCertificatesApi.addAPIClientCertificate(apiId, certificate,
                alias, tier);
        HttpResponse response = null;
        if (StringUtils.isNotEmpty(certificateDTO.getAlias())) {
            response = new HttpResponse("Successfully uploaded the certificate", 200);
        }
        return response;

    }

    /**
     * Update an API
     *
     * @param apidto
     * @return
     * @throws ApiException
     */
    public APIDTO updateAPI(APIDTO apidto) throws ApiException {

        ApiResponse<APIDTO> response = apIsApi.updateAPIWithHttpInfo(apidto.getId(), apidto, null);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
        return response.getData();
    }

    /**
     * Get subscription of an API
     *
     * @param apiID
     * @return
     * @throws ApiException
     */
    public SubscriptionListDTO getSubscriptionByAPIID(String apiID) throws ApiException {

        ApiResponse<SubscriptionListDTO> apiResponse =
                subscriptionsApi.getSubscriptionsWithHttpInfo(apiID, 10, 0, null, null);
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK);
        return apiResponse.getData();
    }

    public APIProductListDTO getAllApiProducts() throws ApiException {

        ApiResponse<APIProductListDTO> apiResponse =
                apiProductsApi.getAllAPIProductsWithHttpInfo(null, null, null, null, null);
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK);
        return apiResponse.getData();
    }

    public APIProductDTO getApiProduct(String apiProductId) throws ApiException {

        ApiResponse<APIProductDTO> apiResponse =
                apiProductsApi.getAPIProductWithHttpInfo(apiProductId, null, null);
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK);
        return apiResponse.getData();
    }

    public APIProductDTO addApiProduct(APIProductDTO apiProductDTO) throws ApiException {

        ApiResponse<APIProductDTO> apiResponse = apiProductsApi.createAPIProductWithHttpInfo(apiProductDTO);
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_CREATED);
        return apiResponse.getData();
    }

    public void deleteApiProduct(String apiProductId) throws ApiException {

        ApiResponse<Void> apiResponse = apiProductsApi.deleteAPIProductWithHttpInfo(apiProductId, null);
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK);
    }

    /**
     * Method to retrieve the Audit Report of an API
     *
     * @param apiId apiId of the API
     * @return HttpResponse response
     * @throws ApiException
     */
    public HttpResponse getAuditApi(String apiId) throws ApiException {

        HttpResponse response = null;
        ApiResponse<AuditReportDTO> auditReportResponse = apiAuditApi
                .getAuditReportOfAPIWithHttpInfo(apiId, "application/json");
        if (auditReportResponse.getStatusCode() == 200) {
            response = new HttpResponse("Successfully audited the report", 200);
        }
        return response;
    }

    /**
     * Method to retrieve the GraphQL Schema Type List
     *
     * @param apiId apiId of the API
     * @return HttpResponse response
     * @throws ApiException
     */
    public HttpResponse getGraphQLSchemaTypeListResponse(String apiId) throws ApiException {

        HttpResponse response = null;
        ApiResponse<GraphQLSchemaTypeListDTO> graphQLSchemaTypeListDTOApiResponse = graphQlPoliciesApi
                .getGraphQLPolicyComplexityTypesOfAPIWithHttpInfo(apiId);
        if (graphQLSchemaTypeListDTOApiResponse.getStatusCode() == 200) {
            response = new HttpResponse("Successfully get the GraphQL Schema Type List", 200);
        }
        return response;
    }

    /**
     * Method to retrieve the GraphQL Schema Type List
     *
     * @param apiId apiId of the API
     * @return GraphQLSchemaTypeListDTO GraphQLSchemaTypeList object
     * @throws ApiException
     */
    public GraphQLSchemaTypeListDTO getGraphQLSchemaTypeList(String apiId) throws ApiException {

        ApiResponse<GraphQLSchemaTypeListDTO> graphQLSchemaTypeListDTOApiResponse = graphQlPoliciesApi
                .getGraphQLPolicyComplexityTypesOfAPIWithHttpInfo(apiId);
        Assert.assertEquals(graphQLSchemaTypeListDTOApiResponse.getStatusCode(), HttpStatus.SC_OK);
        return graphQLSchemaTypeListDTOApiResponse.getData();
    }

    /**
     * Method to add GraphQL Complexity Info of an API
     *
     * @param apiID
     * @param graphQLQueryComplexityInfoDTO GraphQL Complexity Object
     * @return
     * @throws ApiException
     */
    public void addGraphQLComplexityDetails(GraphQLQueryComplexityInfoDTO graphQLQueryComplexityInfoDTO, String apiID)
            throws ApiException {

        ApiResponse<Void> apiResponse = graphQlPoliciesApi.updateGraphQLPolicyComplexityOfAPIWithHttpInfo(apiID,
                graphQLQueryComplexityInfoDTO);
        Assert.assertEquals(apiResponse.getStatusCode(), HttpStatus.SC_OK);
    }

    /**
     * Method to retrieve the GraphQL Complexity Details
     *
     * @param apiId apiId of the API
     * @return HttpResponse response
     * @throws ApiException
     */
    public HttpResponse getGraphQLComplexityResponse(String apiId) throws ApiException {

        HttpResponse response = null;
        ApiResponse<GraphQLQueryComplexityInfoDTO> complexityResponse = graphQlPoliciesApi
                .getGraphQLPolicyComplexityOfAPIWithHttpInfo(apiId);
        if (complexityResponse.getStatusCode() == 200) {
            response = new HttpResponse("Successfully get the GraphQL Complexity Details", 200);
        }
        return response;
    }

    /**
     * This method is used to create an API Revision.
     *
     * @param apiRevisionRequest API Revision create object body
     * @return HttpResponse
     * @throws ApiException throws of an error occurred when creating the API Revision.
     */
    public HttpResponse addAPIRevision(APIRevisionRequest apiRevisionRequest) throws ApiException {

        setActivityID();
        APIRevisionDTO apiRevisionDTO = new APIRevisionDTO();
        apiRevisionDTO.setDescription(apiRevisionRequest.getDescription());
        Gson gson = new Gson();
        try {
            ApiResponse<APIRevisionDTO> httpInfo = apiRevisionsApi.
                    createAPIRevisionWithHttpInfo(apiRevisionRequest.getApiUUID(), apiRevisionDTO);
            Assert.assertEquals(201, httpInfo.getStatusCode());
            apiRevisionDTO = httpInfo.getData();
        } catch (ApiException e) {
            if (e.getResponseBody().contains("already exists")) {
                return null;
            } else if (e.getCode() != 0) {
                return new HttpResponse(null, e.getCode());
            }
            throw new ApiException(e);
        }
        HttpResponse response = null;
        if (apiRevisionDTO != null && StringUtils.isNotEmpty(apiRevisionDTO.getId())) {
            response = new HttpResponse(gson.toJson(apiRevisionDTO), 201);
        }
        return response;
    }

    /**
     * This method is used to create an API Revision.
     *
     * @param apiId API Revision create object body
     * @return HttpResponse
     * @throws ApiException throws of an error occurred when creating the API Revision.
     */
    public APIRevisionDTO addAPIRevision(String apiId) throws ApiException {

        APIRevisionDTO apiRevisionDTO = new APIRevisionDTO();
        Gson gson = new Gson();
        try {
            ApiResponse<APIRevisionDTO> httpInfo = apiRevisionsApi.
                    createAPIRevisionWithHttpInfo(apiId, apiRevisionDTO);
            Assert.assertEquals(201, httpInfo.getStatusCode());
            return httpInfo.getData();
        } catch (ApiException e) {
            throw new ApiException(e);
        }
    }

    /**
     * Method to get API Revisions per API
     *
     * @param apiUUID - API uuid
     * @param query   - Search query
     * @return http response object
     * @throws ApiException - Throws if api information cannot be retrieved.
     */
    public HttpResponse getAPIRevisions(String apiUUID, String query) throws ApiException {

        APIRevisionListDTO apiRevisionListDTO = null;
        HttpResponse response = null;
        Gson gson = new Gson();
        try {
            apiRevisionListDTO = apiRevisionsApi.getAPIRevisions(apiUUID, query);
        } catch (ApiException e) {
            return new HttpResponse(gson.toJson(e.getResponseBody()), e.getCode());
        }
        if (StringUtils.isNotEmpty(apiRevisionListDTO.getList().toString())) {
            response = new HttpResponse(gson.toJson(apiRevisionListDTO), 200);
        }
        return response;
    }

    /**
     * This method is used to deploy API Revision to Gateways.
     *
     * @param apiRevisionDeployRequestList API Revision deploy object body
     * @return HttpResponse
     * @throws ApiException throws of an error occurred when creating the API Revision.
     */
    public HttpResponse deployAPIRevision(String apiUUID, String revisionUUID,
                                          List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList,
                                          String apiType)
    throws ApiException, APIManagerIntegrationTestException {
        Gson gson = new Gson();
        List<APIRevisionDeploymentDTO> apiRevisionDeploymentDTOList = new ArrayList<>();
        List<APIRevisionDeploymentDTO> apiRevisionDeploymentDTOResponseList = new ArrayList<>();
        for (APIRevisionDeployUndeployRequest apiRevisionDeployRequest : apiRevisionDeployRequestList) {
            APIRevisionDeploymentDTO apiRevisionDeploymentDTO = new APIRevisionDeploymentDTO();
            apiRevisionDeploymentDTO.setName(apiRevisionDeployRequest.getName());
            apiRevisionDeploymentDTO.setVhost(apiRevisionDeployRequest.getVhost());
            apiRevisionDeploymentDTO.setDisplayOnDevportal(apiRevisionDeployRequest.isDisplayOnDevportal());
            apiRevisionDeploymentDTOList.add(apiRevisionDeploymentDTO);
        }
        try {
            ApiResponse<List<APIRevisionDeploymentDTO>> httpInfo =
                    apiRevisionsApi.deployAPIRevisionWithHttpInfo(apiUUID, revisionUUID, apiRevisionDeploymentDTOList);
            Assert.assertEquals(201, httpInfo.getStatusCode());

            waitForDeployAPI(apiUUID, revisionUUID, apiType);
            //apiRevisionDeploymentDTOResponseList = httpInfo.getData();
        } catch (ApiException e) {
            if (e.getResponseBody().contains("already exists")) {
                return null;
            } else if (e.getCode() != 0) {
                return new HttpResponse(null, e.getCode());
            }
            throw new ApiException(e);
        }
        HttpResponse response = null;
        response = new HttpResponse(gson.toJson(apiRevisionDeploymentDTOResponseList), 201);
//        if (StringUtils.isNotEmpty(apiRevisionDeploymentDTOResponseList.toString())) {
//            response = new HttpResponse(gson.toJson(apiRevisionDeploymentDTOResponseList), 201);
//        }
        return response;
    }

    /**
     * This method is used to deploy API Revision to Gateways.
     *
     * @param apiRevisionDeployRequestList API Revision deploy object body
     * @return HttpResponse
     * @throws ApiException throws of an error occurred when creating the API Revision.
     */
    public HttpResponse deployAPIRevision(String apiUUID, String revisionUUID,
                                          APIRevisionDeployUndeployRequest apiRevisionDeployRequest, String apiType)
    throws ApiException, APIManagerIntegrationTestException {
        Gson gson = new Gson();
        List<APIRevisionDeploymentDTO> apiRevisionDeploymentDTOList = new ArrayList<>();
        List<APIRevisionDeploymentDTO> apiRevisionDeploymentDTOResponseList = new ArrayList<>();
        APIRevisionDeploymentDTO apiRevisionDeploymentDTO = new APIRevisionDeploymentDTO();
        apiRevisionDeploymentDTO.setName(apiRevisionDeployRequest.getName());
        apiRevisionDeploymentDTO.setVhost(apiRevisionDeployRequest.getVhost());
        apiRevisionDeploymentDTO.setDisplayOnDevportal(apiRevisionDeployRequest.isDisplayOnDevportal());
        apiRevisionDeploymentDTOList.add(apiRevisionDeploymentDTO);
        try {
            ApiResponse<List<APIRevisionDeploymentDTO>> httpInfo =
                    apiRevisionsApi.deployAPIRevisionWithHttpInfo(apiUUID, revisionUUID, apiRevisionDeploymentDTOList);
            Assert.assertEquals(201, httpInfo.getStatusCode());
            waitForDeployAPI(apiUUID,revisionUUID, apiType);
        } catch (ApiException e) {
            if (e.getResponseBody().contains("already exists")) {
                return null;
            } else if (e.getCode() != 0) {
                return new HttpResponse(null, e.getCode());
            }
            throw new ApiException(e);
        }
        HttpResponse response = null;
        response = new HttpResponse(gson.toJson(apiRevisionDeploymentDTOResponseList), 201);
        return response;
    }

    private void waitForDeployAPI(String apiUUID, String revisionUUID, String apiType) throws ApiException,
            APIManagerIntegrationTestException {
        if (Boolean.parseBoolean(disableVerification)){
            return;
        }
        String context, version, provider, name, apiPolicy;
        if ("APIProduct".equals(apiType)) {
            APIProductDTO apiProduct = getApiProduct(revisionUUID);
            context = apiProduct.getContext();
            version = "1.0.0";
            provider = apiProduct.getProvider();
            name = apiProduct.getName();
            apiPolicy = apiProduct.getApiThrottlingPolicy();
        } else {
            APIDTO api = getAPIByID(revisionUUID);
            context = api.getContext();
            version = api.getVersion();
            provider = api.getProvider();
            name = api.getName();
            apiPolicy = api.getApiThrottlingPolicy();
        }
        APIInfoDTO apiInfo = restAPIGateway.getAPIInfo(apiUUID);
        if (apiInfo != null) {
            if (!"APIProduct".equals(apiType)) {
                if (context.startsWith("/{version}")) {
                    Assert.assertEquals(apiInfo.getContext(), context.replace("{version}", version));
                } else {
                    Assert.assertEquals(apiInfo.getContext(), context.concat("/").concat(version));
                }
            } else {
                Assert.assertEquals(apiInfo.getContext(), context);

            }
            Assert.assertEquals(apiInfo.getName(), name);
            Assert.assertEquals(apiInfo.getProvider(), provider);
            if (!StringUtils.equals(apiPolicy, apiInfo.getPolicy())) {
                int retries = 0;
                while (retries <= 5) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                    }
                    apiInfo = restAPIGateway.getAPIInfo(apiUUID);
                    if (!StringUtils.equals(apiPolicy, apiInfo.getPolicy())) {
                        retries++;
                    } else {
                        break;
                    }
                }
            }
            return;
        }
        int retries = 0;
        while (retries <= 20) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
            apiInfo = restAPIGateway.getAPIInfo(apiUUID);
            if (apiInfo != null) {
                if (!"APIProduct".equals(apiType)) {
                    if (context.startsWith("/{version}")) {
                        Assert.assertEquals(apiInfo.getContext(), context.replace("{version}", version));
                    } else {
                        Assert.assertEquals(apiInfo.getContext(), context.concat("/").concat(version));
                    }
                } else {
                    Assert.assertEquals(apiInfo.getContext(), context);

                }
                Assert.assertEquals(apiInfo.getName(), name);
                Assert.assertEquals(apiInfo.getProvider(), provider);
                break;
            }
            retries++;
        }
    }

    /**
     * This method is used to undeploy API Revision to Gateways.
     *
     * @param apiUUID                        API UUID
     * @param revisionUUID                   API Revision UUID
     * @param apiRevisionUndeployRequestList API Revision undeploy object body
     * @return HttpResponse
     * @throws ApiException throws of an error occurred when undeploying the API Revision.
     */
    public HttpResponse undeployAPIRevision(String apiUUID, String revisionUUID,
                                            List<APIRevisionDeployUndeployRequest> apiRevisionUndeployRequestList)
            throws ApiException, APIManagerIntegrationTestException {
        Gson gson = new Gson();
        List<APIRevisionDeploymentDTO> apiRevisionUnDeploymentDTOList = new ArrayList<>();
        List<APIRevisionDeploymentDTO> apiRevisionUnDeploymentDTOResponseList = new ArrayList<>();
        for (APIRevisionDeployUndeployRequest apiRevisionUndeployRequest : apiRevisionUndeployRequestList) {
            APIRevisionDeploymentDTO apiRevisionDeploymentDTO = new APIRevisionDeploymentDTO();
            apiRevisionDeploymentDTO.setName(apiRevisionUndeployRequest.getName());
            apiRevisionDeploymentDTO.setVhost(apiRevisionUndeployRequest.getVhost());
            apiRevisionDeploymentDTO.setDisplayOnDevportal(apiRevisionUndeployRequest.isDisplayOnDevportal());
            apiRevisionUnDeploymentDTOList.add(apiRevisionDeploymentDTO);
        }
        try {
            ApiResponse<Void> httpInfo = apiRevisionsApi.undeployAPIRevisionWithHttpInfo(apiUUID, revisionUUID, null,
                    false, apiRevisionUnDeploymentDTOList);
            Assert.assertEquals(201, httpInfo.getStatusCode());
            waitForUnDeployAPI(apiUUID);
            //apiRevisionUnDeploymentDTOResponseList = httpInfo.getData();
        } catch (ApiException e) {
            if (e.getResponseBody().contains("already exists")) {
                return null;
            } else if (e.getCode() != 0) {
                return new HttpResponse(null, e.getCode());
            }
            throw new ApiException(e);
        }
        HttpResponse response = null;
        response = new HttpResponse(gson.toJson(apiRevisionUnDeploymentDTOResponseList), 201);
        return response;
    }

    private void waitForUnDeployAPI(String apiUUID) throws APIManagerIntegrationTestException {

        if (Boolean.parseBoolean(disableVerification)) {
            return;
        }
        APIInfoDTO apiInfo = restAPIGateway.getAPIInfo(apiUUID);
        if (apiInfo == null) {
            return;
        }
        int retries = 0;
        while (retries <= 20) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
            apiInfo = restAPIGateway.getAPIInfo(apiUUID);
            if (apiInfo == null) {
                break;
            }
            retries++;
        }
    }

    /**
     * This method is used to restore an API Revision.
     *
     * @param apiUUID      API UUID
     * @param revisionUUID API Revision UUID
     * @return HttpResponse
     * @throws ApiException throws of an error occurred when creating the API Revision.
     */
    public HttpResponse restoreAPIRevision(String apiUUID, String revisionUUID) throws ApiException {

        Gson gson = new Gson();
        APIDTO apidto = null;
        try {
            ApiResponse<APIDTO> httpInfo = apiRevisionsApi.restoreAPIRevisionWithHttpInfo(apiUUID, revisionUUID);
            Assert.assertEquals(201, httpInfo.getStatusCode());
            apidto = httpInfo.getData();
        } catch (ApiException e) {
            if (e.getResponseBody().contains("already exists")) {
                return null;
            } else if (e.getCode() != 0) {
                return new HttpResponse(null, e.getCode());
            }
            throw new ApiException(e);
        }
        HttpResponse response = null;
        if (StringUtils.isNotEmpty(apidto.toString())) {
            response = new HttpResponse(gson.toJson(apidto), 201);
        }
        return response;
    }

    /**
     * This method is used to delete an API Revision.
     *
     * @param apiUUID      API UUID
     * @param revisionUUID API Revision UUID
     * @return HttpResponse
     * @throws ApiException throws of an error occurred when creating the API Revision.
     */
    public HttpResponse deleteAPIRevision(String apiUUID, String revisionUUID) throws ApiException {

        Gson gson = new Gson();
        APIRevisionListDTO apiRevisionListDTO = null;
        try {
            ApiResponse<APIRevisionListDTO> httpInfo = apiRevisionsApi.deleteAPIRevisionWithHttpInfo(apiUUID, revisionUUID);
            Assert.assertEquals(200, httpInfo.getStatusCode());
            apiRevisionListDTO = httpInfo.getData();
        } catch (ApiException e) {
            if (e.getResponseBody().contains("already exists")) {
                return null;
            } else if (e.getCode() != 0) {
                return new HttpResponse(null, e.getCode());
            }
            throw new ApiException(e);
        }
        HttpResponse response = null;
        if (StringUtils.isNotEmpty(apiRevisionListDTO.toString())) {
            response = new HttpResponse(gson.toJson(apiRevisionListDTO), 200);
        }
        return response;
    }

    public MediationListDTO retrieveMediationPolicies() throws ApiException {

        return globalMediationPoliciesApi.getAllGlobalMediationPolicies(100, 0, null, null);
    }

    /**
     * This method is used to create an API Product Revision.
     *
     * @param apiRevisionRequest API Revision create object body
     * @return HttpResponse
     * @throws ApiException throws of an error occurred when creating the API Revision.
     */
    public HttpResponse addAPIProductRevision(APIRevisionRequest apiRevisionRequest) throws ApiException {

        APIRevisionDTO apiRevisionDTO = new APIRevisionDTO();
        apiRevisionDTO.setDescription(apiRevisionRequest.getDescription());
        Gson gson = new Gson();
        try {
            ApiResponse<APIRevisionDTO> httpInfo = apiProductRevisionsApi.
                    createAPIProductRevisionWithHttpInfo(apiRevisionRequest.getApiUUID(), apiRevisionDTO);
            Assert.assertEquals(201, httpInfo.getStatusCode());
            apiRevisionDTO = httpInfo.getData();
        } catch (ApiException e) {
            if (e.getResponseBody().contains("already exists")) {
                return null;
            }
            throw new ApiException(e);
        }
        HttpResponse response = null;
        if (apiRevisionDTO != null && StringUtils.isNotEmpty(apiRevisionDTO.getId())) {
            response = new HttpResponse(gson.toJson(apiRevisionDTO), 201);
        }
        return response;
    }

    /**
     * Method to get API Product Revisions per API
     *
     * @param apiUUID - API uuid
     * @param query   - Search query
     * @return http response object
     * @throws ApiException - Throws if api information cannot be retrieved.
     */
    public HttpResponse getAPIProductRevisions(String apiUUID, String query) throws ApiException {

        APIRevisionListDTO apiRevisionListDTO = null;
        HttpResponse response = null;
        Gson gson = new Gson();
        try {
            apiRevisionListDTO = apiProductRevisionsApi.getAPIProductRevisions(apiUUID, query);
        } catch (ApiException e) {
            return new HttpResponse(gson.toJson(e.getResponseBody()), e.getCode());
        }
        if (StringUtils.isNotEmpty(apiRevisionListDTO.getList().toString())) {
            response = new HttpResponse(gson.toJson(apiRevisionListDTO), 200);
        }
        return response;
    }

    /**
     * This method is used to deploy API Product Revision to Gateways.
     *
     * @param apiRevisionDeployRequestList API Revision deploy object body
     * @return HttpResponse
     * @throws ApiException throws of an error occurred when creating the API Revision.
     */
    public HttpResponse deployAPIProductRevision(String apiUUID, String revisionUUID,
                                          List<APIRevisionDeployUndeployRequest> apiRevisionDeployRequestList,
                                                 String apiType)
            throws ApiException, APIManagerIntegrationTestException {
        Gson gson = new Gson();
        List<APIRevisionDeploymentDTO> apiRevisionDeploymentDTOList = new ArrayList<>();
        List<APIRevisionDeploymentDTO> apiRevisionDeploymentDTOResponseList = new ArrayList<>();
        for (APIRevisionDeployUndeployRequest apiRevisionDeployRequest : apiRevisionDeployRequestList) {
            APIRevisionDeploymentDTO apiRevisionDeploymentDTO = new APIRevisionDeploymentDTO();
            apiRevisionDeploymentDTO.setName(apiRevisionDeployRequest.getName());
            apiRevisionDeploymentDTO.setVhost(apiRevisionDeployRequest.getVhost());
            apiRevisionDeploymentDTO.setDisplayOnDevportal(apiRevisionDeployRequest.isDisplayOnDevportal());
            apiRevisionDeploymentDTOList.add(apiRevisionDeploymentDTO);
        }
        try {
            ApiResponse<Void> httpInfo = apiProductRevisionsApi.deployAPIProductRevisionWithHttpInfo(
                    apiUUID, revisionUUID, apiRevisionDeploymentDTOList);
            Assert.assertEquals(201, httpInfo.getStatusCode());
            waitForDeployAPI(apiUUID, revisionUUID, apiType);
            //apiRevisionDeploymentDTOResponseList = httpInfo.getData();
        } catch (ApiException e) {
            if (e.getResponseBody().contains("already exists")) {
                return null;
            }
            throw new ApiException(e);
        }
        HttpResponse response = null;
        response = new HttpResponse(gson.toJson(apiRevisionDeploymentDTOResponseList), 201);
//        if (StringUtils.isNotEmpty(apiRevisionDeploymentDTOResponseList.toString())) {
//            response = new HttpResponse(gson.toJson(apiRevisionDeploymentDTOResponseList), 201);
//        }
        return response;
    }

    /**
     * This method is used to undeploy API Product Revision to Gateways.
     *
     * @param apiUUID                        API UUID
     * @param revisionUUID                   API Revision UUID
     * @param apiRevisionUndeployRequestList API Revision undeploy object body
     * @return HttpResponse
     * @throws ApiException throws of an error occurred when undeploying the API Revision.
     */
    public HttpResponse undeployAPIProductRevision(String apiUUID, String revisionUUID,
                                            List<APIRevisionDeployUndeployRequest> apiRevisionUndeployRequestList)
            throws ApiException, APIManagerIntegrationTestException {
        Gson gson = new Gson();
        List<APIRevisionDeploymentDTO> apiRevisionUnDeploymentDTOList = new ArrayList<>();
        List<APIRevisionDeploymentDTO> apiRevisionUnDeploymentDTOResponseList = new ArrayList<>();
        for (APIRevisionDeployUndeployRequest apiRevisionUndeployRequest : apiRevisionUndeployRequestList) {
            APIRevisionDeploymentDTO apiRevisionDeploymentDTO = new APIRevisionDeploymentDTO();
            apiRevisionDeploymentDTO.setName(apiRevisionUndeployRequest.getName());
            apiRevisionDeploymentDTO.setVhost(apiRevisionUndeployRequest.getVhost());
            apiRevisionDeploymentDTO.setDisplayOnDevportal(apiRevisionUndeployRequest.isDisplayOnDevportal());
            apiRevisionUnDeploymentDTOList.add(apiRevisionDeploymentDTO);
        }
        try {
            ApiResponse<Void> httpInfo = apiProductRevisionsApi.undeployAPIProductRevisionWithHttpInfo(
                    apiUUID, revisionUUID, null, false, apiRevisionUnDeploymentDTOList);
            Assert.assertEquals(201, httpInfo.getStatusCode());
            waitForUnDeployAPI(apiUUID);
            //apiRevisionUnDeploymentDTOResponseList = httpInfo.getData();
        } catch (ApiException e) {
            if (e.getResponseBody().contains("already exists")) {
                return null;
            }
            throw new ApiException(e);
        }
        HttpResponse response = null;
        response = new HttpResponse(gson.toJson(apiRevisionUnDeploymentDTOResponseList), 201);
//        if (StringUtils.isNotEmpty(apiRevisionUnDeploymentDTOResponseList.toString())) {
//            response = new HttpResponse(gson.toJson(apiRevisionUnDeploymentDTOResponseList), 201);
//        }
        return response;
    }

    /**
     * This method is used to restore an API Product Revision.
     *
     * @param apiUUID      API UUID
     * @param revisionUUID API Revision UUID
     * @return HttpResponse
     * @throws ApiException throws of an error occurred when creating the API Revision.
     */
    public HttpResponse restoreAPIProductRevision(String apiUUID, String revisionUUID) throws ApiException {

        Gson gson = new Gson();
        APIProductDTO apiProductDTO = null;
        try {
            ApiResponse<APIProductDTO> httpInfo = apiProductRevisionsApi.restoreAPIProductRevisionWithHttpInfo(
                    apiUUID, revisionUUID);
            Assert.assertEquals(201, httpInfo.getStatusCode());
            apiProductDTO = httpInfo.getData();
        } catch (ApiException e) {
            if (e.getResponseBody().contains("already exists")) {
                return null;
            }
            throw new ApiException(e);
        }
        HttpResponse response = null;
        if (StringUtils.isNotEmpty(apiProductDTO.toString())) {
            response = new HttpResponse(gson.toJson(apiProductDTO), 201);
        }
        return response;
    }

    /**
     * This method is used to delete an API product Revision.
     *
     * @param apiUUID      API UUID
     * @param revisionUUID API Revision UUID
     * @return HttpResponse
     * @throws ApiException throws of an error occurred when creating the API Revision.
     */
    public HttpResponse deleteAPIProductRevision(String apiUUID, String revisionUUID) throws ApiException {

        Gson gson = new Gson();
        APIRevisionListDTO apiRevisionListDTO = null;
        try {
            ApiResponse<APIRevisionListDTO> httpInfo = apiProductRevisionsApi.deleteAPIProductRevisionWithHttpInfo(
                    apiUUID, revisionUUID);
            Assert.assertEquals(200, httpInfo.getStatusCode());
            apiRevisionListDTO = httpInfo.getData();
        } catch (ApiException e) {
            if (e.getResponseBody().contains("already exists")) {
                return null;
            }
            throw new ApiException(e);
        }
        HttpResponse response = null;
        if (StringUtils.isNotEmpty(apiRevisionListDTO.toString())) {
            response = new HttpResponse(gson.toJson(apiRevisionListDTO), 200);
        }
        return response;
    }

    public ApiResponse<APIKeyDTO> generateInternalApiKey(String apiId) throws ApiException {

        return apIsApi.generateInternalAPIKeyWithHttpInfo(apiId);
    }

    /**
     * Add comment to given API
     *
     * @param apiId    - api Id
     * @param comment  - comment to  add
     * @param category - category of the comment
     * @param replyTo  - comment id of the root comment to add replies
     * @return - http response of add comment
     * @throws ApiException - throws if add comment fails
     */

    public HttpResponse addComment(String apiId, String comment, String category, String replyTo) throws ApiException {

        PostRequestBodyDTO postRequestBodyDTO = new PostRequestBodyDTO();
        postRequestBodyDTO.setContent(comment);
        postRequestBodyDTO.setCategory(category);
        Gson gson = new Gson();
        CommentDTO commentDTO = commentsApi.addCommentToAPI(apiId, postRequestBodyDTO, replyTo);
        HttpResponse response = null;
        if (commentDTO != null) {
            response = new HttpResponse(gson.toJson(commentDTO), 200);
        }
        return response;
    }

    /**
     * Get Comment from given API
     *
     * @param commentId    - comment Id
     * @param apiId        - api Id
     * @param tenantDomain - tenant domain
     * @param limit        - for pagination
     * @param offset       - for pagination
     * @return - http response get comment
     * @throws ApiException - throws if get comment fails
     */
    public HttpResponse getComment(String commentId, String apiId, String tenantDomain, boolean includeCommentorInfo,
                                   Integer limit, Integer offset) throws ApiException {

        CommentDTO commentDTO;
        HttpResponse response = null;
        Gson gson = new Gson();
        try {
            commentDTO = commentsApi.getCommentOfAPI(commentId, apiId, tenantDomain, null,
                    includeCommentorInfo, limit, offset);
        } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
            return new HttpResponse(gson.toJson(e.getResponseBody()), e.getCode());
        }
        if (StringUtils.isNotEmpty(commentDTO.getId())) {
            response = new HttpResponse(gson.toJson(commentDTO), 200);
        }
        return response;
    }

    /**
     * Get all the comments from given API
     *
     * @param apiId        - api Id
     * @param tenantDomain - tenant domain
     * @param limit        - for pagination
     * @param offset       - for pagination
     * @return - http response get comment
     * @throws ApiException - throws if get comment fails
     */
    public HttpResponse getComments(String apiId, String tenantDomain, boolean includeCommentorInfo, Integer limit,
                                    Integer offset) throws ApiException {

        CommentListDTO commentListDTO;
        HttpResponse response = null;
        Gson gson = new Gson();
        try {
            commentListDTO = commentsApi.getAllCommentsOfAPI(apiId, tenantDomain, limit, offset, includeCommentorInfo);
        } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
            return new HttpResponse(gson.toJson(e.getResponseBody()), e.getCode());
        }
        if (commentListDTO.getCount() > 0) {
            response = new HttpResponse(gson.toJson(commentListDTO), 200);
        }
        return response;
    }

    /**
     * Get replies of a comment from given API
     *
     * @param commentId    - comment Id
     * @param apiId        - api Id
     * @param tenantDomain - tenant domain
     * @param limit        - for pagination
     * @param offset       - for pagination
     * @return - http response get comment
     * @throws ApiException - throws if get comment fails
     */
    public HttpResponse getReplies(String commentId, String apiId, String tenantDomain, boolean includeCommentorInfo, Integer limit, Integer offset)
            throws ApiException {

        CommentListDTO commentListDTO;
        HttpResponse response = null;
        Gson gson = new Gson();

        try {
            commentListDTO = commentsApi.getRepliesOfComment(commentId, apiId, tenantDomain, limit, offset, null, includeCommentorInfo);

        } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
            return new HttpResponse(gson.toJson(e.getResponseBody()), e.getCode());
        }
        if (commentListDTO.getCount() > 0) {
            response = new HttpResponse(gson.toJson(commentListDTO), 200);
        }

        return response;
    }

    /**
     * Get Comment from given API
     *
     * @param commentId - comment Id
     * @param apiId     - api Id
     * @param comment   - comment to  add
     * @param category  - category of the comment
     * @return - http response get comment
     * @throws ApiException - throws if get comment fails
     */
    public HttpResponse editComment(String commentId, String apiId, String comment, String category) throws
            ApiException {

        HttpResponse response = null;
        Gson gson = new Gson();
        try {
            PatchRequestBodyDTO patchRequestBodyDTO = new PatchRequestBodyDTO();
            patchRequestBodyDTO.setCategory(category);
            patchRequestBodyDTO.setContent(comment);

            CommentDTO editedCommentDTO = commentsApi.editCommentOfAPI(commentId, apiId, patchRequestBodyDTO);
            if (editedCommentDTO != null) {
                response = new HttpResponse(gson.toJson(editedCommentDTO), 200);
            } else {
                response = new HttpResponse(null, 200);
            }
        } catch (ApiException e) {
            return new HttpResponse(gson.toJson(e.getResponseBody()), e.getCode());
        }
        return response;
    }

    /**
     * Remove comment in given API
     *
     * @param commentId - comment Id
     * @param apiId     - api Id
     * @throws ApiException - throws if remove comment fails
     */
    public HttpResponse removeComment(String commentId, String apiId) throws ApiException {

        HttpResponse response;
        try {
            commentsApi.deleteComment(commentId, apiId, null);
            response = new HttpResponse("Successfully deleted the comment", 200);
        } catch (org.wso2.am.integration.clients.publisher.api.ApiException e) {
            response = new HttpResponse("Failed to delete the comment", e.getCode());
        }
        return response;
    }

    public ApiResponse<APIProductDTO> updateAPIProduct(APIProductDTO apiProductDTO) throws ApiException {

        return apiProductsApi.updateAPIProductWithHttpInfo(apiProductDTO.getId(), apiProductDTO, null);
    }

    public CertificatesDTO getEndpointCertificiates(String endpoint, String alias) throws ApiException {

        return endpointCertificatesApi.getEndpointCertificates(Integer.MAX_VALUE, 0, alias, endpoint);
    }

    public org.wso2.am.integration.clients.publisher.api.v1.dto.CertificateInfoDTO getendpointCertificateContent(String alias) throws ApiException {

        return endpointCertificatesApi.getEndpointCertificateByAlias(alias);

    }

    public ApiResponse<Void> deleteEndpointCertificate(String alias) throws ApiException {

        return endpointCertificatesApi.deleteEndpointCertificateByAliasWithHttpInfo(alias);

    }

    private void setActivityID() {

        apiPublisherClient.addDefaultHeader("activityID", System.getProperty(testNameProperty));
    }
}
