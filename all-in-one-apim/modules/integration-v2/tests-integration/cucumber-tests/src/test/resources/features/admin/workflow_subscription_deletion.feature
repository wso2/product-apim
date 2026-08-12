@cleanup
Feature: Approval workflow - subscription deletion

  With the SubscriptionDeletion Approval executor active, deleting a subscription is accepted (201) but the
  subscription stays DELETE_PENDING until an admin approves the AM_SUBSCRIPTION_DELETION task, after which it
  is REMOVED — proved by re-reading the application's subscriptions and finding none, not by the approve's own
  200 (which a silent no-op would also return). Ports the subscription-deletion arc of
  WorkflowApprovalExecutorTest#testSubscriptionDeletionWorkflowProcess.

  Runs across both the REQUESTER axis (admin vs subscriber) and the tenant axis (super tenant vs tenant1.com),
  producing four rows. The tenant admin decides each request.

  @cap:admin @feat:workflows @dep:devportal @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario Outline: Subscription stays DELETE_PENDING until the deletion is approved, then is removed as requester <requester>
    Given The system is ready with an admin approver and "<requester>" as the requester

    # This scenario owns its API (a runner-scoped shared one cannot survive the per-scenario @cleanup sweep).
    # Publishing it is a prerequisite, not the subject, so the admin does it.
    Given I act as the tenant admin for "<requester>"
    When I publish API from "artifacts/payloads/create_apim_test_api.json" through the approval workflow as "wfApiId"

    # An approved application, subscribed (and approved) to the published API — owned by the requester.
    Given I act as "<requester>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "wfApp"
    And I create an application with payload "wfApp"
    Then The response status code should be 201
    And I extract response field "name" and store it as "createdAppName"
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_APPLICATION_CREATION" workflow reference where "applicationName" is "{{createdAppName}}" as "appWfRef"
    And I "APPROVED" the workflow with reference "appWfRef"
    Then The response status code should be 200
    Given I act as "<requester>"
    When I subscribe application "createdAppId" to API "wfApiId" retrying transient errors as "wfSubId"
    Then The response status code should be 201
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_SUBSCRIPTION_CREATION" workflow reference where "applicationName" is "{{createdAppName}}" as "subWfRef"
    And I "APPROVED" the workflow with reference "subWfRef"
    Then The response status code should be 200

    # Delete the subscription — accepted (201) but it parks in DELETE_PENDING and is STILL listed.
    Given I act as "<requester>"
    When I delete the subscription with id "wfSubId"
    Then The response status code should be 201
    When I retrieve the subscription for Api "wfApiId" by Application "createdAppId"
    Then The response status code should be 200
    And The response should contain "DELETE_PENDING"
    When I retrieve all subscriptions of application "createdAppId"
    Then The response status code should be 200
    And The subscription list should contain exactly 1 subscriptions

    # Approve the deletion workflow.
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_SUBSCRIPTION_DELETION" workflow reference where "applicationName" is "{{createdAppName}}" as "subDelWfRef"
    And I "APPROVED" the workflow with reference "subDelWfRef"
    Then The response status code should be 200

    # The EFFECT of the approval: the subscription is gone. This count is what distinguishes an approve that
    # actually removed the subscription from one that returned 200 and left it DELETE_PENDING — the
    # 1-before/0-after pair is load-bearing and must not be trimmed.
    Given I act as "<requester>"
    When I retrieve all subscriptions of application "createdAppId"
    Then The response status code should be 200
    And The subscription list should contain exactly 0 subscriptions

    Examples:
      | requester      |
      | admin          |
      | admin@tenant1.com |
      | subscriberUser |
      | subscriberUser@tenant1.com |
