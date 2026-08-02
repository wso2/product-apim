@cleanup
Feature: Approval workflow - application creation

  With the ApplicationCreation Approval executor active, creating an application parks it in CREATED until an
  admin approves it, after which it becomes APPROVED. The pending task surfaces the application name and (via
  applicationAttributesVisibility) the custom application attributes. A non-admin token is rejected 401 on the
  workflow endpoints. Ports WorkflowApprovalExecutorTest#testApplicationWorkflowProcess.

  @cap:admin @feat:workflows @dep:devportal @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario: Application stays CREATED until approved, then APPROVED
    Given The system is ready
    And I have valid access tokens as "admin"

    When I put JSON payload from file "artifacts/payloads/create_apim_workflow_app.json" in context as "wfApp"
    And I create an application with payload "wfApp"
    Then The response status code should be 201
    And I extract response field "name" and store it as "createdAppName"

    # Application parks in CREATED.
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    And The response should contain "CREATED"

    # Admin sees the pending task with the application's name and custom attributes surfaced.
    When I capture the pending "AM_APPLICATION_CREATION" workflow reference where "applicationName" is "{{createdAppName}}" as "appWfRef"
    And I get the workflow with reference "appWfRef"
    Then The response status code should be 200
    And The workflow property "applicationName" should be "{{createdAppName}}"
    And The workflow application attribute "External Reference ID" should be "10"
    And The workflow application attribute "Department Name" should be "Finance"

    # Admin approves; the application becomes APPROVED.
    When I "APPROVED" the workflow with reference "appWfRef"
    Then The response status code should be 200
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    And The response should contain "APPROVED"

  @cap:admin @feat:workflows @dep:devportal @legacy:WorkflowApprovalExecutorTest @type:negative
  Scenario: A non-admin token cannot view the pending workflows
    Given The system is ready and I have valid devportal access token as "subscriberUser"
    When I attempt to list pending "AM_APPLICATION_CREATION" workflows as a non-admin
    Then The response status code should be 401
