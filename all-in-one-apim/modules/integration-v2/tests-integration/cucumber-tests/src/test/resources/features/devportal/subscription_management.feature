@cleanup
Feature: DevPortal Subscription Management

  Subscription management-plane operations: blocking / unblocking a subscription and updating its
  throttling plan. Runs in both the super tenant and tenant1.com. The flow spans the provider plane
  (create/publish/block the API) and the consumer plane (subscribe), so it runs as the tenant admin, which
  holds both capabilities. Asserts only management-plane outcomes — that a blocked subscription is refused
  at the gateway (401) is covered by gateway/security-enforcement. Teardown via the per-scenario cleanup
  hook (the subscription cascades when its application is deleted).

  @cap:devportal @feat:subscription-management @type:regression @rule:blocking @legacy:SubscriptionBlockingTestCase
  Scenario Outline: Block and unblock an API subscription as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I have set up application with keys, subscribed to API "createdApiId", and obtained access token for "subscriptionId"
    Then The response status code should be 200

    When I block the subscription with "subscriptionId" for the resource
    Then The response status code should be 200
    When I unblock the subscription with "subscriptionId" for the resource
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:devportal @feat:subscription-management @type:regression @rule:throttling-plan @legacy:SubscriptionThrottlingPolicyTestCase
  Scenario Outline: Update a subscription throttling plan as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I have set up application with keys, subscribed to API "createdApiId", and obtained access token for "subscriptionId"
    Then The response status code should be 200

    # Capture the current subscription so the update step can mutate its throttling plan
    When I get the subscription with id "subscriptionId"
    Then The response status code should be 200
    And I put the response payload in context as "subscriptionPayload"

    When I update the subscription "subscriptionId" with subscription plan "Gold"
    Then The response status code should be 200
    When I get the subscription with id "subscriptionId"
    Then The response status code should be 200
    And The response should contain "Gold"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:devportal @feat:subscription-management @type:negative @rule:blocking @legacy:SubscriptionBlockingTestCase
  Scenario Outline: A subscriber-role user cannot block a subscription as <actor>
    # Admin sets up the API + subscription, then a subscriber-role user (no subscription_block scope) attempts
    # to block it and is rejected.
    Given The system is ready
    And I have valid access tokens as "admin<tenantSuffix>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I have set up application with keys, subscribed to API "createdApiId", and obtained access token for "subscriptionId"
    Then The response status code should be 200

    Given The system is ready and I have valid publisher access tokens as "subscriberUser<tenantSuffix>"
    When I block the subscription with "subscriptionId" for the resource
    Then The response status code should be 401
    # Switch back so @cleanup tears down with the admin token.
    And I act as "admin<tenantSuffix>"

    Examples:
      | tenantSuffix |
      |              |
      | @tenant1.com |

  # Wave B-3: force-changing a subscription's business plan to an invalid plan is rejected (400). Ports the 400
  # negatives of ChangeSubscriptionBusinessPlanForcefullyTestCase via the PUBLISHER change-business-plan endpoint
  # (which validates the plan). verify-first FINDING: the devportal subscription PUT does NOT validate — it
  # silently keeps the current plan and returns 200 — so this uses the publisher force-change endpoint instead.
  @cap:devportal @feat:subscription-management @rule:invalid-plan @type:negative @legacy:ChangeSubscriptionBusinessPlanForcefullyTestCase
  Scenario Outline: Force-changing a subscription to an invalid business plan is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "createdApiId" and deployed it
    When I publish the "apis" resource with id "createdApiId"
    Then The lifecycle status of API "createdApiId" should be "Published"
    When I have set up application with keys, subscribed to API "createdApiId", and obtained access token for "subscriptionId"
    Then The response status code should be 200
    # An empty business plan is rejected.
    When I change the subscription business plan of "subscriptionId" to ""
    Then The response status code should be 400
    # A nonexistent business plan is rejected.
    When I change the subscription business plan of "subscriptionId" to "INVALID_BUSINESS_PLAN"
    Then The response status code should be 400
    # A plan not among the API's allowed tiers (the API offers Gold/Bronze/Unlimited; Silver is a global default it does not offer).
    When I change the subscription business plan of "subscriptionId" to "Silver"
    Then The response status code should be 400

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
