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
 * Runner for the OTLP/gRPC leg of distributed tracing. Belongs to a block that sets
 * {@code initOtlpCollector=true} and a {@code tomlExtraOverlayPath} of
 * {@code artifacts/configFiles/distributedTracing/jaegerOtlpGrpc.toml} — the overlay selects the
 * {@code jaeger} tracer (which hardcodes the gRPC exporter) and points its {@code remote_tracer.hostname} at
 * the collector's network alias, so the block boots {@code DynamicOtlpCollector} before APIM.
 *
 * <p>The live OTLP/HTTP leg lives in {@link AnalyticsTracingMoesifLiveRunner} rather than here: a server has
 * one remote tracer and the protocol is fixed at startup, so the two transports need two containers.
 */
@CucumberOptions(
        features = {"src/test/resources/features/analytics/tracing_otlp_grpc.feature"},
        glue = {"org.wso2.am.integration.cucumbertests.stepdefinitions"},
        plugin = {"pretty", "html:target/cucumber-report/analytics-tracing-otlp-grpc.html"}
)
public class AnalyticsTracingOtlpGrpcRunner extends BaseBlockRunner {
}
