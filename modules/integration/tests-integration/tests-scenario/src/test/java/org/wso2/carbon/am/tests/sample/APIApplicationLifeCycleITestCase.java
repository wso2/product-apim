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

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class APIApplicationLifeCycleITestCase extends APIManagerIntegrationTest {
	private APIPublisherRestClient apiPublisher;
	private APIStoreRestClient apiStore;

	//move to base class
	private String publisherURLHttp;
	private String storeURLHttp;

	@BeforeClass(alwaysRun = true)
	public void init() throws Exception {
		super.init();
		if (isBuilderEnabled()) {
			publisherURLHttp = getServerURLHttp();
			storeURLHttp = getServerURLHttp();

		} else {
			publisherURLHttp = getPublisherServerURLHttp();
			storeURLHttp = getStoreServerURLHttp();
		}
		apiPublisher = new APIPublisherRestClient(publisherURLHttp);
		apiStore = new APIStoreRestClient(storeURLHttp);
	}

	@Test(groups = { "wso2.am" }, description = "API Life cycle test case")
	public void testAPIApplicationLifeCycleITestCase() throws Exception {
		String APIName = "APILifeCycleTestAPI";
		String APIContext = "testAPI";
		String tags = "youtube, video, media";
		String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
		String description = "This is test API create by API manager integration test";
		String providerName = "admin";
		String APIVersion = "1.0.0";
		//add all option methods
		apiPublisher.login(context.getContextTenant().getContextUser().getUserName(),
		                   context.getContextTenant().getContextUser().getPassword());
		APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
		apiRequest.setTags(tags);
		apiRequest.setDescription(description);
		apiRequest.setVersion(APIVersion);
		apiRequest.setWsdl(
				"https://svn.wso2.org/repos/wso2/carbon/platform/trunk/products/bps/modules/samples/product/src/main/resources/bpel/2.0/MyRoleMexTestProcess/echo.wsdl");
		apiRequest.setVisibility("restricted");
		apiRequest.setRoles("admin");
		apiPublisher.addAPI(apiRequest);
		apiPublisher.deleteApi(APIName, APIVersion, providerName);
		//add assertion
		apiPublisher.addAPI(apiRequest);
		APIBean apiBean = APIMgtTestUtil
				.getAPIBeanFromHttpResponse(apiPublisher.getApi(APIName, providerName));
		APILifeCycleStateRequest updateRequest =
				new APILifeCycleStateRequest(APIName, providerName, APILifeCycleState.PUBLISHED);
		apiPublisher.changeAPILifeCycleStatusTo(updateRequest);
		//Test API properties
		Assert.assertEquals(apiBean.getId().getApiName(), APIName, "API Name mismatch");
		Assert.assertEquals(
				apiBean.getContext().trim().substring(apiBean.getContext().indexOf("/") + 1),
				APIContext, "API context mismatch");
		Assert.assertEquals(apiBean.getId().getVersion(), APIVersion, "API version mismatch");
		Assert.assertEquals(apiBean.getId().getProviderName(), providerName,
		                    "Provider Name mismatch");
		for (String tag : apiBean.getTags()) {
			Assert.assertTrue(tags.contains(tag), "API tag data mismatched");
		}
		Assert.assertEquals(apiBean.getDescription(), description, "API description mismatch");
		apiStore.login(context.getContextTenant().getContextUser().getUserName(),
		               context.getContextTenant().getContextUser().getPassword());
		apiStore.addApplication("APILifeCycleTestAPI-application", "Gold", "", "this-is-test");
		SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName,
		                                                                  context.getContextTenant()
		                                                                         .getContextUser()
		                                                                         .getUserName()
		);
		subscriptionRequest.setApplicationName("APILifeCycleTestAPI-application");
		apiStore.subscribe(subscriptionRequest);

		GenerateAppKeyRequest generateAppKeyRequest =
				new GenerateAppKeyRequest("APILifeCycleTestAPI-application");
		String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
		JSONObject response = new JSONObject(responseString);
		String accessToken =
				response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.put("Authorization", "Bearer " + accessToken);
		//Here add API tags and check same have in response.
		//Here check same tags are there
		//Add some comment to API
		//check comment is there
		//Add rating
		//check rating
		Thread.sleep(60000);
		//  for (int i = 0; i < 19; i++) {

		HttpResponse youTubeResponse = HttpRequestUtil
				.doGet(getApiInvocationURLHttp("testAPI/1.0.0/most_popular"), requestHeaders);
		Assert.assertEquals(youTubeResponse.getResponseCode(), 200, "Response code mismatched");
		Assert.assertTrue(youTubeResponse.getData().contains("<feed"), "Response data mismatched");
		Assert.assertTrue(youTubeResponse.getData().contains("<category"),
		                  "Response data mismatched");
		Assert.assertTrue(youTubeResponse.getData().contains("<entry>"),
		                  "Response data mismatched");

		// }
		//Do get,post,put,delete all here
		//HttpResponse youTubeResponse = HttpRequestUtil.doGet(getApiInvocationURLHttp("commentRating/1.0.0/most_popular"), requestHeaders);
		//Assert.assertEquals(youTubeResponse.getResponseCode(), 503, "Response code mismatched");
		//Thread.sleep(60000);
		HttpResponse youTubeResponse1 =
				HttpRequestUtil.doGet(getApiInvocationURLHttp("testAPI/1.0.0/most_popular"), null);
		Assert.assertEquals(youTubeResponse1.getResponseCode(), 401, "Response code mismatched");
		requestHeaders.clear();
		requestHeaders.put("Authorization", "Bearer " + "-wrong-tokent-text-");
		HttpResponse youTubeResponseError =
				HttpRequestUtil.doGet(getApiInvocationURLHttp("testAPI/1.0.0/most_popular"), null);
		Assert.assertEquals(youTubeResponseError.getResponseCode(), 401,
		                    "Response code mismatched");

		apiStore.getAllPublishedAPIs();
		apiStore.getAllApplications();
		apiStore.getPublishedAPIsByApplication("APILifeCycleTestAPI-application");
		apiStore.isRatingActivated();
		apiStore.addRatingToAPI(APIName, APIVersion, providerName, "4");
		apiStore.addRatingToAPI(APIName, APIVersion, providerName, "2");
		apiStore.addRatingToAPI(APIName, APIVersion, providerName, "1");
		apiStore.removeRatingFromAPI(APIName, APIVersion, providerName);
		apiStore.getAllDocumentationOfApi(APIName, APIVersion, providerName);
		//apiStore.getAllPaginatedPublishedAPIs("carbon.super","0","10");
		//Negative cases
		apiStore.getPublishedAPIsByApplication("APILifeCycleTestAPI-application-wrong");
		apiStore.isRatingActivated();
		apiStore.addRatingToAPI("NoAPI", APIVersion, providerName, "4");
		apiStore.removeRatingFromAPI("NoAPI", APIVersion, providerName);
		apiStore.getAllDocumentationOfApi("NoAPI", APIVersion, providerName);
		apiRequest.setTags("updated");
		apiRequest.setProvider("admin");
		Thread.sleep(1000);
		apiPublisher.updateAPI(apiRequest);
		//TODO need to reformat this code after we finalize new APIs
		apiPublisher.addDocument(APIName, APIVersion, providerName, "Doc Name", "How To", "In-line",
		                         "url-no-need", "summary", "");
		apiPublisher.addDocument(APIName, APIVersion, providerName, "Doc Name", "How To", "URL",
		                         "http://www.businesstoday.lk/article.php?article=3549", "summary",
		                         "");
		apiPublisher.addDocument(APIName, APIVersion, providerName, "Doc Name", "How To", " File",
		                         "url-no-need", "summary", getAMResourceLocation()
		                                                   + File.separator +
		                                                   "configFiles/tokenTest/" +
		                                                   "api-manager.xml"
		);
		apiPublisher.removeDocumentation(APIName, APIVersion, providerName, "Doc Name", "How To");
		//create application
		apiStore.addApplication("test-application", "Gold", "", "this-is-test");
		apiStore.getAllApplications();

		//Test case to create new application and make subscriptions to that application
		SubscriptionRequest subscriptionRequest1 = new SubscriptionRequest(APIName,
		                                                                   context.getContextTenant()
		                                                                          .getContextUser()
		                                                                          .getUserName()
		);
		subscriptionRequest1.setApplicationName("test-application");
		apiStore.subscribe(subscriptionRequest1);
		GenerateAppKeyRequest generateAppKeyRequest1 =
				new GenerateAppKeyRequest("test-application");
		String responseString1 = apiStore.generateApplicationKey(generateAppKeyRequest1).getData();
		JSONObject response1 = new JSONObject(responseString1);
		String accessToken1 =
				response1.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
		Map<String, String> requestHeaders1 = new HashMap<String, String>();
		requestHeaders1.put("Authorization", "Bearer " + accessToken1);
		HttpResponse youTubeResponseTestApp = HttpRequestUtil
				.doGet(getApiInvocationURLHttp("testAPI/1.0.0/most_popular"), requestHeaders1);
		for (int i = 0; i < 40; i++) {
			youTubeResponseTestApp = HttpRequestUtil
					.doGet(getApiInvocationURLHttp("testAPI/1.0.0/most_popular"), requestHeaders1);
		}
		Assert.assertEquals(youTubeResponseTestApp.getResponseCode(), 503,
		                    "Response code mismatched");

		//Add comment
		apiStore.isCommentActivated();
		apiStore.addComment(APIName, APIVersion, providerName, "this-is-comment");
		apiStore.getRecentlyAddedAPIs("carbon.super", "5");

		apiStore.updateApplication("test-application", "test-updated-application", "test-url",
		                           "this-is-updated", "bronze");
		apiStore.getAllApplications();
		apiStore.removeApplication("test-updated-application");
		apiStore.getAllSubscriptions();
		apiStore.getAllTags();

		//Update role permissions
		HttpResponse updateTierPermissionResponse =
				apiPublisher.updatePermissions("Gold", "Deny", "admin");
		JSONObject jsonObject = new JSONObject(updateTierPermissionResponse.getData());
		Assert.assertTrue(!(Boolean) jsonObject.get("error"),
		                  "Error while updating tier permission");

	}

	@AfterClass(alwaysRun = true)
	public void destroy() throws Exception {
		apiStore.removeApplication("APILifeCycleTestAPI-application");
		super.cleanup();
	}
}
