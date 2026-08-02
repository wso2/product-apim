@cleanup
Feature: Approval workflow - pending task cleanup on resource delete

  Deleting a resource that has a pending workflow task cleans up that task: after a fresh application's
  creation workflow is parked (CREATED), deleting the application removes the pending
  AM_APPLICATION_CREATION task, so a get-by-reference on it returns 404. Ports the cleanup arc of
  WorkflowApprovalExecutorTest#testCleanUpWorkflowProcess.

  @cap:admin @feat:workflows @dep:devportal @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario: Deleting an application with a pending creation task cleans up the task
    Given The system is ready
    And I have valid access tokens as "admin"

    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "wfApp"
    And I create an application with payload "wfApp"
    Then The response status code should be 201
    And I extract response field "name" and store it as "createdAppName"

    # The creation task is pending and retrievable by reference.
    When I capture the pending "AM_APPLICATION_CREATION" workflow reference where "applicationName" is "{{createdAppName}}" as "appWfRef"
    And I get the workflow with reference "appWfRef"
    Then The response status code should be 200

    # Delete the application (accepted, 201 → DELETE_PENDING) and approve the deletion; completing the delete
    # cleans up the still-pending creation task.
    When I delete the application with id "createdAppId"
    Then The response status code should be 201
    When I capture the pending "AM_APPLICATION_DELETION" workflow reference where "applicationName" is "{{createdAppName}}" as "appDelWfRef"
    And I "APPROVED" the workflow with reference "appDelWfRef"
    Then The response status code should be 200

    # The pending creation task no longer exists.
    When I get the workflow with reference "appWfRef"
    Then The response status code should be 404
