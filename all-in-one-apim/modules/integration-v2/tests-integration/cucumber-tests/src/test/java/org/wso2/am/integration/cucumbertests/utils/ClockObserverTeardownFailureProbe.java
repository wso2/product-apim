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
