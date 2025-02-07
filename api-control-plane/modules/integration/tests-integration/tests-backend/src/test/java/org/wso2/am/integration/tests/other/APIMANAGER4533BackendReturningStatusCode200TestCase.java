/*
*Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.admin.clients.mediation.SynapseConfigAdminClient;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;

import javax.xml.xpath.XPathExpressionException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Issue is discussed in https://wso2.org/jira/browse/APIMANAGER-4464
 * fix is available in https://wso2.org/jira/browse/ESBJAVA-4386 and https://wso2.org/jira/browse/CARBON-15759
 */
public class APIMANAGER4533BackendReturningStatusCode200TestCase extends APIMIntegrationBaseTest {
	private static final Log log = LogFactory.getLog(APIMANAGER4533BackendReturningStatusCode200TestCase.class);

	private APIPublisherRestClient apiPublisher;
	private APIStoreRestClient apiStore;
	private String newSynapseConfig;
	private SynapseConfigAdminClient synapseConfigAdminClient;

	@Factory(dataProvider = "userModeDataProvider")
	public APIMANAGER4533BackendReturningStatusCode200TestCase(TestUserMode userMode) {
		this.userMode = userMode;
	}

	@DataProvider
	public static Object[][] userModeDataProvider() {
		return new Object[][] {
				new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
				new Object[] { TestUserMode.TENANT_ADMIN },
				new Object[] { TestUserMode.SUPER_TENANT_USER },
				new Object[] { TestUserMode.TENANT_USER }
		};
	}

	@BeforeClass(alwaysRun = true)
	public void setEnvironment() throws Exception {
		super.init(userMode);

		String gatewaySessionCookie = createSession(gatewayContextWrk);
		//Initialize publisher and store.
		apiPublisher = new APIPublisherRestClient(publisherUrls.getWebAppURLHttp());
		apiStore = new APIStoreRestClient(storeUrls.getWebAppURLHttp());

		synapseConfigAdminClient =
				new SynapseConfigAdminClient(gatewayContextWrk.getContextUrls().getBackEndUrl(), gatewaySessionCookie);

		String synapseConfigArtifactsPath =
				TestConfigurationProvider.getResourceLocation() + File.separator + "artifacts" + File.separator + "AM" +
				File.separator + "synapseconfigs" + File.separator + "rest" + File.separator +
				"dummy_api_APIMANAGER-4533.xml";
		newSynapseConfig = readFile(synapseConfigArtifactsPath);
		synapseConfigAdminClient.updateConfiguration(newSynapseConfig);
	}

	@Test(groups = "wso2.am",
	      description = "Send a request to a backend returning 200 and check if the expected result is received")
	public void testAPIReturningStatusCode200() {
		//Login to the API Publisher
		try {
			apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
			                   publisherContext.getContextTenant().getContextUser().getPassword());
		} catch (APIManagerIntegrationTestException e) {
			log.error("APIManagerIntegrationTestException " + e.getMessage());
			Assert.assertTrue(false);
		} catch (XPathExpressionException e) {
			log.error("XPathExpressionException " + e.getMessage());
			Assert.assertTrue(false);
		}

		String apiName = "Test200_API" + userMode;
		String apiVersion = "1.0.0";
		String apiContext = "test200_api" + userMode;
		String endpointUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "response200";

		//Create the api creation request object
		APIRequest apiRequest = null;
		try {
			apiRequest = new APIRequest(apiName, apiContext, new URL(endpointUrl));
		} catch (APIManagerIntegrationTestException e) {
			log.error("Error creating APIRequest " + e.getMessage());
			Assert.assertTrue(false);
		} catch (MalformedURLException e) {
			log.error("Invalid URL " + gatewayUrlsWrk.getWebAppURLNhttp() + "response200", e);
			Assert.assertTrue(false);
		}

		apiRequest.setVersion(apiVersion);
		apiRequest.setTiersCollection("Unlimited");
		apiRequest.setTier("Unlimited");
		apiRequest.setResourceMethod("GET");

		try {
			apiRequest.setProvider(publisherContext.getContextTenant().getContextUser().getUserName());

			//Add the API using the API publisher.
			apiPublisher.addAPI(apiRequest);

			APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName,
			                                                                      publisherContext.getContextTenant()
			                                                                                      .getContextUser()
			                                                                                      .getUserName(),
			                                                                      APILifeCycleState.PUBLISHED);
			//Publish the API
			apiPublisher.changeAPILifeCycleStatus(updateRequest);

			//Login to the API Store
			apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
			               storeContext.getContextTenant().getContextUser().getPassword());

			//Add an Application in the Store.
			apiStore.addApplication("APP200", "Unlimited", "", "");

			//Subscribe the API to the DefaultApplication
			SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, apiVersion,
			                                                                  storeContext.getContextTenant()
			                                                                              .getContextUser()
			                                                                              .getUserName(), "APP200",
			                                                                  "Unlimited");
			apiStore.subscribe(subscriptionRequest);

			//Generate production token and invoke with that
			APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator("APP200");
			String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
			JSONObject response = new JSONObject(responseString);

			//Get the accessToken which was generated.
			String accessToken = response.getJSONObject("data").getJSONObject("key").getString("accessToken");

			String apiInvocationUrl;
			if (userMode == TestUserMode.TENANT_ADMIN || userMode == TestUserMode.TENANT_USER) {
				apiInvocationUrl = gatewayUrlsWrk.getWebAppURLNhttps() + "t/wso2.com/" + apiContext + "/" + apiVersion;
			} else {
				apiInvocationUrl = gatewayUrlsWrk.getWebAppURLNhttps() + apiContext + "/" + apiVersion;
			}

			HttpClient httpclient = new DefaultHttpClient();
			HttpUriRequest get = new HttpGet(apiInvocationUrl);
			get.addHeader(new BasicHeader("Authorization", "Bearer " + accessToken));
			get.addHeader(new BasicHeader("Accept", "application/json"));
			org.apache.http.HttpResponse httpResponse = httpclient.execute(get);

			Assert.assertEquals(httpResponse.getStatusLine().getStatusCode(), 200, "Status Code is not 200");

		} catch (APIManagerIntegrationTestException e) {
			log.error("APIManagerIntegrationTestException " + e.getMessage(), e);
			Assert.assertTrue(false);
		} catch (JSONException e) {
			log.error("Error parsing JSON to get access token " + e.getMessage(), e);
			Assert.assertTrue(false);
		} catch (XPathExpressionException e) {
			log.error("XPathExpressionException " + e.getMessage(), e);
			Assert.assertTrue(false);
		} catch (IOException e) {
			log.error("IOException " + e.getMessage(), e);
			Assert.assertTrue(false);
		}
	}

	@AfterClass(alwaysRun = true)
	public void destroy() throws Exception {
		super.cleanUp();
	}

	/**
	 * Read the file content and return the content as String.
	 *
	 * @param fileLocation - Location of the file.
	 * @return String - content of the file.
	 * @throws APIManagerIntegrationTestException - exception throws when reading the file.
	 */
	protected String readFile(String fileLocation) throws APIManagerIntegrationTestException {
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new FileReader(new File(fileLocation)));
			String line;
			StringBuilder stringBuilder = new StringBuilder();
			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line);
			}
			return stringBuilder.toString();
		} catch (IOException ioE) {
			throw new APIManagerIntegrationTestException("IOException when reading the file from:" + fileLocation, ioE);
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					log.warn("Error when closing the buffer reade which used to reed the file:" + fileLocation +
					         ". Error:" + e.getMessage());
				}
			}
		}
	}
}
