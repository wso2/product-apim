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
 * Runner for admin/secondary_userstore — a runtime-added JDBC secondary user store with case-insensitive usernames
 * (the port of SecondaryUserStoreCaseInsensitiveTestCase) plus the ×4 store-user-as-actor coverage. Runs in its
 * OWN thread-count=1 block: the secondary store is container-global, so no sibling class may share the container
 * while it is (un)deployed. The scenarios are self-contained (each creates its own prerequisites inline) — the
 * shared {@code common/_setup_published_apis} fixture is NOT reused here because cucumber-testng orders features by
 * full URI, and {@code admin/…} sorts before {@code common/…}, so that setup would run last.
 */
@CucumberOptions(
        features = "src/test/resources/features/admin/secondary_userstore.feature",
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/admin-secondary-userstore.html"}
)
public class AdminSecondaryUserStoreRunner extends BaseBlockRunner {
}
