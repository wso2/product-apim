@cleanup
Feature: External BPMN workflow - API state change start request

  The sibling workflow_api_state_change feature covers the IN-PRODUCT APIStateChange Approval executor. This one
  covers the EXTERNAL variant, APIStateChangeWSWorkflowExecutor: instead of parking a task for the admin UI, it
  POSTs a process-start request to a BPMN engine and only then holds the transition. Ports the start-request
  assertions of APIStateChangeWorkflowTestCase#testAPIStateChangeAndApproveWorkflow, which are what the in-product
  executor cannot exercise - the callback credentials (clientId, clientSecret, scope) the executor DCR-registers
  and hands to the engine so the engine can call back, and the businessKey it uses to correlate the process with
  the workflow entry.

  The engine is the BPMNProcessServerApp double on the shared docker network (nodebackend:3004). It is PASSIVE: it
  records the start request and replays it, and never posts a completion callback. That is enough here, because
  the assertion target is the request APIM SENDS; the approve/reject halves are covered against the in-product
  executor in workflow_api_state_change.

  THE FLIP IS BRACKETED BY ITS OWN RUNNER. Each row flips ITS OWN tenant's APIStateChange executor to the WS
  variant and does NOT flip it back mid-scenario - a per-scenario restore would not run on failure and would leave
  the block worse off. Instead this file is the sole feature of WorkflowWsApiStateChangeRunner, whose AfterClass
  restores the original executors of EVERY tenant it captured. It previously lived in WorkflowApprovalRunner and
  relied on "ws" sorting LAST among its workflow_* features; a rename or a new "workflow_x*"/"workflow_z*" sibling
  would have silently routed that sibling's publish requests to the BPMN double, so the ordering assumption was
  replaced by the runner boundary. Do NOT add another feature to that runner.

  BOTH TENANTS. The flip is a governance-registry write made as the ACTING actor, and that registry resource is
  tenant-scoped (the sibling _setup_workflow_executors already writes it per tenant, and the restore is keyed by
  tenant), so a tenant row flips only tenant1.com. Examples rows run sequentially and this file still runs last,
  so the "nothing runs afterwards" argument that makes the super-tenant row safe covers the tenant row unchanged.
  The BPMN double stores only the LAST start request, which is likewise safe sequentially: the apiName pinned
  below is the row's own unique name, so a row can never be satisfied by the previous row's record.

  @cap:admin @feat:workflows @dep:publisher @legacy:APIStateChangeWorkflowTestCase @type:regression
  Scenario Outline: The API state-change process start request carries the callback credentials and the workflow business key as <requester>
    Given The system is ready with an admin approver and "<requester>" as the requester

    # Route APIStateChange at the external executor for the requester's OWN tenant (the write is made as the
    # acting actor against that tenant's registry). It invalidates that tenant's workflow config cache (a registry
    # media-type handler does it), so the flip takes effect on the next transition.
    When I enable approval workflow executors from "artifacts/configFiles/approveWorkflow/workflow-extensions-bpmn.xml"

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
    # Where the engine is told to call back - the admin update-workflow-status endpoint whose auth and
    # unknown-reference negatives workflow_api_state_change covers.
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
    When I extract response field "businessKey" and store it as "bpmnWfRef"
    And I get the workflow with reference "bpmnWfRef"
    Then The response status code should be 200
    And The value of response field "referenceId" should be "{{bpmnWfRef}}"
    And The value of response field "workflowType" should be "API_STATE"
    And The value of response field "workflowStatus" should be "CREATED"

    # Resolve the held task through the product's own cleanup path rather than leaving it pending: deleting the
    # API makes the WS executor delete the process instance at the engine and drop the workflow entry, so the
    # reference stops resolving. This also exercises the external cleanUpPendingTask contract end to end.
    When I undeploy revision "bpmnRevId" of "apis" resource "bpmnApiId"
    Then The response status code should be 201
    When I delete revision "bpmnRevId" of "apis" resource "bpmnApiId"
    Then The response status code should be 200
    When I delete the "apis" resource with id "bpmnApiId"
    Then The response status code should be 200
    When I get the workflow with reference "bpmnWfRef"
    Then The response status code should be 404

    Examples:
      | requester         | apiProvider          |
      | admin             | admin                |
      | admin@tenant1.com | admin-AT-tenant1.com |
