
/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.am.integration.clients.publisher.api.ApiException;

import java.net.URL;
import java.net.MalformedURLException;

import static org.testng.Assert.assertEquals;

/**
 * Update the API with Rest API with null values for specific fields and
 * check if bad request response occurs
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE })
public class UpdateAPINullPointerTestCase extends APIManagerLifecycleBaseTest {

    private final Log log = LogFactory.getLog(UpdateAPINullPointerTestCase.class);
    private final String API_NAME = "nullPointerTestApi";
    private final String API_DESCRIPTION = "This is a test API to check for null pointers when uodating api using rest apis";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String apiId;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;

        APIRequest apiRequest;
        apiRequest = new APIRequest(API_NAME, API_NAME.toLowerCase(), new URL(apiEndPointUrl));

        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setDescription(API_DESCRIPTION);


        apiId = createAndPublishAPIWithoutRequireReSubscriptionUsingRest(apiRequest, restAPIPublisher);

        waitForAPIDeploymentSync(apiRequest.getProvider(),
                apiRequest.getName(),
                apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        log.info("api created successfully " + apiId);
    }

    @Test(groups = {"wso2.am"}, description = "Set the endpointConfig parameter as null and test for Bad Request")
    public void testBadRequestWithSecuritySchemeAsNull() throws APIManagerIntegrationTestException, ApiException, MalformedURLException {
        APIRequest apiRequest;
        apiRequest = new APIRequest(API_NAME, API_NAME.toLowerCase(), new URL(apiEndPointUrl));
        apiRequest.setSecurityScheme(null);
        HttpResponse updateAPIHTTPResponse = restAPIPublisher.updateAPI(apiRequest, apiId);
        waitForAPIDeployment();
        assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Update API Response Code is invalid. API Name:" + API_NAME);
    }

    @Test(groups = {"wso2.am"}, description = "Set the endpointConfig parameter as null and test for Bad Request")
    public void testBadRequestWithEndpointConfigAsNull() throws APIManagerIntegrationTestException, ApiException, MalformedURLException {
        APIRequest apiRequest;
        apiRequest = new APIRequest(API_NAME, API_NAME.toLowerCase(), new URL(apiEndPointUrl));
        apiRequest.setEndpoint(null);
        HttpResponse updateAPIHTTPResponse = restAPIPublisher.updateAPI(apiRequest, apiId);
        waitForAPIDeployment();
        assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Update API Response Code is invalid. API Name:" + API_NAME);
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        restAPIPublisher.deleteAPI(apiId);
    }

}
