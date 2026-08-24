@cleanup
Feature: Gateway WebSocket API — Anonymous Proxy Routing

  Verifies that the gateway routes a backend WS connection through an HTTP CONNECT proxy when
  the backend hostname is matched by a proxy profile with no bypass_hosts. Runs in a container
  whose deployment.toml carries a single [[transport.ws.proxy_profile]] (wsProxyAnonRouting overlay):
    target_hosts: ["nodebackend"], proxy_host: "squid-proxy", proxy_port: 3128 (anonymous Squid).
  No bypass_hosts — the gateway sends CONNECT nodebackend:3001 to squid-proxy:3128. Squid resolves
  nodebackend on the Docker network, relays the TCP tunnel, and the echo arrives end-to-end through
  the proxy. A CONNECT count assertion on the Squid access log confirms the proxy path was taken
  (not a silent fallback to direct connection).

  @cap:gateway @feat:streaming-invocation @rule:proxy-routing @type:smoke @dep:publisher @legacy:WebSocketProxyProfileTestCase
  Scenario Outline: proxy profile without bypass — gateway routes WS connection through anonymous Squid proxy as <actor>
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
    # The backend URL is ws://nodebackend:3001. The profile matches nodebackend with no bypass_hosts,
    # so the gateway sends CONNECT nodebackend:3001 to squid-proxy:3128. Squid relays the tunnel.
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "proxy-routed" using access token "generatedAccessToken" expecting echo "PROXY-ROUTED" within 60 seconds
    # Exactly one CONNECT entry in the anonymous proxy access log proves the gateway used the proxy
    # path and did not silently fall back to a direct connection.
    Then the anonymous proxy should have received exactly 1 CONNECT request(s)
    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |

  @cap:gateway @feat:streaming-invocation @rule:proxy-routing @type:smoke @dep:publisher @legacy:WebSocketProxyProfileTestCase
  Scenario Outline: proxy routes multiple WS connections — CONNECT count accumulates per connection as <actor>
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
    # Each WebSocket connection establishes its own TCP tunnel through the proxy — the proxy cannot
    # reuse a tunnel across independent WS sessions. Two invocations must produce two CONNECT entries.
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "first-connection" using access token "generatedAccessToken" expecting echo "FIRST-CONNECTION" within 60 seconds
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "second-connection" using access token "generatedAccessToken" expecting echo "SECOND-CONNECTION" within 60 seconds
    Then the anonymous proxy should have received exactly 2 CONNECT request(s)
    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
