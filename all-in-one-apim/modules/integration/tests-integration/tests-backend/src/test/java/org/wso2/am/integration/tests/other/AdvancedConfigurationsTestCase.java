/*
 * Copyright (c) 2021, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

package org.wso2.am.integration.tests.other;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.admin.ApiClient;
import org.wso2.am.integration.clients.admin.ApiException;
import org.wso2.am.integration.clients.admin.api.TenantConfigApi;
import org.wso2.am.integration.test.helpers.AdminApiTestHelper;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AdvancedConfigurationsTestCase extends APIMIntegrationBaseTest {
    private final Log log = LogFactory.getLog(AdvancedConfigurationsTestCase.class);
    private AdminApiTestHelper adminApiTestHelper;

    @Factory(dataProvider = "userModeDataProvider")
    public AdvancedConfigurationsTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][] { new Object[] { TestUserMode.SUPER_TENANT_ADMIN },
                new Object[] { TestUserMode.TENANT_ADMIN },};
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        adminApiTestHelper = new AdminApiTestHelper();
    }

    @Test(groups = { "wso2.am" }, description = "Test Get Tenant Configuration")
    public void testGetTenantConfiguration() throws Exception {
        String tenantConfigBeforeTestCase = restAPIAdmin.getTenantConfig().toString();
        Assert.assertNotNull(tenantConfigBeforeTestCase);
    }

    @Test(groups = { "wso2.am" }, description = "Test add Tenant Configuration", dependsOnMethods = "testGetTenantConfiguration")
    public void testUpdateTenantConfiguration() throws Exception {
        String tenantConfContent = FileUtils.readFileToString(new File(getAMResourceLocation() + File.separator
                + "configFiles" + File.separator + "tenantConf" + File.separator + "tenant-conf.json"), "UTF-8");
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(tenantConfContent);
        restAPIAdmin.updateTenantConfig(jsonObject);
        String updatedConfContent = restAPIAdmin.getTenantConfig().toString();
        Assert.assertNotNull(updatedConfContent);
    }

    @Test(groups = { "wso2.am" }, description = "Test Get Tenant Schema Configuration", dependsOnMethods = "testGetTenantConfiguration")
    public void testGetTenantConfigurationSchema() throws Exception {
        String tenantConfigSchema = restAPIAdmin.getTenantConfigSchema().toString();
        Assert.assertNotNull(tenantConfigSchema);
    }

    @Test(groups = { "wso2.am" }, description = "Test Update Tenant Configuration with Invalid JWT returns 401")
    public void testUpdateTenantConfigurationWithInvalidJWT() throws Exception {
        // Create an invalid JWT with HS256 algorithm
        String invalidJwt = createInvalidJWT();

        // Create a new ApiClient with the invalid JWT
        ApiClient invalidClient = new ApiClient();
        invalidClient.setBasePath(restAPIAdmin.apiAdminClient.getBasePath());
        invalidClient.addDefaultHeader("Authorization", "Bearer " + invalidJwt);

        // Create TenantConfigApi with invalid client
        TenantConfigApi invalidTenantConfigApi = new TenantConfigApi(invalidClient);

        // Load valid tenant config
        String tenantConfContent = FileUtils.readFileToString(new File(getAMResourceLocation() + File.separator
                + "configFiles" + File.separator + "tenantConf" + File.separator + "tenant-conf.json"), "UTF-8");
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(tenantConfContent);

        // Attempt to update tenant config with invalid JWT - should fail with 401
        try {
            invalidTenantConfigApi.updateTenantConfig(jsonObject);
            Assert.fail("Expected ApiException with 401 status code, but request succeeded");
        } catch (ApiException e) {
            Assert.assertEquals(e.getCode(), 401, "Expected 401 Unauthorized but got: " + e.getCode());
            log.info("Successfully received 401 Unauthorized for invalid JWT");
        }
    }

    /**
     * Create an HS256 JWT with an invalid signature.
     * This creates a token with an invalid signature since the server's secret is unknown.
     */
    private String createInvalidJWT() {
        // Base64URL encoding function (replaces + with -, / with _, removes padding)
        java.util.function.Function<byte[], String> base64url = (byte[] input) -> {
            String encoded = Base64.getEncoder().encodeToString(input);
            encoded = encoded.replace("=", "");
            encoded = encoded.replace("+", "-");
            encoded = encoded.replace("/", "_");
            return encoded;
        };

        long now = System.currentTimeMillis() / 1000;

        // Create header with HS256 algorithm
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String headerB64 = base64url.apply(headerJson.getBytes(StandardCharsets.UTF_8));

        // Create payload - using admin user claims
        String payloadJson = "{" +
                "\"sub\":\"admin\"," +
                "\"iss\":\"https://localhost:9443/oauth2/token\"," +
                "\"exp\":" + (now + 3600) + "," +
                "\"iat\":" + now + "," +
                "\"scope\":\"openid apim:admin\"," +
                "\"azp\":\"x\"," +
                "\"aud\":\"https://localhost:9443/oauth2/token\"" +
                "}";
        String payloadB64 = base64url.apply(payloadJson.getBytes(StandardCharsets.UTF_8));

        // Create signing input
        String signingInput = headerB64 + "." + payloadB64;

        // Generate signature with dummy secret - the server should reject this
        // Since the server validates HS256 with its secret, an invalid signature should result in 401
        String invalidSignature = base64url.apply("invalid_signature".getBytes(StandardCharsets.UTF_8));

        return signingInput + "." + invalidSignature;
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
    }
}
