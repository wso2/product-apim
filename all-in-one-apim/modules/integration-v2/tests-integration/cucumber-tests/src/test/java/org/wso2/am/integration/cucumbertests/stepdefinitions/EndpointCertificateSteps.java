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

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.ContainerLogDiagnostics;
import org.wso2.am.integration.cucumbertests.utils.GracefulServerRestart;
import org.wso2.am.integration.cucumbertests.utils.HealGate;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.ResourceCleanup;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.am.testcontainers.ApimRuntime;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Step definitions for endpoint-certificate management (ports of APIEndpointCertificateTestCase management surface
 * and APIEndpointCertificateUsageTestCase). Exercises the Publisher {@code /endpoint-certificates} REST API:
 * multipart upload of a {@code .cer} against an endpoint URL, search by endpoint/alias, read a certificate's
 * information, delete, and the usage query with pagination.
 *
 * <p>These are the MANAGEMENT-plane steps. The runtime half of the legacy cert test — an API pointed at an HTTPS
 * backend the gateway does not trust, invoked 500 → certificate uploaded → 200 → certificate deleted → 500 — is a
 * {@code @cap:gateway} concern and lives in {@code features/gateway/endpoint_certificate_invocation.feature},
 * reusing the upload/delete steps here plus the invocation steps in {@link APIInvocationSteps}.
 *
 * <p>Uploads funnel through {@link Requests#postMultipart} (which publishes the response as {@code httpResponse}),
 * so the feature asserts the exact status (201 create / 409 duplicate-alias / 400 expired) itself. Certificates are
 * registered for failure-safe teardown and swept as their creating actor by {@link ResourceCleanup}.
 */
public class EndpointCertificateSteps {

    /**
     * Context-key prefixes remembering what an alias was uploaded FROM, so the trust gate below can re-fire the
     * upload without the feature restating the certificate path and endpoint it already gave the upload step.
     */
    private static final String CERT_SOURCE_PREFIX = "endpointCertSource::";
    private static final String CERT_ENDPOINT_PREFIX = "endpointCertEndpoint::";

    /** One initial propagation window followed by one bounded recovery window. */
    private static final int TRUST_GATE_ATTEMPTS = 2;
    /** One deletion-propagation window followed by one gateway-restart recovery window. */
    private static final int REMOVAL_GATE_ATTEMPTS = 2;
    private static final String GATEWAY_CERTIFICATE_ADDED_LOG =
            "The certificate with Alias '%s' is successfully added to the Gateway Trust Store.";
    private static final String GATEWAY_CERTIFICATE_REMOVED_LOG =
            "The certificate with Alias '%s' is successfully removed from the Gateway Trust Store.";
    private static final String SSL_PROFILE_RELOAD_LOG = "PassThroughHttpSender reloading SSL Config";
    private static final String CERT_REMOVAL_RELOAD_BASELINE_PREFIX = "endpointCertRemovalReloadBaseline::";
    private static final String CERT_REMOVAL_LOG_BASELINE_PREFIX = "endpointCertRemovalLogBaseline::";

    private Map<String, String> publisherAuthHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        return headers;
    }

    /**
     * Copies a classpath cert resource to a temp file (the multipart upload needs a {@link File}). The temp file is
     * deleted on JVM exit.
     */
    private File certFile(String resourcePath) throws IOException {
        String suffix = resourcePath.substring(resourcePath.lastIndexOf('.'));
        File temp;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("Certificate resource not found on classpath: " + resourcePath);
            }
            temp = File.createTempFile("endpoint-cert", suffix);
            temp.deleteOnExit();
            Files.copy(in, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return temp;
    }

    private HttpResponse uploadCertificate(String resourcePath, String alias, String endpoint) throws IOException {
        Map<String, File> files = new HashMap<>();
        files.put("certificate", certFile(resourcePath));
        Map<String, String> formFields = new HashMap<>();
        formFields.put("alias", alias);
        formFields.put("endpoint", endpoint);
        return Requests.postMultipart(Utils.getEndpointCertificatesURL(Utils.getBaseUrl()), publisherAuthHeaders(),
                files, formFields);
    }

    /**
     * Uploads an endpoint certificate (multipart: {@code certificate} file + {@code alias} + {@code endpoint}),
     * asserts 201, and registers the alias for teardown. The endpoint resolves {@code {{...}}} placeholders so a
     * scenario-unique endpoint URL flows through. Use this for the positive create; use the {@code attempt} variant
     * for the negatives (duplicate alias / expired cert).
     */
    @When("I upload endpoint certificate {string} with alias {string} for endpoint {string}")
    public void iUploadEndpointCertificate(String resourcePath, String alias, String endpoint) throws IOException {
        String resolvedAlias = Utils.resolveContextPlaceholders(alias);
        String resolvedEndpoint = Utils.resolveContextPlaceholders(endpoint);
        HttpResponse response = uploadCertificate(resourcePath, resolvedAlias, resolvedEndpoint);
        Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        ResourceCleanup.register(ResourceCleanup.CREATED_ENDPOINT_CERTIFICATE_ALIASES, resolvedAlias);
        // Remembered so the gateway-trust gate can re-fire this exact upload as its heal.
        TestContext.set(CERT_SOURCE_PREFIX + resolvedAlias, resourcePath);
        TestContext.set(CERT_ENDPOINT_PREFIX + resolvedAlias, resolvedEndpoint);
    }

    /**
     * Waits until the GATEWAY actually trusts an uploaded endpoint certificate, re-firing the upload if the
     * certificate-deploy event was dropped. Fixture readiness, never an assertion target — the scenario's own
     * {@code until 200} invoke remains the assertion (CLAUDE.md §15).
     *
     * <p><b>Why this gate has to exist.</b> Uploading a certificate is a two-hop operation: the control plane
     * writes it to the publisher trust store and emits an {@code ENDPOINT_CERTIFICATE_ADD} event, and the gateway
     * consumes that event and fetches the certificate over {@code /internal/data/v1/endpoint-certificates} before
     * adding it to its own trust store. That fetch needs an SSL context, which the product rebuilds from
     * {@code client-truststore.jks} on every call — the very file the control plane rewrites in place. When the
     * two overlap, the gateway reads a truncated keystore, falls back to a context with no trust manager, and
     * burns its whole retry chain on a client that can never work. The event is at-most-once and nothing re-fires
     * it, so the certificate never reaches the gateway and every invocation stays 500 for the rest of the run.
     * Measured twice in two consecutive CI runs (on the super-tenant row once and the tenant row once), with the
     * server log naming each step of the chain.
     *
     * <p><b>Why a longer deadline is not the fix.</b> The product stops trying after ~465s and never retries
     * again, so waiting longer only fails later. Re-firing is the only thing that recovers it, and it recovers
     * reliably because each event builds a FRESH client and SSL context.
     *
     * <p><b>Why the heal cannot mask a real defect.</b> The probe is the same invocation the scenario asserts on.
     * A certificate that is genuinely not honoured — wrong certificate, wrong endpoint, broken trust handling —
     * stays 500 through every re-upload and the gate fails with the observed state plus the server log evidence.
     * Each re-fire logs the shared grep-able {@code self-heal:} line, so how often the product race actually bites
     * stays countable in CI rather than being silently absorbed.
     *
     * @param alias    context-resolved alias of the certificate whose propagation is awaited
     * @param context  gateway context path to invoke, exactly as the following assertion step invokes it
     * <p>If the initial window expires, the single recovery is deliberately ordered as a Gateway process restart,
     * one delete/upload event re-trigger, and confirmation of the Gateway certificate-added and SSL-sender reload
     * log markers. Repeating delete/upload without resetting the Gateway is unsafe: it builds an event backlog while
     * the product's failed SSL client is still retrying the original event.
     *
     * @param tokenKey context key holding the access token to present
     */
    @Then("the endpoint certificate {string} should be trusted by the gateway at context {string} with access token {string}, restarting the gateway once and re-uploading if propagation is lost")
    public void certificateShouldBeTrustedByTheGateway(String alias, String context, String tokenKey)
            throws Exception {

        String resolvedAlias = Utils.resolveContextPlaceholders(alias);
        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String token = TestContext.resolve(tokenKey).toString();
        // The context already carries any /t/<tenant> prefix, so it is appended to the gateway base URL verbatim.
        String url = Utils.getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
        Map<String, String> auth = new HashMap<>();
        auth.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + token);
        String what = "gateway trust of endpoint certificate '" + resolvedAlias + "'";
        boolean[] recoveryStarted = {false};

        try {
            HealGate.awaitOrHeal(what,
                    () -> {
                        // Intermediate read (§7): the raw client, so the scenario's httpResponse is never clobbered
                        // by the gate and the following assertion still reads its OWN invocation.
                        HttpResponse r = SimpleHTTPClient.getInstance().doGet(url, auth);
                        if (r == null) {
                            return new HealGate.NotReady("no response from the gateway");
                        }
                        int code = r.getResponseCode();
                        if (code == 200) {
                            return new HealGate.Ready();
                        }
                        // 500 is the EXPECTED not-yet-trusted state: the gateway reached the backend and the TLS
                        // handshake failed. Anything else means the fixture, not propagation, is wrong — the API
                        // was already routable and returning 500 before this gate ran (the scenario's first leg
                        // asserts exactly that), so a 404 or an auth rejection can never resolve by waiting.
                        if (code == 500) {
                            return new HealGate.NotReady("HTTP 500 (backend TLS still untrusted): " + r.getData());
                        }
                        if (code == 404 && recoveryStarted[0]) {
                            return new HealGate.NotReady("gateway route is still settling after the recovery restart");
                        }
                        return new HealGate.Fatal("gateway returned HTTP " + code
                                + ", which is neither the trusted 200 nor the untrusted 500 this arc moves"
                                + " between — the API or the credential is wrong, not the certificate"
                                + " propagation: " + r.getData());
                    },
                    attempt -> {
                        recoveryStarted[0] = true;
                        return restartAndReUploadCertificate(resolvedAlias);
                    },
                    TRUST_GATE_ATTEMPTS);
        } catch (AssertionError gateFailed) {
            // The chain that causes this lives ONLY in the server log; without it the report is a bare
            // "expected 200 but found 500" that names neither the trust store nor the dropped event.
            throw new AssertionError(gateFailed.getMessage()
                    + ContainerLogDiagnostics.explainTrustStoreRace(what), gateFailed);
        }
    }

    /**
     * Re-fires {@code ENDPOINT_CERTIFICATE_ADD} for an alias by deleting and re-uploading it.
     *
     * <p>Delete-then-upload rather than upload-only because re-posting an existing alias is rejected with 409 and
     * would emit no event at all. The pair emits {@code REMOVE} then {@code ADD}; a JMS topic delivers to a given
     * subscriber in order, so the {@code ADD} is always the last of the two applied and the heal can never leave
     * the gateway less trusting than it found it.
     *
     * <p>Both calls use the raw client deliberately: a heal is not the step's response under test, and publishing
     * {@code httpResponse} here would leave the following assertion reading the gate's own traffic (§7).
     *
     * <p><b>A failed re-trigger is NOT fatal, and that is load-bearing.</b> Deleting or uploading a certificate
     * makes the control plane rewrite the very trust store whose racy read this gate exists to survive, so the
     * heal's own management calls can be hit by the SAME race and answer 500. Measured: a mutation run aborted at
     * attempt 3 of 10 because the DELETE returned 500, with {@code Unable to read the trust store file: null} in
     * the server log at that instant. Treating that as fatal would make the gate give up exactly when the product
     * is misbehaving in the way it is meant to absorb. A transient refusal therefore reports {@code NotReady} —
     * HealGate counts the round and moves on, so the next window simply tries again. Only conditions that can
     * never resolve ({@code 401}/{@code 403}, or an alias this class never uploaded) are {@code Fatal}.
     */
    private HealGate.Verdict restartAndReUploadCertificate(String alias) throws Exception {
        // Reset the Gateway's in-memory SSL client and its exhausted endpoint-certificate retry chain before
        // emitting a fresh event. In distributed topology this targets only the Gateway; in all-in-one it is the
        // unified APIM node, both through the topology-provided management URL.
        GracefulServerRestart.gateway();
        int reloadCountBeforeRetrigger = countOccurrences(readGatewayLog(), SSL_PROFILE_RELOAD_LOG);
        HealGate.Verdict retrigger = reUploadCertificate(alias);
        if (retrigger instanceof HealGate.Fatal || retrigger instanceof HealGate.NotReady) {
            return retrigger;
        }
        return awaitGatewayCertificateReload(alias, reloadCountBeforeRetrigger);
    }

    private HealGate.Verdict reUploadCertificate(String alias) {
        Object source = TestContext.get(CERT_SOURCE_PREFIX + alias);
        Object endpoint = TestContext.get(CERT_ENDPOINT_PREFIX + alias);
        if (source == null || endpoint == null) {
            return new HealGate.Fatal("cannot re-upload certificate '" + alias + "': it was not uploaded through"
                    + " the upload step, so its source file and endpoint are unknown");
        }
        try {
            HttpResponse deleted = SimpleHTTPClient.getInstance().doDelete(
                    Utils.getEndpointCertificateByAliasURL(Utils.getBaseUrl(), alias), publisherAuthHeaders());
            int deleteCode = deleted == null ? -1 : deleted.getResponseCode();
            // 404 is fine: the alias already being absent is a valid starting point for the re-upload.
            if (deleteCode != 200 && deleteCode != 404) {
                return classifyHealFailure("delete of certificate '" + alias + "'", deleteCode, deleted);
            }
            Map<String, File> files = new HashMap<>();
            files.put("certificate", certFile(source.toString()));
            Map<String, String> formFields = new HashMap<>();
            formFields.put("alias", alias);
            formFields.put("endpoint", endpoint.toString());
            HttpResponse added = SimpleHTTPClient.getInstance().doPostMultipartWithFiles(
                    Utils.getEndpointCertificatesURL(Utils.getBaseUrl()), publisherAuthHeaders(), files, formFields);
            int addCode = added == null ? -1 : added.getResponseCode();
            if (addCode != 201) {
                return classifyHealFailure("re-upload of certificate '" + alias + "'", addCode, added);
            }
            // The alias is registered for teardown by the original upload and is re-created under the same name,
            // so the registration still names a live resource and must not be touched here.
            return new HealGate.Ready();
        } catch (IOException transientDuringHeal) {
            // Connectivity, not configuration — the next window retries.
            return new HealGate.NotReady("re-trigger of '" + alias + "' could not reach the management API: "
                    + transientDuringHeal);
        } catch (Exception reUploadFailed) {
            return new HealGate.Fatal("re-upload of certificate '" + alias + "' threw: " + reUploadFailed);
        }
    }

    /**
     * Waits for product evidence that the fresh event reached the Gateway and that the outbound SSL sender rebuilt
     * its profile. The functional gateway probe remains the gate's pass condition; these markers prevent the retry
     * window from racing ahead of the product's own reload path and make a future failure actionable.
     */
    private HealGate.Verdict awaitGatewayCertificateReload(String alias, int reloadCountBeforeRetrigger)
            throws InterruptedException {
        String certificateAddedMarker = String.format(GATEWAY_CERTIFICATE_ADDED_LOG, alias);
        long start = System.currentTimeMillis();
        long deadline = start + Constants.RUNTIME_PROPAGATION_TIMEOUT;
        String lastObserved = "no Gateway certificate/reload log markers observed";
        while (System.currentTimeMillis() < deadline) {
            String gatewayLog = readGatewayLog();
            if (gatewayLog != null) {
                boolean certificateAdded = gatewayLog.contains(certificateAddedMarker);
                boolean senderReloaded = countOccurrences(gatewayLog, SSL_PROFILE_RELOAD_LOG)
                        > reloadCountBeforeRetrigger;
                if (certificateAdded && senderReloaded) {
                    return new HealGate.Ready();
                }
                lastObserved = "certificate-added=" + certificateAdded + ", sender-reloaded=" + senderReloaded;
            }
            Utils.pollPause(start, Constants.RETRY_INTERVAL_TIME);
        }
        return new HealGate.NotReady("Gateway reload markers did not converge after recovery: " + lastObserved);
    }

    /**
     * Waits for the gateway to stop trusting a certificate after its control-plane record has been deleted.
     *
     * <p>The ordinary delete step verifies only the management-plane response. In distributed topology the gateway
     * removal event can be lost, and in either topology the gateway can retain the certificate in its in-memory SSL
     * profile indefinitely. This gate keeps the runtime assertion strict: HTTP 500 is accepted only after the
     * gateway has emitted a fresh outbound-SSL reload marker after the DELETE, or after the one permitted gateway
     * restart has completed and emitted a fresh reload marker. The removal text marker is retained as diagnostic
     * evidence when the topology emits it, but it is not a distributed contract because the management-side removal
     * log may be written by the control plane rather than the gateway. HTTP 200 remains not-ready, so waiting cannot
     * turn a still-trusted certificate into a pass.
     *
     * <p>The following scenario steps still invoke the API until 500 and verify that 500 remains stable. This is a
     * bounded fixture-convergence repair, not a replacement or relaxation of the product assertion.
     */
    @Then("the endpoint certificate {string} should no longer be trusted by the gateway at context {string} with access token {string}, restarting the gateway once if removal propagation is lost")
    public void certificateShouldNoLongerBeTrustedByTheGateway(String alias, String context, String tokenKey)
            throws Exception {

        String resolvedAlias = Utils.resolveContextPlaceholders(alias);
        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String token = TestContext.resolve(tokenKey).toString();
        String url = Utils.getBaseGatewayUrl() + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
        Map<String, String> auth = new HashMap<>();
        auth.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + token);
        String what = "gateway removal of endpoint certificate '" + resolvedAlias + "'";
        boolean[] recoveryStarted = {false};

        Object reloadBaseline = TestContext.get(CERT_REMOVAL_RELOAD_BASELINE_PREFIX + resolvedAlias);
        Object removalLogBaseline = TestContext.get(CERT_REMOVAL_LOG_BASELINE_PREFIX + resolvedAlias);
        int[] reloadCountBaseline = {reloadBaseline instanceof Integer ? (Integer) reloadBaseline : -1};
        int removalLogCountBeforeDelete = removalLogBaseline instanceof Integer
                ? (Integer) removalLogBaseline : -1;

        try {
            HealGate.awaitOrHeal(what,
                    () -> {
                        String gatewayLog = readGatewayLog();
                        int reloadCount = countOccurrences(gatewayLog, SSL_PROFILE_RELOAD_LOG);
                        String removalMarker = String.format(GATEWAY_CERTIFICATE_REMOVED_LOG, resolvedAlias);
                        int removalLogCount = countOccurrences(gatewayLog, removalMarker);
                        HttpResponse response = SimpleHTTPClient.getInstance().doGet(url, auth);
                        if (response == null) {
                            return new HealGate.NotReady("no response from the gateway");
                        }
                        int code = response.getResponseCode();
                        if (code == 500) {
                            boolean removalObserved = removalLogCountBeforeDelete >= 0
                                    && removalLogCount > removalLogCountBeforeDelete;
                            boolean senderReloaded = reloadCountBaseline[0] >= 0
                                    && reloadCount > reloadCountBaseline[0];
                            if (senderReloaded) {
                                return new HealGate.Ready();
                            }
                            return new HealGate.NotReady("HTTP 500 reached, but gateway removal/reload has not"
                                    + " converged yet (removal-marker=" + removalObserved
                                    + ", sender-reload=" + senderReloaded + ")");
                        }
                        if (code == 200) {
                            return new HealGate.NotReady("HTTP 200 — gateway still trusts the deleted certificate");
                        }
                        if (code == 404 && recoveryStarted[0]) {
                            return new HealGate.NotReady("gateway route is still settling after the recovery restart");
                        }
                        return new HealGate.Fatal("gateway returned HTTP " + code
                                + ", which is neither the expected untrusted 500 nor the still-trusted 200: "
                                + response.getData());
                    },
                    attempt -> {
                        recoveryStarted[0] = true;
                        String gatewayLog = readGatewayLog();
                        reloadCountBaseline[0] = countOccurrences(gatewayLog, SSL_PROFILE_RELOAD_LOG);
                        TestContext.set(CERT_REMOVAL_RELOAD_BASELINE_PREFIX + resolvedAlias, reloadCountBaseline[0]);
                        GracefulServerRestart.gateway();
                        return new HealGate.NotReady("gateway restarted; waiting for its fresh SSL sender reload");
                    },
                    REMOVAL_GATE_ATTEMPTS);
        } catch (AssertionError gateFailed) {
            throw new AssertionError(gateFailed.getMessage()
                    + ContainerLogDiagnostics.explainTrustStoreRace(what), gateFailed);
        }
    }

    private String readGatewayLog() {
        Object candidate = TestContext.get("blockApimContainer");
        if (!(candidate instanceof ApimRuntime runtime)) {
            return null;
        }
        try {
            return runtime.readGatewayLogFile(ContainerLogDiagnostics.SERVER_LOG);
        } catch (RuntimeException logUnavailable) {
            return null;
        }
    }

    private static int countOccurrences(String value, String marker) {
        if (value == null || marker == null || marker.isEmpty()) {
            return 0;
        }
        int count = 0;
        int from = 0;
        while ((from = value.indexOf(marker, from)) >= 0) {
            count++;
            from += marker.length();
        }
        return count;
    }

    /**
     * Decides whether a refused heal call can ever succeed. An auth rejection cannot, so it fails the gate
     * immediately with a clear cause; anything else (notably a {@code 5xx} from the trust-store race) is reported
     * as not-ready so the gate keeps its remaining attempts.
     */
    private static HealGate.Verdict classifyHealFailure(String what, int code, HttpResponse response) {
        String body = response == null ? "null" : response.getData();
        if (code == 401 || code == 403) {
            return new HealGate.Fatal(what + " was refused with HTTP " + code
                    + " — the acting actor cannot manage this certificate, which re-trying cannot fix: " + body);
        }
        return new HealGate.NotReady(what + " did not take effect this round: HTTP " + code + " / " + body);
    }

    /**
     * Attempts to upload an endpoint certificate WITHOUT asserting success — for the negatives (re-upload of an
     * existing alias → 409, expired cert → 400). Neither asserts a status nor registers an alias; the feature
     * asserts the resulting status/body. Publishes the response as {@code httpResponse}.
     */
    @When("I attempt to upload endpoint certificate {string} with alias {string} for endpoint {string}")
    public void iAttemptToUploadEndpointCertificate(String resourcePath, String alias, String endpoint)
            throws IOException {
        uploadCertificate(resourcePath, Utils.resolveContextPlaceholders(alias),
                Utils.resolveContextPlaceholders(endpoint));
    }

    /** Searches endpoint certificates by endpoint URL (publishes the response for assertion). */
    @When("I search endpoint certificates by endpoint {string}")
    public void iSearchEndpointCertificatesByEndpoint(String endpoint) throws IOException {
        Requests.get(Utils.getEndpointCertificatesSearchURL(Utils.getBaseUrl(),
                Utils.resolveContextPlaceholders(endpoint), null), publisherAuthHeaders());
    }

    /** Searches endpoint certificates by alias (publishes the response for assertion). */
    @When("I search endpoint certificates by alias {string}")
    public void iSearchEndpointCertificatesByAlias(String alias) throws IOException {
        Requests.get(Utils.getEndpointCertificatesSearchURL(Utils.getBaseUrl(), null,
                Utils.resolveContextPlaceholders(alias)), publisherAuthHeaders());
    }

    /**
     * Deletes an endpoint certificate by alias (publishes the response for assertion). On a successful delete the
     * alias is DEREGISTERED from the teardown list: the sweep would otherwise re-delete it, get a 404 and log the
     * "assumed already deleted" line — which is exactly the shape a real leak takes, so leaving it there would
     * train reviewers to ignore the one signal that matters. After this, a 404 in the sweep means something the
     * scenario did NOT delete is missing.
     */
    @When("I delete the endpoint certificate with alias {string}")
    public void iDeleteEndpointCertificate(String alias) throws IOException {
        String resolvedAlias = Utils.resolveContextPlaceholders(alias);
        String gatewayLogBeforeDelete = readGatewayLog();
        TestContext.set(CERT_REMOVAL_RELOAD_BASELINE_PREFIX + resolvedAlias,
                countOccurrences(gatewayLogBeforeDelete, SSL_PROFILE_RELOAD_LOG));
        TestContext.set(CERT_REMOVAL_LOG_BASELINE_PREFIX + resolvedAlias,
                countOccurrences(gatewayLogBeforeDelete,
                        String.format(GATEWAY_CERTIFICATE_REMOVED_LOG, resolvedAlias)));
        HttpResponse response = Requests.delete(Utils.getEndpointCertificateByAliasURL(Utils.getBaseUrl(),
                resolvedAlias), publisherAuthHeaders());
        if (response != null && response.getResponseCode() == 200) {
            ResourceCleanup.deregister(ResourceCleanup.CREATED_ENDPOINT_CERTIFICATE_ALIASES, resolvedAlias);
        }
    }

    /**
     * Reads the CONTENT (certificate information) of an uploaded endpoint certificate:
     * {@code GET /endpoint-certificates/{alias}} → CertificateInfoDTO. Publishes the response so the feature
     * asserts the exact status/subject/version/validity itself. Ports the
     * {@code getendpointCertificateContent} half of testSearchEndpointCertificates — note that legacy method name
     * is misleading: it calls the by-alias INFORMATION resource, not {@code /content}.
     *
     * <p>The product answers this by reading the certificate back out of the GATEWAY TRUST STORE (see
     * {@code CertificateMgtUtils#getCertificateInformation}), so a 200 here is also evidence the upload really
     * landed in the trust store and not merely in the metadata table.
     */
    @When("I retrieve the content of endpoint certificate {string}")
    public void iRetrieveEndpointCertificateContent(String alias) throws IOException {
        Requests.get(Utils.getEndpointCertificateByAliasURL(Utils.getBaseUrl(),
                Utils.resolveContextPlaceholders(alias)), publisherAuthHeaders());
    }

    /**
     * Queries the usage of an endpoint certificate (the APIs whose endpoint uses it) with an explicit limit/offset,
     * publishing the response for assertion. Ports {@code getCertificateUsage(alias, limit, offset)}.
     */
    @When("I retrieve the usage of endpoint certificate {string} with limit {int} and offset {int}")
    public void iRetrieveEndpointCertificateUsage(String alias, int limit, int offset) throws IOException {
        Requests.get(Utils.getEndpointCertificateUsageURL(Utils.getBaseUrl(),
                Utils.resolveContextPlaceholders(alias), limit, offset), publisherAuthHeaders());
    }

    /**
     * Polls the usage query until it lists {@code expectedCount} APIs (index-readiness gate). Certificate usage is
     * computed from an eventually-consistent index — a freshly-uploaded cert / freshly-created APIs are not matched
     * immediately (the legacy slept 5s), so this retries. Once it settles, the following single-shot pagination
     * queries are consistent. Publishes the last response and asserts the count after the loop.
     */
    @When("I retrieve the usage of endpoint certificate {string} with limit {int} and offset {int} until it lists {int} APIs within {int} seconds")
    public void iRetrieveUsageUntilCount(String alias, int limit, int offset, int expectedCount, int timeoutSeconds)
            throws InterruptedException {
        String url = Utils.getEndpointCertificateUsageURL(Utils.getBaseUrl(), Utils.resolveContextPlaceholders(alias),
                limit, offset);
        HttpResponse last = Utils.retryUntil(timeoutSeconds * 1000L,
                () -> Requests.get(url, publisherAuthHeaders()),
                response -> Utils.listCountOf(response) == expectedCount);
        Assert.assertNotNull(last, "No endpoint-certificate usage response was captured within " + timeoutSeconds
                + "s — every attempt threw.");
        Assert.assertEquals(Utils.listCountOf(last), expectedCount,
                "Endpoint-certificate usage did not list " + expectedCount + " APIs within " + timeoutSeconds
                        + "s; last response: " + last.getResponseCode() + " / " + last.getData());
    }

    /** The {@code count} of a usage/search response, or -1 when the response carried no usable 2xx body. */

    /**
     * Asserts the number of certificates in the last search response ({@code count} field of the CertificatesDTO).
     */
    @Then("The endpoint certificate search should return {int} certificates")
    public void theSearchShouldReturnNCertificates(int expected) {
        JSONObject body = lastResponseBody();
        Assert.assertEquals(body.optInt("count", -1), expected,
                "Endpoint-certificate search count mismatch; body: " + body);
    }

    /**
     * Asserts the number of APIs in the last certificate-usage response ({@code count} field of the
     * APIMetadataListDTO).
     */
    @Then("The endpoint certificate usage should list {int} APIs")
    public void theUsageShouldListNApis(int expected) {
        JSONObject body = lastResponseBody();
        Assert.assertEquals(body.optInt("count", -1), expected,
                "Endpoint-certificate usage API count mismatch; body: " + body);
    }

    /**
     * Asserts the usage response lists EXACTLY the given API ids — not merely the right NUMBER of them. A
     * count-only assertion (the step above) passes just as happily when the product returns three unrelated APIs,
     * so the identity check is what actually pins "these are the APIs bound to this certificate's endpoint".
     *
     * @param expectedIdsKey context key holding the comma-separated ids of the APIs bound to the endpoint (set by
     *                       the bulk endpoint-API create step)
     */
    @Then("The endpoint certificate usage should list exactly the APIs in {string}")
    public void theUsageShouldListExactlyTheApis(String expectedIdsKey) {
        Set<String> expected = new HashSet<>(Arrays.asList(
                TestContext.resolve(expectedIdsKey).toString().split("\\s*,\\s*")));
        JSONArray list = lastResponseBody().getJSONArray("list");
        // Cardinality before the set compare (§12): a Set collapses duplicates, so a list carrying an entry
        // twice would still equal the expected set.
        Assert.assertEquals(list.length(), expected.size(),
                "Endpoint-certificate usage returned " + list.length() + " entries but expected "
                        + expected.size() + "; list: " + list);
        Set<String> actual = new HashSet<>();
        for (int i = 0; i < list.length(); i++) {
            actual.add(list.getJSONObject(i).getString("id"));
        }
        Assert.assertEquals(actual, expected, "Endpoint-certificate usage listed the WRONG APIs — expected exactly "
                + expected + " but got " + actual);
    }

    /**
     * Asserts the exact certificate information the last content read returned. Legacy pinned these four fields per
     * alias; they come straight off the X509 certificate the gateway trust store holds
     * ({@code CertificateMgtUtils#getCertificateMetaData}), so they also prove the trust store holds OUR fixture
     * and not some other certificate that happens to share the alias.
     *
     * <p>{@code subject} is {@code X509Certificate#getSubjectDN().toString()}, which renders the RDNs in REVERSE
     * order of the PEM (so a {@code C=LK,…,CN=nodebackend} certificate reads {@code CN=nodebackend, …, C=LK}).
     */
    @Then("The endpoint certificate content should have status {string}, subject {string} and version {string}")
    public void theCertificateContentShouldHave(String expectedStatus, String expectedSubject,
                                                String expectedVersion) {
        JSONObject info = lastResponseBody();
        Assert.assertEquals(info.optString("status", null), expectedStatus,
                "Certificate status mismatch; body: " + info);
        Assert.assertEquals(info.optString("subject", null), expectedSubject,
                "Certificate subject DN mismatch; body: " + info);
        Assert.assertEquals(info.optString("version", null), expectedVersion,
                "Certificate version mismatch; body: " + info);
    }

    /**
     * Asserts the exact validity window of the last content read — as INSTANTS, not as rendered text.
     *
     * <p>The product emits both bounds as {@code java.util.Date#toString()} in the SERVER's default time zone
     * ({@code validity.from}/{@code to} are untyped strings in the publisher OAS, so there is no epoch or
     * ISO-8601 field to read instead). Comparing that text verbatim would pin the CONTAINER's zone: the same
     * certificate renders {@code "Fri May 06 18:11:14 UTC 2022"} on a UTC container and
     * {@code "Fri May 06 23:41:14 IST 2022"} on an IST one, and the assertion would fail on the latter for no
     * product reason. Parsing BOTH sides with the same formatter and comparing epoch millis keeps the assertion
     * exact while making it independent of the zone either side was rendered in.
     */
    @Then("The endpoint certificate validity should be from {string} to {string}")
    public void theCertificateValidityShouldBe(String expectedFrom, String expectedTo) {
        JSONObject validity = lastResponseBody().getJSONObject("validity");
        assertSameInstant("from", validity.optString("from", null), expectedFrom, validity);
        assertSameInstant("to", validity.optString("to", null), expectedTo, validity);
    }

    /** Parses both sides as {@code Date#toString()} text and asserts they name the same instant. */
    private static void assertSameInstant(String bound, String actual, String expected, JSONObject validity) {

        Date actualDate = parseDateToString(actual, "certificate validity '" + bound + "' from the server");
        Date expectedDate = parseDateToString(expected, "expected certificate validity '" + bound + "'");
        Assert.assertEquals(actualDate.getTime(), expectedDate.getTime(),
                "Certificate validity '" + bound + "' mismatch: server returned '" + actual + "' which is "
                        + actualDate.toInstant() + ", expected '" + expected + "' which is "
                        + expectedDate.toInstant() + "; body: " + validity);
    }

    /**
     * {@code Date#toString()} text -> Date. Locale.US is pinned because that method always renders English day
     * and month names regardless of the default locale, so a non-English default must not change the parse.
     */
    private static Date parseDateToString(String text, String what) {

        Assert.assertTrue(text != null && !text.isBlank(), "No " + what + " to compare (got: " + text + ")");
        SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
        try {
            return format.parse(text);
        } catch (ParseException notDateToString) {
            throw new AssertionError("Could not parse " + what + " as a java.util.Date#toString() value: '"
                    + text + "'", notDateToString);
        }
    }

    /**
     * The last published response as JSON, guarded. Delegates to {@link Utils#requireJsonBody} rather than
     * repeating the 2xx-with-a-body check: that guard is the shared one every plane uses (§15), and a second
     * copy here would drift from it.
     */
    private static JSONObject lastResponseBody() {
        return Utils.requireJsonBody((HttpResponse) TestContext.get("httpResponse"),
                "Endpoint-certificate request");
    }
}
