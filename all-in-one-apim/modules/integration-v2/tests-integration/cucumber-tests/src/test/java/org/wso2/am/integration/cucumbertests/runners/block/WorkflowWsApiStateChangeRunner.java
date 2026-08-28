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
 * Runner for the EXTERNAL BPMN {@code APIStateChange} executor — ports the start-request assertions of
 * {@code APIStateChangeWorkflowTestCase}. Its feature routes {@code APIStateChange} at
 * {@code APIStateChangeWSWorkflowExecutor} (pointed at the BPMNProcessServerApp double on
 * {@code nodebackend:3004}) by writing an alternate {@code workflow-extensions.xml} into the governance registry.
 *
 * <p><b>Why this is its OWN runner rather than the last feature of {@link WorkflowApprovalRunner}.</b> That flip is
 * server-global per tenant and is not undone until an {@code @AfterClass}. While it sat inside the approval runner
 * its safety rested entirely on {@code workflow_ws_*} sorting LAST by filename (cucumber-testng orders features
 * lexicographically, not by array position), so a rename or a new {@code workflow_x*}/{@code workflow_z*} sibling
 * would have routed that sibling's publish requests at the BPMN double — surfacing as "publish returned 200 but the
 * API stayed Created", which reads as a product fault rather than a config one. As its own class the flip is
 * bracketed by this runner's own {@link #restoreWorkflowExecutors()}, so no ordering assumption is left to hold.
 *
 * <p>It still shares the {@code IntegrationV2-ApprovalWorkflow} block (and therefore one container) with
 * {@link WorkflowApprovalRunner}, which is safe ONLY because that block is {@code thread-count=1}: classes are
 * serialised, so each runner's {@code @AfterClass} restore completes before the next class starts, in either order.
 * That constraint is not new — the block already requires it for the approval flip. Raising the block's
 * thread-count would interleave the two flips and must not be done without splitting this into its own block.
 */
@CucumberOptions(
        features = "src/test/resources/features/admin/workflow_ws_api_state_change.feature",
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/admin-workflow-ws-api-state-change.html"}
)
public class WorkflowWsApiStateChangeRunner extends BaseBlockRunner {

    /**
     * Restores the workflow executors captured before this runner's flip, once after its scenarios and even on
     * failure. Safe to declare here as well as on {@link WorkflowApprovalRunner}: the shared helper keys captured
     * originals by container URL and {@code remove()}s them on restore, so whichever class runs first restores and
     * disarms, and the next class re-captures the pristine config before its own flip.
     */
    @AfterClass(alwaysRun = true)
    void restoreWorkflowExecutors() {
        WorkflowAdminSteps.restoreWorkflowExecutors();
    }
}
