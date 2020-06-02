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
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.ApiResponse;
import org.wso2.am.integration.clients.store.api.v1.ApIsApi;
import org.wso2.am.integration.clients.store.api.v1.ApiKeysApi;
import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;
import org.wso2.am.integration.clients.store.api.v1.ApplicationsApi;
import org.wso2.am.integration.clients.store.api.v1.CommentsApi;
import org.wso2.am.integration.clients.store.api.v1.KeyManagersCollectionApi;
import org.wso2.am.integration.clients.store.api.v1.RatingsApi;
import org.wso2.am.integration.clients.store.api.v1.SdKsApi;
import org.wso2.am.integration.clients.store.api.v1.SubscriptionsApi;
import org.wso2.am.integration.clients.store.api.v1.TagsApi;
import org.wso2.am.integration.clients.store.api.v1.UnifiedSearchApi;
import org.wso2.am.integration.clients.store.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIInfoDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyMappingRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyReGenerateResponseDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.CommentDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.KeyManagerListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.RatingDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SearchResultListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.TagListDTO;
import org.wso2.am.integration.test.ClientAuthenticator;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

/**
 * This util class performs the actions related to APIDTOobjects.
 */
public class RestAPIStoreImpl {
    public ApIsApi apIsApi = new ApIsApi();
    public ApplicationsApi applicationsApi = new ApplicationsApi();
    public SubscriptionsApi subscriptionIndividualApi = new SubscriptionsApi();
    public ApplicationKeysApi applicationKeysApi = new ApplicationKeysApi();
    public CommentsApi commentsApi = new CommentsApi();
    public RatingsApi ratingsApi = new RatingsApi();
    public TagsApi tagsApi = new TagsApi();
    public SdKsApi sdKsApi = new SdKsApi();
    public ApiKeysApi apiKeysApi = new ApiKeysApi();
    public UnifiedSearchApi unifiedSearchApi = new UnifiedSearchApi();
    public KeyManagersCollectionApi keyManagersCollectionApi = new KeyManagersCollectionApi();
    ApiClient apiStoreClient = new ApiClient();
    public static final String appName = "Integration_Test_App_Store";
    public static final String callBackURL = "test.com";
    public static final String tokenScope = "Production";
    public static final String appOwner = "admin";
    public static final String grantType = "password";
    public static final String username = "admin";
    public static final String password = "admin";
    public String storeURL;
    public String tenantDomain;

    @Deprecated
    public RestAPIStoreImpl() {
        this(username, password, "", "https://localhost:9943");
    }

    public RestAPIStoreImpl(String username, String password, String tenantDomain, String storeURL) {
        // token/DCR of Store node itself will be used
        String tokenURL = storeURL + "oauth2/token";
        String dcrURL = storeURL + "client-registration/v0.16/register";
        String scopes = "openid apim:subscribe apim:app_update apim:app_manage apim:sub_manage "
                + "apim:self-signup apim:dedicated_gateway apim:store_settings apim:api_key";

        String accessToken = ClientAuthenticator
                .getAccessToken(scopes, appName, callBackURL, tokenScope, appOwner, grantType, dcrURL, username,
                        password, tenantDomain, tokenURL);

        apiStoreClient.setDebugging(Boolean.valueOf(System.getProperty("okHttpLogs")));
        apiStoreClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
        apiStoreClient.setBasePath(storeURL + "api/am/store/v1");
        apIsApi.setApiClient(apiStoreClient);
        applicationsApi.setApiClient(apiStoreClient);
        subscriptionIndividualApi.setApiClient(apiStoreClient);
        applicationKeysApi.setApiClient(apiStoreClient);
        commentsApi.setApiClient(apiStoreClient);
        ratingsApi.setApiClient(apiStoreClient);
        tagsApi.setApiClient(apiStoreClient);
        unifiedSearchApi.setApiClient(apiStoreClient);
        apiKeysApi.setApiClient(apiStoreClient);
        keyManagersCollectionApi.setApiClient(apiStoreClient);
        apiStoreClient.setDebugging(true);
        this.storeURL = storeURL;
        this.tenantDomain = tenantDomain;
    }


    public RestAPIStoreImpl(String tenantDomain, String storeURL) {
        apiStoreClient.setDebugging(Boolean.valueOf(System.getProperty("okHttpLogs")));
        apiStoreClient.setBasePath(storeURL + "api/am/store/v1");
        apiStoreClient.setDebugging(true);
        apIsApi.setApiClient(apiStoreClient);
        applicationsApi.setApiClient(apiStoreClient);
        subscriptionIndividualApi.setApiClient(apiStoreClient);
        applicationKeysApi.setApiClient(apiStoreClient);
        tagsApi.setApiClient(apiStoreClient);
        keyManagersCollectionApi.setApiClient(apiStoreClient);
        this.storeURL = storeURL;
        this.tenantDomain = tenantDomain;
    }

    public HttpResponse createApplication(String appName, String description, String throttleTier,
                                          ApplicationDTO.TokenTypeEnum tokenType) {
        try {
            ApplicationDTO application = new ApplicationDTO();
            application.setName(appName);
            application.setDescription(description);
            application.setThrottlingPolicy(throttleTier);
            application.setTokenType(tokenType);

            ApplicationDTO createdApp = applicationsApi.applicationsPost(application);
            HttpResponse response = null;
            if (StringUtils.isNotEmpty(createdApp.getApplicationId())) {
                response = new HttpResponse(createdApp.getApplicationId(), 200);
            }
            return response;
        } catch (ApiException e) {
            if (e.getResponseBody().contains("already exists")) {
                return null;
            }
        }
        return null;
    }

    public HttpResponse createApplicationWithCustomAttribute(String appName, String description, String throttleTier,
                                          ApplicationDTO.TokenTypeEnum tokenType, Map<String, String> attribute) {
        try {
            ApplicationDTO application = new ApplicationDTO();
            application.setName(appName);
            application.setDescription(description);
            application.setThrottlingPolicy(throttleTier);
            application.setTokenType(tokenType);
            application.setAttributes(attribute);

            ApplicationDTO createdApp = applicationsApi.applicationsPost(application);
            HttpResponse response = null;
            if (StringUtils.isNotEmpty(createdApp.getApplicationId())) {
                response = new HttpResponse(createdApp.getApplicationId(), 200);
            }
            return response;
        } catch (ApiException e) {
            if (e.getResponseBody().contains("already exists")) {
                return null;
            }
        }
        return null;
    }


    public HttpResponse deleteApplication(String applicationId) {
        try {
            applicationsApi.applicationsApplicationIdDelete(applicationId, null);
            applicationsApi.applicationsApplicationIdGet(applicationId, null);

            return null;
        } catch (ApiException e) {
            HttpResponse response = null;
            if (HttpStatus.SC_NOT_FOUND == e.getCode()) {
                response = new HttpResponse(applicationId, 200);
            }
            return response;
        }
    }

    public ApplicationListDTO getAllApps() throws ApiException {
        ApiResponse<ApplicationListDTO> appResponse = applicationsApi.applicationsGetWithHttpInfo(null,
                null, null, null, null, null, null);
        Assert.assertEquals(HttpStatus.SC_OK, appResponse.getStatusCode());
        return appResponse.getData();
    }

    public HttpResponse updateApplicationByID(String applicationId, String appName, String description,
                                              String throttleTier,
                                              ApplicationDTO.TokenTypeEnum tokenType){
        try {
            ApplicationDTO application = new ApplicationDTO();
            application.setName(appName);
            application.setDescription(description);
            application.setThrottlingPolicy(throttleTier);
            application.setTokenType(tokenType);

            ApplicationDTO createdApp = applicationsApi.applicationsApplicationIdPut(applicationId, application, null);
            HttpResponse response = null;
            if (StringUtils.isNotEmpty(createdApp.getApplicationId())) {
                response = new HttpResponse(createdApp.toString(), 200);
            }
            return response;
        } catch (ApiException e) {
            if (e.getResponseBody().contains("already exists")) {
                return null;
            }
        }
        return null;
    }

    public HttpResponse createSubscription(String apiId, String applicationId, String subscriptionTier) {
        try {
            SubscriptionDTO subscription = new SubscriptionDTO();
            subscription.setApplicationId(applicationId);
            subscription.setApiId(apiId);
            subscription.setThrottlingPolicy(subscriptionTier);
            subscription.setRequestedThrottlingPolicy(subscriptionTier);
            SubscriptionDTO subscriptionResponse = subscriptionIndividualApi.subscriptionsPost(subscription, this.tenantDomain);

            HttpResponse response = null;
            if (StringUtils.isNotEmpty(subscriptionResponse.getSubscriptionId())) {
                response = new HttpResponse(subscriptionResponse.getSubscriptionId(), 200);
            }
            return response;
        } catch (ApiException e) {
            if (e.getResponseBody().contains("already exists")) {
                return null;
            }
        }
        return null;
    }

    public SubscriptionListDTO getSubscription(String apiId, String applicationId, String apiType, String groupId)
            throws ApiException {
        ApiResponse<SubscriptionListDTO> suscriptionResponse = subscriptionIndividualApi.subscriptionsGetWithHttpInfo
                (apiId, applicationId, groupId, this.tenantDomain, null, null, null);
        Assert.assertEquals(HttpStatus.SC_OK, suscriptionResponse.getStatusCode());
        return suscriptionResponse.getData();
    }

    public HttpResponse removeSubscription(String subscriptionId) throws ApiException {

        ApiResponse<Void> deleteResponse = subscriptionIndividualApi.subscriptionsSubscriptionIdDeleteWithHttpInfo(subscriptionId, null);

        HttpResponse response = null;
        if (deleteResponse.getStatusCode() == 200) {
            response = new HttpResponse("Subscription deleted successfully : sub ID: " + subscriptionId, 200);
        }
        return response;

    }

    public ApplicationKeyDTO generateKeys(String applicationId, String validityTime, String callBackUrl,
                                          ApplicationKeyGenerateRequestDTO.KeyTypeEnum keyTypeEnum, ArrayList<String> scopes,
                                          List<String> grantTypes)
            throws ApiException {
        ApplicationKeyGenerateRequestDTO applicationKeyGenerateRequest = new ApplicationKeyGenerateRequestDTO();
        applicationKeyGenerateRequest.setValidityTime(validityTime);
        applicationKeyGenerateRequest.setCallbackUrl(callBackUrl);
        applicationKeyGenerateRequest.setKeyType(keyTypeEnum);
        applicationKeyGenerateRequest.setScopes(scopes);
        applicationKeyGenerateRequest.setGrantTypesToBeSupported(grantTypes);

        ApiResponse<ApplicationKeyDTO> response = applicationKeysApi
                .applicationsApplicationIdGenerateKeysPostWithHttpInfo(applicationId, applicationKeyGenerateRequest);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
        return response.getData();
    }

    public ApplicationKeyDTO generateKeys(String applicationId, String validityTime, String callBackUrl,
                                          ApplicationKeyGenerateRequestDTO.KeyTypeEnum keyTypeEnum, List<String> scopes,
                                          List<String> grantTypes,Map<String,Object> additionalProperties,
                                          String keyManager)
            throws ApiException {
        ApplicationKeyGenerateRequestDTO applicationKeyGenerateRequest = new ApplicationKeyGenerateRequestDTO();
        applicationKeyGenerateRequest.setValidityTime(validityTime);
        applicationKeyGenerateRequest.setCallbackUrl(callBackUrl);
        applicationKeyGenerateRequest.setKeyType(keyTypeEnum);
        applicationKeyGenerateRequest.setScopes(scopes);
        applicationKeyGenerateRequest.setGrantTypesToBeSupported(grantTypes);
        applicationKeyGenerateRequest.setAdditionalProperties(additionalProperties);
        applicationKeyGenerateRequest.setKeyManager(keyManager);
        ApiResponse<ApplicationKeyDTO> response = applicationKeysApi
                .applicationsApplicationIdGenerateKeysPostWithHttpInfo(applicationId, applicationKeyGenerateRequest);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
        return response.getData();
    }

    public ApiResponse<ApplicationKeyDTO> generateKeysWithApiResponse(String applicationId, String validityTime,
                                                           String callBackUrl,
                                                       ApplicationKeyGenerateRequestDTO.KeyTypeEnum keyTypeEnum, List<String> scopes,
                                                       List<String> grantTypes, Map<String,Object> additionalProperties,
                                                       String keyManager)
            throws ApiException {
        ApplicationKeyGenerateRequestDTO applicationKeyGenerateRequest = new ApplicationKeyGenerateRequestDTO();
        applicationKeyGenerateRequest.setValidityTime(validityTime);
        applicationKeyGenerateRequest.setCallbackUrl(callBackUrl);
        applicationKeyGenerateRequest.setKeyType(keyTypeEnum);
        applicationKeyGenerateRequest.setScopes(scopes);
        applicationKeyGenerateRequest.setGrantTypesToBeSupported(grantTypes);
        applicationKeyGenerateRequest.setAdditionalProperties(additionalProperties);
        applicationKeyGenerateRequest.setKeyManager(keyManager);
        ApiResponse<ApplicationKeyDTO> response = applicationKeysApi
                .applicationsApplicationIdGenerateKeysPostWithHttpInfo(applicationId, applicationKeyGenerateRequest);
        return response;
    }


    public APIKeyDTO generateAPIKeys(String applicationId, String keyType, int validityPeriod,
                                     String permittedIP, String permittedReferer) throws ApiException {
        APIKeyGenerateRequestDTO keyGenerateRequestDTO = new APIKeyGenerateRequestDTO();
        keyGenerateRequestDTO.setValidityPeriod(validityPeriod);
        HashMap additionalProperties = new HashMap<String, String>();
        additionalProperties.put("permittedIP", permittedIP);
        additionalProperties.put("permittedReferer", permittedReferer);
        keyGenerateRequestDTO.setAdditionalProperties(additionalProperties);

        ApiResponse<APIKeyDTO> response = apiKeysApi
                .applicationsApplicationIdApiKeysKeyTypeGeneratePostWithHttpInfo(applicationId, keyType,
                        keyGenerateRequestDTO, null);

        Assert.assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
        return response.getData();
    }

    /**
     * Regenerate consumer secret of an application
     *
     * @param applicationId - ID of the application
     * @param keyType       - PRODUCTION or SANDBOX
     * @param applicationKeyDTO - application DTO to be updated
     * @return - APIResponse of update request
     * @throws ApiException - throws if update keys fails.
     */
    public ApiResponse<ApplicationKeyDTO> updateKeys(String applicationId, String keyType,
            ApplicationKeyDTO applicationKeyDTO) throws Exception{

        return  applicationKeysApi
                .applicationsApplicationIdKeysKeyTypePutWithHttpInfo(applicationId, keyType, applicationKeyDTO);
    }

    /**
     * Regenerate consumer secret of an application
     *
     * @param applicationId - ID of the application
     * @param keyType       - PRODUCTION or SANDBOX
     * @return - APIResponse of re-generate request
     * @throws ApiException - throws if consumer secret re-generationx fails.
     */
    public ApiResponse<ApplicationKeyReGenerateResponseDTO> regenerateConsumerSecret(String applicationId,
                                                                                     String keyType) throws Exception {
        return applicationKeysApi
                .applicationsApplicationIdKeysKeyTypeRegenerateSecretPostWithHttpInfo(applicationId, keyType);
    }

    /**
     * Regenerate consumer secret of an application
     *
     * @param applicationId - ID of the application
     * @param keyType       - PRODUCTION or SANDBOX
     * @return - APIResponse of get application keys request
     * @throws ApiException - throws if get application keys fails.
     */
    public ApiResponse<ApplicationKeyDTO> getApplicationKeysByKeyType(String applicationId,
            String keyType) throws Exception {
        return applicationKeysApi
                .applicationsApplicationIdKeysKeyTypeGetWithHttpInfo(applicationId, keyType, null);
    }

    /**
     * Get api which are published
     *
     * @return - http response of get API post request
     * @throws ApiException - throws if API information retrieval fails.
     */
    public APIDTO getAPI(String apiId) throws ApiException {
        return apIsApi.apisApiIdGet(apiId, null, null);
    }

    /**
     * Get all published apis
     *
     * @return - http response of get all published apis
     * @throws APIManagerIntegrationTestException - throws if getting publish APIs fails
     */
    public APIListDTO getAllPublishedAPIs() throws APIManagerIntegrationTestException {
        try {
            return apIsApi.apisGet(null, 0, null, null, null);
        } catch (ApiException e) {
            throw new APIManagerIntegrationTestException("Error when retrieving APIs " + e.getResponseBody(), e);
        }
    }

    /**
     *
     * @return
     * @throws ApiException
     */
    public APIListDTO getAllAPIs() throws ApiException {
        return getAllAPIs(this.tenantDomain);
    }

    /**
     *
     * @return
     * @throws ApiException
     */
    public APIListDTO getAllAPIs(String tenantDomain) throws ApiException {
        ApiResponse<APIListDTO> apiResponse = apIsApi.apisGetWithHttpInfo(null, null, tenantDomain, null, null);
        Assert.assertEquals(HttpStatus.SC_OK, apiResponse.getStatusCode());
        return apiResponse.getData();
    }

    /**
     * Retrieve the APIs according to the search query in Publisher.
     *
     * @param query - The query on which the APIs needs to be filtered
     * @return SearchResultListDTO - The search results of the query
     * @throws ApiException
     */
    public SearchResultListDTO searchAPIs(String query) throws ApiException {
        ApiResponse<SearchResultListDTO> searchResponse = unifiedSearchApi
                .searchGetWithHttpInfo(null, null, this.tenantDomain, query, null);
        Assert.assertEquals(HttpStatus.SC_OK, searchResponse.getStatusCode());
        return searchResponse.getData();
    }

    /**
     * Retrieve the APIs according to the search query in Publisher.
     *
     * @param query - The query on which the APIs needs to be filtered
     * @param tenantDomain - The tenant domain on which the APIs needs to be filtered
     * @return SearchResultListDTO - The search results of the query
     * @throws ApiException
     */
    public SearchResultListDTO searchAPIs(String query, String tenantDomain) throws ApiException {
        ApiResponse<SearchResultListDTO> searchResponse = unifiedSearchApi
                .searchGetWithHttpInfo(null, null, tenantDomain, query, null);
        Assert.assertEquals(HttpStatus.SC_OK, searchResponse.getStatusCode());
        return searchResponse.getData();
    }

    /**
     * Get APIs for the given limit and offset values
     *
     * @param offset starting position
     * @param limit maximum number of APIs to return
     * @return APIs for the given limit and offset values
     * @throws ApiException
     */
    public APIListDTO getAPIs(int offset, int limit) throws ApiException {
        ApiResponse<APIListDTO> apiResponse = apIsApi.apisGetWithHttpInfo(limit, offset, this.tenantDomain, null, null);
        Assert.assertEquals(HttpStatus.SC_OK, apiResponse.getStatusCode());
        return apiResponse.getData();
    }


    /**
     * Get application by ID
     *
     * @return - http response of get of application
     * @throws ApiException - throws if get application fails.
     */
    public ApplicationDTO getApplicationById(String applicationId) throws ApiException {
        ApiResponse<ApplicationDTO> applicationDTOApiResponse = applicationsApi.
                applicationsApplicationIdGetWithHttpInfo(applicationId, null);
        Assert.assertEquals(applicationDTOApiResponse.getStatusCode(), HttpStatus.SC_OK);
        return applicationDTOApiResponse.getData();

    }

    /**
     * Get application details by given name
     *
     * @param applicationName - application name
     * @return - http response of get application request
     * @throws APIManagerIntegrationTestException - throws if get application by name fails
     */
    public HttpResponse getPublishedAPIsByApplication(String applicationName)
            throws APIManagerIntegrationTestException {
//        try {
//
//            checkAuthentication();
//            return HTTPSClientUtils.doGet(
//                    backendURL + "store/site/blocks/subscription/subscription-list/ajax/" +
//                            "subscription-list.jag?action=getSubscriptionByApplication&app=" +
//                            applicationName, requestHeaders);
//
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to retrieve the application -  " + applicationName
//                    + ". Error: " + e.getMessage(), e);
//        }
        return null;

    }

    /**
     * Get application details by given name
     *
     * @param applicationName - application name
     * @return - http response of get application request
     * @throws APIManagerIntegrationTestException - throws if get application by name fails
     */
    public HttpResponse getPublishedAPIsByApplicationId(String applicationName, int applicationId)
            throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doGet(
//                    backendURL + "store/site/blocks/subscription/subscription-list/ajax/" +
//                            "subscription-list.jag?action=getSubscriptionForApplicationById&app=" +
//                            applicationName + "&appId=" + applicationId, requestHeaders);
//
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to retrieve the application -  " + applicationName
//                    + ". Error: " + e.getMessage(), e);
//        }
        return null;
    }

    /**
     * Add rating into api
     *
     * @param apiId        - api Id
     * @param rating       - api rating
     * @param tenantDomain - tenant domain
     * @return - http response of add rating request
     * @throws ApiException - throws if rating of api fails
     */
    public HttpResponse addRating(String apiId, Integer rating, String tenantDomain) throws ApiException {
        RatingDTO ratingDTO = new RatingDTO();
        ratingDTO.setRating(rating);
        Gson gson = new Gson();
        ApiResponse<RatingDTO> apiResponse = ratingsApi
                .apisApiIdUserRatingPutWithHttpInfo(apiId, ratingDTO, tenantDomain);
        Assert.assertEquals(HttpStatus.SC_OK, apiResponse.getStatusCode());
        HttpResponse response = null;
        if (apiResponse.getData() != null && StringUtils.isNotEmpty(apiResponse.getData().getRatingId())) {
            response = new HttpResponse(gson.toJson(apiResponse.getData()), 200);
        }
        return response;
    }

    /**
     * Remove rating from given API
     *
     * @param apiId        - api Id
     * @param tenantDomain - tenant domain
     * @throws ApiException - throws if remove rating fails
     */
    public HttpResponse removeRating(String apiId, String tenantDomain) {
        HttpResponse response;
        try {
            ratingsApi.apisApiIdUserRatingDelete(apiId, tenantDomain, null);
            response = new HttpResponse("Successfully deleted the rating", 200);
        } catch (ApiException e) {
            response = new HttpResponse("Failed to delete the rating", e.getCode());
        }
        return response;
    }

    /**
     * Remove rating of given API
     *
     * @param apiName  - name of api
     * @param version  - api version
     * @param provider - provider of api
     * @return - http response of remove rating request
     * @throws APIManagerIntegrationTestException - Throws if remove API rating fails
     */
    public HttpResponse removeRatingFromAPI(String apiName, String version, String provider)
            throws APIManagerIntegrationTestException {
//        try {
//
//            checkAuthentication();
//            return HTTPSClientUtils.doGet(
//                    backendURL + "store/site/blocks/api/api-info/ajax/api-info.jag?" +
//                            "action=removeRating&name=" + apiName + "&version=" + version +
//                            "&provider=" + provider, requestHeaders);
//
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to remove rating of API -  " + apiName
//                    + ". Error: " + e.getMessage(), e);
//        }
        return null;
    }

    /**
     * Check if API rating activated
     *
     * @return - http response of rating activated request
     * @throws APIManagerIntegrationTestException - Throws if rating status cannot be retrieved
     */
    public HttpResponse isRatingActivated() throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doGet(
//                    backendURL + "store/site/blocks/api/api-info/ajax/api-info.jag?" +
//                            "action=isRatingActivated", requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Rating status cannot be retrieved."
//                    + " Error: " + e.getMessage(), e);
//        }
        return null;
    }

    /**
     * Method to retrieve all documents of given api
     *
     * @param apiName  - name of api
     * @param version  - api version
     * @param provider - provider of api
     * @return - http response of get all documentation of APIs
     * @throws APIManagerIntegrationTestException - throws if retrieval of API documentation fails
     */
    public HttpResponse getAllDocumentationOfAPI(String apiName, String version, String provider)
            throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doGet(
//                    backendURL + "store/site/blocks/api/listing/ajax/list.jag?" +
//                            "action=getAllDocumentationOfApi&name=" + apiName +
//                            "&version=" + version + "&provider=" + provider, requestHeaders);
//
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to retrieve documentation for - " +
//                    apiName + ". Error: " + e.getMessage(), e);
//
//        }
        return null;
    }

    /**
     * Method to retrieve all endpoint urls
     */
    public HttpResponse getApiEndpointUrls(String apiName, String version, String provider)
            throws APIManagerIntegrationTestException {
//        try{
//            checkAuthentication();
//            return HTTPSClientUtils.doGet(
//                    backendURL+ "store/site/blocks/api/api-info/ajax/api-info.jag?"+
//                            "action=getAPIEndpointURLs&name=" + apiName+
//                            "&version=" + version + "&provider=" + provider, requestHeaders);
//
//
//        }catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to retrieve documentation for - " +
//                    apiName + ". Error: " + e.getMessage(), e);
//
//        }
        return null;
    }


    /**
     * Get all paginated published API for a given tenant
     *
     * @param tenant - tenant name
     * @param start  - starting index
     * @param end    - closing  index
     * @return - http response of paginated published APIs
     * @throws APIManagerIntegrationTestException - throws if paginated apis cannot be retrieved.
     */
    public HttpResponse getAllPaginatedPublishedAPIs(String tenant, String start, String end)
            throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doGet(backendURL + "store/site/blocks/api/listing/ajax/list.jag?" +
//                    "action=getAllPaginatedPublishedAPIs&tenant=" + tenant +
//                    "&start=" + start + "&end=" + end, requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to retrieve paginated published APIs for tenant - "
//                    + tenant + ". Error: " + e.getMessage(), e);
//        }
        return null;
    }

    /**
     * Clean up application registration by ID
     *
     * @param applicationId   - application ID
     * @param keyType - key type
     * @return - http response of paginated published APIs
     * @throws APIManagerIntegrationTestException - throws if paginated apis cannot be retrieved.
     */
    public HttpResponse cleanUpApplicationRegistrationByApplicationId(String applicationId, String keyType)
            throws APIManagerIntegrationTestException, ApiException {

        ApiResponse<Void> httpInfo = applicationKeysApi
                .applicationsApplicationIdKeysKeyTypeCleanUpPostWithHttpInfo(applicationId, keyType, null);

        HttpResponse response = null;
        if (httpInfo.getStatusCode() == 200) {
            response = new HttpResponse("Successfully cleaned up the application registration", 200);
        }
        return response;
    }


    /**
     * Gell all paginated published apis for a given tenant
     *
     * @param tenant - tenant name
     * @param start  - starting index
     * @param end    - ending index
     */
    public HttpResponse getAllPaginatedPublishedAPIs(String tenant, int start, int end)
            throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//
//            return HTTPSClientUtils.doGet(backendURL + "store/site/blocks/api/listing/ajax/list.jag?" +
//                    "action=getAllPaginatedPublishedAPIs&tenant=" + tenant +
//                    "&start=" + start + "&end=" + end, requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to retrieve paginated published " +
//                    "APIs for tenant - " + tenant + ". Error: " + e.getMessage(), e);
//        }
        return null;
    }


    /**
     * Get all published APIs for tenant
     *
     * @param tenant - tenant name
     * @return - http response of published API
     * @throws APIManagerIntegrationTestException - throws if published API retrieval fails.
     */
    public HttpResponse getAllPublishedAPIs(String tenant)
            throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doPost(
//                    new URL(backendURL + "store/site/blocks/api/listing/ajax/list.jag?action=getAllPublishedAPIs&tenant=" +
//                            tenant), "", requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to retrieve published APIs for tenant - " + tenant
//                    + ". Error: " + e.getMessage(), e);
//
//        }
        return null;
    }

    /**
     * Add application
     *
     * @param application - application  name
     * @param tier        - throttling tier
     * @param callbackUrl - callback url
     * @param description - description of app
     * @return - http response of add application
     * @throws APIManagerIntegrationTestException - if fails to add application
     */
    public ApplicationDTO addApplication(String application, String tier, String callbackUrl, String description)
            throws ApiException {
        ApplicationDTO dto = new ApplicationDTO();
        dto.setName(application);
        dto.setThrottlingPolicy(tier);
        dto.setDescription(description);

        ApiResponse<ApplicationDTO> apiResponse = applicationsApi.applicationsPostWithHttpInfo(dto);
        Assert.assertEquals(HttpStatus.SC_CREATED, apiResponse.getStatusCode());
        return apiResponse.getData();
    }

    /**
     * Add application with token type
     *
     * @param application - application  name
     * @param tier        - throttling tier
     * @param description - description of app
     * @param tokenType   - token type of app (JWT ot DEFAULT)
     * @return - ApplicationDTO of add application
     * @throws APIManagerIntegrationTestException - if fails to add application
     */
    public ApplicationDTO addApplicationWithTokenType(String application, String tier, String callbackUrl,
                                                    String description, String tokenType)
            throws ApiException {

        ApplicationDTO dto = new ApplicationDTO();
        dto.setName(application);
        dto.setThrottlingPolicy(tier);
        dto.setDescription(description);
        dto.setTokenType(ApplicationDTO.TokenTypeEnum.fromValue(tokenType));

        ApiResponse<ApplicationDTO> apiResponse = applicationsApi.applicationsPostWithHttpInfo(dto);
        Assert.assertEquals(HttpStatus.SC_CREATED, apiResponse.getStatusCode());
        return apiResponse.getData();
    }

    /**
     * Get application
     *
     * @return - http response of get applications
     * @throws APIManagerIntegrationTestException - throws if applications cannot be retrieved.
     */
    public HttpResponse getApplications() throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doPost(
//                    new URL(backendURL + "store/site/blocks/application/application-list/ajax/" +
//                            "application-list.jag?action=getApplications"), "", requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to get applications. Error: " + e.getMessage(), e);
//
//        }
        return null;
    }

    /**
     * Delete application by Id
     *
     * @param applicationId - application Id
     * @return - http response of remove application request
     * @throws APIManagerIntegrationTestException - throws if remove application fails
     */
    public void removeApplicationById(String applicationId) throws ApiException {
        if (applicationId == null) {
            return;
        }
        ApiResponse<Void> response = applicationsApi.applicationsApplicationIdDeleteWithHttpInfo(applicationId, null);
        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
    }

    /**
     * Update given application
     *
     * @param applicationOld - application name old
     * @param applicationNew - new  application name
     * @param callbackUrlNew - call back url
     * @param descriptionNew - updated description
     * @param tier           - access tier
     * @return - http response of update application
     * @throws APIManagerIntegrationTestException - throws if update application fails
     */
    public HttpResponse updateApplication(String applicationOld, String applicationNew,
                                          String callbackUrlNew, String descriptionNew, String tier)
            throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doPost(
//                    new URL(backendURL + "store/site/blocks/application/application-update/ajax/application-update.jag?" +
//                            "action=updateApplication&applicationOld=" + applicationOld + "&applicationNew=" +
//                            applicationNew + "&callbackUrlNew=" + callbackUrlNew + "&descriptionNew=" +
//                            descriptionNew + "&tier=" + tier), "", requestHeaders);
//
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to update application - " + applicationOld
//                    + ". Error: " + e.getMessage(), e);
//
//        }
        return null;

    }

    /**
     * Update given application
     *
     * @param applicationOld - application name old
     * @param applicationNew - new  application name
     * @param callbackUrlNew - call back url
     * @param descriptionNew - updated description
     * @param tier           - access tier
     * @return - http response of update application
     * @throws APIManagerIntegrationTestException - throws if update application fails
     */
    public HttpResponse updateApplicationById(int applicationId, String applicationOld, String applicationNew,
                                              String callbackUrlNew, String descriptionNew, String tier) throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doPost(
//                    new URL(backendURL + "store/site/blocks/application/application-update/ajax/application-update.jag?" +
//                            "action=updateApplicationById&applicationOld=" + applicationOld + "&applicationNew=" +
//                            applicationNew + "&appId=" + applicationId + "&callbackUrlNew=" + callbackUrlNew + "&descriptionNew=" +
//                            descriptionNew + "&tier=" + tier), "", requestHeaders);
//
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to update application - " + applicationOld
//                    + ". Error: " + e.getMessage(), e);
//
//        }
        return null;

    }

    /**
     * Update given Auth application
     *
     * @param application       auth application name
     * @param keyType           type of the key
     * @param authorizedDomains authorized domains
     * @param retryAfterFailure retry after fail
     * @param jsonParams        json parameters for grant type
     * @param callbackUrl       call back url
     * @return Http response of the update request
     * @throws APIManagerIntegrationTestException APIManagerIntegrationTestException - throws if update application fail
     */
    public HttpResponse updateClientApplication(String application, String keyType, String authorizedDomains,
                                                String retryAfterFailure, String jsonParams, String callbackUrl) throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doPost(new URL(backendURL
//                            + "/store/site/blocks/subscription/subscription-add/ajax/subscription-add.jag?" +
//                            "action=updateClientApplication&application=" + application + "&keytype=" +
//                            keyType + "&authorizedDomains=" + authorizedDomains + "&retryAfterFailure=" +
//                            retryAfterFailure + "&jsonParams=" + URLEncoder.encode(jsonParams, "UTF-8")
//                            + "&callbackUrl=" + callbackUrl), "",
//                    requestHeaders);
//
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException(
//                    "Unable to update application - " + application + ". Error: " + e.getMessage(), e);
//
//        }
        return null;
    }

    /**
     * Update given Auth application
     *
     * @param applicationId       auth application id
     * @param applicationDTO      DTO of the application
     * @return Http response of the update request
     * @throws APIManagerIntegrationTestException APIManagerIntegrationTestException - throws if update application fail
     */
    public HttpResponse updateClientApplicationById(String applicationId, ApplicationDTO  applicationDTO) {

        try {
            ApplicationDTO responseApplicationDTO = applicationsApi.applicationsApplicationIdPut(applicationId, applicationDTO, null);
            HttpResponse response = null;
            if (StringUtils.isNotEmpty(responseApplicationDTO.getApplicationId())) {
                Gson gson = new Gson();
                response = new HttpResponse(gson.toJson(responseApplicationDTO), 200);
            }
            return response;
        } catch (ApiException e) {
            if (e.getResponseBody().contains("already exists")) {
                return null;
            }
        }

        return null;
    }

    /**
     * Regenerate consumer secret.
     *
     * @param clientId Consumer Key of an application for which consumer secret need to be regenerate.
     * @return Regenerated consumer secret.
     * @throws APIManagerIntegrationTestException Throws if regeneration of consumer secret fail.
     */
    public HttpResponse regenerateConsumerSecret(String clientId) throws APIManagerIntegrationTestException {

//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doPost(new URL(backendURL
//                    + "/store/site/blocks/subscription/subscription-add/ajax/subscription-add.jag?" +
//                    "action=regenerateConsumerSecret&clientId=" + clientId), "", requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to regenerate consumer secrete. "
//                    + " Error: " + e.getMessage(), e);
//        }
        return null;
    }

    /**
     * Get All subscriptions for an application.
     *
     * @param applicationId application
     * @return
     * @throws ApiException Throws if an error occurred when getting subscriptions.
     */
    public SubscriptionListDTO getAllSubscriptionsOfApplication(String applicationId) throws ApiException {

        SubscriptionListDTO subscriptionListDTO = subscriptionIndividualApi.subscriptionsGet(null, applicationId, null, this.tenantDomain, null, null, null);
        if (subscriptionListDTO.getCount() > 0) {
            return subscriptionListDTO;
        }
        return null;
    }

    /**
     * Get subscribed Apis by application name
     *
     * @param applicationName - Application Name
     */
    public HttpResponse getSubscribedAPIs(String applicationName) throws
            APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//
//
//            return HTTPSClientUtils.doPost(
//                    new URL(backendURL + "store/site/blocks/subscription/subscription-list/" +
//                            "ajax/subscription-list.jag?action=getAllSubscriptions&selectedApp="
//                            + applicationName), "", requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to get all subscribed APIs"
//                    + ". Error: " + e.getMessage(), e);
//
//        }
        return null;
    }


    /**
     * Get subscribed APIs for the specific Application
     *
     * @param applicationName - Name of the Application of API in store
     * @return - HttpResponse - Response with subscribed APIs
     * @throws APIManagerIntegrationTestException
     */

    public HttpResponse getSubscribedAPIs(String applicationName, String domain) throws
            APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doPost(
//                    new URL(backendURL + "store/site/blocks/subscription/subscription-list/" +
//                            "ajax/subscription-list.jag?action=getAllSubscriptions&selectedApp="
//                            + applicationName + "&tenant="+domain), "", requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to get all subscribed APIs"
//                    + ". Error: " + e.getMessage(), e);
//
//        }
        return null;
    }


    /**
     * Unsubscribe from API
     *
     * @param API           - name of api
     * @param version       - api version
     * @param provider      - provider name
     * @param applicationId - application id
     * @return - http response of unsubscription request
     * @throws APIManagerIntegrationTestException - Throws if unsubscription fails
     */
    public HttpResponse removeAPISubscription(String API, String version, String provider,
                                              String applicationId)
            throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doPost(
//                    new URL(backendURL + "store/site/blocks/subscription/subscription-remove/ajax/subscription-remove.jag?" +
//                            "action=removeSubscription&name=" + API + "&version=" + version + "&provider=" + provider +
//                            "&applicationId=" + applicationId), "", requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to get all subscriptions"
//                    + ". Error: " + e.getMessage(), e);
//
//        }
        return null;
    }

    /**
     * Unsubscribe from API by application name
     *
     * @param API             - name of api
     * @param version         - api version
     * @param provider        - provider name
     * @param applicationName - application Name
     * @return - http response of unsubscription request
     * @throws APIManagerIntegrationTestException - Throws if unsubscription fails
     */
    public HttpResponse removeAPISubscriptionByApplicationName(String API, String version,
                                                               String provider,
                                                               String applicationName)
            throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doPost(
//                    new URL(backendURL + "store/site/blocks/subscription/subscription-remove/ajax/subscription-remove.jag?" +
//                            "action=removeSubscription&name=" + API + "&version=" + version + "&provider=" + provider +
//                            "&applicationName=" + applicationName), "", requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to get all subscriptions"
//                    + ". Error: " + e.getMessage(), e);
//
//        }
        return null;
    }

    /**
     * Unsubscribe from API
     *
     * @param API      - name of api
     * @param version  - api version
     * @param provider - provider name
     * @param appName  - application name
     * @return - http response of unsubscription request
     * @throws APIManagerIntegrationTestException - Throws if unsubscription fails
     */

    public HttpResponse removeAPISubscriptionByName(String API, String version, String provider,
                                                    String appName) throws APIManagerIntegrationTestException {
//        try{
//            checkAuthentication();
//            HttpResponse responseApp = getAllApplications();
//            String appId = getApplicationId(responseApp.getData(), appName);
//
//            return removeAPISubscription(API,version,provider,appId);
//
//
//        } catch(Exception e){
//            throw new APIManagerIntegrationTestException("Unable to remove subscriptions API:" + API +
//                    " Version: " + version + "Provider: " + provider + "App Name: "+ appName +
//                    ". Error: " + e.getMessage(), e);
//
//        }
        return null;
    }

    /**
     * Get all API tags
     *
     * @return - http response of get all api tags
     * @throws APIManagerIntegrationTestException - throws if get all tags fails
     */
    public TagListDTO getAllTags() throws ApiException {
        ApiResponse<TagListDTO> tagsResponse = tagsApi.tagsGetWithHttpInfo(25, 0, tenantDomain, "");
        Assert.assertEquals(HttpStatus.SC_OK, tagsResponse.getStatusCode());
        return tagsResponse.getData();
    }

    /**
     * Add comment to given API
     *
     * @param apiId   - api Id
     * @param comment - comment to  add
     * @return - http response of add comment
     * @throws ApiException - throws if add comment fails
     */
    public HttpResponse addComment(String apiId, String comment) throws ApiException {
        CommentDTO commentDTO = new CommentDTO();
        commentDTO.setContent(comment);
        ApiResponse<CommentDTO> apiResponse = commentsApi.addCommentToAPIWithHttpInfo(apiId, commentDTO);
        Assert.assertEquals(HttpStatus.SC_CREATED, apiResponse.getStatusCode());
        HttpResponse response = null;
        if (apiResponse.getData() != null && StringUtils.isNotEmpty(apiResponse.getData().getId())) {
            response = new HttpResponse(apiResponse.getData().getId(), 201);
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
        } catch (ApiException e) {
            response = new HttpResponse("Failed to delete the comment", e.getCode());
        }
        return response;
    }

    /**
     * Get Comment from given API
     *
     * @param commentId - comment Id
     * @param apiId     - api Id
     * @param tenantDomain - tenant domain
     * @return - http response get comment
     * @throws ApiException - throws if get comment fails
     */
    public HttpResponse getComment(String commentId, String apiId, String tenantDomain) throws ApiException {
        CommentDTO commentDTO;
        HttpResponse response = null;
        Gson gson = new Gson();
        try {
            commentDTO = commentsApi.getCommentOfAPI(commentId, apiId, tenantDomain, null);
        } catch (ApiException e) {
            return new HttpResponse(gson.toJson(e.getResponseBody()), e.getCode());
        }
        if (StringUtils.isNotEmpty(commentDTO.getId())) {
            response = new HttpResponse(gson.toJson(commentDTO), 200);
        }
        return response;
    }

    /**
     * Check whether commenting is enabled
     *
     * @return - http response of comment status
     * @throws APIManagerIntegrationTestException - Throws if retrieving comment activation status fails.
     */
    public HttpResponse isCommentActivated() throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doGet(
//                    backendURL + "store/site/blocks/comment/comment-add/ajax/comment-add.jag?" +
//                            "action=isCommentActivated", requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Failed to get comment activation status"
//                    + ". Error: " + e.getMessage(), e);
//        }
        return null;
    }

    /**
     * Get recently added APIs by tenant
     *
     * @param tenant - tenant name
     * @param limit  - limit of result set
     * @return - http response of recently added API request
     * @throws APIManagerIntegrationTestException - throws if
     */
    public HttpResponse getRecentlyAddedAPIs(String tenant, String limit)
            throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doPost(
//                    new URL(backendURL + "store/site/blocks/api/" +
//                            "recently-added/ajax/list.jag?action=getRecentlyAddedAPIs&tenant=" +
//                            tenant + "&limit=" + limit), "", requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Failed to get recently added APIs from tenant - " +
//                    tenant + ". Error: " + e.getMessage(), e);
//        }
        return null;
    }

    private String getApplicationId(String jsonStringOfApplications, String applicationName)
            throws APIManagerIntegrationTestException {
//        String applicationId = null;
//        JSONObject obj;
//        try {
//            obj = new JSONObject(jsonStringOfApplications);
//            JSONArray arr = obj.getJSONArray("applications");
//            for (int i = 0; i < arr.length(); i++) {
//                String appName = arr.getJSONObject(i).getString("name");
//                if (applicationName.equals(appName)) {
//                    applicationId = arr.getJSONObject(i).getString("id");
//                }
//            }
//        } catch (JSONException e) {
//            throw new APIManagerIntegrationTestException("getting application Id failed"
//                    + ". Error: " + e.getMessage(), e);
//        }
//        return applicationId;
        return null;
    }

    /**
     * Get the  web page with filtered API when  click the API Tag link
     *
     * @param apiTag - API tag the need ti filter the api.waitForSwaggerDocument
     * @return HttpResponse - Response  that contains the web page with filtered API when  click the API Tag link
     * @throws APIManagerIntegrationTestException - Exception throws when check the Authentication and
     *                                            HTTPSClientUtils.sendGetRequest() method call
     */
    public APIListDTO getAPIsFilteredWithTags(String apiTag)
            throws APIManagerIntegrationTestException, ApiException {

        String query = "tag:" + apiTag;
        APIListDTO apis = apIsApi.apisGet(null, null, null, query, null);
        if (apis.getCount() > 0) {
            return apis;
        }
        return null;
    }

    /**
     * Get the  web page with filtered API when  click the API Tag link
     *
     * @param limit - number of APIs needs to be returned
     * @param offset - offset where the APIs needs to be returned should start
     * @param tenantDomain - tenant domain of which the APIs to be returned
     * @param query - query that needs to be passed to the backend
     * @return APIListDTO - The DTO which contains the list of matching APIs
     * @throws ApiException - Exception throws when check the Authentication and
     *                                            HTTPSClientUtils.sendGetRequest() method call
     */
    public APIListDTO searchPaginatedAPIs(int limit, int offset, String tenantDomain, String query)
            throws ApiException {

        APIListDTO apis = apIsApi.apisGet(limit, offset, tenantDomain, query, null);
        if (apis.getCount() > 0) {
            return apis;
        }
        return null;
    }

    /**
     * Subscribe and API. This method return the response of the subscription server REST call.
     *
     * @param subscriptionRequest -SubscriptionRequest request instance  with API subscription information.
     * @return HttpResponse - Response f the subscription server REST call
     * @throws APIManagerIntegrationTestException - Exception throws when check the Authentication and
     *                                            HTTPSClientUtils.doPost() method call.
     */
    public HttpResponse subscribeToAPI(SubscriptionRequest subscriptionRequest)
            throws APIManagerIntegrationTestException {
        //This method  do the same functionality as subscribe(), except this method  always returns the response object
        //regardless of the response code. But subscribe() returns the response object only if  the response code is
        // 200 or else it will return an Exception.
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doPost(new URL(backendURL +
//                            "/store/site/blocks/subscription/subscription-add/ajax/subscription-add.jag")
//                    , subscriptionRequest.generateRequestParameters(), requestHeaders);
//        } catch (Exception ex) {
//            throw new APIManagerIntegrationTestException("Exception when Subscribing to a API"
//                    + ". Error: " + ex.getMessage(), ex);
//        }
        return null;
    }

    public SubscriptionDTO subscribeToAPI(String apiID, String appID, String tier) throws ApiException {
        SubscriptionDTO subscription = new SubscriptionDTO();
        subscription.setApplicationId(appID);
        subscription.setApiId(apiID);
        subscription.setThrottlingPolicy(tier);
        ApiResponse<SubscriptionDTO> subscriptionResponse =
                subscriptionIndividualApi.subscriptionsPostWithHttpInfo(subscription, this.tenantDomain);
        Assert.assertEquals(HttpStatus.SC_CREATED, subscriptionResponse.getStatusCode());
        return subscriptionResponse.getData();
    }

//    /**
//     * Retrieve the API store page as anonymous user.
//     *
//     * @param storeTenantDomain - Tenant domain of store that need to  get the page.
//     * @return HttpResponse - Response with API store page of the provided domain.
//     * @throws APIManagerIntegrationTestException - IOException throws from HttpRequestUtil.doGet() method call
//     */
//
//    public HttpResponse getAPIStorePageAsAnonymousUser(String storeTenantDomain) throws APIManagerIntegrationTestException {
//        try {
//            return HttpRequestUtil.doGet(
//                    backendURL + "store/?tenant=" + storeTenantDomain, requestHeaders);
//        } catch (Exception ioE) {
//            throw new APIManagerIntegrationTestException(
//                    "Exception when retrieve the API store page as anonymous user", ioE);
//        }
//    }

    /**
     *
     * @param tenantDomain
     * @return
     * @throws ApiException
     */
    public APIListDTO getAPIListFromStoreAsAnonymousUser(String tenantDomain) throws ApiException {
        ApIsApi apIsApi = new ApIsApi();
        ApiClient apiStoreClient = new ApiClient();
        apiStoreClient = apiStoreClient.setBasePath(storeURL + "api/am/store/v1");
        apIsApi.setApiClient(apiStoreClient);

        ApiResponse<APIListDTO> apiResponse = apIsApi.apisGetWithHttpInfo(null, null, tenantDomain, null, null);
        Assert.assertEquals(HttpStatus.SC_OK, apiResponse.getStatusCode());
        return apiResponse.getData();
    }


    /**
     * API Store logout
     */
    public HttpResponse logout() throws APIManagerIntegrationTestException {
//        try{
//            checkAuthentication();
//            return HTTPSClientUtils.doPost(new URL(backendURL + "store/site/blocks/user/login/ajax/login.jag"),
//                    "action=logout", requestHeaders);
//        }catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Error in store app logout. Error: " + e.getMessage(), e);
//        }
        return null;
    }

    /**
     * API Store sign up
     *
     * @param userName  - store user name
     * @param password  -store password
     * @param firstName - user first name
     * @param lastName  - user's last name
     * @param email     - user's email
     * @return
     * @throws APIManagerIntegrationTestException
     */
    public HttpResponse signUp(String userName, String password, String firstName, String lastName, String email) throws
            APIManagerIntegrationTestException {
//        try {
//            return HTTPSClientUtils.doPost(new URL(backendURL + "store/site/blocks/user/sign-up/ajax/user-add.jag"),
//                    "action=addUser&username=" + userName + "&password=" + password + "&allFieldsValues=" + firstName +
//                            "|" + lastName + "|" + email, requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Error in user sign up. Error: " + e.getMessage(), e);
//        }
        return null;
    }

    /**
     * API Store sign up
     *
     * @param userName - store user name
     * @param password -store password
     * @param claims   - tenants claims
     * @return
     * @throws APIManagerIntegrationTestException
     */
    public HttpResponse signUpforTenant(String userName, String password, String claims) throws
            APIManagerIntegrationTestException {
//        try {
//            Map<String, String> requestHeaders = new HashMap<String, String>();
//            requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
//            return HttpRequestUtil.doPost(new URL(backendURL + "/store/site/blocks/user/sign-up/ajax/user-add.jag?tenant=wso2.com"),
//                    "action=addUser&username=" + userName + "&password=" + password + "&allFieldsValues=" +
//                            claims, requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Error in user sign up. Error: " + e.getMessage(), e);
//        }
        return null;
    }

    /**
     * API Store sign up with organization
     *
     * @param userName     - store user name
     * @param password     -store password
     * @param firstName    - user first name
     * @param lastName     - user's last name
     * @param email        - user's email
     * @param organization - user's organization
     * @return
     * @throws APIManagerIntegrationTestException
     */
    public HttpResponse signUpWithOrganization(String userName, String password, String firstName, String lastName, String email,
                                               String organization) throws APIManagerIntegrationTestException {
//        try {
//            return HTTPSClientUtils.doPost(new URL(backendURL + "store/site/blocks/user/sign-up/ajax/user-add.jag"),
//                    "action=addUser&username=" + userName + "&password=" + password + "&allFieldsValues=" + firstName
//                            + "|" + lastName + "|" + organization + "|" + email, requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Error in user sign up. Error: " + e.getMessage(), e);
//        }
        return null;
    }

    /**
     * Get Prototyped APIs in Store
     *
     * @return HttpResponse - Response with APIs which are deployed as a Prototyped APIs
     * @throws APIManagerIntegrationTestException
     */
    public APIListDTO getPrototypedAPIs(String tenant) throws APIManagerIntegrationTestException {
        try {
            APIListDTO prototypedAPIs = new APIListDTO();
            APIListDTO apiListDTO = apIsApi.apisGet(null, null, tenant, null, null);
            for (APIInfoDTO apidto : apiListDTO.getList()) {
                if (apidto.getLifeCycleStatus().equals("PROTOTYPED")) {
                    prototypedAPIs.addListItem(apidto);
                }
            }
            return prototypedAPIs;
        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to get prototype APIs. Error: " + e.getMessage(), e);
        }
    }
    /**
     * Wait for swagger document until its updated.
     *
     * @param userName         - Name of the api provider
     * @param apiName          - API Name
     * @param apiVersion       - API Version
     * @param expectedResponse - Expected response of the API
     * @param executionMode    - Mode of the test execution (Standalone or Platform)
     * @throws IOException                              - Throws if Swagger document cannot be found
     * @throws XPathExpressionException - Throws if Swagger document cannot be found
     */
    public void waitForSwaggerDocument(String userName, String apiName, String apiVersion,
                                       String expectedResponse, String executionMode)
            throws IOException, XPathExpressionException {

//        long currentTime = System.currentTimeMillis();
//        long waitTime = currentTime + WAIT_TIME;
//        HttpResponse response = null;
//
//        if (executionMode.equalsIgnoreCase(String.valueOf(ExecutionEnvironment.PLATFORM))) {
//
//            while (waitTime > System.currentTimeMillis()) {
//
//                log.info("WAIT for swagger document of API :" + apiName + " with version: " + apiVersion
//                        + " user :" + userName + " with expected response : " + expectedResponse);
//
//                try {
//                    response = getSwaggerDocument(userName, apiName, apiVersion, executionMode);
//                } catch (APIManagerIntegrationTestException ignored) {
//
//                }
//                if (response != null) {
//                    if (response.getData().contains(expectedResponse)) {
//                        log.info("API :" + apiName + " with version: " + apiVersion +
//                                " with expected response " + expectedResponse + " found");
//                        break;
//                    }
//                } else {
//                    try {
//                        Thread.sleep(500);
//                    } catch (InterruptedException ignored) {
//
//                    }
//
//                }
//            }
//        }

    }

    /**
     * This method will return swagger document of given api name and version
     *
     * @param userName      - User who request the swagger document
     * @param apiName       - Name of the API
     * @param apiVersion    - Version of the API
     * @param executionMode - Mode of the test execution (Standalone or Platform)
     * @return - HTTP Response of the GET swagger document request
     * @throws APIManagerIntegrationTestException - Throws if swagger document GET request fails
     */
    public HttpResponse getSwaggerDocument(String userName, String apiName, String apiVersion,
                                           String executionMode)
            throws APIManagerIntegrationTestException {
        HttpResponse response = null;

//        if (executionMode.equalsIgnoreCase(String.valueOf(ExecutionEnvironment.PLATFORM))) {
//            try {
//                checkAuthentication();
//                String tenant = MultitenantUtils.getTenantDomain(userName);
//                response = HTTPSClientUtils.doGet(backendURL + "store/api-docs/" + tenant + "/" +
//                        apiName + "/" + apiVersion, null);
//            } catch (IOException ex) {
//                throw new APIManagerIntegrationTestException("Exception when get APO page filtered by tag"
//                        + ". Error: " + ex.getMessage(), ex);
//            }
//
//        }
//        return response;
        return null;
    }

    /**
     * Get application page
     *
     * @return - http response of get application
     * @throws APIManagerIntegrationTestException - if fails to get application page
     */
    public HttpResponse getApplicationPage() throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doPost(new URL(backendURL + APIMIntegrationConstants.STORE_APPLICATION_REST_URL), "",
//                    requestHeaders);
//        } catch (APIManagerIntegrationTestException e) {
//            throw new APIManagerIntegrationTestException("No Session Cookie found. Please login first. "
//                    + "Error: " + e.getMessage(), e);
//        } catch (MalformedURLException e) {
//            throw new APIManagerIntegrationTestException("Unable to get application page, URL is not valid. "
//                    + "Error: " + e.getMessage(), e);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to get application page. Error: " + e.getMessage(), e);
//        }
        return null;
    }

    /**
     * Generate SDK for a given programming language
     *
     * @param apiId The api id which the sdk should be downloaded.
     * @param language  The required sdk language.
     * @return org.apache.http.HttpResponse for the SDK generation
     * @throws APIManagerIntegrationTestException if failed to generate the SDK
     */
    public ApiResponse<byte[]> generateSDKUpdated(String apiId, String language)
            throws ApiException, IOException {

        sdKsApi.setApiClient(apiStoreClient);
        return sdKsApi.apisApiIdSdksLanguageGetWithHttpInfo(apiId, language, this.tenantDomain);

    }

    /**
     * Generate SDK for a given programming language
     *
     * @param apiId The api id which the sdk should be downloaded.
     * @param language  The required sdk language.
     * @param tenantDomain The tenant domain of the sdk to be generated
     * @return org.apache.http.HttpResponse for the SDK generation
     * @throws APIManagerIntegrationTestException if failed to generate the SDK
     */
    public ApiResponse<byte[]> generateSDKUpdated(String apiId, String language, String tenantDomain)
            throws ApiException, IOException {

        sdKsApi.setApiClient(apiStoreClient);
        return sdKsApi.apisApiIdSdksLanguageGetWithHttpInfo(apiId, language, tenantDomain);

    }

    /**
     * Change password of the user
     *
     * @param username        username of the user
     * @param currentPassword current password of the user
     * @param newPassword     new password of the user
     * @return
     * @throws APIManagerIntegrationTestException if failed to change password
     */
    public HttpResponse changePassword(String username, String currentPassword, String newPassword)
            throws APIManagerIntegrationTestException {
//        try {
//            return HTTPSClientUtils.doPost(new URL(
//                    backendURL + "store/site/blocks/user/user-info/ajax/user-info.jag?action=changePassword" +
//                            "&username=" + username + "&currentPassword=" +
//                            currentPassword + "&newPassword=" + newPassword), "", requestHeaders);
//
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to change password. Error: " + e.getMessage(), e);
//        }
        return null;
    }

    /**
     * Add application with custom attributes
     *
     * @param application           - application  name
     * @param tier                  - throttling tier
     * @param callbackUrl           - callback url
     * @param description           - description of app
     * @param applicationAttributes - Json string of custom attributes defined by user
     * @return - http response of add application
     * @throws APIManagerIntegrationTestException - if fails to add application
     */
    public HttpResponse addApplicationWithCustomAttributes(String application, String tier, String callbackUrl,
                                                           String description, String applicationAttributes)
            throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            String urlAppAttributes = URLEncoder.encode(applicationAttributes, "UTF-8");
//            return HTTPSClientUtils.doPost(
//                    new URL(backendURL +
//                            "store/site/blocks/application/application-add" +
//                            "/ajax/application-add.jag?action=addApplication&tier=" +
//                            tier + "&callbackUrl=" + callbackUrl + "&description=" + description +
//                            "&application=" + application + "&applicationAttributes=" +
//                            urlAppAttributes), "", requestHeaders);
//        } catch (IOException e) {
//            String message = "Unable to add application - " + application + " with custom attributes. Error: "
//                    + e.getMessage();
//            log.error(message);
//            throw new APIManagerIntegrationTestException(message, e);
//        }
        return null;
    }

    public String getSwaggerByID(String apiId, String tenantDomain) throws ApiException {
        ApiResponse<String> response =
                apIsApi.apisApiIdSwaggerGetWithHttpInfo(apiId, null, "Production and Sandbox", null, tenantDomain);
        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        return response.getData();
    }



    /**
     * Generate user access key
     *
     * @param consumeKey       - consumer  key of user
     * @param consumerSecret   - consumer secret key
     * @param messageBody      - message body
     * @param tokenEndpointURL - token endpoint url
     * @return - http response of generate access token api call
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if generating APIM access token fails
     */
    public HttpResponse generateUserAccessKey(String consumeKey, String consumerSecret,
                                              String messageBody, URL tokenEndpointURL)
            throws APIManagerIntegrationTestException {

        try {
            Map<String, String> authenticationRequestHeaders = new HashMap<String, String>();
            String basicAuthHeader = consumeKey + ":" + consumerSecret;
            byte[] encodedBytes = Base64.encodeBase64(basicAuthHeader.getBytes("UTF-8"));

            authenticationRequestHeaders.put("Authorization", "Basic " + new String(encodedBytes, "UTF-8"));

            return HTTPSClientUtils.doPost(tokenEndpointURL, messageBody, authenticationRequestHeaders);

        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Unable to generate API access token. " +
                    "Error: " + e.getMessage(), e);
        }
    }

    public KeyManagerListDTO getKeyManagers() throws ApiException {
        return keyManagersCollectionApi.keyManagersGet(tenantDomain);
    }

    public ApplicationKeyDTO getApplicationKeyByKeyMappingId(String applicationId, String keyMappingId)
            throws ApiException {

        return applicationKeysApi.applicationsApplicationIdOauthKeysKeyMappingIdGet(applicationId, keyMappingId, null);
    }

    public ApplicationKeyDTO updateApplicationKeyByKeyMappingId(String applicationId, String keyMappingId,
                                                                ApplicationKeyDTO applicationKeyDTO)
            throws ApiException {

        return applicationKeysApi
                .applicationsApplicationIdOauthKeysKeyMappingIdPut(applicationId, keyMappingId, applicationKeyDTO);
    }

    public ApplicationKeyListDTO getApplicationKeysByAppId(String jwtAppId) throws ApiException {

        return applicationKeysApi.applicationsApplicationIdKeysGet(jwtAppId);
    }

    public ApplicationKeyDTO mapConsumerKeyWithApplication(String consumerKey, String appid, String keyManager) throws ApiException {

        ApplicationKeyMappingRequestDTO applicationKeyMappingRequestDTO =
                new ApplicationKeyMappingRequestDTO().consumerKey(consumerKey).keyType(
                        ApplicationKeyMappingRequestDTO.KeyTypeEnum.PRODUCTION).keyManager(keyManager);
        return applicationKeysApi.applicationsApplicationIdMapKeysPost(appid,applicationKeyMappingRequestDTO);
    }
}
