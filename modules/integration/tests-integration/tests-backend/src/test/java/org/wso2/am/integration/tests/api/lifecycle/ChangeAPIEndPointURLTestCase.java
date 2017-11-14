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

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.xpath.XPathExpressionException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Change the API end point URL and  test the invocation.
 */
public class ChangeAPIEndPointURLTestCase extends APIManagerLifecycleBaseTest {

    private final String API1_END_POINT_METHOD = "/customers/123";
    private final String API1_RESPONSE_DATA = "<id>123</id><name>John</name></Customer>";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String INVOKABLE_API_CONTEXT = API_VERSION_1_0_0 + "/api";
    private final String API2_RESPONSE_DATA = "HelloWSO2";
    private final String API2_END_POINT_POSTFIX_URL = "name-check1_SB/name";
    private String api2EndPointUrl;

    private APIIdentifier apiIdentifier;
    private String providerName;
    private Map<String, String> requestHeaders;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException, XPathExpressionException,
                                    MalformedURLException, RemoteException {
        super.init();

        providerName = user.getUserName();
        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);
        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(user.getUserName(), user.getPassword());
        //Login to API Store with  admin
        apiStoreClientUser1.login(user.getUserName(), user.getPassword());
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
    }

    @Test(dependsOnGroups = {"webapp"}, groups = {"wso2.am"}, description = "Test  invocation of API before change the  api end point URL.")
    public void testAPIInvocationBeforeChangeTheEndPointURL() throws Exception {
        //get access token
        String accessToken = System.getProperty(APPLICATION_NAME + "-accessToken");
        // Create requestHeaders
        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "*/*");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        //Invoke  old version

        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(INVOKABLE_API_CONTEXT)  + API1_END_POINT_METHOD,
                        requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke api before change the end point URL");
        assertTrue(oldVersionInvokeResponse.getData().contains(API1_RESPONSE_DATA),
                "Response data mismatched when invoke  API  before change the end point URL" +
                        " Response Data:" + oldVersionInvokeResponse.getData() + ". Expected Response Data: " + API1_RESPONSE_DATA);

    }


    @Test(dependsOnGroups = {"webapp"}, groups = {"wso2.am"}, description = "Test changing of the API end point URL",
            dependsOnMethods = "testAPIInvocationBeforeChangeTheEndPointURL")
    public void testEditEndPointURL() throws APIManagerIntegrationTestException, MalformedURLException {
        //Create the API Request with new context
        api2EndPointUrl = getGatewayURLHttp() + API2_END_POINT_POSTFIX_URL;
        APICreationRequestBean apiCreationRequestBeanUpdate = new APICreationRequestBean(
                API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName, new URL(api2EndPointUrl));
        apiCreationRequestBeanUpdate.setTags(API_TAGS);
        apiCreationRequestBeanUpdate.setDescription(API_DESCRIPTION);
        //Update API with Edited information
        HttpResponse updateAPIHTTPResponse = apiPublisherClientUser1.updateAPI(apiCreationRequestBeanUpdate);

        assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Update API end point URL Response Code is invalid." + getAPIIdentifierString(apiIdentifier));
        assertEquals(getValueFromJSON(updateAPIHTTPResponse, "error"), "false",
                "Error in API end point URL Update in " + getAPIIdentifierString(apiIdentifier) +
                        "Response Data:" + updateAPIHTTPResponse.getData());
        waitForAPIDeployment();
    }


    @Test(dependsOnGroups = {"webapp"}, groups = {"wso2.am"}, description = "Test the invocation of API using new end point URL" +
            "  after end point URL  change", dependsOnMethods = "testEditEndPointURL")
    public void testInvokeAPIAfterChangeAPIEndPointURLWithNewEndPointURL() throws Exception {
        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(INVOKABLE_API_CONTEXT),
                        requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke  API  after change the end point URL");
        assertTrue(oldVersionInvokeResponse.getData().contains(API2_RESPONSE_DATA),
                "Response data mismatched when invoke  API  after change the end point URL" +
                        " Response Data:" + oldVersionInvokeResponse.getData() + ". Expected Response Data: " + API2_RESPONSE_DATA);
        assertFalse(oldVersionInvokeResponse.getData().contains(API1_RESPONSE_DATA),
                "Response data mismatched when invoke  API  after change the end point URL. It contains the" +
                        " Old end point URL response data. Response Data:" + oldVersionInvokeResponse.getData() +
                        ". Expected Response Data: " + API2_RESPONSE_DATA);

    }
}
