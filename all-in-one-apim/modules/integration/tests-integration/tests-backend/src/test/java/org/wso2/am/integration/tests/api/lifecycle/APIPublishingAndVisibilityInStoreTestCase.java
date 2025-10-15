/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.api.lifecycle;

import org.apache.http.HttpStatus;
import org.codehaus.plexus.util.StringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.APIDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.MalformedURLException;
import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Publish a API and check its visibility in the API Store.
 */
public class APIPublishingAndVisibilityInStoreTestCase extends APIManagerLifecycleBaseTest {

    private final String API_NAME = "APIPublishingAndVisibilityInStoreTest";
    private final String API_CONTEXT = "APIPublishingAndVisibilityInStore";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String apiId;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException {
        super.init();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;

    }


    @Test(groups = {"wso2.am"}, description = "Create a API and  check its availability in Publisher.")
    public void testAvailabilityOfAPIInPublisher() throws APIManagerIntegrationTestException, MalformedURLException, ApiException {


        //Create the api creation request object
        APIRequest apiRequest;
        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));

        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

        //Add the API using the API publisher.
        HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
        apiId = apiResponse.getData();

        assertEquals(apiResponse.getResponseCode(), HTTP_RESPONSE_CODE_CREATED,
                "Create API Response Code is invalid." + apiId);


        //Verify the API in API Publisher
        HttpResponse apiDto = restAPIPublisher.getAPI(apiResponse.getData());
        assertTrue(StringUtils.isNotEmpty(apiDto.getData()),
                "Added Api is not available in APi Publisher. API ID " + apiId);
    }


    @Test(groups = {"wso2.am"}, description = "Check the visibility of API in Store before the API publish. " +
            "it should not be available in store.", dependsOnMethods = "testAvailabilityOfAPIInPublisher")
    public void testVisibilityOfAPIInStoreBeforePublishing() throws Exception {

        try {
            APIDTO apiDto = restAPIStore.getAPI(apiId);
            assertFalse(StringUtils.isNotEmpty(apiDto.getId()),
                    "Api is visible in API Store before publish. API ID" + apiId);
        } catch (org.wso2.am.integration.clients.store.api.ApiException e) {
            assertEquals(e.getCode(), HttpStatus.SC_FORBIDDEN,
                    "Api is visible in API Store before publish. API ID" + apiId);
        }

    }


    @Test(groups = {"wso2.am"}, description = "Test the API publishing action. " +
            "Response HTTP message should contains API status change from  CREATED to PUBLISHED",
            dependsOnMethods = "testVisibilityOfAPIInStoreBeforePublishing")
    public void testAPIPublishing() throws Exception {
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        //Publish the API
        HttpResponse response = restAPIPublisher
                .changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);

        assertEquals(response.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API publish Response code is invalid " + apiId);

        APIDTO apiDto = restAPIStore.getAPI(apiId);

        assertFalse(StringUtils.isEmpty(apiDto.getId()),
                "Api is not visible in API Store. API ID" + apiId);
    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
    }


}
