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
 * Runner for correlation logging (ports CorrelationLoggingTest).
 *
 * <p>Correlation components are container-global switches, and one scenario RESTARTS the server to prove the
 * configuration is persisted — so this class cannot share a container with anything running concurrently.
 * Its block is {@code thread-count=1} for that reason.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/analytics/correlation_logging.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/analytics-correlation-logging.html"}
)
public class AnalyticsCorrelationLoggingRunner extends BaseBlockRunner {
}
