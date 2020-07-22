package org.wso2.am.scenario.tests.update.published.api.using.publisher;

import io.swagger.models.HttpMethod;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.scenario.test.common.*;
import org.wso2.am.scenario.test.common.swagger.*;
import org.wso2.am.scenario.test.common.swagger.Parameters;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;

public class UpdatePublishedAPITest extends ScenarioTestBase {

    private static final String UPDATE_RESOURCE = "/update";
    private static final String API_VERSION = "1.0.0";



    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_USERNAME = "admin@wso2.com";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";

    @Factory(dataProvider = "userModeDataProvider")
    public UpdatePublishedAPITest(TestUserMode userMode) {
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
    }

    @Test(description = "6.1.1.1")
    public void testAddNewResourceToAlreadyPublishedAPI() throws Exception {

        String apiName = "APIResourceAPI";
        String apiId = createApi(apiName);
        publishAPI(apiId);
        String storeSwagger = null;
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            storeSwagger = restAPIStore.getSwaggerByID(apiId,"carbon.super");
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            storeSwagger = restAPIStore.getSwaggerByID(apiId,"wso2.com");
        }

        // Get published API in store
        String publisherSwagger = restAPIPublisher.getSwaggerByID(apiId);

        String modifiedResource = "{\n" +
                "  \"openapi\" : \"3.0.1\",\n" +
                "  \"info\" : {\n" +
                "    \"title\" : \"APIResourceTestAPI\",\n" +
                "    \"description\" : \"description\",\n" +
                "    \"version\" : \"1.0.0\"\n" +
                "  },\n" +
                "  \"servers\" : [ {\n" +
                "    \"url\" : \"https://localhost:8243/menu/1.0.0\"\n" +
                "  }, {\n" +
                "    \"url\" : \"http://localhost:8280/menu/1.0.0\"\n" +
                "  } ],\n" +
                "  \"security\" : [ {\n" +
                "    \"default\" : [ ]\n" +
                "  } ],\n" +
                "  \"paths\" : {\n" +
                "    \"/*\" : {\n" +
                "      \"get\" : {\n" +
                "        \"responses\" : {\n" +
                "          \"200\" : {\n" +
                "            \"description\" : \"OK\"\n" +
                "          }\n" +
                "        },\n" +
                "        \"security\" : [ {\n" +
                "          \"default\" : [ ]\n" +
                "        } ],\n" +
                "        \"x-auth-type\" : \"Application & Application User\",\n" +
                "        \"x-throttling-tier\" : \"Unlimited\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"/updatedPath\" : {\n" +
                "      \"get\" : {\n" +
                "        \"responses\" : {\n" +
                "          \"200\" : {\n" +
                "            \"description\" : \"OK\"\n" +
                "          }\n" +
                "        },\n" +
                "        \"security\" : [ {\n" +
                "          \"default\" : [ ]\n" +
                "        } ],\n" +
                "        \"x-auth-type\" : \"Application & Application User\",\n" +
                "        \"x-throttling-tier\" : \"Unlimited\"\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"components\" : {\n" +
                "    \"securitySchemes\" : {\n" +
                "      \"default\" : {\n" +
                "        \"type\" : \"oauth2\",\n" +
                "        \"flows\" : {\n" +
                "          \"implicit\" : {\n" +
                "            \"authorizationUrl\" : \"https://localhost:8243/authorize\",\n" +
                "            \"scopes\" : { }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        String publisherSwaggerUpdated = restAPIPublisher.updateSwagger(apiId, modifiedResource);
        assertNotNull(publisherSwaggerUpdated);
        publishAPI(apiId);
        String storeSwaggerUpdated = null;
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            storeSwaggerUpdated = restAPIStore.getSwaggerByID(apiId,"carbon.super");
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            storeSwaggerUpdated = restAPIStore.getSwaggerByID(apiId,"wso2.com");
        }

        publisherSwaggerUpdated = restAPIPublisher.getSwaggerByID(apiId);
        assertTrue(storeSwaggerUpdated.contains("updatedPath"));
        assertNotEquals(storeSwagger, storeSwaggerUpdated);
        assertNotEquals(publisherSwagger, publisherSwaggerUpdated);
        restAPIPublisher.deleteAPI(apiId);

    }

    private String createApi(String apiName) throws Exception {
        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(apiName, "/menu", API_VERSION,
                "admin", new URL("https://localhost:9443/am/sample/pizzashack/v1/api/"));

        APIDTO apiDto = restAPIPublisher.addAPI(apiCreationRequestBean);
        assertTrue(StringUtils.isNotEmpty(apiDto.getId()), "Error occurred when creating api");
        return apiDto.getId();
    }

    private void publishAPI(String apiID) throws Exception {
        restAPIPublisher.changeAPILifeCycleStatus(apiID, APILifeCycleAction.PUBLISH.getAction(), null);
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

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            // deleteUser(API_CREATOR_PUBLISHER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
            // deleteUser(API_SUBSCRIBER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            // deleteUser(API_CREATOR_PUBLISHER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            // deleteUser(API_SUBSCRIBER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            // deactivateAndDeleteTenant(ScenarioTestConstants.TENANT_WSO2);
        }
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        setup();
        // return the relevant parameters for each test run
        // 1) Super tenant API creator
        // 2) Tenant API creator
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_USER},
                new Object[]{TestUserMode.TENANT_USER},
        };
    }
}
