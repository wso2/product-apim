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

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.AMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.publisher.utils.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;

import javax.ws.rs.core.Response;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DefaultVersionWithScopesTestCase extends AMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(DefaultVersionWithScopesTestCase.class);

    private APIPublisherRestClient apiPublisher;

    private APIStoreRestClient apiStore;

    private UserManagementClient userManagementClient = null;

    private static final String API_NAME = "DefaultVersionScopeAPI";

    private static final String API_VERSION = "1.0.0";

    private static final String API_PROVIDER = "admin";

    private static final String APP_NAME = "DefVersionScopeApp";

    private static final String USER_SAM = "sam";

    private static final String USER_MIKE = "mike";

    private static final String SUBSCRIBER_ROLE = "subscriber";

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();

        //Initialize publisher and store.
        apiPublisher = new APIPublisherRestClient(getPublisherServerURLHttp());
        apiStore = new APIStoreRestClient(getStoreServerURLHttp());

        //Load the back-end dummy API
        loadAPIMConfigurationFromClasspath("artifacts" + File.separator + "AM"
                + File.separator + "synapseconfigs" + File.separator + "rest"
                + File.separator + "dummy_api.xml");
    }

    @Test(groups = "wso2.am", description = "Check functionality of the default version API with scopes")
    public void testDefaultVersionAPIWithScopes() throws UserAdminUserAdminException, RemoteException,
            XPathExpressionException, JSONException {

        //Add a user called mike and assign him to the subscriber role.
        try {
            userManagementClient = new UserManagementClient(apimContext.getContextUrls().getBackEndUrl(), "admin",
                    "admin");
            //adding new role subscriber
            userManagementClient.addRole(SUBSCRIBER_ROLE, new String[]{}, new String[]{"/permission/admin/login",
                    "/permission/admin/manage/api/subscribe"});

            //creating user mike
            userManagementClient.addUser(USER_MIKE, "mike123", new String[]{}, USER_MIKE);

            //creating user sam
            userManagementClient.addUser(USER_SAM, "sam123", new String[]{SUBSCRIBER_ROLE}, "sam");

        } catch (AxisFault axisFault) {
            log.error("Error while creating UserManagementClient " + axisFault.getMessage());
            //Fail the test case.
            assertTrue(false, axisFault.getMessage());
        } catch (RemoteException e) {
            log.error("Error while adding role 'subscriber' or user 'mike'" + e.getMessage());
            //Fail the test case.
            assertTrue(false, e.getMessage());
        } catch (UserAdminUserAdminException e) {
            log.error("Error while adding role 'subscriber' or user 'mike'" + e.getMessage());
            //Fail the test case.
            assertTrue(false, e.getMessage());
        }

        String apiProviderPassword = "admin";

        // Adding API
        String apiContext = "defaultversionscope";
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
        apiRequest.setDefault_version("default_version");
        apiRequest.setDefault_version_checked("default_version");
        apiRequest.setVersion(API_VERSION);
        apiRequest.setTiersCollection("Unlimited");
        apiRequest.setTier("Unlimited");

        try {
            apiPublisher.login(API_PROVIDER, apiProviderPassword);

            apiPublisher.addAPI(apiRequest);

            //publishing API
            APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(API_NAME, API_PROVIDER,
                    APILifeCycleState.PUBLISHED);
            apiPublisher.changeAPILifeCycleStatusTo(updateRequest);

            //resources are modified using swagger doc.
            String modifiedResource = "{\"apiVersion\":\"1.0.0\",\"swaggerVersion\":\"1.2\"," +
                    "\"authorizations\":{\"oauth2\":{\"scopes\":[{\"description\":\"\", " +
                    "\"name\":\"admin_scope\",\"roles\":\"admin\",\"key\":\"admin_scope\"}," +
                    "{\"description\":\"\",\"name\":\"user_scope\",\"roles\":\"subscriber\"," +
                    "\"key\":\"user_scope\"}]," +
                    "\"type\":\"oauth2\"}},\"apis\":[{\"index\":0,\"file\":{\"apiVersion\":\"1.0.0\"," +
                    "\"swaggerVersion\":\"1.2\",\"resourcePath\":\"/default\",\"apis\":[{\"index\":0," +
                    "\"path\":\"/*\",\"operations\":[{\"scope\":\"user_scope\"," +
                    "\"auth_type\":\"Application User\"," +
                    "\"throttling_tier\":\"Unlimited\",\"method\":\"GET\",\"parameters\":[]}," +
                    "{\"scope\":\"\",\"auth_type\":\"Application User\"," +
                    "\"throttling_tier\":\"Unlimited\"," +
                    "\"method\":\"POST\",\"parameters\":[]},{\"scope\":\"\",\"auth_type\":\"Application" +
                    " User\"," +
                    "\"throttling_tier\":\"Unlimited\",\"method\":\"PUT\",\"parameters\":[]}," +
                    "{\"auth_type\":\"Application User\",\"throttling_tier\":\"Unlimited\"," +
                    "\"method\":\"DELETE\"," +
                    "\"parameters\":[]},{\"auth_type\":\"None\",\"throttling_tier\":\"Unlimited\"," +
                    "\"method\":\"OPTIONS\",\"parameters\":[]}]}]},\"description\":\"\",\"path\":" +
                    "\"/default\"}],\"info\":{\"title\":\"" + API_NAME + "\",\"termsOfServiceUrl\":\"" +
                    "\",\"description\":\"\",\"license\":\"\",\"contact\":\"\",\"licenseUrl\":\"\"}}";

            apiPublisher.updateResourceOfAPI(API_PROVIDER, API_NAME, API_VERSION, modifiedResource
            );

            // For Admin user
            // create new application and subscribing
            apiStore.login("admin", "admin");
            apiStore.addApplication(APP_NAME, "Unlimited", "", "");
            SubscriptionRequest subscriptionRequest = new SubscriptionRequest(API_NAME, API_PROVIDER);
            subscriptionRequest.setApplicationName(APP_NAME);
            apiStore.subscribe(subscriptionRequest);

            //Generate production token and invoke with that
            GenerateAppKeyRequest generateAppKeyRequest = new GenerateAppKeyRequest(APP_NAME);
            String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
            JSONObject jsonResponse = new JSONObject(responseString);

            // get Consumer Key and Consumer Secret
            String consumerKey = jsonResponse.getJSONObject("data").getJSONObject("key").getString("consumerKey");
            String consumerSecret = jsonResponse.getJSONObject("data").getJSONObject("key").getString("consumerSecret");

            URL tokenEndpointURL = new URL("https://localhost:8243/token");
            String accessToken;
            Map<String, String> requestHeaders;
            HttpResponse response;
            String requestBody;
            JSONObject accessTokenGenerationResponse;

            //Obtain user access token for sam, request scope 'user_scope'
            requestBody = "grant_type=password&username=" + USER_SAM + "&password=sam123&scope=user_scope";
            accessTokenGenerationResponse = new JSONObject(apiStore.generateUserAccessKey(consumerKey, consumerSecret,
                    requestBody, tokenEndpointURL)
                    .getData());
            accessToken = accessTokenGenerationResponse.getString("access_token");

            requestHeaders = new HashMap<String, String>();
            requestHeaders.put("Authorization", "Bearer " + accessToken);

            //Accessing GET method without the version in the URL using the token sam received
            response = HttpRequestUtil.doGet(getGatewayServerURLHttp() + "/defaultversionscope", requestHeaders);
            assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                    "sam cannot access the GET Method. Response = "
                            + response.getData());

            //Obtaining user access token for mike, request scope 'user_scope'
            requestBody = "grant_type=password&username=" + USER_MIKE + "&password=mike123&scope=user_scope";
            accessTokenGenerationResponse = new JSONObject(apiStore.generateUserAccessKey(consumerKey, consumerSecret,
                    requestBody, tokenEndpointURL)
                    .getData());
            accessToken = accessTokenGenerationResponse.getString("access_token");

            requestHeaders = new HashMap<String, String>();
            requestHeaders.put("Authorization", "Bearer " + accessToken);

            //Accessing GET method without the version in the URL using the token mike received.
            response = HttpRequestUtil.doGet(getGatewayServerURLHttp() + "/defaultversionscope", requestHeaders);
            assertEquals(response.getResponseCode(), Response.Status.FORBIDDEN.getStatusCode(),
                    "Mike should receive an HTTP 403 when trying to access"
                            + " the GET resource. But the response code was " + response.getResponseCode());
        }
        //Catching generic Exception since apiPublisher and apiStore classes throw Exception from their methods.
        catch (Exception e) {
            log.error("Error while executing test case " + e.getMessage(), e);
            //fail the test case.
            assertTrue(false, e.getMessage());
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        if (apiStore != null) {
            apiStore.removeApplication(APP_NAME);
        }

        if (apiPublisher != null) {
            apiPublisher.deleteApi(API_NAME, API_VERSION, API_PROVIDER);
        }

        if (userManagementClient != null) {
            userManagementClient.deleteUser(USER_SAM);
            userManagementClient.deleteUser(USER_MIKE);
            userManagementClient.deleteRole(SUBSCRIBER_ROLE);
        }

        super.cleanup();
    }
}
