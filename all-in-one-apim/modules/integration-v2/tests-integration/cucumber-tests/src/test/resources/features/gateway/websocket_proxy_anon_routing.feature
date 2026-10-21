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

  Runtime observation: a tenant-scoped invocation establishes two gateway-to-backend WS connections (the data
  connection and a separate connection carrying an empty SOAP envelope); a super-tenant invocation establishes one.
  The exact per-actor CONNECT counts below preserve this distinction and detect unexpected extra or missing routes.

  @cap:gateway @feat:streaming-invocation @rule:proxy-routing @type:smoke @dep:publisher @legacy:WebSocketProxyProfileTestCase
  Scenario Outline: proxy profile without bypass — gateway routes WS connection through anonymous Squid proxy as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_ws_echo_api.json" as "wsApiId" and deployed it
    When I publish the "apis" resource with id "wsApiId"
    Then The lifecycle status of API "wsApiId" should be "Published"
    When I retrieve the "apis" resource with id "wsApiId"
    Then The value of response field "endpointConfig.production_endpoints.url" should be "ws://nodebackend:3001"
    And The value of response field "endpointConfig.sandbox_endpoints.url" should be "ws://nodebackend:3001"
    And I extract response field "context" and store it as "wsContext"
    Then The response field "context" should start with "<contextPrefix>"
    When I have set up application with keys, subscribed to API "wsApiId" with plan "AsyncUnlimited", and obtained access token for "wsSubId"
    Then The response status code should be 200
    # Establish route readiness before clearing setup traffic. Measure a single non-retried invocation afterward.
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "route-ready" using access token "generatedAccessToken" expecting echo "ROUTE-READY" within 60 seconds and await close
    And the anonymous proxy CONNECT activity should settle before the access-log baseline is reset
    And the proxy access logs are cleared
    When I invoke the WebSocket API once at gateway ws context "{{wsContext}}/1.0.0" with message "proxy-routed" using access token "generatedAccessToken" expecting echo "PROXY-ROUTED"
    # Exact actor-specific CONNECT count proves the backend routes used the proxy.
    Then the anonymous proxy should have received exactly <connectCount> CONNECT request(s)

    Examples:
      | actor             | contextPrefix     | connectCount |
      | admin             | /                 | 1            |
      | admin@tenant1.com | /t/tenant1.com/   | 2            |

  @cap:gateway @feat:streaming-invocation @rule:proxy-routing @type:smoke @dep:publisher @legacy:WebSocketProxyProfileTestCase
  Scenario Outline: proxy routes multiple WS connections — CONNECT count accumulates per connection as <actor>
    Given The system is ready
    And I have valid access tokens as "<actor>"
    And I have created an api from "artifacts/payloads/create_apim_ws_echo_api.json" as "wsApiId" and deployed it
    When I publish the "apis" resource with id "wsApiId"
    Then The lifecycle status of API "wsApiId" should be "Published"
    When I retrieve the "apis" resource with id "wsApiId"
    Then The value of response field "endpointConfig.production_endpoints.url" should be "ws://nodebackend:3001"
    And The value of response field "endpointConfig.sandbox_endpoints.url" should be "ws://nodebackend:3001"
    And I extract response field "context" and store it as "wsContext"
    Then The response field "context" should start with "<contextPrefix>"
    When I have set up application with keys, subscribed to API "wsApiId" with plan "AsyncUnlimited", and obtained access token for "wsSubId"
    Then The response status code should be 200
    # A 200 subscription response can precede gateway route installation. Establish readiness with a real echo,
    # then clear its setup connection so only the two measured, one-shot invocations count toward CONNECT.
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "route-ready" using access token "generatedAccessToken" expecting echo "ROUTE-READY" within 60 seconds and await close
    And the anonymous proxy CONNECT activity should settle before the access-log baseline is reset
    And the proxy access logs are cleared
    # Assert the exact actor-specific total across two independent client sessions.
    When I invoke the WebSocket API once at gateway ws context "{{wsContext}}/1.0.0" with message "first-connection" using access token "generatedAccessToken" expecting echo "FIRST-CONNECTION"
    When I invoke the WebSocket API once at gateway ws context "{{wsContext}}/1.0.0" with message "second-connection" using access token "generatedAccessToken" expecting echo "SECOND-CONNECTION"
    Then the anonymous proxy should have received exactly <connectCount> CONNECT request(s)

    Examples:
      | actor             | contextPrefix     | connectCount |
      | admin             | /                 | 2            |
      | admin@tenant1.com | /t/tenant1.com/   | 4            |
