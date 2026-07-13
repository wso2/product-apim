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

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.wso2.am.integration.cucumbertests.utils.TestContext;

/**
 * Probe used by {@link BlockScopeIsolationVerificationTest}. Run inside a nested TestNG suite whose
 * scope is set by {@code BlockScopeListener}; it simply writes its block id into the <b>shared</b>
 * context under a fixed key, so the orchestrating test can later assert that two same-named blocks in
 * different suites each kept their own value (no cross-block merge).
 */
public class SentinelProbe {

    public static final String SENTINEL_KEY = "fv-2.1-sentinel";

    @Test
    @Parameters("blockId")
    public void writeSentinel(String blockId) {
        TestContext.setShared(SENTINEL_KEY, blockId);
    }
}
