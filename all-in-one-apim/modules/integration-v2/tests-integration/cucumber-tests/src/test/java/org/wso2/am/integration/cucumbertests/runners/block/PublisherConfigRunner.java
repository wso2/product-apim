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
 * Runner for publisher/api-config. Demonstrates the {@code _setup_*} fixture pattern: the features array
 * lists {@code _setup_config_api} FIRST (it creates the shared base API into the runner's local scope),
 * then {@code api_config} (whose scenarios PATCH configuration fields against that API). The array order is
 * the execution order. The shared API is NOT torn down per-scenario — {@code BaseBlockRunner}'s
 * {@code @AfterClass} sweep deletes it once after all scenarios complete.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/common/_setup_config_api.feature",
                "src/test/resources/features/publisher/api_config.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/publisher-api-config.html"}
)
public class PublisherConfigRunner extends BaseBlockRunner {
}
