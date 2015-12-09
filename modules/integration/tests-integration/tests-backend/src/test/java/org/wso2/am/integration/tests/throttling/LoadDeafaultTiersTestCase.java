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
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*
*/
package org.wso2.am.integration.tests.throttling;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.*;
import org.wso2.am.admin.clients.registry.ResourceAdminServiceClient;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE }) public class LoadDeafaultTiersTestCase
        extends APIManagerLifecycleBaseTest {
    private static final Log log = LogFactory.getLog(LoadDeafaultTiersTestCase.class);
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String TIER_XML_REG_CONFIG_LOCATION = "/_system/governance/apimgt/applicationdata/tiers.xml";
    private final String TIER_XML_REG_CONFIG_APP_LOCATION = "/_system/governance/apimgt/applicationdata/app-tiers.xml";
    private final String TIER_XML_REG_CONFIG_RES_LOCATION = "/_system/governance/apimgt/applicationdata/res-tiers.xml";
    private final String TIER_MANAGE_PAGE_TIER_GOLD = "{ \"value\": \"Gold\", \"text\": \"Gold\" }";
    private final String TIER_MANAGE_PAGE_TIER_SILVER = "<option value=\"Silver\" >Silver</option>";

    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private final String TIER_MANAGE_PAGE_RESOURCE_TIER_ULTIMATE = "\"value\": \"Ultimate\"";
    private final String TIER_MANAGE_PAGE_RESOURCE_TIER_SILVER = "<option value=\"Silver\" >Silver</option>";
    private final String TIER_MANAGE_PAGE_APPLICATION_TIER_GOLD = "<option value=\"Gold\" >";
    private final String TIER_MANAGE_PAGE_APPLICATION_TIER_LARGE = "<option value=\"Large\" >";

    private String apiEndPointUrl;
    private String providerName;
    private String newTiersXML;
    private ResourceAdminServiceClient resourceAdminServiceClient;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;

    private String APITierTEST_API;
    private String APITierTEST_APP;
    private String APPTierTEST_API;
    private String APPTierTEST_APP;
    private String RESTierTEST_API;
    private String RESTierTEST_APP;

    private String Before_TierTEST_API;
    private String After_TierTEST_API;

    private APIIdentifier APITierTEST_ID;
    private APIIdentifier APPTierTEST_ID;
    private APIIdentifier RESTierTEST_ID;
    private APIIdentifier Before_TierTEST_ID;
    private APIIdentifier After_TierTEST_ID;

    @Factory(dataProvider = "userModeDataProvider")
    public LoadDeafaultTiersTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN }
//                , new Object[] { TestUserMode.TENANT_ADMIN }
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        apiEndPointUrl = getGatewayURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();
        //        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
        //                new URL(apiEndPointUrl));
        //        apiCreationRequestBean.setTags(API_TAGS);
        //        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        String publisherURLHttp = getPublisherURLHttp();
        String storeURLHttp = getStoreURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);

        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(user.getUserName(), user.getPassword());

        //Login to API Store with  admin
        apiStoreClientUser1.login(user.getUserName(), user.getPassword());

        //        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        String artifactsLocation = TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" +
                File.separator + "AM" + File.separator + "lifecycletest" + File.separator + "default-tiers.xml";
        resourceAdminServiceClient = new ResourceAdminServiceClient(publisherContext.getContextUrls().getBackEndUrl(),
                createSession(publisherContext));
        newTiersXML = readFile(artifactsLocation);
    }

    @Test(groups = { "wso2.am" }, description = "Test availability of tiers in API Manage Page before change tiers")
    public void testAvailabilityOfTiersInAPIManagePageBeforeChangeTiersXML()
            throws APIManagerIntegrationTestException, RemoteException, ResourceAdminServiceExceptionException,
            MalformedURLException {

        Before_TierTEST_API = "beforeAPITier_api_name";
        String Before_TierTEST_context = "beforeAPITier_api_name";

        APICreationRequestBean apiRequestBean = new APICreationRequestBean(Before_TierTEST_API, Before_TierTEST_context,
                API_VERSION_1_0_0, providerName, new URL(apiEndPointUrl));
        apiRequestBean.setTags(API_TAGS);
        apiRequestBean.setDescription(API_DESCRIPTION);

        Before_TierTEST_ID = new APIIdentifier(providerName, Before_TierTEST_API, API_VERSION_1_0_0);
        createAndPublishAPI(Before_TierTEST_ID, apiRequestBean, apiPublisherClientUser1, false);

        HttpResponse tierManagePageHttpResponse = apiPublisherClientUser1
                .getAPIManagePage(Before_TierTEST_API, providerName, API_VERSION_1_0_0);
        assertEquals(tierManagePageHttpResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke to get Tier Permission Page");
        assertTrue(tierManagePageHttpResponse.getData().contains(TIER_MANAGE_PAGE_TIER_SILVER),
                "default tier Silver is not available in Tier Permission page before  add new tear in tiers.xml");
        assertTrue(tierManagePageHttpResponse.getData().contains(TIER_MANAGE_PAGE_RESOURCE_TIER_ULTIMATE),
                "default Resource tier Ultimate is not available in Tier Permission page before  add new tear in tiers.xml");

        HttpResponse applicationPageHttpResponse = apiStoreClientUser1.getApplicationPage();
        assertEquals(applicationPageHttpResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke to get Tier Permission Page");
        assertTrue(applicationPageHttpResponse.getData().contains(TIER_MANAGE_PAGE_APPLICATION_TIER_LARGE),
                "default tier Large is not available in Tier Permission page before add new tear in tiers.xml");

        deleteAPI(Before_TierTEST_ID, apiPublisherClientUser1);

    }

    @Test(groups = {
            "wso2.am" }, description = "Test API Tier availability and it's throttle limit", dependsOnMethods = "testAvailabilityOfTiersInAPIManagePageBeforeChangeTiersXML")
    public void testApi() throws IOException, APIManagerIntegrationTestException, XPathExpressionException {

        APITierTEST_API = "testAPITier_api_name";
        String apiTierAPI_context = "testAPITier_api_name";
        APITierTEST_APP = "testAPITier_app_name";
        String API_RESPONSE_DATA = "<id>123</id><name>John</name></Customer>";

        APICreationRequestBean apiRequestBean = new APICreationRequestBean(APITierTEST_API, apiTierAPI_context,
                API_VERSION_1_0_0, providerName, new URL(apiEndPointUrl));
        apiRequestBean.setTags(API_TAGS);
        apiRequestBean.setDescription(API_DESCRIPTION);

        APITierTEST_ID = new APIIdentifier(providerName, APITierTEST_API, API_VERSION_1_0_0);
        createAndPublishAPI(APITierTEST_ID, apiRequestBean, apiPublisherClientUser1, false);

        apiStoreClientUser1.addApplication(APITierTEST_APP, TIER_UNLIMITED, "", "");
        HttpResponse subscribeResponse = subscribeToAPI(APITierTEST_ID, APITierTEST_APP, apiStoreClientUser1);
        assertEquals(subscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of API request not successful");

        String accessToken = generateApplicationKeys(apiStoreClientUser1, APITierTEST_APP).getAccessToken();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        int cnt = 0;
        for (int i = 0; i < APIMIntegrationConstants.API_TIER.GOLD_LIMIT + 10; i++) {
            //Invoke  old version
            HttpResponse invokeResponse = HttpRequestUtil
                    .doGet(getAPIInvocationURLHttp(apiTierAPI_context, API_VERSION_1_0_0) + "/" +
                            "customers/123", requestHeaders);
            if (invokeResponse.getResponseCode() == HTTP_RESPONSE_CODE_OK) {
                cnt++;
            } else if (invokeResponse.getResponseCode() == HTTP_RESPONSE_CODE_TOO_MANY_REQUESTS) {
                break;
            }

        }
        assertEquals(cnt, APIMIntegrationConstants.API_TIER.GOLD_LIMIT, "Error number of throttle request count");

    }

    @Test(groups = {
            "wso2.am" }, description = "Test Application Tier availability and it's throttle limit", dependsOnMethods = "testApi")
    public void testApplication() throws IOException, APIManagerIntegrationTestException, XPathExpressionException {

        APPTierTEST_API = "testAPPTier_api_name";
        String apiTierAPI_context = "testAPPTier_api_name";
        APPTierTEST_APP = "testAPPTier_app_name";
        String API_RESPONSE_DATA = "<id>123</id><name>John</name></Customer>";

        APICreationRequestBean apiRequestBean = new APICreationRequestBean(APPTierTEST_API, apiTierAPI_context,
                API_VERSION_1_0_0, providerName, new URL(apiEndPointUrl));
        apiRequestBean.setTags(API_TAGS);
        apiRequestBean.setDescription(API_DESCRIPTION);

        APPTierTEST_ID = new APIIdentifier(providerName, APPTierTEST_API, API_VERSION_1_0_0);
        createAndPublishAPI(APPTierTEST_ID, apiRequestBean, apiPublisherClientUser1, false);

        apiStoreClientUser1.addApplication(APPTierTEST_APP, APIMIntegrationConstants.APPLICATION_TIER.MEDIUM, "", "");
        HttpResponse subscribeResponse = subscribeToAPI(APPTierTEST_ID, APPTierTEST_APP, apiStoreClientUser1);
        assertEquals(subscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of API request not successful");

        String accessToken = generateApplicationKeys(apiStoreClientUser1, APPTierTEST_APP).getAccessToken();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        int cnt = 0;
        for (int i = 0; i < APIMIntegrationConstants.APPLICATION_TIER.MEDIUM_LIMIT + 10; i++) {
            //Invoke  old version
            HttpResponse invokeResponse = HttpRequestUtil
                    .doGet(getAPIInvocationURLHttp(apiTierAPI_context, API_VERSION_1_0_0) + "/" +
                            "customers/123", requestHeaders);
            if (invokeResponse.getResponseCode() == HTTP_RESPONSE_CODE_OK) {
                cnt++;
            } else if (invokeResponse.getResponseCode() == HTTP_RESPONSE_CODE_TOO_MANY_REQUESTS) {
                break;
            }

        }
        assertEquals(cnt, APIMIntegrationConstants.APPLICATION_TIER.MEDIUM_LIMIT,
                "Error number of throttle request count");

    }

    @Test(groups = {
            "wso2.am" }, description = "Test Resource Tier availability and it's throttle limit", dependsOnMethods = "testApplication")
    public void testResource() throws IOException, APIManagerIntegrationTestException, XPathExpressionException {

        RESTierTEST_API = "testRESTier_api_name";
        String apiTierAPI_context = "testRESTier_api_name";
        RESTierTEST_APP = "testRESTier_app_name";
        String API_RESPONSE_DATA = "<id>123</id><name>John</name></Customer>";

        APICreationRequestBean apiRequestBean = new APICreationRequestBean(RESTierTEST_API, apiTierAPI_context,
                API_VERSION_1_0_0, providerName, APIMIntegrationConstants.API_TIER.GOLD,
                APIMIntegrationConstants.RESOURCE_TIER.PLUS, new URL(apiEndPointUrl));
        apiRequestBean.setTags(API_TAGS);
        apiRequestBean.setDescription(API_DESCRIPTION);

        RESTierTEST_ID = new APIIdentifier(providerName, RESTierTEST_API, API_VERSION_1_0_0);
        createAndPublishAPI(RESTierTEST_ID, apiRequestBean, apiPublisherClientUser1, false);

        apiStoreClientUser1.addApplication(RESTierTEST_APP, TIER_UNLIMITED, "", "");
        HttpResponse subscribeResponse = subscribeToAPI(RESTierTEST_ID, RESTierTEST_APP, apiStoreClientUser1);
        assertEquals(subscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of API request not successful");

        String accessToken = generateApplicationKeys(apiStoreClientUser1, RESTierTEST_APP).getAccessToken();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        int cnt = 0;
        for (int i = 0; i < APIMIntegrationConstants.RESOURCE_TIER.PLUS_LIMIT + 10; i++) {
            //Invoke  old version
            HttpResponse invokeResponse = HttpRequestUtil
                    .doGet(getAPIInvocationURLHttp(apiTierAPI_context, API_VERSION_1_0_0) + "/" +
                            "customers/123", requestHeaders);
            if (invokeResponse.getResponseCode() == HTTP_RESPONSE_CODE_OK) {
                cnt++;
            } else if (invokeResponse.getResponseCode() == HTTP_RESPONSE_CODE_TOO_MANY_REQUESTS) {
                break;
            }

        }
        assertEquals(cnt, APIMIntegrationConstants.RESOURCE_TIER.PLUS_LIMIT, "Error number of throttle request count");

    }

    @Test(groups = {
            "wso2.am" }, description = "Test availability of tiers in API Manage Page after change tiers", dependsOnMethods = "testResource")
    public void testAvailabilityOfTiersInAPIManagePageAfterChangeTiersXML() throws Exception {

        clean();

        resourceAdminServiceClient.updateTextContent(TIER_XML_REG_CONFIG_LOCATION, newTiersXML);
        resourceAdminServiceClient.updateTextContent(TIER_XML_REG_CONFIG_APP_LOCATION, newTiersXML);
        resourceAdminServiceClient.updateTextContent(TIER_XML_REG_CONFIG_RES_LOCATION, newTiersXML);

        After_TierTEST_API = "afterAPITier_api_name";
        String After_TierTEST_context = "afterAPITier_api_name";

        APICreationRequestBean apiRequestBean = new APICreationRequestBean(After_TierTEST_API, After_TierTEST_context,
                API_VERSION_1_0_0, providerName, new URL(apiEndPointUrl));
        apiRequestBean.setTags(API_TAGS);
        apiRequestBean.setDescription(API_DESCRIPTION);

        After_TierTEST_ID = new APIIdentifier(providerName, After_TierTEST_API, API_VERSION_1_0_0);
        createAndPublishAPI(After_TierTEST_ID, apiRequestBean, apiPublisherClientUser1, false);

        HttpResponse tierManagePageHttpResponse = apiPublisherClientUser1
                .getAPIManagePage(After_TierTEST_API, providerName, API_VERSION_1_0_0);
        assertEquals(tierManagePageHttpResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke to get Tier Permission Page");
        assertTrue(tierManagePageHttpResponse.getData().contains(TIER_MANAGE_PAGE_TIER_GOLD),
                "default tier Gold is not available in Tier Permission page before  add new tear in tiers.xml");
        assertTrue(tierManagePageHttpResponse.getData().contains(TIER_MANAGE_PAGE_RESOURCE_TIER_SILVER),
                "default Resource tier Silver is not available in Tier Permission page before  add new tear in tiers.xml");

        HttpResponse applicationPageHttpResponse = apiStoreClientUser1.getApplicationPage();
        assertEquals(applicationPageHttpResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke to get Tier Permission Page");
        assertTrue(applicationPageHttpResponse.getData().contains(TIER_MANAGE_PAGE_APPLICATION_TIER_GOLD),
                "default tier Gold is not available in Tier Permission page before  add new tear in tiers.xml");
    }

    private void clean() throws Exception {
        apiStoreClientUser1.removeApplication(APITierTEST_APP);
        apiStoreClientUser1.removeApplication(APPTierTEST_APP);
        apiStoreClientUser1.removeApplication(RESTierTEST_APP);

        deleteAPI(APITierTEST_ID, apiPublisherClientUser1);
        deleteAPI(APPTierTEST_ID, apiPublisherClientUser1);
        deleteAPI(RESTierTEST_ID, apiPublisherClientUser1);
    }
    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        deleteAPI(After_TierTEST_ID, apiPublisherClientUser1);

        super.cleanUp();
    }

}
