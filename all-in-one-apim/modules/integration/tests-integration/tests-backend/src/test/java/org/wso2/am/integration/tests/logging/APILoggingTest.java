/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.am.integration.tests.logging;

import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APIThrottlingTier;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.am.integration.tests.restapi.RESTAPITestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;
import org.wso2.carbon.utils.ServerConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.xpath.XPathExpressionException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class APILoggingTest extends APIManagerLifecycleBaseTest {
    private String apiId;
    private String apiId2;
    private String templateMatchApiId;
    private APIRequest apiRequest;
    private Map<String, String> requestHeaders = new HashMap<String, String>();
    private String applicationId;
    private String templateMatchApplicationId;
    private ArrayList<String> grantTypes = new ArrayList<>();
    private final String CALLBACK_URL = "https://localhost:9443/store/";
    private String applicationId2;
    private final String TIER_COLLECTION = APIMIntegrationConstants.API_TIER.UNLIMITED;
    private String API_VERSION = "1.0.0";
    private String endpointUrl;
    private int logLineCounter = 0;
    private String tokenURL;
    private String identityLoginURL;

    @Factory(dataProvider = "userModeDataProvider")
    public APILoggingTest(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{new Object[]{TestUserMode.SUPER_TENANT_ADMIN}, new Object[]{TestUserMode.TENANT_ADMIN}};
    }

    @BeforeClass(alwaysRun = true)
    public void initialize() throws APIManagerIntegrationTestException, RemoteException, XPathExpressionException,
            UserAdminUserAdminException {
        logLineCounter = 0;
        super.init();
    }

    @Test(groups = {"wso2.am" }, description = "Sending http request to per API logging enabled API: ")
    public void testAPIPerAPILoggingTestcase() throws Exception {

        // Get list of APIs without any API
        Map<String, String> header = new HashMap<>();
        byte[] encodedBytes = Base64.encodeBase64(RESTAPITestConstants.BASIC_AUTH_HEADER
                .getBytes(StandardCharsets.UTF_8));
        header.put("Authorization", "Basic " + new String(encodedBytes, StandardCharsets.UTF_8));
        header.put("Content-Type", "application/json");
        HttpResponse loggingResponse = HTTPSClientUtils.doGet(getStoreURLHttps()
                + "api/am/devops/v0/tenant-logs/carbon.super/apis", header);
        assertEquals(loggingResponse.getData(), "{\"apis\":[]}");

        String API_NAME = "APILoggingTestAPI";
        String API_CONTEXT = "apiloggingtest";
        String API_TAGS = "testTag1, testTag2, testTag3";
        String API_END_POINT_POSTFIX_URL = "xmlapi";
        String APPLICATION_NAME = "APILoggingTestApp";

        // Create an application
        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Test Application AccessibilityOfBlockAPITestCase", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();

        // Create an API and subscribe to it using created application
        APIRequest apiRequest;
        String apiEndPointUrl = getAPIInvocationURLHttp(API_END_POINT_POSTFIX_URL, API_VERSION);
        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTags(API_TAGS);
        apiRequest.setProvider(user.getUserName());
        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore,
                applicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);

        // Get list of APIs with an API
        loggingResponse = HTTPSClientUtils.doGet(getStoreURLHttps()
                + "api/am/devops/v0/tenant-logs/carbon.super/apis", header);
        assertEquals(loggingResponse.getData(), "{\"apis\":[{\"context\":\"/" + API_CONTEXT + "/" + API_VERSION +
                "\",\"logLevel\":\"OFF\",\"apiId\":\"" + apiId + "\",\"resourceMethod\":null,\"resourcePath\":null}," +
                "{\"context\":\"/" + API_CONTEXT + "/" + API_VERSION + "\",\"logLevel\":\"OFF\",\"apiId\":\""+apiId+"\",\"" +
                "resourceMethod\":\"GET\",\"resourcePath\":\""
                + apiRequest.getUriTemplate() + "\"}]}");
        String addNewLoggerPayload = "{ \"logLevel\": \"FULL\" }";
        HTTPSClientUtils.doPut(getStoreURLHttps() + "api/am/devops/v0/tenant-logs/carbon.super/apis/" + apiId, header,
                addNewLoggerPayload);

        // Get list of APIs which have log-level=FULL
        loggingResponse = HTTPSClientUtils.doGet(getStoreURLHttps()
                + "api/am/devops/v0/tenant-logs/carbon.super/apis?log-level=full", header);
        assertEquals(loggingResponse.getData(), "{\"apis\":[{\"context\":\"/" + API_CONTEXT + "/" + API_VERSION +
                "\",\"logLevel\":\"FULL\",\"apiId\":\"" + apiId + "\",\"resourceMethod\":null,\"resourcePath\":null}" +
                "]}");
        // Invoke the API
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add("client_credentials");
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        assertNotNull(applicationKeyDTO.getToken());
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        HttpClient client = HttpClientBuilder.create().setHostnameVerifier(new AllowAllHostnameVerifier()).build();
        HttpGet request = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION));
        request.setHeader("Authorization", "Bearer " + accessToken);
        org.apache.http.HttpResponse response = client.execute(request);
        assertEquals(response.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation fails for GET request");

        // Validate API Logs
        String apiLogFilePath = System.getProperty(ServerConstants.CARBON_HOME) + File.separator + "repository"
                + File.separator + "logs" + File.separator + "api.log";
        BufferedReader bufferedReader = new BufferedReader(new FileReader(apiLogFilePath));
        String logLine;
        int lineNo = 0;
        while ((logLine = bufferedReader.readLine()) != null) {
            if (lineNo == logLineCounter) {
                if (logLineCounter < 4 || (logLineCounter >= 8 && logLineCounter < 12)) {
                    assertTrue(logLine.contains("INFO {API_LOG} " + API_NAME));
                    assertTrue(logLine.contains("correlationId"));
                }
                logLineCounter++;
            }
            lineNo++;
        }
    }

    @Test(groups = {"wso2.am"}, description = "Sending http request to per API logging enabled API: ",
            dependsOnMethods = "testAPIPerAPILoggingTestcase")
    public void testAPIPerAPIResourceLoggingTestcase() throws Exception {
        // Get list of APIs without any API
        Map<String, String> header = new HashMap<>();
        byte[] encodedBytes = Base64.encodeBase64(RESTAPITestConstants.BASIC_AUTH_HEADER
                .getBytes(StandardCharsets.UTF_8));
        header.put("Authorization", "Basic " + new String(encodedBytes, StandardCharsets.UTF_8));
        header.put("Content-Type", "application/json");
        HttpResponse loggingResponse = HTTPSClientUtils.doGet(getStoreURLHttps()
                + "api/am/devops/v0/tenant-logs/carbon.super/apis?log-level=full", header);
        assertEquals(loggingResponse.getData(), "{\"apis\":[{\"context\":\"/apiloggingtest/" + API_VERSION +
                "\",\"logLevel\":\"FULL\",\"apiId\":\"" + apiId + "\",\"resourceMethod\":null,\"resourcePath\":null}" +
                "]}");

        String API_NAME = "APILoggingTestAPIWithResources";
        String API_CONTEXT = "apiloggingtestwithresources";
        String API_TAGS = "testTag1, testTag2, testTag3";
        String APPLICATION_NAME = "APILoggingTestApp2";
        String DESCRIPTION = "This is a test Application";

        storeURLHttp = getStoreURLHttp();
        endpointUrl = backEndServerUrl.getWebAppURLHttp() + "am/sample/calculator/v1/api";
        tokenURL = getKeyManagerURLHttps() + "oauth2/token";
        identityLoginURL = getKeyManagerURLHttps() + "oauth2/authorize";

        //create Application
        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Test Application", APIThrottlingTier.UNLIMITED.getState(),
                ApplicationDTO.TokenTypeEnum.JWT);
        assertEquals(applicationResponse.getResponseCode(), HttpStatus.SC_OK, "Response code is not as expected");
        applicationId2 = applicationResponse.getData();
        String providerName = user.getUserName();

        List<APIOperationsDTO> apiOperationsDTOS = new ArrayList<>();
        APIOperationsDTO apiOperationsDTOAdd = new APIOperationsDTO();
        APIOperationsDTO apiOperationsDTOMultiply = new APIOperationsDTO();
        apiOperationsDTOAdd.setVerb(RESTAPITestConstants.GET_METHOD);
        apiOperationsDTOAdd
                .setAuthType(APIMIntegrationConstants.ResourceAuthTypes.APPLICATION_AND_APPLICATION_USER.getAuthType());
        apiOperationsDTOMultiply.setVerb(RESTAPITestConstants.GET_METHOD);
        apiOperationsDTOMultiply
                .setAuthType(APIMIntegrationConstants.ResourceAuthTypes.APPLICATION_AND_APPLICATION_USER.getAuthType());
        apiOperationsDTOAdd.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.TWENTYK_PER_MIN);
        apiOperationsDTOMultiply.setThrottlingPolicy(APIMIntegrationConstants.RESOURCE_TIER.TWENTYK_PER_MIN);
        apiOperationsDTOAdd.setTarget("/add");
        apiOperationsDTOMultiply.setTarget("/multiply");
        apiOperationsDTOS.add(apiOperationsDTOAdd);
        apiOperationsDTOS.add(apiOperationsDTOMultiply);

        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(endpointUrl));
        apiRequest.setVersion(API_VERSION);
        apiRequest.setProvider(providerName);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setOperationsDTOS(apiOperationsDTOS);
        apiRequest.setTiersCollection(TIER_COLLECTION);
        apiRequest.setTags(API_TAGS);
        apiRequest.setDescription(DESCRIPTION);

        // Create an API and subscribe to it using created application
        apiId2 = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationId2,
                APIMIntegrationConstants.API_TIER.UNLIMITED);

        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.AUTHORIZATION_CODE);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.REFRESH_CODE);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.SAML2);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.NTLM);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.JWT);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.IMPLICIT);

        ApplicationKeyDTO applicationKeyDTO = restAPIStore
                .generateKeys(applicationId2, "3600", CALLBACK_URL, ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION,
                        null, grantTypes);
        assertNotNull(applicationKeyDTO.getToken().getAccessToken());
        String consumerKey = applicationKeyDTO.getConsumerKey();
        String consumerSecret = applicationKeyDTO.getConsumerSecret();
        Assert.assertNotNull(consumerKey, "Consumer Key not found");
        Assert.assertNotNull(consumerSecret, "Consumer Secret not found ");

        //Invoking the api with /add and /multiply with logging disabled
        requestHeaders.clear();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " +
                applicationKeyDTO.getToken().getAccessToken());
        String invokeURL = getAPIInvocationURLHttp(API_CONTEXT, API_VERSION);
        HttpResponse res1 = HTTPSClientUtils.doGet(invokeURL + "/add?x=1&y=1", requestHeaders);
        HttpResponse res2 = HTTPSClientUtils.doGet(invokeURL + "/multiply?x=1&y=1", requestHeaders);
        assertEquals(res1.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation fails for GET request");
        assertEquals(res2.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation fails for GET request");
        //Enabling the log level as full for the /add resource path and GET resource method
        String addNewLoggerPayload = "{\"logLevel\": \"full\", \"resourceMethod\":\"GET\", \"resourcePath\":\"/add\"}";
        HTTPSClientUtils.doPut(getStoreURLHttps() + "api/am/devops/v0/tenant-logs/carbon.super/apis/" + apiId2,
                header, addNewLoggerPayload);
        //Get the apis which have enabled the log level as full
        loggingResponse = HTTPSClientUtils.doGet(getStoreURLHttps()
                + "api/am/devops/v0/tenant-logs/carbon.super/apis?log-level=full", header);
        assertEquals(loggingResponse.getData(), "{\"apis\":[{\"context\":\"/apiloggingtest/1.0.0\",\"logLevel\":" +
                "\"FULL\",\"apiId\":\"" + apiId + "\",\"resourceMethod\":null,\"resourcePath\":null},{\"context\":\"/"
                + API_CONTEXT + "/1.0.0\",\"logLevel\":\"FULL\",\"apiId\":\"" + apiId2 + "\",\"resourceMethod\":" +
                "\"GET\",\"resourcePath\":\"/add\"}]}");
        Thread.sleep(1000);
        //Invoking the /add and /multply resource, When invoking /add, full log should be printed.
        res1 = HTTPSClientUtils.doGet(invokeURL + "/add?x=1&y=1", requestHeaders);
        res2 = HTTPSClientUtils.doGet(invokeURL + "/multiply?x=1&y=1", requestHeaders);
        assertEquals(res1.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation fails for GET request");
        assertEquals(res2.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Invocation fails for GET request");

        //Disabling the log level.
        addNewLoggerPayload = "{\"logLevel\": \"off\", \"resourceMethod\":\"GET\", \"resourcePath\":\"/add\"}";
        HTTPSClientUtils.doPut(getStoreURLHttps() + "api/am/devops/v0/tenant-logs/carbon.super/apis/" + apiId2,
                header, addNewLoggerPayload);

        //Validate API Logs
        String apiLogFilePath = System.getProperty(ServerConstants.CARBON_HOME) + File.separator + "repository"
                + File.separator + "logs" + File.separator + "api.log";
        String logLine;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(apiLogFilePath));
        int lineNo = 0;
        while ((logLine = bufferedReader.readLine()) != null) {
            if (lineNo == logLineCounter) {
                if (logLineCounter >= 12 || (logLineCounter >= 4 && logLineCounter < 8)) {
                    assertTrue(logLine.contains("INFO {API_LOG} " + API_NAME));
                    assertTrue(logLine.contains("correlationId"));
                }
                logLineCounter++;
            }
            lineNo++;
        }
    }

    @Test(groups = {"wso2.am"}, description = "Invoking API with logging enabled with similar templates: ",
            dependsOnMethods = "testAPIPerAPIResourceLoggingTestcase")
    public void testSimilarTemplateInvocationWithLoggingTestcase() throws Exception {
        String API_NAME = "SimilarTemplateAPI";
        String API_CONTEXT = "similartemplatetest";
        String API_TAGS = "testTag1, testTag2, testTag3";
        String API_END_POINT_POSTFIX_URL = "xmlapi";
        String APPLICATION_NAME = "SimilarTemplateTestApp";
        String templateMatchAccessToken;
        HttpClient templateMatchClient;

        // Create a templateMatch application
        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME,
                "Test Application for Similar Template Test", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        templateMatchApplicationId = applicationResponse.getData();

        // Create a templateMatch API
        String apiEndPointUrl = getAPIInvocationURLHttp(API_END_POINT_POSTFIX_URL, API_VERSION);
        APIRequest templateMatchApiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));
        templateMatchApiRequest.setVersion(API_VERSION);
        templateMatchApiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        templateMatchApiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        templateMatchApiRequest.setTags(API_TAGS);
        templateMatchApiRequest.setProvider(user.getUserName());
        templateMatchApiId = createPublishAndSubscribeToAPIUsingRest(templateMatchApiRequest, restAPIPublisher, restAPIStore,
                templateMatchApplicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);

        // Enable logging for the templateMatch API
        Map<String, String> header = new HashMap<>();
        byte[] encodedBytes = Base64.encodeBase64(RESTAPITestConstants.BASIC_AUTH_HEADER
                .getBytes(StandardCharsets.UTF_8));
        header.put("Authorization", "Basic " + new String(encodedBytes, StandardCharsets.UTF_8));
        header.put("Content-Type", "application/json");
        String addNewLoggerPayload = "{ \"logLevel\": \"FULL\" }";
        HTTPSClientUtils.doPut(getStoreURLHttps() + "api/am/devops/v0/tenant-logs/carbon.super/apis/" + templateMatchApiId, header,
                addNewLoggerPayload);

        // Generate templateMatch access token
        ArrayList<String> templateMatchGrantTypes = new ArrayList<>();
        templateMatchGrantTypes.add("client_credentials");
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(templateMatchApplicationId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, templateMatchGrantTypes);
        assertNotNull(applicationKeyDTO.getToken());
        templateMatchAccessToken = applicationKeyDTO.getToken().getAccessToken();
        templateMatchClient = HttpClientBuilder.create().setHostnameVerifier(new AllowAllHostnameVerifier()).build();

        // Update API with additional operations
        HttpResponse getAPIResponse = restAPIPublisher.getAPI(templateMatchApiId);
        APIDTO apidto = new Gson().fromJson(getAPIResponse.getData(), APIDTO.class);

        List<APIOperationsDTO> operations = apidto.getOperations();
        if (operations == null) {
            operations = new ArrayList<>();
        }
        // Add POST /payee/personal operation
        APIOperationsDTO postPayeeOperation = new APIOperationsDTO();
        postPayeeOperation.setVerb("POST");
        postPayeeOperation.setTarget("/payee/personal");
        postPayeeOperation.setAuthType("Application & Application User");
        postPayeeOperation.setThrottlingPolicy("Unlimited");
        operations.add(postPayeeOperation);
        // Add GET /payee/{id} operation
        APIOperationsDTO getPayeeOperation = new APIOperationsDTO();
        getPayeeOperation.setVerb("GET");
        getPayeeOperation.setTarget("/payee/{id}");
        getPayeeOperation.setAuthType("Application & Application User");
        getPayeeOperation.setThrottlingPolicy("Unlimited");
        operations.add(getPayeeOperation);

        apidto.setOperations(operations);
        restAPIPublisher.updateAPI(apidto);

        // Create new revision and deploy
        createAPIRevisionAndDeployUsingRest(templateMatchApiId, restAPIPublisher);
        waitForAPIDeployment();

        // Send OPTIONS pre-flight request for POST
        HttpOptions optionsPostRequest = new HttpOptions(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION) +
                "/payee/personal");
        optionsPostRequest.addHeader("Origin", "https://localhost:9443");
        optionsPostRequest.addHeader("Access-Control-Request-Method", "POST");
        optionsPostRequest.addHeader("Access-Control-Request-Headers", "authorization,content-type");
        org.apache.http.HttpResponse optionsPostResponse = templateMatchClient.execute(optionsPostRequest);
        assertEquals(optionsPostResponse.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK,
                "OPTIONS pre-flight request for POST /payee/personal should succeed");
        optionsPostRequest.releaseConnection();

        // Actual POST call
        HttpPost postRequest = new HttpPost(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION) + "/payee/personal");
        postRequest.setHeader("Authorization", "Bearer " + templateMatchAccessToken);
        postRequest.setHeader("Content-Type", "application/json");
        postRequest.setEntity(new StringEntity("{\"name\":\"test\"}"));
        org.apache.http.HttpResponse postResponse = templateMatchClient.execute(postRequest);
        assertEquals(postResponse.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK,
                "POST request to /payee/personal should succeed");
        postRequest.releaseConnection();

        // Send OPTIONS pre-flight request for GET /payee/{id}
        HttpOptions optionsGetRequest = new HttpOptions(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION) +
                "/payee/123");
        optionsGetRequest.addHeader("Origin", "https://localhost:9443");
        optionsGetRequest.addHeader("Access-Control-Request-Method", "GET");
        optionsGetRequest.addHeader("Access-Control-Request-Headers", "authorization");
        org.apache.http.HttpResponse optionsGetResponse = templateMatchClient.execute(optionsGetRequest);
        assertEquals(optionsGetResponse.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK,
                "OPTIONS pre-flight request for GET /payee/{id} should succeed");
        optionsGetRequest.releaseConnection();

        // Actual GET call
        HttpGet getRequest = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION) + "/payee/123");
        getRequest.setHeader("Authorization", "Bearer " + templateMatchAccessToken);
        org.apache.http.HttpResponse getResponse = templateMatchClient.execute(getRequest);
        assertEquals(getResponse.getStatusLine().getStatusCode(), HTTP_RESPONSE_CODE_OK,
                "GET request to /payee/{id} should succeed");
        getRequest.releaseConnection();
    }

    @AfterClass(alwaysRun = true)
    void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        restAPIStore.deleteApplication(applicationId2);
        restAPIStore.deleteApplication(templateMatchApplicationId);
        restAPIPublisher.deleteAPI(apiId);
        restAPIPublisher.deleteAPI(apiId2);
        restAPIPublisher.deleteAPI(templateMatchApiId);
    }
}
