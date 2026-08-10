@cleanup
Feature: Approval workflow - API revision deployment

  With the APIRevisionDeployment Approval executor active, a deploy-revision request parks an
  AM_REVISION_DEPLOYMENT workflow and the revision is NOT deployed until an admin approves it; rejecting the
  request leaves it undeployed while approving deploys it. Covers the reject arc, the approve arc, the UNDEPLOY
  arc and the second-revision (replacement) arc. Ports
  WorkflowApprovalExecutorTest#testAPIRevisionDeploymentWorkflowProcess.

  Runs twice over the REQUESTER axis (the legacy SUPER_TENANT_ADMIN vs SUPER_TENANT_USER factory): the admin
  decides every parked workflow in both rows, while the actor that creates the API and requests each
  deploy/undeploy is the admin itself in one row and a least-privilege publisher user in the other.

  @cap:admin @feat:workflows @dep:publisher @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario Outline: Revision deployment is held until approved and a second revision replaces the first as requester <requester>
    Given The system is ready with an admin approver and "<requester>" as the requester

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
    # Capture revision 1's own id: the shared "revisionId" key is overwritten when revision 2 is created below.
    And I extract response field "id" and store it as "wfRev1Id"
    When I put the following JSON payload in context as "wfDeploy"
    """
    [{"name": "{{gatewayEnvironment}}", "vhost": "localhost", "displayOnDevportal": true}]
    """

    # First deploy request → parked. Reject it → the revision is not deployed.
    When I make a request to deploy revision "wfRev1Id" of "apis" resource "wfApiId" with payload "wfDeploy"
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_REVISION_DEPLOYMENT" workflow reference where "apiName" is "{{wfApiName}}" as "depWfRef"
    And I "REJECTED" the workflow with reference "depWfRef"
    Then The response status code should be 200
    Given I act as "<requester>"
    When I retrieve the deployed revisions of "apis" resource "wfApiId"
    Then The response status code should be 200
    And The response list should have 0 entries

    # Deploy again → approve it → revision 1 is deployed.
    When I make a request to deploy revision "wfRev1Id" of "apis" resource "wfApiId" with payload "wfDeploy"
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_REVISION_DEPLOYMENT" workflow reference where "apiName" is "{{wfApiName}}" as "depWfRef2"
    And I "APPROVED" the workflow with reference "depWfRef2"
    Then The response status code should be 200
    Given I act as "<requester>"
    When I retrieve the deployed revisions of "apis" resource "wfApiId"
    Then The response status code should be 200
    And The response list should have 1 entries
    And The response should contain "{{wfRev1Id}}"

    # Undeploy arc: an undeploy is NOT gated by the approval executor — it takes effect immediately (201) and
    # parks no AM_REVISION_DEPLOYMENT task of its own, leaving zero deployed revisions. An undeploy that DID
    # park a task would strand the revision deployed until an admin acted, which is the regression guarded here.
    When I undeploy revision "wfRev1Id" of "apis" resource "wfApiId"
    Then The response status code should be 201
    Given I act as the tenant admin for "<requester>"
    Then There should be no pending "AM_REVISION_DEPLOYMENT" workflow where "apiName" is "{{wfApiName}}"
    Given I act as "<requester>"
    When I retrieve the deployed revisions of "apis" resource "wfApiId"
    Then The response status code should be 200
    And The response list should have 0 entries

    # Re-deploy revision 1 (approved), then request a SECOND revision's deployment while revision 1 is live.
    When I make a request to deploy revision "wfRev1Id" of "apis" resource "wfApiId" with payload "wfDeploy"
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_REVISION_DEPLOYMENT" workflow reference where "apiName" is "{{wfApiName}}" as "depWfRef3"
    And I "APPROVED" the workflow with reference "depWfRef3"
    Then The response status code should be 200
    Given I act as "<requester>"
    When I put the following JSON payload in context as "wfRevision2"
    """
    {"description": "Second Revision"}
    """
    And I make a request to create a revision for "apis" resource "wfApiId" with payload "wfRevision2"
    Then The response status code should be 201
    And I extract response field "id" and store it as "wfRev2Id"
    When I make a request to deploy revision "wfRev2Id" of "apis" resource "wfApiId" with payload "wfDeploy"

    # Revision 2's request is held: revision 1 is STILL the only revision with an APPROVED deployment.
    # The entry COUNT cannot be used from here on — measured: `query=deployed:true` also lists a revision whose
    # deployment is merely pending (revision 2 appears with deploymentInfo status CREATED), so only the
    # deployment status distinguishes deployed from requested.
    When I retrieve the deployed revisions of "apis" resource "wfApiId"
    Then The response status code should be 200
    And The only revision deployed with an APPROVED deployment should be "wfRev1Id"

    # Reject revision 2's request → revision 1 stays deployed and revision 2 is still not deployed.
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_REVISION_DEPLOYMENT" workflow reference where "apiName" is "{{wfApiName}}" as "rev2RejectWfRef"
    And I "REJECTED" the workflow with reference "rev2RejectWfRef"
    Then The response status code should be 200
    Given I act as "<requester>"
    When I retrieve the deployed revisions of "apis" resource "wfApiId"
    Then The response status code should be 200
    And The only revision deployed with an APPROVED deployment should be "wfRev1Id"

    # Request revision 2 again and APPROVE it → revision 2 is deployed and revision 1 is replaced.
    When I make a request to deploy revision "wfRev2Id" of "apis" resource "wfApiId" with payload "wfDeploy"
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_REVISION_DEPLOYMENT" workflow reference where "apiName" is "{{wfApiName}}" as "rev2ApproveWfRef"
    And I "APPROVED" the workflow with reference "rev2ApproveWfRef"
    Then The response status code should be 200
    Given I act as "<requester>"
    When I retrieve the deployed revisions of "apis" resource "wfApiId"
    Then The response status code should be 200
    And The only revision deployed with an APPROVED deployment should be "wfRev2Id"

    Examples:
      | requester     |
      | admin         |
      | admin@tenant1.com |
      | publisherUser |
      | publisherUser@tenant1.com |
