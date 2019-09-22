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

import org.apache.commons.lang.StringUtils;
import org.wso2.am.integration.clients.store.api.ApiClient;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.ApIsApi;
import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;
import org.wso2.am.integration.clients.store.api.v1.ApplicationsApi;
import org.wso2.am.integration.clients.store.api.v1.SubscriptionsApi;
import org.wso2.am.integration.clients.store.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.carbon.apimgt.test.ClientAuthenticator;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

/**
 * This util class performs the actions related to APIDTOobjects.
 */
public class RestAPIStoreImpl {
    public static ApIsApi apIsApi = new ApIsApi();
    public static ApplicationsApi applicationsApi = new ApplicationsApi();
    public static SubscriptionsApi subscriptionIndividualApi = new SubscriptionsApi();

    ApiClient apiStoreClient = new ApiClient();
    public static final String appName = "Integration_Test_App_Store";
    public static final String callBackURL = "test.com";
    public static final String tokenScope = "Production";
    public static final String appOwner = "admin";
    public static final String grantType = "password client_credentials";
    public static final String dcrEndpoint = "http://127.0.0.1:10263/client-registration/v0.14/register";
    public static final String username = "admin";
    public static final String password = "admin";
    public static final String tenantDomain = "";
    public static final String tokenEndpoint = "https://127.0.0.1:9943/oauth2/token";

    public RestAPIStoreImpl() {

        String scopes = "openid apim:subscribe apim:app_update apim:app_manage apim:sub_manage " +
                "apim:self-signup apim:dedicated_gateway apim:store_settings";

        String accessToken = ClientAuthenticator
                .getAccessToken(scopes,
                        appName, callBackURL, tokenScope, appOwner, grantType, dcrEndpoint, username, password, tenantDomain, tokenEndpoint);

        apiStoreClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
        apiStoreClient.setBasePath("https://localhost:9943/api/am/store/v1.0");
        apIsApi.setApiClient(apiStoreClient);
        applicationsApi.setApiClient(apiStoreClient);
        subscriptionIndividualApi.setApiClient(apiStoreClient);
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
        } catch (org.wso2.am.integration.clients.store.api.ApiException e) {
            if (e.getResponseBody().contains("already exists")) {
                return null;
            }
        }
        return null;
    }

    public HttpResponse createSubscription(String apiId, String applicationId, String subscriptionTier,
                                           SubscriptionDTO.StatusEnum statusEnum, SubscriptionDTO.TypeEnum typeEnum) {
        try {
            SubscriptionDTO subscription = new SubscriptionDTO();
            subscription.setApplicationId(applicationId);
            subscription.setApiId(apiId);
            subscription.setThrottlingPolicy(subscriptionTier);
            subscription.setStatus(statusEnum);
            subscription.setType(typeEnum);
            SubscriptionDTO subscriptionResponse = subscriptionIndividualApi.subscriptionsPost(subscription);

            HttpResponse response = null;
            if (StringUtils.isNotEmpty(subscriptionResponse.getSubscriptionId())) {
                response = new HttpResponse(subscriptionResponse.getSubscriptionId(), 200);
            }
            return response;
        } catch (org.wso2.am.integration.clients.store.api.ApiException e) {
            if (e.getResponseBody().contains("already exists")) {
                return null;
            }
        }
        return null;
    }

    public static ApplicationKeyDTO generateKeys(String applicationId, String validityTime, String callBackUrl,
                                                 ApplicationKeyGenerateRequestDTO.KeyTypeEnum keyTypeEnum, ArrayList<String> scopes,
                                                 ArrayList<String> grantTypes)
            throws org.wso2.am.integration.clients.store.api.ApiException {
        ApplicationKeyGenerateRequestDTO applicationKeyGenerateRequest = new ApplicationKeyGenerateRequestDTO();
        applicationKeyGenerateRequest.setValidityTime(validityTime);
        applicationKeyGenerateRequest.setCallbackUrl(callBackUrl);
        applicationKeyGenerateRequest.setKeyType(keyTypeEnum);
        applicationKeyGenerateRequest.setScopes(scopes);
        applicationKeyGenerateRequest.setGrantTypesToBeSupported(grantTypes);

        ApplicationKeysApi applicationKeysApi = new ApplicationKeysApi();
        return applicationKeysApi
                .applicationsApplicationIdGenerateKeysPost(applicationId, applicationKeyGenerateRequest);

    }


    /**
     * Get api which are published
     *
     * @return - http response of get API post request
     * @throws ApiException - throws if API information retrieval fails.
     */
    public HttpResponse getAPI(String apiId) throws ApiException {
        APIDTO apiDto = apIsApi.apisApiIdGet(apiId, null, null);
        HttpResponse response = null;
        if (StringUtils.isNotEmpty(apiDto.getId())) {
            response = new HttpResponse(apiDto.getId(), 200);
        }
        return response;
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

//        try {
//            //checkAuthentication();
//            Map<String, String> authenticationRequestHeaders = new HashMap<String, String>();
//            String basicAuthHeader = consumeKey + ":" + consumerSecret;
//            byte[] encodedBytes = Base64.encodeBase64(basicAuthHeader.getBytes("UTF-8"));
//
//            authenticationRequestHeaders.put("Authorization", "Basic " + new String(encodedBytes, "UTF-8"));
//
//            return HTTPSClientUtils.doPost(tokenEndpointURL, messageBody, authenticationRequestHeaders);
//
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to generate API access token. " +
//                    "Error: " + e.getMessage(), e);
//        }

        return null;
    }

    /**
     * Get all published apis
     *
     * @return - http response of get all published apis
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if getting publish APIs fails
     */
    public HttpResponse getAllPublishedAPIs() throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doGet(
//                    backendURL + "store/site/blocks/api/listing/ajax/list.jag?action=getAllPublishedAPIs",
//                    requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to get retrieve all published APIs. " +
//                    "Error: " + e.getMessage(), e);
//        }
        return null;
    }

    /**
     * Get all the applications
     *
     * @return - http response of get get all applications
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if get all application fails.
     */
    public HttpResponse getAllApplications() throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doGet(
//                    backendURL + "store/site/blocks/application/application-list/ajax/" +
//                            "application-list.jag?action=getApplications",
//                    requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to retrieve all applications. " +
//                    "Error: " + e.getMessage(), e);
//        }
        return null;
    }

    /**
     * Get application by ID
     *
     * @return - http response of get of application
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if get application fails.
     */
    public HttpResponse getApplicationById(int applicationId) throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doGet(
//                    backendURL + "store/site/blocks/application/application-list/ajax/" +
//                            "application-list.jag?action=getApplicationById&appId=" + applicationId,
//                    requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to retrieve all applications. " +
//                    "Error: " + e.getMessage(), e);
//        }
        return null;
    }

    /**
     * Get application details by given name
     *
     * @param applicationName - application name
     * @return - http response of get application request
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if get application by name fails
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
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if get application by name fails
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
     * @param apiName  - name of api
     * @param version  - api version
     * @param provider - provider of api
     * @param rating   - api rating
     * @return - http response of add rating request
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if rating of api fails
     */
    public HttpResponse addRatingToAPI(String apiName, String version, String provider,
                                       String rating) throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doGet(
//                    backendURL + "store/site/blocks/api/api-info/ajax/api-info.jag?" +
//                            "action=addRating&name=" + apiName + "&version=" + version + "&provider=" +
//                            provider + "&rating=" + rating, requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to rate API -  " + apiName
//                    + ". Error: " + e.getMessage(), e);
//        }
        return null;
    }

    /**
     * Remove rating of given API
     *
     * @param apiName  - name of api
     * @param version  - api version
     * @param provider - provider of api
     * @return - http response of remove rating request
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Throws if remove API rating fails
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
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Throws if rating status cannot be retrieved
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
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if retrieval of API documentation fails
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
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if paginated apis cannot be retrieved.
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
     * @param applicationName - application name
     * @return - http response of paginated published APIs
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if paginated apis cannot be retrieved.
     */
    public HttpResponse cleanUpApplicationRegistrationByApplicationId(int applicationId, String applicationName)
            throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            String requestData =
//                    "action=cleanUpApplicationRegistrationByApplicationId&appId=" + applicationId + "&applicationName="
//                            + applicationName + "&keyType=PRODUCTION";
//            return HTTPSClientUtils.doPost(new URL(
//                    backendURL + "store/site/blocks/subscription/subscription-add/ajax/subscription-add.jag?"
//                            + requestData), "", requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to cleanup application - "
//                    + applicationName + ". Error: " + e.getMessage(), e);
//        }
        return null;
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
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if published API retrieval fails.
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
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - if fails to add application
     */
    public HttpResponse addApplication(String application, String tier, String callbackUrl,
                                       String description)
            throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doPost(
//                    new URL(backendURL +
//                            "store/site/blocks/application/application-add" +
//                            "/ajax/application-add.jag?action=addApplication&tier=" +
//                            tier + "&callbackUrl=" + callbackUrl + "&description=" + description +
//                            "&application=" + application), "", requestHeaders);
//
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to add application - " + application
//                    + ". Error: " + e.getMessage(), e);
//
//        }
        return null;
    }

    /**
     * Add application with Group
     *
     * @param application - application  name
     * @param tier        - throttling tier
     * @param callbackUrl - callback url
     * @param description - description of app
     * @param groupId     - group to share
     * @return - http response of add application
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - if fails to add application
     */
    public HttpResponse addApplicationWithGroup(String application, String tier, String callbackUrl,
                                                String description, String groupId)
            throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doPost(
//                    new URL(backendURL +
//                            "store/site/blocks/application/application-add" +
//                            "/ajax/application-add.jag?action=addApplication&tier=" +
//                            tier + "&callbackUrl=" + callbackUrl + "&description=" + description +
//                            "&application=" + application + "&groupId=" + groupId), "", requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to add application - " + application
//                    + ". Error: " + e.getMessage(), e);
//        }
        return null;
    }

    /**
     * Add application with token type
     *
     * @param application - application  name
     * @param tier        - throttling tier
     * @param callbackUrl - callback url
     * @param description - description of app
     * @param tokenType   - token type of app (JWT ot DEFAULT)
     * @return - http response of add application
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - if fails to add application
     */
    public HttpResponse addApplicationWithTokenType(String application, String tier, String callbackUrl,
                                                    String description, String tokenType)
            throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doPost(
//                    new URL(backendURL +
//                            "store/site/blocks/application/application-add" +
//                            "/ajax/application-add.jag?action=addApplication&tier=" +
//                            tier + "&callbackUrl=" + callbackUrl + "&description=" + description +
//                            "&application=" + application + "&tokenType=" + tokenType), "", requestHeaders);
//
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to add application - " + application
//                    + ". Error: " + e.getMessage(), e);
//
//        }
        return null;
    }

    /**
     * Get application
     *
     * @return - http response of get applications
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if applications cannot be retrieved.
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
     * Delete application
     *
     * @param application - application name
     * @return - http response of remove application request
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if remove application fails
     */
    public HttpResponse removeApplication(String application)
            throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doPost(
//                    new URL(backendURL + "store/site/blocks/application/application-remove/ajax/application-remove.jag?" +
//                            "action=removeApplication&application=" + application), "", requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to remove application - " + application
//                    + ". Error: " + e.getMessage(), e);
//
//        }
        return null;
    }

    /**
     * Delete application by Id
     *
     * @param applicationId - application Id
     * @return - http response of remove application request
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if remove application fails
     */
    public HttpResponse removeApplicationById(int applicationId)
            throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doPost(
//                    new URL(backendURL + "store/site/blocks/application/application-remove/ajax/application-remove.jag?" +
//                            "action=removeApplicationById&appId=" + applicationId), "", requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to remove application - " + applicationId
//                    + ". Error: " + e.getMessage(), e);
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
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if update application fails
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
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if update application fails
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
     * @param application       auth application name
     * @param keyType           type of the key
     * @param authorizedDomains authorized domains
     * @param retryAfterFailure retry after fail
     * @param jsonParams        json parameters for grant type
     * @param callbackUrl       call back url
     * @return Http response of the update request
     * @throws APIManagerIntegrationTestException APIManagerIntegrationTestException - throws if update application fail
     */
    public HttpResponse updateClientApplicationById(int applicationId, String application, String keyType, String authorizedDomains,
                                                    String retryAfterFailure, String jsonParams, String callbackUrl) throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doPost(new URL(
//                    backendURL + "/store/site/blocks/subscription/subscription-add/ajax/subscription-add.jag?"
//                            + "action=updateClientApplicationByAppId&appId=" + applicationId + "&application="
//                            + application + "&keytype=" + keyType + "&authorizedDomains=" + authorizedDomains
//                            + "&retryAfterFailure=" + retryAfterFailure + "&jsonParams=" + URLEncoder
//                            .encode(jsonParams, "UTF-8") + "&callbackUrl=" + callbackUrl), "", requestHeaders);
//
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException(
//                    "Unable to update application - " + application + ". Error: " + e.getMessage(), e);
//
//        }
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

        SubscriptionListDTO subscriptionListDTO = subscriptionIndividualApi.subscriptionsGet(null, applicationId, null, null, null, null, null);
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
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Throws if unsubscription fails
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
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Throws if unsubscription fails
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
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Throws if unsubscription fails
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
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if get all tags fails
     */
    public HttpResponse getAllTags() throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//
//            return HTTPSClientUtils.doPost(
//                    new URL(backendURL + "store/site/blocks/tag/tag-cloud/ajax/list.jag?action=getAllTags"),
//                    "", requestHeaders);
//
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to get all tags. Error: " + e.getMessage(), e);
//        }
        return null;

    }


    /**
     * Add comment to given API
     *
     * @param apiName  - name of the api
     * @param version  - api version
     * @param provider - provider name
     * @param comment  - comment to  add
     * @return - http response of add comment
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if add comment fails
     */
    public HttpResponse addComment(String apiName, String version, String provider, String comment)
            throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doPost(
//                    new URL(backendURL + "store/site/blocks/comment/comment-add/ajax/comment-add.jag?" +
//                            "action=addComment&name=" + apiName + "&version=" + version + "&provider=" +
//                            provider + "&comment=" + comment), "", requestHeaders);
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable add a comment in to API - " + apiName
//                    + ". Error: " + e.getMessage(), e);
//        }
        return null;
    }

    /**
     * Check whether commenting is enabled
     *
     * @return - http response of comment status
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - Throws if retrieving comment activation status fails.
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
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - throws if
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
     * @param apiTag - API tag the need ti filter the api.
     * @return HttpResponse - Response  that contains the web page with filtered API when  click the API Tag link
     * @throws APIManagerIntegrationTestException - Exception throws when check the Authentication and
     *                                            HTTPSClientUtils.sendGetRequest() method call
     */
    public HttpResponse getAPIPageFilteredWithTags(String apiTag)
            throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//            return HTTPSClientUtils.doGet(backendURL + "/store/apis/list?tag=" + apiTag + "&tenant=carbon.super",
//                    requestHeaders);
//        } catch (IOException ex) {
//            throw new APIManagerIntegrationTestException("Exception when get APO page filtered by tag"
//                    + ". Error: " + ex.getMessage(), ex);
//        }
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

    public HttpResponse getAPIListFromStoreAsAnonymousUser(String tenantDomain)
            throws APIManagerIntegrationTestException {
//        try {
//            HttpResponse httpResponse = HTTPSClientUtils.doGet(backendURL + "store/site/blocks/api/recently-added/ajax/list.jag"
//                    + "?action=getRecentlyAddedAPIs&tenant=" + tenantDomain, new HashMap<String, String>());
//
//            if (new JSONObject(httpResponse.getData()).getBoolean("error")) {
//                throw new APIManagerIntegrationTestException("Error when getting API list as AsAnonymousUser");
//            }
//
//            return httpResponse;
//        } catch (IOException ioE) {
//            throw new APIManagerIntegrationTestException(
//                    "Exception when retrieve the API list as anonymous user. Error: " + ioE.getMessage(), ioE);
//        } catch (JSONException e) {
//            throw new APIManagerIntegrationTestException("Response message is not JSON Response"
//                    + ". Error: " + e.getMessage(), e);
//        }
        return null;
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
    public HttpResponse getPrototypedAPI(String tenant) throws APIManagerIntegrationTestException {
//        try {
//            checkAuthentication();
//
//            return HTTPSClientUtils.doGet(backendURL + "store/site/pages/list-prototyped-apis.jag?"
//                    + "tenant=" +tenant , requestHeaders);
//
//        } catch (Exception e) {
//            throw new APIManagerIntegrationTestException("Unable to get prototype APIs. Error: " + e.getMessage(), e);
//        }
        return null;
    }


    public HttpResponse searchPaginateAPIs(String tenant, String start, String end,
                                           String searchTerm)
            throws Exception {
//        checkAuthentication();
//        HttpResponse response = HTTPSClientUtils.doPost(new URL(
//                        backendURL + "/store/site/blocks/search/api-search/ajax/search.jag?")
//                , "action=searchAPIs&tenant=" + tenant + "&start=" + start + "&end=" + end + "&query=" + searchTerm
//                , requestHeaders);
//        if (response.getResponseCode() == 200) {
//            return response;
//        } else {
//            throw new Exception("Get API Information failed> " + response.getData());
//        }
        return null;
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
     * @throws javax.xml.xpath.XPathExpressionException - Throws if Swagger document cannot be found
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
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - if fails to get application page
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
     * @param sdkLanguage programming language for the SDK
     * @param apiName     name of the API
     * @param apiVersion  version of the API
     * @param apiProvider provider of the API
     * @return org.apache.http.HttpResponse for the SDK generation
     * @throws APIManagerIntegrationTestException if failed to generate the SDK
     */
    public org.apache.http.HttpResponse generateSDKUpdated(String sdkLanguage, String apiName, String apiVersion,
                                                           String apiProvider, String tenant)
            throws APIManagerIntegrationTestException {

//        try {
//            checkAuthentication();
//            SimpleHttpClient httpClient = new SimpleHttpClient();
//            String restURL = backendURL + "store/site/blocks/sdk/ajax/sdk-create.jag?" +
//                    "action=generateSDK&apiName=" + apiName + "&apiVersion=" + apiVersion + "&tenant=" +
//                    tenant + "&language=java";
//            //response is org.apache.http.HttpResponse, because we need to write it to a file
//            return httpClient.doGet(restURL, requestHeaders);
//        } catch (IOException e) {
//            throw new APIManagerIntegrationTestException("Error in generating SDK for API : " + apiName +
//                    " API version : " + apiVersion + " Error : " + e.getMessage(), e);
//        }
        return null;

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
     * @throws org.wso2.am.integration.test.utils.APIManagerIntegrationTestException - if fails to add application
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


}
