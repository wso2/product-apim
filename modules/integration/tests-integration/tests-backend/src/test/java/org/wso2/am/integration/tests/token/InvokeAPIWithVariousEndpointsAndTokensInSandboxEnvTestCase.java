/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

package org.wso2.am.integration.tests.token;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIOperationsDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.FrameworkConstants;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.utils.mgt.ServerConfigurationManager;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

@SetEnvironment(executionEnvironments = { ExecutionEnvironment.STANDALONE })
public class InvokeAPIWithVariousEndpointsAndTokensInSandboxEnvTestCase extends APIManagerLifecycleBaseTest {
    private ServerConfigurationManager serverConfigurationManager;
    private static final Log log = LogFactory.getLog(InvokeAPIWithVariousEndpointsAndTokensTestCase.class);
    private List<APIOperationsDTO> apiOperationsDTOList;
    private final String apiTier = APIMIntegrationConstants.API_TIER.UNLIMITED;
    private final String applicationTier = APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED;
    private final String resourceTier = APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED;
    private String gatewayUrl;
    private String providerName;
    private String apiId1;
    private String apiId2;
    private String applicationId;
    ApplicationKeyDTO applicationKeyProductionDTO;
    ApplicationKeyDTO applicationKeySandboxDTO;
    private final String SANDBOX_RESPONSE_BODY = "HelloWSO2 from File 1_Sandbox";
    private final String PRODUCTION_KEY_FOR_API_WITH_NO_PRODUCTION_ENDPOINT_ERROR = "<error><message>Production Key Provided for Sandbox Gateway</message></error>";

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN } };
    }

    @Factory(dataProvider = "userModeDataProvider")
    public InvokeAPIWithVariousEndpointsAndTokensInSandboxEnvTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        log.info("Test starting user mode: " + userMode);

        serverConfigurationManager = new ServerConfigurationManager(gatewayContextWrk);
        serverConfigurationManager.applyConfiguration(new File(
                getAMResourceLocation() + File.separator + "configFiles" + File.separator + "tokenTest" + File.separator + "apiInvokeCombinationsTest" + File.separator + "deployment.toml"));

        // Create an application to subscribe to the APIs
        String applicationName = "InvokeAPIWithVariousEndpointsAndTokens";
        String applicationDescription = "Application for Invoke API with various endpoints and tokens";
        HttpResponse applicationResponse = restAPIStore.createApplication(applicationName, applicationDescription,
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.JWT);
        applicationId = applicationResponse.getData();

        // Generate production and sandbox keys
        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        applicationKeyProductionDTO = restAPIStore.generateKeys(applicationId, "3600", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        applicationKeySandboxDTO = restAPIStore.generateKeys(applicationId, "3600", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.SANDBOX, null, grantTypes);

        if (gatewayContextWrk.getContextTenant().getDomain().equals(FrameworkConstants.SUPER_TENANT_DOMAIN_NAME)) {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp();
        } else {
            gatewayUrl = gatewayUrlsWrk.getWebAppURLNhttp() + "t/" + gatewayContextWrk.getContextTenant()
                    .getDomain() + "/";
        }

        providerName = publisherContext.getContextTenant().getContextUser().getUserName();
    }

    @Test(groups = { "wso2.am" }, description = "Invoke API with both sandbox and production endpoints and tokens")
    public void testInvokeAPIWithBothEndpointsAndTokens() throws Exception {
        // Create an API
        String apiName = "InvokeAPIWithBothEndpointsAndTokens";
        String apiContext = "invokeAPIWithBothEndpointsAndTokens";
        String apiVersion = "1.0.0";
        String apiDescription = "This is a test API created by API manager integration test";
        String endpointUrl = gatewayUrl + apiContext + "/" + apiVersion + "/name";
        String apiSandboxEndpoint = backEndServerUrl.getWebAppURLHttp() + APIMIntegrationConstants.SANDBOXEP1_WEB_APP_NAME;
        String apiProductionEndpoint = backEndServerUrl.getWebAppURLHttp() + APIMIntegrationConstants.PRODEP1_WEB_APP_NAME;

        // Add the resource
        apiOperationsDTOList = new ArrayList<>();
        APIOperationsDTO apiOperationDTO = new APIOperationsDTO();
        apiOperationDTO.setTarget("/name");
        apiOperationDTO.setVerb("GET");
        apiOperationDTO.authType(
                APIMIntegrationConstants.ResourceAuthTypes.APPLICATION_AND_APPLICATION_USER.getAuthType());
        apiOperationDTO.setThrottlingPolicy(resourceTier);
        apiOperationsDTOList.add(apiOperationDTO);

        APIRequest apiRequest = new APIRequest(apiName, apiContext, new URL(apiProductionEndpoint),
                new URL(apiSandboxEndpoint));
        apiRequest.setTiersCollection(apiTier);
        apiRequest.setTier(apiTier);
        apiRequest.setVersion(apiVersion);
        apiRequest.setOperationsDTOS(apiOperationsDTOList);
        apiRequest.setVisibility("public");
        apiRequest.setDescription(apiDescription);
        apiRequest.setProvider(providerName);
        APIIdentifier apiIdentifier = new APIIdentifier(providerName, apiName, apiVersion);

        apiId1 = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationId,
                applicationTier);

        // Invoke the API with sandbox endpoint and JWT token
        Map<String, String> requestHeaders = new HashMap<>();
        assert applicationKeySandboxDTO.getToken() != null;
        requestHeaders.put("Authorization", "Bearer " + applicationKeySandboxDTO.getToken().getAccessToken());
        HttpResponse sandboxResponse = HttpRequestUtil.doGet(endpointUrl, requestHeaders);
        assertEquals(sandboxResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertEquals(sandboxResponse.getData(), SANDBOX_RESPONSE_BODY, "Response body mismatch");

        // Invoke the API with production endpoint and JWT token
        requestHeaders = new HashMap<>();
        assert applicationKeyProductionDTO.getToken() != null;
        requestHeaders.put("Authorization", "Bearer " + applicationKeyProductionDTO.getToken().getAccessToken());
        HttpResponse productionResponse = HttpRequestUtil.doGet(endpointUrl, requestHeaders);
        assertEquals(productionResponse.getResponseCode(), HTTP_RESPONSE_CODE_UNAUTHORIZED, "Response code mismatched");
        assertEquals(productionResponse.getData(), PRODUCTION_KEY_FOR_API_WITH_NO_PRODUCTION_ENDPOINT_ERROR, "Response body mismatch");

        // Update the resource auth type to none
        apiOperationDTO = apiOperationsDTOList.get(0);
        apiOperationDTO.authType(APIMIntegrationConstants.ResourceAuthTypes.NONE.getAuthType());
        apiOperationsDTOList.clear();
        apiOperationsDTOList.add(apiOperationDTO);
        apiRequest.setOperationsDTOS(apiOperationsDTOList);
        HttpResponse updateAPIHTTPResponse = restAPIPublisher.updateAPI(apiRequest, apiId1);

        // Undeploy and Delete API Revisions
        undeployAndDeleteAPIRevisionsUsingRest(apiId1, restAPIPublisher);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId1, restAPIPublisher);

        waitForAPIDeploymentSync(providerName, apiName, apiVersion, APIMIntegrationConstants.IS_API_EXISTS);

        assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Update API Response Code is invalid. " + getAPIIdentifierString(apiIdentifier));
        assertNotNull(updateAPIHTTPResponse.getData(),
                "Error in API Update in " + getAPIIdentifierString(apiIdentifier));

        // Invoke the updated API with sandbox endpoint and JWT token
        requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer " + applicationKeySandboxDTO.getToken().getAccessToken());
        sandboxResponse = HttpRequestUtil.doGet(endpointUrl, requestHeaders);
        assertEquals(sandboxResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertEquals(sandboxResponse.getData(), SANDBOX_RESPONSE_BODY, "Response body mismatch");

        // Invoke the updated API with production endpoint and JWT token
        requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer " + applicationKeyProductionDTO.getToken().getAccessToken());
        productionResponse = HttpRequestUtil.doGet(endpointUrl, requestHeaders);
        assertEquals(productionResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertEquals(productionResponse.getData(), SANDBOX_RESPONSE_BODY, "Response body mismatch");
    }

    @Test(groups = { "wso2.am" }, description = "Invoke API with sandbox endpoint and both tokens")
    public void testInvokeAPIWithSandboxEndpointAndBothTokens() throws Exception {
        // Create an API
        String apiName = "InvokeAPIWithSandboxEndpointAndBothTokens";
        String apiContext = "invokeAPIWithSandboxEndpointAndBothTokens";
        String apiVersion = "1.0.0";
        String apiDescription = "This is a test API created by API manager integration test";
        String endpointUrl = gatewayUrl + apiContext + "/" + apiVersion + "/name";
        String apiSandboxEndpoint = backEndServerUrl.getWebAppURLHttp() + APIMIntegrationConstants.SANDBOXEP1_WEB_APP_NAME;
        List<String> endpointLB = new ArrayList<>();
        endpointLB.add(apiSandboxEndpoint);

        // Add the resource
        apiOperationsDTOList = new ArrayList<>();
        APIOperationsDTO apiOperationDTO = new APIOperationsDTO();
        apiOperationDTO.setTarget("/name");
        apiOperationDTO.setVerb("GET");
        apiOperationDTO.authType(
                APIMIntegrationConstants.ResourceAuthTypes.APPLICATION_AND_APPLICATION_USER.getAuthType());
        apiOperationDTO.setThrottlingPolicy(resourceTier);
        apiOperationsDTOList.add(apiOperationDTO);

        APIRequest apiRequest = new APIRequest(apiName, apiContext, apiVersion, null, endpointLB);
        apiRequest.setTiersCollection(apiTier);
        apiRequest.setTier(apiTier);
        apiRequest.setVersion(apiVersion);
        apiRequest.setOperationsDTOS(apiOperationsDTOList);
        apiRequest.setVisibility("public");
        apiRequest.setDescription(apiDescription);
        apiRequest.setProvider(providerName);
        APIIdentifier apiIdentifier = new APIIdentifier(providerName, apiName, apiVersion);

        apiId2 = createPublishAndSubscribeToAPIUsingRest(apiRequest, restAPIPublisher, restAPIStore, applicationId,
                applicationTier);

        // Invoke the API with sandbox endpoint and JWT token
        Map<String, String> requestHeaders = new HashMap<>();
        assert applicationKeySandboxDTO.getToken() != null;
        requestHeaders.put("Authorization", "Bearer " + applicationKeySandboxDTO.getToken().getAccessToken());
        HttpResponse sandboxResponse = HttpRequestUtil.doGet(endpointUrl, requestHeaders);
        assertEquals(sandboxResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertEquals(sandboxResponse.getData(), SANDBOX_RESPONSE_BODY, "Response body mismatch");

        // Invoke the API with production endpoint and JWT token
        requestHeaders = new HashMap<>();
        assert applicationKeyProductionDTO.getToken() != null;
        requestHeaders.put("Authorization", "Bearer " + applicationKeyProductionDTO.getToken().getAccessToken());
        HttpResponse productionResponse = HttpRequestUtil.doGet(endpointUrl, requestHeaders);
        assertEquals(productionResponse.getResponseCode(), HTTP_RESPONSE_CODE_UNAUTHORIZED, "Response code mismatched");
        assertEquals(productionResponse.getData(), PRODUCTION_KEY_FOR_API_WITH_NO_PRODUCTION_ENDPOINT_ERROR,
                "Response body mismatch");

        // Update the resource auth type to none
        apiOperationDTO = apiOperationsDTOList.get(0);
        apiOperationDTO.authType(APIMIntegrationConstants.ResourceAuthTypes.NONE.getAuthType());
        apiOperationsDTOList.clear();
        apiOperationsDTOList.add(apiOperationDTO);
        apiRequest.setOperationsDTOS(apiOperationsDTOList);
        HttpResponse updateAPIHTTPResponse = restAPIPublisher.updateAPI(apiRequest, apiId2);

        // Undeploy and Delete API Revisions
        undeployAndDeleteAPIRevisionsUsingRest(apiId2, restAPIPublisher);
        // Create Revision and Deploy to Gateway
        createAPIRevisionAndDeployUsingRest(apiId2, restAPIPublisher);

        waitForAPIDeploymentSync(providerName, apiName, apiVersion, APIMIntegrationConstants.IS_API_EXISTS);

        assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Update API Response Code is invalid. " + getAPIIdentifierString(apiIdentifier));
        assertNotNull(updateAPIHTTPResponse.getData(),
                "Error in API Update in " + getAPIIdentifierString(apiIdentifier));

        // Invoke the updated API with sandbox endpoint and JWT token
        requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer " + applicationKeySandboxDTO.getToken().getAccessToken());
        sandboxResponse = HttpRequestUtil.doGet(endpointUrl, requestHeaders);
        assertEquals(sandboxResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertEquals(sandboxResponse.getData(), SANDBOX_RESPONSE_BODY, "Response body mismatch");

        // Invoke the updated API with production endpoint and JWT token
        requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer " + applicationKeyProductionDTO.getToken().getAccessToken());
        productionResponse = HttpRequestUtil.doGet(endpointUrl, requestHeaders);
        assertEquals(productionResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched");
        assertEquals(productionResponse.getData(), SANDBOX_RESPONSE_BODY, "Response body mismatch");
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        undeployAndDeleteAPIRevisionsUsingRest(apiId1, restAPIPublisher);
        undeployAndDeleteAPIRevisionsUsingRest(apiId2, restAPIPublisher);
        restAPIPublisher.deleteAPI(apiId1);
        restAPIPublisher.deleteAPI(apiId2);
        serverConfigurationManager.restoreToLastConfiguration();
        super.cleanUp();
    }
}
