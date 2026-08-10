@cleanup
Feature: Approval workflow - application deletion

  With the ApplicationDeletion Approval executor active, deleting an approved application is accepted (201) but
  the application stays DELETE_PENDING until an admin approves the AM_APPLICATION_DELETION task; a re-delete
  while pending is rejected 400, and after approval the application is gone (404). The deletion task carries
  its own properties and (via applicationAttributesVisibility) the application's custom attributes, so the
  approver can see WHICH application is about to be destroyed. Ports
  WorkflowApprovalExecutorTest#testApplicationDeletionWorkflowProcess.

  Runs across both the REQUESTER axis (admin vs subscriber) and the tenant axis (super tenant vs tenant1.com),
  producing four rows. The tenant admin decides each request.

  @cap:admin @feat:workflows @dep:devportal @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario Outline: Application stays DELETE_PENDING until the deletion is approved as requester <requester>
    Given The system is ready with an admin approver and "<requester>" as the requester

    # Create + approve an application so it is APPROVED before we delete it.
    When I put JSON payload from file "artifacts/payloads/create_apim_workflow_app.json" in context as "wfApp"
    And I create an application with payload "wfApp"
    Then The response status code should be 201
    And I extract response field "name" and store it as "createdAppName"
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_APPLICATION_CREATION" workflow reference where "applicationName" is "{{createdAppName}}" as "appWfRef"
    And I "APPROVED" the workflow with reference "appWfRef"
    Then The response status code should be 200

    # Delete — accepted (201) but the application parks in DELETE_PENDING.
    Given I act as "<requester>"
    When I delete the application with id "createdAppId"
    Then The response status code should be 201
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    And The response should contain "DELETE_PENDING"

    # A re-delete while DELETE_PENDING is rejected 400.
    When I delete the application with id "createdAppId"
    Then The response status code should be 400

    # The deletion task identifies the application by name and surfaces its custom attributes.
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_APPLICATION_DELETION" workflow reference where "applicationName" is "{{createdAppName}}" as "appDelWfRef"
    And I get the workflow with reference "appDelWfRef"
    Then The response status code should be 200
    And The workflow property "applicationName" should be "{{createdAppName}}"
    And The workflow application attribute "Department Name" should be "Finance"
    And The workflow application attribute "External Reference ID" should be "10"
    And The workflow application attribute "Technical Contact" should be "bob@example.com"

    # Approve the deletion; the application is gone (404).
    When I "APPROVED" the workflow with reference "appDelWfRef"
    Then The response status code should be 200
    Given I act as "<requester>"
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 404

    Examples:
      | requester      |
      | admin          |
      | admin@tenant1.com |
      | subscriberUser |
      | subscriberUser@tenant1.com |
