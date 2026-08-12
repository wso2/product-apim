@cleanup
Feature: Approval workflow - subscription update

  With the SubscriptionUpdate Approval executor active, changing a subscription's tier parks an
  AM_SUBSCRIPTION_UPDATE workflow; the new tier is applied only after an admin approves it. The pending task
  carries an `updates` delta naming the attribute and both its current and requested values — that is the whole
  basis on which the approver decides, so it is asserted rather than only the end state. Ports the
  subscription-update arc of WorkflowApprovalExecutorTest#testSubscriptionUpdateWorkflowProcess.

  Runs across both the REQUESTER axis (admin vs subscriber) and the tenant axis (super tenant vs tenant1.com),
  producing four rows. The tenant admin decides each request.

  @cap:admin @feat:workflows @dep:devportal @legacy:WorkflowApprovalExecutorTest @type:regression
  Scenario Outline: Subscription tier change is applied only after approval as requester <requester>
    Given The system is ready with an admin approver and "<requester>" as the requester

    # This scenario owns its API (a runner-scoped shared one cannot survive the per-scenario @cleanup sweep).
    # Publishing it is a prerequisite, not the subject, so the admin does it.
    Given I act as the tenant admin for "<requester>"
    When I publish API from "artifacts/payloads/create_apim_test_api.json" through the approval workflow as "wfApiId"

    # An approved application, subscribed (and approved) to the published API at Unlimited.
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

    # Request the tier change Unlimited → Gold — parks an update workflow; the effective tier stays Unlimited.
    # Capture the real subscription as the update payload so it carries the actual application/api ids, and send
    # the change as the DevPortal PUT's two-field form (current in throttlingPolicy, target in
    # requestedThrottlingPolicy) — the pair the parked task's `updates` delta is built from.
    Given I act as "<requester>"
    When I get the subscription with id "wfSubId"
    Then The response status code should be 200
    And I put the response payload in context as "subscriptionPayload"
    When I request a subscription plan change of "wfSubId" from "Unlimited" to "Gold"
    Then The response status code should be 200
    When I retrieve the subscription for Api "wfApiId" by Application "createdAppId"
    Then The response status code should be 200
    And The value of response field "list[0].throttlingPolicy" should be "Unlimited"

    # The pending task tells the approver exactly which attribute changes and between which values.
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_SUBSCRIPTION_UPDATE" workflow reference where "applicationName" is "{{createdAppName}}" as "subUpdWfRef"
    And I get the workflow with reference "subUpdWfRef"
    Then The response status code should be 200
    And The workflow property "applicationName" should be "{{createdAppName}}"
    And The workflow update entry "Subscription Tier" should change from "Unlimited" to "Gold"

    # Approve the update workflow; the tier is now Gold.
    When I "APPROVED" the workflow with reference "subUpdWfRef"
    Then The response status code should be 200
    Given I act as "<requester>"
    When I retrieve the subscription for Api "wfApiId" by Application "createdAppId"
    Then The response status code should be 200
    And The response should contain "Gold"
    And The value of response field "list[0].throttlingPolicy" should be "Gold"

    Examples:
      | requester      |
      | admin          |
      | admin@tenant1.com |
      | subscriberUser |
      | subscriberUser@tenant1.com |
