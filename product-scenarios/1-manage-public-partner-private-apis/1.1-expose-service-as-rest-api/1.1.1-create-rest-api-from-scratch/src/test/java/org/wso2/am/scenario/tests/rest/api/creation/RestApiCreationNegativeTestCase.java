package org.wso2.am.scenario.tests.rest.api.creation;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

public class RestApiCreationNegativeTestCase extends ScenarioTestBase {
    private APIRequest apiRequest;

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_USERNAME = "admin@wso2.com";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";

    private String apiName = "PhoneVerificationNeg";
    private String newApiName = "PhoneVerificationNegNew";
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
    private String apiProductionEndPointUrl;
    private  String apiProductionEndpointPostfixUrl = "jaxrs_basic/services/customers/" + "customerservice/customers/123";
    private String apiId;
    private String apiProviderName;
    private List<String> apiIdList = new ArrayList<>();
    private HttpResponse serviceResponse;

    protected TestUserMode userMode;

    @Factory(dataProvider = "userModeDataProvider")
    public RestApiCreationNegativeTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                    ADMIN_USERNAME, ADMIN_PW);
            createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, ADMIN_USERNAME, ADMIN_PW);
        }

        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            // create user in wso2.com tenant
            addTenantAndActivate(ScenarioTestConstants.TENANT_WSO2, ADMIN_USERNAME, ADMIN_PW);
            if (isActivated(ScenarioTestConstants.TENANT_WSO2)) {
                //Add and activate wso2.com tenant
                createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                        TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
                createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, TENANT_ADMIN_USERNAME,
                        TENANT_ADMIN_PW);
            }
        }

        super.init(userMode);

        apiProviderName = publisherContext.getContextTenant().getContextUser().getUserName();

        List<APIOperationsDTO> apiOperationsDTOs = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget(apiResource);
        apiOperationsDTOs.add(apiOperationsDTO);

        apiRequest = new APIRequest(apiName, apiContext, new URL(endpointUrl));
        apiRequest.setVersion(apiVersion);
        apiRequest.setVisibility(apiVisibility);
        apiRequest.setOperationsDTOS(apiOperationsDTOs);

        //Design API with name,context, version,visibility, URL and apiResource
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = serviceResponse.getData();
        apiIdList.add(apiId);

    }

    @Test(description = "1.1.1.7")
    public void testRESTAPICreationWithExistingName() throws Exception {

        List<APIOperationsDTO> apiOperationsDTOs = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget(apiResource);

        apiOperationsDTOs.add(apiOperationsDTO);

        apiRequest = new APIRequest(apiName, newContext, new URL(endpointUrl));
        apiRequest.setVersion(apiVersion);
        apiRequest.setVisibility(apiVisibility);
        apiRequest.setTiersCollection(tiersCollection);
        apiRequest.setOperationsDTOS(apiOperationsDTOs);

        //Try to add API with same api name
        serviceResponse = restAPIPublisher.addAPI(apiRequest);
        Assert.assertTrue(serviceResponse == null, "API created with existing name!");

        apiRequest = new APIRequest(apiName.toUpperCase(), newContext, new URL(endpointUrl));
        apiRequest.setVersion(apiVersion);
        apiRequest.setVisibility(apiVisibility);
        apiRequest.setTiersCollection(tiersCollection);
        apiRequest.setOperationsDTOS(apiOperationsDTOs);

        //Try to add API with same api name with uppercase
        serviceResponse = restAPIPublisher.addAPI(apiRequest);
        Assert.assertTrue(serviceResponse == null, "API created with existing name");
    }

    @Test(description = "1.1.1.8")
    public void testRESTAPICreationWithExistingContext() throws Exception {

        List<APIOperationsDTO> apiOperationsDTOs = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget(apiResource);

        apiOperationsDTOs.add(apiOperationsDTO);

        apiRequest = new APIRequest(newApiName, apiContext, new URL(endpointUrl));
        apiRequest.setVersion(apiVersion);
        apiRequest.setVisibility(apiVisibility);
        apiRequest.setTiersCollection(tiersCollection);
        apiRequest.setOperationsDTOS(apiOperationsDTOs);

        //Try to add API with same api context
        HttpResponse serviceResponse = restAPIPublisher.addAPI(apiRequest);
        Assert.assertTrue(serviceResponse == null, "API created with existing context!");
    }

    @Test(description = "1.1.1.9")
    public void testRESTAPICreationWith255CharactersName() throws Exception {

        List<APIOperationsDTO> apiOperationsDTOs = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget(apiResource);

        apiOperationsDTOs.add(apiOperationsDTO);

        apiRequest = new APIRequest(apiName255, newContext, new URL(endpointUrl));
        apiRequest.setVersion(apiVersion);
        apiRequest.setVisibility(apiVisibility);
        apiRequest.setTiersCollection(tiersCollection);
        apiRequest.setOperationsDTOS(apiOperationsDTOs);

        //Try to add API with api name with max characters (185)
        try {
            serviceResponse = restAPIPublisher.addAPI(apiRequest);
        } catch (ApiException e){
            Assert.assertTrue(serviceResponse == null, "API created with 255 character name!");
        }
    }

    @Test(description = "1.1.1.10")
    public void testRESTAPICreationWithNotAllowedCharactersName() throws Exception {

        List<APIOperationsDTO> apiOperationsDTOs = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget(apiResource);

        apiOperationsDTOs.add(apiOperationsDTO);

        apiRequest = new APIRequest(apiNameSpecial, newContext, new URL(endpointUrl));
        apiRequest.setVersion(apiVersion);
        apiRequest.setVisibility(apiVisibility);
        apiRequest.setTiersCollection(tiersCollection);
        apiRequest.setOperationsDTOS(apiOperationsDTOs);

        //Try to add API with api name with not allowed special characters
        try {
            serviceResponse = restAPIPublisher.addAPI(apiRequest);
        } catch (ApiException e){
            Assert.assertTrue(serviceResponse == null, "API created with not allowed characters!");
        }
    }

    //TODO: Remove the comment once considered environment fix for create context with 255characters

//    @Test(description = "1.1.1.11")
//    public void testRESTAPICreationWith255CharactersContext() throws Exception {
//
//        apiRequest = new APIRequest(newApiName, apiContext255, apiVisibility, apiVersion, apiResource, tiersCollection, new URL(endpointUrl));
//
//        //Try to add API with api context with 255 characters
//        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
//        Assert.assertTrue(serviceResponse.getData().contains(InvalidNameResponse + newApiName + "-" + apiVersion));
//    }

    /*
     //TODO: this test should be enabled once context validation in jaggery call fixed..
    @Test(description = "1.1.1.12")
    public void testRESTAPICreationWithNotAllowedCharactersContext() throws Exception{

        apiRequest = new APIRequest(newApiName, apiContextSpecial, apiVisibility, apiVersion, apiResource, tiersCollection, new URL (endpointUrl));

        //Try to add API with api context in not allowed special characters
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        //Assert.assertTrue(serviceResponse.getData().contains(InvalidSpecialNameResponse + apiNameSpecial + "-" + apiVersion));
    }

     //TODO: this test should be enabled once api validation in jaggery call fixed..
    @Test(description = "1.1.1.13")
    public void testRESTAPICreationWithSpacesContext() throws Exception{

        apiRequest = new APIRequest(newApiName, apiContextSpaces, apiVisibility, apiVersion, apiResource, tiersCollection, new URL (endpointUrl));

        //Try to add API with api context in white space characters
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(serviceResponse);
    }

     //TODO: this test should be enabled once api validation in jaggery call fixed..
    @Test(description = "1.1.1.14")
    public void testRESTAPICreationWithSpacesApiname() throws Exception{

        apiRequest = new APIRequest(apiNameSpaces, newContext, apiVisibility, apiVersion, apiResource, tiersCollection, new URL (endpointUrl));

        //Try to add API with api name in white space characters
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(serviceResponse);
    }
*/

    @Test(description = "1.1.1.15")
    public void testRESTAPICreationWithoutApiname() throws Exception {

        List<APIOperationsDTO> apiOperationsDTOs = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget(apiResource);

        apiOperationsDTOs.add(apiOperationsDTO);

        apiRequest = new APIRequest("", newContext, new URL(endpointUrl));
        apiRequest.setVersion(apiVersion);
        apiRequest.setVisibility(apiVisibility);
        apiRequest.setTiersCollection(tiersCollection);
        apiRequest.setOperationsDTOS(apiOperationsDTOs);

        //Design API without an API name

        try {
            System.out.println("Api name is null in the request");
            serviceResponse = restAPIPublisher.addAPI(apiRequest);
        } catch (ApiException e){
            Assert.assertTrue(serviceResponse == null, "API created without name!");
        }
    }


    @Test(description = "1.1.1.16")
    public void testRESTAPICreationWithoutContext() throws Exception {

        List<APIOperationsDTO> apiOperationsDTOs = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget(apiResource);

        apiOperationsDTOs.add(apiOperationsDTO);

        apiRequest = new APIRequest(newApiName, "", new URL(endpointUrl));
        apiRequest.setVersion(apiVersion);
        apiRequest.setVisibility(apiVisibility);
        apiRequest.setTiersCollection(tiersCollection);
        apiRequest.setOperationsDTOS(apiOperationsDTOs);

        //Design API without an API context
        try {
            System.out.println("Api context is null in the request");
            serviceResponse = restAPIPublisher.addAPI(apiRequest);
        } catch (ApiException e){
            Assert.assertTrue(serviceResponse == null, "API created without context!");
        }
    }


    @Test(description = "1.1.1.17")
    public void testRESTAPICreationWithoutVersion() throws Exception {

        List<APIOperationsDTO> apiOperationsDTOs = new ArrayList<>();
        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget(apiResource);

        apiOperationsDTOs.add(apiOperationsDTO);

        apiRequest = new APIRequest(newApiName, newContext, new URL(endpointUrl));
        apiRequest.setVersion("");
        apiRequest.setVisibility(apiVisibility);
        apiRequest.setTiersCollection(tiersCollection);
        apiRequest.setOperationsDTOS(apiOperationsDTOs);

        //Design API without an API version
        try {
            System.out.println("Api version is null in the request");
            serviceResponse = restAPIPublisher.addAPI(apiRequest);
        } catch (ApiException e){
            Assert.assertTrue(serviceResponse == null, "API created without version!");
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        for (String apiId : apiIdList) {
            restAPIPublisher.deleteAPI(apiId);
        }

        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            deleteUser(API_CREATOR_PUBLISHER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
            deleteUser(API_SUBSCRIBER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            deleteUser(API_CREATOR_PUBLISHER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deleteUser(API_SUBSCRIBER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            deactivateAndDeleteTenant(ScenarioTestConstants.TENANT_WSO2);
        }
    }

    @DataProvider
    public static Object[][] userModeDataProvider() throws Exception {
        setup();
        // return the relevant parameters for each test run
        // 1) Super tenant API creator
        // 2) Tenant API creator
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_USER},
                // new Object[]{TestUserMode.TENANT_USER},
        };
    }

}
