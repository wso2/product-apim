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
 * Phase 7.4 verification runner: a runner with a setup feature ordered before a consumer feature; the consumer
 * reads the resource id/payload the setup stored in the runner's local scope, proving the _setup_* fixture
 * handoff. NOTE: cucumber-testng runs a runner's features in LEXICOGRAPHIC FILENAME order (not features={} array
 * order) — hence the `1_setup` / `2_consumer` names, and why the production `_setup_*` prefix (leading
 * underscore) sorts first.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/framework-verification/fv_handoff_1_setup.feature",
                "src/test/resources/features/framework-verification/fv_handoff_2_consumer.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions",
                "org.wso2.am.integration.cucumbertests.verification.steps"
        },
        plugin = {"pretty", "html:target/cucumber-report/framework-features-handoff.html"}
)
public class FrameworkFeaturesHandoffRunner extends BaseBlockRunner {
}
