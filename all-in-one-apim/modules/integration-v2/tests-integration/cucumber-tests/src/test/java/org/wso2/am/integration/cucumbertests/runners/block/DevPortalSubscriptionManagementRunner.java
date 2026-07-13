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
 * Runner for devportal/subscription-management — subscription block/unblock and throttling-plan update,
 * across both tenants. Self-contained {@code @cleanup} scenario outlines. Extends {@link BaseBlockRunner}
 * for the block boot-failure guard and runner-scoped cleanup safety net.
 */
@CucumberOptions(
        features = "src/test/resources/features/devportal/subscription_management.feature",
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/devportal-subscription-management.html"}
)
public class DevPortalSubscriptionManagementRunner extends BaseBlockRunner {
}
