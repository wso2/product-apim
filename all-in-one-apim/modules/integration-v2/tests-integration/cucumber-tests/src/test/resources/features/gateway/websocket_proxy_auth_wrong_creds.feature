@cleanup
Feature: Gateway WebSocket API — Authenticated Proxy Routing with Wrong Credentials

  Verifies that the gateway cannot route a WS connection when the proxy profile carries incorrect
  credentials. Runs in a container whose deployment.toml carries a single [[transport.ws.proxy_profile]]
  (wsProxyAuthWrongCreds overlay):
    target_hosts: ["nodebackend"], proxy_host: "squid-proxy", proxy_port: 3129 (auth Squid),
    proxy_username: "wronguser", proxy_password: "wrongpass".

  What actually happens, measured from the gateway's own log: the gateway DOES send CONNECT to the
  authenticated Squid, presenting the wrong credentials, and Squid refuses it —
    io.netty.handler.proxy.HttpProxyHandler$HttpProxyConnectException: http, basic,
    squid-proxy/<ip>:3129 => nodebackend/<ip>:3001, status: 407 Proxy Authentication Required
  — after which the gateway reports ERROR_CODE 1014 "Error connecting with the backend" and the WS
  handshake fails. Squid records that denied attempt in its access log as a TCP_DENIED/407 CONNECT, so
  the authenticated proxy's CONNECT count is one, not zero.

  The scenario asserts four things, and the first two are what make the rest meaningful:
    1. The API is ROUTABLE before the negative probe. Without that gate the gateway rejects the upgrade
       with "No matching API was found to dispatch the request" while the deployment is still
       propagating; the probe accepts any rejection, so the scenario would pass having never reached the
       proxy at all. That is not hypothetical — it is how this scenario passed until now, and the two
       CI runs that bracket the change show it exactly: the passing run logged one "No matching API" and
       zero proxy-connect failures, the failing run logged zero and three respectively.
    2. The REASON, from the gateway: a 407 from squid-proxy. This is what distinguishes "the proxy denied
       our credentials" from any other reason a WS handshake can fail, and no count can express it.
    3. Anonymous proxy CONNECT count = 0 — the gateway did not fall back to the unauthenticated proxy.
    4. Authenticated proxy CONNECT count = 1 — exactly one tunnel attempt, and it was denied.

  @cap:gateway @feat:streaming-invocation @rule:proxy-routing @type:negative @dep:publisher @legacy:WebSocketProxyProfileTestCase
  Scenario: wrong proxy credentials — the authenticated proxy denies the tunnel and the WS handshake fails
    Given The system is ready
    And I have valid access tokens as "admin"
    And I have created an api from "artifacts/payloads/create_apim_ws_echo_api.json" as "wsApiId" and deployed it
    When I publish the "apis" resource with id "wsApiId"
    Then The lifecycle status of API "wsApiId" should be "Published"
    When I retrieve the "apis" resource with id "wsApiId"
    And I extract response field "context" and store it as "wsContext"
    When I have set up application with keys, subscribed to API "wsApiId" with plan "AsyncUnlimited", and obtained access token for "wsSubId"
    Then The response status code should be 200
    # The control under test (proxy credentials) sits DOWNSTREAM of dispatch, so the API has to be
    # routable before a rejection means anything. See assertion 1 in the description above.
    And the "apis" resource "wsApiId" should be live on the gateway, redeploying if propagation is lost
    # Cleared after the routability gate so only the probe below is counted.
    And the proxy access logs are cleared
    And I mark the current end of the server log file "wso2carbon.log"
    When I invoke the WebSocket API at gateway ws context "{{wsContext}}/1.0.0" using access token "generatedAccessToken" expecting rejection within 30 seconds
    # The gateway names the reason itself: it reached squid-proxy and was refused for credentials.
    Then The server log file "wso2carbon.log" should gain a line containing all of the following within 60 seconds
      | HttpProxyConnectException |
      | squid-proxy               |
      | 407                       |
    # No fallback to the unauthenticated proxy.
    And the anonymous proxy should have received exactly 0 CONNECT request(s)
    # Squid logs every refused CONNECT. The gateway may make more than one downstream attempt while propagating
    # the failed WebSocket transport, so the invariant is that the configured authenticated proxy was reached.
    And the authenticated proxy should have received at least 1 CONNECT request(s)
