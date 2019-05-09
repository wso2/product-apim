/*
*  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.am.integration.tests.restapi;

import org.apache.axiom.util.base64.Base64Utils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.exceptions.AutomationFrameworkException;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;

public class ContentSearchTestCase extends APIMIntegrationBaseTest {
    private Log log  = LogFactory.getLog(ContentSearchTestCase.class);
    private String storeRestAPIBasePath = "api/am/store/v0.14/";
    private String publisherRestAPIBasePath = "api/am/publisher/v0.14/";
    private URL tokenApiUrl;
    private String endpointURL = "http://gdata.youtube.com/feeds/api/standardfeeds";
    private String version = "1.0.0";
    private int retries = 10; //because indexing needs time, we are retrying api calls at an interval of 3s

    @Factory(dataProvider = "userModeDataProvider") public ContentSearchTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }, };
    }

    @BeforeClass(alwaysRun = true) public void setEnvironment() throws Exception {
        super.init(userMode);
        tokenApiUrl = new URL(getKeyManagerURLHttps() + "oauth2/token");
    }

    @Test(groups = { "wso2.am" }, description = "Test basic content Search") public void testBasicContentSearch()
            throws Exception {
        log.info("Basic Content Search");
        String contentSearchTestAPI = "contentSearchTestAPI";
        String description = "Unified Search Feature";
        String responseString;
        APIRequest apiRequest = createAPIRequest(contentSearchTestAPI, contentSearchTestAPI, endpointURL, version,
                user.getUserName(), description);
        apiPublisher.login(user.getUserName(), user.getPassword());
        apiPublisher.addAPI(apiRequest);
        APIIdentifier apiIdentifier = new APIIdentifier(user.getUserName(), contentSearchTestAPI, version);
        apiPublisher.changeAPILifeCycleStatusToPublish(apiIdentifier, false);

        HttpClient client = HTTPSClientUtils.getHttpsClient();
        String accessToken = getAccessToken("publisher_client", user.getUserName(), user.getPassword());

        //check in publisher
        for (int i = 0; i <= retries; i++) {
            HttpGet getPublisherAPIs = new HttpGet(
                    getPublisherURLHttps() + publisherRestAPIBasePath + "search?query=" + URLEncoder
                            .encode(description, "UTF-8"));
            getPublisherAPIs.setHeader("Authorization", "Bearer " + accessToken);
            HttpResponse publisherResponse = client.execute(getPublisherAPIs);
            responseString = getResponseBody(publisherResponse);
            if (getResultCount(responseString) == 1) {
                Assert.assertTrue(true);
                break;
            } else {
                if (i == retries) {
                    Assert.fail("Basic content search in publisher failed. Received response : " + responseString);
                } else {
                    log.warn("Basic content search in publisher failed. Received response : " + responseString + " Retrying...");
                    Thread.sleep(5000);
                }
            }
        }

        for (int i = 0; i <= retries; i++) {
            HttpGet getStoreAPIs = new HttpGet(getStoreURLHttps() + storeRestAPIBasePath + "search?query=" + URLEncoder
                    .encode(description, "UTF-8"));
            if (TestUserMode.TENANT_ADMIN == userMode) {
                getStoreAPIs.setHeader("X-WSO2-Tenant", user.getUserDomain());
            }

            //search term : UnifiedSearchFeature, created api has this in description filed
            getStoreAPIs.setHeader("Authorization", "Bearer " + accessToken);
            HttpResponse storeResponse = client.execute(getStoreAPIs);
            responseString = getResponseBody(storeResponse);
            if (getResultCount(responseString) == 1) {
                Assert.assertTrue(true);
                break;
            } else {
                if (i == retries) {
                    Assert.fail("Basic content search in store failed. Received response : " + responseString);
                } else {
                    log.warn("Basic content search in store failed. Received response : " + responseString + " Retrying...");
                    Thread.sleep(5000);
                }
            }
        }

        //change status to create and check whether it is accessible from store
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(contentSearchTestAPI, user.getUserName(),
                APILifeCycleState.CREATED);
        apiPublisher.changeAPILifeCycleStatus(updateRequest);
        for (int i = 0; i <= retries; i++) {
            HttpGet getStoreAPIs = new HttpGet(getStoreURLHttps() + storeRestAPIBasePath + "search?query=" + URLEncoder
                    .encode(description, "UTF-8"));
            if (TestUserMode.TENANT_ADMIN == userMode) {
                getStoreAPIs.setHeader("X-WSO2-Tenant", user.getUserDomain());
            }

            //search term : UnifiedSearchFeature, created api has this in description filed
            getStoreAPIs.setHeader("Authorization", "Bearer " + accessToken);
            HttpResponse storeResponse = client.execute(getStoreAPIs);
            responseString = getResponseBody(storeResponse);
            if (getResultCount(responseString) == 0) {
                Assert.assertTrue(true);
                break;
            } else {
                if (i == retries) {
                    Assert.fail("Basic content search in store failed. 0 results expected. Received response : "
                            + responseString);
                } else {
                    log.warn("Basic content search in store failed. 0 results expected. Received response : "
                            + responseString + " Retrying...");
                    Thread.sleep(5000);
                }
            }
        }

        //clear
        apiPublisher.deleteAPI(contentSearchTestAPI, version, user.getUserName());
        Thread.sleep(5000);
    }

    @Test(groups = { "wso2.am" }, description = "Test document content Search") public void testDocumentContentSearch()
            throws Exception {
        log.info("Document Content Search");
        String apiName = "contentSearchTestAPIWithDocument";
        String apiContext = "/contentSearchTestAPIWithDocument";
        String provider = user.getUserName();
        String documentName = "Test-Document";
        String documentContent = "This is a sample API to test unified search feature - github4156";
        String responseString;
        APIRequest apiRequest = createAPIRequest(apiName, apiContext, endpointURL, version, provider, "");
        apiPublisher.login(user.getUserName(), user.getPassword());
        apiPublisher.addAPI(apiRequest);
        APIIdentifier apiIdentifier = new APIIdentifier(provider, apiName, version);
        apiPublisher.changeAPILifeCycleStatusToPublish(apiIdentifier, false);
        apiPublisher.addDocument(apiName, version, provider, documentName,
                APIMIntegrationConstants.API_DOCUMENT_TYPE_HOW_TO, APIMIntegrationConstants.API_DOCUMENT_SOURCE_INLINE,
                "", "document summary", "");
        apiPublisher.updateDocument(apiName, version, user.getUserName(), documentName, documentContent);

        HttpClient client = HTTPSClientUtils.getHttpsClient();
        String accessToken = getAccessToken("publisher_client", user.getUserName(), user.getPassword());

        //check in publisher
        for (int i = 0; i <= retries; i++) {
            HttpGet getPublisherAPIs = new HttpGet(
                    getPublisherURLHttps() + publisherRestAPIBasePath + "search?query=github4156");
            getPublisherAPIs.setHeader("Authorization", "Bearer " + accessToken);
            HttpResponse publisherResponse = client.execute(getPublisherAPIs);
            responseString = getResponseBody(publisherResponse);
            if (getResultCount(responseString) == 1) {
                Assert.assertTrue(true);
                break;
            } else {
                if (i == retries) {
                    Assert.fail("Document content search in store failed. 1 result expected. Received response : "
                            + responseString);
                } else {
                    log.warn("Document content search in store failed. 1 results expected. Received response : "
                            + responseString + " Retrying...");
                    Thread.sleep(5000);
                }
            }
        }

        //check in store
        for (int i = 0; i <= retries; i++) {
            HttpGet getStoreAPIs = new HttpGet(getStoreURLHttps() + storeRestAPIBasePath + "search?query=github4156");
            if (TestUserMode.TENANT_ADMIN == userMode) {
                getStoreAPIs.setHeader("X-WSO2-Tenant", user.getUserDomain());
            }
            getStoreAPIs.setHeader("Authorization", "Bearer " + accessToken);
            HttpResponse storeResponse = client.execute(getStoreAPIs);
            responseString = getResponseBody(storeResponse);
            if (getResultCount(responseString) == 1) {
                Assert.assertTrue(true);
                break;
            } else {
                if (i == retries) {
                    Assert.fail("Document content search in store failed. 1 result expected. Received response : "
                            + responseString);
                } else {
                    log.warn("Document content search in store failed. 1 results expected. Received response : "
                            + responseString + " Retrying...");
                    Thread.sleep(5000);
                }
            }
        }

        apiPublisher.deleteAPI(apiName, version, provider);
        Thread.sleep(5000);
    }

    @Test(groups = {
            "wso2.am" }, description = "Test content Search with access control") public void testContentSearchWithAccessControl()
            throws Exception {
        String apiName = "contentSearchTestAPIWithAccessControl";
        String apiContext = "/contentSearchTestAPIWithAccessControl";
        String apiCreatePermission = "/permission/admin/manage/api/create";
        String loginPermission = "/permission/admin/login";
        String apiPublishPermission = "/permission/admin/manage/api/publish";
        String password = "wso2apim";
        String user1 = "user1";
        String user2 = "user2";
        String role1 = "creator_publisher_role";
        String role2 = "publisher_role";
        String description = "UnifiedSearchFeatureWithAccessControl";
        String responseString;

        userManagementClient.addRole(role1, new String[] {},
                new String[] { loginPermission, apiCreatePermission, apiPublishPermission });
        userManagementClient.addRole(role2, new String[] {}, new String[] { loginPermission, apiPublishPermission });

        userManagementClient.addUser(user1, password, new String[] { role1, "Internal/publisher" }, user1);
        userManagementClient.addUser(user2, password, new String[] { role2, "Internal/publisher" }, user2);

        APIRequest apiRequest = createAPIRequest(apiName, apiContext, endpointURL, version, user.getUserName(),
                description);
        apiRequest.setAccessControl("restricted");
        apiRequest.setAccessControlRoles(role1);
        apiPublisher.login(user.getUserName(), user.getPassword());
        apiPublisher.addAPI(apiRequest);
        APIIdentifier apiIdentifier = new APIIdentifier(user.getUserName(), apiName, version);
        apiPublisher.changeAPILifeCycleStatusToPublish(apiIdentifier, false);
        Thread.sleep(10000);

        HttpClient client = HTTPSClientUtils.getHttpsClient();

        //check with user1
        for (int i = 0; i <= retries; i++) {
            HttpGet getAPIsForUser1 = new HttpGet(
                    getPublisherURLHttps() + publisherRestAPIBasePath + "search?query=" + description);
            if (TestUserMode.TENANT_ADMIN == userMode) {
                user1 = user1 + "@" + user.getUserDomain();
            }
            String user1AccessToken = getAccessToken(user1.replace("@", "_"), user1, password);
            getAPIsForUser1.setHeader("Authorization", "Bearer " + user1AccessToken);
            HttpResponse publisherResponse = client.execute(getAPIsForUser1);
            responseString = getResponseBody(publisherResponse);
            if (getResultCount(responseString) == 1) {
                Assert.assertTrue(true);
                break;
            } else {
                if (i == retries) {
                    Assert.fail("Content search with access control failed. 1 result expected. Received response : "
                            + responseString);
                } else {
                    log.warn("Content search with access control failed. 1 results expected. Received response : "
                            + responseString + " Retrying...");
                    Thread.sleep(5000);
                }
            }
        }

        //check with user2 who doesn't have permissions for api
        for (int i = 0; i <= retries; i++) {
            HttpGet getAPIsForUser2 = new HttpGet(
                    getPublisherURLHttps() + publisherRestAPIBasePath + "search?query=" + description);
            if (TestUserMode.TENANT_ADMIN == userMode) {
                user2 = user2 + "@" + user.getUserDomain();
            }
            String user2AccessToken = getAccessToken(user2.replace("@", "_"), user2, password);
            getAPIsForUser2.setHeader("Authorization", "Bearer " + user2AccessToken);
            HttpResponse publisherResponse = client.execute(getAPIsForUser2);
            responseString = getResponseBody(publisherResponse);
            if (getResultCount(responseString) == 0) {
                Assert.assertTrue(true);
                break;
            } else {
                if (i == retries) {
                    Assert.fail("Content search with access control failed. 0 result expected. Received response : "
                            + responseString);
                } else {
                    log.warn("Content search with access control failed. 0 results expected. Received response : "
                            + responseString + " Retrying...");
                    Thread.sleep(5000);
                }
            }
        }

        //clear apis, roles and users
        apiPublisher.deleteAPI(apiName, version, user.getUserName());
        userManagementClient.deleteRole(role1);
        userManagementClient.deleteRole(role2);
        userManagementClient.deleteUser(MultitenantUtils.getTenantAwareUsername(user1));
        userManagementClient.deleteUser(MultitenantUtils.getTenantAwareUsername(user2));

    }

    @Test(groups = {
            "wso2.am" }, description = "Test content Search with store visibility") public void testContentSearchWithStoreVisibility()
            throws Exception {
        String apiName = "contentSearchTestAPIWithAccessControl";
        String apiContext = "/contentSearchTestAPIWithAccessControl";
        String apiSubscribePermission = "/permission/admin/manage/api/subscribe";
        String loginPermission = "/permission/admin/login";
        String password = "wso2apim";
        String user1 = "user1";
        String user2 = "user2";
        String role1 = "subscriber1_role";
        String role2 = "subscriber2_role";
        String description = "UnifiedSearchFeatureWithAccessControl";
        String responseString;

        userManagementClient.addRole(role1, new String[] {}, new String[] { loginPermission, apiSubscribePermission });
        userManagementClient.addRole(role2, new String[] {}, new String[] { loginPermission, apiSubscribePermission });

        userManagementClient.addUser(user1, password, new String[] { role1, "Internal/subscriber" }, user1);
        userManagementClient.addUser(user2, password, new String[] { role2, "Internal/subscriber" }, user2);

        APIRequest apiRequest = createAPIRequest(apiName, apiContext, endpointURL, version, user.getUserName(),
                description);
        apiRequest.setVisibility("restricted");
        apiRequest.setRoles(role1);
        apiPublisher.login(user.getUserName(), user.getPassword());
        apiPublisher.addAPI(apiRequest);
        APIIdentifier apiIdentifier = new APIIdentifier(user.getUserName(), apiName, version);
        apiPublisher.changeAPILifeCycleStatusToPublish(apiIdentifier, false);
        Thread.sleep(10000);

        HttpClient client = HTTPSClientUtils.getHttpsClient();

        //check with user1
        for (int i = 0; i <= retries; i++) {
            HttpGet getAPIsForUser1 = new HttpGet(
                    getStoreURLHttps() + storeRestAPIBasePath + "search?query=" + description);
            if (TestUserMode.TENANT_ADMIN == userMode) {
                getAPIsForUser1.setHeader("X-WSO2-Tenant", user.getUserDomain());
                user1 = user1 + "@" + user.getUserDomain();
            }
            String user1AccessToken = getAccessToken(user1.replace("@", "_"), user1, password);
            getAPIsForUser1.setHeader("Authorization", "Bearer " + user1AccessToken);
            HttpResponse storeResponse = client.execute(getAPIsForUser1);
            responseString = getResponseBody(storeResponse);
            if (getResultCount(responseString) == 1) {
                Assert.assertTrue(true);
                break;
            } else {
                if (i == retries) {
                    Assert.fail("Content search with access control failed. 1 result expected. Received response : "
                            + responseString);
                } else {
                    log.warn("Content search with access control failed. 1 results expected. Received response : "
                            + responseString + " Retrying...");
                    Thread.sleep(5000);
                }
            }
        }

        //check with user2 who doesn't have permissions for api
        for (int i = 0; i <= retries; i++) {
            HttpGet getAPIsForUser2 = new HttpGet(
                    getStoreURLHttps() + storeRestAPIBasePath + "search?query=" + description);
            if (TestUserMode.TENANT_ADMIN == userMode) {
                getAPIsForUser2.setHeader("X-WSO2-Tenant", user.getUserDomain());
                user2 = user2 + "@" + user.getUserDomain();
            }
            String user2AccessToken = getAccessToken(user2.replace("@", "_"), user2, password);
            getAPIsForUser2.setHeader("Authorization", "Bearer " + user2AccessToken);
            HttpResponse storeResponse = client.execute(getAPIsForUser2);
            responseString = getResponseBody(storeResponse);
            if (getResultCount(responseString) == 0) {
                Assert.assertTrue(true);
                break;
            } else {
                if (i == retries) {
                    Assert.fail("Content search with access control failed. 0 result expected. Received response : "
                            + responseString);
                } else {
                    log.warn("Content search with access control failed. 0 results expected. Received response : "
                            + responseString + " Retrying...");
                    Thread.sleep(5000);
                }
            }
        }

        apiPublisher.deleteAPI(apiName, version, user.getUserName());
        userManagementClient.deleteRole(role1);
        userManagementClient.deleteRole(role2);
        userManagementClient.deleteUser(MultitenantUtils.getTenantAwareUsername(user1));
        userManagementClient.deleteUser(MultitenantUtils.getTenantAwareUsername(user2));

    }

    private APIRequest createAPIRequest(String name, String context, String url, String version, String provider,
            String description) throws MalformedURLException, APIManagerIntegrationTestException {
        APIRequest apiRequest = new APIRequest(name, context, new URL(url));
        apiRequest.setVersion(version);
        apiRequest.setDescription(description);
        apiRequest.setProvider(provider);

        return apiRequest;
    }

    private String getResponseBody(HttpResponse response) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String jsonString = "";
        String line;

        while ((line = reader.readLine()) != null) {
            jsonString = jsonString + line;
        }
        return jsonString;
    }

    private int getResultCount(String jsonString) throws JSONException {
        JSONObject responseJSON = new JSONObject(jsonString);
        log.info(responseJSON);
        return responseJSON.getInt("count");
    }

    private String getAccessToken(String clientName, String username, String password)
            throws IOException, JSONException, AutomationFrameworkException {
        HttpClient client = HTTPSClientUtils.getHttpsClient();
        HttpPost tokenPost = new HttpPost("https://localhost:9943/client-registration/v0.14/register");
        byte[] namePasswordPair = (username + ":" + password).getBytes();
        tokenPost.setHeader("Authorization", "Basic " + Base64Utils.encode(namePasswordPair));
        tokenPost.setHeader("Content-Type", "application/json");
        StringEntity payload = new StringEntity(
                "{\"callbackUrl\": \"www.google.lk\", \"clientName\": \"" + clientName + "\", \"owner\": \"" + username
                        + "\", \"grantType\": \"password refresh_token\", \"saasApp\": true}", "UTF-8");
        tokenPost.setEntity(payload);
        HttpResponse keySecretResponse = client.execute(tokenPost);

        BufferedReader reader = new BufferedReader(new InputStreamReader(keySecretResponse.getEntity().getContent()));
        String jsonString = "";
        String line;

        while ((line = reader.readLine()) != null) {
            jsonString = jsonString + line;
        }

        JSONObject clientSecretResponseJSON = new JSONObject(jsonString);
        log.info("DCR Response: " + clientSecretResponseJSON.toString());
        String clientId = clientSecretResponseJSON.getString("clientId");
        String clientSecret = clientSecretResponseJSON.getString("clientSecret");
        String idColonSecret = clientId + ":" + clientSecret;
        byte[] encodedKeys = Base64.encodeBase64(idColonSecret.getBytes("UTF-8"));

        HttpGet tokenGet = new HttpGet("https://localhost:8243/token");

        String messageBody =
                "grant_type=password&username=" + username + "&password=" + password + "&scope=apim:api_view";
        HashMap<String, String> accessKeyMap = new HashMap<String, String>();

        accessKeyMap.put(RESTAPITestConstants.AUTHORIZATION_KEY, "Basic " + new String(encodedKeys, "UTF-8"));
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse tokenGenerateResponse = HttpRequestUtil
                .doPost(tokenApiUrl, messageBody, accessKeyMap);
        JSONObject tokenGenJsonObject = new JSONObject(tokenGenerateResponse);
        log.info("Token Response: " + tokenGenerateResponse.getData());
        String accessToken = new JSONObject(tokenGenJsonObject.get(RESTAPITestConstants.DATA_SECTION).toString())
                .get(RESTAPITestConstants.ACCESS_TOKEN_TEXT).toString();

        if (accessToken != null) {
            return accessToken;
        }
        return accessToken;
    }
}

