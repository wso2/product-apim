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
 * Runner for the external-IdP self-validated-JWT block (IntegrationV2-ExternalIdpJwt in testng-v2.xml) - the v2
 * port of the legacy {@code ExternalIDPJWTTestCase} / {@code ExternalIDPJWTTestSuite} pair. No external Identity
 * Server is booted: both key managers here only SELF-VALIDATE JWTs the test signs itself against a committed PEM
 * certificate, so the block needs the node backend (the /reflect-headers route) and the block's backend-JWT
 * overlay, nothing else.
 *
 * <p>Runs the {@code _setup_} fixture pattern: {@code _setup_external_idp_jwt} provisions, per tenant, the API,
 * the application with Resident-KM token-exchange keys, the subscription and the two external key managers with
 * their mapped consumer keys; {@code external_idp_jwt} then covers the key-manager tokenType lifecycle
 * (DIRECT / EXCHANGED / BOTH and the runtime effect of each), the claim-mapping to backend-JWT translation, and
 * the unknown-azp / untrusted-signer negatives.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/admin/_setup_external_idp_jwt.feature",
                "src/test/resources/features/admin/external_idp_jwt.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/external-idp-jwt.html"}
)
public class ExternalIdpJwtRunner extends BaseBlockRunner {
}
