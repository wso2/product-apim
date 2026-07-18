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
 * Runner for custom application attributes — the port of the legacy ApplicationAttributesTestCase. Runs in the
 * IntegrationV2-ApplicationAttributes block, which needs a feature-specific TOML overlay (backend JWT
 * generation + the declared application attribute) and a backend (the header-reflecting /reflect-headers
 * route), so the attribute can be verified both stored on the application and surfaced in the X-JWT-Assertion
 * backend JWT claim.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/devportal/application_attributes.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/devportal-application-attributes.html"}
)
public class DevPortalApplicationAttributesRunner extends BaseBlockRunner {
}
