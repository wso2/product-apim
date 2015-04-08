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

package org.wso2.am.integration.tests.jwt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.admin.clients.user.RemoteUserStoreManagerServiceClient;
import org.wso2.am.integration.test.utils.APIMgtTestUtil;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.monitor.utils.WireMonitorServer;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;
import java.net.URL;

import static org.testng.AssertJUnit.assertTrue;


public class JWTTestCase extends APIMIntegrationBaseTest {

    private ServerConfigurationManager serverConfigurationManager;
    private UserManagementClient userManagementClient;
    private static final Log log = LogFactory.getLog(JWTTestCase.class);

    private String publisherURLHttp;
    private String storeURLHttp;
    private WireMonitorServer server;
    int hostPort = 9988;

    private String APIName = "JWTTokenTestAPI";
    private String APIContext = "tokenTest";
    private String tags = "token, jwt";
    private String wireMonitorURL = "";
    private String description = "This is test API create by API manager integration test";
    private String providerName = "admin";
    private String APIVersion = "1.0.0";
    private String ApplicationName = "APILifeCycleTestAPI-application";
    private String APITier = "Gold";

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();

        publisherURLHttp = publisherUrls.getWebAppURLHttp();
        storeURLHttp = storeUrls.getWebAppURLHttp();

        //enable JWT token generation
        serverConfigurationManager = new ServerConfigurationManager(gatewayContext);
        serverConfigurationManager.applyConfigurationWithoutRestart(new File(getAMResourceLocation()
                + File.separator +
                "configFiles/tokenTest/" +
                "api-manager.xml"));
        serverConfigurationManager.applyConfiguration(new File(getAMResourceLocation()
                + File.separator +
                "configFiles/tokenTest/" +
                "log4j.properties"));


        userManagementClient = new UserManagementClient(
                gatewayContext.getContextUrls().getBackEndUrl(),
                gatewayContext.getContextTenant().getContextUser().getUserName(),
                gatewayContext.getContextTenant().getContextUser().getPassword());

        URL url = new URL(gatewayUrls.getWebAppURLHttp());
        wireMonitorURL = "http://" + url.getHost() + ":" + hostPort;

        server = new WireMonitorServer(hostPort);
        server.setReadTimeOut(300);
        server.start();
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanup();
        serverConfigurationManager.restoreToLastConfiguration();
    }

    private void addAPI() throws Exception {

        APIPublisherRestClient apiPublisher = new APIPublisherRestClient(publisherURLHttp);

        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(wireMonitorURL));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setVisibility("public");
        apiPublisher.addAPI(apiRequest);

        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(APIName, providerName,
                APILifeCycleState.PUBLISHED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);

    }

    @Test(groups = {"wso2.am"}, description = "Enabling JWT Token generation, admin user claims", enabled = true)
    public void testEnableJWTAndClaims() throws Exception {

        RemoteUserStoreManagerServiceClient remoteUserStoreManagerServiceClient =
                new RemoteUserStoreManagerServiceClient(
                        gatewayContext.getContextUrls().getBackEndUrl(),
                        gatewayContext.getContextTenant().getContextUser().getUserName(),
                        gatewayContext.getContextTenant().getContextUser().getPassword());

        String username = gatewayContext.getContextTenant().getContextUser().getUserName();
        String profile = "default";

        remoteUserStoreManagerServiceClient.setUserClaimValue(
                username, "http://wso2.org/claims/givenname",
                "first name", profile);

        remoteUserStoreManagerServiceClient.setUserClaimValue(
                username, "http://wso2.org/claims/lastname",
                "last name", profile);

        // restart the server since updated claims not picked unless cache expired
        serverConfigurationManager.restartGracefully();

        addAPI();

        APIStoreRestClient apiStoreRestClient = new APIStoreRestClient(storeURLHttp);
        apiStoreRestClient.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());

        apiStoreRestClient.addApplication(ApplicationName, APITier, "", "this-is-test");
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName,
                storeContext.getContextTenant().getContextUser().getUserName());
        subscriptionRequest.setApplicationName(ApplicationName);
        apiStoreRestClient.subscribe(subscriptionRequest);

        GenerateAppKeyRequest generateAppKeyRequest = new GenerateAppKeyRequest(ApplicationName);
        String responseString = apiStoreRestClient.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();

        String url = gatewayUrls.getWebAppURLNhttp() + "tokenTest/1.0.0";

        APIMgtTestUtil.sendGetRequest(url, accessToken);
        String serverMessage = server.getCapturedMessage();

        String decodedJWTString = APIMgtTestUtil.getDecodedJWT(serverMessage);

        log.debug("Decoded JWTString = " + decodedJWTString);

        JSONObject jsonObject = new JSONObject(decodedJWTString);

        // check default claims
        checkDefaultUserClaims(jsonObject);

        // check user profile info claims
        String claim = jsonObject.getString("http://wso2.org/claims/givenname");
        assertTrue("JWT claim givenname  not received" + claim, claim.contains("first name"));

        claim = jsonObject.getString("http://wso2.org/claims/lastname");
        assertTrue("JWT claim lastname  not received" + claim, claim.contains("last name"));

        boolean bExceptionOccured = false;
        try {
            jsonObject.getString("http://wso2.org/claims/wrongclaim");
        } catch (JSONException e) {
            bExceptionOccured = true;
        }

        assertTrue("JWT claim invalid  claim received", bExceptionOccured);
    }

    private void checkDefaultUserClaims(JSONObject jsonObject) throws JSONException {
        String claim = jsonObject.getString("iss");
        assertTrue("JWT assertion is invalid", claim.contains("wso2.org/products/am"));

        claim = jsonObject.getString("http://wso2.org/claims/subscriber");
        assertTrue("JWT claim subscriber invalid. Received " + claim, claim.contains("admin"));

        claim = jsonObject.getString("http://wso2.org/claims/applicationname");
        assertTrue("JWT claim applicationname invalid. Received " + claim,
                claim.contains("APILifeCycleTestAPI-application"));

        claim = jsonObject.getString("http://wso2.org/claims/applicationtier");
        assertTrue("JWT claim applicationtier invalid. Received " + claim, claim.contains("Gold"));

        claim = jsonObject.getString("http://wso2.org/claims/apicontext");
        assertTrue("JWT claim apicontext invalid. Received " + claim, claim.contains("/tokenTest" + "/"
                + jsonObject.getString("http://wso2.org/claims/version")));

        claim = jsonObject.getString("http://wso2.org/claims/version");
        assertTrue("JWT claim version invalid. Received " + claim, claim.contains("1.0.0"));

        claim = jsonObject.getString("http://wso2.org/claims/tier");
        assertTrue("JWT claim tier invalid. Received " + claim, claim.contains("Gold"));

        claim = jsonObject.getString("http://wso2.org/claims/keytype");
        assertTrue("JWT claim keytype invalid. Received " + claim, claim.contains("PRODUCTION"));

        claim = jsonObject.getString("http://wso2.org/claims/usertype");
        assertTrue("JWT claim usertype invalid. Received " + claim, claim.contains("APPLICATION"));

        claim = jsonObject.getString("http://wso2.org/claims/enduserTenantId");
        assertTrue("JWT claim enduserTenantId invalid. Received " + claim, claim.contains("-1234"));

        claim = jsonObject.getString("http://wso2.org/claims/role");
        assertTrue("JWT claim role invalid. Received " + claim,
                claim.contains("admin") && claim.contains("Internal/subscriber") && claim.contains("Internal/everyone"));
    }

    @Test(groups = {"wso2.am"}, description = "Enabling JWT Token generation, specific user claims", enabled = true)
    public void testSpecificUserJWTClaims() throws Exception {

        server.setFinished(false);
        server.start();

        String subscriberUser = "subscriberUser";
        String password = "password@123";
        String accessToken;

        if ((userManagementClient != null) &&
                !userManagementClient.userNameExists("Internal/subscriber", subscriberUser)) {
            userManagementClient.addUser(subscriberUser, password,
                    new String[]{"Internal/subscriber"}, null);
        }

        RemoteUserStoreManagerServiceClient remoteUserStoreManagerServiceClient =
                new RemoteUserStoreManagerServiceClient(
                        gatewayContext.getContextUrls().getBackEndUrl(),
                        gatewayContext.getContextTenant().getContextUser().getUserName(),
                        gatewayContext.getContextTenant().getContextUser().getPassword());

        String profile = "default";

        remoteUserStoreManagerServiceClient.setUserClaimValue(
                subscriberUser, "http://wso2.org/claims/givenname",
                "subscriberUser name", profile);

        remoteUserStoreManagerServiceClient.setUserClaimValue(
                subscriberUser, "http://wso2.org/claims/lastname",
                "subscriberUser name", profile);

        // restart the server since updated claims not picked unless cache expired
        serverConfigurationManager.restartGracefully();
        super.init();

        APIStoreRestClient apiStoreRestClient = new APIStoreRestClient(storeURLHttp);
        apiStoreRestClient.login(subscriberUser, password);

        apiStoreRestClient.addApplication(ApplicationName, APITier, "", "this-is-test");
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName,
                providerName);
        subscriptionRequest.setApplicationName(ApplicationName);
        apiStoreRestClient.subscribe(subscriptionRequest);

        GenerateAppKeyRequest generateAppKeyRequest = new GenerateAppKeyRequest(ApplicationName);
        String responseString = apiStoreRestClient.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();

        String url = gatewayUrls.getWebAppURLNhttp() + "tokenTest/1.0.0/";

        APIMgtTestUtil.sendGetRequest(url, accessToken);
        String serverMessage = server.getCapturedMessage();

        Assert.assertTrue(serverMessage.contains("X-JWT-Assertion"), "JWT assertion not in the header");

        String decodedJWTString = APIMgtTestUtil.getDecodedJWT(serverMessage);

        log.debug("Decoded JWTString = " + decodedJWTString);

        JSONObject jsonObject = new JSONObject(decodedJWTString);

        // check claims
        String claim = jsonObject.getString("iss");
        assertTrue("JWT assertion is invalid", claim.contains("wso2.org/products/am"));

        claim = jsonObject.getString("http://wso2.org/claims/subscriber");
        assertTrue("JWT claim subscriber invalid. Received " + claim, claim.contains("subscriberUser"));

        claim = jsonObject.getString("http://wso2.org/claims/applicationname");
        assertTrue("JWT claim applicationname invalid. Received " + claim,
                claim.contains("APILifeCycleTestAPI-application"));

    }


    @Test(groups = {"wso2.am"}, description = "Enabling JWT Token generation, tenant user claims", enabled = false)
    public void testTenantUserJWTClaims() throws Exception {

        server.setFinished(false);
        server.start();

        serverConfigurationManager.restartGracefully();
        super.init();

        String provider = "admin-AT-wso2.com";
        String tenantUser = "admin@wso2.com";
        String password = "wso2@123";
        String accessToken;

        APIPublisherRestClient apiPublisherRestClient = new APIPublisherRestClient(publisherURLHttp);

        apiPublisherRestClient.login(tenantUser, password);

        APIRequest apiRequest = new APIRequest(APIName, APIContext, new URL(wireMonitorURL));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(APIVersion);
        apiRequest.setWsdl("https://svn.wso2.org/repos/wso2/carbon/platform/trunk/products" +
                "/bps/modules/samples/product/src/main/resources/bpel/2.0/MyRoleMexTestProcess/echo.wsdl");
        apiRequest.setVisibility("public");

        apiRequest.setProvider(provider);
        apiPublisherRestClient.addAPI(apiRequest);

        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(APIName, provider,
                APILifeCycleState.PUBLISHED);
        apiPublisherRestClient.changeAPILifeCycleStatus(updateRequest);

        APIStoreRestClient apiStoreRestClient = new APIStoreRestClient(storeURLHttp);
        apiStoreRestClient.login(tenantUser, password);

        apiStoreRestClient.addApplication(ApplicationName, APITier, "", "this-is-test");
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(APIName,
                provider);
        subscriptionRequest.setApplicationName(ApplicationName);
        apiStoreRestClient.subscribe(subscriptionRequest);

        GenerateAppKeyRequest generateAppKeyRequest = new GenerateAppKeyRequest(ApplicationName);
        String responseString = apiStoreRestClient.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();

        String url = gatewayUrls.getWebAppURLNhttp() + "t/wso2.com/tokenTest/1.0.0/";
        APIMgtTestUtil.sendGetRequest(url, accessToken);
        String serverMessage = server.getCapturedMessage();

        String decodedJWTString = APIMgtTestUtil.getDecodedJWT(serverMessage);

        JSONObject jsonObject = new JSONObject(decodedJWTString);

        log.debug("Decoded JWTString = " + decodedJWTString);
        // check claims
        String claim = jsonObject.getString("iss");
        assertTrue("JWT assertion is invalid", claim.contains("wso2.org/products/am"));

        claim = jsonObject.getString("http://wso2.org/claims/subscriber");
        assertTrue("JWT claim subscriber invalid. Received " + claim, claim.contains("admin@wso2.com"));

        claim = jsonObject.getString("http://wso2.org/claims/apicontext");
        assertTrue("JWT claim apicontext invalid. Received " + claim, claim.contains("/t/wso2.com/tokenTest"));

    }
}
