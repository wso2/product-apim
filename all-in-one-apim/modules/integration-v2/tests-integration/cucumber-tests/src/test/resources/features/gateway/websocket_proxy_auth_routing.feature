@cleanup
Feature: Gateway WebSocket API — Authenticated Proxy Routing

  Verifies that the gateway routes a backend WS connection through a Basic-auth HTTP CONNECT proxy
  when the proxy profile specifies credentials. Runs in a container whose deployment.toml carries a
  single [[transport.ws.proxy_profile]] (wsProxyAuthRouting overlay):
    target_hosts: ["nodebackend"], proxy_host: "squid-proxy", proxy_port: 3129 (auth Squid),
    proxy_username: "testproxyuser", proxy_password: "testproxypass".
  The gateway attaches a Proxy-Authorization header on the CONNECT request. Squid validates the
  credentials against its htpasswd file, relays the TCP tunnel to nodebackend:3001, and the echo
  arrives end-to-end through the proxy. A CONNECT count assertion on the authenticated Squid access
  log confirms the proxy path (and credential handshake) was completed successfully.

  @cap:gateway @feat:streaming-invocation @rule:proxy-routing @type:smoke @dep:publisher @legacy:WebSocketProxyProfileTestCase
  Scenario Outline: proxy profile with Basic-auth credentials — gateway authenticates with and routes through Squid as <actor>
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
    # The backend URL is ws://nodebackend:3001. The profile matches nodebackend with proxy_username
    # and proxy_password configured; the gateway sends a Proxy-Authorization header on the CONNECT.
    # Squid authenticates the credentials, relays the tunnel to nodebackend, and echo arrives.
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" with message "auth-proxy-routed" using access token "generatedAccessToken" expecting echo "AUTH-PROXY-ROUTED" within 60 seconds
    # Exactly one CONNECT entry in the authenticated proxy access log proves the gateway provided
    # valid credentials and the proxy accepted the tunnelling request.
    Then the authenticated proxy should have received exactly 1 CONNECT request(s)
    Examples:
      | actor             |
      | admin             |
      | admin@tenant1.com |
