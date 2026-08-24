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
 * Runner for the ELK analytics metric log (ports APIMAnalyticsTest and
 * ELKAnalyticsWithRespondMediatorTestCase).
 *
 * <p>Needs its own block: {@code [apim.analytics] enable/type} is server-global config, and once it is on every
 * invocation in the container writes an event to {@code apim_metrics.log}.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/analytics/elk_metrics.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/analytics-elk-metrics.html"}
)
public class AnalyticsElkMetricsRunner extends BaseBlockRunner {
}
