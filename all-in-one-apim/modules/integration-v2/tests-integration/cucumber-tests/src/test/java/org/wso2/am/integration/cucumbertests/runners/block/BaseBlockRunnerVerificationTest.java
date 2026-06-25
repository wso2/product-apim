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

package org.wso2.am.integration.cucumbertests.runners.block;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.Test;

/**
 * Phase 4.1 verification (Type-A, no Docker): proves the {@link BaseBlockRunner} guard rethrows a
 * recorded {@code bootError} as a hard failure (so the block's classes are FAILED, not SKIPPED, keeping
 * the build red) and is a no-op when boot succeeded. Lives in the same package so it can invoke the
 * package-private guard directly, and uses the real {@link ITestContext} TestNG injects into each test
 * (only its IAttributes methods are touched — never the Guice-typed methods, which a hand-rolled proxy
 * would force-resolve).
 */
public class BaseBlockRunnerVerificationTest {

    private static final Log logger = LogFactory.getLog(BaseBlockRunnerVerificationTest.class);

    private static final class Probe extends BaseBlockRunner {
    }

    @Test
    public void guardFailsWithBootErrorAsCauseWhenBootFailed(ITestContext context) {
        BaseBlockRunner runner = new Probe();
        RuntimeException bootError = new RuntimeException("simulated APIM boot/readiness failure");
        context.setAttribute(BaseBlockRunner.BOOT_ERROR_ATTRIBUTE, bootError);
        try {
            IllegalStateException failure = Assert.expectThrows(IllegalStateException.class,
                    () -> runner.abortIfBlockBootFailed(context));
            Assert.assertSame(failure.getCause(), bootError,
                    "failure cause must be the recorded boot error (single root cause, no NPE cascade)");
            logger.info("Guard rethrew a recorded bootError as a hard failure with the boot error "
                    + "as its cause");
        } finally {
            context.removeAttribute(BaseBlockRunner.BOOT_ERROR_ATTRIBUTE);
        }
    }

    @Test
    public void guardIsNoOpWhenBootSucceeded(ITestContext context) {
        context.removeAttribute(BaseBlockRunner.BOOT_ERROR_ATTRIBUTE);
        BaseBlockRunner runner = new Probe();
        runner.abortIfBlockBootFailed(context);   // must not throw
        logger.info("Guard was a no-op when no bootError was recorded (block proceeds normally)");
    }
}
