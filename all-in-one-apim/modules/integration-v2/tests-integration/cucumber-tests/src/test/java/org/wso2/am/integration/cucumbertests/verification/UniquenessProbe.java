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

import org.testng.annotations.Test;

/**
 * Trivial test class referenced (by name) from the XML fixtures that
 * {@link BlockUniquenessLintVerificationTest} parses. It is never executed: the lint rejects the
 * duplicate-composite suite at load, before TestNG would instantiate any block.
 */
public class UniquenessProbe {

    @Test
    public void mark() {
        // intentionally empty — see class javadoc
    }
}
