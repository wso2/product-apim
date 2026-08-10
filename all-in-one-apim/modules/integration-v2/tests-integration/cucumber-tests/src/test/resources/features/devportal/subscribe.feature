Feature: DevPortal Subscribe

  DevPortal-plane subscription: a consumer (subscriber-role) creates an application and subscribes it to a
  published API, then confirms the subscription exists. Runs in both the super tenant and tenant1.com. The
  published API for each tenant is provided by _setup_published_apis (listed first in the runner, created as
  that tenant's admin) and shared via tenant-qualified keys. The subscribe itself is performed as the
  subscriber consumer — the genuine role-distinct path (a subscriber can subscribe). Teardown is the
  runner's AfterClass sweep (the subscription cascades when its application is deleted).

  # The THIRD row runs the same round trip as a SECONDARY.COM user-store consumer (CLAUDE.md §12), closing the
  # legacy SUPER_TENANT_USER_STORE_USER mode for the subscriber-identity facet. It is a real identity probe rather
  # than a repeat: the application and the subscription are both owned by the calling principal, and the closing
  # read ("the subscription ... should be in the list of all subscriptions") is a devportal read scoped to that
  # principal — so if a store-qualified username failed to resolve to the same subscriber on the write and the
  # read, the list would come back without it. It stays in the super tenant because the store domain, not the
  # tenant, is the variable; the row above already varies the tenant.
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
    And I subscribe to API "publishedApiId<tenantSuffix>" using application "createdAppId" with payload "apiSubscriptionPayload" as "createdSubscriptionId"

    # Confirm the subscription exists. The id asserted here is the one captured AT SUBSCRIBE TIME, under its own
    # key: the retrieve step publishes the application's subscription list and also writes the id it found into
    # "subscriptionId", so asserting THAT key would take the id from the list and then look for it in the same
    # list — circular, and unable to fail. Comparing against the subscribe-time id is what actually proves the
    # subscription the product created is the one it now returns.
    When I retrieve the subscription for Api "publishedApiId<tenantSuffix>" by Application "createdAppId"
    Then The response status code should be 200
    And The subscription with id "createdSubscriptionId" should be in the list of all subscriptions

    Examples:
      | actor                          | tenantSuffix |
      | subscriberUser                 |              |
      | subscriberUser@tenant1.com     | @tenant1.com |
      | SECONDARY.COM/subscriberUser1  |              |

  # Ports the subscription half of AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase plus the store-
  # visibility facet that class left entirely commented out. A DEPRECATED API accepts NO new subscription; it also
  # drops out of the devportal SEARCH under the shipped default config while remaining readable BY ID, so existing
  # subscribers keep their access. Both halves of that split are asserted at the end of the scenario.
  #
  # Legacy's refusal assertion was CONFOUNDED: it re-used the one application that was ALREADY subscribed to the
  # API before the deprecation, so the null return it asserted is equally explained by a duplicate-subscription
  # rejection (409) and says nothing about the lifecycle state. Here the refusal is measured with a SECOND, FRESH
  # application that has never been subscribed, so only the DEPRECATED state can explain it — and the first
  # application's successful subscribe while the API was still PUBLISHED is the positive control in the same
  # scenario, proving the API/tier/actor combination is subscribable in principle.
  #
  # This API is created inline rather than reusing this runner's shared _setup_published_apis fixture: deprecating
  # that shared API would break every sibling scenario in the runner. Torn down by the runner's AfterClass sweep.
  @cap:devportal @feat:subscribe @type:negative @dep:publisher @legacy:AccessibilityOfDeprecatedOldAPIAndPublishedCopyAPITestCase
  Scenario Outline: A deprecated API refuses a new subscription and leaves the devportal search but stays readable by id (<tenant>)
    Given The system is ready
    And I have valid access tokens as "admin<tenantSuffix>"
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "depSubApiId" and deployed it
    When I retrieve the "apis" resource with id "depSubApiId"
    Then The response status code should be 200
    And I extract response field "name" and store it as "depSubApiName"
    When I publish the "apis" resource with id "depSubApiId"
    Then The lifecycle status of API "depSubApiId" should be "Published"
    # BASELINE for the discoverability pair asserted at the end of this scenario: while PUBLISHED, the API IS
    # returned by a devportal name search. Without this control, the count-0 assertion after the deprecation could
    # equally be explained by the API never having been indexed at all.
    When I search DevPortal APIs with query "{{depSubApiName}}" and limit 25 until the result count is 1 within 60 seconds

    # POSITIVE CONTROL — while the API is PUBLISHED, a fresh consumer application subscribes successfully.
    Given The system is ready and I have valid devportal access token as "subscriberUser<tenantSuffix>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "depSubApp1"
    And I create an application with payload "depSubApp1"
    Then The response status code should be 201
    And I extract response field "applicationId" and store it as "depSubApp1Id"
    When I put the following JSON payload in context as "depSubPayload1"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "depSubApiId" using application "depSubApp1Id" with payload "depSubPayload1" as "depSubId1"
    Then The response status code should be 201

    # Deprecate the API.
    Given I act as "admin<tenantSuffix>"
    When I change the lifecycle of API "depSubApiId" with action "Deprecate"
    Then The response status code should be 200
    And The lifecycle status of API "depSubApiId" should be "Deprecated"

    # NEGATIVE — a SECOND, never-subscribed application cannot subscribe to the now-DEPRECATED API.
    Given The system is ready and I have valid devportal access token as "subscriberUser<tenantSuffix>"
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "depSubApp2"
    And I create an application with payload "depSubApp2"
    Then The response status code should be 201
    And I extract response field "applicationId" and store it as "depSubApp2Id"
    When I put the following JSON payload in context as "depSubPayload2"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I attempt to subscribe to API "depSubApiId" using application "depSubApp2Id" with payload "depSubPayload2"
    # PRODUCT FINDING (pinned as observed, NOT as it should be): the refusal is real but the status is wrong.
    # APIConsumerImpl.addSubscription throws APIMgtResourceNotFoundException("Subscriptions not allowed on
    # APIs/API Products in the state: DEPRECATED") constructed WITHOUT an ErrorHandler, and the devportal's
    # SubscriptionsApiServiceImpl does not catch it — so GlobalThrowableMapper finds no error handler and falls
    # through to its generic 500 envelope instead of the 404 the exception type intends. Asserted exactly here
    # so the day the product attaches an ErrorHandler this test fails and is updated deliberately.
    Then The response status code should be 500
    # The decisive assertion, independent of the status code: the FRESH application ended up with NO subscription.
    # Queried by APPLICATION, not by API — the devportal's GET /subscriptions IGNORES applicationId whenever apiId
    # is also supplied (SubscriptionsApiServiceImpl.subscriptionsGet branches `if (apiId) … else if
    # (applicationId)`), so an api-scoped query returns the FIRST application's subscription and would read as a
    # false failure here. Verified live.
    When I retrieve all subscriptions of application "depSubApp2Id"
    Then The response status code should be 200
    And The response should contain "\"count\":0"

    # VISIBILITY — deprecation splits the devportal's two read paths, and BOTH halves are pinned here. This is the
    # facet legacy left entirely commented out.
    #
    # (1) Still SERVED BY ID to the consumer, carrying its DEPRECATED lifecycle status: deprecation does not
    #     withdraw the API from existing subscribers, who must keep reading its definition and invoking it.
    When I retrieve the devportal API "depSubApiId" until it contains "DEPRECATED" within 60 seconds
    Then The response status code should be 200
    And The value of response field "lifeCycleStatus" should be "DEPRECATED"
    # (2) But NOT DISCOVERABLE by search — verified behaviour under the shipped default configuration, contrasted
    #     against the count-1 baseline taken above while the same API was PUBLISHED. Mechanism:
    #     RegistrySearchUtil.getDevPortalSearchQuery appends `lcState=(PUBLISHED OR PROTOTYPED)` to EVERY devportal
    #     query, and only adds DEPRECATED to that list when APIUtil.isAllowDisplayAPIsWithMultipleStatus() is true —
    #     i.e. api-manager.xml <APIStore><DisplayAllAPIs>, deployment.toml `apim.devportal.display_deprecated_apis`,
    #     which ships as false. So the "deprecated APIs stay in the store" facet legacy tried to assert is
    #     config-gated, NOT unconditional; asserting count 1 here would be asserting a non-default configuration.
    #     The display_deprecated_apis=true direction needs a toml overlay block and is left as a follow-up (§13);
    #     the false direction — the shipped default, the one users actually get — is closed exactly, here.
    When I search DevPortal APIs with query "{{depSubApiName}}" and limit 25 until the result count is 0 within 60 seconds

    Examples:
      | tenant       | tenantSuffix |
      | carbon.super |              |
      | tenant1.com  | @tenant1.com |

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
