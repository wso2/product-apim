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
import io.cucumber.java.en.When;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

import java.util.Map;

import java.io.IOException;

/**
 * Step definitions for the approval-workflow suite (ports {@code WorkflowApprovalExecutorTest}). No workflow
 * admin glue existed in integration-v2 before this class.
 *
 * <p>Two concerns live here:
 * <ol>
 *   <li><b>Executor selection (setup/teardown).</b> The product ships {@code SimpleWorkflowExecutor}s
 *       (auto-approve). The suite flips them to the {@code *ApprovalWorkflowExecutor}s (manual/pending) by
 *       writing an alternate {@code workflow-extensions.xml} into the governance registry via the Carbon
 *       {@code ResourceAdminService} SOAP admin service — the ONLY interface for that registry resource (there
 *       is no REST equivalent), so §14's narrow SOAP exception applies. The flip is picked up live (no restart)
 *       and is server-global; it persists in the shared registry DB, so the block runs isolated
 *       ({@code thread-count=1}) and the original content is read before the write and restored by the runner's
 *       {@code @AfterClass} (see {@code WorkflowApprovalRunner}). Authenticated as the acting actor's carbon
 *       credentials (the super-tenant admin), mirroring {@code KeyManagerAdminSteps}' OAuthAdminService call.</li>
 *   <li><b>Admin workflow REST.</b> List the pending tasks of a workflow type
 *       ({@code GET api/am/admin/v4/workflows?workflowType=...}), capture a task's {@code referenceId} by
 *       matching a property (applicationName / apiName / tenantAwareUserName — the legacy loop), get one by
 *       reference, and approve/reject it
 *       ({@code POST api/am/admin/v4/workflows/update-workflow-status?workflowReferenceId=...}). All funnel
 *       through {@link Requests} on the acting actor's admin token.</li>
 * </ol>
 */
public class WorkflowAdminSteps {

    private static final Log logger = LogFactory.getLog(WorkflowAdminSteps.class);

    /** Governance-registry path of the workflow-extensions resource that selects the executors. */
    private static final String WF_EXTENSIONS_REGISTRY_PATH =
            "/_system/governance/apimgt/applicationdata/workflow-extensions.xml";
    /** SOAP namespace ResourceAdminService requires (the axis2 xsd namespace faults "namespace mismatch"). */
    private static final String RESOURCE_ADMIN_NS = "http://services.resource.registry.carbon.wso2.org";

    private final BaseSteps baseSteps = new BaseSteps();
    private final PublisherBaseSteps publisherSteps = new PublisherBaseSteps();
    private final OrganizationSteps organizationSteps = new OrganizationSteps();

    /**
     * Composite fixture for the subscription scenarios: creates an API from the given payload file, deploys it
     * and publishes it THROUGH the active Approval executors — approving the parked revision-deployment and
     * API-state workflows — then waits until it is available in the DevPortal store, storing the API id under
     * {@code apiIdKey}. Each subscription scenario owns its own API this way because a runner-scoped shared API
     * cannot survive the per-scenario {@code @cleanup} sweep (§5/§10). Not a {@code _setup_} step — it runs
     * inside a {@code @cleanup} scenario as an inline prerequisite, so the API it creates is swept with the rest.
     */
    @When("I publish API from {string} through the approval workflow as {string}")
    public void iPublishApiThroughApprovalWorkflow(String payloadFile, String apiIdKey)
            throws IOException, InterruptedException {
        baseSteps.putJsonPayloadFromFile(payloadFile, "<wfApiPayload>");
        publisherSteps.iCreateAnAPIWithPayloadAs("apis", "<wfApiPayload>", apiIdKey);
        String apiName = Utils.extractValueFromPayload(
                ((HttpResponse) TestContext.get("httpResponse")).getData(), "name").toString();
        TestContext.set("wfApiName", apiName);
        baseSteps.putJsonPayloadInContext("<wfRevisionPayload>", "{\"description\":\"Initial Revision\"}");
        publisherSteps.iCreateResourceRevision("apis", apiIdKey, "<wfRevisionPayload>");
        baseSteps.putJsonPayloadInContext("<wfDeployPayload>",
                "[{\"name\":\"{{gatewayEnvironment}}\",\"vhost\":\"localhost\",\"displayOnDevportal\":true}]");
        publisherSteps.iDeployApiRevisionGivenPayload("<revisionId>", "apis", apiIdKey, "<wfDeployPayload>");
        approveByProperty("AM_REVISION_DEPLOYMENT", "apiName", apiName);
        publisherSteps.iPublishTheResource("apis", apiIdKey);
        approveByProperty("AM_API_STATE", "apiName", apiName);
        organizationSteps.iRetrieveDevportalApiUntilContains(apiIdKey, "PUBLISHED", 60);
    }

    /** Captures the pending workflow of a type by a property match and approves it, asserting the 200. */
    private void approveByProperty(String workflowType, String property, String value) throws IOException {
        String reference = findPendingWorkflowReference(workflowType, property, value);
        Assert.assertNotNull(reference, "No pending '" + workflowType + "' workflow with " + property
                + "='" + value + "' to approve.");
        HttpResponse resp = Requests.post(Utils.getUpdateWorkflowStatusURL(Utils.getBaseUrl(), reference),
                Identity.adminHeaders(),
                new JSONObject().put("status", "APPROVED").put("description", "auto").toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertEquals(resp.getResponseCode(), 200, "Approving " + workflowType + " failed: " + resp.getData());
    }

    /**
     * The original {@code workflow-extensions.xml} content, captured before the Approval variant is written, so
     * the runner's {@code @AfterClass} can restore it. Static because the setup feature and the runner teardown
     * are different instances on the same runner; a null value means the setup never ran (nothing to restore).
     */
    private static volatile String originalWorkflowExtensions;

    /**
     * Enables the Approval workflow executors: reads the current {@code workflow-extensions.xml} from the registry
     * (stored for restore), then writes the Approval variant from the classpath resource. Runs as the acting
     * actor (super-tenant admin). This is a {@code _setup_} step — it enables behaviour the scenarios then test.
     *
     * @param resourcePath classpath path of the Approval {@code workflow-extensions.xml}
     */
    @When("I enable approval workflow executors from {string}")
    public void iEnableApprovalWorkflowExecutors(String resourcePath) throws IOException {
        originalWorkflowExtensions = getRegistryTextContent(WF_EXTENSIONS_REGISTRY_PATH);
        Assert.assertNotNull(originalWorkflowExtensions,
                "Could not read the current workflow-extensions.xml from the registry — cannot safely flip "
                        + "executors without a captured original to restore.");
        String approvalXml = Utils.readClasspathResource(resourcePath);
        boolean written = updateRegistryTextContent(WF_EXTENSIONS_REGISTRY_PATH, approvalXml);
        Assert.assertTrue(written, "ResourceAdminService.updateTextContent did not return true for the "
                + "workflow-extensions.xml write — the executor flip did not take effect.");
    }

    /**
     * Restores the workflow executors to whatever content was captured before the flip. Best-effort and
     * idempotent: called from the runner's {@code @AfterClass} so the shared registry is not left mutated for
     * a later run against the same DB. A no-op if the setup step never captured an original.
     */
    public static void restoreWorkflowExecutors() {
        String original = originalWorkflowExtensions;
        if (original == null) {
            return;
        }
        try {
            User admin = Identity.resolveActor("admin");
            String envelope = updateEnvelope(WF_EXTENSIONS_REGISTRY_PATH, original);
            Requests.soap(Utils.getResourceAdminServiceURL(Utils.getBaseUrl()), envelope,
                    "urn:updateTextContent", admin.getUserName(), admin.getPassword());
        } catch (IOException | RuntimeException e) {
            // A restore failure must not mask the run result; log and move on (the container is torn down anyway,
            // and the next run's setup re-reads whatever is there and overwrites it). IOException covers the SOAP
            // transport; RuntimeException covers an unresolvable actor ref.
            logger.warn("WorkflowAdminSteps: failed to restore workflow-extensions.xml: " + e.getMessage());
        } finally {
            originalWorkflowExtensions = null;
        }
    }

    /**
     * Lists the pending workflow tasks of the given type as admin and captures the {@code referenceId} of the
     * task whose {@code properties.<matchProperty>} equals the resolved {@code matchValue}, storing it under
     * {@code refKey}. Mirrors the legacy property-match loop (applicationName / apiName / tenantAwareUserName).
     * The list call's response is published so a following {@code Then The response status code should be 200}
     * can assert it; the captured reference is asserted non-null here so a missing task fails fast with a clear
     * message rather than an opaque downstream NPE.
     *
     * @param workflowType  workflow type filter (e.g. {@code AM_APPLICATION_CREATION})
     * @param matchProperty property name inside each task's {@code properties} object to match on
     * @param matchValue    context-resolvable expected value of that property
     * @param refKey        context key to store the matched task's referenceId under
     */
    @When("I capture the pending {string} workflow reference where {string} is {string} as {string}")
    public void iCaptureThePendingWorkflowReference(String workflowType, String matchProperty, String matchValue,
                                                    String refKey) throws IOException {
        String expected = Utils.resolveContextPlaceholders(matchValue);
        String reference = findPendingWorkflowReference(workflowType, matchProperty, expected);
        Assert.assertNotNull(reference, "No pending '" + workflowType + "' workflow found with " + matchProperty
                + "='" + expected + "'.");
        TestContext.set(Utils.normalizeContextKey(refKey), reference);
    }

    /**
     * Lists the pending workflow tasks of the given type as admin and publishes the response for assertion (the
     * positive list, expect 200). The non-admin negative uses
     * {@link #iAttemptToListPendingWorkflowsAsNonAdmin} instead, which sends a token the non-admin actor holds.
     */
    @When("I list pending {string} workflows")
    public void iListPendingWorkflows(String workflowType) throws IOException {
        Requests.get(Utils.getWorkflowsByTypeURL(Utils.getBaseUrl(), workflowType), Identity.adminHeaders());
    }

    /**
     * Attempts to list pending workflows of a type with the acting actor's DEVPORTAL token (a non-admin
     * credential) and publishes the response — the admin-only guard negative. The management API rejects a
     * token that lacks {@code apim:admin} with 401 ("Unauthenticated request", §12). Distinct from
     * {@link #iListPendingWorkflows} (which uses the admin token) so the negative sends a token the non-admin
     * actor actually holds, rather than trying to resolve an admin token the actor never minted.
     */
    @When("I attempt to list pending {string} workflows as a non-admin")
    public void iAttemptToListPendingWorkflowsAsNonAdmin(String workflowType) throws IOException {
        Requests.get(Utils.getWorkflowsByTypeURL(Utils.getBaseUrl(), workflowType),
                Identity.devportalHeaders());
    }

    /** Gets a single workflow task by its external reference and publishes the response for assertion. */
    @When("I get the workflow with reference {string}")
    public void iGetTheWorkflowByReference(String refKey) throws IOException {
        String reference = TestContext.resolve(refKey).toString();
        Requests.get(Utils.getWorkflowByReferenceURL(Utils.getBaseUrl(), reference), Identity.adminHeaders());
    }

    /**
     * Approves or rejects a captured pending workflow by reference and publishes the response for assertion.
     * {@code decision} is {@code APPROVED} or {@code REJECTED}. Uses the acting actor's admin token.
     */
    @When("I {string} the workflow with reference {string}")
    public void iDecideTheWorkflow(String decision, String refKey) throws IOException {
        String reference = TestContext.resolve(refKey).toString();
        String body = new JSONObject().put("status", decision)
                .put("description", "integration-v2 approval-workflow test").toString();
        Requests.post(Utils.getUpdateWorkflowStatusURL(Utils.getBaseUrl(), reference), Identity.adminHeaders(),
                body, Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    /**
     * Asserts a field of the last response's {@code properties} object equals an expected (context-resolvable)
     * value. Used to check the surfaced {@code applicationName}/{@code apiName} and the custom
     * {@code applicationAttributes} on a pending task.
     */
    @Then("The workflow property {string} should be {string}")
    public void theWorkflowPropertyShouldBe(String property, String expectedValue) {
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response != null && response.getResponseCode() >= 200
                        && response.getResponseCode() < 300 && response.getData() != null
                        && !response.getData().isBlank(),
                "Cannot read workflow properties — last response was not a successful body: got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));
        String expected = Utils.resolveContextPlaceholders(expectedValue);
        JSONObject properties = new JSONObject(response.getData()).getJSONObject("properties");
        Assert.assertEquals(properties.optString(property), expected,
                "Workflow property '" + property + "' mismatch. Full properties: " + properties);
    }

    /**
     * Asserts a key inside the pending task's {@code applicationAttributes} (a JSON string nested under
     * {@code properties}) equals an expected value — the custom attributes surfaced by
     * {@code applicationAttributesVisibility=true}.
     */
    @Then("The workflow application attribute {string} should be {string}")
    public void theWorkflowApplicationAttributeShouldBe(String attributeName, String expectedValue) {
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response != null && response.getResponseCode() >= 200
                        && response.getResponseCode() < 300 && response.getData() != null
                        && !response.getData().isBlank(),
                "Cannot read applicationAttributes — last response was not a successful body: got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));
        JSONObject properties = new JSONObject(response.getData()).getJSONObject("properties");
        Assert.assertTrue(properties.has("applicationAttributes"),
                "Pending workflow carried no applicationAttributes (is applicationAttributesVisibility enabled?): "
                        + properties);
        JSONObject attributes = new JSONObject(properties.getString("applicationAttributes"));
        Assert.assertEquals(attributes.optString(attributeName), expectedValue,
                "Application attribute '" + attributeName + "' mismatch. Full attributes: " + attributes);
    }

    /**
     * Asserts the length of the {@code list} array in the last response equals an expected count. Used by the
     * revision-deployment scenario to confirm a held/rejected deploy left zero deployed revisions and an
     * approved one left exactly one — the effect an approve/reject has on the deployment, which a plain
     * substring "contains" check cannot express.
     */
    @Then("The response list should have {int} entries")
    public void theResponseListShouldHaveEntries(int expectedCount) {
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response != null && response.getResponseCode() >= 200
                        && response.getResponseCode() < 300 && response.getData() != null
                        && !response.getData().isBlank(),
                "Cannot count list entries — last response was not a successful body: got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));
        JSONArray list = new JSONObject(response.getData()).optJSONArray("list");
        int actual = list == null ? 0 : list.length();
        Assert.assertEquals(actual, expectedCount,
                "Expected " + expectedCount + " list entries but found " + actual + ": " + response.getData());
    }

    /**
     * Subscribes an application to an API, retrying the transient {@code 900967} (HTTP 500) the subscribe
     * access-check returns while the target API's gateway artifact is still settling right after publish — the
     * plain subscribe step's short built-in retry is not always enough for a just-published shared API under an
     * approval-workflow block's create/approve load. Retries until 201 within the shared propagation window and
     * publishes the 201 so the following {@code Then ... 201} and the subscription-status retrieve see it. Stores
     * the created subscription id under {@code subKey}. Uses the acting actor's devportal token.
     *
     * @param apiIdKey  context key holding the API id
     * @param appIdKey  context key holding the application id
     * @param subKey    context key to store the created subscriptionId under
     */
    @When("I subscribe application {string} to API {string} retrying transient errors as {string}")
    public void iSubscribeWithRetry(String appIdKey, String apiIdKey, String subKey)
            throws IOException, InterruptedException {
        String apiId = TestContext.resolve(apiIdKey).toString();
        String appId = TestContext.resolve(appIdKey).toString();
        String payload = new JSONObject().put("applicationId", appId).put("apiId", apiId)
                .put("throttlingPolicy", "Unlimited").toString();
        Map<String, String> headers = Identity.devportalHeaders();
        String url = Utils.getCreateSubscriptionURL(Utils.getBaseUrl());
        // retryUntil retries only IOException; the transient here is an HTTP 500 body, so the accept predicate
        // drives the retry (accept only 201) and the raw client is used for the attempt so a 500 is a returned
        // response, not a thrown assertion.
        HttpResponse last = Utils.retryUntil(0L,
                () -> SimpleHTTPClient.getInstance().doPost(url, headers, payload,
                        Constants.CONTENT_TYPES.APPLICATION_JSON),
                resp -> resp != null && resp.getResponseCode() == 201);
        TestContext.set("httpResponse", last);
        Assert.assertNotNull(last, "Subscribe never returned a response for app " + appId + " -> api " + apiId);
        Assert.assertEquals(last.getResponseCode(), 201,
                "Subscribe did not reach 201 within the window: got=" + last.getResponseCode() + "/"
                        + last.getData());
        Object subId = Utils.extractValueFromPayload(last.getData(), "subscriptionId");
        TestContext.set(Utils.normalizeContextKey(subKey), subId);
    }

    /**
     * Generates production keys for an application under the Approval registration executor and publishes the
     * response WITHOUT extracting the consumer key/secret — under approval the key is issued in state CREATED
     * with no consumerKey yet, so the generic key-gen step's unconditional {@code consumerKey} extraction NPEs
     * on the null value. The feature asserts the {@code CREATED} keyState on the published response.
     */
    @When("I generate pending production keys for application {string}")
    public void iGeneratePendingProductionKeys(String appIdKey) throws IOException {
        String appId = TestContext.resolve(appIdKey).toString();
        String payload = new JSONObject().put("keyType", "PRODUCTION")
                .put("grantTypesToBeSupported", new JSONArray().put("client_credentials").put("password"))
                .put("validityTime", 3600).toString();
        Requests.post(Utils.getGenerateApplicationKeysURL(Utils.getBaseUrl(), appId), Identity.devportalHeaders(),
                payload, Constants.CONTENT_TYPES.APPLICATION_JSON);
    }

    // --------------------------------------------------------------------------------------------------------
    // Internals
    // --------------------------------------------------------------------------------------------------------

    /** Fetches the {@code list} of pending tasks of a type and returns the referenceId of the property match. */
    private String findPendingWorkflowReference(String workflowType, String matchProperty, String expected)
            throws IOException {
        HttpResponse response = Requests.get(Utils.getWorkflowsByTypeURL(Utils.getBaseUrl(), workflowType),
                Identity.adminHeaders());
        Assert.assertTrue(response != null && response.getResponseCode() == 200 && response.getData() != null
                        && !response.getData().isBlank(),
                "Listing '" + workflowType + "' workflows did not return a 200 body: got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));
        JSONArray list = new JSONObject(response.getData()).optJSONArray("list");
        if (list == null) {
            return null;
        }
        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.getJSONObject(i);
            JSONObject properties = item.optJSONObject("properties");
            if (properties != null && expected.equals(properties.optString(matchProperty))) {
                return item.optString("referenceId", null);
            }
        }
        return null;
    }

    /** Reads a registry text resource via {@code ResourceAdminService.getTextContent} as the acting admin. */
    private String getRegistryTextContent(String path) throws IOException {
        User actor = Identity.actingActor();
        String envelope = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ser=\"" + RESOURCE_ADMIN_NS + "\"><soapenv:Header/><soapenv:Body>"
                + "<ser:getTextContent><ser:path>" + Utils.escapeXml(path) + "</ser:path></ser:getTextContent>"
                + "</soapenv:Body></soapenv:Envelope>";
        HttpResponse response = Requests.soap(Utils.getResourceAdminServiceURL(Utils.getBaseUrl()), envelope,
                "urn:getTextContent", actor.getUserName(), actor.getPassword());
        Assert.assertTrue(response != null && response.getResponseCode() == 200 && response.getData() != null,
                "ResourceAdminService.getTextContent failed for " + path + ": got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));
        return extractSoapReturn(response.getData());
    }

    /** Writes a registry text resource via {@code ResourceAdminService.updateTextContent} as the acting admin. */
    private boolean updateRegistryTextContent(String path, String content) throws IOException {
        User actor = Identity.actingActor();
        String envelope = updateEnvelope(path, content);
        HttpResponse response = Requests.soap(Utils.getResourceAdminServiceURL(Utils.getBaseUrl()), envelope,
                "urn:updateTextContent", actor.getUserName(), actor.getPassword());
        Assert.assertTrue(response != null && response.getResponseCode() == 200 && response.getData() != null,
                "ResourceAdminService.updateTextContent failed for " + path + ": got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));
        return "true".equalsIgnoreCase(extractSoapReturn(response.getData()));
    }

    /**
     * Builds the {@code updateTextContent} SOAP envelope. The element names MUST be {@code resourcePath} and
     * {@code contentText} (the stub's serialized names) — {@code path} faults "Index 2 out of bounds"; and the
     * namespace MUST be the resource-registry one, not the axis2 xsd namespace.
     */
    private static String updateEnvelope(String path, String content) {
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ser=\"" + RESOURCE_ADMIN_NS + "\"><soapenv:Header/><soapenv:Body>"
                + "<ser:updateTextContent>"
                + "<ser:resourcePath>" + Utils.escapeXml(path) + "</ser:resourcePath>"
                + "<ser:contentText>" + Utils.escapeXml(content) + "</ser:contentText>"
                + "</ser:updateTextContent></soapenv:Body></soapenv:Envelope>";
    }

    /**
     * Extracts the text between {@code <ns:return>...</ns:return>} of a ResourceAdminService SOAP response and
     * XML-unescapes it. The response is an MTOM/XOP multipart, but the return element is inline text; a simple
     * substring extraction is sufficient (and avoids pulling in a SOAP parser for two operations).
     */
    private static String extractSoapReturn(String soapBody) {
        int start = soapBody.indexOf(":return>");
        if (start < 0) {
            return null;
        }
        start = soapBody.indexOf('>', start) + 1;
        int end = soapBody.indexOf("</", start);
        if (end < 0) {
            return null;
        }
        return unescapeXml(soapBody.substring(start, end));
    }

    private static String unescapeXml(String value) {
        return value.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&apos;", "'").replace("&amp;", "&");
    }
}
