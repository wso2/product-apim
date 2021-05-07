package org.wso2.am.integration.test.impl;

import org.wso2.am.integration.clients.gateway.api.ApiClient;
import org.wso2.am.integration.clients.gateway.api.ApiException;
import org.wso2.am.integration.clients.gateway.api.v2.DeployApiApi;
import org.wso2.am.integration.clients.gateway.api.v2.GetApiArtifactsApi;
import org.wso2.am.integration.clients.gateway.api.v2.SubscriptionsApi;
import org.wso2.am.integration.clients.gateway.api.v2.UndeployApiApi;
import org.wso2.am.integration.clients.gateway.api.v2.dto.APIArtifactDTO;
import org.wso2.am.integration.clients.gateway.api.v2.dto.EndpointsDTO;
import org.wso2.am.integration.clients.gateway.api.v2.dto.LocalEntryDTO;
import org.wso2.am.integration.clients.gateway.api.v2.dto.SequencesDTO;
import org.wso2.am.integration.clients.gateway.api.v2.dto.SubscriptionDTO;
import org.wso2.am.integration.clients.gateway.api.v2.dto.SubscriptionListDTO;

import java.nio.charset.StandardCharsets;
import javax.xml.bind.DatatypeConverter;

public class RestAPIGatewayImpl {
    private String tenantDomain;
    GetApiArtifactsApi getApiArtifactsApi = new GetApiArtifactsApi();
    DeployApiApi deployApiApi = new DeployApiApi();
    UndeployApiApi undeployApiApi = new UndeployApiApi();
    SubscriptionsApi subscriptionsApi = new SubscriptionsApi();
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
        subscriptionsApi.setApiClient(apiClient);
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

    public SubscriptionDTO retrieveSubscription(String apiUUID, String applicationUUID) throws ApiException {

        SubscriptionListDTO subscriptions = subscriptionsApi.getSubscriptions(apiUUID, applicationUUID, tenantDomain);
        if (subscriptions != null && subscriptions.getList().size() > 0) {
            return subscriptions.getList().get(0);
        }
        return null;
    }
}
