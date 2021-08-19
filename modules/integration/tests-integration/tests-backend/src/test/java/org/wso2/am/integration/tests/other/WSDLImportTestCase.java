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

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import static org.testng.Assert.assertEquals;

/**
 * This test case is used to test the API creation with WSDL definitions
 */
@SetEnvironment(executionEnvironments = { ExecutionEnvironment.ALL }) public class WSDLImportTestCase
        extends APIManagerLifecycleBaseTest {

    private final Log log = LogFactory.getLog(APIImportExportTestCase.class);

    private String API_NAME = "WSDLImportAPI";
    private String API_CONTEXT = "wsdlimport";
    private String WSDL_ZIP_API_NAME = "WSDLImportAPIWithZip";
    private String WSDL_ZIP_API_CONTEXT = "wsdlimportwithzip";
    private String API_VERSION = "1.0.0";
    private String backendEndWSDL;
    private String backendEndUrl;
    private String importApiId;
    private String importZipApiId;
    private ArrayList<String> grantTypes;
    private String publisherURLHttps;
    private String tierCollection;
    private String userName;
    private String password;
    private String tenantDomain;
    private String apiId1;
    private String apiId2;
    private String apiId3;
    private String applicationId;
    private final String KEYSTORE_FILE_PATH_CLIENT =
            TestConfigurationProvider.getResourceLocation() + File.separator + "keystores" + File.separator + "products"
                    + File.separator + "wso2carbon.jks";
    private final String TRUSTSTORE_FILE_PATH_CLIENT =
            TestConfigurationProvider.getResourceLocation() + File.separator + "keystores" + File.separator + "products"
                    + File.separator + "client-truststore.jks";
    private ApplicationKeyDTO applicationKeyDTO;

    @BeforeClass(alwaysRun = true) public void setEnvironment() throws Exception {
        log.info("WSDLImportTestCase  Initiated");

        super.init();
        tenantDomain = storeContext.getContextTenant().getDomain();
        userName = keyManagerContext.getContextTenant().getTenantAdmin().getUserName();
        password = keyManagerContext.getContextTenant().getTenantAdmin().getPassword();
        grantTypes = new ArrayList<>();
        publisherURLHttps = publisherUrls.getWebAppURLHttps();
        backendEndWSDL = getGatewayURLNhttp() + "services/echo?wsdl";

        tierCollection = APIMIntegrationConstants.API_TIER.BRONZE + "," + APIMIntegrationConstants.API_TIER.GOLD + ","
                + APIMIntegrationConstants.API_TIER.SILVER + "," + APIMIntegrationConstants.API_TIER.UNLIMITED;

        //Setting the system properties to call the etcd endpoint
        System.setProperty("javax.net.ssl.keyStoreType", "JKS");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.keyStore", KEYSTORE_FILE_PATH_CLIENT);
        System.setProperty("javax.net.ssl.keyStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.trustStore", TRUSTSTORE_FILE_PATH_CLIENT);
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
    }

    @Test(groups = {"wso2.am" }, description = "Importing WSDL API definition and create API")
    public void testWsdlDefinitionImport()throws Exception {
        log.info("testWsdlDefinitionImport  Initiated");

        //Create additional properties object
        ArrayList<String> environment = new ArrayList<String>();
        environment.add("Production and Sandbox");

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("provider", user.getUserName());
        additionalPropertiesObj.put("name", API_NAME);
        additionalPropertiesObj.put("version", API_VERSION);
        additionalPropertiesObj.put("context", API_CONTEXT);

        JSONObject url = new JSONObject();
        url.put("url", "/");
        JSONObject endpointConfig = new JSONObject();
        endpointConfig.put("endpoint_type", "http");
        endpointConfig.put("sandbox_endpoints", url);
        endpointConfig.put("production_endpoints", url);
        additionalPropertiesObj.put("endpointConfig", endpointConfig);
        additionalPropertiesObj.put("gatewayEnvironments", environment);

        //create API by importing the WSDL definition as .wsdl file
        String wsdlDefinitionPath = FrameworkPathUtil.getSystemResourceLocation()  + "wsdl" + File.separator + "Sample.wsdl";

        File file = new File(wsdlDefinitionPath);
        APIDTO apidto = restAPIPublisher
                .importWSDLSchemaDefinition(file, backendEndUrl, additionalPropertiesObj.toString(), "SOAP");

        //Make sure API is created properly
        assertEquals(apidto.getName(), API_NAME);
        assertEquals(apidto.getContext(), "/" + API_CONTEXT);
        importApiId = apidto.getId();
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(importApiId);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode());

        //create API by importing the WSDL definition as .zip file (using archive)
        additionalPropertiesObj.put("provider", user.getUserName());
        additionalPropertiesObj.put("name", WSDL_ZIP_API_NAME);
        additionalPropertiesObj.put("version", API_VERSION);
        additionalPropertiesObj.put("context", WSDL_ZIP_API_CONTEXT);
        String wsdlZipDefinitionPath = FrameworkPathUtil.getSystemResourceLocation()  + "wsdl" + File.separator + "Sample.zip";

        File zipFile = new File(wsdlZipDefinitionPath);
        APIDTO zipApidto = restAPIPublisher
                .importWSDLSchemaDefinition(zipFile, backendEndUrl, additionalPropertiesObj.toString(), "SOAP");

        //Make sure API is created properly
        assertEquals(zipApidto.getName(), WSDL_ZIP_API_NAME);
        assertEquals(zipApidto.getContext(), "/" + WSDL_ZIP_API_CONTEXT);
        importZipApiId = zipApidto.getId();
        HttpResponse createdApiResponse2 = restAPIPublisher.getAPI(importZipApiId);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse2.getResponseCode());

    }

    @Test(groups = {"wso2.am" }, description = "Get WSDL API definition of the created API", dependsOnMethods = "testWsdlDefinitionImport")
    public void testGetWsdlDefinitions()throws Exception {
        log.info("testGetWsdlDefinition  Initiated");

        // get wsdl definition of the API created with .wsdl file
        ApiResponse<Void> response = restAPIPublisher.getWSDLSchemaDefinitionOfAPI(importApiId);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());

        // get wsdl definition of the API created with .zip file
        ApiResponse<Void> zipFlowResponse = restAPIPublisher.getWSDLSchemaDefinitionOfAPI(importZipApiId);
        assertEquals(Response.Status.OK.getStatusCode(), zipFlowResponse.getStatusCode());
    }

    @Test(groups = {"wso2.am" }, description = "Download WSDL API definition of the created API from the store", dependsOnMethods = "testGetWsdlDefinitions")
    public void testDownloadWsdlDefinitionsFromStore()throws Exception {
        log.info("testDownloadWsdlDefinitionFromStore  Initiated");

        String environmentName = "Production and Sandbox";
        //wsdl definition of the API created with .wsdl file from the store
        org.wso2.am.integration.clients.store.api.ApiResponse<Void> response = restAPIStore
                .downloadWSDLSchemaDefinitionOfAPI(importApiId, environmentName);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());

        //wsdl definition of the API created with .zip file from the store
        org.wso2.am.integration.clients.store.api.ApiResponse<Void> zipFlowResponse = restAPIStore
                .downloadWSDLSchemaDefinitionOfAPI(importZipApiId, environmentName);
        assertEquals(Response.Status.OK.getStatusCode(), zipFlowResponse.getStatusCode());
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
        apiProperties.put("gatewayEnvironments", environment);
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

        restAPIPublisher.changeAPILifeCycleStatus(apiId1, APILifeCycleAction.PUBLISH.getAction());

        HttpResponse applicationResponse = restAPIStore.createApplication("TestApp",
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();

        restAPIStore.subscribeToAPI(apiId1, applicationId, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        applicationKeyDTO = restAPIStore.generateKeys(applicationId, "36000", "",
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

    @Test(groups = {"wso2.am"}, description = "Importing WSDL archive and create API",
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
        apiProperties.put("gatewayEnvironments", environment);
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

        restAPIPublisher.changeAPILifeCycleStatus(apiId2, APILifeCycleAction.PUBLISH.getAction());
        restAPIStore.subscribeToAPI(apiId2, applicationId, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);

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
        apiProperties.put("gatewayEnvironments", environment);
        apiProperties.put("policies", tierList);

        APIDTO apidto = restAPIPublisher
                .importWSDLSchemaDefinition(null,
                        "http://ws.cdyne.com/phoneverify/phoneverify.asmx?wsdl", apiProperties.toString(), "SOAP");

        //Make sure API is created properly
        assertEquals(apidto.getName(), "PhoneVerificationURL");
        apiId3 = apidto.getId();
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(apiId3);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode());

        restAPIPublisher.changeAPILifeCycleStatus(apiId3, APILifeCycleAction.PUBLISH.getAction());
        restAPIStore.subscribeToAPI(apiId3, applicationId, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);

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

    @AfterClass(alwaysRun = true)
    void destroy() throws Exception {
        restAPIPublisher.deleteAPI(importApiId);
        restAPIPublisher.deleteAPI(importZipApiId);
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPIByID(apiId1);
        restAPIPublisher.deleteAPIByID(apiId2);
        restAPIPublisher.deleteAPIByID(apiId3);
    }

}
