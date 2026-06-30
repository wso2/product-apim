/*
 *Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.am.integration.tests.server.restart;

import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.http.HTTPSClientUtils;
import org.wso2.am.integration.tests.api.lifecycle.APIManagerLifecycleBaseTest;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Verifies that an access token revoked before a server restart continues to be rejected by the
 * Control Plane (DevPortal) REST API after the server is restarted.
 * <p>
 * The JWT type application, the JWT user access token and its revocation are set up in
 * {@link ServerRestartTestCase} before the server restart. This test class reads the revoked token
 * from the test context and asserts that the Control Plane still rejects it after the restart, which
 * guards the fix for https://github.com/wso2-enterprise/wso2-apim-internal/issues/17125.
 */
public class CPRevokedJWTTokenServerRestartTestCase extends APIManagerLifecycleBaseTest {

    private String cpRevokedAccessToken;
    private String cpApplicationsUrl;
    private Map<String, String> requestHeaders;

    @BeforeClass
    public void initialize(ITestContext ctx) throws Exception {
        super.init();

        cpRevokedAccessToken = (String) ctx.getAttribute("cpRevokedAccessToken");
        cpApplicationsUrl = getStoreURLHttps() + "api/am/devportal/v3/applications";
        requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer " + cpRevokedAccessToken);
    }

    @Test(groups = "wso2.am", description = "Test that a token revoked before a server restart is still "
            + "rejected by the Control Plane REST API after the restart")
    public void testRevokedTokenRejectionByControlPlaneAfterRestart() throws Exception {

        Assert.assertNotNull(cpRevokedAccessToken,
                "Revoked access token was not set up before the server restart");

        // The token was revoked before the server restart (verified in ServerRestartTestCase). After the
        // restart, the Control Plane REST API must continue to reject the revoked token. Allow the server a
        // short while to settle after the restart, then invoke the Control Plane once and assert the revoked
        // token is rejected.
        Thread.sleep(5000L);
        HttpResponse invocationResponse = HTTPSClientUtils.doGet(cpApplicationsUrl, requestHeaders);
        int responseCode = invocationResponse.getResponseCode();

        if (responseCode != HTTP_RESPONSE_CODE_UNAUTHORIZED && responseCode != HTTP_RESPONSE_CODE_OK) {
            throw new APIManagerIntegrationTestException("Unexpected response received when invoking the "
                    + "Control Plane REST API after the server restart. Response received: "
                    + invocationResponse.getData() + ":" + invocationResponse.getResponseMessage());
        }

        Assert.assertEquals(responseCode, HTTP_RESPONSE_CODE_UNAUTHORIZED,
                "Revoked token is accepted by the Control Plane REST API after the server restart. Expected "
                        + "response code: " + HTTP_RESPONSE_CODE_UNAUTHORIZED + ", but got: " + responseCode);
    }
}
