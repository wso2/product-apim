/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.integration.tests.publisher;

import static org.testng.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;
import org.wso2.carbon.utils.ServerConstants;

import com.google.gson.Gson;

/**
 * 
 * This test case adds a custom velocity template to add a custom header ( custom-header: apim-test-header) to the
 * response when the API has an additional `property enable.custom.header` with the value as `true`
 *
 */
public class APIAdditionalPropertiesWithStoreVisibilityPropagateToVelocityTemplateTestCase
        extends APIManagerLifecycleBaseTest {

    private String originalContent = null;
    ServerConfigurationManager serverConfigurationManager;
    private final Log log = LogFactory
            .getLog(APIAdditionalPropertiesWithStoreVisibilityPropagateToVelocityTemplateTestCase.class);
    private final String VELOCITY_PATH = "repository" + File.separator + "resources" + File.separator + "api_templates"
            + File.separator + "velocity_template.xml";
    private final String CARBON_HOME = System.getProperty(ServerConstants.CARBON_HOME);

    private static final String MODIFIED_VELOCITY_PATH = FrameworkPathUtil.getSystemResourceLocation() + "artifacts"
            + File.separator + "AM" + File.separator + "configFiles" + File.separator + "velocityTemplates"
            + File.separator + "velocity_template.xml";

    private final String API_NAME = "VelocityModifiedAPI";
    private final String API_CONTEXT = "velocity-modified-api";
    private final String API_TAGS = "testTag1, testTag2, testTag3";
    private final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private final String API_END_POINT_METHOD = "/customers/123";
    private final String API_VERSION_1_0_0 = "1.0.0";
    private final String APPLICATION_NAME = "VelocityModifiedApp";
    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private String apiEndPointUrl;
    private String providerName;
    private String applicationId;
    private String apiId;
    private APIRequest apiRequest;
    private Map<String, String> requestHeaders;
    private ArrayList<String> grantTypes;
    private String accessToken;

    @BeforeClass
    public void setEnvironment() throws Exception {
        super.init();
        serverConfigurationManager = new ServerConfigurationManager(gatewayContextMgt);

        originalContent = getFileContent(CARBON_HOME + File.separator + VELOCITY_PATH);
        String modifiedContent = getFileContent(MODIFIED_VELOCITY_PATH);
        replaceFileContent(modifiedContent, CARBON_HOME + File.separator + VELOCITY_PATH);
        // restart after config change
        serverConfigurationManager.restartGracefully();

        grantTypes = new ArrayList<>();
        apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = user.getUserName();

        apiRequest = new APIRequest(API_NAME, API_CONTEXT, new URL(apiEndPointUrl));
        apiRequest.setVersion(API_VERSION_1_0_0);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setProvider(providerName);
        apiRequest.setTags(API_TAGS);
        apiRequest.setDescription(API_DESCRIPTION);

        HttpResponse applicationResponse = restAPIStore.createApplication(APPLICATION_NAME, "Test Application",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();
        apiId = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "3600", null,
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        accessToken = applicationKeyDTO.getToken().getAccessToken();
        waitForAPIDeployment();

    }

    @Test(groups = { "wso2.am" }, description = "Test invocation of API")
    public void testAPIInvocation() throws Exception {
        waitForAPIDeploymentSync(user.getUserName(), API_NAME, API_VERSION_1_0_0,
                APIMIntegrationConstants.IS_API_EXISTS);
        // Create requestHeaders
        requestHeaders = new HashMap<String, String>();
        requestHeaders.put("accept", "text/xml");
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        HttpResponse invokeResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttps(API_CONTEXT, API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke api");

    }

    @Test(groups = {
            "wso2.am" }, description = "Test invocation of API after adding additional property", 
                    dependsOnMethods = "testAPIInvocation")
    public void testAPIInvocationAfterAddingAdditionalProperty() throws Exception {
        HttpResponse response = restAPIPublisher.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);
        Map<String, String> additionalProperties = new HashMap<String, String>();
        additionalProperties.put("enable.custom.header", "true");
        apidto.setAdditionalProperties(additionalProperties);

        restAPIPublisher.updateAPI(apidto);
        waitForAPIDeployment();
        HttpResponse invokeResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttps(API_CONTEXT, API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke api");

        HttpClient httpclient = HttpClients.createDefault();
        HttpGet get = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + API_END_POINT_METHOD);
        get.setHeader("Authorization", "Bearer " + accessToken);

        org.apache.http.HttpResponse httpResponse = httpclient.execute(get);
        assertEquals(httpResponse.getFirstHeader("custom-header").getValue(), "apim-test-header",
                "Custom header is not available");


    }
    
    @Test(groups = {
            "wso2.am" }, description = "Test invocation of API after adding additional property with devportal "
                    + "visible option", dependsOnMethods = "testAPIInvocation")
    public void testAPIInvocationAfterAddingAdditionalPropertyWithDevportalVisibleOption() throws Exception {
        HttpResponse response = restAPIPublisher.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);
        Map<String, String> additionalProperties = new HashMap<String, String>();
        // set dev portal display to true
        additionalProperties.put("enable.custom.header__display", "true");
        apidto.setAdditionalProperties(additionalProperties);
        restAPIPublisher.updateAPI(apidto);
        waitForAPIDeployment();
        HttpResponse invokeResponse = HttpRequestUtil
                .doGet(getAPIInvocationURLHttps(API_CONTEXT, API_VERSION_1_0_0) + API_END_POINT_METHOD, requestHeaders);
        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Response code mismatched when invoke api");
        HttpClient httpclient = HttpClients.createDefault();
        HttpGet get = new HttpGet(getAPIInvocationURLHttp(API_CONTEXT, API_VERSION_1_0_0) + API_END_POINT_METHOD);
        get.setHeader("Authorization", "Bearer " + accessToken);

        org.apache.http.HttpResponse httpResponse = httpclient.execute(get);
        assertEquals(httpResponse.getFirstHeader("custom-header").getValue(), "apim-test-header",
                "Custom header is not available when devportal visibility enabled");
    }

    @AfterClass
    public void cleanupArtifacts() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(apiId);
        if (originalContent != null) {
            // replace velocity template to the original one
            replaceFileContent(originalContent, CARBON_HOME + File.separator + VELOCITY_PATH);
            serverConfigurationManager.restartGracefully();
        }
    }

    private String getFileContent(String path) {
        String content = "";

        try {
            content = new String(Files.readAllBytes(Paths.get(path)));
        } catch (IOException e) {
            log.error("Error while reading content ", e);
        }
        return content;
    }

    private void replaceFileContent(String content, String path) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(path));
            writer.write(content);
        } catch (IOException e) {
            log.error("Error while writing content ", e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }

            } catch (IOException e) {
            }
        }

    }

}
