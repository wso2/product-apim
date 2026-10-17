/*
 * Copyright (c) 2026, WSO2 LLC. (http://wso2.com) All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License").
 */
package org.wso2.am.integration.cucumbertests.utils;

import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/** Verifies that observer loss after the final scenario still fails Maven after reports are written. */
public class ClockObserverTeardownFailureProbe {
    @Test
    public void scenarioCompletesBeforeObserverLoss() {
        // The intentional failure is introduced during teardown, after afterInvocation checked this test.
    }

    @AfterClass(alwaysRun = true)
    public void terminateObserverDuringTeardown(ITestContext context) throws InterruptedException {
        ClockObserverFailureProbe.terminateObserver(context);
    }
}
