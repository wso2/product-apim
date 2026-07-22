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
 * Runner for the RFC 8693 token-exchange block (testng-is7tx.xml). Boots APIM (with
 * enforce_type_header_validation on) + an external WSO2 IS 7.x used only as the subject-token issuer
 * (bootExternalIdentityServer, no key-manager registration - the subject token is validated by an APIM-side
 * trusted IdP, not a KM). Runs the {@code _setup_} fixture pattern: {@code _setup_token_exchange} (API, app with
 * Resident-KM token-exchange keys, subscription, IS subject app) then {@code token_exchange} (PEM + JWKS
 * exchange to gateway 200, the id_token type-header negative, and the tampered / missing / untrusted negatives).
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/admin/_setup_token_exchange.feature",
                "src/test/resources/features/admin/token_exchange.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/token-exchange.html"}
)
public class TokenExchangeRunner extends BaseBlockRunner {
}
