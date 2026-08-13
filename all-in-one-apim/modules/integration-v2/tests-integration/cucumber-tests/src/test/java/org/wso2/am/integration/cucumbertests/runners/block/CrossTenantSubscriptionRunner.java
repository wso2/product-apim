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
import org.testng.annotations.AfterClass;
import org.wso2.am.integration.cucumbertests.stepdefinitions.WorkflowAdminSteps;

/**
 * Runner for the cross-tenant subscription discovery + subscribe facets (devportal plane). Needs the
 * cross-tenant subscription block (its {@code tomlExtraOverlayPath} enables
 * {@code [apim.devportal] enable_cross_tenant_subscriptions}). Self-contained {@code @cleanup} scenarios.
 */
@CucumberOptions(
        features = "src/test/resources/features/devportal/cross_tenant_subscription.feature",
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/cross-tenant-subscription.html"}
)
public class CrossTenantSubscriptionRunner extends BaseBlockRunner {

    /**
     * Restores the original workflow-extensions.xml once after all scenarios (idempotent, failure-safe, and a
     * no-op unless a scenario actually flipped it). The last scenario in this feature flips ONLY the
     * SubscriptionUpdate executor to the Approval variant — a server-global registry write — so the runner
     * un-does it here rather than leaving it flipped for the sibling {@link CrossTenantInvocationRunner} that
     * shares this block's container.
     */
    @AfterClass(alwaysRun = true)
    void restoreWorkflowExecutors() {
        WorkflowAdminSteps.restoreWorkflowExecutors();
    }
}
