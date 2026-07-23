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
 * Runner for the WSO2-IS-7 default-key-manager block (IntegrationV2-Is7DefaultKeyManager in testng-v2.xml). Boots APIM with
 * skip_create_resident_key_manager + [[apim.tenant_sharing]] type=WSO2-IS-7 and asserts a tenant provisioned
 * through the notify endpoint is auto-configured with a WSO2-IS-7 key manager. No external IS is needed - the KM
 * is created from the tenant-created event, not fetched from IS.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/admin/default_key_manager.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/default-key-manager.html"}
)
public class DefaultKmRunner extends BaseBlockRunner {
}
