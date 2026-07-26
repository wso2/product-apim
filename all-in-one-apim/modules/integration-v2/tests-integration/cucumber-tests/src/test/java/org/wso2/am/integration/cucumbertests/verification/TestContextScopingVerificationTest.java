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

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;

/**
 * Phase 7.1 verification (Type-A): the test-authoring context features that the ported suite relies on but the
 * original Phase 1–6 lane never exercised — {@code ${UNIQUE:…}} naming, {@code {{…}}} placeholder resolution,
 * and the local (runner) vs shared (block) {@link TestContext} scope semantics. In-JVM only; no container.
 */
public class TestContextScopingVerificationTest {

    @AfterMethod(alwaysRun = true)
    public void clearScope() {
        TestContext.clearScope();
    }

    /** {@code ${UNIQUE:<base>}} must yield collision-free names that still carry the base. */
    @Test
    public void uniquePlaceholderYieldsCollisionFreeNames() {
        String tmpl = "{\"name\":\"${UNIQUE:APIMTest}\",\"context\":\"${UNIQUE:apiTestContext}\"}";
        String first = Utils.resolvePayloadPlaceholders(tmpl);
        String second = Utils.resolvePayloadPlaceholders(tmpl);

        Assert.assertTrue(first.contains("APIMTest"), "resolved payload lost the base name: " + first);
        Assert.assertFalse(first.contains("${UNIQUE:"), "placeholder not substituted: " + first);
        Assert.assertNotEquals(first, second,
                "two resolutions produced identical names — not collision-free: " + first);
    }

    /** {@code {{key}}} resolves from context; a missing key fails loudly rather than silently. */
    @Test
    public void contextPlaceholderResolvesFromContextAndFailsOnMissing() {
        TestContext.setScope("fv-7.1-shared", "fv-7.1-local");
        TestContext.set("apiContext", "/t/tenant1.com/myapi");

        Assert.assertEquals(Utils.resolveContextPlaceholders("{{apiContext}}/1.0.0/customers/123/"),
                "/t/tenant1.com/myapi/1.0.0/customers/123/");

        try {
            Utils.resolveContextPlaceholders("{{missingKey}}");
            Assert.fail("resolving a missing context key should throw, not return silently");
        } catch (IllegalStateException expected) {
            // expected — missing placeholders must fail loudly
        }
    }

    /**
     * Local context is runner-scoped (isolated per local scope id); shared context is block-scoped (visible to
     * every runner sharing the block's scope id). This is what lets a {@code _setup_*} fixture hand resources to
     * later features in the SAME runner while NOT leaking them to a sibling runner.
     */
    @Test
    public void localIsRunnerScopedAndSharedIsBlockScoped() {
        // Runner 1 in block A.
        TestContext.setScope("blockA", "runner1");
        TestContext.set("localKey", "local-v");
        TestContext.setShared("sharedKey", "shared-v");
        Assert.assertEquals(TestContext.get("localKey"), "local-v");
        Assert.assertEquals(TestContext.get("sharedKey"), "shared-v");

        // Runner 2 in the SAME block: shared value is visible, local value is NOT.
        TestContext.setScope("blockA", "runner2");
        Assert.assertEquals(TestContext.get("sharedKey"), "shared-v",
                "shared (block-scoped) value should be visible to another runner in the same block");
        Assert.assertNull(TestContext.get("localKey"),
                "local (runner-scoped) value must NOT leak to another runner");
    }
}
