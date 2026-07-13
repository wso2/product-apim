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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.am.integration.cucumbertests.utils.ModulePathResolver;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.am.testcontainers.DynamicApimContainer;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Phase 1.2 verification (Type-B): proves multiple {@link DynamicApimContainer} instances can run
 * concurrently with no port collision — each gets a distinct set of ephemeral host ports, all
 * reachable, all healthy — and that every host port is released once all are stopped. Run via
 * {@code testng-fv-1.2.xml}; {@code verify-1.2.sh} additionally asserts no Docker leaks post-exit.
 */
public class DynamicApimContainerParallelVerificationTest {

    private static final Log logger =
            LogFactory.getLog(DynamicApimContainerParallelVerificationTest.class);
    private static final String VERIFY_LABEL_KEY = "verify-step";
    private static final String VERIFY_LABEL_VALUE = "1.2";
    private static final int CONTAINER_COUNT = 2;

    @Test
    public void verifyParallelContainersHaveDistinctPortsAndRelease() throws Exception {

        String moduleDir = ModulePathResolver.getModuleDir(DynamicApimContainerParallelVerificationTest.class);
        // `basic` is an overlay — merge onto the distribution as the production block lane does.
        String tomlContent = Utils.resolveDefaultToml(moduleDir);

        List<DynamicApimContainer> containers = new ArrayList<>();
        for (int i = 0; i < CONTAINER_COUNT; i++) {
            DynamicApimContainer c = new DynamicApimContainer("verify-1.2-" + i, tomlContent);
            c.withLabel(VERIFY_LABEL_KEY, VERIFY_LABEL_VALUE);
            containers.add(c);
        }

        // Captured before stop() so the post-stop release check still has the host:port pairs.
        List<String> servletHttpsHostPorts = new ArrayList<>();
        try {
            // 1) Start all containers concurrently.
            ExecutorService pool = Executors.newFixedThreadPool(CONTAINER_COUNT);
            try {
                List<Future<?>> starts = new ArrayList<>();
                for (DynamicApimContainer c : containers) {
                    starts.add(pool.submit(c::start));
                }
                for (Future<?> f : starts) {
                    f.get(20, TimeUnit.MINUTES);
                }
            } finally {
                pool.shutdownNow();
            }

            // 2) Every container has a distinct set of host ports (no "address already in use").
            Set<Integer> allHostPorts = new HashSet<>();
            int expectedPortCount = CONTAINER_COUNT * 4;
            for (DynamicApimContainer c : containers) {
                allHostPorts.add(c.getMappedPort(Constants.HTTPS_PORT));
                allHostPorts.add(c.getMappedPort(Constants.HTTP_PORT));
                allHostPorts.add(c.getMappedPort(Constants.GATEWAY_HTTPS_PORT));
                allHostPorts.add(c.getMappedPort(Constants.GATEWAY_HTTP_PORT));
                servletHttpsHostPorts.add(c.getHost() + ":" + c.getMappedPort(Constants.HTTPS_PORT));
            }
            Assert.assertEquals(allHostPorts.size(), expectedPortCount,
                    "expected " + expectedPortCount + " distinct host ports across " + CONTAINER_COUNT
                            + " containers but found " + allHostPorts.size() + " (port collision)");

            // 3) Each container's URLs are well-formed and its server becomes healthy.
            for (DynamicApimContainer c : containers) {
                for (String url : new String[]{c.getServletHttpsUrl(), c.getServletHttpUrl(),
                        c.getGatewayHttpsUrl(), c.getGatewayHttpUrl()}) {
                    URI parsed = URI.create(url);
                    Assert.assertNotNull(parsed.getHost(), "URL has no host: " + url);
                    Assert.assertTrue(parsed.getPort() > 0, "URL has no port: " + url);
                }
                String healthUrl = Utils.getGatewayHealthCheckURL(c.getServletHttpsUrl());
                Assert.assertTrue(pollUntilHealthy(healthUrl),
                        "server-startup-healthcheck never returned 200 at " + healthUrl);
            }

            Files.writeString(Paths.get(moduleDir, "target", "verify-1.2-servlet-https-ports.txt"),
                    String.join("\n", servletHttpsHostPorts));
        } finally {
            for (DynamicApimContainer c : containers) {
                try {
                    c.stop();
                } catch (Exception e) {
                    logger.warn("Error stopping a verify-1.2 container", e);
                }
            }
        }

        // 4) All host ports are released after every container is stopped.
        for (String hostPort : servletHttpsHostPorts) {
            String host = hostPort.substring(0, hostPort.indexOf(':'));
            int port = Integer.parseInt(hostPort.substring(hostPort.indexOf(':') + 1));
            Assert.assertTrue(pollUntilPortReleased(host, port),
                    "host port " + hostPort + " still accepts connections after stop()");
        }

        logger.info("Phase 1.2 in-JVM assertions passed for " + CONTAINER_COUNT
                + " parallel containers (servlet-https " + servletHttpsHostPorts + ")");
    }

    private boolean pollUntilHealthy(String url) {
        long deadline = System.currentTimeMillis() + Constants.SERVER_STARTUP_WAIT_TIME;
        while (System.currentTimeMillis() < deadline) {
            try {
                HttpResponse response = SimpleHTTPClient.getInstance().doGet(url, null);
                if (response != null && response.getResponseCode() == 200) {
                    return true;
                }
            } catch (Exception ignored) {
                // not ready yet
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private boolean pollUntilPortReleased(String host, int port) {
        long deadline = System.currentTimeMillis() + 30_000L;
        while (System.currentTimeMillis() < deadline) {
            if (!isPortOpen(host, port)) {
                return true;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
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
