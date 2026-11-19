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
import org.testng.ITestContext;
import org.testng.annotations.Test;
import org.wso2.am.integration.cucumbertests.utils.listeners.ClockStabilityListener;
import org.wso2.am.testcontainers.DockerHostClockMonitor;

/** Explicit framework-verification suite only; allows parallel blocks to verify the same observer. */
public class ClockObserverHealthyProbe {
    @Test
    public void sharedObserverStaysHealthy(ITestContext context) throws InterruptedException {
        DockerHostClockMonitor monitor = (DockerHostClockMonitor) context.getSuite().getAttribute(
                ClockStabilityListener.class.getName());
        Assert.assertNotNull(monitor);
        context.setAttribute("clockObserverId", monitor.containerId());
        long end = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < end) {
            monitor.assertHealthy();
            Thread.sleep(100);
        }
    }
}
