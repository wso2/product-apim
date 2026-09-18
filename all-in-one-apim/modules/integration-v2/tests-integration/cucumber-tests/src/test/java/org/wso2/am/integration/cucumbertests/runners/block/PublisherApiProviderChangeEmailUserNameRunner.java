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
 * Admin-plane API provider transfer TO an email-form username. Rides the IntegrationV2-EmailUserName block
 * because the dimension is a CONTAINER MODE (emailUserMode + enable_email_domain), not an actor that could be
 * added as Examples rows to the publisher block's api_provider_change.feature. The plain-username transfer
 * assertions stay in {@link PublisherApiProviderChangeRunner}; only the e-mail-domain round trip is added here.
 */
@CucumberOptions(
        features = "src/test/resources/features/publisher/api_provider_change_email_username.feature",
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/publisher-api-provider-change-email-username.html"}
)
public class PublisherApiProviderChangeEmailUserNameRunner extends BaseBlockRunner {
}
