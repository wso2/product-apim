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
 * Runner for multiple-client-secrets (consumer-secret CRUD) — the port of the legacy ApplicationTestCase
 * consumer-secret delta. Runs in the standard IntegrationV2-KeyManager block; multiple-client-secrets mode is
 * enabled by default in the pack (default.json: oauth.multiple_client_secrets.enable = true), so no overlay is
 * needed.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/key-manager/multiple_client_secrets.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/keymanager-multiple-client-secrets.html"}
)
public class KeyManagerMultipleClientSecretsRunner extends BaseBlockRunner {
}
