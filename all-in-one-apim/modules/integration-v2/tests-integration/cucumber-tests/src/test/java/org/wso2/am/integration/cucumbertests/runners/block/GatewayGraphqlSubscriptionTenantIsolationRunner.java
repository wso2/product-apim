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
 * Runner for the gateway GraphQL-subscription tenant-isolation regression. Publishes a carbon.super GraphQL
 * subscription victim and a tenant1.com WS leak-source, then asserts the victim subscription authenticates while
 * a flood of concurrent tenant handshakes poisons the shared netty event-loop thread. The scenario self-floods,
 * so it needs no block concurrency (the block runs thread-count=1). Reproduces the WS tenant-flow leak
 * (InboundWebSocketProcessor.handleHandshake never calls endTenantFlow) — EXPECTED to FAIL with WS close 4001
 * "Invalid JWT token" on an unfixed gateway; passes once the fix is in the gateway jar.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/gateway/graphql_subscription_tenant_isolation.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/gateway-graphql-subscription-tenant-isolation.html"}
)
public class GatewayGraphqlSubscriptionTenantIsolationRunner extends BaseBlockRunner {
}
