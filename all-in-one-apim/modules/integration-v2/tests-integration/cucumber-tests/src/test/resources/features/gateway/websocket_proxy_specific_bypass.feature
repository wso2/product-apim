@cleanup
Feature: Gateway WebSocket API — Specific Proxy Profile Bypass

  Verifies bypass_hosts behaviour when a specific (non-catch-all) profile matches the backend
  hostname. Runs in a container whose deployment.toml carries a single [[transport.ws.proxy_profile]]
  entry (wsProxySpecificBypass overlay):
    Profile — target_hosts: ["nodebackend"], bypass_hosts: ["nodebackend"],
               proxy_host: "squid-proxy", proxy_port: 3128.
  bypass_hosts takes precedence over the proxy — the gateway connects directly to nodebackend
  without routing through Squid. A CONNECT count assertion of 0 is the definitive proof — a
  passing echo alone is insufficient because Squid can also reach nodebackend on the Docker network.

  @cap:gateway @feat:streaming-invocation @rule:proxy-bypass @type:smoke @dep:publisher @legacy:WebSocketProxyProfileTestCase
  Scenario: bypass_hosts in a specific proxy profile — gateway connects directly to the backend
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
    # The backend URL is ws://nodebackend:3001. The profile matches nodebackend (target_hosts) but
    # nodebackend is also in bypass_hosts, so the gateway bypasses the proxy and connects directly.
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "proxy-bypass-direct" using access token "generatedAccessToken" expecting echo "PROXY-BYPASS-DIRECT" within 60 seconds
    # Zero CONNECT entries prove Squid was never contacted, confirming bypass took effect.
    Then the anonymous proxy should have received exactly 0 CONNECT request(s)
