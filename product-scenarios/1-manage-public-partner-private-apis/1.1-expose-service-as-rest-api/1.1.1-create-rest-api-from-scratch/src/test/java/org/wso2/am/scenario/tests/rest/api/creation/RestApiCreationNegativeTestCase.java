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
    private String apiName255 = "REST_API_with_API_name_contains_more_than_255_charactors_REST_API_with_API_name_contains_more_than_255_charactors_REST_API_with_API_name_contains_more_than_255_charactors_REST_API1234567890";
    private String apiNameSpecial = "Phone@$#Verify#";
    private String apiNameSpaces = "Phone Verify Api";
    private String apiContext = "/phoneverify";
    private String newContext = "/phoneverifynew";
    private String apiContext255 = "/REST_API_with_API_context_contains_more_than_255_charactors_REST_API_with_API_context_contains_more_than_255_charactors_REST_API_with_API_context_contains_more_than_255_charactors_REST_API_with_API_context_contains_more_than_255_charactors_REST_API_with_";
    private String apiContextSpecial = "/phone^@#*#verify";
    private String apiContextSpaces = "/phone verify";
    private String apiVersion = "1.0.0";
    private String apiVisibility = "public";
    private String apiResource = "/find";
    private String tiersCollection = "Gold,Bronze";
    private String endpointUrl = "http://test";
    private String DuplicateNameResponse = "A duplicate API already exists for ";
    private String DuplicateContextResponse = "A duplicate API context already exists for ";
    private String InvalidNameResponse = " Error while adding the API- ";


    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException {
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
    public void testRESTAPICreationWithExistingName() throws Exception {

        apiRequest = new APIRequest(apiName, newContext, apiVisibility, apiVersion, apiResource, tiersCollection, new URL(endpointUrl));

        //Try to add API with same api name
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        Assert.assertTrue(serviceResponse.getData().contains(DuplicateNameResponse + apiName + "-" + apiVersion));
    }


    @Test(description = "1.1.1.7", dependsOnMethods = "testRESTAPICreationWithMandatoryValues")
    public void testRESTAPICreationWithExistingContext() throws Exception {

        apiRequest = new APIRequest(newApiName, apiContext, apiVisibility, apiVersion, apiResource, tiersCollection, new URL(endpointUrl));

        //Try to add API with same api context
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        Assert.assertTrue(serviceResponse.getData().contains(DuplicateContextResponse + apiContext));
    }


    @Test(description = "1.1.1.8")
    public void testRESTAPICreationWith255CharactersName() throws Exception {

        apiRequest = new APIRequest(apiName255, newContext, apiVisibility, apiVersion, apiResource, tiersCollection, new URL(endpointUrl));

        //Try to add API with api name with max characters (185)
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        Assert.assertTrue(serviceResponse.getData().contains(InvalidNameResponse + apiName255 + "-" + apiVersion));
    }


    @Test(description = "1.1.1.9")
    public void testRESTAPICreationWithSpecialCharactersName() throws Exception {

        apiRequest = new APIRequest(apiNameSpecial, newContext, apiVisibility, apiVersion, apiResource, tiersCollection, new URL(endpointUrl));

        //Try to add API with api name with special characters
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        Assert.assertTrue(serviceResponse.getData().contains(InvalidNameResponse + apiNameSpecial + "-" + apiVersion));
    }


    @Test(description = "1.1.1.10")
    public void testRESTAPICreationWith255CharactersContext() throws Exception {

        apiRequest = new APIRequest(newApiName, apiContext255, apiVisibility, apiVersion, apiResource, tiersCollection, new URL(endpointUrl));

        //Try to add API with api context with 255 characters
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        Assert.assertTrue(serviceResponse.getData().contains(InvalidNameResponse + newApiName + "-" + apiVersion));
    }

    /*
     //TODO: this test should be enabled once context validation in jaggery call fixed..
    @Test(description = "1.1.1.11")
    public void testRESTAPICreationWithSpecialCharactersContext() throws Exception{

        apiRequest = new APIRequest(newApiName, apiContextSpecial, apiVisibility, apiVersion, apiResource, tiersCollection, new URL (endpointUrl));

        //Try to add API with api context in special characters
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        //Assert.assertTrue(serviceResponse.getData().contains(InvalidSpecialNameResponse + apiNameSpecial + "-" + apiVersion));
    }

     //TODO: this test should be enabled once api validation in jaggery call fixed..
    @Test(description = "1.1.1.12")
    public void testRESTAPICreationWithSpacesContext() throws Exception{

        apiRequest = new APIRequest(newApiName, apiContextSpaces, apiVisibility, apiVersion, apiResource, tiersCollection, new URL (endpointUrl));

        //Try to add API with api context in white space characters
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(serviceResponse);
    }

     //TODO: this test should be enabled once api validation in jaggery call fixed..
    @Test(description = "1.1.1.13")
    public void testRESTAPICreationWithSpacesApiname() throws Exception{

        apiRequest = new APIRequest(apiNameSpaces, newContext, apiVisibility, apiVersion, apiResource, tiersCollection, new URL (endpointUrl));

        //Try to add API with api name in white space characters
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(serviceResponse);
    }
*/

    @Test(description = "1.1.1.14")
    public void testRESTAPICreationWithoutApiname() throws Exception {

        apiRequest = new APIRequest("", newContext, apiVisibility, apiVersion, apiResource);

        //Design API without an API name
        HttpResponse serviceResponse = apiPublisher.designAPI(apiRequest);
        try {
            System.out.println("Api name is null in the request");
        } catch (NullPointerException e) {
            Assert.assertTrue(serviceResponse.getResponseMessage().contains("Error"), "false");
        }
    }


    @Test(description = "1.1.1.15")
    public void testRESTAPICreationWithoutContext() throws Exception {

        apiRequest = new APIRequest(newApiName, "", apiVisibility, apiVersion, apiResource);

        //Design API without an API context
        HttpResponse serviceResponse = apiPublisher.designAPI(apiRequest);
        try {
            System.out.println("Api context is null in the request");
        } catch (NullPointerException e) {
            Assert.assertTrue(serviceResponse.getResponseMessage().contains("Error"), "false");
        }
    }


    @Test(description = "1.1.1.16")
    public void testRESTAPICreationWithoutVersion() throws Exception {

        apiRequest = new APIRequest(newApiName, newContext, apiVisibility, "", apiResource);

        //Design API without an API version
        HttpResponse serviceResponse = apiPublisher.designAPI(apiRequest);
        try {
            System.out.println("Api version is null in the request");
        } catch (NullPointerException e) {
            Assert.assertTrue(serviceResponse.getResponseMessage().contains("Error"), "false");
        }
    }


    @Test(description = "1.1.1.17", dependsOnMethods = "testRESTAPICreationWithMandatoryValues")
    public void testRESTAPICreationWithExistingNameCaseInsensitive() throws Exception {

        apiRequest = new APIRequest(apiName.toUpperCase(), newContext, apiVisibility, apiVersion, apiResource, tiersCollection, new URL(endpointUrl));

        //Try to add API with same api name with uppercase
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        Assert.assertTrue(serviceResponse.getData().contains(DuplicateNameResponse));
    }


    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        HttpResponse serviceResponse = apiPublisher.deleteAPI(apiName, apiVersion, "admin");
        verifyResponse(serviceResponse);
    }

}
