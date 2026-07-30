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
 * a flood of concurrent tenant handshakes exercises the shared netty event-loop threads. It runs in the parallel
 * gateway block (thread-count=2): with the end-tenant-flow fix in place the flood's handshakes end their tenant
 * flow, so they leave NO leaked tenant on the shared event-loop threads for a concurrent neighbour to inherit —
 * the only cross-runner effect is transient event-loop contention, which every gateway runner's
 * retry-until-deadline invocation absorbs (and the scenario itself self-retries and stops its sustained flood in
 * teardown). Guards against the WS tenant-flow leak
 * (InboundWebSocketProcessor.handleHandshake not ending its tenant flow): with the end-tenant-flow fix in the
 * gateway jar it PASSES, and would FAIL with WS close 4001 "Invalid JWT token" if that leak regressed.
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
