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


public class APINewVersionTestCase extends ScenarioTestBase {
    private final Log log = LogFactory.getLog(ScenarioSampleTest.class);
    private APIPublisherRestClient apiPublisher;
    private String publisherURLHttp;
    private APIRequest apiRequest;

    private final String apiName = "Mobile_Stock_API";
    private final String APIContext = "MobileStock";
    private final String tags = "stock";
    private final String description = "This is test API created for scenario test";
    private final String APIVersion = "1.0.0";
    private final String APIVersionNew = "2.0.0";
    private final String InvalidVersionResponse = "API already exists with version";
    private String providerName = "admin";
    private String endpointUrl;
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
        if (authority != null && authority.contains("/")) {
            authority = authority.split("/")[2];
        } else if (authority == null) {
            authority = "localhost";
        }
        publisherURLHttp = "http://" + authority + ":9763/";
        endpointUrl = "http://" + authority + ":9763/am/sample/calculator/v1/api/add";

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

        //copy initial api with new version
        serviceResponse = apiPublisher
                .copyAPI(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(), APIVersionNew,
                        null);
        verifyResponse(serviceResponse);

        //test the copied api
        serviceResponse = apiPublisher.getAPI(apiRequest.getName(), apiRequest.getProvider(), APIVersionNew);

        JSONObject response = new JSONObject(serviceResponse.getData());
        String version = response.getJSONObject("api").get("version").toString();
        Assert.assertEquals(version, APIVersionNew);

    }

    @Test(description = "1.1.1.2", enabled = true)
    public void testAPINewVersionWithExistingVersion() throws Exception{

        //copy initial api with invalid version
        HttpResponse serviceResponse = apiPublisher.copyAPI(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(), APIVersionNew,
                null);

        Assert.assertTrue(serviceResponse.getData().contains(InvalidVersionResponse));
    }

    @Test(description = "1.1.1.3", enabled = true)
    public void testAPINewVersionPublish() throws Exception{
        //publish new versioned api
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, "admin",
                APILifeCycleState.PUBLISHED);
        HttpResponse serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);

        Assert.assertTrue(serviceResponse.getData().contains(APILifeCycleState.PUBLISHED.getState()));
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
