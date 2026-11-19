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
import io.cucumber.java.en.Then;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

    private static final int NODE_AUDIT_APP_PORT = 3002;
    private static final String AUDIT_MOCK_OBSERVABILITY_PATH = "/auditapi/test-observability";

    /** Resets the mock's request history so the following product requests are distinguishable from earlier cases. */
    @When("I reset the security-audit mock request log")
    public void resetSecurityAuditMockRequestLog() throws IOException {
        HttpResponse response = Requests.post(Utils.getNodeBackendUrl(NODE_AUDIT_APP_PORT)
                + AUDIT_MOCK_OBSERVABILITY_PATH + "/reset", new HashMap<>(), "{}",
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertNotNull(response, "Security-audit mock reset returned no response");
        Assert.assertEquals(response.getResponseCode(), 200,
                "Unable to reset the security-audit mock request log: " + response.getData());
    }

    /** Confirms APIM called the mock's product-facing endpoints in the exact expected order. */
    @Then("the security-audit mock should have received methods {string}")
    public void securityAuditMockShouldHaveReceivedMethods(String expectedMethods) throws IOException {
        HttpResponse response = Requests.get(Utils.getNodeBackendUrl(NODE_AUDIT_APP_PORT)
                + AUDIT_MOCK_OBSERVABILITY_PATH, new HashMap<>());
        Assert.assertNotNull(response, "Security-audit mock observability endpoint returned no response");
        Assert.assertEquals(response.getResponseCode(), 200,
                "Unable to read security-audit mock request log: " + response.getData());
        JSONObject body = new JSONObject(response.getData());
        JSONArray methods = body.optJSONArray("methods");
        Assert.assertNotNull(methods, "Security-audit mock returned no methods array: " + body);
        JSONArray expected = new JSONArray();
        for (String method : expectedMethods.split(",")) {
            expected.put(method.trim());
        }
        Assert.assertEquals(methods.toString(), expected.toString(),
                "APIM audit integration request sequence differed from expected product behavior");
    }

    /**
     * Ports the legacy APISecurityAuditTestCase teardown with explicit HTTP contracts: list deployed revisions,
     * undeploy each, list all revisions, delete every revision, and wait until the gateway no longer serves the
     * API artifact. API deletion remains with the normal owner-aware @cleanup hook.
     */
    @When("I undeploy and delete revisions of API {string} and verify gateway removal")
    public void undeployAndDeleteApiRevisionsAndVerifyGatewayRemoval(String apiIdKey) throws Exception {
        String apiId = TestContext.resolve(apiIdKey).toString();
        Map<String, String> publisherHeaders = bearer(Identity.publisherToken());
        HttpResponse apiResponse = Requests.get(apiUrl(apiId), publisherHeaders);
        assertStatus(apiResponse, 200, "read API before revision teardown");
        JSONObject api = new JSONObject(apiResponse.getData());
        String apiName = api.getString("name");
        String apiVersion = api.getString("version");
        String provider = api.getString("provider");
        int domainSeparator = provider.lastIndexOf('@');
        String tenantDomain = domainSeparator < 0 ? Constants.SUPER_TENANT_DOMAIN
                : provider.substring(domainSeparator + 1);

        HttpResponse deployedResponse = Requests.get(Utils.getRevisionDeployments(Utils.getBaseUrl(), "apis", apiId),
                publisherHeaders);
        assertStatus(deployedResponse, 200, "list deployed revisions");
        JSONArray deployed = new JSONObject(deployedResponse.getData()).optJSONArray("list");
        Assert.assertNotNull(deployed, "Deployed-revisions response has no list: " + deployedResponse.getData());
        Assert.assertTrue(deployed.length() > 0,
                "Security-audit API had no deployed revisions to clean up; API=" + apiId);
        for (int i = 0; i < deployed.length(); i++) {
            String revisionId = deployed.getJSONObject(i).getString("id");
            JSONArray target = new JSONArray().put(new JSONObject()
                    .put("name", System.getenv(Constants.GATEWAY_ENVIRONMENT))
                    .put("vhost", JSONObject.NULL)
                    .put("displayOnDevportal", true));
            HttpResponse undeploy = Requests.post(
                    Utils.getRevisionUnDeploymentURL(Utils.getBaseUrl(), "apis", apiId, revisionId),
                    publisherHeaders, target.toString(), Constants.CONTENT_TYPES.APPLICATION_JSON);
            assertStatus(undeploy, 201, "undeploy revision " + revisionId);
        }

        HttpResponse allRevisionsResponse = Requests.get(Utils.getRevisionURL(Utils.getBaseUrl(), "apis", apiId),
                publisherHeaders);
        assertStatus(allRevisionsResponse, 200, "list all revisions after undeploy");
        JSONArray allRevisions = new JSONObject(allRevisionsResponse.getData()).optJSONArray("list");
        Assert.assertNotNull(allRevisions, "Revision response has no list: " + allRevisionsResponse.getData());
        for (int i = 0; i < allRevisions.length(); i++) {
            String revisionId = allRevisions.getJSONObject(i).getString("id");
            HttpResponse deleted = Requests.delete(
                    Utils.getRevisionByID(Utils.getBaseUrl(), "apis", apiId, revisionId), publisherHeaders);
            assertStatus(deleted, 200, "delete revision " + revisionId);
        }

        String gatewayUrl = Utils.getGatewayArtifactURL(Utils.getBaseGatewayManagementUrl(), "api-artifact",
                apiName, apiVersion, tenantDomain);
        User gatewayAdmin = Identity.gatewayManagementAdmin();
        String credentials = gatewayAdmin.getUserName() + ":" + gatewayAdmin.getPassword();
        Map<String, String> gatewayHeaders = new HashMap<>();
        gatewayHeaders.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Basic "
                + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        HttpResponse last = Utils.retryUntilWithInterval(Constants.RUNTIME_PROPAGATION_TIMEOUT, 1000L, () -> {
            HttpResponse response = SimpleHTTPClient.getInstance().doGet(gatewayUrl, gatewayHeaders);
            if (response.getResponseCode() != 200 && response.getResponseCode() != 404) {
                throw new IllegalStateException("Unexpected Gateway artifact response during removal convergence: "
                        + response.getResponseCode() + "/" + response.getData());
            }
            return response;
        }, response -> response.getResponseCode() == 404);
        Assert.assertNotNull(last, "Gateway artifact probe returned no response after revision teardown");
        Assert.assertEquals(last.getResponseCode(), 404,
                "Gateway still serves API artifact after all revisions were undeployed/deleted; API=" + apiId
                        + ", last=" + last.getResponseCode() + "/" + last.getData());
        // The API itself is deliberately left registered for ResourceCleanup's owner-aware API-delete contract.
    }

    private static String apiUrl(String apiId) {
        return Utils.getBaseUrl() + Constants.DEFAULT_APIM_API_DEPLOYER + "apis/" + apiId;
    }

    private static Map<String, String> bearer(String token) {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.REQUEST_HEADERS.AUTHORIZATION, "Bearer " + token);
        return headers;
    }

    private static void assertStatus(HttpResponse response, int expected, String action) {
        Assert.assertNotNull(response, "No HTTP response while trying to " + action);
        Assert.assertEquals(response.getResponseCode(), expected,
                "Failed to " + action + ": " + response.getResponseCode() + "/" + response.getData());
    }

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
