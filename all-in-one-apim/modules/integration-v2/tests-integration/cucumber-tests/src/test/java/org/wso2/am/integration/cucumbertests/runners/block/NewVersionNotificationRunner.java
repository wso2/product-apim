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
 * Runner for the new-API-version subscriber notification email (the port of the legacy NotificationTestCase).
 * Owns the IntegrationV2-NewVersionNotification block: its overlay repoints {@code [output_adapter.email]} at the
 * node backend's SMTP capture sink, which is server-wide config, and the feature mutates the container-wide
 * tenant configuration — hence its own block at thread-count=1.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/publisher/version_notification_email.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/publisher-version-notification-email.html"}
)
public class NewVersionNotificationRunner extends BaseBlockRunner {
}
