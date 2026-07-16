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
 * Runner for conditional-group advanced throttling enforcement (IP / header / query-param / JWT-claim). Runs in
 * the IntegrationV2-ConditionalThrottling block, whose overlay enables the header/query/JWT-claim throttling
 * flags that are off by default — so a matching condition actually trips its lower group limit. Ports the
 * enforcement arc of JWTRequestCountThrottlingTestCase.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/gateway/conditional_throttling.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/conditional-throttling.html"}
)
public class ConditionalThrottlingRunner extends BaseBlockRunner {
}
