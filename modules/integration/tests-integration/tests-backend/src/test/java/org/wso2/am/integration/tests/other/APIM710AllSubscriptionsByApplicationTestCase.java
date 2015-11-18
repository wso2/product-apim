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
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.am.integration.test.utils.webapp.WebAppDeploymentUtil;
import org.wso2.carbon.automation.engine.FrameworkConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * APIM2-710:List all the the subscriptions by application
 * APIM2-711:Remove a subscription from the application with application name through store rest api
 * APIM2-713:Remove a subscription from the application with application id through store rest api
 */

public class APIM710AllSubscriptionsByApplicationTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(APIM710AllSubscriptionsByApplicationTestCase.class);
    private String apiName="SubscriptionAPITest";
    private String apiContext="subscriptionapicontext";
    private final String version = "1.0.0";
    private final String visibility = "public";
    private final String description = "API subscription";
    private String providerName;
    private final String tier = "Unlimited";
    private final String tags = "subscription";
    private final String applicationName = "SubscribeApplication";
    private static final String webApp = "jaxrs_basic";
    private final String endPointType = "http";
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String gatewayUrl;
    private int numberOfApis=5;
    List<String> apiNameList=new ArrayList<String>();
    List<String> apiContextList=new ArrayList<String>();
    private String accessUrl;
    private boolean isApisDeleted=false;
    private int defaultAppListIndex;
    private String newApplicationName="NewApplication1";



    @Factory(dataProvider = "userModeDataProvider")
    public APIM710AllSubscriptionsByApplicationTestCase(TestUserMode userMode) {
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

        //copy first .war file
        String path = TestConfigurationProvider.getResourceLocation() + File.separator +
                "artifacts" + File.separator + "AM" + File.separator + "lifecycletest" + File.separator;

        String sourcePath = path + webApp + fileFormat;

        String sessionId = createSession(gatewayContextWrk);
        WebAppAdminClient webAppAdminClient = new WebAppAdminClient(gatewayContextWrk.getContextUrls().
                getBackEndUrl(), sessionId);
        webAppAdminClient.uploadWarFile(sourcePath);
        boolean isWebAppDeployed = WebAppDeploymentUtil.isWebApplicationDeployed
                (gatewayContextWrk.getContextUrls().getBackEndUrl(), sessionId, webApp);
        assertTrue(isWebAppDeployed, "Web APP is not deployed");

        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);

        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

        String uri = "customers/{id}/";
        List<APIResourceBean> resourceBeanList = new ArrayList<APIResourceBean>();
        resourceBeanList.add(new APIResourceBean("GET", "Application & Application User", tier, uri));
        String endpoint = "/services/customers/customerservice";

        String endpointUrl = gatewayUrlsWrk.getWebAppURLHttp() + webApp + endpoint;
        providerName = publisherContext.getContextTenant().getContextUser().getUserName();

        //creating  api
        int count=1;
        for(int apiCount=0; apiCount<numberOfApis;apiCount++){

            String tempApiName=apiName+count;
            String tempApiContext=apiContext+count;
            APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(tempApiName, tempApiContext,
                    version,providerName,
                    new URL(endpointUrl));
            apiCreationRequestBean.setEndpointType(endPointType);
            apiCreationRequestBean.setTiersCollection(tier);
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

            HttpResponse verifyApiResponse = apiPublisher.getApi(tempApiName, providerName, version);
            JSONObject verifyApiJsonObject = new JSONObject(verifyApiResponse.getData());
            assertFalse(verifyApiJsonObject.getBoolean("error"), "Error in Verify API Response");

            //add apiName and apiContext to lists
            apiNameList.add(tempApiName);
            apiContextList.add(tempApiContext);

            //publish API
            APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(tempApiName, providerName,
                    APILifeCycleState.PUBLISHED);

            HttpResponse statusUpdateResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
            assertEquals(statusUpdateResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response Code is Mismatched");

            JSONObject statusUpdateJsonObject = new JSONObject(statusUpdateResponse.getData());
            assertFalse(statusUpdateJsonObject.getBoolean("error"), "API is not published");

            count++;
        }

        if (gatewayContextWrk.getContextTenant().getDomain().equals(FrameworkConstants.SUPER_TENANT_DOMAIN_NAME)) {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp();
        } else {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "t/" +
                    gatewayContextWrk.getContextTenant().getDomain() + "/";

        }
        providerName = storeContext.getContextTenant().getContextUser().getUserName();

        //create Application
        HttpResponse createAppResponse = apiStore.addApplication(applicationName, tier, "", "");
        assertEquals(createAppResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error in Application Creation");
        JSONObject createAppJsonObject = new JSONObject(createAppResponse.getData());
        assertFalse(createAppJsonObject.getBoolean("error"), "Error in Application creation Response: "
                + applicationName);


        //subscribe first set apis for the created created application
        for(int apiCount=0;apiCount<numberOfApis-2;apiCount++){
            accessUrl = gatewayUrl + apiContextList.get(apiCount) + "/" + version + "/customers/123";
            SubscriptionRequest apiSubscriptionRequest = new SubscriptionRequest(apiNameList.get(apiCount), version, providerName,
                    applicationName, tier);
            HttpResponse subscriptionResponse = apiStore.subscribe(apiSubscriptionRequest);
            assertEquals(subscriptionResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Subscription Response Code is Mismatched");
            JSONObject subscriptionResponseJsonObject = new JSONObject(subscriptionResponse.getData());
            assertFalse(subscriptionResponseJsonObject.getBoolean("error"), "Subscription Response is Mismatched");

        }
        //create Application
        HttpResponse createNewAppResponse = apiStore.addApplication(newApplicationName, tier, "", "");
        assertEquals(createAppResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error in Application Creation");
        JSONObject createNewAppJsonObject = new JSONObject(createNewAppResponse.getData());
        assertFalse(createAppJsonObject.getBoolean("error"), "Error in Application creation Response: "
                + applicationName);


        //subscribe second set(last 2 apis) for the second application
        providerName=storeContext.getContextTenant().getContextUser().getUserName();
        for(int apiCount=numberOfApis-2;apiCount<numberOfApis;apiCount++){
            accessUrl = gatewayUrl + apiContextList.get(apiCount) + "/" + version + "/customers/123";
            SubscriptionRequest apiSubscriptionRequestDefaultApp=new SubscriptionRequest(apiNameList.get(apiCount),
                    version,providerName,
                    newApplicationName,tier);
            HttpResponse subscriptionResponseDefaultApp = apiStore.subscribe(apiSubscriptionRequestDefaultApp);
            assertEquals(subscriptionResponseDefaultApp.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Subscription Response Code is Mismatched");
            JSONObject subscriptionResponseJsonObject = new JSONObject(subscriptionResponseDefaultApp.getData());
            assertFalse(subscriptionResponseJsonObject.getBoolean("error"), "Subscription Response is Mismatched");

        }
    }
    @Test(description = "List all Subscriptions By Application Name")
    public void testAllSubscriptionsByAppName() throws Exception{

        HttpResponse publishedApiByAppResponse;
        JSONObject publishedApiByAppJsonObject;
        //get first set of apis by created application
        publishedApiByAppResponse=apiStore.getPublishedAPIsByApplication(applicationName);
        assertEquals(publishedApiByAppResponse.getResponseCode(),Response.Status.OK.getStatusCode(),
                "Response Code Mismatched: " + applicationName);
        publishedApiByAppJsonObject=new JSONObject(publishedApiByAppResponse.getData());
        assertFalse(publishedApiByAppJsonObject.getBoolean("error"),"Response Data Mismatched: " + applicationName);
        log.info(publishedApiByAppJsonObject);
        JSONArray applicationSubscribedJsonArray= publishedApiByAppJsonObject.getJSONArray("apis");
        //verify the apis count
        assertTrue(applicationSubscribedJsonArray.length() >= apiNameList.size() - 2, "Api Count is mismatched");
        //verify application names response
        boolean isApisAvailable=false;
        for(int index=0;index<applicationSubscribedJsonArray.length();index++){
            isApisAvailable=true;
            assertEquals(applicationSubscribedJsonArray.getJSONObject(index).getString("apiName"),apiNameList.get(index),
                    "Api Name is Mismatched");
        }
        assertTrue(isApisAvailable,"Response Error in Apis");

        //get second set (last 2 apis) by the new application
        publishedApiByAppResponse=apiStore.getPublishedAPIsByApplication(newApplicationName);
        assertEquals(publishedApiByAppResponse.getResponseCode(),Response.Status.OK.getStatusCode(),
                "Response Code Mismatched: " + newApplicationName);
        publishedApiByAppJsonObject=new JSONObject(publishedApiByAppResponse.getData());
        assertFalse(publishedApiByAppJsonObject.getBoolean("error"),"Response Data Mismatched: " + newApplicationName);
        log.info(publishedApiByAppJsonObject);
        JSONArray defaultApplicationSubscribedJsonArray= publishedApiByAppJsonObject.getJSONArray("apis");
        //verify the apis count
        assertTrue(defaultApplicationSubscribedJsonArray.length()>=2, "Api Count is mismatched");
        //verify application names response
        isApisAvailable=false;
        int defaultAppListIndex=numberOfApis-2;
        for(int index=0;index<defaultApplicationSubscribedJsonArray.length();index++){
            isApisAvailable=true;
            assertEquals(defaultApplicationSubscribedJsonArray.getJSONObject(index).getString("apiName"),
                    apiNameList.get(defaultAppListIndex), "Api Name is Mismatched");
            defaultAppListIndex++;
        }
        assertTrue(isApisAvailable,"Response Error in Apis");
    }

    @Test(description = "Remove Subscription by Application Name")
    public void testRemoveSubscriptionByAppName() throws Exception{

        providerName=storeContext.getContextTenant().getContextUser().getUserName();
        for(int apiCount=0;apiCount<numberOfApis-2;apiCount++){

            HttpResponse removeSubscriptionByAppName=apiStore.removeAPISubscriptionByName(apiNameList.get(apiCount),
                    version,providerName,applicationName);
            assertEquals(removeSubscriptionByAppName.getResponseCode(),Response.Status.OK.getStatusCode());
            JSONObject removeSubscriptionByAppNameJsonObject=new JSONObject(removeSubscriptionByAppName.getData());
            assertFalse(removeSubscriptionByAppNameJsonObject.getBoolean("error"),
                    "Error in Remove Subscription By Application Name: "+ applicationName);
        }
        //verify subscription exists
        HttpResponse verifySubscriptionResponse=apiStore.getPublishedAPIsByApplication(applicationName);
        assertEquals(verifySubscriptionResponse.getResponseCode(),Response.Status.OK.getStatusCode(),
                "Error in Subscription verification Response");
        JSONObject verifySubscriptionJsonObject=new JSONObject(verifySubscriptionResponse.getData());
        assertFalse(verifySubscriptionJsonObject.getBoolean("error"), "Error in Subscription verification Response");
        JSONArray apisJsonArray=verifySubscriptionJsonObject.getJSONArray("apis");

        //verify the length
//        assertTrue(apisJsonArray.length()>=0,"Error in verify api Response");
//        //verify subscription exists
//        boolean isSubscribeAppRemoved=true;
//        if(apisJsonArray.length()>0){
//            for(int index=0;index<apisJsonArray.length();index++){
//                if(apiNameList.get(index).equals(apisJsonArray.getJSONObject(index).getString("name"))){
//                    isSubscribeAppRemoved=false;
//
//                }
//            }
//            assertTrue(isSubscribeAppRemoved,"Error in Subscription Removal");
//        }
    }

    @Test(description = "Remove Subscription By Application Id")
    public void testRemoveSubscriptionByAppId() throws Exception{

        defaultAppListIndex=numberOfApis-2;
        //get default application Id
        HttpResponse getAllAppResponse= apiStore.getAllApplications();
        assertEquals(getAllAppResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Error in Get All Apps Response");
        JSONObject getAllAppJsonObject=new JSONObject(getAllAppResponse.getData());
        assertFalse(getAllAppJsonObject.getBoolean("error"), "Error in get Applications Response");
        JSONArray getAllAppsJsonArray=getAllAppJsonObject.getJSONArray("applications");

        for(int arrayIndex=0;arrayIndex<getAllAppsJsonArray.length();arrayIndex++){
            if(getAllAppsJsonArray.getJSONObject(arrayIndex).getString("name").equals(newApplicationName)){
                String defaultAppId=getAllAppsJsonArray.getJSONObject(arrayIndex).getString("id");

                //remove subscription by application Id
                HttpResponse removeSubscriptionByIdResponse=apiStore.removeAPISubscription
                        (apiNameList.get(defaultAppListIndex), version, providerName,defaultAppId);
                assertEquals(removeSubscriptionByIdResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                        "Invalid Response Code");
                JSONObject removeSubscriptionByIdJsonObject=new JSONObject(removeSubscriptionByIdResponse.getData());
                assertFalse(removeSubscriptionByIdJsonObject.getBoolean("error"),
                        "Error in Removal Subscription By Application Id");
            }
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception{

        apiStore.removeApplication(applicationName);
        isApisDeleted=false;

        //delete first set of apis
        for(int listIndex=0;listIndex<apiNameList.size()-2;listIndex++){
            providerName=publisherContext.getContextTenant().getContextUser().getUserName();
            apiPublisher.deleteAPI(apiNameList.get(listIndex),version,providerName);
            isApisDeleted=true;

        }
        assertTrue(isApisDeleted, "Application and Apis are Not deleted");

        //delete created apis
        isApisDeleted=false;
        providerName=storeContext.getContextTenant().getContextUser().getUserName();
        for(int listIndex=0;listIndex<2;listIndex++){
            providerName=publisherContext.getContextTenant().getContextUser().getUserName();
            apiPublisher.deleteAPI(apiNameList.get(defaultAppListIndex),version,providerName);
            isApisDeleted=true;
            defaultAppListIndex++;
        }
        assertTrue(isApisDeleted,"Error in Application Deleted: " + applicationName);

        apiStore.removeApplication(newApplicationName);
    }

}
