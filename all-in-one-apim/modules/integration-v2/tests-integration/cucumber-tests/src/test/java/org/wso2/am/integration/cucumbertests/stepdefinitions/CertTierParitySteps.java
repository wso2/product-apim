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

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Steps for the endpoint-certificate usage fixture (APIs on distinct per-index endpoint URLs) and for the
 * bounded throttle-trip check of the application-tier change scenario.
 */
public class CertTierParitySteps {

    private static final Logger logger = LoggerFactory.getLogger(CertTierParitySteps.class);

    /** Token in an endpoint template that is replaced by the API's index (0..count-1). */
    private static final String INDEX_TOKEN = "{index}";

    private final BaseSteps baseSteps = new BaseSteps();
    private final PublisherBaseSteps publisherSteps = new PublisherBaseSteps();

    /**
     * Creates {@code count} APIs (from the base test-API payload) named/contexted {@code prefix}0..N-1, whose
     * production and sandbox endpoint is {@code endpointTemplate} with {@code {index}} replaced by the API's
     * index, and stores their ids comma-separated under {@code idsKey}. No publish/deploy: certificate usage is
     * computed from the endpoint config. Each API is registered for teardown by the create primitive.
     */
    @Given("I create {int} APIs with per-index production endpoint {string} named {string} storing their ids as {string}")
    public void iCreateApisWithPerIndexEndpoint(int count, String endpointTemplate, String namePrefixRef,
                                                String idsKey) throws IOException {
        String prefix = Utils.resolveContextPlaceholders(namePrefixRef);
        String template = Utils.resolveContextPlaceholders(endpointTemplate);
        List<String> createdIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String endpoint = template.replace(INDEX_TOKEN, String.valueOf(i));
            baseSteps.putJsonPayloadFromFile("artifacts/payloads/create_apim_test_api.json", "<epIdxApiPayload>");
            JSONObject json = new JSONObject(TestContext.resolve("<epIdxApiPayload>").toString());
            json.put("name", prefix + i);
            json.put("context", prefix + i);
            JSONObject endpointConfig = new JSONObject();
            endpointConfig.put("endpoint_type", "http");
            endpointConfig.put("production_endpoints", new JSONObject().put("url", endpoint));
            endpointConfig.put("sandbox_endpoints", new JSONObject().put("url", endpoint));
            json.put("endpointConfig", endpointConfig);
            baseSteps.putJsonPayloadInContext("<epIdxApiPayload>", json.toString());
            publisherSteps.iCreateAnAPIWithPayloadAs("apis", "<epIdxApiPayload>", "epIdxApiId");
            createdIds.add(TestContext.resolve("epIdxApiId").toString());
        }
        TestContext.set(Utils.normalizeContextKey(idsKey), String.join(",", createdIds));
    }

    /**
     * Invokes a deployed API once every {@code intervalMillis} and asserts it is throttled (429) no later than
     * call {@code maxCalls}; every call before the 429 must be 200. The 429 response is published for the fault
     * assertions that follow. With {@code maxCalls} well below a higher tier's per-minute limit, reaching 429
     * within the bound shows the LOWER tier is the one enforced. The pacing leaves room for the asynchronous
     * traffic-manager decision to reach the gateway. Each call is retried only on a transient IOException.
     */
    @When("I invoke the API at gateway context {string} with method {string} using access token {string} every {int} milliseconds expecting status 429 by call {int}")
    public void invokeExpectingThrottleByCall(String context, String httpMethod, String accessToken,
                                              int intervalMillis, int maxCalls) throws Exception {
        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String endpointUrl = Utils.getBaseGatewayUrl()
                + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext;
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + TestContext.resolve(accessToken).toString());
        String method = httpMethod.toUpperCase();
        Assert.assertEquals(method, "GET", "Only GET is supported by the bounded throttle-trip step.");

        for (int call = 1; call <= maxCalls; call++) {
            if (call > 1) {
                Thread.sleep(intervalMillis);
            }
            HttpResponse response = Utils.retryUntil(Constants.RUNTIME_PROPAGATION_TIMEOUT,
                    () -> SimpleHTTPClient.getInstance().doGet(endpointUrl, headers),
                    completed -> true);
            Requests.publishPollResult(response);
            Assert.assertNotNull(response, "Call " + call + " of at most " + maxCalls
                    + " never completed (gateway unreachable within the warmup window).");
            if (response.getResponseCode() == 429) {
                logger.info("Throttled (429) at call {} of at most {} ({} ms pacing) for {}", call, maxCalls,
                        intervalMillis, resolvedContext);
                return;
            }
            Assert.assertEquals(response.getResponseCode(), 200, "Call " + call + " of at most " + maxCalls
                    + " was neither 200 nor 429; body: " + response.getData());
        }
        Assert.fail("No 429 within " + maxCalls + " calls at " + intervalMillis + " ms pacing: the enforced limit "
                + "is higher than the expected tier allows.");
    }
}
