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

package org.wso2.am.integration.cucumbertests.utils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates unique-by-construction names per runner instance. Scenarios inside a runner run sequentially
 * and share the runner's local scope, so a single suffix + monotonic counter is enough to guarantee no
 * two resources created by that runner collide. Parallel runners are isolated by different suffixes.
 *
 * <p>The suffix is created lazily and cached in the current local {@link TestContext}; the first call in a
 * runner seeds it, subsequent calls reuse it. This replaces the legacy hardcoded names such as
 * {@code APIMTest} / {@code apiTestContext} that caused cross-test collisions when run in parallel.
 */
public final class Names {

    private static final String SCOPE_SUFFIX_KEY = "uniqueNameSuffix";
    private static final String COUNTER_KEY = "uniqueNameCounter";

    private Names() {
    }

    /**
     * Returns a unique name derived from the given base. Repeated calls with the same base in the same
     * runner still return distinct values because of the monotonic counter.
     */
    public static String unique(String base) {

        String suffix = (String) TestContext.get(SCOPE_SUFFIX_KEY);
        if (suffix == null) {
            suffix = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            TestContext.set(SCOPE_SUFFIX_KEY, suffix);
        }

        AtomicInteger counter = (AtomicInteger) TestContext.get(COUNTER_KEY);
        if (counter == null) {
            counter = new AtomicInteger(0);
            TestContext.set(COUNTER_KEY, counter);
        }

        return base + "_" + suffix + "_" + counter.incrementAndGet();
    }
}
