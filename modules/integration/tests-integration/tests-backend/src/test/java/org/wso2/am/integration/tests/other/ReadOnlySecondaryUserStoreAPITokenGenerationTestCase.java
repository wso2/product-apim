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

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.integration.common.admin.client.AuthenticatorClient;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;
import java.net.URL;

/*
* This class test the token generation for the APIs subscribed from an ReadOnly External Store
* */
public class ReadOnlySecondaryUserStoreAPITokenGenerationTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(ReadOnlySecondaryUserStoreAPITokenGenerationTestCase.class);

    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private static String apiProvider;
    private String gatewaySessionCookie;

    private ServerConfigurationManager serverConfigurationManager;
    private UserManagementClient userMgtClient;
    private UserManagementClient testUserMgtClient;
    private AuthenticatorClient authenticatorClient;
    private String newUserName = "CAT.COM/chamalee";
    private String newUserRole = "CAT.COM/userStoreSubscriber";

    private String newUserPassword = "password";
    private String backendURL;

    private static final String API_NAME = "JDBCSecStoreTestAPI";
    private static final String API_VERSION = "1.0.0";
    private static final String APP_NAME = "chamsApp";

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        super.init();
        //change the user-mgt.xml configuration
        String userMgtXml = getAMResourceLocation() + File.separator + "configFiles/externalstore/user-mgt.xml";
        serverConfigurationManager = new ServerConfigurationManager(gatewayContext);
        serverConfigurationManager.applyConfiguration(new File(userMgtXml));
    }

    @Test(groups = "wso2.am", description = "Read only External Store Token Generation")
    public void addUserIntoJDBCUserStore() throws Exception {

        gatewaySessionCookie = createSession(gatewayContext);
        backendURL = gatewayContext.getContextUrls().getBackEndUrl();

        userMgtClient = new UserManagementClient(backendURL, gatewaySessionCookie);
        authenticatorClient = new AuthenticatorClient(gatewayContext.getContextUrls().getBackEndUrl());

        //add Role
        userMgtClient.addRole(newUserRole, null, new String[]{"/permission/admin/login"
                , "/permission/admin/manage/api/subscribe"});
        Assert.assertTrue(userMgtClient.roleNameExists(newUserRole), "Role name doesn't exists");

        //add User
        userMgtClient.addUser(newUserName, newUserPassword, new String[]{newUserRole}, null);
        Assert.assertTrue(userMgtClient.userNameExists(newUserRole, newUserName), "User name doesn't exists");

        String sessionCookie = authenticatorClient.login(newUserName, newUserPassword, "localhost");
        Assert.assertTrue(sessionCookie.contains("JSESSIONID"), "Session Cookie not found. Login failed");


        //change the user-mgt.xml file with read only property
        String userMgtReadOnlyXml = getAMResourceLocation() + File.separator
                + "configFiles/externalstore/user-mgt-readonly.xml";
        String targetReadOnlyXML = serverConfigurationManager.getCarbonHome() + "/repository/conf/user-mgt.xml";

        serverConfigurationManager = new ServerConfigurationManager(gatewayContext);
        serverConfigurationManager.applyConfiguration(new File(userMgtReadOnlyXml), new File(targetReadOnlyXML));

        //create a new gateway session cookie since server is restarted
        String newGatewaySessionCookie = createSession(gatewayContext);

        //check for AxisFault when adding roles to the read only user store
        testUserMgtClient = new UserManagementClient(backendURL, newGatewaySessionCookie);
        AuthenticatorClient testAuthenticatorClient = new AuthenticatorClient(gatewayContext.getContextUrls().getBackEndUrl());

        try{
            testUserMgtClient.addRole("CAT.COM/testRole", null, new String[]{"/permission/admin/login"
                    , "/permission/admin/manage/api/subscribe"});

        }catch (AxisFault axisFault) {
            log.info("The Exception is expected since adding roles is disabled when user store is readonly.");
        }
        Assert.assertEquals(testUserMgtClient.roleNameExists("CAT.COM/testRole"), false,
                "User Store is still not readonly");

        //Load the back-end dummy API
        loadSynapseConfigurationFromClasspath("artifacts" + File.separator + "AM"
                + File.separator + "synapseconfigs" + File.separator + "rest"
                + File.separator + "dummy_api.xml", gatewayContext, newGatewaySessionCookie);

        //add an API
        String apiContext = "test";
        String tags = "testing";
        String url = gatewayUrls.getWebAppURLNhttp() + "response";
        String description = "This is a test API created by API manager integration test";

        apiPublisher = new APIPublisherRestClient(publisherUrls.getWebAppURLHttp());
        apiStore = new APIStoreRestClient(storeUrls.getWebAppURLHttp());
        apiProvider = publisherContext.getSuperTenant().getContextUser().getUserName();

        apiPublisher.login("admin", "admin");

        APIRequest apiRequest = new APIRequest(API_NAME, apiContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(API_VERSION);
        apiRequest.setProvider(apiProvider);
        apiRequest.setSandbox(url);

        apiPublisher.addAPI(apiRequest);

        //publish the API
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(API_NAME, apiProvider,
                APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

        authenticatorClient.logOut();

        //go to store with external store user credentials
        apiStore.login(newUserName, newUserPassword);

        //create application
        apiStore.addApplication(APP_NAME, "Unlimited", "some_url", "NewApp");

        //subscribed the API
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(API_NAME, apiProvider);
        subscriptionRequest.setApplicationName(APP_NAME);

        apiStore.subscribe(subscriptionRequest);

        //Generate sandbox Token
        APPKeyRequestGenerator generateAppKeyRequestSandBox = new APPKeyRequestGenerator(APP_NAME);
        generateAppKeyRequestSandBox.setKeyType("SANDBOX");
        String responseStringSandBox =
                apiStore.generateApplicationKey(generateAppKeyRequestSandBox).getData();
        JSONObject responseSandBox = new JSONObject(responseStringSandBox);
        Assert.assertEquals(responseSandBox.getJSONObject("data").equals(null), false, "Generating SANDBOX key failed");

        String sandBoxAccessToken =
                responseSandBox.getJSONObject("data").getJSONObject("key").get("accessToken")
                        .toString();
        Assert.assertEquals(sandBoxAccessToken.isEmpty(), false, "SANDBOX access token is Empty");


        APPKeyRequestGenerator generateAppKeyRequestProduction = new APPKeyRequestGenerator(APP_NAME);
        generateAppKeyRequestProduction.setKeyType("PRODUCTION");

        String responseStringProduction =
                apiStore.generateApplicationKey(generateAppKeyRequestProduction).getData();
        JSONObject responseProduction = new JSONObject(responseStringProduction);
        Assert.assertEquals(responseProduction.getJSONObject("data").equals(null), false,
                "Generating production key failed");

        String productionAccessToken =
                responseProduction.getJSONObject("data").getJSONObject("key").get("accessToken")
                        .toString();
        Assert.assertEquals(productionAccessToken.isEmpty(), false, "Production access token is Empty");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        //removing APIs and Applications
        super.cleanUp(gatewayContext.getContextTenant().getTenantAdmin().getUserName(),
                gatewayContext.getContextTenant().getContextUser().getPassword(),
                storeUrls.getWebAppURLHttp(), publisherUrls.getWebAppURLHttp());

        //restore configuration
        serverConfigurationManager.restoreToLastConfiguration();
        log.info("Restored configuration");
    }
}

