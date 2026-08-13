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
 * Runner for gateway WebSub-API invocation — the port of WebSubAPITestCase / SecretValidationTestCase and the
 * subscription-count half of the WebSub ThrottlingTestCase. Registers webhooks with a published WebSub API's hub,
 * publishes content to the hub's event-receiver inbound, and asserts the hub's fan-out to the callbacks hosted on
 * the node backend. Runs in a gateway-invoking block (needs the node backend for the callback receiver, and the
 * container's exposed WebSub event-receiver inbound port).
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/gateway/websub_invocation.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/gateway-websub-invocation.html"}
)
public class GatewayWebSubInvocationRunner extends BaseBlockRunner {
}
