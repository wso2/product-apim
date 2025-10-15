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

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationListDTO;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.Response;

import static org.testng.Assert.*;

public class APIScopeTestCase extends APIManagerLifecycleBaseTest {

    private static final Log log = LogFactory.getLog(APIScopeTestCase.class);
    private UserManagementClient userManagementClient1 = null;
    private static final String API_NAME = "APIScopeTestAPI";
    private static final String API_VERSION = "1.0.0";
    private static final String APP_NAME = "NewApplication";
    private String USER_SMITH = "smith";
    private String ADMIN_ROLE = "admin";
    private String INTERNAL_CREATOR_USER = "UserNew1";
    private String INTERNAL_CREATOR_PUBLISHER_USER = "UserNew2";

    private static final String SUBSCRIBER_ROLE = "subscriber";
    private final String INTERNAL_ROLE_SUBSCRIBER = "Internal/subscriber";
    private final String INTERNAL_ROLE_CREATOR = "Internal/creator";
    private final String API_VERSION_WITH_SCOPE = "1.0.0";
    private final String API_VERSION_WITH_SCOPE_COPY = "2.0.0";
    private final String API_NAME_WITH_SCOPE = "APIScopeTestWithScopeName";
    private final String API_CONTEXT_WITH_SCOPE = "APIScopeTestWithScopeContext";
    private final String SCOPE_NAME = "APIScopeUpdateCopyScope";
    private final String ALLOWED_ROLE = "admin";
    private final String[] ADMIN_PERMISSIONS = { "/permission/admin/login", "/permission/admin/manage",
            "/permission/admin/configure", "/permission/admin/monitor" };
    private final String[] NEW_ROLE_LIST = { "Internal/publisher", "Internal/creator",
            "Internal/subscriber", "Internal/everyone", "admin" };
    private final String[] OLD_ROLE_LIST = { "Internal/publisher", "Internal/creator",
            "Internal/subscriber", "Internal/everyone", "role1" };
    private final String[] SEC_OLD_ROLE_LIST = { "Internal/publisher", "Internal/creator",
            "Internal/subscriber", "Internal/everyone"};
    private final String[] INTERNAL_CREATOR_PUBLISHER_USER_ROLE_LIST = { "Internal/publisher", "Internal/creator"};
    private String apiId;
    private String apiIdWithScope;
    private String copyApiId;
    private String applicationId;
    private ArrayList<String> grantTypes;

    @Factory(dataProvider = "userModeDataProvider")
    public APIScopeTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        grantTypes = new ArrayList<>();
    }

    @Test(groups = {"wso2.am"}, description = "Testing the scopes with admin, subscriber roles")
    public void testSetScopeToResourceTestCase() throws Exception {
        userManagementClient1 = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                keyManagerContext.getContextTenant().getTenantAdmin().getUserName(),
                keyManagerContext.getContextTenant().getTenantAdmin().getPassword());
        // crating user smith
        String userSmith;
        String user1;
        String user2;
        String gatewayUrl;

        if (TestUserMode.SUPER_TENANT_USER_STORE_USER.equals(userMode)) {
            USER_SMITH = APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + USER_SMITH;
            ADMIN_ROLE = APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + ADMIN_ROLE;
            INTERNAL_CREATOR_USER = APIMIntegrationConstants.SECONDARY_USER_STORE + "/" + INTERNAL_CREATOR_USER;
            INTERNAL_CREATOR_PUBLISHER_USER = APIMIntegrationConstants.SECONDARY_USER_STORE + "/"
                    + INTERNAL_CREATOR_PUBLISHER_USER;
            userManagementClient1.addRole(ADMIN_ROLE, new String[]{user.getUserNameWithoutDomain()}, ADMIN_PERMISSIONS);
        }

        //Updating the email username with admin role
        if (TestUserMode.SUPER_TENANT_EMAIL_USER.equals(userMode) || TestUserMode.TENANT_EMAIL_USER.equals(userMode)) {
            userManagementClient1.updateRolesOfUser(user.getUserNameWithoutDomain(), NEW_ROLE_LIST);
        }

        if (keyManagerContext.getContextTenant().getDomain().equals("carbon.super")) {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp();
            userSmith = USER_SMITH;
            user1 = INTERNAL_CREATOR_USER;
            user2 = INTERNAL_CREATOR_PUBLISHER_USER;
        } else {
            gatewayUrl =
                    gatewayUrlsWrk.getWebAppURLNhttp() + "t/" + keyManagerContext.getContextTenant().getDomain() + "/";
            userSmith = USER_SMITH + "@" + keyManagerContext.getContextTenant().getDomain();
            user1 = INTERNAL_CREATOR_USER + "@" + keyManagerContext.getContextTenant().getDomain();
            user2 = INTERNAL_CREATOR_PUBLISHER_USER + "@" + keyManagerContext.getContextTenant().getDomain();
        }

        userManagementClient1.addUser(USER_SMITH, "john123", new String[]{INTERNAL_ROLE_SUBSCRIBER}, USER_SMITH);
        userManagementClient1.addUser(INTERNAL_CREATOR_USER, "john123", new String[]{INTERNAL_ROLE_CREATOR}, INTERNAL_CREATOR_USER);
        userManagementClient1.addUser(INTERNAL_CREATOR_PUBLISHER_USER, "john123", INTERNAL_CREATOR_PUBLISHER_USER_ROLE_LIST, INTERNAL_CREATOR_PUBLISHER_USER);

        restAPIPublisher = new RestAPIPublisherImpl(
                publisherContext.getContextTenant().getContextUser().getUserNameWithoutDomain(),
                publisherContext.getContextTenant().getContextUser().getPassword(),
                publisherContext.getContextTenant().getDomain(), publisherURLHttps);
        restAPIStore =
                new RestAPIStoreImpl(storeContext.getContextTenant().getContextUser().getUserNameWithoutDomain(),
                        storeContext.getContextTenant().getContextUser().getPassword(),
                        storeContext.getContextTenant().getDomain(), storeURLHttps);

        // Adding API
        String apiContext = "testScopeAPI";
        String tags = "thomas-bayer, testing, rest-Apis";
        String url = getGatewayURLNhttp() + "response";
        String description = "This is a test API created by API manager integration test";

        APIRequest apiRequest = new APIRequest(API_NAME, apiContext, new URL(url));
        apiRequest.setTags(tags);
        apiRequest.setDescription(description);
        apiRequest.setVersion(API_VERSION);
        apiRequest.setProvider(user.getUserName());

        apiId = createAndPublishAPIUsingRest(apiRequest, restAPIPublisher, false);

        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);

        //resources are modified using swagger doc.
        // admin_scope(used for POST) :- admin
        // user_scope (used for GET) :- admin,subscriber
        String modifiedResource =
                "{\"paths\":{ \"/*\":{\"put\":{ \"responses\":{\"200\":{ \"description\": \"Successful\" }},\"x-auth-type\":\"Application User\","
                        + "\"x-throttling-tier\":\"Unlimited\" },\"post\":{ \"responses\":{\"200\":{ \"description\": \"Successful\" }},\"x-auth-type\":\"Application User\","
                        + "\"x-throttling-tier\":\"Unlimited\",\"x-scope\":\"admin_scope\"},\"get\":{ \"responses\":{\"200\":{ \"description\": \"Successful\" }},\"x-auth-type\":\"Application User\","
                        + "\"x-throttling-tier\":\"Unlimited\",\"x-scope\":\"user_scope\"},\"delete\":{ \"responses\":{\"200\":{ \"description\": \"Successful\" }},\"x-auth-type\":\"Application User\","
                        + "\"x-throttling-tier\":\"Unlimited\"},\"options\":{ \"responses\":{\"200\":{ \"description\": \"Successful\" }},\"x-auth-type\":\"None\","
                        + "\"x-throttling-tier\":\"Unlimited\"}}},\"swagger\":\"2.0\",\"info\":{\"title\":\"APIScopeTestAPI\",\"version\":\"1.0.0\"},"
                        + "\"x-wso2-security\":{\"apim\":{\"x-wso2-scopes\":[{\"name\":\"admin_scope\",\"description\":\"\",\"key\":\"admin_scope\",\"roles\":\""
                        + ADMIN_ROLE + "\"},"
                        + "{\"name\":\"user_scope\",\"description\":\"\",\"key\":\"user_scope\",\"roles\":\""
                        + ADMIN_ROLE + ",Internal/subscriber\"}]}}}";


        restAPIPublisher.updateSwagger(apiId, modifiedResource);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);

        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION, APIMIntegrationConstants.IS_API_NOT_EXISTS);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);

        // For Admin user
        // create new application and subscribing
        //add an application
        HttpResponse applicationResponse = restAPIStore.createApplication(APP_NAME,
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();

        String provider = user.getUserName();
        //subscribe to the api
        HttpResponse subscribeResponse = subscribeToAPIUsingRest(apiId, applicationId,
                APIMIntegrationConstants.API_TIER.GOLD, restAPIStore);
        assertEquals(subscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of old API version request not successful " +
                        " API Name:" + API_NAME + " API Version:" + API_VERSION +
                        " API Provider Name :" + provider);

        //Generate production token and invoke with that
        //get access token
        ArrayList<String> scopes = new ArrayList<>();
        scopes.add("admin_scope");
        scopes.add("user_scope");
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, scopes, grantTypes);

        // get Consumer Key and Consumer Secret
        String consumerKey = applicationKeyDTO.getConsumerKey();
        String consumerSecret = applicationKeyDTO.getConsumerSecret();

        URL tokenEndpointURL = new URL(keyManagerHTTPSURL + "oauth2/token");
        String accessToken;
        Map<String, String> requestHeaders;
        HttpResponse response;
        URL endPointURL;
        String requestBody;
        JSONObject accessTokenGenerationResponse;

        //Obtain user access token for Admin
        requestBody = "grant_type=password&username=" + user.getUserName() +
                "&password=" + user.getPassword() +
                "&scope=admin_scope user_scope";

        response = restAPIStore.generateUserAccessKey(consumerKey, consumerSecret,
                                                  requestBody, tokenEndpointURL);
        accessTokenGenerationResponse = new JSONObject(response.getData());
        accessToken = accessTokenGenerationResponse.getString("access_token");

        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        response = HttpRequestUtil.doGet(gatewayUrl + "testScopeAPI/1.0.0/test", requestHeaders);

        assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                     "Admin user cannot access the GET Method");

        // Accessing POST method
        endPointURL = new URL(gatewayUrl + "testScopeAPI/1.0.0/test");
        response = HttpRequestUtil.doPost(endPointURL, "", requestHeaders);
        assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                     "Admin user cannot access the POST Method");

        //Obtain user access token for Admin without scopes
        requestBody = "grant_type=password&username=" + user.getUserName() +
                "&password=" + user.getPassword();

        response = restAPIStore.generateUserAccessKey(consumerKey, consumerSecret,
                requestBody, tokenEndpointURL);
        accessTokenGenerationResponse = new JSONObject(response.getData());
        accessToken = accessTokenGenerationResponse.getString("access_token");
        Assert.assertTrue(accessTokenGenerationResponse.getString("scope").contains("default"));
        Assert.assertFalse(accessTokenGenerationResponse.getString("scope").contains("user_scope"));
        Assert.assertFalse(accessTokenGenerationResponse.getString("scope").contains("admin_scope"));
        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        response = HttpRequestUtil.doGet(gatewayUrl + "testScopeAPI/1.0.0/test", requestHeaders);

        assertEquals(response.getResponseCode(), Response.Status.FORBIDDEN.getStatusCode(),
                "Admin user can access the GET Method without scope");

        try {
            // Accessing POST method
            endPointURL = new URL(gatewayUrl + "testScopeAPI/1.0.0/test");
            response = HttpRequestUtil.doPost(endPointURL, "", requestHeaders);
            assertTrue(response.getResponseCode() != Response.Status.OK.getStatusCode(),
                    "Admin user can access the POST Method");
        } catch (Exception e) {
            log.error("user Admin cannot access the resources (expected behaviour)");
            assertTrue(true, "user admin cannot access the resources");
        }

        //Obtaining user access token for john
        requestBody = "grant_type=password&username=" + userSmith + "&password=john123&scope=admin_scope user_scope";
        accessTokenGenerationResponse = new JSONObject(
                restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL)
                        .getData());
        accessToken = accessTokenGenerationResponse.getString("access_token");

        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        // Accessing GET method
        response = HttpRequestUtil.doGet(gatewayUrl + "testScopeAPI/1.0.0/test", requestHeaders);
        assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                     "User John cannot access the GET Method");

        try {
            // Accessing POST method
            endPointURL = new URL(gatewayUrl + "testScopeAPI/1.0.0/test");
            response = HttpRequestUtil.doPost(endPointURL, "", requestHeaders);
            assertTrue(response.getResponseCode() != Response.Status.OK.getStatusCode(),
                       "testRole John can access the POST Method");

        } catch (Exception e) {
            log.error("user john cannot access the resources (expected behaviour)");
            assertTrue(true, "user john cannot access the resources");
        }

        //Obtaining user access token for john without scope
        requestBody = "grant_type=password&username=" + userSmith + "&password=john123";
        accessTokenGenerationResponse = new JSONObject(
                restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL)
                        .getData());
        accessToken = accessTokenGenerationResponse.getString("access_token");
        Assert.assertTrue(accessTokenGenerationResponse.getString("scope").contains("default"));
        Assert.assertFalse(accessTokenGenerationResponse.getString("scope").contains("user_scope"));
        Assert.assertFalse(accessTokenGenerationResponse.getString("scope").contains("admin_scope"));
        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        // Accessing GET method
        response = HttpRequestUtil.doGet(gatewayUrl + "testScopeAPI/1.0.0/test", requestHeaders);
        assertEquals(response.getResponseCode(), Response.Status.FORBIDDEN.getStatusCode(),
                "User John can access the GET Method without scopes");

        // Contains both Publisher and Store REST API related scopes
        String fullScopes = "openid apim:api_view apim:api_create apim:api_delete apim:api_publish " +
                "apim:subscription_view apim:subscription_block apim:external_services_discover " +
                "apim:threat_protection_policy_create apim:threat_protection_policy_manage " +
                "apim:document_create apim:document_manage apim:mediation_policy_view " +
                "apim:mediation_policy_create apim:mediation_policy_manage " +
                "apim:client_certificates_view apim:client_certificates_add " +
                "apim:client_certificates_update apim:ep_certificates_view " +
                "apim:ep_certificates_add apim:ep_certificates_update apim:publisher_settings " +
                "apim:pub_alert_manage apim:shared_scope_manage apim:api_generate_key apim:comment_view " +
                "apim:comment_write apim:subscribe apim:app_update apim:app_manage apim:sub_manage " +
                "apim:self-signup apim:dedicated_gateway apim:store_settings apim:api_key";

        //Obtaining user access token for john(consumer) with all scopes
        requestBody = "grant_type=password&username=" + userSmith + "&password=john123&scope=" + fullScopes;
        accessTokenGenerationResponse = new JSONObject(
                restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL)
                        .getData());
        accessTokenGenerationResponse.getString("access_token");
        Assert.assertTrue(accessTokenGenerationResponse.getString("scope").contains("openid"));
        Assert.assertTrue(accessTokenGenerationResponse.getString("scope").contains("apim:subscribe"));
        Assert.assertTrue(accessTokenGenerationResponse.getString("scope").contains("apim:app_update"));
        Assert.assertFalse(accessTokenGenerationResponse.getString("scope").contains("apim:api_create"));
        Assert.assertFalse(accessTokenGenerationResponse.getString("scope").contains("apim:api_delete"));
        Assert.assertFalse(accessTokenGenerationResponse.getString("scope").contains("apim:api_publish"));

        //Obtaining user access token for user1(creator) with full Scopes
        requestBody = "grant_type=password&username=" + user1 + "&password=john123&scope=" + fullScopes;
        accessTokenGenerationResponse = new JSONObject(
                restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL)
                        .getData());
        accessTokenGenerationResponse.getString("access_token");
        Assert.assertTrue(accessTokenGenerationResponse.getString("scope").contains("apim:api_create"));
        Assert.assertFalse(accessTokenGenerationResponse.getString("scope").contains("apim:api_publish"));
        Assert.assertFalse(accessTokenGenerationResponse.getString("scope").contains("apim:subscribe"));
        Assert.assertFalse(accessTokenGenerationResponse.getString("scope").contains("apim:app_update"));

        //Obtaining user access token for user2(creator+publisher) with full scopes
        requestBody = "grant_type=password&username=" + user2 + "&password=john123&scope=" + fullScopes;
        accessTokenGenerationResponse = new JSONObject(
                restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL)
                        .getData());
        accessTokenGenerationResponse.getString("access_token");
        Assert.assertTrue(accessTokenGenerationResponse.getString("scope").contains("apim:api_create"));
        Assert.assertTrue(accessTokenGenerationResponse.getString("scope").contains("apim:api_publish"));
        Assert.assertFalse(accessTokenGenerationResponse.getString("scope").contains("apim:subscribe"));
        Assert.assertFalse(accessTokenGenerationResponse.getString("scope").contains("apim:app_update"));
    }

    @Test(groups = { "wso2.am" }, description = "Testing Copy api with scopes assigned",
            dependsOnMethods = "testSetScopeToResourceTestCase")
    public void testCopyApiWithScopes() throws Exception {
        String tierCollection = APIMIntegrationConstants.API_TIER.UNLIMITED;
        String endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api";
        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(API_NAME_WITH_SCOPE,
                API_CONTEXT_WITH_SCOPE, API_VERSION_WITH_SCOPE, user.getUserName(), new URL(endpointUrl));
        apiCreationRequestBean.setTiersCollection(tierCollection);

        //adding resources using swagger
        String swagger = "{" + "\"paths\": {" + "\"/add\": {" + "\"get\": {" + "\"x-auth-type\": \"" + URLEncoder
                .encode(APIMIntegrationConstants.RESOURCE_AUTH_TYPE_APPLICATION_AND_APPLICATION_USER, "UTF-8") + "\","
                + "\"x-throttling-tier\": \"" + APIMIntegrationConstants.API_TIER.UNLIMITED + "\"," + "\"x-scope\": \""
                + SCOPE_NAME + "\"," + "\"responses\": {" + "\"200\": { \"description\": \"Successful\" }" + "},"
                + "\"parameters\": [{" + "\"name\": \"x\"," + "\"required\": false,"
                + "\"type\": \"string\"," + "\"description\": \"First value\"," + "\"in\": \"query\"" + "}, {"
                + "\"name\": \"y\"," + "\"required\": false," + "\"type\": \"string\","
                + "\"description\": \"Second Value\"," + "\"in\": \"query\"" + "}]" + "}" + "}" + "},"
                + "\"swagger\": \"2.0\"," + "\"x-wso2-security\": {" + "\"apim\": {" + "\"x-wso2-scopes\": [{"
                + "\"description\": \"Sample Scope\"," + "\"name\": \"" + SCOPE_NAME + "\"," + "\"roles\": \""
                + ALLOWED_ROLE + "\"," + "\"key\": \"" + SCOPE_NAME + "\"" + "}]" + "}" + "}," + "\"info\": {"
                + "\"title\": \"" + API_NAME_WITH_SCOPE + "\"," + "\"version\": " + "\"" + API_VERSION + "\""
                + "}" + "}";
        apiCreationRequestBean.setSwagger(swagger);

        //add test api and publish
        APIDTO apiDto = createAndPublishAPI(apiCreationRequestBean, restAPIPublisher, false);
        apiIdWithScope = apiDto.getId();

        restAPIPublisher.updateSwagger(apiIdWithScope, swagger);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiIdWithScope, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME_WITH_SCOPE, API_VERSION_WITH_SCOPE,
                APIMIntegrationConstants.IS_API_NOT_EXISTS);
        waitForAPIDeploymentSync(user.getUserName(), API_NAME_WITH_SCOPE, API_VERSION_WITH_SCOPE,
                APIMIntegrationConstants.IS_API_EXISTS);

        //copy published api
        HttpResponse newVersionResponse = restAPIPublisher.copyAPI(API_VERSION_WITH_SCOPE_COPY, apiIdWithScope, null);
        assertEquals(newVersionResponse.getResponseCode(), HttpStatus.SC_OK, "Response Code Mismatch");
        copyApiId = newVersionResponse.getData();
        waitForAPIDeploymentSync(user.getUserName(), API_NAME_WITH_SCOPE, API_VERSION_WITH_SCOPE_COPY,
                APIMIntegrationConstants.IS_API_EXISTS);

    }

    @Test(groups = { "wso2.am" }, description = "Testing Update api with scopes assigned",
            dependsOnMethods = "testCopyApiWithScopes")
    public void testUpdateApiWithScopes() throws Exception {
        String tierCollection = APIMIntegrationConstants.API_TIER.GOLD;
        String endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api/add";

        APIRequest apiRequest = new APIRequest(API_NAME_WITH_SCOPE, API_CONTEXT_WITH_SCOPE, new URL(endpointUrl));
        apiRequest.setTiersCollection(tierCollection);
        apiRequest.setVersion(API_VERSION_WITH_SCOPE);
        apiRequest.setDescription("test api description");

        HttpResponse updateResponse = restAPIPublisher.updateAPI(apiRequest, apiIdWithScope);
        assertEquals(updateResponse.getResponseCode(), HttpStatus.SC_OK, "Response Code Mismatch");
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiIdWithScope, restAPIPublisher);
    }

    @Test(groups = { "wso2.am" }, description = "Testing REST API scopes",
            dependsOnMethods = "testUpdateApiWithScopes")
    public void testRESTAPIScopes() throws Exception {

        //Generate a token with subscriber user for Devportal Access
        RestAPIStoreImpl restAPIStoreUserSmith =
                new RestAPIStoreImpl(USER_SMITH,
                        "john123",
                        storeContext.getContextTenant().getDomain(), storeURLHttps);
        Assert.assertNotNull(restAPIStoreUserSmith.getAccessToken());

        //Generate a token with creator user for Publisher Access
        RestAPIPublisherImpl restAPIPublisherUser1 = new RestAPIPublisherImpl(
                INTERNAL_CREATOR_USER,
                "john123",
                publisherContext.getContextTenant().getDomain(), publisherURLHttps);
        Assert.assertNotNull(restAPIPublisherUser1.getAccessToken());

        //Generate a token with creator/publisher user for Publisher Access
        RestAPIPublisherImpl restAPIPublisherUser2 = new RestAPIPublisherImpl(
                INTERNAL_CREATOR_PUBLISHER_USER,
                "john123",
                publisherContext.getContextTenant().getDomain(), publisherURLHttps);
        Assert.assertNotNull(restAPIPublisherUser2.getAccessToken());

        //Generate a token with subscriber user for Publisher Access
        RestAPIPublisherImpl restAPIPublisherNegative = new RestAPIPublisherImpl(
                USER_SMITH,
                "john123",
                publisherContext.getContextTenant().getDomain(), publisherURLHttps);
        Assert.assertNotNull(restAPIPublisherNegative.getAccessToken());
        try {
            restAPIPublisherNegative.apIsApi.getAllAPIs(null, null, keyManagerContext.getContextTenant().getDomain(),
                    null, null, null, null, null);
            Assert.assertTrue(false);
        } catch (ApiException e) {
            Assert.assertTrue(true);
        }

        //Generate a token with creator user for Devportal Access
        RestAPIStoreImpl restAPIStoreUserNegative =
                new RestAPIStoreImpl(INTERNAL_CREATOR_USER,
                        "john123",
                        storeContext.getContextTenant().getDomain(), storeURLHttps);
        Assert.assertNotNull(restAPIStoreUserNegative.getAccessToken());
        try {
            ApplicationListDTO responseData = restAPIStoreUserNegative.getAllApps();
            Assert.fail();
        } catch (org.wso2.am.integration.clients.store.api.ApiException e) {
            Assert.assertTrue(true);
        }

        //Generate a token with creator/publisher user for Devportal Access
        RestAPIStoreImpl restAPIStoreUserNegative2 =
                new RestAPIStoreImpl(INTERNAL_CREATOR_PUBLISHER_USER,
                        "john123",
                        storeContext.getContextTenant().getDomain(), storeURLHttps);
        Assert.assertNotNull(restAPIStoreUserNegative2.getAccessToken());
        try {
            ApplicationListDTO responseData = restAPIStoreUserNegative2.getAllApps();
            Assert.fail();
        } catch (org.wso2.am.integration.clients.store.api.ApiException e) {
            Assert.assertTrue(true);
        }


    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(apiIdWithScope, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId);
        restAPIPublisher.deleteAPI(apiIdWithScope);
        restAPIPublisher.deleteAPI(copyApiId);

        //Reverting back the roles of email users
        if (TestUserMode.SUPER_TENANT_EMAIL_USER.equals(userMode) || TestUserMode.TENANT_EMAIL_USER.equals(userMode)) {
            userManagementClient1.updateRolesOfUser(user.getUserNameWithoutDomain(), OLD_ROLE_LIST);
        }

        if (userManagementClient1 != null) {
            if (TestUserMode.SUPER_TENANT_USER_STORE_USER.equals(userMode)) {
                userManagementClient1.updateRolesOfUser(user.getUserNameWithoutDomain(), SEC_OLD_ROLE_LIST);
            }
            userManagementClient1.deleteUser(USER_SMITH);
            userManagementClient1.deleteUser(INTERNAL_CREATOR_USER);
            userManagementClient1.deleteUser(INTERNAL_CREATOR_PUBLISHER_USER);
        }
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
                new Object[] { TestUserMode.SUPER_TENANT_USER_STORE_USER },
                new Object[] { TestUserMode.SUPER_TENANT_EMAIL_USER },
                new Object[] { TestUserMode.TENANT_EMAIL_USER },
        };
    }
}

