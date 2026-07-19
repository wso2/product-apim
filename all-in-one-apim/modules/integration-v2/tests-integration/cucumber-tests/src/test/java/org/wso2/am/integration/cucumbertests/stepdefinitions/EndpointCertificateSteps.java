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
import java.util.HashMap;
import java.util.Map;

/**
 * Step definitions for endpoint-certificate management (ports of APIEndpointCertificateTestCase management surface
 * and APIEndpointCertificateUsageTestCase). Exercises the Publisher {@code /endpoint-certificates} REST API:
 * multipart upload of a {@code .cer} against an endpoint URL, search by endpoint/alias, delete, and the usage query
 * with pagination. The invocation half of the legacy cert test (a WireMock HTTPS backend + SSL-profile-reload
 * polling) is intentionally NOT ported — it needs custom TLS backend infra; the management/usage REST behaviour is
 * the portable, high-value subject.
 *
 * <p>Uploads funnel through {@link Requests#postMultipart} (which publishes the response as {@code httpResponse}),
 * so the feature asserts the exact status (201 create / 409 duplicate-alias / 400 expired) itself. Certificates are
 * registered for failure-safe teardown and swept as their creating actor by {@link ResourceCleanup}.
 */
public class EndpointCertificateSteps {

    private final BaseSteps baseSteps = new BaseSteps();

    private String getBaseUrl() {
        return baseSteps.getBaseUrl();
    }

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
        return Requests.postMultipart(Utils.getEndpointCertificatesURL(getBaseUrl()), publisherAuthHeaders(),
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
        Requests.get(Utils.getEndpointCertificatesSearchURL(getBaseUrl(),
                Utils.resolveContextPlaceholders(endpoint), null), publisherAuthHeaders());
    }

    /** Searches endpoint certificates by alias (publishes the response for assertion). */
    @When("I search endpoint certificates by alias {string}")
    public void iSearchEndpointCertificatesByAlias(String alias) throws IOException {
        Requests.get(Utils.getEndpointCertificatesSearchURL(getBaseUrl(), null,
                Utils.resolveContextPlaceholders(alias)), publisherAuthHeaders());
    }

    /** Deletes an endpoint certificate by alias (publishes the response for assertion). */
    @When("I delete the endpoint certificate with alias {string}")
    public void iDeleteEndpointCertificate(String alias) throws IOException {
        Requests.delete(Utils.getEndpointCertificateByAliasURL(getBaseUrl(),
                Utils.resolveContextPlaceholders(alias)), publisherAuthHeaders());
    }

    /**
     * Queries the usage of an endpoint certificate (the APIs whose endpoint uses it) with an explicit limit/offset,
     * publishing the response for assertion. Ports {@code getCertificateUsage(alias, limit, offset)}.
     */
    @When("I retrieve the usage of endpoint certificate {string} with limit {int} and offset {int}")
    public void iRetrieveEndpointCertificateUsage(String alias, int limit, int offset) throws IOException {
        Requests.get(Utils.getEndpointCertificateUsageURL(getBaseUrl(),
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
            throws IOException, InterruptedException {
        String url = Utils.getEndpointCertificateUsageURL(getBaseUrl(), Utils.resolveContextPlaceholders(alias),
                limit, offset);
        long endTime = System.currentTimeMillis() + timeoutSeconds * 1000L;
        HttpResponse response;
        int actual = -1;
        while (true) {
            response = Requests.get(url, publisherAuthHeaders());
            if (response != null && response.getResponseCode() == 200
                    && response.getData() != null && !response.getData().isEmpty()) {
                actual = new JSONObject(response.getData()).optInt("count", -1);
            }
            if (actual == expectedCount || System.currentTimeMillis() >= endTime) {
                break;
            }
            Thread.sleep(2000);
        }
        Assert.assertEquals(actual, expectedCount,
                "Endpoint-certificate usage did not list " + expectedCount + " APIs within " + timeoutSeconds
                        + "s; last count=" + actual);
    }

    /**
     * Asserts the number of certificates in the last search response ({@code count} field of the CertificatesDTO).
     */
    @Then("The endpoint certificate search should return {int} certificates")
    public void theSearchShouldReturnNCertificates(int expected) {
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertNotNull(response, "No endpoint-certificate search response captured");
        Assert.assertTrue(response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isEmpty(),
                "Endpoint-certificate search did not return a 2xx body: got " + response.getResponseCode()
                        + " / " + response.getData());
        int actual = new JSONObject(response.getData()).optInt("count", -1);
        Assert.assertEquals(actual, expected,
                "Endpoint-certificate search count mismatch; body: " + response.getData());
    }

    /**
     * Asserts the number of APIs in the last certificate-usage response ({@code count} field of the
     * APIMetadataListDTO).
     */
    @Then("The endpoint certificate usage should list {int} APIs")
    public void theUsageShouldListNApis(int expected) {
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertNotNull(response, "No endpoint-certificate usage response captured");
        Assert.assertTrue(response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isEmpty(),
                "Endpoint-certificate usage did not return a 2xx body: got " + response.getResponseCode()
                        + " / " + response.getData());
        int actual = new JSONObject(response.getData()).optInt("count", -1);
        Assert.assertEquals(actual, expected,
                "Endpoint-certificate usage API count mismatch; body: " + response.getData());
    }
}
