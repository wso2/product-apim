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
import org.wso2.am.testcontainers.SquidProxyServer;

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

    @Given("the proxy access logs are cleared")
    public void clearProxyLogs() throws Exception {
        getProxy().clearLogs();
        log.debug("Squid access logs cleared");
    }

    @Then("the anonymous proxy should have received exactly {int} CONNECT request\\(s)")
    public void assertAnonConnectCount(int expected) throws Exception {
        int actual = getProxy().getAnonConnectCount();
        Assert.assertEquals(actual, expected,
                "Anonymous proxy CONNECT count mismatch: expected=" + expected + " actual=" + actual
                        + " — check that the proxy profile target_hosts and bypass_hosts are configured correctly");
    }

    @Then("the authenticated proxy should have received exactly {int} CONNECT request\\(s)")
    public void assertAuthConnectCount(int expected) throws Exception {
        int actual = getProxy().getAuthConnectCount();
        Assert.assertEquals(actual, expected,
                "Authenticated proxy CONNECT count mismatch: expected=" + expected + " actual=" + actual
                        + " — check that the proxy profile credentials and target_hosts are configured correctly");
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
