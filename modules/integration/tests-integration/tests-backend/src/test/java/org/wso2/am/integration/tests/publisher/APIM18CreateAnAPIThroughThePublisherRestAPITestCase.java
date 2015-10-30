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

import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Create an API through the Publisher Rest API and validate the API
 * APIM2-18 / APIM2-538
 */
public class APIM18CreateAnAPIThroughThePublisherRestAPITestCase extends APIMIntegrationBaseTest {
    private final String apiNameTest = "APIM18PublisherTest";
    private final String apiVersion = "1.0.0";
    private APIPublisherRestClient apiPublisher;
    private String apiProviderName;
    private String apiProductionEndPointUrl;

    @Factory(dataProvider = "userModeDataProvider")
    public APIM18CreateAnAPIThroughThePublisherRestAPITestCase(TestUserMode userMode) {
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

        apiProductionEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() +
                apiProductionEndpointPostfixUrl;
        apiProviderName = publisherContext.getContextTenant().getContextUser().getUserName();

    }

    @Test(groups = {"wso2.am"}, description = "Create an API Through the Publisher Rest API")
    public void testCreateAnAPIThroughThePublisherRest() throws Exception {

        String apiContextTest = "apim18PublisherTestAPI";
        String apiDescription = "This is Test API Created by API Manager Integration Test";
        String apiTag = "tag18-1, tag18-2, tag18-3";

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiNameTest, apiContextTest, apiVersion, apiProviderName,
                        new URL(apiProductionEndPointUrl));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setTiersCollection("Gold,Bronze");
        apiCreationRequestBean.setDefaultVersion("default_version");
        apiCreationRequestBean.setDefaultVersionChecked("default_version");
        apiCreationRequestBean.setBizOwner("api18b");
        apiCreationRequestBean.setBizOwnerMail("api18b@ee.com");
        apiCreationRequestBean.setTechOwner("api18t");
        apiCreationRequestBean.setTechOwnerMail("api18t@ww.com");

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        JSONObject apiResponse = new JSONObject(apiCreationResponse.getData());

        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when creating the API");
        assertFalse(apiResponse.getBoolean("error"), apiNameTest + "is not created as expected");

        //Check the availability of an API in Publisher
        HttpResponse response = apiPublisher.getApi(apiNameTest, apiProviderName, apiVersion);
        assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when creating the API");
        assertTrue(response.getData().contains(apiNameTest), "Invalid API Name");
        assertTrue(response.getData().contains(apiVersion), "Invalid API Version");
        assertTrue(response.getData().contains(apiContextTest), "Invalid API Context");

    }

    @Test(groups = {"wso2.am"}, description = "Remove an API Through the Publisher Rest API",
            dependsOnMethods = "testCreateAnAPIThroughThePublisherRest")
    public void testRemoveAnAPIThroughThePublisherRest() throws Exception {

        //Remove an API and validate the Response
        HttpResponse removeApiResponse = apiPublisher.deleteAPI
                (apiNameTest, apiVersion, apiProviderName);
        JSONObject jsonObjectRemoveApi = new JSONObject(removeApiResponse.getData());
        assertEquals(removeApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when removing an API");
        assertFalse(jsonObjectRemoveApi.getBoolean("error"),
                "Error when removing the " + apiNameTest);

        //Check the availability of an API  after removing
        HttpResponse response = apiPublisher.getApi(apiNameTest, apiProviderName, apiVersion);
        JSONObject jsonObjectGettingApi = new JSONObject(response.getData());
        assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when getting an API");
        assertTrue(jsonObjectGettingApi.getBoolean("error"),
                "Publisher Rest API allow to get deleted API ");
        assertEquals(jsonObjectGettingApi.getString("message"), " Cannot find the requested API- "
                + apiNameTest + "-" + apiVersion, "Invalid error " +
                "message populated when trying to get deleted API through the publisher");

    }


    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        apiPublisher.deleteAPI(apiNameTest, apiVersion, apiProviderName);
    }

}


