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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.File;
import java.util.ArrayList;

import static org.testng.Assert.assertEquals;

/**
 * This test case is used to test the API creation with WSDL definitions
 */
@SetEnvironment(executionEnvironments = {ExecutionEnvironment.ALL})
public class WSDLImportTestCase
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
    private String userName;
    private String password;
    private String tenantDomain;
    private final String KEYSTORE_FILE_PATH_CLIENT =
            TestConfigurationProvider.getResourceLocation() + File.separator + "keystores" + File.separator + "products"
                    + File.separator + "wso2carbon.jks";
    private final String TRUSTSTORE_FILE_PATH_CLIENT =
            TestConfigurationProvider.getResourceLocation() + File.separator + "keystores" + File.separator + "products"
                    + File.separator + "client-truststore.jks";

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        log.info("WSDLImportTestCase  Initiated");

        super.init();
        tenantDomain = storeContext.getContextTenant().getDomain();
        userName = keyManagerContext.getContextTenant().getTenantAdmin().getUserName();
        password = keyManagerContext.getContextTenant().getTenantAdmin().getPassword();
        grantTypes = new ArrayList<>();
        publisherURLHttps = publisherUrls.getWebAppURLHttps();
        backendEndWSDL = getGatewayURLNhttp() + "services/echo?wsdl";

        //Setting the system properties to call the etcd endpoint
        System.setProperty("javax.net.ssl.keyStoreType", "JKS");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.keyStore", KEYSTORE_FILE_PATH_CLIENT);
        System.setProperty("javax.net.ssl.keyStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.trustStore", TRUSTSTORE_FILE_PATH_CLIENT);
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
    }

    @Test(groups = {"wso2.am"}, description = "Importing WSDL API definition and create API")
    public void testWsdlDefinitionImport() throws Exception {
        log.info("testWsdlDefinitionImport  Initiated");

        //Create additional properties object
        ArrayList<String> environment = new ArrayList<String>();
        environment.add(Constants.GATEWAY_ENVIRONMENT);

        ArrayList<String> policies = new ArrayList<String>();
        policies.add(APIMIntegrationConstants.API_TIER.UNLIMITED);

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
        additionalPropertiesObj.put("policies", policies);

        //create API by importing the WSDL definition as .wsdl file
        String wsdlDefinitionPath = FrameworkPathUtil.getSystemResourceLocation() + "wsdl" + File.separator + "Sample.wsdl";

        File file = new File(wsdlDefinitionPath);
        APIDTO apidto = restAPIPublisher
                .importWSDLSchemaDefinition(file, backendEndUrl, additionalPropertiesObj.toString(), "SOAP");

        //Make sure API is created properly
        assertEquals(apidto.getName(), API_NAME);
        assertEquals(apidto.getContext(), "/" + API_CONTEXT);
        importApiId = apidto.getId();
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(importApiId, restAPIPublisher);
        waitForAPIDeploymentSync(userName, API_NAME, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);

        restAPIPublisher.changeAPILifeCycleStatus(importApiId, Constants.PUBLISHED);
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(importApiId);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode());

        //create API by importing the WSDL definition as .zip file (using archive)
        additionalPropertiesObj.put("provider", user.getUserName());
        additionalPropertiesObj.put("name", WSDL_ZIP_API_NAME);
        additionalPropertiesObj.put("version", API_VERSION);
        additionalPropertiesObj.put("context", WSDL_ZIP_API_CONTEXT);
        String wsdlZipDefinitionPath = FrameworkPathUtil.getSystemResourceLocation() + "wsdl" + File.separator + "Sample.zip";

        File zipFile = new File(wsdlZipDefinitionPath);
        APIDTO zipApidto = restAPIPublisher
                .importWSDLSchemaDefinition(zipFile, backendEndUrl, additionalPropertiesObj.toString(), "SOAP");

        //Make sure API is created properly
        assertEquals(zipApidto.getName(), WSDL_ZIP_API_NAME);
        assertEquals(zipApidto.getContext(), "/" + WSDL_ZIP_API_CONTEXT);
        importZipApiId = zipApidto.getId();
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(importZipApiId, restAPIPublisher);
        waitForAPIDeploymentSync(userName, WSDL_ZIP_API_NAME, API_VERSION, APIMIntegrationConstants.IS_API_EXISTS);

        restAPIPublisher.changeAPILifeCycleStatus(importZipApiId, Constants.PUBLISHED);
        HttpResponse createdApiResponse2 = restAPIPublisher.getAPI(importZipApiId);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse2.getResponseCode());

    }

    @Test(groups = {"wso2.am"}, description = "Get WSDL API definition of the created API", dependsOnMethods = "testWsdlDefinitionImport")
    public void testGetWsdlDefinitions() throws Exception {
        log.info("testGetWsdlDefinition  Initiated");

        // get wsdl definition of the API created with .wsdl file
        ApiResponse<Void> response = restAPIPublisher.getWSDLSchemaDefinitionOfAPI(importApiId);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());

        // get wsdl definition of the API created with .zip file
        ApiResponse<Void> zipFlowResponse = restAPIPublisher.getWSDLSchemaDefinitionOfAPI(importZipApiId);
        assertEquals(Response.Status.OK.getStatusCode(), zipFlowResponse.getStatusCode());
    }

    @Test(groups = {"wso2.am"}, description = "Download WSDL API definition of the created API from the store", dependsOnMethods = "testGetWsdlDefinitions")
    public void testDownloadWsdlDefinitionsFromStore() throws Exception {
        log.info("testDownloadWsdlDefinitionFromStore  Initiated");

        String environmentName = Constants.GATEWAY_ENVIRONMENT;
        //wsdl definition of the API created with .wsdl file from the store
        org.wso2.am.integration.clients.store.api.ApiResponse<Void> response = restAPIStore
                .downloadWSDLSchemaDefinitionOfAPI(importApiId, environmentName);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatusCode());

        //wsdl definition of the API created with .zip file from the store
        org.wso2.am.integration.clients.store.api.ApiResponse<Void> zipFlowResponse = restAPIStore
                .downloadWSDLSchemaDefinitionOfAPI(importZipApiId, environmentName);
        assertEquals(Response.Status.OK.getStatusCode(), zipFlowResponse.getStatusCode());
    }

    @AfterClass(alwaysRun = true)
    void destroy() throws Exception {
        undeployAndDeleteAPIRevisionsUsingRest(importApiId, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(importZipApiId, restAPIPublisher);
        restAPIPublisher.deleteAPI(importApiId);
        restAPIPublisher.deleteAPI(importZipApiId);
    }

}
