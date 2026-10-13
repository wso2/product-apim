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
 * Runner for the LOG tracing leg of distributed tracing. Belongs to a block whose {@code tomlExtraOverlayPath}
 * is {@code artifacts/configFiles/distributedTracing/logTracing.toml}, which turns the remote tracer OFF and the
 * log tracer ON ({@code remote_tracer.enable = false}, {@code log_tracer.enable = true}). The gateway then
 * writes each exported span as a TRACE line into {@code wso2-apimgt-open-tracing.log} inside the APIM container
 * instead of POSTing it to a collector, and the feature reads that file back and asserts on the lines.
 *
 * <p>The remote legs (Jaeger gRPC, Moesif OTLP/HTTP, Zipkin) live in their own runners rather than here: a
 * server has one tracer output selected at startup, so each leg needs its own container.
 */
@CucumberOptions(
        features = {"src/test/resources/features/analytics/tracing_log.feature"},
        glue = {"org.wso2.am.integration.cucumbertests.stepdefinitions"},
        plugin = {"pretty", "html:target/cucumber-report/analytics-tracing-log-tracing.html"}
)
public class AnalyticsTracingLogTracingRunner extends BaseBlockRunner {
}