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

import io.cucumber.java.en.Then;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Revision assertions that need more than a status/substring check, and therefore cannot be expressed with the
 * generic response steps (ports the two substantive halves of APIRevisionTestCase):
 *
 * <ul>
 *   <li><b>Deployment acknowledgement counts</b> — the gateway acks a revision deployment back to the publisher
 *       asynchronously, so the counts inside {@code deploymentInfo} start at zero and only become meaningful
 *       once the ack lands. The step polls for the ack and then asserts the whole shape AND the relations
 *       between the counts, which is the only thing that makes them more than three integers.</li>
 *   <li><b>Governance-registry artifact traces</b> — whether the registry collection backing an API still
 *       exists. There is no REST view of the registry, so this goes through the Carbon
 *       {@code ResourceAdminService} SOAP admin service ({@code getResourceData}, the same operation the legacy
 *       {@code ResourceAdminServiceClient} wraps), authenticated as the ACTING actor per §14.</li>
 * </ul>
 */
public class ApiRevisionSteps {

    /** Carbon registry-resource admin service — the only interface that can read a governance registry path. */
    private static final String RESOURCE_ADMIN_SERVICE_PATH = "services/ResourceAdminService";

    /** Axis2 namespace of the ResourceAdminService operations (from the generated stub's MY_QNAME). */
    private static final String RESOURCE_ADMIN_OP_NS = "http://services.resource.registry.carbon.wso2.org";

    /**
     * Registry collection holding one child collection per API, named by API id — {@code
     * APIConstants.API_REVISION_LOCATION}, where the API's REVISION artifacts live. It is created by the API's
     * first revision (NOT by the API create) and removed by {@code RegistryPersistenceImpl.deleteAPI}.
     */
    private static final String API_ARTIFACT_REGISTRY_PATH = "/_system/governance/apimgt/applicationdata/apis/";

    /**
     * Asserts the deployment-acknowledgement contract of a DEPLOYED revision: the revision's {@code
     * deploymentInfo} carries the three gateway counts, an environment name and a deployed time, and the counts
     * relate to each other consistently ({@code liveGatewayCount > 0}, {@code deployedGatewayCount > 0} and
     * {@code deployed + failed <= live}).
     *
     * <p>The relations are the point of the test. The counts alone are three integers that a broken gateway
     * notification path would report as {@code 0/0/0} while every field-presence check still passed; only
     * "there is a live gateway, it acknowledged this revision, and no more gateways acknowledged than exist"
     * distinguishes a real deployment from a silent one.</p>
     *
     * <p>The ack is asynchronous (the gateway reports it after the artifact is deployed), so the GET is retried
     * through {@link Utils#retryUntil} until the ack arrives, and the assertions are made on the LAST response —
     * the step fails on its own rather than leaning on a following {@code Then} (§7/§15).</p>
     *
     * @param revisionIdKey  context key holding the deployed revision's id
     * @param resourceType   {@code "apis"} / {@code "api-products"}
     * @param resourceIdKey  context key holding the resource id
     */
    @Then("The deployment info of revision {string} of {string} resource {string} should report acknowledged "
            + "gateway counts")
    public void deploymentInfoShouldReportAcknowledgedGatewayCounts(String revisionIdKey, String resourceType,
                                                                   String resourceIdKey)
            throws InterruptedException {

        String resourceId = TestContext.resolve(resourceIdKey).toString();
        String revisionId = TestContext.resolve(revisionIdKey).toString();
        String url = Utils.getRevisionURL(Utils.getBaseUrl(), resourceType, resourceId);
        Map<String, String> headers = Identity.publisherHeaders();

        HttpResponse response = Utils.retryUntil(Constants.RUNTIME_PROPAGATION_TIMEOUT,
                () -> Requests.get(url, headers),
                r -> r != null && r.getResponseCode() == 200 && r.getData() != null
                        && !r.getData().isBlank() && acknowledged(r.getData(), revisionId));

        Assert.assertTrue(response != null && response.getResponseCode() == 200 && response.getData() != null
                        && !response.getData().isBlank(),
                "Could not list revisions of " + resourceType + " " + resourceId + " to read deploymentInfo; got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));

        JSONObject revision = findRevision(response.getData(), revisionId);
        Assert.assertNotNull(revision, "Revision " + revisionId + " is not listed for " + resourceType + " "
                + resourceId + ": " + response.getData());

        JSONArray deploymentInfo = revision.optJSONArray("deploymentInfo");
        Assert.assertTrue(deploymentInfo != null && deploymentInfo.length() > 0,
                "Revision " + revisionId + " reports no deploymentInfo after deployment: " + revision);

        for (int i = 0; i < deploymentInfo.length(); i++) {
            JSONObject info = deploymentInfo.getJSONObject(i);
            String where = "revision " + revisionId + " deploymentInfo[" + i + "]=" + info;

            Assert.assertTrue(info.has("liveGatewayCount"), "liveGatewayCount is absent in " + where);
            Assert.assertTrue(info.has("deployedGatewayCount"), "deployedGatewayCount is absent in " + where);
            Assert.assertTrue(info.has("failedGatewayCount"), "failedGatewayCount is absent in " + where);

            int live = info.getInt("liveGatewayCount");
            int deployed = info.getInt("deployedGatewayCount");
            int failed = info.getInt("failedGatewayCount");

            Assert.assertTrue(live > 0, "No live gateway is reported, so the deployment was never acknowledged: "
                    + where);
            Assert.assertTrue(deployed > 0, "No gateway acknowledged the deployment: " + where);
            Assert.assertTrue(failed >= 0, "failedGatewayCount is negative: " + where);
            Assert.assertTrue(deployed + failed <= live, "More gateways acknowledged (deployed=" + deployed
                    + " + failed=" + failed + ") than are live (" + live + "): " + where);

            String environment = info.optString("name", null);
            Assert.assertTrue(environment != null && !environment.isBlank(),
                    "Gateway environment name is missing in " + where);
            String deployedTime = info.optString("deployedTime", null);
            Assert.assertTrue(deployedTime != null && !deployedTime.isBlank(),
                    "deployedTime is missing in " + where);
        }
    }

    /**
     * Asserts the API's governance-registry artifact collection EXISTS — the "before" half of the
     * deleted-API-traces check, and the guard that makes the "after" half meaningful (without it, a path that
     * never existed would satisfy the not-found assertion; that is not hypothetical — the path faults right
     * after a bare API create, because the collection is written by the API's first REVISION).
     */
    @Then("The governance registry artifact of API {string} should exist")
    public void governanceRegistryArtifactShouldExist(String apiIdKey) throws Exception {

        String apiId = TestContext.resolve(apiIdKey).toString();
        HttpResponse response = getResourceData(API_ARTIFACT_REGISTRY_PATH + apiId);

        Assert.assertEquals(response.getResponseCode(), 200,
                "ResourceAdminService.getResourceData failed for API " + apiId + ": " + response.getData());
        List<String> names = Utils.getNodeTextsByXPath(soapEnvelopeOf(response.getData()),
                "//*[local-name()='return']/*[local-name()='name']");
        Assert.assertTrue(names.contains(apiId), "The registry artifact collection of API " + apiId
                + " is not reported at " + API_ARTIFACT_REGISTRY_PATH + apiId + "; names=" + names
                + "; response=" + response.getData());
    }

    /**
     * Asserts the API's governance-registry artifact collection is GONE — i.e. deleting the API left no trace
     * in the registry. Axis2 answers a nonexistent path with a SOAP fault, which over plain HTTP is a 500
     * carrying the registry's own message; both are pinned, so the assertion cannot be satisfied by an
     * unrelated server error.
     */
    @Then("The governance registry artifact of API {string} should no longer exist")
    public void governanceRegistryArtifactShouldNoLongerExist(String apiIdKey) throws Exception {

        String apiId = TestContext.resolve(apiIdKey).toString();
        HttpResponse response = getResourceData(API_ARTIFACT_REGISTRY_PATH + apiId);

        Assert.assertEquals(response.getResponseCode(), 500,
                "Reading the registry path of the DELETED API " + apiId + " should fault, but got "
                        + response.getResponseCode() + "/" + response.getData());
        // Body presence first: a 500 with no body would otherwise NPE inside contains() and report nothing useful.
        Assert.assertTrue(response.getData() != null && !response.getData().isBlank(),
                "The fault for the deleted API " + apiId + " carried no body to match the registry's not-found "
                        + "message against");
        Assert.assertTrue(response.getData().contains("Resource does not exist at path"),
                "The fault for the deleted API " + apiId + " is not the registry's not-found fault: "
                        + response.getData());
    }

    /**
     * Reads one registry path through {@code ResourceAdminService.getResourceData} as the ACTING actor (§14) —
     * the response is the step's assertion target, so it goes through the {@code Requests} funnel.
     */
    private HttpResponse getResourceData(String registryPath) throws IOException {

        String envelope = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ser=\"" + RESOURCE_ADMIN_OP_NS + "\">"
                + "<soapenv:Header/><soapenv:Body>"
                + "<ser:getResourceData><ser:paths>" + Utils.escapeXml(registryPath) + "</ser:paths>"
                + "</ser:getResourceData>"
                + "</soapenv:Body></soapenv:Envelope>";

        User actor = Identity.actingActor();
        return Requests.soap(Utils.getBaseUrl() + RESOURCE_ADMIN_SERVICE_PATH, envelope, "urn:getResourceData",
                actor.getUserName(), actor.getPassword());
    }

    /**
     * Trims a ResourceAdminService response down to its SOAP envelope. The service answers MTOM, so the body
     * arrives as a MIME multipart ({@code --MIMEBoundary…} + part headers, then the envelope, then the closing
     * boundary) and handing it straight to an XML parser fails in the prolog. Cuts from the XML declaration (the
     * part headers carry a {@code Content-ID: <…@apache.org>}, so the first {@code '<'} is NOT the document) to
     * the last {@code '>'}, which is the envelope's close since only the boundary marker follows it.
     */
    private static String soapEnvelopeOf(String responseBody) {

        int start = responseBody.indexOf("<?xml");
        if (start < 0) {
            start = responseBody.indexOf("<soapenv:");
        }
        int end = responseBody.lastIndexOf('>');
        Assert.assertTrue(start >= 0 && end > start,
                "No SOAP envelope found in the ResourceAdminService response: " + responseBody);
        return responseBody.substring(start, end + 1);
    }

    /** True once the named revision reports at least one live gateway that acknowledged the deployment. */
    private boolean acknowledged(String revisionsBody, String revisionId) {

        JSONObject revision = findRevision(revisionsBody, revisionId);
        if (revision == null) {
            return false;
        }
        JSONArray deploymentInfo = revision.optJSONArray("deploymentInfo");
        if (deploymentInfo == null) {
            return false;
        }
        for (int i = 0; i < deploymentInfo.length(); i++) {
            JSONObject info = deploymentInfo.getJSONObject(i);
            if (info.optInt("liveGatewayCount") > 0 && info.optInt("deployedGatewayCount") > 0) {
                return true;
            }
        }
        return false;
    }

    /** The revision entry with the given id from a {@code {"list":[...]}} revisions response, or null. */
    private JSONObject findRevision(String revisionsBody, String revisionId) {

        JSONArray list;
        try {
            // A malformed body is "not found yet", not fatal: this runs inside a retry predicate, and a
            // JSONException would escape the loop (only IOException is retried) as an opaque parse error.
            list = new JSONObject(revisionsBody).optJSONArray("list");
        } catch (org.json.JSONException malformedDuringWarmup) {
            return null;
        }
        if (list == null) {
            return null;
        }
        for (int i = 0; i < list.length(); i++) {
            JSONObject revision = list.getJSONObject(i);
            if (revisionId.equals(revision.optString("id"))) {
                return revision;
            }
        }
        return null;
    }
}
