@cleanup
Feature: Gateway Throttling Enforcement

  Runtime throttling enforcement at the gateway across the throttle-policy dimensions: an entity bound to a low
  limit is refused with 429 once it exceeds it. This is the coverage the legacy throttling suite intended but
  never delivered (its API-level throttle tests used unthrottled built-in tiers with unreachable/commented 429
  assertions, and its burst test was commented out of the suite). We create bespoke low policies via the admin
  API (built-in tiers are thousands/min — far too high to trip in a test) and drive invocations past the limit
  until the gateway returns 429, covering six dimensions:
    1. APPLICATION request-count (the application is bound to a low application policy);
    2. SUBSCRIPTION request-count (the subscription is on a low subscription tier);
    3. SUBSCRIPTION burst control (a low burst/rate limit on top of a high quota, so the early 429 is
       unambiguously the burst limit, not the quota);
    4. API-LEVEL (advanced) request-count (a low advanced policy set as the API's apiThrottlingPolicy, enforced
       across every subscription to the API regardless of the app/subscription tier);
    5. APPLICATION BANDWIDTH (the BANDWIDTHLIMIT policy type — a 1 KB/min data quota rather than a request
       count; small GETs accumulate past it and the gateway 429s, then stays throttled for the window). This was
       verified deterministic by direct observation before committing (a 1 KB/min quota trips at ~the 16th small
       GET and stays 429), so the standard until-429 retry trips it within the window;
    6. CUSTOM (Siddhi) rule — a global custom throttling rule whose Siddhi eligibility is keyed on this test's
       UNIQUE apiContext (keyTemplate $apiContext), so it throttles only this test's own API after N/min and
       stays isolation-safe in the shared container. Verified deterministic by observation before committing
       (trips at the 5th request, sticky). NOTE: custom rules are an admin-global feature — a tenant admin gets
       403 creating one (verified) — so this dimension runs SUPER-TENANT ONLY, not ×2 like the other five.
  Each runs in BOTH the super tenant and tenant1.com to prove enforcement is tenant-agnostic (every row creates
  its own uniquely-named policy/app/API in its tenant, so the time-sensitive throttle windows never overlap).
  Teardown via the per-scenario cleanup hook (API, application, and admin throttling policies are all
  registered).

  # Note on scope vs. the legacy BurstControlServerRestartTestCase: that test also swapped the subscription
  # tier mid-scenario (5/min -> 25/min) with a 60s window-reset wait and re-verified the new limit. That
  # tier-swap is DROPPED here on purpose: the 60s sleep + remove/re-subscribe dance is the flaky part that got
  # the legacy test disabled, and once each tier enforces its own limit independently (scenarios 2 and 3), the
  # swap adds cost without a distinct assertion. Burst is set at MINUTE granularity so it trips deterministically
  # via the cumulative until-429 retry rather than a sub-second window that would reset between attempts.

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

    # Once routable, drive past the 3/min application limit — the gateway must refuse with 429.
    And I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{apiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429

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

    # Drive past the 3/min SUBSCRIPTION limit — the gateway must refuse with 429.
    And I invoke the API at gateway context "{{subApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{subApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429

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

    # Drive past the 5/min burst limit (well under the 1000/min quota) — the gateway must refuse with 429.
    And I invoke the API at gateway context "{{burstApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{burstApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429

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

    # Drive past the API-level 3/min limit — the gateway must refuse with 429 (applies across the whole API).
    And I invoke the API at gateway context "{{advApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{advApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:throttling-enforcement @type:regression @dep:admin @dep:publisher @dep:devportal @legacy:JWTBandwidthThrottlingServerRestartTestCase
  Scenario Outline: An application is throttled with 429 once it exceeds its BANDWIDTH quota as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # A bespoke application BANDWIDTH policy: 1 KB/min (the BANDWIDTHLIMIT type). Small GET responses accumulate
    # past the quota in ~16 calls (verified by observation), so the until-429 retry trips it within the window.
    When I create an application throttling policy "${UNIQUE:bw1KBperMin}" allowing 1 KB per minute
    Then The response status code should be 201

    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "bwApiId" and deployed it
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

    # Drive small GETs until the 1 KB/min bandwidth quota is exceeded — the gateway must refuse with 429.
    And I invoke the API at gateway context "{{bwApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{bwApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429

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

    # Drive past the 5/min custom rule — the gateway must refuse with 429.
    And I invoke the API at gateway context "{{custApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{custApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 90 seconds
    Then The response status code should be 429

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

    # Drive past the operation's 3/min limit — the gateway must refuse with 429 on that operation.
    And I invoke the API at gateway context "{{opApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200
    When I invoke the API at gateway context "{{opApiContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429

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

    # Drive past the 3/min limit -> 429.
    When I invoke the API at gateway context "{{resetContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 429 within 60 seconds
    Then The response status code should be 429

    # Reset the application's throttle counter -> invocation succeeds again.
    When I reset the application throttle policy for "createdAppId" owned by "<actor>"
    Then The response status code should be 200
    When I invoke the API at gateway context "{{resetContext}}/1.0.0/customers/123/" with method "GET" using access token "generatedAccessToken" and payload "" until response status code becomes 200 within 60 seconds
    Then The response status code should be 200

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
