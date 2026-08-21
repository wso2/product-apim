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
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.am.testcontainers.DynamicApimContainer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Logging glue — the two devops logging APIs and the server log files they drive. Ports
 * CorrelationLoggingTest and APILoggingTest.
 *
 * <p>Two independent product surfaces live here because they are configured the same way (the devops API,
 * basic-auth as the super admin) and verified the same way (by reading a log file out of the running
 * container):
 *
 * <ul>
 *   <li><b>Correlation logging</b> — {@code /api/am/devops/v0/config/correlation} toggles the five components
 *       ({@code http, jdbc, ldap, synapse, method-calls}) named in {@code api-manager.xml}, and the enabled
 *       ones emit into {@code repository/logs/correlation.log}. Config is persisted in
 *       {@code AM_CORRELATION_CONFIGS}, which is why it survives a restart.</li>
 *   <li><b>Per-API logging</b> — {@code /api/am/devops/v0/tenant-logs/{tenant}/apis} sets a log level per API
 *       or per API resource, and {@code FULL} makes the gateway emit into {@code repository/logs/api.log}.</li>
 * </ul>
 *
 * <h2>Reading the log files</h2>
 * Legacy held a {@code BufferedReader} open on the log file for the life of the test class and consumed
 * forward from wherever the last read stopped. There is no such handle here (the file is inside the
 * container), so the equivalent is an explicit BYTE OFFSET: a scenario marks the file's current length, does
 * something, and then asserts only over what was appended after that mark. That makes each assertion about
 * the lines THIS scenario provoked and immune to whatever a previous scenario left behind — which matters far
 * more here than it did upstream, because in v2 the container is shared across a block.
 *
 * <h2>On the marker strings</h2>
 * The markers the features match are the literals the product actually emits, not the loose substrings legacy
 * used. This matters because legacy's matchers over-match badly:
 * <ul>
 *   <li>{@code isHTTPLogLine} matched any line containing {@code "HTTP"}. But {@code |HTTP|} also appears in
 *       the synapse {@code ROUND-TRIP LATENCY} / {@code BACKEND LATENCY} lines and in the gateway
 *       {@code LogsHandler} line — and the {@code LogsHandler} line is gated on the <b>method-calls</b>
 *       component, not {@code http}. The genuine {@code http} component is the Tomcat
 *       {@code RequestCorrelationIdValve}, which emits {@code |HTTP-In-Request|} / {@code |HTTP-In-Response|}
 *       for SERVLET traffic (9443: publisher/devportal/admin/devops REST) and nothing at all for gateway
 *       traffic, which goes through the Synapse passthrough NIO transport and never enters the Catalina
 *       pipeline. So the features here provoke {@code http} lines with a management-plane call, and match
 *       {@code |HTTP-In-Request|}.</li>
 *   <li>{@code isMethodCallsLogLine} additionally matched the application NAME, so any line merely mentioning
 *       the app satisfied it. The real token is {@code |METHOD|}.</li>
 * </ul>
 */
public class LoggingSteps {

    private static final Logger logger = LoggerFactory.getLogger(LoggingSteps.class);

    /** The component list {@code api-manager.xml} declares, in the order the devops API returns them. */
    private static final List<String> CORRELATION_COMPONENTS =
            Arrays.asList("http", "jdbc", "ldap", "synapse", "method-calls");

    /** Seeded into {@code AM_CORRELATION_PROPERTIES} for the jdbc component only. */
    private static final List<String> JDBC_DENIED_THREADS = Arrays.asList(
            "MessageDeliveryTaskThreadPool", "HumanTaskServer", "BPELServer", "CarbonDeploymentSchedulerThread");

    private static final String CORRELATION_CONFIG_PATH = "api/am/devops/v0/config/correlation";
    private static final String TENANT_LOGS_PATH = "api/am/devops/v0/tenant-logs/";

    /** Context key prefix under which {@link #iRecordLogFileLength} stores a file's marked length. */
    private static final String LOG_MARK_PREFIX = "logFileMark::";

    // ---------------------------------------------------------------------------------------------
    // Correlation logging configuration
    // ---------------------------------------------------------------------------------------------

    /** Reads the current correlation configuration. Non-asserting. */
    @When("I retrieve the correlation logging configuration")
    public void iRetrieveTheCorrelationConfiguration() throws IOException {
        Requests.get(Utils.getBaseUrl() + CORRELATION_CONFIG_PATH, devopsHeaders());
    }

    /**
     * Enables exactly the named components and disables every other one, in a single PUT — the devops API
     * takes the whole document, so "enable synapse" always means "and disable the other four". Pass an empty
     * string to disable everything. Non-asserting.
     */
    @When("I enable only the correlation logging components {string}")
    public void iEnableOnlyTheCorrelationComponents(String componentsCsv) throws IOException {
        Set<String> enabled = parseComponents(componentsCsv);
        Requests.put(Utils.getBaseUrl() + CORRELATION_CONFIG_PATH, devopsHeaders(),
                correlationPayload(enabled).toString(), "application/json");
    }

    /**
     * Asserts the response body is the correlation configuration document with exactly the named components
     * enabled — all five present, in the declared order, each carrying the right {@code enabled} flag, and
     * jdbc carrying its {@code deniedThreads} property. This is the structural equivalent of legacy's
     * string equality against a hardcoded document, minus the brittleness of pinning key order.
     */
    @Then("The correlation configuration should have exactly the components {string} enabled")
    public void theCorrelationConfigurationShouldHaveEnabled(String componentsCsv) {
        Set<String> expected = parseComponents(componentsCsv);
        String body = responseBody();
        JSONArray components = new JSONObject(body).getJSONArray("components");

        Assert.assertEquals(components.length(), CORRELATION_COMPONENTS.size(),
                "Correlation configuration should list every declared component. Body: " + body);

        for (int i = 0; i < CORRELATION_COMPONENTS.size(); i++) {
            String name = CORRELATION_COMPONENTS.get(i);
            JSONObject component = components.getJSONObject(i);
            Assert.assertEquals(component.getString("name"), name,
                    "Component at position " + i + " should be '" + name + "'. Body: " + body);
            Assert.assertEquals(component.getString("enabled"), Boolean.toString(expected.contains(name)),
                    "Component '" + name + "' has the wrong enabled flag. Body: " + body);

            JSONArray properties = component.getJSONArray("properties");
            if ("jdbc".equals(name)) {
                Assert.assertEquals(properties.length(), 1,
                        "jdbc should carry exactly its deniedThreads property. Body: " + body);
                JSONObject denied = properties.getJSONObject(0);
                Assert.assertEquals(denied.getString("name"), "deniedThreads", "Body: " + body);
                List<String> actual = new ArrayList<>();
                JSONArray value = denied.getJSONArray("value");
                for (int v = 0; v < value.length(); v++) {
                    actual.add(value.getString(v));
                }
                Assert.assertEquals(actual, JDBC_DENIED_THREADS,
                        "jdbc deniedThreads should be the seeded defaults. Body: " + body);
            } else {
                Assert.assertEquals(properties.length(), 0,
                        "Component '" + name + "' should carry no properties. Body: " + body);
            }
        }
    }

    /**
     * Waits for a configuration change to reach the gateway — correlation logging or per-API logging.
     * Both are devops PUTs propagated the same way, so they share this step; {@code what} names which
     * one for readability in the scenario and in the log line.
     *
     * <p>The devops PUT is acknowledged as soon as the row is written to {@code AM_CORRELATION_CONFIGS}; the
     * running gateway is updated SEPARATELY and asynchronously, by an event published
     * on the event hub. So a 200 from the PUT — and even a GET that reads the new state straight back — does
     * not yet mean the emitters have been switched.
     *
     * <p>This wait is therefore load-bearing rather than defensive, and it was established empirically: a
     * first run asserted immediately after a 200 from the disable and the very next invocation still produced
     * a full set of synapse and method-calls lines. It cannot be replaced by polling the assertion, because
     * the traffic that would produce the lines is driven ONCE, before the check — by the time a poll noticed,
     * the lines would already have been written.
     */
    @When("I wait {int} seconds for the {string} configuration to reach the gateway")
    public void iWaitForConfigToPropagate(int seconds, String what) throws InterruptedException {

        // Logged because this is an UNCONDITIONAL wait: without it a run sits silent for the whole window and
        // reads as a hang in CI. The label also names WHICH propagation is being waited on.
        logger.info("Waiting {}s for the {} configuration to reach the gateway", seconds, what);
        Thread.sleep(seconds * 1000L);
    }

    // ---------------------------------------------------------------------------------------------
    // Per-API logging configuration (devops tenant-logs)
    // ---------------------------------------------------------------------------------------------

    /** Lists per-API log levels for a tenant. Non-asserting. */
    @When("I retrieve the per-API log levels for tenant {string}")
    public void iRetrievePerApiLogLevels(String tenant) throws IOException {
        Requests.get(Utils.getBaseUrl() + TENANT_LOGS_PATH + tenant + "/apis", devopsHeaders());
    }

    /** Lists per-API log levels for a tenant filtered to one level (e.g. {@code full}). Non-asserting. */
    @When("I retrieve the per-API log levels for tenant {string} filtered to level {string}")
    public void iRetrievePerApiLogLevelsFiltered(String tenant, String level) throws IOException {
        Requests.get(Utils.getBaseUrl() + TENANT_LOGS_PATH + tenant + "/apis?log-level=" + level,
                devopsHeaders());
    }

    /** Sets an API-wide log level. Non-asserting. */
    @When("I set the log level of API {string} to {string} for tenant {string}")
    public void iSetApiLogLevel(String apiId, String level, String tenant) throws IOException {
        String actualApiId = TestContext.resolve(apiId).toString();
        JSONObject payload = new JSONObject().put("logLevel", level);
        Requests.put(Utils.getBaseUrl() + TENANT_LOGS_PATH + tenant + "/apis/" + actualApiId,
                devopsHeaders(), payload.toString(), "application/json");
    }

    /** Sets a log level scoped to one resource (verb + path) of an API. Non-asserting. */
    @When("I set the log level of API {string} to {string} for resource {string} {string} in tenant {string}")
    public void iSetResourceLogLevel(String apiId, String level, String verb, String path, String tenant)
            throws IOException {
        String actualApiId = TestContext.resolve(apiId).toString();
        JSONObject payload = new JSONObject()
                .put("logLevel", level)
                .put("resourceMethod", verb)
                .put("resourcePath", path);
        Requests.put(Utils.getBaseUrl() + TENANT_LOGS_PATH + tenant + "/apis/" + actualApiId,
                devopsHeaders(), payload.toString(), "application/json");
    }

    /**
     * Asserts the tenant-logs listing carries an entry for the API with exactly these four field values.
     * Pass {@code null} for {@code resourceMethod}/{@code resourcePath} to match the API-wide entry.
     *
     * <p>Legacy asserted the WHOLE listing equalled a one- or two-entry document, which only holds on a server
     * whose entire API inventory belongs to that one test. The container is shared here, so the portable form
     * of the same claim is that the API under test appears with precisely the expected shape. The
     * discriminating power legacy got from exact equality is recovered by the negative below.
     */
    @Then("The per-API log listing should contain API {string} at level {string} for resource {string} {string}")
    public void theListingShouldContain(String apiId, String level, String resourceMethod, String resourcePath) {
        String actualApiId = TestContext.resolve(apiId).toString();
        String body = responseBody();
        JSONArray apis = new JSONObject(body).getJSONArray("apis");

        for (int i = 0; i < apis.length(); i++) {
            JSONObject entry = apis.getJSONObject(i);
            if (!actualApiId.equals(entry.optString("apiId", null))) {
                continue;
            }
            if (!matches(entry, "resourceMethod", resourceMethod) || !matches(entry, "resourcePath", resourcePath)) {
                continue;
            }
            Assert.assertEquals(entry.getString("logLevel"), level,
                    "API '" + actualApiId + "' is listed at the wrong log level. Body: " + body);
            return;
        }
        Assert.fail("No entry for API '" + actualApiId + "' with resourceMethod=" + resourceMethod
                + " resourcePath=" + resourcePath + " in the tenant-logs listing. Body: " + body);
    }

    /** Asserts the listing carries NO entry at all for an API — used against the {@code log-level} filter. */
    @Then("The per-API log listing should not contain API {string}")
    public void theListingShouldNotContain(String apiId) {
        String actualApiId = TestContext.resolve(apiId).toString();
        String body = responseBody();
        JSONArray apis = new JSONObject(body).getJSONArray("apis");

        for (int i = 0; i < apis.length(); i++) {
            if (actualApiId.equals(apis.getJSONObject(i).optString("apiId", null))) {
                Assert.fail("API '" + actualApiId + "' should not appear in this listing. Body: " + body);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Container log files
    // ---------------------------------------------------------------------------------------------

    /**
     * Marks a server log file's current length, so later assertions consider only what is appended after this
     * point. The mark is per file name, so a scenario can track correlation.log and api.log independently.
     */
    @When("I mark the current end of the server log file {string}")
    public void iRecordLogFileLength(String fileName) {
        TestContext.set(LOG_MARK_PREFIX + fileName, readLogFile(fileName).length());
    }

    /**
     * Polls until a line appended after the mark contains {@code marker}, or fails with the appended text so
     * the actual log content is visible in the report rather than just "expected true but found false".
     */
    @Then("The server log file {string} should gain a line containing {string} within {int} seconds")
    public void logFileShouldGainLine(String fileName, String marker, int seconds) throws InterruptedException {
        String expected = Utils.resolveContextPlaceholders(marker);
        String appended = Utils.retryUntil(seconds * 1000L,
                () -> appendedSinceMark(fileName),
                text -> containsMarker(text, expected));
        Assert.assertTrue(appended != null && containsMarker(appended, expected),
                "No line containing '" + expected + "' was appended to " + fileName + " within " + seconds
                        + "s. Appended since the mark:\n" + appended);
    }

    /**
     * Waits out a settle period and then asserts NO line appended after the mark contains {@code marker}.
     *
     * <p>The wait is deliberate and cannot be shortened into a poll: proving absence means giving the server
     * the same time to write the line that the positive assertion would have allowed it. (Legacy wrapped its
     * negative in the same retry-until-it-passes loop as its positive, which would have accepted the very
     * first sample and so never really waited.)
     */
    @Then("The server log file {string} should gain no line containing {string} within {int} seconds")
    public void logFileShouldNotGainLine(String fileName, String marker, int seconds)
            throws InterruptedException {
        String unexpected = Utils.resolveContextPlaceholders(marker);
        Thread.sleep(seconds * 1000L);
        String appended = appendedSinceMark(fileName);
        if (containsMarker(appended, unexpected)) {
            Assert.fail("A line containing '" + unexpected + "' was appended to " + fileName
                    + " but none was expected. Appended since the mark:\n" + appended);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    /** Devops APIs are basic-auth only; they take the acting actor's carbon credentials. */
    private Map<String, String> devopsHeaders() {
        Map<String, String> headers = Identity.actingBasicAuthHeaders();
        headers.put(Constants.REQUEST_HEADERS.CONTENT_TYPE, "application/json");
        headers.put("Accept", "application/json");
        return headers;
    }

    private static Set<String> parseComponents(String componentsCsv) {
        Set<String> parsed = new LinkedHashSet<>();
        for (String raw : componentsCsv.split(",")) {
            String name = raw.trim();
            if (name.isBlank()) {
                continue;
            }
            if (!CORRELATION_COMPONENTS.contains(name)) {
                throw new IllegalArgumentException("'" + name + "' is not a declared correlation component "
                        + CORRELATION_COMPONENTS);
            }
            parsed.add(name);
        }
        return parsed;
    }

    /** Builds the whole configuration document with exactly {@code enabled} switched on. */
    private static JSONObject correlationPayload(Set<String> enabled) {
        JSONArray components = new JSONArray();
        for (String name : CORRELATION_COMPONENTS) {
            JSONObject component = new JSONObject()
                    .put("name", name)
                    .put("enabled", Boolean.toString(enabled.contains(name)));
            JSONArray properties = new JSONArray();
            if ("jdbc".equals(name)) {
                properties.put(new JSONObject()
                        .put("name", "deniedThreads")
                        .put("value", new JSONArray(JDBC_DENIED_THREADS)));
            }
            component.put("properties", properties);
            components.put(component);
        }
        return new JSONObject().put("components", components);
    }

    /** Treats the Gherkin literal {@code null} as "this field must be JSON null / absent". */
    private static boolean matches(JSONObject entry, String field, String expected) {
        String actual = entry.isNull(field) ? null : entry.optString(field, null);
        return "null".equals(expected) ? actual == null : expected.equals(actual);
    }

    private static boolean containsMarker(String appended, String marker) {
        for (String line : appended.split("\\R")) {
            if (line.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private String appendedSinceMark(String fileName) {
        Object mark = TestContext.get(LOG_MARK_PREFIX + fileName);
        if (mark == null) {
            throw new IllegalStateException("The end of " + fileName + " was never marked — the scenario must "
                    + "mark it before asserting on what was appended.");
        }
        String content = readLogFile(fileName);
        int from = Integer.parseInt(mark.toString());
        // The file rolls over at 10MB, which resets its length; treat a shrink as "read it all".
        return from <= content.length() ? content.substring(from) : content;
    }

    private String readLogFile(String fileName) {
        // Path shape owned by DynamicApimContainer alongside getContainerLog4j2Path(), so the in-container
        // layout lives in one module; the accessor also validates apim.server.name instead of silently
        // resolving a path containing "null".
        return container().readContainerFile(container().getContainerLogFilePath(fileName));
    }

    private DynamicApimContainer container() {
        Object candidate = TestContext.get("blockApimContainer");
        if (!(candidate instanceof DynamicApimContainer)) {
            throw new IllegalStateException("Block APIM container is not available in the test context");
        }
        return (DynamicApimContainer) candidate;
    }

    /**
     * The body of the response under assertion, guaranteed 2xx and non-blank (§7). Callers parse it as JSON, so a
     * failed or empty response must fail HERE naming the status — otherwise it surfaces as an opaque
     * JSONException/NPE that says nothing about what actually came back.
     */
    private static String responseBody() {
        Object response = TestContext.get("httpResponse");
        if (response == null) {
            throw new IllegalStateException("No response is in the context to assert against.");
        }
        org.wso2.carbon.automation.test.utils.http.client.HttpResponse httpResponse =
                (org.wso2.carbon.automation.test.utils.http.client.HttpResponse) response;
        Assert.assertTrue(httpResponse.getResponseCode() >= 200 && httpResponse.getResponseCode() < 300
                        && httpResponse.getData() != null && !httpResponse.getData().isBlank(),
                "Expected a 2xx response with a body to assert against, but got: "
                        + httpResponse.getResponseCode() + " / " + httpResponse.getData());
        return httpResponse.getData();
    }
}
