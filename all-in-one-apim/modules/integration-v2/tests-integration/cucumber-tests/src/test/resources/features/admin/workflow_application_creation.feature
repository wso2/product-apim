@cleanup
Feature: Approval workflow - application creation

  With the ApplicationCreation Approval executor active, creating an application parks it in CREATED until an
  admin approves it, after which it becomes APPROVED. The pending task surfaces the application name and (via
  applicationAttributesVisibility) the custom application attributes. A non-admin token is rejected 401 on
  every workflow endpoint - the type listing, the get-by-external-reference and the deciding
  update-workflow-status. Ports WorkflowApprovalExecutorTest#testApplicationWorkflowProcess.

  The regression scenario runs across both the REQUESTER axis (admin vs subscriber) and the tenant axis
  (super tenant vs tenant1.com), producing four rows. The tenant admin approves each request.

  @cap:admin @feat:workflows @dep:devportal @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario Outline: Application stays CREATED until approved, then APPROVED as requester <requester>
    Given The system is ready with an admin approver and "<requester>" as the requester

    When I put JSON payload from file "artifacts/payloads/create_apim_workflow_app.json" in context as "wfApp"
    And I create an application with payload "wfApp"
    Then The response status code should be 201
    And I extract response field "name" and store it as "createdAppName"

    # Application parks in CREATED.
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    And The response should contain "CREATED"

    # Admin sees the pending task with the application's name and custom attributes surfaced.
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_APPLICATION_CREATION" workflow reference where "applicationName" is "{{createdAppName}}" as "appWfRef"
    And I get the workflow with reference "appWfRef"
    Then The response status code should be 200
    And The workflow property "applicationName" should be "{{createdAppName}}"
    And The workflow application attribute "External Reference ID" should be "10"
    And The workflow application attribute "Department Name" should be "Finance"
    And The workflow application attribute "Technical Contact" should be "bob@example.com"

    # Admin approves; the application becomes APPROVED for its owner.
    When I "APPROVED" the workflow with reference "appWfRef"
    Then The response status code should be 200
    Given I act as "<requester>"
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    And The response should contain "APPROVED"

    Examples:
      | requester      |
      | admin          |
      | admin@tenant1.com |
      | subscriberUser |
      | subscriberUser@tenant1.com |

  @cap:admin @feat:workflows @dep:devportal @legacy:WorkflowApprovalExecutorTest @type:negative
  Scenario Outline: A non-admin token cannot list the pending workflows of type <workflowType> as <actor>
    Given The system is ready and I have valid devportal access token as "<actor>"
    When I attempt to list pending "<workflowType>" workflows as a non-admin
    Then The response status code should be 401

    Examples:
      | workflowType                           | actor                      |
      | AM_APPLICATION_CREATION                | subscriberUser             |
      | AM_APPLICATION_CREATION                | subscriberUser@tenant1.com |
      | AM_API_STATE                           | subscriberUser             |
      | AM_API_STATE                           | subscriberUser@tenant1.com |
      | AM_SUBSCRIPTION_CREATION               | subscriberUser             |
      | AM_SUBSCRIPTION_CREATION               | subscriberUser@tenant1.com |
      | AM_APPLICATION_REGISTRATION_PRODUCTION | subscriberUser             |
      | AM_APPLICATION_REGISTRATION_PRODUCTION | subscriberUser@tenant1.com |
      | AM_USER_SIGNUP                         | subscriberUser             |
      | AM_USER_SIGNUP                         | subscriberUser@tenant1.com |

  # The deciding endpoint is the one whose bypass would be an actual privilege escalation (a non-admin
  # self-approving its own pending request), so its 401 is asserted against a REAL pending reference - and the
  # admin then approves that same reference for 200, which is what proves the 401s were the scope guard firing
  # rather than an unknown-reference rejection.
  # Also carries the wrong-scope half of APIStateChangeWorkflowTestCase#testWorkflowCallbackRestAPI: that legacy
  # method posts to the very same update-workflow-status endpoint with a token lacking the workflow scope. Legacy
  # asserted 401 against the publisher API of the day; MEASURED on this build the admin endpoint answers 401 too,
  # so the case is this scenario and is not duplicated. Its unknown-reference half (404) lives in
  # workflow_api_state_change.feature.
  @cap:admin @feat:workflows @dep:devportal @legacy:WorkflowApprovalExecutorTest @legacy:APIStateChangeWorkflowTestCase @type:negative
  Scenario Outline: A non-admin token cannot read or decide a pending workflow by its external reference as <requester>
    Given The system is ready with an admin approver and "<requester>" as the requester

    When I put JSON payload from file "artifacts/payloads/create_apim_workflow_app.json" in context as "guardApp"
    And I create an application with payload "guardApp"
    Then The response status code should be 201
    And I extract response field "name" and store it as "guardAppName"
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_APPLICATION_CREATION" workflow reference where "applicationName" is "{{guardAppName}}" as "guardWfRef"

    # The requester holds no apim:admin scope, so both workflow endpoints reject its token with 401.
    Given I act as "<requester>"
    When I attempt to get the workflow with reference "guardWfRef" as a non-admin
    Then The response status code should be 401
    When I attempt to "APPROVED" the workflow with reference "guardWfRef" as a non-admin
    Then The response status code should be 401

    # The application is still pending - the rejected approve had no effect.
    When I retrieve the application with id "createdAppId"
    Then The response status code should be 200
    And The response should contain "CREATED"

    # The same reference IS decidable by the admin.
    Given I act as the tenant admin for "<requester>"
    When I "APPROVED" the workflow with reference "guardWfRef"
    Then The response status code should be 200

    Examples:
      | requester                 |
      | subscriberUser            |
      | subscriberUser@tenant1.com |
