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
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Step definitions for admin-plane API provider change (port of ChangeApiProviderTestCase). Uses the admin REST
 * {@code POST /api/am/admin/v4/apis/{apiId}/change-provider?provider=<name>} operation. Publishes the response as
 * {@code httpResponse} so the feature asserts the exact status (200 success, 400 + 901409 on a cross-tenant
 * provider). The retained-metadata assertions (provider/docs/scopes/endpoints) are made by the feature via a
 * subsequent Publisher GET.
 */
public class ApiProviderChangeSteps {

    private final BaseSteps baseSteps = new BaseSteps();

    /**
     * Changes an API's provider to {@code providerName} via the admin change-provider REST operation, using the
     * acting actor's admin token. {@code providerName} resolves {@code {{...}}} placeholders (so a provisioned
     * user's key flows through). Non-asserting — the feature asserts the resulting status (and, on success, the
     * retained metadata via a following GET).
     *
     * @param apiIdKey     context key holding the API id
     * @param providerName the new provider username (may be tenant-qualified or {@code SECONDARY/}-prefixed)
     */
    @When("I change the provider of API {string} to {string}")
    public void iChangeApiProvider(String apiIdKey, String providerName) throws IOException {
        String apiId = TestContext.resolve(apiIdKey).toString();
        String provider = Utils.resolveContextPlaceholders(providerName);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        // change-provider is a POST with the new provider as a query param and no body.
        Requests.post(Utils.getAPIProvider(Utils.getBaseUrl(), apiId, provider), headers, "",
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Attempts the change-provider operation with the {@code provider} query parameter OMITTED ENTIRELY — the
     * required-parameter negative. Distinct from passing an empty value: {@code ?provider=} is present and reaches
     * the resource's own user-existence branch, while an absent parameter is refused by the parameter validator
     * before the resource runs. Non-asserting; the feature asserts the 400.
     *
     * @param apiIdKey context key holding the API id
     */
    @When("I attempt to change the provider of API {string} without naming a provider")
    public void iAttemptToChangeApiProviderWithoutProvider(String apiIdKey) throws IOException {
        String apiId = TestContext.resolve(apiIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        Requests.post(Utils.getAPIProvider(Utils.getBaseUrl(), apiId), headers, "",
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Attempts the change-provider operation with the acting actor's PUBLISHER token — the authorization negative.
     * The operation is guarded by {@code apim:admin} + {@code apim:api_provider_change}, neither of which a
     * creator/publisher token carries, so this is the only way to drive the endpoint as a non-admin: the positive
     * step above always authenticates with the admin token. Non-asserting; the feature asserts the 401.
     *
     * @param apiIdKey     context key holding the API id
     * @param providerName the new provider username (resolves {@code {{...}}})
     */
    @When("I attempt to change the provider of API {string} to {string} using the publisher token")
    public void iAttemptToChangeApiProviderAsPublisher(String apiIdKey, String providerName) throws IOException {
        String apiId = TestContext.resolve(apiIdKey).toString();
        String provider = Utils.resolveContextPlaceholders(providerName);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        Requests.post(Utils.getAPIProvider(Utils.getBaseUrl(), apiId, provider), headers, "",
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /** Context key holding the two responses of the concurrent change-provider pair. */
    private static final String CONCURRENT_RESPONSES_KEY = "concurrentProviderChangeResponses";

    /**
     * Issues TWO change-provider calls on the same API AT THE SAME TIME, one per named candidate owner, and stores
     * both responses for the assertion step below.
     *
     * <p>Deliberately NOT funnelled through {@code Requests.*}: that funnel publishes its response to the shared
     * {@code httpResponse} key, and two concurrent calls would race each other for it, leaving the following
     * assertion reading whichever landed last (§7's stale-response trap, in its worst form). The pair is the
     * assertion target here, so it is stored under its own key and {@code httpResponse} is cleared rather than
     * left holding one arbitrary half of the race.
     *
     * <p>Both requests are released from one latch so they overlap in the server rather than merely following each
     * other. Non-asserting on the individual statuses; the feature asserts the allowed pair.
     *
     * @param apiIdKey    context key holding the API id both calls target
     * @param firstName   the new provider username the first call names (resolves {@code {{...}}})
     * @param secondName  the new provider username the second call names (resolves {@code {{...}}})
     */
    @When("I change the provider of API {string} concurrently to {string} and {string}")
    public void iChangeApiProviderConcurrently(String apiIdKey, String firstName, String secondName)
            throws Exception {
        String apiId = TestContext.resolve(apiIdKey).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        String firstUrl = Utils.getAPIProvider(Utils.getBaseUrl(), apiId,
                Utils.resolveContextPlaceholders(firstName));
        String secondUrl = Utils.getAPIProvider(Utils.getBaseUrl(), apiId,
                Utils.resolveContextPlaceholders(secondName));

        TestContext.remove("httpResponse");
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<HttpResponse>> futures = new ArrayList<>();
            for (String url : new String[]{firstUrl, secondUrl}) {
                Callable<HttpResponse> call = () -> {
                    release.await();
                    return SimpleHTTPClient.getInstance().doPost(url, headers, "",
                            Constants.CONTENT_TYPES.APPLICATION_JSON);
                };
                futures.add(pool.submit(call));
            }
            release.countDown();
            List<HttpResponse> responses = new ArrayList<>();
            for (Future<HttpResponse> future : futures) {
                // A timeout here is itself a finding — an unanswered request is one of the outcomes the scenario
                // forbids — so it is allowed to surface as the step's failure.
                responses.add(future.get(Constants.RUNTIME_PROPAGATION_TIMEOUT, TimeUnit.MILLISECONDS));
            }
            TestContext.set(CONCURRENT_RESPONSES_KEY, responses);
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * Asserts EVERY response of the concurrent pair is either the success status, or the failure status carrying
     * the given management error code. The disjunction is the specified outcome of a race — one contender may lose
     * its transaction — and not a widened stand-in for an unknown value: any other status, any other error code,
     * and any unanswered request fails here.
     *
     * @param successStatus the status a winning call returns
     * @param failureStatus the status a losing call returns
     * @param failureCode   the management error {@code code} a losing call must carry
     */
    @Then("Each concurrent provider change should have returned status {int} or status {int} with error code {string}")
    public void eachConcurrentProviderChangeShouldHaveReturned(int successStatus, int failureStatus,
                                                               String failureCode) {
        Object stored = TestContext.get(CONCURRENT_RESPONSES_KEY);
        Assert.assertNotNull(stored, "No concurrent change-provider responses were recorded");
        @SuppressWarnings("unchecked")
        List<HttpResponse> responses = (List<HttpResponse>) stored;
        Assert.assertEquals(responses.size(), 2, "Expected exactly two concurrent change-provider responses");
        for (HttpResponse response : responses) {
            Assert.assertNotNull(response, "A concurrent change-provider request went unanswered");
            int status = response.getResponseCode();
            if (status == successStatus) {
                continue;
            }
            Assert.assertEquals(status, failureStatus, "A concurrent change-provider answered " + status
                    + ", which is neither the success status " + successStatus + " nor the failure status "
                    + failureStatus + ". Body: " + response.getData());
            Assert.assertTrue(response.getData() != null && !response.getData().isBlank(),
                    "A concurrent change-provider failed with " + status + " and no body to carry an error code");
            Assert.assertEquals(String.valueOf(new JSONObject(response.getData()).opt("code")), failureCode,
                    "A concurrent change-provider failed with an unexpected error code. Body: "
                            + response.getData());
        }
    }
}
