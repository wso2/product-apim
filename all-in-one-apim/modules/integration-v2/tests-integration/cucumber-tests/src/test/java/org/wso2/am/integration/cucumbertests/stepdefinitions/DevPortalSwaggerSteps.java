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
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DevPortal Swagger {@code servers} URL glue (ports the swagger-server-URL half of EnvironmentTestCase).
 *
 * <p>The DevPortal fetches an API's definition with {@code GET /apis/{apiId}/swagger} and NO
 * {@code environmentName} query parameter, leaving the server to resolve the gateway environment itself and
 * stamp the resulting URL into the definition's {@code servers} section — the URL the try-out console then calls.
 * That resolution must consider only environments the API is ACTUALLY deployed to: picking any other one yields a
 * host-less URL such as {@code http:///pizzashack/1.0.0}.</p>
 *
 * <p>Both steps poll: a deploy/undeploy propagates to the DevPortal asynchronously, so the newly resolved host
 * appears only after a moment. Neither step relies on a following assertion — each fails on its own once its
 * deadline passes, reporting the raw {@code servers} array so a malformed URL is visible in the failure.</p>
 */
public class DevPortalSwaggerSteps {

    private String getBaseUrl() {
        return TestContext.get("baseUrl").toString();
    }

    /**
     * Asserts the API's DevPortal Swagger resolves its server URL to EXACTLY {@code expectedHost} — the vhost of
     * the single gateway environment the API is deployed to.
     *
     * @param apiIdKey       context key holding the API id
     * @param expectedHost   the host the resolved {@code servers[0].url} must carry
     * @param timeoutSeconds how long to keep polling for the DevPortal to reflect the deployment
     */
    @Then("the devportal swagger of API {string} should resolve its server host to {string} within {int} seconds")
    public void swaggerServerHostShouldBe(String apiIdKey, String expectedHost, int timeoutSeconds)
            throws InterruptedException {
        assertResolvedHost(apiIdKey, Arrays.asList(expectedHost), timeoutSeconds,
                "exactly \"" + expectedHost + "\"");
    }

    /**
     * Asserts the API's DevPortal Swagger resolves its server URL to one of {@code csvHosts} — used when the API is
     * deployed to SEVERAL gateway environments, where the product may legitimately resolve to any of them. The
     * assertion is still strict about the thing that broke: the host must be a real vhost of an environment the API
     * is deployed to, never blank and never one it is not deployed to.
     *
     * @param apiIdKey       context key holding the API id
     * @param csvHosts       comma-separated vhosts of the environments the API is deployed to
     * @param timeoutSeconds how long to keep polling for the DevPortal to reflect the deployment
     */
    @Then("the devportal swagger of API {string} should resolve its server host to one of {string} within {int} seconds")
    public void swaggerServerHostShouldBeOneOf(String apiIdKey, String csvHosts, int timeoutSeconds)
            throws InterruptedException {
        List<String> hosts = Arrays.stream(csvHosts.split(",")).map(String::trim).collect(Collectors.toList());
        assertResolvedHost(apiIdKey, hosts, timeoutSeconds, "one of " + hosts);
    }

    /**
     * Polls the DevPortal swagger until {@code servers[0].url}'s host is one of {@code acceptableHosts}, then
     * returns; fails after the deadline with the last raw {@code servers} value.
     */
    private void assertResolvedHost(String apiIdKey, List<String> acceptableHosts, int timeoutSeconds,
                                    String expectation) throws InterruptedException {
        String apiId = TestContext.resolve(apiIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());

        // retryUntil owns the deadline (floored at RUNTIME_PROPAGATION_TIMEOUT), the tiered pacing and the
        // IOException-only retry (a bad api id / context key still fails fast in resolve above); the accept
        // condition parses the swagger and holds once servers[0].url's host is one we deployed to. It returns
        // the last response, which we publish and (on non-match) re-parse for the failure message.
        HttpResponse last = Utils.retryUntil(timeoutSeconds * 1000L,
                () -> SimpleHTTPClient.getInstance()
                        .doGet(Utils.getDevportalApiSwaggerURL(getBaseUrl(), apiId), headers),
                resp -> resolvedHostOf(resp, acceptableHosts) != null);
        TestContext.set("httpResponse", last);
        String host = resolvedHostOf(last, acceptableHosts);
        if (host != null) {
            return;
        }
        Assert.fail("DevPortal swagger of API " + apiId + " did not resolve its server host to " + expectation
                + " within " + timeoutSeconds + "s. Last servers=" + serversOf(last)
                + ". A blank host means the environment was resolved to one the API is not deployed to.");
    }

    /**
     * The host of {@code servers[0].url} in the swagger response IF it is one of {@code acceptableHosts}, else
     * {@code null} (a non-200/empty body, a missing/empty {@code servers} section — counted as not-yet-propagated
     * rather than a hard failure — or a host that is not one we deployed to). Never throws on a malformed body.
     */
    private static String resolvedHostOf(HttpResponse resp, List<String> acceptableHosts) {
        if (resp == null || resp.getResponseCode() != 200 || resp.getData() == null || resp.getData().isBlank()) {
            return null;
        }
        JSONArray servers = new JSONObject(resp.getData()).optJSONArray("servers");
        if (servers == null || servers.length() == 0) {
            return null;
        }
        String host = URI.create(servers.getJSONObject(0).optString("url")).getHost();
        return host != null && acceptableHosts.contains(host) ? host : null;
    }

    /** The raw {@code servers} array of the swagger response for the failure message, or {@code null}. */
    private static String serversOf(HttpResponse resp) {
        if (resp == null || resp.getResponseCode() != 200 || resp.getData() == null || resp.getData().isBlank()) {
            return null;
        }
        JSONArray servers = new JSONObject(resp.getData()).optJSONArray("servers");
        return servers == null ? null : servers.toString();
    }
}
