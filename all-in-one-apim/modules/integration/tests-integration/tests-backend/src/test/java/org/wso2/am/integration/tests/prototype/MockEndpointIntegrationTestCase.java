/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

package org.wso2.am.integration.tests.prototype;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.ws.rs.core.Response;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Integration tests for mock implementation (inline JS mediation scripts) on the API Gateway,
 * aligned with the official Mock Implementation guide: generate scripts from OpenAPI, apply the
 * modified inline script (including {@code uri.var.petId} branching), and invoke via the gateway.
 */
public class MockEndpointIntegrationTestCase extends APIMIntegrationBaseTest {

    private static final String API_VERSION = "1.0.0";
    private static final String API_CONTEXT = "/mockpetstore";
    private static final String APPLICATION_NAME = "MockEndpointIntegrationTestCaseApp";
    private static final String RESOURCE_PATH = "oas" + File.separator + "v3" + File.separator + "mock-endpoint"
            + File.separator;
    private static final String GET_PET_PATH = "/pet/{petId}";

    private String apiId;
    private String apiName;
    private String applicationId;

    /**
     * @param userMode tenant user mode for multitenant test execution
     */
    @Factory(dataProvider = "userModeDataProvider")
    public MockEndpointIntegrationTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    /** Provides super-tenant and tenant user modes for parameterized test runs. */
    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_USER}
        };
    }

    /** Initializes publisher, store, and gateway clients for the configured user mode. */
    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws APIManagerIntegrationTestException {
        super.init(userMode);
    }

    /**
     * End-to-end test aligned with {@link PrototypedAPITestcase#testOAS3InlinePrototypeWithMock}:
     * import, prototype, generate mock scripts, apply doc-style mediation script, deploy, subscribe,
     * and invoke gateway with petId and responseCode variants.
     */
    @Test(groups = {"wso2.am"}, description = "Mock endpoint E2E: generate script, apply modifications, deploy and invoke")
    public void testMockEndpointEndToEnd() throws Exception {
        apiId = importMockPetStoreApi();
        restAPIPublisher.changeAPILifeCycleStatus(apiId, Constants.DEPLOY_AS_PROTOTYPE);

        HttpResponse mockGenResponse = restAPIPublisher.generateMockScript(apiId);
        Assert.assertEquals(mockGenResponse.getResponseCode(), 200, "Mock script generation failed");

        String generatedSwagger = restAPIPublisher.getSwaggerByID(apiId);
        Assert.assertTrue(generatedSwagger.contains("x-mediation-script"),
                "Generated swagger does not contain x-mediation-script");
        Assert.assertTrue(generatedSwagger.contains("query.param.responseCode"),
                "Auto-generated mock script does not include responseCode handling from OAS parser");
        Assert.assertTrue(generatedSwagger.contains("mc.setPayloadJSON"),
                "Auto-generated mock script does not set JSON payload");
        Assert.assertTrue(generatedSwagger.contains(GET_PET_PATH),
                "Generated swagger does not contain " + GET_PET_PATH);

        applyModifiedMediationScriptFromDoc();

        String updatedSwagger = restAPIPublisher.getSwaggerByID(apiId);
        Assert.assertTrue(updatedSwagger.contains("MANUALLY ADDED CODE"),
                "Modified mediation script was not saved to swagger");
        Assert.assertTrue(updatedSwagger.contains("uri.var.petId"),
                "Modified mediation script does not contain petId path parameter handling");
        Assert.assertTrue(updatedSwagger.contains("German Shepherd"),
                "Modified mediation script does not contain petId=1 branch payload");

        HttpResponse apiResponse = restAPIPublisher.getAPI(apiId);
        Assert.assertEquals(apiResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Failed to retrieve API details. Response: " + apiResponse.getData());
        APIDTO apiDto = new Gson().fromJson(apiResponse.getData(), APIDTO.class);
        Assert.assertEquals(apiDto.getEndpointImplementationType(), APIDTO.EndpointImplementationTypeEnum.INLINE,
                "API endpointImplementationType is not INLINE for mock implementation");
        String invocationContext = resolveGatewayInvocationContext(apiDto.getContext());

        createAPIRevisionAndDeployUsingRest(apiId, restAPIPublisher);
        waitForAPIDeploymentSync(user.getUserName(), apiName, API_VERSION,
                APIMIntegrationConstants.IS_API_EXISTS);

        HttpResponse applicationResponse = restAPIStore.createApplication(
                APPLICATION_NAME + userMode, "Mock Endpoint Test Application",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.JWT);
        Assert.assertTrue(applicationResponse.getResponseCode() >= 200
                        && applicationResponse.getResponseCode() < 300,
                "Application creation failed. Response: " + applicationResponse.getData());
        Assert.assertNotNull(applicationResponse.getData(), "Application creation returned no application id");
        applicationId = applicationResponse.getData();
        restAPIStore.subscribeToAPI(apiId, applicationId, APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED);

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);

        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        Assert.assertNotNull(applicationKeyDTO.getToken(), "Application key token is null");
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        Assert.assertNotNull(accessToken, "Access token is null after key generation");

        waitForAPIDeployment();

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("accept", "application/json");
        String baseUrl = getAPIInvocationURLHttps(invocationContext, API_VERSION);

        HttpResponse petZeroResponse = HTTPSClientUtils.doGet(baseUrl + "/pet/0", requestHeaders);
        Assert.assertEquals(petZeroResponse.getResponseCode(), 200,
                "Mock endpoint invocation failed for petId=0. URL: " + baseUrl + "/pet/0 Response: "
                        + petZeroResponse.getData());
        JSONObject petZeroBody = new JSONObject(petZeroResponse.getData());
        Assert.assertEquals(petZeroBody.getInt("id"), 10,
                "petId=0 should return default mock payload with id 10. Response: " + petZeroResponse.getData());
        Assert.assertEquals(petZeroBody.getJSONObject("category").getString("name"), "Dogs",
                "petId=0 should return Dogs category. Response: " + petZeroResponse.getData());

        HttpResponse petOneResponse = HTTPSClientUtils.doGet(baseUrl + "/pet/1", requestHeaders);
        Assert.assertEquals(petOneResponse.getResponseCode(), 200,
                "Mock endpoint invocation failed for petId=1. URL: " + baseUrl + "/pet/1 Response: "
                        + petOneResponse.getData());
        JSONObject petOneBody = new JSONObject(petOneResponse.getData());
        Assert.assertEquals(petOneBody.getInt("id"), 1,
                "petId=1 should return mock payload with id 1. Response: " + petOneResponse.getData());
        Assert.assertEquals(petOneBody.getJSONArray("tags").getJSONObject(0).getString("name"),
                "German Shepherd",
                "petId=1 should return manually modified mock payload. Response: " + petOneResponse.getData());

        HttpResponse responseCodeResponse = HTTPSClientUtils.doGet(baseUrl + "/pet/0?responseCode=501",
                requestHeaders);
        Assert.assertEquals(responseCodeResponse.getResponseCode(), 501,
                "Expected HTTP 501 for responseCode=501. URL: " + baseUrl
                        + "/pet/0?responseCode=501 Response: " + responseCodeResponse.getData());
        JSONObject response501Body = new JSONObject(responseCodeResponse.getData());
        Assert.assertEquals(response501Body.getString("message"), "Not Implemented",
                "Mock 501 response body mismatch. Response: " + responseCodeResponse.getData());
    }

    /** Deletes the test application and removes APIs created during the test run. */
    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (applicationId != null) {
            restAPIStore.deleteApplication(applicationId);
        }
        super.cleanUp();
    }

    /**
     * Replaces the GET /pet/{petId} x-mediation-script with the modified script from the official guide
     * (generated responses map + manual petId branch + responseCode selection).
     */
    private void applyModifiedMediationScriptFromDoc() throws Exception {
        String modifiedScript = readResource(RESOURCE_PATH + "get_pet_modified_mediation_script.js");

        JSONObject swagger = new JSONObject(restAPIPublisher.getSwaggerByID(apiId));
        Assert.assertTrue(swagger.getJSONObject("paths").has(GET_PET_PATH),
                "Swagger is missing path " + GET_PET_PATH + " before applying modified script");
        swagger.getJSONObject("paths").getJSONObject(GET_PET_PATH).getJSONObject("get")
                .put("x-mediation-script", modifiedScript);
        restAPIPublisher.updateSwagger(apiId, swagger.toString());
    }

    /**
     * Strips leading slash and tenant prefix from API context for gateway invocation URLs.
     * Tenant domain is already present in the gateway base URL for tenant user mode.
     */
    private String resolveGatewayInvocationContext(String apiContext) {
        String context = apiContext.startsWith("/") ? apiContext.substring(1) : apiContext;
        String tenantPrefix = "t/" + user.getUserDomain() + "/";
        if (context.startsWith(tenantPrefix)) {
            context = context.substring(tenantPrefix.length());
        }
        return context;
    }

    /** Imports the mock pet store OAS and returns the created API id. */
    private String importMockPetStoreApi() throws Exception {
        apiName = "MockPetStore" + userMode;
        String context = API_CONTEXT;
        if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(user.getUserDomain())) {
            context = "/t/" + user.getUserDomain() + context;
        }

        String oasDefinition = readResource(RESOURCE_PATH + "petstore_oas.json");
        String additionalProperties = readResource(RESOURCE_PATH + "additionalProperties.json");

        JSONObject additionalPropertiesObj = new JSONObject(additionalProperties);
        additionalPropertiesObj.put("name", apiName);
        additionalPropertiesObj.put("provider", user.getUserName());
        additionalPropertiesObj.put("context", context);

        File file = getTempFileWithContent(oasDefinition);
        APIDTO apiDto = restAPIPublisher.importOASDefinition(file, additionalPropertiesObj.toString());
        return apiDto.getId();
    }

    /** Writes content to a temporary file for OAS import. */
    private File getTempFileWithContent(String content) throws Exception {
        File temp = File.createTempFile("swagger", ".json");
        temp.deleteOnExit();
        try (BufferedWriter out = new BufferedWriter(new FileWriter(temp))) {
            out.write(content);
        }
        return temp;
    }

    /**
     * Reads a classpath test resource as UTF-8 text.
     *
     * @param resourcePath classpath-relative resource path
     * @return resource contents
     * @throws NullPointerException if the resource is not found on the classpath
     */
    private String readResource(String resourcePath) throws Exception {
        try (InputStream inputStream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream(resourcePath),
                "Missing test resource: " + resourcePath)) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }
    }
}
