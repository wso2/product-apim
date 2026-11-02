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

package org.wso2.am.integration.cucumbertests.verification.steps;

import io.cucumber.java.en.Then;
import org.testcontainers.containers.Network;
import org.testng.Assert;
import org.wso2.am.testcontainers.NodeAppServer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Framework verification for the shared backend's HOST-facing address stability across block network churn.
 *
 * <p>Framework-only: this drives {@code NodeAppServer}'s attach/detach directly with throwaway networks rather
 * than any product API, so it belongs here and not in a product step class.
 */
public class BackendPortStabilitySteps {

    /** Connect timeout for the reachability probe — local docker, so a slow answer is a failure, not patience. */
    private static final int PROBE_TIMEOUT_MILLIS = 5_000;
    private static final int HTTP_PROBE_TIMEOUT_MILLIS = 2_000;
    private static final long CONTINUOUS_PROBE_INTERVAL_MILLIS = 10;

    @Then("The shared backend's published host port for container port {int} survives {int} network attach and "
            + "detach cycles")
    public void backendHostPortSurvivesRehoming(int containerPort, int cycles) throws IOException {

        NodeAppServer backend = NodeAppServer.getInstance();
        int atLaunch = backend.livePublishedPort(containerPort);

        assertCachedAgreesWithLive(backend, containerPort, atLaunch, "at launch");
        assertReachable(backend, containerPort, "at launch");

        AtomicBoolean probing = new AtomicBoolean(true);
        AtomicInteger successfulProbes = new AtomicInteger();
        AtomicReference<Throwable> probeFailure = new AtomicReference<>();
        URI probeUri = URI.create(backend.getBaseUrl(containerPort)
                + "/runtime/process-instances?debugInfo=startRequest");
        // Use the BPMN mock's actual request-inspection route. Repeated HTTP reads exercise the same host-forwarded
        // port the workflow scenario reads while other blocks mutate the shared backend's network attachments.
        Assert.assertEquals(sendProbe(probeUri), 200, "BPMN mock should answer before network churn");

        Thread probeThread = new Thread(() -> {
            while (probing.get()) {
                try {
                    int status = sendProbe(probeUri);
                    if (status != 200) {
                        throw new IOException("BPMN mock returned HTTP " + status + " from " + probeUri);
                    }
                    successfulProbes.incrementAndGet();
                    Thread.sleep(CONTINUOUS_PROBE_INTERVAL_MILLIS);
                } catch (InterruptedException e) {
                    if (probing.get()) {
                        probeFailure.compareAndSet(null, e);
                    }
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    if (probing.get()) {
                        probeFailure.compareAndSet(null, e);
                    }
                    return;
                }
            }
        }, "node-backend-host-port-probe");
        probeThread.setDaemon(true);
        probeThread.start();

        try {
            for (int cycle = 1; cycle <= cycles; cycle++) {
                Network probeNetwork = Network.newNetwork();
                boolean attached = false;
                boolean detached = false;
                try {
                    backend.attachToNetwork(probeNetwork);
                    attached = true;
                    Assert.assertEquals(backend.livePublishedPort(containerPort), atLaunch,
                            "The backend's published host port for container port " + containerPort
                                    + " CHANGED after attaching to a block network (cycle " + cycle + ").");
                    assertCachedAgreesWithLive(backend, containerPort, atLaunch, "after attach, cycle " + cycle);
                    assertReachable(backend, containerPort, "after attach, cycle " + cycle);

                    backend.detachFromNetwork(probeNetwork);
                    detached = true;
                    Assert.assertEquals(backend.livePublishedPort(containerPort), atLaunch,
                            "The backend's published host port for container port " + containerPort
                                    + " CHANGED after detaching from a block network (cycle " + cycle + ").");
                    assertCachedAgreesWithLive(backend, containerPort, atLaunch, "after detach, cycle " + cycle);
                    assertReachable(backend, containerPort, "after detach, cycle " + cycle);
                } finally {
                    // A failed post-attach assertion skips the detach above; still release the shared backend from
                    // this network so it can be removed and does not leak into the rest of the run.
                    if (attached && !detached) {
                        backend.detachFromNetwork(probeNetwork);
                    }
                    probeNetwork.close();
                }
            }
        } finally {
            probing.set(false);
            probeThread.interrupt();
            try {
                probeThread.join(HTTP_PROBE_TIMEOUT_MILLIS + 1_000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Assert.fail("Interrupted while stopping the shared-backend reachability probe", e);
            }
        }

        Assert.assertFalse(probeThread.isAlive(), "The shared-backend reachability probe did not stop");
        Assert.assertNull(probeFailure.get(), "The BPMN mock became unreachable during network churn");
        Assert.assertTrue(successfulProbes.get() > 0, "The concurrent reachability probe issued no requests");

        Assert.assertEquals(backend.livePublishedPort(containerPort), atLaunch,
                "The backend's published host port for container port " + containerPort + " must be stable for the "
                        + "container's whole life, but it ended at a different value than it launched with.");
    }

    private int sendProbe(URI uri) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setConnectTimeout(HTTP_PROBE_TIMEOUT_MILLIS);
        connection.setReadTimeout(HTTP_PROBE_TIMEOUT_MILLIS);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Connection", "close");
        try {
            return connection.getResponseCode();
        } finally {
            connection.disconnect();
        }
    }

    /**
     * The cached mapping is what {@code getBaseUrl} builds every test URL from; the live binding is what drifted.
     * They must be the same value, or a URL handed to a block is already dead when it is used.
     */
    private void assertCachedAgreesWithLive(NodeAppServer backend, int containerPort, int expected, String when) {
        int cached = URI.create(backend.getBaseUrl(containerPort)).getPort();
        Assert.assertEquals(cached, expected, "The backend URL the tests would build (" + when + ") points at host "
                + "port " + cached + ", but docker publishes container port " + containerPort + " on " + expected
                + ". A test using this address would fail with a bare 'connection refused'.");
    }

    /** The address must not merely read back correct — it must still answer, which is what actually broke. */
    private void assertReachable(NodeAppServer backend, int containerPort, String when) {
        URI url = URI.create(backend.getBaseUrl(containerPort));
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(url.getHost(), url.getPort()), PROBE_TIMEOUT_MILLIS);
        } catch (IOException e) {
            Assert.fail("The backend's published address " + url + " (" + when + ") did not accept a connection: "
                    + e + ". The port mapping is stale or the app on container port " + containerPort
                    + " is not listening.");
        }
    }
}
