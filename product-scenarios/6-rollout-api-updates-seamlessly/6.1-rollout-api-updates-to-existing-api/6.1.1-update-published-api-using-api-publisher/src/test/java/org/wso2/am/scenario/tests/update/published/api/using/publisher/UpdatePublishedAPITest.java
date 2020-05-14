package org.wso2.am.scenario.tests.update.published.api.using.publisher;

import io.swagger.models.HttpMethod;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.scenario.test.common.*;
import org.wso2.am.scenario.test.common.swagger.*;
import org.wso2.am.scenario.test.common.swagger.Parameters;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UpdatePublishedAPITest extends ScenarioTestBase {

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private List<APIIdentifier> createdAPIs = new ArrayList<>();

    private String providerUsername;
    private String providerPassword;
    private String subscriberUserName;
    private String subscriberPassword;
    private String adminUsername;
    private String adminPassword;


    private static final String ADD_RESOURCE = "/add";
    private static final String RETRIEVE_RESOURCE = "/retrieve";
    private static final String UPDATE_RESOURCE = "/update";

    private static final String API_KEY = "api";
    private static final String TEMPLATES_KEY = "templates";
    private static final String RESOURCES_KEY = "resources";


    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";

    private static final String PROVIDER_USERNAME = "creator";
    private static final String PROVIDER_PASSWORD = "creator@123";

    public static final String SUBSCRIBER_USERNAME = "subscriber";
    public static final String SUBSCRIBER_PASSWORD = "subscriber@123";

    // All tests in this class will run with a super tenant API creator and a tenant API creator.
    @Factory(dataProvider = "userModeDataProvider")
    public UpdatePublishedAPITest(String providerUsername, String providerPassword, String subscriberUserName,
                                  String subscriberPassword, String adminUsername, String adminPassword) {
        this.providerUsername = providerUsername;
        this.providerPassword = providerPassword;
        this.subscriberUserName = subscriberUserName;
        this.subscriberPassword = subscriberPassword;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        apiPublisher = new APIPublisherRestClient(publisherURL);
        apiStore = new APIStoreRestClient(storeURL);

        apiStore.login(subscriberUserName, subscriberPassword);
    }

    @Test(description = "6.1.1.1")
    public void testAddNewResourceToAlreadyPublishedAPI() throws Exception {
        String provider = providerUsername;
        String apiName = UUID.randomUUID().toString();
        String apiVersion = "1.0.0";
        String endpointUrl = "http://test";

        Swagger2Builder swagger = buildSwagger(apiName, apiVersion);

        apiPublisher.login(providerUsername, providerPassword);

        APIIdentifier apiIdentifier = addAPI(provider, apiName, apiVersion, endpointUrl, swagger);

        publishAPI(apiIdentifier);

        // Get published API in store
        HttpResponse apiResponse = apiStore.getAPI(provider, apiName, apiVersion);

        JSONObject api = new JSONObject(apiResponse.getData());

        JSONArray apiTemplates = api.getJSONObject(API_KEY).getJSONArray(TEMPLATES_KEY);
        String apiResources = api.getJSONObject(API_KEY).getString(RESOURCES_KEY);

        Assert.assertEquals(apiTemplates.length(), 2);
        Assert.assertTrue(apiResources.contains(ADD_RESOURCE));
        Assert.assertTrue(apiResources.contains(RETRIEVE_RESOURCE));

        // Add a new resource to existing swagger
        addNewResourceToSwagger(swagger);

        // Update API
        updateAPI(apiName, apiVersion, endpointUrl, swagger);

        // Check new resource in store
        HttpResponse updatedApiResponse = apiStore.getAPI(provider, apiName, apiVersion);

        JSONObject updatedApi = new JSONObject(updatedApiResponse.getData());

        JSONArray updatedApiTemplates = updatedApi.getJSONObject(API_KEY).getJSONArray(TEMPLATES_KEY);
        String updatedApiResources = updatedApi.getJSONObject(API_KEY).getString(RESOURCES_KEY);

        Assert.assertEquals(updatedApiTemplates.length(), 3);
        Assert.assertTrue(updatedApiResources.contains(ADD_RESOURCE));
        Assert.assertTrue(updatedApiResources.contains(RETRIEVE_RESOURCE));
        Assert.assertTrue(updatedApiResources.contains(UPDATE_RESOURCE));

        int matchingTemplateCount = 0;
        JSONArray newTemplate = new JSONArray();

        for (int i = 0; i < apiTemplates.length(); ++i) {
            JSONArray apiTemplate = (JSONArray) apiTemplates.get(i);

            for (int j = 0; j < updatedApiTemplates.length(); ++j) {
                JSONArray updatedApiTemplate = (JSONArray) updatedApiTemplates.get(j);

                if (apiTemplate.toString().equals(updatedApiTemplate.toString())) {
                    ++matchingTemplateCount;
                    break;
                } else {
                    newTemplate = updatedApiTemplate;
                }
            }
        }

        Assert.assertEquals(matchingTemplateCount, 2);
        Assert.assertTrue(newTemplate.toString().contains(UPDATE_RESOURCE));
    }

    private APIIdentifier addAPI(String provider, String apiName, String apiVersion,
                                 String endpointUrl, Swagger2Builder swagger) throws Exception {
        APIRequest apiRequest = new APIRequest(apiName, "/" + apiName, "public", apiVersion, "dummy",
                "Unlimited", new URL(endpointUrl));
        apiRequest.setSwagger(swagger.getSwaggerJSON());
        //Create API
        HttpResponse serviceResponse = apiPublisher.addAPI(apiRequest);
        verifyResponse(serviceResponse);

        APIIdentifier apiIdentifier = new APIIdentifier(provider, apiName, apiVersion);

        // Store created API so we can delete it at the end of the test
        createdAPIs.add(apiIdentifier);

        return apiIdentifier;
    }

    private void publishAPI(APIIdentifier apiIdentifier) throws Exception {
        apiPublisher.changeAPILifeCycleStatusToPublish(apiIdentifier, true);
    }

    private void updateAPI(String apiName, String apiVersion,
                           String endpointUrl, Swagger2Builder swagger) throws Exception {
        APIRequest apiRequest = new APIRequest(apiName, "/" + apiName, "public", apiVersion, "dummy",
                "Unlimited", new URL(endpointUrl));
        apiRequest.setSwagger(swagger.getSwaggerJSON());

        HttpResponse serviceResponse = apiPublisher.updateAPI(apiRequest);
        verifyResponse(serviceResponse);
    }

    private void addNewResourceToSwagger(Swagger2Builder swagger) {
        Responses updateResourceResponses = new Responses();
        updateResourceResponses.addResponse("200", "Updated");

        ResourcePaths resourcePaths = new ResourcePaths();

        Parameters parameters = new Parameters();
        parameters.addQueryParameter("id", "Query Id", true);

        TypeProperties typeProperties = new TypeProperties();
        typeProperties.addStringProperty("name");
        parameters.addBodyParameter("Body", "Request", true, typeProperties);

        resourcePaths.addResourcePath(UPDATE_RESOURCE, HttpMethod.PUT, parameters, updateResourceResponses);

        swagger.createResourcePaths(resourcePaths);
    }

    private Swagger2Builder buildSwagger(String apiName, String version) {
        Swagger2Builder swagger2Builder = new Swagger2Builder(apiName, version);

        Responses addResourceResponses = new Responses();
        addResourceResponses.addResponse("201", "Added");

        ResourcePaths resourcePaths = new ResourcePaths();
        resourcePaths.addResourcePath(ADD_RESOURCE, HttpMethod.POST, addResourceResponses);

        Responses retrieveResourceResponses = new Responses();
        retrieveResourceResponses.addResponse("200", "Returned");

        Parameters parameters = new Parameters();
        parameters.addQueryParameter("id", "Query Id", true);

        resourcePaths.addResourcePath(RETRIEVE_RESOURCE, HttpMethod.GET, parameters, retrieveResourceResponses);

        swagger2Builder.createResourcePaths(resourcePaths);

        return swagger2Builder;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        //Remove created APIs
        for (APIIdentifier apiIdentifier : createdAPIs) {
            apiPublisher.deleteAPI(apiIdentifier.getApiName(),
                    apiIdentifier.getVersion(), apiIdentifier.getProviderName());
        }

        deleteUser(MultitenantUtils.getTenantAwareUsername(providerUsername), adminUsername, adminPassword);
        deleteUser(MultitenantUtils.getTenantAwareUsername(subscriberUserName), adminUsername, adminPassword);
    }

    // This method runs prior to the @BeforeClass method.
    // Create users and tenants needed or the tests in here. Try to reuse the TENANT_WSO2 as much as possible to avoid the number of tenants growing.
    @DataProvider
    public static Object[][] userModeDataProvider() throws Exception {
        setup();
        //Add and activate wso2.com tenant
        addTenantAndActivate(ScenarioTestConstants.TENANT_WSO2, ADMIN_USERNAME, ADMIN_PASSWORD);

        // create provider user in super tenant
        createUserWithPublisherAndCreatorRole(PROVIDER_USERNAME, PROVIDER_PASSWORD, ADMIN_USERNAME, ADMIN_PASSWORD);

        // create provider user in wso2.com tenant
        createUserWithPublisherAndCreatorRole(PROVIDER_USERNAME, PROVIDER_PASSWORD, appendTenant(ADMIN_USERNAME),
                ADMIN_PASSWORD);

        // create subscriber user in super tenant
        createUserWithSubscriberRole(SUBSCRIBER_USERNAME, SUBSCRIBER_PASSWORD, ADMIN_USERNAME, ADMIN_PASSWORD);

        // create subscriber user in wso2.com tenant
        createUserWithSubscriberRole(SUBSCRIBER_USERNAME, SUBSCRIBER_PASSWORD, appendTenant(ADMIN_USERNAME),
                ADMIN_PASSWORD);

        // return the relevant parameters for each test run
        // 1) Super tenant API creator
        // 2) Tenant API creator
        return new Object[][] {
                {
                    PROVIDER_USERNAME, PROVIDER_PASSWORD,
                    SUBSCRIBER_USERNAME, SUBSCRIBER_PASSWORD,
                    ADMIN_USERNAME, ADMIN_PASSWORD
                },
                {
                    appendTenant(PROVIDER_USERNAME), PROVIDER_PASSWORD,
                    appendTenant(SUBSCRIBER_USERNAME), SUBSCRIBER_PASSWORD,
                    appendTenant(ADMIN_USERNAME), ADMIN_PASSWORD
                }
        };
    }

    private static String appendTenant(String userName) {
        return userName + '@' + ScenarioTestConstants.TENANT_WSO2;
    }
}
