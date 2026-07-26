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

package org.wso2.am.integration.cucumbertests.verification;

import org.testng.ITestContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.am.integration.cucumbertests.utils.TestContext;

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Probe for {@link BlockScopeThreadVisibilityVerificationTest}. Runs in nested TestNG suites with
 * {@code parallel="tests"} and a {@code parallel=true} data provider so several invocations execute
 * concurrently on worker threads. Each invocation writes its own block id into the shared scope, then
 * (after a window for siblings to interfere) reads it back. Any read that is not its own block id —
 * the pre-seeded global {@link #POISON} (scope never applied on the worker thread) or a sibling's id
 * (cross-block leak) — is recorded as a failure for the orchestrator to assert on.
 */
public class ThreadScopeProbe {

    public static final String SENTINEL_KEY = "fv-2.2-sentinel";
    public static final String POISON = "GLOBAL-POISON";

    static final ConcurrentLinkedQueue<String> failures = new ConcurrentLinkedQueue<>();
    static final Set<String> threadNames = new CopyOnWriteArraySet<>();
    static final AtomicInteger invocations = new AtomicInteger();

    static void reset() {
        failures.clear();
        threadNames.clear();
        invocations.set(0);
    }

    @DataProvider(name = "rows", parallel = true)
    public Object[][] rows(ITestContext ctx) {
        String blockId = ctx.getCurrentXmlTest().getLocalParameters().get("blockId");
        return new Object[][]{{blockId, 0}, {blockId, 1}};
    }

    @Test(dataProvider = "rows")
    public void probe(String blockId, int row) throws InterruptedException {
        invocations.incrementAndGet();
        threadNames.add(Thread.currentThread().getName());

        // Default-leak guard: if the per-invocation scope wasn't visible on this worker thread, the
        // read falls back to the global scope, which we pre-seeded with POISON.
        Object pre = TestContext.get(SENTINEL_KEY);
        if (POISON.equals(pre)) {
            failures.add("block " + blockId + " row " + row + " saw global POISON before writing "
                    + "(scope not applied on worker thread " + Thread.currentThread().getName() + ")");
        }

        // Cross-block guard: write own id, give siblings a window to interfere, then read own back.
        TestContext.setShared(SENTINEL_KEY, blockId);
        Thread.sleep(150);
        Object post = TestContext.get(SENTINEL_KEY);
        if (!blockId.equals(post)) {
            failures.add("block " + blockId + " row " + row + " read '" + post
                    + "' after writing '" + blockId + "' (cross-block scope leak)");
        }
    }
}
