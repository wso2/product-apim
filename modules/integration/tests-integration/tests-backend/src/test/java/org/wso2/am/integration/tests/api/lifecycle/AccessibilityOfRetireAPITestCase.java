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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * "Retire an API and check its accessibility  and visibility in the API Store."
 */
public class AccessibilityOfRetireAPITestCase extends APIManagerLifecycleBaseTest {
    private static final String API_NAME = "RetireAPITest";
    private static final String API_CONTEXT = "RetireAPI";
    private static final String API_TAGS = "testTag1, testTag2, testTag3";
    private static final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private static final String API_END_POINT_METHOD = "/customers/123";
    private static final String API_RESPONSE_DATA = "<id>123</id><name>John</name></Customer>";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private static final String APPLICATION_NAME = "AccessibilityOfRetireAPITestCase";
    private static final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private APIIdentifier apiIdentifier;
    private String providerName;
    private APICreationRequestBean apiCreationRequestBean;
    private Map<String, String> requestHeaders;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException, XPathExpressionException, MalformedURLException {
        super.init();
        apiEndPointUrl = gatewayUrls.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                        new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);
        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        //Login to API Store with  admin
        apiStoreClientUser1.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiStoreClientUser1.addApplication(APPLICATION_NAME, "", "", "");
    }


    @Test(groups = {"wso2.am"}, description = "Test invocation of the APi before retire")
    public void testInvokeAPIBeforeChangeAPILifecycleToRetired() throws APIManagerIntegrationTestException, IOException {
        //Create and publish  and subscribe API version 1.0.0
        createPublishAndSubscribeToAPI(apiIdentifier, apiCreationRequestBean,
                apiPublisherClientUser1, apiStoreClientUser1, APPLICATION_NAME);
        //get access token
        String accessToken = generateApplicationKeys(apiStoreClientUser1, APPLICATION_NAME).getAccessToken();
        // Create requestHeaders
        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        //Invoke  old version
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(GATEWAY_WEB_APP_URL + API_CONTEXT + "/" + API_VERSION_1_0_0 +
                        API_END_POINT_METHOD, requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke api before Retire");
        assertTrue(oldVersionInvokeResponse.getData().contains(API_RESPONSE_DATA),
                "Response data mismatched when invoke  API  before Retire" +
                        " Response Data:" + oldVersionInvokeResponse.getData());
    }


    @Test(groups = {"wso2.am"}, description = "Change API lifecycle to Retired",
            dependsOnMethods = "testInvokeAPIBeforeChangeAPILifecycleToRetired")
    public void testChangeAPILifecycleToRetired() throws APIManagerIntegrationTestException {
        //Block the API version 1.0.0
        APILifeCycleStateRequest blockUpdateRequest =
                new APILifeCycleStateRequest(API_NAME, providerName, APILifeCycleState.RETIRED);
        blockUpdateRequest.setVersion(API_VERSION_1_0_0);
        //Change API lifecycle  to Block
        HttpResponse blockAPIActionResponse =
                apiPublisherClientUser1.changeAPILifeCycleStatus(blockUpdateRequest);
        assertEquals(blockAPIActionResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertTrue(verifyAPIStatusChange(blockAPIActionResponse, APILifeCycleState.PUBLISHED,
                APILifeCycleState.RETIRED), "API status Change is invalid when retire an API :" +
                getAPIIdentifierString(apiIdentifier) +
                " Response Code:" + blockAPIActionResponse.getData());
    }


    @Test(groups = {"wso2.am"}, description = "Test the availability of retired API in the store",
            dependsOnMethods = "testChangeAPILifecycleToRetired")
    public void testAvailabilityOfRetiredAPIInStore() throws APIManagerIntegrationTestException {
        //  Verify the API in API Store : API should not be available in the store.
        List<APIIdentifier> apiStoreAPIIdentifierList = APIMTestCaseUtils.getAPIIdentifierListFromHttpResponse(
                apiStoreClientUser1.getAPI());
        assertFalse(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList),
                "Api is  visible in API Store after retire." + getAPIIdentifierString(apiIdentifier));
    }


    @Test(groups = {"wso2.am"}, description = "Test the invocation of the API after retire",
            dependsOnMethods = "testAvailabilityOfRetiredAPIInStore")
    public void testInvokeAPIAfterChangeAPILifecycleToRetired() throws APIManagerIntegrationTestException, IOException {

        //Invoke  old version
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(GATEWAY_WEB_APP_URL + API_CONTEXT + "/" + API_VERSION_1_0_0 +
                        API_END_POINT_METHOD, requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Response code mismatched when invoke api after retire");
        assertTrue(oldVersionInvokeResponse.getData().contains(HTTP_RESPONSE_DATA_NOT_FOUND),
                "Response data mismatched when invoke  API  after retire" +
                        " Response Data:" + oldVersionInvokeResponse.getData());

    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException {
        apiStoreClientUser1.removeApplication(APPLICATION_NAME);
        deleteAPI(apiIdentifier, apiPublisherClientUser1);
    }


}
