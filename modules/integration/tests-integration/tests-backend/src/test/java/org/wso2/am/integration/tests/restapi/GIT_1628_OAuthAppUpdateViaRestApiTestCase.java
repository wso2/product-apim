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
import com.google.gson.JsonPrimitive;
import org.apache.commons.codec.binary.Base64;
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

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class GIT_1628_OAuthAppUpdateViaRestApiTestCase extends APIMIntegrationBaseTest {

    private String storeRestApiBaseUrl;
    private URL tokenApiUrl;
    private String applicationName = "Application_GIT_1628";
    private String applicationId;
    private String consumerKey;
    private String consumerSecret;
    private Map<String, String> headers;

    @Factory(dataProvider = "userModeDataProvider")
    public GIT_1628_OAuthAppUpdateViaRestApiTestCase(TestUserMode userMode) {
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
                "    \"description\": \"Application for GIT_1628\",\n" +
                "    \"name\": \"" + applicationName + "_" + user.getUserDomain() + "\",\n" +
                "    \"callbackUrl\": \"http://GIT_1628.com/initial\"\n" +
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
    }

    @Test(description = "Key generation response should contain grant types and callback url")
    public void testKeyGenerationResponseContainsGrantTypesAndCallback() throws Exception {
        String url = storeRestApiBaseUrl + "applications/generate-keys?applicationId=" + applicationId;
        String payload = "{\n" +
                "  \"validityTime\": \"3600\",\n" +
                "  \"keyType\": \"PRODUCTION\",\n" +
                "  \"accessAllowDomains\": [\"ALL\" ],\n" +
                "  \"callbackUrl\": \"http://GIT_1628.com/prod_callback\",\n" +
                "  \"supportedGrantTypes\":    [\n" +
                "      \"refresh_token\",\n" +
                "      \"client_credentials\"\n" +
                "  ]\n" +
                '}';
        HttpResponse response = HTTPSClientUtils.doPost(url, headers, payload);
        if (response.getResponseCode() != 200) {
            Assert.fail("Key generation failed: Response code is " + response.getResponseCode()
                    + " Response message is '" + response.getData() + '\'');
        }

        String json = response.getData();
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        consumerKey = jsonObject.getAsJsonPrimitive("consumerKey").getAsString();
        Assert.assertNotNull(consumerKey, "ConsumerKey is not available in the response:" + response.getData());
        consumerSecret = jsonObject.getAsJsonPrimitive("consumerSecret").getAsString();
        Assert.assertNotNull(consumerSecret, "ConsumerSecret is not available in the response:" + response.getData());

        String callbackUrl = jsonObject.getAsJsonPrimitive("callbackUrl").getAsString();
        Assert.assertEquals(callbackUrl, "http://GIT_1628.com/prod_callback");

        JsonArray grantTypes = jsonObject.getAsJsonArray("supportedGrantTypes");
        Assert.assertNotNull(grantTypes, "Grant Types are not available in the response:" + response.getData());

        Assert.assertTrue(grantTypes.contains(new JsonPrimitive("refresh_token")), "refresh_token is not available " +
                "in grant type list. Response: " + response.getData());
        Assert.assertTrue(grantTypes.contains(new JsonPrimitive("client_credentials")), "client_credentials is not available " +
                "in grant type list. Response: " + response.getData());

        Assert.assertFalse(grantTypes.contains(new JsonPrimitive("password")), "password is available " +
                "in grant type list. Response: " + response.getData());
        Assert.assertFalse(grantTypes.contains(new JsonPrimitive("iwa:ntlm")), "iwa:ntlm is available " +
                "in grant type list. Response: " + response.getData());
        Assert.assertFalse(grantTypes.contains(new JsonPrimitive("urn:ietf:params:oauth:grant-type:saml2-bearer")),
                "urn:ietf:params:oauth:grant-type:saml2-bearer is available in grant type list. Response: "
                        + response.getData());
    }

    @Test(description = "Generate token with password grant type when password grant type is not enabled",
            dependsOnMethods = "testKeyGenerationResponseContainsGrantTypesAndCallback")
    public void testTokenGenerationWithPasswordGrant() throws Exception {
        Map tokenApiHeaders = new HashMap<String, String>();
        String authenticationHeader = consumerKey + ":" + consumerSecret;
        byte[] encodedBytes = Base64.encodeBase64(authenticationHeader.getBytes("UTF-8"));
        tokenApiHeaders.put(RESTAPITestConstants.AUTHORIZATION_KEY, "Basic " + new String(encodedBytes, "UTF-8"));
        String payload = "grant_type=password&username=" + user.getUserName() + "&password=" + user.getPassword();

        HttpResponse response = HTTPSClientUtils.doPost(tokenApiUrl, payload, tokenApiHeaders);
        Assert.assertEquals(response.getResponseCode(), 400, "Password grant type is not disabled properly: " +
                "Response code is " + response.getResponseCode() + " Response message is '" +
                response.getData() + '\'');
        Assert.assertTrue(response.getData().contains("The authenticated client is not authorized to use this " +
                "authorization grant type"), "Wrong error message found.");

    }

    @Test(description = "Generate token with client credentials grant type when client credentials grant type is " +
            "enabled", dependsOnMethods = "testKeyGenerationResponseContainsGrantTypesAndCallback")
    public void testTokenGenerationWithClientCredentialsGrant() throws Exception {
        Map tokenApiHeaders = new HashMap<String, String>();
        String authenticationHeader = consumerKey + ":" + consumerSecret;
        byte[] encodedBytes = Base64.encodeBase64(authenticationHeader.getBytes("UTF-8"));
        tokenApiHeaders.put(RESTAPITestConstants.AUTHORIZATION_KEY, "Basic " + new String(encodedBytes, "UTF-8"));
        String payload = "grant_type=client_credentials";

        HttpResponse response = HTTPSClientUtils.doPost(tokenApiUrl, payload, tokenApiHeaders);
        Assert.assertEquals(response.getResponseCode(), 200, "Client Credentials token call is not successful: " +
                "Response code is " + response.getResponseCode() + " Response message is '" +
                response.getData() + '\'');
    }

    @Test(description = "Updating grant types and callback url",
            dependsOnMethods = "testKeyGenerationResponseContainsGrantTypesAndCallback")
    public void testUpdateGrantTypesAndCallback() throws Exception {
        String url = storeRestApiBaseUrl + "applications/" + applicationId + "/keys/PRODUCTION";
        String payload = "{\n" +
                "  \"callbackUrl\": \"http://GIT_1628.com/prod_updated\",\n" +
                "  \"supportedGrantTypes\":    [\n" +
                "      \"password\",\n" +
                "      \"iwa:ntlm\"\n" +
                "  ]\n" +
                '}';
        HttpResponse response = HTTPSClientUtils.doPut(url, headers, payload);
        if (response.getResponseCode() != 200) {
            Assert.fail("Grant type/callback update failed: Response code is " + response.getResponseCode()
                    + " Response message is '" + response.getData() + '\'');
        }

        String json = response.getData();
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        String callbackUrl = jsonObject.getAsJsonPrimitive("callbackUrl").getAsString();
        Assert.assertEquals(callbackUrl, "http://GIT_1628.com/prod_updated");

        JsonArray grantTypes = jsonObject.getAsJsonArray("supportedGrantTypes");
        Assert.assertNotNull(grantTypes, "Grant Types are not available in the response. Response: "
                + response.getData());

        Assert.assertTrue(grantTypes.contains(new JsonPrimitive("password")), "password is not available " +
                "in grant type list. Response: " + response.getData());
        Assert.assertTrue(grantTypes.contains(new JsonPrimitive("iwa:ntlm")), "iwa:ntlm is not available " +
                "in grant type list. Response: " + response.getData());

        Assert.assertFalse(grantTypes.contains(new JsonPrimitive("client_credentials")),
                "client_credentials is available in grant type list. Response: " + response.getData());
        Assert.assertFalse(grantTypes.contains(new JsonPrimitive("refresh_token")), "refresh_token is available " +
                "in grant type list. Response: " + response.getData());
        Assert.assertFalse(grantTypes.contains(new JsonPrimitive("urn:ietf:params:oauth:grant-type:saml2-bearer")),
                "urn:ietf:params:oauth:grant-type:saml2-bearer is available in grant type list. Response: "
                        + response.getData());

        //test updated grant types
        HashMap<String, String> tokenApiHeaders = new HashMap<String, String>();
        String authenticationHeader = consumerKey + ":" + consumerSecret;
        byte[] encodedBytes = Base64.encodeBase64(authenticationHeader.getBytes("UTF-8"));
        tokenApiHeaders.put(RESTAPITestConstants.AUTHORIZATION_KEY, "Basic " + new String(encodedBytes, "UTF-8"));

        //password
        payload = "grant_type=password&username=" + user.getUserName() + "&password=" + user.getPassword();

        response = HTTPSClientUtils.doPost(tokenApiUrl, payload, tokenApiHeaders);
        Assert.assertEquals(response.getResponseCode(), 200, "Password grant token call is not successful: " +
                "Response code is " + response.getResponseCode() + " Response message is '" +
                response.getData() + '\'');

        //client credentials
        payload = "grant_type=client_credentials";

        response = HTTPSClientUtils.doPost(tokenApiUrl, payload, tokenApiHeaders);
        Assert.assertEquals(response.getResponseCode(), 400, "Client Credentials grant type is not disabled properly: "
                + "Response code is " + response.getResponseCode() + " Response message is '"
                + response.getData() + '\'');
        Assert.assertTrue(response.getData().contains("The authenticated client is not authorized to use this " +
                "authorization grant type"), "Wrong error message found.");
    }

    @Test(description = "Retrieving grant types and callback url", dependsOnMethods = "testUpdateGrantTypesAndCallback")
    public void testGetGrantTypesAndCallback() throws Exception {
        String url = storeRestApiBaseUrl + "applications/" + applicationId + "/keys/PRODUCTION";
        HttpResponse response = HTTPSClientUtils.doGet(url, headers);
        if (response.getResponseCode() != 200) {
            Assert.fail("Grant type/callback retrieval failed: Response code is " + response.getResponseCode()
                    + " Response message is '" + response.getData() + '\'');
        }

        String json = response.getData();
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        String callbackUrl = jsonObject.getAsJsonPrimitive("callbackUrl").getAsString();
        Assert.assertEquals(callbackUrl, "http://GIT_1628.com/prod_updated");

        JsonArray grantTypes = jsonObject.getAsJsonArray("supportedGrantTypes");
        Assert.assertNotNull(grantTypes, "Grant Types are not available in the response. Response: "
                + response.getData());

        Assert.assertTrue(grantTypes.contains(new JsonPrimitive("password")), "password is not available " +
                "in grant type list. Response: " + response.getData());
        Assert.assertTrue(grantTypes.contains(new JsonPrimitive("iwa:ntlm")), "iwa:ntlm is not available " +
                "in grant type list. Response: " + response.getData());

        Assert.assertFalse(grantTypes.contains(new JsonPrimitive("refresh_token")), "refresh_token is available " +
                "in grant type list. Response: " + response.getData());
        Assert.assertFalse(grantTypes.contains(new JsonPrimitive("client_credentials")),
                "client_credentials is available in grant type list");
        Assert.assertFalse(grantTypes.contains(new JsonPrimitive("urn:ietf:params:oauth:grant-type:saml2-bearer")),
                "urn:ietf:params:oauth:grant-type:saml2-bearer is available in grant type list. Response: "
                        + response.getData());
    }

    @Test(description = "Retrieving application and check keys", dependsOnMethods = "testGetGrantTypesAndCallback")
    public void testGetApplicationAndCheckKeys() throws Exception {
        String url = storeRestApiBaseUrl + "applications/" + applicationId;
        HttpResponse response = HTTPSClientUtils.doGet(url, headers);
        if (response.getResponseCode() != 200) {
            Assert.fail("Grant type/callback retrieval failed: Response code is " + response.getResponseCode()
                    + " Response message is '" + response.getData() + '\'');
        }

        String json = response.getData();
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        JsonArray keys = jsonObject.getAsJsonArray("keys");
        Assert.assertNotNull(keys, "Keys are not available in the response. Response: " + response.getData());

        JsonObject key = (JsonObject) keys.get(0);

        String callbackUrl = key.getAsJsonPrimitive("callbackUrl").getAsString();
        Assert.assertEquals(callbackUrl, "http://GIT_1628.com/prod_updated");

        JsonArray grantTypes = key.getAsJsonArray("supportedGrantTypes");
        Assert.assertNotNull(grantTypes, "Grant Types are not available in the response. Response: "
                + response.getData());

        Assert.assertTrue(grantTypes.contains(new JsonPrimitive("password")), "password is not available " +
                "in grant type list. Response: " + response.getData());
        Assert.assertTrue(grantTypes.contains(new JsonPrimitive("iwa:ntlm")), "iwa:ntlm is not available " +
                "in grant type list. Response: " + response.getData());

        Assert.assertFalse(grantTypes.contains(new JsonPrimitive("refresh_token")), "refresh_token is available " +
                "in grant type list. Response: " + response.getData());
        Assert.assertFalse(grantTypes.contains(new JsonPrimitive("client_credentials")),
                "client_credentials is available in grant type list");
        Assert.assertFalse(grantTypes.contains(new JsonPrimitive("urn:ietf:params:oauth:grant-type:saml2-bearer")),
                "urn:ietf:params:oauth:grant-type:saml2-bearer is available in grant type list. Response: "
                        + response.getData());
    }

    @Test(description = "Key generation response should contain all grant types even when request does not have " +
            "grant types. (for backward compatibility)",
            dependsOnMethods = "testGetApplicationAndCheckKeys")
    public void testKeyGenerationWithoutGrantTypesNorCallback() throws Exception {
        String url = storeRestApiBaseUrl + "applications/generate-keys?applicationId=" + applicationId;
        String payload = "{\n" +
                "  \"validityTime\": \"3600\",\n" +
                "  \"keyType\": \"SANDBOX\",\n" +
                "  \"accessAllowDomains\": [\"ALL\" ]\n" +
                '}';
        HttpResponse response = HTTPSClientUtils.doPost(url, headers, payload);
        if (response.getResponseCode() != 200) {
            Assert.fail("Key generation failed: Response code is " + response.getResponseCode()
                    + " Response message is '" + response.getData() + '\'');
        }

        String json = response.getData();
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        JsonArray grantTypes = jsonObject.getAsJsonArray("supportedGrantTypes");
        Assert.assertNotNull(grantTypes, "Grant Types are not available in the response:" + response.getData());

        Assert.assertTrue(grantTypes.contains(new JsonPrimitive("refresh_token")), "refresh_token is not available " +
                "in grant type list. Response: " + response.getData());
        Assert.assertTrue(grantTypes.contains(new JsonPrimitive("client_credentials")), "client_credentials is not available " +
                "in grant type list. Response: " + response.getData());
        Assert.assertTrue(grantTypes.contains(new JsonPrimitive("password")), "password is not available " +
                "in grant type list. Response: " + response.getData());
        Assert.assertTrue(grantTypes.contains(new JsonPrimitive("iwa:ntlm")), "iwa:ntlm is not available " +
                "in grant type list. Response: " + response.getData());
        Assert.assertTrue(grantTypes.contains(new JsonPrimitive("urn:ietf:params:oauth:grant-type:saml2-bearer")),
                "urn:ietf:params:oauth:grant-type:saml2-bearer is not available " +
                        "in grant type list. Response: " + response.getData());
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.login(user.getUserName(), user.getPassword());
        apiStore.removeApplication(applicationName);
        super.cleanUp();
    }

    private String generateOAuthAccessToken(String username, String password, String consumerKey, String consumerSecret)
            throws APIManagerIntegrationTestException {

        try {
            String messageBody = "grant_type=password&username=" + username + "&password=" + password +
                    "&scope=apim:subscribe";
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
