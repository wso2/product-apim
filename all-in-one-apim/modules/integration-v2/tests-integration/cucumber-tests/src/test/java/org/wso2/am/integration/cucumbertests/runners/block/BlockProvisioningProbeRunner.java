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
 * Phase 5.3 probe: runs against a block booted with {@code initTenantUsers=true}, so the lifecycle has
 * already provisioned the default tenant set into this block's container. The scenario asserts the
 * provisioned beans are readable from shared scope, that {@code CURRENT_TENANT} resolves off them, and
 * that the tenants/users exist in the live server (queried back via the legacy SOAP retrieve steps). Glue
 * spans both the legacy step package (readiness + SOAP retrieval) and the verification steps (assertions).
 */
@CucumberOptions(
        features = "src/test/resources/features/framework-verification/block_probe_provisioning.feature",
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions",
                "org.wso2.am.integration.cucumbertests.verification.steps"
        },
        plugin = {"pretty", "html:target/cucumber-report/block-probe-provisioning.html"}
)
public class BlockProvisioningProbeRunner extends BaseBlockRunner {
}
