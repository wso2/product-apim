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
 * Runner for the IS7 OAuth grant-type and token-validation coverage. Runs in the core external-KM block
 * (IntegrationV2-Is7KeyManager in testng-v2.xml). Uses the {@code _setup_} fixture pattern -
 * {@code _setup_is7_grant_app} (listed first, registers the WSO2-IS-7 KM and provisions the API / application /
 * IS client credentials once), then {@code is7_grant_types} (one scenario per
 * grant) and {@code is7_token_validation} (tampered-JWT rejection + UserInfo), all reusing that fixture. The
 * fixture is torn down once by {@link BaseBlockRunner}'s AfterClass sweep, not per-scenario.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/admin/_setup_is7_grant_app.feature",
                "src/test/resources/features/admin/is7_grant_types.feature",
                "src/test/resources/features/admin/is7_keygen_negatives.feature",
                "src/test/resources/features/admin/is7_token_validation.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/is7-grant-types.html"}
)
public class Is7KeyManagerGrantTypesRunner extends BaseBlockRunner {
}
