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
 * Re-runs existing TokenAPITestCase scenarios and successful JWTGrantTestCase scenarios with OAuth token persistence
 * enabled. The runner is registered in the serialized server-restart block so these scenarios share the persistence
 * overlay without duplicating their definitions. The tagged security-enforcement case retains the invalid-token and
 * most-popular rejection checks.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/key-manager/token_issuance.feature",
                "src/test/resources/features/key-manager/jwt_grant.feature",
                "src/test/resources/features/gateway/security_enforcement.feature"
        },
        tags = "@legacy:TokenAPITestCase or (@legacy:JWTGrantTestCase and (@type:smoke or @feat:scope-issuance))",
        glue = {"org.wso2.am.integration.cucumbertests.stepdefinitions"},
        plugin = {"pretty", "html:target/cucumber-report/keymanager-token-persistence.html"}
)
public class KeyManagerTokenPersistenceRunner extends BaseBlockRunner {
}
