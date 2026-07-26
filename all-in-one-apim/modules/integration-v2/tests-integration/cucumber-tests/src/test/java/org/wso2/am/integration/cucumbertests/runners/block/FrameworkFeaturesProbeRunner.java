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
 * Phase 7.2/7.3/7.7/7.8 verification runner: tenancy provisioning + routing, the actor/Identity model with auth
 * keys, gateway invocation wiring, and the extra-overlay merge — all in the IntegrationV2-FrameworkFeatures
 * block (initTenantUsers + initBackend + tomlExtraOverlayPath). Reuses the product step library.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/framework-verification/framework_features_probe.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions",
                "org.wso2.am.integration.cucumbertests.verification.steps"
        },
        plugin = {"pretty", "html:target/cucumber-report/framework-features-probe.html"}
)
public class FrameworkFeaturesProbeRunner extends BaseBlockRunner {
}
