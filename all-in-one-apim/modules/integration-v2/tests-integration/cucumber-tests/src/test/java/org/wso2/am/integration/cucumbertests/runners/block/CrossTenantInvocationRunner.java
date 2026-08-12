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
 * Runner for the cross-tenant gateway-invocation scenarios. Shares the cross-tenant subscription block with
 * {@link CrossTenantSubscriptionRunner} (same {@code tomlExtraOverlayPath} enabling
 * {@code [apim.devportal] enable_cross_tenant_subscriptions}), and additionally needs the block's
 * {@code initBackend} so the provider tenant's API has a live upstream to invoke. Runs the {@code _setup_}
 * fixture pattern: {@code _setup_cross_tenant_subscription} (provider API, consumer application, cross-tenant
 * subscription, provider Resident-KM id) then {@code cross_tenant_invocation} (provider-KM keys invoke 200,
 * consumer-KM keys rejected 401). The fixture must survive both scenarios, so neither feature is
 * {@code @cleanup} - teardown is this runner's AfterClass sweep.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/gateway/_setup_cross_tenant_subscription.feature",
                "src/test/resources/features/gateway/cross_tenant_invocation.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/cross-tenant-invocation.html"}
)
public class CrossTenantInvocationRunner extends BaseBlockRunner {
}
