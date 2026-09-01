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
 * Runner for the network access-control URL-gate tenant-policy tests - the port of HostValidationTenantAllowModeTestCase. Under a per-tenant allow policy every publisher/admin URL-validation entry point rejects a non-allow-listed host, an allow-listed host passes, and a second tenant is unaffected. Runs in the network-access-control-tenant-allow container.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/publisher/network_access_control_url_tenant_allow.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/publisher-network-access-control-url-tenant-allow.html"}
)
public class NetworkAccessControlUrlTenantAllowRunner extends BaseBlockRunner {
}
