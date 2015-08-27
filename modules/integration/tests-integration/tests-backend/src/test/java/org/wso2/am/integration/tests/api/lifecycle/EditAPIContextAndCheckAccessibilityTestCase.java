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
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Edit the API context and check its accessibility.
 */
public class EditAPIContextAndCheckAccessibilityTestCase extends APIManagerLifecycleBaseTest {
    private final String API_NAME = "EditAPIContextAndCheckAccessibilityTest";
    private final String API_CONTEXT = "EditAPIContextAndCheckAccessibility";
    private final String API_TAGS = "testTag1, testTag2, testTag3";

    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_END_POINT_METHOD = "customers/123";
    private final String API_RESPONSE_DATA = "<id>123</id><name>John</name></Customer>";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "EditAPIContextAndCheckAccessibilityTestCase";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String providerName;
    private APIIdentifier apiIdentifier;
    private Map<String, String> requestHeaders;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private APICreationRequestBean apiCreationRequestBean;
    private String newContext;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException, XPathExpressionException, MalformedURLException {
        super.init();
        apiEndPointUrl = getGatewayURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                new URL(apiEndPointUrl));

        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);

        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(user.getUserName(), user.getPassword());

        //Login to API Store with  admin
        apiStoreClientUser1.login(user.getUserName(), user.getPassword());
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiStoreClientUser1.addApplication(APPLICATION_NAME, TIER_UNLIMITED, "", "");
    }


    @Test(groups = {"wso2.am"}, description = "Test invoke the API before the context change")
    public void testInvokeAPIBeforeChangeAPIContext() throws Exception {
        //Create and publish  and subscribe API version 1.0.0
        createPublishAndSubscribeToAPI(apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1,
                apiStoreClientUser1, APPLICATION_NAME);
        //get access token
        String accessToken = generateApplicationKeys(apiStoreClientUser1, APPLICATION_NAME).getAccessToken();
        // Create requestHeaders
        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        //Invoke  old version
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0)  + "/" +
                        API_END_POINT_METHOD, requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke api before change the context");
        assertTrue(oldVersionInvokeResponse.getData().contains(API_RESPONSE_DATA),
                "Response data mismatched when invoke  API  before change the context" +
                        " Response Data:" + oldVersionInvokeResponse.getData());
    }


    @Test(groups = {"wso2.am"}, description = "Test changing of the API context",
            dependsOnMethods = "testInvokeAPIBeforeChangeAPIContext")
    public void testEditAPIContext() throws APIManagerIntegrationTestException, MalformedURLException {
        //Create the API Request with new context
        newContext = "new" + API_CONTEXT;
        apiCreationRequestBean = new APICreationRequestBean(API_NAME, newContext, API_VERSION_1_0_0,
                providerName, new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        //Update API with Edited information
        HttpResponse updateAPIHTTPResponse = apiPublisherClientUser1.updateAPI(apiCreationRequestBean);
        assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Update API Response Code is invalid." + getAPIIdentifierString(apiIdentifier));
        assertEquals(getValueFromJSON(updateAPIHTTPResponse, "error"), "false",
                "Error in API Update in " + getAPIIdentifierString(apiIdentifier) +
                        "Response Data:" + updateAPIHTTPResponse.getData());
    }


    @Test(groups = {"wso2.am"}, description = "Test the invocation of API using old context after Context change" +
            " after the API context change", dependsOnMethods = "testEditAPIContext")
    public void testInvokeAPIAfterChangeAPIContextWithOldContext() throws Exception {
        //Invoke  old context
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0)  + "/" +
                        API_END_POINT_METHOD, requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND,
                "Response code mismatched when invoke api before changing the context");
        assertTrue(oldVersionInvokeResponse.getData().contains(HTTP_RESPONSE_DATA_NOT_FOUND),
                "Response data mismatched when invoke  API  before changing the context" +
                        " Response Data:" + oldVersionInvokeResponse.getData());
    }


    @Test(groups = {"wso2.am"}, description = "Test the invocation of API using new context after Context change",
            dependsOnMethods = "testInvokeAPIAfterChangeAPIContextWithOldContext")
    public void testInvokeAPIAfterChangeAPIContextWithNewContext() throws Exception {
        //Invoke  new context
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(newContext, API_VERSION_1_0_0)  + "/" +
                        API_END_POINT_METHOD, requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke api after changing the context");
        assertTrue(oldVersionInvokeResponse.getData().contains(API_RESPONSE_DATA),
                "Response data mismatched when invoke  API  after changing the context" +
                        " Response Data:" + oldVersionInvokeResponse.getData());
    }

    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException {
        apiStoreClientUser1.removeApplication(APPLICATION_NAME);
        deleteAPI(apiIdentifier, apiPublisherClientUser1);

    }


}




