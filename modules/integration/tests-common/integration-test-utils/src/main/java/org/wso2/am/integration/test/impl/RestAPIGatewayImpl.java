/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.am.integration.test.impl;

import org.wso2.am.integration.clients.gateway.api.ApiClient;
import org.wso2.am.integration.clients.gateway.api.ApiException;
import org.wso2.am.integration.clients.gateway.api.v2.DeployApiApi;
import org.wso2.am.integration.clients.gateway.api.v2.GetApiArtifactsApi;
import org.wso2.am.integration.clients.gateway.api.v2.GetApiInfoApi;
import org.wso2.am.integration.clients.gateway.api.v2.GetApplicationInfoApi;
import org.wso2.am.integration.clients.gateway.api.v2.GetSubscriptionInfoApi;
import org.wso2.am.integration.clients.gateway.api.v2.SubscriptionsApi;
import org.wso2.am.integration.clients.gateway.api.v2.UndeployApiApi;
import org.wso2.am.integration.clients.gateway.api.v2.dto.APIArtifactDTO;
import org.wso2.am.integration.clients.gateway.api.v2.dto.APIInfoDTO;
import org.wso2.am.integration.clients.gateway.api.v2.dto.ApplicationInfoDTO;
import org.wso2.am.integration.clients.gateway.api.v2.dto.ApplicationListDTO;
import org.wso2.am.integration.clients.gateway.api.v2.dto.EndpointsDTO;
import org.wso2.am.integration.clients.gateway.api.v2.dto.LocalEntryDTO;
import org.wso2.am.integration.clients.gateway.api.v2.dto.SequencesDTO;
import org.wso2.am.integration.clients.gateway.api.v2.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.gateway.api.v2.dto.SubscriptionListDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;

import java.nio.charset.StandardCharsets;
import javax.xml.bind.DatatypeConverter;

public class RestAPIGatewayImpl {
    private String tenantDomain = null;
    GetApiArtifactsApi getApiArtifactsApi = new GetApiArtifactsApi();
    DeployApiApi deployApiApi = new DeployApiApi();
    UndeployApiApi undeployApiApi = new UndeployApiApi();
    GetApiInfoApi apiInfoApi = new GetApiInfoApi();
    GetApplicationInfoApi applicationInfoApi = new GetApplicationInfoApi();
    GetSubscriptionInfoApi subscriptionInfoApi = new GetSubscriptionInfoApi();
    public RestAPIGatewayImpl(String username, String password, String tenantDomain) {
        ApiClient apiClient = new ApiClient();
        String basicEncoded =
                DatatypeConverter.printBase64Binary((username + ':' + password).getBytes(StandardCharsets.UTF_8));
        apiClient.addDefaultHeader("Authorization", "Basic " + basicEncoded);
        apiClient.setDebugging(true);
        apiClient.setBasePath("https://localhost:9943/api/am/gateway/v2");
        apiClient.setReadTimeout(600000);
        apiClient.setConnectTimeout(600000);
        apiClient.setWriteTimeout(600000);
        getApiArtifactsApi.setApiClient(apiClient);
        deployApiApi.setApiClient(apiClient);
        undeployApiApi.setApiClient(apiClient);
        apiInfoApi.setApiClient(apiClient);
        applicationInfoApi.setApiClient(apiClient);
        subscriptionInfoApi.setApiClient(apiClient);
        this.tenantDomain = tenantDomain;
    }

    public APIArtifactDTO retrieveAPI(String name, String version) throws ApiException {
        return getApiArtifactsApi.getAPIArtifacts(name, version, tenantDomain);
    }

    public EndpointsDTO retrieveEndpoints(String name, String version) throws ApiException {
        return getApiArtifactsApi.getEndpoints(name, version, tenantDomain);
    }

    public LocalEntryDTO retrieveLocalEntries(String name, String version) throws ApiException {
        return getApiArtifactsApi.getLocalEntries(name, version, tenantDomain);
    }

    public SequencesDTO retrieveSequences(String name, String version) throws ApiException {
        return getApiArtifactsApi.getSequences(name, version, tenantDomain);
    }

    public SubscriptionDTO retrieveSubscription(String apiId, String applicationId) throws APIManagerIntegrationTestException {

        try {
            return subscriptionInfoApi.subscriptionsGet(apiId, applicationId, tenantDomain);
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return null;
            } else {
                throw new APIManagerIntegrationTestException(e);
            }
        }
    }

    public ApplicationInfoDTO retrieveApplication(String applicationId) throws APIManagerIntegrationTestException {

        try {
            ApplicationListDTO applicationListDTO = applicationInfoApi.applicationsGet(null, applicationId,
                    tenantDomain);
            if (applicationListDTO != null && applicationListDTO.getList() != null) {
                for (ApplicationInfoDTO applicationInfoDTO : applicationListDTO.getList()) {
                    if (applicationInfoDTO.getUuid().equals(applicationId)) {
                        return applicationInfoDTO;
                    }
                }
            }

        } catch (ApiException e) {
            throw new APIManagerIntegrationTestException(e);
        }
        return null;
    }

    public APIInfoDTO getAPIInfo(String apiId) throws APIManagerIntegrationTestException {

        try {
            return apiInfoApi.apisApiIdGet(apiId, tenantDomain);
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return null;
            }else{
                throw new APIManagerIntegrationTestException(e);
            }
        }
    }
}
