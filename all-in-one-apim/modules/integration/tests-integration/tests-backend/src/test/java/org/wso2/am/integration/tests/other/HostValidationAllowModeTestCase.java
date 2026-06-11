/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.other;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ApiEndpointValidationResponseDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

/**
 * Integration tests for outbound host validation with platform-level allow mode (NetworkSecurityAccessControl).
 */
public class HostValidationAllowModeTestCase extends APIMIntegrationBaseTest {

    // Not in the platform allowlist (["169.254.*", "*.internal", "test.com"]) — expected to be blocked.
    private static final String NON_PLATFORM_LISTED_URL = "http://api.allowed.example.com/endpoint";
    private static final String PRIVATE_IP_URL          = "http://192.168.1.1/api";
    private static final String BLOCKED_URL             = "http://attacker.internal.corp/endpoint";

    private String originalTenantConfig;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(TestUserMode.SUPER_TENANT_ADMIN);
        originalTenantConfig = restAPIAdmin.getTenantConfig();
    }

    @Test(groups = {"wso2.am"},
            description = "[allow mode]: non-platform-listed URL is blocked by platform allow mode")
    public void testAllowMode_NonPlatformListedURLBlocked() throws Exception {
        ApiEndpointValidationResponseDTO dto = restAPIPublisher.validateEndpointRaw(NON_PLATFORM_LISTED_URL, null);
        Assert.assertNotNull(dto, "[allow mode] Endpoint validation response must not be null");
        Assert.assertNotNull(dto.getError(), "[allow mode] Expected error for URL not in platform hosts list");
        Assert.assertTrue(dto.getError().contains("not trusted"),
                "[allow mode] Expected allow-mode block, got: " + dto.getError());
    }

    @Test(groups = {"wso2.am"},
            description = "[allow mode]: private IP URL is blocked by platform allow mode",
            dependsOnMethods = "testAllowMode_NonPlatformListedURLBlocked")
    public void testAllowMode_PrivateIPURLBlocked() throws Exception {
        ApiEndpointValidationResponseDTO dto = restAPIPublisher.validateEndpointRaw(PRIVATE_IP_URL, null);
        Assert.assertNotNull(dto, "[allow mode] Endpoint validation response must not be null");
        Assert.assertNotNull(dto.getError(),
                "[allow mode] Expected error for private IP not in allow mode hosts list");
        Assert.assertTrue(dto.getError().contains("not trusted"),
                "[allow mode] Expected block, got: " + dto.getError());
    }

    @Test(groups = {"wso2.am"},
            description = "[allow mode]: platform allow mode overrides tenant-level allow mode config",
            dependsOnMethods = "testAllowMode_PrivateIPURLBlocked")
    public void testAllowMode_PlatformOverridesTenantAllowMode() throws Exception {
        enableTenantAllowMode(new String[]{"*.corp"});
        try {
            ApiEndpointValidationResponseDTO dto = restAPIPublisher.validateEndpointRaw(BLOCKED_URL, null);
            Assert.assertNotNull(dto, "[allow mode precedence] Endpoint validation response must not be null");
            Assert.assertNotNull(dto.getError(),
                    "[allow mode precedence] Expected block despite tenant allow mode config");
            Assert.assertTrue(dto.getError().contains("not trusted"),
                    "[allow mode precedence] Platform allow mode must block non-listed URLs despite tenant config, got: "
                            + dto.getError());
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        try {
            restoreOriginalTenantConfig();
        } finally {
            super.cleanUp();
        }
    }

    private void enableTenantAllowMode(String[] patterns) throws Exception {
        JSONParser parser = new JSONParser();
        JSONObject config = (JSONObject) parser.parse(restAPIAdmin.getTenantConfig());
        JSONObject accessControl = new JSONObject();
        accessControl.put("Mode", "allow");
        JSONArray hostsArray = new JSONArray();
        for (String p : patterns) {
            hostsArray.add(p);
        }
        accessControl.put("Hosts", hostsArray);
        config.put("NetworkSecurityAccessControl", accessControl);
        restAPIAdmin.updateTenantConfig(config);
    }

    private void restoreOriginalTenantConfig() throws Exception {
        if (originalTenantConfig != null) {
            JSONParser parser = new JSONParser();
            JSONObject config = (JSONObject) parser.parse(originalTenantConfig);
            restAPIAdmin.updateTenantConfig(config);
        }
    }
}
