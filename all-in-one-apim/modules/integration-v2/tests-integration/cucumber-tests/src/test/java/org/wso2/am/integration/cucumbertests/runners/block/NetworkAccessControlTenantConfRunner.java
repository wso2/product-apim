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
 * Runner for the tenant-configuration policy source of the network access-control feature. Runs in the
 * network-access-control-tenant-conf container, which has NO platform policy, so the gate is driven purely by
 * a per-tenant NetworkSecurityAccessControl policy set in tenant-conf.json: the configuring tenant is blocked
 * while a tenant with no policy resolves the same reference.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/publisher/network_access_control_tenant_conf.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/publisher-network-access-control-tenant-conf.html"}
)
public class NetworkAccessControlTenantConfRunner extends BaseBlockRunner {
}
