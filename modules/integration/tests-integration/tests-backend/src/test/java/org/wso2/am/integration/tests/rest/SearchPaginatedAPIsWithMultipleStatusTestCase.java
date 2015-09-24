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

package org.wso2.am.integration.tests.rest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.net.URL;

public class SearchPaginatedAPIsWithMultipleStatusTestCase extends APIMIntegrationBaseTest {

	private APIPublisherRestClient apiPublisher;
	private APIStoreRestClient apiStore;
	private ServerConfigurationManager serverConfigurationManager;
	private final int apiCount = 24;
	private static final String PROVIDER = "admin";
	private static final String TENANT_DOMAIN = "carbon.super";
	private static final String API_NAME_PREFIX = "YoutubeFeeds";
	private static final String API_CONTEXT_PREFIX = "youtube";
	private static final String API_VERSION = "1.0.0";
	private static final String API_URL = "http://gdata.youtube.com/feeds/api/standardfeeds";
	private static final int PAGINATED_COUNT = 10;

	@Factory(dataProvider = "userModeDataProvider")
	public SearchPaginatedAPIsWithMultipleStatusTestCase(TestUserMode userMode) {
		this.userMode = userMode;
	}

	@DataProvider
	public static Object[][] userModeDataProvider() {
		return new Object[][]{
				new Object[]{TestUserMode.SUPER_TENANT_ADMIN}
		};
	}

	@BeforeClass(alwaysRun = true)
	public void setEnvironment() throws Exception {
		super.init(userMode);
		String publisherURLHttp = getPublisherURLHttp();
		String storeURLHttp = getStoreURLHttp();

		apiPublisher = new APIPublisherRestClient(publisherURLHttp);
		apiStore = new APIStoreRestClient(storeURLHttp);

		apiPublisher.login(user.getUserName(), user.getPassword());
		apiStore.login(user.getUserName(), user.getPassword());
	}

	@Test(groups = { "wso2.am" }, description = "check paginated API count")
	public void testPaginationWithMultipleStatus() throws Exception {
		for (int i = 0; i < apiCount; i++) {
			APIRequest apiRequest = new APIRequest(API_NAME_PREFIX + i, API_CONTEXT_PREFIX + i,
					new URL(API_URL));
			apiPublisher.addAPI(apiRequest);
			if (i % 2 == 0) {

				APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(API_NAME_PREFIX + i,
						user.getUserName(), APILifeCycleState.PUBLISHED);
				apiPublisher.changeAPILifeCycleStatus(updateRequest);
			}
			Thread.sleep(500);
		}

		Thread.sleep(10000);//Wait till APIs get indexed
		HttpResponse response = apiStore.searchPaginateAPIs(TENANT_DOMAIN, "0", "10", API_NAME_PREFIX);
		JSONObject responseJSON = new JSONObject(response.getData());
		JSONArray returnedAPIs = responseJSON.getJSONArray("result");
		Assert.assertEquals(returnedAPIs.length(), PAGINATED_COUNT);
	}

	@AfterClass(alwaysRun = true)
	public void destroy() throws Exception {
		for (int i = 0; i < apiCount; i++) {
			apiPublisher.deleteAPI(API_NAME_PREFIX + i, API_VERSION, PROVIDER);
		}
		super.cleanUp();
	}
}
