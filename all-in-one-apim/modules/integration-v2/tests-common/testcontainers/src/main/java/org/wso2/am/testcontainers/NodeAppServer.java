/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NodeAppServer {

    private static final Log logger = LogFactory.getLog(NodeAppServer.class);

    /** Network alias APIM gateways use to reach the sample backend apps on whichever network it is attached to. */
    public static final String NETWORK_ALIAS = "nodebackend";
    /** Additional alias used by the tenant-specific TLS fixture. */
    public static final String TENANT_NETWORK_ALIAS = "tenantbackend";

    // Publishing is only needed for HOST access; container-to-container traffic over the attached networks works
    // regardless (which is why mcp-server on 3020 functions unpublished). 3021 (sse-emitter) and 3022
    // (websub-receiver) ARE published because their introspection/reset endpoints are read from the test JVM.
    // Every port listed also joins the startup liveness set (Wait.forListeningPort waits on ALL exposed ports),
    // so an app added here must actually listen or every block's boot stalls.
    private final Integer[] exposedPorts = {3000, 3001, 3002, 3003, 3004, 3005, 3006, 3007, 3008, 3009, 3010, 3011,
            3012, 3013, 3014, 3015, 3016, 3017, 3018, 3019, 3021, 3022, 3023, 3024, 3026};

    /** Attempts to draw a fresh set of host ports when docker reports one of them already bound. */
    private static final int START_ATTEMPTS = 4;

    /** {@code containerPort -> explicitly chosen host port}, redrawn per boot attempt. See {@link #freeHostPort()}. */
    private final Map<Integer, Integer> stableHostPorts = new LinkedHashMap<>();

    private final GenericContainer<?> container;

    public NodeAppServer() {
        logger.info("Initializing NodeAppServer...");
        container = startWithPortRetry();
        logger.info("NodeAppServer successfully initialized");
    }

    /**
     * Starts the backend, redrawing its declared host ports when docker reports a bind conflict.
     *
     * <p>A conflict is expected occasionally: {@link #freeHostPort()} closes its probe socket before docker binds
     * the port, and this container declares one per exposed port. It is retried rather than reported because a
     * fresh draw almost always succeeds, and reporting it as a boot failure would point the reader at the backend
     * instead of at a taken port. A conflict surviving every attempt is a hard failure.
     */
    private GenericContainer<?> startWithPortRetry() {
        for (int attempt = 1; attempt <= START_ATTEMPTS; attempt++) {
            GenericContainer<?> candidate = buildContainer();
            try {
                candidate.start();
                // Logged because a stale host port is otherwise invisible: the failure it causes is a bare
                // "connection refused" from a test, with nothing tying it back to this mapping.
                logger.info("NodeAppServer published on stable host ports (containerPort->hostPort): "
                        + stableHostPorts);
                return candidate;
            } catch (RuntimeException e) {
                if (!isPortConflict(e) || attempt == START_ATTEMPTS) {
                    throw e;
                }
                logger.warn("NodeAppServer boot hit a host-port conflict on attempt " + attempt + " of "
                        + START_ATTEMPTS + "; redrawing ports and retrying. Cause: " + e.getMessage());
                // The half-started container holds nothing useful and would leak; drop it before redrawing.
                try {
                    candidate.stop();
                } catch (RuntimeException stopFailure) {
                    logger.warn("Could not drop the half-started NodeAppServer container (it may leak): "
                            + stopFailure.getMessage());
                }
            }
        }
        throw new IllegalStateException("unreachable: START_ATTEMPTS must be >= 1");
    }

    private GenericContainer<?> buildContainer() {
        drawStableHostPorts();
        GenericContainer<?> candidate = new GenericContainer<>(System.getProperty("node.docker.image.name"))
                //expose the app to the host machine
                .withExposedPorts(exposedPorts)
                .withNetwork(ContainerNetwork.BACKEND_HOME_NETWORK)
                .withNetworkAliases(NETWORK_ALIAS, TENANT_NETWORK_ALIAS)
                .withCreateContainerCmdModifier(this::declareStableHostPorts)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)));
        candidate.withLogConsumer(new JclLogConsumer(logger));
        return candidate;
    }

    private void drawStableHostPorts() {
        stableHostPorts.clear();
        for (Integer containerPort : exposedPorts) {
            stableHostPorts.put(containerPort, freeHostPort());
        }
    }

    /**
     * Publishes every exposed port on an explicitly chosen host port instead of letting docker pick an ephemeral
     * one, so this backend's host-facing address is stable for the container's whole life.
     *
     * <p>Docker reallocates an ephemeral published port on every network connect AND disconnect, and this backend
     * is the only container re-homed after start ({@link #attachToNetwork(Network)}) — repeatedly, as blocks come
     * and go. A declared binding is re-published unchanged instead. Without it, the mapping captured at start goes
     * stale (which is what {@code getMappedPort()} answers from — {@code GenericContainer.getContainerInfo()} reads
     * the inspect taken at start, not a live one), and every host-side read of this backend fails with a bare
     * "connection refused".
     *
     * <p>Re-resolving the port per use would not be sufficient: blocks run concurrently, so a later block's attach
     * invalidates an address an earlier block already holds — in its context, in a payload, in a connection pool.
     * Applied to all published ports rather than only the currently host-read ones, because the failure mode of
     * missing one is silent.
     */
    private void declareStableHostPorts(CreateContainerCmd cmd) {
        List<PortBinding> bindings = new ArrayList<>(stableHostPorts.size());
        stableHostPorts.forEach((containerPort, hostPort) ->
                bindings.add(new PortBinding(Ports.Binding.bindPort(hostPort), new ExposedPort(containerPort))));
        cmd.getHostConfig().withPortBindings(bindings);
    }

    /**
     * Draws a candidate host port by binding port 0 and reading back the kernel's choice.
     *
     * <p>Best-effort, not a reservation: the socket is closed before docker binds the port, and on a VM-backed
     * docker the bind happens inside the VM, whose port usage this cannot observe. It spreads claims out; the bind
     * conflict retry in {@link #startWithPortRetry()} is what makes the result correct.
     */
    private static int freeHostPort() {
        try (ServerSocket probe = new ServerSocket(0)) {
            return probe.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("Could not draw a free host port for the node backend", e);
        }
    }

    /**
     * Recognises the daemon's "that host port is taken" message, walking the cause chain because testcontainers
     * wraps the docker failure. Matched on the message because the docker API returns it as an opaque 500 with no
     * code a caller can switch on; kept narrow, since a broader match would retry genuine boot failures and turn
     * one clear error into four slow ones.
     */
    private static boolean isPortConflict(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message == null) {
                continue;
            }
            String lower = message.toLowerCase(Locale.ROOT);
            if (lower.contains("port is already allocated")
                    || lower.contains("address already in use")
                    || lower.contains("failed programming external connectivity")) {
                return true;
            }
        }
        return false;
    }

    /**
     * The host port this backend's {@code containerPort} is published on, read LIVE from docker.
     *
     * <p>Test-observability only, for the regression test that pins the stability invariant
     * ({@link #declareStableHostPorts(CreateContainerCmd)}): {@code getMappedPort()} answers from the snapshot taken
     * at start, so it would agree with itself and pass even while the real binding moved.
     */
    public int livePublishedPort(int containerPort) {
        Ports.Binding[] bindings = DockerClientFactory.instance().client()
                .inspectContainerCmd(container.getContainerId()).exec()
                .getNetworkSettings().getPorts().getBindings().get(new ExposedPort(containerPort));
        if (bindings == null || bindings.length == 0 || bindings[0].getHostPortSpec() == null) {
            throw new IllegalStateException("Container port " + containerPort + " is not published");
        }
        return Integer.parseInt(bindings[0].getHostPortSpec());
    }

    /**
     * HOST-reachable base URL of the node app listening on {@code containerPort}, e.g.
     * {@code http://localhost:32771}. Needed by backends whose state the test JVM reads back over HTTP (the
     * websub-receiver's delivery introspection, the sse-emitter's stream diagnostics). A caller INSIDE the docker
     * network (an API's endpoint URL) must use {@code http://nodebackend:<containerPort>} instead — only the ports
     * in {@link #exposedPorts} are published to the host.
     *
     * <p>The returned address is STABLE for this container's whole life, including across the network attach and
     * detach that every block performs — see {@link #declareStableHostPorts(CreateContainerCmd)}. It is therefore
     * safe to store one, hand it to another block, or hold it in a connection pool.
     */
    public String getBaseUrl(int containerPort) {
        return String.format("http://%s:%d", container.getHost(), container.getMappedPort(containerPort));
    }

    private static class InstanceHolder {
        private static final NodeAppServer instance = new NodeAppServer();
    }

    public static NodeAppServer getInstance() {
        return NodeAppServer.InstanceHolder.instance;
    }

    /**
     * Multi-homes this shared backend onto a block's private network under {@link #NETWORK_ALIAS}, so that
     * block's APIM gateway resolves {@code nodebackend} to this singleton instead of needing a backend container
     * per block. Safe ONLY because the backend is a stateless upstream that resolves no peer names: traffic is
     * always APIM→backend, so being attached to several block networks at once creates no DNS ambiguity (a
     * container that must resolve a name pointing AT APIM cannot be shared this way — see
     * {@link ContainerNetwork#BACKEND_HOME_NETWORK}).
     *
     * <p>Deliberately NOT best-effort: a failed attach leaves the block's gateway unable to resolve the upstream,
     * which surfaces later as an opaque {@code 303001 endpoint SUSPENDED} on every invocation. Failing the block's
     * boot here names the real cause instead.
     *
     * <p>Called once per block, each with that block's own unique network; {@link #detachFromNetwork(Network)}
     * removes it at block teardown, before the network is closed (Docker refuses to remove a network that still
     * has a container connected).
     */
    public void attachToNetwork(Network network) {
        DockerClientFactory.instance().client().connectToNetworkCmd()
                .withContainerId(container.getContainerId())
                .withNetworkId(network.getId())
                .withContainerNetwork(new com.github.dockerjava.api.model.ContainerNetwork()
                .withAliases(Arrays.asList(NETWORK_ALIAS, TENANT_NETWORK_ALIAS)))
                .exec();
        logger.info("NodeAppServer attached to network " + network.getId() + " as '" + NETWORK_ALIAS
                + "' and '" + TENANT_NETWORK_ALIAS + "'");
    }

    /**
     * Disconnects this shared backend from a block's private network at teardown, so the block's
     * {@link Network#close()} (docker network rm) can succeed. Best-effort by design: a disconnect failure is
     * logged, never thrown, so it cannot mask the block's real teardown outcome — but it IS logged loudly,
     * because a backend left attached is exactly what makes the block's network un-removable and therefore leaked.
     */
    public void detachFromNetwork(Network network) {
        try {
            DockerClientFactory.instance().client().disconnectFromNetworkCmd()
                    .withContainerId(container.getContainerId())
                    .withNetworkId(network.getId())
                    .exec();
            logger.info("NodeAppServer detached from network " + network.getId());
        } catch (RuntimeException e) {
            logger.warn("NodeAppServer detach from network " + network.getId()
                    + " FAILED (continuing teardown; this network will likely leak): " + e.getMessage());
        }
    }

    /**
     * Networks this backend is currently attached to, from a live container inspect. Test-observability only —
     * lets the leak verification assert the backend is left on its home network alone once every block has torn
     * down (any extra entry is a block network that was never detached, i.e. a leak).
     */
    public java.util.Set<String> attachedNetworkNames() {
        return DockerClientFactory.instance().client().inspectContainerCmd(container.getContainerId())
                .exec().getNetworkSettings().getNetworks().keySet();
    }
}
