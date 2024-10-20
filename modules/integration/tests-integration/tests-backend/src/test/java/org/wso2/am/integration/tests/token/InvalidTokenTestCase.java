/*
 *   Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.am.integration.tests.token;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertTrue;

/**
 * Test case for testing for errors when accessing an API using an invalid access token
 */
public class InvalidTokenTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(InvalidTokenTestCase.class);

    private APIPublisherRestClient apiPublisher;

    private String provider;

    private String id;

    private static final String API_NAME = "InvalidTokenAPI";

    private static final String API_VERSION = "1.0.0";

    @Factory(dataProvider = "userModeDataProvider")
    public InvalidTokenTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        //Initialize publisher and store.
        apiPublisher = new APIPublisherRestClient(getPublisherURLHttp());
        provider = user.getUserName();
    }

    @Test(groups = "wso2.am", description = "Check functionality of API access with invalid token")
    public void testAPIAccessWithInvalidToken()
            throws XPathExpressionException, APIManagerIntegrationTestException, ApiException, JSONException {

        // Adding API
        String apiContext = "invalidtokenapi";
        String endpointUrl = "http://localhost:8280/response";

        //Create the api creation request object
        APIRequest apiRequest = null;
        try {
            apiRequest = new APIRequest(API_NAME, apiContext, new URL(endpointUrl));
        } catch (MalformedURLException e) {
            log.error("Invalid URL " + endpointUrl, e);
            //Fail the test case
            assertTrue(false);
        }
        apiRequest.setVersion(API_VERSION);
        apiRequest.setTiersCollection("Unlimited");
        apiRequest.setTier("Unlimited");
        apiRequest.setProvider(provider);

        HttpResponse response = restAPIPublisher.addAPI(apiRequest);
        Assert.assertNotNull("API Creation failed", response.getData());
        id = response.getData();
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(id, restAPIPublisher);
        //publishing API
        restAPIPublisher.changeAPILifeCycleStatus(id, APILifeCycleAction.PUBLISH.getAction(), null);

        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                APIMIntegrationConstants.IS_API_EXISTS);

        String  apiInvocationUrl = getAPIInvocationURLHttp(apiContext, API_VERSION);

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + "abcdefgh");

        try {
            HttpResponse httpResponse = HttpRequestUtil.doGet(apiInvocationUrl, headers);
            //Fail test if response is null
            org.testng.Assert.assertNotNull(httpResponse);
            //Fail test if response code != 401
            Assert.assertEquals(401, httpResponse.getResponseCode());

            String responsePayload = httpResponse.getData();
            Assert.assertNotNull(responsePayload);
            JSONObject responsePayloadJsonObject = new JSONObject(responsePayload);
            Assert.assertNotNull(responsePayloadJsonObject.get("description"), "Error message doesn't contain a " +
                    "'description'");
            String description = responsePayloadJsonObject.getString("description");
            Assert.assertTrue(description.contains("Make sure you have provided the correct security credentials"),
                    "Unexpected error response string. Expected to have 'Make sure you have provided the correct " +
                            "security credentials' but received '" + description + "'");
        } catch (IOException e) {
            log.error("Error sending request to endpoint " + apiInvocationUrl, e);
            Assert.assertTrue(false,"Could not send request to endpoint " + apiInvocationUrl + ": " + e.getMessage());
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        undeployAndDeleteAPIRevisionsUsingRest(id, restAPIPublisher);
        restAPIPublisher.deleteAPI(id);
    }
}
