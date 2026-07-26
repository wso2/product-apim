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
    And I have created an api from "artifacts/payloads/create_apim_ws_echo_api.json" as "wsApiId" and deployed it
    # Register the scope on the API and gate both WS operations (SUBSCRIBE + PUBLISH) with it
    When I retrieve the "apis" resource with id "wsApiId"
    And I put the response payload in context as "wsScopePayload"
    And I extract response field "context" and store it as "wsContext"
    When I update the "apis" resource "wsApiId" and "wsScopePayload" with configuration type "scopes" and value:
      """
      [{"shared":true,"scope":{"name":"wsScopeEnf","displayName":"wsScopeEnf","description":"ws scope enforcement","bindings":["admin"]}}]
      """
    Then The response status code should be 200
    When I retrieve the "apis" resource with id "wsApiId"
    And I put the response payload in context as "wsScopePayload"
    When I update the "apis" resource "wsApiId" and "wsScopePayload" with configuration type "operations" and value:
      """
      [{"target":"/*","verb":"SUBSCRIBE","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":["wsScopeEnf"],"operationPolicies":{"request":[],"response":[],"fault":[]}},{"target":"/*","verb":"PUBLISH","authType":"Application & Application User","throttlingPolicy":"Unlimited","scopes":["wsScopeEnf"],"operationPolicies":{"request":[],"response":[],"fault":[]}}]
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
    When I request an OAuth access token for the current user using password grant with scope "wsScopeEnf"
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

  # Throttling: PARKED (raw-WS) — re-probed with BOTH mechanisms and neither enforces on the all-in-one profile:
  #   * subscription EVENT-COUNT plan (EVENTCOUNTLIMIT) — relies on the async Traffic-Manager event loop → no-op.
  #   * API-level advanced REQUEST-COUNT policy (the legacy WebSocketAPITestCase mechanism) — RE-PROBED after the
  #     GraphQL finding: 10 frames on a 4/min limit still ALL echoed (0 throttled).
  # Contrast: GraphQL SUBSCRIPTION throttling (graphql-ws) with the SAME request-count policy DOES enforce (4003 at
  # frame 4 — see gateway/graphql_subscription_throttling). The difference is operation-awareness: the gateway
  # parses each graphql-ws `start` as a countable request and throttles locally, whereas RAW-WS frames are opaque
  # passthrough whose frame throttling depends on the TM binary-event flow that is inactive in the single-container
  # profile. So this park is genuine (confirmed with the correct mechanism), not a mechanism mismatch. Re-enable
  # only with a real Traffic Manager (wso2am-tm). Glue kept: the WS multi-frame throttle-detection step.
  #
  # @cap:gateway @feat:throttling-enforcement @rule:ws-request-count @type:regression @dep:admin @legacy:WebSocketAPITestCase
  # Scenario: A WS API is throttled once it exceeds its API-level request-count limit
  #   Given The system is ready
  #   And I have valid access tokens as "admin"
  #   When I create an advanced throttling policy "${UNIQUE:wsReq4perMin}" allowing 4 requests per minute
  #   Then The response status code should be 201
  #   And I have created an api from "artifacts/payloads/create_apim_ws_echo_api.json" as "wsApiId" and deployed it
  #   When I retrieve the "apis" resource with id "wsApiId"
  #   And I put the response payload in context as "wsThrPayload"
  #   And I extract response field "context" and store it as "wsContext"
  #   When I update the "apis" resource "wsApiId" and "wsThrPayload" with configuration type "apiThrottlingPolicy" and value:
  #   """
  #   {{advThrottlePolicyName}}
  #   """
  #   Then The response status code should be 200
  #   When I deploy the API with id "wsApiId"
  #   When I publish the "apis" resource with id "wsApiId"
  #   Then The lifecycle status of API "wsApiId" should be "Published"
  #   When I have set up application with keys, subscribed to API "wsApiId" with plan "AsyncUnlimited", and obtained access token for "wsThrSubId"
  #   Then The response status code should be 200
  #   When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" sending 10 messages using access token "generatedAccessToken" expecting throttling within 120 seconds

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
