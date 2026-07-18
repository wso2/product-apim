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
 * Minimal probe class #3 for the parallel-on-shared-container lane. Identical in behaviour to
 * {@link BlockProbeRunnerOne}/{@link BlockProbeRunnerTwo}; a distinct class so a block can hold three
 * probe classes. The 4.7 gate puts three of these in a block running {@code parallel=classes
 * thread-count=2} to prove at most M=2 classes run at once on the block's single shared container.
 */
@CucumberOptions(
        features = "src/test/resources/features/framework-verification/block_probe.feature",
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions",
                "org.wso2.am.integration.cucumbertests.verification.steps"
        },
        plugin = {"pretty", "html:target/cucumber-report/block-probe-three.html"}
)
public class BlockProbeRunnerThree extends BaseBlockRunner {
}
