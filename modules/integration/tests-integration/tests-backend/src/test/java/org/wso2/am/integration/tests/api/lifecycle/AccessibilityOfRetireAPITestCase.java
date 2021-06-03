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

import org.apache.commons.httpclient.HttpStatus;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.store.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * "Retire an API and check its accessibility  and visibility in the API Store."
 */
public class AccessibilityOfRetireAPITestCase extends APIManagerLifecycleBaseTest {
    private final String API_NAME = "RetireAPITest";
    private final String API_CONTEXT = "RetireAPI";
    private final String API_END_POINT_METHOD = "/customers/123";
    private final String API_RESPONSE_DATA = "<id>123</id><name>John</name></Customer>";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "AccessibilityOfRetireAPITestCase";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String apiId;
    private String applicationId;
    private ArrayList<String> grantTypes;
    private Map<String, String> requestHeaders;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        grantTypes = new ArrayList<>();

        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        assertEquals(applicationResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");

        applicationId = applicationResponse.getData();

        //Create and publish  and subscribe API version 1.0.0
        //Create the api creation request object
        APIRequest apiRequest;
        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));

        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);
        //get access token
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        // Create requestHeaders
        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + applicationKeyDTO.getToken().getAccessToken());

        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);

    }


    @Test(groups = {"wso2.am"}, description = "Test invocation of the APi before retire")
    public void testInvokeAPIBeforeChangeAPILifecycleToRetired() throws Exception {

        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttps(API_CONTEXT, API_VERSION_1_0_0) +
                        API_END_POINT_METHOD, requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke api before Retire");
        assertTrue(oldVersionInvokeResponse.getData().contains(API_RESPONSE_DATA),
                "Response data mismatched when invoke  API  before Retire" +
                        " Response Data:" + oldVersionInvokeResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Change API lifecycle to Retired",
            dependsOnMethods = "testInvokeAPIBeforeChangeAPILifecycleToRetired")
    public void testChangeAPILifecycleToDepricated() throws Exception {
        //DEPRECATE the API version 1.0.0
        //Change API lifecycle  to DEPRECATED
        HttpResponse blockAPIActionResponse = restAPIPublisher
                .changeAPILifeCycleStatus(apiId, APILifeCycleAction.DEPRECATE.getAction(), null);
        assertEquals(blockAPIActionResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertTrue(APILifeCycleState.DEPRECATED.getState().equals(blockAPIActionResponse.getData()),
                "API status Change is invalid when retire an API :" + API_NAME + " with API ID ("
                        + apiId + ")" + " Response Code:" + blockAPIActionResponse.getResponseCode());
    }

    @Test(groups = {"wso2.am"}, description = "Change API lifecycle to Retired",
            dependsOnMethods = "testChangeAPILifecycleToDepricated")
    public void testChangeAPILifecycleToRetired() throws Exception {
        //RETIRE the API version 1.0.0
        HttpResponse blockAPIActionResponse = restAPIPublisher
                .changeAPILifeCycleStatus(apiId, APILifeCycleAction.RETIRE.getAction(), null);
        assertEquals(blockAPIActionResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertTrue(APILifeCycleState.RETIRED.getState().equals(blockAPIActionResponse.getData()),
                "API status Change is invalid when retire an API :" + API_NAME + " with API ID ("
                        + apiId + ")" + " Response Code:" + blockAPIActionResponse.getResponseCode());
    }


    @Test(groups = {"wso2.am"}, description = "Test the availability of retired API in the store",
            dependsOnMethods = "testChangeAPILifecycleToRetired")
    public void testAvailabilityOfRetiredAPIInStore() {
        //  Verify the API in API Store : API should not be available in the store.
        try {
            restAPIStore.getAPI(apiId);
        } catch (ApiException e) {
            assertEquals(e.getCode(), HTTP_RESPONSE_CODE_FORBIDDEN, "Response code mismatch");
        }
    }


    @Test(groups = {"wso2.am"}, description = "Test the invocation of the API after retire",
            dependsOnMethods = "testAvailabilityOfRetiredAPIInStore")
    public void testInvokeAPIAfterChangeAPILifecycleToRetired() throws Exception {

        //Invoke  old version
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_NOT_EXISTS);

        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttps(API_CONTEXT, API_VERSION_1_0_0) +
                        API_END_POINT_METHOD, requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Response code mismatched when invoke api after retire");
        assertTrue(oldVersionInvokeResponse.getData().contains(HTTP_RESPONSE_DATA_NOT_FOUND),
                "Response data mismatched when invoke  API  after retire" +
                        " Response Data:" + oldVersionInvokeResponse.getData());

    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(apiId);
        super.cleanUp();
    }


}
