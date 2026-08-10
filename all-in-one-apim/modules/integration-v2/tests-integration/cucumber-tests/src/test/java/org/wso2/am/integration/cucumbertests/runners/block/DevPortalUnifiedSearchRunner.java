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
 * Runner for devportal/unified_search — the {@code GET /search} endpoint on both planes: document-content search
 * (the ContentSearchTestCase parity), the mixed API/DEFINITION/DOC result shape, and index withdrawal on document
 * delete. Self-contained {@code @cleanup} scenarios. Extends {@link BaseBlockRunner} for the block boot-failure
 * guard and runner-scoped cleanup safety net.
 */
@CucumberOptions(
        features = "src/test/resources/features/devportal/unified_search.feature",
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/devportal-unified-search.html"}
)
public class DevPortalUnifiedSearchRunner extends BaseBlockRunner {
}
