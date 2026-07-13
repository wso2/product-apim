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

package org.wso2.am.integration.cucumbertests.verification;

import com.sun.net.httpserver.HttpServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.am.integration.cucumbertests.utils.ServerReadiness;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Phase 4.13 verification (Type-B): the readiness gate must key on an HTTP 200 from the health-check, not
 * merely on the port being open - so a container that opens its TCP port but never serves 200 (a partial
 * boot) must be REJECTED, never reported falsely "ready".
 *
 * <p><b>Negative control (the real risk, run against a real container):</b> a genuine Testcontainers
 * {@code node-app-server} - the framework's own backend image, already present locally - listens on its
 * port but returns 404 on the gateway health-check path. Its mapped port is provably open, so a naive
 * {@code Wait.forListeningPort} gate would call it ready; {@link ServerReadiness#awaitReady(String, long)}
 * must nonetheless return {@code false}. (nginx would be the textbook stub, but this environment cannot
 * pull public images; node-app-server gives the same "listens but non-200" shape with no registry pull.)
 *
 * <p><b>Positive control (non-vacuity):</b> a tiny in-JVM HTTP server that answers 200 on every path makes
 * the same gate return {@code true}, proving the rejection above is genuinely driven by the 200 check and
 * is not vacuously always-false.
 *
 * <p>Run via {@code testng-fv-4.13.xml}; {@code verify-4.13.sh} additionally asserts no stub leaks.
 */
public class PartialBootReadinessVerificationTest {

    private static final Log logger = LogFactory.getLog(PartialBootReadinessVerificationTest.class);

    private static final String STUB_IMAGE =
            System.getProperty("node.docker.image.name", "node-app-server:latest");
    private static final int STUB_PORT = 3000;
    private static final String BLOCK_LABEL_KEY = "block";
    private static final String BLOCK_LABEL_VALUE = "fv-4.13";
    /** Short, bounded poll window: the partial-boot path must give up quickly, not after the 300s default. */
    private static final long PROBE_TIMEOUT_MILLIS = 10_000L;

    @Test
    public void readinessGateRejectsPartialBoot() throws Exception {

        // Negative control: a REAL container that LISTENS but never serves 200 on the health-check path.
        try (GenericContainer<?> partial = new GenericContainer<>(STUB_IMAGE)
                .withExposedPorts(STUB_PORT)
                .waitingFor(Wait.forListeningPort())) {
            partial.withLabel(BLOCK_LABEL_KEY, BLOCK_LABEL_VALUE);
            partial.start();

            String host = partial.getHost();
            int port = partial.getMappedPort(STUB_PORT);
            String baseUrl = "http://" + host + ":" + port + "/";

            // The port is provably open - so this is a partial boot, not a refused connection. A naive
            // "wait for listening port" gate would call this ready.
            Assert.assertTrue(isPortOpen(host, port),
                    "precondition: the stub's mapped port should be open (a partial boot, not refused)");

            // The readiness gate must still reject it, because the health-check path never returns 200.
            Assert.assertFalse(ServerReadiness.awaitReady(baseUrl, PROBE_TIMEOUT_MILLIS),
                    "readiness gate falsely reported ready for a listening-but-non-200 (partial boot) server");
            logger.info("Phase 4.13 negative control passed: partial boot at " + baseUrl
                    + " correctly rejected");
        }

        // Positive control: same gate, but now the health-check path returns 200.
        HttpServer ready = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        ready.createContext("/", exchange -> {
            byte[] body = "ready".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        ready.start();
        try {
            String baseUrl = "http://127.0.0.1:" + ready.getAddress().getPort() + "/";
            Assert.assertTrue(ServerReadiness.awaitReady(baseUrl, PROBE_TIMEOUT_MILLIS),
                    "readiness gate rejected a server that serves 200 on the health-check path");
            logger.info("Phase 4.13 positive control passed: 200-serving server at " + baseUrl
                    + " accepted");
        } finally {
            ready.stop(0);
        }
    }

    private boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
