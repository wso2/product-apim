/*
 * Copyright (c) 2026, WSO2 LLC. (http://wso2.com) All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package org.wso2.am.integration.cucumbertests.utils;

import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.Test;
import org.testcontainers.DockerClientFactory;
import org.wso2.am.integration.cucumbertests.utils.listeners.ClockStabilityListener;
import org.wso2.am.testcontainers.DockerHostClockMonitor;

/** Selected only by the inner negative-verification suite; never by either product suite. */
public class ClockObserverFailureProbe {
    @Test
    public void terminateOnlyThisSuitesObserver(ITestContext context) throws InterruptedException {
        terminateObserver(context);
    }

    static void terminateObserver(ITestContext context) throws InterruptedException {
        DockerHostClockMonitor monitor = (DockerHostClockMonitor) context.getSuite().getAttribute(
                ClockStabilityListener.class.getName());
        Assert.assertNotNull(monitor);
        DockerClientFactory.instance().client().killContainerCmd(monitor.containerId()).exec();
        long end = System.nanoTime() + 15_000_000_000L;
        while (System.nanoTime() < end) {
            try {
                monitor.assertHealthy();
            } catch (IllegalStateException expected) {
                return; // The listener must turn this otherwise successful method into FAILURE.
            }
            Thread.sleep(100);
        }
        Assert.fail("Clock monitor did not detect the terminated observer");
    }
}
