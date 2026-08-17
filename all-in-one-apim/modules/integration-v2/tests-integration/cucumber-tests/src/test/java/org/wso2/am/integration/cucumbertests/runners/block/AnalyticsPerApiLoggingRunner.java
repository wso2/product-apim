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
 * Runner for per-API / per-resource log levels (ports APILoggingTest).
 *
 * <p>Co-hosts with correlation logging: the two drive different devops resources and assert against different
 * log files ({@code api.log} vs {@code correlation.log}), and neither one's configuration changes what the
 * other emits. It shares that block's {@code thread-count=1} because a sibling there restarts the server.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/analytics/per_api_logging.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/analytics-per-api-logging.html"}
)
public class AnalyticsPerApiLoggingRunner extends BaseBlockRunner {
}
