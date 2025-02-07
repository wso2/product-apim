/*
 *   Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;

import javax.xml.xpath.XPathExpressionException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class TokenEncryptionScopeTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(TokenEncryptionScopeTestCase.class);

    private UserManagementClient userManagementClient1 = null;

    private static final String API_NAME = "TokenEncryptionAPI";

    private static final String API_VERSION = "1.0.0";

    private static final String APP_NAME = "TokenEncryptionApp";

    private static final String USER_SAM = "sam";

    private static final String APP_DEV_USER = "mike";

    private static final String APP_DEV_PWD = "mike123";

    private static final String SUBSCRIBER_ROLE = "subscriber";

    private static String applicationId;
    private static String apiId;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
    }

    @Test(groups = "wso2.am", description = "Check if Scopes work fine with token encryption enabled.")
    public void testTokenEncryptionWithScopes() throws XPathExpressionException {

        try {
            userManagementClient1 = new UserManagementClient(publisherContext.getContextUrls().getBackEndUrl(),
                    publisherContext.getContextTenant().getContextUser().getUserName(),
                    publisherContext.getContextTenant().getContextUser().getPassword());
            //adding new role subscriber
            userManagementClient1.addRole(SUBSCRIBER_ROLE, new String[]{}, new String[]{"/permission/admin/login",
                    "/permission/admin/manage/api/subscribe"});

            //creating user sam
            userManagementClient1.addUser(USER_SAM, "sam123", new String[]{SUBSCRIBER_ROLE}, "sam");

            //creating user mike
            userManagementClient1.addUser(APP_DEV_USER, APP_DEV_PWD, new String[]{SUBSCRIBER_ROLE}, APP_DEV_USER);

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
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

        try {
            //Add the API using the API publisher.
            HttpResponse apiResponse = restAPIPublisher.addAPI(apiRequest);
            apiId = apiResponse.getData();

            //publishing API
            restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);

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

            restAPIPublisher.updateSwagger(apiId, modifiedResource);

            HttpResponse applicationResponse = restAPIStore.createApplication(APP_NAME,
                    "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                    ApplicationDTO.TokenTypeEnum.OAUTH);
            applicationId = applicationResponse.getData();

            //Generate production token and invoke with that
            ArrayList grantTypes = new ArrayList();
            grantTypes.add("client_credentials");
            ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "3600", null, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);

            // get Consumer Key and Consumer Secret
            String consumerKey = applicationKeyDTO.getConsumerKey();
            String consumerSecret = applicationKeyDTO.getConsumerSecret();

            URL tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
            String requestBody;

            //Obtain user access token for sam, request scope 'user_scope'
            requestBody = "grant_type=password&username=" + USER_SAM + "&password=sam123&scope=user_scope";
            HttpResponse firstResponse = restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody,
                    tokenEndpointURL);
            JSONObject accessTokenGenerationResponse = new JSONObject(firstResponse.getData());
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
        } catch (ApiException e) {
            e.printStackTrace();
        } catch (org.wso2.am.integration.clients.store.api.ApiException e) {
            e.printStackTrace();
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(apiId);
        if (userManagementClient1 != null) {
            userManagementClient1.deleteUser(USER_SAM);
            userManagementClient1.deleteUser(APP_DEV_USER);
            userManagementClient1.deleteRole(SUBSCRIBER_ROLE);
        }
    }

}