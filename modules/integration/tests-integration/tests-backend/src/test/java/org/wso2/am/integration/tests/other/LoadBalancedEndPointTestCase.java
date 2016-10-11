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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.webapp.WebAppAdminClient;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.FrameworkConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class LoadBalancedEndPointTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(LoadBalancedEndPointTestCase.class);
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String apiName = "LoadBalanacedAPITestCase";
    private String context = "LoadBalancedAPI";
    private String version = "1.0.0";
    private String visibility = "public";
    private String providerName;
    private String tier= APIMIntegrationConstants.API_TIER.UNLIMITED;
    private String resTier= APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED;
    private String appTier= APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED;
    private String endPointType = "load_balance";
    private String applicationName = "LoadBalanceAPIApplication";
    private List<APIResourceBean> resourceBeanList;
    private String apiNameSandbox = "SandboxAPITestCase";
    private String contextSandbox = "SandboxAPI";
    private String applicationNameSandbox = "SandboxAPIApplication";
    private String firstProductionEndPoint = "";
    private String secondProductionEndPoint = "";
    private String thirdProductionEndPoint = "";
    private String productionEndpointPrefix = "HelloWSO2 from File ";
    private String gatewayUrl;
    private WebAppAdminClient webAppAdminClient;

    @Factory(dataProvider = "userModeDataProvider")
    public LoadBalancedEndPointTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                //                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        log.info("Test Starting user mode: " + userMode);

        String gatewayMgtSessionId = createSession(gatewayContextMgt);

        String testArtifactPath = TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" +
                                  File.separator + "AM" + File.separator;
        String testArtifactWarFilePath = testArtifactPath + "lifecycletest" + File.separator;

        webAppAdminClient = new WebAppAdminClient(
                gatewayContextMgt.getContextUrls().getBackEndUrl(), gatewayMgtSessionId);

        webAppAdminClient.uploadWarFile(testArtifactWarFilePath + APIMIntegrationConstants.PRODEP1_WEB_APP_NAME + ".war");
        webAppAdminClient.uploadWarFile(testArtifactWarFilePath + APIMIntegrationConstants.PRODEP2_WEB_APP_NAME + ".war");
        webAppAdminClient.uploadWarFile(testArtifactWarFilePath + APIMIntegrationConstants.PRODEP3_WEB_APP_NAME + ".war");
        webAppAdminClient.uploadWarFile(testArtifactWarFilePath + APIMIntegrationConstants.SANDBOXEP1_WEB_APP_NAME + ".war");
        webAppAdminClient.uploadWarFile(testArtifactWarFilePath + APIMIntegrationConstants.SANDBOXEP2_WEB_APP_NAME + ".war");
        webAppAdminClient.uploadWarFile(testArtifactWarFilePath + APIMIntegrationConstants.SANDBOXEP3_WEB_APP_NAME + ".war");


        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);

        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());
        //add resources
        resourceBeanList = new ArrayList<APIResourceBean>();
        resourceBeanList.add(new APIResourceBean("GET", "Application & Application User", resTier, "name"));

        if (gatewayContextWrk.getContextTenant().getDomain().equals(FrameworkConstants.SUPER_TENANT_DOMAIN_NAME)) {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp();
        } else {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "t/" +
                    gatewayContextWrk.getContextTenant().getDomain() + "/";

        }

        //checking whether endpoint web apps are deployed
        isWebAppDeployed(gatewayContextWrk.getContextUrls().getWebAppURL(),
                APIMIntegrationConstants.SANDBOXEP1_WEB_APP_NAME, "HelloWSO2 from File 1_Sandbox");
        isWebAppDeployed(gatewayContextWrk.getContextUrls().getWebAppURL(),
                APIMIntegrationConstants.SANDBOXEP2_WEB_APP_NAME, "HelloWSO2 from File 2_Sandbox");
        isWebAppDeployed(gatewayContextWrk.getContextUrls().getWebAppURL(),
                APIMIntegrationConstants.SANDBOXEP3_WEB_APP_NAME, "HelloWSO2 from File 3_Sandbox");
        isWebAppDeployed(gatewayContextWrk.getContextUrls().getWebAppURL(),
                APIMIntegrationConstants.PRODEP1_WEB_APP_NAME, "HelloWSO2 from File 1");
        isWebAppDeployed(gatewayContextWrk.getContextUrls().getWebAppURL(),
                APIMIntegrationConstants.PRODEP2_WEB_APP_NAME, "HelloWSO2 from File 2");
        isWebAppDeployed(gatewayContextWrk.getContextUrls().getWebAppURL(),
                APIMIntegrationConstants.PRODEP3_WEB_APP_NAME, "HelloWSO2 from File 3");

    }

    @Test(groups = { "wso2.am" }, description = "Test Load Balance End Points", priority = 1)
    public void testCreateApiWithDifferentProductionEndpoints() throws Exception {

        String tags = "LB";
        String description = "LoadBalancedEnd-point";

        firstProductionEndPoint = gatewayUrlsWrk.getWebAppURLHttp() + APIMIntegrationConstants.PRODEP1_WEB_APP_NAME;
        secondProductionEndPoint = gatewayUrlsWrk.getWebAppURLHttp() + APIMIntegrationConstants.PRODEP2_WEB_APP_NAME;
        thirdProductionEndPoint = gatewayUrlsWrk.getWebAppURLHttp() + APIMIntegrationConstants.PRODEP3_WEB_APP_NAME;

        ArrayList<String> endpointLB = new ArrayList<String>();
        endpointLB.add(firstProductionEndPoint);
        endpointLB.add(secondProductionEndPoint);
        endpointLB.add(thirdProductionEndPoint);

        providerName = publisherContext.getContextTenant().getContextUser().getUserName();

        APICreationRequestBean apiCreateRequestBean = new APICreationRequestBean(apiName, context, version,
                providerName, endpointLB);
        apiCreateRequestBean.setTiersCollection(tier);
        apiCreateRequestBean.setResourceBeanList(resourceBeanList);
        apiCreateRequestBean.setVisibility(visibility);
        apiCreateRequestBean.setTags(tags);
        apiCreateRequestBean.setDescription(description);
        apiCreateRequestBean.setEndpointType(endPointType);

        HttpResponse apiCreateResponse = apiPublisher.addAPI(apiCreateRequestBean);
        assertEquals(apiCreateResponse.getResponseCode(), Response.Status.OK.getStatusCode(), "Invalid Response Code");

        //assert JSON object
        JSONObject jsonObject = new JSONObject(apiCreateResponse.getData());
        assertFalse(jsonObject.getBoolean("error"), "Error in API creation");

        HttpResponse verifyApi = apiPublisher.getApi(apiName, providerName, version);
        JSONObject verifyApiJsonObject = new JSONObject(verifyApi.getData());
        assertFalse(verifyApiJsonObject.getBoolean("error"), "Error in Verify API Response");

        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, providerName,
                APILifeCycleState.PUBLISHED);
        HttpResponse statusUpdateResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        assertEquals(statusUpdateResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code is Mismatched");
        JSONObject statusUpdateJsonObject = new JSONObject(statusUpdateResponse.getData());
        assertFalse(statusUpdateJsonObject.getBoolean("error"), "API is not published");

    }

    @Test(groups = { "wso2.am" }, description = "Verify Round Robin Algorithm by Invoking the Production Endpoint API",
            dependsOnMethods = { "testCreateApiWithDifferentProductionEndpoints" })
    public void testRoundRobinAlgorithmInProductionEndpoints() throws Exception {

        String accessUrl = gatewayUrl + context + "/" + version + "/name";
        apiStore.addApplication(applicationName, appTier, "", "");

        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, version, providerName,
                applicationName, tier);
        HttpResponse subscriptionResponse = apiStore.subscribe(subscriptionRequest);
        assertEquals(subscriptionResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code Mismatched");
        JSONObject subscriptionJsonObject = new JSONObject(subscriptionResponse.getData());
        assertEquals(subscriptionJsonObject.getBoolean("error"), false, "Error in Subscription Response");

        APPKeyRequestGenerator appKeyRequestGenerator = new APPKeyRequestGenerator(applicationName);
        String responseString = apiStore.generateApplicationKey(appKeyRequestGenerator).getData();

        JSONObject jsonObject = new JSONObject(responseString);
        String accessToken = jsonObject.getJSONObject("data").getJSONObject("key").getString("accessToken");

        Map<String, String> applicationHeader = new HashMap<String, String>();
        applicationHeader.put("Authorization", " Bearer " + accessToken);

        //Verify Round Robin Algorithm by invoking the api multiple times

        HttpResponse apiInvokeResponse;

        int numberOfEndpoints = 3;
        int requestCount = 10;
        //Todo replace sleep with proper method
        Thread.sleep(80000);
        for (int requestNumber = 1; requestNumber < requestCount; requestNumber++) {
            apiInvokeResponse = HttpRequestUtil.doGet(accessUrl, applicationHeader);
            assertEquals(apiInvokeResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response Code Mismatch");

            int remainder = requestNumber % numberOfEndpoints;
            if (remainder == 0) {
                log.info(apiInvokeResponse.getData());
                assertEquals(apiInvokeResponse.getData(), productionEndpointPrefix + "3",
                        "Error in Round Robin Algorithm in cycle " + requestNumber);

            } else {
                log.info(apiInvokeResponse.getData());
                assertEquals(apiInvokeResponse.getData(), productionEndpointPrefix + remainder,
                        "Error in Round Robin Algorithm in Cycle " + requestNumber);
            }
        }
    }

    @Test(groups = {
            "wso2.am" }, description = "Test Load balanced function with both Production and Sandbox Endpoints",
            priority = 2)
    public void testCreateApiWithBothProdAndSandboxEndpoints() throws Exception {

        String descriptionSandbox = "SandboxEnd-point";
        String tagsSandbox = "sandbox";

        firstProductionEndPoint = gatewayUrlsWrk.getWebAppURLHttp() + APIMIntegrationConstants.PRODEP1_WEB_APP_NAME;
        secondProductionEndPoint = gatewayUrlsWrk.getWebAppURLHttp() + APIMIntegrationConstants.PRODEP2_WEB_APP_NAME;
        thirdProductionEndPoint = gatewayUrlsWrk.getWebAppURLHttp() + APIMIntegrationConstants.PRODEP3_WEB_APP_NAME;

        //Add production endpoints
        List<String> endpointProd = new ArrayList<String>();
        endpointProd.add(firstProductionEndPoint);
        endpointProd.add(secondProductionEndPoint);
        endpointProd.add(thirdProductionEndPoint);

        //add sandbox endpoints
        String firstSandboxEndpoint =
                gatewayUrlsWrk.getWebAppURLHttp() + APIMIntegrationConstants.SANDBOXEP1_WEB_APP_NAME;
        String secondSandboxEndpoint =
                gatewayUrlsWrk.getWebAppURLHttp() + APIMIntegrationConstants.SANDBOXEP2_WEB_APP_NAME;
        String thirdSandboxEndpoint =
                gatewayUrlsWrk.getWebAppURLHttp() + APIMIntegrationConstants.SANDBOXEP3_WEB_APP_NAME;

        List<String> endpointSandbox = new ArrayList<String>();
        endpointSandbox.add(firstSandboxEndpoint);
        endpointSandbox.add(secondSandboxEndpoint);
        endpointSandbox.add(thirdSandboxEndpoint);

        providerName = publisherContext.getContextTenant().getContextUser().getUserName();

        APICreationRequestBean apiSBCreationRequestBean = new APICreationRequestBean(apiNameSandbox, contextSandbox,
                version, providerName, endpointProd, endpointSandbox);
        apiSBCreationRequestBean.setTiersCollection(tier);
        apiSBCreationRequestBean.setResourceBeanList(resourceBeanList);
        apiSBCreationRequestBean.setVisibility(visibility);
        apiSBCreationRequestBean.setTags(tagsSandbox);
        apiSBCreationRequestBean.setDescription(descriptionSandbox);
        apiSBCreationRequestBean.setEndpointType(endPointType);

        HttpResponse apiSBCreateResponse = apiPublisher.addAPI(apiSBCreationRequestBean);
        assertEquals(apiSBCreateResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Invalid Response code");
        JSONObject apiSBCreateJsonObject = new JSONObject(apiSBCreateResponse.getData());
        assertFalse(apiSBCreateJsonObject.getBoolean("error"), "Response Data is Mismatched in API Creation");

        HttpResponse verifyAPI = apiPublisher.getAPI(apiNameSandbox, providerName, version);
        assertEquals(verifyAPI.getResponseCode(), Response.Status.OK.getStatusCode(), "Response Code Mismatched");
        JSONObject verifyApiJSONObject = new JSONObject(verifyAPI.getData());
        assertFalse(verifyApiJSONObject.getBoolean("error"), "API is not exists");

        APILifeCycleStateRequest updateStateRequest = new APILifeCycleStateRequest(apiNameSandbox, providerName,
                APILifeCycleState.PUBLISHED);
        HttpResponse stateUpdateResponse = apiPublisher.changeAPILifeCycleStatus(updateStateRequest);
        assertEquals(stateUpdateResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response Code Mismatched in publish API");
        JSONObject apiPublishedJsonObject = new JSONObject(stateUpdateResponse.getData());
        assertFalse(apiPublishedJsonObject.getBoolean("error"), "API is not published");
    }

    @Test(groups = { "wso2.am" }, description = "Verify Round Robin Algorithm by Invoking the Sandbox Endpoint API",
            dependsOnMethods = { "testCreateApiWithBothProdAndSandboxEndpoints" })
    public void testRoundRobinAlgorithmInProductionAndSandboxEndpoints() throws Exception {

        String webAppSandboxResponsePrefix = "HelloWSO2 from File ";
        String webAppSandboxResponseSuffix = "_Sandbox";

        String accessUrl = gatewayUrl + contextSandbox + "/" + version + "/name";

        apiStore.addApplication(applicationNameSandbox, appTier, "", "");

        providerName = storeContext.getContextTenant().getContextUser().getUserName();

        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiNameSandbox, version, providerName,
                applicationNameSandbox, tier);
        HttpResponse subscriptionResponse = apiStore.subscribe(subscriptionRequest);
        assertEquals(subscriptionResponse.getResponseCode(), Response.Status.OK.getStatusCode());
        JSONObject subscriptionJsonObject = new JSONObject(subscriptionResponse.getData());
        assertFalse(subscriptionJsonObject.getBoolean("error"), "Error in Subscription Response");

        //generate production endpoint key
        APPKeyRequestGenerator appKeyRequestGeneratorProduction = new APPKeyRequestGenerator(applicationNameSandbox);
        String responseStringProductionEndpoint = apiStore.generateApplicationKey(appKeyRequestGeneratorProduction)
                .getData();
        JSONObject productionEndpointJsonObject = new JSONObject(responseStringProductionEndpoint);
        String accessTokenProduction = productionEndpointJsonObject.getJSONObject("data").getJSONObject("key")
                .getString("accessToken");

        Map<String, String> applicationHeaderProduction = new HashMap<String, String>();
        applicationHeaderProduction.put("Authorization", " Bearer " + accessTokenProduction);

        //generate sandbox endpoint key
        APPKeyRequestGenerator appKeyRequestGenerator = new APPKeyRequestGenerator(applicationNameSandbox);
        appKeyRequestGenerator.setKeyType("SANDBOX");
        String responseString = apiStore.generateApplicationKey(appKeyRequestGenerator).getData();

        JSONObject jsonObject = new JSONObject(responseString);
        String accessTokenSandbox = jsonObject.getJSONObject("data").getJSONObject("key").getString("accessToken");

        Map<String, String> applicationHeaderSandbox = new HashMap<String, String>();
        applicationHeaderSandbox.put("Authorization", " Bearer " + accessTokenSandbox);

        //Verify Round Robin Algorithm by invoking the api multiple times
        HttpResponse apiSandboxInvokeResponse;
        HttpResponse apiProductionInvokeResponse;

        int requestCount = 10;
        int numberOfProductionEndpoints = 3;
        int numberOfSandboxEndpoints = 3;
        int requestNumber;

        //TODO replace sleep with proper method
        Thread.sleep(80000);
        //production end point api invoke with production key
        for (requestNumber = 1; requestNumber < requestCount; requestNumber++) {
            apiProductionInvokeResponse = HttpRequestUtil.doGet(accessUrl, applicationHeaderProduction);
            assertEquals(apiProductionInvokeResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response Code Mismatched in Production API invoke");

            int remainder = requestNumber % numberOfProductionEndpoints;
            if (remainder == 0) {
                log.info(apiProductionInvokeResponse.getData());
                assertEquals(apiProductionInvokeResponse.getData(), productionEndpointPrefix + "3",
                        "Error in Round Robin Algorithm in cycle ");
            } else {
                log.info(apiProductionInvokeResponse.getData());
                assertEquals(apiProductionInvokeResponse.getData(), productionEndpointPrefix + remainder,
                        "Error in Production Endpoint Round Robin Algorithm in request: " + requestNumber);
            }
        }

        //sandbox endpoint api invoke with sandbox key
        for (requestNumber = 1; requestNumber < requestCount; requestNumber++) {
            apiSandboxInvokeResponse = HttpRequestUtil.doGet(accessUrl, applicationHeaderSandbox);
            assertEquals(apiSandboxInvokeResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response Code Mismatch in Sandbox Endpoint invoke");

            int remainder = requestNumber % numberOfSandboxEndpoints;
            if (remainder == 0) {
                log.info(apiSandboxInvokeResponse.getData());
                assertEquals(apiSandboxInvokeResponse.getData(), webAppSandboxResponsePrefix + "3" +
                        webAppSandboxResponseSuffix, "Error in Round Robin Algorithm in cycle ");
            } else {
                log.info(apiSandboxInvokeResponse.getData());
                assertEquals(apiSandboxInvokeResponse.getData(), webAppSandboxResponsePrefix + remainder +
                        webAppSandboxResponseSuffix, "Error in Round Robin Algorithm in Cycle " + requestNumber);
            }
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeAPISubscriptionByName(apiName, version, providerName, applicationName);
        apiStore.removeAPISubscriptionByName(apiNameSandbox, version, providerName, applicationNameSandbox);
        apiStore.removeApplication(applicationName);
        apiStore.removeApplication(applicationNameSandbox);
        apiPublisher.deleteAPI(apiName, version, providerName);
        apiPublisher.deleteAPI(apiNameSandbox, version, providerName);

        List<String> webAppList = new ArrayList<String>();
        webAppList.add(APIMIntegrationConstants.PRODEP1_WEB_APP_NAME);
        webAppList.add(APIMIntegrationConstants.PRODEP2_WEB_APP_NAME);
        webAppList.add(APIMIntegrationConstants.PRODEP3_WEB_APP_NAME);
        webAppList.add(APIMIntegrationConstants.SANDBOXEP1_WEB_APP_NAME);
        webAppList.add(APIMIntegrationConstants.SANDBOXEP2_WEB_APP_NAME);
        webAppList.add(APIMIntegrationConstants.SANDBOXEP3_WEB_APP_NAME);
        webAppAdminClient.deleteWebAppList(webAppList, gatewayContextMgt.getDefaultInstance().getHosts().get("default"));

        super.cleanUp();
    }

    /**
     * Verify whether WEB_APPs are deployed or not
     * @param webAppURL - Backend URL of the webApp (not need to include webApp name or context)
     *
     */
    public static void isWebAppDeployed(String webAppURL, String webAppName, String expectedResponse) {
        long currentTime = System.currentTimeMillis();
        long waitTime = currentTime + (90 * 1000);
        HttpResponse response = null;

        while (waitTime > System.currentTimeMillis()) {
            try {
                response = HttpRequestUtil.sendGetRequest(webAppURL + "/" + webAppName + "/name", null);

            } catch (IOException ignore) {
                log.info("WAIT for webapp deployment :" + webAppName);
            }

            if (response != null) {
                if (response.getData().contains(expectedResponse)) {
                    log.info("WEB_APP :" + webAppName + " found");
                    break;
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {

                    }
                }
            }
        }
    }
}
