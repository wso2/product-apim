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
 * Probe for {@link BlockScopeLeakVerificationTest}. Each block writes one shared and one local var
 * (so its scope maps materialise), then — when {@code cleanup} is set — calls {@link TestContext#clear()}
 * to mimic the per-block teardown that Phase 4's {@code BlockLifecycleListener.onFinish} will perform.
 * With cleanup the block's map entries are reclaimed; without it they linger, which is the leak the
 * verification guards against.
 */
public class ManyBlocksProbe {

    @Test
    @Parameters({"blockId", "cleanup"})
    public void touchScope(String blockId, boolean cleanup) {
        TestContext.setShared("fv-2.3-shared", blockId);
        TestContext.set("fv-2.3-local", blockId);
        if (cleanup) {
            TestContext.clear();
        }
    }
}
