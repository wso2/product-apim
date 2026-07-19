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
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Step definitions for the gateway internal REST API artifact surface (port of GatewayRestAPITestCase). Retrieves a
 * deployed API's synapse artifacts — the API artifact, its endpoints, its local entry and its mediation sequences —
 * from {@code api/am/gateway/v2/{api-artifact|end-points|local-entry|sequence}?apiName=&version=&tenantDomain=}.
 *
 * <p>Pinned live: this gateway REST API authenticates with BASIC admin credentials (a Bearer token is rejected
 * 401), so these steps send the acting actor's tenant-admin Basic auth (not the publisher/admin OAuth token). The
 * response is published as {@code httpResponse} for the generic assertion steps.
 */
public class GatewayRestArtifactsSteps {

    private final BaseSteps baseSteps = new BaseSteps();

    private String getBaseUrl() {
        return baseSteps.getBaseUrl();
    }

    /** Basic-auth header for the acting actor's tenant admin (the gateway REST API needs admin Basic auth). */
    private Map<String, String> gatewayBasicAuthHeaders() {
        User admin = Identity.actingTenantAdmin();
        String creds = admin.getUserName() + ":" + admin.getPassword();
        String encoded = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic " + encoded);
        return headers;
    }

    /**
     * Retrieves a gateway synapse artifact ({@code kind} = {@code api-artifact} / {@code end-points} /
     * {@code local-entry} / {@code sequence}) for the API named {@code apiName} version {@code version} in
     * {@code tenantDomain}, publishing the response for assertion. Name/version resolve {@code {{...}}}.
     */
    @When("I retrieve the gateway {string} for API {string} version {string} in tenant {string}")
    public void iRetrieveGatewayArtifact(String kind, String apiName, String version, String tenantDomain)
            throws IOException {
        String resolvedName = Utils.resolveContextPlaceholders(apiName);
        String resolvedVersion = Utils.resolveContextPlaceholders(version);
        Requests.get(Utils.getGatewayArtifactURL(getBaseUrl(), kind, resolvedName, resolvedVersion, tenantDomain),
                gatewayBasicAuthHeaders());
    }

    /**
     * As above, but polls until the gateway returns 200 — the synapse artifact is not queryable immediately after a
     * deploy returns 201 (the gateway materialises it asynchronously), so a first query can 404. Retries until 200
     * or the deadline, then publishes the last response for the following assertions.
     */
    @When("I retrieve the gateway {string} for API {string} version {string} in tenant {string} until it is available within {int} seconds")
    public void iRetrieveGatewayArtifactUntilAvailable(String kind, String apiName, String version,
            String tenantDomain, int timeoutSeconds) throws IOException, InterruptedException {
        String resolvedName = Utils.resolveContextPlaceholders(apiName);
        String resolvedVersion = Utils.resolveContextPlaceholders(version);
        String url = Utils.getGatewayArtifactURL(getBaseUrl(), kind, resolvedName, resolvedVersion, tenantDomain);
        long endTime = System.currentTimeMillis()
                + Math.max(timeoutSeconds * 1000L, Constants.DEPLOYMENT_WAIT_TIME);
        HttpResponse response;
        do {
            response = Requests.get(url, gatewayBasicAuthHeaders());
            if (response != null && response.getResponseCode() == 200) {
                return;
            }
            Thread.sleep(2000);
        } while (System.currentTimeMillis() < endTime);
        Assert.assertEquals(response == null ? -1 : response.getResponseCode(), 200,
                "Gateway artifact '" + kind + "' for " + resolvedName + " did not become available within "
                        + timeoutSeconds + "s; last: " + (response == null ? "null" : response.getData()));
    }
}
