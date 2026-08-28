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
 */

package org.wso2.am.integration.cucumbertests.runners.block;

import io.cucumber.testng.CucumberOptions;

/**
 * Dedicated runner for the gateway deployment-acknowledgement contract. Keeping this scenario in its own
 * block makes its strict acknowledgement assertion observable independently from the high-churn Publisher
 * block, while retaining the Publisher actors and API lifecycle setup.
 */
@CucumberOptions(
        features = {"src/test/resources/features/publisher/api_revisions.feature"},
        glue = {"org.wso2.am.integration.cucumbertests.stepdefinitions"},
        plugin = {"pretty", "html:target/cucumber-report/publisher-api-revision-acknowledgement.html"},
        tags = "@rule:deployment-ack"
)
public class PublisherAPIRevisionAcknowledgementRunner extends BaseBlockRunner {
}
