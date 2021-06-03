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

import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

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

    @BeforeClass(alwaysRun = true)
    public void initialize(ITestContext ctx) throws APIManagerIntegrationTestException {
        super.init();
        providerName = user.getUserName();
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
    }

    @Test(groups = {"wso2.am"}, description = "Test  invocation of API before change the  api end point URL.")
    public void testAPIInvocationBeforeChangeTheEndPointURL(ITestContext ctx) throws Exception {
        //get access token
        String accessToken = (String) ctx.getAttribute("accessToken");
        // Create requestHeaders
        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "*/*");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        //Invoke  old version

        HttpResponse oldVersionInvokeResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(INVOKABLE_API_CONTEXT) + API1_END_POINT_METHOD,
                        requestHeaders);
        assertEquals(oldVersionInvokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke api before change the end point URL");
        assertTrue(oldVersionInvokeResponse.getData().contains(API1_RESPONSE_DATA),
                "Response data mismatched when invoke  API  before change the end point URL" +
                        " Response Data:" + oldVersionInvokeResponse.getData() + ". Expected Response Data: " + API1_RESPONSE_DATA);

    }


    @Test(groups = {"wso2.am"}, description = "Test changing of the API end point URL",
            dependsOnMethods = "testAPIInvocationBeforeChangeTheEndPointURL")
    public void testEditEndPointURL(ITestContext ctx)
            throws ApiException, ParseException, XPathExpressionException, APIManagerIntegrationTestException,
            JSONException {

        String apiId = (String) ctx.getAttribute("apiId");
        HttpResponse response = restAPIPublisher.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);

        String endPointString = "{\n" +
                "  \"production_endpoints\": {\n" +
                "    \"template_not_supported\": false,\n" +
                "    \"config\": null,\n" +
                "    \"url\": \"" + backEndServerUrl.getWebAppURLHttp() + API2_END_POINT_POSTFIX_URL + "\"\n" +
                "  },\n" +
                "  \"sandbox_endpoints\": {\n" +
                "    \"url\": \"" + backEndServerUrl.getWebAppURLHttp() + API2_END_POINT_POSTFIX_URL + "\",\n" +
                "    \"config\": null,\n" +
                "    \"template_not_supported\": false\n" +
                "  },\n" +
                "  \"endpoint_type\": \"http\"\n" +
                "}";

        JSONParser parser = new JSONParser();
        JSONObject endpoint = (JSONObject) parser.parse(endPointString);

        apidto.setEndpointConfig(endpoint);

        //Update API with Edited information
        APIDTO updateAPIHTTPResponse = restAPIPublisher.updateAPI(apidto, apiId);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);

        assertTrue(StringUtils.isNotEmpty(updateAPIHTTPResponse.getId()),
                "Update API end point URL Response Code is invalid." + getAPIIdentifierString(apiIdentifier));
        waitForAPIDeployment();
        waitForAPIDeploymentSync(apidto.getProvider(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
    }


    @Test(groups = {"wso2.am"}, description = "Test the invocation of API using new end point URL" +
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
