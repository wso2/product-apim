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
 * Update an API through the REST api
 */

public class APIM520UpdateAnAPIThroughThePublisherRestAPITestCase extends APIMIntegrationBaseTest {

    private final String apiNameTest = "APIM520PublisherTest";
    private final String apiVersion = "1.0.0";
    private APIPublisherRestClient apiPublisher;
    private String apiProviderName;
    private String apiProductionEndPointUrl;

    @Factory(dataProvider = "userModeDataProvider")
    public APIM520UpdateAnAPIThroughThePublisherRestAPITestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
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

    @Test(groups = {"wso2.am"}, description = "Update an API Through the Publisher Rest API")
    public void testUpdateAnAPIThroughThePublisherRest() throws Exception {

        String apiContextTest = "apim520PublisherTestAPI";
        String apiDescription = "This is Test API Created by API Manager Integration Test";
        String apiTag = "tag520-1, tag520-2, tag520-3";

        //Create an API
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(apiNameTest, apiContextTest, apiVersion, apiProviderName,
                        new URL(apiProductionEndPointUrl));
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setTiersCollection("Gold,Bronze");
        apiCreationRequestBean.setDefaultVersion("default_version");
        apiCreationRequestBean.setDefaultVersionChecked("default_version");
        apiCreationRequestBean.setBizOwner("api520b");
        apiCreationRequestBean.setBizOwnerMail("api520b@ee.com");
        apiCreationRequestBean.setTechOwner("api520t");
        apiCreationRequestBean.setTechOwnerMail("api520t@ww.com");

        HttpResponse apiCreationResponse = apiPublisher.addAPI(apiCreationRequestBean);
        JSONObject apiResponse = new JSONObject(apiCreationResponse.getData());
        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when creating the API");
        assertFalse(apiResponse.getBoolean("error"), apiNameTest + "is not created as expected");

        //Check availability of the API in publisher
        HttpResponse apiResponsePublisher = apiPublisher.getAPI
                (apiNameTest, apiProviderName, apiVersion);
        JSONObject jsonObject = new JSONObject(apiResponsePublisher.getData());
        assertFalse(jsonObject.getBoolean("error"), apiNameTest + " is not visible in publisher");
        assertTrue(apiResponsePublisher.getData().contains(apiNameTest),
                apiNameTest + " is not visible in publisher");

        //Update API with the description and tiersCollection & validate the result
        apiCreationRequestBean.setDescription("Description Changed");
        apiCreationRequestBean.setTiersCollection("Unlimited,Gold,Bronze");

        HttpResponse apiUpdateResponse = apiPublisher.updateAPI(apiCreationRequestBean);
        assertTrue(apiUpdateResponse.getData().contains("\"error\" : false"),
                apiNameTest + " is not updated properly");
        waitForAPIDeployment();

        //Check whether API is updated from the above request
        HttpResponse apiUpdateResponsePublisher = apiPublisher.getAPI
                (apiNameTest, apiProviderName, apiVersion);
        assertTrue(apiUpdateResponsePublisher.getData().contains(apiNameTest),
                apiNameTest + " is not updated");
        assertTrue(apiUpdateResponsePublisher.getData().contains("Description Changed"),
                "Description of the " + apiNameTest + " is not updated");
        assertTrue(apiUpdateResponsePublisher.getData().contains("Unlimited"),
                "Tier Collection of the " + apiNameTest + " is not updated");
        assertTrue(apiUpdateResponsePublisher.getData().contains("Bronze"),
                "Tier Collection of the " + apiNameTest + " is not updated");
        assertTrue(apiUpdateResponsePublisher.getData().contains("Gold"),
                "Tier Collection of the " + apiNameTest + " is not updated");

    }

    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        apiPublisher.deleteAPI(apiNameTest, apiVersion, apiProviderName);
        super.cleanUp();
    }


}
