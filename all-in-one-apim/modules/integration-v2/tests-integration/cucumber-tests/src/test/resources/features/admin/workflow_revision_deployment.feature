@cleanup
Feature: Approval workflow - API revision deployment

  With the APIRevisionDeployment Approval executor active, a deploy-revision request parks an
  AM_REVISION_DEPLOYMENT workflow and the revision is NOT deployed until an admin approves it; rejecting the
  request leaves it undeployed while approving deploys it. Covers BOTH the reject and approve arcs. Ports the
  reject/approve arc of WorkflowApprovalExecutorTest#testAPIRevisionDeploymentWorkflowProcess.

  @cap:admin @feat:workflows @dep:publisher @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario: Revision deployment is held until approved; a rejected request stays undeployed
    Given The system is ready
    And I have valid access tokens as "admin"

    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "wfApi"
    And I create an "apis" resource with payload "wfApi" as "wfApiId"
    Then The response status code should be 201
    And I extract response field "name" and store it as "wfApiName"
    When I put the following JSON payload in context as "wfRevision"
    """
    {"description": "Initial Revision"}
    """
    And I make a request to create a revision for "apis" resource "wfApiId" with payload "wfRevision"
    When I put the following JSON payload in context as "wfDeploy"
    """
    [{"name": "{{gatewayEnvironment}}", "vhost": "localhost", "displayOnDevportal": true}]
    """

    # First deploy request → parked. Reject it → the revision is not deployed.
    When I make a request to deploy revision "revisionId" of "apis" resource "wfApiId" with payload "wfDeploy"
    And I capture the pending "AM_REVISION_DEPLOYMENT" workflow reference where "apiName" is "{{wfApiName}}" as "depWfRef"
    And I "REJECTED" the workflow with reference "depWfRef"
    Then The response status code should be 200
    When I retrieve the deployed revisions of "apis" resource "wfApiId"
    Then The response status code should be 200
    And The response list should have 0 entries

    # Deploy again → approve it → the revision is deployed.
    When I make a request to deploy revision "revisionId" of "apis" resource "wfApiId" with payload "wfDeploy"
    And I capture the pending "AM_REVISION_DEPLOYMENT" workflow reference where "apiName" is "{{wfApiName}}" as "depWfRef2"
    And I "APPROVED" the workflow with reference "depWfRef2"
    Then The response status code should be 200
    When I retrieve the deployed revisions of "apis" resource "wfApiId"
    Then The response status code should be 200
    And The response list should have 1 entries
