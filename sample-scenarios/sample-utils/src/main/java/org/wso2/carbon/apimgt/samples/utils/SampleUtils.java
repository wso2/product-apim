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
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiClient;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.APICollectionApi;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.api.APIIndividualApi;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.model.API;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.model.APIBusinessInformation;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.model.APICorsConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * This util class performs the actions related to API objects.
 */
public class SampleUtils {

    private static String apiDefinition = null;

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
            add(Constants.PROTOCOL_HTTP);
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
        String endpointConfig = "{\"production_endpoints\":{\"url\":\"https://" + hostname + ":" + port
                + "/am/sample/pizzashack/v1/api/\",\"config\":null},\"sandbox_endpoints\":"
                + "{\"url\":\"https://localhost:9443/am/sample/pizzashack/v1/api/\",\"config\":null},"
                + "\"endpoint_type\":\"http\"}";

        body.setEndpointConfig(endpointConfig);
        try {
            body.setApiDefinition(getApiDefinition());
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
            add(Constants.PROTOCOL_HTTP);
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
        String endpointConfig = "{\"production_endpoints\":{\"url\":\"https://" + hostname + ":" + port
                + "/am/sample/pizzashack/v1/api/\",\"config\":null},\"sandbox_endpoints\":"
                + "{\"url\":\"https://localhost:9443/am/sample/pizzashack/v1/api/\",\"config\":null},"
                + "\"endpoint_type\":\"http\"}";

        body.setEndpointConfig(endpointConfig);
        try {
            body.setApiDefinition(getApiDefinition());
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
     * This method is used to get the API definition.
     *
     * @return API definition.
     * @throws IOException  throws if an error occurred when creating the API.
     */
    private static String getApiDefinition() throws IOException {
        if (apiDefinition == null) {
            apiDefinition = IOUtils.toString(
                    SampleUtils.class.getClassLoader().getResourceAsStream(Constants.API_DEFINITION_JSON_FILE),
                    StandardCharsets.UTF_8.name());
        }

        return apiDefinition;
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
