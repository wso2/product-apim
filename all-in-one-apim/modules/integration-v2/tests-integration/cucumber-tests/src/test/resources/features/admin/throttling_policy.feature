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

  # Custom (Siddhi) rules are GLOBAL, so every operation on them is guarded by an explicit super-tenant check in
  # ThrottlingApiServiceImpl#checkTenantDomainForCustomRules — a tenant admin holding a valid apim:admin token is
  # refused 403 (NOT the 401 a missing scope produces). Legacy CustomThrottlingPolicyTestCase asserted this only
  # inside a catch block that never had to run, so neither outcome was pinned; each operation is pinned here.
  # A well-formed random UUID is enough for get/update/delete: the tenant guard fires before any id lookup, so a
  # real policy id is not needed (and a tenant admin cannot create one to get one).
  @cap:admin @feat:throttling-policies @type:negative @legacy:CustomThrottlingPolicyTestCase
  Scenario: Every custom (Siddhi) throttling rule operation is refused to a tenant admin
    Given The system is ready
    And I have valid access tokens as "admin@tenant1.com"
    When I create a custom throttling policy "tenantCustom${UNIQUE:P}" throttling API context "/tc${UNIQUE:C}" after 1000 requests per minute
    Then The response status code should be 403
    When I retrieve all "custom" throttling policies
    Then The response status code should be 403
    When I generate a random UUID and store it as "tenantCustomPolicyId"
    And I retrieve the "custom" throttling policy with id "tenantCustomPolicyId"
    Then The response status code should be 403
    When I attempt to update the custom throttling policy "tenantCustomPolicyId" with description "tenant update attempt"
    Then The response status code should be 403
    When I delete the "custom" throttling policy with id "tenantCustomPolicyId"
    Then The response status code should be 403

  # Multi-condition conditional group (ports AdvancedThrottlingPolicyTestCase#testAddPolicyWithConditionalGroups,
  # whose DTO round-trip covered FOUR condition types in ONE group). The CRUD scenario above only creates a
  # single HEADERCONDITION group, so the IP / query-parameter / JWT-claim condition shapes were never stored or
  # read back. Each condition's detail is asserted through a type-filtered path, so the assertion is exact and
  # independent of the order the server returns the conditions in.
  @cap:admin @feat:throttling-policies @type:regression @legacy:AdvancedThrottlingPolicyTestCase
  Scenario Outline: An advanced throttling policy round-trips a conditional group of all four condition types as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an advanced throttling policy "${UNIQUE:advFour}" allowing 50 requests per minute with a conditional group of all four condition types
    Then The response status code should be 201
    And The value of response field "policyName" should be "{{advThrottlePolicyName}}"
    And The value of response field "type" should be "AdvancedThrottlePolicy"
    And The value of response field "defaultLimit.type" should be "REQUESTCOUNTLIMIT"
    And The value of response field "defaultLimit.requestCount.requestCount" should be "50"
    # Read the stored policy back so the assertions prove persistence, not just the create echo.
    When I retrieve the "advanced" throttling policy with id "advThrottlePolicyId"
    Then The response status code should be 200
    # Count pinned alongside the value: the set comparison alone would pass if only ONE group survived.
    And The response array field "conditionalGroups[*].description" should have exactly 1 entries
    And The response field "conditionalGroups[*].description" should be exactly the list "conditional group"
    And The response field "conditionalGroups[0].conditions[*].type" should be exactly the list "IPCONDITION,HEADERCONDITION,QUERYPARAMETERCONDITION,JWTCLAIMSCONDITION"
    And The value of response field "conditionalGroups[0].limit.type" should be "REQUESTCOUNTLIMIT"
    And The value of response field "conditionalGroups[0].limit.requestCount.requestCount" should be "50"
    And The response field "conditionalGroups[0].conditions[?(@.type=='IPCONDITION')].ipCondition.ipConditionType" should be exactly the list "IPSPECIFIC"
    And The response field "conditionalGroups[0].conditions[?(@.type=='IPCONDITION')].ipCondition.specificIP" should be exactly the list "10.100.1.22"
    And The response field "conditionalGroups[0].conditions[?(@.type=='HEADERCONDITION')].headerCondition.headerName" should be exactly the list "Host"
    And The response field "conditionalGroups[0].conditions[?(@.type=='HEADERCONDITION')].headerCondition.headerValue" should be exactly the list "10.100.7.77"
    And The response field "conditionalGroups[0].conditions[?(@.type=='QUERYPARAMETERCONDITION')].queryParameterCondition.parameterName" should be exactly the list "claimUrl"
    And The response field "conditionalGroups[0].conditions[?(@.type=='QUERYPARAMETERCONDITION')].queryParameterCondition.parameterValue" should be exactly the list "claimAttribute"
    And The response field "conditionalGroups[0].conditions[?(@.type=='JWTCLAIMSCONDITION')].jwtClaimsCondition.claimUrl" should be exactly the list "name"
    And The response field "conditionalGroups[0].conditions[?(@.type=='JWTCLAIMSCONDITION')].jwtClaimsCondition.attribute" should be exactly the list "admin"
    # One per condition — without the count this passes if only one condition kept its invertCondition.
    And The response array field "conditionalGroups[0].conditions[*].invertCondition" should have exactly 4 entries
    And The response field "conditionalGroups[0].conditions[*].invertCondition" should be exactly the list "false"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:admin @feat:throttling-policies @type:regression @legacy:GetThrottlingPoliciesTestCase
  Scenario Outline: Searching all throttling policies returns the built-in defaults as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I retrieve all "subscription" throttling policies
    Then The response status code should be 200
    # Name-filtered, for the same reason as the type:all assertions below — and because "Unlimited" is a SUBSTRING
    # of AsyncUnlimited/AsyncWHUnlimited, so a containment check passes even when the built-in Unlimited is gone.
    And The response field "list[?(@.policyName=='Unlimited')].policyName" should be exactly the list "Unlimited"
    # The type:all search spans every policy type at once; assert one built-in default per type by an exact
    # name-filtered path (exactly one hit each), rather than a substring match that a similarly-named policy
    # created by a concurrent scenario could satisfy.
    When I search throttling policies with query "type:all"
    Then The response status code should be 200
    And The response field "list[?(@.policyName=='50PerMin')].policyName" should be exactly the list "50PerMin"
    And The response field "list[?(@.policyName=='Gold')].policyName" should be exactly the list "Gold"
    And The response field "list[?(@.policyName=='10KPerMin')].policyName" should be exactly the list "10KPerMin"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Full SubscriptionThrottlePolicy DTO round-trip (ports SubscriptionThrottlingPolicyTestCase
  # #testAddPolicyWithRequestCountLimit, which DTO-verified the whole business plan). The shared low-limit create
  # step used by the enforcement suites deliberately omits the GraphQL query-analysis limits, the subscriber
  # count and custom attributes — adding them there would change what those suites enforce — so the complete
  # business-plan shape is created and asserted here instead.
  @cap:admin @feat:throttling-policies @type:regression @legacy:SubscriptionThrottlingPolicyTestCase
  Scenario Outline: A subscription throttling policy round-trips its full business plan as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a subscription throttling policy "${UNIQUE:subFull}" allowing 50 requests per minute with the full business plan configuration
    Then The response status code should be 201
    When I retrieve the "subscription" throttling policy with id "subThrottlePolicyId"
    Then The response status code should be 200
    And The value of response field "policyName" should be "{{subThrottlePolicyName}}"
    And The value of response field "type" should be "SubscriptionThrottlePolicy"
    And The value of response field "defaultLimit.type" should be "REQUESTCOUNTLIMIT"
    And The value of response field "defaultLimit.requestCount.requestCount" should be "50"
    And The value of response field "defaultLimit.requestCount.timeUnit" should be "min"
    And The value of response field "defaultLimit.requestCount.unitTime" should be "1"
    And The value of response field "graphQLMaxComplexity" should be "400"
    And The value of response field "graphQLMaxDepth" should be "10"
    And The value of response field "rateLimitCount" should be "-1"
    And The value of response field "rateLimitTimeUnit" should be "NA"
    And The value of response field "stopOnQuotaReach" should be "false"
    And The value of response field "billingPlan" should be "COMMERCIAL"
    And The value of response field "subscriberCount" should be "0"
    And The value of response field "monetization.monetizationPlan" should be "DYNAMICRATE"
    # One attribute is created, and name/value are counted separately: an attribute that kept its name but lost
    # its value would leave the value path empty, which the set comparison alone reads as a match.
    And The response array field "customAttributes[*].name" should have exactly 1 entries
    And The response field "customAttributes[*].name" should be exactly the list "testAttribute"
    And The response array field "customAttributes[*].value" should have exactly 1 entries
    And The response field "customAttributes[*].value" should be exactly the list "testValue"
    And The value of response field "permissions.permissionType" should be "ALLOW"
    And The response field "permissions.roles" should be exactly the list "Internal/creator"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Deleting an application policy by a well-formed but unknown id is a 404 on the DELETE itself. The CRUD
  # scenario above only proves a GET after a successful delete is 404, which does not pin the DELETE's own
  # not-found behaviour. Ports ApplicationThrottlingPolicyTestCase#testDeletePolicyWithNonExistingPolicyId.
  @cap:admin @feat:throttling-policies @type:negative @legacy:ApplicationThrottlingPolicyTestCase
  Scenario Outline: Deleting an application throttling policy by an unknown id is not found as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I generate a random UUID and store it as "absentAppPolicyId"
    And I delete the "application" throttling policy with id "absentAppPolicyId"
    Then The response status code should be 404

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Cross-admin delete of an ASSIGNED advanced policy. Together with the two scenarios above this separates the
  # two possible causes of the refusal: an unassigned policy created by admin1 IS deletable by admin2 (200, above),
  # and an assigned policy is refused to its own creator (403, above) — so pinning the refusal for a DIFFERENT
  # admin here establishes that ASSIGNMENT, not ownership, is what blocks the delete. Ports
  # AdvancedThrottlingPolicyTestCase#testDeleteAssignedAPILevelAdvancedPolicyWithDifferentAdminUser (whose
  # try/catch had no Assert.fail, so a successful delete would have passed silently). The API is only CREATED,
  # not deployed: the in-use check reads the stored API record, so a gateway deployment adds nothing. The API and
  # policy are left to the cleanup hook, which deletes APIs before advanced policies (FK-safe) as their creator.
  @cap:admin @feat:throttling-policies @type:negative @dep:publisher @legacy:AdvancedThrottlingPolicyTestCase
  Scenario Outline: An advanced throttling policy assigned to an API cannot be deleted by a different admin in <tenant>
    Given The system is ready
    And I provision user "policyAdmin3" with roles "admin" in tenant "<tenant>"
    And I have valid access tokens as "<admin1>"
    When I create an advanced throttling policy "xAdminApi${UNIQUE:P}" allowing 1000 requests per minute
    Then The response status code should be 201
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "xAdminApiPayload"
    And I create an "apis" resource with payload "xAdminApiPayload" as "xAdminApiId"
    When I retrieve the "apis" resource with id "xAdminApiId"
    And I put the response payload in context as "xAdminApiFetched"
    And I update the "apis" resource "xAdminApiId" and "xAdminApiFetched" with configuration type "apiThrottlingPolicy" and value:
    """
    {{advThrottlePolicyName}}
    """
    Then The response status code should be 200
    And I have valid access tokens as "<admin2>"
    When I delete the "advanced" throttling policy with id "advThrottlePolicyId"
    Then The response status code should be 403

    Examples:
      | tenant       | admin1            | admin2                   |
      | carbon.super | admin             | policyAdmin3             |
      | tenant1.com  | admin@tenant1.com | policyAdmin3@tenant1.com |

  # The RESOURCE-level (per-operation) form of the same refusal: the policy is attached to a single operation's
  # throttlingPolicy rather than to the API's apiThrottlingPolicy, so the in-use check has to consult the API's
  # URL templates and not just the API-level tier. Ports AdvancedThrottlingPolicyTestCase
  # #testDeleteAssignedResourceLevelAdvancedPolicyWithDifferentAdminUser (also vacuous in legacy).
  @cap:admin @feat:throttling-policies @type:negative @dep:publisher @legacy:AdvancedThrottlingPolicyTestCase
  Scenario Outline: An advanced throttling policy assigned to one API operation cannot be deleted by a different admin in <tenant>
    Given The system is ready
    And I provision user "policyAdmin4" with roles "admin" in tenant "<tenant>"
    And I have valid access tokens as "<admin1>"
    When I create an advanced throttling policy "xAdminOp${UNIQUE:P}" allowing 1000 requests per minute
    Then The response status code should be 201
    When I put JSON payload from file "artifacts/payloads/create_apim_test_api.json" in context as "xAdminOpApiPayload"
    And I create an "apis" resource with payload "xAdminOpApiPayload" as "xAdminOpApiId"
    When I retrieve the "apis" resource with id "xAdminOpApiId"
    And I put the response payload in context as "xAdminOpApiFetched"
    And I update the "apis" resource "xAdminOpApiId" and "xAdminOpApiFetched" with configuration type "operations" and value:
    """
    [{"verb":"GET","target":"/customers/{id}","authType":"Application & Application User","throttlingPolicy":"{{advThrottlePolicyName}}","scopes":[]}]
    """
    Then The response status code should be 200
    # The update above replaced the operation set with exactly one GET, so one entry is the whole array.
    And The response array field "operations[*].throttlingPolicy" should have exactly 1 entries
    And The response field "operations[*].throttlingPolicy" should be exactly the list "{{advThrottlePolicyName}}"
    And I have valid access tokens as "<admin2>"
    When I delete the "advanced" throttling policy with id "advThrottlePolicyId"
    Then The response status code should be 403

    Examples:
      | tenant       | admin1            | admin2                   |
      | carbon.super | admin             | policyAdmin4             |
      | tenant1.com  | admin@tenant1.com | policyAdmin4@tenant1.com |

  # Subscription-tier role permission enforced on the PUBLISHER force-change path. The scenario above proves the
  # ALLOW list is enforced at SUBSCRIBE time in the devportal; this one proves the same policy permission also
  # gates a publisher forcing an existing subscription onto that tier, and that flipping an existing policy's
  # permission from ALLOW to DENY takes effect on an already-created subscription. Ports
  # ChangeSubscriptionBusinessPlanForcefullyTestCase#testUpdateSubscriptionBusinessPlanWhenSubscriberRestrictedToUseSpecificTier
  # (legacy's refusal assertion lived in a catch with no Assert.fail, so a successful force-change passed silently).
  @cap:admin @feat:throttling-policies @type:negative @dep:publisher @dep:devportal @legacy:ChangeSubscriptionBusinessPlanForcefullyTestCase
  Scenario Outline: Flipping a subscription tier permission to DENY refuses a publisher business-plan force-change as <adminActor>
    Given The system is ready
    And I have valid access tokens as "<adminActor>"
    When I create a subscription throttling policy "denyFlipTier${UNIQUE:P}" allowing 1000 requests per minute restricted to role "Internal/subscriber"
    Then The response status code should be 201
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "flipApiId" and deployed it
    When I retrieve the "apis" resource with id "flipApiId"
    And I put the response payload in context as "flipApiPayload"
    And I update the "apis" resource "flipApiId" and "flipApiPayload" with configuration type "policies" and value:
    """
    ["Unlimited","{{subThrottlePolicyName}}"]
    """
    Then The response status code should be 200
    When I publish the "apis" resource with id "flipApiId"
    Then The lifecycle status of API "flipApiId" should be "Published"
    And The system is ready and I have valid devportal access token as "<subscriberActor>"
    And I create an application "${UNIQUE:FlipApp}" with visibility "PRIVATE" as "flipAppId"
    Then The response status code should be 201
    When I put the following JSON payload in context as "flipSub"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "flipApiId" using application "flipAppId" with payload "flipSub" as "flipSubId"
    Then The response status code should be 201
    # While the tier still ALLOWs Internal/subscriber, the publisher may force the subscription onto it.
    And I act as "<adminActor>"
    When I change the subscription business plan of "flipSubId" to "{{subThrottlePolicyName}}"
    Then The response status code should be 200
    # Flip the SAME policy's permission to DENY for that role, then repeat the identical force-change.
    When I set the "subscription" throttling policy "subThrottlePolicyId" permission to "DENY" for role "Internal/subscriber"
    Then The response status code should be 200
    When I change the subscription business plan of "flipSubId" to "{{subThrottlePolicyName}}"
    Then The response status code should be 403

    Examples:
      | adminActor        | subscriberActor            |
      | admin             | subscriberUser             |
      | admin@tenant1.com | subscriberUser@tenant1.com |
