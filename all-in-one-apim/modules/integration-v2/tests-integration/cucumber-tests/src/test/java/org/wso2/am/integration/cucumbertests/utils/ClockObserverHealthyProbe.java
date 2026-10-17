/*
 * Copyright (c) 2026, WSO2 LLC. (http://wso2.com) All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License").
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
