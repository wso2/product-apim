@cleanup
Feature: Approval workflow - subscription deletion

  With the SubscriptionDeletion Approval executor active, deleting a subscription is accepted (201) but the
  subscription stays DELETE_PENDING until an admin approves the AM_SUBSCRIPTION_DELETION task, after which it
  is removed. Ports the subscription-deletion arc of
  WorkflowApprovalExecutorTest#testSubscriptionDeletionWorkflowProcess.

  @cap:admin @feat:workflows @dep:devportal @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario: Subscription stays DELETE_PENDING until the deletion is approved
    Given The system is ready
    And I have valid access tokens as "admin"

    # This scenario owns its API (a runner-scoped shared one cannot survive the per-scenario @cleanup sweep).
    When I publish API from "artifacts/payloads/create_apim_test_api.json" through the approval workflow as "wfApiId"

    # An approved application, subscribed (and approved) to the published API.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "wfApp"
    And I create an application with payload "wfApp"
    Then The response status code should be 201
    And I extract response field "name" and store it as "createdAppName"
    When I capture the pending "AM_APPLICATION_CREATION" workflow reference where "applicationName" is "{{createdAppName}}" as "appWfRef"
    And I "APPROVED" the workflow with reference "appWfRef"
    Then The response status code should be 200
    When I subscribe application "createdAppId" to API "wfApiId" retrying transient errors as "wfSubId"
    Then The response status code should be 201
    When I capture the pending "AM_SUBSCRIPTION_CREATION" workflow reference where "applicationName" is "{{createdAppName}}" as "subWfRef"
    And I "APPROVED" the workflow with reference "subWfRef"
    Then The response status code should be 200

    # Delete the subscription — accepted (201) but it parks in DELETE_PENDING.
    When I delete the subscription with id "wfSubId"
    Then The response status code should be 201
    When I retrieve the subscription for Api "wfApiId" by Application "createdAppId"
    Then The response status code should be 200
    And The response should contain "DELETE_PENDING"

    # Approve the deletion workflow; the subscription is removed.
    When I capture the pending "AM_SUBSCRIPTION_DELETION" workflow reference where "applicationName" is "{{createdAppName}}" as "subDelWfRef"
    And I "APPROVED" the workflow with reference "subDelWfRef"
    Then The response status code should be 200
