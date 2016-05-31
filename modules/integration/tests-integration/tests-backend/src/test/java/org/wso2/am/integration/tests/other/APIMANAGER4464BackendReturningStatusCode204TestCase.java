/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class APIMANAGER4464BackendReturningStatusCode204TestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(APIMANAGER4464BackendReturningStatusCode204TestCase.class);

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;

    @Factory(dataProvider = "userModeDataProvider")
    public APIMANAGER4464BackendReturningStatusCode204TestCase(TestUserMode userMode) {
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

        String gatewaySessionCookie = createSession(gatewayContextMgt);
        //Initialize publisher and store.
        apiPublisher = new APIPublisherRestClient(publisherUrls.getWebAppURLHttp());
        apiStore = new APIStoreRestClient(storeUrls.getWebAppURLHttp());

        //Load the back-end dummy API
        loadSynapseConfigurationFromClasspath(
                "artifacts" + File.separator + "AM" + File.separator + "synapseconfigs" + File.separator + "rest"
                        + File.separator + "dummy_api_APIMANAGER-4464.xml", gatewayContextMgt, gatewaySessionCookie);
    }

    @Test(groups = "wso2.am",
            description = "Send a request to a backend returning 204 and check if the expected result is received")
    public void testAPIReturningStatusCode204() throws Exception {
        //Login to the API Publisher
        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        String apiName = "Test_API" + userMode;
        String apiVersion = "1.0.0";
        String apiContext = "/somecontext" + userMode;
        String endpointUrl = gatewayUrlsMgt.getWebAppURLNhttp() + "response";

        //Create the api creation request object
        APIRequest apiRequest = null;
        try {
            apiRequest = new APIRequest(apiName, apiContext, new URL(endpointUrl));
        } catch (APIManagerIntegrationTestException e) {
            log.error("Error creating APIRequest " + e.getMessage());
            Assert.assertTrue(false);
        } catch (MalformedURLException e) {
            log.error("Invalid URL " + gatewayUrlsMgt.getWebAppURLNhttp() + "response", e);
            Assert.assertTrue(false);
        }

        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection("Unlimited");
        apiRequest.setTier("Unlimited");
        apiRequest.setResourceMethod("POST");

        try {
            apiRequest.setProvider(publisherContext.getContextTenant().getContextUser().getUserName());

            //Add the API using the API publisher.
            apiPublisher.addAPI(apiRequest);

            APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName,
                    publisherContext.getContextTenant().getContextUser().getUserName(), APILifeCycleState.PUBLISHED);
            //Publish the API
            apiPublisher.changeAPILifeCycleStatus(updateRequest);

            //Login to the API Store
            apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                    storeContext.getContextTenant().getContextUser().getPassword());

            //Add an Application in the Store.
            apiStore.addApplication("APP", "Unlimited", "", "");

            //Subscribe the API to the DefaultApplication
            SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, apiVersion,
                    storeContext.getContextTenant().getContextUser().getUserName(), "APP", "Unlimited");
            apiStore.subscribe(subscriptionRequest);

            //Generate production token and invoke with that
            APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator("APP");
            String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
            JSONObject response = new JSONObject(responseString);

            //Get the accessToken which was generated.
            String accessToken = response.getJSONObject("data").getJSONObject("key").getString("accessToken");

            String apiInvocationUrl;
            if (userMode == TestUserMode.TENANT_ADMIN || userMode == TestUserMode.TENANT_USER) {
                apiInvocationUrl = gatewayUrlsMgt.getWebAppURLNhttp() + "/t/wso2.com" + apiContext + "/" + apiVersion;
            } else {
                apiInvocationUrl = gatewayUrlsMgt.getWebAppURLNhttp() + apiContext + "/" + apiVersion;
            }

            HttpClient httpclient = new DefaultHttpClient();
            HttpUriRequest post = new HttpPost(apiInvocationUrl);
            post.addHeader(new BasicHeader("Authorization", "Bearer " + accessToken));
            org.apache.http.HttpResponse httpResponse = httpclient.execute(post);

            Assert.assertEquals(httpResponse.getStatusLine().getStatusCode(), 204, "Status Code is not 204");

        }  catch (JSONException e) {
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
}
