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
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
    private String apiProviderName;
    private String apiProductionEndPointUrl;
    private String apiId;

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


        apiProductionEndPointUrl = gatewayUrlsWrk.getWebAppURLHttp() +
                apiProductionEndpointPostfixUrl;
        apiProviderName = publisherContext.getContextTenant().getContextUser().getUserName();

    }

    @Test(groups = {"wso2.am"}, description = "Create an API Through the Publisher Rest API")
    public void testCreateAnAPIThroughThePublisherRest() throws Exception {

        String apiContextTest = "apim18PublisherTestAPI";
        String apiDescription = "This is Test API Created by API Manager Integration Test";
        String apiTag = "tag18-1, tag18-2, tag18-3";
        
        APIRequest apiCreationRequestBean;
        apiCreationRequestBean = new APIRequest(apiNameTest, apiContextTest, new URL(apiProductionEndPointUrl));

        apiCreationRequestBean.setVersion(apiVersion);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setTags(apiTag);
        apiCreationRequestBean.setTiersCollection("Gold,Bronze");
        apiCreationRequestBean.setTier("Gold");

        APIOperationsDTO apiOperationsDTO = new APIOperationsDTO();
        apiOperationsDTO.setVerb("GET");
        apiOperationsDTO.setTarget("/customers/{id}");

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTO);
        apiCreationRequestBean.setOperationsDTOS(operationsDTOS);
        apiCreationRequestBean.setOperationsDTOS(operationsDTOS);
        apiCreationRequestBean.setDefault_version_checked("true");;

        apiCreationRequestBean.setBusinessOwner("api18b");
        apiCreationRequestBean.setBusinessOwnerEmail("api18b@ee.com");
        apiCreationRequestBean.setTechnicalOwner("api18t");
        apiCreationRequestBean.setTechnicalOwnerEmail("api18t@ww.com");

        apiCreationRequestBean.setOperationsDTOS(operationsDTOS);
        HttpResponse apiCreationResponse = restAPIPublisher.addAPI(apiCreationRequestBean);
        apiId = apiCreationResponse.getData();

        assertEquals(apiCreationResponse.getResponseCode(), Response.Status.CREATED.getStatusCode(),
                "Response Code miss matched when creating the API");


        //Check the availability of an API in Publisher
        HttpResponse response = restAPIPublisher.getAPI(apiId);
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
        HttpResponse removeApiResponse = restAPIPublisher.deleteAPI(apiId);
        assertEquals(removeApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code miss matched when removing an API");

        //Check the availability of an API  after removing
        
        HttpResponse response = restAPIPublisher.getAPI(apiId);
        assertEquals(response.getResponseCode(), Response.Status.NOT_FOUND.getStatusCode(),
                "Status code mismatch");

    }


    @AfterClass(alwaysRun = true)
    public void destroyAPIs() throws Exception {
        super.cleanUp();
    }

}


