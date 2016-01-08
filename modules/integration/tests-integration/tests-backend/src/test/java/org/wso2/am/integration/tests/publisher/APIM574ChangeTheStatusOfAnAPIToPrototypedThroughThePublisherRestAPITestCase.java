/*
 *
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.wso2.am.integration.tests.publisher;

import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Change the status of an API to PROTOTYPED and PUBLISHED through the publisher REST api
 * APIM2-574 /APIM2-587
 */

public class APIM574ChangeTheStatusOfAnAPIToPrototypedThroughThePublisherRestAPITestCase
        extends APIMIntegrationBaseTest {

    private final String apiNameTest = "APIM574PublisherTest";
    private final String apiVersion = "1.0.0";
    private APIPublisherRestClient apiPublisher;
    private String apiProvider;
    private String apiEndPointUrl;

    @Factory(dataProvider = "userModeDataProvider")
    public APIM574ChangeTheStatusOfAnAPIToPrototypedThroughThePublisherRestAPITestCase
            (TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
//                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        String apiProductionEndpointPostfixUrl = "jaxrs_basic/services/customers/" +
                "customerservice/customers/123";

        String publisherURLHttp = publisherUrls.getWebAppURLHttp();

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        apiEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() + apiProductionEndpointPostfixUrl;
        apiProvider = publisherContext.getContextTenant().getContextUser().getUserName();

    }

    @Test(groups = {"wso2.am"}, description = "Create an API throgh the publisher rest API ")
    public void testCreateAnAPIThroughThePublisherRest() throws Exception {

        String apiContext = "apim574PublisherTestAPI";
        String apiDescription = "This is Test API Created by API Manager Integration Test";
        String apiTags = "tag574-1, tag574-2, tag587-3";

        //Create an API
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiNameTest, apiContext, apiVersion, apiProvider,
                        new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(apiTags);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setTiersCollection("Gold,Bronze");
        apiCreationRequestBean.setDefaultVersion("default_version");
        apiCreationRequestBean.setDefaultVersionChecked("default_version");
        apiCreationRequestBean.setBizOwner("api574b");
        apiCreationRequestBean.setBizOwnerMail("api574b@ee.com");
        apiCreationRequestBean.setTechOwner("api574t");
        apiCreationRequestBean.setTechOwnerMail("api574t@ww.com");

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        JSONObject apiResponse = new JSONObject(apiCreationResponse.getData());
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when creating the API");
        assertFalse(apiResponse.getBoolean("error"), apiNameTest + "is not created as expected");

        //Check availability of the API in publisher
        HttpResponse apiResponsePublisher = apiPublisher.getAPI(apiNameTest, apiProvider, apiVersion);
        JSONObject jsonObject = new JSONObject(apiResponsePublisher.getData());
        JSONObject apiObject = new JSONObject(jsonObject.getString("api"));
        assertFalse(jsonObject.getBoolean("error"), apiNameTest + " is not visible in publisher");
        assertTrue(apiObject.getString("name").equals(apiNameTest),
                apiNameTest + " is not visible in publisher");
        assertTrue(apiObject.getString("status").equals("CREATED"),
                "Status of the " + apiNameTest + "is not a valid status");

    }

    @Test(groups = {"wso2.am"}, description = "Change the status of the API to PROTOTYPED through" +
            " the publisher rest API ", dependsOnMethods = "testCreateAnAPIThroughThePublisherRest")
    public void testChangeTheStatusOfTheAPIToPrototyped() throws Exception {

        //Change the status of the API from CREATED to PROTOTYPED
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiNameTest, apiProvider,
                APILifeCycleState.PROTOTYPED);
        updateRequest.setRequireResubscription("true");
        HttpResponse creationResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        assertTrue(creationResponse.getData().contains("PROTOTYPED"),
                apiNameTest + "  status not updated as Prototyped");

        //Check whether Prototype API is available in publisher
        HttpResponse prototypedApiResponse = (apiPublisher.getAPI
                (apiNameTest, apiProvider, apiVersion));
        JSONObject prototypedApiObject = new JSONObject(prototypedApiResponse.getData());
        JSONObject allApiObject = new JSONObject(prototypedApiObject.getString("api"));
        assertFalse(prototypedApiObject.getBoolean("error"), apiNameTest +
                " is not visible in publisher");
        assertTrue(allApiObject.getString("name").equals(apiNameTest),
                apiNameTest + " is not visible in publisher");
        assertTrue(allApiObject.getString("status").equals("PROTOTYPED"),
                "Status of the " + apiNameTest + "is not a valid status");
    }


    @Test(groups = {"wso2.am"}, description = "Change the status of the API to PUBLISHED through" +
            " the publisher rest API ", dependsOnMethods = "testChangeTheStatusOfTheAPIToPrototyped")
    public void testChangeTheStatusOfTheAPIToPublished() throws Exception {

        //Change the status PROTOTYPED to PUBLISHED
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest
                (apiNameTest, apiProvider, APILifeCycleState.PUBLISHED);
        updateRequest.setRequireResubscription("true");
        HttpResponse creationResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        assertTrue(creationResponse.getData().contains("PUBLISHED"),
                apiNameTest + "  status not updated as Prototyped");

        //Check whether published API is available in publisher
        HttpResponse prototypedApiResponse = (apiPublisher.getAPI
                (apiNameTest, apiProvider, apiVersion));
        JSONObject prototypedApiObject = new JSONObject(prototypedApiResponse.getData());
        JSONObject allApiObject = new JSONObject(prototypedApiObject.getString("api"));
        assertFalse(prototypedApiObject.getBoolean("error"), apiNameTest +
                " is not visible in publisher");
        assertTrue(allApiObject.getString("name").equals(apiNameTest),
                apiNameTest + " is not visible in publisher");
        assertTrue(allApiObject.getString("status").equals("PUBLISHED"),
                "Status of the " + apiNameTest + "is not a valid status");

    }

    @Test(groups = {"wso2.am"}, description = "Change the status of the API to DEPRECATED through" +
            " the publisher rest API ", dependsOnMethods = "testChangeTheStatusOfTheAPIToPublished")
    public void testChangeTheStatusOfTheAPIToDeprecated() throws Exception {

        //Change the status PUBLISHED to DEPRECATED
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest
                (apiNameTest, apiProvider, APILifeCycleState.DEPRECATED);
        updateRequest.setRequireResubscription("true");
        HttpResponse creationResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        assertTrue(creationResponse.getData().contains("DEPRECATED"),
                apiNameTest + "  status not updated as Prototyped");

        //Check whether deprecated API is available in publisher
        HttpResponse prototypedApiResponse = (apiPublisher.getAPI
                (apiNameTest, apiProvider, apiVersion));
        JSONObject prototypedApiObject = new JSONObject(prototypedApiResponse.getData());
        JSONObject allApiObject = new JSONObject(prototypedApiObject.getString("api"));
        assertFalse(prototypedApiObject.getBoolean("error"), apiNameTest +
                " is not visible in publisher");
        assertTrue(allApiObject.getString("name").equals(apiNameTest),
                apiNameTest + " is not visible in publisher");
        assertTrue(allApiObject.getString("status").equals("DEPRECATED"),
                "Status of the " + apiNameTest + "is not a valid status");

    }

    @Test(groups = {"wso2.am"}, description = "Change the status of the API to RETIRED through" +
            " the publisher rest API ", dependsOnMethods = "testChangeTheStatusOfTheAPIToDeprecated")
    public void testChangeTheStatusOfTheAPIToRetired() throws Exception {

        //Change the status DEPRECATED to RETIRED
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest
                (apiNameTest, apiProvider, APILifeCycleState.RETIRED);
        updateRequest.setRequireResubscription("true");
        HttpResponse creationResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        assertTrue(creationResponse.getData().contains("RETIRED"),
                apiNameTest + "  status not updated as Prototyped");

        //Check whether deprecated API is available in publisher
        HttpResponse prototypedApiResponse = (apiPublisher.getAPI
                (apiNameTest, apiProvider, apiVersion));
        JSONObject prototypedApiObject = new JSONObject(prototypedApiResponse.getData());
        JSONObject allApiObject = new JSONObject(prototypedApiObject.getString("api"));
        assertFalse(prototypedApiObject.getBoolean("error"), apiNameTest +
                " is not visible in publisher");
        assertTrue(allApiObject.getString("name").equals(apiNameTest),
                apiNameTest + " is not visible in publisher");
        assertTrue(allApiObject.getString("status").equals("RETIRED"),
                "Status of the " + apiNameTest + "is not a valid status");

    }

    @AfterClass(alwaysRun = true)
    public void cleanUp() throws APIManagerIntegrationTestException, JSONException {
        apiPublisher.deleteAPI(apiNameTest, apiVersion, apiProvider);

    }
}
