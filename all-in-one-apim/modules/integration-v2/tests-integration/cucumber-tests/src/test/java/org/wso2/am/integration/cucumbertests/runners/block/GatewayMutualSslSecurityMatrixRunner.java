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
 * Runner for the gateway mutual-SSL x application-security matrix — ports the mutual-SSL and internal-key
 * coverage of APISecurityTestCase. Uses the {@code _setup_*} fixture pattern:
 * {@code _setup_mutual_ssl_security_matrix} is listed FIRST (it publishes three APIs differing only in which
 * security schemes they mandate, uploads the accepted client certificate to each, and subscribes one application
 * to the two OAuth-bearing ones), then {@code mutual_ssl_security_matrix} asserts each cell of the matrix at both
 * the versioned and versionless context. The fixture is NOT torn down per scenario — {@link BaseBlockRunner}'s
 * {@code @AfterClass} sweep deletes it after all scenarios complete. Needs the node backend and the block's
 * shrunken SSL-profile read interval so an uploaded client certificate is picked up in seconds.
 *
 * <p>Separate from {@code GatewayMutualSslInvocationRunner} rather than another feature in it: that runner's
 * feature is per-scenario {@code @cleanup}, and its sweep would delete this fixture out from under the matrix.
 * A distinct runner also gets its own {@code TestContext} local scope and unique-name suffix.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/gateway/_setup_mutual_ssl_security_matrix.feature",
                "src/test/resources/features/gateway/mutual_ssl_security_matrix.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/gateway-mutual-ssl-security-matrix.html"}
)
public class GatewayMutualSslSecurityMatrixRunner extends BaseBlockRunner {
}
