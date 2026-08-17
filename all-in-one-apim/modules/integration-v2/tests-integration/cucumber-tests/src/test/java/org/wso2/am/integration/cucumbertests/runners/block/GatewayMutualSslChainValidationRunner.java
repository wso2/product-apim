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
 * Runner for mutual-SSL certificate CHAIN validation — the opt-in mode where a client is authorised by the
 * issuer path of the certificate it presents rather than by the certificate itself. Ports
 * APISecurityMutualSSLCertificateChainValidationTestCase.
 *
 * <p>It has its own block because the mode is selected by a single gateway-wide boolean
 * ({@code [apimgt.mutual_ssl] enable_certificate_chain_validation}) that switches the authenticator onto a
 * different code path. Enabling it in the mtlsWsCors block would convert every default-mode mutual-SSL
 * assertion there into a statement about this mode instead, so the two cannot share a container. Needs the
 * node backend.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/gateway/mutual_ssl_chain_validation.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/gateway-mutual-ssl-chain-validation.html"}
)
public class GatewayMutualSslChainValidationRunner extends BaseBlockRunner {
}
