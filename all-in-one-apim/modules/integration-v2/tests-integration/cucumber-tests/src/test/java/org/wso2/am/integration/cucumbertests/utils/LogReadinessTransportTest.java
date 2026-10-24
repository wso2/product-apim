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

package org.wso2.am.integration.cucumbertests.utils;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.wso2.am.testcontainers.ApimRuntime;
import org.wso2.am.testcontainers.ContainerLogHistory;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

/** Log-based readiness polls keep polling through a transient log read and stop on an interrupted one. */
public class LogReadinessTransportTest {

    private static final String LISTENER_STARTED = "[2026-10-23 10:00:00,000]  INFO {PassThroughHttpListener} -"
            + " Pass-through HTTPS Listener started on 0:0:0:0:0:0:0:0:8243\n";
    private static final String THROTTLE_PIPELINE_READY =
            "[2026-10-23 10:00:00,000]  INFO {CarbonCoreActivator} - WSO2 Carbon started in 40 sec\n"
            + "[2026-10-23 10:00:00,001]  INFO {EventPublisherConfigurationFilesystemInvoker} - Event Publisher"
            + " configuration successfully deployed and in active state: jmsEventPublisher2\n"
            + "[2026-10-23 10:00:00,002]  INFO {JMSConnectionFactory} - Connection attempt: 1 for JMS Provider for"
            + " listener: Siddhi-JMS-Consumer#throttleData was successful!\n"
            + "[2026-10-23 10:00:00,003]  INFO {JMSListener} - Started to listen on destination : throttleData\n";
    private static final String TENANT_PLAN_DEPLOYED = "[2026-10-23 10:00:00,000]  INFO {EventProcessorService} -"
            + " Execution plan is deployed successfully and in active state  : tenant1.com_app_10PerMin\n";

    @AfterMethod
    public void clearInterrupt() {
        Thread.interrupted();
    }

    @Test
    public void listenerPollSurvivesTransientRead() throws InterruptedException {
        AtomicInteger reads = new AtomicInteger();
        ListenerSslReloadReadiness.Result result =
                ListenerSslReloadReadiness.await(runtime(reads, LISTENER_STARTED, transientFailure()), 30);
        Assert.assertEquals(result.state(), ListenerSslReloadReadiness.State.READY, result.diagnostics());
        Assert.assertEquals(reads.get(), 2);
    }

    @Test
    public void listenerPollStopsOnInterruptedRead() {
        AtomicInteger reads = new AtomicInteger();
        ApimRuntime runtime = runtime(reads, LISTENER_STARTED, interruptedFailure());
        Assert.expectThrows(ContainerLogHistory.ReadUnavailableException.class,
                () -> ListenerSslReloadReadiness.await(runtime, 30));
        Assert.assertEquals(reads.get(), 1);
    }

    @Test
    public void throttlePollSurvivesTransientRead() throws InterruptedException {
        AtomicInteger reads = new AtomicInteger();
        ThrottleDataReadiness.Result result =
                ThrottleDataReadiness.await(runtime(reads, THROTTLE_PIPELINE_READY, transientFailure()), 30);
        Assert.assertTrue(result.ready(), result.diagnostics());
    }

    @Test
    public void throttlePollStopsOnInterruptedRead() {
        AtomicInteger reads = new AtomicInteger();
        ApimRuntime runtime = runtime(reads, THROTTLE_PIPELINE_READY, interruptedFailure());
        Assert.expectThrows(ContainerLogHistory.ReadUnavailableException.class,
                () -> ThrottleDataReadiness.await(runtime, 30));
        Assert.assertEquals(reads.get(), 1);
    }

    @Test
    public void tenantPolicyPollSurvivesTransientRead() throws InterruptedException {
        AtomicInteger reads = new AtomicInteger();
        TenantPolicyDeployReadiness.Result result = TenantPolicyDeployReadiness.await(
                runtime(reads, "readTrafficManagerLogHistory", TENANT_PLAN_DEPLOYED, transientFailure()),
                "tenant1.com", 30);
        Assert.assertTrue(result.settled(), result.diagnostics());
        Assert.assertEquals(result.plans(), 1);
    }

    @Test
    public void tenantPolicyPollStopsOnInterruptedRead() {
        AtomicInteger reads = new AtomicInteger();
        ApimRuntime runtime =
                runtime(reads, "readTrafficManagerLogHistory", TENANT_PLAN_DEPLOYED, interruptedFailure());
        Assert.expectThrows(ContainerLogHistory.ReadUnavailableException.class,
                () -> TenantPolicyDeployReadiness.await(runtime, "tenant1.com", 30));
        Assert.assertEquals(reads.get(), 1);
    }

    private static ContainerLogHistory.ReadUnavailableException transientFailure() {
        return new ContainerLogHistory.ReadUnavailableException("docker exec transient", null);
    }

    private static ContainerLogHistory.ReadUnavailableException interruptedFailure() {
        return new ContainerLogHistory.ReadUnavailableException("Interrupted capturing log history",
                new InterruptedException());
    }

    /** The first gateway-log read fails with {@code firstRead}; later reads return {@code log}. */
    private static ApimRuntime runtime(AtomicInteger reads, String log,
                                       ContainerLogHistory.ReadUnavailableException firstRead) {
        return runtime(reads, "readGatewayLogHistory", log, firstRead);
    }

    /** The first {@code reader} call fails with {@code firstRead}; later calls return {@code log}. */
    private static ApimRuntime runtime(AtomicInteger reads, String reader, String log,
                                       ContainerLogHistory.ReadUnavailableException firstRead) {
        return (ApimRuntime) Proxy.newProxyInstance(ApimRuntime.class.getClassLoader(),
                new Class<?>[]{ApimRuntime.class}, (proxy, method, arguments) -> {
                    if (!method.getName().equals(reader)) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    if (reads.incrementAndGet() == 1) {
                        throw firstRead;
                    }
                    return new ContainerLogHistory.Snapshot("boot:carbon", log, true);
                });
    }
}
