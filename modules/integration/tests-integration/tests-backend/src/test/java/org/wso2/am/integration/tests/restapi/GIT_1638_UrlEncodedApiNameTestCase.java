/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.am.integration.tests.restapi;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.restapi.utils.RESTAPITestUtil;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.exceptions.AutomationFrameworkException;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class GIT_1638_UrlEncodedApiNameTestCase extends APIMIntegrationBaseTest {

    private String storeRestApiBaseUrl;
    private String publisherRestApiBaseUrl;
    private URL tokenApiUrl;
    private String applicationName = "Application_GIT_1638";
    private String applicationId;
    private String apiName = "git-1638";
    private String context = "git1638";
    private String encodedApiName = "git%2D1638";
    private Map<String, String> headers;

    @Factory(dataProvider = "userModeDataProvider")
    public GIT_1638_UrlEncodedApiNameTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] {new Object[] {TestUserMode.SUPER_TENANT_ADMIN},
                new Object[] {TestUserMode.TENANT_ADMIN},};
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        storeRestApiBaseUrl = getStoreURLHttps() + "api/am/store/v0.11/";
        publisherRestApiBaseUrl = getStoreURLHttps() + "api/am/publisher/v0.11/";
        tokenApiUrl = new URL(getKeyManagerURLHttps() + "oauth2/token");
        Map<String, String> dataMap = RESTAPITestUtil.registerOAuthApplication(getKeyManagerURLHttps());
        String accessToken = generateOAuthAccessToken(user.getUserName(), user.getPassword(),
                dataMap.get(RESTAPITestConstants.CONSUMER_KEY),
                dataMap.get(RESTAPITestConstants.CONSUMER_SECRET));
        headers = new HashMap<String, String>();
        headers.put("Authorization", "Bearer " + accessToken);
        headers.put("Content-Type", "application/json");
        //create application
        String url = storeRestApiBaseUrl + "applications";
        String payload = "{\n" +
                "    \"throttlingTier\": \"Unlimited\",\n" +
                "    \"description\": \"Application for GIT_1638\",\n" +
                "    \"name\": \"" + applicationName + "_" + user.getUserDomain() + "\",\n" +
                "    \"callbackUrl\": \"http://GIT_1638.com/initial\"\n" +
                '}';
        HttpResponse response = HTTPSClientUtils.doPost(url, headers, payload);
        if (response.getResponseCode() != 201) {
            Assert.fail("Application creation failed: Response code is " + response.getResponseCode()
                    + " Response message is '" + response.getData() + '\'');
        }
        String json = response.getData();
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
        applicationId = jsonObject.get("applicationId").getAsString();
        //Api Creation
        String apiCreationUrl = publisherRestApiBaseUrl + "apis";
        String apiCreateJsonLocation = getAMResourceLocation() + File.separator + "git1638" + File.separator
                + File.separator + "APICreate.json";
        String apiCreationJson = IOUtils.toString(new FileInputStream(apiCreateJsonLocation));
        apiCreationJson = apiCreationJson.replace("${name}", apiName);
        if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(user.getUserDomain())) {
            context = "/t/" + user.getUserDomain() + "." + "/" + context;

        }
        apiCreationJson = apiCreationJson.replace("${context}", context);
        apiCreationJson = apiCreationJson.replace("${provider}", user.getUserName());
        HttpResponse apiCreationResponse = HTTPSClientUtils.doPost(apiCreationUrl, headers, apiCreationJson);
        if (response.getResponseCode() != 201) {
            Assert.fail("API creation failed: Response code is " + apiCreationResponse.getResponseCode()
                    + " Response message is '" + response.getData() + '\'');
        }
    }

    @Test(description = "api detail with API name having hyphen")
    public void testGetApiDetailFromPublisher() throws Exception {
        String url = publisherRestApiBaseUrl + "apis/" + user.getUserNameWithoutDomain();
        if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(user.getUserDomain())) {
            url += "-AT-" + user.getUserDomain();
        }
        url += "-" + encodedApiName + "-1.0.0";
        HttpResponse response = HTTPSClientUtils.doGet(url, headers);
        if (response.getResponseCode() != 200) {
            Assert.fail("Api Detail get failed: Response code is " + response.getResponseCode()
                    + " Response message is '" + response.getData() + '\'');
        }
        String json = response.getData();
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
        String retrievedApiName = jsonObject.getAsJsonPrimitive("name").getAsString();
        Assert.assertEquals(apiName, retrievedApiName);

        //checking publisher get APIs list
        String publisherApisList = HTTPSClientUtils.doGet(publisherRestApiBaseUrl + "apis/", headers).getData();
        Assert.assertTrue(publisherApisList.contains("pagination"), "Pagination data not present in response");
        //checking store get APIs list
        String storeApisList = HTTPSClientUtils.doGet(storeRestApiBaseUrl + "apis/", null).getData();
        Assert.assertTrue(storeApisList.contains("pagination"), "Pagination data not present in response");
    }

    @Test(description = "api detail with API name having hyphen")
    public void testChangeLifeCycleToPublish() throws Exception {
        String apiIdentifier = user.getUserNameWithoutDomain();
        if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(user.getUserDomain())) {
            apiIdentifier += "-AT-" + user.getUserDomain();
        }
        apiIdentifier += "-" + encodedApiName + "-1.0.0";
        String lifecycleChangeUrl = publisherRestApiBaseUrl + "apis/change-lifecycle?apiId=" + apiIdentifier + "&" +
                "action=Publish";
        HttpResponse response = HTTPSClientUtils.doPost(lifecycleChangeUrl, headers, "");
        if (response.getResponseCode() != 200) {
            Assert.fail("APi lifecycle change failed: Response code is " + response.getResponseCode()
                    + " Response message is '" + response.getData() + '\'');
        }
        // subscribe to api
        String subscriptionUrl = storeRestApiBaseUrl + "subscriptions";
        String payload = "{\n" +
                "    \"tier\": \"Unlimited\",\n" +
                "    \"apiIdentifier\": \"" + apiIdentifier + "\",\n" +
                "    \"applicationId\": \"" + applicationId + "\"\n" +
                "}";
        HttpResponse subscriptionResponse = HTTPSClientUtils.doPost(subscriptionUrl, headers, payload);
        if (subscriptionResponse.getResponseCode() != 201) {
            Assert.fail("Subscription: Response code is " + subscriptionResponse.getResponseCode()
                    + " Response message is '" + subscriptionResponse.getData() + '\'');
        }
        String json = subscriptionResponse.getData();
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
        String retrievedApiName = jsonObject.getAsJsonPrimitive("apiIdentifier").getAsString();
        String apiIdentifierForAssert = user.getUserName();
        apiIdentifierForAssert += "-" + encodedApiName + "-1.0.0";
        Assert.assertEquals(apiIdentifierForAssert, retrievedApiName, "ApiIdentifier got changed");
        // get subscriptions according to api from publisher
        String getSubscriptionUrl = publisherRestApiBaseUrl + "subscriptions?apiId=" + apiIdentifier;
        HttpResponse getSubscriptionResponse = HTTPSClientUtils.doGet(getSubscriptionUrl, headers);
        if (getSubscriptionResponse.getResponseCode() != 200) {
            Assert.fail("Subscription: Response code is " + getSubscriptionResponse.getResponseCode()
                    + " Response message is '" + getSubscriptionResponse.getData() + '\'');
        }
        String subscriptionResponseData = getSubscriptionResponse.getData();
        JsonObject subscriptionResponseDataJson = gson.fromJson(subscriptionResponseData, JsonObject.class);
        JsonArray subscriptionList = subscriptionResponseDataJson.getAsJsonArray("list");
        JsonObject subscription = subscriptionList.get(0).getAsJsonObject();
        String retrievedApiIdentifier = subscription.getAsJsonPrimitive("apiIdentifier").getAsString();
        Assert.assertEquals(apiIdentifierForAssert, retrievedApiIdentifier, "ApiIdentifier got changed");

        // Get subscriptions from store
        String getSubscriptionFromStoreUrl = storeRestApiBaseUrl + "subscriptions?apiId=" + apiIdentifier;
        HttpResponse getSubscriptionFromStoreResponse = HTTPSClientUtils.doGet(getSubscriptionFromStoreUrl, headers);
        if (getSubscriptionFromStoreResponse.getResponseCode() != 200) {
            Assert.fail("Subscription: Response code is " + getSubscriptionFromStoreResponse.getResponseCode()
                    + " Response message is '" + getSubscriptionFromStoreResponse.getData() + '\'');
        }
        String subscriptionResponseFromStoreData = getSubscriptionFromStoreResponse.getData();
        JsonObject subscriptionResponseFromStoreDataJson = gson.fromJson(subscriptionResponseFromStoreData,
                JsonObject.class);
        JsonArray subscriptionListFromStore = subscriptionResponseFromStoreDataJson.getAsJsonArray("list");
        JsonObject subscriptionFromStore = subscriptionListFromStore.get(0).getAsJsonObject();
        String retrievedApiIdentifierFromStore = subscriptionFromStore.getAsJsonPrimitive("apiIdentifier")
                .getAsString();
        Assert.assertEquals(apiIdentifierForAssert, retrievedApiIdentifierFromStore, "ApiIdentifier got changed");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.login(user.getUserName(), user.getPassword());
        apiStore.removeApplication(applicationName);
        apiPublisher.login(user.getUserName(), user.getPassword());
        apiPublisher.deleteAPI(apiName, "1.0.0", user.getUserName());
        super.cleanUp();
    }

    private String generateOAuthAccessToken(String username, String password, String consumerKey, String consumerSecret)
            throws APIManagerIntegrationTestException {

        try {
            String messageBody = "grant_type=password&username=" + username + "&password=" + password +
                    "&scope=apim:api_publish apim:api_create apim:api_view apim:subscribe apim:tier_view " +
                    "apim:tier_manage apim:subscription_view apim:subscription_block";
            HashMap<String, String> accessKeyMap = new HashMap<String, String>();

            String authenticationHeader = consumerKey + ":" + consumerSecret;
            byte[] encodedBytes = Base64.encodeBase64(authenticationHeader.getBytes("UTF-8"));
            accessKeyMap.put(RESTAPITestConstants.AUTHORIZATION_KEY, "Basic " + new String(encodedBytes, "UTF-8"));
            HttpResponse tokenGenerateResponse = HttpRequestUtil.doPost(tokenApiUrl, messageBody, accessKeyMap);
            JSONObject tokenGenJsonObject = new JSONObject(tokenGenerateResponse);
            String accessToken = new JSONObject(tokenGenJsonObject.get(RESTAPITestConstants.DATA_SECTION).toString())
                    .get(RESTAPITestConstants.ACCESS_TOKEN_TEXT).toString();

            if (accessToken != null) {
                return accessToken;
            }
        } catch (UnsupportedEncodingException unsupportedEncodingException) {
            throw new APIManagerIntegrationTestException
                    ("Message header encoding was unsuccessful using UTF-8.", unsupportedEncodingException);
        } catch (AutomationFrameworkException automationFrameworkException) {
            throw new APIManagerIntegrationTestException
                    ("Error in sending the request to token endpoint.", automationFrameworkException);
        } catch (JSONException e) {
            throw new APIManagerIntegrationTestException
                    ("Error in parsing JSON content in response from token endpoint.", e);
        }
        return null;
    }
}
