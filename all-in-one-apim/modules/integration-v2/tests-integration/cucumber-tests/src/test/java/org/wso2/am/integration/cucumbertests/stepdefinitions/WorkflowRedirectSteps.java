/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.Map;

/** Steps for asserting the HTTP redirect response returned by a custom subscription-creation workflow executor. */
public class WorkflowRedirectSteps {

    @When("I submit a subscription for API {string} and application {string} with payload {string}")
    public void submitSubscriptionForRedirect(String apiIdKey, String applicationIdKey, String payloadKey)
            throws IOException {

        String apiId = TestContext.resolve(apiIdKey).toString();
        String applicationId = TestContext.resolve(applicationIdKey).toString();
        String payload = TestContext.resolve(payloadKey).toString()
                .replace("{{apiId}}", apiId)
                .replace("{{applicationId}}", applicationId);
        payload = Utils.resolveContextPlaceholders(payload);
        Map<String, String> headers = Identity.bearerHeaders(Identity.devportalToken());
        HttpResponse response = Requests.post(Utils.getCreateSubscriptionURL(Utils.getBaseUrl()), headers, payload,
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertNotNull(response, "Subscription request returned no HTTP response");
        TestContext.set("httpResponse", response);
        Assert.assertTrue(response.getResponseCode() >= 200 && response.getResponseCode() < 300,
                "Subscription request failed: HTTP " + response.getResponseCode() + ": " + response.getData());
    }

    @Then("The subscription workflow response should contain redirect URL {string}")
    public void subscriptionWorkflowResponseShouldContainRedirectUrl(String expectedRedirectUrl) {

        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertNotNull(response, "No subscription workflow response is available");
        Assert.assertTrue(response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isBlank(),
                "Expected a 2xx subscription workflow response with a body, got " + response.getResponseCode()
                        + " / " + response.getData());
        JSONObject body = new JSONObject(response.getData());
        // The DevPortal REST API returns the custom executor's HttpWorkflowResponse JSON payload as the subscription
        // DTO's redirectionParams string (SubscriptionsApiServiceImpl#subscriptionsPost).
        String redirectionParams = body.optString("redirectionParams", null);
        Assert.assertNotNull(redirectionParams,
                "Subscription workflow response has no redirectionParams: " + response.getData());
        JSONObject workflowPayload = new JSONObject(redirectionParams);
        Assert.assertEquals(workflowPayload.optString("redirectUrl"), expectedRedirectUrl,
                "Custom subscription executor returned an unexpected redirect URL");
    }
}
