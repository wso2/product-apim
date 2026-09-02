@cleanup
Feature: Gateway WebSocket API — Authenticated Proxy Routing with Wrong Credentials

  Verifies that the gateway fails to route a WS connection when the proxy profile carries
  incorrect credentials. Runs in a container whose deployment.toml carries a single
  [[transport.ws.proxy_profile]] (wsProxyAuthWrongCreds overlay):
    target_hosts: ["nodebackend"], proxy_host: "squid-proxy", proxy_port: 3129 (auth Squid),
    proxy_username: "wronguser", proxy_password: "wrongpass".
  APIM's WS proxy client fails before sending CONNECT to Squid (connection rejected at the
  gateway/proxy-setup layer), so both Squid access logs receive zero CONNECT entries. The test
  asserts three things:
    1. WS is rejected — proves the feature works (wrong credentials → WS handshake fails).
    2. Anonymous proxy CONNECT count = 0 — proves the gateway did not fall back to the anon proxy.
    3. Authenticated proxy CONNECT count = 0 — proves the failure is at the proxy-setup layer,
       not a Squid-side rejection; Squid was never reached.
  Together these three assertions distinguish this failure mode from broken network (would affect
  all proxy blocks), wrong target host (would show a different count pattern), or stale API state
  (caught by the lifecycle-status assertion earlier in the scenario).

  @cap:gateway @feat:streaming-invocation @rule:proxy-routing @type:negative @dep:publisher @legacy:WebSocketProxyProfileTestCase
  Scenario: wrong proxy credentials — gateway fails to establish proxy tunnel and rejects WS handshake
    Given The system is ready
    And I have valid access tokens as "admin"
    And the proxy access logs are cleared
    And I have created an api from "artifacts/payloads/create_apim_ws_echo_api.json" as "wsApiId" and deployed it
    When I publish the "apis" resource with id "wsApiId"
    Then The lifecycle status of API "wsApiId" should be "Published"
    When I retrieve the "apis" resource with id "wsApiId"
    And I extract response field "context" and store it as "wsContext"
    When I have set up application with keys, subscribed to API "wsApiId" with plan "AsyncUnlimited", and obtained access token for "wsSubId"
    Then The response status code should be 200
    # The proxy profile carries wrong credentials (wronguser:wrongpass). The gateway fails to
    # establish the proxy tunnel, so the WS upgrade is rejected.
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" using access token "generatedAccessToken" expecting rejection within 30 seconds
    # count = 0 on the anon proxy — the gateway did not fall back to the unauthenticated proxy.
    Then the anonymous proxy should have received exactly 0 CONNECT request(s)
    # count = 0 on the auth proxy — APIM's WS proxy client fails before sending CONNECT to Squid;
    # the rejection is at the gateway/proxy-setup layer, not a Squid 407 credential rejection.
    And the authenticated proxy should have received exactly 0 CONNECT request(s)
