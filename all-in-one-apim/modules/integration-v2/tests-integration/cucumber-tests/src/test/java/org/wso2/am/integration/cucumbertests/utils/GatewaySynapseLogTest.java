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

package org.wso2.am.integration.cucumbertests.utils;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.wso2.am.testcontainers.ApimRuntime;
import org.wso2.am.testcontainers.ContainerLogHistory;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

/** Marked artifact checks require a new event and retain their boundary through rollover. */
public class GatewaySynapseLogTest {
    private static final String EVENT = "API : UniqueAPI:v1 " + GatewaySynapseLog.SYNAPSE_ADDED + "\n";

    @AfterMethod
    public void clearContext() {
        TestContext.clear();
    }

    @Test
    public void oldArtifactCannotSatisfyTheMarkAndNewArtifactCan() {
        AtomicReference<ContainerLogHistory.Snapshot> history = runtime(EVENT);
        GatewaySynapseLog.mark("id");
        Assert.assertTrue(GatewaySynapseLog.artifactAddedSinceMark("id", "UniqueAPI:v1") instanceof HealGate.NotReady);
        history.set(new ContainerLogHistory.Snapshot("boot:carbon", EVENT + EVENT, true));
        Assert.assertTrue(GatewaySynapseLog.artifactAddedSinceMark("id", "UniqueAPI:v1") instanceof HealGate.Ready);
    }

    @Test
    public void restartRejectsAnOldMark() {
        AtomicReference<ContainerLogHistory.Snapshot> history = runtime(EVENT);
        GatewaySynapseLog.mark("id");
        history.set(new ContainerLogHistory.Snapshot("new-boot:carbon", EVENT, true));
        Assert.assertTrue(GatewaySynapseLog.artifactAddedSinceMark("id", "UniqueAPI:v1") instanceof HealGate.Fatal);
    }

    @Test
    public void unreadableHistoryCannotTurnAMarkedDeployReady() {
        AtomicReference<ContainerLogHistory.Snapshot> history = runtime(EVENT);
        GatewaySynapseLog.mark("id");
        history.set(null);
        Assert.assertTrue(GatewaySynapseLog.artifactAddedSinceMark("id", "UniqueAPI:v1") instanceof HealGate.NotReady);
    }

    @Test
    public void missingBootCannotProduceAMark() {
        AtomicReference<ContainerLogHistory.Snapshot> history = runtime(EVENT);
        history.set(new ContainerLogHistory.Snapshot("boot:carbon", "", false));
        Assert.expectThrows(IllegalStateException.class, () -> GatewaySynapseLog.mark("id"));
    }

    @Test
    public void transientTransportCanRecoverBeforeMarkingAndAfterDeploying() {
        java.util.concurrent.atomic.AtomicInteger reads = new java.util.concurrent.atomic.AtomicInteger();
        ApimRuntime runtime = (ApimRuntime) Proxy.newProxyInstance(ApimRuntime.class.getClassLoader(),
                new Class<?>[]{ApimRuntime.class}, (proxy, method, arguments) -> {
                    int read = reads.incrementAndGet();
                    if (read == 1 || read == 3) {
                        throw new ContainerLogHistory.ReadUnavailableException("docker exec transient", null);
                    }
                    return new ContainerLogHistory.Snapshot("boot:carbon", read == 2 ? EVENT : EVENT + EVENT, true);
                });
        TestContext.set("blockApimContainer", runtime);
        GatewaySynapseLog.mark("id", 1000, 0);
        Assert.assertEquals(reads.get(), 2);
        Assert.assertTrue(GatewaySynapseLog.artifactAddedSinceMark("id", "UniqueAPI:v1") instanceof HealGate.NotReady);
        Assert.assertTrue(GatewaySynapseLog.artifactAddedSinceMark("id", "UniqueAPI:v1") instanceof HealGate.Ready);
    }

    @Test
    public void missingMarkBoundaryIsNotPolledAsATransportFailure() {
        runtime(EVENT).set(new ContainerLogHistory.Snapshot("boot:carbon", "", false));
        IllegalStateException failure = Assert.expectThrows(IllegalStateException.class,
                () -> GatewaySynapseLog.mark("id", 1000, 0));
        Assert.assertTrue(failure.getMessage().contains("Starting WSO2 Carbon"));
    }

    private AtomicReference<ContainerLogHistory.Snapshot> runtime(String content) {
        AtomicReference<ContainerLogHistory.Snapshot> history = new AtomicReference<>(
                new ContainerLogHistory.Snapshot("boot:carbon", content, true));
        ApimRuntime runtime = (ApimRuntime) Proxy.newProxyInstance(ApimRuntime.class.getClassLoader(),
                new Class<?>[]{ApimRuntime.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("readGatewayLogHistory")) {
                        return history.get();
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        TestContext.set("blockApimContainer", runtime);
        return history;
    }
}
