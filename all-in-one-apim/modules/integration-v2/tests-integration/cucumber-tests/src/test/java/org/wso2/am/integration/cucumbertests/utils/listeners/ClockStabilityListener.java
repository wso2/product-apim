/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.am.integration.cucumbertests.utils.listeners;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.IExecutionListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestResult;
import org.wso2.am.integration.cucumbertests.utils.ModulePathResolver;
import org.wso2.am.testcontainers.DockerHostClockMonitor;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** A single observer spans all parallel blocks, including their startup and teardown. */
public final class ClockStabilityListener implements ISuiteListener, IInvokedMethodListener, IExecutionListener {
    private static final Log LOG = LogFactory.getLog(ClockStabilityListener.class);
    private static final String KEY = ClockStabilityListener.class.getName();
    private static final String TOPOLOGY_PARAMETER = "apim.topology";
    private final AtomicReference<RuntimeException> executionFailure = new AtomicReference<>();
    private final AtomicBoolean reportedByTest = new AtomicBoolean();

    @Override
    public void onStart(ISuite suite) {
        Path directory = Path.of(ModulePathResolver.getModuleDir(ClockStabilityListener.class),
                "target", "logs", "host-clock", UUID.randomUUID().toString());
        try {
            boolean distributedTopology = "distributed".equalsIgnoreCase(
                    suite.getXmlSuite().getParameter(TOPOLOGY_PARAMETER));
            DockerHostClockMonitor monitor = new DockerHostClockMonitor(directory, distributedTopology);
            suite.setAttribute(KEY, monitor);
            String marker = System.getProperty("apim.clock.prepared.file");
            monitor.start(marker == null || marker.isBlank() ? null : Path.of(marker));
            LOG.info("Docker host clock preflight passed; observer=" + monitor.containerId() + "; evidence="
                    + directory);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("APIM test infrastructure clock preflight failed; evidence=" + directory, e);
        }
    }

    public static void assertHealthy(ISuite suite) {
        Object value = suite.getAttribute(KEY);
        if (value instanceof DockerHostClockMonitor monitor) {
            monitor.assertHealthy();
        }
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult result) {
        if (!method.isTestMethod()) {
            return;
        }
        try {
            assertHealthy(result.getTestContext().getSuite());
        } catch (IllegalStateException failure) {
            LOG.error(failure.getMessage());
            if (result.getThrowable() == null) {
                result.setThrowable(failure);
            } else {
                result.getThrowable().addSuppressed(failure);
            }
            result.setStatus(ITestResult.FAILURE);
            reportedByTest.set(true);
        }
    }

    @Override
    public void onFinish(ISuite suite) {
        Object value = suite.getAttribute(KEY);
        if (!(value instanceof DockerHostClockMonitor monitor)) {
            return;
        }
        RuntimeException clockFailure = null;
        try {
            monitor.assertHealthy();
        } catch (RuntimeException e) {
            clockFailure = e;
        }
        try {
            monitor.close();
            monitor.assertHealthy();
        } catch (Exception e) {
            if (clockFailure == null) {
                clockFailure = new IllegalStateException("Could not finish Docker host clock monitoring", e);
            } else {
                clockFailure.addSuppressed(e);
            }
        }
        if (clockFailure != null) {
            LOG.error(clockFailure.getMessage(), clockFailure);
            executionFailure.compareAndSet(null, clockFailure);
            return;
        }
        LOG.info("Docker host clock monitoring completed successfully");
    }

    @Override
    public void onExecutionFinish() {
        // TestNG generates its reports before this callback. Preserve them even when a clock failure occurs
        // after the last test (e.g. during block teardown), so a build cannot pass with a late invalid clock.
        if (executionFailure.get() != null && !reportedByTest.get()) {
            throw executionFailure.get();
        }
    }
}
