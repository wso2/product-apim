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
 * Runner for WS proxy profile — end-to-end anonymous proxy routing. Runs in the
 * IntegrationV2-WsProxyAnonRouting block whose deployment.toml overlay (wsProxyAnonRouting) loads
 * a single [[transport.ws.proxy_profile]] that matches "nodebackend" with no bypass_hosts, pointing
 * at squid-proxy:3128 (the anonymous Squid instance). The gateway sends CONNECT nodebackend:3001
 * to Squid, which relays the tunnel on the Docker network. A passing echo plus an exact CONNECT
 * count assertion proves the gateway used the proxy path.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/gateway/websocket_proxy_anon_routing.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/gateway-ws-proxy-anon-routing.html"}
)
public class GatewayWebSocketProxyAnonRoutingRunner extends BaseBlockRunner {
}
