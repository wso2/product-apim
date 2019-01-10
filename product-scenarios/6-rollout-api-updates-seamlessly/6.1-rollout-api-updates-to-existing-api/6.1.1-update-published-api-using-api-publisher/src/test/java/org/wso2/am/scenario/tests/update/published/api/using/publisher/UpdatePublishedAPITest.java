package org.wso2.am.scenario.tests.update.published.api.using.publisher;

import io.swagger.models.HttpMethod;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONString;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.swagger.*;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class UpdatePublishedAPITest extends ScenarioTestBase {

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private Properties infraProperties;
    private List<APIIdentifier> createdAPIs = new ArrayList<>();

    private static final String ADD_RESOURCE = "/add";
    private static final String RETRIEVE_RESOURCE = "/retrieve";
    private static final String UPDATE_RESOURCE = "/update";

    private static final String API_KEY = "api";
    private static final String TEMPLATES_KEY = "templates";
    private static final String RESOURCES_KEY = "resources";


    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";

    private static final String CREATOR_USERNAME = "admin";
    private static final String CREATOR_PASSWORD = "admin";

    private static final String PUBLISHER_USERNAME = "admin";
    private static final String PUBLISHER_PASSWORD = "admin";


    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        infraProperties = getDeploymentProperties();
        publisherURL = infraProperties.getProperty(PUBLISHER_URL);
        String storeURL = infraProperties.getProperty(STORE_URL);

        if (publisherURL == null) {
            publisherURL = "http://localhost:9763/publisher";
        }
        if (storeURL == null) {
            storeURL = "http://localhost:9763/store";
        }

        setKeyStoreProperties();
        apiPublisher = new APIPublisherRestClient(publisherURL);
        apiStore = new APIStoreRestClient(storeURL);

        //createUserWithCreatorRole(CREATOR_USERNAME, CREATOR_PASSWORD, ADMIN_USERNAME, ADMIN_PASSWORD);
        //createUserWithPublisherRole(PUBLISHER_USERNAME, PUBLISHER_PASSWORD, ADMIN_USERNAME, ADMIN_PASSWORD);


        apiStore.login(PUBLISHER_USERNAME, PUBLISHER_PASSWORD);
    }

    @Test(description = "6.1.1.1")
    public void testAddNewResourceToAlreadyPublishedAPI() throws Exception {
        String provider = CREATOR_USERNAME;
        String apiName = UUID.randomUUID().toString();
        String apiVersion = "1.0.0";
        String endpointUrl = "http://test";

        Swagger2Builder swagger = buildSwagger(apiName, apiVersion);

        apiPublisher.login(CREATOR_USERNAME, CREATOR_PASSWORD);

        APIIdentifier apiIdentifier = addAPI(provider, apiName, apiVersion, endpointUrl, swagger);

        apiPublisher.login(PUBLISHER_USERNAME, PUBLISHER_PASSWORD);

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
            //JSONString apiTemplate = (JSONString) ((JSONArray)apiTemplates.get(0)).get(i);
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
    }
}
