/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 */

package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.en.Given;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.ThrottleDataReadiness;
import org.wso2.am.testcontainers.ApimRuntime;

/**
 * Readiness for the asynchronous throttle event path. Carbon's HTTP readiness page only proves that a component
 * is serving requests; it does not prove that the Traffic Manager event publisher or the Gateway JMS consumer has
 * finished binding to {@code throttleData}. This gate makes that prerequisite explicit before an enforcement
 * scenario starts sending requests.
 *
 * <p>The positive markers are emitted by the product itself. Registry-path and broker-binding failures are also
 * evaluated, including failures that occur after an earlier positive marker.</p>
 */
public class ThrottlingReadinessSteps {

    @Given("the Traffic Manager throttleData publisher and Gateway throttleData consumer are ready within {int} seconds")
    public void throttleDataPipelineIsReady(int timeoutSeconds) throws InterruptedException {
        ApimRuntime runtime = runtime();
        ThrottleDataReadiness.Result last = ThrottleDataReadiness.await(runtime, timeoutSeconds);

        Assert.assertNotNull(last, "No throttleData readiness sample was obtained");
        Assert.assertTrue(last.ready(), "The throttleData pipeline did not become ready. " + last.diagnostics());
    }

    private static ApimRuntime runtime() {
        Object candidate = TestContext.get("blockApimContainer");
        if (!(candidate instanceof ApimRuntime)) {
            throw new IllegalStateException("Block APIM container is not available in the test context");
        }
        return (ApimRuntime) candidate;
    }

}
