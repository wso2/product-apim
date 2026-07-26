@cleanup
Feature: Gateway WebSocket API CORS Origin Validation

  WebSocket CORS origin validation: with the gateway-wide toggle enable_validation_for_ws=true and an
  allow_origins allow-list (supplied by this block's TOML overlay), the gateway validates the Origin header on
  the WS upgrade — an ALLOWED origin connects and echoes, NO origin is allowed (connects), and a DISALLOWED
  origin is rejected at the handshake. Ports WebSocketAPICorsValidationTestCase (and adds the disallowed-origin
  rejection the legacy omitted). Its own block because the [apim.cors] config is gateway-wide. Runs in both the
  super tenant and tenant1.com. Teardown via the per-scenario cleanup hook.

  @cap:gateway @feat:streaming-invocation @rule:cors @type:regression @dep:publisher @legacy:WebSocketAPICorsValidationTestCase
  Scenario Outline: WS API CORS validation — allowed origin echoes, disallowed origin is rejected as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_ws_echo_api.json" as "wsApiId" and deployed it
    When I publish the "apis" resource with id "wsApiId"
    Then The lifecycle status of API "wsApiId" should be "Published"
    When I retrieve the "apis" resource with id "wsApiId"
    And I extract response field "context" and store it as "wsContext"
    When I have set up application with keys, subscribed to API "wsApiId" with plan "AsyncUnlimited", and obtained access token for "wsSubId"
    Then The response status code should be 200
    # An ALLOWED origin (in the gateway allow_origins list) connects and echoes
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "hello ws" using access token "generatedAccessToken" and origin "http://allowed.example.com" expecting echo "HELLO WS" within 60 seconds
    # NO origin is allowed (the product's fix — a same-origin/non-browser client has no Origin header)
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "hello ws" using access token "generatedAccessToken" expecting echo "HELLO WS" within 60 seconds
    # A DISALLOWED origin is rejected at the WS handshake
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" using access token "generatedAccessToken" and origin "http://evil.example.com" expecting rejection within 60 seconds

    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
