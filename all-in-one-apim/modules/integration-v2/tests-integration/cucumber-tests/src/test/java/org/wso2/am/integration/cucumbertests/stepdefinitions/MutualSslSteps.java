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
        uploadClientCertificate(Utils.getClientCertificatesURL(Utils.getBaseUrl(),
                TestContext.resolve(apiId).toString()), certPath, alias, tier);
    }

    /**
     * Uploads a client certificate to an API for a specific KEY TYPE (multipart
     * {@code POST /apis/{apiId}/client-certs/{keyType}}). The key type is part of the certificate's identity — a
     * cert uploaded under SANDBOX authorises the sandbox key type — and this is the CURRENT endpoint: the un-typed
     * {@code /client-certificates} POST the sibling step uses is marked {@code deprecated} in publisher-api.yaml.
     * Ports the per-key-type certificate uploads of APISecurityTestCase#initialize, which uploads a SANDBOX and a
     * PRODUCTION certificate to the same API. Non-asserting; the feature asserts the status.
     *
     * @param keyType PRODUCTION or SANDBOX
     */
    @When("I upload client certificate {string} with alias {string} and key type {string} to API {string} for tier {string}")
    public void iUploadClientCertificateOfKeyType(String certPath, String alias, String keyType, String apiId,
                                                  String tier) throws Exception {
        uploadClientCertificate(Utils.getClientCertificatesByKeyTypeURL(Utils.getBaseUrl(),
                TestContext.resolve(apiId).toString(), keyType), certPath, alias, tier);
    }

    /** Shared multipart client-certificate upload (certificate + alias + tier) as the acting publisher. */
    private void uploadClientCertificate(String url, String certPath, String alias, String tier) throws Exception {
        File certFile = Utils.classpathToTempFile(certPath, "mtls", ".cer");

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Map<String, File> files = new HashMap<>();
        files.put("certificate", certFile);
        Map<String, String> formFields = new HashMap<>();
        formFields.put("alias", alias);
        formFields.put("tier", tier);

        Requests.postMultipart(url, headers, files, formFields);
    }

    /**
     * Invokes an mTLS API at the gateway PRESENTING a client certificate (from the given classpath keystore),
     * retrying until the expected status (uploaded certs take a moment to propagate to the gateway — the legacy
     * test slept 120s; here we poll). Stores the last response for a following assertion.
     */
    @When("I invoke the API at gateway context {string} presenting client certificate {string} until response status code becomes {int} within {int} seconds")
    public void iInvokeWithClientCert(String context, String keystorePath, int expectedStatus, int timeoutSeconds)
            throws Exception {
        invokeMtlsUntilStatus(context, keystorePath, new HashMap<>(), expectedStatus, timeoutSeconds);
    }

    /**
     * Invokes an mTLS API at the gateway with NO client certificate, retrying until the expected status. Reuses
     * the trust-all singleton client (which offers no key material) — the mandatory-mTLS API must reject it.
     */
    @When("I invoke the API at gateway context {string} with no client certificate until response status code becomes {int} within {int} seconds")
    public void iInvokeWithoutClientCert(String context, int expectedStatus, int timeoutSeconds) throws Exception {
        invokeMtlsUntilStatus(context, null, new HashMap<>(), expectedStatus, timeoutSeconds);
    }

    /**
     * Invokes an API at the HTTPS gateway presenting BOTH a client certificate and an OAuth2 bearer token. The
     * combination that distinguishes the mutual-SSL security modes and cannot be expressed by either single-axis
     * step: on a {@code mutualssl_mandatory} + application-security-mandatory API both credentials are required
     * (cert alone or token alone is refused), while on a mutualssl-OPTIONAL API the token alone suffices — so the
     * cert-plus-token positive is the only case that proves both legs are accepted together.
     *
     * @param keystorePath classpath JKS whose client certificate is presented on the TLS handshake
     * @param accessToken  context key holding the bearer token
     */
    @When("I invoke the API at gateway context {string} presenting client certificate {string} and access token {string} until response status code becomes {int} within {int} seconds")
    public void iInvokeWithClientCertAndToken(String context, String keystorePath, String accessToken,
                                              int expectedStatus, int timeoutSeconds) throws Exception {
        invokeMtlsUntilStatus(context, keystorePath, bearer(accessToken), expectedStatus, timeoutSeconds);
    }

    /**
     * Invokes an API at the HTTPS gateway with NO client certificate but WITH a bearer token. The strong form of
     * the mandatory-mTLS negative: a VALID OAuth2 token must not substitute for the missing handshake
     * certificate. (Sending no credential at all — the {@code with no client certificate} variant — cannot tell
     * "the cert is mandatory" apart from "some credential is required".)
     */
    @When("I invoke the API at gateway context {string} with no client certificate and access token {string} until response status code becomes {int} within {int} seconds")
    public void iInvokeWithoutClientCertWithToken(String context, String accessToken, int expectedStatus,
                                                  int timeoutSeconds) throws Exception {
        invokeMtlsUntilStatus(context, null, bearer(accessToken), expectedStatus, timeoutSeconds);
    }

    /**
     * Invokes an API at the HTTPS gateway with NO handshake certificate but with the accepted certificate's PEM
     * text base64url-encoded into the {@code X-WSO2-CLIENT-CERTIFICATE} header (alongside a bearer token). That
     * header is the one the gateway reads when a TLS-terminating load balancer forwards the client certificate,
     * so a client that can set it directly must NOT be able to pass mandatory mutual SSL with it — the header
     * must never substitute for the handshake certificate. Ports
     * APISecurityTestCase#testAPIInvocationWithMutualSSLHeader.
     *
     * @param certPath    classpath PEM certificate whose text is base64url-encoded into the header
     * @param accessToken context key holding the bearer token
     */
    @When("I invoke the API at gateway context {string} with no client certificate but certificate {string} in the X-WSO2-CLIENT-CERTIFICATE header and access token {string} until response status code becomes {int} within {int} seconds")
    public void iInvokeWithForwardedCertHeader(String context, String certPath, String accessToken,
                                               int expectedStatus, int timeoutSeconds) throws Exception {
        Map<String, String> headers = bearer(accessToken);
        // STANDARD base64, not base64url: the gateway constant is BASE64_ENCODED_CLIENT_CERTIFICATE_HEADER and
        // decodes accordingly. Encoding url-safe made the header UNREADABLE, so the expected 401 was satisfied by
        // a decode failure and proved nothing about the rule under test — that a forwarded-cert header must not
        // substitute for the handshake certificate. Readable cert => the 401 can only mean the rule was enforced.
        headers.put("X-WSO2-CLIENT-CERTIFICATE",
                java.util.Base64.getEncoder().encodeToString(
                        Utils.readClasspathResource(certPath).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        invokeMtlsUntilStatus(context, null, headers, expectedStatus, timeoutSeconds);
    }

    /**
     * THE retry-until-status envelope for every mutual-SSL invocation here, funnelled through
     * {@link Utils#retryUntil} so the deadline (floored at the shared propagation ceiling), the poll pacing and
     * the retry-only-on-{@code IOException} policy live in one place rather than in a loop per variant. A
     * {@code null} keystore means "offer no client key material" (the plain HTTPS client).
     *
     * <p>This family cannot go through {@code APIInvocationSteps.execute}: that funnel's chokepoint is the plain
     * {@code SimpleHTTPClient} verbs, and an mTLS call needs the keystore-bearing {@code doMutualSSLGet}
     * variant. It keeps the same {@code httpResponse} contract — cleared before each attempt so a throwing
     * attempt leaves NO stale response behind, and set only on a real one.
     */
    private void invokeMtlsUntilStatus(String context, String keystorePath, Map<String, String> extraHeaders,
                                       int expectedStatus, int timeoutSeconds) throws Exception {

        String endpointUrl = buildUrl(context);
        File keystore = keystorePath == null ? null : Utils.classpathToTempFile(keystorePath, "mtls", ".jks");
        Map<String, String> headers = acceptXml();
        headers.putAll(extraHeaders);

        HttpResponse last = Utils.retryUntil(timeoutSeconds * 1000L, () -> {
            TestContext.remove("httpResponse");
            HttpResponse response = keystore == null
                    ? SimpleHTTPClient.getInstance().doGet(endpointUrl, headers)
                    : SimpleHTTPClient.getInstance().doMutualSSLGet(keystore.getAbsolutePath(), KEYSTORE_PASSWORD,
                            endpointUrl, headers);
            TestContext.set("httpResponse", response);
            return response;
        }, response -> response.getResponseCode() == expectedStatus);

        assertNotNull(last, "No response received from the mutual-SSL gateway invocation within the timeout");
        assertEquals(last.getResponseCode(), expectedStatus,
                "Mutual-SSL invocation did not reach the expected status. Body: " + last.getData());
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

    /** Bearer headers for a token held under the given context key. */
    private Map<String, String> bearer(String accessTokenKey) {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                "Bearer " + TestContext.resolve(accessTokenKey));
        return headers;
    }
}
