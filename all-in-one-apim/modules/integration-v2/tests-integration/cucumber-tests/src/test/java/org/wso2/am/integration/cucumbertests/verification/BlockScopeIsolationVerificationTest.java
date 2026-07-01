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
 * Phase 2.1 verification (Type-A, no Docker): proves the namespaced shared-scope key keeps two
 * {@code <test>} blocks of the <b>same name</b> in <b>different suites</b> from merging shared state,
 * and that the bare-{@code testName} keying it replaces would have collided.
 *
 * <p>Positive case runs two real nested TestNG suites — {@code FV-2.1-SuiteA} and {@code FV-2.1-SuiteB},
 * each with a {@code <test name="SharedBlock">} and {@code BlockScopeListener} registered — where each
 * block writes its own id into the shared context via {@link SentinelProbe}. Because the scope is keyed
 * by {@code suiteName::testName}, each block's write lands in a distinct map and is read back intact.
 * The negative case drives {@link TestContext} directly with the legacy bare-{@code testName} key to
 * show the same two blocks would clobber each other.
 */
public class BlockScopeIsolationVerificationTest {

    private static final Log logger = LogFactory.getLog(BlockScopeIsolationVerificationTest.class);

    private static final String BLOCK_NAME = "SharedBlock";
    private static final String SUITE_A = "FV-2.1-SuiteA";
    private static final String SUITE_B = "FV-2.1-SuiteB";

    @Test
    public void sameNamedBlocksAcrossSuitesStayIsolated() {

        // --- Positive: real nested TestNG runs through BlockScopeListener ---
        XmlSuite suiteA = buildProbeSuite(SUITE_A, "A");
        XmlSuite suiteB = buildProbeSuite(SUITE_B, "B");

        TestNG tng = new TestNG();
        tng.setXmlSuites(List.of(suiteA, suiteB));
        tng.setUseDefaultListeners(false);
        tng.setVerbose(0);
        tng.run();

        Assert.assertEquals(tng.getStatus() & 1, 0,
                "nested probe suites reported failures (status=" + tng.getStatus() + ")");

        // Reconstruct each block's shared-scope key exactly as BlockScopeListener derived it
        // (suiteName::testName) and assert each kept its own sentinel — no cross-block merge.
        String scopeA = SUITE_A + "::" + BLOCK_NAME;
        String scopeB = SUITE_B + "::" + BLOCK_NAME;
        Assert.assertNotEquals(scopeA, scopeB, "namespaced scope ids must differ across suites");

        Object readA = readSharedUnderScope(scopeA, SentinelProbe.SENTINEL_KEY);
        Object readB = readSharedUnderScope(scopeB, SentinelProbe.SENTINEL_KEY);
        Assert.assertEquals(readA, "A",
                "block in " + SUITE_A + " did not read its own sentinel (got " + readA + ")");
        Assert.assertEquals(readB, "B",
                "block in " + SUITE_B + " did not read its own sentinel (got " + readB + ")");
        logger.info("Positive: same-named blocks across suites isolated — " + scopeA + "='" + readA
                + "', " + scopeB + "='" + readB + "'");

        // --- Negative: legacy bare-testName key collides for the same two blocks ---
        String legacyKey = BLOCK_NAME;
        String negKey = "fv-2.1-neg-sentinel";
        TestContext.setScope(legacyKey, legacyKey + "::A");
        TestContext.setShared(negKey, "A");
        TestContext.setScope(legacyKey, legacyKey + "::B");
        TestContext.setShared(negKey, "B");
        Object collided = readSharedUnderScope(legacyKey, negKey);
        Assert.assertEquals(collided, "B",
                "expected bare-testName keying to collide (last writer wins), but got " + collided);
        Assert.assertNotEquals(collided, "A",
                "bare-testName keying unexpectedly preserved the first block's value");
        logger.info("Negative: bare-testName key '" + legacyKey + "' collided as expected — first "
                + "block's value lost");

        // Clean up scopes/maps this test created so it leaves no static residue.
        TestContext.setScope(scopeA, scopeA);
        TestContext.clear();
        TestContext.setScope(scopeB, scopeB);
        TestContext.clear();
        TestContext.setScope(legacyKey, legacyKey);
        TestContext.clear();
        TestContext.clearScope();
    }

    private XmlSuite buildProbeSuite(String suiteName, String blockId) {
        XmlSuite suite = new XmlSuite();
        suite.setName(suiteName);
        suite.setListeners(List.of(BlockScopeListener.class.getName()));

        XmlTest test = new XmlTest(suite);
        test.setName(BLOCK_NAME);
        Map<String, String> params = new HashMap<>();
        params.put("blockId", blockId);
        test.setParameters(params);
        test.setXmlClasses(List.of(new XmlClass(SentinelProbe.class)));
        return suite;
    }

    private Object readSharedUnderScope(String sharedScopeId, String key) {
        TestContext.setScope(sharedScopeId, sharedScopeId + "::read");
        return TestContext.get(key);
    }
}
