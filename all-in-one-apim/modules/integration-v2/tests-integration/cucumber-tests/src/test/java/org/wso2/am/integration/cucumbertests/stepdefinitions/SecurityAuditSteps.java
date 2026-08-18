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

package org.wso2.am.integration.cucumbertests.stepdefinitions;

import io.cucumber.java.en.When;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.test.utils.Constants;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * API Security Audit glue (ports APISecurityAuditTestCase). Drives the single publisher resource that talks
 * to the external audit service, {@code GET /apis/{apiId}/auditapi}.
 *
 * <p>Only one step is needed because everything interesting is on the product side. That one request makes
 * the server perform a THREE-leg exchange with the audit service configured in
 * {@code artifacts/configFiles/is7txSecurityAudit/deployment.toml}:
 *
 * <ol>
 *   <li>if {@code AM_SECURITY_AUDIT_UUID_MAPPING} has no row for the API — {@code POST {base_url}} with the
 *       OpenAPI definition as a {@code multipart/form-data} part, reading the new audit id from
 *       {@code desc.id} and persisting it;</li>
 *   <li>otherwise — {@code PUT {base_url}/{auditUuid}} with the base64 definition;</li>
 *   <li>always — {@code GET {base_url}/{auditUuid}/assessmentreport?} for the report itself.</li>
 * </ol>
 *
 * <p>The response is an {@code AuditReport}: {@code report} (the base64 {@code data} field DECODED by the
 * server), {@code grade} and {@code numErrors} (lifted from {@code attr.data}) and {@code externalApiId}
 * (the audit id). Asserting all four in the feature is what proves the whole exchange happened rather than
 * just that the resource answered — the legacy test asserted only non-null and 200.
 *
 * <p>Response goes through the standard {@code Requests} funnel, so the shared
 * {@code The response status code should be} / {@code The value of response field ...} assertions apply.
 */
public class SecurityAuditSteps {

    /**
     * Requests the security audit report for an API. Non-asserting, so the feature can pin the status code
     * and every field of the report itself.
     */
    @When("I retrieve the security audit report for API {string}")
    public void iRetrieveTheSecurityAuditReport(String apiId) throws IOException {

        String actualApiId = TestContext.resolve(apiId).toString();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.publisherToken());

        Requests.get(Utils.getSecurityAuditURL(Utils.getBaseUrl(), actualApiId), headers);
    }
}
