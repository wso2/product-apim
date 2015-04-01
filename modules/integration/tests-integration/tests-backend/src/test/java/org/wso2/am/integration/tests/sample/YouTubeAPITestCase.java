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

public class YouTubeAPITestCase extends AMIntegrationBaseTest {
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();

        apiStore = new APIStoreRestClient(storeURLHttp);
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);


        apiPublisher.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
        apiStore.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());

    }

    @Test(groups = {"wso2.am"}, description = "You Tube sample")
    public void testYouTubeApiSample() throws Exception {
        APIRequest apiRequest = new APIRequest("YoutubeFeeds", "youtube",
                new URL("http://gdata.youtube.com/feeds/api/standardfeeds"));
        apiPublisher.addAPI(apiRequest);
        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest("YoutubeFeeds", apimContext
                        .getContextTenant().getContextUser().getUserName(),
                        APILifeCycleState.PUBLISHED
                );
        apiPublisher.changeAPILifeCycleStatus(updateRequest);
        apiStore.addApplication("YoutubeFeeds-Application", "Gold", "", "this-is-test");
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest("YoutubeFeeds",
                apimContext.getContextTenant()
                        .getContextUser()
                        .getUserName()
        );
        subscriptionRequest.setApplicationName("YoutubeFeeds-Application");
        apiStore.subscribe(subscriptionRequest);

        GenerateAppKeyRequest generateAppKeyRequest =
                new GenerateAppKeyRequest("YoutubeFeeds-Application");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken =
                response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        Thread.sleep(2000);
        /*HttpResponse youTubeResponse = HttpRequestUtil.doGet(
                  getApiInvocationURLHttp("youtube/1.0.0/most_popular"), requestHeaders);*/

        HttpResponse youTubeResponse = HttpRequestUtil.doGet(
                gatewayUrls.getWebAppURLNhttp() + "/youtube/1.0.0/most_popular", requestHeaders);
        assertEquals(youTubeResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when api invocation");
        assertTrue(youTubeResponse.getData().contains("<feed"),
                "Response data mismatched when api invocation");
        assertTrue(youTubeResponse.getData().contains("<category"),
                "Response data mismatched when api invocation");
        assertTrue(youTubeResponse.getData().contains("<entry>"),
                "Response data mismatched when api invocation");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeApplication("YoutubeFeeds-Application");
        super.cleanup();
    }
}