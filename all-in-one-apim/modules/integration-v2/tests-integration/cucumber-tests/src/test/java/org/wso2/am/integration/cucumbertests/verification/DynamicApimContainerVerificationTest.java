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

/**
 * Phase 1.1 verification (Type-B): proves {@link DynamicApimContainer} maps the canonical APIM
 * ports to ephemeral host ports, exposes well-formed URLs, becomes ready, and releases its host
 * ports on stop. Run via {@code testng-framework-verification.xml}; the {@code verify-1.1.sh}
 * wrapper additionally asserts no Docker containers leak after this JVM exits.
 */
public class DynamicApimContainerVerificationTest {

    private static final Log logger = LogFactory.getLog(DynamicApimContainerVerificationTest.class);
    private static final String VERIFY_LABEL_KEY = "verify-step";
    private static final String VERIFY_LABEL_VALUE = "1.1";

    @Test
    public void verifyDynamicPortLifecycle() throws Exception {

        String moduleDir = ModulePathResolver.getModuleDir(DynamicApimContainerVerificationTest.class);
        // `basic` is an overlay, not a complete config — merge it onto the distribution the same way the
        // production block lane does, otherwise the container boots under-configured and exits at startup.
        String tomlContent = Utils.resolveDefaultToml(moduleDir);

        DynamicApimContainer container = new DynamicApimContainer("verify-1.1", tomlContent);
        container.withLabel(VERIFY_LABEL_KEY, VERIFY_LABEL_VALUE);

        String host;
        int servletHttpsMapped;
        try {
            container.start();

            host = container.getHost();
            servletHttpsMapped = container.getMappedPort(Constants.HTTPS_PORT);

            // 1) Canonical ports are mapped to ephemeral host ports (not equal to the internal port).
            Assert.assertNotEquals(servletHttpsMapped, Constants.HTTPS_PORT,
                    "servlet HTTPS port was not remapped to an ephemeral host port");
            Assert.assertNotEquals(container.getMappedPort(Constants.HTTP_PORT), Constants.HTTP_PORT,
                    "servlet HTTP port was not remapped");
            Assert.assertNotEquals(container.getMappedPort(Constants.GATEWAY_HTTPS_PORT),
                    Constants.GATEWAY_HTTPS_PORT, "gateway HTTPS port was not remapped");
            Assert.assertNotEquals(container.getMappedPort(Constants.GATEWAY_HTTP_PORT),
                    Constants.GATEWAY_HTTP_PORT, "gateway HTTP port was not remapped");

            // 2) All four accessor URLs are well-formed.
            for (String url : new String[]{container.getServletHttpsUrl(), container.getServletHttpUrl(),
                    container.getGatewayHttpsUrl(), container.getGatewayHttpUrl()}) {
                URI parsed = URI.create(url);
                Assert.assertNotNull(parsed.getHost(), "URL has no host: " + url);
                Assert.assertTrue(parsed.getPort() > 0, "URL has no port: " + url);
            }

            // Record the mapped servlet-HTTPS host port so the wrapper can re-check release post-exit.
            Files.writeString(Paths.get(moduleDir, "target", "verify-1.1-servlet-https-port.txt"),
                    host + ":" + servletHttpsMapped);

            // 3) The server becomes ready: health-check returns 200 within the startup window.
            String healthUrl = Utils.getGatewayHealthCheckURL(container.getServletHttpsUrl());
            Assert.assertTrue(pollUntilHealthy(healthUrl),
                    "server-startup-healthcheck never returned 200 at " + healthUrl);
        } finally {
            container.stop();
        }

        // 4) The mapped host port is released after stop. Docker frees the host-side binding
        // asynchronously once stop() returns, so poll (release is eventually-consistent).
        Assert.assertTrue(pollUntilPortReleased(host, servletHttpsMapped),
                "host port " + servletHttpsMapped + " still accepts connections after stop()");

        logger.info("Phase 1.1 in-JVM assertions passed (mapped servlet-https port was "
                + servletHttpsMapped + ")");
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
