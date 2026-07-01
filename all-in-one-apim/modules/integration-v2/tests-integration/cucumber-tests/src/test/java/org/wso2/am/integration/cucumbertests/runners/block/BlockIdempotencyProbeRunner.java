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
 * Phase 5.7 probe: runs against a block booted with {@code initTenantUsers=true}, so the lifecycle has
 * already provisioned the default tenant set once. The scenario re-provisions the same set and asserts the
 * re-run no-ops (skip-if-exists) - it must not throw and must leave the tenant/user existing exactly once on
 * the live server. Glue spans the legacy step package (readiness + SOAP retrieval) and the verification
 * steps (re-provision + assertions).
 */
@CucumberOptions(
        features = "src/test/resources/features/framework-verification/block_probe_idempotency.feature",
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions",
                "org.wso2.am.integration.cucumbertests.verification.steps"
        },
        plugin = {"pretty", "html:target/cucumber-report/block-probe-idempotency.html"}
)
public class BlockIdempotencyProbeRunner extends BaseBlockRunner {
}
