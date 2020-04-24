/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.wso2.am.integration.clients.admin.api.v1.*;
import org.wso2.am.integration.clients.admin.api.ApiClient;
import org.wso2.am.integration.clients.admin.api.v1.dto.WorkflowDTO;
import org.wso2.am.integration.clients.admin.api.v1.dto.WorkflowInfoDTO;
import org.wso2.am.integration.clients.admin.api.ApiException;
import org.wso2.am.integration.clients.admin.api.v1.dto.WorkflowListDTO;
import org.wso2.am.integration.test.ClientAuthenticator;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

/**
 * This util class performs the actions related to WorkflowDTOobjects.
 */
public class RestAPIAdminImpl {

    public ApiCategoryCollectionApi apiCategoryCollection = new ApiCategoryCollectionApi();
    public ApiCategoryIndividualApi individualCategory = new ApiCategoryIndividualApi();
    public ApiIndividualApi individualApi = new ApiIndividualApi();
    public ApplicationApi applicationApi = new ApplicationApi();
    public ApplicationCollectionApi applicationCollectionApi = new ApplicationCollectionApi();
    public ApplicationIndividualApi individualApplicationApi = new ApplicationIndividualApi();
    public ApplicationPolicyCollectionApi applicationPolicyCollectionApi = new ApplicationPolicyCollectionApi();
    public ApplicationPolicyIndividualApi applicationPolicyIndividualApi = new ApplicationPolicyIndividualApi();
    public BlacklistCollectionApi blacklistCollectionApi = new BlacklistCollectionApi();
    public BlacklistIndividualApi blacklistIndividualApi = new BlacklistIndividualApi();
    public CustomRulesCollectionApi customRulesCollectionApi = new CustomRulesCollectionApi();
    public CustomRulesIndividualApi customRulesIndividualApi = new CustomRulesIndividualApi();
    public DefaultApi defaultApi = new DefaultApi();
    public LabelApi labelApi = new LabelApi();
    public LabelCollectionApi labelCollectionApi = new LabelCollectionApi();
    public MediationPolicyCollectionApi mediationPolicyCollectionApi = new MediationPolicyCollectionApi();
    public MediationPolicyIndividualApi mediationPolicyIndividualApi = new MediationPolicyIndividualApi();
    public MonetizationCollectionApi monetizationCollectionApi = new MonetizationCollectionApi();
    public SubscriptionPolicyCollectionApi subscriptionPolicyCollectionApi = new SubscriptionPolicyCollectionApi();
    public SubscriptionPolicyIndividualApi subscriptionPolicyIndividualApi = new SubscriptionPolicyIndividualApi();
    public TenantsApi tenantsApi = new TenantsApi();
    public WorkflowCollectionApi workflowCollectionApi = new WorkflowCollectionApi();
    public WorkflowsIndividualApi workflowsIndividualApi = new WorkflowsIndividualApi();

    public ApiClient apiAdminClient = new ApiClient();
    public static final String appName = "Integration_Test_App_Publisher";
    public static final String callBackURL = "test.com";
    public static final String tokenScope = "Production";
    public static final String appOwner = "admin";
    public static final String grantType = "password";
    public static final String username = "admin";
    public static final String password = "admin";
    public String tenantDomain;

    @Deprecated
    public RestAPIAdminImpl() {
        this(username, password, "", "https://localhost:9943");
    }

    public RestAPIAdminImpl(String username, String password, String tenantDomain, String adminURL) {
        // token/DCR of Publisher node itself will be used
        String tokenURL = adminURL + "oauth2/token";
        String dcrURL = adminURL + "client-registration/v0.16/register";
        String accessToken = ClientAuthenticator
                .getAccessToken("openid apim:tier_view apim:tier_manage apim:bl_view apim:bl_manage " +
                                "apim:mediation_policy_view apim:mediation_policy_create apim:app_owner_change " +
                                "apim:app_import_export apim:api_import_export  apim:label_manage apim:label_read " +
                                "apim:monetization_usage_publish apim:api_workflow apim:bot_data apim:tenantInfo " +
                                "apim:admin_operations",
                        appName, callBackURL, tokenScope, appOwner, grantType, dcrURL, username, password, tenantDomain, tokenURL);

        apiAdminClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
        apiAdminClient.setBasePath(adminURL + "api/am/admin/v0.16");
        workflowCollectionApi.setApiClient(apiAdminClient);
        workflowsIndividualApi.setApiClient(apiAdminClient);
        tenantsApi.setApiClient(apiAdminClient);
        subscriptionPolicyIndividualApi.setApiClient(apiAdminClient);
        subscriptionPolicyCollectionApi.setApiClient(apiAdminClient);
        monetizationCollectionApi.setApiClient(apiAdminClient);
        mediationPolicyIndividualApi.setApiClient(apiAdminClient);
        mediationPolicyCollectionApi.setApiClient(apiAdminClient);
        labelCollectionApi.setApiClient(apiAdminClient);
        labelApi.setApiClient(apiAdminClient);
        defaultApi.setApiClient(apiAdminClient);
        customRulesIndividualApi.setApiClient(apiAdminClient);
        customRulesCollectionApi.setApiClient(apiAdminClient);
        blacklistIndividualApi.setApiClient(apiAdminClient);
        blacklistCollectionApi.setApiClient(apiAdminClient);
        applicationPolicyIndividualApi.setApiClient(apiAdminClient);
        individualApplicationApi.setApiClient(apiAdminClient);
        applicationCollectionApi.setApiClient(apiAdminClient);
        applicationPolicyIndividualApi.setApiClient(apiAdminClient);
        applicationApi.setApiClient(apiAdminClient);
        individualApi.setApiClient(apiAdminClient);
        individualCategory.setApiClient(apiAdminClient);
        apiCategoryCollection.setApiClient(apiAdminClient);
        this.tenantDomain = tenantDomain;
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
}
