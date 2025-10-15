/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.test.utils.base.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;

/**
 * This test case is used to test the API creation with WSDL definitions
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.ALL})
public class WSDLImportTestCase extends APIManagerLifecycleBaseTest {
    private final Log log = LogFactory.getLog(WSDLImportTestCase.class);
    private String WSDL_FILE_API_NAME = "WSDLImportAPIWithWSDLFile";
    private String WSDL_FILE_API_CONTEXT = "wsdlimportwithwsdlfile";
    private String WSDL_FILE_MALFORMED_API_CONTEXT = "wsdlimportwithwsdlfile{version}";
    private String WSDL_ZIP_API_NAME = "WSDLImportAPIWithZipFile";
    private String WSDL_ZIP_API_CONTEXT = "wsdlimportwithzipfile";
    private String WSDL_URL_API_NAME = "WSDLImportAPIWithURL";
    private String WSDL_URL_API_CONTEXT = "wsdlimportwithurl";
    private String API_VERSION = "1.0.0";
    private String backendEndUrl;
    private String wsdlFileApiId;
    private String zipFileApiId;
    private String wsdlUrlApiId;
    private ArrayList<String> grantTypes;
    private String publisherURLHttps;
    private String userName;
    private String password;
    private String tenantDomain;
    private final String KEYSTORE_FILE_PATH_CLIENT =
            TestConfigurationProvider.getResourceLocation() + File.separator + "keystores" + File.separator + "products"
                    + File.separator + "wso2carbon.jks";
    private final String TRUSTSTORE_FILE_PATH_CLIENT =
            TestConfigurationProvider.getResourceLocation() + File.separator + "keystores" + File.separator + "products"
                    + File.separator + "client-truststore.jks";
    private String applicationId;
    private String applicationId2;
    private String accessToken;
    private String wsdlDefinition;
    private String requestBody;
    private String responseBody;
    private String endpointHost = "http://localhost";
    private int endpointPort;
    private WireMockServer wireMockServer;
    private String apiEndPointURL;
    private String wsdlURL;
    private String apiId1;
    private String apiId2;
    private String apiId3;
    private String apiId4;
    private ApplicationKeyDTO applicationKeyDTO;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        log.info("WSDLImportTestCase initiated");

        super.init();
        tenantDomain = storeContext.getContextTenant().getDomain();
        userName = keyManagerContext.getContextTenant().getTenantAdmin().getUserName();
        password = keyManagerContext.getContextTenant().getTenantAdmin().getPassword();
        grantTypes = new ArrayList<>();
        publisherURLHttps = publisherUrls.getWebAppURLHttps();

        // Setting the system properties to call the etcd endpoint
        System.setProperty("javax.net.ssl.keyStoreType", "JKS");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.keyStore", KEYSTORE_FILE_PATH_CLIENT);
        System.setProperty("javax.net.ssl.keyStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.trustStore", TRUSTSTORE_FILE_PATH_CLIENT);
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");

        // Load request/response body
        wsdlDefinition = readFile(getAMResourceLocation() + File.separator + "soap" + File.separator
                + "phoneverify.wsdl");
        requestBody = readFile(getAMResourceLocation() + File.separator + "soap" + File.separator
                + "checkPhoneNumberRequestBody.xml");
        responseBody = readFile(getAMResourceLocation() + File.separator + "soap" + File.separator
                + "checkPhoneNumberResponseBody.xml");

        // Start wiremock server
        startWiremockServer();
        apiEndPointURL = endpointHost + ":" + endpointPort + "/phoneverify";
        wsdlURL = endpointHost + ":" + endpointPort + "/phoneverify/wsdl";
    }

    @Test(groups = {"wso2.am"}, description = "Importing WSDL API definition and create API")
    public void testWsdlDefinitionImport() throws Exception {
        log.info("testWsdlDefinitionImport initiated");

        // Set environment
        ArrayList<String> environment = new ArrayList<>();
        environment.add(Constants.GATEWAY_ENVIRONMENT);

        // Set policies
        ArrayList<String> policies = new ArrayList<>();
        policies.add(APIMIntegrationConstants.API_TIER.UNLIMITED);

        // Set endpointConfig
        JSONObject url = new JSONObject();
        url.put("url", apiEndPointURL);
        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("sandbox_endpoints", url);
        endpointConfig.put("production_endpoints", url);

        // Create additional properties object
        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("provider", user.getUserName());
        additionalPropertiesObj.put("name", WSDL_FILE_API_NAME);
        additionalPropertiesObj.put("context", WSDL_FILE_API_CONTEXT);
        additionalPropertiesObj.put("version", API_VERSION);
        additionalPropertiesObj.put("policies", policies);
        additionalPropertiesObj.put("endpointConfig", endpointConfig);

        // Create API by importing the WSDL definition as .wsdl file
        String wsdlDefinitionPath = FrameworkPathUtil.getSystemResourceLocation() + "wsdl"
                + File.separator + "Sample.wsdl";
        File file = new File(wsdlDefinitionPath);
        APIDTO wsdlFileApidto = restAPIPublisher
                .importWSDLSchemaDefinition(file, null, additionalPropertiesObj.toString(), "SOAP");

        // Make sure API is created properly
        assertEquals(wsdlFileApidto.getName(), WSDL_FILE_API_NAME);
        assertEquals(wsdlFileApidto.getContext(), "/" + WSDL_FILE_API_CONTEXT);
        wsdlFileApiId = wsdlFileApidto.getId();

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(wsdlFileApiId, restAPIPublisher);
        waitForAPIDeploymentSync(userName, WSDL_FILE_API_NAME, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);
        restAPIPublisher.changeAPILifeCycleStatus(wsdlFileApiId, Constants.PUBLISHED);
        HttpResponse createdApiResponse1 = restAPIPublisher.getAPI(wsdlFileApiId);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse1.getResponseCode());

        // Update additional properties object
        additionalPropertiesObj.put("name", WSDL_ZIP_API_NAME);
        additionalPropertiesObj.put("context", WSDL_ZIP_API_CONTEXT);

        // Create API by importing the WSDL definition as .zip file (using archive)
        String wsdlZipDefinitionPath = FrameworkPathUtil.getSystemResourceLocation() + "wsdl"
                + File.separator + "Sample.zip";
        File zipFile = new File(wsdlZipDefinitionPath);
        APIDTO zipFileApidto = restAPIPublisher
                .importWSDLSchemaDefinition(zipFile, null, additionalPropertiesObj.toString(), "SOAP");

        // Make sure API is created properly
        assertEquals(zipFileApidto.getName(), WSDL_ZIP_API_NAME);
        assertEquals(zipFileApidto.getContext(), "/" + WSDL_ZIP_API_CONTEXT);
        zipFileApiId = zipFileApidto.getId();

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(zipFileApiId, restAPIPublisher);
        waitForAPIDeploymentSync(userName, WSDL_ZIP_API_NAME, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);
        restAPIPublisher.changeAPILifeCycleStatus(zipFileApiId, Constants.PUBLISHED);
        HttpResponse createdApiResponse2 = restAPIPublisher.getAPI(zipFileApiId);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse2.getResponseCode());

        // Update additional properties object
        additionalPropertiesObj.put("name", WSDL_URL_API_NAME);
        additionalPropertiesObj.put("context", WSDL_URL_API_CONTEXT);

        // Create API by WSDL URL
        APIDTO wsdlUrlApidto = restAPIPublisher
                .importWSDLSchemaDefinition(null, wsdlURL, additionalPropertiesObj.toString(), "SOAP");

        // Make sure API is created properly
        assertEquals(wsdlUrlApidto.getName(), WSDL_URL_API_NAME);
        assertEquals(wsdlUrlApidto.getContext(), "/" + WSDL_URL_API_CONTEXT);
        wsdlUrlApiId = wsdlUrlApidto.getId();

        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(wsdlUrlApiId, restAPIPublisher);
        waitForAPIDeploymentSync(userName, WSDL_URL_API_NAME, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);
        restAPIPublisher.changeAPILifeCycleStatus(wsdlUrlApiId, Constants.PUBLISHED);
        HttpResponse createdApiResponse3 = restAPIPublisher.getAPI(wsdlUrlApiId);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse3.getResponseCode());

        // Create application and subscribe to API
        ApplicationDTO applicationDTO = restAPIStore.addApplication(APPLICATION_NAME,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, "", "");
        applicationId = applicationDTO.getApplicationId();
        restAPIStore.subscribeToAPI(wsdlUrlApiId, applicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);

        // Generate access token
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add("client_credentials");
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        assertNotNull(applicationKeyDTO.getToken(), "Unable to get access token");
        accessToken = applicationKeyDTO.getToken().getAccessToken();
    }

    @Test(groups = {"wso2.am"}, description = "Importing WSDL API definition and create API")
    public void testWsdlDefinitionImportWithMalformedContext() throws Exception {
        log.info("testWsdlDefinitionImport initiated");

        // Set environment
        ArrayList<String> environment = new ArrayList<>();
        environment.add(Constants.GATEWAY_ENVIRONMENT);

        // Set policies
        ArrayList<String> policies = new ArrayList<>();
        policies.add(APIMIntegrationConstants.API_TIER.UNLIMITED);

        // Set endpointConfig
        JSONObject url = new JSONObject();
        url.put("url", apiEndPointURL);
        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("sandbox_endpoints", url);
        endpointConfig.put("production_endpoints", url);

        // Create additional properties object
        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("provider", user.getUserName());
        additionalPropertiesObj.put("name", WSDL_FILE_API_NAME);
        additionalPropertiesObj.put("context", WSDL_FILE_MALFORMED_API_CONTEXT);
        additionalPropertiesObj.put("version", API_VERSION);
        additionalPropertiesObj.put("policies", policies);
        additionalPropertiesObj.put("endpointConfig", endpointConfig);

        // Create API by importing the WSDL definition as .wsdl file
        String wsdlDefinitionPath = FrameworkPathUtil.getSystemResourceLocation() + "wsdl"
                + File.separator + "Sample.wsdl";
        File file = new File(wsdlDefinitionPath);

        try{
            APIDTO wsdlFileApidto = restAPIPublisher.importWSDLSchemaDefinition(file, null,
                    additionalPropertiesObj.toString(), "SOAP");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), Response.Status.BAD_REQUEST.getStatusCode());
            Assert.assertTrue(e.getResponseBody().contains(APIMIntegrationConstants.API_CONTEXT_MALFORMED_ERROR));
        }
    }

    @Test(groups = {"wso2.am"}, description = "Get WSDL API definition of the created API",
            dependsOnMethods = "testWsdlDefinitionImport")
    public void testGetWsdlDefinitions() throws Exception {
        log.info("testGetWsdlDefinition initiated");

        // get wsdl definition of the API created with .wsdl file
        ApiResponse<Void> wsdlFileFlowResponse = restAPIPublisher.getWSDLSchemaDefinitionOfAPI(wsdlFileApiId);
        assertEquals(Response.Status.OK.getStatusCode(), wsdlFileFlowResponse.getStatusCode());

        // get wsdl definition of the API created with .zip file
        ApiResponse<Void> zipFileFlowResponse = restAPIPublisher.getWSDLSchemaDefinitionOfAPI(zipFileApiId);
        assertEquals(Response.Status.OK.getStatusCode(), zipFileFlowResponse.getStatusCode());

        // get wsdl definition of the API created with wsdl url
        ApiResponse<Void> wsdlUrlFlowResponse = restAPIPublisher.getWSDLSchemaDefinitionOfAPI(wsdlUrlApiId);
        assertEquals(Response.Status.OK.getStatusCode(), wsdlUrlFlowResponse.getStatusCode());
    }

    @Test(groups = {"wso2.am"}, description = "Download WSDL API definition of the created API from the store",
            dependsOnMethods = "testGetWsdlDefinitions")
    public void testDownloadWsdlDefinitionsFromStore() throws Exception {
        log.info("testDownloadWsdlDefinitionFromStore initiated");
        String environmentName = Constants.GATEWAY_ENVIRONMENT;

        // wsdl definition of the API created with .wsdl file from the store
        org.wso2.am.integration.clients.store.api.ApiResponse<Void> wsdlFileFlowResponse = restAPIStore
                .downloadWSDLSchemaDefinitionOfAPI(wsdlFileApiId, environmentName);
        assertEquals(Response.Status.OK.getStatusCode(), wsdlFileFlowResponse.getStatusCode());

        // wsdl definition of the API created with .zip file from the store
        org.wso2.am.integration.clients.store.api.ApiResponse<Void> zipFileFlowResponse = restAPIStore
                .downloadWSDLSchemaDefinitionOfAPI(zipFileApiId, environmentName);
        assertEquals(Response.Status.OK.getStatusCode(), zipFileFlowResponse.getStatusCode());

        // wsdl definition of the API created with wsdl url from the store
        org.wso2.am.integration.clients.store.api.ApiResponse<Void> wsdlUrlFlowResponse = restAPIStore
                .downloadWSDLSchemaDefinitionOfAPI(wsdlUrlApiId, environmentName);
        assertEquals(Response.Status.OK.getStatusCode(), wsdlUrlFlowResponse.getStatusCode());
    }

    @Test(groups = {"wso2.am"}, description = "Test invoking Check Phone Number method",
            dependsOnMethods = "testDownloadWsdlDefinitionsFromStore")
    public void testInvokeCheckPhoneNumber() throws Exception {
        log.info("testInvokeCheckPhoneNumber initiated");
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "text/xml");
        headers.put("accept", "text/xml");
        headers.put("Authorization", "Bearer " + accessToken);
        HttpResponse invokeAPIResponse = HttpRequestUtil.doPost(new URL(getAPIInvocationURLHttp(WSDL_URL_API_CONTEXT,
                API_VERSION)), requestBody, headers);
        assertEquals(invokeAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Unable to invoke Pass Through SOAP API");
    }

    private void startWiremockServer() {
        wireMockServer = new WireMockServer(options().port(endpointPort));
        wireMockServer.stubFor(WireMock.get(urlEqualTo("/phoneverify/wsdl")).willReturn(aResponse()
                .withStatus(200).withHeader("Content-Type", "text/xml").withBody(wsdlDefinition)));
        wireMockServer.stubFor(WireMock.post(urlEqualTo("/phoneverify")).willReturn(aResponse()
                .withStatus(200).withHeader("Content-Type", "text/xml").withBody(responseBody)));
        wireMockServer.start();
        endpointPort = wireMockServer.port();
        log.info("Wiremock server started on port " + endpointPort);
    }

    @Test(groups = {"wso2.am"}, description = "Importing WSDL API definition and create API",
            dependsOnMethods = "testDownloadWsdlDefinitionsFromStore" )
    public void testCreateSOAPAPIFromFile() throws Exception {
        JSONObject endpoints = new JSONObject();
        endpoints.put("url", "http://ws.cdyne.com/phoneverify/phoneverify.asmx");

        ArrayList<String> environment = new ArrayList<String>();
        environment.add("Production and Sandbox");

        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("production_endpoints", endpoints);
        endpointConfig.put("sandbox_endpoints", endpoints);

        List<String> tierList = new ArrayList<>();
        tierList.add(APIMIntegrationConstants.API_TIER.UNLIMITED);
        tierList.add(APIMIntegrationConstants.API_TIER.GOLD);

        JSONObject apiProperties = new JSONObject();
        apiProperties.put("name", "PhoneVerification");
        apiProperties.put("context", "phoneverify");
        apiProperties.put("version", "1.0.0");
        apiProperties.put("provider", user.getUserName());
        apiProperties.put("endpointConfig", endpointConfig);
        apiProperties.put("policies", tierList);

        String wsdlDefinitionPath = FrameworkPathUtil.getSystemResourceLocation() + "wsdl" + File.separator + "PhoneVerification.wsdl";

        File file = new File(wsdlDefinitionPath);
        APIDTO apidto = restAPIPublisher
                .importWSDLSchemaDefinition(file, backendEndUrl, apiProperties.toString(), "SOAP");

        //Make sure API is created properly
        assertEquals(apidto.getName(), "PhoneVerification");
        apiId1 = apidto.getId();
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(apiId1);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode());

        createAPIRevisionAndDeployUsingRest(apiId1, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatusToPublish(apiId1, false);
        waitForAPIDeploymentSync(user.getUserName(), "PhoneVerification", "1.0.0",
                APIMIntegrationConstants.IS_API_EXISTS);

        HttpResponse applicationResponse = restAPIStore.createApplication("TestApp",
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationId2 = applicationResponse.getData();

        restAPIStore.subscribeToAPI(apiId1, applicationId2, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        applicationKeyDTO = restAPIStore.generateKeys(applicationId2, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);

        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        Assert.assertNotNull("Access Token not found ", accessToken);

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Content-Type", "text/xml");
        requestHeaders.put("accept", "application/json");
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        String invokeURL = getAPIInvocationURLHttp("phoneverify", API_VERSION);
        String requestBody = "<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"> <soap:Body> <CheckPhoneNumber xmlns=\"http://ws.cdyne.com/PhoneVerify/query\"> <PhoneNumber>077383968</PhoneNumber> <LicenseKey>123</LicenseKey> </CheckPhoneNumber> </soap:Body></soap:Envelope>";
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, requestBody);
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK, "API invocation failed");
    }

    @Test(groups = {"wso2.am"}, description = "Importing WSDL archive with wsdl file and create API",
            dependsOnMethods = "testCreateSOAPAPIFromFile")
    public void testCreateSOAPAPIFromArchive() throws Exception {
        JSONObject endpoints = new JSONObject();
        endpoints.put("url", "http://ws.cdyne.com/phoneverify/phoneverify.asmx");

        ArrayList<String> environment = new ArrayList<String>();
        environment.add("Production and Sandbox");

        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("production_endpoints", endpoints);
        endpointConfig.put("sandbox_endpoints", endpoints);

        List<String> tierList = new ArrayList<>();
        tierList.add(APIMIntegrationConstants.API_TIER.UNLIMITED);
        tierList.add(APIMIntegrationConstants.API_TIER.GOLD);

        JSONObject apiProperties = new JSONObject();
        apiProperties.put("name", "PhoneVerificationArchive");
        apiProperties.put("context", "phoneverifyarchive");
        apiProperties.put("version", "1.0.0");
        apiProperties.put("provider", user.getUserName());
        apiProperties.put("endpointConfig", endpointConfig);
        apiProperties.put("policies", tierList);

        String wsdlDefinitionPath = FrameworkPathUtil.getSystemResourceLocation() + "wsdl" + File.separator + "PhoneVerification.zip";

        File file = new File(wsdlDefinitionPath);
        APIDTO apidto = restAPIPublisher
                .importWSDLSchemaDefinition(file, backendEndUrl, apiProperties.toString(), "SOAP");

        //Make sure API is created properly
        assertEquals(apidto.getName(), "PhoneVerificationArchive");
        apiId2 = apidto.getId();
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(apiId2);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode());

        createAPIRevisionAndDeployUsingRest(apiId2, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatusToPublish(apiId2, false);
        waitForAPIDeploymentSync(user.getUserName(), "PhoneVerificationArchive", "1.0.0",
                APIMIntegrationConstants.IS_API_EXISTS);
        restAPIStore.subscribeToAPI(apiId2, applicationId2, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        Assert.assertNotNull("Access Token not found ", accessToken);

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Content-Type", "text/xml");
        requestHeaders.put("accept", "application/json");
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        String invokeURL = getAPIInvocationURLHttp("phoneverifyarchive", API_VERSION);
        String requestBody = "<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"> <soap:Body> <CheckPhoneNumber xmlns=\"http://ws.cdyne.com/PhoneVerify/query\"> <PhoneNumber>077383968</PhoneNumber> <LicenseKey>123</LicenseKey> </CheckPhoneNumber> </soap:Body></soap:Envelope>";
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, requestBody);
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK, "API invocation failed");
    }

    @Test(groups = {"wso2.am"}, description = "Importing WSDL archive with multiple files and create API",
            dependsOnMethods = "testCreateSOAPAPIFromArchive")
    public void testCreateSOAPAPIFromArchiveWithMultipleFiles() throws Exception {
        // Create API using greetings archive
        JSONObject endpoints = new JSONObject();
        endpoints.put("url", "http://example.com");

        ArrayList<String> environment = new ArrayList<String>();
        environment.add("Production and Sandbox");

        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("production_endpoints", endpoints);
        endpointConfig.put("sandbox_endpoints", endpoints);

        List<String> tierList = new ArrayList<>();
        tierList.add(APIMIntegrationConstants.API_TIER.UNLIMITED);
        tierList.add(APIMIntegrationConstants.API_TIER.GOLD);

        JSONObject apiProperties = new JSONObject();
        apiProperties.put("name", "GreetingsArchive");
        apiProperties.put("context", "greetingsarchive");
        apiProperties.put("version", "1.0.0");
        apiProperties.put("provider", user.getUserName());
        apiProperties.put("endpointConfig", endpointConfig);
        apiProperties.put("policies", tierList);

        String wsdlDefinitionPath = FrameworkPathUtil.getSystemResourceLocation() + "wsdl" + File.separator
                + "greetings.zip";

        File file = new File(wsdlDefinitionPath);
        APIDTO apidto = restAPIPublisher
                .importWSDLSchemaDefinition(file, backendEndUrl, apiProperties.toString(), "SOAP");

        // Make sure API is created properly
        assertEquals(apidto.getName(), "GreetingsArchive");
        apiId4 = apidto.getId();
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(apiId4);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode());
    }

    @Test(groups = {"wso2.am"}, description = "Creating SOAP API from URL",
            dependsOnMethods = "testCreateSOAPAPIFromArchive")
    public void testCreateSOAPAPIFromURL() throws Exception {
        JSONObject endpoints = new JSONObject();
        endpoints.put("url", "http://ws.cdyne.com/phoneverify/phoneverify.asmx");

        ArrayList<String> environment = new ArrayList<String>();
        environment.add("Production and Sandbox");

        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("production_endpoints", endpoints);
        endpointConfig.put("sandbox_endpoints", endpoints);

        List<String> tierList = new ArrayList<>();
        tierList.add(APIMIntegrationConstants.API_TIER.UNLIMITED);
        tierList.add(APIMIntegrationConstants.API_TIER.GOLD);

        JSONObject apiProperties = new JSONObject();
        apiProperties.put("name", "PhoneVerificationURL");
        apiProperties.put("context", "phoneverifyurl123");
        apiProperties.put("version", "1.0.0");
        apiProperties.put("provider", user.getUserName());
        apiProperties.put("endpointConfig", endpointConfig);
        apiProperties.put("policies", tierList);

        APIDTO apidto = restAPIPublisher
                .importWSDLSchemaDefinition(null,
                        "http://ws.cdyne.com/phoneverify/phoneverify.asmx?wsdl", apiProperties.toString(), "SOAP");

        //Make sure API is created properly
        assertEquals(apidto.getName(), "PhoneVerificationURL");
        apiId3 = apidto.getId();
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(apiId3);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode());

        createAPIRevisionAndDeployUsingRest(apiId3, restAPIPublisher);
        restAPIPublisher.changeAPILifeCycleStatusToPublish(apiId3, false);
        waitForAPIDeploymentSync(user.getUserName(), "PhoneVerificationURL", "1.0.0",
                APIMIntegrationConstants.IS_API_EXISTS);
        restAPIStore.subscribeToAPI(apiId3, applicationId2, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        Assert.assertNotNull("Access Token not found ", accessToken);

        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Content-Type", "text/xml");
        requestHeaders.put("accept", "application/json");
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        String invokeURL = getAPIInvocationURLHttp("phoneverifyurl123", API_VERSION);
        String requestBody = "<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"> <soap:Body> <CheckPhoneNumber xmlns=\"http://ws.cdyne.com/PhoneVerify/query\"> <PhoneNumber>077383968</PhoneNumber> <LicenseKey>123</LicenseKey> </CheckPhoneNumber> </soap:Body></soap:Envelope>";
        HttpResponse serviceResponse = HTTPSClientUtils.doPost(invokeURL, requestHeaders, requestBody);
        Assert.assertEquals(serviceResponse.getResponseCode(), HttpStatus.SC_OK, "API invocation failed");
    }

    /**
     * Check whether give port is available
     *
     * @param port Port Number
     * @return status
     */
    private boolean isPortFree(int port) {
        Socket s = null;
        try {
            s = new Socket(endpointHost, port);
            // something is using the port and has responded.
            return false;
        } catch (IOException e) {
            // port available
            return true;
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to close connection ", e);
                }
            }
        }
    }

    @AfterClass(alwaysRun = true)
    void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        restAPIStore.deleteApplication(applicationId2);
        undeployAndDeleteAPIRevisionsUsingRest(wsdlFileApiId, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(zipFileApiId, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(wsdlUrlApiId, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(apiId1, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(apiId2, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(apiId3, restAPIPublisher);
        restAPIPublisher.deleteAPI(wsdlFileApiId);
        restAPIPublisher.deleteAPI(zipFileApiId);
        restAPIPublisher.deleteAPI(wsdlUrlApiId);
        restAPIPublisher.deleteAPI(apiId1);
        restAPIPublisher.deleteAPI(apiId2);
        restAPIPublisher.deleteAPI(apiId3);
        restAPIPublisher.deleteAPI(apiId4);
        wireMockServer.stop();
    }
}
