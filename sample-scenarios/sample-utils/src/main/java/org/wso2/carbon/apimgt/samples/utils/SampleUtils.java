/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

package org.wso2.carbon.apimgt.samples.utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.AdvancedPolicyCollectionApi;
import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.api.AdvancedPolicyIndividualApi;
import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.model.AdvancedThrottlePolicy;
import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.model.BandwidthLimit;
import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.model.RequestCountLimit;
import org.wso2.carbon.apimgt.samples.utils.admin.rest.client.model.ThrottleLimit;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiClient;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.APICollectionApi;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.APIIndividualApi;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.model.API;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.model.APIBusinessInformation;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.model.APICorsConfiguration;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.model.Tier;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.model.TierList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * This util class performs the actions related to API objects.
 */
public class SampleUtils {

    /**
     * This method is used to create an API.
     *
     * @param apiName   API name.
     * @param version   API version.
     * @param context   API context.
     * @param visibleRoles  visible roles of the API.
     * @param visibleTenants    visible tenants of the API.
     * @param subscriptionAvailabilityEnum  subscription visible tenants if the API.
     * @param hostname  host name of the API Manager instance that needs to create the API.
     * @param port  port of the API Manager instance that need to create the API.
     * @param tags  tags that need to be added to the API.
     * @return Id of the created API.
     * @throws ApiException throws of an error occurred when creating the API.
     */
    public static String createApi(String apiName, String version, String context, ArrayList<String> visibleRoles,
            ArrayList<String> visibleTenants, API.SubscriptionAvailabilityEnum subscriptionAvailabilityEnum,
            String hostname, String port, ArrayList<String> tags) throws ApiException {

        APICollectionApi api = new APICollectionApi();
        API body = new API();

        body.setName(apiName);
        body.setContext(context);
        body.setVersion(version);
        body.setVisibility(API.VisibilityEnum.PUBLIC);
        body.setDescription(Constants.API_DESCRIPTION);
        body.setProvider(Constants.PROVIDER_ADMIN);
        body.setTransport(new ArrayList<String>() {{
            add(Constants.PROTOCOL_HTTPS);
        }});
        body.isDefaultVersion(false);
        body.setCacheTimeout(100);
        body.setGatewayEnvironments(Constants.GATEWAY_ENVIRONMENTS);
        body.setSubscriptionAvailability(subscriptionAvailabilityEnum);
        body.setVisibleRoles(visibleRoles);
        body.setSubscriptionAvailableTenants(visibleTenants);
        body.setSequences(new ArrayList<>());
        body.setBusinessInformation(new APIBusinessInformation());
        body.setCorsConfiguration(new APICorsConfiguration());
        body.setTags(tags);
        String endpointConfig;
        String filePrefix = context.substring(1);
        try {
            endpointConfig = getJsonContent(Constants.ENDPOINT_DEFINITION + filePrefix + Constants.JSON_EXTENSION);
        } catch (IOException e) {
            throw new ApiException("Could not read End point definition");
        }

        body.setEndpointConfig(endpointConfig);
        try {
            body.setApiDefinition(getJsonContent(Constants.API_DEFINITION + filePrefix + Constants.JSON_EXTENSION));
        } catch (IOException e) {
            throw new ApiException("Could not read API definition file");
        }
        List<String> tierList = new ArrayList<>();
        tierList.add(Constants.TIERS_UNLIMITED);
        body.setTiers(tierList);
        API response = api.apisPost(body, Constants.APPLICATION_JSON);
        return response.getId();
    }

    /**
     * This method is used to create an API for a tenant.
     *
     * @param apiName   API name.
     * @param version   API version.
     * @param context   API context.
     * @param visibleRoles  visible roles of the API.
     * @param visibleTenants    visible tenants of the API.
     * @param subscriptionAvailabilityEnum  subscription visible tenants if the API.
     * @param hostname  host name of the API Manager instance that needs to create the API.
     * @param port  port of the API Manager instance that need to create the API.
     * @param tags  tags that need to be added to the API.
     * @return Id of the created API.
     * @param tenantDomain  tenant domain name
     * @param adminUsername tenant admin username
     * @param adminPassword tenant admin password
     * @return
     * @throws ApiException throws of an error occurred when creating the API.
     */
    public static String createApiForTenant(String apiName, String version, String context,
            API.VisibilityEnum visibilityEnum, ArrayList<String> visibleRoles, ArrayList<String> visibleTenants,
            API.SubscriptionAvailabilityEnum subscriptionAvailabilityEnum, String hostname, String port,
            ArrayList<String> tags, String tenantDomain, String adminUsername, String adminPassword)
            throws ApiException {

        APICollectionApi api = new APICollectionApi();
        API body = new API();
        ApiClient apiClient = new ApiClient(tenantDomain, adminUsername, adminPassword);
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
        body.setGatewayEnvironments(Constants.GATEWAY_ENVIRONMENTS);
        body.setSubscriptionAvailability(subscriptionAvailabilityEnum);
        body.setVisibleRoles(visibleRoles);
        body.setSubscriptionAvailableTenants(visibleTenants);
        body.setSequences(new ArrayList<>());
        body.setBusinessInformation(new APIBusinessInformation());
        body.setCorsConfiguration(new APICorsConfiguration());
        body.setTags(tags);
        String endpointConfig;

        String[] splitData = context.split("/");
        String filePrefix = splitData[splitData.length -1];

        try {
            endpointConfig = getJsonContent(Constants.ENDPOINT_DEFINITION + filePrefix + Constants.JSON_EXTENSION);
        } catch (IOException e) {
            throw new ApiException("Could not read End point definition");
        }

        body.setEndpointConfig(endpointConfig);
        try {
            body.setApiDefinition(getJsonContent(Constants.API_DEFINITION + filePrefix + Constants.JSON_EXTENSION));
        } catch (IOException e) {
            throw new ApiException("Could not read API definition file");
        }
        List<String> tierList = new ArrayList<>();
        tierList.add(Constants.TIERS_UNLIMITED);
        body.setTiers(tierList);
        API response = api.apisPost(body, Constants.APPLICATION_JSON);
        return response.getId();
    }

    /**
     * This method is used to create a new API of the existing API.
     *
     * @param newVersion    new API version
     * @param apiId         old API ID
     * @return  apiID of the newly created api version.
     * @throws ApiException Throws if an error occurs when creating the new API version.
     */
    public static String createNewAPIVersion(String newVersion, String apiId) throws ApiException {
        APIIndividualApi api = new APIIndividualApi();
        String apiLocation = api.apisCopyApiPostWithHttpInfo(newVersion, apiId).getHeaders().get("Location").get(0);
        String[] splitValues = apiLocation.split("/");
        return  splitValues[splitValues.length -1];
    }


    /**
     * This method is used to get the JSON content.
     *
     * @return API definition.
     * @throws IOException  throws if an error occurred when creating the API.
     */
    private static String getJsonContent(String fileName) throws IOException {
        if (StringUtils.isNotEmpty(fileName)) {
            return IOUtils.toString(
                    SampleUtils.class.getClassLoader().getResourceAsStream(fileName),
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
        APIIndividualApi apiIndividualApi = new APIIndividualApi();
        apiIndividualApi.apisChangeLifecyclePost(Constants.PUBLISHED, apiId, null, null, null);
    }


    /**
     * This method is used to deprecate the created API.
     *
     * @param apiId API id that need to published.
     * @throws ApiException throws if an error occurred when publishing the API.
     */
    public static void deprecateAPI(String apiId) throws ApiException {
        APIIndividualApi apiIndividualApi = new APIIndividualApi();
        apiIndividualApi.apisChangeLifecyclePost(Constants.DEPRECATE, apiId, null, null, null);
    }

    /**
     * This method is used to block the created API.
     *
     * @param apiId API id that need to published.
     * @throws ApiException throws if an error occurred when publishing the API.
     */
    public static void blockAPI(String apiId) throws ApiException {
        APIIndividualApi apiIndividualApi = new APIIndividualApi();
        apiIndividualApi.apisChangeLifecyclePost(Constants.BLOCK, apiId, null, null, null);
    }

    /**
     * This method is used to reject the created API.
     *
     * @param apiId API id that need to published.
     * @throws ApiException throws if an error occurred when publishing the API.
     */
    public static void rejectAPI(String apiId) throws ApiException {
        APIIndividualApi apiIndividualApi = new APIIndividualApi();
        apiIndividualApi.apisChangeLifecyclePost(Constants.REJECT, apiId, null, null, null);
    }

    /**
     * This methos id used to publish the created API for tenants.
     *
     * @param apiId API id that need to published.
     * @throws ApiException throws if an error occurred when publishing the API.
     */
    public static void publishAPI(String apiId, String tenantDomain, String adminUsername, String adminPassword)
            throws ApiException {
        APIIndividualApi apiIndividualApi = new APIIndividualApi();
        ApiClient apiClient = new ApiClient(tenantDomain, adminUsername, adminPassword);
        apiIndividualApi.setApiClient(apiClient);
        apiIndividualApi.apisChangeLifecyclePost(Constants.PUBLISHED, apiId, null, null, null);
    }


}
