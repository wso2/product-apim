@cleanup
Feature: Approval workflow - API state change

  With the APIStateChange Approval executor active, a publish lifecycle-change request is accepted (the
  workflow is created) but the API stays in CREATED until an admin approves the AM_API_STATE task, after
  which it becomes PUBLISHED. Ports the API-state arc of
  WorkflowApprovalExecutorTest#testAPIWorkflowProcess.

  Runs across both the REQUESTER axis (admin vs publisher) and the tenant axis (super tenant vs tenant1.com),
  producing four rows. The tenant admin decides both parked workflows for each request.

  @cap:admin @feat:workflows @dep:publisher @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario Outline: API stays CREATED after a publish request until the state change is approved as requester <requester>
    Given The system is ready with an admin approver and "<requester>" as the requester

    # A fresh API, created + deployed under the (now active) Approval executors. Deployment itself parks a
    # revision-deployment workflow, so approve it before requesting publish.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "wfApi"
    And I create an "apis" resource with payload "wfApi" as "wfApiId"
    Then The response status code should be 201
    And I extract response field "name" and store it as "wfApiName"
    When I put the following JSON payload in context as "wfRevision"
    """
    {"description": "Initial Revision"}
    """
    And I make a request to create a revision for "apis" resource "wfApiId" with payload "wfRevision"
    Then The response status code should be 201
    When I put the following JSON payload in context as "wfDeploy"
    """
    [{"name": "{{gatewayEnvironment}}", "vhost": "localhost", "displayOnDevportal": true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "wfApiId" with payload "wfDeploy"
    Then The response status code should be 201
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_REVISION_DEPLOYMENT" workflow reference where "apiName" is "{{wfApiName}}" as "depWfRef"
    And I "APPROVED" the workflow with reference "depWfRef"
    Then The response status code should be 200

    # Request publish — accepted (200) but the API must remain CREATED pending approval.
    Given I act as "<requester>"
    When I publish the "apis" resource with id "wfApiId"
    Then The response status code should be 200
    And The lifecycle status of API "wfApiId" should be "Created"

    # Admin approves the API state-change workflow; the API becomes PUBLISHED.
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_API_STATE" workflow reference where "apiName" is "{{wfApiName}}" as "stateWfRef"
    And I "APPROVED" the workflow with reference "stateWfRef"
    Then The response status code should be 200
    Given I act as "<requester>"
    Then The lifecycle status of API "wfApiId" should be "Published"

    Examples:
      | requester     |
      | admin         |
      | admin@tenant1.com |
      | publisherUser |
      | publisherUser@tenant1.com |

  # Ports APIStateChangeWorkflowTestCase#testAPIStateChangeAndRejectWorkflow (the legacy class is commented out of
  # testng.xml, so this arc has never run). "Stays PUBLISHED" is ALSO true before the reject, so asserting the
  # state alone would prove nothing. The discriminating gate is the pair around it: the task must first be
  # RETRIEVABLE by its reference (200 - the Block request really parked), and after the reject the SAME reference
  # must be gone (404 - the admin get-by-reference resolves only CREATED tasks, so a rejected one no longer
  # answers). Only then does "still Published" mean "the rejected transition was not applied".
  # Two rows, one per tenant: the REQUESTER axis (admin vs non-admin) is already covered by the approve scenario
  # above against the very same executor and endpoint, so repeating it here would buy nothing.
  @cap:admin @feat:workflows @dep:publisher @legacy:APIStateChangeWorkflowTestCase @type:regression
  Scenario Outline: A rejected Block request leaves the API PUBLISHED as requester <requester>
    Given The system is ready with an admin approver and "<requester>" as the requester

    # A PUBLISHED API, taken there through the approval executors (deploy + publish both parked and approved).
    Given I act as the tenant admin for "<requester>"
    When I publish API from "artifacts/payloads/create_apim_test_api.json" through the approval workflow as "rejectApiId"
    Then The lifecycle status of API "rejectApiId" should be "Published"

    # Request Block — accepted (200) but Published:Block is in the executor's stateList, so it parks instead of
    # transitioning; the API must still read PUBLISHED.
    Given I act as "<requester>"
    When I change the lifecycle of API "rejectApiId" with action "Block"
    Then The response status code should be 200
    And The lifecycle status of API "rejectApiId" should be "Published"

    # The Block request really parked: the pending task exists and is retrievable by its reference.
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_API_STATE" workflow reference where "apiName" is "{{wfApiName}}" as "blockWfRef"
    And I get the workflow with reference "blockWfRef"
    Then The response status code should be 200

    # Reject it. The reject was PROCESSED - the same reference is no longer a pending task - and only then is
    # "the API is still PUBLISHED" evidence that the rejected transition was never applied.
    When I "REJECTED" the workflow with reference "blockWfRef"
    Then The response status code should be 200
    When I get the workflow with reference "blockWfRef"
    Then The response status code should be 404
    And There should be no pending "AM_API_STATE" workflow where "apiName" is "{{wfApiName}}"
    Given I act as "<requester>"
    Then The lifecycle status of API "rejectApiId" should be "Published"

    Examples:
      | requester         |
      | admin             |
      | admin@tenant1.com |

  # Ports the unknown-reference half of APIStateChangeWorkflowTestCase#testWorkflowCallbackRestAPI. The
  # wrong-scope half of that method is already covered against this same endpoint by
  # workflow_application_creation.feature's "A non-admin token cannot read or decide a pending workflow by its
  # external reference" scenario (measured 401), so only the unknown-reference case is added here.
  # The reference is a freshly minted UUID rather than a literal: it is guaranteed absent by construction and
  # cannot collide with a real task another scenario in this serialized block has parked.
  @cap:admin @feat:workflows @legacy:APIStateChangeWorkflowTestCase @type:negative
  Scenario Outline: The workflow callback rejects an unknown workflow reference as <adminActor>
    Given The system is ready
    And I have valid access tokens as "<adminActor>"
    When I generate a random UUID and store it as "unknownWfRef"
    And I "APPROVED" the workflow with reference "unknownWfRef"
    Then The response status code should be 404

    Examples:
      | adminActor        |
      | admin             |
      | admin@tenant1.com |
