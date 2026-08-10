@cleanup
Feature: Gateway WebSocket API — Authenticated Proxy Routing with Wrong Credentials

  Verifies that the gateway is rejected by the authenticated Squid proxy when the proxy profile
  carries incorrect credentials. Runs in a container whose deployment.toml carries a single
  [[transport.ws.proxy_profile]] (wsProxyAuthWrongCreds overlay):
    target_hosts: ["nodebackend"], proxy_host: "squid-proxy", proxy_port: 3129 (auth Squid),
    proxy_username: "wronguser", proxy_password: "wrongpass".
  Squid validates CONNECT requests against its htpasswd file (testproxyuser:testproxypass). The
  gateway sends Proxy-Authorization with the wrong credentials; Squid returns 407 Proxy
  Authentication Required and the CONNECT tunnel is never established. The WS upgrade therefore
  fails. A CONNECT count of 1 in the authenticated proxy access log confirms the proxy was reached
  and the rejection came from Squid credential validation, not a pre-proxy network failure.

  @cap:gateway @feat:streaming-invocation @rule:proxy-routing @type:negative @dep:publisher @legacy:WebSocketProxyProfileTestCase
  Scenario: wrong proxy credentials — Squid rejects the CONNECT and WS handshake fails
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
    # The gateway sends CONNECT nodebackend:3001 with Proxy-Authorization: Basic wronguser:wrongpass.
    # Squid validates against htpasswd and returns 407. The tunnel cannot be established, so the
    # WS upgrade request fails and the client connection is rejected.
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" using access token "generatedAccessToken" expecting rejection within 30 seconds
    # One CONNECT entry in the authenticated log proves Squid received and processed the request.
    # Zero would mean the gateway gave up before reaching the proxy (different failure mode).
    Then the authenticated proxy should have received exactly 1 CONNECT request(s)
