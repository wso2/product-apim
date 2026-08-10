@cleanup
Feature: Approval workflow - application update

  With the ApplicationUpdate Approval executor active, updating an approved application parks it in
  UPDATE_PENDING; a second update while pending is rejected 409. Approving applies the changes - name,
  description and attributes - and returns the status to APPROVED; a later update that is REJECTED leaves the
  app UPDATE_REJECTED with every previous value intact. The pending task also carries an `updates` delta
  telling the approver exactly what would change. Covers BOTH the approve and reject arcs. Ports
  WorkflowApprovalExecutorTest#testApplicationUpdateWorkflowProcess.

  Runs twice over the REQUESTER axis (the legacy SUPER_TENANT_ADMIN vs SUPER_TENANT_USER factory): the admin
  decides in both rows, while the actor that owns and updates the application is the admin itself in one row
  and a plain devportal subscriber in the other.

  @cap:admin @feat:workflows @dep:devportal @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario Outline: Application update parks UPDATE_PENDING, approve applies, second update rejects as requester <requester>
    Given The system is ready with an admin approver and "<requester>" as the requester

    # Create an application and approve its creation so it is APPROVED before we exercise updates.
    When I put JSON payload from file "artifacts/payloads/create_apim_workflow_app.json" in context as "wfApp"
    And I create an application with payload "wfApp"
    Then The response status code should be 201
    And I extract response field "name" and store it as "createdAppName"
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_APPLICATION_CREATION" workflow reference where "applicationName" is "{{createdAppName}}" as "appWfRef"
    And I "APPROVED" the workflow with reference "appWfRef"
    Then The response status code should be 200

    # Update the application (new name, description and attributes) — it parks in UPDATE_PENDING.
    Given I act as "<requester>"
    When I put the following JSON payload in context as "wfAppUpdate"
    """
    {"name": "{{createdAppName}}Updated", "throttlingPolicy": "Unlimited", "description": "Updated for approval", "attributes": {"Department Name": "Finance", "External Reference ID": "20", "Technical Contact": "alice@example.com"}}
    """
    And I update the application "createdAppId" with payload "wfAppUpdate"
    Then The response status code should be 200
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    And The response should contain "UPDATE_PENDING"
    # Nothing is applied yet: the app still carries its pre-update name and description.
    And The value of response field "name" should be "{{createdAppName}}"
    And The value of response field "description" should be "Application for the approval-workflow suite carrying the three custom attributes"

    # A second update while UPDATE_PENDING is blocked with 409.
    When I update the application "createdAppId" with payload "wfAppUpdate"
    Then The response status code should be 409

    # The pending task is keyed by the application's CURRENT (pre-update) name and shows the approver the delta.
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_APPLICATION_UPDATE" workflow reference where "applicationName" is "{{createdAppName}}" as "updWfRef"
    And I get the workflow with reference "updWfRef"
    Then The response status code should be 200
    And The workflow property "applicationName" should be "{{createdAppName}}"
    And The workflow update entry "External Reference ID" should change from "10" to "20"

    # Approve the update — name, description and the attribute are all applied, and the app returns to APPROVED.
    When I "APPROVED" the workflow with reference "updWfRef"
    Then The response status code should be 200
    Given I act as "<requester>"
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    And The response should contain "APPROVED"
    And The response should contain "alice@example.com"
    And The value of response field "name" should be "{{createdAppName}}Updated"
    And The value of response field "description" should be "Updated for approval"

    # A further update, then REJECT it — the app is UPDATE_REJECTED and keeps every approved value.
    When I put the following JSON payload in context as "wfAppReject"
    """
    {"name": "{{createdAppName}}Rejected", "throttlingPolicy": "Unlimited", "description": "Should be rejected", "attributes": {"Department Name": "HR", "External Reference ID": "30", "Technical Contact": "charlie@example.com"}}
    """
    And I update the application "createdAppId" with payload "wfAppReject"
    Then The response status code should be 200
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_APPLICATION_UPDATE" workflow reference where "applicationName" is "{{createdAppName}}Updated" as "rejWfRef"
    And I "REJECTED" the workflow with reference "rejWfRef"
    Then The response status code should be 200
    Given I act as "<requester>"
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    And The response should contain "UPDATE_REJECTED"
    And The response should contain "alice@example.com"
    And The response should not contain "charlie@example.com"
    And The value of response field "name" should be "{{createdAppName}}Updated"
    And The value of response field "description" should be "Updated for approval"

    Examples:
      | requester      |
      | admin          |
      | admin@tenant1.com |
      | subscriberUser |
      | subscriberUser@tenant1.com |
