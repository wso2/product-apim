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
 * Runner for the IntegrationV2-ServerRestart block: a shared scope persists across a graceful server restart
 * and stays editable (the legacy SharedScopeTestWithRestart). Shares the block with
 * {@code ServerRestartTokenPersistenceRunner}; the block runs sequentially ({@code thread-count=1}) because
 * the restart bounces the shared server, and its overlay enables {@code [server] enable_restart_from_api}.
 * Teardown is the per-scenario {@code @cleanup} hook.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/publisher/shared_scope_restart.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/server-restart-shared-scope.html"}
)
public class ServerRestartSharedScopeRunner extends BaseBlockRunner {
}
