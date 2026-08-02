@cleanup
Feature: Approval workflow - API state change

  With the APIStateChange Approval executor active, a publish lifecycle-change request is accepted (the
  workflow is created) but the API stays in CREATED until an admin approves the AM_API_STATE task, after
  which it becomes PUBLISHED. Ports the API-state arc of
  WorkflowApprovalExecutorTest#testAPIWorkflowProcess.

  @cap:admin @feat:workflows @dep:publisher @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario: API stays CREATED after a publish request until the state change is approved
    Given The system is ready
    And I have valid access tokens as "admin"

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
    When I put the following JSON payload in context as "wfDeploy"
    """
    [{"name": "{{gatewayEnvironment}}", "vhost": "localhost", "displayOnDevportal": true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "wfApiId" with payload "wfDeploy"
    When I capture the pending "AM_REVISION_DEPLOYMENT" workflow reference where "apiName" is "{{wfApiName}}" as "depWfRef"
    And I "APPROVED" the workflow with reference "depWfRef"
    Then The response status code should be 200

    # Request publish — accepted (200) but the API must remain CREATED pending approval.
    When I publish the "apis" resource with id "wfApiId"
    Then The response status code should be 200
    And The lifecycle status of API "wfApiId" should be "Created"

    # Admin approves the API state-change workflow; the API becomes PUBLISHED.
    When I capture the pending "AM_API_STATE" workflow reference where "apiName" is "{{wfApiName}}" as "stateWfRef"
    And I "APPROVED" the workflow with reference "stateWfRef"
    Then The response status code should be 200
    And The lifecycle status of API "wfApiId" should be "Published"
