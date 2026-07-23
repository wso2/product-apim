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
 * Runner for the token-exchange DISABLED negative, co-hosted in the IntegrationV2-SandboxAndTokenExchangeDisabled
 * block (testng-v2.xml). Boots APIM with the grant
 * disabled ([oauth.grant_type.token_exchange] enable=false) and asserts the token endpoint refuses a
 * token-exchange request with unsupported_grant_type. No external IS and no key manager are needed - the grant
 * is rejected at dispatch, before any subject token is processed.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/admin/_setup_token_exchange_disabled.feature",
                "src/test/resources/features/admin/token_exchange_disabled.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/token-exchange-disabled.html"}
)
public class TokenExchangeDisabledRunner extends BaseBlockRunner {
}
