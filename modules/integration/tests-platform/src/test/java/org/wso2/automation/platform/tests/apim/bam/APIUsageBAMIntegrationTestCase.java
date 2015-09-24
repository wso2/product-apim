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

package org.wso2.automation.platform.tests.apim.bam;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.utils.AuthenticateStubUtil;
import org.wso2.carbon.server.admin.stub.ServerAdminException;
import org.wso2.carbon.server.admin.stub.ServerAdminStub;
import org.wso2.carbon.server.admin.stub.types.carbon.ServerData;
import org.wso2.carbon.utils.FileManipulator;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/*

Note: This test case is not run with default API manager integration tests. To run this test we assume that BAM server
is running with port offset 1 and API Manager is running with offset 500. All the servers should be configured properly.

https://docs.wso2.com/display/AM190/Publishing+API+Runtime+Statistics

BAM
Configure the WSO2AM_STATS_DB data source in BAM
Start BAM server with offset 1 and deploy the API_Manager_Analytics.tbox

AM
Configure the WSO2AM_STATS_DB data source in AM
Copy jag files in resources/artifacts/AM/jaggery to repository/deployment/server/jaggeryapps/testapp
Enable APIUsageTracking in API manager and start the am server

 */

public class APIUsageBAMIntegrationTestCase extends APIMIntegrationBaseTest {

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String app1Name;
    private String app2Name;
    private final String apiName = "UsageTestAPI";
    private final String apiNameFaultyAPI = "UsageTestAPIFaultyAPI";
    private final String apiVersion = "1.0.0";
    private String[] arrayStat;
    private int faultCountBefore = 0;
    private int apiCountByUserBefore = 0;

    private int successRequestCount = 10;
    private int faultRequestCount = 10;

    private static final Log log = LogFactory.getLog(APIUsageBAMIntegrationTestCase.class);


    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException {

        super.init();

        apiPublisher = new APIPublisherRestClient(publisherUrls.getWebAppURLHttp());
        apiStore = new APIStoreRestClient(storeUrls.getWebAppURLHttp());

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        HttpResponse httpResponse;
        if(app1Name != null) {
            httpResponse = apiStore.removeApplication(app1Name);
            checkError(httpResponse.getData(), "Error removing application " + app1Name);
        }
        if(app2Name != null) {
            httpResponse = apiStore.removeApplication(app2Name);
            checkError(httpResponse.getData(), "Error removing application " + app2Name);
        }

        httpResponse = apiPublisher.deleteAPI(apiName, apiVersion
                , publisherContext.getSuperTenant().getContextUser().getUserName());
        checkError(httpResponse.getData(), "Error deleting API " + apiName);

        httpResponse = apiPublisher.deleteAPI(apiNameFaultyAPI, apiVersion
                , publisherContext.getSuperTenant().getContextUser().getUserName());
        checkError(httpResponse.getData(), "Error deleting API " + apiNameFaultyAPI);
        super.cleanUp();
    }


    @Test(groups = {"wso2.am"}, description = "APIM - BAM Integration API Usage statistics analysis test")
    public void testGenerateStatistics() throws Exception {
        String providerName = user.getUserName();
        String password = user.getPassword();


        String apiContext = "UsageTestAPI";
        String tags = "UsageTestAPI";
        String description = "This is test API create by API manager usage integration test";
        String visibility = "user";
        String url = "http://en.wikipedia.org/w/api.php";

        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(apiVersion);
        apiRequest.setVisibility(visibility);
        apiRequest.setProvider(providerName);
        HttpResponse response;

        response = apiPublisher.login(providerName, password);
        checkError(response.getData(), "Error while authenticating to publisher " + providerName);
        HttpResponse addApiResponse = apiPublisher.addAPI(apiRequest);
        checkError(addApiResponse.getData(), "Error while adding API " + apiName);


        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, providerName,
                                                                              APILifeCycleState.PUBLISHED);

        response = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        checkError(response.getData(), "Error while publishing API " + apiName);

        // subscribing to api
        response = apiStore.login(providerName, password);
        checkError(response.getData(), "Error when authenticating to Store " + providerName);
        app1Name = "Statistics-Application-" + new Random().nextInt(10000);
        response = apiStore.addApplication(app1Name, APIThrottlingTier.UNLIMITED.getState(), "", "this-is-test");
        checkError(response.getData(), "Error while adding Application " + app1Name);
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, providerName);
        subscriptionRequest.setApplicationName(app1Name);
        response = apiStore.subscribe(subscriptionRequest);
        checkError(response.getData(), "Error while subscribing fro API " + apiName);


        //Here will do 11 faulty invocations

        String apiContextFaultyAPI = "UsageTestAPIFaultyAPI";
        String tagsFaultyAPI = "youtube, video, media";
        //this url should not exists and then it will return with API fault invocation
        String urlFaultyAPI = "http://thisiswrong.com/feeds/api/standardfeeds";
        String descriptionFaultyAPI = "This is test API create by API manager usage integration test";

        app2Name = "Statistics-Application-" + new Random().nextInt(10000);


        APIRequest apiRequestFaultyAPI = new APIRequest(apiNameFaultyAPI, apiContextFaultyAPI
                , new URL(urlFaultyAPI));
        apiRequestFaultyAPI.setTags(tagsFaultyAPI);
        apiRequestFaultyAPI.setDescription(descriptionFaultyAPI);
        apiRequestFaultyAPI.setVersion(apiVersion);
        response = apiPublisher.addAPI(apiRequestFaultyAPI);
        checkError(response.getData(), "Error while adding API " + apiNameFaultyAPI);

        APILifeCycleStateRequest updateRequestFaultyAPI = new APILifeCycleStateRequest(apiNameFaultyAPI,
                                                                                       providerName,
                                                                                       APILifeCycleState.PUBLISHED);
        response = apiPublisher.changeAPILifeCycleStatus(updateRequestFaultyAPI);
        checkError(response.getData(), "Error while publishing API " + apiNameFaultyAPI);
        SubscriptionRequest subscriptionRequestFaultyAPI = new SubscriptionRequest(apiNameFaultyAPI,
                                                                                   providerName);
        subscriptionRequestFaultyAPI.setApplicationName(app2Name);
        response = apiStore.addApplication(app2Name, APIThrottlingTier.UNLIMITED.getState(), "", "this-is-test");
        checkError(response.getData(), "Error while adding application " + app2Name);
        response = apiStore.subscribe(subscriptionRequestFaultyAPI);
        checkError(response.getData(), "Error while subscribing to API " + apiNameFaultyAPI);

        //host object tests
        String fileName = "testUsageWithBAM.jag";
        String sourcePath = computeJaggeryResourcePath(fileName);
        String destinationPath = computeDestinationPath(fileName);
        copySampleFile(sourcePath, destinationPath);

        //getting api counts before invocation
        String finalOutputUsageTest;
        finalOutputUsageTest = HttpRequestUtil.doGet(getTestApplicationUsagePublisherServerURLHttp()
                , new HashMap<String, String>()).getData();
        assert finalOutputUsageTest != null;

        String[] array = finalOutputUsageTest.split("==");

        if(array.length > 3) {
            JSONArray apiCountJsonArray = new JSONArray(array[2]);
            if(apiCountJsonArray.length() > 0 && !apiCountJsonArray.isNull(0)) {
                apiCountByUserBefore = apiCountJsonArray.getJSONObject(0).getInt("count");
            }

            JSONArray faultCountJsonArray = new JSONArray(array[0]);
            if(faultCountJsonArray.length() > 0 && !faultCountJsonArray.isNull(0)) {
                faultCountBefore = faultCountJsonArray.getJSONObject(0).getInt("count");
            }
        }


        //invoking the API UsageTestAPI
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(app1Name);
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject jsonResponse = new JSONObject(responseString);
        String accessToken = jsonResponse.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        HttpResponse apiResponse;
        //Here will do 10 successful invocations
        for (int i = 0; i < successRequestCount; i++) {
            apiResponse = HttpRequestUtil.doGet(getAPIInvocationURLHttp("UsageTestAPI", apiVersion)
                                                    + "?format=json&action=query&titles=MainPage" +
                                                    "&prop=revisions&rvprop=content",
                                                    requestHeaders);
            assertEquals(apiResponse.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        }

        //invoking faulty API UsageTestAPIFaultyAPI
        APPKeyRequestGenerator generateAppKeyRequestFaultyAPI = new APPKeyRequestGenerator(app2Name);
        String responseStringFaultyAPI = apiStore.generateApplicationKey(generateAppKeyRequestFaultyAPI).getData();
        JSONObject responseFaultyAPI = new JSONObject(responseStringFaultyAPI);
        String accessTokenFaultyAPI = responseFaultyAPI.getJSONObject("data").getJSONObject("key").
                get("accessToken").toString();
        Map<String, String> requestHeadersFaultyAPI = new HashMap<String, String>();
        requestHeadersFaultyAPI.put("Authorization", "Bearer " + accessTokenFaultyAPI);
        HttpResponse apiResponseFaultyAPI;
        for (int i = 0; i < faultRequestCount; i++) {
            apiResponseFaultyAPI = HttpRequestUtil.doGet(getAPIInvocationURLHttp("UsageTestAPIFaultyAPI", apiVersion)
                                                             + "?format=json&action=query&titles=MainPage" +
                                                             "&prop=revisions&rvprop=content"
                    , requestHeadersFaultyAPI);
            assertEquals(apiResponseFaultyAPI.getResponseCode(), HttpStatus.SC_INTERNAL_SERVER_ERROR
                    , "Response code mismatched");
        }



        //sleep toolbox to run and populate the result to database
        log.info("waiting for hive job to run and populate statics data");
        Thread.sleep(240000);

        finalOutputUsageTest = HttpRequestUtil.doGet(getTestApplicationUsagePublisherServerURLHttp()
                , new HashMap<String, String>()).getData();
        assertNotNull(finalOutputUsageTest, "No response from " + getTestApplicationUsagePublisherServerURLHttp());

        arrayStat = finalOutputUsageTest.split("==");

        log.info(finalOutputUsageTest + "\n");
    }


    @Test(groups = {"wso2.am"}, description = "API Usage statistics by APIResponseFaultCount"
            , dependsOnMethods = {"testGenerateStatistics"})
    public void testAPIResponseFaultCount() throws Exception {
        JSONObject element;
        log.info("Verifying getAPIResponseFaultCount in publisher");
        assertNotNull(arrayStat[0], "API Fault Usage for Subscriber from API publisher host object not found" +
                                " (getAPIResponseFaultCount)");
        element = new JSONArray(arrayStat[0]).getJSONObject(0);
        log.info(element.toString() + "\n");
        assertEquals((element.getInt("count") - faultCountBefore) ,faultRequestCount
                , "API Fault Requests count mismatched");


    }

    @Test(groups = {"wso2.am"}, description = "API Usage statistics by APIUsageByResourcePath"
            , dependsOnMethods = {"testGenerateStatistics"})
    public void testAPIUsageByResourcePath() throws Exception {
        JSONObject element;
        log.info("Verifying getAPIUsageByResourcePath in publisher");
        assertNotNull(arrayStat[1], "API Usage by ResourcePath from API publisher host object " +
                                "(getAPIUsageByResourcePath)");
        element = new JSONArray(arrayStat[1]).getJSONObject(0);
        log.info(element.toString() + "\n");
        assertTrue(element.getInt("count") > 0, "No API Usage count by Resource Path");


    }

    @Test(groups = {"wso2.am"}, description = "API Usage statistics by APIUsageByUser"
            , dependsOnMethods = {"testGenerateStatistics"})
    public void testAPIUsageByUser() throws Exception {
        JSONObject element;
        log.info("Verifying getAPIUsageByResourcePath in publisher");
        assertNotNull(arrayStat[2], "No API Usage By User Response from API publisher host object " +
                                "(getAPIUsageByUser)");
        element = new JSONArray(arrayStat[2]).getJSONObject(0);
        log.info(element.toString() + "\n");
        assertEquals((element.getInt("count") - apiCountByUserBefore) , successRequestCount
                , "API Usage count by User mismatched");

    }

    @Test(groups = {"wso2.am"}, description = "API Usage statistics by AllAPIUsageByProvider"
            , dependsOnMethods = {"testGenerateStatistics"})
    public void testAllAPIUsageByProvider() throws Exception {
        JSONObject element;
        log.info("Verifying getAllAPIUsageByProvider in publisher");
        assertNotNull(arrayStat[3], "No API Usage by Provider from API publisher host object " +
                                "(getAllAPIUsageByProvider)");
        element = new JSONArray(arrayStat[3]).getJSONObject(0);
        log.info(element.toString() + "\n");
        assertNotNull(element.get("userName"), "userName not found in API Usage Response by Provider");

    }

    @Test(groups = {"wso2.am"}, description = "API Usage statistics by FirstAccessTime"
            , dependsOnMethods = {"testGenerateStatistics"})
    public void testFirstAccessTime() throws Exception {
        JSONObject element;
        log.info("Verifying getFirstAccessTime in publisher");
        assertNotNull(arrayStat[4], "No First Access Time Response from API publisher host object " +
                                    "(getFirstAccessTime)");
        element = new JSONArray(arrayStat[4]).getJSONObject(0);
        log.info(element.toString() + "\n");
        assertTrue(element.getInt("year") > 0, "No API Usage count by Provider");

    }

    @Test(groups = {"wso2.am"}, description = "API Usage statistics by ProviderAPIServiceTime"
            , dependsOnMethods = {"testGenerateStatistics"})
    public void testProviderAPIServiceTime() throws Exception {
        JSONObject element;
        log.info("Verifying getProviderAPIServiceTime in publisher");
        assertNotNull(arrayStat[5], "No Service Time Response from API publisher host object " +
                                "(getProviderAPIServiceTime)");
        element = new JSONArray(arrayStat[5]).getJSONObject(0);
        log.info(element.toString()+ "\n");
        assertTrue(element.getDouble("serviceTime") >= 0, "Service Time Not Found in getProviderAPIServiceTime response");
    }

    @Test(groups = {"wso2.am"}, description = "API Usage statistics by ProviderAPIUsage"
            , dependsOnMethods = {"testGenerateStatistics"})
    public void testProviderAPIUsage() throws Exception {
        JSONObject element;
        log.info("Verifying getProviderAPIUsage in publisher");
        assertNotNull(arrayStat[6], "No API Usage By provider Response from API publisher host object " +
                                "(getProviderAPIUsage)");
        element = new JSONArray(arrayStat[6]).getJSONObject(0);
        log.info(element.toString() + "\n");
        assertTrue(element.getInt("count") > 0, "No API Usage count by Provider");
    }

    @Test(groups = {"wso2.am"}, description = "API Usage statistics by ProviderAPIVersionUsage"
            , dependsOnMethods = {"testGenerateStatistics"})
    public void testProviderAPIVersionUsage() throws Exception {
        JSONObject element;
        log.info("Verifying getProviderAPIVersionUsage in publisher");
        assertNotNull(arrayStat[7], "No API Version Usage By provider Response from API publisher host object " +
                                "(getProviderAPIVersionUsage)");
        element = new JSONArray(arrayStat[7]).getJSONObject(0);
        log.info(element.toString() + "\n");
        assertTrue(element.getInt("count") > 0, "No API Version Usage count by Provider");
    }

    @Test(groups = {"wso2.am"}, description = "API Usage statistics by ProviderAPIVersionUserUsage"
            , dependsOnMethods = {"testGenerateStatistics"})
    public void testProviderAPIVersionUserUsage() throws Exception {
        JSONObject element;
        log.info("Verifying getProviderAPIVersionUserUsage in publisher");
        assertNotNull(arrayStat[8], "No API Version Usage By user Response from API publisher host object " +
                                "(getProviderAPIVersionUserUsage)");
        element = new JSONArray(arrayStat[8]).getJSONObject(0);
        log.info(element.toString() + "\n");
        assertTrue(element.getInt("count") > 0, "No API Version Usage count by user");
    }

    private String getTestApplicationUsagePublisherServerURLHttp() {
        return storeUrls.getWebAppURLHttp() + "testapp/testUsageWithBAM.jag";
    }


    private void copySampleFile(String sourcePath, String destinationPath) {
        File sourceFile = new File(sourcePath);
        File destinationFile = new File(destinationPath);
        try {
            FileManipulator.copyFile(sourceFile, destinationFile);
        } catch (IOException e) {
            log.error("Error while copying the other into Jaggery server", e);
        }
    }


    private String computeDestinationPath(String fileName)
            throws XPathExpressionException, RemoteException, ServerAdminException {

        String deploymentPath = getServerDeploymentDir() + "jaggeryapps" + File.separator + "testapp";
        File depFile = new File(deploymentPath);
        if (!depFile.exists() && !depFile.mkdir()) {
            log.error("Error while creating the deployment folder : "
                      + deploymentPath);
        }

        return deploymentPath + File.separator + fileName;
    }

    private String getServerDeploymentDir()
            throws XPathExpressionException, RemoteException, ServerAdminException {
        ServerAdminStub serverAdminStub;
        final String serviceName = "ServerAdmin";
        String endPoint = publisherContext.getContextUrls().getBackEndUrl() + serviceName;
        serverAdminStub = new ServerAdminStub(endPoint);
        AuthenticateStubUtil.authenticateStub(user.getUserName(), user.getPassword(), serverAdminStub);
        ServerData serverData = serverAdminStub.getServerData();
        return serverData.getRepoLocation().substring(serverData.getRepoLocation().indexOf(":") + 1);

    }


    private String computeJaggeryResourcePath(String fileName) {

        return getAMResourceLocation()
               + File.separator + "jaggery" + File.separator + fileName;
    }

    private void checkError(String jsonString, String message) throws JSONException {
        log.info("Response: " + jsonString);
        JSONObject jsonObject = new JSONObject(jsonString);
        assertFalse(jsonObject.getBoolean(APIMIntegrationConstants.API_RESPONSE_ELEMENT_NAME_ERROR), message);

    }

}
