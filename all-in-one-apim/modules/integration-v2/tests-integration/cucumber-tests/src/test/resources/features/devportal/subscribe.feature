Feature: DevPortal Subscribe

  DevPortal-plane subscription: a consumer (subscriber-role) creates an application and subscribes it to a
  published API, then confirms the subscription exists. Runs in both the super tenant and tenant1.com. The
  published API for each tenant is provided by _setup_published_apis (listed first in the runner, created as
  that tenant's admin) and shared via tenant-qualified keys. The subscribe itself is performed as the
  subscriber consumer — the genuine role-distinct path (a subscriber can subscribe). Teardown is the
  runner's AfterClass sweep (the subscription cascades when its application is deleted).

  @cap:devportal @feat:subscribe @type:smoke @legacy:SubscriptionTestCase
  Scenario Outline: Subscribe an application to a published API as <actor>
    Given The system is ready and I have valid devportal access token as "<actor>"

    # Create the consumer application
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201

    # Subscribe the application to the tenant's published API
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {
      "applicationId": "{{applicationId}}",
      "apiId": "{{apiId}}",
      "throttlingPolicy": "Unlimited"
    }
    """
    And I subscribe to API "publishedApiId<tenantSuffix>" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"

    # Confirm the subscription exists
    When I retrieve the subscription for Api "publishedApiId<tenantSuffix>" by Application "createdAppId"
    Then The response status code should be 200
    And The subscription with id "subscriptionId" should be in the list of all subscriptions

    Examples:
      | actor                       | tenantSuffix |
      | subscriberUser              |              |
      | subscriberUser@tenant1.com  | @tenant1.com |

  @cap:devportal @feat:subscribe @type:negative @legacy:SubscriptionTestCase
  Scenario Outline: A publisher-role user without subscribe scope cannot subscribe as <actor>
    # Create the application as the consumer, then re-authenticate as a publisher-role user whose token lacks
    # the apim:subscribe scope and confirm the subscribe is rejected.
    Given The system is ready and I have valid devportal access token as "subscriberUser<tenantSuffix>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201

    Given The system is ready and I have valid publisher access tokens as "publisherUser<tenantSuffix>"
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I attempt to subscribe to API "publishedApiId<tenantSuffix>" using application "createdAppId" with payload "apiSubscriptionPayload"
    Then The response status code should be 401
    # Switch back so @cleanup deletes the subscriber-owned application with the subscriber's token.
    And I act as "subscriberUser<tenantSuffix>"

    Examples:
      | tenantSuffix |
      |              |
      | @tenant1.com |
