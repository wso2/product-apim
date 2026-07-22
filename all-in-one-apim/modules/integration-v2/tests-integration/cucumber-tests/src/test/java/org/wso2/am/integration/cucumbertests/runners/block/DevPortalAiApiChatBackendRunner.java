/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.cucumbertests.runners.block;

import io.cucumber.testng.CucumberOptions;

// _setup_published_apis.feature is listed first (the leading underscore sorts before letters, so cucumber-testng
// runs it first) — it publishes a REST API and hands off "publishedApiId" for the API Chat prepare scenario.
@CucumberOptions(
        features = {
                "src/test/resources/features/common/_setup_published_apis.feature",
                "src/test/resources/features/devportal/ai_api_chat_backend.feature"
        },
        glue = {"org.wso2.am.integration.cucumbertests.stepdefinitions"},
        plugin = {"pretty", "html:target/cucumber-report/devportal-ai-api-chat-backend.html"}
)
public class DevPortalAiApiChatBackendRunner extends BaseBlockRunner {
}
