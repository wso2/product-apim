/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/


package org.wso2.am.integration.tests.hostobject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APIBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.extensions.jmeter.JMeterTest;
import org.wso2.carbon.automation.extensions.jmeter.JMeterTestManager;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.FileManipulator;
import org.wso2.carbon.utils.ServerConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class HostObjectTestCase extends APIMIntegrationBaseTest {
    private Log log = LogFactory.getLog(getClass());
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;

    @Factory(dataProvider = "userModeDataProvider")
    public HostObjectTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        /*
        If test run in external distributed deployment you need to copy following resources accordingly.
        configFiles/hostobjecttest/api-manager.xml
        configFiles/tokenTest/log4j.properties
        Also need to copy the content of /resources/artifacts/AM/jaggery to servers following folder folder
        repository/deployment/server/jaggeryapps/testapp
        */
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();
        ServerConfigurationManager serverConfigurationManager =
                new ServerConfigurationManager(new AutomationContext("APIM", "gateway",
                                                                     TestUserMode.SUPER_TENANT_ADMIN));

        serverConfigurationManager.applyConfiguration(new File(getAMResourceLocation()
                                                               + File.separator +
                                                               "configFiles/hostobjecttest/" +
                                                               "api-manager.xml"));
        serverConfigurationManager.applyConfiguration(new File(getAMResourceLocation()
                                                               + File.separator +
                                                               "configFiles/tokenTest/" +
                                                               "log4j.properties"));
        super.init(userMode);

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);


    }

    private void copySampleFile(String sourcePath, String destPath) {
        File sourceFile = new File(sourcePath);
        File destFile = new File(destPath);
        try {
            FileManipulator.copyFile(sourceFile, destFile);
        } catch (IOException e) {
            log.error("Error while copying the other into Jaggery server", e);
        }
    }

    private String computeDestPath(String fileName) {
        String serverRoot = System.getProperty(ServerConstants.CARBON_HOME);
        String deploymentPath = serverRoot + "/repository/deployment/server/jaggeryapps/testapp";
        File depFile = new File(deploymentPath);
        if (!depFile.exists() && !depFile.mkdir()) {
            log.error("Error while creating the deployment folder : "
                      + deploymentPath);
        }
        return deploymentPath + File.separator + fileName;
    }

    private String computeSourcePath(String fileName) {

        return getAMResourceLocation()
               + File.separator + "jaggery/" + fileName;
    }

    @Test(groups = {"wso2.am"}, description = "API Life cycle test case")
    public void testHostObjectTestCase() throws Exception {

        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                           publisherContext.getContextTenant().getContextUser().getPassword());
        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                       storeContext.getContextTenant().getContextUser().getPassword());

        //Tenant Create test cases -  This will create new tenant in the system
        JMeterTest script =
                new JMeterTest(new File(getAMResourceLocation() + File.separator + "scripts"
                                        + File.separator + "tenant_create.jmx"));
        JMeterTestManager manager = new JMeterTestManager();
        manager.runTest(script);
        //End of tenant creation

        String APIName = "HostObjectTestAPI";
        String APIContext = "HostObjectTestAPIAPIContext";
        String tags = "youtube, video, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";
        String providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        String APIVersion = "1.0.0";

        String filePublisher, fileStore;
        if (gatewayContext.getContextTenant().getDomain().equals("carbon.super")) {
            filePublisher = "testPublisher.jag";
            fileStore = "testStore.jag";
        } else {
            filePublisher = "testPublisherTenant.jag";
            fileStore = "testStoreTenant.jag";
        }

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiPublisher.addAPI(apiRequest);
        apiPublisher.deleteAPI(APIName, APIVersion, providerName);
        apiPublisher.addAPI(apiRequest);
        APIBean apiBean = APIMTestCaseUtils
                .getAPIBeanFromHttpResponse(apiPublisher.getAPI(APIName, providerName));
        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(APIName, providerName, APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);
        //Test API properties
        assertEquals(apiBean.getId().getApiName(), APIName, "API Name mismatch");
        assertTrue(apiBean.getContext().contains(APIContext), "API context mismatch");
        assertEquals(apiBean.getId().getVersion(), APIVersion, "API version mismatch");
        assertEquals(apiBean.getId().getProviderName(), providerName,
                     "Provider Name mismatch");
        for (String tag : apiBean.getTags()) {
            assertTrue(tags.contains(tag), "API tag data mismatched");
        }
        assertEquals(apiBean.getDescription(), description, "API description mismatch");
        apiStore.addApplication("HostObjectTestAPI-Application", "Gold", "", "this-is-test");

        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName, providerName);
        subscriptionRequest.setApplicationName("HostObjectTestAPI-Application");
        apiStore.subscribe(subscriptionRequest);
        apiPublisher.addDocument(APIName, APIVersion, providerName, "Doc-Name", "How To", "In-line",
                                 "url-no-need", "summary", "");
        APPKeyRequestGenerator generateAppKeyRequest =
                new APPKeyRequestGenerator("HostObjectTestAPI-Application");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken =
                response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        //host object tests
        String sourcePath = computeSourcePath(filePublisher);
        String destinationPath = computeDestPath(filePublisher);
        copySampleFile(sourcePath, destinationPath);

        sourcePath = computeSourcePath(fileStore);
        destinationPath = computeDestPath(fileStore);
        copySampleFile(sourcePath, destinationPath);

        String finalOutputPublisher = null;
        int deploymentDelayInMilliseconds = 90 * 1000;
        long startTime = System.currentTimeMillis();

        try {

//            validatePublisherResponseArray(arr);
            String[] responseArrayFromPublisher = new String[35];

            boolean isPublisherResponse = false;
            while (((System.currentTimeMillis() - startTime) < deploymentDelayInMilliseconds) && !isPublisherResponse) {
                Thread.sleep(500);
                System.out.println("1 : " + " " + ((System.currentTimeMillis() - startTime) < deploymentDelayInMilliseconds) +
                                   " " + isPublisherResponse +"  "+gatewayContext.getContextTenant().getDomain());
                URL jaggeryURL = new URL(publisherUrls.getWebAppURLHttp() + "testapp/" + filePublisher);
                URLConnection jaggeryServerConnection = jaggeryURL.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        jaggeryServerConnection.getInputStream()));

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    finalOutputPublisher = inputLine;
                }
                if (null != finalOutputPublisher) {
                    responseArrayFromPublisher = finalOutputPublisher.split("==");
                    isPublisherResponse = responseArrayFromPublisher[30].contains("HostObjectTestAPI");
                }
                log.info(finalOutputPublisher);
                in.close();

            }
            validatePublisherResponseArray(responseArrayFromPublisher);

        } catch (IOException e) {
            log.error("Error while invoking test application to test publisher host object");
        } finally {
            assertNotNull(finalOutputPublisher, "Result cannot be null");
        }
        String finalOutputStore = null;
        try {
            String[] responseArrayFromStore = new String[27];
            boolean isStoreResponse = false;
            startTime = System.currentTimeMillis();
            while (((System.currentTimeMillis() - startTime) < deploymentDelayInMilliseconds) && !isStoreResponse) {
                Thread.sleep(500);
                System.out.println("2 : " +" "+((System.currentTimeMillis() - startTime) < deploymentDelayInMilliseconds) +
                                   " " + isStoreResponse+" "+gatewayContext.getContextTenant().getDomain());
                URL jaggeryURL = new URL(storeUrls.getWebAppURLHttp() + "testapp/" + fileStore);
                URLConnection jaggeryServerConnection = jaggeryURL.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        jaggeryServerConnection.getInputStream()));

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    finalOutputStore = inputLine;
                }
                if (null != finalOutputStore) {
                    responseArrayFromStore = finalOutputStore.split("==");
                    isStoreResponse = responseArrayFromStore[9].contains("HostObjectTestAPI");
                }
                in.close();
                log.info(finalOutputStore);
            }
            validateStoreResponseArray(responseArrayFromStore);


        } catch (IOException e) {
            log.error("Error while invoking test application to test publisher host object");
        } finally {
            assertNotNull(finalOutputStore, "Result cannot be null");
        }
    }

    @Test(groups = {"wso2.am"}, description = "Test application object")
    public void testApplication() {
    }

    @Test(groups = {"wso2.am"}, description = "Test application operations")
    public void testApplicationOperations() {
    }

    @Test(groups = {"wso2.am"}, description = "Test application operations")
    public void testAPIProvider() {
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeApplication("HostObjectTestAPI-Application");
        super.cleanup();
    }

    public static boolean validateStoreResponseArray(String[] array) {
        assertTrue(array[1].contains("true"),
                   "Error while getting status of billing system from API store host object (isBillingEnabled)");
        assertTrue(array[2].contains("https"),
                   "Error while getting https url from API store host object (getHTTPsURL)");
        assertTrue(array[3].contains("services"),
                   "Error while getting auth service url from API store host object (getAuthServerURL)");
        assertTrue(array[4].contains("http"),
                   "Error while getting http url from API store host object (getHTTPURL)");
        assertTrue(array[5].contains("tierName"),
                   "Error while getting denied tiers from API store host object (getDeniedTiers)");
        assertTrue(array[6].contains("tenantdomain1.com"),
                   "Error while getting active tenant domains from API store host object (getActiveTenantDomains)");
        assertTrue(array[7].contains("false"),
                   "Error while getting status of self sign in from API store host object (isSelfSignupEnabled)");
        assertTrue(array[8].contains("fieldName"),
                   "Error while getting user fields from API store host object (getUserFields)");
        assertTrue(array[9].contains("HostObjectTestAPI"),
                   "Error while search Paginated APIs By Type from API store host object (searchPaginatedAPIsByType)");
        assertTrue(array[10].contains("HostObjectTestAPI"),
                   "Error while search Paginated APIs By Type with pattern * from API store host object (searchPaginatedAPIsByType)");
        assertTrue(array[11].contains("HostObjectTestAPI"),
                   "Error while getting paginated APIs with tag from API store host object (getPaginatedAPIsWithTag)");
        assertTrue(array[12].contains("HostObjectTestAPI"),
                   "Error while rating API from API store host object (rateAPI)");
        assertTrue(array[13].contains("newRating"),
                   "Error while removing rating from API store host object (removeAPIRating)");
        assertTrue(array[14].contains("apis"),
                   "Error while getting Paginated , published APIs from API store host object (getAllPaginatedPublishedAPIs)");
        assertTrue(array[15].contains("HostObjectTestAPI"),
                   "Error while getting APIs With Tag from API store host object (getAPIsWithTag)");
        assertTrue(array[16].contains("HostObjectTestAPI"),
                   "Error while getting all published APIs from API store host object (getAllPublishedAPIs)");
        assertTrue(array[17].contains("true"),
                   "Error while checking user in the system from API store host object (isUserExists)");
        assertTrue(array[18].contains("HostObjectTestAPI"),
                   "Error while getting API from API store host object (getAPI)");
        assertTrue(array[19].contains("true"),
                   "Error while checking subscription state from API store host object (isSubscribed)");
        //      assertTrue(array[20].contains("application"),
        //               "Error while getting subscriptions from API store host object (getSubscriptions)");
        assertTrue(array[21].contains("true"),
                   "Error while checking user permission from API store host object (hasUserPermissions)");
        assertTrue(array[22].contains("true"),
                   "Error while checking Subscribe Permission from API store host object (hasSubscribePermission)");
        assertTrue(array[23].contains("true"),
                   "Error while update Application Tier from API store host object (updateApplicationTier)");
        assertTrue(array[24].contains("true"),
                   "Error while update Application from API store host object (updateApplication)");
        assertTrue(array[25].contains("200"),
                   "Error while validate WF Permission from API store host object (validateWFPermission)");
        assertTrue(array[26].contains("false"),
                   "Error while getting state of Email Username from API store host object (isEnableEmailUsername)");


        return true;
    }

    public static boolean validatePublisherResponseArray(String[] array) {

        assertTrue(array[1].contains("true"),
                   "Error while validating roles from API store host object (validateRoles)");
        assertTrue(array[2].contains("success"),
                   "Error while checking url validity from API store host object (isURLValid)");
        assertTrue(array[3].contains("HostObjectTestAPI"),
                   "Error while getting APIs by provider from API store host object (getAPIsByProvider)");
        assertTrue(array[4].contains("HostObjectTestAPI"),
                   "Error while getting subscribed APIs from API store host object (getSubscribedAPIs)");
        assertTrue(array[5].contains("HostObjectTestAPI"),
                   "Error while getting API from API store host object (getAPI)");
        assertTrue(array[6].contains("Bronze"),
                   "Error while getting tier permission from API store host object (getTierPermissions)");
        assertTrue(array[7].contains("Bronze"),
                   "Error while getting tiers from API store host object (getTiers)");
        assertTrue(array[8].contains("HostObjectTestAPI"),
                   "Error while getting all APIs By Type from API store host object (getAllAPIs)");
        assertTrue(array[9].contains("HostObjectTestAPI"),
                   "Error while getting APIs By provider with pattern * from API store host object (getAPIsByProvider)");
        assertTrue(array[10].contains("subscribedDate"),
                   "Error while getting subscribers of API from API store host object (getSubscribersOfAPI)");
        assertTrue(array[11].contains("false"),
                   "Error while checking contexts from API store host object (isContextExist)");
        assertTrue(array[12].contains("HostObjectTestAPI"),
                   "Error while searching APIs from API store host object (searchAPIs)");
        assertTrue(array[13].contains("true"),
                   "Error while checking create permission from API store host object (hasCreatePermission)");
        assertTrue(array[14].contains("true"),
                   "Error while checking manage tier permission from API store host object (hasManageTierPermission)");
        assertTrue(array[15].contains("true"),
                   "Error while checking user permission from API store host object (hasUserPermissions)");
        assertTrue(array[16].contains("true"),
                   "Error while checking publisher permissions (hasPublishPermission)");
        assertTrue(array[17].contains("services"),
                   "Error while getting auth server url from API store host object (getAuthServerURL)");
        assertTrue(array[18].contains("log_in_message"),
                   "Error while getting in sequences from API store host object (getCustomInSequences)");
        assertTrue(array[19].contains("log_out_message"),
                   "Error while getting out sequences from API store host object (getCustomOutSequences)");
        assertTrue(array[20].contains("https"),
                   "Error while getting https url from API store host object (getHTTPsURL)");
        assertTrue(array[21].contains("true"),
                   "Error while checking gateway type from API store host object (isSynapseGateway)");
        assertTrue(array[22].contains("null"),
                   "Error while load Registry Of Tenant API store host object (loadRegistryOfTenant)");
        assertTrue(array[23].contains("token"),
                   "Error while search Access Tokens from API store host object (searchAccessTokens)");
        assertTrue(array[24].contains("false"),
                   "Error while checking API Older Versions from API store host object (isAPIOlderVersionExist)");
        assertTrue(array[25].contains("true"),
                   "Error while update Subscription Status from API store host object (updateSubscriptionStatus)");
        assertTrue(array[26].contains("true"),
                   "Error while update Tier Permissions from API store host object (updateTierPermissions)");
        // API visibility test cases
        assertTrue(array[27].contains("HostObjectTestAPI"),
                   "Error while searching APIs by Version from API store host object (searchAPIs)");
        assertTrue(array[28].contains("HostObjectTestAPI"),
                   "Error while searching APIs by Status from API store host object (searchAPIs)");
        assertTrue(array[29].contains("HostObjectTestAPI"),
                   "Error while search API by part of the API name (searchAPIs)");
        assertTrue(array[30].contains("HostObjectTestAPI"),
                   "Error while search API by uppercase API name (searchAPIs)");
        assertTrue(array[31].contains("HostObjectTestAPI"),
                   "Error while search API by lowercase API name (searchAPIs)");
        assertTrue(array[32].contains("HostObjectTestAPI"),
                   "Error while search API by provider (searchAPIs)");
        assertTrue(array[33].contains("HostObjectTestAPI"),
                   "Error while search API by part of the provider (searchAPIs)");
        assertTrue(!array[34].contains("HostObjectTestAPI"),
                   "Error while search API by invalid search key (searchAPIs)");

        return true;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }
}
