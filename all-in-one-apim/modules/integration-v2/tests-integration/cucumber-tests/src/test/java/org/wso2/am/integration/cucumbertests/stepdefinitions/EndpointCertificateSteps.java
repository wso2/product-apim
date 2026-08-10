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
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.ResourceCleanup;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
        HttpResponse response = uploadCertificate(resourcePath, resolvedAlias,
                Utils.resolveContextPlaceholders(endpoint));
        Assert.assertEquals(response.getResponseCode(), 201, response.getData());
        ResourceCleanup.register(ResourceCleanup.CREATED_ENDPOINT_CERTIFICATE_ALIASES, resolvedAlias);
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
                response -> usageCount(response) == expectedCount);
        Assert.assertNotNull(last, "No endpoint-certificate usage response was captured within " + timeoutSeconds
                + "s — every attempt threw.");
        Assert.assertEquals(usageCount(last), expectedCount,
                "Endpoint-certificate usage did not list " + expectedCount + " APIs within " + timeoutSeconds
                        + "s; last response: " + last.getResponseCode() + " / " + last.getData());
    }

    /** The {@code count} of a usage/search response, or -1 when the response carried no usable 2xx body. */
    private static int usageCount(HttpResponse response) {
        if (response == null || response.getResponseCode() != 200
                || response.getData() == null || response.getData().isBlank()) {
            return -1;
        }
        return new JSONObject(response.getData()).optInt("count", -1);
    }

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
     * Asserts the exact validity window of the last content read. The product renders both bounds with
     * {@code java.util.Date#toString()} in the SERVER's default time zone, so the expected strings are the
     * container's rendering of the fixture's notBefore/notAfter — not the local machine's.
     */
    @Then("The endpoint certificate validity should be from {string} to {string}")
    public void theCertificateValidityShouldBe(String expectedFrom, String expectedTo) {
        JSONObject validity = lastResponseBody().getJSONObject("validity");
        Assert.assertEquals(validity.optString("from", null), expectedFrom,
                "Certificate validity 'from' mismatch; body: " + validity);
        Assert.assertEquals(validity.optString("to", null), expectedTo,
                "Certificate validity 'to' mismatch; body: " + validity);
    }

    /**
     * The last published response's body as JSON, guarded so a failed/empty response fails with the status and body
     * rather than an opaque JSONException from the parse (§7).
     */
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
