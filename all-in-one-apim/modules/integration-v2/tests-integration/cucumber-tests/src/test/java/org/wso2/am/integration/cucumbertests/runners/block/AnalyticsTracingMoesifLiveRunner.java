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
 * Runner for the LIVE OTLP/HTTP leg — the one that exports to the real Moesif collector rather than to a local
 * stand-in (the hermetic ingest stand-in leg has been retired). Belongs to a block whose
 * {@code tomlExtraOverlayPath} is {@code artifacts/configFiles/distributedTracing/moesifOtlpHttpLive.toml},
 * pointing {@code remote_tracer.url} at {@code https://api.moesif.net/v1/traces} with the collector application
 * id taken from the environment.
 *
 * <p>Delivery is asserted by querying Moesif's own Management API through {@code MoesifManagementApiClient}, which needs a Management API
 * key — a different credential from the collector application id the gateway exports with. The scenario skips
 * itself when either credential is absent, so a job without the secrets reports skipped rather than failed.
 *
 * <p>The block needs outbound network access to Moesif.
 */
@CucumberOptions(
        features = {"src/test/resources/features/analytics/tracing_moesif_live.feature"},
        glue = {"org.wso2.am.integration.cucumbertests.stepdefinitions"},
        plugin = {"pretty", "html:target/cucumber-report/analytics-tracing-moesif-live.html"}
)
public class AnalyticsTracingMoesifLiveRunner extends BaseBlockRunner {
}