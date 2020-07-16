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
import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIInfoDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.impl.RestAPIStoreImpl;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * This test case is used to test the API Manager Import Export tool
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL })
public class APIImportExportTestCase extends APIManagerLifecycleBaseTest {
    private final Log log = LogFactory.getLog(APIImportExportTestCase.class);
    private final String API_NAME = "APIImportExportTestCaseAPIName";
    private final String NEW_API_NAME = "NewAPIImportExportTestCaseAPIName";
    private final String PRESERVE_PUBLISHER_API_NAME = "preserveNewAPIImportExportAPIName";
    private final String NOT_PRESERVE_PUBLISHER_API_NAME = "notPreserveNewAPIImportExportAPIName";
    private final String PRESERVE_PUBLISHER_API_CONTEXT = "preserveAPIImportExportContext";
    private final String NOT_PRESERVE_PUBLISHER_API_CONTEXT = "notPreserveAPIImportExportContext";
    private final String API_CONTEXT = "APIImportExportTestCaseContext";
    private final String NEW_API_CONTEXT = "NewAPIImportExportTestCaseContext";
    private final String ALLOWED_ROLE = "allowedRole";
    private final String VISIBILITY_ROLE = "visibilityRole";
    private final String NOT_ALLOWED_ROLE = "denyRole";
    private final String ADMIN_ROLE = "admin";
    private final String INTERNAL_ROLE_SUBSCRIBER = "Internal/subscriber";
    private final String[] PERMISSIONS = { "/permission/admin/login", "/permission/admin/manage/api/subscribe" };
    private final char[] ALLOWED_USER_PASS = "pass@123".toCharArray();
    private final char[] PUBLISHER_USER_PASS = "pass@123".toCharArray();
    private final String SCOPE_NAME = "ImportExportScope";
    private final String TAG1 = "import";
    private final String TAG2 = "export";
    private final String TAG3 = "test";
    private final String DESCRIPTION = "This is test API create by API manager integration test";
    private final String UPDATED_DESCRIPTION = "This is the updated version of API create by API manager integration test";
    private final String API_VERSION = "1.0.0";
    private final String APP_NAME = "APIImportExportTestCaseApp";
    private final String NEW_APP_NAME = "newAPIImportExportTestCaseApp";
    private RestAPIStoreImpl allowedStoreUser;
    private String allowedUser = "allowedUser";
    private String publisherUser = "importExportPublisher";
    private String publisherURLHttps;
    private File zipTempDir, apiZip, newApiZip, preservePublisherApiZip, notPreservePublisherApiZip;
    private String importUrl;
    private String exportUrl;
    private APICreationRequestBean apiCreationRequestBean;
    private List<APIResourceBean> resList;
    private String tags;
    private String tierCollection;
    private String endpointUrl;
    private Map<String, String> requestHeaders = new HashMap<String, String>();
    private String apiId;
    private String newApiId;
    private String applicationId;
    private String newApplicationId;
    private String preservePublisherApiId;
    private String notPreservePublisherApiId;
    private ArrayList<String> grantTypes;

    @Factory(dataProvider = "userModeDataProvider")
    public APIImportExportTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        grantTypes = new ArrayList<>();
        publisherURLHttps = publisherUrls.getWebAppURLHttps();
        endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api";

        //concat tags
        tags = TAG1 + "," + TAG2 + "," + TAG3;
        tierCollection = APIMIntegrationConstants.API_TIER.BRONZE + "," + APIMIntegrationConstants.API_TIER.GOLD + ","
                + APIMIntegrationConstants.API_TIER.SILVER + "," + APIMIntegrationConstants.API_TIER.UNLIMITED;
        importUrl = publisherURLHttps + APIMIntegrationConstants.REST_API_ADMIN_CONTEXT_FULL_0
                + APIMIntegrationConstants.REST_API_ADMIN_IMPORT_API_RESOURCE;
        exportUrl = publisherURLHttps + APIMIntegrationConstants.REST_API_ADMIN_CONTEXT_FULL_0
                + APIMIntegrationConstants.REST_API_ADMIN_EXPORT_API_RESOURCE;

        //adding new 3 roles and two users
        userManagementClient = new UserManagementClient(keyManagerContext.getContextUrls().getBackEndUrl(),
                createSession(keyManagerContext));
        userManagementClient.addRole(ALLOWED_ROLE, null, PERMISSIONS);
        userManagementClient.addRole(NOT_ALLOWED_ROLE, null, PERMISSIONS);
        userManagementClient.addRole(VISIBILITY_ROLE, null, PERMISSIONS);

        userManagementClient.addUser(allowedUser, String.valueOf(ALLOWED_USER_PASS),
                new String[] { INTERNAL_ROLE_SUBSCRIBER, VISIBILITY_ROLE }, null);

        userManagementClient
                .addUser(publisherUser, String.valueOf(PUBLISHER_USER_PASS), new String[] { ADMIN_ROLE }, null);

        allowedStoreUser = new RestAPIStoreImpl(allowedUser, String.valueOf(ALLOWED_USER_PASS),
                keyManagerContext.getContextTenant().getDomain(), storeURLHttps);


        if (!keyManagerContext.getContextTenant().getDomain().equals("carbon.super")) {
            allowedUser = allowedUser + "@" + keyManagerContext.getContextTenant().getDomain();
            publisherUser = publisherUser + "@" + keyManagerContext.getContextTenant().getDomain();
        }

        createAndPublishAPI();

    }

    private void createAndPublishAPI() throws Exception {
        String providerName = user.getUserName();

        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION, providerName,
                new URL(exportUrl));
        apiCreationRequestBean.setTags(tags);
        apiCreationRequestBean.setDescription(DESCRIPTION);
        apiCreationRequestBean.setSubPolicyCollection(tierCollection);

        //define resources
        resList = new ArrayList<APIResourceBean>();
        APIResourceBean res1 = new APIResourceBean("POST",
                APIMIntegrationConstants.ResourceAuthTypes.NONE.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.TWENTYK_PER_MIN, "/post");
        APIResourceBean res2 = new APIResourceBean("GET",
                APIMIntegrationConstants.ResourceAuthTypes.APPLICATION.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.FIFTYK_PER_MIN, "/get");
        APIResourceBean res3 = new APIResourceBean("PUT",
                APIMIntegrationConstants.ResourceAuthTypes.APPLICATION_USER.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.TENK_PER_MIN, "/put");
        APIResourceBean res4 = new APIResourceBean("DELETE",
                APIMIntegrationConstants.ResourceAuthTypes.APPLICATION_AND_APPLICATION_USER.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED, "/delete");
        APIResourceBean res5 = new APIResourceBean("PATCH",
                APIMIntegrationConstants.ResourceAuthTypes.NONE.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.FIFTYK_PER_MIN, "/patch");
        APIResourceBean res6 = new APIResourceBean("HEAD",
                APIMIntegrationConstants.ResourceAuthTypes.NONE.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.FIFTYK_PER_MIN, "/head");
        APIResourceBean res7 = new APIResourceBean("OPTIONS",
                APIMIntegrationConstants.ResourceAuthTypes.NONE.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.FIFTYK_PER_MIN, "/options");
        resList.add(res1);
        resList.add(res2);
        resList.add(res3);
        resList.add(res4);
        resList.add(res5);
        resList.add(res6);
        resList.add(res7);
        apiCreationRequestBean.setResourceBeanList(resList);

        //add test api
        APIDTO apiDto = createAndPublishAPI(apiCreationRequestBean, restAPIPublisher, false);
        apiId = apiDto.getId();

    }

    @Test(groups = { "wso2.am" }, description = "Exported Sample API")
    public void testAPIExport() throws Exception {

        //construct export API url
        URL exportRequest =
                new URL(exportUrl + "?name=" + API_NAME + "&version=" + API_VERSION + "&providerName=" + user
                        .getUserName() + "&format=JSON");
        zipTempDir = Files.createTempDir();

        //set the export file name with tenant prefix
        String fileName = user.getUserDomain() + "_" + API_NAME;
        apiZip = new File(zipTempDir.getAbsolutePath() + File.separator + fileName + ".zip");
        //save the exported API
        exportAPI(exportRequest, apiZip);
    }

    @Test(groups = { "wso2.am" }, description = "Importing exported API", dependsOnMethods = "testAPIExport")
    public void testAPIImport() throws Exception {
        //delete exported API before import
        HttpResponse serviceResponse = restAPIPublisher.deleteAPI(apiId);
        assertEquals(serviceResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "API delete failed");
        //upload the exported zip
        importAPI(importUrl, apiZip, user.getUserName(), user.getPassword().toCharArray());
        waitForAPIDeployment();
    }

    @Test(groups = {
            "wso2.am" }, description = "Checking status of the imported API", dependsOnMethods = "testAPIImport")
    public void testAPIState() throws Exception {
        //get the imported API information
        APIDTO apiObj = getAPI(API_NAME, API_VERSION, user.getUserName());
        apiId = apiObj.getId();

        String state = apiObj.getLifeCycleStatus();
        assertEquals(state, APILifeCycleState.PUBLISHED.getState().toUpperCase(),
                "Imported API not in Published state");
        assertEquals(API_NAME, apiObj.getName(), "Imported API Name is incorrect");
        assertEquals(API_VERSION, apiObj.getVersion(), "Imported API version is incorrect");
        assertEquals(DESCRIPTION, apiObj.getDescription(), "Imported API description is incorrect");
        assertEquals("/" + API_CONTEXT, apiObj.getContext().replace("/t/wso2.com", ""),
                "Imported API context is incorrect");
        List<String> tagList = apiObj.getTags();
        Assert.assertTrue(tagList.contains(TAG1), "Imported API not contain tag: " + TAG1);
        Assert.assertTrue(tagList.contains(TAG2), "Imported API not contain tag: " + TAG2);
        Assert.assertTrue(tagList.contains(TAG3), "Imported API not contain tag: " + TAG3);
        Assert.assertTrue(
                apiObj.getPolicies().contains(APIMIntegrationConstants.API_TIER.GOLD),
                "Imported API not contain Tier: " + APIMIntegrationConstants.API_TIER.GOLD);
        Assert.assertTrue(
                apiObj.getPolicies().contains(APIMIntegrationConstants.API_TIER.BRONZE),
                "Imported API not contain Tier: " + APIMIntegrationConstants.API_TIER.BRONZE);
        Assert.assertTrue(
                apiObj.getPolicies().contains(APIMIntegrationConstants.API_TIER.SILVER),
                "Imported API not contain Tier: " + APIMIntegrationConstants.API_TIER.SILVER);
        Assert.assertTrue(
                apiObj.getPolicies().contains(APIMIntegrationConstants.API_TIER.UNLIMITED),
                "Imported API not contain Tier: " + APIMIntegrationConstants.API_TIER.UNLIMITED);
        Assert.assertTrue(apiObj.getTransport().contains(Constants.PROTOCOL_HTTP),
                "Imported API HTTP transport status is incorrect");
        Assert.assertTrue(apiObj.getTransport().contains(Constants.PROTOCOL_HTTPS),
                "Imported API HTTP transport status is incorrect");
        Assert.assertFalse(apiObj.isResponseCachingEnabled(),
                "Imported API response Cache status is incorrect");
        assertEquals(APIDTO.VisibilityEnum.PUBLIC, apiObj.getVisibility(), "Imported API visibility is incorrect");
        Assert.assertFalse(apiObj.isIsDefaultVersion(),
                "Imported API Default Version status is incorrect");

        List<APIOperationsDTO> apiOperationsDTOList = apiObj.getOperations();

        assertEquals(resList.size(), apiOperationsDTOList.size(), "Imported API not in Created state");
        String method = null, authType = null, tier = null, urlPattern = null;
        APIResourceBean res;
        for (int i = 0; i < resList.size(); i++) {
            res = resList.get(i);
            for (APIOperationsDTO apiOperationsDTO: apiOperationsDTOList) {
                method = apiOperationsDTO.getVerb();
                if (StringUtils.equals(res.getResourceMethod(), method)) {
                    authType = apiOperationsDTO.getAuthType();
                    tier = apiOperationsDTO.getThrottlingPolicy();
                    urlPattern = apiOperationsDTO.getTarget();
                    break;
                }
            }
            assertEquals(res.getResourceMethod(), method, "Imported API Resource method is incorrect");
            //Need to uncomment this after fixing product-apim/issues/6859
            //Assert.assertEquals(res.getResourceMethodThrottlingTier(), tier, "Imported API Resource Tier is incorrect");
            assertEquals(res.getUriTemplate(), urlPattern, "Imported API Resource URL template is incorrect");
        }
    }

    @Test(groups = { "wso2.am" }, description = "Update imported Sample API",dependsOnMethods = "testAPIState")
    public void testAPIUpdate() throws Exception {
        //get the imported API information
        APIDTO apiObj = getAPI(API_NAME, API_VERSION, user.getUserName());
        apiId = apiObj.getId();

        //Update imported API information
        apiObj.setDescription(UPDATED_DESCRIPTION);
        restAPIPublisher.updateAPI(apiObj);
    }

    @Test(groups = {
            "wso2.am" }, description = "Checking status of the updated API", dependsOnMethods = "testAPIUpdate")
    public void testAPIStateAfterUpdate() throws Exception {
        //get the updated API information
        APIDTO updatedApiObj = getAPI(API_NAME, API_VERSION, user.getUserName());
        apiId = updatedApiObj.getId();

        String state = updatedApiObj.getLifeCycleStatus();
        assertEquals(state, APILifeCycleState.PUBLISHED.getState().toUpperCase(),
                "Imported API not in Published state");
        assertEquals(API_NAME, updatedApiObj.getName(), "Imported API Name is incorrect");
        assertEquals(API_VERSION, updatedApiObj.getVersion(), "Imported API version is incorrect");
        assertEquals(UPDATED_DESCRIPTION, updatedApiObj.getDescription(), "Imported API description is incorrect");
        List<String> tagList = updatedApiObj.getTags();
        Assert.assertTrue(tagList.contains(TAG1), "Imported API not contain tag: " + TAG1);
        Assert.assertTrue(tagList.contains(TAG2), "Imported API not contain tag: " + TAG2);
        Assert.assertTrue(tagList.contains(TAG3), "Imported API not contain tag: " + TAG3);
        Assert.assertTrue(
                updatedApiObj.getPolicies().contains(APIMIntegrationConstants.API_TIER.GOLD),
                "Imported API not contain Tier: " + APIMIntegrationConstants.API_TIER.GOLD);
        Assert.assertTrue(
                updatedApiObj.getPolicies().contains(APIMIntegrationConstants.API_TIER.BRONZE),
                "Imported API not contain Tier: " + APIMIntegrationConstants.API_TIER.BRONZE);
        Assert.assertTrue(
                updatedApiObj.getPolicies().contains(APIMIntegrationConstants.API_TIER.SILVER),
                "Imported API not contain Tier: " + APIMIntegrationConstants.API_TIER.SILVER);
        Assert.assertTrue(
                updatedApiObj.getPolicies().contains(APIMIntegrationConstants.API_TIER.UNLIMITED),
                "Imported API not contain Tier: " + APIMIntegrationConstants.API_TIER.UNLIMITED);
        Assert.assertTrue(updatedApiObj.getTransport().contains(Constants.PROTOCOL_HTTP),
                "Imported API HTTP transport status is incorrect");
        Assert.assertTrue(updatedApiObj.getTransport().contains(Constants.PROTOCOL_HTTPS),
                "Imported API HTTP transport status is incorrect");
        Assert.assertFalse(updatedApiObj.isResponseCachingEnabled(),
                "Imported API response Cache status is incorrect");
        assertEquals(APIDTO.VisibilityEnum.PUBLIC, updatedApiObj.getVisibility(), "Imported API visibility is incorrect");
        Assert.assertFalse(updatedApiObj.isIsDefaultVersion(),
                "Imported API Default Version status is incorrect");

        List<APIOperationsDTO> apiOperationsDTOList = updatedApiObj.getOperations();

        assertEquals(resList.size(), apiOperationsDTOList.size(), "Imported API not in Created state");
        String method = null, authType = null, tier = null, urlPattern = null;
        APIResourceBean res;
        for (int i = 0; i < resList.size(); i++) {
            res = resList.get(i);
            for (APIOperationsDTO apiOperationsDTO: apiOperationsDTOList) {
                method = apiOperationsDTO.getVerb();
                if (StringUtils.equals(res.getResourceMethod(), method)) {
                    authType = apiOperationsDTO.getAuthType();
                    tier = apiOperationsDTO.getThrottlingPolicy();
                    urlPattern = apiOperationsDTO.getTarget();
                    break;
                }
            }
            assertEquals(res.getResourceMethod(), method, "Imported API Resource method is incorrect");
            //Need to uncomment this after fixing product-apim/issues/6859
            //Assert.assertEquals(res.getResourceMethodThrottlingTier(), tier, "Imported API Resource Tier is incorrect");
            assertEquals(res.getUriTemplate(), urlPattern, "Imported API Resource URL template is incorrect");
        }
    }

    @Test(groups = {
            "wso2.am" }, description = "Implementing sample api for scope test", dependsOnMethods = "testAPIStateAfterUpdate")
    public void testNewAPICreation() throws Exception {
        String providerName = user.getUserName();

        apiCreationRequestBean = new APICreationRequestBean(NEW_API_NAME, NEW_API_CONTEXT, API_VERSION, providerName,
                new URL(endpointUrl));

        //adding resources using swagger
        String resourceFile =
                "artifacts" + File.separator + "AM" + File.separator + "configFiles" + File.separator + "importExport"
                        + File.separator + "swagger.json";
        String swagger = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(resourceFile), "UTF-8");
        swagger = swagger.replaceAll("\\$authType",
                APIMIntegrationConstants.RESOURCE_AUTH_TYPE_APPLICATION_AND_APPLICATION_USER)
                .replaceAll("\\$tier", APIMIntegrationConstants.API_TIER.UNLIMITED)
                .replaceAll("\\$scope", SCOPE_NAME)
                .replaceAll("\\$roles", ALLOWED_ROLE)
                .replaceAll("\\$apiName", NEW_API_NAME)
                .replaceAll("\\$apiVersion", API_VERSION);

        apiCreationRequestBean.setSwagger(swagger);
        apiCreationRequestBean.setVisibility(APIDTO.VisibilityEnum.RESTRICTED.getValue());
        apiCreationRequestBean.setRoles(VISIBILITY_ROLE);

        //add test api and publish
        APIDTO apiDto = createAndPublishAPI(apiCreationRequestBean, restAPIPublisher, false);
        newApiId = apiDto.getId();

    }

    @Test(groups = { "wso2.am" }, description = "Invoke the API before export", dependsOnMethods = "testNewAPICreation")
    public void testNewAPIInvoke() throws Exception {

        //add an application
        HttpResponse applicationResponse = allowedStoreUser.createApplication(APP_NAME,
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();


        String provider = user.getUserName();

        //subscribe to the api
        HttpResponse subscribeResponse = subscribeToAPIUsingRest(newApiId, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED, allowedStoreUser);
        assertEquals(subscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of old API version request not successful " +
                        " API Name:" + NEW_API_NAME + " API Version:" + API_VERSION +
                        " API Provider Name :" + provider);


        //generate the key for the subscription
        //get access token
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = allowedStoreUser.generateKeys(applicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);

        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        Assert.assertNotNull("Access Token not found ", accessToken);

        //invoke api
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        String invokeURL = getAPIInvocationURLHttp(NEW_API_CONTEXT, API_VERSION);
        HttpResponse serviceResponse = HTTPSClientUtils.doGet(invokeURL + "/add?x=1&y=1", requestHeaders);
        assertEquals(HttpStatus.SC_OK, serviceResponse.getResponseCode(), "Imported API not in Created state");
    }

    @Test(groups = {
            "wso2.am" }, description = "Exporting above created new API", dependsOnMethods = "testNewAPIInvoke")
    public void testNewAPIExport() throws Exception {
        //export api
        URL exportRequest =
                new URL(exportUrl + "?name=" + NEW_API_NAME + "&version=" + API_VERSION + "&providerName=" + user
                        .getUserName() + "&format=JSON");
        String fileName = user.getUserDomain() + "_" + NEW_API_NAME;
        newApiZip = new File(zipTempDir.getAbsolutePath() + File.separator + fileName + ".zip");
        //save the exported API
        exportAPI(exportRequest, newApiZip);
    }

    @Test(groups = { "wso2.am" }, description = "Importing new API", dependsOnMethods = "testNewAPIExport")
    public void testNewAPIImport() throws Exception {
        //remove existing application and api
        allowedStoreUser.removeApplicationById(applicationId);
        HttpResponse serviceResponse = restAPIPublisher.deleteAPI(newApiId);
        assertEquals(serviceResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "API delete failed");
        //deploy exported API
        importAPI(importUrl, newApiZip, user.getUserName(), user.getPassword().toCharArray());
        waitForAPIDeployment();
    }

    @Test(groups = {
            "wso2.am" }, description = "Checking newly imported API status", dependsOnMethods = "testNewAPIImport")
    public void testNewAPIState() throws Exception {
        //get the imported API information
        APIDTO apiObj = getAPI(NEW_API_NAME, API_VERSION, user.getUserName());
        newApiId = apiObj.getId();

        String state = apiObj.getLifeCycleStatus();
        assertEquals(state, APILifeCycleState.PUBLISHED.getState().toUpperCase(),
                "Imported API not in Published state");
        assertEquals(NEW_API_NAME, apiObj.getName(), "Imported API Name is incorrect");
        assertEquals(API_VERSION, apiObj.getVersion(), "Imported API version is incorrect");
        assertEquals("/" + NEW_API_CONTEXT, apiObj.getContext().replace("/t/wso2.com", ""),
                "Imported API context is incorrect");

        assertEquals(APIDTO.VisibilityEnum.RESTRICTED, apiObj.getVisibility(), "Imported API Visibility is incorrect");
        String endpointConfig = apiObj.getEndpointConfig().toString();
        Assert.assertTrue(endpointConfig.contains(endpointUrl),
                "Imported API Endpoint url is incorrect");

    }

    @Test(groups = { "wso2.am" }, description = "Invoke the newly imported API", dependsOnMethods = "testNewAPIState")
    public void testNewAPIInvokeAfterImport() throws Exception {
        //add an application
        HttpResponse applicationResponse = allowedStoreUser.createApplication(NEW_APP_NAME,
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        newApplicationId = applicationResponse.getData();

        String provider = user.getUserName();
        //subscribe to the api
        HttpResponse subscribeResponse = subscribeToAPIUsingRest(newApiId, newApplicationId,
                APIMIntegrationConstants.API_TIER.GOLD, allowedStoreUser);
        assertEquals(subscribeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Subscribe of old API version request not successful " +
                        " API Name:" + NEW_API_NAME + " API Version:" + API_VERSION +
                        " API Provider Name :" + provider);

        //generate the key for the subscription
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = allowedStoreUser.generateKeys(newApplicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);

        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        Assert.assertNotNull("Access Token not found ", accessToken);

        //invoke the API
        requestHeaders.clear();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        String invokeURL = getAPIInvocationURLHttp(NEW_API_CONTEXT, API_VERSION);
        HttpResponse serviceResponse = HTTPSClientUtils.doGet(invokeURL + "/add?x=1&y=1", requestHeaders);
        assertEquals(HttpStatus.SC_OK, serviceResponse.getResponseCode(),
                "Imported API not in Created state");
    }

    @Test(groups = { "wso2.am" }, description = "Sample API creation", dependsOnMethods = "testNewAPIInvokeAfterImport")
    public void testPreserveProviderTrueAPICreation() throws Exception {
        String providerName = user.getUserName();
        apiCreationRequestBean = new APICreationRequestBean(PRESERVE_PUBLISHER_API_NAME, PRESERVE_PUBLISHER_API_CONTEXT,
                API_VERSION, providerName, new URL(exportUrl));
        apiCreationRequestBean.setTags(tags);
        apiCreationRequestBean.setDescription(DESCRIPTION);
        apiCreationRequestBean.setTiersCollection(tierCollection);
        //define resources
        resList = new ArrayList<>();
        APIResourceBean resource = new APIResourceBean("POST",
                APIMIntegrationConstants.ResourceAuthTypes.NONE.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.TWENTYK_PER_MIN, "/post");
        resList.add(resource);
        apiCreationRequestBean.setResourceBeanList(resList);
        //add test api and publish
        APIDTO apiDto = createAndPublishAPI(apiCreationRequestBean, restAPIPublisher, false);
        preservePublisherApiId = apiDto.getId();
    }

    @Test(groups = {
            "wso2.am" }, description = "Exported Sample API", dependsOnMethods = "testPreserveProviderTrueAPICreation")
    public void testPreserveProviderTrueApiExport() throws Exception {

        //construct export API url
        URL exportRequest = new URL(exportUrl + "?name=" + PRESERVE_PUBLISHER_API_NAME + "&version=" + API_VERSION
                + "&providerName=" + user.getUserName() + "&format=JSON");
        //set the export file name with tenant prefix
        String fileName = user.getUserDomain() + "_" + PRESERVE_PUBLISHER_API_NAME;
        preservePublisherApiZip = new File(zipTempDir.getAbsolutePath() + File.separator + fileName + ".zip");
        //save the exported API
        exportAPI(exportRequest, preservePublisherApiZip);
        HttpResponse serviceResponse = restAPIPublisher.deleteAPI(preservePublisherApiId);
        assertEquals(serviceResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "API delete failed");
    }

    @Test(groups = {
            "wso2.am" }, description = "Importing exported API", dependsOnMethods = "testPreserveProviderTrueApiExport")
    public void testPreserveProviderTrueSameProviderApiImport() throws Exception {
        //import the exported zip on same publisher
        importAPI(importUrl + "?preserveProvider=true", preservePublisherApiZip, user.getUserName(),
                user.getPassword().toCharArray());
        waitForAPIDeployment();
        //get the imported file information

        log.info("API ID before import: " + preservePublisherApiId);
        APIDTO apiObj = getAPI(PRESERVE_PUBLISHER_API_NAME, API_VERSION, user.getUserName());
        preservePublisherApiId = apiObj.getId();

        String provider = apiObj.getProvider();
        assertEquals(provider, user.getUserName(), "Provider is not as expected when 'preserveProvider'=true");

        //delete the existing API to import it again
        HttpResponse serviceResponse = restAPIPublisher.deleteAPI(preservePublisherApiId);
        assertEquals(serviceResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "API delete failed");

        //import the exported zip on different publisher
        importAPI(importUrl + "?preserveProvider=true", preservePublisherApiZip, publisherUser, PUBLISHER_USER_PASS);
        waitForAPIDeployment();
        //get the imported file information
        log.info("API ID  different publisher before import: " + preservePublisherApiId);
        apiObj = getAPI(PRESERVE_PUBLISHER_API_NAME, API_VERSION, user.getUserName());
        preservePublisherApiId = apiObj.getId();
        provider = apiObj.getProvider();
        assertEquals(provider, user.getUserName(), "Provider is not as expected when 'preserveProvider'=true");
    }

    @Test(groups = { "wso2.am" }, description = "Sample API creation",
            dependsOnMethods = "testPreserveProviderTrueSameProviderApiImport")
    public void testPreserveProviderFalseAPICreation() throws Exception {
        String providerName = user.getUserName();

        apiCreationRequestBean = new APICreationRequestBean(NOT_PRESERVE_PUBLISHER_API_NAME,
                NOT_PRESERVE_PUBLISHER_API_CONTEXT, API_VERSION, providerName, new URL(exportUrl));
        apiCreationRequestBean.setTags(tags);
        apiCreationRequestBean.setDescription(DESCRIPTION);
        apiCreationRequestBean.setTiersCollection(tierCollection);
        //define resources
        resList = new ArrayList<APIResourceBean>();
        APIResourceBean resource = new APIResourceBean("POST",
                APIMIntegrationConstants.ResourceAuthTypes.NONE.getAuthType(),
                APIMIntegrationConstants.RESOURCE_TIER.TWENTYK_PER_MIN, "/post");
        resList.add(resource);
        apiCreationRequestBean.setResourceBeanList(resList);
        //add test api and publish
        APIDTO apiDto = createAndPublishAPI(apiCreationRequestBean, restAPIPublisher, false);
        notPreservePublisherApiId = apiDto.getId();
    }

    @Test(groups = { "wso2.am" }, description = "Exported Sample API",
            dependsOnMethods = "testPreserveProviderFalseAPICreation")
    public void testPreserveProviderFalseApiExport() throws Exception {

        //construct export API url
        URL exportRequest = new URL(exportUrl + "?name=" + NOT_PRESERVE_PUBLISHER_API_NAME + "&version=" + API_VERSION
                + "&providerName=" + user.getUserName() + "&format=JSON");
        //set the export file name with tenant prefix
        String fileName = user.getUserDomain() + "_" + NOT_PRESERVE_PUBLISHER_API_NAME;
        notPreservePublisherApiZip = new File(zipTempDir.getAbsolutePath() + File.separator + fileName + ".zip");
        //save the exported API
        exportAPI(exportRequest, notPreservePublisherApiZip);
        HttpResponse serviceResponse = restAPIPublisher.deleteAPI(notPreservePublisherApiId);
        assertEquals(serviceResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "API delete failed");
    }

    @Test(groups = { "wso2.am" }, description = "Importing exported API",
            dependsOnMethods = "testPreserveProviderFalseApiExport")
    public void testPreserveProviderFalseSameProviderApiImport() throws Exception {
        //import the exported zip on same publisher
        importAPI(importUrl + "?preserveProvider=false", notPreservePublisherApiZip, user.getUserName(),
                user.getPassword().toCharArray());
        waitForAPIDeployment();
        //get the imported file information
        log.info("API ID before import: " + notPreservePublisherApiId);
        APIDTO apiObj = getAPI(NOT_PRESERVE_PUBLISHER_API_NAME, API_VERSION, user.getUserName());
        notPreservePublisherApiId = apiObj.getId();
        String provider = apiObj.getProvider();
        assertEquals(provider, user.getUserName(), "Provider is not as expected when 'preserveProvider'=false");

        HttpResponse serviceResponse = restAPIPublisher.deleteAPI(notPreservePublisherApiId);
        assertEquals(serviceResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "API delete failed");

        //import the exported zip on different publisher
        importAPI(importUrl + "?preserveProvider=false", notPreservePublisherApiZip, publisherUser,
                PUBLISHER_USER_PASS);
        waitForAPIDeployment();
        //get the imported file information
        log.info("API ID  different publisher before import: " + notPreservePublisherApiId);
        apiObj = getAPI(NOT_PRESERVE_PUBLISHER_API_NAME, API_VERSION, publisherUser);
        notPreservePublisherApiId = apiObj.getId();
        provider = apiObj.getProvider();
        assertEquals(provider, publisherUser, "Provider is not as expected when 'preserveProvider'=false");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        allowedStoreUser.deleteApplication(applicationId);
        allowedStoreUser.deleteApplication(newApplicationId);
        restAPIPublisher.deleteAPI(apiId);
        restAPIPublisher.deleteAPI(newApiId);
        restAPIPublisher.deleteAPI(preservePublisherApiId);
        restAPIPublisher.deleteAPI(notPreservePublisherApiId);
        boolean deleteStatus;
        deleteStatus = apiZip.delete();
        Assert.assertTrue(deleteStatus, "temp file delete not successful");
        deleteStatus = newApiZip.delete();
        Assert.assertTrue(deleteStatus, "temp file delete not successful");
        deleteStatus = preservePublisherApiZip.delete();
        Assert.assertTrue(deleteStatus, "temp file delete not successful");
        deleteStatus = notPreservePublisherApiZip.delete();
        Assert.assertTrue(deleteStatus, "temp file delete not successful");
        deleteStatus = zipTempDir.delete();
        Assert.assertTrue(deleteStatus, "temp directory delete not successful");
        super.cleanUp();
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }, };
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

    /**
     * Save file from a given URL
     *
     * @param exportRequest URL of the file location
     * @param fileName      expected File to be saved
     * @throws URISyntaxException throws if URL is malformed
     * @throws IOException        throws if connection issues occurred
     */
    private void exportAPI(URL exportRequest, File fileName) throws URISyntaxException, IOException {
        CloseableHttpClient client = HTTPSClientUtils.getHttpsClient();
        HttpGet get = new HttpGet(exportRequest.toURI());
        get.addHeader(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                      "Basic " + encodeCredentials(user.getUserName(), user.getPassword().toCharArray()));
        CloseableHttpResponse response = client.execute(get);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            FileOutputStream outStream = new FileOutputStream(fileName);
            try {
                entity.writeTo(outStream);
            } finally {
                outStream.close();
            }
        }

        assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK, "Response code is not as expected");
        Assert.assertTrue(fileName.exists(), "File save was not successful");
    }

    /**
     * Upload a file to the given URL
     *
     * @param importUrl URL to be file upload
     * @param fileName  Name of the file to be upload
     * @throws IOException throws if connection issues occurred
     */
    private static void importAPI(String importUrl, File fileName, String user, char[] pass) throws IOException {
        //open import API url connection and deploy the exported API
        URL url = new URL(importUrl);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        });
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");

        FileBody fileBody = new FileBody(fileName);
        MultipartEntity multipartEntity = new MultipartEntity(HttpMultipartMode.STRICT);
        multipartEntity.addPart("file", fileBody);

        connection.setRequestProperty("Content-Type", multipartEntity.getContentType().getValue());
        connection.setRequestProperty(APIMIntegrationConstants.AUTHORIZATION_HEADER,
                "Basic " + encodeCredentials(user, pass));
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
        assertEquals(status, HttpStatus.SC_OK, "Response code is not as expected : " + response);
    }

    private APIDTO getAPI(String apiName, String apiVersion, String provider)
            throws Exception {
        //get the imported file information
        int retry = 10;
        String apiId = "";
        APIListDTO apiListDTO = restAPIPublisher.getAllAPIs();
        log.info("Get All APIS after import: " + apiListDTO.toString());
        if (apiListDTO == null) {
            Thread.sleep(3000);
            for (int i = 0; i < retry; i++) {
                apiListDTO = restAPIPublisher.getAllAPIs();
                if (apiListDTO == null) {
                    retry++;
                    Thread.sleep(3000);
                    log.info("Waiting for the imported APIs");
                } else {
                    break;
                }
            }
        }
        assertNotNull(apiListDTO, "No APIs found in API Publisher");
        for (APIInfoDTO apiInfoDTO : apiListDTO.getList()) {
            if (apiName.equals(apiInfoDTO.getName()) && apiVersion.equals(apiInfoDTO.getVersion()) && provider
                    .equals(apiInfoDTO.getProvider())) {
                apiId = apiInfoDTO.getId();
                log.info("API Object after Import: " + apiInfoDTO.toString());
            }
        }
        log.info("API ID after import: " + apiId);
        HttpResponse response = restAPIPublisher.getAPI(apiId);
        assertEquals(response.getResponseCode(), HTTP_RESPONSE_CODE_OK, "API get failed");
        Gson g = new Gson();
        APIDTO apiObj = g.fromJson(response.getData(), APIDTO.class);
        return apiObj;
    }

}
