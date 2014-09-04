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
import org.wso2.am.integration.test.utils.APIManagerIntegrationTest;
import org.wso2.am.integration.test.utils.APIMgtTestUtil;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
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

import static org.testng.Assert.assertNotNull;

/*
This test case is not run with default API manager integration tests. To run this test we assume that BAM server
is  running with port offset 1. If you have changed anything please update api-manager.xml file and master data source
properties file available in  /resources/artifacts/AM/configFiles/usagemonitortest directory.For this test case we will
run API manager as a part of integration test and BAM server should up and run by user. In this test we will create API
and do basic API management related operations and usage data will push to BAM. User should deploy usage tool box inside
BAM server(ideally user should setup BAM as we described in statistics help document). And all API manager related
configurations listed on statistics help doc should apply to files available in above mentioned directory
(/resources/artifacts/AM/configFiles/usagemonitortest)
 */
public class APIUsageBAMIntegrationTestCase extends APIManagerIntegrationTest {
	private APIPublisherRestClient apiPublisher;
	private APIStoreRestClient apiStore;
	private ServerConfigurationManager serverConfigurationManager;
	private String publisherURLHttp;
	private String storeURLHttp;

	@BeforeClass(alwaysRun = true)
	public void init() throws Exception {
		super.init();

		if (isBuilderEnabled()) {
			publisherURLHttp = getServerURLHttp();
			storeURLHttp = getServerURLHttp();
			//If test run in external distributed deployment you need to copy following resources accordingly.
			//configFiles/usagemonitortest/api-manager.xml
			//configFiles/usagemonitortest/master-datasources.xml
			//configFiles/tokenTest/log4j.properties
			//Also need to copy the content of /resources/artifacts/AM/jaggery to servers following folder folder
			///repository/deployment/server/jaggeryapps/testapp
			serverConfigurationManager = new ServerConfigurationManager(context);
			serverConfigurationManager.applyConfiguration(new File(getAMResourceLocation()
			                                                       + File.separator +
			                                                       "configFiles/usagemonitortest/" +
			                                                       "api-manager.xml"));
			String dataSourceDestinationPath =
					computeDestinationPathForDataSource("master-datasources.xml");
			String dataSourceSourcePath = computeDataSourceResourcePath("master-datasources.xml");
			copyDataSourceFile(dataSourceSourcePath, dataSourceDestinationPath);
			serverConfigurationManager.applyConfiguration(new File(getAMResourceLocation()
			                                                       + File.separator +
			                                                       "configFiles/tokenTest/" +
			                                                       "log4j.properties"));
			super.init();

		} else {
			publisherURLHttp = getPublisherServerURLHttp();
			storeURLHttp = getStoreServerURLHttp();
		}
		apiPublisher = new APIPublisherRestClient(publisherURLHttp);
		apiStore = new APIStoreRestClient(storeURLHttp);
	}

	private void copySampleFile(String sourcePath, String destinationPath) {
		File sourceFile = new File(sourcePath);
		File destinationFile = new File(destinationPath);
		try {
			FileManipulator.copyFile(sourceFile, destinationFile);
		} catch (IOException e) {
			log.error("Error while copying the sample into Jaggery server", e);
		}
	}

	private void copyDataSourceFile(String sourcePath, String destPath) {
		File sourceFile = new File(sourcePath);
		File destFile = new File(destPath);
		try {
			FileManipulator.copyFile(sourceFile, destFile);
		} catch (IOException e) {
			log.error("Error while copying the sample into Jaggery server", e);
		}
	}

	private String computeDestinationPath(String fileName) {
		String serverRoot = System.getProperty(ServerConstants.CARBON_HOME);
		String deploymentPath = serverRoot + "/repository/deployment/server/jaggeryapps/testapp";
		File depFile = new File(deploymentPath);
		if (!depFile.exists() && !depFile.mkdir()) {
			log.error("Error while creating the deployment folder : "
			          + deploymentPath);
		}
		return deploymentPath + File.separator + fileName;
	}

	private String computeDestinationPathForDataSource(String fileName) {
		String serverRoot = System.getProperty(ServerConstants.CARBON_HOME);
		String deploymentPath = serverRoot + "/repository/conf/datasources";
		File depFile = new File(deploymentPath);
		if (!depFile.exists() && !depFile.mkdir()) {
			log.error("Error while creating the deployment folder : "
			          + deploymentPath);
		}
		return deploymentPath + File.separator + fileName;
	}

	private String computeJaggeryResourcePath(String fileName) {

		String sourcePath = getAMResourceLocation()
		                    + File.separator + "jaggery/" + fileName;
		return sourcePath;
	}

	private String computeDataSourceResourcePath(String fileName) {

		String sourcePath = getAMResourceLocation()
		                    + File.separator + "configFiles/usagemonitortest/" + fileName;
		return sourcePath;
	}

	@Test(groups = { "wso2.am" }, description = "API Life cycle test case")
	public void usageDataTestCase() throws Exception {
		String APIName = "UsageTestAPI";
		String APIContext = "UsageTestAPI";
		String tags = "youtube, video, media";
		String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
		String description = "This is test API create by API manager usage integration test";
		String providerName = "admin";
		String APIVersion = "1.0.0";
		apiPublisher.login(context.getContextTenant().getContextUser().getUserName(),
		                   context.getContextTenant().getContextUser().getPassword());
		APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
		apiRequest.setTags(tags);
		apiRequest.setDescription(description);
		apiRequest.setVersion(APIVersion);
		apiPublisher.addAPI(apiRequest);
		apiPublisher.deleteApi(APIName, APIVersion, providerName);
		apiPublisher.addAPI(apiRequest);
		APIBean apiBean = APIMgtTestUtil
				.getAPIBeanFromHttpResponse(apiPublisher.getApi(APIName, providerName));
		APILifeCycleStateRequest updateRequest =
				new APILifeCycleStateRequest(APIName, providerName, APILifeCycleState.PUBLISHED);
		apiPublisher.changeAPILifeCycleStatusTo(updateRequest);
		apiStore.login(context.getContextTenant().getContextUser().getUserName(),
		               context.getContextTenant().getContextUser().getPassword());
		SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName,
		                                                                  context.getContextTenant()
		                                                                         .getContextUser()
		                                                                         .getUserName());
		apiStore.subscribe(subscriptionRequest);
		apiPublisher.addDocument(APIName, APIVersion, providerName, "Doc-Name", "How To", "In-line",
		                         "url-no-need", "summary", "");
		GenerateAppKeyRequest generateAppKeyRequest =
				new GenerateAppKeyRequest("DefaultApplication");
		String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
		JSONObject response = new JSONObject(responseString);
		String accessToken =
				response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.put("Authorization", "Bearer " + accessToken);
		HttpResponse youTubeResponse = HttpRequestUtil
				.doGet(getApiInvocationURLHttp("UsageTestAPI/1.0.0/most_popular"), requestHeaders);
		Assert.assertEquals(youTubeResponse.getResponseCode(), 200, "Response code mismatched");
		//Here will do 20 successful invocations
		for (int i = 0; i < 20; i++) {
			youTubeResponse = HttpRequestUtil
					.doGet(getApiInvocationURLHttp("UsageTestAPI/1.0.0/most_popular"),
					       requestHeaders);
		}
		Thread.sleep(60000);
		//Here will do 20 faulty invocations
		String APINameFaultyAPI = "UsageTestAPIFaultyAPI";
		String APIContextFaultyAPI = "UsageTestAPIFaultyAPI";
		String tagsFaultyAPI = "youtube, video, media";
		//this url should not exists and then it will return with API fault invocation
		String urlFaultyAPI = "http://thisiswrong.com/feeds/api/standardfeeds";
		String descriptionFaultyAPI =
				"This is test API create by API manager usage integration test";
		String APIVersionFaultyAPI = "1.0.0";
		APIRequest apiRequestFaultyAPI =
				new APIRequest(APINameFaultyAPI, APIContextFaultyAPI, new URL(urlFaultyAPI));
		apiRequestFaultyAPI.setTags(tagsFaultyAPI);
		apiRequestFaultyAPI.setDescription(descriptionFaultyAPI);
		apiRequestFaultyAPI.setVersion(APIVersionFaultyAPI);
		apiPublisher.addAPI(apiRequestFaultyAPI);
		APIBean apiBeanFaultyAPI = APIMgtTestUtil
				.getAPIBeanFromHttpResponse(apiPublisher.getApi(APINameFaultyAPI, providerName));
		APILifeCycleStateRequest updateRequestFaultyAPI =
				new APILifeCycleStateRequest(APINameFaultyAPI, providerName,
				                             APILifeCycleState.PUBLISHED);
		apiPublisher.changeAPILifeCycleStatusTo(updateRequestFaultyAPI);
		SubscriptionRequest subscriptionRequestFaultyAPI = new SubscriptionRequest(APINameFaultyAPI,
		                                                                           context.getContextTenant()
		                                                                                  .getContextUser()
		                                                                                  .getUserName());
		apiStore.subscribe(subscriptionRequestFaultyAPI);
		apiPublisher.addDocument(APINameFaultyAPI, APIVersionFaultyAPI, providerName, "Doc-Name",
		                         "How To", "In-line", "url-no-need", "summary", "");
		GenerateAppKeyRequest generateAppKeyRequestFaultyAPI =
				new GenerateAppKeyRequest("DefaultApplication");
		String responseStringFaultyAPI =
				apiStore.generateApplicationKey(generateAppKeyRequestFaultyAPI).getData();
		JSONObject responseFaultyAPI = new JSONObject(responseStringFaultyAPI);
		String accessTokenFaultyAPI =
				responseFaultyAPI.getJSONObject("data").getJSONObject("key").get("accessToken")
				                 .toString();
		Map<String, String> requestHeadersFaultyAPI = new HashMap<String, String>();
		requestHeadersFaultyAPI.put("Authorization", "Bearer " + accessTokenFaultyAPI);
		HttpResponse youTubeResponseFaultyAPI = HttpRequestUtil
				.doGet(getApiInvocationURLHttp("UsageTestAPIFaultyAPI/1.0.0/most_popular"),
				       requestHeadersFaultyAPI);
		Assert.assertEquals(youTubeResponseFaultyAPI.getResponseCode(), 500,
		                    "Respofnse code mismatched");
		for (int i = 0; i < 20; i++) {
			youTubeResponseFaultyAPI = HttpRequestUtil
					.doGet(getApiInvocationURLHttp("UsageTestAPIFaultyAPI/1.0.0/incorrect"),
					       requestHeaders);
		}

		//host object tests
		String fileName = "testUsageWithBAM.jag";
		String sourcePath = computeJaggeryResourcePath(fileName);
		String destinationPath = computeDestinationPath(fileName);
		copySampleFile(sourcePath, destinationPath);

		Thread.sleep(240000);
		String finalOutputUsageTest = null;
		//ClientConnectionUtil.waitForPort(9763, "");
		try {
			URL jaggeryURL = new URL(getTestApplicationUsagePublisherServerURLHttp());
			URLConnection jaggeryServerConnection = jaggeryURL.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(
					jaggeryServerConnection.getInputStream()));

			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				finalOutputUsageTest = inputLine;
			}
			String[] arr = finalOutputUsageTest.split("==");
			System.out.println(finalOutputUsageTest);
			validateUsageResponseArray(arr);
			in.close();
		} catch (IOException e) {
			log.error("Error while invoking test application to test publisher host object");
		} finally {
			assertNotNull(finalOutputUsageTest, "Result cannot be null");
		}
	}

	@AfterClass(alwaysRun = true)
	public void destroy() throws Exception {
		super.cleanup();
	}

	//take exact count
	public static boolean validateUsageResponseArray(String[] array) {
		//This order is based on the operations of jaggery file operation order
		//If we edit jaggary file we need to modify this as well
		Assert.assertNotNull(array[1],
		                     "Error while getting API Usage for Subscriber from API publisher host object (getAPIUsageforSubscriber)");
		Assert.assertNotNull(array[2],
		                     "Error while getting API Faulty Analyze By Time from API publisher host object (getAPIFaultyAnalyzeByTime)");
		Assert.assertNotNull(array[3],
		                     "Error while getting API Response Fault Count from API publisher host object (getAPIResponseFaultCount)");
		Assert.assertNotNull(array[4],
		                     "Error while getting external api stores from API publisher host object (getAPIUsageByResourcePath)");
		Assert.assertNotNull(array[5],
		                     "Error while getting external api stores from API publisher host object (getAPIUsageByUser)");
		Assert.assertNotNull(array[6],
		                     "Error while getting external api stores from API publisher host object (getAllAPIUsageByProvider)");
		Assert.assertNotNull(array[7],
		                     "Error while getting external api stores from API publisher host object (getFirstAccessTime)");
		Assert.assertNotNull(array[8],
		                     "Error while getting external api stores from API publisher host object (getProviderAPIServiceTime)");
		Assert.assertNotNull(array[9],
		                     "Error while getting external api stores from API publisher host object (getProviderAPIUsage)");
		Assert.assertNotNull(array[10],
		                     "Error while getting external api stores from API publisher host object (getProviderAPIVersionUsage)");
		Assert.assertNotNull(array[11],
		                     "Error while getting external api stores from API publisher host object (getProviderAPIVersionUserUsage)");
		Assert.assertNotNull(array[12],
		                     "Error while getting external api stores from API publisher host object (getProviderAPIVersionUserLastAccess)");
		return true;
	}
}
