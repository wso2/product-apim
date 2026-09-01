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
 * Runner for the network access-control URL-gate private-block tests - the port of the HostValidationPrivateBlockTestCase URL-gate cases. Under the private-block deny + block-private-network policy, endpoint validation, Key Manager creation and WSDL import from a loopback/link-local URL are rejected before any fetch. Runs in the network-access-control-private-block container.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/publisher/network_access_control_url_privateblock.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/publisher-network-access-control-url-privateblock.html"}
)
public class NetworkAccessControlUrlPrivateBlockRunner extends BaseBlockRunner {
}
