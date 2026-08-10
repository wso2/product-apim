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
 * Runner for WS proxy profile — authenticated proxy with wrong credentials. Runs in the
 * IntegrationV2-WsProxyAuthWrongCreds block whose deployment.toml overlay (wsProxyAuthWrongCreds)
 * points nodebackend at squid-proxy:3129 (the Basic-auth Squid instance) with incorrect
 * credentials. Squid returns 407, the CONNECT tunnel is never established, and the WS upgrade
 * fails. A CONNECT count of 1 confirms the rejection came from Squid credential validation
 * rather than a pre-proxy network failure.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/gateway/websocket_proxy_auth_wrong_creds.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/gateway-ws-proxy-auth-wrong-creds.html"}
)
public class GatewayWebSocketProxyAuthWrongCredsRunner extends BaseBlockRunner {
}
