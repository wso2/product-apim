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

import com.sun.net.httpserver.HttpServer;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.am.testcontainers.DynamicApimContainer;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Remote-server-logging glue (ports RemoteLoggingAppenderTest). Configures the Carbon
 * {@code RemoteLoggingConfig} Axis2 admin service over hand-rolled SOAP (the same approach the
 * user-store/org-claim provisioning uses — there is no REST equivalent) to redirect a log type's appender to
 * a remote HTTP endpoint, and asserts the effect two ways: (1) the {@code log4j2.properties} appender flips
 * type (RollingFile ⇄ SecuredHttp), read straight from the running container; (2) end-to-end, an audit action's
 * log entry is delivered to a host mock sink the container reaches via {@code host.docker.internal}.
 *
 * <p>It also covers how the service MANAGES the appender blocks in that file: a scenario seeds a
 * {@code log4j2.properties} fixture (one of {@code artifacts/configFiles/remoteLogging/*}, e.g. with the
 * AUDIT_LOGFILE block stripped out) into the running container, drives the service, and asserts what the
 * server wrote back — a missing block is created as a remote appender and registered in the top-level
 * {@code appenders} list, an existing block is not duplicated, and appenders for log types WITHOUT remote
 * logging are left alone.</p>
 *
 * <p>Remote logging is a super-tenant, server-global setting (not per-tenant), so these scenarios run once as
 * the super-tenant admin, in a dedicated thread-count=1 block (they mutate the shared server's log config).</p>
 */
public class RemoteLoggingSteps {

    /* Axis2 namespaces from the RemoteLoggingConfig service WSDL. */
    private static final String OP_NS = "http://org.apache.axis2/xsd";
    private static final String DATA_NS = "http://data.service.logging.carbon.wso2.org/xsd";
    private static final String SERVICE_PATH = "services/RemoteLoggingConfig";

    /** Classpath directory holding the log4j2.properties fixtures the appender-management scenarios seed. */
    private static final String LOG4J2_FIXTURE_DIR = "artifacts/configFiles/remoteLogging/";

    /** Granularity for every poll/settle loop below — the server's log4j2 rewrite is asynchronous. */
    private static final long POLL_INTERVAL_MILLIS = 2000L;

    private static final Log log = LogFactory.getLog(RemoteLoggingSteps.class);

    /* Mock HTTP sink shared across steps + the teardown hook. */
    private static HttpServer sinkServer;
    private static final List<String> sinkPayloads = new CopyOnWriteArrayList<>();

    /* Log types enabled via addRemoteServerConfig, reset in teardown — failure-safe, since the inline "disable"
       step is skipped if a scenario fails after enabling. */
    private static final List<String> enabledLogTypes = new CopyOnWriteArrayList<>();

    /* The block container's untouched log4j2.properties, captured before the first scenario and written back
       after every one, so a scenario that seeds a stripped fixture cannot leak that config into the next. */
    private static String pristineLog4j2;

    private String baseUrl() {
        return TestContext.get("baseUrl").toString();
    }

    private DynamicApimContainer container() {
        Object c = TestContext.get("blockApimContainer");
        if (!(c instanceof DynamicApimContainer)) {
            throw new IllegalStateException("Block APIM container is not available in the test context");
        }
        return (DynamicApimContainer) c;
    }

    /** Enables remote logging for a log type (AUDIT/CARBON/API) by pointing its appender at {@code url}. */
    @When("I enable remote logging for log type {string} pointing at URL {string}")
    public void enableRemoteLogging(String logType, String url) throws Exception {
        sendConfigOp("addRemoteServerConfig", logType, Utils.resolveContextPlaceholders(url));
        if (!enabledLogTypes.contains(logType)) {
            enabledLogTypes.add(logType);
        }
    }

    /** Resets (disables) remote logging for a log type — its appender reverts to the local RollingFile. */
    @When("I disable remote logging for log type {string}")
    public void disableRemoteLogging(String logType) throws Exception {
        sendConfigOp("resetRemoteServerConfig", logType, "");
        enabledLogTypes.remove(logType);
    }

    /**
     * Runs the startup-time reconciliation ({@code syncRemoteServerConfigs}): the server re-reads every
     * persisted remote-logging config and re-applies it to {@code log4j2.properties}. This is the path that
     * repairs an appender block someone removed from the file while the log type is still configured — and
     * that must leave log types WITHOUT a remote URL untouched.
     */
    @When("I sync the remote logging configurations")
    public void syncRemoteLoggingConfigurations() throws Exception {
        sendSoapOp("syncRemoteServerConfigs", "");
    }

    private void sendConfigOp(String op, String logType, String url) throws Exception {
        String data = "<ax2:connectTimeoutMillis>2000</ax2:connectTimeoutMillis>"
                + "<ax2:logType>" + Utils.escapeXml(logType) + "</ax2:logType>"
                + "<ax2:url>" + Utils.escapeXml(url == null ? "" : url) + "</ax2:url>"
                + "<ax2:verifyHostname>false</ax2:verifyHostname>";
        sendSoapOp(op, "<ns:data>" + data + "</ns:data><ns:args1>false</ns:args1>");
    }

    /** Posts a RemoteLoggingConfig operation as the super-tenant admin and asserts Axis2 accepted it. */
    private void sendSoapOp(String op, String innerXml) throws Exception {
        String envelope = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ns=\"" + OP_NS + "\" xmlns:ax2=\"" + DATA_NS + "\">"
                + "<soapenv:Header/><soapenv:Body>"
                + "<ns:" + op + ">" + innerXml + "</ns:" + op + ">"
                + "</soapenv:Body></soapenv:Envelope>";

        User admin = Identity.resolveActor("admin");
        HttpResponse response = Requests.soap(baseUrl() + SERVICE_PATH, envelope,
                "urn:" + op, admin.getUserName(), admin.getPassword());
        // These are one-way (Robust In-Only) SOAP ops — Axis2 acknowledges with 202 Accepted (no response body),
        // not 200. A SOAP fault would surface as 500, so 202 confirms the config was accepted.
        Assert.assertEquals(response.getResponseCode(), 202, op + " SOAP call failed: " + response.getData());
    }

    /**
     * Seeds one of the {@code artifacts/configFiles/remoteLogging/} fixtures as the running server's
     * {@code log4j2.properties}, so a scenario can start from a known appender layout — typically one with a
     * target appender block stripped out. The service only reads this file when an operation is invoked, so
     * writing it changes nothing on its own; the following enable/disable/sync step is what acts on it.
     * The untouched file is restored after every scenario by the teardown hook.
     */
    @When("I apply the log4j2 fixture {string} to the server")
    public void applyLog4j2Fixture(String fixtureName) throws Exception {
        String resource = LOG4J2_FIXTURE_DIR + fixtureName;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            Assert.assertNotNull(in, "log4j2 fixture not found on the test classpath: " + resource);
            container().writeContainerFile(container().getContainerLog4j2Path(),
                    new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    /**
     * Polls the running container's {@code log4j2.properties} until the named appender's {@code type} equals the
     * expected value (the OSGi log reconfig after the SOAP call is asynchronous, so this waits, never sleeps blind).
     * Also the wait for an appender block that was ABSENT to be CREATED, since a missing block reads as a null type.
     */
    @Then("the {string} log appender should become {string} within {int} seconds")
    public void appenderShouldBecome(String appenderName, String expectedType, int timeoutSeconds) throws Exception {
        long end = System.currentTimeMillis() + Math.max(timeoutSeconds * 1000L, 10000L);
        String actual = null;
        while (System.currentTimeMillis() < end) {
            actual = appenderType(log4j2Content(), appenderName);
            if (expectedType.equals(actual)) {
                return;
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        Assert.assertEquals(actual, expectedType,
                appenderName + " appender type did not become " + expectedType + " within the deadline");
    }

    /** Asserts the named appender's current type, with no wait — for an appender the scenario did NOT target. */
    @Then("the {string} log appender type should be {string}")
    public void appenderTypeShouldBe(String appenderName, String expectedType) {
        Assert.assertEquals(appenderType(log4j2Content(), appenderName), expectedType,
                appenderName + " appender has an unexpected type in log4j2.properties");
    }

    /** Asserts the named appender block is not in log4j2.properties at all. */
    @Then("the {string} log appender block should be absent")
    public void appenderBlockShouldBeAbsent(String appenderName) {
        Assert.assertNull(appenderType(log4j2Content(), appenderName),
                appenderName + " appender block must not be present in log4j2.properties");
    }

    /**
     * Asserts the named appender KEEPS its type for the whole window. A "the server left this alone" claim has no
     * positive signal to wait for, so it needs a settle window: asserting once, immediately, would pass before a
     * wrong write had a chance to land. This re-reads throughout and fails on the first deviation — unlike a blind
     * sleep-then-check, it also pins down when the change happened.
     */
    @Then("the {string} log appender type should remain {string} for {int} seconds")
    public void appenderTypeShouldRemain(String appenderName, String expectedType, int seconds) throws Exception {
        long end = System.currentTimeMillis() + seconds * 1000L;
        do {
            Assert.assertEquals(appenderType(log4j2Content(), appenderName), expectedType,
                    appenderName + " appender type changed during the settle window — the server rewrote an "
                            + "appender it was not asked to touch");
            Thread.sleep(POLL_INTERVAL_MILLIS);
        } while (System.currentTimeMillis() < end);
    }

    /** The absence counterpart of the settle-window assertion above. */
    @Then("the {string} log appender block should remain absent for {int} seconds")
    public void appenderBlockShouldRemainAbsent(String appenderName, int seconds) throws Exception {
        long end = System.currentTimeMillis() + seconds * 1000L;
        do {
            Assert.assertNull(appenderType(log4j2Content(), appenderName),
                    appenderName + " appender block was created during the settle window — the server wrote an "
                            + "appender for a log type that has no remote logging configured");
            Thread.sleep(POLL_INTERVAL_MILLIS);
        } while (System.currentTimeMillis() < end);
    }

    /**
     * Asserts how many times the appender is named in the top-level {@code appenders = ...} list. Expect 1 for an
     * appender the server wrote (an appender missing from the list is a dangling appender log4j2 never
     * instantiates — and writing it twice is the duplication bug this pins), and 0 for one it must not touch.
     */
    @Then("the {string} log appender should be listed in the appenders list exactly {int} time(s)")
    public void appenderShouldBeListedTimes(String appenderName, int expectedCount) {
        Assert.assertEquals(countInAppendersList(log4j2Content(), appenderName), expectedCount,
                appenderName + " appears an unexpected number of times in the 'appenders' list: "
                        + appendersLine(log4j2Content()));
    }

    /** Asserts the appender block defines a property, e.g. the {@code url} a remote appender publishes to. */
    @Then("the {string} log appender block should define property {string}")
    public void appenderBlockShouldDefineProperty(String appenderName, String property) {
        Assert.assertTrue(Pattern.compile("(?m)^appender\\." + Pattern.quote(appenderName) + "\\."
                        + Pattern.quote(property) + "\\s*=").matcher(log4j2Content()).find(),
                "appender." + appenderName + "." + property + " is missing from log4j2.properties");
    }

    private String log4j2Content() {
        return container().readContainerFile(container().getContainerLog4j2Path());
    }

    /** The {@code type} of the named appender, or {@code null} when the block is absent. */
    private static String appenderType(String content, String appenderName) {
        Matcher m = Pattern.compile("(?m)^appender\\." + Pattern.quote(appenderName) + "\\.type\\s*=\\s*(\\S+)")
                .matcher(content);
        return m.find() ? m.group(1).trim() : null;
    }

    /** The raw top-level {@code appenders = ...} line (NOT an {@code appender.<name>.*} property), or null. */
    private static String appendersLine(String content) {
        for (String line : content.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("appenders") && trimmed.contains("=") && !trimmed.startsWith("appender.")) {
                return trimmed;
            }
        }
        return null;
    }

    private static int countInAppendersList(String content, String appenderName) {
        String line = appendersLine(content);
        if (line == null) {
            return 0;
        }
        int count = 0;
        for (String token : line.substring(line.indexOf('=') + 1).split(",")) {
            if (token.trim().equals(appenderName)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Starts a host HTTP sink on an ephemeral port and stores the container-reachable URL under {@code ctxKey}
     * (via {@code host.docker.internal}, which {@code DynamicApimContainer} maps to the host gateway).
     */
    @When("I start a mock log sink and store its container URL as {string}")
    public void startMockSink(String ctxKey) throws Exception {
        sinkPayloads.clear();
        sinkServer = HttpServer.create(new InetSocketAddress(0), 0);
        int port = sinkServer.getAddress().getPort();
        sinkServer.createContext("/api/logs/consume", exchange -> {
            try (InputStream body = exchange.getRequestBody()) {
                sinkPayloads.add(new String(body.readAllBytes(), StandardCharsets.UTF_8));
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        sinkServer.start();
        TestContext.set(Utils.normalizeContextKey(ctxKey),
                "http://host.docker.internal:" + port + "/api/logs/consume");
    }

    /** Triggers an action that emits an AUDIT_LOG entry — an authenticated admin GET of the key-managers. */
    @When("I trigger an audit log entry")
    public void triggerAuditLogEntry() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        HttpResponse response = Requests.get(Utils.getKeyManagersURL(baseUrl()), headers);
    }

    /** Asserts the mock sink receives at least one payload within the deadline, re-triggering audit actions. */
    @Then("the mock log sink should receive a log payload within {int} seconds")
    public void sinkShouldReceivePayload(int timeoutSeconds) throws Exception {
        long end = System.currentTimeMillis() + Math.max(timeoutSeconds * 1000L, 10000L);
        while (System.currentTimeMillis() < end) {
            if (!sinkPayloads.isEmpty()) {
                return;
            }
            triggerAuditLogEntry();
            Thread.sleep(2000);
        }
        Assert.assertFalse(sinkPayloads.isEmpty(), "Mock log sink received no log payload within the deadline");
    }

    /**
     * Asserts the remote stream STOPS after remote logging is disabled. The runtime OSGi reconfig lags the
     * log4j2.properties file update, so audit logs already in flight keep arriving briefly after the disable
     * call — this first waits for the stream to quiesce (count stable across a quiet window), then confirms a
     * FRESH audit action produces no new payload (remote logging is genuinely off, not just draining).
     */
    @Then("the mock log sink should stop receiving payloads within {int} seconds")
    public void sinkShouldStopReceiving(int timeoutSeconds) throws Exception {
        long end = System.currentTimeMillis() + Math.max(timeoutSeconds * 1000L, 20000L);
        int last = -1;
        int stableRounds = 0;
        while (System.currentTimeMillis() < end) {
            int now = sinkPayloads.size();
            if (now == last) {
                if (++stableRounds >= 3) {
                    break;   // count unchanged across ~6s → stream quiesced
                }
            } else {
                stableRounds = 0;
                last = now;
            }
            Thread.sleep(2000);
        }
        // The stream has quiesced — a fresh audit action must now NOT reach the sink (appender is local again).
        int before = sinkPayloads.size();
        triggerAuditLogEntry();
        Thread.sleep(5000);
        Assert.assertEquals(sinkPayloads.size(), before,
                "Mock sink still received a payload after remote logging was disabled and the stream quiesced");
    }

    /**
     * Captures the block container's untouched {@code log4j2.properties} once, before the first remote-logging
     * scenario, so {@link #restoreLog4j2Configuration()} can put it back after each one. Taken here rather than
     * lazily at the first fixture write, so the baseline is the pristine distribution config and not whatever an
     * earlier scenario's service call happened to leave behind.
     */
    @Before("@remote-logging")
    public void snapshotLog4j2Configuration() {
        if (pristineLog4j2 == null) {
            pristineLog4j2 = log4j2Content();
        }
    }

    /** Stops the host mock sink and undoes the scenario's server-config mutations (idempotent). */
    @After("@remote-logging")
    public void stopMockSink() {
        // Failure-safe teardown: if a scenario failed before its inline "disable remote logging" step, the server
        // is still redirecting logs — reset every type we enabled (idempotent) so this block's container isn't
        // left mutated. On the happy path the disable step already cleared enabledLogTypes, so this no-ops.
        for (String logType : enabledLogTypes) {
            try {
                sendConfigOp("resetRemoteServerConfig", logType, "");
            } catch (Exception e) {
                log.warn("Teardown: failed to reset remote logging for type '" + logType + "': " + e.getMessage());
            }
        }
        enabledLogTypes.clear();
        restoreLog4j2Configuration();
        if (sinkServer != null) {
            sinkServer.stop(0);
            sinkServer = null;
        }
        sinkPayloads.clear();
    }

    /**
     * Writes the pristine {@code log4j2.properties} back, so the next scenario in this block starts from the
     * distribution config instead of the stripped fixture this one seeded.
     *
     * <p>The resets above are asynchronous, so this first waits for the file to stop changing — restoring while
     * the server still has a rewrite in flight would let that rewrite land on top and silently defeat the
     * restore.</p>
     */
    private void restoreLog4j2Configuration() {
        if (pristineLog4j2 == null) {
            return;
        }
        try {
            waitForLog4j2ToSettle();
            container().writeContainerFile(container().getContainerLog4j2Path(), pristineLog4j2);
        } catch (Exception e) {
            log.warn("Teardown: failed to restore log4j2.properties: " + e.getMessage());
        }
    }

    /** Waits (bounded) until two consecutive reads of log4j2.properties agree — i.e. no rewrite is in flight. */
    private void waitForLog4j2ToSettle() throws InterruptedException {
        long end = System.currentTimeMillis() + 15000L;
        String previous = null;
        while (System.currentTimeMillis() < end) {
            String current = log4j2Content();
            if (current.equals(previous)) {
                return;
            }
            previous = current;
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        log.warn("log4j2.properties was still changing after the settle deadline; restoring anyway");
    }
}
