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

  # B6 / ChangeSubscriptionBusinessPlanForcefullyTestCase#testUpdateSubscriptionBusinessPlanWhenSubscriptionIsIn
  # TierUpdatePendingStatus. While an AM_SUBSCRIPTION_UPDATE task is parked, the subscription sits in
  # TIER_UPDATE_PENDING, and the PUBLISHER-plane force-change endpoint
  # (POST /subscriptions/{id}/subscription-policy/{policy}) must REFUSE — the pending approval is the authority
  # on that subscription's tier, so letting a publisher overwrite it would silently strip the approver's
  # decision out of the flow.
  #
  # LEGACY ASSERTED NOTHING HERE, and is deliberately not ported as written. Its body is
  #     try { changeSubscriptionBusinessPlan(...); } catch (ApiException e) { assertEquals(e.getCode(), 409); }
  # with the comment "this business plan update should not fail" — so it passes whether the call SUCCEEDS (no
  # assertion runs at all) or is refused, and its comment contradicts its own catch. Porting that shape would
  # import a test that cannot fail.
  #
  # The real behaviour was read off the shipped 9.33.162 bytecode rather than guessed:
  # APIProviderImpl#updateSubscriptionTier compares the subscription's status to TIER_UPDATE_PENDING and throws
  # ExceptionCodes.INVALID_STATE_FOR_BUSINESS_PLAN_CHANGE, whose constant carries errorCode 902022 and HTTP 409
  # with message "Cannot change the business plan of the subscription." Those exact values are pinned below, so
  # a future product change to either the code or the status is a failure rather than a silent pass.
  #
  # Asserted BEYOND legacy: that the pending change has not been applied on the publisher plane before the
  # attempt, and that the refusal is total — the plan is still the original one afterwards, so a rejected
  # force-change cannot have partially written.
  @cap:admin @feat:workflows @dep:devportal @dep:publisher @type:negative @legacy:ChangeSubscriptionBusinessPlanForcefullyTestCase
  Scenario Outline: A publisher force-change of the business plan is refused while the subscription is TIER_UPDATE_PENDING as requester <requester>
    Given The system is ready with an admin approver and "<requester>" as the requester

    Given I act as the tenant admin for "<requester>"
    When I publish API from "artifacts/payloads/create_apim_test_api.json" through the approval workflow as "tupApiId"

    Given I act as "<requester>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "tupApp"
    And I create an application with payload "tupApp"
    Then The response status code should be 201
    And I extract response field "name" and store it as "createdAppName"
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_APPLICATION_CREATION" workflow reference where "applicationName" is "{{createdAppName}}" as "tupAppWfRef"
    And I "APPROVED" the workflow with reference "tupAppWfRef"
    Then The response status code should be 200
    Given I act as "<requester>"
    When I subscribe application "createdAppId" to API "tupApiId" retrying transient errors as "tupSubId"
    Then The response status code should be 201
    Given I act as the tenant admin for "<requester>"
    When I capture the pending "AM_SUBSCRIPTION_CREATION" workflow reference where "applicationName" is "{{createdAppName}}" as "tupSubWfRef"
    And I "APPROVED" the workflow with reference "tupSubWfRef"
    Then The response status code should be 200

    # Park an update task: the subscription enters TIER_UPDATE_PENDING and the tier is NOT yet applied.
    Given I act as "<requester>"
    When I get the subscription with id "tupSubId"
    Then The response status code should be 200
    And I put the response payload in context as "subscriptionPayload"
    When I request a subscription plan change of "tupSubId" from "Unlimited" to "Gold"
    Then The response status code should be 200

    # The status is the precondition of this whole scenario — assert it, do not assume the request parked.
    When I get the subscription with id "tupSubId"
    Then The response status code should be 200
    And The value of response field "status" should be "TIER_UPDATE_PENDING"
    # ...and the requested tier has NOT been applied while the task is pending.
    And The publisher subscription "tupSubId" of API "tupApiId" should have business plan "Unlimited"

    # The subject: a publisher force-change to a THIRD plan, refused with the exact code the product defines.
    Given I act as the tenant admin for "<requester>"
    When I change the subscription business plan of "tupSubId" to "Silver"
    Then The response status code should be 409
    And The value of error response field "code" should be "902022"
    And The value of error response field "message" should be "Cannot change the business plan of the subscription."
    # The description names the offending status; the subscription id in it varies, hence a contains-check here.
    And The response should contain "TIER_UPDATE_PENDING"

    # The refusal is total: neither the requested Gold nor the forced Silver was written.
    And The publisher subscription "tupSubId" of API "tupApiId" should have business plan "Unlimited"

    Examples:
      | requester         |
      | admin             |
      | admin@tenant1.com |
