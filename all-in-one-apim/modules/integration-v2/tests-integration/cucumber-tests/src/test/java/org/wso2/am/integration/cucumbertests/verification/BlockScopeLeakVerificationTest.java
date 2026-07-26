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
 * Phase 2.3 verification (Type-A, no Docker): proves the {@link TestContext} shared/local scope maps
 * do not grow one-entry-per-block. Runs many short blocks through {@code BlockScopeListener} and
 * asserts that when each block tears down via {@link TestContext#clear()} (the primitive Phase 4's
 * {@code onFinish} will invoke) the maps return to baseline, while a control run without teardown
 * leaves exactly one lingering entry per block — confirming {@code clear()} is what reclaims them.
 */
public class BlockScopeLeakVerificationTest {

    private static final Log logger = LogFactory.getLog(BlockScopeLeakVerificationTest.class);

    private static final int BLOCK_COUNT = 20;

    @Test
    public void manyBlocksDoNotLeakScopeMapsWhenClearedOnTeardown() {

        // --- Positive: every block clears on teardown -> no net growth ---
        int sharedBefore = TestContext.sharedScopeCount();
        int localBefore = TestContext.localScopeCount();
        runBlocks("FV-2.3-pos", true);
        int sharedGrowthPos = TestContext.sharedScopeCount() - sharedBefore;
        int localGrowthPos = TestContext.localScopeCount() - localBefore;
        Assert.assertEquals(sharedGrowthPos, 0,
                "shared scope maps grew by " + sharedGrowthPos + " despite per-block clear() (leak)");
        Assert.assertEquals(localGrowthPos, 0,
                "local scope maps grew by " + localGrowthPos + " despite per-block clear() (leak)");
        logger.info("Positive: " + BLOCK_COUNT
                + " blocks each cleared on teardown -> shared/local map growth = 0");

        // --- Negative (control): no teardown -> exactly one lingering entry per block ---
        int sharedBeforeNeg = TestContext.sharedScopeCount();
        int localBeforeNeg = TestContext.localScopeCount();
        runBlocks("FV-2.3-neg", false);
        int sharedGrowthNeg = TestContext.sharedScopeCount() - sharedBeforeNeg;
        int localGrowthNeg = TestContext.localScopeCount() - localBeforeNeg;
        Assert.assertEquals(sharedGrowthNeg, BLOCK_COUNT,
                "expected uncleaned blocks to leave one shared entry each (got " + sharedGrowthNeg + ")");
        Assert.assertEquals(localGrowthNeg, BLOCK_COUNT,
                "expected uncleaned blocks to leave one local entry each (got " + localGrowthNeg + ")");
        logger.info("Negative: " + BLOCK_COUNT + " blocks without teardown -> shared/local map growth = "
                + sharedGrowthNeg + " each (confirms clear() is the reclaiming mechanism)");

        // Reclaim the control run's shared residue. (Local maps are keyed sharedId::class#instance, so
        // they aren't reclaimable by shared id here; harmless as this is a dedicated single-test JVM.)
        for (int i = 0; i < BLOCK_COUNT; i++) {
            String scopeId = "FV-2.3-neg::" + blockName(i);
            TestContext.setScope(scopeId, scopeId);
            TestContext.clear();
        }
        TestContext.clearScope();
    }

    private void runBlocks(String suiteName, boolean cleanup) {
        XmlSuite suite = new XmlSuite();
        suite.setName(suiteName);
        suite.setListeners(List.of(BlockScopeListener.class.getName()));
        for (int i = 0; i < BLOCK_COUNT; i++) {
            XmlTest test = new XmlTest(suite);
            test.setName(blockName(i));
            Map<String, String> params = new HashMap<>();
            params.put("blockId", suiteName + "-" + i);
            params.put("cleanup", Boolean.toString(cleanup));
            test.setParameters(params);
            test.setXmlClasses(List.of(new XmlClass(ManyBlocksProbe.class)));
        }

        TestNG tng = new TestNG();
        tng.setXmlSuites(List.of(suite));
        tng.setUseDefaultListeners(false);
        tng.setVerbose(0);
        tng.run();
        Assert.assertEquals(tng.getStatus() & 1, 0,
                "nested block suite '" + suiteName + "' reported failures (status=" + tng.getStatus() + ")");
    }

    private String blockName(int i) {
        return String.format("blk-%02d", i);
    }
}
