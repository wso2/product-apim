/*
 * Copyright (c) 2020, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.am.integration.tests.other;

import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class APICategoriesTestCase extends APIManagerLifecycleBaseTest {
    private final Log log = LogFactory.getLog(APICategoriesTestCase.class);
    private String publisherURLHttps;
    private String categoriesAdminAPIURL;
    private String categoryId;

    @Factory(dataProvider = "userModeDataProvider")
    public APICategoriesTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }, };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        publisherURLHttps = publisherUrls.getWebAppURLHttps();
        categoriesAdminAPIURL = publisherURLHttps + APIMIntegrationConstants.REST_API_ADMIN_CONTEXT_FULL_0
                + APIMIntegrationConstants.REST_API_ADMIN_API_CATEGORIES_RESOURCE;
    }

    @Test(groups = { "wso2.am" }, description = "Test add API category")
    public void testAddAPICategory() throws Exception {
        try (CloseableHttpClient client = HTTPSClientUtils.getHttpsClient();) {
            HttpPost post = new HttpPost(categoriesAdminAPIURL);
            post.addHeader(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                    "Basic " + encodeCredentials(user.getUserName(), user.getPassword().toCharArray()));
            post.addHeader("Content-Type", "application/json");
            StringEntity payload = new StringEntity(
                    "{\"name\": \"Marketing\", \"description\": \"Marketing category\"}", "UTF-8");
            payload.setContentType("application/json");
            post.setEntity(payload);
            CloseableHttpResponse response = client.execute(post);
            Assert.assertEquals(response.getStatusLine().getStatusCode(), 201);

            HttpEntity entity = response.getEntity();
            if (entity != null) {
                try {
                    String responsePayload = EntityUtils.toString(entity);
                    JSONParser parser = new JSONParser();
                    JSONObject object = (JSONObject) parser.parse(responsePayload);
                    Assert.assertEquals((String) object.get("name"), "Marketing");

                    //store the create category's UUID to later use in the category update test
                    categoryId = (String) object.get("id");
                } finally {
                    response.close();
                }
            }
        }
    }

    @Test(groups = { "wso2.am" }, description = "Test add API category with duplicate name", dependsOnMethods = {
            "testAddAPICategory" })
    public void addAPICategoryWithDuplicateName() throws Exception {
        try (CloseableHttpClient client = HTTPSClientUtils.getHttpsClient();) {
            HttpPost post = new HttpPost(categoriesAdminAPIURL);
            post.addHeader(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                    "Basic " + encodeCredentials(user.getUserName(), user.getPassword().toCharArray()));
            post.addHeader("Content-Type", "application/json");
            StringEntity payload = new StringEntity(
                    "{\"name\": \"Marketing\", \"description\": \"Marketing category\"}", "UTF-8");
            payload.setContentType("application/json");
            post.setEntity(payload);
            CloseableHttpResponse response = client.execute(post);
            try {
                HttpEntity entity = response.getEntity();
                String responsePayload = EntityUtils.toString(entity);
                Assert.assertTrue(responsePayload.contains("Category with name 'Marketing' already exists"));
                Assert.assertEquals(response.getStatusLine().getStatusCode(), 500);
            } finally {
                response.close();
            }
        }
    }

    @Test(groups = { "wso2.am" }, description = "Test update API category", dependsOnMethods = {
            "addAPICategoryWithDuplicateName" })
    public void testUpdateAPICategory() throws Exception {
        try (CloseableHttpClient client = HTTPSClientUtils.getHttpsClient();) {
            HttpPut put = new HttpPut(categoriesAdminAPIURL + "/" + categoryId);
            put.addHeader(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                    "Basic " + encodeCredentials(user.getUserName(), user.getPassword().toCharArray()));
            put.addHeader("Content-Type", "application/json");
            StringEntity payload = new StringEntity("{\"name\": \"Sales\", \"description\": \"Sales category\"}",
                    "UTF-8");
            payload.setContentType("application/json");
            put.setEntity(payload);
            try (CloseableHttpResponse response = client.execute(put);) {
                HttpEntity entity = response.getEntity();
                Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
            }
        }
    }

    @Test(groups = { "wso2.am" }, description = "Test get API categories",
            dependsOnMethods = { "testUpdateAPICategory" })
    public void testGetAPICategoriesFromAdminAPI() throws Exception {
        try (CloseableHttpClient client = HTTPSClientUtils.getHttpsClient();) {
            HttpGet get = new HttpGet(categoriesAdminAPIURL);
            get.addHeader(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                    "Basic " + encodeCredentials(user.getUserName(), user.getPassword().toCharArray()));
            try (CloseableHttpResponse response = client.execute(get);) {
                HttpEntity entity = response.getEntity();
                String responsePayload = EntityUtils.toString(entity);
                JSONParser parser = new JSONParser();
                JSONObject object = (JSONObject) parser.parse(responsePayload);
                int count = (int) (long) object.get("count");
                Assert.assertEquals(count, 1);
            }
        }
    }

    @Test(groups = { "wso2.am" }, description = "Test attach API category to API")
    public void testAttachAPICategoryToAPI() throws Exception{
        //Add Category
        try (CloseableHttpClient client = HTTPSClientUtils.getHttpsClient();) {
            HttpPost post = new HttpPost(categoriesAdminAPIURL);
            post.addHeader(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                    "Basic " + encodeCredentials(user.getUserName(), user.getPassword().toCharArray()));
            post.addHeader("Content-Type", "application/json");
            StringEntity payload = new StringEntity(
                    "{\"name\": \"Sales\", \"description\": \"Sales category\"}", "UTF-8");
            payload.setContentType("application/json");
            post.setEntity(payload);
            CloseableHttpResponse response = client.execute(post);
        }

        //Add API
        String apiName = "CategoryTestAPI";
        String apiContext = "category";
        String apiVersion = "1.0";
        String url = getGatewayURLHttp() + "jaxrs_basic/services/customers/customerservice";
        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(url));
        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(user.getUserName());

        //Add the API using the API publisher.
        HttpResponse postResponse = restAPIPublisher.addAPI(apiRequest);
        String apiId = postResponse.getData();

        //update API with category mapping
        List<String> apiCategories = new ArrayList<>();
        apiCategories.add("Sales");
        apiRequest.setApiCategories(apiCategories);
        HttpResponse updateResponse = restAPIPublisher.updateAPI(apiRequest, apiId);

        waitForAPIDeployment();
        HttpResponse getResponse = restAPIPublisher.getAPI(updateResponse.getData());

        Gson g = new Gson();
        APIDTO apidto = g.fromJson(getResponse.getData(), APIDTO.class);
        List<String> categoriesInReceivedAPI = apidto.getCategories();
        Assert.assertNotNull(categoriesInReceivedAPI);
        Assert.assertTrue(categoriesInReceivedAPI.contains("Sales"));
    }

    @Test(groups = { "wso2.am" }, description = "Test delete API category", dependsOnMethods = {
            "testGetAPICategoriesFromAdminAPI" })
    public void testDeleteAPICategory() throws Exception {
        try (CloseableHttpClient client = HTTPSClientUtils.getHttpsClient();) {
            HttpDelete delete = new HttpDelete(categoriesAdminAPIURL + "/" + categoryId);
            delete.addHeader(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                    "Basic " + encodeCredentials(user.getUserName(), user.getPassword().toCharArray()));
            try (CloseableHttpResponse response = client.execute(delete);) {
                Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
            }
        }
    }

    /**
     * get the base64 encoded username and password
     *
     * @param user username
     * @param pass password
     * @return encoded basic auth, as string
     */
    private static String encodeCredentials(String user, char[] pass) {
        StringBuilder builder = new StringBuilder(user).append(':').append(pass);
        String cred = builder.toString();
        byte[] encodedBytes = Base64.encodeBase64(cred.getBytes());
        return new String(encodedBytes);
    }
}
