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
import org.wso2.carbon.am.tests.APIManagerIntegrationTest;
import org.wso2.carbon.am.tests.util.bean.APILifeCycleState;
import org.wso2.carbon.am.tests.util.APIPublisherRestClient;
import org.wso2.carbon.am.tests.util.APIStoreRestClient;
import org.wso2.carbon.am.tests.util.bean.APILifeCycleStateRequest;
import org.wso2.carbon.am.tests.util.bean.APIRequest;
import org.wso2.carbon.am.tests.util.bean.SubscriptionRequest;
import org.wso2.carbon.am.tests.util.bean.GenerateAppKeyRequest;
import org.wso2.carbon.automation.core.utils.HttpRequestUtil;
import org.wso2.carbon.automation.core.utils.HttpResponse;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class YouTubeAPITestCase extends APIManagerIntegrationTest {
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String publisherURLHttp;
    private String storeURLHttp;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init(0);
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
        apiPublisher.login(userInfo.getUserName(), userInfo.getPassword());
        APIRequest apiRequest = new APIRequest("YoutubeFeeds", "youtube", new URL("http://gdata.youtube.com/feeds/api/standardfeeds"));
        apiPublisher.addAPI(apiRequest);

        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest("YoutubeFeeds", userInfo.getUserName(), APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatusTo(updateRequest);

        apiStore.login(userInfo.getUserName(), userInfo.getPassword());

        //create application
        apiStore.addApplication("YoutubeFeedsApplication", "Gold", "", "this-is-test");

        // Subscribe to application
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest("YoutubeFeeds", userInfo.getUserName());
        subscriptionRequest.setApplicationName("YoutubeFeedsApplication");
        apiStore.subscribe(subscriptionRequest);

        GenerateAppKeyRequest generateAppKeyRequest = new GenerateAppKeyRequest("YoutubeFeedsApplication");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        Thread.sleep(2000);
        HttpResponse youTubeResponse = HttpRequestUtil.doGet(getApiInvocationURLHttp("youtube/1.0.0/most_popular"), requestHeaders);
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
