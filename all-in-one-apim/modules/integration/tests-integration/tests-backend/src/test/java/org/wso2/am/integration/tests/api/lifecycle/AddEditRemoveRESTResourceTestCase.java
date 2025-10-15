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
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.exceptions.AutomationFrameworkException;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Add , edit and remove rest resource and test the invocation of API
 */
public class AddEditRemoveRESTResourceTestCase extends APIManagerLifecycleBaseTest {
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String INVOKABLE_API_CONTEXT = API_VERSION_1_0_0 + "/api";

    private final String RESPONSE_GET = "<id>123</id><name>John</name></Customer>";
    private final String RESPONSE_POST = "Tom";
    private final String API_GET_ENDPOINT_METHOD = "/customers/123";
    private final String API_POST_ENDPOINT_METHOD = "/customers/name/";
    private final String INVALID_URL = "/invalid";
    private final String INVALID_RESOURCE_INVOCATION =
            "No matching resource found for given API Request";

    private String apiEndPointUrl;
    private String providerName;
    private String postEndPointURL;
    private HashMap<String, String> requestHeadersGet;
    private HashMap<String, String> requestHeadersPost;
    private ITestContext ctx;

    @BeforeClass(alwaysRun = true)
    public void initialize(ITestContext ctx) throws APIManagerIntegrationTestException, XPathExpressionException, JSONException, ApiException {
        super.init();
        postEndPointURL = getAPIInvocationURLHttp(INVOKABLE_API_CONTEXT) + API_POST_ENDPOINT_METHOD;
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();

        requestHeadersGet = new HashMap<String, String>();
        requestHeadersGet.put("accept", "text/xml");
        requestHeadersPost = new HashMap<String, String>();
        requestHeadersPost.put("accept", "text/plain");
        requestHeadersPost.put("Content-Type", "text/plain");
        this.ctx = ctx;
        String apiId = (String) ctx.getAttribute("apiId");
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();
    }


    @Test(groups = {"webapp"}, description = "Test the invocation of GET resource")
    public void testInvokeGETResource(ITestContext ctx) throws Exception {
        String accessToken = (String) ctx.getAttribute("accessToken");
        requestHeadersGet.put("Authorization", "Bearer " + accessToken);
        requestHeadersPost.put("Authorization", "Bearer " + accessToken);
        //Send GET Request

        HttpResponse httpResponse =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(INVOKABLE_API_CONTEXT) + API_GET_ENDPOINT_METHOD,
                        requestHeadersGet);
        assertEquals(httpResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request");
        assertTrue(httpResponse.getData().contains(RESPONSE_GET), "Response Data not match for GET request." +
                " Expected value :\"" + RESPONSE_GET + "\" not contains in response data:\"" + httpResponse.getData() + "\"");

    }

    @Test(groups = {"webapp"}, description = "Test the invocation of POST resource, before adding a POSt resource",
            dependsOnMethods = "testInvokeGETResource")
    public void testInvokePOSTResourceBeforeAddingPOSTResource()
            throws APIManagerIntegrationTestException, MalformedURLException {
        //Send POST Request
        String exceptionMessage = "";
        try {
            HttpRequestUtil.doPost(new URL(postEndPointURL), "id=25", requestHeadersPost);
            //catching a IOException, Because “HttpRequestUtil.doPost()” throws and Exception which wraps an IOException
        } catch (AutomationFrameworkException e) {
            exceptionMessage = e.getMessage();
        } finally {
            assertTrue(exceptionMessage.contains("Server returned HTTP response code: 405"), "Not Return IOException with 405 when accessing a " +
                    "POST resource which is not define yet. "
                    + exceptionMessage);
            assertTrue(exceptionMessage.contains(INVOKABLE_API_CONTEXT), "API Context is not in error message " + exceptionMessage);
        }

    }


    @Test(groups = {"webapp"}, description = "Test the invocation of POST and GET resource, after adding a POST resource",
            dependsOnMethods = "testInvokePOSTResourceBeforeAddingPOSTResource")
    public void testInvokePOSTAndGETResourceAfterAddingPOSTResource() throws Exception {


        String apiId = (String) ctx.getAttribute("apiId");
        HttpResponse response = restAPIPublisher.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);


        List<APIOperationsDTO> operation = apidto.getOperations();
        APIOperationsDTO apiOperationsDTO2 = new APIOperationsDTO();
        apiOperationsDTO2.setVerb("POST");
        apiOperationsDTO2.setTarget("/customers/name/");
        apiOperationsDTO2.setAuthType("Application & Application User");
        apiOperationsDTO2.setThrottlingPolicy("Unlimited");
        operation.add(apiOperationsDTO2);

        APIDTO updateReponse = restAPIPublisher.updateAPI(apidto, apiId);
//        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
//        waitForAPIDeployment();

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeployment();


        assertTrue(StringUtils.isNotEmpty(updateReponse.getId()), "Update APi with new Resource information fail");
        //Send GET Request
        waitForAPIDeployment();
        waitForAPIDeploymentSync(apidto.getProvider(), apidto.getName(), apidto.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);
        HttpResponse httpResponseGet =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(INVOKABLE_API_CONTEXT) + API_GET_ENDPOINT_METHOD,
                        requestHeadersGet);
        assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request after " +
                "update the api with  both GET and POST resource");
        assertTrue(httpResponseGet.getData().contains(RESPONSE_GET), "Response Data not match for GET request after" +
                " update the api with  both GET and POST resource. Expected value :\"" + RESPONSE_GET + "\" not contains" +
                " in response data:\"" + httpResponseGet.getData() + "\"");
        //Send POST Request
        HttpResponse httpResponsePOST = HttpRequestUtil.doPost(new URL(getAPIInvocationURLHttp(INVOKABLE_API_CONTEXT)
                + API_POST_ENDPOINT_METHOD), "id=25", requestHeadersPost);

        assertEquals(httpResponsePOST.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation of  POST resource fail after update the api with  both GET and POST resource");
        assertTrue(httpResponsePOST.getData().contains(RESPONSE_POST), "Invocation of  POST resource fail after update " +
                "the api with both GET and POST resource. Expected value :\"" + RESPONSE_POST + "\" not contains in " +
                "response data:\"" + httpResponsePOST.getData() + "\"");
    }


    @Test(groups = {"webapp"}, description = "Test the invocation of POST and GET resource, after adding a URL pattern",
            dependsOnMethods = "testInvokePOSTAndGETResourceAfterAddingPOSTResource")
    public void testInvokePOSTAndGetResourceAfterAddingURLPattern() throws Exception {

        //Send GET Request
        HttpResponse httpResponseGet =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(INVOKABLE_API_CONTEXT) +
                        API_GET_ENDPOINT_METHOD, requestHeadersGet);
        assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request after " +
                "update the api with  URLPattern");
        assertTrue(httpResponseGet.getData().contains(RESPONSE_GET), "Response Data not match for GET request after" +
                " update the api with  URLPattern. Expected value :\"" + RESPONSE_GET + "\" not contains" +
                " in response data:\"" + httpResponseGet.getData() + "\"");
        //Send GET Request with invalid url
        HttpResponse httpResponseGetInvalidUrl =
                HttpRequestUtil.doGet(getAPIInvocationURLHttp(INVOKABLE_API_CONTEXT) + API_GET_ENDPOINT_METHOD +
                        INVALID_URL, requestHeadersGet);
        assertEquals(httpResponseGetInvalidUrl.getResponseCode(), HTTP_RESPONSE_CODE_NOT_FOUND, "Invocation is not " +
                "forbidden when try to invoke GET resource  via invalid url pattern");
        assertTrue(httpResponseGetInvalidUrl.getData().contains(INVALID_RESOURCE_INVOCATION), "Invocation is not" +
                " forbidden when try to invoke GET resource  via invalid url pattern. Expected value :\"" +
                RESPONSE_GET + "\" not contains in response data:\"" + httpResponseGetInvalidUrl.getData() + "\"");
        //Send POST Request
        HttpResponse httpResponsePOST =
                HttpRequestUtil.doPost(new URL(getAPIInvocationURLHttp(INVOKABLE_API_CONTEXT) +
                        API_POST_ENDPOINT_METHOD), "id=25", requestHeadersPost);
        assertEquals(httpResponsePOST.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation of  POST resource fail after" +
                " update the api with  URLPattern");
        assertTrue(httpResponsePOST.getData().contains(RESPONSE_POST), "Invocation of  POST resource fail after update " +
                "the api with  URLPattern. Expected value :\"" + RESPONSE_GET + "\" not contains in " + "response data:\"" +
                httpResponsePOST.getData() + "\"");

    }


}
