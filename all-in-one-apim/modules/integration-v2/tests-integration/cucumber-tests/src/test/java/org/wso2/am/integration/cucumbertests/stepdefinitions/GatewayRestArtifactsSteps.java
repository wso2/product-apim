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
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Step definitions for the gateway internal REST API artifact surface (port of GatewayRestAPITestCase). Retrieves a
 * deployed API's synapse artifacts — the API artifact, its endpoints, its local entry and its mediation sequences —
 * from {@code api/am/gateway/v2/{api-artifact|end-points|local-entry|sequence}?apiName=&version=&tenantDomain=}.
 *
 * <p>Pinned live: this gateway REST API authenticates with BASIC admin credentials (a Bearer token is rejected
 * 401), so these steps send the acting actor's tenant-admin Basic auth (not the publisher/admin OAuth token). The
 * response is published as {@code httpResponse} for the generic assertion steps.
 */
public class GatewayRestArtifactsSteps {

    private final BaseSteps baseSteps = new BaseSteps();

    /** Basic-auth header for the acting actor's tenant admin (the gateway REST API needs admin Basic auth). */
    private Map<String, String> gatewayBasicAuthHeaders() {
        User admin = Identity.actingTenantAdmin();
        String creds = admin.getUserName() + ":" + admin.getPassword();
        String encoded = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic " + encoded);
        return headers;
    }

    /**
     * Retrieves a gateway synapse artifact ({@code kind} = {@code api-artifact} / {@code end-points} /
     * {@code local-entry} / {@code sequence}) for the API named {@code apiName} version {@code version} in
     * {@code tenantDomain}, publishing the response for assertion. Name/version resolve {@code {{...}}}.
     */
    @When("I retrieve the gateway {string} for API {string} version {string} in tenant {string}")
    public void iRetrieveGatewayArtifact(String kind, String apiName, String version, String tenantDomain)
            throws IOException {
        String resolvedName = Utils.resolveContextPlaceholders(apiName);
        String resolvedVersion = Utils.resolveContextPlaceholders(version);
        Requests.get(Utils.getGatewayArtifactURL(Utils.getBaseUrl(), kind, resolvedName, resolvedVersion, tenantDomain),
                gatewayBasicAuthHeaders());
    }

    /**
     * As above, but polls until the gateway returns 200 — the synapse artifact is not queryable immediately after a
     * deploy returns 201 (the gateway materialises it asynchronously), so a first query can 404. Retries until 200
     * or the deadline — catching only transient {@code IOException} (a network-level failure while the gateway
     * settles) and retaining the last real response for the failure message — then publishes the last response for
     * the following assertions.
     */
    @When("I retrieve the gateway {string} for API {string} version {string} in tenant {string} until it is available within {int} seconds")
    public void iRetrieveGatewayArtifactUntilAvailable(String kind, String apiName, String version,
            String tenantDomain, int timeoutSeconds) throws InterruptedException {
        String resolvedName = Utils.resolveContextPlaceholders(apiName);
        String resolvedVersion = Utils.resolveContextPlaceholders(version);
        String url = Utils.getGatewayArtifactURL(Utils.getBaseUrl(), kind, resolvedName, resolvedVersion, tenantDomain);
        long endTimeStart = System.currentTimeMillis();
        long endTime = endTimeStart + Math.max(timeoutSeconds * 1000L, Constants.RUNTIME_PROPAGATION_TIMEOUT);
        HttpResponse response = null;
        do {
            try {
                response = Requests.get(url, gatewayBasicAuthHeaders());
                if (response.getResponseCode() == 200) {
                    return;
                }
            } catch (IOException transientFailure) {
                // transient network failure while the gateway settles — keep polling; the previous
                // response (if any) is retained for the failure message
            }
            Utils.pollPause(endTimeStart, 2000);
        } while (System.currentTimeMillis() < endTime);
        Assert.assertNotNull(response, "Gateway artifact '" + kind + "' for " + resolvedName
                + " returned no response within " + timeoutSeconds + "s (every poll attempt failed)");
        Assert.assertEquals(response.getResponseCode(), 200,
                "Gateway artifact '" + kind + "' for " + resolvedName + " did not become available within "
                        + timeoutSeconds + "s; last: " + response.getData());
    }

    /**
     * Asserts that EXACTLY ONE of the retrieved synapse sequences belongs to the named flow ({@code In} /
     * {@code Out} / {@code Fault}) and that that sequence carries {@code expectedContent}. The flow is identified
     * by the sequence's own synapse name ending in {@code --<flow>} (e.g. {@code admin--MyAPI:v1.0.0--Out}).
     *
     * <p>Why a dedicated step rather than a substring check on the whole response: the mediation sequences all
     * arrive in ONE {@code sequences} array, so {@code The response should contain "<header>"} is satisfied by ANY
     * of them — a port that injects one header into all three flows cannot tell a missing Out or Fault sequence from
     * a present one. Legacy GatewayRestAPITestCase had the same hole in reverse: its per-flow checks sat inside an
     * unguarded {@code if (sequence.contains("--Out"))}, so the whole block passed silently when no sequence matched.
     * Asserting exactly-one match per flow closes both.
     *
     * @param flow            flow discriminator — {@code In}, {@code Out} or {@code Fault}
     * @param expectedContent text the matching flow's sequence XML must contain (resolves {@code {{...}}})
     */
    @Then("The gateway sequence for flow {string} should contain {string}")
    public void theGatewaySequenceForFlowShouldContain(String flow, String expectedContent) {
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response != null && response.getResponseCode() == 200
                        && response.getData() != null && !response.getData().isBlank(),
                "Expected a 200 sequences response to inspect flow '" + flow + "', but got: "
                        + (response == null ? "null" : response.getResponseCode() + " / " + response.getData()));
        String expected = Utils.resolveContextPlaceholders(expectedContent);
        JSONArray sequences = new JSONObject(response.getData()).getJSONArray("sequences");
        String flowMarker = "--" + flow;
        List<String> matches = new ArrayList<>();
        List<String> allNames = new ArrayList<>();
        for (int i = 0; i < sequences.length(); i++) {
            String sequenceXml = sequences.getString(i);
            String sequenceName = synapseSequenceName(sequenceXml);
            allNames.add(String.valueOf(sequenceName));
            // endsWith, not contains: `--` also precedes the API name, so contains("--In") would match an API
            // whose name merely starts with "In" (admin--Invoice:v1.0.0--Out).
            if (sequenceName != null && sequenceName.endsWith(flowMarker)) {
                matches.add(sequenceXml);
            }
        }
        Assert.assertEquals(matches.size(), 1, "Expected exactly ONE deployed sequence for flow '" + flow
                + "' (synapse name ending with '" + flowMarker + "') but found " + matches.size()
                + ". Sequence names: " + allNames);
        Assert.assertTrue(matches.get(0).contains(expected), "The '" + flow + "' flow sequence did not contain '"
                + expected + "': " + matches.get(0));
    }

    /** The {@code name} attribute of a synapse {@code <sequence …>} document, or {@code null} if absent. */
    private static String synapseSequenceName(String sequenceXml) {
        Matcher matcher = SEQUENCE_NAME_PATTERN.matcher(sequenceXml);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static final Pattern SEQUENCE_NAME_PATTERN =
            Pattern.compile("<sequence[^>]*\\sname=\"([^\"]+)\"");
}
