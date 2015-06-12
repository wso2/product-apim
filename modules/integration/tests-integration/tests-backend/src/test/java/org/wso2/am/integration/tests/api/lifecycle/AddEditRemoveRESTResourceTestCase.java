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
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Add , edit and remove rest resource and test the invocation of API
 */
public class AddEditRemoveRESTResourceTestCase extends APIManagerLifecycleBaseTest {
    private static final String API_NAME = "EditRemoveRESTResourceTest";
    private static final String API_CONTEXT = "EditRemoveRESTResource";
    private static final String API_TAGS = "testTag1, testTag2, testTag3";
    private static final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private static final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private static final String APPLICATION_NAME = "AddEditRemoveRESTResourceTestCase";
    private static final String RESPONSE_GET = "<id>123</id><name>John</name></Customer>";
    private static final String RESPONSE_POST = "Tom";
    private static final String API_GET_ENDPOINT_METHOD = "/customers/123";
    private static final String API_POST_ENDPOINT_METHOD = "/customers/name/";
    private static final String INVALID_URL = "/invalid";
    private static final String INVALID_URL_INVOKE_RESPONSE =
            "No matching resource found in the API for the given request";
    private String apiEndPointUrl;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private String providerName;
    private APIIdentifier apiIdentifier;
    private String postEndPointURL;
    private HashMap<String, String> requestHeadersGet;
    private HashMap<String, String> requestHeadersPost;

    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException, XPathExpressionException,
            RemoteException, MalformedURLException {
        super.init();
        postEndPointURL = gatewayWebAppUrl + API_CONTEXT + "/" + API_VERSION_1_0_0 + API_POST_ENDPOINT_METHOD;
        apiEndPointUrl = gatewayUrls.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName, new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        providerName = publisherContext.getContextTenant().getContextUser().getUserName();
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
        requestHeadersGet = new HashMap<String, String>();
        requestHeadersGet.put("accept", "text/xml");
        requestHeadersPost = new HashMap<String, String>();
        requestHeadersPost.put("accept", "text/plain");
        requestHeadersPost.put("Content-Type", "text/plain");
    }


    @Test(groups = {"wso2.am"}, description = "Test the invocation of GET resource")
    public void testInvokeGETResource() throws APIManagerIntegrationTestException, IOException {
        //Create application
        apiStoreClientUser1.addApplication(APPLICATION_NAME, TIER_GOLD, "", "");
        //Create publish and subscribe a API
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiIdentifier.setTier(TIER_GOLD);
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName, new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        createPublishAndSubscribeToAPI(
                apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1, apiStoreClientUser1, APPLICATION_NAME);
        //get the  access token
        String accessToken = generateApplicationKeys(apiStoreClientUser1, APPLICATION_NAME).getAccessToken();
        requestHeadersGet.put("Authorization", "Bearer " + accessToken);
        requestHeadersPost.put("Authorization", "Bearer " + accessToken);
        //Send GET Request
        HttpResponse httpResponse =
                HttpRequestUtil.doGet(gatewayWebAppUrl + API_CONTEXT + "/" + API_VERSION_1_0_0 + API_GET_ENDPOINT_METHOD,
                        requestHeadersGet);
        assertEquals(httpResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request");
        assertTrue(httpResponse.getData().contains(RESPONSE_GET), "Response Data not match for GET request." +
                " Expected value :\"" + RESPONSE_GET + "\" not contains in response data:\"" + httpResponse.getData() + "\"");

    }

    @Test(groups = {"wso2.am"}, description = "Test the invocation of POST resource, before adding a POSt resource",
            dependsOnMethods = "testInvokeGETResource")
    public void testInvokePOSTResourceBeforeAddingPOSTResource() throws APIManagerIntegrationTestException {
        //Send POST Request
        String exceptionMessage = "";
        try {
            HttpRequestUtil.doPost(new URL(postEndPointURL), "id=25", requestHeadersPost);
            //catching a IOException, Because “HttpRequestUtil.doPost()” throws and Exception which wraps an IOException
        } catch (Exception e) {
            exceptionMessage = e.getMessage();
        } finally {
            assertTrue(exceptionMessage.contains("java.io.IOException: Server returned HTTP response code:" +
                    " 403 for URL: " + postEndPointURL), "Not Return IOException " +
                    "with 403 when accessing a POST resource which is not define yet");
        }

    }


    @Test(groups = {"wso2.am"}, description = "Test the invocation of POST and GET resource, after adding a POST resource",
            dependsOnMethods = "testInvokePOSTResourceBeforeAddingPOSTResource")
    public void testInvokePOSTAndGETResourceAfterAddingPOSTResource() throws Exception {
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName, new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        List<APIResourceBean> apiResourceBeansList = new ArrayList<APIResourceBean>();

        APIResourceBean apiResourceBeanGET =
                new APIResourceBean("GET", "Application & Application User", "Unlimited", "/*");
        APIResourceBean apiResourceBeanPOST =
                new APIResourceBean("POST", "Application & Application User", "Unlimited", "/*");
        apiResourceBeansList.add(apiResourceBeanGET);
        apiResourceBeansList.add(apiResourceBeanPOST);
        apiCreationRequestBean.setResourceBeanList(apiResourceBeansList);
        //Update API with Edited information
        HttpResponse updateAPIHTTPResponse = apiPublisherClientUser1.updateAPI(apiCreationRequestBean);
        assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Update APi with new Resource information fail");
        assertEquals(getValueFromJSON(updateAPIHTTPResponse, "error"), "false",
                "Update APi with new Resource information fail");
        //Send GET Request
        HttpResponse httpResponseGet =
                HttpRequestUtil.doGet(gatewayWebAppUrl + API_CONTEXT + "/" + API_VERSION_1_0_0 + API_GET_ENDPOINT_METHOD,
                        requestHeadersGet);
        assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request after " +
                "update the api with  both GET and POST resource");
        assertTrue(httpResponseGet.getData().contains(RESPONSE_GET), "Response Data not match for GET request after" +
                " update the api with  both GET and POST resource. Expected value :\"" + RESPONSE_GET + "\" not contains" +
                " in response data:\"" + httpResponseGet.getData() + "\"");
        //Send POST Request
        HttpResponse httpResponsePOST = HttpRequestUtil.doPost(new URL(gatewayWebAppUrl + API_CONTEXT + "/" +
                API_VERSION_1_0_0 + API_POST_ENDPOINT_METHOD), "id=25", requestHeadersPost);

        assertEquals(httpResponsePOST.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation of  POST resource fail after update the api with  both GET and POST resource");
        assertTrue(httpResponsePOST.getData().contains(RESPONSE_POST), "Invocation of  POST resource fail after update " +
                "the api with both GET and POST resource. Expected value :\"" + RESPONSE_POST + "\" not contains in " +
                "response data:\"" + httpResponsePOST.getData() + "\"");
    }


    @Test(groups = {"wso2.am"}, description = "Test the invocation of POST and GET resource, after adding a URL pattern",
            dependsOnMethods = "testInvokePOSTAndGETResourceAfterAddingPOSTResource")
    public void testInvokePOSTAndGetResourceAfterAddingURLPattern() throws Exception {
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName, new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        //apiCreationRequestBean.setVersion(API_VERSION_1_0_0);
        apiCreationRequestBean.setVisibility("public");
        List<APIResourceBean> apiResourceBeansList = new ArrayList<APIResourceBean>();
        APIResourceBean apiResourceBeanGET =
                new APIResourceBean("GET", "Application & Application User", "Unlimited", "/customers/{id}");
        APIResourceBean apiResourceBeanPOST =
                new APIResourceBean("POST", "Application & Application User", "Unlimited", "/customers/name");
        apiResourceBeansList.add(apiResourceBeanGET);
        apiResourceBeansList.add(apiResourceBeanPOST);
        apiCreationRequestBean.setResourceBeanList(apiResourceBeansList);
        //Update API with Edited information
        HttpResponse updateAPIHTTPResponse = apiPublisherClientUser1.updateAPI(apiCreationRequestBean);
        assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Update APi with new Resource information fail");
        assertEquals(getValueFromJSON(updateAPIHTTPResponse, "error"), "false",
                "Update APi with new Resource information fail");
        //Send GET Request
        HttpResponse httpResponseGet =
                HttpRequestUtil.doGet(gatewayWebAppUrl + API_CONTEXT + "/" + API_VERSION_1_0_0 +
                        API_GET_ENDPOINT_METHOD, requestHeadersGet);
        assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request after " +
                "update the api with  URLPattern");
        assertTrue(httpResponseGet.getData().contains(RESPONSE_GET), "Response Data not match for GET request after" +
                " update the api with  URLPattern. Expected value :\"" + RESPONSE_GET + "\" not contains" +
                " in response data:\"" + httpResponseGet.getData() + "\"");
        //Send GET Request with invalid url
        HttpResponse httpResponseGetInvalidUrl =
                HttpRequestUtil.doGet(gatewayWebAppUrl + API_CONTEXT + "/" + API_VERSION_1_0_0 + API_GET_ENDPOINT_METHOD +
                        INVALID_URL, requestHeadersGet);
        assertEquals(httpResponseGetInvalidUrl.getResponseCode(), HTTP_RESPONSE_CODE_FORBIDDEN, "Invocation is not " +
                "forbidden when try to invoke GET resource  via invalid url pattern");
        assertTrue(httpResponseGetInvalidUrl.getData().contains(INVALID_URL_INVOKE_RESPONSE), "Invocation is not" +
                " forbidden when try to invoke GET resource  via invalid url pattern. Expected value :\"" +
                RESPONSE_GET + "\" not contains in response data:\"" + httpResponseGetInvalidUrl.getData() + "\"");
        //Send POST Request
        HttpResponse httpResponsePOST =
                HttpRequestUtil.doPost(new URL(gatewayWebAppUrl + API_CONTEXT + "/" + API_VERSION_1_0_0 +
                        API_POST_ENDPOINT_METHOD), "id=25", requestHeadersPost);
        assertEquals(httpResponsePOST.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation of  POST resource fail after" +
                " update the api with  URLPattern");
        assertTrue(httpResponsePOST.getData().contains(RESPONSE_POST), "Invocation of  POST resource fail after update " +
                "the api with  URLPattern. Expected value :\"" + RESPONSE_GET + "\" not contains in " + "response data:\"" +
                httpResponsePOST.getData() + "\"");

    }


    @Test(groups = {"wso2.am"}, description = "Test the invocation of POST and GET resource, after Remove POST resource",
            dependsOnMethods = "testInvokePOSTAndGetResourceAfterAddingURLPattern")
    public void testInvokeGETAndPOSTResourceAfterRemovePOSTResource() throws APIManagerIntegrationTestException, IOException {
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName, new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiCreationRequestBean.setVisibility("public");
        List<APIResourceBean> apiResourceBeansList = new ArrayList<APIResourceBean>();
        APIResourceBean apiResourceBeanGET = new APIResourceBean("GET", "Application & Application User", "Unlimited", "/*");
        apiResourceBeansList.add(apiResourceBeanGET);
        apiCreationRequestBean.setResourceBeanList(apiResourceBeansList);
        //Update API with Edited information
        HttpResponse updateAPIHTTPResponse = apiPublisherClientUser1.updateAPI(apiCreationRequestBean);
        assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Update APi with new Resource information fail");
        assertEquals(getValueFromJSON(updateAPIHTTPResponse, "error"), "false",
                "Update APi with new Resource information fail");
        //Send GET request
        HttpResponse httpResponseGet =
                HttpRequestUtil.doGet(gatewayWebAppUrl + API_CONTEXT + "/" + API_VERSION_1_0_0 + API_GET_ENDPOINT_METHOD,
                        requestHeadersGet);
        assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request after " +
                "remove the POST resource from api");
        assertTrue(httpResponseGet.getData().contains(RESPONSE_GET), "Response Data not match for GET request after" +
                " remove the POST resource from api. Expected value :\"" + RESPONSE_GET + "\" not contains" +
                " in response data:\"" + httpResponseGet.getData() + "\"");
        //Send POST Request
        String exceptionMessage = "";
        try {
            HttpRequestUtil.doPost(new URL(postEndPointURL), "id=25", requestHeadersPost);
            //catching a IOException, Because “HttpRequestUtil.doPost()” throws and Exception which wraps an IOException
        } catch (Exception e) {
            exceptionMessage = e.getMessage();
        } finally {
            assertTrue(exceptionMessage.contains("java.io.IOException: Server returned HTTP response code:" +
                    " 403 for URL: " + postEndPointURL), "Not Return IOException " +
                    "with 403 when accessing a POST resource after deleting the POST resource from API.");
        }
    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException, XPathExpressionException {
        apiStoreClientUser1.removeApplication(APPLICATION_NAME);
        deleteAPI(apiIdentifier, apiPublisherClientUser1);

    }


}
