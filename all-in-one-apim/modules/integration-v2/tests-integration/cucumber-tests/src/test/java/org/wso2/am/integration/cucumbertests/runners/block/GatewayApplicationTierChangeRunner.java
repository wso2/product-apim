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
 * Runner for gateway/application-tier-change. Belongs to its own IntegrationV2-ApplicationTierChange block
 * (thread-count=1, initBackend): application-throttle windows are time-sensitive and the tier-change + reset
 * dance must not share a container with other time-sensitive throttle scenarios. The scenario creates and
 * registers its own resources (API, application, admin application policies); teardown is the per-scenario
 * {@code @cleanup} hook.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/gateway/application_tier_change.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/gateway-application-tier-change.html"}
)
public class GatewayApplicationTierChangeRunner extends BaseBlockRunner {
}
