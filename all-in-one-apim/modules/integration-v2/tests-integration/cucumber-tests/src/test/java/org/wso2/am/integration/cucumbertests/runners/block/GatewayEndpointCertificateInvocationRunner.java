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
 * Runner for gateway/endpoint_certificate_invocation — the RUNTIME half of the endpoint-certificate feature
 * (ports the invocation methods of APIEndpointCertificateTestCase). Invokes an API whose backend is the
 * tls-backend HTTPS app on {@code https://nodebackend:3023} and asserts the 500 → upload → 200 → delete → 500
 * transition, so it needs the node backend AND its own container block: the block's TOML overlay shortens the
 * HTTPS sender's SSL-profile read interval and the certificate reloader period from the product defaults (10
 * minutes each), without which the post-upload 200 leg is unreachable inside any test window.
 */
@CucumberOptions(
        features = "src/test/resources/features/gateway/endpoint_certificate_invocation.feature",
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/gateway-endpoint-certificate-invocation.html"}
)
public class GatewayEndpointCertificateInvocationRunner extends BaseBlockRunner {
}
