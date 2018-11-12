package org.wso2.am.scenario.tests.rest.api.creation;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.annotations.AfterClass;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.util.Properties;
import java.net.URL;

public class RestApiCreationNegativeTestCase extends ScenarioTestBase {
    private APIPublisherRestClient apiPublisher;
    private String publisherURLHttp;
    private APIRequest apiRequest;
    private Properties infraProperties;

    private String apiName = "PhoneVerification";
    private String newApiName = "PhoneVerificationNew";
    private String apiContext = "/phoneverify";
    private String newContext = "/phoneverifynew";
    private String apiVersion = "1.0.0";
    private String apiVisibility = "public";
    private String apiResource = "/find";
    private String tiersCollection= "Gold,Bronze";
    private String endpointUrl= "http://test";
    private String InvalidNameResponse = "A duplicate API already exists";
    private String InvalidContextResponse = "A duplicate API context already exists";


    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException{
        infraProperties = getDeploymentProperties();
        String authority = infraProperties.getProperty(CARBON_SERVER_URL);
        if (authority != null && authority.contains("/")) {
            authority = authority.split("/")[2];
        } else if (authority == null) {
            authority = "localhost";
        }
        publisherURLHttp = "http://" + authority + ":9763/";

        setKeyStoreProperties();
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login("admin", "admin");
    }

    @Test(description = "Base Test case")
    public void testRESTAPICreationWithMandatoryValues() throws Exception {

        apiRequest = new APIRequest(apiName, apiContext, apiVisibility, apiVersion, apiResource);

        //Design API with name,context, version,visibility and apiResource
        HttpResponse serviceResponse = apiPublisher.designAPI(apiRequest);
        verifyResponse(serviceResponse);

    }

    @Test(description = "1.1.1.6", dependsOnMethods = "testRESTAPICreationWithMandatoryValues")
    public void testRESTAPICreationWithExistingName() throws Exception{

        apiRequest = new APIRequest(apiName, newContext, apiVisibility, apiVersion, apiResource, tiersCollection, new URL (endpointUrl));

        //Try to add API with same api name
        HttpResponse serviceResponse1 = apiPublisher.addAPI(apiRequest);
        Assert.assertTrue(serviceResponse1.getData().contains(InvalidNameResponse));

        //Try to add API with same api name to check case sensitivity
        apiRequest = new APIRequest(apiName.toUpperCase(), newContext, apiVisibility, apiVersion, apiResource, tiersCollection, new URL (endpointUrl));
        HttpResponse serviceResponse2 = apiPublisher.addAPI(apiRequest);
        Assert.assertTrue(serviceResponse2.getData().contains(InvalidNameResponse));
    }

    @Test(description = "1.1.1.7", dependsOnMethods = "testRESTAPICreationWithMandatoryValues")
    public void testRESTAPICreationWithExistingContext() throws Exception{

        apiRequest = new APIRequest(newApiName, apiContext, apiVisibility, apiVersion, apiResource, tiersCollection, new URL (endpointUrl));

        //Try to add API with same api context
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        Assert.assertTrue(serviceResponse.getData().contains(InvalidContextResponse));
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        HttpResponse serviceResponse = apiPublisher.deleteAPI(apiName, apiVersion, "admin");
        verifyResponse(serviceResponse);
    }

}
