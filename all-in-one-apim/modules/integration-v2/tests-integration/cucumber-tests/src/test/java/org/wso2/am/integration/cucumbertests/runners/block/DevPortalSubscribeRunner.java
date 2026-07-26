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
 * Runner for devportal/subscribe. Demonstrates the {@code _setup_*} fixture pattern on the consumer plane:
 * {@code _setup_published_apis} is listed FIRST (publishes a REST API per tenant as that tenant's admin into
 * the runner's local scope), then {@code subscribe} (a subscriber-role consumer subscribes its application to
 * that API). Array order is execution order. The published API is NOT torn down per-scenario —
 * {@link BaseBlockRunner}'s {@code @AfterClass} sweep deletes it after all scenarios complete.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/common/_setup_published_apis.feature",
                "src/test/resources/features/devportal/subscribe.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/devportal-subscribe.html"}
)
public class DevPortalSubscribeRunner extends BaseBlockRunner {
}
