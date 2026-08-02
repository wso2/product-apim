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
 * Runner for the approval-workflow suite — ports {@code WorkflowApprovalExecutorTest}. The
 * {@code _setup_workflow_executors} feature flips the product's Simple workflow executors to the Approval
 * variants by writing an alternate {@code workflow-extensions.xml} into the governance registry (see
 * {@link WorkflowAdminSteps}); the per-type features then trigger an action (app create, subscribe, key gen,
 * API state change, revision deploy, DevPortal self sign-up, ...) and assert it parks pending until an admin
 * approves/rejects it.
 *
 * <p>Runs in its own {@code thread-count=1} block: the executor selection is server-global and persists in the
 * shared registry DB, so no sibling block may share the container while it is flipped, and the flip is restored
 * after all scenarios by {@link #restoreWorkflowExecutors()} below (a runner-scoped {@code @AfterClass}, so it
 * runs once even on scenario failure — a per-scenario hook would delete the shared fixture out from under the
 * following scenarios).
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/admin/_setup_workflow_executors.feature",
                "src/test/resources/features/admin/workflow_subscription_creation.feature",
                "src/test/resources/features/admin/workflow_application_creation.feature",
                "src/test/resources/features/admin/workflow_api_state_change.feature",
                "src/test/resources/features/admin/workflow_application_update.feature",
                "src/test/resources/features/admin/workflow_revision_deployment.feature",
                "src/test/resources/features/admin/workflow_subscription_update.feature",
                "src/test/resources/features/admin/workflow_subscription_deletion.feature",
                "src/test/resources/features/admin/workflow_application_deletion.feature",
                "src/test/resources/features/admin/workflow_key_registration.feature",
                "src/test/resources/features/admin/workflow_pending_cleanup.feature",
                "src/test/resources/features/admin/workflow_user_signup.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/admin-workflow-approval.html"}
)
public class WorkflowApprovalRunner extends BaseBlockRunner {

    /**
     * Restores the original workflow-extensions.xml once after all scenarios (idempotent, failure-safe), so the
     * shared registry DB is not left with the Approval executors enabled for a later run.
     */
    @AfterClass(alwaysRun = true)
    void restoreWorkflowExecutors() {
        WorkflowAdminSteps.restoreWorkflowExecutors();
    }
}
