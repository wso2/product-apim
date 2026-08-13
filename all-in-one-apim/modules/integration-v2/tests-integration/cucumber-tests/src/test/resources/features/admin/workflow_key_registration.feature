@cleanup
Feature: Approval workflow - application key registration

  With the ProductionApplicationRegistration Approval executor active, generating production keys for an
  application parks the key in state CREATED until an admin approves the
  AM_APPLICATION_REGISTRATION_PRODUCTION task, after which the key state becomes COMPLETED. The registration
  task carries its own properties and (via applicationAttributesVisibility) the application's custom
  attributes, so the approver can see WHICH application is asking for credentials. Ports
  WorkflowApprovalExecutorTest#testRegistrationWorkflowProcess.

  The application is created from the suite's attribute-carrying payload precisely so those attributes can be
  asserted on the registration task (legacy generates keys for the same application its application-creation
  flow built, which carries all three).

  Runs across both the REQUESTER axis (admin vs subscriber) and the tenant axis (super tenant vs tenant1.com),
  producing four rows. The tenant admin decides each request.

  @cap:admin @feat:workflows @dep:devportal @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario Outline: Key generation stays CREATED until approved, then COMPLETED as requester <requester>
    Given The system is ready with an admin approver and "<requester>" as the requester

    # An approved application to generate keys for.
    When I put JSON payload from file "artifacts/payloads/create_apim_workflow_app.json" in context as "wfApp"
    And I create an application with payload "wfApp"
    Then The response status code should be 201
    And I extract response field "name" and store it as "createdAppName"
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_APPLICATION_CREATION" workflow reference where "applicationName" is "{{createdAppName}}" as "appWfRef"
    And I "APPROVED" the workflow with reference "appWfRef"
    Then The response status code should be 200

    # Generate production keys — accepted (200) but the key parks in CREATED (no consumerKey issued yet).
    Given I act as "<requester>"
    When I generate pending production keys for application "createdAppId"
    Then The response status code should be 200
    And The response should contain "CREATED"

    # The registration task identifies the application by name and surfaces its custom attributes.
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_APPLICATION_REGISTRATION_PRODUCTION" workflow reference where "applicationName" is "{{createdAppName}}" as "keyWfRef"
    And I get the workflow with reference "keyWfRef"
    Then The response status code should be 200
    And The workflow property "applicationName" should be "{{createdAppName}}"
    And The workflow application attribute "Department Name" should be "Finance"
    And The workflow application attribute "External Reference ID" should be "10"
    And The workflow application attribute "Technical Contact" should be "bob@example.com"

    # Approve the key-registration workflow; the key state becomes COMPLETED.
    When I "APPROVED" the workflow with reference "keyWfRef"
    Then The response status code should be 200
    Given I act as "<requester>"
    When I retrieve existing application keys for "createdAppId"
    Then The response status code should be 200
    And The response should contain "COMPLETED"

    Examples:
      | requester      |
      | admin          |
      | admin@tenant1.com |
      | subscriberUser |
      | subscriberUser@tenant1.com |
