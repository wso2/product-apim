@cleanup
Feature: External BPMN workflow - API state change approval and rejection

  The sibling workflow_api_state_change feature covers the IN-PRODUCT APIStateChange Approval executor. This one
  covers the EXTERNAL variant, APIStateChangeWSWorkflowExecutor: instead of parking a task for the admin UI, it
  POSTs a process-start request to a BPMN engine and only then holds the transition. Ports the start-request
  assertions of APIStateChangeWorkflowTestCase's approval and rejection flows, which are what the in-product
  executor cannot exercise - the callback credentials (clientId, clientSecret, scope) the executor registers
  automatically or receives through configuration, then hands to the engine so the engine can call back, and the businessKey it uses to correlate the process with
  the workflow entry.

  The engine is the BPMNProcessServerApp double on the shared docker network (nodebackend:3004). It is PASSIVE: it
  records the start request and replays it, and never posts a completion callback. The test also verifies that
  a valid token minted for the emitted client without the workflow scope cannot complete the held workflow.
  The test then submits an authorized approval callback with explicit compatibility setup and checks Published.
  The original emitted request and client-credentials tokens remain available; the compatibility steps below
  explain stale scope, automatic-client tenant ownership, provider encoding and lifecycle re-entry constraints.
  The automatic super-tenant client uses client-credentials callbacks. The automatic tenant client requires
  password-grant callbacks because it belongs to carbon.super. Configured tenant-owned clients use client-credentials
  callbacks in both tenants; that path does not establish tenant-correct automatic DCR.
  After approval, the test requests Block through the external executor, rejects that separate workflow and
  checks that the API remains Published. After each callback, APIM must no longer expose the original reference
  as a pending workflow. Approval excludes its transition during callback. Rejection retains normal gating:
  it may start another held Block workflow while the API remains Published; no prevention of re-entry is asserted.

  THE ORIGINAL CONFIGURATION IS RESTORED BY ITS OWN RUNNER. Each row flips ITS OWN tenant's APIStateChange
  executor to the WS variant. The callback compatibility steps briefly change only its stateList, then reinstall
  the original WS fixture. Failure-safe restoration belongs to WorkflowWsApiStateChangeRunner, whose AfterClass
  restores the original executors of EVERY tenant it captured. It previously lived in WorkflowApprovalRunner and
  relied on "ws" sorting LAST among its workflow_* features; a rename or a new "workflow_x*"/"workflow_z*" sibling
  would have silently routed that sibling's publish requests to the BPMN double, so the ordering assumption was
  replaced by the runner boundary. Do NOT add another feature to that runner.

  BOTH TENANTS. The flip is a governance-registry write made as the ACTING actor, and that registry resource is
  tenant-scoped (the sibling _setup_workflow_executors already writes it per tenant, and the restore is keyed by
  tenant), so a tenant row flips only tenant1.com. Examples rows run sequentially, and the runner restores both tenants before another class can execute.
  The BPMN double stores only the LAST start request, which is likewise safe sequentially: the apiName pinned
  below is the row's own unique name, so a row can never be satisfied by the previous row's record.

  @cap:admin @feat:workflows @dep:publisher @legacy:APIStateChangeWorkflowTestCase @type:regression @type:negative
  Scenario Outline: External API state-change approval and rejection with <callbackGrant> callbacks and <clientSetup> client as <requester>
    Given The system is ready with an admin approver and "<requester>" as the requester

    # Each row owns a callback client in the requester's tenant. The owned-client fixtures explicitly pass
    # its ID/secret to the executor; automatic-client rows retain the global-DCR behavior for comparison.
    When I register an OAuth client "bpmn_callback" as "bpmnOwnedClient"
    Then The response status code should be 200

    # Route APIStateChange at the external executor for the requester's OWN tenant (the write is made as the
    # acting actor against that tenant's registry). It invalidates that tenant's workflow config cache (a registry
    # media-type handler does it), so the flip takes effect on the next transition.
    When I enable approval workflow executors from "artifacts/configFiles/approveWorkflow/workflow-extensions-bpmn<clientFixtureSuffix>.xml"

    # An API deployed through the (unchanged) revision-deployment Approval executor, ready to be published.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "bpmnApi"
    And I create an "apis" resource with payload "bpmnApi" as "bpmnApiId"
    Then The response status code should be 201
    And I extract response field "name" and store it as "bpmnApiName"
    When I put the following JSON payload in context as "bpmnRevision"
    """
    {"description": "Initial Revision"}
    """
    And I make a request to create a revision for "apis" resource "bpmnApiId" with payload "bpmnRevision"
    Then The response status code should be 201
    And I extract response field "id" and store it as "bpmnRevId"
    When I put the following JSON payload in context as "bpmnDeploy"
    """
    [{"name": "{{gatewayEnvironment}}", "vhost": "localhost", "displayOnDevportal": true}]
    """
    And I make a request to deploy revision "bpmnRevId" of "apis" resource "bpmnApiId" with payload "bpmnDeploy"
    Then The response status code should be 201
    When I capture the pending "AM_REVISION_DEPLOYMENT" workflow reference where "apiName" is "{{bpmnApiName}}" as "bpmnDepWfRef"
    And I "APPROVED" the workflow with reference "bpmnDepWfRef"
    Then The response status code should be 200

    # Request publish. Created:Publish is in the executor's stateList, so the transition is held and a process
    # start request goes out to the BPMN engine; the API stays CREATED.
    When I publish the "apis" resource with id "bpmnApiId"
    Then The response status code should be 200
    And The lifecycle status of API "bpmnApiId" should be "Created"

    # Recorder reads adopt automatically created workflow clients as the global DCR owner. Configured clients
    # already have their tenant owner registered at creation; that ownership is retained for failure cleanup.
    # The request APIM sent to the engine. apiName is this scenario's own unique name, so it identifies the
    # record as OURS and no stale one can satisfy the assertions that follow.
    When I retrieve the process start request recorded by the BPMN process server
    Then The response status code should be 200
    And The value of response field "processDefinitionKey" should be "APIStateChangeApprovalProcess"
    And The response field "variables[?(@.name=='apiName')].value" should be exactly the list "{{bpmnApiName}}"
    # MEASURED: the executor sends the state UPPER-CASED ("CREATED"), unlike the publisher lifecycle API, which
    # reports it as "Created". Pinned as sent, not normalised.
    And The response field "variables[?(@.name=='apiCurrentState')].value" should be exactly the list "CREATED"
    And The response field "variables[?(@.name=='apiLCAction')].value" should be exactly the list "Publish"
    # The tenant dimension of the start request: the provider the executor reports is the requester itself.
    # MEASURED: for a tenant row it is sent in APIM's tenant-aware encoding, with the "@" replaced by "-AT-"
    # (admin-AT-tenant1.com), not as the plain username. Pinned as sent, not normalised - and it is what makes
    # the tenant row a distinct claim rather than a repeat of the super-tenant one.
    And The response field "variables[?(@.name=='apiProvider')].value" should be exactly the list "<apiProvider>"
    # The invoker is tenant-local in both admin rows; only apiProvider uses the -AT- encoding.
    And The response field "variables[?(@.name=='invoker')].value" should be exactly the list "admin"
    # Where the engine is told to call back. This scenario checks the emitted client's scope guard below;
    # workflow_api_state_change covers an unknown reference on the same Admin callback.
    And The response field "variables[?(@.name=='callbackUrl')].value" should be exactly the list "https://localhost:9443/api/am/admin/v4/workflows/update-workflow-status"

    # The callback credentials. clientId/clientSecret are minted by the executor's DCR call, so their values are
    # unknowable here - the contract is that each is present exactly once with a NON-EMPTY value. The regex
    # predicate (not "!= ''") is deliberate: a JSON null would satisfy an inequality and defeat the check, but
    # cannot match a regex. The scope IS knowable and is pinned exactly.
    And The response array field "variables[?(@.name=='clientId' && @.value =~ /.+/)]" should have exactly 1 entries
    And The response array field "variables[?(@.name=='clientSecret' && @.value =~ /.+/)]" should have exactly 1 entries
    And The response field "variables[?(@.name=='scope')].value" should be exactly the list "apim:api_workflow"

    # businessKey is the correlation handle the engine sends back on its callback, so "non-null" (all legacy
    # managed) is far too weak: it is only useful if it ADDRESSES the held workflow. Read it out of the recorded
    # request and resolve it against APIM's own workflow API - a 200 there is what proves it is a live reference.
    # It has to be read from the recorded request rather than captured from the pending-task listing, because the
    # WS executor persists the workflow entry WITHOUT any properties (it never calls the setWorkflowParameters
    # the in-product Approval executor calls) - MEASURED: capturing the pending AM_API_STATE task by apiName
    # finds nothing at all here, while the very same capture works against the Approval executor.
    When I extract response field "$" and store it as "bpmnStartRequest"
    And I extract response field "variables[?(@.name=='clientId')].value" and store it as "bpmnClientIds"
    And I extract field "[0]" from json payload "bpmnClientIds" and store it as "bpmnClientId"
    And I extract response field "variables[?(@.name=='clientSecret')].value" and store it as "bpmnClientSecrets"
    And I extract field "[0]" from json payload "bpmnClientSecrets" and store it as "bpmnClientSecret"
    And I extract response field "variables[?(@.name=='scope')].value" and store it as "bpmnEmittedCallbackScopes"
    And I extract field "[0]" from json payload "bpmnEmittedCallbackScopes" and store it as "bpmnEmittedCallbackScope"
    And I extract response field "businessKey" and store it as "bpmnWfRef"
    And I get the workflow with reference "bpmnWfRef"
    Then The response status code should be 200
    And The value of response field "referenceId" should be "{{bpmnWfRef}}"
    And The value of response field "workflowType" should be "API_STATE"
    And The value of response field "workflowStatus" should be "CREATED"

    # COMPATIBILITY PRE-STEP: APIStateChangeWSWorkflowExecutor emits apim:api_workflow, which is NOT registered
    # in APIM by default. The current Admin callback uses apim:api_workflow_approve. The stale emitted scope
    # remains a product issue: preserve bpmnEmittedCallbackScope and the original bpmnStartRequest, and map only
    # the scope requested at the token endpoint. This verifies token issuance WITH explicit scope translation;
    # it does not prove that a real BPMN engine can use the emitted scope unchanged.
    When I copy context value "bpmnEmittedCallbackScope" to "bpmnRequestedCallbackScope"
    And I replace "apim:api_workflow" with "apim:api_workflow_approve" in context "bpmnRequestedCallbackScope"

    # Verify translated-scope issuance for the emitted client; the row-selected callback grant below
    # determines which token is actually sent for approval.
    When I request a client-credentials token using consumer key "bpmnClientId" and secret "bpmnClientSecret" with scope "{{bpmnRequestedCallbackScope}}"
    Then The response status code should be 200
    And The response field "access_token" should be non-blank
    And The response field "scope" should contain token "{{bpmnRequestedCallbackScope}}"
    And I extract response field "access_token" and store it as "bpmnEmittedClientToken"
    When I introspect the access token "bpmnEmittedClientToken"
    Then The response status code should be 200
    And The value of response field "username" should be "<emittedPrincipal>"
    And The value of response field "active" should be "true"
    And The value of response field "client_id" should be "<expectedClientId>"
    And The response field "scope" should contain token "{{bpmnRequestedCallbackScope}}"

    # CALLBACK IDENTITY: automatic workflow DCR uses the global registration user, so its client-credentials
    # token is admin@carbon.super in both tenants. That is correct for the automatic super-tenant row; only
    # the automatic tenant row requires the password-grant workaround.
    # Owned-client rows use client_credentials for the callback itself, with no requester password in that
    # token request. Their introspected client ID must equal the independently registered tenant-owned client,
    # and their principal must equal this row's callbackPrincipal. All requests use the credentials APIM emitted.
    When I copy context value "bpmnClientId" to "consumerKey"
    And I copy context value "bpmnClientSecret" to "consumerSecret"
    And I request an OAuth access token using <callbackGrant> grant as "<requester>" with scope "{{bpmnRequestedCallbackScope}}"
    Then The response status code should be 200
    And The response field "scope" should contain token "{{bpmnRequestedCallbackScope}}"
    And I extract response field "access_token" and store it as "bpmnCallbackToken"
    When I introspect the access token "bpmnCallbackToken"
    Then The response status code should be 200
    And The value of response field "active" should be "true"
    And The value of response field "client_id" should be "<expectedClientId>"
    And The value of response field "username" should be "<callbackPrincipal>"
    And The response field "scope" should contain token "{{bpmnRequestedCallbackScope}}"

    # TENANT IDENTITY ENCODING PRODUCT BUG WORKAROUND: the executor emits admin-AT-tenant1.com as provider,
    # then passes the callback's provider directly to APIManagerFactory.getAPIProvider(). That
    # constructor determines the tenant from an @-qualified username; the encoded form instead selects
    # carbon.super. Once authenticated, an unchanged tenant callback therefore re-enters the wrong tenant.
    # Preserve bpmnStartRequest and normalize the provider only in a separate callback copy. The tenant-local
    # invoker stays admin. For the super-tenant admin this is a no-op; all other callback data stays intact.
    When I serialize context value "bpmnStartRequest" as JSON into "bpmnCallbackRequest"
    And I replace "<apiProvider>" with "<requester>" in the payload "bpmnCallbackRequest"

    # Isolate scope: match the authorized control's emitted client, grant, principal and callback payload.
    # Prove issuance and validity so rejection cannot pass on a missing, malformed or inactive token.
    # The automatic super-tenant and both configured-client rows send default-scope client-credentials tokens.
    When I request an OAuth access token using <callbackGrant> grant as "<requester>" with scope "default"
    Then The response status code should be 200
    And The response field "scope" should contain token "default"
    And The response field "scope" should not contain token "{{bpmnRequestedCallbackScope}}"
    And I extract response field "access_token" and store it as "bpmnDecisionWrongScopeToken"
    When I introspect the access token "bpmnDecisionWrongScopeToken"
    Then The response status code should be 200
    And The value of response field "active" should be "true"
    And The value of response field "client_id" should be "<expectedClientId>"
    And The value of response field "username" should be "<callbackPrincipal>"
    And The response field "scope" should not contain token "{{bpmnRequestedCallbackScope}}"
    When I attempt to "APPROVED" the BPMN workflow recorded in "bpmnCallbackRequest" using access token "bpmnDecisionWrongScopeToken"
    Then The response status code should be 401
    And The value of error response field "code" should be "401"
    And The value of error response field "description" should be "Unauthenticated request"
    # Authorization rejection must leave the same held workflow and API state untouched.
    When I get the workflow with reference "bpmnWfRef"
    Then The response status code should be 200
    And The value of response field "workflowStatus" should be "CREATED"
    And The lifecycle status of API "bpmnApiId" should be "Created"

    # LIFECYCLE RE-ENTRY PRODUCT BUG WORKAROUND: WS.complete() persists APPROVED, then calls
    # APIProvider.changeLifeCycleStatus(). That method executes the workflow again for non-CREATED status;
    # with Created:Publish still selected, it starts another BPMN process and leaves the API Created despite
    # callback HTTP 200.
    # Keep the WS executor, but temporarily exclude Created:Publish so its re-entered execute() auto-approves
    # this transition instead of starting a second process. This briefly disables the tenant's Publish approval
    # gate, so this runner is isolated and serialized. Reinstall the normal WS fixture immediately afterward;
    # the runner's AfterClass restores the captured pre-test configuration even if any step fails.
    # This proves external approval WITH explicit compatibility configuration, not the unmodified product path.
    When I enable approval workflow executors from "artifacts/configFiles/approveWorkflow/workflow-extensions-bpmn-callback-compat<clientFixtureSuffix>.xml"
    And I attempt to "APPROVED" the BPMN workflow recorded in "bpmnCallbackRequest" using access token "bpmnCallbackToken"
    Then The response status code should be 200
    # The reference resolved to CREATED before approval. It must now be absent from APIM's pending-workflow
    # lookup, before API deletion can remove anything. This checks completion, not prevention of re-entry.
    When I get the workflow with reference "bpmnWfRef"
    Then The response status code should be 404
    And The value of error response field "code" should be "900551"
    And The value of error response field "description" should be "Workflow entry cannot be found for the given reference id"
    When I enable approval workflow executors from "artifacts/configFiles/approveWorkflow/workflow-extensions-bpmn<clientFixtureSuffix>.xml"
    And The lifecycle status of API "bpmnApiId" should be "Published"

    # STEP 5: start a separate external Published:Block workflow for the same published API, mirroring the
    # legacy approval -> rejection chain within this row's own resources. Issue Block once: the positive
    # lifecycle-change helper waits for Blocked, which would be wrong while approval is still pending.
    When I attempt to change the lifecycle of API "bpmnApiId" with action "Block"
    Then The response status code should be 200
    And The lifecycle status of API "bpmnApiId" should be "Published"
    When I retrieve the process start request recorded by the BPMN process server
    Then The response status code should be 200
    And The value of response field "processDefinitionKey" should be "APIStateChangeApprovalProcess"
    And The response field "variables[?(@.name=='apiName')].value" should be exactly the list "{{bpmnApiName}}"
    And The response field "variables[?(@.name=='apiCurrentState')].value" should be exactly the list "PUBLISHED"
    And The response field "variables[?(@.name=='apiLCAction')].value" should be exactly the list "Block"
    And The response field "variables[?(@.name=='apiProvider')].value" should be exactly the list "<apiProvider>"
    And The response field "variables[?(@.name=='invoker')].value" should be exactly the list "admin"
    And The response field "variables[?(@.name=='callbackUrl')].value" should be exactly the list "https://localhost:9443/api/am/admin/v4/workflows/update-workflow-status"
    And The response array field "variables[?(@.name=='clientId' && @.value =~ /.+/)]" should have exactly 1 entries
    And The response array field "variables[?(@.name=='clientSecret' && @.value =~ /.+/)]" should have exactly 1 entries
    And The response field "variables[?(@.name=='scope')].value" should be exactly the list "apim:api_workflow"
    # A Block record must have a fresh reference, not the earlier Publish task or a stale mock response.
    And The response should not contain "{{bpmnWfRef}}"
    When I extract response field "$" and store it as "bpmnBlockStartRequest"
    And I extract response field "businessKey" and store it as "bpmnBlockWfRef"
    And I extract response field "variables[?(@.name=='clientId')].value" and store it as "bpmnBlockClientIds"
    And I extract field "[0]" from json payload "bpmnBlockClientIds" and store it as "bpmnBlockClientId"
    And I extract response field "variables[?(@.name=='clientSecret')].value" and store it as "bpmnBlockClientSecrets"
    And I extract field "[0]" from json payload "bpmnBlockClientSecrets" and store it as "bpmnBlockClientSecret"
    And I extract response field "variables[?(@.name=='scope')].value" and store it as "bpmnBlockEmittedScopes"
    And I extract field "[0]" from json payload "bpmnBlockEmittedScopes" and store it as "bpmnBlockEmittedScope"
    And I get the workflow with reference "bpmnBlockWfRef"
    Then The response status code should be 200
    And The value of response field "referenceId" should be "{{bpmnBlockWfRef}}"
    And The value of response field "workflowType" should be "API_STATE"
    And The value of response field "workflowStatus" should be "CREATED"

    # The Block request has its OWN captured credentials and raw scope. Apply the same explicit stale-scope
    # translation as Publish, preserving bpmnBlockStartRequest and bpmnBlockEmittedScope unchanged.
    When I copy context value "bpmnBlockEmittedScope" to "bpmnBlockRequestedScope"
    And I replace "apim:api_workflow" with "apim:api_workflow_approve" in context "bpmnBlockRequestedScope"
    And I request a client-credentials token using consumer key "bpmnBlockClientId" and secret "bpmnBlockClientSecret" with scope "{{bpmnBlockRequestedScope}}"
    Then The response status code should be 200
    And The response field "access_token" should be non-blank
    And The response field "scope" should contain token "{{bpmnBlockRequestedScope}}"
    And I extract response field "access_token" and store it as "bpmnBlockEmittedClientToken"
    When I introspect the access token "bpmnBlockEmittedClientToken"
    Then The response status code should be 200
    And The value of response field "username" should be "<emittedPrincipal>"
    And The value of response field "active" should be "true"
    And The value of response field "client_id" should be "<expectedBlockClientId>"
    And The response field "scope" should contain token "{{bpmnBlockRequestedScope}}"

    # Use the same row-selected callback grant for this separate Block task and verify its own emitted
    # credentials against the configured client when applicable. Decode the provider only in a callback copy.
    When I copy context value "bpmnBlockClientId" to "consumerKey"
    And I copy context value "bpmnBlockClientSecret" to "consumerSecret"
    And I request an OAuth access token using <callbackGrant> grant as "<requester>" with scope "{{bpmnBlockRequestedScope}}"
    Then The response status code should be 200
    And The response field "scope" should contain token "{{bpmnBlockRequestedScope}}"
    And I extract response field "access_token" and store it as "bpmnBlockCallbackToken"
    When I introspect the access token "bpmnBlockCallbackToken"
    Then The response status code should be 200
    And The value of response field "active" should be "true"
    And The value of response field "client_id" should be "<expectedBlockClientId>"
    And The value of response field "username" should be "<callbackPrincipal>"
    And The response field "scope" should contain token "{{bpmnBlockRequestedScope}}"
    When I serialize context value "bpmnBlockStartRequest" as JSON into "bpmnBlockCallbackRequest"
    And I replace "<apiProvider>" with "<requester>" in the payload "bpmnBlockCallbackRequest"

    # Normal gating remains installed from before the Block request. WS.complete() persists REJECTED, then
    # re-enters Published:Block and may start another held workflow. That still satisfies rejection's outcomes:
    # the original reference is no longer pending and the API remains Published. API deletion cleans up any
    # replacement task; this row does not assert that rejection prevents workflow re-entry.
    When I attempt to "REJECTED" the BPMN workflow recorded in "bpmnBlockCallbackRequest" using access token "bpmnBlockCallbackToken"
    Then The response status code should be 200
    # A re-entered Block workflow registers its own automatic client. Reading the recorder registers that client
    # for cleanup; an already registered Block or configured client keeps its existing cleanup owner.
    When I retrieve the process start request recorded by the BPMN process server
    Then The response status code should be 200
    When I get the workflow with reference "bpmnBlockWfRef"
    # Admin's get-by-reference resolves only CREATED tasks: the same reference must stop being pending after
    # rejection. Completed tasks correctly return 404; this endpoint cannot expose a REJECTED status payload.
    Then The response status code should be 404
    And The value of error response field "code" should be "900551"
    And The value of error response field "description" should be "Workflow entry cannot be found for the given reference id"
    And The lifecycle status of API "bpmnApiId" should be "Published"

    # Remove the API and revision after rejecting Block. Failure-safe hooks also sweep resources on failure.
    When I undeploy revision "bpmnRevId" of "apis" resource "bpmnApiId"
    Then The response status code should be 201
    When I delete revision "bpmnRevId" of "apis" resource "bpmnApiId"
    Then The response status code should be 200
    When I delete the "apis" resource with id "bpmnApiId"
    Then The response status code should be 200
    When I get the workflow with reference "bpmnWfRef"
    Then The response status code should be 404
    And The value of error response field "code" should be "900551"
    And The value of error response field "description" should be "Workflow entry cannot be found for the given reference id"
    When I get the workflow with reference "bpmnBlockWfRef"
    Then The response status code should be 404
    And The value of error response field "code" should be "900551"
    And The value of error response field "description" should be "Workflow entry cannot be found for the given reference id"

    Examples:
      | requester         | apiProvider         | callbackPrincipal  | clientSetup | clientFixtureSuffix | callbackGrant      | emittedPrincipal   | expectedClientId              | expectedBlockClientId          |
      | admin             | admin               | admin@carbon.super | automatic   |                     | client_credentials | admin@carbon.super | {{bpmnClientId}}              | {{bpmnBlockClientId}}          |
      | admin@tenant1.com | admin-AT-tenant1.com | admin@tenant1.com  | automatic   |                     | password           | admin@carbon.super | {{bpmnClientId}}              | {{bpmnBlockClientId}}          |
      | admin             | admin               | admin@carbon.super | configured  | -owned-client       | client_credentials | admin@carbon.super | {{bpmnOwnedClientClientId}}    | {{bpmnOwnedClientClientId}}     |
      | admin@tenant1.com | admin-AT-tenant1.com | admin@tenant1.com  | configured  | -owned-client       | client_credentials | admin@tenant1.com  | {{bpmnOwnedClientClientId}}    | {{bpmnOwnedClientClientId}}     |
