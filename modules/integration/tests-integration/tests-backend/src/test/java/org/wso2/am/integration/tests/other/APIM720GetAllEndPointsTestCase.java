/*
 *
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.*;
import org.wso2.am.admin.clients.webapp.WebAppAdminClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.am.integration.test.utils.webapp.WebAppDeploymentUtil;
import org.wso2.carbon.automation.engine.FrameworkConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;


//APIM2-720:Get all endpoint URLs of a API through the store rest api
//APIM2-722:Add a comment on an API through the store api manager
public class APIM720GetAllEndPointsTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(APIM720GetAllEndPointsTestCase.class);
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private static final String apiName = "EndPointTestAPI";
    private static final String apiVersion = "1.0.0";
    private static final String apiContext = "endpointtestapi";
    private final String tags = "document";
    private final String tier = "Unlimited";
    private final String description = "testApi";
    private String apiProvider;
    private static final String webApp = "jaxrs_basic";
    private String endpointUrl;
    private final String endPointType = "http";
    private final String visibility = "public";
    private String gatewayUrl;


    @Factory(dataProvider = "userModeDataProvider")
    public APIM720GetAllEndPointsTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
//                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception{

        String fileFormat = ".war";
        super.init(userMode);
        log.info("Test Starting user mode:" + userMode);

        //copy  .war file
        String path = TestConfigurationProvider.getResourceLocation() + File.separator +
                "artifacts" + File.separator + "AM" + File.separator + "lifecycletest" + File.separator;

        String sourcePath = path + webApp + fileFormat;

        String sessionId = createSession(gatewayContextWrk);
        WebAppAdminClient webAppAdminClient = new WebAppAdminClient(gatewayContextWrk.getContextUrls().
                getBackEndUrl(), sessionId);
        webAppAdminClient.uploadWarFile(sourcePath);
        boolean isWebAppDeployed = WebAppDeploymentUtil.isWebApplicationDeployed
                (gatewayContextWrk.getContextUrls().getBackEndUrl(), sessionId, webApp);
        assertTrue(isWebAppDeployed, "Web APP is not deployed: " + webApp);

        String storeURLHttp = storeUrls.getWebAppURLHttp();
        apiStore = new APIStoreRestClient(storeURLHttp);

        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);

        apiProvider = publisherContext.getContextTenant().getContextUser().getUserName();

        //publisher login
        HttpResponse publisherLogin = apiPublisher.login
                (publisherContext.getContextTenant().getContextUser().getUserName(),
                        publisherContext.getContextTenant().getContextUser().getPassword());
        assertEquals(publisherLogin.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Publisher Login Response Code is Mismatched: ");

        //store login
        HttpResponse loginResponse = apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());
        assertEquals(loginResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code is Mismatched in Login Response");
        JSONObject loginJsonObject = new JSONObject(loginResponse.getData());
        assertFalse(loginJsonObject.getBoolean("error"), "Response data error in Login Request");

        String uri = "customers/{id}/";
        List<APIResourceBean> resourceBeanList = new ArrayList<APIResourceBean>();
        resourceBeanList.add(new APIResourceBean("GET", "Application & Application User", tier, uri));
        String endpointProduction = "/services/customers/customerservice";
        String endpointSandbox="/services/customers/customerservice";

        List<String> prodEndpointList=new ArrayList<String>();
        prodEndpointList.add(endpointProduction);

        List<String> sandboxEndpointList=new ArrayList<String>();
        sandboxEndpointList.add(endpointSandbox);

        APICreationRequestBean apiCreationRequestBean=new APICreationRequestBean(apiName,apiContext,apiVersion,
                apiProvider,prodEndpointList,sandboxEndpointList);
        apiCreationRequestBean.setEndpointType(endPointType);
        apiCreationRequestBean.setTier(tier);
        apiCreationRequestBean.setTags(tags);
        apiCreationRequestBean.setResourceBeanList(resourceBeanList);
        apiCreationRequestBean.setDescription(description);
        apiCreationRequestBean.setVisibility(visibility);

        HttpResponse apiCreateResponse = apiPublisher.addAPI(apiCreationRequestBean);
        assertEquals(apiCreateResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Invalid Response Code");

        //assert JSON object
        JSONObject createApiJsonObject = new JSONObject(apiCreateResponse.getData());
        assertEquals(createApiJsonObject.getBoolean("error"), false, "Error in API Creation");

        HttpResponse verifyApiResponse = apiPublisher.getApi(apiName, apiProvider, apiVersion);
        JSONObject verifyApiJsonObject = new JSONObject(verifyApiResponse.getData());
        assertFalse(verifyApiJsonObject.getBoolean("error"), "Error in Verify API Response");

        //publish API
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, apiProvider,
                APILifeCycleState.PUBLISHED);

        HttpResponse statusUpdateResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        assertEquals(statusUpdateResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code is Mismatched");

        JSONObject statusUpdateJsonObject = new JSONObject(statusUpdateResponse.getData());
        assertFalse(statusUpdateJsonObject.getBoolean("error"), "API is not published");

        if (gatewayContextWrk.getContextTenant().getDomain().equals(FrameworkConstants.SUPER_TENANT_DOMAIN_NAME)) {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp();
        } else {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "t/" +
                    gatewayContextWrk.getContextTenant().getDomain() + "/";
        }
    }

    @Test(description = "Get All Endpoints")
    public void getAllEndpointUrlsTest() throws Exception{

        HttpResponse getApiResponse=apiStore.getAllPublishedAPIs();
        assertEquals(getApiResponse.getResponseCode(),Response.Status.OK.getStatusCode());
        JSONObject getApiJsonObject=new JSONObject(getApiResponse.getData());
        assertFalse(getApiJsonObject.getBoolean("error"), "Response code Mismatched in Get Api Response");
        JSONArray getApiJsonArray=getApiJsonObject.getJSONArray("apis");

        boolean isApiAvailable=false;
        boolean isEndpointUrlValid=false;
        boolean isHttpsUrlAvailable=false;
        boolean isHttpUrlAvailable=false;
        String environmentName="Production and Sandbox";
        String environmentType="hybrid";

        for(int apiIndex=0;apiIndex<getApiJsonArray.length();apiIndex++){

            if (getApiJsonArray.getJSONObject(apiIndex).getString("name").equals(apiName)) {
                isApiAvailable = true;
                HttpResponse getEndpointApiResponse = apiStore.getApiEndpointUrls(apiName, apiVersion, apiProvider);
                assertEquals(getEndpointApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                        "Error in get Endpoints Response Code");
                JSONObject getEndpointJsonObject = new JSONObject(getEndpointApiResponse.getData());
                assertFalse(getEndpointJsonObject.getBoolean("error"), "Error in End point Urls Response");
                JSONArray getEndPointUrlsJsonArray = getEndpointJsonObject.getJSONArray("endpointURLs");
                for (int index = 0; index < getEndPointUrlsJsonArray.length(); index++) {
                    if (getEndPointUrlsJsonArray.getJSONObject(index).getString("environmentURLs").contains(apiContext)) {
                        isEndpointUrlValid = true;
                        assertTrue(getEndPointUrlsJsonArray.getJSONObject(index).
                                        getString("environmentName").equalsIgnoreCase(environmentName),
                                "Error in environment Name");
                        assertTrue(getEndPointUrlsJsonArray.getJSONObject(index).
                                        getString("environmentType").equalsIgnoreCase(environmentType),
                                "Error in environment Type");
                        JSONArray environmentUrlsArray = getEndPointUrlsJsonArray.
                                getJSONObject(index).getJSONArray("environmentURLs");

                        Map<String, String> urlMap = new HashMap<String, String>();

                            for (int mapIndex = 0; mapIndex < environmentUrlsArray.length(); mapIndex++) {

                                String jsonArrayElement = environmentUrlsArray.getString(mapIndex);
                                String[] keyValue = jsonArrayElement.split("=");
                                urlMap.put(keyValue[0], keyValue[1]);
                                URL url = new URL(keyValue[1]);
                                if (keyValue[0].equals("https")) {
                                    isHttpsUrlAvailable = true;
                                    assertEquals(url.getProtocol(), keyValue[0], "Error in URL Protocol");
                                    assertEquals(url.getPath(), "/" + apiContext + "/" + apiVersion, "Error in URL Path");
                                } else if (keyValue[0].equals("http")) {
                                    isHttpUrlAvailable = true;
                                    assertEquals(url.getProtocol(), keyValue[0], "Error in URL Protocol");
                                    assertEquals(url.getPath(), "/" + apiContext + "/" + apiVersion, "Error in URL Path");
                                }
                                if(isHttpsUrlAvailable==true && isHttpUrlAvailable==true){
                                    break;
                                }

                            }




                    }
                }
                break;
            }
        }
        assertTrue(isHttpsUrlAvailable, "Error: Https Url is mismatched");
        assertTrue(isHttpUrlAvailable, "Error: Http Url is mismatched");
        assertTrue(isEndpointUrlValid, "Error: EndPoint Url is not found");
        assertTrue(isApiAvailable, "Error: Api is not available in Store");
    }

    @Test(description = "Add Comments", dependsOnMethods = "getAllEndpointUrlsTest")
    public void addCommentTest() throws Exception{
        apiProvider=storeContext.getContextTenant().getContextUser().getUserName();
        String comment="testComment";
        HttpResponse addCommentResponse=apiStore.addComment(apiName,apiVersion,apiProvider,comment);
        assertEquals(addCommentResponse.getResponseCode(),Response.Status.OK.getStatusCode(),
                "Error in Add Comment Response");
        JSONObject addCommentJsonObject=new JSONObject(addCommentResponse.getData());
        assertFalse(addCommentJsonObject.getBoolean("error"), "Error in Add Comment Response");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception{
        apiProvider=publisherContext.getContextTenant().getContextUser().getUserName();
        HttpResponse deleteApiResponse=apiPublisher.deleteAPI(apiName,apiVersion,apiProvider);
        assertEquals(deleteApiResponse.getResponseCode(),Response.Status.OK.getStatusCode(),
                "Error in Delete API Response");
        JSONObject deleteApiJsonObject=new JSONObject(deleteApiResponse.getData());
        assertFalse(deleteApiJsonObject.getBoolean("error"), "Error in Delete API");
    }
}
