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

import io.cucumber.testng.CucumberOptions;

/**
 * Minimal probe class #2 for the parallel-on-shared-container lane (Phase 4.3). Identical in behaviour
 * to {@link BlockProbeRunnerOne} (same probe feature/glue, inherits the {@link BaseBlockRunner} skip
 * guard); a distinct class so a single block can contain two probe classes. The 4.4 gate asserts both
 * observe the same ready server, and the 4.7 gate uses several such classes to exercise intra-block
 * {@code parallel=classes} concurrency on one shared container.
 */
@CucumberOptions(
        features = "src/test/resources/features/framework-verification/block_probe.feature",
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions",
                "org.wso2.am.integration.cucumbertests.verification.steps"
        },
        plugin = {"pretty", "html:target/cucumber-report/block-probe-two.html"}
)
public class BlockProbeRunnerTwo extends BaseBlockRunner {
}
