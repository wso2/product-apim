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
 * Runner for the introspection-mode external-key-manager feature. Runs in the introspection block
 * (testng-is7km-introspect.xml), which registers the IS7 KM with enableSelfValidationJWT=false
 * (externalKmPayload=wso2is7-introspect.json) so the gateway validates tokens via IS /oauth2/introspect. Proves
 * the KM works in introspection mode and that a revoke propagates to IS's introspection endpoint. Extends
 * {@link BaseBlockRunner} for the block boot-failure guard and the runner-scoped cleanup safety net.
 */
@CucumberOptions(
        features = "src/test/resources/features/admin/is7_introspection.feature",
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/is7-introspection.html"}
)
public class Is7KeyManagerIntrospectionRunner extends BaseBlockRunner {
}
