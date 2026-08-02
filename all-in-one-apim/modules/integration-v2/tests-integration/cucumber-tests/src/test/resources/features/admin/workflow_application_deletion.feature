@cleanup
Feature: Approval workflow - application deletion

  With the ApplicationDeletion Approval executor active, deleting an approved application is accepted (201) but
  the application stays DELETE_PENDING until an admin approves the AM_APPLICATION_DELETION task; a re-delete
  while pending is rejected 400, and after approval the application is gone (404). Ports
  WorkflowApprovalExecutorTest#testApplicationDeletionWorkflowProcess.

  @cap:admin @feat:workflows @dep:devportal @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario: Application stays DELETE_PENDING until the deletion is approved
    Given The system is ready
    And I have valid access tokens as "admin"

    # Create + approve an application so it is APPROVED before we delete it.
    When I put JSON payload from file "artifacts/payloads/create_apim_workflow_app.json" in context as "wfApp"
    And I create an application with payload "wfApp"
    Then The response status code should be 201
    And I extract response field "name" and store it as "createdAppName"
    When I capture the pending "AM_APPLICATION_CREATION" workflow reference where "applicationName" is "{{createdAppName}}" as "appWfRef"
    And I "APPROVED" the workflow with reference "appWfRef"
    Then The response status code should be 200

    # Delete — accepted (201) but the application parks in DELETE_PENDING.
    When I delete the application with id "createdAppId"
    Then The response status code should be 201
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    And The response should contain "DELETE_PENDING"

    # A re-delete while DELETE_PENDING is rejected 400.
    When I delete the application with id "createdAppId"
    Then The response status code should be 400

    # Approve the deletion; the application is gone (404).
    When I capture the pending "AM_APPLICATION_DELETION" workflow reference where "applicationName" is "{{createdAppName}}" as "appDelWfRef"
    And I "APPROVED" the workflow with reference "appDelWfRef"
    Then The response status code should be 200
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 404
