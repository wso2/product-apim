@cleanup
Feature: Gateway WebSocket API Invocation

  Gateway-plane runtime invocation of a published WebSocket API: create a WS API whose backend is the raw-WS
  echo endpoint on the node backend, publish it, subscribe an application (async plan) and obtain a token, then
  open a WebSocket connection THROUGH the gateway's WS inbound (apim.ws.port 9099) and assert the backend echoes
  the sent message uppercased. This is the streaming counterpart of the REST/SOAP gateway-invocation features
  (which the publisher-plane streaming-design feature only creates/publishes). Runs in both the super tenant and
  tenant1.com as the tenant admin (the flow spans publish + subscribe + invoke). Teardown via the per-scenario
  cleanup hook.

  @cap:gateway @feat:streaming-invocation @type:smoke @dep:publisher @legacy:WebSocketAPITestCase
  Scenario Outline: Invoke a published WebSocket API through the gateway as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_ws_echo_api.json" as "wsApiId" and deployed it
    When I publish the "apis" resource with id "wsApiId"
    Then The lifecycle status of API "wsApiId" should be "Published"

    # Capture the API's full gateway context (already carries /t/<tenant> for tenant APIs)
    When I retrieve the "apis" resource with id "wsApiId"
    And I extract response field "context" and store it as "wsContext"

    # Subscribe an application (WS APIs require an async plan) and obtain an access token
    When I have set up application with keys, subscribed to API "wsApiId" with plan "AsyncUnlimited", and obtained access token for "wsSubId"
    Then The response status code should be 200

    # Open a WebSocket through the gateway WS inbound and assert the backend echoes the message uppercased
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "hello websocket" using access token "generatedAccessToken" expecting echo "HELLO WEBSOCKET" within 60 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Token-type parity: invoke a WS API with BOTH an application whose token type is JWT (self-contained, the
  # product default — the smoke scenario above already exercises it) and one whose token type is OAUTH (opaque
  # UUID). Legacy WebSocketAPITestCase covers both. Presents the token in the Authorization header.
  @cap:gateway @feat:streaming-invocation @rule:token-type @type:regression @dep:publisher @legacy:WebSocketAPITestCase
  Scenario Outline: Invoke a WS API with a <tokenType> application token as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_ws_echo_api.json" as "wsApiId" and deployed it
    When I publish the "apis" resource with id "wsApiId"
    Then The lifecycle status of API "wsApiId" should be "Published"
    When I retrieve the "apis" resource with id "wsApiId"
    And I extract response field "context" and store it as "wsContext"
    When I have set up a "<tokenType>" token type application with keys, subscribed to API "wsApiId" with plan "AsyncUnlimited", and obtained access token for "wsSubId"
    Then The response status code should be 200
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "hello ws" using access token "generatedAccessToken" expecting echo "HELLO WS" within 60 seconds

    Examples:
      | tokenType | actor             |
      | JWT       | admin             |
      | OAUTH     | admin             |
      | JWT       | admin@tenant1.com |
      | OAUTH     | admin@tenant1.com |

  # Negative: an invalid OAuth token is rejected at the WS handshake. A valid-token invoke first proves the API
  # is routable, so the subsequent rejection is genuine (not warm-up).
  @cap:gateway @feat:streaming-invocation @rule:security-negative @type:negative @dep:publisher @legacy:WebSocketAPITestCase
  Scenario Outline: Reject a WS invocation carrying an invalid token as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_ws_echo_api.json" as "wsApiId" and deployed it
    When I publish the "apis" resource with id "wsApiId"
    Then The lifecycle status of API "wsApiId" should be "Published"
    When I retrieve the "apis" resource with id "wsApiId"
    And I extract response field "context" and store it as "wsContext"
    When I have set up application with keys, subscribed to API "wsApiId" with plan "AsyncUnlimited", and obtained access token for "wsSubId"
    Then The response status code should be 200
    # Routable control: a valid token echoes
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "hello ws" using access token "generatedAccessToken" expecting echo "HELLO WS" within 60 seconds
    # An invalid token is rejected at the WS upgrade
    When I put the following JSON payload in context as "wsBadToken"
    """
    this-is-an-invalid-websocket-token
    """
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" using access token "wsBadToken" expecting rejection within 30 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # API-key auth: a WS API whose securityScheme includes api_key is invoked with an application API key in the
  # `apikey` header (the WS api-key auth mode).
  @cap:gateway @feat:streaming-invocation @rule:api-key @type:regression @dep:publisher @legacy:WebSocketAPITestCase
  Scenario Outline: Invoke a WS API with an application API key as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_ws_apikey_api.json" as "wsApiId" and deployed it
    When I publish the "apis" resource with id "wsApiId"
    Then The lifecycle status of API "wsApiId" should be "Published"
    When I retrieve the "apis" resource with id "wsApiId"
    And I extract response field "context" and store it as "wsContext"
    When I have set up application with keys, subscribed to API "wsApiId" with plan "AsyncUnlimited", and obtained access token for "wsSubId"
    Then The response status code should be 200
    When I put the following JSON payload in context as "wsApiKeyGenPayload"
    """
    {"keyName": "WsTestAPIKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "wsApiKeyGenPayload"
    Then The response status code should be 200
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "hello ws" using api key "apiKey" expecting echo "HELLO WS" within 60 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Negative: an API whose securityScheme is oauth2-only rejects an api-key invocation (api-key auth not enabled).
  @cap:gateway @feat:streaming-invocation @rule:security-negative @type:negative @dep:publisher @legacy:WebSocketAPITestCase
  Scenario Outline: Reject a WS api-key invocation when api-key auth is not enabled as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_ws_echo_api.json" as "wsApiId" and deployed it
    When I publish the "apis" resource with id "wsApiId"
    Then The lifecycle status of API "wsApiId" should be "Published"
    When I retrieve the "apis" resource with id "wsApiId"
    And I extract response field "context" and store it as "wsContext"
    When I have set up application with keys, subscribed to API "wsApiId" with plan "AsyncUnlimited", and obtained access token for "wsSubId"
    Then The response status code should be 200
    # Routable control: a valid token echoes
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "hello ws" using access token "generatedAccessToken" expecting echo "HELLO WS" within 60 seconds
    # The app has an API key, but the API does not enable api_key auth → rejected
    When I put the following JSON payload in context as "wsApiKeyGenPayload"
    """
    {"keyName": "WsTestAPIKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "wsApiKeyGenPayload"
    Then The response status code should be 200
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" using api key "apiKey" expecting rejection within 30 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # API-key IP restriction (WS): an API key's permittedIP is enforced at the WS handshake on the REAL socket
  # client IP. The gateway sees a host→published-port connection as the container's docker-network GATEWAY IP
  # (published by the harness as {{gatewayClientIp}}), so a key restricted to THAT IP is authorised (positive),
  # while a key restricted to a different IP is rejected (negative) — even with a matching X-Forwarded-For, since
  # the WS inbound ignores XFF (uses the socket IP), unlike the REST passthrough. All three asserted below.
  @cap:gateway @feat:streaming-invocation @rule:api-key-ip-restriction @type:regression @dep:publisher @legacy:WebSocketAPITestCase
  Scenario Outline: A WS API key's IP restriction is enforced (matching IP allowed, others rejected) as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_ws_apikey_api.json" as "wsApiId" and deployed it
    When I publish the "apis" resource with id "wsApiId"
    Then The lifecycle status of API "wsApiId" should be "Published"
    When I retrieve the "apis" resource with id "wsApiId"
    And I extract response field "context" and store it as "wsContext"
    When I have set up application with keys, subscribed to API "wsApiId" with plan "AsyncUnlimited", and obtained access token for "wsSubId"
    Then The response status code should be 200
    # POSITIVE: a key restricted to the client's effective IP (the gateway's view = {{gatewayClientIp}}) → echoes
    When I put the following JSON payload in context as "wsMatchKeyPayload"
    """
    {"keyName": "WsMatchKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "GATEWAY_CLIENT_IP", "permittedReferer": ""}}
    """
    And I replace "GATEWAY_CLIENT_IP" with "{{gatewayClientIp}}" in the payload "wsMatchKeyPayload"
    And I request an api key for application id "createdAppId" using payload "wsMatchKeyPayload"
    Then The response status code should be 200
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "hello ws" using api key "apiKey" expecting echo "HELLO WS" within 60 seconds
    # NEGATIVE: a key restricted to a different IP → rejected
    When I put the following JSON payload in context as "wsWrongKeyPayload"
    """
    {"keyName": "WsWrongKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "1.2.3.4", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "wsWrongKeyPayload"
    Then The response status code should be 200
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" using api key "apiKey" expecting rejection within 30 seconds
    # TRANSPORT FINDING: still rejected even with a matching X-Forwarded-For — the WS inbound uses the socket IP, not XFF
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" using api key "apiKey" and forwarded-for "1.2.3.4" expecting rejection within 30 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Scope enforcement: a shared scope gates the WS operations — a token WITH the scope connects and echoes; one
  # WITHOUT it is rejected at the WS handshake. Ports WebSocketAPIScopeTestCase.
  @cap:gateway @feat:streaming-invocation @rule:scope-enforcement @type:regression @dep:publisher @legacy:WebSocketAPIScopeTestCase
  Scenario Outline: A scope-gated WS API is enforced (echo with the scope, rejected without) as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a new shared scope as "wsScopeEnf"
    Then The response status code should be 201
    And I extract response field "name" and store it as "wsScopeName"
    And I have created an api from "artifacts/payloads/create_apim_ws_echo_api.json" as "wsApiId" and deployed it
    # Register the scope on the API and gate both WS operations (SUBSCRIBE + PUBLISH) with it
    When I retrieve the "apis" resource with id "wsApiId"
    And I put the response payload in context as "wsScopePayload"
    And I extract response field "context" and store it as "wsContext"
    When I update the "apis" resource "wsApiId" and "wsScopePayload" with configuration type "scopes" and value:
      """
      [{"shared":true,"scope":{"name":"{{wsScopeName}}","displayName":"{{wsScopeName}}","description":"ws scope enforcement","bindings":["admin"]}}]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "wsApiId"
    And I put the response payload in context as "wsScopePayload"
    When I update the "apis" resource "wsApiId" and "wsScopePayload" with configuration type "operations" and value:
      """
      [{"target":"/*","verb":"SUBSCRIBE","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":["{{wsScopeName}}"],"operationPolicies":{"request":[],"response":[],"fault":[]}},{"target":"/*","verb":"PUBLISH","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":["{{wsScopeName}}"],"operationPolicies":{"request":[],"response":[],"fault":[]}}]
      """
    Then The response status code should be 200
    # Redeploy the gated definition
    When I put the following JSON payload in context as "wsScopeRevPayload"
    """
    {"description":"scope revision"}
    """
    And I make a request to create a revision for "apis" resource "wsApiId" with payload "wsScopeRevPayload"
    When I put the following JSON payload in context as "wsScopeDeployPayload"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "wsApiId" with payload "wsScopeDeployPayload"
    When I publish the "apis" resource with id "wsApiId"
    Then The lifecycle status of API "wsApiId" should be "Published"
    # Subscribe an application (async plan) with password-grant keys so a scoped token can be requested
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "wsScopeKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "wsScopeKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "wsScopeSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "AsyncUnlimited"}
    """
    And I subscribe to API "wsApiId" using application "createdAppId" with payload "wsScopeSubPayload" as "wsScopeSubId"
    Then The response status code should be 201
    # A token WITH the scope connects and echoes
    When I request an OAuth access token for the current user using password grant with scope "{{wsScopeName}}"
    Then The response status code should be 200
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "hello ws" using access token "generatedAccessToken" expecting echo "HELLO WS" within 60 seconds
    # A token WITHOUT the scope is rejected (allow time for the freshly-attached scope gate to propagate under load)
    When I request an OAuth access token for the current user using password grant with scope "openid"
    Then The response status code should be 200
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" using access token "generatedAccessToken" expecting rejection within 60 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Frame-quota enforcement on a raw WebSocket, on the lever the docs prescribe for streaming APIs: a
  # SUBSCRIPTION-level EVENTCOUNTLIMIT plan of 4 events/min is the API's tier and the application subscribes on it.
  # Per docs-apim api-gateway/rate-limiting/enforce-streaming-api-limits, "each WebSocket frame (in either
  # direction) counts as one event", so this is a per-frame quota rather than a per-request one.
  #
  # BOTH-DIRECTION ARITHMETIC, and why the trip index is NOT asserted. The both-direction claim is confirmed in the
  # gateway source, not just the docs: InboundWebSocketProcessor dispatches a client->server frame to
  # RequestProcessor.handleRequest and a server->client frame to ResponseProcessor.handleResponse, and BOTH call the
  # same InboundWebsocketProcessorUtil.doThrottle, which publishes one event per frame to
  # org.wso2.throttle.request.stream:1.0.0. Against the UPPERCASING ECHO backend every client send therefore costs
  # TWO units of the quota (the send plus its echo), so a 4-events/min quota is consumed after only TWO sends — half
  # what a send-only reading would predict. But doThrottle CHECKS the local ThrottleDataHolder BEFORE it publishes,
  # and the holder is filled asynchronously by the traffic manager, so whether the third send or the fourth is the
  # one refused is a function of decision latency and not of the product. Asserting "exactly 2 echoes" would
  # therefore be a guess wearing an exact assertion. The step asserts the three things that ARE exact: the first
  # message echoed (so a never-routed API cannot pass — the vacuity that let legacy
  # ThrottlingTestCase.testEventsThrottling's `assertTrue(received < 10)` pass at 0), a frame carrying the product's
  # own literal "Error code: 4003 reason: Websocket frame throttled out" arrived, and no frame carried a DIFFERENT
  # error code. 12 messages are offered so the quota is comfortably exceeded whichever send trips.
  #
  # WHY THIS IS NO LONGER PARKED. The previous park recorded "10 frames on a 4/min limit still ALL echoed (0
  # throttled)" and attributed it to raw-WS frames being "opaque passthrough whose frame throttling depends on the TM
  # binary-event flow". BOTH halves of that are now refuted:
  #   * The mechanism claim is wrong — raw WS and graphql-ws share the SAME doThrottle (see above); the only
  #     difference is that GraphQL passes a VerbInfoDTO, which changes which key is consulted, not whether the
  #     throttle runs. So there is no separate "binary-event flow" for raw WS to depend on.
  #   * The measurement could not have detected a throttle AT ALL, on any lever. The old step counted EVERY inbound
  #     text frame as an echo and asserted `received < messageCount`. A raw-WS throttle does not go quiet and does not
  #     close: doThrottle leaves closeConnection false, so the handler writes a TEXT frame reading
  #     "Error code: 4003 ..." — which the old step tallied as an echo. A fully throttled 10-message run therefore
  #     reported 10 "echoes" and failed as "no throttling observed", which is verbatim what the park note recorded.
  #     The step now classifies the frame by that literal prefix.
  # TWO THINGS CHANGED TOGETHER (the lever AND the step), so this scenario passing does NOT by itself attribute the
  # earlier reading to one or the other; what IS established is that the previous park's stated reason was wrong on
  # the mechanism and unmeasurable in its evidence. MEASURED on the current shape: the echo backend logged
  # throttle-msg-0/1/2 and the 4th send came back as the 4003 frame — three sends is six frames, which is past a
  # 4-event quota, so the both-direction arithmetic is confirmed end to end.
  # THE TRIP INDEX IS A RANGE, NOT A NUMBER — MEASURED, and this is the one thing not to "tighten". A later run on the
  # SAME build tripped one send later: frames were [THROTTLE-MSG-0, -1, -2, -3, "Error code: 4003 ..."], i.e. four
  # echoes before the throttle instead of three. That is an OVERSHOOT past the 4-event quota, and the sibling SSE
  # scenario overshoots the same way and varies MORE: across three runs its first stream delivered 4, 4, then 7
  # events against a 2-events/min quota. Because the variance is measured INDEPENDENTLY ON BOTH LANES it is async
  # decision propagation through the embedded traffic manager, not a WS-specific defect: the gateway keeps serving
  # while the verdict is still in flight.
  # GENERAL RULE for every event-count-throttling row: the quota trips within a RANGE, never at a deterministic
  # index. The assertion is therefore "a 4003 frame appears WITHIN the message budget", never "on the Nth send".
  # Do not convert it to an exact index or an exact echo count — it would flake on this build.
  # This scenario is consequently re-enabled as a real measurement rather than kept as a park.
  @cap:gateway @feat:throttling-enforcement @rule:ws-event-count @type:regression @dep:admin @dep:publisher @legacy:WebSocketAPITestCase
  Scenario Outline: A WS API is throttled once its frames exceed the subscription plan's event quota as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create a subscription throttling policy "${UNIQUE:wsEv4perHour}" allowing 4 events per 60 "min"
    Then The response status code should be 201
    When I put JSON payload from file "artifacts/payloads/create_apim_ws_echo_api.json" in context as "wsThrPayload"
    And I replace "AsyncUnlimited" with "{{subThrottlePolicyName}}" in the payload "wsThrPayload"
    And I create an "apis" resource with payload "wsThrPayload" as "wsApiId"
    And I deploy the API with id "wsApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "wsApiId"
    Then The lifecycle status of API "wsApiId" should be "Published"
    When I retrieve the "apis" resource with id "wsApiId"
    # The API must actually OFFER the quota plan — an ignored tier would make the throttle assertion fail for the
    # wrong reason. Exact array element, not a "should contain".
    Then The value of response field "policies[0]" should be "{{subThrottlePolicyName}}"
    And I extract response field "context" and store it as "wsContext"
    When I have set up application with keys, subscribed to API "wsApiId" with plan "{{subThrottlePolicyName}}", and obtained access token for "wsThrSubId"
    Then The response status code should be 200
    When I get the subscription with id "wsThrSubId"
    Then The value of response field "status" should be "UNBLOCKED"
    And The value of response field "throttlingPolicy" should be "{{subThrottlePolicyName}}"
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" using access token "generatedAccessToken" expecting a throttled-out frame within 120 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Secure WebSocket (wss://): invoke a WS API over the gateway's SECURE WS inbound (apim.wss.port 8099) with both
  # a token and an API key. NEW vs legacy (which only tested ws://); docs-apim documents wss://host:8099 as a
  # first-class invocation path. The trust-all WS client handles the TLS handshake.
  @cap:gateway @feat:streaming-invocation @rule:wss @type:regression @dep:publisher @legacy:WebSocketAPITestCase
  Scenario Outline: Invoke a WS API over the secure wss endpoint with a token and an API key as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_ws_apikey_api.json" as "wsApiId" and deployed it
    When I publish the "apis" resource with id "wsApiId"
    Then The lifecycle status of API "wsApiId" should be "Published"
    When I retrieve the "apis" resource with id "wsApiId"
    And I extract response field "context" and store it as "wsContext"
    When I have set up application with keys, subscribed to API "wsApiId" with plan "AsyncUnlimited", and obtained access token for "wsSubId"
    Then The response status code should be 200
    When I put the following JSON payload in context as "wssApiKeyGenPayload"
    """
    {"keyName": "WssTestAPIKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "wssApiKeyGenPayload"
    Then The response status code should be 200
    # Secure WS over wss://:8099 — with an OAuth token, then with an API key
    When I invoke the WebSocket API at gateway wss context "{{wsContext}}/1.0.0" with message "hello wss" using access token "generatedAccessToken" expecting echo "HELLO WSS" within 60 seconds
    When I invoke the WebSocket API at gateway wss context "{{wsContext}}/1.0.0" with message "hello wss" using api key "apiKey" expecting echo "HELLO WSS" within 60 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Gateway-URL shape: the DevPortal advertises the WebSocket gateway invocation URL (ws:// on the WS inbound
  # port 9099) for a published WS API. Ports the enabled half of APIMANAGER5869WSGatewayURLTestCase (the disabled
  # after-config-change half is skipped). Devportal read only — no WebSocket connection.
  @cap:gateway @feat:streaming-invocation @rule:gateway-url @type:regression @dep:publisher @legacy:APIMANAGER5869WSGatewayURLTestCase
  Scenario Outline: The DevPortal advertises the WebSocket gateway URL for a WS API as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_ws_echo_api.json" as "wsApiId" and deployed it
    When I publish the "apis" resource with id "wsApiId"
    Then The lifecycle status of API "wsApiId" should be "Published"
    # The DevPortal API detail advertises the gateway endpoint URLs; a WS API carries a ws:// URL on port 9099
    When I retrieve the devportal API "wsApiId" until it contains "ws://" within 60 seconds
    Then The response status code should be 200
    And The response should contain "ws://"
    And The response should contain ":9099"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # API-key Referer restriction (WS): an API key's permittedReferer is enforced at the WS handshake against the
  # client-sent Referer header (unlike permittedIP, which the gateway matches on the socket IP). ONE referer-
  # restricted key: invoked WITH a matching Referer → echoes (positive); with a NON-matching Referer → rejected
  # (negative). Ports the referer-restriction cases of WebSocketAPITestCase.
  @cap:gateway @feat:streaming-invocation @rule:api-key-referer-restriction @type:regression @dep:publisher @legacy:WebSocketAPITestCase
  Scenario Outline: A WS API key's Referer restriction is enforced (matching referer allowed, others rejected) as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_ws_apikey_api.json" as "wsApiId" and deployed it
    When I publish the "apis" resource with id "wsApiId"
    Then The lifecycle status of API "wsApiId" should be "Published"
    When I retrieve the "apis" resource with id "wsApiId"
    And I extract response field "context" and store it as "wsContext"
    When I have set up application with keys, subscribed to API "wsApiId" with plan "AsyncUnlimited", and obtained access token for "wsSubId"
    Then The response status code should be 200
    # A key restricted to a set of referers
    When I put the following JSON payload in context as "wsRefererKeyPayload"
    """
    {"keyName": "WsRefererKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": "www.example.com/path, sub.example.com/*, *.example.com/*, www.wso2.com"}}
    """
    And I request an api key for application id "createdAppId" using payload "wsRefererKeyPayload"
    Then The response status code should be 200
    # POSITIVE: a matching Referer header → echoes
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "hello ws" using api key "apiKey" and referer "www.wso2.com" expecting echo "HELLO WS" within 60 seconds
    # NEGATIVE: a non-matching Referer → rejected
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" using api key "apiKey" and referer "www.wso2.org" expecting rejection within 30 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Negative: an EXPIRED API key is rejected at the WS handshake. A normal key echoes first (warms the route +
  # positive control), then a key generated with a 1-second validity is rejected once expired. Ports
  # WebSocketAPITestCase#testWebSocketAPIInvocationUsingExpiredAPIKey.
  @cap:gateway @feat:streaming-invocation @rule:api-key-expired @type:negative @dep:publisher @legacy:WebSocketAPITestCase
  Scenario Outline: Reject a WS invocation carrying an expired API key as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_ws_apikey_api.json" as "wsApiId" and deployed it
    When I publish the "apis" resource with id "wsApiId"
    Then The lifecycle status of API "wsApiId" should be "Published"
    When I retrieve the "apis" resource with id "wsApiId"
    And I extract response field "context" and store it as "wsContext"
    When I have set up application with keys, subscribed to API "wsApiId" with plan "AsyncUnlimited", and obtained access token for "wsSubId"
    Then The response status code should be 200
    # Warm the route + positive control with a normal key
    When I put the following JSON payload in context as "wsValidKeyPayload"
    """
    {"keyName": "WsValidKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "wsValidKeyPayload"
    Then The response status code should be 200
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "hello ws" using api key "apiKey" expecting echo "HELLO WS" within 60 seconds
    # A key valid for only 1 second → rejected once expired
    When I put the following JSON payload in context as "wsExpiredKeyPayload"
    """
    {"keyName": "WsExpiredKey", "validityPeriod": 1, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "wsExpiredKeyPayload"
    Then The response status code should be 200
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" using api key "apiKey" expecting rejection within 60 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Negative: an API whose securityScheme is api_key-only rejects an OAuth-token invocation (oauth2 auth not
  # enabled). The mirror of the api-key-when-disabled negative. Ports
  # WebSocketAPITestCase#testWebSocketAPIInvocationUsingOAuthWhenOAuthAuthenticationDisabled.
  @cap:gateway @feat:streaming-invocation @rule:oauth-disabled @type:negative @dep:publisher @legacy:WebSocketAPITestCase
  Scenario Outline: Reject a WS OAuth-token invocation when oauth2 auth is not enabled as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_ws_apikeyonly_api.json" as "wsApiId" and deployed it
    When I publish the "apis" resource with id "wsApiId"
    Then The lifecycle status of API "wsApiId" should be "Published"
    When I retrieve the "apis" resource with id "wsApiId"
    And I extract response field "context" and store it as "wsContext"
    When I have set up application with keys, subscribed to API "wsApiId" with plan "AsyncUnlimited", and obtained access token for "wsSubId"
    Then The response status code should be 200
    # An API key works (warms the route + positive control)
    When I put the following JSON payload in context as "wsApiKeyGenPayload"
    """
    {"keyName": "WsTestAPIKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "wsApiKeyGenPayload"
    Then The response status code should be 200
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "hello ws" using api key "apiKey" expecting echo "HELLO WS" within 60 seconds
    # An OAuth token is rejected because the API only enables api_key auth
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" using access token "generatedAccessToken" expecting rejection within 30 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Query-parameter auth (WS): the credential is presented as a query parameter on the WS URL instead of a header
  # (?apikey=<key> for an API key, ?access_token=<token> for an OAuth token) — the AUTH_IN.*_QUERY modes legacy
  # exercised. Both echo. Ports the query-param invocations of WebSocketAPITestCase.
  @cap:gateway @feat:streaming-invocation @rule:query-param-auth @type:regression @dep:publisher @legacy:WebSocketAPITestCase
  Scenario Outline: Invoke a WS API authenticating via a query parameter as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_ws_apikey_api.json" as "wsApiId" and deployed it
    When I publish the "apis" resource with id "wsApiId"
    Then The lifecycle status of API "wsApiId" should be "Published"
    When I retrieve the "apis" resource with id "wsApiId"
    And I extract response field "context" and store it as "wsContext"
    When I have set up application with keys, subscribed to API "wsApiId" with plan "AsyncUnlimited", and obtained access token for "wsSubId"
    Then The response status code should be 200
    When I put the following JSON payload in context as "wsApiKeyGenPayload"
    """
    {"keyName": "WsTestAPIKey", "validityPeriod": 3600, "additionalProperties": {"permittedIP": "", "permittedReferer": ""}}
    """
    And I request an api key for application id "createdAppId" using payload "wsApiKeyGenPayload"
    Then The response status code should be 200
    # API key as a query parameter
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "hello ws" using api key query param "apiKey" expecting echo "HELLO WS" within 60 seconds
    # OAuth token as a query parameter
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "hello ws" using access token query param "generatedAccessToken" expecting echo "HELLO WS" within 60 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Sandbox-only endpoint routing (WS): a WS API configured with ONLY a sandbox endpoint routes a SANDBOX token to
  # the sandbox backend (echoes — positive) but rejects a PRODUCTION token, since no production endpoint is
  # configured (negative). Ports WebSocketAPITestCase#testWebSocketAPIRemoveEndpoint.
  @cap:gateway @feat:streaming-invocation @rule:sandbox-endpoint @type:regression @dep:publisher @legacy:WebSocketAPITestCase
  Scenario Outline: A sandbox-only WS API routes a sandbox token and rejects a production token as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_ws_sandbox_api.json" as "wsApiId" and deployed it
    When I publish the "apis" resource with id "wsApiId"
    Then The lifecycle status of API "wsApiId" should be "Published"
    When I retrieve the "apis" resource with id "wsApiId"
    And I extract response field "context" and store it as "wsContext"
    # Create an application and subscribe with an async plan
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    # SANDBOX credentials + a sandbox token → routes to the sandbox endpoint → echoes (positive)
    When I put the following JSON payload in context as "wsSandboxKeysPayload"
    """
    {"keyType": "SANDBOX", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "wsSandboxKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "wsSandboxSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "AsyncUnlimited"}
    """
    And I subscribe to API "wsApiId" using application "createdAppId" with payload "wsSandboxSubPayload" as "wsSandboxSubId"
    Then The response status code should be 201
    When I request an OAuth access token for the current user using password grant with scope "SANDBOX"
    Then The response status code should be 200
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "hello ws" using access token "generatedAccessToken" expecting echo "HELLO WS" within 60 seconds
    # PRODUCTION credentials + a production token → no production endpoint configured → rejected (negative)
    When I put the following JSON payload in context as "wsProdKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "wsProdKeysPayload"
    Then The response status code should be 200
    When I request an OAuth access token for the current user using password grant with scope ""
    Then The response status code should be 200
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" using access token "generatedAccessToken" expecting rejection within 30 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # PER-URI-TEMPLATE scope discrimination — the substance of WebSocketAPIScopeTestCase, which the single shared
  # scope on a single wildcard operation above does NOT exercise. FOUR distinct API-LOCAL scopes gate FOUR
  # distinct SUBSCRIBE/PUBLISH templates (/products/catalog/{catalog-id}, /products/popular, /orders, and the
  # wildcard /*), and a password-grant token carrying exactly ONE of them may open ONLY that template's sub-path;
  # every other sub-path is REFUSED at the WS handshake. The refusals are the point — without them the test would
  # only show that a correct token works, not that the gateway discriminates per template.
  #
  # Why this works on a WS API: scope validation is per-URL-MAPPING. DefaultKeyValidationHandler#validateScopes
  # (carbon-apimgt @ 5964dc37a49, line 198 onward) finds the mapping whose urlPattern matches the resource the WS
  # dispatcher elected — bypassing the verb comparison entirely for a WS API — and authorises only if the token
  # carries one of THAT mapping's scopes. InboundWebSocketProcessor#setMatchingResource prefers an exact template
  # over /*, which is what makes the wildcard row discriminate too (a /* token is refused on /orders, and an
  # /orders token is refused on the wildcard path).
  #
  # Also pins the token response legacy asserted for each scope: the granted scope comes back in the `scope` field,
  # and the token's advertised lifetime is exactly the server's configured user-access-token validity. Legacy
  # asserted expires_in == 3600 and read that as "the 3600 the keys were generated with" — VERIFIED LIVE TO BE
  # WRONG: with the keys generated as validityTime 3600 the password-grant token still came back
  # expires_in = 86400, i.e. the value of [oauth.token_validation] user_access_token_validity in this block's
  # config (artifacts/configFiles/basic/deployment.toml). A key-generation validityTime governs the APPLICATION
  # (client_credentials) token, not the user token, so legacy's 3600 was simply the stock default of that same
  # server setting. The assertion below therefore pins the configured value, which is the property legacy was
  # actually observing.
  @cap:gateway @feat:streaming-invocation @rule:per-template-scopes @type:regression @dep:publisher @legacy:WebSocketAPIScopeTestCase
  Scenario Outline: Four per-template scopes on a WS API each open only their own sub-path as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    # Four API-local scope names, unique per runner so parallel lanes and both tenants never collide
    And I generate a unique alphanumeric value and store it as "wsTplScopeA"
    And I generate a unique alphanumeric value and store it as "wsTplScopeB"
    And I generate a unique alphanumeric value and store it as "wsTplScopeC"
    And I generate a unique alphanumeric value and store it as "wsTplScopeD"
    And I have created an api from "artifacts/payloads/create_apim_ws_echo_api.json" as "wsTplApiId" and deployed it
    When I retrieve the "apis" resource with id "wsTplApiId"
    And I put the response payload in context as "wsTplPayload"
    And I extract response field "context" and store it as "wsTplContext"
    # Register the four scopes on the API as API-LOCAL scopes (shared=false), each bound to the admin role
    When I update the "apis" resource "wsTplApiId" and "wsTplPayload" with configuration type "scopes" and value:
      """
      [{"shared":false,"scope":{"name":"{{wsTplScopeA}}","displayName":"{{wsTplScopeA}}","description":"catalog template","bindings":["admin"]}},{"shared":false,"scope":{"name":"{{wsTplScopeB}}","displayName":"{{wsTplScopeB}}","description":"popular products","bindings":["admin"]}},{"shared":false,"scope":{"name":"{{wsTplScopeC}}","displayName":"{{wsTplScopeC}}","description":"orders","bindings":["admin"]}},{"shared":false,"scope":{"name":"{{wsTplScopeD}}","displayName":"{{wsTplScopeD}}","description":"wildcard","bindings":["admin"]}}]
      """
    Then The response status code should be 200
    # Gate each template with its own scope
    When I retrieve the "apis" resource with id "wsTplApiId"
    And I put the response payload in context as "wsTplPayload"
    When I update the "apis" resource "wsTplApiId" and "wsTplPayload" with configuration type "operations" and value:
      """
      [{"target":"/products/catalog/{catalog-id}","verb":"SUBSCRIBE","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":["{{wsTplScopeA}}"],"operationPolicies":{"request":[],"response":[],"fault":[]}},{"target":"/products/catalog/{catalog-id}","verb":"PUBLISH","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":["{{wsTplScopeA}}"],"operationPolicies":{"request":[],"response":[],"fault":[]}},{"target":"/products/popular","verb":"SUBSCRIBE","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":["{{wsTplScopeB}}"],"operationPolicies":{"request":[],"response":[],"fault":[]}},{"target":"/products/popular","verb":"PUBLISH","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":["{{wsTplScopeB}}"],"operationPolicies":{"request":[],"response":[],"fault":[]}},{"target":"/orders","verb":"SUBSCRIBE","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":["{{wsTplScopeC}}"],"operationPolicies":{"request":[],"response":[],"fault":[]}},{"target":"/orders","verb":"PUBLISH","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":["{{wsTplScopeC}}"],"operationPolicies":{"request":[],"response":[],"fault":[]}},{"target":"/*","verb":"SUBSCRIBE","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":["{{wsTplScopeD}}"],"operationPolicies":{"request":[],"response":[],"fault":[]}},{"target":"/*","verb":"PUBLISH","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":["{{wsTplScopeD}}"],"operationPolicies":{"request":[],"response":[],"fault":[]}}]
      """
    Then The response status code should be 200
    # Redeploy the gated definition and publish
    When I put the following JSON payload in context as "wsTplRevPayload"
    """
    {"description":"per-template scope revision"}
    """
    And I make a request to create a revision for "apis" resource "wsTplApiId" with payload "wsTplRevPayload"
    When I put the following JSON payload in context as "wsTplDeployPayload"
    """
    [{"name":"{{gatewayEnvironment}}","vhost":"localhost","displayOnDevportal":true}]
    """
    And I make a request to deploy revision "revisionId" of "apis" resource "wsTplApiId" with payload "wsTplDeployPayload"
    When I publish the "apis" resource with id "wsTplApiId"
    Then The lifecycle status of API "wsTplApiId" should be "Published"
    # An application with password-grant keys, subscribed on the async plan, so a token can be minted per scope
    When I put JSON payload from file "artifacts/payloads/create_apim_test_app.json" in context as "createAppPayload"
    And I create an application with payload "createAppPayload"
    Then The response status code should be 201
    When I put the following JSON payload in context as "wsTplKeysPayload"
    """
    {"keyType": "PRODUCTION", "grantTypesToBeSupported": ["client_credentials", "password"]}
    """
    And I generate client credentials for application id "createdAppId" with payload "wsTplKeysPayload"
    Then The response status code should be 200
    When I put the following JSON payload in context as "wsTplSubPayload"
    """
    {"applicationId": "{{applicationId}}", "apiId": "{{apiId}}", "throttlingPolicy": "AsyncUnlimited"}
    """
    And I subscribe to API "wsTplApiId" using application "createdAppId" with payload "wsTplSubPayload" as "wsTplSubId"
    Then The response status code should be 201

    # --- ScopeA gates /products/catalog/{catalog-id}: opens /products/catalog/1, refused everywhere else ---
    When I request an OAuth access token for the current user using password grant with scope "{{wsTplScopeA}}"
    Then The response status code should be 200
    And The response should contain "{{wsTplScopeA}}"
    # 86400 = [oauth.token_validation] user_access_token_validity for this block (see the note above)
    And The value of response field "expires_in" should be "86400"
    When I invoke the WebSocket API at gateway ws context "{{wsTplContext}}/1.0.0/products/catalog/1" with message "hello ws" using access token "generatedAccessToken" expecting echo "HELLO WS" within 60 seconds
    When I invoke the WebSocket API at gateway ws context "{{wsTplContext}}/1.0.0/products/popular" using access token "generatedAccessToken" expecting rejection within 30 seconds
    When I invoke the WebSocket API at gateway ws context "{{wsTplContext}}/1.0.0/orders" using access token "generatedAccessToken" expecting rejection within 30 seconds
    When I invoke the WebSocket API at gateway ws context "{{wsTplContext}}/1.0.0/noexactmatch" using access token "generatedAccessToken" expecting rejection within 30 seconds

    # --- ScopeB gates /products/popular ---
    When I request an OAuth access token for the current user using password grant with scope "{{wsTplScopeB}}"
    Then The response status code should be 200
    And The response should contain "{{wsTplScopeB}}"
    # 86400 = [oauth.token_validation] user_access_token_validity for this block (see the note above)
    And The value of response field "expires_in" should be "86400"
    When I invoke the WebSocket API at gateway ws context "{{wsTplContext}}/1.0.0/products/popular" with message "hello ws" using access token "generatedAccessToken" expecting echo "HELLO WS" within 60 seconds
    When I invoke the WebSocket API at gateway ws context "{{wsTplContext}}/1.0.0/products/catalog/1" using access token "generatedAccessToken" expecting rejection within 30 seconds
    When I invoke the WebSocket API at gateway ws context "{{wsTplContext}}/1.0.0/orders" using access token "generatedAccessToken" expecting rejection within 30 seconds
    When I invoke the WebSocket API at gateway ws context "{{wsTplContext}}/1.0.0/noexactmatch" using access token "generatedAccessToken" expecting rejection within 30 seconds

    # --- ScopeC gates /orders ---
    When I request an OAuth access token for the current user using password grant with scope "{{wsTplScopeC}}"
    Then The response status code should be 200
    And The response should contain "{{wsTplScopeC}}"
    # 86400 = [oauth.token_validation] user_access_token_validity for this block (see the note above)
    And The value of response field "expires_in" should be "86400"
    When I invoke the WebSocket API at gateway ws context "{{wsTplContext}}/1.0.0/orders" with message "hello ws" using access token "generatedAccessToken" expecting echo "HELLO WS" within 60 seconds
    When I invoke the WebSocket API at gateway ws context "{{wsTplContext}}/1.0.0/products/catalog/1" using access token "generatedAccessToken" expecting rejection within 30 seconds
    When I invoke the WebSocket API at gateway ws context "{{wsTplContext}}/1.0.0/products/popular" using access token "generatedAccessToken" expecting rejection within 30 seconds
    When I invoke the WebSocket API at gateway ws context "{{wsTplContext}}/1.0.0/noexactmatch" using access token "generatedAccessToken" expecting rejection within 30 seconds

    # --- ScopeD gates the wildcard /*: opens a path matching NO exact template, refused on all three exact ones ---
    When I request an OAuth access token for the current user using password grant with scope "{{wsTplScopeD}}"
    Then The response status code should be 200
    And The response should contain "{{wsTplScopeD}}"
    # 86400 = [oauth.token_validation] user_access_token_validity for this block (see the note above)
    And The value of response field "expires_in" should be "86400"
    When I invoke the WebSocket API at gateway ws context "{{wsTplContext}}/1.0.0/noexactmatch" with message "hello ws" using access token "generatedAccessToken" expecting echo "HELLO WS" within 60 seconds
    When I invoke the WebSocket API at gateway ws context "{{wsTplContext}}/1.0.0/products/catalog/1" using access token "generatedAccessToken" expecting rejection within 30 seconds
    When I invoke the WebSocket API at gateway ws context "{{wsTplContext}}/1.0.0/products/popular" using access token "generatedAccessToken" expecting rejection within 30 seconds
    When I invoke the WebSocket API at gateway ws context "{{wsTplContext}}/1.0.0/orders" using access token "generatedAccessToken" expecting rejection within 30 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Raw-WS frame throttling — formerly PARKED as an infrastructure gap ("0 of 10 frames throttled, needs a real
  # Traffic Manager"). That park was WRONG on both counts and is retracted; the evidence is set out below.
  #   * The mechanism is NOT absent: RequestProcessor#handleRequest calls
  #     InboundWebsocketProcessorUtil#doThrottle(msgSize, null, ...) for EVERY non-control frame, and doThrottle
  #     publishes an org.wso2.throttle.request.stream:1.0.0 event and reads the decision back from
  #     ThrottleDataHolder — the very same Traffic-Manager round trip the HTTP ThrottleHandler uses, and the same
  #     one the GraphQL subscription path uses (doThrottleForGraphQL just delegates to doThrottle). There is no
  #     raw-WS-specific gate, so "GraphQL throttles but raw WS cannot" was not a real distinction.
  #   * The re-probe measured the wrong thing: a throttled raw-WS frame is NOT dropped. With
  #     closeConnection=false the gateway answers it with a TEXT frame carrying
  #     InboundProcessorResponseDTO#getErrorResponseString() — "Error code: 4003 reason: Websocket frame throttled
  #     out" — and the old detection step counted ANY inbound text frame as a successful echo, so ten throttled
  #     frames were indistinguishable from ten echoes. The step now matches the 4003 error frame explicitly.
  # The policy is attached BEFORE the API's first and only revision is deployed, so the assertion cannot depend on
  # a second deploy event propagating.
  @cap:gateway @feat:throttling-enforcement @rule:ws-request-count @type:regression @dep:admin @dep:publisher @legacy:WebSocketAPITestCase
  Scenario Outline: A WS API is throttled once it exceeds its API-level request-count limit as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    When I create an advanced throttling policy "${UNIQUE:wsReq4perMin}" allowing 4 requests per minute
    Then The response status code should be 201
    # Attach the policy to the API definition BEFORE the first deploy, so one revision carries it
    When I put JSON payload from file "artifacts/payloads/create_apim_ws_echo_api.json" in context as "wsThrPayload"
    And I set the field "apiThrottlingPolicy" to "{{advThrottlePolicyName}}" in the payload "wsThrPayload"
    And I create an "apis" resource with payload "wsThrPayload" as "wsThrApiId"
    Then The response status code should be 201
    And I extract response field "context" and store it as "wsThrContext"
    And The value of response field "apiThrottlingPolicy" should be "{{advThrottlePolicyName}}"
    When I deploy the API with id "wsThrApiId"
    Then The response status code should be 201
    When I publish the "apis" resource with id "wsThrApiId"
    Then The lifecycle status of API "wsThrApiId" should be "Published"
    # The deployed revision must actually carry the policy — otherwise a "not throttled" outcome would only mean
    # the policy never reached the gateway.
    When I retrieve the "apis" resource with id "wsThrApiId"
    Then The value of response field "apiThrottlingPolicy" should be "{{advThrottlePolicyName}}"
    When I have set up application with keys, subscribed to API "wsThrApiId" with plan "AsyncUnlimited", and obtained access token for "wsThrSubId"
    Then The response status code should be 200
    When I invoke the WebSocket API at gateway ws context "{{wsThrContext}}/1.0.0" using access token "generatedAccessToken" expecting a throttled-out frame within 120 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  # Gateway-URL shape: the DevPortal advertises the gateway invocation URLs for a published API, and legacy
  # APIMANAGER5869WSGayewatURLTestCase#testApiGatewayUrlsTest pins their FULL shape for both API types —
  # http(s)://host:port/<CONTEXT>/<VERSION> for a REST API and ws://host:port/<CONTEXT>/<VERSION> for a WS API,
  # with the /t/<tenant> prefix in the tenant case. The previous v2 assertion only checked that the substrings
  # "ws://" and ":9099" appeared ANYWHERE in the devportal payload — which says nothing about the path, so it
  # would have passed on a URL missing the context, the version or the tenant prefix entirely.
  #
  # Asserted here EXACTLY instead of by legacy's regex, on the built-in Default environment (vhost localhost,
  # VHost.DEFAULT_WS_PORT 9099 / DEFAULT_WSS_PORT 8099, http 8280 / https 8243). The publisher `context` field
  # already carries the /t/<tenant> prefix for a tenant API and does NOT carry the version, so the version is
  # appended — which is precisely the CONTEXT/VERSION statement the legacy regex was reaching for.
  #
  # The disabled second half of that legacy class (testApiGatewayUrlsAfterConfigChangeTest — override the
  # advertised gateway host/port and re-assert) is covered by devportal/environment_urls.feature, which drives the
  # override through the live vhost mechanism and asserts all four URL flavours exactly.
  # Devportal read only — no WebSocket connection.
  @cap:gateway @feat:streaming-invocation @rule:gateway-url @type:regression @dep:publisher @legacy:APIMANAGER5869WSGatewayURLTestCase
  Scenario Outline: The DevPortal advertises the full gateway URL shape for a WS API and a REST API as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"

    # --- A WebSocket API: the ws/wss URLs carry the WS inbound ports and the full context/version path ---
    And I have created an api from "artifacts/payloads/create_apim_ws_echo_api.json" as "wsUrlApiId" and deployed it
    When I publish the "apis" resource with id "wsUrlApiId"
    Then The lifecycle status of API "wsUrlApiId" should be "Published"
    When I retrieve the "apis" resource with id "wsUrlApiId"
    And I extract response field "context" and store it as "wsUrlContext"
    When I retrieve the devportal API "wsUrlApiId" until it contains "ws://" within 60 seconds
    Then The response status code should be 200
    And The value of response field "endpointURLs[0].URLs.ws" should be "ws://localhost:9099{{wsUrlContext}}/1.0.0"
    And The value of response field "endpointURLs[0].URLs.wss" should be "wss://localhost:8099{{wsUrlContext}}/1.0.0"

    # --- A REST API: the http/https URLs carry the passthrough ports and the same context/version path ---
    And I have created an api from "artifacts/payloads/create_apim_test_api.json" as "restUrlApiId" and deployed it
    When I publish the "apis" resource with id "restUrlApiId"
    Then The lifecycle status of API "restUrlApiId" should be "Published"
    When I retrieve the "apis" resource with id "restUrlApiId"
    And I extract response field "context" and store it as "restUrlContext"
    When I retrieve the devportal API "restUrlApiId" until it contains "http://" within 60 seconds
    Then The response status code should be 200
    And The value of response field "endpointURLs[0].URLs.http" should be "http://localhost:8280{{restUrlContext}}/1.0.0"
    And The value of response field "endpointURLs[0].URLs.https" should be "https://localhost:8243{{restUrlContext}}/1.0.0"

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
