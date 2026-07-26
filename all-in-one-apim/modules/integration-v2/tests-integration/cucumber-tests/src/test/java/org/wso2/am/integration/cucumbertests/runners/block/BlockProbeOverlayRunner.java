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
 * Phase 4.14 probe for the overlay path: its {@code <test>} sets {@code tomlOverlayPath} to a generated
 * toml carrying a distinctive marker. The 4.14 gate uses it (alongside {@link BlockProbeDefaultsRunner})
 * to prove the listener ships the overlay into the running container - confirmed by cat-ing the
 * deployment.toml inside the live server and finding the marker.
 */
@CucumberOptions(
        features = "src/test/resources/features/framework-verification/block_probe_overlay.feature",
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions",
                "org.wso2.am.integration.cucumbertests.verification.steps"
        },
        plugin = {"pretty", "html:target/cucumber-report/block-probe-overlay.html"}
)
public class BlockProbeOverlayRunner extends BaseBlockRunner {
}
