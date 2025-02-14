/*
 * Copyright (c) 2019, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the"License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an"AS IS"BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.am.integration.test.impl;

import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.wso2.am.integration.clients.admin.ApiClient;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.ApiResponse;
import org.wso2.am.integration.clients.admin.api.*;
import org.wso2.am.integration.clients.admin.api.dto.*;
import org.wso2.am.integration.test.ClientAuthenticator;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.HttpResponse;

import java.io.File;
import java.util.Arrays;
import java.util.Base64;

/**
 * This util class performs the actions related to APIDTOobjects.
 */
public class RestAPIAdminImpl {

    public ApiClient apiAdminClient = new ApiClient();
    private KeyManagerCollectionApi keyManagerCollectionApi = new KeyManagerCollectionApi();
    private KeyManagerIndividualApi keyManagerIndividualApi = new KeyManagerIndividualApi();
    public WorkflowCollectionApi workflowCollectionApi = new WorkflowCollectionApi();
    public WorkflowsIndividualApi workflowsIndividualApi = new WorkflowsIndividualApi();
    private SettingsApi settingsApi = new SettingsApi();
    private ApiCategoryIndividualApi apiCategoryIndividualApi = new ApiCategoryIndividualApi();
    private ApiCategoryCollectionApi apiCategoryCollectionApi = new ApiCategoryCollectionApi();
    private ApplicationPolicyIndividualApi applicationPolicyIndividualApi = new ApplicationPolicyIndividualApi();
    public ApplicationPolicyCollectionApi applicationPolicyCollectionApi = new ApplicationPolicyCollectionApi();
    private SubscriptionPolicyIndividualApi subscriptionPolicyIndividualApi = new SubscriptionPolicyIndividualApi();
    private SubscriptionPolicyCollectionApi subscriptionPolicyCollectionApi = new SubscriptionPolicyCollectionApi();
    private CustomRulesIndividualApi customRulesIndividualApi = new CustomRulesIndividualApi();
    private CustomRulesCollectionApi customRulesCollectionApi = new CustomRulesCollectionApi();
    private DenyPolicyIndividualApi denyPolicyIndividualApi = new DenyPolicyIndividualApi();
    private DenyPoliciesCollectionApi denyPolicyCollectionApi = new DenyPoliciesCollectionApi();
    private AdvancedPolicyIndividualApi advancedPolicyIndividualApi = new AdvancedPolicyIndividualApi();
    private AdvancedPolicyCollectionApi advancedPolicyCollectionApi = new AdvancedPolicyCollectionApi();
    private ApplicationCollectionApi applicationCollectionApi = new ApplicationCollectionApi();
    private ImportExportThrottlingPolicyApi exportImportThrottlingPolicyApi = new ImportExportThrottlingPolicyApi();
    private ThrottlingPolicySearchApi throttlingPolicySearchApi = new ThrottlingPolicySearchApi();
    private SystemScopesApi systemScopesApi = new SystemScopesApi();
    private ApplicationApi applicationApi = new ApplicationApi();
    private ApiProviderChangeApi apiProviderChangeApi = new ApiProviderChangeApi();
    private LabelApi labelApi = new LabelApi();
    private LabelCollectionApi labelCollectionApi = new LabelCollectionApi();
    private EnvironmentApi environmentApi = new EnvironmentApi();
    private LlmProviderApi llmProviderApi = new LlmProviderApi();
    private LlmProvidersApi llmProvidersApi = new LlmProvidersApi();
    private EnvironmentCollectionApi environmentCollectionApi = new EnvironmentCollectionApi();
    private TenantConfigApi tenantConfigApi = new TenantConfigApi();
    private TenantConfigSchemaApi tenantConfigSchemaApi = new TenantConfigSchemaApi();
    public static final String appName = "Integration_Test_App_Admin";
    public static final String callBackURL = "test.com";
    public static final String tokenScope = "Production";
    public static final String appOwner = "admin";
    public static final String grantType = "password";
    public static final String username = "admin";
    public static final String password = "admin";
    public String tenantDomain;


    public RestAPIAdminImpl(String username, String password, String tenantDomain, String adminURl) {
        // token/DCR of Publisher node itself will be used
        String tokenURL = adminURl + "oauth2/token";
        String dcrURL = adminURl + "client-registration/v0.17/register";
        String scopeList = "openid  " +
                "apim:admin " +
                "apim:tier_view " +
                "apim:tier_manage " +
                "apim:bl_view " +
                "apim:bl_manage " +
                "apim:mediation_policy_view " +
                "apim:mediation_policy_create " +
                "apim:app_owner_change " +
                "apim:app_import_export " +
                "apim:api_import_export " +
                "apim:api_product_import_export " +
                "apim:label_manage " +
                "apim:label_read " +
                "apim:environment_manage " +
                "apim:environment_read " +
                "apim:monetization_usage_publish " +
                "apim:api_workflow_approve " +
                "apim:bot_data " +
                "apim:tenantInfo " +
                "apim:tenant_theme_manage " +
                "apim:admin_operations " +
                "apim:admin_settings " +
                "apim:admin_alert_manage " +
                "apim:api_workflow_view " +
                "apim:api_workflow_approve " +
                "apim:admin_operation " +
                "apim:policies_import_export " +
                "apim:keymanagers_manage " +
                "apim:api_category " +
                "apim:admin_tier_view " +
                "apim:admin_tier_manage " +
                "apim:scope_manage";

        String accessToken = ClientAuthenticator
                .getAccessToken(scopeList,
                        appName, callBackURL, tokenScope, appOwner, grantType, dcrURL, username, password, tenantDomain,
                        tokenURL);

        apiAdminClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
        apiAdminClient.setBasePath(adminURl + "api/am/admin/v4");
        apiAdminClient.setDebugging(true);
        apiAdminClient.setReadTimeout(600000);
        apiAdminClient.setConnectTimeout(600000);
        apiAdminClient.setWriteTimeout(600000);
        keyManagerCollectionApi.setApiClient(apiAdminClient);
        keyManagerIndividualApi.setApiClient(apiAdminClient);
        settingsApi.setApiClient(apiAdminClient);
        applicationPolicyIndividualApi.setApiClient(apiAdminClient);
        applicationPolicyCollectionApi.setApiClient(apiAdminClient);
        subscriptionPolicyIndividualApi.setApiClient(apiAdminClient);
        subscriptionPolicyCollectionApi.setApiClient(apiAdminClient);
        customRulesIndividualApi.setApiClient(apiAdminClient);
        customRulesCollectionApi.setApiClient(apiAdminClient);
        denyPolicyCollectionApi.setApiClient(apiAdminClient);
        denyPolicyIndividualApi.setApiClient(apiAdminClient);
        advancedPolicyIndividualApi.setApiClient(apiAdminClient);
        advancedPolicyCollectionApi.setApiClient(apiAdminClient);
        exportImportThrottlingPolicyApi.setApiClient(apiAdminClient);
        throttlingPolicySearchApi.setApiClient(apiAdminClient);
        applicationCollectionApi.setApiClient(apiAdminClient);
        applicationApi.setApiClient(apiAdminClient);
        labelApi.setApiClient(apiAdminClient);
        labelCollectionApi.setApiClient(apiAdminClient);
        environmentApi.setApiClient(apiAdminClient);
        llmProviderApi.setApiClient(apiAdminClient);
        llmProvidersApi.setApiClient(apiAdminClient);
        environmentCollectionApi.setApiClient(apiAdminClient);
        workflowCollectionApi.setApiClient(apiAdminClient);
        workflowsIndividualApi.setApiClient(apiAdminClient);
        apiCategoryCollectionApi.setApiClient(apiAdminClient);
        apiCategoryIndividualApi.setApiClient(apiAdminClient);
        systemScopesApi.setApiClient(apiAdminClient);
        tenantConfigApi.setApiClient(apiAdminClient);
        tenantConfigSchemaApi.setApiClient(apiAdminClient);
        apiProviderChangeApi.setApiClient(apiAdminClient);
        this.tenantDomain = tenantDomain;
    }

    /**
     * This method is used to get a list throttling policy details
     *
     * @param query Filters by throttling policy type
     * @return Throttling Policy details list
     * @throws ApiException Throws if an error occurred while getting throttling policy details
     */
    public ThrottlePolicyDetailsListDTO getThrottlePolicies (String query) throws ApiException {

        return throttlingPolicySearchApi.throttlingPolicySearch(query);
    }

    /**
     * This method is used to export a Throttling Policy
     *
     * @param policyName Throttling Policy name to be exported
     * @param policyType Throttling Policy type
     * @return ExportThrottlePolicyApi response returned by the API call
     * @throws ApiException Throws if an error occurred while exporting Throttling policy
     */
    public ApiResponse<ExportThrottlePolicyDTO> exportThrottlePolicy(String policyName, String policyType)
            throws ApiException {

        return exportImportThrottlingPolicyApi.exportThrottlingPolicyWithHttpInfo(null, policyName, policyType);
    }

    /**
     * This method is used to import a Throttling Policy
     *
     * @param file      Exported throttling policy file
     * @param overwrite overwrites already existing throttling policy
     * @return ImportThrottlePolicyApi response returned by the API call
     * @throws ApiException Throws if an error occurred while importing Throttling policy
     */
    public ApiResponse<Void> importThrottlePolicy(File file, Boolean overwrite) throws ApiException {

        return exportImportThrottlingPolicyApi.importThrottlingPolicyWithHttpInfo(file, overwrite);
    }

    /***
     * This method is used to add an API category.
     *
     * @param apiCategoryDTO API category DTO to be added
     * @return API response returned by the API call.
     * @throws ApiException Throws if an error occurred while adding the new API category.
     */
    public ApiResponse<APICategoryDTO> addApiCategory(APICategoryDTO apiCategoryDTO) throws ApiException {

        return apiCategoryIndividualApi.apiCategoriesPostWithHttpInfo(apiCategoryDTO);
    }

    /***
     * This method is used to update an API category
     *
     * @param uuid           UUID of the API category to be updated
     * @param apiCategoryDTO API category DTO to be updated
     * @return API response returned by the API call.
     * @throws ApiException  Throws if an error occurred while updating the new API category.
     */
    public ApiResponse<APICategoryDTO> updateApiCategory(String uuid, APICategoryDTO apiCategoryDTO)
            throws ApiException {

        return apiCategoryIndividualApi.apiCategoriesApiCategoryIdPutWithHttpInfo(uuid, apiCategoryDTO);
    }

    /**
     * This method is used to retrieve all API categories.
     *
     * @return API response returned by the API call.
     * @throws ApiException Throws if an error occurred while retrieving all API categories.
     */
    public ApiResponse<APICategoryListDTO> getApiCategories() throws ApiException {

        return apiCategoryCollectionApi.apiCategoriesGetWithHttpInfo();
    }

    /**
     * This method is used to delete an API category.
     *
     * @param uuid uuid of the API category to be deleted.
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while deleting the API category.
     */
    public ApiResponse<Void> deleteApiCategory(String uuid) throws ApiException {

        return apiCategoryIndividualApi.apiCategoriesApiCategoryIdDeleteWithHttpInfo(uuid, null, null);
    }

    /**
     * Retrieves a list of LLM Providers.
     *
     * @return ApiResponse containing a list of LLMProviderSummaryResponseListDTO with details about available LLM providers.
     * @throws ApiException if there is an error during the API call.
     */
    public ApiResponse<LLMProviderSummaryResponseListDTO> getLLMProviders() throws ApiException {
        return llmProvidersApi.llmProvidersGetWithHttpInfo();
    }

    /**
     * Retrieves details of a specific LLM Provider by its ID.
     *
     * @param llmProviderId The unique identifier of the LLM provider.
     * @return ApiResponse containing LLMProviderResponseDTO with details of the specified LLM provider.
     * @throws ApiException if there is an error during the API call.
     */
    public ApiResponse<LLMProviderResponseDTO> getLLMProvider(String llmProviderId) throws ApiException {
        return llmProviderApi.llmProvidersLlmProviderIdGetWithHttpInfo(llmProviderId);
    }

    /**
     * Add the details of a specific LLM Provider.
     *
     * @param name          The new name of the LLM provider.
     * @param apiVersion    The API version of the LLM provider.
     * @param description   A brief description of the LLM provider.
     * @param configuration Configuration details for the LLM provider.
     * @param apiDefinition The API definition file for the LLM provider.
     * @param modelList     The list of models for the LLM provider.
     * @return ApiResponse containing LLMProviderResponseDTO with the updated details of the LLM provider.
     * @throws ApiException if there is an error during the API call.
     */
    public ApiResponse<LLMProviderResponseDTO> addLLMProvider(String name, String apiVersion, String description,
            String configuration, File apiDefinition, String modelList) throws ApiException {

        return llmProvidersApi.llmProvidersPostWithHttpInfo(name, apiVersion, description, configuration, apiDefinition,
                modelList);
    }

    /**
     * Updates the details of a specific LLM Provider by its ID.
     *
     * @param llmProviderId The unique identifier of the LLM provider.
     * @param name          The new name of the LLM provider.
     * @param apiVersion    The API version of the LLM provider.
     * @param description   A brief description of the LLM provider.
     * @param configuration Configuration details for the LLM provider.
     * @param apiDefinition The API definition file for the LLM provider.
     * @param modelList     The list of models for the LLM provider.
     * @return ApiResponse containing LLMProviderResponseDTO with the updated details of the LLM provider.
     * @throws ApiException if there is an error during the API call.
     */
    public ApiResponse<LLMProviderResponseDTO> updateLLMProvider(String llmProviderId, String name, String apiVersion,
            String description, String configuration, File apiDefinition, String modelList) throws ApiException {
        return llmProviderApi.llmProvidersLlmProviderIdPutWithHttpInfo(llmProviderId, name, apiVersion, description,
                configuration, apiDefinition, modelList);
    }

    /**
     * Deletes a specific LLM Provider by its ID.
     *
     * @param llmProviderId The unique identifier of the LLM provider to be deleted.
     * @return ApiResponse containing void, indicating successful deletion of the LLM provider.
     * @throws ApiException if there is an error during the API call.
     */
    public ApiResponse<Void> deleteLLMProvider(String llmProviderId) throws ApiException {
        return llmProviderApi.llmProvidersLlmProviderIdDeleteWithHttpInfo(llmProviderId);
    }

    /***
     * This method is used to put an system scopes mapping.
     *
     * @param roleAliasListDTO Role Alias DTO to be updated
     * @return API response returned by the API call.
     * @throws ApiException Throws if an error occurred while adding the new API category.
     */

    public ApiResponse<RoleAliasListDTO> putRoleAliases(RoleAliasListDTO roleAliasListDTO) throws ApiException {

        return systemScopesApi.systemScopesRoleAliasesPutWithHttpInfo(roleAliasListDTO);
    }

    /***
     * This method is used to get role aliases.
     *
     * @return API response returned by the API call.
     * @throws ApiException Throws if an error occurred while adding the new API category.
     */

    public ApiResponse<RoleAliasListDTO> getRoleAliases() throws ApiException {

        return systemScopesApi.systemScopesRoleAliasesGetWithHttpInfo();
    }

    public ApiResponse<KeyManagerDTO> addKeyManager(KeyManagerDTO keyManagerDTO) throws ApiException {

        return keyManagerCollectionApi.keyManagersPostWithHttpInfo(keyManagerDTO);
    }

    public KeyManagerListDTO getKeyManagers() throws ApiException {

        return keyManagerCollectionApi.keyManagersGet();
    }

    public ApiResponse<KeyManagerDTO> getKeyManager(String uuid) throws ApiException {

        return keyManagerIndividualApi.keyManagersKeyManagerIdGetWithHttpInfo(uuid);
    }

    public ApiResponse<KeyManagerDTO> updateKeyManager(String uuid, KeyManagerDTO keyManagerDTO) throws ApiException {

        return keyManagerIndividualApi.keyManagersKeyManagerIdPutWithHttpInfo(uuid, keyManagerDTO);
    }

    public ApiResponse<Void> deleteKeyManager(String uuid) throws ApiException {

        return keyManagerIndividualApi.keyManagersKeyManagerIdDeleteWithHttpInfo(uuid);
    }

    public SettingsDTO getSettings() throws ApiException {

        return settingsApi.settingsGet();
    }

    /**
     * This method is used to create an application throttling policy.
     *
     * @param applicationThrottlePolicyDTO Application throttling policy DTO to be added.
     * @return API response returned by the API call.
     * @throws ApiException if an error occurs while creating the application throttling policy.
     */
    public ApiResponse<ApplicationThrottlePolicyDTO> addApplicationThrottlingPolicy(
            ApplicationThrottlePolicyDTO applicationThrottlePolicyDTO) throws ApiException {

        return applicationPolicyCollectionApi
                .throttlingPoliciesApplicationPostWithHttpInfo(Constants.APPLICATION_JSON,
                        applicationThrottlePolicyDTO);
    }

    /**
     * This method is used to retrieve an application throttling policy.
     *
     * @return API response returned by the API call.
     * @throws ApiException if an error occurs while retrieving the application throttling policy.
     */
    public ApiResponse<ApplicationThrottlePolicyDTO> getApplicationThrottlingPolicy(String policyId)
            throws ApiException {

        return applicationPolicyIndividualApi
                .throttlingPoliciesApplicationPolicyIdGetWithHttpInfo(policyId, null, null);
    }

    /**
     * This method is used to update an application throttling policy.
     *
     * @param policyId                     Policy Id of the application throttling policy to be updated.
     * @param applicationThrottlePolicyDTO Application throttling policy DTO which contains the update content.
     * @return API response returned by the API call.
     * @throws ApiException if an error occurs while updating the application throttling policy.
     */
    public ApiResponse<ApplicationThrottlePolicyDTO> updateApplicationThrottlingPolicy(String policyId,
            ApplicationThrottlePolicyDTO applicationThrottlePolicyDTO) throws ApiException {

        return applicationPolicyIndividualApi
                .throttlingPoliciesApplicationPolicyIdPutWithHttpInfo(policyId, Constants.APPLICATION_JSON,
                        applicationThrottlePolicyDTO, null, null);
    }

    /**
     * This method is used to delete an application throttling policy.
     *
     * @param policyId Policy Id of the application throttling policy to be deleted.
     * @return API response returned by the API call.
     * @throws ApiException if an error occurs while deleting the application throttling policy.
     */
    public ApiResponse<Void> deleteApplicationThrottlingPolicy(String policyId) throws ApiException {

        return applicationPolicyIndividualApi
                .throttlingPoliciesApplicationPolicyIdDeleteWithHttpInfo(policyId, null, null);
    }

    /**
     * This method is used to create a subscription throttling policy.
     *
     * @param subscriptionThrottlePolicyDTO Subscription throttling policy DTO to be added.
     * @return API response returned by the API call.
     * @throws ApiException Throws if an error occurred while creating the new subscription throttling policy.
     */
    public ApiResponse<SubscriptionThrottlePolicyDTO> addSubscriptionThrottlingPolicy(
            SubscriptionThrottlePolicyDTO subscriptionThrottlePolicyDTO) throws ApiException {

        return subscriptionPolicyCollectionApi
                .throttlingPoliciesSubscriptionPostWithHttpInfo(Constants.APPLICATION_JSON,
                        subscriptionThrottlePolicyDTO);

    }

    /**
     * This method is used to retrieve a subscription throttling policy.
     *
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while retrieving the subscription throttling policy.
     */
    public ApiResponse<SubscriptionThrottlePolicyDTO> getSubscriptionThrottlingPolicy(String policyId)
            throws ApiException {

        return subscriptionPolicyIndividualApi
                .throttlingPoliciesSubscriptionPolicyIdGetWithHttpInfo(policyId, null, null);
    }

    /**
     * This method is used to update a subscription throttling policy.
     *
     * @param policyId                      Policy Id of the subscription throttling policy to be updated.
     * @param subscriptionThrottlePolicyDTO Subscription throttling policy DTO which contains the updated content.
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while updating the subscription throttling policy.
     */
    public ApiResponse<SubscriptionThrottlePolicyDTO> updateSubscriptionThrottlingPolicy(String policyId,
            SubscriptionThrottlePolicyDTO subscriptionThrottlePolicyDTO) throws ApiException {

        return subscriptionPolicyIndividualApi
                .throttlingPoliciesSubscriptionPolicyIdPutWithHttpInfo(policyId,
                        Constants.APPLICATION_JSON, subscriptionThrottlePolicyDTO, null, null);
    }

    /**
     * This method is used to delete a subscription throttling policy.
     *
     * @param policyId Subscription throttling policy Id.
     * @return API response returned by the API call.
     * @throws ApiException Throws if an error occurred while deleting the subscription throttling policy.
     */
    public ApiResponse<Void> deleteSubscriptionThrottlingPolicy(String policyId) throws ApiException {

        return subscriptionPolicyIndividualApi
                .throttlingPoliciesSubscriptionPolicyIdDeleteWithHttpInfo(policyId, null, null);
    }

    /**
     * This method is used to create an custom throttling policy.
     *
     * @param customRuleDTO Custom throttling policy DTO to be added.
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while creating the custom throttling policy.
     */
    public ApiResponse<CustomRuleDTO> addCustomThrottlingPolicy(CustomRuleDTO customRuleDTO) throws ApiException {

        return customRulesCollectionApi
                .throttlingPoliciesCustomPostWithHttpInfo(Constants.APPLICATION_JSON, customRuleDTO);
    }

    /**
     * This method is used to retrieve a custom throttling policy.
     *
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while retrieving the custom throttling policy.
     */
    public ApiResponse<CustomRuleDTO> getCustomThrottlingPolicy(String policyId) throws ApiException {

        return customRulesIndividualApi
                .throttlingPoliciesCustomRuleIdGetWithHttpInfo(policyId, null, null);
    }

    /**
     * This method is used to update a custom throttling policy.
     *
     * @param policyId      Policy Id of the custom throttling policy to be updated.
     * @param customRuleDTO Custom throttling policy DTO which contains the updated content.
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while updating the custom throttling policy.
     */
    public ApiResponse<CustomRuleDTO> updateCustomThrottlingPolicy(String policyId, CustomRuleDTO customRuleDTO)
            throws ApiException {

        return customRulesIndividualApi.throttlingPoliciesCustomRuleIdPutWithHttpInfo(policyId,
                Constants.APPLICATION_JSON, customRuleDTO, null, null);
    }

    /**
     * This method is used to delete a custom throttling policy.
     *
     * @param policyId Policy Id of the custom throttling policy to be deleted.
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while deleting the custom throttling policy.
     */
    public ApiResponse<Void> deleteCustomThrottlingPolicy(String policyId) throws ApiException {

        return customRulesIndividualApi.throttlingPoliciesCustomRuleIdDeleteWithHttpInfo(policyId, null, null);
    }

    /**
     * Creates an deny throttling policy.
     *
     * @param denyPolicyDTO deny throttling policy DTO to be added.
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while creating the deny throttling policy.
     */
    public ApiResponse<BlockingConditionDTO> addDenyThrottlingPolicy(BlockingConditionDTO denyPolicyDTO) throws ApiException {

        return denyPolicyCollectionApi
                .throttlingDenyPoliciesPostWithHttpInfo(Constants.APPLICATION_JSON, denyPolicyDTO);
    }

    /**
     * Updates an deny throttling policy.
     *
     * @param conditionId policy id of the deny throttling policy to be updated.
     * @param conditionType condition type of the deny throttling policy to be.
     * @param blockingConditionStatusDTO deny throttling policy status DTO to be updated.
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while creating the deny throttling policy.
     */
    public ApiResponse<BlockingConditionDTO> updateDenyThrottlingPolicy(String conditionId, String conditionType, BlockingConditionStatusDTO blockingConditionStatusDTO) throws ApiException {

        return denyPolicyIndividualApi
                .throttlingDenyPolicyConditionIdPatchWithHttpInfo(conditionId,conditionType,blockingConditionStatusDTO,
                        null,
                        null);
    }

    /**
     * Retrieves a deny throttling policy.
     *
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while retrieving the deny throttling policy.
     */
    public ApiResponse<BlockingConditionDTO> getDenyThrottlingPolicy(String policyId) throws ApiException {

        return denyPolicyIndividualApi
                .throttlingDenyPolicyConditionIdGetWithHttpInfo(policyId, null, null);
    }

    /**
     * This method is used to retrieve blocking conditions by condition type and condition value.
     *
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while retrieving the blocking conditions.
     */
    public BlockingConditionListDTO getBlockingConditionsByConditionTypeAndValue(String query) throws ApiException {

        return denyPolicyCollectionApi.throttlingDenyPoliciesGet(Constants.APPLICATION_JSON, null, null, query);
    }


    /**
     * Deletes a deny throttling policy.
     *
     * @param policyId policy id of the deny throttling policy to be deleted.
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while deleting the deny throttling policy.
     */
    public ApiResponse<Void> deleteDenyThrottlingPolicy(String policyId) throws ApiException {

        return denyPolicyIndividualApi.throttlingDenyPolicyConditionIdDeleteWithHttpInfo(policyId, null, null);
    }

    /**
     * This method is used to create an advanced throttling policy.
     *
     * @param advancedThrottlePolicyDTO Advanced throttling policy DTO to be added.
     * @return API response returned by the API call.
     * @throws ApiException Throws if an error occurred while creating the new advanced policy.
     */
    public ApiResponse<AdvancedThrottlePolicyDTO> addAdvancedThrottlingPolicy(
            AdvancedThrottlePolicyDTO advancedThrottlePolicyDTO) throws ApiException {

        return advancedPolicyCollectionApi.throttlingPoliciesAdvancedPostWithHttpInfo(
                Constants.APPLICATION_JSON, advancedThrottlePolicyDTO);

    }

    /**
     * This method is used to retrieve an advanced throttling policy.
     *
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while retrieving the advanced throttling policy.
     */
    public ApiResponse<AdvancedThrottlePolicyDTO> getAdvancedThrottlingPolicy(String policyId) throws ApiException {

        return advancedPolicyIndividualApi.throttlingPoliciesAdvancedPolicyIdGetWithHttpInfo(policyId, null, null);
    }

    /**
     * This method is used to update an advanced throttling policy.
     *
     * @param policyId                  Policy Id of the advanced throttling policy to be updated.
     * @param advancedThrottlePolicyDTO Advanced throttling policy DTO which contains the updated content.
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while updating the advanced throttling policy.
     */
    public ApiResponse<AdvancedThrottlePolicyDTO> updateAdvancedThrottlingPolicy(String policyId,
            AdvancedThrottlePolicyDTO advancedThrottlePolicyDTO) throws ApiException {

        return advancedPolicyIndividualApi
                .throttlingPoliciesAdvancedPolicyIdPutWithHttpInfo(policyId, Constants.APPLICATION_JSON,
                        advancedThrottlePolicyDTO, null, null);
    }

    /**
     * This method is used to delete an advanced throttling policy.
     *
     * @param policyId Policy Id of the advanced throttling policy to be deleted.
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while deleting the advanced throttling policy.
     */
    public ApiResponse<Void> deleteAdvancedThrottlingPolicy(String policyId) throws ApiException {

        return advancedPolicyIndividualApi.throttlingPoliciesAdvancedPolicyIdDeleteWithHttpInfo(policyId, null, null);
    }

    /**
     * This method is used to add a label.
     *
     * @param labelDTO Label DTO to be added.
     * @return API response returned by the API call.
     * @throws ApiException Throws if an error occurred while adding the new label.
     */
    public ApiResponse<LabelDTO> addLabel(LabelDTO labelDTO) throws ApiException {

        return labelApi.labelsPostWithHttpInfo(labelDTO);
    }

    /**
     * This method is used to retrieve all labels.
     *
     * @return API response returned by the API call.
     * @throws ApiException Throws if an error occurred while retrieving all labels.
     */
    public ApiResponse<LabelListDTO> getLabels() throws ApiException {

        return labelCollectionApi.labelsGetWithHttpInfo();
    }

    /**
     * This method is used to update a label.
     *
     * @param labelId  Label Id of the label to be updated.
     * @param labelDTO Label DTO which contains the updated content.
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while updating the label.
     */
    public ApiResponse<LabelDTO> updateLabel(String labelId, LabelDTO labelDTO) throws ApiException {

        return labelApi.labelsLabelIdPutWithHttpInfo(labelId, labelDTO);
    }

    /**
     * This method is used to delete a label.
     *
     * @param labelId Label Id of the label to be updated.
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while deleting the label.
     */
    public ApiResponse<Void> deleteLabel(String labelId) throws ApiException {

        return labelApi.labelsLabelIdDeleteWithHttpInfo(labelId, null, null);
    }

    /**
     * This method is used to add an environment.
     *
     * @param environmentDTO Environment DTO to be added.
     * @return API response returned by the API call.
     * @throws ApiException Throws if an error occurred while adding the new environment.
     */
    public ApiResponse<EnvironmentDTO> addEnvironment(EnvironmentDTO environmentDTO) throws ApiException {

        return environmentApi.environmentsPostWithHttpInfo(environmentDTO);
    }

    /**
     * This method is used to retrieve all environments.
     *
     * @return API response returned by the API call.
     * @throws ApiException Throws if an error occurred while retrieving all environments of tenant.
     */
    public ApiResponse<EnvironmentListDTO> getEnvironments() throws ApiException {

        return environmentCollectionApi.environmentsGetWithHttpInfo();
    }

    /**
     * This method is used to update an environment.
     *
     * @param environmentId Environment Id of the label to be updated.
     * @param environmentDTO Environment DTO which contains the updated content.
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while updating the environment.
     */
    public ApiResponse<EnvironmentDTO> updateEnvironment(String environmentId, EnvironmentDTO environmentDTO)
            throws ApiException {

        return environmentApi.environmentsEnvironmentIdPutWithHttpInfo(environmentId, environmentDTO);
    }

    /**
     * This method is used to delete an environment.
     *
     * @param environmentId Environment Id of the label to be deleted.
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while deleting the environment.
     */
    public ApiResponse<Void> deleteEnvironment(String environmentId) throws ApiException {

        return environmentApi.environmentsEnvironmentIdDeleteWithHttpInfo(environmentId);
    }
    
    /**
     * This method is used to retrieve applications.
     *
     * @param user            Username of the application creator.
     * @param limit           Maximum number of applications to return.
     * @param offset          Starting point within the complete list of applications qualified.
     * @param appTenantDomain Tenant domain of the applications to get.
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while retrieving applications.
     */
    public ApiResponse<ApplicationListDTO> getApplications(String user, Integer limit, Integer offset,
            String appTenantDomain, String name) throws ApiException {

        return applicationCollectionApi.applicationsGetWithHttpInfo(user, limit, offset, null,
                null, name, appTenantDomain);
    }

    /**
     * This method is used to change the owner of an application.
     *
     * @param newOwner      New owner of the application.
     * @param applicationId Application ID of the application.
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while changing the owner of an application.
     */
    public ApiResponse<Void> changeApplicationOwner(String newOwner, String applicationId) throws ApiException {

        return applicationApi.applicationsApplicationIdChangeOwnerPostWithHttpInfo(newOwner, applicationId);
    }


    public ApiResponse<Void> changeApiProvider(String newProvider, String apiId) throws ApiException {
        return apiProviderChangeApi.providerNamePostWithHttpInfo(newProvider, apiId);
    }
  
    /**
     * This method is used to retrieve scopes for a particular user.
     *
     * @param scopeName Scope name.
     * @param username  Username of the user.
     * @return ScopeSettingsDTO returned by API call.
     * @throws ApiException if an error occurs while retrieving the scopes of a particular user.
     */
    public ScopeSettingsDTO retrieveScopesForParticularUser(String scopeName, String username) throws ApiException {
        return systemScopesApi.systemScopesScopeNameGet(new String(Base64.getEncoder().encode(scopeName.getBytes())), username);
    }

    /**
     * This method is used to add a new role alias mapping for system scope roles.
     *
     * @param count   The number of role aliases.
     * @param role    Name of the role.
     * @param aliases List of aliases.
     * @return RoleAliasListDTO returned by API call.
     * @throws ApiException if an error occurs while adding role aliases mappings for system scope roles.
     */
    public RoleAliasListDTO addRoleAliasMappingForSystemScopeRoles(int count, String role, String[] aliases) throws ApiException {

        RoleAliasDTO roleAliasDTO = new RoleAliasDTO();
        roleAliasDTO.setRole(role);
        roleAliasDTO.setAliases(Arrays.asList(aliases));

        RoleAliasListDTO roleAliasListDTO = new RoleAliasListDTO();
        roleAliasListDTO.setCount(count);
        roleAliasListDTO.setList(Arrays.asList(roleAliasDTO));

        return systemScopesApi.systemScopesRoleAliasesPut(roleAliasListDTO);
    }

    public HttpResponse getWorkflowByExternalWorkflowReference(String externalWorkflowRef) throws ApiException {
        WorkflowInfoDTO workflowInfodto = null;
        HttpResponse response = null;
        Gson gson = new Gson();
        try {
            workflowInfodto = workflowsIndividualApi.workflowsExternalWorkflowRefGet(externalWorkflowRef, null);
        } catch (ApiException e) {
            return new HttpResponse(gson.toJson(e.getResponseBody()), e.getCode());
        }
        if (StringUtils.isNotEmpty(workflowInfodto.getReferenceId())) {
            response = new HttpResponse(gson.toJson(workflowInfodto), 200);
        }
        return response;
    }

    public HttpResponse getWorkflows(String workflowType) throws ApiException {
        WorkflowListDTO workflowListdto = null;
        HttpResponse response = null;
        Gson gson = new Gson();
        try {
            workflowListdto = workflowCollectionApi.workflowsGet(null, null, null, null, workflowType);
            response = new HttpResponse(gson.toJson(workflowListdto), 200);
        } catch (ApiException e) {
            return new HttpResponse(gson.toJson(e.getResponseBody()), e.getCode());
        }
        return response;
    }

    public HttpResponse updateWorkflowStatus(String workflowReferenceId) throws ApiException {
        WorkflowDTO workflowdto = null;
        HttpResponse response = null;
        Gson gson = new Gson();

        WorkflowDTO body = new WorkflowDTO();
        WorkflowDTO.StatusEnum status = WorkflowDTO.StatusEnum.valueOf(WorkflowDTO.StatusEnum.class, "APPROVED");
        body.setStatus(status);
        body.setDescription("Approve workflow request.");
        //body.setAttributes();
        try {
            workflowdto = workflowsIndividualApi.workflowsUpdateWorkflowStatusPost(workflowReferenceId, body);
            response = new HttpResponse(gson.toJson(workflowdto), 200);
        } catch (ApiException e) {
            return new HttpResponse(gson.toJson(e.getResponseBody()), e.getCode());
        }
        return response;
    }

    /**
     * This method is used to reject a workflow
     *
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while rejecting a workflow
     */
    public HttpResponse rejectWorkflowStatus(String workflowReferenceId) throws ApiException {
        WorkflowDTO workflowdto = null;
        HttpResponse response = null;
        Gson gson = new Gson();

        WorkflowDTO body = new WorkflowDTO();
        WorkflowDTO.StatusEnum status = WorkflowDTO.StatusEnum.valueOf(WorkflowDTO.StatusEnum.class, "REJECTED");
        body.setStatus(status);
        body.setDescription("Reject workflow request.");
        //body.setAttributes();
        try {
            workflowdto = workflowsIndividualApi.workflowsUpdateWorkflowStatusPost(workflowReferenceId, body);
            response = new HttpResponse(gson.toJson(workflowdto), 200);
        } catch (ApiException e) {
            return new HttpResponse(gson.toJson(e.getResponseBody()), e.getCode());
        }
        return response;
    }

    /**
     * This method is used to retrieve tenant Config.
     *
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while retrieving tenant Config.
     */
    public String getTenantConfig() throws ApiException {
        return tenantConfigApi.exportTenantConfig();
    }

    /**
     * This method is used to update tenant config.
     *
     * @param tenantConf Tenant Configuration.
     * @return API response returned by API call.
     * @throws ApiException if an error occurs updating the tenant conf.
     */
    public Object updateTenantConfig(Object tenantConf) throws ApiException {
        return tenantConfigApi.updateTenantConfig(tenantConf);
    }

    /**
     * This method is used to retrieve tenant Config Schema.
     *
     * @return API response returned by API call.
     * @throws ApiException if an error occurs while retrieving tenant Config schema.
     */
    public Object getTenantConfigSchema() throws ApiException {
        return tenantConfigSchemaApi.exportTenantConfigSchema();
    }


    public WorkflowListDTO getWorkflowsByWorkflowType(String workflowType) throws ApiException {
        return workflowCollectionApi.workflowsGet(null, null, null, null, workflowType);
    }
}
