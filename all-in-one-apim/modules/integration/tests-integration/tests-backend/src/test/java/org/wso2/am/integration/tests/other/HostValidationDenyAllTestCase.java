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
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.net.URL;

/**
 * Integration tests for host validation with deny_all mode and bpna=true.
 */
public class HostValidationDenyAllTestCase extends APIMIntegrationBaseTest {

    private static final String ALLOWED_URL    = "http://api.allowed.example.com/endpoint";
    private static final String LINK_LOCAL_URL = "http://169.254.169.254/latest/meta-data/";
    private static final String BLOCKED_URL    = "http://attacker.internal.corp/endpoint";

    private String originalTenantConfig;
    private String apiId;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(TestUserMode.SUPER_TENANT_ADMIN);
        originalTenantConfig = restAPIAdmin.getTenantConfig();
        APIRequest apiRequest = new APIRequest("HostValidationDenyAllTestAPI", "/hvdenyall",
                new URL(backEndServerUrl.getWebAppURLHttp()
                        + "jaxrs_basic/services/customers/customerservice/"));
        apiRequest.setVersion("1.0.0");
        apiRequest.setProvider(user.getUserName());
        apiId = restAPIPublisher.addAPI(apiRequest).getData();
    }

    @Test(groups = {"wso2.am"},
            description = "Host validation [deny_all+bpna=true]: all URLs blocked; platform deny_all overrides tenant allowlist")
    public void testDenyAllMode_AllURLsBlocked() throws Exception {
        ApiEndpointValidationResponseDTO s1dto1 = restAPIPublisher.validateEndpointRaw(ALLOWED_URL, apiId);
        Assert.assertNotNull(s1dto1, "[deny_all+bpna=true] Endpoint validation response must not be null");
        Assert.assertNotNull(s1dto1.getError(), "[deny_all+bpna=true] Expected error for non-exception URL");
        Assert.assertTrue(s1dto1.getError().contains("not trusted"),
                "[deny_all+bpna=true] Expected deny_all block, got: " + s1dto1.getError());

        ApiEndpointValidationResponseDTO s1dto2 = restAPIPublisher.validateEndpointRaw(LINK_LOCAL_URL, apiId);
        Assert.assertNotNull(s1dto2, "[deny_all+bpna=true] Endpoint validation response must not be null");
        Assert.assertNotNull(s1dto2.getError(),
                "[deny_all+bpna=true] Expected bpna block for link-local IP");
        Assert.assertTrue(s1dto2.getError().contains("not trusted"),
                "[deny_all+bpna=true] Expected bpna block, got: " + s1dto2.getError());

        enableTenantAllowlist(new String[]{"*.corp"});
        try {
            ApiEndpointValidationResponseDTO s1dto3 = restAPIPublisher.validateEndpointRaw(BLOCKED_URL, apiId);
            Assert.assertNotNull(s1dto3, "[deny_all precedence] Endpoint validation response must not be null");
            Assert.assertNotNull(s1dto3.getError(),
                    "[deny_all precedence] Expected block despite tenant allowlist");
            Assert.assertTrue(s1dto3.getError().contains("not trusted"),
                    "[deny_all precedence] Platform deny_all must override tenant allowlist, got: "
                            + s1dto3.getError());
        } finally {
            restoreOriginalTenantConfig();
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restoreOriginalTenantConfig();
        if (apiId != null) {
            restAPIPublisher.deleteAPI(apiId);
        }
        super.cleanUp();
    }

    private void enableTenantAllowlist(String[] patterns) throws Exception {
        JSONParser parser = new JSONParser();
        JSONObject config = (JSONObject) parser.parse(restAPIAdmin.getTenantConfig());
        JSONObject outboundSecurity = new JSONObject();
        outboundSecurity.put("EnableHostAllowlist", Boolean.TRUE);
        JSONArray patternArray = new JSONArray();
        for (String p : patterns) {
            patternArray.add(p);
        }
        outboundSecurity.put("HostAllowlistPatterns", patternArray);
        config.put("OutboundRequestSecurity", outboundSecurity);
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
