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
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
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
 * <p>Remote logging is a super-tenant, server-global setting (not per-tenant), so these scenarios run once as
 * the super-tenant admin, in a dedicated thread-count=1 block (they mutate the shared server's log config).</p>
 */
public class RemoteLoggingSteps {

    /* Axis2 namespaces from the RemoteLoggingConfig service WSDL. */
    private static final String OP_NS = "http://org.apache.axis2/xsd";
    private static final String DATA_NS = "http://data.service.logging.carbon.wso2.org/xsd";
    private static final String SERVICE_PATH = "services/RemoteLoggingConfig";

    /* Mock HTTP sink shared across steps + the teardown hook. */
    private static HttpServer sinkServer;
    private static final List<String> sinkPayloads = new CopyOnWriteArrayList<>();

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
        sendConfigOp("addRemoteServerConfig", "urn:addRemoteServerConfig", logType,
                Utils.resolveContextPlaceholders(url));
    }

    /** Resets (disables) remote logging for a log type — its appender reverts to the local RollingFile. */
    @When("I disable remote logging for log type {string}")
    public void disableRemoteLogging(String logType) throws Exception {
        sendConfigOp("resetRemoteServerConfig", "urn:resetRemoteServerConfig", logType, "");
    }

    private void sendConfigOp(String op, String soapAction, String logType, String url) throws Exception {
        StringBuilder data = new StringBuilder()
                .append("<ax2:connectTimeoutMillis>2000</ax2:connectTimeoutMillis>")
                .append("<ax2:logType>").append(logType).append("</ax2:logType>")
                .append("<ax2:url>").append(url == null ? "" : url).append("</ax2:url>")
                .append("<ax2:verifyHostname>false</ax2:verifyHostname>");
        String envelope = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ns=\"" + OP_NS + "\" xmlns:ax2=\"" + DATA_NS + "\">"
                + "<soapenv:Header/><soapenv:Body>"
                + "<ns:" + op + "><ns:data>" + data + "</ns:data><ns:args1>false</ns:args1></ns:" + op + ">"
                + "</soapenv:Body></soapenv:Envelope>";

        User admin = Identity.resolveActor("admin");
        HttpResponse response = SimpleHTTPClient.getInstance().sendSoapRequest(baseUrl() + SERVICE_PATH, envelope,
                soapAction, admin.getUserName(), admin.getPassword());
        TestContext.set("httpResponse", response);
        // These are one-way (Robust In-Only) SOAP ops — Axis2 acknowledges with 202 Accepted (no response body),
        // not 200. A SOAP fault would surface as 500, so 202 confirms the config was accepted.
        Assert.assertEquals(response.getResponseCode(), 202, op + " SOAP call failed: " + response.getData());
    }

    /**
     * Polls the running container's {@code log4j2.properties} until the named appender's {@code type} equals the
     * expected value (the OSGi log reconfig after the SOAP call is asynchronous, so this waits, never sleeps blind).
     */
    @Then("the {string} log appender should become {string} within {int} seconds")
    public void appenderShouldBecome(String appenderName, String expectedType, int timeoutSeconds) throws Exception {
        Pattern p = Pattern.compile("(?m)^appender\\." + Pattern.quote(appenderName) + "\\.type\\s*=\\s*(\\S+)");
        long end = System.currentTimeMillis() + Math.max(timeoutSeconds * 1000L, 10000L);
        String actual = null;
        while (System.currentTimeMillis() < end) {
            Matcher m = p.matcher(container().readContainerFile(container().getContainerLog4j2Path()));
            actual = m.find() ? m.group(1).trim() : null;
            if (expectedType.equals(actual)) {
                return;
            }
            Thread.sleep(2000);
        }
        Assert.assertEquals(actual, expectedType,
                appenderName + " appender type did not become " + expectedType + " within the deadline");
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
        HttpResponse response = SimpleHTTPClient.getInstance().doGet(Utils.getKeyManagersURL(baseUrl()), headers);
        TestContext.set("httpResponse", response);
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

    /** Stops the host mock sink after the remote-logging scenarios (idempotent). */
    @After("@remote-logging")
    public void stopMockSink() {
        if (sinkServer != null) {
            sinkServer.stop(0);
            sinkServer = null;
        }
        sinkPayloads.clear();
    }
}
