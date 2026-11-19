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
 * Runner for the Solace event-API lifecycle. Belongs to a block that sets {@code initSolaceBroker=true}
 * and an overlay carrying {@code [apim.solace_config]} ({@code artifacts/configFiles/solace/deployment.toml},
 * or {@code solaceMtlsChain/deployment.toml} in the distributed suite) — its {@code apim_api_endpoint} is the
 * shim's network alias, and the block boots {@code DynamicSolaceBroker} before APIM so that alias resolves.
 */
@CucumberOptions(
        features = {"src/test/resources/features/publisher/solace_event_api.feature"},
        glue = {"org.wso2.am.integration.cucumbertests.stepdefinitions"},
        plugin = {"pretty", "html:target/cucumber-report/solace-api-lifecycle.html"}
)
public class SolaceApiLifecycleRunner extends BaseBlockRunner {
}
