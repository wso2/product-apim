/*
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.integration.tests.other;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
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
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;
import org.wso2.carbon.utils.ServerConstants;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class TokenEncryptionScopeTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(TokenEncryptionScopeTestCase.class);

    private APIPublisherRestClient apiPublisher;

    private APIStoreRestClient apiStore;

    private UserManagementClient userManagementClient = null;

    private static final String API_NAME = "TokenEncryptionAPI";

    private static final String API_VERSION = "1.0.0";

    private static final String APP_NAME = "TokenEncryptionApp";

    private static final String USER_SAM = "sam";

    private static final String APP_DEV_USER = "mike";

    private static final String APP_DEV_PWD = "mike123";

    private static final String SUBSCRIBER_ROLE = "subscriber";

    private ServerConfigurationManager serverManager;

    private static final String APIM_CONFIG_XML = "api-manager.xml";

    private static final String IDENTITY_CONFIG_XML = "identity.xml";

    private static String apiProvider;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init();

        apiProvider = publisherContext.getSuperTenant().getContextUser().getUserName();

        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
        String artifactsLocation = TestConfigurationProvider.getResourceLocation() +
                                   File.separator + "artifacts" + File.separator + "AM" + File.separator +
                                   "configFiles" + File.separator + "token_encryption" + File.separator;

        String apimConfigArtifactLocation = artifactsLocation + APIM_CONFIG_XML;
        String identityConfigArtifactLocation = artifactsLocation + IDENTITY_CONFIG_XML;

        String apimRepositoryConfigLocation = carbonHome + File.separator + "repository" +
                                              File.separator + "conf" + File.separator + APIM_CONFIG_XML;

        String identityRepositoryConfigLocation = carbonHome + File.separator + "repository" +
                                                  File.separator + "conf" + File.separator + "identity" + File.separator +
                                                  IDENTITY_CONFIG_XML;

        File apimConfSourceFile = new File(apimConfigArtifactLocation);
        File apimConfTargetFile = new File(apimRepositoryConfigLocation);

        File identityConfSourceFile = new File(identityConfigArtifactLocation);
        File identityConfTargetFile = new File(identityRepositoryConfigLocation);

        serverManager = new ServerConfigurationManager(gatewayContextWrk);

        // apply configuration to  api-manager.xml
        serverManager.applyConfigurationWithoutRestart(apimConfSourceFile, apimConfTargetFile, true);
        log.info("api-manager.xml configuration file copy from :" + apimConfigArtifactLocation +
                 " to :" + apimRepositoryConfigLocation);

        // apply configuration to identity.xml
        serverManager.applyConfigurationWithoutRestart(identityConfSourceFile, identityConfTargetFile, true);
        log.info("identity.xml configuration file copy from :" + identityConfigArtifactLocation +
                 " to :" + identityRepositoryConfigLocation);

        serverManager.restartGracefully();

        //Initialize publisher and store.
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();

        String storeURLHttp = storeUrls.getWebAppURLHttp();

        apiPublisher = new APIPublisherRestClient(publisherURLHttp);

        apiStore = new APIStoreRestClient(storeURLHttp);
    }

    @Test(groups = "wso2.am", description = "Check if Scopes work fine with token encryption enabled.")
    public void testTokenEncryptionWithScopes() {

        try {
            userManagementClient = new UserManagementClient(publisherContext.getContextUrls().getBackEndUrl(),
                                                            publisherContext.getContextTenant().getContextUser().getUserName(),
                                                            publisherContext.getContextTenant().getContextUser().getPassword());
            //adding new role subscriber
            userManagementClient.addRole(SUBSCRIBER_ROLE, new String[]{}, new String[]{"/permission/admin/login",
                                                                                       "/permission/admin/manage/api/subscribe"});

            //creating user sam
            userManagementClient.addUser(USER_SAM, "sam123", new String[]{SUBSCRIBER_ROLE}, "sam");

            //creating user mike
            userManagementClient.addUser(APP_DEV_USER, APP_DEV_PWD, new String[]{SUBSCRIBER_ROLE}, APP_DEV_USER);

        } catch (AxisFault axisFault) {
            log.error("Error while creating UserManagementClient " + axisFault.getMessage());
            //Fail the test case.
            Assert.assertTrue(false, axisFault.getMessage());
        } catch (RemoteException e) {
            log.error("Error while adding role 'subscriber' or user 'mike'" + e.getMessage());
            //Fail the test case.
            Assert.assertTrue(false, e.getMessage());
        } catch (UserAdminUserAdminException e) {
            log.error("Error while adding role 'subscriber' or user 'mike'" + e.getMessage());
            //Fail the test case.
            Assert.assertTrue(false, e.getMessage());
        } catch (XPathExpressionException e) {
            log.error("Error when getting backend URLs of the publisher to initialize the UserManagementClient"
                      + e.getMessage());
            //Fail the test case.
            Assert.assertTrue(false, e.getMessage());
        }

        // Adding API
        String apiContext = "tokenencapi";
        String endpointUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "response";

        //Create the api creation request object
        APIRequest apiRequest = null;
        try {
            apiRequest = new APIRequest(API_NAME, apiContext, new URL(endpointUrl));
        } catch (MalformedURLException e) {
            log.error("Invalid URL " + endpointUrl, e);
            //Fail the test case
            Assert.assertTrue(false);
        } catch (APIManagerIntegrationTestException e) {
            log.error("Integration Test error occurred ", e);
            //Fail the test case
            Assert.assertTrue(false);
        }
        apiRequest.setVersion(API_VERSION);
        apiRequest.setTiersCollection("Unlimited");
        apiRequest.setTier("Unlimited");

        try {
            apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                               publisherContext.getContextTenant().getContextUser().getPassword());

            apiPublisher.addAPI(apiRequest);

            //publishing API
            APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(
                    API_NAME, apiProvider,
                    APILifeCycleState.PUBLISHED);
            apiPublisher.changeAPILifeCycleStatus(updateRequest);

            //resources are modified using swagger doc.
            // admin_scope(used for POST) :- admin
            // user_scope (used for GET) :- admin,subscriber
            String modifiedResource = "{\"paths\":{ \"/*\":{\"put\":{ \"responses\":{\"200\":{}},\"x-auth-type\":\"Application User\"," +
                                      "\"x-throttling-tier\":\"Unlimited\" },\"post\":{ \"responses\":{\"200\":{}},\"x-auth-type\":\"Application User\"," +
                                      "\"x-throttling-tier\":\"Unlimited\",\"x-scope\":\"admin_scope\"},\"get\":{ \"responses\":{\"200\":{}},\"x-auth-type\":\"Application User\"," +
                                      "\"x-throttling-tier\":\"Unlimited\",\"x-scope\":\"user_scope\"},\"delete\":{ \"responses\":{\"200\":{}},\"x-auth-type\":\"Application User\"," +
                                      "\"x-throttling-tier\":\"Unlimited\"},\"options\":{ \"responses\":{\"200\":{}},\"x-auth-type\":\"None\"," +
                                      "\"x-throttling-tier\":\"Unlimited\"}}},\"swagger\":\"2.0\",\"info\":{\"title\":\"" + API_NAME + "\",\"version\":\"1.0.0\"}," +
                                      "\"x-wso2-security\":{\"apim\":{\"x-wso2-scopes\":[{\"name\":\"admin_scope\",\"description\":\"\",\"key\":\"admin_scope\",\"roles\":\"admin\"}," +
                                      "{\"name\":\"user_scope\",\"description\":\"\",\"key\":\"user_scope\",\"roles\":\"admin,subscriber\"}]}}}";

            apiPublisher.updateResourceOfAPI(apiProvider, API_NAME, API_VERSION, modifiedResource);

            // For Admin user
            // create new application and subscribing
            apiStore.login(APP_DEV_USER, APP_DEV_PWD);
            apiStore.addApplication(APP_NAME, "Unlimited", "some_url2", "NewApp");
            SubscriptionRequest subscriptionRequest = new SubscriptionRequest(API_NAME, apiProvider);
            subscriptionRequest.setApplicationName(APP_NAME);
            subscriptionRequest.setTier("Unlimited");
            apiStore.subscribe(subscriptionRequest);

            //Generate production token and invoke with that
            APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(APP_NAME);
            String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
            JSONObject jsonResponse = new JSONObject(responseString);

            // get Consumer Key and Consumer Secret
            String consumerKey = jsonResponse.getJSONObject("data").getJSONObject("key").getString("consumerKey");
            String consumerSecret = jsonResponse.getJSONObject("data").getJSONObject("key").getString("consumerSecret");

            URL tokenEndpointURL = new URL(gatewayUrlsWrk.getWebAppURLNhttps() + "token");
            String requestBody;
            JSONObject accessTokenGenerationResponse;

            //Obtain user access token for sam, request scope 'user_scope'
            requestBody = "grant_type=password&username=" + USER_SAM + "&password=sam123&scope=user_scope";
            accessTokenGenerationResponse = new JSONObject(
                    apiStore.generateUserAccessKey(consumerKey, consumerSecret,
                                                   requestBody, tokenEndpointURL).getData());
            String receivedScope = accessTokenGenerationResponse.getString("scope");

            //Check if we receive the scope we requested for.
            Assert.assertEquals(receivedScope, "user_scope", "Received scope is " + receivedScope +
                                                             ", but expected user_scope");
        } catch (APIManagerIntegrationTestException e) {
            log.error("Error occurred while executing Test", e);
            //Fail the test case
            Assert.assertTrue(false);
        } catch (MalformedURLException e) {
            log.error("Malformed tokenEndpointURL ", e);
            //Fail the test case
            Assert.assertTrue(false);
        } catch (JSONException e) {
            log.error("Could not parse response JSON message received from the token endpoint ", e);
            //Fail the test case
            //Assert.assertTrue(false);
        } catch (XPathExpressionException e) {
            log.error("Error occurred while getting credentials from the publisher/store context ", e);
            //Fail the test case
            Assert.assertTrue(false);
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        if (apiStore != null) {
            apiStore.removeApplication(APP_NAME);
        }

        if (apiPublisher != null) {
            apiPublisher.deleteAPI(API_NAME, API_VERSION, apiProvider);
        }

        if (userManagementClient != null) {
            userManagementClient.deleteUser(USER_SAM);
            userManagementClient.deleteUser(APP_DEV_USER);
            userManagementClient.deleteRole(SUBSCRIBER_ROLE);
        }

        serverManager.restoreToLastConfiguration();
        serverManager.restartGracefully();
        log.info("Restored configuration and restarted gracefully...");
    }

}
