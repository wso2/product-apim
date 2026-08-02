@cleanup
Feature: Approval workflow - application key registration

  With the ProductionApplicationRegistration Approval executor active, generating production keys for an
  application parks the key in state CREATED until an admin approves the
  AM_APPLICATION_REGISTRATION_PRODUCTION task, after which the key state becomes COMPLETED. Ports
  WorkflowApprovalExecutorTest#testRegistrationWorkflowProcess.

  @cap:admin @feat:workflows @dep:devportal @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario: Key generation stays CREATED until approved, then COMPLETED
    Given The system is ready
    And I have valid access tokens as "admin"

    # An approved application to generate keys for.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "wfApp"
    And I create an application with payload "wfApp"
    Then The response status code should be 201
    And I extract response field "name" and store it as "createdAppName"
    When I capture the pending "AM_APPLICATION_CREATION" workflow reference where "applicationName" is "{{createdAppName}}" as "appWfRef"
    And I "APPROVED" the workflow with reference "appWfRef"
    Then The response status code should be 200

    # Generate production keys — accepted (200) but the key parks in CREATED (no consumerKey issued yet).
    When I generate pending production keys for application "createdAppId"
    Then The response status code should be 200
    And The response should contain "CREATED"

    # Approve the key-registration workflow; the key state becomes COMPLETED.
    When I capture the pending "AM_APPLICATION_REGISTRATION_PRODUCTION" workflow reference where "applicationName" is "{{createdAppName}}" as "keyWfRef"
    And I "APPROVED" the workflow with reference "keyWfRef"
    Then The response status code should be 200
    When I retrieve existing application keys for "createdAppId"
    Then The response status code should be 200
    And The response should contain "COMPLETED"
