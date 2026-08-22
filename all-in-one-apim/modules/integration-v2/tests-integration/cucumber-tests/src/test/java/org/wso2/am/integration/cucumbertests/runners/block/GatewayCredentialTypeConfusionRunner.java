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
 * Runner for gateway/credential-type confusion — each auth header accepts only its own credential kind. The
 * {@code _setup_} fixture publishes the two application-security APIs and mints the three live credentials
 * (OAuth token, application API key, internal key); it sorts first by filename so it runs before the matrix
 * (execution order is lexicographic by feature filename, not array order). Teardown is the runner's AfterClass
 * sweep, so the consumer feature is deliberately not {@code @cleanup}.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/gateway/_setup_credential_type_confusion.feature",
                "src/test/resources/features/gateway/credential_type_confusion.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/gateway-credential-type-confusion.html"}
)
public class GatewayCredentialTypeConfusionRunner extends BaseBlockRunner {
}
