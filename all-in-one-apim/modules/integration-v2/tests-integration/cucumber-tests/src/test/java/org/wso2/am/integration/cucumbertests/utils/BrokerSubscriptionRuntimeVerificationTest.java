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
import org.testng.annotations.Test;
import org.testcontainers.DockerClientFactory;
import org.wso2.am.testcontainers.ApimRuntime;
import org.wso2.am.testcontainers.DistributedDynamicApimContainer;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;

/** Selected only by focused verification suites, after all product scenarios in the block have completed. */
public class BrokerSubscriptionRuntimeVerificationTest {
    private String runtimeId;
    @Test
    public void liveReadinessIgnoresStartupMarkersAndRejectsOtherOriginsAndTopics() throws Exception {
        ApimRuntime runtime = runtime();
        runtimeId = runtime.getContainerId();
        // Forward only the two topology facts the gate needs; attempting to read any startup log fails this test.
        ApimRuntime withoutLogs = (ApimRuntime) Proxy.newProxyInstance(ApimRuntime.class.getClassLoader(),
                new Class<?>[] {ApimRuntime.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getServletHttpsUrl")) {
                        return runtime.getServletHttpsUrl();
                    }
                    if (method.getName().equals("getGatewayBrokerClientAddresses")) {
                        return runtime.getGatewayBrokerClientAddresses();
                    }
                    throw new AssertionError("Readiness must not depend on historical logs: " + method.getName());
                });
        var live = KeyManagerReadiness.await(withoutLogs, 180);
        Assert.assertNotNull(live);
        Assert.assertTrue(live.ready(), live.diagnostics());
        var wrongOrigin = BrokerSubscriptionReadiness.inspect(runtime.getServletHttpsUrl(), "keyManager",
                Set.of("203.0.113.254"));
        Assert.assertFalse(wrongOrigin.ready(), wrongOrigin.diagnostics());
        var wrongTopic = BrokerSubscriptionReadiness.inspect(runtime.getServletHttpsUrl(), "missing-verification-topic",
                runtime.getGatewayBrokerClientAddresses());
        Assert.assertFalse(wrongTopic.ready(), wrongTopic.diagnostics());
        record("positive", live.diagnostics() + "\nwrong origin: " + wrongOrigin.diagnostics()
                + "\nwrong topic: " + wrongTopic.diagnostics());
    }

    @Test(dependsOnMethods = "liveReadinessIgnoresStartupMarkersAndRejectsOtherOriginsAndTopics")
    public void disconnectedGatewayCannotRemainReady() throws Exception {
        ApimRuntime runtime = runtime();
        var addresses = runtime.getGatewayBrokerClientAddresses();
        String brokerUrl = runtime.getServletHttpsUrl();
        String gatewayId = runtime instanceof DistributedDynamicApimContainer distributed
                ? distributed.getGatewayContainerId() : runtime.getContainerId();
        // This is the last class in the focused suite. Stop only this block's own Gateway after product assertions.
        DockerClientFactory.instance().client().stopContainerCmd(gatewayId).withTimeout(10).exec();
        var disconnected = Utils.retryUntilWithInterval(Duration.ofSeconds(30).toMillis(), 250,
                () -> BrokerSubscriptionReadiness.inspect(brokerUrl, "keyManager", addresses),
                result -> !result.ready());
        Assert.assertNotNull(disconnected);
        Assert.assertFalse(disconnected.ready(), disconnected.diagnostics());
        record("disconnected", disconnected.diagnostics());
    }

    private static ApimRuntime runtime() {
        Object value = TestContext.get("blockApimContainer");
        Assert.assertTrue(value instanceof ApimRuntime, "Focused runtime verification needs a booted APIM block");
        return (ApimRuntime) value;
    }

    private void record(String phase, String evidence) throws Exception {
        Path directory = Path.of(ModulePathResolver.getModuleDir(BrokerSubscriptionRuntimeVerificationTest.class),
                "target", "logs", "broker-readiness");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(runtimeId
                + "-" + phase + ".txt"), evidence);
    }
}
