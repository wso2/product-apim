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

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.am.integration.cucumbertests.utils.ModulePathResolver;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.am.testcontainers.DynamicApimContainer;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Phase 1.4 verification: misuse and abnormal-termination behavior of {@link DynamicApimContainer}.
 * <ol>
 *   <li><b>Misuse:</b> calling {@code getMappedPort()} before {@code start()} fails fast with a clear
 *       {@link IllegalStateException} — never a silent {@code 0} or a wrong port.</li>
 *   <li><b>Abnormal termination:</b> a hard {@code docker kill} (not a graceful {@code stop()})
 *       releases the host port (no host-port leak) but leaves the container record in {@code exited}
 *       state — i.e. abnormal exit requires an explicit prune. The test removes the record itself so
 *       the run stays self-cleaning.</li>
 * </ol>
 * Run via {@code testng-fv-1.4.xml}; {@code verify-1.4.sh} additionally asserts no Docker leaks post-exit.
 */
public class DynamicApimContainerMisuseVerificationTest {

    private static final Log logger =
            LogFactory.getLog(DynamicApimContainerMisuseVerificationTest.class);
    private static final String VERIFY_LABEL_KEY = "verify-step";
    private static final String VERIFY_LABEL_VALUE = "1.4";

    @Test
    public void getMappedPortBeforeStartFailsFast() throws Exception {
        String tomlContent = readDefaultToml();
        DynamicApimContainer container = new DynamicApimContainer("verify-1.4-misuse", tomlContent);
        try {
            int port = container.getMappedPort(Constants.HTTPS_PORT);
            Assert.fail("getMappedPort before start() should throw, but returned " + port);
        } catch (IllegalStateException e) {
            Assert.assertNotNull(e.getMessage(), "fail-fast exception must carry a clear message");
            Assert.assertFalse(e.getMessage().isBlank(), "fail-fast exception message must not be blank");
            logger.info("getMappedPort before start() failed fast as expected: " + e.getMessage());
        }
    }

    @Test
    public void abnormalKillReleasesHostPortButLeavesContainerRecord() throws Exception {
        String tomlContent = readDefaultToml();
        DynamicApimContainer container = new DynamicApimContainer("verify-1.4-kill", tomlContent);
        container.withLabel(VERIFY_LABEL_KEY, VERIFY_LABEL_VALUE);

        String containerId = null;
        DockerClient docker = null;
        try {
            container.start();
            docker = container.getDockerClient();
            containerId = container.getContainerId();
            String host = container.getHost();
            int hostPort = container.getMappedPort(Constants.HTTPS_PORT);
            Assert.assertTrue(isPortOpen(host, hostPort),
                    "mapped host port should be open while the container runs");

            // Abnormal termination: hard kill, bypassing the graceful stop()/Ryuk path.
            docker.killContainerCmd(containerId).exec();
            logger.info("Hard-killed container " + containerId + " (host port was " + host + ":"
                    + hostPort + ")");

            // The host port must be released even on an abnormal exit (no host-port leak).
            Assert.assertTrue(pollUntilPortReleased(host, hostPort),
                    "host port " + host + ":" + hostPort + " still accepts connections after docker kill");

            // The container record, however, survives a kill — documenting the manual-prune expectation.
            List<Container> records = docker.listContainersCmd()
                    .withShowAll(true)
                    .withIdFilter(Collections.singleton(containerId))
                    .exec();
            Assert.assertEquals(records.size(), 1,
                    "a hard-killed container should remain as an exited record until explicitly pruned");
            logger.info("Killed container " + containerId + " remains as an exited record (state="
                    + records.get(0).getState() + ") — manual prune required");
        } finally {
            // Self-clean: remove the killed record so the run leaves no leaks.
            if (docker != null && containerId != null) {
                try {
                    docker.removeContainerCmd(containerId).withForce(true).exec();
                } catch (Exception e) {
                    logger.warn("Error force-removing killed verify-1.4 container " + containerId, e);
                }
            }
        }
    }

    private String readDefaultToml() throws Exception {
        String moduleDir = ModulePathResolver.getModuleDir(DynamicApimContainerMisuseVerificationTest.class);
        // `basic` is an overlay — merge onto the distribution as the production block lane does.
        return Utils.resolveDefaultToml(moduleDir);
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
