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
import org.wso2.am.integration.clients.publisher.api.v1.DocumentIndividualApi;
import org.wso2.am.integration.clients.publisher.api.v1.ThrottlingPoliciesApi;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIBusinessInformationDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APICorsConfigurationDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.DocumentDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.WorkflowResponseDTO;
import org.wso2.am.integration.clients.store.api.v1.ApplicationKeysApi;
import org.wso2.am.integration.clients.store.api.v1.ApplicationsApi;
import org.wso2.am.integration.clients.store.api.v1.SubscriptionsApi;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.ClientCertificateCreationBean;
import org.wso2.carbon.apimgt.test.ClientAuthenticator;
import org.wso2.carbon.apimgt.test.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This util class performs the actions related to APIDTOobjects.
 */
public class RestAPIStoreImpl {
    public static ApiIndividualApi apiPublisherApi = new ApiIndividualApi();
    public static ApiCollectionApi apiCollectionApi = new ApiCollectionApi();
    public static ApplicationsApi applicationsApi = new ApplicationsApi();
    public static SubscriptionsApi subscriptionIndividualApi = new SubscriptionsApi();
    public static DocumentIndividualApi documentIndividualApi = new DocumentIndividualApi();
    public static ThrottlingPoliciesApi throttlingPoliciesApi = new ThrottlingPoliciesApi();

    public static ApiClient apiPublisherClient = new ApiClient();
    public static org.wso2.am.integration.clients.store.api.ApiClient apiStoreClient = new org.wso2.am.integration.clients.store.api.ApiClient();
    public static final String appName = "Integration_Test_App";
    public static final String callBackURL = "test.com";
    public static final String tokenScope = "Production";
    public static final String appOwner = "admin";
    public static final String grantType = "client_credentials";
    public static final String dcrEndpoint = "http://127.0.0.1:10263/client-registration/v0.14/register";
    public static final String username = "admin";
    public static final String password = "admin";
    public static final String tenantDomain = "";
    public static final String tokenEndpoint = "https://127.0.0.1:9943/oauth2/token";

    public RestAPIStoreImpl() {

        String accessToken = ClientAuthenticator
                .getAccessToken("apim:api_create apim:api_delete apim:api_publish " +
                                "apim:api_view apim:subscribe apim:subscription_block apim:subscription_view apim:tier_manage apim:tier_view",
                        appName, callBackURL, tokenScope, appOwner, grantType, dcrEndpoint, username, password, tenantDomain, tokenEndpoint);

        apiStoreClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
        apiStoreClient.setBasePath("https://localhost:9943/api/am/store/v1.0");
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
                                           SubscriptionDTO.StatusEnum statusEnum) {
        try {
            SubscriptionDTO subscription = new SubscriptionDTO();
            subscription.setApplicationId(applicationId);
            subscription.setApiId(apiId);
            subscription.setThrottlingPolicy(subscriptionTier);
            subscription.setStatus(statusEnum);
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
}
