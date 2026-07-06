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
 * Runner for publisher/definitions — import of OAS 2/3/3.1 definitions through revision/deploy/publish.
 * Each Scenario-Outline row is self-contained and tagged {@code @cleanup}. Extends {@link BaseBlockRunner}
 * for the block boot-failure guard and the runner-scoped cleanup safety net.
 */
@CucumberOptions(
        features = "src/test/resources/features/publisher/definitions.feature",
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/publisher-definitions.html"}
)
public class PublisherDefinitionsRunner extends BaseBlockRunner {
}
