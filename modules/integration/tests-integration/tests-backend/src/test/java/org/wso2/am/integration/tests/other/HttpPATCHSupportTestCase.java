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

package org.wso2.am.integration.tests.other;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import org.testng.annotations.*;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import javax.ws.rs.core.Response;
import java.net.URL;

import static org.testng.Assert.assertEquals;

/**
 * This class is used to check the functionality of sending an HTTP PATCH request and receiving a 200 OK response from the backend.
 */

public class HttpPATCHSupportTestCase extends APIMIntegrationBaseTest {

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;

    @Factory(dataProvider = "userModeDataProvider")
    public HttpPATCHSupportTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN } };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        //Initialize publisher and store.
        apiPublisher = new APIPublisherRestClient(publisherUrls.getWebAppURLHttp());
        apiStore = new APIStoreRestClient(storeUrls.getWebAppURLHttp());
    }

    @Test(groups = "wso2.am", description = "Check functionality of HTTP PATCH support for APIM")
    public void testHttpPatchSupport() throws Exception {
        //Login to the API Publisher
        apiPublisher.login(user.getUserName(), user.getPassword());

        String APIName = "HttpPatchAPI";
        String APIContext = "patchTestContext";
        String url = "http://jsonplaceholder.typicode.com/posts/1";
        String providerName = user.getUserName();
        String APIVersion = "1.0.0";

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(url));
        apiRequest.setVersion(APIVersion);
        apiRequest.setProvider(providerName);

        //Adding the API to the publisher
        apiPublisher.addAPI(apiRequest);

        //Publish the API
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(APIName, providerName,
                APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        String modifiedResource = "{\"paths\":{ \"/*\":{\"patch\":{ \"responses\":{\"200\":{}},\"x-auth-type\":\"Application\"," +
                "\"x-throttling-tier\":\"Unlimited\" },\"get\":{ \"responses\":{\"200\":{}},\"x-auth-type\":\"Application\"," +
                "\"x-throttling-tier\":\"Unlimited\",\"x-scope\":\"user_scope\"}}},\"swagger\":\"2.0\",\"info\":{\"title\":\"HttpPatchAPI\",\"version\":\"1.0.0\"}," +
                "\"x-wso2-security\":{\"apim\":{\"x-wso2-scopes\":[{\"name\":\"admin_scope\",\"description\":\"\",\"key\":\"admin_scope\",\"roles\":\"admin\"}," +
                "{\"name\":\"user_scope\",\"description\":\"\",\"key\":\"user_scope\",\"roles\":\"admin,subscriber\"}]}}}";

        //Modify the resources to add the PATCH resource method to the API
        apiPublisher.updateResourceOfAPI(providerName, APIName, APIVersion, modifiedResource);

        //Login to the API Store
        apiStore.login(user.getUserName(), user.getPassword());

        //Add an Application in the Store.
        apiStore.addApplication("HttpPatchSupportAPP", "Gold", "", "Test-HTTP-PATCH");

        //Subscribe to the new application
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName, APIVersion, providerName,
                "HttpPatchSupportAPP", "Gold");
        apiStore.subscribe(subscriptionRequest);

        //Generate a production token and invoke the API
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator("HttpPatchSupportAPP");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject jsonResponse = new JSONObject(responseString);

        //Get the accessToken generated.
        String accessToken = jsonResponse.getJSONObject("data").getJSONObject("key").getString("accessToken");

        String apiInvocationUrl = getAPIInvocationURLHttp(APIContext, APIVersion);

        //Invoke the API by sending a PATCH request;

        HttpClient client = HttpClientBuilder.create().build();
        HttpPatch request = new HttpPatch(apiInvocationUrl);
        request.setHeader("Accept", "application/json");
        request.setHeader("Authorization", "Bearer " + accessToken);
        StringEntity payload = new StringEntity("{}", "UTF-8");
        payload.setContentType("application/json");
        request.setEntity(payload);

        HttpResponse httpResponsePatch = client.execute(request);

        //Assertion
        assertEquals(httpResponsePatch.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                "The response code is not 200 OK");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }

}