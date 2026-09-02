/**
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.cucumbertests.runners.block;

import io.cucumber.testng.CucumberOptions;

/**
 * Runner for WS proxy profile — specific profile precedence over catch-all. Runs in the
 * IntegrationV2-WsProxySpecificOverCatchAll block whose deployment.toml overlay
 * (wsProxySpecificOverCatchAll) loads two profiles: a specific one for "nodebackend" that routes
 * through the anonymous Squid proxy, and a catch-all with bypass_hosts for "nodebackend" that
 * would connect directly. The CONNECT count assertion distinguishes which profile was selected —
 * count = 1 means the specific profile won; count = 0 would mean the catch-all won.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/gateway/websocket_proxy_specific_over_catch_all.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/gateway-ws-proxy-specific-over-catch-all.html"}
)
public class GatewayWebSocketProxySpecificOverCatchAllRunner extends BaseBlockRunner {
}
