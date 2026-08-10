@cleanup
Feature: Gateway SSE API Invocation

  Gateway-plane runtime invocation of a published SSE (Server-Sent Events) API: create an SSE API backed by the
  node sse-emitter endpoint, publish it, subscribe an application on an async plan and obtain a token, then read
  the event stream THROUGH the gateway and assert every event the backend sent arrived. SSE is not a WebSocket
  protocol — the SseApiHandler only rewrites the verb to SUBSCRIBE for authentication and the API is otherwise an
  ordinary synapse resource on the default HTTP transport — so this is an authenticated GET on the gateway, not a
  handshake on the WS inbound. The emitter's stream is BOUNDED by a caller-supplied event count, which is what
  makes "events sent == events received" an exact assertion instead of the legacy's race against an unbounded
  stream torn down after a fixed wait. Runs in both the super tenant and tenant1.com as the tenant admin (the
  flow spans publish + subscribe + invoke). Teardown via the per-scenario cleanup hook.
  Ports ServerSentEventsAPITestCase.

  @cap:gateway @feat:streaming-invocation @type:smoke @dep:publisher @legacy:ServerSentEventsAPITestCase
  Scenario Outline: Invoke a published SSE API through the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_sse_api.json" as "sseApiId" and deployed it
    When I publish the "apis" resource with id "sseApiId"
    Then The lifecycle status of API "sseApiId" should be "Published"

    # Capture the API's full gateway context (already carries /t/<tenant> for tenant APIs)
    When I retrieve the "apis" resource with id "sseApiId"
    And I extract response field "context" and store it as "sseContext"

    # Subscribe an application (SSE APIs require an async plan) and obtain an access token
    When I have set up application with keys, subscribed to API "sseApiId" with plan "AsyncUnlimited", and obtained access token for "sseSubId"
    Then The response status code should be 200
    # The subscription must be EFFECTIVE, not merely created. The composite asserts 201 on the subscribe, and a
    # subscription parked ON_HOLD pending admin approval ALSO answers 201 — so the status code alone cannot tell an
    # active subscription from a pending one. Read the persisted subscription back and pin its exact status, as the
    # legacy did (Assert.assertEquals(subscriptionDTO.getStatus(), SubscriptionDTO.StatusEnum.UNBLOCKED)). Asserted
    # in the feature, NOT inside the shared subscribe step: the admin subscription-workflow features legitimately
    # park subscriptions ON_HOLD and assert that, so a blanket assertion in the glue would break them.
    When I get the subscription with id "sseSubId"
    Then The value of response field "status" should be "UNBLOCKED"

    # Read the stream through the gateway, then cross-check what arrived against the emitter's own record of
    # what it sent (the legacy eventsSent == eventsReceived pairing). The API id tags the stream so the
    # cross-check reads THIS scenario row's stream and not the other tenant row's.
    When I invoke the SSE API at gateway context "{{sseContext}}/1.0.0" using access token "generatedAccessToken" requesting 25 events tagged "{{sseApiId}}" within 60 seconds
    Then The response status code should be 200
    And The SSE backend should have sent every event the gateway delivered for tag "{{sseApiId}}"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Topic discovery: the DevPortal advertises the channels a streaming API declares — one per async operation, so
  # this SSE API (a single "/*" SUBSCRIBE operation) reports exactly one. A DevPortal read kept with the
  # streaming-invocation family, the same house placement as the WS gateway-URL scenario.
  # Ports ServerSentEventsAPITestCase#testTopicRetrievalofSSEApi.
  @cap:gateway @feat:streaming-invocation @rule:topics @type:regression @dep:publisher @legacy:ServerSentEventsAPITestCase
  Scenario Outline: The DevPortal lists the topics of a published SSE API as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_sse_api.json" as "sseApiId" and deployed it
    When I publish the "apis" resource with id "sseApiId"
    Then The lifecycle status of API "sseApiId" should be "Published"
    When I retrieve the topics of devportal API "sseApiId"
    Then The response status code should be 200
    And The value of response field "count" should be "1"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Negative: the SSE route enforces authentication like any other gateway route — the SseApiHandler wraps the
  # authentication handler (it only swaps the verb for SUBSCRIBE), so a garbage credential is rejected before any
  # stream is opened. A valid stream read first proves the route is live, so the rejection is genuine and not
  # warm-up. NEW vs legacy, which exercised only the throttle-out path on an SSE stream.
  @cap:gateway @feat:streaming-invocation @rule:security-negative @type:negative @dep:publisher @legacy:ServerSentEventsAPITestCase
  Scenario Outline: Reject an SSE invocation carrying an invalid token as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_test_sse_api.json" as "sseApiId" and deployed it
    When I publish the "apis" resource with id "sseApiId"
    Then The lifecycle status of API "sseApiId" should be "Published"
    When I retrieve the "apis" resource with id "sseApiId"
    And I extract response field "context" and store it as "sseContext"
    When I have set up application with keys, subscribed to API "sseApiId" with plan "AsyncUnlimited", and obtained access token for "sseSubId"
    Then The response status code should be 200
    # Routable control: a valid token streams
    When I invoke the SSE API at gateway context "{{sseContext}}/1.0.0" using access token "generatedAccessToken" requesting 5 events tagged "{{sseApiId}}" within 60 seconds
    Then The response status code should be 200
    # An invalid token is rejected on the same path
    When I put the following JSON payload in context as "sseBadToken"
    """
    this-is-an-invalid-sse-token
    """
    When I invoke the API at gateway context "{{sseContext}}/1.0.0/events" with method "GET" using access token "sseBadToken" and payload "" until response status code becomes 401 within 60 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Event-quota enforcement on an SSE stream, on the CORRECT lever: a SUBSCRIPTION-level EVENTCOUNTLIMIT plan of
  # 2 events/min is attached as the API's tier (its `policies` entry) and the application subscribes ON THAT PLAN, so
  # the gateway must CUT the stream short instead of forwarding every frame the emitter offers.
  # Ports ServerSentEventsAPITestCase#testSseApiThrottling.
  #
  # WHY THE SUBSCRIPTION TIER AND NOT `apiThrottlingPolicy`: streaming limits are a SUBSCRIPTION-plan concern only.
  # docs-apim design/rate-limiting/set-streaming-api-limits states it directly — "Streaming APIs support three types
  # of limits through subscription tiers" and "Instead of limiting requests per minute, you control the number of
  # events clients can consume and how long they can maintain connections" — and the product seeds ONLY subscription
  # plans for the async families (APIUtil.addDefaultTenantAsyncThrottlePolicies builds AsyncGold/Silver/Bronze/
  # Unlimited and the AsyncWH* set; there is no API-level async default). It is also the mechanism the legacy WebSub
  # ThrottlingTestCase used for the very same quota (SubscriptionThrottlePolicyDTO + setTiersCollection), and the
  # mechanism the LIVE WebSub subscription-count scenario in gateway/websub_invocation uses. An earlier revision of
  # this scenario attached an ADVANCED (API-level) EVENTCOUNTLIMIT policy via `apiThrottlingPolicy` and left the
  # subscription on AsyncUnlimited; that measured "20/20 frames delivered", but on an inapplicable lever, so it
  # proved nothing about the quota. Attaching the plan the same way the live WebSub scenario does is what makes the
  # measurement interpretable.
  #
  # THE ASSERTION IS "THROTTLED TO ZERO", NOT "EXACTLY 2 DELIVERED", and that is a product fact rather than a
  # concession. MEASURED with this lever: the first stream is genuinely TRUNCATED but NOT at the quota, because
  # SseResponseStreamInterceptor READS the throttle verdict from the shared throttle-data holder while separately
  # PUBLISHING each buffer's event count to the traffic manager on another thread, so the verdict lags the traffic
  # that caused it by one decision round-trip.
  # THE OVERSHOOT IS A RANGE, NOT A NUMBER — MEASURED ACROSS THREE RUNS ON THE SAME BUILD, and this is the one thing
  # not to "tighten". Against the same 2-events/min plan the FIRST stream delivered:
  #     4 of 10   (streaming-final4)
  #     4 of 10   (streaming-mutD2)
  #     7 of 10   (streaming-mutE, observed via the mutated guard's own failure message)
  # So SSE overshoots a 2-event quota by 2 to 5 events. The sibling raw-WS scenario overshoots the same way and also
  # VARIES (3 then 4 echoes before the 4003 frame, against a 4-event quota). Because the variance is measured
  # INDEPENDENTLY ON BOTH LANES it is async decision propagation through the embedded traffic manager, not an
  # SSE-specific or WS-specific defect: the gateway keeps serving while the verdict is still in flight.
  # GENERAL RULE for every event-count-throttling row: the quota trips within a RANGE, never at a deterministic
  # index. Any assertion pinning an exact trip index, or an exact first-stream count, WILL flake. Do not "sharpen"
  # `first > 0` into `first == 4` — that number was observed twice and then contradicted by the third run.
  # THE ASSERTION SHAPE IS DELIBERATE AND SURVIVES THE WHOLE 4-TO-7 SPREAD UNTOUCHED, which is the point: it asserts
  # only `first > 0` (the API routed at all) and then `settled == 0` (an exhausted quota silences a FRESH stream).
  # Neither depends on WHERE the quota bit, so a wider spread cannot destabilise it.
  # What IS exact is the END state: once the verdict
  # has landed the quota is absolute for the rest of the window, so a FRESH stream delivers exactly 0. The step
  # re-opens the stream until it sees that 0 and additionally requires the FIRST stream to have delivered at least
  # one event, so an API that never routed cannot satisfy it — "fewer than requested" is avoided in both directions
  # (§12's widened form, and the vacuity at zero that let legacy ThrottlingTestCase's `assertTrue(received < 10)`
  # pass with a completely dead fan-out).
  # This is also the legacy's own second observable: it expected a RECONNECT after the throttle to be refused.
  # Per the docs an SSE event is counted server->client only (unlike WebSocket's both-direction aggregate), so no
  # halving applies to the quota here.
  # The backend cross-check step is deliberately NOT used — it asserts the emitter ran the stream to COMPLETION,
  # which a throttled stream must not.
  # WHY THIS IS PORTED AT ALL: the legacy method carries no "@Test" annotation, so it never executed. That is a
  # reason the legacy coverage was fictional, NOT a reason the behaviour is unprotected here.
  @cap:gateway @feat:streaming-invocation @rule:event-count-throttling @type:regression @dep:admin @dep:publisher @legacy:ServerSentEventsAPITestCase
  Scenario Outline: SSE events beyond the subscription plan's event quota are not delivered as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a subscription throttling policy "${UNIQUE:sseEvQuota}" allowing 2 events per minute
    Then The response status code should be 201
    When I put JSON payload from file "artifacts/payloads/create_apim_test_sse_api.json" in context as "sseApiPayload"
    And I replace "AsyncUnlimited" with "{{subThrottlePolicyName}}" in the payload "sseApiPayload"
    And I create an "apis" resource with payload "sseApiPayload" as "sseApiId"
    And I deploy the API with id "sseApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "sseApiId"
    Then The lifecycle status of API "sseApiId" should be "Published"
    When I retrieve the "apis" resource with id "sseApiId"
    # The API must actually OFFER the quota plan — a rejected/ignored tier would make the count assertion below pass
    # or fail for the wrong reason. Asserted on the exact array element, not with a "should contain".
    Then The value of response field "policies[0]" should be "{{subThrottlePolicyName}}"
    And I extract response field "context" and store it as "sseContext"
    # ...and the application must be subscribed ON that plan, not on AsyncUnlimited
    When I have set up application with keys, subscribed to API "sseApiId" with plan "{{subThrottlePolicyName}}", and obtained access token for "sseSubId"
    Then The response status code should be 200
    When I get the subscription with id "sseSubId"
    Then The value of response field "status" should be "UNBLOCKED"
    And The value of response field "throttlingPolicy" should be "{{subThrottlePolicyName}}"
    # 10 frames offered per stream, one per second; the 2-events/min quota must silence the stream entirely once the
    # traffic manager's verdict has landed. 10 rather than 20 so each attempt costs ~10s and the deadline affords
    # many attempts inside one quota window — the window RESETS every minute, so an attempt that happens to straddle
    # a reset gets a fresh allowance and the loop must be able to try again rather than run out of budget.
    When I invoke the SSE API at gateway context "{{sseContext}}/1.0.0" using access token "generatedAccessToken" requesting 10 events tagged "{{sseApiId}}" until its event quota throttles the stream to zero within 120 seconds

    Examples:
      | actor             |
      | admin             |
