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

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.Collections;

/**
 * Glue for the carbon-health-check framework's {@code GET /api/health-check/v1.0/health} endpoint, including
 * the TM health check probe's keys (tm.throttle.receiver.*, tm.message.broker.*) it contributes. The endpoint
 * is unauthenticated, so unlike the other steps in this package no actor/token is involved.
 */
public class HealthCheckSteps {

    private String getBaseUrl() {
        return TestContext.get("baseUrl").toString();
    }

    @When("I call the health check endpoint")
    public void iCallTheHealthCheckEndpoint() throws IOException {
        Requests.get(Utils.getHealthCheckEndpointURL(getBaseUrl()), Collections.emptyMap());
    }

    /**
     * Polls until {@code key} reports {@code expectedValue}. The TM probe's first broker round trip after boot can
     * miss its receive window while the broker warms up; the following assertion steps still check the exact
     * values on the last response, so a broker that never comes up still fails.
     */
    @When("I call the health check endpoint until key {string} becomes {string} within {int} seconds")
    public void iCallTheHealthCheckEndpointUntilKeyBecomes(String key, String expectedValue, int timeoutSeconds)
            throws InterruptedException {

        String url = Utils.getHealthCheckEndpointURL(getBaseUrl());
        TestContext.remove("httpResponse");
        HttpResponse response = Utils.retryUntil(timeoutSeconds * 1000L,
                () -> Requests.get(url, Collections.emptyMap()),
                candidate -> candidate != null && candidate.getResponseCode() == 200 && candidate.getData() != null
                        && expectedValue.equals(lookupHealthKeyValue(candidate.getData(), key)));
        Requests.publishPollResult(response);
    }

    @Then("the health check response key {string} should have value {string}")
    public void theHealthCheckResponseKeyShouldHaveValue(String key, String expectedValue) {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isEmpty(),
                "Health check call did not return a usable body: " + (response == null ? "no response"
                        : "got=" + response.getResponseCode() + "/" + response.getData()));

        String actualValue = findHealthKeyValue(response.getData(), key);
        Assert.assertEquals(actualValue, expectedValue,
                "Health check key '" + key + "' was [" + actualValue + "] but expected [" + expectedValue
                        + "]. Full response: " + response.getData());
    }

    @And("the health check response should contain no key matching {string}")
    public void theHealthCheckResponseShouldContainNoKeyMatching(String substring) {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        JSONArray health = new JSONObject(response.getData()).getJSONArray("health");
        for (int i = 0; i < health.length(); i++) {
            String key = health.getJSONObject(i).getString("key");
            Assert.assertFalse(key.contains(substring),
                    "Did not expect any health check key to contain '" + substring + "' but found '" + key
                            + "'. Full response: " + response.getData());
        }
    }

    /**
     * The health check response is a flat {@code {"health":[{"key":...,"value":...}]}} array rather than a
     * plain JSON object, so a key's value can't be read with a simple field path - this scans for the
     * matching entry.
     */
    private String findHealthKeyValue(String jsonBody, String key) {

        String value = lookupHealthKeyValue(jsonBody, key);
        if (value == null) {
            Assert.fail("Health check response had no entry for key '" + key + "'. Full response: " + jsonBody);
        }
        return value;
    }

    /** The value for {@code key}, or null when the body has no such entry or is not a health check response. */
    private static String lookupHealthKeyValue(String jsonBody, String key) {

        JSONArray health = new JSONObject(jsonBody).optJSONArray("health");
        if (health == null) {
            return null;
        }
        for (int i = 0; i < health.length(); i++) {
            JSONObject entry = health.getJSONObject(i);
            if (key.equals(entry.optString("key"))) {
                return entry.optString("value", null);
            }
        }
        return null;
    }
}
