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
 * Runner for the publisher's external API security audit (ports APISecurityAuditTestCase).
 *
 * <p>Needs the {@code [security_audit]} overlay and the node backend, which carries the
 * {@code am-auditApi-sample} mock audit service the overlay points at. The overlay is inert for every other
 * flow — {@code getAuditReport} has a single product call site, the {@code /apis/{id}/auditapi} resource, and
 * nothing in API create/update/publish consults it — so this feature can co-host with any block whose config
 * it does not otherwise disturb.
 */
@CucumberOptions(
        features = {
                "src/test/resources/features/publisher/security_audit.feature"
        },
        glue = {
                "org.wso2.am.integration.cucumbertests.stepdefinitions"
        },
        plugin = {"pretty", "html:target/cucumber-report/publisher-security-audit.html"}
)
public class PublisherSecurityAuditRunner extends BaseBlockRunner {
}
