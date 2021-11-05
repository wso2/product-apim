package org.wso2.am.integration.tests.logging;

import org.apache.axis2.AxisFault;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.LogViewerClient;
import org.wso2.carbon.logging.view.data.xsd.LogEvent;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.xml.xpath.XPathExpressionException;

import static org.testng.Assert.assertEquals;

public class APILoggingTest extends APIManagerLifecycleBaseTest {

    private LogViewerClient logViewerClient;
    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException, AxisFault, XPathExpressionException {
        super.init();
        logViewerClient = new LogViewerClient(
                gatewayContextMgt.getContextUrls().getBackEndUrl(), createSession(gatewayContextMgt));

        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();

        apiStore = new APIStoreRestClient(storeURLHttp);
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);


        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                           publisherContext.getContextTenant().getContextUser().getPassword());
        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                       storeContext.getContextTenant().getContextUser().getPassword());
    }

    @Test(groups = {"wso2.am" }, description = "Sending http request to per API logging enabled API: ")
    public void testAPIPerAPILoggingTestcase() throws Exception {
        boolean isPercentEncoded = false;
        logViewerClient.clearLogs();

        Map<String, String> header = new HashMap<>();
        header.put("Authorization", "Basic YWRtaW46YWRtaW4=");
        header.put("Content-Type", "application/json");
        HttpResponse loggingResponse = HTTPSClientUtils.doGet(getStoreURLHttps() + "api/am/devops/v1/tenant-logs/carbon.super/apis?logging-enabled=false", header);
        Assert.assertEquals(loggingResponse.getData(), "{\"apis\":[]}");

        final String API_NAME = "AddNewMediationAndInvokeAPITest";
        final String API_CONTEXT = "AddNewMediationAndInvokeAPI";
        final String API_TAGS = "testTag1, testTag2, testTag3";
        final String API_END_POINT_POSTFIX_URL = "xmlapi";
        final String API_VERSION_1_0_0 = "1.0.0";
        final String APPLICATION_NAME = "AddNewMediationAndInvokeAPI";
        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                                                                          "Test Application AccessibilityOfBlockAPITestCase", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                                                                          ApplicationDTO.TokenTypeEnum.JWT);
        String applicationId = applicationResponse.getData();

        //Create the api creation request object
        APIRequest apiRequest;
        String apiEndPointUrl = getAPIInvocationURLHttp(API_END_POINT_POSTFIX_URL, API_VERSION_1_0_0);
        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));

        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTags(API_TAGS);

        String apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationId,
                                                        APIMIntegrationConstants.API_TIER.UNLIMITED);


        loggingResponse = HTTPSClientUtils.doGet(getStoreURLHttps() + "api/am/devops/v1/tenant-logs/carbon.super/apis?logging-enabled=false", header);
        Assert.assertEquals(loggingResponse.getData(), "{\"apis\":[{\"context\":\"/AddNewMediationAndInvokeAPI/1.0.0\",\"logLevel\":\"OFF\",\"apiId\":\"" + apiId + "\"}]}");

        String addNewLoggerPayload = "{ \"logLevel\": \"FULL\" }";
        HTTPSClientUtils.doPut(getStoreURLHttps() + "api/am/devops/v1/tenant-logs/carbon.super/apis/" + apiId, header, addNewLoggerPayload);

        loggingResponse = HTTPSClientUtils.doGet(getStoreURLHttps() + "api/am/devops/v1/tenant-logs/carbon.super/apis?logging-enabled=false", header);
        Assert.assertEquals(loggingResponse.getData(), "{\"apis\":[{\"context\":\"/AddNewMediationAndInvokeAPI/1.0.0\",\"logLevel\":\"FULL\",\"apiId\":\"" + apiId + "\"}]}");

        ArrayList grantTypes = new ArrayList();
        grantTypes.add("client_credentials");

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "3600", null,
                                                                        ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        String accessToken = applicationKeyDTO.getToken().getAccessToken();

        HttpClient client = HttpClientBuilder.create().setHostnameVerifier(new AllowAllHostnameVerifier()).build();
        HttpGet request = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0));
        request.setHeader("Authorization", "Bearer " + accessToken);
        org.apache.http.HttpResponse response = client.execute(request);

        assertEquals(response.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request");

        LogEvent[] logs = logViewerClient.getAllSystemLogs();
        for (LogEvent logEvent : logs) {
            String message = logEvent.getMessage();
            if (message.contains(" | response-out<<<< | GET | AddNewMediationAndInvokeAPI/1.0.0 | PAYLOAD | ")) {
                isPercentEncoded = true;
                break;
            }
        }
        Assert.assertTrue(isPercentEncoded,
                          "Reserved character should be percent encoded while uri-template expansion");
    }
    protected String createPublishAndSubscribeToAPIUsingRest(APIRequest apiRequest,
                                                             RestAPIPublisherImpl publisherRestClient, RestAPIStoreImpl storeRestClient, String applicationId,
                                                             String tier)
            throws APIManagerIntegrationTestException, ApiException, XPathExpressionException {
        String apiId = createAndPublishAPIUsingRest(apiRequest, publisherRestClient, false);
        waitForAPIDeploymentSync(user.getUserName(), apiRequest.getName(), apiRequest.getVersion(),
                                 APIMIntegrationConstants.IS_API_EXISTS);
        HttpResponse httpResponseSubscribeAPI = subscribeToAPIUsingRest(apiId, applicationId, tier, storeRestClient);
        if (!(httpResponseSubscribeAPI.getResponseCode() == HTTP_RESPONSE_CODE_OK &&
              !StringUtils.isEmpty(httpResponseSubscribeAPI.getData()))) {
            throw new APIManagerIntegrationTestException("Error in API Subscribe." +
                                                         getAPIIdentifierStringFromAPIRequest(apiRequest) +
                                                         "Response Code:" + httpResponseSubscribeAPI.getResponseCode());
        }
        return apiId;
    }
}
