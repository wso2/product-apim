/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.am.integration.cucumbertests.runners.block;

import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.AfterClass;
import org.wso2.am.integration.cucumbertests.stepdefinitions.WorkflowAdminSteps;

/**
 * Runner for the custom subscription HTTP-redirect executor, whose JAR is installed into components/lib at container
 * boot. Its feature selects that executor by writing an alternate workflow-extensions.xml into the governance
 * registry; {@link #restoreWorkflowExecutors()} undoes it once after the runner, even on failure. It shares the
 * thread-count=1 IntegrationV2-ApprovalWorkflow block with the other workflow-executor runners, each of which
 * flips and restores the same server-global configuration in its own AfterClass. It is listed first in that block:
 * the product clears its workflow-executor cache before the write commits, so a configuration load still in flight
 * from an earlier runner could keep the previous executor cached for the subscription this scenario asserts on.
 */
@CucumberOptions(
        features = "src/test/resources/features/admin/workflow_subscription_http_redirect.feature",
        glue = {"org.wso2.am.integration.cucumbertests.stepdefinitions"},
        plugin = {"pretty", "html:target/cucumber-report/admin-workflow-subscription-http-redirect.html"}
)
public class SubscriptionWorkflowRedirectRunner extends BaseBlockRunner {

    @AfterClass(alwaysRun = true)
    void restoreWorkflowExecutors() {
        WorkflowAdminSteps.restoreWorkflowExecutors();
    }
}
