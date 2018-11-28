package org.wso2.am.integration.tests.versioning;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.config.RequestConfig;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.Properties;


public class ScenarioSampleTest extends ScenarioTestBase {
    private final Log log = LogFactory.getLog(ScenarioSampleTest.class);
    private APIPublisherRestClient apiPublisher;
    private String publisherURLHttp;
    private APIRequest apiRequest;

    private String apiName = "Mobile_Stock_API";
    private String APIContext = "MobileStock";
    private String tags = "stock";
    private String endpointUrl;
    private String description = "This is test API created for scenario test";
    private String APIVersion = "1.0.0";
    private String APIVersionNew = "2.0.0";
    private String providerName = "admin";
    private Properties infraProperties;
    String resourceLocation = System.getProperty("framework.resource.location");
    int timeout = 10;
    RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(timeout * 100)
            .setConnectionRequestTimeout(timeout * 1000)
            .setSocketTimeout(timeout * 1000).build();

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        infraProperties = getDeploymentProperties();
        String authority = infraProperties.getProperty(CARBON_SERVER_URL);
        publisherURLHttp = "https://localhost:9443/publisher";
        endpointUrl = "http://" + "localhost" + ":9763/am/sample/calculator/v1/api/add";

        setKeyStoreProperties();
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login("admin", "admin");
    }

    @Test(description = "1.1.1.1", enabled = true)
    public void testAPINewVersionCreation() throws Exception {

        apiRequest = new APIRequest(apiName, APIContext, new URL(endpointUrl));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setProvider(providerName);

        //add test api
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(serviceResponse);

        //publish the api
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, providerName,
                APILifeCycleState.PROTOTYPED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);

        //copy test api
        serviceResponse = apiPublisher
                .copyAPI(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(), APIVersionNew,
                        null);
        verifyResponse(serviceResponse);

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiPublisher.deleteAPI(apiName, APIVersion, "admin");
        apiPublisher.deleteAPI(apiName, APIVersionNew, "admin");
    }

    private void setKeyStoreProperties() {
        System.setProperty("javax.net.ssl.trustStore", resourceLocation + "/keystores/wso2carbon.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
    }

    protected void verifyResponse(HttpResponse httpResponse) throws JSONException {
        Assert.assertNotNull(httpResponse, "Response object is null");
        log.info("Response Code : " + httpResponse.getResponseCode());
        log.info("Response Message : " + httpResponse.getData());
        Assert.assertEquals(httpResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");
        JSONObject responseData = new JSONObject(httpResponse.getData());
        Assert.assertFalse(responseData.getBoolean(APIMIntegrationConstants.API_RESPONSE_ELEMENT_NAME_ERROR),
                "Error message received " + httpResponse.getData());
    }
}
