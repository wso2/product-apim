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
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Step definitions for listing subscriptions by API id and asserting subscription-list sizes. Ports the
 * listing behaviours of APIM710AllSubscriptionsByApplicationTestCase — the by-application-id listing reuses
 * the existing "I retrieve all subscriptions of application" step, while the by-API-id variant and the
 * list-count assertion are added here (the shared step class already carries the by-application variant and
 * the "subscription is in the list" assertion, which this feature also reuses). Kept in a NEW class so the
 * shared step classes are untouched.
 */
public class SubscriptionListingSteps {

    private final BaseSteps baseSteps = new BaseSteps();

    private String getBaseUrl() {
        return baseSteps.getBaseUrl();
    }

    /**
     * Retrieves all subscriptions of a given API id (devportal {@code GET /subscriptions?apiId=…}) as the
     * devportal actor, publishing the response for assertion. The counterpart of the shared by-application
     * variant, needed for the "list subscriptions by API id" arc.
     */
    @When("I retrieve all subscriptions of api {string}")
    public void iRetrieveAllSubscriptionsOfApi(String apiIdKey) throws IOException {
        String apiId = TestContext.resolve(apiIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.devportalToken());
        Requests.get(Utils.getAllSubscriptionsURL(getBaseUrl(), apiId, null, null, null, null), headers);
    }

    /**
     * Asserts the subscription list in the last response reports exactly {@code expectedCount} entries — the
     * DTO's top-level {@code count} field and the actual {@code list} length must both equal it (mirrors the
     * legacy {@code SubscriptionListDTO.getCount()} / list-size checks).
     */
    @Then("The subscription list should contain exactly {int} subscriptions")
    public void theSubscriptionListShouldContainExactly(int expectedCount) {
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response != null && response.getResponseCode() >= 200 && response.getResponseCode() < 300
                        && response.getData() != null && !response.getData().isEmpty(),
                "No successful subscription-list response to assert on, got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));
        JSONObject body = new JSONObject(response.getData());
        int listLength = body.getJSONArray("list").length();
        Assert.assertEquals(listLength, expectedCount,
                "Subscription list length mismatch: " + response.getData());
        if (body.has("count")) {
            Assert.assertEquals(body.getInt("count"), expectedCount,
                    "Subscription list 'count' field mismatch: " + response.getData());
        }
    }
}
