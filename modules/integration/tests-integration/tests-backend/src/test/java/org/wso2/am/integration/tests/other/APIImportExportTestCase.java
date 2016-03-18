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
import java.util.*;

@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
public class APIImportExportTestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(APIImportExportTestCase.class);
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;

    private String apiName = "APIImportExportTestCaseAPIName";
    private String newApiName = "NewAPIImportExportTestCaseAPIName";
    private String APIContext = "APIImportExportTestCaseContext";
    private String NEWAPIContext = "NewAPIImportExportTestCaseContext";
    private String tags;
    private String tag1 = "import";
    private String tag2 = "export";
    private String tag3 = "test";
    private String tierCollection;
    private String endpointUrl;
    private String description = "This is test API create by API manager integration test";
    private String APIVersion = "1.0.0";
    private String appName = "APIImportExportTestCaseApp";
    private Map<String, String> requestHeaders = new HashMap<String, String>();
    private APIRequest apiRequest;

    private String publisherURLHttp;
    private String storeURLHttp;
    private File zipTempDir, apiZip, newApiZip;
    private String importUrl = "https://localhost:9443/api-import-export-1.0.1/import-api";
    private String exportUrl = "https://localhost:9443/api-import-export-1.0.1/export-api";
    private APICreationRequestBean apiCreationRequestBean;
    private List<APIResourceBean> resList;
    private UserManagementClient userManagementClient1;
    private final String ALLOWED_ROLE = "allowedRole";
    private final String NOT_ALLOWED_ROLE = "denyRole";
    private final String[] permissions = { "/permission/admin/login", "/permission/admin/manage/api/subscribe" };
    private final String ALLOWED_USER = "allowedUser";
    private final char[] ALLOWED_USER_PASS = "pass@123".toCharArray();
    private final String DENIED_USER = "deniedUser";
    private final char[] DENIED_USER_PASS = "pass@123".toCharArray();
    private final String SCOPE_NAME = "ImportExportScope";

    @Factory(dataProvider = "userModeDataProvider")
    public APIImportExportTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {

        //        super.init();
        /*ContextUrls contextUrls = new ContextUrls();
        contextUrls.setWebAppURL("http://localhost:9763/");
        contextUrls.setWebAppURLHttps("https://localhost:9443/");
        contextUrls.setServiceUrl("http://localhost:9470/");
        contextUrls.setSecureServiceUrl("http://localhost:9470/");
        contextUrls.setBackEndUrl("http://localhost:9470/");

        APIMURLBean bean = new APIMURLBean(contextUrls);
        publisherUrls = bean;
        storeUrls = bean;

        User user = new User();
        user.setUserName("admin");
        user.setPassword("admin");
        super.user = user;*/

        super.init(userMode);

        publisherURLHttp = getPublisherURLHttps();//set s
        storeURLHttp = getStoreURLHttp();
        endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api";
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);
        apiPublisher.login(user.getUserName(), user.getPassword());
        apiStore = new APIStoreRestClient(storeURLHttp);
        apiStore.login(user.getUserName(), user.getPassword());

        tags = tag1 + "," + tag2 + "," + tag3;
        tierCollection = APIMIntegrationConstants.API_TIER.BRONZE + "," + APIMIntegrationConstants.API_TIER.GOLD + ","
                + APIMIntegrationConstants.API_TIER.SILVER + "," + APIMIntegrationConstants.API_TIER.UNLIMITED;
        importUrl = publisherURLHttp + "api-import-export-1.0.1/import-api";
        exportUrl = publisherURLHttp + "api-import-export-1.0.1/export-api";

        //adding new role and two users
        userManagementClient1 = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                createSession(keyManagerContext));
        userManagementClient1.addRole(ALLOWED_ROLE, null, permissions);
        userManagementClient1.addRole(NOT_ALLOWED_ROLE, null, permissions);
        userManagementClient1
                .addUser(ALLOWED_USER, String.valueOf(ALLOWED_USER_PASS), new String[] { ALLOWED_ROLE }, null);
        userManagementClient1
                .addUser(DENIED_USER, String.valueOf(DENIED_USER_PASS), new String[] { NOT_ALLOWED_ROLE }, null);
    }

    @Test(groups = { "wso2.am" }, description = "Sample API creation and subscribe")
    public void testAPICreation() throws Exception {
        String providerName = user.getUserName();

        apiCreationRequestBean = new APICreationRequestBean(apiName, APIContext, APIVersion, providerName,
                new URL(exportUrl));
        apiCreationRequestBean.setTags(tags);
        apiCreationRequestBean.setDescription(description);
        apiCreationRequestBean.setTiersCollection(tierCollection);

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
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(apiName, user.getUserName(),
                APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);

    }

    @Test(groups = {
            "wso2.am" }, description = "Sample API creation and subscribe", dependsOnMethods = "testAPICreation")
    public void testAPIExport() throws Exception {

        System.setProperty("javax.net.ssl.trustStore", "/home/rukshan/wso2-jks/wso2carbon.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");

        URL exportRequest = new URL(
                exportUrl + "?name=" + apiName + "&version=" + APIVersion + "&provider=" + user.getUserName());
        zipTempDir = Files.createTempDir();

        String fileName = user.getUserDomain() + "_" + apiName;
        apiZip = new File(zipTempDir.getAbsolutePath() + File.separator + fileName + ".zip");

        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(exportRequest.toURI());
        get.addHeader("Authorization",
                "Basic " + encodeCredentials(user.getUserName(), user.getPassword().toCharArray()));
        CloseableHttpResponse response = client.execute(get);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            FileOutputStream outStream = new FileOutputStream(apiZip);
            entity.writeTo(outStream);

        }
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK,
                "Response code is not as expected");

    }

    @Test(groups = { "wso2.am" }, description = "Sample API creation and subscribe", dependsOnMethods = "testAPIExport")
    public void testAPIImport() throws Exception {

        HttpResponse serviceResponse = apiPublisher.deleteAPI(apiName, APIVersion, user.getUserName());
        verifyResponse(serviceResponse);

        URL url = new URL(importUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");

        FileBody fileBody = new FileBody(apiZip);
        MultipartEntity multipartEntity = new MultipartEntity(HttpMultipartMode.STRICT);
        multipartEntity.addPart("file", fileBody);

        connection.setRequestProperty("Content-Type", multipartEntity.getContentType().getValue());
        connection.setRequestProperty("Authorization",
                "Basic " + encodeCredentials(user.getUserName(), user.getPassword().toCharArray()));
        OutputStream out = connection.getOutputStream();
        try {
            multipartEntity.writeTo(out);
        } finally {
            out.close();
        }
        int status = connection.getResponseCode();
        BufferedReader read = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String response = "", s;
        while ((s = read.readLine()) != null) {
            response += s;
        }
        Assert.assertEquals(status, HttpStatus.SC_CREATED, "Response code is not as expected : " + response);
        Assert.assertEquals(response, "API imported successfully.", "API importing is not successfully");
    }

    @Test(groups = { "wso2.am" }, description = "Sample API creation and subscribe", dependsOnMethods = "testAPIImport")
    public void testAPIState() throws Exception {
        HttpResponse response = apiPublisher.getAPI(apiName, user.getUserName(), APIVersion);
        verifyResponse(response);
        JSONObject responseObj = new JSONObject(response.getData());
        JSONObject apiObj = responseObj.getJSONObject("api");

        String state = apiObj.getString("status");
        Assert.assertEquals(state, APILifeCycleState.CREATED.getState(), "Imported API not in Created state");
        Assert.assertEquals(apiName, apiObj.getString("name"), "Imported API not in Created state");
        Assert.assertEquals(APIVersion, apiObj.getString("version"), "Imported API not in Created state");
        Assert.assertEquals(description, apiObj.getString("description"), "Imported API not in Created state");
        Assert.assertTrue(tags.contains(tag1), "Imported API not in Created state");
        Assert.assertTrue(tags.contains(tag2), "Imported API not in Created state");
        Assert.assertTrue(tags.contains(tag3), "Imported API not in Created state");
        Assert.assertTrue(
                apiObj.getString("availableTiersDisplayNames").contains(APIMIntegrationConstants.API_TIER.GOLD),
                "Imported API not in Created state");
        Assert.assertTrue(
                apiObj.getString("availableTiersDisplayNames").contains(APIMIntegrationConstants.API_TIER.BRONZE),
                "Imported API not in Created state");
        Assert.assertTrue(
                apiObj.getString("availableTiersDisplayNames").contains(APIMIntegrationConstants.API_TIER.SILVER),
                "Imported API not in Created state");
        Assert.assertTrue(
                apiObj.getString("availableTiersDisplayNames").contains(APIMIntegrationConstants.API_TIER.UNLIMITED),
                "Imported API not in Created state");
        Assert.assertEquals("checked", apiObj.getString("transport_http"), "Imported API not in Created state");
        Assert.assertEquals("checked", apiObj.getString("transport_https"), "Imported API not in Created state");
        Assert.assertEquals("Disabled", apiObj.getString("responseCache"), "Imported API not in Created state");
        Assert.assertEquals("Disabled", apiObj.getString("destinationStats"), "Imported API not in Created state");
        Assert.assertEquals("Disabled", apiObj.getString("responseCache"), "Imported API not in Created state");
        Assert.assertEquals("public", apiObj.getString("visibility"), "Imported API not in Created state");
        Assert.assertEquals("false", apiObj.getString("isDefaultVersion"), "Imported API not in Created state");

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
            Assert.assertEquals(res.getResourceMethod(), method, "Imported API not in Created state");
            Assert.assertEquals(res.getResourceMethodAuthType(), authType, "Imported API not in Created state");
            Assert.assertEquals(res.getResourceMethodThrottlingTier(), tier, "Imported API not in Created state");
            Assert.assertEquals(res.getUriTemplate(), urlPattern, "Imported API not in Created state");
        }
    }

    @Test(groups = { "wso2.am" }, description = "Sample API creation and subscribe", dependsOnMethods = "testAPIState")
    public void testNewAPICreation() throws Exception {
        String providerName = user.getUserName();

        apiCreationRequestBean = new APICreationRequestBean(newApiName, NEWAPIContext, APIVersion, providerName,
                new URL(endpointUrl));

        String swagger = "{\n" + "\t\"paths\": {\n" + "\t\t\"/ADD\": {\t\t\t\n" + "\t\t\t\"get\": {\n"
                + "\t\t\t\t\"x-auth-type\": \""
                + APIMIntegrationConstants.RESOURCE_AUTH_TYPE_APPLICATION_AND_APPLICATION_USER + "\",\n"
                + "\t\t\t\t\"x-throttling-tier\": \"" + APIMIntegrationConstants.API_TIER.UNLIMITED + "\",\n"
                + "\t\t\t\t\"x-scope\": \"" + SCOPE_NAME + "\",\n" + "\t\t\t\t\"responses\": {\n"
                + "\t\t\t\t\t\"200\": {}\n" + "\t\t\t\t},\n" + "\t\t\t\t\"parameters\": [{\n"
                + "\t\t\t\t\t\"name\": \"x\",\n" + "\t\t\t\t\t\"paramType\": \"query\",\n"
                + "\t\t\t\t\t\"required\": false,\n" + "\t\t\t\t\t\"type\": \"string\",\n"
                + "\t\t\t\t\t\"description\": \"First value\",\n" + "\t\t\t\t\t\"in\": \"query\"\n" + "\t\t\t\t}, {\n"
                + "\t\t\t\t\t\"name\": \"y\",\n" + "\t\t\t\t\t\"paramType\": \"query\",\n"
                + "\t\t\t\t\t\"required\": false,\n" + "\t\t\t\t\t\"type\": \"string\",\n"
                + "\t\t\t\t\t\"description\": \"Second Value\",\n" + "\t\t\t\t\t\"in\": \"query\"\n" + "\t\t\t\t}]\n"
                + "\t\t\t}\n" + "\t\t}\n" + "\t},\n" + "\t\"swagger\": \"2.0\",\n" + "\t\"x-wso2-security\": {\n"
                + "\t\t\"apim\": {\n" + "\t\t\t\"x-wso2-scopes\": [{\n" + "\t\t\t\t\"description\": \"Sample Scope\",\n"
                + "\t\t\t\t\"name\": \"" + SCOPE_NAME + "\",\n" + "\t\t\t\t\"roles\": \"" + ALLOWED_ROLE + "\",\n"
                + "\t\t\t\t\"key\": \"" + SCOPE_NAME + "\"\n" + "\t\t\t}]\n" + "\t\t}\n" + "\t},\n" + "\t\"info\": {\n"
                + "\t\t\"title\": \"" + newApiName + "\",\n" + "\t\t\"" + APIVersion + "\": \"1.0.0\"\n" + "\t}\n"
                + "}";

        apiCreationRequestBean.setSwagger(swagger);
        apiCreationRequestBean.setVisibility("restricted");
        apiCreationRequestBean.setRoles(ALLOWED_ROLE);

        //add test api
        HttpResponse serviceResponse = apiPublisher.addAPI(apiCreationRequestBean);
        verifyResponse(serviceResponse);

        //publish the api
        APILifeCycleStateRequest updateRequest = new APILifeCycleStateRequest(newApiName, user.getUserName(),
                APILifeCycleState.PUBLISHED);
        serviceResponse = apiPublisher.changeAPILifeCycleStatus(updateRequest);
        verifyResponse(serviceResponse);

    }

    @Test(groups = {
            "wso2.am" }, description = "Sample API creation and subscribe", dependsOnMethods = "testNewAPICreation")
    public void testNewAPIInvoke() throws Exception {

        //add a application
        HttpResponse serviceResponse = apiStore
                .addApplication(appName, APIThrottlingTier.UNLIMITED.getState(), "", "this-is-test");
        verifyResponse(serviceResponse);

        String provider = user.getUserName();

        //subscribe to the api
        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(newApiName, provider);
        subscriptionRequest.setApplicationName(appName);
        subscriptionRequest.setTier(APIMIntegrationConstants.API_TIER.GOLD);
        serviceResponse = apiStore.subscribe(subscriptionRequest);
        verifyResponse(serviceResponse);

        //generate the key for the subscription
        APPKeyRequestGenerator generateAppKeyRequest = new APPKeyRequestGenerator(appName);
        generateAppKeyRequest.setTokenScope(SCOPE_NAME);
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken = response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Assert.assertNotNull("Access Token not found " + responseString, accessToken);

        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        String invokeURL = getAPIInvocationURLHttp(NEWAPIContext, APIVersion);
        serviceResponse = HttpRequestUtil.doGet(invokeURL, requestHeaders);
        System.out.println(serviceResponse.getResponseCode());
    }

    @Test(groups = {
            "wso2.am" }, description = "Sample API creation and subscribe", dependsOnMethods = "testNewAPIInvoke")
    public void testNewAPIExport() throws Exception {

        URL exportRequest = new URL(
                exportUrl + "?name=" + newApiName + "&version=" + APIVersion + "&provider=" + user.getUserName());
        String fileName = user.getUserDomain() + "_" + newApiName;
        newApiZip = new File(zipTempDir.getAbsolutePath() + File.separator + fileName + ".zip");

        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(exportRequest.toURI());
        get.addHeader("Authorization",
                "Basic " + encodeCredentials(user.getUserName(), user.getPassword().toCharArray()));
        CloseableHttpResponse response = client.execute(get);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            FileOutputStream outStream = new FileOutputStream(newApiZip);
            entity.writeTo(outStream);

        }
        Assert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK,
                "Response code is not as expected");

    }

    @Test(groups = {
            "wso2.am" }, description = "Sample API creation and subscribe", dependsOnMethods = "testNewAPIExport")
    public void testNewAPIImport() throws Exception {

        HttpResponse serviceResponse = apiPublisher.deleteAPI(newApiName, APIVersion, user.getUserName());
        verifyResponse(serviceResponse);

        URL url = new URL(importUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");

        FileBody fileBody = new FileBody(newApiZip);
        MultipartEntity multipartEntity = new MultipartEntity(HttpMultipartMode.STRICT);
        multipartEntity.addPart("file", fileBody);

        connection.setRequestProperty("Content-Type", multipartEntity.getContentType().getValue());
        connection.setRequestProperty("Authorization",
                "Basic " + encodeCredentials(user.getUserName(), user.getPassword().toCharArray()));
        OutputStream out = connection.getOutputStream();
        try {
            multipartEntity.writeTo(out);
        } finally {
            out.close();
        }
        int status = connection.getResponseCode();
        BufferedReader read = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String response = "", s;
        while ((s = read.readLine()) != null) {
            response += s;
        }
        Assert.assertEquals(status, HttpStatus.SC_CREATED, "Response code is not as expected : " + response);
        Assert.assertEquals(response, "API imported successfully.", "API importing is not successfully");
    }

    @Test(groups = {
            "wso2.am" }, description = "Sample API creation and subscribe", dependsOnMethods = "testNewAPIImport")
    public void testNewAPIState() throws Exception {
        HttpResponse response = apiPublisher.getAPI(newApiName, user.getUserName(), APIVersion);
        verifyResponse(response);
        JSONObject responseObj = new JSONObject(response.getData());
        JSONObject apiObj = responseObj.getJSONObject("api");

        String state = apiObj.getString("status");
        Assert.assertEquals(state, APILifeCycleState.CREATED.getState(), "Imported API not in Created state");
        Assert.assertEquals(newApiName, apiObj.getString("name"), "Imported API not in Created state");
        Assert.assertEquals(APIVersion, apiObj.getString("version"), "Imported API not in Created state");
        Assert.assertEquals("restricted", apiObj.getString("visibility"), "Imported API not in Created state");
        String endpointConfig = apiObj.getString("endpointConfig");
        Assert.assertEquals(endpointUrl,
                new JSONObject(endpointConfig).getJSONObject("production_endpoints").getString("url"),
                "Imported API not in Created state");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        //        apiStore.removeApplication(appName);
        apiPublisher.deleteAPI(apiName, APIVersion, user.getUserName());
        apiPublisher.deleteAPI(newApiName, APIVersion, user.getUserName());
        apiZip.delete();
        newApiZip.delete();
        zipTempDir.delete();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                //                                new Object[] { TestUserMode.TENANT_ADMIN },
        };
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
