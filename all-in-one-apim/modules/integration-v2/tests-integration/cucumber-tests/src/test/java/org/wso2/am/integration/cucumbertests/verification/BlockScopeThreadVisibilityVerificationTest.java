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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.TestNG;
import org.testng.annotations.Test;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.listeners.BlockScopeListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 2.2 verification (Type-A, no Docker, critical): proves the {@code InheritableThreadLocal}
 * scope set by {@code BlockScopeListener.beforeInvocation} is visible on the worker thread that runs
 * the test body, even under {@code data-provider-thread-count > 1} and {@code parallel="tests"}, and
 * that no invocation ever reads the default/global scope or a sibling block's shared vars.
 *
 * <p>It runs a nested TestNG suite of three same-shaped {@code <test>} blocks in parallel, each with a
 * {@code parallel=true} data provider, with {@code BlockScopeListener} registered. The global scope is
 * pre-seeded with a {@link ThreadScopeProbe#POISON} value: if a worker thread doesn't see its scope,
 * the probe reads POISON; if a sibling block's write leaks across, the probe reads the wrong block id.
 * Both are recorded as failures and asserted to be absent.
 */
public class BlockScopeThreadVisibilityVerificationTest {

    private static final Log logger =
            LogFactory.getLog(BlockScopeThreadVisibilityVerificationTest.class);

    private static final String SUITE = "FV-2.2";
    private static final List<String> BLOCKS = List.of("Block-1", "Block-2", "Block-3");
    private static final int ROWS_PER_BLOCK = 2;

    @Test
    public void scopeVisibleOnWorkerThreadWithNoLeak() {

        ThreadScopeProbe.reset();

        // Pre-seed the global/default scope with a poison sentinel. Any probe that falls back to the
        // global scope (scope not visible on its worker thread) will read this instead of its own id.
        TestContext.clearScope();
        TestContext.setShared(ThreadScopeProbe.SENTINEL_KEY, ThreadScopeProbe.POISON);

        XmlSuite suite = new XmlSuite();
        suite.setName(SUITE);
        suite.setParallel(XmlSuite.ParallelMode.TESTS);
        suite.setThreadCount(BLOCKS.size());
        suite.setDataProviderThreadCount(2);
        suite.setListeners(List.of(BlockScopeListener.class.getName()));
        for (String blockId : BLOCKS) {
            XmlTest test = new XmlTest(suite);
            test.setName(blockId);
            Map<String, String> params = new HashMap<>();
            params.put("blockId", blockId);
            test.setParameters(params);
            test.setXmlClasses(List.of(new XmlClass(ThreadScopeProbe.class)));
        }

        TestNG tng = new TestNG();
        tng.setXmlSuites(List.of(suite));
        tng.setUseDefaultListeners(false);
        tng.setVerbose(0);
        tng.run();

        Assert.assertEquals(tng.getStatus() & 1, 0,
                "nested probe suite reported failures (status=" + tng.getStatus() + ")");
        Assert.assertEquals(ThreadScopeProbe.invocations.get(), BLOCKS.size() * ROWS_PER_BLOCK,
                "expected every block row to run");
        Assert.assertTrue(ThreadScopeProbe.threadNames.size() > 1,
                "probes did not run concurrently on multiple worker threads (threads="
                        + ThreadScopeProbe.threadNames + ") — the test would not exercise the race");
        Assert.assertTrue(ThreadScopeProbe.failures.isEmpty(),
                "scope visibility/leak failures detected: " + ThreadScopeProbe.failures);

        logger.info("Phase 2.2: " + ThreadScopeProbe.invocations.get() + " invocations across "
                + BLOCKS.size() + " parallel blocks on worker threads " + ThreadScopeProbe.threadNames
                + " — all read their own block scope, none saw global POISON or a sibling");

        // Clean up the maps this test created so it leaves no static residue.
        TestContext.clearScope();
        TestContext.clear();
        for (String blockId : BLOCKS) {
            String scopeId = SUITE + "::" + blockId;
            TestContext.setScope(scopeId, scopeId);
            TestContext.clear();
        }
        TestContext.clearScope();
    }
}
