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
 * Step definitions for admin-plane API provider change (port of ChangeApiProviderTestCase). Uses the admin REST
 * {@code POST /api/am/admin/v4/apis/{apiId}/change-provider?provider=<name>} operation. Publishes the response as
 * {@code httpResponse} so the feature asserts the exact status (200 success, 400 + 901409 on a cross-tenant
 * provider). The retained-metadata assertions (provider/docs/scopes/endpoints) are made by the feature via a
 * subsequent Publisher GET.
 */
public class ApiProviderChangeSteps {

    private final BaseSteps baseSteps = new BaseSteps();

    private String getBaseUrl() {
        return baseSteps.getBaseUrl();
    }

    /**
     * Changes an API's provider to {@code providerName} via the admin change-provider REST operation, using the
     * acting actor's admin token. {@code providerName} resolves {@code {{...}}} placeholders (so a provisioned
     * user's key flows through). Non-asserting — the feature asserts the resulting status (and, on success, the
     * retained metadata via a following GET).
     *
     * @param apiIdKey     context key holding the API id
     * @param providerName the new provider username (may be tenant-qualified or {@code SECONDARY/}-prefixed)
     */
    @When("I change the provider of API {string} to {string}")
    public void iChangeApiProvider(String apiIdKey, String providerName) throws IOException {
        String apiId = TestContext.resolve(apiIdKey).toString();
        String provider = Utils.resolveContextPlaceholders(providerName);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + Identity.adminToken());
        // change-provider is a POST with the new provider as a query param and no body.
        Requests.post(Utils.getAPIProvider(getBaseUrl(), apiId, provider), headers, "",
                Constants.CONTENT_TYPES.APPLICATION_JSON);
    }
}
