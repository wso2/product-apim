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
 * Runner for WS proxy profile — no profile match. Runs in the IntegrationV2-WsProxyNoMatch block
 * whose deployment.toml overlay (wsProxyNoMatch) loads a single [[transport.ws.proxy_profile]] that
 * only targets "proxied\.backend\.test". The test API uses ws://nodebackend:3001, which does not
 * match the pattern, so no profile is selected and the gateway connects directly. A passing echo
 * proves that unmatched hostnames are not inadvertently routed through the proxy.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/gateway/websocket_proxy_no_match.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/gateway-ws-proxy-no-match.html"}
)
public class GatewayWebSocketProxyNoMatchRunner extends BaseBlockRunner {
}
