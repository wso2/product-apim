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
 * Runner for WS proxy profile — specific profile bypass. Runs in the IntegrationV2-WsProxySpecificBypass
 * block whose deployment.toml overlay (wsProxySpecificBypass) loads two [[transport.ws.proxy_profile]]
 * entries: one for nodebackend with bypass_hosts = ["nodebackend"] (direct connection) and one for
 * proxied.backend.test without bypass (proxy routed, proxy unreachable → rejection).
 * Ports WebSocketProxyProfileTestCase#testBypassHostInSpecificProfileGosDirect.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/gateway/websocket_proxy_specific_bypass.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/gateway-ws-proxy-specific-bypass.html"}
)
public class GatewayWebSocketProxySpecificBypassRunner extends BaseBlockRunner {
}
