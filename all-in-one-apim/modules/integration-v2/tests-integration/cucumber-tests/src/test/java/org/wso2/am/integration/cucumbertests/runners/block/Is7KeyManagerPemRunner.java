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
 * Runner for the PEM-certificate self-validation feature, co-hosted in the validation-modes block
 * (IntegrationV2-Is7KeyManagerValidationModes in testng-v2.xml). The scenario registers the WSO2-IS-7
 * key manager with {@code certificates.type=PEM} (the IS token-signing cert, wso2is7-pem.json) instead of the
 * JWKS endpoint the other IS7 blocks use, and the feature proves gateway self-validation follows the pinned
 * cert: live-signer PEM passes, a stale different-key-pair PEM rejects fresh tokens, re-uploading recovers.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/admin/is7_pem_validation.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/is7-pem-validation.html"}
)
public class Is7KeyManagerPemRunner extends BaseBlockRunner {
}
