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
 * Runner for Moesif analytics event publishing: request/response body capture, header redaction and masking,
 * dynamic sampling and the retry queue.
 *
 * <p>Sole runner of the IntegrationV2-Analytics block, which is the only container with
 * {@code [apim.analytics] enable = true}. That switch is container-global — the block's mock collector receives
 * an event for EVERY gateway invocation in the container — which is why the block is thread-count=1 and why this
 * runner does not share it.
 *
 * <p>The {@code _setup_} prefix on the fixture feature is load-bearing: a runner's features execute in
 * LEXICOGRAPHIC filename order, not array order, so the underscore is what guarantees the API, application and
 * token exist before the scenarios that invoke through them. Its resources are swept by BaseBlockRunner's
 * AfterClass hook rather than per-scenario, because the scenarios consume that fixture.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/analytics/_setup_moesif_analytics_api.feature",
                "src/test/resources/features/analytics/moesif_analytics_events.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/analytics-moesif-events.html"}
)
public class AnalyticsMoesifEventsRunner extends BaseBlockRunner {
}
