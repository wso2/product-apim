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
 * Runner for WS proxy profile — end-to-end authenticated proxy routing. Runs in the
 * IntegrationV2-WsProxyAuthRouting block whose deployment.toml overlay (wsProxyAuthRouting) loads
 * a single [[transport.ws.proxy_profile]] that matches "nodebackend" with no bypass_hosts, pointing
 * at squid-proxy:3129 (the Basic-auth Squid instance) with testproxyuser credentials. The gateway
 * attaches a Proxy-Authorization header on the CONNECT request. Squid validates the credentials and
 * relays the tunnel. A passing echo plus an exact CONNECT count assertion proves the gateway
 * authenticated with and routed through the proxy.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/gateway/websocket_proxy_auth_routing.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/gateway-ws-proxy-auth-routing.html"}
)
public class GatewayWebSocketProxyAuthRoutingRunner extends BaseBlockRunner {
}
