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

package org.wso2.carbon.apimgt.test.impl;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.ApiCollectionApi;
import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;
import org.wso2.am.integration.clients.publisher.api.v1.CertificatesIndividualApi;
import org.wso2.am.integration.clients.publisher.api.v1.DocumentIndividualApi;
import org.wso2.am.integration.clients.publisher.api.v1.RolesApi;
import org.wso2.am.integration.clients.publisher.api.v1.ThrottlingPoliciesApi;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIBusinessInformationDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APICorsConfigurationDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.CertMetadataDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.DocumentDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ThrottlingPolicyListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.WorkflowResponseDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.apimgt.test.ClientAuthenticator;
import org.wso2.carbon.apimgt.test.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This util class performs the actions related to APIDTOobjects.
 */
public class RestAPIPublisherImpl {
    public static ApiIndividualApi apiPublisherApi = new ApiIndividualApi();
    public static ApiCollectionApi apiCollectionApi = new ApiCollectionApi();
    public static DocumentIndividualApi documentIndividualApi = new DocumentIndividualApi();
    public static ThrottlingPoliciesApi throttlingPoliciesApi = new ThrottlingPoliciesApi();
    public static CertificatesIndividualApi certificatesIndividualApi = new CertificatesIndividualApi();
    public static RolesApi rolesApi = new RolesApi();

    public static ApiClient apiPublisherClient = new ApiClient();
    public static final String appName = "Integration_Test_App_Publisher";
    public static final String callBackURL = "test.com";
    public static final String tokenScope = "Production";
    public static final String appOwner = "admin";
    public static final String grantType = "client_credentials";
    public static final String dcrEndpoint = "http://127.0.0.1:10263/client-registration/v0.14/register";
    public static final String username = "admin";
    public static final String password = "admin";
    public static final String tenantDomain = "";
    public static final String tokenEndpoint = "https://127.0.0.1:9943/oauth2/token";

    public RestAPIPublisherImpl() {

        String accessToken = ClientAuthenticator
                .getAccessToken("apim:api_create apim:api_delete apim:api_publish apim:ep_certificates_view" +
                                "apim:ep_certificates_update apim:api_view apim:tier_manage apim:tier_view",
                        appName, callBackURL, tokenScope, appOwner, grantType, dcrEndpoint, username, password, tenantDomain, tokenEndpoint);

        apiPublisherClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
        apiPublisherClient.setBasePath("https://localhost:9943/api/am/publisher/v1.0");
        apiPublisherApi.setApiClient(apiPublisherClient);
        documentIndividualApi.setApiClient(apiPublisherClient);
        apiCollectionApi.setApiClient(apiPublisherClient);
        throttlingPoliciesApi.setApiClient(apiPublisherClient);
        certificatesIndividualApi.setApiClient(apiPublisherClient);
        rolesApi.setApiClient(apiPublisherClient);
    }


    /**
     * This method is used to create an API.
     *
     * @param apiRequest
     * @return HttpResponse
     * @throws ApiException throws of an error occurred when creating the API.
     */
    public HttpResponse addAPI(APIRequest apiRequest) throws ApiException {

        APIDTO body = new APIDTO();

        body.setName(apiRequest.getName());
        body.setContext(apiRequest.getContext());
        body.setVersion(apiRequest.getVersion());
        body.setVisibility(APIDTO.VisibilityEnum.PUBLIC);
        body.setDescription(Constants.API_DESCRIPTION);
        body.setProvider(Constants.PROVIDER_ADMIN);
        if (!StringUtils.isEmpty(tenantDomain)) {
            body.setProvider(username + "-AT-" + tenantDomain);
        }
        body.setTransport(new ArrayList<String>() {{
            add(Constants.PROTOCOL_HTTPS);
        }});
        body.isDefaultVersion(false);
        body.setCacheTimeout(100);
        ArrayList<String> gatewayEnvironments = new ArrayList<>();
        gatewayEnvironments.add(apiRequest.getEnvironment());
        body.setGatewayEnvironments(gatewayEnvironments);
//        body.setSubscriptionAvailability(ALL_TENANTS);
//        body.setVisibleRoles(visibleRoles);
//        body.setSubscriptionAvailableTenants();
        body.setBusinessInformation(new APIBusinessInformationDTO());
        body.setCorsConfiguration(new APICorsConfigurationDTO());
        body.setTags(Arrays.asList(apiRequest.getTags().split(",")));
        body.setEndpointConfig(apiRequest.getEndpointConfig());
        List<String> tierList = new ArrayList<>();
        tierList.add(Constants.TIERS_UNLIMITED);
        body.setPolicies(tierList);
        APIDTO apidto;
        try {
            apidto = apiPublisherApi.apisPost(body);
//            this.apiPublisherApi.apisApiIdGet(apidto.getId(), "carbon.super", null);
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


    /**
     * This method is used to create a new API of the existing API.
     *
     * @param newVersion     new API version
     * @param apiId          old API ID
     * @param defaultVersion is this the
     * @return apiID of the newly created api version.
     * @throws ApiException Throws if an error occurs when creating the new API version.
     */
    public static String createNewAPIVersion(String newVersion, String apiId, boolean defaultVersion) throws ApiException {
        String apiLocation = apiPublisherApi.apisCopyApiPostWithHttpInfo(newVersion, apiId, defaultVersion).getHeaders().get("Location").get(0);
        String[] splitValues = apiLocation.split("/");
        return splitValues[splitValues.length - 1];
    }

    /**
     * This method is used to get the JSON content.
     *
     * @return API definition.
     * @throws IOException throws if an error occurred when creating the API.
     */
    private static String getJsonContent(String fileName) throws IOException {
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
     * @throws ApiException throws if an error occurred when publishing the API.
     */
    public void changeAPILifeCycleStatus(String apiId, String action) throws ApiException {
        WorkflowResponseDTO response = this.apiPublisherApi.apisChangeLifecyclePost(action, apiId, null, null);
    }

    /**
     * This method is used to deprecate the created API.
     *
     * @param apiId API id that need to published.
     * @throws ApiException throws if an error occurred when publishing the API.
     */
    public static void deprecateAPI(String apiId) throws ApiException {

        apiPublisherApi.apisChangeLifecyclePost(Constants.DEPRECATE, apiId, null, null);
    }

    /**
     * This method is used to block the created API.
     *
     * @param apiId API id that need to published.
     * @throws ApiException throws if an error occurred when publishing the API.
     */
    public static void blockAPI(String apiId) throws ApiException {
        apiPublisherApi.apisChangeLifecyclePost(Constants.BLOCK, apiId, null, null);
    }

    /**
     * This method is used to reject the created API.
     *
     * @param apiId API id that need to published.
     * @throws ApiException throws if an error occurred when publishing the API.
     */
    public static void rejectAPI(String apiId) throws ApiException {

        apiPublisherApi.apisChangeLifecyclePost(Constants.REJECT, apiId, null, null);
    }

    /**
     * copy API from existing API
     *
     * @param newVersion - new version of the API
     * @param apiId      - existing API Id
     * @param isDefault  - make the default version
     * @return - http response object
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Throws if error occurred at API copy operation
     */
    public HttpResponse copyAPI(String newVersion, String apiId, Boolean isDefault) throws ApiException {
        ApiResponse<Void> httpResponse = apiPublisherApi.apisCopyApiPostWithHttpInfo(newVersion, apiId, isDefault);
        HttpResponse response = null;
        if (httpResponse.getStatusCode() == 200) {
            response = new HttpResponse("Successfully copied the new API", 200);
        }
        return response;
    }

    /**
     * Facilitate update API
     *
     * @param apiRequest - constructed API request object
     * @return http response object
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if update API fails
     */
    public HttpResponse updateAPI(APIRequest apiRequest, String apiId) throws ApiException {
        APIDTO body = new APIDTO();

        body.setName(apiRequest.getName());
        body.setContext(apiRequest.getContext());
        body.setVersion(apiRequest.getVersion());
        body.setVisibility(APIDTO.VisibilityEnum.PUBLIC);
        body.setDescription(Constants.API_DESCRIPTION);
        body.setProvider(Constants.PROVIDER_ADMIN);
        if (!StringUtils.isEmpty(tenantDomain)) {
            body.setProvider(username + "-AT-" + tenantDomain);
        }
        body.setTransport(new ArrayList<String>() {{
            add(Constants.PROTOCOL_HTTPS);
        }});
        body.isDefaultVersion(false);
        body.setCacheTimeout(100);
        ArrayList<String> gatewayEnvironments = new ArrayList<>();
        gatewayEnvironments.add(apiRequest.getEnvironment());
        body.setGatewayEnvironments(gatewayEnvironments);
//        body.setSubscriptionAvailability();
//        body.setVisibleRoles(visibleRoles);
//        body.setSubscriptionAvailableTenants(apiRequest.getV);
        List<APIOperationsDTO> operationsDTOS = null;
        body.setOperations(operationsDTOS);
        body.setBusinessInformation(new APIBusinessInformationDTO());
        body.setCorsConfiguration(new APICorsConfigurationDTO());
        body.setTags(Arrays.asList(apiRequest.getTags().split(",")));
        body.setEndpointConfig(apiRequest.getEndpointConfig());
        List<String> tierList = new ArrayList<>();
        tierList.add(Constants.TIERS_UNLIMITED);
        body.setPolicies(tierList);
        APIDTO apidto;
        try {
            apidto = apiPublisherApi.apisApiIdPut(apiId, body, null);
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


    /**
     * Method to get API information
     *
     * @param apiId - API id
     * @return http response object
     * @throws ApiException - Throws if api information cannot be retrieved.
     */
    public HttpResponse getAPI(String apiId) throws ApiException {
        APIDTO apidto = apiPublisherApi.apisApiIdGet(apiId, null, null);
        HttpResponse response = null;
        if (StringUtils.isNotEmpty(apidto.getId())) {
            response = new HttpResponse(apidto.getId(), 200);
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
        ApiResponse<Void> deleteResponse = apiPublisherApi.apisApiIdDeleteWithHttpInfo(apiId, null);
        HttpResponse response = null;
        if (deleteResponse.getStatusCode() == 200) {
            response = new HttpResponse("Successfully deleted the API", 200);
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

        ApiResponse<Void> deleteResponse = documentIndividualApi.apisApiIdDocumentsDocumentIdDeleteWithHttpInfo(apiId, docId, null);
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
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if access token revoke fails
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
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if permission update fails
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
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if API resource update fails
     */
    public HttpResponse updateResourceOfAPI(String apiId, String api)
            throws APIManagerIntegrationTestException {
        return null;
    }


    /**
     * Check whether the Endpoint is valid
     *
     * @param endpointUrl url of the endpoint
     * @param type        type of Endpoint
     * @return HttpResponse -  Response of the getAPI request
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Check for valid endpoint fails.
     */
    public HttpResponse checkValidEndpoint(String type, String endpointUrl, String providerName, String apiName,
                                           String apiVersion) throws APIManagerIntegrationTestException {
        return null;
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
        ApiResponse<WorkflowResponseDTO> responseDTOApiResponse = this.apiPublisherApi
                .apisChangeLifecyclePostWithHttpInfo(Constants.PUBLISHED, apiId, "Re-Subscription:" + isRequireReSubscription, null);
        HttpResponse response = null;
        if (responseDTOApiResponse.getStatusCode() == 200) {
            response = new HttpResponse("Successfully deleted the API", 200);
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
     * Adding a Document to a API
     *
     * @param apiId      - Id of the API.
     * @param docId      - document Id.
     * @param docContent - document content
     * @return HttpResponse - Response  with Document adding result.
     * @throws ApiException - Exception throws if error occurred when adding document.
     */
    public HttpResponse addDocument(String apiId, String docId, String docContent) throws ApiException {
        DocumentDTO doc = documentIndividualApi.apisApiIdDocumentsDocumentIdContentPost(apiId, docId, null, docContent, null);
        HttpResponse response = null;
        if (StringUtils.isNotEmpty(doc.getDocumentId())) {
            response = new HttpResponse("Successfully created the documentation", 200);
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

        DocumentDTO doc = documentIndividualApi.apisApiIdDocumentsDocumentIdPut(apiId, docId, documentDTO, null);
        HttpResponse response = null;
        if (StringUtils.isNotEmpty(doc.getDocumentId())) {
            response = new HttpResponse("Successfully created the documentation", 200);
        }
        return response;
    }


    /**
     * Retrieve the All APIs available for the user in Publisher.
     *
     * @return HttpResponse - Response that contains all available APIs for the user
     * @throws APIManagerIntegrationTestException - Exception throws from checkAuthentication() method and
     *                                            HTTPSClientUtils.doGet() method call
     */
    public APIListDTO getAllAPIs(String tenantDomain) throws APIManagerIntegrationTestException, ApiException {

        APIListDTO apis = apiCollectionApi.apisGet(null, null, null, null, null, null, null, tenantDomain);
        if (apis.getCount() > 0) {
            return apis;
        }
        return null;
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
    public HttpResponse uploadCertificate(File certificate, String alias, String endpoint) throws ApiException {

        CertMetadataDTO certificateDTO = certificatesIndividualApi.endpointCertificatesPost(certificate, alias, endpoint);
        HttpResponse response = null;
        if (StringUtils.isNotEmpty(certificateDTO.getAlias())) {
            response = new HttpResponse("Successfully uploaded the certificate", 200);
        }
        return response;

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

    /**
     * This method is used to validate roles.
     *
     * @param roleId role Id
     * @return HttpResponse
     * @throws APIManagerIntegrationTestException
     */
    public HttpResponse validateRoles(String roleId) throws ApiException {
        ApiResponse<Void> releResponse = rolesApi.validateSystemRoleWithHttpInfo(roleId);

        HttpResponse response = null;
        if (releResponse.getStatusCode() == 200) {
            response = new HttpResponse("Successfully validate the role", 200);
        }
        return response;
    }
}
