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
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.HmacTestUtils;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Names;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Gateway WebSub-API runtime glue — the port of WebSubAPITestCase / SecretValidationTestCase. Drives the THREE
 * distinct network legs a WebSub arc needs, none of which the ordinary REST invocation steps can express:
 *
 * <ol>
 *   <li><b>Hub (un)subscribe</b> — a POST to the API's gateway context carrying {@code hub.mode} / {@code hub.topic}
 *       / {@code hub.callback} / {@code hub.secret} / {@code hub.lease_seconds}, either as QUERY parameters or as a
 *       form-urlencoded BODY (the legacy covers both encodings, and they are not interchangeable in the product).</li>
 *   <li><b>Content publish</b> — a POST by the event SOURCE to the hub's event-receiver inbound, a listener
 *       SEPARATE from the gateway passthrough ({@link Utils#getBaseWebSubEventReceiverUrl()}), signed with the
 *       API's own {@code websubSubscriptionConfiguration.secret}.</li>
 *   <li><b>Persisted-subscription read</b> — the internal {@code webhooks-subscriptions} resource, which is the only
 *       surface that observes a registration having been RECORDED without depending on the hub's fan-out (and,
 *       because its query filters on {@code EXPIRY_AT}, is also where lease expiry is observable).</li>
 *   <li><b>Subscriber-side introspection</b> — reading back what the hub actually delivered. The callback must be
 *       reachable from INSIDE the APIM container (the hub delivers server-to-server), so it is the
 *       {@code websub-receiver} node app on the shared network; the recorded deliveries are read from the test JVM
 *       over its HOST-published port.</li>
 * </ol>
 *
 * <p>Receivers are isolated per NAME so parallel scenarios cannot cross-count (§4): every scenario mints its own
 * via {@link Names#unique}. Absence of a delivery is proved with a POSITIVE BARRIER — a second, still-subscribed
 * receiver that must observe the same event — never by sleeping and hoping.
 *
 * <p><b>The FAN-OUT leg is currently PARKED in the feature.</b> Every step here that asserts a delivery — the settled
 * and exact delivery counts, the delivered body/signature and the {@code link} header — is retained and working glue,
 * but is referenced ONLY by the commented-out scenarios in {@code features/gateway/websub_invocation.feature}. The
 * hub's fan-out reads an in-memory subscriber map that is empty for a runtime-created subscription on this lane; the
 * feature's shared park note carries the full evidence and the named next step. What is NOT parked, and is what
 * narrowed that note this increment, is the persisted-subscription read below: it proves the registration reached the
 * DAO, so the break lies strictly between the {@code asyncWebhooksData} publish and the gateway's in-memory map.
 * Nothing needs to be added here to re-enable those scenarios — do not "simplify" these steps away as unused.
 *
 * <p>Every wait funnels through {@link Utils#retryUntil}; the request steps publish through {@link Requests} so the
 * feature asserts the exact status itself, and the local introspection reads use {@link SimpleHTTPClient} directly
 * because their bodies are consumed inside the step (§7).
 */
public class WebSubInvocationSteps {

    private static final Log log = LogFactory.getLog(WebSubInvocationSteps.class);

    /**
     * Container port of the {@code websub-receiver} node app (see
     * {@code tests-common/testcontainers/.../nodeapps/websub-receiver/server.js}). In-network it is addressed as
     * {@code http://nodebackend:3022}; the host-published mapping is resolved via
     * {@link Utils#getNodeBackendUrl(int)}.
     */
    private static final int WEBSUB_RECEIVER_PORT = 3022;

    /** In-network host the APIM container reaches the node backend at (the shared-network alias). */
    private static final String NODE_BACKEND_HOST = "nodebackend";

    /** Suffix appended to the receiver's context key to hold its in-network callback URL. */
    private static final String CALLBACK_KEY_SUFFIX = "Callback";

    /**
     * Sub-path appended to a WebSub API's gateway context for HUB (un)subscribe requests. NOT known to be required:
     * it is retained only so that the payload fix below and this path were not changed as two variables at once, and
     * is a candidate for removal once the block runs green.
     *
     * <p>MEASURED, in this order:
     * <ol>
     *   <li>posting to the bare {@code <context>/<version>} was rejected with {@code 403} and
     *       {@code 900906 "No matching resource found in the API for the given request"};</li>
     *   <li>appending this segment did NOT fix it — the same {@code 403}/900906 came back. So the path shape was
     *       never the cause.</li>
     * </ol>
     *
     * <p>RESOLVED cause: the API's own declared operations — but NOT because the {@code POST} verb was rejected. The
     * matcher never sees {@code POST}. {@code WebhookApiHandler.handleRequest} rewrites the request before auth: it
     * sets {@code HTTP_METHOD} to the literal {@code SUBSCRIBE} and elects {@code hub.topic} as
     * {@code API_ELECTED_RESOURCE}, then delegates to {@code APIAuthenticationHandler}. So the URI-template match
     * that decides 900906 is <em>verb {@code SUBSCRIBE} AND urlPattern == the request's {@code hub.topic}</em>, and it
     * runs against the API's declared operations, not against the synapse template's
     * {@code <resource url-mapping="/*">}. These payloads declared
     * {@code operations: [{target:"/*", verb:"SUBSCRIBE"}]} while every scenario subscribes to topic
     * {@code _default}: the verb agreed, the target did not ({@code /*} != {@code _default}), so no template matched,
     * {@code NO_MATCHING_AUTH_SCHEME} became 900906 and the request never reached the hub resource. No path, empty or
     * otherwise, can get past that. The fix is therefore in the payloads: they no longer send {@code operations},
     * letting the product default the WEBSUB URI templates to target {@code _default} exactly as the legacy did (the
     * legacy posted to the bare context and asserted 202 precisely because it never sent {@code operations}).
     * Declaring {@code operations} explicitly would have to use {@code target:"_default", verb:"SUBSCRIBE"}, which is
     * byte-identical to that default — hence sending none.
     *
     * <p>The same topic-vs-template match gates the delivery leg: {@code WebhookApiHandler.validateTopic} matches the
     * event receiver's {@code ?topic=} against the same {@code getUrlMappings()}, and a miss becomes the {@code 404}
     * the unknown-topic scenario asserts.
     *
     * <p>The SSE port is not subject to this: {@code type=SSE} renders {@code methods="GET"} and its invocation is a
     * GET, so its declared operation and its verb agree. Its own {@code /events} sub-path records the bare-context
     * symptom as a {@code 404}, a different code from the {@code 403}/900906 measured here.
     *
     * <p>Cost a killed run to find, because a wrong expectation on a hub leg burns the full 180s {@code retryUntil}
     * floor per step.
     */
    private static final String HUB_PATH = "/hub";

    /**
     * How long a receiver's delivery count must stay UNCHANGED before it is taken as final (see
     * {@link Utils#awaitSettledCount}). Must exceed the gap between two consecutive deliveries of one fan-out, not
     * the fan-out's total duration — the hub delivers server-to-server on the same LAN with no backoff between
     * distinct events, so consecutive arrivals are sub-second; 10s leaves an order of magnitude of headroom while
     * keeping the added cost to one quiet window per count assertion. Raise it if a count ever settles LOW.
     */
    private static final long DELIVERY_SETTLE_QUIET_MILLIS = 10_000L;

    /**
     * Internal (data-plane) REST resource listing the PERSISTED webhook subscriptions of one tenant — the
     * {@code AM_WEBHOOKS_SUBSCRIPTION} read the legacy WebSubAPITestCase#testMultipleSubscriptions asserted
     * through {@code restAPIInternal.retrieveWebhooksSubscriptions()}. It is the only interface that observes the
     * persistence leg of a registration WITHOUT relying on the hub's fan-out, so it also carries the lease-expiry
     * assertion: its query is {@code WHERE (EXPIRY_AT >= now OR EXPIRY_AT = 0)}, so a subscription whose
     * {@code hub.lease_seconds} has elapsed drops out of this list by construction.
     */
    private static final String INTERNAL_WEBHOOKS_SUBSCRIPTIONS_PATH = "internal/data/v1/webhooks-subscriptions";

    /** Header the internal API requires to select the tenant whose subscriptions are listed. */
    private static final String TENANT_HEADER = "xWSO2Tenant";

    /**
     * Quiet window for the PERSISTED-subscription count. Far shorter than {@link #DELIVERY_SETTLE_QUIET_MILLIS}
     * because the write is synchronous with the hub request rather than an asynchronous fan-out: the hub's
     * {@code SubscribersPersistMediator} POSTs to {@code /internal/data/v1/notify} with a BLOCKING http client and
     * inspects the status, so by the time a subscribe/unsubscribe has answered, the row is already written. The
     * window only has to outlast the gap between two consecutive rows of one scenario's back-to-back
     * registrations, which is one HTTP round trip.
     */
    private static final long PERSISTED_SETTLE_QUIET_MILLIS = 5_000L;

    /**
     * Mints a uniquely-named WebSub callback receiver on the node backend and stores BOTH the name (under
     * {@code contextKey}) and its IN-NETWORK callback URL (under {@code contextKey + "Callback"}, the value a
     * {@code hub.callback} must carry, since the hub delivers from inside the APIM container).
     *
     * <p>{@code flavour} selects the receiver's handshake behaviour — the two legacy callback servlets:
     * <ul>
     *   <li>{@code verifying} — echoes {@code hub.challenge} back (200 {@code text/plain}), i.e. a conforming
     *       subscriber. Required when the API sets {@code enableSubscriberVerification}.</li>
     *   <li>{@code silent} — records the handshake but answers with an EMPTY body (legacy
     *       {@code CallbackServerServlet}), i.e. a subscriber that never confirms.</li>
     * </ul>
     * No reset is needed (and none is issued): the name is unique per scenario, so nothing can be inherited.
     */
    @Given("I have a {string} WebSub callback receiver stored as {string}")
    public void iHaveAWebSubCallbackReceiver(String flavour, String contextKey) {

        String family = switch (flavour) {
            case "verifying" -> "receiver";
            case "silent" -> "silent";
            default -> throw new IllegalArgumentException(
                    "Unknown WebSub callback receiver flavour '" + flavour + "' — expected 'verifying' or 'silent'");
        };
        String key = Utils.normalizeContextKey(contextKey);
        String name = Names.unique(key).replaceAll("[^A-Za-z0-9]", "");
        TestContext.set(key, name);
        TestContext.set(key + CALLBACK_KEY_SUFFIX,
                "http://" + NODE_BACKEND_HOST + ":" + WEBSUB_RECEIVER_PORT + "/" + family + "/" + name);
    }

    /**
     * Sends a WebSub hub request ({@code hub.mode=subscribe} / {@code unsubscribe}) to the API's gateway context
     * with the parameters in a FORM-URLENCODED body, retrying until the expected status. An empty
     * {@code leaseSeconds} omits {@code hub.lease_seconds} (the legacy's "infinite expiry" variant); an empty
     * {@code hubMode} omits nothing and sends the parameter blank — that is the missing-mandatory-parameter
     * negative.
     */
    @When("I send a WebSub {string} request as form data to gateway context {string} with callback {string} topic {string} secret {string} lease seconds {string} using access token {string} until response status code becomes {int} within {int} seconds")
    public void sendHubRequestAsFormData(String hubMode, String context, String callback, String topic, String secret,
                                         String leaseSeconds, String accessToken, int expectedStatus,
                                         int timeoutSeconds) throws Exception {
        sendHubRequest(hubMode, context, callback, topic, secret, leaseSeconds, accessToken, false, expectedStatus,
                timeoutSeconds);
    }

    /**
     * As {@link #sendHubRequestAsFormData} but with the hub parameters in the QUERY STRING and an EMPTY body
     * (the legacy testInvokeWebSubApiWithQueryParameters encoding). Kept as a distinct step because the two
     * encodings travel different code paths in the hub and the legacy asserts both.
     */
    @When("I send a WebSub {string} request as query parameters to gateway context {string} with callback {string} topic {string} secret {string} lease seconds {string} using access token {string} until response status code becomes {int} within {int} seconds")
    public void sendHubRequestAsQueryParameters(String hubMode, String context, String callback, String topic,
                                                String secret, String leaseSeconds, String accessToken,
                                                int expectedStatus, int timeoutSeconds) throws Exception {
        sendHubRequest(hubMode, context, callback, topic, secret, leaseSeconds, accessToken, true, expectedStatus,
                timeoutSeconds);
    }

    private void sendHubRequest(String hubMode, String context, String callback, String topic, String secret,
                                String leaseSeconds, String accessToken, boolean asQueryParameters,
                                int expectedStatus, int timeoutSeconds) throws Exception {

        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String resolvedCallback = Utils.resolveContextPlaceholders(callback);
        String resolvedTopic = Utils.resolveContextPlaceholders(topic);
        String resolvedSecret = Utils.resolveContextPlaceholders(secret);
        String parameters = hubParameters(hubMode, resolvedCallback, resolvedTopic, resolvedSecret,
                Utils.resolveContextPlaceholders(leaseSeconds));
        // HUB_PATH is not known to be required — the 403/900906 came from the payload's declared operations, not from
        // the path. Retained pending a green run; see the constant.
        String gatewayUrl = Utils.getBaseGatewayUrl()
                + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext + HUB_PATH;
        String what = "WebSub " + (hubMode.isEmpty() ? "<no hub.mode>" : hubMode) + " at "
                + resolvedContext + HUB_PATH;

        retryUntilStatus(what, accessToken, expectedStatus, timeoutSeconds, () -> {
            Map<String, String> headers = new HashMap<>();
            headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION,
                    "Bearer " + TestContext.resolve(accessToken).toString());
            if (asQueryParameters) {
                return Requests.post(gatewayUrl + "?" + parameters, headers, "",
                        Constants.CONTENT_TYPES.APPLICATION_JSON);
            }
            return Requests.post(gatewayUrl, headers, parameters,
                    Constants.CONTENT_TYPES.APPLICATION_X_WWW_FORM_URLENCODED);
        });
    }

    /** Builds the {@code hub.*} parameter string shared by the query-parameter and form-body encodings. */
    private static String hubParameters(String hubMode, String callback, String topic, String secret,
                                        String leaseSeconds) {
        StringBuilder parameters = new StringBuilder()
                .append("hub.callback=").append(Utils.urlEncode(callback))
                .append("&hub.mode=").append(Utils.urlEncode(hubMode))
                .append("&hub.secret=").append(Utils.urlEncode(secret))
                .append("&hub.topic=").append(Utils.urlEncode(topic));
        if (!leaseSeconds.isBlank()) {
            parameters.append("&hub.lease_seconds=").append(Utils.urlEncode(leaseSeconds));
        }
        return parameters.toString();
    }

    /**
     * Publishes ONE event as the event SOURCE: a POST of {@code eventBody} to the hub's event-receiver inbound for
     * the given API context and {@code topic}, carrying the {@code x-hub-signature} computed over the body with the
     * API's OWN secret — retrying until the expected status. Also the unknown-topic negative (a topic the API does
     * not expose) and the routability probe for a freshly deployed WebSub API.
     */
    @When("I publish the WebSub event {string} to the event receiver at gateway context {string} topic {string} signed with secret {string} until response status code becomes {int} within {int} seconds")
    public void publishEventUntilStatus(String eventBody, String context, String topic, String secret,
                                        int expectedStatus, int timeoutSeconds) throws Exception {

        String body = TestContext.resolve(eventBody).toString();
        String resolvedTopic = Utils.resolveContextPlaceholders(topic);
        String receiverUrl = eventReceiverUrl(context, resolvedTopic);
        String signature = HmacTestUtils.hubSignature("SHA1", body,
                Utils.resolveContextPlaceholders(secret));

        retryUntilStatus("WebSub event publish to " + receiverUrl, null, expectedStatus, timeoutSeconds,
                () -> Requests.post(receiverUrl, signatureHeaders(signature), body,
                        Constants.CONTENT_TYPES.APPLICATION_JSON));
    }

    /**
     * Publishes {@code times} events back-to-back as the event source, asserting EACH is accepted with 200 — the
     * multi-event delivery-count arc. Each individual POST is retried only until it COMPLETES (a transient
     * connectivity error during warm-up), and the 200 is then asserted per event so a mid-burst rejection is
     * reported against the event that caused it rather than a final count mismatch.
     */
    @When("I publish the WebSub event {string} to the event receiver at gateway context {string} topic {string} signed with secret {string} {int} times expecting status 200")
    public void publishEventNTimes(String eventBody, String context, String topic, String secret, int times)
            throws Exception {

        String body = TestContext.resolve(eventBody).toString();
        String receiverUrl = eventReceiverUrl(context, Utils.resolveContextPlaceholders(topic));
        String signature = HmacTestUtils.hubSignature("SHA1", body,
                Utils.resolveContextPlaceholders(secret));

        for (int i = 1; i <= times; i++) {
            HttpResponse response = Utils.retryUntil(Constants.RUNTIME_PROPAGATION_TIMEOUT,
                    () -> Requests.post(receiverUrl, signatureHeaders(signature), body,
                            Constants.CONTENT_TYPES.APPLICATION_JSON),
                    completed -> true);
            Assert.assertNotNull(response, "WebSub event " + i + " of " + times + " never completed — the event "
                    + "receiver at " + receiverUrl + " was unreachable within the warmup window.");
            Assert.assertEquals(response.getResponseCode(), 200, "WebSub event " + i + " of " + times
                    + " was not accepted by the event receiver; response: " + response.getData());
        }
    }

    private static Map<String, String> signatureHeaders(String signature) {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-hub-signature", signature);
        headers.put("Accept", Constants.CONTENT_TYPES.APPLICATION_JSON);
        return headers;
    }

    /**
     * {@code <eventReceiverBase><apiContext+version><WEBSUB_EVENT_RECEIVER_RESOURCE>?topic=<topic>} — the URL an
     * event source posts to. The context is used VERBATIM (it already carries any {@code /t/<tenant>} prefix), and
     * the port comes from the block's published mapping, never a literal (§4).
     */
    private static String eventReceiverUrl(String context, String topic) {
        String resolvedContext = Utils.resolveContextPlaceholders(context);
        String base = Utils.getBaseWebSubEventReceiverUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + (resolvedContext.startsWith("/") ? "" : "/") + resolvedContext
                + Constants.WEBSUB_EVENT_RECEIVER_RESOURCE + "?topic=" + Utils.urlEncode(topic);
    }

    /**
     * THE retry envelope for this family (the WebSub counterpart of {@code APIInvocationSteps#invokeUntilStatus}):
     * delegates the loop, deadline and exception policy to {@link Utils#retryUntil}, reports an UNEXPECTED 401 once
     * through {@link Utils#logAuthRejection} so a revoked credential is named rather than surfacing as an opaque
     * status mismatch, then asserts the EXACT expected status on the last response the loop captured.
     */
    private void retryUntilStatus(String what, String credentialContextKey, int expectedStatus, int timeoutSeconds,
                                  Utils.RetryAttempt<HttpResponse> attempt) throws Exception {

        long started = System.currentTimeMillis();
        HttpResponse last = Utils.retryUntil(timeoutSeconds * 1000L, attempt,
                response -> response.getResponseCode() == expectedStatus);
        if (last != null && last.getResponseCode() == 401 && expectedStatus != 401) {
            String credential = null;
            if (credentialContextKey != null) {
                try {
                    credential = TestContext.resolve(credentialContextKey).toString();
                } catch (RuntimeException unresolvable) {
                    credential = null;
                }
            }
            Utils.logAuthRejection(what, credentialContextKey, credential, last.getResponseCode(), last.getData(),
                    System.currentTimeMillis() - started);
        }
        Assert.assertNotNull(last, "No response was captured for " + what + " while waiting for status "
                + expectedStatus + " — every attempt threw (event receiver/gateway unreachable, or a bad "
                + "token/payload context key).");
        Assert.assertEquals(last.getResponseCode(), expectedStatus, what + " did not return " + expectedStatus
                + " within the deadline; last response: " + last.getData());
    }

    /**
     * Waits until the named receiver's delivery count has SETTLED, then asserts it is exactly
     * {@code expectedCount}. Uses {@link Utils#awaitSettledCount} rather than {@link Utils#retryUntil} precisely
     * so that an OVER-delivery is caught: an accept-on-{@code >= expected} poll passes the instant the count
     * touches the expected value, leaving any duplicate or unthrottled extra that arrives moments later
     * permanently invisible (§12 — the widened form is not made exact by the assertion that follows it).
     * Settling also removes the need for a sleep to prove "and nothing further arrived" (§4).
     */
    @Then("The WebSub receiver {string} should have received {int} event(s) within {int} seconds")
    public void receiverShouldHaveReceivedWithin(String receiverKey, int expectedCount, int timeoutSeconds)
            throws Exception {

        String name = TestContext.resolve(receiverKey).toString();
        Utils.SettledCount count = Utils.awaitSettledCount(DELIVERY_SETTLE_QUIET_MILLIS, timeoutSeconds * 1000L,
                () -> readReceiver(name).getInt("count"));
        String observed = "last seen " + count.value() + " over " + count.samples() + " sample(s), quiet window "
                + DELIVERY_SETTLE_QUIET_MILLIS + "ms";
        Assert.assertTrue(count.settled(), "WebSub receiver '" + name + "' delivery count never stopped changing "
                + "within the deadline (" + observed + ") — either deliveries were still arriving, so no exact "
                + "count can be asserted, or the receiver could not be read at all (a -1 value, 0 samples).");
        // The full receiver state (which events, with headers) is what identifies an UNEXPECTED delivery, so it is
        // worth reporting — but read it only when the count is already known wrong: an argument to assertEquals is
        // evaluated on the passing path too, where it would be a needless GET per assertion and a receiver blip
        // would turn a passing assertion into a spurious IOException.
        String state = count.value() == expectedCount ? "" : "; state: " + readReceiver(name);
        Assert.assertEquals(count.value(), expectedCount, "WebSub receiver '" + name
                + "' settled delivery count mismatch (" + observed + ")" + state);
    }

    /**
     * Asserts the named receiver's delivery count RIGHT NOW, with no waiting — the "and nothing more was
     * delivered" half of an absence check. Only sound after a POSITIVE BARRIER has been awaited (a second,
     * still-subscribed receiver observing the same event proves the hub's fan-out for that event has completed),
     * which is how these scenarios avoid a sleep.
     */
    /**
     * Asserts the named receiver's delivery count SETTLED strictly BELOW a ceiling — the assertion shape an
     * EVENT-COUNT QUOTA needs, because an exact count is not available to assert against.
     *
     * <p>WHY NOT AN EXACT COUNT, measured rather than assumed: APIM's throttling counts locally on the gateway and
     * reconciles with the traffic manager ASYNCHRONOUSLY, so a burst OVERSHOOTS the configured limit before
     * enforcement engages. Measured on a live server: a 9-events/month plan delivered 12, and in this suite a
     * 2-event plan delivered 6. The quota is real, but its boundary is approximate under burst, so
     * {@code should have received N events} can only be written by guessing the overshoot — which is how the
     * previous version of the quota scenario came to assert a number it could never reliably hit.
     *
     * <p>WHY NOT A STATUS ASSERTION: the quota is invisible to the publisher. Measured — publishing past an
     * exhausted quota still answers 200 with an empty body; the only observable effect is that deliveries to THAT
     * subscriber stop. (Contrast the subscriber-COUNT cap, which the hub rejects on subscribe with 429/900808.)
     *
     * <p>This assertion is only sound WITH A CONTROL SUBSCRIBER on an unlimited plan: "fewer than N" is equally
     * satisfied by delivery being broken entirely, which is exactly the failure that kept these scenarios parked.
     * The control receiving all N is what makes the shortfall attributable to the quota.
     */
    @Then("The WebSub receiver {string} should have settled below {int} event(s) within {int} seconds")
    public void receiverShouldSettleBelow(String receiverKey, int ceiling, int timeoutSeconds) throws Exception {

        String name = TestContext.resolve(receiverKey).toString();
        Utils.SettledCount count = Utils.awaitSettledCount(DELIVERY_SETTLE_QUIET_MILLIS, timeoutSeconds * 1000L,
                () -> readReceiver(name).getInt("count"));
        String observed = "last seen " + count.value() + " over " + count.samples() + " sample(s), quiet window "
                + DELIVERY_SETTLE_QUIET_MILLIS + "ms";
        Assert.assertTrue(count.settled(), "WebSub receiver '" + name + "' delivery count never stopped changing ("
                + observed + "), so no ceiling can be asserted.");
        Assert.assertTrue(count.value() > 0, "WebSub receiver '" + name + "' received NOTHING (" + observed
                + "). A quota that withholds every event is indistinguishable from broken delivery, so this fails "
                + "rather than passing as 'below the ceiling'.");
        Assert.assertTrue(count.value() < ceiling, "WebSub receiver '" + name + "' settled at " + count.value()
                + ", not below " + ceiling + " (" + observed + ") — the event-count quota did not stop delivery.");
    }

    @Then("The WebSub receiver {string} should have received exactly {int} event(s)")
    public void receiverShouldHaveReceivedExactly(String receiverKey, int expectedCount) throws Exception {

        String name = TestContext.resolve(receiverKey).toString();
        JSONObject state = readReceiver(name);
        Assert.assertEquals(state.getInt("count"), expectedCount, "WebSub receiver '" + name
                + "' delivery count mismatch; state: " + state);
    }

    /**
     * Asserts how many webhook subscriptions the product has PERSISTED for one API, read back from the internal
     * webhooks-subscriptions resource as the acting actor for the acting actor's tenant. Ports the halves of
     * WebSubAPITestCase#testMultipleSubscriptions that v2 could not express — the registration count rising by one
     * per {@code hub.mode=subscribe} and returning to its starting value after the unsubscribes — and carries the
     * lease-expiry assertion (LeaseTimeSubscriptionTestCase), since an expired lease removes the row from this
     * list's own query.
     *
     * <p>Scoped to ONE API's uuid rather than to the whole tenant list (the legacy's {@code initialCount + 3}
     * delta). That is deliberate and strictly stronger: an absolute per-API count cannot be satisfied by another
     * scenario's leftover row, so it is exact under §4 parallelism, whereas a whole-tenant delta silently absorbs
     * a residual row from a scenario that failed before its unsubscribe (see this file's RESIDUE NOTE).
     *
     * <p>TWO-PHASE wait, and both phases are load-bearing (§15). {@link Utils#retryUntil} first waits for the count
     * to REACH the expected value — needed because the count also DECREASES (unsubscribe, lease expiry), which
     * {@link Utils#awaitSettledCount} alone cannot wait for: it returns at the first quiet window, so a value
     * sitting at 1 for a 15-second lease would settle at 1 and never observe the drop to 0. Then
     * {@code awaitSettledCount} confirms the count STAYS there, which is what makes an OVER-count catchable — the
     * exact failure mode a plain accept-on-reached loop cannot see.
     */
    @Then("The internal webhooks subscription list should hold exactly {int} subscription(s) for API {string} within {int} seconds")
    public void internalWebhooksListShouldHold(int expectedCount, String apiIdKey, int timeoutSeconds)
            throws Exception {

        String apiId = TestContext.resolve(apiIdKey).toString();
        String tenantDomain = Identity.actingTenantDomain();
        Integer reached = Utils.retryUntil(timeoutSeconds * 1000L,
                () -> countPersistedSubscriptions(apiId, tenantDomain),
                count -> count == expectedCount);
        // Settle even when the reach loop timed out: the settled value is the number the failure message must
        // report, and a settled-but-wrong count is the over/under-count signal rather than a transient sample.
        Utils.SettledCount settled = Utils.awaitSettledCount(PERSISTED_SETTLE_QUIET_MILLIS,
                PERSISTED_SETTLE_QUIET_MILLIS * 2, () -> countPersistedSubscriptions(apiId, tenantDomain));
        String observed = "reached=" + reached + ", settled=" + settled.value() + " over " + settled.samples()
                + " sample(s), quiet window " + PERSISTED_SETTLE_QUIET_MILLIS + "ms";
        Assert.assertTrue(settled.settled(), "The persisted webhook-subscription count for API " + apiId
                + " in tenant " + tenantDomain + " never stopped changing (" + observed + "), so no exact count "
                + "can be asserted.");
        Assert.assertEquals(settled.value(), expectedCount, "Persisted webhook-subscription count mismatch for API "
                + apiId + " in tenant " + tenantDomain + " (" + observed + "); list: "
                + readPersistedSubscriptions(tenantDomain));
    }

    /** Subscriptions the internal list reports for {@code apiId} in {@code tenantDomain}, right now. */
    private static int countPersistedSubscriptions(String apiId, String tenantDomain) throws IOException {
        JSONArray list = readPersistedSubscriptions(tenantDomain);
        int matches = 0;
        for (int i = 0; i < list.length(); i++) {
            if (apiId.equals(list.getJSONObject(i).optString("apiUUID", null))) {
                matches++;
            }
        }
        return matches;
    }

    /**
     * Asserts ONE exact field of the single webhook subscription the product has PERSISTED for an API, read back
     * from the internal webhooks-subscriptions resource. One field per step (§12) rather than a multi-field
     * composite, so a mismatch names the field that drifted.
     *
     * <p>This is the product's OWN record of a registration — {@code apiUUID}, {@code apiContext},
     * {@code apiVersion}, {@code topicName}, {@code callbackURL}, {@code tier} — and therefore the only surface on
     * which the test's constructed event-receiver URL can be checked against what the hub actually stored. The
     * fan-out's in-memory subscriber map is keyed off exactly these values ({@code apiUUID + "_" + topicName}), so a
     * drift here is a drift in the fan-out key.
     *
     * <p>Asserts there is EXACTLY ONE row for the API first: with several rows "the persisted subscription" is
     * ambiguous, and picking the first would make the field assertion pass or fail for a reason the feature never
     * stated. Use the count step for the multi-registration arcs.
     */
    @Then("The persisted webhook subscription for API {string} should have {string} equal to {string}")
    public void persistedSubscriptionFieldShouldBe(String apiIdKey, String field, String expectedValue)
            throws Exception {

        String apiId = TestContext.resolve(apiIdKey).toString();
        String expected = Utils.resolveContextPlaceholders(expectedValue);
        JSONObject row = theOnlyPersistedSubscription(apiId);
        Assert.assertEquals(row.optString(field, null), expected, "Persisted webhook-subscription field '" + field
                + "' mismatched for API " + apiId + "; row: " + row);
    }

    /**
     * Asserts the event-receiver URL an event SOURCE posts to is the very URL the product's own persisted
     * registration describes — the check that was an unstated ASSUMPTION until now. The test constructs that URL
     * from the API context it captured plus {@code Constants.WEBSUB_EVENT_RECEIVER_RESOURCE} and {@code ?topic=};
     * this step rebuilds it a second time from the PERSISTED row's {@code apiContext}/{@code apiVersion}/
     * {@code topicName} and requires the two to be byte-identical.
     *
     * <p>Why it matters: the shipped {@code websub_api_template.xml} answers 200 to the source once
     * {@code TOPIC_VALIDITY} passes, and fans out to however many subscribers the in-memory map yields — zero
     * included, with no error. So a post that reached a DIFFERENT context/topic than the one registered would look
     * exactly like a successful publish with no subscribers. Comparing the two URLs is what tells those apart, and
     * it settles by construction whether the registered {@code hub.topic} string differs from the source's
     * {@code ?topic=} string.
     */
    @Then("The event receiver URL for gateway context {string} topic {string} should match the persisted webhook subscription of API {string}")
    public void persistedSubscriptionShouldDescribeReceiverUrl(String context, String topic, String apiIdKey)
            throws Exception {

        String apiId = TestContext.resolve(apiIdKey).toString();
        String resolvedTopic = Utils.resolveContextPlaceholders(topic);
        String posted = eventReceiverUrl(context, resolvedTopic);

        JSONObject row = theOnlyPersistedSubscription(apiId);
        // MEASURED: the persisted apiContext ALREADY ends in the version (e.g. "/myctx_ab12/1.0.0") — the hub stores
        // the versioned synapse context, not the bare one — while apiVersion repeats it separately. Appending
        // apiVersion here produced a "/1.0.0/1.0.0/" URL and a mismatch that was the step's bug, not the product's.
        String persistedContext = row.optString("apiContext", null);
        String persistedVersion = row.optString("apiVersion", null);
        String persistedTopic = row.optString("topicName", null);
        Assert.assertNotNull(persistedContext, "The persisted webhook subscription carries no apiContext; row: " + row);
        Assert.assertNotNull(persistedVersion, "The persisted webhook subscription carries no apiVersion; row: " + row);
        Assert.assertNotNull(persistedTopic, "The persisted webhook subscription carries no topicName; row: " + row);
        Assert.assertTrue(persistedContext.endsWith("/" + persistedVersion), "The persisted apiContext ("
                + persistedContext + ") does not end in the persisted apiVersion (" + persistedVersion + "), so the "
                + "receiver URL cannot be rebuilt from the context alone; row: " + row);

        String described = eventReceiverUrl(persistedContext, persistedTopic);
        Assert.assertEquals(posted, described, "The event source posts to a URL the product's own registration does "
                + "not describe — so a 200 from the receiver cannot mean the published event entered fan-out. "
                + "posted=" + posted + " persisted-row=" + row);
        // Both URLs on a green run too: "they matched" is only interpretable next to what they matched AS, and this
        // is the check the fan-out park note's wrong-listener hypothesis turns on.
        log.info("WebSub event-receiver URL agreed with the product's own registration: posted=" + posted
                + " described-by-persisted-row=" + described + " row=" + row);
    }

    /** The single persisted webhook-subscription row for {@code apiId}, failing clearly on 0 or 2+ rows. */
    private static JSONObject theOnlyPersistedSubscription(String apiId) throws Exception {
        String tenantDomain = Identity.actingTenantDomain();
        // The registration is persisted asynchronously (hub -> /internal/data/v1/notify -> DAO), so wait for the
        // row rather than reading once and racing it.
        Integer reached = Utils.retryUntil(Constants.RUNTIME_PROPAGATION_TIMEOUT,
                () -> countPersistedSubscriptions(apiId, tenantDomain), count -> count == 1);
        JSONArray list = readPersistedSubscriptions(tenantDomain);
        JSONArray matches = new JSONArray();
        for (int i = 0; i < list.length(); i++) {
            if (apiId.equals(list.getJSONObject(i).optString("apiUUID", null))) {
                matches.put(list.getJSONObject(i));
            }
        }
        Assert.assertEquals(matches.length(), 1, "Expected EXACTLY ONE persisted webhook subscription for API "
                + apiId + " in tenant " + tenantDomain + " (reached=" + reached + "), so that \"the persisted "
                + "subscription\" is unambiguous; matched rows: " + matches);
        return matches.getJSONObject(0);
    }

    /**
     * The tenant's persisted webhook-subscription array. An intermediate read consumed inside the step, so it uses
     * {@link SimpleHTTPClient} directly and must NOT publish {@code httpResponse} (§7) — the preceding hub request
     * left its response there for the feature's own status assertion.
     */
    private static JSONArray readPersistedSubscriptions(String tenantDomain) throws IOException {
        String url = Utils.getBaseUrl() + INTERNAL_WEBHOOKS_SUBSCRIPTIONS_PATH;
        Map<String, String> headers = new HashMap<>(Identity.actingBasicAuthHeaders());
        headers.put(TENANT_HEADER, tenantDomain);
        HttpResponse response = SimpleHTTPClient.getInstance().doGet(url, headers);
        Assert.assertTrue(response != null && response.getResponseCode() == 200 && response.getData() != null
                        && !response.getData().isBlank(),
                "Failed to read the internal webhooks-subscriptions list at " + url + " for tenant " + tenantDomain
                        + "; got=" + (response == null ? "null" : response.getResponseCode() + "/"
                        + response.getData()));
        // An empty tenant may serialise without the array at all, which is not an error — it is a count of zero.
        JSONArray list = new JSONObject(response.getData()).optJSONArray("list");
        return list != null ? list : new JSONArray();
    }

    /**
     * Waits for the hub's VERIFICATION handshake at the callback and asserts its mandatory parameters: the exact
     * {@code hub.mode} and {@code hub.topic} echoed to the subscriber, plus a NON-EMPTY {@code hub.challenge}
     * (its value is random per handshake, so only its presence can be pinned). Ports
     * WebSubAPITestCase#testMandatoryParameters.
     */
    @Then("The WebSub receiver {string} should have recorded a {string} verification for topic {string} with a non-empty challenge within {int} seconds")
    public void receiverShouldHaveRecordedVerification(String receiverKey, String expectedMode, String expectedTopic,
                                                       int timeoutSeconds) throws Exception {

        String name = TestContext.resolve(receiverKey).toString();
        String topic = Utils.resolveContextPlaceholders(expectedTopic);
        JSONObject last = Utils.retryUntil(timeoutSeconds * 1000L, () -> readReceiver(name),
                state -> hasVerification(state, expectedMode));
        Assert.assertNotNull(last, "Could not read the WebSub receiver '" + name + "' introspection state at all.");
        Assert.assertTrue(hasVerification(last, expectedMode), "WebSub receiver '" + name + "' never received a '"
                + expectedMode + "' verification handshake; state: " + last);

        JSONObject verification = lastVerification(last, expectedMode);
        Assert.assertEquals(verification.optString("mode", null), expectedMode,
                "hub.mode at the callback mismatched; verification: " + verification);
        Assert.assertEquals(verification.optString("topic", null), topic,
                "hub.topic at the callback mismatched; verification: " + verification);
        String challenge = verification.optString("challenge", "");
        Assert.assertFalse(challenge.isBlank(),
                "The hub did not send a hub.challenge to the callback; verification: " + verification);
    }

    private static boolean hasVerification(JSONObject state, String mode) {
        return lastVerification(state, mode) != null;
    }

    private static JSONObject lastVerification(JSONObject state, String mode) {
        JSONArray verifications = state.getJSONArray("verifications");
        for (int i = verifications.length() - 1; i >= 0; i--) {
            JSONObject verification = verifications.getJSONObject(i);
            if (mode.equals(verification.optString("mode", null))) {
                return verification;
            }
        }
        return null;
    }

    /**
     * Asserts the LAST delivery's body is byte-identical to what was published AND that its
     * {@code x-hub-signature} is exactly the HMAC the SUBSCRIBER's {@code hub.secret} produces over that body —
     * i.e. the hub re-signs the fan-out with each subscriber's own secret, not with the API's. That distinction is
     * the whole subject of SecretValidationTestCase, so both secrets differ in the scenario that uses this.
     */
    @Then("The last WebSub event delivered to receiver {string} should have body {string} signed with {string} using secret {string}")
    public void lastDeliveryShouldBeSignedWith(String receiverKey, String expectedBody, String signingAlgorithm,
                                               String secret) throws Exception {

        String name = TestContext.resolve(receiverKey).toString();
        String body = TestContext.resolve(expectedBody).toString();
        String expectedSignature = HmacTestUtils.hubSignature(signingAlgorithm, body,
                Utils.resolveContextPlaceholders(secret));

        JSONObject delivery = lastDelivery(name);
        Assert.assertEquals(delivery.optString("body", null), body,
                "The delivered body differed from the published content; delivery: " + delivery);
        Assert.assertEquals(delivery.optString("signature", null), expectedSignature,
                "The delivered " + signingAlgorithm + " x-hub-signature did not verify against the subscriber's "
                        + "hub.secret; delivery: " + delivery);
    }

    /**
     * Asserts a named HEADER on the last delivery contains the given substring — the hub self-advertisement check
     * (WebSubAPITestCase asserts the content-distribution request's {@code link} header points back at the hub).
     * A substring, not an exact value, because the hub embeds its own resolved host in that URL.
     */
    @Then("The last WebSub event delivered to receiver {string} should carry a {string} header containing {string}")
    public void lastDeliveryHeaderShouldContain(String receiverKey, String headerName, String expectedFragment)
            throws Exception {

        String name = TestContext.resolve(receiverKey).toString();
        String fragment = Utils.resolveContextPlaceholders(expectedFragment);
        JSONObject delivery = lastDelivery(name);
        // Node lower-cases incoming header names, so look the header up in that form.
        String headerValue = delivery.getJSONObject("headers").optString(headerName.toLowerCase(java.util.Locale.ROOT),
                null);
        Assert.assertNotNull(headerValue, "The delivery carried no '" + headerName + "' header; delivery: " + delivery);
        Assert.assertTrue(headerValue.contains(fragment), "The '" + headerName + "' header was [" + headerValue
                + "] which does not contain [" + fragment + "]");
    }

    /** The most recent recorded delivery at the named receiver, failing clearly when none was recorded. */
    private static JSONObject lastDelivery(String name) throws IOException {
        JSONObject state = readReceiver(name);
        JSONArray events = state.getJSONArray("events");
        Assert.assertTrue(events.length() > 0,
                "WebSub receiver '" + name + "' recorded no deliveries at all; state: " + state);
        return events.getJSONObject(events.length() - 1);
    }

    /**
     * Reads the named receiver's recorded state from the node app's HOST-published introspection endpoint. An
     * intermediate read consumed entirely within the step, so it goes through {@link SimpleHTTPClient} directly
     * and must NOT publish {@code httpResponse} (§7) — otherwise it would clobber the response the preceding
     * request step left for the feature's own assertion.
     */
    private static JSONObject readReceiver(String name) throws IOException {
        String url = Utils.getNodeBackendUrl(WEBSUB_RECEIVER_PORT) + "/events/" + name;
        HttpResponse response = SimpleHTTPClient.getInstance().doGet(url, new HashMap<>());
        Assert.assertTrue(response != null && response.getResponseCode() == 200 && response.getData() != null
                        && !response.getData().isBlank(),
                "Failed to read the WebSub receiver introspection at " + url + "; got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));
        return new JSONObject(response.getData());
    }
}
