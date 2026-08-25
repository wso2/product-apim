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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

/**
 * The single READER for the node backend's SMTP capture sink (see
 * {@code tests-common/testcontainers/.../nodeapps/duplicate-header-backend/controllers/mailSinkController.js}) —
 * the replacement for the legacy NotificationTestCase's in-JVM GreenMail server.
 *
 * <p>The sink accepts APIM's outbound notification mail on {@code nodebackend:3025} (the send is made from inside
 * the APIM container, so it cannot be captured in the test JVM) and exposes what it captured over the HTTP port
 * that app already publishes. This class is the one reader for that state, matching the one-reader-per-nodeapp-
 * double rule the WebSub receiver established.
 *
 * <p>IT ASSERTS NOTHING ABOUT THE MESSAGE. The step waits and then PUBLISHES the sink's response as
 * {@code httpResponse}, so every claim about the captured mail — cardinality, recipient, subject, body — is made
 * by the existing generic assertion steps in the feature. That is deliberate: it keeps the added glue to this one
 * step instead of a family of mail-specific assertion steps.
 *
 * <p>NO RESET STEP EXISTS AND NONE IS NEEDED. Every scenario mints a UNIQUE recipient address
 * ({@code utils/Names.unique}), so a message inherited from an earlier scenario is impossible by construction —
 * the same argument documented on the WebSub receiver. The sink's clear endpoint exists for a future consumer that
 * reuses an address; this family must not, because a shared address would also let two parallel scenarios read
 * each other's mail.
 */
public class NotificationMailSteps {

    private static final Log log = LogFactory.getLog(NotificationMailSteps.class);

    /**
     * Container port of the mail sink's HTTP introspection API — the {@code duplicate-header-backend} app's own
     * port, which is already published to the host ({@code NodeAppServer#exposedPorts}). The SMTP listener sits on
     * 3025 and is intentionally NOT published: only the APIM container talks to it.
     */
    private static final int MAIL_SINK_HTTP_PORT = 3005;

    /**
     * How long a mailbox's message count must stay UNCHANGED before it is taken as final (see
     * {@link Utils#awaitSettledCount}). The notifier publishes one message per old published version in a tight
     * loop through a single output-event adapter, so two messages of one trigger would arrive within a second of
     * each other; 10s leaves an order of magnitude of headroom. Raise it if a count ever settles LOW.
     */
    private static final long MAIL_SETTLE_QUIET_MILLIS = 10_000L;

    /**
     * Waits until the sink's mailbox for {@code recipient} holds exactly {@code expectedCount} messages and has
     * STOPPED changing, then publishes the mailbox as {@code httpResponse} for the feature's field assertions.
     *
     * <p>Two phases, because neither alone is sound. {@link Utils#retryUntil} waits for ARRIVAL (mail is sent
     * asynchronously from a background adapter thread, so the trigger returns long before the message lands) but
     * its accept condition can only say "has it reached N", which passes the instant the count touches N and
     * leaves a duplicate arriving moments later invisible. {@link Utils#awaitSettledCount} catches that
     * over-count, but on its own it would settle at 0 during the delivery delay and fail a perfectly good
     * scenario. Arrival first, then settle, is the only order that both waits long enough and can observe an
     * over-count (§12).
     *
     * @param recipient      the recipient address whose mailbox to read (may carry {@code {{contextKey}}} refs)
     * @param expectedCount  the exact number of captured messages expected
     * @param timeoutSeconds arrival/settle window floor (raised to the shared propagation ceiling)
     */
    @Then("The captured mailbox of {string} should settle at exactly {int} message(s) within {int} seconds")
    public void theCapturedMailboxShouldSettleAt(String recipient, int expectedCount, int timeoutSeconds)
            throws IOException, InterruptedException {

        String address = Utils.resolveContextPlaceholders(recipient).trim().toLowerCase(Locale.ROOT);
        String url = Utils.getNodeBackendUrl(MAIL_SINK_HTTP_PORT) + "/mail/inbox?recipient="
                + Utils.urlEncode(address);

        Integer arrived = Utils.retryUntil(timeoutSeconds * 1000L, () -> readMessageCount(url),
                count -> count >= expectedCount);
        Assert.assertNotNull(arrived, "The mail sink at " + url + " never answered, so nothing can be said about "
                + "the notification sent to '" + address + "'.");
        Assert.assertTrue(arrived >= expectedCount, "The mail sink captured " + arrived + " message(s) for '"
                + address + "' within " + timeoutSeconds + "s but at least " + expectedCount + " were expected. A "
                + "count of 0 means the product never delivered to nodebackend:3025 — check the block's "
                + "[output_adapter.email] overlay and the subscriber's emailaddress claim.");

        Utils.SettledCount settled = Utils.awaitSettledCount(MAIL_SETTLE_QUIET_MILLIS, timeoutSeconds * 1000L,
                () -> readMessageCount(url));
        Assert.assertTrue(settled.settled(), "The mailbox of '" + address + "' never stopped receiving (last seen "
                + settled.value() + " over " + settled.samples() + " sample(s), quiet window "
                + MAIL_SETTLE_QUIET_MILLIS + "ms), so its message count cannot be called final.");
        Assert.assertEquals(settled.value(), expectedCount, "The mailbox of '" + address + "' settled at "
                + settled.value() + " message(s), not the expected " + expectedCount + ".");

        // Publish the mailbox so the feature asserts the captured message's fields with the generic steps.
        HttpResponse response = Requests.get(url, new HashMap<>());
        Assert.assertEquals(response.getResponseCode(), 200,
                "Reading the settled mailbox of '" + address + "' failed: " + response.getData());
        // Log what actually arrived. The field assertions that follow only report the body when they FAIL, so
        // without this a green run leaves no record of the message the product sent — the exact thing a later
        // "did the template change?" question needs.
        log.info("mail-sink read for '" + address + "': " + response.getData());
    }

    /**
     * Number of messages the sink holds for {@code url}'s recipient; 0 when the sink has never seen the address
     * (it 404s an unknown mailbox rather than inventing an empty one). Consumed inside this step, so it uses the
     * raw client and does NOT touch {@code httpResponse} (§7).
     */
    private static int readMessageCount(String url) throws IOException {

        HttpResponse response = SimpleHTTPClient.getInstance().doGet(url, new HashMap<>());
        if (response == null) {
            throw new IOException("No response from the mail sink at " + url);
        }
        if (response.getResponseCode() == 404) {
            return 0;
        }
        Assert.assertEquals(response.getResponseCode(), 200,
                "Unexpected status from the mail sink at " + url + ": " + response.getData());
        String body = response.getData();
        Assert.assertTrue(body != null && !body.trim().isEmpty(),
                "The mail sink at " + url + " returned an empty response body: " + body);
        return new JSONObject(body).getInt("count");
    }
}
