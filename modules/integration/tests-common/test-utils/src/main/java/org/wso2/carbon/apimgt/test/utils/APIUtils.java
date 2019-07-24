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
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.ApplicationsApi;
import org.wso2.am.integration.clients.store.api.v1.SubscriptionsApi;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;
import org.wso2.carbon.apimgt.test.ClientAuthenticator;
import org.wso2.carbon.apimgt.test.Constants;
import org.wso2.am.integration.clients.publisher.api.ApiClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * This util class performs the actions related to APIDTOobjects.
 */
public class APIUtils {

    /**
     * This method is used to create an API.
     *
     * @param apiName                      API name.
     * @param version                      API version.
     * @param context                      API context.
     * @param visibleRoles                 visible roles of the API.
     * @param visibleTenants               visible tenants of the API.
     * @param subscriptionAvailabilityEnum subscription visible tenants if the API.
     * @param hostname                     host name of the API Manager instance that needs to create the API.
     * @param port                         port of the API Manager instance that need to create the API.
     * @param tags                         tags that need to be added to the API.
     * @return Id of the created API.
     * @throws ApiException throws of an error occurred when creating the API.
     */
    public static String createApi(String apiName, String version, String context, ArrayList<String> visibleRoles,
                                   ArrayList<String> visibleTenants, APIDTO.SubscriptionAvailabilityEnum subscriptionAvailabilityEnum,
                                   String hostname, String port, ArrayList<String> tags) throws ApiException {

        ApiIndividualApi api = new ApiIndividualApi();
        ApiClient apiClient = new ApiClient();
        apiClient.setAccessToken();
        apiClient.addDefaultHeader("Authorization", "Bearer " + ClientAuthenticator.getAccessToken("apim:subscribe apim:signup " +
                "apim:workflow_approve apim:api_delete apim:api_update apim:api_view apim:api_create apim:api_publish apim:tier_view " +
                "apim:tier_manage apim:subscription_view apim:apidef_update apim:subscription_block apim:workflow_approve"));


        api.setApiClient(apiClient);
        APIDTO body = new APIDTO();

        body.setName(apiName);
        body.setContext(context);
        body.setVersion(version);
        body.setVisibility(APIDTO.VisibilityEnum.PUBLIC);
        body.setDescription(Constants.API_DESCRIPTION);
        body.setProvider(Constants.PROVIDER_ADMIN);
        body.setTransport(new ArrayList<String>() {{
            add(Constants.PROTOCOL_HTTPS);
        }});
        body.isDefaultVersion(false);
        body.setCacheTimeout(100);
//        body.setGatewayEnvironments(Constants.GATEWAY_ENVIRONMENTS);
        body.setSubscriptionAvailability(subscriptionAvailabilityEnum);
        body.setVisibleRoles(visibleRoles);
        body.setSubscriptionAvailableTenants(visibleTenants);
        body.setBusinessInformation(new APIBusinessInformationDTO());
        body.setCorsConfiguration(new APICorsConfigurationDTO());
        body.setTags(tags);
        String endpointConfig;
        String filePrefix = context.substring(1);
        try {
            endpointConfig = getJsonContent(Constants.ENDPOINT_DEFINITION + filePrefix + Constants.JSON_EXTENSION);
        } catch (IOException e) {
            throw new ApiException("Could not read End point definition");
        }

        body.setEndpointConfig(endpointConfig);
        List<String> tierList = new ArrayList<>();
        tierList.add(Constants.TIERS_UNLIMITED);
        body.setPolicies(tierList);
        APIDTO response;
        try {
            response = api.apisPost(body);
        } catch (ApiException e) {
            if (e.getResponseBody().contains("already exists")) {

                return null;
            }
            throw new ApiException(e);
        }
        return response.getId();
    }

    /**
     * This method is used to create an API for a tenant.
     *
     * @param apiName                      API name.
     * @param version                      API version.
     * @param context                      API context.
     * @param visibleRoles                 visible roles of the API.
     * @param visibleTenants               visible tenants of the API.
     * @param subscriptionAvailabilityEnum subscription visible tenants if the API.
     * @param hostname                     host name of the API Manager instance that needs to create the API.
     * @param port                         port of the API Manager instance that need to create the API.
     * @param tags                         tags that need to be added to the API.
     * @param tenantDomain                 tenant domain name
     * @param adminUsername                tenant admin username
     * @param adminPassword                tenant admin password
     * @return
     * @throws ApiException throws of an error occurred when creating the API.
     */
    public static String createApiForTenant(String apiName, String version, String context,
                                            APIDTO.VisibilityEnum visibilityEnum, ArrayList<String> visibleRoles, ArrayList<String> visibleTenants,
                                            APIDTO.SubscriptionAvailabilityEnum subscriptionAvailabilityEnum, String hostname, String port,
                                            ArrayList<String> tags, String tenantDomain, String adminUsername, String adminPassword)
            throws ApiException {

        ApiIndividualApi api = new ApiIndividualApi();
        APIDTO body = new APIDTO();
        ApiClient apiClient = new ApiClient();
        apiClient.addDefaultHeader("Authorization", "Bearer " + ClientAuthenticator.getAccessTokenForTenant("apim:subscribe apim:signup " +
                        "apim:workflow_approve apim:api_delete apim:api_update apim:api_view apim:api_create apim:api_publish apim:tier_view " +
                        "apim:tier_manage apim:subscription_view apim:apidef_update apim:subscription_block apim:workflow_approve",
                tenantDomain,  adminUsername,  adminPassword));

        api.setApiClient(apiClient);

        body.setName(apiName);
        body.setContext(context);
        body.setVersion(version);
        body.setVisibility(visibilityEnum);
        body.setDescription(Constants.API_DESCRIPTION);
        body.setProvider(adminUsername + "-AT-" + tenantDomain);
        body.setTransport(new ArrayList<String>() {{
            add(Constants.PROTOCOL_HTTPS);
        }});
        body.isDefaultVersion(false);
        body.setCacheTimeout(100);
//        body.setGatewayEnvironments(Constants.GATEWAY_ENVIRONMENTS);
        body.setSubscriptionAvailability(subscriptionAvailabilityEnum);
        body.setVisibleRoles(visibleRoles);
        body.setSubscriptionAvailableTenants(visibleTenants);
//        body.setSequences(new ArrayList<Sequence>());
        body.setBusinessInformation(new APIBusinessInformationDTO());
        body.setCorsConfiguration(new APICorsConfigurationDTO());
        body.setTags(tags);
        String endpointConfig;

        String[] splitData = context.split("/");
        String filePrefix = splitData[splitData.length - 1];

        try {
            endpointConfig = getJsonContent(Constants.ENDPOINT_DEFINITION + filePrefix + Constants.JSON_EXTENSION);
        } catch (IOException e) {
            throw new ApiException("Could not read End point definition");
        }

        body.setEndpointConfig(endpointConfig);

        APIOperationsDTO apiOperationsDTO1 = new APIOperationsDTO();
        apiOperationsDTO1.setUritemplate("/users");
        apiOperationsDTO1.setHttpVerb("GET");
        apiOperationsDTO1.setAuthType("Any");

        List<APIOperationsDTO> operationsDTOS = new ArrayList<APIOperationsDTO>();

        operationsDTOS.add(apiOperationsDTO1);

        body.setOperations(operationsDTOS);

        List<String> tierList = new ArrayList<String>();
        tierList.add(Constants.TIERS_UNLIMITED);
        body.setPolicies(tierList);

        APIDTO response;
        try {
            response = api.apisPost(body);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return response.getId();
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
        ApiIndividualApi api = new ApiIndividualApi();
        String apiLocation = api.apisCopyApiPostWithHttpInfo(newVersion, apiId, defaultVersion).getHeaders().get("Location").get(0);
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
     * @param apiId API id that need to published.
     * @throws ApiException throws if an error occurred when publishing the API.
     */
    public static void publishAPI(String apiId) throws ApiException {
        ApiIndividualApi apiIndividualApi = new ApiIndividualApi();
        apiIndividualApi.apisChangeLifecyclePost(Constants.PUBLISHED, apiId, null, null);
    }

    /**
     * This method is used to deprecate the created API.
     *
     * @param apiId API id that need to published.
     * @throws ApiException throws if an error occurred when publishing the API.
     */
    public static void deprecateAPI(String apiId) throws ApiException {
        ApiIndividualApi apiIndividualApi = new ApiIndividualApi();
        apiIndividualApi.apisChangeLifecyclePost(Constants.DEPRECATE, apiId, null, null);
    }

    /**
     * This method is used to block the created API.
     *
     * @param apiId API id that need to published.
     * @throws ApiException throws if an error occurred when publishing the API.
     */
    public static void blockAPI(String apiId) throws ApiException {
        ApiIndividualApi apiIndividualApi = new ApiIndividualApi();
        apiIndividualApi.apisChangeLifecyclePost(Constants.BLOCK, apiId, null, null);
    }

    /**
     * This method is used to reject the created API.
     *
     * @param apiId API id that need to published.
     * @throws ApiException throws if an error occurred when publishing the API.
     */
    public static void rejectAPI(String apiId) throws ApiException {
        ApiIndividualApi apiIndividualApi = new ApiIndividualApi();
        apiIndividualApi.apisChangeLifecyclePost(Constants.REJECT, apiId, null, null);
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
//        ApiClient apiClient = new ApiClient(tenantDomain, adminUsername, adminPassword);
//        apiIndividualApi.setApiClient(apiClient);
//        apiIndividualApi.apisChangeLifecyclePost(Constants.PUBLISHED, apiId, null, null);
//    }

    public static String createApplication(String appName, String description, String throttleTier) {
        try {
            ApplicationDTO application = new ApplicationDTO();
            application.setName(appName);
            application.setDescription(description);
            application.setThrottlingPolicy(throttleTier);

            ApplicationsApi applicationIndividualApi = new ApplicationsApi();
            ApplicationDTO createdApp = applicationIndividualApi.applicationsPost(application);
            return createdApp.getApplicationId();
        } catch (org.wso2.am.integration.clients.store.api.ApiException e) {
            // Ignoring the exception to facilitate running the same sample to a specific APIM instance multiple times.
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
