@cleanup
Feature: Admin Throttling Policy CRUD

  Admin-plane CRUD of throttling policies via the admin REST API, across all policy types — application,
  subscription, advanced (API-level), and custom (Siddhi). Covers create (request-count + bandwidth limit
  types; advanced conditional groups), retrieve, update, delete, and the not-found (404) edge, plus listing.
  Ports the CRUD of the backend Application/Subscription/Advanced/Custom ThrottlingPolicyTestCase (+ the CRUD
  half of the restart-family policy tests). Enforcement (429) is covered by gateway/throttling_enforcement.
  Application/subscription/advanced run ×2 tenant (tenant admins manage their own tiers); custom rules are an
  admin-global feature that WSO2 APIM supports for the super tenant ONLY, so the custom scenarios are
  deliberately super-tenant only. Each scenario uses uniquely-named policies (parallel-safe) and
  cleans them up. Duplicate-name (409) and delete-of-an-in-use advanced policy (403) are covered below;
  export/import is in throttle_policy_export_import.feature. Deferred to a later increment: advanced
  op↔API-level enforcement (gateway) + cross-admin permission, subscription permission-visibility.

  @cap:admin @feat:throttling-policies @type:regression @legacy:ApplicationThrottlingPolicyTestCase @legacy:ApplicationThrottlingPolicyServerRestartTestCase
  Scenario Outline: Application throttling policy CRUD as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an application throttling policy "${UNIQUE:appCrud}" allowing 20 requests per minute
    Then The response status code should be 201
    And The response should contain "REQUESTCOUNTLIMIT"
    When I retrieve the "application" throttling policy with id "appThrottlePolicyId"
    Then The response status code should be 200
    And The response should contain "{{appThrottlePolicyName}}"
    And The response should contain "REQUESTCOUNTLIMIT"
    When I update the "application" throttling policy "appThrottlePolicyId" setting its description to "updated application policy"
    Then The response status code should be 200
    And The response should contain "updated application policy"
    # Bandwidth limit-type variant.
    When I create an application throttling policy "${UNIQUE:appCrudBw}" allowing 5 KB per minute
    Then The response status code should be 201
    And The response should contain "BANDWIDTHLIMIT"
    # Delete, then confirm it is gone via a GET → 404 (parity with the legacy CRUD assertion).
    When I delete the "application" throttling policy with id "appThrottlePolicyId"
    Then The response status code should be 200
    When I retrieve the "application" throttling policy with id "appThrottlePolicyId"
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:throttling-policies @type:regression @legacy:SubscriptionThrottlingPolicyTestCase
  Scenario Outline: Subscription throttling policy CRUD as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a subscription throttling policy "${UNIQUE:subCrud}" allowing 20 requests per minute
    Then The response status code should be 201
    And The response should contain "REQUESTCOUNTLIMIT"
    When I retrieve the "subscription" throttling policy with id "subThrottlePolicyId"
    Then The response status code should be 200
    And The response should contain "{{subThrottlePolicyName}}"
    When I update the "subscription" throttling policy "subThrottlePolicyId" setting its description to "updated subscription policy"
    Then The response status code should be 200
    And The response should contain "updated subscription policy"
    When I create a subscription throttling policy "${UNIQUE:subCrudBw}" allowing 5 KB per minute
    Then The response status code should be 201
    And The response should contain "BANDWIDTHLIMIT"
    When I delete the "subscription" throttling policy with id "subThrottlePolicyId"
    Then The response status code should be 200
    When I delete the "subscription" throttling policy with id "subThrottlePolicyId"
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:throttling-policies @type:regression @legacy:AdvancedThrottlingPolicyTestCase
  Scenario Outline: Advanced (API-level) throttling policy CRUD as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an advanced throttling policy "${UNIQUE:advCrud}" allowing 20 requests per minute
    Then The response status code should be 201
    And The response should contain "REQUESTCOUNTLIMIT"
    When I retrieve the "advanced" throttling policy with id "advThrottlePolicyId"
    Then The response status code should be 200
    And The response should contain "{{advThrottlePolicyName}}"
    When I update the "advanced" throttling policy "advThrottlePolicyId" setting its description to "updated advanced policy"
    Then The response status code should be 200
    And The response should contain "updated advanced policy"
    # Bandwidth + conditional-group limit-type variants.
    When I create an advanced throttling policy "${UNIQUE:advCrudBw}" allowing 5 KB per minute
    Then The response status code should be 201
    And The response should contain "BANDWIDTHLIMIT"
    When I create an advanced throttling policy "${UNIQUE:advCrudCond}" allowing 20 requests per minute with a header conditional group
    Then The response status code should be 201
    And The response should contain "HEADERCONDITION"
    When I delete the "advanced" throttling policy with id "advThrottlePolicyId"
    Then The response status code should be 200
    When I delete the "advanced" throttling policy with id "advThrottlePolicyId"
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # SUPER-TENANT ONLY BY PRODUCT DESIGN — not a missing tenant variant, do not "fix" this by adding a
  # ×2-tenant Examples table. Custom (Siddhi) throttling rules are an admin-global capability that APIM
  # supports for the super tenant only; a tenant admin is refused (403) when creating one. That 403 is
  # deliberately not asserted — if you want it pinned, add its own @type:negative scenario.
  @cap:admin @feat:throttling-policies @type:regression @legacy:CustomThrottlingPolicyTestCase @legacy:CustomThrottlingPolicyServerRestartTestCase
  Scenario: Custom (Siddhi) throttling rule CRUD
    Given The system is ready
    And I have valid access tokens as "admin"
    When I create a custom throttling policy "${UNIQUE:customCrud}" throttling API context "crudDummyContext" after 10 requests per minute
    Then The response status code should be 201
    And The response should contain "siddhiQuery"
    When I retrieve the "custom" throttling policy with id "customThrottlePolicyId"
    Then The response status code should be 200
    And The response should contain "{{customThrottlePolicyName}}"
    When I update the "custom" throttling policy "customThrottlePolicyId" setting its description to "updated custom rule"
    Then The response status code should be 200
    And The response should contain "updated custom rule"
    When I delete the "custom" throttling policy with id "customThrottlePolicyId"
    Then The response status code should be 200
    # Confirm it is gone via a GET → 404 (parity with the legacy CRUD assertion).
    When I retrieve the "custom" throttling policy with id "customThrottlePolicyId"
    Then The response status code should be 404

  @cap:admin @feat:throttling-policies @type:regression @legacy:GetThrottlingPoliciesTestCase
  Scenario Outline: List throttling policies and confirm the built-in defaults are present as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I retrieve all "subscription" throttling policies
    Then The response status code should be 200
    And The response should contain "Unlimited"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # --- Duplicate-name → 409 (increment-2 Group A). Create, then re-create with the captured name. ---
  @cap:admin @feat:throttling-policies @type:negative @legacy:ApplicationThrottlingPolicyTestCase
  Scenario Outline: Creating an application throttling policy with an existing name is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an application throttling policy "dupApp${UNIQUE:P}" allowing 1000 requests per minute
    Then The response status code should be 201
    When I create an application throttling policy "{{appThrottlePolicyName}}" allowing 1000 requests per minute
    Then The response status code should be 409

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:throttling-policies @type:negative @legacy:SubscriptionThrottlingPolicyTestCase
  Scenario Outline: Creating a subscription throttling policy with an existing name is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a subscription throttling policy "dupSub${UNIQUE:P}" allowing 1000 requests per minute
    Then The response status code should be 201
    When I create a subscription throttling policy "{{subThrottlePolicyName}}" allowing 1000 requests per minute
    Then The response status code should be 409

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:throttling-policies @type:negative @legacy:AdvancedThrottlingPolicyTestCase
  Scenario Outline: Creating an advanced throttling policy with an existing name is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an advanced throttling policy "dupAdv${UNIQUE:P}" allowing 1000 requests per minute
    Then The response status code should be 201
    When I create an advanced throttling policy "{{advThrottlePolicyName}}" allowing 1000 requests per minute
    Then The response status code should be 409

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Super-tenant only by product design (a tenant admin is refused 403); intentionally no tenant row.
  @cap:admin @feat:throttling-policies @type:negative @legacy:CustomThrottlingPolicyTestCase
  Scenario: Creating a custom throttling policy with an existing name is rejected
    Given The system is ready
    And I have valid access tokens as "admin"
    When I create a custom throttling policy "dupCustom${UNIQUE:P}" throttling API context "/dc${UNIQUE:C}" after 1000 requests per minute
    Then The response status code should be 201
    When I create a custom throttling policy "{{customThrottlePolicyName}}" throttling API context "/dc2${UNIQUE:C}" after 1000 requests per minute
    Then The response status code should be 409

  # --- Delete-of-an-in-use advanced policy → 403 (increment-2 Group C #5). ---
  @cap:admin @feat:throttling-policies @type:negative @dep:publisher @legacy:AdvancedThrottlingPolicyTestCase
  Scenario Outline: An advanced throttling policy assigned to an API cannot be deleted as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an advanced throttling policy "inUse${UNIQUE:P}" allowing 1000 requests per minute
    Then The response status code should be 201
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "inUseApiId" and deployed it
    When I retrieve the "apis" resource with id "inUseApiId"
    And I put the response payload in context as "inUseApiPayload"
    And I update the "apis" resource "inUseApiId" and "inUseApiPayload" with configuration type "apiThrottlingPolicy" and value:
    """
    {{advThrottlePolicyName}}
    """
    Then The response status code should be 200
    When I delete the "advanced" throttling policy with id "advThrottlePolicyId"
    Then The response status code should be 403
    # Un-assign by deleting the API so the (registered) policy can then be cleaned up.
    When I delete the "apis" resource with id "inUseApiId"
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Cross-admin advanced-policy delete (increment-2 Group B) — a second admin can delete a policy created by
  # another admin (200): admin management is not owner-scoped. Provisions a 2nd admin user per tenant. x2 tenant.
  # Ports AdvancedThrottlingPolicyTestCase #11.
  @cap:admin @feat:throttling-policies @type:regression @legacy:AdvancedThrottlingPolicyTestCase
  Scenario Outline: An advanced throttling policy created by one admin can be deleted by another admin in <tenant>
    Given The system is ready
    And I provision user "policyAdmin2" with roles "admin" in tenant "<tenant>"
    And I have valid access tokens as "<admin1>"
    When I create an advanced throttling policy "crossAdmin${UNIQUE:P}" allowing 1000 requests per minute
    Then The response status code should be 201
    And I have valid access tokens as "<admin2>"
    When I delete the "advanced" throttling policy with id "advThrottlePolicyId"
    Then The response status code should be 200

    Examples:
      | tenant       | admin1            | admin2                   |
      | carbon.super | admin             | policyAdmin2             |
      | tenant1.com  | admin@tenant1.com | policyAdmin2@tenant1.com |

  # Role-restricted subscription tier (increment-2 Group B) — a tier ALLOW-restricted to a role cannot be used to
  # subscribe by a user outside that role (403 "Tier … is not allowed"). subscriberUser (Internal/subscriber)
  # lacks Internal/creator. Spans admin (policy) + publisher (API) + devportal (subscribe). x2 tenant. Ports
  # SubscriptionThrottlingPolicyTestCase#testCheckPolicyPermission.
  @cap:admin @feat:throttling-policies @type:negative @dep:publisher @dep:devportal @legacy:SubscriptionThrottlingPolicyTestCase
  Scenario Outline: A subscription tier restricted to a role cannot be used by a user outside that role as <adminActor>
    Given The system is ready
    And I have valid access tokens as "<adminActor>"
    When I create a subscription throttling policy "restrictedTier${UNIQUE:P}" allowing 1000 requests per minute restricted to role "Internal/creator"
    Then The response status code should be 201
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "b2ApiId" and deployed it
    When I retrieve the "apis" resource with id "b2ApiId"
    And I put the response payload in context as "b2ApiPayload"
    And I update the "apis" resource "b2ApiId" and "b2ApiPayload" with configuration type "policies" and value:
    """
    ["Unlimited","{{subThrottlePolicyName}}"]
    """
    Then The response status code should be 200
    When I publish the "apis" resource with id "b2ApiId"
    Then The lifecycle status of API "b2ApiId" should be "Published"
    And The system is ready and I have valid devportal access token as "<subscriberActor>"
    And I create an application "${UNIQUE:B2App}" with visibility "PRIVATE" as "b2AppId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "b2Sub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "{{subThrottlePolicyName}}"}
    """
    And I attempt to subscribe to API "b2ApiId" using application "b2AppId" with payload "b2Sub"
    Then The response status code should be 403

    Examples:
      | adminActor        | subscriberActor            |
      | admin             | subscriberUser             |
      | admin@tenant1.com | subscriberUser@tenant1.com |

  # The THREE streaming rate-limiting types, all of which are subscription-plan-only (docs-apim
  # administer/rate-limiting/manage-subscription-policies#streaming-api-limits-in-subscription-policies and
  # api-design-manage/design/rate-limiting/set-streaming-api-limits): count-based, time-based and count-time hybrid.
  #
  # THEY ARE NOT THREE SCHEMAS — SETTLED FROM THE PRODUCT'S OWN MODEL, NOT ASSUMED. SubscriptionThrottlePolicyDTO
  # carries no duration/validity field at all (graphQLMaxComplexity, graphQLMaxDepth, defaultLimit, monetization,
  # rateLimitCount, rateLimitTimeUnit, customAttributes, stopOnQuotaReach, billingPlan, permissions, subscriberCount)
  # and EventCountLimitDTO carries only timeUnit/unitTime/eventCount. rateLimitCount/rateLimitTimeUnit are the
  # burst-rate pair (requests per second), NOT a subscription duration.
  #
  # So only TWO of the three docs types are policy-body configurations at all, and they are the SAME MECHANISM AT
  # DIFFERENT SCALES — one EventCountLimit triple, a large count over a long window versus a small count over a
  # short one — which is exactly how the product seeds its own streaming plans in
  # APIUtil.addDefaultTenantAsyncThrottlePolicies: AsyncGold/Silver/Bronze/Unlimited as unitTime=1, timeUnit="days"
  # with eventCounts {50000, 25000, 5000, Integer.MAX_VALUE}, and the AsyncWH* family the same with
  # timeUnit="months". They are therefore rows over ONE step here rather than separate scenarios.
  #
  # TIME-BASED IS NOT A POLICY FIELD. There is no lever on a subscription plan that bounds how long a subscription
  # may be held — the DTO field list above is the evidence. For WebSub it is the REGISTRATION-time hub.lease_seconds
  # parameter, covered live by gateway/websub_invocation's "stops being a subscription once its lease expires"
  # scenario. For WebSocket and SSE there is no lease_seconds analogue and no policy duration, so the time-based
  # type is NOT EXPRESSIBLE for those protocols — there is nothing to assert, which is why no row below claims it.
  # The 2147483647 row is the product's own AsyncUnlimited shape (an uncapped count over a bounded window); it is
  # NOT a time limit, since the window is only the accounting period the count resets on.
  #
  # Integer.MAX_VALUE (2147483647) is the product's own "unlimited" sentinel for an event count, not a magic number
  # of the test's choosing — it is the value APIUtil seeds AsyncUnlimited and AsyncWHUnlimited with. timeUnit is
  # passed through UNVALIDATED by the admin REST layer (CommonThrottleMappingUtil just copies it), so the product's
  # own spellings "days"/"months" are used; note they disagree with admin-api.yaml's singular "day" examples, which
  # is the product's inconsistency and is why the read-back is asserted verbatim rather than normalised.
  @cap:admin @feat:throttling-policies @rule:streaming-limit-types @type:regression @legacy:ThrottlingTestCase
  Scenario Outline: A streaming subscription plan expresses the <limitType> streaming limit as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a subscription throttling policy "${UNIQUE:strmLimit}" allowing <eventCount> events per <unitTime> "<timeUnit>"
    Then The response status code should be 201
    When I retrieve the "subscription" throttling policy with id "subThrottlePolicyId"
    Then The response status code should be 200
    And The value of response field "policyName" should be "{{subThrottlePolicyName}}"
    And The value of response field "defaultLimit.type" should be "EVENTCOUNTLIMIT"
    And The value of response field "defaultLimit.eventCount.eventCount" should be "<eventCount>"
    And The value of response field "defaultLimit.eventCount.unitTime" should be "<unitTime>"
    And The value of response field "defaultLimit.eventCount.timeUnit" should be "<timeUnit>"

    Examples:
      # count-based     : a large count over a long window — the cap is what bites ("1M total events a month")
      # hybrid          : the same triple at a short scale — the docs' own "1M events within a day"
      # unlimited-count : the AsyncUnlimited shape, uncapped count over a bounded window. Pinned because the
      #                   read-back proves the admin layer neither rejects nor clamps the product's own sentinel.
      | limitType       | eventCount | unitTime | timeUnit | actor             |
      | count-based     | 1000000    | 1        | months   | admin             |
      | hybrid          | 1000000    | 1        | days     | admin             |
      | unlimited-count | 2147483647 | 7        | days     | admin             |
      | count-based     | 1000000    | 1        | months   | admin@tenant1.com |
      | hybrid          | 1000000    | 1        | days     | admin@tenant1.com |
      | unlimited-count | 2147483647 | 7        | days     | admin@tenant1.com |

  # A WEBSUB-family streaming plan carries a FOURTH limit the other protocols have no analogue for: subscriberCount,
  # the number of concurrent callback registrations one subscription may hold ("Active Subscriptions" in the docs'
  # per-protocol event-counting table). It sits alongside the event quota rather than replacing it, which is how the
  # product seeds AsyncWHGold ("10000 events per month and 1000 active subscriptions"). Asserted here as a plan
  # SHAPE; that the cap is ENFORCED is asserted at runtime by gateway/websub_invocation's subscription-count
  # scenario, which is the one streaming limit this profile enforces (it is counted from
  # AM_POLICY_SUBSCRIPTION.CONNECTIONS_COUNT by WebhooksDAO, not through the traffic manager).
  @cap:admin @feat:throttling-policies @rule:streaming-limit-types @type:regression @legacy:ThrottlingTestCase
  Scenario Outline: A WebSub streaming plan expresses an active-subscription cap alongside its event quota as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a subscription throttling policy "${UNIQUE:strmWhLimit}" allowing 10000 events per 1 "months" and at most 1000 webhook subscriptions
    Then The response status code should be 201
    When I retrieve the "subscription" throttling policy with id "subThrottlePolicyId"
    Then The response status code should be 200
    And The value of response field "defaultLimit.type" should be "EVENTCOUNTLIMIT"
    And The value of response field "defaultLimit.eventCount.eventCount" should be "10000"
    And The value of response field "defaultLimit.eventCount.timeUnit" should be "months"
    And The value of response field "subscriberCount" should be "1000"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # An ADVANCED (API-level) policy can also carry an EVENTCOUNTLIMIT, and this is the admin CRUD surface for it —
  # deliberately NOT presented as streaming coverage. Streaming limits are subscription-plan-only (see the note
  # above), so an API-level event quota is not the lever a streaming API is rate-limited with; an earlier revision of
  # gateway/sse_invocation attached one via apiThrottlingPolicy and measured nothing meaningful as a result. It is
  # covered here because it IS a shipped admin capability and because the shape is easy to get wrong: the legacy
  # fixture streamingAPIs/serverSentEventsTest/policy.json declares "type":"EVENTCOUNTLIMIT" with the value under
  # requestCount (its Java re-read the scalars into an EventCountLimitDTO before sending), and posting that shape
  # verbatim is rejected with 500 "Error while adding an Advanced level policy". The pairing the admin API actually
  # requires — EVENTCOUNTLIMIT with an eventCount object — is what this asserts.
  @cap:admin @feat:throttling-policies @rule:streaming-limit-types @type:regression @legacy:ServerSentEventsAPITestCase
  Scenario Outline: An advanced policy can carry an event-count limit as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an advanced throttling policy "${UNIQUE:advEvQuota}" allowing 2 events per minute
    Then The response status code should be 201
    When I retrieve the "advanced" throttling policy with id "advThrottlePolicyId"
    Then The response status code should be 200
    And The value of response field "policyName" should be "{{advThrottlePolicyName}}"
    And The value of response field "defaultLimit.type" should be "EVENTCOUNTLIMIT"
    And The value of response field "defaultLimit.eventCount.eventCount" should be "2"
    And The value of response field "defaultLimit.eventCount.timeUnit" should be "min"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
