/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.am.tests.sample;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.automation.tools.jmeter.JMeterTest;
import org.wso2.automation.tools.jmeter.JMeterTestManager;
import org.wso2.carbon.am.tests.APIManagerIntegrationTest;
import org.wso2.carbon.am.tests.util.bean.APILifeCycleState;
import org.wso2.carbon.am.tests.util.APIMgtTestUtil;
import org.wso2.carbon.am.tests.util.APIPublisherRestClient;
import org.wso2.carbon.am.tests.util.APIStoreRestClient;
import org.wso2.carbon.am.tests.util.bean.*;
import org.wso2.carbon.automation.core.ProductConstant;
import org.wso2.carbon.automation.core.utils.serverutils.ServerConfigurationManager;
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

public class HostObjectTestCase extends APIManagerIntegrationTest {
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private ServerConfigurationManager serverConfigurationManager;
    private String publisherURLHttp;
    private String storeURLHttp;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init(0);
        if (isBuilderEnabled()) {
            /*
            If test run in external distributed deployment you need to copy following resources accordingly.
            configFiles/hostobjecttest/api-manager.xml
            configFiles/tokenTest/log4j.properties
            Also need to copy the content of /resources/artifacts/AM/jaggery to servers following folder folder
            repository/deployment/server/jaggeryapps/testapp
            */
            publisherURLHttp = getServerURLHttp();
            storeURLHttp = getServerURLHttp();
            serverConfigurationManager = new ServerConfigurationManager(amServer.getBackEndUrl());
            serverConfigurationManager.applyConfiguration(new File(ProductConstant.getResourceLocations(ProductConstant.AM_SERVER_NAME)
                    + File.separator + "configFiles/hostobjecttest/" + "api-manager.xml"));
            serverConfigurationManager.applyConfiguration(new File(ProductConstant.getResourceLocations(ProductConstant.AM_SERVER_NAME)
                    + File.separator + "configFiles/tokenTest/" + "log4j.properties"));
            super.init(0);
        } else {
            publisherURLHttp = getPublisherServerURLHttp();
            storeURLHttp = getStoreServerURLHttp();
        }
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiStore = new APIStoreRestClient(storeURLHttp);
    }

    private void copySampleFile(String sourcePath, String destPath) {
        File sourceFile = new File(sourcePath);
        File destFile = new File(destPath);
        try {
            FileManipulator.copyFile(sourceFile, destFile);
        } catch (IOException e) {
            log.error("Error while copying the sample into Jaggery server", e);
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

        String sourcePath = ProductConstant.getResourceLocations(ProductConstant.AM_SERVER_NAME)
                + File.separator + "jaggery/" + fileName;
        return sourcePath;
    }

    @Test(groups = {"wso2.am"}, description = "API Life cycle test case")
    public void testHostObjectTestCase() throws Exception {
        //Tenant Create test cases -  This will create new tenant in the system
        JMeterTest script =
                new JMeterTest(new File(ProductConstant.SYSTEM_TEST_RESOURCE_LOCATION + File.separator + "artifacts"
                        + File.separator + "AM" + File.separator + "scripts"
                        + File.separator + "tenant_create.jmx"));
        JMeterTestManager manager = new JMeterTestManager();
        //manager.runTest(script);
        //End of tenant creation


        String APIName = "HostObjectTestAPI";
        String APIContext = "HostObjectTestAPI";
        String tags = "youtube, video, media";
        String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
        String description = "This is test API create by API manager integration test";
        String providerName = "admin";
        String APIVersion = "1.0.0";
        apiPublisher.login(userInfo.getUserName(), userInfo.getPassword());
        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiPublisher.addAPI(apiRequest);
        apiPublisher.deleteApi(APIName, APIVersion, providerName);
        apiPublisher.addAPI(apiRequest);
        APIBean apiBean = APIMgtTestUtil.getAPIBeanFromHttpResponse(apiPublisher.getApi(APIName, providerName));
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(APIName, providerName, APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatusTo(updateRequest);
        //Test API properties
        Assert.assertEquals(apiBean.getId().getApiName(), APIName, "API Name mismatch");
        Assert.assertEquals(apiBean.getContext().trim().substring(apiBean.getContext().indexOf("/") + 1), APIContext, "API context mismatch");
        Assert.assertEquals(apiBean.getId().getVersion(), APIVersion, "API version mismatch");
        Assert.assertEquals(apiBean.getId().getProviderName(), providerName, "Provider Name mismatch");
        for (String tag : apiBean.getTags()) {
            Assert.assertTrue(tags.contains(tag), "API tag data mismatched");
        }
        Assert.assertEquals(apiBean.getDescription(), description, "API description mismatch");
        apiStore.login(userInfo.getUserName(), userInfo.getPassword());

        // Create application
        apiStore.addApplication("HostObjectApplication", "Gold", "", "this-is-test");

        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName, userInfo.getUserName());
        subscriptionRequest.setApplicationName("HostObjectApplication");
        apiStore.subscribe(subscriptionRequest);
        apiPublisher.addDocument(APIName, APIVersion, providerName, "Doc-Name", "How To", "In-line", "url-no-need", "summary", "");



        GenerateAppKeyRequest generateAppKeyRequest = new GenerateAppKeyRequest("HostObjectApplication");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        //host object tests
        String fileName = "testPublisher.jag";
        String sourcePath = computeSourcePath(fileName);
        String destinationPath = computeDestPath(fileName);
        copySampleFile(sourcePath, destinationPath);


        fileName = "testStore.jag";
        sourcePath = computeSourcePath(fileName);
        destinationPath = computeDestPath(fileName);
        copySampleFile(sourcePath, destinationPath);


        Thread.sleep(20000);
        String finalOutputPublisher = null;
        //ClientConnectionUtil.waitForPort(9763, "");
        try {
            URL jaggeryURL = new URL(getTestApplicationPublisherServerURLHttp());
            URLConnection jaggeryServerConnection = jaggeryURL.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    jaggeryServerConnection.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                finalOutputPublisher = inputLine;
            }
            String[] arr = finalOutputPublisher.split("==");
            System.out.println(finalOutputPublisher);
            validatePublisherResponseArray(arr);
            in.close();
        } catch (IOException e) {
            log.error("Error while invoking test application to test publisher host object");
        } finally {
            assertNotNull(finalOutputPublisher, "Result cannot be null");
        }

        String finalOutputStore = null;
        Thread.sleep(2000);
        //ClientConnectionUtil.waitForPort(9763, "");
        try {
            URL jaggeryURL = new URL(getTestApplicationStoreServerURLHttp());
            URLConnection jaggeryServerConnection = jaggeryURL.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    jaggeryServerConnection.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                finalOutputStore = inputLine;
            }
            String[] arr = finalOutputStore.split("==");
            validateStoreResponseArray(arr);
            System.out.println(finalOutputStore);
            in.close();
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
        super.cleanup();
    }

    public static boolean validateStoreResponseArray(String[] array) {
        //This order is based on the operations of jaggery file operation order
        //If we edit jaggery file we need to modify this as well
        Assert.assertTrue(array[1].contains("true"), "Error while getting status of billing system from API store host object (isBillingEnabled)");
        Assert.assertTrue(array[2].contains("https"), "Error while getting https url from API store host object (getHTTPsURL)");
        Assert.assertTrue(array[4].contains("http"), "Error while getting http url from API store host object (getHTTPURL)");
        Assert.assertTrue(array[3].contains("services"), "Error while getting auth service url from API store host object (getAuthServerURL)");
        Assert.assertTrue(array[5].contains("[{\"tierName\" : \"Gold\"}]"), "Error while getting denied tiers from API store host object (getDeniedTiers)");
       // Assert.assertTrue(array[6].contains("tenantdomain1.com"), "Error while getting active tenant domains from API store host object (getActiveTenantDomains)");
        Assert.assertTrue(array[7].contains("true"), "Error while getting status of self sign in from API store host object (isSelfSignupEnabled)");
        Assert.assertTrue(array[8].contains("fieldName"), "Error while getting user fields from API store host object (getUserFields)");
       // Assert.assertTrue(array[9].contains("HostObjectTestAPI"), "Error while search Paginated APIs By Type from API store host object (searchPaginatedAPIsByType)");
       // Assert.assertTrue(array[10].contains("HostObjectTestAPI"), "Error while search Paginated APIs By Type with pattern * from API store host object (searchPaginatedAPIsByType)");
       // Assert.assertTrue(array[11].contains("HostObjectTestAPI"), "Error while search API by Type with pattern * from API store host object (searchAPIbyType)");
       // Assert.assertTrue(array[12].contains("HostObjectTestAPI"), "Error while search API by type from API store host object (searchAPIbyType)");
        Assert.assertTrue(array[13].contains("HostObjectTestAPI"), "Error while getting paginated APIs with tag from API store host object (getPaginatedAPIsWithTag)");
        //14 getPublishedAPIsByProvider
        //15 getAllSubscriptions
        Assert.assertTrue(array[16].contains("HostObjectTestAPI"), "Error while rating API from API store host object (rateAPI)");
        Assert.assertTrue(array[17].contains("[{\"newRating\" : \"0.0\"}]"), "Error while removing rating from API store host object (removeAPIRating)");
        Assert.assertTrue(array[18].contains("apis"), "Error while getting Paginated , published APIs from API store host object (getAllPaginatedPublishedAPIs)");
        Assert.assertTrue(array[19].contains("HostObjectTestAPI"), "Error while getting APIs With Tag from API store host object (getAPIsWithTag)");
        Assert.assertTrue(array[20].contains("HostObjectTestAPI"), "Error while getting all published APIs from API store host object (getAllPublishedAPIs)");
        //21 getComments
        Assert.assertTrue(array[22].contains("true"), "Error while checking user in the system from API store host object (isUserExists)");
        Assert.assertTrue(array[23].contains("HostObjectTestAPI"), "Error while getting API from API store host object (getAPI)");
        Assert.assertTrue(array[24].contains("true"), "Error while checking subscription state from API store host object (isSubscribed)");
        Assert.assertTrue(array[25].contains("application"), "Error while getting subscriptions from API store host object (getSubscriptions)");
        //26 getSubscribedAPIs
        Assert.assertTrue(array[27].contains("true"), "Error while checking user permission from API store host object (hasUserPermissions)");
        //28 getTiers
        //29 getAPIUsageforSubscriber
        //30 getDeniedTiers
        //31 getRecentlyAddedAPIs
        //32 getTopRatedAPIs
        Assert.assertTrue(array[33].contains("true"), "Error while getting billing status from API store host object (isBillingEnabled)");
        Assert.assertTrue(array[34].contains("true"), "Error while checking Subscribe Permission from API store host object (hasSubscribePermission)");
        Assert.assertTrue(array[35].contains("false"), "Error while getting state of Email Username from API store host object (isEnableEmailUsername)");
        Assert.assertTrue(array[36].contains("true"), "Error while update Application Tier from API store host object (updateApplicationTier)");
        Assert.assertTrue(array[37].contains("true"), "Error while update Application from API store host object (updateApplication)");
        Assert.assertTrue(array[38].contains("200"), "Error while validate WF Permission from API store host object (validateWFPermission)");
        Assert.assertTrue(array[39].contains("HostObjectTestAPI-1.0.0-admin/api-doc.json"), "Error while getting Swagger Discovery Url from API store host object (getSwaggerDiscoveryUrl)");
        return true;
    }


    public static boolean validatePublisherResponseArray(String[] array) {
        //This order is based on the operations of jaggery file operation order
        //If we edit jaggary file we need to modify this as well
        Assert.assertNotNull(array[1], "Error while getting external api stores from API store host object (getExternalAPIStores)");
        Assert.assertTrue(array[2].contains("true"), "Error while validating roles from API store host object (validateRoles)");
        Assert.assertTrue(array[3].contains("success"), "Error while checking url validity from API store host object (isURLValid)");
        Assert.assertTrue(array[4].contains("HostObjectTestAPI"), "Error while getting APIs by provider from API store host object (getAPIsByProvider)");
        Assert.assertTrue(array[5].contains("HostObjectTestAPI"), "Error while getting subscribed APIs from API store host object (getSubscribedAPIs)");
        Assert.assertTrue(array[6].contains("HostObjectTestAPI"), "Error while getting API from API store host object (getAPI)");
        Assert.assertTrue(array[7].contains("Bronze"), "Error while getting tier permission from API store host object (getTierPermissions)");
        Assert.assertTrue(array[8].contains("Bronze"), "Error while getting tiers from API store host object (getTiers)");
        Assert.assertTrue(array[9].contains("HostObjectTestAPI"), "Error while getting all APIs By Type from API store host object (getAllAPIs)");
        Assert.assertTrue(array[10].contains("HostObjectTestAPI"), "Error while getting APIs By provider with pattern * from API store host object (getAPIsByProvider)");
        Assert.assertTrue(array[11].contains("subscribedDate"), "Error while getting subscribers of API from API store host object (getSubscribersOfAPI)");
        Assert.assertTrue(array[12].contains("false"), "Error while checking contexts from API store host object (isContextExist)");
        Assert.assertTrue(array[13].contains("HostObjectTestAPI"), "Error while searching APIs from API store host object (searchAPIs)");
        Assert.assertTrue(array[14].contains("true"), "Error while checking create permission from API store host object (hasCreatePermission)");
        Assert.assertTrue(array[15].contains("true"), "Error while checking manage tier permission from API store host object (hasManageTierPermission)");
        Assert.assertTrue(array[16].contains("true"), "Error while checking user permission from API store host object (hasUserPermissions)");
        Assert.assertTrue(array[17].contains("true"), "Error while checking publisher permissions (hasPublishPermission)");
        Assert.assertTrue(array[18].contains("services"), "Error while getting auth server url from API store host object (getAuthServerURL)");
        Assert.assertTrue(array[19].contains("[\"log_in_message\"]"), "Error while getting in sequences from API store host object (getCustomInSequences)");
        Assert.assertTrue(array[20].contains("[\"log_out_message\"]"), "Error while getting out sequences from API store host object (getCustomOutSequences)");
        Assert.assertTrue(array[21].contains("https"), "Error while getting https url from API store host object (getHTTPsURL)");
        Assert.assertTrue(array[22].contains("true"), "Error while checking gateway type from API store host object (isSynapseGateway)");
        Assert.assertTrue(array[23].contains("null"), "Error while load Registry Of Tenant API store host object (loadRegistryOfTenant)");
        Assert.assertTrue(array[24].contains("true"), "Error while update Documentation from API store host object (updateDocumentation)");
        //Assert.assertTrue(array[25].contains("null"), "Error while adding Inline Content from API store host object (addInlineContent)");
        Assert.assertTrue(array[26].contains("providerName"), "Error while getting Inline Content from API store host object (getInlineContent)");
        //Assert.assertTrue(array[27].contains("docName"), "Error while getting All Documentation from API store host object (getAllDocumentation)");
        //Assert.assertTrue(array[28].contains("token"), "Error while search Access Tokens from API store host object (searchAccessTokens)");
        //Assert.assertTrue(array[29].contains("true"), "Error while checking user permission from API store host object (getSubscriberCountByAPIs)");
        //Assert.assertTrue(array[30].contains("true"), "Error while checking user permission from API store host object (getSubscriberCountByAPIVersions)");
        //Assert.assertTrue(array[31].contains("null"), "Error while getting External API Stores from API store host object (getExternalAPIStores)");
        Assert.assertTrue(array[32].contains("false"), "Error while checking API Older Versions from API store host object (isAPIOlderVersionExist)");
        Assert.assertTrue(array[33].contains("true"), "Error while update Subscription Status from API store host object (updateSubscriptionStatus)");
        Assert.assertTrue(array[34].contains("true"), "Error while update Tier Permissions from API store host object (updateTierPermissions)");
        //Assert.assertTrue(array[35].contains("404"), "Error while testing url endpoint validating (isURLValid)");
        Assert.assertTrue(array[35].contains("HostObjectTestAPI"), "Error while searching APIs from API store host object (searchAPIs)");
        return true;
    }
}
