/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.en.When;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Mutual-SSL (mTLS) API-security glue (ports APISecurityMutualSSLCertificateChainValidationTestCase). An API
 * whose securityScheme is {@code mutualssl}/{@code mutualssl_mandatory} authenticates the CLIENT by its TLS
 * certificate: the publisher uploads the accepted certificate to the API, and a client presenting the matching
 * cert on the gateway HTTPS handshake is authorised (200) while a client with no / a wrong cert is rejected
 * (401). The gateway HTTPS listener already ships {@code SSLVerifyClient=optional} (default 4.7.0 pack) and the
 * container exposes port 8243, so no config overlay is needed — {@code baseGatewayUrl} is the HTTPS gateway URL.
 * Client keystores are on the classpath; they are copied to a temp file because the SSL layer needs a real path.
 */
public class MutualSslSteps {

    private static final String KEYSTORE_PASSWORD = "password";

    /**
     * Uploads a client certificate to an API (multipart {@code POST /apis/{apiId}/client-certificates}): the
     * public certificate file + an {@code alias} + a {@code tier}. Publisher-plane. Non-asserting; stores the
     * response so the feature asserts the status.
     */
    @When("I upload client certificate {string} with alias {string} to API {string} for tier {string}")
    public void iUploadClientCertificate(String certPath, String alias, String apiId, String tier) throws Exception {
        String actualApiId = TestContext.resolve(apiId).toString();
        File certFile = Utils.classpathToTempFile(certPath, "mtls", ".cer");

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Map<String, File> files = new HashMap<>();
        files.put("certificate", certFile);
        Map<String, String> formFields = new HashMap<>();
        formFields.put("alias", alias);
        formFields.put("tier", tier);

        HttpResponse response = Requests.postMultipart(Utils.getClientCertificatesURL(Utils.getBaseUrl(), actualApiId),
                headers, files, formFields);
    }

    /**
     * Invokes an mTLS API at the gateway PRESENTING a client certificate (from the given classpath keystore),
     * retrying until the expected status (uploaded certs take a moment to propagate to the gateway — the legacy
     * test slept 120s; here we poll). Stores the last response for a following assertion.
     */
    @When("I invoke the API at gateway context {string} presenting client certificate {string} until response status code becomes {int} within {int} seconds")
    public void iInvokeWithClientCert(String context, String keystorePath, int expectedStatus, int timeoutSeconds)
            throws Exception {
        invokeMtls(context, keystorePath, expectedStatus, timeoutSeconds);
    }

    /**
     * Invokes an mTLS API at the gateway with NO client certificate, retrying until the expected status. Reuses
     * the trust-all singleton client (which offers no key material) — the mandatory-mTLS API must reject it.
     */
    @When("I invoke the API at gateway context {string} with no client certificate until response status code becomes {int} within {int} seconds")
    public void iInvokeWithoutClientCert(String context, int expectedStatus, int timeoutSeconds) throws Exception {
        String endpointUrl = buildUrl(context);
        long endTimeStart = System.currentTimeMillis();
        long endTime = endTimeStart + Math.max(timeoutSeconds * 1000L, 1000L);
        HttpResponse last = null;
        do {
            try {
                last = SimpleHTTPClient.getInstance().doGet(endpointUrl, acceptXml());
                if (last.getResponseCode() == expectedStatus) {
                    TestContext.set("httpResponse", last);
                    return;
                }
            } catch (IOException transientDuringWarmup) {
                // retry
            }
            Utils.pollPause(endTimeStart, 3000);
        } while (System.currentTimeMillis() < endTime);
        finish(last, expectedStatus);
    }

    private void invokeMtls(String context, String keystorePath, int expectedStatus, int timeoutSeconds)
            throws Exception {
        String endpointUrl = buildUrl(context);
        File keystore = Utils.classpathToTempFile(keystorePath, "mtls", ".jks");
        long endTimeStart = System.currentTimeMillis();
        long endTime = endTimeStart + Math.max(timeoutSeconds * 1000L, 1000L);
        HttpResponse last = null;
        do {
            try {
                last = SimpleHTTPClient.getInstance()
                        .doMutualSSLGet(keystore.getAbsolutePath(), KEYSTORE_PASSWORD, endpointUrl, acceptXml());
                if (last.getResponseCode() == expectedStatus) {
                    TestContext.set("httpResponse", last);
                    return;
                }
            } catch (IOException transientDuringWarmup) {
                // TLS handshake / gateway warm-up — retry
            }
            Utils.pollPause(endTimeStart, 3000);
        } while (System.currentTimeMillis() < endTime);
        finish(last, expectedStatus);
    }

    private String buildUrl(String context) {
        String resolvedContext = Utils.resolveContextPlaceholders(context);
        return Utils.getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
    }

    private Map<String, String> acceptXml() {
        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "text/xml");
        return headers;
    }

    private void finish(HttpResponse last, int expectedStatus) {
        assertNotNull(last, "No response received from the mutual-SSL gateway invocation within the timeout");
        TestContext.set("httpResponse", last);
        assertEquals(last.getResponseCode(), expectedStatus,
                "Mutual-SSL invocation did not reach the expected status. Body: " + last.getData());
    }
}
