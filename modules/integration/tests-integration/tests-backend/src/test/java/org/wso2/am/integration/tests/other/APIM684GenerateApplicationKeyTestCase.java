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
import org.json.JSONObject;
import org.testng.annotations.*;
import org.wso2.am.admin.clients.webapp.WebAppAdminClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.am.integration.test.utils.webapp.WebAppDeploymentUtil;
import org.wso2.carbon.automation.engine.FrameworkConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

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

public class APIM684GenerateApplicationKeyTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(APIM684GenerateApplicationKeyTestCase.class);
    private APIStoreRestClient apiStore;
    private APIPublisherRestClient apiPublisher;
    private String apiName="APISubscriptionTestAPI";
    private final String version = "1.0.0";
    private final String context = "testScopeAPI";
    private final String tags = "testtag1,teasttag2";
    private final String apiDescription = "TestApiDescription";
    private String apiProvider;
    private String visibility = "public";
    private String endPointType = "http";
    private String tier = "Unlimited";
    private final String webApp = "jaxrs_basic";
    private String applicationName="NewApplication";
    private final String applicationDescription = "NewKeyGeneration";
    private final String callbackUrl = "http://myserver.com";
    private String endpointUrl="";
    List<APIResourceBean> resourceBeanList;
    private UserManagementClient userManagementClient;
    private static final String subscriber_role="subscriber";
//    private static final String apiCreator_role="apiCreator";
    private static final String admin_user="wso2admin";
    private static final String test_user="testUser";
    APICreationRequestBean apiCreationRequestBean;


    @Factory(dataProvider = "userModeDataProvider")
    public APIM684GenerateApplicationKeyTestCase(TestUserMode userMode){
        this.userMode=userMode;
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

        HttpResponse publisherLogin=apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        assertEquals(publisherLogin.getResponseCode(),Response.Status.OK.getStatusCode(),
                "Publisher Login Response Code is Mismatched: ");

        HttpResponse loginResponse = apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());
        assertEquals(loginResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code is Mismatched in Login Response");
        JSONObject loginJsonObject = new JSONObject(loginResponse.getData());
        assertFalse(loginJsonObject.getBoolean("error"), "Response data error in Login Request");

        String uriGet = "customers/{id}/";
        String uriPost = "customers/name/";
        resourceBeanList = new ArrayList<APIResourceBean>();
        resourceBeanList.add(new APIResourceBean("GET", "Application & Application User", tier, uriGet));
        resourceBeanList.add(new APIResourceBean("POST", "Application & Application User", tier, uriPost));
        String endpoint = "/services/customers/customerservice";

        endpointUrl = gatewayUrlsWrk.getWebAppURLHttp() + webApp + endpoint;
        apiProvider = publisherContext.getContextTenant().getContextUser().getUserName();



    }

    @Test(description = "Generate New Application Key")
    public void generateApplicationKeyWithoutScope() throws Exception{

        apiCreationRequestBean = new APICreationRequestBean(apiName, context, version,
                apiProvider, new URL(endpointUrl));
        apiCreationRequestBean.setEndpointType(endPointType);
        apiCreationRequestBean.setTier(tier);
        apiCreationRequestBean.setTags(tags);
        apiCreationRequestBean.setResourceBeanList(resourceBeanList);
        apiCreationRequestBean.setDescription(apiDescription);
        apiCreationRequestBean.setVisibility(visibility);

        log.info(apiCreationRequestBean);

        HttpResponse apiCreateResponse = apiPublisher.addAPI(apiCreationRequestBean);
        assertEquals(apiCreateResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Invalid Response Code");

        //assert JSON object
        JSONObject createApiJsonObject = new JSONObject(apiCreateResponse.getData());
        assertEquals(createApiJsonObject.getBoolean("error"), false, "Error in API Creation");

        HttpResponse verifyApiResponse = apiPublisher.getApi(apiName, apiProvider, version);
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

        apiProvider = storeContext.getContextTenant().getContextUser().getUserName();

        //add application
        HttpResponse addApplicationResponse=apiStore.addApplication(applicationName, tier, callbackUrl,
                applicationDescription);
        assertEquals(addApplicationResponse.getResponseCode(),Response.Status.OK.getStatusCode(),
                "Response Code Mismatched in Create Application: " + applicationName);
        JSONObject addApplicationJsonObject = new JSONObject(addApplicationResponse.getData());
        assertFalse(addApplicationJsonObject.getBoolean("error") , "Error in Create Application: " + applicationName);

        SubscriptionRequest apiSubscriptionRequest = new SubscriptionRequest(apiName, version, apiProvider,
                applicationName, tier);
        HttpResponse subscriptionResponse = apiStore.subscribe(apiSubscriptionRequest);
        assertEquals(subscriptionResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Subscription Response Code is Mismatched");
        JSONObject subscriptionResponseJsonObject = new JSONObject(subscriptionResponse.getData());
        assertFalse(subscriptionResponseJsonObject.getBoolean("error"), "Subscription Response is Mismatched");

        String gatewayUrl;
        if (gatewayContextWrk.getContextTenant().getDomain().equals(FrameworkConstants.SUPER_TENANT_DOMAIN_NAME)) {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp();
        } else {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "t/" +
                    gatewayContextWrk.getContextTenant().getDomain() + "/";

        }

        String accessUrl = gatewayUrl + context + "/" + version + "/customers/123";
        APPKeyRequestGenerator appKeyRequestGenerator = new APPKeyRequestGenerator(applicationName);
        String responseString = apiStore.generateApplicationKey(appKeyRequestGenerator).getData();

        JSONObject jsonObject = new JSONObject(responseString);
        String accessToken = jsonObject.getJSONObject("data").getJSONObject("key").getString("accessToken");

        Map<String, String> applicationHeader = new HashMap<String, String>();
        applicationHeader.put("Authorization", " Bearer " + accessToken);
        applicationHeader.put("accept", "text/xml");
        HttpResponse apiInvokeResponse = new HttpRequestUtil().doGet(accessUrl, applicationHeader);

        String apiResponse = "<Customer><id>123</id><name>John</name></Customer>";
        assertEquals(apiInvokeResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code Mismatched");
        assertTrue(apiInvokeResponse.getData().contains(apiResponse), "Response Data Mismatch");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception{

        HttpResponse removeSubscriptionResponse = apiStore.removeAPISubscriptionByName
                (apiName, version, apiProvider, applicationName);
        assertEquals(removeSubscriptionResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Remove Subscription Response Code Mismatched: "+ apiName);
        JSONObject removeSubscriptionJsonObject = new JSONObject(removeSubscriptionResponse.getData());
        assertFalse(removeSubscriptionJsonObject.getBoolean("error"),
                "Response data Mismatched in Remove Subscription: "+ "Api Name: "+apiName +
                        " Application Name: "+ applicationName);

        //delete API from Publisher
        HttpResponse deleteApiResponse = apiPublisher.deleteAPI(apiName, version, apiProvider);
        assertEquals(deleteApiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Delete Api Response Code is Mismatched: " + apiName);
        JSONObject deleteApiJsonObject= new JSONObject(deleteApiResponse.getData());
        assertFalse(deleteApiJsonObject.getBoolean("error"), "Response data is Mismatched in Delete Api: "+apiName);

        //remove Application
        HttpResponse removeApplicationResponse = apiStore.removeApplication(applicationName);
        assertEquals(removeApplicationResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code is Mismatched in Remove Application: " + applicationName);
        JSONObject removeApplicationJsonObject= new JSONObject(removeApplicationResponse.getData());
        assertFalse(removeApplicationJsonObject.getBoolean("error"),"Response Data Mismatched in remove Application");

    }




}
