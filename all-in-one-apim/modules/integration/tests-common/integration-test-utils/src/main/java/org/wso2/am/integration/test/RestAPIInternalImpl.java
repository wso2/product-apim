package org.wso2.am.integration.test;

import org.wso2.am.integration.clients.internal.ApiClient;
import org.wso2.am.integration.clients.internal.ApiException;
import org.wso2.am.integration.clients.internal.api.RetrievingWebhooksSubscriptionsApi;
import org.wso2.am.integration.clients.internal.api.RevokeJwt_Api;
import org.wso2.am.integration.clients.internal.api.dto.RevokedEventsDTO;
import org.wso2.am.integration.clients.internal.api.dto.WebhooksSubscriptionsListDTO;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;

public class RestAPIInternalImpl {
    RevokeJwt_Api revokedListAPI = new RevokeJwt_Api();
    ApiClient apiClient = new ApiClient();
    RetrievingWebhooksSubscriptionsApi webhooksSubscriptionsApi = new RetrievingWebhooksSubscriptionsApi();
    String tenantDomain;

    public RestAPIInternalImpl(String username, String password, String tenantDomain) {
        String basicEncoded =
                DatatypeConverter.printBase64Binary((username + ':' + password).getBytes(StandardCharsets.UTF_8));
        apiClient.addDefaultHeader("Authorization", "Basic " + basicEncoded);
        apiClient.setDebugging(true);
        apiClient.setBasePath("https://localhost:9943/internal/data/v1");
        apiClient.setReadTimeout(600000);
        apiClient.setConnectTimeout(600000);
        apiClient.setWriteTimeout(600000);
        revokedListAPI.setApiClient(apiClient);
        this.tenantDomain = tenantDomain;
        webhooksSubscriptionsApi.setApiClient(apiClient);
    }

    public RevokedEventsDTO retrieveRevokedList() throws ApiException {
        return revokedListAPI.revokedjwtGet();
    }

    public WebhooksSubscriptionsListDTO retrieveWebhooksSubscriptions() throws ApiException {
        return webhooksSubscriptionsApi.webhooksSubscriptionsGet(tenantDomain);
    }
}
