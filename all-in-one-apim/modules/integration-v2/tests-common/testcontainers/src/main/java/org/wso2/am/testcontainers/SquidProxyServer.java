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

package org.wso2.am.testcontainers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

/**
 * JVM-wide singleton Squid HTTP proxy container used by WS/HTTP proxy profile integration tests.
 *
 * <p>Two Squid instances run inside one container:
 * <ul>
 *   <li>Port {@value #ANON_PORT} — anonymous proxy, no credentials required.</li>
 *   <li>Port {@value #AUTH_PORT} — authenticated proxy, requires
 *       {@code Proxy-Authorization: Basic testproxyuser:testproxypass}.</li>
 * </ul>
 *
 * <p>Both instances join {@link ContainerNetwork#SHARED_NETWORK} under the alias {@code squid-proxy},
 * so the APIM container (on the same network) can reach them at {@code squid-proxy:3128} and
 * {@code squid-proxy:3129} without any host networking. The TOML overlay for proxy-profile tests
 * uses {@code proxy_host = "squid-proxy"} with the appropriate port.
 *
 * <p>CONNECT tunnel counts are asserted by grepping the per-instance Squid access logs via
 * {@link #getAnonConnectCount()} and {@link #getAuthConnectCount()}. Call {@link #clearLogs()}
 * between scenarios to reset the counts.
 */
public class SquidProxyServer {

    private static final Log logger = LogFactory.getLog(SquidProxyServer.class);

    /** Port of the anonymous (no-auth) Squid instance. Referenced in TOML overlays. */
    public static final int ANON_PORT = 3128;

    /** Port of the authenticated (Basic auth) Squid instance. Referenced in TOML overlays. */
    public static final int AUTH_PORT = 3129;

    /** Proxy username baked into the image's htpasswd file. */
    public static final String PROXY_USERNAME = "testproxyuser";

    /** Proxy password baked into the image's htpasswd file. */
    public static final String PROXY_PASSWORD = "testproxypass";

    private final GenericContainer<?> container;

    private SquidProxyServer() {
        logger.info("Initializing SquidProxyServer...");
        container = new GenericContainer<>(System.getProperty("squid.docker.image.name", "squid-proxy:latest"))
                .withExposedPorts(ANON_PORT, AUTH_PORT)
                .withNetwork(ContainerNetwork.SHARED_NETWORK)
                .withNetworkAliases("squid-proxy")
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

        container.withLogConsumer(new JclLogConsumer(logger));
        container.start();
        logger.info("SquidProxyServer started — anon port " + ANON_PORT + ", auth port " + AUTH_PORT);
    }

    private static class InstanceHolder {
        private static final SquidProxyServer instance = new SquidProxyServer();
    }

    public static SquidProxyServer getInstance() {
        return InstanceHolder.instance;
    }

    /**
     * Returns the number of HTTP CONNECT requests received by the anonymous proxy.
     * Counts every line in the Squid access log where the method field is {@code CONNECT},
     * regardless of whether Squid successfully relayed the tunnel (TCP_TUNNEL/200) or rejected
     * it (TCP_TUNNEL/503, NONE/-). This is the right signal for both positive assertions
     * ("proxy received N connections") and bypass assertions ("proxy received 0 connections").
     */
    public int getAnonConnectCount() throws Exception {
        ExecResult result = container.execInContainer(
                "sh", "-c",
                "grep -c ' CONNECT ' /var/log/squid/anon-access.log 2>/dev/null || echo 0"
        );
        return Integer.parseInt(result.getStdout().trim());
    }

    /**
     * Returns the number of HTTP CONNECT requests received by the authenticated proxy.
     * Uses the same method-field grep as {@link #getAnonConnectCount()}.
     */
    public int getAuthConnectCount() throws Exception {
        ExecResult result = container.execInContainer(
                "sh", "-c",
                "grep -c ' CONNECT ' /var/log/squid/auth-access.log 2>/dev/null || echo 0"
        );
        return Integer.parseInt(result.getStdout().trim());
    }

    /**
     * Truncates both access logs so CONNECT counts start from zero for the next scenario.
     * Should be called from a {@code @Before} hook or the first step of each proxy scenario.
     */
    public void clearLogs() throws Exception {
        container.execInContainer(
                "sh", "-c",
                "truncate -s 0 /var/log/squid/anon-access.log /var/log/squid/auth-access.log 2>/dev/null || true"
        );
    }
}
