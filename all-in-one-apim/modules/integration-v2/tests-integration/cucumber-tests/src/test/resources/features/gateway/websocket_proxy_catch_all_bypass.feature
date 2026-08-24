@cleanup
Feature: Gateway WebSocket API — Catch-All Proxy Profile Bypass

  Verifies bypass_hosts behaviour when the backend hostname is matched only by a catch-all profile
  (target_hosts: ["*"]). Runs in a container whose deployment.toml carries a single catch-all
  [[transport.ws.proxy_profile]] (wsProxyCatchAll overlay):
    target_hosts: ["*"], bypass_hosts: ["nodebackend"], proxy_host: "squid-proxy", proxy_port: 3128.
  No specific profile for nodebackend is present — it falls through to the catch-all. Because
  nodebackend is in bypass_hosts, the gateway bypasses the proxy and connects directly.
  Because Squid can resolve nodebackend on the Docker network, a passing echo alone is NOT sufficient
  to prove the bypass path — the gateway could route through Squid and still echo. The CONNECT count
  assertion (expected 0) is the definitive structural proof that the bypass took effect.

  @cap:gateway @feat:streaming-invocation @rule:proxy-bypass @type:smoke @dep:publisher @legacy:WebSocketProxyProfileTestCase
  Scenario Outline: bypass_hosts in a catch-all proxy profile — gateway connects directly to the backend as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And the proxy access logs are cleared
    And I have created an api from "artifacts/payloads/create_apim_ws_echo_api.json" as "wsApiId" and deployed it
    When I publish the "apis" resource with id "wsApiId"
    Then The lifecycle status of API "wsApiId" should be "Published"
    When I retrieve the "apis" resource with id "wsApiId"
    And I extract response field "context" and store it as "wsContext"
    When I have set up application with keys, subscribed to API "wsApiId" with plan "AsyncUnlimited", and obtained access token for "wsSubId"
    Then The response status code should be 200
    # The backend URL is ws://nodebackend:3001. nodebackend does not match any specific profile,
    # so it falls through to the catch-all. The catch-all has nodebackend in bypass_hosts, so the
    # gateway bypasses the proxy and connects directly to nodebackend:3001.
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "catchall-bypass-direct" using access token "generatedAccessToken" expecting echo "CATCHALL-BYPASS-DIRECT" within 60 seconds
    # Zero CONNECT entries prove Squid was never contacted. The echo alone cannot prove bypass
    # because Squid can also reach nodebackend on the Docker network.
    Then the anonymous proxy should have received exactly 0 CONNECT request(s)
    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
