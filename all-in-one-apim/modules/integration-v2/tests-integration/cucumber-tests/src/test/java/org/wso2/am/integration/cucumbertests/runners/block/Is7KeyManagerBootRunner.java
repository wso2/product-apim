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
 * Runner for the framework-verification feature that boots WSO2 IS 7.x as an external key manager and proves
 * an end-to-end token-issue-from-IS + gateway-invoke flow, plus the admin well-known discovery pin. Runs in the
 * core external-KM block (testng-v2.xml, bootExternalIdentityServer=true): BlockLifecycleListener augments
 * APIM's truststore and starts IS, and the scenario itself registers the key manager (admin product behaviour).
 * Extends {@link BaseBlockRunner}
 * for the block boot-failure guard and the runner-scoped cleanup safety net.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/admin/is7_key_manager_boot.feature",
                "src/test/resources/features/admin/is7_wellknown_discovery.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/is7-key-manager-boot.html"}
)
public class Is7KeyManagerBootRunner extends BaseBlockRunner {
}
