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

import io.cucumber.java.en.When;
import org.testng.Assert;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.testcontainers.DynamicSolaceBroker;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Publisher-plane steps for the Solace Event-Portal integration.
 *
 * <p>These two endpoints are APIM's own proxy over the Solace API: the Publisher UI calls them while a user
 * walks "Create API -&gt; AsyncAPI -&gt; Solace", and APIM answers by calling Solace's
 * {@code /eventApiProducts} and {@code /eventApiProducts/{id}/plans/{planId}/eventApis/{apiId}} through
 * {@code SolaceV2Apis}. So exercising them exercises the APIM-to-Solace hop, which is the point.
 *
 * <p>They are here rather than in {@link PublisherBaseSteps} because they are the only vendor-specific
 * publisher endpoints in the suite; nothing else needs them, and folding two Solace-shaped URLs into the
 * generic resource steps would widen those for a single caller.
 */
public class SolaceSteps {

    /** APIM's own path for listing third-party integrated APIs, keyed by vendor. */
    private static final String INTEGRATED_APIS_SOLACE = "apis/integrated-apis/solace";

    private static Map<String, String> publisherHeaders() {

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());
        return headers;
    }

    /**
     * Lists the Event API Products APIM can see, which makes APIM call Solace's {@code /eventApiProducts}
     * and map the result into {@code IntegratedSolaceApisResponse} (apiId / apiName / plans).
     */
    @When("I retrieve the integrated Solace APIs")
    public void iRetrieveTheIntegratedSolaceApis() throws IOException {

        Requests.get(Utils.getBaseUrl() + Constants.DEFAULT_APIM_API_DEPLOYER + INTEGRATED_APIS_SOLACE, publisherHeaders());
    }

    /**
     * Fetches the AsyncAPI document for one product/plan/event-API triple through APIM.
     *
     * <p>The triple travels as a JSON object in a single {@code params} query parameter — APIM's shape, not
     * Solace's — and APIM then calls
     * {@code /eventApiProducts/{productId}/plans/{planId}/eventApis/{eventApiId}?asyncApiVersion=2.2.0}.
     * The document comes back VERBATIM (SolaceV2Apis returns the client's JsonObject with no unwrapping),
     * which is why a scenario can assert the {@code x-ep-*} extensions survive the round trip.
     */
    @When("I retrieve the integrated Solace API definition for product {string} plan {string} api {string}")
    public void iRetrieveTheIntegratedSolaceApiDefinition(String productId, String planId, String eventApiId)
            throws IOException {

        String params = "{\"eventApiProductId\":\"" + productId + "\",\"planId\":\"" + planId
                + "\",\"eventApiId\":\"" + eventApiId + "\"}";
        String url = Utils.getBaseUrl() + Constants.DEFAULT_APIM_API_DEPLOYER + INTEGRATED_APIS_SOLACE + "/definition?params="
                + Utils.urlEncode(params);
        Requests.get(url, publisherHeaders());
    }

    /**
     * Reads a client username straight off the BROKER via SEMP, so a scenario can assert that a credential
     * APIM pushed really landed there.
     *
     * <p>This exists because the APIM status code alone is not evidence, which had to be measured rather than
     * assumed. Mutation runs showed key generation returns 200 BOTH when the notifier never calls Solace at
     * all (an application with no Solace subscription) AND when Solace answers 500 to the credential push —
     * the notifier swallows both. So a scenario asserting only "200" cannot fail for any Solace-side reason.
     * SEMP is the independent witness, and it is the assertion channel {@link DynamicSolaceBroker} mandates
     * (never the shim's own responses, which are satisfied by construction).
     *
     * <p>MEASURED: SEMP answers 200 for a provisioned client username and 400 once it is absent — so the
     * feature asserts 200 here, and the mutation that removes the push turns it into a 400.
     *
     * <p>No polling: the shim AWAITS the SEMP provisioning before answering the credential push, so by the
     * time key generation has returned the username already exists. If that await is ever removed this read
     * becomes racy.
     */
    @When("I retrieve the Solace broker client username for consumer key {string}")
    public void iRetrieveBrokerClientUsername(String consumerKey) throws IOException {

        String url = DynamicSolaceBroker.getInstance().getSempUrl() + "/config/msgVpns/"
                + DynamicSolaceBroker.SEMP_VPN + "/clientUsernames/"
                + Utils.urlEncode(Utils.resolveContextPlaceholders(consumerKey));
        Requests.get(url, Identity.basicAuthHeaders(DynamicSolaceBroker.SEMP_USER, DynamicSolaceBroker.SEMP_PASSWORD));
    }

    /*
     * ---- Data plane: publishing to the REAL broker with the token APIM issued ------------------------
     *
     * This is the only step in the Solace feature that touches the broker rather than the control-plane
     * double, and the only one whose outcome nothing on this project authored: SolOS decides. Two independent
     * links must both hold for a 200, which is what makes it a claim about the arc rather than a status check:
     *   1. the broker fetches APIM's JWKS and verifies the access token's signature, expiry and type;
     *   2. the client username it resolves from the token's client_id claim must have been PROVISIONED, which
     *      only happens via APIM key generation -> SolaceKeyGenNotifier ->
     *      POST /appRegistrations/{app}/credentials -> the connector's SEMP clientUsername.
     * Break either link and the publish is rejected. Both are mutation-proven against a live broker; see the
     * shim's configureApimOAuth for the provision/deprovision matrix.
     *
     * Publishing over REST (POST /TOPIC/<topic>) rather than smf/mqtt is deliberate: it needs no messaging
     * client, so it rides Requests/SimpleHTTPClient like every other call in the suite. Note the credential
     * form differs from Solace's smf/mqtt tutorials — see DynamicSolaceBroker.REST_PORT.
     *
     * SCOPE. These rows prove BOTH halves: the broker authenticates an APIM-issued token belonging to a
     * provisioned application, AND it enforces the topics that application's subscription actually granted --
     * the connector double turns an access request's permissions into an ACL profile (default publish action
     * disallow plus an exception per granted topic) attached to the client username. What they still do NOT
     * cover is subscribe-side enforcement (only publishing is exercised) and protocol translation for
     * smf/mqtt/amqp, since every publish here goes over REST.
     */
    private static String brokerTopicUrl(String topic) {

        return DynamicSolaceBroker.getInstance().getRestMessagingUrl() + "/TOPIC/" + topic;
    }

    /** Sample event body. Content is irrelevant to every assertion here; only the credential's fate is. */
    private static final String EVENT_PAYLOAD = "{\"orderId\":\"o-1\",\"amount\":42}";

    /*
     * ---- Subscribe side: MQTT, because REST cannot receive -----------------------------------------------
     *
     * Publishing rides REST (a plain authenticated POST), but Solace's REST messaging is send-only for clients
     * -- there is no REST consume -- so the SUBSCRIBE half of an access request's permissions cannot be
     * exercised over HTTP at all. MQTT is the cheapest real messaging protocol here and is the one Solace's own
     * "invoke your event API" walkthrough uses.
     *
     * WHY THE PROTOCOL IS HAND-ROLLED rather than pulling in a client library: the only MQTT client in the
     * local repository is org.eclipse.paho:mqtt-client:0.4.0, a decade-old artifact, and this module
     * deliberately avoids new transport dependencies (WebSocketInvocationSteps says so explicitly and uses the
     * JDK's own WebSocket). Everything needed here is four MQTT 3.1.1 control packets, and the assertion is a
     * single return code, so a socket plus ~40 lines of framing is both smaller and more transparent than a
     * dependency. This is NOT a general MQTT client and should not grow into one: no QoS>0, no keepalive, no
     * reconnect. If a scenario ever needs real message flow, reach for a library then.
     *
     * WHAT THE RETURN CODES MEAN, and why SUBACK is the assertion rather than a delivered message: the broker
     * answers a SUBSCRIBE with a per-filter code -- 0x00/0x01/0x02 grant the requested QoS, 0x80 is failure.
     * An ACL denial surfaces there, so subscribe-side enforcement is observable WITHOUT publishing anything
     * and without waiting on delivery (which would need a settling window and could flake).
     */
    private static final int MQTT_CONNECT = 0x10;
    private static final int MQTT_CONNACK = 0x20;
    private static final int MQTT_SUBSCRIBE = 0x82;
    private static final int MQTT_SUBACK = 0x90;
    private static final int MQTT_SUBACK_FAILURE = 0x80;
    private static final int MQTT_SOCKET_TIMEOUT_MS = 15000;

    /** MQTT's length-prefixed UTF-8 string: two big-endian length bytes then the bytes. */
    private static void writeMqttString(java.io.ByteArrayOutputStream out, String value) {

        byte[] bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        out.write(bytes.length >> 8);
        out.write(bytes.length & 0xFF);
        out.write(bytes, 0, bytes.length);
    }

    /** MQTT's variable-length integer encoding of a packet's remaining length. */
    private static void writeRemainingLength(java.io.OutputStream out, int length) throws IOException {

        int remaining = length;
        do {
            int digit = remaining % 128;
            remaining /= 128;
            out.write(remaining > 0 ? (digit | 0x80) : digit);
        } while (remaining > 0);
    }

    /** Reads a packet's remaining-length field, which is what says how many bytes follow. */
    private static int readRemainingLength(java.io.InputStream in) throws IOException {

        int multiplier = 1;
        int value = 0;
        int encoded;
        do {
            encoded = in.read();
            if (encoded < 0) {
                throw new IOException("connection closed while reading an MQTT remaining-length");
            }
            value += (encoded & 0x7F) * multiplier;
            multiplier *= 128;
        } while ((encoded & 0x80) != 0);
        return value;
    }

    private static int readOrFail(java.io.InputStream in, String what) throws IOException {

        int b = in.read();
        if (b < 0) {
            throw new IOException("connection closed while reading " + what);
        }
        return b;
    }

    /**
     * Connects over MQTT with an APIM-issued OAuth token and subscribes to one topic, returning the broker's
     * SUBACK code for that filter.
     *
     * <p>The token travels as the PASSWORD in Solace's {@code OAUTH~<profile>~<token>} form and the username is
     * ignored — the credential shape Solace's tutorial documents for smf/mqtt, and the one REST rejects (see
     * {@link DynamicSolaceBroker#REST_PORT}). So this also covers the credential form the publish rows cannot.
     */
    private static int mqttSubscribe(String topic, String token) throws IOException {

        DynamicSolaceBroker broker = DynamicSolaceBroker.getInstance();
        try (java.net.Socket socket = new java.net.Socket("localhost", broker.getMappedMqttPort())) {
            socket.setSoTimeout(MQTT_SOCKET_TIMEOUT_MS);
            java.io.OutputStream out = socket.getOutputStream();
            java.io.InputStream in = socket.getInputStream();

            // ---- CONNECT: clean session, username+password present (flags 0xC2), keep-alive 60s.
            java.io.ByteArrayOutputStream body = new java.io.ByteArrayOutputStream();
            writeMqttString(body, "MQTT");
            body.write(0x04);                       // protocol level 4 == MQTT 3.1.1
            body.write(0xC2);                       // username | password | clean-session
            body.write(0x00);
            body.write(0x3C);                       // keep-alive 60s
            writeMqttString(body, "acl-probe-" + System.nanoTime());
            writeMqttString(body, "ignored");       // username: Solace ignores it under OAuth
            writeMqttString(body, "OAUTH~" + DynamicSolaceBroker.OAUTH_PROFILE_NAME + "~" + token);
            out.write(MQTT_CONNECT);
            writeRemainingLength(out, body.size());
            body.writeTo(out);
            out.flush();

            int connackType = readOrFail(in, "CONNACK type");
            readRemainingLength(in);
            readOrFail(in, "CONNACK session-present");
            int connackCode = readOrFail(in, "CONNACK return code");
            Assert.assertEquals(connackType & 0xF0, MQTT_CONNACK,
                    "Expected a CONNACK from the Solace broker but got packet type 0x"
                            + Integer.toHexString(connackType));
            // Fail HERE rather than letting a rejected connection masquerade as a denied subscription: a
            // non-zero CONNACK is an AUTHENTICATION problem (bad/expired token, OAuth profile misconfigured),
            // which is a different finding from the ACL denial these scenarios are about.
            Assert.assertEquals(connackCode, 0, "MQTT connection was refused with CONNACK code " + connackCode
                    + " — the token was not accepted, so no conclusion can be drawn about topic authorisation."
                    + " Check the broker's event.log SYSTEM_CLIENT_CONNECT_AUTH_FAIL line.");

            // ---- SUBSCRIBE: one filter at QoS 0, packet id 1.
            java.io.ByteArrayOutputStream sub = new java.io.ByteArrayOutputStream();
            sub.write(0x00);
            sub.write(0x01);                        // packet identifier
            writeMqttString(sub, topic);
            sub.write(0x00);                        // requested QoS
            out.write(MQTT_SUBSCRIBE);
            writeRemainingLength(out, sub.size());
            sub.writeTo(out);
            out.flush();

            int subackType = readOrFail(in, "SUBACK type");
            int subackRemaining = readRemainingLength(in);
            for (int i = 0; i < subackRemaining - 1; i++) {
                readOrFail(in, "SUBACK header");     // packet identifier
            }
            int subackCode = readOrFail(in, "SUBACK return code");
            Assert.assertEquals(subackType & 0xF0, MQTT_SUBACK,
                    "Expected a SUBACK but got packet type 0x" + Integer.toHexString(subackType));
            return subackCode;
        }
    }

    /**
     * Subscribes over MQTT and asserts whether the broker GRANTED or DENIED the topic — the subscribe half of
     * what an access request's permissions authorise, which no REST call can reach.
     *
     * <p>ONE step for both outcomes, so the feature shows which topic produced which verdict. {@code granted}
     * means a QoS code (0x00-0x02) and {@code denied} means 0x80; anything else fails loudly rather than being
     * bucketed into one of them.
     */
    @When("I subscribe over MQTT to the Solace topic {string} with OAuth token {string} expecting {string}")
    public void iSubscribeOverMqtt(String topic, String token, String expected) throws IOException {

        int code = mqttSubscribe(topic, Utils.resolveContextPlaceholders(token));
        boolean denied = code == MQTT_SUBACK_FAILURE;
        boolean granted = code >= 0x00 && code <= 0x02;
        Assert.assertTrue(denied || granted, "Unexpected MQTT SUBACK code 0x" + Integer.toHexString(code)
                + " for topic " + topic + " — neither a granted QoS (0x00-0x02) nor a failure (0x80)");

        if ("granted".equals(expected)) {
            Assert.assertTrue(granted, "Expected the broker to GRANT a subscription to " + topic
                    + " but it returned SUBACK 0x" + Integer.toHexString(code) + " (failure). The access"
                    + " request's subscribe permissions should have become an ACL subscribe-topic exception.");
        } else if ("denied".equals(expected)) {
            Assert.assertTrue(denied, "Expected the broker to DENY a subscription to " + topic
                    + " but it granted QoS " + code + ". The ACL profile's subscribeTopicDefaultAction should"
                    + " be disallow, with an exception only for the granted topics.");
        } else {
            Assert.fail("Unknown expectation '" + expected + "' — use \"granted\" or \"denied\"");
        }
    }

    /**
     * Publishes to a broker topic with the given OAuth token — ONE step for both the accepted and the rejected
     * case, because they differ only in the token passed and the status awaited.
     *
     * <p>The token goes through {@link Utils#resolveContextPlaceholders(String)}, so a feature may pass either a
     * {@code {{contextKey}}} reference (e.g. {@code {{generatedAccessToken}}}, which the token-request step
     * stores) or a literal. That keeps the falsified value VISIBLE IN THE FEATURE rather than buried in Java: a
     * reader sees exactly what was wrong with the credential and what status it produced, and a new case needs
     * no glue change. It also lets the negative row stay self-contained — a literal bad token needs no API,
     * application, subscription or keys.
     *
     * <p>Retrying is not optional for the positive: the broker fetches APIM's JWKS LAZILY, on the first token it
     * validates, so the very first publish of a run races that fetch. Funnelled through
     * {@link Utils#retryUntil} per §7/§15 — the response is this step's assertion target, so the envelope
     * returns the last result and the assertion below is made here rather than deferred to a following
     * {@code Then}. The negative costs nothing extra: its expected status arrives on the first attempt, so
     * {@code accept} holds immediately and no polling happens.
     */
    @When("I publish an event to the Solace topic {string} with OAuth token {string} until the response status "
            + "code becomes {int} within {int} seconds")
    public void iPublishToSolaceTopic(String topic, String token, int expectedStatus, int timeoutSeconds)
            throws InterruptedException {

        String url = brokerTopicUrl(topic);
        Map<String, String> headers = Identity.bearerHeaders(Utils.resolveContextPlaceholders(token));

        HttpResponse response = Utils.retryUntil(timeoutSeconds * 1000L,
                () -> Requests.post(url, headers, EVENT_PAYLOAD, Constants.CONTENT_TYPES.APPLICATION_JSON),
                r -> r != null && r.getResponseCode() == expectedStatus);

        Assert.assertNotNull(response, "Publishing to Solace topic " + topic + " never returned a response");
        // The broker's HTTP body carries only a generic reason; the ACTUAL cause of a rejection is in the
        // broker's /usr/sw/jail/logs/event.log (SYSTEM_CLIENT_CONNECT_AUTH_FAIL). Say so, so a failure here
        // does not send the next reader hunting through APIM's logs, where the cause never is.
        Assert.assertEquals(response.getResponseCode(), expectedStatus,
                "Publishing to Solace topic " + topic + " returned " + response.getResponseCode()
                        + " (expected " + expectedStatus + "). Body: " + response.getData()
                        + ". For the real reason read the broker's event.log SYSTEM_CLIENT_CONNECT_AUTH_FAIL "
                        + "line, and check the shim logged 'broker configured as OAuth resource server'.");
    }
}
