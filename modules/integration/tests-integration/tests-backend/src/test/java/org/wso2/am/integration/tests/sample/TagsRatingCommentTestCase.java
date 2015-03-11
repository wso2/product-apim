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

package org.wso2.am.integration.tests.sample;

import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIMgtTestUtil;
import org.wso2.am.integration.test.utils.base.AMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TagsRatingCommentTestCase extends AMIntegrationBaseTest {

	private APIPublisherRestClient apiPublisher;
	private APIStoreRestClient apiStore;

	@BeforeClass(alwaysRun = true)
	public void init() throws Exception {
		super.init();
	    /*
        This test can point to external API manager deployment without adding any resources to system
         */
        String publisherURLHttp;
        String storeURLHttp;
        if (isBuilderEnabled()) {
			publisherURLHttp = getServerURLHttp();
			storeURLHttp = getServerURLHttp();
		} else {
			publisherURLHttp = getPublisherServerURLHttp();
			storeURLHttp = getStoreServerURLHttp();
		}
		apiPublisher = new APIPublisherRestClient(publisherURLHttp);
		apiStore = new APIStoreRestClient(storeURLHttp);

		apiPublisher.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
		apiStore.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
	}

	@Test(groups = { "wso2.am" }, description = "Comment Rating Test case")
	public void testTagsRatingCommentTestCase() throws Exception {
		String APIName = "CommentRatingAPI";
		String APIContext = "commentRating";
		String tags = "youtube, video, media";
		String url = "http://gdata.youtube.com/feeds/api/standardfeeds";
		String description = "This is test API create by API manager integration test";
		String providerName = apimContext.getContextTenant().getContextUser().getUserName();
		String APIVersion = "1.0.0";

		APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
		apiRequest.setTags(tags);
		apiRequest.setDescription(description);
		apiRequest.setVersion(APIVersion);
		apiPublisher.addAPI(apiRequest);
		APIBean apiBean = APIMgtTestUtil
				.getAPIBeanFromHttpResponse(apiPublisher.getApi(APIName, providerName));
		APILifeCycleStateRequest updateRequest =
				new APILifeCycleStateRequest(APIName, providerName, APILifeCycleState.PUBLISHED);
		apiPublisher.changeAPILifeCycleStatusTo(updateRequest);
		//Test API properties
		assertEquals(apiBean.getId().getApiName(), APIName, "API Name mismatch");
		assertEquals(
                apiBean.getContext().trim().substring(apiBean.getContext().indexOf("/") + 1),
                APIContext, "API context mismatch");
		assertEquals(apiBean.getId().getVersion(), APIVersion, "API version mismatch");
		assertEquals(apiBean.getId().getProviderName(), providerName,
                "Provider Name mismatch");
		for (String tag : apiBean.getTags()) {
			assertTrue(tags.contains(tag), "API tag data mismatched");
		}
		assertEquals(apiBean.getDescription(), description, "API description mismatch");

		apiStore.addApplication("CommentRatingAPI-Application", "Gold", "", "this-is-test");
		SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName,
                apimContext.getContextTenant()
		                                                                         .getContextUser()
		                                                                         .getUserName());
		subscriptionRequest.setApplicationName("CommentRatingAPI-Application");
		apiStore.subscribe(subscriptionRequest);

		GenerateAppKeyRequest generateAppKeyRequest =
				new GenerateAppKeyRequest("CommentRatingAPI-Application");
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
		Thread.sleep(2000);
		for (int i = 0; i < 19; i++) {

			HttpResponse youTubeResponse = HttpRequestUtil
					.doGet(getGatewayServerURLHttp()+"commentRating/1.0.0/most_popular",
					       requestHeaders);

			assertEquals(youTubeResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "Response code mismatched");
			assertTrue(youTubeResponse.getData().contains("<feed"),
                    "Response data mismatched");
			assertTrue(youTubeResponse.getData().contains("<category"),
                    "Response data mismatched");
			assertTrue(youTubeResponse.getData().contains("<entry>"),
                    "Response data mismatched");

		}
		//Do get,post,put,delete all here
		//HttpResponse youTubeResponse = HttpRequestUtil.doGet(getApiInvocationURLHttp("commentRating/1.0.0/most_popular"), requestHeaders);

		Thread.sleep(60000);
		HttpResponse youTubeResponse1 = HttpRequestUtil
				.doGet(getGatewayServerURLHttp()+"commentRating/1.0.0/most_popular", null);
		assertEquals(youTubeResponse1.getResponseCode(), 401, "Response code mismatched");
		// URL url1 = new URL(url);
		// HttpResponse youTubeResponse2 = HttpRequestUtil.doPost(url1,"-");
		//Remove subscription and then remove API

		apiStore.getAllPublishedAPIs();
		apiStore.getAllApplications();
		apiStore.getPublishedAPIsByApplication("CommentRatingAPI-Application");
		apiStore.isRatingActivated();
		apiStore.addRatingToAPI(APIName, APIVersion, providerName, "4");
		apiStore.removeRatingFromAPI(APIName, APIVersion, providerName);
		apiStore.getAllDocumentationOfApi(APIName, APIVersion, providerName);
		//apiStore.getAllPaginatedPublishedAPIs("carbon.super","0","10");
		//Negative cases
		//add assert
		apiStore.getPublishedAPIsByApplication("CommentRatingAPI-Application-Wrong");
		apiStore.isRatingActivated();
		apiStore.addRatingToAPI("NoAPI", APIVersion, providerName, "4");
		apiStore.removeRatingFromAPI("NoAPI", APIVersion, providerName);
		apiStore.getAllDocumentationOfApi("NoAPI", APIVersion, providerName);
	}

	@AfterClass(alwaysRun = true)
	public void destroy() throws Exception {
		apiStore.removeApplication("CommentRatingAPI-Application");
		super.cleanup();
	}
}
