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
 * Runner for the tenant-sharing-off negative block (IntegrationV2-Is7TenantSharingOff in testng-v2.xml). Boots APIM with
 * skip_create_resident_key_manager but NO tenant-sharing, and asserts a tenant provisioned through the notify
 * endpoint has no key manager, so application key generation is refused (901403 "Key Manager not Registered").
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/admin/tenant_sharing_off.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/tenant-sharing-off.html"}
)
public class TenantSharingOffRunner extends BaseBlockRunner {
}
