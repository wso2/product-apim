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
  Scenario: Reject an SSE invocation carrying an invalid token
    Given The system is ready
    And I have valid access tokens as "admin"
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
