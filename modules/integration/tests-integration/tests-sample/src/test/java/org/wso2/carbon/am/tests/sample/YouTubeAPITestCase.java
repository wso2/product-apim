package org.wso2.carbon.am.tests.sample;

import org.wso2.am.integration.test.utils.APIManagerIntegrationTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;


import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class YouTubeAPITestCase  extends APIManagerIntegrationTest {
	private APIPublisherRestClient apiPublisher;
	private APIStoreRestClient apiStore;
	private String publisherURLHttp;
	private String storeURLHttp;

	@BeforeClass(alwaysRun = true)
	public void init() throws Exception {
		super.init();
		if (isBuilderEnabled()) {
			publisherURLHttp = getServerURLHttp();
			storeURLHttp = getServerURLHttp();
		}
		else {
			publisherURLHttp = getPublisherServerURLHttp();
			storeURLHttp = getStoreServerURLHttp();
		}
		apiPublisher = new APIPublisherRestClient(publisherURLHttp);
		apiStore = new APIStoreRestClient(storeURLHttp);
	}

	@Test(groups = {"wso2.am"}, description = "You Tube sample")
	public void testYouTubeApiSample() throws Exception {
		apiPublisher.login(amServer.getContextTenant().getContextUser().getUserName(), amServer.getContextTenant().getContextUser().getPassword());
		APIRequest apiRequest = new APIRequest("YoutubeFeeds", "youtube", new URL("http://gdata.youtube.com/feeds/api/standardfeeds"));
		apiPublisher.addAPI(apiRequest);


		APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest("YoutubeFeeds", amServer.getContextTenant().getContextUser().getUserName(), APILifeCycleState.PUBLISHED);
		apiPublisher.changeAPILifeCycleStatusTo(updateRequest);

		apiStore.login(amServer.getContextTenant().getContextUser().getUserName(), amServer.getContextTenant().getContextUser().getPassword());
		SubscriptionRequest subscriptionRequest = new SubscriptionRequest("YoutubeFeeds", amServer.getContextTenant().getContextUser().getUserName());
		apiStore.subscribe(subscriptionRequest);

		GenerateAppKeyRequest generateAppKeyRequest = new GenerateAppKeyRequest("DefaultApplication");
		String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
		JSONObject response = new JSONObject(responseString);
		String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.put("Authorization", "Bearer " + accessToken);

		Thread.sleep(2000);
		HttpResponse youTubeResponse = HttpRequestUtil.doGet(
				getApiInvocationURLHttp("youtube/1.0.0/most_popular"), requestHeaders);
		Assert.assertEquals(youTubeResponse.getResponseCode(), 200, "Response code mismatched when api invocation");
		Assert.assertTrue(youTubeResponse.getData().contains("<feed"), "Response data mismatched when api invocation");
		Assert.assertTrue(youTubeResponse.getData().contains("<category"), "Response data mismatched when api invocation");
		Assert.assertTrue(youTubeResponse.getData().contains("<entry>"), "Response data mismatched when api invocation");

	}

	@AfterClass(alwaysRun = true)
	public void destroy() throws Exception {
		super.cleanup();
	}
}
