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

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.xpath.XPathExpressionException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * "Retire an API and check its accessibility  and visibility in the API Store."
 */
public class AccessibilityOfRetireAPITestCase extends APIManagerLifecycleBaseTest {
    private final String API_NAME = "RetireAPITest";
    private final String API_CONTEXT = "RetireAPI";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_END_POINT_METHOD = "/customers/123";
    private final String API_RESPONSE_DATA = "<id>123</id><name>John</name></Customer>";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "AccessibilityOfRetireAPITestCase";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private APIIdentifier apiIdentifier;
    private String providerName;
    private APICreationRequestBean apiCreationRequestBean;
    private Map<String, String> requestHeaders;
    private APIPublisherRestClient apiPublisherRestClient;
    private APIStoreRestClient apiStoreRestClient;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException, XPathExpressionException, MalformedURLException {
        super.init();
        apiEndPointUrl = getGatewayURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                        new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();
        apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);
        apiStoreRestClient = new APIStoreRestClient(storeURLHttp);
        //Login to API Publisher with  admin
        apiPublisherRestClient.login(user.getUserName(), user.getPassword());
        //Login to API Store with  admin
        apiStoreRestClient.login(user.getUserName(), user.getPassword());
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiStoreRestClient
                .addApplication(APPLICATION_NAME, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");
    }


    @Test(groups = {"wso2.am"}, description = "Test invocation of the APi before retire")
    public void testInvokeAPIBeforeChangeAPILifecycleToRetired() throws Exception {
        //Create and publish  and subscribe API version 1.0.0
        createPublishAndSubscribeToAPI(apiIdentifier, apiCreationRequestBean,
                                       apiPublisherRestClient, apiStoreRestClient, APPLICATION_NAME);
        //get access token
        String accessToken = generateApplicationKeys(apiStoreRestClient, APPLICATION_NAME).getAccessToken();
        // Create requestHeaders
        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0,
                                 APIMIntegrationConstants.IS_API_EXISTS);

        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT,  API_VERSION_1_0_0) +
                        API_END_POINT_METHOD, requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke api before Retire");
        assertTrue(oldVersionInvokeResponse.getData().contains(API_RESPONSE_DATA),
                "Response data mismatched when invoke  API  before Retire" +
                        " Response Data:" + oldVersionInvokeResponse.getData());
        Thread.sleep(1000); //This is required to set a time difference between timestamps of current state and next
    }

    @Test(groups = {"wso2.am"}, description = "Change API lifecycle to Retired",
            dependsOnMethods = "testInvokeAPIBeforeChangeAPILifecycleToRetired") 
    public void testChangeAPILifecycleToDepricated() throws Exception {
        //DEPRECATE the API version 1.0.0
        APILifeCycleStateRequest deprecateUpdateRequest =
                new APILifeCycleStateRequest(API_NAME, providerName, APILifeCycleState.DEPRECATED);
        deprecateUpdateRequest.setVersion(API_VERSION_1_0_0);
        //Change API lifecycle  to DEPRECATED
        HttpResponse blockAPIActionResponse =
                apiPublisherRestClient.changeAPILifeCycleStatus(deprecateUpdateRequest);
        assertEquals(blockAPIActionResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertTrue(verifyAPIStatusChange(blockAPIActionResponse, APILifeCycleState.PUBLISHED,
                APILifeCycleState.DEPRECATED), "API status Change is invalid when retire an API :" +
                getAPIIdentifierString(apiIdentifier) +
                " Response Code:" + blockAPIActionResponse.getData());
        Thread.sleep(1000); //This is required to set a time difference between timestamps of current state and next
    }

    @Test(groups = {"wso2.am"}, description = "Change API lifecycle to Retired",
            dependsOnMethods = "testChangeAPILifecycleToDepricated") 
    public void testChangeAPILifecycleToRetired() throws APIManagerIntegrationTestException {
        //RETIRE the API version 1.0.0
        APILifeCycleStateRequest retireUpdateRequest =
                new APILifeCycleStateRequest(API_NAME, providerName, APILifeCycleState.RETIRED);
        retireUpdateRequest.setVersion(API_VERSION_1_0_0);
        //Change API lifecycle  to RETIRED
        HttpResponse blockAPIActionResponse =
                apiPublisherRestClient.changeAPILifeCycleStatus(retireUpdateRequest);
        assertEquals(blockAPIActionResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertTrue(verifyAPIStatusChange(blockAPIActionResponse, APILifeCycleState.DEPRECATED,
                APILifeCycleState.RETIRED), "API status Change is invalid when retire an API :" +
                getAPIIdentifierString(apiIdentifier) +
                " Response Code:" + blockAPIActionResponse.getData());
    }


    @Test(groups = {"wso2.am"}, description = "Test the availability of retired API in the store",
            dependsOnMethods = "testChangeAPILifecycleToRetired")
    public void testAvailabilityOfRetiredAPIInStore() throws Exception {
        //  Verify the API in API Store : API should not be available in the store.
        List<APIIdentifier> apiStoreAPIIdentifierList = APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(
                apiStoreRestClient.getAPI());
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList),
                "Api is  visible in API Store after retire." + getAPIIdentifierString(apiIdentifier));
    }


    @Test(groups = {"wso2.am"}, description = "Test the invocation of the API after retire",
            dependsOnMethods = "testAvailabilityOfRetiredAPIInStore")
    public void testInvokeAPIAfterChangeAPILifecycleToRetired() throws Exception {

        //Invoke  old version
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0,
                                 APIMIntegrationConstants.IS_API_NOT_EXISTS);

        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0)  +
                        API_END_POINT_METHOD, requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Response code mismatched when invoke api after retire");
        assertTrue(oldVersionInvokeResponse.getData().contains(HTTP_RESPONSE_DATA_NOT_FOUND),
                "Response data mismatched when invoke  API  after retire" +
                        " Response Data:" + oldVersionInvokeResponse.getData());

    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException {
        apiStoreRestClient.removeApplication(APPLICATION_NAME);
        deleteAPI(apiIdentifier, apiPublisherRestClient);
    }


}
