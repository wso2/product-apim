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

package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.en.Given;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.KeyManagerReadiness;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.testcontainers.ApimRuntime;

/** Cucumber steps for the gateway key-manager event-consumer readiness boundary. */
public class KeyManagerReadinessSteps {

    @Given("the Gateway key-manager consumer is ready within {int} seconds")
    public void gatewayKeyManagerConsumerIsReady(int timeoutSeconds) throws InterruptedException {

        KeyManagerReadiness.Result result = KeyManagerReadiness.await(runtime(), timeoutSeconds);
        Assert.assertNotNull(result, "No key-manager readiness sample was obtained");
        Assert.assertTrue(result.ready(), "The Gateway key-manager consumer did not become ready. "
                + result.diagnostics());
    }

    private static ApimRuntime runtime() {

        Object candidate = TestContext.get("blockApimContainer");
        if (!(candidate instanceof ApimRuntime)) {
            throw new IllegalStateException("Block APIM container is not available in the test context");
        }
        return (ApimRuntime) candidate;
    }
}
