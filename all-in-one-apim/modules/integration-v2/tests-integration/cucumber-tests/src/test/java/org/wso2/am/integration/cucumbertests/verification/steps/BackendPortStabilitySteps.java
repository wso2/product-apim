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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;

/**
 * Framework verification for the shared backend's HOST-facing address stability across block network churn.
 *
 * <p>Framework-only: this drives {@code NodeAppServer}'s attach/detach directly with throwaway networks rather
 * than any product API, so it belongs here and not in a product step class.
 */
public class BackendPortStabilitySteps {

    /** Connect timeout for the reachability probe — local docker, so a slow answer is a failure, not patience. */
    private static final int PROBE_TIMEOUT_MILLIS = 5_000;

    @Then("The shared backend's published host port for container port {int} survives {int} network attach and "
            + "detach cycles")
    public void backendHostPortSurvivesRehoming(int containerPort, int cycles) {

        NodeAppServer backend = NodeAppServer.getInstance();
        int atLaunch = backend.livePublishedPort(containerPort);

        assertCachedAgreesWithLive(backend, containerPort, atLaunch, "at launch");
        assertReachable(backend, containerPort, "at launch");

        for (int cycle = 1; cycle <= cycles; cycle++) {
            Network probeNetwork = Network.newNetwork();
            try {
                backend.attachToNetwork(probeNetwork);
                Assert.assertEquals(backend.livePublishedPort(containerPort), atLaunch,
                        "The backend's published host port for container port " + containerPort + " CHANGED after "
                                + "attaching to a block network (cycle " + cycle + "). Docker reallocates an "
                                + "ephemeral published port on every network connect; the binding must be declared "
                                + "explicitly so it is re-published unchanged.");
                assertCachedAgreesWithLive(backend, containerPort, atLaunch, "after attach, cycle " + cycle);
                assertReachable(backend, containerPort, "after attach, cycle " + cycle);

                backend.detachFromNetwork(probeNetwork);
                Assert.assertEquals(backend.livePublishedPort(containerPort), atLaunch,
                        "The backend's published host port for container port " + containerPort + " CHANGED after "
                                + "detaching from a block network (cycle " + cycle + "). Detach reallocates the port "
                                + "just as attach does.");
                assertCachedAgreesWithLive(backend, containerPort, atLaunch, "after detach, cycle " + cycle);
                assertReachable(backend, containerPort, "after detach, cycle " + cycle);
            } finally {
                // Closed even on assertion failure: a leaked probe network outlives the run and, with the backend
                // still attached, cannot be removed afterwards either.
                probeNetwork.close();
            }
        }

        Assert.assertEquals(backend.livePublishedPort(containerPort), atLaunch,
                "The backend's published host port for container port " + containerPort + " must be stable for the "
                        + "container's whole life, but it ended at a different value than it launched with.");
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
