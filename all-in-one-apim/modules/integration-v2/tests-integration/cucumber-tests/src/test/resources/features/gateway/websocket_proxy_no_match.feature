@cleanup
Feature: Gateway WebSocket API — No Proxy Profile Match

  Verifies that a backend hostname not matched by any proxy profile's target_hosts is connected to
  directly by the gateway without routing through a proxy. Runs in a container whose deployment.toml
  carries a single [[transport.ws.proxy_profile]] (wsProxyNoMatch overlay):
    target_hosts: ["proxied\.backend\.test"], proxy_host: "squid-proxy", proxy_port: 3128.
  The test API's backend is ws://nodebackend:3001. "nodebackend" does not match the pattern, so no
  profile is selected and the gateway connects directly.
  Because Squid can resolve nodebackend on the Docker network, a passing echo alone is NOT sufficient
  to prove the direct path — the gateway could silently route through Squid and still echo. The
  CONNECT count assertion (expected 0) is the definitive structural proof that Squid was never
  contacted and the gateway went direct.

  @cap:gateway @feat:streaming-invocation @rule:proxy-no-match @type:smoke @dep:publisher @legacy:WebSocketProxyProfileTestCase
  Scenario: backend hostname unmatched by any proxy profile — gateway connects directly
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
    # The backend URL is ws://nodebackend:3001. The only profile targets "proxied\.backend\.test",
    # which does not match "nodebackend". No profile is selected so the gateway goes direct.
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "no-profile-match-direct" using access token "generatedAccessToken" expecting echo "NO-PROFILE-MATCH-DIRECT" within 60 seconds
    # Zero CONNECT entries prove Squid was never contacted. The echo alone cannot prove the direct
    # path because Squid can also reach nodebackend on the Docker network.
    Then the anonymous proxy should have received exactly 0 CONNECT request(s)
