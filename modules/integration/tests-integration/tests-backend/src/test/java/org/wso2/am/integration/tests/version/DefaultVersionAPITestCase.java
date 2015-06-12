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
package org.wso2.am.integration.tests.version;

import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * This class is used to check the functionality of the Default Version API.
 * <p>
 * Note: By default an API will always have a version. Ex: 1.0.0. It is mandatory that the API request url contains this
 * version field. Ex: http://localhost:8280/twitter/1.0.0. By specifying a particular version of an API as the 'default'
 * versioned API, it makes it possible to invoke that particular api without having the version as part of the request
 * url. Ex: http://localhost:8280/twitter
 * </p>
 */
public class DefaultVersionAPITestCase extends APIMIntegrationBaseTest {

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String gatewaySessionCookie;
    private String provider;

    @Factory(dataProvider = "userModeDataProvider")
    public DefaultVersionAPITestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }


    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        gatewaySessionCookie = createSession(gatewayContext);
        //Initialize publisher and store.
        apiPublisher = new APIPublisherRestClient(publisherUrls.getWebAppURLHttps());
        apiStore = new APIStoreRestClient(storeUrls.getWebAppURLHttp());
        provider = storeContext.getContextTenant().getContextUser().getUserName();

        //Load the back-end dummy API
        loadSynapseConfigurationFromClasspath("artifacts" + File.separator + "AM"
                                              + File.separator + "synapseconfigs" + File.separator + "rest"
                                              + File.separator + "dummy_api.xml", gatewayContext, gatewaySessionCookie);
    }

    @Test(groups = "wso2.am", description = "Check functionality of the default version API")
    public void testDefaultVersionAPI() throws Exception {

        //Login to the API Publisher
        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                           publisherContext.getContextTenant().getContextUser().getPassword());

        String apiName = "DefaultVersionAPI";
        String apiVersion = "1.0.0";
        String apiContext = "defaultversion";
        String endpointUrl = gatewayUrls.getWebAppURLNhttp() + "response";

        //Create the api creation request object
        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(endpointUrl));
        apiRequest.setDefault_version("default_version");
        apiRequest.setDefault_version_checked("default_version");
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection("Unlimited");
        apiRequest.setTier("Unlimited");

        //Add the API using the API publisher.
        apiPublisher.addAPI(apiRequest);

        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest(apiName, publisherContext.getContextTenant().getContextUser().getUserName(),
                                             APILifeCycleState.PUBLISHED);
        //Publish the API
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        //Login to the API Store
        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                       storeContext.getContextTenant().getContextUser().getPassword());

        //Add an Application in the Store.
        apiStore.addApplication("DefaultVersionAPP", "Unlimited", "", "");

        //Subscribe the API to the DefaultApplication
        SubscriptionRequest subscriptionRequest =
                new SubscriptionRequest(apiName, apiVersion, provider, "DefaultVersionAPP", "Unlimited");
        apiStore.subscribe(subscriptionRequest);

        //Generate production token and invoke with that
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator("DefaultVersionAPP");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);

        //Get the accessToken which was generated.
        String accessToken = response.getJSONObject("data").getJSONObject("key").getString("accessToken");

        String directBackEndEndpoint, apiInvocationUrl;
        if (gatewayContext.getContextTenant().getDomain().equals("carbon.super")) {
            directBackEndEndpoint = gatewayUrls.getWebAppURLNhttp() + "response";
            apiInvocationUrl = gatewayUrls.getWebAppURLNhttp() + apiContext;
        } else {
            directBackEndEndpoint = gatewayUrls.getWebAppURLNhttp() + "response";
            apiInvocationUrl = gatewayUrls.getWebAppURLNhttp() + "t/" +
                               gatewayContext.getContextTenant().getDomain() + "/" + apiContext;
        }

        //Going to access the API without the version in the request url.
        HttpResponse directResponse = HttpRequestUtil.doGet(directBackEndEndpoint, new HashMap<String, String>());

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + accessToken);
        //Invoke the API
        HttpResponse httpResponse = HttpRequestUtil.doGet(apiInvocationUrl, headers);

        //Check if accessing the back-end directly and accessing it via the API yield the same responses.
        assertEquals(httpResponse.getData(), directResponse.getData(),
                     "Default version API test failed while " + "invoking the API.");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp(gatewayContext.getContextTenant().getTenantAdmin().getUserName(),
                      gatewayContext.getContextTenant().getContextUser().getPassword(),
                      storeUrls.getWebAppURLHttp(), publisherUrls.getWebAppURLHttp());
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }
}