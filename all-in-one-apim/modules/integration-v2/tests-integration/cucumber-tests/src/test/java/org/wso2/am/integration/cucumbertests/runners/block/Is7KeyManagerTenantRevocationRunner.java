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
 * Runner for the tenant-org token-revocation feature in the core external-KM block (IntegrationV2-Is7KeyManager
 * in testng-v2.xml). The tenant admin registers its own org-scoped WSO2-IS-7 key manager (the super-tenant KM
 * the other scenarios register exists only in the super tenant), runs the self-validate token arc against a tenant API, and proves the gateway enforces a revocation
 * performed at IS - even though the IS-side notification carries IS's tenancy (carbon.super), not the APIM org.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/admin/is7_tenant_revocation.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/is7-tenant-revocation.html"}
)
public class Is7KeyManagerTenantRevocationRunner extends BaseBlockRunner {
}
