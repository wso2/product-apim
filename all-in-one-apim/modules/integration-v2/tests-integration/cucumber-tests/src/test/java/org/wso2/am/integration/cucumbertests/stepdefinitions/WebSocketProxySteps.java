/**
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.testcontainers.SquidProxyServer;

import java.io.IOException;

/**
 * Step definitions for asserting HTTP CONNECT proxy behaviour in WS proxy profile tests.
 *
 * <p>Requires a {@link SquidProxyServer} to be present in the test context under the key
 * {@code "blockSquidProxy"} — this is populated by {@code BlockLifecycleListener} when the
 * TestNG block declares {@code <parameter name="initProxy" value="true"/>}.
 *
 * <p>Two Squid instances are available:
 * <ul>
 *   <li>Anonymous proxy on port {@value SquidProxyServer#ANON_PORT} — no credentials needed.</li>
 *   <li>Authenticated proxy on port {@value SquidProxyServer#AUTH_PORT} — Basic auth required.</li>
 * </ul>
 * Use {@link #clearProxyLogs()} at the start of each scenario that checks counts, so counts
 * from prior scenarios in the same block do not bleed through.
 */
public class WebSocketProxySteps {

    private static final Log log = LogFactory.getLog(WebSocketProxySteps.class);

    private static final String SQUID_PROXY_KEY = "blockSquidProxy";

    /**
     * How long the CONNECT count must hold still before it is asserted. Comfortably above the gap between Squid
     * accepting a tunnel request and appending its access-log line (sub-second), and well below the interval
     * between two invocations any scenario makes — so it cannot settle early and under-count.
     */
    private static final long QUIET_MILLIS = 5000L;
    /**
     * Deadline for settling. {@link Utils#awaitSettledCount} floors this at the shared propagation ceiling, so a
     * counter that never goes quiet is bounded there rather than here; a settled counter returns after one quiet
     * window regardless.
     */
    private static final long SETTLE_TIMEOUT_MILLIS = 30_000L;

    @Given("the proxy access logs are cleared")
    public void clearProxyLogs() throws Exception {
        getProxy().clearLogs();
        log.debug("Squid access logs cleared");
    }

    @Then("the anonymous proxy should have received exactly {int} CONNECT request\\(s)")
    public void assertAnonConnectCount(int expected) throws InterruptedException {
        assertSettledConnectCount("Anonymous", expected,
                () -> sample(getProxy()::getAnonConnectCount),
                "check that the proxy profile target_hosts and bypass_hosts are configured correctly");
    }

    @Then("the authenticated proxy should have received exactly {int} CONNECT request\\(s)")
    public void assertAuthConnectCount(int expected) throws InterruptedException {
        assertSettledConnectCount("Authenticated", expected,
                () -> sample(getProxy()::getAuthConnectCount),
                "check that the proxy profile credentials and target_hosts are configured correctly");
    }

    /**
     * Asserts that the authenticated proxy was reached at least once, after its CONNECT count has settled.
     * Negative proxy scenarios must prove that the request reached the configured authenticated proxy, but the
     * number of denied CONNECT attempts is an implementation detail of the gateway's failed transport path.
     */
    @Then("the authenticated proxy should have received at least {int} CONNECT request\\(s)")
    public void assertAuthConnectCountAtLeast(int minimum) throws InterruptedException {
        Utils.SettledCount settled = Utils.awaitSettledCount(QUIET_MILLIS, SETTLE_TIMEOUT_MILLIS,
                () -> sample(getProxy()::getAuthConnectCount));
        Assert.assertTrue(settled.settled(),
                "Authenticated proxy CONNECT count never stopped changing (last=" + settled.value() + " after "
                        + settled.samples() + " samples) — something is still opening tunnels.");
        Assert.assertTrue(settled.value() >= minimum,
                "Authenticated proxy CONNECT count mismatch: expected at least=" + minimum + " actual="
                        + settled.value() + " (settled over " + settled.samples() + " samples) — the configured "
                        + "authenticated proxy was not reached.");
    }

    /**
     * Asserts a Squid CONNECT count once it has STOPPED CHANGING, then asserts the exact value (§12/§15).
     *
     * <p>Reading the log once is unsound in BOTH directions and the failure is silent either way. Squid appends a
     * line per CONNECT — including one it denies with {@code TCP_DENIED/407} — but it does so after the gateway's
     * connection attempt returns, so a single read taken too early under-counts. That makes an "exactly 0"
     * assertion pass for the wrong reason (nothing logged YET, rather than nothing attempted), which is precisely
     * how the wrong-credentials scenario used to go green while the gateway had in fact been refused by the proxy.
     * An "exactly N" assertion has the mirror-image problem: {@code retryUntil} accepting on {@code >= N} would
     * pass the instant the counter touches N and never see an extra arrival. Settling is the only formulation
     * that bounds the wait AND can observe an over-count.
     *
     * <p>Sound here because the counter is monotonic and goes quiescent: every scenario asserts after its
     * invocations have completed, so no further CONNECT can arrive once the value holds still.
     */
    private void assertSettledConnectCount(String which, int expected, Utils.CountProbe probe, String hint)
            throws InterruptedException {

        Utils.SettledCount settled = Utils.awaitSettledCount(QUIET_MILLIS, SETTLE_TIMEOUT_MILLIS, probe);
        Assert.assertTrue(settled.settled(),
                which + " proxy CONNECT count never stopped changing (last=" + settled.value() + " after "
                        + settled.samples() + " samples) — something is still opening tunnels, so no exact count "
                        + "can be asserted. " + hint);
        Assert.assertEquals(settled.value(), expected,
                which + " proxy CONNECT count mismatch: expected=" + expected + " actual=" + settled.value()
                        + " (settled over " + settled.samples() + " samples) — " + hint);
    }

    /**
     * Adapts a Squid log read to {@link Utils.CountProbe}. Only {@link IOException} is retried by the envelope, so
     * an interrupt is restored and surfaced as one (the next poll pause rethrows it) while anything else fails
     * fast rather than being mistaken for a transient.
     */
    private static int sample(ConnectCountRead read) throws IOException {
        try {
            return read.get();
        } catch (IOException transientRead) {
            throw transientRead;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while reading the Squid access log", interrupted);
        } catch (Exception unexpected) {
            throw new IllegalStateException("Could not read the Squid access log", unexpected);
        }
    }

    @FunctionalInterface
    private interface ConnectCountRead {
        int get() throws Exception;
    }

    private SquidProxyServer getProxy() {
        Object proxy = TestContext.get(SQUID_PROXY_KEY);
        if (!(proxy instanceof SquidProxyServer)) {
            throw new IllegalStateException(
                    "SquidProxyServer not in test context (key='" + SQUID_PROXY_KEY + "')"
                            + " — block must declare <parameter name=\"initProxy\" value=\"true\"/>");
        }
        return (SquidProxyServer) proxy;
    }
}
