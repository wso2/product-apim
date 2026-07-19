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

  # A "broken" API (a subscribed API whose visibility role is changed away from the subscriber's role, so the
  # subscriber can no longer see it) must NOT break the subscriber's whole subscription list — the other (healthy)
  # subscription is still returned. Ports APIMANAGER4373BrokenAPIInStoreTestCase (regression for wso2/APIMANAGER-4373).
  @cap:devportal @feat:subscription-management @rule:broken-api-resilience @type:regression @dep:publisher @legacy:APIMANAGER4373BrokenAPIInStoreTestCase
  Scenario Outline: A broken subscribed API does not break the subscription list in <tenant>
    Given The system is ready
    And I have valid access tokens as "admin<suffix>"
    And I generate a unique value and store it as "brrole"
    And I generate a unique value and store it as "brotherrole"
    And I provision store-visibility role "{{brrole}}" in tenant "<tenant>"
    And I provision store-visibility role "{{brotherrole}}" in tenant "<tenant>"
    And I provision user "brSub" with roles "Internal/subscriber,{{brrole}}" in tenant "<tenant>"
    And The system is ready and I have valid devportal access token as "brSub<suffix>"

    # Admin authors two restricted-visibility APIs both visible to the subscriber's role, and publishes them.
    Given I act as "admin<suffix>"
    When I have created an api from "artifacts/payloads/create_apim_test_api.json" with restricted visibility for roles "{{brrole}}" as "brBrokenApi" and deployed it
    And I publish the "apis" resource with id "brBrokenApi"
    Then The lifecycle status of API "brBrokenApi" should be "Published"
    When I have created an api from "artifacts/payloads/create_apim_test_api.json" with restricted visibility for roles "{{brrole}}" as "brHealthyApi" and deployed it
    And I publish the "apis" resource with id "brHealthyApi"
    Then The lifecycle status of API "brHealthyApi" should be "Published"
    When I retrieve the "apis" resource with id "brHealthyApi"
    And I extract response field "name" and store it as "brHealthyApiName"

    # The subscriber creates an application and subscribes to both APIs.
    When I act as "brSub<suffix>"
    And I create an application "${UNIQUE:BrokenApp}" with visibility "PRIVATE" as "brAppId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "brSubBroken"
    """
    {"applicationId": "{{brAppId}}", "apiId": "{{brBrokenApi}}", "throttlingPolicy": "Gold"}
    """
    And I subscribe to API "brBrokenApi" using application "brAppId" with payload "brSubBroken" as "brBrokenSubId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "brSubHealthy"
    """
    {"applicationId": "{{brAppId}}", "apiId": "{{brHealthyApi}}", "throttlingPolicy": "Gold"}
    """
    And I subscribe to API "brHealthyApi" using application "brAppId" with payload "brSubHealthy" as "brHealthySubId"
    Then The response status code should be 201

    # Admin changes the broken API's visibility to a role the subscriber does NOT hold, breaking their access to it.
    When I act as "admin<suffix>"
    And I set the visibility roles of API "brBrokenApi" to "{{brotherrole}}"
    Then The response status code should be 200

    # The subscriber's full subscription list still returns the healthy API (the broken one no longer resolving
    # must not fail the whole listing). Poll until the state settles.
    When I act as "brSub<suffix>"
    And I retrieve all subscriptions of application "brAppId"
    Then The response status code should be 200
    And The response should contain "{{brHealthyApiName}}"

    Examples:
      | tenant       | suffix       |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

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

  # Listing subscriptions by application id and by API id. Ports APIM710AllSubscriptionsByApplicationTestCase.
  # Topology: two published APIs and two applications, cross-subscribed so both listings return exactly two:
  #   app1 -> api1, app1 -> api2   (by application: app1 has 2 subscriptions)
  #   app2 -> api1                 (by API: api1 has 2 subscriptions, across app1 + app2)
  # Runs as the tenant admin in both tenants (spans the provider plane to publish and the consumer plane to
  # subscribe). Subscriptions cascade with their applications on teardown.
  @cap:devportal @feat:subscription-management @rule:listing @type:regression @dep:publisher @legacy:APIM710AllSubscriptionsByApplicationTestCase
  Scenario Outline: List subscriptions by application id and by API id as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # Two published APIs.
    When I have created an api from "artifacts/payloads/create_apim_test_api.json" as "listApi1" and deployed it
    And I publish the "apis" resource with id "listApi1"
    Then The lifecycle status of API "listApi1" should be "Published"
    When I have created an api from "artifacts/payloads/create_apim_test_api.json" as "listApi2" and deployed it
    And I publish the "apis" resource with id "listApi2"
    Then The lifecycle status of API "listApi2" should be "Published"

    # Two applications.
    When I create an application "${UNIQUE:SubListApp1}" with visibility "PUBLIC" as "listApp1"
    Then The response status code should be 201
    When I create an application "${UNIQUE:SubListApp2}" with visibility "PUBLIC" as "listApp2"
    Then The response status code should be 201

    # Cross-subscribe: app1 -> api1, app1 -> api2, app2 -> api1.
    When I put the following JSON payload in context as "subApp1Api1Payload"
    """
    {"applicationId": "{{listApp1}}", "apiId": "{{listApi1}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "listApi1" using application "listApp1" with payload "subApp1Api1Payload" as "subApp1Api1"
    Then The response status code should be 201
    When I put the following JSON payload in context as "subApp1Api2Payload"
    """
    {"applicationId": "{{listApp1}}", "apiId": "{{listApi2}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "listApi2" using application "listApp1" with payload "subApp1Api2Payload" as "subApp1Api2"
    Then The response status code should be 201
    When I put the following JSON payload in context as "subApp2Api1Payload"
    """
    {"applicationId": "{{listApp2}}", "apiId": "{{listApi1}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "listApi1" using application "listApp2" with payload "subApp2Api1Payload" as "subApp2Api1"
    Then The response status code should be 201

    # By application id: app1 has exactly two subscriptions (to api1 and api2).
    When I retrieve all subscriptions of application "listApp1"
    Then The response status code should be 200
    And The subscription list should contain exactly 2 subscriptions
    And The subscription with id "subApp1Api1" should be in the list of all subscriptions
    And The subscription with id "subApp1Api2" should be in the list of all subscriptions

    # By API id: api1 has exactly two subscriptions (from app1 and app2).
    When I retrieve all subscriptions of api "listApi1"
    Then The response status code should be 200
    And The subscription list should contain exactly 2 subscriptions
    And The subscription with id "subApp1Api1" should be in the list of all subscriptions
    And The subscription with id "subApp2Api1" should be in the list of all subscriptions

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
