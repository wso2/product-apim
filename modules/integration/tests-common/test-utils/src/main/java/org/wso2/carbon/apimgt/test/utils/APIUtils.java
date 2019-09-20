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

package org.wso2.carbon.apimgt.test.utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.ApiIndividualApi;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIBusinessInformationDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APICorsConfigurationDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.WorkflowResponseDTO;
import org.wso2.am.integration.clients.store.api.v1.ApplicationsApi;
import org.wso2.am.integration.clients.store.api.v1.SubscriptionsApi;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.apimgt.test.ClientAuthenticator;
import org.wso2.carbon.apimgt.test.Constants;
import org.wso2.am.integration.clients.publisher.api.ApiClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This util class performs the actions related to APIDTOobjects.
 */
public class APIUtils {
    public static ApiIndividualApi apiPublisherApi = new ApiIndividualApi();
    public static ApplicationsApi apiIndividualApi = new ApplicationsApi();

    public static ApiClient apiPublisherClient = new ApiClient();
    public static org.wso2.am.integration.clients.store.api.ApiClient apiStoreClient = new org.wso2.am.integration.clients.store.api.ApiClient();
    public static final String appName = "Integration_Test_App";
    public static final String callBackURL = "test.com";
    public static final String tokenScope= "Production";
    public static final String appOwner= "admin";
    public static final String grantType= "client_credentials";
    public static final String dcrEndpoint= "http://127.0.0.1:10263/client-registration/v0.14/register";
    public static final String username= "admin";
    public static final String password= "admin";
    public static final String tenantDomain= "";
    public static final String tokenEndpoint= "https://127.0.0.1:9943/oauth2/token";

    public APIUtils(){

        apiPublisherClient.addDefaultHeader("Authorization", "Bearer " + ClientAuthenticator.getAccessToken("apim:subscribe apim:signup " +
                        "apim:workflow_approve apim:api_delete apim:api_update apim:api_view apim:api_create apim:api_publish apim:tier_view " +
                        "apim:tier_manage apim:subscription_view apim:apidef_update apim:subscription_block apim:workflow_approve",
                appName,  callBackURL,  tokenScope,  appOwner,grantType,  dcrEndpoint,  username,  password,  tenantDomain,  tokenEndpoint));

        apiStoreClient.addDefaultHeader("Authorization", "Bearer " + ClientAuthenticator.getAccessToken("apim:subscribe apim:signup " +
                        "apim:workflow_approve apim:api_delete apim:api_update apim:api_view apim:api_create apim:api_publish apim:tier_view " +
                        "apim:tier_manage apim:subscription_view apim:apidef_update apim:subscription_block apim:workflow_approve",
                appName,  callBackURL,  tokenScope,  appOwner,grantType,  dcrEndpoint,  username,  password,  tenantDomain,  tokenEndpoint));

        apiPublisherClient.setBasePath("https://localhost:9943/api/am/publisher/v1.0");
        apiPublisherApi.setApiClient(apiPublisherClient);
        apiIndividualApi.setApiClient(apiStoreClient);
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
////        body.setGatewayEnvironments(apiRequest.ge);
//        body.setSubscriptionAvailability(apiRequest.);
////        body.setVisibleRoles(visibleRoles);
//        body.setSubscriptionAvailableTenants(apiRequest.getV);
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
        if(StringUtils.isNotEmpty(apidto.getId())) {
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
            return IOUtils.toString(APIUtils.class.getClassLoader().getResourceAsStream(fileName),
                    StandardCharsets.UTF_8.name());
        }
        return null;
    }

    /**
     * This method is used to publish the created API.
     *
     * @param action API id that need to published.
     * @param apiId API id that need to published.
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

//    /**
//     * This methos id used to publish the created API for tenants.
//     *
//     * @param apiId API id that need to published.
//     * @throws ApiException throws if an error occurred when publishing the API.
//     */
//    public static void publishAPI(String apiId, String tenantDomain, String adminUsername, String adminPassword)
//            throws ApiException {
//        ApiIndividualApi apiIndividualApi = new ApiIndividualApi();
//        ApiClient apiPublisherClient = new ApiClient(tenantDomain, adminUsername, adminPassword);
//        apiIndividualApi.setApiClient(apiPublisherClient);
//        apiIndividualApi.apisChangeLifecyclePost(Constants.PUBLISHED, apiId, null, null);
//    }

    public  HttpResponse createApplication(String appName, String description, String throttleTier,
                                           ApplicationDTO.TokenTypeEnum tokenType) throws ApiException {
        try {
            ApplicationDTO application = new ApplicationDTO();
            application.setName(appName);
            application.setDescription(description);
            application.setThrottlingPolicy(throttleTier);
            application.setTokenType(tokenType);
            ApplicationDTO createdApp = null;


            apiIndividualApi.applicationsPost(application);
//            this.apiPublisherApi.apisApiIdGet(apidto.getId(), "carbon.super", null);
            HttpResponse response = null;
            if(StringUtils.isNotEmpty(createdApp.getApplicationId())) {
                response = new HttpResponse(createdApp.getApplicationId(), 200);
            }
            return response;
        } catch (org.wso2.am.integration.clients.store.api.ApiException e) {

        }
        return null;
    }

    public static String createSubscription(String apiId, String applicationId, String subscriptionTier,
                                            SubscriptionDTO.StatusEnum statusEnum)
            throws org.wso2.am.integration.clients.store.api.ApiException {
        SubscriptionDTO subscription = new SubscriptionDTO();
        subscription.setApplicationId(applicationId);
        subscription.setThrottlingPolicy(subscriptionTier);
        subscription.setStatus(statusEnum);

        SubscriptionsApi subscriptionIndividualApi = new SubscriptionsApi();
        return subscriptionIndividualApi.subscriptionsPost(subscription)
                .getSubscriptionId();

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
}
