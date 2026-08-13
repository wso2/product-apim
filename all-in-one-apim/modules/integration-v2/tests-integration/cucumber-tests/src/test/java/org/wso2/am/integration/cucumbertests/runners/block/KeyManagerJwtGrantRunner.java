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
 * Runner for key-manager/jwt-grant — the jwt-bearer grant against a trusted external IdP registered over the
 * Carbon IdentityProviderMgtService SOAP admin service: valid assertion exchange (both tenants), the negative
 * paths (unregistered issuer, expired, tampered, wrong signing cert), and role-mapped scope issuance before/
 * after an IdP role-config update (super tenant). Extends {@link BaseBlockRunner}.
 */
@CucumberOptions(
        features = "src/test/resources/features/key-manager/jwt_grant.feature",
        glue = {"org.wso2.am.integration.cucumbertests.stepdefinitions"},
        plugin = {"pretty", "html:target/cucumber-report/key-manager-jwt-grant.html"}
)
public class KeyManagerJwtGrantRunner extends BaseBlockRunner {
}
