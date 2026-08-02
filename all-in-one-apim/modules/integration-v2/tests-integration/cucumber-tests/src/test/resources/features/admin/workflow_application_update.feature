@cleanup
Feature: Approval workflow - application update

  With the ApplicationUpdate Approval executor active, updating an approved application parks it in
  UPDATE_PENDING; a second update while pending is rejected 409. Approving applies the changes (status back to
  APPROVED); a later update that is REJECTED leaves the app UPDATE_REJECTED with the previous values intact.
  Covers BOTH the approve and reject arcs. Ports
  WorkflowApprovalExecutorTest#testApplicationUpdateWorkflowProcess.

  @cap:admin @feat:workflows @dep:devportal @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario: Application update parks UPDATE_PENDING, approve applies, second update rejects
    Given The system is ready
    And I have valid access tokens as "admin"

    # Create an application and approve its creation so it is APPROVED before we exercise updates.
    When I put JSON payload from file "artifacts/payloads/create_apim_workflow_app.json" in context as "wfApp"
    And I create an application with payload "wfApp"
    Then The response status code should be 201
    And I extract response field "name" and store it as "createdAppName"
    When I capture the pending "AM_APPLICATION_CREATION" workflow reference where "applicationName" is "{{createdAppName}}" as "appWfRef"
    And I "APPROVED" the workflow with reference "appWfRef"
    Then The response status code should be 200

    # Update the application — it parks in UPDATE_PENDING.
    When I put the following JSON payload in context as "wfAppUpdate"
    """
    {"name": "{{createdAppName}}", "throttlingPolicy": "Unlimited", "description": "Updated for approval", "attributes": {"Department Name": "Finance", "External Reference ID": "20", "Technical Contact": "alice@example.com"}}
    """
    And I update the application "createdAppId" with payload "wfAppUpdate"
    Then The response status code should be 200
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    And The response should contain "UPDATE_PENDING"

    # A second update while UPDATE_PENDING is blocked with 409.
    When I update the application "createdAppId" with payload "wfAppUpdate"
    Then The response status code should be 409

    # Approve the update — the new attribute value is applied and the app returns to APPROVED.
    When I capture the pending "AM_APPLICATION_UPDATE" workflow reference where "applicationName" is "{{createdAppName}}" as "updWfRef"
    And I "APPROVED" the workflow with reference "updWfRef"
    Then The response status code should be 200
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    And The response should contain "APPROVED"
    And The response should contain "alice@example.com"

    # A further update, then REJECT it — the app is UPDATE_REJECTED and keeps the approved values.
    When I put the following JSON payload in context as "wfAppReject"
    """
    {"name": "{{createdAppName}}", "throttlingPolicy": "Unlimited", "description": "Should be rejected", "attributes": {"Department Name": "HR", "External Reference ID": "30", "Technical Contact": "charlie@example.com"}}
    """
    And I update the application "createdAppId" with payload "wfAppReject"
    Then The response status code should be 200
    When I capture the pending "AM_APPLICATION_UPDATE" workflow reference where "applicationName" is "{{createdAppName}}" as "rejWfRef"
    And I "REJECTED" the workflow with reference "rejWfRef"
    Then The response status code should be 200
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    And The response should contain "UPDATE_REJECTED"
    And The response should contain "alice@example.com"
    And The response should not contain "charlie@example.com"
