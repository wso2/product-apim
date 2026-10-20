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

package org.wso2.am.testcontainers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * Per-block Squid HTTP proxy container used by WS/HTTP proxy profile integration tests.
 *
 * <p>Two Squid instances run inside one container:
 * <ul>
 *   <li>Port {@value #ANON_PORT} — anonymous proxy, no credentials required.</li>
 *   <li>Port {@value #AUTH_PORT} — authenticated proxy, requires
 *       {@code Proxy-Authorization: Basic testproxyuser:testproxypass}.</li>
 * </ul>
 *
 * <p>The container starts on the owning block's private network under the alias {@code squid-proxy}, so the APIM
 * container can reach {@code squid-proxy:3128} and {@code squid-proxy:3129} without host networking. The TOML
 * overlay for proxy-profile tests uses {@code proxy_host = "squid-proxy"} with the appropriate port.
 *
 * <p>CONNECT tunnel counts use the union of completed Squid access-log entries and live proxy sockets,
 * deduplicated by the client source port. This includes tunnels that remain open and avoids double-counting a
 * CONNECT present in both observations. Call {@link #clearLogs()} between scenarios to establish the baseline.
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

    /** Both Squid instances together use a few percent of one core at peak (measured), so half a CPU is ample. */
    public static final CpuLimit CPU_LIMIT = CpuLimit.of(0.5);
    public static final long DISTRIBUTED_MEMORY_LIMIT_BYTES = 50L * 1024 * 1024;

    private final GenericContainer<?> container;
    private final String blockLabel;
    private Set<String> anonBaselinePorts = Set.of();
    private Set<String> authBaselinePorts = Set.of();

    public SquidProxyServer(String blockLabel, Network network) {
        this(blockLabel, network, null);
    }

    public SquidProxyServer(String blockLabel, Network network, Long memoryLimitBytes) {
        this(blockLabel, network, memoryLimitBytes, memoryLimitBytes);
    }

    public SquidProxyServer(String blockLabel, Network network, Long memoryLimitBytes, Long memoryAndSwapLimitBytes) {
        if (memoryLimitBytes != null
                && (memoryAndSwapLimitBytes == null || memoryAndSwapLimitBytes < memoryLimitBytes)) {
            throw new IllegalArgumentException("Squid memory-plus-swap limit must be at least its memory limit");
        }
        this.blockLabel = blockLabel;
        logger.info("Initializing SquidProxyServer for block '" + blockLabel + "'...");
        container = new GenericContainer<>(System.getProperty("squid.docker.image.name", "squid-proxy:latest"))
                .withExposedPorts(ANON_PORT, AUTH_PORT)
                .withNetwork(network)
                .withNetworkAliases("squid-proxy")
                .withCreateContainerCmdModifier(cmd -> {
                    CPU_LIMIT.applyTo(cmd);
                    if (memoryLimitBytes != null) {
                        cmd.getHostConfig().withMemory(memoryLimitBytes)
                                .withMemorySwap(memoryAndSwapLimitBytes);
                    }
                })
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

        container.withLogConsumer(new JclLogConsumer(logger));
    }

    public void start() {
        container.start();
        logger.info("SquidProxyServer for block '" + blockLabel + "' started — anon port " + ANON_PORT
                + ", auth port " + AUTH_PORT + ", " + CPU_LIMIT);
    }

    public void stop() {
        if (container.isRunning()) {
            container.stop();
            logger.info("SquidProxyServer for block '" + blockLabel + "' stopped");
        }
    }

    /**
     * Returns the number of HTTP CONNECT requests received by the anonymous proxy.
     * Counts every line in the Squid access log where the method field is {@code CONNECT},
     * regardless of whether Squid successfully relayed the tunnel (TCP_TUNNEL/200) or rejected
     * it (TCP_TUNNEL/503, NONE/-). This is the right signal for both positive assertions
     * ("proxy received N connections") and bypass assertions ("proxy received 0 connections").
     */
    public int getAnonConnectCount() throws Exception {
        // grep -c exits with status 1 when it finds zero matches — the || echo 0 fallback then appends a
        // second "0" line, making stdout "0\n0" and breaking Integer.parseInt. Using grep | wc -l instead:
        // wc -l always exits 0 and always emits exactly one number, even for an absent or empty log file.
        return connectPorts("/var/log/squid/anon-access.log", ANON_PORT, anonBaselinePorts).size();
    }

    /**
     * Returns the number of HTTP CONNECT requests received by the authenticated proxy.
     * Uses the same method-field grep as {@link #getAnonConnectCount()}.
     */
    public int getAuthConnectCount() throws Exception {
        return connectPorts("/var/log/squid/auth-access.log", AUTH_PORT, authBaselinePorts).size();
    }

    /**
     * Truncates both access logs so CONNECT counts start from zero for the next scenario.
     * Should be called from a {@code @Before} hook or the first step of each proxy scenario.
     */
    public void clearLogs() throws Exception {
        // Squid writes a CONNECT access-log record only after the tunnel ends. Some tenant WebSocket paths
        // leave the gateway-to-backend tunnel open after the client close handshake, so snapshot in-flight
        // tunnels before truncating and include only the post-clear delta in exact request counts.
        anonBaselinePorts = activeClientPorts(ANON_PORT);
        authBaselinePorts = activeClientPorts(AUTH_PORT);
        container.execInContainer(
                "sh", "-c",
                "truncate -s 0 /var/log/squid/anon-access.log /var/log/squid/auth-access.log 2>/dev/null || true"
        );
    }

    private Set<String> connectPorts(String logPath, int port, Set<String> baselinePorts) throws Exception {
        Set<String> sourcePorts = new HashSet<>(activeClientPorts(port));
        ExecResult log = container.execInContainer("sh", "-c", "cat " + logPath + " 2>/dev/null || true");
        for (String line : log.getStdout().split("\\R")) {
            String[] fields = line.trim().split("\\s+");
            // wsproxy logformat: timestamp, client address, client source port, result/status, method, URL.
            if (fields.length >= 5 && "CONNECT".equals(fields[4])) {
                sourcePorts.add(fields[2]);
            }
        }
        sourcePorts.removeAll(baselinePorts);
        return sourcePorts;
    }

    private Set<String> activeClientPorts(int port) throws Exception {
        ExecResult result = container.execInContainer("sh", "-c",
                "netstat -tn 2>/dev/null | awk '$1 ~ /^tcp/ && $4 ~ /:" + port
                        + "$/ && $6 != \"TIME_WAIT\" && $6 != \"LISTEN\" "
                        + "{ split($5, peer, \":\"); print peer[length(peer)] }'");
        Set<String> ports = new HashSet<>();
        for (String line : result.getStdout().split("\\R")) {
            String value = line.trim();
            if (!value.isEmpty() && value.matches("\\d+")) {
                ports.add(value);
            }
        }
        return ports;
    }
}
