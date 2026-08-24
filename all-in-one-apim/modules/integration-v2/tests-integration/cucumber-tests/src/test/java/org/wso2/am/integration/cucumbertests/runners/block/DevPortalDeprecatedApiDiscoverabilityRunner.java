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
 * Runner for devportal/deprecated_api_discoverability — a DEPRECATED API remains discoverable in the devportal
 * search when {@code DisplayAllAPIs} is enabled, across both tenants. MUST run in the
 * IntegrationV2-DisplayDeprecatedApis block, whose overlay sets
 * {@code apim.devportal.display_deprecated_apis = true}; under the shipped default the same API is filtered out
 * of every devportal query (that direction is asserted in devportal/subscribe.feature). Self-contained
 * {@code @cleanup} scenario outline. Extends {@link BaseBlockRunner} for the block boot-failure guard and
 * runner-scoped cleanup safety net.
 */
@CucumberOptions(
        features = "src/test/resources/features/devportal/deprecated_api_discoverability.feature",
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/devportal-deprecated-api-discoverability.html"}
)
public class DevPortalDeprecatedApiDiscoverabilityRunner extends BaseBlockRunner {
}
