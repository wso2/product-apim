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
 * Runner for the SECONDARY.COM store-principal token arcs (token endpoint / userinfo / gateway).
 *
 * <p><b>Why these are not simply part of {@link KeyManagerTokenIssuanceRunner}.</b> That runner is registered in
 * TWO blocks — {@code IntegrationV2-KeyManager} and {@code IntegrationV2-EmailUserName} — so every scenario in
 * {@code token_issuance.feature} runs twice, under two different server configurations. These arcs can only pass
 * under the first. Measured in the email-username block: the {@code carbon.super} row passes, but
 * {@code SECONDARY.COM/subscriberUser1@tenant1.com} is refused 401, because {@code EnableEmailUserName} splits a
 * username on its LAST "@" only when it carries TWO or more — a single-"@" store principal therefore resolves to
 * carbon.super and never to tenant1. That is the rule {@code gateway/basic_auth_email_username.feature} exists to
 * pin, so the arcs are kept out of that block rather than the store being seeded into it.
 *
 * <p>Register this ONLY in a block that sets {@code initSecondaryUserStore} (the seeder provisions
 * SECONDARY.COM/subscriberUser1 and SECONDARY.COM/publisherUser1 per tenant) and does NOT set
 * {@code emailUserMode}.
 */
@CucumberOptions(
        features = "src/test/resources/features/key-manager/token_issuance_store_principal.feature",
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/keymanager-token-issuance-store-principal.html"}
)
public class KeyManagerTokenIssuanceStorePrincipalRunner extends BaseBlockRunner {
}
