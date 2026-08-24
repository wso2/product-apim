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

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.wso2.am.integration.cucumbertests.utils.Identity;
import org.wso2.am.integration.cucumbertests.utils.Requests;
import org.wso2.am.integration.cucumbertests.utils.ResourceCleanup;
import org.wso2.am.integration.cucumbertests.utils.TestContext;
import org.wso2.am.integration.cucumbertests.utils.Utils;
import org.wso2.am.integration.cucumbertests.utils.clients.SimpleHTTPClient;
import org.wso2.am.integration.test.utils.Constants;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

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

    /**
     * Container port of the {@code BPMNProcessServerApp-1.0.0} node double (published to the host — see
     * {@code NodeAppServer}), which records the process-start request the external
     * {@code APIStateChangeWSWorkflowExecutor} POSTs and replays it on {@code ?debugInfo=startRequest}.
     */
    private static final int BPMN_PROCESS_SERVER_PORT = 3004;

    /** Governance-registry path of the workflow-extensions resource that selects the executors. */
    private static final String WF_EXTENSIONS_REGISTRY_PATH =
            "/_system/governance/apimgt/applicationdata/workflow-extensions.xml";
    /** SOAP namespace ResourceAdminService requires (the axis2 xsd namespace faults "namespace mismatch"). */
    private static final String RESOURCE_ADMIN_NS = "http://services.resource.registry.carbon.wso2.org";

    private final BaseSteps baseSteps = new BaseSteps();
    private final PublisherBaseSteps publisherSteps = new PublisherBaseSteps();
    private final OrganizationSteps organizationSteps = new OrganizationSteps();

    /**
     * Auth composite for the approval-workflow suite's TWO-ACTOR topology: an admin APPROVER plus a (possibly
     * non-admin) REQUESTER. Every approval flow has both — the requester performs the product action that parks
     * a pending task, the approver decides it — and legacy {@code WorkflowApprovalExecutorTest} runs each flow
     * twice over exactly this axis ({@code SUPER_TENANT_ADMIN} = requester is the admin itself,
     * {@code SUPER_TENANT_USER} = requester is a non-admin while {@code restAPIAdmin} stays the approver).
     *
     * <p>Neither existing composite in {@link BaseSteps} can express it: each records ONE acting actor and mints
     * that actor's tokens only, whereas here the admin's {@code apim:admin} token must stay available for the
     * workflow-admin steps while the scenario acts as the requester. So this delegates to the existing
     * composites (no token logic is duplicated) and leaves the REQUESTER acting — scenarios flip to the approver
     * with {@code Given I act as the tenant admin for "<requester>"} around the capture/approve steps, which is what makes the actor
     * hand-off visible in the feature rather than hidden in glue.
     *
     * @param requesterRef actor reference performing the workflow-triggering action ({@code admin},
     *                     {@code publisherUser} for publisher-plane flows, {@code subscriberUser} for
     *                     devportal-plane flows)
     */
    @Given("The system is ready with an admin approver and {string} as the requester")
    public void systemIsReadyWithApproverAndRequester(String requesterRef) throws Exception {
        baseSteps.theSystemIsReady();
        // The approver is the admin of the requester's tenant.
        baseSteps.iHaveTokensAs(adminActorRef(requesterRef));
        // The requester: DCR + publisher + devportal tokens (NO admin token — a non-admin is denied apim:admin).
        // Also re-records the acting actor as the requester, which is where the scenario starts.
        baseSteps.iHavePublisherTokensAs(requesterRef);
    }

    /** Selects the admin belonging to the referenced actor's tenant. */
    @Given("I act as the tenant admin for {string}")
    public void iActAsTenantAdminFor(String actorRef) {
        Identity.setActingActor(adminActorRef(actorRef));
    }

    private static String adminActorRef(String actorRef) {
        String tenant = Identity.resolveActor(actorRef).getUserDomain();
        return Constants.SUPER_TENANT_DOMAIN.equals(tenant) ? Constants.ADMIN_USER_KEY : "admin@" + tenant;
    }

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
        User approver = Identity.tenantOf(Identity.actingActor()).getTenantAdmin();
        Map<String, String> approverHeaders = Identity.bearerHeaders(Identity.adminToken(approver));
        baseSteps.putJsonPayloadFromFile(payloadFile, "<wfApiPayload>");
        publisherSteps.iCreateAnAPIWithPayloadAs("apis", "<wfApiPayload>", apiIdKey);
        HttpResponse createResponse = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertNotNull(createResponse, "Create API produced no response");
        Assert.assertTrue(createResponse.getResponseCode() >= 200 && createResponse.getResponseCode() < 300,
                "Create API failed: " + createResponse.getResponseCode() + "/" + createResponse.getData());
        Assert.assertNotNull(createResponse.getData(), "Create API response had no body");
        Object apiNameValue = Utils.extractValueFromPayload(createResponse.getData(), "name");
        Assert.assertNotNull(apiNameValue, "Create API response had no name: " + createResponse.getData());
        String apiName = apiNameValue.toString();
        TestContext.set("wfApiName", apiName);
        baseSteps.putJsonPayloadInContext("<wfRevisionPayload>", "{\"description\":\"Initial Revision\"}");
        publisherSteps.iCreateResourceRevision("apis", apiIdKey, "<wfRevisionPayload>");
        baseSteps.putJsonPayloadInContext("<wfDeployPayload>",
                "[{\"name\":\"{{gatewayEnvironment}}\",\"vhost\":\"localhost\",\"displayOnDevportal\":true}]");
        publisherSteps.iDeployApiRevisionGivenPayload("<revisionId>", "apis", apiIdKey, "<wfDeployPayload>");
        approveByProperty("AM_REVISION_DEPLOYMENT", "apiName", apiName, approverHeaders);
        publisherSteps.iPublishTheResource("apis", apiIdKey);
        approveByProperty("AM_API_STATE", "apiName", apiName, approverHeaders);
        organizationSteps.iRetrieveDevportalApiUntilContains(apiIdKey, "PUBLISHED", 60);
    }

    /** Captures the pending workflow of a type by a property match and approves it, asserting the 200. */
    private void approveByProperty(String workflowType, String property, String value,
                                   Map<String, String> approverHeaders) throws IOException {
        String reference = findPendingWorkflowReference(workflowType, property, value, approverHeaders);
        Assert.assertNotNull(reference, "No pending '" + workflowType + "' workflow with " + property
                + "='" + value + "' to approve.");
        HttpResponse resp = Requests.post(Utils.getUpdateWorkflowStatusURL(Utils.getBaseUrl(), reference),
                approverHeaders,
                new JSONObject().put("status", "APPROVED").put("description", "auto").toString(),
                Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertEquals(resp.getResponseCode(), 200, "Approving " + workflowType + " failed: " + resp.getData());
    }

    /**
     * The original {@code workflow-extensions.xml} content, captured before the Approval variant is written, so
     * the runner's {@code @AfterClass} can restore it. Static because the setup feature and the runner teardown
     * are different instances on the same runner; a null value means the setup never ran (nothing to restore).
     */
    private static final Map<String, Map<String, String>> originalWorkflowExtensions = new ConcurrentHashMap<>();

    /**
     * Enables the Approval workflow executors: reads the current {@code workflow-extensions.xml} from the registry
     * (stored for restore), then writes the Approval variant from the classpath resource. Runs as the acting
     * tenant admin. This is a {@code _setup_} step — it enables behaviour the scenarios then test.
     *
     * @param resourcePath classpath path of the Approval {@code workflow-extensions.xml}
     */
    @When("I enable approval workflow executors from {string}")
    public void iEnableApprovalWorkflowExecutors(String resourcePath) throws IOException {
        String tenant = Identity.actingTenantDomain();
        Map<String, String> originalsByTenant = originalWorkflowExtensions.computeIfAbsent(
                Utils.getBaseUrl(), ignored -> new ConcurrentHashMap<>());
        synchronized (originalsByTenant) {
            if (!originalsByTenant.containsKey(tenant)) {
                String original = getRegistryTextContent(WF_EXTENSIONS_REGISTRY_PATH);
                Assert.assertNotNull(original,
                        "Could not read the current workflow-extensions.xml from the registry — cannot safely flip "
                                + "executors without a captured original to restore for tenant " + tenant + ".");
                originalsByTenant.put(tenant, original);
            }
        }
        String approvalXml = Utils.readClasspathResource(resourcePath);
        boolean written = updateRegistryTextContent(WF_EXTENSIONS_REGISTRY_PATH, approvalXml);
        Assert.assertTrue(written, "ResourceAdminService.updateTextContent did not return true for the "
                + "workflow-extensions.xml write — the executor flip did not take effect.");
    }

    /**
     * Restores the workflow executors to the content captured before the flip, and VERIFIES the restore by reading
     * the registry back. Called from the runner's {@code @AfterClass}; a no-op if the setup step never captured an
     * original.
     *
     * <p>Self-verifying because {@code workflow-extensions.xml} is SERVER-GLOBAL: a silently failed restore leaves
     * the Approval executors active for every OTHER runner sharing this block's container, whose unrelated
     * application/subscription scenarios then hang {@code ON_HOLD} with nothing pointing back here. Scope, measured
     * rather than assumed: under the {@code default} profile the databases are in-container H2, so the state dies
     * with the container and does NOT reach a later run; under {@code migration}, which points at an external
     * MySQL, it does persist across runs. A discarded SOAP result is not enough to detect either — the service
     * answers 200 with a body of {@code false} — so the write's return is checked AND the content is re-read and
     * compared, mirroring {@link ResourceCleanup#deleteSignedUpUsers()}'s read-after-delete. Failure is logged as
     * an ERROR naming the tenant rather than thrown: this runs in teardown, where an exception would mask the
     * scenarios' own results.
     */
    public static void restoreWorkflowExecutors() {
        Map<String, String> originalsByTenant = originalWorkflowExtensions.remove(Utils.getBaseUrl());
        if (originalsByTenant == null) {
            return;
        }
        for (Map.Entry<String, String> entry : originalsByTenant.entrySet()) {
            String tenant = entry.getKey();
            String original = entry.getValue();
            try {
                String actorRef = Constants.SUPER_TENANT_DOMAIN.equals(tenant) ? Constants.ADMIN_USER_KEY
                        : "admin@" + tenant;
                User admin = Identity.resolveActor(actorRef);
                String envelope = updateEnvelope(WF_EXTENSIONS_REGISTRY_PATH, original);
                HttpResponse response = Requests.soap(Utils.getResourceAdminServiceURL(Utils.getBaseUrl()), envelope,
                        "urn:updateTextContent", admin.getUserName(), admin.getPassword());
                boolean written = response != null && response.getResponseCode() == 200 && response.getData() != null
                        && "true".equalsIgnoreCase(extractSoapReturn(response.getData()));
                String readBack = getRegistryTextContentAs(WF_EXTENSIONS_REGISTRY_PATH, admin);
                if (!written || !original.equals(readBack)) {
                    logger.error("WorkflowAdminSteps: FAILED to restore workflow-extensions.xml for tenant " + tenant
                            + " — updateTextContent returned " + written + " and the registry content "
                            + (original.equals(readBack) ? "matches" : "DOES NOT match") + " the captured original. "
                            + "This state is server-global, so the Approval executors may still be active for any "
                            + "other runner sharing this container — its application/subscription scenarios will "
                            + "hang ON_HOLD. On the default profile the DB is in-container H2 so it dies with the "
                            + "container; against an external DB (migration profile) it also poisons the next run.");
                }
            } catch (IOException | RuntimeException | AssertionError e) {
                logger.error("WorkflowAdminSteps: FAILED to restore workflow-extensions.xml for tenant " + tenant
                        + " (" + e.getMessage() + "). This state is server-global — the Approval executors may still "
                        + "be active for any other runner sharing this container.", e);
            }
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

    /**
     * Reads back the process-start request the external {@code APIStateChangeWSWorkflowExecutor} POSTed to the
     * BPMN process server, publishing it as {@code httpResponse} so the feature asserts its contents with the
     * generic response-field steps (the recorded body is plain JSON: {@code tenantId}, {@code processDefinitionKey},
     * {@code businessKey} and the {@code variables} name/value array).
     *
     * <p>A dedicated step because there is NO step in this module that issues a request to an arbitrary URL — every
     * request step targets a product endpoint through {@code Requests}/{@code execute}, and the node doubles whose
     * own state the test JVM reads back each get exactly one such reader (the WebSub receiver's delivery log, the
     * SSE emitter's stream diagnostics, the mock log sink). This is that reader for the BPMN double. It is HOST
     * addressed via {@link Utils#getNodeBackendUrl(int)}, unlike the executor's {@code serviceEndpoint}, which must
     * use the in-network {@code nodebackend} alias because the POST is made by the APIM container.
     *
     * <p>The double stores only the LAST start request, which is sound here because it is the only recorder of a
     * request no other feature or block triggers: the WS executor is selected by exactly one scenario, whose
     * Examples rows run sequentially and each pin their own unique apiName, so a row cannot read the previous
     * row's record.
     */
    @When("I retrieve the process start request recorded by the BPMN process server")
    public void iRetrieveTheRecordedBpmnStartRequest() throws IOException {
        Requests.get(Utils.getNodeBackendUrl(BPMN_PROCESS_SERVER_PORT)
                + "/runtime/process-instances?debugInfo=startRequest", Map.of());
    }

    /** Gets a single workflow task by its external reference and publishes the response for assertion. */
    @When("I get the workflow with reference {string}")
    public void iGetTheWorkflowByReference(String refKey) throws IOException {
        String reference = TestContext.resolve(refKey).toString();
        Requests.get(Utils.getWorkflowByReferenceURL(Utils.getBaseUrl(), reference), Identity.adminHeaders());
    }

    /**
     * Attempts the GET-workflow-BY-EXTERNAL-REFERENCE with the acting actor's DEVPORTAL token (a non-admin
     * credential) and publishes the response — the admin-only guard on the single-task read. A separate step from
     * {@link #iAttemptToListPendingWorkflowsAsNonAdmin} because it is a DIFFERENT endpoint
     * ({@code /workflows/{externalWorkflowRef}} vs {@code /workflows?workflowType=}); the legacy test asserts 401
     * on each independently, and a guard can regress on one without the other.
     */
    @When("I attempt to get the workflow with reference {string} as a non-admin")
    public void iAttemptToGetTheWorkflowByReferenceAsNonAdmin(String refKey) throws IOException {
        String reference = TestContext.resolve(refKey).toString();
        Requests.get(Utils.getWorkflowByReferenceURL(Utils.getBaseUrl(), reference), Identity.devportalHeaders());
    }

    /**
     * Attempts the update-workflow-status POST (approve/reject) with the acting actor's DEVPORTAL token and
     * publishes the response — the admin-only guard on the DECIDING endpoint, the one whose bypass would be an
     * actual privilege escalation (a non-admin self-approving its own pending request). Distinct from
     * {@link #iDecideTheWorkflow}, which sends the admin token.
     */
    @When("I attempt to {string} the workflow with reference {string} as a non-admin")
    public void iAttemptToDecideTheWorkflowAsNonAdmin(String decision, String refKey) throws IOException {
        String reference = TestContext.resolve(refKey).toString();
        String body = new JSONObject().put("status", decision)
                .put("description", "integration-v2 non-admin approval attempt").toString();
        Requests.post(Utils.getUpdateWorkflowStatusURL(Utils.getBaseUrl(), reference), Identity.devportalHeaders(),
                body, Constants.CONTENT_TYPES.APPLICATION_JSON);
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
        Assert.assertEquals(attributes.optString(attributeName), Utils.resolveContextPlaceholders(expectedValue),
                "Application attribute '" + attributeName + "' mismatch. Full attributes: " + attributes);
    }

    /**
     * Asserts the pending task's {@code updates} property (a JSON-ARRAY string nested under {@code properties})
     * carries an entry for {@code attributeName} whose {@code current} and {@code expected} values are exactly as
     * given. This is the WHAT-WILL-CHANGE payload an approver decides on — the subscription-update workflow's
     * {@code Subscription Tier: Unlimited -> Gold} and the application-update workflow's attribute deltas. It
     * cannot be expressed with {@link #theWorkflowPropertyShouldBe}, whose value is a flat string; here the
     * property must be parsed as an array and the matching entry located, and asserting only the end state (the
     * pre-existing check) would pass even if the approver were shown the wrong delta.
     *
     * @param attributeName  the {@code attributeName} of the entry to locate (e.g. {@code Subscription Tier})
     * @param currentValue   expected {@code current} value of that entry
     * @param expectedValue  expected {@code expected} (post-approval) value of that entry
     */
    @Then("The workflow update entry {string} should change from {string} to {string}")
    public void theWorkflowUpdateEntryShouldChange(String attributeName, String currentValue, String expectedValue) {
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response != null && response.getResponseCode() >= 200
                        && response.getResponseCode() < 300 && response.getData() != null
                        && !response.getData().isBlank(),
                "Cannot read the workflow 'updates' property — last response was not a successful body: got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));
        JSONObject properties = new JSONObject(response.getData()).getJSONObject("properties");
        Assert.assertTrue(properties.has("updates"),
                "Pending workflow carried no 'updates' property, so the approver is shown no delta at all: "
                        + properties);
        JSONArray updates = new JSONArray(properties.getString("updates"));
        JSONObject match = null;
        for (int i = 0; i < updates.length(); i++) {
            JSONObject entry = updates.getJSONObject(i);
            if (attributeName.equals(entry.optString("attributeName"))) {
                match = entry;
                break;
            }
        }
        Assert.assertNotNull(match, "No 'updates' entry with attributeName '" + attributeName + "'. Full updates: "
                + updates);
        Assert.assertEquals(match.optString("current"), Utils.resolveContextPlaceholders(currentValue),
                "'" + attributeName + "' current value mismatch in the updates entry: " + match);
        Assert.assertEquals(match.optString("expected"), Utils.resolveContextPlaceholders(expectedValue),
                "'" + attributeName + "' expected value mismatch in the updates entry: " + match);
    }

    /**
     * Asserts NO pending task of the given type carries a property matching the given value — the negative form of
     * {@link #iCaptureThePendingWorkflowReference}, needed where the product must have CLEANED UP an entry (the
     * revision-deployment workflow entry after an undeploy). A get-by-reference 404 cannot express this: the
     * reference of an entry that was never created is unknown, so the absence has to be asserted against the
     * listing. Retries the listing within the shared propagation window while an entry is still present, so a
     * lagging cleanup is waited out rather than flaking, and fails with the offending referenceId when it persists.
     *
     * @param workflowType  workflow type filter (e.g. {@code AM_REVISION_DEPLOYMENT})
     * @param matchProperty property name inside each task's {@code properties} object
     * @param matchValue    context-resolvable value that must match NO pending task
     */
    @Then("There should be no pending {string} workflow where {string} is {string}")
    public void thereShouldBeNoPendingWorkflow(String workflowType, String matchProperty, String matchValue)
            throws InterruptedException {
        String expected = Utils.resolveContextPlaceholders(matchValue);
        // The probe returns the BOOLEAN "is it gone", not the reference: retryUntil can never ACCEPT a null
        // result (its accept-then-return contract treats null as "not yet"), so probing for the reference
        // itself would burn the whole propagation window even on an immediate pass. The last reference seen is
        // carried out separately for the failure message.
        AtomicReference<String> lastSeen = new AtomicReference<>();
        Boolean absent = Utils.retryUntil(0L, () -> {
            String reference = findPendingWorkflowReference(workflowType, matchProperty, expected);
            lastSeen.set(reference);
            return reference == null;
        }, Boolean::booleanValue);
        Assert.assertTrue(Boolean.TRUE.equals(absent), "A pending '" + workflowType + "' workflow with "
                + matchProperty + "='" + expected + "' still exists (referenceId=" + lastSeen.get()
                + ") — it was not cleaned up.");
    }

    /**
     * Asserts the length of the {@code list} array in the last response equals an expected count. Used by the
     * revision-deployment scenario to confirm a held/rejected deploy left zero deployed revisions and an
     * approved one left exactly one — the effect an approve/reject has on the deployment, which a plain
     * substring "contains" check cannot express.
     *
     * <p>Sound for the deployed-revisions listing ONLY while no deploy request is outstanding: a revision whose
     * deployment is still pending approval is ALSO listed by {@code query=deployed:true}. Use
     * {@link #theOnlyApprovedDeployedRevisionShouldBe} once a request is parked.
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
     * Asserts that EXACTLY ONE revision in the last deployed-revisions listing has an {@code APPROVED}
     * deployment, and that it is the given revision.
     *
     * <p>Needed because {@code GET /revisions?query=deployed:true} also lists a revision whose deployment is
     * still PENDING an approval — measured behaviour: with revision 1 approved and revision 2's deploy request
     * parked, the listing returns BOTH, revision 2 carrying {@code deploymentInfo[0].status = CREATED}. So
     * {@code The response list should have 1 entries} cannot express "only revision 1 is actually deployed"
     * whenever a request is outstanding; the deployment STATUS is the discriminator. Asserting exactly-one
     * matters as much as the id: the legacy loop only checked ids of entries that happened to be APPROVED, so it
     * passed vacuously when none was.
     *
     * @param revisionIdKey context key holding the id of the revision that must be the sole APPROVED deployment
     */
    @Then("The only revision deployed with an APPROVED deployment should be {string}")
    public void theOnlyApprovedDeployedRevisionShouldBe(String revisionIdKey) {
        HttpResponse response = (HttpResponse) TestContext.get("httpResponse");
        Assert.assertTrue(response != null && response.getResponseCode() >= 200
                        && response.getResponseCode() < 300 && response.getData() != null
                        && !response.getData().isBlank(),
                "Cannot read the deployed-revisions listing — last response was not a successful body: got="
                        + (response == null ? "null" : response.getResponseCode() + "/" + response.getData()));
        String expectedRevisionId = TestContext.resolve(revisionIdKey).toString();
        JSONArray list = new JSONObject(response.getData()).optJSONArray("list");
        JSONArray approved = new JSONArray();
        for (int i = 0; list != null && i < list.length(); i++) {
            JSONObject revision = list.getJSONObject(i);
            JSONArray deployments = revision.optJSONArray("deploymentInfo");
            for (int d = 0; deployments != null && d < deployments.length(); d++) {
                if ("APPROVED".equals(deployments.getJSONObject(d).optString("status"))) {
                    approved.put(revision.optString("id"));
                    break;
                }
            }
        }
        Assert.assertEquals(approved.length(), 1, "Expected exactly one revision with an APPROVED deployment but "
                + "found " + approved.length() + " (" + approved + "): " + response.getData());
        Assert.assertEquals(approved.getString(0), expectedRevisionId,
                "The APPROVED-deployed revision is not the expected one: " + response.getData());
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
        String listUrl = Utils.getAllSubscriptionsURL(Utils.getBaseUrl(), apiId, appId, null, null, null);
        // retryUntil retries only IOException; the transient here is an HTTP 500 body, so the accept predicate
        // drives the retry. Check first whether an earlier attempt already committed the non-idempotent POST.
        HttpResponse last = Utils.retryUntil(0L,
                () -> {
                    HttpResponse existing = SimpleHTTPClient.getInstance().doGet(listUrl, headers);
                    if (existing.getResponseCode() == 200 && existing.getData() != null) {
                        try {
                            JSONArray subscriptions = new JSONObject(existing.getData()).optJSONArray("list");
                            for (int i = 0; subscriptions != null && i < subscriptions.length(); i++) {
                                JSONObject subscription = subscriptions.getJSONObject(i);
                                if (apiId.equals(subscription.optString("apiId"))
                                        && appId.equals(subscription.optString("applicationId"))) {
                                    return new HttpResponse(subscription.toString(), 201, existing.getHeaders());
                                }
                            }
                        } catch (JSONException e) {
                            return existing;
                        }
                    }
                    return SimpleHTTPClient.getInstance().doPost(url, headers, payload,
                            Constants.CONTENT_TYPES.APPLICATION_JSON);
                },
                resp -> resp != null && resp.getResponseCode() == 201);
        TestContext.set("httpResponse", last);
        Assert.assertNotNull(last, "Subscribe never returned a response for app " + appId + " -> api " + apiId);
        Assert.assertEquals(last.getResponseCode(), 201,
                "Subscribe did not reach 201 within the window: got=" + last.getResponseCode() + "/"
                        + last.getData());
        Assert.assertNotNull(last.getData(),
                "Subscribe response had no body for app " + appId + " -> api " + apiId);
        Object subId = Utils.extractValueFromPayload(last.getData(), "subscriptionId");
        Assert.assertNotNull(subId, "Subscribe response had no subscriptionId: " + last.getData());
        TestContext.set(Utils.normalizeContextKey(subKey), subId);
        ResourceCleanup.registerSubscription(subId, null);
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
        HttpResponse response = Requests.post(Utils.getGenerateApplicationKeysURL(Utils.getBaseUrl(), appId),
                Identity.devportalHeaders(), payload, Constants.CONTENT_TYPES.APPLICATION_JSON);
        Assert.assertNotNull(response, "Pending key generation produced no response for application " + appId);
        if (response.getResponseCode() >= 200 && response.getResponseCode() < 300) {
            Assert.assertNotNull(response.getData(), "Pending key-generation response had no body");
            Object keyMappingId = Utils.extractValueFromPayload(response.getData(), "keyMappingId");
            Assert.assertNotNull(keyMappingId,
                    "Pending key-generation response had no keyMappingId: " + response.getData());
            ResourceCleanup.registerApplicationKeyMapping(appId, keyMappingId);
        }
    }

    // --------------------------------------------------------------------------------------------------------
    // Internals
    // --------------------------------------------------------------------------------------------------------

    /** Fetches the {@code list} of pending tasks of a type and returns the referenceId of the property match. */
    private String findPendingWorkflowReference(String workflowType, String matchProperty, String expected)
            throws IOException {
        return findPendingWorkflowReference(workflowType, matchProperty, expected, Identity.adminHeaders());
    }

    private String findPendingWorkflowReference(String workflowType, String matchProperty, String expected,
                                                Map<String, String> adminHeaders) throws IOException {
        HttpResponse response = Requests.get(Utils.getWorkflowsByTypeURL(Utils.getBaseUrl(), workflowType),
                adminHeaders);
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
        return getRegistryTextContentAs(path, Identity.actingActor());
    }

    /**
     * Actor-explicit read, for callers that cannot rely on the acting actor — notably the static
     * {@link #restoreWorkflowExecutors()}, which runs from the runner's teardown and must read each tenant's copy
     * as that tenant's admin.
     */
    private static String getRegistryTextContentAs(String path, User actor) throws IOException {
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
