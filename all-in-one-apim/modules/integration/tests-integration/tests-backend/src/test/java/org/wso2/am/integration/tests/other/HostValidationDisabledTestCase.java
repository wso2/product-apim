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

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.ApiEndpointValidationResponseDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;

/**
 * Integration tests for host validation with the feature disabled (no config block present).
 */
public class HostValidationDisabledTestCase extends APIMIntegrationBaseTest {

    private static final String LINK_LOCAL_URL = "http://169.254.169.254/latest/meta-data/";
    private static final String LOOPBACK_URL   = "http://127.0.0.1:9999/api";

    private String apiId;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(TestUserMode.SUPER_TENANT_ADMIN);
        APIRequest apiRequest = new APIRequest("HostValidationDisabledTestAPI", "/hvdisabled",
                new URL(backEndServerUrl.getWebAppURLHttp()
                        + "jaxrs_basic/services/customers/customerservice/"));
        apiRequest.setVersion("1.0.0");
        apiRequest.setProvider(user.getUserName());
        HttpResponse addApiResponse = restAPIPublisher.addAPI(apiRequest);
        Assert.assertNotNull(addApiResponse, "Failed to create test API in setup");
        apiId = addApiResponse.getData();
    }

    @Test(groups = {"wso2.am"},
            description = "Host validation [feature disabled]: all platform checks skipped, nothing blocked")
    public void testHostValidationDisabled_NothingBlocked() throws Exception {
        ApiEndpointValidationResponseDTO s2dto1 = restAPIPublisher.validateEndpointRaw(LINK_LOCAL_URL, apiId);
        Assert.assertNotNull(s2dto1, "[feature disabled] Endpoint validation response must not be null");
        if (s2dto1.getError() != null) {
            Assert.assertFalse(s2dto1.getError().contains("not trusted"),
                    "[feature disabled] Should not block when disabled, error: " + s2dto1.getError());
        }
        ApiEndpointValidationResponseDTO s2dto2 = restAPIPublisher.validateEndpointRaw(LOOPBACK_URL, apiId);
        Assert.assertNotNull(s2dto2, "[feature disabled] Endpoint validation response must not be null");
        if (s2dto2.getError() != null) {
            Assert.assertFalse(s2dto2.getError().contains("not trusted"),
                    "[feature disabled] Should not block loopback when disabled, error: " + s2dto2.getError());
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        if (apiId != null) {
            restAPIPublisher.deleteAPI(apiId);
        }
        super.cleanUp();
    }
}
