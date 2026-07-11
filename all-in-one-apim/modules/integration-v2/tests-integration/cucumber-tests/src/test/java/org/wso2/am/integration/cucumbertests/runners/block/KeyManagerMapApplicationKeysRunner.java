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
 * Runner for map-application-keys (bind a pre-existing/BYO OAuth client to an application) — the port of the
 * legacy ApplicationTestCase#mapApplicationKeys / mapApplicationKeysNegative arc. Runs in the standard
 * IntegrationV2-KeyManager block; the map targets the resident Key Manager which ships in every pack, so no
 * external Key Manager backend or config overlay is needed.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/key-manager/map_application_keys.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/keymanager-map-application-keys.html"}
)
public class KeyManagerMapApplicationKeysRunner extends BaseBlockRunner {
}
