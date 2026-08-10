@cleanup
Feature: Approval workflow - subscription creation

  With the SubscriptionCreation Approval executor active, subscribing an application to a published API parks
  the subscription in ON_HOLD until an admin approves it, after which it becomes UNBLOCKED. Ports the
  subscription arc of WorkflowApprovalExecutorTest#testSubscriptionWorkflowProcess.

  The scenario is self-contained (it owns its API and application): a runner-scoped shared API cannot be used
  here because every scenario is @cleanup, and the per-scenario cleanup hook sweeps ALL registered resources
  after the first scenario — so each subscription scenario publishes its own API (approving the
  revision-deployment and API-state workflows that publishing parks under the active Approval executors).

  Runs twice over the REQUESTER axis (the legacy SUPER_TENANT_ADMIN vs SUPER_TENANT_USER factory): the admin
  decides in both rows, while the actor that owns the application and subscribes is the admin itself in one row
  and a plain devportal subscriber in the other.

  @cap:admin @feat:workflows @dep:devportal @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario Outline: Subscription stays ON_HOLD until approved as requester <requester>
    Given The system is ready with an admin approver and "<requester>" as the requester

    # Publish an API to subscribe to (owned by this scenario; approves the parked deploy + state workflows).
    # Publishing is a prerequisite, not the subject, so the admin does it.
    Given I act as the tenant admin for "<requester>"
    When I publish API from "artifacts/payloads/create_apim_test_api.json" through the approval workflow as "wfApiId"

    # An application to subscribe with — approve its creation workflow so it is usable.
    Given I act as "<requester>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "wfApp"
    And I create an application with payload "wfApp"
    Then The response status code should be 201
    And I extract response field "name" and store it as "createdAppName"
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_APPLICATION_CREATION" workflow reference where "applicationName" is "{{createdAppName}}" as "appWfRef"
    And I "APPROVED" the workflow with reference "appWfRef"
    Then The response status code should be 200

    # Subscribe the approved app to the published API (retrying the transient gateway-artifact race).
    Given I act as "<requester>"
    When I subscribe application "createdAppId" to API "wfApiId" retrying transient errors as "wfSubId"
    Then The response status code should be 201

    # The subscription parks ON_HOLD until approved.
    When I retrieve the subscription for Api "wfApiId" by Application "createdAppId"
    Then The response status code should be 200
    And The response should contain "ON_HOLD"
    And The value of response field "list[0].status" should be "ON_HOLD"

    # Admin approves the subscription workflow.
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_SUBSCRIPTION_CREATION" workflow reference where "applicationName" is "{{createdAppName}}" as "subWfRef"
    And I "APPROVED" the workflow with reference "subWfRef"
    Then The response status code should be 200

    # The subscription is now UNBLOCKED.
    Given I act as "<requester>"
    When I retrieve the subscription for Api "wfApiId" by Application "createdAppId"
    Then The response status code should be 200
    And The response should contain "UNBLOCKED"
    And The value of response field "list[0].status" should be "UNBLOCKED"

    Examples:
      | requester      |
      | admin          |
      | admin@tenant1.com |
      | subscriberUser |
      | subscriberUser@tenant1.com |
