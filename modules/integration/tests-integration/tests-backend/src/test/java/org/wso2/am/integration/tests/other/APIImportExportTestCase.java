/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*
*/
package org.wso2.am.integration.tests.other;

import com.google.common.io.Files;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.http.HttpRequestUtil;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

/**
 * This test case is used to test the API Manager Import Export tool
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
public class APIImportExportTestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(APIImportExportTestCase.class);
    private final String API_NAME = "APIImportExportTestCaseAPIName";
    private final String NEW_API_NAME = "NewAPIImportExportTestCaseAPIName";
    private final String API_CONTEXT = "APIImportExportTestCaseContext";
    private final String NEW_API_CONTEXT = "NewAPIImportExportTestCaseContext";
    private final String ALLOWED_ROLE = "allowedRole";
    private final String VISIBILITY_ROLE = "visibilityRole";
    private final String NOT_ALLOWED_ROLE = "denyRole";
    private final String[] PERMISSIONS = { "/permission/admin/login", "/permission/admin/manage/api/subscribe" };
    private final String ALLOWED_USER = "allowedUser";
    private final char[] ALLOWED_USER_PASS = "pass@123".toCharArray();
    private final String DENIED_USER = "deniedUser";
    private final char[] DENIED_USER_PASS = "pass@123".toCharArray();
    private final String SCOPE_NAME = "ImportExportScope";
    private final String TAG1 = "import";
    private final String TAG2 = "export";
    private final String TAG3 = "test";
    private final String DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_VERSION = "1.0.0";
    private final String APP_NAME = "APIImportExportTestCaseApp";
    private final String NEW_APP_NAME = "newAPIImportExportTestCaseApp";

    private String publisherURLHttp;
    private String storeURLHttp;
    private File zipTempDir, apiZip, newApiZip;
    private String importUrl;
    private String exportUrl;
    private APICreationRequestBean apiCreationRequestBean;
    private List<APIResourceBean> resList;
    private String tags;
    private String tierCollection;
    private String endpointUrl;
    private Map<String, String> requestHeaders = new HashMap<String, String>();
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;

    @Factory(dataProvider = "userModeDataProvider")
    public APIImportExportTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);

        publisherURLHttp = getPublisherURLHttps();
        storeURLHttp = getStoreURLHttp();
        endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api";
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(user.getUserName(), user.getPassword());

        //concat tags
        tags = TAG1 + "," + TAG2 + "," + TAG3;
        tierCollection = APIMIntegrationConstants.API_TIER.BRONZE + "," + APIMIntegrationConstants.API_TIER.GOLD + ","
                + APIMIntegrationConstants.API_TIER.SILVER + "," + APIMIntegrationConstants.API_TIER.UNLIMITED;
        importUrl = publisherURLHttp + "api-import-export-1.0.1/import-api";
        exportUrl = publisherURLHttp + "api-import-export-1.0.1/export-api";

        //adding new 3 roles and two users
        userManagementClient = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                createSession(keyManagerContext));
        userManagementClient.addRole(ALLOWED_ROLE, null, PERMISSIONS);
        userManagementClient.addRole(NOT_ALLOWED_ROLE, null, PERMISSIONS);
        userManagementClient.addRole(VISIBILITY_ROLE, null, PERMISSIONS);

        userManagementClient.addUser(ALLOWED_USER, String.valueOf(ALLOWED_USER_PASS),
                new String[] { ALLOWED_ROLE, VISIBILITY_ROLE }, null);
        userManagementClient.addUser(DENIED_USER, String.valueOf(DENIED_USER_PASS),
                new String[] { NOT_ALLOWED_ROLE, VISIBILITY_ROLE }, null);
    }

    @Test(groups = { "wso2.am" }, description = "Sample API creation")
    public void testAPICreation() throws Exception {
        String providerName = user.getUserName();

        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION, providerName,
                new URL(exportUrl));
        apiCreationRequestBean.setTags(tags);
        apiCreationRequestBean.setDescription(DESCRIPTION);
        apiCreationRequestBean.setTiersCollection(tierCollection);

        //define resources
        resList = new ArrayList<APIResourceBean>();
        APIResourceBean res1 = new APIResourceBean("POST",
                APIMIntegrationConstants.ResourceAuthTypes.NONE.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.PLUS, "/post");
        APIResourceBean res2 = new APIResourceBean("GET",
                APIMIntegrationConstants.ResourceAuthTypes.APPLICATION.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.BASIC, "/get");
        APIResourceBean res3 = new APIResourceBean("PUT",
                APIMIntegrationConstants.ResourceAuthTypes.APPLICATION_USER.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.ULTIMATE, "/put");
        APIResourceBean res4 = new APIResourceBean("DELETE",
                APIMIntegrationConstants.ResourceAuthTypes.APPLICATION_AND_APPLICATION_USER.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED, "/delete");
        APIResourceBean res5 = new APIResourceBean("PATCH",
                APIMIntegrationConstants.ResourceAuthTypes.NONE.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.BASIC, "/patch");
        APIResourceBean res6 = new APIResourceBean("HEAD",
                APIMIntegrationConstants.ResourceAuthTypes.NONE.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.BASIC, "/head");
        APIResourceBean res7 = new APIResourceBean("OPTIONS",
                APIMIntegrationConstants.ResourceAuthTypes.NONE.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.BASIC, "/options");
        resList.add(res1);
        resList.add(res2);
        resList.add(res3);
        resList.add(res4);
        resList.add(res5);
        resList.add(res6);
        resList.add(res7);
        apiCreationRequestBean.setResourceBeanList(resList);

        //add test api
        HttpResponse serviceResponse = apiPublisher.addAPI(apiCreationRequestBean);
        verifyResponse(serviceResponse);

        //publish the api
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(API_NAME, user.getUserName(),
                APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);

    }

    @Test(groups = { "wso2.am" }, description = "Exported Sample API", dependsOnMethods = "testAPICreation")
    public void testAPIExport() throws Exception {

        //construct export API url
        URL exportRequest = new URL(
                exportUrl + "?name=" + API_NAME + "&version=" + API_VERSION + "&provider=" + user.getUserName());
        zipTempDir = Files.createTempDir();

        //set the export file name with tenant prefix
        String fileName = user.getUserDomain() + "_" + API_NAME;
        apiZip = new File(zipTempDir.getAbsolutePath() + File.separator + fileName + ".zip");

        //open a client connection and save the file
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(exportRequest.toURI());
        get.addHeader(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                "Basic " + encodeCredentials(user.getUserName(), user.getPassword().toCharArray()));
        CloseableHttpResponse response = client.execute(get);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            FileOutputStream outStream = null;
            try {
                outStream = new FileOutputStream(apiZip);
                entity.writeTo(outStream);
            } finally {
                if (outStream != null) {
                    outStream.close();
                }
            }

        }
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK,
                "Response code is not as expected");

    }

    @Test(groups = { "wso2.am" }, description = "Importing exported API", dependsOnMethods = "testAPIExport")
    public void testAPIImport() throws Exception {

        //delete exported API before import
        HttpResponse serviceResponse = apiPublisher.deleteAPI(API_NAME, API_VERSION, user.getUserName());
        verifyResponse(serviceResponse);

        //open import API url connection and deploy the exported API
        URL url = new URL(importUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");

        FileBody fileBody = new FileBody(apiZip);
        MultipartEntity multipartEntity = new MultipartEntity(HttpMultipartMode.STRICT);
        multipartEntity.addPart("file", fileBody);

        connection.setRequestProperty("Content-Type", multipartEntity.getContentType().getValue());
        connection.setRequestProperty(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                "Basic " + encodeCredentials(user.getUserName(), user.getPassword().toCharArray()));
        OutputStream out = connection.getOutputStream();
        try {
            multipartEntity.writeTo(out);
        } finally {
            out.close();
        }
        int status = connection.getResponseCode();
        BufferedReader read = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String temp;
        StringBuilder response = new StringBuilder();
        while ((temp = read.readLine()) != null) {
            response.append(temp);
        }
        Assert.assertEquals(status, HttpStatus.SC_CREATED, "Response code is not as expected : " + response);
    }

    @Test(groups = {
            "wso2.am" }, description = "Checking status of the imported API", dependsOnMethods = "testAPIImport")
    public void testAPIState() throws Exception {
        //get the imported API information
        HttpResponse response = apiPublisher.getAPI(API_NAME, user.getUserName(), API_VERSION);
        verifyResponse(response);
        JSONObject responseObj = new JSONObject(response.getData());
        JSONObject apiObj = responseObj.getJSONObject("api");

        String state = apiObj.getString("status");
        Assert.assertEquals(state, APILifeCycleState.CREATED.getState(), "Imported API not in Created state");
        Assert.assertEquals(API_NAME, apiObj.getString("name"), "Imported API Name is incorrect");
        Assert.assertEquals(API_VERSION, apiObj.getString("version"), "Imported API version is incorrect");
        Assert.assertEquals(DESCRIPTION, apiObj.getString("description"), "Imported API description is incorrect");
        Assert.assertTrue(tags.contains(TAG1), "Imported API not contain tag: " + TAG1);
        Assert.assertTrue(tags.contains(TAG2), "Imported API not contain tag: " + TAG2);
        Assert.assertTrue(tags.contains(TAG3), "Imported API not contain tag: " + TAG3);
        Assert.assertTrue(
                apiObj.getString("availableTiersDisplayNames").contains(APIMIntegrationConstants.API_TIER.GOLD),
                "Imported API not contain Tier: " + APIMIntegrationConstants.API_TIER.GOLD);
        Assert.assertTrue(
                apiObj.getString("availableTiersDisplayNames").contains(APIMIntegrationConstants.API_TIER.BRONZE),
                "Imported API not contain Tier: " + APIMIntegrationConstants.API_TIER.BRONZE);
        Assert.assertTrue(
                apiObj.getString("availableTiersDisplayNames").contains(APIMIntegrationConstants.API_TIER.SILVER),
                "Imported API not contain Tier: " + APIMIntegrationConstants.API_TIER.SILVER);
        Assert.assertTrue(
                apiObj.getString("availableTiersDisplayNames").contains(APIMIntegrationConstants.API_TIER.UNLIMITED),
                "Imported API not contain Tier: " + APIMIntegrationConstants.API_TIER.UNLIMITED);
        Assert.assertEquals("checked", apiObj.getString("transport_http"),
                "Imported API HTTP transport status is incorrect");
        Assert.assertEquals("checked", apiObj.getString("transport_https"),
                "Imported API HTTPS transport status is incorrect");
        Assert.assertEquals("Disabled", apiObj.getString("responseCache"),
                "Imported API response Cache status is incorrect");
        Assert.assertEquals("Disabled", apiObj.getString("destinationStats"),
                "Imported API destination Stats status is incorrect");
        Assert.assertEquals("public", apiObj.getString("visibility"), "Imported API visibility is incorrect");
        Assert.assertEquals("false", apiObj.getString("isDefaultVersion"),
                "Imported API Default Version status is incorrect");

        JSONArray resourcesList = new JSONArray(apiObj.getString("resources"));

        Assert.assertEquals(resList.size(), resourcesList.length(), "Imported API not in Created state");
        String method = null, authType = null, tier = null, urlPattern = null;
        APIResourceBean res;
        for (int i = 0; i < resourcesList.length(); i++) {
            res = resList.get(i);
            JSONObject verb = resourcesList.getJSONObject(i).getJSONObject("http_verbs");
            Iterator it = verb.keys();
            if (it.hasNext()) {
                method = (String) it.next();
                JSONObject resProp = verb.getJSONObject(method);
                authType = resProp.getString("auth_type");
                tier = resProp.getString("throttling_tier");
            }
            urlPattern = resourcesList.getJSONObject(i).getString("url_pattern");
            Assert.assertEquals(res.getResourceMethod(), method, "Imported API Resource method is incorrect");
            Assert.assertEquals(res.getResourceMethodAuthType(), authType,
                    "Imported API Resource Auth Type is incorrect");
            Assert.assertEquals(res.getResourceMethodThrottlingTier(), tier, "Imported API Resource Tier is incorrect");
            Assert.assertEquals(res.getUriTemplate(), urlPattern, "Imported API Resource URL template is incorrect");
        }
    }

    @Test(groups = {
            "wso2.am" }, description = "Implementing sample api for scope test", dependsOnMethods = "testAPIState")
    public void testNewAPICreation() throws Exception {
        String providerName = user.getUserName();

        apiCreationRequestBean = new APICreationRequestBean(NEW_API_NAME, NEW_API_CONTEXT, API_VERSION, providerName,
                new URL(endpointUrl));

        //adding resources using swagger
        String swagger = "{" + "\"paths\": {" + "\"/add\": {" + "\"get\": {" + "\"x-auth-type\": \"" + URLEncoder
                .encode(APIMIntegrationConstants.RESOURCE_AUTH_TYPE_APPLICATION_AND_APPLICATION_USER, "UTF-8") + "\","
                + "\"x-throttling-tier\": \"" + APIMIntegrationConstants.API_TIER.UNLIMITED + "\"," + "\"x-scope\": \""
                + SCOPE_NAME + "\"," + "\"responses\": {" + "\"200\": {}" + "}," + "\"parameters\": [{"
                + "\"name\": \"x\"," + "\"paramType\": \"query\"," + "\"required\": false," + "\"type\": \"string\","
                + "\"description\": \"First value\"," + "\"in\": \"query\"" + "}, {" + "\"name\": \"y\","
                + "\"paramType\": \"query\"," + "\"required\": false," + "\"type\": \"string\","
                + "\"description\": \"Second Value\"," + "\"in\": \"query\"" + "}]" + "}" + "}" + "},"
                + "\"swagger\": \"2.0\"," + "\"x-wso2-security\": {" + "\"apim\": {" + "\"x-wso2-scopes\": [{"
                + "\"description\": \"Sample Scope\"," + "\"name\": \"" + SCOPE_NAME + "\"," + "\"roles\": \""
                + ALLOWED_ROLE + "\"," + "\"key\": \"" + SCOPE_NAME + "\"" + "}]" + "}" + "}," + "\"info\": {"
                + "\"title\": \"" + NEW_API_NAME + "\"," + "\"" + API_VERSION + "\": \"1.0.0\"" + "}" + "}";

        apiCreationRequestBean.setSwagger(swagger);
        apiCreationRequestBean.setVisibility("restricted");
        apiCreationRequestBean.setRoles(VISIBILITY_ROLE);

        //add test api
        HttpResponse serviceResponse = apiPublisher.addAPI(apiCreationRequestBean);
        verifyResponse(serviceResponse);

        //publish the api
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(NEW_API_NAME, user.getUserName(),
                APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);

    }

    @Test(groups = { "wso2.am" }, description = "Invoke the API before export", dependsOnMethods = "testNewAPICreation")
    public void testNewAPIInvoke() throws Exception {

        apiStore = new APIStoreRestClient(storeURLHttp);
        apiStore.login(ALLOWED_USER, String.valueOf(ALLOWED_USER_PASS));
        //add a application
        HttpResponse serviceResponse = apiStore
                .addApplication(APP_NAME, APIThrottlingTier.UNLIMITED.getState(), "", "this-is-test");
        verifyResponse(serviceResponse);

        String provider = user.getUserName();

        //subscribe to the api
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(NEW_API_NAME, provider);
        subscriptionRequest.setApplicationName(APP_NAME);
        subscriptionRequest.setTier(APIMIntegrationConstants.API_TIER.GOLD);
        serviceResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(serviceResponse);

        //generate the key for the subscription
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(APP_NAME);
        generateAppKeyRequest.setTokenScope(SCOPE_NAME);
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Assert.assertNotNull("Access Token not found " + responseString, accessToken);

        //invoke api
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        String invokeURL = getAPIInvocationURLHttp(NEW_API_CONTEXT, API_VERSION);
        serviceResponse = HttpRequestUtil.doGet(invokeURL + "/add?x=1&y=1", requestHeaders);
        Assert.assertEquals(HttpStatus.SC_OK, serviceResponse.getResponseCode(), "Imported API not in Created state");
    }

    @Test(groups = {
            "wso2.am" }, description = "Exporting above created new API", dependsOnMethods = "testNewAPIInvoke")
    public void testNewAPIExport() throws Exception {

        //export api
        URL exportRequest = new URL(
                exportUrl + "?name=" + NEW_API_NAME + "&version=" + API_VERSION + "&provider=" + user.getUserName());
        String fileName = user.getUserDomain() + "_" + NEW_API_NAME;
        newApiZip = new File(zipTempDir.getAbsolutePath() + File.separator + fileName + ".zip");

        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(exportRequest.toURI());
        get.addHeader(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                "Basic " + encodeCredentials(user.getUserName(), user.getPassword().toCharArray()));
        CloseableHttpResponse response = client.execute(get);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            FileOutputStream outStream = new FileOutputStream(newApiZip);
            try {
                entity.writeTo(outStream);
            } finally {
                outStream.close();
            }
        }
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK,
                "Response code is not as expected");

    }

    @Test(groups = { "wso2.am" }, description = "Importing new API", dependsOnMethods = "testNewAPIExport")
    public void testNewAPIImport() throws Exception {
        //remove existing application and api
        HttpResponse serviceResponse = apiStore.removeApplication(APP_NAME);
        verifyResponse(serviceResponse);
        serviceResponse = apiPublisher.deleteAPI(NEW_API_NAME, API_VERSION, user.getUserName());
        verifyResponse(serviceResponse);

        //deploy exported API
        URL url = new URL(importUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");

        FileBody fileBody = new FileBody(newApiZip);
        MultipartEntity multipartEntity = new MultipartEntity(HttpMultipartMode.STRICT);
        multipartEntity.addPart("file", fileBody);

        connection.setRequestProperty("Content-Type", multipartEntity.getContentType().getValue());
        connection.setRequestProperty(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                "Basic " + encodeCredentials(user.getUserName(), user.getPassword().toCharArray()));
        OutputStream out = connection.getOutputStream();
        try {
            multipartEntity.writeTo(out);
        } finally {
            out.close();
        }
        int status = connection.getResponseCode();
        BufferedReader read = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String temp;
        StringBuilder response = new StringBuilder();
        while ((temp = read.readLine()) != null) {
            response.append(temp);
        }
        Assert.assertEquals(status, HttpStatus.SC_CREATED, "Response code is not as expected : " + response.toString());
        Assert.assertEquals(response.toString(), "API imported successfully.", "API importing is not successfully");
    }

    @Test(groups = {
            "wso2.am" }, description = "Checking newly imported API status", dependsOnMethods = "testNewAPIImport")
    public void testNewAPIState() throws Exception {
        //get the API information
        HttpResponse response = apiPublisher.getAPI(NEW_API_NAME, user.getUserName(), API_VERSION);
        verifyResponse(response);
        JSONObject responseObj = new JSONObject(response.getData());
        JSONObject apiObj = responseObj.getJSONObject("api");

        String state = apiObj.getString("status");
        Assert.assertEquals(state, APILifeCycleState.CREATED.getState(), "Imported API not in Created state");
        Assert.assertEquals(NEW_API_NAME, apiObj.getString("name"), "Imported API name is incorrect");
        Assert.assertEquals(API_VERSION, apiObj.getString("version"), "Imported API version is incorrect");
        Assert.assertEquals("restricted", apiObj.getString("visibility"), "Imported API Visibility is incorrect");
        String endpointConfig = apiObj.getString("endpointConfig");
        Assert.assertEquals(endpointUrl,
                new JSONObject(endpointConfig).getJSONObject("production_endpoints").getString("url"),
                "Imported API Endpoint url is incorrect");

    }

    @Test(groups = { "wso2.am" }, description = "Invoke the newly imported API", dependsOnMethods = "testNewAPIState")
    public void testNewAPIInvokeAfterImport() throws Exception {

        //publish the api
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(NEW_API_NAME, user.getUserName(),
                APILifeCycleState.PUBLISHED);
        HttpResponse serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);

        apiStore = new APIStoreRestClient(storeURLHttp);
        apiStore.login(DENIED_USER, String.valueOf(DENIED_USER_PASS));

        //add a application
        serviceResponse = apiStore
                .addApplication(NEW_APP_NAME, APIThrottlingTier.UNLIMITED.getState(), "", "this-is-test");
        verifyResponse(serviceResponse);

        String provider = user.getUserName();

        //subscribe to the api
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(NEW_API_NAME, provider);
        subscriptionRequest.setApplicationName(NEW_APP_NAME);
        subscriptionRequest.setTier(APIMIntegrationConstants.API_TIER.GOLD);
        serviceResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(serviceResponse);

        //generate the key for the subscription
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(NEW_APP_NAME);
        generateAppKeyRequest.setTokenScope(SCOPE_NAME);
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Assert.assertNotNull("Access Token not found " + responseString, accessToken);

        //invoke the API
        requestHeaders.clear();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        String invokeURL = getAPIInvocationURLHttp(NEW_API_CONTEXT, API_VERSION);
        serviceResponse = HttpRequestUtil.doGet(invokeURL + "/add?x=1&y=1", requestHeaders);
        Assert.assertEquals(HttpStatus.SC_FORBIDDEN, serviceResponse.getResponseCode(),
                "Imported API not in Created state");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {

        apiStore.removeApplication(NEW_APP_NAME);
        apiPublisher.deleteAPI(API_NAME, API_VERSION, user.getUserName());
        apiPublisher.deleteAPI(NEW_API_NAME, API_VERSION, user.getUserName());
        boolean deleteStatus;
        deleteStatus = apiZip.delete();
        Assert.assertTrue(deleteStatus, "temp file delete not successful");
        deleteStatus = newApiZip.delete();
        Assert.assertTrue(deleteStatus, "temp file delete not successful");
        deleteStatus = zipTempDir.delete();
        Assert.assertTrue(deleteStatus, "temp file delete not successful");
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN }, };
    }

    /**
     * get the base64 encoded username and password
     *
     * @param user username
     * @param pass password
     * @return encoded basic auth, as string
     */
    private String encodeCredentials(String user, char[] pass) {
        StringBuilder builder = new StringBuilder(user).append(':').append(pass);
        String cred = builder.toString();
        byte[] encodedBytes = Base64.encodeBase64(cred.getBytes());
        return new String(encodedBytes);
    }
}
