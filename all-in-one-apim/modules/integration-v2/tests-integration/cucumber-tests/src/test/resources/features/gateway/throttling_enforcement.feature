@cleanup
Feature: Gateway Throttling Enforcement

  Runtime throttling enforcement at the gateway across the throttle-policy dimensions: an entity bound to a low
  limit is refused with 429 once it exceeds it. This is the coverage the legacy throttling suite intended but
  never delivered (its API-level throttle tests used unthrottled built-in tiers with unreachable/commented 429
  assertions, and its burst test was commented out of the suite). We create bespoke low policies via the admin
  API (built-in tiers are thousands/min — far too high to trip in a test) and drive invocations past the limit
  until the gateway returns 429, covering seven dimensions:
    1. APPLICATION request-count (the application is bound to a low application policy) — code 900803;
    2. SUBSCRIPTION request-count (the subscription is on a low subscription tier) — code 900804;
    3. SUBSCRIPTION burst control (a low burst/rate limit on top of a high quota, so the early 429 is
       unambiguously the burst limit, not the quota) — code 900807;
    4. API-LEVEL (advanced) request-count (a low advanced policy set as the API's apiThrottlingPolicy, enforced
       across every subscription to the API regardless of the app/subscription tier) — code 900800;
    5. APPLICATION BANDWIDTH (the BANDWIDTHLIMIT policy type — a 1 KB/min DATA quota rather than a request count)
       — code 900803. A single 4 KB POST spends four times the whole quota, so the refusal follows immediately and
       needs no accumulation across the quota's minute window (which resets every minute, faster than a poll loop
       pacing itself over the propagation deadline can accumulate);
    6. CUSTOM (Siddhi) rule — a global custom throttling rule whose Siddhi eligibility is keyed on this test's
       UNIQUE apiContext (keyTemplate $apiContext), so it throttles only this test's own API after N/min and
       stays isolation-safe in the shared container — code 900806. Verified deterministic by observation before
       committing (trips at the 5th request, sticky). NOTE: custom rules are an admin-global feature — a tenant
       admin gets 403 creating one (verified) — so this dimension runs SUPER-TENANT ONLY, not ×2 like the others.
    7. OPERATION-LEVEL (advanced) request-count (the advanced policy is set on one operation rather than on the
       whole API) — code 900802.
  Every 429 additionally asserts the throttle `code` in the fault body, so a scenario cannot pass on a 429 raised
  by a DIFFERENT limit than the one it configures — the status alone cannot tell the seven dimensions apart.
  900803 is shared by application request-count and application bandwidth, so the code does NOT prove which of the
  two fired; the FIXTURE does — the bandwidth rows leave the subscription on Unlimited and set no
  apiThrottlingPolicy, making the bandwidth policy the only binding limit.
  Each runs in BOTH the super tenant and tenant1.com to prove enforcement is tenant-agnostic (every row creates
  its own uniquely-named policy/app/API in its tenant, so the time-sensitive throttle windows never overlap).
  Teardown via the per-scenario cleanup hook (API, application, and admin throttling policies are all
  registered).

  # The legacy BurstControlTestCase tier-swap (5/min -> 25/min, re-verifying the RAISED limit) IS covered — see the
  # burst-tier-swap scenario below. Legacy's 60s window-reset sleep is replaced by polling: an until-403 probe proves
  # the unsubscribe reached the gateway and an until-200 probe returns only once the burst window has rolled over,
  # so the measured burst starts on a fresh counter under the new tier. Burst is set at MINUTE granularity so it
  # trips deterministically via the cumulative until-429 retry rather than a sub-second window that would reset
  # between attempts.

  @cap:gateway @feat:throttling-enforcement @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:APIThrottlingTestCase
  Scenario Outline: An application is throttled with 429 once it exceeds its request-count limit as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # A bespoke application policy allowing only 3 requests/min, so the limit is reachable in a test.
    When I create an application throttling policy "${UNIQUE:throttle3perMin}" allowing 3 requests per minute
    Then The response status code should be 201

    # Publish and deploy an API to invoke.
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "throttleApiId" and deployed it
    When I publish the "apis" resource with id "throttleApiId"
    Then The lifecycle status of API "throttleApiId" should be "Published"
    When I retrieve the "apis" resource with id "throttleApiId"
    And I extract response field "context" and store it as "apiContext"

    # An application bound to the low policy, subscribed and keyed.
    When I create an application "${UNIQUE:ThrottleApp}" with throttling policy from "appThrottlePolicyName"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "throttleApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subscriptionId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200

    # Once routable, drive past the 3/min application limit — the gateway must refuse with 429 code 900803
    # (APPLICATION-level throttling), not a 429 from any other dimension.
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900803"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:throttling-enforcement @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:BurstControlServerRestartTestCase
  Scenario Outline: An application is throttled with 429 once it exceeds its SUBSCRIPTION request-count limit as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # A bespoke subscription policy allowing only 3 requests/min.
    When I create a subscription throttling policy "${UNIQUE:subReq3perMin}" allowing 3 requests per minute
    Then The response status code should be 201

    # Publish and deploy an API to invoke.
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "subThrottleApiId" and deployed it
    And the "apis" resource "subThrottleApiId" should be live on the gateway, redeploying if propagation is lost
    # A subscription can only use a tier the API OFFERS, so add the custom tier to the API's business plans.
    When I retrieve the "apis" resource with id "subThrottleApiId"
    And I put the response payload in context as "subApiPayload"
    When I update the "apis" resource "subThrottleApiId" and "subApiPayload" with configuration type "policies" and value:
    """
    ["Unlimited","{{subThrottlePolicyName}}"]
    """
    Then The response status code should be 200
    When I publish the "apis" resource with id "subThrottleApiId"
    Then The lifecycle status of API "subThrottleApiId" should be "Published"
    When I retrieve the "apis" resource with id "subThrottleApiId"
    And I extract response field "context" and store it as "subApiContext"

    # A normal (Unlimited-tier) application, subscribed on the LOW subscription tier, and keyed.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "{{subThrottlePolicyName}}"}
    """
    And I subscribe to API "subThrottleApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subSubscriptionId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200

    # Drive past the 3/min SUBSCRIPTION limit — the gateway must refuse with 429 code 900804 (SUBSCRIPTION-level
    # throttling), distinguishing it from the application-level 900803.
    And I invoke the API at gateway context "{{subApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{subApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900804"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:throttling-enforcement @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:BurstControlServerRestartTestCase
  Scenario Outline: An application is throttled with 429 by SUBSCRIPTION burst control as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # A subscription policy with a HIGH quota (1000/min) but a LOW burst limit (5/min): any early 429 is the
    # burst limit tripping, not the quota — isolating burst-control enforcement.
    When I create a subscription throttling policy "${UNIQUE:subBurst5perMin}" allowing 1000 requests per minute with burst limit 5 per minute
    Then The response status code should be 201

    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "burstApiId" and deployed it
    # A subscription can only use a tier the API OFFERS, so add the custom tier to the API's business plans.
    When I retrieve the "apis" resource with id "burstApiId"
    And I put the response payload in context as "burstApiPayload"
    When I update the "apis" resource "burstApiId" and "burstApiPayload" with configuration type "policies" and value:
    """
    ["Unlimited","{{subThrottlePolicyName}}"]
    """
    Then The response status code should be 200
    When I publish the "apis" resource with id "burstApiId"
    Then The lifecycle status of API "burstApiId" should be "Published"
    When I retrieve the "apis" resource with id "burstApiId"
    And I extract response field "context" and store it as "burstApiContext"

    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "{{subThrottlePolicyName}}"}
    """
    And I subscribe to API "burstApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "burstSubscriptionId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200

    # Drive past the 5/min burst limit (well under the 1000/min quota) — the gateway must refuse with 429 code
    # 900807 (BURST control), which is what proves the burst limit fired rather than the subscription quota (900804).
    And I invoke the API at gateway context "{{burstApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{burstApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900807"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # BURST TIER SWAP — ports BurstControlTestCase#testBurstLimitChange (disabled in legacy): a subscription on a LOW
  # burst tier throttles at the low limit; after UNSUBSCRIBING and RE-SUBSCRIBING on a HIGH burst tier, the same
  # application sustains a burst the low tier would have refused. The two tiers carry the SAME 1000/min quota and
  # differ ONLY in burst (5/min vs 25/min), so the swap changes exactly one variable.
  # Why the post-swap burst DISCRIMINATES: the measured burst is 12 back-to-back calls, plus the one call that the
  # until-200 probe below already spent in the same window = 13 requests. 13 is ABOVE the low tier's 5/min burst
  # (which would refuse the 6th) and BELOW the high tier's 25/min, so an all-200 outcome is possible only while the
  # HIGH tier is in force. A window rollover mid-burst only LOWERS the in-window count, so the burst can never
  # exceed 25 in any window — the assertion has no false-failure mode from timing, only the intended teeth.
  # Why NO sleep is needed for the window (the flaky part of legacy): the pre-swap 429 spent this minute's burst
  # counter, so a burst started immediately could 429 on LEFTOVER count rather than on the new tier. Instead of
  # sleeping out the window, we poll until a call SUCCEEDS again — a 200 means the burst counter admitted a request,
  # which is true only on a rolled-over (or freshly keyed) window, so the counter is at 1 when the burst starts.
  # Sound whichever way the gateway keys the burst counter: if the key survives the re-subscribe, the 200 proves the
  # rollover; if the re-subscribe re-keys it, the counter was fresh anyway. The until-200 envelope's deadline is
  # floored at the 180s propagation window — three burst windows — so a 60s rollover always fits inside it.
  @cap:gateway @feat:throttling-enforcement @rule:burst-tier-swap @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:BurstControlTestCase
  Scenario Outline: A subscription's burst limit rises once it is re-subscribed on a higher burst tier as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # Two subscription tiers, same 1000/min quota, burst 5/min and 25/min. Captured by name from each create
    # response because the create step publishes only the LATEST policy name to context.
    When I create a subscription throttling policy "${UNIQUE:swapBurst5perMin}" allowing 1000 requests per minute with burst limit 5 per minute
    Then The response status code should be 201
    And I extract response field "policyName" and store it as "lowBurstTier"
    When I create a subscription throttling policy "${UNIQUE:swapBurst25perMin}" allowing 1000 requests per minute with burst limit 25 per minute
    Then The response status code should be 201
    And I extract response field "policyName" and store it as "highBurstTier"

    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "swapBurstApiId" and deployed it
    And the "apis" resource "swapBurstApiId" should be live on the gateway, redeploying if propagation is lost
    # A subscription can only use a tier the API OFFERS, and this one moves between two — so offer BOTH.
    When I retrieve the "apis" resource with id "swapBurstApiId"
    And I put the response payload in context as "swapBurstApiPayload"
    When I update the "apis" resource "swapBurstApiId" and "swapBurstApiPayload" with configuration type "policies" and value:
    """
    ["Unlimited","{{lowBurstTier}}","{{highBurstTier}}"]
    """
    Then The response status code should be 200
    When I publish the "apis" resource with id "swapBurstApiId"
    Then The lifecycle status of API "swapBurstApiId" should be "Published"
    When I retrieve the "apis" resource with id "swapBurstApiId"
    And I extract response field "context" and store it as "swapBurstApiContext"

    # A normal (Unlimited-tier) application, subscribed on the LOW burst tier, and keyed.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "swapLowTierSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "{{lowBurstTier}}"}
    """
    And I subscribe to API "swapBurstApiId" using application "createdAppId" with payload "swapLowTierSubPayload" as "swapLowBurstSubId"
    Then The response status code should be 201
    And The value of response field "throttlingPolicy" should be "{{lowBurstTier}}"
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200

    # LOW tier in force: driving past 5/min gives 429 code 900807 (BURST control), not the 1000/min quota's 900804.
    And I invoke the API at gateway context "{{swapBurstApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{swapBurstApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900807"

    # SWAP, step 1 — unsubscribe, then poll until the SAME token is refused with 403. An unsubscribed application is
    # rejected at authentication, ahead of the throttle handler, so this 403 is reachable even while burst-throttled.
    # It is the deterministic proof the DELETE reached the gateway, so the measured burst below cannot be served by a
    # still-live LOW-tier subscription (which would otherwise make an all-200 burst meaningless).
    When I delete the subscription with id "swapLowBurstSubId"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{swapBurstApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 403 within 90 seconds
    Then The response status code should be 403

    # SWAP, step 2 — re-subscribe on the HIGH burst tier. The token is NOT re-minted on purpose: the gateway resolves
    # the tier from the live subscription, so the same credential going 200 -> 403 -> 200 is itself the evidence.
    When I put the following JSON payload in context as "swapHighTierSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "{{highBurstTier}}"}
    """
    And I subscribe to API "swapBurstApiId" using application "createdAppId" with payload "swapHighTierSubPayload" as "swapHighBurstSubId"
    Then The response status code should be 201
    And The value of response field "throttlingPolicy" should be "{{highBurstTier}}"

    # SWAP, step 3 — the window/propagation gate (see the header comment): poll until a call succeeds again. The 200
    # proves BOTH that the new subscription is live at the gateway and that the burst counter admits requests again.
    When I invoke the API at gateway context "{{swapBurstApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 90 seconds
    Then The response status code should be 200

    # THE DISCRIMINATING BURST: 12 more calls, every one of which must be 200 (the N-times step asserts each
    # internally). With the probe above that is 13 in the window — 2.6x the low tier's 5/min burst, so this burst
    # would have been refused under the tier we swapped away from.
    When I invoke the API at gateway context "{{swapBurstApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" 12 times expecting status 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:throttling-enforcement @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:JWTBandwidthThrottlingServerRestartTestCase
  Scenario Outline: An API is throttled with 429 once it exceeds its API-LEVEL (advanced) request-count limit as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # A bespoke advanced (API-level) policy allowing only 3 requests/min across the whole API.
    When I create an advanced throttling policy "${UNIQUE:advReq3perMin}" allowing 3 requests per minute
    Then The response status code should be 201

    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "advThrottleApiId" and deployed it
    # Set the API-level throttling policy, then redeploy so the gateway enforces it.
    When I retrieve the "apis" resource with id "advThrottleApiId"
    And I put the response payload in context as "advApiPayload"
    When I update the "apis" resource "advThrottleApiId" and "advApiPayload" with configuration type "apiThrottlingPolicy" and value:
    """
    {{advThrottlePolicyName}}
    """
    Then The response status code should be 200
    When I deploy the API with id "advThrottleApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "advThrottleApiId"
    Then The lifecycle status of API "advThrottleApiId" should be "Published"
    When I retrieve the "apis" resource with id "advThrottleApiId"
    And I extract response field "context" and store it as "advApiContext"

    # A normal Unlimited-tier application (the limit is on the API, not the app/subscription), subscribed and keyed.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "advThrottleApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "advSubscriptionId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200

    # Drive past the API-level 3/min limit — the gateway must refuse with 429 code 900800 (API-level throttling,
    # applied across the whole API), not the app/subscription codes.
    And I invoke the API at gateway context "{{advApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{advApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900800"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:throttling-enforcement @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:JWTBandwidthThrottlingServerRestartTestCase
  Scenario Outline: An application is throttled with 429 once it exceeds its BANDWIDTH quota as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # A bespoke application BANDWIDTH policy: 1 KB/min (the BANDWIDTHLIMIT type) — a BYTE quota, not a request count.
    When I create an application throttling policy "${UNIQUE:bw1KBperMin}" allowing 1 KB per minute
    Then The response status code should be 201

    # An API exposing POST /reflect-body, whose backend echoes the request body: a single oversized POST spends the
    # whole byte quota, which a ~24-byte GET can never do without accumulating across the quota's minute window.
    And I have created an api from "artifacts/payloads/create_apim_postbody_api.json" as "bwApiId" and deployed it
    When I publish the "apis" resource with id "bwApiId"
    Then The lifecycle status of API "bwApiId" should be "Published"
    When I retrieve the "apis" resource with id "bwApiId"
    And I extract response field "context" and store it as "bwApiContext"

    # An application bound to the bandwidth policy, subscribed and keyed.
    When I create an application "${UNIQUE:BwApp}" with throttling policy from "appThrottlePolicyName"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "bwApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "bwSubscriptionId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200

    # Warm up the POST resource with an empty body, then spend 4x the 1 KB/min quota in ONE request — the gateway
    # refuses with 429 code 900803 (the application-level code, shared with application request-count; here the
    # bandwidth policy is the only binding limit, since the subscription is Unlimited and no API policy is set).
    When I put a 4 KB text payload in context as "bwLargePayload"
    And I invoke the API at gateway context "{{bwApiContext}}/1.0.0/reflect-body" with method "POST" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{bwApiContext}}/1.0.0/reflect-body" with method "POST" using access token "generatedAccessToken" and payload "bwLargePayload" with content type "text/plain" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900803"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # SUBSCRIPTION-level BANDWIDTH: a BANDWIDTHLIMIT policy of subscription type bound at the subscription tier — a
  # BYTE quota, not a request count. Complements the application-bandwidth scenario above (its 900803 proves only
  # the application level fired); here the subscription tier is the sole binding limit (the app is Unlimited and no
  # API policy is set), so the code must be the SUBSCRIPTION-level 900804. Ports JWTBandwidthThrottlingTestCase
  # #testSubscriptionLevelThrottling (whose ~230-byte-body/15-request loop only asserted a bare 429; the byte quota
  # and the level-specific code were never pinned).
  @cap:gateway @feat:throttling-enforcement @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:JWTBandwidthThrottlingTestCase
  Scenario Outline: An application is throttled with 429 once it exceeds its SUBSCRIPTION BANDWIDTH quota as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # A bespoke subscription BANDWIDTH policy: 1 KB/min (the BANDWIDTHLIMIT type) — a BYTE quota, not a request count.
    When I create a subscription throttling policy "${UNIQUE:subBw1KB}" allowing 1 KB per minute
    Then The response status code should be 201

    # An API exposing POST /reflect-body, whose backend echoes the request body: a single oversized POST spends the
    # whole byte quota in one call.
    And I have created an api from "artifacts/payloads/create_apim_postbody_api.json" as "subBwApiId" and deployed it
    And the "apis" resource "subBwApiId" should be live on the gateway, redeploying if propagation is lost
    # A subscription can only use a tier the API OFFERS, so add the custom bandwidth tier to the API's business plans.
    When I retrieve the "apis" resource with id "subBwApiId"
    And I put the response payload in context as "subBwApiPayload"
    When I update the "apis" resource "subBwApiId" and "subBwApiPayload" with configuration type "policies" and value:
    """
    ["Unlimited","{{subThrottlePolicyName}}"]
    """
    Then The response status code should be 200
    When I publish the "apis" resource with id "subBwApiId"
    Then The lifecycle status of API "subBwApiId" should be "Published"
    When I retrieve the "apis" resource with id "subBwApiId"
    And I extract response field "context" and store it as "subBwApiContext"

    # A normal (Unlimited-tier) application, subscribed on the LOW bandwidth subscription tier, and keyed.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "{{subThrottlePolicyName}}"}
    """
    And I subscribe to API "subBwApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "subBwSubscriptionId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200

    # Warm up the POST resource with an empty body, then spend 4x the 1 KB/min quota in ONE request — the gateway
    # refuses with 429 code 900804 (SUBSCRIPTION-level throttling), distinguishing it from the application code 900803.
    When I put a 4 KB text payload in context as "subBwLargePayload"
    And I invoke the API at gateway context "{{subBwApiContext}}/1.0.0/reflect-body" with method "POST" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{subBwApiContext}}/1.0.0/reflect-body" with method "POST" using access token "generatedAccessToken" and payload "subBwLargePayload" with content type "text/plain" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900804"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # API-LEVEL (advanced) BANDWIDTH: a BANDWIDTHLIMIT advanced policy set as the API's apiThrottlingPolicy — enforced
  # across every subscription to the API regardless of the app/subscription tier. Complements the application- and
  # subscription-bandwidth scenarios; here the app and subscription are both Unlimited, so the API-level policy is
  # the sole binding limit and the code must be the API-level 900800. Ports JWTBandwidthThrottlingTestCase
  # #testAPILevelThrottling (whose ~230-byte-body/15-request loop only asserted a bare 429).
  @cap:gateway @feat:throttling-enforcement @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:JWTBandwidthThrottlingTestCase
  Scenario Outline: An API is throttled with 429 once it exceeds its API-LEVEL (advanced) BANDWIDTH quota as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # A bespoke advanced (API-level) BANDWIDTH policy: 1 KB/min (the BANDWIDTHLIMIT type) across the whole API.
    When I create an advanced throttling policy "${UNIQUE:advBw1KB}" allowing 1 KB per minute
    Then The response status code should be 201

    # An API exposing POST /reflect-body, whose backend echoes the request body: a single oversized POST spends the
    # whole byte quota in one call.
    And I have created an api from "artifacts/payloads/create_apim_postbody_api.json" as "advBwApiId" and deployed it
    # Set the API-level throttling policy to the bandwidth policy, then redeploy so the gateway enforces it.
    When I retrieve the "apis" resource with id "advBwApiId"
    And I put the response payload in context as "advBwApiPayload"
    When I update the "apis" resource "advBwApiId" and "advBwApiPayload" with configuration type "apiThrottlingPolicy" and value:
    """
    {{advThrottlePolicyName}}
    """
    Then The response status code should be 200
    When I deploy the API with id "advBwApiId"
    Then The response status code should be 201
    And the "apis" resource "advBwApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "advBwApiId"
    Then The lifecycle status of API "advBwApiId" should be "Published"
    When I retrieve the "apis" resource with id "advBwApiId"
    And I extract response field "context" and store it as "advBwApiContext"

    # A normal Unlimited-tier application (the limit is on the API, not the app/subscription), subscribed and keyed.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "advBwApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "advBwSubscriptionId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200

    # Warm up the POST resource with an empty body, then spend 4x the 1 KB/min API-level quota in ONE request — the
    # gateway refuses with 429 code 900800 (API-level throttling, applied across the whole API), not an app/sub code.
    When I put a 4 KB text payload in context as "advBwLargePayload"
    And I invoke the API at gateway context "{{advBwApiContext}}/1.0.0/reflect-body" with method "POST" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{advBwApiContext}}/1.0.0/reflect-body" with method "POST" using access token "generatedAccessToken" and payload "advBwLargePayload" with content type "text/plain" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900800"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # SUPER-TENANT ONLY (not a Scenario Outline like the others): custom Siddhi rules are an admin-global feature
  # — a tenant admin (admin@tenant1.com) gets 403 "not allowed" creating one (verified). So this dimension can
  # only run in the super tenant, unlike the per-app/subscription/API dimensions above.
  @cap:gateway @feat:throttling-enforcement @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:CustomThrottlingPolicyServerRestartTestCase
  Scenario: An API is throttled with 429 by a custom (Siddhi) throttling rule
    Given The system is ready
    And I have valid access tokens as "admin"

    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "custApiId" and deployed it
    When I publish the "apis" resource with id "custApiId"
    Then The lifecycle status of API "custApiId" should be "Published"
    When I retrieve the "apis" resource with id "custApiId"
    And I extract response field "context" and store it as "custApiContext"

    # A custom Siddhi rule throttling THIS unique API context after 5 req/min (isolation-safe: the global rule's
    # eligibility is keyed on this test's own apiContext, so no sibling scenario is affected).
    When I create a custom throttling policy "${UNIQUE:custCtx5}" throttling API context "{{custApiContext}}" after 5 requests per minute
    Then The response status code should be 201

    # A normal Unlimited-tier application, subscribed and keyed.
    When I have set up application with keys, subscribed to API "custApiId", and obtained access token for "custSubscriptionId"
    Then The response status code should be 200

    # Drive past the 5/min custom rule — the gateway must refuse with 429 code 900806 (CUSTOM rule), proving the
    # Siddhi rule fired and not one of the policy-tier dimensions.
    And I invoke the API at gateway context "{{custApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{custApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 90 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900806"

  # OPERATION-LEVEL (advanced) request-count: the advanced policy is set on a specific operation (not the whole
  # API via apiThrottlingPolicy), so exceeding it on that operation → 429. Complements the API-LEVEL scenario
  # above — together they port the operation↔API-level change of AdvancedThrottlingPolicyTestCase (#9/#10).
  @cap:gateway @feat:rest-invocation @type:regression @dep:admin @legacy:AdvancedThrottlingPolicyTestCase
  Scenario Outline: An operation is throttled with 429 once it exceeds its OPERATION-LEVEL advanced limit as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # A bespoke advanced policy allowing only 3 requests/min, assigned to a single operation.
    When I create an advanced throttling policy "${UNIQUE:advOp3perMin}" allowing 3 requests per minute
    Then The response status code should be 201

    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "opThrottleApiId" and deployed it
    When I retrieve the "apis" resource with id "opThrottleApiId"
    And I put the response payload in context as "opApiPayload"
    And I update the "apis" resource "opThrottleApiId" and "opApiPayload" with configuration type "operations" and value:
    """
    [{"target":"/customers/{id}","verb":"GET","authType":"Application & Application User","throttlingPolicy":"{{advThrottlePolicyName}}","scopes":[],"operationPolicies":{"request":[],"response":[],"fault":[]}}]
    """
    Then The response status code should be 200
    When I deploy the API with id "opThrottleApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "opThrottleApiId"
    Then The lifecycle status of API "opThrottleApiId" should be "Published"
    When I retrieve the "apis" resource with id "opThrottleApiId"
    And I extract response field "context" and store it as "opApiContext"

    # A normal Unlimited-tier application (the limit is on the operation, not the app/subscription).
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "opThrottleApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "opSubscriptionId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200

    # Drive past the operation's 3/min limit — the gateway must refuse with 429 code 900802 (RESOURCE/operation-level
    # throttling), distinct from the whole-API 900800 above.
    And I invoke the API at gateway context "{{opApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    And The value of response field "id" should be "123"
    And The value of response field "name" should be "John"
    When I invoke the API at gateway context "{{opApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900802"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Reset-throttle-policy: after an application trips its low request-count limit (429), the DevPortal
  # reset-throttle-policy endpoint clears the application's counters so the very next invocation succeeds (200)
  # again — without waiting out the throttle window. Ports ApplicationThrottlingResetTestCase.
  @cap:gateway @feat:throttling-enforcement @rule:throttle-reset @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:ApplicationThrottlingResetTestCase
  Scenario Outline: Resetting an application's throttle counter clears the 429 as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # A bespoke application policy allowing only 3 requests/min, so the limit is reachable in a test.
    When I create an application throttling policy "${UNIQUE:resetThrottle3}" allowing 3 requests per minute
    Then The response status code should be 201
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "resetApiId" and deployed it
    When I publish the "apis" resource with id "resetApiId"
    Then The lifecycle status of API "resetApiId" should be "Published"
    When I retrieve the "apis" resource with id "resetApiId"
    And I extract response field "context" and store it as "resetContext"

    # An application bound to the low policy, subscribed and keyed.
    When I create an application "${UNIQUE:ResetApp}" with throttling policy from "appThrottlePolicyName"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "resetApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "resetSubId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200

    # Drive past the 3/min limit -> 429 code 900803 (APPLICATION-level), the limit this reset then clears.
    When I invoke the API at gateway context "{{resetContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900803"

    # Reset the application's throttle counter -> invocation succeeds again.
    When I reset the application throttle policy for "createdAppId" owned by "<actor>"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{resetContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # Re-drive past the limit again -> 429 with the same application code: proves the reset CLEARED the counter (not
    # disabled throttling) — it re-accumulates and trips the 3/min limit once more.
    When I invoke the API at gateway context "{{resetContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900803"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Bandwidth reset: the same reset arc, but the application is bound to a low BANDWIDTH policy (1 KB/min) rather
  # than a request-count policy — the reset endpoint clears the bandwidth counter too, so the next invocation
  # succeeds (200) without waiting out the window. Complements the request-count reset above; ports the bandwidth
  # variant of ApplicationThrottlingResetTestCase.
  @cap:gateway @feat:throttling-enforcement @rule:throttle-reset @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:ApplicationThrottlingResetTestCase
  Scenario Outline: Resetting an application's BANDWIDTH throttle counter clears the 429 as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # A bespoke application BANDWIDTH policy: 1 KB/min (the BANDWIDTHLIMIT type) — a BYTE quota, tripped by one
    # oversized POST to the body-echoing /reflect-body resource.
    When I create an application throttling policy "${UNIQUE:resetBw1KB}" allowing 1 KB per minute
    Then The response status code should be 201
    And I have created an api from "artifacts/payloads/create_apim_postbody_api.json" as "resetBwApiId" and deployed it
    When I publish the "apis" resource with id "resetBwApiId"
    Then The lifecycle status of API "resetBwApiId" should be "Published"
    When I retrieve the "apis" resource with id "resetBwApiId"
    And I extract response field "context" and store it as "resetBwContext"

    # An application bound to the bandwidth policy, subscribed and keyed.
    When I create an application "${UNIQUE:ResetBwApp}" with throttling policy from "appThrottlePolicyName"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "resetBwApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "resetBwSubId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200

    # Warm up the POST resource, then spend 4x the 1 KB/min quota in ONE request -> 429 code 900803.
    When I put a 4 KB text payload in context as "resetBwLargePayload"
    And I invoke the API at gateway context "{{resetBwContext}}/1.0.0/reflect-body" with method "POST" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{resetBwContext}}/1.0.0/reflect-body" with method "POST" using access token "generatedAccessToken" and payload "resetBwLargePayload" with content type "text/plain" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900803"

    # Reset the application's throttle counter -> an empty-body invocation succeeds again (0 bytes, so this call
    # does not itself re-spend the quota).
    When I reset the application throttle policy for "createdAppId" owned by "<actor>"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{resetBwContext}}/1.0.0/reflect-body" with method "POST" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # Re-spend the 1 KB/min quota again -> 429: proves the reset cleared the bandwidth accumulator (throttling still
    # enforced afterwards).
    When I invoke the API at gateway context "{{resetBwContext}}/1.0.0/reflect-body" with method "POST" using access token "generatedAccessToken" and payload "resetBwLargePayload" with content type "text/plain" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900803"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # NON-ADMIN OWNER-RESETS-OWN request-count reset: the throttling policy and API are set up by the tenant admin
  # (both admin-only ops), but the application is created, subscribed, keyed, driven to 429 AND reset by a
  # NON-ADMIN subscriber themselves. The reset endpoint authorizes on the CALLER's ownership
  # (APIConsumerImpl#resetApplicationThrottlePolicy validates the caller owns the app — there is no admin
  # override), so the owner, not an admin, performs the reset. This covers the reset authorization path for a
  # non-admin principal, which the admin-only reset scenarios above cannot reach. Ports the SUPER_TENANT_USER /
  # TENANT_USER owner fan-out of ApplicationThrottlingResetTestCase, whose reset was driven by each fanned owner
  # over their OWN application (restAPIStore.resetApplicationThrottlePolicy with applicationDTO.getOwner()).
  @cap:gateway @feat:throttling-enforcement @rule:throttle-reset @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:ApplicationThrottlingResetTestCase
  Scenario Outline: A subscriber resets their own application's throttle counter clearing the 429 as <ownerActor>
    Given The system is ready
    And I have valid access tokens as "<adminActor>"

    # A bespoke application policy allowing only 3 requests/min, reachable in a test (admin-only op).
    When I create an application throttling policy "${UNIQUE:xoResetThrottle3}" allowing 3 requests per minute
    Then The response status code should be 201
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "xoResetApiId" and deployed it
    And the "apis" resource "xoResetApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "xoResetApiId"
    Then The lifecycle status of API "xoResetApiId" should be "Published"
    When I retrieve the "apis" resource with id "xoResetApiId"
    And I extract response field "context" and store it as "xoResetContext"

    # SWITCH to the NON-ADMIN subscriber, who OWNS the application: created, subscribed, keyed, driven AND reset as the subscriber.
    Given The system is ready and I have valid devportal access token as "<ownerActor>"
    When I create an application "${UNIQUE:XoResetApp}" with throttling policy from "appThrottlePolicyName"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "xoResetApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "xoResetSubId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200

    # Drive the SUBSCRIBER's token past the 3/min limit -> 429 code 900803 (APPLICATION-level), the limit the reset clears.
    When I invoke the API at gateway context "{{xoResetContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900803"

    # Reset the SUBSCRIBER's own application counter AS the subscriber (the reset authorizes on the caller's ownership).
    When I reset the application throttle policy for "createdAppId" owned by "<ownerActor>"
    Then The response status code should be 200
    # The post-reset invocation uses the same subscriber token -> succeeds again, proving the owner cleared their own bucket.
    When I invoke the API at gateway context "{{xoResetContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # Re-drive past the limit again -> 429 with the same application code: proves the reset CLEARED the counter (not
    # disabled throttling) — it re-accumulates and trips the 3/min limit once more.
    When I invoke the API at gateway context "{{xoResetContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900803"

    Examples:
      | adminActor        | ownerActor                 |
      | admin             | subscriberUser             |
      | admin@tenant1.com | subscriberUser@tenant1.com |

  # NON-ADMIN OWNER-RESETS-OWN bandwidth reset: the same non-admin owner-resets-own split as above, but the
  # application is bound to a low BANDWIDTH policy (1 KB/min) rather than a request-count policy — the owner's
  # reset clears their bandwidth accumulator, so the next invocation succeeds (200) without waiting out the
  # window. Complements the request-count owner-resets-own reset; ports the bandwidth variant of
  # ApplicationThrottlingResetTestCase over a non-admin application owner.
  @cap:gateway @feat:throttling-enforcement @rule:throttle-reset @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:ApplicationThrottlingResetTestCase
  Scenario Outline: A subscriber resets their own application's BANDWIDTH throttle counter clearing the 429 as <ownerActor>
    Given The system is ready
    And I have valid access tokens as "<adminActor>"

    # A bespoke application BANDWIDTH policy: 1 KB/min (the BANDWIDTHLIMIT type), tripped by one oversized POST.
    When I create an application throttling policy "${UNIQUE:xoResetBw1KB}" allowing 1 KB per minute
    Then The response status code should be 201
    And I have created an api from "artifacts/payloads/create_apim_postbody_api.json" as "xoResetBwApiId" and deployed it
    And the "apis" resource "xoResetBwApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "xoResetBwApiId"
    Then The lifecycle status of API "xoResetBwApiId" should be "Published"
    When I retrieve the "apis" resource with id "xoResetBwApiId"
    And I extract response field "context" and store it as "xoResetBwContext"

    # SWITCH to the NON-ADMIN subscriber, who OWNS and resets the bandwidth-bound application.
    Given The system is ready and I have valid devportal access token as "<ownerActor>"
    When I create an application "${UNIQUE:XoResetBwApp}" with throttling policy from "appThrottlePolicyName"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "xoResetBwApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "xoResetBwSubId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200

    # Warm up the POST resource, then spend 4x the 1 KB/min quota in ONE request (subscriber's token) -> 429 code 900803.
    When I put a 4 KB text payload in context as "xoResetBwLargePayload"
    And I invoke the API at gateway context "{{xoResetBwContext}}/1.0.0/reflect-body" with method "POST" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{xoResetBwContext}}/1.0.0/reflect-body" with method "POST" using access token "generatedAccessToken" and payload "xoResetBwLargePayload" with content type "text/plain" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900803"

    # Reset the SUBSCRIBER's own bandwidth counter AS the subscriber -> an empty-body invocation (0 bytes, so it
    # does not itself re-spend the quota) succeeds again with the same subscriber token.
    When I reset the application throttle policy for "createdAppId" owned by "<ownerActor>"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{xoResetBwContext}}/1.0.0/reflect-body" with method "POST" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    # Re-spend the 1 KB/min quota again -> 429: proves the reset cleared the SUBSCRIBER's own bandwidth accumulator.
    When I invoke the API at gateway context "{{xoResetBwContext}}/1.0.0/reflect-body" with method "POST" using access token "generatedAccessToken" and payload "xoResetBwLargePayload" with content type "text/plain" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900803"

    Examples:
      | adminActor        | ownerActor                 |
      | admin             | subscriberUser             |
      | admin@tenant1.com | subscriberUser@tenant1.com |

  # Unlimited API tier is NOT throttled: an API whose apiThrottlingPolicy is the built-in Unlimited tier accepts
  # far more traffic than any low tier would allow, with no 429. Ports the first assertion of
  # JWTRequestCountThrottlingTestCase#testAPILevelThrottlingWithIpCondition
  # (assertFalse(isThrottled(...), "Request was throttled unexpectedly in Unlimited API tier")). No IP condition
  # is ported: that legacy method's name is misleading — its assertions are about TIER and REDEPLOY semantics, not
  # the IP condition, and an IP condition would make the test depend on the container's source IP as the gateway
  # sees it (environment-dependent). This is the soundly-asserted NEGATIVE: the burst below is 4x a 3/min low tier's
  # quota, so it WOULD trip any low policy — its all-200 outcome therefore has teeth.
  @cap:gateway @feat:throttling-enforcement @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:JWTRequestCountThrottlingTestCase
  Scenario Outline: An API on the Unlimited API tier is not throttled however much traffic it receives as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "unlThrottleApiId" and deployed it
    # Set the API-level throttling policy explicitly to the built-in Unlimited tier and redeploy, so the deployed
    # limit under test is unambiguously Unlimited (not merely the create default).
    When I retrieve the "apis" resource with id "unlThrottleApiId"
    And I put the response payload in context as "unlApiPayload"
    When I update the "apis" resource "unlThrottleApiId" and "unlApiPayload" with configuration type "apiThrottlingPolicy" and value:
    """
    Unlimited
    """
    Then The response status code should be 200
    When I deploy the API with id "unlThrottleApiId"
    Then The response status code should be 201
    And the "apis" resource "unlThrottleApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "unlThrottleApiId"
    Then The lifecycle status of API "unlThrottleApiId" should be "Published"
    When I retrieve the "apis" resource with id "unlThrottleApiId"
    And I extract response field "context" and store it as "unlApiContext"

    # A normal Unlimited-tier application, subscribed and keyed.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "unlThrottleApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "unlSubscriptionId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200

    # Warm up the route, then send a burst of 12 requests — 4x what a bespoke 3/min low tier permits (see the
    # request-count scenarios above), so this burst WOULD trip any low policy. Every one must return 200: the
    # Unlimited API tier never throttles. The N-times step asserts each response is 200 internally.
    And I invoke the API at gateway context "{{unlApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{unlApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" 12 times expecting status 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # A policy change is NOT enforced until the API is REDEPLOYED — the discriminating arm of
  # JWTRequestCountThrottlingTestCase#testAPILevelThrottlingWithIpCondition
  # (assertFalse(isThrottled(...), "Request not need to throttle since policy was Unlimited") after changing the
  # policy WITHOUT redeploying, then assertTrue(isThrottled(...)) once redeployed). The gateway runs the DEPLOYED
  # revision, so switching apiThrottlingPolicy on the working copy has no runtime effect until a new revision is
  # deployed. Again no IP condition, for the same reason as above.
  @cap:gateway @feat:throttling-enforcement @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:JWTRequestCountThrottlingTestCase
  Scenario Outline: An API-level throttling policy change is not enforced until the API is redeployed as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # A bespoke advanced (API-level) policy allowing only 3 requests/min — the low limit we will switch TO.
    When I create an advanced throttling policy "${UNIQUE:redeployAdv3perMin}" allowing 3 requests per minute
    Then The response status code should be 201

    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "redeployApiId" and deployed it
    # Deploy the API on the Unlimited API tier (explicit), then publish — this is the deployed baseline.
    When I retrieve the "apis" resource with id "redeployApiId"
    And I put the response payload in context as "redeployApiPayload"
    When I update the "apis" resource "redeployApiId" and "redeployApiPayload" with configuration type "apiThrottlingPolicy" and value:
    """
    Unlimited
    """
    Then The response status code should be 200
    When I deploy the API with id "redeployApiId"
    Then The response status code should be 201
    And the "apis" resource "redeployApiId" should be live on the gateway, redeploying if propagation is lost
    When I publish the "apis" resource with id "redeployApiId"
    Then The lifecycle status of API "redeployApiId" should be "Published"
    When I retrieve the "apis" resource with id "redeployApiId"
    And I extract response field "context" and store it as "redeployApiContext"

    # A normal Unlimited-tier application, subscribed and keyed.
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "generateApplicationKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "generateApplicationKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "apiSubscriptionPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "Unlimited"}
    """
    And I subscribe to API "redeployApiId" using application "createdAppId" with payload "apiSubscriptionPayload" as "redeploySubscriptionId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "PRODUCTION"
    Then The response status code should be 200

    # Baseline: with Unlimited deployed, a burst of 12 requests (4x the 3/min policy's quota) all return 200.
    And I invoke the API at gateway context "{{redeployApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{redeployApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" 12 times expecting status 200

    # Switch apiThrottlingPolicy to the low 3/min policy but do NOT redeploy: the gateway still runs the deployed
    # Unlimited revision, so another burst of 12 requests (4x the new quota) STILL all return 200 — the change is
    # not yet enforced. This is the negative arm's teeth: 12 >> 3, so if the change WERE live these would 429.
    When I retrieve the "apis" resource with id "redeployApiId"
    And I put the response payload in context as "redeployApiPayload2"
    When I update the "apis" resource "redeployApiId" and "redeployApiPayload2" with configuration type "apiThrottlingPolicy" and value:
    """
    {{advThrottlePolicyName}}
    """
    Then The response status code should be 200
    When I invoke the API at gateway context "{{redeployApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" 12 times expecting status 200

    # Now REDEPLOY: a new revision carrying the low policy replaces the Unlimited one. The until-429 loop keeps
    # sending until the new revision is live and the 3/min limit accumulates — the 429 (with the exact API-level
    # code 900800) is itself proof the new revision deployed, distinguishing it from the not-yet-enforced state.
    When I deploy the API with id "redeployApiId"
    Then The response status code should be 201
    And I wait until "apis" "redeployApiId" revision is deployed in the gateway
    When I invoke the API at gateway context "{{redeployApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 90 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900800"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # An UNAUTHENTICATED (authType None) resource is throttled: a resource whose security is turned OFF still enforces
  # its throttling policy, refusing token-less traffic with 429 once the limit is exceeded. Ports
  # JWTRequestCountThrottlingTestCase#testNonaunthenticatedResourceThrottlingWithJWTClaimCondition (which flipped a
  # resource to authType None with an operation-level advanced policy and drove token-less traffic until throttled).
  # An unauthenticated request carries no application/subscription, so which level's code applies is not obvious
  # from the app/subscription/API taxonomy — the asserted code below was OBSERVED in the run, not copied from legacy.
  @cap:gateway @feat:throttling-enforcement @type:regression @dep:admin @dep:publisher @legacy:JWTRequestCountThrottlingTestCase
  Scenario Outline: An unauthenticated resource is throttled with 429 once it exceeds its operation-level limit as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # A bespoke advanced policy allowing only 3 requests/min, assigned to the (soon-to-be) unauthenticated operation.
    When I create an advanced throttling policy "${UNIQUE:noAuthOp3perMin}" allowing 3 requests per minute
    Then The response status code should be 201

    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "noAuthApiId" and deployed it
    # Switch the GET operation to authType "None" (token-less) AND attach the low advanced policy, then redeploy so
    # the gateway enforces both. Same operations-array pattern as the security-enforcement authType-None flip.
    When I retrieve the "apis" resource with id "noAuthApiId"
    And I put the response payload in context as "noAuthApiPayload"
    And I update the "apis" resource "noAuthApiId" and "noAuthApiPayload" with configuration type "operations" and value:
    """
    [{"target":"/customers/{id}","verb":"GET","authType":"None","throttlingPolicy":"{{advThrottlePolicyName}}","scopes":[],"operationPolicies":{"request":[],"response":[],"fault":[]}}]
    """
    Then The response status code should be 200
    When I deploy the API with id "noAuthApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "noAuthApiId"
    Then The lifecycle status of API "noAuthApiId" should be "Published"
    And the "apis" resource "noAuthApiId" should be live on the gateway, redeploying if propagation is lost
    Then Every operation of API "noAuthApiId" should declare authType "None"
    When I retrieve the "apis" resource with id "noAuthApiId"
    And I extract response field "context" and store it as "noAuthApiContext"

    # Drive TOKEN-LESS traffic past the operation's 3/min limit — the gateway must refuse with 429. No token is
    # presented (the without-authentication invoke variants), proving throttling applies to an unauthenticated
    # resource. The exact code is asserted from the observed run.
    And I invoke the API at gateway context "{{noAuthApiContext}}/1.0.0/customers/123/" with method "GET" without authentication until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{noAuthApiContext}}/1.0.0/customers/123/" with method "GET" without authentication until response status code becomes 429 within 90 seconds
    Then The response status code should be 429
    And The value of error response field "code" should be "900802"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
