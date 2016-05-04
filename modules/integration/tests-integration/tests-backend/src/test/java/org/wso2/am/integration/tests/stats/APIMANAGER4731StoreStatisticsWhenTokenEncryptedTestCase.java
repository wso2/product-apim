/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.integration.tests.stats;

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.integration.test.utils.bean.APIThrottlingTier;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.analytics.spark.admin.stub.AnalyticsProcessorAdminServiceStub;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.ServerConstants;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This test class is for testing APIM Store statistics, when token encryption is enabled. For this DAS or
 * APIM-analytics should be configured and should run with offset 3.
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE })
public class APIMANAGER4731StoreStatisticsWhenTokenEncryptedTestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(APIMANAGER4731StoreStatisticsWhenTokenEncryptedTestCase.class);
    private final String API_NAME = "APIInvocationStatPublisherAPIName";
    private final String API_CONTEXT = "APIInvocationStatPublisherContext";
    private final String DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION = "1.0.0";
    private final String APP_NAME = "APIInvocationStatPublisherApp";
    private final String TIER_COLLECTION = APIMIntegrationConstants.API_TIER.UNLIMITED;
    private final long WAIT_TIME = 5 * 60 * 1000;
    private final String APIM_CONFIG_XML = "api-manager.xml";
    private final String IDENTITY_CONFIG_XML = "identity.xml";
    private final String DAS_USERNAME = "admin";
    private final String DAS_PASSWORD = "admin";
    private final String ANALYTICS_SCRIPT_NAME = "APIM_STAT_SCRIPT";
    private final String DAS_ANALYTICS_PROCESSOR_SERVICE_URL = "https://localhost:9446/services/AnalyticsProcessorAdminService";
    private final String API_STORE_STAT_URL = "/store/site/blocks/stats/perAppAPICount/ajax/stats.jag";

    private String publisherURLHttps;
    private String storeURLHttp;
    private APICreationRequestBean apiCreationRequestBean;
    private List<APIResourceBean> resList;
    private String endpointUrl;
    private Map<String, String> requestHeaders = new HashMap<String, String>();
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private ServerConfigurationManager serverConfigurationManager;

    @Factory(dataProvider = "userModeDataProvider")
    public APIMANAGER4731StoreStatisticsWhenTokenEncryptedTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        publisherURLHttps = publisherUrls.getWebAppURLHttp();
        storeURLHttp = getStoreURLHttp();
        endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api";
        apiPublisher = new APIPublisherRestClient(publisherURLHttps);
        apiStore = new APIStoreRestClient(storeURLHttp);
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
            String artifactsLocation = TestConfigurationProvider.getResourceLocation() +
                    File.separator + "artifacts" + File.separator + "AM" + File.separator +
                    "configFiles" + File.separator + "token_encryption" + File.separator;
            String apimConfigArtifactLocation =
                    getAMResourceLocation() + File.separator + "configFiles" + File.separator + "stats" + File.separator
                            + "tokenEncryptionEnabled" + File.separator + "api-manager.xml";
            String identityConfigArtifactLocation = artifactsLocation + IDENTITY_CONFIG_XML;
            String apimRepositoryConfigLocation = carbonHome + File.separator + "repository" +
                    File.separator + "conf" + File.separator + APIM_CONFIG_XML;
            String identityRepositoryConfigLocation = carbonHome + File.separator + "repository" +
                    File.separator + "conf" + File.separator + "identity" + File.separator +
                    IDENTITY_CONFIG_XML;

            File apimConfSourceFile = new File(apimConfigArtifactLocation);
            File apimConfTargetFile = new File(apimRepositoryConfigLocation);
            File identityConfSourceFile = new File(identityConfigArtifactLocation);
            File identityConfTargetFile = new File(identityRepositoryConfigLocation);
            serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);

            // apply configuration to  api-manager.xml
            serverConfigurationManager.applyConfigurationWithoutRestart(apimConfSourceFile, apimConfTargetFile, true);
            log.info("api-manager.xml configuration file copy from :" + apimConfigArtifactLocation +
                    " to :" + apimRepositoryConfigLocation);

            // apply configuration to identity.xml
            serverConfigurationManager
                    .applyConfigurationWithoutRestart(identityConfSourceFile, identityConfTargetFile, true);
            log.info("identity.xml configuration file copy from :" + identityConfigArtifactLocation +
                    " to :" + identityRepositoryConfigLocation);
            serverConfigurationManager.restartGracefully();
        }
        apiPublisher.login(user.getUserName(), user.getPassword());
        apiStore.login(user.getUserName(), user.getPassword());
    }

    @Test(groups = { "wso2.am" }, description = "Sample API creation")
    public void testAPICreation() throws Exception {
        String providerName = user.getUserName();

        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION, providerName,
                new URL(endpointUrl));
        apiCreationRequestBean.setDescription(DESCRIPTION);
        apiCreationRequestBean.setTiersCollection(TIER_COLLECTION);

        //define resources
        resList = new ArrayList<APIResourceBean>();
        APIResourceBean res1 = new APIResourceBean(APIMIntegrationConstants.HTTP_VERB_GET,
                APIMIntegrationConstants.ResourceAuthTypes.APPLICATION_AND_APPLICATION_USER.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.PLUS, "/add");
        resList.add(res1);

        apiCreationRequestBean.setResourceBeanList(resList);

        //add test api
        HttpResponse serviceResponse = apiPublisher.addAPI(apiCreationRequestBean);
        verifyResponse(serviceResponse);

        //publish the api
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(API_NAME, user.getUserName(),
                APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);
    }

    @Test(groups = { "wso2.am" }, description = "Test API invocation", dependsOnMethods = "testAPICreation")
    public void testApiInvocationAndStatLoadingTest() throws Exception {
        //add a application
        HttpResponse serviceResponse = apiStore
                .addApplication(APP_NAME, APIThrottlingTier.UNLIMITED.getState(), "", "this-is-test");
        verifyResponse(serviceResponse);

        //subscribe to the api
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(API_NAME, user.getUserName());
        subscriptionRequest.setApplicationName(APP_NAME);
        subscriptionRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        serviceResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(serviceResponse);

        //generate the key for the subscription
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(APP_NAME);
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Assert.assertNotNull("Access Token not found " + responseString, accessToken);

        //invoke api
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION);
        serviceResponse = HTTPSClientUtils.doGet(invokeURL + "/add?x=1&y=1", requestHeaders);
        Assert.assertEquals(HttpStatus.SC_OK, serviceResponse.getResponseCode(), "Error in response code");

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Cookie", apiStore.getSession());

        //run the analytics script on DAS
        executeScript();

        //sending request for store stat summary data
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Calendar calender = Calendar.getInstance();
        calender.set(Calendar.DATE, calender.get(Calendar.DATE) - 1);
        String fromDate = dateFormat.format(calender.getTime());
        calender.set(Calendar.DATE, calender.get(Calendar.DATE) + 2);
        String toDate = dateFormat.format(calender.getTime());
        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add(new BasicNameValuePair("action", "getProviderAPIUsage"));
        urlParameters.add(new BasicNameValuePair("currentLocation", "/store/site/pages/statistics.jag"));
        urlParameters.add(new BasicNameValuePair("fromDate", fromDate));
        urlParameters.add(new BasicNameValuePair("toDate", toDate));
        String url = backEndServerUrl.getWebAppURLHttp() + API_STORE_STAT_URL;
        HttpResponse res = HTTPSClientUtils.doPost(url, headers, urlParameters);
        verifyResponse(res);
        Assert.assertTrue(res.getData().contains(API_NAME), "Store Statistics data not contain expected api name");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
        if (TestUserMode.SUPER_TENANT_ADMIN == userMode) {
            serverConfigurationManager.restoreToLastConfiguration();
        }
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }, };
    }

    /**
     * Execute the APIM_STAT_SCRIPT for generate store statistics summary
     *
     * @throws Exception throws any service access error occurred
     */
    private void executeScript() throws Exception {
        //initiating admin service stub for executing script
        AnalyticsProcessorAdminServiceStub stub = new AnalyticsProcessorAdminServiceStub(
                DAS_ANALYTICS_PROCESSOR_SERVICE_URL);
        ServiceClient client = stub._getServiceClient();
        Options client_options = client.getOptions();
        HttpTransportProperties.Authenticator authenticator = new HttpTransportProperties.Authenticator();
        authenticator.setUsername(DAS_USERNAME);
        authenticator.setPassword(DAS_PASSWORD);
        authenticator.setPreemptiveAuthentication(true);
        client_options.setProperty(org.apache.axis2.transport.http.HTTPConstants.AUTHENTICATE, authenticator);
        client.setOptions(client_options);

        //trigger the script
        long currentTime = System.currentTimeMillis();
        long waitTime = currentTime + WAIT_TIME;
        while (waitTime > System.currentTimeMillis()) {
            if (!stub.isAnalyticsTaskExecuting(ANALYTICS_SCRIPT_NAME) && !stub
                    .isAnalyticsScriptExecuting(ANALYTICS_SCRIPT_NAME)) {
                AnalyticsProcessorAdminServiceStub.AnalyticsQueryResultDto[] results = stub
                        .executeScript(ANALYTICS_SCRIPT_NAME);
                break;
            }
            Thread.sleep(30000);
        }
    }
}
