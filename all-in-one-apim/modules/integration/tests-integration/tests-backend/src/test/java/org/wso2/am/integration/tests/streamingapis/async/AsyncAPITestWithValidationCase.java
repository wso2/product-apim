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

package org.wso2.am.integration.tests.streamingapis.async;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.ApiResponse;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.SubscriptionDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class AsyncAPITestWithValidationCase extends APIMIntegrationBaseTest {

    private final String ASYNC_TYPE = "ASYNC";
    private final String ASYNC_API_V2_NAME = "AsyncAPI";
    private final String ASYNC_API_V2_CONTEXT = "async";
    private final String ASYNC_API_V2_VERSION = "1.0.0";
    private final String ASYNC_API_V2_VERSION_APP_NAME = "testAppAsync";

    private final String INVALID_ASYNC_API_V2_NAME = "InvalidAsyncAPI";
    private final String INVALID_ASYNC_API_V2_CONTEXT = "invalid/async";
    private final String INVALID_ASYNC_API_V2_VERSION = "1.0.0";

    private final String ASYNC_API_V3_NAME = "AsyncAPIV3";
    private final String ASYNC_API_V3_CONTEXT = "async/v3";
    private final String ASYNC_API_V3_VERSION = "1.0.0";
    private final String ASYNC_API_V3_VERSION_APP_NAME = "testAppAsyncAPIV3";

    private String provider;
    private String apiId;
    private String asyncApiV3Id;
    private String appId;
    private String asyncAPIDefinition;

    @Factory(dataProvider = "userModeDataProvider")
    public AsyncAPITestWithValidationCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN }
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        provider = user.getUserName();
    }

    @Test(description = "Create Async API V2 as a normal API")
    public void testCreateAsyncApiWithoutAdvertiseOnly() throws Exception {
        asyncAPIDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("async" + File.separator + "asyncapiv2.yaml"),
                StandardCharsets.UTF_8);

        File file = getTempFileWithContent(asyncAPIDefinition);

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("name", ASYNC_API_V2_NAME);
        additionalPropertiesObj.put("context", ASYNC_API_V2_CONTEXT);
        additionalPropertiesObj.put("version", ASYNC_API_V2_VERSION);
        additionalPropertiesObj.put("type", ASYNC_TYPE);

        // create Async API
        ApiResponse<APIDTO> response = null;
        try {
            response = restAPIPublisher
                    .importAsyncAPIDefinition(file, additionalPropertiesObj.toString());
        } catch (ApiException e) {
            Assert.assertTrue(e.getResponseBody()
                    .contains("ASYNC type APIs only can be created as third party APIs"));
        }
        if (response != null) {
            Assert.fail();
        }
    }

    @Test(description = "Create Async API V3 as a normal API")
    public void testCreateAsyncApiV3WithoutAdvertiseOnly() throws Exception {
        asyncAPIDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("async" + File.separator + "asyncapiv3.yaml"),
                StandardCharsets.UTF_8);

        File file = getTempFileWithContent(asyncAPIDefinition);

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("name", ASYNC_API_V3_NAME);
        additionalPropertiesObj.put("context", ASYNC_API_V3_CONTEXT);
        additionalPropertiesObj.put("version", ASYNC_API_V3_VERSION);
        additionalPropertiesObj.put("type", ASYNC_TYPE);

        // create Async API
        ApiResponse<APIDTO> response = null;
        try {
            response = restAPIPublisher
                    .importAsyncAPIDefinition(file, additionalPropertiesObj.toString());
        } catch (ApiException e) {
            Assert.assertTrue(e.getResponseBody()
                    .contains("ASYNC type APIs only can be created as third party APIs"));
        }
        if (response != null) {
            Assert.fail();
        }
    }

    @Test(description = "Import and Publish Async API V2")
    public void testPublishAsyncApi() throws Exception {
        asyncAPIDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("async" + File.separator + "asyncapiv2.yaml"),
                StandardCharsets.UTF_8);

        File file = getTempFileWithContent(asyncAPIDefinition);

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("name", ASYNC_API_V2_NAME);
        additionalPropertiesObj.put("context", ASYNC_API_V2_CONTEXT);
        additionalPropertiesObj.put("version", ASYNC_API_V2_VERSION);
        additionalPropertiesObj.put("type", ASYNC_TYPE);

        ArrayList<String> policies = new ArrayList<>();
        policies.add(APIMIntegrationConstants.API_TIER.ASYNC_WH_UNLIMITED);
        additionalPropertiesObj.put("policies", policies);

        JSONObject advertiseInfo = new JSONObject();
        advertiseInfo.put("advertised", true);
        advertiseInfo.put("apiExternalProductionEndpoint", "https://test.com");
        advertiseInfo.put("apiExternalSandboxEndpoint", "https://test.com");
        advertiseInfo.put("originalDevPortalUrl", "https://test.com");
        advertiseInfo.put("vendor", "WSO2");
        advertiseInfo.put("apiOwner", provider);
        additionalPropertiesObj.put("advertiseInfo", advertiseInfo);

        // create Async API
        ApiResponse<APIDTO> response = restAPIPublisher
                .importAsyncAPIDefinition(file, additionalPropertiesObj.toString());
        Assert.assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        APIDTO apidto = response.getData();
        apiId = apidto.getId();
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(apiId);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode(),
                ASYNC_API_V2_NAME + " API creation is failed");

        String revisionUUID = null;
        // Create Revision and Deploy to Gateway
        try {
            revisionUUID = createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        } catch (ApiException e) {
            Assert.assertTrue(e.getMessage().contains("Error while adding new API Revision for API : ")
                    || e.getMessage().contains("Creating API Revisions is not supported"));
        }
        if (revisionUUID != null) {
            Assert.fail();
        }

        // publish api
        restAPIPublisher.changeAPILifeCycleStatus(apiId, Constants.PUBLISHED);
        waitForAPIDeployment();
        APIIdentifier apiIdentifier = new APIIdentifier(provider, ASYNC_API_V2_NAME, ASYNC_API_V2_VERSION);
        APIListDTO apiPublisherAllAPIs = restAPIPublisher.getAllAPIs();
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiPublisherAllAPIs),
                "Published API is visible in API Publisher.");
    }

    @Test(description = "Import and Publish Async API V3")
    public void testPublishAsyncApiV3() throws Exception {
        asyncAPIDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("async" + File.separator + "asyncapiv3.yaml"),
                StandardCharsets.UTF_8);

        File file = getTempFileWithContent(asyncAPIDefinition);

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("name", ASYNC_API_V3_NAME);
        additionalPropertiesObj.put("context", ASYNC_API_V3_CONTEXT);
        additionalPropertiesObj.put("version", ASYNC_API_V3_VERSION);
        additionalPropertiesObj.put("type", ASYNC_TYPE);

        ArrayList<String> policies = new ArrayList<>();
        policies.add(APIMIntegrationConstants.API_TIER.ASYNC_WH_UNLIMITED);
        additionalPropertiesObj.put("policies", policies);

        JSONObject advertiseInfo = new JSONObject();
        advertiseInfo.put("advertised", true);
        advertiseInfo.put("apiExternalProductionEndpoint", "https://test.com");
        advertiseInfo.put("apiExternalSandboxEndpoint", "https://test.com");
        advertiseInfo.put("originalDevPortalUrl", "https://test.com");
        advertiseInfo.put("vendor", "WSO2");
        advertiseInfo.put("apiOwner", provider);
        additionalPropertiesObj.put("advertiseInfo", advertiseInfo);

        // create Async API
        ApiResponse<APIDTO> response = restAPIPublisher
                .importAsyncAPIDefinition(file, additionalPropertiesObj.toString());
        Assert.assertEquals(HttpStatus.SC_CREATED, response.getStatusCode());
        APIDTO apidto = response.getData();
        asyncApiV3Id = apidto.getId();
        HttpResponse createdApiResponse = restAPIPublisher.getAPI(asyncApiV3Id);
        assertEquals(Response.Status.OK.getStatusCode(), createdApiResponse.getResponseCode(),
                ASYNC_API_V3_NAME + " API creation is failed");

        String revisionUUID = null;
        // Create Revision and Deploy to Gateway
        try {
            revisionUUID = createAPIRevisionAndDeployUsingRest(asyncApiV3Id, restAPIPublisher);
        } catch (ApiException e) {
            Assert.assertTrue(e.getMessage().contains("Error while adding new API Revision for API : ")
                    || e.getMessage().contains("Creating API Revisions is not supported"));
        }
        if (revisionUUID != null) {
            Assert.fail();
        }

        // publish api
        restAPIPublisher.changeAPILifeCycleStatus(asyncApiV3Id, Constants.PUBLISHED);
        waitForAPIDeployment();
        APIIdentifier apiIdentifier = new APIIdentifier(provider, ASYNC_API_V3_NAME, ASYNC_API_V3_VERSION);
        APIListDTO apiPublisherAllAPIs = restAPIPublisher.getAllAPIs();
        assertTrue(APIMTestCaseUtils.isAPIAvailable(apiIdentifier, apiPublisherAllAPIs),
                "Published API is visible in API Publisher.");
    }

    @Test(description = "Import and Publish InvalidAsync API V2")
    public void testPublishInvalidAsyncApi() throws Exception {
        asyncAPIDefinition = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("async" + File.separator + "asyncapi.yaml"),
                StandardCharsets.UTF_8);

        File file = getTempFileWithContent(asyncAPIDefinition);

        JSONObject additionalPropertiesObj = new JSONObject();
        additionalPropertiesObj.put("name", INVALID_ASYNC_API_V2_NAME);
        additionalPropertiesObj.put("context", INVALID_ASYNC_API_V2_CONTEXT);
        additionalPropertiesObj.put("version", INVALID_ASYNC_API_V2_VERSION);
        additionalPropertiesObj.put("type", ASYNC_TYPE);

        ArrayList<String> policies = new ArrayList<>();
        policies.add(APIMIntegrationConstants.API_TIER.ASYNC_WH_UNLIMITED);
        additionalPropertiesObj.put("policies", policies);

        JSONObject advertiseInfo = new JSONObject();
        advertiseInfo.put("advertised", true);
        advertiseInfo.put("apiExternalProductionEndpoint", "https://test.com");
        advertiseInfo.put("apiExternalSandboxEndpoint", "https://test.com");
        advertiseInfo.put("originalDevPortalUrl", "https://test.com");
        advertiseInfo.put("vendor", "WSO2");
        advertiseInfo.put("apiOwner", provider);
        additionalPropertiesObj.put("advertiseInfo", advertiseInfo);

        // create Async API V2 with invalid def
        ApiResponse<APIDTO> response = null;
        try {
            response = restAPIPublisher
                    .importAsyncAPIDefinition(file, additionalPropertiesObj.toString());
        } catch (ApiException e) {
            // Expected path — new V2 parser rejects invalid asyncapi v2 and publisher returns error
            String respBody = e.getResponseBody();
            Assert.assertTrue(respBody != null && respBody.contains("Implicit OAuth Flow is missing"),
                    "Unexpected AsyncAPI validation response; body: " + respBody);
        }
        if (response != null) {
            Assert.fail();
        }
    }

    @Test(description = "Create Application and subscribe a Async API V2", dependsOnMethods = "testPublishAsyncApi")
    public void testAsyncApiApplicationSubscription() throws Exception {
        HttpResponse applicationResponse = restAPIStore.createApplication(ASYNC_API_V2_VERSION_APP_NAME,
                "", APIMIntegrationConstants.API_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH);
        appId = applicationResponse.getData();
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(apiId, appId,
                APIMIntegrationConstants.API_TIER.ASYNC_WH_UNLIMITED);
        // Validate Subscription of the API
        Assert.assertEquals(subscriptionDTO.getStatus(), SubscriptionDTO.StatusEnum.UNBLOCKED);
    }

    @Test(description = "Create Application and subscribe a Async API V3", dependsOnMethods = "testPublishAsyncApiV3")
    public void testAsyncApiV3ApplicationSubscription() throws Exception {
        HttpResponse applicationResponse = restAPIStore.createApplication(ASYNC_API_V3_VERSION_APP_NAME,
                "", APIMIntegrationConstants.API_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH);
        appId = applicationResponse.getData();
        SubscriptionDTO subscriptionDTO = restAPIStore.subscribeToAPI(asyncApiV3Id, appId,
                APIMIntegrationConstants.API_TIER.ASYNC_WH_UNLIMITED);
        // Validate Subscription of the API
        Assert.assertEquals(subscriptionDTO.getStatus(), SubscriptionDTO.StatusEnum.UNBLOCKED);
    }

    private File getTempFileWithContent(String asyncApi) throws Exception {
        File temp = File.createTempFile("async", ".yaml");
        temp.deleteOnExit();
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.write(asyncApi);
        out.close();
        return temp;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }
}
